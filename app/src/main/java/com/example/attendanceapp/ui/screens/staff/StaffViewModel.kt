package com.example.attendanceapp.ui.screens.staff

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.dao.SessionUserDao
import com.example.attendanceapp.data.location.LocationClient
import com.example.attendanceapp.data.repository.AttendanceRepo
import com.example.attendanceapp.data.repository.AuthRepo
import com.example.attendanceapp.data.repository.StaffRepo
import com.example.attendanceapp.domain.usecase.ProcessFaceUseCase
import com.example.attendanceapp.domain.usecase.VerifyFaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val authRepo: AuthRepo,
    private val staffRepo: StaffRepo,
    private val attendanceRepo: AttendanceRepo,
    private val processFaceUseCase: ProcessFaceUseCase,
    private val verifyFaceUseCase: VerifyFaceUseCase,
    private val locationClient: LocationClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    init {
        observeSessionUser()
    }

    private fun observeSessionUser() {
        viewModelScope.launch {
            authRepo.getSessionUser().collect { sessionUser ->
                _uiState.update {
                    it.copy(
                        user = sessionUser,
                        isLoadingUser = false
                    )
                }

                if (sessionUser?.staffId != null) {
                    // If embedding not p[resent, fetch from server
                    if (sessionUser.enrolledFaceEmbedding == null) {
                        fetchAndCacheEmbedding(sessionUser.staffId)
                    }
                }
            }
        }
    }

    private fun fetchAndCacheEmbedding(staffId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingEmbedding = true) }
            staffRepo.getStaffEmbedding(staffId)
                .onSuccess {
                    _uiState.update { it.copy(isFetchingEmbedding = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isFetchingEmbedding = false) }
                }
        }
    }

    fun onMarkAttendanceClicked() {
        val user = _uiState.value.user
        if (user == null || user.staffId == null) {
            _uiState.update {
                it.copy(
                    attendanceStage = AttendanceStage.Failure(
                        reason = FailureReason.NOT_ENROLLED,
                        message = "User session not found. Please log in again."
                    )
                )
            }
            return
        }

        if (user.enrolledFaceEmbedding == null) {
            // Check if we can fetch it now
            viewModelScope.launch {
                _uiState.update { it.copy(isFetchingEmbedding = true) }
                staffRepo.getStaffEmbedding(user.staffId)
                    .onSuccess { dto ->
                        _uiState.update { it.copy(isFetchingEmbedding = false) }
                        if (dto.embedding.isNotEmpty()) {
                            _uiState.update { it.copy(isCameraActive = true, attendanceStage = AttendanceStage.Idle) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    attendanceStage = AttendanceStage.Failure(
                                        reason = FailureReason.NOT_ENROLLED,
                                        message = "Your face is not enrolled yet. Please contact your admin to enroll your face first."
                                    )
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isFetchingEmbedding = false,
                                attendanceStage = AttendanceStage.Failure(
                                    reason = FailureReason.NOT_ENROLLED,
                                    message = "Face not enrolled. Admin must enroll your face before marking attendance."
                                )
                            )
                        }
                    }
            }
            return
        }

        _uiState.update {
            it.copy(
                isCameraActive = true,
                attendanceStage = AttendanceStage.Idle
            )
        }
    }

    fun onSelfieCaptured(bitmap: Bitmap) {
        val user = _uiState.value.user
        val staffId = user?.staffId
        val enrolledEmbedding = user?.enrolledFaceEmbedding

        if (user == null || staffId == null || enrolledEmbedding == null) {
            _uiState.update {
                it.copy(
                    isCameraActive = false,
                    attendanceStage = AttendanceStage.Failure(
                        reason = FailureReason.NOT_ENROLLED,
                        message = "Enrolled face data missing. Please contact your administrator."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            // Processing Selfie
            _uiState.update {
                it.copy(
                    isCameraActive = false,
                    attendanceStage = AttendanceStage.ProcessingSelfie
                )
            }

            val faceProcessResult = processFaceUseCase(bitmap)
            if (faceProcessResult.isFailure) {
                val error = faceProcessResult.exceptionOrNull()
                val reason = when {
                    error?.message?.contains("Multiple") == true -> FailureReason.MULTIPLE_FACES
                    else -> FailureReason.NO_FACE
                }
                _uiState.update {
                    it.copy(
                        attendanceStage = AttendanceStage.Failure(
                            reason = reason,
                            message = error?.message ?: "Face detection failed. Please take a clear selfie."
                        )
                    )
                }
                return@launch
            }

            val processedFace = faceProcessResult.getOrThrow()

            // Verify Face Match
            _uiState.update { it.copy(attendanceStage = AttendanceStage.VerifyingFace) }

            val verification = verifyFaceUseCase(
                capturedEmbedding = processedFace.embedding,
                enrolledEmbedding = enrolledEmbedding
            )

            if (!verification.isMatch) {
                _uiState.update {
                    it.copy(
                        attendanceStage = AttendanceStage.Failure(
                            reason = FailureReason.FACE_MISMATCH,
                            message = "Face does not match the enrolled profile. Attendance will not be recorded.",
                            similarityScore = verification.similarityScore
                        )
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(attendanceStage = AttendanceStage.FetchingLocation) }

            val locationResult = locationClient.getCurrentLocation()
            val coordinates = locationResult.getOrElse {
                // Fallback default coordinates if location retrieval fails gracefully
                null
            }

            val latitude = coordinates?.latitude ?: 0.0
            val longitude = coordinates?.longitude ?: 0.0

            // Submit Attendance to Server
            _uiState.update { it.copy(attendanceStage = AttendanceStage.SubmittingAttendance) }

            val timestamp = System.currentTimeMillis()
            attendanceRepo.recordAttendance(
                staffId = staffId,
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude,
                selfie = processedFace.fullBitmap
            ).onSuccess { res ->
                _uiState.update {
                    it.copy(
                        attendanceStage = AttendanceStage.Success(
                            res = res,
                            timestamp = timestamp,
                            latitude = latitude,
                            longitude = longitude,
                            selfie = processedFace.fullBitmap
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        attendanceStage = AttendanceStage.Failure(
                            reason = FailureReason.SERVER_ERROR,
                            message = error.message ?: "Failed to record attendance on the server. Please try again."
                        )
                    )
                }
            }
        }
    }

    fun dismissCamera() {
        _uiState.update { it.copy(isCameraActive = false) }
    }

    fun dismissStage() {
        _uiState.update { it.copy(attendanceStage = AttendanceStage.Idle) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepo.logout()
            onLoggedOut()
        }
    }
}
