package com.nicenotes.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val remindersFirst: Boolean
        get() = preferences.getBoolean(KEY_REMINDERS_FIRST, true)

    fun setRemindersFirst(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_REMINDERS_FIRST, enabled).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "note_preferences"
        const val KEY_REMINDERS_FIRST = "reminders_first"
    }
}
