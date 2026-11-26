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

package stanissay.wear.pass.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.navigation3.runtime.NavKey
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.Entry
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Navigation
@Serializable data object FileBrowserKey : NavKey
@Serializable data object MasterPasswordKey : NavKey
@Serializable data object PinSetupKey : NavKey
@Serializable data object PinUnlockKey : NavKey
@Serializable data class GroupKey(val groupUuid: String? = null) : NavKey
@Serializable data class EntryKey(val entryUuid: String) : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data class AuthKey(val target: UnlockMethod) : NavKey
@Serializable data object SafeSetupKey : NavKey
@Serializable data object SafeUnlockKey : NavKey
@Serializable data object SystemSetupKey : NavKey
@Serializable data object SystemUnlockKey : NavKey
@Serializable data object SafeTestKey : NavKey
fun NavKey.requiresUnlock() = this is GroupKey || this is EntryKey || this is AuthKey || this is PinSetupKey || this is SafeSetupKey || this is SystemSetupKey

    // Auth
data class AuthState(
    val busy: Boolean = false,
    val locked: Boolean = false,
    val message: String? = null,
    val resetKey: Int = 0,
)

    // Browser
data class BrowserUiState(
    val dir: File,
    val items: List<File> = emptyList(),
    val mode: BrowserMode = BrowserMode.Idle
)
sealed interface BrowserMode {
    data object NoAccess : BrowserMode
    data object Idle : BrowserMode
    data object Importing : BrowserMode
    data class Failed(val message: String) : BrowserMode
}

    // Unlock
sealed interface UnlockState {
    data object Idle : UnlockState
    data object Working : UnlockState
    data class Error(val message: String) : UnlockState
}
enum class UnlockMethod { Pin, Safe, SystemLock }
interface PinVault {
    suspend fun setup(pin: CharArray, password: CharArray)
    suspend fun unlock(pin: CharArray): PinResult
    fun wipe()
    fun codeLength(): Int?
}
sealed interface PinResult {
    class Success(val password: CharArray) : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data object TooMany : PinResult
}
data class PinUnlockState(
    val busy: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0
)
data class PinSetupState(
    val confirming: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val resetKey: Int = 0
)
enum class SafeResult { Moved, Committed, Completed, Ignored }
sealed interface SystemUnlockResult {
    class Success(val password: CharArray) : SystemUnlockResult
    data object DeviceLocked : SystemUnlockResult
    data object Invalidated : SystemUnlockResult
    data class Failed(val message: String) : SystemUnlockResult
}
data class SystemUnlockState(
    val busy: Boolean = false,
    val locked: Boolean = false,
    val message: String? = null,
)
data class SystemSetupState(val busy: Boolean = false, val error: String? = null)

    // Vault
data class Revealed(val id: String, val value: String)
enum class VaultStatus { NotImported, NeedsMasterPassword, PinLocked, SafeLocked, SystemLocked }
data class IconRef(val customId: String? = null, val builtIn: Int? = null)
data class GroupItem(
    val id: String, val name: String,
    val entryCount: Int, val icon: IconRef
)
data class EntryItem(
    val id: String, val title: String,
    val userName: String, val icon: IconRef, val colors: EntryColors? = null
)
data class EntryView(
    val id: String, val title: String, val userName: String,
    val hasPassword: Boolean, val url: String, val notes: String,
    val icon: IconRef, val customFields: List<CustomField>, val attachments: List<String>, val colors: EntryColors? = null,
)
data class GroupView(
    val id: String, val name: String,
    val groups: List<GroupItem>, val entries: List<EntryItem>,
    val icon: IconRef
)
data class CustomField(val name: String, val value: String?, val isProtected: Boolean)
data class EntryColors(val container: Int?, val content: Int?)
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


    // Classes and functions

class SwipeGate {
    private var blockers by mutableIntStateOf(0)
    val enabled get() = blockers == 0
    fun block() { blockers++ }
    fun unblock() { blockers = (blockers - 1).coerceAtLeast(0) }
}

class RotaryBus { @Volatile var listener: ((Float) -> Boolean)? = null }

class DialHaptics(private val view: View) {
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)
    fun commit() = perform(HapticFeedbackConstants.CONFIRM)
    fun clear() = perform(HapticFeedbackConstants.CONTEXT_CLICK)
    private fun perform(constant: Int) { view.performHapticFeedback(constant) }
}

@Immutable
class ResolvedColors(val container: Color, val content: Color)

fun CharArray.toUtf8(): ByteArray {
    val bb = Charsets.UTF_8.encode(CharBuffer.wrap(this))
    val out = ByteArray(bb.remaining()).also { bb.get(it) }
    bb.array().fill(0)
    return out
}

fun ByteArray.toChars(): CharArray {
    val cb = Charsets.UTF_8.decode(ByteBuffer.wrap(this))
    return CharArray(cb.remaining()).also { cb.get(it) }
}

fun Entry.colors(): EntryColors? {
    val bg = parseHex(backgroundColor)
    val fg = parseHex(foregroundColor)
    return if (bg == null && fg == null) null else EntryColors(container = bg, content = fg)
}

@SuppressLint("UseKtx")
fun parseHex(s: String?): Int? {
    val t = s?.trim().orEmpty()
    if (t.isEmpty()) return null
    return runCatching {
        android.graphics.Color.parseColor(if (t.startsWith("#")) t else "#$t")
    }.getOrNull()
}

fun contrast(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}

fun readableOn(bg: Color): Color = if (contrast(Color.Black, bg) >= contrast(Color.White, bg)) Color.Black else Color.White

fun resolveEntryColors(c: EntryColors?, defaultContainer: Color, defaultContent: Color, ): ResolvedColors {
    val container = c?.container?.let { Color(it) } ?: defaultContainer
    val wanted = c?.content?.let { Color(it) } ?: if (c?.container != null) readableOn(container) else defaultContent
    val content = if (contrast(wanted, container) >= 3f) wanted else readableOn(container)
    return ResolvedColors(container, content)
}

fun writeAtomic(target: File, bytes: ByteArray) {
    val tmp = File(target.parentFile, target.name + ".tmp")
    tmp.writeBytes(bytes)
    check(tmp.renameTo(target)) { "Не вдалося записати ${target.name}" }
}

fun keyStore() = KeyStore.getInstance(DataConstants.ANDROID_KEY_STORE).apply { load(null) }!!

fun sha256(f: File): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    f.inputStream().use { i ->
        val buf = ByteArray(8192)
        while (true) {
            val n = i.read(buf)
            if (n < 0) break
            md.update(buf, 0, n)
        }
    }
    return md.digest()
}

@SuppressLint("WearRecents")
fun openCredentialSettings(context: Context) {
    runCatching { androidx.credentials.CredentialManager.create(context).createSettingsPendingIntent().send() }
        .onFailure {
            runCatching {
                context.startActivity(
                    Intent("android.settings.CREDENTIAL_PROVIDER").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
}