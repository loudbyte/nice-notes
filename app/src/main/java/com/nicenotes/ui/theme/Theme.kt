package com.nicenotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isSystemInDarkTheme()) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        NiceNotesDarkColorScheme
    }.copy(
        background = Black,
        surface = Surface,
        surfaceVariant = SurfaceVariant,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        secondary = ReminderBlue,
        onSecondary = Black,
        error = ImportantRed,
        outline = Divider,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NiceNotesTypography,
        content = content
    )
}
