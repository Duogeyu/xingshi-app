package app.shixing.client.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 用网页自己渲染的那张卡来显示卡面。
 *
 * 不搬站点素材：WebView 里打开 vrcc.cc 的「我的星」页（用本机已有的网页设备密钥自动登录），
 * 等它把卡渲染出来，再把那张 `.mt-mc` 缩放铺满、其它节点藏掉。
 * 卡面永远跟站点同步，也不用抄 CSS/JS。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebStarCard(
    deviceKey: String?,
    modifier: Modifier = Modifier,
    onReady: (Boolean) -> Unit = {},
    onTap: (() -> Unit)? = null,
) {
    val readyState = remember { mutableStateOf(false) }
    var ready by readyState

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.textZoom = 100
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

                var injected = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (!injected) {
                            injected = true
                            if (deviceKey.isNullOrBlank()) {
                                onReady(false)
                                return
                            }
                            // 第一趟：把设备密钥写进站点自己的 localStorage，站点就当我们已登录
                            view.evaluateJavascript(
                                "try{localStorage.setItem('vrcc-match-k'," + jsString(deviceKey) + ")}catch(e){}",
                            ) {
                                view.loadUrl(MATCH_URL)
                            }
                            return
                        }
                        view.evaluateJavascript(SOLO_JS) { ret ->
                            val ok = ret?.contains("ok") == true
                            ready = ok
                            onReady(ok)
                        }
                    }
                }
                if (onTap != null) {
                    setOnTouchListener { _, ev ->
                        if (ev.action == android.view.MotionEvent.ACTION_DOWN) onTap.invoke()
                        true
                    }
                }
                loadUrl(MATCH_URL)
            }
        },
    )
    // 未就绪时保持透明，由外层用原生卡打底
    if (!ready) Unit
}

private const val MATCH_URL = "https://vrcc.cc/match/?tab=me"

private fun jsString(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "") + "\""

/**
 * 等 `.mt-mc` 出现，把它按视口缩放铺满，其它兄弟节点隐藏。
 * 返回 "ok" / "fail"。
 */
private const val SOLO_JS = """
(function(){
  var t0 = Date.now();
  function hideOthers(card){
    var kids = [].slice.call(document.body.children);
    for (var i=0;i<kids.length;i++){
      var c = kids[i];
      if (c === card) continue;
      if (c.contains && c.contains(card)) continue;
      c.style.display = 'none';
    }
    document.documentElement.style.background = 'transparent';
    document.body.style.background = 'transparent';
    document.body.style.overflow = 'hidden';
  }
  function fit(card){
    var r = card.getBoundingClientRect();
    if (!r.width || !r.height) return false;
    var s = Math.min(window.innerWidth / r.width, window.innerHeight / r.height);
    card.style.transformOrigin = '0 0';
    card.style.transform = 'scale(' + s + ') translate(' + (-r.left) + 'px,' + (-r.top) + 'px)';
    card.style.zIndex = '2147483000';
    return true;
  }
  try {
    var card = document.querySelector('.mt-mc');
    if (card && fit(card)) {
      hideOthers(card);
      setTimeout(function(){ fit(card); }, 350);
      setTimeout(function(){ fit(card); }, 900);
      return 'ok';
    }
  } catch (e) { }
  return 'fail';
})()
"""
