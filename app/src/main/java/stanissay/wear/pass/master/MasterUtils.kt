package stanissay.wear.pass.master

sealed interface UnlockState {
    data object Idle : UnlockState
    data object Working : UnlockState
    data class Error(val message: String) : UnlockState
}