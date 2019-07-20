package com.appacoustic.cointester;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;

public class AxisImageView extends AppCompatImageView {

    private final int blockSize;
    private final int width;
    private Paint paint;
    private Bitmap bitmap;
    private Canvas axisCanvas;

    private int textSize;
    private int yOffset;

    public AxisImageView(Context context, int blockSize, int width, int magnitudeTextSize, int paintColor) {
        super(context);
        this.blockSize = blockSize;
        this.width = width;

        int spaceToDraw = 100;
        textSize = magnitudeTextSize - 8;
        yOffset = textSize + 10;

        paint = new Paint();
        paint.setColor(paintColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(textSize);

        bitmap = Bitmap.createBitmap(width, yOffset + spaceToDraw, Bitmap.Config.ARGB_8888);
        axisCanvas = new Canvas(bitmap);
        setImageBitmap(bitmap);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int bigChunk = blockSize / 2;
        int smallChunk = blockSize / 16;
        int heightMinorTicks = 10;
        int heightMajorTicks = 15;
        axisCanvas.drawLine(0, yOffset, width, yOffset, paint); // Axis
        for (int i = 0, j = 0; i < width; i = i + bigChunk, j++) {
            for (int k = i; k < i + bigChunk; k = k + smallChunk) {
                axisCanvas.drawLine(k, yOffset, k, yOffset - heightMinorTicks, paint); // MinorTicks
            }
            axisCanvas.drawLine(i, yOffset + heightMajorTicks, i, yOffset, paint); // MajorTicks
            if (j % 2 == 0) axisCanvas.drawText(String.valueOf(j / 2), i, yOffset * 2, paint);
            if (j == 7) axisCanvas.drawText("(kHz)", i, yOffset * 2, paint);
        }
    }
}