package app.shixing.client.ui

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shixing.client.data.DecoCatalog
import app.shixing.client.legacy.LegacyViewModel
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
                "装扮走的是网页自己的接口，需要先在这台手机上登录一次网页版。登录之后，你拥有的卡面、卡背、卡套都会出现在这里。",
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
    val deco = state.deco
    val curFace = deco?.str("face") ?: "nebula"
    val curBack = deco?.str("back") ?: "nebula"
    val curSleeve = deco?.str("sleeve")

    val ownedF = state.owned.filter { it.kind == "f" }.map { it.key }.toSet() + "nebula"
    val ownedB = state.owned.filter { it.kind == "b" }.map { it.key }.toSet() + listOf("crest", "grid", "seigaiha", "airmail")
    val ownedV = state.owned.filter { it.kind == "v" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("卡面", "卡背", "卡套").forEachIndexed { i, name ->
                val on = tab == i
                Text(
                    name,
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) MaterialTheme_color.accent else MaterialTheme_color.card2)
                        .clickable { tab = i }
                        .padding(vertical = 9.dp),
                    fontSize = 13.sp,
                    color = if (on) Color(0xFF04121F) else MaterialTheme_color.ink2,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        state.error?.let {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FF7A7A))
                    .border(1.dp, Color(0x66FF7A7A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(it, fontSize = 12.sp, color = Color(0xFFFFB4B4)) }
            Spacer(Modifier.height(10.dp))
        }
        if (state.busy) {
            Text("正在换上…", fontSize = 12.sp, color = MaterialTheme_color.ink3)
            Spacer(Modifier.height(8.dp))
        }

        when (tab) {
            0 -> {
                Text("卡面 ${ownedF.size} / ${FACE_ORDER.size}", fontSize = 12.sp, color = MaterialTheme_color.ink3)
                Spacer(Modifier.height(10.dp))
                FACE_ORDER.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { id ->
                            val st = DecoCatalog.style(id)
                            Swatch(
                                name = st.name,
                                top = st.top,
                                bottom = st.bottom,
                                ink = st.ink,
                                owned = id in ownedF,
                                equipped = id == curFace,
                                modifier = Modifier.weight(1f),
                                onClick = { onFace(id) },
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            1 -> {
                val backs = DecoCatalog.backs.values.toList()
                Text("卡背", fontSize = 12.sp, color = MaterialTheme_color.ink3)
                Spacer(Modifier.height(10.dp))
                backs.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { bk ->
                            Swatch(
                                name = bk.id,
                                top = bk.bg,
                                bottom = bk.bg,
                                ink = bk.ink,
                                owned = bk.id in ownedB,
                                equipped = bk.id == curBack,
                                modifier = Modifier.weight(1f),
                                onClick = { onBack(bk.id) },
                            )
                        }
                        repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            else -> {
                Text("卡套（仓库里有的）", fontSize = 12.sp, color = MaterialTheme_color.ink3)
                Spacer(Modifier.height(10.dp))
                if (ownedV.isEmpty()) {
                    Empty("仓库里还没有卡套。开包或兑换之后会出现在这里。")
                } else {
                    ownedV.forEach { v ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme_color.card)
                                .clickable { onSleeve(v.key) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(v.key, fontSize = 14.sp, color = MaterialTheme_color.ink)
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (v.key == curSleeve) "已装备" else "换上",
                                fontSize = 12.sp,
                                color = if (v.key == curSleeve) MaterialTheme_color.accent else MaterialTheme_color.ink3,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "卡面和卡背会立刻写回你的星卡；贴纸摆位还在做。",
            fontSize = 11.sp,
            color = MaterialTheme_color.ink3,
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Swatch(
    name: String,
    top: Color,
    bottom: Color,
    ink: Color,
    owned: Boolean,
    equipped: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(top, bottom)))
                .border(
                    width = if (equipped) 2.dp else 1.dp,
                    color = if (equipped) MaterialTheme_color.accent else Color(0x14FFFFFF),
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(enabled = owned, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    equipped -> "已装备"
                    owned -> "换上"
                    else -> "未拥有"
                },
                fontSize = 11.sp,
                color = if (owned) ink.copy(alpha = 0.85f) else ink.copy(alpha = 0.35f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            fontSize = 10.sp,
            color = if (owned) MaterialTheme_color.ink2 else MaterialTheme_color.ink3.copy(alpha = 0.6f),
        )
    }
}
