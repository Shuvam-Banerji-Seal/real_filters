package com.realfilters.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToEditor = { navController.navigate("editor") }
            )
        }
        composable("editor") {
            FilterEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
