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

package com.mediatek.cmmb.app;

import android.content.Context;
import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
//import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
class VisualizerView extends View {
    private final int mBANDS = 20;
    private final int mMARGIN = 3;//margin between bars in horizontal direction.
    private byte[] mBytes;
    private Rect[] mBarCoordines;
    private int[] mCeilings;
    private int mPrecision;
    //refer to foobar spectrum display;
    private final int[] mBands = {60,70,126,176,224,276,352,465,616,846,1100,1500,2000
            ,2800,3900,5400,7700,11000,14000,20000}; 
    private final int[] mAverages = {1,2,2,2,2,3,3,3,4,4,6,6
            ,6,8,8,8,8,8,10,10};     

    private Paint mForePaint = new Paint();

    public VisualizerView(Context context) {
        super(context);
        init();
    }
    
    public VisualizerView(Context context,AttributeSet attrs) {
        super(context,attrs);
        init();
    }  
    
    public VisualizerView(Context context,AttributeSet attrs,int defStyle) {
        super(context,attrs,defStyle);
        init();
    }    

    private void init() {
        mBytes = null;

        mForePaint.setStrokeWidth(2f);
        mForePaint.setAntiAlias(true);
        mForePaint.setStyle(Paint.Style.FILL);
        //Shader mShader=new LinearGradient(0,0,100,100,new int[]{Color.BLUE,Color.RED}
            //,null,Shader.TileMode.REPEAT);
        //mForePaint.setShader(mShader);
        mCeilings = new int[mBANDS];        

    }

    public void updateVisualizer(byte[] bytes,int samplingRate) {
        mBytes = bytes;
        mPrecision = samplingRate / 1000 / mBytes.length; 
        invalidate();
    }
    
    private void calculateTops() {
        int height = getHeight();
        for (int i = 0;i < mBANDS;i++) {
            int value = 0;

            //get the position of the band being processing  in mBytes,
            //and make an averaging over samples including this one 
            //and the following ones of specified number.
            int k = (mBands[i] / mPrecision) * 2;
            for (int j = 0;j < mAverages[i]; j++) {
                value += Math.abs(mBytes[k + 2 * j]);
            }
            value /= mAverages[i];
            //from experiment,the sampled value is too small.
            //double it here in order to get a better graphical view.
            value *= 2;

            //clipping will cause distortion,but it is ok here because we are not producing 
            //data for signal processing but for displaying.
            if (value > 127) {
                value = 127;
            }
            value = height - (height * value) / 127;

            mBarCoordines[i].top = value;
            int var = mBarCoordines[i].top - mCeilings[i];            
            mCeilings[i] = mBarCoordines[i].top - var * 2 / 3;
            
            if (mCeilings[i] > mBarCoordines[i].top) {
                mCeilings[i] = mBarCoordines[i].top;
            }    
            
        }
    }
    
    private void drawBarsAndCeilings(Canvas canvas) {
        for (int i = 0;i < mBANDS;i++) {
            mForePaint.setColor(0xff699500);
            //draw ceilings.
            canvas.drawLine(mBarCoordines[i].left,mCeilings[i]
                        ,mBarCoordines[i].right,mCeilings[i],mForePaint);            
            //draw bars.
            canvas.drawRect(mBarCoordines[i],mForePaint);            
        }
    }
 
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }
        
        if (mBarCoordines == null) {
            mBarCoordines = new Rect[mBANDS];
            int barwidth = (getWidth() - mMARGIN - 1 * mBANDS) / mBANDS;
            int height = getHeight();
            //initialize coordines of each bar.
            //left,right,bottom can be determined at this time,top is caculated at a later time based on the samples.
            for (int i = 0;i < mBANDS; i++) {
                mBarCoordines[i] = new Rect();
                mBarCoordines[i].left = mMARGIN + (1 + barwidth) * i;
                mBarCoordines[i].right = mBarCoordines[i].left + barwidth;
                mCeilings[i] = height;
                mBarCoordines[i].bottom = height;
                mBarCoordines[i].top = height;
            }   
        }
        calculateTops();
        drawBarsAndCeilings(canvas);
    }
}
