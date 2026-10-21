package com.timscdr.eventease.repository;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.robolectric.Shadows.shadowOf;
import android.os.Looper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class FirebaseSnapshotLiveDataTest {
    @Rule public InstantTaskExecutorRule liveDataRule = new InstantTaskExecutorRule();

    @Test public void emitsSafeFallbackAndRetriesFailedListener() {
        FakeSource source = new FakeSource();
        FirebaseSnapshotLiveData<List<String>> data = new FirebaseSnapshotLiveData<>(source, Collections.emptyList());
        AtomicReference<List<String>> latest = new AtomicReference<>();
        Observer<List<String>> observer = latest::set;
        data.observeForever(observer);
        try {
            assertEquals(1, source.starts);
            source.failures.get(0).accept(new IllegalStateException("temporary permission race"));
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(Collections.emptyList(), latest.get());

            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
            assertEquals(2, source.starts);
            source.values.get(1).accept(Collections.singletonList("restored"));
            assertEquals(Collections.singletonList("restored"), latest.get());
        } finally {
            data.removeObserver(observer);
        }
        assertEquals(2, source.removals);
    }

    private static final class FakeSource implements FirebaseSnapshotLiveData.Source<List<String>> {
        final List<java.util.function.Consumer<List<String>>> values = new ArrayList<>();
        final List<java.util.function.Consumer<Exception>> failures = new ArrayList<>();
        int starts, removals;

        @Override public ListenerRegistration listen(java.util.function.Consumer<List<String>> value,
                java.util.function.Consumer<Exception> error) {
            starts++;
            values.add(value);
            failures.add(error);
            return () -> removals++;
        }
    }
}
