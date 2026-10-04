package com.ashmeet.hyperlauncher.renderer.renderers

import com.ashmeet.hyperlauncher.renderer.RendererInterface

object PanfrostRenderer : RendererInterface {
    override fun getRendererId(): String = "gallium_panfrost"

    override fun getUniqueIdentifier(): String = "9b2808c4-11af-4c72-a9c6-94c940396475"

    override fun getRendererName(): String = "Panfrost (Mali)"

    override fun getMaxMCVersion(): String = "1.21.4"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa_2300d.so"
}