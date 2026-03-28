package com.brouken.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.media3.ui.DefaultTimeBar;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomDefaultTimeBar extends DefaultTimeBar {

    private Rect scrubberBar;
    private Rect progressBar;
    private boolean scrubbing;
    private int scrubbingStartX;

    private long forwardMs = 0;
    private long durationMs = 0;
    private Paint textPaint;

    public CustomDefaultTimeBar(Context context) {
        this(context, null);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet timebarAttrs) {
        this(context, attrs, defStyleAttr, timebarAttrs, 0);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet timebarAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttr, timebarAttrs, defStyleRes);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Utils.dpToPx(12)); // Adjust size if needed
        textPaint.setAntiAlias(true);

        try {
            Field sField = DefaultTimeBar.class.getDeclaredField("scrubberBar");
            sField.setAccessible(true);
            scrubberBar = (Rect) sField.get(this);

            Field pField = DefaultTimeBar.class.getDeclaredField("progressBar");
            pField.setAccessible(true);
            progressBar = (Rect) pField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Call this from PlayerActivity to update the numbers
    public void setBufferInfo(long forwardMs, long durationMs) {
        this.forwardMs = forwardMs;
        this.durationMs = durationMs;
        invalidate(); // Force a redraw
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas); // Draw the standard ExoPlayer bar first

        if (durationMs <= 0 || progressBar == null || scrubberBar == null) return;

        // 2. Draw Forward buffer text
        if (forwardMs > 0) {
            int textY = progressBar.top - Utils.dpToPx(8);
            String forwardText = "+" + (forwardMs / 1000) + "s";
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(forwardText, scrubberBar.right + Utils.dpToPx(4), textY, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && scrubberBar != null) {
            scrubbing = false;
            scrubbingStartX = (int)event.getX();
            final int distanceFromScrubber = Math.abs(scrubberBar.right - scrubbingStartX);
            if (distanceFromScrubber > Utils.dpToPx(24))
                return true;
            else
                scrubbing = true;
        }
        if (!scrubbing && event.getAction() == MotionEvent.ACTION_MOVE && scrubberBar != null) {
            final int distanceFromStart = Math.abs(((int)event.getX()) - scrubbingStartX);
            if (distanceFromStart > Utils.dpToPx(6)) {
                scrubbing = true;
                try {
                    final Method method = DefaultTimeBar.class.getDeclaredMethod("startScrubbing", long.class);
                    method.setAccessible(true);
                    method.invoke(this, (long) 0);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}