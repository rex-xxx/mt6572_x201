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


import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.data.FolderView;
import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;
import com.android.mms.transaction.CBMessagingNotification;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.transaction.WapPushMessagingNotification;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.DraftCache;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.pdu.PduHeaders;

import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.mms.ext.IAppGuideExt;
import com.mediatek.mms.ext.IMmsDialogNotify;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/** M:
 * This activity provides a list view of existing conversations.
 */
public class FolderViewList extends ListActivity implements DraftCache.OnDraftChangedListener {
    private static final String TAG = "FolderViewList";
    private static final String CONV_TAG = "Mms/FolderViewList";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG;

    public static final int OPTION_INBOX    = 0;
    public static final int OPTION_OUTBOX   = 1;
    public static final int OPTION_DRAFTBOX = 2;
    public static final int OPTION_SENTBOX  = 3;

    public static final int DRAFTFOLDER_LIST_QUERY_TOKEN      = 1009;
    public static final int INBOXFOLDER_LIST_QUERY_TOKEN      = 1111;
    public static final int OUTBOXFOLDER_LIST_QUERY_TOKEN     = 1121;
    public static final int SENTFOLDER_LIST_QUERY_TOKEN       = 1131;
    public static final int FOLDERVIEW_DELETE_TOKEN           = 1001;
    public static final int FOLDERVIEW_HAVE_LOCKED_MESSAGES_TOKEN     = 1002;
    private static final int FOLDERVIEW_DELETE_OBSOLETE_THREADS_TOKEN = 1003;
    
    
    private static final Uri SMS_URI = Uri.parse("content://sms/");
    private static final Uri MMS_URI = Uri.parse("content://mms/");
    private static final Uri WAPPUSH_URI = Uri.parse("content://wappush/");
    private static final Uri CB_URI = Uri.parse("content://cb/messages/");
    private static final Uri SIM_SMS_URI = Uri.parse("content://mms-sms/sim_sms/");
    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;
    public static final int MENU_VIEW_CONTACT         = 2;
    public static final int MENU_ADD_TO_CONTACTS      = 3;
    public static final int MENU_SIM_SMS              = 4;
    public static final int MENU_FORWORD              = 5;
    public static final int MENU_REPLY                = 6;
    
    // IDs of the option menu items for the list of conversations.
    public static final int MENU_MULTIDELETE          = 0;
    public static final int MENU_CHANGEVIEW           = 1;
    
    public static final String FOLDERVIEW_KEY         = "floderview_key";    
    private View mFolderSpinner;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private ThreadListQueryHandler mQueryHandler;
/// M: @{
    private FolderViewListAdapter mListAdapter = null;
/// @}
    private Handler mHandler;
    private boolean mNeedToMarkAsSeen;
    private Contact mContact = null;
    //private SearchView mSearchView;
   // private StatusBarManager mStatusBarManager;
    //wappush: indicates the type of thread, this exits already, but has not been used before
    private int mType;
    public static int mgViewID;
    private Context context = null;
    private AccountDropdownPopup mAccountDropdown;
    private TextView mSpinnerTextView;
    private TextView mCountTextView;
    public static final int REQUEST_CODE_SELECT_SIMINFO = 180;
    private Uri mSIM_SMS_URI_NEW = Uri.parse("content://mms-sms/sim_sms/#");
    
    private SimpleAdapter mAdapter;
    private static final String VIEW_ITEM_KEY_BOXNAME   = "spinner_line_2";
    private String where = null;
    

    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    private static final String FOR_FOLDERMODE_MULTIDELETE = "ForFolderMultiDelete";
    
    private boolean mDisableSearchFlag = false;
    
    private boolean mNeedUpdateListView = false;
/// M: @{
    private boolean mIsQuerying = false;//is in querying
    private boolean mNeedQuery = false;//whether receive oncontentchanged info
    private boolean mIsInActivity = false;//whether in activity
/// @}
    
    public static final int REQUEST_CODE_DELETE_RESULT = 180;
    
    private static int mDeleteCounter = 0;


    public ModeCallback mModeCallBack = new ModeCallback();
    public ActionMode mActionMode = null;
    private static String ACTIONMODE = "actionMode";
    private static String BOXTYPE = "boxType";
    private static String NEED_RESTORE_ADAPTER_STATE = "needRestore";
    private static String mSELECT_ITEM_IDS = "selectItemIds";
    private boolean mIsNeedRestoreAdapterState = false;
    private long[] mListSelectedItem;
    
    private boolean mHasLockedMsg = false;
    private Map<Long, Boolean> mListItemLockInfo;
    
    /** M: this is used to record the fontScale, if it is > 1.1[1.1 is big style]
     *  we need make the content view of FolderViewListItem to be one line
     *  or it will overlapping with the above from view.
     */ 
    private float mFontScale;
    public static final float MAX_FONT_SCALE = 1.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.folderview_list_screen);
        mQueryHandler = new ThreadListQueryHandler(getContentResolver());
        
        ListView listView = getListView();
        //listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        listView.setOnKeyListener(mThreadListKeyListener);
        
//        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
//        listView.setMultiChoiceModeListener(new ModeCallback());

        listView.setOnItemLongClickListener(new MultiSelectOnLongClickListener());
        if (savedInstanceState != null) {
            mgViewID = savedInstanceState.getInt(BOXTYPE, OPTION_INBOX);
            mIsNeedRestoreAdapterState = savedInstanceState.getBoolean(NEED_RESTORE_ADAPTER_STATE, false);
        } else {
            mgViewID = getIntent().getIntExtra(FOLDERVIEW_KEY, OPTION_INBOX);
            mIsNeedRestoreAdapterState = false;
        }

        View emptyView = findViewById(R.id.empty);
        listView.setEmptyView(emptyView);
        
        context = FolderViewList.this;
        initListAdapter();
        mHandler = new Handler();

        initSpinnerListAdapter();
        setTitle("");
//        mgViewID = getIntent().getIntExtra(FOLDERVIEW_KEY, 0);
        Log.d(TAG, "onCreate, mgViewID:" + mgViewID);
        setBoxTitle(mgViewID);
        mListItemLockInfo = new HashMap<Long, Boolean>();
        
        /** M: get fontscale
         *  we only need to set it to true if needed
         *  font scale change will make this activity create again
         */
        mFontScale = getResources().getConfiguration().fontScale;
        MmsLog.d(TAG, "system fontscale is:" + mFontScale);
        if (mFontScale >= MAX_FONT_SCALE) {
            mListAdapter.setSubjectSingleLineMode(true);
        }
    }
        
    private void initSpinnerListAdapter() {
        
        mAdapter = new SimpleAdapter(this, getData(),
              R.layout.folder_mode_item,
              new String[] {"spinner_line_2"},
              new int[] {R.id.spinner_line_2});     
        setupActionBar();
        
        mAccountDropdown = new AccountDropdownPopup(context);
        mAccountDropdown.setAdapter(mAdapter);
  
   }
    
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        ViewGroup v = (ViewGroup)LayoutInflater.from(this).inflate(R.layout.folder_mode_actionbar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.LEFT));
        mCountTextView = (TextView)v.findViewById(R.id.message_count);

        mFolderSpinner = (View)v.findViewById(R.id.account_spinner);
        mSpinnerTextView = (TextView)v.findViewById(R.id.boxname);
        mSpinnerTextView.setText(R.string.inbox);
        
        mFolderSpinner.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (mAdapter.getCount() > 0) {
                    mAccountDropdown.show();
                }
            }
        });
    }
    
    // Based on Spinner.DropdownPopup
    private class AccountDropdownPopup extends ListPopupWindow {
        public AccountDropdownPopup(Context mcontext) {
            super(mcontext);
            setAnchorView(mFolderSpinner);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);
            setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    onAccountSpinnerItemClicked(position);
                    dismiss();
                }
            });
        }

        @Override
        public void show() {
            WindowManager windowM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            Configuration config = context.getResources().getConfiguration();
            Display defDisplay = windowM.getDefaultDisplay();
            int w = 0;
            if ( config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                w = defDisplay.getWidth();
            } else {
                w = defDisplay.getHeight();
            }
            setWidth(w / 3);
            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            // List view is instantiated in super.show(), so we need to do this after...
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }
    
    private void onAccountSpinnerItemClicked(int position) {
        switch (position) {
            case OPTION_INBOX:
                mgViewID = OPTION_INBOX;
                mSpinnerTextView.setText(R.string.inbox);
                mNeedToMarkAsSeen = true;
                startAsyncQuery();
                break;
            case OPTION_OUTBOX:
                mgViewID = OPTION_OUTBOX;
                mSpinnerTextView.setText(R.string.outbox);
                startAsyncQuery();
                break;
            case OPTION_DRAFTBOX:
                mgViewID = OPTION_DRAFTBOX;
                mSpinnerTextView.setText(R.string.draftbox);
                startAsyncQuery();
                break;
            case OPTION_SENTBOX:
                mgViewID = OPTION_SENTBOX;
                mSpinnerTextView.setText(R.string.sentbox);
                startAsyncQuery();
                break;
            default:
                break;
        }
        MmsLog.d(TAG, "onAccountSpinnerItemClicked mgViewID = " + mgViewID);
//        invalidateOptionsMenu();
    }
    
    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        Resources res = getResources();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.inbox));
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.outbox));
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.draftbox));
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.sentbox));
        list.add(map);

//        map = new HashMap<String, Object>();
//        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.simbox));
//        list.add(map);
        
        return list;
    }
    

    private final FolderViewListAdapter.OnContentChangedListener mContentChangedListener =
        new FolderViewListAdapter.OnContentChangedListener() {
        public void onContentChanged(FolderViewListAdapter adapter) {
            Log.d(TAG, "onContentChanged : mIsInActivity =" + mIsInActivity + "mIsQuerying =" + mIsQuerying +
                "mNeedQuery =" + mNeedQuery);
            if (mIsInActivity) {
                mNeedQuery = true;
                if (!mIsQuerying) {
                    startAsyncQuery();
                }
            }
        }
    };

    private void initListAdapter() {
        MmsLog.d(TAG, "initListAdapter");
        if (mListAdapter == null) {
            MmsLog.d(TAG, "create it");
            mListAdapter = new FolderViewListAdapter(this, null);
            mListAdapter.setOnContentChangedListener(mContentChangedListener);
            setListAdapter(mListAdapter);
            getListView().setRecyclerListener(mListAdapter);
        }
    }

   
    @Override
    protected void onNewIntent(Intent intent) {
        // Handle intents that occur after the activity has already been created.
        setIntent(intent);
        mgViewID = intent.getIntExtra(FOLDERVIEW_KEY, 0);
        Log.d(TAG, "onNewIntent, mgViewID:" + mgViewID);
        setBoxTitle(mgViewID);
        if (mgViewID == OPTION_OUTBOX) {
            FolderView.markFailedSmsMmsSeen(this);//mark as seen
        }
        startAsyncQuery();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /// M: add for application guide. @{
        IAppGuideExt appGuideExt = (IAppGuideExt)MmsPluginManager.getMmsPluginObject(
            MmsPluginManager.MMS_PLUGIN_TYPE_APPLICATION_GUIDE);
        if (appGuideExt != null) {
            appGuideExt.showAppGuide("MMS");
        }
        ///@}
        //ComposeMessageActivity.mDestroy = true;
        /* useless?
        MessagingNotification.nonBlockingUpdateNewMessageIndicator(FolderViewList.this, false, false);
        if(EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT){
            WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(FolderViewList.this,false);
        }
        CBMessagingNotification.updateAllNotifications(FolderViewList.this);
        */
    }

    private void setBoxTitle(int id) {
        switch (id) {
            case OPTION_INBOX:
                mSpinnerTextView.setText(R.string.inbox);
                break;
            case OPTION_OUTBOX:
                mSpinnerTextView.setText(R.string.outbox);
                break;
            case OPTION_DRAFTBOX:
                mSpinnerTextView.setText(R.string.draftbox);
                break;
            case OPTION_SENTBOX:
                mSpinnerTextView.setText(R.string.sentbox);
                break;
            default:
                Log.d(TAG, "mgViewID = " + id);
                break;
        }
    }
    
    @Override
    protected void onPause() {
        //mStatusBarManager.hideSIMIndicator(getComponentName());
        super.onPause();
    }
    @Override
    protected void onStart() {
        super.onStart();

        if (mListAdapter != null) {
            MmsLog.d(TAG, "set OnContentChangedListener");
            mListAdapter.setOnContentChangedListener(mContentChangedListener);
        }
        
        if (mNeedUpdateListView) {
            Log.d(TAG,"onStart mNeedUpdateListView");
            //mListAdapter.notifyDataSetChanged();
            mListAdapter.changeCursor(null);
            mNeedUpdateListView = false;
        }
        MmsConfig.setMmsDirMode(true);
        //Notify to close dialog mode screen
        IMmsDialogNotify dialogPlugin = (IMmsDialogNotify)MmsPluginManager.getMmsPluginObject(
            MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
        dialogPlugin.closeMsgDialog();
        
        DraftCache.getInstance().addOnDraftChangedListener(this);
        DraftCache.getInstance().refresh();
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            int mindex = MmsConfig.getSimCardInfo();
            SIMInfo si = null;
            if (mindex == 0) {
                where = null;
            } else if (mindex > 0){
                si = SIMInfo.getSIMInfoBySlot(FolderViewList.this, (mindex - 1));
                if(si != null){
                    where = "sim_id = "+(int)si.getSimId();
                }
            } else {
                return;
            }
        }
        mNeedToMarkAsSeen = true;
        startAsyncQuery();
        mIsInActivity = true;
        /// M: ALPS00440523, print mms mem @{
        MmsConfig.printMmsMemStat(this, "FolderViewList.onStart");
        /// @}
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsInActivity = false;
        Contact.invalidateCache();
        DraftCache.getInstance().removeOnDraftChangedListener(this);
        if (mListAdapter != null) {
            MmsLog.d(TAG, "clear OnContentChangedListener");
            mListAdapter.setOnContentChangedListener(null);
        }
        MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        MmsLog.d(TAG, "onDestroy");
        if (mQueryHandler != null) {
            mQueryHandler.removeCallbacksAndMessages(null);
            mQueryHandler.cancelOperation(DRAFTFOLDER_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(INBOXFOLDER_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(OUTBOXFOLDER_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(SENTFOLDER_LIST_QUERY_TOKEN);
        }
        if (mListAdapter != null) {
            MmsLog.d(TAG, "clear mListAdapter");
            mListAdapter.changeCursor(null);
        }
        super.onDestroy();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isTaskRoot()) {
                    moveTaskToBack(false);
                } else {
                    finish();
                }
            return true;
            case KeyEvent.KEYCODE_SEARCH:
                if (mDisableSearchFlag) {
                    return true;
                } else {
                    break;
                }
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startAsyncQuery() {
        try {
            mNeedQuery = false;
            mIsQuerying = true;
            setProgressBarIndeterminateVisibility(true);
            MmsLog.d(TAG, "startAsyncQuery mgViewID = " + mgViewID);
            switch (mgViewID) {
                case OPTION_INBOX:
                    FolderView.startQueryForInboxView(mQueryHandler, INBOXFOLDER_LIST_QUERY_TOKEN,where);
                    MessagingNotification.cancelNotification(this, MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_OUTBOX:
                    FolderView.startQueryForOutBoxView(mQueryHandler, OUTBOXFOLDER_LIST_QUERY_TOKEN,where);
                    //MessagingNotification.cancelNotification(this, MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_DRAFTBOX:
                    FolderView.startQueryForDraftboxView(mQueryHandler, DRAFTFOLDER_LIST_QUERY_TOKEN);
                    break;
                case OPTION_SENTBOX:
                    FolderView.startQueryForSentboxView(mQueryHandler, SENTFOLDER_LIST_QUERY_TOKEN,where);
                    break;
                default:
                    break;
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void startAsyncQuery(int iPostTime) {
        try {
//            setTitle(getString(R.string.refreshing));
//            setProgressBarIndeterminateVisibility(true);
            MmsLog.d(TAG, "startAsyncQuery(int iPostTime) mgViewID = " + mgViewID);
            switch (mgViewID) {
                case OPTION_INBOX:
                    FolderView.startQueryForInboxView(mQueryHandler, INBOXFOLDER_LIST_QUERY_TOKEN,where ,iPostTime);
                    MessagingNotification.cancelNotification(this, MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_OUTBOX:
                    FolderView.startQueryForOutBoxView(mQueryHandler, OUTBOXFOLDER_LIST_QUERY_TOKEN , where, iPostTime);
                    //MessagingNotification.cancelNotification(this, MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_DRAFTBOX:
                    FolderView.startQueryForDraftboxView(mQueryHandler, DRAFTFOLDER_LIST_QUERY_TOKEN ,iPostTime);
                    break;
                case OPTION_SENTBOX:
                    FolderView.startQueryForSentboxView(mQueryHandler, SENTFOLDER_LIST_QUERY_TOKEN,where ,iPostTime);
                    break;
                default:
                    break;
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
        if (cursor == null) {
            Log.d(TAG, "cursor == null");
             return;
        }
        int type = cursor.getInt(6);
        int messageid   = cursor.getInt(0);
        
        if (mActionMode != null && mListAdapter != null) {
            long itemId = mListAdapter.getKey(type, messageid);
            mModeCallBack.setItemChecked(itemId, !mListAdapter.isContainItemId(itemId));
            mListAdapter.notifyDataSetChanged();
            return;
        }

        MmsLog.d(TAG, "messageid =" + messageid + "  mgViewID = " + mgViewID);
        if (mgViewID == OPTION_DRAFTBOX) {  //in draftbox
            long threadId = cursor.getLong(1);
            Intent it = ComposeMessageActivity.createIntent(this, threadId);
            it.putExtra("folderbox", mgViewID);
            it.putExtra("hiderecipient", false);//all draft can show editor
            it.putExtra("showinput", true);
            startActivity(it);
        } else if (type == 1) {  //sms
            Intent intent = new Intent();
            intent.setClass(context, FolderModeSmsViewer.class);
            intent.setData(ContentUris.withAppendedId(SMS_URI, messageid));
            intent.putExtra("msg_type", 1);
            intent.putExtra("folderbox", mgViewID);
            startActivityForResult(intent,REQUEST_CODE_DELETE_RESULT);
        } else if (type == 3) {  //wappush
           //messageid = cursor.getInt(1);
            Intent intent = new Intent();
            intent.setClass(context, FolderModeSmsViewer.class);
            intent.setData(ContentUris.withAppendedId(WAPPUSH_URI, messageid));
            intent.putExtra("msg_type", 3);
            intent.putExtra("folderbox", mgViewID);
            startActivity(intent);
        } else if (type == 4) {  //cb
          //  messageid = cursor.getInt(1);
            Intent intent = new Intent();
            intent.setClass(context, FolderModeSmsViewer.class);
            intent.setData(ContentUris.withAppendedId(CB_URI, messageid));
            intent.putExtra("msg_type", 4);
            intent.putExtra("folderbox", mgViewID);
            startActivity(intent);
        } else if (type == 2) { //mms
            Log.d(TAG,"TYPE1 = " + cursor.getInt(9) + "   mgViewID=" + mgViewID);
            if (cursor.getInt(9) == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
                DownloadManager dManager = DownloadManager.getInstance();
                int loadstate = dManager.getState(ContentUris.withAppendedId(MMS_URI, messageid));
                if (loadstate != DownloadManager.STATE_DOWNLOADING) {  
                    confirmDownloadDialog(new DownloadMessageListener(
                        ContentUris.withAppendedId(MMS_URI, messageid),cursor.getInt(10),messageid));
                } else {
                    Toast.makeText(context, R.string.folder_download, Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent intent = new Intent();
                intent.setClass(context, MmsPlayerActivity.class);
                intent.setData(ContentUris.withAppendedId(MMS_URI, messageid));
                intent.putExtra("dirmode", true);
                intent.putExtra("folderbox", mgViewID);
                startActivityForResult(intent,REQUEST_CODE_DELETE_RESULT);   
            }
            
        }
        
    }

    private class DownloadMessageListener implements OnClickListener {
        private final Uri mDownloadUri;
        private final int iSimid;
        private final int iMessageid;
        public DownloadMessageListener(Uri sDownloadUri,int simid,int msgid) {
            mDownloadUri = sDownloadUri;
            Log.d(TAG,"mDownloadUri =" + mDownloadUri);
            iSimid       = simid;
            Log.d(TAG,"iSimid =" + iSimid);
            iMessageid    = msgid;
            Log.d(TAG,"iMessageid =" + iMessageid);
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            markMmsIndReaded(ContentUris.withAppendedId(MMS_URI, iMessageid));
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(FolderViewList.this,
                MessagingNotification.THREAD_NONE, false);
            DownloadManager sManager = DownloadManager.getInstance();
            sManager.setState(ContentUris.withAppendedId(MMS_URI, iMessageid),DownloadManager.STATE_DOWNLOADING);
            Intent intent = new Intent(context, TransactionService.class);
            intent.putExtra(TransactionBundle.URI, mDownloadUri.toString());
            intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                    Transaction.RETRIEVE_TRANSACTION);
            // add for gemini
            intent.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, iSimid);
            context.startService(intent);
        }    
    }


//    private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener =
//        new OnCreateContextMenuListener() {
//        public void onCreateContextMenu(ContextMenu menu, View v,
//                ContextMenuInfo menuInfo) {
//            Cursor cursor = mListAdapter.getCursor();
//            if (cursor == null || cursor.getPosition() < 0) {
//                return;
//            }
//
//            int type = cursor.getInt(6);
//            int boxtype = cursor.getInt(11);
//            String recipientIds = cursor.getString(2);
//            ContactList recipients;
//            if(type == 2 || (type == 1 && boxtype ==3) || type == 4){
//                recipients = ContactList.getByIds(recipientIds, false);
//            }else{
//                recipients = ContactList.getByNumbers(recipientIds, false, true);
//            }          
//            menu.setHeaderTitle(recipients.formatNames(","));
//
//            if (recipients.size() == 1) {
//                // do we have this recipient in contacts?
//                if (recipients.get(0).existsInDatabase()) {
//                    menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact);
//                } else {
//                    menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts);
//                }
//            }
//        }
//        return super.onContextItemSelected(item);
//    }

    public void onAddContactButtonClickInt(final String number) {
        if (!TextUtils.isEmpty(number)) {
            String message = this.getResources().getString(R.string.add_contact_dialog_message, number);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                                         .setTitle(number)
                                                         .setMessage(message);
            
            AlertDialog dialog = builder.create();
            
            dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                this.getResources().getString(R.string.add_contact_dialog_existing),
                new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    intent.setType(Contacts.CONTENT_ITEM_TYPE);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
                        startActivity(intent);
                }
            });

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, this.getResources().getString(R.string.add_contact_dialog_new),
                new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
                        startActivity(intent);
                }
                
            });
            dialog.show();
        }
    }
    private void forwardMessage(String body) {
        Intent intent = new Intent();
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        intent.putExtra("forwarded_message", true);
        if (body != null) {
            intent.putExtra("sms_body", body);
        }
        startActivity(intent);
    } 
    
//    private class DeleteMessageListener implements OnClickListener {

//        public void onClick(DialogInterface dialog, int whichButton) {
//            mHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
//                    null, mDeleteUri, null, null);
//            DraftCache.getInstance().updateDraftStateInCache(threadid);
//            dialog.dismiss();
//        }
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // We override this method to avoid restarting the entire
        // activity when the keyboard is opened (declared in
        // AndroidManifest.xml).  Because the only translatable text
        // in this activity is "New Message", which has the full width
        // of phone to work with, localization shouldn't be a problem:
        // no abbreviated alternate words should be needed even in
        // 'wide' languages like German or Russian.

        super.onConfigurationChanged(newConfig);
        if (DEBUG) {
            Log.v(TAG, "onConfigurationChanged: " + newConfig);
        }
    }

    private void confirmDeleteMessageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
//        builder.setMessage(R.string.confirm_delete_allmessage);

        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(getString(R.string.confirm_delete_allmessage));

        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        checkbox.setChecked(false);
        checkbox.setVisibility(mHasLockedMsg ? View.VISIBLE : View.GONE);
        builder.setView(contents);

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface mDialog, int arg1) {
                // TODO Auto-generated method stub
                mDialog.dismiss();
                mQueryHandler.setProgressDialog(DeleteProgressDialogUtil.getProgressDialog(
                    FolderViewList.this));
                mQueryHandler.showProgressDialog();
                Uri mDeleteUri = null;
                if (mgViewID == OPTION_INBOX) {
                    mDeleteUri = ContentUris.withAppendedId(Uri.parse("content://mms-sms/folder_delete/"), 1);
                } else if (mgViewID == OPTION_OUTBOX) {
                    mDeleteUri = ContentUris.withAppendedId(Uri.parse("content://mms-sms/folder_delete/"), 4);
                } else if (mgViewID == OPTION_DRAFTBOX) {
                    mDeleteUri = ContentUris.withAppendedId(Uri.parse("content://mms-sms/folder_delete/"), 3);
                } else if (mgViewID == OPTION_SENTBOX) {
                    mDeleteUri = ContentUris.withAppendedId(Uri.parse("content://mms-sms/folder_delete/"), 2);
                }

                String whereClause = where;
                if (!checkbox.isChecked()) {
                    whereClause = where == null ? " locked=0 " : where + " AND locked=0 ";
                }
                FolderView.startDeleteBoxMessage(mQueryHandler, FOLDERVIEW_DELETE_TOKEN, mDeleteUri, whereClause);
            }          
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void confirmDownloadDialog(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.download);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_download_message);
        builder.setPositiveButton(R.string.download, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }
    
    private void showSimInfoSelectDialog() {
        Intent intent = new Intent();
        intent.setClass(context, SiminfoSelectedActivity.class);
        Log.d(TAG,"showSimInfoSelectDialog");
        startActivityForResult(intent, REQUEST_CODE_SELECT_SIMINFO);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_SIMINFO && resultCode == RESULT_OK) {
              where = data.getStringExtra("sim_id");
              Log.d(TAG, "onActivityResult where=" + where);
              startAsyncQuery();
        } else if (requestCode == REQUEST_CODE_DELETE_RESULT && resultCode == RESULT_OK) {
              mNeedUpdateListView = data.getBooleanExtra("delete_flag", false);
              Log.d(TAG, "onActivityResult mNeedUpdateListView =" + mNeedUpdateListView);
        }      
    }
    


    private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL:
                        long id = getListView().getSelectedItemId();
                        //if (id > 0) {
                            //confirmDeleteThread(id, mQueryHandler);
                        //}
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }
    };


    
    /**
     * The base class about the handler with progress dialog function.
     */
    public static abstract class BaseProgressQueryHandler extends AsyncQueryHandler {
        private NewProgressDialog dialog;
        private int progress;
        
        public BaseProgressQueryHandler(ContentResolver resolver) {
            super(resolver);
        }
        
        /**
         * Sets the progress dialog.
         * @param dialog the progress dialog.
         */
        public void setProgressDialog(NewProgressDialog cdialog) {
            this.dialog = cdialog;
        }
        
        /**
         * Sets the max progress.
         * @param max the max progress.
         */
        public void setMax(int max) {
            if (dialog != null) {
                dialog.setMax(max);
            }
        }
        
        /**
         * Shows the progress dialog. Must be in UI thread.
         */
        public void showProgressDialog() {
            if (dialog != null) {
                dialog.show();
            } else {
                Log.d(TAG,"dialog = null");
            }
        }
        
        /**
         * Rolls the progress as + 1.
         * @return if progress >= max.
         */
        protected boolean progress() {
            if (dialog != null) {
                Log.d(TAG,"progress =" + progress + ";   dialog.getMax() =" + dialog.getMax());
                return ++progress >= dialog.getMax();
            } else {
                return false;
            }
        }
        
        /**
         * Dismisses the progress dialog.
         */
        protected void dismissProgressDialog() {
            try {
                dialog.setDismiss(true);
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                dialog = null;
            }
        }
    }

    private final Runnable mDeleteObsoleteThreadsRunnable = new Runnable() {
        public void run() {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                LogTag.debug("mDeleteObsoleteThreadsRunnable getSavingDraft(): "
                        + DraftCache.getInstance().getSavingDraft());
            }
            if (DraftCache.getInstance().getSavingDraft()) {
                // We're still saving a draft. Try again in a second. We don't
                // want to delete
                // any threads out from under the draft.
                mHandler.postDelayed(mDeleteObsoleteThreadsRunnable, 1000);
            } else {
                MessageUtils.asyncDeleteOldMms();
                Conversation.asyncDeleteObsoleteThreads(mQueryHandler,
                        FOLDERVIEW_DELETE_OBSOLETE_THREADS_TOKEN);
            }
        }
    };

    private final class ThreadListQueryHandler extends BaseProgressQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mIsQuerying = false;
            if (cursor == null || cursor.getCount() == 0) {
                Log.d(TAG,"cursor == null||count==0.");
                mCountTextView.setVisibility(View.INVISIBLE);
                if (cursor != null) {
                    mListAdapter.changeCursor(cursor);
                }
                setProgressBarIndeterminateVisibility(false);
                if (mNeedQuery && mIsInActivity) {
                    startAsyncQuery();
                }
                if (mNeedToMarkAsSeen) {
                    MessagingNotification.nonBlockingUpdateSendFailedNotification(context);
                }
                invalidateOptionsMenu();
                return;
            }
            if (mListAdapter == null || mListAdapter.getOnContentChangedListener() == null) {
                MmsLog.d(TAG, "onQueryComplete, no OnContentChangedListener");
                cursor.close();
                return;
            }
            //in this case the adpter should be notifychanged.
            if (mSearchView != null) {
                String searchString = mSearchView.getQuery().toString();
                if (searchString != null && searchString.length() > 0) {
                    Log.d(TAG, "onQueryComplete mSearchView != null");
                    mSearchView.getSuggestionsAdapter().notifyDataSetChanged();
                }
            }
            mCountTextView.setVisibility(View.VISIBLE);
            switch (token) {
            case DRAFTFOLDER_LIST_QUERY_TOKEN:
                mCountTextView.setText("" + cursor.getCount());
                Log.d(TAG,"onQueryComplete DRAFTFOLDER_LIST_QUERY_TOKEN");
                mListAdapter.changeCursor(cursor);

                if (mNeedToMarkAsSeen) {
                    mNeedToMarkAsSeen = false;
                    Conversation.markAllConversationsAsSeen(getApplicationContext(),
                            Conversation.MARK_ALL_MESSAGE_AS_SEEN);
                    // Delete any obsolete threads. Obsolete threads are threads that aren't
                    // referenced by at least one message in the pdu or sms tables. We only call
                    // this on the first query (because of mNeedToMarkAsSeen).
                    mHandler.post(mDeleteObsoleteThreadsRunnable);
                }
                break;
            case INBOXFOLDER_LIST_QUERY_TOKEN:
                if (mNeedToMarkAsSeen) {
                    mNeedToMarkAsSeen = false;
                    Conversation.markAllConversationsAsSeen(getApplicationContext(),
                            Conversation.MARK_ALL_MESSAGE_AS_SEEN);
                    // Delete any obsolete threads. Obsolete threads are threads that aren't
                    // referenced by at least one message in the pdu or sms tables. We only call
                    // this on the first query (because of mNeedToMarkAsSeen).
                    mHandler.post(mDeleteObsoleteThreadsRunnable);
                }
                int count = 0;
                while (cursor.moveToNext()) {
                    if (cursor.getInt(5) == 0) {
                        count++;
                    }
                }
                mCountTextView.setText("" + count + "/" + cursor.getCount());
                Log.d(TAG,"onQueryComplete INBOXFOLDER_LIST_QUERY_TOKEN count " + count);
                mListAdapter.changeCursor(cursor);
                if (mNeedToMarkAsSeen) {
                    MessagingNotification.nonBlockingUpdateSendFailedNotification(context);
                }
                break;
            case OUTBOXFOLDER_LIST_QUERY_TOKEN:
                mCountTextView.setText("" + cursor.getCount());
                Log.d(TAG,"onQueryComplete OUTBOXFOLDER_LIST_QUERY_TOKEN");
                mListAdapter.changeCursor(cursor);
                break;
            case SENTFOLDER_LIST_QUERY_TOKEN:
                mCountTextView.setText("" + cursor.getCount());
                Log.d(TAG,"onQueryComplete SENTFOLDER_LIST_QUERY_TOKEN");
                mListAdapter.changeCursor(cursor);
                break;
            case FOLDERVIEW_HAVE_LOCKED_MESSAGES_TOKEN:
//                Collection<Long> threadIds = (Collection<Long>)cookie;
//                confirmDeleteThreadDialog(new DeleteThreadListener(threadIds, mQueryHandler,
//                    FolderViewList.this), threadIds,
//                        cursor != null && cursor.getCount() > 0,
//                        FolderViewList.this);
                break;

            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }
            /** m: add code @{ */
            invalidateOptionsMenu();
            Log.d(TAG,"onQueryComplete invalidateOptionsMenu");
            /** @} */
            setProgressBarIndeterminateVisibility(false);
            Log.d(TAG,"onQueryComplete : mNeedQuery =" + mNeedQuery);
            if (mNeedQuery && mIsInActivity) {
                startAsyncQuery();
            }
            mHasLockedMsg = false;
            if (mListItemLockInfo != null) {
                mListItemLockInfo.clear();
            }
            if (cursor != null) {
                cursor.moveToPosition(-1);
                boolean isLocked = false;
                while (cursor.moveToNext()) {
                    isLocked = cursor.getInt(13) > 0;
                    if (isLocked) {
                        mHasLockedMsg = true;
                    }
                    mListItemLockInfo.put(
                            FolderViewListAdapter.getKey(cursor.getInt(6), cursor.getInt(0)),
                            isLocked);
                }
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            // When this callback is called after deleting, token is 1803(DELETE_OBSOLETE_THREADS_TOKEN)
            // not 1801(DELETE_CONVERSATION_TOKEN)
            switch (token) {
            case FOLDERVIEW_DELETE_TOKEN:
                if (mDeleteCounter > 1) {
                    mDeleteCounter--;
                    MmsLog.d(TAG, "igonre a onDeleteComplete,mDeleteCounter:" + mDeleteCounter);
                    return;
                }
                mDeleteCounter = 0;
                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(FolderViewList.this,
                        MessagingNotification.THREAD_NONE, false);
                // Update the notification for failed messages since they
                // may be deleted.
                //MessagingNotification.updateSendFailedNotification(FolderViewList.this);
                //MessagingNotification.updateDownloadFailedNotification(FolderViewList.this);

                //Update the notification for new WAP Push messages
                if (EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT) {
                    WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(FolderViewList.this,
                                                        WapPushMessagingNotification.THREAD_NONE);
                }
                CBMessagingNotification.updateAllNotifications(FolderViewList.this);
                // Make sure the list reflects the delete
                //startAsyncQuery();
                mListAdapter.clearbackupstate();
                if (progress()) {
                    dismissProgressDialog();
                }
                break;

            case FOLDERVIEW_DELETE_OBSOLETE_THREADS_TOKEN:
                // Nothing to do here.
                break;
            default:
                break;
            }
        }
    }

    private void markMmsIndReaded(final Uri uri) {
        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);
                SqliteWrapper.update(getApplicationContext(), getContentResolver(), uri, values, null, null);
            }
        }).start();
        MessagingNotification.nonBlockingUpdateNewMessageIndicator(this, MessagingNotification.THREAD_NONE, false);
    }
    
    @Override
    public void onDraftChanged(long threadId, boolean hasDraft) {
        // TODO Auto-generated method stub
        Log.d(TAG,"Override onDraftChanged");
        if (mgViewID == OPTION_DRAFTBOX) {
            FolderView.startQueryForDraftboxView(mQueryHandler, DRAFTFOLDER_LIST_QUERY_TOKEN);       
        }       
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mListAdapter.getCount() > 0) {
            menu.add(0, MENU_MULTIDELETE, 0, R.string.menu_delete_messages);
        }
        getMenuInflater().inflate(R.menu.conversation_list_menu, menu);
        menu.removeItem(R.id.action_delete_all);
        menu.removeItem(R.id.action_debug_dump);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();

        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        mSearchView.setIconifiedByDefault(true);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            SearchableInfo info = searchManager.getSearchableInfo(this.getComponentName());
            mSearchView.setSearchableInfo(info);
        }
        
        MenuItem cellBroadcastItem = menu.findItem(R.id.action_cell_broadcasts);
        if (cellBroadcastItem != null) {
            // Enable link to Cell broadcast activity depending on the value in config.xml.
            boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                    com.android.internal.R.bool.config_cellBroadcastAppLinks);
            try {
                if (isCellBroadcastAppLinkEnabled) {
                    PackageManager pm = getPackageManager();
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                            == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                    }
                }
            } catch (IllegalArgumentException ignored) {
                isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
            }
            if (!isCellBroadcastAppLinkEnabled) {
                cellBroadcastItem.setVisible(false);
            }
        }
        
        
        menu.add(0, MENU_CHANGEVIEW, 0, R.string.changeview);
        
        menu.add(0, MENU_SIM_SMS, 0, R.string.menu_sim_sms).setIcon(
            R.drawable.ic_menu_sim_sms);
        MenuItem item = menu.findItem(MENU_SIM_SMS);
        List<SIMInfo> listSimInfo = SIMInfo.getInsertedSIMList(this);
        if (listSimInfo == null || listSimInfo.isEmpty()) {
            item.setEnabled(false);
            Log.d(TAG, "onPrepareOptionsMenu MenuItem setEnabled(false)");
        }
        
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && listSimInfo != null && 
                !listSimInfo.isEmpty() && mgViewID != OPTION_DRAFTBOX) {
            item = menu.findItem(R.id.action_siminfo);
            item.setVisible(true);
        }
    
        // omacp menu
        item = menu.findItem(R.id.action_omacp);
        item.setVisible(false);
        Context otherAppContext = null;
        try {
            otherAppContext = this.createPackageContext("com.mediatek.omacp", 
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            MmsLog.e(CONV_TAG, "ConversationList NotFoundContext");
        }
        if (null != otherAppContext) {
            SharedPreferences sp = otherAppContext.getSharedPreferences("omacp", 
                    MODE_WORLD_READABLE | MODE_MULTI_PROCESS);
            boolean omaCpShow = sp.getBoolean("configuration_msg_exist", false);
            if (omaCpShow) {
                item.setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_CHANGEVIEW:
                MmsConfig.setMmsDirMode(false);
                MessageUtils.updateNotification(this);
                startActivity(new Intent(this, ConversationList.class));
                finish();
                break;
            case R.id.action_compose_new:
                    Intent intent = new Intent(context, ComposeMessageActivity.class);
                    intent.putExtra("folderbox", mgViewID);
                    startActivity(intent);
                break;
            case R.id.action_settings:
                    Intent sintent = new Intent(this, SettingListActivity.class);
                    startActivityIfNeeded(sintent, -1);
                break;
            case R.id.action_siminfo:
                showSimInfoSelectDialog();
                break;
            case R.id.action_omacp:
                Intent omacpintent = new Intent();
                omacpintent.setClassName("com.mediatek.omacp", "com.mediatek.omacp.message.OmacpMessageList");
                omacpintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityIfNeeded(omacpintent, -1);
                break;
            case R.id.action_wappush:
                Intent wpIntent = new Intent(this, WPMessageActivity.class);
                startActivity(wpIntent);
                break;
            case MENU_MULTIDELETE: 
                confirmDeleteMessageDialog();
                break;
            case MENU_SIM_SMS:
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    List<SIMInfo> listSimInfo = SIMInfo.getInsertedSIMList(this);
                    if (listSimInfo.size() > 1) { 
                        Intent simSmsIntent = new Intent();
                        simSmsIntent.setClass(this, SelectCardPreferenceActivity.class);
                        simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        simSmsIntent.putExtra("preference", SmsPreferenceActivity.SMS_MANAGE_SIM_MESSAGES);
                        startActivity(simSmsIntent);
                    } else if (listSimInfo.size() == 1) {
                        Intent simSmsIntent = new Intent();
                        simSmsIntent.setClass(this, ManageSimMessages.class);
                        simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        simSmsIntent.putExtra("SlotId", listSimInfo.get(0).getSlot());
                        startActivity(simSmsIntent);
                    } else {
                        Toast.makeText(FolderViewList.this, R.string.no_sim_1, Toast.LENGTH_SHORT).show();
                    }
                } else { 
                    startActivity(new Intent(this, ManageSimMessages.class));
                }
                break;
            case R.id.action_cell_broadcasts:
                Intent cellBroadcastIntent = new Intent(Intent.ACTION_MAIN);
                cellBroadcastIntent.setComponent(new ComponentName(
                        "com.android.cellbroadcastreceiver",
                        "com.android.cellbroadcastreceiver.CellBroadcastListActivity"));
                cellBroadcastIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(cellBroadcastIntent);
                } catch (ActivityNotFoundException ignored) {
                    Log.e(TAG, "ActivityNotFoundException for CellBroadcastListActivity");
                }
                return true;
            default:
                return true;
        }
        return true;
    }

    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(FolderViewList.this, SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    @Override
    public boolean onSearchRequested() {
        mSearchItem.expandActionView();
        return true;
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (state.getBoolean(ACTIONMODE, false)) {
            mListSelectedItem = state.getLongArray(mSELECT_ITEM_IDS);
            mActionMode = this.startActionMode(mModeCallBack);
            Log.d(TAG, "onRestoreInstanceState: start actionMode");
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionMode != null) {
            Log.d(TAG, "onSaveInstanceState: mActionMode not null");
            outState.putBoolean(ACTIONMODE, true);
            outState.putInt(BOXTYPE, mgViewID);
            MmsLog.d(TAG, "onSaveInstanceState    mgViewID = " + mgViewID);
            outState.putBoolean(NEED_RESTORE_ADAPTER_STATE, true);
            Set<Long> selectItemId = mListAdapter.getBackUpItemList().keySet();
            Long[] selectList = (Long[])selectItemId.toArray(new Long[selectItemId.size()]);
            long[] selectedList = new long[selectList.length];
            for (int i = 0; i < selectList.length; i++) {
                selectedList[i] = selectList[i].longValue();
            }
            outState.putLongArray(mSELECT_ITEM_IDS, selectedList);
            Log.d(TAG, "onSaveInstanceState--selectItemIds:" + selectedList.toString());
        }
    }
    
    //    private class ModeCallback implements ListView.MultiChoiceModeListener {
    private class ModeCallback implements ActionMode.Callback {
        private View mMultiSelectActionBarView;
        private TextView mSelectedConvCount;
        private MenuItem mDeleteitem;
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();

            mDisableSearchFlag = true;
            mListAdapter.clearstate();
            mListAdapter.clearbackupstate();

            if (mIsNeedRestoreAdapterState) {
                for (int i = 0; i < mListSelectedItem.length; i++) {
                    mListAdapter.setSelectedState(mListSelectedItem[i]);
                }
                Log.d(TAG, "onCreateActionMode: saved selected number " + mListAdapter.getSelectedNumber());
                mIsNeedRestoreAdapterState = false;
            } else {
                Log.d(TAG, "onCreateActionMode: no need to restore adapter state");
            }

            inflater.inflate(R.menu.folderview_multi_select_menu, menu);
            mDeleteitem = menu.findItem(R.id.folderview_delete);
            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = (ViewGroup)LayoutInflater.from(FolderViewList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar, null);

                mSelectedConvCount =
                    (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_conv_count);

                mSelectedConvCount.setText(Integer.toString(mListAdapter.getSelectedNumber()));

            }
            mode.setCustomView(mMultiSelectActionBarView);
            ((TextView)mMultiSelectActionBarView.findViewById(R.id.title))
                .setText(R.string.select_message);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup)LayoutInflater.from(FolderViewList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar, null);
                mode.setCustomView(v);

                mSelectedConvCount = (TextView)v.findViewById(R.id.selected_conv_count);

                mSelectedConvCount.setText(Integer.toString(mListAdapter.getSelectedNumber()));

            }
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.folderview_delete:
                    if (mListAdapter.getItemList().size() > 0) {
                        confirmMultiDelete();
//                        mode.finish();
                    } else {
//                        mHandler.post(new Runnable() {
//
//                            public void run() {
//                                Toast.makeText(FolderViewList.this, 
//                                        R.string.no_item_selected, Toast.LENGTH_SHORT).show();
//                            }
//                        });
                          item.setEnabled(false);  
                    }
                    break;

                case R.id.folderview_select_all:
                    isSelectAll(true);
                    break;
                case R.id.folderview_cancel_select:
                    isSelectAll(false);
                    mode.finish();
                    break;

                default:
                    break;
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            mListAdapter.clearstate();
            mDisableSearchFlag = false;

            getListView().setLongClickable(true);
            mActionMode = null;
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }

        }

        public void setItemChecked(long itemId, boolean checked) {
            Log.d(TAG, "itemId =" + itemId);
            if (checked) {
                 mListAdapter.setSelectedState(itemId);
            } else {
                 mListAdapter.removeSelectedState(itemId);
            }
            int num = mListAdapter.getSelectedNumber(); 
            if (num > 0) {
                mSelectedConvCount.setText(Integer.toString(num));
                if (mDeleteitem != null) {
                    if (num > 0) {
                        mDeleteitem.setEnabled(true);
                    } else {
                        mDeleteitem.setEnabled(false);
                    }
                }
            } else if (mActionMode != null) {
                mActionMode.finish();
            }
            
            Log.d(TAG, "setItemChecked:checked count = " + num);
        }
        
        private void isSelectAll(boolean check) {
            cancelSelect();
            if (check) {
                Log.d(TAG, "select all messages, count is : " + mListAdapter.getCount());
                long itemId = -1;
                int selectCount = mListAdapter.getCount();
                for (int i = 0; i < selectCount; i++) {
                    itemId = mListAdapter.getItemId(i);
                    mListAdapter.setSelectedState(itemId);
                }
                mDeleteitem.setEnabled(true);
            } else {
                mDeleteitem.setEnabled(false);
            }
            mSelectedConvCount.setText(Integer.toString(mListAdapter.getSelectedNumber()));
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }
        private void cancelSelect() {
            Log.d(TAG, "cancel select messages.");
            mListAdapter.clearbackupstate();
            mListAdapter.clearstate();
        }

    }
    
    private void confirmMultiDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
//        builder.setMessage(R.string.confirm_delete_selected_messages);

        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(getString(R.string.confirm_delete_selected_messages));

        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        checkbox.setChecked(false);
        checkbox.setVisibility(selectedMsgHasLocked() ? View.VISIBLE : View.GONE);
        builder.setView(contents);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mQueryHandler.setProgressDialog(DeleteProgressDialogUtil.getProgressDialog(
                    FolderViewList.this));
                mQueryHandler.showProgressDialog();
                new Thread(new Runnable() {
                    public void run() {
                        Log.d(TAG,"mListAdapter.getBackUpItemList() =" + mListAdapter.getBackUpItemList());
                        mDeleteCounter = 0;
                        Iterator iter = mListAdapter.getBackUpItemList().entrySet().iterator();
                        Uri deleteSmsUri = Sms.CONTENT_URI;
                        Uri deleteMmsUri = null;
                        Uri deleteCbUri  = null;
                        Uri deleteWpUri = null;
                        Log.d(TAG,"mListAdapter.getSelectedNumber() =" + mListAdapter.getSelectedNumber());
                        String[] argsSms = new String[mListAdapter.getSelectedNumber()];
                        String[] argsMms = new String[mListAdapter.getSelectedNumber()];
                        String[] argsCb = new String[mListAdapter.getSelectedNumber()];
                        // String[] argsWp = new String[mListAdapter.getSelectedNumber()];
                        int i = 0;
                        int j = 0;
                        int k = 0;
                        int m = 0;
                        while (iter.hasNext()) {
                            @SuppressWarnings("unchecked")
                            Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                            if (entry.getValue()) {                  
                                if (!checkbox.isChecked()) {
                                    if (isMsgLocked(entry.getKey())) {
                                        continue;
                                    }
                                }
                                if (entry.getKey() > 100000) {
                                    deleteWpUri = ContentUris.withAppendedId(WAPPUSH_URI, entry.getKey() - 100000);
                                    Log.i(TAG, "wappush :entry.getKey()-100000 = " + (entry.getKey() - 100000));
                                    mDeleteCounter++;
                                    MmsLog.d(TAG, "wappush mDeleteCounter = " + mDeleteCounter);
                                    mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                        null, deleteWpUri, null, null);                              
                                    m++;
                                } else if (entry.getKey() < -100000) {
                                    argsCb[k] = Long.toString(-(entry.getKey() + 100000));
                                    Log.i(TAG, "CB :-entry.getKey() +100000= " + (-(entry.getKey() + 100000)));
                                    Log.i(TAG, "argsSms[i]" + argsCb[k]);
                       
                                    deleteCbUri = CB_URI;
                                    k++;
                                } else if (entry.getKey() < 0) {
                                    argsMms[j] = Long.toString(-entry.getKey());
                                    Log.i(TAG, "mms :-entry.getKey() = " + (-entry.getKey()));
                                    Log.i(TAG, "argsMms[j]" + argsMms[j]);
                  
                                    deleteMmsUri = Mms.CONTENT_URI;
                                    j++;
                                } else if (entry.getKey() > 0) {
                                    Log.i(TAG, "sms");
                                    argsSms[i] = Long.toString(entry.getKey());
                                    Log.i(TAG, "argsSms[i]" + argsSms[i]);
                                    deleteSmsUri = Sms.CONTENT_URI;
                                    i++;
                                }
                            }
                             
                        }

//                        mQueryHandler.setMax(
//                             (deleteSmsUri != null ? 1 : 0) +
//                             (deleteMmsUri != null ? 1 : 0)+(deleteCbUri != null ? 1 : 0));
                        if (deleteSmsUri != null) {
                            mDeleteCounter++;
                        }
                        if (deleteMmsUri != null) {
                            mDeleteCounter++;
                        }
                        if (deleteCbUri != null) {
                            mDeleteCounter++;
                        }
                        MmsLog.d(TAG, "mDeleteCounter = " + mDeleteCounter);
                        if (deleteSmsUri != null) {
                            mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                null, deleteSmsUri, FOR_MULTIDELETE, argsSms);
                        }
                       
                        if (deleteMmsUri != null) {
                            mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                    null, deleteMmsUri, FOR_MULTIDELETE, argsMms);
                        }
                        if (deleteCbUri != null) {
                            mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                    null, deleteCbUri, FOR_MULTIDELETE, argsCb);
                        }
                        runOnUiThread(new Runnable() {

                            public void run() {
                                if (mActionMode != null) {
                                    mActionMode.finish();
                                }
                            }
                        });
                    }
                }).start();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }
    
    private class MultiSelectOnLongClickListener implements OnItemLongClickListener {

        public boolean onItemLongClick(AdapterView<?> parent, View view,
                int position, long id) {
            Log.d(TAG, "folder view: MultiSelectOnLongClickListener");
            getListView().setLongClickable(false);
//            mModeCallBack = new ModeCallback();
            mActionMode = startActionMode(mModeCallBack);
            mModeCallBack.setItemChecked(mListAdapter.getItemId(position), true);
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            return true;
        }
    }

    private boolean isMsgLocked(long id) {
        if (mListItemLockInfo != null && mListItemLockInfo.containsKey(id)) {
            return mListItemLockInfo.get(id);
        }
        return false;
    }

    private boolean selectedMsgHasLocked() {
        Iterator iter = mListAdapter.getBackUpItemList().entrySet().iterator();
        while (iter.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
            if (entry.getValue()) {
                if (isMsgLocked(entry.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

}
