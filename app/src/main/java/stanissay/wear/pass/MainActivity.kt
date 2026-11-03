package stanissay.wear.pass

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material3.TimeText
import stanissay.wear.pass.utils.MainTheme

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
                AppNav(vault)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as App).autoLock.onForeground()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) (application as App).autoLock.onBackground()
    }
}