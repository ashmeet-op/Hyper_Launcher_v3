package dev.horizon.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.PointerIcon
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.ashmeet.hyperbackend.customcontrols.ControlLayout
import com.ashmeet.hyperbackend.game.GameView
import com.ashmeet.hyperbackend.game.platform.Platform
import com.ashmeet.hyperbackend.game.platform.cursor.CursorUtils
import com.ashmeet.hyperbackend.game.platform.cursor.PlatformCursor
import com.ashmeet.hyperbackend.game.platform.cursor.PlatformCursorImplementor
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.HyperUtils
import net.ashmeet.hyperlauncher.R

class HorizonCursorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), PlatformCursorImplementor {

    private val customCursorPaint = Paint()
    private val cursorDrawable: Drawable
    private var noDraw = false
    private var mouseScale = LauncherPreferences.PREF_MOUSESCALE

    init {
        var launcherCursor: PlatformCursor? = null
        try {
            launcherCursor = CursorUtils.loadStandardCursor(context, 0)
        } catch (_: Throwable) {}

        if (launcherCursor?.bitmap != null && !launcherCursor.bitmap.isRecycled) {
            cursorDrawable = launcherCursor.bitmap.toDrawable(context.resources)
            cursorDrawable.setBounds(0, 0, launcherCursor.bitmap.width, launcherCursor.bitmap.height)
        } else {
            val drawable = ContextCompat.getDrawable(context, R.drawable.img_mouse_pointer_arrow)
            requireNotNull(drawable)
            cursorDrawable = drawable
            val size = HyperUtils.dpToPx(24f).toInt()
            cursorDrawable.setBounds(0, 0, size, (size * (cursorDrawable.intrinsicHeight.toFloat() / cursorDrawable.intrinsicWidth)).toInt())
        }
    }

    private fun findGameView(): View? {
        if (parent is GameView) {
            return parent as GameView
        }
        if (parent is ControlLayout) {
            val gv = (parent as ControlLayout).gameSurface
            if (gv != null) return gv
        }
        if (parent is View) {
            val root = (parent as View).rootView
            if (root != null) {
                val gv = root.findViewById<View>(R.id.main_game_render_view)
                if (gv is GameView) return gv
            }
        }
        return Platform.getHostView()
    }

    private fun getCursorRatioX(): Double {
        val gameView = findGameView()
        val windowWidth = GameView.getWindowWidth()
        val currentWidth = gameView?.width ?: width
        if (windowWidth > 0 && currentWidth > 0) {
            return currentWidth.toDouble() / windowWidth
        }
        return 1.0
    }

    private fun getCursorRatioY(): Double {
        val gameView = findGameView()
        val windowHeight = GameView.getWindowHeight()
        val currentHeight = gameView?.height ?: height
        if (windowHeight > 0 && currentHeight > 0) {
            return currentHeight.toDouble() / windowHeight
        }
        return 1.0
    }

    override fun onDraw(canvas: Canvas) {
        if (noDraw) return
        val dx = (Platform.cursorX * getCursorRatioX()).toInt()
        val dy = (Platform.cursorY * getCursorRatioY()).toInt()
        canvas.translate(dx.toFloat(), dy.toFloat())
        val cursor = Platform.getCursor()
        canvas.scale(mouseScale, mouseScale)
        if (cursor == null) {
            cursorDrawable.draw(canvas)
        } else {
            cursor.bitmap?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    canvas.drawBitmap(bitmap, -cursor.hotX.toFloat(), -cursor.hotY.toFloat(), customCursorPaint)
                }
            }
        }
    }

    override fun onCursorPosition() {
        if (!noDraw) post { invalidate() }
    }

    override fun onCursorChanged() {
        post {
            invalidate()
            updatePointerIcon()
        }
    }

    override fun onGrabState(isGrabbing: Boolean) {
        noDraw = isGrabbing
        post {
            invalidate()
            updatePointerIcon()
        }
    }

    fun updatePointerIcon() {
        if (noDraw) {
            setPointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_NULL))
            (parent as? View)?.setPointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_NULL))
            return
        }
        val cursor = Platform.getCursor()
        val icon: PointerIcon = if (cursor?.bitmap != null && !cursor.bitmap.isRecycled) {
            val hX = cursor.hotX.coerceIn(0, cursor.bitmap.width - 1)
            val hY = cursor.hotY.coerceIn(0, cursor.bitmap.height - 1)
            PointerIcon.create(cursor.bitmap, hX.toFloat(), hY.toFloat())
        } else {
            PointerIcon.getSystemIcon(context, PointerIcon.TYPE_ARROW)
        }
        setPointerIcon(icon)
        (parent as? View)?.setPointerIcon(icon)
    }

    fun setCursorScale(scale: Float) {
        this.mouseScale = scale
    }

    override fun getImplementorContext(): Context {
        return context
    }
}
