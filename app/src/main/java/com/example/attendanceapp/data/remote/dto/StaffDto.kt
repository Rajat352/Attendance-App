package com.example.attendanceapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StaffDto(
    @SerialName("staffId")
    val staffId: Long,
    val name: String,
    val enrolled: Boolean
)

@Serializable
data class CreateStaffReq(
    @SerialName("staffId")
    val staffId: Int,
    val name: String
)

@Serializable
data class ApiRes(
    val success: Boolean,
    val message: String? = null,
    @SerialName("staffId")
    val staffId: Int? = null,
    val username: String? = null
)

@Serializable
data class EmbeddingDto(
    val embedding: List<Float>
)