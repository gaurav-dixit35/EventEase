package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.repository.RegistrationRepository;

public final class TicketViewModel extends ViewModel {
    public static final class State {
        public final Event event; public final StudentProfile profile; public final Registration registration;
        public final boolean loading, unavailable;
        State(Event event, StudentProfile profile, Registration registration, boolean loading, boolean unavailable) {
            this.event = event; this.profile = profile; this.registration = registration; this.loading = loading;
            this.unavailable = unavailable;
        }
    }
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private Event event; private StudentProfile profile; private Registration registration;
    private boolean eventLoaded, profileLoaded, registrationLoaded;
    private final long eventId, studentId;
    public TicketViewModel(EventRepository events, ProfileRepository profiles, RegistrationRepository registrations,
            long eventId, long studentId, long registrationId) {
        this.eventId = eventId;
        this.studentId = studentId;
        state.addSource(events.observeById(eventId), value -> { event = value; eventLoaded = true; publish(); });
        state.addSource(profiles.observeById(studentId), value -> { profile = value; profileLoaded = true; publish(); });
        state.addSource(registrations.observeById(registrationId), value -> {
            registration = value; registrationLoaded = true; publish();
        });
        publish();
    }
    public LiveData<State> state() { return state; }
    private void publish() {
        boolean loading = !eventLoaded || !profileLoaded || !registrationLoaded;
        boolean unavailable = !loading && (event == null || profile == null || registration == null
                || event.id != eventId || profile.id != studentId
                || registration.eventId != eventId || registration.studentId != studentId);
        state.setValue(new State(event, profile, registration, loading, unavailable));
    }
}
