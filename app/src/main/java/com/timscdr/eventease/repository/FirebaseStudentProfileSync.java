package com.timscdr.eventease.repository;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Publishes the signed-in local student's attendee details for the organizer CRM. */
final class FirebaseStudentProfileSync implements StudentProfilePublisher {
    private final FirebaseFirestore database;
    private final android.content.SharedPreferences cloudSession;
    FirebaseStudentProfileSync(FirebaseFirestore database, android.content.SharedPreferences cloudSession) {
        this.database = database; this.cloudSession = cloudSession;
    }
    @Override public CompletableFuture<Void> publish(StudentProfile profile) {
        cacheProfile(profile);
        return FirebaseEnvironment.ensureStudentIdentity().thenCompose(user -> publish(user, profile));
    }

    CompletableFuture<Void> publish(FirebaseUser user, StudentProfile profile) {
        if (user == null || profile == null || user.isAnonymous() == false) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("A student session is required to sync this profile."));
            return failed;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("studentUid", user.getUid());
        values.put("studentId", profile.id);
        values.put("name", profile.name);
        values.put("rollNo", profile.rollNo);
        values.put("course", profile.course);
        values.put("year", profile.year);
        values.put("email", profile.email);
        values.put("collegeName", profile.collegeName);
        values.put("role", "student");
        String rollKey = FirebaseKeys.rollKey(profile.rollNo);
        values.put("rollKey", rollKey);
        cacheProfile(profile);
        return FirebaseTask.voidFuture(database.collection("students").document(user.getUid())
                .set(values, com.google.firebase.firestore.SetOptions.merge())).thenRun(() ->
                cacheProfile(profile));
    }

    private void cacheProfile(StudentProfile profile) {
        if (profile == null) return;
        cloudSession.edit().putLong("student_id", profile.id)
                .putString("student_roll_key", FirebaseKeys.rollKey(profile.rollNo)).apply();
    }
}
