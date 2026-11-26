package com.bringyour.network.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A country flag at its true aspect ratio: the full flag as a rounded
 * rectangle (the standard flag art flagcdn serves), with a hairline border
 * for readability on the dark background. [height] sets the chip's height;
 * the width follows the flag's own proportions (3:2 flags render 1.5x the
 * height, square and portrait flags their own shape).
 */
@Composable
fun RoundedFlag(
    imageResourceId: Int,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(id = imageResourceId)
    val intrinsic = painter.intrinsicSize
    val aspect = if (intrinsic.width > 0f && intrinsic.height > 0f) {
        intrinsic.width / intrinsic.height
    } else {
        1.5f
    }
    val shape = RoundedCornerShape(6.dp)
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(width = height * aspect, height = height)
            .clip(shape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = shape,
            )
    )
}
