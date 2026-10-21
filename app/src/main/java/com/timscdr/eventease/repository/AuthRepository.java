package com.timscdr.eventease.repository;

import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.concurrent.CompletableFuture;

/** Local profile selection, not server-verified authentication. */
public interface AuthRepository {
    CompletableFuture<StudentProfile> signIn(String name, String rollNo);
    CompletableFuture<StudentProfile> signUp(StudentProfile profile);
    /** Null means signed out, including when the saved profile no longer exists. */
    CompletableFuture<StudentProfile> restoreSession();
    CompletableFuture<Void> logout();
}
