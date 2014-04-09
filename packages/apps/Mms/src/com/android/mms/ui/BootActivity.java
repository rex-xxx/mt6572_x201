/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.mms.data.Conversation;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;

import com.mediatek.encapsulation.MmsLog;
import com.mediatek.ipmsg.util.IpMessageUtils;
/// M: add for ipmessage
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;

/**
 * M: BootActivity
 */
public class BootActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MmsLog.d("BootActivity", "bootActivity: onCreate");
        MmsConfig.setSimCardInfo(0);

        Intent intent = getIntent();
        if (IpMessageConsts.ACTION_GROUP_NOTIFICATION_CLICKED.equals(intent.getAction())) {
            long threadId = intent.getLongExtra(RemoteActivities.KEY_THREAD_ID, 0);
            boolean isImportant = intent.getBooleanExtra(RemoteActivities.KEY_BOOLEAN, false);

            Intent it = new Intent(RemoteActivities.CHAT_DETAILS_BY_THREAD_ID);
            it.putExtra(RemoteActivities.KEY_THREAD_ID, threadId);
            it.putExtra(RemoteActivities.KEY_BOOLEAN, isImportant);
            it.putExtra(RemoteActivities.KEY_NEED_NEW_TASK, false);
            IpMessageUtils.startRemoteActivity(this, it);

            Conversation conv = Conversation.get(this, threadId, true);
            conv.markAsSeen();

            finish();
            return;
        }
            enterMms();
    }

    private void enterMms() {
        MmsLog.d("BootActivity", "bootActivity enter MMS");
        Intent intent;
        finish();
        boolean dirMode;
        dirMode = MmsConfig.getMmsDirMode();
        if (MmsConfig.getFolderModeEnabled() && dirMode) {
            intent = new Intent(this, FolderViewList.class);
            intent.putExtra("floderview_key", FolderViewList.OPTION_INBOX);// show inbox by default
        } else {
            intent = new Intent(this, ConversationList.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);///M: changed for alps00437708
        startActivity(intent);
    }

    ///M: add for cmcc Long Press guide
    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && (data != null && data.getBooleanExtra("LongPressGuide", false))) {
            enterMms();
        }
    }
}
