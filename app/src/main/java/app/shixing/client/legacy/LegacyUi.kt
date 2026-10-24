package app.shixing.client.legacy

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.shixing.client.BuildConfig
import app.shixing.client.ui.MaterialTheme_color
import app.shixing.client.ui.ScreenTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 「网页功能」：装扮 / 仓库 / 兑换码 / 账号。
 *
 * 这些能力官方 v1 接口还没有，走的是网页自己用的 `/api/match`。
 * 因此必须先在这台手机上「通过网页登录」拿到设备密钥；密钥只留在本机。
 */
class LegacyViewModel(private val appContext: android.content.Context) : ViewModel() {

    data class State(
        val hasKey: Boolean = false,
        val busy: Boolean = false,
        val error: String? = null,
        val hint: String? = null,
        val me: JsonObject? = null,
        val bag: JsonObject? = null,
        val keys: JsonObject? = null,
        val redeems: List<String> = emptyList(),
        val showLogin: Boolean = false,
        val photoUrl: String? = null,
        val photoFor: Long? = null,
    )

    private val client = LegacyClient(keyProvider = { LegacyKey.get(appContext) })

    private val _state = MutableStateFlow(State(hasKey = LegacyKey.get(appContext) != null))
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        if (_state.value.hasKey) refresh()
    }

    fun openLogin() = _state.update { it.copy(showLogin = true) }
    /** 取一张星卡照片（网页同款动作，需要本机有网页设备密钥）。 */
    fun loadPhoto(star: Long) {
        if (_state.value.photoFor == star && _state.value.photoUrl != null) return
        if (LegacyKey.get(appContext) == null) return
        viewModelScope.launch {
            runCatching { client.photos(listOf(star to 2)) }.onSuccess { array ->
                val img = (array.firstOrNull() as? JsonObject)?.str("img")
                if (!img.isNullOrBlank()) {
                    _state.update { it.copy(photoUrl = img, photoFor = star) }
                }
            }
        }
    }

    fun closeLogin() = _state.update { it.copy(showLogin = false) }

    fun saveKey(key: String) {
        LegacyKey.set(appContext, key)
        _state.update { it.copy(hasKey = true, showLogin = false, hint = "已连上网页账号") }
        refresh()
    }

    fun forget() {
        LegacyKey.set(appContext, null)
        _state.update { State() }
    }

    fun dismissHint() = _state.update { it.copy(hint = null) }

    fun refresh() {
        if (LegacyKey.get(appContext) == null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            val me = runCatching { client.me() }.getOrNull()
            val bag = runCatching { client.bag() }.getOrNull()
            val keys = runCatching { client.keys() }.getOrNull()
            _state.update { it.copy(busy = false, me = me, bag = bag, keys = keys, hasKey = true) }
        }
    }

    fun redeem(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, redeems = emptyList()) }
            runCatching { client.redeem(code) }
                .onSuccess { result ->
                    val gives = (result["gives"] as? JsonArray)?.mapNotNull { g ->
                        (g as? JsonPrimitive)?.content
                    }.orEmpty()
                    _state.update {
                        it.copy(
                            busy = false,
                            redeems = gives.ifEmpty { listOf("兑换成功") },
                            hint = "已放进「我的仓库」",
                        )
                    }
                    refresh()
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, error = (e as? LegacyError)?.text ?: "兑换失败") }
                }
        }
    }

    fun dropKey(id: String) {
        viewModelScope.launch {
            runCatching { client.dropKey(id) }
                .onSuccess { refresh() }
                .onFailure { e ->
                    _state.update { it.copy(error = (e as? LegacyError)?.text ?: "移除失败") }
                }
        }
    }

    fun leave() {
        viewModelScope.launch {
            runCatching { client.leave() }
                .onSuccess { forget() }
                .onFailure { e ->
                    _state.update { it.copy(error = (e as? LegacyError)?.text ?: "退出失败") }
                }
        }
    }

    companion object {
        fun factory(context: android.content.Context) = viewModelFactory {
            initializer { LegacyViewModel(context.applicationContext) }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LegacyLoginScreen(onCaptured: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val captured by rememberUpdatedState(onCaptured)

    val web = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString =
                settings.userAgentString + " XingyuAndroid/" + BuildConfig.VERSION_NAME
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadUrl("https://vrcc.cc/match/?tab=me")
        }
    }

    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        var stopped = false
        val poll = object : Runnable {
            override fun run() {
                if (stopped) return
                web.evaluateJavascript(
                    "(function(){try{return localStorage.getItem('vrcc-match-k')||''}catch(e){return ''}})()",
                ) { raw ->
                    val value = raw?.trim()?.trim('"')
                    if (LegacyKey.valid(value)) {
                        stopped = true
                        captured(value!!)
                    }
                }
                handler.postDelayed(this, 1500)
            }
        }
        handler.postDelayed(poll, 1500)
        onDispose {
            stopped = true
            handler.removeCallbacks(poll)
            web.destroy()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme_color.bg),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "登录网页版",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme_color.ink,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("取消", color = MaterialTheme_color.ink2, fontSize = 13.sp)
            }
        }
        Text(
            "在下面登录一次（邮箱验证码或扫码都行），成功后会自动回来",
            fontSize = 12.sp,
            color = MaterialTheme_color.ink3,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        AndroidView(
            factory = { web },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
fun RedeemScreen(vm: LegacyViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    var code by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme_color.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Header("兑换码", "网页版「兑换码」里那种一串字母数字", onBack)

        if (!s.hasKey) {
            ConnectFirst { vm.openLogin() }
            return@Column
        }

        Card8 {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(24) },
                placeholder = { Text("输入兑换码", fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme_color.ink,
                    unfocusedTextColor = MaterialTheme_color.ink,
                    focusedBorderColor = MaterialTheme_color.accent,
                    unfocusedBorderColor = MaterialTheme_color.line,
                    focusedContainerColor = MaterialTheme_color.card2,
                    unfocusedContainerColor = MaterialTheme_color.card2,
                    focusedPlaceholderColor = MaterialTheme_color.ink3,
                    unfocusedPlaceholderColor = MaterialTheme_color.ink3,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.redeem(code) },
                enabled = code.isNotBlank() && !s.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (s.busy) "兑换中…" else "兑换", fontSize = 15.sp) }

            if (s.redeems.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "兑换成功",
                    fontSize = 13.sp,
                    color = MaterialTheme_color.accent,
                    fontWeight = FontWeight.SemiBold,
                )
                s.redeems.forEach { item ->
                    Text(
                        "· $item",
                        fontSize = 13.sp,
                        color = MaterialTheme_color.ink2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            s.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, fontSize = 13.sp, color = MaterialTheme_color.warn)
            }
        }

        Note(
            "兑换的东西直接进「我的仓库」，和网页是同一份。这一组官方 v1 接口还没开放，" +
                "走的是网页自己的接口，所以要先登录一次网页版。",
        )
    }
}

@Composable
fun AccountScreen(vm: LegacyViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme_color.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Header("账号与设备", "只在你自己登录过的这台手机上访问", onBack)

        if (!s.hasKey) {
            ConnectFirst { vm.openLogin() }
            return@Column
        }

        val meBody = s.me?.childObj("me") ?: JsonObject(emptyMap())
        val star = meBody.int("s")
        val name = meBody.str("name") ?: "我的星"

        Card8 {
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme_color.ink)
            Text(
                if (star != null) "星号 $star" else "已连接网页账号",
                fontSize = 12.sp,
                color = MaterialTheme_color.ink3,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill("刷新", MaterialTheme_color.ink2) { vm.refresh() }
                ActionPill("换密钥", MaterialTheme_color.ink2) { vm.openLogin() }
                ActionPill("退出", MaterialTheme_color.warn) { vm.leave() }
            }
        }

        Spacer(Modifier.height(12.dp))
        val list = (s.keys?.get("keys") as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

        Card8 {
            Text(
                "登着的设备",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme_color.ink,
            )
            if (list.isEmpty()) {
                Text(
                    "没有别的设备。",
                    fontSize = 13.sp,
                    color = MaterialTheme_color.ink3,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                list.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.str("name") ?: "未知设备",
                                fontSize = 14.sp,
                                color = MaterialTheme_color.ink,
                            )
                            Text(
                                listOfNotNull(
                                    item.str("at")?.let { clockText(it) },
                                    item.str("where"),
                                ).joinToString(" · "),
                                fontSize = 11.sp,
                                color = MaterialTheme_color.ink3,
                            )
                        }
                        TextButton(onClick = { item.str("id")?.let(vm::dropKey) }) {
                            Text("移除", fontSize = 12.sp, color = MaterialTheme_color.warn)
                        }
                    }
                }
            }
        }

        s.error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 13.sp, color = MaterialTheme_color.warn)
        }

        Note("改资料、注销这类操作官方不提供接口，要回网页端做。这里只做「看设备」和「踢掉别的设备」。")
    }
}

@Composable
fun BagScreen(vm: LegacyViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme_color.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Header("我的仓库", "贴纸包、卡面、星尘，和网页同一份", onBack)

        if (!s.hasKey) {
            ConnectFirst { vm.openLogin() }
            return@Column
        }

        val bag = s.bag ?: JsonObject(emptyMap())
        val tickets = bag.int("tickets")
        val dust = bag.int("dust")

        Card8 {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("贴纸包", tickets?.toString() ?: "—")
                Stat("星尘", dust?.toString() ?: "—")
                Stat("任务", bag.int("tasks")?.toString() ?: "—")
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "其余背包内容（卡面、卡背、卡套、贴纸）还能拿到，但网页上是拖拽编辑卡片的交互，" +
                "原生做需要另外一套编辑器。下一步再做。",
            fontSize = 12.sp,
            color = MaterialTheme_color.ink3,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme_color.ink)
        Text(label, fontSize = 11.sp, color = MaterialTheme_color.ink3, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Header(title: String, sub: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionPill("返回", MaterialTheme_color.ink2, onBack)
        Spacer(Modifier.weight(1f))
    }
    ScreenTitle(title, sub)
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = MaterialTheme_color.ink3,
        modifier = Modifier.padding(top = 14.dp, bottom = 24.dp),
    )
}

@Composable
private fun ActionPill(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        text,
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        fontSize = 13.sp,
        color = color,
    )
}

@Composable
private fun ConnectFirst(onClick: () -> Unit) {
    Card8 {
        Text(
            "还没连接网页账号",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme_color.ink,
        )
        Text(
            "这一组功能（仓库、装扮、兑换码、设备管理）走的是网页自己的接口，需要你先在这台手机上登录一次网页版，" +
                "App 会把网页的设备密钥留在本机，之后就不用再登。",
            fontSize = 12.sp,
            color = MaterialTheme_color.ink3,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text("登录网页版", fontSize = 15.sp)
        }
    }
}

@Composable
private fun Card8(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme_color.card, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) { Column(content = content) }
}

private fun clockText(at: String): String = runCatching {
    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
        .format(java.util.Date(at.toLong()))
}.getOrDefault(at)
