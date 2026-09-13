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
import com.example.attendanceapp.ui.screens.admin.StaffDetailsScreen
import com.example.attendanceapp.ui.screens.login.LoginScreen
import com.example.attendanceapp.ui.screens.staff.StaffScreen

@Composable
fun RootNav(
    modifier: Modifier = Modifier,
    startDestination: Route.TopLevel = Route.TopLevel.Login
) {
    val backStack = rememberNavBackStack(startDestination)

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
                    LoginScreen(
                        onAdminLogin = {
                            backStack.clear()
                            backStack.add(Route.TopLevel.Admin)
                        },
                        onStaffLogin = {
                            backStack.clear()
                            backStack.add(Route.TopLevel.Staff)
                        }
                    )
                }
                entry<Route.TopLevel.Admin> {
                    AdminScreen(
                        onStaffClick = { staffId ->
                            backStack.add(Route.StaffDetails(staffId))
                        }
                    )
                }
                entry<Route.StaffDetails> { route ->
                    StaffDetailsScreen(
                        staffId = route.staffId,
                        onBackClick = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
                entry<Route.TopLevel.Staff> {
                    StaffScreen()
                }
            }
        )
    }
}