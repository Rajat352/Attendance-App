package com.example.attendanceapp.data.repository

import android.util.Log
import com.example.attendanceapp.data.dao.SessionUserDao
import com.example.attendanceapp.data.dto.SessionUser
import com.example.attendanceapp.data.remote.api.AttendanceApiService
import com.example.attendanceapp.data.remote.dto.LoginReq
import com.example.attendanceapp.data.remote.dto.LoginRes

interface AuthRepo {
    suspend fun login(username: String, password: String): Result<LoginRes>
}

class AuthRepoImpl(
    private val attendanceApiService: AttendanceApiService,
    private val sessionUserDao: SessionUserDao
): AuthRepo {

    // api call to backend + persistence in room db
    override suspend fun login(username: String, password: String): Result<LoginRes> {
        return try {
            val res = attendanceApiService.login(LoginReq(username, password))
            if (res.isSuccessful) {
                val body = res.body()

                if(body != null && body.success && !body.role.isNullOrEmpty()) {
                    sessionUserDao.clearSession()
                    sessionUserDao.createSessionUser(
                        SessionUser(
                            staffId = body.staffId,
                            name = body.name ?: "unknown",
                            role = body.role
                        )
                    )
                    Result.success(body)
                } else {
                    Result.failure(Exception("Login failed, ${res.message()}"))
                }
            } else {
                Result.failure(Exception("Login failed, ${res.message()}"))
            }
        }catch (e: Exception) {
            Log.e(TAG, "An error occurred while logging in", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AuthRepoImpl"
    }

}