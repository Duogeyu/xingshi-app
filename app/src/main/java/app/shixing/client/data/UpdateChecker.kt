package app.shixing.client.data

import android.content.Context
import app.shixing.client.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 查到的新版本。 */
data class UpdateInfo(
    val tag: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val assetUrl: String?,
)

/**
 * 更新检查：只问 GitHub Releases，不经过任何自己的服务器。
 *
 * - 有 release 就用 `/releases/latest`，里面的 `tag_name` 当版本号；
 * - 没有 release（还没发过）就退一步读 `/tags`，拿最新的 tag；
 * - 两种都失败就当作「没查到」，不打扰用户。
 */
object UpdateChecker {

    private const val REPO = "daa0v0/xingshi-app"
    private const val PREFS = "shixing-update"
    private const val KEY_LAST = "last_check"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private fun request(path: String) = Request.Builder()
        .url("https://api.github.com/repos/$REPO/$path")
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "shixing-android/" + BuildConfig.VERSION_NAME)
        .build()

    private fun get(path: String): String? = runCatching {
        client.newCall(request(path)).execute().use { r ->
            if (!r.isSuccessful) null else r.body?.string()
        }
    }.getOrNull()

    /** 今天的日期，用来做「一天最多问一次」。 */
    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

    fun checkedToday(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST, null) == today()

    fun markChecked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST, today()).apply()
    }

    /** 查一次，有比当前新的就返回它。 */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        // 1) 正式 release
        get("releases/latest")?.let { body ->
            runCatching {
                val o = JSONObject(body)
                val tag = o.optString("tag_name").ifBlank { return@runCatching null }
                if (!isNewer(tag, BuildConfig.VERSION_NAME)) return@runCatching null
                var asset: String? = null
                o.optJSONArray("assets")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val a = arr.optJSONObject(i) ?: continue
                        val name = a.optString("name")
                        if (name.endsWith(".apk")) {
                            asset = a.optString("browser_download_url")
                            break
                        }
                    }
                }
                UpdateInfo(
                    tag = tag,
                    title = o.optString("name").ifBlank { tag },
                    notes = o.optString("body"),
                    pageUrl = o.optString("html_url"),
                    assetUrl = asset,
                )
            }.getOrNull()
        } ?: run {
            // 2) 没有 release 就读 tags
            val body = get("tags") ?: return@withContext null
            runCatching {
                val arr = JSONArray(body)
                if (arr.length() == 0) return@runCatching null
                val tag = arr.getJSONObject(0).optString("name")
                if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) return@runCatching null
                UpdateInfo(
                    tag = tag,
                    title = "新版本 " + tag,
                    notes = "",
                    pageUrl = "https://github.com/$REPO/releases",
                    assetUrl = null,
                )
            }.getOrNull()
        }
    }

    /** 版本号比较：把 v1.2.3 拆成数字逐位比，遇到非数字就按字符串比。 */
    fun isNewer(remote: String, local: String): Boolean {
        val a = numbers(remote)
        val b = numbers(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbers(v: String): List<Int> =
        Regex("\\d+").findAll(v.trim().removePrefix("v").removePrefix("V"))
            .map { it.value.toIntOrNull() ?: 0 }
            .toList()
}
