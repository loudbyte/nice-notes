# NiceNotes — Android Implementation Plan

> **Stack**: Kotlin · Jetpack Compose · Room · WorkManager · Hilt · App Widgets (Glance)  
> **Min SDK**: Android 16 (API 36)  
> **Architecture**: MVVM + Clean Architecture (UI → ViewModel → UseCase → Repository → Room)

---

## Data Schema (derived from `example-of-data.json`)

The existing export file is a **JSON array** of note objects. Below is the complete field map discovered from the 2 164-note dataset:

| Field | Type | Meaning |
|---|---|---|
| `mLocalId` | `String` (UUID) | Primary key |
| `mGlobalId` | `String?` (UUID) | Optional cloud/sync id — keep for import compat |
| `mContent` | `String` | Note text (multiline, `\n`-separated) |
| `mStatus` | `String` | Always `"NEW"` in dataset — keep for future |
| `mCreateTime` | `String` | `"MMM d, yyyy h:mm:ss a"` pattern |
| `mUpdateTime` | `String` | Same pattern |
| `mTimestamp` | `String` | Last sync timestamp — keep for import compat |
| `mFinishTime` | `String?` | Set when note is "done" / archived |
| `mIsDelete` | `Boolean` | Soft-delete / archive flag |
| `mIsLocal` | `Boolean` | Was created locally (not synced) |
| `mAlarmTime` | `String?` | Reminder datetime (`"MMM d, yyyy h:mm:ss a"`) |
| `mAlarmTimePre` | `String?` | Previous alarm (for undo) |
| `mNextAlarmTime` | `String?` | Next occurrence for repeating reminders |
| `mRepeatRulePre` | `String` | iCalendar `RRULE` string (empty = no repeat) |
| `mForceReminderPre` | `Boolean` | Force-show reminder even if past |
| `isReminded` | `Boolean` | Whether alarm has already fired |
| `mColorIndex` | `Int` | `-1` = default; reserved for color theming |
| `mParentId` | `String?` | Parent note UUID (for linked/sub-notes) |
| `mSysVersion` | `Int` | Schema version field |
| `mExtra` | `JsonObject?` | `{ forceReminder: Boolean, repeatRule: String }` |

### New fields added by NiceNotes (not in export schema, stored in Room only)

| Field | Type | Purpose |
|---|---|---|
| `isPinned` | `Boolean` | Pinned to top of list |
| `isImportant` | `Boolean` | Red highlight |
| `position` | `Long` | Manual ordering index |
| `isArchived` | `Boolean` | Replaces `mIsDelete` semantics |

> **Import mapping**: `mIsDelete = true` → `isArchived = true`.  
> **Export schema version**: wrap array in `{ "version": 1, "notes": [...] }`.

---

## Project Structure

```
nice-notes/
├── app/
│   └── src/main/
│       ├── java/com/nicenotes/
│       │   ├── data/
│       │   │   ├── db/
│       │   │   │   ├── NoteDatabase.kt
│       │   │   │   ├── NoteDao.kt
│       │   │   │   └── entity/
│       │   │   │       ├── NoteEntity.kt
│       │   │   │       └── ReminderEntity.kt   ← optional separate table
│       │   │   ├── model/
│       │   │   │   ├── Note.kt                ← domain model
│       │   │   │   └── Reminder.kt
│       │   │   ├── repository/
│       │   │   │   └── NoteRepository.kt
│       │   │   └── export/
│       │   │       ├── ExportSchema.kt        ← data classes for JSON I/O
│       │   │       └── ImportExportManager.kt
│       │   ├── domain/usecase/
│       │   │   ├── GetActiveNotesUseCase.kt
│       │   │   ├── GetArchivedNotesUseCase.kt
│       │   │   ├── SaveNoteUseCase.kt
│       │   │   ├── DeleteNoteUseCase.kt
│       │   │   ├── ArchiveNoteUseCase.kt
│       │   │   ├── ReorderNotesUseCase.kt
│       │   │   └── SearchNotesUseCase.kt
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt
│       │   │   │   ├── Theme.kt
│       │   │   │   └── Type.kt
│       │   │   ├── screen/
│       │   │   │   ├── main/
│       │   │   │   │   ├── MainScreen.kt
│       │   │   │   │   ├── MainViewModel.kt
│       │   │   │   │   ├── NoteListItem.kt
│       │   │   │   │   └── ManagementModeOverlay.kt
│       │   │   │   ├── editor/
│       │   │   │   │   ├── NoteEditorSheet.kt    ← ModalBottomSheet
│       │   │   │   │   └── NoteEditorViewModel.kt
│       │   │   │   ├── archive/
│       │   │   │   │   ├── ArchiveScreen.kt
│       │   │   │   │   └── ArchiveViewModel.kt
│       │   │   │   └── settings/
│       │   │   │       ├── SettingsScreen.kt
│       │   │   │       └── SettingsViewModel.kt
│       │   │   └── component/
│       │   │       ├── ReminderDialog.kt
│       │   │       ├── DeleteConfirmSnackbar.kt
│       │   │       └── SearchBar.kt
│       │   ├── notification/
│       │   │   ├── ReminderScheduler.kt       ← WorkManager wrapper
│       │   │   ├── ReminderWorker.kt
│       │   │   └── NoteNotificationManager.kt
│       │   ├── widget/
│       │   │   ├── NotesWidget.kt             ← Glance AppWidget
│       │   │   ├── NotesWidgetReceiver.kt
│       │   │   └── WidgetViewModel.kt
│       │   ├── di/
│       │   │   ├── DatabaseModule.kt
│       │   │   ├── RepositoryModule.kt
│       │   │   └── NotificationModule.kt
│       │   └── MainActivity.kt
│       └── res/
│           ├── xml/
│           │   └── notes_widget_info.xml
│           └── layout/
│               └── notes_widget.xml           ← initial size hints
```

---

## Phase 1 — Core (CRUD + UI)

### Step 1 · Project setup

1. Create new Android project via Android Studio → **Empty Activity (Compose)**.
2. Set `minSdk = 36`, `targetSdk = 36`, `compileSdk = 36`.
3. Add dependencies to `app/build.gradle.kts`:

```kotlin
// Room
implementation("androidx.room:room-runtime:2.7.x")
implementation("androidx.room:room-ktx:2.7.x")
ksp("androidx.room:room-compiler:2.7.x")

// Hilt
implementation("com.google.dagger:hilt-android:2.51.x")
ksp("com.google.dagger:hilt-compiler:2.51.x")
implementation("androidx.hilt:hilt-navigation-compose:1.2.x")

// Compose BOM
implementation(platform("androidx.compose:compose-bom:2025.x"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.10.x")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.x")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.10.x")
implementation("androidx.hilt:hilt-work:1.2.x")

// Glance (widget)
implementation("androidx.glance:glance-appwidget:1.1.x")
implementation("androidx.glance:glance-material3:1.1.x")

// Kotlinx serialization (for import/export JSON)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.x")
```

---

### Step 2 · Theme — Black/Dark design

**`Color.kt`**
```kotlin
val Black = Color(0xFF000000)
val Surface = Color(0xFF0D0D0D)
val SurfaceVariant = Color(0xFF1A1A1A)
val CardBackground = Color(0xFF141414)
val ImportantRed = Color(0xFFE53935)
val ImportantRedSubtle = Color(0xFF3B0F0F)
val PinGold = Color(0xFFFFC107)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFF888888)
val Divider = Color(0xFF2A2A2A)
val AccentWhite = Color(0xFFFFFFFF)
val ReminderBlue = Color(0xFF42A5F5)
```

**`Theme.kt`** — always dark, no system-theme switching in MVP.

```kotlin
@Composable
fun NiceNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Black,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            primary = AccentWhite,
            onPrimary = Black,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
        ),
        typography = NiceNotesTypography,
        content = content
    )
}
```

**Typography**: Use **Inter** from Google Fonts via `downloadable fonts` resource.

---

### Step 3 · Data layer — Room

**`NoteEntity.kt`**

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val mLocalId: String,           // UUID from export
    val mGlobalId: String? = null,
    val mContent: String,
    val mStatus: String = "NEW",
    val mCreateTime: Long,                       // stored as epoch millis
    val mUpdateTime: Long,
    val mTimestamp: Long,
    val mFinishTime: Long? = null,
    val mIsDelete: Boolean = false,              // kept for export compat
    val mIsLocal: Boolean = true,
    val mAlarmTime: Long? = null,
    val mAlarmTimePre: Long? = null,
    val mNextAlarmTime: Long? = null,
    val mRepeatRulePre: String = "",
    val mForceReminderPre: Boolean = false,
    val isReminded: Boolean = false,
    val mColorIndex: Int = -1,
    val mParentId: String? = null,
    val mSysVersion: Int = 1,
    val mExtraForceReminder: Boolean = false,    // flattened from mExtra
    val mExtraRepeatRule: String = "",

    // NiceNotes-specific fields
    val isPinned: Boolean = false,
    val isImportant: Boolean = false,
    val isArchived: Boolean = false,
    val position: Long = System.currentTimeMillis(),
)
```

> **Date handling**: Store all times as epoch milliseconds (`Long`). During import, parse the `"MMM d, yyyy h:mm:ss a"` format with `SimpleDateFormat` using `Locale.ENGLISH`.

**`NoteDao.kt`** — key queries:

```kotlin
@Dao
interface NoteDao {
    // Active notes ordered: pinned first, then by position desc
    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
        ORDER BY isPinned DESC, position DESC
    """)
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
          AND mContent LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, position DESC
    """)
    fun searchActiveNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY mUpdateTime DESC")
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE mLocalId = :id")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT MAX(position) FROM notes WHERE isArchived = 0")
    suspend fun maxPosition(): Long?
}
```

---

### Step 4 · Repository

**`NoteRepository.kt`** wraps the DAO:

```kotlin
class NoteRepository @Inject constructor(private val dao: NoteDao) {

    fun observeActiveNotes() = dao.observeActiveNotes()
    fun observeArchivedNotes() = dao.observeArchivedNotes()
    fun searchNotes(query: String) = dao.searchActiveNotes(query)

    suspend fun save(note: NoteEntity) = dao.upsert(note)
    suspend fun delete(note: NoteEntity) = dao.delete(note)

    suspend fun archive(note: NoteEntity) =
        dao.update(note.copy(isArchived = true, isPinned = false,
            mIsDelete = true, mFinishTime = System.currentTimeMillis()))

    suspend fun unarchive(note: NoteEntity) =
        dao.update(note.copy(isArchived = false, mIsDelete = false,
            mFinishTime = null))

    suspend fun updatePositions(updates: List<Pair<String, Long>>) {
        updates.forEach { (id, pos) ->
            dao.getById(id)?.let { dao.update(it.copy(position = pos)) }
        }
    }

    suspend fun nextPosition() = (dao.maxPosition() ?: 0L) + 1000L
}
```

---

### Step 5 · Main screen

**`MainScreen.kt`** — Scaffold with:

- **Top**: `SearchBar` composable (always visible, `BasicTextField`, magnifier icon).
- **Body**: `LazyColumn` of `NoteListItem`.
- **FAB**: `+` button bottom-right → opens `NoteEditorSheet`.
- **Top-right**: `IconButton("⋮")` → `DropdownMenu` with **Settings** and **Archive**.

**`NoteListItem.kt`** — each item shows:

```
[Red left border if important] [📌 if pinned] Note text (2-line max)
                               [🔔 reminder time if set]  [timestamp]
```

Interaction handlers:
- `Modifier.clickable` → open editor sheet.
- `Modifier.combinedClickable(onLongClick = ...)` → enter management mode (set `isManagementMode = true` in ViewModel state).
- `Modifier.swipeable` (or `AnchoredDraggable`) → swipe right to archive.

**Swipe-to-archive**: Use `SwipeToDismissBox` from Material3 with a green archive background revealed on swipe right. On dismiss: call `ArchiveNoteUseCase`, show `Snackbar("Note archived", actionLabel = "UNDO")`.

---

### Step 6 · Note editor (ModalBottomSheet)

**`NoteEditorSheet.kt`**:

```
┌──────────────────────────────────────────┐
│  ● New note / Edit note            [✕]   │  ← drag handle + title + close
├──────────────────────────────────────────┤
│                                          │
│  [TextField — fills available space]     │  ← focusRequester, auto-focus
│                                          │
├──────────────────────────────────────────┤
│  🔔 [reminder text / "Add reminder"]    │
│  ☐ Important     📌 [pin toggle]         │
├──────────────────────────────────────────┤
│                              [ Save ]    │
└──────────────────────────────────────────┘
```

- Sheet height: `SheetValue.Expanded` by default.
- `FocusRequester` + `LaunchedEffect` to auto-open keyboard.
- **Save** button: disabled if content is blank; calls `SaveNoteUseCase`; dismisses sheet.
- **Important** toggle: `Checkbox` that sets `isImportant`.
- **Pin** toggle: `IconToggleButton` with pin icon.
- **Reminder row**: tapping opens `ReminderDialog`.

---

### Step 7 · Management mode

When `isManagementMode = true`, the `LazyColumn` switches to a `ReorderableColumn` (use the **`sh.calvin.reorderable`** library: `implementation("sh.calvin.reorderable:reorderable:2.4.x")`):

```
[☰ drag handle]  Note text  [🗑 delete]
```

- Drag handle: `Modifier.draggable` bound to the reorderable item.
- On reorder complete: call `ReorderNotesUseCase` to update `position` values.
- Delete icon: immediately delete, show **Snackbar with UNDO** (keep deleted note in memory for undo window).
- Exit: `BackHandler { isManagementMode = false }`.

---

### Step 8 · Navigation

Use `NavHost` with named routes:

```kotlin
sealed class Screen(val route: String) {
    object Main     : Screen("main")
    object Archive  : Screen("archive")
    object Settings : Screen("settings")
}
```

Navigation is triggered from the three-dot menu and back-stack.

---

### Step 9 · Archive screen

`ArchiveScreen.kt` — identical `LazyColumn` but sourced from `observeArchivedNotes()`. Each item has a **long-press management mode** too. Swipe-right → unarchive (reverse of main screen behavior).

---

### Step 10 · Settings screen

Three sections:

| Section | Content |
|---|---|
| **Appearance** | Static "Theme: Black" label |
| **Data** | `Export data` button, `Import data` button |
| **About** | `Version 1.0` |

---

## Phase 2 — Productivity (Reminders + Polish)

### Step 11 · Reminder dialog

**`ReminderDialog.kt`** — `AlertDialog` containing:

1. **Date row** → calls `DatePickerDialog` (Material3 `DatePicker`).
2. **Time row** → calls `TimePickerDialog` (Material3 `TimePicker`).
3. **Repeat checkbox** → when checked, radio group:
   - `Every day` → RRULE: `FREQ=DAILY;INTERVAL=1;WKST=SU`
   - `Every week` → RRULE: `FREQ=WEEKLY;INTERVAL=1;WKST=SU`
   - `Every month` → RRULE: `FREQ=MONTHLY;INTERVAL=1;WKST=SU`
4. **Cancel / Save** buttons.

On save: store `mAlarmTime` (epoch ms), `mRepeatRulePre` (RRULE string) on the note entity, then call `ReminderScheduler.schedule(note)`.

---

### Step 12 · WorkManager-based reminders

**`ReminderWorker.kt`** — a `CoroutineWorker`:

```kotlin
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: NoteRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getString(KEY_NOTE_ID) ?: return Result.failure()
        val note = repo.getById(noteId) ?: return Result.failure()
        NoteNotificationManager.showReminder(applicationContext, note)
        repo.markReminded(note)

        // Schedule next occurrence if repeating
        if (note.mRepeatRulePre.isNotEmpty()) {
            ReminderScheduler.scheduleNext(applicationContext, note)
        }
        return Result.success()
    }
}
```

**`NoteNotificationManager.kt`**:
- Channel id: `"nicenotes_reminders"`.
- Notification contains note content.
- `PendingIntent` → `MainActivity` with `noteId` extra → opens editor sheet directly.

**`ReminderScheduler.kt`**:
- Uses `WorkManager.enqueueUniqueWork(noteId, REPLACE, OneTimeWorkRequest)` with `setInitialDelay`.
- On note delete / archive / reminder removed: cancel work by `noteId`.
- **Boot receiver** (`BootCompletedReceiver`): re-schedule all active reminders after reboot.

---

### Step 13 · Snackbar / Undo system

Use a shared `SnackbarHostState` in `MainScreen`. Pattern:

```kotlin
// In ViewModel
sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String?, val onAction: () -> Unit) : UiEvent()
}

// In Screen
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is ShowSnackbar -> {
                val result = snackbarHostState.showSnackbar(event.message, event.action)
                if (result == SnackbarResult.ActionPerformed) event.onAction()
            }
        }
    }
}
```

---

### Step 14 · Animations

- Note list item enter: `AnimatedVisibility` with `fadeIn + slideInVertically`.
- Management mode toggle: `AnimateContentSize` on item row.
- Swipe dismiss: built-in `SwipeToDismissBox` animation.
- Sheet open: standard `ModalBottomSheet` animation (already animated by Material3).
- Important left border: `animateColorAsState`.

---

## Phase 3 — Home Screen Widget

### Step 15 · Glance widget setup

**`notes_widget_info.xml`**:

```xml
<appwidget-provider
    android:minWidth="160dp"
    android:minHeight="110dp"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:initialLayout="@layout/notes_widget_loading"
    android:description="@string/widget_description"
    android:configure="com.nicenotes.widget.WidgetConfigActivity" />
```

**`NotesWidget.kt`** — Glance composable:

```kotlin
@Composable
fun NotesWidgetContent(notes: List<NoteEntity>) {
    Column(modifier = GlanceModifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        // Header row
        Row {
            Text("Notes", style = TextStyle(color = ColorProvider(Color.White)))
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_add),
                contentDescription = "Add note",
                modifier = GlanceModifier.clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(MainActivityParams.ACTION to "CREATE")
                    )
                )
            )
        }
        // Scrollable note list
        LazyColumn {
            items(notes) { note ->
                Text(
                    text = note.mContent.take(80),
                    modifier = GlanceModifier.clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(MainActivityParams.NOTE_ID to note.mLocalId)
                        )
                    )
                )
            }
        }
    }
}
```

**`NotesWidgetReceiver.kt`** — extends `GlanceAppWidgetReceiver`. On `onUpdate`, collects first page of notes from Room and calls `update()`.

**Widget update trigger**: When any note changes in Room, call `NotesWidgetReceiver.update(context)`. Do this inside `NoteRepository` after every `save` / `delete` / `archive`.

---

## Phase 4 — Data Portability

### Step 16 · Export

**`ExportSchema.kt`**:

```kotlin
@Serializable
data class ExportRoot(
    val version: Int = 1,
    val exportedAt: String,        // ISO-8601
    val notes: List<ExportNote>
)

@Serializable
data class ExportNote(
    val mLocalId: String,
    val mGlobalId: String? = null,
    val mContent: String,
    val mStatus: String = "NEW",
    val mCreateTime: String,       // "MMM d, yyyy h:mm:ss a"
    val mUpdateTime: String,
    val mTimestamp: String,
    val mFinishTime: String? = null,
    val mIsDelete: Boolean,
    val mIsLocal: Boolean = true,
    val mAlarmTime: String? = null,
    val mAlarmTimePre: String? = null,
    val mNextAlarmTime: String? = null,
    val mRepeatRulePre: String = "",
    val mForceReminderPre: Boolean = false,
    val isReminded: Boolean = false,
    val mColorIndex: Int = -1,
    val mParentId: String? = null,
    val mSysVersion: Int = 1,
    val mExtra: ExportExtra? = null,
    // NiceNotes extensions
    val isPinned: Boolean = false,
    val isImportant: Boolean = false,
    val isArchived: Boolean = false,
    val position: Long = 0L,
)

@Serializable
data class ExportExtra(
    val forceReminder: Boolean = false,
    val repeatRule: String = "",
)
```

**`ImportExportManager.kt`**:

- **Export flow**:
  1. Load all notes from Room.
  2. Serialize to `ExportRoot` JSON.
  3. Write to `FileProvider` cache file: `nicenotes_backup_YYYYMMDD.json`.
  4. Launch `Intent.ACTION_SEND` with MIME `application/json`.

- **Import flow**:
  1. Launch `Intent.ACTION_GET_CONTENT` with MIME `application/json`.
  2. Read file content via `ContentResolver.openInputStream`.
  3. Parse JSON → validate:
     - `version` field must be `1` (or handle future migrations).
     - Each note must have `mLocalId` and `mContent`.
     - Invalid records are skipped and logged.
  4. Call `dao.upsertAll(notes)` — existing notes with same `mLocalId` are updated.
  5. Show result: `"Imported X notes, skipped Y invalid records"`.

---

## Testing Checklist

### Unit tests
- [ ] `NoteDao` queries with in-memory Room database.
- [ ] `ImportExportManager` parses `example-of-data.json` correctly (2164 notes, 809 archived).
- [ ] `ReminderScheduler` schedules/cancels correctly.
- [ ] RRULE parsing for all 4 patterns found in dataset.

### UI tests (Compose)
- [ ] Main screen renders note list.
- [ ] Long press enters management mode.
- [ ] Swipe right archives note and shows snackbar.
- [ ] UNDO restores note.
- [ ] Search filters in real time.
- [ ] Editor sheet opens and saves note.
- [ ] Widget + button creates note.

---

## Delivery Order

```
Week 1  ─── Steps 1–8   (Project + Data layer + Main screen + Editor)
Week 2  ─── Steps 9–10  (Archive + Settings screens)
Week 3  ─── Steps 11–14 (Reminders + WorkManager + Snackbar + Animations)
Week 4  ─── Steps 15    (Glance widget)
Week 5  ─── Steps 16    (Import / Export)
Week 6  ─── Testing + Polish + Release prep
```

---

## Key Decisions Summary

| Decision | Choice | Rationale |
|---|---|---|
| Architecture | MVVM + Clean | Testable, standard Android |
| DI | Hilt | First-class Compose + WorkManager support |
| Database | Room | Type-safe, reactive Flow |
| Serialization | kotlinx.serialization | Kotlin-native, no reflection |
| Widget | Glance | Modern, Compose-style API |
| Reminders | WorkManager | Survives process death and reboots |
| Date format | Epoch millis in DB | Locale-safe; parse legacy format on import |
| Drag-to-reorder | sh.calvin.reorderable | Mature, well-maintained Compose library |
| Export format | `{ version, exportedAt, notes[] }` | Schema versioning for future migrations |
| Empty notes | Not saved | Enforced in `SaveNoteUseCase` |
| Pinned + archive | Unpin on archive | Pinned is a main-screen concept only |
