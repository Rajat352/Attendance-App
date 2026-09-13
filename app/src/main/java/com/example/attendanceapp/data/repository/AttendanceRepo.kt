package com.example.attendanceapp.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.attendanceapp.data.remote.api.AttendanceApiService
import com.example.attendanceapp.data.remote.dto.AttendanceDto
import com.example.attendanceapp.data.remote.dto.SubmitAttendanceRes
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.math.min
import androidx.core.graphics.scale

interface AttendanceRepo {
    suspend fun getStaffAttendance(staffId: Int): Result<List<AttendanceDto>>
    suspend fun recordAttendance(staffId: Int, timestamp: Long, latitude: Double, longitude: Double, selfie: Bitmap): Result<SubmitAttendanceRes>
}

class AttendanceRepoImpl(
    private val attendanceApiService: AttendanceApiService
): AttendanceRepo {

    override suspend fun recordAttendance(
        staffId: Int,
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        selfie: Bitmap
    ): Result<SubmitAttendanceRes> {
        return try {
            val outputStream = ByteArrayOutputStream()
            val scaled = if (selfie.width > 720 || selfie.height > 720) {
                val ratio = min(720f / selfie.width, 720f / selfie.height)
                selfie.scale((selfie.width * ratio).toInt(), (selfie.height * ratio).toInt())
            } else selfie
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()

            val selfieRequestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val selfiePart = MultipartBody.Part.createFormData(
                name = "selfie",
                filename = "selfie_${staffId}_$timestamp.jpg",
                body = selfieRequestBody
            )

            val staffIdBody = staffId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val timestampBody = timestamp.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudeBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudeBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val response = attendanceApiService.recordAttendance(
                staffId = staffIdBody,
                timestamp = timestampBody,
                latitude = latitudeBody,
                longitude = longitudeBody,
                selfie = selfiePart
            )

            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg.ifBlank { "Failed to record attendance" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while submitting attendance record", e)
            Result.failure(e)
        }
    }

    override suspend fun getStaffAttendance(staffId: Int): Result<List<AttendanceDto>> {
        return try {
            val response = attendanceApiService.getStaffAttendance(staffId)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while fetching attendance records from server", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AttendanceRepoImpl"
    }
}