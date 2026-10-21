package com.timscdr.eventease.repository;

import static org.junit.Assert.*;

import android.content.SharedPreferences;
import androidx.room.Room;
import com.timscdr.eventease.data.database.AppDatabase;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LocalAuthRepositoryTest {
    private AppDatabase db;
    private ExecutorService worker;
    private SharedPreferences preferences;
    private AuthRepository auth;

    @Before public void setUp() {
        worker = Executors.newFixedThreadPool(2);
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .setQueryExecutor(worker).build();
        preferences = RuntimeEnvironment.getApplication().getSharedPreferences("auth_test", 0);
        preferences.edit().clear().commit();
        auth = new LocalAuthRepository(db, preferences, worker);
    }

    @After public void tearDown() throws Exception {
        worker.shutdown();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        db.close();
    }

    @Test public void signupPersistsProfileAndSessionAcrossRepositoryInstances() throws Exception {
        StudentProfile input = profile();
        StudentProfile saved = await(auth.signUp(input));
        assertTrue(saved.id > 0);
        assertEquals(0, input.id);
        assertEquals("EE001", saved.rollNo);
        assertEquals("Test Student", saved.name);
        AuthRepository recreated = new LocalAuthRepository(db, preferences, worker);
        assertEquals(saved.id, await(recreated.restoreSession()).id);
        await(recreated.logout());
        assertNull(await(recreated.restoreSession()));
        assertEquals(saved.id, await(recreated.signIn(" test student ", " ee001 ")).id);
    }

    @Test public void wrongNameCannotSelectAnExistingProfile() throws Exception {
        await(auth.signUp(profile()));
        await(auth.logout());
        assertFailure(RepositoryException.Reason.AUTH_FAILED, auth.signIn("Someone Else", "EE001"));
        assertFailure(RepositoryException.Reason.AUTH_FAILED, auth.signIn("Test Student", "missing"));
        assertNull(await(auth.restoreSession()));
    }

    @Test public void simultaneousSignupsCannotCreateTwoProfilesForSameRoll() throws Exception {
        CompletableFuture<StudentProfile> first = auth.signUp(profile());
        CompletableFuture<StudentProfile> second = auth.signUp(profile());
        int success = 0;
        int duplicate = 0;
        for (CompletableFuture<StudentProfile> future : java.util.Arrays.asList(first, second)) {
            try { await(future); success++; }
            catch (ExecutionException error) {
                assertEquals(RepositoryException.Reason.ALREADY_EXISTS,
                        ((RepositoryException) error.getCause()).getReason());
                duplicate++;
            }
        }
        assertEquals(1, success);
        assertEquals(1, duplicate);
        assertEquals(1, worker.submit(() -> db.profileDao().findByRollNo("ee001").size()).get().intValue());
    }

    @Test public void staleSessionIsCleared() throws Exception {
        preferences.edit().putLong("student_id", 500).commit();
        assertNull(await(auth.restoreSession()));
        assertFalse(preferences.contains("student_id"));
    }

    @Test public void legacyDuplicateProfilesAreNotSilentlySelected() throws Exception {
        worker.submit(() -> {
            db.profileDao().insert(profile());
            db.profileDao().insert(profile());
        }).get();
        assertFailure(RepositoryException.Reason.AUTH_FAILED, auth.signIn("Test Student", "EE001"));
        assertNull(await(auth.restoreSession()));
    }

    @Test public void incompleteSignupDoesNotPersistSessionOrProfile() throws Exception {
        StudentProfile input = profile();
        input.course = " ";
        assertFailure(RepositoryException.Reason.INVALID_INPUT, auth.signUp(input));
        assertNull(await(auth.restoreSession()));
        assertEquals(0, worker.submit(() -> db.profileDao().findByRollNo("EE001").size()).get().intValue());
    }

    private static StudentProfile profile() {
        StudentProfile result = new StudentProfile();
        result.name = " Test Student ";
        result.rollNo = " ee001 ";
        result.course = "MCA";
        result.year = 1;
        result.email = "test@example.edu";
        return result;
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    private static void assertFailure(RepositoryException.Reason reason, CompletableFuture<?> future) {
        ExecutionException error = assertThrows(ExecutionException.class, () -> await(future));
        assertEquals(reason, ((RepositoryException) error.getCause()).getReason());
    }
}
