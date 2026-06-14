package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import com.example.data.MessageItem
import com.example.data.NotificationSchedule
import com.example.ShortcutActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.receiver.NotificationReceiver
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun getShimmerBrush(
    baseColor: Color,
    shineColor: Color
): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    return Brush.linearGradient(
        colorStops = arrayOf(
            0f to baseColor,
            (progress - 0.25f).coerceIn(0f, 1f) to baseColor,
            progress.coerceIn(0f, 1f) to shineColor,
            (progress + 0.25f).coerceIn(0f, 1f) to baseColor,
            1f to baseColor
        ),
        start = Offset(0f, 0f),
        end = Offset(400f, 200f)
    )
}

fun getLoc(vi: String, en: String): String {
    return if (com.example.ui.theme.ThemeManager.currentLanguage == "vi") vi else en
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: NotificationViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val activeSchedules by viewModel.activeSchedules.collectAsStateWithLifecycle()
    val historySchedules by viewModel.historySchedules.collectAsStateWithLifecycle()

    // Notification Permission Handling for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, getLoc("Đã cấp quyền thông báo thành công!", "Notification permission successfully granted!"), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, getLoc("Ứng dụng cần quyền thông báo để hoạt động đúng cách.", "The app requires notification permissions to function correctly."), Toast.LENGTH_LONG).show()
        }
    }

    // Auto-request permission on mount if not granted
    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showSplash by remember { mutableStateOf(true) }
    val fullAppName = "Cnotifi"
    var typedAppName by remember { mutableStateOf("") }
    val roundedBoldFont = remember { FontFamily(android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.BOLD)) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        fullAppName.forEachIndexed { index, _ ->
            typedAppName = fullAppName.substring(0, index + 1)
            kotlinx.coroutines.delay(100L)
        }
        kotlinx.coroutines.delay(350L)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Styled circle icon matching HTML spec header template: "w-10 h-10 rounded-full bg-[#6750A4] flex items-center justify-center text-white"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "App Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Cnotifi",
                                fontWeight = FontWeight.Bold,
                                fontFamily = roundedBoldFont,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    androidx.compose.ui.graphics.RectangleShape
                )
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Tạo mới") },
                    label = { Text(getLoc("Tạo Mới", "Create")) },
                    modifier = Modifier.testTag("tab_create")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Giao diện") },
                    label = { Text(getLoc("Giao Diện", "Theme")) },
                    modifier = Modifier.testTag("tab_theme")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeSchedules.isNotEmpty()) {
                                    Badge { Text(activeSchedules.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Lịch sử")
                        }
                    },
                    label = { Text(getLoc("Lịch Sử", "History")) },
                    modifier = Modifier.testTag("tab_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Phím Tắt") },
                    label = { Text(getLoc("Phím Tắt", "Shortcuts")) },
                    modifier = Modifier.testTag("tab_shortcut")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner Callout
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = getLoc("Thiếu quyền gửi thông báo", "Notification Permission Missing"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = getLoc("Vui lòng cho phép ứng dụng gửi thông báo để bạn có thể nhận được lời nhắc đúng thời gian đã đặt.", "Please allow the application to send notifications so you can receive your reminders on time."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(getLoc("Cấp quyền ngay", "Grant Now"))
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> CreateTab(viewModel)
                    1 -> ThemeSetupTab(viewModel)
                    2 -> HistoryTab(activeSchedules, historySchedules, viewModel)
                    3 -> ShortcutTab(viewModel)
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Logo Banner
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .size(110.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Cnotifi Logo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                // App Title
                Text(
                    text = typedAppName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = roundedBoldFont,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Spacer to bridge center icon & bottom credentials
                Spacer(modifier = Modifier.height(24.dp))

                val shimmerBrushText = getShimmerBrush(
                    baseColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    shineColor = MaterialTheme.colorScheme.primary
                )

                // Custom Shimmered Title Labels for Author & Project
                Text(
                    text = "Author : Xounzii",
                    style = MaterialTheme.typography.titleMedium.copy(brush = shimmerBrushText),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Text(
                    text = "github.com/Anhdeface/Cnotifi",
                    style = MaterialTheme.typography.bodyMedium.copy(brush = shimmerBrushText),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTab(viewModel: NotificationViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("shortcut_prefs", Context.MODE_PRIVATE) }

    fun serializeMessageList(list: List<MessageItem>): String {
        return list.joinToString("###") { item ->
            val safeText = item.text.replace("||", "").replace("###", "")
            "$safeText||${item.delaySeconds}"
        }
    }

    fun deserializeMessageList(data: String?): List<MessageItem> {
        if (data.isNullOrBlank()) {
            return listOf(
                MessageItem(text = "" , delaySeconds = 2),
                MessageItem(text = "" , delaySeconds = 2)
            )
        }
        return data.split("###").map { part ->
            val subparts = part.split("||")
            val text = subparts.getOrNull(0) ?: ""
            val delay = subparts.getOrNull(1)?.toIntOrNull() ?: 2
            MessageItem(text = text, delaySeconds = delay.coerceIn(1, 3))
        }
    }

    // Load initial values from SharedPreferences
    val initialTitle = prefs.getString("title", "") ?: ""
    var title by remember { mutableStateOf(initialTitle) }

    val initialMsgSerialized = prefs.getString("message_items", "") ?: ""
    var messageItems by remember { mutableStateOf(deserializeMessageList(initialMsgSerialized)) }

    // Icon & Color lists
    val availableIcons = remember {
        listOf(
            "bell" to "Chuông",
            "star" to "Sao",
            "heart" to "Yêu thích",
            "alert" to "Cảnh báo",
            "chat" to "Trò chuyện",
            "gift" to "Quà tặng",
            "coffee" to "Cốc nước",
            "check" to "Hoàn thành",
            "mail" to "Thư",
            "music" to "Nhạc",
            "home" to "Nhà",
            "settings" to "Cài đặt",
            "phone" to "Điện thoại"
        )
    }

    val colors = remember {
        listOf(
            "#E53935" to Color(0xFFE53935), // Red
            "#D81B60" to Color(0xFFD81B60), // Pink
            "#8E24AA" to Color(0xFF8E24AA), // Purple
            "#5E35B1" to Color(0xFF5E35B1), // Deep Purple
            "#3949AB" to Color(0xFF3949AB), // Indigo
            "#1E88E5" to Color(0xFF1E88E5), // Blue
            "#039BE5" to Color(0xFF039BE5), // Light Blue
            "#00ACC1" to Color(0xFF00ACC1), // Cyan
            "#00897B" to Color(0xFF00897B), // Teal
            "#43A047" to Color(0xFF43A047), // Green
            "#7CB342" to Color(0xFF7CB342), // Light Green
            "#C0CA33" to Color(0xFFC0CA33), // Lime
            "#FDD835" to Color(0xFFFDD835), // Yellow
            "#FFB300" to Color(0xFFFFB300), // Amber
            "#FB8C00" to Color(0xFFFB8C00), // Orange
            "#F4511E" to Color(0xFFF4511E), // Deep Orange
            "#6D4C41" to Color(0xFF6D4C41), // Brown
            "#757575" to Color(0xFF757575), // Grey
            "#546E7A" to Color(0xFF546E7A), // Blue Grey
            "#212121" to Color(0xFF212121)  // Charcoal
        )
    }

    var selectedIcon by remember { mutableStateOf(prefs.getString("icon_name", "bell") ?: "bell") }
    var selectedColorHex by remember { mutableStateOf(prefs.getString("icon_color", "#FB8C00") ?: "#FB8C00") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            showCropDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Live status bar notification notification preview card!
        item {
            Text(
                text = getLoc("Xem trước thông báo tiếp theo", "Preview Next Notification"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            NotificationMockupCard(
                title = title.ifBlank { getLoc("Tiêu đề", "Title") },
                content = messageItems.firstOrNull()?.text?.ifBlank { getLoc("Nhập nội dung thứ nhất...", "Enter first message content...") } ?: getLoc("Nhập nội dung thứ nhất...", "Enter first message content..."),
                iconName = selectedIcon,
                colorHex = selectedColorHex
            )
        }

        // Section header Custom Content: Custom message list planner!
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = getLoc("DANH SÁCH CHUỖI TIN NHẮN", "MESSAGES CHAIN LIST"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(getLoc("Tiêu đề", "Title")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_title"),
                        singleLine = true,
                        placeholder = { Text(getLoc("Nhập tiêu đề", "Enter title")) }
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = getLoc("Nội dung và giây chờ của từng tin nhắn:", "Content and delay for each individual message:"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    messageItems.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = getLoc("Tin nhắn #${index + 1}", "Message #${index + 1}"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (messageItems.size > 1) {
                                        IconButton(
                                            onClick = {
                                                messageItems = messageItems.toMutableList().apply { removeAt(index) }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = getLoc("Xóa tin nhắn", "Delete message"),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = item.text,
                                    onValueChange = { newText: String ->
                                        messageItems = messageItems.toMutableList().apply {
                                            this[index] = this[index].copy(text = newText)
                                        }
                                    },
                                    label = { Text(getLoc("Nội dung tin nhắn", "Message content")) },
                                    modifier = Modifier.fillMaxWidth().testTag("input_message_$index"),
                                    maxLines = 3,
                                    placeholder = {
                                        Text(getLoc("Nhập nội dung...", "Enter content..."))
                                    }
                                )

                                Column {
                                    Text(
                                        text = getLoc("Thời gian nghỉ sau tin này (Độ trễ):", "Delay after this message (Duration):"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf(1, 2, 3).forEach { seconds ->
                                            val isSelected = item.delaySeconds == seconds
                                            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        BorderStroke(
                                                            1.dp,
                                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                        ),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        messageItems = messageItems.toMutableList().apply {
                                                            this[index] = this[index].copy(delaySeconds = seconds)
                                                        }
                                                    },
                                                color = containerColor,
                                                contentColor = contentColor
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = getLoc("$seconds giây ⏱️", "$seconds seconds ⏱️"),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            messageItems = messageItems.toMutableList().apply {
                                add(MessageItem(text = "", delaySeconds = 2))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm mới")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(getLoc("Thêm tin nhắn tiếp theo", "Add Next Message"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section Icon & Color customization
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = getLoc("BIỂU TƯỢNG & MÀU SẮC", "ICON & COLORS"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )

                    // Logo Select Row
                    Text(
                        text = getLoc("Chọn Icon hiển thị", "Select Icon to display"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableIcons, key = { it.first }) { (key, label) ->
                            val isSelected = selectedIcon == key
                            val tintColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            val bkColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            val borderMod = if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedIcon = key }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .then(borderMod)
                                        .background(bkColor, CircleShape)
                                ) {
                                    Icon(
                                        painter = painterResource(id = NotificationReceiver.getIconResId(key)),
                                        contentDescription = label,
                                        tint = tintColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = getLoc("Hoặc sử dụng ảnh riêng từ thiết bị 📷", "Or use personal custom image 📷"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isCustomIconSelected = selectedIcon.startsWith("custom_icon_")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val lastCustomIconFileName = remember(selectedIcon) {
                            val list = context.filesDir.listFiles { _, name -> name.startsWith("custom_icon_") }
                            list?.maxByOrNull { it.lastModified() }?.name
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (isCustomIconSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .clickable {
                                    if (lastCustomIconFileName != null) {
                                        selectedIcon = lastCustomIconFileName
                                    } else {
                                        galleryLauncher.launch("image/*")
                                    }
                                }
                        ) {
                            if (lastCustomIconFileName != null) {
                                val b = BitmapFactory.decodeFile(File(context.filesDir, lastCustomIconFileName).absolutePath)
                                if (b != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = b.asImageBitmap(),
                                        contentDescription = "Custom preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Photo, contentDescription = "Pick image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCustomIconSelected) getLoc("Đang dùng ảnh cá nhân", "Using custom photo") else getLoc("Chưa bật ảnh cá nhân", "No custom photo active"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomIconSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lastCustomIconFileName != null) getLoc("Bấm ảnh tròn để chọn nhanh ảnh đã thiết lập", "Tap circular thumbnail to switch back to set photo") else getLoc("Hãy chọn ảnh từ máy để cắt và làm đại diện", "Select image from gallery to crop and set as avatar"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (lastCustomIconFileName != null) getLoc("Đổi ảnh", "Change Image") else getLoc("Chọn ảnh", "Pick Image"), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Color Select Row
                    Text(
                        text = getLoc("Màu sắc đại diện (Accent color)", "Accent Color"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.forEach { (hex, tint) ->
                            val isSelected = selectedColorHex == hex
                            val borderMod = if (isSelected) {
                                Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else Modifier

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(borderMod)
                                    .background(tint, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { selectedColorHex = hex }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Color",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Shortcut & system settings toggle entirely removed

        // Save and Activate Action Buttons
        item {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { getLoc("Nhắc nhở chuỗi", "Chain Reminder") }
                    if (messageItems.any { it.text.isBlank() }) {
                        Toast.makeText(context, getLoc("Vui lòng nhập đầy đủ nội dung hoặc xóa bớt tin nhắn trống!", "Please enter all contents or delete empty messages!"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Save style configuration and items list to SharedPreferences for Launcher Shortcut triggering
                    val prefsEditor = prefs.edit()
                    prefsEditor.putString("title", finalTitle)
                    prefsEditor.putString("icon_name", selectedIcon)
                    prefsEditor.putString("icon_color", selectedColorHex)
                    prefsEditor.putString("message_items", serializeMessageList(messageItems))
                    prefsEditor.apply()

                    Toast.makeText(context, getLoc("Cấu hình thông báo đã lưu thành công! Hãy tạo phím tắt ở tab 'Phím Tắt'", "Notification configuration saved successfully! Go create a home shortcut at the 'Shortcuts' tab."), Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("button_schedule"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = getLoc("LƯU CẤU HÌNH TIN NHẮN", "SAVE MESSAGES CONFIGURATION"),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    if (showCropDialog && selectedImageUri != null) {
        CropDialog(
            imageUri = selectedImageUri!!,
            onDismiss = {
                showCropDialog = false
                selectedImageUri = null
            },
            onCropSuccess = { croppedBitmap ->
                try {
                    // Save the cropped circular bitmap as PNG
                    val fileName = "custom_icon_${System.currentTimeMillis()}.png"
                    val file = File(context.filesDir, fileName)
                    FileOutputStream(file).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    selectedIcon = fileName
                    Toast.makeText(context, getLoc("Đã áp dụng ảnh đại diện cá nhân cực đẹp!", "Applied custom notification personal avatar!"), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, getLoc("Lỗi khi lưu ảnh cắt, vui lòng thử lại.", "Error saving cropped image, please try again."), Toast.LENGTH_SHORT).show()
                }
                showCropDialog = false
                selectedImageUri = null
            }
        )
    }
}

@Composable
fun ThemeSetupTab(viewModel: NotificationViewModel) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome text / Header
        item {
            Column {
                Text(
                    text = getLoc("Cài đặt Giao diện & Chủ đề", "Appearance & Theme Settings"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getLoc("Tùy chỉnh giao diện, phông chữ và bố cục tiện ích để có trải nghiệm hiển thị phù hợp nhất.", "Customize the visual appearance, fonts, and widget layout for your style."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section: Language Selection (Ngôn Ngữ)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = getLoc("Ngôn ngữ", "Language"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getLoc("Chọn ngôn ngữ hiển thị chính cho ứng dụng.", "Select the main appearance language for the application."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isViSelected = com.example.ui.theme.ThemeManager.currentLanguage == "vi"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    com.example.ui.theme.ThemeManager.saveLanguage(context, "vi")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isViSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isViSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Tiếng Việt 🇻🇳",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isViSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isViSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val isEnSelected = com.example.ui.theme.ThemeManager.currentLanguage == "en"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    com.example.ui.theme.ThemeManager.saveLanguage(context, "en")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEnSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isEnSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "English 🇺🇸",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isEnSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isEnSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Color Themes (Chủ đề Màu sắc)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = getLoc("Chủ đề Màu Sắc", "Color Themes"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getLoc("Chọn các tông màu hiển thị phổ biến hoặc phong cách lập trình viên nổi tiếng.", "Select popular UI color palettes or famous developer themes."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val themesList = listOf(
                        "clean_minimal" to getLoc("Tối Giản Tinh Tế (Sáng)", "Minimalist Clean (Light)"),
                        "organic_emerald" to getLoc("Thảo Mộc Eco (Xanh Rêu)", "Organic Herb (Sage Green)"),
                        "light_coding" to getLoc("Lập Trình Viên (Sáng)", "Developer Light"),
                        "nebula_cosmic" to getLoc("Vũ Trụ Nebula (Tối)", "Cosmic Nebula (Dark)"),
                        "monokai_dev" to getLoc("Retro Monokai (Tối)", "Retro Monokai (Dark)"),
                        "tokyo_night" to getLoc("Tokyo Cyberpunk (Tối)", "Tokyo Cyberpunk (Dark)"),
                        "midnight_black" to getLoc("Midnight Pure AMOLED (Đen)", "Midnight Pure AMOLED (Black)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themesList.forEach { (themeKey, themeLabel) ->
                            val isSelected = com.example.ui.theme.ThemeManager.currentTheme == themeKey
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        com.example.ui.theme.ThemeManager.saveTheme(context, themeKey)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = themeLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Layout & Alignments (Bố cục & Kiểu hiển thị)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = getLoc("Bố cục Danh sách", "List Layout"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val layoutsList = listOf(
                        "card_grid" to getLoc("Thẻ Bo Tròn Lớn", "Rounded Large Cards"),
                        "compact_row" to getLoc("Dòng Đơn Tối Giản", "Minimal Single Row")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        layoutsList.forEach { (layoutKey, layoutLabel) ->
                            val isSelected = com.example.ui.theme.ThemeManager.currentLayout == layoutKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable {
                                        com.example.ui.theme.ThemeManager.saveLayout(context, layoutKey)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = layoutLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Cỡ chữ & Phông chữ (Text customization)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = getLoc("Cỡ Chữ & Phông Chữ", "Font Size & Style"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getLoc("Tự do phóng to, thu nhỏ cỡ chữ và chuyển đổi phong cách phông chữ của riêng bạn.", "Freely scale text size and select custom typography styles."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Font Size option title
                    Text(
                        text = getLoc("Cỡ Chữ Ứng Dụng:", "App Font Size:"),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val sizesList = listOf(
                        "small" to getLoc("Nhỏ (0.85x)", "Small (0.85x)"),
                        "normal" to getLoc("Vừa (Mặc định)", "Normal (Default)"),
                        "large" to getLoc("Lớn (1.15x)", "Large (1.15x)")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizesList.forEach { (sizeKey, sizeLabel) ->
                            val isSelected = com.example.ui.theme.ThemeManager.currentFontSize == sizeKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable {
                                        com.example.ui.theme.ThemeManager.saveFontSize(context, sizeKey)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = sizeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Family option title
                    Text(
                        text = getLoc("Kiểu Phông Chữ:", "Font Style:"),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val familiesList = listOf(
                        "system" to getLoc("Hệ thống", "System Default"),
                        "monospace" to getLoc("Lập trình (Mono)", "Monospace (Mono)"),
                        "serif" to getLoc("Cổ điển (Serif)", "Classic (Serif)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        familiesList.forEach { (familyKey, familyLabel) ->
                            val isSelected = com.example.ui.theme.ThemeManager.currentFontFamily == familyKey
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable {
                                        com.example.ui.theme.ThemeManager.saveFontFamily(context, familyKey)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = familyLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    schedules: List<NotificationSchedule>,
    history: List<NotificationSchedule>,
    viewModel: NotificationViewModel
) {
    val shortDateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }
    val longDateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()) }

    if (schedules.isEmpty() && history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = "History Empty",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = getLoc("Lịch sử & Hàng chờ trống", "History & Queue Empty"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getLoc("Chưa có thông báo nào được đặt lịch hoặc gửi đi. Hãy cấu hình một thông báo mới ở tab \"Tạo Mới\" ngay nhé!", "No scheduled or sent notifications found. Create a new notification chain at the \"Create\" tab!"),
                    maxLines = 3,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        val groupedSchedules = remember(schedules) {
            schedules.groupBy { it.groupId ?: "single_${it.id}" }
        }

        val isCompactLayout = com.example.ui.theme.ThemeManager.currentLayout == "compact_row"
        val activeCardShape = if (isCompactLayout) RoundedCornerShape(12.dp) else RoundedCornerShape(24.dp)
        val activeCardPadding = if (isCompactLayout) 10.dp else 16.dp
        val activeIconBoxSize = if (isCompactLayout) 28.dp else 40.dp
        val activeIconSize = if (isCompactLayout) 14.dp else 20.dp
        val activeHorizontalSpace = if (isCompactLayout) 8.dp else 12.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // === PART 1: SCHEDULED/PENDING ITEMS ===
            if (schedules.isNotEmpty()) {
                item {
                    Text(
                        text = getLoc("Thông Báo Đang Chờ Gửi (${schedules.size})", "Pending Notifications (${schedules.size})"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(groupedSchedules.toList(), key = { "active_${it.first}" }) { (groupId, groupSchedules) ->
                    val isGroup = groupSchedules.size > 1 && groupId.startsWith("group_")

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("schedule_card_$groupId"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGroup) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isGroup) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        shape = activeCardShape
                    ) {
                        Column(modifier = Modifier.padding(activeCardPadding)) {
                            if (isGroup) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = "Chain logo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = getLoc("Chuỗi gửi liên tiếp (${groupSchedules.size} tin)", "Consecutive sending chain (${groupSchedules.size} msgs)"),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.cancelGroup(groupId) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Hủy", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(getLoc("Hủy chuỗi", "Cancel Chain"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Divider(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            }

                            groupSchedules.forEachIndexed { sIdx, item ->
                                val parsedColor = try { Color(android.graphics.Color.parseColor(item.iconColor ?: "#FB8C00")) } catch (e: Exception) { Color(0xFFFB8C00) }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(activeIconBoxSize)
                                                .background(
                                                    color = parsedColor.copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(id = NotificationReceiver.getIconResId(item.iconName)),
                                                contentDescription = "Notify symbol",
                                                tint = parsedColor,
                                                modifier = Modifier.size(activeIconSize)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(activeHorizontalSpace))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${getLoc("Hẹn gửi: ", "Scheduled: ")}${shortDateFormat.format(Date(item.triggerTimeMs))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { viewModel.cancelSchedule(item.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isGroup) Icons.Default.Close else Icons.Default.DeleteSweep,
                                            contentDescription = "Xóa",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(if (isGroup) 16.dp else 20.dp)
                                        )
                                    }
                                }

                                if (sIdx < groupSchedules.size - 1) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // === PART 2: SENT HISTORY ===
            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getLoc("Lịch Sử Đã Gửi (${history.size})", "Sent History (${history.size})"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { viewModel.clearHistory() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History Logo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getLoc("Xóa hết", "Clear All"), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(history, key = { "history_${it.id}" }) { item ->
                    val parsedColor = try { Color(android.graphics.Color.parseColor(item.iconColor ?: "#FB8C00")) } catch (e: Exception) { Color(0xFFFB8C00) }

                    if (isCompactLayout) {
                        // Minimal list layout
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("history_item_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = parsedColor,
                                            shape = CircleShape
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.content,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success tick",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = getLoc("Đã gửi", "Sent"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("history_item_${item.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = parsedColor.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(id = NotificationReceiver.getIconResId(item.iconName)),
                                        contentDescription = "Sent Notify Icon",
                                        tint = parsedColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Success tick",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = getLoc("Đã gửi", "Sent"),
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "${getLoc("Gửi vào lúc: ", "Sent at: ")}${longDateFormat.format(Date(item.triggerTimeMs))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A beautiful visual mockup mimicking high fidelity Android Status Bar Notification card
 */
@Composable
fun NotificationMockupCard(
    title: String,
    content: String,
    iconName: String,
    colorHex: String
) {
    val context = LocalContext.current
    val accentColor = remember(colorHex) { Color(android.graphics.Color.parseColor(colorHex)) }

    val customIconFile = remember(iconName) {
        if (iconName.startsWith("custom_icon_")) {
            File(context.filesDir, iconName)
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Simulated Status Bar notification metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .background(accentColor, CircleShape)
                            .clip(CircleShape)
                    ) {
                        if (customIconFile != null && customIconFile.exists()) {
                            val b = BitmapFactory.decodeFile(customIconFile.absolutePath)
                            if (b != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = b.asImageBitmap(),
                                    contentDescription = "Custom preview status badge",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = NotificationReceiver.getIconResId(iconName)),
                                    contentDescription = "System mockup icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(id = NotificationReceiver.getIconResId(iconName)),
                                contentDescription = "System mockup icon",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = "CUSTOM NOTIFIER",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "bây giờ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Simulated Notification Content text body
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Icon layout generator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape)
                        .clip(CircleShape)
                ) {
                    if (customIconFile != null && customIconFile.exists()) {
                        val b = BitmapFactory.decodeFile(customIconFile.absolutePath)
                        if (b != null) {
                            androidx.compose.foundation.Image(
                                bitmap = b.asImageBitmap(),
                                contentDescription = "Custom mockup large icon",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = NotificationReceiver.getIconResId(iconName)),
                                contentDescription = "mockup logo represent",
                                tint = accentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = NotificationReceiver.getIconResId(iconName)),
                            contentDescription = "mockup logo represent",
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropSuccess: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    
    // Load bitmap with proper size restrictions to prevent out of memory issues
    val sourceBitmap = remember(imageUri) {
        try {
            val contentResolver = context.contentResolver
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            val maxDimension = 1500
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            
            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true
            }
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, finalOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    if (sourceBitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Lỗi tải ảnh") },
            text = { Text("Không thể mở hoặc xử lý ảnh này. Vui lòng chọn ảnh khác từ máy!") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Đóng")
                }
            }
        )
        return
    }

    // Viewport and gesturings
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cắt ảnh đại diện",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Di chuyển ảnh bằng tay và điều chỉnh thanh thu phóng ở dưới để lấy lòng vòng tròn làm logo.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // The Crop bounds view frame!
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        Image(
                            bitmap = sourceBitmap.asImageBitmap(),
                            contentDescription = "Original image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }

                    // Mask Layer overlays a clean circular visual cutout
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    ) {
                        val canvasSize = size
                        val circleRadius = canvasSize.minDimension * 0.44f
                        val path = Path().apply {
                            addRect(Rect(Offset.Zero, canvasSize))
                        }
                        drawPath(path, color = Color.Black.copy(alpha = 0.55f))
                        drawCircle(
                            color = Color.Transparent,
                            radius = circleRadius,
                            center = center,
                            blendMode = BlendMode.Clear
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = circleRadius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Interactive zoom Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Độ thu phóng (Zoom)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..4f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick reset button
                OutlinedButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đặt lại vị trí", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val srcW = sourceBitmap.width
                    val srcH = sourceBitmap.height
                    
                    val viewportDp = 240
                    val contextDensity = context.resources.displayMetrics.density
                    val viewSizePx = viewportDp * contextDensity
                    
                    val scaleToFit = maxOf(viewSizePx / srcW, viewSizePx / srcH)
                    val outSize = 256
                    val ratio = outSize.toFloat() / viewSizePx

                    val cropped = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(cropped)
                    canvas.drawColor(android.graphics.Color.TRANSPARENT)

                    val matrix = android.graphics.Matrix()
                    matrix.postTranslate(-srcW / 2f, -srcH / 2f)
                    val totalScale = scaleToFit * scale * ratio
                    matrix.postScale(totalScale, totalScale)
                    matrix.postTranslate(outSize / 2f + offsetX * ratio, outSize / 2f + offsetY * ratio)

                    canvas.drawBitmap(
                        sourceBitmap,
                        matrix,
                        Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true }
                    )

                    val circularCropped = getCircularBitmap(cropped)
                    onCropSuccess(circularCropped)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Xong & Cắt", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = MaterialTheme.colorScheme.error)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

fun getCircularBitmap(srcBitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint().apply {
        isAntiAlias = true
    }
    val rect = android.graphics.Rect(0, 0, srcBitmap.width, srcBitmap.height)
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(srcBitmap.width / 2f, srcBitmap.height / 2f, srcBitmap.width / 2f, paint)
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(srcBitmap, rect, rect, paint)
    return output
}

fun renderShortcutIconToBitmap(context: Context, iconName: String, colorHex: String): Bitmap {
    val size = 256
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Draw background circle
    paint.color = android.graphics.Color.parseColor(colorHex)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Draw vector icon on top
    val resId = NotificationReceiver.getIconResId(iconName)
    val drawable = context.resources.getDrawable(resId, context.theme)
    if (drawable != null) {
        androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, android.graphics.Color.WHITE)
        val iconSize = (size * 0.52).toInt()
        val left = (size - iconSize) / 2
        val top = (size - iconSize) / 2
        drawable.setBounds(left, top, left + iconSize, top + iconSize)
        drawable.draw(canvas)
    }
    
    return bitmap
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutTab(viewModel: NotificationViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("shortcut_prefs", Context.MODE_PRIVATE) }
    
    var shortcutName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("bell") }
    var selectedColorHex by remember { mutableStateOf("#1E88E5") }
    var showPopupProgress by remember { mutableStateOf(prefs.getBoolean("show_popup_progress", true)) }

    val activeVersion = remember { prefs.getLong("active_shortcut_version", 0L) }
    
    val availableIcons = remember {
        listOf(
            "bell" to "Chuông",
            "star" to "Sao",
            "heart" to "Yêu thích",
            "alert" to "Cảnh báo",
            "chat" to "Trò chuyện",
            "gift" to "Quà tặng",
            "coffee" to "Cốc nước",
            "check" to "Hoàn thành",
            "mail" to "Thư",
            "music" to "Nhạc",
            "home" to "Nhà",
            "settings" to "Cài đặt",
            "phone" to "Điện thoại"
        )
    }

    val colors = remember {
        listOf(
            "#E53935" to Color(0xFFE53935),
            "#D81B60" to Color(0xFFD81B60),
            "#8E24AA" to Color(0xFF8E24AA),
            "#5E35B1" to Color(0xFF5E35B1),
            "#3949AB" to Color(0xFF3949AB),
            "#1E88E5" to Color(0xFF1E88E5),
            "#039BE5" to Color(0xFF039BE5),
            "#00ACC1" to Color(0xFF00ACC1),
            "#00897B" to Color(0xFF00897B),
            "#43A047" to Color(0xFF43A047),
            "#7CB342" to Color(0xFF7CB342),
            "#C0CA33" to Color(0xFFC0CA33),
            "#FDD835" to Color(0xFFFDD835),
            "#FFB300" to Color(0xFFFFB300),
            "#FB8C00" to Color(0xFFFB8C00),
            "#F4511E" to Color(0xFFF4511E),
            "#6D4C41" to Color(0xFF6D4C41),
            "#757575" to Color(0xFF757575),
            "#546E7A" to Color(0xFF546E7A),
            "#212121" to Color(0xFF212121)
        )
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            showCropDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Builder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = getLoc("Trình dựng Phím tắt", "Shortcut Builder"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = getLoc("Tùy chỉnh biểu tượng & tên hiển thị theo phong cách cá nhân và gán ra màn hình chính làm phím tắt siêu tốc!", "Customize shortcut name & icon style to represent your custom workspace, then pin to your launcher for high-speed chain notifications!"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getLoc("ẢNH XEM TRƯỚC LỐI TẮT MÀN HÌNH CHÍNH", "LAUNCHER SHORTCUT PREVIEW"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )

                Box(
                    modifier = Modifier
                        .size(140.dp, 140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(selectedColorHex))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedIcon.startsWith("custom_icon_")) {
                                val customIconFile = File(context.filesDir, selectedIcon)
                                if (customIconFile.exists()) {
                                    val b = BitmapFactory.decodeFile(customIconFile.absolutePath)
                                    if (b != null) {
                                        Image(
                                            bitmap = b.asImageBitmap(),
                                            contentDescription = "Custom icon preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Photo, contentDescription = "Custom", tint = Color.White)
                                    }
                                } else {
                                    Icon(Icons.Default.Photo, contentDescription = "Custom", tint = Color.White)
                                }
                            } else {
                                Icon(
                                    painter = painterResource(id = NotificationReceiver.getIconResId(selectedIcon)),
                                    contentDescription = "System mockup icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = shortcutName.ifBlank { getLoc("Gửi tin nhanh", "Send Fast") },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = getLoc("TÊN PHÍM TẮT MÀN HÌNH CHÍNH", "LAUNCHER SHORTCUT NAME"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = shortcutName,
                        onValueChange = { shortcutName = it },
                        modifier = Modifier.fillMaxWidth().testTag("shortcut_name_input"),
                        placeholder = { Text(getLoc("Tên lối tắt", "Shortcut Name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = getLoc("THIẾT KẾ BIỂU TƯỢNG PHÍM TẮT", "LAUNCHER SHORTCUT ICON DESIGN"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = getLoc("Kiểu hiển thị biểu tượng", "Icon visual style"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        availableIcons.forEach { (iconId, label) ->
                            val isSelected = selectedIcon == iconId
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedIcon = iconId }
                            ) {
                                Icon(
                                    painter = painterResource(id = NotificationReceiver.getIconResId(iconId)),
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = getLoc("Màu sắc nền thương hiệu", "Brand background color"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.forEach { (hex, color) ->
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        BorderStroke(
                                            if (isSelected) 3.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = getLoc("Hoặc sử dụng ảnh tải lên từ thiết bị", "Or use image uploaded from device"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isCustomIconSelected = selectedIcon.startsWith("custom_icon_")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val lastCustomIconFileName = remember(selectedIcon) {
                            val list = context.filesDir.listFiles { _, name -> name.startsWith("custom_icon_") }
                            list?.maxByOrNull { it.lastModified() }?.name
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (isCustomIconSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .clickable {
                                    if (lastCustomIconFileName != null) {
                                        selectedIcon = lastCustomIconFileName
                                    } else {
                                        galleryLauncher.launch("image/*")
                                    }
                                }
                        ) {
                            if (lastCustomIconFileName != null) {
                                val b = BitmapFactory.decodeFile(File(context.filesDir, lastCustomIconFileName).absolutePath)
                                if (b != null) {
                                    Image(
                                        bitmap = b.asImageBitmap(),
                                        contentDescription = "Custom preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Photo, contentDescription = "Pick image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCustomIconSelected) getLoc("Đang dùng ảnh cá nhân", "Using custom photo") else getLoc("Chưa bật ảnh cá nhân", "No custom photo active"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomIconSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lastCustomIconFileName != null) getLoc("Bấm màu tròn để chọn nhanh ảnh đã cắt", "Tap circular thumbnail to switch back to set photo") else getLoc("Tự upload ảnh có sẵn lấy một vùng tròn", "Upload photo from gallery to crop and apply"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (lastCustomIconFileName != null) getLoc("Đổi ảnh", "Change Image") else getLoc("Chọn ảnh", "Pick Image"), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = getLoc("CẤU HÌNH PHƯƠNG THỨC KÍCH HOẠT", "TRIGGER BEHAVIOR CONFIGURATION"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = getLoc("Hiển thị thanh tiến trình khi chạy", "Show trigger progress overlay"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = getLoc("Thực thi tác vụ và thông báo chạy ngầm ngay lập tức, đem lại trải nghiệm mượt mà, tối giản.", "Allows starting tasks in the background with visual progress feedback overlay on screen."),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Switch(
                            checked = showPopupProgress,
                            onCheckedChange = { showPopupProgress = it },
                            modifier = Modifier.testTag("switch_show_progress_shortcut")
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val newVersion = System.currentTimeMillis()
                    val prefsEditor = prefs.edit()
                    prefsEditor.putBoolean("show_popup_progress", showPopupProgress)
                    prefsEditor.putLong("active_shortcut_version", newVersion)
                    prefsEditor.apply()

                    var renderIcon: Bitmap? = null
                    if (selectedIcon.startsWith("custom_icon_")) {
                        val file = File(context.filesDir, selectedIcon)
                        if (file.exists()) {
                            renderIcon = BitmapFactory.decodeFile(file.absolutePath)
                        }
                    }
                    if (renderIcon == null) {
                        renderIcon = renderShortcutIconToBitmap(context, selectedIcon, selectedColorHex)
                    }

                    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                        val pinShortcutInfo = ShortcutInfoCompat.Builder(context, "shortcut_pin")
                            .setShortLabel(shortcutName.ifBlank { getLoc("Gửi tin nhanh", "Send Fast") })
                            .setIcon(IconCompat.createWithBitmap(renderIcon))
                            .setIntent(Intent(context, ShortcutActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                putExtra("shortcut_version", newVersion)
                            })
                            .build()

                        val pinSuccess = ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
                        if (pinSuccess) {
                            Toast.makeText(context, getLoc("Đã khởi tạo phím tắt mới! Hãy xác nhận thêm phím tắt ngoài màn hình chính của thiết bị.", "Created new shortcut! Please confirm adding the shortcut onto your device's home screen."), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, getLoc("Lỗi yêu cầu thêm phím tắt, vui lòng thử lại.", "Error requesting shortcut addition, please try again."), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, getLoc("Thiết bị không hỗ trợ ghim phím tắt động!", "Device does not support pinning dynamic shortcuts!"), Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_build_shortcut_button"),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Build, contentDescription = "Build icon")
                    Text(getLoc("TẠO PHÍM TẮT MỚI", "CREATE NEW SHORTCUT"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = getLoc("Hướng dẫn quan trọng khi build", "Important compilation guide"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getLoc(
                            "• Mặc định khi tải ứng dụng sẽ không có phím tắt sẵn ngoài màn hình chính.\n" +
                                    "• Mỗi lần bạn 'Build phím tắt mới' thành công, phím tắt cũ trước đó (nếu có trên màn hình chính) sẽ lập tức hết hạn, hiển thị cảnh báo yêu cầu gỡ bỏ và không thể sử dụng để tránh nhầm lẫn.\n" +
                                    "• Trên một số dòng máy Android, bạn cần cấp thêm quyền 'Lối tắt màn hình chính' trong Cài đặt ứng dụng để có thể gán phím tắt tự động.",
                            "• By default, there is no shortcut pinned on your home screen.\n" +
                                    "• Every time you click 'Create New Shortcut', any previous shortcut on your home screen will immediately expire, display an expiration warning, and cannot be used to prevent confusion.\n" +
                                    "• On some Android devices, you must grant the 'Home screen shortcut' permission in App settings to allow automatic shortcut pinning."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                    if (activeVersion > 0) {
                        Text(
                            text = "${getLoc("Mã phiên bản hoạt động hiện tại: ", "Current active version: ")}VS-$activeVersion",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCropDialog && selectedImageUri != null) {
        CropDialog(
            imageUri = selectedImageUri!!,
            onDismiss = {
                showCropDialog = false
                selectedImageUri = null
            },
            onCropSuccess = { croppedBitmap ->
                try {
                    val fileName = "custom_icon_${System.currentTimeMillis()}.png"
                    val file = File(context.filesDir, fileName)
                    FileOutputStream(file).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    selectedIcon = fileName
                    Toast.makeText(context, getLoc("Đã áp dụng ảnh đại diện cá nhân cực đẹp!", "Applied custom notification personal avatar!"), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, getLoc("Lỗi khi lưu ảnh cắt, vui lòng thử lại.", "Error saving cropped image, please try again."), Toast.LENGTH_SHORT).show()
                }
                showCropDialog = false
                selectedImageUri = null
            }
        )
    }
}

