package com.timscdr.eventease.repository;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.util.EventImageEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Cloud event catalog. Firestore snapshot listeners propagate edits to every connected device. */
final class FirebaseEventRepository implements EventRepository {
    private final FirebaseFirestore database;
    private final FirebaseAuth auth;
    private final FirebaseFirestore organizerDatabase;
    private final FirebaseAuth organizerAuth;
    private final SharedPreferences organizerPreferences;

    FirebaseEventRepository(FirebaseFirestore database, FirebaseAuth auth,
            FirebaseFirestore organizerDatabase, FirebaseAuth organizerAuth,
            SharedPreferences organizerPreferences) {
        this.database = database; this.auth = auth;
        this.organizerDatabase = organizerDatabase; this.organizerAuth = organizerAuth;
        this.organizerPreferences = organizerPreferences;
    }

    @Override public CompletableFuture<List<Event>> findUpcoming(long now, @Nullable String category, String search) {
        return FirebaseTask.future(database.collection("events").get()).thenApply(snapshot ->
                filter(events(snapshot), now, category, search, false));
    }
    @Override public CompletableFuture<Integer> countAll() {
        return FirebaseTask.future(database.collection("events").get()).thenApply(snapshot -> snapshot.size());
    }
    /** Sample catalogs stay local; cloud mode lists only events published by organizers. */
    @Override public CompletableFuture<Void> loadSamples(long now) { return CompletableFuture.completedFuture(null); }
    @Override public boolean supportsSampleEvents() { return false; }
    @Override public LiveData<Event> observeById(long id) {
        return new FirebaseSnapshotLiveData<>((value, error) -> database.collection("events")
                .document(String.valueOf(id)).addSnapshotListener((snapshot, failure) -> {
                    if (failure != null) { error.accept(failure); return; }
                    value.accept(snapshot == null || !snapshot.exists() ? null : event(snapshot));
                }));
    }
    @Override public LiveData<List<Event>> observeUpcoming(long now, @Nullable String category, String search) {
        return new FirebaseSnapshotLiveData<>((value, error) -> database.collection("events")
                .addSnapshotListener((snapshot, failure) -> {
                    if (failure != null) { error.accept(failure); return; }
                    value.accept(snapshot == null ? Collections.emptyList()
                            : filter(events(snapshot), now, category, search, false));
                }), Collections.emptyList());
    }
    @Override public LiveData<List<Event>> observeUpcomingForStudent(long studentId, long now) {
        return observeStudentEvents(studentId, now, false);
    }
    @Override public LiveData<List<Event>> observePastForStudent(long studentId, long now) {
        return observeStudentEvents(studentId, now, true);
    }
    private LiveData<List<Event>> observeStudentEvents(long studentId, long now, boolean past) {
        return new FirebaseSnapshotLiveData<>((value, error) -> {
            String uid = currentUid();
            AtomicBoolean cancelled = new AtomicBoolean();
            AtomicReference<ListenerRegistration> active = new AtomicReference<>();
            if (uid == null) {
                // A first sign-in creates the anonymous Firebase identity in the
                // background. Wait for it here so event listeners do not start
                // with an unauthenticated query and remain stuck on empty data.
                FirebaseEnvironment.ensureStudentIdentity().whenComplete((user, failure) -> {
                    if (cancelled.get()) return;
                    if (failure != null) {
                        error.accept(asException(failure));
                    } else if (user == null) {
                        error.accept(new IllegalStateException("Student cloud identity is unavailable."));
                    } else {
                        active.set(listenStudentEvents(user.getUid(), now, past, value, error));
                        if (cancelled.get()) {
                            ListenerRegistration current = active.getAndSet(null);
                            if (current != null) current.remove();
                        }
                    }
                });
            } else {
                active.set(listenStudentEvents(uid, now, past, value, error));
            }
            return new ListenerRegistration() {
                @Override public void remove() {
                    cancelled.set(true);
                    ListenerRegistration current = active.getAndSet(null);
                    if (current != null) current.remove();
                }
            };
        }, Collections.emptyList());
    }

    private ListenerRegistration listenStudentEvents(String uid, long now, boolean past,
            Consumer<List<Event>> value, Consumer<Exception> error) {
        StudentEventQueryState queryState = new StudentEventQueryState();
        Runnable emit = () -> {
            List<Event> result = queryState.snapshot(now, past);
            if (result != null) value.accept(result);
        };
        ListenerRegistration eventsListener = database.collection("events").addSnapshotListener((snap, failure) -> {
            if (failure != null) { error.accept(failure); return; }
            queryState.setEvents(snap == null ? Collections.emptyList() : events(snap)); emit.run();
        });
        // Match the Firestore ownership rule's studentUid constraint. A
        // rollKey-only query is rejected because rules evaluate possible
        // query results and cannot trust a locally cached rollKey value.
        Query ticketsQuery = database.collection("registrations").whereEqualTo("studentUid", uid);
        ListenerRegistration ticketsListener = ticketsQuery.addSnapshotListener((snap, failure) -> {
            if (failure != null) { error.accept(failure); return; }
            List<Long> ids = new ArrayList<>();
            if (snap != null) for (DocumentSnapshot doc : snap.getDocuments()) {
                Long eventId = doc.getLong("eventId");
                if (eventId != null) ids.add(eventId);
            }
            queryState.setRegisteredIds(ids); emit.run();
        });
        return new ListenerRegistration() {
            @Override public void remove() {
                queryState.close(); eventsListener.remove(); ticketsListener.remove();
            }
        };
    }

    private static Exception asException(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
    }
    @Override public CompletableFuture<Event> findById(long id) {
        return FirebaseTask.future(database.collection("events").document(String.valueOf(id)).get())
                .thenApply(snapshot -> snapshot.exists() ? event(snapshot) : null);
    }
    @Override public CompletableFuture<Long> insert(Event input) {
        String uid = currentOrganizerUid();
        if (uid == null) return failed(new IllegalStateException("Sign in to the organizer portal first."));
        return FirebaseIds.next(organizerDatabase, "event").thenCompose(id -> {
            Map<String, Object> values = values(input);
            values.put("id", id);
            values.put("organizerUid", uid);
            values.put("organizerName", input.organizerName.isEmpty() ? currentOrganizerNameValue() : input.organizerName);
            values.put("registeredCount", 0L);
            values.put("checkedInCount", 0L);
            values.put("archived", false);
            values.put("createdAt", System.currentTimeMillis());
            values.put("updatedAt", System.currentTimeMillis());
            return FirebaseTask.voidFuture(organizerDatabase.collection("events").document(String.valueOf(id)).set(values))
                    .thenApply(unused -> id);
        });
    }
    @Override public CompletableFuture<Void> update(Event input) {
        String uid = currentOrganizerUid();
        if (uid == null) return failed(new IllegalStateException("Organizer session expired."));
        DocumentReference ref = organizerDatabase.collection("events").document(String.valueOf(input.id));
        Map<String, Object> values = values(input);
        values.put("updatedAt", System.currentTimeMillis());
        return FirebaseTask.voidFuture(ref.update(values));
    }
    @Override public CompletableFuture<Void> delete(long id) {
        DocumentReference ref = organizerDatabase.collection("events").document(String.valueOf(id));
        return FirebaseTask.future(organizerDatabase.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(ref);
            if (!doc.exists()) throw new IllegalStateException("Event not found.");
            Long count = doc.getLong("registeredCount");
            if (count != null && count > 0) throw new IllegalStateException("Event has registrations and cannot be deleted.");
            transaction.delete(ref);
            return Boolean.TRUE;
        })).thenApply(unused -> null);
    }
    @Override public CompletableFuture<String> currentOrganizerName() {
        return CompletableFuture.completedFuture(currentOrganizerNameValue());
    }

    private Map<String, Object> values(Event event) {
        Map<String, Object> result = new HashMap<>();
        result.put("title", event.title); result.put("category", event.category);
        result.put("description", event.description); result.put("venue", event.venue);
        result.put("dateTime", event.dateTime); result.put("capacity", event.capacity);
        result.put("organizerName", event.organizerName);
        String image = event.bannerImageUrl;
        if (image != null && image.startsWith(EventImageEncoder.DATA_URI_PREFIX))
            result.put("bannerImageData", image.substring(EventImageEncoder.DATA_URI_PREFIX.length()));
        return result;
    }
    private static Event event(DocumentSnapshot doc) {
        Event result = new Event();
        Long id = doc.getLong("id"); result.id = id == null ? parseId(doc.getId()) : id;
        result.title = string(doc, "title"); result.category = string(doc, "category");
        result.description = string(doc, "description"); result.venue = string(doc, "venue");
        Long time = doc.getLong("dateTime"); result.dateTime = time == null ? 0 : time;
        String image = doc.getString("bannerImageData");
        // Ignore legacy hosted URLs; new shared photos are embedded within Firestore event documents.
        result.bannerImageUrl = image == null || image.isEmpty()
                ? null : EventImageEncoder.DATA_URI_PREFIX + image;
        Long capacity = doc.getLong("capacity"); result.capacity = capacity == null ? 0 : capacity.intValue();
        result.organizerName = string(doc, "organizerName");
        return result;
    }
    static OrganizerEventSummary summary(DocumentSnapshot doc) {
        Event event = event(doc);
        Long count = doc.getLong("registeredCount"), checked = doc.getLong("checkedInCount");
        return new OrganizerEventSummary(event, count == null ? 0 : count.intValue(), checked == null ? 0 : checked.intValue());
    }
    static List<Event> events(QuerySnapshot snapshot) {
        List<Event> result = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            if (Boolean.TRUE.equals(doc.getBoolean("archived"))) continue;
            Event event = event(doc);
            result.add(event);
        }
        result.sort((left, right) -> Long.compare(left.dateTime, right.dateTime));
        return result;
    }
    private static final class StudentEventQueryState {
        private List<Event> events;
        private List<Long> registeredIds;
        private boolean closed;

        synchronized void setEvents(List<Event> value) { events = value; }
        synchronized void setRegisteredIds(List<Long> value) { registeredIds = value; }
        synchronized void close() { closed = true; }

        synchronized List<Event> snapshot(long now, boolean past) {
            if (closed || events == null || registeredIds == null) return null;
            java.util.Set<Long> registered = new java.util.HashSet<>(registeredIds);
            List<Event> result = new ArrayList<>();
            for (Event row : events) {
                boolean inTime = past ? row.dateTime < now : row.dateTime >= now;
                if (inTime && registered.contains(row.id)) result.add(row);
            }
            if (past) Collections.reverse(result);
            return result;
        }
    }
    private static List<Event> filter(List<Event> values, long now, String category, String search, boolean past) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<Event> result = new ArrayList<>();
        for (Event event : values) {
            if ((past ? event.dateTime < now : event.dateTime >= now)
                    && (category == null || category.isEmpty() || event.category.equalsIgnoreCase(category))
                    && event.title.toLowerCase(Locale.ROOT).contains(query)) result.add(event);
        }
        return result;
    }
    private String currentUid() {
        return auth == null || auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }
    private String currentOrganizerUid() {
        return organizerAuth == null || organizerAuth.getCurrentUser() == null
                || organizerAuth.getCurrentUser().isAnonymous()
                ? null : organizerAuth.getCurrentUser().getUid();
    }
    private String currentOrganizerNameValue() {
        return organizerPreferences.getString("organizer_name", "Event Organizer");
    }
    private static String string(DocumentSnapshot doc, String key) {
        String value = doc.getString(key); return value == null ? "" : value;
    }
    private static long parseId(String value) { try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return 0; } }
    private static <T> CompletableFuture<T> failed(Throwable error) {
        CompletableFuture<T> result = new CompletableFuture<>(); result.completeExceptionally(error); return result;
    }
}
