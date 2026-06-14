package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scheduleId = intent.getIntExtra("schedule_id", -1)
        val title = intent.getStringExtra("title") ?: "Custom Notification"
        val content = intent.getStringExtra("content") ?: "Your scheduled reminder"
        val iconName = intent.getStringExtra("icon_name") ?: "bell"
        val iconColor = intent.getStringExtra("icon_color") ?: "#FF5722"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (scheduleId != -1) {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.notificationDao
                    val schedule = dao.getScheduleById(scheduleId)
                    if (schedule != null) {
                        dao.update(schedule.copy(isSent = true))
                    }
                }

                showNotification(context, scheduleId, title, content, iconName, iconColor)
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error processing notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        content: String,
        iconName: String,
        iconColor: String
    ) {
        val channelId = "custom_notifications_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Custom Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channels for user custom scheduled notifications"
                enableLights(true)
                lightColor = android.graphics.Color.parseColor(iconColor)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val smallIconResId = getIconResId(iconName)
        val largeIconBitmap = createNotificationLargeIcon(context, iconName, iconColor)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconResId)
            .setContentTitle(title)
            .setContentText(content)
            .setColor(android.graphics.Color.parseColor(iconColor))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        notificationManager.notify(if (id != -1) id else System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        fun getIconResId(iconName: String): Int {
            return when (iconName) {
                "star" -> R.drawable.ic_notify_star
                "heart" -> R.drawable.ic_notify_heart
                "alert" -> R.drawable.ic_notify_alert
                "chat" -> R.drawable.ic_notify_chat
                "gift" -> R.drawable.ic_notify_gift
                "coffee" -> R.drawable.ic_notify_coffee
                "check" -> R.drawable.ic_notify_check
                else -> R.drawable.ic_notify_bell
            }
        }

        fun createNotificationLargeIcon(context: Context, iconName: String, colorHex: String): Bitmap? {
            return try {
                if (iconName.startsWith("custom_icon_")) {
                    val file = java.io.File(context.filesDir, iconName)
                    if (file.exists()) {
                        return android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    }
                }
                val size = 192
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor(colorHex)
                    style = Paint.Style.FILL
                }

                canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

                val resId = getIconResId(iconName)
                val drawable = ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val wrapDrawable = DrawableCompat.wrap(drawable).mutate()
                    DrawableCompat.setTint(wrapDrawable, android.graphics.Color.WHITE)
                    val padding = (size * 0.22f).toInt()
                    wrapDrawable.setBounds(padding, padding, size - padding, size - padding)
                    wrapDrawable.draw(canvas)
                }
                bitmap
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error creating large icon", e)
                null
            }
        }
    }
}
