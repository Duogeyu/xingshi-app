package app.shixing.client.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 贴纸在卡面上的位置（网页 spots 的四个数：x%、y%、旋转角、尺寸）。 */
data class DecoSpot(
    val x: Float,
    val y: Float,
    val rot: Float,
    val size: Float,
)

/** 一张卡自带的装扮，字段名跟网页一致。 */
data class CardDeco(
    val face: String? = null,
    val back: String? = null,
    val sleeve: String? = null,
    val spots: List<DecoSpot> = emptyList(),
)

/** 卡面档位：网页 Deco.STYLES 里的 14 种，颜色是按网页观感配的近似值。 */
data class CardStyle(
    val id: String,
    val name: String,
    val top: Color,
    val bottom: Color,
    val glow: Color,
    val ink: Color = Color(0xFFF3F4F8),
    val subInk: Color = Color(0xFFE9E4D6),
    val frame: Color = Color(0xFFE8C983),
    val light: Boolean = false,
)

/** 卡背风格：网页 CSS 里 `.back-*` 的实际取值。 */
data class CardBackStyle(
    val id: String,
    val bg: Color,
    val ink: Color,
)

object DecoCatalog {

    val styles: Map<String, CardStyle> = listOf(
        CardStyle("nebula", "星群色", Color(0xFF241A3F), Color(0xFF0A0C13), Color(0xFF6E5DFF)),
        CardStyle("aurora", "极光", Color(0xFF0E3B33), Color(0xFF06120F), Color(0xFF3BE0A8)),
        CardStyle("nova", "超新星", Color(0xFF3A1030), Color(0xFF120611), Color(0xFFFF5FB4)),
        CardStyle("chart", "星图", Color(0xFF0B1630), Color(0xFF060B18), Color(0xFF4DB4FF)),
        CardStyle("eclipse", "日食", Color(0xFF141018), Color(0xFF050409), Color(0xFFFFC46B)),
        CardStyle("comet", "彗星", Color(0xFF0F2A44), Color(0xFF06101A), Color(0xFF66E0FF)),
        CardStyle("rosette", "玫瑰星云", Color(0xFF3A1226), Color(0xFF140711), Color(0xFFFF8FC7)),
        CardStyle(
            "polaroid", "拍立得", Color(0xFFEDE7DB), Color(0xFFC9C2B2), Color(0xFFFFFFFF),
            ink = Color(0xFF1A1712), subInk = Color(0xFF4A443A), frame = Color(0xFFB9B1A0), light = true,
        ),
        CardStyle("gilded", "鎏金", Color(0xFF2A2113), Color(0xFF120C06), Color(0xFFE8C983)),
        CardStyle("film", "胶片", Color(0xFF141414), Color(0xFF050505), Color(0xFFB9BEC6)),
        CardStyle("clear", "透卡", Color(0xFF22303F), Color(0xFF0C141C), Color(0xFF9FD8FF)),
        CardStyle("ticket", "镭射票", Color(0xFF1A1230), Color(0xFF0A0716), Color(0xFFC8A6FF)),
        CardStyle(
            "shikishi", "色纸", Color(0xFFF0E3CF), Color(0xFFD9C8AE), Color(0xFFFFF3DE),
            ink = Color(0xFF241C12), subInk = Color(0xFF5A4A34), frame = Color(0xFFC2AE8C), light = true,
        ),
        CardStyle("acrylic", "亚克力", Color(0xFF10303A), Color(0xFF06141A), Color(0xFF7FF0E0)),
    ).associateBy { it.id }

    val default: CardStyle = styles["nebula"]!!

    val backs: Map<String, CardBackStyle> = listOf(
        CardBackStyle("nebula", Color(0xFF0B0D15), Color(0xFFF3F4F8)),
        CardBackStyle("crest", Color(0xFF0E0B06), Color(0xFFF3D38F)),
        CardBackStyle("grid", Color(0xFF0B1630), Color(0xFF8CC8FF)),
        CardBackStyle("seigaiha", Color(0xFF0D1430), Color(0xFF9FB6FF)),
        CardBackStyle("airmail", Color(0xFF12131A), Color(0xFFF3F4F8)),
    ).associateBy { it.id }

    val finishes: Map<String, String> = mapOf(
        "paper" to "纸质",
        "glitter" to "闪粉",
        "holo" to "镭射",
        "gold" to "烫金",
    )

    val wearNames: List<String> = listOf("全新", "轻微磨损", "明显磨损", "严重磨损", "只剩图标")

    fun style(id: String?): CardStyle = id?.let { styles[it] } ?: default

    fun back(id: String?): CardBackStyle = id?.let { backs[it] } ?: backs["nebula"]!!

    /** 从卡片 JSON 里读 deco；没有就返回 null。 */
    fun parse(deco: JsonObject?): CardDeco? {
        if (deco == null) return null
        val spots = (deco["spots"] as? JsonArray)?.mapNotNull { el ->
            val arr = el as? JsonArray ?: return@mapNotNull null
            fun at(i: Int): Float = arr.getOrNull(i)?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
            if (arr.size < 4) null else DecoSpot(at(0), at(1), at(2), at(3))
        }.orEmpty()
        return CardDeco(
            face = deco.strOrNull("face"),
            back = deco.strOrNull("back"),
            sleeve = deco.strOrNull("sleeve"),
            spots = spots,
        )
    }

    private fun JsonObject.strOrNull(key: String): String? =
        (this[key] as? JsonElement)?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
}
