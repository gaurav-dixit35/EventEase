package com.timscdr.eventease.ui.dashboard;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.timscdr.eventease.R;
import com.timscdr.eventease.ui.myevents.MyEventsFragment;
import com.timscdr.eventease.ui.profile.ProfileFragment;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

public final class DashboardActivity extends AppCompatActivity {
    private AuthViewModel auth;
    private boolean routed;
    private long studentId;
    private int selected = R.id.nav_events;
    private BottomNavigationView navigation;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        if (savedInstanceState != null) selected = savedInstanceState.getInt("tab", R.id.nav_events);
        navigation = findViewById(R.id.bottom_navigation);
        navigation.setOnItemSelectedListener(item -> {
            if (studentId <= 0) return false;
            select(item.getItemId());
            return true;
        });
        auth = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(AuthViewModel.class);
        findViewById(R.id.retry).setOnClickListener(view -> auth.restoreSession());
        auth.state().observe(this, state -> {
            boolean signedIn = state.status == AuthState.Status.SIGNED_IN;
            findViewById(R.id.dashboard_content).setVisibility(signedIn ? View.VISIBLE : View.GONE);
            findViewById(R.id.session_gate).setVisibility(signedIn ? View.GONE : View.VISIBLE);
            boolean failed = state.status == AuthState.Status.ERROR;
            findViewById(R.id.progress).setVisibility(failed ? View.GONE : View.VISIBLE);
            findViewById(R.id.session_error).setVisibility(failed ? View.VISIBLE : View.GONE);
            findViewById(R.id.retry).setVisibility(failed ? View.VISIBLE : View.GONE);
            if (signedIn) {
                studentId = state.studentId;
                navigation.setSelectedItemId(selected);
            } else if (state.status == AuthState.Status.SIGNED_OUT && !routed) {
                routed = true;
                ScreenNavigation.login(this);
            }
        });
    }

    private void select(int id) {
        selected = id;
        String tag = "tab:" + id;
        Fragment target = getSupportFragmentManager().findFragmentByTag(tag);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // A restored fragment must belong to the currently validated session.
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment.requireArguments().getLong(ProfileFragment.STUDENT_ID) != studentId) {
                transaction.remove(fragment);
                if (fragment == target) target = null;
            } else if (fragment != target) {
                transaction.hide(fragment).setMaxLifecycle(fragment, Lifecycle.State.STARTED);
            }
        }
        if (target == null) {
            target = id == R.id.nav_profile ? ProfileFragment.create(studentId)
                    : id == R.id.nav_my_events ? MyEventsFragment.create(studentId)
                    : EventListFragment.create(studentId);
            transaction.add(R.id.profile_container, target, tag);
        } else transaction.show(target);
        transaction.setMaxLifecycle(target, Lifecycle.State.RESUMED).commitNow();
    }

    @Override protected void onStart() { super.onStart(); auth.restoreSession(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("tab", selected);
        super.onSaveInstanceState(out);
    }
}
