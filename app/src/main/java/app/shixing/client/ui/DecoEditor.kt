package app.shixing.client.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shixing.client.data.CardDeco
import app.shixing.client.data.CardStyle
import app.shixing.client.data.DecoCatalog
import app.shixing.client.legacy.LegacyViewModel
import app.shixing.client.legacy.childObj
import app.shixing.client.legacy.int
import app.shixing.client.legacy.str

/** 网页「装扮」的卡面顺序，跟网页底部一排一致。 */
private val FACE_ORDER = listOf(
    "nebula", "nova", "eclipse", "polaroid", "gilded", "clear",
    "aurora", "chart", "comet", "rosette", "film", "ticket", "shikishi", "acrylic",
)

@Composable
fun DecoEditorScreen(
    state: LegacyViewModel.State,
    onFace: (String) -> Unit,
    onBack: (String) -> Unit,
    onSleeve: (String) -> Unit,
    onClose: () -> Unit,
    onLogin: () -> Unit = {},
    onSave: (String?, String?, String?) -> Unit = { _, _, _ -> },
) {
    if (!state.hasKey) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "返回",
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme_color.card2)
                        .clickable(onClick = onClose)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme_color.ink,
                )
                Spacer(Modifier.width(12.dp))
                Text("装扮", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme_color.ink)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "装扮走的是网页自己的接口，需要先在这台手机上登录一次网页版。登录之后，你拥有的卡面、卡背都会出现在这里。",
                fontSize = 13.sp,
                color = MaterialTheme_color.ink2,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "登录网页版",
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme_color.accent)
                    .clickable(onClick = onLogin)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                fontSize = 14.sp,
                color = Color(0xFF04121F),
                fontWeight = FontWeight.SemiBold,
            )
        }
        return
    }

    var tab by remember { mutableStateOf(0) }
    val savedFace = state.deco?.str("face") ?: "nebula"
    val savedBack = state.deco?.str("back") ?: "nebula"
    val savedSleeve = state.deco?.str("sleeve")

    // 预览用本地状态：点了立刻在卡上看到，按「完成」才写回服务器
    var face by remember(savedFace) { mutableStateOf(savedFace) }
    var back by remember(savedBack) { mutableStateOf(savedBack) }
    var sleeve by remember(savedSleeve) { mutableStateOf(savedSleeve) }

    val dirty = face != savedFace || back != savedBack || sleeve != savedSleeve
    val busy = state.busy

    val meBody = state.me?.childObj("me")
    val preview = CardFace(
        name = meBody?.str("name") ?: "我的星",
        star = meBody?.int("star"),
        line = meBody?.str("line"),
        group = meBody?.str("group"),
        verified = true,
        no = meBody?.int("star"),
        deco = CardDeco(face = face, back = back, sleeve = sleeve),
        hasPhoto = false,
        common = emptyList(),
    )

    val ownedF = state.owned.filter { it.kind == "f" }.associateBy { it.key } + ("nebula" to LegacyViewModel.Owned("f", "nebula", 1))
    val ownedB = state.owned.filter { it.kind == "b" }.associateBy { it.key }
    val ownedV = state.owned.filter { it.kind == "v" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "返回",
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme_color.card2)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = MaterialTheme_color.ink,
            )
            Spacer(Modifier.width(12.dp))
            Text("装扮", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme_color.ink)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    busy -> "保存中…"
                    dirty -> "完成"
                    else -> "已完成"
                },
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (dirty) MaterialTheme_color.accent else MaterialTheme_color.card2)
                    .clickable(enabled = dirty && !busy) {
                        onSave(
                            face.takeIf { it != savedFace },
                            back.takeIf { it != savedBack },
                            sleeve?.takeIf { it != savedSleeve },
                        )
                        // 顺手把单项也写一遍，兼容老的 equip 接口
                        if (face != savedFace) onFace(face)
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dirty) Color(0xFF04121F) else MaterialTheme_color.ink3,
            )
        }

        // 预览：一张真卡，实时跟着下面选的档位变
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth(0.74f)) {
                FlippableStarCard(face = preview, photoUrl = state.photoUrl, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            DecoCatalog.style(face).name + " · " + DecoCatalog.back(back).id + if (dirty) " · 未保存" else "",
            Modifier.fillMaxWidth(),
            fontSize = 11.sp,
            color = if (dirty) MaterialTheme_color.accent else MaterialTheme_color.ink3,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        // 分页
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("卡面", "贴纸", "卡背").forEachIndexed { i, name ->
                val on = tab == i
                Text(
                    name,
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) MaterialTheme_color.card2 else Color.Transparent)
                        .clickable { tab = i }
                        .padding(vertical = 9.dp),
                    fontSize = 13.sp,
                    color = if (on) MaterialTheme_color.ink else MaterialTheme_color.ink3,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        state.error?.let {
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FF7A7A))
                    .border(1.dp, Color(0x66FF7A7A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(it, fontSize = 12.sp, color = Color(0xFFFFB4B4)) }
            Spacer(Modifier.height(10.dp))
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            when (tab) {
                0 -> FaceGrid(face, ownedF, onPick = { face = it })
                1 -> Empty("贴纸摆放还在做：先把卡面和卡背做成网页那样。")
                else -> BackGrid(back, ownedB, onPick = { back = it })
            }
            Spacer(Modifier.height(20.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FaceGrid(cur: String, owned: Map<String, LegacyViewModel.Owned>, onPick: (String) -> Unit) {
    Text(
        "卡面 " + owned.size + " / ${FACE_ORDER.size}　开卡面包获得更多",
        fontSize = 11.sp,
        color = MaterialTheme_color.ink3,
        modifier = Modifier.padding(bottom = 10.dp),
    )
    FACE_ORDER.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { id ->
                val st = DecoCatalog.style(id)
                val own = owned[id]
                Tile(
                    name = st.name,
                    top = st.top,
                    bottom = st.bottom,
                    ink = st.ink,
                    selected = id == cur,
                    owned = own != null,
                    expiry = own?.until,
                    modifier = Modifier.weight(1f),
                    onClick = { onPick(id) },
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun BackGrid(cur: String, owned: Map<String, LegacyViewModel.Owned>, onPick: (String) -> Unit) {
    val list = DecoCatalog.backs.values.toList()
    Text("卡背 ${owned.size} / ${list.size}", fontSize = 11.sp, color = MaterialTheme_color.ink3, modifier = Modifier.padding(bottom = 10.dp))
    list.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { b ->
                val own = owned[b.id]
                Tile(
                    name = b.id,
                    top = b.bg,
                    bottom = b.bg,
                    ink = b.ink,
                    selected = b.id == cur,
                    owned = own != null || b.id in setOf("crest", "grid", "seigaiha", "airmail"),
                    expiry = own?.until,
                    modifier = Modifier.weight(1f),
                    onClick = { onPick(b.id) },
                )
            }
            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SleeveList(list: List<LegacyViewModel.Owned>, cur: String?, onPick: (String) -> Unit) {
    Text(
        "卡套 ${list.size} 个　开卡面包或兑换获得",
        fontSize = 11.sp,
        color = MaterialTheme_color.ink3,
        modifier = Modifier.padding(bottom = 10.dp),
    )
    if (list.isEmpty()) {
        Empty("仓库里还没有卡套。开包或兑换之后会出现在这里。")
        return
    }
    val rows = list.chunked(3)
    rows.forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { v ->
                val sv = DecoCatalog.sleeve(v.key)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF07080D))
                            .border(
                                width = if (v.key == cur) 2.dp else 1.dp,
                                color = if (v.key == cur) MaterialTheme_color.accent else Color(0x14FFFFFF),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onPick(v.key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sv != null) {
                            SleevePreview(sv, Modifier.fillMaxSize().padding(5.dp))
                        }
                        if (v.key == cur) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme_color.accent),
                                contentAlignment = Alignment.Center,
                            ) { Text("✓", fontSize = 11.sp, color = Color(0xFF04121F), fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        sv?.name ?: v.key,
                        fontSize = 10.sp,
                        color = MaterialTheme_color.ink2,
                    )
                    expiryText(v.until)?.let { Text(it, fontSize = 9.sp, color = MaterialTheme_color.ink3) }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}
@Composable
private fun Tile(
    name: String,
    top: Color,
    bottom: Color,
    ink: Color,
    selected: Boolean,
    owned: Boolean,
    expiry: Long?,
    modifier: Modifier = Modifier,
    style: CardStyle? = null,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF07080D))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme_color.accent else Color(0x14FFFFFF),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(enabled = owned, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // 网页那边每一格就是这张卡的小样，不是一块纯色
            if (style != null) {
                MiniFace(style, Modifier.fillMaxSize().padding(5.dp))
            } else {
                Box(Modifier.fillMaxSize().padding(5.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(top, bottom))))
            }
            if (!owned) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)))
                Text("未拥有", fontSize = 10.sp, color = Color(0x99FFFFFF))
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme_color.accent),
                    contentAlignment = Alignment.Center,
                ) { Text("✓", fontSize = 11.sp, color = Color(0xFF04121F), fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            fontSize = 10.sp,
            color = if (owned) MaterialTheme_color.ink2 else MaterialTheme_color.ink3.copy(alpha = 0.6f),
        )
        expiryText(expiry)?.let {
            Text(it, fontSize = 9.sp, color = MaterialTheme_color.ink3)
        }
    }
}

private fun expiryText(until: Long?): String? {
    if (until == null || until <= 0L) return null
    val left = until - System.currentTimeMillis()
    if (left <= 0L) return "已过期"
    val days = left / 86_400_000L
    if (days >= 1) return "剩 $days 天"
    val hours = left / 3_600_000L
    if (hours >= 1) return "剩 $hours 小时"
    return "剩 " + (left / 60_000L).coerceAtLeast(1) + " 分钟"
}

/** 一格卡面小样：只画底、内框、光效，不放文字。 */
@Composable
private fun MiniFace(st: CardStyle, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(st.frameColors, start = Offset.Zero, end = Offset(240f, 340f))),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(st.framePad.coerceAtMost(5f).dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.linearGradient(listOf(st.top, st.bottom), start = Offset.Zero, end = Offset(200f, 320f))),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(st.glow.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(120f, 90f),
                            radius = 220f,
                        ),
                    ),
            )
            when (st.fx) {
                CardStyle.Fx.NOVA -> Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.34f
                    for (i in 0 until 28) {
                        val a = (i / 28f) * 6.28318f
                        drawLine(
                            color = Color(0xFFFFD27A).copy(alpha = 0.25f),
                            start = Offset(cx, cy),
                            end = Offset(cx + kotlin.math.cos(a) * size.width * 0.7f, cy + kotlin.math.sin(a) * size.width * 0.7f),
                            strokeWidth = 1.2f,
                        )
                    }
                    drawCircle(Color(0xFFFFF3D0), radius = size.width * 0.13f, center = Offset(cx, cy))
                }

                CardStyle.Fx.ECLIPSE -> Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.34f
                    drawCircle(Color(0xE6FFF6E6), radius = size.width * 0.2f, center = Offset(cx, cy), style = Stroke(width = 1.6f))
                }

                else -> Unit
            }
        }
    }
}