package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.repository.OrganizerAccount;
import com.timscdr.eventease.repository.OrganizerAuthRepository;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OrganizerAuthViewModel extends ViewModel {
    public enum Status { IDLE, LOADING, SIGNED_IN, SIGNED_OUT, ERROR }
    public static final class State {
        public final Status status;
        public final OrganizerAccount account;
        public final String error;
        State(Status status, OrganizerAccount account, String error) {
            this.status = status; this.account = account; this.error = error;
        }
    }
    private final OrganizerAuthRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.IDLE, null, null));
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean cleared;
    public OrganizerAuthViewModel(OrganizerAuthRepository repository) { this.repository = repository; }
    public LiveData<State> state() { return state; }
    public boolean isConfigured() { return repository.isConfigured(); }
    public String currentName() { return repository.currentDisplayName(); }

    public void restoreSession() {
        if (!busy.compareAndSet(false, true)) return;
        state.setValue(new State(Status.LOADING, null, null));
        repository.restoreSession().whenComplete((account, failure) -> {
            if (!cleared) state.postValue(failure == null
                    ? new State(account == null ? Status.SIGNED_OUT : Status.SIGNED_IN, account, null)
                    : new State(Status.ERROR, null, "storage"));
            busy.set(false);
        });
    }
    public void signIn(String email, String password) {
        if (!repository.isConfigured()) {
            state.setValue(new State(Status.ERROR, null, "configuration")); return;
        }
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            state.setValue(new State(Status.ERROR, null, "credentials")); return;
        }
        if (!busy.compareAndSet(false, true)) return;
        state.setValue(new State(Status.LOADING, null, null));
        repository.signIn(email.trim(), password).whenComplete((account, failure) -> {
            if (!cleared) state.postValue(failure == null
                    ? new State(Status.SIGNED_IN, account, null)
                    : new State(Status.ERROR, null, "unauthorized"));
            busy.set(false);
        });
    }
    public void signOut() {
        if (!busy.compareAndSet(false, true)) return;
        repository.signOut().whenComplete((unused, failure) -> {
            if (!cleared) state.postValue(new State(failure == null ? Status.SIGNED_OUT : Status.ERROR,
                    null, failure == null ? null : "storage"));
            busy.set(false);
        });
    }
    @Override protected void onCleared() { cleared = true; }
}
