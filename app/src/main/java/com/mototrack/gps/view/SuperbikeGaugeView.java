package com.mototrack.gps.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class SuperbikeGaugeView extends View {

    private Paint arcPaint;
    private Paint progressPaint;
    private Paint tickPaint;
    private Paint needlePaint;
    private Paint hubPaint;
    private Paint textSpeedPaint;
    private Paint textUnitPaint;
    private RectF arcBounds;

    private float currentSpeedKmh = 0f;
    private float animatedSpeedKmh = 0f;
    private static final float MAX_SPEED = 260f;
    private static final float START_ANGLE = 150f;
    private static final float SWEEP_ANGLE = 240f;

    private ValueAnimator needleAnimator;

    public SuperbikeGaugeView(Context context) {
        super(context);
        init();
    }

    public SuperbikeGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SuperbikeGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(26f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(Color.parseColor("#1C2636"));

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(26f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeWidth(8f);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        needlePaint.setColor(Color.parseColor("#FF1744"));

        hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setStyle(Paint.Style.FILL);

        textSpeedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSpeedPaint.setColor(Color.WHITE);
        textSpeedPaint.setTextAlign(Paint.Align.CENTER);
        textSpeedPaint.setTextSize(96f);
        textSpeedPaint.setFakeBoldText(true);

        textUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textUnitPaint.setColor(Color.parseColor("#00E5FF"));
        textUnitPaint.setTextAlign(Paint.Align.CENTER);
        textUnitPaint.setTextSize(26f);
        textUnitPaint.setLetterSpacing(0.2f);
        textUnitPaint.setFakeBoldText(true);

        arcBounds = new RectF();
    }

    public void setSpeed(float speedKmh) {
        final float target = Math.max(0f, Math.min(speedKmh, MAX_SPEED));
        if (needleAnimator != null && needleAnimator.isRunning()) {
            needleAnimator.cancel();
        }
        needleAnimator = ValueAnimator.ofFloat(animatedSpeedKmh, target);
        needleAnimator.setDuration(280);
        needleAnimator.setInterpolator(new DecelerateInterpolator());
        needleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedSpeedKmh = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        needleAnimator.start();
        currentSpeedKmh = target;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 40f;
        float size = Math.min(w, h) - (padding * 2f);
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        arcBounds.set(left, top, left + size, top + size);

        int[] colors = new int[]{
                Color.parseColor("#00E5FF"),
                Color.parseColor("#00E676"),
                Color.parseColor("#FFD600"),
                Color.parseColor("#FF1744"),
                Color.parseColor("#00E5FF")
        };
        float[] positions = new float[]{0.0f, 0.35f, 0.65f, 0.90f, 1.0f};
        SweepGradient gradient = new SweepGradient(w / 2f, h / 2f, colors, positions);
        progressPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = (arcBounds.width() / 2f);

        // 1. Background Arc
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, arcPaint);

        // 2. Active Progress Arc
        float progressSweep = (animatedSpeedKmh / MAX_SPEED) * SWEEP_ANGLE;
        if (progressSweep > 0.5f) {
            canvas.drawArc(arcBounds, START_ANGLE, progressSweep, false, progressPaint);
        }

        // 3. Ticks
        for (int i = 0; i <= 13; i++) {
            float tickValue = i * 20f;
            float angleDeg = START_ANGLE + (tickValue / MAX_SPEED) * SWEEP_ANGLE;
            double angleRad = Math.toRadians(angleDeg);
            boolean isMajor = (i % 2 == 0);

            float innerR = radius - (isMajor ? 28f : 16f);
            float outerR = radius - 8f;

            float x1 = (float) (cx + innerR * Math.cos(angleRad));
            float y1 = (float) (cy + innerR * Math.sin(angleRad));
            float x2 = (float) (cx + outerR * Math.cos(angleRad));
            float y2 = (float) (cy + outerR * Math.sin(angleRad));

            tickPaint.setStrokeWidth(isMajor ? 5f : 2.5f);
            tickPaint.setColor(tickValue <= animatedSpeedKmh ? Color.parseColor("#00E5FF") : Color.parseColor("#455A64"));
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // 4. Sweeping Needle
        float needleAngleDeg = START_ANGLE + (animatedSpeedKmh / MAX_SPEED) * SWEEP_ANGLE;
        double needleRad = Math.toRadians(needleAngleDeg);
        float needleLength = radius - 35f;

        float tipX = (float) (cx + needleLength * Math.cos(needleRad));
        float tipY = (float) (cy + needleLength * Math.sin(needleRad));

        canvas.drawLine(cx, cy, tipX, tipY, needlePaint);

        // 5. Pivot Hub
        hubPaint.setColor(Color.parseColor("#FF1744"));
        canvas.drawCircle(cx, cy, 18f, hubPaint);
        hubPaint.setColor(Color.parseColor("#0A0E17"));
        canvas.drawCircle(cx, cy, 8f, hubPaint);

        // 6. Central Speedometer Digital Readout
        int displaySpeed = Math.round(animatedSpeedKmh);
        canvas.drawText(String.valueOf(displaySpeed), cx, cy + 90f, textSpeedPaint);
        canvas.drawText("KM / H", cx, cy + 130f, textUnitPaint);
    }
}
