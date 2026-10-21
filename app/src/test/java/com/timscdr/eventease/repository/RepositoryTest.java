package com.timscdr.eventease.repository;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import com.timscdr.eventease.data.database.AppDatabase;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
public class RepositoryTest {
    @Rule public InstantTaskExecutorRule liveDataRule = new InstantTaskExecutorRule();
    private AppDatabase db;
    private ExecutorService worker;
    private AppRepositories repositories;

    @Before public void setUp() {
        // Deliberately keep Room's main-thread protection enabled.
        worker = Executors.newSingleThreadExecutor();
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .setQueryExecutor(worker).build();
        repositories = new AppRepositories(db, worker, RuntimeEnvironment.getApplication()
                .getSharedPreferences("repository_test_session", 0));
    }

    @After public void tearDown() throws InterruptedException {
        worker.shutdown();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        db.close();
    }

    @Test public void writesAndReadsRunOffMainThreadWithoutMutatingInputs() throws Exception {
        StudentProfile input = profile();
        long studentId = await(repositories.profiles().insert(input));
        assertTrue(studentId > 0);
        assertEquals(0, input.id);
        StudentProfile stored = await(repositories.profiles().findById(studentId));
        assertEquals("001", stored.rollNo);
        stored.year = 2;
        await(repositories.profiles().update(stored));
        assertEquals(2, await(repositories.profiles().findByRollNo("001")).get(0).year);
        assertNull(await(repositories.events().findById(999)));
        assertNull(await(repositories.profiles().findById(999)));
    }

    @Test public void sampleCatalogIsOptInAndRepeatedLoadDoesNotDuplicateIt() throws Exception {
        assertEquals(Integer.valueOf(0), await(repositories.events().countAll()));
        long now = System.currentTimeMillis();
        await(repositories.events().loadSamples(now));
        await(repositories.events().loadSamples(now));
        assertEquals(Integer.valueOf(5), await(repositories.events().countAll()));
        List<Event> workshops = await(repositories.events().findUpcoming(now, "Workshop", "android"));
        assertEquals(1, workshops.size());
        assertTrue(workshops.get(0).title.startsWith("Sample:"));
        assertTrue(workshops.get(0).dateTime > now);
        assertTrue(await(repositories.events().findUpcoming(now, "Sports", "android")).isEmpty());
    }

    @Test public void sampleLoaderNeverChangesAnExistingCatalog() throws Exception {
        await(repositories.events().insert(event()));
        await(repositories.events().loadSamples(System.currentTimeMillis()));
        assertEquals(Integer.valueOf(1), await(repositories.events().countAll()));
    }

    @Test public void queuedWriteUsesSnapshotTakenAtCallTime() throws Exception {
        List<Runnable> queued = new ArrayList<>();
        EventRepository events = new RoomEventRepository(db.eventDao(), queued::add);
        Event input = event();
        CompletableFuture<Long> result = events.insert(input);
        input.title = "Changed after submit";
        input.id = 900;
        assertFalse(result.isDone());
        worker.execute(queued.remove(0));
        long id = await(result);
        assertEquals("Java Workshop", await(repositories.events().findById(id)).title);
        assertNotEquals(900, id);
    }

    @Test public void duplicateAndForeignKeyFailuresAreReportedAndQueueRecovers() throws Exception {
        long eventId = await(repositories.events().insert(event()));
        long studentId = await(repositories.profiles().insert(profile()));
        long id = await(repositories.registrations().insert(registration(eventId, studentId, "ticket")));
        assertFailure(RepositoryException.Reason.CONFLICT,
                repositories.registrations().insert(registration(eventId, studentId, "duplicate")));
        assertFailure(RepositoryException.Reason.CONFLICT,
                repositories.registrations().insert(registration(eventId + 100, studentId, "orphan")));
        assertEquals(id, await(repositories.registrations().findByCode("ticket")).id);
        assertEquals(Integer.valueOf(1), await(repositories.registrations().countForEvent(eventId)));
        assertNull(await(repositories.registrations().findByCode("duplicate")));
    }

    @Test public void repeatedCheckInAcceptsOnlyOneRequest() throws Exception {
        long eventId = await(repositories.events().insert(event()));
        long studentId = await(repositories.profiles().insert(profile()));
        await(repositories.registrations().insert(registration(eventId, studentId, "ticket")));
        CompletableFuture<Boolean> first = repositories.registrations().markCheckedIn("ticket");
        CompletableFuture<Boolean> second = repositories.registrations().markCheckedIn("ticket");
        assertTrue(await(first));
        assertFalse(await(second));
        assertFalse(await(repositories.registrations().markCheckedIn("missing")));
    }

    @Test public void invalidInsertAndMissingUpdateHaveStableErrorReasons() throws Exception {
        Event event = event();
        event.id = 999;
        assertFailure(RepositoryException.Reason.INVALID_INPUT, repositories.events().insert(event));
        assertFailure(RepositoryException.Reason.NOT_FOUND, repositories.events().update(event));
        StudentProfile profile = profile();
        profile.id = 999;
        assertFailure(RepositoryException.Reason.NOT_FOUND, repositories.profiles().update(profile));
        worker.shutdown();
        assertFailure(RepositoryException.Reason.STORAGE, repositories.events().findById(1));
    }

    @Test public void observedListChangesAfterRepositoryWrite() throws Exception {
        LiveData<List<Event>> source = repositories.events().observeUpcoming(100, "Workshop", "java");
        BlockingQueue<List<Event>> emissions = new LinkedBlockingQueue<>();
        Observer<List<Event>> observer = emissions::offer;
        source.observeForever(observer);
        try {
            List<Event> initial = emissions.poll(5, TimeUnit.SECONDS);
            assertNotNull(initial);
            assertTrue(initial.isEmpty());
            long id = await(repositories.events().insert(event()));
            List<Event> changed = emissions.poll(5, TimeUnit.SECONDS);
            assertNotNull(changed);
            assertEquals(1, changed.size());
            assertEquals(id, changed.get(0).id);
        } finally {
            source.removeObserver(observer);
        }
    }

    private static Event event() {
        Event event = new Event();
        event.title = "Java Workshop";
        event.category = "Workshop";
        event.dateTime = 100;
        event.capacity = 10;
        return event;
    }

    private static StudentProfile profile() {
        StudentProfile profile = new StudentProfile();
        profile.name = "Student";
        profile.rollNo = "001";
        profile.course = "MCA";
        profile.year = 1;
        profile.email = "student@example.edu";
        return profile;
    }

    private static Registration registration(long eventId, long studentId, String code) {
        Registration result = new Registration();
        result.eventId = eventId;
        result.studentId = studentId;
        result.registrationCode = code;
        result.registeredAt = 50;
        return result;
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    private static void assertFailure(RepositoryException.Reason reason, CompletableFuture<?> future) {
        ExecutionException error = assertThrows(ExecutionException.class,
                () -> future.get(5, TimeUnit.SECONDS));
        assertTrue(error.getCause() instanceof RepositoryException);
        assertEquals(reason, ((RepositoryException) error.getCause()).getReason());
    }
}
