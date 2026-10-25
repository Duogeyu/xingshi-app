package app.shixing.client.ui

import app.shixing.client.data.ApiException
import app.shixing.client.data.BoardMap
import app.shixing.client.data.CrossStar
import app.shixing.client.data.DeviceCode
import app.shixing.client.data.InboxResp
import app.shixing.client.data.MyCard
import app.shixing.client.data.MyRating
import app.shixing.client.data.MutualStar
import app.shixing.client.data.NotificationItem
import app.shixing.client.data.Post
import app.shixing.client.data.Repository
import app.shixing.client.data.Thread
import app.shixing.client.data.TodayResp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val session: Boolean = false,
    val star: Int? = null,
    val vrc: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val hint: String? = null,
    val boards: List<BoardMap> = emptyList(),
    val recent: List<NotificationItem> = emptyList(),
    val readId: Long? = null,
    val dot: Int = 0,
    val crossings: List<CrossStar> = emptyList(),
    val crossingsWhy: String? = null,
    val mutuals: List<MutualStar> = emptyList(),
    val dailyUsed: Int = 0,
    /** 正在等玩家在网页上同意时，这张卡片显示 user_code。 */
    val device: DeviceCode? = null,
    /** 设备码还剩下的秒数（用于「还剩 8:32」）。 */
    val deviceLeft: Int = 0,
    // ---- 新接口 ----
    val today: TodayResp? = null,
    val inbox: InboxResp? = null,
    val posts: List<Post> = emptyList(),
    val card: MyCard? = null,
    val myMaps: List<MyRating> = emptyList(),
    val threads: List<Thread> = emptyList(),
    /** 每个页面各自的问题（今天/连向我/星友圈/我的），不互相顶掉。 */
    val pageErrors: Map<String, String> = emptyMap(),
)


class MainViewModel(private val repo: Repository) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val token = repo.storeToken()
            if (token != null) {
                val (star, vrc) = repo.session()
                _state.update { it.copy(session = true, star = star, vrc = vrc) }
                refreshAll()
            } else {
                loadBoards()
            }
        }
    }

    private fun loadBoards() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { repo.boards() }
            .onSuccess { r -> _state.update { it.copy(boards = r.maps, loading = false) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = describe(e)) } }
    }

    fun refreshAll() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        // 每一页单独记错误，一个权限没申请到不影响其它页。
        val errs = mutableMapOf<String, String>()

        runCatching { repo.today() }.onSuccess { t ->
            _state.update { it.copy(today = t) }
        }.onFailure { errs["today"] = describe(it) }

        runCatching { repo.inbox() }.onSuccess { i ->
            _state.update { it.copy(inbox = i) }
        }.onFailure { errs["inbox"] = describe(it) }

        runCatching { repo.posts() }.onSuccess { p ->
            _state.update { it.copy(posts = p.posts) }
        }.onFailure { errs["posts"] = describe(it) }

        runCatching { repo.card() }.onSuccess { c ->
            _state.update { it.copy(card = c) }
        }.onFailure { errs["card"] = describe(it) }

        runCatching { repo.myMaps() }.onSuccess { m ->
            _state.update { it.copy(myMaps = m.maps) }
        }.onFailure { errs["mymaps"] = describe(it) }

        runCatching { repo.chats() }.onSuccess { c ->
            _state.update { it.copy(threads = c.threads) }
        }.onFailure { errs["chats"] = describe(it) }

        runCatching { repo.recent() }.onSuccess { r ->
            _state.update { it.copy(recent = r.items, readId = r.read) }
        }.onFailure { errs["recent"] = describe(it) }

        runCatching { repo.crossings() }.onSuccess { c ->
            _state.update { it.copy(crossings = c.stars, crossingsWhy = c.why, dot = c.n ?: 0) }
        }.onFailure { errs["crossings"] = describe(it) }

        runCatching { repo.mutuals() }.onSuccess { m ->
            _state.update { it.copy(mutuals = m.stars) }
        }.onFailure { errs["mutuals"] = describe(it) }

        runCatching { repo.boards() }.onSuccess { b ->
            _state.update { it.copy(boards = b.maps) }
        }.onFailure { if (errs.isEmpty()) errs["boards"] = describe(it) }

        _state.update {
            it.copy(
                loading = false,
                error = null,
                pageErrors = errs,
                dailyUsed = repo.dailyUsed(),
            )
        }
    }

    /** 切到哪个页签就刷哪个，顺手把这一页的旧报错清掉。 */
    fun refreshTab(index: Int) {
        when (index) {
            0 -> refreshToday()
            1 -> refreshInbox()
            2 -> refreshCrossings()
            3 -> refreshPosts()
            else -> {
                refreshChats()
                refreshCard()
            }
        }
    }

    fun refreshToday() = viewModelScope.launch {
        runCatching { repo.today() }
            .onSuccess { t ->
                android.util.Log.d("shixing-deco", "v1 today deco=" + t.cards.take(4).map { c -> c.n to c.deco })
                _state.update { it.copy(today = t, pageErrors = it.pageErrors - "today") }
            }
            .onFailure { e -> _state.update { it.copy(pageErrors = it.pageErrors + ("today" to describe(e))) } }
    }

    fun refreshInbox() = viewModelScope.launch {
        runCatching { repo.inbox(mark = true) }
            .onSuccess { i -> _state.update { it.copy(inbox = i, pageErrors = it.pageErrors - "inbox") } }
            .onFailure { e -> _state.update { it.copy(pageErrors = it.pageErrors + ("inbox" to describe(e))) } }
    }

    fun refreshPosts(filter: String = "all") = viewModelScope.launch {
        runCatching { repo.posts(filter) }
            .onSuccess { p -> _state.update { it.copy(posts = p.posts, pageErrors = it.pageErrors - "posts") } }
            .onFailure { e -> _state.update { it.copy(pageErrors = it.pageErrors + ("posts" to describe(e))) } }
    }

    fun refreshChats() = viewModelScope.launch {
        runCatching { repo.chats() }
            .onSuccess { c -> _state.update { it.copy(threads = c.threads, pageErrors = it.pageErrors - "chats") } }
            .onFailure { e -> _state.update { it.copy(pageErrors = it.pageErrors + ("chats" to describe(e))) } }
    }

    fun refreshCrossings() = viewModelScope.launch {
        runCatching { repo.crossings() }
            .onSuccess { c ->
                _state.update {
                    it.copy(
                        crossings = c.stars,
                        crossingsWhy = c.why,
                        dot = c.n ?: 0,
                        pageErrors = it.pageErrors - "crossings",
                    )
                }
            }
            .onFailure { e ->
                _state.update { it.copy(pageErrors = it.pageErrors + ("crossings" to describe(e))) }
            }
    }

    fun refreshCard() = viewModelScope.launch {
        runCatching { repo.card() }
            .onSuccess { c -> _state.update { it.copy(card = c, pageErrors = it.pageErrors - "card") } }
            .onFailure { e -> _state.update { it.copy(pageErrors = it.pageErrors + ("card" to describe(e))) } }
    }

    /** 点亮一条动态：先本地加一，失败就把数字改回去。 */
    fun light(id: Long, on: Boolean = true) = viewModelScope.launch {
        _state.update { s: UiState ->
            s.copy(posts = s.posts.map { p -> if (p.id == id) p.copy(lit = p.lit + if (on) 1 else -1) else p })
        }
        runCatching { repo.light(id, on) }
            .onSuccess { showHint("已点亮") }
            .onFailure { e ->
                _state.update { s: UiState ->
                    s.copy(posts = s.posts.map { p -> if (p.id == id) p.copy(lit = p.lit - if (on) 1 else -1) else p })
                }
                showHint(describe(e))
            }
    }

    /** 发一条星友圈。onDone 回 "ok" 或错误文案。 */
    fun writePost(text: String, onDone: (String) -> Unit) = viewModelScope.launch {
        runCatching { repo.writePost(text) }
            .onSuccess {
                onDone("ok")
                showHint("发出去了")
                refreshPosts()
            }
            .onFailure { e -> onDone(describe(e)) }
    }

    /** 玩家点了某颗星的「连上」。只能连网页允许的星。 */
    fun link(to: Long, via: String? = null, post: Long? = null, onDone: (String) -> Unit) =
        viewModelScope.launch {
            runCatching { repo.link(to, via, post) }
                .onSuccess { r ->
                    onDone(if (r.mutual) "你们互相连上了" else "已连向 ${r.card?.name ?: "这颗星"}")
                    refreshInbox()
                    refreshToday()
                }
                .onFailure { e -> onDone(describe(e)) }
        }

    /** 打开一段私聊；返回 null 表示失败了（错误已经进 pageErrors）。 */
    suspend fun openChat(n: Long, before: Long? = null): app.shixing.client.data.ChatResp? =
        runCatching { repo.chat(n, before) }.getOrNull()

    /** 只在玩家点「发送」时调用。 */
    fun send(n: Long, text: String, onDone: (String) -> Unit) = viewModelScope.launch {
        val t = text.trim()
        if (t.isEmpty()) return@launch
        runCatching { repo.sendChat(n, t) }
            .onSuccess { onDone("") }
            .onFailure { e -> onDone(describe(e)) }
    }


    fun connect(code: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, hint = null) }
        runCatching { repo.connect(code) }
            .onSuccess { r ->
                _state.update {
                    it.copy(
                        session = true,
                        star = r.me?.star,
                        vrc = r.me?.vrc,
                        loading = false,
                        hint = "已连接",
                    )
                }
                refreshAll()
            }
            .onFailure { e -> _state.update { it.copy(loading = false, error = describe(e)) } }
    }

    fun disconnect() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        repo.disconnect()
        _state.update {
            UiState(session = false, boards = it.boards, hint = "已断开，令牌已从本机删除")
        }
    }

    // ---- 设备授权 ----

    private var pollJob: Job? = null

    /** 申请设备码，然后把 user_code 显示出来等玩家在网页上同意。 */
    fun startDevice() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, hint = null) }
            val dc = runCatching { repo.startDeviceFlow() }
                .onFailure { e -> _state.update { it.copy(loading = false, error = describe(e)) } }
                .getOrNull() ?: return@launch
            _state.update { it.copy(loading = false, device = dc, deviceLeft = dc.expiresIn) }
            // 服务器只给了 profile：说明敏感权限没批下来，先让玩家知道，别让人对着空白页猜。
            val granted = dc.scope.orEmpty()
            if (granted.isNotBlank() && !granted.contains("crossings")) {
                _state.update {
                    it.copy(hint = "服务端这次只授予了《$granted》。星轨/互相连上要等应用审核通过后才能申请")
                }
            }
            val startedAt = System.currentTimeMillis()
            while (true) {
                delay(dc.interval.coerceAtLeast(3) * 1000L)
                val left = dc.expiresIn - ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                if (left <= 0) {
                    _state.update { it.copy(device = null, deviceLeft = 0, error = "连接码过期了，重新获取一枚") }
                    return@launch
                }
                _state.update { it.copy(deviceLeft = left) }
                try {
                    val r = repo.pollDeviceToken(dc.deviceCode)
                    _state.update {
                        it.copy(
                            session = true,
                            star = r.me?.star,
                            vrc = r.me?.vrc,
                            device = null,
                            deviceLeft = 0,
                            hint = "已连接",
                        )
                    }
                    refreshAll()
                    return@launch
                } catch (e: ApiException) {
                    when (e.error) {
                        "authorization_pending" -> Unit // 继续等
                        "access_denied" -> {
                            _state.update { it.copy(device = null, error = "你在网页上点了取消") }
                            return@launch
                        }
                        else -> {
                            _state.update { it.copy(device = null, error = describe(e)) }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(device = null, error = describe(e)) }
                    return@launch
                }
            }
        }
    }

    fun cancelDevice() {
        pollJob?.cancel()
        _state.update { it.copy(device = null, deviceLeft = 0) }
    }

    fun markCrossingsRead() = viewModelScope.launch {
        runCatching { repo.crossings(mark = true) }
            .onSuccess { c -> _state.update { it.copy(crossings = c.stars, crossingsWhy = c.why, dot = 0) } }
    }

    fun markNotificationsRead() = viewModelScope.launch {
        runCatching { repo.recent(mark = true) }
            .onSuccess { r -> _state.update { it.copy(recent = r.items, readId = r.read) } }
    }

    /** 打开一张地图的「我的评分」。会消耗一次个人接口配额。 */
    fun openMap(world: String, onResult: (app.shixing.client.data.MyMap?) -> Unit) =
        viewModelScope.launch {
            runCatching { repo.myMap(world) }
                .onSuccess { onResult(it) }
                .onFailure { e ->
                    onResult(null)
                    _state.update { it.copy(error = describe(e)) }
                }
        }

    /** 只由玩家点击调用；text 传 "" 表示删掉短评。 */
    fun rate(world: String, score: Int, text: String?, onDone: (String) -> Unit) =
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repo.rate(world, score, text) }
                .onSuccess { m ->
                    _state.update { it.copy(loading = false) }
                    refreshAll()
                    onDone("已记录：你的评分 ${m.mine ?: score}")
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false) }
                    onDone(describe(e))
                }
        }

    fun clearHint() = _state.update { it.copy(hint = null) }

    fun showHint(text: String) = _state.update { it.copy(hint = text) }

    companion object {
        fun factory(repo: Repository) = viewModelFactory {
            initializer { MainViewModel(repo) }
        }
    }
}

/** 把接口错误翻成人话，顺带把官方的 need/why 提示带出来。 */
fun describe(e: Throwable): String {
    val api = e as? ApiException
    if (api != null) {
        return when (api.error) {
            "too_soon" -> "问得太快了，等一分钟再试"
            "too_many" -> "今天的次数用完了，明天再来"
            "busy" -> "今天输错的连接码太多，连接功能明天恢复"
            "local_quota" -> "本地日配额已用满（每天 300 次），明天再来"
            "code" -> "连接码不对、已过期或已经用过"
            "token_missing", "gone" -> "连接已失效，请重新连接"
            "authorization_pending" -> "还在等你同意"
            "access_denied" -> "你在网页上点了取消"
            "expired_token" -> "连接码过期了，重新获取一枚"
            "invalid_scope" -> "申请了不认识的权限"
            "client_missing", "client_unknown", "client_off" ->
                "客户端标识没被接受（${api.error}）。到网页「我的资料 → 第三方客户端」确认「我登记的应用」里还有「拾星」，名字改过会回到未审核"
            "client_mismatch" -> "客户端标识和取码时不一致"
            "scope_unreviewed" -> "敏感权限还没通过审核，这个账号暂时不能同意"
            "map_not_went" -> when (api.need) {
                "verify" -> "先验证 VRChat 账号才能打分"
                "friend" -> "先同意机器人的好友请求"
                "go" -> "要先在这张地图上待一会儿"
                else -> "现在还不能给这张地图打分"
            }
            "map_elsewhere" -> "机器人看到你现在在别的地图"
            "map_off", "xd_world_no" -> "这张地图不给打分"
            "yq_words" -> "短评没通过检查" + (api.why?.let { "：$it" } ?: "")
            "yq_empty" -> "短评不能只有标点或空白"
            "scope" -> "这个页面要的权限还没有：${api.need ?: "未知"}（重新连接时把权限都同意上）"
            "not_offered" -> "网页现在不允许连这颗星"
            "quota" -> "这一轮已经连满了，等下一轮"
            "fof_day", "yq_link_day", "cross_link_day" -> "今天这一类连线的次数用完了"
            "star_gone" -> "这颗星已经不在了"
            "msg_not_mutual" -> "只有互相连上的人才能私聊"
            "msg_words" -> "消息没通过检查" + (api.why?.let { "：$it" } ?: "")
            "msg_empty" -> "消息不能是空的"
            "bad_target" -> "连不到这颗星"
            else -> "请求失败（${api.status} ${api.error}）"
        }
    }
    return e.message ?: "网络不太顺，稍后再试"
}
