package com.nicenotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nicenotes.ui.Screen
import com.nicenotes.ui.screen.archive.ArchiveScreen
import com.nicenotes.ui.screen.main.MainScreen
import com.nicenotes.ui.screen.settings.SettingsScreen
import com.nicenotes.ui.theme.NiceNotesTheme
import com.nicenotes.widget.NotesWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navigationRequest by mutableStateOf(NavigationRequest())
    private var requestSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotesWidgetReceiver.updateWidget(this)
        updateNavigationRequest(intent)

        setContent {
            NiceNotesTheme {
                val request = navigationRequest
                NiceNotesNavGraph(
                    openNoteId = request.noteId,
                    openNoteRequestId = request.id,
                    createNoteRequestId = request.id.takeIf { request.createNote }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotesWidgetReceiver.updateWidget(this)
        updateNavigationRequest(intent)
    }

    private fun updateNavigationRequest(intent: Intent) {
        navigationRequest = NavigationRequest(
            id = ++requestSequence,
            noteId = intent.getStringExtra(EXTRA_NOTE_ID),
            createNote = intent.action == ACTION_CREATE_NOTE ||
                intent.getStringExtra(EXTRA_ACTION) == ACTION_CREATE
        )
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_ACTION = "action"
        const val ACTION_CREATE = "create"
        const val ACTION_CREATE_NOTE = "com.nicenotes.action.CREATE_NOTE"
    }
}

private data class NavigationRequest(
    val id: Long = 0,
    val noteId: String? = null,
    val createNote: Boolean = false
)

@Composable
fun NiceNotesNavGraph(
    openNoteId: String? = null,
    openNoteRequestId: Long = 0,
    createNoteRequestId: Long? = null
) {
    val navController = rememberNavController()

    LaunchedEffect(openNoteRequestId, openNoteId, createNoteRequestId) {
        if (openNoteId != null || createNoteRequestId != null) {
            navigateToMain(navController)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigate = { route -> navController.navigate(route) },
                openNoteId = openNoteId,
                openNoteRequestId = openNoteRequestId,
                createNoteRequestId = createNoteRequestId
            )
        }
        composable(Screen.Archive.route) {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun navigateToMain(navController: NavHostController) {
    val currentRoute = navController.currentDestination?.route ?: return
    if (currentRoute != Screen.Main.route &&
        !navController.popBackStack(Screen.Main.route, inclusive = false)) {
        navController.navigate(Screen.Main.route) {
            launchSingleTop = true
        }
    }
}
