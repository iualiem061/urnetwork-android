package com.bringyour.network.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * The app's motion kit: the small, soft, consistent animations shared by every
 * screen -- the press bounce of a tappable row or button, the staggered fade
 * and rise with which sections and cards settle in, and the quiet breathing
 * dot of a live status. Everything here is built from springs and short tweens
 * so the app-wide feel stays one style: quick, gentle, never bouncy beyond a
 * small overshoot.
 */

/**
 * A press feedback: while the pointer is down the element eases to
 * [scaleDown] and springs back on release. Attach the same
 * [interactionSource] to the clickable that drives it.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    scaleDown: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A clickable with the app's press bounce and an optional soft haptic tick:
 * the row eases in a touch while pressed and springs back on release.
 */
fun Modifier.bouncyClick(
    haptic: Boolean = false,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    pressScale(interactionSource, scaleDown)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
        ) {
            if (haptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
}

/**
 * The entrance every section, card and row uses: starts invisible a little
 * below its place and settles up into it, later siblings arriving a beat
 * later, so a screen reads as cascading in rather than blinking on.
 *
 * [index] is the element's position in its group; the stagger is capped so
 * long lists do not wait behind a queue of delays.
 */
fun Modifier.entrance(
    index: Int = 0,
    rise: Dp = 14.dp,
    durationMillis: Int = 340,
): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = min(index * 45, 360),
            easing = FastOutSlowInEasing,
        ),
        label = "entrance",
    )
    graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1f - progress) * rise.toPx()
    }
}

/**
 * A status dot that breathes: a slow, quiet pulse of alpha and size, the
 * way a live indicator reads as alive without pulling attention. [pulsing]
 * turns it into a plain solid dot (a settled state).
 */
@Composable
fun PulseDot(
    color: Color,
    size: Dp = 10.dp,
    pulsing: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val a = if (pulsing) alpha else 1f
                val s = if (pulsing) scale else 1f
                this.alpha = a
                scaleX = s
                scaleY = s
            }
            .background(color, CircleShape),
    )
}

/**
 * The soft slide a swapped value rides on: a small offset that eases to the
 * [target] with a medium spring, for one-line values that move as one
 * (a tab's name, a counter). Use from a graphicsLayer's translation.
 */
fun Modifier.offsetSpring(
    target: () -> Float,
): Modifier = composed {
    val value by animateFloatAsState(
        targetValue = target(),
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = StiffnessMediumLow,
        ),
        label = "offsetSpring",
    )
    graphicsLayer {
        translationX = value
    }
}
