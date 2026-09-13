package com.example.attendanceapp.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route: NavKey {
    sealed interface TopLevel: Route {
        @Serializable data object Login: TopLevel
        @Serializable data object Admin: TopLevel
        @Serializable data object Staff: TopLevel
    }

    @Serializable
    data class StaffDetails(val staffId: Int): Route
}