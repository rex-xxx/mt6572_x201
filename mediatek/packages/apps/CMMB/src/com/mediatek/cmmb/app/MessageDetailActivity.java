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

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.mbbms.MBBMSStore.EB;
import com.mediatek.notification.NotificationManagerPlus;

public class MessageDetailActivity extends Activity {
    private static final String TAG = "MessageDetailActivity";
    private static final boolean LOG = true;

    private static final String[] PROJECTION = new String[] { EB.Broadcast.ID, EB.Broadcast.LEVEL, EB.Broadcast.MESSAGE,
            EB.Broadcast.RECEIVE_TIME };

    private static final int COL_ID = 0;
    private static final int COL_LEVEL = 1;
    private static final int COL_MESSAGE = 2;
    private static final int COL_RECEIVE_TIME = 3;

    private Uri mUri;
    private ModeSwitchManager mModeSwitchManager;
    private NotificationManagerPlus mNMP;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ebm_detail_layout);
        mUri = getIntent().getData();
        if (LOG) {
            Log.v(TAG, "mUri=" + mUri);
        }
        Cursor cursor = getContentResolver().query(mUri, PROJECTION, null, null, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int level = cursor.getInt(COL_LEVEL);
                String message = cursor.getString(COL_MESSAGE);
                long time = cursor.getLong(COL_RECEIVE_TIME);
                // find view
                TextView txtMessage = (TextView) findViewById(R.id.message);
                TextView txtTime = (TextView) findViewById(R.id.time);
                ImageView icon = (ImageView) findViewById(R.id.icon);
                // set value
                setTitle(Utils.getEBLevelString(this, level));
                icon.setImageResource(Utils.getEBLevelIcon(level));
                txtMessage.setText(message);
                txtTime.setText(Utils.getFormatDateTime(this, time));
                // read it
                Utils.readBroadcast(this, mUri);
            } else {
                finish();
            }
            cursor.close();
        }
        mModeSwitchManager = new ModeSwitchManager(this, null, savedInstanceState);
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
    }

    private static final int MENU_ITEM_DELETE = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_DELETE:
            deleteMessage();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteMessage() {
        try {
            long id = Long.parseLong(mUri.getLastPathSegment());
            EBManager.delete(getContentResolver(), id);
            finish();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mModeSwitchManager.onSaveInstanceState(state);
    }

    @Override
    public void onStart() {
        super.onStart();
        mModeSwitchManager.onActivityStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mModeSwitchManager.onActivityStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNMP.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNMP.startListening();
    }
}
