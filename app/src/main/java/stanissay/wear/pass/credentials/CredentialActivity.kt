package stanissay.wear.pass.credentials

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import stanissay.wear.pass.App
import stanissay.wear.pass.MainActivity
import stanissay.wear.pass.R
import stanissay.wear.pass.pin.PinUnlockScreen
import stanissay.wear.pass.settings.UnlockMethod
import stanissay.wear.pass.sl.SystemUnlockScreen
import stanissay.wear.pass.utils.ClickableListItem
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.MainTheme
import stanissay.wear.pass.vault.VaultStatus

class CredentialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val app = application as App

        intent.getStringExtra(DataConstants.EXTRA_ENTRY_ID)?.let { id ->
            deliverPassword(app, id)
            return
        }

        val begin = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
        if (begin == null) { cancel(); return }
        if (app.session.isUnlocked) { deliverEntries(app, begin); return }

        setContent {
            MainTheme {
                val status by app.vault.status.collectAsState()
                val done = { deliverEntries(app, begin) }
                when (status) {
                    VaultStatus.PinLocked -> PinUnlockScreen(
                        method = UnlockMethod.Pin, onUnlocked = done,
                        onForcedMaster = ::cancel
                    )
                    VaultStatus.SafeLocked -> PinUnlockScreen(
                        method = UnlockMethod.Safe, onUnlocked = done,
                        onForcedMaster = ::cancel
                    )
                    VaultStatus.SystemLocked -> SystemUnlockScreen(
                        onUnlocked = done, onForcedMaster = ::cancel,
                    )
                    else -> NeedAppUnlock(onCancel = ::cancel)
                }
            }
        }
    }

    private fun deliverEntries(app: App, begin: BeginGetCredentialRequest) {
        val result = Intent()
        PendingIntentHandler.setBeginGetCredentialResponse(
            result, CredentialEntries.build(this, begin, app.session)
        )
        setResult(RESULT_OK, result)
        finish()
    }

    private fun deliverPassword(app: App, entryId: String) {
        val getRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        val index = app.session.index
        val pkg = getRequest?.callingAppInfo?.packageName
        val entry = index?.entry(entryId)
        val password = index?.password(entryId)

        if (getRequest == null || pkg == null || entry == null || password == null ||
            !index.matchesApp(entryId, pkg)
        ) { cancel(); return }

        val result = Intent()
        PendingIntentHandler.setGetCredentialResponse(
            result,
            GetCredentialResponse(PasswordCredential(entry.userName, password)),
            getRequest
        )
        setResult(RESULT_OK, result)
        finish()
    }

    private fun cancel() { setResult(RESULT_CANCELED); finish() }
}

@SuppressLint("WearRecents")
@Composable
private fun NeedAppUnlock(onCancel: () -> Unit) {
    val context = LocalContext.current
    ScalingLazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item { MainText(stringResource(R.string.credential_open)) }
        item {
            ClickableListItem(
                onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    onCancel()
                }
            ) { MainText(stringResource(R.string.open)) }
        }
    }
}