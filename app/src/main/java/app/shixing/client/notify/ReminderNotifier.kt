package app.shixing.client.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.shixing.client.MainActivity
import app.shixing.client.R
import app.shixing.client.data.NotificationItem

/**
 * 后台提醒的本地通知。
 *
 * 这里只负责"把已经拿到的数据摆到通知栏"，不发起任何网络请求 —— 请求在
 * [app.shixing.client.work.ReminderWorker] 里，受配额和频率限制。
 */
object ReminderNotifier {

    private const val CHANNEL_ID = "reminders"
    private const val CHANNEL_NAME = "提醒"
    private const val ID_REMINDERS = 2001
    private const val ID_EXPIRED = 2002

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "有人连向你、有人回你消息时提醒你"
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** 有新提醒时调用。`items` 已按时间倒序，只用前几条。 */
    @SuppressLint("MissingPermission") // canPost() 已经检查过 POST_NOTIFICATIONS
    fun show(context: Context, items: List<NotificationItem>) {
        if (items.isEmpty()) return
        ensureChannel(context)
        if (!canPost(context)) return

        val first = items.first()
        val title = if (items.size == 1) {
            first.title.orEmpty().ifBlank { "拾星" }
        } else {
            "${items.size} 条新提醒"
        }
        val text = if (items.size == 1) {
            first.body.orEmpty().ifBlank { "点开看看" }
        } else {
            items.take(3).joinToString("\n") { "· " + it.body.orEmpty().ifBlank { it.title.orEmpty() } }
        }

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(ID_REMINDERS, n) }
    }

    /** 令牌失效（断开了、重新授权过，或 90 天没用）：只提醒一次，令牌已删，之后后台轮询直接返回。 */
    @SuppressLint("MissingPermission")
    fun expired(context: Context) {
        ensureChannel(context)
        if (!canPost(context)) return

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            1,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("连接已失效")
            .setContentText("断开过或 90 天没用，点开重新连接")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(ID_EXPIRED, n) }
    }
}
