package com.example.assignment3_datastorage

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "contactList") {
        composable("contactList") { ContactListScreen(navController) }

        composable("contactEdit/{contactId}") { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")
            ContactEditScreen(navController, contactId)
        }
    }
}
