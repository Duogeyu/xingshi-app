package app.shixing.client.data

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.sqrt

/**
 * 星卡照片。
 *
 * 官方接口从不给图片文件：给的是 `grid`——一串 base64，解出来是 N×N 的 RGB 像素
 * （每 3 字节一个像素），客户端自己放大就成网页上那种「粗颜色格」。
 * 只有互相连上（level 2）才会额外给一个 10 分钟有效的 `url`。
 */
data class CardPhoto(
    val level: Int = 0,
    val url: String? = null,
    val grid: Bitmap? = null,
) {
    val visible: Boolean get() = url != null || grid != null
}

object PhotoParser {

    fun parse(o: JsonObject?): CardPhoto? {
        if (o == null) return null
        fun str(k: String): String? = (o[k] as? JsonPrimitive)?.contentOrNull
        val level = str("level")?.toIntOrNull() ?: 0
        val url = str("url")?.takeIf { it.isNotBlank() }
        val grid = str("grid")?.let { decodeGrid(it) }
        val p = CardPhoto(level = level, url = url, grid = grid)
        return if (p.visible) p else null
    }

    /** `g:` 前缀 + base64；解出来每 3 字节一个像素。 */
    fun decodeGrid(raw: String): Bitmap? = runCatching {
        val body = raw.removePrefix("g:")
        val bytes = Base64.decode(body, Base64.DEFAULT)
        val pixels = bytes.size / 3
        if (pixels < 4) return null
        val side = sqrt(pixels.toDouble()).toInt().coerceAtLeast(2)
        val n = side * side
        if (n > pixels) return null
        val colors = IntArray(n)
        for (i in 0 until n) {
            val r = bytes[i * 3].toInt() and 0xFF
            val g = bytes[i * 3 + 1].toInt() and 0xFF
            val b = bytes[i * 3 + 2].toInt() and 0xFF
            colors[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        Bitmap.createBitmap(colors, side, side, Bitmap.Config.ARGB_8888)
    }.getOrNull()
}
