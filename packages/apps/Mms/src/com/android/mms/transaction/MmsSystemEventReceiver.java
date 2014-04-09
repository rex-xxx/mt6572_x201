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

package com.android.mms.transaction;

import static android.provider.BaseColumns._ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.google.android.mms.util.PduCache;

/// M:
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SqliteWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.ThreadSettings;
import android.telephony.SmsManager;

import com.android.mms.MmsConfig;
import com.android.mms.ui.ChatPreferenceActivity;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.SmsTemplateEditActivity;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.MuteCache;
import com.android.mms.R;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.content.EncapsulatedAction;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.ipmsg.util.IpMessageUtils;


/**
 * MmsSystemEventReceiver receives the
 * {@link android.content.intent.ACTION_BOOT_COMPLETED},
 * {@link com.android.internal.telephony.TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED}
 * and performs a series of operations which may include:
 * <ul>
 * <li>Show/hide the icon in notification area which is used to indicate
 * whether there is new incoming message.</li>
 * <li>Resend the MM's in the outbox.</li>
 * </ul>
 */
public class MmsSystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSystemEventReceiver";
    private static MmsSystemEventReceiver sMmsSystemEventReceiver;

    private static void wakeUpService(Context context) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "wakeUpService: start transaction service ...");
        }

        context.startService(new Intent(context, TransactionService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "Intent received: " + intent);
        }
        /// M:
        MmsLog.d(MmsApp.LOG_TAG, "onReceive(): intent=" + intent.toString());
        String action = intent.getAction();
        if (action.equals(Mms.Intents.CONTENT_CHANGED_ACTION)) {
        /// M:Code analyze 003, put in a new thread @{
            final Intent mIntent = intent;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Uri changed = (Uri) mIntent.getParcelableExtra(Mms.Intents.DELETED_CONTENTS);
                    if (changed != null) {
                        MmsApp.getApplication().getPduLoaderManager().removePdu(changed);
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "Mms.Intents.CONTENT_CHANGED_ACTION: " + changed);
                }
            }).start();
        /// @}
        } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
            String state = intent.getStringExtra(EncapsulatedPhone.STATE_KEY);

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "ANY_DATA_STATE event received: " + state);
            }
            /// M:Code analyze 004, modify the logic,if data connection change,unRegister the listener of
            /// connection changed and wake up transaction service,and add gemini logic @{
            String apnType = intent.getStringExtra(EncapsulatedPhone.DATA_APN_TYPE_KEY);

            //if (state.equals("CONNECTED")) {
            if (EncapsulatedPhone.APN_TYPE_MMS.equals(apnType)) {
                MmsLog.d(MmsApp.TXN_TAG, "TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED, type is mms.");
                // if the network is not available for mms, keep listening
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                if (ni != null && !ni.isAvailable()) {
                    MmsLog.d(MmsApp.TXN_TAG, "network is not available for mms, keep listening.");
                    return;
                }
                
                unRegisterForConnectionStateChanges(context);
                // add for gemini
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    // conver slot id to sim id
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(context,
                            intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY,EncapsulatedPhone.GEMINI_SIM_1));
                    if (null == si) {
                        MmsLog.e(MmsApp.TXN_TAG, "System event receiver: SIMInfo is null for slot "
                                + intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));
                        return;
                    }
                    int simId = (int)si.getSimId();
                    wakeUpServiceGemini(context, simId/*intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY,Phone.GEMINI_SIM_1)*/);
                } else {
                    wakeUpService(context);
                }
            }
            /// @}
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            /// M:Code analyze 005, mark pending msg failed after boot complete @{
            MmsLog.d(MmsApp.TXN_TAG, "Intent.ACTION_BOOT_COMPLETED");
            final Context contxt = context;
            new Thread(new Runnable() {
                public void run() {
                    IpMessageUtils.getIpMessagePlugin(contxt).getServiceManager(contxt.getApplicationContext()).startIpService();
                    setPendingMmsFailed(contxt);
                    setNotificationIndUnstarted(contxt);
                }
            }).start();
            /// @}
            // We should check whether there are unread incoming
            // messages in the Inbox and then update the notification icon.
            // Called on the UI thread so don't block.
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                    context, MessagingNotification.THREAD_NONE, false);
            /// M: when power off,save the chat setting. {@
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(contxt.getApplicationContext());
            final long unSavedthreadId = sp.getLong(ChatPreferenceActivity.CHAT_THREAD_ID, -1l);
            Log.v(TAG, "unSaved chatThreadSetting: " + unSavedthreadId);
            if (unSavedthreadId > 0) {
                final boolean mNotificationEnable = sp.getBoolean(ChatPreferenceActivity.ENABLE_NOTIFICATION, false);
                final String mMute = sp.getString(ChatPreferenceActivity.CHAT_MUTE, "0");
                final String mRingtone = sp.getString(ChatPreferenceActivity.CHAT_RINGTONE, ChatPreferenceActivity.DEFAULT_RINGTONE);
                final boolean mVibrate = sp.getBoolean(ChatPreferenceActivity.CHAT_VIBRATE, false);
                final long mMuteStart = sp.getLong(ChatPreferenceActivity.CHAT_MUTE_START, 0);
                new Thread() {
                    public void run() {
                        SharedPreferences spChatThreadId = PreferenceManager.getDefaultSharedPreferences(contxt.getApplicationContext());
                        Uri uri = ContentUris.withAppendedId(Uri.parse(ChatPreferenceActivity.CHAT_SETTINGS_URI), unSavedthreadId);
                        ContentValues values = new ContentValues();
                        values.put(ThreadSettings.NOTIFICATION_ENABLE, mNotificationEnable ? 1 : 0);
                        values.put(ThreadSettings.MUTE, Integer.parseInt(mMute));
                        values.put(ThreadSettings.MUTE_START, mMuteStart);
                        values.put(ThreadSettings.RINGTONE, mRingtone);
                        values.put(ThreadSettings.VIBRATE, mVibrate ? 1 : 0);
                        contxt.getContentResolver().update(uri, values, null, null);
                        SharedPreferences.Editor editor = spChatThreadId.edit();
                        editor.putLong(ChatPreferenceActivity.CHAT_THREAD_ID, -1l);
                        editor.commit();
                        if (MmsConfig.getFolderModeEnabled()) {
                            MuteCache.setMuteCache(unSavedthreadId,Long.parseLong(mMute),mMuteStart,mNotificationEnable);
                        }
                    }
                }.start();
            }
            /// M: @}
        /// M:Code analyze 006,add for listening sim setting info changed @{
        } else if (action.equals(EncapsulatedAction.SIM_SETTINGS_INFO_CHANGED)) {
            int simId = (int)intent.getLongExtra("simid", -1);
            MessageUtils.simInfoMap.remove(simId);
            MessageUtils.getSimInfo(context, simId);
        /// @}
        /// M:Code analyze 001,add for save draft mms when received event of shutdown @{
        } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
            mSaveDraft = (OnShutDownListener) ComposeMessageActivity.getComposeContext();
            if (mSaveDraft != null) {
                mSaveDraft.onShutDown();
            }
        /// @}
        /// M:Code analyze 002,add for listening the changing of sim info @{
        } else if (action.equals(EncapsulatedAction.ACTION_SMS_DEFAULT_SIM_CHANGED)) {
            MmsLog.d(MmsApp.LOG_TAG, "SMS default SIM changed.");
            mSimInforChangedListener = (OnSimInforChangedListener) ComposeMessageActivity.getComposeContext();
            if (mSimInforChangedListener != null) {
                mSimInforChangedListener.onSimInforChanged();
            }
            mSimInforChangedListener = (OnSimInforChangedListener) ConversationList.getContext();
            if (mSimInforChangedListener != null) {
                mSimInforChangedListener.onSimInforChanged();
            }
        /// @}
        /// M:Code analyze 007,add for listening device storage full or not @{
        } else if (action.equals(Intent.ACTION_DEVICE_STORAGE_FULL)) {
            MmsConfig.setDeviceStorageFullStatus(true);
        } else if (action.equals(Intent.ACTION_DEVICE_STORAGE_NOT_FULL)) {
            MmsConfig.setDeviceStorageFullStatus(false);
            MessagingNotification.cancelNotification(context,
                    SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);
        /// @}
        /// M: new feature, add default quick_text @{
        } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
            if (MmsConfig.getInitQuickText()) {
                return;
            }
            final Context runContext = context;
            new Thread(new Runnable() {
                public void run() {
                    Cursor cursor = runContext.getContentResolver().
                        query(EncapsulatedTelephony.MmsSms.CONTENT_URI_QUICKTEXT, null, null, null, null);
                    // get delete and add ID in DB, id quick Text array
                    ArrayList<Integer> delAddID = new ArrayList<Integer>();
                    ArrayList<Integer> quickTextID = new ArrayList<Integer>();
                    if (cursor != null) {
                        try {
                            ArrayList<String> preQuickText = MmsConfig.getPreQuickText();
                            while (cursor.moveToNext()) {
                                int id = cursor.getInt(0);
                                String str = cursor.getString(1);
                                for (int i = 0; i < preQuickText.size(); i++) {
                                    if (str.equals(preQuickText.get(i))) {
                                        delAddID.add(new Integer(id));
                                        quickTextID.add(new Integer(i));
                                        break;
                                    }
                                }
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                    // delete old language default quick text
                    for (Integer i : delAddID) {
                        runContext.getContentResolver().delete(EncapsulatedTelephony.MmsSms.CONTENT_URI_QUICKTEXT,
                                _ID + "=" + i, null);
                    }

                    // add new language default quick text
                    String[] default_quick_texts = runContext.getResources()
                                .getStringArray(R.array.default_quick_texts);
                    for (int i = 0, j = 0 ; i < delAddID.size() && j < quickTextID.size();
                                                                                      i++, j++) {
                        ContentValues values = new ContentValues();
                        values.put(_ID, delAddID.get(i));
                        values.put("text", default_quick_texts[quickTextID.get(j)]);
                        runContext.getContentResolver()
                                .insert(EncapsulatedTelephony.MmsSms.CONTENT_URI_QUICKTEXT, values);
                    }
                    MmsConfig.setPreQuickText(default_quick_texts);
                }
            }).start();
        }
        /// @}
    }

    public static void registerForConnectionStateChanges(Context context) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "registerForConnectionStateChanges");
        unRegisterForConnectionStateChanges(context);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "registerForConnectionStateChanges");
        }
        if (sMmsSystemEventReceiver == null) {
            sMmsSystemEventReceiver = new MmsSystemEventReceiver();
        }

        context.registerReceiver(sMmsSystemEventReceiver, intentFilter);
    }

    public static void unRegisterForConnectionStateChanges(Context context) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "unRegisterForConnectionStateChanges");
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "unRegisterForConnectionStateChanges");
        }
        if (sMmsSystemEventReceiver != null) {
            try {
                context.unregisterReceiver(sMmsSystemEventReceiver);
            } catch (IllegalArgumentException e) {
                // Allow un-matched register-unregister calls
            }
        }
    }

    /// M: new members and new methods
    /// M:Code analyze 001,add a new variable for save draft mms when received event of shutdown @{
    private OnShutDownListener mSaveDraft;
    /// @}
    /// M:Code analyze 002,add for listening the changing of sim info @{
    private OnSimInforChangedListener mSimInforChangedListener;
    /// @}
    /// M:Code analyze 008, add for gemini,start transaction service @{
    private static void wakeUpServiceGemini(Context context, int simId) {
        MmsLog.v(MmsApp.TXN_TAG, "wakeUpServiceGemini: start transaction service ... simId=" + simId);
        
        Intent it = new Intent(context, TransactionService.class);
        it.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, simId);

        context.startService(it);
    }
    /// @}

    /// M:Code analyze 009,add for setting the pending mms failed,mainly using after boot complete @{
    public static void setPendingMmsFailed(final Context context) {
        MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed");
        Cursor cursor = PduPersister.getPduPersister(context).getPendingMessages(
                Long.MAX_VALUE/*System.currentTimeMillis()*/);
        if (cursor != null) {
            try {
                int count = cursor.getCount();
                MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed: Pending Message Size=" + count);

                if (count == 0) {
                    return;
                }
                DefaultRetryScheme scheme = new DefaultRetryScheme(context, 100);
                ContentValues values = null;
                int columnIndex = 0;
                int columnType = 0;
                int id = 0;
                int type = 0;
                while (cursor.moveToNext()) {
                    columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
                    id = cursor.getInt(columnIndex);

                    columnType = cursor.getColumnIndexOrThrow(PendingMessages.MSG_TYPE);
                    type = cursor.getInt(columnType);

                    MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed: type=" + type + "; MsgId=" + id);

                    if (type == PduHeaders.MESSAGE_TYPE_SEND_REQ) {
                        values = new ContentValues(2);
                        values.put(PendingMessages.ERROR_TYPE,  MmsSms.ERR_TYPE_GENERIC_PERMANENT);
                        values.put(PendingMessages.RETRY_INDEX, scheme.getRetryLimit());
                        SqliteWrapper.update(context, 
                                context.getContentResolver(),
                                PendingMessages.CONTENT_URI,
                                values, PendingMessages._ID + "=" + id, null);
                    }
                }
            } catch (SQLiteDiskIOException e) {
                // Ignore
                MmsLog.e(MmsApp.TXN_TAG, "SQLiteDiskIOException caught while set pending message failed", e);
            } finally {
                cursor.close();
            }
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed: no pending MMS.");
        }
    }
    /// @}

    /// M:Code analyze 011,add for setting the mms being downloading when shutdown to unrecognized
    /// after boot complete again,have to manual download @{
    public static void setNotificationIndUnstarted(final Context context) {
        MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted");
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),Mms.CONTENT_URI,
                new String[] {Mms._ID,Mms.STATUS},
                Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND, null, null);
        if (cursor != null) {
            try {
                int count = cursor.getCount();
                MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted: Message Size=" + count);

                if (count == 0) {
                    return;
                }

                ContentValues values = null;
                int id = 0;
                int status = 0;
                while (cursor.moveToNext()) {
                    id = cursor.getInt(0);
                    status = cursor.getInt(1);
                    MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted: MsgId=" + id + "; status=" + status);

                    if (DownloadManager.STATE_DOWNLOADING == (status & ~DownloadManager.DEFERRED_MASK)) {
                        values = new ContentValues(1);
                        values.put(Mms.STATUS,  PduHeaders.STATUS_UNRECOGNIZED);
                        SqliteWrapper.update(context, 
                                context.getContentResolver(),
                                Mms.CONTENT_URI,
                                values, Mms._ID + "=" + id, null);
                    }
                }
            } catch (SQLiteDiskIOException e) {
                // Ignore
                MmsLog.e(MmsApp.TXN_TAG, "SQLiteDiskIOException caught while set notification ind unstart", e);
            } finally {
                cursor.close();
            }
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted: no pending messages.");
        }
    }
    /// @}

    /// M:Code analyze 002,new interface,add for listening the changing of sim info @{
    public interface OnSimInforChangedListener {
        void onSimInforChanged();
    }
    /// @}

    /// M:Code analyze 001,a new interface for listening the event of shut down @{
    public interface OnShutDownListener {
        void onShutDown();
    }
    /// @}
}
