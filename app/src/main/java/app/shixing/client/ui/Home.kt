package app.shixing.client.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shixing.client.BuildConfig
import app.shixing.client.data.Groups
import app.shixing.client.data.Msg
import app.shixing.client.data.StarCard
import app.shixing.client.data.Thread
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 五个页面，和网页的底栏一致：今天 / 连向我 / 星轨 / 星友圈 / 我的。
 * （地图榜和「我打过的地图」收在「我的」里，避免底栏超过五个。）
 */
@Composable
fun Home(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableStateOf(0) }
    var boardOpen by rememberSaveable { mutableStateOf(false) }
    var chatOpen by rememberSaveable { mutableStateOf<Long?>(null) }

    state.hint?.let { h ->
        LaunchedEffect(h) {
            snack.showSnackbar(h)
            vm.clearHint()
        }
    }
    state.error?.let { e ->
        LaunchedEffect(e) {
            snack.showSnackbar(e)
        }
    }

    Scaffold(
        containerColor = MaterialTheme_color.bg,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (state.session && !boardOpen && chatOpen == null) {
                BottomBar(tab) { picked ->
                    tab = picked
                    // 切页就重新拉一次：审核/授权状态可能刚变，别把上一次的报错留在页面上。
                    vm.refreshTab(picked)
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(MaterialTheme_color.bg)
                .statusBarsPaddingIfNeeded(),
        ) {
            if (state.loading) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme_color.accent,
                    trackColor = Color.Transparent,
                )
            } else {
                Spacer(Modifier.height(2.dp))
            }

            val chatPeer = chatOpen
            when {
                !state.session -> ConnectPage(vm, state)
                boardOpen -> BoardPage(
                    maps = state.boards,
                    onBack = { boardOpen = false },
                    onRate = { world -> vm.openMap(world) { } },
                    vm = vm,
                )
                chatPeer != null -> ChatPage(vm, chatPeer) { chatOpen = null }
                tab == 0 -> TodayPage(vm, state)
                tab == 1 -> InboxPage(vm, state)
                tab == 2 -> CrossingsPage(vm, state)
                tab == 3 -> PostsPage(vm, state)
                else -> MePage(vm, state, onOpenBoard = { boardOpen = true }, onOpenChat = { n -> chatOpen = n })
            }
        }
    }
}

/** 一个够用的配色容器；不引 MaterialTheme 的 colorScheme，避免和主题里那份重复。 */
object MaterialTheme_color {
    val bg = Color(0xFF05060A)
    val card = Color(0xFF11141F)
    val card2 = Color(0xFF1A1F2E)
    val ink = Color(0xFFF3F4F8)
    val ink2 = Color(0xFFAAB0C4)
    val ink3 = Color(0xFF6B7189)
    val accent = Color(0xFF4DB4FF)
    val accentDeep = Color(0xFF0397FE)
    val accent2 = Color(0xFFD69BFF)
    val line = Color(0x21FFFFFF)
    val warn = Color(0xFFFF7A7A)
}

private fun Modifier.statusBarsPaddingIfNeeded(): Modifier = this

// ---------------- 底栏 ----------------

@Composable
private fun BottomBar(current: Int, onPick: (Int) -> Unit) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "今天" to Icons.Filled.DateRange,
        "连向我" to Icons.Filled.Email,
        "星轨" to Icons.Filled.Star,
        "星友圈" to Icons.Filled.ThumbUp,
        "我的" to Icons.Filled.Person,
    )
    Column(Modifier.background(Color(0xFF070910))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, item ->
                val label = item.first
                val icon = item.second
                val on = i == current
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPick(i) }
                        .background(if (on) Color(0x1F4DB4FF) else Color.Transparent)
                        .padding(horizontal = 13.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (on) MaterialTheme_color.accent else MaterialTheme_color.ink3,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        fontSize = 10.5.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) MaterialTheme_color.accent else MaterialTheme_color.ink2,
                    )
                }
            }
        }
    }
}

// ---------------- 连接页 ----------------

@Composable
private fun ConnectPage(vm: MainViewModel, s: UiState) {
    val ctx = LocalContext.current
    var code by rememberSaveable { mutableStateOf("") }
    val dev = s.device

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Box(
            Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7FD3FF), Color(0xFF4DB4FF), Color(0xFFD69BFF)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFF05060A),
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("拾星", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme_color.accent)
        Spacer(Modifier.height(4.dp))
        Text("一个只读官方接口的第三方客户端", fontSize = 13.sp, color = MaterialTheme_color.ink2)
        Spacer(Modifier.height(18.dp))

        BoxCard {
            Text("一步连上", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme_color.ink)
            Spacer(Modifier.height(8.dp))

            if (dev == null) {
                Text(
                    "点下面的按钮会生成一个连接码；然后在网页上打开「我的资料 → 第三方客户端 → 连接新客户端」，把码填进去点「同意」。",
                    fontSize = 13.sp,
                    color = MaterialTheme_color.ink2,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { vm.startDevice() },
                    Modifier.fillMaxWidth(),
                    enabled = !s.loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme_color.accent),
                ) { Text("生成连接码", color = Color(0xFF04121F), fontWeight = FontWeight.SemiBold) }
            } else {
                Text("在网页上输入这串码", fontSize = 13.sp, color = MaterialTheme_color.ink2)
                Spacer(Modifier.height(6.dp))
                Text(
                    dev.userCode,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme_color.accent,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "还剩 ${left(s.deviceLeft)}；同意后会自动跳进去",
                    fontSize = 13.sp,
                    color = MaterialTheme_color.ink2,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val url = dev.verificationUriComplete
                            ?: (dev.verificationUri ?: "https://vrcc.cc/match/?connect=").let {
                                if (it.contains("?")) "$it=${dev.userCode.replace("-", "")}" else it
                            }
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    },
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme_color.accent),
                ) { Text("在浏览器里打开同意页", color = Color(0xFF04121F)) }
                Spacer(Modifier.height(9.dp))
                Text(
                    "取消",
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.cancelDevice() }
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme_color.ink2,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        BoxCard {
            Text("已经有 8 位连接码？", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme_color.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "网页「我的资料 → 在 VRCX 里提醒」生成的那种码，也可以直接填在这里。",
                fontSize = 13.sp,
                color = MaterialTheme_color.ink2,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.uppercase().filter { it.isLetterOrDigit() }.take(8) },
                placeholder = { Text("8 位连接码", color = MaterialTheme_color.ink2) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 17.sp, color = MaterialTheme_color.ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.connect(code) },
                Modifier.fillMaxWidth(),
                enabled = code.length == 8 && !s.loading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme_color.accent,
                    disabledContainerColor = MaterialTheme_color.card2,
                    disabledContentColor = MaterialTheme_color.ink2,
                ),
            ) { Text("连接") }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "每个客户端（包括这个）都有自己的授权，不会顶掉 VRCX 脚本或其他客户端。玩家可以在「我的资料 → 第三方客户端」里看到并单独断开。",
            fontSize = 12.sp,
            color = MaterialTheme_color.ink2,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "本应用不是官方应用，名字、图标都不使用「星遇同频 / VRCC / VRChat / 官方 / 管理员」。",
            fontSize = 12.sp,
            color = MaterialTheme_color.ink2,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun left(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}

// ---------------- 今天 ----------------

@Composable
private fun TodayPage(vm: MainViewModel, s: UiState) {
    val t = s.today
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        ScreenTitle("今天", t?.day?.let { "$it · 这一轮路过的星" } ?: "这一轮路过的星")
        PageError(vm, s, "today")
        t?.round?.let { r ->
            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val used = r.used ?: 0
                val max = r.max ?: 0
                Pill(
                    "已连 $used / $max",
                    Color(0x1F4DB4FF),
                    MaterialTheme_color.accent,
                    size = 11,
                )
                if (t.cards.isNotEmpty()) {
                    Pill(
                        "${t.cards.size} 张星卡",
                        Color(0x14FFFFFF),
                        MaterialTheme_color.ink2,
                        size = 11,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (t == null || t.cards.isEmpty()) {
            Empty("这一轮还没有星路过。")
        } else {
            t.cards.forEach { c ->
                StarCardItem(c, onLink = { vm.link(c.n, null, null) { m -> vm.showHint(m) } })
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "刷新",
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { vm.refreshToday() }
                .background(MaterialTheme_color.card2)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            fontSize = 13.sp,
            color = MaterialTheme_color.ink,
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- 连向我 ----------------

@Composable
private fun InboxPage(vm: MainViewModel, s: UiState) {
    val i = s.inbox
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        val counts = i?.counts.orEmpty()
        fun n(key: String): Int? = counts[key]?.let {
            runCatching { it.jsonPrimitive.intOrNull }.getOrNull()
        }
        val sub = buildList {
            n("in")?.let { add("连向我 $it") }
            n("out")?.let { add("我连出 $it") }
            n("mutual")?.let { add("互相连上 $it") }
        }.joinToString(" · ")
        ScreenTitle("连向我", sub.ifBlank { "连向我的星" })
        PageError(vm, s, "inbox")
        Spacer(Modifier.height(10.dp))
        if (i == null || i.cards.isEmpty()) {
            Empty("还没有星连向你。")
        } else {
            i.cards.forEach { c ->
                StarCardItem(
                    c,
                    onLink = {
                        if (c.mutual) vm.showHint("你们已经互相连上了")
                        else vm.link(c.n, null, null) { m -> vm.showHint(m) }
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- 共用零件 ----------------

@Composable
fun StarCardItem(c: StarCard, onLink: (() -> Unit)? = null) {
    BoxCard(tint = Color(Groups.color(c.group))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.name, c.group, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.name ?: "一颗星",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme_color.ink,
                    )
                    Spacer(Modifier.width(6.dp))
                    GroupPill(c.group)
                    if (c.verified) {
                        Spacer(Modifier.width(6.dp))
                        Pill("已验证", Color(0x332FD08A), Color(0xFF2FD08A))
                    }
                }
                c.line?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme_color.ink2)
                }
            }
            Text("#${c.star ?: c.n}", fontSize = 12.sp, color = MaterialTheme_color.ink2)
        }

        c.intro?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 13.sp, color = MaterialTheme_color.ink, lineHeight = 20.sp)
        }

        if (c.about.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            c.about.take(3).forEach { qa ->
                val q = qa.q ?: return@forEach
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text("· $q", fontSize = 12.sp, color = MaterialTheme_color.ink2)
                    Text(qa.a ?: "", fontSize = 12.sp, color = MaterialTheme_color.ink)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (c.mutual) {
                Pill("互相连上", Color(0x332FD08A), Color(0xFF2FD08A))
                c.vrc?.let {
                    Spacer(Modifier.width(8.dp))
                    Text("VRChat：$it", fontSize = 12.sp, color = MaterialTheme_color.ink2)
                }
            } else if (c.linked) {
                Pill("已连上", Color(0x33FFB547), Color(0xFFFFB547))
            } else {
                Pill("未连", Color(0x1FFFFFFF), MaterialTheme_color.ink2)
            }
            c.friends?.let {
                Spacer(Modifier.width(8.dp))
                Text("共同星友 $it", fontSize = 12.sp, color = MaterialTheme_color.ink2)
            }
            Spacer(Modifier.weight(1f))
            if (onLink != null && !c.linked) {
                Text(
                    "连上",
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onLink() }
                        .background(MaterialTheme_color.accent)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    color = Color(0xFF04121F),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun BoxCard(tint: Color? = null, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val top = (tint ?: Color(0xFF161A28)).copy(alpha = 0.15f)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(top, Color(0xFF090A11)),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(900f, 1400f),
                ),
            )
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(22.dp)),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 14.dp), content = content)
    }
}

@Composable
fun ScreenTitle(title: String, sub: String? = null) {
    Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme_color.ink)
    if (sub != null) {
        Spacer(Modifier.height(5.dp))
        Text(sub, fontSize = 13.sp, color = MaterialTheme_color.ink2)
    }
}

@Composable
fun Empty(text: String) {
    Text(
        text,
        Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        textAlign = TextAlign.Center,
        fontSize = 13.sp,
        color = MaterialTheme_color.ink2,
    )
}

@Composable
fun ErrorBar(text: String) {
    Surface(
        color = Color(0x22FF7A7A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FF7A7A)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            fontSize = 12.sp,
            color = Color(0xFFFFB4B4),
        )
    }
}

@Composable
fun PageError(vm: MainViewModel, s: UiState, key: String) {
    s.pageErrors[key]?.let {
        Spacer(Modifier.height(8.dp))
        ErrorBar(it)
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, size: Int = 11) {
    Text(
        text,
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontSize = size.sp,
        color = fg,
    )
}

@Composable
fun GroupPill(group: String?) {
    if (group == null) return
    val label = Groups.label(group)
    if (label.isEmpty() || label == "未知") return
    val c = Color(Groups.color(group))
    Pill(label, c.copy(alpha = 0.16f), c)
}

fun clock(at: Long?): String {
    if (at == null) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(at))
}

fun clockShort(at: Long?): String {
    if (at == null) return ""
    return SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(at))
}

@Composable
fun Centered(loading: Boolean, empty: String, emptyWhen: Boolean, content: @Composable () -> Unit) {
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme_color.accent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
        }
        emptyWhen -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(empty, fontSize = 13.sp, color = MaterialTheme_color.ink2)
        }
        else -> content()
    }
}

/** 顶部错误条（点击错误可以把错误清掉）。 */
@Composable
fun Notice(text: String?) {
    if (text == null) return
    ErrorBar(text)
}
