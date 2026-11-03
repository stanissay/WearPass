package stanissay.wear.pass.sl

sealed interface SystemUnlockResult {
    class Success(val password: CharArray) : SystemUnlockResult
    data object DeviceLocked : SystemUnlockResult
    data object Invalidated : SystemUnlockResult
    data class Failed(val message: String) : SystemUnlockResult
}

data class SystemUnlockState(
    val busy: Boolean = false,
    val locked: Boolean = false,
    val message: String? = null,
)

data class SystemSetupState(val busy: Boolean = false, val error: String? = null)