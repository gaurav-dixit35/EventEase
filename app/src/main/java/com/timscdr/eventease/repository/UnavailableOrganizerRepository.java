package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.timscdr.eventease.data.entity.Event;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class UnavailableOrganizerRepository implements OrganizerRepository {
    private final LiveData<List<OrganizerEventSummary>> events = new MutableLiveData<>(Collections.emptyList());
    private final LiveData<OrganizerStats> stats = new MutableLiveData<>(new OrganizerStats(0, 0, 0));
    private final LiveData<List<AttendeeRecord>> attendees = new MutableLiveData<>(Collections.emptyList());
    @Override public LiveData<List<OrganizerEventSummary>> observeEvents() { return events; }
    @Override public LiveData<OrganizerStats> observeStats() { return stats; }
    @Override public LiveData<List<AttendeeRecord>> observeAttendees(long eventId) { return attendees; }
    @Override public CompletableFuture<Void> deleteEvent(long eventId) { return failed(); }
    private static CompletableFuture<Void> failed() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        result.completeExceptionally(new IllegalStateException("Firebase is not configured."));
        return result;
    }
}
