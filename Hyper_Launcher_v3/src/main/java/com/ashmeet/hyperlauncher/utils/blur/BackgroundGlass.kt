package com.ashmeet.hyperlauncher.utils.blur

import android.graphics.Matrix
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.core.graphics.withSave
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

typealias BackgroundViewModel = BackgroundGlassBlur

val LocalBackgroundViewModel = compositionLocalOf<BackgroundGlassBlur?> { null }


@Composable
fun Modifier.backgroundGlass(
    blur: Int,
    color: Color,
    enabled: Boolean = true,
): Modifier {
    if (!enabled || !LauncherPreferences.PREF_LIQUID_GLASS_ELEMENTS_ENABLED) return this
    val background = LocalBackgroundViewModel.current?.takeIf { it.isValid } ?: return this
    if (blur <= 0 || LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_OPACITY >= 100) return this
    return this then GlassElement(blur, color, background)
}


internal fun Modifier.backgroundCapture(
    store: BackgroundViewModel,
    recordContent: Boolean,
    blurRadiusPx: Float,
    whiteOverlayAlpha: Float,
): Modifier = this then BackgroundCaptureElement(store, recordContent, blurRadiusPx, whiteOverlayAlpha)

internal fun whiteOverlayAlpha(blur: Int): Float {
    val t = (blur / 80f).coerceIn(0f, 1f)
    return lerp(start = 0f, stop = 0.25f, fraction = sqrt(t))
}

private data class GlassElement(
    val blur: Int,
    val color: Color,
    val store: BackgroundViewModel,
) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(blur, color, store)

    override fun update(node: GlassNode) {
        node.blur = blur
        node.color = color
    }
}

private class GlassNode(
    blur: Int,
    color: Color,
    val store: BackgroundViewModel,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    var blur: Int = blur
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    var color: Color = color
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    //元素在屏幕坐标系中的位置，随布局逐帧更新，绘制时据此对齐采样
    private var screenOrigin = Offset.Zero
    private var hasOrigin = false

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val position = coordinates.positionOnScreen()
        if (!position.isSpecified) return
        if (!hasOrigin || position != screenOrigin) {
            screenOrigin = position
            hasOrigin = true
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawGlass()
        drawContent()
    }

    private fun DrawScope.drawGlass() {
        if (blur <= 0 || LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_OPACITY >= 100) return
        val bounds = store.backgroundBounds
        if (bounds.isEmpty || !hasOrigin) return

        val nativeCanvas = drawContext.canvas.nativeCanvas
        nativeCanvas.withSave {
            val current = Matrix()
            @Suppress("DEPRECATION")
            getMatrix(current)
            val inverted = Matrix()
            if (current.invert(inverted)) {
                val values = FloatArray(9).also(current::getValues)
                val left = bounds.left - screenOrigin.x + values[Matrix.MTRANS_X]
                val top = bounds.top - screenOrigin.y + values[Matrix.MTRANS_Y]

                concat(inverted)
                if (Build.VERSION.SDK_INT >= 31) {
                    store.glassLayer?.let { layer ->
                        this@drawGlass.translate(left, top) {
                            drawLayer(layer)
                        }
                    }
                } else {
                    store.blurredBackground?.let { bitmap ->
                        this@drawGlass.drawImage(
                            image = bitmap,
                            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                            dstSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt())
                        )
                    }
                }
            }
        }

        drawRect(color = color, blendMode = BlendMode.SrcOver)
        drawRect(
            color = Color.White.copy(alpha = whiteOverlayAlpha(blur)),
            blendMode = BlendMode.Softlight
        )
    }
}

private data class BackgroundCaptureElement(
    val store: BackgroundViewModel,
    val recordContent: Boolean,
    val blurRadiusPx: Float,
    val whiteOverlayAlpha: Float,
) : ModifierNodeElement<BackgroundCaptureNode>() {
    override fun create() = BackgroundCaptureNode(store, recordContent, blurRadiusPx, whiteOverlayAlpha)

    override fun update(node: BackgroundCaptureNode) {
        node.store = store
        node.recordContent = recordContent
        node.blurRadiusPx = blurRadiusPx
        node.whiteOverlayAlpha = whiteOverlayAlpha
        node.sync()
        node.invalidateDraw()
    }
}


private class BackgroundCaptureNode(
    var store: BackgroundViewModel,
    var recordContent: Boolean,
    var blurRadiusPx: Float,
    var whiteOverlayAlpha: Float,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var graphicsContext: GraphicsContext? = null
    private var sourceLayer: GraphicsLayer? = null
    private var blurredLayer: GraphicsLayer? = null
    private var blurredEffectRadius = 0f
    private var captureJob: Job? = null

    override fun onAttach() {
        sync()
    }

    override fun onDetach() {
        releaseLayers()
        graphicsContext = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        store.updateBackgroundBounds(coordinates)
    }

    override fun ContentDrawScope.draw() {
        val source = sourceLayer
        if (recordContent && source != null) {
            source.record { this@draw.drawContent() }
            drawLayer(source)
            if (Build.VERSION.SDK_INT >= 31) {
                ensureBlurredLayer().record { drawLayer(source) }
            }
        } else {
            drawContent()
        }
        if (whiteOverlayAlpha > 0f) {
            drawRect(
                color = Color.White.copy(alpha = whiteOverlayAlpha),
                blendMode = BlendMode.Softlight
            )
        }
    }


    fun sync() {
        syncLayers()
        syncCaptureLoop()
    }

    private fun syncLayers() {
        if (recordContent) {
            val source = sourceLayer ?: createLayer().also { sourceLayer = it }
            if (Build.VERSION.SDK_INT >= 31) {
                store.attachGlassLayer(ensureBlurredLayer())
                store.attachCaptureLayer(null)
            } else {
                store.attachCaptureLayer(source)
                store.attachGlassLayer(null)
            }
        } else {
            releaseLayers()
        }
    }

    private fun ensureBlurredLayer(): GraphicsLayer {
        blurredLayer?.let { layer ->
            if (blurredEffectRadius != blurRadiusPx) {
                layer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
                blurredEffectRadius = blurRadiusPx
            }
            return layer
        }
        val layer = createLayer()
        layer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
        blurredEffectRadius = blurRadiusPx
        blurredLayer = layer
        return layer
    }

    private fun createLayer(): GraphicsLayer {
        val context = currentValueOf(LocalGraphicsContext)
        graphicsContext = context
        return context.createGraphicsLayer()
    }

    private fun syncCaptureLoop() {
        if (recordContent && Build.VERSION.SDK_INT < 31) {
            if (captureJob?.isActive == true) return
            captureJob = coroutineScope.launch {
                snapshotFlow {
                    store.isValid && store.captureLayer != null &&
                            LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR > 0 &&
                            LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_OPACITY < 100
                }.collectLatest { active ->
                    if (!active) return@collectLatest
                    while (currentCoroutineContext().isActive) {
                        val started = System.nanoTime()
                        store.captureGlassFrame()
                        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
                        delay((16L - elapsedMs).coerceAtLeast(0L).milliseconds)
                    }
                }
            }
        } else {
            captureJob?.cancel()
            captureJob = null
        }
    }

    private fun releaseLayers() {
        store.attachGlassLayer(null)
        store.attachCaptureLayer(null)
        val context = graphicsContext
        sourceLayer?.let { context?.releaseGraphicsLayer(it) }
        sourceLayer = null
        blurredLayer?.let { context?.releaseGraphicsLayer(it) }
        blurredLayer = null
    }
}
