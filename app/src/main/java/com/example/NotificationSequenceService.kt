package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.data.AppDatabase
import com.example.data.MessageItem
import com.example.data.NotificationSchedule
import com.example.receiver.NotificationReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationSequenceService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "Nhắc nhở"
        val iconName = intent?.getStringExtra("icon_name") ?: "bell"
        val iconColorHex = intent?.getStringExtra("icon_color") ?: "#FB8C00"
        val messagesSerialized = intent?.getStringExtra("messages_serialized") ?: ""

        val messagesList = deserializeMessageList(messagesSerialized)

        if (messagesList.isNotEmpty()) {
            serviceScope.launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val dao = db.notificationDao
                    val groupId = "shortcut_${System.currentTimeMillis()}"

                    messagesList.forEachIndexed { idx, message ->
                        val schedule = NotificationSchedule(
                            title = title,
                            content = message.text,
                            iconName = iconName,
                            iconColor = iconColorHex,
                            triggerTimeMs = System.currentTimeMillis(),
                            isSent = true,
                            sequenceIndex = idx + 1,
                            sequenceTotal = messagesList.size,
                            delaySeconds = message.delaySeconds,
                            groupId = groupId
                        )
                        dao.insert(schedule)

                        showDynamicNotification(
                            applicationContext,
                            title = title,
                            content = message.text,
                            iconName = iconName,
                            iconColorHex = iconColorHex
                        )

                        if (idx < messagesList.size - 1) {
                            delay(message.delaySeconds * 1000L)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    stopSelf(startId)
                }
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun deserializeMessageList(data: String?): List<MessageItem> {
        if (data.isNullOrBlank()) {
            return emptyList()
        }
        return data.split("###").mapNotNull { part ->
            val subparts = part.split("||")
            if (subparts.size >= 2) {
                val text = subparts[0]
                val delay = subparts[1].toIntOrNull() ?: 2
                MessageItem(text = text, delaySeconds = delay.coerceIn(1, 3))
            } else {
                null
            }
        }
    }

    private fun showDynamicNotification(
        context: Context,
        title: String,
        content: String,
        iconName: String,
        iconColorHex: String
    ) {
        val channelId = "custom_notifications_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Custom Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channels for user custom scheduled notifications"
                enableLights(true)
                lightColor = android.graphics.Color.parseColor(iconColorHex)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val smallIconResId = NotificationReceiver.getIconResId(iconName)
        val largeIconBitmap = NotificationReceiver.createNotificationLargeIcon(context, iconName, iconColorHex)

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconResId)
            .setContentTitle(title)
            .setContentText(content)
            .setColor(android.graphics.Color.parseColor(iconColorHex))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
