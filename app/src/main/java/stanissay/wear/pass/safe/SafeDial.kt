package stanissay.wear.pass.safe

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.wear.compose.material3.MaterialTheme
import stanissay.wear.pass.App
import stanissay.wear.pass.utils.ClickableBox
import stanissay.wear.pass.utils.DataConstants
import stanissay.wear.pass.utils.LocalAmbient
import stanissay.wear.pass.utils.MainCheckBox
import stanissay.wear.pass.utils.MainText
import stanissay.wear.pass.utils.UiConstants
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun SafeDial(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    resetKey: Any = Unit,
    onComplete: (CharArray) -> Unit,
) {
    val app = LocalContext.current.applicationContext as App
    val view = LocalView.current
    val ambient = LocalAmbient.current
    val haptics = remember(view) { DialHaptics(view) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnComplete by rememberUpdatedState(onComplete)
    val logic = remember(resetKey, DataConstants.SAFE_LENGTH) { SafeDialLogic(DataConstants.SAFE_LENGTH) }
    var cursor by remember(logic) { mutableStateOf<Int?>(null) }
    var count by remember(logic) { mutableIntStateOf(0) }
    val rotary = remember(logic) { FloatArray(1) }
    var pendingLast by remember(logic) { mutableStateOf(false) }
    val dwell = remember(logic) { Animatable(0f) }
    val primary = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground)
    val measurer = rememberTextMeasurer()
    val normal = remember(measurer) {
        List(10) { measurer.measure(it.toString(), textStyle) }
    }
    val selected = remember(measurer) {
        List(10) { measurer.measure(it.toString(), textStyle) }
    }
    val onStep: (Int) -> Unit = remember(logic, haptics) {
        { dir ->
            if (currentEnabled) {
                val r = logic.step(dir)
                when (r) {
                    SafeResult.Moved -> haptics.tick()
                    SafeResult.Committed, SafeResult.Completed -> haptics.commit()
                    SafeResult.Ignored -> Unit
                }
                cursor = logic.cursor
                count = logic.count
                pendingLast = logic.awaitingLast
                if (r == SafeResult.Completed) currentOnComplete(logic.code())
            }
        }
    }
    val onClearAll: () -> Unit = remember(logic, haptics) {
        { if (currentEnabled) { logic.clear(); haptics.clear(); cursor = null; count = 0; pendingLast = false } }
    }
    val onTap: () -> Unit = remember(logic, haptics, DataConstants.SAFE_LENGTH) {
        {
            val allowed = logic.count == DataConstants.SAFE_LENGTH - 1
            if (currentEnabled && allowed) {
                val r = logic.commitCurrent()
                if (r != SafeResult.Ignored) {
                    haptics.commit()
                    cursor = logic.cursor
                    count = logic.count
                    pendingLast = false
                    if (r == SafeResult.Completed) currentOnComplete(logic.code())
                }
            }
        }
    }
    val onDwellCommit: () -> Unit = remember(logic, haptics) {
        {
            val r = logic.commitByDwell()
            if (currentEnabled && r == SafeResult.Completed) {
                haptics.commit()
                cursor = logic.cursor
                count = logic.count
                pendingLast = false
                currentOnComplete(logic.code())
            }
        }
    }
    val handleRotary: (Float) -> Boolean = remember(logic) {
        { px ->
            if (px != 0f && rotary[0] != 0f && (px > 0f) != (rotary[0] > 0f)) rotary[0] = 0f
            rotary[0] += px
            while (rotary[0] >= DataConstants.PX_PER_STEP) { rotary[0] -= DataConstants.PX_PER_STEP; onStep(+1) }
            while (rotary[0] <= -DataConstants.PX_PER_STEP) { rotary[0] += DataConstants.PX_PER_STEP; onStep(-1) }
            true
        }
    }

    BackHandler(enabled = !app.swipeGate.enabled) {}

    LaunchedEffect(logic, cursor, count, pendingLast) {
        dwell.snapTo(0f)
        if (pendingLast && currentEnabled) {
            dwell.animateTo(1f, tween(DataConstants.SAFE_DWELL_MS, easing = LinearEasing))
            onDwellCommit()
        }
    }

    LaunchedEffect(ambient) {
        if (ambient) { logic.clear(); cursor = null; count = 0; pendingLast = false }
    }

    DisposableEffect(handleRotary) {
        app.rotaryBus.listener = handleRotary
        onDispose { if (app.rotaryBus.listener === handleRotary) app.rotaryBus.listener = null }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val diameter = minOf(maxWidth, maxHeight)

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val innerR = minOf(size.width, size.height) / 2f * DataConstants.CENTER_FRACTION
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val onRing = hypot(down.position.x - cx, down.position.y - cy) > innerR
                        if (onRing) app.swipeGate.block()
                        try {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                            } while (event.changes.any { it.pressed })
                        } finally {
                            if (onRing) app.swipeGate.unblock()
                        }
                    }
                }
                .pointerInput(logic) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val innerR = minOf(size.width, size.height) / 2f * DataConstants.CENTER_FRACTION
                    var active = false
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { o -> active = hypot(o.x - cx, o.y - cy) > innerR; acc = 0f },
                        onDragEnd = { active = false },
                        onDragCancel = { active = false },
                    ) { change, _ ->
                        if (!active) return@detectDragGestures
                        change.consume()
                        val a1 = atan2(change.previousPosition.x - cx, cy - change.previousPosition.y)
                        val a2 = atan2(change.position.x - cx, cy - change.position.y)
                        var d = a2 - a1
                        if (d > DataConstants.TWO_PI / 2f) d -= DataConstants.TWO_PI
                        if (d < -DataConstants.TWO_PI / 2f) d += DataConstants.TWO_PI
                        acc += d
                        while (acc >= DataConstants.STEP_RAD) { acc -= DataConstants.STEP_RAD; onStep(+1) }
                        while (acc <= -DataConstants.STEP_RAD) { acc += DataConstants.STEP_RAD; onStep(-1) }
                    }
                },
        ) {
            val c = center
            val r = size.minDimension / 2f
            val ringR = r * DataConstants.RING_FRACTION
            val chipR = r * DataConstants.CHIP_FRACTION

            fun pos(deg: Float, radius: Float): Offset {
                val a = Math.toRadians(deg.toDouble())
                return Offset(c.x + (radius * sin(a)).toFloat(), c.y - (radius * cos(a)).toFloat())
            }

            if (cursor == null) {
                drawCircle(primary, radius = UiConstants.SPACE.toPx(), center = pos(-18f, ringR))
            }
            for (d in 0..9) {
                val p = pos(d * 36f, ringR)
                val sel = cursor == d
                if (sel) drawCircle(primary, radius = chipR, center = p)
                val layout = if (sel) selected[d] else normal[d]
                drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
            }
            val progress = dwell.value
            val cur = cursor
            if (pendingLast && cur != null && progress > 0f) {
                val p = pos(cur * 36f, ringR)
                val arcR = chipR + UiConstants.SPACE.toPx()
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(p.x - arcR, p.y - arcR),
                    size = Size(arcR * 2, arcR * 2),
                    style = Stroke(width = UiConstants.THICKNESS.toPx(), cap = StrokeCap.Round),
                )
            }
        }

        ClickableBox(
            modifier = Modifier.size(diameter * DataConstants.CENTER_FRACTION)
                .align(Alignment.Center)
                .clip(CircleShape),
            onClick = { onTap() },
            onLongClick = { onClearAll() }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiConstants.SPACE)
            ) {
                MainText(subtitle ?: title)
                Row(horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACE)) {
                    repeat(DataConstants.SAFE_LENGTH) { i ->
                        MainCheckBox(check = i < count)
                    }
                }
            }
        }
    }
}