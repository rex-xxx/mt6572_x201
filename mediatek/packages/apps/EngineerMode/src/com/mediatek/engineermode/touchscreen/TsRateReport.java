/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.engineermode.touchscreen;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

/**
 * Demonstrates wrapping a layout in a ScrollView.
 */

public class TsRateReport extends Activity {

    MyView mView = null;
    DisplayMetrics mMetrick = new DisplayMetrics();

    private static final String TAG = "EM/TouchScreen/RR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(new MyView(this));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mView = new MyView(this);
        setContentView(mView);
    }

    @Override
    public void onResume() {
//        Xlog.v(TAG, "-->onResume");
        super.onResume();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrick);
    }


    public class MyView extends View {

        private final ArrayList<PointerData> mPtsStatus = new ArrayList<PointerData>();
        private int mPointerNumDetected = 0;
        private int mMinPtrId = -1;
        
        public MyView(Context c) {
            super(c);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            Xlog.v(TAG, "-->onDraw");
            int textsize = 15;
            canvas.drawText("Pointer number detected: "
                    + String.valueOf(mPointerNumDetected), 3, textsize + 10,
                    getPaint(4, textsize));

            for (int idx = 0; idx < mPtsStatus.size(); idx++) {
                PointerData pt = mPtsStatus.get(idx);

                pt.setUTimeStamp();
                pt.calculateRate();
                String s = String.format("pid=%2d, X=%3d, Y=%3d.", pt.mPid,
                        pt.mLastX, pt.mLastY);
                String ss = String.format("Rate=%dHz, Count=%d, Time=%dms",
                        pt.mRate, pt.mCnt, pt.mMills);

                int x = 3;
                int y = 10 + (textsize * 3) + idx * 3 * textsize;

                canvas.drawText(s, x, y, getPaint(idx, textsize));
                canvas.drawText(ss, x, y + textsize, getPaint(idx, textsize));

            }

        }

        private class PointerData {
            private volatile boolean mDown;
            private volatile long mDownTime;
            private volatile long mUpTime;
            public volatile int mCnt;
            public volatile int mRate;
            public volatile int mMills;
            public volatile int mPid;
            public volatile int mLastX;
            public volatile int mLastY;

            public void setDTimeStamp() {
                mDownTime = System.currentTimeMillis();
            }

            public void setUTimeStamp() {
                mUpTime = System.currentTimeMillis();
            }

            public void calculateRate() {
                mMills = (int) (mUpTime - mDownTime);
//                if (mMills != 0) {
//                    mRate = (int) ((1000L * mCnt) / mMills);
//                } else {
//                    mRate = -1;
//                }
                if (mMills == 0) {
                    mRate = -1;
                } else {
                    mRate = (int) ((1000L * mCnt) / mMills);
                }
            }

            public void clean() {
                mDownTime = 0;
                mUpTime = 0;
                mCnt = 0;
                mPid = 0;
                mRate = 0;
                mDown = false;
            }

        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            Xlog.v(TAG, "-->onTouchEvent");
            int action = event.getAction();
            int actionCode = action & MotionEvent.ACTION_MASK;

            int ptIdx = action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
            if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                    || actionCode == MotionEvent.ACTION_DOWN) {
                PointerData pt = null;
                // new point is added.
                if (ptIdx >= mPtsStatus.size()) {
                    pt = new PointerData();
                    mPtsStatus.add(pt);
                } else {
                    pt = mPtsStatus.get(ptIdx);
                }
                pt.clean();
                pt.mPid = ptIdx;
                pt.setDTimeStamp();
                pt.mDown = true;
                mPointerNumDetected++;
            } else if (actionCode == MotionEvent.ACTION_POINTER_UP
                    || actionCode == MotionEvent.ACTION_UP) {
                PointerData pt = null;
                // new point is added.
                if (ptIdx >= mPtsStatus.size()) {
                    // never happened.
                    pt = new PointerData();
                    mPtsStatus.add(pt);
                } else {
                    pt = mPtsStatus.get(ptIdx);
                }
                pt.setUTimeStamp();
                pt.mDown = false;
                // pt.CalculateRate();
            }
            if (actionCode == MotionEvent.ACTION_POINTER_UP) {
                mPointerNumDetected--;
            } else if (actionCode == MotionEvent.ACTION_UP) {
                mPointerNumDetected = 0;
            }

            int pointCt = event.getPointerCount();
            // Log.i("MTXX", "Pointer counts = "+pointCt);
            Xlog.v(TAG, "Pointer counts = " + pointCt + " mPtsStatus.size()= "
                    + mPtsStatus.size());

//            try {
            for (int i = 0; i < pointCt; i++) {
                // new point is added.
                if (i < mPtsStatus.size()) {
                    int notZeroBasedPid = event.getPointerId(i);
                    calcMinId(notZeroBasedPid);
                    int pid = notZeroBasedPid - mMinPtrId;

                    PointerData pt = mPtsStatus.get(i);
//                    PointerData pt = mPtsStatus.get(pid);
                    pt.mCnt++;
                    pt.mLastX = (int) event.getX(i);
                    pt.mLastY = (int) event.getY(i);
                }
            }
//            } catch (Exception e) {
//                Xlog.d(TAG, e.getMessage());
//            }

            invalidate();
            return true;
        }



        private void calcMinId(int currentId) {
            if (mMinPtrId == -1) {
                mMinPtrId = currentId;
            } else {
                mMinPtrId = mMinPtrId < currentId ? mMinPtrId : currentId;
            }
        }

        public void clear() {
            mPtsStatus.clear();
            invalidate();
        }

        Paint getPaint(int idx, int textsize) {
            Paint paint = new Paint();
            paint.setAntiAlias(false);
            if (idx < TsMultiTouch.RGB.length) {
                paint.setARGB(255, TsMultiTouch.RGB[idx][0],
                        TsMultiTouch.RGB[idx][1], TsMultiTouch.RGB[idx][2]);
            } else {
                paint.setARGB(255, 255, 255, 255);
            }
            // 60=a*10+b;
            // int textsize = (int)(10);
            paint.setTextSize(textsize);
            return paint;
        }

    }

}
