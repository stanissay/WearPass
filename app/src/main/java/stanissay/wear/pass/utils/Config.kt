package stanissay.wear.pass.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

object DataConstants {
    const val AUTO_LOCK_MS = 30_000L
    const val KDBX = "kdbx"
    const val KDBX_MAX_SIZE = 64L * 1024 * 1024
    val KDBX_SIGNATURE = byteArrayOf(
        0x03, 0xD9.toByte(), 0xA2.toByte(), 0x9A.toByte(),
        0x67, 0xFB.toByte(), 0x4B, 0xB5.toByte()
    )
    const val BROWSER_ROOT = "/"
    const val BROWSER_UP = ".."
    const val PIN_LENGTH = 4
    const val SAFE_LENGTH = 4
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
    const val PIN_ALIAS = "wearkeepass_pin_wrap"
    const val SAFE_ALIAS = "wearkeepass_safe_wrap"
    const val SYS_ALIAS = "wearkeepass_system_lock"
    const val VERSION: Byte = 1
    const val SALT_LEN = 16
    const val IV_LEN = 12
    const val PBKDF2_ITERATIONS = 200_000
    const val MAX_ATTEMPTS = 5
    const val TITLE = "Title"
    const val USER = "UserName"
    const val PASSWORD = "Password"
    const val URL = "URL"
    const val NOTES = "Notes"
    val STANDARD_FIELDS = setOf(TITLE, USER, PASSWORD, URL, NOTES)
    const val EXTRA_ENTRY_ID = "entry_id"
    const val PASSWORD_VISIBLE_MS = 15_000L
    val SWIPE_THRESHOLD = 72.dp
    const val TWO_PI = (2.0 * Math.PI).toFloat()
    const val STEP_RAD = TWO_PI / 10f
    const val PX_PER_STEP = 136f
    const val RING_FRACTION = 0.75f
    const val CHIP_FRACTION = 0.15f
    const val CENTER_FRACTION = 0.5f
    const val SAFE_DWELL_MS = 2000
}

val LocalAmbient = compositionLocalOf { false }