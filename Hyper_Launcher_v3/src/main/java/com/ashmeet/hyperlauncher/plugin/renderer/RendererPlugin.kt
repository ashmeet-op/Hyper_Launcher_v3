package com.ashmeet.hyperlauncher.plugin.renderer

import com.ashmeet.hyperlauncher.plugin.Plugin
import com.ashmeet.hyperlauncher.renderer.RendererInterface

class RendererPlugin(
    val packageName: String,
    val id: String,
    val displayName: String,
    val summary: String? = null,
    val minMCVer: String? = null,
    val maxMCVer: String? = null,
    private val glName: String,
    private val eglName: String,
    val path: String,
    private val env: Map<String, String>,
    private val dlopen: List<String>,
    val isConfigurable: Boolean = false,
): RendererInterface, Plugin {
    override fun getIdentifier(): String = packageName
    override fun getNativeLibPath(): String = path

    override fun getRendererId(): String = id
    override fun getUniqueIdentifier(): String = packageName
    override fun getRendererName(): String = displayName
    override fun getRendererSummary(): String? = summary
    override fun getMinMCVersion(): String? = minMCVer
    override fun getMaxMCVersion(): String? = maxMCVer
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { env }
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy {
        dlopen.map { lib -> "$path/$lib" }
    }
    override fun getRendererLibrary(): String = "$path/$glName"
    override fun getRendererEGL(): String = eglName
}
