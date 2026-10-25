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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shixing.client.data.CardBackStyle
import app.shixing.client.data.CardDeco
import app.shixing.client.data.CardScores
import app.shixing.client.data.CardStyle
import app.shixing.client.data.DecoCatalog
import app.shixing.client.data.Groups
import app.shixing.client.data.MyCard
import app.shixing.client.data.SleeveStyle
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
    val scoresMap: Map<String, Int>? = null,
    val overlap: Int? = null,
    val sprout: Boolean = false,
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
    scoresMap = scores,
    sprout = sprout,
)

private val GOLD = Color(0xFFE8C983)

@Composable
fun StarCardFront(
    face: CardFace,
    photoUrl: String?,
    photoGrid: android.graphics.Bitmap? = null,
    modifier: Modifier = Modifier,
    faceLabel: String? = null,
    maxHeight: androidx.compose.ui.unit.Dp? = null,
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
        (if (maxHeight != null) modifier.heightIn(max = maxHeight) else modifier)
            .aspectRatio(0.75f)
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
        } else if (photoGrid != null) {
            androidx.compose.foundation.Image(
                bitmap = photoGrid.asImageBitmap(),
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

        // 贴纸：位置来自 deco.spots，图形由离屏 WebView 画好再贴上来
        StickerLayer(face.deco?.spots.orEmpty())


        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.28f to Color.Transparent,
                        0.58f to (if (st.light) Color(0x44FFFFFF) else Color(0xCC05060A)),
                        1f to (if (st.light) Color(0xE9FFFFFF) else Color(0xF805060A)),
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
            // 别人的卡：TA 的成分 + 和我的重合度
            face.scoresMap?.let { m ->
                if (face.scores == null) MixBlock(m, face.overlap, Modifier.padding(top = 12.dp))
            }
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
    maxHeight: androidx.compose.ui.unit.Dp? = null,
) {
    val shape = RoundedCornerShape(26.dp)
    val st = DecoCatalog.style(face.deco?.face)
    val bk = DecoCatalog.back(face.deco?.back)
    val ring = Brush.linearGradient(bk.frameColors, start = Offset.Zero, end = Offset(700f, 1050f))

    Box(
        (if (maxHeight != null) modifier.heightIn(max = maxHeight) else modifier)
            .aspectRatio(0.75f)
            .clip(shape)
            .background(bk.bg)
            .border((bk.framePad + 1.2f).dp, ring, shape),
    ) {
        // 卡背底纹：斜纹金线 / 蓝格 / 青海波 / 航空边
        BackPattern(bk.pattern, bk.bg)

    Column(
        Modifier
            .fillMaxSize()
            .padding((17 + bk.framePad).dp),
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
}
/** 一张能翻面的星卡：默认显示正面，点一下翻到背面。 */
@Composable
fun FlippableStarCard(
    face: CardFace,
    photoUrl: String?,
    photoGrid: android.graphics.Bitmap? = null,
    modifier: Modifier = Modifier,
    faceLabel: String? = null,
    intro: String? = null,
    about: List<Pair<String?, String?>> = emptyList(),
    footer: (@Composable () -> Unit)? = null,
    maxHeight: androidx.compose.ui.unit.Dp? = null,
) {
    var flipped by remember(face.star) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(420),
        label = "flip",
    )
    val sleeve = DecoCatalog.sleeve(face.deco?.sleeve)
    val pad = 0.dp

    Box(modifier) {
        // 卡本身铺满整格（和没卡套的卡一样大），卡套是一层盖在上面的塑料
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { flipped = !flipped },
            contentAlignment = Alignment.TopCenter,
        ) {
            if (rotation <= 90f) {
                StarCardFront(face, photoUrl, photoGrid, Modifier.fillMaxWidth(), faceLabel, maxHeight)
            } else {
                Box(Modifier.graphicsLayer { rotationY = 180f }) {
                    StarCardBack(
                        face = face,
                        modifier = Modifier.fillMaxWidth(),
                        intro = intro,
                        about = about,
                        footer = footer,
                        maxHeight = maxHeight,
                    )
                }
            }
        }
    }
}

/** 卡套的外壳：铺满整格，卡从里面垫出去一圈。 */

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
/** 卡背底纹。 */
@Composable
private fun BackPattern(pattern: CardBackStyle.Pattern, bg: Color) {
    if (pattern == CardBackStyle.Pattern.NONE) return
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (pattern) {
            CardBackStyle.Pattern.CREST -> {
                val step = w * 0.05f
                var x = -h
                while (x < w + h) {
                    drawLine(Color(0x12FFD68C), Offset(x, 0f), Offset(x + h, h), 1.2f)
                    drawLine(Color(0x12FFD68C), Offset(x, h), Offset(x + h, 0f), 1.2f)
                    x += step
                }
            }

            CardBackStyle.Pattern.GRID -> {
                val big = w / 4f
                val small = w / 20f
                var x = 0f
                while (x < w) {
                    drawLine(Color(0x2878BEFF), Offset(x, 0f), Offset(x, h), 1f)
                    x += small
                }
                var y = 0f
                while (y < h) {
                    drawLine(Color(0x2878BEFF), Offset(0f, y), Offset(w, y), 1f)
                    y += small
                }
                x = 0f
                while (x < w) {
                    drawLine(Color(0x5578BEFF), Offset(x, 0f), Offset(x, h), 1.6f)
                    x += big
                }
                y = 0f
                while (y < h) {
                    drawLine(Color(0x5578BEFF), Offset(0f, y), Offset(w, y), 1.6f)
                    y += big
                }
            }

            CardBackStyle.Pattern.SEIGAIHA -> {
                val r = w * 0.12f
                val dx = r * 1.6f
                val dy = r * 0.8f
                var row = 0
                var y = h * 0.2f
                while (y < h + r) {
                    var x = if (row % 2 == 0) -r else 0f
                    while (x < w + r) {
                        drawCircle(Color(0x18FFFFFF), r, Offset(x, y), style = Stroke(1.4f))
                        drawCircle(Color(0x10FFFFFF), r * 0.72f, Offset(x, y), style = Stroke(1.4f))
                        drawCircle(Color(0x0AFFFFFF), r * 0.44f, Offset(x, y), style = Stroke(1.4f))
                        x += dx
                    }
                    y += dy
                    row++
                }
            }

            CardBackStyle.Pattern.AIRMAIL -> {
                // 网页是 2.6cqw 的一圈细条纹边
                val band = w * 0.026f
                val colors = listOf(Color(0xFFE0464E), Color(0xFFF4EFE6), Color(0xFF3563B8), Color(0xFFF4EFE6))
                var i = 0
                var x = -band * 2
                while (x < w + band * 2) {
                    val c = colors[i % colors.size]
                    drawLine(c, Offset(x, 0f), Offset(x + band * 1.4f, band), band * 0.95f)
                    drawLine(c, Offset(x, h), Offset(x + band * 1.4f, h - band), band * 0.95f)
                    x += band * 1.4f
                    i++
                }
                i = 0
                var y = -band * 2
                while (y < h + band * 2) {
                    val c = colors[i % colors.size]
                    drawLine(c, Offset(0f, y), Offset(band, y + band * 1.4f), band * 0.95f)
                    drawLine(c, Offset(w, y), Offset(w - band, y + band * 1.4f), band * 0.95f)
                    y += band * 1.4f
                    i++
                }
            }
            else -> Unit
        }
    }
}

/** 卡套：套在卡外面的一层塑料壳。 */
@Composable
private fun SleeveOverlay(style: SleeveStyle, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Box(modifier.fillMaxSize().clip(shape).background(style.fill)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (style.pattern) {
                SleeveStyle.Pattern.STARS -> {
                    var s = 37
                    repeat(46) {
                        s = (s * 1103515245 + 12345) and 0x7FFFFFFF
                        val x = (s % 1000) / 1000f * w
                        s = (s * 1103515245 + 12345) and 0x7FFFFFFF
                        val y = (s % 1000) / 1000f * h
                        drawCircle(Color(0x66FFFFFF), 1.6f, Offset(x, y))
                    }
                }

                SleeveStyle.Pattern.CHART -> {
                    var x = 0f
                    while (x < w) {
                        drawLine(Color(0x2278C8FF), Offset(x, 0f), Offset(x, h), 1f)
                        x += w / 12f
                    }
                    var y = 0f
                    while (y < h) {
                        drawLine(Color(0x2278C8FF), Offset(0f, y), Offset(w, y), 1f)
                        y += h / 16f
                    }
                }

                SleeveStyle.Pattern.WALLS -> {
                    var x = 0f
                    while (x < w) {
                        drawLine(Color(0x14FFFFFF), Offset(x, 0f), Offset(x, h), 2f)
                        x += w / 9f
                    }
                }

                else -> Unit
            }
        }
        // 塑料高光
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        style.gloss,
                        start = Offset(0f, 0f),
                        end = Offset(900f, 1200f),
                    ),
                ),
        )
        if (style.foil) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(
                        3.dp,
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF7AD9), Color(0xFFFFE07A), Color(0xFF7DFFC4),
                                Color(0xFF7AD0FF), Color(0xFFC08BFF), Color(0xFFFF7AD9),
                            ),
                        ),
                        shape,
                    ),
            )
        }
        Box(Modifier.fillMaxSize().border(1.dp, style.ring.copy(alpha = 0.5f), shape))
    }
}
/** 卡套小样：一张卡 + 套在上面的壳，装扮页的格子用。 */
@Composable
fun SleevePreview(style: SleeveStyle, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF10131C))) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2A2440), Color(0xFF0C0E16)))),
        )
        SleeveOverlay(style)
    }
}

/**
 * 「TA 的成分 / 和你重合 N%」——网页版是这一块四条短条加百分比。
 * 数据来自卡片 JSON 的 scores{G,W,Z,L}，重合度是拿我自己的 scores 逐项算的。
 */
@Composable
private fun MixBlock(m: Map<String, Int>, overlap: Int?, modifier: Modifier = Modifier) {
    val order = listOf("L" to "聊天", "Z" to "创作", "G" to "漫游", "W" to "游戏")
    val total = order.sumOf { (k, _) -> m[k] ?: 0 }.coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TA 的成分", fontSize = 12.sp, color = Color(0xCCFFFFFF), fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            if (overlap != null) {
                Text("和你重合 $overlap%", fontSize = 12.sp, color = Color(0xFFF3D38F), fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            order.forEach { (k, _) ->
                val v = (m[k] ?: 0)
                val frac = v.toFloat() / total
                Box(
                    Modifier
                        .weight(if (v <= 0) 0.12f else frac.coerceAtLeast(0.12f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(app.shixing.client.data.Groups.color(k))),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            order.forEach { (k, label) ->
                val pct = Math.round((m[k] ?: 0) * 100f / total)
                Text(
                    "● $label $pct%",
                    fontSize = 9.5.sp,
                    color = Color(0x99FFFFFF),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}


/** 贴纸：位置/大小来自 deco.spots，图形由离屏 WebView 画好再贴上来。 */
@Composable
private fun StickerLayer(spots: List<app.shixing.client.data.DecoSpot>) {
    if (spots.isEmpty()) return
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        spots.forEach { s ->
            val id = s.id ?: return@forEach
            // 文档：摆放是 [x, y, 宽度, 旋转]，x/y 是贴纸中心（卡宽/卡高的百分比），宽度是卡宽的百分比
            val box = w * (s.size / 100f)
            val url = "https://vrcc.cc/api/v1/stickers/" + id +
                "?fin=" + (s.fin ?: "paper") + "&wear=" + s.wear + "&seed=" + s.seed
            val pic by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
                initialValue = null,
                key1 = url,
            ) { value = StickerImages.bitmap(url) }
            pic?.let { bmp ->
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = w * (s.x / 100f) - box / 2, y = h * (s.y / 100f) - box / 2)
                        .size(box)
                        .rotate(s.rot),
                )
            }
        }
    }
}
