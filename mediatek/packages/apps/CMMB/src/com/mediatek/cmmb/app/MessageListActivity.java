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

import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.MBBMSStore.EB;
import com.mediatek.notification.NotificationManagerPlus;

public class MessageListActivity extends ListActivity implements OnItemClickListener, OnItemLongClickListener {
    private static final String TAG = "MessageListActivity";
    private static final boolean LOG = true;

    private static final String[] PROJECTION = new String[] { EB.Broadcast.ID, EB.Broadcast.LEVEL, EB.Broadcast.MESSAGE,
            EB.Broadcast.RECEIVE_TIME, EB.Broadcast.HAS_READ };

    private static final int COL_ID = 0;
    private static final int COL_LEVEL = 1;
    private static final int COL_MESSAGE = 2;
    private static final int COL_RECEIVE_TIME = 3;
    private static final int COL_HAS_READ = 4;

    private ModeSwitchManager mModeSwitchManager;
    private NotificationManagerPlus mNMP;
    private ListAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ebm_list);

        Cursor cursor = getContentResolver().query(MBBMSStore.EB.Broadcast.CONTENT_URI, PROJECTION, null, null,
                MBBMSStore.EB.BroadcastColumns.RECEIVE_TIME + " desc");
        mAdapter = new ListAdapter(this, R.layout.ebm_item, cursor, new String[] {}, new int[] {});
        getListView().setAdapter(mAdapter);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        mModeSwitchManager = new ModeSwitchManager(this, null, savedInstanceState);
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.changeCursor(null);
    }

    class ListAdapter extends SimpleCursorAdapter {

        public ListAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            ViewHolder holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.message = (TextView) view.findViewById(R.id.message);
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.panel = (RelativeLayout) view.findViewById(R.id.panel);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.id = cursor.getInt(COL_ID);
            int level = cursor.getInt(COL_LEVEL);
            holder.icon.setImageDrawable(getResources().getDrawable(Utils.getEBLevelIcon(level)));
            holder.title.setText(Utils.getEBLevelString(MessageListActivity.this, level));
            holder.message.setText(cursor.getString(COL_MESSAGE));
            holder.time.setText(Utils.getFormatDate(MessageListActivity.this, cursor.getLong(COL_RECEIVE_TIME)));
            int read = cursor.getInt(COL_HAS_READ);
            if (read == 1) {
                holder.panel.setBackgroundResource(R.drawable.background_read);
            } else {
                holder.panel.setBackgroundResource(R.drawable.background_unread);
            }
            if (LOG) {
                Log.v(TAG, "bindView() id=" + holder.id + ", read=" + read + ", " + holder);
            }
        }

    }

    private class ViewHolder {
        public int id;
        public ImageView icon;
        public TextView title;
        public TextView message;
        public TextView time;
        public RelativeLayout panel;

        @Override
        public String toString() {
            return "ViewHolder(id=" + id + ")";
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Uri data = ContentUris.withAppendedId(MBBMSStore.EB.Broadcast.CONTENT_URI, holder.id);
        Intent intent = new Intent();
        intent.setClass(MessageListActivity.this, MessageDetailActivity.class);
        intent.setData(data);
        startActivity(intent);
        if (LOG) {
            Log.v(TAG, "onItemClick(" + view + ", " + position + ", " + id + ") " + holder);
        }
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        new Builder(this).setTitle(holder.message.getText()).setItems(new String[] { getString(R.string.delete) },
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialogInterface, int which) {
                        EBManager.delete(MessageListActivity.this.getContentResolver(), holder.id);
                    }

                }).create().show();
        if (LOG) {
            Log.v(TAG, "onItemLongClick(" + view + ", " + position + ", " + id + ") " + holder);
        }
        return true;
    }

    private static final int MENU_ITEM_DELETE_ALL = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ITEM_DELETE_ALL, 0, R.string.delete_all);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_DELETE_ALL:
            EBManager.clear(getContentResolver());
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

}
