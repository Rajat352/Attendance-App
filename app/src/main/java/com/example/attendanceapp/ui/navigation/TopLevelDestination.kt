package com.example.attendanceapp.ui.navigation

enum class TopLevelDestination (
    val route: Route.TopLevel
) {
    LOGIN(Route.TopLevel.Login),
    ADMIN(Route.TopLevel.Admin),
    STAFF(Route.TopLevel.Staff)
}