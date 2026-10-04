package com.ashmeet.hyperlauncher.plugin.renderer_v2

import com.ashmeet.hyperlauncher.plugin.Plugin
import com.ashmeet.hyperlauncher.plugin.renderer_v2.data.RendererConfig
import com.ashmeet.hyperlauncher.plugin.renderer_v2.data.RendererEnv
import com.ashmeet.hyperlauncher.renderer.RendererInterface

/**
 * V2 Renderer plugin data item
 * @param packageName Plugin package name
 * @param nativePath Native library path
 * @param renderer Imported renderer configuration
 */
class RendererV2Data(
    val packageName: String,
    val nativePath: String,
    val summary: String,
    val renderer: RendererConfig,
    genSummary: (metaString: String) -> String?,
): RendererInterface, Plugin {
    val env = RendererEnv(
        packageName = packageName,
        envs = renderer.env,
        genSummary = genSummary,
    )

    override fun getIdentifier(): String = packageName
    override fun getNativeLibPath(): String = nativePath

    override fun getRendererId(): String = renderer.rendererId
    override fun getUniqueIdentifier(): String = packageName
    override fun getRendererName(): String = renderer.displayName
    override fun getRendererSummary(): String = summary
    override fun getMinMCVersion(): String? = renderer.minMCVer
    override fun getMaxMCVersion(): String? = renderer.maxMCVer
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { env.getEnv() }
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { renderer.dlopenLibPaths }
    override fun getRendererLibrary(): String = renderer.rendererGLPath
    override fun getRendererEGL(): String = renderer.rendererEGLPath
}