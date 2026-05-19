package com.realfilters.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val parentEntry = remember(navController.currentBackStackEntry) {
                navController.getBackStackEntry("home")
            }
            val viewModel: FilterViewModel = hiltViewModel(parentEntry)
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { navController.navigate("editor") }
            )
        }
        composable("editor") {
            val parentEntry = remember(navController.currentBackStackEntry) {
                navController.getBackStackEntry("home")
            }
            val viewModel: FilterViewModel = hiltViewModel(parentEntry)
            FilterEditorScreen(
                viewModel = viewModel,
                themeViewModel = themeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
