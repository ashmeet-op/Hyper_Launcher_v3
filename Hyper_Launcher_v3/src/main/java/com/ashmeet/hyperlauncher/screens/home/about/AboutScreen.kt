package com.ashmeet.hyperlauncher.screens.home.about

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.widget.Toast
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.PreferenceCategory
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.utils.drawable.rememberDrawablePainter
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.BuildConfig
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Architecture
import net.kdt.pojavlaunch.Logger
import com.ashmeet.hyperlauncher.utils.Tools

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Logger.appendToLog("Info: Launcher version: " + BuildConfig.VERSION_NAME)
        Logger.appendToLog("Info: Build type: " + BuildConfig.BUILD_TYPE)
        Logger.appendToLog("Info: Architecture: " + Architecture.archAsString(Architecture.getDeviceArchitecture()))
        Logger.appendToLog("Info: Device model: " + Build.MANUFACTURER + " " + Build.MODEL)
        Logger.appendToLog("Info: API version: " + Build.VERSION.SDK_INT)
    }

    SettingsScreenWrapper(
        title = translatedText("About"),
        onBack = onBack,
        addTopGap = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                val appIcon = remember(context) { ContextCompat.getDrawable(context, R.drawable.ic_hyper_full) }
                Image(
                    painter = rememberDrawablePainter(appIcon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )

                Text(
                    text = "Hyper Launcher",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PreferenceCategory(title = translatedText("Application"))
                SettingsCard(position = CardPosition.TOP, useSurface = true) {
                    SettingsActionItem(title = translatedText("App Name"), summary = "Hyper Launcher 3", onClick = {})
                }

                val versionParts = remember { BuildConfig.VERSION_NAME.split("-") }
                val displayVersion = versionParts.getOrNull(0) ?: BuildConfig.VERSION_NAME
                val displayBuild = versionParts.getOrNull(1)?.let { "${it}_build" }

                var developerClickCount by remember { mutableIntStateOf(0) }
                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("Version"),
                        summary = displayVersion,
                        onClick = {
                            if (!LauncherPreferences.PREF_DEVELOPER_OPTIONS) {
                                developerClickCount++
                                if (developerClickCount >= 10) {
                                    LauncherPreferences.PREF_DEVELOPER_OPTIONS = true
                                    LauncherPreferences.prefs.edit { putBoolean("developer_options", true) }
                                    Toast.makeText(context, "Developer options enabled!", Toast.LENGTH_SHORT).show()
                                } else if (developerClickCount > 5) {
                                    Toast.makeText(context, "You are now ${10 - developerClickCount} steps away from being a developer.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                if (displayBuild != null) {
                    SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                        SettingsActionItem(
                            title = translatedText("Build"),
                            summary = displayBuild,
                            onClick = {}
                        )
                    }
                }

                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsActionItem(title = translatedText("Version Code"), summary = BuildConfig.VERSION_CODE.toString(), onClick = {})
                }

                val packageName = BuildConfig.APPLICATION_ID
                val isDebugPackage = packageName.contains("debug", ignoreCase = true)
                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("Package Name"),
                        summary = packageName,
                        warningTooltip = if (isDebugPackage) "Warning: This is a debug package. Performance and stability may be affected." else null,
                        onClick = {}
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PreferenceCategory(title = translatedText("Information"))
                SettingsCard(position = CardPosition.TOP, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("About this launcher"),
                        summary = translatedText("Learn more about Hyper Launcher"),
                        icon = Icons.Rounded.Description,
                        onClick = { (context as? Activity)?.let { Tools.openURL(it, "https://github.com/hollowlauncher/Hyper_Launcher_v3/blob/master/README.md") } }
                    )
                }
                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("Website"),
                        summary = translatedText("Visit our official website"),
                        icon = Icons.Rounded.Public,
                        onClick = { (context as? Activity)?.let { Tools.openURL(it, Tools.URL_HOME) } }
                    )
                }
                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("Discord"),
                        summary = translatedText("Join our community on Discord"),
                        iconPainter = painterResource(id = R.drawable.ic_discord),
                        onClick = { (context as? Activity)?.let { Tools.openURL(it, context.getString(R.string.social_media_invite)) } }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PreferenceCategory(title = translatedText("Device Info"))
                SettingsCard(position = CardPosition.TOP, useSurface = true) {
                    SettingsActionItem(title = translatedText("Architecture"), summary = Architecture.archAsString(Architecture.getDeviceArchitecture()), onClick = {})
                }
                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsActionItem(title = translatedText("Device Model"), summary = "${Build.MANUFACTURER} ${Build.MODEL}", onClick = {})
                }
                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsActionItem(title = translatedText("Android Version"), summary = "API ${Build.VERSION.SDK_INT}", onClick = {})
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
