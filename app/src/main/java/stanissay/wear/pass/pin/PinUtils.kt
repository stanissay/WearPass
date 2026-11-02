package stanissay.wear.pass.pin

data class PinSetupState(
    val confirming: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0
)

data class PinUnlockState(
    val busy: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0
)

sealed interface PinResult {
    class Success(val password: CharArray) : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data object TooMany : PinResult
}

interface PinVault {
    suspend fun setup(pin: CharArray, password: CharArray)
    suspend fun unlock(pin: CharArray): PinResult
    fun wipe()
}