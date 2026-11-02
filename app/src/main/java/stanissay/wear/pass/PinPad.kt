package stanissay.wear.pass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.utils.MainConstants
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.PaddingStyle
import stanissay.wear.pass.utils.PageSubTitle
import stanissay.wear.pass.utils.PageTitle

@Composable
fun PinPad(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    resetKey: Any = Unit,
    onComplete: (CharArray) -> Unit,
) {
    var digits by remember(resetKey) { mutableStateOf("") }

    fun press(d: Char) {
        if (!enabled || digits.length >= MainConstants.PIN_LENGTH) return
        digits += d
        if (digits.length == MainConstants.PIN_LENGTH) onComplete(digits.toCharArray())
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PageTitle(title)
        subtitle?.let { PageSubTitle(it) }

        Row(
            modifier = Modifier.padding(PaddingStyle.verticalXs),
            horizontalArrangement = Arrangement.spacedBy(MainConstants.SPACE),
        ) {
            repeat(MainConstants.PIN_LENGTH) { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < digits.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { c -> KeyButton(c.toString()) { press(c) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.size(MainConstants.BUTTON_SIZE))
                KeyButton("0") { press('0') }
                KeyButton("⌫") { digits = digits.dropLast(1) }
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(MainConstants.BUTTON_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { MainText(label) }
}