package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import com.timscdr.eventease.repository.SettingsRepository;
import com.timscdr.eventease.util.EventReminderScheduler;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executor;

public final class RegistrationViewModel extends ViewModel {
    public static final class State {
        public final Event event;
        public final StudentProfile profile;
        public final Registration registration;
        public final boolean loading, submitting, full, started, alreadyRegistered, error, success;
        State(Event event, StudentProfile profile, Registration registration, boolean loading,
                boolean submitting, boolean full, boolean started, boolean alreadyRegistered, boolean error, boolean success) {
            this.event = event; this.profile = profile; this.registration = registration;
            this.loading = loading; this.submitting = submitting; this.full = full;
            this.started = started; this.alreadyRegistered = alreadyRegistered;
            this.error = error; this.success = success;
        }
    }

    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Executor main;
    private final SettingsRepository settings;
    private final EventReminderScheduler reminderScheduler;
    private Event event;
    private StudentProfile profile;
    private Registration registration;
    private Integer count;
    private boolean eventLoaded, profileLoaded, registrationLoaded, submitting, full, error, success, cleared;

    public RegistrationViewModel(EventRepository events, ProfileRepository profiles,
            RegistrationRepository registrations, SettingsRepository settings, EventReminderScheduler reminderScheduler,
            long eventId, long studentId, Executor main) {
        this.events = events; this.registrations = registrations; this.settings = settings;
        this.reminderScheduler = reminderScheduler; this.main = main;
        state.addSource(events.observeById(eventId), value -> { event = value; eventLoaded = true; publish(); });
        state.addSource(profiles.observeById(studentId), value -> { profile = value; profileLoaded = true; publish(); });
        state.addSource(registrations.observeForEventAndStudent(eventId, studentId), value -> {
            registration = value; registrationLoaded = true; publish();
        });
        state.addSource(registrations.observeCountForEvent(eventId), value -> { count = value; publish(); });
        publish();
    }

    public LiveData<State> state() { return state; }

    public void submit() {
        if (!eventLoaded || !profileLoaded || !registrationLoaded || count == null
                || event == null || profile == null || registration != null
                || !busy.compareAndSet(false, true)) return;
        if (event.dateTime <= System.currentTimeMillis()) {
            busy.set(false);
            publish();
            return;
        }
        if (count >= event.capacity) {
            busy.set(false);
            full = true;
            publish();
            return;
        }
        submitting = true; error = false; publish();
        Registration ticket = new Registration();
        ticket.eventId = event.id;
        ticket.studentId = profile.id;
        ticket.registrationCode = "EVENTEASE-" + UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT);
        ticket.registeredAt = System.currentTimeMillis();
        registrations.registerIfCapacity(ticket, event.capacity, profile).whenComplete((id, failure) -> {
            main.execute(() -> {
                submitting = false;
                if (!cleared) {
                    if (failure != null) error = true;
                    else if (id == -1L) full = true;
                    else {
                        success = true;
                        try {
                            if (settings.notificationsEnabled()) reminderScheduler.schedule(ticket, event);
                        } catch (RuntimeException reminderFailure) {
                            Log.w("EventEase", "Registration saved, but its reminder could not be scheduled.", reminderFailure);
                        }
                    }
                    publish();
                }
                busy.set(false);
            });
        });
    }

    private void publish() {
        boolean loading = !eventLoaded || !profileLoaded || !registrationLoaded || count == null;
        state.setValue(new State(event, profile, registration, loading, submitting, full,
                event != null && event.dateTime <= System.currentTimeMillis(), registration != null, error, success));
    }
    @Override protected void onCleared() { cleared = true; }
}
