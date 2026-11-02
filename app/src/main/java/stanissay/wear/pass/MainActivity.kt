package stanissay.wear.pass

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import stanissay.wear.pass.utils.AppEventManager
import stanissay.wear.pass.utils.MainTheme
import stanissay.wear.pass.utils.UiEvent
import stanissay.wear.pass.utils.showCustomToast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        val vault = (application as App).vault
        setContent {
            MainTheme {
                LaunchedEffect(Unit) {
                    AppEventManager.uiEvents.collect { event ->
                        when (event) {
                            is UiEvent.ShowToast -> {
                                showCustomToast(event.message, this@MainActivity)
                                AppEventManager.resetReplayCache()
                            }
                        }
                    }
                }

                AppNav(vault)
            }
        }
    }
}