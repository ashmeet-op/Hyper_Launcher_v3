package com.ashmeet.hyperlauncher.screens.auth

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.skin.SkinPreview
import com.ashmeet.hyperlauncher.utils.SkinUtils
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon

import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ToggleFloatingActionButton

import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Texture



import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.platform.LocalContext
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import java.io.File
import java.io.FileOutputStream
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener

@Composable
fun AuthLayout(
    title: String,
    onBack: (() -> Unit)? = null,
    onFragmentViewCreated: (FrameLayout) -> Unit
) {
    var currentAccount by remember {
        mutableStateOf(try { Accounts.getCurrent() } catch (_: Exception) { null })
    }

    val context = LocalContext.current
    var skinModel by remember(currentAccount) {
        mutableStateOf(SkinUtils.getModelType(currentAccount))
    }
    var customSkinPath by remember { mutableStateOf<String?>(null) }
    var customCapePath by remember { mutableStateOf<String?>(null) }

    val skinPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithExtension("image/png")
    ) { uri ->
        uri?.let {
            val skinFile = File(context.cacheDir, "skin_import_temp_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(skinFile).use { output ->
                        input.copyTo(output)
                    }
                }
                customSkinPath = skinFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val capePickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithExtension("image/png")
    ) { uri ->
        uri?.let {
            val capeFile = File(context.cacheDir, "cape_import_temp_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(capeFile).use { output ->
                        input.copyTo(output)
                    }
                }
                customCapePath = capeFile.absolutePath
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SkinPreview(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        skinUrl = customSkinPath?.let { "file://$it" } ?: SkinUtils.getSkinUrl(currentAccount),
                        model = skinModel,
                        capeUrl = customCapePath?.let { "file://$it" } ?: when (currentAccount?.authType) {
                            AuthType.MICROSOFT -> "https://crafatar.com/capes/${currentAccount?.profileId}"
                            AuthType.ELY_BY -> "http://skinsystem.ely.by/capes/${currentAccount?.username}.png"
                            AuthType.LOCAL -> currentAccount?.capePath?.let { "file://$it" }
                            else -> null
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                                    onClick = { skinModel = option },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    label = { Text(if (option == "slim") translatedText("Slim") else translatedText("Wide")) },
                                    modifier = Modifier.height(40.dp)
                                )
                            }
                        }

                        var fabMenuExpanded by remember { mutableStateOf(false) }
                        val fabMenuStartColor = MaterialTheme.colorScheme.secondary
                        val fabMenuEndColor = MaterialTheme.colorScheme.surface
                        val fabMenuIconStartColor = MaterialTheme.colorScheme.onSecondary
                        val fabMenuIconEndColor = MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier.size(40.dp)
                        ) {
                            FloatingActionButtonMenu(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .wrapContentSize(align = Alignment.BottomCenter, unbounded = true)
                                    .offset(y = 16.dp), // Compensate for internal FabMenuButtonPaddingBottom
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
                                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                    text = { Text(text = translatedText("Add Skin")) }
                                )
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        fabMenuExpanded = false
                                        capePickerLauncher.launch(null)
                                    },
                                    icon = { Icon(Icons.Rounded.Texture, contentDescription = null) },
                                    text = { Text(text = translatedText("Add Cape")) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1.2f)
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
