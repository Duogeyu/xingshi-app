package app.shixing.client.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.shixing.client.data.CardPhoto
import app.shixing.client.data.StarCard
import app.shixing.client.data.Thread

/**
 * 「查看全部」：把和我连上的人摊成网格。
 * 连上的用照片 + VRChat 名，没连上的用模糊格 + 星名。
 */
@Composable
fun ThreadsGrid(
    threads: List<Thread>,
    cards: List<StarCard>,
    photos: Map<Long, CardPhoto>,
    onClose: () -> Unit,
    onPick: (StarCard) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "和我连上的人",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme_color.ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "关闭",
                fontSize = 12.sp,
                color = MaterialTheme_color.ink2,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        threads.chunked(4).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                rowItems.forEach { th ->
                    val n = th.peer?.n
                    val card = cards.firstOrNull { it.n == n }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(bottom = 18.dp)
                            .clickable(enabled = card != null) { card?.let(onPick) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PhotoAvatar(
                            photoUrl = n?.let { photos[it]?.url },
                            grid = n?.let { photos[it]?.grid },
                            name = th.peer?.vrc ?: th.peer?.name,
                            size = 58.dp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            th.peer?.vrc ?: th.peer?.name ?: "一颗星",
                            fontSize = 10.5.sp,
                            color = MaterialTheme_color.ink,
                            maxLines = 1,
                        )
                    }
                }
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 点开的大卡：铺满宽度，点空白（对话框外面）关掉。 */
@Composable
fun CardPreviewDialog(
    card: StarCard,
    photoUrl: String?,
    photoGrid: android.graphics.Bitmap?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FlippableStarCard(
                face = card.toFace(),
                photoUrl = photoUrl,
                photoGrid = photoGrid,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f),
                intro = card.intro,
                about = card.about.map { it.q to it.a },
            )
        }
    }
}

/** 深色背景，给对话框一个底。 */
@Composable
fun DimBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xEE05060A))) { content() }
}
