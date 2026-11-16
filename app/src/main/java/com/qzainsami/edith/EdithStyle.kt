package com.qzainsami.edith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

enum class AppTheme(val id: String, val displayName: String, val subtitle: String) {
    DRACULA("dracula", "Dracula", "Comfortable aesthetic palette"),
    DARK("dark", "Dark", "Clean modern dark"),
    LIGHT("light", "Light", "Crisp daylight theme"),
    SYNTHWAVE("synthwave", "Synthwave", "Retro neon night")
}

data class EdithColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundGradientEnd: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPink: Color,
    val accentGold: Color,
    val statusOnline: Color,
    val statusOffline: Color,
    val chatUserBubble: Color,
    val chatUserBorder: Color,
    val chatEdithBubble: Color,
    val chatEdithBorder: Color,
    val navIndicator: Color,
    val navSelectedText: Color,
    val navUnselected: Color,
    val liveDisplayGradientStart: Color,
    val liveDisplayGradientEnd: Color
)

// Official Dracula Theme specification: https://draculatheme.com/spec
val DraculaColors = EdithColors(
    isDark = true,
    background = Color(0xFF282A36),          // Dracula Background
    backgroundGradientEnd = Color(0xFF1E1F29), // Subtle darker gradient depth
    surface = Color(0xFF343746),             // Soft elevated card surface
    surfaceVariant = Color(0xFF44475A),      // Dracula Current Line / Selection
    border = Color(0xFF6272A4).copy(alpha = 0.45f), // Dracula Comment
    primary = Color(0xFFBD93F9),             // Dracula Purple
    onPrimary = Color(0xFF282A36),
    secondary = Color(0xFFFF79C6),           // Dracula Pink
    onSecondary = Color(0xFF282A36),
    textPrimary = Color(0xFFF8F8F2),         // Dracula Foreground
    textSecondary = Color(0xFFA4B1D6),       // Readable comment tint
    accentPink = Color(0xFFFF79C6),          // Dracula Pink
    accentGold = Color(0xFFF1FA8C),          // Dracula Yellow
    statusOnline = Color(0xFF50FA7B),        // Dracula Green
    statusOffline = Color(0xFFFF5555),       // Dracula Red
    chatUserBubble = Color(0xFF44475A),      // Current Line
    chatUserBorder = Color(0xFFBD93F9).copy(alpha = 0.5f),
    chatEdithBubble = Color(0xFF343746),
    chatEdithBorder = Color(0xFF6272A4).copy(alpha = 0.35f),
    navIndicator = Color(0xFF44475A),
    navSelectedText = Color(0xFFBD93F9),
    navUnselected = Color(0xFF6272A4),
    liveDisplayGradientStart = Color(0xFF343746),
    liveDisplayGradientEnd = Color(0xFF44475A)
)

val DarkColors = EdithColors(
    isDark = true,
    background = Color(0xFF121214),
    backgroundGradientEnd = Color(0xFF18181B),
    surface = Color(0xFF1E1E24),
    surfaceVariant = Color(0xFF272730),
    border = Color(0xFF3F3F4E),
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF0F172A),
    textPrimary = Color(0xFFF4F4F5),
    textSecondary = Color(0xFFA1A1AA),
    accentPink = Color(0xFFF472B6),
    accentGold = Color(0xFFFBBF24),
    statusOnline = Color(0xFF4ADE80),
    statusOffline = Color(0xFFF87171),
    chatUserBubble = Color(0xFF2E2E3E),
    chatUserBorder = Color(0xFF818CF8).copy(alpha = 0.5f),
    chatEdithBubble = Color(0xFF1E1E24),
    chatEdithBorder = Color(0xFF3F3F4E),
    navIndicator = Color(0xFF2E2E3E),
    navSelectedText = Color(0xFF818CF8),
    navUnselected = Color(0xFFA1A1AA),
    liveDisplayGradientStart = Color(0xFF1E1E24),
    liveDisplayGradientEnd = Color(0xFF272732)
)

val LightColors = EdithColors(
    isDark = false,
    background = Color(0xFFF1F5F9),
    backgroundGradientEnd = Color(0xFFE2E8F0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF8FAFC),
    border = Color(0xFFCBD5E1),
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFDB2777),
    onSecondary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    accentPink = Color(0xFFDB2777),
    accentGold = Color(0xFFD97706),
    statusOnline = Color(0xFF16A34A),
    statusOffline = Color(0xFFDC2626),
    chatUserBubble = Color(0xFFEEF2FF),
    chatUserBorder = Color(0xFFC7D2FE),
    chatEdithBubble = Color(0xFFFFFFFF),
    chatEdithBorder = Color(0xFFE2E8F0),
    navIndicator = Color(0xFFE0E7FF),
    navSelectedText = Color(0xFF4F46E5),
    navUnselected = Color(0xFF64748B),
    liveDisplayGradientStart = Color(0xFFFFFFFF),
    liveDisplayGradientEnd = Color(0xFFF1F5F9)
)

val SynthwaveColors = EdithColors(
    isDark = true,
    background = Color(0xFF240046),
    backgroundGradientEnd = Color(0xFF1A0033),
    surface = Color(0xFF3C096C),
    surfaceVariant = Color(0xFF481078),
    border = Color(0xFF7209B7),
    primary = Color(0xFFFFD60A),
    onPrimary = Color(0xFF240046),
    secondary = Color(0xFFF72585),
    onSecondary = Color(0xFF240046),
    textPrimary = Color(0xFFFFF1FA),
    textSecondary = Color(0xFFD9B8EE),
    accentPink = Color(0xFFFF79B9),
    accentGold = Color(0xFFFFD60A),
    statusOnline = Color(0xFFFFD60A),
    statusOffline = Color(0xFFFF8585),
    chatUserBubble = Color(0xFF55158A),
    chatUserBorder = Color(0xFFB58ACA),
    chatEdithBubble = Color(0xFF3C096C),
    chatEdithBorder = Color(0xFF7209B7),
    navIndicator = Color(0xFF7209B7),
    navSelectedText = Color(0xFFFFD60A),
    navUnselected = Color(0xFFD9B8EE),
    liveDisplayGradientStart = Color(0xFF3C096C),
    liveDisplayGradientEnd = Color(0xFF7209B7)
)

// Legacy constants retained for compatibility
val EdithGold = Color(0xFFFFD60A)
val EdithPink = Color(0xFFF72585)
val EdithPinkText = Color(0xFFFF79B9)
val EdithInk = Color(0xFF240046)
val EdithPanel = Color(0xFF3C096C)
val EdithLine = Color(0xFF7209B7)

val LocalEdithColors = staticCompositionLocalOf { DraculaColors }

object EdithTheme {
    val colors: EdithColors
        @Composable
        get() = LocalEdithColors.current
}

@Composable
fun EdithTheme(
    appTheme: AppTheme = AppTheme.DRACULA,
    content: @Composable () -> Unit
) {
    val colors = when(appTheme) {
        AppTheme.DRACULA -> DraculaColors
        AppTheme.DARK -> DarkColors
        AppTheme.LIGHT -> LightColors
        AppTheme.SYNTHWAVE -> SynthwaveColors
    }
    val colorScheme = if(colors.isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.surfaceVariant,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            secondaryContainer = colors.surfaceVariant,
            onSecondaryContainer = colors.textPrimary,
            tertiary = colors.accentPink,
            onTertiary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.background,
            surfaceContainerHigh = colors.surfaceVariant,
            surfaceContainerHighest = colors.surfaceVariant,
            surfaceContainerLowest = colors.background,
            outline = colors.border,
            outlineVariant = colors.surfaceVariant,
            error = colors.statusOffline,
            onError = colors.onPrimary
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.navIndicator,
            onPrimaryContainer = colors.primary,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            secondaryContainer = Color(0xFFFCE7F3),
            onSecondaryContainer = colors.secondary,
            tertiary = colors.accentPink,
            onTertiary = Color.White,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.background,
            surfaceContainerHigh = colors.surfaceVariant,
            surfaceContainerHighest = colors.border,
            surfaceContainerLowest = Color.White,
            outline = colors.border,
            outlineVariant = Color(0xFFE2E8F0),
            error = colors.statusOffline,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalEdithColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(18.dp)
            ),
            typography = Typography(
                titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
                titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
                labelSmall = Typography().labelSmall.copy(letterSpacing = 0.3.sp)
            ),
            content = content
        )
    }
}

@Composable
fun EdithLogo(modifier: Modifier = Modifier) {
    val colors = EdithTheme.colors
    Canvas(modifier.background(colors.surfaceVariant, RoundedCornerShape(12.dp))) {
        withTransform({ scale(size.width / 108f, size.height / 108f, Offset.Zero) }) {
            drawRoundRect(colors.secondary, Offset(23f, 28f), Size(62f, 50f), CornerRadius(13f), style = Stroke(3f))
            drawLine(colors.secondary, Offset(54f, 28f), Offset(54f, 21f), 3f)
            drawCircle(colors.primary, 3.5f, Offset(54f, 19f))
            drawRoundRect(colors.primary, Offset(34f, 43f), Size(13f, 14f), CornerRadius(4f))
            drawRoundRect(colors.primary, Offset(61f, 43f), Size(13f, 14f), CornerRadius(4f))
            val mouth = Path().apply { moveTo(45f, 64f); quadraticTo(54f, 72f, 63f, 64f) }
            drawPath(mouth, colors.primary, style = Stroke(3f))
        }
    }
}

@Composable
fun NavGlyph(index: Int, selected: Boolean) {
    val colors = EdithTheme.colors
    val ink = if (selected) colors.navSelectedText else colors.navUnselected
    val bg = colors.surface
    Canvas(Modifier.size(22.dp)) {
        withTransform({ scale(size.width / 24f, size.height / 24f, Offset.Zero) }) {
            when(index) {
                0 -> {
                    drawRoundRect(ink, Offset(3f, 3f), Size(18f, 14f), CornerRadius(4f), style = Stroke(1.6f))
                    drawLine(ink, Offset(6f, 17f), Offset(6f, 21f), 1.6f)
                    drawLine(ink, Offset(6f, 21f), Offset(11f, 17f), 1.6f)
                    for (x in listOf(8f, 12f, 16f)) drawCircle(ink, 0.8f, Offset(x, 10f))
                }
                1 -> {
                    for (i in 0..2) {
                        val x = 5f + i * 7
                        val y = if (i == 1) 15f else 8f
                        drawLine(ink, Offset(x, 3f), Offset(x, 21f), 1.6f)
                        drawCircle(bg, 3f, Offset(x, y))
                        drawCircle(ink, 2.5f, Offset(x, y), style = Stroke(1.6f))
                    }
                }
                else -> {
                    drawCircle(ink, 6f, Offset(12f, 12f), style = Stroke(1.6f))
                    drawCircle(ink, 2f, Offset(12f, 12f), style = Stroke(1.6f))
                    for (i in 0..7) {
                        val a = i * Math.PI / 4
                        drawLine(
                            ink,
                            Offset(12f + 7f * cos(a).toFloat(), 12f + 7f * sin(a).toFloat()),
                            Offset(12f + 9f * cos(a).toFloat(), 12f + 9f * sin(a).toFloat()),
                            1.6f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = EdithTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .background(colors.secondary, RoundedCornerShape(2.dp))
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
                headerAction?.invoke()
            }
            content()
        }
    }
}
