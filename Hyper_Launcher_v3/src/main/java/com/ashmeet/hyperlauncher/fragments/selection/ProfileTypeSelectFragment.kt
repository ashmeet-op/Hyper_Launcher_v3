package com.ashmeet.hyperlauncher.fragments.selection

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.components.button.MineButton

import com.ashmeet.hyperlauncher.fragments.modloader.BTAInstallFragment
import com.ashmeet.hyperlauncher.fragments.installer.ContentInstallerFragment
import com.ashmeet.hyperlauncher.fragments.modloader.FabricInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.ForgeInstallFragment
import com.ashmeet.hyperlauncher.fragments.instances.InstanceEditorFragment
import com.ashmeet.hyperlauncher.fragments.modloader.LegacyFabricInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.NeoforgeInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.OptiFineInstallFragment
import com.ashmeet.hyperlauncher.fragments.modloader.QuiltInstallFragment
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.instances.Instances
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
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
                        ProfileTypeContent(
                            onVanillaClick = {
                                try {
                                    val instance = Instances.createDefaultInstance()
                                    Instances.setSelectedInstance(instance)
                                    Tools.swapFragment(
                                        requireActivity(),
                                        InstanceEditorFragment::class.java,
                                        InstanceEditorFragment.TAG,
                                        Bundle(1)
                                    )
                                } catch (e: IOException) {
                                    Tools.showError(context, e)
                                }
                            },
                            onOptiFineClick = {
                                Tools.swapFragment(
                                    requireActivity(),
                                    OptiFineInstallFragment::class.java,
                                    OptiFineInstallFragment.TAG,
                                    null
                                )
                            },
                            onFabricClick = {
                                Tools.swapFragment(
                                    requireActivity(),
                                    FabricInstallFragment::class.java,
                                    FabricInstallFragment.TAG,
                                    null
                                )
                            },
                            onForgeClick = {
                                Tools.swapFragment(
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
                                Tools.swapFragment(
                                    requireActivity(),
                                    ContentInstallerFragment::class.java,
                                    ContentInstallerFragment.TAG,
                                    args
                                )
                            },
                            onQuiltClick = {
                                Tools.swapFragment(
                                    requireActivity(),
                                    QuiltInstallFragment::class.java,
                                    QuiltInstallFragment.TAG,
                                    null
                                )
                            },
                            onBTAClick = {
                                Tools.swapFragment(
                                    requireActivity(),
                                    BTAInstallFragment::class.java,
                                    BTAInstallFragment.TAG,
                                    null
                                )
                            },
                            onNeoForgeClick = {
                                Tools.swapFragment(
                                    requireActivity(),
                                    NeoforgeInstallFragment::class.java,
                                    NeoforgeInstallFragment.TAG,
                                    null
                                )
                            },
                            onLegacyFabricClick = {
                                Tools.swapFragment(
                                    requireActivity(),
                                    LegacyFabricInstallFragment::class.java,
                                    LegacyFabricInstallFragment.TAG,
                                    null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

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
    onLegacyFabricClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = translatedText(stringResource(R.string.create_profile_vanilla_like_versions)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        MineButton(
            text = translatedText(stringResource(R.string.create_instance_vanilla)),
            onClick = onVanillaClick,
            modifier = Modifier.fillMaxWidth()
        )

        MineButton(
            text = translatedText(stringResource(R.string.mod_dl_install_optifine)),
            onClick = onOptiFineClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = translatedText(stringResource(R.string.create_profile_modded_versions)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        MineButton(
            text = translatedText(stringResource(R.string.modloader_dl_install_fabric_instance)),
            onClick = onFabricClick,
            modifier = Modifier.fillMaxWidth()
        )

        MineButton(
            text = translatedText(stringResource(R.string.modloader_dl_install_quilt_instance)),
            onClick = onQuiltClick,
            modifier = Modifier.fillMaxWidth()
        )

        MineButton(
            text = translatedText(stringResource(R.string.modloader_dl_install_legacy_fabric_instance)),
            onClick = onLegacyFabricClick,
            modifier = Modifier.fillMaxWidth()
        )

        MineButton(
            text = translatedText(stringResource(R.string.modloader_dl_install_forge_instance)),
            onClick = onForgeClick,
            modifier = Modifier.fillMaxWidth()
        )

        MineButton(
            text = translatedText(stringResource(R.string.modloader_dl_install_neoforge_instance)),
            onClick = onNeoForgeClick,
            modifier = Modifier.fillMaxWidth()
        )

        MineButton(
            text = translatedText(stringResource(R.string.modpack_install_instance_button)),
            onClick = onModpackClick,
            modifier = Modifier.fillMaxWidth()
        )

        MineButton(
            text = translatedText(stringResource(R.string.create_bta_instance)),
            onClick = onBTAClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
