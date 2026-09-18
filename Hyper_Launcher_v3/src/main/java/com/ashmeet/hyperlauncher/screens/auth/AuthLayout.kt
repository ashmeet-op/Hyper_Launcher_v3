package com.ashmeet.hyperlauncher.screens.auth


import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.skin.SkinPreview
import com.ashmeet.hyperlauncher.skin.model.SkinModelType
import com.ashmeet.hyperlauncher.utils.SkinUtils
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthLayout(
    title: String,
    isFullScreen: Boolean = false,
    onBack: (() -> Unit)? = null,
    onFragmentViewCreated: (FrameLayout) -> Unit
) {
    val animations = listOf("NewIdle", "DefaultIdle", "Walking", "Running", "Flying", "Wave", "Crouch", "Hit")
    var currentAnimation by remember { mutableStateOf(LauncherPreferences.PREF_SKIN_ANIMATION) }
    val backEquipment by remember {
        derivedStateOf { if (currentAnimation == "Flying") "elytra" else "cape" }
    }
    var showAnimationDialog by remember { mutableStateOf(false) }

    var isLoadingSkin by remember { mutableStateOf(true) }
    var currentAccount by remember {
        mutableStateOf(try { Accounts.getCurrent() } catch (_: Exception) { null })
    }

    val context = LocalContext.current
    var skinModel by remember(currentAccount) {
        mutableStateOf(SkinUtils.getModelType(currentAccount))
    }
    var customSkinPath by remember(currentAccount) { mutableStateOf(currentAccount?.skinPath) }
    var customCapePath by remember(currentAccount) { mutableStateOf(currentAccount?.capePath) }

    val skinPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithExtension("image/png")
    ) { uri ->
        uri?.let {
            val skinFile = File(context.filesDir, "skins/skin_${currentAccount?.username}_${System.currentTimeMillis()}.png")
            skinFile.parentFile?.mkdirs()
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(skinFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = skinFile.absolutePath
                customSkinPath = path
                currentAccount?.let { acc ->
                    acc.skinPath = path
                    acc.save()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val capePickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithExtension("image/png")
    ) { uri ->
        uri?.let {
            val capeFile = File(context.filesDir, "capes/cape_${currentAccount?.username}_${System.currentTimeMillis()}.png")
            capeFile.parentFile?.mkdirs()
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(capeFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = capeFile.absolutePath
                customCapePath = path
                currentAccount?.let { acc ->
                    acc.capePath = path
                    acc.save()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        val accountListener = ExtraListener<Any> { _, _ ->
            currentAccount = try { Accounts.getCurrent() } catch (_: Exception) { null }
            false
        }
        ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, accountListener)
        onDispose {
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, accountListener)
        }
    }

    @Suppress("RemoveRedundantQualifierName")
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val animationSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.Dp>(500)
            val weightAnimationSpec = androidx.compose.animation.core.tween<Float>(500)

            val animatedPadding by animateDpAsState(
                targetValue = if (isFullScreen) 0.dp else 16.dp,
                animationSpec = animationSpec,
                label = "PaddingAnimation"
            )

            val sideWeight by animateFloatAsState(
                targetValue = if (isFullScreen) 0.001f else 1.0f,
                animationSpec = weightAnimationSpec,
                label = "SideWeightAnimation"
            )

            val mainWeight by animateFloatAsState(
                targetValue = if (isFullScreen) 1.0f else 1.2f,
                animationSpec = weightAnimationSpec,
                label = "MainWeightAnimation"
            )

            val spacerWidth by animateDpAsState(
                targetValue = if (isFullScreen) 0.dp else 16.dp,
                animationSpec = animationSpec,
                label = "SpacerAnimation"
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(animatedPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = !isFullScreen,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(sideWeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var fabMenuExpanded by remember { mutableStateOf(false) }
                        val modelAlpha by animateFloatAsState(if (fabMenuExpanded) 0.3f else 1.0f)

                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxWidth()
                                .graphicsLayer(alpha = modelAlpha),
                            contentAlignment = Alignment.Center
                        ) {
                            SkinPreview(
                                modifier = Modifier
                                    .fillMaxSize(),
                                skinUrl = customSkinPath?.let { "file://$it" } ?: SkinUtils.getSkinUrl(currentAccount),
                                model = skinModel,
                                animation = currentAnimation,
                                backEquipment = backEquipment,
                                capeUrl = customCapePath?.let { "file://$it" } ?: currentAccount?.capePath?.let { "file://$it" } ?: when (currentAccount?.authType) {
                                    AuthType.MICROSOFT -> "https://crafatar.com/capes/${currentAccount?.profileId}"
                                    AuthType.ELY_BY -> "http://skinsystem.ely.by/capes/${currentAccount?.username}.png"
                                    else -> null
                                },
                                onLoadingStateChanged = { isLoadingSkin = it }
                            )

                            if (isLoadingSkin) {
                                LoadingIndicator()
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.wrapContentSize(),
                            ) {
                                val options = listOf("slim", "default")
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = skinModel == option,
                                        onClick = {
                                            skinModel = option
                                            currentAccount?.let { acc ->
                                                acc.skinModel = if (option == "slim") SkinModelType.ALEX else SkinModelType.STEVE
                                                try {
                                                    acc.save()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                        label = { Text(if (option == "slim") translatedText("Slim") else translatedText("Wide")) },
                                        modifier = Modifier.height(40.dp)
                                    )
                                }
                            }

                            val fabMenuStartColor = MaterialTheme.colorScheme.surface
                            val fabMenuEndColor = MaterialTheme.colorScheme.secondary
                            val fabMenuIconStartColor = MaterialTheme.colorScheme.onSurface
                            val fabMenuIconEndColor = MaterialTheme.colorScheme.onSecondary

                            Box(
                                modifier = Modifier.size(40.dp)
                            ) {
                                FloatingActionButtonMenu(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .wrapContentSize(align = Alignment.BottomCenter, unbounded = true)
                                        .offset(y = 16.dp),
                                    expanded = fabMenuExpanded,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    button = {
                                        ToggleFloatingActionButton(
                                            checked = fabMenuExpanded,
                                            onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                                            modifier = Modifier.size(40.dp),
                                            containerSize = { 40.dp },
                                            containerCornerRadius = { 20.dp },
                                            contentAlignment = Alignment.Center,
                                            containerColor = { progress ->
                                                androidx.compose.ui.graphics.lerp(fabMenuStartColor, fabMenuEndColor, progress)
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val imageVector = if (fabMenuExpanded) Icons.Rounded.Close else Icons.Rounded.Add
                                                Icon(
                                                    imageVector = imageVector,
                                                    contentDescription = null,
                                                    modifier = Modifier.animateIcon(
                                                        checkedProgress = { checkedProgress },
                                                        color = { progress ->
                                                            androidx.compose.ui.graphics.lerp(fabMenuIconStartColor, fabMenuIconEndColor, progress)
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    FloatingActionButtonMenuItem(
                                        onClick = {
                                            fabMenuExpanded = false
                                            skinPickerLauncher.launch(null)
                                        },
                                        icon = { Icon(Icons.Rounded.AddReaction, contentDescription = null) },
                                        text = { Text(text = translatedText("Add Skin")) },
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    )
                                    FloatingActionButtonMenuItem(
                                        onClick = {
                                            fabMenuExpanded = false
                                            capePickerLauncher.launch(null)
                                        },
                                        icon = { Icon(Icons.Rounded.Sell, contentDescription = null) },
                                        text = { Text(text = translatedText("Add Cape")) },
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    )
                                    FloatingActionButtonMenuItem(
                                        onClick = {
                                            fabMenuExpanded = false
                                            showAnimationDialog = true
                                        },
                                        icon = { Icon(Icons.Rounded.Animation, contentDescription = null) },
                                        text = { Text(text = translatedText("Animations")) },
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(spacerWidth))

                Box(
                    modifier = Modifier
                        .weight(mainWeight)
                        .fillMaxHeight()
                ) {
                    AndroidView(
                        factory = { context ->
                            FrameLayout(context).apply {
                                id = R.id.container_fragment_auth
                                onFragmentViewCreated(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (showAnimationDialog) {
                com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog(
                    title = translatedText("Choose Animation"),
                    options = animations,
                    optionValues = animations,
                    selectedValue = currentAnimation,
                    onValueChange = {
                        currentAnimation = it
                        LauncherPreferences.prefs.edit { putString("skin_animation", it) }
                        LauncherPreferences.PREF_SKIN_ANIMATION = it
                        showAnimationDialog = false
                    },
                    onDismiss = { showAnimationDialog = false }
                )
            }

            if (onBack != null) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = translatedText("Back"),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 15.dp)
                        )
                    }
                }
            }
        }
    }
}
