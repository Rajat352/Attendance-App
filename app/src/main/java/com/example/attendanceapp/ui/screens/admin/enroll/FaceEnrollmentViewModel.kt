package com.example.attendanceapp.ui.screens.admin.enroll

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.data.repository.StaffRepo
import com.example.attendanceapp.domain.usecase.ProcessFaceUseCase
import com.example.attendanceapp.domain.usecase.ProcessedFace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class FaceEnrollmentUiState(
    val isProcessing: Boolean = false,
    val isUploading: Boolean = false,
    val processedFace: ProcessedFace? = null,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class FaceEnrollmentViewModel @Inject constructor(
    private val processFaceUseCase: ProcessFaceUseCase,
    private val staffRepo: StaffRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceEnrollmentUiState())
    val uiState: StateFlow<FaceEnrollmentUiState> = _uiState.asStateFlow()

    fun processCapturedImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    errorMessage = null
                )
            }

            processFaceUseCase(bitmap)
                .onSuccess { processed ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processedFace = processed,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processedFace = null,
                            errorMessage = error.message ?: "Face detection failed. Please try again."
                        )
                    }
                }
        }
    }

    fun confirmEnrollment(staffId: Int) {
        val processed = _uiState.value.processedFace ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    errorMessage = null
                )
            }

            staffRepo.updateStaffEmbedding(staffId, processed.embedding)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            isSuccess = true,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = error.message ?: "Failed to enroll face. Please try again."
                        )
                    }
                }
        }
    }

    fun resetCapture() {
        _uiState.update {
            it.copy(
                isProcessing = false,
                isUploading = false,
                processedFace = null,
                errorMessage = null,
                isSuccess = false
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
