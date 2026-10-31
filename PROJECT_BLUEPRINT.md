# NiceNotes Project Blueprint

## 1. Purpose and status

NiceNotes is a local-first Android notes app combining quick text capture with organization, reminders, home-screen access, and backup/restore.

This document serves as the comprehensive architectural blueprint and system reference for the NiceNotes codebase. It outlines the application architecture, data persistence, background scheduling, UI surfaces, current implementation status, and known issues or technical debt.

## 2. Product scope

### Implemented surfaces

- Active-note list with text search.
- Note editor with pin, important, and reminder controls.
- Long-press management mode with drag reordering and hard delete.
- Swipe-to-archive on the active list.
- Archive list with swipe-to-unarchive (right) and swipe-to-delete (left).
- Settings screen with theme label and data import/export actions.
- Exact AlarmManager reminders with a WorkManager fallback, notifications, repeat scheduling, and boot-time re-scheduling.
- Scrollable native RemoteViews widget showing all active notes with one-line ellipsized previews; the first widget requests a delayed follow-up after the launcher binds the service, while ordinary `onUpdate` callbacks do not enqueue repeated delayed refreshes. Its add control sends an Activity Intent (`ACTION_CREATE_NOTE`) to open the note editor for a new note, including when the app is running.
- Legacy JSON-array import and versioned NiceNotes JSON serialization.

### Known limitations and pending enhancements

- Archive bulk management mode.
- Reliable full-database export snapshotting and robust import error reporting/validation.
- Exact recurrence rules, complete reminder lifecycle management, and reminder-state handling across updates/deletions.
- In-memory Room DAO tests and Compose UI integration tests.
- Downloadable Inter font configuration.

## 3. Technology and build configuration

| Concern | Current implementation |
|---|---|
| Language/UI | Kotlin, Jetpack Compose, Material 3, Java 17 |
| Architecture | Compose screens/ViewModels → use cases → domain `Note` → repository mapping → Room DAO/entities |
| Dependency injection | Hilt; widget update worker accesses the singleton database through a Hilt entry point |
| Persistence | Room, one `notes` table, schema version 1, exported schema snapshots |
| Background work | AlarmManager exact reminders with WorkManager/Hilt fallback |
| Notifications | Android notification channel and notification intents |
| Widget | Android AppWidget `RemoteViews` collection |
| JSON | kotlinx.serialization |
| Reordering | `sh.calvin.reorderable` |
| Android SDK | min 33, compile 36, target 36 |
| Project modules | `:app` only |

The application supports Android 13 (API 33) and newer. It compiles and targets API 36.

## 4. High-level architecture

```mermaid
flowchart TD
    UI[Compose screens] --> VM[Hilt ViewModels]
    VM --> UC[Use cases]
    UC --> Repo[NoteRepository]
    Repo --> DAO[NoteDao]
    DAO --> DB[(Room: nicenotes.db)]
    Repo --> Widget[RemoteViews collection refresh]
    Editor[Note editor] --> Scheduler[ReminderScheduler]
    Scheduler --> Alarm[AlarmManager exact while idle]
    Alarm --> Receiver[ReminderReceiver]
    Scheduler -. exact access unavailable .-> WM[WorkManager fallback]
    WM --> Worker[ReminderWorker]
    Receiver --> Delivery[ReminderDelivery]
    Worker --> Delivery
    Delivery --> Repo
    Delivery --> Notify[NoteNotificationManager]
    Boot[BootCompletedReceiver] --> Repo
    Boot --> Scheduler
    Settings[Settings screen] --> SettingsVM[SettingsViewModel]
    SettingsVM --> Repo
    SettingsVM --> IE[ImportExportManager]
```

Room schema snapshots are generated under `app/schemas/`; the schema remains version 1 because the domain model is not persisted.

### Runtime entry points

- `NiceNotesApp` enables Hilt and configures WorkManager with `HiltWorkerFactory`; it creates the reminder notification channel at application startup.
- `MainActivity` hosts the type-safe Navigation Compose graph for Main, Archive, and Settings using serializable route objects.
- `BootCompletedReceiver` re-schedules active reminders after device boot, app updates, and exact-alarm access grants.
- `ReminderReceiver` delivers exact alarms; WorkManager runs the best-effort fallback when exact-alarm access is unavailable.
- `NotesWidgetReceiver` supplies the native widget layout and schedules a single delayed follow-up collection refresh when the first widget is enabled; `NotesWidgetService` reads rows from the Hilt-provided database, and repository writes notify the widget collection directly.
- `FileProvider` shares a cached JSON backup from Settings.

### Data ownership and flows

- The Room database is the persistent source of truth.
- `NoteDao` exposes active and archived note flows; active rows are ordered pinned-first and then by descending `position`.
- `NoteRepository` delegates persistence, supports reminder lookups, and triggers widget refreshes after several writes.
- Use cases expose discrete note actions using the domain `Note` model. `NoteRepository` maps between that model and Room's `NoteEntity`; import/export and widget code stay at their data boundaries.
- Screens collect ViewModel state and invoke user actions; the Main screen and Archive screen each handle snackbar events.

## 5. Data model

`NoteEntity` is stored in the `notes` table and keyed by `mLocalId`.

| Field group | Fields and purpose |
|---|---|
| Identity/content | `mLocalId`, optional `mGlobalId`, `mContent`, `mParentId` |
| Legacy compatibility | `mStatus`, `mTimestamp`, `mIsDelete`, `mIsLocal`, `mColorIndex`, `mSysVersion`, reminder pre-state, flattened `mExtra` values |
| Timestamps | Create/update/finish and alarm values stored as epoch milliseconds (`Long`/nullable `Long`) |
| Reminder state | `mAlarmTime`, `mAlarmTimePre`, `mNextAlarmTime`, `mRepeatRulePre`, `mForceReminderPre`, `isReminded` |
| NiceNotes organization | `isPinned`, `isImportant`, `isArchived`, `position` |

Legacy import maps `mIsDelete` to `isArchived`. Export maps entity timestamps back to the legacy formatted timestamp fields while retaining NiceNotes-only fields in the versioned format.

### Backup formats

- **Legacy input:** a bare JSON array of legacy note records.
- **NiceNotes format:** a JSON object containing `version`, `exportedAt`, and `notes`.
- The serializer ignores unknown JSON keys, allowing unknown fields to be ignored during decoding.
- Import uses `mLocalId` as the identity and `REPLACE` upsert semantics for collisions.

Current parsing checks are not equivalent to the planned validation: the version is not checked, required content is not validated, and broad parsing failures are converted to an empty result with zero skipped records.

## 6. Subsystem status and technical audit

| Subsystem / Area | Implemented behavior | Current limitations and known issues |
|---|---|---|
| Setup/dependencies | Compose, Room, Hilt, type-safe Navigation, WorkManager, Android RemoteViews, serialization, and reorderable dependencies are configured targeting Java 17 bytecode. Room schema export is enabled and release builds use R8. | Legacy storage and unused foreground service permissions removed. |
| Domain/data boundary | ViewModels and use cases operate on domain `Note`; the repository owns entity mapping while DAO/Room remain persistence-specific. | Domain/entity mapping has focused unit coverage; DAO integration tests are still absent. |
| Theme | Always-dark palette and Material 3 theme are implemented directly without loading dynamic palettes. | Inter downloadable font is absent. |
| CRUD/list/search | Room-backed create/edit, blank-note guard, search flow, pin and important controls. | No dedicated DAO tests. |
| Archive | Main-list swipe archives, Archive screen swipe-to-unarchive (right) and swipe-to-delete (left) are implemented with snackbar undo. | Archive bulk management mode is absent. |
| Reordering | Management mode uses the reorderable Compose library and writes updated positions. | No UI test covers reorder behavior. |
| Reminders | Dialog chooses a future date/time and daily/weekly/monthly recurrence; the editor requests Android notification and exact-alarm access when needed. AlarmManager exact-while-idle alarms deliver through a receiver; WorkManager is a warned best-effort fallback. Boot and exact-access-grant broadcasts re-schedule reminders. Notification-channel sound is configured by default and disabled app/channel states link to system settings. Deep-link notifications resolve the target note directly by ID. | Sound still follows user channel settings, Do Not Disturb, and device volume. Archive does not cancel work. Monthly cadence is approximated as 30 days. |
| Snackbar/undo | Main list and Archive screen archive, unarchive, and delete actions all expose undo actions. | — |
| Animations | Note enter visibility, important border color, content-size and Material sheet/swipe transitions are present. | No UI tests verify motion or accessibility behavior. |
| Widget | A native RemoteViews `ListView` collection reads and scrolls through all active notes in DAO order, displays one-line ellipsized previews, and refreshes directly after repository mutations without WorkManager scheduling. The first widget requests a delayed follow-up to avoid notifying before the launcher binds the collection service; ordinary `onUpdate` callbacks do not schedule delayed work. Each row passes its note ID to the app, and the add-note control sends an Activity Intent (`ACTION_CREATE_NOTE`) to open the editor for a new note, including when the app is running. Click bindings refresh after app updates and activity launches. Room read failures propagate instead of being converted to an empty list. | — |
| Import/export | File chooser, import manager, legacy compatibility, JSON export, FileProvider share intent are present. | Export collection waits on a non-terminating Flow and only observes active notes. Parser errors can look like a zero-error import; import does not reschedule reminder jobs. |
| Testing | Import/export dataset & round-trip tests; daily/weekly/monthly simple interval parser tests. | No in-memory Room DAO tests or planned Compose UI tests. |

### Important concrete behavior gaps

1. **Export does not take a one-time snapshot.** `SettingsViewModel.exportData()` calls `collect` on `observeActiveNotes()`, which is a Room `Flow` that normally stays active. Its `return@collect` only returns from the current collection callback; it does not stop collection. The export function therefore does not reliably reach serialization/sharing. The queried source also excludes archived notes.
2. **Archive unarchive undo and delete (resolved).** `ArchiveViewModel.archiveBack()` now re-archives the note via `ArchiveNoteUseCase`, and `deleteNote()` exposes an UNDO action that restores the archived note via `SaveNoteUseCase`. Swipe-to-delete is enabled in `ArchiveScreen`.
3. **Reminder lifecycle is inconsistent.** Reminder alarms are scheduled/cancelled from the editor save path, but repository archive/delete operations do not cancel them. Exact timing requires Android exact-alarm access; without it, reminders use best-effort WorkManager scheduling. Repeating monthly intervals are fixed to 30 days. Notification permission and channel settings must allow alerts; the editor requests permission and provides system-settings actions when notification or sound settings block delivery.
4. **Notification deep-link lookup (resolved).** Deep links are keyed on the unique navigation request ID and load notes directly by ID via `GetNoteByIdUseCase`, correctly resolving both active and archived notes without redundant lookups on list emissions. Intent extra keys are aligned to `MainActivity.EXTRA_NOTE_ID` (`"note_id"`) with legacy `"noteId"` fallback.
5. **Import errors are not surfaced faithfully.** `importFromStream()` catches broad exceptions and returns `(emptyList(), 0)`, which the Settings UI can report as a zero-record import rather than a parse failure.

## 7. Tests and verification

Current test sources:

- `ImportExportTest` checks legacy JSON import/export and round-trip behavior.
- `ReminderSchedulerTest` checks basic daily/weekly/monthly interval conversion, unknown rules, and next occurrence behavior.
- `NoteMappersTest` verifies all stored entity fields survive a domain-model round trip.
- `NoteDateFormatsTest` verifies the legacy timestamp format round-trips in the system timezone.

Automated coverage currently lacks DAO tests against in-memory Room and Compose UI tests for main-list, editor, archive, undo, search, and widget interactions. Those tests are not present in `app/src/test` or `app/src/androidTest` based on the current source tree.

Verification for the architecture modernization:

- `.\gradlew.bat :app:testDebugUnitTest` — **BUILD SUCCESSFUL**.
- `.\gradlew.bat :app:assembleDebug :app:assembleRelease` — **BUILD SUCCESSFUL**, including R8 minification.
- R8 reports a Kotlin metadata parsing warning with the current Kotlin/R8 versions; the release APK is still produced successfully.

This verification establishes compilation and passing configured JVM tests; it does not verify the app's interactive behavior on a device.

## 8. Recommended completion order

These are development priorities based on identified issues and technical debt:

1. Fix export to read one snapshot of **all** notes, including archived entries; test error reporting and exported shape.
2. Add focused repository/ViewModel tests for Archive undo and deletion.
3. Make import failures explicit, validate supported schema versions and required fields, and ensure input streams are closed.
4. Define reminder semantics for edits, archive, deletion, import, repeating dates, and boot re-scheduling; implement and test each lifecycle transition.
5. Add DAO tests and Compose UI tests; review SDK minimum and typography requirements with release expectations.

## 9. Source map

```text
app/src/main/java/com/nicenotes/
├── data/
│   ├── db/
│   │   ├── entity/          NoteEntity
│   │   ├── NoteMappers.kt   Room/domain conversions
│   │   ├── NoteDao.kt
│   │   └── NoteDatabase.kt
│   ├── export/              ExportSchema (ExportRoot/ExportNote), LegacyNote, ImportExportManager
│   └── repository/          NoteRepository
├── di/                      DatabaseModule, NotificationModule
├── domain/
│   ├── model/               Note
│   └── usecase/             UseCases (CRUD, archive, reorder, note lookup)
├── notification/            ReminderScheduler, exact receiver, WorkManager fallback, delivery, notifications, boot receiver
├── ui/
│   ├── Screen.kt            Navigation route definitions
│   ├── common/              Shared UI events
│   ├── component/           ReminderDialog
│   ├── screen/main/         Active notes, search, management mode, editor entry
│   ├── screen/editor/       Note editor sheet and ViewModel
│   ├── screen/archive/      Archived notes and ViewModel
│   ├── screen/settings/     Import/export and settings
│   └── theme/               Colors, typography, Material theme
├── util/                    Immutable java.time note date formatters
├── widget/                  RemoteViews widget provider and WorkManager update worker
├── MainActivity.kt
└── NiceNotesApp.kt
```
