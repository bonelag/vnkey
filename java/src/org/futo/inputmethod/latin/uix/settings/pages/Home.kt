package org.futo.inputmethod.latin.uix.settings.pages

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.futo.inputmethod.latin.BuildConfig
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.APP_THEME_MODE
import org.futo.inputmethod.latin.uix.LocalNavController
import org.futo.inputmethod.latin.uix.TextEditPopupActivity
import org.futo.inputmethod.latin.uix.USE_SYSTEM_VOICE_INPUT
import org.futo.inputmethod.latin.uix.settings.NavigationItem
import org.futo.inputmethod.latin.uix.settings.NavigationItemStyle
import org.futo.inputmethod.latin.uix.settings.UserSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.render
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.settings.useDataStoreValue
import org.futo.inputmethod.latin.uix.settings.userSettingNavigationItem
import org.futo.inputmethod.latin.uix.theme.Typography
import org.futo.inputmethod.updates.ConditionalMigrateUpdateNotice
import org.futo.inputmethod.updates.openManualUpdateCheck

val HomeScreenLite = UserSettingsMenu(
    title = R.string.settings_home_title,
    navPath = "home", registerNavPath = false,
    settings = listOf(
        

        userSettingNavigationItem(
            title = R.string.language_settings_title,
            style = NavigationItemStyle.HomePrimary,
            navigateTo = "languages",
            icon = R.drawable.globe
        ),

        userSettingNavigationItem(
            title = R.string.settings_keyboard_typing_title,
            style = NavigationItemStyle.HomeSecondary,
            navigateTo = "keyboardAndTyping",
            icon = R.drawable.keyboard
        ),

        userSettingNavigationItem(
            title = SwipeMenu.title,
            style = NavigationItemStyle.HomePrimary,
            navigateTo = SwipeMenu.navPath,
            icon = R.drawable.swipe_icon
        ),

        userSettingNavigationItem(
            title = R.string.prediction_settings_title,
            style = NavigationItemStyle.HomeTertiary,
            navigateTo = PredictiveTextMenu.navPath,
            icon = R.drawable.text_prediction
        ),

        UserSetting(
            name = R.string.voice_input_settings_title
        ) {
            val navController = LocalNavController.current
            NavigationItem(
                title = stringResource(R.string.voice_input_settings_title),
                style = NavigationItemStyle.HomePrimary,
                subtitle = if(useDataStoreValue(USE_SYSTEM_VOICE_INPUT)) {
                    stringResource(R.string.voice_input_settings_builtin_disabled_notice)
                } else { null },
                navigate = { navController!!.navigate(VoiceInputMenu.navPath) },
                icon = painterResource(R.drawable.mic_fill)
            )
        },

        userSettingNavigationItem(
            title = R.string.action_settings_title,
            style = NavigationItemStyle.HomeSecondary,
            navigateTo = "actions",
            icon = R.drawable.smile
        ),

        userSettingNavigationItem(
            title = R.string.theme_settings_title,
            style = NavigationItemStyle.HomeTertiary,
            navigateTo = "themes",
            icon = R.drawable.themes
        ),

        userSettingNavigationItem(
            title = R.string.help_menu_title,
            style = NavigationItemStyle.HomeSecondary,
            navigateTo = "help",
            icon = R.drawable.help_circle
        ),

        //if(isDeveloper || LocalInspectionMode.current) {
        userSettingNavigationItem(
            title = R.string.dev_settings_title,
            style = NavigationItemStyle.HomeTertiary,
            navigateTo = "developer",
            icon = R.drawable.code
        ).copy(visibilityCheck = {
            useDataStoreValue(IS_DEVELOPER) == true || LocalInspectionMode.current
        }),
        //}

        userSettingNavigationItem(
            title = R.string.misc_settings_title,
            style = NavigationItemStyle.MiscNoArrow,
            navigateTo = "misc",
        ),
        
        userSettingNavigationItem(
            title = R.string.settings_check_for_updates_manually,
            style = NavigationItemStyle.Misc,
            navigate = { nav -> nav.context.openManualUpdateCheck() }
        ),

    )
)

/** Hero header: large title + version, sitting on a soft tonal gradient panel. */
@Composable
private fun HomeHero() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primaryContainer.copy(alpha = 0.55f),
                        scheme.tertiaryContainer.copy(alpha = 0.35f)
                    )
                )
            )
            .padding(horizontal = 22.dp, vertical = 26.dp)
    ) {
        Column {
            Text(
                stringResource(R.string.english_ime_settings),
                style = Typography.Heading.MediumMl,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = Typography.SmallMl,
                color = scheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/** Pill-shaped tap target that opens the search screen. */
@Composable
private fun SearchPill(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = scheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.settings_search_menu_title),
            style = Typography.Body.RegularMl,
            color = scheme.onSurfaceVariant
        )
    }
}

/** Segmented System / Light / Dark theme-mode toggle bound to APP_THEME_MODE. */
@Composable
private fun ThemeModeToggle() {
    val scheme = MaterialTheme.colorScheme
    val (mode, setMode) = useDataStore(APP_THEME_MODE)
    val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label) ->
            val selected = mode == key
            val bg by animateColorAsState(
                if (selected) scheme.primary else Color.Transparent,
                label = "themeModeBg"
            )
            val fg by animateColorAsState(
                if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                label = "themeModeFg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { setMode(key) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = Typography.SmallMl, color = fg)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreen(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column {
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HomeHero()
            SearchPill { navController.navigate("search") }
            ThemeModeToggle()

            ConditionalMigrateUpdateNotice()

            HomeScreenLite.render(showTitle = false)

            Spacer(modifier = Modifier.height(24.dp))
        }
        TextButton(onClick = {
            val intent = Intent()
            intent.setClass(context, TextEditPopupActivity::class.java)
            intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            context.startActivity(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_try_typing_here), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth())
        }
    }
}
