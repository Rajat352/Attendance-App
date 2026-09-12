package com.example.attendanceapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginReq (
    val username: String,
    val password: String
)

@Serializable
data class LoginRes (
    val success: Boolean,
    val name: String? = null,
    val role: String? = null,
    @SerialName("staffId")
    val staffId: Int? = null,
    val message: String? = null
)