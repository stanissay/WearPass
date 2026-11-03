package stanissay.wear.pass.vault

import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.Group
import stanissay.wear.pass.utils.DataConstants

class VaultIndex(db: KeePassDatabase) {
    private val groups = HashMap<String, Group>()
    private val entries = HashMap<String, Entry>()
    private val rootId: String

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
                        it.text(DataConstants.USER))
                }.sortedBy { it.title.lowercase() },
        )
    }

    fun entry(id: String): EntryView? = entries[id]?.let {
        EntryView(
            id,
            it.text(DataConstants.TITLE),
            it.text(DataConstants.USER),
            it.text(DataConstants.URL),
            it.text(DataConstants.NOTES))
    }

    fun password(id: String): String? = entries[id]?.fields?.get(DataConstants.PASSWORD)?.content

    private fun Entry.text(key: String): String = fields[key]?.content.orEmpty()
}