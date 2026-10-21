package com.timscdr.eventease.ui.organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.timscdr.eventease.R;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.ScanTicketViewModel;

public final class ScanTicketActivity extends AppCompatActivity {
    public static final String EVENT_ID = "scan_event_id";
    private ScanTicketViewModel model;
    private final ActivityResultLauncher<ScanOptions> scanner = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Snackbar.make(findViewById(R.id.scan_ticket_action), R.string.scan_cancelled, Snackbar.LENGTH_SHORT).show();
        } else model.validate(result.getContents());
    });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_scan_ticket);
        ((MaterialToolbar) findViewById(R.id.scan_toolbar)).setNavigationOnClickListener(v -> finish());
        long eventId = getIntent().getLongExtra(EVENT_ID, 0);
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0, eventId)).get(ScanTicketViewModel.class);
        ((MaterialButton) findViewById(R.id.scan_ticket_action)).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getString(R.string.scan_camera_prompt));
            options.setBeepEnabled(false);
            options.setBarcodeImageEnabled(false);
            scanner.launch(options);
        });
        model.state().observe(this, this::render);
    }

    private void render(ScanTicketViewModel.State state) {
        TextView result = findViewById(R.id.scan_result);
        if (state.status == ScanTicketViewModel.Status.IDLE) {
            result.setVisibility(View.GONE);
            findViewById(R.id.scan_progress).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.scan_progress).setVisibility(state.status == ScanTicketViewModel.Status.CHECKING
                ? View.VISIBLE : View.GONE);
        result.setVisibility(View.VISIBLE);
        if (state.status == ScanTicketViewModel.Status.CHECKING) result.setText(R.string.scan_checking);
        else if (state.status == ScanTicketViewModel.Status.INVALID) result.setText(R.string.scan_invalid);
        else if (state.status == ScanTicketViewModel.Status.ALREADY_USED) result.setText(getString(R.string.scan_already_used,
                state.studentName == null ? "" : state.studentName));
        else if (state.status == ScanTicketViewModel.Status.VALID) result.setText(getString(R.string.scan_valid,
                state.studentName, state.rollNo, state.eventTitle));
        else result.setText(R.string.scan_storage_error);
        int color = state.status == ScanTicketViewModel.Status.VALID ? R.color.success
                : state.status == ScanTicketViewModel.Status.CHECKING ? R.color.text_primary : R.color.error;
        result.setTextColor(getColor(color));
    }
}
