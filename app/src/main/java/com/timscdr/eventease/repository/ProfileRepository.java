package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Profile persistence only; authentication/session policy belongs to the next slice. */
public interface ProfileRepository {
    LiveData<StudentProfile> observeById(long studentId);
    /** Completes with null if the profile is absent. */
    CompletableFuture<StudentProfile> findById(long studentId);
    CompletableFuture<List<StudentProfile>> findByRollNo(String rollNo);
    /** Returns the generated ID without mutating the input. New records must have ID 0. */
    CompletableFuture<Long> insert(StudentProfile profile);
    CompletableFuture<Void> update(StudentProfile profile);
}
