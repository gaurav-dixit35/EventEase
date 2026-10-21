package com.timscdr.eventease.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class SyncingAuthRepositoryTest {
    @Test public void localSignupSucceedsWhenCloudSyncFails() {
        StudentProfile profile = profile();
        FakeAuthRepository local = new FakeAuthRepository(profile);
        AtomicInteger publishCalls = new AtomicInteger();
        StudentProfilePublisher publisher = ignored -> {
            publishCalls.incrementAndGet();
            return failed(new IllegalStateException("Anonymous sign-in is disabled"));
        };

        StudentProfile result = new SyncingAuthRepository(local, publisher).signUp(profile).join();

        assertSame(profile, result);
        assertEquals(1, publishCalls.get());
    }

    @Test public void localLoginSucceedsWhenCloudSyncFails() {
        StudentProfile profile = profile();
        FakeAuthRepository local = new FakeAuthRepository(profile);
        AtomicInteger publishCalls = new AtomicInteger();
        StudentProfilePublisher publisher = ignored -> {
            publishCalls.incrementAndGet();
            return failed(new IllegalStateException("Network unavailable"));
        };

        StudentProfile result = new SyncingAuthRepository(local, publisher).signIn(profile.name, profile.rollNo).join();

        assertSame(profile, result);
        assertEquals(1, publishCalls.get());
    }

    private static StudentProfile profile() {
        StudentProfile profile = new StudentProfile();
        profile.id = 4;
        profile.name = "Student Example";
        profile.rollNo = "MCA004";
        return profile;
    }

    private static <T> CompletableFuture<T> failed(Throwable failure) {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(failure);
        return result;
    }

    private static final class FakeAuthRepository implements AuthRepository {
        private final StudentProfile profile;
        FakeAuthRepository(StudentProfile profile) { this.profile = profile; }
        @Override public CompletableFuture<StudentProfile> signIn(String name, String rollNo) {
            return CompletableFuture.completedFuture(profile);
        }
        @Override public CompletableFuture<StudentProfile> signUp(StudentProfile input) {
            return CompletableFuture.completedFuture(profile);
        }
        @Override public CompletableFuture<StudentProfile> restoreSession() {
            return CompletableFuture.completedFuture(profile);
        }
        @Override public CompletableFuture<Void> logout() { return CompletableFuture.completedFuture(null); }
    }
}
