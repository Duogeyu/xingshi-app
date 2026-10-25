package app.shixing.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.shixing.client.notify.ReminderNotifier
import app.shixing.client.ui.Home
import app.shixing.client.ui.MainViewModel
import app.shixing.client.ui.theme.ShixingTheme
import app.shixing.client.work.ReminderScheduler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repo by lazy { (application as ShixingApplication).repo }

    private val vm: MainViewModel by viewModels { MainViewModel.factory(repo) }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* denied is fine */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ReminderNotifier.ensureChannel(this)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 连接状态一变就调整后台提醒的排班：连上就登记，断开就撤掉。
        lifecycleScope.launch {
            vm.state
                .map { it.session }
                .distinctUntilChanged()
                .collect { connected ->
                    if (connected) ReminderScheduler.ensure(this@MainActivity)
                    else ReminderScheduler.cancel(this@MainActivity)
                }
        }

        setContent {
            ShixingTheme {
                Home(vm)
            }
        }

        // 贴纸的离屏渲染机：一只 1px 的 WebView 挂在窗口上才画得出东西
        (findViewById<android.view.ViewGroup>(android.R.id.content))?.let {
            app.shixing.client.ui.StickerRaster.host(this, it)
        }
    }
}
