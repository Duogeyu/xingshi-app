package app.shixing.client.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * 一张贴纸。网页的 spots 每一项是
 * `[序号, 贴纸 id, 工艺, 磨损, 随机种子, [x%, y%, 旋转角, 尺寸%]]`。
 */
data class DecoSpot(
    val id: String? = null,
    val fin: String? = null,
    val wear: Float = 0f,
    val seed: Int = 0,
    val x: Float = 0f,
    val y: Float = 0f,
    val rot: Float = 0f,
    val size: Float = 0f,
)

/** 一张卡自带的装扮，字段名跟网页一致。 */
data class CardDeco(
    val face: String? = null,
    val back: String? = null,
    val sleeve: String? = null,
    val spots: List<DecoSpot> = emptyList(),
)

/**
 * 卡面档位。颜色不是拍脑袋配的，是对着网页 css 里 `.face-<id>` 的
 * `background` 与 `.mt-mc-frame` 的 `padding` / `linear-gradient` 抄下来的。
 */
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
    /** 内框那圈渐变（网页 `.mt-mc-frame` 的 background）。 */
    val frameColors: List<Color> = listOf(
        Color(0xFF6D4712), Color(0xFFFFE9B0), Color(0xFFB07F26),
        Color(0xFFFFF6D8), Color(0xFFA26F1C),
    ),
    /** 内框粗细（网页 `.mt-mc-frame` 的 padding）。 */
    val framePad: Float = 2f,
    /** 星名是否用渐变字（鎏金/胶片/镭射票是）。 */
    val nameColors: List<Color>? = null,
    /** 卡面上的光效层，取值见 Fx。 */
    val fx: Fx = Fx.NONE,
) {
    enum class Fx { NONE, NOVA, ECLIPSE, AURORA, COMET, CHART, ROSETTE }
}

/**
 * 卡背风格：网页 CSS 里 `.back-*` 的实际取值。
 * 五种卡背各有自己的底纹（斜纹金线 / 蓝格 / 青海波 / 航空边）与内框。
 */
data class CardBackStyle(
    val id: String,
    val bg: Color,
    val ink: Color,
    val frameColors: List<Color> = listOf(
        Color(0xFF6D4712), Color(0xFFFFE9B0), Color(0xFFB07F26), Color(0xFFFFF6D8),
    ),
    val framePad: Float = 1.2f,
    val pattern: Pattern = Pattern.NONE,
) {
    enum class Pattern { NONE, CREST, GRID, SEIGAIHA, AIRMAIL }
}

/** 卡套外观：塑料套底 + 一圈描边 + （可选）镭射边/花纹。 */
data class SleeveStyle(
    val id: String,
    val name: String,
    val fill: Color,
    val ring: Color,
    val gloss: List<Color> = listOf(Color(0x22FFFFFF), Color(0x08FFFFFF)),
    val pattern: Pattern = Pattern.NONE,
    val foil: Boolean = false,
) {
    enum class Pattern { NONE, STARS, CHART, WALLS }
}

object DecoCatalog {

    private val GOLD = listOf(
        Color(0xFF6D4712), Color(0xFFFFE9B0), Color(0xFFB07F26),
        Color(0xFFFFF6D8), Color(0xFFA26F1C), Color(0xFFFFDF96),
    )

    val styles: Map<String, CardStyle> = listOf(
        CardStyle(
            "nebula", "星群色", Color(0xFF241A3F), Color(0xFF0A0C13), Color(0xFF6E5DFF),
            frameColors = listOf(Color(0xFF7B6BFF), Color(0xFFD9D2FF), Color(0xFF5B4BD6)),
            framePad = 2f,
        ),
        CardStyle(
            "aurora", "极光", Color(0xFF0E3B33), Color(0xFF06120F), Color(0xFF3BE0A8),
            frameColors = listOf(Color(0xFF7CF5C4), Color(0xFF6EC6FF), Color(0xFFB78CFF), Color(0xFFFF6B9A)),
            framePad = 2f,
            fx = CardStyle.Fx.AURORA,
        ),
        CardStyle(
            "nova", "超新星", Color(0xFF3A1208), Color(0xFF120B05), Color(0xFFFFB547),
            frame = Color(0xFFFFCE7A), frameColors = GOLD, framePad = 3.5f,
            fx = CardStyle.Fx.NOVA,
        ),
        CardStyle(
            "chart", "星图", Color(0xFF0A182B), Color(0xFF050D18), Color(0xFF4DB4FF),
            frame = Color(0xFF9BE4FF),
            frameColors = listOf(Color(0xFF78DCFF), Color(0xFFE6FBFF), Color(0xFF4DB4FF)),
            framePad = 1.5f,
            fx = CardStyle.Fx.CHART,
        ),
        CardStyle(
            "eclipse", "日食", Color(0xFF0B0B10), Color(0xFF040406), Color(0xFFE1E6FF),
            frame = Color(0xFFE8EAF5),
            frameColors = listOf(Color(0xFF5B5E69), Color(0xFFF6F7FB), Color(0xFF8A8E9A), Color(0xFFFFFFFF)),
            framePad = 2f,
            fx = CardStyle.Fx.ECLIPSE,
        ),
        CardStyle(
            "comet", "彗星", Color(0xFF0A1626), Color(0xFF050A14), Color(0xFF7FB8FF),
            frame = Color(0xFFBEE1FF),
            frameColors = listOf(Color(0xFFF2FAFF), Color(0xFF8CC2FF), Color(0xFFF4C9FF), Color(0xFFE8F5FF)),
            framePad = 1.5f,
            fx = CardStyle.Fx.COMET,
        ),
        CardStyle(
            "rosette", "玫瑰星云", Color(0xFF2A0C18), Color(0xFF110509), Color(0xFFFF6F9C),
            frame = Color(0xFFFFC4B4),
            frameColors = listOf(Color(0xFF6A382C), Color(0xFFFFD8C6), Color(0xFFAD6853), Color(0xFFFFF1E9)),
            framePad = 2.5f,
            fx = CardStyle.Fx.ROSETTE,
        ),
        CardStyle(
            "polaroid", "拍立得", Color(0xFF1B1B2A), Color(0xFF0E0E18), Color(0xFFE6E9FF),
            ink = Color(0xFFF3F4F8), subInk = Color(0xFFD8D2E4), frame = Color(0xFFFFFFFF),
            frameColors = listOf(
                Color(0xFFFFFFFF), Color(0xFFFFD6EA), Color(0xFFF6F3FF),
                Color(0xFFC4FAE4), Color(0xFFCBE3FF), Color(0xFFFFFFFF),
            ),
            framePad = 9f,
        ),
        CardStyle(
            "gilded", "鎏金", Color(0xFF3A2A12), Color(0xFF120C06), Color(0xFFE8C983),
            frame = Color(0xFFFFDF96), frameColors = GOLD, framePad = 4f,
            nameColors = listOf(
                Color(0xFFFFF4D6), Color(0xFFF2C768), Color(0xFFFFF8E6),
                Color(0xFFD9A441), Color(0xFFFFF0C4),
            ),
        ),
        CardStyle(
            "film", "胶片", Color(0xFF241A10), Color(0xFF14110D), Color(0xFFFF963C),
            frame = Color(0xFFFFB468),
            frameColors = listOf(Color(0xFFFFB468), Color(0xFF6B4A2A), Color(0xFFFFB468)),
            framePad = 1.5f,
            nameColors = listOf(Color(0xFFFFF6EA), Color(0xFFFFD29A), Color(0xFFFFF6EA), Color(0xFFFFC27D)),
        ),
        CardStyle(
            "clear", "透卡", Color(0xFF22303F), Color(0xFF0A0C14), Color(0xFFBED7FF),
            frame = Color(0xFFE6F1FF),
            frameColors = listOf(Color(0xCCFFFFFF), Color(0x22FFFFFF), Color(0x66D2E6FF), Color(0x33FFFFFF)),
            framePad = 6f,
        ),
        CardStyle(
            "ticket", "镭射票", Color(0xFF1A1230), Color(0xFF0D0D14), Color(0xFFE6C8FF),
            frame = Color(0xFFF6D6FF),
            frameColors = listOf(
                Color(0xFFF6D6FF), Color(0xFFC9F3FF), Color(0xFFFFF6C9),
                Color(0xFFFFD1E8), Color(0xFFD3FFE9),
            ),
            framePad = 2f,
            nameColors = listOf(
                Color(0xFFFFFFFF), Color(0xFFF6D6FF), Color(0xFFC9F3FF),
                Color(0xFFFFF6C9), Color(0xFFFFFFFF),
            ),
        ),
        CardStyle(
            "shikishi", "色纸", Color(0xFF241C12), Color(0xFF14100A), Color(0xFFF2D58A),
            ink = Color(0xFFF6EFE0), subInk = Color(0xFFD8CBB4), frame = Color(0xFFF7E2A4),
            frameColors = listOf(Color(0xFF8A6418), Color(0xFFF7E2A4), Color(0xFFB8892C), Color(0xFFFFF3CC)),
            framePad = 2f,
        ),
        CardStyle(
            "acrylic", "亚克力", Color(0xFF10303A), Color(0xFF0C0E16), Color(0xFFB4D2FF),
            frame = Color(0xFFFFFFFF),
            frameColors = listOf(Color(0x99FFFFFF), Color(0x33C8DCFF), Color(0x99FFFFFF)),
            framePad = 1.5f,
        ),
    ).associateBy { it.id }

    val default: CardStyle = styles["nebula"]!!

    val backs: Map<String, CardBackStyle> = listOf(
        CardBackStyle("nebula", Color(0xFF0B0D15), Color(0xFFF3F4F8)),
        CardBackStyle(
            "crest", Color(0xFF0E0B06), Color(0xFFF3D38F),
            pattern = CardBackStyle.Pattern.CREST,
        ),
        CardBackStyle(
            "grid", Color(0xFF0B1630), Color(0xFF8CC8FF),
            frameColors = listOf(Color(0x6678C8FF), Color(0xCCD6EEFF), Color(0x6678C8FF)),
            framePad = 1f,
            pattern = CardBackStyle.Pattern.GRID,
        ),
        CardBackStyle(
            "seigaiha", Color(0xFF0D1430), Color(0xFF9FB6FF),
            frameColors = listOf(Color(0x669FB6FF), Color(0xCCDCE6FF), Color(0x669FB6FF)),
            framePad = 1f,
            pattern = CardBackStyle.Pattern.SEIGAIHA,
        ),
        CardBackStyle(
            "airmail", Color(0xFF12131A), Color(0xFFF3F4F8),
            pattern = CardBackStyle.Pattern.AIRMAIL,
        ),
    ).associateBy { it.id }

    /** 卡套：网页 `.mt-sleeve` 的基础是半透明塑料套，再加一个变体色/花纹。 */
    val sleeves: Map<String, SleeveStyle> = listOf(
        SleeveStyle("brand", "品牌款", Color(0x1AFFFFFF), Color(0x66FFFFFF), listOf(Color(0x22FFFFFF), Color(0x08FFFFFF))),
        SleeveStyle("launch", "首发金", Color(0x1AFFD479), Color(0xCCFFE4A0), listOf(Color(0x33FFD479), Color(0x0AFFD479))),
        SleeveStyle("launchb", "首发银", Color(0x1AF4F7FF), Color(0xCCF4F7FF), listOf(Color(0x33E6ECFF), Color(0x0AE6ECFF))),
        SleeveStyle("stars", "星空", Color(0x14FFFFFF), Color(0x80FFFFFF), listOf(Color(0x26FFFFFF), Color(0x0AFFFFFF)), pattern = SleeveStyle.Pattern.STARS),
        SleeveStyle("chart", "星图", Color(0x1478C8FF), Color(0x8078C8FF), listOf(Color(0x2678C8FF), Color(0x0878C8FF)), pattern = SleeveStyle.Pattern.CHART),
        SleeveStyle("walls", "墙纸", Color(0x14FFFFFF), Color(0x70FFFFFF), listOf(Color(0x22FFFFFF), Color(0x08FFFFFF)), pattern = SleeveStyle.Pattern.WALLS),
        SleeveStyle("light", "微光", Color(0x1AFFFFFF), Color(0x99FFFFFF), listOf(Color(0x3DFFFFFF), Color(0x0AFFFFFF))),
        SleeveStyle("tint", "染色", Color(0x1AD69BFF), Color(0x80D69BFF), listOf(Color(0x2ED69BFF), Color(0x0AD69BFF))),
        SleeveStyle("foil", "镭射边", Color(0x14FFFFFF), Color(0x99FFFFFF), listOf(Color(0x33FF7AD9), Color(0x0A7AD0FF)), foil = true),
        SleeveStyle("print", "印刷", Color(0x14FFFFFF), Color(0x70FFFFFF), listOf(Color(0x22FFFFFF), Color(0x08FFFFFF))),
        SleeveStyle("glare", "炫光", Color(0x1AFFFFFF), Color(0xAAFFFFFF), listOf(Color(0x4DFFFFFF), Color(0x0AFFFFFF))),
    ).associateBy { it.id }

    fun sleeve(id: String?): SleeveStyle? = id?.let { sleeves[it] }

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
        // 每一项形如 [序号, 贴纸 id, 工艺, 磨损, 种子?, [x%, y%, 旋转角, 尺寸%]]
        // 种子可能没有，所以不按固定下标取，按「字符串 / 数字 / 内层数组」分类来读。
        val spots = (deco["spots"] as? JsonArray)?.mapNotNull { el ->
            val arr = el as? JsonArray ?: return@mapNotNull null
            val box = arr.filterIsInstance<JsonArray>().lastOrNull() ?: return@mapNotNull null
            if (box.size < 4) return@mapNotNull null
            fun f(i: Int) = box.getOrNull(i)?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f

            val strings = arr.filterIsInstance<JsonPrimitive>()
                .filter { it.isString }
                .map { it.content }
            val numbers = arr.filterIsInstance<JsonPrimitive>()
                .filter { !it.isString && it.content.toFloatOrNull() != null }

            DecoSpot(
                id = strings.getOrNull(0),
                fin = strings.getOrNull(1),
                wear = numbers.getOrNull(1)?.content?.toFloatOrNull() ?: 0f,
                seed = numbers.getOrNull(2)?.content?.toIntOrNull() ?: 0,
                x = f(0), y = f(1), rot = f(2), size = f(3),
            )
        }.orEmpty()
        return CardDeco(
            face = (deco["face"] as? JsonElement)?.jsonPrimitive?.content,
            back = (deco["back"] as? JsonElement)?.jsonPrimitive?.content,
            sleeve = (deco["sleeve"] as? JsonElement)?.jsonPrimitive?.content,
            spots = spots,
        )
    }
}