package org.futo.inputmethod.latin.uix.theme

import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class KeyBackground(
    val foregroundColor: Int?,
    val padding: Rect,
    val background: Drawable
)

data class KeyIcon(
    val drawable: Drawable
)

data class AdvancedThemeOptions(
    val backgroundShader: String? = null,
    val backgroundImage: ImageBitmap? = null,
    val backgroundImageVisibleArea: Rect? = null,
    val thumbnailImage: ImageBitmap? = null,
    val thumbnailScale: Float = 1.0f,
    val keyRoundness: Float = 1.0f,
    val keyBorders: Boolean? = null,
    val keyBorderSize: Float = 0.0f,
    val keyShadowSize: Float = 0.0f,
    val keyBorderColor: Color? = null,
    val spacebarColor: Color? = null,
    val onSpacebarColor: Color? = null,
    val spacebarBorderColor: Color? = null,
    val functionalKeyColor: Color? = null,
    val onFunctionalKeyColor: Color? = null,
    val functionalKeyBorderColor: Color? = null,
    val toolbarColor: Color? = null,
    val onToolbarColor: Color? = null,
    val popupColor: Color? = null,
    val onPopupColor: Color? = null,
    val keyboardHeightScale: Float = 1.0f,
    val keyBackgrounds: KeyedBitmaps<KeyBackground>? = null,
    val keyIcons: KeyedBitmaps<KeyIcon>? = null,
    val font: Typeface? = null,
    val themeName: String? = null,
    val themeAuthor: String? = null,
)