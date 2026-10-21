package com.timscdr.eventease.viewmodel;

import android.util.Patterns;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.AuthRepository;
import com.timscdr.eventease.repository.RepositoryException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class AuthViewModel extends ViewModel {
    private final AuthRepository repository;
    private final MutableLiveData<AuthState> state = new MutableLiveData<>(AuthState.of(AuthState.Status.IDLE));
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean cleared;

    public AuthViewModel(AuthRepository repository) { this.repository = repository; }
    public LiveData<AuthState> state() { return state; }

    public void restoreSession() { run(repository::restoreSession); }

    public void signIn(String name, String roll) {
        if (busy.get()) return;
        if (clean(name).isEmpty()) { invalid(AuthState.Error.NAME); return; }
        if (clean(roll).isEmpty()) { invalid(AuthState.Error.ROLL); return; }
        run(() -> repository.signIn(clean(name), clean(roll)));
    }

    public void signUp(String name, String roll, String course, String year, String email) {
        signUp(name, roll, course, year, email, "TIMSCDR");
    }

    public void signUp(String name, String roll, String course, String year, String email, String college) {
        if (busy.get()) return;
        if (clean(name).isEmpty()) { invalid(AuthState.Error.NAME); return; }
        if (clean(roll).isEmpty()) { invalid(AuthState.Error.ROLL); return; }
        if (clean(course).isEmpty()) { invalid(AuthState.Error.COURSE); return; }
        int parsedYear;
        try {
            parsedYear = Integer.parseInt(clean(year));
            if (parsedYear < 1) throw new NumberFormatException();
        } catch (NumberFormatException error) { invalid(AuthState.Error.YEAR); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(clean(email)).matches()) {
            invalid(AuthState.Error.EMAIL); return;
        }
        if (clean(college).isEmpty()) { invalid(AuthState.Error.COLLEGE); return; }
        StudentProfile profile = new StudentProfile();
        profile.name = clean(name);
        profile.rollNo = clean(roll);
        profile.course = clean(course);
        profile.year = parsedYear;
        profile.email = clean(email);
        profile.collegeName = clean(college);
        run(() -> repository.signUp(profile));
    }

    public void logout() { run(() -> repository.logout().thenApply(ignored -> null)); }

    private void run(Supplier<CompletableFuture<StudentProfile>> operation) {
        if (!busy.compareAndSet(false, true)) return;
        state.setValue(AuthState.of(AuthState.Status.LOADING));
        operation.get().whenComplete((profile, error) -> {
            if (!cleared) {
                if (error == null) {
                    state.postValue(profile == null ? AuthState.of(AuthState.Status.SIGNED_OUT)
                            : new AuthState(AuthState.Status.SIGNED_IN, AuthState.Error.NONE, profile.id));
                } else {
                    while (error.getCause() != null && !(error instanceof RepositoryException)) {
                        error = error.getCause();
                    }
                    AuthState.Error reason = AuthState.Error.STORAGE;
                    if (error instanceof RepositoryException) {
                        RepositoryException.Reason code = ((RepositoryException) error).getReason();
                        if (code == RepositoryException.Reason.AUTH_FAILED) reason = AuthState.Error.AUTH_FAILED;
                        if (code == RepositoryException.Reason.ALREADY_EXISTS) reason = AuthState.Error.DUPLICATE;
                    }
                    state.postValue(new AuthState(AuthState.Status.ERROR, reason, 0));
                }
            }
            busy.set(false);
        });
    }

    private void invalid(AuthState.Error error) {
        state.setValue(new AuthState(AuthState.Status.ERROR, error, 0));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    @Override protected void onCleared() { cleared = true; }
}
