package com.timscdr.eventease.data.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.timscdr.eventease.data.entity.Event;
import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT COUNT(*) FROM Event")
    int countAll();

    @Query("SELECT * FROM Event WHERE dateTime >= :now "
            + "AND (:category IS NULL OR category = :category) "
            + "AND instr(lower(title), lower(:search)) > 0 ORDER BY dateTime, id")
    List<Event> findUpcoming(long now, @Nullable String category, String search);

    @androidx.room.Transaction
    default void insertSamplesIfEmpty(List<Event> samples) {
        if (countAll() == 0) {
            for (Event event : samples) insert(event);
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Event event);

    @Update(onConflict = OnConflictStrategy.ABORT)
    int update(Event event);

    @Query("DELETE FROM Event WHERE id = :eventId")
    int deleteById(long eventId);

    @Query("SELECT * FROM Event WHERE id = :eventId")
    LiveData<Event> observeById(long eventId);

    @Nullable
    @Query("SELECT * FROM Event WHERE id = :eventId")
    Event findById(long eventId);

    // null category means All. instr treats '%' and '_' as literal search text.
    @Query("SELECT * FROM Event WHERE dateTime >= :now "
            + "AND (:category IS NULL OR category = :category) "
            + "AND instr(lower(title), lower(:search)) > 0 ORDER BY dateTime, id")
    LiveData<List<Event>> observeUpcoming(long now, @Nullable String category, String search);

    @Query("SELECT e.* FROM Event e INNER JOIN Registration r ON e.id = r.eventId "
            + "WHERE r.studentId = :studentId AND e.dateTime >= :now ORDER BY e.dateTime, e.id")
    LiveData<List<Event>> observeUpcomingForStudent(long studentId, long now);

    @Query("SELECT e.* FROM Event e INNER JOIN Registration r ON e.id = r.eventId "
            + "WHERE r.studentId = :studentId AND e.dateTime < :now ORDER BY e.dateTime DESC, e.id DESC")
    LiveData<List<Event>> observePastForStudent(long studentId, long now);
}
