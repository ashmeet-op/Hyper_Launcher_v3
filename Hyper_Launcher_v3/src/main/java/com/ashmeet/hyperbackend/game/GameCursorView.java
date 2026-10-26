package com.ashmeet.hyperbackend.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.PointerIcon;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.ashmeet.hyperlauncher.R;
import com.ashmeet.hyperlauncher.utils.HyperUtils;
import com.ashmeet.hyperbackend.game.platform.Platform;
import com.ashmeet.hyperbackend.game.platform.cursor.PlatformCursor;
import com.ashmeet.hyperbackend.game.platform.cursor.PlatformCursorImplementor;



public class GameCursorView extends View implements PlatformCursorImplementor {
    private final Paint customCursorPaint = new Paint();
    private final Drawable cursorDrawable;
    private boolean noDraw = false;
    private float mouseScale = 1f;

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GameCursorView(Context context) {
        this(context, null);
    }

    public GameCursorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        cursorDrawable = ContextCompat.getDrawable(context, R.drawable.img_mouse_pointer_arrow);
        assert cursorDrawable != null;
        int size = (int) HyperUtils.dpToPx(24);
        cursorDrawable.setBounds(0, 0, size, (int) (size * ((float) cursorDrawable.getIntrinsicHeight() / cursorDrawable.getIntrinsicWidth())));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (noDraw) return;
        int dx = (int) (Platform.cursorX * ((GameView)getParent()).cursorRatioX);
        int dy = (int) (Platform.cursorY * ((GameView)getParent()).cursorRatioY);
        canvas.translate(dx, dy);
        PlatformCursor cursor = Platform.getCursor();
        canvas.scale(mouseScale, mouseScale);
        if (cursor == null) {
            cursorDrawable.draw(canvas);
        } else {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX, -cursor.hotY, customCursorPaint);
        }
    }

    @Override
    public void onCursorPosition() {
        if (!noDraw) post(this::invalidate);
    }

    @Override
    public void onCursorChanged() {
        post(() -> {
            invalidate();
            updatePointerIcon();
        });
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        noDraw = isGrabbing;
        post(() -> {
            invalidate();
            updatePointerIcon();
        });
    }

    public void updatePointerIcon() {
        if (noDraw) {
            setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_NULL));
            if (getParent() instanceof View) {
                ((View) getParent()).setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_NULL));
            }
            return;
        }
        PlatformCursor cursor = Platform.getCursor();
        PointerIcon icon;
        if (cursor != null && cursor.bitmap != null && !cursor.bitmap.isRecycled()) {
            int hX = Math.max(0, Math.min(cursor.hotX, cursor.bitmap.getWidth() - 1));
            int hY = Math.max(0, Math.min(cursor.hotY, cursor.bitmap.getHeight() - 1));
            icon = PointerIcon.create(cursor.bitmap, hX, hY);
        } else {
            icon = PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW);
        }
        setPointerIcon(icon);
        if (getParent() instanceof View) {
            ((View) getParent()).setPointerIcon(icon);
        }
    }

    public void setCursorScale(float scale) {
        this.mouseScale = scale;
    }

    public void drawCursorToCanvas(Canvas canvas, float scaleX, float scaleY) {
        if (noDraw) return;
        int saveCount = canvas.save();
        float x = (float) (Platform.cursorX * scaleX);
        float y = (float) (Platform.cursorY * scaleY);
        canvas.translate(x, y);
        canvas.scale(mouseScale * scaleX, mouseScale * scaleY);
        PlatformCursor cursor = Platform.getCursor();
        if (cursor == null) {
            cursorDrawable.draw(canvas);
        } else {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX, -cursor.hotY, customCursorPaint);
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    public Context getImplementorContext() {
        return getContext();
    }
}
