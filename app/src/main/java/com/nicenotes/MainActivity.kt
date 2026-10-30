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
import com.nicenotes.ui.ArchiveRoute
import com.nicenotes.ui.MainRoute
import com.nicenotes.ui.SettingsRoute
import com.nicenotes.ui.screen.archive.ArchiveScreen
import com.nicenotes.ui.screen.main.MainScreen
import com.nicenotes.ui.screen.settings.SettingsScreen
import com.nicenotes.ui.theme.NiceNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navigationRequest by mutableStateOf(NavigationRequest())
    private var requestSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        updateNavigationRequest(intent)
    }

    private fun updateNavigationRequest(intent: Intent) {
        navigationRequest = NavigationRequest(
            id = ++requestSequence,
            noteId = intent.getStringExtra(EXTRA_NOTE_ID)
                ?: intent.getStringExtra(EXTRA_LEGACY_NOTE_ID),
            createNote = intent.action == ACTION_CREATE_NOTE ||
                intent.getStringExtra(EXTRA_ACTION) == ACTION_CREATE
        )
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_LEGACY_NOTE_ID = "noteId"
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
        startDestination = MainRoute
    ) {
        composable<MainRoute> {
            MainScreen(
                onNavigateToArchive = { navController.navigate(ArchiveRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                openNoteId = openNoteId,
                openNoteRequestId = openNoteRequestId,
                createNoteRequestId = createNoteRequestId
            )
        }
        composable<ArchiveRoute> {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun navigateToMain(navController: NavHostController) {
    if (!navController.popBackStack<MainRoute>(inclusive = false)) {
        navController.navigate(MainRoute) {
            launchSingleTop = true
        }
    }
}
