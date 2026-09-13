package com.example.attendanceapp.data.repository

import android.util.Log
import com.example.attendanceapp.data.dao.StaffListCacheDao
import com.example.attendanceapp.data.dto.StaffListCache
import com.example.attendanceapp.data.remote.api.AttendanceApiService
import com.example.attendanceapp.data.remote.dto.ApiRes
import com.example.attendanceapp.data.remote.dto.CreateStaffReq
import com.example.attendanceapp.data.remote.dto.EmbeddingDto
import com.example.attendanceapp.data.remote.dto.StaffDto
import kotlinx.coroutines.flow.Flow

// Offline first staff list implementation
interface StaffRepo {
    val staffList: Flow<List<StaffListCache>>
    suspend fun refreshStaffList(): Result<Unit>
    suspend fun addStaff(staffId: Int, name: String): Result<Unit>
    suspend fun updateStaffEmbedding(staffId: Int, embedding: List<Float>): Result<ApiRes>
    suspend fun getStaffEmbedding(staffId: Int): Result<EmbeddingDto>
    fun getStaffById(staffId: Int): Flow<StaffListCache?>
}

class StaffRepoImpl(
    private val attendanceApiService: AttendanceApiService,
    private val staffListCacheDao: StaffListCacheDao
): StaffRepo {

    // Room populates UI and not api
    override val staffList: Flow<List<StaffListCache>> = staffListCacheDao.getAllStaff()

    override fun getStaffById(staffId: Int): Flow<StaffListCache?> = staffListCacheDao.getStaffById(staffId)

    override suspend fun refreshStaffList(): Result<Unit> {
        return try {
            val res = attendanceApiService.listStaff()
            if (res.isSuccessful && res.body() != null) {
                val remoteStaff = res.body()!!

                val staffList = remoteStaff.map { dto ->
                    StaffListCache(
                        staffId = dto.staffId.toInt(),
                        name = dto.name,
                        isFaceEmbeddingEnrolled = dto.enrolled
                    )
                }

                // Save to room first
                staffListCacheDao.insertStaffList(staffList)
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.message()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while getting staff list", e)
            Result.failure(e)
        }
    }

    override suspend fun getStaffEmbedding(staffId: Int): Result<EmbeddingDto> {
        return try {
            val response = attendanceApiService.getStaffEmbedding(staffId)

            val body = response.body()
            if (response.isSuccessful && body != null) {
                val embedding = EmbeddingDto(embedding = body.embedding)

                Result.success(embedding)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while getting staff embedding from server")
            Result.failure(e)
        }
    }

    override suspend fun addStaff(staffId: Int, name: String): Result<Unit> {
        return try {
            val response = attendanceApiService.createStaff(CreateStaffReq(staffId, name))
            if (response.isSuccessful) {
                // Immediately save to Room
                staffListCacheDao.insertStaff(
                    StaffListCache(
                        staffId = staffId,
                        name = name,
                        isFaceEmbeddingEnrolled = false
                    )
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while creating staff",e)
            Result.failure(e)
        }
    }

    override suspend fun updateStaffEmbedding(
        staffId: Int,
        embedding: List<Float>
    ): Result<ApiRes> {
        return try {
            val response = attendanceApiService.updateStaffEmbedding(staffId, EmbeddingDto(embedding))

            val body = response.body()
            if(response.isSuccessful && body != null && body.success) {
                staffListCacheDao.updateFaceEmbeddingEnrollmentStatus(staffId, true)
                Result.success(body)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while updating staff embedding")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "StaffRepoImpl"
    }
}