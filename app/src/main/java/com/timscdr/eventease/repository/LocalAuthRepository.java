package com.timscdr.eventease.repository;

import android.content.SharedPreferences;
import com.timscdr.eventease.data.database.AppDatabase;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class LocalAuthRepository implements AuthRepository {
    private static final String STUDENT_ID = "student_id";
    private final AppDatabase database;
    private final SharedPreferences session;
    private final Executor executor;

    LocalAuthRepository(AppDatabase database, SharedPreferences session, Executor executor) {
        this.database = database;
        this.session = session;
        this.executor = executor;
    }

    @Override public CompletableFuture<StudentProfile> signIn(String name, String rollNo) {
        return RepositoryTasks.submit(executor, () -> {
            List<StudentProfile> matches = database.profileDao().findByRollNo(clean(rollNo));
            if (clean(name).isEmpty() || clean(rollNo).isEmpty() || matches.size() != 1
                    || !matches.get(0).name.trim().equalsIgnoreCase(clean(name))) {
                throw new RepositoryException(RepositoryException.Reason.AUTH_FAILED,
                        "Name and roll number do not identify one profile.");
            }
            StudentProfile profile = matches.get(0);
            saveSession(profile.id);
            return profile;
        });
    }

    @Override public CompletableFuture<StudentProfile> signUp(StudentProfile input) {
        StudentProfile profile = EntitySnapshots.copy(input);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(profile.id);
            profile.name = clean(profile.name);
            profile.rollNo = clean(profile.rollNo).toUpperCase(Locale.ROOT);
            profile.course = clean(profile.course);
            profile.email = clean(profile.email);
            profile.collegeName = clean(profile.collegeName);
            if (profile.collegeName.isEmpty()) profile.collegeName = "TIMSCDR";
            if (profile.name.isEmpty() || profile.rollNo.isEmpty() || profile.course.isEmpty()
                    || profile.email.isEmpty() || profile.collegeName.isEmpty() || profile.year < 1) {
                throw new RepositoryException(RepositoryException.Reason.INVALID_INPUT,
                        "Complete all profile fields.");
            }
            // SQLite serializes this check-and-insert, without changing the supplied schema.
            database.runInTransaction(() -> {
                if (!database.profileDao().findByRollNo(profile.rollNo).isEmpty()) {
                    throw new RepositoryException(RepositoryException.Reason.ALREADY_EXISTS,
                            "A profile already uses this roll number.");
                }
                profile.id = database.profileDao().insert(profile);
            });
            saveSession(profile.id);
            return profile;
        });
    }

    @Override public CompletableFuture<StudentProfile> restoreSession() {
        return RepositoryTasks.submit(executor, () -> {
            long id = session.getLong(STUDENT_ID, 0);
            if (id == 0) return null;
            StudentProfile profile = database.profileDao().findById(id);
            if (profile == null) saveSession(0);
            return profile;
        });
    }

    @Override public CompletableFuture<Void> logout() {
        return RepositoryTasks.submit(executor, () -> {
            saveSession(0);
            return null;
        });
    }

    private void saveSession(long id) {
        SharedPreferences.Editor editor = session.edit();
        if (id == 0) editor.remove(STUDENT_ID); else editor.putLong(STUDENT_ID, id);
        if (!editor.commit()) {
            throw new RepositoryException(RepositoryException.Reason.STORAGE,
                    "Could not save the session. The profile remains available for sign-in.");
        }
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
