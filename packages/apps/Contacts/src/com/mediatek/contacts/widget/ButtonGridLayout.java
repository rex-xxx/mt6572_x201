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
 */

/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.contacts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.android.contacts.R;
import com.android.contacts.dialpad.DialpadImageButton;

public class ButtonGridLayout extends FrameLayout {
    private final int mCOLUMNS = 3;
    private final int mROWS = 4;

    // Width and height of a button
    private int mButtonWidth;
    private int mButtonHeight;

    // Width and height of a button + padding.
    private int mWidthInc;
    private int mHeightInc;

    // Height of the dialpad. Used to align it at the bottom of the
    // view.
    private int mHeight;

    private boolean mLayouted = false;

    public ButtonGridLayout(Context context) {
        super(context);
    }

    public ButtonGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected DialpadImageButton createGridButton(int id, int imageResId, Drawable drawable) {
        DialpadImageButton imageButton = new DialpadImageButton(getContext());
        imageButton.setId(id);
        imageButton.setImageResource(imageResId);
        imageButton.setBackgroundDrawable(drawable);
        imageButton.setSoundEffectsEnabled(false);

        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(mButtonWidth, mButtonHeight);
        imageButton.setLayoutParams(lParams);

        return imageButton;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mButtonWidth = getContext().getResources().getDimensionPixelSize(R.dimen.button_grid_layout_button_width);
        mButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.button_grid_layout_button_height);
        mWidthInc = mButtonWidth;
        mHeightInc = mButtonHeight;

        DialpadImageButton button;
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[] {
            android.R.attr.selectableItemBackground
        });
        Drawable drawable = typedArray.getDrawable(0);

        button = createGridButton(R.id.one, R.drawable.dial_num_1_wht, drawable);
        addView(button);

        button = createGridButton(R.id.two, R.drawable.dial_num_2_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.three, R.drawable.dial_num_3_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.four, R.drawable.dial_num_4_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.five, R.drawable.dial_num_5_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.six, R.drawable.dial_num_6_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.seven, R.drawable.dial_num_7_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.eight, R.drawable.dial_num_8_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.nine, R.drawable.dial_num_9_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.star, R.drawable.dial_num_star_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.zero, R.drawable.dial_num_0_wht, drawable.getConstantState().newDrawable());
        addView(button);

        button = createGridButton(R.id.pound, R.drawable.dial_num_pound_wht, drawable.getConstantState().newDrawable());
        addView(button);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mLayouted) {
            return;
        }
        mLayouted = true;

        int i = 0;
        // The last row is bottom aligned.
        int y = mPaddingTop;
        for (int row = 0; row < mROWS; row++) {
            int x = mPaddingLeft;
            for (int col = 0; col < mCOLUMNS; col++) {
                View child = getChildAt(i);

                child.layout(x, y, x + mButtonWidth, y + mButtonHeight);

                x += mWidthInc;
                i++;
            }
            y += mHeightInc;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Measure the first child and get it's size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        final int width = displayMetrics.widthPixels;
        final int height = mButtonHeight * mROWS;

        setMeasuredDimension(width, height);
    }
}
