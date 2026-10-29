package com.nicenotes.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nicenotes.ui.theme.*
import com.nicenotes.util.NoteDateFormats
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    initialAlarmTime: Long?,
    initialRrule: String,
    onConfirm: (Long?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val calendar = remember {
        Calendar.getInstance().apply {
            initialAlarmTime?.let { timeInMillis = it }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(initialAlarmTime) }
    var repeatEnabled by remember { mutableStateOf(initialRrule.isNotEmpty()) }
    var repeatOption by remember { mutableStateOf(initialRrule.ifEmpty { "FREQ=DAILY;INTERVAL=1;WKST=SU" }) }
    var showPastTimeError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialAlarmTime ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date ->
                        calendar.timeInMillis = date
                        selectedTime = calendar.timeInMillis
                        showPastTimeError = false
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
        return
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = SurfaceVariant,
            title = { Text("Pick time", color = TextPrimary) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    calendar.set(Calendar.SECOND, 0)
                    selectedTime = calendar.timeInMillis
                    showPastTimeError = false
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Set reminder", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date row
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (selectedTime != null)
                            NoteDateFormats.formatDate(selectedTime!!)
                        else "Pick date"
                    )
                }

                // Time row
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (selectedTime != null)
                            NoteDateFormats.formatTime(selectedTime!!)
                        else "Pick time"
                    )
                }

                // Repeat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = repeatEnabled,
                        onCheckedChange = { repeatEnabled = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ReminderBlue,
                            uncheckedColor = TextSecondary
                        )
                    )
                    Text("Repeat", color = TextPrimary)
                }

                if (repeatEnabled) {
                    val options = listOf(
                        "Every day" to "FREQ=DAILY;INTERVAL=1;WKST=SU",
                        "Every week" to "FREQ=WEEKLY;INTERVAL=1;WKST=SU",
                        "Every month" to "FREQ=MONTHLY;INTERVAL=1;WKST=SU",
                    )
                    options.forEach { (label, rrule) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = repeatOption == rrule,
                                onClick = { repeatOption = rrule },
                                colors = RadioButtonDefaults.colors(selectedColor = ReminderBlue)
                            )
                            Text(label, color = TextSecondary)
                        }
                    }
                }

                if (showPastTimeError) {
                    Text(
                        "Choose a future date and time.",
                        color = ImportantRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Clear reminder option
                if (initialAlarmTime != null) {
                    TextButton(
                        onClick = { onConfirm(null, "") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove reminder", color = ImportantRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val time = selectedTime
                    if (time == null || time <= System.currentTimeMillis()) {
                        showPastTimeError = true
                    } else {
                        onConfirm(time, if (repeatEnabled) repeatOption else "")
                    }
                },
                colors = niceNotesPrimaryButtonColors(),
                enabled = selectedTime != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
