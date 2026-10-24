package app.shixing.client.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shixing.client.data.CardScores
import app.shixing.client.data.Groups
import app.shixing.client.data.MyCard
import coil.compose.AsyncImage

/**
 * 和网页版同款的星卡：竖版、带外描边、顶部编号、底部星星名与四维条。
 * 点一下翻到背面（自己看的时候）——背面是四维数值和介绍。
 */
@Composable
fun StarCardFace(
    card: MyCard,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    faceLabel: String? = null,
) {
    val shape = RoundedCornerShape(26.dp)
    val group = card.group
    val accent = Color(Groups.color(group ?: "G"))
    val ring = Brush.linearGradient(listOf(Color(0xFFE8C983), Color(0xFF8A6A32)))

    Box(
        modifier
            .aspectRatio(0.70f)
            .clip(shape)
            .background(Color(0xFF0A0C13))
            .border(2.dp, ring, shape),
    ) {
        // 底色：有照片就铺照片，没有就用群组色的暗渐变
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
                            listOf(accent.copy(alpha = 0.45f), Color(0xFF0A0C13)),
                            start = Offset.Zero,
                            end = Offset(900f, 1400f),
                        ),
                    ),
            )
        }

        // 底部压暗，保证文字可读
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        0.62f to Color(0x9905060A),
                        1f to Color(0xF205060A),
                    ),
                ),
        )

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "VRCHAT 中文社区 · 星遇同频",
                    fontSize = 8.sp,
                    color = Color(0xFFE8C983),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "No." + (card.star?.toString()?.padStart(5, '0') ?: "-----"),
                    fontSize = 15.sp,
                    color = Color(0xFFE8C983),
                    fontWeight = FontWeight.Bold,
                )
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
                        color = Color(0xFFE8C983),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "来自" + GroupName.of(group),
                        fontSize = 11.sp,
                        color = Color(0xFFE8C983),
                        fontWeight = FontWeight.SemiBold,
                    )
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
                Text(
                    card.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3F4F8),
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (card.line.isNotBlank()) {
                    Text(
                        card.line,
                        fontSize = 12.sp,
                        color = Color(0xFFE9E4D6),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                card.scores?.let { scores ->
                    ScoreBar(scores, Modifier.padding(top = 12.dp))
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
                val weight = value.coerceAtLeast(1).toFloat()
                Box(
                    Modifier
                        .weight(weight)
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

/** 背面：四维数值 + 介绍 + 关于我。 */
@Composable
fun StarCardBack(card: MyCard, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    val ring = Brush.linearGradient(listOf(Color(0xFFE8C983), Color(0xFF8A6A32)))

    Column(
        modifier
            .aspectRatio(0.70f)
            .clip(shape)
            .background(Color(0xFF0B0D15))
            .border(2.dp, ring, shape)
            .padding(20.dp),
    ) {
        Text(
            "No." + (card.star?.toString()?.padStart(5, '0') ?: "-----"),
            fontSize = 13.sp,
            color = Color(0xFFE8C983),
            fontWeight = FontWeight.Bold,
        )
        Text(
            card.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF3F4F8),
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            card.line,
            fontSize = 12.sp,
            color = MaterialTheme_color.ink2,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(16.dp))
        card.scores?.pairs()?.forEach { (group, value) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Groups.label(group), fontSize = 12.sp, color = MaterialTheme_color.ink2, modifier = Modifier.width(40.dp))
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
                    fontSize = 12.sp,
                    color = MaterialTheme_color.ink,
                    modifier = Modifier
                        .width(34.dp)
                        .padding(start = 8.dp),
                    textAlign = TextAlign.End,
                )
            }
        }

        card.intro?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, fontSize = 12.sp, color = MaterialTheme_color.ink2, lineHeight = 19.sp)
        }
    }
}

/** 全屏的「我的星卡」：点一下翻面。 */
@Composable
fun MyCardScreen(
    card: MyCard?,
    photoUrl: String?,
    faceLabel: String?,
    onBack: () -> Unit,
) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(420),
        label = "flip",
    )

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
                Box(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { flipped = !flipped },
                ) {
                    if (rotation <= 90f) {
                        StarCardFace(card, photoUrl, Modifier.fillMaxWidth(), faceLabel)
                    } else {
                        Box(Modifier.graphicsLayer { rotationY = 180f }) {
                            StarCardBack(card, Modifier.fillMaxWidth())
                        }
                    }
                }
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
