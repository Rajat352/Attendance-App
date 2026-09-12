package com.example.attendanceapp.data.dto

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "staff_list_cache"
)
data class StaffListCache(
    @PrimaryKey(autoGenerate = false)
    val staffId: Int,
    val name: String,
    val isFaceEmbeddingEnrolled: Boolean = false
)
