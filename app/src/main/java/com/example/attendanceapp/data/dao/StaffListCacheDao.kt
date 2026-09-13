package com.example.attendanceapp.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.attendanceapp.data.dto.StaffListCache
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffListCacheDao {
    @Query("SELECT * FROM staff_list_cache ORDER BY staffId ASC")
    fun getAllStaff(): Flow<List<StaffListCache>>

    @Query("SELECT * FROM staff_list_cache WHERE staffId = :staffId LIMIT 1")
    fun getStaffById(staffId: Int): Flow<StaffListCache?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffList(staff: List<StaffListCache>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffListCache)

    @Query("DELETE FROM staff_list_cache")
    suspend fun clearStaff()
}