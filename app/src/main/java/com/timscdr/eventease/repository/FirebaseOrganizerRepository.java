package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.timscdr.eventease.data.entity.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Organizer scoped CRM reads. Firestore rules also scope every query to the signed-in UID. */
final class FirebaseOrganizerRepository implements OrganizerRepository {
    private final FirebaseFirestore database;
    private final FirebaseAuth auth;
    private final EventRepository events;
    FirebaseOrganizerRepository(FirebaseFirestore database, FirebaseAuth auth, EventRepository events) {
        this.database = database; this.auth = auth; this.events = events;
    }
    @Override public LiveData<List<OrganizerEventSummary>> observeEvents() {
        String uid = uid();
        if (uid == null) return new androidx.lifecycle.MutableLiveData<>(Collections.emptyList());
        return new FirebaseSnapshotLiveData<>((value, error) -> database.collection("events")
                .whereEqualTo("organizerUid", uid).addSnapshotListener((snapshot, failure) -> {
                    if (failure != null) { error.accept(failure); return; }
                    List<OrganizerEventSummary> result = new ArrayList<>();
                    if (snapshot != null) for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!Boolean.TRUE.equals(doc.getBoolean("archived"))) result.add(FirebaseEventRepository.summary(doc));
                    }
                    result.sort((left, right) -> Long.compare(left.event.dateTime, right.event.dateTime));
                    value.accept(result);
                }), Collections.emptyList());
    }
    @Override public LiveData<OrganizerStats> observeStats() {
        return Transformations.map(observeEvents(), events -> {
            int registrations = 0, checkedIn = 0;
            if (events != null) for (OrganizerEventSummary event : events) {
                registrations += event.registeredCount;
                checkedIn += event.checkedInCount;
            }
            return new OrganizerStats(events == null ? 0 : events.size(), registrations, checkedIn);
        });
    }
    @Override public LiveData<List<AttendeeRecord>> observeAttendees(long eventId) {
        String uid = uid();
        if (uid == null) return new androidx.lifecycle.MutableLiveData<>(Collections.emptyList());
        Query query = database.collection("registrations").whereEqualTo("organizerUid", uid)
                .whereEqualTo("eventId", eventId).orderBy("registeredAt", Query.Direction.ASCENDING);
        return new FirebaseSnapshotLiveData<>((value, error) -> query.addSnapshotListener((snapshot, failure) -> {
            if (failure != null) { error.accept(failure); return; }
            List<AttendeeRecord> result = new ArrayList<>();
            if (snapshot != null) for (DocumentSnapshot doc : snapshot.getDocuments()) {
                result.add(new AttendeeRecord(doc.getString("registrationCode"), safe(doc.getString("studentName")),
                        safe(doc.getString("rollNo")), safe(doc.getString("email")), safe(doc.getString("collegeName")),
                        value(doc.getLong("registeredAt")), Boolean.TRUE.equals(doc.getBoolean("checkedIn"))));
            }
            value.accept(result);
        }), Collections.emptyList());
    }
    @Override public CompletableFuture<Void> deleteEvent(long eventId) { return events.delete(eventId); }
    private String uid() {
        return auth.getCurrentUser() == null || auth.getCurrentUser().isAnonymous()
                ? null : auth.getCurrentUser().getUid();
    }
    private static String safe(String value) { return value == null ? "" : value; }
    private static long value(Long value) { return value == null ? 0 : value; }
}
