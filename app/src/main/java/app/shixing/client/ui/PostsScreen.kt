package app.shixing.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shixing.client.data.Groups
import app.shixing.client.data.Post
import app.shixing.client.legacy.LegacyViewModel
import app.shixing.client.legacy.childObj
import app.shixing.client.legacy.int
import app.shixing.client.legacy.str
import coil.compose.AsyncImage
import kotlinx.serialization.json.JsonArray

/**
 * 星友圈：上面「动态 / 榜单」两个子页，动态页里带星搭、筛选和刷动态。
 * 榜单页直接复用地图榜（评分入口仍在「我的」里）。
 */
@Composable
fun PostsScreen(vm: MainViewModel, s: UiState, legacyVm: LegacyViewModel) {
    val legacyState by legacyVm.state.collectAsState()
    var sub by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf(0) } // 0 全部 1 好友 2 我的
    var showWrite by remember { mutableStateOf(false) }

    val mine = legacyState.me?.childObj("me")
    val xd = mine?.childObj("xd")
    val rooms = (mine?.get("xdRooms") as? JsonArray)

    androidx.compose.runtime.LaunchedEffect(s.posts.map { it.author?.n }) {
        s.posts.forEach { p -> p.author?.n?.let { n -> if (s.cardPhotos[n] == null) vm.loadPhoto(n) } }
    }
    Column(Modifier.fillMaxSize()) {
        // 顶：动态 / 榜单 / 发动态
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 14.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("动态", "榜单").forEachIndexed { idx, label ->
                val on = sub == idx
                Column(
                    Modifier
                        .clickable { sub = idx }
                        .padding(end = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        fontSize = 20.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) MaterialTheme_color.ink else MaterialTheme_color.ink2,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(if (on) 34.dp else 0.dp)
                            .background(MaterialTheme_color.accent),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x14FFFFFF))
                    .clickable { showWrite = !showWrite },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.Add,
                    contentDescription = "发动态",
                    tint = MaterialTheme_color.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (sub == 1) {
            BoardPage(
                maps = s.boards,
                onBack = {},
                onRate = { world -> vm.openMap(world) { m -> m?.let { vm.showHint("已拿到这张图的分") } } },
                vm = vm,
                showBack = false,
            )
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // 星搭：网页那排门票
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("星搭", fontSize = 12.sp, color = MaterialTheme_color.ink2)
                Spacer(Modifier.weight(1f))
                Text(
                    "怎么玩 ›",
                    fontSize = 12.sp,
                    color = MaterialTheme_color.ink2,
                    modifier = Modifier.clickable { vm.showHint("星搭：在地图里约个时间，等别人拿门票来。") },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val tickets = rooms?.take(6) ?: emptyList()
                tickets.forEach { el ->
                    val o = el as? kotlinx.serialization.json.JsonObject
                    TicketCard(
                        title = o?.str("name") ?: o?.str("title") ?: "星搭",
                        world = o?.str("worldName") ?: o?.str("map") ?: "",
                        time = o?.int("start")?.let { clock(it.toLong()) } ?: "",
                        joined = o?.int("seats") ?: 0,
                    )
                }
                // 发一张门票
                Column(
                    Modifier
                        .width(126.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0DFFFFFF))
                        .clickable { vm.showHint("发门票走网页的星搭表单，这块还没接。") },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("＋", fontSize = 22.sp, color = MaterialTheme_color.ink2)
                    Spacer(Modifier.height(6.dp))
                    Text("发一张门票", fontSize = 12.sp, color = MaterialTheme_color.ink2)
                }
            }

            // 筛选
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x0FFFFFFF))
                    .padding(3.dp),
            ) {
                listOf("全部", "好友", "我的").forEachIndexed { idx, label ->
                    val on = filter == idx
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (on) Color(0x1FFFFFFF) else Color.Transparent)
                            .clickable { filter = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = if (on) MaterialTheme_color.ink else MaterialTheme_color.ink2,
                        )
                    }
                }
            }

            if (showWrite) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.padding(horizontal = 18.dp)) { WritePostCard(vm) }
            }

            Spacer(Modifier.height(12.dp))
            PageError(vm, s, "posts")

            val list = when (filter) {
                2 -> s.posts.filter { it.mine == true }
                1 -> s.posts.filter { it.mine }
                else -> s.posts
            }
            if (list.isEmpty()) {
                Empty("还没有动态。")
            } else {
                list.forEach { p ->
                    Box(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                        PostCard(vm, p, s.cardPhotos)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TicketCard(title: String, world: String, time: String, joined: Int) {
    Column(
        Modifier
            .width(126.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .padding(10.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme_color.ink, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        if (time.isNotBlank()) {
            Text(time, fontSize = 12.sp, color = Color(0xFF2FD08A))
        }
        Spacer(Modifier.weight(1f))
        if (world.isNotBlank()) {
            Text(world, fontSize = 10.sp, color = MaterialTheme_color.ink2, maxLines = 1)
        }
        if (joined > 0) {
            Text("$joined 人", fontSize = 10.sp, color = MaterialTheme_color.ink3)
        }
    }
}

@Composable
private fun PostCard(vm: MainViewModel, p: Post, photos: Map<Long, app.shixing.client.data.CardPhoto> = emptyMap()) {
    val au = p.author
    val mutual = au?.mutual == true
    // 连上的用照片 + VRChat 名，没连上的用模糊格 + 代号
    val who = (if (mutual) au?.vrc ?: au?.name else au?.name)
        ?: if (p.kind == "official") "星遇同频" else "一颗星"
    BoxCard(tint = Color(Groups.color(p.author?.group))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoAvatar(
                photoUrl = if (mutual) au?.let { photos[it.n]?.url } else null,
                grid = au?.let { photos[it.n]?.grid },
                name = who,
                size = 32.dp,
            )
            Spacer(Modifier.width(9.dp))
            Text(who, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme_color.ink)
            p.author?.group?.let {
                Spacer(Modifier.width(6.dp))
                GroupPill(it)
            }
            Spacer(Modifier.weight(1f))
            Text(clock(p.at), fontSize = 11.sp, color = MaterialTheme_color.ink2)
        }
        p.text?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 13.sp, color = MaterialTheme_color.ink, lineHeight = 20.sp)
        }
        p.imgUrl?.let { url ->
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme_color.card2),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "👍 ${p.lit}",
                fontSize = 12.sp,
                color = MaterialTheme_color.ink2,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { vm.light(p.id, true) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text("评论 ${p.cmts}", fontSize = 12.sp, color = MaterialTheme_color.ink2)
        }
    }
}
