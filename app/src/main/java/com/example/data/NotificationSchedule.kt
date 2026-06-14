package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_schedules")
data class NotificationSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val iconName: String,
    val iconColor: String,
    val triggerTimeMs: Long,
    val isSent: Boolean = false,
    val sequenceIndex: Int = 1,
    val sequenceTotal: Int = 1,
    val delaySeconds: Int = 0,
    val groupId: String? = null
)
