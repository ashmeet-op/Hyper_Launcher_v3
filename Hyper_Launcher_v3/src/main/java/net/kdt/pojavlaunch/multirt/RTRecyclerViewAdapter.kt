package net.kdt.pojavlaunch.multirt

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.ashmeet.hyperlauncher.components.list.MultiRTRuntimeItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.utils.Tools
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException

class RTRecyclerViewAdapter : RecyclerView.Adapter<RTRecyclerViewAdapter.RTViewHolder>() {

    interface OnRuntimeSelectedListener {
        fun onRuntimeSelected(runtime: Runtime)
        fun onRuntimeDeleted()
    }

    private var mListener: OnRuntimeSelectedListener? = null
    private var mIsDeleting = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RTViewHolder {
        return RTViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: RTViewHolder, position: Int) {
        val runtimes = MultiRTUtils.getRuntimes()
        holder.bindRuntime(runtimes[position], position)
    }

    override fun getItemCount(): Int = MultiRTUtils.getRuntimes().size

    fun isDefaultRuntime(rt: Runtime): Boolean = LauncherPreferences.PREF_DEFAULT_RUNTIME == rt.name

    @SuppressLint("NotifyDataSetChanged")
    fun setDefault(rt: Runtime) {
        LauncherPreferences.PREF_DEFAULT_RUNTIME = rt.name
        LauncherPreferences.DEFAULT_PREF?.edit()?.putString("defaultRuntime", LauncherPreferences.PREF_DEFAULT_RUNTIME)?.apply()
        notifyDataSetChanged()
    }

    inner class RTViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bindRuntime(runtime: Runtime, pos: Int) {
            val isCompatible = runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch)
            
            val javaName = if (isCompatible) {
                runtime.name.replace(".tar.xz", "").replace("-", " ")
            } else {
                runtime.name
            }
            
            val javaVersion = when {
                runtime.versionString == null -> composeView.context.getString(R.string.multirt_runtime_corrupt)
                !isCompatible -> composeView.context.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch)
                else -> runtime.versionString
            }

            composeView.setContent {
                MultiRTRuntimeItem(
                    javaVersionName = javaName,
                    javaVersionFull = javaVersion,
                    isSelected = isDefaultRuntime(runtime) && !mIsDeleting,
                    onRemove = if (mIsDeleting || !isCompatible) {
                        { deleteRuntime(runtime) }
                    } else null,
                    onClick = {
                        if (!mIsDeleting && isCompatible && !isDefaultRuntime(runtime)) {
                            setDefault(runtime)
                            mListener?.onRuntimeSelected(runtime)
                        }
                    }
                )
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun deleteRuntime(runtime: Runtime) {
            if (MultiRTUtils.getRuntimes().size < 2) {
                MaterialAlertDialogBuilder(composeView.context)
                    .setTitle(R.string.global_error)
                    .setMessage(R.string.multirt_config_removeerror_last)
                    .setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                    .show()
                return
            }

            PojavApplication.sExecutorService.execute {
                try {
                    MultiRTUtils.removeRuntimeNamed(runtime.name)
                    composeView.post {
                        notifyDataSetChanged()
                        mListener?.onRuntimeDeleted()
                    }
                } catch (e: IOException) {
                    Tools.showError(composeView.context, e)
                }
            }
        }
    }
}
