package com.ashmeet.hyperlauncher.fragments.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import com.ashmeet.hyperlauncher.components.dialog.SimpleAlertDialog
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.instances.Instances
import java.io.IOException

class DeleteConfirmDialogFragment : DialogFragment() {
    private val mInstance = Instances.loadSelectedInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    if (mInstance == null) {
                        dismiss()
                    } else {
                        SimpleAlertDialog(
                            title = translatedText(stringResource(R.string.instance_delete)),
                            text = translatedText(stringResource(R.string.instance_delete_confirmation)),
                            confirmText = translatedText(stringResource(R.string.global_delete)),
                            dismissText = translatedText(stringResource(R.string.global_no)),
                            onConfirm = {
                                InstanceIconProvider.dropIcon(mInstance)
                                Tools.removeCurrentFragment(requireActivity())
                                try {
                                    Instances.removeInstance(mInstance)
                                } catch (e: IOException) {
                                    Tools.showErrorRemote(e)
                                }
                                dismiss()
                            },
                            onDismiss = { dismiss() }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setGravity(android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL)
    }

    companion object {
        const val TAG = "delete_dialog_confirm"
    }
}