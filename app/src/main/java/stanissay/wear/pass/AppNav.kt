package stanissay.wear.pass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import stanissay.wear.pass.auth.AuthScreen
import stanissay.wear.pass.browser.FileBrowserScreen
import stanissay.wear.pass.master.MasterPasswordScreen
import stanissay.wear.pass.pin.PinSetupScreen
import stanissay.wear.pass.pin.PinUnlockScreen
import stanissay.wear.pass.settings.SettingsScreen
import stanissay.wear.pass.settings.UnlockMethod
import stanissay.wear.pass.sl.SystemSetupScreen
import stanissay.wear.pass.sl.SystemUnlockScreen
import stanissay.wear.pass.utils.AuthKey
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.EntryKey
import stanissay.wear.pass.utils.FileBrowserKey
import stanissay.wear.pass.utils.GroupKey
import stanissay.wear.pass.utils.MasterPasswordKey
import stanissay.wear.pass.utils.PinSetupKey
import stanissay.wear.pass.utils.PinUnlockKey
import stanissay.wear.pass.utils.SafeSetupKey
import stanissay.wear.pass.utils.SafeUnlockKey
import stanissay.wear.pass.utils.SettingsKey
import stanissay.wear.pass.utils.SystemSetupKey
import stanissay.wear.pass.utils.SystemUnlockKey
import stanissay.wear.pass.utils.requiresUnlock
import stanissay.wear.pass.vault.EntryScreen
import stanissay.wear.pass.vault.GroupScreen
import stanissay.wear.pass.vault.VaultRepository
import stanissay.wear.pass.vault.VaultStatus
import kotlin.math.abs

@Composable
fun AppNav(vault: VaultRepository) {
    val session = (LocalContext.current.applicationContext as App).session
    val backStack = rememberNavBackStack(remember { startKey(vault) })
    val relock: () -> Unit = { backStack.clear(); backStack.add(startKey(vault)) }
    val openSettings: () -> Unit = {
        if (backStack.lastOrNull() != SettingsKey) backStack.add(SettingsKey)
    }
    val unlocked by session.unlocked.collectAsState()
    val needsRelock = !unlocked && backStack.any { it.requiresUnlock() }
    LaunchedEffect(needsRelock) { if (needsRelock) relock() }
    if (needsRelock) {
        Box(Modifier.fillMaxSize())
        return
    }

    SwipeLeftToOpen(onSwipe = openSettings) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
            entryProvider = entryProvider {
                entry<FileBrowserKey> {
                    FileBrowserScreen(onImported = {
                        backStack.clear(); backStack.add(MasterPasswordKey)
                    })
                }
                entry<MasterPasswordKey> {
                    MasterPasswordScreen(onOpened = {
                        backStack.clear(); backStack.add(setupKeyFor(vault.unlockMethod))
                    })
                }
                entry<PinSetupKey> {
                    DisposableEffect(Unit) { onDispose { session.clearPendingPassword() } }
                    Guarded(session.isUnlocked, relock) {
                        PinSetupScreen(
                            method = UnlockMethod.Pin,
                            onDone = { backStack.clear(); backStack.add(GroupKey()) }
                        )
                    }
                }
                entry<PinUnlockKey> {
                    PinUnlockScreen(
                        method = UnlockMethod.Pin,
                        onUnlocked = { backStack.clear(); backStack.add(GroupKey()) },
                        onForcedMaster = { backStack.clear(); backStack.add(MasterPasswordKey) }
                    )
                }
                entry<GroupKey> { key ->
                    Guarded(session.isUnlocked, relock) {
                        GroupScreen(
                            groupId = key.groupUuid,
                            onGroup = { backStack.add(GroupKey(it)) },
                            onEntry = { backStack.add(EntryKey(it)) },
                        )
                    }
                }
                entry<EntryKey> { key ->
                    Guarded(session.isUnlocked, relock) { EntryScreen(key.entryUuid) }
                }
                entry<SettingsKey> {
                    SettingsScreen(
                        onImport = { backStack.add(FileBrowserKey) },
                        onWiped = { backStack.clear(); backStack.add(FileBrowserKey) },
                        onLockNow = { session.lock(); relock() },
                        onSwitchTo = { target -> backStack.add(AuthKey(target)) },
                    )
                }
                entry<AuthKey> { key ->
                    Guarded(session.isUnlocked, relock) {
                        AuthScreen(
                            onConfirmed = { backStack.removeLastOrNull(); backStack.add(setupKeyFor(key.target)) },
                            onForcedMaster = { backStack.clear(); backStack.add(MasterPasswordKey) },
                        )
                    }
                }
                entry<SafeSetupKey> {
                    DisposableEffect(Unit) { onDispose { session.clearPendingPassword() } }
                    Guarded(session.isUnlocked, relock) {
                        PinSetupScreen(
                            method = UnlockMethod.Safe,
                            onDone = { backStack.clear(); backStack.add(GroupKey()) }
                        )
                    }
                }
                entry<SafeUnlockKey> {
                    PinUnlockScreen(
                        method = UnlockMethod.Safe,
                        onUnlocked = { backStack.clear(); backStack.add(GroupKey()) },
                        onForcedMaster = { backStack.clear(); backStack.add(MasterPasswordKey) }
                    )
                }
                entry<SystemSetupKey> {
                    DisposableEffect(Unit) { onDispose { session.clearPendingPassword() } }
                    Guarded(session.isUnlocked, relock) {
                        SystemSetupScreen(onDone = { backStack.clear(); backStack.add(GroupKey()) })
                    }
                }
                entry<SystemUnlockKey> {
                    SystemUnlockScreen(
                        onUnlocked = { backStack.clear(); backStack.add(GroupKey()) },
                        onForcedMaster = { backStack.clear(); backStack.add(MasterPasswordKey) },
                    )
                }
            }
        )
    }
}

fun setupKeyFor(m: UnlockMethod): NavKey = when (m) {
    UnlockMethod.Pin -> PinSetupKey
    UnlockMethod.Safe -> SafeSetupKey
    UnlockMethod.SystemLock -> SystemSetupKey
}

fun startKey(vault: VaultRepository): NavKey = when (vault.status.value) {
    VaultStatus.NotImported -> FileBrowserKey
    VaultStatus.NeedsMasterPassword -> MasterPasswordKey
    VaultStatus.PinLocked -> PinUnlockKey
    VaultStatus.SafeLocked -> SafeUnlockKey
    VaultStatus.SystemLocked -> SystemUnlockKey
}

@Composable
private fun Guarded(unlocked: Boolean, onLocked: () -> Unit, content: @Composable () -> Unit) {
    if (unlocked) content() else LaunchedEffect(Unit) { onLocked() }
}

@Composable
fun SwipeLeftToOpen(
    enabled: Boolean = true,
    onSwipe: () -> Unit,
    content: @Composable () -> Unit,
) {
    val current by rememberUpdatedState(onSwipe)
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val threshold = DataConstants.SWIPE_THRESHOLD.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var dx = 0f
                    var dy = 0f
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (fired) event.changes.forEach { it.consume() }
                        if (!change.pressed) break
                        val delta = change.position - change.previousPosition
                        dx += delta.x
                        dy += delta.y
                        if (!fired && dx < -threshold && abs(dx) > abs(dy) * 2f) {
                            fired = true
                            event.changes.forEach { it.consume() }
                            current()
                        }
                    }
                }
            }
    ) { content() }
}