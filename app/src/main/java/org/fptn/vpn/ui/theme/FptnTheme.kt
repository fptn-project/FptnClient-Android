package org.fptn.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF1B023B)
val Secondary = Color(0xFF03DAC5)
val White = Color(0xFFFFFFFF)
val Yellow = Color(0xFFBCBC1C)
val Hint = Color(0xFF999E9D)
val White10 = Color(0x1AFFFFFF)
val DeniedRed = Color(0xFFCC0000)
val GrantedGreen = Color(0xFF64DD17)

private val FptnColors = darkColorScheme(
    primary = Secondary,
    onPrimary = Primary,
    primaryContainer = Secondary,
    onPrimaryContainer = Primary,
    secondary = Secondary,
    onSecondary = Primary,
    background = Primary,
    onBackground = White,
    surface = Primary,
    onSurface = White,
    surfaceVariant = White10,
    onSurfaceVariant = White,
    outline = White10,
    error = Yellow,
    onError = Primary,
)

@Composable
fun FptnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FptnColors,
        content = content
    )
}
