package com.ashmeet.hyperlauncher.fragments.selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.fragments.modloader.BTAInstallFragment
import com.ashmeet.hyperlauncher.fragments.installer.ContentInstallerFragment
import com.ashmeet.hyperlauncher.fragments.modloader.FabricInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.ForgeInstallFragment
import com.ashmeet.hyperlauncher.fragments.instances.InstanceEditorFragment
import com.ashmeet.hyperlauncher.fragments.modloader.LegacyFabricInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.NeoforgeInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.OptiFineInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.QuiltInstallFragment
import com.ashmeet.hyperlauncher.components.list.ProfileTypeListItem
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.horizon.HorizonUtils
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperbackend.instances.DisplayInstance
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.IOException

class ProfileTypeSelectFragment : Fragment() {
    companion object {
        const val TAG = "ProfileTypeSelectFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    ProfileTypeContent(
                        onVanillaClick = {
                            try {
                                val instance = Instances.createDefaultInstance()
                                Instances.setSelectedInstance(instance)
                                HyperUtils.swapFragment(
                                    requireActivity(),
                                    InstanceEditorFragment::class.java,
                                    InstanceEditorFragment.TAG,
                                    Bundle(1)
                                )
                            } catch (e: IOException) {
                                HyperUtils.showError(context, e)
                            }
                        },
                        onOptiFineClick = {
                            HyperUtils.swapFragment(
                                requireActivity(),
                                OptiFineInstallFragment::class.java,
                                OptiFineInstallFragment.TAG,
                                null
                            )
                        },
                        onFabricClick = {
                            HyperUtils.swapFragment(
                                requireActivity(),
                                FabricInstallFragment::class.java,
                                FabricInstallFragment.TAG,
                                null
                            )
                        },
                        onForgeClick = {
                            HyperUtils.swapFragment(
                                requireActivity(),
                                ForgeInstallFragment::class.java,
                                ForgeInstallFragment.TAG,
                                null
                            )
                        },
                        onModpackClick = {
                            val args = Bundle().apply {
                                putString("type", "MODPACKS")
                                putBoolean("bypass", true)
                            }
                            HyperUtils.swapFragment(
                                requireActivity(),
                                ContentInstallerFragment::class.java,
                                ContentInstallerFragment.TAG,
                                args
                            )
                        },
                        onQuiltClick = {
                            HyperUtils.swapFragment(
                                requireActivity(),
                                QuiltInstallFragment::class.java,
                                QuiltInstallFragment.TAG,
                                null
                            )
                        },
                        onBTAClick = {
                            HyperUtils.swapFragment(
                                requireActivity(),
                                BTAInstallFragment::class.java,
                                BTAInstallFragment.TAG,
                                null
                            )
                        },
                        onNeoForgeClick = {
                            HyperUtils.swapFragment(
                                requireActivity(),
                                NeoforgeInstallFragment::class.java,
                                NeoforgeInstallFragment.TAG,
                                null
                            )
                        },
                        onLegacyFabricClick = {
                            HyperUtils.swapFragment(
                                requireActivity(),
                                LegacyFabricInstallFragment::class.java,
                                LegacyFabricInstallFragment.TAG,
                                null
                            )
                        },
                        onHorizonClick = {
                            try {
                                val instance = Instances.createInstance({ inst ->
                                    inst.name = "Horizon 26.3 (Rust)"
                                    inst.versionId = HorizonUtils.HORIZON_VERSION_ID
                                    inst.selectedRuntime = HorizonUtils.HORIZON_RUNTIME
                                    inst.icon = "horizon"
                                    inst.sharedData = true
                                }, "Horizon-26.3")
                                Instances.setSelectedInstance(instance)
                                HyperUtils.swapFragment(
                                    requireActivity(),
                                    InstanceEditorFragment::class.java,
                                    InstanceEditorFragment.TAG,
                                    Bundle(1)
                                )
                            } catch (e: IOException) {
                                HyperUtils.showError(context, e)
                            }
                        }
                    )
                }
            }
        }
    }
}

data class ProfileOption(
    val name: String,
    val versionId: String,
    val icon: String,
    val enabled: Boolean = true,
    val warningMessage: String? = null,
    val onClick: () -> Unit
)

@Composable
fun ProfileTypeContent(
    onVanillaClick: () -> Unit,
    onOptiFineClick: () -> Unit,
    onFabricClick: () -> Unit,
    onForgeClick: () -> Unit,
    onModpackClick: () -> Unit,
    onQuiltClick: () -> Unit,
    onBTAClick: () -> Unit,
    onNeoForgeClick: () -> Unit,
    onLegacyFabricClick: () -> Unit,
    onHorizonClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        translatedText("Vanilla"),
        translatedText("Modded"),
        translatedText("Misc")
    )

    val isArm64 = remember { Architecture.getDeviceArchitecture() == Architecture.ARCH_ARM64 }
    val gson = remember { Gson() }

    val vanillaOptions = listOf(
        ProfileOption(
            name = translatedText(stringResource(R.string.create_instance_vanilla)),
            versionId = "Vanilla",
            icon = "vanilla",
            onClick = onVanillaClick
        ),
        ProfileOption(
            name = translatedText(stringResource(R.string.mod_dl_install_optifine)),
            versionId = "OptiFine",
            icon = "optifine",
            onClick = onOptiFineClick
        )
    )

    val moddedOptions = listOf(
        ProfileOption(
            name = translatedText(stringResource(R.string.modloader_dl_install_fabric_instance)),
            versionId = "Fabric",
            icon = "fabric",
            onClick = onFabricClick
        ),
        ProfileOption(
            name = translatedText(stringResource(R.string.modloader_dl_install_quilt_instance)),
            versionId = "Quilt",
            icon = "quilt",
            onClick = onQuiltClick
        ),
        ProfileOption(
            name = translatedText(stringResource(R.string.modloader_dl_install_legacy_fabric_instance)),
            versionId = "Legacy Fabric",
            icon = "legacyfabric",
            onClick = onLegacyFabricClick
        ),
        ProfileOption(
            name = translatedText(stringResource(R.string.modloader_dl_install_forge_instance)),
            versionId = "Forge",
            icon = "forge",
            onClick = onForgeClick
        ),
        ProfileOption(
            name = translatedText(stringResource(R.string.modloader_dl_install_neoforge_instance)),
            versionId = "NeoForge",
            icon = "neoforge",
            onClick = onNeoForgeClick
        ),
        ProfileOption(
            name = translatedText(stringResource(R.string.modpack_install_instance_button)),
            versionId = "Modpack",
            icon = "modrinth",
            onClick = onModpackClick
        ),
        ProfileOption(
            name = translatedText(stringResource(R.string.create_bta_instance)),
            versionId = "Better Than Adventure",
            icon = "curseforge",
            onClick = onBTAClick
        )
    )

    val miscOptions = listOf(
        ProfileOption(
            name = translatedText(stringResource(R.string.create_instance_horizon_rust)),
            versionId = if (isArm64) "Minecraft 26.3 only" else "Not available: requires ARM64 device",
            icon = "horizon",
            enabled = isArm64,
            warningMessage = "Horizon Rust engine is only available on ARM64 devices.",
            onClick = onHorizonClick
        )
    )

    val currentOptions = when (selectedTab) {
        0 -> vanillaOptions
        1 -> moddedOptions
        else -> miscOptions
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                containerColor = Color.Transparent,
                divider = {},
                indicator = @Composable {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(selectedTab)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        height = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(currentOptions) { option ->
                    val displayInstance = remember(option) {
                        val jsonObj = JsonObject().apply {
                            addProperty("name", option.name)
                            addProperty("versionId", option.versionId)
                            addProperty("icon", option.icon)
                        }
                        gson.fromJson(jsonObj, DisplayInstance::class.java).apply {
                            mInstanceRoot = File(context.cacheDir, option.name)
                        }
                    }

                    ProfileTypeListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (option.enabled) 1f else 0.5f),
                        instance = displayInstance,
                        onClick = {
                            if (option.enabled) {
                                option.onClick()
                            } else {
                                Toast.makeText(context, option.warningMessage ?: "Not available on this device architecture.", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }
    }
}
