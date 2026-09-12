package com.example.attendanceapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceDto(
    val id: Int,
    @SerialName("staffId")
    val staffId: Int,
    val timestamp: Long,
    @SerialName("selfieUrl")
    val selfieUrl: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class SubmitAttendanceRes(
    val success: Boolean,
    val message: String? = null,
    val id: Int? = null,
    @SerialName("selfieUrl")
    val selfieUrl: String? = null
)