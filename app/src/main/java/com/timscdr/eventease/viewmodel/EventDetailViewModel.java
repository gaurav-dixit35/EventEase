package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import java.util.function.LongSupplier;

public final class EventDetailViewModel extends ViewModel {
    public static final class State {
        public final Event event;
        public final boolean loading, registered, started;
        public final Integer available;
        State(Event event, boolean loading, boolean registered, boolean started, Integer available) {
            this.event = event; this.loading = loading; this.registered = registered;
            this.started = started; this.available = available;
        }
    }
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final LongSupplier clock;
    private Event event;
    private Integer count;
    private boolean eventLoaded, registrationLoaded, registered;

    public EventDetailViewModel(EventRepository events, RegistrationRepository tickets,
            long eventId, long studentId, LongSupplier clock) {
        this.clock = clock;
        state.addSource(events.observeById(eventId), value -> { event = value; eventLoaded = true; publish(); });
        state.addSource(tickets.observeForEventAndStudent(eventId, studentId), value -> {
            registered = value != null; registrationLoaded = true; publish();
        });
        state.addSource(tickets.observeCountForEvent(eventId), value -> { count = value; publish(); });
        publish();
    }
    public LiveData<State> state() { return state; }
    public void tick() { publish(); }
    public long nextTickDelay() {
        return event == null || event.dateTime < clock.getAsLong() ? 60_000
                : Math.max(1, Math.min(60_000, event.dateTime - clock.getAsLong() + 1));
    }
    private void publish() {
        boolean loading = !eventLoaded || (event != null && (!registrationLoaded || count == null));
        Integer available = event == null || count == null ? null : Math.max(0, event.capacity - count);
        state.setValue(new State(event, loading, registered,
                event != null && event.dateTime < clock.getAsLong(), available));
    }
}
