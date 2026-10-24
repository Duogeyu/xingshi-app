package app.shixing.client.data

import app.shixing.client.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** 官方接口返回的错误。`error` 是文档里那套错误码。 */
class ApiException(
    val status: Int,
    val error: String,
    val why: String? = null,
    val need: String? = null,
) : Exception("HTTP $status $error")

/**
 * `/api/v1` 的唯一出口。
 *
 * 合规约束（官方《使用要求》）：
 * - 每个请求都带 `X-Client`，标识在 BuildConfig.CLIENT_ID 里；
 * - 只允许相对路径，基址写死 https://vrcc.cc/api/v1/，令牌不可能被发到别处；
 * - 写操作只由玩家点击触发，这里不排队、不重试、不批量。
 */
class VrccClient(
    private val base: String = BuildConfig.API_BASE,
    private val token: suspend () -> String?,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private suspend fun buildRequest(
        method: String,
        path: String,
        body: String?,
        authed: Boolean,
    ): Request {
        // 硬约束：只拼接相对路径。任何 "https://…" 都会在这里炸掉，
        // 防止以后有人不小心把令牌发到第三方地址。
        require(!path.startsWith("http", ignoreCase = true) && !path.startsWith("//")) {
            "只允许 /api/v1/ 下的相对路径"
        }
        val b = Request.Builder()
            .url(base + path)
            .header("X-Client", BuildConfig.CLIENT_ID)
            .header("Accept", "application/json")
        if (authed) {
            val t = token() ?: throw ApiException(401, "token_missing")
            b.header("Authorization", "Bearer $t")
        }
        when (method) {
            "GET" -> b.get()
            "POST" -> b.post((body ?: "{}").toRequestBody(JSON_TYPE))
            else -> throw IllegalArgumentException("method $method")
        }
        return b.build()
    }

    private suspend fun errorOf(status: Int, text: String): ApiException {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        val err = obj?.get("error")?.jsonPrimitive?.contentOrNullSafe() ?: "http_$status"
        val why = obj?.get("why")?.jsonPrimitive?.contentOrNullSafe()
        val need = obj?.get("need")?.jsonPrimitive?.contentOrNullSafe()
        return ApiException(status, err, why, need)
    }

    private suspend fun <T> call(
        method: String,
        path: String,
        body: String?,
        authed: Boolean,
        ser: DeserializationStrategy<T>,
    ): T = withContext(Dispatchers.IO) {
        val req = buildRequest(method, path, body, authed)
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw errorOf(resp.code, text)
            json.decodeFromString(ser, text)
        }
    }

    // ---- 开放数据 ----

    suspend fun boards(limit: Int = 100): Boards =
        call("GET", "boards/maps?limit=${limit.coerceIn(1, 100)}", null, false, Boards.serializer())

    suspend fun maps(ids: List<String>): Boards =
        call("GET", "maps?ids=" + ids.joinToString(","), null, false, Boards.serializer())

    // ---- 连接 ----

    suspend fun connect(code: String): ConnectResult {
        val body = buildJsonObject { put("code", JsonPrimitive(code)) }.toString()
        return call("POST", "connect", body, false, ConnectResult.serializer())
    }

    suspend fun disconnect(): JsonObject =
        call("POST", "disconnect", null, true, JsonObject.serializer())

    // ---- 设备授权（RFC 8628，推荐）----

    suspend fun deviceCode(scope: String): DeviceCode {
        val body = buildJsonObject { put("scope", JsonPrimitive(scope)) }.toString()
        return call("POST", "device/code", body, false, DeviceCode.serializer())
    }

    /**
     * 用连接码换令牌。玩家还没点同意时抛 `ApiException(400, "authorization_pending")`，
     * 调用方按 `interval` 秒再来问；`access_denied` / `expired_token` 要停下。
     */
    suspend fun deviceToken(deviceCode: String): TokenResponse {
        val body = buildJsonObject { put("device_code", JsonPrimitive(deviceCode)) }.toString()
        return call("POST", "device/token", body, false, TokenResponse.serializer())
    }


    // ---- 玩家自己的接口 ----

    suspend fun notifications(since: Long?): Notifications =
        call(
            "GET",
            if (since == null) "me/notifications" else "me/notifications?since=$since",
            null,
            true,
            Notifications.serializer(),
        )

    suspend fun recent(mark: Boolean = false): Recent =
        call(
            "GET",
            if (mark) "me/notifications/recent?mark=1" else "me/notifications/recent",
            null,
            true,
            Recent.serializer(),
        )

    suspend fun crossings(mark: Boolean = false): Crossings =
        call(
            "GET",
            if (mark) "me/crossings?mark=1" else "me/crossings",
            null,
            true,
            Crossings.serializer(),
        )

    suspend fun mutuals(): Mutuals =
        call("GET", "me/mutuals", null, true, Mutuals.serializer())

    suspend fun myMap(world: String): MyMap =
        call("GET", "me/maps/$world", null, true, MyMap.serializer())

    /** 只能在玩家主动点击时调用。`text` 传 "" 表示删掉已有短评，传 null 表示只改分。 */
    suspend fun rate(world: String, score: Int, text: String?): MyMap {
        val body = buildJsonObject {
            put("score", JsonPrimitive(score.coerceIn(1, 10)))
            if (text != null) put("text", JsonPrimitive(text.take(30)))
        }.toString()
        return call("POST", "me/maps/$world/rating", body, true, MyMap.serializer())
    }

    // ---- 星卡 / 今天 / 连向我 / 星友圈 / 私聊（scope: card today inbox posts chat）----

    suspend fun card(): MyCard =
        call("GET", "me/card", null, true, MyCard.serializer())

    suspend fun myMaps(): MyMapsResp =
        call("GET", "me/maps", null, true, MyMapsResp.serializer())

    suspend fun today(): TodayResp =
        call("GET", "me/today", null, true, TodayResp.serializer())

    suspend fun inbox(mark: Boolean = false): InboxResp =
        call(
            "GET",
            if (mark) "me/inbox?mark=1" else "me/inbox",
            null,
            true,
            InboxResp.serializer(),
        )

    /** 只在玩家主动点「连上」时调用。`via` 只能是 cross（星轨）或 yq（星友圈，要带 post）。 */
    suspend fun link(to: Long, via: String? = null, post: Long? = null): LinkResp {
        val body = buildJsonObject {
            put("to", JsonPrimitive(to))
            if (via != null) put("via", JsonPrimitive(via))
            if (post != null) put("post", JsonPrimitive(post))
        }.toString()
        return call("POST", "me/links", body, true, LinkResp.serializer())
    }

    suspend fun chats(): ChatsResp =
        call("GET", "me/chats", null, true, ChatsResp.serializer())

    suspend fun chat(n: Long, before: Long? = null): ChatResp =
        call(
            "GET",
            if (before == null) "me/chats/$n" else "me/chats/$n?before=$before",
            null,
            true,
            ChatResp.serializer(),
        )

    /** 只在玩家点「发送」时调用，且只能发给互相连上的人。 */
    suspend fun sendChat(n: Long, text: String): ChatResp {
        val body = buildJsonObject { put("text", JsonPrimitive(text)) }.toString()
        return call("POST", "me/chats/$n", body, true, ChatResp.serializer())
    }

    suspend fun posts(f: String = "all", before: Long? = null): PostsResp {
        val q = buildList {
            if (f != "all") add("f=$f")
            if (before != null) add("before=$before")
        }.joinToString("&")
        return call(
            "GET",
            if (q.isEmpty()) "me/posts" else "me/posts?$q",
            null,
            true,
            PostsResp.serializer(),
        )
    }

    private companion object {
        val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
    if (this is kotlinx.serialization.json.JsonNull) null else this.content
