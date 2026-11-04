package stanissay.wear.pass

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material3.TimeText
import stanissay.wear.pass.utils.LocalAmbient
import stanissay.wear.pass.utils.MainColors
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.MainTheme

class MainActivity : ComponentActivity() {
    private val app get() = application as App
    private var ambient by mutableStateOf(false)
    private var burnIn by mutableStateOf(false)
    private var ambientTick by mutableIntStateOf(0)
    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            burnIn = ambientDetails.burnInProtectionRequired
            ambient = true
            app.autoLock.onBackground()
        }
        override fun onExitAmbient() {
            ambient = false
            app.autoLock.onForeground()
        }
        override fun onUpdateAmbient() { ambientTick++ }
    }
    private val ambientObserver = AmbientLifecycleObserver(this, ambientCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        lifecycle.addObserver(ambientObserver)
        val vault = app.vault
        setContent {
            MainTheme {
                CompositionLocalProvider(LocalAmbient provides ambient) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().alpha(if (ambient) 0f else 1f)) { AppNav(vault) }
                        if (ambient) AmbientCover(burnIn, ambientTick, onEscape = { ambient = false })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val isAmbient = ambientObserver.isAmbient
        ambient = isAmbient
        if (isAmbient) app.autoLock.lockIfExpired() else app.autoLock.onForeground()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) app.autoLock.onBackground()
    }

    override fun onDestroy() {
        lifecycle.removeObserver(ambientObserver)
        super.onDestroy()
    }
}

@Composable
fun AmbientCover(burnIn: Boolean, tick: Int, onEscape: () -> Unit) {
    val dx = if (burnIn) listOf(-3, 0, 3, 0)[tick % 4] else 0
    val dy = if (burnIn) listOf(0, 3, 0, -3)[tick % 4] else 0
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MainColors.BACKGROUND)
            .pointerInput(Unit) { detectTapGestures { onEscape() } },
        contentAlignment = Alignment.Center
    ) {
        TimeText()
        MainText(
            text = stringResource(R.string.app_name),
            modifier = Modifier.offset(dx.dp, dy.dp)
        )
    }
}