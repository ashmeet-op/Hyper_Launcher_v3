package com.ashmeet.hyperlauncher.plugin.renderer_v2.data

/**
 * Renderer configuration state and storage
 * @param packageName Plugin package name for MMKV storage isolation
 * @param envs Renderer environment variables
 * @param genSummary Converts [RendererConfig.MetaString] to localized text
 */
class RendererEnv(
    val packageName: String,
    val envs: List<RendererConfig.Env>,
    genSummary: (metaString: String) -> String?,
) {
    private val settingUnits: Map<String, EnvSettingUnit>

    init {
        val mmkv = rendererEnvMMKV()
        val prefix = "$packageName:"

        // Collect all configurable environment variable keys (Selectable / Customizable / Toggleable)
        val currentConfigurableKeys = envs.mapNotNull { env ->
            when (env) {
                is RendererConfig.Env.NormalEnv -> null
                is RendererConfig.Env.SelectableEnv -> env.key
                is RendererConfig.Env.CustomizableEnv -> env.key
                is RendererConfig.Env.ToggleableEnv -> env.key
            }
        }.toSet()

        // Clean up stored keys no longer supported after plugin update
        mmkv.allKeys
            .filter { it.startsWith(prefix) }
            .forEach { storedKey ->
                val envKey = storedKey.removePrefix(prefix)
                val baseKey = envKey.removeSuffix(":check")
                if (baseKey !in currentConfigurableKeys) {
                    mmkv.removeValueForKey(storedKey)
                }
            }

        // Create setting unit for each configurable environment variable
        val units = mutableMapOf<String, EnvSettingUnit>()
        for (env in envs) {
            when (env) {
                is RendererConfig.Env.NormalEnv -> {}

                is RendererConfig.Env.SelectableEnv -> {
                    val mmkvKey = "$prefix${env.key}"
                    val summary = env.getTitleMetaString()?.let { genSummary(it) }
                    val unit = EnvSettingUnit.Selectable(
                        mmkvKey = mmkvKey,
                        rawEnv = env,
                        defaultValue = env.items.defaultValue,
                        values = buildList {
                            add(env.items.defaultValue)
                            addAll(env.items.values)
                        },
                        summary = summary
                    )
                    unit.init()
                    unit.initCheck()

                    // Reset stored value to default if no longer in valid options
                    if (unit.state !in env.items.values) {
                        unit.save(env.items.defaultValue)
                    }

                    units[env.key] = unit
                }

                is RendererConfig.Env.CustomizableEnv -> {
                    val mmkvKey = "$prefix${env.key}"
                    val summary = env.getTitleMetaString()?.let { genSummary(it) }
                    val default = env.defaultValue ?: ""
                    val unit = EnvSettingUnit.Customizable(
                        mmkvKey = mmkvKey,
                        rawEnv = env,
                        defaultValue = default,
                        summary = summary
                    )
                    unit.init()
                    units[env.key] = unit
                }

                is RendererConfig.Env.ToggleableEnv -> {
                    val mmkvKey = "$prefix${env.key}"
                    val summary = env.getTitleMetaString()?.let { genSummary(it) }
                    val default = if (env.toggle) env.value else ""
                    val unit = EnvSettingUnit.Toggleable(
                        mmkvKey = mmkvKey,
                        rawEnv = env,
                        defaultValue = default,
                        envValue = env.value,
                        summary = summary
                    )
                    unit.init()
                    units[env.key] = unit
                }
            }
        }
        settingUnits = units
    }

    /**
     * Get current environment variable map for this renderer
     */
    fun getEnv(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (env in envs) {
            when (env) {
                is RendererConfig.Env.NormalEnv -> {
                    result[env.key] = env.value
                }
                is RendererConfig.Env.SelectableEnv -> {
                    val unit = settingUnits[env.key] as? EnvSettingUnit.Selectable
                    if (unit != null && unit.isEnabled) {
                        result[env.key] = unit.state
                    }
                }
                is RendererConfig.Env.CustomizableEnv -> {
                    val unit = settingUnits[env.key] as? EnvSettingUnit.Customizable
                    if (unit != null && unit.state.isNotEmpty()) {
                        result[env.key] = unit.state
                    }
                }
                is RendererConfig.Env.ToggleableEnv -> {
                    val unit = settingUnits[env.key] as? EnvSettingUnit.Toggleable
                    if (unit != null && unit.isEnabled) {
                        result[env.key] = unit.envValue
                    }
                }
            }
        }
        return result
    }

    /**
     * Get setting units for all configurable environment variables
     */
    fun getConfigurableUnits(): List<EnvSettingUnit> = settingUnits.values.toList()

    /**
     * Extract MetaString key from [RendererConfig.Env]
     */
    private fun RendererConfig.Env.getTitleMetaString(): String? {
        return when (this) {
            is RendererConfig.Env.NormalEnv -> null
            is RendererConfig.Env.SelectableEnv -> title?.key
            is RendererConfig.Env.CustomizableEnv -> title?.key
            is RendererConfig.Env.ToggleableEnv -> title?.key
        }
    }
}
