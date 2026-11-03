package stanissay.wear.pass.vault

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.keemobile.kotpass.database.KeePassDatabase
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class VaultStatus { NotImported, NeedsMasterPassword, PinLocked, SafeLocked, SystemLocked }

data class IconRef(val customId: String? = null, val builtIn: Int? = null)

data class GroupItem(
    val id: String, val name: String,
    val entryCount: Int, val icon: IconRef
)
data class EntryItem(
    val id: String, val title: String,
    val userName: String, val icon: IconRef
)
data class EntryView(
    val id: String, val title: String, val userName: String,
    val hasPassword: Boolean, val url: String, val notes: String,
    val icon: IconRef, val customFields: List<CustomField>, val attachments: List<String>
)
data class GroupView(
    val id: String, val name: String,
    val groups: List<GroupItem>, val entries: List<EntryItem>,
    val icon: IconRef
)
data class CustomField(val name: String, val value: String?, val isProtected: Boolean)

enum class SyncNote { None, Updated, SourceMissing, SourceUnreadable }

class OpenResult(val db: KeePassDatabase, val sync: SyncNote)

class Synced(val note: SyncNote, val db: KeePassDatabase?)

class BuiltInIcons(private val context: Context) {
    private val cache = ConcurrentHashMap<Int, ImageBitmap>()
    private val missing = ConcurrentHashMap.newKeySet<Int>()

    fun get(index: Int): ImageBitmap? {
        if (index !in 0..68 || index in missing) return null
        cache[index]?.let { return it }
        val bmp = runCatching {
            context.assets.open(String.format(Locale.ROOT, "kp_icons/%02d.png", index))
                .use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
        if (bmp == null) { missing.add(index); return null }
        cache[index] = bmp
        return bmp
    }
}