package stanissay.wear.pass.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import stanissay.wear.pass.FileBrowserViewModel

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

object AppEventManager {
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _uiEvents = MutableSharedFlow<UiEvent>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val uiEvents = _uiEvents.asSharedFlow()

    fun sendEvent(event: UiEvent) {
        managerScope.launch {
            _uiEvents.emit(event)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun resetReplayCache() {
        _uiEvents.resetReplayCache()
    }
}

fun showCustomToast(text: String, context: Context) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }

@SuppressLint("ModifierFactoryExtensionFunction")
fun TransformingLazyColumnItemScope.transformedItem(
    transformationSpec: TransformationSpec
): Modifier =
    Modifier
        .transformedHeight(this, transformationSpec)
        .graphicsLayer {
            with(transformationSpec) {
                applyContainerTransformation(scrollProgress)
            }
        }