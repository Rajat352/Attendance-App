package com.example.attendanceapp.data.dto

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "session_user"
)
data class SessionUser(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val staffId: Int? = null,
    val name: String,
    val role: String,
    val enrolledFaceEmbedding: List<Float>? = null
)
