# EventEase

**College event discovery, registration, and organizer CRM for TIMSCDR**

EventEase is a Java Android app for students to discover college events, register, and present QR tickets. Its separate organizer portal manages events, sees a live attendee roster, exports registration data, and validates tickets at the venue.

## Project details

| Setting | Value |
| --- | --- |
| Android application ID | `com.timscdr.eventease` |
| Language and UI | Java and XML layouts |
| Minimum Android version | API 24 (Android 7.0) |
| Compile / target SDK | 35 / 34 |
| Android Gradle Plugin / Gradle | 9.2.1 / 9.4.1 |
| Java compatibility | 17 |

## Features

### Student app

- Create a student profile with name, roll number, course, year, email, and college.
- Browse upcoming events, search by title, filter by category, and see event details and images.
- Register against the event capacity and receive a unique QR ticket.
- View registered events split into upcoming and past events.
- Optionally schedule a local reminder before an event.
- Choose System, Light, or Dark appearance.

### Organizer CRM

- Sign in through a dedicated organizer login; organizer access is granted by an administrator.
- View organizer-owned events and live totals for events, registrations, and check-ins.
- Create events, select a custom event image, edit event details and capacity, and delete events that have no registrations. The organizer's selected photo syncs to the student app; bundled category art is used when no custom photo is selected.
- Open an event roster showing each attendee's name, roll number, email, college, registration time, and check-in status.
- Search the roster and export it as CSV using Android's file picker.
- Scan a QR ticket from that event's attendee page. Tickets from another event are rejected, repeat scans are marked as already checked in, and successful check-ins update the live totals.

## Shared sync and Firebase setup

Organizer accounts, the event catalog, custom event photos, registrations, attendee rosters, and check-ins sync through Firebase Authentication and Cloud Firestore. The organizer portal uses a separate named Firebase app session, so organizer sign-in and sign-out do not replace the student's anonymous sync identity. Organizer photos are compressed to a maximum of 20 KiB and stored as non-indexed image data in the Firestore event document; no Firebase Storage bucket is used. Large originals are downscaled and re-encoded before sync. The free Firestore quota also applies to image data transfer and storage. If Firebase is not configured, the app uses the local Room demo and the organizer portal reports that cloud setup is required.

### Configure a Firebase project

1. Create a Firebase project and register an Android app with package name `com.timscdr.eventease`.
2. Download `google-services.json` and place it at `app/google-services.json`. Do not commit this project-specific file to a public repository.
3. In Firebase Authentication, enable **Anonymous** sign-in for student cloud sync and registration, and **Email/Password** for organizer accounts. Verify both providers are enabled under Authentication → Sign-in method.
4. Create a Cloud Firestore database. For a student/demo deployment, Firestore has a no-cost Spark quota (currently 1 GiB stored data, 50,000 reads/day, 20,000 writes/day, 20,000 deletes/day, and 10 GiB outbound transfer/month); image bytes count toward storage and transfer, so keep the team’s usage within quota and check Firebase's [current pricing](https://firebase.google.com/pricing). This project does not use Firebase Storage or require a Storage bucket.
5. Install and sign in to the Firebase CLI if needed. `.firebaserc` selects project `eventease-31e81`. From the project root, deploy the supplied Firestore rules and indexes (rules can be re-deployed safely if already published):

   ```sh
   firebase login
   firebase deploy --only firestore:rules,firestore:indexes
   ```

6. In Firebase Authentication, create an Email/Password user for each organizer. The current EventEase organizer account is `eventease123@gmail.com` with UID `PbTctivwDpN5pHD7OGfINZZgdb83`. In Firestore, create the document `organizers/PbTctivwDpN5pHD7OGfINZZgdb83` using these fields:

   ```json
   {
     "name": "EventEase Organizer",
     "email": "eventease123@gmail.com",
     "collegeName": "TIMSCDR",
     "active": true
   }
   ```

   Only an administrator should create or disable these documents. The app's Firestore rules deny client writes to the organizer allow-list.

7. Build and share a new APK after adding the Firebase config. Every team member should install that same build; organizer-created events, registrations, attendee rosters, and check-ins then sync through the same Firebase project.

The repository root contains `firebase.json`, `firestore.rules`, and `firestore.indexes.json`. Firestore rules enforce organizer ownership, capacity limits, unique per-event registration locks, one-time check-in, and a size limit for custom image data. The image field has indexing disabled because it is never queried. Deploy the updated Firestore rules and indexes before using custom images with the cloud app. See Firebase's [Android setup guide](https://firebase.google.com/docs/android/setup) and [Firestore rules guide](https://firebase.google.com/docs/firestore/security/rules-conditions) for project-side details.

### Identity and demo limitations

The organizer portal uses Firebase Email/Password authentication plus the administrator-managed organizer allow-list. Student sign-in remains the project's local name-and-roll-number flow. Firebase gives each installation an anonymous sync identity, and a roll-number hash prevents duplicate registrations for the same event across devices. Student registration history and QR tickets are scoped to the installation's anonymous identity, so they do not automatically restore on another device or after app data is cleared. The CRM still sees the cloud registration records. This flow does not verify that a student owns the entered name or roll number; do not use it for sensitive or production deployments without institution-verified student authentication.

Room remains the student's local profile source and app settings stay on-device; the student's attendee details are mirrored to Firestore for registration and the organizer roster. The current cloud setup syncs event catalog data, new registrations, attendee snapshots, and check-in state; it does not import old local sample events or historical registrations into Firestore. Publish events from the organizer portal after setting up Firebase.

## Design

The interface uses navy, gold, and warm ivory in light mode, with a deep navy-black dark background. Gold is reserved for accents. Material Components DayNight themes and `values-night` resources provide dark mode. Playfair Display headings and Inter body text are bundled locally. The TIMSCDR crest is shown on branded screens and tickets on a white background; the launcher icon uses the crest.

| Palette | Light | Dark |
| --- | --- | --- |
| Primary navy | `#0D2545` | `#1B3A63` |
| Gold accent | `#C9A227` | `#D4AF37` |
| Background | `#FAF7F0` | `#0F1420` |

## Architecture

The app follows MVVM. Activities and Fragments observe ViewModel state; ViewModels depend on repository contracts instead of DAOs or Firebase SDKs.

```text
Activity / Fragment
        ↓
    ViewModel
        ↓
 Repository interface
       ↙       ↘
 Room / DAO    Firebase implementation
```

- Room stores student profiles and local settings/session data.
- With Firebase configured, event and registration repositories use Firestore snapshot listeners and transactions; custom event photos travel with the event document, with bundled art as a fallback.
- Without Firebase config, event and registration repositories use Room for the local demo.
- Firebase organizer repositories use a separate named Firebase app session and scope organizer event and attendee reads to the signed-in organizer.

### Main source layout

```text
app/src/main/java/com/timscdr/eventease/
├── data/          Room entities, DAOs, and database
├── receiver/      Event reminder receiver
├── repository/    Repository interfaces and Room/Firebase implementations
├── ui/            Student and separate organizer screens
├── util/          QR, images, navigation, and reminders
└── viewmodel/     Screen state and dependency wiring

app/src/main/res/
├── drawable-nodpi/ Event art and crest assets
├── font/           Local font files
├── layout/         XML screens and row layouts
├── values/         Light palette and shared resources
└── values-night/   Dark palette overrides
```

## Data model

Room schema version is 2. The migration from version 1 adds `StudentProfile.collegeName` with the `TIMSCDR` default.

| Entity | Main fields | Use |
| --- | --- | --- |
| `Event` | Title, category, description, venue, date/time, banner, capacity, organizer | Event catalog and event details |
| `Registration` | Event ID, student ID, registration code, registration time, check-in state | Ticket and attendance state |
| `StudentProfile` | Name, roll number, course, year, email, college | Student profile and attendee details |

Event and registration times are stored as epoch milliseconds. Registration codes are unique. Cloud registration writes use a Firestore transaction to reserve capacity and create a per-event roll-number lock atomically.

## Technology

| Area | Implementation |
| --- | --- |
| UI and theme | Java, AndroidX AppCompat, Material Components, XML, DayNight |
| Screen state | AndroidX ViewModel and LiveData |
| Local persistence | Room / SQLite |
| Shared sync | Firebase Auth and Cloud Firestore (including compressed event image data) |
| Event lists | RecyclerView and SwipeRefreshLayout |
| Images | Glide, bundled category art, and compressed custom event photos in Firestore |
| QR generation and scanning | ZXing Core and JourneyApps Android Embedded |
| Reminders | AlarmManager and AndroidX NotificationCompat |

No Kotlin source or Jetpack Compose is used. Dependency versions are declared in `app/build.gradle`.

## Build and install

Android Studio is not required. Build using the included Gradle wrapper.

### Requirements

- JDK 17 or newer
- Android SDK Platform 35 and Platform Tools
- Network access on the first build to download Gradle and Maven dependencies

Set `ANDROID_HOME` to your Android SDK location, or create an untracked `local.properties` file in the project root:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Linux or macOS:

```sh
chmod +x gradlew
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install on a connected device, run `.\gradlew.bat installDebug` on Windows or `./gradlew installDebug` on Linux/macOS.

## Permissions

| Permission | Use |
| --- | --- |
| `INTERNET` | Firebase sync |
| `POST_NOTIFICATIONS` | Event reminders on Android 13 and newer |
| `CAMERA` | QR ticket scanning; camera hardware is optional for installation |

The organizer chooses an image through Android's system photo picker; the app does not request broad storage access. Photos are compressed on-device before being written with the event document, then students retrieve them through Firestore. Firebase setup uses the project-specific `google-services.json`; no separate API key is entered in the source. Treat that file as project-specific client configuration; access to data is enforced by Firebase Authentication and the deployed security rules, not by hiding the client API key.

## Credits

- Event category images and license details: [docs/ASSET-CREDITS.md](docs/ASSET-CREDITS.md).
- Playfair Display and Inter license files: [licenses/](licenses/).
- The TIMSCDR crest was supplied for this project.

The About screen has four team contribution areas. Add the actual team members' names there before presenting.
