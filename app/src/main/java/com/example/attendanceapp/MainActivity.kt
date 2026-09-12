package com.example.attendanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attendanceapp.ui.navigation.RootNav
import com.example.attendanceapp.ui.navigation.Route
import com.example.attendanceapp.ui.screens.AuthState
import com.example.attendanceapp.ui.screens.MainViewModel
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceAppTheme {

                val authState by viewModel.authState.collectAsStateWithLifecycle()

                when(val state = authState) {
                    is AuthState.Loading -> {
                    }
                    is AuthState.Unauthenticated -> {
                        RootNav(startDestination = Route.TopLevel.Login)
                    }
                    is AuthState.Authenticated -> {
                        RootNav(startDestination = state.startRoute)
                    }
                }
            }
        }
    }
}