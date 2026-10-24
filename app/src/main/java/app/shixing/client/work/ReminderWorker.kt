package app.shixing.client.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.shixing.client.data.ApiException
import app.shixing.client.data.TokenStore
import app.shixing.client.data.VrccClient
import app.shixing.client.notify.ReminderNotifier

/**
 * 后台提醒。用官方 `/me/notifications` 的游标 `since` 只取增量，不做全量拉取。
 *
 * 三条自我约束：
 * 1. 没有令牌就直接返回，什么都不发；
 * 2. 每次先扣本地日配额，扣不到就跳过（四个个人接口合计每天 300 次的上限）；
 * 3. 首次只记基线不提醒，避免刚连上就弹一堆历史提醒。
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)
        store.token() ?: return Result.success()

        if (!store.takeDailyQuota()) return Result.success()

        val client = VrccClient(token = { store.token() })

        return try {
            val since = store.lastNotif()
            val page = client.notifications(since)

            if (since == null) {
                // 第一次跑：只记基线。
                store.setLastNotif(page.last)
                return Result.success()
            }

            val fresh = page.items.filter { it.id > since }
            if (fresh.isNotEmpty()) {
                ReminderNotifier.show(applicationContext, fresh)
            }
            store.setLastNotif(page.last ?: since)
            Result.success()
        } catch (e: ApiException) {
            when (e.error) {
                // 官方要求：两个提醒接口至少间隔 25 秒，太频繁会 429。
                "too_soon", "too_many" -> Result.retry()
                // 令牌被别处顶掉，或者玩家已经断开：提醒一次即可。
                "gone", "token_missing" -> {
                    ReminderNotifier.expired(applicationContext)
                    Result.success()
                }
                else -> Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
