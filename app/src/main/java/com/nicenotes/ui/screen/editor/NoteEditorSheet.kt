package com.nicenotes.ui.screen.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nicenotes.domain.model.Note
import com.nicenotes.notification.NoteNotificationManager
import com.nicenotes.notification.ReminderScheduler
import com.nicenotes.notification.ReminderSchedulingMode
import com.nicenotes.ui.component.ReminderDialog
import com.nicenotes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    note: Note?,
    onDismiss: () -> Unit,
    onMessage: (String, (() -> Unit)?) -> Unit,
    onDelete: (Note) -> Unit,
    onSaved: (Note) -> Unit = {},
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    LaunchedEffect(note) {
        viewModel.loadNote(note)
    }

    val content by viewModel.content.collectAsState()
    val isPinned by viewModel.isPinned.collectAsState()
    val isImportant by viewModel.isImportant.collectAsState()
    val alarmTime by viewModel.alarmTime.collectAsState()
    val context = LocalContext.current

    var editorValue by remember(note) {
        val initialContent = note?.content.orEmpty()
        mutableStateOf(
            TextFieldValue(
                text = initialContent,
                selection = TextRange(initialContent.length),
            )
        )
    }

    var showReminderDialog by remember { mutableStateOf(false) }

    fun persistNote() {
        viewModel.save { savedNote, schedulingMode ->
            if (savedNote.alarmTime != null) {
                val settings = NoteNotificationManager.notificationSettings(context)
                val issues = buildList {
                    if (!settings.notificationsEnabled) {
                        add("notifications are blocked")
                    } else if (!settings.soundEnabled) {
                        add("reminder notification sound is off")
                    }
                    if (schedulingMode == ReminderSchedulingMode.BEST_EFFORT) {
                        add("exact-alarm access is off, so delivery may be delayed")
                    }
                }
                if (issues.isNotEmpty()) {
                    val settingsIntent = when {
                        !settings.notificationsEnabled ->
                            NoteNotificationManager.appNotificationSettingsIntent(context)
                        !settings.soundEnabled ->
                            NoteNotificationManager.channelSettingsIntent(context)
                        else -> ReminderScheduler.exactAlarmSettingsIntent(context)
                    }
                    onMessage(
                        "Reminder saved, but ${issues.joinToString("; ")}.",
                        { context.startActivity(settingsIntent) }
                    )
                }
            }
            onSaved(savedNote)
            onDismiss()
        }
    }

    fun hasValidReminderTime(): Boolean {
        val selectedAlarmTime = alarmTime
        if (selectedAlarmTime != null &&
            selectedAlarmTime <= System.currentTimeMillis()
        ) {
            onMessage("Choose a future reminder time or remove the reminder before saving.", null)
            return false
        }
        return true
    }

    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasValidReminderTime()) {
            persistNote()
        }
    }

    fun saveAfterNotificationPermission() {
        if (alarmTime == null || ReminderScheduler.canScheduleExactAlarms(context)) {
            persistNote()
        } else {
            exactAlarmPermissionLauncher.launch(
                ReminderScheduler.exactAlarmSettingsIntent(context)
            )
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (hasValidReminderTime()) {
            saveAfterNotificationPermission()
        }
    }

    fun saveNote() {
        if (!hasValidReminderTime()) return

        if (alarmTime != null &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        saveAfterNotificationPermission()
    }

    val focusRequester = remember { FocusRequester() }
    val entranceProgress = remember { Animatable(0f) }

    val density = LocalDensity.current
    val sheetState = remember(density) {
        SheetState(
            skipPartiallyExpanded = true,
            // Match Material 3's defaults; only the initial expanded value is customized.
            positionalThreshold = {
                with(density) { 56.dp.toPx() }
            },
            velocityThreshold = {
                with(density) { 125.dp.toPx() }
            },
            initialValue = SheetValue.Expanded,
        )
    }

    LaunchedEffect(Unit) {
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.graphicsLayer {
            translationY = with(density) { (1f - entranceProgress.value) * 18.dp.toPx() }
            alpha = 0.75f + (0.25f * entranceProgress.value)
        },
        sheetState = sheetState,
        containerColor = SurfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (note == null) "New note" else "Edit note",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (note == null) {
                                onDismiss()
                            } else {
                                onDelete(note)
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = if (note == null) "Discard note" else "Delete note",
                            tint = ImportantRed
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            HorizontalDivider(color = Divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Text area
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            TextField(
                value = editorValue,
                onValueChange = {
                    editorValue = it
                    viewModel.setContent(it.text)
                },
                placeholder = {
                    Text("Write your note...", color = TextSecondary)
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant,
                    cursorColor = AccentWhite,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 350.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { showReminderDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Reminder",
                        tint = if (alarmTime != null) ReminderBlue else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = isImportant,
                        onCheckedChange = { viewModel.toggleImportant() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ImportantRed,
                            uncheckedColor = TextSecondary,
                        )
                    )
                    Text("Important", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }

                IconToggleButton(
                    checked = isPinned,
                    onCheckedChange = { viewModel.togglePin() }
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (isPinned) PinGold else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = ::saveNote,
                    enabled = content.isNotBlank(),
                    colors = niceNotesPrimaryButtonColors(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showReminderDialog) {
        ReminderDialog(
            initialAlarmTime = alarmTime,
            initialRrule = viewModel.repeatRule.collectAsState().value,
            onConfirm = { time, rrule ->
                viewModel.setAlarm(time, rrule)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false }
        )
    }
}
