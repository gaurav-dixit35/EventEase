package com.timscdr.eventease.ui;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.AppRepositories;
import com.timscdr.eventease.ui.dashboard.DashboardActivity;
import com.timscdr.eventease.ui.event.EventDetailActivity;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class EventBrowsingTest {
    @org.junit.Before public void resetApplication() throws Exception { RepositoryIsolation.reset(); }
    @Test public void browsingFiltersNavigationRecreationDetailsAndShareWork() throws Exception {
        AppRepositories repositories = AppRepositories.getInstance(RuntimeEnvironment.getApplication());
        StudentProfile student = new StudentProfile();
        student.name = "Event Tester"; student.rollNo = "BROWSE001"; student.course = "MCA";
        student.year = 1; student.email = "browse@example.edu";
        repositories.auth().signUp(student).get(5, TimeUnit.SECONDS);
        repositories.events().loadSamples(System.currentTimeMillis()).get(5, TimeUnit.SECONDS);
        Event workshop = repositories.events().findUpcoming(System.currentTimeMillis(), "Workshop", "").get().get(0);
        try (ActivityController<DashboardActivity> controller = Robolectric.buildActivity(DashboardActivity.class).setup()) {
            DashboardActivity dashboard = controller.get();
            until(() -> ready(dashboard, 5));
            assertNotNull(dashboard.findViewById(R.id.events_brand_logo));
            assertEquals(dashboard.getString(R.string.app_name),
                    ((TextView) dashboard.findViewById(R.id.events_brand_title)).getText().toString());
            dashboard.findViewById(R.id.category_workshop).performClick();
            until(() -> ready(dashboard, 1));
            ((MaterialToolbar) dashboard.findViewById(R.id.events_toolbar)).getMenu().performIdentifierAction(R.id.action_search, 0);
            SearchView search = dashboard.findViewById(R.id.event_search);
            search.setQuery("no-match", false);
            until(() -> ready(dashboard, 0));
            assertEquals(android.view.View.VISIBLE, dashboard.findViewById(R.id.empty_list).getVisibility());
            search.setQuery("Android", false);
            until(() -> ready(dashboard, 1));
            BottomNavigationView navigation = dashboard.findViewById(R.id.bottom_navigation);
            navigation.setSelectedItemId(R.id.nav_my_events);
            assertNotNull(dashboard.findViewById(R.id.my_events_list));
            navigation.setSelectedItemId(R.id.nav_profile);
            until(() -> "Event Tester".contentEquals(((TextView) dashboard.findViewById(R.id.student_name)).getText()));
            navigation.setSelectedItemId(R.id.nav_events);
            until(() -> ready(dashboard, 1));
            controller.recreate();
            DashboardActivity recreated = controller.get();
            until(() -> ready(recreated, 1));
            assertEquals("Android", ((SearchView) recreated.findViewById(R.id.event_search)).getQuery().toString());
            RecyclerView list = recreated.findViewById(R.id.event_list);
            until(() -> list.findViewHolderForAdapterPosition(0) != null);
            list.findViewHolderForAdapterPosition(0).itemView.performClick();
            Intent detail = shadowOf(recreated).getNextStartedActivity();
            assertEquals(workshop.id, detail.getLongExtra(EventDetailActivity.EVENT_ID, 0));
        }
        Intent open = new Intent(RuntimeEnvironment.getApplication(), EventDetailActivity.class)
                .putExtra(EventDetailActivity.EVENT_ID, workshop.id);
        try (ActivityController<EventDetailActivity> controller = Robolectric.buildActivity(EventDetailActivity.class, open).setup()) {
            EventDetailActivity detail = controller.get();
            until(() -> workshop.title.contentEquals(((TextView) detail.findViewById(R.id.detail_title)).getText()));
            assertTrue(detail.findViewById(R.id.register_now).isEnabled());
            ((MaterialToolbar) detail.findViewById(R.id.detail_toolbar)).getMenu().performIdentifierAction(R.id.action_share, 0);
            Intent chooser = shadowOf(detail).getNextStartedActivity();
            assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
            Intent share = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);
            assertEquals("text/plain", share.getType());
            assertTrue(share.getStringExtra(Intent.EXTRA_TEXT).contains(workshop.title));
            detail.findViewById(R.id.register_now).performClick();
            assertEquals(Integer.valueOf(0), repositories.registrations().countForEvent(workshop.id).get());
        }
    }

    private static boolean ready(DashboardActivity activity, int expected) {
        RecyclerView list = activity.findViewById(R.id.event_list);
        SwipeRefreshLayout swipe = activity.findViewById(R.id.swipe_refresh);
        return list != null && list.getAdapter() != null && list.getAdapter().getItemCount() == expected
                && swipe != null && !swipe.isRefreshing();
    }

    private static void until(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(20);
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue("Timed out waiting for browsing state", condition.getAsBoolean());
    }
}
