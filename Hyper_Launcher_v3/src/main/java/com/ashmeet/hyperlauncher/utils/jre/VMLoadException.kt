package com.ashmeet.hyperlauncher.utils.jre

import android.content.Context
import net.ashmeet.hyperlauncher.R

class VMLoadException(
    errorInfo: String?,
    private val loadStep: Int,
    private val errorCode: Int
) : Exception(errorInfo) {

    companion object {
        private fun getLoadStepRes(loadStep: Int): Int {
            return when (loadStep) {
                0 -> R.string.vml_fail_load_runtime
                1 -> R.string.vml_fail_create_runtime
                2 -> R.string.vml_fail_find_hooks_native
                3 -> R.string.vml_fail_find_hooks
                4 -> R.string.vml_fail_insert_hooks
                5 -> R.string.vml_fail_load_classpath
                6 -> R.string.vml_fail_run_main
                else -> R.string.vml_huh
            }
        }

        private fun getErrorCodeRes(errorCode: Int): Int {
            return when (errorCode) {
                0 -> R.string.vml_err_ok
                -2 -> R.string.vml_err_detached
                -3 -> R.string.vml_err_version
                -4 -> R.string.vml_err_nomem
                -5 -> R.string.vml_err_exists
                -6 -> R.string.vml_err_inval
                else -> R.string.vml_err_unknown
            }
        }
    }

    fun toString(context: Context): String {
        val loadStepRes = getLoadStepRes(loadStep)
        return when (loadStep) {
            0 -> context.getString(loadStepRes, message)
            1, 4 -> context.getString(loadStepRes, context.getString(getErrorCodeRes(errorCode)))
            else -> context.getString(loadStepRes)
        }
    }
}
