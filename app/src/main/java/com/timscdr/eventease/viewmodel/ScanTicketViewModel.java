package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScanTicketViewModel extends ViewModel {
    public enum Status { IDLE, CHECKING, VALID, ALREADY_USED, INVALID, ERROR }
    public static final class State {
        public final Status status; public final String studentName, rollNo, eventTitle, message;
        State(Status status, String studentName, String rollNo, String eventTitle, String message) {
            this.status = status; this.studentName = studentName; this.rollNo = rollNo;
            this.eventTitle = eventTitle; this.message = message;
        }
    }
    private final RegistrationRepository registrations;
    private final ProfileRepository profiles;
    private final EventRepository events;
    private final Executor main;
    private final long expectedEventId;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.IDLE, null, null, null, null));
    private volatile boolean cleared;

    public ScanTicketViewModel(RegistrationRepository registrations, ProfileRepository profiles,
            EventRepository events, Executor main) {
        this(registrations, profiles, events, 0, main);
    }
    public ScanTicketViewModel(RegistrationRepository registrations, ProfileRepository profiles,
            EventRepository events, long expectedEventId, Executor main) {
        this.registrations = registrations; this.profiles = profiles; this.events = events; this.main = main;
        this.expectedEventId = expectedEventId;
    }
    public LiveData<State> state() { return state; }

    public void validate(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty() || !busy.compareAndSet(false, true)) {
            if (code.isEmpty()) state.setValue(new State(Status.INVALID, null, null, null, "empty"));
            return;
        }
        state.setValue(new State(Status.CHECKING, null, null, null, null));
        registrations.findByCode(code).thenCompose(registration -> {
            if (registration == null) return java.util.concurrent.CompletableFuture.completedFuture(
                    (Object) new State(Status.INVALID, null, null, null, null));
            if (expectedEventId > 0 && registration.eventId != expectedEventId) {
                return java.util.concurrent.CompletableFuture.completedFuture((Object) new State(Status.INVALID, null, null, null, null));
            }
            if (registration.checkedIn) return alreadyUsed(registration);
            return events.findById(registration.eventId).thenCompose(event -> {
                if (event == null) return java.util.concurrent.CompletableFuture.completedFuture(
                        (Object) new Check(registration, null, null, null, null));
                if (registration.attendeeName != null && !registration.attendeeName.isEmpty()) {
                    return java.util.concurrent.CompletableFuture.completedFuture((Object) new Check(registration,
                            registration.attendeeName, registration.attendeeRollNo, event, null));
                }
                return profiles.findById(registration.studentId).thenApply(profile -> (Object) new Check(registration,
                        profile == null ? null : profile.name, profile == null ? null : profile.rollNo, event, profile));
            });
        }).thenCompose(result -> {
            if (result instanceof State) return java.util.concurrent.CompletableFuture.completedFuture((State) result);
            Check check = (Check) result;
            if (check.name == null || check.event == null) return java.util.concurrent.CompletableFuture.completedFuture(
                    new State(Status.INVALID, null, null, null, null));
            return registrations.markCheckedIn(check.registration.registrationCode).thenApply(marked -> marked
                    ? new State(Status.VALID, check.name, check.rollNo, check.event.title, null)
                    : new State(Status.ALREADY_USED, check.name, check.rollNo, check.event.title, null));
        }).whenComplete((result, failure) -> main.execute(() -> {
            if (!cleared) state.setValue(failure == null ? result : new State(Status.ERROR, null, null, null, null));
            busy.set(false);
        }));
    }

    private java.util.concurrent.CompletableFuture<Object> alreadyUsed(Registration registration) {
        if (registration.attendeeName != null && !registration.attendeeName.isEmpty()) {
            return events.findById(registration.eventId).thenApply(event -> (Object) new State(Status.ALREADY_USED,
                    registration.attendeeName, registration.attendeeRollNo, event == null ? null : event.title, null));
        }
        return profiles.findById(registration.studentId).thenCombine(events.findById(registration.eventId),
                (profile, event) -> (Object) new State(Status.ALREADY_USED, profile == null ? null : profile.name,
                        profile == null ? null : profile.rollNo, event == null ? null : event.title, null));
    }
    private static final class Check {
        final Registration registration; final String name, rollNo; final Event event; final StudentProfile profile;
        Check(Registration registration, String name, String rollNo, Event event, StudentProfile profile) {
            this.registration = registration; this.name = name; this.rollNo = rollNo; this.event = event; this.profile = profile;
        }
    }
    @Override protected void onCleared() { cleared = true; }
}
