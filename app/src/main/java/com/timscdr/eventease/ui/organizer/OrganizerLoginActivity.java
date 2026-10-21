package com.timscdr.eventease.ui.organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.OrganizerAuthViewModel;

public final class OrganizerLoginActivity extends AppCompatActivity {
    private OrganizerAuthViewModel model;
    private boolean routed;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_organizer_login);
        ((MaterialToolbar) findViewById(R.id.organizer_login_toolbar)).setNavigationOnClickListener(v -> finish());
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(OrganizerAuthViewModel.class);
        findViewById(R.id.back_to_student_login).setOnClickListener(v -> ScreenNavigation.login(this));
        findViewById(R.id.organizer_login_submit).setOnClickListener(v -> model.signIn(
                text(R.id.organizer_email), text(R.id.organizer_password)));
        model.state().observe(this, stateValue -> {
            boolean loading = stateValue.status == OrganizerAuthViewModel.Status.LOADING;
            findViewById(R.id.organizer_login_progress).setVisibility(loading ? View.VISIBLE : View.GONE);
            findViewById(R.id.organizer_login_submit).setEnabled(!loading);
            TextView error = findViewById(R.id.organizer_login_error);
            if (stateValue.status == OrganizerAuthViewModel.Status.ERROR) {
                error.setText("configuration".equals(stateValue.error) ? R.string.firebase_not_configured
                        : "credentials".equals(stateValue.error) ? R.string.organizer_credentials_required
                        : R.string.organizer_login_error);
                error.setVisibility(View.VISIBLE);
            } else error.setVisibility(View.GONE);
            if (stateValue.status == OrganizerAuthViewModel.Status.SIGNED_IN && !routed) {
                routed = true; ScreenNavigation.organizerHome(this);
            }
        });
    }
    private String text(int id) {
        TextInputEditText input = findViewById(id);
        return input.getText() == null ? "" : input.getText().toString();
    }
}
