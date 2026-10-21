package com.timscdr.eventease.viewmodel;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.ViewModelStore;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.AuthRepository;
import com.timscdr.eventease.repository.RepositoryException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AuthViewModelTest {
    @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
    private FakeAuth repository;
    private AuthViewModel model;

    @Before public void setUp() { repository = new FakeAuth(); model = new AuthViewModel(repository); }

    @Test public void validatesAllSignupFieldsBeforeWriting() {
        model.signUp("", "1", "MCA", "1", "test@example.edu");
        assertEquals(AuthState.Error.NAME, model.state().getValue().error);
        model.signUp("Name", "", "MCA", "1", "test@example.edu");
        assertEquals(AuthState.Error.ROLL, model.state().getValue().error);
        model.signUp("Name", "1", "", "1", "test@example.edu");
        assertEquals(AuthState.Error.COURSE, model.state().getValue().error);
        model.signUp("Name", "1", "MCA", "0", "test@example.edu");
        assertEquals(AuthState.Error.YEAR, model.state().getValue().error);
        model.signUp("Name", "1", "MCA", "999999999999", "test@example.edu");
        assertEquals(AuthState.Error.YEAR, model.state().getValue().error);
        model.signUp("Name", "1", "MCA", "1", "not-email");
        assertEquals(AuthState.Error.EMAIL, model.state().getValue().error);
        assertEquals(0, repository.calls);
    }

    @Test public void ignoresDoubleSubmitAndReportsSuccess() {
        model.signUp(" Name ", " 1 ", " MCA ", "1", "test@example.edu");
        model.signUp("Name", "1", "MCA", "1", "test@example.edu");
        assertEquals(1, repository.calls);
        assertEquals("Name", repository.input.name);
        assertEquals(AuthState.Status.LOADING, model.state().getValue().status);
        StudentProfile profile = new StudentProfile(); profile.id = 42;
        repository.pending.complete(profile);
        assertEquals(AuthState.Status.SIGNED_IN, model.state().getValue().status);
        assertEquals(42, model.state().getValue().studentId);
    }

    @Test public void mapsRepositoryFailureAndAllowsRetry() {
        model.signIn("Name", "1");
        repository.pending.completeExceptionally(new RepositoryException(
                RepositoryException.Reason.AUTH_FAILED, "test"));
        assertEquals(AuthState.Error.AUTH_FAILED, model.state().getValue().error);
        repository.pending = new CompletableFuture<>();
        model.signIn("Name", "1");
        assertEquals(2, repository.calls);
        assertEquals(AuthState.Status.LOADING, model.state().getValue().status);
    }

    @Test public void sessionRestorationAndLogoutEmitSignedOut() {
        model.restoreSession();
        repository.pending.complete(null);
        assertEquals(AuthState.Status.SIGNED_OUT, model.state().getValue().status);
        model.logout();
        assertEquals(AuthState.Status.SIGNED_OUT, model.state().getValue().status);
    }

    @Test public void clearedViewModelDoesNotPublishLateResult() {
        ViewModelStore store = new ViewModelStore();
        store.put("auth", model);
        model.signIn("Name", "1");
        store.clear();
        repository.pending.complete(new StudentProfile());
        assertEquals(AuthState.Status.LOADING, model.state().getValue().status);
    }

    private static class FakeAuth implements AuthRepository {
        CompletableFuture<StudentProfile> pending = new CompletableFuture<>();
        StudentProfile input;
        int calls;
        @Override public CompletableFuture<StudentProfile> signIn(String name, String roll) { calls++; return pending; }
        @Override public CompletableFuture<StudentProfile> signUp(StudentProfile profile) { calls++; input = profile; return pending; }
        @Override public CompletableFuture<StudentProfile> restoreSession() { calls++; return pending; }
        @Override public CompletableFuture<Void> logout() { return CompletableFuture.completedFuture(null); }
    }
}
