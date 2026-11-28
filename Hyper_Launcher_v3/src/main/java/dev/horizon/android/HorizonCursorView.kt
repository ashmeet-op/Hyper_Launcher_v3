package dev.horizon.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
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

    private val customCursorPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }
    private var defaultCursorDrawable: Drawable
    private var cursorDrawable: Drawable
    private var noDraw = false
    private var mouseScale = 1f
    private var lastCursorShape = -1

    init {
        LauncherPreferences.loadPreferences(context)
        val drawable = ContextCompat.getDrawable(context, R.drawable.img_mouse_pointer_arrow)
        requireNotNull(drawable)
        defaultCursorDrawable = drawable
        if (defaultCursorDrawable is BitmapDrawable) {
            (defaultCursorDrawable as BitmapDrawable).isFilterBitmap = true
        }
        val size = HyperUtils.dpToPx(24f).toInt()
        val height = if (defaultCursorDrawable.intrinsicWidth > 0) {
            (size * (defaultCursorDrawable.intrinsicHeight.toFloat() / defaultCursorDrawable.intrinsicWidth)).toInt()
        } else size
        defaultCursorDrawable.setBounds(0, 0, size, height)

        var launcherCursor: PlatformCursor? = null
        try {
            launcherCursor = CursorUtils.loadStandardCursor(context, 0)
        } catch (_: Throwable) {}

        if (launcherCursor?.bitmap != null && !launcherCursor.bitmap.isRecycled) {
            defaultCursorDrawable = launcherCursor.bitmap.toDrawable(context.resources)
            if (defaultCursorDrawable is BitmapDrawable) {
                (defaultCursorDrawable as BitmapDrawable).isFilterBitmap = true
            }
            defaultCursorDrawable.setBounds(0, 0, launcherCursor.bitmap.width, launcherCursor.bitmap.height)
        }

        cursorDrawable = defaultCursorDrawable
        if (cursorDrawable is BitmapDrawable) {
            (cursorDrawable as BitmapDrawable).isFilterBitmap = true
        }
        mouseScale = LauncherPreferences.PREF_MOUSESCALE * 4f
    }

    private fun findGameView(): GameView? {
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
        if ((gameView != null) && (gameView.cursorRatioX > 0)) {
            return gameView.cursorRatioX
        }
        val windowWidth = GameView.getWindowWidth()
        val currentWidth = gameView?.width ?: width
        if (windowWidth > 0 && currentWidth > 0) {
            return currentWidth.toDouble() / windowWidth
        }
        return 1.0
    }

    private fun getCursorRatioY(): Double {
        val gameView = findGameView()
        if (gameView != null && gameView.cursorRatioY > 0) {
            return gameView.cursorRatioY
        }
        val windowHeight = GameView.getWindowHeight()
        val currentHeight = gameView?.height ?: height
        if (windowHeight > 0 && currentHeight > 0) {
            return currentHeight.toDouble() / windowHeight
        }
        return 1.0
    }

    private fun checkCursorShape() {
        try {
            var shape = HorizonInput.cursorShape()
            if (shape <= 0) {
                val menuCursor = HorizonInput.menuCursor()
                if (menuCursor > 0) {
                    shape = (menuCursor and 0xFFFFL).toInt()
                }
            }
            if (shape != lastCursorShape) {
                lastCursorShape = shape
                val shapeName = when (shape) {
                    0, 0x00036001 -> 0 // ARROW
                    1, 0x00036002 -> 1 // IBEAM
                    2, 0x00036003 -> 2 // CROSSHAIR
                    3, 0x00036004 -> 3 // HAND
                    4, 0x00036005 -> 4 // HRESIZE
                    5, 0x00036006 -> 5 // VRESIZE
                    6, 0x00036007, 0x00036008, 0x00036009 -> 6 // RESIZE
                    7, 0x0003600A -> 7 // NOT ALLOWED
                    else -> if (shape in 0..7) shape else -1
                }
                if (shapeName > 0) {
                    val stdCursor = CursorUtils.loadStandardCursor(context, shapeName)
                    if (stdCursor?.bitmap != null && !stdCursor.bitmap.isRecycled) {
                        cursorDrawable = stdCursor.bitmap.toDrawable(context.resources)
                        if (cursorDrawable is BitmapDrawable) {
                            (cursorDrawable as BitmapDrawable).isFilterBitmap = true
                        }
                        cursorDrawable.setBounds(0, 0, stdCursor.bitmap.width, stdCursor.bitmap.height)
                        Platform.setCursor(stdCursor.bitmap, stdCursor.hotX, stdCursor.hotY)
                    } else {
                        cursorDrawable = defaultCursorDrawable
                        Platform.setCursor(null, 0, 0)
                    }
                } else {
                    cursorDrawable = defaultCursorDrawable
                    Platform.setCursor(null, 0, 0)
                }
                updatePointerIcon()
                invalidate()
            }
        } catch (_: Throwable) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        if (noDraw) return
        checkCursorShape()
        val dx = (Platform.cursorX * getCursorRatioX()).toInt()
        val dy = (Platform.cursorY * getCursorRatioY()).toInt()
        canvas.translate(dx.toFloat(), dy.toFloat())
        val cursor = Platform.getCursor()
        canvas.scale(mouseScale, mouseScale)
        if (cursor?.bitmap == null || cursor.bitmap.isRecycled) {
            cursorDrawable.draw(canvas)
        } else {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX.toFloat(), -cursor.hotY.toFloat(), customCursorPaint)
        }
    }

    override fun onCursorPosition() {
        if (!noDraw) post {
            checkCursorShape()
            invalidate()
        }
    }

    override fun onCursorChanged() {
        post {
            checkCursorShape()
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
