package stanissay.wear.pass.settings

import android.content.Context
import stanissay.wear.pass.utils.DataConstants
import androidx.core.content.edit
import kotlin.math.pow

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences(DataConstants.SETTINGS_PREFS, Context.MODE_PRIVATE)

    var safeLength: Int
        get() = prefs.getInt(DataConstants.KEY_SAFE_LENGTH, DataConstants.SAFE_LENGTH).coerceIn(DataConstants.SAFE_MIN, DataConstants.SAFE_MAX)
        set(v) { prefs.edit { putInt(DataConstants.KEY_SAFE_LENGTH, v.coerceIn(DataConstants.SAFE_MIN, DataConstants.SAFE_MAX)) } }

    var bezelLevel: Int
        get() = prefs.getInt(DataConstants.KEY_BEZEL, DataConstants.BEZEL_DEFAULT).coerceIn(DataConstants.BEZEL_MIN, DataConstants.BEZEL_MAX)
        set(v) { prefs.edit { putInt(DataConstants.KEY_BEZEL, v.coerceIn(DataConstants.BEZEL_MIN, DataConstants.BEZEL_MAX)) } }

    fun pxPerStep(): Float = DataConstants.PX_PER_STEP * 1.25f.pow(DataConstants.BEZEL_DEFAULT - bezelLevel)
}