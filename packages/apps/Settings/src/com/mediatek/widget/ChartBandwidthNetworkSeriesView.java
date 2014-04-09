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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.net.NetworkStats;
import android.util.AttributeSet;
import android.view.View;

import com.android.settings.R;
import com.android.settings.widget.ChartAxis;
import com.google.common.base.Preconditions;
import com.mediatek.xlog.Xlog;
/**
 * {@link NetworkStats} series to render inside a {@link ChartView},
 * using {@link ChartAxis} to map into screen coordinates.
 */
public class ChartBandwidthNetworkSeriesView extends View {
    private static final String TAG = "ChartBandwidthNetworkSeriesView";
    private static final int TOTAL_LEN = 90;
    private static final long MB_IN_BYTES = 1024 * 1024;
    private ChartAxis mHoriz;
    private ChartAxis mVert;

    private Paint mPaintStroke;

    private Path mPathStroke;

    private long mStart;
    private long mEnd;

    private long mLeftBound;

    private NetworkStats mStats;
    private long [] mCurrentBytes;
    private int mCurrentLen;
    private long mTotalUsed;

    /** Series will be extended to reach this end time. */
    private long mEndTime = Long.MIN_VALUE;

    private boolean mPathValid = false;

    private long mMax;

    public ChartBandwidthNetworkSeriesView(Context context) {
        this(context, null, 0);
    }

    public ChartBandwidthNetworkSeriesView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartBandwidthNetworkSeriesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ChartNetworkSeriesView, defStyle, 0);
        final int stroke = a.getColor(R.styleable.ChartNetworkSeriesView_strokeColor, Color.RED);
        setSeriesColor(stroke);
        setWillNotDraw(false);
        a.recycle();
        mPathStroke = new Path();
        mCurrentBytes = new long[TOTAL_LEN];
        mCurrentLen = 0;
    }

    public void setSeriesColor(int stroke) {
        mPaintStroke = new Paint();
        mPaintStroke.setStrokeWidth(3);
        mPaintStroke.setColor(stroke);
        mPaintStroke.setStyle(Style.STROKE);
        mPaintStroke.setAntiAlias(true);
    }
    void init(ChartAxis horiz, ChartAxis vert) {
        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");
    }

    public long getMaxBytes() {
        return mMax > MB_IN_BYTES ? mMax : MB_IN_BYTES;
    }

    public long getTotalUsedData() {
        return mTotalUsed;
    }

    public void setNetworkStates(NetworkStats networkStats) {
        mStats = networkStats;
        generatePath();
    }
    public void invalidatePath() {
        mPathValid = false;
        mMax = 0;
        invalidate();
    }
    /**
     * Erase any existing {@link Path} and generate series outline based on
     * currently bound {@link NetworkStats} data.
     */
    public void generatePath() {
        long range = getMaxBytes();
        mMax = 0;
        mPathStroke.reset();

        // bail when not enough stats to render
        if (mStats == null || mStats.size() < 1) {
            return;
        }

        mPathValid = true;
        long totalData = 0;
        long currentData = 0;

        NetworkStats.Entry entry = null;
        entry = mStats.getValues(0, entry);
        totalData = entry.rxBytes + entry.txBytes;

        currentData = mTotalUsed == 0 ? 0 : totalData - mTotalUsed;
        mTotalUsed = totalData;
        Xlog.d(TAG, "currentData = " + currentData);

        if (mCurrentLen < 90) {
            mCurrentBytes[mCurrentLen] = currentData;
            mCurrentLen++;
        } else {
            System.arraycopy(mCurrentBytes, 1, mCurrentBytes, 0, 89);
            mCurrentBytes[89] = currentData;
        }

        mPathStroke.moveTo(mHoriz.convertToPoint(100 - mCurrentLen + 1), 
                mVert.convertToPoint(mCurrentBytes[0] / range * 100));

        for (int i = 0; i < mCurrentLen; i++) {
            mPathStroke.lineTo(mHoriz.convertToPoint(100 - mCurrentLen + 1 + i), 
                    mVert.convertToPoint((long)mCurrentBytes[i]));
            mMax = mMax < mCurrentBytes[i] ? mCurrentBytes[i] : mMax;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int save;

        if (!mPathValid) {
            generatePath();
        }

        final float primaryLeftPoint = mHoriz.convertToPoint(0);
        final float primaryRightPoint = mHoriz.convertToPoint(100);

        save = canvas.save();
        canvas.clipRect(primaryLeftPoint, 0, primaryRightPoint, getHeight());
        canvas.drawPath(mPathStroke, mPaintStroke);
        canvas.restoreToCount(save);
    }
}
