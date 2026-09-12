package com.example.attendanceapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.dao.SessionUserDao
import com.example.attendanceapp.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AuthState {
    data object Loading: AuthState
    data object Unauthenticated: AuthState
    data class Authenticated(val startRoute: Route.TopLevel): AuthState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionUserDao: SessionUserDao
) : ViewModel() {

    val authState: StateFlow<AuthState> = sessionUserDao.getSessionUser()
        .map { sessionUser ->
            if (sessionUser == null) {
                AuthState.Unauthenticated
            } else if (sessionUser.role == "admin") {
                AuthState.Authenticated(Route.TopLevel.Admin)
            } else {
                AuthState.Authenticated(Route.TopLevel.Staff)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading
        )
}