# NiceNotes

NiceNotes is a dark-themed Android notes app built with Kotlin and Jetpack Compose. It stores notes locally with Room and includes note editing, archive and pin/importance controls, reminders, a home-screen widget, and JSON import/export surfaces.

> **Implementation status:** The app is an active implementation of [`nice_notes_implementation_plan.md`](./nice_notes_implementation_plan.md), not a complete realization of every plan item. Several features are partial or have known functional gaps; see [Implementation status and limitations](#implementation-status-and-limitations) and [`PROJECT_BLUEPRINT.md`](./PROJECT_BLUEPRINT.md).

## Features

- Create and edit text notes; blank notes are not saved.
- Search active notes via a collapsible search field (magnifier icon in header).
- View full note content in the list without truncation.
- Pin notes, mark them important, and manually reorder them in management mode.
- Archive active notes by swiping; browse and unarchive notes from the Archive screen.
- Set one-time or daily, weekly, and monthly repeating reminders.
- Receive reminder notifications and reopen the app from a notification.
- View and scroll through all active notes in a home-screen widget; each preview is limited to one line with overflow ellipsized. Tap `+` to open a new note.
- Scroll-to-top button appears when scrolling down through notes.
- Notes count displayed at the top of the main screen.
- Import legacy JSON arrays and versioned NiceNotes JSON; export notes through Android's share sheet.
- Use the app with a black/dark Compose theme with consistent snackbar colors.

The feature list describes implemented UI/code surfaces. Some behaviors have caveats documented below; in particular, the current Settings export flow does not complete successfully.

## Requirements

- Android Studio with an Android SDK Platform 36 installation.
- JDK 17 to run the Gradle 8.14.6 wrapper, Android Gradle Plugin 8.13.2, and Kotlin 2.3.21 build.
- An Android device or emulator running API 36 or newer.

The app is configured with `minSdk = 36`, `compileSdk = 36`, and `targetSdk = 36`, aligning with the implementation plan specification.
Library and plugin versions are centralized in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml).

## Build and run

Open the project root in Android Studio and allow Gradle sync to finish. Or use the Gradle wrapper from PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

To run the JVM unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

`local.properties` is machine-specific and should not be copied between developer machines. No release signing configuration is provided.

## Data and backup

The Room database is `nicenotes.db`, with the `notes` table defined by `NoteEntity`. Times are represented as epoch milliseconds in Room. Import/export maps these to the legacy timestamp format (`MMM d, yyyy h:mm:ss a`) and wraps NiceNotes exports in a JSON object with a version and export timestamp.

The repository includes [`example-of-data.json`](example-of-data.json), a sample legacy JSON export. The unit test suite checks that it parses as 2,164 notes (1,355 active and 809 archived) and exercises a 50-note export/re-import round trip.

Import and export are exposed from **Settings → Data**. Imported note IDs use Room upsert semantics, so matching IDs overwrite existing records. Review [Implementation status and limitations](#implementation-status-and-limitations) before relying on in-app export as a backup.

## Architecture and code map

The project is a single Android app module (`:app`) organized around Compose screens, Hilt-injected ViewModels/use cases, a repository, and Room:

```text
app/src/main/java/com/nicenotes/
├── data/
│   ├── db/                 Room database, DAO, and note entity
│   ├── export/              Legacy and versioned JSON mapping
│   └── repository/          Note operations and widget refresh triggers
├── di/                      Hilt database and notification modules
├── domain/usecase/          Note list, search, save, archive, delete, reorder actions
├── notification/            Exact-alarm reminders, WorkManager fallback, and boot rescheduling
├── ui/
│   ├── screen/              Main, editor, archive, and settings screens/ViewModels
│   ├── component/           Reminder dialog
│   └── theme/               Dark Material 3 theme
├── widget/                  RemoteViews widget provider and collection service
├── MainActivity.kt          Navigation host and notification entry point
└── NiceNotesApp.kt          Hilt and WorkManager initialization
```

For subsystem relationships, data flows, and the implementation audit, see [`PROJECT_BLUEPRINT.md`](./PROJECT_BLUEPRINT.md).

## Implementation status and limitations

The plan's core scaffolding and most screen/code structures exist. The following planned behaviors are incomplete or need correction before they should be treated as reliable:

| Area | Current state |
|---|---|
| SDK target | `compileSdk`/`targetSdk`/`minSdk` are all 36, matching the plan. |
| Theme and typography | Dark theme and palette exist. The planned downloadable Inter font is not configured. |
| Notes, search, pinning, importance | Main/editor UI and Room flows are present. |
| Archive and reorder | Main-screen swipe archive, archive listing/unarchive, and drag reorder are present. The Archive screen does not implement the planned management mode; archive deletion has no undo, and its undo path for unarchive is incorrect. |
| Reminders | Future reminders use exact alarms while idle when Android exact-alarm access is allowed, with a WorkManager fallback and a warning when exact access is unavailable. Notification and exact-alarm access are requested when saving a reminder; disabled notifications or channel sound link to system settings. The channel uses the system default notification sound unless changed in Android settings. Boot, app-update, and permission-grant rescheduling are supported. Past reminder times are rejected. Archiving does not cancel an existing reminder; repeating monthly reminders use a fixed 30-day interval rather than calendar-month rules. |
| Undo | Main-screen archive/delete snackbars expose undo. Archive-screen delete does not, and Archive unarchive's undo handler deletes the note rather than re-archiving it. |
| Widget | A native RemoteViews `ListView` collection displays all active notes in DAO order, clips previews to one line, and refreshes after repository mutations. Each row passes its note ID to the app; the add-note control sends a dedicated receiver action that opens note creation, including when the app is already running. Click bindings are refreshed after app updates and activity launches. |
| Import | Legacy bare arrays and versioned exports are parsed and upserted. Validation is limited, and parser failures can be reported as a successful import of zero notes. Import does not reschedule imported reminders. |
| Export | JSON serialization and FileProvider sharing code exist, but the Settings ViewModel collects a long-lived Room `Flow` instead of taking one snapshot. As a result, the export operation can remain suspended and fail to produce a share event; it also reads only active notes. |
| Tests | Two JVM test classes cover sample-data import/export and simple RRULE interval parsing. The planned DAO/in-memory Room tests and Compose UI tests are absent. |

This review is based on the checked-in source and a successful unit-test run; it is not a claim that every interaction has been manually tested on a device. Refer to the blueprint for the recommended completion order.

## Tests

Existing test files:

- `app/src/test/java/com/nicenotes/data/export/ImportExportTest.kt`
- `app/src/test/java/com/nicenotes/notification/ReminderSchedulerTest.kt`

Verified during this documentation update:

```text
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
BUILD SUCCESSFUL
```

Kotlin compilation emitted deprecation warnings for Material `Divider` and `Icons.Filled.ArrowBack`, and an opt-in warning for `flatMapLatest`. These warnings did not fail the build.

## License

No license file is currently provided. Contact the project owner before redistributing this project.
