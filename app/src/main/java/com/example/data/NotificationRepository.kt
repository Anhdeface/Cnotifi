package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.NotificationReceiver

class NotificationRepository(
    private val context: Context,
    private val dao: NotificationDao
) {
    val activeSchedules = dao.getActiveSchedulesFlow()
    val historySchedules = dao.getHistoryFlow()

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleSingle(
        title: String,
        content: String,
        iconName: String,
        iconColor: String,
        triggerTimeMs: Long
    ) {
        val schedule = NotificationSchedule(
            title = title,
            content = content,
            iconName = iconName,
            iconColor = iconColor,
            triggerTimeMs = triggerTimeMs
        )
        val id = dao.insert(schedule).toInt()
        registerAlarm(schedule.copy(id = id))
    }

    suspend fun scheduleConsecutive(
        title: String,
        content: String,
        iconName: String,
        iconColor: String,
        baseTimeMs: Long,
        count: Int,
        delaySeconds: Int
    ) {
        val groupId = "group_${System.currentTimeMillis()}"
        val titleLines = title.lines().filter { it.isNotBlank() }
        val contentLines = content.lines().filter { it.isNotBlank() }

        val schedules = (1..count).map { index ->
            val triggerTime = baseTimeMs + (index - 1) * delaySeconds * 1000L
            val finalTitle = when {
                titleLines.isNotEmpty() -> titleLines[(index - 1) % titleLines.size]
                else -> title
            }
            val finalTitleWithIndicator = if (titleLines.size <= 1 && count > 1) {
                "$finalTitle - ($index/$count)"
            } else {
                finalTitle
            }

            val finalContent = when {
                contentLines.isNotEmpty() -> contentLines[(index - 1) % contentLines.size]
                else -> content
            }

            NotificationSchedule(
                title = finalTitleWithIndicator,
                content = finalContent,
                iconName = iconName,
                iconColor = iconColor,
                triggerTimeMs = triggerTime,
                sequenceIndex = index,
                sequenceTotal = count,
                delaySeconds = delaySeconds,
                groupId = groupId
            )
        }
        val insertedIds = dao.insertAll(schedules)
        schedules.forEachIndexed { idx, schedule ->
            val id = insertedIds[idx].toInt()
            registerAlarm(schedule.copy(id = id))
        }
    }

    suspend fun cancelSchedule(id: Int) {
        val schedule = dao.getScheduleById(id)
        if (schedule != null) {
            cancelAlarm(schedule.id)
            dao.deleteById(schedule.id)
        }
    }

    suspend fun cancelGroup(groupId: String) {
        val activeInGroup = dao.getActiveByGroupId(groupId)
        activeInGroup.forEach { schedule ->
            cancelAlarm(schedule.id)
        }
        dao.deleteActiveByGroupId(groupId)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun markAsSent(id: Int) {
        val schedule = dao.getScheduleById(id)
        if (schedule != null) {
            dao.update(schedule.copy(isSent = true))
        }
    }

    private fun registerAlarm(schedule: NotificationSchedule) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("schedule_id", schedule.id)
            putExtra("title", schedule.title)
            putExtra("content", schedule.content)
            putExtra("icon_name", schedule.iconName)
            putExtra("icon_color", schedule.iconColor)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d("NotificationRepository", "Successfully scheduled alarm for ${schedule.title} at ${schedule.triggerTimeMs}")
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                schedule.triggerTimeMs,
                pendingIntent
            )
            Log.e("NotificationRepository", "SecurityException: Exact alarm fallback used", e)
        }
    }

    private fun cancelAlarm(id: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("NotificationRepository", "Canceled alarm for ID $id")
        }
    }
}
