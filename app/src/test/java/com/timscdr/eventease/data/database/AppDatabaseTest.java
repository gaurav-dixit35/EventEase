package com.timscdr.eventease.data.database;

import static org.junit.Assert.*;

import android.database.sqlite.SQLiteConstraintException;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AppDatabaseTest {
    @Rule public InstantTaskExecutorRule liveDataRule = new InstantTaskExecutorRule();
    private AppDatabase db;
    private long studentId;

    @Before public void setUp() {
        // Main-thread access is enabled only for this disposable test database.
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries().build();
        StudentProfile profile = new StudentProfile();
        profile.name = "Student One";
        profile.rollNo = "001";
        profile.course = "MCA";
        profile.year = 1;
        profile.email = "student@example.edu";
        studentId = db.profileDao().insert(profile);
    }

    @After public void tearDown() {
        db.close();
    }

    @Test public void profileAndEventRoundTripPreservesData() {
        StudentProfile profile = db.profileDao().findById(studentId);
        assertNotNull(profile);
        assertEquals("001", profile.rollNo);
        assertEquals(1, profile.year);
        profile.year = 2;
        assertEquals(1, db.profileDao().update(profile));
        assertEquals(2, db.profileDao().findByRollNo("001").get(0).year);
        long eventId = event("Workshop", 1234567890000L);
        Event stored = db.eventDao().findById(eventId);
        assertNotNull(stored);
        assertEquals(1234567890000L, stored.dateTime);
        assertNull(stored.bannerImageUrl);
    }

    @Test public void registrationRequiresBothParents() {
        long eventId = event("Workshop", 100L);
        assertThrows(SQLiteConstraintException.class,
                () -> db.registrationDao().insert(registration(eventId + 100, studentId, "bad-event")));
        assertThrows(SQLiteConstraintException.class,
                () -> db.registrationDao().insert(registration(eventId, studentId + 100, "bad-student")));
        assertEquals(0, db.registrationDao().countForEvent(eventId));
    }

    @Test public void duplicateCodeAndDuplicateStudentEventAreRejectedWithoutReplacingTicket() {
        long first = event("First", 100L);
        long second = event("Second", 200L);
        long id = db.registrationDao().insert(registration(first, studentId, "ticket"));
        assertThrows(SQLiteConstraintException.class,
                () -> db.registrationDao().insert(registration(second, studentId, "ticket")));
        assertThrows(SQLiteConstraintException.class,
                () -> db.registrationDao().insert(registration(first, studentId, "another-ticket")));
        assertEquals(id, db.registrationDao().findByCode("ticket").id);
        assertEquals(1, db.registrationDao().countForEvent(first));
        assertEquals(0, db.registrationDao().countForEvent(second));
    }

    @Test public void checkInCanSucceedOnlyOnceAndCountsAttendance() throws Exception {
        long eventId = event("Workshop", 100L);
        db.registrationDao().insert(registration(eventId, studentId, "ticket"));
        assertEquals(Integer.valueOf(0), value(db.registrationDao().observeAttendanceCount(studentId)));
        assertEquals(1, db.registrationDao().markCheckedIn("ticket"));
        assertEquals(0, db.registrationDao().markCheckedIn("ticket"));
        assertEquals(0, db.registrationDao().markCheckedIn("unknown"));
        assertTrue(db.registrationDao().findByCode("ticket").checkedIn);
        assertEquals(Integer.valueOf(1), value(db.registrationDao().observeAttendanceCount(studentId)));
        assertEquals(Integer.valueOf(1), value(db.registrationDao().observeRegistrationCount(studentId)));
    }

    @Test public void timeBoundaryAndStudentIsolationAreCorrect() throws Exception {
        long past = event("Past", 99L);
        long boundary = event("Now", 100L);
        long future = event("Future", 101L);
        event("Unregistered", 102L);
        db.registrationDao().insert(registration(past, studentId, "past"));
        db.registrationDao().insert(registration(boundary, studentId, "now"));
        db.registrationDao().insert(registration(future, studentId, "future"));
        List<Event> upcoming = value(db.eventDao().observeUpcomingForStudent(studentId, 100L));
        assertEquals(2, upcoming.size());
        assertEquals(boundary, upcoming.get(0).id);
        assertEquals(future, upcoming.get(1).id);
        List<Event> previous = value(db.eventDao().observePastForStudent(studentId, 100L));
        assertEquals(1, previous.size());
        assertEquals(past, previous.get(0).id);
        assertTrue(value(db.eventDao().observeUpcomingForStudent(studentId + 100, 100L)).isEmpty());
    }

    @Test public void filtersCombineAndSearchUsesLiteralCharacters() throws Exception {
        long selected = event("Learn 100% Java", 100L);
        event("Learn Java", 101L);
        Event cultural = new Event();
        cultural.title = "Learn 100% Dance";
        cultural.category = "Cultural";
        cultural.dateTime = 102L;
        db.eventDao().insert(cultural);
        List<Event> matches = value(db.eventDao().observeUpcoming(100L, "Workshop", "100% jAvA"));
        assertEquals(1, matches.size());
        assertEquals(selected, matches.get(0).id);
        assertEquals(3, value(db.eventDao().observeUpcoming(100L, null, "")).size());
        assertTrue(value(db.eventDao().observeUpcoming(100L, null, "_")).isEmpty());
    }

    @Test public void parentUpdatesPreserveTicketsAndParentDeletesAreRestricted() {
        long eventId = event("Workshop", 100L);
        db.registrationDao().insert(registration(eventId, studentId, "ticket"));
        Event updated = db.eventDao().findById(eventId);
        updated.title = "Updated";
        assertEquals(1, db.eventDao().update(updated));
        assertNotNull(db.registrationDao().findByCode("ticket"));
        assertThrows(SQLiteConstraintException.class, () -> db.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM Event WHERE id = ?", new Object[]{eventId}));
        assertThrows(SQLiteConstraintException.class, () -> db.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM StudentProfile WHERE id = ?", new Object[]{studentId}));
    }

    private long event(String title, long time) {
        Event event = new Event();
        event.title = title;
        event.category = "Workshop";
        event.dateTime = time;
        event.venue = "Auditorium";
        event.organizerName = "TIMSCDR";
        event.capacity = 50;
        return db.eventDao().insert(event);
    }

    private Registration registration(long eventId, long profileId, String code) {
        Registration registration = new Registration();
        registration.eventId = eventId;
        registration.studentId = profileId;
        registration.registrationCode = code;
        registration.registeredAt = 50L;
        return registration;
    }

    private static <T> T value(LiveData<T> source) throws InterruptedException {
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch ready = new CountDownLatch(1);
        Observer<T> observer = data -> {
            result.set(data);
            ready.countDown();
        };
        source.observeForever(observer);
        try {
            assertTrue("LiveData did not emit within 5 seconds", ready.await(5, TimeUnit.SECONDS));
            return result.get();
        } finally {
            source.removeObserver(observer);
        }
    }
}
