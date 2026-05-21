package com.realfilters.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()

    // Create and remember the ViewModel at the NavHost level
    val filterViewModel: FilterViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = filterViewModel,
                onNavigateToEditor = { navController.navigate("editor") }
            )
        }
        composable("editor") {
            FilterEditorScreen(
                viewModel = filterViewModel,
                themeViewModel = themeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
