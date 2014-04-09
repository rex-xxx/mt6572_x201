/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.android.camera.Log;
import com.android.camera.R;

class ProgressBarDrawable extends Drawable {

    private static final String TAG = "ProgressBarDrawable";
    private static final boolean LOG = Log.LOGV;
    private int mBlockSizes[];
    private int mPadding;
    private View mAttachedView;
    private final Paint mPaint = new Paint();
    private Drawable mCleanBlock;
    private Drawable mDirtyBlock;

    public ProgressBarDrawable(Context context, View view, int[] blockSizes, int padding) {
        Resources res = context.getResources();
        mBlockSizes = blockSizes;
        mAttachedView = view;
        mPadding = padding;
        mCleanBlock = res.getDrawable(R.drawable.ic_panorama_block);
        mDirtyBlock = res.getDrawable(R.drawable.ic_panorama_block_highlight);
    }

    @Override
    protected boolean onLevelChange(int level) {
        if (LOG) {
            Log.v(TAG, "onLevelChange: " + level);
        }
        invalidateSelf();
        return true;
    }

    @Override
    public int getIntrinsicWidth() {
        int width = 0;
        for (int i = 0, len = mBlockSizes.length; i < len - 1; i++) {
            width += mBlockSizes[i] + mPadding;
        }
        width += mBlockSizes[mBlockSizes.length - 1];
        if (LOG) {
            Log.v(TAG, "getIntrinsicWidth: " + width);
        }

        return width;
    }

    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    public void draw(Canvas canvas) {
        int xoffset = 0;
        int level = getLevel();
        // draw dirty block according to the number captured.
        for (int i = 0; i < level; i++) {
            int yoffset = (mAttachedView.getHeight() - mBlockSizes[i]) / 2;
            mDirtyBlock.setBounds(xoffset, yoffset, xoffset + mBlockSizes[i], yoffset + mBlockSizes[i]);
            mDirtyBlock.draw(canvas);
            if (LOG) {
                Log.v(TAG, "draw: i=" + i + " xoffset = " + xoffset + " yoffset = " + yoffset);
            }
            xoffset += (mBlockSizes[i] + mPadding);
        }

        // draw the rest as clean block.
        for (int i = level, len = mBlockSizes.length; i < len; i++) {
            int yoffset = (mAttachedView.getHeight() - mBlockSizes[i]) / 2;
            mCleanBlock.setBounds(xoffset, yoffset, xoffset + mBlockSizes[i], yoffset + mBlockSizes[i]);
            mCleanBlock.draw(canvas);
            if (LOG) {
                Log.v(TAG, "draw: i=" + i + " xoffset = " + xoffset + " yoffset = " + yoffset);
            }
            xoffset += (mBlockSizes[i] + mPadding);
        }
    }
}
