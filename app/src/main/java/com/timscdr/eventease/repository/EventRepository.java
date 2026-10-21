package com.timscdr.eventease.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.entity.Event;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Storage-independent contract. Future completions are not main-thread callbacks. */
public interface EventRepository {
    CompletableFuture<List<Event>> findUpcoming(long now, @Nullable String category, String search);
    CompletableFuture<Integer> countAll();
    /** Explicit demo action; does nothing if the catalog already contains events. */
    CompletableFuture<Void> loadSamples(long now);
    default boolean supportsSampleEvents() { return true; }
    LiveData<Event> observeById(long eventId);
    LiveData<List<Event>> observeUpcoming(long now, @Nullable String category, String search);
    LiveData<List<Event>> observeUpcomingForStudent(long studentId, long now);
    LiveData<List<Event>> observePastForStudent(long studentId, long now);
    /** Completes with null if the event is absent. */
    CompletableFuture<Event> findById(long eventId);
    /** Returns the generated ID without mutating the input. New records must have ID 0. */
    CompletableFuture<Long> insert(Event event);
    /** Fails with NOT_FOUND if no record has this ID. */
    CompletableFuture<Void> update(Event event);
    /** Deletes only when no tickets have been issued; implementations must keep ticket relations valid. */
    CompletableFuture<Void> delete(long eventId);
    default CompletableFuture<String> currentOrganizerName() {
        return CompletableFuture.completedFuture("Event Organizer");
    }
}
