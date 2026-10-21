package com.timscdr.eventease.repository;

import android.util.Log;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.concurrent.CompletableFuture;

final class SyncingAuthRepository implements AuthRepository {
    private static final String TAG = "EventEase";
    private final AuthRepository local;
    private final StudentProfilePublisher profiles;
    SyncingAuthRepository(AuthRepository local, StudentProfilePublisher profiles) {
        this.local = local; this.profiles = profiles;
    }
    @Override public CompletableFuture<StudentProfile> signIn(String name, String rollNo) {
        return local.signIn(name, rollNo).thenApply(this::publishInBackground);
    }
    @Override public CompletableFuture<StudentProfile> signUp(StudentProfile profile) {
        return local.signUp(profile).thenApply(this::publishInBackground);
    }
    @Override public CompletableFuture<StudentProfile> restoreSession() {
        // Session restoration is an offline startup path. Never hold the first screen
        // open while Firebase is starting, unavailable, or waiting for a network call.
        return local.restoreSession().thenApply(profile -> {
            if (profile != null) {
                publishInBackground(profile);
            }
            return profile;
        });
    }
    @Override public CompletableFuture<Void> logout() { return local.logout(); }
    private StudentProfile publishInBackground(StudentProfile profile) {
        try {
            profiles.publish(profile).whenComplete((unused, error) -> {
                if (error != null) Log.w(TAG, "Student profile saved locally; cloud sync will retry on the next login or registration.", error);
            });
        } catch (RuntimeException error) {
            Log.w(TAG, "Student profile saved locally; cloud sync will retry on the next login or registration.", error);
        }
        return profile;
    }
}
