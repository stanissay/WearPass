package stanissay.wear.pass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.VaultStatus

@Composable
fun AppNav(vault: VaultRepository) {
    val start = remember {
        when (vault.status.value) {
            VaultStatus.NotImported -> FileBrowserKey
            VaultStatus.NeedsMasterPassword -> MasterPasswordKey
            VaultStatus.PinLocked -> PinUnlockKey
        }
    }
    val backStack = rememberNavBackStack(start)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryProvider = entryProvider {
            entry<FileBrowserKey> {
                FileBrowserScreen(onImported = {
                    backStack.clear(); backStack.add(MasterPasswordKey)
                })
            }
            entry<MasterPasswordKey> { Stub("Майстер-пароль") }
            entry<PinSetupKey> { Stub("Створи PIN") }
            entry<PinUnlockKey> { Stub("PIN") }
            entry<GroupKey> { Stub("Групи й записи") }
            entry<EntryKey> { Stub("Запис") }
            entry<SettingsKey> { Stub("Налаштування") }
        },
    )
}

@Composable private fun Stub(title: String) = Box(Modifier.fillMaxSize(), Alignment.Center) { MainText(title) }