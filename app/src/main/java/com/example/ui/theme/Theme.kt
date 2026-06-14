package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Theme manager state synced with SharedPreferences
object ThemeManager {
    var currentTheme by mutableStateOf("clean_minimal")
    var currentLayout by mutableStateOf("card_grid")
    var currentFontSize by mutableStateOf("normal")
    var currentFontFamily by mutableStateOf("app_font")
    var currentLanguage by mutableStateOf("vi")

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        currentTheme = prefs.getString("theme", "clean_minimal") ?: "clean_minimal"
        currentLayout = prefs.getString("layout", "card_grid") ?: "card_grid"
        currentFontSize = prefs.getString("font_size", "normal") ?: "normal"
        currentFontFamily = prefs.getString("font_family", "app_font") ?: "app_font"
        currentLanguage = prefs.getString("language", "vi") ?: "vi"
    }

    fun saveTheme(context: Context, theme: String) {
        currentTheme = theme
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", theme)
            .apply()
    }

    fun saveLayout(context: Context, layout: String) {
        currentLayout = layout
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("layout", layout)
            .apply()
    }

    fun saveFontSize(context: Context, size: String) {
        currentFontSize = size
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("font_size", size)
            .apply()
    }

    fun saveFontFamily(context: Context, family: String) {
        currentFontFamily = family
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("font_family", family)
            .apply()
    }

    fun saveLanguage(context: Context, lang: String) {
        currentLanguage = lang
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("language", lang)
            .apply()
    }
}

// 1. Tối Giản Tinh Tế (Clean Slate - Lavender Airy Light default)
private val CleanSlateColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    background = Color(0xFFF7F2FA),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFECE6F0),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFD9D9D9),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260)
)

// 2. Vũ Trụ Huyền Bí (Deep Cosmic/Nebula Cosmic)
private val DeepCosmicColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF3700B3),
    onPrimaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF0C0B14),
    onBackground = Color(0xFFFFFFFFF0),
    surface = Color(0xFF131124),
    onSurface = Color(0xFFF0EFFF),
    surfaceVariant = Color(0xFF221F38),
    onSurfaceVariant = Color(0xFFCECADF),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF3D3A57),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFFCF6679)
)

// 3. Thảo Mộc Xanh Rêu (Sage Emerald)
private val SageEmeraldColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0A310D),
    background = Color(0xFFEDF5EF),
    onBackground = Color(0xFF19241B),
    surface = Color(0xFFF4F9F5),
    onSurface = Color(0xFF1C2D1F),
    surfaceVariant = Color(0xFFDCE6DE),
    onSurfaceVariant = Color(0xFF4E5E53),
    outline = Color(0xFF727F74),
    outlineVariant = Color(0xFFCAD1CB),
    secondary = Color(0xFF388E3C),
    tertiary = Color(0xFFF57C00)
)

// 4. Monokai Dev (Dark Retro Pro)
private val MonokaiDevColorScheme = darkColorScheme(
    primary = Color(0xFFF92672), // Monokai Pink
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF501227),
    onPrimaryContainer = Color(0xFFF92672),
    background = Color(0xFF272822), // Hard charcoal
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF1E1F1C),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF3E3F3A),
    onSurfaceVariant = Color(0xFFA6E22E), // Lime Green highlight
    outline = Color(0xFF75715E),
    outlineVariant = Color(0xFF49483E),
    secondary = Color(0xFFA6E22E),
    tertiary = Color(0xFF66D9EF)
)

// 5. Tokyo Night (Cyberpunk Modern)
private val TokyoNightColorScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7), // Tokyo Blue
    onPrimary = Color(0xFF1A1B26),
    primaryContainer = Color(0xFF21253C),
    onPrimaryContainer = Color(0xFF89DDFF),
    background = Color(0xFF1A1B26),
    onBackground = Color(0xFFC0CAF5),
    surface = Color(0xFF16161E),
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = Color(0xFF24283B),
    onSurfaceVariant = Color(0xFFBB9AF3), // Tokyo Purple
    outline = Color(0xFF565F89),
    outlineVariant = Color(0xFF383E56),
    secondary = Color(0xFF2AC3DE),
    tertiary = Color(0xFF9ECE6A)
)

// 6. Midnight Black (Amoled Dark)
private val MidnightBlackColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF222222),
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFDDDDDD),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFF444444),
    secondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF888888)
)

// 7. Lập Trình Viên Sáng (Coding Light)
private val CodingLightColorScheme = lightColorScheme(
    primary = Color(0xFF006064), // Deep Teal
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF37474F),
    outline = Color(0xFF78909C),
    outlineVariant = Color(0xFFCFD8DC),
    secondary = Color(0xFF00ACC1),
    tertiary = Color(0xFFF57C00)
)

fun getDynamicTypography(): Typography {
    // 1. Determine Font Family
    val family = when (ThemeManager.currentFontFamily) {
        "monospace" -> FontFamily.Monospace
        "serif" -> FontFamily.Serif
        "app_font" -> FontFamily.SansSerif // Fallback since EmojiFontManager is removed
        else -> FontFamily.SansSerif
    }

    // 2. Determine Scale Factor
    val scale = when (ThemeManager.currentFontSize) {
        "small" -> 0.85f
        "large" -> 1.15f
        else -> 1.0f
    }

    return Typography(
        displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (57 * scale).sp, lineHeight = (64 * scale).sp),
        displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (45 * scale).sp, lineHeight = (52 * scale).sp),
        displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (36 * scale).sp, lineHeight = (44 * scale).sp),
        
        headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (32 * scale).sp, lineHeight = (40 * scale).sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (28 * scale).sp, lineHeight = (36 * scale).sp),
        headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (24 * scale).sp, lineHeight = (32 * scale).sp),
        
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (22 * scale).sp, lineHeight = (28 * scale).sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp),
        titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
        
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp, letterSpacing = 0.4.sp),
        
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (11 * scale).sp, lineHeight = (16 * scale).sp)
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (ThemeManager.currentTheme) {
        "nebula_cosmic" -> DeepCosmicColorScheme
        "organic_emerald" -> SageEmeraldColorScheme
        "monokai_dev" -> MonokaiDevColorScheme
        "tokyo_night" -> TokyoNightColorScheme
        "midnight_black" -> MidnightBlackColorScheme
        "light_coding" -> CodingLightColorScheme
        else -> CleanSlateColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getDynamicTypography(),
        content = content
    )
}
