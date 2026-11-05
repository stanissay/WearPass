/*
 * KeePass manager for Wear OS
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

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