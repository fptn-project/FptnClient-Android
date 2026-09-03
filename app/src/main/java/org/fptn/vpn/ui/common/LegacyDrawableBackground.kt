package org.fptn.vpn.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
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
 * `painterResource` only understands `<vector>` XML and raster images — it throws on plain
 * `<shape>`/gradient/layer-list drawables, which is exactly what `application_background` and
 * every `round_*_100`/`round_*_20` card background are. `drawBehind` draws the resolved
 * `Drawable` directly through its own `draw(Canvas)` (which every drawable type supports) and,
 * unlike `Modifier.paint`, is draw-phase only — a background can never end up influencing the
 * element's own measured size, exactly like `android:background` never did for a View.
 */
fun Modifier.legacyDrawableBackground(@DrawableRes id: Int): Modifier = composed {
    val context = LocalContext.current
    val drawable = remember(id) { ContextCompat.getDrawable(context, id) }
    if (drawable == null) {
        this
    } else {
        this.drawBehind {
            val width = size.width.toInt().coerceAtLeast(1)
            val height = size.height.toInt().coerceAtLeast(1)
            drawIntoCanvas { canvas ->
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}
