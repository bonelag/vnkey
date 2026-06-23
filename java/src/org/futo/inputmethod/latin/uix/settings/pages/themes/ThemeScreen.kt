package org.futo.inputmethod.latin.uix.settings.pages.themes

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.THEME_KEY
import org.futo.inputmethod.latin.uix.settings.RotatingChevronIcon
import org.futo.inputmethod.latin.uix.settings.Route
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.theme.ZipThemes
import org.futo.inputmethod.latin.uix.theme.Typography
import org.futo.inputmethod.latin.uix.theme.selector.ThemePicker
import org.futo.inputmethod.latin.uix.theme.selector.ZipThemePreview
import org.futo.inputmethod.latin.uix.urlEncode


@Composable
fun DeleteCustomThemeDialog(name: String, navController: NavHostController) {
    val context = LocalContext.current
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        icon = {},
        title = {
            Text("Chọn thao tác", style = Typography.Body.MediumMl, color = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        text = {
            ZipThemePreview(ZipThemes.custom(name), true, Modifier, null) { }
        },
        onDismissRequest = {
            navController.navigateUp()
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { navController.navigateUp() }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.primary, style = Typography.Body.Medium, maxLines = 1, softWrap = false)
                }
                
                OutlinedButton(
                    onClick = {
                        ZipThemes.delete(context, ZipThemes.custom(name))
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, style = Typography.Body.Medium, maxLines = 1, softWrap = false)
                }

                Button(onClick = {
                    navController.navigateUp()
                    navController.navigate(Route.CustomTheme("edit:$name"))
                }) {
                    Text("Chỉnh sửa", style = Typography.Body.Medium, maxLines = 1, softWrap = false)
                }
            }
        }
    )
}

@Preview
@Composable
fun ThemeScreen(navController: NavHostController = rememberNavController()) {
    val (theme, _) = useDataStore(THEME_KEY.key, THEME_KEY.default)

    val context = LocalContext.current
    val enableKeyboardPreview = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    var showKeyboard by remember { mutableStateOf(false) }

    if (enableKeyboardPreview) {
        val textInputService = LocalTextInputService.current
        val rootView = (context as? Activity)?.window?.decorView?.rootView
        val session = remember { mutableStateOf<TextInputSession?>(null) }

        DisposableEffect(showKeyboard, theme) {
            val service = textInputService ?: return@DisposableEffect onDispose { }

            if (showKeyboard) {
                session.value = service.startInput(
                    TextFieldValue(""),
                    imeOptions = ImeOptions.Default.copy(
                        platformImeOptions = PlatformImeOptions(
                            privateImeOptions = "org.futo.inputmethod.latin.NoSuggestions=1"
                        )
                    ),
                    onEditCommand = { },
                    onImeActionPerformed = { }
                )
            }

            onDispose {
                service.stopInput(session.value ?: return@onDispose)
            }
        }

        // Detect manual keyboard dismissal (e.g., back button press)
        rootView?.let { view ->
            DisposableEffect(view) {
                ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                    val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                    if (!isKeyboardVisible && showKeyboard) {
                        showKeyboard = false
                    }
                    insets
                }

                onDispose {
                    ViewCompat.setOnApplyWindowInsetsListener(view, null)
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (enableKeyboardPreview) {
                SmallFloatingActionButton(
                    onClick = {
                        showKeyboard = !showKeyboard
                    }
                ) {
                    RotatingChevronIcon(!showKeyboard)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ScreenTitle(stringResource(R.string.theme_settings_title), showBack = true, navController)
            ThemePicker({
                navController.navigate(Route.DeleteTheme(it))
            }, {
                navController.navigate(Route.CustomTheme(""))
            })
        }
    }
}