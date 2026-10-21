package com.timscdr.eventease.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import com.timscdr.eventease.ui.auth.LoginActivity;
import com.timscdr.eventease.ui.splash.SplashActivity;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SplashStartupTest {
    @Before public void resetApplication() throws Exception { RepositoryIsolation.reset(); }

    @Test public void freshInstallLeavesSplashForLoginAfterBrandDelay() throws Exception {
        try (ActivityController<SplashActivity> controller = Robolectric.buildActivity(SplashActivity.class).setup()) {
            shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS);
            SplashActivity splash = controller.get();
            until(() -> shadowOf(splash).peekNextStartedActivity() != null);
            Intent destination = shadowOf(splash).getNextStartedActivity();
            assertNotNull(destination);
            assertEquals(LoginActivity.class.getName(), destination.getComponent().getClassName());
        }
    }

    private static void until(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(20);
        }
        shadowOf(Looper.getMainLooper()).idle();
    }
}
