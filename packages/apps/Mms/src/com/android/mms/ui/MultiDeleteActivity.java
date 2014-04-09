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

/*
 * Copyright (C) 2008 Esmertec AG.
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
package com.android.mms.ui;

import static com.android.mms.ui.MessageListAdapter.PROJECTION;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.android.mms.R;
import com.android.mms.data.WorkingMessage;

import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.util.DraftCache;
import com.android.mms.util.ThreadCountManager;

import com.mediatek.mms.ext.IMmsMultiDeleteAndForwardHost;
import com.mediatek.mms.ext.IMmsMultiDeleteAndForward;
import com.mediatek.mms.ext.MmsMultiDeleteAndForwardImpl;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import com.mediatek.ipmsg.util.IpMessageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/// M: add for ipmessage
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.AndroidException;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.android.mms.data.ContactList;
import com.android.mms.MmsConfig;
import com.google.android.mms.pdu.PduHeaders;
/// M: add for one mms forward. @{
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.MmsException;
/// @}
import com.mediatek.mms.ipmessage.INotificationsListener;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.DownloadAttachStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.FeatureId;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
import com.mediatek.mms.ipmessage.IpMessageConsts.SelectContactType;
import com.mediatek.mms.ipmessage.message.IpAttachMessage;
import com.mediatek.mms.ipmessage.message.IpMessage;

/// M: add for multi-forward
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * M: MultiDeleteActivity
 */
public class MultiDeleteActivity extends ListActivity implements IMmsMultiDeleteAndForwardHost, INotificationsListener {

    public static final String TAG = "Mms/MultiDeleteActivity";
    public static final String IPMSG_TAG = "Mms/ipmsg/MultiDeleteActivity";

    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;
    private static final int DELETE_MESSAGE_TOKEN = 9700;

    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    //add for multi_forward
    public static final String FORWARD_MESSAGE = "forwarded_message";

    private ListView mMsgListView; // ListView for messages in this conversation
    public MessageListAdapter mMsgListAdapter; // and its corresponding ListAdapter

    private boolean mPossiblePendingNotification; // If the message list has changed, we may have
    // a pending notification to deal with.
    private long mThreadId; // Thread we are working in
    private Conversation mConversation; // Conversation we are working in
    private BackgroundQueryHandler mBackgroundQueryHandler;
    private ThreadCountManager mThreadCountManager = ThreadCountManager.getInstance();

    private MenuItem mSelectAll;
    private MenuItem mCancelSelect;
    private MenuItem mDelete;
//    private TextView mActionBarText;

    private boolean mIsSelectedAll;
    private int mDeleteRunningCount = 0; // The count of running Message-deleting
    /// M: Operator Plugin
    private IMmsMultiDeleteAndForward mMmsDeleteAndForwardPlugin  = null;

    /// M: add for ipmessage
    private SelectActionMode mSelectActionMode;
    private ActionMode mSelectMode;
    private Button mChatSelect;

    private String mForwardMsgIds;
    public static final String IPMSG_IDS = "forward_ipmsg_ids";
    public static final int SHOW_DOWNLOAD_PROGRESS_BAR = 1000;
    public static final int HIDE_DOWNLOAD_PROGRESS_BAR = 1001;
    public static final int UPDATE_SELECTED_COUNT = 1002;

    private static final int REQUEST_CODE_FORWARD = 1000;

    /// M: add for ipmessage, record the ipmessage id.
    private HashSet<Long> mSelectedIpMessageIds = new HashSet<Long>();
    private int mMmsNotificationCount = 0;
    private boolean mMmsNotificationHasRun = false;
    private int mUnDownloadedIpMessageCount = 0;
    private int mDownloadedIpMessageStepCounter;
    private int mDownloadedIpMessageStepCounterSuccess;
    private int mDownloadedIpMessageStepCounterFail;
    private ActionMode mTempActionMode;
    private MenuItem mTempActionModeMenu;
    private HashSet<MessageItem> mSelectedIpAttachMessageItem = new HashSet<MessageItem>();
    private ProgressDialog mDownloadDialog;

    private boolean mOP01Plugin = false;
    /// M: fix bug ALPS00450886. @{
    private boolean mIsLockOrUnlockFinish = true;
    private ProgressDialog mProgressDialog;
    /// @}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /** M: add mediatek code @{ */
        initPlugin(this);
        setContentView(R.layout.multi_delete_list_screen);
        setProgressBarVisibility(false);

        /// M: add for ipmessage
        if (MmsConfig.getIpMessagServiceId(this) == IpMessageServiceId.ISMS_SERVICE) {
            MmsLog.d(IPMSG_TAG, "onCreate(): is ip service ready ?= "
                    + IpMessageUtils.getServiceManager(this).serviceIsReady());
            if (!IpMessageUtils.getServiceManager(this).serviceIsReady()) {
                MmsLog.d(IPMSG_TAG, "Turn on ipmessage service by ConversationList.");
                IpMessageUtils.getServiceManager(this).startIpService();
            }
        }

        mThreadId = getIntent().getLongExtra("thread_id", 0);
        if (mThreadId == 0) {
            MmsLog.e("TAG", "mThreadId can't be zero");
            finish();
        }
        mConversation = Conversation.get(this, mThreadId, false);
        mMsgListView = getListView();
        mMsgListView.setDivider(null);      // no divider so we look like IM conversation.
        mMsgListView.setDividerHeight(getResources().getDimensionPixelOffset(R.dimen.ipmsg_message_list_divier_height));
        initMessageList();
        initActivityState(savedInstanceState);

        setUpActionBar();
        mBackgroundQueryHandler = new BackgroundQueryHandler(getContentResolver());
        /// M: update font size, this is common feature.
        if (MmsConfig.getAdjustFontSizeEnabled()) {
            float textSize = MessageUtils.getPreferenceValueFloat(this,
                                        GeneralPreferenceActivity.TEXT_SIZE,
                                        GeneralPreferenceActivity.TEXT_SIZE_DEFAULT);
            setTextSize(textSize);
        }
        /// M: add for ipmessage
        if (MmsConfig.getIpMessagServiceId(this) > IpMessageServiceId.NO_SERVICE) {
            IpMessageUtils.addIpMsgNotificationListeners(this, this);
        }
    }

    public void setTextSize(float size) {
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setTextSize(size);
        }

        if (mMsgListView != null && mMsgListView.getVisibility() == View.VISIBLE) {
            int count = mMsgListView.getChildCount();
            for (int i = 0; i < count; i++) {
                MessageListItem item =  (MessageListItem)mMsgListView.getChildAt(i);
                if (item != null) {
                    item.setBodyTextSize(size);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mConversation.blockMarkAsRead(true);
        startMsgListQuery();
        mIsSelectedAll = false;
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged " + newConfig);
        super.onConfigurationChanged(newConfig);
    }

    /// M: new methods
    private void initPlugin(Context context) {
        try {
            mMmsDeleteAndForwardPlugin =
                    (IMmsMultiDeleteAndForward)EncapsulatedPluginManager.createPluginObject(context,
                                                                                IMmsMultiDeleteAndForward.class.getName());
            mOP01Plugin = true;
            MmsLog.d(TAG, "operator mMmsDeleteAndForwardPlugin = " + mMmsDeleteAndForwardPlugin);
        } catch (AndroidException e) {
            mMmsDeleteAndForwardPlugin = new MmsMultiDeleteAndForwardImpl(context);
            mOP01Plugin = false;
            MmsLog.d(TAG, "default mMmsDeleteAndForwardPlugin = " + mMmsDeleteAndForwardPlugin);
        }

        mMmsDeleteAndForwardPlugin.init(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMsgListAdapter != null) {
            if (mMsgListAdapter.getSelectedNumber() == mMsgListAdapter.getCount()) {
                outState.putBoolean("is_all_selected", true);
            } else if (mMsgListAdapter.getSelectedNumber() == 0) {
                return;
            } else {
                long[] checkedArray = new long[mMsgListAdapter.getSelectedNumber()];
                Iterator iter = mMsgListAdapter.getItemList().entrySet().iterator();
                int i = 0;
                while (iter.hasNext()) {
                    @SuppressWarnings("unchecked")
                    Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                    if (entry.getValue()) {
                        checkedArray[i] = entry.getKey();
                        i++;
                    }
                }
                outState.putLongArray("select_list", checkedArray);
            }

        }
    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose_multi_select_menu, menu);
        mSelectAll = menu.findItem(R.id.select_all);
        mCancelSelect = menu.findItem(R.id.cancel_select);
        mDelete = menu.findItem(R.id.delete);
        mDelete.setEnabled(false);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int selectNum = getSelectedCount();
        mActionBarText.setText(getResources().getQuantityString(
            R.plurals.message_view_selected_message_count, selectNum, selectNum));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.select_all:
            if (!mIsSelectedAll) {
                mIsSelectedAll = true;
                markCheckedState(mIsSelectedAll);
                invalidateOptionsMenu();
            }
            mDelete.setEnabled(true);
            break;
        case R.id.cancel_select:
            if (mMsgListAdapter.getSelectedNumber() > 0) {
                mIsSelectedAll = false;
                markCheckedState(mIsSelectedAll);
                invalidateOptionsMenu();
            }
            mDelete.setEnabled(false);
            break;
        case R.id.delete:
            int mSelectedNumber = mMsgListAdapter.getSelectedNumber();
            if (mSelectedNumber >= mMsgListAdapter.getCount()) {
                Long threadId = mConversation.getThreadId();
                MultiDeleteMsgListener mMultiDeleteMsgListener = new MultiDeleteMsgListener();
                confirmMultiDeleteMsgDialog(mMultiDeleteMsgListener, selectedMsgHasLocked(),
                    true, threadId, MultiDeleteActivity.this);
            } else if (mMsgListAdapter.getSelectedNumber() > 0) {
                MultiDeleteMsgListener mMultiDeleteMsgListener = new MultiDeleteMsgListener();
                confirmMultiDeleteMsgDialog(mMultiDeleteMsgListener, selectedMsgHasLocked(),
                    false, null, MultiDeleteActivity.this);
            }
            break;
        default:
            break;
        }
        return true;
    }
*/

    @Override
    protected void onListItemClick(ListView parent, View view, int position, long id) {
        if (view != null) {
            ((MessageListItem) view).onMessageListItemClick();
        }
    }

    private void initActivityState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            boolean selectedAll = savedInstanceState.getBoolean("is_all_selected");
            if (selectedAll) {
                mMsgListAdapter.setItemsValue(true, null);
                return;
            }

            long[] selectedItems = savedInstanceState.getLongArray("select_list");
            if (selectedItems != null) {
                mMsgListAdapter.setItemsValue(true, selectedItems);
            }
        }
    }

    private void setUpActionBar() {
/*
        ActionBar actionBar = getActionBar();

        ViewGroup v = (ViewGroup) LayoutInflater.from(this).inflate(
            R.layout.multi_delete_list_actionbar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
            | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        ImageButton mQuit = (ImageButton) v.findViewById(R.id.cancel_button);
        mQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                MultiDeleteActivity.this.finish();
            }
        });

        mActionBarText = (TextView) v.findViewById(R.id.select_items);
        actionBar.setCustomView(v);
*/
        mSelectActionMode = new SelectActionMode();
        mSelectMode = startActionMode(mSelectActionMode);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayUseLogoEnabled(true);
    }

    private void initMessageList() {
        if (mMsgListAdapter != null) {
            return;
        }

        String highlightString = getIntent().getStringExtra("highlight");
        Pattern highlight = highlightString == null ? null : Pattern.compile("\\b"
            + Pattern.quote(highlightString), Pattern.CASE_INSENSITIVE);

        // Initialize the list adapter with a null cursor.
        mMsgListAdapter = new MessageListAdapter(this, null, mMsgListView, true, highlight, mMmsDeleteAndForwardPlugin);
        mMsgListAdapter.mIsDeleteMode = true;
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms @{
        boolean isGroupMms = MmsPreferenceActivity.getIsGroupMmsEnabled(MultiDeleteActivity.this)
                                                && mConversation.getRecipients().size() > 1;
        mMsgListAdapter.setIsGroupConversation(isGroupMms);
        /// @}
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setItemsCanFocus(false);
        mMsgListView.setVisibility(View.VISIBLE);
    }

    private void startMsgListQuery() {
        // Cancel any pending queries
        mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);
        try {
            mBackgroundQueryHandler.postDelayed(new Runnable() {
                public void run() {
                    mBackgroundQueryHandler.startQuery(MESSAGE_LIST_QUERY_TOKEN, mThreadId,
                        mConversation.getUri(), PROJECTION, null, null, null);
                }
            }, 50);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void markCheckedState(boolean checkedState) {
        mMsgListAdapter.setItemsValue(checkedState, null);
        int count = mMsgListView.getChildCount();
        MessageListItem item = null;
        /// M: clear counter and record, re-create them.
        mImportantCount = 0;
        /// M: add for ipmessage
        mSelectedIpMessageIds.clear();
        mMmsNotificationCount = 0;
        mUnDownloadedIpMessageCount = 0;
        mSelectedIpAttachMessageItem.clear();

        Iterator iter = mMsgListAdapter.getItemList().entrySet().iterator();
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor == null) {
            MmsLog.d(TAG, "[markCheckedState] cursor is null");
            return;
        }
        int position = cursor.getPosition();
        int locked = 0;
        if (checkedState && cursor.moveToFirst()) {
            do {
                Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                if (entry.getValue()) {
                    long mMmsId = entry.getKey();
                    MessageItem m = null;
                    if (mMmsId < 0) {
                        locked = cursor.getInt(mMsgListAdapter.COLUMN_MMS_LOCKED);
                        if (cursor.getInt(mMsgListAdapter.COLUMN_MMS_MESSAGE_TYPE) ==
                                PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
                            mMmsNotificationCount++;
                        }
                    } else {
                        locked = cursor.getInt(mMsgListAdapter.COLUMN_SMS_LOCKED);
                    }
                    if (locked == 1) {
                        mImportantCount ++;
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.moveToPosition(position);
        for (int i = 0; i < count; i++) {
            item = (MessageListItem) mMsgListView.getChildAt(i);
            if (item == null || item.getMessageItem() == null) {
                continue;
            }
            /// M: add for ipmessage
            if (checkedState && item.getMessageItem().mIpMessageId > 0) {
                mSelectedIpMessageIds.add(item.getMessageItem().mMsgId);
            }
            if (checkedState &&
                item.getMessageItem().mIpMessage != null &&
                item.getMessageItem().mIpMessage instanceof IpAttachMessage) {
                IpAttachMessage ipAttachMessage = (IpAttachMessage)item.getMessageItem().mIpMessage;
                if (ipAttachMessage.isInboxMsgDownloalable()) {
                    mUnDownloadedIpMessageCount++;
                    mSelectedIpAttachMessageItem.add(item.getMessageItem());
                }
            }
            item.setSelectedBackGroud(checkedState);
        }

        updateSelectCount();
    }

    /**
     * @return the number of messages that are currently selected.
     */
    private int getSelectedCount() {
        return mMsgListAdapter.getSelectedNumber();
    }

    @Override
    public void onUserInteraction() {
        checkPendingNotification();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
          /// M: add for ipmessage, remove mark as read
//        if (hasFocus) {
//          mConversation.markAsRead();
//        }
    }

    private void checkPendingNotification() {
        if (mPossiblePendingNotification && hasWindowFocus()) {
            mConversation.markAsRead();
            mPossiblePendingNotification = false;
        }
    }

    /// M: fix bug ALPS00367594
    private HashSet<Long> mSelectedLockedMsgIds;

    /**
     * Judge weather selected messages include locked messages or not.
     * 
     * @return
     */
    private boolean selectedMsgHasLocked() {
        boolean mHasLockedMsg = false;
        if (mMsgListAdapter == null) {
            return false;
        }
        mSelectedLockedMsgIds = new HashSet<Long>();
        Iterator iter = mMsgListAdapter.getItemList().entrySet().iterator();
        Cursor cursor = mMsgListAdapter.getCursor();
        int position = cursor.getPosition();
        int locked = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                if (entry.getValue()) {
                    long mMmsId = entry.getKey();
                    MessageItem m = null;
                    if (mMmsId < 0) {
                        locked = cursor.getInt(mMsgListAdapter.COLUMN_MMS_LOCKED);
                    } else {
                        locked = cursor.getInt(mMsgListAdapter.COLUMN_SMS_LOCKED);
                    }
                    if (locked == 1) {
                        mHasLockedMsg = true;
                        mSelectedLockedMsgIds.add(mMmsId);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.moveToPosition(position);
        return mHasLockedMsg;
    }

    private boolean isMsgLocked(Map.Entry<Long, Boolean> entry) {
         if (entry == null) {
             return false;
         }
         long mMmsId = entry.getKey();
         MessageItem m = null;
         for (Long selectedMsgIds : mSelectedLockedMsgIds) {
             if (mMmsId == selectedMsgIds) {
                 return true;
             }
         }
         return false;
    }
    /// @}

    private void confirmMultiDeleteMsgDialog(final MultiDeleteMsgListener listener,
            boolean hasLockedMessages, boolean deleteThread, Long threadIds, Context context) {
        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        if (!deleteThread) {
            msg.setText(getString(R.string.confirm_delete_selected_messages));
        } else {
            listener.setDeleteThread(deleteThread);
            listener.setHasLockedMsg(hasLockedMessages);
            if (threadIds == null) {
                msg.setText(R.string.confirm_delete_all_conversations);
            } else {
                // Show the number of threads getting deleted in the confirmation dialog.
                msg.setText(context.getResources().getQuantityString(
                    R.plurals.confirm_delete_conversation, 1, 1));
            }
        }

        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        if (hasLockedMessages) {
            /// M: change the string to important if ipmessage plugin exist
            if (MmsConfig.getIpMessagServiceId(context) > IpMessageServiceId.NO_SERVICE) {
                checkbox.setText(IpMessageUtils.getResourceManager(context)
                    .getSingleString(IpMessageConsts.string.ipmsg_delete_important));
            }
            listener.setDeleteLockedMessage(checkbox.isChecked());
            checkbox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.setDeleteLockedMessage(checkbox.isChecked());
                }
            });
        } else {
            checkbox.setVisibility(View.GONE);
        }

        Cursor cursor = null;
        int smsId = 0;
        int mmsId = 0;
        cursor = context.getContentResolver().query(Sms.CONTENT_URI,
                new String[] {"max(_id)"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                smsId = cursor.getInt(0);
                MmsLog.d(TAG, "confirmMultiDeleteMsgDialog max SMS id = " + smsId);
                }
            } finally {
                cursor.close();
                cursor = null;
            }
        }
        cursor = context.getContentResolver().query(Mms.CONTENT_URI,
                new String[] {"max(_id)"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                mmsId = cursor.getInt(0);
                MmsLog.d(TAG, "confirmMultiDeleteMsgDialog max MMS id = " + mmsId);
                }
            } finally {
                cursor.close();
                cursor = null;
            }
        }
        listener.setMaxMsgId(mmsId, smsId);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.confirm_dialog_title).setIconAttribute(
            android.R.attr.alertDialogIcon).setCancelable(true).setPositiveButton(R.string.delete,
            listener).setNegativeButton(R.string.no, null).setView(contents).show();
    }

    private class MultiDeleteMsgListener implements OnClickListener {
        private boolean mDeleteLockedMessages = false;
        private boolean mDeleteThread = false;
        private boolean mHasLockedMsg = false;
        private int mMaxMmsId;
        private int mMaxSmsId;

        public MultiDeleteMsgListener() {
        }

        public void setMaxMsgId(int mmsId, int smsId) {
            mMaxMmsId = mmsId;
            mMaxSmsId = smsId;
        }

        public void setHasLockedMsg(boolean hasLockedMsg) {
            this.mHasLockedMsg = hasLockedMsg;
        }

        public void setDeleteThread(boolean deleteThread) {
            mDeleteThread = deleteThread;
        }

        public void setDeleteLockedMessage(boolean deleteLockedMessages) {
            mDeleteLockedMessages = deleteLockedMessages;
        }

        public void onClick(DialogInterface dialog, final int whichButton) {
            mBackgroundQueryHandler.setProgressDialog(DeleteProgressDialogUtil
                    .getProgressDialog(MultiDeleteActivity.this));
            mBackgroundQueryHandler.showProgressDialog();

            if (mDeleteThread) {
                if ((!mHasLockedMsg) || (mDeleteLockedMessages && mHasLockedMsg)) {
                      new Thread(new Runnable() {
                        public void run() {
                            /// M: delete ipmessage in external db
                            HashSet<Long> threads = new HashSet<Long>();
                            threads.add(mThreadId);
                            IpMessageUtils.deleteIpMessage(MultiDeleteActivity.this,
                                                            threads,
                                                            mDeleteLockedMessages,
                                                            mMaxSmsId);
                            int token = ConversationList.DELETE_CONVERSATION_TOKEN;
                            Conversation.startDelete(mBackgroundQueryHandler, token, mDeleteLockedMessages,
                            mThreadId, mMaxMmsId, mMaxSmsId);
                            DraftCache.getInstance().setDraftState(mThreadId, false);
                        }
                      }).start();
                      return;
                }
            }
            /// M: add for ipmessage
            final boolean deleteLocked = mDeleteLockedMessages;
            new Thread(new Runnable() {
                public void run() {
                    Iterator iter = mMsgListAdapter.getItemList().entrySet().iterator();
                    Uri deleteSmsUri = null;
                    Uri deleteMmsUri = null;
                    String[] argsSms = new String[mMsgListAdapter.getSelectedNumber()];
                    String[] argsMms = new String[mMsgListAdapter.getSelectedNumber()];
                    int i = 0;
                    int j = 0;
                    while (iter.hasNext()) {
                        @SuppressWarnings("unchecked")
                        Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                        if (!mDeleteLockedMessages) {
                            if (isMsgLocked(entry)) {
                                continue;
                            }
                        }
                        if (entry.getValue()) {
                            if (entry.getKey() > 0) {
                                MmsLog.i(TAG, "sms");
                                argsSms[i] = Long.toString(entry.getKey());
                                MmsLog.i(TAG, "argsSms[i]" + argsSms[i]);
                                deleteSmsUri = Sms.CONTENT_URI;
                                i++;
                            } else {
                                MmsLog.i(TAG, "mms");
                                argsMms[j] = Long.toString(-entry.getKey());
                                MmsLog.i(TAG, "argsMms[j]" + argsMms[j]);
                                deleteMmsUri = Mms.CONTENT_URI;
                                j++;
                            }
                        }
                    }
                    mBackgroundQueryHandler.setMax((deleteSmsUri != null ? 1 : 0)
                        + (deleteMmsUri != null ? 1 : 0));
                    /// M: delete ipmessage in external db
                    if (mSelectedIpMessageIds.size() > 0) {
                        long [] ids = new long[mSelectedIpMessageIds.size()];
                        int k = 0;
                        for (Long id : mSelectedIpMessageIds) {
                            ids[k++] = id;
                            MmsLog.d(TAG, "delete ipmessage, id:" + ids[k - 1]);
                        }
                        IpMessageUtils.getMessageManager(MultiDeleteActivity.this)
                                        .deleteIpMsg(ids, deleteLocked, false);
                        mSelectedIpMessageIds.clear();
                    }
                    if (deleteSmsUri != null) {
                        mDeleteRunningCount++;
                        mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN, null,
                            deleteSmsUri, FOR_MULTIDELETE, argsSms);
                    }
                    if (deleteMmsUri != null) {
                        mDeleteRunningCount++;
                        mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN, null,
                            deleteMmsUri, FOR_MULTIDELETE, argsMms);
                    }

                    if (deleteSmsUri == null && deleteMmsUri == null) {
                        mBackgroundQueryHandler.dismissProgressDialog();
                    }
                }
            }).start();
        }
    }

    private void updateSendFailedNotification() {
        final long threadId = mConversation.getThreadId();
        if (threadId <= 0) {
            return;
        }

        // updateSendFailedNotificationForThread makes a database call, so do the work off
        // of the ui thread.
        new Thread(new Runnable() {
            public void run() {
                MessagingNotification.updateSendFailedNotificationForThread(
                    MultiDeleteActivity.this, threadId);
            }
        }, "updateSendFailedNotification").start();
    }

    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String type;
            switch (msg.what) {
            case MessageListItem.ITEM_CLICK:
                // add for multi-delete
                mMsgListAdapter.changeSelectedState(msg.arg1);
                if (msg.arg2 == MESSAGE_STATUS_IMPORTANT) {
                    if (mMsgListAdapter.getItemList().get((long) msg.arg1)) {
                        mImportantCount++;
                    } else {
                        mImportantCount--;
                    }
                }

                /// M: add for ipmessage
                MessageItem msgItem = (MessageItem)msg.obj;
                if (msgItem.mIpMessageId > 0) {
                    if (mMsgListAdapter.getItemList().get((long) msg.arg1)) {
                        mSelectedIpMessageIds.add((long)msg.arg1);
                    } else {
                        mSelectedIpMessageIds.remove((long)msg.arg1);
                    }
                }
                if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == msgItem.mMessageType) {
                    if (mMsgListAdapter.getItemList().get((long) msg.arg1)) {
                        mMmsNotificationCount++;
                    } else {
                        mMmsNotificationCount--;
                    }
                }
                if (msgItem.mIpMessage != null && msgItem.mIpMessage instanceof IpAttachMessage) {
                    IpAttachMessage ipAttachMessage = (IpAttachMessage)msgItem.mIpMessage;
                    if (ipAttachMessage.isInboxMsgDownloalable()) {
                        if (mMsgListAdapter.getItemList().get((long) msg.arg1)) {
                            mUnDownloadedIpMessageCount++;
                            mSelectedIpAttachMessageItem.add(msgItem);
                        } else {
                            mUnDownloadedIpMessageCount--;
                            mSelectedIpAttachMessageItem.remove(msgItem);
                        }
                    }
                }
                mIsSelectedAll = false;
                if (mMsgListAdapter.getSelectedNumber() > 0) {
                    if (mMsgListAdapter.getSelectedNumber() == mMsgListAdapter.getCount()) {
                        mIsSelectedAll = true;
                    }
                }
                updateSelectCount();
                if (mSelectMode != null) {
                    mSelectMode.invalidate();
                }
                return;
            case SHOW_DOWNLOAD_PROGRESS_BAR:
                if (mDownloadDialog == null) {
                    mDownloadDialog = new ProgressDialog(MultiDeleteActivity.this);
                    mDownloadDialog.setCancelable(false);
                    mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mDownloadDialog.setMessage(IpMessageUtils.getResourceManager(MultiDeleteActivity.this)
                        .getSingleString(IpMessageConsts.string.ipmsg_download_history_dlg));
                    // ignore the search key, when deleting we do not want the search bar come out.
                    mDownloadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            return (keyCode == KeyEvent.KEYCODE_SEARCH);
                        }
                    });
                    MmsLog.d(TAG, "show download dialog.");
                    mDownloadDialog.show();
                }
                /// M: begin download the undownloaded ipmessage
                mDownloadedIpMessageStepCounter = 0;
                mDownloadedIpMessageStepCounterSuccess = 0;
                mDownloadedIpMessageStepCounterFail = 0;
                for (MessageItem item : mSelectedIpAttachMessageItem) {
                    MmsLog.d(TAG, "check a item.");
                    if (item.mIpMessage != null && item.mIpMessage instanceof IpAttachMessage) {
                        IpAttachMessage ipAttachMessage = (IpAttachMessage)item.mIpMessage;
                        if (ipAttachMessage.isInboxMsgDownloalable() &&
                            !IpMessageUtils.getMessageManager(MultiDeleteActivity.this).isDownloading(item.mMsgId)) {
                            IpMessageUtils.getMessageManager(MultiDeleteActivity.this).downloadAttach(item.mMsgId);
                            MmsLog.d(TAG, "request download an ipattachmessage, id:" + item.mMsgId);
                        }
                    }
                    MmsLog.d(TAG, "check a item end.");
                }
                break;
            case HIDE_DOWNLOAD_PROGRESS_BAR:
                if (mDownloadDialog != null) {
                    mDownloadDialog.dismiss();
                    mDownloadDialog = null;
                }
                mMsgListAdapter.notifyDataSetChanged();
                /// M: begin forward the selected message or show all failed toast.
                if (mDownloadedIpMessageStepCounterFail == mUnDownloadedIpMessageCount &&
                    mDownloadedIpMessageStepCounterFail == getSelectedCount()) {
                    /// M: selected message is all ipmessage and all download fail.
                    Toast.makeText(MultiDeleteActivity.this,
                                    MultiDeleteActivity.this.getString(R.string.multi_forward_failed_all),
                                    Toast.LENGTH_SHORT).show();
                } else {
                    /// M: now begin to forward.
                    mUnDownloadedIpMessageCount = 0;
                    mSelectActionMode.onActionItemClicked(mTempActionMode, mTempActionModeMenu);
                    mTempActionMode = null;
                    mTempActionModeMenu = null;
                }
                break;
            case UPDATE_SELECTED_COUNT:
                updateSelectCount();
                break;
            /// M: fix bug ALPS00554810, When the cache add new item, notify the data has been changed .@{
            case MessageListAdapter.MSG_LIST_NEED_REFRASH:
                boolean isClearCache = msg.arg1 == MessageListAdapter.MESSAGE_LIST_REFRASH_WITH_CLEAR_CACHE;
                MmsLog.d(MessageListAdapter.CACHE_TAG, "mMessageListItemHandler#handleMessage(): " +
                            "run adapter notify in mMessageListItemHandler. isClearCache = " + isClearCache);
                mMsgListAdapter.setClearCacheFlag(isClearCache);
                mMsgListAdapter.notifyDataSetChanged();
                return;
            /// @}
            default:
                Log.w(TAG, "Unknown message: " + msg.what);
                return;
            }
        }
    };

    private final class BackgroundQueryHandler extends BaseProgressQueryHandler {
        private int mListCount = 0;
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case MESSAGE_LIST_QUERY_TOKEN:
                /// M: fix bug ALPS00450886. @{
                if (!mIsLockOrUnlockFinish) {
                    /// M: fix bug ALPS00540064, avoid CursorLeak @{
                    if (cursor != null) {
                        cursor.close();
                    }
                    /// @}
                    return;
                }
                /// @}
                if (cursor == null) {
                    MmsLog.w(TAG, "onQueryComplete, cursor is null.");
                    return;
                }
                // check consistency between the query result and
                // 'mConversation'
                long tid = (Long) cookie;

                if (tid != mConversation.getThreadId()) {
                    MmsLog.d(TAG, "onQueryComplete: msg history query result is for threadId "
                        + tid + ", but mConversation has threadId "
                        + mConversation.getThreadId() + " starting a new query");
                    startMsgListQuery();
                    /// M: fix bug ALPS00540064, avoid CursorLeak @{
                    if (cursor != null) {
                        cursor.close();
                    }
                    /// @}
                    return;
                }

                if (mMsgListAdapter.mIsDeleteMode) {
                    mMsgListAdapter.initListMap(cursor);
                    if (mListCount != cursor.getCount()) {
                        mListCount = cursor.getCount();
                        mMsgListView.setSelection(mListCount);
                        mMsgListView.smoothScrollToPosition(mListCount);
                    }
                }

                mMsgListAdapter.changeCursor(cursor);
                mConversation.blockMarkAsRead(false);
                return;
                
            default:
                break;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            /// M: fix bug ALPS00351620; for requery searchactivity.
            SearchActivity.setNeedRequery();
            Intent mIntent = new Intent();
            switch (token) {
            case ConversationList.DELETE_CONVERSATION_TOKEN:
                try {
                    /** M: MTK Encapsulation ITelephony */
                    // ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
                    //        .checkService("phone"));
                    EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
                    if (phone != null) {
                        if (phone.isTestIccCard()) {
                            MmsLog.d(TAG, "All messages has been deleted, send notification...");
                            EncapsulatedSmsManager.setSmsMemoryStatus(true);
                        }
                    } else {
                        MmsLog.d(TAG, "Telephony service is not available!");
                    }
                } catch (RemoteException ex) {
                    // This shouldn't happen in the normal case
                    MmsLog.e(TAG, "onDeleteComplete, RemoteException: " + ex.getMessage());
                } catch (NullPointerException ex) {
                    // This could happen before phone restarts due to crashing
                    MmsLog.e(TAG, "onDeleteComplete, NullPointerException: " + ex.getMessage());
                }
                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                    MultiDeleteActivity.this, MessagingNotification.THREAD_NONE, false);
                // Update the notification for failed messages since they
                // may be deleted.
                updateSendFailedNotification();
                MessagingNotification
                        .updateDownloadFailedNotification(MultiDeleteActivity.this);
                if (progress()) {
                    dismissProgressDialog();
                }
                mIntent.putExtra("delete_all", true);
                break;

            case DELETE_MESSAGE_TOKEN:
                if (mDeleteRunningCount > 1) {
                    mDeleteRunningCount--;
                    return;
                }
                MmsLog.d(TAG, "onDeleteComplete(): before update mConversation, ThreadId = "
                    + mConversation.getThreadId());
                mConversation = Conversation.upDateThread(MultiDeleteActivity.this,
                    mConversation.getThreadId(), false);
                mThreadCountManager.isFull(mThreadId, MultiDeleteActivity.this,
                    ThreadCountManager.OP_FLAG_DECREASE);
                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                    MultiDeleteActivity.this, MessagingNotification.THREAD_NONE, false);
                // Update the notification for failed messages since they
                // may be deleted.
                updateSendFailedNotification();
                MessagingNotification
                        .updateDownloadFailedNotification(MultiDeleteActivity.this);
                MmsLog.d(TAG, "onDeleteComplete(): MessageCount = "
                    + mConversation.getMessageCount() + ", ThreadId = "
                    + mConversation.getThreadId());
                if (progress()) {
                    dismissProgressDialog();
                }
                mIntent.putExtra("delete_all", false);
                mDeleteRunningCount = 0;
                break;

            default:
                break;
            }
            setResult(RESULT_OK, mIntent);
            finish();
        }
    }

    private final MessageListAdapter.OnDataSetChangedListener mDataSetChangedListener
            = new MessageListAdapter.OnDataSetChangedListener() {
        public void onDataSetChanged(MessageListAdapter adapter) {
            mPossiblePendingNotification = true;
        }

        public void onContentChanged(MessageListAdapter adapter) {
            MmsLog.d(TAG, "MessageListAdapter.OnDataSetChangedListener.onContentChanged");
            startMsgListQuery();
            mIsSelectedAll = false;
        }
    };

    private void setSelectAll() {
        if (!mIsSelectedAll) {
            mIsSelectedAll = true;
            markCheckedState(mIsSelectedAll);
            if (mSelectMode != null) {
                mSelectMode.invalidate();
            }
        }
    }

    private void setDeselectAll() {
        if (mMsgListAdapter.getSelectedNumber() > 0) {
            mIsSelectedAll = false;
            markCheckedState(mIsSelectedAll);
            if (mSelectMode != null) {
                mSelectMode.invalidate();
            }
        }
    }

    private void updateSelectCount() {
        int selectNum = getSelectedCount();
        mChatSelect.setText(getResources().getQuantityString(
            R.plurals.message_view_selected_message_count, selectNum, selectNum));
//        mActionBarText.setText(getResources().getQuantityString(
//            R.plurals.message_view_selected_message_count, selectNum, selectNum));
    }

    private int mImportantCount = 0;
    public static final int MESSAGE_STATUS_IMPORTANT = 1;
    public static final int MESSAGE_STATUS_NOT_IMPORTANT = 0;
    private class SelectActionMode implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
            ViewGroup v = (ViewGroup) LayoutInflater.from(MultiDeleteActivity.this).inflate(
                    R.layout.chat_select_action_bar, null);
            mode.setCustomView(v);
            mChatSelect = ((Button) v.findViewById(R.id.bt_chat_select));
            mChatSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(MultiDeleteActivity.this, v);
                    popup.getMenuInflater().inflate(R.menu.select_menu, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();
                            if (id == R.id.menu_select_all) {
                                setSelectAll();
                            } else if (id == R.id.menu_select_cancel) {
                                setDeselectAll();
                            } else {
                                return true;
                            }
                            return false;
                        }
                    });

                    Menu popupMenu = popup.getMenu();
                    MenuItem selectAllItem = popupMenu.findItem(R.id.menu_select_all);
                    MenuItem unSelectAllItem = popupMenu.findItem(R.id.menu_select_cancel);
                    if (mMsgListAdapter != null) {
                        Cursor cursor = mMsgListAdapter.getCursor();
                        if (cursor != null) {
                            if (mMsgListAdapter.getSelectedNumber() >= cursor.getCount()) {
                                if (selectAllItem != null) {
                                    selectAllItem.setVisible(false);
                                }
                                if (unSelectAllItem != null) {
                                    unSelectAllItem.setVisible(true);
                                }
                            } else {
                                if (selectAllItem != null) {
                                    selectAllItem.setVisible(true);
                                }
                                if (unSelectAllItem != null) {
                                    unSelectAllItem.setVisible(false);
                                }
                            }
                        } else {
                            if (selectAllItem != null) {
                                selectAllItem.setVisible(true);
                            }
                            if (unSelectAllItem != null) {
                                unSelectAllItem.setVisible(false);
                            }
                        }
                    } else if (selectAllItem != null) {
                        selectAllItem.setVisible(true);
                        if (unSelectAllItem != null) {
                            unSelectAllItem.setVisible(false);
                        }
                    }

                    popup.show();
                }
            });
            updateSelectCount();
            getMenuInflater().inflate(R.menu.compose_multi_select_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuItem deleteItem = menu.findItem(R.id.delete);
            MenuItem importantItem = menu.findItem(R.id.important);
            MenuItem forwardItem = menu.findItem(R.id.forward);
            MenuItem removeItem = menu.findItem(R.id.remove_important);
            if (MmsConfig.getIpMessagServiceId(MultiDeleteActivity.this) > IpMessageServiceId.NO_SERVICE) {
                if (importantItem != null) {
                    importantItem.setTitle(R.string.ipmsg_mark_as_important);
                }
                if (removeItem != null) {
                    removeItem.setTitle(R.string.ipmsg_remove_from_important);
                }
            } else {
                if (importantItem != null) {
                    importantItem.setTitle(R.string.menu_lock);
                }
                if (removeItem != null) {
                    removeItem.setTitle(R.string.menu_unlock);
                }
            }
            MmsLog.d(IPMSG_TAG, "onPrepareActionMode(): mImportantCount = " + mImportantCount);
            /// M: make disable if no item selected.
            int selectNum = getSelectedCount();
            if (mImportantCount > 0) {
                menu.setGroupVisible(R.id.remove_important_group, true);
            } else {
                menu.setGroupVisible(R.id.remove_important_group, false);
            }
            if (mImportantCount > 0 && mImportantCount == selectNum) {
                menu.setGroupVisible(R.id.important_group, false);
            } else {
                menu.setGroupVisible(R.id.important_group, true);
            }

            if (selectNum > 0) {
                deleteItem.setEnabled(true);
                importantItem.setEnabled(true);
                forwardItem.setEnabled(true);
                removeItem.setEnabled(true);
            } else {
                deleteItem.setEnabled(false);
                importantItem.setEnabled(false);
                forwardItem.setEnabled(false);
                removeItem.setEnabled(false);
            }
//            if (!MessageUtils.isActivateSimCard(this)) {
//                menu.setGroupVisible(R.id.importantP, false);
//                menu.setGroupVisible(R.id.remove_importantP, false);
//            }
            return true;
        }

        private void showMmsTipsDialog(final ActionMode mode, final MenuItem item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MultiDeleteActivity.this);
            builder.setTitle(R.string.forward_tips_title)
                   .setIconAttribute(android.R.attr.alertDialogIcon)
                   .setCancelable(true)
                   .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                       public final void onClick(DialogInterface dialog, int which) {
                           dialog.dismiss();
                           mMmsNotificationHasRun = true;
                           onActionItemClicked(mode, item);
                       }
                   })
                   .setNegativeButton(R.string.Cancel, null)
                   .setMessage(R.string.forward_tips_body)
                   .show();
        }

        private void showIpMessageDownloadDialog(final ActionMode mode, final MenuItem item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MultiDeleteActivity.this);
            builder.setTitle(R.string.download)
                   .setIconAttribute(android.R.attr.alertDialogIcon)
                   .setCancelable(true)
                   .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                       public final void onClick(DialogInterface dialog, int which) {
                           dialog.dismiss();
                           //mUnDownloadedIpMessageCount = 0;
                           //onActionItemClicked(mode, item);
                           /// M: download the undownloaded ipmessage
                           mTempActionMode = mode;
                           mTempActionModeMenu = item;
                           Message msg = mMessageListItemHandler.obtainMessage(SHOW_DOWNLOAD_PROGRESS_BAR);
                           mMessageListItemHandler.sendMessage(msg);
                       }
                   })
                   .setNegativeButton(R.string.Cancel, null)
                   .setMessage(IpMessageUtils.getResourceManager(MultiDeleteActivity.this)
                            .getSingleString(IpMessageConsts.string.ipmsg_multi_forward_tips_content))
                   .show();
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()) {
            case R.id.delete:
                int mSelectedNumber = mMsgListAdapter.getSelectedNumber();
                /// M: fix bug ALPS00456634, delete msgs when the conversation has draft
                if (mSelectedNumber >= mMsgListAdapter.getCount() && !mConversation.hasDraft()) {
                    Long threadId = mConversation.getThreadId();
                    MultiDeleteMsgListener mMultiDeleteMsgListener = new MultiDeleteMsgListener();
                    confirmMultiDeleteMsgDialog(mMultiDeleteMsgListener, selectedMsgHasLocked(), true, threadId,
                        MultiDeleteActivity.this);
                } else if (mMsgListAdapter.getSelectedNumber() > 0) {
                    MultiDeleteMsgListener mMultiDeleteMsgListener = new MultiDeleteMsgListener();
                    confirmMultiDeleteMsgDialog(mMultiDeleteMsgListener, selectedMsgHasLocked(), false, null,
                        MultiDeleteActivity.this);
                }
                break;
            case R.id.forward:
                /// M: if forward has mms notification, we need show a tips dialog, this type can not forward.
                if (mMmsNotificationCount > 0 && !mMmsNotificationHasRun) {
                    showMmsTipsDialog(mode, item);
                    return true;
                }
                /// M: ipmessage multi-forward
                if (IpMessageUtils.getIpMessagePlugin(MultiDeleteActivity.this).isActualPlugin()) {
                    if (mUnDownloadedIpMessageCount > 0) {
                        showIpMessageDownloadDialog(mode, item);
                        return true;
                    }
                    mForwardMsgIds = getForwardMsgIds();
                    if (mForwardMsgIds == null || mForwardMsgIds.equals("")) {
                        return true;
                    }
                    if (IpMessageUtils.getServiceManager(MultiDeleteActivity.this)
                                    .isFeatureSupported(FeatureId.CONTACT_SELECTION)) {
                        try {
                            Intent intent = new Intent(RemoteActivities.CONTACT);
                            intent.putExtra(RemoteActivities.KEY_REQUEST_CODE,
                                ComposeMessageActivity.REQUEST_CODE_IPMSG_PICK_CONTACT);
                            intent.putExtra(RemoteActivities.KEY_TYPE, SelectContactType.ALL);
                            IpMessageUtils.startRemoteActivityForResult(MultiDeleteActivity.this,
                                intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(MultiDeleteActivity.this,
                                    MultiDeleteActivity.this.getString(R.string.no_application_response),
                                    Toast.LENGTH_SHORT).show();
                            MmsLog.e(TAG, e.getMessage(), e);
                        }
                    } else {
                        try {
                            Intent intent = new Intent(MessageUtils.ACTION_CONTACT_SELECTION);
                            intent.setType(Phone.CONTENT_TYPE);
                            startActivityForResult(intent,
                                ComposeMessageActivity.REQUEST_CODE_IPMSG_PICK_CONTACT);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(MultiDeleteActivity.this,
                                    MultiDeleteActivity.this.getString(R.string.no_application_response),
                                    Toast.LENGTH_SHORT).show();
                            MmsLog.e(TAG, e.getMessage(), e);
                        }
                    }
                    return true;
                }
                if (mMsgListAdapter.getSelectedNumber() == 1) {
                    if (WorkingMessage.sCreationMode == 0 || !MessageUtils.isRestrictedType(MultiDeleteActivity.this, getForwardMsgId())) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mMmsDeleteAndForwardPlugin.onMultiforwardItemSelected();;
                            }
                        });
                    } else if (WorkingMessage.sCreationMode == WorkingMessage.WARNING_TYPE) {
                        new AlertDialog.Builder(MultiDeleteActivity.this)
                        .setTitle(R.string.restricted_forward_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.restricted_forward_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            mMmsDeleteAndForwardPlugin.onMultiforwardItemSelected();
                                        }
                                    });
                    }
                            })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                }
                } else {
                    mMmsDeleteAndForwardPlugin.onMultiforwardItemSelected();
                }
                break;
            case R.id.important:
                long[][] ids = getSelectedMsgIds();
                if (ids != null) {
                    markAsImportant(ids[0], ids[1], true);
                }
                break;
            case R.id.remove_important:
                long[][] msgIds = getSelectedMsgIds();
                if (msgIds != null) {
                    markAsImportant(msgIds[0], msgIds[1], false);
                }
                break;
            default:
                break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            MultiDeleteActivity.this.finish();
//            setMarkState(false);
        }

        private long[][] getSelectedMsgIds() {
            Iterator importantIter = mMsgListAdapter.getItemList().entrySet().iterator();
            long[][] selectMessageIds = new long[2][mMsgListAdapter.getSelectedNumber()];

            int i = 0;
            int mmsIndex = 0;
            while (importantIter.hasNext()) {
                Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) importantIter.next();
                if (entry.getValue() && entry.getKey() > 0) {
                    selectMessageIds[0][i] = entry.getKey();
                    i++;
                } else if (entry.getValue() && entry.getKey() < 0) {
                    selectMessageIds[1][mmsIndex] = -entry.getKey();
                    mmsIndex++;
                }
            }
            return selectMessageIds;
        }

        private long getForwardMsgId() {
            Iterator importantIter = mMsgListAdapter.getItemList().entrySet().iterator();
            long msgId = 0;
            while (importantIter.hasNext()) {
                Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) importantIter.next();
                if (entry.getValue()) {
                    msgId = entry.getKey();
                }
            }
            if (msgId < 0) {
                msgId = -msgId;
            }
            return msgId;
        }

        /**
         * M:
         * @return
         */
        private String getForwardMsgIds() {
            Iterator importantIter = mMsgListAdapter.getItemList().entrySet().iterator();
            StringBuffer forwardMsgIds = new StringBuffer();
            while (importantIter.hasNext()) {
                Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) importantIter.next();
                if (entry.getValue()) {
                    forwardMsgIds.append(entry.getKey() + ",");
                }
            }
            String ids = forwardMsgIds.toString();
            if (ids.length() > 1) {
                ids = ids.substring(0, ids.length() - 1);
            }
            return ids;
        }

        private void forwardMsg() {
            long[][] ids = getSelectedMsgIds();
            if (ids == null || ids.length < 1) {
                return;
            }
            Intent intent = new Intent(MultiDeleteActivity.this,ComposeMessageActivity.class);
            intent.putExtra(ComposeMessageActivity.FORWARD_MESSAGE, true);
            intent.putExtra(IPMSG_IDS, ids);
        }
    }

    /**
     * M:
     * @param smsIds
     * @param mmsIds
     * @param important
     */
    private void markAsImportant(final long smsIds[], final long mmsIds[], final boolean important) {
        showProgressIndication();
        mIsLockOrUnlockFinish = false;
        final ContentValues values = new ContentValues(1);
        values.put("locked", important ? 1 : 0);
        new Thread(new Runnable() {
            public void run() {
                if (smsIds != null && smsIds.length > 0) {
                    Uri uri = Sms.CONTENT_URI;
                    StringBuffer strBuf = new StringBuffer();
                    for (long id : smsIds) {
                        strBuf.append(id + ",");
                    }
                    String str = strBuf.toString();
                    String idSelect = str.substring(0, str.length() - 1);
                    if (important) {
                        IpMessageUtils.getMessageManager(MultiDeleteActivity.this)
                                .addMessageToImportantList(smsIds);
                    } else {
                        IpMessageUtils.getMessageManager(MultiDeleteActivity.this)
                                .deleteMessageFromImportantList(smsIds);
                    }
                    getContentResolver().update(uri, values, "_id in (" + idSelect + ")", null);
                }
                if (mmsIds != null && mmsIds.length > 0) {
                    Uri uri = Mms.CONTENT_URI;
                    StringBuffer strBuf = new StringBuffer();
                    for (long id : mmsIds) {
                        strBuf.append(id + ",");
                    }
                    String str = strBuf.toString();
                    String idSelect = str.substring(0, str.length() - 1);
                    getContentResolver().update(uri, values, "_id in (" + idSelect + ")", null);
                }
                if (mMessageListItemHandler != null) {
                    mMessageListItemHandler.post(new Runnable() {
                        public void run() {
                            dismissProgressIndication();
                            MultiDeleteActivity.this.finish();
                        }
                    });
                }
            }
        }).start();
        int smsIdsLen = smsIds != null ? smsIds.length : 0;
        int mmsIdsLen = mmsIds != null ? mmsIds.length : 0;
        if (important) {
            mImportantCount = (smsIdsLen + mmsIdsLen);
        } else {
            mImportantCount = 0;
        }
    }

    /**
     * M:
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case ComposeMessageActivity.REQUEST_CODE_IPMSG_PICK_CONTACT:
                String selectedContactsNums = data
                        .getStringExtra(IpMessageUtils.SELECTION_CONTACT_RESULT);
                long[] contactsId = data
                        .getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
                if (TextUtils.isEmpty(selectedContactsNums)
                    && (contactsId == null || contactsId.length < 1)) {
                    return;
                }
                String numbers = "";
                if (TextUtils.isEmpty(selectedContactsNums)) {
                    ContactList selected = ContactList.blockingGetByIds(contactsId);
                    numbers = selected.serialize();
                }
                final String mSelectContactsNumbers = TextUtils.isEmpty(selectedContactsNums) ? numbers
                    : selectedContactsNums;
                Intent testIntent = new Intent();
                testIntent.setAction(MultiForwardMessageActivity.MULTI_FORWARD_ACTION);
                testIntent.putExtra(MultiForwardMessageActivity.MULTI_FORWARD_PARAM_MESSAGEIDS,
                    mForwardMsgIds);
                testIntent.putExtra(MultiForwardMessageActivity.MULTI_FORWARD_PARAM_THREADID,
                    this.mThreadId);
                testIntent.putExtra(MultiForwardMessageActivity.MULTI_FORWARD_PARAM_NUMBERS,
                    mSelectContactsNumbers);
                startActivityForResult(testIntent, REQUEST_CODE_FORWARD);
                break;
            case REQUEST_CODE_FORWARD:
                finish();
                break;
        }
    }

    public void prepareToForwardMessage() {
        Boolean mHasMms = false;
        Iterator iter = mMsgListAdapter.getItemList().entrySet().iterator();
        ArrayList<Long> selectSms = new ArrayList<Long>();
        ArrayList<Long> selectMms = new ArrayList<Long>();
        while (iter.hasNext()) {
            Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
            if (entry.getValue()) {
                if (entry.getKey() > 0) {
                    MmsLog.i(TAG, "sms");
                    selectSms.add(entry.getKey());
                } else {
                    MmsLog.i(TAG, "have  mms");
                    selectMms.add(entry.getKey());
                    mHasMms = true;
                }
            }
        }
        final ArrayList<Long> finalSelectSms = selectSms;
        if (mHasMms && !mMmsNotificationHasRun) {
            if (getSelectedCount() == 1) {
                /// M :add for one mms forward. @{
                long mMmsId = selectMms.get(0);
                MmsLog.i(TAG, "enter forward one mms and mMmsId is " + mMmsId);
                MessageItem item = mMsgListAdapter.getCachedMessageItem("mms", -mMmsId, null);
                forwardOneMms(item);
                /// @}
            } else if (getSelectedCount() > 1) {
                MmsLog.i(TAG, "enter have  mms");
                new AlertDialog.Builder(MultiDeleteActivity.this)
                .setTitle(R.string.discard_mms_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.discard_mms_content)
                .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                    public final void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            public void run() {
                                forwardMessage(finalSelectSms);
                            }
                        }, "ForwardMessage").start();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            }
        } else {
            MmsLog.i(TAG, "enter have  sms");
            if (mMmsNotificationHasRun) {
                mMmsNotificationHasRun = false;
            }
            new Thread(new Runnable() {
                public void run() {
                    forwardMessage(finalSelectSms);
                }
            }, "ForwardMessage").start();
        }
    }

    /// M: add for one mms forward. @{
    private ForwardMmsAsyncDialog mAsyncDialog;   // Used for background tasks.
    private class ForwardMmsAsyncDialog extends AsyncDialog {
        private Uri mTempMmsUri;            // Only used as a temporary to hold a slideshow uri
        private long mTempThreadId;         // Only used as a temporary to hold a threadId

        public ForwardMmsAsyncDialog(Activity activity) {
            super(activity);
        }

    }

    AsyncDialog getAsyncDialog() {
        if (mAsyncDialog == null) {
            mAsyncDialog = new ForwardMmsAsyncDialog(this);
        }
        return mAsyncDialog;
    }

    private void forwardOneMms(final MessageItem msgItem) {
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                if (msgItem.mType.equals("mms")) {
                    SendReq sendReq = new SendReq();
                    String subject = getString(R.string.forward_prefix);
                    if (msgItem.mSubject != null) {
                        subject += msgItem.mSubject;
                    }
                    sendReq.setBody(msgItem.mSlideshow.makeCopy());
                    mAsyncDialog.mTempMmsUri = null;
                    try {
                        PduPersister persister =
                                PduPersister.getPduPersister(MultiDeleteActivity.this);
                        mAsyncDialog.mTempMmsUri = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true,
                                MmsPreferenceActivity
                                    .getIsGroupMmsEnabled(MultiDeleteActivity.this));
                        mAsyncDialog.mTempThreadId = MessagingNotification.getThreadId(
                                MultiDeleteActivity.this, mAsyncDialog.mTempMmsUri);
                    } catch (MmsException e) {
                        Log.e(TAG, "Failed to copy message: " + msgItem.mMessageUri);
                        Toast.makeText(MultiDeleteActivity.this,
                                R.string.cannot_save_message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable is run
                // on the UI thread.
                Intent intent = createIntent(MultiDeleteActivity.this);
                if (MmsConfig.isNeedExitComposerAfterForward()) {
                    intent.putExtra("exit_on_sent", true);
                }
                intent.putExtra(FORWARD_MESSAGE, true);
                if (mAsyncDialog.mTempThreadId > 0) {
                    intent.putExtra("thread_id", mAsyncDialog.mTempThreadId);
                }
                intent.putExtra("msg_uri", mAsyncDialog.mTempMmsUri);
                String subject = getString(R.string.forward_prefix);
                if (msgItem.mSubject != null) {
                    subject += msgItem.mSubject;
                }
                intent.putExtra("subject", subject);
                intent.setClassName(MultiDeleteActivity.this,
                        "com.android.mms.ui.ForwardMessageActivity");
                startActivity(intent);
            }
        }, R.string.sync_mms_to_db);
    }
    /// @}

    private void forwardMessage(ArrayList<Long> smsList) {
        if (smsList.size() <= 0) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MultiDeleteActivity.this, R.string.toast_sms_forward, Toast.LENGTH_SHORT).show();
                    if (mMsgListAdapter.getSelectedNumber() > 0) {
                        mIsSelectedAll = false;
                        markCheckedState(mIsSelectedAll);
                    }
                }
            });
            return;
        }
        Collections.sort(smsList);

        int maxLength = MmsConfig.getMaxTextLimit();
        StringBuffer strbuf = new StringBuffer();
        String tempbuf = null;
        String smsBody = null;
        boolean reachLimitFlag = false;

        /// M: For multi-forward issue fix ALPS00407998 @{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MultiDeleteActivity.this);
        boolean smsForwardWithSender = prefs.getBoolean(SmsPreferenceActivity.SMS_FORWARD_WITH_SENDER, true);
        MmsLog.d(TAG, "forwardMessage(): SMS Forward With Sender ?= " + smsForwardWithSender);
        /// @}

        for (int i = 0; i < smsList.size(); i++) {
            long mMmsId = smsList.get(i);

            if (mMmsId > 0) {
                strbuf.append(mMmsDeleteAndForwardPlugin.getBody(mMmsId));
                /// M: For multi-forward issue fix ALPS00407998 @{
                if (mOP01Plugin && smsForwardWithSender) {
                /// @}
                    strbuf.append("\n");
                    strbuf.append(getString(R.string.forward_from));
                    Contact contact = Contact.get(mMmsDeleteAndForwardPlugin.getAddress(mMmsId), false);
                    String number = Contact.formatNameAndNumber(contact.getName(), contact.getNumber(),"");
                    MmsLog.d(TAG, "forwardMessage(): Contact's name and number=" + number);
                    strbuf.append(number);
                }
                if (i < smsList.size() - 1) {
                    strbuf.append("\n");
                }
            }

            if (strbuf.length() > maxLength) {
                reachLimitFlag = true;
                /// M: fix bug ALPS00444391, remove the last "\n" when > maxLength @{
                if (tempbuf != null && tempbuf.endsWith("\n")) {
                    tempbuf = tempbuf.substring(0, tempbuf.length() - 1);
                }
                /// @}
                break;
            } else {
                tempbuf = strbuf.toString();
            }
            MmsLog.d(TAG, "forwardMessage  strbuf.length()=" + strbuf.length() +
                            "  tempbuf.length() = " + tempbuf.length());
        }
        if (reachLimitFlag) {
            final String contentbuf = tempbuf;
            runOnUiThread(new Runnable() {
                public void run() {
                    showReachLimitDialog(contentbuf);
                }
            });
            return;
        }
        Intent intent = createIntent(this);
        if (MmsConfig.isNeedExitComposerAfterForward()) {
            intent.putExtra("exit_on_sent", true);
        }
        intent.putExtra(FORWARD_MESSAGE, true);
        intent.putExtra(ComposeMessageActivity.SMS_BODY, tempbuf);

        // ForwardMessageActivity is simply an alias in the manifest for ComposeMessageActivity.
        // We have to make an alias because ComposeMessageActivity launch flags specify
        // singleTop. When we forward a message, we want to start a separate ComposeMessageActivity.
        // The only way to do that is to override the singleTop flag, which is impossible to do
        // in code. By creating an alias to the activity, without the singleTop flag, we can
        // launch a separate ComposeMessageActivity to edit the forward message.
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        startActivity(intent);
    }

    private Intent createIntent(Context context) {
        Intent intent = new Intent(context, MultiDeleteActivity.class);
        return intent;
    }

    private void showReachLimitDialog(final String mcontent) {
        new AlertDialog.Builder(MultiDeleteActivity.this)
        .setTitle(R.string.sms_size_limit)
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .setMessage(R.string.dialog_sms_limit)
        .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = createIntent(MultiDeleteActivity.this);
                if (MmsConfig.isNeedExitComposerAfterForward()) {
                    intent.putExtra("exit_on_sent", true);
                }
                intent.putExtra(FORWARD_MESSAGE, true);
                intent.putExtra(ComposeMessageActivity.SMS_BODY, mcontent);
                intent.setClassName(MultiDeleteActivity.this, "com.android.mms.ui.ForwardMessageActivity");
                startActivity(intent);
            }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
    }

    @Override
    protected void onDestroy() {
        if (MmsConfig.getIpMessagServiceId(this) > IpMessageServiceId.NO_SERVICE) {
            IpMessageUtils.removeIpMsgNotificationListeners(this, this);
        }
        /// M: add for alps00613259 @{
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setOnDataSetChangedListener(null);
        }
        /// @}
        super.onDestroy();
    }

    @Override
    public void notificationsReceived(Intent intent) {
        MmsLog.d(TAG, "notificationsReceived(): intent = " + intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (IpMessageUtils.getActionTypeByAction(action)) {
        case IpMessageUtils.IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION:
            MmsLog.d(TAG, "notificationsReceived():" + " download status notification.");
            long msgId = intent.getLongExtra(DownloadAttachStatus.DOWNLOAD_MSG_ID, 0);
            int downloadStatus = intent.getIntExtra(DownloadAttachStatus.DOWNLOAD_MSG_STATUS, DownloadAttachStatus.STARTING);
            MmsLog.d(TAG, "notificationsReceived(): downloadStatus = " + downloadStatus + ", id:" + msgId);
            if (mDownloadedIpMessageStepCounter >= mUnDownloadedIpMessageCount) {
                MmsLog.d(TAG, "get more download status.");
                return;
            }
            if (downloadStatus == DownloadAttachStatus.DONE) {
                mDownloadedIpMessageStepCounter++;
                mDownloadedIpMessageStepCounterSuccess++;
            } else if (downloadStatus == DownloadAttachStatus.FAILED) {
                mDownloadedIpMessageStepCounter++;
                mDownloadedIpMessageStepCounterFail++;
            }
            if (mDownloadedIpMessageStepCounter == mUnDownloadedIpMessageCount) {
                Message msg = mMessageListItemHandler.obtainMessage(HIDE_DOWNLOAD_PROGRESS_BAR);
                mMessageListItemHandler.sendMessage(msg);
            }
            break;
        default:
            MmsLog.d(TAG, "ignore a event.");
            break;
        }
    }

    /// M: fix bug ALPS00450886. @{
    private void showProgressIndication() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(MultiDeleteActivity.this);
            mProgressDialog.setMessage(getString(R.string.please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.show();
    }

    private void dismissProgressIndication() {
        if (mProgressDialog != null && !isFinishing() && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
    ///@}
}
