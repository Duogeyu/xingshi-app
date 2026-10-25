package app.shixing.client.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shixing.client.data.CardDeco
import app.shixing.client.data.CardScores
import app.shixing.client.data.CardStyle
import app.shixing.client.data.DecoCatalog
import app.shixing.client.data.Groups
import app.shixing.client.data.MyCard
import app.shixing.client.data.StarCard
import coil.compose.AsyncImage

/** 卡面要显示的东西——自己和其他星都用同一套。 */
data class CardFace(
    val name: String,
    val star: Int?,
    val line: String?,
    val group: String?,
    val verified: Boolean = false,
    val scores: CardScores? = null,
    /** 卡面上的编号：自己的用 star，别人的用 n。 */
    val no: Int? = null,
    /** 网页给的装扮：卡面档位 / 卡背 / 卡套 / 贴纸位置。 */
    val deco: CardDeco? = null,
    /** 对方有照片（能不能显示还得看有没有取到链接）。 */
    val hasPhoto: Boolean = false,
    /** 网页接口给的「你们的共同点」。 */
    val common: List<String> = emptyList(),
)

fun MyCard.toFace(): CardFace = CardFace(
    name = name,
    star = star,
    line = line,
    group = group,
    verified = true,
    scores = scores,
    no = star,
    deco = DecoCatalog.parse(deco),
    hasPhoto = photo,
    common = emptyList(),
)

fun StarCard.toFace(): CardFace = CardFace(
    name = name ?: "一颗星",
    star = star,
    line = line,
    group = group,
    verified = verified,
    scores = null,
    no = star ?: n.toInt(),
    deco = DecoCatalog.parse(deco),
    hasPhoto = photo,
    common = common,
)

private val GOLD = Color(0xFFE8C983)

@Composable
fun StarCardFront(
    face: CardFace,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    faceLabel: String? = null,
) {
    val shape = RoundedCornerShape(26.dp)
    val st = DecoCatalog.style(face.deco?.face)
    // 网页的内框是一圈金属/镭射渐变（.mt-mc-frame），厚度各档不同
    val ring = Brush.linearGradient(
        st.frameColors,
        start = Offset.Zero,
        end = Offset(700f, 1050f),
    )

    Box(
        modifier
            .aspectRatio(0.70f)
            .clip(shape)
            .background(Color(0xFF0A0C13))
            .border((st.framePad + 1.2f).dp, ring, shape),
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(st.top, st.bottom),
                            start = Offset.Zero,
                            end = Offset(900f, 1400f),
                        ),
                    ),
            )
            // 卡面档位自带的几团柔光（网页是 5 个点位，这里取同样位置的三团做主视觉）
            listOf(
                Triple(0.86f, 0.10f, 0.55f),
                Triple(0.08f, 0.46f, 0.42f),
                Triple(0.78f, 0.82f, 0.5f),
            ).forEach { (px, py, a) ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(st.glow.copy(alpha = a * 0.5f), Color.Transparent),
                                center = Offset(px * 900f, py * 1400f),
                                radius = 620f,
                            ),
                        ),
                )
            }
            if (st.light) {
                Box(Modifier.fillMaxSize().background(Color(0x0DFFFFFF)))
            }
            // 斜向高光：网页卡面都有一层很淡的镜面感
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (st.light) 0.18f else 0.07f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.03f),
                                Color.Transparent,
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1100f, 1700f),
                        ),
                    ),
            )
            // 四角压暗，让中间的主体更集中
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = if (st.light) 0.10f else 0.34f),
                            ),
                            center = Offset(450f, 620f),
                            radius = 1150f,
                        ),
                    ),
            )
        }

        // 各档位自带的贴面光效（网页是 .mt-face-fx 那层 screen 混合）
        StyleFx(st.fx)

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.34f to Color.Transparent,
                        0.62f to (if (st.light) Color(0x33FFFFFF) else Color(0x9905060A)),
                        1f to (if (st.light) Color(0xE6FFFFFF) else Color(0xF205060A)),
                    ),
                ),
        )

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = (13 + st.framePad).dp, end = (13 + st.framePad).dp, top = (10 + st.framePad).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "VRCHAT 中文社区 · 星遇同频",
                    fontSize = 8.sp,
                    color = st.frame,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                (face.no ?: face.star)?.let { n ->
                    Text(
                        "No." + n.toString().padStart(5, '0'),
                        fontSize = 15.sp,
                        color = st.frame,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (faceLabel != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        faceLabel,
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC0B0D14))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 8.sp,
                        color = GOLD,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(Modifier.padding(start = (15 + st.framePad).dp, end = (15 + st.framePad).dp, bottom = (14 + st.framePad).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "来自" + GroupName.of(face.group),
                        fontSize = 11.sp,
                        color = st.frame,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (face.verified) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "✓ 已验证",
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x334DB4FF))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            color = Color(0xFF8FCBFF),
                        )
                    }
                }
                if (st.nameColors != null) {
                    Text(
                        face.name,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.linearGradient(st.nameColors),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text(
                        face.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = st.ink,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                face.line?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = st.subInk,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                face.scores?.let { scores -> ScoreBar(scores, Modifier.padding(top = 12.dp)) }
            }
        }
    }
}

@Composable
private fun ScoreBar(scores: CardScores, modifier: Modifier = Modifier) {
    val total = scores.pairs().sumOf { it.second.coerceAtLeast(0) }.coerceAtLeast(1)
    val ordered = scores.pairs().sortedByDescending { it.second }

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
        ) {
            ordered.forEach { (group, value) ->
                Box(
                    Modifier
                        .weight(value.coerceAtLeast(1).toFloat())
                        .height(5.dp)
                        .background(Color(Groups.color(group))),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ordered.forEach { (group, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(Groups.color(group))),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${Groups.label(group)} ${value * 100 / total}%",
                        fontSize = 10.sp,
                        color = Color(0xFFEDEBE3),
                    )
                }
            }
        }
    }
}

/** 背面：四维（自己有）+ 介绍 + 关于我 + 底部按钮。 */
@Composable
fun StarCardBack(
    face: CardFace,
    modifier: Modifier = Modifier,
    intro: String? = null,
    about: List<Pair<String?, String?>> = emptyList(),
    footer: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(26.dp)
    val st = DecoCatalog.style(face.deco?.face)
    val bk = DecoCatalog.back(face.deco?.back)
    val ring = Brush.linearGradient(listOf(st.frame, st.frame.copy(alpha = 0.45f)))

    Column(
        modifier
            .aspectRatio(0.70f)
            .clip(shape)
            .background(bk.bg)
            .border(2.dp, ring, shape)
            .padding(18.dp),
    ) {
        (face.no ?: face.star)?.let { n ->
            Text(
                "No." + n.toString().padStart(5, '0'),
                fontSize = 12.sp,
                color = bk.ink.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            face.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = bk.ink,
            modifier = Modifier.padding(top = 4.dp),
        )
        face.line?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                fontSize = 11.sp,
                color = MaterialTheme_color.ink2,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        face.scores?.let { scores ->
            Spacer(Modifier.height(12.dp))
            scores.pairs().forEach { (group, value) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Groups.label(group),
                        fontSize = 11.sp,
                        color = MaterialTheme_color.ink2,
                        modifier = Modifier.width(36.dp),
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme_color.card2),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(Color(Groups.color(group)), RoundedCornerShape(3.dp)),
                        )
                    }
                    Text(
                        "$value",
                        fontSize = 11.sp,
                        color = MaterialTheme_color.ink,
                        modifier = Modifier
                            .width(32.dp)
                            .padding(start = 6.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        intro?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, fontSize = 12.sp, color = MaterialTheme_color.ink2, lineHeight = 18.sp)
        }

        if (face.common.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "你们的共同点",
                fontSize = 11.sp,
                color = st.frame,
                fontWeight = FontWeight.SemiBold,
            )
            face.common.take(3).forEach { line ->
                Row(Modifier.padding(top = 5.dp)) {
                    Text("·", fontSize = 12.sp, color = st.frame)
                    Spacer(Modifier.width(5.dp))
                    Text(line, fontSize = 11.sp, color = MaterialTheme_color.ink2, lineHeight = 16.sp)
                }
            }
        }

        about.take(3).forEach { (q, a) ->
            if (!q.isNullOrBlank() || !a.isNullOrBlank()) {
                Column(Modifier.padding(top = 8.dp)) {
                    if (!q.isNullOrBlank()) {
                        Text("· $q", fontSize = 10.sp, color = MaterialTheme_color.ink3)
                    }
                    Text(a.orEmpty(), fontSize = 12.sp, color = MaterialTheme_color.ink, lineHeight = 18.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        footer?.invoke()
    }
}

/** 一张能翻面的星卡：默认显示正面，点一下翻到背面。 */
@Composable
fun FlippableStarCard(
    face: CardFace,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    faceLabel: String? = null,
    intro: String? = null,
    about: List<Pair<String?, String?>> = emptyList(),
    footer: (@Composable () -> Unit)? = null,
) {
    var flipped by remember(face.star) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(420),
        label = "flip",
    )

    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { flipped = !flipped },
    ) {
        if (rotation <= 90f) {
            StarCardFront(face, photoUrl, Modifier.fillMaxWidth(), faceLabel)
        } else {
            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                StarCardBack(
                    face = face,
                    modifier = Modifier.fillMaxWidth(),
                    intro = intro,
                    about = about,
                    footer = footer,
                )
            }
        }
    }
}

/** 全屏的「我的星卡」页（现在「我的」页里直接用卡片本身）。 */
@Composable
fun MyCardScreen(
    card: MyCard?,
    photoUrl: String?,
    faceLabel: String?,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme_color.bg)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "返回",
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme_color.card2)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = MaterialTheme_color.ink,
            )
            Spacer(Modifier.weight(1f))
            Text("点卡片翻面", fontSize = 11.sp, color = MaterialTheme_color.ink3)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (card == null) {
                Text("星卡还没拿到", fontSize = 13.sp, color = MaterialTheme_color.ink3)
            } else {
                FlippableStarCard(
                    face = card.toFace(),
                    photoUrl = photoUrl,
                    modifier = Modifier.fillMaxWidth(),
                    faceLabel = faceLabel,
                    intro = card.intro,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** 群组的中文名（网页里「来自聊天星群」那个）。 */
object GroupName {
    fun of(group: String?): String = when (group) {
        "G" -> "游戏星群"
        "W" -> "漫游星群"
        "Z" -> "创作星群"
        "L" -> "聊天星群"
        "A" -> "万象星群"
        else -> "星星"
    }
}

/** 各档位卡面的贴面光效，对应网页 `.mt-face-fx` 那一层。 */
@Composable
private fun StyleFx(fx: CardStyle.Fx) {
    if (fx == CardStyle.Fx.NONE) return
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.33f
        when (fx) {
            CardStyle.Fx.NOVA -> {
                val rays = 72
                for (i in 0 until rays) {
                    val a = (i / rays.toFloat()) * 6.28318f
                    val len = w * 0.66f
                    drawLine(
                        color = Color(0xFFFFD27A).copy(alpha = if (i % 2 == 0) 0.20f else 0.10f),
                        start = Offset(cx, cy),
                        end = Offset(cx + kotlin.math.cos(a) * len, cy + kotlin.math.sin(a) * len * 1.5f),
                        strokeWidth = if (i % 2 == 0) 3f else 1.4f,
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFF6DC), Color(0x66FFC46B), Color(0x00FFC46B)),
                        center = Offset(cx, cy),
                        radius = w * 0.22f,
                    ),
                    radius = w * 0.22f,
                    center = Offset(cx, cy),
                )
            }

            CardStyle.Fx.ECLIPSE -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x00FFFFFF), Color(0x33FFF0DC), Color(0x00FFE0B8)),
                        center = Offset(cx, cy),
                        radius = w * 0.42f,
                    ),
                    radius = w * 0.42f,
                    center = Offset(cx, cy),
                )
                drawCircle(Color(0xE6FFF6E6), radius = w * 0.235f, center = Offset(cx, cy), style = Stroke(width = 2.2f))
                drawCircle(Color(0x55FFE7C4), radius = w * 0.265f, center = Offset(cx, cy), style = Stroke(width = 6f))
            }

            CardStyle.Fx.AURORA -> {
                drawRect(
                    Brush.linearGradient(
                        listOf(
                            Color(0x007CF5C4), Color(0x667CF5C4), Color(0x006EC6FF), Color(0x55B78CFF), Color(0x00FF6B9A),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h * 0.9f),
                    ),
                )
            }

            CardStyle.Fx.COMET -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFFFFF), Color(0x88C8EBFF), Color(0x00A0D7FF)),
                        center = Offset(w * 0.76f, h * 0.15f),
                        radius = w * 0.16f,
                    ),
                    radius = w * 0.16f,
                    center = Offset(w * 0.76f, h * 0.15f),
                )
                drawLine(
                    color = Color(0x99E6F6FF),
                    start = Offset(w * 0.76f, h * 0.15f),
                    end = Offset(w * 0.10f, h * 0.62f),
                    strokeWidth = 2.5f,
                )
                drawLine(
                    color = Color(0x44E6F6FF),
                    start = Offset(w * 0.78f, h * 0.17f),
                    end = Offset(w * 0.18f, h * 0.58f),
                    strokeWidth = 7f,
                )
            }

            CardStyle.Fx.CHART -> {
                val step = 11f
                var x = 0f
                while (x < w) {
                    drawLine(Color(0x558CD8FF), Offset(x, 0f), Offset(x, h * 0.045f), 1.2f)
                    drawLine(Color(0x558CD8FF), Offset(x, h), Offset(x, h * 0.955f), 1.2f)
                    x += step
                }
                var y = 0f
                while (y < h) {
                    drawLine(Color(0x558CD8FF), Offset(0f, y), Offset(w * 0.045f, y), 1.2f)
                    drawLine(Color(0x558CD8FF), Offset(w, y), Offset(w * 0.955f, y), 1.2f)
                    y += step
                }
            }

            CardStyle.Fx.ROSETTE -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x55FF8FB8), Color(0x22C42A66), Color(0x00FF6F9C)),
                        center = Offset(cx, cy + h * 0.05f),
                        radius = w * 0.55f,
                    ),
                    radius = w * 0.55f,
                    center = Offset(cx, cy + h * 0.05f),
                )
            }

            else -> Unit
        }
    }
}