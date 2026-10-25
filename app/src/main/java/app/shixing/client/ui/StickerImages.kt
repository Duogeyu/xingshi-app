package app.shixing.client.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.shixing.client.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 贴纸图片：直接拿字节自己解码。
 *
 * 走自己的 OkHttp 而不是 Coil，原因有两条：
 * 1. /api/v1 要求每个请求带 X-Client，Coil 默认不带；
 * 2. 接口还回来的其实是 webp（不是文档里写的 svg），我们只要字节解码就行。
 */
object StickerImages {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, Bitmap>()

    @Volatile
    var lastError: String? = null

    /** 当前令牌：贴纸接口也要它，登录/连接成功后由 ViewModel 写进来。 */
    @Volatile
    var token: String? = null

    suspend fun bitmap(url: String): Bitmap? {
        cache[url]?.let { return it }
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val b = Request.Builder()
                    .url(url)
                    .header("X-Client", BuildConfig.CLIENT_ID)
                token?.let { b.header("Authorization", "Bearer $it") }
                val req = b.build()
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) {
                        lastError = "HTTP " + r.code
                        return@use null
                    }
                    val bytes = r.body?.bytes()
                    if (bytes == null) {
                        lastError = "no body"
                        return@use null
                    }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp == null) lastError = "decode fail " + bytes.size + " head=" + String(bytes, 0, 4)
                    bmp
                }
            }.onFailure { lastError = it.javaClass.simpleName + ": " + it.message }.getOrNull()
        }
        if (bmp != null) cache[url] = bmp
        return bmp
    }
}
