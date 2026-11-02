package stanissay.wear.pass.sl

sealed interface SystemUnlockResult {
    class Success(val password: CharArray) : SystemUnlockResult
    data object DeviceLocked : SystemUnlockResult
    data object Invalidated : SystemUnlockResult
    data class Failed(val message: String) : SystemUnlockResult
}