# Step 2 — complete new and changed files

Room entities, DAOs, database, exported version 1 schema, tests, Gradle changes, and updated README. Each file is reproduced in full below. Step 1 files not listed here are unchanged.

Reference: [Android Room entities](https://developer.android.com/training/data-storage/room/defining-data) and [Room DAOs](https://developer.android.com/training/data-storage/room/accessing-data).

## app/build.gradle

``groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.timscdr.eventease'
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
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    implementation 'com.google.zxing:core:3.5.3'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.robolectric:robolectric:4.14.1'
    testImplementation 'androidx.arch.core:core-testing:2.2.0'
}

``

## app/src/main/java/com/timscdr/eventease/data/dao/EventDao.java

``java
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
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Event event);

    @Update(onConflict = OnConflictStrategy.ABORT)
    int update(Event event);

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

``

## app/src/main/java/com/timscdr/eventease/data/dao/ProfileDao.java

``java
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
    @Query("SELECT * FROM StudentProfile WHERE rollNo = :rollNo ORDER BY id")
    List<StudentProfile> findByRollNo(String rollNo);
}

``

## app/src/main/java/com/timscdr/eventease/data/dao/RegistrationDao.java

``java
package com.timscdr.eventease.data.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.timscdr.eventease.data.entity.Registration;
import java.util.List;

@Dao
public interface RegistrationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Registration registration);

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

    @Query("SELECT COUNT(*) FROM Registration WHERE studentId = :studentId")
    LiveData<Integer> observeRegistrationCount(long studentId);

    @Query("SELECT COUNT(*) FROM Registration WHERE studentId = :studentId AND checkedIn = 1")
    LiveData<Integer> observeAttendanceCount(long studentId);

    /** Atomic state change: 1 means accepted; 0 means unknown or already used. */
    @Query("UPDATE Registration SET checkedIn = 1 WHERE registrationCode = :code AND checkedIn = 0")
    int markCheckedIn(String code);
}

``

## app/src/main/java/com/timscdr/eventease/data/database/AppDatabase.java

``java
package com.timscdr.eventease.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.timscdr.eventease.data.dao.EventDao;
import com.timscdr.eventease.data.dao.ProfileDao;
import com.timscdr.eventease.data.dao.RegistrationDao;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;

@Database(entities = {Event.class, Registration.class, StudentProfile.class},
        version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract EventDao eventDao();
    public abstract RegistrationDao registrationDao();
    public abstract ProfileDao profileDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "eventease.db").build();
                }
            }
        }
        return instance;
    }
}

``

## app/src/main/java/com/timscdr/eventease/data/entity/Event.java

``java
package com.timscdr.eventease.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "Event", indices = {@Index("dateTime")})
public class Event {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull public String title = "";
    @NonNull public String category = "";
    @NonNull public String description = "";
    @NonNull public String venue = "";
    /** UTC epoch milliseconds; format in the user's timezone in the UI. */
    public long dateTime;
    /** Either a remote URL or a local image URI/path; null means no banner. */
    @Nullable public String bannerImageUrl;
    public int capacity;
    @NonNull public String organizerName = "";
}

``

## app/src/main/java/com/timscdr/eventease/data/entity/Registration.java

``java
package com.timscdr.eventease.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "Registration",
        foreignKeys = {
                @ForeignKey(entity = Event.class, parentColumns = "id",
                        childColumns = "eventId", onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = StudentProfile.class, parentColumns = "id",
                        childColumns = "studentId", onDelete = ForeignKey.RESTRICT)
        },
        indices = {
                @Index(value = "registrationCode", unique = true),
                @Index(value = {"eventId", "studentId"}, unique = true),
                @Index("studentId")
        })
public class Registration {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long eventId;
    public long studentId;
    @NonNull public String registrationCode = "";
    /** UTC epoch milliseconds. */
    public long registeredAt;
    public boolean checkedIn;
}

``

## app/src/main/java/com/timscdr/eventease/data/entity/StudentProfile.java

``java
package com.timscdr.eventease.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "StudentProfile")
public class StudentProfile {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull public String name = "";
    @NonNull public String rollNo = "";
    @NonNull public String course = "";
    public int year;
    @NonNull public String email = "";
}

``

## app/src/test/java/com/timscdr/eventease/data/database/AppDatabaseTest.java

``java
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

``

## app/schemas/com.timscdr.eventease.data.database.AppDatabase/1.json

``json
{
  "formatVersion": 1,
  "database": {
    "version": 1,
    "identityHash": "cfd0f254caf84180b647902f12b077b1",
    "entities": [
      {
        "tableName": "Event",
        "createSql": "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `venue` TEXT NOT NULL, `dateTime` INTEGER NOT NULL, `bannerImageUrl` TEXT, `capacity` INTEGER NOT NULL, `organizerName` TEXT NOT NULL)",
        "fields": [
          {
            "fieldPath": "id",
            "columnName": "id",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "title",
            "columnName": "title",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "category",
            "columnName": "category",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "description",
            "columnName": "description",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "venue",
            "columnName": "venue",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "dateTime",
            "columnName": "dateTime",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "bannerImageUrl",
            "columnName": "bannerImageUrl",
            "affinity": "TEXT",
            "notNull": false
          },
          {
            "fieldPath": "capacity",
            "columnName": "capacity",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "organizerName",
            "columnName": "organizerName",
            "affinity": "TEXT",
            "notNull": true
          }
        ],
        "primaryKey": {
          "autoGenerate": true,
          "columnNames": [
            "id"
          ]
        },
        "indices": [
          {
            "name": "index_Event_dateTime",
            "unique": false,
            "columnNames": [
              "dateTime"
            ],
            "orders": [],
            "createSql": "CREATE INDEX IF NOT EXISTS `index_Event_dateTime` ON `${TABLE_NAME}` (`dateTime`)"
          }
        ],
        "foreignKeys": []
      },
      {
        "tableName": "Registration",
        "createSql": "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, `studentId` INTEGER NOT NULL, `registrationCode` TEXT NOT NULL, `registeredAt` INTEGER NOT NULL, `checkedIn` INTEGER NOT NULL, FOREIGN KEY(`eventId`) REFERENCES `Event`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`studentId`) REFERENCES `StudentProfile`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "fields": [
          {
            "fieldPath": "id",
            "columnName": "id",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "eventId",
            "columnName": "eventId",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "studentId",
            "columnName": "studentId",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "registrationCode",
            "columnName": "registrationCode",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "registeredAt",
            "columnName": "registeredAt",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "checkedIn",
            "columnName": "checkedIn",
            "affinity": "INTEGER",
            "notNull": true
          }
        ],
        "primaryKey": {
          "autoGenerate": true,
          "columnNames": [
            "id"
          ]
        },
        "indices": [
          {
            "name": "index_Registration_registrationCode",
            "unique": true,
            "columnNames": [
              "registrationCode"
            ],
            "orders": [],
            "createSql": "CREATE UNIQUE INDEX IF NOT EXISTS `index_Registration_registrationCode` ON `${TABLE_NAME}` (`registrationCode`)"
          },
          {
            "name": "index_Registration_eventId_studentId",
            "unique": true,
            "columnNames": [
              "eventId",
              "studentId"
            ],
            "orders": [],
            "createSql": "CREATE UNIQUE INDEX IF NOT EXISTS `index_Registration_eventId_studentId` ON `${TABLE_NAME}` (`eventId`, `studentId`)"
          },
          {
            "name": "index_Registration_studentId",
            "unique": false,
            "columnNames": [
              "studentId"
            ],
            "orders": [],
            "createSql": "CREATE INDEX IF NOT EXISTS `index_Registration_studentId` ON `${TABLE_NAME}` (`studentId`)"
          }
        ],
        "foreignKeys": [
          {
            "table": "Event",
            "onDelete": "RESTRICT",
            "onUpdate": "NO ACTION",
            "columns": [
              "eventId"
            ],
            "referencedColumns": [
              "id"
            ]
          },
          {
            "table": "StudentProfile",
            "onDelete": "RESTRICT",
            "onUpdate": "NO ACTION",
            "columns": [
              "studentId"
            ],
            "referencedColumns": [
              "id"
            ]
          }
        ]
      },
      {
        "tableName": "StudentProfile",
        "createSql": "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `rollNo` TEXT NOT NULL, `course` TEXT NOT NULL, `year` INTEGER NOT NULL, `email` TEXT NOT NULL)",
        "fields": [
          {
            "fieldPath": "id",
            "columnName": "id",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "name",
            "columnName": "name",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "rollNo",
            "columnName": "rollNo",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "course",
            "columnName": "course",
            "affinity": "TEXT",
            "notNull": true
          },
          {
            "fieldPath": "year",
            "columnName": "year",
            "affinity": "INTEGER",
            "notNull": true
          },
          {
            "fieldPath": "email",
            "columnName": "email",
            "affinity": "TEXT",
            "notNull": true
          }
        ],
        "primaryKey": {
          "autoGenerate": true,
          "columnNames": [
            "id"
          ]
        },
        "indices": [],
        "foreignKeys": []
      }
    ],
    "views": [],
    "setupQueries": [
      "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
      "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'cfd0f254caf84180b647902f12b077b1')"
    ]
  }
}
``

## README.md

``markdown
# EventEase — Step 2

Java + XML Android project for TIMSCDR. The project foundation and Room layer are implemented.

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
Repositories and ViewModels are not implemented yet.
The organizer package is reserved only; optional organizer work has not started.
SplashActivity currently hosts the foundation preview, not the finished splash.

Next steps, each requiring your confirmation: repositories; authentication
and profile; dashboard and browsing; registration and QR; My Events; settings.
UI will observe ViewModels; ViewModels will depend on repository contracts.
Room access will remain behind repositories to preserve a future backend swap.

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
Production main-thread database access is disabled. Step 3 repositories will run
synchronous DAO methods and multi-operation transactions on background executors.
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

Full file listings: docs/STEP-1-FILES.md (historical) and docs/STEP-2-FILES.md.

``
