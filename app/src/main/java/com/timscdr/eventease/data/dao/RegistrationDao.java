package com.timscdr.eventease.data.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.timscdr.eventease.data.entity.Registration;
import java.util.List;

@Dao
public interface RegistrationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Registration registration);

    /** Atomically reserves a seat; returns -1 when the event has reached capacity. */
    @Transaction
    default long insertIfCapacity(Registration registration, int capacity) {
        if (countForEvent(registration.eventId) >= capacity) return -1;
        return insert(registration);
    }

    @Query("SELECT * FROM Registration WHERE studentId = :studentId ORDER BY registeredAt DESC, id DESC")
    LiveData<List<Registration>> observeForStudent(long studentId);

    @Query("SELECT * FROM Registration WHERE eventId = :eventId AND studentId = :studentId")
    LiveData<Registration> observeForEventAndStudent(long eventId, long studentId);

    @Nullable
    @Query("SELECT * FROM Registration WHERE registrationCode = :code")
    Registration findByCode(String code);

    @Query("SELECT * FROM Registration WHERE id = :registrationId")
    LiveData<Registration> observeById(long registrationId);

    @Query("SELECT COUNT(*) FROM Registration WHERE eventId = :eventId")
    int countForEvent(long eventId);

    @Query("SELECT COUNT(*) FROM Registration WHERE eventId = :eventId")
    LiveData<Integer> observeCountForEvent(long eventId);

    @Query("SELECT COUNT(*) FROM Registration WHERE studentId = :studentId")
    LiveData<Integer> observeRegistrationCount(long studentId);

    @Query("SELECT COUNT(*) FROM Registration WHERE studentId = :studentId AND checkedIn = 1")
    LiveData<Integer> observeAttendanceCount(long studentId);

    /** Atomic state change: 1 means accepted; 0 means unknown or already used. */
    @Query("UPDATE Registration SET checkedIn = 1 WHERE registrationCode = :code AND checkedIn = 0")
    int markCheckedIn(String code);
}
