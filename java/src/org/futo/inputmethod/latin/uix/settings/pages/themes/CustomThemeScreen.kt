package org.futo.inputmethod.latin.uix.settings.pages.themes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.futo.inputmethod.latin.uix.theme.ZipThemes
import org.futo.inputmethod.latin.uix.theme.SerializableCustomTheme
import org.futo.inputmethod.latin.uix.urlDecode

@Composable
fun CustomThemeScreen(imgUri: String, navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val isEdit = imgUri.startsWith("edit:")
    val themeName = if (isEdit) imgUri.substring(5) else ""

    val loadedData = remember(imgUri) {
        if (isEdit) {
            try {
                val loaded = ZipThemes.load(context, ZipThemes.custom(themeName))
                val theme = loaded.second
                val bgBytes = theme.backgroundImage?.let { loaded.first.getFileBytes(it) }
                val bitmap = if (bgBytes != null) {
                    BitmapFactory.decodeByteArray(bgBytes, 0, bgBytes.size)
                } else null
                loaded.first.close()
                Pair(theme, bitmap)
            } catch (e: Exception) {
                Pair(null, null)
            }
        } else {
            Pair(null, null)
        }
    }

    val initialTheme = loadedData.first
    val startingBitmap = remember(imgUri, loadedData) {
        if (isEdit) {
            loadedData.second
        } else if (imgUri.isBlank()) {
            null
        } else {
            try {
                val uri = imgUri.urlDecode().toUri()
                val resolver = context.contentResolver
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    val nextAvailableName = remember(isEdit, themeName) {
        if (isEdit) {
            themeName
        } else {
            var i = 0
            val themes = ZipThemes.listCustom(context).map { it.name }
            while (themes.contains("$i")) i++
            "$i"
        }
    }

    CustomThemeEditorScreen(
        navController, 
        name = nextAvailableName, 
        startingBitmap = startingBitmap,
        initialTheme = initialTheme
    )
}

