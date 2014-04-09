/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.systemui;

import com.android.internal.os.ProcessStats;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.TrafficStats;
import android.util.Log;

public class LoadAverageService extends Service {
    private View mView;

    private static final class Stats extends ProcessStats {
        String mLoadText;
        int mLoadWidth;

        private final Paint mPaint;

        Stats(Paint paint) {
            super(false);
            mPaint = paint;
        }

        @Override
        public void onLoadChanged(float load1, float load5, float load15) {
            mLoadText = load1 + " / " + load5 + " / " + load15;
            mLoadWidth = (int)mPaint.measureText(mLoadText);
        }

        @Override
        public int onMeasureProcessName(String name) {
            return (int)mPaint.measureText(name);
        }
    }

    private class LoadView extends View {
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if (0 == mThermalIndicatorMode)
                    {
                        mStats.update();
                    }
                    updateThermalInfo();
                    updateDisplay();
                    Message m = obtainMessage(1);

                    if (mThermalIndicatorMode > 0 && mThermalIndicatorDelay >= 2000)
                    {
                        sendMessageDelayed(m, mThermalIndicatorDelay);
                    }else if (mThermalIndicatorMode == 99 && mThermalIndicatorDelay >= 500)
                    {
                        sendMessageDelayed(m, mThermalIndicatorDelay);
                    }else
                    {
                        sendMessageDelayed(m, 2000);
                    }
                }
            }
        };

        private final Stats mStats;

        private Paint mLoadPaint;
        private Paint mAddedPaint;
        private Paint mRemovedPaint;
        private Paint mShadowPaint;
        private Paint mShadow2Paint;
        private Paint mIrqPaint;
        private Paint mSystemPaint;
        private Paint mUserPaint;
        private float mAscent;
        private int mFH;

        private int mNeededWidth;
        private int mNeededHeight;

        private long mPreTxBytes = 0;
        private long mPreTxTime = 0;

        private String mThermalText;
        private String mThermalTextExtra;
        private String mThermalTextLineThree;
        private int mThermalWidth;
        private int mThermalWidthExtra;
        private int mThermalWidthLineThree;
        private int mThermalIndicatorMode = 0;
        private int mThermalIndicatorDelay = 0;

        private int mStorageIndicatorMode = 0;
        private int mStorageIndicatorDelay = 0;

        LoadView(Context c) {
            super(c);

            setPadding(4, 4, 4, 4);
            //setBackgroundResource(com.android.internal.R.drawable.load_average_background);

            // Need to scale text size by density...  but we won't do it
            // linearly, because with higher dps it is nice to squeeze the
            // text a bit to fit more of it.  And with lower dps, trying to
            // go much smaller will result in unreadable text.
            int textSize = 10;
            float density = c.getResources().getDisplayMetrics().density;
            if (density < 1) {
                textSize = 9;
            } else {
                textSize = (int)(10*density);
                if (textSize < 10) {
                    textSize = 10;
                }
            }
            mLoadPaint = new Paint();
            mLoadPaint.setAntiAlias(true);
            mLoadPaint.setTextSize(textSize);
            mLoadPaint.setARGB(255, 255, 255, 255);

            mAddedPaint = new Paint();
            mAddedPaint.setAntiAlias(true);
            mAddedPaint.setTextSize(textSize);
            mAddedPaint.setARGB(255, 128, 255, 128);

            mRemovedPaint = new Paint();
            mRemovedPaint.setAntiAlias(true);
            mRemovedPaint.setStrikeThruText(true);
            mRemovedPaint.setTextSize(textSize);
            mRemovedPaint.setARGB(255, 255, 128, 128);

            mShadowPaint = new Paint();
            mShadowPaint.setAntiAlias(true);
            mShadowPaint.setTextSize(textSize);
            //mShadowPaint.setFakeBoldText(true);
            mShadowPaint.setARGB(192, 0, 0, 0);
            mLoadPaint.setShadowLayer(4, 0, 0, 0xff000000);

            mShadow2Paint = new Paint();
            mShadow2Paint.setAntiAlias(true);
            mShadow2Paint.setTextSize(textSize);
            //mShadow2Paint.setFakeBoldText(true);
            mShadow2Paint.setARGB(192, 0, 0, 0);
            mLoadPaint.setShadowLayer(2, 0, 0, 0xff000000);

            mIrqPaint = new Paint();
            mIrqPaint.setARGB(0x80, 0, 0, 0xff);
            mIrqPaint.setShadowLayer(2, 0, 0, 0xff000000);
            mSystemPaint = new Paint();
            mSystemPaint.setARGB(0x80, 0xff, 0, 0);
            mSystemPaint.setShadowLayer(2, 0, 0, 0xff000000);
            mUserPaint = new Paint();
            mUserPaint.setARGB(0x80, 0, 0xff, 0);
            mSystemPaint.setShadowLayer(2, 0, 0, 0xff000000);

            mAscent = mLoadPaint.ascent();
            float descent = mLoadPaint.descent();
            mFH = (int)(descent - mAscent + .5f);

            mStats = new Stats(mLoadPaint);
            mStats.init();
            updateDisplay();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mHandler.sendEmptyMessage(1);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mHandler.removeMessages(1);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(resolveSize(mNeededWidth, widthMeasureSpec),
                    resolveSize(mNeededHeight, heightMeasureSpec));
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final int W = mNeededWidth;
            final int RIGHT = getWidth()-1;

            final Stats stats = mStats;
            final int userTime = stats.getLastUserTime();
            final int systemTime = stats.getLastSystemTime();
            final int iowaitTime = stats.getLastIoWaitTime();
            final int irqTime = stats.getLastIrqTime();
            final int softIrqTime = stats.getLastSoftIrqTime();
            final int idleTime = stats.getLastIdleTime();

            final int totalTime = userTime+systemTime+iowaitTime+irqTime+softIrqTime+idleTime;
            if (totalTime == 0) {
                return;
            }
            int userW = (userTime*W)/totalTime;
            int systemW = (systemTime*W)/totalTime;
            int irqW = ((iowaitTime+irqTime+softIrqTime)*W)/totalTime;

            int x = RIGHT - mPaddingRight;
            int top = mPaddingTop + 2;
            int bottom = mPaddingTop + mFH - 2;
            int y = mPaddingTop - (int)mAscent;

            if ((1 == mThermalIndicatorMode) || (2 == mThermalIndicatorMode) || (3 == mThermalIndicatorMode))
            {
                /* add for thermal info*/
                //updateThermalInfo();

                canvas.drawText(mThermalText, RIGHT-mPaddingRight-mThermalWidth,
                    y, mLoadPaint);

                x = RIGHT - mPaddingRight;
                y += mFH;
                top += mFH;
                bottom += mFH;
            }

            if ((2 == mThermalIndicatorMode) || (3 == mThermalIndicatorMode))
            {
                canvas.drawText(mThermalTextExtra, RIGHT-mPaddingRight-mThermalWidthExtra,
                    y, mLoadPaint);

                x = RIGHT - mPaddingRight;
                y += mFH;
                top += mFH;
                bottom += mFH;
            }

            if (3 == mThermalIndicatorMode)
            {
                canvas.drawText(mThermalTextLineThree, RIGHT-mPaddingRight-mThermalWidthLineThree,
                    y, mLoadPaint);

                x = RIGHT - mPaddingRight;
                y += mFH;
                top += mFH;
                bottom += mFH;
            }

            if (99 == mThermalIndicatorMode)
            {
                /* add for thermal info*/
                //updateThermalInfo();

                canvas.drawText(mThermalText, RIGHT-mPaddingRight-mThermalWidth,
                    y, mLoadPaint);

                x = RIGHT - mPaddingRight;
                y += mFH;
                top += mFH;
                bottom += mFH;

                canvas.drawText(mThermalTextExtra, RIGHT-mPaddingRight-mThermalWidthExtra,
                    y, mLoadPaint);

                x = RIGHT - mPaddingRight;
                y += mFH;
                top += mFH;
                bottom += mFH;
            }

            if (0 == mThermalIndicatorMode)
            {
                if (irqW > 0) {
                    canvas.drawRect(x-irqW, top, x, bottom, mIrqPaint);
                    x -= irqW;
                }
                if (systemW > 0) {
                    canvas.drawRect(x-systemW, top, x, bottom, mSystemPaint);
                    x -= systemW;
                }
                if (userW > 0) {
                    canvas.drawRect(x-userW, top, x, bottom, mUserPaint);
                    x -= userW;
                }

                canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth-1,
                        y-1, mShadowPaint);
                canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth-1,
                        y+1, mShadowPaint);
                canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth+1,
                        y-1, mShadow2Paint);
                canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth+1,
                        y+1, mShadow2Paint);
                canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth,
                        y, mLoadPaint);
            }

            // If thermal indicator is turned on, skip stats.
            int N = 0;
            if (0 == mThermalIndicatorMode)
            {
                N = stats.countWorkingStats();
            }

            for (int i=0; i<N; i++) {
                Stats.Stats st = stats.getWorkingStats(i);
                y += mFH;
                top += mFH;
                bottom += mFH;

                userW = (st.rel_utime*W)/totalTime;
                systemW = (st.rel_stime*W)/totalTime;
                x = RIGHT - mPaddingRight;
                if (systemW > 0) {
                    canvas.drawRect(x-systemW, top, x, bottom, mSystemPaint);
                    x -= systemW;
                }
                if (userW > 0) {
                    canvas.drawRect(x-userW, top, x, bottom, mUserPaint);
                    x -= userW;
                }

                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth-1,
                        y-1, mShadowPaint);
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth-1,
                        y+1, mShadowPaint);
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth+1,
                        y-1, mShadow2Paint);
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth+1,
                        y+1, mShadow2Paint);
                Paint p = mLoadPaint;
                if (st.added) p = mAddedPaint;
                if (st.removed) p = mRemovedPaint;
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth, y, p);
            }
        }

        void updateDisplay() {
            final Stats stats = mStats;
            //final int NW = stats.countWorkingStats();
            int NW = 0;

            // If thermal indicator is turned on, skip stats.
            if (0 == mThermalIndicatorMode)
            {
                NW = stats.countWorkingStats();
            }

            int maxWidth = stats.mLoadWidth;
            for (int i=0; i<NW; i++) {
                Stats.Stats st = stats.getWorkingStats(i);
                if (st.nameWidth > maxWidth) {
                    maxWidth = st.nameWidth;
                }
            }

            int neededWidth = mPaddingLeft + mPaddingRight + maxWidth;
            int neededHeight = mPaddingTop + mPaddingBottom + (mFH*(1+mThermalIndicatorMode+NW)); // Add two newlines for thermal indicator
            if (neededWidth != mNeededWidth || neededHeight != mNeededHeight) {
                mNeededWidth = neededWidth;
                mNeededHeight = neededHeight;
                requestLayout();
            } else {
                invalidate();
            }
        }

        void updateThermalInfo()
        {
            getThermalIndicatorModeAndDelay();
            getStorageIndicatorModeAndDelay();
            if (0 == mThermalIndicatorMode) {
                mThermalText = "";
                mThermalTextExtra = "";
                mThermalTextLineThree = "";
            } else if (1 == mThermalIndicatorMode) {
                mThermalText = "Tcpu=" +  getCpuTemp() + " limit=" + getThermalLimitCpuOpp();
                mThermalTextExtra = "";
                mThermalTextLineThree = "";
            } else if (2 == mThermalIndicatorMode) {
                mThermalText = "Tcpu=" +  getCpuTemp() + " limit=" + getThermalLimitCpuOpp();
                mThermalTextExtra = "Tbat=" + getBattTemp() + " Tpa=" + getPaTemp() + " Twifi=" + getWiFiTemp();
                mThermalTextLineThree = "";
            } else if (3 == mThermalIndicatorMode) {
            	String StringGetWiFiTxUsage = getWiFiTxUsage();
            	String StringGetTxBytes = getTxBytes();
            	String StringGetWifiTemp = getWiFiTemp();
            	mThermalText = "Tcpu=" +  getCpuTemp() + " limit=" + getThermalLimitCpuOpp();
            	mThermalTextExtra = "Tbat=" + getBattTemp() + " Tpa=" + getPaTemp() + " Twifi=" + StringGetWifiTemp;
            	mThermalTextLineThree = "Tx=" + StringGetWiFiTxUsage + "/" + StringGetTxBytes + "Kbps";
            	/*Log.i("LoadAvg", "WifiTxBytes=" + StringGetWiFiTxUsage + "/" + StringGetTxBytes);*/
            	/*Log.i("LoadAvg", "WiFiTemp=" + getWiFiTemp());*/
            }else if( (99 == mThermalIndicatorMode) && (1 == mStorageIndicatorMode)) {
                mThermalText = getmmcqd1();
                mThermalTextExtra = getmmcqd2();
                mThermalTextLineThree = "";
                mThermalIndicatorDelay = mStorageIndicatorDelay;
            }
            mThermalWidth = (int)mLoadPaint.measureText(mThermalText);
            mThermalWidthExtra = (int)mLoadPaint.measureText(mThermalTextExtra);
            mThermalWidthLineThree = (int)mLoadPaint.measureText(mThermalTextLineThree);
        }

        void getThermalIndicatorModeAndDelay()
        {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/driver/mtk_thermal_indicator");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                mThermalIndicatorMode = Integer.valueOf(text);
                text = br.readLine();
                mThermalIndicatorDelay = Integer.valueOf(text);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

	void getStorageIndicatorModeAndDelay()
        {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/driver/mtk_io_osd_config");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                mStorageIndicatorMode = Integer.valueOf(text);
                text = br.readLine();
                mStorageIndicatorDelay = Integer.valueOf(text);

            if(mStorageIndicatorMode == 1)
                mThermalIndicatorMode = 99;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String getmmcqd2() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/driver/mtk_io_osd_mmcqd2");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getmmcqd1() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/driver/mtk_io_osd_mmcqd1");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getCpuTemp() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/mtktz/mtktscpu");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getBattTemp() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/mtktz/mtktsbattery");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getPaTemp() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/mtktz/mtktspa");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getWiFiTemp() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/mtktz/mtktswmt");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getThermalLimitCpuOpp() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/mtktscpu/mtktscpu_opp");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getMaxCpuFreq() {
            String result = "N/A";
            try {
                FileReader fr = new FileReader(
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getMinCpuFreq() {
            String result = "N/A";
            try {
                FileReader fr = new FileReader(
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getCurCpuFreq() {
            String result = "N/A";
            try {
                FileReader fr = new FileReader(
                    "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getWiFiTxUsage() {
            String result = "";
            try {
                FileReader fr = new FileReader("/proc/wmt_tm/tx_thro");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                if (text != null)
                    result = text.trim();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        String getPhyRate() {
            String result = "";
            WifiManager mgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = mgr.getConnectionInfo();
            if (wifiInfo != null)
                result = String.valueOf(wifiInfo.getLinkSpeed()) + " Mbps";
            return result;
        }

        String getTxBytes() {
            String result = "";
            long txMobileBytes = TrafficStats.getMobileTxBytes();
            long txTotalBytes = TrafficStats.getTotalTxBytes();
            long throughput = 0;
            long txWlanBits = (txTotalBytes - txMobileBytes) * 8;
            long curTime = System.currentTimeMillis() / 1000;
            if (txWlanBits > 0 && mPreTxTime > 0) {
                throughput = (txWlanBits - mPreTxBytes)/ (curTime - mPreTxTime);
				throughput = throughput / 1000;
            }

            mPreTxTime = curTime;
            mPreTxBytes = txWlanBits;

            result = String.valueOf(throughput);
            return result;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mView = new LoadView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(mView);
        mView = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
