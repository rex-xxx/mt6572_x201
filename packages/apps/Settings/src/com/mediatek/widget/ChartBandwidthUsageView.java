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
import android.content.res.Resources;
import android.net.NetworkStats;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.android.internal.util.Objects;
import com.android.settings.R;
import com.android.settings.widget.ChartAxis;
import com.android.settings.widget.InvertedChartAxis;
import com.mediatek.xlog.Xlog;
/**
 * Specific {@link ChartView} that displays {@link ChartBandwidthNetworkSeriesView} along
 * with {@link ChartSweepView} for inspection ranges and warning/limits.
 */
public class ChartBandwidthUsageView extends ChartView {
    private static final String TAG = "ChartBandwidthUsageView";

    public static final long KB_IN_BYTES = 1024;
    public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    public static final long GB_IN_BYTES = MB_IN_BYTES * 1024;
    private static final long MAX_SIZE = 10 * MB_IN_BYTES;
    private static final int MSG_UPDATE_AXIS = 100;
    private static final int DELAY_MILLIS = 250;

    private ChartBandwidthGridView mGrid;
    private ChartBandwidthNetworkSeriesView mSeries;
    private ChartSweepView mSweepLimit;

    /** Current maximum value of {@link #mVert}. */
    private long mVertMax;
    private Handler mHandler;

    public interface BandwidthChartListener {
        void onLimitChanging();
        void onLimitChanged();
        void requestLimitEdit();
    }

    private BandwidthChartListener mListener;

    public ChartBandwidthUsageView(Context context) {
        this(context, null, 0);
    }

    public ChartBandwidthUsageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartBandwidthUsageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(new TimeAxis(), new InvertedChartAxis(new BandwidthAxis()));
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final ChartSweepView sweep = (ChartSweepView) msg.obj;
                updateVertAxisBounds(sweep);

                // we keep dispatching repeating updates until sweep is dropped
                sendUpdateAxisDelayed(sweep, true);
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        Xlog.d(TAG,"onFinishInflate");
        super.onFinishInflate();

        mGrid = (ChartBandwidthGridView) findViewById(R.id.grid);
        mSeries = (ChartBandwidthNetworkSeriesView) findViewById(R.id.series);

        mSweepLimit = (ChartSweepView) findViewById(R.id.sweep_limit);
        mSweepLimit.setMaxValue(MAX_SIZE);
        mSweepLimit.addOnSweepListener(mVertListener);
        mSweepLimit.setDragInterval(2 * KB_IN_BYTES);

        // tell everyone about our axis
        mGrid.init(mHoriz, mVert);
        mSeries.init(mHoriz, mVert);
        mSweepLimit.init(mVert);
        mSweepLimit.setEnabled(true);
        updateVertAxisBounds(mSweepLimit);

    }

    public void setListener(BandwidthChartListener listener) {
        mListener = listener;
    }

    private ChartSweepView.OnSweepListener mVertListener = new ChartSweepView.OnSweepListener() {
        /** {@inheritDoc} */
        @Override
        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            if (sweepDone) {
                clearUpdateAxisDelayed(sweep);
                if (sweep == mSweepLimit && mListener != null) {
                    mListener.onLimitChanged();
                }
            } else {
                // while moving, kick off delayed grow/shrink axis updates
                sendUpdateAxisDelayed(sweep, false);
                mListener.onLimitChanging();
            }
        }
        /** {@inheritDoc} */
        @Override
        public void requestEdit(ChartSweepView sweep) {
            if (sweep == mSweepLimit && mListener != null) {
                mListener.requestLimitEdit();
            }
        }
    };
    private void sendUpdateAxisDelayed(ChartSweepView sweep, boolean force) {
        if (force || !mHandler.hasMessages(MSG_UPDATE_AXIS, sweep)) {
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(MSG_UPDATE_AXIS, sweep), DELAY_MILLIS);
        }
    }

    private void clearUpdateAxisDelayed(ChartSweepView sweep) {
        mHandler.removeMessages(MSG_UPDATE_AXIS, sweep);
    }

    public long getLimitBytes() {
        return mSweepLimit.getLabelValue();
    }

    public void setLimitBytes(long value) {
        mSweepLimit.setValue(value);
    }

    public void setLimitState(boolean state) {
        mSweepLimit.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    }

    public long getTotalUsedData() {
        return mSeries.getTotalUsedData();
    }

    /**
     * Update {@link #mVert} to both show data from {@link NetworkStatsHistory}
     * and controls from {@link NetworkPolicy}.
     */
    public void updateVertAxisBounds(ChartSweepView activeSweep) {
        final long max = mVertMax;

        long newMax = 0;
        if (activeSweep != null) {
            final int adjustAxis = activeSweep.shouldAdjustAxis();
            if (adjustAxis > 0) {
                // hovering around upper edge, grow axis
                newMax = max * 11 / 10;
            } else if (adjustAxis < 0) {
                // hovering around lower edge, shrink axis
                newMax = max * 9 / 10;
            } else {
                newMax = max;
            }
        }

        // always show known data and policy lines
        final long maxVisible = mSeries.getMaxBytes() * 12 / 10;
        final long maxDefault = Math.max(maxVisible, 512 * KB_IN_BYTES);
        final long maxValue = Math.max(maxDefault, mSweepLimit.getValue() * 11 / 10);
        newMax = Math.max(maxValue, newMax);
        newMax = newMax > MAX_SIZE ? MAX_SIZE : newMax;

        // only invalidate when vertMax actually changed
        if (newMax != mVertMax) {
            mVertMax = newMax;

            final boolean changed = mVert.setBounds(0L, newMax);
            mSweepLimit.setValidRange(0L, newMax);

            if (changed) {
                mSeries.invalidatePath();
            }

            mGrid.invalidate();

            // since we just changed axis, make sweep recalculate its value
            if (activeSweep != null) {
                activeSweep.updateValueFromPosition();
            }

            // layout other sweeps to match changed axis
            // TODO: find cleaner way of doing this, such as requesting full
            // layout and making activeSweep discard its tracking MotionEvent.
            if (mSweepLimit != activeSweep) {
                layoutSweep(mSweepLimit);
            }
        }
    }


    public void setNetworkStates(NetworkStats networkStats) {
        mSeries.setNetworkStates(networkStats);
    }

    public class TimeAxis implements ChartAxis {
        private static final long TICK_INTERVAL = 5;
        private long mMin;
        private long mMax;
        private float mSize;

        public TimeAxis() {
            setBounds(0, 100);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mMin, mMax, mSize);
        }

        /** {@inheritDoc} */
        @Override
        public boolean setBounds(long min, long max) {
            if (mMin != min || mMax != max) {
                mMin = min;
                mMax = max;
                return true;
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean setSize(float size) {
            if (mSize != size) {
                mSize = size;
                return true;
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public float convertToPoint(long value) {
            return (mSize * (value - mMin)) / (mMax - mMin);
        }

        /** {@inheritDoc} */
        @Override
        public long convertToValue(float point) {
            return (long) (mMin + ((point * (mMax - mMin)) / mSize));
        }

        /** {@inheritDoc} */
        @Override
        public long buildLabel(Resources res, SpannableStringBuilder builder, long value) {
            // TODO: convert to better string
            builder.replace(0, builder.length(), Long.toString(value));
            return value;
        }

        /** {@inheritDoc} */
        @Override
        public float[] getTickPoints() {
            // tick mark for every week
            final int tickCount = (int) ((mMax - mMin) / TICK_INTERVAL);
            final float[] tickPoints = new float[tickCount];
            for (int i = 0; i < tickCount; i++) {
                tickPoints[i] = convertToPoint(mMax - (TICK_INTERVAL * (i + 1)));
            }
            return tickPoints;
        }

        /** {@inheritDoc} */
        @Override
        public int shouldAdjustAxis(long value) {
            // time axis never adjusts
            return 0;
        }
    }
    public static class BandwidthAxis implements ChartAxis {
        private long mMin;
        private long mMax;
        private float mSize;

        public BandwidthAxis() {
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mMin, mMax, mSize);
        }

        /** {@inheritDoc} */
        @Override
        public boolean setBounds(long min, long max) {
            if (mMin != min || mMax != max) {
                mMin = min;
                mMax = max;
                return true;
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean setSize(float size) {
            if (mSize != size) {
                mSize = size;
                return true;
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public float convertToPoint(long value) {
            // derived polynomial fit to make lower values more visible
            final double normalized = ((double) value - mMin) / (mMax - mMin);
            final double fraction = Math.pow(
                    10, 0.36884343106175121463 * Math.log10(normalized) + -0.04328199452018252624);
            return (float) (fraction * mSize);
        }

        /** {@inheritDoc} */
        @Override
        public long convertToValue(float point) {
            final double normalized = point / mSize;
            final double fraction = 1.3102228476089056629
                    * Math.pow(normalized, 2.7111774693164631640);
            return (long) (mMin + (fraction * (mMax - mMin)));
        }

        private static final Object SPAN_SIZE = new Object();
        private static final Object SPAN_UNIT = new Object();

        /** {@inheritDoc} */
        @Override
        public long buildLabel(Resources res, SpannableStringBuilder builder, long value) {

            final CharSequence unit;
            final long unitFactor;

            if (value < 1 * MB_IN_BYTES) {
                unit = res.getText(R.string.wifi_ap_bandwidth_KbyteShort);
                unitFactor = KB_IN_BYTES;
            } else {
                unit = res.getText(R.string.wifi_ap_bandwidth_megabyteShort);
                unitFactor = MB_IN_BYTES;
            }

            final double result = (double) value / unitFactor;
            final double resultRounded;
            final CharSequence size;

            if (value > 1 * MB_IN_BYTES && result < 10) {
                size = String.format("%.1f", result);
                resultRounded = (unitFactor * Math.round(result * 10)) / 10;
            } else {
                size = String.format("%.0f", result);
                resultRounded = unitFactor * Math.round(result);
            }

            final int[] sizeBounds = findOrCreateSpan(builder, SPAN_SIZE, "^1");
            builder.replace(sizeBounds[0], sizeBounds[1], size);
            final int[] unitBounds = findOrCreateSpan(builder, SPAN_UNIT, "^2");
            builder.replace(unitBounds[0], unitBounds[1], unit);

            return (long) resultRounded;
        }

        /** {@inheritDoc} */
        @Override
        public float[] getTickPoints() {
            final long range = mMax - mMin;
            final long tickJump;
            if (range < 3 * MB_IN_BYTES) {
                tickJump = 64 * KB_IN_BYTES;
            } else if (range < 6 * MB_IN_BYTES) {
                tickJump = 128 * KB_IN_BYTES;
            } else {
                tickJump = 256 * KB_IN_BYTES;
            }

            final int tickCount = (int) (range / tickJump);
            final float[] tickPoints = new float[tickCount];
            long value = mMin;
            for (int i = 0; i < tickPoints.length; i++) {
                tickPoints[i] = convertToPoint(value);
                value += tickJump;
            }

            return tickPoints;
        }

        /** {@inheritDoc} */
        @Override
        public int shouldAdjustAxis(long value) {
            final float point = convertToPoint(value);
            if (point < mSize * 0.5) {
                return -1;
            } else if (point > mSize * 0.85) {
                return 1;
            } else {
                return 0;
            }
        }

        private int[] findOrCreateSpan(
                SpannableStringBuilder builder, Object key, CharSequence bootstrap) {
            int start = builder.getSpanStart(key);
            int end = builder.getSpanEnd(key);
            if (start == -1) {
                start = TextUtils.indexOf(builder, bootstrap);
                end = start + bootstrap.length();
                builder.setSpan(key, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            return new int[] { start, end };
        }
    }
}
