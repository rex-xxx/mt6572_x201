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

package com.android.mms.ui;

import android.app.ListActivity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.mms.R;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ipmessage.IpMessageConsts;

public class SettingListActivity extends ListActivity {
    private boolean mIsWithIpMsg = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.menu_preferences));
        setContentView(R.layout.setting_list);
        String strIpMsg = IpMessageUtils.getResourceManager(SettingListActivity.this).getSingleString(
            IpMessageConsts.string.ipmsg_ip_message);
        if (TextUtils.isEmpty(strIpMsg)) {
            strIpMsg = " ";
        }
        if (IpMessageUtils.getServiceManager(SettingListActivity.this).isFeatureSupported(
            IpMessageConsts.FeatureId.APP_SETTINGS)) {
            String[] settingList = new String[] {strIpMsg, getResources().getString(R.string.pref_setting_sms),
                getResources().getString(R.string.pref_setting_mms),
                getResources().getString(R.string.pref_setting_notification),
                getResources().getString(R.string.pref_setting_general)};
            setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, settingList));
            mIsWithIpMsg = true;
        } else {
            String[] settingListWithoutIpMsg = new String[] {getResources().getString(R.string.pref_setting_sms),
                getResources().getString(R.string.pref_setting_mms),
                getResources().getString(R.string.pref_setting_notification),
                getResources().getString(R.string.pref_setting_general)};
            setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, settingListWithoutIpMsg));
        }
    }

    @Override
    protected void onListItemClick(ListView arg0, View arg1, int arg2, long arg3) {
        if (mIsWithIpMsg) {
            switch (arg2) {
            case 0:
                Intent systemSettingsIntent = new Intent(IpMessageConsts.RemoteActivities.SYSTEM_SETTINGS);
                IpMessageUtils.startRemoteActivity(SettingListActivity.this, systemSettingsIntent);
                break;
            case 1:
                Intent smsPreferenceIntent = new Intent(SettingListActivity.this, SmsPreferenceActivity.class);
                startActivity(smsPreferenceIntent);
                break;
            case 2:
                Intent mmsPreferenceIntent = new Intent(SettingListActivity.this, MmsPreferenceActivity.class);
                startActivity(mmsPreferenceIntent);
                break;
            case 3:
                Intent notificationPreferenceIntent = new Intent(SettingListActivity.this,
                        NotificationPreferenceActivity.class);
                startActivity(notificationPreferenceIntent);
                break;
            case 4:
                Intent generalPreferenceIntent = new Intent(SettingListActivity.this, GeneralPreferenceActivity.class);
                startActivity(generalPreferenceIntent);
                break;
            default:
                break;
            }
        } else {
            switch (arg2) {
            case 0:
                Intent smsPreferenceIntent = new Intent(SettingListActivity.this, SmsPreferenceActivity.class);
                startActivity(smsPreferenceIntent);
                break;
            case 1:
                Intent mmsPreferenceIntent = new Intent(SettingListActivity.this, MmsPreferenceActivity.class);
                startActivity(mmsPreferenceIntent);
                break;
            case 2:
                Intent notificationPreferenceIntent = new Intent(SettingListActivity.this,
                        NotificationPreferenceActivity.class);
                startActivity(notificationPreferenceIntent);
                break;
            case 3:
                Intent generalPreferenceIntent = new Intent(SettingListActivity.this, GeneralPreferenceActivity.class);
                startActivity(generalPreferenceIntent);
                break;
            default:
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // The user clicked on the Messaging icon in the action bar. Take them back from
            // wherever they came from
            finish();
            return true;
        default:
            break;
        }
        return false;
    }
}
