package org.fptn.vpn.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Paints a legacy drawable resource (shape, gradient, layer-list, vector, or raster) as the
 * full-bounds background of a composable, so shapes such as `@drawable/round_back_white10_20`
 * or `@drawable/application_background` render pixel-for-pixel like they did as an XML
 * `android:background`, instead of being hand-recreated with Compose shapes/brushes.
 *
 * Uses `drawWithCache` instead of `drawBehind`: the drawable bounds are prepared ONCE per size
 * change and cached, so recompositions (e.g. connection-state flips) never re-resolve or
 * re-layout the drawable — this removes the 1-frame "hole" where the Material3 theme background
 * flashed through during state transitions.
 */
fun Modifier.legacyDrawableBackground(@DrawableRes id: Int): Modifier = composed {
    val context = LocalContext.current
    val drawable = remember(id) { ContextCompat.getDrawable(context, id) }
    if (drawable == null) {
        this
    } else {
        this.drawWithCache {
            val width = size.width.toInt().coerceAtLeast(1)
            val height = size.height.toInt().coerceAtLeast(1)
            drawable.setBounds(0, 0, width, height)
            onDrawBehind {
                drawIntoCanvas { canvas ->
                    drawable.draw(canvas.nativeCanvas)
                }
            }
        }
    }
}