package com.bringyour.network.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bringyour.network.ui.theme.AppTheme
import com.bringyour.network.ui.theme.Pink

/**
 * One tab of a custom navigation bar: the two icon variants (unselected and
 * selected, the same pair the classic bar uses), its label, and its test tag.
 */
class CustomNavItem(
    val iconRes: Int,
    val selectedIconRes: Int,
    val label: String,
    val testTag: String,
)

/**
 * The Morphing navigation bar: a floating rounded pill over the content. The
 * highlight behind the tabs slides from the old tab to the new one with a
 * springy glide, the chosen icon pops up a touch and settles, and each tap
 * lands with a soft haptic tick, so switching tabs reads as one continuous
 * motion instead of a blink.
 */
@Composable
fun MorphingNavBar(
    items: List<Pair<CustomNavItem, Boolean>>,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = items.size.coerceAtLeast(1)
    val selectedIndex = items.indexOfFirst { it.second }.let { if (it < 0) 0 else it }
    val haptics = LocalHapticFeedback.current

    // where the pill sits, as the fraction of the row the glide covers
    val pillPosition by animateFloatAsState(
        targetValue = selectedIndex / count.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pillPosition",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = AppTheme.bgElevated,
        border = BorderStroke(1.dp, AppTheme.border),
        shadowElevation = 16.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            // the sliding highlight: one slot wide, gliding between tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f / count)
                    .graphicsLayer {
                        translationX = pillPosition * size.width * count
                    }
                    .background(AppTheme.bgTinted, RoundedCornerShape(20.dp)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items.forEachIndexed { index, (item, selected) ->
                    val pop by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "pop",
                    )
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .pressScale(interactionSource, scaleDown = 0.94f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onTab(index)
                            }
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (selected) item.selectedIconRes else item.iconRes,
                                ),
                                contentDescription = item.label,
                                tint = if (selected) Pink else AppTheme.textMuted,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        val s = 1f + 0.14f * pop
                                        scaleX = s
                                        scaleY = s
                                    },
                            )
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Pink else AppTheme.textMuted,
                                modifier = Modifier.graphicsLayer { alpha = 0.75f + 0.25f * pop },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Neon navigation bar: a floating full-width bar of icons only. The
 * selected tab carries a soft neon aura -- a glow orb that breathes quietly
 * behind the icon -- and the aura glides from the old tab to the new one with
 * the same springy glide the morphing pill uses, so the light itself appears
 * to travel. Each tap lands with the soft haptic tick and the chosen icon
 * pops and settles.
 */
@Composable
fun NeonGlowNavBar(
    items: List<Pair<CustomNavItem, Boolean>>,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = items.size.coerceAtLeast(1)
    val selectedIndex = items.indexOfFirst { it.second }.let { if (it < 0) 0 else it }
    val haptics = LocalHapticFeedback.current

    // where the glow orb sits, as the fraction of the row the glide covers
    val glowPosition by animateFloatAsState(
        targetValue = selectedIndex / count.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "glowPosition",
    )

    // the aura breathes: a slow, quiet pulse of its own alpha
    val breath = rememberInfiniteTransition(label = "neonBreath")
    val auraAlpha by breath.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "auraAlpha",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = AppTheme.bgElevated,
        border = BorderStroke(1.dp, AppTheme.border),
        shadowElevation = 16.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            // the gliding glow orb: one slot wide, its soft neon light
            // centered behind the chosen icon, gliding between tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f / count)
                    .graphicsLayer {
                        translationX = glowPosition * size.width * count
                    }
                    .drawBehind {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val halo = size.minDimension * 0.92f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Pink.copy(alpha = 0.38f * auraAlpha),
                                    Pink.copy(alpha = 0.12f * auraAlpha),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = halo,
                            ),
                            radius = halo,
                            center = center,
                        )
                    },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items.forEachIndexed { index, (item, selected) ->
                    val pop by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "pop",
                    )
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .pressScale(interactionSource, scaleDown = 0.94f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onTab(index)
                            }
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    val s = if (selected) 1f else 0f
                                    scaleX = (0.4f + 0.6f * s).coerceIn(0.01f, 1f)
                                    scaleY = (0.4f + 0.6f * s).coerceIn(0.01f, 1f)
                                    alpha = 0.35f + 0.65f * s
                                }
                                .background(AppTheme.bgTinted, CircleShape),
                        )
                        Icon(
                            painter = painterResource(
                                id = if (selected) item.selectedIconRes else item.iconRes,
                            ),
                            contentDescription = item.label,
                            tint = if (selected) Pink else AppTheme.textMuted,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val s = 1f + 0.16f * pop
                                    scaleX = s
                                    scaleY = s
                                },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The Bubble navigation bar: a floating bar of round bubbles. The selected
 * bubble inflates with a springy overshoot into a wide pill that carries the
 * tab's name inside it and lifts a touch above its siblings; the rest stay
 * small quiet circles. A tap on a bubble lands with the soft haptic tick and
 * the bounce, so the bar reads as one row of beads with one grown.
 */
@Composable
fun BubbleNavBar(
    items: List<Pair<CustomNavItem, Boolean>>,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = AppTheme.bgElevated,
        border = BorderStroke(1.dp, AppTheme.border),
        shadowElevation = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            items.forEachIndexed { index, (item, selected) ->

                // how inflated this bubble is: 0 = the quiet circle,
                // 1 = the full pill with its name inside
                val inflate by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "inflate",
                )
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .graphicsLayer {
                                // the grown bubble sits a touch above its siblings
                                translationY = -6.dp.toPx() * inflate
                            }
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                if (selected) AppTheme.bgTinted else Color.Transparent
                            )
                            .pressScale(interactionSource, scaleDown = 0.92f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onTab(index)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (selected) item.selectedIconRes else item.iconRes,
                            ),
                            contentDescription = item.label,
                            tint = if (selected) Pink else AppTheme.textMuted,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val s = 0.92f + 0.16f * inflate
                                    scaleX = s
                                    scaleY = s
                                },
                        )
                        // the tab's name rides along inside the grown bubble
                        AnimatedVisibility(visible = selected) {
                            Row {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Pink,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
/**
 * The Capsule navigation bar: a floating stadium of icons only. The selected
 * icon's circular fill springs in with a small overshoot, the icon itself
 * pops and settles, and the name pill under the capsule swaps with a soft
 * slide when the tab changes.
 */
@Composable
fun CapsuleNavBar(
    items: List<Pair<CustomNavItem, Boolean>>,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = AppTheme.bgElevated,
            border = BorderStroke(1.dp, AppTheme.border),
            shadowElevation = 16.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                items.forEachIndexed { index, (item, selected) ->
                    // the circular fill behind the selected icon, sprung in
                    val fill by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.45f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "fill",
                    )
                    val pop by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "pop",
                    )
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .pressScale(interactionSource, scaleDown = 0.92f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onTab(index)
                            }
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    val s = fill.coerceIn(0.01f, 1f)
                                    scaleX = s
                                    scaleY = s
                                    alpha = fill
                                }
                                .background(AppTheme.bgTinted, CircleShape),
                        )
                        Icon(
                            painter = painterResource(
                                id = if (selected) item.selectedIconRes else item.iconRes,
                            ),
                            contentDescription = item.label,
                            tint = if (selected) Pink else AppTheme.textMuted,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val s = 1f + 0.18f * pop
                                    scaleX = s
                                    scaleY = s
                                },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // the active tab's name under the capsule, sliding softly on a switch
        val activeLabel = items.firstOrNull { it.second }?.first?.label ?: ""
        AnimatedContent(
            targetState = activeLabel,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { it / 2 },
                ) + fadeIn()) togetherWith
                    (slideOutVertically(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        targetOffsetY = { -it / 2 },
                    ) + fadeOut())
            },
            label = "activeLabel",
        ) { label ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AppTheme.bgTinted,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Pink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }
    }
}
