package app.shixing.client.data

import kotlinx.serialization.Serializable

/** `connect` 与星轨等接口里通用的「我」：点亮编号 + VRChat 名字。 */
@Serializable
data class Me(
    val star: Int? = null,
    val vrc: String? = null,
)

/** 地图榜 / 查地图：`{"board":"maps","min":5,"maps":[...],"at":...}` */
@Serializable
data class Boards(
    val board: String? = null,
    val min: Int? = null,
    val maps: List<BoardMap> = emptyList(),
    val at: Long? = null,
)

@Serializable
data class BoardMap(
    val world: String,
    val name: String? = null,
    val author: String? = null,
    val avg: Double? = null,
    val raters: Int? = null,
    val rank: Int? = null,
    val weight: String? = null,
    val page: String? = null,
    val img: String? = null,
)

@Serializable
data class ConnectResult(
    val token: String,
    val me: Me? = null,
)

@Serializable
data class NotificationItem(
    val id: Long,
    val title: String? = null,
    val body: String? = null,
    val url: String? = null,
    val at: Long? = null,
)

@Serializable
data class Notifications(
    val last: Long? = null,
    val items: List<NotificationItem> = emptyList(),
    val every: Int? = null,
    val dot: Int? = null,
)

@Serializable
data class Recent(
    val items: List<NotificationItem> = emptyList(),
    val read: Long? = null,
)

/** 星轨相交。`why` 非空表示这一块不该显示或缺前置条件。 */
@Serializable
data class CrossStar(
    val n: Long,
    val name: String? = null,
    val line: String? = null,
    val group: String? = null,
    val star: Int? = null,
    val intro: String? = null,
    val vrc: String? = null,
    val linked: Boolean = false,
    val mutual: Boolean = false,
    val at: Long? = null,
    val fresh: Boolean = false,
)

@Serializable
data class Crossings(
    val stars: List<CrossStar> = emptyList(),
    val n: Int? = null,
    val why: String? = null,
    val me: Me? = null,
)

@Serializable
data class MutualStar(
    val n: Long,
    val name: String? = null,
    val group: String? = null,
    val star: Int? = null,
    val vrc: String? = null,
    val vrcUser: String? = null,
    val at: Long? = null,
)

@Serializable
data class Mutuals(
    val stars: List<MutualStar> = emptyList(),
    val n: Int? = null,
)

/** 「所在的地图」与打分接口返回同一个形状。 */
@Serializable
data class MyMap(
    val world: String,
    val name: String? = null,
    val board: Boolean = false,
    val pending: Boolean = false,
    val off: Boolean = false,
    val avg: Double? = null,
    val raters: Int? = null,
    val mine: Int? = null,
    val can: Boolean = false,
    val need: String? = null,
    val words: String? = null,
    val wordsOk: String? = null,
    val img: String? = null,
) {
    /** 官方要求：`off` 为真时这块不给这位玩家看。 */
    val visible: Boolean get() = !off
}

/** 星群的配色，与网页 `{G,W,Z,L,A}` 对齐。 */
object Groups {
    fun label(g: String?): String = when (g) {
        "G" -> "漫游"
        "W" -> "游戏"
        "Z" -> "创作"
        "L" -> "聊天"
        "A" -> "万象"
        else -> "未知"
    }

    fun color(g: String?): Long = when (g) {
        "G" -> 0xFF4DB4FF
        "W" -> 0xFFFFB547
        "Z" -> 0xFFD69BFF
        "L" -> 0xFF5FE0B4
        "A" -> 0xFFF3F4F8
        else -> 0xFF9AA3B5
    }
}
