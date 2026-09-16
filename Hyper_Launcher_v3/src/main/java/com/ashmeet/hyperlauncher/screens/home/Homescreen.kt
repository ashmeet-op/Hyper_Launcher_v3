package com.ashmeet.hyperlauncher.screens.home

import android.content.SharedPreferences
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashmeet.hyperlauncher.components.button.MineButton
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.SkinUtils
import com.ashmeet.hyperlauncher.utils.drawable.rememberDrawablePainter
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.instances.DisplayInstance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.instances.Instances

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainMenuFragmentCompose(
    onAboutClick: () -> Unit,
    onSocialMediaClick: () -> Unit,
    onCustomControlsClick: () -> Unit,
    onInstallJarClick: () -> Unit,
    onShareLogsClick: () -> Unit,
    onOpenFilesClick: () -> Unit,
    onEditProfileClick: (DisplayInstance?) -> Unit,
    onPlayClick: () -> Unit,
    onVersionSpinnerClick: () -> Unit,
    onAccountManagerClick: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var launcherBgPath by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH) }
    val hasBackground = launcherBgPath != null
    val backgroundTransparency = if (hasBackground) 0.5f else 1f
    var hideActionButtons by remember { mutableStateOf(LauncherPreferences.PREF_HIDE_SIDEBAR) }

    var selectedInstance by remember {
        mutableStateOf(
            if (isPreview) null
            else try { Instances.loadSelectedInstance() } catch (_: Exception) { null }
        )
    }

    if (!isPreview) {
        DisposableEffect(Unit) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "hide_sidebar") {
                    hideActionButtons = LauncherPreferences.prefs.getBoolean("hide_sidebar", false)
                }
                if (key == "launcher_background_path") {
                    launcherBgPath = LauncherPreferences.prefs.getString("launcher_background_path", null)
                }
                if (key == LauncherPreferences.PREF_KEY_CURRENT_INSTANCE) {
                    selectedInstance = try { Instances.loadSelectedInstance() } catch (_: Exception) { null }
                }
            }
            LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                LauncherPreferences.prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    SideEffect {
        if (!isPreview) {
            val instance = try { Instances.loadSelectedInstance() } catch (_: Exception) { null }
            if (selectedInstance != instance) {
                selectedInstance = instance
            }
        }
    }

    var currentAccount by remember {
        mutableStateOf(if (isPreview) null else Accounts.getCurrent())
    }

    DisposableEffect(Unit) {
        if (isPreview) return@DisposableEffect onDispose {}

        val accountListener = ExtraListener<Any> { _, _ ->
            currentAccount = Accounts.getCurrent()
            false
        }

        ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, accountListener)

        onDispose {
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, accountListener)
        }
    }

    val skinHead by SkinUtils.rememberSkinHead2D(currentAccount)

    val instanceIcon = remember(selectedInstance) {
        if (!isPreview && selectedInstance != null)
            InstanceIconProvider.fetchIcon(context.resources, selectedInstance!!)
        else null
    }

    val headInteractionSource = remember { MutableInteractionSource() }
    val isHeadPressed by headInteractionSource.collectIsPressedAsState()
    val headScale by animateFloatAsState(
        targetValue = if (isHeadPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                val rightSidebarWidth = 260.dp
                if (!hideActionButtons) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .padding(end = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            title = translatedText("About"),
                            icon = Icons.Rounded.Info,
                            position = CardPosition.TOP,
                            onClick = onAboutClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            title = translatedText(stringResource(id = R.string.mcl_button_social_media)),
                            icon = Icons.Rounded.Share,
                            position = CardPosition.MIDDLE,
                            onClick = onSocialMediaClick
                        )

                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            title = translatedText(stringResource(id = R.string.mcl_option_customcontrol)),
                            icon = Icons.Rounded.Build,
                            position = CardPosition.MIDDLE,
                            onClick = onCustomControlsClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            title = translatedText(stringResource(id = R.string.main_install_jar_file)),
                            icon = Icons.Rounded.Add,
                            position = CardPosition.MIDDLE,
                            onClick = onInstallJarClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            title = translatedText(stringResource(id = R.string.main_share_logs)),
                            icon = Icons.AutoMirrored.Rounded.Send,
                            position = CardPosition.MIDDLE,
                            onClick = onShareLogsClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            title = translatedText(stringResource(id = R.string.mcl_button_open_directory)),
                            icon = Icons.Rounded.Search,
                            position = CardPosition.BOTTOM,
                            onClick = onOpenFilesClick
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Surface(
                    modifier = Modifier
                        .width(rightSidebarWidth)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(32.dp),
                    color = if (hasBackground) MaterialTheme.colorScheme.surface.copy(alpha = backgroundTransparency)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    tonalElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .scale(headScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = headInteractionSource,
                                        indication = null,
                                        onClick = onAccountManagerClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentAccount != null) {
                                    if (skinHead != null) {
                                        Image(
                                            bitmap = skinHead!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Fit,
                                            filterQuality = FilterQuality.None
                                        )
                                    } else {
                                        LoadingIndicator(
                                            modifier = Modifier.size(42.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = translatedText("Add Account"),
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onVersionSpinnerClick)
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (instanceIcon != null) {
                                            Image(
                                                painter = rememberDrawablePainter(instanceIcon),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        val name = selectedInstance?.name
                                        val instanceDisplayName = if (selectedInstance == null) {
                                            translatedText(stringResource(id = R.string.no_instance))
                                        } else if (name.isNullOrBlank()) {
                                            translatedText("UNNAMED")
                                        } else {
                                            name
                                        }

                                        Text(
                                            text = instanceDisplayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = selectedInstance?.versionId ?: translatedText(stringResource(id = R.string.version_select_hint)),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onEditProfileClick(selectedInstance) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = translatedText("Edit Profile"),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            MineButton(
                                text = translatedText("Launch"),
                                onClick = onPlayClick,
                                modifier = Modifier
                                    .weight(1f),
                                height = 56.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    position: CardPosition = CardPosition.SINGLE,
    onClick: () -> Unit
) {
    SettingsCard(
        modifier = modifier,
        position = position,
        useSurface = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=800dp,height=400dp,dpi=420",
)
@Composable
fun MainMenuRevampPreview() {
    PojavTheme {
        MainMenuFragmentCompose(
            onAboutClick = {},
            onSocialMediaClick = {},
            onCustomControlsClick = {},
            onInstallJarClick = {},
            onShareLogsClick = {},
            onOpenFilesClick = {},
            onEditProfileClick = {},
            onPlayClick = {},
            onVersionSpinnerClick = {},
            onAccountManagerClick = {}
        )
    }
}
