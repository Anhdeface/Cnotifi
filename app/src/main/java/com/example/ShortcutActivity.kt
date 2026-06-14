package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.MessageItem
import com.example.data.NotificationSchedule
import com.example.receiver.NotificationReceiver
import androidx.core.content.pm.ShortcutManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShortcutActivity : ComponentActivity() {

    private fun getLoc(vi: String, en: String): String {
        return if (com.example.ui.theme.ThemeManager.currentLanguage == "vi") vi else en
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.theme.ThemeManager.init(applicationContext)

        val prefs = getSharedPreferences("shortcut_prefs", Context.MODE_PRIVATE)
        val activeShortcutVersion = prefs.getLong("active_shortcut_version", 0L)
        val shortcutVersion = intent?.getLongExtra("shortcut_version", 0L) ?: 0L

        // If the shortcut is expired
        if (activeShortcutVersion != 0L && shortcutVersion != activeShortcutVersion) {
            setContent {
                com.example.ui.theme.MyApplicationTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .width(320.dp)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = getLoc("🚨 PHÍM TẮT HẾT HẠN", "🚨 EXPIRED SHORTCUT"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = getLoc(
                                        "Phím tắt này đã hết hạn vì bạn đã tạo một phím tắt (shortcut) mới!\n\nVui lòng gỡ bỏ/xóa phím tắt cũ này trên màn hình chính của thiết bị.",
                                        "This shortcut has expired because you've compiled a newer one!\n\nPlease delete/remove this older shortcut from your device's home screen."
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = {
                                        try {
                                            ShortcutManagerCompat.disableShortcuts(applicationContext, listOf("shortcut_pin"), "Lối tắt đã hết hạn")
                                            ShortcutManagerCompat.removeDynamicShortcuts(applicationContext, listOf("shortcut_pin"))
                                            Toast.makeText(applicationContext, getLoc("🧹 Đã xóa cấu hình & vô hiệu hóa phím tắt hết hạn thành công!", "🧹 Successfully cleared config & disabled expired shortcut!"), Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(getLoc("Xóa Phím Tắt 🗑️", "Delete Shortcut 🗑️"), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            return
        }

        val title = prefs.getString("title", "Nhắc nhở") ?: "Nhắc nhở"
        val iconName = prefs.getString("icon_name", "bell") ?: "bell"
        val iconColorHex = prefs.getString("icon_color", "#FB8C00") ?: "#FB8C00"
        val messagesSerialized = prefs.getString("message_items", "") ?: ""
        val showProgress = prefs.getBoolean("show_popup_progress", true)

        // Simple customized parser to deserialize sequence item list
        val messagesList = deserializeMessageList(messagesSerialized)

        if (messagesList.isEmpty()) {
            Toast.makeText(this, getLoc("⚠️ Vui lòng mở ứng dụng chính để cài đặt tin nhắn trước!", "⚠️ Please open the main app to configure messages first!"), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!showProgress) {
            val serviceIntent = Intent(this, NotificationSequenceService::class.java).apply {
                putExtra("title", title)
                putExtra("icon_name", iconName)
                putExtra("icon_color", iconColorHex)
                putExtra("messages_serialized", messagesSerialized)
            }
            startService(serviceIntent)
            finish()
            return
        }

        setContent {
            var currentMsgIndex by remember { mutableStateOf(0) }
            var isSendingCompleted by remember { mutableStateOf(false) }
            var currentMessageText by remember { mutableStateOf("") }
            val currentDelayLeft = remember { mutableStateOf(0) }

            // Trigger actual dispatch process in a stable, foreground-backed lifecycle scope
            LaunchedEffect(Unit) {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.notificationDao
                val groupId = "shortcut_${System.currentTimeMillis()}"

                messagesList.forEachIndexed { idx, message ->
                    currentMsgIndex = idx + 1
                    currentMessageText = message.text

                    // Insert history sequence item tracking inside database
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

                    // Instantly notify current message
                    showDynamicNotification(
                        applicationContext,
                        title = title,
                        content = message.text,
                        iconName = iconName,
                        iconColorHex = iconColorHex
                    )

                    // Custom configured Delay option
                    if (idx < messagesList.size - 1) {
                        for (sec in message.delaySeconds downTo 1) {
                            currentDelayLeft.value = sec
                            delay(1000L)
                        }
                    }
                }

                currentMessageText = "Đã hoàn thành chuỗi tin nhắn!"
                isSendingCompleted = true
                delay(800L) // Beautiful short success showcase
                finish()
            }

            // Translucent glass spotlight container over native dim
            com.example.ui.theme.MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.01f)),
                    contentAlignment = Alignment.Center
                ) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isSendingCompleted) {
                            // High priority neon spinner
                            CircularProgressIndicator(
                                strokeWidth = 5.dp,
                                color = Color(android.graphics.Color.parseColor(iconColorHex)),
                                modifier = Modifier.size(54.dp)
                            )
                        } else {
                            // Pulsing success indicator
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Thành công",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Text(
                            text = if (!isSendingCompleted) "🚀 ĐANG GỬI TIN NHẮN..." else "✨ THÀNH CÔNG!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Tiến độ: $currentMsgIndex / ${messagesList.size} tin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Message Text Box with premium custom apple emoji font
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ) {
                            Text(
                                text = currentMessageText,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isSendingCompleted && currentMsgIndex < messagesList.size) {
                            Text(
                                text = "Đang chờ gửi tin tiếp theo sau: ${currentDelayLeft.value}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
          }
        }
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
