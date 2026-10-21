package com.timscdr.eventease.ui.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.util.DateTimeUtils;

public final class ConfirmationActivity extends AppCompatActivity {
    static final String EVENT_ID = "event_id", STUDENT_ID = "student_id", REGISTRATION_ID = "registration_id";
    static final String TITLE = "event_title", DATE = "event_date", VENUE = "event_venue";
    static Intent intent(Context context, Event event, StudentProfile profile, Registration registration) {
        return new Intent(context, ConfirmationActivity.class).putExtra(EVENT_ID, event.id)
                .putExtra(STUDENT_ID, profile.id).putExtra(REGISTRATION_ID, registration.id)
                .putExtra(TITLE, event.title).putExtra(DATE, DateTimeUtils.dateTime(context, event.dateTime))
                .putExtra(VENUE, event.venue);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_confirmation);
        ((TextView) findViewById(R.id.confirmation_event_title)).setText(getIntent().getStringExtra(TITLE));
        ((TextView) findViewById(R.id.confirmation_event_summary)).setText(
                getString(R.string.event_summary, getIntent().getStringExtra(DATE), getIntent().getStringExtra(VENUE)));
        ((MaterialButton) findViewById(R.id.view_ticket)).setOnClickListener(v -> startActivity(TicketQRActivity.intent(
                this, getIntent().getLongExtra(EVENT_ID, 0), getIntent().getLongExtra(STUDENT_ID, 0),
                getIntent().getLongExtra(REGISTRATION_ID, 0))));
        findViewById(R.id.back_to_events).setOnClickListener(v -> finish());
    }
}
