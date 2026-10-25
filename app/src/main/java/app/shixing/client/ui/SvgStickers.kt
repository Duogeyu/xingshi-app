package app.shixing.client.ui

import android.graphics.Picture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import app.shixing.client.data.DecoSpot
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 官方贴纸是 SVG，里面用 mask 夹了一张 PNG 噪声做磨损质感；AndroidSVG 解不了
 * mask 里的位图，会整张失败。这里先把 mask 那几段剔掉再解，贴纸主体是路径，
 * 少了做旧层照样认得出来。
 */
object SvgStickers {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, Picture>()

    suspend fun picture(key: String, url: String): Picture? {
        cache[key]?.let { return it }
        val pic = withContext(Dispatchers.IO) { load(url) }
        if (pic != null) cache[key] = pic
        return pic
    }

    private fun load(url: String): Picture? = try {
        val req = Request.Builder().url(url).header("Accept", "image/svg+xml").build()
        val text = client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            r.body?.string()
        } ?: return null
        val cleaned = text
            .replace(Regex("<mask[\\s\\S]*?</mask>"), "")
            .replace("mask=", "data-mask=")
            .replace(Regex("<image[\\s\\S]*?/>"), "")
            .replace(Regex("<image[\\s\\S]*?</image>"), "")
        SVG.getFromString(cleaned).renderToPicture()
    } catch (t: Throwable) {
        null
    }
}

/** 卡面上的贴纸：位置和大小来自 deco.spots（百分比），图形走官方贴纸接口。 */
@Composable
fun StickerLayer(spots: List<DecoSpot>, modifier: Modifier = Modifier) {
    if (spots.isEmpty()) return
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        spots.forEach { s ->
            val id = s.id ?: return@forEach
            val box = w * (s.size / 100f)
            val key = id + "/" + s.fin + "/" + s.wear + "/" + s.seed
            val url = "https://vrcc.cc/api/v1/stickers/" + id +
                "?fin=" + (s.fin ?: "paper") + "&wear=" + s.wear + "&seed=" + s.seed
            val pic by produceState<Picture?>(initialValue = null, key1 = key) {
                value = SvgStickers.picture(key, url)
            }
            Box(
                Modifier
                    .offset(x = w * (s.x / 100f) - box / 2, y = h * (s.y / 100f) - box / 2)
                    .size(box)
                    .rotate(s.rot),
            ) {
                val p = pic ?: return@Box
                Canvas(Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        native.save()
                        native.scale(size.width / 124f, size.height / 124f)
                        native.translate(12f, 12f)
                        native.drawPicture(p)
                        native.restore()
                    }
                }
            }
        }
    }
}
