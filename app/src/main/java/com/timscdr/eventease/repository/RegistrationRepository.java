package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RegistrationRepository {
    LiveData<Registration> observeById(long registrationId);
    LiveData<List<Registration>> observeForStudent(long studentId);
    LiveData<Registration> observeForEventAndStudent(long eventId, long studentId);
    LiveData<Integer> observeRegistrationCount(long studentId);
    LiveData<Integer> observeAttendanceCount(long studentId);
    /** Completes with null if the ticket is absent. */
    CompletableFuture<Registration> findByCode(String code);
    CompletableFuture<Integer> countForEvent(long eventId);
    LiveData<Integer> observeCountForEvent(long eventId);
    /** Caller supplies the unique code; insert never replaces an existing ticket. */
    CompletableFuture<Long> insert(Registration registration);
    /** Atomically inserts when seats remain; completes with -1 when full. */
    CompletableFuture<Long> registerIfCapacity(Registration registration, int capacity);
    default CompletableFuture<Long> registerIfCapacity(Registration registration, int capacity, StudentProfile profile) {
        return registerIfCapacity(registration, capacity);
    }
    /** True only for the first check-in; false for unknown or already-used tickets. */
    CompletableFuture<Boolean> markCheckedIn(String code);
}
