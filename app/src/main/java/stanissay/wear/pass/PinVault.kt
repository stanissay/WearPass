package stanissay.wear.pass

sealed interface PinResult {
    data class Success(val password: CharArray) : PinResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (!password.contentEquals(other.password)) return false

            return true
        }

        override fun hashCode(): Int {
            return password.contentHashCode()
        }
    }

    data class Wrong(val attemptsLeft: Int) : PinResult
    data object TooMany : PinResult // блок уже стерто
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