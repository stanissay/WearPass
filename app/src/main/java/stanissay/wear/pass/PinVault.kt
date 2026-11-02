package stanissay.wear.pass

sealed interface PinResult {
    class Success(val password: CharArray) : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data object TooMany : PinResult
}

interface PinVault {
    suspend fun setup(pin: CharArray, password: CharArray)
    suspend fun unlock(pin: CharArray): PinResult
}

class PinVaultStub : PinVault {
    override suspend fun setup(pin: CharArray, password: CharArray) =
        throw UnsupportedOperationException("PinVault ще не реалізований")
    override suspend fun unlock(pin: CharArray): PinResult =
        throw UnsupportedOperationException("PinVault ще не реалізований")
}