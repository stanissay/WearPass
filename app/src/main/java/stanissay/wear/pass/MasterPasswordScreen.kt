package stanissay.wear.pass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.utils.CircularBar
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle

@Composable
fun MasterPasswordScreen(
    onOpened: () -> Unit,
    vm: MasterPasswordViewModel = viewModel(),
) {
    val ui by vm.state.collectAsState()
    val field = rememberTextFieldState()
    val working = ui is UnlockState.Working

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ListHeader { MainText("Майстер-пароль") } }
        item {
            BasicSecureTextField(
                state = field,
                enabled = !working,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                onKeyboardAction = KeyboardActionHandler {
                    vm.unlock(field.text.toString(), onOpened)
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                    .padding(PaddingStyle.xs),
            )
        }
        item {
            Button(
                onClick = { vm.unlock(field.text.toString(), onOpened) },
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
                label = { MainText("Відкрити") },
            )
        }
        when (val s = ui) {
            UnlockState.Working -> item { CircularBar() }
            is UnlockState.Error -> item { MainText(s.message) }
            UnlockState.Idle -> Unit
        }
    }
}