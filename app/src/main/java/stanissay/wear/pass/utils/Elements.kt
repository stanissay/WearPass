package stanissay.wear.pass.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorColors
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ripple

@Composable
fun MainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorStyle,
        typography = TypographyStyle,
        shapes = ShapesStyle
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { content() }
    }
}

@Composable
fun CircularBar() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingStyle.circularBar)
            .background(MainColors.TRANSPARENT),
        contentAlignment = Alignment.BottomCenter
    ) {
        CircularProgressIndicator(
            colors = ProgressIndicatorColors(
                indicatorBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainer),
                trackBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                overflowTrackBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                disabledIndicatorBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainer),
                disabledTrackBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                disabledOverflowTrackBrush = SolidColor(MaterialTheme.colorScheme.secondary)
            )
        )
    }
}

@Composable
fun MainCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    borderColor: Color = MainColors.TRANSPARENT,
    contentPadding: PaddingValues = PaddingStyle.none,
    containerPadding: PaddingValues = PaddingStyle.none,
    shape: Shape = ShapesStyle.medium,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(containerPadding)
            .shadow(
                elevation = UiConstants.ELEVATION,
                shape = shape
            )
            .clip(shape)
            .background(containerColor)
            .border(
                width = UiConstants.THICKNESS,
                color = borderColor,
                shape = shape
            )
    ) { Box(modifier = Modifier.padding(contentPadding)) { content() } }
}

@Composable
fun ClickableBox(
    modifier: Modifier = Modifier,
    background: Color = MainColors.TRANSPARENT,
    borderColor: Color = MainColors.TRANSPARENT,
    shape: Shape = ShapesStyle.small,
    contentAlignment: Alignment = Alignment.Center,
    rippleRadius: Dp = Dp.Unspecified,
    rippleColor: Color = MaterialTheme.colorScheme.primary,
    bounded: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val clickModifier = when {
        onLongClick == null && onDoubleClick == null -> {
            Modifier.clickable(
                onClick = onClick,
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
            )
        }
        onLongClick == null && onDoubleClick != null -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDoubleClick()
                },
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
            )
        }
        onLongClick != null && onDoubleClick == null -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
            )
        }
        else -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke()
                },
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDoubleClick?.invoke()
                },
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(
                width = UiConstants.THICKNESS,
                color = borderColor,
                shape = shape
            )
            .then(clickModifier)
    ) {
        Box(
            modifier = Modifier
                .align(contentAlignment)
                .matchParentSize(),
            contentAlignment = contentAlignment
        ) { content() }
    }
}

@Composable
fun MainText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontSize: TextUnit = style.fontSize,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = MaterialTheme.colorScheme.onBackground,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textDecoration: TextDecoration = TextDecoration.None
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = style,
        color = color,
        maxLines = maxLines,
        textDecoration = textDecoration,
        overflow = overflow,
        fontSize = fontSize
    )
}

@Composable
fun MainText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontSize: TextUnit = style.fontSize,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = MaterialTheme.colorScheme.onBackground,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textDecoration: TextDecoration = TextDecoration.None
) {
    MainText(
        text = AnnotatedString(text),
        modifier = modifier,
        style = style,
        textAlign = textAlign,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textDecoration = textDecoration,
        fontSize = fontSize
    )
}

@Composable
fun PageTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    MainText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.displayMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 2
    )
}

@Composable
fun PageSubTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    MainText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = textAlign,
        maxLines = 2
    )
}

@Composable
fun MainDivider() {
    Box(
        modifier = Modifier.fillMaxWidth().height(UiConstants.THICKNESS)
            .padding(horizontal = UiConstants.MAIN_PADDING)
            .background(MaterialTheme.colorScheme.outline)
    )
}