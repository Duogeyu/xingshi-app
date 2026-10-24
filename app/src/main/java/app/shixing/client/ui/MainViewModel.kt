package app.shixing.client.ui

import app.shixing.client.data.ApiException
import app.shixing.client.data.BoardMap
import app.shixing.client.data.CrossStar
import app.shixing.client.data.MutualStar
import app.shixing.client.data.NotificationItem
import app.shixing.client.data.Repository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
        var firstError: String? = null
        runCatching { repo.recent() }.onSuccess { r ->
            _state.update { it.copy(recent = r.items, readId = r.read) }
        }.onFailure { if (firstError == null) firstError = describe(it) }

        runCatching { repo.crossings() }.onSuccess { c ->
            _state.update { it.copy(crossings = c.stars, crossingsWhy = c.why, dot = c.n ?: 0) }
        }.onFailure { if (firstError == null) firstError = describe(it) }

        runCatching { repo.mutuals() }.onSuccess { m ->
            _state.update { it.copy(mutuals = m.stars) }
        }.onFailure { if (firstError == null) firstError = describe(it) }

        runCatching { repo.boards() }.onSuccess { b ->
            _state.update { it.copy(boards = b.maps) }
        }.onFailure { if (firstError == null) firstError = describe(it) }

        _state.update { it.copy(loading = false, error = firstError, dailyUsed = repo.dailyUsed()) }
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
            else -> "请求失败（${api.status} ${api.error}）"
        }
    }
    return e.message ?: "网络不太顺，稍后再试"
}
