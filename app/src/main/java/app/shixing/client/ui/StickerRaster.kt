package app.shixing.client.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * 贴纸的「自己的轮子」：官方贴纸接口给的是 SVG，里面用了 mask + 内嵌位图，
 * AndroidSVG 解不了。这里用一只离屏 WebView 把 SVG 自己画出来，再截成位图缓存。
 *
 * WebView 必须挂在窗口上才画得出东西，所以由 [host] 在 Activity 里放一个 1px 的壳。
 */
object StickerRaster {

    private const val SIZE = 132

    private var web: WebView? = null
    private val cache = ConcurrentHashMap<String, Bitmap?>()
    private val main = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    fun host(context: Context, parent: ViewGroup) {
        if (web != null) return
        val w = WebView(context.applicationContext)
        w.settings.javaScriptEnabled = false
        w.settings.allowFileAccess = false
        w.settings.allowContentAccess = false
        w.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        w.alpha = 0.01f
        w.measure(
            View.MeasureSpec.makeMeasureSpec(1, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1, View.MeasureSpec.EXACTLY),
        )
        w.layout(0, 0, 1, 1)
        parent.addView(
            w,
            ViewGroup.LayoutParams(1, 1),
        )
        web = w
    }

    suspend fun bitmap(key: String, url: String): Bitmap? {
        cache[key]?.let { return it }
        val bmp = withContext(Dispatchers.Main) { render(url) }
        // ConcurrentHashMap 不收 null，失败的就别放缓存，下次再试
        if (bmp != null) cache[key] = bmp
        return bmp
    }

    private suspend fun render(url: String): Bitmap? {
        val w = web ?: return null
        val html = """
            <html><head><meta name="viewport" content="width=$SIZE,height=$SIZE">
            <style>html,body{margin:0;padding:0;background:transparent;overflow:hidden}
            img{width:${SIZE}px;height:${SIZE}px;object-fit:contain;display:block}</style></head>
            <body><img src="$url"></body></html>
        """.trimIndent()

        val done = withTimeoutOrNull(4000L) {
            suspendCancellableCoroutine { cont ->
                w.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, urlStr: String?) {
                        // 图片解码还要一点时间，给两帧
                        main.postDelayed({
                            if (cont.isActive) cont.resume(Unit)
                        }, 320)
                    }
                }
                w.loadDataWithBaseURL("https://vrcc.cc", html, "text/html", "utf-8", null)
            }
        }
        if (done == null) return null

        w.measure(
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
        )
        w.layout(0, 0, SIZE, SIZE)

        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        w.draw(c)
        return bmp
    }
}
