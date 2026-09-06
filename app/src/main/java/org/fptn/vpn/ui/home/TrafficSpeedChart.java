/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class TrafficSpeedChart extends View {

    private static final int BUFFER_SIZE = 300;
    // Show at least 1 Mbps range so the chart never looks "too spiky"
    private static final float MIN_MAX_MBPS = 0.001f;

    private final float[] downloadSamples = new float[BUFFER_SIZE];
    private final float[] uploadSamples = new float[BUFFER_SIZE];
    private int head = 0;
    private int count = 0;

    private final Paint downloadLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint downloadFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uploadLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uploadFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final Path path = new Path();
    private float paddingPx;

    public TrafficSpeedChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        paddingPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());

        downloadLinePaint.setColor(0xFF4FC87A);
        downloadLinePaint.setStyle(Paint.Style.STROKE);
        downloadLinePaint.setStrokeWidth(2f);

        downloadFillPaint.setColor(0x334FC87A);
        downloadFillPaint.setStyle(Paint.Style.FILL);

        uploadLinePaint.setColor(0xFF7AADFF);
        uploadLinePaint.setStyle(Paint.Style.STROKE);
        uploadLinePaint.setStrokeWidth(2f);

        uploadFillPaint.setColor(0x337AADFF);
        uploadFillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(0x14FFFFFF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
    }

    public void addSample(long downloadBps, long uploadBps) {
        downloadSamples[head] = downloadBps / 125_000f; // bytes/s → Mbps
        uploadSamples[head] = uploadBps / 125_000f;
        head = (head + 1) % BUFFER_SIZE;
        if (count < BUFFER_SIZE) count++;
        postInvalidate();
    }

    public void reset() {
        head = 0;
        count = 0;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        // Draw subtle horizontal grid lines (25%, 50%, 75%)
        float[] gridY = {h * 0.25f, h * 0.5f, h * 0.75f};
        for (float gy : gridY) {
            canvas.drawLine(0, gy, w, gy, gridPaint);
        }

        if (count < 2) return;

        float maxVal = MIN_MAX_MBPS;
        for (int i = 0; i < count; i++) {
            int idx = (head - count + i + BUFFER_SIZE) % BUFFER_SIZE;
            if (downloadSamples[idx] > maxVal) maxVal = downloadSamples[idx];
            if (uploadSamples[idx] > maxVal) maxVal = uploadSamples[idx];
        }
        maxVal *= 1.25f;

        float drawH = h - paddingPx;

        drawSeries(canvas, downloadSamples, w, h, drawH, maxVal, downloadLinePaint, downloadFillPaint);
        drawSeries(canvas, uploadSamples, w, h, drawH, maxVal, uploadLinePaint, uploadFillPaint);
    }

    private void drawSeries(Canvas canvas, float[] samples, int w, int h, float drawH,
                            float maxVal, Paint linePaint, Paint fillPaint) {
        float step = (float) w / Math.max(count - 1, 1);
        float startX = 0;

        path.reset();
        boolean moved = false;
        for (int i = 0; i < count; i++) {
            int idx = (head - count + i + BUFFER_SIZE) % BUFFER_SIZE;
            float x = startX + i * step;
            float y = h - paddingPx - (samples[idx] / maxVal) * drawH;
            if (!moved) {
                path.moveTo(x, y);
                moved = true;
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);

        // Close fill to bottom
        path.lineTo(startX + (count - 1) * step, h);
        path.lineTo(startX, h);
        path.close();
        canvas.drawPath(path, fillPaint);
    }
}
