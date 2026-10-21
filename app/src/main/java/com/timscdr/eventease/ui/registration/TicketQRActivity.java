package com.timscdr.eventease.ui.registration;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.WriterException;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.QRCodeGenerator;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.TicketViewModel;

public final class TicketQRActivity extends AppCompatActivity {
    static final String EVENT_ID = "event_id", STUDENT_ID = "student_id", REGISTRATION_ID = "registration_id";
    public static Intent intent(Context context, long eventId, long studentId, long registrationId) {
        return new Intent(context, TicketQRActivity.class).putExtra(EVENT_ID, eventId)
                .putExtra(STUDENT_ID, studentId).putExtra(REGISTRATION_ID, registrationId);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_ticket_qr);
        ((MaterialToolbar) findViewById(R.id.ticket_toolbar)).setNavigationOnClickListener(v -> finish());
        long eventId = getIntent().getLongExtra(EVENT_ID, 0), studentId = getIntent().getLongExtra(STUDENT_ID, 0);
        long ticketId = getIntent().getLongExtra(REGISTRATION_ID, 0);
        TicketViewModel model = new ViewModelProvider(this,
                new EventEaseViewModelFactory(this, studentId, eventId, ticketId)).get(TicketViewModel.class);
        model.state().observe(this, this::render);
    }
    private void render(TicketViewModel.State state) {
        findViewById(R.id.ticket_loading).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        boolean ready = !state.loading && !state.unavailable
                && state.event != null && state.profile != null && state.registration != null;
        findViewById(R.id.ticket_card).setVisibility(ready ? View.VISIBLE : View.GONE);
        findViewById(R.id.ticket_data_error).setVisibility(state.unavailable ? View.VISIBLE : View.GONE);
        if (!ready) return;
        findViewById(R.id.ticket_error).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.ticket_event_title)).setText(state.event.title);
        ((TextView) findViewById(R.id.ticket_date)).setText(DateTimeUtils.dateTime(this, state.event.dateTime));
        ((TextView) findViewById(R.id.ticket_venue)).setText(state.event.venue);
        ((TextView) findViewById(R.id.ticket_student)).setText(getString(R.string.ticket_student_identity,
                state.profile.name, state.profile.rollNo));
        ((TextView) findViewById(R.id.ticket_code)).setText(state.registration.registrationCode);
        Bitmap crest = BitmapFactory.decodeResource(getResources(), R.drawable.timscdr_crest_white);
        if (crest == null) {
            findViewById(R.id.ticket_error).setVisibility(View.VISIBLE);
            return;
        }
        try {
            Bitmap qr = QRCodeGenerator.create(state.registration.registrationCode, 640, crest,
                    ContextCompat.getColor(this, R.color.primary), ContextCompat.getColor(this, R.color.accent));
            ((ImageView) findViewById(R.id.ticket_qr)).setImageBitmap(qr);
        } catch (WriterException | IllegalArgumentException error) {
            findViewById(R.id.ticket_error).setVisibility(View.VISIBLE);
        } finally {
            crest.recycle();
        }
    }
}
