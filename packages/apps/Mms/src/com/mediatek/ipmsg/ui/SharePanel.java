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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.ui.LevelControlLayout;
import com.android.mms.ui.LevelControlLayout.OnScrollToScreenListener;
import com.android.mms.util.MessageConsts;

import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.FeatureId;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;
import com.mediatek.encapsulation.MmsLog;
public class SharePanel extends LinearLayout {

    private Handler mHandler;
    private Context mContext;
    private View mConvertView;
    private LevelControlLayout mScrollLayout;
    private LinearLayout mSharePanelMain;
    private RadioButton mDotFirst;
    private RadioButton mDotSec;

    private int mOrientation;
    private int[] mColumnArray;
    private int mScreenIndex;

    /// M: MMS
    public static final int TAKE_PICTURE            = 0;
    public static final int RECORD_VIDEO            = 1;
    public static final int RECORD_SOUND            = 2;
    public static final int ADD_VCARD               = 3;
    public static final int ADD_IMAGE               = 4;
    public static final int ADD_VIDEO               = 5;
    public static final int ADD_SOUND               = 6;
    public static final int ADD_VCALENDAR           = 7;
    public static final int ADD_SLIDESHOW           = 8;

    /// M: IP message
    public static final int IPMSG_TAKE_PHOTO        = 100;
    public static final int IPMSG_RECORD_VIDEO      = 101;
    public static final int IPMSG_RECORD_AUDIO      = 102;
    public static final int IPMSG_DRAW_SKETCH       = 103;
    public static final int IPMSG_CHOOSE_PHOTO      = 104;
    public static final int IPMSG_CHOOSE_VIDEO      = 105;
    public static final int IPMSG_CHOOSE_AUDIO      = 106;
    public static final int IPMSG_SHARE_LOCATION    = 107;
    public static final int IPMSG_SHARE_CONTACT     = 108;
    public static final int IPMSG_SHARE_CALENDAR    = 109;
    public static final int IPMSG_SHARE_SLIDESHOW   = 110;

    private static final int[] MMS_ACTIONS = {
        TAKE_PICTURE, RECORD_VIDEO, RECORD_SOUND, ADD_VCARD,
        ADD_IMAGE, ADD_VIDEO, ADD_SOUND, ADD_VCALENDAR,
        ADD_SLIDESHOW};
    private static final int[] IP_MESSAGE_ACTIONS = {
        IPMSG_TAKE_PHOTO, IPMSG_RECORD_VIDEO, IPMSG_RECORD_AUDIO, IPMSG_DRAW_SKETCH,
        IPMSG_CHOOSE_PHOTO, IPMSG_CHOOSE_VIDEO, IPMSG_CHOOSE_AUDIO, IPMSG_SHARE_LOCATION,
        IPMSG_SHARE_CONTACT, IPMSG_SHARE_CALENDAR, IPMSG_SHARE_SLIDESHOW};
    private static final int[] IP_MESSAGE_ACTIONS_WITHOUT_SKETCH_AND_LOCATION = {
        IPMSG_TAKE_PHOTO, IPMSG_RECORD_VIDEO, IPMSG_RECORD_AUDIO, IPMSG_SHARE_CONTACT,
        IPMSG_CHOOSE_PHOTO, IPMSG_CHOOSE_VIDEO, IPMSG_CHOOSE_AUDIO, IPMSG_SHARE_CALENDAR,
        IPMSG_SHARE_SLIDESHOW};

    public static final String SHARE_ACTION = "shareAction";
    private static final String TAG = "Mms/ipmsg/SharePanel";
    private String[] mSource;

    public SharePanel(Context context) {
        super(context);
        mContext = context;
    }

    public SharePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        if (MmsConfig.getIpMessagServiceId(context) == IpMessageServiceId.NO_SERVICE) {
            mConvertView = inflater.inflate(R.layout.share_common_panel, this, true);
        } else {
            mConvertView = inflater.inflate(R.layout.share_panel, this, true);
        }
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mScrollLayout = (LevelControlLayout) mConvertView.findViewById(R.id.share_panel_zone);
        mSharePanelMain = (LinearLayout) mConvertView.findViewById(R.id.share_panel_main);
        mDotFirst = (RadioButton) mConvertView.findViewById(R.id.rb_dot_first);
        mDotSec = (RadioButton) mConvertView.findViewById(R.id.rb_dot_sec);
        resetShareItem();
    }

    public void resetShareItem() {
        mOrientation = getResources().getConfiguration().orientation;
        if (mScrollLayout.getChildCount() != 0) {
            mScrollLayout.removeAllViews();
        }
        addSharePage(0);
        addSharePage(1);
        mDotSec.setVisibility(View.VISIBLE);
        mDotFirst.setVisibility(View.VISIBLE);
        mDotFirst.setChecked(true);
        mScrollLayout.setOnScrollToScreen(new OnScrollToScreenListener() {
            @Override
            public void doAction(int whichScreen) {
                mScreenIndex = whichScreen;
                if (whichScreen == 0) {
                    mDotFirst.setChecked(true);
                } else {
                    mDotSec.setChecked(true);
                }
            }
        });
        mScrollLayout.setDefaultScreen(mScreenIndex);
        mScrollLayout.autoRecovery();
    }

    private void addSharePage(int index) {
        mColumnArray = getResources().getIntArray(R.array.share_column);
        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.share_flipper, mScrollLayout, false);
        GridView gridView = (GridView) v.findViewById(R.id.gv_share_gridview);
        android.view.ViewGroup.LayoutParams params = mSharePanelMain.getLayoutParams();
        if (mOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
                params.height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_common_port_height);
            } else {
                params.height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_port_height);
            }
        } else {
            if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
                params.height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_common_lan_height);
            } else {
                params.height = mContext.getResources().getDimensionPixelOffset(R.dimen.share_panel_lan_height);
            }
        }
        mSharePanelMain.setLayoutParams(params);
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            gridView.setNumColumns(mColumnArray[0]);
        } else {
            gridView.setNumColumns(mColumnArray[1]);
        }
        ShareAdapter adapter = new ShareAdapter(getLableArray(index), getIconArray(index));
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Message msg = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                if (!IpMessageUtils.getIpMessagePlugin(mContext).isActualPlugin()) {
                    TextView text = (TextView) view.findViewById(R.id.tv_share_name);
                    MmsLog.d(TAG, "text = " + text.getText());
                    int actionPosition = 0;
                    if (TextUtils.isEmpty(text.getText())) {
                        actionPosition = -1;
                    } else {
                        for (int i = 0; i < mSource.length; i++) {
                            MmsLog.d(TAG, "i = " + i + "source[i]=" + mSource[i]);
                            if (text.getText().equals(mSource[i])) {
                                actionPosition = i;
                                break;
                            }
                        }
                    }
                    //int actionPosition = getActionId(position);
                    if (actionPosition == -1 || actionPosition >= MMS_ACTIONS.length ) {
                        return;
                    }
                    bundle.putInt(SHARE_ACTION, MMS_ACTIONS[actionPosition]);
                } else if (IpMessageUtils.getServiceManager(mContext).isFeatureSupported(FeatureId.SKETCH)
                        && IpMessageUtils.getServiceManager(mContext).isFeatureSupported(FeatureId.LOCATION)) {
                    int actionPosition = getActionId(position);
                    if (actionPosition >= IP_MESSAGE_ACTIONS.length) {
                        return;
                    }
                    bundle.putInt(SHARE_ACTION, IP_MESSAGE_ACTIONS[actionPosition]);
                } else {
                    int actionPosition = getActionId(position);
                    if (actionPosition >= IP_MESSAGE_ACTIONS_WITHOUT_SKETCH_AND_LOCATION.length) {
                        return;
                    }
                    bundle.putInt(SHARE_ACTION, IP_MESSAGE_ACTIONS_WITHOUT_SKETCH_AND_LOCATION[actionPosition]);
                }
                msg.setData(bundle);
                msg.what = MessageConsts.ACTION_SHARE;
                mHandler.sendMessage(msg);
            }
        });
        mScrollLayout.addView(v);
    }

    private String[] getLableArray(int index) {
        if (IpMessageUtils.getIpMessagePlugin(mContext).isActualPlugin()
                && (IpMessageUtils.getServiceManager(mContext).isFeatureSupported(FeatureId.SKETCH)
                        && IpMessageUtils.getServiceManager(mContext).isFeatureSupported(FeatureId.LOCATION))) {
            mSource = IpMessageUtils.getResourceManager(mContext)
                .getStringArray(IpMessageConsts.array.ipmsg_share_string_array);
        } else {
            mSource = getResources().getStringArray(R.array.share_string_array);
        }
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * 2;
        } else {
            onePage = mColumnArray[1];
        }
        if (index == 0) {
            String[] index0 = new String[onePage];
            for (int i = 0; i < onePage; i++) {
                index0[i] = mSource[i];
            }
            return index0;
        } else {
            int count = mSource.length - onePage;
            String[] index1 = new String[count];
            for (int i = 0; i < count; i++) {
                index1[i] = mSource[onePage + i];
            }
            return index1;
        }
    }

    private int[] getIconArray(int index) {
        int[] source = MessageConsts.shareIconArr;
        if (IpMessageUtils.getIpMessagePlugin(mContext).isActualPlugin()
                && (IpMessageUtils.getServiceManager(mContext).isFeatureSupported(FeatureId.SKETCH)
                        && IpMessageUtils.getServiceManager(mContext).isFeatureSupported(FeatureId.LOCATION))) {
            source = MessageConsts.ipmsgShareIconArr;
        }
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * 2;
        } else {
            onePage = mColumnArray[1];
        }
        if (index == 0) {
            int[] index0 = new int[onePage];
            for (int i = 0; i < onePage; i++) {
                index0[i] = source[i];
            }
            return index0;
        } else {
            int count = source.length - onePage;
            int[] index1 = new int[count];
            for (int i = 0; i < count; i++) {
                index1[i] = source[onePage + i];
            }
            return index1;
        }
    }

    private int getActionId(int position) {
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * 2;
        } else {
            onePage = mColumnArray[1];
        }
        if (mScreenIndex == 0) {
            return position;
        } else {
            return onePage + position;
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void recycleView() {
        if (mScrollLayout != null && mScrollLayout.getChildCount() != 0) {
            mScrollLayout.removeAllViews();
        }
    }

    private class ShareAdapter extends BaseAdapter {

        private String[] mStringArray;
        private int[] mIconArray;

        public ShareAdapter(String[] stringArray, int[] iconArray) {
            mStringArray = stringArray;
            mIconArray = iconArray;
        }

        @Override
        public int getCount() {
            int count = 0;
            if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                count = mColumnArray[0] * 2;
            } else {
                count = mColumnArray[1];
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                if (MmsConfig.getIpMessagServiceId(mContext) == IpMessageServiceId.NO_SERVICE) {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.share_grid_common_item, null);
                } else {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.share_grid_item, null);
                }
                convertView.setTag(convertView);
            } else {
                convertView = (View) convertView.getTag();
            }

            TextView text = (TextView) convertView.findViewById(R.id.tv_share_name);
            ImageView img = (ImageView) convertView.findViewById(R.id.iv_share_icon);
            if (position < mStringArray.length) {
                text.setText(mStringArray[position]);
                img.setImageResource(mIconArray[position]);
            }
            return convertView;
        }
    }
}
