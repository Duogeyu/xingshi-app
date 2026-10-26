package app.shixing.client.data

import app.shixing.client.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 签名图片（星友圈的图 `/img/…`、玩家照片 `/photo/…`）10 分钟后失效，打开回 `404`
 * （1.9.0 起正文是 `expired`，之前是 `gone`）。这只是这一张图的地址过期了，令牌没事：
 * 重新请求一次列表或照片就有新地址，不要当成断开。
 *
 * 图片由 Coil 直接加载，不经过 [VrccClient]，所以在 Coil 的事件里接住 404，交给界面层重新拿地址。
 */
object SignedImages {

    private val _expired = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** 打不开的签名图片地址。 */
    val expired: SharedFlow<String> = _expired.asSharedFlow()

    fun isSigned(url: String): Boolean =
        url.startsWith(BuildConfig.API_BASE + "img/") || url.startsWith(BuildConfig.API_BASE + "photo/")

    /** Coil 加载失败时调用：只收签名图片的 404。 */
    fun failed(url: String, status: Int?) {
        if (status == 404 && isSigned(url)) _expired.tryEmit(url)
    }
}
