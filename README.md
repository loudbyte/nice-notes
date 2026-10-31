# NiceNotes

NiceNotes is a dark-themed Android notes app built with Kotlin and Jetpack Compose. It stores notes locally with Room and includes note editing, archive and pin/importance controls, reminders, a home-screen widget, and JSON import/export surfaces.

> **Current status:** NiceNotes is an active, standalone Android app in continuous development. Several features have known caveats and technical debt; see [Feature status and known limitations](#feature-status-and-known-limitations) and [`PROJECT_BLUEPRINT.md`](./PROJECT_BLUEPRINT.md).

## Features

- Create and edit text notes; blank notes are not saved.
- Search active notes via a collapsible search field (magnifier icon in header).
- View full note content in the list without truncation.
- Pin notes, mark them important, and manually reorder them in management mode.
- Sort notes with reminders first using the persistent “Reminders first” toggle in the main menu; enabled by default and applied to search and management mode.
- Archive active notes by swiping; browse, unarchive (swipe right), and delete (swipe left) notes from the Archive screen with undo.
- Set one-time or daily, weekly, and monthly repeating reminders.
- Receive reminder notifications and tap to open the note directly in the editor.
- View and scroll through all active notes in a home-screen widget; each preview is limited to one line with overflow ellipsized. Tap `+` to open a new note.
- Scroll-to-top button appears when scrolling down through notes.
- Notes count displayed at the top of the main screen.
- Import legacy JSON arrays and versioned NiceNotes JSON; export notes through Android's share sheet.
- Use the app with a black/dark Compose theme with consistent snackbar colors.

The feature list describes implemented UI/code surfaces. Some behaviors have caveats documented below; in particular, the current Settings export flow does not complete successfully.

## Requirements

- Android Studio with an Android SDK Platform 36 installation.
- JDK 17 to run the Gradle 8.14.6 wrapper, Android Gradle Plugin 8.13.2, and Kotlin 2.3.21 build targeting Java 17 bytecode.
- An Android device or emulator running API 33 (Android 13) or newer.

The app is configured with `minSdk = 33`, `compileSdk = 36`, and `targetSdk = 36`. Android SDK Platform 36 is still required to build the app.
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

Legacy JSON import and export are exercised by the unit test suite, including a 50-note export/re-import round trip.

Import and export are exposed from **Settings → Data**. Imported note IDs use Room upsert semantics, so matching IDs overwrite existing records. Review [Feature status and known limitations](#feature-status-and-known-limitations) before relying on in-app export as a backup.

## Architecture and code map

The project is a single Android app module (`:app`) organized around Compose screens, Hilt-injected ViewModels/use cases, a repository, and Room:

```text
app/src/main/java/com/nicenotes/
├── data/
│   ├── db/
│   │   ├── entity/          NoteEntity
│   │   ├── NoteDao.kt
│   │   └── NoteDatabase.kt
│   ├── export/              ExportSchema, LegacyNote, ImportExportManager
│   ├── preferences/         App-local note display preferences
│   └── repository/          Note operations and widget refresh triggers
├── di/                      Hilt database and notification modules
├── domain/usecase/          UseCases (CRUD, archive, reorder, lookup)
├── notification/            Exact-alarm reminders, WorkManager fallback, and boot rescheduling
├── ui/
│   ├── Screen.kt            Navigation route definitions
│   ├── screen/              Main, editor, archive, and settings screens/ViewModels
│   ├── component/           Reminder dialog
│   └── theme/               Dark Material 3 theme
├── widget/                  RemoteViews widget provider and WorkManager update worker
├── MainActivity.kt          Navigation host and notification entry point
└── NiceNotesApp.kt          Hilt and WorkManager initialization
```

For subsystem relationships, data flows, and the implementation audit, see [`PROJECT_BLUEPRINT.md`](./PROJECT_BLUEPRINT.md).

## Feature status and known limitations

The application's core architecture and primary screen structures are in place. The following areas have known functional caveats or work in progress:

| Area | Current state |
|---|---|
| SDK support | `minSdk` is 33 (Android 13); `compileSdk` and `targetSdk` are 36. Runtime notification permission is requested when saving a reminder on Android 13 and newer. |
| Theme and typography | Dark theme and palette are implemented. Inter downloadable font is not yet configured. |
| Notes, search, pinning, importance | Main/editor UI and Room flows are present. The main menu's persistent “Reminders first” toggle sorts reminder notes first in the main list, search results, and management mode while retaining order within each group. |
| Archive and reorder | Main-screen swipe archive, management-mode drag reorder and swipe-to-delete, and Archive screen swipe-to-unarchive and swipe-to-delete are present with undo. Management mode shows its actions beside the note count; the drag handle is centered in each note's footer row. |
| Reminders | Future reminders use exact alarms while idle when Android exact-alarm access is allowed, with a WorkManager fallback and a warning when exact access is unavailable. Notification and exact-alarm access are requested when saving a reminder; disabled notifications or channel sound link to system settings. The channel uses the system default notification sound unless changed in Android settings. Boot, app-update, and permission-grant rescheduling are supported. Past reminder times are rejected. Archiving does not cancel an existing reminder; repeating monthly reminders use a fixed 30-day interval rather than calendar-month rules. |
| Undo | Main-screen and Archive-screen archive, unarchive, and delete snackbars all expose functional undo actions. |
| Widget | A native RemoteViews `ListView` collection displays all active notes in DAO order, clips previews to one line, and refreshes its collection data directly after repository mutations without scheduling background work or replacing the whole widget layout. The first widget requests one delayed follow-up after the launcher binds its service; ordinary `onUpdate` callbacks do not enqueue repeated delayed refreshes. Each row passes its note ID to the app; the add-note control sends an Activity intent (`ACTION_CREATE_NOTE`) that opens note creation, including when the app is already running. Collection rows are provided by `NotesWidgetService`. |
| Import | Legacy bare arrays and versioned exports are parsed and upserted. Validation is limited, and parser failures can be reported as a successful import of zero notes. Import does not reschedule imported reminders. |
| Export | JSON serialization and FileProvider sharing code exist, but the Settings ViewModel collects a long-lived Room `Flow` instead of taking one snapshot. As a result, the export operation can remain suspended and fail to produce a share event; it also reads only active notes. |
| Tests | JVM tests cover sample-data import/export, reminder interval parsing, and stable reminder-first ordering. In-memory Room DAO tests and Compose UI tests are currently absent. |

This review is based on the checked-in source and a successful unit-test run; it is not a claim that every interaction has been manually tested on a device. Refer to the blueprint for the recommended completion order.

## Tests

Existing test files:

- `app/src/test/java/com/nicenotes/data/export/ImportExportTest.kt`
- `app/src/test/java/com/nicenotes/notification/ReminderSchedulerTest.kt`
- `app/src/test/java/com/nicenotes/ui/screen/main/ReminderNoteSortingTest.kt`

Test commands:

```powershell
.\gradlew.bat test
.\gradlew.bat testDebugUnitTest
.\gradlew.bat testDebugUnitTest --info
.\gradlew.bat testDebugUnitTest --rerun-tasks
```

Test report: `app\build\reports\tests\testDebugUnitTest\index.html`

Verified during this documentation update:

```text
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
BUILD SUCCESSFUL
```

Kotlin compilation emitted deprecation warnings for Material `Divider` and `Icons.Filled.ArrowBack`, and an opt-in warning for `flatMapLatest`. These warnings did not fail the build.

## License

No license file is currently provided. Contact the project owner before redistributing this project.
