# Step 4 — complete new and changed files

Local Name + Roll No. sign-in, signup, session restoration, profile display, logout, and tests. Every source/resource file changed in this step is reproduced below with its path. No Gradle dependency or database schema changes were made. The approved crest remains a user-supplied asset; see README.md.

Verification: assembleDebug, all 26 tests, and lintDebug passed (0 lint errors; 16 non-blocking warnings). Physical-device installation could not run because the device disconnected.

## app/src/main/AndroidManifest.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.EventEase">
        <activity android:name=".ui.auth.LoginActivity" android:exported="false"
            android:windowSoftInputMode="adjustResize" />
        <activity android:name=".ui.auth.SignupActivity" android:exported="false"
            android:windowSoftInputMode="adjustResize" />
        <activity android:name=".ui.dashboard.DashboardActivity" android:exported="false" />
        <activity
            android:name=".ui.splash.SplashActivity"
            android:theme="@style/Theme.EventEase.Splash"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>

````

## app/src/main/java/com/timscdr/eventease/data/dao/ProfileDao.java

````java
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

````

## app/src/main/java/com/timscdr/eventease/repository/AppRepositories.java

````java
package com.timscdr.eventease.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.timscdr.eventease.data.database.AppDatabase;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Application-scoped wiring. Future ViewModel factories receive these contracts. */
public final class AppRepositories {
    private static volatile AppRepositories instance;
    private final EventRepository events;
    private final ProfileRepository profiles;
    private final RegistrationRepository registrations;
    private final AuthRepository auth;

    // Injection seam for tests; the production executor lives for the app process.
    AppRepositories(AppDatabase database, Executor executor, SharedPreferences session) {
        events = new RoomEventRepository(database.eventDao(), executor);
        profiles = new RoomProfileRepository(database.profileDao(), executor);
        registrations = new RoomRegistrationRepository(database.registrationDao(), executor);
        auth = new LocalAuthRepository(database, session, executor);
    }

    public static AppRepositories getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRepositories.class) {
                if (instance == null) {
                    instance = new AppRepositories(AppDatabase.getInstance(context),
                            Executors.newSingleThreadExecutor(), context.getApplicationContext()
                            .getSharedPreferences("eventease_session", Context.MODE_PRIVATE));
                }
            }
        }
        return instance;
    }

    public EventRepository events() { return events; }
    public ProfileRepository profiles() { return profiles; }
    public RegistrationRepository registrations() { return registrations; }
    public AuthRepository auth() { return auth; }
}

````

## app/src/main/java/com/timscdr/eventease/repository/AuthRepository.java

````java
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

````

## app/src/main/java/com/timscdr/eventease/repository/LocalAuthRepository.java

````java
package com.timscdr.eventease.repository;

import android.content.SharedPreferences;
import com.timscdr.eventease.data.database.AppDatabase;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class LocalAuthRepository implements AuthRepository {
    private static final String STUDENT_ID = "student_id";
    private final AppDatabase database;
    private final SharedPreferences session;
    private final Executor executor;

    LocalAuthRepository(AppDatabase database, SharedPreferences session, Executor executor) {
        this.database = database;
        this.session = session;
        this.executor = executor;
    }

    @Override public CompletableFuture<StudentProfile> signIn(String name, String rollNo) {
        return RepositoryTasks.submit(executor, () -> {
            List<StudentProfile> matches = database.profileDao().findByRollNo(clean(rollNo));
            if (clean(name).isEmpty() || clean(rollNo).isEmpty() || matches.size() != 1
                    || !matches.get(0).name.trim().equalsIgnoreCase(clean(name))) {
                throw new RepositoryException(RepositoryException.Reason.AUTH_FAILED,
                        "Name and roll number do not identify one profile.");
            }
            StudentProfile profile = matches.get(0);
            saveSession(profile.id);
            return profile;
        });
    }

    @Override public CompletableFuture<StudentProfile> signUp(StudentProfile input) {
        StudentProfile profile = EntitySnapshots.copy(input);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(profile.id);
            profile.name = clean(profile.name);
            profile.rollNo = clean(profile.rollNo).toUpperCase(Locale.ROOT);
            profile.course = clean(profile.course);
            profile.email = clean(profile.email);
            if (profile.name.isEmpty() || profile.rollNo.isEmpty() || profile.course.isEmpty()
                    || profile.email.isEmpty() || profile.year < 1) {
                throw new RepositoryException(RepositoryException.Reason.INVALID_INPUT,
                        "Complete all profile fields.");
            }
            // SQLite serializes this check-and-insert, without changing the supplied schema.
            database.runInTransaction(() -> {
                if (!database.profileDao().findByRollNo(profile.rollNo).isEmpty()) {
                    throw new RepositoryException(RepositoryException.Reason.ALREADY_EXISTS,
                            "A profile already uses this roll number.");
                }
                profile.id = database.profileDao().insert(profile);
            });
            saveSession(profile.id);
            return profile;
        });
    }

    @Override public CompletableFuture<StudentProfile> restoreSession() {
        return RepositoryTasks.submit(executor, () -> {
            long id = session.getLong(STUDENT_ID, 0);
            if (id == 0) return null;
            StudentProfile profile = database.profileDao().findById(id);
            if (profile == null) saveSession(0);
            return profile;
        });
    }

    @Override public CompletableFuture<Void> logout() {
        return RepositoryTasks.submit(executor, () -> {
            saveSession(0);
            return null;
        });
    }

    private void saveSession(long id) {
        SharedPreferences.Editor editor = session.edit();
        if (id == 0) editor.remove(STUDENT_ID); else editor.putLong(STUDENT_ID, id);
        if (!editor.commit()) {
            throw new RepositoryException(RepositoryException.Reason.STORAGE,
                    "Could not save the session. The profile remains available for sign-in.");
        }
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}

````

## app/src/main/java/com/timscdr/eventease/repository/RepositoryException.java

````java
package com.timscdr.eventease.repository;

/** Stable error categories so ViewModels never need to catch SQLite exceptions. */
public final class RepositoryException extends RuntimeException {
    public enum Reason { INVALID_INPUT, NOT_FOUND, CONFLICT, STORAGE, AUTH_FAILED, ALREADY_EXISTS }

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

````

## app/src/main/java/com/timscdr/eventease/ui/auth/AuthFormActivity.java

````java
package com.timscdr.eventease.ui.auth;

import android.view.View;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

/** Shared rendering only. Validation and persistence are delegated to the ViewModel. */
abstract class AuthFormActivity extends AppCompatActivity {
    protected AuthViewModel model;
    private OnBackPressedCallback busyBack;
    private boolean routed;

    protected void bindForm(int layout) {
        setContentView(layout);
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(AuthViewModel.class);
        busyBack = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { /* Finish the submitted operation first. */ }
        };
        getOnBackPressedDispatcher().addCallback(this, busyBack);
        model.state().observe(this, this::render);
    }

    protected String input(int id) {
        TextInputEditText field = findViewById(id);
        return field.getText() == null ? "" : field.getText().toString();
    }

    private void render(AuthState state) {
        boolean loading = state.status == AuthState.Status.LOADING;
        busyBack.setEnabled(loading);
        findViewById(R.id.progress).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.submit).setEnabled(!loading);
        findViewById(R.id.alternate).setEnabled(!loading);
        int[] layouts = {R.id.name_layout, R.id.roll_layout, R.id.course_layout, R.id.year_layout, R.id.email_layout};
        for (int id : layouts) {
            TextInputLayout field = findViewById(id);
            if (field != null) { field.setEnabled(!loading); field.setError(null); }
        }
        TextView error = findViewById(R.id.form_error);
        error.setVisibility(View.GONE);
        if (state.status == AuthState.Status.SIGNED_IN && !routed) {
            routed = true;
            ScreenNavigation.home(this);
        } else if (state.status == AuthState.Status.ERROR) {
            int field = 0;
            int message;
            switch (state.error) {
                case NAME: field = R.id.name_layout; message = R.string.error_name; break;
                case ROLL: field = R.id.roll_layout; message = R.string.error_roll; break;
                case COURSE: field = R.id.course_layout; message = R.string.error_course; break;
                case YEAR: field = R.id.year_layout; message = R.string.error_year; break;
                case EMAIL: field = R.id.email_layout; message = R.string.error_email; break;
                case AUTH_FAILED: message = R.string.error_auth; break;
                case DUPLICATE: message = R.string.error_duplicate; break;
                default: message = R.string.error_storage;
            }
            if (field != 0) {
                TextInputLayout target = findViewById(field);
                target.setError(getString(message));
                if (target.getEditText() != null) target.getEditText().requestFocus();
            } else {
                error.setText(message);
                error.setVisibility(View.VISIBLE);
            }
        }
    }
}

````

## app/src/main/java/com/timscdr/eventease/ui/auth/LoginActivity.java

````java
package com.timscdr.eventease.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import com.timscdr.eventease.R;

public final class LoginActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindForm(R.layout.activity_login);
        findViewById(R.id.submit).setOnClickListener(view -> model.signIn(input(R.id.name), input(R.id.roll)));
        findViewById(R.id.alternate).setOnClickListener(view -> {
            startActivity(new Intent(this, SignupActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}

````

## app/src/main/java/com/timscdr/eventease/ui/auth/SignupActivity.java

````java
package com.timscdr.eventease.ui.auth;

import android.os.Bundle;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;

public final class SignupActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindForm(R.layout.activity_signup);
        findViewById(R.id.submit).setOnClickListener(view -> model.signUp(input(R.id.name), input(R.id.roll),
                input(R.id.course), input(R.id.year), input(R.id.email)));
        findViewById(R.id.alternate).setOnClickListener(view -> ScreenNavigation.login(this));
    }
}

````

## app/src/main/java/com/timscdr/eventease/ui/dashboard/DashboardActivity.java

````java
package com.timscdr.eventease.ui.dashboard;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.timscdr.eventease.R;
import com.timscdr.eventease.ui.profile.ProfileFragment;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

/** Step 4 host for the profile slice. Step 5 adds Events/My Events navigation. */
public final class DashboardActivity extends AppCompatActivity {
    private AuthViewModel auth;
    private boolean routed;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        auth = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(AuthViewModel.class);
        findViewById(R.id.retry).setOnClickListener(view -> auth.restoreSession());
        auth.state().observe(this, state -> {
            boolean signedIn = state.status == AuthState.Status.SIGNED_IN;
            findViewById(R.id.profile_container).setVisibility(signedIn ? View.VISIBLE : View.GONE);
            findViewById(R.id.session_gate).setVisibility(signedIn ? View.GONE : View.VISIBLE);
            boolean failed = state.status == AuthState.Status.ERROR;
            findViewById(R.id.progress).setVisibility(failed ? View.GONE : View.VISIBLE);
            findViewById(R.id.session_error).setVisibility(failed ? View.VISIBLE : View.GONE);
            findViewById(R.id.retry).setVisibility(failed ? View.VISIBLE : View.GONE);
            if (signedIn) {
                Fragment existing = getSupportFragmentManager().findFragmentById(R.id.profile_container);
                if (existing == null || existing.requireArguments().getLong(ProfileFragment.STUDENT_ID) != state.studentId) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.profile_container, ProfileFragment.create(state.studentId)).commit();
                }
            } else if (state.status == AuthState.Status.SIGNED_OUT && !routed) {
                routed = true;
                ScreenNavigation.login(this);
            }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        auth.restoreSession();
    }
}

````

## app/src/main/java/com/timscdr/eventease/ui/profile/ProfileFragment.java

````java
package com.timscdr.eventease.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.timscdr.eventease.R;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.ProfileViewModel;
import java.util.Locale;

public final class ProfileFragment extends Fragment {
    public static final String STUDENT_ID = "student_id";
    public ProfileFragment() { super(R.layout.fragment_profile); }

    public static ProfileFragment create(long studentId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putLong(STUDENT_ID, studentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long studentId = requireArguments().getLong(STUDENT_ID);
        EventEaseViewModelFactory factory = new EventEaseViewModelFactory(requireContext(), studentId);
        ProfileViewModel model = new ViewModelProvider(this, factory).get(ProfileViewModel.class);
        AuthViewModel auth = new ViewModelProvider(requireActivity(), factory).get(AuthViewModel.class);
        model.profile.observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) { auth.restoreSession(); return; }
            ((TextView) view.findViewById(R.id.student_name)).setText(profile.name);
            ((TextView) view.findViewById(R.id.student_roll)).setText(getString(R.string.profile_roll, profile.rollNo));
            ((TextView) view.findViewById(R.id.student_course)).setText(getString(R.string.course_year, profile.course, profile.year));
            ((TextView) view.findViewById(R.id.student_email)).setText(profile.email);
            String name = profile.name.trim();
            String initial = name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1));
            ((TextView) view.findViewById(R.id.initial)).setText(initial.toUpperCase(Locale.ROOT));
        });
        model.registrations.observe(getViewLifecycleOwner(), count ->
                ((TextView) view.findViewById(R.id.registration_count)).setText(getString(R.string.registration_count, count == null ? 0 : count)));
        model.attended.observe(getViewLifecycleOwner(), count ->
                ((TextView) view.findViewById(R.id.attendance_count)).setText(getString(R.string.attendance_count, count == null ? 0 : count)));
        view.findViewById(R.id.logout).setOnClickListener(button -> auth.logout());
    }
}

````

## app/src/main/java/com/timscdr/eventease/ui/splash/SplashActivity.java

````java
package com.timscdr.eventease.ui.splash;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

public class SplashActivity extends AppCompatActivity {
    private ObjectAnimator loadingLine;
    private boolean routed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        View line = findViewById(R.id.loading_line);
        int width = (int) (120 * getResources().getDisplayMetrics().density);
        loadingLine = ObjectAnimator.ofInt(line, "minimumWidth", 1, width);
        loadingLine.setDuration(700);
        loadingLine.setRepeatCount(ObjectAnimator.INFINITE);
        loadingLine.setRepeatMode(ObjectAnimator.REVERSE);
        AuthViewModel model = new ViewModelProvider(this,
                new EventEaseViewModelFactory(this, 0)).get(AuthViewModel.class);
        findViewById(R.id.retry).setOnClickListener(view -> model.restoreSession());
        model.state().observe(this, state -> {
            boolean failed = state.status == AuthState.Status.ERROR;
            findViewById(R.id.splash_error).setVisibility(failed ? View.VISIBLE : View.GONE);
            findViewById(R.id.retry).setVisibility(failed ? View.VISIBLE : View.GONE);
            if (!routed && state.status == AuthState.Status.SIGNED_IN) {
                routed = true;
                ScreenNavigation.home(this);
            } else if (!routed && state.status == AuthState.Status.SIGNED_OUT) {
                routed = true;
                ScreenNavigation.login(this);
            }
        });
        if (model.state().getValue().status == AuthState.Status.IDLE) model.restoreSession();
    }

    @Override protected void onStart() { super.onStart(); loadingLine.start(); }
    @Override protected void onStop() { loadingLine.cancel(); super.onStop(); }
}

````

## app/src/main/java/com/timscdr/eventease/util/ScreenNavigation.java

````java
package com.timscdr.eventease.util;

import android.app.Activity;
import android.content.Intent;
import com.timscdr.eventease.ui.auth.LoginActivity;
import com.timscdr.eventease.ui.dashboard.DashboardActivity;

public final class ScreenNavigation {
    private ScreenNavigation() { }
    public static void home(Activity activity) { replaceTask(activity, DashboardActivity.class); }
    public static void login(Activity activity) { replaceTask(activity, LoginActivity.class); }

    private static void replaceTask(Activity activity, Class<?> destination) {
        activity.startActivity(new Intent(activity, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        activity.finish();
    }
}

````

## app/src/main/java/com/timscdr/eventease/viewmodel/AuthState.java

````java
package com.timscdr.eventease.viewmodel;

public final class AuthState {
    public enum Status { IDLE, LOADING, SIGNED_IN, SIGNED_OUT, ERROR }
    public enum Error { NONE, NAME, ROLL, COURSE, YEAR, EMAIL, AUTH_FAILED, DUPLICATE, STORAGE }
    public final Status status;
    public final Error error;
    public final long studentId;

    public AuthState(Status status, Error error, long studentId) {
        this.status = status;
        this.error = error;
        this.studentId = studentId;
    }

    public static AuthState of(Status status) { return new AuthState(status, Error.NONE, 0); }
}

````

## app/src/main/java/com/timscdr/eventease/viewmodel/AuthViewModel.java

````java
package com.timscdr.eventease.viewmodel;

import android.util.Patterns;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.AuthRepository;
import com.timscdr.eventease.repository.RepositoryException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class AuthViewModel extends ViewModel {
    private final AuthRepository repository;
    private final MutableLiveData<AuthState> state = new MutableLiveData<>(AuthState.of(AuthState.Status.IDLE));
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean cleared;

    public AuthViewModel(AuthRepository repository) { this.repository = repository; }
    public LiveData<AuthState> state() { return state; }

    public void restoreSession() { run(repository::restoreSession); }

    public void signIn(String name, String roll) {
        if (busy.get()) return;
        if (clean(name).isEmpty()) { invalid(AuthState.Error.NAME); return; }
        if (clean(roll).isEmpty()) { invalid(AuthState.Error.ROLL); return; }
        run(() -> repository.signIn(clean(name), clean(roll)));
    }

    public void signUp(String name, String roll, String course, String year, String email) {
        if (busy.get()) return;
        if (clean(name).isEmpty()) { invalid(AuthState.Error.NAME); return; }
        if (clean(roll).isEmpty()) { invalid(AuthState.Error.ROLL); return; }
        if (clean(course).isEmpty()) { invalid(AuthState.Error.COURSE); return; }
        int parsedYear;
        try {
            parsedYear = Integer.parseInt(clean(year));
            if (parsedYear < 1) throw new NumberFormatException();
        } catch (NumberFormatException error) { invalid(AuthState.Error.YEAR); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(clean(email)).matches()) {
            invalid(AuthState.Error.EMAIL); return;
        }
        StudentProfile profile = new StudentProfile();
        profile.name = clean(name);
        profile.rollNo = clean(roll);
        profile.course = clean(course);
        profile.year = parsedYear;
        profile.email = clean(email);
        run(() -> repository.signUp(profile));
    }

    public void logout() { run(() -> repository.logout().thenApply(ignored -> null)); }

    private void run(Supplier<CompletableFuture<StudentProfile>> operation) {
        if (!busy.compareAndSet(false, true)) return;
        state.setValue(AuthState.of(AuthState.Status.LOADING));
        operation.get().whenComplete((profile, error) -> {
            if (!cleared) {
                if (error == null) {
                    state.postValue(profile == null ? AuthState.of(AuthState.Status.SIGNED_OUT)
                            : new AuthState(AuthState.Status.SIGNED_IN, AuthState.Error.NONE, profile.id));
                } else {
                    while (error.getCause() != null && !(error instanceof RepositoryException)) {
                        error = error.getCause();
                    }
                    AuthState.Error reason = AuthState.Error.STORAGE;
                    if (error instanceof RepositoryException) {
                        RepositoryException.Reason code = ((RepositoryException) error).getReason();
                        if (code == RepositoryException.Reason.AUTH_FAILED) reason = AuthState.Error.AUTH_FAILED;
                        if (code == RepositoryException.Reason.ALREADY_EXISTS) reason = AuthState.Error.DUPLICATE;
                    }
                    state.postValue(new AuthState(AuthState.Status.ERROR, reason, 0));
                }
            }
            busy.set(false);
        });
    }

    private void invalid(AuthState.Error error) {
        state.setValue(new AuthState(AuthState.Status.ERROR, error, 0));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    @Override protected void onCleared() { cleared = true; }
}

````

## app/src/main/java/com/timscdr/eventease/viewmodel/EventEaseViewModelFactory.java

````java
package com.timscdr.eventease.viewmodel;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.timscdr.eventease.repository.AppRepositories;

/** Composition boundary; screen classes never acquire repositories or DAOs themselves. */
public final class EventEaseViewModelFactory implements ViewModelProvider.Factory {
    private final AppRepositories repositories;
    private final long studentId;

    public EventEaseViewModelFactory(Context context, long studentId) {
        repositories = AppRepositories.getInstance(context.getApplicationContext());
        this.studentId = studentId;
    }

    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
        if (type == AuthViewModel.class) return type.cast(new AuthViewModel(repositories.auth()));
        if (type == ProfileViewModel.class) {
            return type.cast(new ProfileViewModel(repositories.profiles(), repositories.registrations(), studentId));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + type.getName());
    }
}

````

## app/src/main/java/com/timscdr/eventease/viewmodel/ProfileViewModel.java

````java
package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.repository.RegistrationRepository;

public final class ProfileViewModel extends ViewModel {
    public final LiveData<StudentProfile> profile;
    public final LiveData<Integer> registrations;
    public final LiveData<Integer> attended;

    public ProfileViewModel(ProfileRepository profiles, RegistrationRepository tickets, long studentId) {
        profile = profiles.observeById(studentId);
        registrations = tickets.observeRegistrationCount(studentId);
        attended = tickets.observeAttendanceCount(studentId);
    }
}

````

## app/src/main/res/drawable/profile_avatar.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid
        android:color="@color/primary" />
    <corners
        android:radius="12dp" />
    <stroke
        android:width="1dp"
        android:color="@color/accent" />
</shape>

````

## app/src/main/res/layout/activity_dashboard.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/profile_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone" />
    <LinearLayout
        android:id="@+id/session_gate"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:orientation="vertical"
        android:padding="24dp">
        <ProgressBar
            android:id="@+id/progress"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:contentDescription="@string/loading" />
        <TextView
            android:id="@+id/session_error"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/error_storage"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:visibility="gone"
            android:gravity="center" />
        <com.google.android.material.button.MaterialButton
            android:id="@+id/retry"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/retry"
            android:visibility="gone" />
    </LinearLayout>
</FrameLayout>

````

## app/src/main/res/layout/activity_login.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">
        <include
            layout="@layout/view_brand" />
        <TextView
            android:id="@+id/heading"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/login_title"
            android:textAppearance="@style/TextAppearance.EventEase.Heading"
            android:layout_marginTop="24dp" />
        <View
            android:layout_width="72dp"
            android:layout_height="1dp"
            android:layout_marginTop="12dp"
            android:background="@drawable/gold_divider" />
        <TextView
            android:id="@+id/subtitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/login_subtitle"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:layout_marginTop="16dp" />
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            app:cardBackgroundColor="@color/primary">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/name_layout"
                    style="@style/Widget.MaterialComponents.TextInputLayout.FilledBox"
                    app:boxBackgroundColor="@color/surface"
                    app:errorTextColor="@color/on_primary"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="@string/name_label">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/name"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textPersonName"
                        android:maxLines="1"
                        android:singleLine="true"
                        android:imeOptions="actionNext"
                        android:saveEnabled="true" />
                </com.google.android.material.textfield.TextInputLayout>
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/roll_layout"
                    style="@style/Widget.MaterialComponents.TextInputLayout.FilledBox"
                    app:boxBackgroundColor="@color/surface"
                    app:errorTextColor="@color/on_primary"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="@string/roll_label">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/roll"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textCapCharacters"
                        android:maxLines="1"
                        android:singleLine="true"
                        android:imeOptions="actionNext"
                        android:saveEnabled="true" />
                </com.google.android.material.textfield.TextInputLayout>
                <TextView
                    android:id="@+id/form_error"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/error_storage"
                    android:textAppearance="@style/TextAppearance.EventEase.Body"
                    android:visibility="gone"
                    android:textColor="@color/on_primary"
                    android:accessibilityLiveRegion="polite"
                    android:layout_marginTop="16dp" />
                <ProgressBar
                    android:id="@+id/progress"
                    android:layout_width="32dp"
                    android:layout_height="32dp"
                    android:layout_gravity="center_horizontal"
                    android:layout_marginTop="12dp"
                    android:visibility="gone"
                    android:contentDescription="@string/loading" />
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/submit"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:minHeight="52dp"
                    android:layout_marginTop="12dp"
                    app:strokeColor="@color/accent"
                    app:strokeWidth="1dp"
                    android:text="@string/sign_in" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>
        <com.google.android.material.button.MaterialButton
            android:id="@+id/alternate"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:minHeight="48dp"
            android:textColor="@color/text_primary"
            android:textAllCaps="false"
            android:text="@string/new_here" />
        <TextView
            android:id="@+id/login_hint"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/local_login_hint"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:gravity="center"
            android:textSize="13sp" />
    </LinearLayout>
</ScrollView>

````

## app/src/main/res/layout/activity_signup.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">
        <include
            layout="@layout/view_brand" />
        <TextView
            android:id="@+id/heading"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/signup_title"
            android:textAppearance="@style/TextAppearance.EventEase.Heading"
            android:layout_marginTop="24dp" />
        <View
            android:layout_width="72dp"
            android:layout_height="1dp"
            android:layout_marginTop="12dp"
            android:background="@drawable/gold_divider" />
        <TextView
            android:id="@+id/subtitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/signup_subtitle"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:layout_marginTop="16dp" />
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/name_layout"
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="@string/name_label">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/name"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textPersonName"
                        android:maxLines="1"
                        android:singleLine="true"
                        android:imeOptions="actionNext"
                        android:saveEnabled="true" />
                </com.google.android.material.textfield.TextInputLayout>
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/roll_layout"
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="@string/roll_label">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/roll"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textCapCharacters"
                        android:maxLines="1"
                        android:singleLine="true"
                        android:imeOptions="actionNext"
                        android:saveEnabled="true" />
                </com.google.android.material.textfield.TextInputLayout>
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/course_layout"
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="@string/course_label">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/course"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textCapWords"
                        android:maxLines="1"
                        android:singleLine="true"
                        android:imeOptions="actionNext"
                        android:saveEnabled="true" />
                </com.google.android.material.textfield.TextInputLayout>
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/year_layout"
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="@string/year_label">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/year"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="number"
                        android:maxLines="1"
                        android:singleLine="true"
                        android:imeOptions="actionNext"
                        android:saveEnabled="true" />
                </com.google.android.material.textfield.TextInputLayout>
                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/email_layout"
                    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="@string/email_label">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/email"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textEmailAddress"
                        android:maxLines="1"
                        android:singleLine="true"
                        android:imeOptions="actionNext"
                        android:saveEnabled="true" />
                </com.google.android.material.textfield.TextInputLayout>
                <TextView
                    android:id="@+id/form_error"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/error_storage"
                    android:textAppearance="@style/TextAppearance.EventEase.Body"
                    android:visibility="gone"
                    android:textColor="@color/error"
                    android:accessibilityLiveRegion="polite"
                    android:layout_marginTop="16dp" />
                <ProgressBar
                    android:id="@+id/progress"
                    android:layout_width="32dp"
                    android:layout_height="32dp"
                    android:layout_gravity="center_horizontal"
                    android:layout_marginTop="12dp"
                    android:visibility="gone"
                    android:contentDescription="@string/loading" />
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/submit"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:minHeight="52dp"
                    android:layout_marginTop="12dp"
                    android:text="@string/create_account" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>
        <com.google.android.material.button.MaterialButton
            android:id="@+id/alternate"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:minHeight="48dp"
            android:textColor="@color/text_primary"
            android:textAllCaps="false"
            android:text="@string/existing_account" />
    </LinearLayout>
</ScrollView>

````

## app/src/main/res/layout/activity_splash.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="32dp">
    <include
        layout="@layout/view_brand" />
    <TextView
        android:id="@+id/heading"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textAppearance="@style/TextAppearance.EventEase.Heading"
        android:gravity="center"
        android:textColor="@color/on_primary"
        android:textSize="40sp"
        android:layout_marginTop="24dp" />
    <View
        android:id="@+id/loading_line"
        android:layout_width="wrap_content"
        android:layout_height="1dp"
        android:layout_marginTop="16dp"
        android:background="@drawable/gold_divider" />
    <TextView
        android:id="@+id/tagline"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/splash_tagline"
        android:textAppearance="@style/TextAppearance.EventEase.Body"
        android:gravity="center"
        android:textColor="@color/on_primary"
        android:layout_marginTop="24dp" />
    <TextView
        android:id="@+id/splash_error"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/error_storage"
        android:textAppearance="@style/TextAppearance.EventEase.Body"
        android:gravity="center"
        android:visibility="gone"
        android:textColor="@color/on_primary"
        android:layout_marginTop="24dp" />
    <com.google.android.material.button.MaterialButton
        android:id="@+id/retry"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        android:text="@string/retry" />
</LinearLayout>

````

## app/src/main/res/layout/fragment_profile.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">
        <TextView
            android:id="@+id/heading"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/profile_title"
            android:textAppearance="@style/TextAppearance.EventEase.Heading" />
        <View
            android:layout_width="72dp"
            android:layout_height="1dp"
            android:layout_marginTop="12dp"
            android:background="@drawable/gold_divider" />
        <TextView
            android:id="@+id/subtitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/profile_subtitle"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:layout_marginTop="16dp" />
        <TextView
            android:id="@+id/initial"
            android:layout_width="72dp"
            android:layout_height="72dp"
            android:layout_marginTop="32dp"
            android:background="@drawable/profile_avatar"
            android:gravity="center"
            android:textColor="@color/on_primary"
            android:textSize="32sp"
            android:fontFamily="@font/playfair_display"
            android:importantForAccessibility="no" />
        <TextView
            android:id="@+id/student_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/loading"
            android:textAppearance="@style/TextAppearance.EventEase.Heading"
            android:layout_marginTop="20dp" />
        <TextView
            android:id="@+id/student_roll"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/roll_label"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:layout_marginTop="12dp" />
        <TextView
            android:id="@+id/student_course"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/course_label"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:layout_marginTop="12dp" />
        <TextView
            android:id="@+id/student_email"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/email_label"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:layout_marginTop="12dp" />
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="28dp">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="20dp">
                <TextView
                    android:id="@+id/registration_count"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textAppearance="@style/TextAppearance.EventEase.Body" />
                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:layout_marginTop="16dp"
                    android:layout_marginBottom="16dp"
                    android:background="@drawable/gold_divider" />
                <TextView
                    android:id="@+id/attendance_count"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textAppearance="@style/TextAppearance.EventEase.Body" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>
        <com.google.android.material.button.MaterialButton
            android:id="@+id/logout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:minHeight="52dp"
            android:text="@string/logout" />
    </LinearLayout>
</ScrollView>

````

## app/src/main/res/layout/view_brand.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    <ImageView
        android:id="@+id/college_crest"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:layout_gravity="center_horizontal"
        android:contentDescription="@string/crest_description"
        android:visibility="gone" />
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:text="@string/institution_short"
        android:textColor="@color/accent"
        android:letterSpacing="0.18"
        android:textSize="14sp" />
</LinearLayout>

````

## app/src/main/res/values/strings.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">EventEase</string>
    <string name="institution_short">TIMSCDR</string>
    <string name="login_title">Sign in to TIMSCDR EventEase</string>
    <string name="login_subtitle">Your next college experience starts here.</string>
    <string name="local_login_hint">Use the name and roll number from your saved profile.</string>
    <string name="signup_title">Create your profile</string>
    <string name="signup_subtitle">A place for every student. An opportunity in every event.</string>
    <string name="name_label">Full name</string>
    <string name="roll_label">Roll number</string>
    <string name="course_label">Course</string>
    <string name="year_label">Year of study</string>
    <string name="email_label">College email</string>
    <string name="sign_in">Sign In</string>
    <string name="create_account">Create Account</string>
    <string name="new_here">New here? Create Account</string>
    <string name="existing_account">Already registered? Sign In</string>
    <string name="profile_title">My Profile</string>
    <string name="profile_subtitle">Your TIMSCDR student identity</string>
    <string name="logout">Log Out</string>
    <string name="loading">Please waitâ€¦</string>
    <string name="retry">Try Again</string>
    <string name="error_name">Enter your full name.</string>
    <string name="error_roll">Enter your roll number.</string>
    <string name="error_course">Enter your course.</string>
    <string name="error_year">Enter a valid year of study (1 or above).</string>
    <string name="error_email">Enter a valid email address.</string>
    <string name="error_auth">No matching profile. Check your name and roll number, or create an account.</string>
    <string name="error_duplicate">This roll number already has a profile. Please sign in.</string>
    <string name="error_storage">We could not complete this request. Try again. If you just created your profile, try signing in.</string>
    <string name="registration_count">My Registrations: %1$d</string>
    <string name="attendance_count">Events Attended: %1$d</string>
    <string name="course_year">%1$s Â· Year %2$d</string>
    <string name="profile_roll">Roll No. %1$s</string>
    <string name="splash_tagline">Discover. Participate. Belong.</string>
    <string name="crest_description">TIMSCDR crest</string>
</resources>

````

## app/src/main/res/values/themes.xml

````xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.EventEase" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryVariant">@color/primary</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="colorSecondary">@color/accent</item>
        <item name="colorSecondaryVariant">@color/accent</item>
        <item name="colorOnSecondary">@color/on_accent</item>
        <item name="colorAccent">@color/accent</item>
        <item name="android:colorBackground">@color/background</item>
        <item name="android:windowBackground">@color/background</item>
        <item name="colorOnBackground">@color/text_primary</item>
        <item name="colorSurface">@color/surface</item>
        <item name="colorOnSurface">@color/text_primary</item>
        <item name="colorError">@color/error</item>
        <item name="colorOnError">@color/on_error</item>
        <item name="android:textColorPrimary">@color/text_primary</item>
        <item name="android:textColorSecondary">@color/text_secondary</item>
        <item name="fontFamily">@font/inter</item>
        <item name="android:fontFamily">@font/inter</item>
        <item name="textAppearanceHeadline4">@style/TextAppearance.EventEase.Heading</item>
        <item name="textAppearanceHeadline6">@style/TextAppearance.EventEase.Heading</item>
        <item name="textAppearanceBody1">@style/TextAppearance.EventEase.Body</item>
        <item name="shapeAppearanceSmallComponent">@style/ShapeAppearance.EventEase</item>
        <item name="shapeAppearanceMediumComponent">@style/ShapeAppearance.EventEase</item>
        <item name="materialButtonStyle">@style/Widget.EventEase.Button</item>
        <item name="materialCardViewStyle">@style/Widget.EventEase.Card</item>
        <item name="android:statusBarColor">@color/primary</item>
        <item name="android:navigationBarColor">@color/primary</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
    <style name="Theme.EventEase.Splash" parent="Theme.EventEase">
        <item name="android:windowBackground">@color/primary</item>
    </style>
</resources>

````

## app/src/test/java/com/timscdr/eventease/repository/LocalAuthRepositoryTest.java

````java
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

````

## app/src/test/java/com/timscdr/eventease/repository/RepositoryTest.java

````java
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

````

## app/src/test/java/com/timscdr/eventease/ui/AuthFlowTest.java

````java
package com.timscdr.eventease.ui;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.timscdr.eventease.R;
import com.timscdr.eventease.ui.auth.LoginActivity;
import com.timscdr.eventease.ui.auth.SignupActivity;
import com.timscdr.eventease.ui.dashboard.DashboardActivity;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AuthFlowTest {
    @Test @Config(sdk = 28, qualifiers = "night")
    public void nightModeSignupInflatesAndRetainsDraftOnRecreation() {
        try (ActivityController<SignupActivity> controller = Robolectric.buildActivity(SignupActivity.class).setup()) {
            ((TextInputEditText) controller.get().findViewById(R.id.name)).setText("Draft Student");
            ((TextInputEditText) controller.get().findViewById(R.id.roll)).setText("DRAFT001");
            controller.recreate();
            assertEquals("Draft Student", ((TextInputEditText) controller.get().findViewById(R.id.name)).getText().toString());
            assertEquals("DRAFT001", ((TextInputEditText) controller.get().findViewById(R.id.roll)).getText().toString());
            assertEquals(0xFF0F1420, controller.get().getColor(R.color.background));
        }
    }

    @Test public void signupProfileLogoutAndLoginRunThroughRealScreens() throws Exception {
        try (ActivityController<SignupActivity> signupController = Robolectric.buildActivity(SignupActivity.class).setup()) {
            SignupActivity signup = signupController.get();
            signup.findViewById(R.id.submit).performClick();
            assertEquals(signup.getString(R.string.error_name),
                    ((TextInputLayout) signup.findViewById(R.id.name_layout)).getError());
            ((TextInputEditText) signup.findViewById(R.id.name)).setText("Screen Student");
            ((TextInputEditText) signup.findViewById(R.id.roll)).setText("SCREEN001");
            ((TextInputEditText) signup.findViewById(R.id.course)).setText("MCA");
            ((TextInputEditText) signup.findViewById(R.id.year)).setText("1");
            ((TextInputEditText) signup.findViewById(R.id.email)).setText("screen@example.edu");
            signup.findViewById(R.id.submit).performClick();
            until(() -> shadowOf(signup).peekNextStartedActivity() != null);
            Intent destination = shadowOf(signup).getNextStartedActivity();
            assertEquals(DashboardActivity.class.getName(), destination.getComponent().getClassName());
            assertTrue((destination.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
        }

        try (ActivityController<DashboardActivity> dashboardController = Robolectric.buildActivity(DashboardActivity.class).setup()) {
            DashboardActivity dashboard = dashboardController.get();
            until(() -> dashboard.findViewById(R.id.student_name) != null
                    && "Screen Student".contentEquals(((TextView) dashboard.findViewById(R.id.student_name)).getText()));
            assertEquals("Roll No. SCREEN001", ((TextView) dashboard.findViewById(R.id.student_roll)).getText().toString());
            dashboard.findViewById(R.id.logout).performClick();
            until(() -> shadowOf(dashboard).peekNextStartedActivity() != null);
            assertEquals(LoginActivity.class.getName(), shadowOf(dashboard).getNextStartedActivity().getComponent().getClassName());
        }

        try (ActivityController<LoginActivity> loginController = Robolectric.buildActivity(LoginActivity.class).setup()) {
            LoginActivity login = loginController.get();
            ((TextInputEditText) login.findViewById(R.id.name)).setText("screen student");
            ((TextInputEditText) login.findViewById(R.id.roll)).setText("screen001");
            login.findViewById(R.id.submit).performClick();
            until(() -> shadowOf(login).peekNextStartedActivity() != null);
            assertEquals(DashboardActivity.class.getName(), shadowOf(login).getNextStartedActivity().getComponent().getClassName());
        }
    }

    private static void until(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(20);
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue("Timed out waiting for screen state", condition.getAsBoolean());
    }
}

````

## app/src/test/java/com/timscdr/eventease/viewmodel/AuthViewModelTest.java

````java
package com.timscdr.eventease.viewmodel;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.ViewModelStore;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.AuthRepository;
import com.timscdr.eventease.repository.RepositoryException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AuthViewModelTest {
    @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
    private FakeAuth repository;
    private AuthViewModel model;

    @Before public void setUp() { repository = new FakeAuth(); model = new AuthViewModel(repository); }

    @Test public void validatesAllSignupFieldsBeforeWriting() {
        model.signUp("", "1", "MCA", "1", "test@example.edu");
        assertEquals(AuthState.Error.NAME, model.state().getValue().error);
        model.signUp("Name", "", "MCA", "1", "test@example.edu");
        assertEquals(AuthState.Error.ROLL, model.state().getValue().error);
        model.signUp("Name", "1", "", "1", "test@example.edu");
        assertEquals(AuthState.Error.COURSE, model.state().getValue().error);
        model.signUp("Name", "1", "MCA", "0", "test@example.edu");
        assertEquals(AuthState.Error.YEAR, model.state().getValue().error);
        model.signUp("Name", "1", "MCA", "999999999999", "test@example.edu");
        assertEquals(AuthState.Error.YEAR, model.state().getValue().error);
        model.signUp("Name", "1", "MCA", "1", "not-email");
        assertEquals(AuthState.Error.EMAIL, model.state().getValue().error);
        assertEquals(0, repository.calls);
    }

    @Test public void ignoresDoubleSubmitAndReportsSuccess() {
        model.signUp(" Name ", " 1 ", " MCA ", "1", "test@example.edu");
        model.signUp("Name", "1", "MCA", "1", "test@example.edu");
        assertEquals(1, repository.calls);
        assertEquals("Name", repository.input.name);
        assertEquals(AuthState.Status.LOADING, model.state().getValue().status);
        StudentProfile profile = new StudentProfile(); profile.id = 42;
        repository.pending.complete(profile);
        assertEquals(AuthState.Status.SIGNED_IN, model.state().getValue().status);
        assertEquals(42, model.state().getValue().studentId);
    }

    @Test public void mapsRepositoryFailureAndAllowsRetry() {
        model.signIn("Name", "1");
        repository.pending.completeExceptionally(new RepositoryException(
                RepositoryException.Reason.AUTH_FAILED, "test"));
        assertEquals(AuthState.Error.AUTH_FAILED, model.state().getValue().error);
        repository.pending = new CompletableFuture<>();
        model.signIn("Name", "1");
        assertEquals(2, repository.calls);
        assertEquals(AuthState.Status.LOADING, model.state().getValue().status);
    }

    @Test public void sessionRestorationAndLogoutEmitSignedOut() {
        model.restoreSession();
        repository.pending.complete(null);
        assertEquals(AuthState.Status.SIGNED_OUT, model.state().getValue().status);
        model.logout();
        assertEquals(AuthState.Status.SIGNED_OUT, model.state().getValue().status);
    }

    @Test public void clearedViewModelDoesNotPublishLateResult() {
        ViewModelStore store = new ViewModelStore();
        store.put("auth", model);
        model.signIn("Name", "1");
        store.clear();
        repository.pending.complete(new StudentProfile());
        assertEquals(AuthState.Status.LOADING, model.state().getValue().status);
    }

    private static class FakeAuth implements AuthRepository {
        CompletableFuture<StudentProfile> pending = new CompletableFuture<>();
        StudentProfile input;
        int calls;
        @Override public CompletableFuture<StudentProfile> signIn(String name, String roll) { calls++; return pending; }
        @Override public CompletableFuture<StudentProfile> signUp(StudentProfile profile) { calls++; input = profile; return pending; }
        @Override public CompletableFuture<StudentProfile> restoreSession() { calls++; return pending; }
        @Override public CompletableFuture<Void> logout() { return CompletableFuture.completedFuture(null); }
    }
}

````

## README.md

````markdown
# EventEase — Step 4

Java + XML Android project for TIMSCDR. Steps 1–4 are implemented: foundation,
Room, repositories, and local login/signup with the StudentProfile flow.

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

## Verify the current app

Launch EventEase on a device (USB debugging enabled). Create a profile using your
name, roll number, course, year, and email. Confirm the saved profile is displayed.
Log out, then sign in with the same name and roll number. Reopen the app to confirm
the session is restored. The detailed Step 4 checklist is below.
Switch the device between light and dark modes and reopen the app to check both
palettes. App-specific theme selection belongs to Step 8.

## Structure and next steps

Base package: com.timscdr.eventease.
The data/entity, data/dao, data/database, repository, viewmodel, ui, util, and
receiver packages follow the supplied specification. Empty directories are kept
in Git with empty .gitkeep files. The Room entities, DAOs, and AppDatabase are implemented.
Repositories and authentication/profile ViewModels are implemented.
The organizer package is reserved only; optional organizer work has not started.
SplashActivity checks session state and routes to LoginActivity or DashboardActivity.
DashboardActivity currently hosts ProfileFragment only; bottom navigation and event
browsing are Step 5.

Next steps, each requiring your confirmation: dashboard and browsing;
registration and QR; My Events; settings.
UI observes ViewModels; ViewModels depend on repository interfaces.
Room access is behind repositories to preserve a future backend swap.

## Assets and decisions

All eight light/dark palette roles match the supplied specification. Dark colors
override values through values-night/colors.xml; the shared DayNight theme does
not need a duplicate values-night/themes.xml. Navy fills buttons; gold is an
accent, following your explicit instruction over the spec's gold button examples.
Cards have no elevation and 10dp corners. Screen transitions fade subtly. The navy
splash uses an ObjectAnimator to animate the thin gold line's width.

Local static regular-weight TTFs are bundled in res/font. Sources:
- https://github.com/google/fonts/tree/main/ofl/playfairdisplay
- https://github.com/google/fonts/tree/main/ofl/inter

They were instantiated at their default axes using fontTools for API 24 support.
Their SIL Open Font Licenses are included in licenses/. Fonts never need runtime
network access. Gradle wrapper files come from Gradle v8.9.0; its distribution
checksum is pinned in gradle-wrapper.properties.

The real TIMSCDR crest has not been supplied. Provide the approved PNG/WebP asset
to complete splash/login branding; no substitute crest has been invented.
view_brand.xml reserves a hidden ImageView for it above the TIMSCDR wordmark.
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
The Step 1 preview was replaced by the authentication/profile slice in Step 4.

Full file listings: docs/STEP-1-FILES.md and docs/STEP-2-FILES.md (historical),
and docs/STEP-3-FILES.md (historical). Current changes are in docs/STEP-4-FILES.md.

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

Step 3 wrapped persistence operations only. Step 4 now adds login/session rules,
form validation, and a transactional signup. Registration-code generation and
capacity policy remain for the registration slice. No schema changes were needed.
The check-in method wraps the existing atomic DAO operation; organizer screens remain
optional and have not been started. No repository retains an Activity or Fragment.

Verification: assembleDebug and all 13 tests passed (seven Room tests plus six repository
tests). Repository tests keep Room main-thread access disabled and use an injected
executor. They cover background persistence, queued input snapshots, duplicate and
foreign-key failures, recovery after failures, repeat check-in, missing updates,
executor rejection, and observable list updates.

No API keys, permissions, or manual Gradle dependencies were required for Step 3.

## Step 4 — Local login, signup, and profile

The specification explicitly permits Name + Roll No. login for a local mini-project.
That option is implemented without adding password fields to the supplied schema.
This selects a local profile; it is not server-verified authentication. No email or
college-domain verification is performed. A real shared backend can later implement
AuthRepository with appropriate identity verification.

Flow: SplashActivity -> LoginActivity -> SignupActivity -> DashboardActivity hosting
ProfileFragment. Returning users with a valid saved profile go directly to the host.
Logout clears the saved session, not the Room profile or its registrations. Successful
login/signup and logout clear the navigation task, so Back cannot reopen the old form
or a signed-out profile. DashboardActivity validates the session whenever it starts.

Architecture:
- AuthRepository / LocalAuthRepository own local sign-in, signup, session restoration,
  and logout. Room and SharedPreferences are accessed only in repositories.
- AuthViewModel validates inputs, blocks overlapping requests, and publishes immutable
  AuthState through LiveData. It holds no Activity, Fragment, or View reference.
- ProfileViewModel observes the profile and registration/attendance counts through the
  existing repository contracts. ProfileFragment renders those values using its view
  lifecycle; it does not read the database or preferences.
- EventEaseViewModelFactory is the dependency-wiring boundary. It injects repositories
  into ViewModels; screens acquire only ViewModels.

Signup trims text, normalizes roll numbers to uppercase, validates nonempty name/roll/
course, a positive integer year, and email syntax. No course-specific maximum year or
college email domain was invented. Name matching is case-insensitive. Roll lookup is
trimmed and case-insensitive for ASCII roll numbers. Signup checks for an existing roll
and inserts inside one Room transaction; duplicate taps or concurrent signups cannot
create two profiles through this flow. Legacy duplicate rows are rejected at sign-in
rather than silently selecting a student. The underlying schema remains version 1.

The saved session contains only a student ID, in app-private SharedPreferences. Reads
and commit() run on the background executor. Session restoration checks that the Room
profile still exists and clears stale IDs. Signup and preference storage are separate
stores: if session saving fails after insertion, the profile remains and can be used
to sign in again. No Room migration or reset is required for this step.

The forms are scrollable for small displays and keyboard use. Text fields have stable
IDs so Android restores drafts on recreation. Requests survive configuration changes
in ViewModels; buttons and Back are disabled during a submitted request. The profile
shows name, roll number, course/year, email, an initial avatar, and live registration /
attendance counts. Profile editing and the Settings link are not added in this slice.

The login card uses navy, with readable surface-colored input fields and a thin gold
button outline. The button itself stays navy per your accent-only instruction.
All screens use local fonts and the existing DayNight palette. The custom splash has
no forced waiting period; its thin line animates only while that screen is visible.
The system may display its own launch screen first on Android 12+, as expected for
the explicit SplashActivity structure requested in the specification.

### Manual check

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat installDebug
```

1. Open EventEase, choose Create Account, and submit empty fields to check validation.
2. Enter a name, roll number, course, positive year, and valid email; create the profile.
3. Confirm the profile fields and initial registration/attendance counts are correct.
4. Close/reopen the app; confirm the same profile opens without signing in again.
5. Log out, then sign in with a wrong name to check the error; use the correct name next.
6. Log out and try creating another profile with the same roll number; it must be rejected.
7. Rotate the device with a signup draft, and check that the entered values survive.
8. Repeat in system dark mode and check readability with the keyboard open.

### Branding still needed

Supply the approved TIMSCDR crest as a PNG/WebP. Save it as
app/src/main/res/drawable/timscdr_crest.png (or .webp), then set the college_crest
ImageView in view_brand.xml to android:src="@drawable/timscdr_crest" and remove its
android:visibility="gone" attribute. This shared layout is already used by Splash,
Login, and Signup; Settings/About and ticket watermark integration come later.

No API keys, permissions, or manual Gradle dependencies are required for Step 4.
The device disconnected before installation during this session, so the APK has not
been visually verified on a physical device. Automated tests cover repository/session
behavior, ViewModel validation, the actual signup -> profile -> logout -> login screens,
and night-mode draft restoration. Lint warnings about older pinned dependencies,
target SDK, custom splash, and pending launcher branding do not block the build.

Implementation references:
- https://developer.android.com/topic/libraries/architecture/viewmodel
- https://developer.android.com/topic/libraries/architecture/livedata

````
