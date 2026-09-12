package com.example.attendanceapp.data

import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.attendanceapp.data.dao.SessionUserDao
import com.example.attendanceapp.data.dao.StaffListCacheDao
import com.example.attendanceapp.data.dto.SessionUser
import com.example.attendanceapp.data.dto.StaffListCache
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @ColumnTypeConverter
    fun fromEmbeddingList(embedding: List<Float>?): String? {
        return embedding?.let { json.encodeToString(it) }
    }

    @ColumnTypeConverter
    fun toEmbeddingList(data: String?): List<Float>? {
        return data?.let { json.decodeFromString<List<Float>>(it) }
    }
}

@Database(
    entities = [SessionUser::class, StaffListCache::class],
    version = 1,
    exportSchema = false
)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {

    abstract fun sessionUserDao(): SessionUserDao
    abstract fun staffListCacheDao(): StaffListCacheDao

}