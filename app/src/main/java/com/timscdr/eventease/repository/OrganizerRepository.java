package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.entity.Event;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Cross-device organizer CRM contract. Its Firebase implementation is protected by Firestore rules. */
public interface OrganizerRepository {
    LiveData<List<OrganizerEventSummary>> observeEvents();
    LiveData<OrganizerStats> observeStats();
    LiveData<List<AttendeeRecord>> observeAttendees(long eventId);
    CompletableFuture<Void> deleteEvent(long eventId);
}
