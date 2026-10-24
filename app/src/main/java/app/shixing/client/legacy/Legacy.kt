package app.shixing.client.legacy

import android.content.Context
import android.util.Log
import app.shixing.client.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 网页内部接口（`/api/match`）的薄封装。
 *
 * 只用于「装扮 / 仓库 / 兑换码 / 账号」这类官方 v1 还没有的能力。
 * 文档把这一组留给官方脚本，所以界面上必须显式标注来源与风险，
 * 并且只有在玩家自己登录过网页、本机存在设备密钥时才会工作。
 */
class LegacyClient(
    private val keyProvider: () -> String?,
    private val base: String = BuildConfig.LEGACY_BASE,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** 一次动作调用；`fields` 会平铺到请求体里（和网页端的调用方式一致）。 */
    fun call(action: String, fields: Map<String, Any?> = emptyMap()): JsonObject {
        val key = keyProvider() ?: throw LegacyError("no_key")
        val body = buildJsonObject {
            put("a", JsonPrimitive(action))
            put("k", JsonPrimitive(key))
            fields.forEach { (name, value) ->
                when (value) {
                    null -> Unit
                    is Boolean -> put(name, JsonPrimitive(value))
                    is Number -> put(name, JsonPrimitive(value))
                    is String -> put(name, JsonPrimitive(value))
                    is JsonObject -> put(name, value)
                    is JsonArray -> put(name, value)
                    else -> put(name, JsonPrimitive(value.toString()))
                }
            }
        }

        val request = Request.Builder()
            .url(base)
            .header("X-Client", BuildConfig.CLIENT_ID)
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (text.isBlank()) {
                    throw LegacyError(if (response.isSuccessful) "empty" else "http_${response.code}")
                }
                val obj = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
                    ?: throw LegacyError("bad_json")
                val error = (obj["error"] as? JsonPrimitive)?.content
                if (!response.isSuccessful || !error.isNullOrBlank()) {
                    throw LegacyError(if (error.isNullOrBlank()) "http_${response.code}" else error)
                }
                obj
            }
        } catch (e: LegacyError) {
            throw e
        } catch (e: Exception) {
            Log.w("Legacy", "$action failed", e)
            throw LegacyError(e.javaClass.simpleName)
        }
    }

    fun boot(): JsonObject = call("boot")

    fun bag(): JsonObject = call("bag").childObj("bag")

    fun redeem(code: String): JsonObject =
        call("redeem", mapOf("code" to code.trim().uppercase()))

    fun cdk(): JsonObject = call("cdk").childObj("cdk")

    fun keys(): JsonObject = call("keys")

    fun dropKey(id: String): JsonObject = call("key-drop", mapOf("id" to id))

    fun me(): JsonObject = call("me")

    /** 批量取星的照片；返回 {photos:[{status,img,level}]}。 */
    fun photos(items: List<Pair<Long, Int>>): JsonArray {
        val list = JsonArray(
            items.map { (to, want) ->
                buildJsonObject {
                    put("to", JsonPrimitive(to))
                    put("want", JsonPrimitive(want))
                }
            },
        )
        return (call("photos", mapOf("list" to list))["photos"] as? JsonArray) ?: JsonArray(emptyList())
    }

    fun leave(): JsonObject = call("leave")

    fun privacy(value: JsonObject): JsonObject = call("privacy", mapOf("privacy" to value))

    fun deco(deco: JsonObject): JsonObject = call("deco", mapOf("deco" to deco))

    fun pack(kind: String): JsonObject = call("pack", mapOf("kind" to kind))

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

class LegacyError(val code: String) : Exception(code) {
    val text: String
        get() = when (code) {
            "no_key" -> "还没连接网页账号"
            "no_star" -> "这个账号还没有星"
            "bad_code", "used", "expired", "invalid" -> "兑换码不对或已经用过"
            "too_many" -> "太频繁了，过一会儿再试"
            "stop" -> "站点暂停服务"
            else -> code
        }
}

fun JsonObject.childObj(name: String): JsonObject =
    this[name] as? JsonObject ?: JsonObject(emptyMap())

fun JsonObject.str(name: String): String? = (this[name] as? JsonPrimitive)?.content

fun JsonObject.int(name: String): Int? = (this[name] as? JsonPrimitive)?.content?.toIntOrNull()

/** 设备密钥（网页的 `vrcc-match-k`）只存在本机。 */
object LegacyKey {
    private const val PREFS = "shixing-legacy"
    private const val NAME = "device_key"

    fun get(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NAME, null)

    fun set(context: Context, value: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (value.isNullOrBlank()) prefs.edit().remove(NAME).apply()
        else prefs.edit().putString(NAME, value.trim()).apply()
    }

    fun valid(value: String?): Boolean = !value.isNullOrBlank() && value.length in 8..128
}
