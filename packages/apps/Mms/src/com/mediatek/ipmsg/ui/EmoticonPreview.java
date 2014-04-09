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
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
package com.mediatek.ipmsg.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.android.mms.R;

public class EmoticonPreview {

    private Context mContext;
    private PopupWindow mPopWindow;
    private LayoutInflater mInflater;
    private View mParent;
    private View mContentView;
    private GifView mImage;
    private int mResId;
    private boolean mIsDismissed = true;

    public EmoticonPreview(Context context, View parent) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);

        mParent = parent;
        constructRecordWinsow();
    }

    private void constructRecordWinsow() {

        mContentView = mInflater.inflate(R.layout.emoticon_preview, null);

        mImage = (GifView) mContentView.findViewById(R.id.iv_emoticon_img);

        int width = mContext.getResources().getDimensionPixelOffset(R.dimen.emoticon_preview_width);
        int height = mContext.getResources().getDimensionPixelOffset(
                R.dimen.emoticon_preview_height);
        mPopWindow = new PopupWindow(mContentView, width, height);
    }

    public void showWindow() {

        mIsDismissed = false;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            int offset = mContext.getResources().getDimensionPixelOffset(
                    R.dimen.emoticon_preview_port_offset);
            mPopWindow.showAtLocation(mParent, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, offset);
        } else {
            int offset = mContext.getResources().getDimensionPixelOffset(
                    R.dimen.emoticon_preview_land_offset);
            mPopWindow.showAtLocation(mParent, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, offset);
        }

        mPopWindow.setFocusable(false);
        mPopWindow.setTouchable(false);
    }

    public void dissWindow() {

        mIsDismissed = true;
        mPopWindow.dismiss();
    }

    public boolean isShow() {
        return !mIsDismissed;
    }

    public void setEmoticon(int id) {
        mResId = id;
        mImage.setSource(id);
    }

    public void setEmoticon(Drawable d) {

    }
}
