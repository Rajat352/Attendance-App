package com.example.attendanceapp.di

import com.example.attendanceapp.data.AppDatabase
import com.example.attendanceapp.data.dao.SessionUserDao
import com.example.attendanceapp.data.remote.api.AttendanceApiService
import com.example.attendanceapp.data.repository.AuthRepo
import com.example.attendanceapp.data.repository.AuthRepoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSessionUserDao(db: AppDatabase): SessionUserDao {
        return db.sessionUserDao()
    }

    @Provides
    @Singleton
    fun provideAuthRepo(
        attendanceApiService: AttendanceApiService,
        sessionUserDao: SessionUserDao
    ): AuthRepo {
        return AuthRepoImpl(attendanceApiService, sessionUserDao)
    }
}