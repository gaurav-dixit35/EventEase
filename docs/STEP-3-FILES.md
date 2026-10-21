# Step 3 — complete new and changed files

All new and changed source files are reproduced in full below, with paths labeled. Room entities, DAOs, schema, Gradle dependencies, and UI files are unchanged in this step.

## app/src/main/java/com/timscdr/eventease/repository/AppRepositories.java

``java
package com.timscdr.eventease.repository;

import android.content.Context;
import com.timscdr.eventease.data.database.AppDatabase;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Application-scoped wiring. Future ViewModel factories receive these contracts. */
public final class AppRepositories {
    private static volatile AppRepositories instance;
    private final EventRepository events;
    private final ProfileRepository profiles;
    private final RegistrationRepository registrations;

    // Injection seam for tests; the production executor lives for the app process.
    AppRepositories(AppDatabase database, Executor executor) {
        events = new RoomEventRepository(database.eventDao(), executor);
        profiles = new RoomProfileRepository(database.profileDao(), executor);
        registrations = new RoomRegistrationRepository(database.registrationDao(), executor);
    }

    public static AppRepositories getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRepositories.class) {
                if (instance == null) {
                    instance = new AppRepositories(AppDatabase.getInstance(context),
                            Executors.newSingleThreadExecutor());
                }
            }
        }
        return instance;
    }

    public EventRepository events() { return events; }
    public ProfileRepository profiles() { return profiles; }
    public RegistrationRepository registrations() { return registrations; }
}

``

## app/src/main/java/com/timscdr/eventease/repository/EntitySnapshots.java

``java
package com.timscdr.eventease.repository;

import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.Objects;

/** Capture mutable form objects before they enter the background queue. */
final class EntitySnapshots {
    private EntitySnapshots() { }

    static Event copy(Event source) {
        Objects.requireNonNull(source, "event");
        Event result = new Event();
        result.id = source.id;
        result.title = source.title;
        result.category = source.category;
        result.description = source.description;
        result.venue = source.venue;
        result.dateTime = source.dateTime;
        result.bannerImageUrl = source.bannerImageUrl;
        result.capacity = source.capacity;
        result.organizerName = source.organizerName;
        return result;
    }

    static StudentProfile copy(StudentProfile source) {
        Objects.requireNonNull(source, "profile");
        StudentProfile result = new StudentProfile();
        result.id = source.id;
        result.name = source.name;
        result.rollNo = source.rollNo;
        result.course = source.course;
        result.year = source.year;
        result.email = source.email;
        return result;
    }

    static Registration copy(Registration source) {
        Objects.requireNonNull(source, "registration");
        Registration result = new Registration();
        result.id = source.id;
        result.eventId = source.eventId;
        result.studentId = source.studentId;
        result.registrationCode = source.registrationCode;
        result.registeredAt = source.registeredAt;
        result.checkedIn = source.checkedIn;
        return result;
    }
}

``

## app/src/main/java/com/timscdr/eventease/repository/EventRepository.java

``java
package com.timscdr.eventease.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.entity.Event;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Storage-independent contract. Future completions are not main-thread callbacks. */
public interface EventRepository {
    LiveData<Event> observeById(long eventId);
    LiveData<List<Event>> observeUpcoming(long now, @Nullable String category, String search);
    LiveData<List<Event>> observeUpcomingForStudent(long studentId, long now);
    LiveData<List<Event>> observePastForStudent(long studentId, long now);
    /** Completes with null if the event is absent. */
    CompletableFuture<Event> findById(long eventId);
    /** Returns the generated ID without mutating the input. New records must have ID 0. */
    CompletableFuture<Long> insert(Event event);
    /** Fails with NOT_FOUND if no record has this ID. */
    CompletableFuture<Void> update(Event event);
}

``

## app/src/main/java/com/timscdr/eventease/repository/ProfileRepository.java

``java
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

``

## app/src/main/java/com/timscdr/eventease/repository/RegistrationRepository.java

``java
package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.entity.Registration;
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
    /** Caller supplies the unique code; insert never replaces an existing ticket. */
    CompletableFuture<Long> insert(Registration registration);
    /** True only for the first check-in; false for unknown or already-used tickets. */
    CompletableFuture<Boolean> markCheckedIn(String code);
}

``

## app/src/main/java/com/timscdr/eventease/repository/RepositoryException.java

``java
package com.timscdr.eventease.repository;

/** Stable error categories so ViewModels never need to catch SQLite exceptions. */
public final class RepositoryException extends RuntimeException {
    public enum Reason { INVALID_INPUT, NOT_FOUND, CONFLICT, STORAGE }

    private final Reason reason;

    public RepositoryException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public RepositoryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}

``

## app/src/main/java/com/timscdr/eventease/repository/RepositoryTasks.java

``java
package com.timscdr.eventease.repository;

import android.database.sqlite.SQLiteConstraintException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

final class RepositoryTasks {
    private RepositoryTasks() { }

    static <T> CompletableFuture<T> submit(Executor executor, Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    future.complete(task.get());
                } catch (RepositoryException error) {
                    future.completeExceptionally(error);
                } catch (SQLiteConstraintException error) {
                    future.completeExceptionally(new RepositoryException(
                            RepositoryException.Reason.CONFLICT,
                            "The record conflicts with a database constraint.", error));
                } catch (RuntimeException error) {
                    future.completeExceptionally(new RepositoryException(
                            RepositoryException.Reason.STORAGE, "Database operation failed.", error));
                }
            });
        } catch (RejectedExecutionException error) {
            future.completeExceptionally(new RepositoryException(
                    RepositoryException.Reason.STORAGE, "Database executor is unavailable.", error));
        }
        return future;
    }

    static void requireNew(long id) {
        if (id != 0) {
            throw new RepositoryException(RepositoryException.Reason.INVALID_INPUT,
                    "New records must use ID 0.");
        }
    }

    static Void requireUpdated(int rows) {
        if (rows == 0) {
            throw new RepositoryException(RepositoryException.Reason.NOT_FOUND,
                    "The record no longer exists.");
        }
        return null;
    }
}

``

## app/src/main/java/com/timscdr/eventease/repository/RoomEventRepository.java

``java
package com.timscdr.eventease.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.dao.EventDao;
import com.timscdr.eventease.data.entity.Event;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class RoomEventRepository implements EventRepository {
    private final EventDao dao;
    private final Executor executor;

    RoomEventRepository(EventDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    @Override public LiveData<Event> observeById(long eventId) {
        return dao.observeById(eventId);
    }

    @Override public LiveData<List<Event>> observeUpcoming(long now, @Nullable String category, String search) {
        return dao.observeUpcoming(now, category, search == null ? "" : search);
    }

    @Override public LiveData<List<Event>> observeUpcomingForStudent(long studentId, long now) {
        return dao.observeUpcomingForStudent(studentId, now);
    }

    @Override public LiveData<List<Event>> observePastForStudent(long studentId, long now) {
        return dao.observePastForStudent(studentId, now);
    }

    @Override public CompletableFuture<Event> findById(long eventId) {
        return RepositoryTasks.submit(executor, () -> dao.findById(eventId));
    }

    @Override public CompletableFuture<Long> insert(Event event) {
        Event snapshot = EntitySnapshots.copy(event);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(snapshot.id);
            return dao.insert(snapshot);
        });
    }

    @Override public CompletableFuture<Void> update(Event event) {
        Event snapshot = EntitySnapshots.copy(event);
        return RepositoryTasks.submit(executor, () -> RepositoryTasks.requireUpdated(dao.update(snapshot)));
    }
}

``

## app/src/main/java/com/timscdr/eventease/repository/RoomProfileRepository.java

``java
package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.dao.ProfileDao;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class RoomProfileRepository implements ProfileRepository {
    private final ProfileDao dao;
    private final Executor executor;

    RoomProfileRepository(ProfileDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    @Override public LiveData<StudentProfile> observeById(long studentId) {
        return dao.observeById(studentId);
    }

    @Override public CompletableFuture<StudentProfile> findById(long studentId) {
        return RepositoryTasks.submit(executor, () -> dao.findById(studentId));
    }

    @Override public CompletableFuture<List<StudentProfile>> findByRollNo(String rollNo) {
        return RepositoryTasks.submit(executor, () -> dao.findByRollNo(rollNo));
    }

    @Override public CompletableFuture<Long> insert(StudentProfile profile) {
        StudentProfile snapshot = EntitySnapshots.copy(profile);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(snapshot.id);
            return dao.insert(snapshot);
        });
    }

    @Override public CompletableFuture<Void> update(StudentProfile profile) {
        StudentProfile snapshot = EntitySnapshots.copy(profile);
        return RepositoryTasks.submit(executor, () -> RepositoryTasks.requireUpdated(dao.update(snapshot)));
    }
}

``

## app/src/main/java/com/timscdr/eventease/repository/RoomRegistrationRepository.java

``java
package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.dao.RegistrationDao;
import com.timscdr.eventease.data.entity.Registration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class RoomRegistrationRepository implements RegistrationRepository {
    private final RegistrationDao dao;
    private final Executor executor;

    RoomRegistrationRepository(RegistrationDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    @Override public LiveData<Registration> observeById(long registrationId) {
        return dao.observeById(registrationId);
    }

    @Override public LiveData<List<Registration>> observeForStudent(long studentId) {
        return dao.observeForStudent(studentId);
    }

    @Override public LiveData<Registration> observeForEventAndStudent(long eventId, long studentId) {
        return dao.observeForEventAndStudent(eventId, studentId);
    }

    @Override public LiveData<Integer> observeRegistrationCount(long studentId) {
        return dao.observeRegistrationCount(studentId);
    }

    @Override public LiveData<Integer> observeAttendanceCount(long studentId) {
        return dao.observeAttendanceCount(studentId);
    }

    @Override public CompletableFuture<Registration> findByCode(String code) {
        return RepositoryTasks.submit(executor, () -> dao.findByCode(code));
    }

    @Override public CompletableFuture<Integer> countForEvent(long eventId) {
        return RepositoryTasks.submit(executor, () -> dao.countForEvent(eventId));
    }

    @Override public CompletableFuture<Long> insert(Registration registration) {
        Registration snapshot = EntitySnapshots.copy(registration);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(snapshot.id);
            return dao.insert(snapshot);
        });
    }

    @Override public CompletableFuture<Boolean> markCheckedIn(String code) {
        return RepositoryTasks.submit(executor, () -> dao.markCheckedIn(code) == 1);
    }
}

``

## app/src/test/java/com/timscdr/eventease/repository/RepositoryTest.java

``java
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
        repositories = new AppRepositories(db, worker);
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

``

## README.md

``markdown
# EventEase — Step 3

Java + XML Android project for TIMSCDR. The foundation, Room layer, and repositories are implemented.

## Terminal build

Use JDK 17 (recommended), Android SDK Platform 35, and Android SDK Build Tools
34.0.0. No Android Studio is needed. AGP 8.7.3 uses the bundled Gradle 8.9 wrapper.
Min SDK is 24; target SDK is 34 (within the requested 34/35 range).
Compile SDK 35 is independent of the target SDK. Target 34 keeps enforced Android
15 edge-to-edge behavior out of this initial theme setup.

Set ANDROID_HOME to your SDK directory, or create an untracked local.properties:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

Install missing SDK packages using the Android command-line tools:

```powershell
sdkmanager "platforms;android-35" "build-tools;34.0.0" "platform-tools"
sdkmanager --licenses
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

On Linux/macOS:

```sh
chmod +x gradlew
./gradlew assembleDebug
```

APK: app/build/outputs/apk/debug/app-debug.apk.
The first build requires network access for Gradle and Maven dependencies.

## Verify the foundation

Launch EventEase on a device (USB debugging enabled). Confirm the foundation
preview shows Playfair Display headings, Inter body text, and a thin gold divider.
Switch the device between light and dark modes and reopen the app to check both
palettes. App-specific theme selection belongs to Step 8.

## Structure and next steps

Base package: com.timscdr.eventease.
The data/entity, data/dao, data/database, repository, viewmodel, ui, util, and
receiver packages follow the supplied specification. Empty directories are kept
in Git with empty .gitkeep files. The Room entities, DAOs, and AppDatabase are implemented.
Repositories are implemented; ViewModels are not implemented yet.
The organizer package is reserved only; optional organizer work has not started.
SplashActivity currently hosts the foundation preview, not the finished splash.

Next steps, each requiring your confirmation: authentication
and profile; dashboard and browsing; registration and QR; My Events; settings.
UI will observe ViewModels; ViewModels will depend on the repository interfaces.
Room access is behind repositories to preserve a future backend swap.

## Assets and decisions

All eight light/dark palette roles match the supplied specification. Dark colors
override values through values-night/colors.xml; the shared DayNight theme does
not need a duplicate values-night/themes.xml. Navy fills buttons; gold is an
accent, following your explicit instruction over the spec's gold button examples.
Cards have no elevation and 10dp corners. No animations are needed in this static
preview; later transitions will be subtle fades/slides.

Local static regular-weight TTFs are bundled in res/font. Sources:
- https://github.com/google/fonts/tree/main/ofl/playfairdisplay
- https://github.com/google/fonts/tree/main/ofl/inter

They were instantiated at their default axes using fontTools for API 24 support.
Their SIL Open Font Licenses are included in licenses/. Fonts never need runtime
network access. Gradle wrapper files come from Gradle v8.9.0; its distribution
checksum is pinned in gradle-wrapper.properties.

The real TIMSCDR crest has not been supplied. Provide the approved PNG/WebP asset
before completing splash/login branding; no substitute crest has been invented.
The launcher currently uses the system default icon pending that asset.

## Manual requirements

No API keys or manual dependency additions are needed. Room, lifecycle, ZXing,
Glide, RecyclerView, SwipeRefreshLayout, and Material dependencies are declared.
No Kotlin plugin or Kotlin source is used.
No app permissions are needed for Step 1. Internet permission for remote event
banners, notification permission/reminder handling, and optional scanner camera
permission will be introduced with their corresponding features.

## Step 2 — Room layer

The database contains exactly the specified entity fields. Event.bannerImageUrl
is the single banner URL/path column. IDs and foreign keys are long integers;
dateTime and registeredAt store UTC epoch milliseconds. StudentProfile.year and
Event.capacity are integers. Required strings are non-null; bannerImageUrl is
nullable. Form validation and business rules belong in the later repository /
ViewModel slices, so the entities do not yet validate blank text or numeric ranges.

Registration.registrationCode is unique. A unique (eventId, studentId) index
additionally enforces one registration per student per event. Foreign keys use
RESTRICT to preserve tickets if a parent deletion is attempted. Inserts use ABORT,
and parent edits use UPDATE, avoiding replacement of existing records. Roll numbers
and emails have no additional uniqueness constraint beyond the provided schema.

EventDao provides category/title filtering and upcoming/past registration joins.
Null category selects all categories; use an empty search string for no text filter.
Title search treats percent/underscore literally and is case-insensitive for ASCII
text using SQLite lower(). Upcoming includes dateTime == now; past is strictly
earlier. With no event end time in the schema, this split uses the event start time.
The caller supplies now; a future ViewModel must refresh this argument on resume
and at time boundaries, since LiveData observes database changes, not the clock.

ProfileDao provides profile insertion, editing, lookup, and observation.
RegistrationDao provides ticket lookup, observation, registration/attendance counts,
and an atomic checkedIn update (one successful update per ticket). This is only a
data operation; the optional organizer screens/scanner have not been started.

AppDatabase is a thread-safe singleton holding only the application context.
Production main-thread database access is disabled. Step 3 repositories run
synchronous DAO methods on a background executor. Future multi-operation business
transactions will also belong inside repository implementations.
No destructive migration fallback is configured. Version 1 is exported under
app/schemas/ for future migration review. Do not edit the generated schema by hand.

Run the build and seven local database tests without a connected phone:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

Robolectric runs an in-memory Room database with an API 28 runtime. Tests cover
profile persistence, foreign keys, duplicate tickets, one-time check-in, attendance,
time boundaries, student isolation, search, and parent update/delete behavior.
Test dependencies are already declared; the first run downloads its test runtime.
There are no new permissions, API keys, or manual dependency changes for Step 2.
The app still shows the Step 1 preview until the authentication slice is built.

Full file listings: docs/STEP-1-FILES.md and docs/STEP-2-FILES.md (historical),
and docs/STEP-3-FILES.md (this step).

## Step 3 — Repository contracts and Room implementations

EventRepository, ProfileRepository, and RegistrationRepository are interfaces.
Their Room implementations are package-private within repository/. AppRepositories
creates the application-scoped instances and exposes only the interfaces. Later
ViewModel factories obtain those interfaces; Activities/Fragments will not use DAOs
or AppDatabase. A Firebase implementation can replace the Room implementations and
wiring here, preserving the contracts used by ViewModels and UI. The existing entity
classes remain the shared Java data objects; consumers do not use their Room annotations.

Observable reads return LiveData directly; Room performs those queries asynchronously.
One-time reads/writes return CompletableFuture and use one shared background executor.
Do not call get() or join() on the Android main thread. A future ViewModel should use
whenComplete and postValue to expose the result as UI state. Future continuations may
run on the worker or caller thread, so they must never update Views directly.
CompletableFuture is available at this project's minimum API 24; no new dependency
or desugaring setup is needed. Cancelling a future is not a database rollback and does
not guarantee that a queued write will be cancelled.

Insert/update calls copy their input before queueing work. Callers may edit their own
objects afterward without changing the submitted values. Insert returns the generated
long ID and does not mutate the caller's object. New objects must have ID 0. Missing
one-time lookups return null (roll-number lookup returns an empty list); missing update
targets report NOT_FOUND. Observable single-row lookups can emit null.

One-time operations complete exceptionally with RepositoryException reasons:
- INVALID_INPUT: an insert supplied a nonzero ID.
- NOT_FOUND: an update matched no row.
- CONFLICT: a database constraint rejected a write, including duplicate or orphan tickets.
- STORAGE: another database failure or unavailable executor.

Use the reason in ViewModels to select user-facing messages, rather than showing
raw SQL errors. get()/join() wrap exceptional completion; whenComplete receives the
error directly for these repository futures. Null entity arguments are programmer
errors rejected synchronously. LiveData does not carry an error channel; the error
categories above apply to one-time operations.

This step wraps persistence operations only. Login/session rules, form validation,
registration-code generation, capacity policy and multi-operation transactions are
implemented with their later feature slices. No schema or UI changes were needed.
The check-in method wraps the existing atomic DAO operation; organizer screens remain
optional and have not been started. No repository retains an Activity or Fragment.

Verification: assembleDebug and all 13 tests passed (seven Room tests plus six repository
tests). Repository tests keep Room main-thread access disabled and use an injected
executor. They cover background persistence, queued input snapshots, duplicate and
foreign-key failures, recovery after failures, repeat check-in, missing updates,
executor rejection, and observable list updates.

No API keys, permissions, or manual Gradle dependencies are required for Step 3.
Run the same build/test command above. The app continues to show the foundation preview;
Step 4 adds login/signup and the StudentProfile flow after your confirmation.

``
