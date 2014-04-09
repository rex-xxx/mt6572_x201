/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.oobe.utils;

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.mediatek.xlog.Xlog;

public class OOBEGesturer implements OnGestureListener {
    private static final String TAG = OOBEConstants.TAG;
    // private static final String TAG = "OOBEGesturer";
    private static final float MIN_FLING_DISTANCE = 100;
    private OOBEGesturerCallback mCallBacker;
    private int mTotalStep;
    private int mStepIndex;

    /**
     * OOBEGesturerCallback
     * @param callBacker OOBEGesturerCallback
     * @param totalStep int
     * @param stepIndex int
     */
    public OOBEGesturer(OOBEGesturerCallback callBacker, int totalStep, int stepIndex) {
        mCallBacker = callBacker;
        mTotalStep = totalStep;
        mStepIndex = stepIndex;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // TODO Auto-generated method stub
        if (OOBEConstants.DEBUG) {
            Xlog.d(TAG, "OOBEGesturer.[onDown]");
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Xlog.d(TAG, "OOBEGesturer.[onFling]");
        float x0 = e1.getX();
        float y0 = e1.getY();
        float x1 = e2.getX();
        float y1 = e2.getY();
        Xlog.d(TAG, "OOBEGesturer: (x0,y0)==(" + x0 + "," + y0 + ")      (x1,y1)==(" + x1 + "," + y1 + ")");
        if (!OOBEConstants.WITH_GESTURE) {
            Xlog.i(TAG, "Gesture do not take effect for OOBE, ignore fling event.");
            return false;
        }
        if (x0 - x1 > MIN_FLING_DISTANCE && Math.abs(x0 - x1) > Math.abs(y0 - y1)) { // fling to left
            Xlog.i(TAG, "Fling to left.");
            return onLeftFling();
        } else if (x1 - x0 > MIN_FLING_DISTANCE && Math.abs(x0 - x1) > Math.abs(y0 - y1)) {
            Xlog.i(TAG, "Fling to right.");
            return onRightFling();
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (OOBEConstants.DEBUG) {
            // TODO Auto-generated method stub
            Xlog.d(TAG, "OOBEGesturer.[onLongPress]");
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // if(OOBEConstants.DEBUG)Xlog.d(TAG, "OOBEGesturer.[onScroll]");
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub
        if (OOBEConstants.DEBUG) {
            Xlog.d(TAG, "OOBEGesturer.[onShowPress]");
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        if (OOBEConstants.DEBUG) {
            Xlog.d(TAG, "OOBEGesturer.OOBEGesturer.[onSingleTapUp]");
        }
        return false;
    }

    /**
     * deal with left fling
     * 
     * @return true if this event is consumed, false else
     */
    public boolean onLeftFling() {
        if (OOBEConstants.DEBUG) {
            Xlog.i(TAG, "onLeftFling()");
        }
        if (mTotalStep > mStepIndex) {
            mCallBacker.onNextStep(true);
            // mActivity.setResult(OOBEConstants.RESULT_CODE_NEXT);
            // mActivity.finish();
            return true;
        } else {
            // Toast.makeText(((Activity)mCallBacker), R.string.oobe_fling_to_last, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * deal with right fling
     * 
     * @return true if this event is consumed, false else
     */
    public boolean onRightFling() {
        if (OOBEConstants.DEBUG) {
            Xlog.i(TAG, "onRightFling()");
        }
        if (mStepIndex > 1) {
            mCallBacker.onNextStep(false);
            // mActivity.setResult(OOBEConstants.RESULT_CODE_BACK);
            // mActivity.finish();
            return true;
        } else {
            // Toast.makeText(((Activity)mCallBacker), R.string.oobe_fling_to_first, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

}
