package stanissay.wear.pass.utils

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

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