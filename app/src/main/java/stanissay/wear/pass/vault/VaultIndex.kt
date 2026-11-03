package stanissay.wear.pass.vault

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.CustomIcon
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.Group
import stanissay.wear.pass.utils.DataConstants

class VaultIndex(db: KeePassDatabase) {
    private val groups = HashMap<String, Group>()
    private val entries = HashMap<String, Entry>()
    private val rootId: String
    private val customIcons: Map<String, CustomIcon> = db.content.meta.customIcons.entries.associate { it.key.toString() to it.value }
    private val iconCache = LruCache<String, ImageBitmap>(48)

    init {
        val root = db.content.group
        rootId = root.uuid.toString()
        val binId = db.content.meta.recycleBinUuid?.toString()
        fun walk(g: Group) {
            groups[g.uuid.toString()] = g
            g.entries.forEach { entries[it.uuid.toString()] = it }
            g.groups.forEach { if (it.uuid.toString() != binId) walk(it) }
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
                .map { GroupItem(it.uuid.toString(), it.name, it.entries.size) }
                .sortedBy { it.name.lowercase() },
            entries = g.entries
                .map {
                    EntryItem(
                        it.uuid.toString(),
                        it.text(DataConstants.TITLE),
                        it.text(DataConstants.USER),
                        it.iconId())
                }.sortedBy { it.title.lowercase() },
        )
    }

    fun entry(id: String): EntryView? = entries[id]?.let {
        EntryView(
            id,
            it.text(DataConstants.TITLE),
            it.text(DataConstants.USER),
            it.text(DataConstants.URL),
            it.text(DataConstants.NOTES),
            it.iconId()
        )
    }

    fun password(id: String): String? = entries[id]?.fields?.get(DataConstants.PASSWORD)?.content

    private fun Entry.text(key: String): String = fields[key]?.content.orEmpty()

    private fun Entry.iconId(): String? = customIconUuid?.toString()?.takeIf { it in customIcons }

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
}