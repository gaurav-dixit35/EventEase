package com.timscdr.eventease.viewmodel;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Rule;
import org.junit.Test;

public class EventViewModelTest {
    @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

    @Test public void staleSearchResponseCannotReplaceNewerResults() {
        FakeEvents events = new FakeEvents();
        EventListViewModel model = list(events, new FakeTickets(), new SavedStateHandle());
        model.refresh();
        model.search("Java");
        events.queries.get(1).complete(Collections.singletonList(event(2, "Java")));
        events.queries.get(0).complete(Collections.singletonList(event(1, "Old")));
        assertEquals("Java", model.state().getValue().events.get(0).title);
        assertFalse(model.state().getValue().loading);
    }

    @Test public void filtersAreSavedAndFailuresCanBeRetried() {
        SavedStateHandle saved = new SavedStateHandle();
        FakeEvents events = new FakeEvents();
        EventListViewModel model = list(events, new FakeTickets(), saved);
        model.category("Workshop"); model.search("Java"); model.setSearchOpen(true);
        events.queries.get(1).completeExceptionally(new IllegalStateException("Read failed"));
        assertTrue(model.state().getValue().error);
        model.refresh();
        events.queries.get(2).complete(Collections.emptyList());
        assertFalse(model.state().getValue().error);
        EventListViewModel restored = list(events, new FakeTickets(), saved);
        assertEquals("Workshop", restored.category());
        assertEquals("Java", restored.search());
        assertTrue(restored.searchOpen());
    }

    @Test public void registrationUpdatesBadgeWithoutRequeryingEvents() {
        FakeEvents events = new FakeEvents(); FakeTickets tickets = new FakeTickets();
        EventListViewModel model = list(events, tickets, new SavedStateHandle());
        Observer<EventListState> observer = ignored -> { };
        model.state().observeForever(observer);
        try {
            model.refresh(); events.queries.get(0).complete(Collections.singletonList(event(9, "Test")));
            Registration ticket = new Registration(); ticket.eventId = 9;
            tickets.all.setValue(Collections.singletonList(ticket));
            assertTrue(model.state().getValue().registeredIds.contains(9L));
            assertEquals(1, events.queries.size());
        } finally { model.state().removeObserver(observer); }
    }

    @Test public void detailTracksSeatsRegistrationAndTimeBoundary() {
        FakeEvents events = new FakeEvents(); FakeTickets tickets = new FakeTickets();
        AtomicLong clock = new AtomicLong(100);
        EventDetailViewModel model = new EventDetailViewModel(events, tickets, 1, 1, clock::get);
        Observer<EventDetailViewModel.State> observer = ignored -> { };
        model.state().observeForever(observer);
        try {
            Event event = event(1, "Test"); event.dateTime = 100; event.capacity = 2;
            events.single.setValue(event); tickets.count.setValue(1); tickets.single.setValue(null);
            assertFalse(model.state().getValue().loading);
            assertFalse(model.state().getValue().started);
            assertEquals(Integer.valueOf(1), model.state().getValue().available);
            tickets.count.setValue(3);
            assertEquals(Integer.valueOf(0), model.state().getValue().available);
            tickets.single.setValue(new Registration());
            assertTrue(model.state().getValue().registered);
            clock.set(101); model.tick(); assertTrue(model.state().getValue().started);
            events.single.setValue(null);
            assertNull(model.state().getValue().event);
            assertFalse(model.state().getValue().loading);
        } finally { model.state().removeObserver(observer); }
    }

    private static EventListViewModel list(FakeEvents events, FakeTickets tickets, SavedStateHandle saved) {
        return new EventListViewModel(events, tickets, 1, saved, Runnable::run, () -> 100);
    }
    private static Event event(long id, String title) {
        Event event = new Event(); event.id = id; event.title = title; event.dateTime = 1000; return event;
    }
    private static class FakeEvents implements EventRepository {
        final List<CompletableFuture<List<Event>>> queries = new ArrayList<>();
        final MutableLiveData<Event> single = new MutableLiveData<>();
        @Override public CompletableFuture<List<Event>> findUpcoming(long now, String category, String search) {
            CompletableFuture<List<Event>> query = new CompletableFuture<>(); queries.add(query); return query;
        }
        @Override public CompletableFuture<Integer> countAll() { return CompletableFuture.completedFuture(1); }
        @Override public LiveData<Event> observeById(long id) { return single; }
        @Override public CompletableFuture<Void> loadSamples(long now) { throw new UnsupportedOperationException(); }
        @Override public LiveData<List<Event>> observeUpcoming(long now, String category, String search) { throw new UnsupportedOperationException(); }
        @Override public LiveData<List<Event>> observeUpcomingForStudent(long id, long now) { throw new UnsupportedOperationException(); }
        @Override public LiveData<List<Event>> observePastForStudent(long id, long now) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Event> findById(long id) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Long> insert(Event event) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> update(Event event) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> delete(long id) { throw new UnsupportedOperationException(); }
    }
    private static class FakeTickets implements RegistrationRepository {
        final MutableLiveData<List<Registration>> all = new MutableLiveData<>(Collections.emptyList());
        final MutableLiveData<Registration> single = new MutableLiveData<>();
        final MutableLiveData<Integer> count = new MutableLiveData<>();
        @Override public LiveData<List<Registration>> observeForStudent(long id) { return all; }
        @Override public LiveData<Registration> observeForEventAndStudent(long event, long student) { return single; }
        @Override public LiveData<Integer> observeCountForEvent(long id) { return count; }
        @Override public LiveData<Registration> observeById(long id) { throw new UnsupportedOperationException(); }
        @Override public LiveData<Integer> observeRegistrationCount(long id) { throw new UnsupportedOperationException(); }
        @Override public LiveData<Integer> observeAttendanceCount(long id) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Registration> findByCode(String code) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Integer> countForEvent(long id) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Long> insert(Registration item) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Long> registerIfCapacity(Registration item, int capacity) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Boolean> markCheckedIn(String code) { throw new UnsupportedOperationException(); }
    }
}
