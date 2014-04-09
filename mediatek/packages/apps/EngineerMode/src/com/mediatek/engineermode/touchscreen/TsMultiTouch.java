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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.mediatek.xlog.Xlog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Demonstrates wrapping a layout in a ScrollView.
 * 
 * 
 */

public class TsMultiTouch extends Activity {
    public static final int CLEAR_CANVAS_ID = 1;
    public static final int SET_PT_SIZE_ID = 2;
    public static final int DIS_HISTORY_ID = 3;
    public static final int[][] RGB = { { 255, 0, 0 }, { 0, 255, 0 },
            { 0, 0, 255 }, { 255, 255, 0 }, { 0, 255, 255 }, { 255, 0, 255 },
            { 100, 0, 0 }, { 0, 100, 0 }, { 0, 0, 100 }, { 100, 100, 0 },
            { 0, 100, 100 }, { 100, 0, 100 }, { 255, 255, 255 } };
    MyView mView = null;
    volatile boolean mDisplayHistory = true;
    DisplayMetrics mMetrics = new DisplayMetrics();

    public int mPointSize = 1;
    private static final String TAG = "EM/TouchScreen/MT";

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
        final SharedPreferences preferences = this.getSharedPreferences(
                "touch_screen_settings", android.content.Context.MODE_PRIVATE);
        String fileString = preferences.getString("filename", "N");
//        if (!fileString.equals("N")) {
        if (!"N".equals(fileString)) {
            final String commPath = fileString;
            new Thread() {
                public void run() {
                    String[] cmd = { "/system/bin/sh", "-c",
                            "echo [ENTER_MULTI_TOUCH] >> " + commPath }; // file
                    int ret;
                    try {
                        ret = TouchScreenShellExe.execCommand(cmd);
                        if (0 == ret) {
                            Xlog.v(TAG, "-->onResume Start logging...");

                        } else {
                            Xlog.v(TAG, "-->onResume Logging failed!");
                        }
                    } catch (IOException e) {
                        Xlog.e(TAG, e.toString());
                    }
                }
            }.start();

        }
        mPointSize = preferences.getInt("size", 10);
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    @Override
    public void onPause() {

        Xlog.v(TAG, "-->onPause");
        final SharedPreferences preferences = this.getSharedPreferences(
                "touch_screen_settings", android.content.Context.MODE_PRIVATE);
        String fileString = preferences.getString("filename", "N");
//        if (!fileString.equals("N")) {
        if (!"N".equals(fileString)) {
            String[] cmd = { "/system/bin/sh", "-c",
                    "echo [LEAVE_MULTI_TOUCH] >> " + fileString }; // file

            int ret;
            try {
                ret = TouchScreenShellExe.execCommand(cmd);
                if (0 == ret) {
                    Toast.makeText(this, "Stop logging...", Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(this, "Logging failed!", Toast.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Xlog.e(TAG, e.toString());
            }

        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, CLEAR_CANVAS_ID, 0, "Clean Table");
        menu.add(0, SET_PT_SIZE_ID, 0, "Set Point Size");

        menu.add(0, DIS_HISTORY_ID, 0, "Hide History");

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mDisplayHistory) {
            menu.getItem(2).setTitle("Hide History");
        } else {
            menu.getItem(2).setTitle("Show History");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        switch (mi.getItemId()) {
        case CLEAR_CANVAS_ID:
            mView.clear();
            break;
        case DIS_HISTORY_ID:
            if (mDisplayHistory) {
                mDisplayHistory = false;
            } else {
                mDisplayHistory = true;
            }

            mView.invalidate();

            break;
        case SET_PT_SIZE_ID:
            // v.Clear();
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            new AlertDialog.Builder(this).setTitle(
                    "Insert pixel size of point [1-10]").setView(input)
                    .setPositiveButton("OK", new OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            if (input.getText() != null
                                    && (!input.getText().toString().equals(""))) {
                                int sz;
                                try {
                                    sz = Integer.valueOf(input.getText()
                                            .toString());
                                } catch (NumberFormatException e) {
                                    return;
                                }
                                if (sz < 1) {
                                    TsMultiTouch.this.mPointSize = 1;
                                } else if (sz > 10) {
                                    TsMultiTouch.this.mPointSize = 10;
                                } else {
                                    TsMultiTouch.this.mPointSize = sz;
                                }
                                final SharedPreferences preferences = TsMultiTouch.this
                                        .getSharedPreferences(
                                                "touch_screen_settings",
                                                android.content.Context.MODE_PRIVATE);
                                preferences.edit().putInt("size",
                                        TsMultiTouch.this.mPointSize).commit();

                                mView.invalidate();
                            }
//                            else {
//                                Xlog.w(TAG, "DIALOG edit null");
//                            }
                        }
                    }).setNegativeButton("Cancel", null).show();
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(mi);
    }

    public class MyView extends View {

        public ArrayList<Vector<Vector<TsPointDataStruct>>> mInputIds = new ArrayList<Vector<Vector<TsPointDataStruct>>>();
        public ArrayList<TsPointStatusStruct> mPtsStatus = new ArrayList<TsPointStatusStruct>();
        private int mMinPtrId = -1;
        
        public MyView(Context c) {
            super(c);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            int fingerNum = mInputIds.size();
            for (int idx = 0; idx < fingerNum; idx++) {
                Vector<Vector<TsPointDataStruct>> inputIdx = mInputIds.get(idx);
                Paint targetPaint = getPaint(idx);
                int inputSize = inputIdx.size();
                Xlog.i(TAG, "idx: " + idx + " input size: " + inputSize);
                for (int j = 0; j < inputSize; j++) {
                    Vector<TsPointDataStruct> line = inputIdx.get(j);
                    int lineSize = line.size();
                    Xlog.i(TAG, "Line" + j + " size " + lineSize);
                    if (lineSize > 0) {
//                        int lastX = line.get(0).coordinateX;
//                        int lastY = line.get(0).coordinateY;
                        int lastX = line.get(0).getmCoordinateX();
                        int lastY = line.get(0).getmCoordinateY();
                        for (int i = 0; i < lineSize; i++) {
                            int x = line.get(i).getmCoordinateX();
                            int y = line.get(i).getmCoordinateY();
//                            float fat_size = line.get(i).fat_size;
//                            float fat_size = line.get(i).getmFatSize();
                            
                            // canvas.drawLine(lastX, lastY, x, y,
                            // mTargetPaint);
                            // canvas.drawPoint(lastX, lastY, mPaint);
                            // canvas.drawCircle(lastX, lastY, fat_size * 100,
                            // targetPaint);
                            if (mDisplayHistory) {
                                canvas.drawCircle(lastX, lastY, mPointSize,
                                        targetPaint);
                            }
                            // canvas.drawPoint(lastX, lastY, mTargetPaint);
                            // Log.i("MTXXS", "point size: " + mPointSize);
                            lastX = x;
                            lastY = y;
                        }
                        TsPointDataStruct last = line.get(lineSize - 1);
                        // last line
                        if (j == inputSize - 1) {
//                            String s = "pid " + String.valueOf(last.pid)
//                                    + " x=" + String.valueOf(last.coordinateX)
//                                    + ", y=" + String.valueOf(last.coordinateY);
                            String s = "pid " + String.valueOf(last.getmPid())
                            + " x=" + String.valueOf(last.getmCoordinateX())
                            + ", y=" + String.valueOf(last.getmCoordinateY());
                            
                            Rect rect = new Rect();
                            targetPaint.getTextBounds(s, 0, s.length(), rect);

                            int x = last.getmCoordinateX() - rect.width() / 2;
                            int y = last.getmCoordinateY() - rect.height() * 3;

                            if (x < 0) {
                                x = 0;
                            } else if (x > mMetrics.widthPixels - rect.width()) {
                                x = mMetrics.widthPixels - rect.width();
                            }

                            if (y < rect.height()) {
                                y = rect.height();
                            } else if (y > mMetrics.heightPixels) {
                                y = mMetrics.heightPixels;
                            }

                            canvas.drawText(s, x, y, targetPaint);
                            canvas.drawCircle(last.getmCoordinateX(),
                                    last.getmCoordinateY(), mPointSize * 3,
                                    targetPaint);
                        }
                    }
                }
            }

        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            int actionCode = action & MotionEvent.ACTION_MASK;

            int ptIdx = action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
            Xlog.i(TAG, "onTouchEvent: ptIdx: " + ptIdx + " mPtsStatus.size(): " + mPtsStatus.size());
            if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                    || actionCode == MotionEvent.ACTION_DOWN) {
                // new point is added.
                if (ptIdx >= mPtsStatus.size()) {
                    TsPointStatusStruct pt = new TsPointStatusStruct();
//                    pt.isDown = true;
                    pt.setmDown(true);
                    mPtsStatus.add(pt);
                } else {
                    TsPointStatusStruct pt = mPtsStatus.get(ptIdx);
//                    pt.isDown = true;
                    pt.setmDown(true);
                }
            } else if (actionCode == MotionEvent.ACTION_POINTER_UP
                    || actionCode == MotionEvent.ACTION_UP) {
                if (ptIdx >= mPtsStatus.size()) {
                 // new point is added.
                    TsPointStatusStruct pt = new TsPointStatusStruct();
//                    pt.isDown = false;
                    pt.setmDown(false);
                    mPtsStatus.add(pt);
                } else {
                    TsPointStatusStruct pt = mPtsStatus.get(ptIdx);
//                    pt.isDown = false;
                    pt.setmDown(false);
                }
            }
            for (int idx = 0; idx < mPtsStatus.size(); idx++) {
                TsPointStatusStruct st = mPtsStatus.get(idx);
                Xlog.i(TAG, "mPtsStatus.size(): " + mPtsStatus.size()
                        + " st.ismDown(): " + st.ismDown()
                        + " st.ismNewLine(): " + st.ismNewLine());
                if (st.ismDown()) {
                    if (!st.ismNewLine()) {
                        Vector<TsPointDataStruct> newLine = new Vector<TsPointDataStruct>();
                        if (idx >= mInputIds.size()) {
                            mInputIds
                                    .add(new Vector<Vector<TsPointDataStruct>>());
                        }
                        mInputIds.get(idx).add(newLine);
//                        st.isNewLine = true;
//                        st.setmNewLine(true);
                        mPtsStatus.get(idx).setmNewLine(true);
                    }
                } else {
//                    st.isNewLine = false;
//                    st.setmNewLine(false);
                    mPtsStatus.get(idx).setmNewLine(false);
                }
            }

            int pointCt = event.getPointerCount();
            Xlog.i(TAG, "Pointer counts = " + pointCt);
            for (int i = 0; i < pointCt; i++) {
                int notZeroBasedPid = event.getPointerId(i);
                calcMinId(notZeroBasedPid);
                Xlog.i(TAG, " i =" + i + " notZeroBasedPid = " + notZeroBasedPid + " mMinPtrId = " + mMinPtrId);
                int pid = notZeroBasedPid - mMinPtrId;
                try {
                    TsPointDataStruct n = new TsPointDataStruct();
                    // Log.i("MTXX", "new pointDataStruct ok0");
    //                    n.action = actionCode;
    //                    n.coordinateX = (int) event.getX(i);
    //                    n.coordinateY = (int) event.getY(i);
    //                    n.pid = pid;
    //                    n.pressure = event.getPressure(i);
    //                    n.fat_size = event.getSize(pid);
                    
                    n.setmAction(actionCode);
                    n.setmCoordinateX((int) event.getX(i));
                    n.setmCoordinateY((int) event.getY(i));
                    n.setmPid(pid);
                    n.setmPressure(event.getPressure(i));
//                    float tmpSize = event.getSize(pid);
                    float tmpSize = event.getSize(i);
                    if (tmpSize < 0.01f) {
                        tmpSize = 0.01f;
                    }
                    n.setmFatSize(tmpSize);
    
                    Xlog.i(TAG, " pid = " +  pid + " mInputIds.size() = " + mInputIds.size());
//                    Xlog.i(TAG, " mInputIds.get(pid).size() = " +  mInputIds.get(pid).size());
                    if (i < mInputIds.size()) {
                        Xlog.i(TAG, " mInputIds.get(i).size() = "
                                + mInputIds.get(i).size());

                        Vector<TsPointDataStruct> currentline = mInputIds
                                .get(i).get(mInputIds.get(i).size() - 1);

                        // Vector<TsPointDataStruct> currentline =
                        // mInputIds.get(
                        // pid).get(mInputIds.get(pid).size() - 1);
                        currentline.add(n);
                    }
                } catch (IllegalArgumentException e) {
                    Xlog.i(TAG, "get point data fail!!");
                    Xlog.i(TAG, e.toString());
                }
//                catch (IndexOutOfBoundsException e) {
//                    Xlog.i(TAG, e.toString());
//                }

            }

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
            for (Vector<Vector<TsPointDataStruct>> inputId : mInputIds) {
                for (Vector<TsPointDataStruct> m : inputId) {
                    m.clear();
                }
                inputId.clear();
            }
            mPtsStatus.clear();
            mInputIds.clear();
            invalidate();
        }

        Paint getPaint(int idx) {
//            final int[][] RGB = { { 255, 0, 0 }, { 0, 255, 0 }, { 0, 0, 255 },
//                    { 255, 255, 0 }, { 0, 255, 255 }, { 255, 0, 255 },
//                    { 255, 255, 255 }, };
            Paint paint = new Paint();
            paint.setAntiAlias(false);
            if (idx < RGB.length) {
                paint.setARGB(255, RGB[idx][0], RGB[idx][1], RGB[idx][2]);
            } else {
                paint.setARGB(255, 255, 255, 255);
            }
            // 60=a*10+b;
            int textsize = (int) (mPointSize * 3.63 + 7.37);
            paint.setTextSize(textsize);
            return paint;
        }

    }

}
