# Finishing Pass — Full File Contents

Complete text for the source, resource, build, and test files covered by the finishing passes. Binary art is listed below; its photo sources and reuse licenses are in [ASSET-CREDITS.md](ASSET-CREDITS.md).

## Binary assets and removed artwork

Category photos: `event_photo_technical.jpg`, `event_photo_cultural.jpg`, `event_photo_sports.jpg`, `event_photo_workshop.jpg`, and `event_photo_seminar.jpg` under `app/src/main/res/drawable-nodpi/`.
Branding: `timscdr_crest_clear.png`, safe-area padded system splash asset `splash_crest_safe.png`, launcher foreground `ic_launcher_foreground.png`, monochrome mask `ic_launcher_monochrome.png`, and legacy launcher icon `app/src/main/res/mipmap-nodpi/ic_launcher.png`. The old opaque crest and generated category vector banners were removed. Photo attribution and reuse licenses are in [ASSET-CREDITS.md](ASSET-CREDITS.md).

## Complete text files

### `README.md`

````markdown
# EventEase — Step 9

Java + XML Android project for TIMSCDR. Steps 1–9 are implemented: foundation,
Room, repositories, local login/signup, event browsing, registration, confirmation,
locally generated QR tickets, the My Events upcoming/past lists, Settings, and the
optional local organizer demo flow.

## Terminal build

Use JDK 17 or newer, Android SDK Platform 35, and Android SDK Platform Tools.
No Android Studio is needed. AGP 9.2.1 uses the bundled Gradle 9.4.1 wrapper.
Min SDK is 24; target SDK is 34 (within the requested 34/35 range).
Compile SDK 35 is independent of the target SDK. Target 34 keeps enforced Android
15 edge-to-edge behavior out of this initial theme setup.

Set ANDROID_HOME to your SDK directory, or create an untracked local.properties:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

Install missing SDK packages using the Android command-line tools:

```powershell
sdkmanager "platforms;android-35" "platform-tools"
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
palettes. App-specific theme selection is available from Profile > Settings.

## Structure and next steps

Base package: com.timscdr.eventease.
The data/entity, data/dao, data/database, repository, viewmodel, ui, util, and
receiver packages follow the supplied specification. Empty directories are kept
in Git with empty .gitkeep files. The Room entities, DAOs, and AppDatabase are implemented.
Repositories and authentication/profile ViewModels are implemented.
The optional organizer demo flow creates local events and scans/checks in local tickets.
SplashActivity checks session state and routes to LoginActivity or DashboardActivity.
DashboardActivity hosts Events, My Events, and Profile tabs. Event browsing, details,
registration, confirmation, QR ticketing, and registered-event lists are implemented.
Settings persist theme and reminder choices. Organizer tools are available from Profile
and are labeled as a local demo because the schema has no role field or shared backend.

Event Detail reads through repositories and ViewModels. Registration uses an atomic
Room seat reservation and the schema's unique student/event constraint. ZXing
generates a local QR bitmap from a unique EVENTEASE-UUID registration code.
UI observes ViewModels; ViewModels depend on repository interfaces.
Room access is behind repositories to preserve a future backend swap.

## Assets and decisions

All eight light/dark palette roles match the supplied specification. Dark colors
override values through values-night/colors.xml; the shared DayNight theme does
not need a duplicate values-night/themes.xml. Navy fills buttons; gold is an
accent, following your explicit instruction over the spec's gold button examples.
Cards have no elevation and 10dp corners. Screen transitions fade subtly. Android's
short system launch transition uses a safely inset crest, then the full branded
SplashActivity stays visible for at least three seconds while the session is restored.
The branded screen offers a retry action if restoration fails.

Local static regular-weight TTFs are bundled in res/font. Sources:
- https://github.com/google/fonts/tree/main/ofl/playfairdisplay
- https://github.com/google/fonts/tree/main/ofl/inter

They were instantiated at their default axes using fontTools for API 24 support.
Their SIL Open Font Licenses are included in licenses/. Fonts never need runtime
network access. The Gradle 8.13 wrapper distribution checksum is pinned in
gradle-wrapper.properties.

The supplied TIMSCDR crest is cleaned into a transparent PNG for screen branding and
the ticket QR. The launcher uses an adaptive navy icon with the crest as its only
foreground graphic.

## Manual requirements

No API keys or manual dependency changes are needed to build the current project.
Room, lifecycle, AndroidX SplashScreen, ZXing, Glide, RecyclerView,
SwipeRefreshLayout, and Material dependencies are declared in app/build.gradle.
The manifest declares Internet, notification, and camera permissions for implemented
features. No Kotlin plugin or Kotlin source is used.

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

Historical file listings: docs/STEP-1-FILES.md through docs/STEP-4-FILES.md.
The latest finishing-pass file contents and image attributions are in
docs/FINISHING-FILES.md and docs/ASSET-CREDITS.md.

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
attendance counts. Settings and About are implemented in Step 8; profile editing is
not part of the supplied schema flow.

The login card uses navy, with readable surface-colored input fields and a thin gold
button outline. The button itself stays navy per your accent-only instruction.
All screens use local fonts and the existing DayNight palette. The system launch
transition leads into the full branded splash, which stays visible for at least three
seconds while the saved session is restored. On a storage error, it provides a Retry action.

### Manual check

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
.\gradlew.bat lintDebug
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

The supplied TIMSCDR crest is shown on Splash/Login/Signup, Settings/About, the ticket,
and inside the ticket QR. See docs/ASSET-CREDITS.md for the category-photo attributions.

### Step 6 — registration and QR ticket

The event detail action opens a prefilled, read-only student form. The student confirms
their saved details using the confirmation checkbox. Registration is stored using the
existing Registration entity, with a unique EVENTEASE-UUID registration code. A Room
transaction checks event capacity and inserts the registration together, so simultaneous
submissions cannot overbook seats. Duplicate registrations remain blocked by the existing
unique (eventId, studentId) index.

The schema has no separate attendee-detail fields, so edits in the registration form are
validated for completeness but the saved ticket displays the signed-in StudentProfile.
This preserves the specified schema and avoids changing account details as a side effect.
The ticket QR is generated locally by ZXing; no API key or new dependency is needed.
No additional permissions are required for this step.

Run `.\gradlew.bat assembleDebug` to build. Physical-device interaction and QR scanning
have not been run in this step; scanning belongs to the optional organizer flow.

### Step 7 — My Events

My Events observes the signed-in student's registrations and displays registered events
in Upcoming and Past sections. The list uses the existing repository queries; Upcoming
includes an event at its start timestamp, and it moves to Past immediately afterward.
The fragment refreshes these time-sensitive queries on resume and at the next event
boundary. Selecting a row opens that registration's QR ticket. No schema changes,
permissions, API keys, or dependencies were needed.

Build verification: `.\gradlew.bat assembleDebug` succeeds. Unit tests and on-device
interaction were not run for this step.

### Step 8 — Settings

The Profile tab opens Settings, which persists System/Light/Dark selection and applies
the saved mode at application startup. About shows the TIMSCDR crest and installed app
version. Logout remains available from Settings. The reminder switch stores its state,
requests Android 13+ notification permission when enabled, and schedules a local
NotificationCompat reminder one hour before each registered event with AlarmManager.
The same scheduler handles registrations created while reminders are on. The
`POST_NOTIFICATIONS` permission is declared in the manifest; no API key or dependency
was added.

Build verification: `.\gradlew.bat assembleDebug` succeeds. Unit tests and physical-device
notification delivery were not run for this step.

### Step 9 — Optional organizer demo

Profile > Organizer tools opens Create Event and Scan Ticket. Event creation validates
the supplied fields, lets the current student choose a future date/time and optional
banner image, then inserts through EventRepository. Scanning uses JourneyApps ZXing
Android Embedded 4.3.0 with its Activity Result contract. A ticket code is looked up
through RegistrationRepository and checked in through its atomic update; duplicate or
unknown codes are rejected. Organizer name comes from the signed-in StudentProfile.

This is local-only demonstration access. The provided StudentProfile schema contains no
organizer role and the project has no shared backend, so organizer tools are not access
controlled. CAMERA is declared in the manifest; the scanner requests it when opened.
No API key is required. `com.journeyapps:zxing-android-embedded:4.3.0` is declared in
app/build.gradle and resolves through the project's Maven Central setup.
The dependency and its API 24+ ScanContract usage follow the [JourneyApps README](https://github.com/journeyapps/zxing-android-embedded/blob/master/README.md).

Build verification: `.\gradlew.bat assembleDebug` succeeds. Camera scanning and event
creation have not been exercised on a physical device.

Implementation references:
- https://developer.android.com/topic/libraries/architecture/viewmodel
- https://developer.android.com/topic/libraries/architecture/livedata

### Presentation polish

The full branded splash displays for at least three seconds after Android's short system
launch transition. Event cards, details, My Events, and the organizer banner preview use
bundled category photographs for
technical, cultural, sports, workshop, and seminar events; custom banner URLs still
load through Glide, with the category photo used as a fallback. The app icon adapts
to Android's launcher mask and uses the crest on a theme-matched navy background.

Settings > About also includes a Team Contributions section with four workstreams
covering UI/navigation, data/persistence, registration/tickets, and testing/release.
Add the actual team members' names in the app before presenting; the project does not
assume or invent them.

Build verification: `assembleDebug` and all 33 unit tests pass. Main-source lint reports
zero errors; its unit-test analyzer may need an uncontended Windows lint cache if an IDE
Gradle process is analyzing the same project. AndroidX SplashScreen is declared in
app/build.gradle; no API key is required. Device-level visual behavior and physical QR
scanning have not been checked.
````

### `build.gradle`

````groovy
plugins {
    id 'com.android.application' version '9.2.1' apply false
}
````

### `app/build.gradle`

````groovy
plugins {
    id 'com.android.application'
}

android {
    namespace = 'com.timscdr.eventease'
    compileSdk 35

    defaultConfig {
        applicationId 'com.timscdr.eventease'
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName '1.0'
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += ['room.schemaLocation': "$projectDir/schemas".toString()]
            }
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.includeAndroidResources = true
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.core:core:1.13.1'
    implementation 'androidx.core:core-splashscreen:1.0.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    implementation 'com.google.zxing:core:3.5.3'
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.robolectric:robolectric:4.14.1'
    testImplementation 'androidx.arch.core:core-testing:2.2.0'
}
````

### `gradle/wrapper/gradle-wrapper.properties`

````properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
distributionSha256Sum=2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb
networkTimeout=60000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
````

### `gradle.properties`

````properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
android.defaults.buildfeatures.resvalues=true
android.sdk.defaultTargetSdkToCompileSdkIfUnset=false
android.enableAppCompileTimeRClass=false
android.usesSdkInManifest.disallowed=false
android.uniquePackageNames=false
android.dependency.useConstraints=true
android.r8.strictFullModeForKeepRules=false
android.r8.optimizedResourceShrinking=false
android.builtInKotlin=false
android.newDsl=false
````

### `app/src/main/AndroidManifest.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <application
        android:name=".EventEaseApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/full_backup_content"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.EventEase">
        <activity android:name=".ui.auth.LoginActivity" android:exported="false"
            android:windowSoftInputMode="adjustResize" />
        <activity android:name=".ui.auth.SignupActivity" android:exported="false"
            android:windowSoftInputMode="adjustResize" />
        <activity android:name=".ui.dashboard.DashboardActivity" android:exported="false" />
        <activity android:name=".ui.event.EventDetailActivity" android:exported="false" />
        <activity android:name=".ui.registration.RegistrationFormActivity" android:exported="false" android:windowSoftInputMode="adjustResize" />
        <activity android:name=".ui.registration.ConfirmationActivity" android:exported="false" />
        <activity android:name=".ui.registration.TicketQRActivity" android:exported="false" />
        <activity android:name=".ui.profile.SettingsActivity" android:exported="false" />
        <activity android:name=".ui.organizer.OrganizerActivity" android:exported="false" />
        <activity android:name=".ui.organizer.CreateEventActivity" android:exported="false" android:windowSoftInputMode="adjustResize" />
        <activity android:name=".ui.organizer.ScanTicketActivity" android:exported="false" />
        <receiver android:name=".receiver.EventReminderReceiver" android:enabled="true" android:exported="false" />
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

### `app/src/main/java/com/timscdr/eventease/ui/splash/SplashActivity.java`

````java
package com.timscdr.eventease.ui.splash;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

public class SplashActivity extends AppCompatActivity {
    private static final long MINIMUM_SPLASH_MS = 3_000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ObjectAnimator loadingLine;
    private boolean routed;
    private boolean minimumDisplayElapsed;
    private AuthState latestAuthState;
    private final Runnable minimumDisplay = () -> {
        minimumDisplayElapsed = true;
        routeIfReady();
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Let Android's brief launch splash transition to the complete branded screen below.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        main.postDelayed(minimumDisplay, MINIMUM_SPLASH_MS);
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
            latestAuthState = state;
            boolean failed = state.status == AuthState.Status.ERROR;
            findViewById(R.id.splash_error).setVisibility(failed ? View.VISIBLE : View.GONE);
            findViewById(R.id.retry).setVisibility(failed ? View.VISIBLE : View.GONE);
            routeIfReady();
        });
        if (model.state().getValue().status == AuthState.Status.IDLE) model.restoreSession();
    }

    @Override protected void onStart() { super.onStart(); loadingLine.start(); }
    @Override protected void onStop() { loadingLine.cancel(); super.onStop(); }
    @Override protected void onDestroy() { main.removeCallbacks(minimumDisplay); super.onDestroy(); }

    private void routeIfReady() {
        if (routed || !minimumDisplayElapsed || latestAuthState == null) return;
        if (latestAuthState.status == AuthState.Status.SIGNED_IN) {
            routed = true;
            ScreenNavigation.home(this);
        } else if (latestAuthState.status == AuthState.Status.SIGNED_OUT) {
            routed = true;
            ScreenNavigation.login(this);
        }
    }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/registration/TicketQRActivity.java`

````java
package com.timscdr.eventease.ui.registration;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.WriterException;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.QRCodeGenerator;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.TicketViewModel;

public final class TicketQRActivity extends AppCompatActivity {
    static final String EVENT_ID = "event_id", STUDENT_ID = "student_id", REGISTRATION_ID = "registration_id";
    public static Intent intent(Context context, long eventId, long studentId, long registrationId) {
        return new Intent(context, TicketQRActivity.class).putExtra(EVENT_ID, eventId)
                .putExtra(STUDENT_ID, studentId).putExtra(REGISTRATION_ID, registrationId);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_ticket_qr);
        ((MaterialToolbar) findViewById(R.id.ticket_toolbar)).setNavigationOnClickListener(v -> finish());
        long eventId = getIntent().getLongExtra(EVENT_ID, 0), studentId = getIntent().getLongExtra(STUDENT_ID, 0);
        long ticketId = getIntent().getLongExtra(REGISTRATION_ID, 0);
        TicketViewModel model = new ViewModelProvider(this,
                new EventEaseViewModelFactory(this, studentId, eventId, ticketId)).get(TicketViewModel.class);
        model.state().observe(this, this::render);
    }
    private void render(TicketViewModel.State state) {
        findViewById(R.id.ticket_loading).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        boolean ready = !state.loading && !state.unavailable
                && state.event != null && state.profile != null && state.registration != null;
        findViewById(R.id.ticket_card).setVisibility(ready ? View.VISIBLE : View.GONE);
        findViewById(R.id.ticket_data_error).setVisibility(state.unavailable ? View.VISIBLE : View.GONE);
        if (!ready) return;
        findViewById(R.id.ticket_error).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.ticket_event_title)).setText(state.event.title);
        ((TextView) findViewById(R.id.ticket_date)).setText(DateTimeUtils.dateTime(this, state.event.dateTime));
        ((TextView) findViewById(R.id.ticket_venue)).setText(state.event.venue);
        ((TextView) findViewById(R.id.ticket_student)).setText(getString(R.string.ticket_student_identity,
                state.profile.name, state.profile.rollNo));
        ((TextView) findViewById(R.id.ticket_code)).setText(state.registration.registrationCode);
        Bitmap crest = BitmapFactory.decodeResource(getResources(), R.drawable.timscdr_crest_clear);
        if (crest == null) {
            findViewById(R.id.ticket_error).setVisibility(View.VISIBLE);
            return;
        }
        try {
            Bitmap qr = QRCodeGenerator.create(state.registration.registrationCode, 640, crest,
                    ContextCompat.getColor(this, R.color.primary), ContextCompat.getColor(this, R.color.accent));
            ((ImageView) findViewById(R.id.ticket_qr)).setImageBitmap(qr);
        } catch (WriterException | IllegalArgumentException error) {
            findViewById(R.id.ticket_error).setVisibility(View.VISIBLE);
        } finally {
            crest.recycle();
        }
    }
}
````

### `app/src/main/java/com/timscdr/eventease/viewmodel/TicketViewModel.java`

````java
package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.repository.RegistrationRepository;

public final class TicketViewModel extends ViewModel {
    public static final class State {
        public final Event event; public final StudentProfile profile; public final Registration registration;
        public final boolean loading, unavailable;
        State(Event event, StudentProfile profile, Registration registration, boolean loading, boolean unavailable) {
            this.event = event; this.profile = profile; this.registration = registration; this.loading = loading;
            this.unavailable = unavailable;
        }
    }
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private Event event; private StudentProfile profile; private Registration registration;
    private boolean eventLoaded, profileLoaded, registrationLoaded;
    private final long eventId, studentId;
    public TicketViewModel(EventRepository events, ProfileRepository profiles, RegistrationRepository registrations,
            long eventId, long studentId, long registrationId) {
        this.eventId = eventId;
        this.studentId = studentId;
        state.addSource(events.observeById(eventId), value -> { event = value; eventLoaded = true; publish(); });
        state.addSource(profiles.observeById(studentId), value -> { profile = value; profileLoaded = true; publish(); });
        state.addSource(registrations.observeById(registrationId), value -> {
            registration = value; registrationLoaded = true; publish();
        });
        publish();
    }
    public LiveData<State> state() { return state; }
    private void publish() {
        boolean loading = !eventLoaded || !profileLoaded || !registrationLoaded;
        boolean unavailable = !loading && (event == null || profile == null || registration == null
                || event.id != eventId || profile.id != studentId
                || registration.eventId != eventId || registration.studentId != studentId);
        state.setValue(new State(event, profile, registration, loading, unavailable));
    }
}
````

### `app/src/main/java/com/timscdr/eventease/viewmodel/RegistrationViewModel.java`

````java
package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import com.timscdr.eventease.repository.SettingsRepository;
import com.timscdr.eventease.util.EventReminderScheduler;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executor;

public final class RegistrationViewModel extends ViewModel {
    public static final class State {
        public final Event event;
        public final StudentProfile profile;
        public final Registration registration;
        public final boolean loading, submitting, full, started, alreadyRegistered, error, success;
        State(Event event, StudentProfile profile, Registration registration, boolean loading,
                boolean submitting, boolean full, boolean started, boolean alreadyRegistered, boolean error, boolean success) {
            this.event = event; this.profile = profile; this.registration = registration;
            this.loading = loading; this.submitting = submitting; this.full = full;
            this.started = started; this.alreadyRegistered = alreadyRegistered;
            this.error = error; this.success = success;
        }
    }

    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Executor main;
    private final SettingsRepository settings;
    private final EventReminderScheduler reminderScheduler;
    private Event event;
    private StudentProfile profile;
    private Registration registration;
    private Integer count;
    private boolean eventLoaded, profileLoaded, registrationLoaded, submitting, full, error, success, cleared;

    public RegistrationViewModel(EventRepository events, ProfileRepository profiles,
            RegistrationRepository registrations, SettingsRepository settings, EventReminderScheduler reminderScheduler,
            long eventId, long studentId, Executor main) {
        this.events = events; this.registrations = registrations; this.settings = settings;
        this.reminderScheduler = reminderScheduler; this.main = main;
        state.addSource(events.observeById(eventId), value -> { event = value; eventLoaded = true; publish(); });
        state.addSource(profiles.observeById(studentId), value -> { profile = value; profileLoaded = true; publish(); });
        state.addSource(registrations.observeForEventAndStudent(eventId, studentId), value -> {
            registration = value; registrationLoaded = true; publish();
        });
        state.addSource(registrations.observeCountForEvent(eventId), value -> { count = value; publish(); });
        publish();
    }

    public LiveData<State> state() { return state; }

    public void submit() {
        if (!eventLoaded || !profileLoaded || !registrationLoaded || count == null
                || event == null || profile == null || registration != null
                || !busy.compareAndSet(false, true)) return;
        if (event.dateTime <= System.currentTimeMillis()) {
            busy.set(false);
            publish();
            return;
        }
        if (count >= event.capacity) {
            busy.set(false);
            full = true;
            publish();
            return;
        }
        submitting = true; error = false; publish();
        Registration ticket = new Registration();
        ticket.eventId = event.id;
        ticket.studentId = profile.id;
        ticket.registrationCode = "EVENTEASE-" + UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT);
        ticket.registeredAt = System.currentTimeMillis();
        registrations.registerIfCapacity(ticket, event.capacity).whenComplete((id, failure) -> {
            main.execute(() -> {
                submitting = false;
                if (!cleared) {
                    if (failure != null) error = true;
                    else if (id == -1L) full = true;
                    else {
                        success = true;
                        try {
                            if (settings.notificationsEnabled()) reminderScheduler.schedule(ticket, event);
                        } catch (RuntimeException reminderFailure) {
                            Log.w("EventEase", "Registration saved, but its reminder could not be scheduled.", reminderFailure);
                        }
                    }
                    publish();
                }
                busy.set(false);
            });
        });
    }

    private void publish() {
        boolean loading = !eventLoaded || !profileLoaded || !registrationLoaded || count == null;
        state.setValue(new State(event, profile, registration, loading, submitting, full,
                event != null && event.dateTime <= System.currentTimeMillis(), registration != null, error, success));
    }
    @Override protected void onCleared() { cleared = true; }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/registration/RegistrationFormActivity.java`

````java
package com.timscdr.eventease.ui.registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.RegistrationViewModel;

public final class RegistrationFormActivity extends AppCompatActivity {
    public static final String EVENT_ID = "event_id";
    public static final String STUDENT_ID = "student_id";
    private RegistrationViewModel model;
    private boolean routed;
    private boolean completedNavigation;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_registration_form);
        long eventId = getIntent().getLongExtra(EVENT_ID, 0), studentId = getIntent().getLongExtra(STUDENT_ID, 0);
        MaterialToolbar toolbar = findViewById(R.id.registration_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, studentId, eventId))
                .get(RegistrationViewModel.class);
        MaterialButton submit = findViewById(R.id.confirm_registration);
        submit.setOnClickListener(v -> {
            RegistrationViewModel.State current = model.state().getValue();
            if (current != null && current.registration != null) { openTicket(current); return; }
            if (!((CheckBox) findViewById(R.id.confirm_details)).isChecked()) {
                ((TextView) findViewById(R.id.registration_message)).setText(R.string.confirm_details_required);
                findViewById(R.id.registration_message).setVisibility(View.VISIBLE); return;
            }
            model.submit();
        });
        model.state().observe(this, this::render);
    }
    private void render(RegistrationViewModel.State state) {
        findViewById(R.id.registration_loading).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.registration_content).setVisibility(state.loading ? View.GONE : View.VISIBLE);
        if (state.event != null) {
            ((TextView) findViewById(R.id.registration_event_title)).setText(state.event.title);
            ((TextView) findViewById(R.id.registration_event_summary)).setText(
                    getString(R.string.event_summary, DateTimeUtils.dateTime(this, state.event.dateTime), state.event.venue));
        }
        if (state.profile != null && !routed) {
            ((EditText) findViewById(R.id.registration_name)).setText(state.profile.name);
            ((EditText) findViewById(R.id.registration_roll)).setText(state.profile.rollNo);
            ((EditText) findViewById(R.id.registration_email)).setText(state.profile.email);
            routed = true;
        }
        TextView message = findViewById(R.id.registration_message);
        int messageId = state.full ? R.string.event_full : state.started ? R.string.event_started
                : state.alreadyRegistered ? R.string.already_registered
                : state.event == null || state.profile == null ? R.string.registration_unavailable
                : state.error ? R.string.registration_error : 0;
        message.setVisibility(messageId == 0 ? View.GONE : View.VISIBLE);
        if (messageId != 0) message.setText(messageId);
        MaterialButton button = findViewById(R.id.confirm_registration);
        button.setText(state.alreadyRegistered ? R.string.view_ticket : R.string.confirm_registration);
        button.setEnabled(!state.loading && !state.submitting && (state.alreadyRegistered
                || (!state.full && !state.started && state.event != null && state.profile != null)));
        if (state.submitting) button.setText(R.string.registration_saving);
        if (state.success && state.registration != null && !completedNavigation) {
            completedNavigation = true;
            startActivity(ConfirmationActivity.intent(this, state.event, state.profile, state.registration));
            finish();
        }
    }
    private void openTicket(RegistrationViewModel.State state) {
        if (state.event != null && state.profile != null && state.registration != null)
            startActivity(TicketQRActivity.intent(this, state.event.id, state.profile.id, state.registration.id));
    }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/registration/ConfirmationActivity.java`

````java
package com.timscdr.eventease.ui.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.util.DateTimeUtils;

public final class ConfirmationActivity extends AppCompatActivity {
    static final String EVENT_ID = "event_id", STUDENT_ID = "student_id", REGISTRATION_ID = "registration_id";
    static final String TITLE = "event_title", DATE = "event_date", VENUE = "event_venue";
    static Intent intent(Context context, Event event, StudentProfile profile, Registration registration) {
        return new Intent(context, ConfirmationActivity.class).putExtra(EVENT_ID, event.id)
                .putExtra(STUDENT_ID, profile.id).putExtra(REGISTRATION_ID, registration.id)
                .putExtra(TITLE, event.title).putExtra(DATE, DateTimeUtils.dateTime(context, event.dateTime))
                .putExtra(VENUE, event.venue);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_confirmation);
        ((TextView) findViewById(R.id.confirmation_event_title)).setText(getIntent().getStringExtra(TITLE));
        ((TextView) findViewById(R.id.confirmation_event_summary)).setText(
                getString(R.string.event_summary, getIntent().getStringExtra(DATE), getIntent().getStringExtra(VENUE)));
        ((MaterialButton) findViewById(R.id.view_ticket)).setOnClickListener(v -> startActivity(TicketQRActivity.intent(
                this, getIntent().getLongExtra(EVENT_ID, 0), getIntent().getLongExtra(STUDENT_ID, 0),
                getIntent().getLongExtra(REGISTRATION_ID, 0))));
        findViewById(R.id.back_to_events).setOnClickListener(v -> finish());
    }
}
````

### `app/src/main/java/com/timscdr/eventease/viewmodel/EventEaseViewModelFactory.java`

````java
package com.timscdr.eventease.viewmodel;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.viewmodel.CreationExtras;
import android.os.Handler;
import android.os.Looper;
import com.timscdr.eventease.repository.AppRepositories;
import com.timscdr.eventease.util.AndroidEventReminderScheduler;
import com.timscdr.eventease.util.EventReminderScheduler;

/** Composition boundary; screen classes never acquire repositories or DAOs themselves. */
public final class EventEaseViewModelFactory implements ViewModelProvider.Factory {
    private final AppRepositories repositories;
    private final long studentId;
    private final long eventId;
    private final long ticketId;
    private final EventReminderScheduler reminderScheduler;

    public EventEaseViewModelFactory(Context context, long studentId) {
        this(context, studentId, 0);
    }

    public EventEaseViewModelFactory(Context context, long studentId, long eventId) {
        this(context, studentId, eventId, 0);
    }

    public EventEaseViewModelFactory(Context context, long studentId, long eventId, long ticketId) {
        repositories = AppRepositories.getInstance(context.getApplicationContext());
        reminderScheduler = new AndroidEventReminderScheduler(context.getApplicationContext());
        this.studentId = studentId;
        this.eventId = eventId;
        this.ticketId = ticketId;
    }

    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
        if (type == EventDetailViewModel.class) return type.cast(new EventDetailViewModel(
                repositories.events(), repositories.registrations(), eventId, studentId, System::currentTimeMillis));
        if (type == RegistrationViewModel.class) return type.cast(new RegistrationViewModel(
                repositories.events(), repositories.profiles(), repositories.registrations(), repositories.settings(),
                reminderScheduler, eventId, studentId,
                command -> new Handler(Looper.getMainLooper()).post(command)));
        if (type == TicketViewModel.class) return type.cast(new TicketViewModel(repositories.events(),
                repositories.profiles(), repositories.registrations(), eventId, studentId, ticketId));
        if (type == MyEventsViewModel.class) return type.cast(new MyEventsViewModel(
                repositories.events(), repositories.registrations(), studentId, System::currentTimeMillis));
        if (type == SettingsViewModel.class) return type.cast(new SettingsViewModel(repositories.settings(),
                repositories.events(), repositories.registrations(), reminderScheduler, studentId));
        if (type == CreateEventViewModel.class) return type.cast(new CreateEventViewModel(repositories.events(),
                repositories.profiles(), studentId, command -> new Handler(Looper.getMainLooper()).post(command)));
        if (type == ScanTicketViewModel.class) return type.cast(new ScanTicketViewModel(repositories.registrations(),
                repositories.profiles(), repositories.events(), command -> new Handler(Looper.getMainLooper()).post(command)));
        if (type == AuthViewModel.class) return type.cast(new AuthViewModel(repositories.auth()));
        if (type == ProfileViewModel.class) {
            return type.cast(new ProfileViewModel(repositories.profiles(), repositories.registrations(), studentId));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + type.getName());
    }

    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type, @NonNull CreationExtras extras) {
        if (type == EventListViewModel.class) {
            Handler handler = new Handler(Looper.getMainLooper());
            return type.cast(new EventListViewModel(repositories.events(), repositories.registrations(), studentId,
                    SavedStateHandleSupport.createSavedStateHandle(extras), command -> handler.post(command), System::currentTimeMillis));
        }
        return create(type);
    }
}
````

### `app/src/main/java/com/timscdr/eventease/util/AndroidEventReminderScheduler.java`

````java
package com.timscdr.eventease.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.receiver.EventReminderReceiver;

/** One local reminder per registration, scheduled one hour before its event. */
public final class AndroidEventReminderScheduler implements EventReminderScheduler {
    public static final String CHANNEL_ID = "eventease_event_reminders";
    private static final long REMINDER_OFFSET = 60L * 60L * 1000L;
    private final Context app;
    public AndroidEventReminderScheduler(Context context) { app = context.getApplicationContext(); }

    @Override public void schedule(Registration registration, Event event) {
        cancel(registration.registrationCode);
        long triggerAt = event.dateTime - REMINDER_OFFSET;
        if (triggerAt <= System.currentTimeMillis()) return;
        createChannel();
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt,
                pending(registration.registrationCode, event.title, event.venue, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    @Override public void cancel(String registrationCode) {
        if (registrationCode == null || registrationCode.isEmpty()) return;
        PendingIntent alarm = pending(registrationCode, "", "", PendingIntent.FLAG_NO_CREATE);
        if (alarm == null) return;
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.cancel(alarm);
        alarm.cancel();
    }

    private PendingIntent pending(String code, String title, String venue, int extraFlags) {
        Intent intent = new Intent(app, EventReminderReceiver.class)
                .setAction("com.timscdr.eventease.EVENT_REMINDER")
                .setData(Uri.parse("eventease://reminder/" + Uri.encode(code)))
                .putExtra(EventReminderReceiver.EXTRA_CODE, code)
                .putExtra(EventReminderReceiver.EXTRA_TITLE, title)
                .putExtra(EventReminderReceiver.EXTRA_VENUE, venue);
        int flags = extraFlags | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(app, 0, intent, flags);
    }

    private void createChannel() {
        NotificationManagerCompat.from(app).createNotificationChannel(new NotificationChannelCompat.Builder(
                CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(app.getString(com.timscdr.eventease.R.string.reminder_channel_name)).build());
    }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/organizer/CreateEventActivity.java`

````java
package com.timscdr.eventease.ui.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.EventBannerImages;
import com.timscdr.eventease.viewmodel.CreateEventViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import java.util.Calendar;

public final class CreateEventActivity extends AppCompatActivity {
    private static final String DATE_TIME = "event_date_time", BANNER_URI = "event_banner_uri";
    private CreateEventViewModel model;
    private long eventDateTime;
    private Uri bannerUri;
    private final ActivityResultLauncher<String[]> imagePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { }
                bannerUri = uri;
                ((TextView) findViewById(R.id.banner_selection)).setText(uri.getLastPathSegment());
                Glide.with(this).load(uri).centerCrop().placeholder(R.drawable.event_banner_placeholder)
                        .into((ImageView) findViewById(R.id.event_banner_preview));
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_create_event);
        ((MaterialToolbar) findViewById(R.id.create_event_toolbar)).setNavigationOnClickListener(v -> finish());
        long studentId = getIntent().getLongExtra(OrganizerActivity.STUDENT_ID, 0);
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, studentId)).get(CreateEventViewModel.class);
        AutoCompleteTextView category = findViewById(R.id.event_category_input);
        category.setAdapter(ArrayAdapter.createFromResource(this, R.array.event_categories,
                android.R.layout.simple_dropdown_item_1line));
        category.setOnClickListener(v -> category.showDropDown());
        category.setOnItemClickListener((parent, selected, position, id) -> previewDefaultBanner(category));
        eventDateTime = state == null ? 0 : state.getLong(DATE_TIME, 0);
        String restoredUri = state == null ? null : state.getString(BANNER_URI);
        if (restoredUri != null) { bannerUri = Uri.parse(restoredUri); loadBanner(); }
        updateDateLabel();
        findViewById(R.id.choose_event_datetime).setOnClickListener(v -> chooseDateTime());
        findViewById(R.id.choose_event_banner).setOnClickListener(v -> imagePicker.launch(new String[]{"image/*"}));
        findViewById(R.id.submit_event).setOnClickListener(v -> model.create(
                text(R.id.event_title_input), category.getText().toString(), text(R.id.event_description_input),
                text(R.id.event_venue_input), eventDateTime, bannerUri == null ? null : bannerUri.toString(),
                text(R.id.event_capacity_input)));
        model.state().observe(this, value -> {
            findViewById(R.id.event_saving).setVisibility(value.saving ? View.VISIBLE : View.GONE);
            findViewById(R.id.submit_event).setEnabled(!value.saving);
            TextView message = findViewById(R.id.create_event_message);
            message.setVisibility(value.error == null ? View.GONE : View.VISIBLE);
            if (value.error != null) message.setText(errorText(value.error));
            if (value.created) { Toast.makeText(this, R.string.event_created, Toast.LENGTH_LONG).show(); finish(); }
        });
    }

    private String text(int id) { return ((EditText) findViewById(id)).getText().toString(); }
    private String errorText(String error) {
        if ("title".equals(error)) return getString(R.string.create_event_title_required);
        if ("category".equals(error)) return getString(R.string.create_event_category_required);
        if ("description".equals(error)) return getString(R.string.create_event_description_required);
        if ("venue".equals(error)) return getString(R.string.create_event_venue_required);
        if ("date".equals(error)) return getString(R.string.create_event_date_required);
        if ("capacity".equals(error)) return getString(R.string.create_event_capacity_required);
        return getString(R.string.create_event_save_error);
    }
    private void chooseDateTime() {
        Calendar calendar = Calendar.getInstance();
        if (eventDateTime > 0) calendar.setTimeInMillis(eventDateTime);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, month); calendar.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (timePicker, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);
                eventDateTime = calendar.getTimeInMillis(); updateDateLabel();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void updateDateLabel() {
        ((MaterialButton) findViewById(R.id.choose_event_datetime)).setText(eventDateTime <= 0
                ? getString(R.string.choose_event_date) : DateTimeUtils.dateTime(this, eventDateTime));
    }
    private void loadBanner() {
        ((TextView) findViewById(R.id.banner_selection)).setText(bannerUri.getLastPathSegment());
        Glide.with(this).load(bannerUri).centerCrop().placeholder(R.drawable.event_banner_placeholder)
                .into((ImageView) findViewById(R.id.event_banner_preview));
    }
    private void previewDefaultBanner(AutoCompleteTextView category) {
        if (bannerUri != null) return;
        String value = category.getText().toString().trim();
        ImageView preview = findViewById(R.id.event_banner_preview);
        if (value.isEmpty()) {
            ((TextView) findViewById(R.id.banner_selection)).setText(R.string.no_banner_selected);
            Glide.with(this).load(R.drawable.event_banner_placeholder).centerCrop().into(preview);
            return;
        }
        ((TextView) findViewById(R.id.banner_selection)).setText(
                getString(R.string.default_banner_for_category, value));
        Glide.with(this).load(EventBannerImages.forCategory(value)).centerCrop().into(preview);
    }
    @Override protected void onPostResume() {
        super.onPostResume();
        if (bannerUri == null) previewDefaultBanner((AutoCompleteTextView) findViewById(R.id.event_category_input));
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong(DATE_TIME, eventDateTime);
        if (bannerUri != null) out.putString(BANNER_URI, bannerUri.toString());
        super.onSaveInstanceState(out);
    }
}
````

### `app/src/main/java/com/timscdr/eventease/util/QRCodeGenerator.java`

````java
package com.timscdr.eventease.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.EnumMap;
import java.util.Map;

/** Creates ticket QR bitmaps locally; no network service or API key is used. */
public final class QRCodeGenerator {
    private QRCodeGenerator() { }

    public static Bitmap create(String value, int size) throws WriterException {
        return create(value, size, null, Color.rgb(13, 37, 69), Color.rgb(201, 162, 39));
    }

    /** Creates a scan-friendly QR with an optional, clearly visible crest at its center. */
    public static Bitmap create(String value, int size, Bitmap logo, int foregroundColor, int accentColor)
            throws WriterException {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("QR value must not be empty.");
        if (size < 128) throw new IllegalArgumentException("QR size must be at least 128 pixels.");
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 4);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        int[] row = new int[size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) row[x] = matrix.get(x, y) ? foregroundColor : Color.WHITE;
            bitmap.setPixels(row, 0, size, 0, y, size, 1);
        }
        if (logo != null) drawCenterLogo(bitmap, logo, accentColor);
        return bitmap;
    }

    private static void drawCenterLogo(Bitmap qr, Bitmap logo, int accentColor) {
        int centerSize = Math.round(qr.getWidth() * 0.235f);
        float left = (qr.getWidth() - centerSize) / 2f;
        float top = (qr.getHeight() - centerSize) / 2f;
        float radius = Math.max(8f, centerSize * 0.09f);
        RectF backing = new RectF(left, top, left + centerSize, top + centerSize);
        Canvas canvas = new Canvas(qr);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(backing, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, qr.getWidth() * 0.004f));
        paint.setColor(accentColor);
        canvas.drawRoundRect(backing, radius, radius, paint);
        int inset = Math.round(centerSize * 0.08f);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, centerSize - inset * 2,
                centerSize - inset * 2, true);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(scaledLogo, left + inset, top + inset, paint);
        if (scaledLogo != logo) scaledLogo.recycle();
    }
}
````

### `app/src/main/java/com/timscdr/eventease/util/EventBannerImages.java`

````java
package com.timscdr.eventease.util;

import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import java.util.Locale;

/** Bundled category photos ensure every event has artwork without an uploaded banner. */
public final class EventBannerImages {
    private EventBannerImages() { }
    public static int forEvent(Event event) {
        return forCategory(event == null ? null : event.category);
    }

    public static int forCategory(String value) {
        String category = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (category) {
            case "technical": return R.drawable.event_photo_technical;
            case "cultural": return R.drawable.event_photo_cultural;
            case "sports": return R.drawable.event_photo_sports;
            case "workshop": return R.drawable.event_photo_workshop;
            case "seminar": return R.drawable.event_photo_seminar;
            default: return R.drawable.event_banner_placeholder;
        }
    }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/dashboard/EventAdapter.java`

````java
package com.timscdr.eventease.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.EventBannerImages;
import com.timscdr.eventease.viewmodel.EventListState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

public final class EventAdapter extends ListAdapter<EventAdapter.Row, EventAdapter.Holder> {
    private final LongConsumer open;
    public EventAdapter(LongConsumer open) {
        super(new DiffUtil.ItemCallback<Row>() {
            @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) { return a.event.id == b.event.id; }
            @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                return a.registered == b.registered && a.event.dateTime == b.event.dateTime
                        && a.event.title.equals(b.event.title) && a.event.category.equals(b.event.category)
                        && a.event.venue.equals(b.event.venue) && Objects.equals(a.event.bannerImageUrl, b.event.bannerImageUrl);
            }
        });
        this.open = open;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }
    public void show(EventListState state) {
        List<Row> rows = new ArrayList<>();
        for (Event event : state.events) rows.add(new Row(event, state.registeredIds.contains(event.id)));
        submitList(rows);
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = getItem(position);
        Event event = row.event;
        holder.title.setText(event.title);
        holder.category.setText(event.category);
        holder.date.setText(DateTimeUtils.dateTime(holder.itemView.getContext(), event.dateTime));
        holder.venue.setText(event.venue);
        holder.badge.setVisibility(row.registered ? View.VISIBLE : View.GONE);
        holder.button.setVisibility(row.registered ? View.GONE : View.VISIBLE);
        holder.button.setOnClickListener(view -> open.accept(event.id));
        holder.itemView.setOnClickListener(view -> open.accept(event.id));
        int banner = EventBannerImages.forEvent(event);
        Glide.with(holder.banner).load(event.bannerImageUrl).centerCrop()
                .placeholder(banner).fallback(banner).error(banner).into(holder.banner);
    }
    @Override public void onViewRecycled(@NonNull Holder holder) {
        // RecyclerView can recycle rows while the owning Activity is being destroyed.
        Glide.with(holder.itemView.getContext().getApplicationContext()).clear(holder.banner);
        super.onViewRecycled(holder);
    }
    static final class Row {
        final Event event;
        final boolean registered;
        Row(Event event, boolean registered) { this.event = event; this.registered = registered; }
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title, category, date, venue, badge;
        final ImageView banner;
        final MaterialButton button;
        Holder(View view) {
            super(view);
            title = view.findViewById(R.id.event_title); category = view.findViewById(R.id.event_category);
            date = view.findViewById(R.id.event_date); venue = view.findViewById(R.id.event_venue);
            badge = view.findViewById(R.id.registered_badge); banner = view.findViewById(R.id.event_banner);
            button = view.findViewById(R.id.quick_register);
        }
    }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/myevents/MyEventsAdapter.java`

````java
package com.timscdr.eventease.ui.myevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.EventBannerImages;
import com.timscdr.eventease.viewmodel.MyEventsState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

final class MyEventsAdapter extends ListAdapter<MyEventsAdapter.Row, RecyclerView.ViewHolder> {
    private static final int SECTION = 0, EVENT = 1;
    private final LongConsumer openTicket;
    MyEventsAdapter(LongConsumer openTicket) {
        super(new DiffUtil.ItemCallback<Row>() {
            @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                if (a.type != b.type) return false;
                return a.type == SECTION ? a.section == b.section : a.event.id == b.event.id;
            }
            @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                if (a.type == SECTION) return a.section == b.section;
                return a.event.title.equals(b.event.title) && a.event.category.equals(b.event.category)
                        && a.event.dateTime == b.event.dateTime && a.event.venue.equals(b.event.venue)
                        && Objects.equals(a.event.bannerImageUrl, b.event.bannerImageUrl);
            }
        });
        this.openTicket = openTicket;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    void show(MyEventsState state) {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.section(R.string.upcoming_my_events));
        for (Event event : state.upcoming) rows.add(Row.event(event));
        rows.add(Row.section(R.string.past_events));
        for (Event event : state.past) rows.add(Row.event(event));
        submitList(rows);
    }

    @Override public int getItemViewType(int position) { return getItem(position).type; }
    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (type == SECTION) return new SectionHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_myevents_section, parent, false));
        return new EventHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = getItem(position);
        if (holder instanceof SectionHolder) {
            ((SectionHolder) holder).title.setText(row.section);
            return;
        }
        EventHolder item = (EventHolder) holder;
        Event event = row.event;
        item.title.setText(event.title);
        item.category.setText(event.category);
        item.date.setText(DateTimeUtils.dateTime(item.itemView.getContext(), event.dateTime));
        item.venue.setText(event.venue);
        item.badge.setText(R.string.open_qr_ticket);
        item.badge.setVisibility(View.VISIBLE);
        item.button.setVisibility(View.GONE);
        item.itemView.setContentDescription(item.itemView.getContext().getString(R.string.open_ticket_for, event.title));
        item.itemView.setOnClickListener(view -> openTicket.accept(event.id));
        int banner = EventBannerImages.forEvent(event);
        Glide.with(item.banner).load(event.bannerImageUrl).centerCrop()
                .placeholder(banner).fallback(banner).error(banner).into(item.banner);
    }
    @Override public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof EventHolder) {
            EventHolder event = (EventHolder) holder;
            Glide.with(event.itemView.getContext().getApplicationContext()).clear(event.banner);
        }
        super.onViewRecycled(holder);
    }

    static final class Row {
        final int type, section; final Event event;
        private Row(int type, int section, Event event) { this.type = type; this.section = section; this.event = event; }
        static Row section(int title) { return new Row(SECTION, title, null); }
        static Row event(Event value) { return new Row(EVENT, 0, value); }
    }
    private static final class SectionHolder extends RecyclerView.ViewHolder {
        final TextView title;
        SectionHolder(View view) { super(view); title = view.findViewById(R.id.section_title); }
    }
    private static final class EventHolder extends RecyclerView.ViewHolder {
        final TextView title, category, date, venue, badge; final ImageView banner; final MaterialButton button;
        EventHolder(View view) {
            super(view); title = view.findViewById(R.id.event_title); category = view.findViewById(R.id.event_category);
            date = view.findViewById(R.id.event_date); venue = view.findViewById(R.id.event_venue);
            badge = view.findViewById(R.id.registered_badge); banner = view.findViewById(R.id.event_banner);
            button = view.findViewById(R.id.quick_register);
        }
    }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/event/EventDetailActivity.java`

````java
package com.timscdr.eventease.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.EventBannerImages;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.ui.registration.RegistrationFormActivity;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventDetailViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;

public final class EventDetailActivity extends AppCompatActivity {
    public static final String EVENT_ID = "event_id";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private EventDetailViewModel model;
    private AuthViewModel auth;
    private Event event;
    private boolean signedIn, routed;
    private long activeStudent;
    private final Runnable tick = () -> { if (model != null) model.tick(); };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        MaterialToolbar toolbar = findViewById(R.id.detail_toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_share || event == null || !signedIn) return false;
            String details = event.title + "\n" + DateTimeUtils.dateTime(this, event.dateTime)
                    + "\n" + event.venue + "\n" + event.organizerName;
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, details), getString(R.string.share_event)));
            return true;
        });
        findViewById(R.id.register_now).setOnClickListener(view -> {
            if (event == null || activeStudent <= 0) return;
            startActivity(new Intent(this, RegistrationFormActivity.class)
                    .putExtra(RegistrationFormActivity.EVENT_ID, event.id)
                    .putExtra(RegistrationFormActivity.STUDENT_ID, activeStudent));
        });
        auth = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(AuthViewModel.class);
        findViewById(R.id.detail_retry).setOnClickListener(view -> auth.restoreSession());
        auth.state().observe(this, state -> {
            signedIn = state.status == AuthState.Status.SIGNED_IN;
            boolean failed = state.status == AuthState.Status.ERROR;
            findViewById(R.id.detail_content).setVisibility(signedIn ? View.VISIBLE : View.GONE);
            findViewById(R.id.detail_status).setVisibility(signedIn ? View.GONE : View.VISIBLE);
            ((TextView) findViewById(R.id.detail_status)).setText(failed ? R.string.error_storage : R.string.loading);
            findViewById(R.id.detail_retry).setVisibility(failed ? View.VISIBLE : View.GONE);
            toolbar.getMenu().findItem(R.id.action_share).setEnabled(signedIn && event != null);
            if (signedIn) {
                if (model == null || activeStudent != state.studentId) {
                    if (model != null) model.state().removeObservers(this);
                    activeStudent = state.studentId;
                    long eventId = getIntent().getLongExtra(EVENT_ID, 0);
                    model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, activeStudent, eventId))
                            .get("event:" + eventId + ":student:" + activeStudent, EventDetailViewModel.class);
                    model.state().observe(this, this::render);
                } else render(model.state().getValue());
            } else if (state.status == AuthState.Status.SIGNED_OUT && !routed) {
                routed = true; ScreenNavigation.login(this);
            }
        });
    }

    private void render(EventDetailViewModel.State state) {
        if (state == null || !signedIn) return;
        event = state.event;
        boolean ready = event != null && !state.loading;
        findViewById(R.id.detail_content).setVisibility(ready ? View.VISIBLE : View.GONE);
        findViewById(R.id.detail_status).setVisibility(ready ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.detail_status)).setText(state.loading ? R.string.loading : R.string.event_missing);
        ((MaterialToolbar) findViewById(R.id.detail_toolbar)).getMenu().findItem(R.id.action_share).setEnabled(ready);
        if (ready) {
            text(R.id.detail_title, event.title);
            text(R.id.detail_category, event.category);
            text(R.id.detail_organizer, event.organizerName);
            text(R.id.detail_date, DateTimeUtils.dateTime(this, event.dateTime));
            text(R.id.detail_venue, event.venue);
            text(R.id.detail_description, event.description);
            text(R.id.detail_capacity, getString(R.string.seats_available, state.available, event.capacity));
            ImageView banner = findViewById(R.id.detail_banner);
            int artwork = EventBannerImages.forEvent(event);
            Glide.with(this).load(event.bannerImageUrl).centerCrop().placeholder(artwork)
                    .fallback(artwork).error(artwork).into(banner);
            MaterialButton button = findViewById(R.id.register_now);
            button.setText(state.registered ? R.string.already_registered : state.started ? R.string.event_started
                    : state.available == 0 ? R.string.event_full : R.string.register_now);
            button.setEnabled(!state.registered && !state.started && state.available > 0);
        }
        handler.removeCallbacks(tick);
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED))
            handler.postDelayed(tick, model.nextTickDelay());
    }
    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
    @Override protected void onStart() { super.onStart(); auth.restoreSession(); }
    @Override protected void onResume() { super.onResume(); if (model != null) model.tick(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/profile/SettingsActivity.java`

````java
package com.timscdr.eventease.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;
import com.timscdr.eventease.viewmodel.AuthState;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.SettingsViewModel;

public final class SettingsActivity extends AppCompatActivity {
    public static final String STUDENT_ID = "student_id";
    private SettingsViewModel settings;
    private SwitchMaterial notifications;
    private boolean rendering, pendingPermission;
    private final ActivityResultLauncher<String> permissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (pendingPermission && granted) settings.setNotificationsEnabled(true);
                else {
                    rendering = true; notifications.setChecked(false); rendering = false;
                    Snackbar.make(notifications, R.string.notification_permission_denied, Snackbar.LENGTH_LONG).show();
                }
                pendingPermission = false;
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_settings);
        long studentId = getIntent().getLongExtra(STUDENT_ID, 0);
        ((MaterialToolbar) findViewById(R.id.settings_toolbar)).setNavigationOnClickListener(v -> finish());
        settings = new ViewModelProvider(this, new EventEaseViewModelFactory(this, studentId))
                .get(SettingsViewModel.class);
        AuthViewModel auth = new ViewModelProvider(this,
                new EventEaseViewModelFactory(this, studentId)).get(AuthViewModel.class);
        auth.state().observe(this, value -> {
            if (value.status == AuthState.Status.SIGNED_OUT) ScreenNavigation.login(this);
        });
        findViewById(R.id.settings_logout).setOnClickListener(v -> auth.logout());
        String version;
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException impossible) { version = "1.0"; }
        ((TextView) findViewById(R.id.about_version)).setText(getString(R.string.about_version, version));

        RadioGroup themeOptions = findViewById(R.id.theme_options);
        themeOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (rendering) return;
            String mode = checkedId == R.id.theme_light ? "light"
                    : checkedId == R.id.theme_dark ? "dark" : "system";
            settings.setThemeMode(mode);
        });
        notifications = findViewById(R.id.notifications_toggle);
        notifications.setOnCheckedChangeListener((button, checked) -> {
            if (rendering) return;
            if (!checked) { settings.setNotificationsEnabled(false); return; }
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = true;
                rendering = true; notifications.setChecked(false); rendering = false;
                permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else settings.setNotificationsEnabled(true);
        });
        settings.state().observe(this, value -> {
            rendering = true;
            int selection = "light".equals(value.themeMode) ? R.id.theme_light
                    : "dark".equals(value.themeMode) ? R.id.theme_dark : R.id.theme_system;
            themeOptions.check(selection);
            notifications.setChecked(value.notificationsEnabled);
            rendering = false;
            int desired = "light".equals(value.themeMode) ? AppCompatDelegate.MODE_NIGHT_NO
                    : "dark".equals(value.themeMode) ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (AppCompatDelegate.getDefaultNightMode() != desired) AppCompatDelegate.setDefaultNightMode(desired);
        });
    }
}
````

### `app/src/main/java/com/timscdr/eventease/ui/auth/LoginActivity.java`

````java
package com.timscdr.eventease.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.ActivityOptionsCompat;
import com.timscdr.eventease.R;

public final class LoginActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindForm(R.layout.activity_login);
        findViewById(R.id.submit).setOnClickListener(view -> model.signIn(input(R.id.name), input(R.id.roll)));
        findViewById(R.id.alternate).setOnClickListener(view -> {
            startActivity(new Intent(this, SignupActivity.class), ActivityOptionsCompat.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle());
        });
    }
}
````

### `app/src/main/java/com/timscdr/eventease/util/ScreenNavigation.java`

````java
package com.timscdr.eventease.util;

import android.app.Activity;
import android.content.Intent;
import androidx.core.app.ActivityOptionsCompat;
import com.timscdr.eventease.ui.auth.LoginActivity;
import com.timscdr.eventease.ui.dashboard.DashboardActivity;

public final class ScreenNavigation {
    private ScreenNavigation() { }
    public static void home(Activity activity) { replaceTask(activity, DashboardActivity.class); }
    public static void login(Activity activity) { replaceTask(activity, LoginActivity.class); }

    private static void replaceTask(Activity activity, Class<?> destination) {
        activity.startActivity(new Intent(activity, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                ActivityOptionsCompat.makeCustomAnimation(activity, android.R.anim.fade_in,
                        android.R.anim.fade_out).toBundle());
        activity.finish();
    }
}
````

### `app/src/main/res/values/themes.xml`

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
    <style name="Theme.EventEase.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/primary</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/splash_crest_safe</item>
        <item name="windowSplashScreenIconBackgroundColor">@color/primary</item>
        <item name="postSplashScreenTheme">@style/Theme.EventEase.SplashContent</item>
    </style>
    <style name="Theme.EventEase.SplashContent" parent="Theme.EventEase">
        <item name="android:windowBackground">@color/primary</item>
    </style>
</resources>
````

### `app/src/main/res/values/strings.xml`

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
    <string name="loading">Please wait&#8230;</string>
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
    <string name="course_year">%1$s &#183; Year %2$d</string>
    <string name="profile_roll">Roll No. %1$s</string>
    <string name="splash_tagline">Discover. Participate. Belong.</string>
    <string name="crest_description">TIMSCDR crest</string>
    <string name="tab_events">Events</string>
    <string name="tab_my_events">My Events</string>
    <string name="tab_profile">Profile</string>
    <string name="upcoming_events">Upcoming Events</string>
    <string name="search_events">Search event titles</string>
    <string name="category_all">All</string>
    <string name="category_technical">Technical</string>
    <string name="category_cultural">Cultural</string>
    <string name="category_sports">Sports</string>
    <string name="category_workshop">Workshop</string>
    <string name="category_seminar">Seminar</string>
    <string name="register">Register</string>
    <string name="registered_badge">Registered &#10003;</string>
    <string name="register_now">Register Now</string>
    <string name="already_registered">Already Registered &#10003;</string>
    <string name="event_started">Event has started</string>
    <string name="event_full">Fully Booked</string>
    <string name="seats_available">Seats Available: %1$d / %2$d</string>
    <string name="about_event">About this event</string>
    <string name="share_event">Share event</string>
    <string name="event_detail">Event Details</string>
    <string name="event_missing">This event is no longer available.</string>
    <string name="catalog_empty">No events have been added yet. You can explore a sample campus-event catalog.</string>
    <string name="no_matching_events">No upcoming events match these filters. Try another category or search.</string>
    <string name="load_samples">Explore Sample Events</string>
    <string name="event_load_error">Events could not be loaded. Please try again.</string>
    <string name="registration_title">Event Registration</string>
    <string name="registration_event">You are registering for</string>
    <string name="registration_profile_notice">Registration uses the name and roll number saved in your profile. These details are read-only in this version.</string>
    <string name="registration_unavailable">The event or student profile could not be loaded. Return to Events and try again.</string>
    <string name="confirm_details">I confirm these details are correct</string>
    <string name="confirm_details_required">Please confirm your details before continuing.</string>
    <string name="confirm_registration">Confirm Registration</string>
    <string name="registration_saving">Saving registration…</string>
    <string name="registration_error">Registration could not be saved. You may already be registered; try opening the event again.</string>
    <string name="registration_success">You’re Registered!</string>
    <string name="view_ticket">View Ticket</string>
    <string name="back_to_events">Back to Events</string>
    <string name="ticket_title">Your Event Ticket</string>
    <string name="ticket_qr_description">QR ticket code for event entry</string>
    <string name="ticket_qr_error">The ticket QR code could not be created. Please reopen this ticket.</string>
    <string name="ticket_student_identity">%1$s &#183; %2$s</string>
    <string name="ticket_unavailable">This ticket, event, or student profile could not be found. Reopen it from My Events.</string>
    <string name="event_summary">%1$s &#183; %2$s</string>
    <string name="registration_checkmark">&#10003;</string>
    <string name="upcoming_my_events">Upcoming</string>
    <string name="past_events">Past</string>
    <string name="open_qr_ticket">Open QR Ticket</string>
    <string name="open_ticket_for">Open QR ticket for %1$s</string>
    <string name="my_events_empty">You have no registered events yet. Browse Events and register to see your tickets here.</string>
    <string name="settings_title">Settings</string>
    <string name="appearance">Appearance</string>
    <string name="theme_system">System default</string>
    <string name="theme_light">Light</string>
    <string name="theme_dark">Dark</string>
    <string name="notifications">Notifications</string>
    <string name="event_reminders">Remind me one hour before registered events</string>
    <string name="notification_permission_note">Android may ask permission to show reminders. Reminders are scheduled locally on this device.</string>
    <string name="notification_permission_denied">Allow notifications in system settings to receive event reminders.</string>
    <string name="about">About EventEase</string>
    <string name="about_description">EventEase helps TIMSCDR students discover college events, register, and keep their tickets in one place.</string>
    <string name="about_version">TIMSCDR · Version %1$s</string>
    <string name="reminder_channel_name">Event reminders</string>
    <string name="event_reminder_title">EventEase reminder</string>
    <string name="event_reminder_message">%1$s starts in about one hour.</string>
    <string name="event_reminder_details">%1$s starts in about one hour at %2$s.</string>
    <string name="organizer_tools_demo">Organizer tools · local demo</string>
    <string name="organizer_tools">Organizer Tools</string>
    <string name="organizer_demo_note">Local demonstration tools. Events and check-ins are stored only on this device. The project profile schema has no organizer role or shared backend.</string>
    <string name="create_event">Create Event</string>
    <string name="scan_ticket">Scan Ticket</string>
    <string name="event_title_label">Event title</string>
    <string name="event_category_label">Category</string>
    <string name="category_dropdown_description">Choose an event category</string>
    <string name="event_description_label">Description</string>
    <string name="event_venue_label">Venue</string>
    <string name="event_capacity_label">Capacity</string>
    <string name="choose_event_date">Choose date and time</string>
    <string name="event_banner_preview_description">Selected event banner preview</string>
    <string name="no_banner_selected">No banner selected (optional)</string>
    <string name="default_banner_for_category">Using the %1$s category photo</string>
    <string name="choose_banner">Choose banner image</string>
    <string name="save_event">Save Event</string>
    <string name="event_created">Event created and saved on this device.</string>
    <string name="create_event_title_required">Enter an event title.</string>
    <string name="create_event_category_required">Choose an event category.</string>
    <string name="create_event_description_required">Enter an event description.</string>
    <string name="create_event_venue_required">Enter an event venue.</string>
    <string name="create_event_date_required">Choose a future date and time.</string>
    <string name="create_event_capacity_required">Enter a capacity of at least one.</string>
    <string name="create_event_save_error">The event could not be saved. Please try again.</string>
    <string name="scan_ticket_instructions">Scan the QR code shown on a registered EventEase ticket. A valid ticket is checked in once.</string>
    <string name="start_scan">Open Camera Scanner</string>
    <string name="scan_camera_prompt">Place the ticket QR code inside the frame</string>
    <string name="scan_cancelled">Scan cancelled.</string>
    <string name="scan_checking">Checking ticket…</string>
    <string name="scan_valid">Valid ticket · %1$s (%2$s) · %3$s checked in.</string>
    <string name="scan_already_used">This ticket is invalid or has already been checked in. %1$s</string>
    <string name="scan_invalid">No EventEase registration matches this QR code.</string>
    <string name="scan_storage_error">Ticket validation could not finish. Try again.</string>
    <string name="team_contributions">Team Contributions</string>
    <string name="team_contributions_subtitle">A four-part build plan for the Mobile Computing project. Add your teammates’ names beside their contribution areas before presenting.</string>
    <string name="team_contributions_details">01  Core Data &amp; Architecture\nRoom entities and DAOs · repositories · ViewModels\n\n02  Event Browsing &amp; Student Experience\nAuthentication · dashboard · search · event details · profile\n\n03  Registration &amp; Ticketing\nCapacity-safe registration · QR tickets · reminders · check-in\n\n04  Design, Settings &amp; Integration\nNavy and gold theme · XML screens · branding · device verification</string>
    <string-array name="event_categories">
        <item>Technical</item><item>Cultural</item><item>Sports</item><item>Workshop</item><item>Seminar</item>
    </string-array>
    <string name="back">Back</string>
</resources>
````

### `app/src/main/res/layout/activity_splash.xml`

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

### `app/src/main/res/layout/fragment_event_list.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
<com.google.android.material.appbar.MaterialToolbar android:id="@+id/events_toolbar" android:layout_width="match_parent" android:layout_height="?attr/actionBarSize" android:background="@color/primary" app:menu="@menu/events_toolbar">
    <LinearLayout android:layout_width="wrap_content" android:layout_height="match_parent" android:layout_gravity="start|center_vertical" android:gravity="center_vertical" android:orientation="horizontal">
        <ImageView android:id="@+id/events_brand_logo" android:layout_width="34dp" android:layout_height="40dp" android:scaleType="fitCenter" android:src="@drawable/timscdr_crest_clear" android:contentDescription="@string/crest_description" />
        <TextView android:id="@+id/events_brand_title" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="8dp" android:text="@string/app_name" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:textColor="@color/on_primary" android:textSize="22sp" android:maxLines="1" />
    </LinearLayout>
</com.google.android.material.appbar.MaterialToolbar>
<androidx.appcompat.widget.SearchView android:id="@+id/event_search" android:layout_width="match_parent" android:layout_height="wrap_content" app:iconifiedByDefault="false" app:queryHint="@string/search_events" android:visibility="gone" />
<TextView android:id="@+id/events_heading" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:text="@string/upcoming_events" android:paddingStart="20dp" android:paddingEnd="20dp" android:paddingTop="20dp"/>
<View android:layout_width="72dp" android:layout_height="1dp" android:layout_marginStart="20dp" android:layout_marginTop="8dp" android:layout_marginBottom="8dp" android:background="@drawable/gold_divider" />
<com.timscdr.eventease.ui.dashboard.CategoryFilterView android:id="@+id/category_filters" android:layout_width="match_parent" android:layout_height="wrap_content" />
<FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1"><androidx.swiperefreshlayout.widget.SwipeRefreshLayout android:id="@+id/swipe_refresh" android:layout_width="match_parent" android:layout_height="match_parent">
<androidx.recyclerview.widget.RecyclerView android:id="@+id/event_list" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="20dp" android:clipToPadding="false" />
</androidx.swiperefreshlayout.widget.SwipeRefreshLayout><LinearLayout android:id="@+id/list_feedback" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:layout_gravity="center" android:visibility="gone">
<TextView android:id="@+id/empty_list" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:text="@string/catalog_empty" android:padding="20dp" android:visibility="gone" android:gravity="center" android:accessibilityLiveRegion="polite"/>
<TextView android:id="@+id/list_error" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:text="@string/event_load_error" android:padding="16dp" android:visibility="gone" android:textColor="@color/error" android:accessibilityLiveRegion="polite"/>
<com.google.android.material.button.MaterialButton android:id="@+id/list_retry" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/retry" android:visibility="gone" android:layout_marginStart="20dp" android:layout_marginEnd="20dp"/>
<com.google.android.material.button.MaterialButton android:id="@+id/load_samples" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/load_samples" android:visibility="gone" android:layout_marginStart="20dp" android:layout_marginEnd="20dp" android:layout_marginBottom="16dp"/>
</LinearLayout></FrameLayout></LinearLayout>
````

### `app/src/main/res/layout/activity_settings.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <com.google.android.material.appbar.MaterialToolbar android:id="@+id/settings_toolbar" android:layout_width="match_parent" android:layout_height="?attr/actionBarSize" android:background="@color/primary" app:title="@string/settings_title" app:titleTextColor="@color/on_primary" app:navigationIcon="@drawable/ic_back" app:navigationIconTint="@color/on_primary" app:navigationContentDescription="@string/back" />
    <ScrollView android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="24dp">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/appearance" android:textAppearance="@style/TextAppearance.EventEase.Heading" />
            <View android:layout_width="72dp" android:layout_height="1dp" android:layout_marginTop="8dp" android:background="@drawable/gold_divider" />
            <RadioGroup android:id="@+id/theme_options" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:layout_marginTop="8dp">
                <RadioButton android:id="@+id/theme_system" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/theme_system" />
                <RadioButton android:id="@+id/theme_light" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/theme_light" />
                <RadioButton android:id="@+id/theme_dark" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/theme_dark" />
            </RadioGroup>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/notifications" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:layout_marginTop="24dp" />
            <View android:layout_width="72dp" android:layout_height="1dp" android:layout_marginTop="8dp" android:background="@drawable/gold_divider" />
            <com.google.android.material.switchmaterial.SwitchMaterial android:id="@+id/notifications_toggle" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:text="@string/event_reminders" android:layout_marginTop="8dp" />
            <TextView android:id="@+id/notification_permission_note" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/notification_permission_note" android:textAppearance="@style/TextAppearance.EventEase.Body" android:textSize="13sp" android:layout_marginTop="4dp" />
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/about" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:layout_marginTop="28dp" />
            <View android:layout_width="72dp" android:layout_height="1dp" android:layout_marginTop="8dp" android:background="@drawable/gold_divider" />
            <com.google.android.material.card.MaterialCardView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="16dp">
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center_horizontal" android:padding="20dp">
                    <ImageView android:layout_width="112dp" android:layout_height="96dp" android:src="@drawable/timscdr_crest_clear" android:scaleType="fitCenter" android:contentDescription="@string/crest_description" />
                    <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/about_description" android:textAppearance="@style/TextAppearance.EventEase.Body" android:gravity="center" android:layout_marginTop="12dp" />
                    <TextView android:id="@+id/about_version" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:gravity="center" android:layout_marginTop="12dp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/team_contributions" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:layout_marginTop="24dp" />
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/team_contributions_subtitle" android:textAppearance="@style/TextAppearance.EventEase.Body" android:layout_marginTop="6dp" />
            <com.google.android.material.card.MaterialCardView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp">
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/team_contributions_details" android:textAppearance="@style/TextAppearance.EventEase.Body" android:lineSpacingExtra="5dp" android:padding="20dp" />
            </com.google.android.material.card.MaterialCardView>
            <com.google.android.material.button.MaterialButton android:id="@+id/settings_logout" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="52dp" android:text="@string/logout" android:layout_marginTop="24dp" />
        </LinearLayout>
    </ScrollView>
</LinearLayout>
````

### `app/src/main/res/layout/activity_scan_ticket.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <com.google.android.material.appbar.MaterialToolbar android:id="@+id/scan_toolbar" android:layout_width="match_parent" android:layout_height="?attr/actionBarSize" android:background="@color/primary" app:title="@string/scan_ticket" app:titleTextColor="@color/on_primary" app:navigationIcon="@drawable/ic_back" app:navigationIconTint="@color/on_primary" app:navigationContentDescription="@string/back" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:gravity="center" android:padding="28dp">
        <ImageView android:layout_width="80dp" android:layout_height="64dp" android:src="@drawable/timscdr_crest_clear" android:contentDescription="@string/crest_description" />
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/scan_ticket_instructions" android:textAppearance="@style/TextAppearance.EventEase.Body" android:gravity="center" android:layout_marginTop="20dp" />
        <ProgressBar android:id="@+id/scan_progress" android:layout_width="36dp" android:layout_height="36dp" android:visibility="gone" android:layout_marginTop="20dp" />
        <TextView android:id="@+id/scan_result" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:gravity="center" android:visibility="gone" android:layout_marginTop="16dp" android:accessibilityLiveRegion="polite" />
        <com.google.android.material.button.MaterialButton android:id="@+id/scan_ticket_action" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="52dp" android:text="@string/start_scan" android:layout_marginTop="24dp" />
    </LinearLayout>
</LinearLayout>
````

### `app/src/main/res/layout/view_brand.xml`

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
        android:src="@drawable/timscdr_crest_clear"
        android:scaleType="fitCenter"
        android:contentDescription="@string/crest_description"
        />
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

### `app/src/main/res/layout/activity_ticket_qr.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/ticket_toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@color/primary"
        app:title="@string/ticket_title"
        app:titleTextColor="@color/on_primary"
        app:navigationIcon="@drawable/ic_back"
        app:navigationIconTint="@color/on_primary"
        app:navigationContentDescription="@string/back" />

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <ProgressBar
            android:id="@+id/ticket_loading"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_gravity="center"
            android:contentDescription="@string/loading" />

        <ScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:fillViewport="true">

            <LinearLayout
                android:id="@+id/ticket_card"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_margin="20dp"
                android:background="@drawable/registered_border"
                android:gravity="center_horizontal"
                android:orientation="vertical"
                android:padding="24dp"
                android:visibility="gone">

                <ImageView
                    android:layout_width="112dp"
                    android:layout_height="92dp"
                    android:src="@drawable/timscdr_crest_clear"
                    android:scaleType="fitCenter"
                    android:contentDescription="@string/crest_description" />

                <TextView
                    android:id="@+id/ticket_event_title"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:gravity="center"
                    android:textAppearance="@style/TextAppearance.EventEase.Heading" />

                <TextView
                    android:id="@+id/ticket_date"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:gravity="center"
                    android:textAppearance="@style/TextAppearance.EventEase.Body" />

                <TextView
                    android:id="@+id/ticket_venue"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:gravity="center"
                    android:textAppearance="@style/TextAppearance.EventEase.Body" />

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:layout_marginTop="20dp"
                    android:background="@drawable/gold_divider" />

                <TextView
                    android:id="@+id/ticket_student"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="18dp"
                    android:gravity="center"
                    android:textAppearance="@style/TextAppearance.EventEase.Body" />

                <ImageView
                    android:id="@+id/ticket_qr"
                    android:layout_width="240dp"
                    android:layout_height="240dp"
                    android:layout_marginTop="20dp"
                    android:contentDescription="@string/ticket_qr_description" />

                <TextView
                    android:id="@+id/ticket_code"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:gravity="center"
                    android:textIsSelectable="true"
                    android:textAppearance="@style/TextAppearance.EventEase.Body" />

                <TextView
                    android:id="@+id/ticket_error"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center"
                    android:text="@string/ticket_qr_error"
                    android:textColor="@color/error"
                    android:visibility="gone" />
            </LinearLayout>
        </ScrollView>

        <TextView
            android:id="@+id/ticket_data_error"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:layout_margin="24dp"
            android:accessibilityLiveRegion="polite"
            android:gravity="center"
            android:text="@string/ticket_unavailable"
            android:textAppearance="@style/TextAppearance.EventEase.Body"
            android:visibility="gone" />
    </FrameLayout>
</LinearLayout>
````

### `app/src/main/res/layout/activity_registration_form.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <com.google.android.material.appbar.MaterialToolbar android:id="@+id/registration_toolbar" android:layout_width="match_parent" android:layout_height="?attr/actionBarSize" android:background="@color/primary" app:title="@string/registration_title" app:titleTextColor="@color/on_primary" app:navigationIcon="@drawable/ic_back" app:navigationIconTint="@color/on_primary" app:navigationContentDescription="@string/back" />
    <ProgressBar android:id="@+id/registration_loading" android:layout_width="40dp" android:layout_height="40dp" android:layout_gravity="center" android:layout_margin="32dp" />
    <ScrollView android:id="@+id/registration_content" android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true" android:visibility="gone">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="24dp">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/registration_event" android:textAppearance="@style/TextAppearance.EventEase.Heading" />
            <TextView android:id="@+id/registration_event_title" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:layout_marginTop="12dp" />
            <TextView android:id="@+id/registration_event_summary" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:layout_marginTop="4dp" />
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/registration_profile_notice" android:textAppearance="@style/TextAppearance.EventEase.Body" android:layout_marginTop="16dp" />
            <View android:layout_width="72dp" android:layout_height="1dp" android:layout_marginTop="16dp" android:background="@drawable/gold_divider" />
            <EditText android:id="@+id/registration_name" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="@string/name_label" android:inputType="textPersonName" android:focusable="false" android:cursorVisible="false" android:importantForAutofill="no" android:layout_marginTop="16dp" />
            <EditText android:id="@+id/registration_roll" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="@string/roll_label" android:inputType="textCapCharacters" android:focusable="false" android:cursorVisible="false" android:importantForAutofill="no" />
            <EditText android:id="@+id/registration_email" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="@string/email_label" android:inputType="textEmailAddress" android:focusable="false" android:cursorVisible="false" android:importantForAutofill="no" />
            <CheckBox android:id="@+id/confirm_details" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/confirm_details" android:layout_marginTop="8dp" />
            <TextView android:id="@+id/registration_message" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:textColor="@color/error" android:visibility="gone" android:paddingTop="8dp" />
            <com.google.android.material.button.MaterialButton android:id="@+id/confirm_registration" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/confirm_registration" android:layout_marginTop="12dp" />
        </LinearLayout>
    </ScrollView>
</LinearLayout>
````

### `app/src/main/res/layout/activity_create_event.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <com.google.android.material.appbar.MaterialToolbar android:id="@+id/create_event_toolbar" android:layout_width="match_parent" android:layout_height="?attr/actionBarSize" android:background="@color/primary" app:title="@string/create_event" app:titleTextColor="@color/on_primary" app:navigationIcon="@drawable/ic_back" app:navigationIconTint="@color/on_primary" app:navigationContentDescription="@string/back" />
    <ScrollView android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="24dp">
            <EditText android:id="@+id/event_title_input" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="@string/event_title_label" android:inputType="textCapSentences" android:importantForAutofill="no" />
            <com.google.android.material.textfield.TextInputLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/event_category_label" android:layout_marginTop="8dp">
                <com.google.android.material.textfield.MaterialAutoCompleteTextView android:id="@+id/event_category_input" android:layout_width="match_parent" android:layout_height="wrap_content" android:inputType="none" android:popupBackground="@drawable/category_dropdown_background" android:contentDescription="@string/category_dropdown_description" />
            </com.google.android.material.textfield.TextInputLayout>
            <EditText android:id="@+id/event_description_input" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="112dp" android:gravity="top|start" android:hint="@string/event_description_label" android:inputType="textCapSentences|textMultiLine" android:importantForAutofill="no" android:layout_marginTop="8dp" />
            <EditText android:id="@+id/event_venue_input" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="@string/event_venue_label" android:inputType="textCapSentences" android:importantForAutofill="no" android:layout_marginTop="8dp" />
            <com.google.android.material.button.MaterialButton android:id="@+id/choose_event_datetime" style="@style/Widget.MaterialComponents.Button.OutlinedButton" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/choose_event_date" android:layout_marginTop="8dp" />
            <EditText android:id="@+id/event_capacity_input" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="@string/event_capacity_label" android:inputType="number" android:importantForAutofill="no" android:layout_marginTop="8dp" />
            <ImageView android:id="@+id/event_banner_preview" android:layout_width="match_parent" android:layout_height="160dp" android:layout_marginTop="16dp" android:scaleType="centerCrop" android:src="@drawable/event_banner_placeholder" android:contentDescription="@string/event_banner_preview_description" />
            <TextView android:id="@+id/banner_selection" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/no_banner_selected" android:textAppearance="@style/TextAppearance.EventEase.Body" android:layout_marginTop="8dp" />
            <com.google.android.material.button.MaterialButton android:id="@+id/choose_event_banner" style="@style/Widget.MaterialComponents.Button.OutlinedButton" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/choose_banner" android:layout_marginTop="4dp" />
            <TextView android:id="@+id/create_event_message" android:layout_width="match_parent" android:layout_height="wrap_content" android:textColor="@color/error" android:textAppearance="@style/TextAppearance.EventEase.Body" android:visibility="gone" android:layout_marginTop="12dp" />
            <ProgressBar android:id="@+id/event_saving" android:layout_width="32dp" android:layout_height="32dp" android:layout_gravity="center_horizontal" android:visibility="gone" android:layout_marginTop="12dp" />
            <com.google.android.material.button.MaterialButton android:id="@+id/submit_event" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="52dp" android:text="@string/save_event" android:layout_marginTop="12dp" />
        </LinearLayout>
    </ScrollView>
</LinearLayout>
````

### `app/src/main/res/layout/activity_confirmation.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:gravity="center" android:padding="28dp">
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/registration_checkmark" android:textColor="@color/success" android:textSize="56sp" android:contentDescription="@string/registration_success" />
    <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/registration_success" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:gravity="center" android:layout_marginTop="12dp" />
    <TextView android:id="@+id/confirmation_event_title" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:gravity="center" android:layout_marginTop="28dp" />
    <TextView android:id="@+id/confirmation_event_summary" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:gravity="center" android:layout_marginTop="8dp" />
    <com.google.android.material.button.MaterialButton android:id="@+id/view_ticket" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/view_ticket" android:layout_marginTop="32dp" />
    <com.google.android.material.button.MaterialButton android:id="@+id/back_to_events" style="@style/Widget.MaterialComponents.Button.OutlinedButton" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/back_to_events" android:layout_marginTop="8dp" />
</LinearLayout>
````

### `app/src/main/res/layout/activity_event_detail.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/detail_toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@color/primary"
        app:title="@string/event_detail"
        app:titleTextColor="@color/on_primary"
        app:navigationIcon="@drawable/ic_back"
        app:navigationIconTint="@color/on_primary"
        app:navigationContentDescription="@string/back"
        app:menu="@menu/detail_toolbar" />

    <TextView
        android:id="@+id/detail_status"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:accessibilityLiveRegion="polite"
        android:gravity="center"
        android:padding="24dp"
        android:text="@string/loading"
        android:textAppearance="@style/TextAppearance.EventEase.Body" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/detail_retry"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:minHeight="48dp"
        android:text="@string/retry"
        android:visibility="gone" />

    <RelativeLayout
        android:id="@+id/detail_content"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:visibility="gone">

        <LinearLayout
            android:id="@+id/detail_action_bar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_alignParentBottom="true"
            android:background="@color/surface"
            android:orientation="vertical"
            android:padding="16dp">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/register_now"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="48dp"
                android:text="@string/register_now" />
        </LinearLayout>

        <androidx.core.widget.NestedScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_above="@id/detail_action_bar"
            android:fillViewport="true">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical">

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:minHeight="240dp">

                    <ImageView
                        android:id="@+id/detail_banner"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:importantForAccessibility="no"
                        android:minHeight="240dp"
                        android:scaleType="centerCrop" />

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:background="@drawable/banner_overlay" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_gravity="bottom"
                        android:orientation="vertical"
                        android:padding="24dp">

                        <TextView
                            android:id="@+id/detail_category"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:textColor="@color/accent"
                            android:textAppearance="@style/TextAppearance.EventEase.Body" />

                        <TextView
                            android:id="@+id/detail_title"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="8dp"
                            android:textColor="@color/on_primary"
                            android:textAppearance="@style/TextAppearance.EventEase.Heading" />
                    </LinearLayout>
                </FrameLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="24dp">

                    <TextView
                        android:id="@+id/detail_organizer"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_primary"
                        android:textAppearance="@style/TextAppearance.EventEase.Body" />

                    <TextView
                        android:id="@+id/detail_date"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="20dp"
                        android:drawablePadding="8dp"
                        android:textAppearance="@style/TextAppearance.EventEase.Body"
                        app:drawableStartCompat="@drawable/ic_time"
                        app:drawableTint="@color/text_secondary" />

                    <TextView
                        android:id="@+id/detail_venue"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="12dp"
                        android:drawablePadding="8dp"
                        android:textAppearance="@style/TextAppearance.EventEase.Body"
                        app:drawableStartCompat="@drawable/ic_place"
                        app:drawableTint="@color/text_secondary" />

                    <TextView
                        android:id="@+id/detail_capacity"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="20dp"
                        android:textColor="@color/text_primary"
                        android:textAppearance="@style/TextAppearance.EventEase.Body" />

                    <TextView
                        android:id="@+id/about_event"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="28dp"
                        android:text="@string/about_event"
                        android:textSize="22sp"
                        android:textAppearance="@style/TextAppearance.EventEase.Heading" />

                    <View
                        android:layout_width="72dp"
                        android:layout_height="1dp"
                        android:layout_marginTop="8dp"
                        android:background="@drawable/gold_divider" />

                    <TextView
                        android:id="@+id/detail_description"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="16dp"
                        android:lineSpacingExtra="4dp"
                        android:textAppearance="@style/TextAppearance.EventEase.Body" />
                </LinearLayout>
            </LinearLayout>
        </androidx.core.widget.NestedScrollView>
    </RelativeLayout>
</LinearLayout>
````

### `app/src/main/res/layout/item_event.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="20dp" android:clickable="true" android:focusable="true">
<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
<ImageView android:id="@+id/event_banner" android:layout_width="match_parent" android:layout_height="176dp" android:scaleType="centerCrop" android:importantForAccessibility="no" />
<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="16dp">
<TextView android:id="@+id/event_category" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:textSize="12sp"/>
<TextView android:id="@+id/event_title" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Heading" android:textSize="22sp" android:textStyle="bold" android:layout_marginTop="8dp"/>
<TextView android:id="@+id/event_date" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" app:drawableStartCompat="@drawable/ic_time" app:drawableTint="@color/text_secondary" android:drawablePadding="8dp" android:layout_marginTop="12dp"/>
<TextView android:id="@+id/event_venue" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" app:drawableStartCompat="@drawable/ic_place" app:drawableTint="@color/text_secondary" android:drawablePadding="8dp" android:layout_marginTop="8dp"/>
<TextView android:id="@+id/registered_badge" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@style/TextAppearance.EventEase.Body" android:text="@string/registered_badge" android:background="@drawable/registered_border" android:layout_marginTop="16dp" android:visibility="gone" android:textColor="@color/text_primary"/>
<com.google.android.material.button.MaterialButton android:id="@+id/quick_register" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:text="@string/register" android:layout_marginTop="12dp"/>
</LinearLayout></LinearLayout></com.google.android.material.card.MaterialCardView>
````

### `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/primary" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
````

### `app/src/main/res/drawable/category_dropdown_background.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/surface" />
    <stroke android:width="1dp" android:color="@color/accent" />
    <corners android:radius="10dp" />
</shape>
````

### `app/src/main/res/xml/data_extraction_rules.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
    </device-transfer>
</data-extraction-rules>
````

### `app/src/main/res/xml/full_backup_content.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="root" path="." />
    <exclude domain="file" path="." />
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
    <exclude domain="external" path="." />
</full-backup-content>
````

### `app/src/test/java/com/timscdr/eventease/ui/EventBrowsingTest.java`

````java
package com.timscdr.eventease.ui;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.AppRepositories;
import com.timscdr.eventease.ui.dashboard.DashboardActivity;
import com.timscdr.eventease.ui.event.EventDetailActivity;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class EventBrowsingTest {
    @org.junit.Before public void resetApplication() throws Exception { RepositoryIsolation.reset(); }
    @Test public void browsingFiltersNavigationRecreationDetailsAndShareWork() throws Exception {
        AppRepositories repositories = AppRepositories.getInstance(RuntimeEnvironment.getApplication());
        StudentProfile student = new StudentProfile();
        student.name = "Event Tester"; student.rollNo = "BROWSE001"; student.course = "MCA";
        student.year = 1; student.email = "browse@example.edu";
        repositories.auth().signUp(student).get(5, TimeUnit.SECONDS);
        repositories.events().loadSamples(System.currentTimeMillis()).get(5, TimeUnit.SECONDS);
        Event workshop = repositories.events().findUpcoming(System.currentTimeMillis(), "Workshop", "").get().get(0);
        try (ActivityController<DashboardActivity> controller = Robolectric.buildActivity(DashboardActivity.class).setup()) {
            DashboardActivity dashboard = controller.get();
            until(() -> ready(dashboard, 5));
            assertNotNull(dashboard.findViewById(R.id.events_brand_logo));
            assertEquals(dashboard.getString(R.string.app_name),
                    ((TextView) dashboard.findViewById(R.id.events_brand_title)).getText().toString());
            dashboard.findViewById(R.id.category_workshop).performClick();
            until(() -> ready(dashboard, 1));
            ((MaterialToolbar) dashboard.findViewById(R.id.events_toolbar)).getMenu().performIdentifierAction(R.id.action_search, 0);
            SearchView search = dashboard.findViewById(R.id.event_search);
            search.setQuery("no-match", false);
            until(() -> ready(dashboard, 0));
            assertEquals(android.view.View.VISIBLE, dashboard.findViewById(R.id.empty_list).getVisibility());
            search.setQuery("Android", false);
            until(() -> ready(dashboard, 1));
            BottomNavigationView navigation = dashboard.findViewById(R.id.bottom_navigation);
            navigation.setSelectedItemId(R.id.nav_my_events);
            assertNotNull(dashboard.findViewById(R.id.my_events_list));
            navigation.setSelectedItemId(R.id.nav_profile);
            until(() -> "Event Tester".contentEquals(((TextView) dashboard.findViewById(R.id.student_name)).getText()));
            navigation.setSelectedItemId(R.id.nav_events);
            until(() -> ready(dashboard, 1));
            controller.recreate();
            DashboardActivity recreated = controller.get();
            until(() -> ready(recreated, 1));
            assertEquals("Android", ((SearchView) recreated.findViewById(R.id.event_search)).getQuery().toString());
            RecyclerView list = recreated.findViewById(R.id.event_list);
            until(() -> list.findViewHolderForAdapterPosition(0) != null);
            list.findViewHolderForAdapterPosition(0).itemView.performClick();
            Intent detail = shadowOf(recreated).getNextStartedActivity();
            assertEquals(workshop.id, detail.getLongExtra(EventDetailActivity.EVENT_ID, 0));
        }
        Intent open = new Intent(RuntimeEnvironment.getApplication(), EventDetailActivity.class)
                .putExtra(EventDetailActivity.EVENT_ID, workshop.id);
        try (ActivityController<EventDetailActivity> controller = Robolectric.buildActivity(EventDetailActivity.class, open).setup()) {
            EventDetailActivity detail = controller.get();
            until(() -> workshop.title.contentEquals(((TextView) detail.findViewById(R.id.detail_title)).getText()));
            assertTrue(detail.findViewById(R.id.register_now).isEnabled());
            ((MaterialToolbar) detail.findViewById(R.id.detail_toolbar)).getMenu().performIdentifierAction(R.id.action_share, 0);
            Intent chooser = shadowOf(detail).getNextStartedActivity();
            assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
            Intent share = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);
            assertEquals("text/plain", share.getType());
            assertTrue(share.getStringExtra(Intent.EXTRA_TEXT).contains(workshop.title));
            detail.findViewById(R.id.register_now).performClick();
            assertEquals(Integer.valueOf(0), repositories.registrations().countForEvent(workshop.id).get());
        }
    }

    private static boolean ready(DashboardActivity activity, int expected) {
        RecyclerView list = activity.findViewById(R.id.event_list);
        SwipeRefreshLayout swipe = activity.findViewById(R.id.swipe_refresh);
        return list != null && list.getAdapter() != null && list.getAdapter().getItemCount() == expected
                && swipe != null && !swipe.isRefreshing();
    }

    private static void until(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(20);
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue("Timed out waiting for browsing state", condition.getAsBoolean());
    }
}
````

### `app/src/test/java/com/timscdr/eventease/viewmodel/EventViewModelTest.java`

````java
package com.timscdr.eventease.viewmodel;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Rule;
import org.junit.Test;

public class EventViewModelTest {
    @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

    @Test public void staleSearchResponseCannotReplaceNewerResults() {
        FakeEvents events = new FakeEvents();
        EventListViewModel model = list(events, new FakeTickets(), new SavedStateHandle());
        model.refresh();
        model.search("Java");
        events.queries.get(1).complete(Collections.singletonList(event(2, "Java")));
        events.queries.get(0).complete(Collections.singletonList(event(1, "Old")));
        assertEquals("Java", model.state().getValue().events.get(0).title);
        assertFalse(model.state().getValue().loading);
    }

    @Test public void filtersAreSavedAndFailuresCanBeRetried() {
        SavedStateHandle saved = new SavedStateHandle();
        FakeEvents events = new FakeEvents();
        EventListViewModel model = list(events, new FakeTickets(), saved);
        model.category("Workshop"); model.search("Java"); model.setSearchOpen(true);
        events.queries.get(1).completeExceptionally(new IllegalStateException("Read failed"));
        assertTrue(model.state().getValue().error);
        model.refresh();
        events.queries.get(2).complete(Collections.emptyList());
        assertFalse(model.state().getValue().error);
        EventListViewModel restored = list(events, new FakeTickets(), saved);
        assertEquals("Workshop", restored.category());
        assertEquals("Java", restored.search());
        assertTrue(restored.searchOpen());
    }

    @Test public void registrationUpdatesBadgeWithoutRequeryingEvents() {
        FakeEvents events = new FakeEvents(); FakeTickets tickets = new FakeTickets();
        EventListViewModel model = list(events, tickets, new SavedStateHandle());
        Observer<EventListState> observer = ignored -> { };
        model.state().observeForever(observer);
        try {
            model.refresh(); events.queries.get(0).complete(Collections.singletonList(event(9, "Test")));
            Registration ticket = new Registration(); ticket.eventId = 9;
            tickets.all.setValue(Collections.singletonList(ticket));
            assertTrue(model.state().getValue().registeredIds.contains(9L));
            assertEquals(1, events.queries.size());
        } finally { model.state().removeObserver(observer); }
    }

    @Test public void detailTracksSeatsRegistrationAndTimeBoundary() {
        FakeEvents events = new FakeEvents(); FakeTickets tickets = new FakeTickets();
        AtomicLong clock = new AtomicLong(100);
        EventDetailViewModel model = new EventDetailViewModel(events, tickets, 1, 1, clock::get);
        Observer<EventDetailViewModel.State> observer = ignored -> { };
        model.state().observeForever(observer);
        try {
            Event event = event(1, "Test"); event.dateTime = 100; event.capacity = 2;
            events.single.setValue(event); tickets.count.setValue(1); tickets.single.setValue(null);
            assertFalse(model.state().getValue().loading);
            assertFalse(model.state().getValue().started);
            assertEquals(Integer.valueOf(1), model.state().getValue().available);
            tickets.count.setValue(3);
            assertEquals(Integer.valueOf(0), model.state().getValue().available);
            tickets.single.setValue(new Registration());
            assertTrue(model.state().getValue().registered);
            clock.set(101); model.tick(); assertTrue(model.state().getValue().started);
            events.single.setValue(null);
            assertNull(model.state().getValue().event);
            assertFalse(model.state().getValue().loading);
        } finally { model.state().removeObserver(observer); }
    }

    private static EventListViewModel list(FakeEvents events, FakeTickets tickets, SavedStateHandle saved) {
        return new EventListViewModel(events, tickets, 1, saved, Runnable::run, () -> 100);
    }
    private static Event event(long id, String title) {
        Event event = new Event(); event.id = id; event.title = title; event.dateTime = 1000; return event;
    }
    private static class FakeEvents implements EventRepository {
        final List<CompletableFuture<List<Event>>> queries = new ArrayList<>();
        final MutableLiveData<Event> single = new MutableLiveData<>();
        @Override public CompletableFuture<List<Event>> findUpcoming(long now, String category, String search) {
            CompletableFuture<List<Event>> query = new CompletableFuture<>(); queries.add(query); return query;
        }
        @Override public CompletableFuture<Integer> countAll() { return CompletableFuture.completedFuture(1); }
        @Override public LiveData<Event> observeById(long id) { return single; }
        @Override public CompletableFuture<Void> loadSamples(long now) { throw new UnsupportedOperationException(); }
        @Override public LiveData<List<Event>> observeUpcoming(long now, String category, String search) { throw new UnsupportedOperationException(); }
        @Override public LiveData<List<Event>> observeUpcomingForStudent(long id, long now) { throw new UnsupportedOperationException(); }
        @Override public LiveData<List<Event>> observePastForStudent(long id, long now) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Event> findById(long id) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Long> insert(Event event) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> update(Event event) { throw new UnsupportedOperationException(); }
    }
    private static class FakeTickets implements RegistrationRepository {
        final MutableLiveData<List<Registration>> all = new MutableLiveData<>(Collections.emptyList());
        final MutableLiveData<Registration> single = new MutableLiveData<>();
        final MutableLiveData<Integer> count = new MutableLiveData<>();
        @Override public LiveData<List<Registration>> observeForStudent(long id) { return all; }
        @Override public LiveData<Registration> observeForEventAndStudent(long event, long student) { return single; }
        @Override public LiveData<Integer> observeCountForEvent(long id) { return count; }
        @Override public LiveData<Registration> observeById(long id) { throw new UnsupportedOperationException(); }
        @Override public LiveData<Integer> observeRegistrationCount(long id) { throw new UnsupportedOperationException(); }
        @Override public LiveData<Integer> observeAttendanceCount(long id) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Registration> findByCode(String code) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Integer> countForEvent(long id) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Long> insert(Registration item) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Long> registerIfCapacity(Registration item, int capacity) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Boolean> markCheckedIn(String code) { throw new UnsupportedOperationException(); }
    }
}
````

### `docs/ASSET-CREDITS.md`

````markdown
# Bundled event photo credits

The following Wikimedia Commons photos are included as offline event-category
fallbacks. Uploaded event banners still take precedence. The photos are unmodified;
Glide crops them to fit each screen at display time.

| App category | Image | Creator | License |
| --- | --- | --- | --- |
| Technical | [Hackathon Participant from ETH Zürich, picture 2](https://commons.wikimedia.org/wiki/File:Hackathon_Participant_from_ETH_Z%C3%BCrich_picture_number_2.jpg) | Arthur Gamsa | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| Cultural | [A Vibrant Bharatanatyam Group Interpretation](https://commons.wikimedia.org/wiki/File:A_Vibrant_Bharatanatyam_Group_Interpretation.jpg) | Mohd Zishan | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| Sports | [Pre-season friendly football match between Sky Sports and Kataka FC](https://commons.wikimedia.org/wiki/File:Pre-season_friendly_football_match_between_Sky_sports_verse_Kataka_FC_played_in_Mbale_stadium.jpg) | Seruinda | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| Workshop | [SWiP workshop at Durban University of Technology](https://commons.wikimedia.org/wiki/File:SWiP_workshop_at_Durban_University_of_Technology,_Indumiso_Campus.jpg) | Slippery slope302 | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| Seminar | [Shirin Sharmin Chaudhury keynote at East West University](https://commons.wikimedia.org/wiki/File:2018-03-12_Shirin_Sharmin_Chaudhury_keynote_International_Women%27s_Day_seminar_East_West_University_(PID-0045183).jpg) | Press Information Department, Bangladesh | Public domain |

The TIMSCDR crest is the logo supplied with the project. It has a transparent
background copy for screens and a navy-backed adaptive launcher treatment. The
system splash uses a padded transparent copy so the full crest remains inside the
Android launch icon's safe area.
````
