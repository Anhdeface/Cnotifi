package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.NotificationRepository
import com.example.data.NotificationSchedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = NotificationRepository(application, database.notificationDao)

    val activeSchedules: StateFlow<List<NotificationSchedule>> = repository.activeSchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val historySchedules: StateFlow<List<NotificationSchedule>> = repository.historySchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun scheduleNotification(
        title: String,
        content: String,
        iconName: String,
        iconColor: String,
        useQuickTimer: Boolean,
        offsetSeconds: Int,
        selectedCalendar: Calendar,
        isConsecutive: Boolean,
        consecutiveCount: Int,
        consecutiveDelaySeconds: Int
    ) {
        viewModelScope.launch {
            val triggerTimeMs = if (useQuickTimer) {
                System.currentTimeMillis() + offsetSeconds * 1000L
            } else {
                selectedCalendar.timeInMillis
            }

            if (isConsecutive) {
                repository.scheduleConsecutive(
                    title = title,
                    content = content,
                    iconName = iconName,
                    iconColor = iconColor,
                    baseTimeMs = triggerTimeMs,
                    count = consecutiveCount,
                    delaySeconds = consecutiveDelaySeconds
                )
            } else {
                repository.scheduleSingle(
                    title = title,
                    content = content,
                    iconName = iconName,
                    iconColor = iconColor,
                    triggerTimeMs = triggerTimeMs
                )
            }
        }
    }

    fun cancelSchedule(id: Int) {
        viewModelScope.launch {
            repository.cancelSchedule(id)
        }
    }

    fun cancelGroup(groupId: String) {
        viewModelScope.launch {
            repository.cancelGroup(groupId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
