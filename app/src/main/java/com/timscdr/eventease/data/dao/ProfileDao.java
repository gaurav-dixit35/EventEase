package com.timscdr.eventease.data.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;

@Dao
public interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(StudentProfile profile);

    @Update(onConflict = OnConflictStrategy.ABORT)
    int update(StudentProfile profile);

    @Query("SELECT * FROM StudentProfile WHERE id = :studentId")
    LiveData<StudentProfile> observeById(long studentId);

    @Nullable
    @Query("SELECT * FROM StudentProfile WHERE id = :studentId")
    StudentProfile findById(long studentId);

    // The supplied schema does not declare rollNo unique: return all matches.
    @Query("SELECT * FROM StudentProfile WHERE lower(trim(rollNo)) = lower(trim(:rollNo)) ORDER BY id")
    List<StudentProfile> findByRollNo(String rollNo);
}
