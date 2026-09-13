package com.example.attendanceapp.ui.screens.admin

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.remote.dto.AttendanceDto
import com.example.attendanceapp.data.repository.AttendanceRepo
import com.example.attendanceapp.data.repository.StaffRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffDetailsScreenViewModel @Inject constructor(
    private val staffRepo: StaffRepo,
    private val attendanceRepo: AttendanceRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffDetailsState())
    val uiState: StateFlow<StaffDetailsState> = _uiState.asStateFlow()

    private var currentStaffId: Int = -1
    private var staffJob: Job? = null
    private var attendanceJob: Job? = null

    fun loadStaffDetails(staffId: Int) {
        if (currentStaffId == staffId && _uiState.value.staffId == staffId) {
            return
        }
        currentStaffId = staffId
        _uiState.update {
            it.copy(
                staffId = staffId,
                isStaffLoading = true,
                staffErrorMessage = null
            )
        }

        // Cancel previous observers if any
        staffJob?.cancel()
        staffJob = viewModelScope.launch {
            staffRepo.getStaffById(staffId).collect { staff ->
                if (staff != null) {
                    _uiState.update {
                        it.copy(
                            name = staff.name,
                            isFaceEmbeddingEnrolled = staff.isFaceEmbeddingEnrolled,
                            isStaffLoading = false,
                            staffErrorMessage = null
                        )
                    }
                } else {
                    // Try refreshing staff list from server if not found in cache
                    staffRepo.refreshStaffList()
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isStaffLoading = false,
                                    staffErrorMessage = error.message ?: "Failed to find staff details"
                                )
                            }
                        }
                }
            }
        }

        refreshAttendance()
    }

    fun refreshAttendance() {
        if (currentStaffId <= 0) return

        attendanceJob?.cancel()
        attendanceJob = viewModelScope.launch {
            _uiState.update {
                it.copy(attendanceStatus = StaffDetailsState.AttendanceStatus.Loading)
            }

            attendanceRepo.getStaffAttendance(currentStaffId)
                .onSuccess { records ->
                    _uiState.update {
                        it.copy(
                            attendanceList = records,
                            attendanceStatus = StaffDetailsState.AttendanceStatus.Success(records)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            attendanceStatus = StaffDetailsState.AttendanceStatus.Error(
                                error.message ?: "Failed to load attendance records"
                            )
                        )
                    }
                }
        }
    }

}

@Stable
data class StaffDetailsState(
    val staffId: Int = 0,
    val name: String = "",
    val isFaceEmbeddingEnrolled: Boolean = false,
    val isStaffLoading: Boolean = true,
    val attendanceList: List<AttendanceDto> = emptyList(),
    val attendanceStatus: AttendanceStatus = AttendanceStatus.Loading,
    val staffErrorMessage: String? = null
) {
    sealed interface AttendanceStatus {
        data object Idle : AttendanceStatus
        data object Loading : AttendanceStatus
        data class Success(val records: List<AttendanceDto>) : AttendanceStatus
        data class Error(val message: String) : AttendanceStatus
    }
}