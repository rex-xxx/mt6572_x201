/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.android.settings.R;
import com.android.settings.widget.ChartAxis;
import com.google.common.base.Preconditions;
/**
 * Background of {@link ChartView} that renders grid lines as requested by
 * {@link ChartAxis#getTickPoints()}.
 */
public class ChartBandwidthGridView extends View {
    public static final String TAG = "ChartBandwidthGridView";
    private ChartAxis mHoriz;
    private ChartAxis mVert;

    private Drawable mSecondary;

    public ChartBandwidthGridView(Context context) {
        this(context, null, 0);
    }

    public ChartBandwidthGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartBandwidthGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ChartGridView, defStyle, 0);

        mSecondary = a.getDrawable(R.styleable.ChartGridView_primaryDrawable);

        a.recycle();
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");
    }

    void setBounds(long start, long end) {
        final Context context = getContext();
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();

        Paint p = new Paint();
        p.setColor(0xFF669900);
        p.setStyle(Paint.Style.STROKE);

        /*final float[] vertTicks = mVert.getTickPoints();
        for (float y : vertTicks) {
            if(y < 320){
                canvas.drawLine(0, (float)y, (float)width, (float)y, p);
                Xlog.d(TAG,"y="+y);
            }
        }*/

        final Drawable secondary = mSecondary;
        final int secondaryHeight = mSecondary.getIntrinsicHeight();

        final float[] vertTicks = mVert.getTickPoints();
        for (float y : vertTicks) {
            final int bottom = (int) Math.min(y + secondaryHeight, height);
            secondary.setBounds(0, (int) y, width, bottom);
            secondary.draw(canvas);
        }

        final float[] horizTicks = mHoriz.getTickPoints();
        for (float x : horizTicks) {
            canvas.drawLine((float)x, 0, (float)x, (float)height, p);
        }
        canvas.drawLine(mHoriz.convertToPoint(100), 0, mHoriz.convertToPoint(100), (float)height, p);

        Paint grayPaint = new Paint();
        grayPaint.setColor(0xFF696969);
        canvas.drawLine(0, 0, (float)width, 0, grayPaint);

    }

}
