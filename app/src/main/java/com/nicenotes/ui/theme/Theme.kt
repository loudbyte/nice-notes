package com.nicenotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NiceNotesDarkColorScheme = darkColorScheme(
    background = Black,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    primary = AccentWhite,
    onPrimary = Black,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    secondary = ReminderBlue,
    onSecondary = Black,
    error = ImportantRed,
    outline = Divider,
)

@Composable
fun niceNotesPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Black,
    contentColor = AccentWhite,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
)

@Composable
fun NiceNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NiceNotesDarkColorScheme,
        typography = NiceNotesTypography,
        content = content
    )
}
