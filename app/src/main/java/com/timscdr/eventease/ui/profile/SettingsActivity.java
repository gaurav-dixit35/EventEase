package com.timscdr.eventease.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.SettingsViewModel;

public final class SettingsActivity extends AppCompatActivity {
    public static final String STUDENT_ID = "student_id";
    private SettingsViewModel settings;
    private SwitchMaterial notifications;
    private boolean rendering, pendingPermission;
    private final ActivityResultLauncher<String> permissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (pendingPermission && granted) settings.setNotificationsEnabled(true);
                else {
                    rendering = true; notifications.setChecked(false); rendering = false;
                    Snackbar.make(notifications, R.string.notification_permission_denied, Snackbar.LENGTH_LONG).show();
                }
                pendingPermission = false;
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_settings);
        long studentId = getIntent().getLongExtra(STUDENT_ID, 0);
        ((MaterialToolbar) findViewById(R.id.settings_toolbar)).setNavigationOnClickListener(v -> finish());
        settings = new ViewModelProvider(this, new EventEaseViewModelFactory(this, studentId))
                .get(SettingsViewModel.class);
        AuthViewModel auth = new ViewModelProvider(this,
                new EventEaseViewModelFactory(this, studentId)).get(AuthViewModel.class);
        auth.state().observe(this, value -> {
            if (value.status == AuthState.Status.SIGNED_OUT) ScreenNavigation.login(this);
        });
        findViewById(R.id.settings_logout).setOnClickListener(v -> auth.logout());
        String version;
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException impossible) { version = "1.0"; }
        ((TextView) findViewById(R.id.about_version)).setText(getString(R.string.about_version, version));

        RadioGroup themeOptions = findViewById(R.id.theme_options);
        themeOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (rendering) return;
            String mode = checkedId == R.id.theme_light ? "light"
                    : checkedId == R.id.theme_dark ? "dark" : "system";
            settings.setThemeMode(mode);
        });
        notifications = findViewById(R.id.notifications_toggle);
        notifications.setOnCheckedChangeListener((button, checked) -> {
            if (rendering) return;
            if (!checked) { settings.setNotificationsEnabled(false); return; }
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = true;
                rendering = true; notifications.setChecked(false); rendering = false;
                permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else settings.setNotificationsEnabled(true);
        });
        settings.state().observe(this, value -> {
            rendering = true;
            int selection = "light".equals(value.themeMode) ? R.id.theme_light
                    : "dark".equals(value.themeMode) ? R.id.theme_dark : R.id.theme_system;
            themeOptions.check(selection);
            notifications.setChecked(value.notificationsEnabled);
            rendering = false;
            int desired = "light".equals(value.themeMode) ? AppCompatDelegate.MODE_NIGHT_NO
                    : "dark".equals(value.themeMode) ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (AppCompatDelegate.getDefaultNightMode() != desired) AppCompatDelegate.setDefaultNightMode(desired);
        });
    }
}
