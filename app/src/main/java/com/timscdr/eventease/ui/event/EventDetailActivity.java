package com.timscdr.eventease.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.EventBannerImages;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.ui.registration.RegistrationFormActivity;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventDetailViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

public final class EventDetailActivity extends AppCompatActivity {
    public static final String EVENT_ID = "event_id";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private EventDetailViewModel model;
    private AuthViewModel auth;
    private Event event;
    private boolean signedIn, routed;
    private long activeStudent;
    private final Runnable tick = () -> { if (model != null) model.tick(); };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        MaterialToolbar toolbar = findViewById(R.id.detail_toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_share || event == null || !signedIn) return false;
            String details = event.title + "\n" + DateTimeUtils.dateTime(this, event.dateTime)
                    + "\n" + event.venue + "\n" + event.organizerName;
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, details), getString(R.string.share_event)));
            return true;
        });
        findViewById(R.id.register_now).setOnClickListener(view -> {
            if (event == null || activeStudent <= 0) return;
            startActivity(new Intent(this, RegistrationFormActivity.class)
                    .putExtra(RegistrationFormActivity.EVENT_ID, event.id)
                    .putExtra(RegistrationFormActivity.STUDENT_ID, activeStudent));
        });
        auth = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(AuthViewModel.class);
        findViewById(R.id.detail_retry).setOnClickListener(view -> auth.restoreSession());
        auth.state().observe(this, state -> {
            signedIn = state.status == AuthState.Status.SIGNED_IN;
            boolean failed = state.status == AuthState.Status.ERROR;
            findViewById(R.id.detail_content).setVisibility(signedIn ? View.VISIBLE : View.GONE);
            findViewById(R.id.detail_status).setVisibility(signedIn ? View.GONE : View.VISIBLE);
            ((TextView) findViewById(R.id.detail_status)).setText(failed ? R.string.error_storage : R.string.loading);
            findViewById(R.id.detail_retry).setVisibility(failed ? View.VISIBLE : View.GONE);
            toolbar.getMenu().findItem(R.id.action_share).setEnabled(signedIn && event != null);
            if (signedIn) {
                if (model == null || activeStudent != state.studentId) {
                    if (model != null) model.state().removeObservers(this);
                    activeStudent = state.studentId;
                    long eventId = getIntent().getLongExtra(EVENT_ID, 0);
                    model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, activeStudent, eventId))
                            .get("event:" + eventId + ":student:" + activeStudent, EventDetailViewModel.class);
                    model.state().observe(this, this::render);
                } else render(model.state().getValue());
            } else if (state.status == AuthState.Status.SIGNED_OUT && !routed) {
                routed = true; ScreenNavigation.login(this);
            }
        });
    }

    private void render(EventDetailViewModel.State state) {
        if (state == null || !signedIn) return;
        event = state.event;
        boolean ready = event != null && !state.loading;
        findViewById(R.id.detail_content).setVisibility(ready ? View.VISIBLE : View.GONE);
        findViewById(R.id.detail_status).setVisibility(ready ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.detail_status)).setText(state.loading ? R.string.loading : R.string.event_missing);
        ((MaterialToolbar) findViewById(R.id.detail_toolbar)).getMenu().findItem(R.id.action_share).setEnabled(ready);
        if (ready) {
            text(R.id.detail_title, event.title);
            text(R.id.detail_category, event.category);
            text(R.id.detail_organizer, event.organizerName);
            text(R.id.detail_date, DateTimeUtils.dateTime(this, event.dateTime));
            text(R.id.detail_venue, event.venue);
            text(R.id.detail_description, event.description);
            text(R.id.detail_capacity, getString(R.string.seats_available, state.available, event.capacity));
            ImageView banner = findViewById(R.id.detail_banner);
            EventBannerImages.load(banner, event);
            MaterialButton button = findViewById(R.id.register_now);
            button.setText(state.registered ? R.string.already_registered : state.started ? R.string.event_started
                    : state.available == 0 ? R.string.event_full : R.string.register_now);
            button.setEnabled(!state.registered && !state.started && state.available > 0);
        }
        handler.removeCallbacks(tick);
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED))
            handler.postDelayed(tick, model.nextTickDelay());
    }
    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
    @Override protected void onStart() { super.onStart(); auth.restoreSession(); }
    @Override protected void onResume() { super.onResume(); if (model != null) model.tick(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
}
