package org.futo.inputmethod.latin.uix.settings.pages

import org.futo.inputmethod.latin.uix.settings.*

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.futo.inputmethod.latin.uix.LocalNavController
import org.futo.inputmethod.latin.uix.TextEditPopupActivity
import org.futo.inputmethod.latin.uix.USE_SYSTEM_VOICE_INPUT
import org.futo.inputmethod.latin.uix.settings.NavigationItem
import org.futo.inputmethod.latin.uix.settings.NavigationItemStyle
import org.futo.inputmethod.latin.uix.settings.UserSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.render
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

@Preview(showBackground = true)
@Composable
fun HomeScreen(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize().background(SettingsTheme.colors.background)) {
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                color = SettingsTheme.colors.surface,
                contentColor = SettingsTheme.colors.onSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SettingsTheme.colors.outlineVariant)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .width(6.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SettingsTheme.colors.primary)
                    )
                    Column(Modifier.weight(1.0f)) {
                        Text(
                            stringResource(R.string.english_ime_settings),
                            style = Typography.Heading.Medium,
                            color = SettingsTheme.colors.onSurface
                        )
                        Text(
                            "v${BuildConfig.VERSION_NAME}",
                            style = Typography.Small,
                            color = SettingsTheme.colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        navController.navigate("search")
                    }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(
                            R.string.settings_search_menu_title
                        ), tint = SettingsTheme.colors.primary)
                    }
                }
            }

            ConditionalMigrateUpdateNotice()

            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                color = SettingsTheme.colors.surface,
                contentColor = SettingsTheme.colors.onSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SettingsTheme.colors.outlineVariant)
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    HomeScreenLite.render(showTitle = false)
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(32.dp))
        }
        TextButton(onClick = {
            val intent = Intent()
            intent.setClass(context, TextEditPopupActivity::class.java)
            intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            context.startActivity(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.settings_try_typing_here),
                color = SettingsTheme.colors.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
