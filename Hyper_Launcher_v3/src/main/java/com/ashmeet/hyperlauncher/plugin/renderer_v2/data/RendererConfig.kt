package com.ashmeet.hyperlauncher.plugin.renderer_v2.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * @param displayName               Display name shown to the user
 * @param rendererId                Renderer ID passed via POJAV_RENDERER environment variable
 * @param rendererGLPath            Path to OpenGL library
 * @param rendererEGLPath           Path to EGL library
 * @param dlopenLibPaths            Paths to libraries requiring dlopen
 * @param env                       List of environment variables
 * @param minMCVer                  Minimum supported Minecraft version, or null if unrestricted
 * @param maxMCVer                  Maximum supported Minecraft version, or null if unrestricted
 */
@Serializable
data class RendererConfig(
    @SerialName("displayName")
    val displayName: String,
    @SerialName("rendererId")
    val rendererId: String,
    @SerialName("rendererGLPath")
    val rendererGLPath: String,
    @SerialName("rendererEGLPath")
    val rendererEGLPath: String,
    @SerialName("dlopenLibPaths")
    val dlopenLibPaths: List<String>,
    @SerialName("env")
    val env: List<Env>,
    @SerialName("minMCVer")
    val minMCVer: String?,
    @SerialName("maxMCVer")
    val maxMCVer: String?,
) {
    @Serializable
    sealed interface Env {
        /**
         * Fixed, non-configurable environment variable
         */
        @Serializable
        @SerialName("NormalEnv")
        data class NormalEnv(
            @SerialName("key")
            val key: String,
            @SerialName("value")
            val value: String,
        ): Env

        /**
         * Environment variable selectable from predefined options
         * @see EnvItems
         * @param check Whether enabled by default (null = always applied)
         * @param title Configuration item title (meta-data index)
         * @param items Environment variable options
         */
        @Serializable
        @SerialName("SelectableEnv")
        data class SelectableEnv(
            @SerialName("key")
            val key: String,
            @SerialName("title")
            val title: MetaString? = null,
            @SerialName("check")
            val check: Boolean? = true,
            @SerialName("items")
            val items: EnvItems
        ): Env

        /**
         * User-customizable environment variable
         * @param title Configuration item title (meta-data index)
         * @param defaultValue Default value
         */
        @Serializable
        @SerialName("CustomizableEnv")
        data class CustomizableEnv(
            @SerialName("key")
            val key: String,
            @SerialName("title")
            val title: MetaString? = null,
            @SerialName("defaultValue")
            val defaultValue: String? = null,
        ): Env

        /**
         * Toggleable environment variable
         * @param title Configuration item title (meta-data index)
         * @param toggle Default state
         */
        @Serializable
        @SerialName("ToggleableEnv")
        data class ToggleableEnv(
            @SerialName("key")
            val key: String,
            @SerialName("value")
            val value: String,
            @SerialName("title")
            val title: MetaString? = null,
            @SerialName("toggle")
            val toggle: Boolean = true,
        ): Env
    }

    /**
     * Environment variable options
     * @param defaultValue Default environment variable value
     * @param values Selectable environment variable values
     */
    @Serializable
    data class EnvItems(
        @SerialName("defaultValue")
        val defaultValue: String,
        @SerialName("values")
        val values: List<String>,
    )

    /**
     * String resource metadata entry for localized titles
     */
    @Serializable
    data class MetaString(
        @SerialName("key")
        val key: String
    )
}

private fun String.resolveNativePath(nativeLibDir: String): String {
    if (!startsWith("**|")) return this
    return File(nativeLibDir, removePrefix("**|")).absolutePath
}

/**
 * Resolves relative paths prefixed with **| to nativeLibraryDir absolute path
 */
fun RendererConfig.resolveNativePaths(nativeLibDir: String): RendererConfig {
    fun String.replacePath() = this.resolveNativePath(nativeLibDir)

    return copy(
        rendererGLPath = rendererGLPath.replacePath(),
        rendererEGLPath = rendererEGLPath.replacePath(),
        dlopenLibPaths = dlopenLibPaths.map { it.replacePath() },
        env = env.map { env ->
            when (env) {
                is RendererConfig.Env.NormalEnv -> env.copy(value = env.value.replacePath())
                is RendererConfig.Env.ToggleableEnv -> env.copy(value = env.value.replacePath())
                else -> env
            }
        }
    )
}
