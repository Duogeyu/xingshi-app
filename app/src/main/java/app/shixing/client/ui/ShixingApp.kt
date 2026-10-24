package app.shixing.client.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shixing.client.BuildConfig
import app.shixing.client.data.BoardMap
import app.shixing.client.data.CrossStar
import app.shixing.client.data.Groups
import app.shixing.client.data.MutualStar
import app.shixing.client.data.MyMap
import app.shixing.client.data.NotificationItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink2 = Color(0xFF9AA3B5)

@Composable
fun ShixingApp(vm: MainViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    val snacks = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var rateTarget by remember { mutableStateOf<MyMap?>(null) }

    LaunchedEffect(s.hint) {
        val h = s.hint
        if (h != null) {
            snacks.showSnackbar(h)
            vm.clearHint()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snacks) },
        bottomBar = {
            if (s.session) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NavTab(0, "提醒", Icons.Filled.Notifications, tab) { tab = it }
                        NavTab(1, "星轨", Icons.Filled.Star, tab) { tab = it }
                        NavTab(2, "连上", Icons.Filled.Favorite, tab) { tab = it }
                        NavTab(3, "地图榜", Icons.Filled.Place, tab) { tab = it }
                        NavTab(4, "我的", Icons.Filled.Person, tab) { tab = it }
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (!s.session) {
                ConnectScreen(s.loading, s.error) { code -> vm.connect(code) }
            } else {
                when (tab) {
                    0 -> RemindersScreen(s.recent, s.readId, s.error, s.loading) {
                        vm.refreshAll()
                    }
                    1 -> CrossingsScreen(s.crossings, s.crossingsWhy, s.error, s.loading) {
                        vm.markCrossingsRead()
                    }
                    2 -> MutualsScreen(s.mutuals, s.error, s.loading) { vm.refreshAll() }
                    3 -> MapsScreen(s.boards, s.error) { map ->
                        vm.openMap(map.world) { rateTarget = it }
                    }
                    else -> MeScreen(s.star, s.vrc, s.dailyUsed, s.loading) { vm.disconnect() }
                }
            }
            if (s.loading) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }
    }

    val target = rateTarget
    if (target != null) {
        RateDialog(
            map = target,
            message = null,
            onDismiss = { rateTarget = null },
            onSubmit = { score, text ->
                vm.rate(target.world, score, text) { msg ->
                    rateTarget = null
                    scope.launch { snacks.showSnackbar(msg) }
                }
            },
        )
    }
}

@Composable
private fun NavTab(index: Int, label: String, icon: ImageVector, current: Int, onPick: (Int) -> Unit) {
    val on = current == index
    val tint = if (on) MaterialTheme.colorScheme.primary else Ink2
    Column(
        Modifier
            .clickable { onPick(index) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = tint,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ---------------------------------------------------------------- 连接页

@Composable
private fun ConnectScreen(loading: Boolean, error: String?, onConnect: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("拾星", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                "一个只读官方接口的第三方客户端",
                fontSize = 13.sp,
                color = Ink2,
            )
        }
        item {
            Card {
                Text("怎么连接", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "在 vrcc.cc/match/ 里打开「我的星」，生成一个 8 位连接码；" +
                        "或者在 VRCC 小助手那里输入「连接码」。拿到码后填在这里。",
                    fontSize = 13.sp,
                    color = Ink2,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                    label = { Text("8 位连接码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onConnect(code) },
                    enabled = !loading && code.length == 8,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text(if (loading) "连接中…" else "连接") }
            }
        }
        if (error != null) {
            item { ErrorCard(error) }
        }
        item {
            Text(
                "连接码只能用一次，30 天有效。每天最多连 30 次；每颗星同时只有一个连接，" +
                    "在别处连接会让你那边的登录失效。",
                fontSize = 12.sp,
                color = Ink2,
                lineHeight = 18.sp,
            )
        }
        item {
            Text(
                "本应用不是官方应用，名字、图标都不使用「星遇同频／VRCC／VRChat／官方／管理员」。",
                fontSize = 12.sp,
                color = Ink2,
                lineHeight = 18.sp,
            )
        }
    }
}

// ---------------------------------------------------------------- 提醒

@Composable
private fun RemindersScreen(
    items: List<NotificationItem>,
    readId: Long?,
    error: String?,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenTitle("提醒", "只显示最近一天的最后五条") }
        if (error != null) item { ErrorCard(error) }
        if (items.isEmpty() && !loading) item { Empty("还没有提醒。") }
        items(items, key = { it.id }) { n ->
            val unread = readId != null && n.id > readId
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(
                                if (unread) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                                RoundedCornerShape(50),
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(n.title ?: "提醒", fontSize = 14.sp, fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal)
                    Spacer(Modifier.weight(1f))
                    Text(clock(n.at), fontSize = 11.sp, color = Ink2)
                }
                Spacer(Modifier.height(6.dp))
                Text(n.body.orEmpty(), fontSize = 13.sp, color = Ink2, lineHeight = 19.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- 星轨

@Composable
private fun CrossingsScreen(
    stars: List<CrossStar>,
    why: String?,
    error: String?,
    loading: Boolean,
    onMarkRead: () -> Unit,
) {
    if (why == "hide") {
        // 官方要求：why=hide 时整块不要显示。
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("这一页现在不可用", color = Ink2, fontSize = 13.sp)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle("星轨相交", "在同一个房间遇到的星", Modifier.weight(1f))
                TextButton(onClick = onMarkRead) { Text("全部已读", fontSize = 12.sp) }
            }
        }
        if (error != null) item { ErrorCard(error) }
        if (why != null && why != "hide") item { WhyCard(why) }
        if (stars.isEmpty() && !loading && error == null) item { Empty("还没有相交记录。") }
        items(stars, key = { it.n }) { st ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(st.name ?: "匿名", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    GroupPill(st.group)
                    if (st.fresh) {
                        Spacer(Modifier.width(6.dp))
                        Pill("新", Color(0xFF5FE0B4))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(clock(st.at), fontSize = 11.sp, color = Ink2)
                }
                if (!st.line.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(st.line, fontSize = 12.sp, color = Ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!st.intro.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(st.intro, fontSize = 13.sp, lineHeight = 19.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Pill(if (st.mutual) "互相连上" else if (st.linked) "已连上" else "未连上",
                        if (st.mutual) MaterialTheme.colorScheme.primary else Ink2)
                    if (st.vrc != null) {
                        Spacer(Modifier.width(6.dp))
                        Pill("VRChat: ${st.vrc}", Color(0xFFD69BFF))
                    }
                }
            }
        }
    }
}

@Composable
private fun WhyCard(why: String) {
    val text = when (why) {
        "verify" -> "先去 vrcc.cc 验证 VRChat 账号，这里才会出现相交记录。"
        "wait" -> "还要再等等：新号的星轨要过一阵才会显示。"
        "queued" -> "机器人还没有扫到这个房间，排队中。"
        "sent" -> "已经把你的请求发过去了，等对方回应。"
        "gone" -> "对方已经不在了。"
        "friend" -> "先和小助手成为好友，它才能读到你的星轨。"
        else -> "暂时没有数据。"
    }
    Card {
        Text(text, fontSize = 13.sp, color = Color(0xFFFFB547), lineHeight = 19.sp)
    }
}

// ---------------------------------------------------------------- 互相连上

@Composable
private fun MutualsScreen(
    stars: List<MutualStar>,
    error: String?,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle("互相连上", "你们两边都点过连线", Modifier.weight(1f))
                TextButton(onClick = onRefresh) { Text("刷新", fontSize = 12.sp) }
            }
        }
        if (error != null) item { ErrorCard(error) }
        if (stars.isEmpty() && !loading && error == null) item { Empty("还没有互相连上的星。") }
        items(stars, key = { it.n }) { m ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.name ?: "匿名", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    GroupPill(m.group)
                    Spacer(Modifier.weight(1f))
                    Text(clock(m.at), fontSize = 11.sp, color = Ink2)
                }
                if (m.vrc != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("VRChat：${m.vrc}", fontSize = 13.sp, color = Color(0xFFD69BFF))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 地图榜

@Composable
private fun MapsScreen(maps: List<BoardMap>, error: String?, onPick: (BoardMap) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenTitle("地图榜", "满 5 人评分才会进榜，点一张地图给自己的评分") }
        if (error != null) item { ErrorCard(error) }
        if (maps.isEmpty()) item { Empty("榜单还没拿到。") }
        items(maps, key = { it.world }) { m ->
            Card(Modifier.clickable { onPick(m) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${m.rank}", fontSize = 13.sp, color = Ink2, modifier = Modifier.width(38.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name ?: "未命名地图", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${m.author ?: "未知作者"} · ${m.weight ?: "—"} · ${m.raters ?: 0} 人评",
                            fontSize = 12.sp,
                            color = Ink2,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(m.avg?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun RateDialog(
    map: MyMap,
    message: String?,
    onDismiss: () -> Unit,
    onSubmit: (Int, String?) -> Unit,
) {
    var score by remember { mutableIntStateOf(map.mine ?: 0) }
    var words by remember { mutableStateOf(map.words ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(map.name ?: "这张地图", fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(
                    "榜单均分 ${map.avg?.let { String.format(Locale.US, "%.1f", it) } ?: "—"} · ${map.raters} 人评",
                    fontSize = 12.sp,
                    color = Ink2,
                )
                Spacer(Modifier.height(10.dp))
                if (!map.can) {
                    Text(
                        when (map.need) {
                            "verify" -> "要先验证 VRChat 账号才能打分。"
                            "friend" -> "先同意小助手的好友请求才能打分。"
                            "go" -> "先在这张地图上待一会儿才能打分。"
                            else -> "这张地图现在不能打分。"
                        },
                        fontSize = 13.sp,
                        color = Color(0xFFFFB547),
                    )
                } else {
                    Text("你的评分", fontSize = 12.sp, color = Ink2)
                    Spacer(Modifier.height(6.dp))
                    for (row in 0 until 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in 1..5) {
                                val v = row * 5 + i
                                Pill(
                                    text = "$v",
                                    color = if (score == v) MaterialTheme.colorScheme.primary else Ink2,
                                    selected = score == v,
                                ) { score = v }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = words,
                        onValueChange = { words = it.take(30) },
                        label = { Text("短评（可不填，最多 30 字）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Text("${words.length}/30", fontSize = 11.sp, color = Ink2)
                }
                if (message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = map.can && score in 1..10,
                onClick = { onSubmit(score, if (words.isBlank()) "" else words.trim()) },
            ) { Text(if (map.mine == null) "提交" else "改一下") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// ---------------------------------------------------------------- 我的

@Composable
private fun MeScreen(star: Int?, vrc: String?, dailyUsed: Int, loading: Boolean, onDisconnect: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenTitle("我的", null) }
        item {
            Card {
                Text("星号 ${star ?: "—"}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (vrc != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("VRChat：$vrc", fontSize = 13.sp, color = Color(0xFFD69BFF))
                }
                Spacer(Modifier.height(8.dp))
                Text("今天已用个人接口 $dailyUsed / 300 次", fontSize = 12.sp, color = Ink2)
            }
        }
        item {
            Card {
                Text("关于", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text("拾星 ${BuildConfig.VERSION_NAME}（第三方客户端，非官方）", fontSize = 13.sp, color = Ink2)
                Spacer(Modifier.height(4.dp))
                Text("客户端标识 ${BuildConfig.CLIENT_ID}", fontSize = 12.sp, color = Ink2)
                Spacer(Modifier.height(4.dp))
                Text("令牌只存在这台设备上，只发给 vrcc.cc；断开会从本机删除。", fontSize = 12.sp, color = Ink2, lineHeight = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text("源代码：${BuildConfig.SOURCE_URL}", fontSize = 12.sp, color = Ink2)
            }
        }
        item {
            Button(
                onClick = onDisconnect,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) { Text("断开连接") }
        }
        item {
            Text(
                "「改资料／注销账号」这一类操作官方不提供接口，请回到网页端做。",
                fontSize = 12.sp,
                color = Ink2,
                lineHeight = 18.sp,
            )
        }
    }
}

// ---------------------------------------------------------------- 小零件

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ScreenTitle(title: String, sub: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, fontSize = 12.sp, color = Ink2)
    }
}

@Composable
private fun ErrorCard(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0x22FF7A7A)) {
        Text(text, fontSize = 13.sp, color = Color(0xFFFFB3B3), modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun Empty(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = Ink2)
    }
}

@Composable
private fun Pill(text: String, color: Color, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) color.copy(alpha = 0.22f) else Color(0x14FFFFFF),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun GroupPill(group: String?) {
    Pill(Groups.label(group), Color(Groups.color(group)))
}

private fun clock(at: Long?): String {
    if (at == null || at <= 0) return ""
    return clockFormat.format(Date(at))
}

private val clockFormat = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
