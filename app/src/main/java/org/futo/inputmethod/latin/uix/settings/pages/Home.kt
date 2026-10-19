package org.futo.inputmethod.latin.uix.settings.pages

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
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
import org.futo.inputmethod.latin.uix.SYSTEM_VOICE_INPUT_PACKAGE
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

// ALTER Keyboard: the settings home is grouped by what people come to do (UI/UX review: progressive disclosure,
// visual hierarchy, clear sections). Voice first, with its live setup status; then typing, look & feel, and more.
// FUTO's "check for updates" item is not shown: ALTER builds come from the repo's Releases page.
val HomeVoiceSettings = listOf(
    UserSetting(
        name = R.string.alter_voice_settings_title,
        subtitle = R.string.alter_voice_settings_subtitle
    ) {
        val navController = LocalNavController.current
        val status = rememberAlterVoiceStatus()
        NavigationItem(
            title = stringResource(R.string.alter_voice_settings_title),
            style = NavigationItemStyle.HomePrimary,
            subtitle = status.summary(),
            navigate = { navController!!.navigate(org.futo.inputmethod.latin.uix.actions.AlterVoiceNavPath) },
            icon = painterResource(R.drawable.mic_fill)
        )
    },
)

val HomeTypingSettings = listOf(
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
)

val HomeLookSettings = listOf(
        userSettingNavigationItem(
            title = R.string.theme_settings_title,
            style = NavigationItemStyle.HomeTertiary,
            navigateTo = "themes",
            icon = R.drawable.themes
        ),
        userSettingNavigationItem(
            title = R.string.action_settings_title,
            style = NavigationItemStyle.HomeSecondary,
            navigateTo = "actions",
            icon = R.drawable.smile
        ),
)

// The payment page stays visible (FUTO Source First License); it is grouped with the other secondary pages.
val HomeMoreSettings = listOf(
        userSettingNavigationItem(
            title = R.string.help_menu_title,
            style = NavigationItemStyle.HomeSecondary,
            navigateTo = "help",
            icon = R.drawable.help_circle
        ),
        userSettingNavigationItem(
            title = R.string.payment_screen_short_title,
            style = NavigationItemStyle.HomePrimary,
            navigateTo = "payment",
            icon = R.drawable.dollar_sign,
        ).copy(visibilityCheck = {
            useDataStoreValue(IS_ALREADY_PAID) == false
        }, appearInSearchIfVisibilityCheckFailed = false),
        userSettingNavigationItem(
            title = R.string.misc_settings_title,
            style = NavigationItemStyle.Misc,
            navigateTo = "misc",
        ),
        userSettingNavigationItem(
            title = R.string.credits_menu_title,
            style = NavigationItemStyle.Misc,
            navigateTo = "credits",
        ),
        userSettingNavigationItem(
            title = R.string.dev_settings_title,
            style = NavigationItemStyle.HomeTertiary,
            navigateTo = "developer",
            icon = R.drawable.code
        ).copy(visibilityCheck = {
            useDataStoreValue(IS_DEVELOPER) == true || LocalInspectionMode.current
        }),
)

val HomeScreenLite = UserSettingsMenu(
    title = R.string.settings_home_title,
    navPath = "home", registerNavPath = false,
    settings = HomeVoiceSettings + HomeTypingSettings + HomeLookSettings + HomeMoreSettings
)

@Composable
private fun HomeGroup(@androidx.annotation.StringRes title: Int, settings: List<UserSetting>) {
    org.futo.inputmethod.latin.uix.settings.AlterSettingsGroup(stringResource(title)) {
        settings.forEach { if (it.visibilityCheck?.invoke() != false) it.component() }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreen(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDeveloper = useDataStoreValue(IS_DEVELOPER)
    val isPaid = useDataStoreValue(IS_ALREADY_PAID)

    Column {
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.english_ime_settings), style = org.futo.inputmethod.latin.uix.settings.AlterType.PageHeading, modifier = Modifier
                    .align(CenterVertically)
                    .weight(1.0f))

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = {
                    navController.navigate("search")
                }) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(
                        R.string.settings_search_menu_title
                    ))
                }
            }

            // ALTER Keyboard: FUTO's manual-update notice (F-Droid, keyboard.futo.tech) is not shown; ALTER builds come
            // from this repo's Releases page.
            AlterVoiceSetupCard(navController)
            ConditionalUnpaidNoticeWithNav(navController)

            HomeGroup(R.string.alter_group_voice, HomeVoiceSettings)
            HomeGroup(R.string.alter_group_typing, HomeTypingSettings)
            HomeGroup(R.string.alter_group_look, HomeLookSettings)
            HomeGroup(R.string.alter_group_more, HomeMoreSettings)


            Spacer(modifier = Modifier.height(16.dp))

            if(isPaid || LocalInspectionMode.current) {
                Text(
                    stringResource(R.string.payment_paid_version_indicator),
                    style = Typography.SmallMl,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = Typography.Small,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
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
            Text(stringResource(R.string.settings_try_typing_here), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth())
        }
    }
}