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

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.mediatek.xlog.Xlog;

/**
 * View for dealing with gesture event
 * 
 * @author mtk80906
 * 
 */
public class OOBERootView extends LinearLayout {
    // private static final String TAG = OOBEConstants.TAG;
    private static final String TAG = "dwz";
    // private OOBEGestureListener mListener=null;
    GestureDetector mGestureDetector = null;

    /**
     * OOBERootView 
     * @param context Context
     */
    public OOBERootView(Context context) {
        super(context);
    }

    /**
     * OOBERootView
     * @param context Context
     * @param attrs AttributeSet
     */
    public OOBERootView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Handle gesture before every sub-component
     * @param ev MotionEvent
     * @return boolean true or false 
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // if(OOBEConstants.DEBUG)Xlog.d(TAG, "OOBERootView().dispatchTouchEvent()");

        // intercept motion event, if not needed gesture, just release it
        if (mGestureDetector != null && mGestureDetector.onTouchEvent(ev)) {
            if (OOBEConstants.DEBUG) {
                Xlog.d(TAG, "OOBERootView() event have been consumed by sub-component, return");
            }
            return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    // @Override
    // public boolean onInterceptTouchEvent(MotionEvent ev) {
    // Xlog.e(TAG, "onInterceptTouchEvent for root view");
    // if(mGestureDetector!=null && mGestureDetector.onTouchEvent(ev)){
    // return true;
    // }
    // return super.onInterceptTouchEvent(ev);
    // }

    /**
     * Have not been handled by sub-component, back to root component As text view could not get fling event, don't transfer
     * to Activity again, just keep by root component, OOBERootView
     * @param event MotionEvent
     * @return boolean true of false
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (OOBEConstants.DEBUG) {
            Xlog.i(TAG, "OOBERootView.onTouchEvent, just consume it");
        }
        return true;
    }

    // @SuppressWarnings("deprecation")
    // public void setGestureListener(OOBEGestureListener listener){
    // mListener = listener;
    // if(mListener!=null){
    // OOBEGesturer gestureListener = new OOBEGesturer(mListener);
    // mGestureDetector = new GestureDetector(gestureListener);
    // }else{
    // mGestureDetector=null;
    // }
    // }

    /**
     * addGestureListener
     * @param gestureListener OOBEGesturer
     */
/*
    public void addGestureListener(OOBEGesturer gestureListener) {
        if (gestureListener != null) {
            mGestureDetector = new GestureDetector(gestureListener);
        } else {
            mGestureDetector = null;
        }
    }
*/
}
