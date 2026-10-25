package app.shixing.client.data

/**
 * 把「官方限流 + 本地配额 + 令牌存储」收在一层里，UI 只能经由它访问网络，
 * 避免某个页面绕过限流直接打接口。
 */
class Repository(private val store: TokenStore) {

    val api: VrccClient = VrccClient { store.token() }

    suspend fun storeToken(): String? = store.token()

    suspend fun hasSession(): Boolean = store.token() != null

    // ---- 开放数据（不需要令牌，服务器 5 分钟才更新一次） ----

    suspend fun boards(limit: Int = 100): Boards {
        Throttle.acquire("boards", 5_000)
        return api.boards(limit)
    }

    suspend fun maps(ids: List<String>): Boards {
        Throttle.acquire("maps", 3_000)
        return api.maps(ids)
    }

    // ---- 连接 ----

    suspend fun connect(code: String): ConnectResult {
        Throttle.acquire("connect", 3_000)
        val clean = code.uppercase().filter { it.isLetterOrDigit() }
        require(clean.length == 8) { "连接码是 8 位" }
        val r = api.connect(clean)
        store.saveConnect(r.token, r.me)
        return r
    }

    suspend fun disconnect() {
        runCatching { api.disconnect() }
        store.clear()
    }

    // ---- 设备授权（推荐路径）：客户端出码，玩家在网页上同意 ----

    /** 申请一枚设备码。scope 至少要有 profile。 */
    suspend fun startDeviceFlow(scope: String = DEFAULT_SCOPE): DeviceCode {
        Throttle.acquire("device-code", 3_000)
        return api.deviceCode(scope)
    }

    /** 查一次结果。`authorization_pending` 由调用方吞掉继续等。 */
    suspend fun pollDeviceToken(deviceCode: String): TokenResponse {
        val r = api.deviceToken(deviceCode)
        store.saveConnect(r.accessToken, r.me)
        return r
    }


    // ---- 玩家自己的接口：每次都要先扣本地日配额 ----

    suspend fun recent(mark: Boolean = false): Recent {
        if (!store.takeDailyQuota()) throw ApiException(429, "local_quota")
        Throttle.acquire("recent", 5_000)
        return api.recent(mark)
    }

    suspend fun crossings(mark: Boolean = false): Crossings {
        if (!store.takeDailyQuota()) throw ApiException(429, "local_quota")
        Throttle.acquire("crossings", 5_000)
        return api.crossings(mark)
    }

    suspend fun mutuals(): Mutuals {
        if (!store.takeDailyQuota()) throw ApiException(429, "local_quota")
        Throttle.acquire("mutuals", 5_000)
        return api.mutuals()
    }

    suspend fun myMap(world: String): MyMap {
        if (!store.takeDailyQuota()) throw ApiException(429, "local_quota")
        Throttle.acquire("mymap", 3_000)
        return api.myMap(world)
    }

    /**
     * 轮询提醒。第一次不带 since（只拿 last），之后带上次的 last；
     * 两次之间至少 25 秒，优先按服务器给的 every。
     */
    suspend fun pollNotifications(): Notifications {
        val last = store.lastNotif()
        val sinceLast = System.currentTimeMillis() - store.lastPollAt()
        val gap = if (last == null) 1_000L else 25_000L
        if (sinceLast in 0 until gap) kotlinx.coroutines.delay(gap - sinceLast)
        if (!store.takeDailyQuota()) throw ApiException(429, "local_quota")
        Throttle.acquire("notifications", 25_000)
        val r = api.notifications(last)
        r.last?.let { store.setLastNotif(it) }
        store.setLastPollAt(System.currentTimeMillis())
        return r
    }

    /** 只在玩家点「提交」时调用。 */
    suspend fun rate(world: String, score: Int, text: String?): MyMap {
        Throttle.acquire("rate:$world", 5_000)
        return api.rate(world, score, text)
    }

    // ---- 新增的只读页面（card / today / inbox / posts / chat / 我打过的地图） ----

    suspend fun card(): MyCard {
        Throttle.acquire("card", 10_000)
        return api.card()
    }

    suspend fun myMaps(): MyMapsResp {
        if (!store.takeDailyQuota()) throw ApiException(429, "local_quota")
        Throttle.acquire("mymaps", 10_000)
        return api.myMaps()
    }

    suspend fun today(): TodayResp {
        Throttle.acquire("today", 10_000)
        return api.today()
    }

    suspend fun inbox(mark: Boolean = false): InboxResp {
        Throttle.acquire("inbox", 5_000)
        return api.inbox(mark)
    }

    /** 只有玩家点「连上」时才会走到这里。 */
    suspend fun link(to: Long, via: String? = null, post: Long? = null): LinkResp {
        Throttle.acquire("link:$to", 3_000)
        return api.link(to, via, post)
    }

    /** 连上之后附一句话（一说一句）。 */
    suspend fun say(n: Long, text: String): kotlinx.serialization.json.JsonObject {
        Throttle.acquire("say:$n", 3_000)
        return api.say(n, text)
    }

    /** TA 的照片地址；拿不到清晰图就返回 null。 */
    suspend fun photoUrl(n: Long): String? = runCatching {
        val o = api.photo(n, 2)
        val level = (o["level"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 0
        if (level < 2) null else (o["url"] as? kotlinx.serialization.json.JsonPrimitive)?.content
    }.getOrNull()

    suspend fun chats(): ChatsResp {
        Throttle.acquire("chats", 10_000)
        return api.chats()
    }

    suspend fun chat(n: Long, before: Long? = null): ChatResp {
        Throttle.acquire("chat:$n", 3_000)
        return api.chat(n, before)
    }

    /** 只在玩家点「发送」时调用。 */
    suspend fun sendChat(n: Long, text: String): ChatResp {
        Throttle.acquire("send:$n", 2_000)
        return api.sendChat(n, text)
    }

    suspend fun posts(f: String = "all", before: Long? = null): PostsResp {
        Throttle.acquire("posts:$f", 5_000)
        return api.posts(f, before)
    }

    /** 点亮/取消。写操作只在玩家点击时调用。 */
    suspend fun light(id: Long, on: Boolean = true): kotlinx.serialization.json.JsonObject {
        Throttle.acquire("light", 2_000)
        return api.light(id, on)
    }

    /** 发一条星友圈（每个令牌每天 5 条，超了服务端回 429）。 */
    suspend fun writePost(text: String): kotlinx.serialization.json.JsonObject {
        require(text.isNotBlank()) { "内容不能是空的" }
        Throttle.acquire("write-post", 5_000)
        return api.writePost(text)
    }

    suspend fun session(): Pair<Int?, String?> = store.star() to store.vrc()

    suspend fun dailyUsed(): Int = store.dailyUsed()

    companion object {
        /**
         * 要申请的权限。
         * - 普通：profile、card、maps、notifications
         * - 敏感：crossings、mutuals、maps.rate、today、inbox、link、chat、chat.send、posts
         *   未审核的自助登记应用里，敏感权限只有登记人本人能同意。
         */
        const val DEFAULT_SCOPE =
            "profile card maps maps.rate notifications crossings mutuals today inbox link chat chat.send posts"
    }
}
