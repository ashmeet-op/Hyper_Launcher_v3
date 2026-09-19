package com.ashmeet.hyperlauncher.fragments.instances

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.components.dialog.SimpleAlertDialog
import com.ashmeet.hyperlauncher.components.dialog.DialogTextInput
import com.ashmeet.hyperlauncher.components.dialog.GenericComposeDialogFragment
import com.ashmeet.hyperlauncher.fragments.installer.ContentInstallerFragment
import com.ashmeet.hyperlauncher.fragments.selection.ProfileTypeSelectFragment
import com.ashmeet.hyperlauncher.screens.instances.InstanceSelectionScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.ShortcutUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances

class InstanceSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    InstanceSelectionScreen(
                        onBack = { parentFragmentManager.popBackStack() },
                        onCreateNew = {
                            Tools.swapFragment(
                                requireActivity(),
                                ProfileTypeSelectFragment::class.java,
                                ProfileTypeSelectFragment.TAG,
                                null
                            )
                        },
                        onImportModpack = {
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
                        onEditInstance = { instance ->
                            PojavApplication.sExecutorService.execute {
                                try {
                                    val loadedAll = Instances.loadAllInstances()
                                    val fullInstance = loadedAll.find { it.mInstanceRoot == instance.mInstanceRoot }
                                    if (fullInstance != null) {
                                        Tools.runOnUiThread {
                                            Instances.setSelectedInstance(fullInstance)
                                            Tools.swapFragment(
                                                requireActivity(),
                                                InstanceEditorFragment::class.java,
                                                InstanceEditorFragment.TAG,
                                                null
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Tools.runOnUiThread { Tools.showError(requireContext(), e) }
                                }
                            }
                        },
                        onRenameInstance = { instance, onRefresh ->
                            PojavApplication.sExecutorService.execute {
                                try {
                                    val loadedAll = Instances.loadAllInstances()
                                    val fullInstance = loadedAll.find { it.mInstanceRoot == instance.mInstanceRoot }
                                    if (fullInstance != null) {
                                        Tools.runOnUiThread {
                                            showRenameDialog(fullInstance, onRefresh)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Tools.runOnUiThread { Tools.showError(requireContext(), e) }
                                }
                            }
                        },
                        onDeleteInstance = { instance, onRefresh ->
                            PojavApplication.sExecutorService.execute {
                                try {
                                    val loadedAll = Instances.loadAllInstances()
                                    val fullInstance = loadedAll.find { it.mInstanceRoot == instance.mInstanceRoot }
                                    if (fullInstance != null) {
                                        Tools.runOnUiThread {
                                            showDeleteConfirmDialog(fullInstance, onRefresh)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Tools.runOnUiThread { Tools.showError(requireContext(), e) }
                                }
                            }
                        },
                        onAddShortcut = { instance ->
                            ShortcutUtils.createShortcut(requireContext(), instance)
                        }
                    )
                }
            }
        }
    }

    private fun showRenameDialog(instance: Instance, onRefresh: () -> Unit) {
        val dialogFragment = GenericComposeDialogFragment {
            DialogTextInput(
                title = getString(R.string.global_name),
                initialValue = instance.name,
                onConfirm = { newName ->
                    if (newName.isNotBlank()) {
                        instance.name = newName
                        instance.maybeWrite()
                        Toast.makeText(requireContext(), R.string.global_save, Toast.LENGTH_SHORT).show()
                        onRefresh()
                    }
                    dismiss()
                },
                onDismiss = { dismiss() }
            )
        }
        dialogFragment.show(parentFragmentManager, "rename_dialog")
    }

    private fun showDeleteConfirmDialog(instance: Instance, onRefresh: () -> Unit) {
        val dialogFragment = GenericComposeDialogFragment {
            SimpleAlertDialog(
                title = getString(R.string.instance_delete),
                text = getString(R.string.instance_delete_confirmation),
                confirmText = getString(R.string.global_delete),
                dismissText = getString(android.R.string.cancel),
                isDestructive = true,
                onConfirm = {
                    Instances.removeInstance(instance)
                    Toast.makeText(requireContext(), R.string.global_delete, Toast.LENGTH_SHORT).show()
                    onRefresh()
                    dismiss()
                },
                onDismiss = { dismiss() }
            )
        }
        dialogFragment.show(parentFragmentManager, "delete_dialog")
    }

    companion object {
        const val TAG = "InstanceSelectionFragment"
    }
}
