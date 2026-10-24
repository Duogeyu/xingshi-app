package app.shixing.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 和站点的深色底保持一致：底 #05060A、卡片 #11141F、主色 #4DB4FF、次色 #D69BFF。
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB4FF),
    onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF123049),
    onPrimaryContainer = Color(0xFFBFE4FF),
    secondary = Color(0xFFD69BFF),
    onSecondary = Color(0xFF1D0630),
    background = Color(0xFF05060A),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF11141F),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF1A1F2E),
    onSurfaceVariant = Color(0xFF9AA3B5),
    outline = Color(0x22FFFFFF),
    error = Color(0xFFFF7A7A),
)

@Composable
fun ShixingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
