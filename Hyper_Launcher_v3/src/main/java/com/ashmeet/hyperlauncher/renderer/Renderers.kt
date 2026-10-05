package com.ashmeet.hyperlauncher.renderer

import android.util.Log
import com.ashmeet.hyperlauncher.renderer.renderers.FreedrenoRenderer
import com.ashmeet.hyperlauncher.renderer.renderers.GL4ESRenderer
import com.ashmeet.hyperlauncher.renderer.renderers.KopperZinkRenderer
import com.ashmeet.hyperlauncher.renderer.renderers.NGGL4ESRenderer
import com.ashmeet.hyperlauncher.renderer.renderers.PanfrostRenderer
import com.ashmeet.hyperlauncher.renderer.renderers.VirGLRenderer


private const val TAG = "Renderers"

object Renderers {
    private val renderers: MutableList<RendererInterface> = mutableListOf()
    private var currentRenderer: RendererInterface? = null
    private var isInitialized: Boolean = false

    fun init(
        reset: Boolean = false
    ) {
        if (isInitialized && !reset) return
        isInitialized = true

        if (reset) {
            renderers.clear()
            currentRenderer = null
        }

        addRenderers(
            NGGL4ESRenderer,
            GL4ESRenderer,
            KopperZinkRenderer,
            VirGLRenderer,
            FreedrenoRenderer,
            PanfrostRenderer
        )
    }

    /**
     * Get the current list of renderers
     */
    fun getRenderers(): List<RendererInterface> = renderers

    fun addRenderers(vararg renderers: RendererInterface) {
        renderers.forEach { renderer ->
            addRenderer(renderer)
        }
    }

    fun addRenderer(renderer: RendererInterface): Boolean {
        return if (renderers.any { it.getUniqueIdentifier() == renderer.getUniqueIdentifier() }) {
            Log.w(TAG, "The unique identifier of this renderer (${renderer.getRendererName()} - ${renderer.getUniqueIdentifier()}) conflicts with an already loaded renderer. " +
                    "Normally, this shouldn't happen. You deliberately caused this conflict, didn't you, user?")
            false
        } else {
            renderers.add(renderer)
            Log.i(TAG, "Renderer loaded: ${renderer.getRendererName()} (${renderer.getRendererId()} - ${renderer.getUniqueIdentifier()})")
            true
        }
    }

    /**
     * Get current selected renderer
     */
    fun getCurrentRenderer(): RendererInterface {
        if (!isInitialized) throw IllegalStateException("Uninitialized renderer!")
        return currentRenderer ?: throw IllegalStateException("Current renderer not set")
    }

}