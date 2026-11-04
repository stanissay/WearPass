package stanissay.wear.pass.safe

import android.view.HapticFeedbackConstants
import android.view.View

enum class SafeResult { Moved, Committed, Completed, Ignored }

class DialHaptics(private val view: View) {
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)
    fun commit() = perform(HapticFeedbackConstants.CONFIRM)
    fun clear() = perform(HapticFeedbackConstants.CONTEXT_CLICK)
    private fun perform(constant: Int) { view.performHapticFeedback(constant) }
}