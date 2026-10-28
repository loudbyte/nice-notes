package com.nicenotes.ui

sealed class Screen(val route: String) {
    object Main     : Screen("main")
    object Archive  : Screen("archive")
    object Settings : Screen("settings")
}
