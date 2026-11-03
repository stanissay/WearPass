package stanissay.wear.pass.utils

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class AutoLock(private val onLock: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var stoppedAt = 0L
    private val task = Runnable { onLock() }

    fun onBackground(timeoutMs: Long = DataConstants.AUTO_LOCK_MS) {
        stoppedAt = SystemClock.elapsedRealtime()
        handler.removeCallbacks(task)
        handler.postDelayed(task, timeoutMs)
    }

    fun onForeground(timeoutMs: Long = DataConstants.AUTO_LOCK_MS) {
        handler.removeCallbacks(task)
        val expired = stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt >= timeoutMs
        stoppedAt = 0L
        if (expired) onLock()
    }
}