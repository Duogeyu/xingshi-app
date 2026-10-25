package app.shixing.client

import android.app.Application
import android.content.Context
import app.shixing.client.data.Repository
import app.shixing.client.data.TokenStore
import app.shixing.client.legacy.LegacyClient
import app.shixing.client.legacy.LegacyKey
import kotlinx.serialization.json.JsonObject

class ShixingApplication : Application() {

    companion object {
        /** 全局 Application 上下文：更新检查这种不需要 Activity 的地方用它。 */
        lateinit var ctx: Context
            private set
    }


    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        ctx = applicationContext
        super.onCreate()
        repo = Repository(TokenStore(this))
        probe()
    }

    private fun probe() {
        val key = LegacyKey.get(this) ?: return
        if (key.isBlank()) return
        Thread {
            val c = LegacyClient(keyProvider = { key })
            fun log(tag: String, v: Any?) = android.util.Log.d("shixing-deco", "$tag=$v")
            runCatching {
                val me = c.me()
                log("me.deco", me["me"]?.let { (it as? JsonObject)?.get("deco") })
                log("me.star", me["me"]?.let { (it as? JsonObject)?.get("star") })
                log("me.keys", (me["me"] as? JsonObject)?.keys?.joinToString(","))
            }.onFailure { log("me.err", it.message) }
            runCatching {
                log("photosSelf", c.photos(listOf(584L to 2)).toString().take(400))
            }.onFailure { log("photosErr", it.message) }
            runCatching {
                log("recs0", c.recs().firstOrNull()?.toString()?.take(600))
            }.onFailure { log("recsErr", it.message) }
            runCatching {
                log("inbox0", c.inbox().firstOrNull()?.toString()?.take(600))
            }.onFailure { log("inboxErr", it.message) }
            runCatching {
                log("photosSelf2", c.photos(listOf(584L to 2)).toString().take(300))
            }.onFailure { log("photosErr2", it.message) }
            runCatching {
                val bag = c.bag()
                log("bag.keys", bag.keys.joinToString(","))
                log("bag.head", bag.toString().take(500))
            }.onFailure { log("bagErr", it.message) }
        }.start()
    }
}
