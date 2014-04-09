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
package com.android.mms.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.ui.LevelControlLayout.OnScrollToScreenListener;
import com.android.mms.util.MessageConsts;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;

public class EmoticonPanel extends LinearLayout implements OnCheckedChangeListener {

    private Handler mHandler;
    private Context mContext;
    private LevelControlLayout mScrollLayout;
    private LinearLayout mSharePanelMain;
    private RadioButton mDotFirst;
    private RadioButton mDotSec;
    private RadioButton mDotThird;
    private RadioButton mDotForth;
    private Button mDelEmoticon;

    private int mOrientation = 0;
    private int[] mColumnArray;
    private RadioButton mDefaultTab;
    private RadioButton mGiftTab;
    private String[] mDefaultName;
    private String[] mGiftName;
    private int mDefaultIndex = 0;
    private int mGiftIndex = 0;
    private EditEmoticonListener mListener;

    private DelEmoticonThread mDelEmoticonThread;
    private Object mObject = new Object();
    private boolean mNeedQuickDelete = false;
    private int mPortEmotionColumnNumber = 0;
    private int mLandEmotionColumnNumber = 0;

    private static final String TAG = "Mms/EmoticonPanel";

    public EmoticonPanel(Context context) {
        super(context);
        mContext = context;
    }

    public EmoticonPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mScrollLayout = (LevelControlLayout)findViewById(R.id.emoticon_panel_zone);
        mSharePanelMain = (LinearLayout)this;
        mDotFirst = (RadioButton)findViewById(R.id.rb_dot_first);
        mDotSec = (RadioButton)findViewById(R.id.rb_dot_sec);
        mDotThird = (RadioButton)findViewById(R.id.rb_dot_third);
        mDotForth = (RadioButton)findViewById(R.id.rb_dot_forth);
        mDefaultTab = (RadioButton)findViewById(R.id.smiley_panel_default_btn);
        mGiftTab = (RadioButton)findViewById(R.id.smiley_panel_gift_btn);
        mDefaultName = mContext.getResources().getStringArray(R.array.default_emoticon_name);
        mGiftName = mContext.getResources().getStringArray(R.array.gift_emoticon_name);
        if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
            mPortEmotionColumnNumber = (int)mContext.getResources().getInteger(R.integer.emoticon_column_port_number);
        } else {
            mPortEmotionColumnNumber = 4;
        }
        if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
            mLandEmotionColumnNumber = (int)mContext.getResources().getInteger(R.integer.emoticon_column_land_number);
        } else {
            mLandEmotionColumnNumber = 2;
        }
        OnClickListener panelClickListener = new LinearLayout.OnClickListener() {
            public void onClick(View v) {
                int clickedId = v.getId();
                if (R.id.default_panel == clickedId) {
                    mDefaultTab.setChecked(true);
                } else if (R.id.gift_panel == clickedId) {
                    mGiftTab.setChecked(true);
                }
            }
        };
        LinearLayout defaultPanel = (LinearLayout)findViewById(R.id.default_panel);
        LinearLayout giftPanel = (LinearLayout)findViewById(R.id.gift_panel);

        mDelEmoticon = (Button)findViewById(R.id.smiley_panel_del_btn);
        mDelEmoticon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mListener.doAction(EditEmoticonListener.delEmoticon, "");
                    startDelEmoticon();
                    break;
                case MotionEvent.ACTION_UP:
                    stopDelEmoticon();
                    break;
                default:
                    break;
                }
                return false;
            }
        });

        defaultPanel.setOnClickListener(panelClickListener);
        giftPanel.setOnClickListener(panelClickListener);
        mDefaultTab.setOnCheckedChangeListener(this);
        mGiftTab.setOnCheckedChangeListener(this);
        resetShareItem();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int checkedId = buttonView.getId();
        if (!isChecked) {
            return;
        }
        if (R.id.smiley_panel_default_btn == checkedId) {
            mDefaultTab.setChecked(true);
            mGiftTab.setChecked(false);
            addDefaultPanel();
        } else if (R.id.smiley_panel_gift_btn == checkedId) {
            mDefaultTab.setChecked(false);
            mGiftTab.setChecked(true);
            addGiftPanel();
        }
    }

    /**
     * Build share item.
     */
    public void resetShareItem() {
        if (mDefaultTab.isChecked()) {
            addDefaultPanel();
        } else if (mGiftTab.isChecked()) {
            addGiftPanel();
        }
        mScrollLayout.autoRecovery();
    }

    public void recycleView() {
        if (mScrollLayout != null && mScrollLayout.getChildCount() != 0) {
            mScrollLayout.removeAllViews();
        }
    }

    /**
     * Sets the handler.
     *
     * @param handler
     *            the new handler
     */
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    protected class EmoticonAdapter extends BaseAdapter {

        protected int[] mIconArr;

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public EmoticonAdapter(int[] iconArray) {
            mIconArr = iconArray;
        }

        @Override
        public int getCount() {
            return mIconArr.length;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.emoticon_grid_item,
                        null);
                convertView.setTag(convertView);
            } else {
                convertView = (View) convertView.getTag();
            }

            ImageView ivPre = (ImageView) convertView.findViewById(R.id.iv_emoticon_icon);

            ivPre.setImageResource(mIconArr[position]);
            return convertView;
        }
    }

    public void setEditEmoticonListener(EditEmoticonListener l) {
        mListener = l;
    }

    /**
     * The listener interface for receiving editEmoticon events. The class that
     * is interested in processing a editEmoticon event implements this
     * interface, and the object created with that class is registered with a
     * component using the component's
     * <code>setEditEmoticonListener<code> method. When
     * the addEmotion, delEmoticon and sendEmoticon event occurs, that object's appropriate
     * method is invoked.
     *
     * @see EditEmoticonEvent
     */
    public interface EditEmoticonListener {

        int addEmoticon = 0;
        int delEmoticon = 1;
        int sendEmoticon = 2;

        /**
         * Do edit emoticon action.
         *
         * @param type
         *            action type
         * @param emotionName
         *            the coding of emoticon
         */
        void doAction(int type, String emotionName);
    }

    private void startDelEmoticon() {
        MmsLog.d(TAG, "Delete one emoticon.");
        if (mDelEmoticonThread != null) {
            synchronized (mDelEmoticonThread) {
                stopDelEmoticon();
            }
        }
        mNeedQuickDelete = true;
        mDelEmoticonThread = new DelEmoticonThread();
        synchronized (mDelEmoticonThread) {
            mDelEmoticonThread.start();
        }
    }

    private void stopDelEmoticon() {
        MmsLog.d(TAG, "Stop quick delete.");
        if (mDelEmoticonThread == null) {
            mNeedQuickDelete = false;
            return;
        }
        synchronized (mDelEmoticonThread) {
            mDelEmoticonThread.stopThread();
            mDelEmoticonThread = null;
        }
    }

    private class DelEmoticonThread extends Thread {
        private boolean mStopThread = false;

        public void stopThread() {
            mStopThread = true;
        }

        @Override
        public void run() {
            synchronized (mObject) {
                try {
                    MmsLog.d(TAG, "Wait for quick delete.");
                    mObject.wait(1000);
                } catch (InterruptedException e) {
                    MmsLog.d(TAG, "Wait for quick delete.InterruptedException");
                }
            }
            Object object = new Object();
            if (mNeedQuickDelete) {
                MmsLog.d(TAG, "Start quick delete. mStopThread = " + mStopThread);
                while (!mStopThread) {
                    /// M: fix bug ALPS00528173, solute Quick delete characters. @{
                    mHandler.removeCallbacks(mDeleteRunnable);
                    mHandler.postDelayed(mDeleteRunnable, 10);
                    /// @}
                    synchronized (object) {
                        try {
                            object.wait(100);
                        } catch (InterruptedException e) {
                            MmsLog.d(TAG, "DelEmoticonThread.InterruptedException");
                        }
                    }
                }
            }
        }
    }

    /// M: fix bug ALPS00528173, solute Quick delete characters. @{
    private Runnable mDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            MmsLog.d(TAG, "Quick delete emoticon characters.");
            mListener.doAction(EditEmoticonListener.delEmoticon, "");
        }
    };
    /// @}

    /**
     * Display default emoticon page in common version.
     */
    private void addDefaultPanel() {
        mColumnArray = mContext.getResources().getIntArray(R.array.emoticon_column);
        mOrientation = mContext.getResources().getConfiguration().orientation;
        if (mScrollLayout.getChildCount() != 0) {
            mScrollLayout.removeAllViews();
        }
        mScrollLayout.setDefaultScreen(mDefaultIndex);
        mOrientation = mContext.getResources().getConfiguration().orientation;
        mDotFirst.setVisibility(View.GONE);
        mDotSec.setVisibility(View.GONE);
        mDotThird.setVisibility(View.GONE);
        mDotForth.setVisibility(View.GONE);
        RadioButton[] DotGroup = new RadioButton[4];
        DotGroup[0] = mDotFirst;
        DotGroup[1] = mDotSec;
        DotGroup[2] = mDotThird;
        DotGroup[3] = mDotForth;
        int num = calculateDefaultPageCount(mOrientation);
        for (int i = 0; i < num; i++) {
            addDefaultPage(i);
            DotGroup[i].setVisibility(View.VISIBLE);
        }
        mScrollLayout.setToScreen(mDefaultIndex);
        mScrollLayout.setOnScrollToScreen(new OnScrollToScreenListener() {
            @Override
            public void doAction(int whichScreen) {
                mDefaultIndex = whichScreen;
                if (whichScreen == 0) {
                    mDotFirst.setChecked(true);
                } else if (whichScreen == 1) {
                    mDotSec.setChecked(true);
                } else if (whichScreen == 2) {
                    mDotThird.setChecked(true);
                } else {
                    mDotForth.setChecked(true);
                }
            }
        });
    }

    /**
     * Display gift emoticon page in common version.
     */
    private void addGiftPanel() {
        mColumnArray = mContext.getResources().getIntArray(R.array.emoticon_column);
        mOrientation = mContext.getResources().getConfiguration().orientation;
        if (mScrollLayout.getChildCount() != 0) {
            mScrollLayout.removeAllViews();
        }
        mScrollLayout.setDefaultScreen(mGiftIndex);
        mOrientation = mContext.getResources().getConfiguration().orientation;
        mDotFirst.setVisibility(View.GONE);
        mDotSec.setVisibility(View.GONE);
        mDotThird.setVisibility(View.GONE);
        mDotForth.setVisibility(View.GONE);
        RadioButton[] DotGroup = new RadioButton[4];
        DotGroup[0] = mDotFirst;
        DotGroup[1] = mDotSec;
        DotGroup[2] = mDotThird;
        DotGroup[3] = mDotForth;
        int num = calculateGiftPageCount(mOrientation);
        for (int i = 0; i < num; i++) {
            addGiftPage(i);
            DotGroup[i].setVisibility(View.VISIBLE);
        }
        mScrollLayout.setToScreen(mGiftIndex);
        mScrollLayout.setOnScrollToScreen(new OnScrollToScreenListener() {
            @Override
            public void doAction(int whichScreen) {
                mGiftIndex = whichScreen;
                if (whichScreen == 0) {
                    mDotFirst.setChecked(true);
                } else if (whichScreen == 1) {
                    mDotSec.setChecked(true);
                } else if (whichScreen == 2) {
                    mDotThird.setChecked(true);
                } else {
                    mDotForth.setChecked(true);
                }
            }
        });
    }

    /**
     * Return the resource id array of the default emoticon page at index.
     *
     * @param index
     *            the visiable page index of default emoticon page.
     * @return the resource id array.
     */
    private int[] getDefaultIconArray(int index) {
        int[] source = MessageConsts.defaultIconArr;
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * mPortEmotionColumnNumber;
        } else {
            onePage = mColumnArray[1] * mLandEmotionColumnNumber;
        }
        int[] arr = new int[onePage];
        for (int i = 0; i < onePage; i++) {
            int index1 = index * onePage + i;
            if (index1 >= source.length) {
                break;
            }
            arr[i] = source[index * onePage + i];
        }
        return arr;
    }

    /**
     * Return the resource id array of the gift emoticon page at index.
     *
     * @param index
     *            the visiable page index of gift emoticon page.
     * @return the resource id array.
     */
    private int[] getGiftIconArray(int index) {
        int[] source = MessageConsts.giftIconArr;
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * mPortEmotionColumnNumber;
        } else {
            onePage = mColumnArray[1] * mLandEmotionColumnNumber;
        }
        int[] arr = new int[onePage];
        for (int i = 0; i < onePage; i++) {
            int index1 = index * onePage + i;
            if (index1 >= source.length) {
                break;
            }
            arr[i] = source[index * onePage + i];
        }
        return arr;
    }

    /**
     * Return the coding of the default emoticon.
     *
     * @param position
     *            the position of gridview.
     * @return the coding of the default emoticon.
     */
    private String getDefaultName(int position) {
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * mPortEmotionColumnNumber;
        } else {
            onePage = mColumnArray[1] * mLandEmotionColumnNumber;
        }
        if (position >= 20) {
            return null;
        }
        int index = position + onePage * mDefaultIndex;
        if (index >= mDefaultName.length) {
            return null;
        }
        return mDefaultName[index];
    }

    /**
     * Return the coding of the gift emoticon.
     *
     * @param position
     *            the position of gridview.
     * @return the coding of the gift emoticon.
     */
    private String getGiftName(int position) {
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * mPortEmotionColumnNumber;
        } else {
            onePage = mColumnArray[1] * mLandEmotionColumnNumber;
        }
        if (position >= 20) {
            return null;
        }
        int index = position + onePage * mGiftIndex;
        if (index >= mGiftName.length) {
            return null;
        }
        return mGiftName[index];
    }

    /**
     * Create the child of default emoticon page, and add it to parent.
     *
     * @param index
     *            the index of child.
     */
    private void addDefaultPage(int index) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.default_emoticon_flipper,
                mScrollLayout, false);
        GridView gridView = (GridView) v.findViewById(R.id.gv_default_emoticon_gridview);
        int height;
        if (mOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_common_port_height);
            } else {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_port_height);
            }
        } else {
            if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_common_lan_height);
            } else {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_lan_height);
            }
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
        mSharePanelMain.setLayoutParams(params);
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            gridView.setNumColumns(mColumnArray[0]);
        } else {
            gridView.setNumColumns(mColumnArray[1]);
        }
        EmoticonAdapter adapter = new EmoticonAdapter(getDefaultIconArray(index));
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = getDefaultName(position);
                if (TextUtils.isEmpty(name)) {
                    return;
                }
                mListener.doAction(EditEmoticonListener.addEmoticon, name);
            }
        });
        mScrollLayout.addView(v);
    }

    /**
     * Create the child of gift emoticon page, and add it to parent.
     *
     * @param index
     *            the index of child.
     */
    private void addGiftPage(int index) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.gift_emoticon_flipper,
                mScrollLayout, false);
        GridView gridView = (GridView) v.findViewById(R.id.gv_gift_emoticon_gridview);
        int height;
        if (mOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_common_port_height);
            } else {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_port_height);
            }
        } else {
            if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_common_lan_height);
            } else {
                height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_lan_height);
            }
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
        mSharePanelMain.setLayoutParams(params);
        if (mOrientation != Configuration.ORIENTATION_PORTRAIT) {
            gridView.setNumColumns(mColumnArray[1]);
        } else {
            gridView.setNumColumns(mColumnArray[0]);
        }
        EmoticonAdapter adapter = new EmoticonAdapter(getGiftIconArray(index));
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = getGiftName(position);
                if (TextUtils.isEmpty(name)) {
                    return;
                }
                mListener.doAction(EditEmoticonListener.addEmoticon, name);
            }
        });
        mScrollLayout.addView(v);
    }
    /**
     * Return the count of default emoticon page.
     *
     * @param orientation
     *            The screen orientation.
     * @return The count of default emoticon page.
     */
    private int calculateDefaultPageCount(int orientation) {
        int onePage;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * mPortEmotionColumnNumber;
        } else {
            onePage = mColumnArray[1] * mLandEmotionColumnNumber;
        }
        int total = MessageConsts.defaultIconArr.length;
        int count = total / onePage;
        if (total > count * onePage) {
            count++;
        }
        return count;
    }
    /**
     * Return the count of gift emoticon page.
     *
     * @param orientation
     *            The screen orientation.
     * @return The count of gift emoticon page.
     */
    private int calculateGiftPageCount(int orientation) {
        int onePage;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * mPortEmotionColumnNumber;
        } else {
            onePage = mColumnArray[1] * mLandEmotionColumnNumber;
        }
        int total = MessageConsts.giftIconArr.length;
        int count = total / onePage;
        if (total > count * onePage) {
            count++;
        }
        return count;
    }
}
