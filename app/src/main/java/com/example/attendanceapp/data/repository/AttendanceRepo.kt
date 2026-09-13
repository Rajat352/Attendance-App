package com.example.attendanceapp.data.repository

import android.media.Image
import android.util.Log
import com.example.attendanceapp.data.remote.api.AttendanceApiService
import com.example.attendanceapp.data.remote.dto.AttendanceDto
import com.example.attendanceapp.data.remote.dto.SubmitAttendanceRes

interface AttendanceRepo {
    suspend fun getStaffAttendance(staffId: Int): Result<List<AttendanceDto>>
    suspend fun recordAttendance(staffId: Int, timestamp: Long, latitude: Double, longitude: Double, selfie: Image): Result<SubmitAttendanceRes>
}

class AttendanceRepoImpl(
    private val attendanceApiService: AttendanceApiService
): AttendanceRepo {
    override suspend fun recordAttendance(
        staffId: Int,
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        selfie: Image
    ): Result<SubmitAttendanceRes> {
        TODO("Not yet implemented")
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