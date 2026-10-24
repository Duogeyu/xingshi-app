package app.shixing.client

import android.app.Application
import app.shixing.client.data.Repository
import app.shixing.client.data.TokenStore

class ShixingApplication : Application() {

    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = Repository(TokenStore(this))
    }
}
