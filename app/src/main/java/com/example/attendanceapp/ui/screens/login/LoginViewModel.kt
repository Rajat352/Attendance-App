package com.example.attendanceapp.ui.screens.login

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.repository.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "LoginVM"

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepo
): ViewModel() {

    private val _uiState = MutableStateFlow(LoginScreenState())
    val uiState: StateFlow<LoginScreenState> = _uiState.asStateFlow()

    fun setUserName(name: String) {
        _uiState.update {
            it.copy(
                userName = name
            )
        }
    }

    fun setPassword(pass: String) {
        _uiState.update {
            it.copy(
                password = pass
            )
        }
    }

    fun login() {
        val currState = uiState.value
        val userName = currState.userName
        val pass = currState.password

        if(userName.isBlank() || pass.isBlank()) {
            _uiState.update {
                it.copy(
                    status = LoginScreenState.Status.Error("Username or password empty!")
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = LoginScreenState.Status.Loading) }
            authRepo.login(userName, pass)
                .onSuccess { res ->
                    if (!res.role.isNullOrEmpty()) {
                        _uiState.update {
                            it.copy(status = LoginScreenState.Status.Success(res.role))
                        }
                    } else {
                        _uiState.update {
                            it.copy(status = LoginScreenState.Status.Error("Role null or empty"))
                        }
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            status = LoginScreenState.Status.Error(error.message ?: "Unknown error occurred")
                        )
                    }
                }
        }
    }
}

@Stable
data class LoginScreenState(
    val userName: String = "",
    val password: String = "",
    val status: Status = Status.Idle
) {
    sealed interface Status {
        data object Idle: Status
        data object Loading: Status
        data class Error(val message: String): Status
        data class Success(val role: String): Status
    }
}