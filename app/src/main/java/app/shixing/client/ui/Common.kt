package app.shixing.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import app.shixing.client.data.Groups

/**
 * 星的自绘头像：没有照片（接口不给照片），用群组色渐变 + 名字首字，
 * 和网页端那个「.xd-ph none」的空心字形是一个思路——但这里更好认。
 */
@Composable
fun Avatar(name: String?, group: String?, size: Dp = 38.dp) {
    val base = Color(Groups.color(group))
    val ch = name?.trim()?.take(1).orEmpty().ifEmpty { "星" }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(base.copy(alpha = 0.95f), base.copy(alpha = 0.45f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ch,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF07101A),
        )
    }
}

/**
 * 只允许 vrcc.cc 的图片地址（令牌和数据都不出站，图片也不能）。
 */
fun siteUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    return when {
        path.startsWith("https://vrcc.cc/") -> path
        path.startsWith("/") -> "https://vrcc.cc$path"
        else -> null
    }
}

/** 地图封面。拿不到地址就留一块同色底，不显示裂图。 */
@Composable
fun Cover(path: String?, modifier: Modifier = Modifier, radius: Dp = 12.dp) {
    val url = siteUrl(path)
    Box(modifier.clip(RoundedCornerShape(radius)).background(MaterialTheme_color.card2)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
