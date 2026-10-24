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
import app.shixing.client.ui.MainViewModel
import app.shixing.client.ui.ShixingApp
import app.shixing.client.ui.theme.ShixingTheme
import app.shixing.client.work.ReminderScheduler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repo by lazy { (application as ShixingApplication).repo }

    private val vm: MainViewModel by viewModels { MainViewModel.factory(repo) }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 拒绝也照常运行 */ }

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
                ShixingApp(vm)
            }
        }
    }
}
