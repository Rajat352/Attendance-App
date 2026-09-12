package com.example.attendanceapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.attendanceapp.ui.screens.admin.AdminScreen
import com.example.attendanceapp.ui.screens.login.LoginScreen
import com.example.attendanceapp.ui.screens.staff.StaffScreen

@Composable
fun RootNav(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(Route.TopLevel.Login)

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        NavDisplay(
            modifier = modifier.padding(paddingValues),
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<Route.TopLevel.Login> {
                    LoginScreen()
                }
                entry<Route.TopLevel.Admin> {
                    AdminScreen()
                }
                entry<Route.TopLevel.Staff> {
                    StaffScreen()
                }
            }
        )
    }
}