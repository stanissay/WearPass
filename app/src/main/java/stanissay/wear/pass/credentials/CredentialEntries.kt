package stanissay.wear.pass.credentials

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.vault.VaultSession

object CredentialEntries {
    fun build(context: Context, request: BeginGetCredentialRequest, session: VaultSession): BeginGetCredentialResponse {
        val pkg = request.callingAppInfo?.packageName.orEmpty()
        val index = session.index
        val options = request.beginGetCredentialOptions.filterIsInstance<BeginGetPasswordOption>()
        if (pkg.isEmpty() || index == null || options.isEmpty()) return BeginGetCredentialResponse()

        val matches = index.entriesForApp(pkg)
        val entries = options.flatMap { option ->
            matches.map { e ->
                PasswordCredentialEntry(
                    context = context,
                    username = e.userName.ifEmpty { e.title.ifEmpty { "—" } },
                    pendingIntent = selectIntent(context, e.id),
                    beginGetPasswordOption = option,
                    displayName = e.title,
                )
            }
        }
        return BeginGetCredentialResponse(credentialEntries = entries)
    }

    private fun selectIntent(context: Context, entryId: String): PendingIntent {
        val intent = Intent(context, CredentialActivity::class.java).putExtra(DataConstants.EXTRA_ENTRY_ID, entryId)
        return PendingIntent.getActivity(
            context, entryId.hashCode(), intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun unlockIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0, Intent(context, CredentialActivity::class.java),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}