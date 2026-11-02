package stanissay.wear.pass.utils

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object FileBrowserKey : NavKey
@Serializable data object MasterPasswordKey : NavKey
@Serializable data object PinSetupKey : NavKey
@Serializable data object PinUnlockKey : NavKey
@Serializable data class GroupKey(val groupUuid: String? = null) : NavKey
@Serializable data class EntryKey(val entryUuid: String) : NavKey
@Serializable data object SettingsKey : NavKey