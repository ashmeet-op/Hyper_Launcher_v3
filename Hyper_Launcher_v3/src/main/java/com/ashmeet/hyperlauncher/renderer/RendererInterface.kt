package com.ashmeet.hyperlauncher.renderer

interface RendererInterface {
    fun getRendererId(): String

    fun getUniqueIdentifier(): String

    fun getRendererName(): String

    fun getRendererSummary(): String? = null

    fun getMinMCVersion(): String? = null

    fun getMaxMCVersion(): String? = null


    fun getDisplayMinMCVersion(): String? = getMinMCVersion()

    fun getDisplayMaxMCVersion(): String? = getMaxMCVersion()

    fun getRendererEnv(): Lazy<Map<String, String>>

    fun getDlopenLibrary(): Lazy<List<String>>


    fun getRendererLibrary(): String

    fun getRendererEGL(): String? = null
}