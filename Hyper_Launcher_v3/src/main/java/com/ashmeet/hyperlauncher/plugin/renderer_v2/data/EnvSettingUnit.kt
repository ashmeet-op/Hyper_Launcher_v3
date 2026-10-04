package com.ashmeet.hyperlauncher.plugin.renderer_v2.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

abstract class AbstractSettingUnit<T>(
    val key: String,
    val defaultValue: T
) {
    var state by mutableStateOf(defaultValue)

    open fun init() {
        state = getValue()
    }

    open fun save(v: T) {
        state = saveValue(v)
    }

    abstract fun getValue(): T
    abstract fun saveValue(v: T): T
}

/**
 * Sealed setting unit for configurable environment variables
 * @param summary Description text provided by the plugin
 */
sealed class EnvSettingUnit(
    mmkvKey: String,
    defaultValue: String,
    val summary: String?,
) : AbstractSettingUnit<String>(mmkvKey, defaultValue) {

    override fun getValue(): String {
        return rendererEnvMMKV().decodeString(key, defaultValue) ?: defaultValue
            .also { state = it }
    }

    override fun saveValue(v: String): String {
        rendererEnvMMKV().encodeString(key, v)
        return v
    }

    /**
     * Selectable environment variable: select value from predefined list
     * @param rawEnv Raw environment variable configuration
     * @param values All selectable values
     */
    class Selectable(
        mmkvKey: String,
        val rawEnv: RendererConfig.Env.SelectableEnv,
        defaultValue: String,
        val values: List<String>,
        summary: String? = null,
    ) : EnvSettingUnit(mmkvKey, defaultValue, summary) {
        private val checkKey = "${mmkvKey}:check"

        /**
         * Whether this environment variable is currently enabled
         */
        var isEnabled by mutableStateOf(rawEnv.check != false)
            private set

        fun initCheck() {
            val mmkv = rendererEnvMMKV()
            val pluginDefault = rawEnv.check != false

            if (rawEnv.check == null) {
                isEnabled = true
            } else if (mmkv.containsKey(checkKey)) {
                isEnabled = mmkv.decodeBool(checkKey, pluginDefault)
            } else {
                isEnabled = pluginDefault
            }
        }

        fun saveCheck(enabled: Boolean) {
            isEnabled = enabled
            rendererEnvMMKV().encodeBool(checkKey, enabled)
        }
    }

    /**
     * Customizable environment variable: custom user input
     * @param rawEnv Raw environment variable configuration
     */
    class Customizable(
        mmkvKey: String,
        val rawEnv: RendererConfig.Env.CustomizableEnv,
        defaultValue: String,
        summary: String? = null,
    ) : EnvSettingUnit(mmkvKey, defaultValue, summary)

    /**
     * Toggleable environment variable: enable/disable variable
     * Enabled value is [RendererConfig.Env.ToggleableEnv.value]
     * @param rawEnv Raw environment variable configuration
     * @param envValue Actual environment variable value when toggle is true
     */
    class Toggleable(
        mmkvKey: String,
        val rawEnv: RendererConfig.Env.ToggleableEnv,
        defaultValue: String,
        val envValue: String,
        summary: String? = null,
    ) : EnvSettingUnit(mmkvKey, defaultValue, summary) {
        /** Whether toggle is currently enabled */
        val isEnabled: Boolean get() = state.isNotEmpty()
    }
}
