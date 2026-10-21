package com.timscdr.eventease.ui.splash;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.OrganizerAuthViewModel;

public class SplashActivity extends AppCompatActivity {
    private static final long MINIMUM_SPLASH_MS = 3_000;
    private static final long MAXIMUM_STARTUP_WAIT_MS = 10_000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ObjectAnimator loadingLine;
    private boolean routed;
    private boolean minimumDisplayElapsed;
    private AuthState latestAuthState;
    private OrganizerAuthViewModel organizerAuth;
    private OrganizerAuthViewModel.State latestOrganizerState;
    private boolean organizerResolved;
    private final Runnable minimumDisplay = () -> {
        minimumDisplayElapsed = true;
        routeIfReady();
    };
    private final Runnable startupTimeout = () -> {
        if (!routed) {
            // Login is still usable offline and provides a manual organizer entry.
            // Do not leave the launcher activity on screen forever for a Firebase task.
            routed = true;
            ScreenNavigation.login(this);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Let Android's brief launch splash transition to the complete branded screen below.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        main.postDelayed(minimumDisplay, MINIMUM_SPLASH_MS);
        main.postDelayed(startupTimeout, MAXIMUM_STARTUP_WAIT_MS);
        View line = findViewById(R.id.loading_line);
        int width = (int) (120 * getResources().getDisplayMetrics().density);
        loadingLine = ObjectAnimator.ofInt(line, "minimumWidth", 1, width);
        loadingLine.setDuration(700);
        loadingLine.setRepeatCount(ObjectAnimator.INFINITE);
        loadingLine.setRepeatMode(ObjectAnimator.REVERSE);
        EventEaseViewModelFactory factory = new EventEaseViewModelFactory(this, 0);
        organizerAuth = new ViewModelProvider(this, factory).get(OrganizerAuthViewModel.class);
        AuthViewModel model = new ViewModelProvider(this,
                factory).get(AuthViewModel.class);
        findViewById(R.id.retry).setOnClickListener(view -> {
            if (latestOrganizerState == null
                    || latestOrganizerState.status == OrganizerAuthViewModel.Status.ERROR) {
                organizerAuth.restoreSession();
            }
            if (latestAuthState == null || latestAuthState.status == AuthState.Status.ERROR
                    || latestAuthState.status == AuthState.Status.SIGNED_OUT) {
                model.restoreSession();
            }
        });
        organizerAuth.state().observe(this, state -> {
            latestOrganizerState = state;
            if (state.status == OrganizerAuthViewModel.Status.SIGNED_IN) {
                findViewById(R.id.splash_error).setVisibility(View.GONE);
                findViewById(R.id.retry).setVisibility(View.GONE);
            } else if (state.status == OrganizerAuthViewModel.Status.SIGNED_OUT
                    || state.status == OrganizerAuthViewModel.Status.ERROR) {
                organizerResolved = true;
                if (state.status == OrganizerAuthViewModel.Status.SIGNED_OUT
                        && model.state().getValue().status == AuthState.Status.IDLE) model.restoreSession();
            }
            routeIfReady();
        });
        model.state().observe(this, state -> {
            latestAuthState = state;
            boolean failed = state.status == AuthState.Status.ERROR;
            findViewById(R.id.splash_error).setVisibility(failed ? View.VISIBLE : View.GONE);
            findViewById(R.id.retry).setVisibility(failed ? View.VISIBLE : View.GONE);
            routeIfReady();
        });
        organizerAuth.restoreSession();
    }

    @Override protected void onStart() { super.onStart(); loadingLine.start(); }
    @Override protected void onStop() { loadingLine.cancel(); super.onStop(); }
    @Override protected void onDestroy() {
        main.removeCallbacks(minimumDisplay);
        main.removeCallbacks(startupTimeout);
        super.onDestroy();
    }

    private void routeIfReady() {
        if (routed || !minimumDisplayElapsed || latestOrganizerState == null) return;
        if (latestOrganizerState != null && latestOrganizerState.status == OrganizerAuthViewModel.Status.SIGNED_IN) {
            routed = true;
            ScreenNavigation.organizerHome(this);
            return;
        }
        if (latestOrganizerState.status == OrganizerAuthViewModel.Status.ERROR) {
            routed = true;
            ScreenNavigation.login(this);
            return;
        }
        if (!organizerResolved || latestAuthState == null) return;
        if (latestAuthState.status == AuthState.Status.SIGNED_IN) {
            routed = true;
            ScreenNavigation.home(this);
        } else if (latestAuthState.status == AuthState.Status.SIGNED_OUT
                || latestAuthState.status == AuthState.Status.ERROR) {
            routed = true;
            ScreenNavigation.login(this);
        }
    }
}
