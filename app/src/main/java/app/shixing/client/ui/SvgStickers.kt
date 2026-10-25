package app.shixing.client.ui

import android.graphics.Picture
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 官方贴纸接口返回的是 SVG，里面用了 <mask> 和嵌进 mask 的 PNG 噪声（做磨损质感）。
 * AndroidSVG 解不了 mask 里的位图，会整张失败，所以这里先把这两样剔掉再解——
 * 贴纸主体是多条 path，去掉遮罩照样认得出，只是少了那层做旧。
 */
object SvgStickers {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, Picture?>()

    /** 拿一张贴纸；失败返回 null（卡面就只留底色）。 */
    suspend fun picture(key: String, url: String): Picture? {
        cache[key]?.let { return it }
        val pic = withContext(Dispatchers.IO) { load(url) }
        cache[key] = pic
        return pic
    }

    private fun load(url: String): Picture? = try {
        val req = Request.Builder().url(url).header("Accept", "image/svg+xml").build()
        val text = client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            r.body?.string() ?: return null
        }
        // 去掉遮罩与内嵌位图
        val cleaned = text
            .replace(Regex("""<mask[\s\S]*?</mask>"""), "")
            .replace(Regex("""\smask="[^"]*""""), "")
            .replace(Regex("""<image[\s\S]*?/>"""), "")
            .replace(Regex("""<image[\s\S]*?</image>"""), "")
        val svg = SVG.getFromString(cleaned)
        svg.renderToPicture()
    } catch (t: Throwable) {
        null
    }
}
