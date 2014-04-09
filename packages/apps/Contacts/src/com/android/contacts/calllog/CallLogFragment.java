/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.calllog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Context;
/// The following lines are provided and maintained by Mediatek Inc.
import android.content.ContentUris;
/// The previous lines are provided and maintained by Mediatek Inc.
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
/** M: New Feature Phone Landscape UI @{ */
import android.content.res.Configuration;
/** @ }*/
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
/// The following lines are provided and maintained by Mediatek Inc.
import android.provider.Contacts.Intents.Insert;
/// The previous lines are provided and maintained by Mediatek Inc.
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.ProviderStatus;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

//import com.android.common.io.MoreCloseables;
import com.android.contacts.BackScrollManager;
import com.android.contacts.BackScrollManager.ScrollableHeader;
import com.android.contacts.CallDetailActivity.Tasks;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactsUtils;
import com.android.contacts.PhoneCallDetails;
import com.android.contacts.PhoneCallDetailsHelper;
import com.android.contacts.R;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.ext.ContactPluginDefault;
import com.android.contacts.format.FormatUtils;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.util.AsyncTaskExecutor;
import com.android.contacts.util.AsyncTaskExecutors;
import com.android.contacts.util.ClipboardUtils;
import com.android.contacts.util.Constants;
import com.android.contacts.util.EmptyLoader;
import com.android.contacts.voicemail.VoicemailStatusHelper;
//import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
//import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.ITelephony;
import com.google.common.annotations.VisibleForTesting;

/// The following lines are provided and maintained by Mediatek Inc.
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.activities.CallLogMultipleDeleteActivity;
import com.mediatek.contacts.calllog.CallLogListAdapter;
import com.mediatek.contacts.calllog.CallLogListItemView;
import com.mediatek.contacts.calllog.CallLogSimInfoHelper;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.VvmUtils;
import com.mediatek.contacts.widget.SimPickerDialog;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.xlog.Xlog;
/// The previous lines are provided and maintained by Mediatek Inc.

/**
 * Displays a list of call log entries.
 */
public class CallLogFragment extends ListFragment
/** M:  modify @ { */
/**
 * implements CallLogQueryHandler.Listener, CallLogAdapter.CallFetcher {
 */
        implements CallLogQueryHandler.Listener, CallLogAdapter.CallFetcher, View.OnClickListener {
    private static final String TAG = "CallLogFragment";

    /**
     * ID of the empty loader to defer other fragments.
     */
    private static final int EMPTY_LOADER_ID = 0;

    private CallLogListAdapter mAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    private boolean mScrollToTop;

    /** Whether there is at least one voicemail source installed. */
    private boolean mVoicemailSourcesAvailable = false;

    private VoicemailStatusHelper mVoicemailStatusHelper;
    private View mStatusMessageView;
    private TextView mStatusMessageText;
    private TextView mStatusMessageAction;
    private KeyguardManager mKeyguardManager;

    private boolean mEmptyLoaderRunning;
    private boolean mCallLogFetched;
    private boolean mVoicemailStatusFetched;
    /** M:  delete @ { */
    // private final Handler mHandler = new Handler();
    
    private boolean mHasSms = true;
    private String mNumber = "";
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";
    private   static int mPosition = 0;
    private TextView mHeaderTextView;
    private View mHeaderOverlayView;
    private ImageView mMainActionView;
    private ImageButton mMainActionPushLayerView;
    private ImageView mContactBackgroundView;
    private AsyncTaskExecutor mAsyncTaskExecutor;
    private ContactInfoHelper mContactInfoHelper;
    LayoutInflater mInflater;
    Resources mResources;
    private CallTypeHelper mCallTypeHelper;
    private PhoneNumberHelper mPhoneNumberHelper;
    private PhoneCallDetailsHelper mPhoneCallDetailsHelper;
    private TextView mSimName;
    private View mControls;
    private View convertView;   
    private ImageView icon;
    private  View divider;
    private TextView text;
    private  View mainAction ;
    private ListView historyList;
    private CharSequence mPhoneNumberLabelToCopy;
    private CharSequence mPhoneNumberToCopy;
    private boolean mHasEditNumberBeforeCallOption;
    private View mHeader;
    private View mSeparator;
    private View mPhoto;
    private View mCallDetail;
    private View mSeparator01;
    private View mSeparator02;
    private View mConvertView1;
    private View mConvertView2;
    private ContactPhotoManager mContactPhotoManager;
    private ActionMode mPhoneNumberActionMode;
    private CallLogListItemView mOldItemView;
    public static boolean  ISTABLET_LAND = false;
    private Context mContext;
    private class CustomContentObserver extends ContentObserver {
        public CustomContentObserver() {
            /** M:  delete @ { */
            // super(mHandler);
            super(new Handler());
        }
        @Override
        public void onChange(boolean selfChange) {
            mRefreshDataRequired = true;
            /** M: add @ { */
            mScrollToTop = true;
        }
    }

    // See issue 6363009
    private final ContentObserver mCallLogObserver = new CustomContentObserver();
    private final ContentObserver mContactsObserver = new CustomContentObserver();
    private boolean mRefreshDataRequired = true;

    // Exactly same variable is in Fragment as a package private.
    private boolean mMenuVisible = true;
    private static final int WAITING_DESCRIPTION_PADDING = 10;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        /** M: add @ { */
        //For performance auto test filter
        Xlog.i(TAG, "[Performance test][Contacts] loading data start time: ["
                + System.currentTimeMillis() + "]");
        /** @ }*/
        mCallLogQueryHandler = new CallLogQueryHandler(getActivity().getContentResolver(), this);
        mKeyguardManager =
                (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        getActivity().getContentResolver().registerContentObserver(
                CallLog.CONTENT_URI, true, mCallLogObserver);
        getActivity().getContentResolver().registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, mContactsObserver);
        setHasOptionsMenu(true);
        /** M: add @ { */
        SIMInfoWrapper.getDefault().registerForSimInfoUpdate(
                mHandler, SIM_INFO_UPDATE_MESSAGE, null);

        mContactPhotoManager = ContactPhotoManager.getInstance(this.getActivity().getApplication());
        mAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
        mInflater = (LayoutInflater) getActivity().getSystemService("layout_inflater");
        mResources = getResources();
        mCallTypeHelper = new CallTypeHelper(getResources());
        mPhoneNumberHelper = new PhoneNumberHelper(mResources);
        mPhoneCallDetailsHelper = new PhoneCallDetailsHelper(mResources, mCallTypeHelper,
                                                            mPhoneNumberHelper, null, getActivity());
        mContext = this.getActivity();
        
    }

    /** Called by the CallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public void onCallsFetched(Cursor cursor) {
        log("onCallsFetched(), cursor = " + cursor);
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        mAdapter.setLoading(false);
        mAdapter.changeCursor(cursor);
        // when dialpadfrangment is in forgoround, not update dial pad menu item.
        Activity activity = getActivity();
        if (activity instanceof DialtactsActivity
                && TAB_INDEX_CALL_LOG == ((DialtactsActivity) getActivity())
                    .getCurrentFragmentId()) {
            // This will update the state of the "Clear call log" menu item.
            getActivity().invalidateOptionsMenu();
        }
        if (mScrollToTop) {
            final ListView listView = getListView();
            /** M:  modify @ { */
            /**
             * 
            // The smooth-scroll animation happens over a fixed time period.
            // As a result, if it scrolls through a large portion of the list,
            // each frame will jump so far from the previous one that the user
            // will not experience the illusion of downward motion.  Instead,
            // if we're not already near the top of the list, we instantly jump
            // near the top, and animate from there.
            if (listView.getFirstVisiblePosition() > 5) {
                listView.setSelection(5);
            }
            // Workaround for framework issue: the smooth-scroll doesn't
            // occur if setSelection() is called immediately before.
            mHandler.post(new Runnable() {
               @Override
               public void run() {
                   if (getActivity() == null || getActivity().isFinishing()) return;
                   listView.smoothScrollToPosition(0);
               }
            });
             */
            
            listView.setSelection(0);
            mScrollToTop = false;
        }
        mCallLogFetched = true;     
        
        /** M: add :Bug Fix for ALPS00115673 @ { */
        Log.i(TAG, "onCallsFetched is call");
        mIsFinished = true;
        mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                android.R.anim.fade_out));
        mLoadingContainer.setVisibility(View.GONE);
        mLoadingContact.setVisibility(View.GONE);
        mProgress.setVisibility(View.GONE);
        // hide calldetail view,let no call log warning show on all screen
        if (mCallDetail != null) {
            if (cursor == null || cursor.getCount() == 0) {
                mCallDetail.setVisibility(View.GONE);
            } else {
                mCallDetail.setVisibility(View.VISIBLE);
            }
        }

        mEmptyTitle.setText(R.string.recentCalls_empty);
        /** @ }*/

        destroyEmptyLoaderIfAllDataFetched();
        // send message,the message will execute after the listview inflate
        handle.sendEmptyMessage(SETFIRSTTAG);
    }
    private static final int SETFIRSTTAG = 101;
    public Handler handle = new Handler() {
         @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      switch(msg.what) {
                //when onCallsFetched finish,set the first item of list on screen
        case SETFIRSTTAG:         
        ListView list = null;
        try {   
            list =  CallLogFragment.this.getListView();
          } catch (Exception e) { 
            e.printStackTrace();
          }     
        CallLogListItemView itemView = null;
        if (list != null) {
          itemView = (CallLogListItemView) list.getChildAt(0);
        }
        if (ISTABLET_LAND) {
          if (itemView != null) {     
            itemView.getSelectImageView().setVisibility(View.VISIBLE);  
            IntentProvider intentProvider = (IntentProvider) itemView.getTag();
            Context context = CallLogFragment.this.getActivity();
                if (intentProvider != null) { 
                      Intent  in = intentProvider.getIntent(context);
                      in.putExtra(Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, true);
                      if (ISTABLET_LAND) {  
                          mAdapter.itemSetSelect(itemView, mOldItemView);
                          mAdapter.setSelectedPosition(itemView.getTagId());
                          mOldItemView = itemView;
                          updateData(getCallLogEntryUris(in));
                       }
                } 
          }
        } else {
          if (itemView != null) { 
            mAdapter.setSelectedPosition(itemView.getTagId());
                  mOldItemView = itemView;
          } 
        }       
        break;      
      }
    } 
    };

    /**
     * Called by {@link CallLogQueryHandler} after a successful query to voicemail status provider.
     */
    @Override
    public void onVoicemailStatusFetched(Cursor statusCursor) {
        /** M:  delete @ { */    
        /**
         * 
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        updateVoicemailStatusMessage(statusCursor);

        int activeSources = mVoicemailStatusHelper.getNumberActivityVoicemailSources(statusCursor);
        setVoicemailSourcesAvailable(activeSources != 0);
        MoreCloseables.closeQuietly(statusCursor);
        mVoicemailStatusFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
     */
        /** @ }*/
    }

    private void destroyEmptyLoaderIfAllDataFetched() {
        if (mCallLogFetched && mVoicemailStatusFetched && mEmptyLoaderRunning) {
            mEmptyLoaderRunning = false;
            getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
        }
    }

    /** Sets whether there are any voicemail sources available in the platform. */
    /** M:  delete @ { */
    /**
     * 
    private void setVoicemailSourcesAvailable(boolean voicemailSourcesAvailable) {
        if (mVoicemailSourcesAvailable == voicemailSourcesAvailable) return;
        mVoicemailSourcesAvailable = voicemailSourcesAvailable;

        Activity activity = getActivity();
        if (activity != null) {
            // This is so that the options menu content is updated.
            activity.invalidateOptionsMenu();
        }
    }
     */
    /** @ }*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.call_log_fragment, container, false);
        /** M:  modify @ { */
        /**
         * 
        mVoicemailStatusHelper = new VoicemailStatusHelperImpl();
        mStatusMessageView = view.findViewById(R.id.voicemail_status);
        mStatusMessageText = (TextView) view.findViewById(R.id.voicemail_status_message);
        mStatusMessageAction = (TextView) view.findViewById(R.id.voicemail_status_action);
         */

        mTypeFilterAll = (Button) view.findViewById(R.id.btn_type_filter_all);
        mTypeFilterOutgoing = (Button) view.findViewById(R.id.btn_type_filter_outgoing);
        mTypeFilterIncoming = (Button) view.findViewById(R.id.btn_type_filter_incoming);
        mTypeFilterMissed = (Button) view.findViewById(R.id.btn_type_filter_missed);

        mLayoutAutorejected = (LinearLayout) view.findViewById(R.id.calllog_auto_rejected_cluster);
        mLayoutSearchbutton = (LinearLayout) view.findViewById(R.id.calllog_search_button_cluster);

        mTypeFilterAll.setOnClickListener(this);
        mTypeFilterOutgoing.setOnClickListener(this);
        mTypeFilterIncoming.setOnClickListener(this);
        mTypeFilterMissed.setOnClickListener(this);
          
        /** add wait cursor*/
        mLoadingContainer = view.findViewById(R.id.loading_container);
        mLoadingContainer.setVisibility(View.GONE);
        mEmptyTitle = (TextView) view.findViewById(android.R.id.empty);
        mEmptyTitle.setText(R.string.recentCalls_empty);
        mLoadingContact = (TextView) view.findViewById(R.id.loading_contact);
        mLoadingContact.setSingleLine(false);
        mLoadingContact.setTextAppearance(this.getActivity(), android.R.style.TextAppearance_Large);
        mLoadingContact.setPadding(WAITING_DESCRIPTION_PADDING, 0, WAITING_DESCRIPTION_PADDING, 0);
        mLoadingContact.setGravity(Gravity.CENTER_HORIZONTAL);
        mLoadingContact.setVisibility(View.GONE);
        mProgress = (ProgressBar) view.findViewById(R.id.progress_loading_contact);
        mProgress.setVisibility(View.GONE);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                this.getActivity()).edit();
        editor.putInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_DEFAULT);
        editor.putInt(Constants.SIM_FILTER_PREF, Constants.FILTER_ALL_RESOURCES);
        changeButton(mTypeFilterAll);
        editor.commit();
        /** @ }*/

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ISTABLET_LAND = true;
        } else {
            ISTABLET_LAND = false;
        }
        //when device in landscape,get the view
        if (ISTABLET_LAND) {  
          mHeaderTextView = (TextView)view.findViewById(R.id.header_text);
            mHeaderOverlayView = view.findViewById(R.id.photo_text_bar);
            mMainActionView = (ImageView) view.findViewById(R.id.main_action);
            mMainActionPushLayerView = (ImageButton) view.findViewById(R.id.main_action_push_layer);
            mContactBackgroundView = (ImageView) view.findViewById(R.id.contact_background);
            mSimName = (TextView) view.findViewById(R.id.sim_name);
            convertView = view.findViewById(R.id.call_and_sms);
            icon = (ImageView) convertView.findViewById(R.id.call_and_sms_icon);
            divider = convertView.findViewById(R.id.call_and_sms_divider);
            text = (TextView) convertView.findViewById(R.id.call_and_sms_text);
            mainAction = convertView.findViewById(R.id.call_and_sms_main_action);
            historyList = (ListView) view.findViewById(R.id.history);
            mControls = view.findViewById(R.id.controls);
            mPhoto = view.findViewById(R.id.contact_background_sizer);
            mHeader = view.findViewById(R.id.photo_text_bar);
            mSeparator = view.findViewById(R.id.blue_separator);
            mCallDetail = view.findViewById(R.id.call_detail);           
            mSeparator01 = view.findViewById(R.id.separator01);
            mSeparator01.setVisibility(View.VISIBLE);
            mSeparator02 = view.findViewById(R.id.separator02);
            mSeparator02.setVisibility(View.VISIBLE);
            mConvertView1 = view.findViewById(R.id.video_call);
            mConvertView2 = view.findViewById(R.id.ip_call);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String currentCountryIso = ContactsUtils.getCurrentCountryIso(getActivity());
        mAdapter = new CallLogListAdapter(getActivity(), this,
                new ContactInfoHelper(getActivity(), currentCountryIso),this);
        setListAdapter(mAdapter);
        /** M:  modify @ { */
       /**
        *  getListView().setItemsCanFocus(true);
        */
        final ListView listView = getListView();
        if (null != listView) {
            listView.setItemsCanFocus(true);
            listView.setOnScrollListener(mAdapter);
        }
        /** @ }*/
    }

    /**
     * Based on the new intent, decide whether the list should be configured
     * to scroll up to display the first item.
     */
    public void configureScreenFromIntent(Intent newIntent) {
        // Typically, when switching to the call-log we want to show the user
        // the same section of the list that they were most recently looking
        // at.  However, under some circumstances, we want to automatically
        // scroll to the top of the list to present the newest call items.
        // For example, immediately after a call is finished, we want to
        // display information about that call.
        mScrollToTop = Calls.CONTENT_TYPE.equals(newIntent.getType());
    }

    @Override
    public void onStart() {
        // Start the empty loader now to defer other fragments.  We destroy it when both calllog
        // and the voicemail status are fetched.
        getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
                new EmptyLoader.Callback(getActivity()));
        mEmptyLoaderRunning = true;
        super.onStart();
        /** M: when low memory clear the thumbnails, set the default photo before load complete @ { */
        if (mAdapter != null && mAdapter.getCursor() != null) {
            mAdapter.notifyDataSetChanged();
        }
        /** @ */
    }

    @Override
    public void onResume() {
        super.onResume();
        /** M: add @ { */
        /* should not load data when calls table did not changed */
        /* mRefreshDataRequired = true; */
        PhoneNumberHelper.getVoiceMailNumber();
        refreshData();
        Xlog.i(TAG, "[Performance test][Contacts] loading data end time: ["
                + System.currentTimeMillis() + "]");
        /** @ }*/
    }
    /** M: delete @ { */
    /**
     * 
    private void updateVoicemailStatusMessage(Cursor statusCursor) {
        List<StatusMessage> messages = mVoicemailStatusHelper.getStatusMessages(statusCursor);
        if (messages.size() == 0) {
            mStatusMessageView.setVisibility(View.GONE);
        } else {
            mStatusMessageView.setVisibility(View.VISIBLE);
            // TODO: Change the code to show all messages. For now just pick the first message.
            final StatusMessage message = messages.get(0);
            if (message.showInCallLog()) {
                mStatusMessageText.setText(message.callLogMessageId);
            }
            if (message.actionMessageId != -1) {
                mStatusMessageAction.setText(message.actionMessageId);
            }
            if (message.actionUri != null) {
                mStatusMessageAction.setVisibility(View.VISIBLE);
                mStatusMessageAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().startActivity(
                                new Intent(Intent.ACTION_VIEW, message.actionUri));
                    }
                });
            } else {
                mStatusMessageAction.setVisibility(View.GONE);
            }
        }
    }
     */
    /** @ }*/

    @Override
    public void onStop() {
        super.onStop();
        updateOnExit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /** M:  delete @ { */
        /**
         * mAdapter.stopRequestProcessing();
         */
        mAdapter.changeCursor(null);
        getActivity().getContentResolver().unregisterContentObserver(mCallLogObserver);
        getActivity().getContentResolver().unregisterContentObserver(mContactsObserver);
        /** M: add @ { */
        SIMInfoWrapper.getDefault().unregisterForSimInfoUpdate(mHandler);
    }

    @Override
    public void fetchCalls() {
        /** M: Bug Fix for ALPS00346240 @{ */
        // mCallLogQueryHandler.fetchAllCalls();
        /** @} */
        /** M: add @ { */
        Activity activity = this.getActivity();
        if (activity == null) {
            Log.e(TAG, " fetchCalls(), but this.getActivity() is null, use default value");
            mCallLogQueryHandler.fetchCallsJionDataView(Constants.FILTER_SIM_DEFAULT,
                    Constants.FILTER_TYPE_DEFAULT);
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this
                    .getActivity());
            int simFilter = prefs.getInt(Constants.SIM_FILTER_PREF,
                    Constants.FILTER_SIM_DEFAULT);
            int typeFilter = prefs.getInt(Constants.TYPE_FILTER_PREF,
                    Constants.FILTER_TYPE_DEFAULT);
            mCallLogQueryHandler.fetchCallsJionDataView(simFilter, typeFilter);
        }
        /** @ }*/
    }

    public void startCallsQuery() {
        mAdapter.setLoading(true);
        /** M:  modify @ { */
        /**
         * mCallLogQueryHandler.fetchAllCalls();
         */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        int simFilter = prefs.getInt(Constants.SIM_FILTER_PREF, Constants.FILTER_SIM_DEFAULT);
        int typeFilter = prefs.getInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_DEFAULT);
        mCallLogQueryHandler.fetchCallsJionDataView(simFilter, typeFilter);
        /* add wait cursor */
        int count = this.getListView().getCount();
        Log.i(TAG, "***********************count : " + count);
        mIsFinished = false;

        ProviderStatusWatcher.Status status = ((DialtactsActivity) getActivity())
                .getProviderStatus();

        if (0 == count) {
            Log.i(TAG, "call sendmessage");
            mHandler.sendMessageDelayed(mHandler.obtainMessage(WAIT_CURSOR_START),
                    WAIT_CURSOR_DELAY_TIME);
            updateProviderStauts(status);
        }
        /** @ }*/
    }
    /** M:  delete @ { */
    /**
     * 
    private void startVoicemailStatusQuery() {
        log("startVoicemailStatusQuery()");
        mCallLogQueryHandler.fetchVoicemailStatus();
    }
     */
    /** @ }*/
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.call_log_options, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem itemDeleteAll = menu.findItem(R.id.delete_all);
        // Check if all the menu items are inflated correctly. As a shortcut, we assume all
        // menu items are ready if the first item is non-null.
        if (itemDeleteAll != null) {
            itemDeleteAll.setEnabled(mAdapter != null && !mAdapter.isEmpty());

            /** M:  delete @ { */
            /**
             * showAllFilterMenuOptions(menu);
             * hideCurrentFilterMenuOption(menu);
             */
            /** @ }*/
            // Only hide if not available.  Let the above calls handle showing.
            if (!mVoicemailSourcesAvailable) {
                menu.findItem(R.id.show_voicemails_only).setVisible(false);
            }
            /** M: modify @ { */
            /**
             * 
            menu.findItem(R.id.show_voicemails_only).setVisible(
                    mVoicemailSourcesAvailable && !mShowingVoicemailOnly);
            menu.findItem(R.id.show_all_calls).setVisible(
                    mVoicemailSourcesAvailable && mShowingVoicemailOnly);
             */
            menu.findItem(R.id.show_all_calls).setVisible(false);
            /** @ }*/
            /** M: New Feature Easy Porting @ { */
                boolean bShowAutoRejectedMenu = ExtensionManager.getInstance()
                        .getCallDetailExtension().isNeedAutoRejectedMenu(
                                !isAutoRejectedFilterMode(), ContactPluginDefault.COMMD_FOR_OP01);
                menu.findItem(R.id.show_auto_rejected_calls).setVisible(bShowAutoRejectedMenu);
             
                 /** @ } */
        }
    }
    

    /** M: delete @ { */
    /**
    private void hideCurrentFilterMenuOption(Menu menu) {
        MenuItem item = null;
        switch (mCallTypeFilter) {
            case CallLogQueryHandler.CALL_TYPE_ALL:
                item = menu.findItem(R.id.show_all_calls);
                break;
            case Calls.INCOMING_TYPE:
                item = menu.findItem(R.id.show_incoming_only);
                break;
            case Calls.OUTGOING_TYPE:
                item = menu.findItem(R.id.show_outgoing_only);
                break;
            case Calls.MISSED_TYPE:
                item = menu.findItem(R.id.show_missed_only);
                break;
            case Calls.VOICEMAIL_TYPE:
                menu.findItem(R.id.show_voicemails_only);
                break;
        }
        if (item != null) {
            item.setVisible(false);
        }
    }
    private void showAllFilterMenuOptions(Menu menu) {
        menu.findItem(R.id.show_all_calls).setVisible(true);
        menu.findItem(R.id.show_incoming_only).setVisible(true);
        menu.findItem(R.id.show_outgoing_only).setVisible(true);
        menu.findItem(R.id.show_missed_only).setVisible(true);
        menu.findItem(R.id.show_voicemails_only).setVisible(true);
    }
    */
    /** @ }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected(), item id = " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.delete_all:
                /** M:  modify @ { */
                /**
                 * ClearCallLogDialog.show(getFragmentManager());
                 */
                final Intent intent = new Intent(getActivity(), 
                                     CallLogMultipleDeleteActivity.class);
                getActivity().startActivity(intent);
                /** @ }*/
                return true;
                /** M:  delete @ { */
//            case R.id.show_voicemails_only:
//                mCallLogQueryHandler.fetchVoicemailOnly();
//                mShowingVoicemailOnly = true;
//                return true;
//
//            case R.id.show_all_calls:
//                /** M:  modify @ { */
//               /**
//                *  mCallLogQueryHandler.fetchAllCalls();
//                */
//                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this
//                        .getActivity());
//                int typeFilter = prefs.getInt(Constants.TYPE_FILTER_PREF,
//                        Constants.FILTER_TYPE_DEFAULT);
//                mCallLogQueryHandler.fetchCallsJionDataView(simFilter, typeFilter);
//                /** @ }*/
//                mShowingVoicemailOnly = false;
//                return true;

                //android-4.4.2_r1
//            case R.id.show_voicemails_only:
//                registerPhoneCallReceiver();
//                mCallLogQueryHandler.fetchCalls(Calls.VOICEMAIL_TYPE);
//                updateFilterTypeAndHeader(Calls.VOICEMAIL_TYPE);
//                return true;
//
//            case R.id.show_all_calls:
//                // Filter is being turned off, receiver no longer needed.
//                unregisterPhoneCallReceiver();
//                mCallLogQueryHandler.fetchCalls(CallLogQueryHandler.CALL_TYPE_ALL);
//                updateFilterTypeAndHeader(CallLogQueryHandler.CALL_TYPE_ALL);
//                return true;
                
                /** @ }*/
                /** M: add @ { */
            case R.id.show_auto_rejected_calls:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
                int simFilter = prefs.getInt(Constants.SIM_FILTER_PREF,
                        Constants.FILTER_SIM_DEFAULT);
                mCallLogQueryHandler.fetchCallsJionDataView(simFilter,
                        Constants.FILTER_TYPE_AUTO_REJECT);
                changeButton(null);
                return true;
                /** @ }*/

            default:
                return false;
        }
    }

    /** M:  delete @ { */
//    public void callSelectedEntry() {
//        log("callSelectedEntry()");
//        int position = getListView().getSelectedItemPosition();
//        if (position < 0) {
//            // In touch mode you may often not have something selected, so
//            // just call the first entry to make sure that [send] [send] calls the
//            // most recent entry.
//            position = 0;
//        }
//        final Cursor cursor = (Cursor)mAdapter.getItem(position);
//        if (cursor != null) {
//            String number = cursor.getString(CallLogQuery.NUMBER);
//            if (TextUtils.isEmpty(number)
//                    || number.equals(CallerInfo.UNKNOWN_NUMBER)
//                    || number.equals(CallerInfo.PRIVATE_NUMBER)
//                    || number.equals(CallerInfo.PAYPHONE_NUMBER)) {
//                // This number can't be called, do nothing
//                return;
//            }
//            Intent intent;
//            // If "number" is really a SIP address, construct a sip: URI.
//            if (PhoneNumberUtils.isUriNumber(number)) {
//                intent = ContactsUtils.getCallIntent(
//                        Uri.fromParts(Constants.SCHEME_SIP, number, null));
//                /** M: add @ { */
//                intent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER);
//            } else {
//                // We're calling a regular PSTN phone number.
//                // Construct a tel: URI, but do some other possible cleanup first.
//                int callType = cursor.getInt(CallLogQuery.CALL_TYPE);
//                if (!number.startsWith("+") &&
//                       (callType == Calls.INCOMING_TYPE
//                                || callType == Calls.MISSED_TYPE)) {
//                    // If the caller-id matches a contact with a better qualified number, use it
//                    String countryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
//                    number = mAdapter.getBetterNumberFromContacts(number, countryIso);
//                }
//                intent = ContactsUtils.getCallIntent(
//                        Uri.fromParts(Constants.SCHEME_TEL, number, null));
//                /** M: add @ { */
//                intent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER);
//            }
//            intent.setFlags(
//                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//            startActivity(intent);
//        }
//    }
    /** @ } */
    @VisibleForTesting
    CallLogListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
            if (!menuVisible) {
                updateOnExit();
            } else if (isResumed()) {
                refreshData();
            }
        }
    }

    /** Requests updates to the data to be shown. */
    private void refreshData() {
        // Prevent unnecessary refresh.
        if (mRefreshDataRequired) {
            log("refreshData()");
            // Mark all entries in the contact info cache as out of date, so they will be looked up
            // again once being shown.
            /** M:  delete @ { */
            // mAdapter.invalidateCache();
            startCallsQuery();
            /** M:  delete @ { */
            // startVoicemailStatusQuery();
            // updateOnEntry();
            mRefreshDataRequired = false;
        }
        /** M:  add :some times do not remove missed call notification@ { */
            updateOnEntry();
    }

    /** Removes the missed call notifications. */
    private void removeMissedCallNotifications() {
        try {
            ITelephony telephony =
                    ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (telephony != null) {
                telephony.cancelMissedCallsNotification();
            } else {
                Log.w(TAG, "Telephony service is null, can't call " +
                        "cancelMissedCallsNotification");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to clear missed calls notification due to remote exception");
        }
    }

    /** Updates call data and notification state while leaving the call log tab. */
    private void updateOnExit() {
        updateOnTransition(false);
    }

    /** Updates call data and notification state while entering the call log tab. */
    private void updateOnEntry() {
        updateOnTransition(true);
    }

    private void updateOnTransition(boolean onEntry) {
        // We don't want to update any call data when keyguard is on because the user has likely not
        // seen the new calls yet.
        // This might be called before onCreate() and thus we need to check null explicitly.
        if (mKeyguardManager != null && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
            // On either of the transitions we reset the new flag and update the notifications.
            // While exiting we additionally consume all missed calls (by marking them as read).
            // This will ensure that they no more appear in the "new" section when we return back.
            /** M:  delete :only when the current tab is call log, call markNewCallsAsOld()@ { */
            // mCallLogQueryHandler.markNewCallsAsOld();
            if (!onEntry) {
                mCallLogQueryHandler.markMissedCallsAsRead();
            }
            /** M:  modify @ { */
            /**
             * 
            removeMissedCallNotifications();
            updateVoicemailNotifications();
             */
            Activity activity = getActivity();
            if (activity instanceof DialtactsActivity
                    && TAB_INDEX_CALL_LOG == ((DialtactsActivity) getActivity())
                            .getCurrentFragmentId()) {
                /**M: [VVM] cancel VVM notification */
                if (VvmUtils.isVvmEnabled()) {
                    updateVoicemailNotifications();
                }
                mCallLogQueryHandler.markNewCallsAsOld();
                removeMissedCallNotifications();
            }
        }
    }

    private void updateVoicemailNotifications() {
        Intent serviceIntent = new Intent(getActivity(), CallLogNotificationsService.class);
        serviceIntent.setAction(CallLogNotificationsService.ACTION_UPDATE_NOTIFICATIONS);
        getActivity().startService(serviceIntent);
    }

    /** M: add @ { */
    private static final int TAB_INDEX_CALL_LOG = 1;
    private static final int SIM_INFO_UPDATE_MESSAGE = 100;
    private static final int FLAG_FILTER_MODE_NORMAL = 0;
    private static final int FLAG_FILTER_MODE_AUTO_REJECTED = FLAG_FILTER_MODE_NORMAL + 1;
    private static final int WAIT_CURSOR_START = 1230;
    private static final int PROVIDER_STATUS_CHANGING_LOCALE = 1250;
    private static final int PROVIDER_STATUS_UPGRATING = 1251;
    private static final long WAIT_CURSOR_DELAY_TIME = 500;
    
    private boolean mIsFinished;
    
    private Button mTypeFilterAll;
    private Button mTypeFilterOutgoing;
    private Button mTypeFilterIncoming;
    private Button mTypeFilterMissed;
    private LinearLayout mLayoutAutorejected;
    private LinearLayout mLayoutSearchbutton;
    
    public  AlertDialog mSelectResDialog;
    private TextView mLoadingContact;
    private ProgressBar mProgress;
    private TextView mEmptyTitle;
    private View mLoadingContainer;
    private View mViewRestored;

    private int mModeFlag = FLAG_FILTER_MODE_NORMAL;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            log("handleMessage msg = " + msg.what + " mIsFinished:" + mIsFinished);

            switch (msg.what) {
                case SIM_INFO_UPDATE_MESSAGE:
                    if (null != mAdapter) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;

                case WAIT_CURSOR_START:
                    Log.i(TAG, "start WAIT_CURSOR_START !isFinished : " + !mIsFinished);
                    if (!mIsFinished) {
                        mEmptyTitle.setText("");
                        mLoadingContact.setText(R.string.contact_list_loading);
                        mLoadingContainer.setVisibility(View.VISIBLE);
                        mLoadingContact.setVisibility(View.VISIBLE);
                        mProgress.setVisibility(View.VISIBLE);
                    }
                    break;

                // Waiting cursor description changed when displaying
                case PROVIDER_STATUS_CHANGING_LOCALE:
                    if (!mIsFinished) {
                        mEmptyTitle.setText("");
                        mLoadingContact.setText(R.string.locale_change_in_progress);
                    }
                    break;

                case PROVIDER_STATUS_UPGRATING:
                    if (!mIsFinished) {
                        mEmptyTitle.setText("");
                        mLoadingContact.setText(R.string.contact_list_loading);
                    }
                    break;

                default:
                    break;
            }
        }
    };

    /** Called by the CallLogQueryHandler when the list of calls has been deleted. */
    @Override
    public void onCallsDeleted() {
        log("onCallsDeleted(), do nothing");
    }

    private void changeButton(View view) {
        log("changeButton(), view = " + view);
        if (null == view) {
            if (FLAG_FILTER_MODE_AUTO_REJECTED != mModeFlag) {
                mModeFlag = FLAG_FILTER_MODE_AUTO_REJECTED;
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                        this.getActivity()).edit();
                editor.putInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_AUTO_REJECT);
                editor.commit();
                setAutoRejectedModeVisibility(View.VISIBLE);
            }
            return;
        }

        if (FLAG_FILTER_MODE_NORMAL != mModeFlag) {
            setAutoRejectedModeVisibility(View.GONE);
            mModeFlag = FLAG_FILTER_MODE_NORMAL;
        }

        mViewRestored = view;
        if (view != mTypeFilterAll) {
            mTypeFilterAll.setBackgroundResource(R.drawable.btn_calllog_all);
        } else {
            mTypeFilterAll.setBackgroundResource(R.drawable.btn_calllog_all_sel);
        }

        if (view != mTypeFilterOutgoing) {
            mTypeFilterOutgoing.setBackgroundResource(R.drawable.btn_calllog_all);
        } else {
            mTypeFilterOutgoing.setBackgroundResource(R.drawable.btn_calllog_all_sel);
        }

        if (view != mTypeFilterIncoming) {
            mTypeFilterIncoming.setBackgroundResource(R.drawable.btn_calllog_all);
        } else {
            mTypeFilterIncoming.setBackgroundResource(R.drawable.btn_calllog_all_sel);
        }

        if (view != mTypeFilterMissed) {
            mTypeFilterMissed.setBackgroundResource(R.drawable.btn_calllog_all);
        } else {
            mTypeFilterMissed.setBackgroundResource(R.drawable.btn_calllog_all_sel);
        }
    }

    private void setAutoRejectedModeVisibility(int visibility) {
        if (View.VISIBLE == visibility) {
            mLayoutSearchbutton.setVisibility(View.GONE);
            mLayoutAutorejected.setVisibility(View.VISIBLE);
        } else {
            mLayoutAutorejected.setVisibility(View.GONE);
            mLayoutSearchbutton.setVisibility(View.VISIBLE);
        }
    }

    public void onClick(View view) {
        int id = view.getId();
        log("onClick(), view id = " + id);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                this.getActivity()).edit();
        //clear the selected position when tab change
        if (id == R.id.btn_type_filter_all || id == R.id.btn_type_filter_outgoing || id == R.id.btn_type_filter_incoming
            || id == R.id.btn_type_filter_missed) {
          if (mOldItemView != null) {
            //mOldItemView.setBackgroundColor(Color.TRANSPARENT);
            mOldItemView.getSelectImageView().setVisibility(View.GONE);
            mAdapter.setSelectedPosition(-1);
            mOldItemView = null;
          }
        
      }
        switch (id) {
            case R.id.btn_type_filter_all:
                editor.putInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_ALL);
                changeButton(view);
                break;
            case R.id.btn_type_filter_outgoing:
                editor.putInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_OUTGOING);
                changeButton(view);
                break;
            case R.id.btn_type_filter_incoming:
                editor.putInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_INCOMING);
                changeButton(view);
                break;
            case R.id.btn_type_filter_missed:
                editor.putInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_MISSED);
                changeButton(view);
                break;
            default:
                break;
        }
        editor.commit();
        mRefreshDataRequired = true;
        refreshData();
    }

    public void showChoiceResourceDialog() {
        final Resources res = getActivity().getResources();
        final String title = res.getString(R.string.choose_resources_header);

        final DialogInterface.OnClickListener clickListener = 
                            new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                int oriSim = prefs.getInt(Constants.SIM_FILTER_PREF, -1);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                        getActivity()).edit();

                AlertDialog alertDialog = (AlertDialog) dialog;
                Object obj = alertDialog.getListView().getAdapter().getItem(which);

                Log.i(TAG, "showChoiceResourceDialog OnClick oriSim:" + oriSim + " return:" + obj);

                int resId = 0;
                if (obj instanceof String) {
                    resId = R.string.all_resources;
                } else if (obj instanceof Integer) {
                    if ((Integer) obj == Integer
                            .valueOf((int) Settings.System.VOICE_CALL_SIM_SETTING_INTERNET)) {
                        resId = R.string.call_sipcall;
                    } else if (SlotUtils.isSlotValid((Integer) obj)) {
                        resId = SlotUtils.getResIdForSlot((Integer) obj);
                    } else {
                        Log.e(TAG, "OnClick Error! return:" + (Integer) obj);
                    }
                }

                long newsimid = (long) Settings.System.DEFAULT_SIM_NOT_SET;

                ///M: [Gemini+] check whether the res id belongs to sims and
                // process sim resource filtering @{
                int slotId = SlotUtils.getSlotIdFromSimResId(resId);
                if (SlotUtils.isSlotValid(slotId)) {
                    int simId = CallLogSimInfoHelper.getSimIdBySlotID(slotId);
                    if (oriSim == simId) {
                        Log.d(TAG, "The current sim " + simId);
                        return;
                    }
                    editor.putInt(Constants.SIM_FILTER_PREF, (int) simId);
                    newsimid = simId;
                ///@}
                } else {
                    Log.d(TAG, "current res is not sim");
                    switch (resId) {
                        case R.string.all_resources:
                            if (oriSim == Constants.FILTER_ALL_RESOURCES) {
                                Log.d(TAG, "The current sim " + Constants.FILTER_ALL_RESOURCES);
                                return;
                            }
                            editor.putInt(Constants.SIM_FILTER_PREF, Constants.FILTER_ALL_RESOURCES);
                            newsimid = Constants.FILTER_ALL_RESOURCES;
                            break;
                        case R.string.call_sipcall:
                            if (oriSim == Constants.FILTER_SIP_CALL) {
                                Log.d(TAG, "The current sim " + Constants.FILTER_SIP_CALL);
                                return;
                            }
                            editor.putInt(Constants.SIM_FILTER_PREF, Constants.FILTER_SIP_CALL);
                            newsimid = Constants.FILTER_SIP_CALL;
                            break;
                        default: {
                            Log.e(TAG, "Unexpected resource: "
                                    + getResources().getResourceEntryName(resId));
                        }
                    }
                }
                Log.e(TAG, "showChoiceResourceDialog OnClick user selected:" + newsimid);
                editor.commit();
                mRefreshDataRequired = true;
                refreshData();
            }
        };
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this
                .getActivity());
        int choiceItem = preference.getInt(Constants.SIM_FILTER_PREF,
                Constants.FILTER_ALL_RESOURCES);
        log("showChoiceResourceDialog() choiceItem " + choiceItem);
        mSelectResDialog = SimPickerDialog.createSingleChoice(getActivity(), title, choiceItem,
                clickListener);
        mSelectResDialog.show();
    }

    private void log(final String log) {
        Log.i(TAG, log);
    }

    public boolean isAutoRejectedFilterMode() {
        return (FLAG_FILTER_MODE_AUTO_REJECTED == mModeFlag);
    }

    public void onBackHandled() {
        Log.i(TAG, "onBackHandled() Mode:" + mModeFlag + " View:" + mViewRestored);
        if (isAutoRejectedFilterMode()) {
            if (null == mViewRestored) {
                mViewRestored = mTypeFilterAll;
            }
            onClick(mViewRestored);
        }
    }
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        int count = l.getAdapter().getCount();
        if ((null == v) || (!(v instanceof CallLogListItemView))) {
            new Exception("CallLogFragment exception").printStackTrace();
            return;
        }
        CallLogListItemView itemView = (CallLogListItemView) v;
        Context context = this.getActivity();
        Log.i(TAG,"context is " + context);
        IntentProvider intentProvider = (IntentProvider) v.getTag();
      if (intentProvider != null) {
            Intent  in = intentProvider.getIntent(context);
            in.putExtra(Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, true);
            if (ISTABLET_LAND) {
              if (itemView != null) {
                    // if itemView and click item is not the same item,change their background
                if (mAdapter.getSelectedPosition() != itemView.getTagId()) {
                  mAdapter.itemSetSelect(itemView,mOldItemView);
                }
                mOldItemView = itemView;
              }             
                updateData(getCallLogEntryUris(in));
            } else {
                context.startActivity(intentProvider.getIntent(context)
                            .putExtra(Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, true));
             }
          
      } 
      mAdapter.setSelectedPosition(itemView.getTagId()); 
    }
    // set old item tag ,this method is used by adapter
    public void setOldItemView(CallLogListItemView oldItemView) {
      this.mOldItemView = oldItemView;
    mAdapter.setSelectedPosition(oldItemView.getTagId());
  }
    
    /** @ } */
    /**
     * Returns the list of URIs to show.
     * <p>
     * There are two ways the URIs can be provided to the activity: as the data on the intent, or as
     * a list of ids in the call log added as an extra on the URI.
     * <p>
     * If both are available, the data on the intent takes precedence.
     */
    private Uri[] getCallLogEntryUris(Intent inten) {

        Uri uri = inten.getData();
        if (uri != null) {
            // If there is a data on the intent, it takes precedence over the extra.
            /** M: add @ { */
          Uri queryUri = Uri.parse("content://call_log/callsjoindataview");
          long id = ContentUris.parseId(uri);
          uri = ContentUris.withAppendedId(queryUri, id);
            /** @ } */
            return new Uri[]{ uri };
        }       
        long[] ids = inten.getLongArrayExtra(EXTRA_CALL_LOG_IDS);
        Uri[] uris = new Uri[ids.length];
        for (int index = 0; index < ids.length; ++index) {
            /** M: modify @ { */
           // uris[index] = ContentUris.withAppendedId(Calls.CONTENT_URI_WITH_VOICEMAIL, ids[index]);
          Uri queryUri = Uri.parse("content://call_log/callsjoindataview");
          uris[index] = ContentUris.withAppendedId(queryUri, ids[index]);
          /** @ }*/
        }
        return uris;
    }
    
    
    /**
     * Update user interface with details of given call.
     *
     * @param callUris URIs into {@link CallLog.Calls} of the calls to be displayed
     */
    private void updateData(final Uri... callUris) {
        class UpdateContactDetailsTask extends AsyncTask<Void, Void, PhoneCallDetails[]> {
            @Override
            public PhoneCallDetails[] doInBackground(Void... params) {
                // TODO: All phone calls correspond to the same person, so we can make a single
                // lookup.
                final int numCalls = callUris.length;
                PhoneCallDetails[] details = new PhoneCallDetails[numCalls];
                try {
                    for (int index = 0; index < numCalls; ++index) {
                        details[index] = getPhoneCallDetailsForUri(callUris[index]);
                    }
                    return details;
                } catch (IllegalArgumentException e) {
                    // Something went wrong reading in our primary data.
                    Log.w(TAG, "invalid URI starting call details", e);
                    return null;
                }
            }

            @Override
            public void onPostExecute(PhoneCallDetails[] details) {
              try {
                if (details == null) {
                    return;
                }

                // We know that all calls are from the same number and the same contact, so pick the
                // first.
                PhoneCallDetails firstDetails = details[0];
                mNumber = firstDetails.number.toString();
                final Uri contactUri = firstDetails.contactUri;
                final Uri photoUri = firstDetails.photoUri;

                // Set the details header, based on the first phone call.
                mPhoneCallDetailsHelper.setCallDetailsHeader(mHeaderTextView, firstDetails);

                // Cache the details about the phone number.
                /** M: add @ { */
                final Uri numberCallUri = mPhoneNumberHelper
                        .getCallUri(mNumber, firstDetails.simId);
                /** @ }*/
                final boolean canPlaceCallsTo = mPhoneNumberHelper.canPlaceCallsTo(mNumber);
                /** M: modify @ { */
                //final boolean isVoicemailNumber = mPhoneNumberHelper.isVoicemailNumber(mNumber);
                final boolean isVoicemailNumber = false;
                /** @ }*/
                final boolean isSipNumber = mPhoneNumberHelper.isSipNumber(mNumber);

                // Let user view contact details if they exist, otherwise add option to create new
                // contact from this number.
                final Intent mainActionIntent;
                final int mainActionIcon;
                final String mainActionDescription;

                final CharSequence nameOrNumber;
                if (!TextUtils.isEmpty(firstDetails.name)) {
                    nameOrNumber = firstDetails.name;
                } else {
                    nameOrNumber = firstDetails.number;
                }

                if (contactUri != null) {
                    mainActionIntent = new Intent(Intent.ACTION_VIEW, contactUri);
                    // This will launch People's detail contact screen, so we probably want to
                    // treat it as a separate People task.
                    mainActionIntent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainActionIcon = R.drawable.ic_contacts_holo_dark;
                    mainActionDescription =
                            getString(R.string.description_view_contact, nameOrNumber);
                } else if (isVoicemailNumber) {
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                    /** M: delete @ { */
                    // } else if (isSipNumber) {
                    // TODO: This item is currently disabled for SIP addresses, because
                    // the Insert.PHONE extra only works correctly for PSTN numbers.
                    //
                    // To fix this for SIP addresses, we need to:
                    // - define ContactsContract.Intents.Insert.SIP_ADDRESS, and use it here if
                    //   the current number is a SIP address
                    // - update the contacts UI code to handle Insert.SIP_ADDRESS by
                    //   updating the SipAddress field
                    // and then we can remove the "!isSipNumber" check above.
                    // mainActionIntent = null;
                    // mainActionIcon = 0;
                    // mainActionDescription = null;
                    /** @ } */
                } else if (canPlaceCallsTo) {
                    mainActionIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    mainActionIntent.setType(Contacts.CONTENT_ITEM_TYPE);
                    /** M: modify @ { */                
                    // mainActionIntent.putExtra(Insert.PHONE, mNumber);
                    if (isSipNumber) {
                        mainActionIntent.putExtra(ContactsContract.Intents.Insert.SIP_ADDRESS,
                                                        mNumber);
                    } else {
                        mainActionIntent.putExtra(Insert.PHONE, mNumber);
                    }
                    /** @ } */
                    mainActionIcon = R.drawable.ic_add_contact_holo_dark;
                    mainActionDescription = getString(R.string.description_add_contact);
                } else {
                    // If we cannot call the number, when we probably cannot add it as a contact either.
                    // This is usually the case of private, unknown, or payphone numbers.
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                }

                if (mainActionIntent == null) {
                    mMainActionView.setVisibility(View.INVISIBLE);
                    mMainActionPushLayerView.setVisibility(View.GONE);
                    mHeaderTextView.setVisibility(View.INVISIBLE);
                    mHeaderOverlayView.setVisibility(View.INVISIBLE);
                } else {
                    mMainActionView.setVisibility(View.VISIBLE);
                    mMainActionView.setImageResource(mainActionIcon);
                    mMainActionPushLayerView.setVisibility(View.VISIBLE);
                    mMainActionPushLayerView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(mainActionIntent);
                        }
                    });
                    mMainActionPushLayerView.setContentDescription(mainActionDescription);
                    mHeaderTextView.setVisibility(View.VISIBLE);
                    mHeaderOverlayView.setVisibility(View.VISIBLE);
                }

                // This action allows to call the number that places the call.
                if (canPlaceCallsTo) {
                    final CharSequence displayNumber =
                            mPhoneNumberHelper.getDisplayNumber(
                                    firstDetails.number, firstDetails.formattedNumber);
                    /** M: modify @ { */
                    // ViewEntry entry = new ViewEntry(
                     //       getString(R.string.menu_callNumber,
                    //                FormatUtils.forceLeftToRight(displayNumber)),
                     //               ContactsUtils.getCallIntent(mNumber),
                     //               getString(R.string.description_call, nameOrNumber));
                    boolean isVoicemailUri = PhoneNumberHelper.isVoicemailUri(numberCallUri);
                    int slotId = SIMInfoWrapper.getDefault().getSimSlotById((int) firstDetails.simId);
                    Intent callIntent = ContactsUtils.getCallIntent(mNumber).putExtra(
                            Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId);
                    callIntent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER);
                    if (isVoicemailUri && slotId != -1) {
                        callIntent.putExtra("simId", slotId);
                    }
                    ViewEntry entry = new ViewEntry(getString(R.string.menu_callNumber, FormatUtils
                            .forceLeftToRight(displayNumber)), callIntent, getString(
                            R.string.description_call, nameOrNumber));
                    /** @ } */
                    
                    // Only show a label if the number is shown and it is not a SIP address.
                    if (!TextUtils.isEmpty(firstDetails.name)
                            && !TextUtils.isEmpty(firstDetails.number)
                            && !PhoneNumberUtils.isUriNumber(firstDetails.number.toString())) {

                        /** M: add @ { */
                        if (0 == firstDetails.numberType) {
                            entry.label = mResources.getString(R.string.list_filter_custom);
                        } else {
                            /** @ } */
                            /** M:AAS @ { original code: 
                             entry.label = Phone.getTypeLabel(mResources, firstDetails.numberType, 
                                    firstDetails.numberLabel); */
                                entry.label = ExtensionManager.getInstance()
                                        .getContactAccountExtension().getTypeLabel(
                                                mResources, firstDetails.numberType,
                                                firstDetails.numberLabel, slotId,
                                                ExtensionManager.COMMD_FOR_AAS);
                            /** @ } */
                        }
                    }

                    // The secondary action allows to send an SMS to the number that placed the
                    // call.
                    /** M: modify @ { */
                    //if (mPhoneNumberHelper.canSendSmsTo(mNumber)) {
                    if (mPhoneNumberHelper.canSendSmsTo(mNumber) && mHasSms) {
                        entry.setSecondaryAction(
                                R.drawable.ic_text_holo_dark,
                                new Intent(Intent.ACTION_SENDTO,
                                           Uri.fromParts("sms", mNumber, null)),
                                getString(R.string.description_send_text_message, nameOrNumber));
                    }

                    /** M: add @ { */
                    // for sim name
                    setSimInfo(firstDetails.simId);
 
                    // For Video Call.
                    if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                        Intent itThird = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberCallUri)
                                .putExtra(Constants.EXTRA_IS_VIDEO_CALL, true)
                                .putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId);
                        itThird.setClassName(Constants.PHONE_PACKAGE,
                                Constants.OUTGOING_CALL_BROADCASTER);
                        if (isVoicemailUri && slotId != -1) {
                            itThird.putExtra("simId", slotId);
                        }
                        entry.setThirdAction(getString(R.string.menu_videocallNumber, FormatUtils
                                .forceLeftToRight(displayNumber)), itThird, getString(
                                R.string.description_call, nameOrNumber));
                    } else {
                        entry.thirdIntent = null;
                    }

                    if (SIMInfoWrapper.getDefault().getInsertedSimCount() != 0) {
                        Intent itFourth = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberCallUri)
                            .putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId)
                            .putExtra(Constants.EXTRA_IS_IP_DIAL, true)
                            .putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId);
                        itFourth.setClassName(Constants.PHONE_PACKAGE,
                                Constants.OUTGOING_CALL_BROADCASTER);
                    if (isVoicemailUri && slotId != -1) {
                        itFourth.putExtra("simId", slotId);
                    }       
                    entry.setFourthAction(getString(R.string.menu_ipcallNumber, FormatUtils
                            .forceLeftToRight(displayNumber)), itFourth, getString(
                                    R.string.description_call, nameOrNumber));
                    } else {
                        entry.fourthIntent = null;
                    }

                    entry.geocode = firstDetails.geocode;
                    /** @ }*/

                    configureCallButton(entry);
                    mPhoneNumberToCopy = displayNumber;
                    mPhoneNumberLabelToCopy = entry.label;
                    /** M: add RCS @ { */
                    String name = null;
                    String number = null;
                    if (null != firstDetails) {
                        number = firstDetails.number.toString();
                        if (null != firstDetails.name) {
                            name = firstDetails.name.toString();
                        }
                    }
                    Log.i(TAG, "updateData name, number : " + name + " , " + number);
                    ExtensionManager.getInstance().getCallDetailExtension().setViewVisibleByActivity(
                            getActivity(), name, number, R.id.RCS_container,
                            R.id.separator03, R.id.RCS, R.id.RCS_action, R.id.RCS_text,
                            R.id.RCS_icon, R.id.RCS_divider, ExtensionManager.COMMD_FOR_RCS);
                } else {
                    /** @ } */
                   // disableCallButton();
                    mPhoneNumberToCopy = null;
                    mPhoneNumberLabelToCopy = null;
                }

                /** M: modify @ { */
                // mHasEditNumberBeforeCallOption =
                //     canPlaceCallsTo && !isSipNumber && !isVoicemailNumber;
                // mHasTrashOption = hasVoicemail();
                // mHasRemoveFromCallLogOption = !hasVoicemail();
                mHasEditNumberBeforeCallOption = canPlaceCallsTo && !isSipNumber;
                /** @ } */
                getActivity().invalidateOptionsMenu();

                //ListView historyList = (ListView) findViewById(R.id.history);

                historyList.setAdapter(
                        new CallDetailHistoryAdapter(getActivity(), mInflater,
                                mCallTypeHelper, details, false, canPlaceCallsTo,
                                mControls));
                                
                                
                                

                BackScrollManager.bind(
                        new ScrollableHeader() {

                            @Override
                            public void setOffset(int offset) {
                                mControls.setY(-offset);
                            }

                            @Override
                            public int getMaximumScrollableHeaderOffset() {
                                // We can scroll the photo out, but we should keep the header if
                                // present.
                                if (mHeader.getVisibility() == View.VISIBLE) {
                                    return mPhoto.getHeight() - mHeader.getHeight();
                                } else {
                                    // If the header is not present, we should also scroll out the
                                    // separator line.
                                    return mPhoto.getHeight() + mSeparator.getHeight();
                                }
                            }
                        },
                        historyList);
                loadContactPhotos(photoUri);
                mCallDetail.setVisibility(View.VISIBLE);                 
                    if (!canPlaceCallsTo) {
                        mSimName.setVisibility(View.GONE);
                        mSeparator.setVisibility(View.GONE);
                        mSeparator02.setVisibility(View.GONE);
                        mConvertView2.setVisibility(View.GONE);
                     } else {
                         mSimName.setVisibility(View.VISIBLE);
                         mSeparator.setVisibility(View.VISIBLE);
                         mSeparator02.setVisibility(View.VISIBLE);
                         mConvertView2.setVisibility(View.VISIBLE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
          }
        }
        mAsyncTaskExecutor.submit(Tasks.UPDATE_PHONE_CALL_DETAILS, new UpdateContactDetailsTask());
    }
    
    
    static final class ViewEntry {
        public final String text;
        public final Intent primaryIntent;
        /** The description for accessibility of the primary action. */
        public final String primaryDescription;

        public CharSequence label = null;
        /** Icon for the secondary action. */
        public int secondaryIcon = 0;
        /** Intent for the secondary action. If not null, an icon must be defined. */
        public Intent secondaryIntent = null;
        /** The description for accessibility of the secondary action. */
        public String secondaryDescription = null;

        public ViewEntry(String text, Intent intent, String description) {
            this.text = text;
            primaryIntent = intent;
            primaryDescription = description;
        }

        public void setSecondaryAction(int icon, Intent intent, String description) {
            secondaryIcon = icon;
            secondaryIntent = intent;
            secondaryDescription = description;
        }
        
        /** M: add @ { */
        public CharSequence geocode;
        
        /**
         * Intent for the third action-vtCall. If not null, an icon must be
         * defined.
         */
        public Intent thirdIntent;
        public String thirdDescription;
        public String videoText;
        
        public Intent fourthIntent;
        public String fourthDescription;
        public String ipText;
        /** The description for accessibility of the third action. */

        public void setThirdAction(String text, Intent intent,
                String description) {
            videoText = text;
            thirdIntent = intent;
            thirdDescription = description;
        }

        /** The description for accessibility of the fourth action. */

        public void setFourthAction(String text, Intent intent,
                String description) {
            ipText = text;
            fourthIntent = intent;
            fourthDescription = description;
        }

        /** @ }*/
    }
    
    
    private void setSimInfo(int simId) {
        //if (FeatureOption.MTK_GEMINI_SUPPORT) {
        int rPadding = this.getResources().getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_right);
        int lPadding = this.getResources().getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_left);
        int tbPadding = 1; //top and bottom padding
        mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
        if (simId == ContactsUtils.CALL_TYPE_SIP) {
            mSimName.setBackgroundResource(R.drawable.sim_dark_internet_call);
            mSimName.setText(R.string.call_sipcall);
            mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
        } else if (ContactsUtils.CALL_TYPE_NONE == simId) {
            mSimName.setVisibility(View.INVISIBLE);
        } else {
            String simName = SIMInfoWrapper.getDefault().getSimDisplayNameById(simId);
            if (null != simName) {
                mSimName.setText(simName);
                mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
            } else {
                mSimName.setVisibility(View.INVISIBLE);
            }
            int color = SIMInfoWrapper.getDefault().getInsertedSimColorById(simId);
            int simColorResId = SIMInfoWrapper.getDefault().getSimBackgroundDarkResByColorId(color);
            if (-1 != color) {
                mSimName.setBackgroundResource(simColorResId);
                mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
            } else {
                mSimName.setBackgroundResource(R.drawable.sim_dark_not_activated);
                mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
            }
        }
    /*} else {
        mSimName.setVisibility(View.GONE);
    }*/
}
    
    /** Configures the call button area using the given entry. */
    private void configureCallButton(ViewEntry entry) {
        convertView.setVisibility(View.VISIBLE);
        mainAction.setOnClickListener(mPrimaryActionListener);
        mainAction.setTag(entry);
        mainAction.setContentDescription(entry.primaryDescription);
        mainAction.setOnLongClickListener(mPrimaryLongClickListener);
        if (entry.secondaryIntent != null) {
            icon.setOnClickListener(mSecondaryActionListener);
            icon.setImageResource(entry.secondaryIcon);
            icon.setVisibility(View.VISIBLE);
            icon.setTag(entry);
            icon.setContentDescription(entry.secondaryDescription);
            divider.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }
        text.setText(entry.text);
        /** M: add @ { */
        text.setPadding(this.getResources().getDimensionPixelSize(R.dimen.call_log_indent_margin), 0, 0, 0);

        TextView label = (TextView) convertView.findViewById(R.id.call_and_sms_label);
        if (TextUtils.isEmpty(entry.label)) {
            label.setVisibility(View.GONE);
        } else {
            label.setText(entry.label);
            label.setVisibility(View.VISIBLE);
            /** M: add @ { */
          label.setPadding(0, 0, this.getResources().getDimensionPixelSize(
                    R.dimen.call_log_inner_margin), 0);
            /** @ } */
        }
        /** M: add @ { */
        //For video call 
        TextView geocode = (TextView) convertView.findViewById(R.id.call_number_geocode);
        View labelAndgeocodeView = (View) convertView.findViewById(R.id.labe_and_geocode_text);
        labelAndgeocodeView.setPadding(this.getResources().getDimensionPixelSize(R.dimen.call_log_indent_margin), 0, 0, 0);

        if (FeatureOption.MTK_PHONE_NUMBER_GEODESCRIPTION) {

            if (TextUtils.isEmpty(entry.geocode)) {
                geocode.setVisibility(View.GONE);
            } else {
                geocode.setText(entry.geocode);
                geocode.setVisibility(View.VISIBLE);
            }

            if (TextUtils.isEmpty(entry.label) && TextUtils.isEmpty(entry.geocode)) {
                labelAndgeocodeView.setVisibility(View.GONE);
            } else {
                labelAndgeocodeView.setVisibility(View.VISIBLE);
            }
        } else {
            geocode.setVisibility(View.GONE);

            if (TextUtils.isEmpty(entry.label)) {
                labelAndgeocodeView.setVisibility(View.GONE);
            } else {
                labelAndgeocodeView.setVisibility(View.VISIBLE);
            }
        }
        
        mSeparator01.setVisibility(View.VISIBLE);
       
        mSeparator02.setVisibility(View.VISIBLE);

        View videoAction = mConvertView1.findViewById(R.id.video_call_action);
        if (entry.thirdIntent != null) {
          
            videoAction.setOnClickListener(mThirdActionListener);
            videoAction.setTag(entry);
            videoAction.setContentDescription(entry.thirdDescription);
            videoAction.setVisibility(View.VISIBLE);
            TextView videoText = (TextView) mConvertView1.findViewById(R.id.video_call_text);

            videoText.setText(entry.videoText);

            TextView videoLabel = (TextView) mConvertView1.findViewById(R.id.video_call_label);
            if (TextUtils.isEmpty(entry.label)) {
                videoLabel.setVisibility(View.GONE);
            } else {
                videoLabel.setText(entry.label);
                videoLabel.setVisibility(View.VISIBLE);
            }
            mSeparator01.setVisibility(View.VISIBLE);
            mConvertView1.setVisibility(View.VISIBLE);
        } else {
            mSeparator01.setVisibility(View.GONE);
            mConvertView1.setVisibility(View.GONE);
        }
        
        //For IP call
        //
        if (entry.fourthIntent != null) {
        View ipAction = mConvertView2.findViewById(R.id.ip_call_action);
        ipAction.setOnClickListener(mFourthActionListener);
        ipAction.setTag(entry);
        ipAction.setContentDescription(entry.fourthDescription);
        TextView ipText = (TextView) mConvertView2.findViewById(R.id.ip_call_text);
        ipText.setText(entry.ipText);
        TextView ipLabel = (TextView) mConvertView2.findViewById(R.id.ip_call_label);
        if (TextUtils.isEmpty(entry.label)) {
            ipLabel.setVisibility(View.GONE);
        } else {
            ipLabel.setText(entry.label);
            ipLabel.setVisibility(View.VISIBLE);
            }
        } else {
            mSeparator02.setVisibility(View.GONE);
            mConvertView2.setVisibility(View.GONE);
        }
        // 
        /** @ } */
    }

    private final View.OnClickListener mPrimaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (finishPhoneNumerSelectedActionModeIfShown()) {
                return;
            }
            startActivity(((ViewEntry) view.getTag()).primaryIntent
                    /** M: add @ { */
                    .putExtra(Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, true));
        }
    };
    
    /** Load the contact photos and places them in the corresponding views. */
    private void loadContactPhotos(Uri photoUri) {
        /** M:  modify @ { */
        /**
         * 
        mContactPhotoManager.loadPhoto(mContactBackgroundView, photoUri,
                mContactBackgroundView.getWidth(), true);
         */
        if (photoUri == null) {
            mContactBackgroundView.setImageResource(R.drawable.ic_contact_picture_180_holo_dark);
        } else {
            mContactPhotoManager.loadPhoto(mContactBackgroundView, photoUri, mContactBackgroundView.getWidth(), true);
        }
        /** @ }*/
    }
    
    private final View.OnLongClickListener mPrimaryLongClickListener =
            new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (finishPhoneNumerSelectedActionModeIfShown()) {
                return true;
            }
            startPhoneNumberSelectedActionMode(v);
            return true;
        }
    };
    
    private final View.OnClickListener mSecondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (finishPhoneNumerSelectedActionModeIfShown()) {
                return;
            }
            startActivity(((ViewEntry) view.getTag()).secondaryIntent);
        }
    };
    
  //VT Call
    private final View.OnClickListener mThirdActionListener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivity(((ViewEntry) view.getTag()).thirdIntent);
      }
    };
    
     //IP Call
    private final View.OnClickListener mFourthActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(((ViewEntry) view.getTag()).fourthIntent);
        }
    };
    
    /**
     * If the phone number is selected, unselect it and return {@code true}.
     * Otherwise, just {@code false}.
     */
    private boolean finishPhoneNumerSelectedActionModeIfShown() {
        if (mPhoneNumberActionMode == null) {
            return false;
        }
        mPhoneNumberActionMode.finish();
        return true;
    }
    private void startPhoneNumberSelectedActionMode(View targetView) {
        mPhoneNumberActionMode = getActivity().startActionMode(new PhoneNumberActionModeCallback(targetView));
    }
    
    
    private class PhoneNumberActionModeCallback implements ActionMode.Callback {
        private final View mTargetView;
        private final Drawable mOriginalViewBackground;

        public PhoneNumberActionModeCallback(View targetView) {
            mTargetView = targetView;

            // Highlight the phone number view.  Remember the old background, and put a new one.
            mOriginalViewBackground = mTargetView.getBackground();
            mTargetView.setBackgroundColor(getResources().getColor(R.color.item_selected));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (TextUtils.isEmpty(mPhoneNumberToCopy)) {
                return false;
            }

            getActivity().getMenuInflater().inflate(R.menu.call_details_cab, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.copy_phone_number:
                    ClipboardUtils.copyText(getActivity(), mPhoneNumberLabelToCopy,
                            mPhoneNumberToCopy, true);
                    mode.finish(); // Close the CAB
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mPhoneNumberActionMode = null;

            // Restore the view background.
            mTargetView.setBackground(mOriginalViewBackground);
        }
    }

    /** Return the phone call details for a given call log URI. */
    private PhoneCallDetails getPhoneCallDetailsForUri(Uri callUri) {

        // ContentResolver resolver = getActivity().getContentResolver();
        if (mContext == null) {
            throw new IllegalArgumentException("Cannot find mContext");
        }
       ContentResolver resolver = mContext.getContentResolver();

        /** M: modify @ { */
        //Cursor callCursor = resolver.query(callUri, CALL_LOG_PROJECTION, null, null, null);
        Cursor callCursor = resolver.query(callUri, CallLogQuery.PROJECTION_CALLS_JOIN_DATAVIEW, null, null, null);
        try {
            if (callCursor == null || !callCursor.moveToFirst()) {
                throw new IllegalArgumentException("Cannot find content: " + callUri);
            }

            
            ContactInfo contactInfo = ContactInfo.fromCursor(callCursor);
            String photo = callCursor.getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_PHOTO_URI);
            Uri photoUri = null;
            if (null != photo) {
                photoUri = Uri.parse(photo);
            } else {
                photoUri = null;
            }
            log("number = " + contactInfo.number);
            if (!mPhoneNumberHelper.canPlaceCallsTo(contactInfo.number)
                    || mPhoneNumberHelper.isVoiceMailNumberForMtk(contactInfo.number,
                            contactInfo.simId)
                    || mPhoneNumberHelper.isEmergencyNumber(contactInfo.number)) {
                contactInfo.formattedNumber = mPhoneNumberHelper.getDisplayNumber(
                        contactInfo.number, null).toString();
                contactInfo.name = "";
                contactInfo.nNumberTypeId = 0;
                contactInfo.label = "";
                photoUri = null;
                contactInfo.lookupUri = null;
            }
            
            return new PhoneCallDetails(contactInfo.number, contactInfo.formattedNumber, 
                    contactInfo.countryIso, contactInfo.geocode,
                    contactInfo.type, contactInfo.date,
                    contactInfo.duration, contactInfo.name,
                    contactInfo.nNumberTypeId, contactInfo.label,
                    contactInfo.lookupUri, photoUri, contactInfo.simId,
                    contactInfo.vtCall, 0, contactInfo.ipPrefix);
            /** @ }*/
        } finally {
            if (callCursor != null) {
                callCursor.close();
            }
        }
    }

    /**
     * Update wait cursor description
     * 
     * @param providerStatus
     */
    public void updateProviderStauts(ProviderStatusWatcher.Status providerStatus) {
        if (null == providerStatus || null == mHandler) {
            Log.e(TAG, "updateProviderStauts Error! providerStatus:" + providerStatus);
            return;
        }
        int msgWhat = -1;
        switch (providerStatus.status) {
            case ProviderStatus.STATUS_CHANGING_LOCALE:
                msgWhat = PROVIDER_STATUS_CHANGING_LOCALE;
                break;

            case ProviderStatus.STATUS_UPGRADING:
                msgWhat = PROVIDER_STATUS_UPGRATING;
                break;

            default:
                Log.i(TAG, "updateProviderStauts needn't handle msg:" + providerStatus.status);
                break;
        }
        Log.i(TAG, "updateProviderStauts status:" + providerStatus.status + " msgWhat:" + msgWhat);
        if (-1 != msgWhat) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(msgWhat), WAIT_CURSOR_DELAY_TIME);
        }
    }
}
