package stanissay.wear.pass

import android.app.Application

class App : Application() {
    val vault by lazy { VaultRepository(this) }
}