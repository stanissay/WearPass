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

package stanissay.wear.pass.vault

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.CustomIcon
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.Group
import stanissay.wear.pass.utils.CustomField
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.EntryItem
import stanissay.wear.pass.utils.EntryView
import stanissay.wear.pass.utils.GroupItem
import stanissay.wear.pass.utils.GroupView
import stanissay.wear.pass.utils.IconRef

class VaultIndex(db: KeePassDatabase) {
    private val groups = HashMap<String, Group>()
    private val entries = HashMap<String, Entry>()
    private val rootId: String
    private val customIcons: Map<String, CustomIcon> = db.content.meta.customIcons.entries.associate { it.key.toString() to it.value }
    private val iconCache = LruCache<String, ImageBitmap>(48)

    init {
        val root = db.content.group
        rootId = root.uuid.toString()
        val meta = db.content.meta
        val hidden = setOfNotNull(
            meta.recycleBinUuid?.toString(),
            meta.entryTemplatesGroup?.toString(),
        )
        fun walk(g: Group) {
            groups[g.uuid.toString()] = g
            g.entries.forEach { entries[it.uuid.toString()] = it }
            g.groups.forEach { if (it.uuid.toString() !in hidden) walk(it) }
        }
        walk(root)
    }

    fun group(id: String?): GroupView? {
        val g = groups[id ?: rootId] ?: return null
        return GroupView(
            id = g.uuid.toString(),
            name = g.name,
            groups = g.groups
                .filter { it.uuid.toString() in groups }
                .map { GroupItem(it.uuid.toString(), it.name, it.entries.size, it.iconRef()) }
                .sortedBy { it.name.lowercase() },
            entries = g.entries
                .map {
                    EntryItem(
                        it.uuid.toString(),
                        it.text(DataConstants.TITLE),
                        it.text(DataConstants.USER),
                        it.iconRef()
                    )
                }.sortedBy { it.title.lowercase() },
            icon = g.iconRef()
        )
    }

    fun entry(id: String): EntryView? = entries[id]?.let {
        EntryView(
            id = id,
            title = it.text(DataConstants.TITLE),
            userName = it.text(DataConstants.USER),
            hasPassword = !it.fields[DataConstants.PASSWORD]?.content.isNullOrEmpty(),
            url = it.text(DataConstants.URL),
            notes = it.text(DataConstants.NOTES),
            icon = it.iconRef(),
            customFields = it.customFields(),
            attachments = it.binaries.map { b -> b.name }.filter { n -> n.isNotBlank() },
        )
    }

    private fun Entry.customFields(): List<CustomField> =
        fields.entries
            .filter { (name, _) ->
                name !in DataConstants.STANDARD_FIELDS && !name.startsWith("KPH: ")
            }
            .map { (name, value) ->
                val protected = value is EntryValue.Encrypted
                CustomField(name, if (protected) null else value.content, protected)
            }

    fun customFieldValue(id: String, name: String): String? = entries[id]?.fields?.get(name)?.content

    fun password(id: String): String? = entries[id]?.fields?.get(DataConstants.PASSWORD)?.content

    private fun Entry.text(key: String): String = fields[key]?.content.orEmpty()

    private fun Entry.iconRef() = IconRef(
        customId = customIconUuid?.toString()?.takeIf { it in customIcons },
        builtIn = icon.ordinal,
    )

    private fun Group.iconRef() = IconRef(
        customId = customIconUuid?.toString()?.takeIf { it in customIcons },
        builtIn = icon.ordinal,
    )

    fun customIcon(id: String): ImageBitmap? {
        iconCache.get(id)?.let { return it }
        val bytes = customIcons[id]?.data ?: return null
        val bmp = decode(bytes) ?: return null
        iconCache.put(id, bmp)
        return bmp
    }

    private fun decode(bytes: ByteArray, maxSide: Int = 96): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
    }

    private fun Entry.matchesApp(pkg: String) =
        text(DataConstants.URL).trim().equals("androidapp://$pkg", ignoreCase = true)

    fun entriesForApp(pkg: String): List<EntryItem> =
        entries.values
            .filter { it.matchesApp(pkg) }
            .map {
                EntryItem(
                    it.uuid.toString(),
                    it.text(DataConstants.TITLE),
                    it.text(DataConstants.USER),
                    it.iconRef()
                )
            }
            .sortedBy { it.title.lowercase() }

    fun matchesApp(id: String, pkg: String): Boolean = entries[id]?.matchesApp(pkg) == true
}