package com.timscdr.eventease.repository;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Firestore registration store. Seat reservations and check-ins are atomic cloud transactions. */
final class FirebaseRegistrationRepository implements RegistrationRepository {
    private final FirebaseFirestore database;
    private final SharedPreferences cloudSession;
    private final FirebaseStudentProfileSync studentProfileSync;
    FirebaseRegistrationRepository(FirebaseFirestore database, SharedPreferences cloudSession,
            FirebaseStudentProfileSync studentProfileSync) {
        this.database = database; this.cloudSession = cloudSession;
        this.studentProfileSync = studentProfileSync;
    }

    @Override public LiveData<Registration> observeById(long id) {
        return observeStudent((user, value, error) -> database.collection("registrations")
                .whereEqualTo("id", id).whereEqualTo("studentUid", user.getUid()).limit(1)
                .addSnapshotListener((snapshot, failure) -> {
                    if (failure != null) { error.accept(failure); return; }
                    value.accept(snapshot == null || snapshot.isEmpty() ? null
                            : registration(snapshot.getDocuments().get(0), true));
                }), null);
    }
    @Override public LiveData<List<Registration>> observeForStudent(long studentId) {
        return studentQuery(studentId, null);
    }
    @Override public LiveData<Registration> observeForEventAndStudent(long eventId, long studentId) {
        return observeStudent((user, value, error) -> database.collection("registrations")
                .whereEqualTo("eventId", eventId).whereEqualTo("studentUid", user.getUid())
                .addSnapshotListener((snapshot, failure) -> {
                    if (failure != null) { error.accept(failure); return; }
                    value.accept(snapshot == null || snapshot.isEmpty() ? null
                            : registration(snapshot.getDocuments().get(0), true));
                }), null);
    }
    @Override public LiveData<Integer> observeRegistrationCount(long studentId) {
        return Transformations.map(studentQuery(studentId, null), rows -> rows == null ? 0 : rows.size());
    }
    @Override public LiveData<Integer> observeAttendanceCount(long studentId) {
        return Transformations.map(studentQuery(studentId, true), rows -> rows == null ? 0 : rows.size());
    }
    private LiveData<List<Registration>> studentQuery(long studentId, @Nullable Boolean checkedIn) {
        return observeStudent((user, value, error) -> {
            Query query = database.collection("registrations").whereEqualTo("studentUid", user.getUid());
            if (checkedIn != null) query = query.whereEqualTo("checkedIn", checkedIn);
            return query.addSnapshotListener((snapshot, failure) -> {
                if (failure != null) { error.accept(failure); return; }
                value.accept(snapshot == null ? Collections.emptyList() : registrations(snapshot, true));
            });
        }, Collections.emptyList());
    }

    private interface StudentListener<T> {
        ListenerRegistration listen(FirebaseUser user, Consumer<T> value, Consumer<Exception> error);
    }

    private <T> LiveData<T> observeStudent(StudentListener<T> listener, T errorFallback) {
        return new FirebaseSnapshotLiveData<>((value, error) -> {
            AtomicBoolean cancelled = new AtomicBoolean();
            AtomicReference<ListenerRegistration> active = new AtomicReference<>();
            FirebaseEnvironment.ensureStudentIdentity().whenComplete((user, failure) -> {
                if (cancelled.get()) return;
                if (failure != null) {
                    error.accept(asException(failure));
                    return;
                }
                if (user == null || !user.isAnonymous()) {
                    error.accept(new IllegalStateException("Student cloud identity is unavailable."));
                    return;
                }
                active.set(listener.listen(user, value, error));
                if (cancelled.get()) {
                    ListenerRegistration current = active.getAndSet(null);
                    if (current != null) current.remove();
                }
            });
            return new ListenerRegistration() {
                @Override public void remove() {
                    cancelled.set(true);
                    ListenerRegistration current = active.getAndSet(null);
                    if (current != null) current.remove();
                }
            };
        }, errorFallback);
    }
    @Override public CompletableFuture<Registration> findByCode(String code) {
        String clean = code == null ? "" : code.trim();
        return FirebaseTask.future(database.collection("registrations").document(clean).get())
                .thenApply(doc -> doc.exists() ? registration(doc, false) : null);
    }
    @Override public CompletableFuture<Integer> countForEvent(long eventId) {
        return FirebaseTask.future(database.collection("events").document(String.valueOf(eventId)).get())
                .thenApply(doc -> { Long count = doc.getLong("registeredCount"); return count == null ? 0 : count.intValue(); });
    }
    @Override public LiveData<Integer> observeCountForEvent(long eventId) {
        return new FirebaseSnapshotLiveData<>((value, error) -> database.collection("events")
                .document(String.valueOf(eventId)).addSnapshotListener((snapshot, failure) -> {
                    if (failure != null) { error.accept(failure); return; }
                    Long count = snapshot == null ? null : snapshot.getLong("registeredCount");
                    value.accept(count == null ? 0 : count.intValue());
                }), 0);
    }
    @Override public CompletableFuture<Long> insert(Registration registration) {
        return profileFromCloud().thenCompose(profile -> registerIfCapacity(registration, Integer.MAX_VALUE, profile));
    }
    @Override public CompletableFuture<Long> registerIfCapacity(Registration input, int ignoredCapacity) {
        return profileFromCloud().thenCompose(profile -> registerIfCapacity(input, ignoredCapacity, profile));
    }
    @Override public CompletableFuture<Long> registerIfCapacity(Registration input, int ignoredCapacity, StudentProfile profile) {
        if (profile == null || profile.id != input.studentId) return failed(new IllegalArgumentException("Student profile is unavailable."));
        return FirebaseEnvironment.ensureStudentIdentity().thenCompose(user ->
                studentProfileSync.publish(user, profile).thenCompose(unused -> {
            String rollKey = FirebaseKeys.rollKey(profile.rollNo);
            String lockId = input.eventId + "_" + rollKey;
            DocumentReference eventRef = database.collection("events").document(String.valueOf(input.eventId));
            DocumentReference studentRef = database.collection("students").document(user.getUid());
            DocumentReference registrationRef = database.collection("registrations").document(input.registrationCode);
            DocumentReference lockRef = database.collection("registrationLocks").document(lockId);
            DocumentReference countersRef = database.collection("system").document("ids");
            return FirebaseTask.future(database.runTransaction(transaction -> {
                DocumentSnapshot event = transaction.get(eventRef);
                DocumentSnapshot student = transaction.get(studentRef);
                DocumentSnapshot lock = transaction.get(lockRef);
                DocumentSnapshot code = transaction.get(registrationRef);
                DocumentSnapshot counters = transaction.get(countersRef);
                if (!event.exists()) throw new IllegalStateException("Event no longer exists.");
                if (!student.exists() || !rollKey.equals(student.getString("rollKey"))) {
                    throw new IllegalStateException("Student profile sync is unavailable. Sign in again and retry.");
                }
                if (lock.exists()) {
                    Long existingId = lock.getLong("registrationId");
                    if (existingId != null) return existingId;
                    throw new IllegalStateException("A registration already exists for this event.");
                }
                if (code.exists()) throw new IllegalStateException("Registration code collision. Please retry.");
                Long registered = event.getLong("registeredCount"), eventCapacity = event.getLong("capacity");
                long current = registered == null ? 0 : registered;
                long capacity = eventCapacity == null ? ignoredCapacity : eventCapacity;
                if (current >= capacity) return -1L;
                Long previous = counters.getLong("registration");
                long id = previous == null ? 1 : previous + 1;
                long now = input.registeredAt > 0 ? input.registeredAt : System.currentTimeMillis();
                Map<String, Object> row = new HashMap<>();
                row.put("id", id); row.put("eventId", input.eventId); row.put("studentId", profile.id);
                row.put("registrationCode", input.registrationCode); row.put("registeredAt", now);
                row.put("checkedIn", false); row.put("studentUid", user.getUid());
                row.put("studentName", profile.name); row.put("rollNo", profile.rollNo);
                row.put("email", profile.email); row.put("collegeName", profile.collegeName);
                row.put("rollKey", rollKey); row.put("organizerUid", event.getString("organizerUid"));
                row.put("eventTitle", event.getString("title"));
                transaction.set(registrationRef, row);
                Map<String, Object> lockValues = new HashMap<>();
                lockValues.put("eventId", input.eventId); lockValues.put("rollKey", rollKey);
                lockValues.put("studentUid", user.getUid()); lockValues.put("registrationId", id);
                lockValues.put("registrationCode", input.registrationCode);
                transaction.set(lockRef, lockValues);
                transaction.set(countersRef, Collections.singletonMap("registration", id),
                        com.google.firebase.firestore.SetOptions.merge());
                transaction.update(eventRef, "registeredCount", current + 1,
                        "lastRegistrationCode", input.registrationCode);
                return id;
            }));
        }));
    }
    @Override public CompletableFuture<Boolean> markCheckedIn(String code) {
        String clean = code == null ? "" : code.trim();
        DocumentReference ticketRef = database.collection("registrations").document(clean);
        return FirebaseTask.future(database.runTransaction(transaction -> {
            DocumentSnapshot ticket = transaction.get(ticketRef);
            if (!ticket.exists()) return false;
            Boolean checkedIn = ticket.getBoolean("checkedIn");
            if (Boolean.TRUE.equals(checkedIn)) return false;
            Long eventId = ticket.getLong("eventId");
            if (eventId == null) return false;
            DocumentReference eventRef = database.collection("events").document(String.valueOf(eventId));
            DocumentSnapshot event = transaction.get(eventRef);
            if (!event.exists()) return false;
            Long count = event.getLong("checkedInCount");
            transaction.update(ticketRef, "checkedIn", true, "checkedInAt", System.currentTimeMillis());
            transaction.update(eventRef, "checkedInCount", (count == null ? 0L : count) + 1L,
                    "lastCheckInRegistrationCode", clean);
            return true;
        }));
    }

    private CompletableFuture<StudentProfile> profileFromCloud() {
        return FirebaseEnvironment.ensureStudentIdentity().thenCompose(user -> FirebaseTask.future(
                database.collection("students").document(user.getUid()).get()).thenApply(doc -> {
                    if (!doc.exists()) return null;
                    StudentProfile profile = new StudentProfile();
                    Long id = doc.getLong("studentId"); profile.id = id == null ? 0 : id;
                    profile.name = string(doc, "name"); profile.rollNo = string(doc, "rollNo");
                    profile.email = string(doc, "email"); profile.collegeName = string(doc, "collegeName");
                    return profile;
                }));
    }
    private String rollKey() { return cloudSession.getString("student_roll_key", null); }
    private Registration registration(DocumentSnapshot doc, boolean currentStudent) {
        Registration result = new Registration();
        Long id = doc.getLong("id"), event = doc.getLong("eventId"), student = doc.getLong("studentId"), time = doc.getLong("registeredAt");
        result.id = id == null ? 0 : id; result.eventId = event == null ? 0 : event;
        String storedRollKey = doc.getString("rollKey");
        String localRollKey = rollKey();
        long localStudentId = cloudSession.getLong("student_id", 0);
        result.studentId = currentStudent && localStudentId > 0 && storedRollKey != null
                && storedRollKey.equals(localRollKey) ? localStudentId : student == null ? 0 : student;
        result.registrationCode = string(doc, "registrationCode"); result.registeredAt = time == null ? 0 : time;
        result.checkedIn = Boolean.TRUE.equals(doc.getBoolean("checkedIn"));
        result.attendeeName = doc.getString("studentName"); result.attendeeRollNo = doc.getString("rollNo");
        result.attendeeEmail = doc.getString("email"); result.attendeeCollegeName = doc.getString("collegeName");
        return result;
    }
    private List<Registration> registrations(QuerySnapshot snapshot, boolean currentStudent) {
        List<Registration> result = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) result.add(registration(doc, currentStudent));
        result.sort((a, b) -> Long.compare(b.registeredAt, a.registeredAt));
        return result;
    }
    private static String string(DocumentSnapshot doc, String key) {
        String value = doc.getString(key); return value == null ? "" : value;
    }
    private static <T> CompletableFuture<T> failed(Throwable error) {
        CompletableFuture<T> result = new CompletableFuture<>(); result.completeExceptionally(error); return result;
    }
    private static Exception asException(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
    }
}
