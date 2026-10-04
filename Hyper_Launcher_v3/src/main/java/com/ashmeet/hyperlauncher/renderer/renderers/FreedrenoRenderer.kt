package com.ashmeet.hyperlauncher.renderer.renderers

import com.ashmeet.hyperlauncher.renderer.RendererInterface

object FreedrenoRenderer : RendererInterface {
    override fun getRendererId(): String = "gallium_freedreno"

    override fun getUniqueIdentifier(): String = "1ad7249f-5784-4f00-bc72-174b3578ee46"

    override fun getRendererName(): String = "Freedreno (Adreno)"

    override fun getMaxMCVersion(): String = "26.3-snapshot-3"

    override fun getDisplayMaxMCVersion(): String = "26.2"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa_8.so"
}