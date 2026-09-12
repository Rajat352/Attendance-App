package com.example.attendanceapp.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.attendanceapp.data.dto.SessionUser
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createSessionUser(user: SessionUser)

    @Query("DELETE FROM session_user")
    suspend fun clearSession()

    @Query("SELECT * FROM session_user LIMIT 1")
    fun getSessionUser(): Flow<SessionUser?>

    @Query("UPDATE session_user SET enrolledFaceEmbedding = :embedding WHERE staffId = :staffId")
    suspend fun upsertEmbedding(staffId: Int, embedding: List<Float>)
}