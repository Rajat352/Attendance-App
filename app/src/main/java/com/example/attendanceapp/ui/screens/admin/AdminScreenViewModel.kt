package com.example.attendanceapp.ui.screens.admin

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.dto.StaffListCache
import com.example.attendanceapp.data.repository.StaffRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminScreenViewModel @Inject constructor(
    private val staffRepo: StaffRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminScreenState())
    val uiState: StateFlow<AdminScreenState> = _uiState.asStateFlow()

    init {
        // Staff list populated from RoomDB
        viewModelScope.launch {
            staffRepo.staffList.collect { list ->
                _uiState.update { it.copy(staffList = list) }
            }
        }
        refreshStaffList()
    }

    fun refreshStaffList() {
        viewModelScope.launch {
            _uiState.update { it.copy(listStatus = AdminScreenState.ListStatus.Loading) }
            staffRepo.refreshStaffList()
                .onSuccess {
                    _uiState.update {
                        it.copy(listStatus = AdminScreenState.ListStatus.Idle)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            listStatus = AdminScreenState.ListStatus.Error(
                                error.message ?: "Failed to load staff list"
                            )
                        )
                    }
                }
        }
    }

    fun openAddStaffDialog() {
        _uiState.update {
            it.copy(dialogState = AdminScreenState.DialogState.Visible())
        }
    }

    fun closeAddStaffDialog() {
        _uiState.update {
            it.copy(dialogState = AdminScreenState.DialogState.Hidden)
        }
    }

    fun setNewStaffName(name: String) {
        val currentDialog = _uiState.value.dialogState
        if (currentDialog is AdminScreenState.DialogState.Visible) {
            _uiState.update {
                it.copy(dialogState = currentDialog.copy(name = name, error = null))
            }
        }
    }

    fun setNewStaffId(id: String) {
        val currentDialog = _uiState.value.dialogState
        if (currentDialog is AdminScreenState.DialogState.Visible) {
            _uiState.update {
                it.copy(dialogState = currentDialog.copy(staffId = id, error = null))
            }
        }
    }

    fun createStaff() {
        val currentDialog = _uiState.value.dialogState
        if (currentDialog !is AdminScreenState.DialogState.Visible) return

        val name = currentDialog.name.trim()
        val idText = currentDialog.staffId.trim()

        if (name.isBlank() || idText.isBlank()) {
            _uiState.update {
                it.copy(dialogState = currentDialog.copy(error = "Name and Employee ID cannot be empty"))
            }
            return
        }

        val staffId = idText.toIntOrNull()
        if (staffId == null) {
            _uiState.update {
                it.copy(dialogState = currentDialog.copy(error = "Employee ID must be a valid number"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(dialogState = currentDialog.copy(isSubmitting = true, error = null))
            }
            try {
                staffRepo.addStaff(staffId, name)
                    .onSuccess {
                        _uiState.update {
                            it.copy(dialogState = AdminScreenState.DialogState.Hidden)
                        }
                    }
                    .onFailure { error ->
                        val activeDialog = _uiState.value.dialogState
                        if (activeDialog is AdminScreenState.DialogState.Visible) {
                            _uiState.update {
                                it.copy(
                                    dialogState = activeDialog.copy(
                                        isSubmitting = false,
                                        error = error.message ?: "Failed to add staff"
                                    )
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                val activeDialog = _uiState.value.dialogState
                if (activeDialog is AdminScreenState.DialogState.Visible) {
                    _uiState.update {
                        it.copy(
                            dialogState = activeDialog.copy(
                                isSubmitting = false,
                                error = e.message ?: "An unexpected error occurred"
                            )
                        )
                    }
                }
            }
        }
    }
}

@Stable
data class AdminScreenState(
    val staffList: List<StaffListCache> = emptyList(),
    val listStatus: ListStatus = ListStatus.Idle,
    val dialogState: DialogState = DialogState.Hidden
) {
    sealed interface ListStatus {
        data object Idle : ListStatus
        data object Loading : ListStatus
        data class Error(val message: String) : ListStatus
    }

    sealed interface DialogState {
        data object Hidden : DialogState
        data class Visible(
            val name: String = "",
            val staffId: String = "",
            val isSubmitting: Boolean = false,
            val error: String? = null
        ) : DialogState
    }
}