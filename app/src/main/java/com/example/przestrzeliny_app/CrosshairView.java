package com.example.przestrzeliny_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CrosshairView extends View {
    private Paint paint;

    public CrosshairView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED); // Kolor celownika
        paint.setStrokeWidth(4f);  // Grubość linii
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);  // Wygładzanie krawędzi
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int lineSize = 70; // Długość ramion celownika


        canvas.drawLine(centerX - lineSize, centerY, centerX + lineSize, centerY, paint);


        canvas.drawLine(centerX, centerY - lineSize, centerX, centerY + lineSize, paint);


        canvas.drawCircle(centerX, centerY, 10, paint);
    }
}