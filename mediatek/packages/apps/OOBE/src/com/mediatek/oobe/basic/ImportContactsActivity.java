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

package com.mediatek.oobe.basic;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.oobe.R;
import com.mediatek.oobe.basic.gemini.SimInfoPreference;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.oobe.utils.OOBEStepPreferenceActivity;
import com.mediatek.oobe.utils.Utils;
import com.mediatek.telephony.PhoneNumberFormatUtilEx;
import com.mediatek.xlog.Xlog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportContactsActivity extends OOBEStepPreferenceActivity implements Button.OnClickListener {
    private static final String TAG = OOBEConstants.TAG;

    private static final int SLOT_INDEX_1 = 0;
    private static final int SLOT_INDEX_2 = 1;
    private static final int SLOT_INDEX_3 = 2;
    private static final int SLOT_INDEX_4 = 3;
    private static final int SLOT_TOTAL_NUMBER = com.android.internal.telephony.PhoneConstants.GEMINI_SIM_NUM;
    private static final int ID_IN_COPY = 0;
    private static final int ID_END_COPY = 1;
    private static final int ID_CANCEL_COPY = 2;

    private static final int SIM_STATE_NORMAL = 1;
    private static final int SIM_STATE_LOCK = 2;
    private static final int SIM_STATE_ABSENT = 3;

    private static final int UI_START_QUERY = 1;
    private static final int UI_FINISH_QUERY = 2;
    private static final int UI_START_COPY = 2;
    private static final int UI_FNISH_COPY = 3;
    private static final int UI_CANCEL_COPY = 4;

    private static final int QUERY_TOKEN = 1;
    private static final String CLAUSE_ONLY_VISIBLE = Contacts.IN_VISIBLE_GROUP + "=1";

    private static final int SUMMARY_ID_COLUMN_INDEX = 0;
    private static final int SUMMARY_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
    private static final int SUMMARY_DISPLAY_NAME_ALTERNATIVE_COLUMN_INDEX = 2;
    private static final int SUMMARY_SORT_KEY_PRIMARY_COLUMN_INDEX = 3;
    private static final int SUMMARY_STARRED_COLUMN_INDEX = 4;
    private static final int SUMMARY_TIMES_CONTACTED_COLUMN_INDEX = 5;
    private static final int SUMMARY_PRESENCE_STATUS_COLUMN_INDEX = 6;
    private static final int SUMMARY_PHOTO_ID_COLUMN_INDEX = 7;
    private static final int SUMMARY_LOOKUP_KEY_COLUMN_INDEX = 8;
    private static final int SUMMARY_PHONETIC_NAME_COLUMN_INDEX = 9;
    private static final int SUMMARY_HAS_PHONE_COLUMN_INDEX = 10;
    private static final int SUMMARY_SNIPPET_MIMETYPE_COLUMN_INDEX = 11;
    private static final int SUMMARY_SNIPPET_DATA1_COLUMN_INDEX = 12;
    private static final int SUMMARY_SNIPPET_DATA4_COLUMN_INDEX = 13;
    private static final int COPYING_DIALOG_ID = 101;

    private final String[] mContactsProjection = new String[] { 
            Contacts._ID, // 0
            Contacts.DISPLAY_NAME_PRIMARY, // 1
            Contacts.DISPLAY_NAME_ALTERNATIVE, // 2
            Contacts.SORT_KEY_PRIMARY, // 3
            Contacts.STARRED, // 4
            Contacts.TIMES_CONTACTED, // 5
            Contacts.CONTACT_PRESENCE, // 6
            Contacts.PHOTO_ID, // 7
            Contacts.LOOKUP_KEY, // 8
            Contacts.PHONETIC_NAME, // 9
            Contacts.HAS_PHONE_NUMBER, // 10
            Contacts.INDICATE_PHONE_SIM, // 11
    };

    /**
     * slot status & infomation
     * 
     * @author mtk54279
     * 
     */
    public static class SIMSlotStatus {
        public boolean mCopyFinished;
        public boolean mIsCopying;
        public int mSuccessCount;
        public int mTotalCount;
        public boolean mIsSelected;
        public boolean mQuerySimDone;
        public boolean mIsSrcSimUSIM;

        /**
         * constructor
         */
        public SIMSlotStatus() {
            // init variable
            mCopyFinished = false;
            mIsCopying = false;
            mSuccessCount = 0;
            mTotalCount = 0;
            mIsSelected = false;
            mQuerySimDone = false;
            mIsSrcSimUSIM = false;
        }
    }

    static final int MESSAGE_TIME_OUT = 1;
    static final String TAG_IMPORT = "import";
    static final String TAG_CANCEL = "cancel";

    private ListView mListView;
    private SimInfoPreference mSlot1SimPref;
    private SimInfoPreference mSlot2SimPref;
    private SimInfoPreference mSlot3SimPref;
    private SimInfoPreference mSlot4SimPref;
    private Map<Integer, SIMInfo> mMapSlot2SimInfo;
    private ITelephony mITelephony;
    private boolean mIsSlot1Insert = false;
    private boolean mIsSlot2Insert = false;

    private SIMSlotStatus[] mSimSlotStatus = new SIMSlotStatus[SLOT_TOTAL_NUMBER];
    private SimInfoPreference[] mSlotSimPref = new SimInfoPreference[SLOT_TOTAL_NUMBER];
    private Cursor[] mCursorQuerySims = new Cursor[SLOT_TOTAL_NUMBER];

    private Button mImportBtn;
    private TextView mTextCopyNote;
    private CopyThread mCopyThread;
    private boolean mBeingCopied = false;
    private boolean mBeingQuery = false;

    // If progress is changed by this UI, do not use call back
    private boolean mSelfChangeFlag = false;
    private boolean mProgressBeenInitFlag = false;
    private static boolean sFirstRunning = false;
    private static boolean sCopying = false;
    private static boolean sCanceled = false;

    private static final String SIM_TOTAL_CONTACTS = "sim_total";
    private static final String SIM_SUCCESS_COPYCOUNT = "sim_success_copycount";
    private static final String IMPORT_CONTACTS_STATE = "is_import_state";

    final ITelephony mTelephonyService = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
    private QueryHandler mQueryHandler;
    private Handler mCopyHandler = new CopyHandler();
    private Integer mResultSimLock = CellConnMgr.RESULT_UNKNOWN;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private static final int NOTIFICATION_ID = 0x00011;

    BroadcastReceiver mSimStateListener = new BroadcastReceiver(){
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); 
            Xlog.d(TAG, "ImportContactsActivity receive:" + action);
            if(action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)
                || action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)){
                updateSimPreference();
            } else if(action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)){
                disableSimPreference();
            }
        }
    };

    private static void setFirstRunning(boolean first) {
        sFirstRunning = first;
    }

    private Runnable mServiceComplete = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            mResultSimLock = mCellMgr.getResult();
            Xlog.d(TAG, "serviceComplete result: " + mResultSimLock);
        }
    };

    private Thread mServiceCompleteThread = new Thread(mServiceComplete);
    private CellConnMgr mCellMgr = new CellConnMgr(mServiceCompleteThread);

    private class ClockHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_TIME_OUT:
                Xlog.d("ClockHandler", "Time out");
                startQuery();
                break;
            default:
                break;
            }
        }
    }

    public static class NamePhoneTypePair {
        public String mName;
        public int mPhoneType;
        public String mPhoneTypeSuffix;

        /**
         * contructor
         * @param nameWithPhoneType String
         */
        public NamePhoneTypePair(String nameWithPhoneType) {
            // Look for /W /H /M or /O at the end of the name signifying the
            // type
            int nameLen = nameWithPhoneType.length();
            if (nameLen - 2 >= 0 && nameWithPhoneType.charAt(nameLen - 2) == '/') {
                char c = Character.toUpperCase(nameWithPhoneType.charAt(nameLen - 1));
                mPhoneTypeSuffix = String.valueOf(nameWithPhoneType.charAt(nameLen - 1));
                if (c == 'W') {
                    mPhoneType = Phone.TYPE_WORK;
                } else if (c == 'M' || c == 'O') {
                    mPhoneType = Phone.TYPE_MOBILE;
                } else if (c == 'H') {
                    mPhoneType = Phone.TYPE_HOME;
                } else {
                    mPhoneType = Phone.TYPE_OTHER;
                }
                mName = nameWithPhoneType.substring(0, nameLen - 2);
            } else {
                mPhoneTypeSuffix = "";
                mPhoneType = Phone.TYPE_OTHER;
                mName = nameWithPhoneType;
            }
        }
    }

    /**
     * setListViewHeightBasedOnChildren
     * @param listView ListView
     */
    public void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = getPreferenceScreen().getRootAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0, len = listAdapter.getCount(); i < len; i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0); // Measure the height of sub items
            totalHeight += listItem.getMeasuredHeight() + 1; // the height of
                                                             // all sub items
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        Xlog.i(TAG,
                "Total height: " + totalHeight + " and height: " + params.height + " Divider Height is :"
                        + listView.getDividerHeight());
        listView.setLayoutParams(params);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Xlog.d(TAG, "OnCreate ImportContactsActivity, isFirstRunning?" + sFirstRunning);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_contacts_layout);
        addPreferencesFromResource(R.xml.oobe_preference_import_contacts);
        initSimSlotStatus();
        initLayout();
        initSpecialLayout();
        initSim();
        // register a callback function to refresh UI progress by SimInfoPreference
        SimInfoPreference.registerCallback(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
    }

    protected void initSpecialLayout() {
        super.initSpecialLayout(R.string.oobe_title_import_contacts, R.string.oobe_summary_import_contacts);
        // Init UI
        mImportBtn = (Button) findViewById(R.id.button_import_contacts);
        mTextCopyNote = (TextView) findViewById(R.id.textView_note_import_contacts);
        mImportBtn.setOnClickListener(this);
        mListView = (ListView) findViewById(android.R.id.list);
        mImportBtn.setTag(TAG_IMPORT);
    }

    /**
     * As many instance of ImportContactsActivity may exist, global variable is needed to sync status, reset these variables
     * when OOBE start
     */
    public void initSimSlotStatus() {
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            mSimSlotStatus[i] = new SIMSlotStatus();
        }

        setFirstRunning(true);
    }

    private void initSim() {
        mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));

        mMapSlot2SimInfo = new HashMap<Integer, SIMInfo>();
        mQueryHandler = new QueryHandler(this);
        mCellMgr.register(this);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        mCellMgr.unregister();
        SimInfoPreference.unregisterCallback(this);
        setFirstRunning(true);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        unregisterReceiver(mSimStateListener);
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Xlog.d(TAG, "onResume");
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            mSlotSimPref[i] = null;
        }

        // if (com.mediatek.featureoption.FeatureOption.MTK_GEMINI_SUPPORT) {
        if (SLOT_TOTAL_NUMBER > 1) {
            addGeminiSimInfoPreference();
        } else {
            addSingleSimPrefrence();
        }
        
        if (mMapSlot2SimInfo.size() > 0) {
            mImportBtn.setVisibility(View.VISIBLE);
        } else {
            mImportBtn.setVisibility(View.GONE);
        }
        
        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        registerReceiver(mSimStateListener, intentFilter);
        
        setListViewHeightBasedOnChildren(mListView);
        updateCopyNote();
    }

    private void updateCopyNote() {
        boolean isCopying = true;
        boolean isImported = true;
        boolean isFinished = true;
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            isCopying = isCopying && mSimSlotStatus[SLOT_INDEX_1].mIsCopying;
            isImported = isImported && (!mSimSlotStatus[i].mIsCopying || mSimSlotStatus[i].mCopyFinished);
            isFinished = isFinished && (mSlotSimPref[i] == null || mSimSlotStatus[i].mCopyFinished);
        }
        if (isCopying) {

            if (isImported) {
                // finish copy
                if (isFinished) {
                    mImportBtn.setVisibility(View.INVISIBLE);
                } else {
                    mImportBtn.setTag(TAG_IMPORT);
                    mImportBtn.setText(R.string.oobe_btn_start_import_contacts);
                }
                mTextCopyNote.setText(R.string.oobe_note_import_contacts_finish);
            } else if (!isFinished) {
                // be copying now
                mImportBtn.setTag(TAG_CANCEL);
                mImportBtn.setEnabled(true);
                mImportBtn.setText(R.string.oobe_btn_cancel_import_contacts);

                mTextCopyNote.setVisibility(View.VISIBLE);
                mTextCopyNote.setText(R.string.oobe_note_import_contacts_going);
            }

            if (mNextBtn != null) {
                mNextBtn.setText(R.string.oobe_btn_text_next);
            }
        } else {
            mNextBtn.setText(R.string.oobe_btn_text_skip);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:

            if (mBeingQuery || mBeingCopied) {
                Xlog.i(TAG, "Being queried or copied");
                return true;
            }
            // finishActivityByResultCode(OOBEConstants.RESULT_CODE_BACK);
            break;
        default:
            break;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        if (v == mImportBtn) {
            if (mImportBtn.getTag().toString().equalsIgnoreCase(TAG_IMPORT)) {
                Xlog.d(TAG, "onClick() importContacts()");
                mSelfChangeFlag = true;
                importContacts();
            } else {
                Xlog.d(TAG, "onClick() cancelImporting()");
                mSelfChangeFlag = true;
                cancelImporting();
            }
        } else {
            super.onClick(v);
        }
    }

    private void addGeminiSimInfoPreference() {
        mMapSlot2SimInfo.clear();
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);

        Xlog.i(TAG, "sim number is " + simList.size());
        for (SIMInfo siminfo : simList) {
            Xlog.i(TAG, "siminfo.mSlot " + Integer.valueOf(siminfo.mSlot));
            mMapSlot2SimInfo.put(Integer.valueOf(siminfo.mSlot), siminfo);
        }

        PreferenceScreen simPrefScreen = getPreferenceScreen();
        if (simPrefScreen == null) {
            Xlog.d(TAG, "simPrefScreen is null");
            return;
        }
        simPrefScreen.removeAll();

        for (int slotIndex : mMapSlot2SimInfo.keySet()) {
            SIMInfo siminfo = mMapSlot2SimInfo.get(slotIndex);

            if (siminfo == null) {
                Xlog.d(TAG, "siminfo is null");
                mSimSlotStatus[slotIndex].mIsSelected = false;
                break;
            }
            if (OOBEConstants.DEBUG) {
                Xlog.i(TAG, "siminfo.mDisplayName = " + siminfo.mDisplayName);
                Xlog.i(TAG, "siminfo.mNumber = " + siminfo.mNumber);
                Xlog.i(TAG, "siminfo.mSlot = " + siminfo.mSlot);
                Xlog.i(TAG, "siminfo.mColor = " + siminfo.mColor);
                Xlog.i(TAG, "siminfo.mNumber = " + siminfo.mNumber);
                Xlog.i(TAG, "siminfo.mDispalyNumberFormat = " + siminfo.mDispalyNumberFormat);
                Xlog.i(TAG, "siminfo.mSimId = " + siminfo.mSimId);
            }


            int status = -1;
            try {
                status = mITelephony.getSimIndicatorStateGemini(siminfo.mSlot);
            } catch (RemoteException exception) {
                Xlog.i(TAG, "RemoteException " + exception.getMessage());
            }
            SimInfoPreference simInfoPref = new SimInfoPreference(this, siminfo.mDisplayName, siminfo.mNumber,
                    siminfo.mSlot, status, siminfo.mColor, siminfo.mDispalyNumberFormat, siminfo.mSimId, true);

            if (sFirstRunning) {
                mSimSlotStatus[slotIndex].mIsSelected = true;
            }
            simInfoPref.setCheck(mSimSlotStatus[slotIndex].mIsSelected);
            simInfoPref.setImporting(mSimSlotStatus[slotIndex].mIsCopying);
            simInfoPref.setFinishImporting(mSimSlotStatus[slotIndex].mCopyFinished);

            Xlog.i(TAG, "simid status is  " + status);

            if (simInfoPref != null) {
                if (siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1) {
                    mIsSlot1Insert = true;
                    mSlot1SimPref = simInfoPref;
                    mSlotSimPref[SLOT_INDEX_1] = simInfoPref;
                    simPrefScreen.addPreference(mSlot1SimPref);
                } else if (siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2) {
                    mIsSlot2Insert = true;
                    mSlot2SimPref = simInfoPref;
                    mSlotSimPref[SLOT_INDEX_2] = simInfoPref;
                    simPrefScreen.addPreference(mSlot2SimPref);
                } else if (siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_3) {
                    mSlot3SimPref = simInfoPref;
                    mSlotSimPref[SLOT_INDEX_3] = simInfoPref;
                    simPrefScreen.addPreference(mSlot3SimPref);
                } else if (siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_4) {
                    mSlot4SimPref = simInfoPref;
                    mSlotSimPref[SLOT_INDEX_4] = simInfoPref;
                    simPrefScreen.addPreference(mSlot4SimPref);
                }
            }
        }
    }

    private void addSingleSimPrefrence() {
        PreferenceScreen simPrefScreen = getPreferenceScreen();
        if (simPrefScreen == null) {
            return;
        }
        simPrefScreen.removeAll();

        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
        if (simList.size() == 0) {
            return;
        }
        SIMInfo siminfo = simList.get(0);
        int status = -1;
        try {
            status = mITelephony.getSimIndicatorState();
        } catch (RemoteException exception) {
            Xlog.e(TAG, "RemoteException " + exception.getMessage());
        }
        SimInfoPreference simInfoPref = new SimInfoPreference(this, siminfo.mDisplayName, siminfo.mNumber,
                siminfo.mSlot, status, siminfo.mColor, siminfo.mDispalyNumberFormat, siminfo.mSimId, true);

        mMapSlot2SimInfo.put(Integer.valueOf(siminfo.mSlot), siminfo);

        if (sFirstRunning) {
            mSimSlotStatus[SLOT_INDEX_1].mIsSelected = true;
        }
        simInfoPref.setCheck(mSimSlotStatus[SLOT_INDEX_1].mIsSelected);
        simInfoPref.setImporting(mSimSlotStatus[SLOT_INDEX_1].mIsCopying);
        simInfoPref.setFinishImporting(mSimSlotStatus[SLOT_INDEX_1].mCopyFinished);

        mIsSlot1Insert = true;
        mSlot1SimPref = simInfoPref;
        mSlotSimPref[SLOT_INDEX_1] = simInfoPref;
        getPreferenceScreen().addPreference(mSlot1SimPref);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof SimInfoPreference) {
            int slotIndex = ((SimInfoPreference) preference).getSlotIndex();
            if (mSimSlotStatus[slotIndex].mCopyFinished) {
                Xlog.w(TAG, "Importing contact, SIM contact has already been imported, can not be unchecked, jump");
                return true;
            }
            if (mSimSlotStatus[slotIndex].mIsCopying) {
                Xlog.w(TAG, "Importing contact, SIM contact in SIM " + slotIndex
                        + " is being copy, can not be clicked, jump");
                return true;
            }
            if (((SimInfoPreference) preference).getCheck()) {
                ((SimInfoPreference) preference).setCheck(false);
                Xlog.i(TAG, "Slot " + slotIndex + " is unselected");
                if (slotIndex > -1 && slotIndex < SLOT_TOTAL_NUMBER) {
                    mSimSlotStatus[slotIndex].mIsSelected = false;
                }
            } else {
                ((SimInfoPreference) preference).setCheck(true);
                Xlog.i(TAG, "Slot " + slotIndex + " is selected");
                if (slotIndex > -1 && slotIndex < SLOT_TOTAL_NUMBER) {
                    mSimSlotStatus[slotIndex].mIsSelected = true;
                }
            }
        }
        return true;
    }

    private void initProgressBar() {
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            if (!mSimSlotStatus[i].mIsSelected) { // did not select this slot to import
                Xlog.w(TAG, "ImportContacts.initProgressBar(), slot " + i + " is not selected to import, jump");
                continue;
            }
            SimInfoPreference simInfoPref = mSlotSimPref[i];
            if (simInfoPref != null) {
                Xlog.w(TAG, "ImportContacts.initProgressBar(), slot=" + i
                        + ", mTotalCount=" + mSimSlotStatus[i].mTotalCount);
                simInfoPref.initProgressBar(mSimSlotStatus[i].mTotalCount);
            }
        }
        createProgressNotificationBar();
    }

    private void finishProgressBar(int simIndex) {
        mSimSlotStatus[simIndex].mCopyFinished = true;
        Xlog.v(TAG, getStepSpecialTag() + "finishProgressBar() for SIM " + simIndex + ", mFinishImporting="
                + mSlotSimPref[simIndex].isFinishImporting());
        mSlotSimPref[simIndex].finishProgressBar();
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Indicate which SIM's contact is importing
     * 
     * @param simIndex
     */
    private void incrementProgressTo(int simIndex, int newProgress) {
        Xlog.v(TAG, getStepSpecialTag() + "SIM " + simIndex + " incrementProgressTo " + newProgress);

        mSlotSimPref[simIndex].incrementProgressTo(newProgress);
        updateProgressNotification(newProgress);
    }

    private void dealWithCancel() {
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            if (!mSimSlotStatus[i].mIsSelected) { // did not select this slot to import
                Xlog.w(TAG, getStepSpecialTag() + "dealWithCancel(), slot " + i
                        + " is not selected, so no need to cancel, just jump");
                continue;
            }
            mSimSlotStatus[i].mCopyFinished = false;
            mSimSlotStatus[i].mIsCopying = false;
            SimInfoPreference simInfoPref = mSlotSimPref[i];
            if (simInfoPref != null) {
                Xlog.v(TAG, "dealWithCancel(), slot=" + i);
                simInfoPref.dealWithCancel();
            }
        }
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private void createProgressNotificationBar() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification(R.drawable.contacts_imp_prog_notification, getString(R.string.oobe_title_import_contacts),
                System.currentTimeMillis());

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.import_progress_notification);
        contentView.setImageViewResource(R.id.download_icon, R.drawable.contacts_imp_prog_statbar);
        contentView.setTextViewText(R.id.title, getString(R.string.oobe_title_import_contacts));

        mNotification.contentView = contentView;
        // mNotification.contentView.setProgressBar(R.id.progress_importing, 100,0, false);

        Intent notificationIntent = new Intent(this, ImportContactsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        //mNotification.contentIntent = contentIntent;

        int totalNum = 0;
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            totalNum += mSimSlotStatus[i].mTotalCount;
        }
        mNotification.contentView.setProgressBar(R.id.progress_importing, totalNum, 0, false);

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);

    }

    private void updateProgressNotification(int progress) {
        if (mNotification != null) {
            int totalNum = 0;
            for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
                totalNum += mSimSlotStatus[i].mTotalCount;
            }
            mNotification.contentView.setProgressBar(R.id.progress_importing, totalNum, progress, false);
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }

    private void updateUI(int state) {
        if (state == UI_START_QUERY) {
            mBeingQuery = true;
            mImportBtn.setTag(TAG_CANCEL);
            mImportBtn.setEnabled(false);
            mImportBtn.setText(R.string.oobe_btn_cancel_import_contacts);
            mBackBtn.setEnabled(false);
            mNextBtn.setEnabled(false);
            mTextCopyNote.setVisibility(View.VISIBLE);
            mTextCopyNote.setText(R.string.oobe_note_import_contacts_prepare);
            // getPreferenceScreen().setEnabled(false);
        } else if (state == UI_START_COPY) {
            mBeingQuery = false;
            mImportBtn.setEnabled(true);
            initProgressBar();
            mBackBtn.setEnabled(true);
            mNextBtn.setEnabled(true);
            mNextBtn.setText(R.string.oobe_btn_text_next);
            mTextCopyNote.setText(R.string.oobe_note_import_contacts_going);
            // show notification on bar;
            sCopying = true;
        } else if (state == UI_FNISH_COPY) {
            boolean isImported = true;
            for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
                isImported = isImported && (mSlotSimPref[i] == null || mSimSlotStatus[i].mCopyFinished);
            }
            if (isImported) {
                // contact in all SIM have been imported, then disappear import buttton
                mImportBtn.setVisibility(View.INVISIBLE);
            } else {
                mImportBtn.setText(R.string.oobe_btn_start_import_contacts);
                mImportBtn.setTag(TAG_IMPORT);
            }
            mTextCopyNote.setText(R.string.oobe_note_import_contacts_finish);
            // getPreferenceScreen().setEnabled(true);
            sCopying = false;
        } else if (state == UI_CANCEL_COPY) {
            mImportBtn.setEnabled(true);
            mImportBtn.setText(R.string.oobe_btn_start_import_contacts);
            mImportBtn.setTag(TAG_IMPORT);
            mBackBtn.setEnabled(true);
            mNextBtn.setEnabled(true);
            mNextBtn.setText(R.string.oobe_btn_text_skip);
            mTextCopyNote.setText(R.string.oobe_note_import_contacts_cancel);
            // getPreferenceScreen().setEnabled(true);
            sCopying = false;
        }
    }

    private boolean checkSimState() {
        Xlog.v(TAG, "checkSimState() begin ");
        // if (com.mediatek.featureoption.FeatureOption.MTK_GEMINI_SUPPORT) {
        if (Utils.isGemini()) {
            List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
            for (int i = 0; i < simList.size(); i++) {
                SIMInfo siminfo = simList.get(i);
                int slotIndex = siminfo.mSlot;
                int simState = -1;
                try {
                    simState = mITelephony.getSimIndicatorStateGemini(slotIndex);
                } catch (RemoteException exception) {
                    Xlog.i(TAG, "RemoteException " + exception.getMessage());
                }
                if (mSimSlotStatus[slotIndex].mIsSelected) {
                    if (simState == com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_LOCKED
                            || simState == com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_RADIOOFF) {
                        mCellMgr.handleCellConn(slotIndex, CellConnMgr.REQUEST_TYPE_SIMLOCK);
                        return false;
                    }
                }
            }
        } else {
            if (mSimSlotStatus[SLOT_INDEX_1].mIsSelected) {
                int simState = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimState();
                if (simState == TelephonyManager.SIM_STATE_PIN_REQUIRED
                        || simState == TelephonyManager.SIM_STATE_PUK_REQUIRED) {
                    mCellMgr.handleCellConn(SLOT_INDEX_1, CellConnMgr.REQUEST_TYPE_SIMLOCK);
                    return false;
                }
            }
        }
        return true;
    }

    private void cancelImporting() {
        ImportContactsActivity.sCanceled = true;
        mImportBtn.setEnabled(false);
        // dealWithCancel();
        // updateUI(UI_CANCEL_COPY);
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            mSimSlotStatus[i].mSuccessCount = 0;
        }
    }

    private void importContacts() {
        boolean notSelected = true;
        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            notSelected = notSelected && !mSimSlotStatus[i].mIsSelected;
        }
        if (notSelected) {
            Xlog.v(TAG, "No SIM is selected");
            return;
        }
        if (sCopying) {
            Xlog.d(TAG, "onClick() importing already, return");
            showDialog(COPYING_DIALOG_ID);
            return;
        }
        if (!checkSimState()) {
            Xlog.v(TAG, "SIM Locked");
            return;
        }

        //Xlog.v(TAG, "Selected SIM: slot1 = " + mSimSlotStatus[SLOT_INDEX_1].mIsSelected + "slot2 = "
        //        + mSimSlotStatus[SLOT_INDEX_2].mIsSelected);
        // Judge the SIM card is USIM or not
        try {
            // if (com.mediatek.featureoption.FeatureOption.MTK_GEMINI_SUPPORT) {
            if (Utils.isGemini()) {
                // if (mTelephonyService == null) {
                // mIsSrcSimUSIM[SLOT_INDEX_1] = false;
                // mIsSrcSimUSIM[SLOT_INDEX_2] = false;
                // }

                for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
                    if (mSimSlotStatus[i].mIsSelected && mTelephonyService.getIccCardTypeGemini(i).equals("USIM")) {
                        mSimSlotStatus[i].mIsSrcSimUSIM = true;
                    }
                }

            } else {
                if (mTelephonyService != null && mTelephonyService.getIccCardType().equals("USIM")) {
                    mSimSlotStatus[SLOT_INDEX_1].mIsSrcSimUSIM = true;
                }
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

        //Xlog.i(TAG, "Source SIM is USIM: SIM1 - " + mSimSlotStatus[SLOT_INDEX_1].mIsSrcSimUSIM + ", SIM2 - "
        //        + mSimSlotStatus[SLOT_INDEX_2].mIsSrcSimUSIM);

        /*
         * ready? boolean bPBKReady = true;
         * if (mSimSlotStatus[SLOT_INDEX_1].mIsSelected 
         *     && 1 != ContactsUtils.getSim1Ready())
         * { bPBKReady = false; } else { bPBKReady = true; }
         * 
         * if (mSimSlotStatus[SLOT_INDEX_2].mIsSelected && 1 != ContactsUtils.getSim2Ready()) { bPBKReady = false; } else {
         * bPBKReady = true; }
         * 
         * if (bPBKReady) { startQuery(); } else {// launch an wait dialog, wait for sim ready.
         * this.mClockHandler.sendMessageDelayed( this.mClockHandler.obtainMessage(MESSAGE_TIME_OUT), TIME_OUT); mNeedQuery =
         * true; Xlog.d(TAG, "wait for sim ready"); }
         */
        updateUI(UI_START_QUERY);
        startQuery();
    }

    private void startQuery() {
        Xlog.i(TAG, " slot number =" + mMapSlot2SimInfo.size() + "  " + mMapSlot2SimInfo.toString());

        Xlog.i(TAG, " slot number =" + mMapSlot2SimInfo.size() + "  " + mMapSlot2SimInfo.toString());
        for (int slot = 0; slot < SLOT_TOTAL_NUMBER; slot++) {
            if (mSimSlotStatus[slot].mIsSelected) {
                if (!mSimSlotStatus[slot].mIsSrcSimUSIM) {
                    // copy from sim1 to phone
                    mQueryHandler.startQuery(slot, null, Contacts.CONTENT_URI, mContactsProjection,
                            RawContacts.INDICATE_PHONE_SIM + " = " + mMapSlot2SimInfo.get(slot).mSimId + " AND "
                                    + CLAUSE_ONLY_VISIBLE, null, Contacts.SORT_KEY_PRIMARY);
                } else {
                    // copy from usim1 to phone
                    mQueryHandler.startQuery(slot, null, Contacts.CONTENT_URI, mContactsProjection,
                            RawContacts.INDICATE_PHONE_SIM + " = " + mMapSlot2SimInfo.get(slot).mSimId, null,
                            Contacts.SORT_KEY_PRIMARY);
                }
            }
        }
    }

    private class QueryHandler extends AsyncQueryHandler {
        protected final WeakReference<ImportContactsActivity> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<ImportContactsActivity>((ImportContactsActivity) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) { //MTK_CS_IGNORE_THIS_LINE
            Xlog.i(TAG, "Query Complete: cursor.getcount is " + cursor.getCount());
            int slotIndex = token;

            try {
                // if (com.mediatek.featureoption.FeatureOption.MTK_GEMINI_SUPPORT) {
                if (Utils.isGemini()) {
                    if (mTelephonyService == null || (!mTelephonyService.isRadioOnGemini(slotIndex))) {
                        Xlog.w(TAG, " query cursor enter gemini phone, null cursor");
                        cursor = null;
                    }
                } else {
                    if (mTelephonyService == null || (!mTelephonyService.isRadioOn())) {
                        cursor = null;
                        Xlog.e(TAG, " query cursor enter single phone, null cursor");
                    }
                }

            } catch (RemoteException e) {
                Xlog.e(TAG, "RemoteException!");
                return;
            }

            if (!mSimSlotStatus[slotIndex].mIsSelected) {
                if (cursor != null) {
                    cursor.close();
                }
                return;
            }
            if (cursor == null) {
                Xlog.e(TAG, "Contact import, query cursor is null, Just return");
                mTextCopyNote.setVisibility(View.VISIBLE);
                mTextCopyNote.setText(R.string.oobe_contact_import_null_cursor);
                mBackBtn.setEnabled(true);
                mNextBtn.setEnabled(true);
                return;
            }
            mSimSlotStatus[slotIndex].mTotalCount = cursor.getCount();
            mSimSlotStatus[slotIndex].mIsCopying = true;
            Xlog.i(TAG, "Query Complete: Total contact count of SIM " + slotIndex + " is "
                    + mSimSlotStatus[slotIndex].mTotalCount);

            mCursorQuerySims[slotIndex] = cursor;
            mSimSlotStatus[slotIndex].mQuerySimDone = true;

            boolean queryDone = true;
            for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
                queryDone = queryDone && (!mSimSlotStatus[i].mIsSelected 
                                || (mSimSlotStatus[i].mIsSelected && mSimSlotStatus[i].mQuerySimDone));
            }
            if (queryDone) {
                startCopy();
            }
        }
    }

    private void startCopy() {
        updateUI(UI_START_COPY);
        mCopyThread = new CopyThread();
        mCopyThread.start();
    }

    private class CopyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            final int msgId = msg.what;
            switch (msgId) {
            case ID_IN_COPY:
                int simIndex = msg.arg1;
                Xlog.v(TAG, getStepSpecialTag() + "CopyHandler, simIndex=" + simIndex + ", mTotalCount="
                        + mSimSlotStatus[simIndex].mTotalCount 
                        + ", success count=" + mSimSlotStatus[simIndex].mSuccessCount);
                if (mSimSlotStatus[simIndex].mSuccessCount >= mSimSlotStatus[simIndex].mTotalCount) {
                    finishProgressBar(simIndex);
                } else {
                    incrementProgressTo(simIndex, mSimSlotStatus[simIndex].mSuccessCount);
                }
                break;
            case ID_END_COPY:
                for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
                    if (mSimSlotStatus[i].mIsSelected) {
                        Xlog.v(TAG, getStepSpecialTag() + "slot " + i + " finish importing");
                        finishProgressBar(i);
                    }
                }
                updateUI(UI_FNISH_COPY);
                break;
            case ID_CANCEL_COPY:
                dealWithCancel();
                updateUI(UI_CANCEL_COPY);
                break;
            default:
                break;
            }
        }
    }

    private class CopyThread extends Thread {
        PowerManager.WakeLock mWakeLock;
        private ContentResolver mResolver = getContentResolver();

        public CopyThread() {
            super();
            Context context = ImportContactsActivity.this;
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        }

        @Override
        public void run() { //MTK_CS_IGNORE_THIS_LINE
            if (mBeingCopied) {
                return;
            }

            mWakeLock.acquire();
            ImportContactsActivity.sCanceled = false;
            mBeingCopied = true;

            int sameNameCount = 0;
            String name = null;
            String number = null;
            Cursor simCursor = null;
            Cursor phoneCursor = null;
            int type = -1;
            Xlog.i(TAG, "copy from sim to phone");
            for (int i = 0; i < SLOT_TOTAL_NUMBER && !ImportContactsActivity.sCanceled; i++) {
                Xlog.i(TAG, "copy thread, mSimSlotStatus[i].mIsSelected=" + mSimSlotStatus[i].mIsSelected);
                if (!mSimSlotStatus[i].mIsSelected) {
                    Xlog.i(TAG, "SIM in slot " + i + " is not selected for contact import");
                    continue;
                }
                if (mSimSlotStatus[i].mCopyFinished) {
                    Xlog.w(TAG, getStepSpecialTag() + "contact in SIM " + i + " have already been imported, ignore");
                    continue;
                }

                /*Xlog.i(TAG, getStepSpecialTag() + "mSimSlotStatus[SLOT_INDEX_1].mIsSelected="
                        + mSimSlotStatus[SLOT_INDEX_1].mIsSelected + ",mSimSlotStatus[SLOT_INDEX_2].mIsSelected="
                        + mSimSlotStatus[SLOT_INDEX_2].mIsSelected);
                Xlog.i(TAG, getStepSpecialTag() + "mSimSlotStatus[SLOT_INDEX_1].mQuerySimDone="
                        + mSimSlotStatus[SLOT_INDEX_1].mQuerySimDone + ", mSimSlotStatus[SLOT_INDEX_2].mQuerySimDone="
                        + mSimSlotStatus[SLOT_INDEX_2].mQuerySimDone);*/

                // if (com.mediatek.featureoption.FeatureOption.MTK_GEMINI_SUPPORT) {
                // if (Utils.isGemini()) {
                // mCopyingSim = mMapSlot2SimInfo.get(i).mDisplayName;
                // } else {
                // mCopyingSim = getString(R.string.oobe_import_contacts_sim_name);
                // }

                try {
                    simCursor = mCursorQuerySims[i];
                    if (simCursor == null || simCursor.isClosed()) {
                        Xlog.w(TAG, "simCursor==null, simIndex=" + i);
                        continue;
                    }
                    if (simCursor.getCount() == 0) {
                        Xlog.w(TAG, "Get 0 contacts from SIM " + i);
                        simCursor.close();
                        continue;
                    }

                    simCursor.moveToFirst();
                    if (!mSimSlotStatus[i].mIsSrcSimUSIM) {
                        while (!ImportContactsActivity.sCanceled) {
                            long contactId = simCursor.getLong(ImportContactsActivity.SUMMARY_ID_COLUMN_INDEX);
                            name = simCursor.getString(ImportContactsActivity.SUMMARY_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
                            Xlog.i(TAG, "Name is: " + name);
                            try {
                                phoneCursor = queryPhoneNumbers(contactId);
                                number = "";
                                type = -1;
                                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                    number = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER));
                                    type = phoneCursor.getInt(phoneCursor.getColumnIndex(Phone.TYPE));
                                }
                            } finally {
                                if(phoneCursor != null)  {
                                    phoneCursor.close();
                                }
                            }
                            ContentValues values = new ContentValues();
                            if (!TextUtils.isEmpty(number)) {
                                number = number.replaceAll("-", "");
                            }
                            if (!TextUtils.isEmpty(name) && name.equals(number)) {
                                values.put("tag", "");
                                values.put("number", number);
                                name = null;
                            } else {
                                values.put("tag", (sameNameCount > 0) ? (name + sameNameCount) : name);
                                values.put("number", number);
                            }

                            insertToDb(name, number, type);
                            mSimSlotStatus[i].mSuccessCount++;
                            Message msg = Message.obtain();
                            msg.what = ID_IN_COPY;
                            msg.arg1 = i;// slot index, which SIM's contact is importing now
                            mCopyHandler.sendMessage(msg);

                            if (!simCursor.moveToNext()) {
                                break;
                            }

                            // slow import speed
                            Xlog.i("dwz", "wait a few second for import");
                            try {
                                final int sleepTime = 2000;
                                sleep(sleepTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        while (!ImportContactsActivity.sCanceled) {
                            String email = null;
                            String additionalNumber = null;
                            long contactId = simCursor.getLong(ImportContactsActivity.SUMMARY_ID_COLUMN_INDEX);
                            // name =
                            // cursor.getString(ContactsListActivity.SUMMARY_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);

                            long rawContactId = queryForRawContactId(getContentResolver(), contactId);
                            Xlog.i(TAG, "copy from usim to phone rawContactId is " + rawContactId);
                            Cursor c = getContentResolver().query(Data.CONTENT_URI,
                                    new String[] { Data.MIMETYPE, Data.DATA1, Data.IS_ADDITIONAL_NUMBER },
                                    Data.RAW_CONTACT_ID + "=" + rawContactId, null, null);
                            if (null != c) {
                                while (c.moveToNext()) {
                                    Xlog.i(TAG,
                                            "copy from usim to phone c.getCount() is " + c.getCount() + " mimeType is "
                                                    + c.getString(0) + " data1 is" + c.getString(1)
                                                    + " is_additional_number is " + c.getString(2));
                                    // if
                                    // (c.getString(0).equals(Phone.CONTENT_ITEM_TYPE)
                                    // && c.getString(2).equals("0")) {
                                    // Number[realLenOfNum] = c.getString(1);
                                    // Xlog.i(TAG,"Number[" + realLenOfNum +
                                    // "] is " +
                                    // Number[realLenOfNum] );
                                    // realLenOfNum++;
                                    // }
                                    if (c.getString(0).equals(Phone.CONTENT_ITEM_TYPE) 
                                            && c.getString(2).equals("1")) { // additional
                                        // number
                                        additionalNumber = c.getString(1);
                                        Xlog.i(TAG, "copy from usim to phone additionalNumber is " + additionalNumber);
                                    }
                                    if (c.getString(0).equals(StructuredName.CONTENT_ITEM_TYPE)) {
                                        name = c.getString(1);
                                        Xlog.i(TAG, "In run name is " + name);
                                    }
                                    if (c.getString(0).equals(Email.CONTENT_ITEM_TYPE)) {
                                        email = c.getString(1);
                                        Xlog.i(TAG, "copy from usim to phone email is " + email);
                                    }

                                }
                                c.close();
                            }
                            try {
                                phoneCursor = queryPhoneNumbers(contactId);
                                number = "";
                                type = -1;
                                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                    number = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER));
                                    type = phoneCursor.getInt(phoneCursor.getColumnIndex(Phone.TYPE));
                                }
                            } finally {
                                if(phoneCursor != null)  {
                                    phoneCursor.close();
                                }
                            }
                            Xlog.i(TAG, "copy from usim to phone name is " + name);
                            Xlog.i(TAG, "copy from usim to phone number is " + number);
                            insertToDB(name, number, email, additionalNumber, mResolver, RawContacts.INDICATE_PHONE, "USIM");
                            // insertToDb(name, number, type);
                            mSimSlotStatus[i].mSuccessCount++;
                            Message msg = Message.obtain();
                            msg.what = ID_IN_COPY;
                            msg.arg1 = i;// slot index, which SIM's contact is importing now
                            mCopyHandler.sendMessage(msg);

                            if (!simCursor.moveToNext()) {
                                break;
                            }
                        }
                    }
                } finally {
                    if (simCursor != null && !simCursor.isClosed()) {
                        simCursor.close();
                    }
                }
            }

            mBeingCopied = false;
            if (ImportContactsActivity.sCanceled) {
                mCopyHandler.sendEmptyMessage(ID_CANCEL_COPY);
            } else {
                mCopyHandler.sendEmptyMessage(ID_END_COPY);
            }

            mWakeLock.release();
        }

        private Cursor queryPhoneNumbers(long contactId) {
            Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
            Uri dataUri = Uri.withAppendedPath(baseUri, Contacts.Data.CONTENT_DIRECTORY);
            Cursor c = getContentResolver().query(
                    dataUri,
                    new String[] { BaseColumns._ID, Phone.NUMBER, Phone.IS_SUPER_PRIMARY, RawContacts.ACCOUNT_TYPE,
                            Phone.TYPE, Phone.LABEL, Data.DATA15 }, Data.MIMETYPE + "=?",
                    new String[] { Phone.CONTENT_ITEM_TYPE }, null);
            if (c != null && c.moveToFirst()) {
                return c;
            }
            if (c != null) {
                c.close();
            }
            return null;
        }

        private void insertToDb(String name, String number, int type) {
            Xlog.v(TAG, "name is " + name + " number is " + number);
            ContentValues values = new ContentValues();
            final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

            // insert basic information to raw_contacts table
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
            values.put(RawContacts.INDICATE_PHONE_SIM, RawContacts.INDICATE_PHONE);

            String myGroupsId = null;
            builder.withValues(values);
            operationList.add(builder.build());

            // insert phone number to data table
            if (!TextUtils.isEmpty(number)) {
                Xlog.i(TAG, "PhoneNumberFormatUtilEx.formatNumber(number) is " + number);
                number = PhoneNumberFormatUtilEx.formatNumber(number);
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                builder.withValue(Phone.NUMBER, number);
                // if (type < 0) {
                // builder.withValue(Phone.TYPE, Phone.TYPE_OTHER);
                // } else {
                builder.withValue(Data.DATA2, 2);
                // }
                operationList.add(builder.build());
            }

            // insert name to data table
            if (!TextUtils.isEmpty(name)) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                builder.withValue(StructuredName.GIVEN_NAME, name);
                operationList.add(builder.build());
            }

            try {
                mResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (RemoteException e) {
                Xlog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (OperationApplicationException e) {
                Xlog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
        }

        public void insertToDB(String name, String number, String email, String additionalNumber, ContentResolver resolver,
                long indicate, String simType) {
            final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
            ContentValues contactvalues = new ContentValues();
            contactvalues.put(RawContacts.INDICATE_PHONE_SIM, indicate);
            contactvalues.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
            builder.withValues(contactvalues);

            operationList.add(builder.build());

            int phoneType = Phone.TYPE_OTHER;
            String phoneTypeSuffix = "";
            // mtk80909 for ALPS00023212
            if (!TextUtils.isEmpty(name)) {
                final NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(name);
                name = namePhoneTypePair.mName;
                phoneType = namePhoneTypePair.mPhoneType;
                phoneTypeSuffix = namePhoneTypePair.mPhoneTypeSuffix;
            }

            // insert number
            if (!TextUtils.isEmpty(number)) {
                number = PhoneNumberFormatUtilEx.formatNumber(number);
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                builder.withValue(Phone.NUMBER, number);
                // mtk80909 for ALPS00023212
                // builder.withValue(Phone.TYPE, phoneType);
                builder.withValue(Data.DATA2, 2);
                if (!TextUtils.isEmpty(phoneTypeSuffix)) {
                    builder.withValue(Data.DATA15, phoneTypeSuffix);
                }
                operationList.add(builder.build());
            }

            // insert name
            if (!TextUtils.isEmpty(name)) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                builder.withValue(StructuredName.GIVEN_NAME, name);
                operationList.add(builder.build());
            }

            // if USIM
            if (simType.equals("USIM")) {
                // insert email
                if (!TextUtils.isEmpty(email)) {
                    // for (String emailAddress : emailAddressArray) {
                    Xlog.i(TAG, "In actuallyImportOneSimContact email is " + email);
                    builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                    builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
                    builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                    builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
                    builder.withValue(Email.DATA, email);
                    operationList.add(builder.build());
                    // }
                }
                if (!TextUtils.isEmpty(additionalNumber)) {
                    additionalNumber = PhoneNumberFormatUtilEx.formatNumber(additionalNumber);
                    Xlog.i(TAG, "additionalNumber is " + additionalNumber);
                    builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                    builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                    builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    // builder.withValue(Phone.TYPE, phoneType);
                    builder.withValue(Data.DATA2, Phone.TYPE_OTHER);
                    builder.withValue(Phone.NUMBER, additionalNumber);
                    builder.withValue(Data.IS_ADDITIONAL_NUMBER, 1);
                    operationList.add(builder.build());
                }
            }

            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, operationList);// saved
                                                                               // in
                                                                               // database
            } catch (RemoteException e) {
                Xlog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (OperationApplicationException e) {
                Xlog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
        }

        public long queryForRawContactId(ContentResolver cr, long contactId) {
            Cursor rawContactIdCursor = null;
            long rawContactId = -1;
            try {
                rawContactIdCursor = cr.query(RawContacts.CONTENT_URI, new String[] { BaseColumns._ID },
                        RawContacts.CONTACT_ID + "=" + contactId, null, null);
                if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                    // Just return the first one.
                    rawContactId = rawContactIdCursor.getLong(0);
                }
            } finally {
                if (rawContactIdCursor != null) {
                    rawContactIdCursor.close();
                }
            }
            return rawContactId;
        }
    }

    public void refreshProgress() {
        Xlog.i(TAG, "ImportContacts===========refreshProgress()==========selfChangeFlag=" + mSelfChangeFlag);
        if (mSelfChangeFlag) {
            return;
        }

        for (int i = 0; i < SLOT_TOTAL_NUMBER; i++) {
            if (!mSimSlotStatus[i].mIsSelected) {
                Xlog.i(TAG, "SIM in slot " + i + " is not selected for contact import, so do not refresh it");
                continue;
            }
            if (OOBEConstants.DEBUG) {
                Xlog.i(TAG, "refreshProgress(), i=" + i + ",mSimSlotStatus[i].mIsCopying=" + mSimSlotStatus[i].mIsCopying
                        + ", mSimSlotStatus[i].mCopyFinished=" + mSimSlotStatus[i].mCopyFinished + ", mFinishImporting="
                        + mSlotSimPref[i].isFinishImporting());
            }
            if (mSimSlotStatus[i].mIsCopying || mSimSlotStatus[i].mCopyFinished) {
                if (!mProgressBeenInitFlag) {
                    mProgressBeenInitFlag = true;
                    updateUI(UI_START_COPY);
                }
                if (mSimSlotStatus[i].mCopyFinished) {
                    finishProgressBar(i);
                    updateCopyNote();
                } else {
                    incrementProgressTo(i, mSimSlotStatus[i].mSuccessCount);
                }
            }
        }
    }

    public void refreshCancel() {
        mImportBtn.setEnabled(true);
        mImportBtn.setText(R.string.oobe_btn_start_import_contacts);
        dealWithCancel();
        updateUI(UI_CANCEL_COPY);
    }

    @Override
    protected String getStepSpecialTag() {
        return "ImportContactsActivity";
    }
    
    /**
     * @author mtk54279
     * The function is disable SIM Preference item when sim card is removed.(hot swap sim card)
     * @param simSlot which slot of the sim card in.
     * @return void
     */
    private void disableSimPreference(){

        if( mSlot1SimPref != null ) {
            mSlot1SimPref.setEnabled(false);
        }
        if( mSlot2SimPref != null ){
            mSlot2SimPref.setEnabled(false);
        }
        if( mSlot3SimPref != null ){
            mSlot3SimPref.setEnabled(false);
        }
        if( mSlot4SimPref != null ){
            mSlot4SimPref.setEnabled(false);
        }
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
        Xlog.i("hotswapdbg", "sim number is "+simList.size());
        
        for(SIMInfo siminfo : simList ) {
            if(siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1){
                if( mSlot1SimPref != null ) {
                    mSlot1SimPref.setEnabled(true);
                }
            } else if(siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2){
                if( mSlot2SimPref != null ){
                    mSlot2SimPref.setEnabled(true);
                }
            } else if(siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_3){
                if( mSlot3SimPref != null ){
                    mSlot3SimPref.setEnabled(true);
                }
            } else if(siminfo.mSlot == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_4){
                if( mSlot4SimPref != null ){
                    mSlot4SimPref.setEnabled(true);
                }
            }
        }

    }
    private void updateSimPreference(){
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
        Xlog.d(TAG, "updateSimPreference(), sim number:" + simList.size());
        int simNum = getPreferenceScreen().getPreferenceCount();
        for (int i = simNum - 1; i >= 0; --i) {
            SimInfoPreference preference = (SimInfoPreference)getPreferenceScreen().getPreference(i);
            for (SIMInfo siminfo : simList) {
                if (siminfo.mSlot == preference.getSlotIndex()) {
                    preference.setStatus(getSimStatus(siminfo.mSlot));
                }
            }
        }

    }
    private int getSimStatus(int slot) {
        int status = -1;
        try {
            if (PhoneConstants.GEMINI_SIM_NUM > 1) {
                status = mITelephony.getSimIndicatorStateGemini(slot);
            } else {
                status = mITelephony.getSimIndicatorState();
            }
        } catch (RemoteException exception) {
            Xlog.e(TAG, "RemoteException " + exception.getMessage());
        }
        Xlog.d(TAG, "getSimStatus: " + status);
        return status;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case COPYING_DIALOG_ID:
                AlertDialog dialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(com.android.internal.R.string.dialog_alert_title)
                    .setMessage(getString(R.string.import_contact_already))
                    .setPositiveButton(com.android.internal.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
            return dialog;
        }
        return super.onCreateDialog(dialogId);
    }
}
