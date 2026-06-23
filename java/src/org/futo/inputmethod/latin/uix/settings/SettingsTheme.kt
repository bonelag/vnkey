package org.futo.inputmethod.latin.uix.settings

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.futo.inputmethod.latin.uix.theme.applyWindowColors

data class SettingsColorScheme(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
)

private fun settingsColors(isDark: Boolean): SettingsColorScheme {
    val textWhite = Color(0xFFFFFFFF)
    val textBlack = Color(0xFF000000)
    return if (isDark) {
        SettingsColorScheme(
            background = Color(0xFF090B0D),
            onBackground = textWhite,
            surface = Color(0xFF101316),
            onSurface = textWhite,
            surfaceVariant = Color(0xFF151B20),
            onSurfaceVariant = textWhite,
            surfaceContainerHigh = Color(0xFF1B2226),
            surfaceContainerHighest = Color(0xFF222A30),
            primary = Color(0xFF72E6C2),
            onPrimary = textWhite,
            primaryContainer = Color(0xFF123A31),
            onPrimaryContainer = textWhite,
            secondary = Color(0xFF9AD8FF),
            onSecondary = textWhite,
            secondaryContainer = Color(0xFF18323D),
            onSecondaryContainer = textWhite,
            tertiary = Color(0xFF86B7FF),
            onTertiary = textWhite,
            tertiaryContainer = Color(0xFF172D4D),
            onTertiaryContainer = textWhite,
            outline = Color(0xFF53616A),
            outlineVariant = Color(0xFF2A3339),
            error = Color(0xFFFF6F6F),
            errorContainer = Color(0xFF4B171C),
            onErrorContainer = textWhite,
        )
    } else {
        SettingsColorScheme(
            background = Color(0xFFF7FAFB),
            onBackground = textBlack,
            surface = Color(0xFFFFFFFF),
            onSurface = textBlack,
            surfaceVariant = Color(0xFFEFF5F7),
            onSurfaceVariant = textBlack,
            surfaceContainerHigh = Color(0xFFE1E9EC),
            surfaceContainerHighest = Color(0xFFD7E2E6),
            primary = Color(0xFF007D61),
            onPrimary = textWhite,
            primaryContainer = Color(0xFFC9F5E8),
            onPrimaryContainer = textBlack,
            secondary = Color(0xFF25657D),
            onSecondary = textWhite,
            secondaryContainer = Color(0xFFD9F0F7),
            onSecondaryContainer = textBlack,
            tertiary = Color(0xFF225FA8),
            onTertiary = textWhite,
            tertiaryContainer = Color(0xFFD9E8FF),
            onTertiaryContainer = textBlack,
            outline = Color(0xFF7B8990),
            outlineVariant = Color(0xFFC9D4D9),
            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = textBlack,
        )
    }
}

val LocalSettingsColors = staticCompositionLocalOf { settingsColors(isDark = true) }
val LocalContentColor = staticCompositionLocalOf { Color.White }

object SettingsTheme {
    val colors: SettingsColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalSettingsColors.current
}

object SettingsTypography {
    val titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    val titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val bodyLarge = TextStyle(fontSize = 16.sp)
    val bodySmall = TextStyle(fontSize = 12.sp)
}

@Composable
fun SettingsThemeAuto(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDark = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    val colors = settingsColors(isDark)

    LaunchedEffect(colors.background) {
        val activity = context as? Activity
        activity?.window?.let {
            applyWindowColors(it, colors.background.toArgb(), statusBar = false)
        }
    }

    CompositionLocalProvider(
        LocalSettingsColors provides colors,
        LocalContentColor provides colors.onBackground,
        content = content
    )
}

fun contentColorFor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) Color(0xFF000000) else Color(0xFFFFFFFF)
}
