package app.shixing.client.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 客户端侧的限流。官方要求：
 * - 每个网络地址合计每分钟 120 次左右；
 * - 轮询提醒两次之间至少 25 秒，建议按返回的 every（60 秒）；
 * - 星轨/互相连上/最近提醒/所在地图四个接口合计每颗星每天 300 次（见 TokenStore）。
 */
object Throttle {

    private const val MINUTE = 60_000L
    private const val PER_MINUTE = 120

    private val mutex = Mutex()
    private val stamps = ArrayDeque<Long>(PER_MINUTE + 8)
    private val lastAt = HashMap<String, Long>()

    /** 挂起直到允许发下一个请求；key 用来区分不同接口的最小间隔。 */
    suspend fun acquire(key: String, minGapMs: Long) {
        val wait = mutex.withLock {
            val now = System.currentTimeMillis()
            while (stamps.isNotEmpty() && now - stamps.first() >= MINUTE) stamps.removeFirst()
            val gapOfKey = (lastAt[key] ?: 0L) + minGapMs - now
            val gapOfMinute = if (stamps.size >= PER_MINUTE) MINUTE - (now - stamps.first()) else 0L
            val wait = maxOf(gapOfKey, gapOfMinute, 0L)
            if (wait == 0L) {
                stamps.addLast(now)
                lastAt[key] = now
            }
            wait
        }
        if (wait > 0) {
            delay(wait)
            acquire(key, minGapMs)
        }
    }

    /** 429 之后的退避：至少等一分钟，最多等十分钟。 */
    suspend fun backoff(attempt: Int) {
        val ms = minOf(60_000L * (1L shl attempt.coerceIn(0, 4)), 600_000L)
        delay(ms)
    }
}
