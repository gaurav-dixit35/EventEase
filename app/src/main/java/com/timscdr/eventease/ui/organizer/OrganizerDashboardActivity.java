package com.timscdr.eventease.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.timscdr.eventease.R;
import com.timscdr.eventease.repository.OrganizerEventSummary;
import com.timscdr.eventease.repository.OrganizerStats;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.OrganizerAuthViewModel;
import com.timscdr.eventease.viewmodel.OrganizerDashboardViewModel;

public final class OrganizerDashboardActivity extends AppCompatActivity {
    private OrganizerDashboardViewModel model;
    private OrganizerAuthViewModel auth;
    private OrganizerEventsAdapter adapter;
    private boolean signingOut;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_organizer_dashboard);
        MaterialToolbar toolbar = findViewById(R.id.organizer_dashboard_toolbar);
        toolbar.setLogoDescription(getString(R.string.crest_description));
        EventEaseViewModelFactory factory = new EventEaseViewModelFactory(this, 0);
        model = new ViewModelProvider(this, factory).get(OrganizerDashboardViewModel.class);
        auth = new ViewModelProvider(this, factory).get(OrganizerAuthViewModel.class);
        ((TextView) findViewById(R.id.organizer_welcome)).setText(auth.currentName());
        RecyclerView list = findViewById(R.id.organizer_events_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrganizerEventsAdapter(new OrganizerEventsAdapter.Actions() {
            @Override public void attendees(OrganizerEventSummary event) { openAttendees(event); }
            @Override public void edit(OrganizerEventSummary event) {
                startActivity(CreateEventActivity.editIntent(OrganizerDashboardActivity.this,
                        event.event, event.registeredCount));
            }
            @Override public void delete(OrganizerEventSummary event) { confirmDelete(event); }
        });
        list.setAdapter(adapter);
        findViewById(R.id.create_organizer_event).setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));
        findViewById(R.id.organizer_sign_out).setOnClickListener(v -> { signingOut = true; auth.signOut(); });
        model.events.observe(this, events -> {
            findViewById(R.id.organizer_events_progress).setVisibility(View.GONE);
            adapter.submit(events);
            findViewById(R.id.organizer_events_empty).setVisibility(events == null || events.isEmpty() ? View.VISIBLE : View.GONE);
        });
        model.stats.observe(this, this::renderStats);
        model.operationError().observe(this, error -> {
            if (error != null) Snackbar.make(list, R.string.organizer_event_delete_blocked, Snackbar.LENGTH_LONG).show();
        });
        auth.state().observe(this, value -> {
            if (signingOut && value.status == OrganizerAuthViewModel.Status.SIGNED_OUT) {
                signingOut = false; ScreenNavigation.organizerLogin(this);
            } else if (value.status == OrganizerAuthViewModel.Status.ERROR) {
                Snackbar.make(list, R.string.organizer_signout_error, Snackbar.LENGTH_LONG).show();
            }
        });
    }
    private void renderStats(OrganizerStats stats) {
        if (stats == null) return;
        ((TextView) findViewById(R.id.stat_events)).setText(String.valueOf(stats.eventCount));
        ((TextView) findViewById(R.id.stat_registrations)).setText(String.valueOf(stats.registrationCount));
        ((TextView) findViewById(R.id.stat_checked_in)).setText(String.valueOf(stats.checkedInCount));
    }
    private void openAttendees(OrganizerEventSummary event) {
        startActivity(new Intent(this, OrganizerAttendeesActivity.class)
                .putExtra(OrganizerAttendeesActivity.EVENT_ID, event.event.id)
                .putExtra(OrganizerAttendeesActivity.EVENT_TITLE, event.event.title)
                .putExtra(OrganizerAttendeesActivity.EVENT_DATE, event.event.dateTime)
                .putExtra(OrganizerAttendeesActivity.EVENT_VENUE, event.event.venue));
    }
    private void confirmDelete(OrganizerEventSummary event) {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.organizer_delete_event)
                .setMessage(R.string.organizer_delete_confirm)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.organizer_delete_event, (dialog, which) -> model.deleteEvent(event.event.id))
                .show();
    }
}
