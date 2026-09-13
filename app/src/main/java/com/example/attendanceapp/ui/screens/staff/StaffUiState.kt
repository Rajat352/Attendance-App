package com.example.attendanceapp.ui.screens.staff

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import com.example.attendanceapp.data.dto.SessionUser
import com.example.attendanceapp.data.remote.dto.SubmitAttendanceRes

@Stable
data class StaffUiState(
    val user: SessionUser? = null,
    val isLoadingUser: Boolean = true,
    val isFetchingEmbedding: Boolean = false,
    val isCameraActive: Boolean = false,
    val attendanceStage: AttendanceStage = AttendanceStage.Idle
)

@Stable
sealed interface AttendanceStage {
    data object Idle : AttendanceStage
    data object ProcessingSelfie : AttendanceStage
    data object VerifyingFace : AttendanceStage
    data object FetchingLocation : AttendanceStage
    data object SubmittingAttendance : AttendanceStage
    data class Success(
        val res: SubmitAttendanceRes,
        val timestamp: Long,
        val latitude: Double,
        val longitude: Double,
        val selfie: Bitmap
    ) : AttendanceStage
    data class Failure(
        val reason: FailureReason,
        val message: String,
        val similarityScore: Float? = null
    ) : AttendanceStage
}

enum class FailureReason {
    NO_FACE,
    MULTIPLE_FACES,
    FACE_MISMATCH,
    NOT_ENROLLED,
    LOCATION_ERROR,
    SERVER_ERROR
}
