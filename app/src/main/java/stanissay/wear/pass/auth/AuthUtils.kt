package stanissay.wear.pass.auth

data class ReauthState(
    val busy: Boolean = false,
    val locked: Boolean = false,
    val message: String? = null,
    val resetKey: Int = 0,
)