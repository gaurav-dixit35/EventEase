package com.timscdr.eventease.ui;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.timscdr.eventease.R;
import com.timscdr.eventease.ui.auth.LoginActivity;
import com.timscdr.eventease.ui.auth.SignupActivity;
import com.timscdr.eventease.ui.dashboard.DashboardActivity;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AuthFlowTest {
    @org.junit.Before public void resetApplication() throws Exception { RepositoryIsolation.reset(); }
    @Test @Config(sdk = 28, qualifiers = "night")
    public void nightModeSignupInflatesAndRetainsDraftOnRecreation() {
        try (ActivityController<SignupActivity> controller = Robolectric.buildActivity(SignupActivity.class).setup()) {
            ((TextInputEditText) controller.get().findViewById(R.id.name)).setText("Draft Student");
            ((TextInputEditText) controller.get().findViewById(R.id.roll)).setText("DRAFT001");
            controller.recreate();
            assertEquals("Draft Student", ((TextInputEditText) controller.get().findViewById(R.id.name)).getText().toString());
            assertEquals("DRAFT001", ((TextInputEditText) controller.get().findViewById(R.id.roll)).getText().toString());
            assertEquals(0xFF0F1420, controller.get().getColor(R.color.background));
        }
    }

    @Test public void signupProfileLogoutAndLoginRunThroughRealScreens() throws Exception {
        try (ActivityController<SignupActivity> signupController = Robolectric.buildActivity(SignupActivity.class).setup()) {
            SignupActivity signup = signupController.get();
            signup.findViewById(R.id.submit).performClick();
            assertEquals(signup.getString(R.string.error_name),
                    ((TextInputLayout) signup.findViewById(R.id.name_layout)).getError());
            ((TextInputEditText) signup.findViewById(R.id.name)).setText("Screen Student");
            ((TextInputEditText) signup.findViewById(R.id.roll)).setText("SCREEN001");
            ((TextInputEditText) signup.findViewById(R.id.course)).setText("MCA");
            ((TextInputEditText) signup.findViewById(R.id.year)).setText("1");
            ((TextInputEditText) signup.findViewById(R.id.email)).setText("screen@example.edu");
            signup.findViewById(R.id.submit).performClick();
            until(() -> shadowOf(signup).peekNextStartedActivity() != null);
            Intent destination = shadowOf(signup).getNextStartedActivity();
            assertEquals(DashboardActivity.class.getName(), destination.getComponent().getClassName());
            assertTrue((destination.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
        }

        try (ActivityController<DashboardActivity> dashboardController = Robolectric.buildActivity(DashboardActivity.class).setup()) {
            DashboardActivity dashboard = dashboardController.get();
            until(() -> dashboard.findViewById(R.id.dashboard_content).getVisibility() == android.view.View.VISIBLE);
            ((com.google.android.material.bottomnavigation.BottomNavigationView) dashboard.findViewById(R.id.bottom_navigation))
                    .setSelectedItemId(R.id.nav_profile);
            until(() -> dashboard.findViewById(R.id.student_name) != null
                    && "Screen Student".contentEquals(((TextView) dashboard.findViewById(R.id.student_name)).getText()));
            assertEquals("Roll No. SCREEN001", ((TextView) dashboard.findViewById(R.id.student_roll)).getText().toString());
            dashboard.findViewById(R.id.logout).performClick();
            until(() -> shadowOf(dashboard).peekNextStartedActivity() != null);
            assertEquals(LoginActivity.class.getName(), shadowOf(dashboard).getNextStartedActivity().getComponent().getClassName());
        }

        try (ActivityController<LoginActivity> loginController = Robolectric.buildActivity(LoginActivity.class).setup()) {
            LoginActivity login = loginController.get();
            ((TextInputEditText) login.findViewById(R.id.name)).setText("screen student");
            ((TextInputEditText) login.findViewById(R.id.roll)).setText("screen001");
            login.findViewById(R.id.submit).performClick();
            until(() -> shadowOf(login).peekNextStartedActivity() != null);
            assertEquals(DashboardActivity.class.getName(), shadowOf(login).getNextStartedActivity().getComponent().getClassName());
        }
    }

    private static void until(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(20);
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue("Timed out waiting for screen state", condition.getAsBoolean());
    }
}
