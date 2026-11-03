package stanissay.wear.pass.master

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.R
import stanissay.wear.pass.utils.CircularBar
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.ClickableListItem
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle
import stanissay.wear.pass.utils.PageTitle
import stanissay.wear.pass.utils.UiConstants

@Composable
fun MasterPasswordScreen(onOpened: () -> Unit, viewModel: MasterViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val field = rememberTextFieldState()
    val working = state is UnlockState.Working

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { ListHeader { PageTitle(stringResource(R.string.master_password)) } }
        item {
            BasicSecureTextField(
                state = field,
                enabled = !working,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Start),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                onKeyboardAction = KeyboardActionHandler {
                    viewModel.unlock(field.text.toString(), onOpened)
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.outline),
                decorator = TextFieldDecorator { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(PaddingStyle.xs),
                        contentAlignment = Alignment.CenterStart,
                    ) { innerTextField() }
                },
                modifier = Modifier.height(UiConstants.BASE_SIZE).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                    .padding(PaddingStyle.xs)
            )
        }
        item {
            ClickableListItem(
                onClick = {
                    if(working) return@ClickableListItem
                    viewModel.unlock(field.text.toString(), onOpened)
                }
            ) { MainText(stringResource(R.string.open)) }
        }
        when (val s = state) {
            UnlockState.Working -> item { CircularBar() }
            is UnlockState.Error -> item { MainText(s.message) }
            UnlockState.Idle -> Unit
        }
    }
}