package com.nicenotes.ui.screen.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicenotes.domain.model.Note
import com.nicenotes.ui.theme.*
import com.nicenotes.util.NoteDateFormats

@Composable
fun NoteListItem(
    note: Note,
    isManagementMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val borderColor = animateColorAsState(
        targetValue = if (note.isImportant) ImportantRed else Color.Transparent,
        animationSpec = tween(300),
        label = "importantBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (note.isImportant) ImportantRedSubtle else CardBackground
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Important indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(64.dp)
                .background(
                    color = borderColor.value,
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
        )

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = PinGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isManagementMode && dragHandle != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.alarmTime != null) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder",
                                tint = ReminderBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = NoteDateFormats.formatNoteDateTime(note.alarmTime),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ReminderBlue,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier.width(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        dragHandle()
                    }
                    Text(
                        text = NoteDateFormats.formatNoteDate(note.updatedAt),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.alarmTime != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder",
                                tint = ReminderBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = NoteDateFormats.formatNoteDateTime(note.alarmTime),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ReminderBlue,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = NoteDateFormats.formatNoteDate(note.updatedAt),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
