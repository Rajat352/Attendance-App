package com.example.attendanceapp.data.repository

import android.media.Image
import com.example.attendanceapp.data.remote.dto.AttendanceDto
import com.example.attendanceapp.data.remote.dto.SubmitAttendanceRes

interface AttendanceRepo {
    suspend fun getStaffAttendance(staffId: Int): Result<AttendanceDto>
    suspend fun recordAttendance(staffId: Int, timestamp: Long, latitude: Double, longitude: Double, selfie: Image): Result<SubmitAttendanceRes>
}