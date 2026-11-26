package com.bringyour.network.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
 * selection behind a tab springs in -- the pill stretches from a point to the
 * tab's full size with a bounce, so the highlight reads as sliding between
 * tabs rather than blinking.
 */
@Composable
fun MorphingNavBar(
    items: List<Pair<CustomNavItem, Boolean>>,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = AppTheme.bgElevated,
        border = BorderStroke(1.dp, AppTheme.border),
        shadowElevation = 16.dp,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
        ) {
            items.forEachIndexed { index, (item, selected) ->
                val pillProgress by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "pill",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTab(index) }
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center,
                ) {
                    // the pill behind the item, sprung in and out
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = pillProgress.coerceIn(0.01f, 1f)
                                scaleY = pillProgress.coerceIn(0.01f, 1f)
                                alpha = pillProgress
                            }
                            .background(AppTheme.bgTinted, RoundedCornerShape(20.dp)),
                    )
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
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Pink else AppTheme.textMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The Capsule navigation bar: a floating stadium of icons only, the selected
 * icon highlighted by a circular fill; the active tab's name rides in a
 * small pill under the capsule, so the icon-only bar still names where you
 * are.
 */
@Composable
fun CapsuleNavBar(
    items: List<Pair<CustomNavItem, Boolean>>,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onTab(index) }
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AppTheme.bgTinted, CircleShape),
                            )
                        }
                        Icon(
                            painter = painterResource(
                                id = if (selected) item.selectedIconRes else item.iconRes,
                            ),
                            contentDescription = item.label,
                            tint = if (selected) Pink else AppTheme.textMuted,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // the active tab's name under the capsule
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AppTheme.bgTinted,
        ) {
            val activeLabel = items.firstOrNull { it.second }?.first?.label ?: ""
            Text(
                activeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Pink,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * The Vertical Side Dock: a floating rounded dock on the right edge of the
 * screen, the tabs stacked top to bottom with their names under their icons,
 * the selected tab highlighted by a pill.
 */
@Composable
fun SideDockNav(
    items: List<Pair<CustomNavItem, Boolean>>,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = AppTheme.bgElevated,
        border = BorderStroke(1.dp, AppTheme.border),
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            items.forEachIndexed { index, (item, selected) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTab(index) }
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(AppTheme.bgTinted, RoundedCornerShape(20.dp)),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (selected) item.selectedIconRes else item.iconRes,
                            ),
                            contentDescription = item.label,
                            tint = if (selected) Pink else AppTheme.textMuted,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Pink else AppTheme.textMuted,
                        )
                    }
                }
            }
        }
    }
}
