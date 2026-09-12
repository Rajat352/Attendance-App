package com.example.attendanceapp.data.remote.api

import com.example.attendanceapp.data.remote.dto.ApiRes
import com.example.attendanceapp.data.remote.dto.AttendanceDto
import com.example.attendanceapp.data.remote.dto.CreateStaffReq
import com.example.attendanceapp.data.remote.dto.EmbeddingDto
import com.example.attendanceapp.data.remote.dto.LoginReq
import com.example.attendanceapp.data.remote.dto.LoginRes
import com.example.attendanceapp.data.remote.dto.StaffDto
import com.example.attendanceapp.data.remote.dto.SubmitAttendanceRes
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface AttendanceApiService {

    // auth endpoint
    @POST("login")
    suspend fun login(
        @Body request: LoginReq
    ): Response<LoginRes>

    // admin endpoints
    @GET("staff")
    suspend fun listStaff(): Response<List<StaffDto>>

    @POST("staff")
    suspend fun createStaff(
        @Body request: CreateStaffReq
    ): Response<ApiRes>

    @PUT("staff/{staff_Id}/embedding")
    suspend fun updateStaffEmbedding(
        @Path("staff_Id") staffId: Int,
        @Body request: EmbeddingDto
    ): Response<ApiRes>

    @GET("staff/{staff_Id}/attendance")
    suspend fun getStaffAttendance(
        @Path("staff_Id") staffId: Int
    ): Response<List<AttendanceDto>>

    // staff endpoints
    @Multipart
    @POST("attendance")
    suspend fun recordAttendance(
        @Part("staffId") staffId: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part selfie: MultipartBody.Part
    ): Response<SubmitAttendanceRes>

    @GET("staff/{staff_id}/embedding")
    suspend fun getStaffEmbedding(
        @Path("staff_id") staffId: Int
    ): Response<EmbeddingDto>
}