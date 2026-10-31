package com.kingm.happylighting.ui.theme

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.kingm.happylighting.model.AppStyleState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.kingm.happylighting.ui.AppleFont
import com.moriafly.salt.ui.saltTextStyles
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.saltConfigs
import kotlin.math.max
import kotlin.math.min

private const val UI_SCALE = 1.0f

@Composable
fun HappyLightingTheme(
    appStyle: AppStyleState,
    content: @Composable () -> Unit,
) {
    val colors = remember(appStyle) {
        val text = contrastText(appStyle)
        val base = darkSaltColors(
            highlight = accentFromStyle(appStyle),
            text = text,
            subText = text.copy(alpha = 0.6f),
        )
        if (appStyle.amoledMode) {
            base.copy(background = Color.Black, subBackground = Color(0xFF121212))
        } else {
            base
        }
    }
    val baseDensity = LocalDensity.current
    val density = remember(baseDensity) {
        Density(density = baseDensity.density * UI_SCALE, fontScale = baseDensity.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides density) {
        SaltTheme(
            configs = saltConfigs(isDarkTheme = true),
            colors = colors,
            textStyles = saltTextStyles(
                main = TextStyle(fontFamily = AppleFont, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.text),
                sub = TextStyle(fontFamily = AppleFont, fontSize = 15.sp, color = colors.subText),
                paragraph = TextStyle(fontFamily = AppleFont, fontSize = 15.sp, lineHeight = 21.sp, color = colors.subText),
            ),
            content = content,
        )
    }
}

private fun accentFromStyle(appStyle: AppStyleState): Color {
    val hsv = FloatArray(3)
    colorToHSV(
        android.graphics.Color.rgb(
            appStyle.accentColor.first,
            appStyle.accentColor.second,
            appStyle.accentColor.third,
        ),
        hsv,
    )
    hsv[1] = (hsv[1] * (appStyle.saturation / 100f)).coerceIn(0f, 1f)
    // Keep the accent legible on dark surfaces even when "match light color" follows a dim output.
    hsv[2] = (hsv[2] * (0.92f + ((appStyle.contrast - 100) / 300f))).coerceIn(0.7f, 1f)
    val colorInt = HSVToColor(hsv)
    return Color(
        red = android.graphics.Color.red(colorInt),
        green = android.graphics.Color.green(colorInt),
        blue = android.graphics.Color.blue(colorInt),
    )
}

private fun contrastText(appStyle: AppStyleState): Color {
    val normalized = ((appStyle.contrast - 60) / 100f).coerceIn(0f, 1f)
    val channel = max(0f, min(1f, 0.82f + (1f - 0.82f) * normalized))
    return Color(channel, channel, channel)
}
