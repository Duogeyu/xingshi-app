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

    suspend fun session(): Pair<Int?, String?> = store.star() to store.vrc()

    suspend fun dailyUsed(): Int = store.dailyUsed()
}
