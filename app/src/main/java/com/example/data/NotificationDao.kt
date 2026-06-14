package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_schedules WHERE isSent = 0 ORDER BY triggerTimeMs ASC")
    fun getActiveSchedulesFlow(): Flow<List<NotificationSchedule>>

    @Query("SELECT * FROM notification_schedules WHERE isSent = 1 ORDER BY triggerTimeMs DESC")
    fun getHistoryFlow(): Flow<List<NotificationSchedule>>

    @Query("SELECT * FROM notification_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Int): NotificationSchedule?

    @Query("SELECT * FROM notification_schedules WHERE groupId = :groupId AND isSent = 0")
    suspend fun getActiveByGroupId(groupId: String): List<NotificationSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: NotificationSchedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<NotificationSchedule>): List<Long>

    @Update
    suspend fun update(schedule: NotificationSchedule)

    @Query("DELETE FROM notification_schedules WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM notification_schedules WHERE groupId = :groupId AND isSent = 0")
    suspend fun deleteActiveByGroupId(groupId: String)

    @Query("DELETE FROM notification_schedules WHERE isSent = 1")
    suspend fun clearHistory()
}
