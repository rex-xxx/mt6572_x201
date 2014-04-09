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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.Conversation.ConversationQueryHandler;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsRejectedReceiver;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.CustomMenu.DropDownMenu;
import com.android.mms.util.DraftCache;
import com.android.mms.util.Recycler;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.pdu.PduHeaders;

import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.database.sqlite.SqliteWrapper;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

/// M:
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.android.mms.MmsConfig;
import com.android.mms.MmsApp;
import com.android.mms.transaction.CBMessagingNotification;
import com.android.mms.transaction.WapPushMessagingNotification;
import com.android.mms.transaction.MmsSystemEventReceiver.OnSimInforChangedListener;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import android.app.StatusBarManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.os.ServiceManager;
import android.provider.Telephony.Sms;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import android.net.Uri;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import android.os.SystemProperties;
import android.util.AndroidException;

import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.encapsulation.android.app.EncapsulatedStatusBarManager;
import com.mediatek.encapsulation.android.content.res.EncapsulatedResources;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.android.mms.MmsPluginManager;
import com.mediatek.mms.ext.IMmsConversation;
import com.mediatek.mms.ext.MmsConversationImpl;
import com.mediatek.mms.ext.IMmsConversationHost;
import com.mediatek.mms.ext.IMmsDialogNotify;
import com.mediatek.mms.ext.IAppGuideExt;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.WapPush;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.ipmsg.ui.ConversationEmptyView;
import com.mediatek.ipmsg.util.IpMessageUtils;

import java.io.File;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Environment;
import android.provider.Telephony.Sms.Conversations;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;

import com.mediatek.mms.ipmessage.INotificationsListener;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.ContactStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
import com.mediatek.mms.ipmessage.IpMessageConsts.SelectContactType;

/**
 * This activity provides a list view of existing conversations.
 */
public class ConversationList extends ListActivity implements DraftCache.OnDraftChangedListener,
        /// M:add interface
        OnSimInforChangedListener, IMmsConversationHost, INotificationsListener {
    private static final String TAG = "ConversationList";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG;

    private static final int THREAD_LIST_QUERY_TOKEN       = 1701;
    private static final int UNREAD_THREADS_QUERY_TOKEN    = 1702;
    public static final int DELETE_CONVERSATION_TOKEN      = 1801;
    public static final int HAVE_LOCKED_MESSAGES_TOKEN     = 1802;
    private static final int DELETE_OBSOLETE_THREADS_TOKEN = 1803;

    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;
    public static final int MENU_VIEW_CONTACT         = 2;
    public static final int MENU_ADD_TO_CONTACTS      = 3;
    private ThreadListQueryHandler mQueryHandler;
    private ConversationListAdapter mListAdapter;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private boolean mNeedToMarkAsSeen;
    private TextView mUnreadConvCount;
    private MenuItem mSearchItem;
    /// M: fix bug ALPS00374917, cancel sim_sms menu when haven't sim card
    private MenuItem mSimSmsItem;
    private SearchView mSearchView;

    /// Google JB MR1.1 patch. conversation list can restore scroll position
    private int mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
    private int mSavedFirstItemOffset;
    // keys for extras and icicles
    private final static String LAST_LIST_POS = "last_list_pos";
    private final static String LAST_LIST_OFFSET = "last_list_offset";

    private static final String CHECKED_MESSAGE_LIMITS = "checked_message_limits";

    /// M: new members
    private static final String CONV_TAG = "Mms/convList";
    /// M: Code analyze 002, For new feature ALPS00041233, Gemini enhancment check in . @{
    private EncapsulatedStatusBarManager mStatusBarManager;
    /// @}
    /// M: Code analyze 001, For new feature ALPS00131956, wappush: add new params . @{
    private int mType;
    private static final String WP_TAG = "Mms/WapPush";
    /// @}
    /// M: Code analyze 004, For bug ALPS00247476, ensure the scroll smooth . @{
    private static final int CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT = 100;
    /// @}
    /// M: Code analyze 004, For bug ALPS00247476, ensure the scroll smooth . @{
    private MyScrollListener mScrollListener =
                    new MyScrollListener(CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT, "ConversationList_Scroll_Tread");
    /// @}

    /// M: Code analyze 007, For bug ALPS00242955, If adapter data is valid . @{
    private boolean mDataValid;
    /// @}
    /// M: Code analyze 008, For bug ALPS00250948, disable search in multi-select status . @{
    private boolean mDisableSearchFalg = false;
    /// M: Code analyze 006, For bug ALPS00291435, solve no response while deleting 50 messages . @{
    private static int sDeleteCounter = 0;
    /// @}
    /// M: Code analyze 005, For new feature ALPS00247476, add selectAll/unSelectAll . @{
    private ModeCallback mActionModeListener = new ModeCallback();
    private ActionMode mActionMode;
    /// @}

    /// M: Code analyze 009, For bug ALPS00270910, Default SIM card icon shown in status bar
    /// is incorrect, need to get current sim information . @{
    private static Activity sActivity = null;
    /// @}

    /// M: Code analyze 009, For new feature, plugin . @{
    private IMmsConversation mMmsConversationPlugin = null;
    /// @}

    /** M: this is used to record the fontscale, if it is > 1.1[1.1 is big style]
     *  we need make the content view of conversationlistitem to be one line
     *  or it will overlapping with the above from view.
     */ 
    private float mFontScale;
    public static final float MAX_FONT_SCALE = 1.1f;

    /// M: add for ipmessage
    /// M: add for display unread thread count
    private static final int MAX_DISPLAY_UNREAD_COUNT = 99;
    private static final String DISPLAY_UNREAD_COUNT_CONTENT_FOR_ABOVE_99 = "99+";

    /// M: add for ipmessage {@
    private static final String IPMSG_TAG = "Mms/ipmsg/ConvList";

    /// M: add for drop down list
    public static final int OPTION_CONVERSATION_LIST_ALL         = 0;
    public static final int OPTION_CONVERSATION_LIST_IMPORTANT   = 1;
    public static final int OPTION_CONVERSATION_LIST_GROUP_CHATS = 2;
    public static final int OPTION_CONVERSATION_LIST_SPAM        = 3;

    private static final String DROP_DOWN_KEY_NAME   = "drop_down_menu_text";
    private ListView mListView; // we need this to update empty view.
    private View mEmptyViewDefault;
    private ConversationEmptyView mEmptyView;

    private ArrayAdapter<String> mDropdownAdapter;
    private AccountDropdownPopup mAccountDropdown;
    private Context mContext = null;
    public static int sConversationListOption = OPTION_CONVERSATION_LIST_ALL;

    private View mConversationSpinner;
    private TextView mSpinnerTextView;
    private ProgressDialog mSaveChatHistory;
    boolean mIsSendEmail = false;

    private int mTypingCounter;
    private LinearLayout mNetworkStatusBar;
    private BroadcastReceiver mNetworkStateReceiver;

    private static final String SAVE_HISTORY_MIMETYPE_ZIP = "application/zip";
    private static final String SAVE_HISTORY_SUFFIX = ".zip";
    private static final String SAVE_HISTORY_MIMETYPE_TEXT = "text/plain";

    /// M: Remove cursor leak @{
    private boolean mNeedQuery = false;    //If onContentChanged is called, set it means we need query again to refresh list
    private boolean mIsInActivity = false; //If activity is not displayed, no need do query
    /// @}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /// M: Code analyze 009, For new feature, plugin . @{
        initPlugin(this);
        /// @}
        /// M: Code analyze 009, For bug ALPS00270910, Default SIM card icon shown in status
        /// bar is incorrect, need to get current sim information . @{
        sActivity = ConversationList.this;
        /// @}
        /// M: Code analyze 010, new feature, MTK_OP01_PROTECT_START . @{
        Intent intent;
        boolean dirMode;
        dirMode = MmsConfig.getMmsDirMode();
        if (MmsConfig.getFolderModeEnabled() && dirMode) {
            intent = new Intent(this, FolderViewList.class);
            intent.putExtra("floderview_key", FolderViewList.OPTION_INBOX);// show inbox by default
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            finish();
            startActivity(intent);
        }
        /// @}
        /// M: Code analyze 012, new feature, mms dialog notify . @{
        IMmsDialogNotify dialogPlugin =
                    (IMmsDialogNotify)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
        dialogPlugin.closeMsgDialog();
        /// @}

        setContentView(R.layout.conversation_list_screen);

        /// M: Code analyze 002, For new feature ALPS00041233, Gemini enhancment check in . @{
        mStatusBarManager = new EncapsulatedStatusBarManager(ConversationList.this);
        /// @}
        mQueryHandler = new ThreadListQueryHandler(getContentResolver());

        mListView = getListView();
        mListView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        mListView.setOnKeyListener(mThreadListKeyListener);
        /// M: Code analyze 005, For new feature ALPS00247476, add selectAll/unSelectAll . @{
        mListView.setOnScrollListener(mScrollListener);
        /// @}

        /// M: add for ipmessage
        if (MmsConfig.getIpMessagServiceId(this) > IpMessageServiceId.NO_SERVICE) {
            IpMessageUtils.addIpMsgNotificationListeners(this, this);
        }

        // Tell the list view which view to display when the list is empty
        mEmptyViewDefault = findViewById(R.id.empty);
        mEmptyView = (ConversationEmptyView)findViewById(R.id.empty2);
        if (!MmsConfig.isActivated(this)) {
            mListView.setEmptyView(mEmptyViewDefault);
        } else {
            mEmptyViewDefault.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mListView.setEmptyView(mEmptyView);
        }
        mNetworkStatusBar = (LinearLayout) findViewById(R.id.no_itnernet_view);
        TextView networkStatusTextView = ((TextView) mNetworkStatusBar.findViewById(R.id.no_internet_text));
        if (networkStatusTextView != null) {
            networkStatusTextView.setText(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_no_internet));
        }

        mListView.setOnItemLongClickListener(new ItemLongClickListener());
        
        initListAdapter();

        mContext = ConversationList.this;
        if (MmsConfig.isActivated(this)) {
            Conversation.setActivated(true);
            initSpinnerListAdapter();
            setTitle("");
        } else {
            MmsLog.d(TAG, "normal message layout");
            setupActionBar();
            setTitle(R.string.app_label);
        }

        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean checkedMessageLimits = mPrefs.getBoolean(CHECKED_MESSAGE_LIMITS, false);
        if (DEBUG) Log.v(TAG, "checkedMessageLimits: " + checkedMessageLimits);
        if (!checkedMessageLimits || DEBUG) {
            runOneTimeStorageLimitCheckForLegacyMessages();
        }

        /** M: get fontscale
         *  we only need to set it to true if needed
         *  font scale change will make this activity create again
         */
        mFontScale = getResources().getConfiguration().fontScale;
        if (mFontScale > MAX_FONT_SCALE) {
            MmsLog.d(TAG, "system fontscale is:" + mFontScale);
            mListAdapter.setSubjectSingleLineMode(true);
        }

        /// Google JB MR1.1 patch. conversation list can restore scroll position
        if (savedInstanceState != null) {
            mSavedFirstVisiblePosition = savedInstanceState.getInt(LAST_LIST_POS,
                    AdapterView.INVALID_POSITION);
            mSavedFirstItemOffset = savedInstanceState.getInt(LAST_LIST_OFFSET, 0);
        } else {
            mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
            mSavedFirstItemOffset = 0;
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();

        ViewGroup v = (ViewGroup)LayoutInflater.from(this)
            .inflate(R.layout.conversation_list_actionbar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));

        mUnreadConvCount = (TextView)v.findViewById(R.id.unread_conv_count);
    }

    private final ConversationListAdapter.OnContentChangedListener mContentChangedListener =
        new ConversationListAdapter.OnContentChangedListener() {
        @Override
        public void onContentChanged(ConversationListAdapter adapter) {
            /// M: Remove cursor leak and reduce needless query @{
            /* Only need when activity is shown*/
            if (mIsInActivity) {
                mNeedQuery = true;
                startAsyncQuery(200);
            }
            /// @}
        }
    };

    private void initListAdapter() {
        mListAdapter = new ConversationListAdapter(this, null);
        /** M: now this code is useless and will lead to a JE, comment it.
         *  listener is set in onStart
         */
        //mListAdapter.setOnContentChangedListener(mContentChangedListener);
        setListAdapter(mListAdapter);
        getListView().setRecyclerListener(mListAdapter);
    }

    /**
     * Checks to see if the number of MMS and SMS messages are under the limits for the
     * recycler. If so, it will automatically turn on the recycler setting. If not, it
     * will prompt the user with a message and point them to the setting to manually
     * turn on the recycler.
     */
    public synchronized void runOneTimeStorageLimitCheckForLegacyMessages() {
        if (Recycler.isAutoDeleteEnabled(this)) {
            if (DEBUG) Log.v(TAG, "recycler is already turned on");
            // The recycler is already turned on. We don't need to check anything or warn
            // the user, just remember that we've made the check.
            markCheckedMessageLimit();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Recycler.checkForThreadsOverLimit(ConversationList.this)) {
                    if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit TRUE");
                    // Dang, one or more of the threads are over the limit. Show an activity
                    // that'll encourage the user to manually turn on the setting. Delay showing
                    // this activity until a couple of seconds after the conversation list appears.
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(ConversationList.this,
                                    WarnOfStorageLimitsActivity.class);
                            startActivity(intent);
                        }
                    }, 2000);
                /** M: comment this else block
                } else {
                    if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit silently turning on recycler");
                    // No threads were over the limit. Turn on the recycler by default.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putBoolean(GeneralPreferenceActivity.AUTO_DELETE, true);
                            editor.apply();
                        }
                    });
                */
                }
                // Remember that we don't have to do the check anymore when starting MMS.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markCheckedMessageLimit();
                    }
                });
            }
        }, "ConversationList.runOneTimeStorageLimitCheckForLegacyMessages").start();
    }

    /**
     * Mark in preferences that we've checked the user's message limits. Once checked, we'll
     * never check them again, unless the user wipe-data or resets the device.
     */
    private void markCheckedMessageLimit() {
        if (DEBUG) Log.v(TAG, "markCheckedMessageLimit");
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(CHECKED_MESSAGE_LIMITS, true);
        editor.apply();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Handle intents that occur after the activity has already been created.
        startAsyncQuery();
        /// M: Code analyze 012, new feature, mms dialog notify . @{
        IMmsDialogNotify dialogPlugin =
                (IMmsDialogNotify)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
        dialogPlugin.closeMsgDialog();
        /// @}
    }

    @Override
    protected void onStart() {
        super.onStart();
        /// M: Code analyze 010, new feature, MTK_OP01_PROTECT_START . @{
        MmsConfig.setMmsDirMode(false);
        MmsLog.i(TAG,"[Performance test][Mms] loading data start time ["
            + System.currentTimeMillis() + "]");
        // ipmessage is activited.
        if ((mDropdownAdapter == null) && MmsConfig.isActivated(this)) {
            Conversation.setActivated(true);
            initSpinnerListAdapter();
            setTitle("");
            mEmptyViewDefault.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mListView.setEmptyView(mEmptyView);
            invalidateOptionsMenu();
        }
        // if ipmessage is actived, the menu sendinvitation will shown/hide if enabled/disabled.
        // so we need refresh menu.
        /// this menu is removed, so comment it
        if (MmsConfig.isServiceEnabled(this)) {
            if (mNetworkStateReceiver == null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                mNetworkStateReceiver = new NetworkStateReceiver();
                registerReceiver(mNetworkStateReceiver, filter);
            }
        }
        /// @}
        MessagingNotification.cancelNotification(getApplicationContext(),
                SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);

        DraftCache.getInstance().addOnDraftChangedListener(this);

        mNeedToMarkAsSeen = true;
        startAsyncQuery();

        /// M: setOnContentChangedListener here, it will be removed in onStop @{
        mIsInActivity = true;
        if (mListAdapter != null) {
            MmsLog.d(TAG, "set onContentChanged listener");
            mListAdapter.setOnContentChangedListener(mContentChangedListener);
        }
        /// @}
        // We used to refresh the DraftCache here, but
        // refreshing the DraftCache each time we go to the ConversationList seems overly
        // aggressive. We already update the DraftCache when leaving CMA in onStop() and
        // onNewIntent(), and when we delete threads or delete all in CMA or this activity.
        // I hope we don't have to do such a heavy operation each time we enter here.
        /// M: Code analyze 0014, For new feature, third party may add/delete
        /// draft, and we must refresh to check this.
        DraftCache.getInstance().refresh();
        /// @}

        // we invalidate the contact cache here because we want to get updated presence
        // and any contact changes. We don't invalidate the cache by observing presence and contact
        // changes (since that's too untargeted), so as a tradeoff we do it here.
        // If we're in the middle of the app initialization where we're loading the conversation
        // threads, don't invalidate the cache because we're in the process of building it.
        // TODO: think of a better way to invalidate cache more surgically or based on actual
        // TODO: changes we care about
        if (!Conversation.loadingThreads()) {
            Contact.invalidateCache();
        }

        /// M: ALPS00440523, print mms mem @{
        MmsConfig.printMmsMemStat(this, "ConversationList.onStart");
        /// @}
    }

    @Override
    protected void onStop() {
        super.onStop();

        /// M: @{
        mIsInActivity = false;
        /// @}

        DraftCache.getInstance().removeOnDraftChangedListener(this);

        /// M: Remove this listener so that no needless query when activity exit but not released yet @{
        if (mListAdapter != null) {
            MmsLog.d(TAG, "remove OnContentChangedListener");
            mListAdapter.setOnContentChangedListener(null);
        }
        if (mQueryHandler != null) {
            MmsLog.d(TAG, "cancel undone queries in onStop");
            mQueryHandler.cancelOperation(THREAD_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(UNREAD_THREADS_QUERY_TOKEN);
            mNeedQuery = false;
        }
        /// @}

        // Simply setting the choice mode causes the previous choice mode to finish and we exit
        // multi-select mode (if we're in it) and remove all the selections.
        /// M:comment a line
        //getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        /// M: comment this line
        //mListAdapter.changeCursor(null);

        /// M: Code analyze 021, For bug, delete thread on widget after delete the draft . @{
        Log.v(TAG,"update MmsWidget");
        MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
        /// @}
    }

    @Override
    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        // Run notifyDataSetChanged() on the main thread.
        mQueryHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("onDraftChanged: threadId=" + threadId + ", hasDraft=" + hasDraft);
                }
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void startAsyncQuery() {
        startAsyncQuery(10);
    }

    private void startAsyncQuery(long delay) {
        try {
            /// M: add for ipmessage
            if (Conversation.getActivated()) {
                String selection = null;
                mNeedQuery = false;

                switch (sConversationListOption) {
                case OPTION_CONVERSATION_LIST_ALL:
                    MmsLog.d(IPMSG_TAG, "startAsyncQuery(): query for all messages except spam");
                    mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                        .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_all));
                    selection = "threads._id not in (SELECT DISTINCT "
                                + Sms.THREAD_ID
                                + " FROM thread_settings WHERE spam=1) ";
                    Conversation.startQueryExtend(mQueryHandler, THREAD_LIST_QUERY_TOKEN, selection);
                    Conversation.startQuery(mQueryHandler, UNREAD_THREADS_QUERY_TOKEN, Threads.READ + "=0"
                            + " and " + selection, 10);
                    break;
                case OPTION_CONVERSATION_LIST_IMPORTANT:
                    mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                        .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_important));
                    selection = "threads._id IN (SELECT " + Sms.THREAD_ID + " FROM sms WHERE " + Sms.LOCKED + "=1" +
                                " UNION SELECT " + Mms.THREAD_ID + " FROM pdu WHERE " + Mms.LOCKED + "=1)";
                    MmsLog.d(IPMSG_TAG, "startAsyncQuery(): query for important messages, selection = " + selection);
                    Conversation.startQueryExtend(mQueryHandler, THREAD_LIST_QUERY_TOKEN, selection);
                    Conversation.startQuery(mQueryHandler, UNREAD_THREADS_QUERY_TOKEN, Threads.READ + "=0"
                            + " and " + selection, 10);
                    break;
                case OPTION_CONVERSATION_LIST_GROUP_CHATS:
                    MmsLog.d(IPMSG_TAG, "startAsyncQuery(): query for group messages");
                    mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                        .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_group_chats));
                    selection = "threads._id IN (SELECT DISTINCT " + Sms.THREAD_ID
                            + " FROM thread_settings WHERE spam=0)"
                            + " AND threads.recipient_ids IN (SELECT _id FROM canonical_addresses" + " WHERE "
                            + "SUBSTR(address, 1, 4) = '" + IpMessageConsts.GROUP_START + "'" + ")";

                    Conversation.startQueryExtend(mQueryHandler, THREAD_LIST_QUERY_TOKEN, selection);
                    Conversation.startQuery(mQueryHandler, UNREAD_THREADS_QUERY_TOKEN, Threads.READ + "=0"
                            + " and " + selection, 10);
                    break;
                case OPTION_CONVERSATION_LIST_SPAM:
                    mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                        .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_spam));
                    //selection = Threads.SPAM + "=1 OR _ID in (SELECT DISTINCT " + Sms.THREAD_ID + " FROM sms WHERE "
                    //        + Sms.SPAM + "=1) ";
                    selection = "threads._id IN (SELECT DISTINCT " + Sms.THREAD_ID + " FROM thread_settings WHERE spam=1) ";
                    MmsLog.d(IPMSG_TAG, "startAsyncQuery(): query for spam messages, selection = " + selection);
                    Conversation.startQueryExtend(mQueryHandler, THREAD_LIST_QUERY_TOKEN, selection);
                    Conversation.startQuery(mQueryHandler, UNREAD_THREADS_QUERY_TOKEN, Threads.READ + "=0"
                            + " and " + selection, 10);
                    break;
                default:
                    break;
                }
                /// M: update dropdown list
                mDropdownAdapter = getDropDownMenuData(mDropdownAdapter, sConversationListOption);
            } else {
                /// M: @{
                mNeedQuery = false;
                /// @}
                ((TextView)(mEmptyViewDefault)).setText(R.string.loading_conversations);
                Conversation.startQueryForAll(mQueryHandler, THREAD_LIST_QUERY_TOKEN, delay);
                Conversation.startQuery(mQueryHandler, UNREAD_THREADS_QUERY_TOKEN, Threads.READ + "=0", delay);
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(ConversationList.this, SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversation_list_menu, menu);

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

        /// M: add for ipmessage menu
        if (MmsConfig.isActivated(this)) {
            MenuItem item = menu.findItem(R.id.create_group_chat);
            if (item != null &&
                IpMessageUtils.getServiceManager(this).isFeatureSupported(IpMessageConsts.FeatureId.GROUP_MESSAGE)) {
                item.setVisible(true);
            }
            /*
            if (MmsConfig.isServiceEnabled(this)) {
                item = menu.findItem(R.id.send_invitations);
                item.setVisible(true);
            } else {
                item = menu.findItem(R.id.send_invitations);
                item.setVisible(false);
            }
            */
        }

        /// M: Code analyze 009, For new feature, plugin . @{
        mMmsConversationPlugin.addOptionMenu(menu, MmsConfig.getPluginMenuIDBase());
        /// @}

        /// M: fix bug ALPS00374917, cancel sim_sms menu when haven't sim card
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item != null && item.isVisible() &&
                     item.getTitle().equals(getResources().getText(R.string.menu_sim_sms))) {
                mSimSmsItem = item;
                break;
            }
        }

        if (mSimSmsItem != null) {
            List<SIMInfo> listSimInfo = SIMInfo.getInsertedSIMList(getContext());
            if (listSimInfo == null || listSimInfo.isEmpty()) {
                mSimSmsItem.setEnabled(false);
            } else {
                mSimSmsItem.setEnabled(true);
            }
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mOptionsMenu = menu ;
        setDeleteMenuVisible(menu);
        MenuItem item;
        if (!LogTag.DEBUG_DUMP) {
            item = menu.findItem(R.id.action_debug_dump);
            if (item != null) {
                item.setVisible(false);
            }
        }

        /// M: Code analyze 011, add code for omacp . @{
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
        /// @}

        /// M: fix bug ALPS00374917, cancel sim_sms menu when haven't sim card
        if (mSimSmsItem != null) {
            List<SIMInfo> listSimInfo = SIMInfo.getInsertedSIMList(getContext());
            if (listSimInfo == null || listSimInfo.isEmpty()) {
                mSimSmsItem.setEnabled(false);
            } else {
                mSimSmsItem.setEnabled(true);
            }
        }

        /// M: add for ipmessage menu
        if (MmsConfig.isActivated(this)) {
            MenuItem createGroupItem = menu.findItem(R.id.create_group_chat);
            createGroupItem.setVisible(true);
            /*
            if (MmsConfig.isServiceEnabled(this)) {
                item = menu.findItem(R.id.send_invitations);
                item.setVisible(true);
            } else {
                item = menu.findItem(R.id.send_invitations);
                item.setVisible(false);
            }
            */
        }
        item = menu.findItem(R.id.action_wappush);
        item.setVisible(true);
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        if (mSearchItem != null) {
            mSearchItem.expandActionView();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /// M: Code analyze 009, For new feature, plugin . @{
        if (mMmsConversationPlugin.onOptionsItemSelected(item)) {
            return true;
        }
        /// @}

        /// M: add for ipmessage menu
        if (MmsConfig.isActivated(this)) {
            switch (item.getItemId()) {
            case R.id.create_group_chat:
                if (IpMessageUtils.checkCurrentIpMessageServiceStatus(this, true, null)) {
                    Intent createGroupIntent = new Intent(RemoteActivities.CONTACT);
                    createGroupIntent.putExtra(RemoteActivities.KEY_TYPE, SelectContactType.IP_MESSAGE_USER);
                    createGroupIntent.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_SELECT_CONTACT_FOR_GROUP);
                    IpMessageUtils.startRemoteActivity(this, createGroupIntent);
                } else {
                    return true;
                }
                break;
            case R.id.send_invitations:
                // TODO send invitation
                Intent createGroupIntent2 = new Intent(RemoteActivities.CONTACT);
                createGroupIntent2.putExtra(RemoteActivities.KEY_TYPE, SelectContactType.NOT_IP_MESSAGE_USER);
                createGroupIntent2.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_INVITE);
                IpMessageUtils.startRemoteActivity(this, createGroupIntent2);
                break;
            default:
                break;
            }
        }

        switch(item.getItemId()) {
            case R.id.action_compose_new:
                createNewMessage();
                break;
            case R.id.action_delete_all:
                /// M: ip message don't delete all threads, always delete selected.
                if (MmsConfig.isActivated(this)) {
                    ArrayList<Long> threadIds = new ArrayList<Long>();
                    ListView listView = getListView();
                    ConversationListAdapter adapter = (ConversationListAdapter)listView.getAdapter();
                    int num = adapter.getCount();
                    for (int position = 0; position < num; position++) {
                        Cursor cursor = (Cursor)listView.getItemAtPosition(position);
                        Conversation conv = Conversation.getFromCursor(ConversationList.this, cursor);
                        threadIds.add(conv.getThreadId());
                    }
                    confirmDeleteThreads(threadIds, mQueryHandler);
                } else {
                    // The invalid threadId of -1 means all threads here.
                    confirmDeleteThread(-1L, mQueryHandler);
                }
                break;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingListActivity.class);
                startActivityIfNeeded(intent, -1);
                break;
            /// M: Code analyze 011, add omacp to option menu . @{
            case R.id.action_omacp:
                Intent omacpintent = new Intent();
                omacpintent.setClassName("com.mediatek.omacp", "com.mediatek.omacp.message.OmacpMessageList");
                omacpintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityIfNeeded(omacpintent, -1);
                break;
            /// @}
            case R.id.action_wappush:
                Intent wpIntent = new Intent(this, WPMessageActivity.class);
                startActivity(wpIntent);
                break;
            case R.id.action_debug_dump:
                LogTag.dumpInternalTables(this);
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
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Note: don't read the thread id data from the ConversationListItem view passed in.
        // It's unreliable to read the cached data stored in the view because the ListItem
        // can be recycled, and the same view could be assigned to a different position
        // if you click the list item fast enough. Instead, get the cursor at the position
        // clicked and load the data from the cursor.
        // (ConversationListAdapter extends CursorAdapter, so getItemAtPosition() should
        // return the cursor object, which is moved to the position passed in)
        Cursor cursor  = (Cursor) getListView().getItemAtPosition(position);
        /// M: Code analyze 015, For bug,  add cursor == null check . @{
        if (cursor == null) {
            return;
        }
        /// @}
        MmsLog.d(TAG, "onListItemClick: pos=" + position);
        Conversation conv = Conversation.from(this, cursor);
        /// M: Code analyze 005, For new feature ALPS00247476, handle click item with ActionMode . @{
        if (mActionMode != null) {
            boolean checked = conv.isChecked();            
            mActionModeListener.setItemChecked(position, !checked, null);
            mActionModeListener.updateActionMode();
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            return;
        }
        /// @}
        long tid = conv.getThreadId();

        if (LogTag.VERBOSE) {
            Log.d(TAG, "onListItemClick: pos=" + position + ", view=" + v + ", tid=" + tid);
        }
        /// M: Code analyze 001, For new feature ALPS00131956, wappush: modify
        /// the calling of openThread, add one parameter. @{
        MmsLog.i(WP_TAG, "ConversationList: " + "conv.getType() is : " + conv.getType());
        /// M: add for ipmessage
        if (MmsConfig.getIpMessagServiceId(this) > IpMessageServiceId.NO_SERVICE) {
            // this is a guide thread.
            if (conv.getType() == Threads.IP_MESSAGE_GUIDE_THREAD) {
                Intent it = new Intent(RemoteActivities.SERVICE_CENTER);
                IpMessageUtils.startRemoteActivity(this, it);
                conv.markAsRead();
                return;
            }
            /// M: add for ipmessage, handle group thread
            ContactList list = conv.getRecipients();
            String number = null;
            if (list == null || list.size() < 1) {
                // there is no recipients!
                MmsLog.d(TAG, "a thread with no recipients, threadId:" + tid);
                number = "";
            } else {
                number = conv.getRecipients().get(0).getNumber();
            }
            MmsLog.i(IPMSG_TAG, "open thread by number " + number);
            if (conv.getRecipients().size() == 1 && number.startsWith(IpMessageConsts.GROUP_START)) {
                MmsLog.i(IPMSG_TAG, "open group thread by thread id " + tid);
                conv.markAsSeen();
                if (sConversationListOption == OPTION_CONVERSATION_LIST_IMPORTANT) {
                    openIpMsgThread(tid, true);
                } else {
                    openIpMsgThread(tid, false);
                }
                return;
            }
            /// M: add for ipmessage, handle important thread
            if (sConversationListOption == OPTION_CONVERSATION_LIST_IMPORTANT) {
                MmsLog.i(IPMSG_TAG, "open important thread by thread id " + tid);
                if (conv.getType() != Threads.WAPPUSH_THREAD && conv.getType() != Threads.CELL_BROADCAST_THREAD) {
                    Intent intent = ComposeMessageActivity.createIntent(this, tid);
                    intent.putExtra("load_important", true);
                    startActivity(intent);
                }
                return;
            }
        }
        openThread(tid, conv.getType());
        /// @}
    }

    private void createNewMessage() {
        startActivity(ComposeMessageActivity.createIntent(this, 0));
    }

    /// M: Code analyze 001, For new feature ALPS00131956, the method is extended. @{
    private void openThread(long threadId, int type) {
        switch (type) {
        case EncapsulatedTelephony.Threads.WAPPUSH_THREAD:
                startActivity(WPMessageActivity.createIntent(this, threadId));            
            break;
        case EncapsulatedTelephony.Threads.CELL_BROADCAST_THREAD:
                startActivity(CBMessageListActivity.createIntent(this, threadId));                
            break;
        default:
                startActivity(ComposeMessageActivity.createIntent(this, threadId));
            break;
        }
    }
    /// @}

    public static Intent createAddContactIntent(String address) {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        return intent;
    }

    private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener =
        new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            Cursor cursor = mListAdapter.getCursor();
            if (cursor == null || cursor.getPosition() < 0) {
                return;
            }
            Conversation conv = Conversation.from(ConversationList.this, cursor);
            /// M: Code analyze 001, For new feature ALPS00131956, wappush: get
            /// the added mType value. @{
            mType = conv.getType();
            MmsLog.i(WP_TAG, "ConversationList: " + "mType is : " + mType);   
            /// @}

            ContactList recipients = conv.getRecipients();
            menu.setHeaderTitle(recipients.formatNames(","));

            AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.add(0, MENU_VIEW, 0, R.string.menu_view);

            // Only show if there's a single recipient
            if (recipients.size() == 1) {
                // do we have this recipient in contacts?
                if (recipients.get(0).existsInDatabase()) {
                    menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact);
                } else {
                    menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts);
                }
            }
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Cursor cursor = mListAdapter.getCursor();
        if (cursor != null && cursor.getPosition() >= 0) {
            Conversation conv = Conversation.from(ConversationList.this, cursor);
            long threadId = conv.getThreadId();
            switch (item.getItemId()) {
            case MENU_DELETE: {
                confirmDeleteThread(threadId, mQueryHandler);
                break;
            }
            case MENU_VIEW: {
                /// M: Code analyze 001, For new feature ALPS00131956,
                /// wappush: method is changed. @{
                openThread(threadId, mType);
                /// @}
                break;
            }
            case MENU_VIEW_CONTACT: {
                Contact contact = conv.getRecipients().get(0);
                Intent intent = new Intent(Intent.ACTION_VIEW, contact.getUri());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                break;
            }
            case MENU_ADD_TO_CONTACTS: {
                String address = conv.getRecipients().get(0).getNumber();
                startActivity(createAddContactIntent(address));
                break;
            }
            default:
                break;
            }
        }
        return super.onContextItemSelected(item);
    }

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
        if (DEBUG) Log.v(TAG, "onConfigurationChanged: " + newConfig);
    }

    /**
     * Start the process of putting up a dialog to confirm deleting a thread,
     * but first start a background query to see if any of the threads or thread
     * contain locked messages so we'll know how detailed of a UI to display.
     * @param threadId id of the thread to delete or -1 for all threads
     * @param handler query handler to do the background locked query
     */
    public static void confirmDeleteThread(long threadId, AsyncQueryHandler handler) {
        ArrayList<Long> threadIds = null;
        if (threadId != -1) {
            threadIds = new ArrayList<Long>();
            threadIds.add(threadId);
        }
        confirmDeleteThreads(threadIds, handler);
    }

    /**
     * Start the process of putting up a dialog to confirm deleting threads,
     * but first start a background query to see if any of the threads
     * contain locked messages so we'll know how detailed of a UI to display.
     * @param threadIds list of threadIds to delete or null for all threads
     * @param handler query handler to do the background locked query
     */
    public static void confirmDeleteThreads(Collection<Long> threadIds, AsyncQueryHandler handler) {
        Conversation.startQueryHaveLockedMessages(handler, threadIds,
                HAVE_LOCKED_MESSAGES_TOKEN);
    }

    /**
     * Build and show the proper delete thread dialog. The UI is slightly different
     * depending on whether there are locked messages in the thread(s) and whether we're
     * deleting single/multiple threads or all threads.
     * @param listener gets called when the delete button is pressed
     * @param threadIds the thread IDs to be deleted (pass null for all threads)
     * @param hasLockedMessages whether the thread(s) contain locked messages
     * @param context used to load the various UI elements
     */
    public static void confirmDeleteThreadDialog(final DeleteThreadListener listener,
            Collection<Long> threadIds,
            boolean hasLockedMessages,
            Context context) {
        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView)contents.findViewById(R.id.message);

        if (threadIds == null) {
            msg.setText(R.string.confirm_delete_all_conversations);
        } else {
            // Show the number of threads getting deleted in the confirmation dialog.
            int cnt = threadIds.size();
            msg.setText(context.getResources().getQuantityString(
                R.plurals.confirm_delete_conversation, cnt, cnt));
        }

        final CheckBox checkbox = (CheckBox)contents.findViewById(R.id.delete_locked);
        if (!hasLockedMessages) {
            checkbox.setVisibility(View.GONE);
        } else {
            /// M: change the string to important if ipmessage plugin exist
            MmsLog.d(TAG, "serviceId:" + MmsConfig.getIpMessagServiceId(context));
            if (MmsConfig.isActivated(context)) {
                checkbox.setText(IpMessageUtils.getResourceManager(context)
                    .getSingleString(IpMessageConsts.string.ipmsg_delete_important));
            }
            listener.setDeleteLockedMessage(checkbox.isChecked());
            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.setDeleteLockedMessage(checkbox.isChecked());
                }
            });
        }
        /// M: Code analyze 023, For bug ALPS00268161, when delete one MMS, one sms will not be deleted . @{
        Cursor cursor = null;
        int smsId = 0;
        int mmsId = 0;
        cursor = context.getContentResolver().query(Sms.CONTENT_URI,
                new String[] {"max(_id)"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                smsId = cursor.getInt(0);
                MmsLog.d(TAG, "confirmDeleteThreadDialog max SMS id = " + smsId);
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
                MmsLog.d(TAG, "confirmDeleteThreadDialog max MMS id = " + mmsId);
                }
            } finally {
                cursor.close();
                cursor = null;
            }
        }
        listener.setMaxMsgId(mmsId, smsId);
        /// @}

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.confirm_dialog_title)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setCancelable(true)
            .setPositiveButton(R.string.delete, listener)
            .setNegativeButton(R.string.no, null)
            .setView(contents)
            .show();
    }

    private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL: {
                        long id = getListView().getSelectedItemId();
                        if (id > 0) {
                            confirmDeleteThread(id, mQueryHandler);
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    };

    public static class DeleteThreadListener implements OnClickListener {
        private final Collection<Long> mThreadIds;
        private final AsyncQueryHandler mHandler;
        private final Context mContext;
        private boolean mDeleteLockedMessages;
        /// M: Code analyze 023, For bug ALPS00268161, when delete one MMS, one
        /// sms will not be deleted. . @{
        private int mMaxMmsId;
        private int mMaxSmsId;
        /// M:
        private ActionMode mMode;
        public static int sDeleteNumber;

        public void setMaxMsgId(int mmsId, int smsId) {
            mMaxMmsId = mmsId;
            mMaxSmsId = smsId;
        }
        /// @}

        public DeleteThreadListener(Collection<Long> threadIds, AsyncQueryHandler handler,
                Context context) {
            mThreadIds = threadIds;
            mHandler = handler;
            mContext = context;
            sDeleteNumber = 0;
        }

        public DeleteThreadListener(Collection<Long> threadIds, AsyncQueryHandler handler,
                Context context, ActionMode mode) {
            mThreadIds = threadIds;
            mHandler = handler;
            mContext = context;
            mMode = mode;
            if (threadIds != null) {
                sDeleteNumber = threadIds.size();
            }
        }

        
        public void setDeleteLockedMessage(boolean deleteLockedMessages) {
            mDeleteLockedMessages = deleteLockedMessages;
        }

        @Override
        public void onClick(DialogInterface dialog, final int whichButton) {
            MessageUtils.handleReadReport(mContext, mThreadIds,
                    PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ, new Runnable() {
                @Override
                public void run() {
                    /// M:
                    if (mMode != null) {
                        mMode.finish();
                        mMode = null;
                    }
                    /// M: Code analyze 013, For bug ALPS00046358 , The method about the
                    /// handler with progress dialog functio . @{
                    showProgressDialog();
                    /// @}

                    /// M: delete ipmessage in ipmessage db
                    IpMessageUtils.deleteIpMessage(mContext, mThreadIds, mDeleteLockedMessages, mMaxSmsId);

                    int token = DELETE_CONVERSATION_TOKEN;
                    if (mThreadIds == null) {
                        /// M: Code analyze 023, For bug ALPS00268161, when delete one
                        /// MMS, one sms will not be deleted. . @{
                        Conversation.startDeleteAll(mHandler, token, mDeleteLockedMessages, mMaxMmsId, mMaxSmsId);
                        /// @}
                        DraftCache.getInstance().refresh();
                        /// M:
                        sDeleteNumber = 0;
                    } else {
                        /// M: Code analyze 006, For bug ALPS00291435, solve no response
                        /// while deleting 50 messages . @{
                        sDeleteCounter = 0;
                        /// @}
                        /// M: wappush: do not need modify the code here, but delete function in provider has been modified.
                        /// M: fix bug ALPS00415754, add some useful log
                        MmsLog.d(TAG, "before delete threads in conversationList");
                        for (long threadId : mThreadIds) {
                            /// M: Code analyze 006, For bug ALPS00291435,
                            /// solve no response while deleting 50 messages . @{
                            sDeleteCounter++;
                            /// @}
                            /// M: Code analyze 023, For bug ALPS00268161, when delete one
                            /// MMS, one sms will not be deleted . @{
                            Conversation.startDelete(mHandler, token, mDeleteLockedMessages,
                                    threadId, mMaxMmsId, mMaxSmsId);
                            /// @}
                            DraftCache.getInstance().setDraftState(threadId, false);
                        }
                        MmsLog.d(TAG, "after delete threads in conversationList");
                        /// M: Code analyze 006, For bug ALPS00291435, solve no response
                        /// while deleting 50 messages . @{
                        MmsLog.d(TAG, "sDeleteCounter = " + sDeleteCounter);
                        /// @}
                    }
                }
                /// M: Code analyze 013, For bug ALPS00046358 , The method about the handler
                /// with progress dialog functio . @{
                private void showProgressDialog() {
                    if (mHandler instanceof BaseProgressQueryHandler) {
                        ((BaseProgressQueryHandler) mHandler).setProgressDialog(
                                DeleteProgressDialogUtil.getProgressDialog(mContext));
                        ((BaseProgressQueryHandler) mHandler).showProgressDialog();
                    }
                }
                /// @}
            });
            dialog.dismiss();
        }
    }
    

    private final Runnable mDeleteObsoleteThreadsRunnable = new Runnable() {
        @Override
        public void run() {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                LogTag.debug("mDeleteObsoleteThreadsRunnable getSavingDraft(): " +
                        DraftCache.getInstance().getSavingDraft());
            }
            if (DraftCache.getInstance().getSavingDraft()) {
                // We're still saving a draft. Try again in a second. We don't want to delete
                // any threads out from under the draft.
                mHandler.postDelayed(mDeleteObsoleteThreadsRunnable, 1000);
            } else {
                /// M: Code analyze 024, For bug ALPS00234739 , draft can't be
                /// saved after share the edited picture to the same ricipient, so
                ///Remove old Mms draft in conversation list instead of compose view . @{
                MessageUtils.asyncDeleteOldMms();
                /// @}
                Conversation.asyncDeleteObsoleteThreads(mQueryHandler,
                        DELETE_OBSOLETE_THREADS_TOKEN);
            }
        }
    };

    private final class ThreadListQueryHandler extends BaseProgressQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        // Test code used for various scenarios where its desirable to insert a delay in
        // responding to query complete. To use, uncomment out the block below and then
        // comment out the @Override and onQueryComplete line.
//        @Override
//        protected void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
//            mHandler.postDelayed(new Runnable() {
//                public void run() {
//                    myonQueryComplete(token, cookie, cursor);
//                    }
//            }, 2000);
//        }
//
//        protected void myonQueryComplete(int token, Object cookie, Cursor cursor) {

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
              /// M: Code analyze 015, For bug,  add cursor == null check . @{
              MmsLog.d(TAG, "onQueryComplete mNeedQuery = " + mNeedQuery +
                            " mIsInActivity = " + mIsInActivity);
              if (cursor == null) {
              /// M: Decrease query counter and do next query if any request  @{
                  setProgressBarIndeterminateVisibility(false);
                  if (mNeedQuery && mIsInActivity) {
                      MmsLog.d(TAG, "onQueryComplete cursor == null startAsyncQuery");
                      startAsyncQuery();
                  }
                  return;
              /// @}
              }
            /// @}
            switch (token) {
            case THREAD_LIST_QUERY_TOKEN:
                /// M: If no listener for content change, means no need to refresh list @{
                if (mListAdapter.getOnContentChangedListener() == null) {
                    cursor.close();
                    return;
                }
                /// @}
                /// M: add for ipmessage, update Empty View
                updateEmptyView(cursor);
                mListAdapter.changeCursor(cursor);
                /// M: make a timer to update the list later, the time should update.
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mListAdapter.notifyDataSetChanged();
                    }
                }, 60000);

                if (!MmsConfig.isActivated(ConversationList.this)) {
                    if (mListAdapter.getCount() == 0) {
                        ((TextView) (getListView().getEmptyView())).setText(R.string.no_conversations);
                    }
                }
                /** M: add code @{ */
                if (!Conversation.isInitialized()) {
                    Conversation.init(getApplicationContext());
                } else {
                    Conversation.removeInvalidCache(cursor);
                }
                /** @} */

                if (mNeedToMarkAsSeen) {
                    mNeedToMarkAsSeen = false;
                /// M: Code analyze 016, For new feature, wappush: method is changed . @{
                    Conversation.markAllConversationsAsSeen(getApplicationContext(),
                            Conversation.MARK_ALL_MESSAGE_AS_SEEN);

                    // Delete any obsolete threads. Obsolete threads are threads that aren't
                    // referenced by at least one message in the pdu or sms tables. We only call
                    // this on the first query (because of mNeedToMarkAsSeen).
                    mHandler.post(mDeleteObsoleteThreadsRunnable);
                }

                /// M: Code analyze 005, For new feature ALPS00247476 . @{
                if (mActionMode != null) {
                    mActionModeListener.confirmSyncCheckedPositons();
                }
                /// @}
                /// M: Fix bug ALPS00416081
                setDeleteMenuVisible(mOptionsMenu);

                /// Google JB MR1.1 patch. conversation list can restore scroll position
                if (mSavedFirstVisiblePosition != AdapterView.INVALID_POSITION) {
                    // Restore the list to its previous position.
                    getListView().setSelectionFromTop(mSavedFirstVisiblePosition,
                            mSavedFirstItemOffset);
                    mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
                }
                break;

            case UNREAD_THREADS_QUERY_TOKEN:
                int count = 0;
                if (cursor != null) {
                    count = cursor.getCount();
                    cursor.close();
                }
                /// M: modified for unread count display
                if (count > MAX_DISPLAY_UNREAD_COUNT) {
                    mUnreadConvCount.setText(DISPLAY_UNREAD_COUNT_CONTENT_FOR_ABOVE_99);
                } else {
                    mUnreadConvCount.setText(count > 0 ? Integer.toString(count) : null);
                }
                /// M: Code analyze 017, For new feature,  unReadConvCount's theme manager. @{
                if (EncapsulatedFeatureOption.MTK_THEMEMANAGER_APP) {
                    // Resources res = getResources();
                    EncapsulatedResources res = new EncapsulatedResources(getResources());
                    int textColor = res.getThemeMainColor();
                    if (textColor != 0) {
                        mUnreadConvCount.setTextColor(textColor);
                    }
                }
                /// @}
                break;

            case HAVE_LOCKED_MESSAGES_TOKEN:
                /// M: add a log
                MmsLog.d(TAG, "onQueryComplete HAVE_LOCKED_MESSAGES_TOKEN");
                @SuppressWarnings("unchecked")
                Collection<Long> threadIds = (Collection<Long>)cookie;
                ListView listView = getListView();
                ConversationListAdapter adapter = (ConversationListAdapter) listView.getAdapter();
                if (adapter != null && threadIds != null) {
                    Cursor c = adapter.getCursor();
                    /// M: ip message don't delete all threads, always delete selected.
                    if (c != null && c.getCount() == threadIds.size() && !MmsConfig.isActivated(ConversationList.this)) {
                        threadIds = null;
                    }
                }
                confirmDeleteThreadDialog(new DeleteThreadListener(threadIds, mQueryHandler,
                        ConversationList.this, mActionMode), threadIds,
                        cursor != null && cursor.getCount() > 0,
                        ConversationList.this);
                if (cursor != null) {
                    cursor.close();
                }
                break;

            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }

            /// M: Do next query if any requested @{
            if (mNeedQuery && mIsInActivity) {
                startAsyncQuery();
            }
            /// @}
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            /// M: comment it
            //super.onDeleteComplete(token, cookie, result);
            switch (token) {
            case DELETE_CONVERSATION_TOKEN:
                    /// M: Code analyze 006, For bug ALPS00291435, solve no
                    /// response while deleting 50 messages . @{
                if (sDeleteCounter > 1) {
                    sDeleteCounter--;
                    MmsLog.d(TAG, "igonre a onDeleteComplete,sDeleteCounter:" + sDeleteCounter);
                    return;
                }
                sDeleteCounter = 0;
                    /// @}
                long threadId = cookie != null ? (Long)cookie : -1;     // default to all threads

                if (threadId == -1) {
                    // Rebuild the contacts cache now that all threads and their associated unique
                    // recipients have been deleted.
                    Contact.init(ConversationList.this);
                } else {
                    // Remove any recipients referenced by this single thread from the
                    // contacts cache. It's possible for two or more threads to reference
                    // the same contact. That's ok if we remove it. We'll recreate that contact
                    // when we init all Conversations below.
                    Conversation conv = Conversation.get(ConversationList.this, threadId, false);
                    if (conv != null) {
                        ContactList recipients = conv.getRecipients();
                        for (Contact contact : recipients) {
                            contact.removeFromCache();
                        }
                    }
                }
                // Make sure the conversation cache reflects the threads in the DB.
                Conversation.init(ConversationList.this);

                /** M: add code @{ */
                try {
                    /** M: MTK Encapsulation ITelephony */
                    // ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
                    if (phone != null) {
                        if (phone.isTestIccCard()) {
                            MmsLog.d(CONV_TAG, "All threads has been deleted, send notification..");
                            EncapsulatedSmsManager.setSmsMemoryStatus(true);
                        }
                    } else {
                        MmsLog.d(CONV_TAG, "Telephony service is not available!");
                    }
                } catch (Exception ex) {
                    MmsLog.e(CONV_TAG, " " + ex.getMessage());
                }
                /** @} */

                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(ConversationList.this,
                        MessagingNotification.THREAD_NONE, false);
                // Update the notification for failed messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateSendFailedNotification(ConversationList.this);
                /// M: update download failed messages since they may be deleted too.
                MessagingNotification.updateDownloadFailedNotification(ConversationList.this);

                /// M: Code analyze 001, For new feature ALPS00131956,
                /// wappush: Update the notification for new WAP Push/CB
                /// messages. @{
                if (EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT) {
                    WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(ConversationList.this,
                                                                                WapPushMessagingNotification.THREAD_NONE);
                }
                /// @}
                    /// M: Code analyze 006, For bug ALPS00291435, solve no
                    /// response while deleting 50 messages . @{
                CBMessagingNotification.updateNewMessageIndicator(ConversationList.this);
                    /// @}
                // Make sure the list reflects the delete
                /// M: comment this line
                // startAsyncQuery();.-
                /** M: fix bug ALPS00357750 @{ */
                dismissProgressDialog();
                /** @} */
                /** M: show a toast
                if (DeleteThreadListener.sDeleteNumber > 0) {
                    int count = DeleteThreadListener.sDeleteNumber;
                    String toastString = ConversationList.this.getResources().getQuantityString(
                            R.plurals.deleted_conversations, count, count);
                    Toast.makeText(ConversationList.this, toastString, Toast.LENGTH_SHORT).show();
                    DeleteThreadListener.sDeleteNumber = 0;
                }
                */
                break;

            case DELETE_OBSOLETE_THREADS_TOKEN:
                // Nothing to do here.
                break;
            }
        }
    }
    /// M: Code analyze 005, For new feature ALPS00247476, replace multichoicemode by longClickListener . @{
    private class ModeCallback implements ActionMode.Callback {
        private View mMultiSelectActionBarView;
        /// M:
        private Button mSelectionTitle;
        //private TextView mSelectedConvCount;
        private HashSet<Long> mSelectedThreadIds;

        /// M: Code analyze 025, For new feature ALPS00114403, to solve ANR
        // happen when multi delete 6 threads . @{
        private HashSet<Integer> mCheckedPosition;

        /// @}
        /// M: Code analyze 005, For new feature ALPS00247476, check numbers . @{
        private int mCheckedNum = 0;
        /// @}
        private MenuItem mDeleteItem;

        private boolean mIsSelectAll = false;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            mSelectedThreadIds = new HashSet<Long>();
            /// M: Code analyze 025, For new feature ALPS00114403, to solve ANR
            // happen when multi delete 6 threads . @{
            mCheckedPosition = new HashSet<Integer>();
            /// @}
            /// M: no ipmessage plugin
            if (MmsConfig.getIpMessagServiceId(ConversationList.this) == 0) {
                inflater.inflate(R.menu.conversation_multi_select_menu_with_selectall, menu);
            } else {
                inflater.inflate(R.menu.conversation_multi_select_menu, menu);
            }
            mDeleteItem = menu.findItem(R.id.delete);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(ConversationList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar2, null);
                /// M: change select tips style
                mSelectionTitle = (Button)mMultiSelectActionBarView.findViewById(R.id.selection_menu);
                //mSelectedConvCount =
                //    (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_conv_count);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            ((Button)mMultiSelectActionBarView.findViewById(R.id.selection_menu))
                .setText(R.string.select_conversations);
            /// M: Code analyze 008, For bug ALPS00250948, disable search in
            // multi-select status . @{
            mDisableSearchFalg = true;
            /// @}
            /// M: Code analyze 005, For new feature ALPS00247476, set long clickable . @{
            getListView().setLongClickable(false);
            /// @}
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup)LayoutInflater.from(ConversationList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar2, null);
                mode.setCustomView(v);
                /// M: change select tips style
                mSelectionTitle = (Button)mMultiSelectActionBarView.findViewById(R.id.selection_menu);
                //mSelectedConvCount = (TextView)v.findViewById(R.id.selected_conv_count);

            }
            /// M: redesign selection action bar and add shortcut in common version. @{
            CustomMenu customMenu = new CustomMenu(ConversationList.this);
            mSelectionMenu = customMenu.addDropDownMenu(mSelectionTitle, R.menu.selection);
            mSelectionMenuItem = mSelectionMenu.findItem(R.id.action_select_all);
            customMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (mListAdapter.isAllSelected()) {
                        setAllItemChecked(mActionMode, false);
                    } else {
                        setAllItemChecked(mActionMode, true);
                    }
                    return false;
                }
            });
            /// @}
            /// M:
            if (sConversationListOption == OPTION_CONVERSATION_LIST_SPAM) {
                MenuItem item = menu.findItem(R.id.mark_as_spam);
                if (item != null) {
                    item.setVisible(false);
                }
                item = menu.findItem(R.id.add_shortcut);
                if (item != null) {
                    item.setVisible(false);
                }
                item = menu.findItem(R.id.mark_as_nonspam);
                if (item != null) {
                    item.setVisible(true);
                }
            } else {
                MenuItem item = menu.findItem(R.id.mark_as_spam);
                if (item != null) {
                    if (!MmsConfig.isActivated(ConversationList.this)) {
                        item.setVisible(false);
                    } else {
                        item.setVisible(true);
                    }
                }
                item = menu.findItem(R.id.add_shortcut);
                if (item != null) {
                    item.setVisible(true);
                }
                item = menu.findItem(R.id.mark_as_nonspam);
                if (item != null) {
                    item.setVisible(false);
                }
            }
            if (!IpMessageUtils.getServiceManager(ConversationList.this).isFeatureSupported(
                IpMessageConsts.FeatureId.SAVE_CHAT_HISTORY)) {
                MenuItem item = menu.findItem(R.id.export);
                if (item != null) {
                    item.setVisible(false);
                }
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    if (mSelectedThreadIds.size() > 0) {
                        Log.v(TAG, "ConversationList->ModeCallback: delete");
                        confirmDeleteThreads(mSelectedThreadIds, mQueryHandler);
                    } else {
                        item.setEnabled(false);
                    }
                    break;

            case R.id.export:
                if (mSelectedThreadIds.size() > 0) {
                    MmsLog.d(TAG, "threadIds:" + mSelectedThreadIds.toString());
                    // TODO ipmessage, show dialog for download to SD card or share via Email
                    ConversationList.this.showExportDialog(mSelectedThreadIds, mode);
                } else {
                    mHandler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(ConversationList.this, R.string.no_item_selected, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;

            case R.id.add_shortcut:
                MmsLog.d(TAG, "click shortcut!");
                if (mSelectedThreadIds.size() > 0) {
                    MessageUtils.addShortcutToLauncher(ConversationList.this, mSelectedThreadIds);
                }
                mode.finish();
                break;

            case R.id.mark_as_spam:
                MmsLog.d(TAG, "click mark as spam!");
                final HashSet<Long> threadIds2 = (HashSet<Long>)mSelectedThreadIds.clone();
                OnClickListener listener = new OnClickListener() {
                    public void onClick(DialogInterface dialog, final int whichButton) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int[] contactIds = new int[threadIds2.size()];
                                int i = 0;
                                for (Long threadId : threadIds2) {
                                    Conversation conversationForMarkSpam = Conversation.get(
                                        ConversationList.this, threadId.longValue(), false);
                                    String numbers = TextUtils.join(",",
                                                        conversationForMarkSpam.getRecipients().getNumbers());
                                    int contactId = IpMessageUtils.getContactManager(ConversationList.this)
                                        .getContactIdByNumber(numbers);
                                    contactIds[i] = contactId;
                                    i++;
                                    MmsLog.d(TAG, "threadId:" + threadId + ", contactId:" + contactId);
                                }
                                IpMessageUtils.getContactManager(ConversationList.this).addContactToSpamList(contactIds);
                            }
                        }).start();
                        mode.finish();
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(ConversationList.this);
                builder.setTitle(R.string.mark_as_spam)
                    .setCancelable(true)
                    .setPositiveButton(
                            IpMessageUtils.getResourceManager(ConversationList.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_continue),
                            listener)
                    .setNegativeButton(R.string.Cancel, null)
                    .setMessage(IpMessageUtils.getResourceManager(ConversationList.this)
                        .getSingleString(IpMessageConsts.string.ipmsg_mark_as_spam_tips))
                    .show();
                break;
            case R.id.mark_as_nonspam:
                MmsLog.d(TAG, "click mark as nonspam!");
                final HashSet<Long> threadIds = mSelectedThreadIds;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int[] contactIds = new int[threadIds.size()];
                        int i = 0;
                        for (Long threadId : threadIds) {
                            Conversation conversationForMarkSpam = Conversation.get(
                                ConversationList.this, threadId.longValue(), false);
                            String numbers = TextUtils.join(",", conversationForMarkSpam.getRecipients().getNumbers());
                            int contactId = IpMessageUtils.getContactManager(ConversationList.this)
                                .getContactIdByNumber(numbers);
                            contactIds[i] = contactId;
                            i++;
                            MmsLog.d(TAG, "threadId:" + threadId + ", contactId:" + contactId);
                        }
                        IpMessageUtils.getContactManager(ConversationList.this).deleteContactFromSpamList(contactIds);
                    }
                }).start();
                mode.finish();
                break;
                default:
                    /// M: Code analyze 025, For new feature ALPS00114403, to
                    /// solve ANR happen when multi delete 6 threads . @{
                    if (mCheckedPosition != null && mCheckedPosition.size() > 0) {
                        mCheckedPosition.clear();
                    }
                    /// @}
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ConversationListAdapter adapter = (ConversationListAdapter)getListView().getAdapter();
            //adapter.uncheckAll();
            /// M: Code analyze 025, For new feature ALPS00114403, to solve ANR
            /// happen when multi delete 6 threads . @{
            adapter.uncheckSelect(mCheckedPosition);
            mCheckedPosition = null;
            /// @}
            mSelectedThreadIds = null;
            /// M: Code analyze 008, For bug ALPS00250948, disable search in multi-select status . @{
            mDisableSearchFalg = false;
            /// @}

            /// M: Code analyze 005, For new feature ALPS00247476, add selectAll/unSelectAll . @{
            getListView().setLongClickable(true);
            mCheckedNum = 0;
            mActionMode = null;
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            /// @}
        }

        public void setItemChecked(int position, boolean checked, Cursor cursor) {
            ListView listView = getListView();
            if (cursor == null) {
                cursor = (Cursor)listView.getItemAtPosition(position);
            } else {
                cursor.moveToPosition(position);
            }
            /// M: Code analyze 005, For new feature ALPS00247476, select multi message in . @{
            Conversation conv = Conversation.getFromCursor(ConversationList.this, cursor);
            if (checked == conv.isChecked()) {
                return;
            }
            /// @}
            conv.setIsChecked(checked);
            if (mSelectedThreadIds == null || mCheckedPosition == null) {
                return;
            }
            long threadId = conv.getThreadId();
            if (checked) {
                mSelectedThreadIds.add(threadId);
                /// M: Code analyze 005, For new feature ALPS00247476, select multi message in . @{
                mCheckedPosition.add(position);
                mCheckedNum ++;
               /// @}
            } else {
                mSelectedThreadIds.remove(threadId);
                /// M: Code analyze 005, For new feature ALPS00247476, select multi message in . @{
                mCheckedPosition.remove(position);
                mCheckedNum --;
               /// @}
            }
        }
        /// @}

        private void updateActionMode() {
            /// M: Code analyze 018, For bug, enable or diable mDeleteItem menu . @{
            if (mDeleteItem != null) {
                if (mCheckedNum > 0) {
                    mDeleteItem.setEnabled(true);
                } else {
                    mDeleteItem.setEnabled(false);
                }
                /// @}
            }
            /// M: exit select mode if no item select
            if (mCheckedNum == 0 && mActionMode != null) {
                mActionMode.finish();
            }
            mSelectionTitle.setText(ConversationList.this.getResources().getQuantityString(
                    R.plurals.message_view_selected_message_count, mCheckedNum, mCheckedNum));
            updateSelectionTitle();
        }

        /// M: Code analyze 005, For new feature ALPS00247476, select all messages . @{
        private void setAllItemChecked(ActionMode mode, final boolean checked) {
            mIsSelectAll = true;
            new AsyncDialog(ConversationList.this).runAsync(new Runnable() {

                @Override
                public void run() {
                    /// M: query a personal cursor for select all operation. @{
                    Cursor cursor = null;
                    String selection = null;
                    /// M: ipmessage query.
                    if (MmsConfig.isActivated(ConversationList.this)) {
                        switch (sConversationListOption) {
                        case OPTION_CONVERSATION_LIST_ALL:
                            MmsLog.d(TAG, "setAllItemChecked(): query for all messages except spam");
                            selection = "threads._id not in (SELECT DISTINCT "
                                        + Sms.THREAD_ID
                                        + " FROM thread_settings WHERE spam=1) ";
                            cursor = getContext().getContentResolver().query(
                                Conversation.sAllThreadsUriExtend,
                                Conversation.ALL_THREADS_PROJECTION_EXTEND, selection, null, Conversations.DEFAULT_SORT_ORDER);
                            break;
                        case OPTION_CONVERSATION_LIST_IMPORTANT:
                            selection = "threads._id IN (SELECT " + Sms.THREAD_ID + " FROM sms WHERE " + Sms.LOCKED + "=1" +
                                        " UNION SELECT " + Mms.THREAD_ID + " FROM pdu WHERE " + Mms.LOCKED + "=1)";
                            MmsLog.d(TAG, "setAllItemChecked(): query for important messages, selection = " + selection);
                            cursor = getContext().getContentResolver().query(
                                Conversation.sAllThreadsUriExtend,
                                Conversation.ALL_THREADS_PROJECTION_EXTEND, selection, null, Conversations.DEFAULT_SORT_ORDER);
                            break;
                        case OPTION_CONVERSATION_LIST_GROUP_CHATS:
                            MmsLog.d(TAG, "setAllItemChecked(): query for group messages");
                            selection = "threads._id IN (SELECT DISTINCT " + Sms.THREAD_ID
                                    + " FROM thread_settings WHERE spam=0)"
                                    + " AND threads.recipient_ids IN (SELECT _id FROM canonical_addresses" + " WHERE "
                                    + "SUBSTR(address, 1, 4) = '" + IpMessageConsts.GROUP_START + "'" + ")";
                            cursor = getContext().getContentResolver().query(
                                Conversation.sAllThreadsUriExtend,
                                Conversation.ALL_THREADS_PROJECTION_EXTEND, selection, null, Conversations.DEFAULT_SORT_ORDER);
                            break;
                        case OPTION_CONVERSATION_LIST_SPAM:
                            //selection = Threads.SPAM + "=1 OR _ID in (SELECT DISTINCT " + Sms.THREAD_ID + " FROM sms WHERE "
                            //        + Sms.SPAM + "=1) ";
                            selection = "threads._id IN (SELECT DISTINCT " + Sms.THREAD_ID + " FROM thread_settings WHERE spam=1) ";
                            MmsLog.d(TAG, "setAllItemChecked(): query for spam messages, selection = " + selection);
                            cursor = getContext().getContentResolver().query(
                                Conversation.sAllThreadsUriExtend,
                                Conversation.ALL_THREADS_PROJECTION_EXTEND, selection, null, Conversations.DEFAULT_SORT_ORDER);
                            break;
                        default:
                            MmsLog.d(TAG, "status error! not at any type.");
                            break;
                        }
                    } else {
                        cursor = getContext().getContentResolver().query(
                            Conversation.sAllThreadsUriExtend,
                            Conversation.ALL_THREADS_PROJECTION_EXTEND, null, null, Conversations.DEFAULT_SORT_ORDER);
                    }
                    try {
                        if (cursor != null) {
                            for (int position = 0; position < cursor.getCount(); position++) {
                                setItemChecked(position, checked, cursor);
                            }
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    mIsSelectAll = false;
                    /// @}
                }
            }, new Runnable() {

                @Override
                public void run() {
                    updateActionMode();
                    // / M: Code analyze 018, For bug, enable or diable
                    // mDeleteItem menu . @{
                    if (checked) {
                        mDeleteItem.setEnabled(true);
                    } else {
                        mDeleteItem.setEnabled(false);
                    }
                    // / @}

                    if (mListAdapter != null) {
                        mListAdapter.notifyDataSetChanged();
                    }
                }
            }, R.string.sync_mms_to_db);

        }
        /// @}

        /// M: Code analyze 005, For new feature ALPS00247476, after adater's cursor
        /// changed, must sync witch mCheckedPosition for one Scenario: a new message
        /// with a new thread id comes when user are selecting items . @{
        public void confirmSyncCheckedPositons() {
            /// M: while doing select all, don't sync conversations. @{
            if (mIsSelectAll) {
                return;
            }
            /// @}
            mCheckedPosition.clear();
            mSelectedThreadIds.clear();
            ListView listView = getListView();
            ConversationListAdapter adapter = (ConversationListAdapter)listView.getAdapter();
            int num = adapter.getCount();
            for (int position = 0; position < num; position++) {
                Cursor cursor = (Cursor)listView.getItemAtPosition(position);
                Conversation conv = Conversation.getFromCursor(ConversationList.this, cursor);
                if (conv.isChecked()) {
                   mCheckedPosition.add(position);
                   mSelectedThreadIds.add(conv.getThreadId());
                }
            }
            mCheckedNum = mCheckedPosition.size();
            /// M:
            mSelectionTitle.setText(ConversationList.this.getResources().getQuantityString(
                    R.plurals.message_view_selected_message_count, mCheckedNum, mCheckedNum));
            //mSelectedConvCount.setText(Integer.toString(mCheckedNum));
            updateSelectionTitle();
        }
        /// @}
    }

    private void log(String format, Object... args) {
        String s = String.format(format, args);
        Log.d(TAG, "[" + Thread.currentThread().getId() + "] " + s);
    }

    private boolean mIsShowSIMIndicator = true;
    /// M: Code analyze 003, For new feature ALPS00242732, SIM indicator UI is not good . @{
    @Override
    public void onSimInforChanged() {
        MmsLog.i(MmsApp.LOG_TAG, "onSimInforChanged(): Conversation List");
        /// M: show SMS indicator
        if (!isFinishing() && mIsShowSIMIndicator) {
            MmsLog.i(MmsApp.LOG_TAG, "Hide current indicator and show new one.");
            mStatusBarManager.hideSIMIndicator(getComponentName());
            mStatusBarManager.showSIMIndicator(getComponentName(), EncapsulatedSettings.System.SMS_SIM_SETTING);
        }
    }
    /// @}

    /// M: Code analyze 009, For bug ALPS00270910, Default SIM card icon shown
    /// in status bar is incorrect, need to get current sim information . @{
    public static Activity getContext() {
        return sActivity;
    }
    /// @}

    /// M: Code analyze 005, For new feature ALPS00247476, long click Listenner . @{
    class ItemLongClickListener implements  ListView.OnItemLongClickListener {

        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            mActionMode = startActionMode(mActionModeListener);
            Log.e(TAG, "OnItemLongClickListener");
            mActionModeListener.setItemChecked(position, true, null);
            mActionModeListener.updateActionMode();
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            return true;
        }
    }
    /// @}

    @Override
    protected void onResume() {
        super.onResume();
        /// M: add for application guide. @{
        IAppGuideExt appGuideExt =
                (IAppGuideExt)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_APPLICATION_GUIDE);
        appGuideExt.showAppGuide("MMS");
        ///@}

        ComposeMessageActivity.mDestroy = true;

        /// M: Code analyze 003, For new feature ALPS00242732, SIM indicator UI is not good . @{
        final ComponentName name = getComponentName();
        mIsShowSIMIndicator = true;
        mStatusBarManager.hideSIMIndicator(name);
        mStatusBarManager.showSIMIndicator(name, EncapsulatedSettings.System.SMS_SIM_SETTING);
        /// @}
    }

    /// M: Code analyze 001, For new feature ALPS00242732, SIM indicator UI is not good . @{
    @Override
    protected void onPause() {
        mStatusBarManager.hideSIMIndicator(getComponentName());
        mIsShowSIMIndicator = false;
        super.onPause();

        /// Google JB MR1.1 patch. conversation list can restore scroll position
        // Remember where the list is scrolled to so we can restore the scroll position
        // when we come back to this activity and *after* we complete querying for the
        // conversations.
        ListView listView = getListView();
        mSavedFirstVisiblePosition = listView.getFirstVisiblePosition();
        View firstChild = listView.getChildAt(0);
        mSavedFirstItemOffset = (firstChild == null) ? 0 : firstChild.getTop();
    }
    /// @}

    @Override
    protected void onDestroy() {
        /// M: Remove not start queries, and close the last cursor hold be adapter@{
        MmsLog.d(TAG, "onDestroy");
        if (mQueryHandler != null) {
            mQueryHandler.removeCallbacksAndMessages(null);
            mQueryHandler.cancelOperation(THREAD_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(UNREAD_THREADS_QUERY_TOKEN);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mListAdapter != null) {
            MmsLog.d(TAG, "clear it");
            mListAdapter.changeCursor(null);
        }
        /// @}

        /// M: Code analyze 004, For bug ALPS00247476, ensure the scroll smooth . @{
        mScrollListener.destroyThread();
        /// @}

        /// M: add for ipmessage
        if (mNetworkStateReceiver != null) {
            unregisterReceiver(mNetworkStateReceiver);
            mNetworkStateReceiver = null;
        }
        if (MmsConfig.getIpMessagServiceId(this) > IpMessageServiceId.NO_SERVICE) {
            IpMessageUtils.removeIpMsgNotificationListeners(this, this);
        }

        if (mActionMode != null) {
            Conversation.clearCache();
        }
        super.onDestroy();
    }

    /// M: Code analyze 008, For bug ALPS00250948, disable search in
    /// multi-select status . @{
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mDisableSearchFalg) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_SEARCH:
                    // do nothing since we don't want search box which may cause UI crash
                    // TODO: mapping to other useful feature
                    return true;
                default:
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
      /// @}

    /// M: Code analyze 020, For bug ALPS00050455, enhance the performance of
    /// launch time . @{
    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            /** M: Instead of stopping, simply push this to the back of the stack.
             * This is only done when running at the top of the stack;
             * otherwise, we have been launched by someone else so need to
             * allow the user to go back to the caller.
             */
            moveTaskToBack(false);
        } else {
            super.onBackPressed();
        }
    }

    /// @}

    /// M: Code analyze 009, For new feature, plugin . @{
    private void initPlugin(Context context) {
        try {
            mMmsConversationPlugin =
                    (IMmsConversation)EncapsulatedPluginManager.createPluginObject(context,
                                                                                IMmsConversation.class.getName());
            MmsLog.d(TAG, "operator mMmsConversationPlugin = " + mMmsConversationPlugin);
        } catch (AndroidException e) {
            mMmsConversationPlugin = new MmsConversationImpl(context);
            MmsLog.d(TAG, "default mMmsConversationPlugin = " + mMmsConversationPlugin);
        }

        mMmsConversationPlugin.init(this);
    }

    /// @}

    /// M: Code analyze 013, For bug ALPS00046358 , The base class about the
    /// handler with progress dialog functio . @{
    public abstract static class BaseProgressQueryHandler extends AsyncQueryHandler {
        private NewProgressDialog mDialog;
        private int mProgress;
        
        public BaseProgressQueryHandler(ContentResolver resolver) {
            super(resolver);
        }

        /** M:
         * Sets the progress dialog.
         * @param dialog the progress dialog.
         */
        public void setProgressDialog(NewProgressDialog dialog) {
            // Patch back ALPS00457128 which the "deleting" progress display for a long time
            if (mDialog == null) {
                mDialog = dialog;
            }
        }

        /** M:
         * Sets the max progress.
         * @param max the max progress.
         */
        public void setMax(int max) {
            if (mDialog != null) {
                mDialog.setMax(max);
            }
        }

        /** M:
         * Shows the progress dialog. Must be in UI thread.
         */
        public void showProgressDialog() {
            if (mDialog != null) {
                mDialog.show();
            }
        }

        /** M:
         * Rolls the progress as + 1.
         * @return if progress >= max.
         */
        protected boolean progress() {
            if (mDialog != null) {
                return ++mProgress >= mDialog.getMax();
            } else {
                return false;
            }
        }

        /** M: fix bug ALPS00351620
         * Dismisses the progress dialog.
         */
        protected void dismissProgressDialog() {
            // M: fix bug ALPS00357750
            if (mDialog == null) {
                MmsLog.e(TAG, "mDialog is null!");
                return;
            }

            mDialog.setDismiss(true);
            try {
                mDialog.dismiss();
            } catch (IllegalArgumentException e) {
                // if parent activity is destroyed,and code come here, will happen this.
                // just catch it.
                MmsLog.d(TAG, "ignore IllegalArgumentException");
            }
            mDialog = null;
        }
    }

    /// @}

    /// M: Code analyze 009, For new feature, plugin . @{
    public void showSimSms() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            List<SIMInfo> listSimInfo = SIMInfo.getInsertedSIMList(this);
            if (listSimInfo.size() > 1) {
                Intent simSmsIntent = new Intent();
                simSmsIntent.setClass(this, SelectCardPreferenceActivity.class);
                simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                simSmsIntent.putExtra("preference", MessagingPreferenceActivity.SMS_MANAGE_SIM_MESSAGES);
                simSmsIntent.putExtra("preferenceTitleId", R.string.pref_title_manage_sim_messages);
                startActivity(simSmsIntent);
            } else if (listSimInfo.size() == 1) {
                Intent simSmsIntent = new Intent();
                simSmsIntent.setClass(this, ManageSimMessages.class);
                simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                simSmsIntent.putExtra("SlotId", listSimInfo.get(0).getSlot());
                startActivity(simSmsIntent);
            } else {
                Toast.makeText(ConversationList.this, R.string.no_sim_1, Toast.LENGTH_SHORT).show();
            }
        } else { 
            startActivity(new Intent(this, ManageSimMessages.class));
        }
    }
    
    public void changeMode() {
        MmsConfig.setMmsDirMode(true);
        MessageUtils.updateNotification(this);
        Intent it = new Intent(this, FolderViewList.class);
        it.putExtra("floderview_key", FolderViewList.OPTION_INBOX);// show inbox by default
        startActivity(it);
        finish();
    }
    /// @}

    private void setupActionBar2() {
        ActionBar actionBar = getActionBar();

        ViewGroup v = (ViewGroup)LayoutInflater.from(this)
                .inflate(R.layout.conversation_list_actionbar2, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.LEFT));

        mUnreadConvCount = (TextView) v.findViewById(R.id.unread_conv_count);
        mSpinnerTextView = (TextView) v.findViewById(R.id.conversation_list_name);
        mConversationSpinner = (View) v.findViewById(R.id.conversation_list_spinner);
        if (MmsConfig.isActivated(this)) {
            mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_all));
            mConversationSpinner.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (mDropdownAdapter.getCount() > 0) {
                        mAccountDropdown.show();
                    }
                }
            });
        } else {
            // hide views if no plugin exist
            mSpinnerTextView.setVisibility(View.GONE);
            mConversationSpinner.setVisibility(View.GONE);
        }
    }

    /// M: add for ipmessage: important, spam, group chats {@
    private void initSpinnerListAdapter() {
        mDropdownAdapter = new ArrayAdapter<String>(this, R.layout.conversation_list_title_drop_down_item,
                R.id.drop_down_menu_text, new ArrayList<String>());
        mDropdownAdapter = getDropDownMenuData(mDropdownAdapter, OPTION_CONVERSATION_LIST_ALL);
        setupActionBar2();

        mAccountDropdown = new AccountDropdownPopup(mContext);
        mAccountDropdown.setAdapter(mDropdownAdapter);
   }

    private ArrayAdapter<String> getDropDownMenuData(ArrayAdapter<String> adapter, int dropdownStatus) {
        if (null == adapter) {
            return null;
        }
        mDropdownAdapter.clear();

        Resources res = getResources();
        if (dropdownStatus != OPTION_CONVERSATION_LIST_ALL) {
            adapter.add(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_all));
        }

        if (dropdownStatus != OPTION_CONVERSATION_LIST_IMPORTANT) {
            adapter.add(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_important));
        }

        if (dropdownStatus != OPTION_CONVERSATION_LIST_GROUP_CHATS) {
            adapter.add(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_group_chats));
        }

        if (dropdownStatus != OPTION_CONVERSATION_LIST_SPAM) {
            adapter.add(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_spam));
        }

        return adapter;
    }

    // Based on Spinner.DropdownPopup
    private class AccountDropdownPopup extends ListPopupWindow {
        public AccountDropdownPopup(Context context) {
            super(context);
            setAnchorView(mConversationSpinner);
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
            setWidth(240);
            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            // List view is instantiated in super.show(), so we need to do this after...
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    private void onAccountSpinnerItemClicked(int position) {
        switch (sConversationListOption) {
        case OPTION_CONVERSATION_LIST_ALL:
            position++;
            break;
        case OPTION_CONVERSATION_LIST_IMPORTANT:
            if (position > 0) {
                position++;
            }
            break;
        case OPTION_CONVERSATION_LIST_GROUP_CHATS:
            if (position > 1) {
                position++;
            }
            break;
        case OPTION_CONVERSATION_LIST_SPAM:
            break;
        default:
            break;
        }
        switch (position) {
            case OPTION_CONVERSATION_LIST_ALL:
                sConversationListOption = OPTION_CONVERSATION_LIST_ALL;
                mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_all));
                mDropdownAdapter = getDropDownMenuData(mDropdownAdapter, sConversationListOption);
                mDropdownAdapter.notifyDataSetChanged();
                break;
            case OPTION_CONVERSATION_LIST_IMPORTANT:
                sConversationListOption = OPTION_CONVERSATION_LIST_IMPORTANT;
                mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_important));
                mDropdownAdapter = getDropDownMenuData(mDropdownAdapter, sConversationListOption);
                mDropdownAdapter.notifyDataSetChanged();
                break;
            case OPTION_CONVERSATION_LIST_GROUP_CHATS:
                sConversationListOption = OPTION_CONVERSATION_LIST_GROUP_CHATS;
                mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_group_chats));
                mDropdownAdapter = getDropDownMenuData(mDropdownAdapter, sConversationListOption);
                mDropdownAdapter.notifyDataSetChanged();
                break;
            case OPTION_CONVERSATION_LIST_SPAM:
                sConversationListOption = OPTION_CONVERSATION_LIST_SPAM;
                mSpinnerTextView.setText(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_conversation_list_spam));
                mDropdownAdapter = getDropDownMenuData(mDropdownAdapter, sConversationListOption);
                mDropdownAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
        startAsyncQuery();
        invalidateOptionsMenu();
    }
    /// @}
    private static final int REQUEST_CODE_SELECT_CONTACT_FOR_GROUP = 100;
    private static final int REQUEST_CODE_INVITE = 101;
    private static final String KEY_SELECTION_SIMID = "SIMID";

    /// M: add for ipmessage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MmsLog.d(IPMSG_TAG, "onActivityResult(): requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (resultCode != RESULT_OK) {
            MmsLog.d(IPMSG_TAG, "onActivityResult(): result is not OK.");
            return;
        }
        switch (requestCode) {
        case REQUEST_CODE_SELECT_CONTACT_FOR_GROUP:
            String[] mSelectContactsIds = data.getStringArrayExtra(IpMessageUtils.SELECTION_CONTACT_RESULT);
            if (mSelectContactsIds != null) {
                for (String contactId : mSelectContactsIds) {
                    MmsLog.d(IPMSG_TAG, "onActivityResult(): SELECT_CONTACT get contact id = " + contactId);
                }
                Intent intent = new Intent(RemoteActivities.NEW_GROUP_CHAT);
                intent.putExtra(RemoteActivities.KEY_SIM_ID, data.getIntExtra(KEY_SELECTION_SIMID, 0));
                intent.putExtra(RemoteActivities.KEY_ARRAY, mSelectContactsIds);
                IpMessageUtils.startRemoteActivity(this, intent);
                mSelectContactsIds = null;
            } else {
                MmsLog.d(IPMSG_TAG, "onActivityResult(): SELECT_CONTACT get contact id is NULL!");
            }
            break;
        case REQUEST_CODE_INVITE:
            final String mSelectContactsNumbers = data.getStringExtra(IpMessageUtils.SELECTION_CONTACT_RESULT);

            if (mSelectContactsNumbers != null) {
                MmsLog.d(IPMSG_TAG, "mSelectContactsNumbers:" + mSelectContactsNumbers);
                StringBuilder numberString = new StringBuilder();
                Intent it = new Intent(this, ComposeMessageActivity.class);
                it.setAction(Intent.ACTION_SENDTO);
                Uri uri = Uri.parse("smsto:" + mSelectContactsNumbers);
                it.setData(uri);
                it.putExtra("sms_body",
                            IpMessageUtils.getResourceManager(this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_invite_friends_content));
                startActivity(it);
            } else {
                MmsLog.d(IPMSG_TAG, "onActivityResult(): INVITE get contact id is NULL!");
            }
            break;
        default:
            MmsLog.d(IPMSG_TAG, "onActivityResult(): default return.");
            return;
        }
    }

    private void openIpMsgThread(final long threadId, boolean isImportant) {
        Intent intent = new Intent(RemoteActivities.CHAT_DETAILS_BY_THREAD_ID);
        intent.putExtra(RemoteActivities.KEY_THREAD_ID, threadId);
        intent.putExtra(RemoteActivities.KEY_BOOLEAN, isImportant);
        intent.putExtra(RemoteActivities.KEY_NEED_NEW_TASK, false);
        IpMessageUtils.startRemoteActivity(this, intent);
    }

    @Override
    public void notificationsReceived(Intent intent) {
        MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "convList.notificationsReceived(): start, intent = " + intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (IpMessageUtils.getActionTypeByAction(action)) {
        case IpMessageUtils.IPMSG_ERROR_ACTION:
            // do nothing
            return;
        case IpMessageUtils.IPMSG_NEW_MESSAGE_ACTION:
//            public static final String IP_MESSAGE_KEY = "IpMessageKey";
            break;
        case IpMessageUtils.IPMSG_REFRESH_CONTACT_LIST_ACTION:
            break;
        case IpMessageUtils.IPMSG_REFRESH_GROUP_LIST_ACTION:
            break;
        case IpMessageUtils.IPMSG_SERCIVE_STATUS_ACTION:
//            public static final int ON  = 1;
//            public static final int OFF = 0;
            break;
        case IpMessageUtils.IPMSG_IM_STATUS_ACTION:
            /** M: show typing feature is off for performance issue now.
            String number = intent.getStringExtra(IpMessageConsts.NUMBER);
            int status = IpMessageUtils.getContactManager(this).getStatusByNumber(number);
            MmsLog.d(TAG, "notificationsReceived(): IM status. number = " + number
                + ", status = " + status);
            if (mTypingCounter > 10) {
                return;
            }
            ContactList contact = new ContactList();
            contact.add(Contact.get(number, false));
            Conversation conv = Conversation.getCached(this, contact);
            if (conv == null) {
                MmsLog.w(TAG, "the number is not in conversation cache!");
                return;
            }
            //long threadId = conv.getThreadId();
            //MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "number query threadId:" + threadId);
            switch (status) {
            case ContactStatus.TYPING:
                conv.setTyping(true);
                MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "start typing");
                mTypingCounter++;
                runOnUiThread(new Runnable() {
                    public void run() {
                        mListAdapter.notifyDataSetChanged();
                    }
                });
                break;
            case ContactStatus.STOP_TYPING:
                conv.setTyping(false);
                MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "stop typing");
                mTypingCounter--;
                runOnUiThread(new Runnable() {
                    public void run() {
                        mListAdapter.notifyDataSetChanged();
                    }
                });
                break;
            default:
                MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "ignore a status:" + status);
                break;
            }
            */
            break;
        case IpMessageUtils.IPMSG_SAVE_HISTORY_ACTION:
            mHandler.post(new Runnable() {
                public void run() {
                    if (mSaveChatHistory != null) {
                        mSaveChatHistory.dismiss();
                        mSaveChatHistory = null;
                    }
                }
            });
            int done = intent.getIntExtra(IpMessageConsts.SaveHistroy.SAVE_HISTRORY_DONE, 1);
            MmsLog.d(TAG, "save history done: " + done);
            final String filePath = intent.getStringExtra(IpMessageConsts.SaveHistroy.DOWNLOAD_HISTORY_FILE);
            MmsLog.d(TAG, "save history file: " + filePath);

            if (!mIsSendEmail) { // it is saving
                if (done == 0) { // save ok
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(
                                getApplicationContext(),
                                getResources().getString(R.string.pref_download_chat_history) + ","
                                    + getResources().getString(R.string.save_history_file) + " " + filePath,
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                } else { //save fail
                    if (filePath != null) {
                        File saveHistoryFile = new File(filePath);
                        if (saveHistoryFile.exists()) {
                            saveHistoryFile.delete();
                        }
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                IpMessageUtils.getResourceManager(ConversationList.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_save_chat_history_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                mIsSendEmail = false;
                if (done == 0 && filePath != null) {
                    File emailFile = new File(filePath);
                    if (emailFile.exists()) {
                        MmsLog.d(TAG, "File: " + emailFile.getName());
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_SEND);
                        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(emailFile));
                        i.setType("message/rfc822");
                        try {
                            startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            MmsLog.w(TAG, "invoke email failed!");
                        }
                    } else {
                        MmsLog.w(TAG, "file does not exist!");
                    }
                } else {
                    if (filePath != null) {
                        File sendHistoryFile = new File(filePath);
                        if (sendHistoryFile.exists()) {
                            sendHistoryFile.delete();
                        }
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                IpMessageUtils.getResourceManager(ConversationList.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_send_chat_history_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            break;
        case IpMessageUtils.IPMSG_ACTIVATION_STATUS_ACTION:
            break;
        case IpMessageUtils.IPMSG_IP_MESSAGE_STATUS_ACTION:
            // handle this notification in MessageListItem
            break;
        case IpMessageUtils.IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION:
            // handle this notification in MessageListItem
            break;
        case IpMessageUtils.IPMSG_SET_PROFILE_RESULT_ACTION:
            break;
        case IpMessageUtils.IPMSG_BACKUP_MSG_STATUS_ACTION:
            break;
        case IpMessageUtils.IPMSG_RESTORE_MSG_STATUS_ACTION:
            break;
        case IpMessageUtils.IPMSG_UPDATE_GROUP_INFO:
            int groupId = intent.getIntExtra(IpMessageConsts.UpdateGroup.GROUP_ID, -1);
            MmsLog.d(TAG, "update group info,group id:" + groupId);
            String number = IpMessageUtils.getContactManager(this).getNumberByEngineId((short)groupId);
            MmsLog.d(TAG, "group number:" + number);
            Contact contact = Contact.get(number, false);
            if (contact != null) {
                contact.setName(null);
                contact.clearAvatar();
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    if (mListAdapter != null) {
                        mListAdapter.notifyDataSetChanged();
                    }
                }
            });
            break;
        case IpMessageUtils.IPMSG_IPMESSAGE_CONTACT_UPDATE:
            /** M: ipmessage plugin send this event when
             *  1. system contact info is changed, we may need update group avatar
             *  2. self head icon is changed, we may need update group avatar
             *  3. a ipmessage head icon is updated,  need update avatar
             *  if a system contact avatar is updated, and it is in a group.
             *  we will not receive a IPMSG_UPDATE_GROUP_INFO event,
             *  so we need invalid the group avatar cache and re-fetch it.
             */
            Contact.invalidateGroupCache();
            runOnUiThread(new Runnable() {
                public void run() {
                    if (mListAdapter != null) {
                        mListAdapter.notifyDataSetChanged();
                    }
                }
            });
            break;
        case IpMessageUtils.IPMSG_SIM_INFO_ACTION:
            /// M: for a special case, boot up enter mms quickly may be not get right status.
            if (mDropdownAdapter == null && !Conversation.getActivated() && MmsConfig.isActivated(this)) {
                /// M: init ipmessage view
                runOnUiThread(new Runnable() {
                    public void run() {
                        Conversation.setActivated(true);
                        initSpinnerListAdapter();
                        setTitle("");
                        mEmptyViewDefault.setVisibility(View.GONE);
                        mEmptyView.setVisibility(View.VISIBLE);
                        mListView.setEmptyView(mEmptyView);
                        invalidateOptionsMenu();
                    }
                });
            }
            break;
        default:
            break;
        }
    }

    private void  updateEmptyView(Cursor cursor) {
        MmsLog.d(TAG, "active:" + MmsConfig.isActivated(this));
        MmsLog.d(TAG, "cursor count:" + cursor.getCount());
        if (MmsConfig.isActivated(this) && (cursor != null) && (cursor.getCount() == 0)) {
            // when there is no items, show a view
            MmsLog.d(TAG, "sConversationListOption:" + sConversationListOption);
            switch (sConversationListOption) {
            case OPTION_CONVERSATION_LIST_ALL:
                mEmptyView.setAllChatEmpty();
                break;
            case OPTION_CONVERSATION_LIST_IMPORTANT:
                mEmptyView.setImportantEmpty(true);
                break;
            case OPTION_CONVERSATION_LIST_GROUP_CHATS:
                mEmptyView.setGroupChatEmpty(true);
                break;
            case OPTION_CONVERSATION_LIST_SPAM:
                mEmptyView.setSpamEmpty(true);
                break;
            default:
                MmsLog.w(TAG, "unkown position!");
                break;
            }
        }
    }

    private void showExportDialog(final HashSet<Long> selectedThreadIds, final ActionMode mode) {
        final java.util.ArrayList<String> urls = new ArrayList<String>();
        urls.add(IpMessageUtils.getResourceManager(this)
            .getSingleString(IpMessageConsts.string.ipmsg_dialog_save_title));
        urls.add(IpMessageUtils.getResourceManager(this)
            .getSingleString(IpMessageConsts.string.ipmsg_dialog_email_title));

        ArrayAdapter<String> adapter =
            new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, urls) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                String url = getItem(position).toString();
                TextView tv = (TextView) v;
                /*
                Drawable d = null;
                if (position == 0) {
                    d = getResources().getDrawable(R.drawable.ic_menu_omacp);
                } else {
                    d = getResources().getDrawable(R.drawable.ic_menu_omacp);
                }
                d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                tv.setCompoundDrawablePadding(10);
                tv.setCompoundDrawables(d, null, null, null);
                */
                tv.setText(url);
                return v;
            }
        };

        AlertDialog.Builder b = new AlertDialog.Builder(mContext);

        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialog, int which) {
                if (!Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(ConversationList.this,
                        ConversationList.this.getResources().getString(R.string.no_sdcard_suggestion),
                        Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }
                switch (which) {
                case 0: // save to SD card.
                case 1: // email
                    MmsLog.d(TAG, "onclick threadIds:" + selectedThreadIds.toString());

                    // export to sd card
                    long[] selectThreadIds = new long[selectedThreadIds.size()];
                    int i = 0;
                    for (Long threadId : selectedThreadIds) {
                        selectThreadIds[i] = threadId.longValue();
                        MmsLog.d(TAG, "threadId:" + threadId);
                        i++;
                    }
                    int resId = IpMessageConsts.string.ipmsg_chat_setting_saving;
                    if (which == 1) {
                        resId = IpMessageConsts.string.ipmsg_chat_setting_sending;
                        mIsSendEmail = true;
                    }
                    mSaveChatHistory = ProgressDialog.show(ConversationList.this, null,
                                IpMessageUtils.getResourceManager(ConversationList.this).getSingleString(resId));
                    IpMessageUtils.getChatManager(ConversationList.this).saveChatHistory(selectThreadIds);
                    break;
                default:
                    break;
                }
                dialog.dismiss();
                mode.finish();
            }
        };

        b.setTitle(R.string.export);
        b.setCancelable(true);
        b.setAdapter(adapter, click);

        b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        b.show();
    }

    // a receiver to moniter the network status.
    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = false;
            ConnectivityManager connManager =
                    (ConnectivityManager) getSystemService(ConversationList.this.CONNECTIVITY_SERVICE);
            State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
            if (State.CONNECTED == state) {
                success = true;
            }
            if (!success) {
                state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
                if (State.CONNECTED == state) {
                    success = true;
                }
            }
            ConversationList.this.showInternetStatusBar(!success);
        }
    }

    private void showInternetStatusBar(boolean show) {
        if (show) {
            mNetworkStatusBar.setVisibility(View.VISIBLE);
        } else {
            mNetworkStatusBar.setVisibility(View.GONE);
        }
    }

    /// M: Fix bug ALPS00416081 @{
    private Menu mOptionsMenu;

    private void setDeleteMenuVisible(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.action_delete_all);
            if (item != null) {
                mDataValid = mListAdapter.isDataValid();
                item.setVisible(mListAdapter.getCount() > 0);
            }
        }
    }
    /// @}

    /// M: redesign selection action bar and add shortcut in common version. @{
    private DropDownMenu mSelectionMenu;
    private MenuItem mSelectionMenuItem;

    private void updateSelectionTitle() {
        if (mSelectionMenuItem != null) {
            if (mListAdapter.isAllSelected()) {
                mSelectionMenuItem.setTitle(R.string.unselect_all);
            } else {
                mSelectionMenuItem.setTitle(R.string.select_all);
            }
        }
    }
    /// @}

    /// Google JB MR1.1 patch. conversation list can restore scroll position
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(LAST_LIST_POS, mSavedFirstVisiblePosition);
        outState.putInt(LAST_LIST_OFFSET, mSavedFirstItemOffset);
    }
}
