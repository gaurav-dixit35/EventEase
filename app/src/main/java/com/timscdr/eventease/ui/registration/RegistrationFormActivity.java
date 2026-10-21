package com.timscdr.eventease.ui.registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.RegistrationViewModel;

public final class RegistrationFormActivity extends AppCompatActivity {
    public static final String EVENT_ID = "event_id";
    public static final String STUDENT_ID = "student_id";
    private RegistrationViewModel model;
    private boolean routed;
    private boolean completedNavigation;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_registration_form);
        long eventId = getIntent().getLongExtra(EVENT_ID, 0), studentId = getIntent().getLongExtra(STUDENT_ID, 0);
        MaterialToolbar toolbar = findViewById(R.id.registration_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, studentId, eventId))
                .get(RegistrationViewModel.class);
        MaterialButton submit = findViewById(R.id.confirm_registration);
        submit.setOnClickListener(v -> {
            RegistrationViewModel.State current = model.state().getValue();
            if (current != null && current.registration != null) { openTicket(current); return; }
            if (!((CheckBox) findViewById(R.id.confirm_details)).isChecked()) {
                ((TextView) findViewById(R.id.registration_message)).setText(R.string.confirm_details_required);
                findViewById(R.id.registration_message).setVisibility(View.VISIBLE); return;
            }
            model.submit();
        });
        model.state().observe(this, this::render);
    }
    private void render(RegistrationViewModel.State state) {
        findViewById(R.id.registration_loading).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.registration_content).setVisibility(state.loading ? View.GONE : View.VISIBLE);
        if (state.event != null) {
            ((TextView) findViewById(R.id.registration_event_title)).setText(state.event.title);
            ((TextView) findViewById(R.id.registration_event_summary)).setText(
                    getString(R.string.event_summary, DateTimeUtils.dateTime(this, state.event.dateTime), state.event.venue));
        }
        if (state.profile != null && !routed) {
            ((EditText) findViewById(R.id.registration_name)).setText(state.profile.name);
            ((EditText) findViewById(R.id.registration_roll)).setText(state.profile.rollNo);
            ((EditText) findViewById(R.id.registration_email)).setText(state.profile.email);
            routed = true;
        }
        TextView message = findViewById(R.id.registration_message);
        int messageId = state.full ? R.string.event_full : state.started ? R.string.event_started
                : state.alreadyRegistered ? R.string.already_registered
                : state.event == null || state.profile == null ? R.string.registration_unavailable
                : state.error ? R.string.registration_error : 0;
        message.setVisibility(messageId == 0 ? View.GONE : View.VISIBLE);
        if (messageId != 0) message.setText(messageId);
        MaterialButton button = findViewById(R.id.confirm_registration);
        button.setText(state.alreadyRegistered ? R.string.view_ticket : R.string.confirm_registration);
        button.setEnabled(!state.loading && !state.submitting && (state.alreadyRegistered
                || (!state.full && !state.started && state.event != null && state.profile != null)));
        if (state.submitting) button.setText(R.string.registration_saving);
        if (state.success && state.registration != null && !completedNavigation) {
            completedNavigation = true;
            startActivity(ConfirmationActivity.intent(this, state.event, state.profile, state.registration));
            finish();
        }
    }
    private void openTicket(RegistrationViewModel.State state) {
        if (state.event != null && state.profile != null && state.registration != null)
            startActivity(TicketQRActivity.intent(this, state.event.id, state.profile.id, state.registration.id));
    }
}
