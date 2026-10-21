package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.function.Consumer;

/** Attaches Firestore listeners only while a screen observes the LiveData. */
final class FirebaseSnapshotLiveData<T> extends LiveData<T> {
    private static final String TAG = "EventEaseFirebase";
    private static final int MAX_RETRIES = 5;
    interface Source<T> {
        ListenerRegistration listen(Consumer<T> value, Consumer<Exception> error);
    }
    private final Source<T> source;
    private final T errorFallback;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ListenerRegistration registration;
    private volatile boolean active;
    private boolean retryPending;
    private int retries;
    private volatile int generation;
    private final Runnable resetRetryBudget = () -> retries = 0;
    private final Runnable retry = () -> {
        retryPending = false;
        attach();
    };

    FirebaseSnapshotLiveData(Source<T> source) { this(source, null); }
    FirebaseSnapshotLiveData(Source<T> source, T errorFallback) {
        this.source = source;
        this.errorFallback = errorFallback;
    }

    @Override protected void onActive() {
        active = true;
        retries = 0;
        attach();
    }

    @Override protected void onInactive() {
        active = false;
        generation++;
        retryPending = false;
        main.removeCallbacks(retry);
        main.removeCallbacks(resetRetryBudget);
        removeRegistration();
    }

    private void attach() {
        if (!active || registration != null) return;
        int token = ++generation;
        try {
            registration = source.listen(value -> {
                if (token == generation) {
                    postValue(value);
                    main.removeCallbacks(resetRetryBudget);
                    main.postDelayed(resetRetryBudget, 30_000);
                }
            }, error -> main.post(() -> failed(token, error)));
        } catch (RuntimeException error) {
            main.post(() -> failed(token, error));
        }
    }

    private void failed(int token, Exception error) {
        if (!active || token != generation) return;
        Log.e(TAG, "Firestore snapshot listener failed", error);
        main.removeCallbacks(resetRetryBudget);
        postValue(errorFallback);
        generation++;
        removeRegistration();
        if (retryPending || retries >= MAX_RETRIES) return;
        long delay = 1_000L << Math.min(retries, 4);
        retries++;
        retryPending = true;
        main.postDelayed(retry, delay);
    }

    private void removeRegistration() {
        ListenerRegistration current = registration;
        registration = null;
        if (current != null) current.remove();
    }
}
