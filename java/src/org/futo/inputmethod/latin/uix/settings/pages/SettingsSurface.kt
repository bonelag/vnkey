package org.futo.inputmethod.latin.uix.settings.pages

import org.futo.inputmethod.latin.uix.settings.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.futo.inputmethod.latin.uix.theme.Typography

@Composable
fun ParagraphText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current.copy(alpha = 0.9f)
) {
    Text(text, modifier = modifier, style = Typography.SmallMl, color = color)
}

@Composable
fun SettingsSurfaceHeading(title: String) {
    Text(
        title,
        style = Typography.Body.MediumMl,
        color = LocalContentColor.current
    )
}

@Composable
fun SettingsSurface(isPrimary: Boolean, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val containerColor = if (isPrimary) {
        SettingsTheme.colors.primaryContainer
    } else {
        SettingsTheme.colors.surface
    }

    val contentColor = if (isPrimary) {
        SettingsTheme.colors.onPrimaryContainer
    } else {
        SettingsTheme.colors.onSurface
    }

    val borderColor = if (isPrimary) {
        SettingsTheme.colors.primary.copy(alpha = 0.34f)
    } else {
        SettingsTheme.colors.outlineVariant.copy(alpha = 0.72f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Center
    ) {
        Surface(
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .let {
                    if (onClick != null) {
                        it.clickable { onClick() }
                    } else {
                        it
                    }
                }
                .widthIn(Dp.Unspecified, 520.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Column(
                    Modifier.padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}
