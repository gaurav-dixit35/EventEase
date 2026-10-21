package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

public final class MyEventsViewModel extends ViewModel {
    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final long studentId;
    private final LongSupplier clock;
    private final MediatorLiveData<MyEventsState> state = new MediatorLiveData<>();
    private LiveData<List<Event>> upcomingSource, pastSource;
    private List<Event> upcoming = new ArrayList<>(), past = new ArrayList<>();
    private Map<Long, Long> registrationIds = new HashMap<>();
    private boolean registrationsLoaded, upcomingLoaded, pastLoaded;

    public MyEventsViewModel(EventRepository events, RegistrationRepository registrations,
            long studentId, LongSupplier clock) {
        this.events = events; this.registrations = registrations; this.studentId = studentId; this.clock = clock;
        state.addSource(registrations.observeForStudent(studentId), rows -> {
            registrationIds = new HashMap<>();
            if (rows != null) for (Registration row : rows) registrationIds.put(row.eventId, row.id);
            registrationsLoaded = true;
            publish();
        });
        bindEventSections();
        publish();
    }

    public LiveData<MyEventsState> state() { return state; }

    /** Rebinds time-sensitive queries when this tab resumes or an event crosses its start time. */
    public void refreshClock() {
        bindEventSections();
        publish();
    }

    public long nextRefreshDelay() {
        long now = clock.getAsLong(), delay = 60_000;
        for (Event event : upcoming) {
            long until = event.dateTime - now + 1;
            if (until > 0) delay = Math.min(delay, until);
        }
        return Math.max(1, delay);
    }

    private void bindEventSections() {
        if (upcomingSource != null) state.removeSource(upcomingSource);
        if (pastSource != null) state.removeSource(pastSource);
        long now = clock.getAsLong();
        upcomingLoaded = false;
        pastLoaded = false;
        upcomingSource = events.observeUpcomingForStudent(studentId, now);
        pastSource = events.observePastForStudent(studentId, now);
        state.addSource(upcomingSource, rows -> {
            upcoming = rows == null ? new ArrayList<>() : rows;
            upcomingLoaded = true;
            publish();
        });
        state.addSource(pastSource, rows -> {
            past = rows == null ? new ArrayList<>() : rows;
            pastLoaded = true;
            publish();
        });
    }

    private void publish() {
        state.setValue(new MyEventsState(upcoming, past, registrationIds,
                !registrationsLoaded || !upcomingLoaded || !pastLoaded));
    }
}
