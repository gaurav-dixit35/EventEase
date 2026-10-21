package com.timscdr.eventease.ui.auth;

import android.view.View;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

/** Shared rendering only. Validation and persistence are delegated to the ViewModel. */
abstract class AuthFormActivity extends AppCompatActivity {
    protected AuthViewModel model;
    private OnBackPressedCallback busyBack;
    private boolean routed;

    protected void bindForm(int layout) {
        setContentView(layout);
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(AuthViewModel.class);
        busyBack = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { /* Finish the submitted operation first. */ }
        };
        getOnBackPressedDispatcher().addCallback(this, busyBack);
        model.state().observe(this, this::render);
    }

    protected String input(int id) {
        TextInputEditText field = findViewById(id);
        return field.getText() == null ? "" : field.getText().toString();
    }

    private void render(AuthState state) {
        boolean loading = state.status == AuthState.Status.LOADING;
        busyBack.setEnabled(loading);
        findViewById(R.id.progress).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.submit).setEnabled(!loading);
        findViewById(R.id.alternate).setEnabled(!loading);
        int[] layouts = {R.id.name_layout, R.id.roll_layout, R.id.course_layout, R.id.year_layout, R.id.email_layout, R.id.college_layout};
        for (int id : layouts) {
            TextInputLayout field = findViewById(id);
            if (field != null) { field.setEnabled(!loading); field.setError(null); }
        }
        TextView error = findViewById(R.id.form_error);
        error.setVisibility(View.GONE);
        if (state.status == AuthState.Status.SIGNED_IN && !routed) {
            routed = true;
            ScreenNavigation.home(this);
        } else if (state.status == AuthState.Status.ERROR) {
            int field = 0;
            int message;
            switch (state.error) {
                case NAME: field = R.id.name_layout; message = R.string.error_name; break;
                case ROLL: field = R.id.roll_layout; message = R.string.error_roll; break;
                case COURSE: field = R.id.course_layout; message = R.string.error_course; break;
                case YEAR: field = R.id.year_layout; message = R.string.error_year; break;
                case EMAIL: field = R.id.email_layout; message = R.string.error_email; break;
                case COLLEGE: field = R.id.college_layout; message = R.string.error_college; break;
                case AUTH_FAILED: message = R.string.error_auth; break;
                case DUPLICATE: message = R.string.error_duplicate; break;
                default: message = R.string.error_storage;
            }
            if (field != 0) {
                TextInputLayout target = findViewById(field);
                target.setError(getString(message));
                if (target.getEditText() != null) target.getEditText().requestFocus();
            } else {
                error.setText(message);
                error.setVisibility(View.VISIBLE);
            }
        }
    }
}
