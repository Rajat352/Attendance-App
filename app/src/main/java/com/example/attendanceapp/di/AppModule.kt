package com.example.attendanceapp.di

import android.content.Context
import androidx.room3.Room
import com.example.attendanceapp.data.AppDatabase
import com.example.attendanceapp.data.location.DefaultLocationClient
import com.example.attendanceapp.data.location.LocationClient
import com.example.attendanceapp.data.remote.api.AttendanceApiService
import com.example.attendanceapp.domain.facenet.FaceRecognitionManager
import com.example.attendanceapp.domain.facenet.FaceRecognitionManagerFaceNetImpl
import com.example.attendanceapp.domain.facenet.Models
import com.example.attendanceapp.domain.mlkit.FaceDetectionManager
import com.example.attendanceapp.domain.mlkit.FaceDetectionManagerMLKitImpl
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val BASE_URL = "http://192.168.1.7:8000/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideAttendanceApiService(): AttendanceApiService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
            .create(AttendanceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideFaceDetectionManager(): FaceDetectionManager {
        return FaceDetectionManagerMLKitImpl()
    }

    @Provides
    @Singleton
    fun provideFaceRecognitionManager(@ApplicationContext context: Context): FaceRecognitionManager {
        return FaceRecognitionManagerFaceNetImpl(context, Models.FACENET)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideLocationClient(
        @ApplicationContext context: Context,
        fusedLocationClient: FusedLocationProviderClient
    ): LocationClient {
        return DefaultLocationClient(context, fusedLocationClient)
    }
}