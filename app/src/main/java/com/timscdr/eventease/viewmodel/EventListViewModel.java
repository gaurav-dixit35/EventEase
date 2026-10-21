package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;

public final class EventListViewModel extends ViewModel {
    private final EventRepository repository;
    private final SavedStateHandle saved;
    private final Executor main;
    private final LongSupplier clock;
    private final MediatorLiveData<EventListState> state = new MediatorLiveData<>();
    private List<Event> events = new ArrayList<>();
    private final Set<Long> registered = new HashSet<>();
    private int request;
    private boolean loading, error, catalogEmpty, cleared, seeding;

    public EventListViewModel(EventRepository events, RegistrationRepository tickets, long studentId,
            SavedStateHandle saved, Executor main, LongSupplier clock) {
        this.repository = events;
        this.saved = saved;
        this.main = main;
        this.clock = clock;
        state.addSource(tickets.observeForStudent(studentId), values -> {
            registered.clear();
            if (values != null) for (Registration registration : values) registered.add(registration.eventId);
            publish();
        });
        publish();
    }

    public LiveData<EventListState> state() { return state; }
    public boolean supportsSampleEvents() { return repository.supportsSampleEvents(); }
    public String search() { String value = saved.get("search"); return value == null ? "" : value; }
    public String category() { return saved.get("category"); }
    public boolean searchOpen() { return Boolean.TRUE.equals(saved.get("searchOpen")); }
    public void setSearchOpen(boolean open) { saved.set("searchOpen", open); }

    public void search(String query) {
        String next = query == null ? "" : query;
        if (!next.equals(search())) { saved.set("search", next); refresh(); }
    }

    public void category(String category) {
        if (!java.util.Objects.equals(category, category())) { saved.set("category", category); refresh(); }
    }

    public void refresh() {
        if (seeding || cleared) return;
        int token = ++request;
        loading = true;
        error = false;
        publish();
        repository.findUpcoming(clock.getAsLong(), category(), search().trim())
                .thenCombine(repository.countAll(), (rows, count) -> new Catalog(rows, count))
                .whenComplete((result, failure) -> main.execute(() -> {
                    if (cleared || token != request) return;
                    loading = false;
                    error = failure != null;
                    if (result != null) { events = result.events; catalogEmpty = result.count == 0; }
                    publish();
                }));
    }

    public void loadSamples() {
        if (loading || !catalogEmpty || cleared) return;
        seeding = true;
        loading = true;
        error = false;
        publish();
        repository.loadSamples(clock.getAsLong()).whenComplete((unused, failure) -> main.execute(() -> {
            if (cleared) return;
            seeding = false;
            loading = false;
            if (failure == null) refresh();
            else { error = true; publish(); }
        }));
    }

    /** Refresh at the nearest event-start boundary, or once per minute. */
    public long nextRefreshDelay() {
        long delay = 60_000;
        for (Event event : events) {
            long until = event.dateTime - clock.getAsLong() + 1;
            if (until > 0) delay = Math.min(delay, until);
        }
        return Math.max(1, delay);
    }

    private void publish() { state.setValue(new EventListState(events, registered, loading, error, catalogEmpty)); }
    @Override protected void onCleared() { cleared = true; request++; }

    private static class Catalog {
        final List<Event> events;
        final int count;
        Catalog(List<Event> events, int count) { this.events = events; this.count = count; }
    }
}
