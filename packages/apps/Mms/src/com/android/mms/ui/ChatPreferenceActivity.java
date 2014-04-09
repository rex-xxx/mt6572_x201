/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mms.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;

import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.ThreadSettings;
import android.telephony.SmsManager;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.transaction.CBMessagingNotification;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.WapPushMessagingNotification;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.ui.ConversationList.DeleteThreadListener;
import com.android.mms.util.MuteCache;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.mms.ipmessage.ChatManager;
import com.mediatek.mms.ipmessage.INotificationsListener;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class ChatPreferenceActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        INotificationsListener {
    private static final String TAG = "ChatPreferenceActivity";

    private static final boolean DEBUG = false;

    // Symbolic names for the keys used for preference lookup
    public static final String CHAT_WALLPAPER = "pref_key_chat_wallpaper_for_chat";

    public static final String CHAT_MUTE = "pref_key_mute_for_chat";

    public static final String CHAT_RINGTONE = "pref_key_ringtone_for_chat";

    public static final String CHAT_VIBRATE = "pref_key_vibrate_for_chat";

    public static final String DOWNLOAD_CHAT_HISTORY = "pref_key_download_chat_history";

    public static final String EMAIL_CHAT_HISTORY = "pref_key_email_chat_history";

    public static final String CLEAR_CHAT_HISTORY = "pref_key_clear_chat_history";

    public static final String ENABLE_NOTIFICATION = "pref_key_enable_notifications_for_chat";

    public static final String ACTION_CATEGORY = "pref_key_actions_settings";

    public static final String CHAT_THREAD_ID = "for_chat_settings_pref";

    public static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";

    public static final String CHAT_SETTINGS_URI = "content://mms-sms/thread_settings/";

    public static final String CHAT_MUTE_START = "chat_mute_start";

    public static final String TELEPHONYPROVIDER_WALLPAPER_ABSOLUTE_PATH =
                                    "/data/data/com.android.providers.telephony/app_wallpaper/";

    public static final String THREAD_ID_KEY = "chatThreadId";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS = 1;

    private CheckBoxPreference mEnableNotificationsPref;

    private CheckBoxPreference mVibratePref;

    private Preference mDownloadChatHistoryPref;

    private Preference mEmailChatHistoryPref;

    private Preference mClearChatHistoryPref;

    private Preference mChatWallpaperPref;

    private ListPreference mChatMutePref;

    private TelephonyManagerEx mTelephonyManager;

    private ProgressDialog mChatDialogUpdate;

    private ProgressDialog mChatDialogSave;

    private ProgressDialog mSaveChatHistory;;

    private ProgressDialog mSendEmail;

    private ProgressDialog mClearhistory;

    private long mChatThreadId;

    private static final int PICK_WALLPAPER = 2;

    private static final int PICK_GALLERY = 3;

    private static final int PICK_PHOTO = 4;

    private boolean mFromDownload = false;

    private boolean mFromSendEmail = false;

    private String mChatWallpaperUri = "";

    private boolean mOldNotificationEnable = false;

    private String mOldMuteValue = "0";

    private String mOldRingtone = DEFAULT_RINGTONE;

    private boolean mOldVibrate = false;

    private String mOldWallpaperUri = "";

    private long mOldMuteStart = 0;

    private String mWallpaperPathForCamera = "";

    private static final String SAVE_HISTORY_MIMETYPE_ZIP = "application/zip";

    private static final String SAVE_HISTORY_SUFFIX = ".zip";

    private static final String SAVE_HISTORY_MIMETYPE_TEXT = "text/plain";

    private static final int DELETE_CONVERSATION_TOKEN = 1801;

    private static final int HAVE_LOCKED_MESSAGES_TOKEN = 1;

    private static final int QUERY_THREAD_SETTINGS = 2;

    private ThreadListQueryHandler mQueryHandler = null;

    private Handler mShowQueryDialogHandler = null;

    private int[] mWallpaperImage = new int[] {R.drawable.wallpaper_launcher_wallpaper,
        R.drawable.wallpaper_launcher_gallery, R.drawable.wallpaper_launcher_camera,
        R.drawable.wallpaper_launcher_default,};

    private int[] mWallpaperText = new int[] {R.string.dialog_wallpaper_chooser_wallpapers,
        R.string.dialog_wallpaper_chooser_gallery, R.string.dialog_wallpaper_chooser_take,
        R.string.dialog_wallpaper_chooser_default};

    @Override
    public void onStart() {
        super.onStart();
        IpMessageUtils.addIpMsgNotificationListeners(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
    }

    @Override
    protected void onStop() {
        if (ChatPreferenceActivity.this.isFinishing()) {
            IpMessageUtils.removeIpMsgNotificationListeners(this, this);
        }
        if(mShowQueryDialogHandler != null) {
            mShowQueryDialogHandler.removeCallbacksAndMessages(null);
         }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        MmsLog.d(TAG, "mWallpaperPathForCamera: " + mWallpaperPathForCamera);
        outState.putString("wallpaperCameraPath",mWallpaperPathForCamera);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MmsLog.d(TAG, "onCreate");
        if (icicle != null && icicle.containsKey("wallpaperCameraPath")) {
           mWallpaperPathForCamera = icicle.getString("wallpaperCameraPath","");
        }
        mChatThreadId = getIntent().getExtras().getLong(THREAD_ID_KEY);
        MmsLog.d(TAG, "mChatThreadId " + mChatThreadId);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.pref_setting_chat));
        mShowQueryDialogHandler = new Handler();
        mShowQueryDialogHandler.postDelayed(new Runnable(){
            public void run() {
                showRefreshDialog();
            }
        } ,1000);
        mQueryHandler = new ThreadListQueryHandler(getContentResolver());
        startAsyncQuery();
    }

    private void showRefreshDialog() {
        /// M: fix bug ALPS00530331, use common string
        mChatDialogUpdate = ProgressDialog.show(ChatPreferenceActivity.this, null,
                    getResources().getString(R.string.chat_setting_updating));
    }

    private void startAsyncQuery() {
        String[] projection = new String[] {ThreadSettings.NOTIFICATION_ENABLE, ThreadSettings.MUTE, ThreadSettings.RINGTONE,
                ThreadSettings.VIBRATE, ThreadSettings.WALLPAPER, ThreadSettings.MUTE_START};
        Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), (int) mChatThreadId);
        mQueryHandler.startQuery(QUERY_THREAD_SETTINGS, null, uri, projection, null, null, null);
    }

    private void showPreference(Cursor c) {
        int mMute = 0;
        if (c == null) {
            MmsLog.d(TAG, "cursor is null.");
            return;
        }
        try {
            if (c.getCount() == 0) {
                MmsLog.d(TAG, "cursor count is 0");
            } else {
                c.moveToFirst();
                int notificationEnable = c.getInt(0);
                mOldNotificationEnable = (notificationEnable == 0 ? false : true);
                mMute = c.getInt(1);
                mOldRingtone = c.getString(2);
                int vibrate = c.getInt(3);
                mOldWallpaperUri = c.getString(4);
                mChatWallpaperUri = mOldWallpaperUri;
                mOldMuteStart = c.getLong(5);
                mOldVibrate = (vibrate == 0 ? false : true);
                MmsLog.d(TAG, "\tmute = " + mMute + "\tringtone = " + mOldRingtone + "\tvibrate = " + mOldVibrate);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sp.edit();
        if (mOldMuteStart > 0 && mMute > 0) {
            MmsLog.d(TAG, "thread mute timeout, reset to default.");
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            if ((mMute * 3600 + mOldMuteStart / 1000) <= currentTime) {
                MmsLog.d(TAG, "mute Overtime");
                editor.putLong(CHAT_MUTE_START, 0);
                editor.putString(CHAT_MUTE, "0");
                mOldMuteValue = "0";
                mOldMuteStart = 0;
                editor.commit();
            } else {
                MmsLog.d(TAG, "mute not Overtime");
                editor.putLong(CHAT_MUTE_START, mOldMuteStart);
                editor.putString(CHAT_MUTE, String.valueOf(mMute));
                mOldMuteValue = String.valueOf(mMute);
                editor.commit();
            }
        } else {
            editor.putLong(CHAT_MUTE_START, 0);
            editor.putString(CHAT_MUTE, "0");
            mOldMuteValue = "0";
            mOldMuteStart = 0;
            editor.commit();
        }
        editor.putBoolean(ENABLE_NOTIFICATION, mOldNotificationEnable);
        editor.putString(CHAT_WALLPAPER, mOldWallpaperUri);
        editor.putString(CHAT_RINGTONE, mOldRingtone);
        editor.putBoolean(CHAT_VIBRATE, mOldVibrate);
        editor.putLong(CHAT_THREAD_ID, mChatThreadId);
        editor.commit();
        setMessagePreferences();
        setEnabledNotificationsPref();
        setListPrefSummary();
        MmsLog.d(TAG, "SAVE IN XML");
        clearRefreshDialog();
    }

    private void clearRefreshDialog() {
        /// M: for flash screen.
        mChatHandler.sendEmptyMessage(0);
        if(mShowQueryDialogHandler != null) {
            mShowQueryDialogHandler.removeCallbacksAndMessages(null);
         }
    }

    Handler mChatHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 0:
                if (mChatDialogUpdate != null) {
                    mChatDialogUpdate.dismiss();
                }
                break;
            case 1:
                if (mChatDialogSave != null) {
                    mChatDialogSave.dismiss();
                }
                ChatPreferenceActivity.this.finish();
                break;
            case 2:
                ChatPreferenceActivity.this.finish();
                break;
            default:
                break;
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            saveToDatabase();
        }
        return false;
    }

    public void saveToDatabase() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean mNotificationEnable = sp.getBoolean(ENABLE_NOTIFICATION, false);
        final String mMute = sp.getString(CHAT_MUTE, "0");
        final String mRingtone = sp.getString(CHAT_RINGTONE, DEFAULT_RINGTONE);
        final boolean mVibrate = sp.getBoolean(CHAT_VIBRATE, false);
        final long mMuteStart = sp.getLong(CHAT_MUTE_START, 0);
        if (mOldMuteValue == null || mOldRingtone == null) {
            MmsLog.d(TAG, "mOldMuteValue = " + mOldMuteValue + "mOldRingtone = " + mOldRingtone);
            return;
        }
        if (mOldNotificationEnable == mNotificationEnable && mOldMuteValue.equals(mMute)
                && mOldRingtone.equals(mRingtone) && mOldVibrate == mVibrate && mOldMuteStart == mMuteStart) {
            MmsLog.d(TAG, "no setting change");
            mChatHandler.sendEmptyMessage(2);
        } else {
            /// M: fix bug ALPS00530331, use common string
            mChatDialogSave = ProgressDialog.show(ChatPreferenceActivity.this, null,
                                getResources().getString(R.string.chat_setting_saving));
            new Thread() {
                public void run() {
                    Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), mChatThreadId);
                    ContentValues values = new ContentValues();
                    values.put(ThreadSettings.NOTIFICATION_ENABLE, mNotificationEnable ? 1 : 0);
                    values.put(ThreadSettings.MUTE, Integer.parseInt(mMute));
                    values.put(ThreadSettings.MUTE_START, mMuteStart);
                    values.put(ThreadSettings.RINGTONE, mRingtone);
                    values.put(ThreadSettings.VIBRATE, mVibrate ? 1 : 0);
                    getContentResolver().update(uri, values, null, null);

                    if (MmsConfig.getFolderModeEnabled()) {
                        MuteCache.setMuteCache(mChatThreadId,Long.parseLong(mMute),mMuteStart,mNotificationEnable);
                    }
                    mChatHandler.sendEmptyMessage(1);
                }
            }.start();
        }
        changeThreadIdToDefault();
    }

    private void changeThreadIdToDefault() {
        SharedPreferences spChatThreadId = PreferenceManager.getDefaultSharedPreferences(ChatPreferenceActivity.this);
        SharedPreferences.Editor editor = spChatThreadId.edit();
        editor.putLong(CHAT_THREAD_ID, -1l);
        editor.commit();
    }

    private void setListPrefSummary() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // For notificationMute;
        String notificationMute = sp.getString(CHAT_MUTE, "0");
        mChatMutePref.setSummary(MessageUtils.getVisualTextName(this, notificationMute, R.array.pref_mute_choices,
                R.array.pref_mute_values));
    }

    private void setMessagePreferences() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            addPreferencesFromResource(R.xml.chatpreferences);
        } else {
            addPreferencesFromResource(R.xml.chatpreferences);
        }
        mChatMutePref = (ListPreference) findPreference(CHAT_MUTE);
        mChatMutePref.setOnPreferenceChangeListener(this);
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(ENABLE_NOTIFICATION);
        mVibratePref = (CheckBoxPreference) findPreference(CHAT_VIBRATE);
        mDownloadChatHistoryPref = findPreference(DOWNLOAD_CHAT_HISTORY);
        mEmailChatHistoryPref = findPreference(EMAIL_CHAT_HISTORY);
        mClearChatHistoryPref = findPreference(CLEAR_CHAT_HISTORY);
        if (!IpMessageUtils.getServiceManager(ChatPreferenceActivity.this).isFeatureSupported(
            IpMessageConsts.FeatureId.APP_SETTINGS)) {
            removeActionsCategory();
        } else if (!IpMessageUtils.getServiceManager(ChatPreferenceActivity.this).isFeatureSupported(
            IpMessageConsts.FeatureId.SAVE_CHAT_HISTORY)) {
            removeSaveAndEmail();
        }
        mChatWallpaperPref = findPreference(CHAT_WALLPAPER);
    }

    public void removeActionsCategory() {
        PreferenceCategory actionsPref = (PreferenceCategory) findPreference(ACTION_CATEGORY);
        getPreferenceScreen().removePreference(actionsPref);
    }

    public void removeSaveAndEmail() {
        PreferenceCategory displayOptions = (PreferenceCategory) findPreference(ACTION_CATEGORY);
        displayOptions.removePreference(findPreference(DOWNLOAD_CHAT_HISTORY));
        displayOptions.removePreference(findPreference(EMAIL_CHAT_HISTORY));
    }

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESTORE_DEFAULTS:
            restoreDefaultPreferences();
            return true;
        case android.R.id.home:
            // The user clicked on the Messaging icon in the action bar. Take them back from
            // wherever they came from
            saveToDatabase();
            return true;
        default:
            break;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);
        } else if (preference == mDownloadChatHistoryPref) {
            if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                AlertDialog.Builder downloadDialog = new AlertDialog.Builder(this);
                downloadDialog.setTitle(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_dialog_save_title));
                downloadDialog.setMessage(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_dialog_save_description));
                downloadDialog.setPositiveButton(R.string.OK, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mFromDownload = true;
                        mSaveChatHistory = ProgressDialog.show(ChatPreferenceActivity.this, null,
                            IpMessageUtils.getResourceManager(ChatPreferenceActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_chat_setting_saving));
                        ChatManager chatManager = IpMessageUtils.getChatManager(ChatPreferenceActivity.this);
                        chatManager.saveChatHistory(new long[] {mChatThreadId});
                        arg0.dismiss();
                    }
                });
                downloadDialog.setNegativeButton(R.string.Cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                    }
                });
                downloadDialog.create().show();
            } else {
                Toast.makeText(ChatPreferenceActivity.this, getResources().getString(R.string.no_sdcard_suggestion),
                    Toast.LENGTH_LONG).show();
            }
        } else if (preference == mEmailChatHistoryPref) {
            if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                AlertDialog.Builder emailDialog = new AlertDialog.Builder(this);
                emailDialog.setTitle(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_dialog_email_title));
                emailDialog.setMessage(IpMessageUtils.getResourceManager(this)
                    .getSingleString(IpMessageConsts.string.ipmsg_dialog_email_description));
                emailDialog.setPositiveButton(R.string.OK, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mFromSendEmail = true;
                        mSendEmail = ProgressDialog.show(ChatPreferenceActivity.this, null,
                            IpMessageUtils.getResourceManager(ChatPreferenceActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_chat_setting_sending));
                        ChatManager chatManager = IpMessageUtils.getChatManager(ChatPreferenceActivity.this);
                        chatManager.saveChatHistory(new long[] {mChatThreadId});
                        arg0.dismiss();
                    }
                });
                emailDialog.setNegativeButton(R.string.Cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                    }
                });
                emailDialog.create().show();
            } else {
                Toast.makeText(ChatPreferenceActivity.this, getResources().getString(R.string.no_sdcard_suggestion),
                    Toast.LENGTH_LONG).show();
            }
        } else if (preference == mClearChatHistoryPref) {
            mQueryHandler = new ThreadListQueryHandler(getContentResolver());
            confirmDeleteThread(mChatThreadId, mQueryHandler);
        } else if (preference == mChatWallpaperPref) {
            pickChatWallpaper();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreDefaultPreferences() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ChatPreferenceActivity.this).edit();
        editor.putBoolean(ENABLE_NOTIFICATION,true);
        editor.putString(CHAT_MUTE,"0");
        editor.putString(CHAT_RINGTONE, DEFAULT_RINGTONE);
        editor.putBoolean(CHAT_VIBRATE, true);
        editor.apply();
        clearWallpaperSingle();
        setPreferenceScreen(null);
        setMessagePreferences();
        setListPrefSummary();
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled = prefs.getBoolean(ChatPreferenceActivity.ENABLE_NOTIFICATION, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(ChatPreferenceActivity.ENABLE_NOTIFICATION, enabled);
        editor.apply();
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        final String key = arg0.getKey();
        String notificationMute = (String) arg1;
        if (CHAT_MUTE.equals(key)) {
            CharSequence mMute = MessageUtils.getVisualTextName(this, notificationMute, R.array.pref_mute_choices,
                R.array.pref_mute_values);
            mChatMutePref.setSummary(mMute);
            MmsLog.d(TAG, "preference change: " + mMute.toString());
            if (notificationMute.equals("0")) {
                MmsLog.d(TAG, "mute_start 0");
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ChatPreferenceActivity.this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putLong(CHAT_MUTE_START, 0);
                editor.commit();
            } else {
                MmsLog.d(TAG, "mute_start not 0");
                Long muteTime = System.currentTimeMillis();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ChatPreferenceActivity.this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putLong(CHAT_MUTE_START, muteTime);
                editor.commit();
            }
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged: newConfig = " + newConfig + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        this.getListView().clearScrapViewsIfNeeded();
    }

    public void pickChatWallpaper() {
        AlertDialog.Builder wallpaperDialog = new AlertDialog.Builder(this);
        ArrayList<HashMap<String, Object>> wallpaper = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < 4; i++) {
            HashMap<String, Object> hm = new HashMap<String, Object>();
            hm.put("ItemImage", mWallpaperImage[i]);
            hm.put("ItemText", getResources().getString(mWallpaperText[i]));
            wallpaper.add(hm);
        }
        SimpleAdapter wallpaperDialogAdapter = new SimpleAdapter(ChatPreferenceActivity.this, wallpaper,
                R.layout.wallpaper_item_each, new String[] {"ItemImage", "ItemText"}, new int[] {
                    R.id.wallpaperitemeachimageview, R.id.wallpaperitemeachtextview});
        LayoutInflater mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = mInflater.inflate(R.layout.wallpaper_chooser_gridview_dialog,
            (ViewGroup) findViewById(R.id.forwallpaperchooser));
        GridView gv = (GridView) layout.findViewById(R.id.wallpaperchooserdialog);
        gv.setAdapter(wallpaperDialogAdapter);
        final AlertDialog wallpaperChooser = wallpaperDialog.setTitle(
            getResources().getString(R.string.dialog_wallpaper_title)).setView(layout).create();
        wallpaperChooser.show();
        gv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                switch (arg2) {
                case 0:
                    pickSysWallpaper();
                    wallpaperChooser.dismiss();
                    break;
                case 1:
                    pickWallpaperFromGallery();
                    wallpaperChooser.dismiss();
                    break;
                case 2:
                    pickWallpaperFromCam();
                    wallpaperChooser.dismiss();
                    break;
                case 3:
                    new Thread() {
                        public void run() {
                            boolean isClearSingle = clearWallpaperSingle();
                            showSaveWallpaperResult(isClearSingle);
                        }
                    }.start();
                    wallpaperChooser.dismiss();
                    break;
                default:
                    break;
                }
            }
        });
    }

    private void pickSysWallpaper() {
        Intent intent = new Intent(this, WallpaperChooser.class);
        startActivityForResult(intent, PICK_WALLPAPER);
    }

    private void showSaveWallpaperResult(boolean isShow) {
        if (isShow) {
            ChatPreferenceActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.save_wallpaper_success),Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ChatPreferenceActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.save_wallpaper_fail),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void pickWallpaperFromSys(Intent data) {
        final int sourceId = data.getIntExtra("wallpaper_index", -1);
        MmsLog.d(TAG, "sourceId: " + sourceId);
        new Thread() {
            public void run() {
                boolean isSaveForSystem = saveResourceWallpaperToMemory(sourceId);
                showSaveWallpaperResult(isSaveForSystem);
            }
        }.start();
    }

    private boolean saveResourceWallpaperToMemory(int resourceId) {
        InputStream is = null;
        Resources r = getResources();
        try {
            is = r.openRawResource(resourceId);
        } catch (NotFoundException e) {
            MmsLog.d(TAG, "NotFoundException", e);
        }
        Bitmap bm = BitmapFactory.decodeStream(is);
        String resourceWallpaper = TELEPHONYPROVIDER_WALLPAPER_ABSOLUTE_PATH + mChatThreadId
            + ".jpeg";
        Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), mChatThreadId);
        ContentValues values = new ContentValues();
        values.put(ThreadSettings.WALLPAPER, resourceWallpaper);
        getContentResolver().update(uri, values, null, null);
        boolean isSaveSucccessed = false;
        try {
            OutputStream o = getContentResolver().openOutputStream(uri);
            isSaveSucccessed = bm.compress(Bitmap.CompressFormat.JPEG, 100, o);
            MmsLog.d(TAG, "decodeFile over");
            if (o != null) {
                o.close();
            }
            if (bm != null) {
                bm.recycle();
            }
        } catch (FileNotFoundException e) {
            MmsLog.d(TAG, "FileNotFoundException", e);
        } catch (IOException e) {
            MmsLog.d(TAG, "IOException", e);
        } finally {
            return isSaveSucccessed;
        }
    }

    private void pickWallpaperFromCam() {
        if (getSDCardPath(this) != null) {
            MmsLog.d(TAG, "SDcard esisted ");
            mWallpaperPathForCamera = getSDCardPath(this) + File.separator + "Message_WallPaper" + File.separator
                + mChatThreadId + "_" + System.currentTimeMillis() + ".jpeg";
            File out = new File(mWallpaperPathForCamera);
            if (!out.getParentFile().exists()) {
                out.getParentFile().mkdirs();
            }
            Uri mWallpaperTakeuri = Uri.fromFile(out);
            Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mWallpaperTakeuri);
            MmsLog.d(TAG, "MediaStoreUri: " + mWallpaperTakeuri);
            try {
                startActivityForResult(imageCaptureIntent, PICK_PHOTO);
            } catch (ActivityNotFoundException e) {
                MmsLog.w(TAG, "activity not found!");
            }
        } else {
            MmsLog.d(TAG, "SDcard not esisted ");
            Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                startActivityForResult(imageCaptureIntent, PICK_PHOTO);
            } catch (ActivityNotFoundException e) {
                MmsLog.w(TAG, "activity not found!");
            }
        }
    }

    private void pickWallpaperFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "Gallery"), PICK_GALLERY);
        } catch (ActivityNotFoundException e) {
            MmsLog.w(TAG, "activity not found!");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case PICK_GALLERY:
                pickWallpaperFromGalleryResult(data);
                break;
            case PICK_PHOTO:
                pickWallpaperFromCamResult();
                break;
            case PICK_WALLPAPER:
                pickWallpaperFromSys(data);
                MmsLog.d(TAG, "sytem result");
                break;
            default:
                break;
            }
            return;
        } else if (resultCode == RESULT_CANCELED) {
            MmsLog.d(TAG, "nothing selected");
            return;
        }
    }

    private void pickWallpaperFromGalleryResult(final Intent data) {
        if (null == data) {
            return;
        }
        Uri mChatWallpaperGalleryUri = data.getData();
        Cursor c = getContentResolver().query(mChatWallpaperGalleryUri, new String[] {MediaStore.Images.Media.DATA},
            null, null, null);
        if (c == null) {
            return;
        }
        String wallpaperPathForGallery = "";
        try {
            if (c.getCount() == 0) {
                return;
            } else {
                c.moveToFirst();
                wallpaperPathForGallery = c.getString(0);
            }
        } finally {
            c.close();
        }
        MmsLog.d(TAG, "Save wallpaper Gallery Uri: " + wallpaperPathForGallery);
        final String chatWallpaperCompressForGallery = compressAndRotateForMemory(wallpaperPathForGallery);
        new Thread() {
            public void run() {
                boolean isSaveForGallery = saveWallpaperToMemory(chatWallpaperCompressForGallery);
                showSaveWallpaperResult(isSaveForGallery);
            }
        }.start();
        return;
    }

    private void pickWallpaperFromCamResult() {
        final String chatWallpaperCompressForCamera = compressAndRotateForMemory(mWallpaperPathForCamera);
        new Thread() {
            public void run() {
                boolean isSaveForCamera = saveWallpaperToMemory(chatWallpaperCompressForCamera);
                showSaveWallpaperResult(isSaveForCamera);
            }
        }.start();
        return;
    }

    public boolean clearWallpaperSingle() {
        ContentValues cv = new ContentValues();
        cv.put(ThreadSettings.WALLPAPER, "");
        Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), mChatThreadId);
        int i = getContentResolver().update(uri, cv, null, null);
        if (i > 0) {
            return true;
        }
        return false;
    }

    private boolean saveWallpaperToMemory(String oldWallpaper) {
        Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), mChatThreadId);
        ContentValues values = new ContentValues();
        String tempFileName = "";
        tempFileName = TELEPHONYPROVIDER_WALLPAPER_ABSOLUTE_PATH + mChatThreadId + ".jpeg";
        values.put(ThreadSettings.WALLPAPER, tempFileName);
        getContentResolver().update(uri, values, null, null);
        boolean isSaveSucceed = false;
        try {
            OutputStream o = getContentResolver().openOutputStream(uri);
            Bitmap bm = BitmapFactory.decodeFile(oldWallpaper);
            isSaveSucceed = bm.compress(Bitmap.CompressFormat.JPEG, 100, o);
            MmsLog.d(TAG, "decodeFile over");
            if (o != null) {
                o.close();
            }
            if (bm != null) {
                bm.recycle();
            }
            File tempFile = new File(oldWallpaper);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        } catch (FileNotFoundException e) {
            MmsLog.d(TAG, "FileNotFoundException", e);
        } catch (IOException e) {
            MmsLog.d(TAG, "IOException", e);
        } finally {
            return isSaveSucceed;
        }
    }

    private String compressAndRotateForMemory(String wallpaperCache) {
        File chatWallpaperPStore = new File(wallpaperCache);
        String chatWallpaperUri = mChatWallpaperUri;
        if (chatWallpaperPStore.exists()) {
            File mChatWallpaperMemory =
                    new File(chatWallpaperPStore.getParent(), mChatThreadId + "_" + System.currentTimeMillis() + ".jpeg");
            try {
                if (!mChatWallpaperMemory.exists()) {
                    mChatWallpaperMemory.createNewFile();
                }
            } catch (IOException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, IOException");
            }
            MmsLog.d(TAG, "mChatWallpapterMemory " + mChatWallpaperMemory.getName());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mChatWallpaperMemory);
            } catch (FileNotFoundException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, FileNotFoundException");
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(wallpaperCache, options);
            options.inJustDecodeBounds = false;
            int wallpaperWidth = options.outWidth;
            int wallpaperHeight = options.outHeight;
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            int mCurrentMaxWidth = windowManager.getDefaultDisplay().getWidth();
            int mCurrentMaxHeight = windowManager.getDefaultDisplay().getHeight();
            int be = 1;
            if (mCurrentMaxHeight > mCurrentMaxWidth) {
                be = Math.max(wallpaperHeight / mCurrentMaxHeight, wallpaperWidth / mCurrentMaxWidth);
            } else {
                be = Math.max(wallpaperHeight / mCurrentMaxWidth, wallpaperWidth / mCurrentMaxHeight);
            }
            MmsLog.d(TAG, "be: " + be);
            if (be < 1) {
                be = 1;
            }
            options.inSampleSize = be;
            int degree = 0;
            int orientation = 0;
            boolean isCopyed = false;
            try {
                ExifInterface exif = new ExifInterface(wallpaperCache);
                if (exif != null) {
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
                    degree = UriImage.getExifRotation(orientation);
                }
            } catch (IOException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, IOException");
            }
            Bitmap bm = BitmapFactory.decodeFile(wallpaperCache, options);
            if (bm != null) {
                bm = UriImage.rotate(bm, degree);
                isCopyed = bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            }
            try {
                if (fos != null) {
                    fos.close();
                }
                if (bm != null) {
                    bm.recycle();
                }
            } catch (IOException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, IOException");
            }
            MmsLog.d(TAG, "isCopyed: " + isCopyed);
            if (!isCopyed) {
                chatWallpaperUri = chatWallpaperPStore.getPath();
                return chatWallpaperUri;
            }
            chatWallpaperUri = mChatWallpaperMemory.getPath();
        }
        return chatWallpaperUri;
    }

    public static String getSDCardPath(Context c) {
        File sdDir = null;
        String sdStatus = Environment.getExternalStorageState();
        if (sdStatus != null) {
            boolean sdCardExist = sdStatus.equals(android.os.Environment.MEDIA_MOUNTED);
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory();
                return sdDir.toString();
            } else {
                return null;
            }
        }
        return null;
    }

    @Override
    public void notificationsReceived(Intent intent) {
        MmsLog.d(TAG, "save history intent = " + intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (IpMessageUtils.getActionTypeByAction(action)) {
        case IpMessageUtils.IPMSG_SAVE_HISTORY_ACTION:
            int done = intent.getIntExtra(IpMessageConsts.SaveHistroy.SAVE_HISTRORY_DONE, 1);
            final String filePath = intent.getStringExtra(IpMessageConsts.SaveHistroy.DOWNLOAD_HISTORY_FILE);
            MmsLog.d(TAG, "save history done: " + done);
            MmsLog.d(TAG, "save history file: " + filePath);
            if (mFromDownload) {
                if (mSaveChatHistory != null) {
                    mSaveChatHistory.dismiss();
                }
            }
            if (done == 0 && mFromDownload) {
                mFromDownload = false;
                ChatPreferenceActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(
                            getApplicationContext(),
                            getResources().getString(R.string.pref_download_chat_history) + ","
                                + getResources().getString(R.string.save_history_file) + " " + filePath,
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (done != 0 && mFromDownload) {
                mFromDownload = false;
                if (filePath != null) {
                    File saveHistoryFile = new File(filePath);
                    if (saveHistoryFile.exists()) {
                        saveHistoryFile.delete();
                    }
                }
                ChatPreferenceActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                            IpMessageUtils.getResourceManager(ChatPreferenceActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_save_chat_history_failed),
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if (mFromSendEmail) {
                if (mSendEmail != null) {
                    mSendEmail.dismiss();
                }
            }
            if (done == 0 && mFromSendEmail) {
                mFromSendEmail = false;
                MmsLog.d(TAG, "send file: " + filePath);
                if (filePath != null) {
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
                            MmsLog.d(TAG, "ActivityNotFoundException.");
                        }
                    }
                } else {
                    ChatPreferenceActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                IpMessageUtils.getResourceManager(ChatPreferenceActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_send_chat_history_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else if (done != 0 && mFromSendEmail) {
                mFromSendEmail = false;
                if (filePath != null) {
                    File sendHistoryFile = new File(filePath);
                    if (sendHistoryFile.exists()) {
                        sendHistoryFile.delete();
                    }
                }
                ChatPreferenceActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                            IpMessageUtils.getResourceManager(ChatPreferenceActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_send_chat_history_failed),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            break;
        default:
            break;
        }
    }

    private final class ThreadListQueryHandler extends BaseProgressQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // / M: add cursor == null check
            if (cursor == null) {
                return;
            }
            switch (token) {
            case QUERY_THREAD_SETTINGS:
                showPreference(cursor);
                break;
            case HAVE_LOCKED_MESSAGES_TOKEN:
                // / M: add a log
                MmsLog.d(TAG, "onQueryComplete HAVE_LOCKED_MESSAGES_TOKEN");
                @SuppressWarnings("unchecked")
                Collection<Long> threadIds = (Collection<Long>) cookie;
                DeleteThreadListener d = new DeleteThreadListener(threadIds, mQueryHandler,
                        ChatPreferenceActivity.this);
                confirmDeleteThreadDialog(d, threadIds, cursor != null && cursor.getCount() > 0,
                    ChatPreferenceActivity.this);
                if (cursor != null) {
                    cursor.close();
                }
                break;
            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token) {
            case DELETE_CONVERSATION_TOKEN:
                Contact.init(ChatPreferenceActivity.this);
                Conversation.init(ChatPreferenceActivity.this);
                try {
                    ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    if (phone != null) {
                        if (phone.isTestIccCard()) {
                            MmsLog.d(TAG, "All threads has been deleted, send notification..");
                            EncapsulatedSmsManager.setSmsMemoryStatus(true);
                        }
                    } else {
                        MmsLog.d(TAG, "Telephony service is not available!");
                    }
                } catch (RemoteException ex) {
                    MmsLog.e(TAG, " " + ex.getMessage());
                }
                // Update the notification for new messages since they
                // may be deleted.
//                MessagingNotification.nonBlockingUpdateNewMessageIndicator(ChatPreferenceActivity.this, false, false);
                // TODO: ip message
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(ChatPreferenceActivity.this,
                                                                            MessagingNotification.THREAD_NONE, false);
                // Update the notification for failed messages since they
                // may be deleted.
                // TODO: ip message
                MessagingNotification.updateSendFailedNotificationForThread(ChatPreferenceActivity.this, mChatThreadId);
                MessagingNotification.updateDownloadFailedNotification(ChatPreferenceActivity.this);
                // Update the notification for new WAP Push messages
                if (EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT) {
//                    WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(ChatPreferenceActivity.this, false);
                    // TODO: ip message
                    WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(ChatPreferenceActivity.this,
                                                                                WapPushMessagingNotification.THREAD_NONE);
                }
                CBMessagingNotification.updateNewMessageIndicator(ChatPreferenceActivity.this);
                // Make sure the list reflects the delete
                // startAsyncQuery();
                dismissProgressDialog();
                Intent i = new Intent(ChatPreferenceActivity.this, ConversationList.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                ChatPreferenceActivity.this.finish();
                break;
            default:
                break;
            }
        }
    }

    public void confirmDeleteThread(long threadId, AsyncQueryHandler handler) {
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
     *
     * @param threadIds
     *            list of threadIds to delete or null for all threads
     * @param handler
     *            query handler to do the background locked query
     */
    public void confirmDeleteThreads(Collection<Long> threadIds, AsyncQueryHandler handler) {
        Conversation.startQueryHaveLockedMessages(handler, threadIds, HAVE_LOCKED_MESSAGES_TOKEN);
    }

    /**
     * Build and show the proper delete thread dialog. The UI is slightly different
     * depending on whether there are locked messages in the thread(s) and whether we're
     * deleting single/multiple threads or all threads.
     *
     * @param listener
     *            gets called when the delete button is pressed
     * @param deleteAll
     *            whether to show a single thread or all threads UI
     * @param hasLockedMessages
     *            whether the thread(s) contain locked messages
     * @param context
     *            used to load the various UI elements
     */
    public void confirmDeleteThreadDialog(final DeleteThreadListener listener, Collection<Long> threadIds,
            boolean hasLockedMessages, Context context) {
        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(IpMessageUtils.getResourceManager(this)
            .getSingleString(IpMessageConsts.string.ipmsg_dialog_clear_description));
        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        if (!hasLockedMessages) {
            checkbox.setVisibility(View.GONE);
        } else {
            // / M: change the string to important if ipmessage plugin exist
            MmsLog.d(TAG, "serviceId:" + MmsConfig.getIpMessagServiceId(context));
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
        }
        Cursor cursor = null;
        int smsId = 0;
        int mmsId = 0;
        cursor = context.getContentResolver().query(Sms.CONTENT_URI, new String[] {"max(_id)"}, null, null, null);
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
        cursor = context.getContentResolver().query(Mms.CONTENT_URI, new String[] {"max(_id)"}, null, null, null);
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
        MmsLog.d(TAG, "Alertdialog shows");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_dialog_clear_title))
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setCancelable(true)
            .setPositiveButton(R.string.delete, listener)
            .setNegativeButton(R.string.no, null)
            .setView(contents)
            .show();
    }
}
