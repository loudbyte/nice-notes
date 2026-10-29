package com.nicenotes.ui.theme

import androidx.compose.material3.MaterialTheme
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
fun NiceNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NiceNotesDarkColorScheme,
        typography = NiceNotesTypography,
        content = content
    )
}
