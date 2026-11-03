package stanissay.wear.pass.vault

import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.Group
import stanissay.wear.pass.utils.DataConstants

enum class VaultStatus { NotImported, NeedsMasterPassword, PinLocked }

data class GroupItem(val id: String, val name: String, val entryCount: Int)
data class EntryItem(val id: String, val title: String, val userName: String)
data class GroupView(
    val id: String,
    val name: String,
    val groups: List<GroupItem>,
    val entries: List<EntryItem>,
)
data class EntryView(
    val id: String,
    val title: String,
    val userName: String,
    val url: String,
    val notes: String,
)

enum class SyncNote { None, Updated, SourceMissing, SourceUnreadable }

class OpenResult(val db: KeePassDatabase, val sync: SyncNote)

class Synced(val note: SyncNote, val db: KeePassDatabase?)