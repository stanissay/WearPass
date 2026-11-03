package stanissay.wear.pass.vault

import app.keemobile.kotpass.database.KeePassDatabase

enum class VaultStatus { NotImported, NeedsMasterPassword, PinLocked }

data class GroupItem(
    val id: String,
    val name: String,
    val entryCount: Int,
    val customIconId: String? = null
)
data class EntryItem(
    val id: String,
    val title: String,
    val userName: String,
    val customIconId: String? = null
)
data class GroupView(
    val id: String,
    val name: String,
    val groups: List<GroupItem>,
    val entries: List<EntryItem>,
    val customIconId: String? = null
)
data class EntryView(
    val id: String,
    val title: String,
    val userName: String,
    val url: String,
    val notes: String,
    val customIconId: String? = null
)

enum class SyncNote { None, Updated, SourceMissing, SourceUnreadable }

class OpenResult(val db: KeePassDatabase, val sync: SyncNote)

class Synced(val note: SyncNote, val db: KeePassDatabase?)