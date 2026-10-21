package com.timscdr.eventease.ui.organizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.timscdr.eventease.R;
import com.timscdr.eventease.repository.AttendeeRecord;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.OrganizerAttendeesViewModel;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class OrganizerAttendeesActivity extends AppCompatActivity {
    public static final String EVENT_ID = "attendee_event_id", EVENT_TITLE = "attendee_event_title";
    public static final String EVENT_DATE = "attendee_event_date", EVENT_VENUE = "attendee_event_venue";
    private AttendeeAdapter adapter;
    private List<AttendeeRecord> exportRows;
    private String eventTitle;
    private final ActivityResultLauncher<String> exportPicker = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"), this::writeCsv);
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_organizer_attendees);
        MaterialToolbar toolbar = findViewById(R.id.attendees_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        long eventId = getIntent().getLongExtra(EVENT_ID, 0);
        if (eventId <= 0) { finish(); return; }
        eventTitle = getIntent().getStringExtra(EVENT_TITLE);
        ((TextView) findViewById(R.id.attendees_event_title)).setText(eventTitle);
        long date = getIntent().getLongExtra(EVENT_DATE, 0);
        String venue = getIntent().getStringExtra(EVENT_VENUE);
        ((TextView) findViewById(R.id.attendees_event_meta)).setText(
                getString(R.string.organizer_attendees_meta, DateTimeUtils.dateTime(this, date), venue));
        RecyclerView list = findViewById(R.id.attendees_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendeeAdapter(); list.setAdapter(adapter);
        TextInputEditText search = findViewById(R.id.attendees_search);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.filter(s.toString()); renderEmpty(); }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        findViewById(R.id.attendees_scan_ticket).setOnClickListener(v -> startActivity(
                new Intent(this, ScanTicketActivity.class).putExtra(ScanTicketActivity.EVENT_ID, eventId)));
        findViewById(R.id.attendees_export).setOnClickListener(v -> {
            exportRows = adapter.all();
            if (!exportRows.isEmpty()) exportPicker.launch(fileName(eventTitle));
            else Toast.makeText(this, R.string.organizer_attendees_empty, Toast.LENGTH_SHORT).show();
        });
        OrganizerAttendeesViewModel model = new ViewModelProvider(this,
                new EventEaseViewModelFactory(this, 0, eventId)).get(OrganizerAttendeesViewModel.class);
        model.attendees.observe(this, rows -> {
            adapter.submit(rows);
            int count = rows == null ? 0 : rows.size(), checked = 0;
            if (rows != null) for (AttendeeRecord row : rows) if (row.checkedIn) checked++;
            ((TextView) findViewById(R.id.attendees_summary)).setText(getResources().getQuantityString(
                    R.plurals.organizer_attendee_count, count, count, checked));
            renderEmpty();
        });
    }
    private void renderEmpty() {
        boolean fullEmpty = adapter.all().isEmpty();
        boolean filteredEmpty = adapter.getItemCount() == 0 && !fullEmpty;
        TextView empty = findViewById(R.id.attendees_empty);
        empty.setText(filteredEmpty ? R.string.attendees_no_search_results : R.string.organizer_attendees_empty);
        empty.setVisibility(adapter.getItemCount() == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    }
    private void writeCsv(Uri uri) {
        if (uri == null || exportRows == null) return;
        try (OutputStream output = getContentResolver().openOutputStream(uri);
             OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.write("Name,Roll Number,Email,College,Registration Code,Registered At,Status\r\n");
            for (AttendeeRecord row : exportRows) {
                writer.write(csv(row.name) + "," + csv(row.rollNo) + "," + csv(row.email) + ","
                        + csv(row.collegeName) + "," + csv(row.registrationCode) + ","
                        + csv(DateTimeUtils.dateTime(this, row.registeredAt)) + ","
                        + csv(row.checkedIn ? "Checked In" : "Registered") + "\r\n");
            }
            writer.flush(); Toast.makeText(this, R.string.organizer_export_saved, Toast.LENGTH_LONG).show();
        } catch (Exception failure) { Toast.makeText(this, R.string.organizer_export_failed, Toast.LENGTH_LONG).show(); }
    }
    static String csv(String value) {
        String safe = value == null ? "" : value;
        int first = 0;
        while (first < safe.length() && (Character.isWhitespace(safe.charAt(first))
                || Character.isISOControl(safe.charAt(first)))) first++;
        if (first < safe.length() && "=+-@".indexOf(safe.charAt(first)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    private static String fileName(String title) {
        String clean = title == null ? "event" : title.trim().replaceAll("[^A-Za-z0-9_-]+", "_");
        return (clean.isEmpty() ? "event" : clean) + "-attendees.csv";
    }
}
