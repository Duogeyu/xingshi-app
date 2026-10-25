package app.shixing.client.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 「你去过这张图但还没打分」的提醒，按世界 + 日期记一次，
 * 关掉之后当天不再弹，第二天再来提醒。
 */
object RateHint {
    private const val PREFS = "shixing-rate-hint"

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

    fun dismissed(context: Context, world: String): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString(world, null) == today()
    }

    fun dismiss(context: Context, world: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(world, today())
            .apply()
    }
}
