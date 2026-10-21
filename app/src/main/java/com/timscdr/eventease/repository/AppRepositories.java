package com.timscdr.eventease.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private final SettingsRepository settings;
    private final OrganizerAuthRepository organizerAuth;
    private final OrganizerRepository organizer;

    // Injection seam for tests; the production executor lives for the app process.
    AppRepositories(AppDatabase database, Executor executor, SharedPreferences session) {
        this(database, executor, session, session);
    }

    AppRepositories(AppDatabase database, Executor executor, SharedPreferences session, SharedPreferences appSettings) {
        profiles = new RoomProfileRepository(database.profileDao(), executor);
        events = new RoomEventRepository(database.eventDao(), executor);
        registrations = new RoomRegistrationRepository(database.registrationDao(), executor);
        auth = new LocalAuthRepository(database, session, executor);
        settings = new LocalSettingsRepository(appSettings);
        organizerAuth = new UnavailableOrganizerAuthRepository();
        organizer = new UnavailableOrganizerRepository();
    }

    private AppRepositories(Context context, AppDatabase database, Executor executor,
            SharedPreferences session, SharedPreferences appSettings) {
        profiles = new RoomProfileRepository(database.profileDao(), executor);
        settings = new LocalSettingsRepository(appSettings);
        SharedPreferences organizerPreferences = context.getSharedPreferences("eventease_organizer", Context.MODE_PRIVATE);
        SharedPreferences cloudSession = context.getSharedPreferences("eventease_cloud_student", Context.MODE_PRIVATE);
        if (FirebaseEnvironment.isConfigured(context)) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            FirebaseApp organizerApp = FirebaseEnvironment.organizerApp(context);
            FirebaseFirestore organizerFirestore = FirebaseFirestore.getInstance(organizerApp);
            FirebaseAuth organizerFirebaseAuth = FirebaseAuth.getInstance(organizerApp);
            FirebaseStudentProfileSync studentProfileSync = new FirebaseStudentProfileSync(firestore, cloudSession);
            events = new FirebaseEventRepository(firestore, firebaseAuth,
                    organizerFirestore, organizerFirebaseAuth, organizerPreferences);
            registrations = new FirebaseRegistrationRepository(firestore, cloudSession, studentProfileSync);
            auth = new SyncingAuthRepository(new LocalAuthRepository(database, session, executor),
                    studentProfileSync);
            organizerAuth = new FirebaseOrganizerAuthRepository(organizerFirebaseAuth, organizerFirestore, organizerPreferences);
            organizer = new FirebaseOrganizerRepository(organizerFirestore, organizerFirebaseAuth, events);
        } else {
            events = new RoomEventRepository(database.eventDao(), executor);
            registrations = new RoomRegistrationRepository(database.registrationDao(), executor);
            auth = new LocalAuthRepository(database, session, executor);
            organizerAuth = new UnavailableOrganizerAuthRepository();
            organizer = new UnavailableOrganizerRepository();
        }
    }

    public static AppRepositories getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRepositories.class) {
                if (instance == null) {
                    instance = new AppRepositories(context.getApplicationContext(), AppDatabase.getInstance(context),
                            Executors.newSingleThreadExecutor(), context.getApplicationContext()
                            .getSharedPreferences("eventease_session", Context.MODE_PRIVATE), context.getApplicationContext()
                            .getSharedPreferences("eventease_settings", Context.MODE_PRIVATE));
                }
            }
        }
        return instance;
    }

    public EventRepository events() { return events; }
    public ProfileRepository profiles() { return profiles; }
    public RegistrationRepository registrations() { return registrations; }
    public AuthRepository auth() { return auth; }
    public SettingsRepository settings() { return settings; }
    public OrganizerAuthRepository organizerAuth() { return organizerAuth; }
    public OrganizerRepository organizer() { return organizer; }
}
