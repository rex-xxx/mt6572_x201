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

package com.mediatek.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CellBroadcastSettings extends TimeConsumingPreferenceActivity
        implements Preference.OnPreferenceClickListener,
        DialogInterface.OnClickListener {

    private static final String LOG_TAG = "Settings/CellBroadcastSettings";
    private static final boolean DBG = true;//(PhoneApp.DBG_LEVEL >= 2);
    private static final int MESSAGE_GET_CONFIG = 100;
    private static final int MESSAGE_SET_CONFIG = 101;

    private static final int MENU_CHANNEL_ENABLE_DISABLE = 10;
    private static final int MENU_CHANNEL_EDIT = 11;
    private static final int MENU_CHANNEL_DELETE = 12;
    private static final int CHANNEL_NAME_LENGTH = 20;

    private static final String KEY_LANGUAGE = "button_language";
    private static final String KEY_ADD_CHANNEL = "button_add_channel";
    private static final String KEY_CHANNEL_LIST = "menu_channel_list";

    private static final String KEYID = "_id";
    private static final String NAME = "name";
    private static final String NUMBER = "number";
    private static final String ENABLE = "enable";
    private static final Uri CHANNEL_URI = Uri.parse("content://cb/channel");
    private static final Uri CHANNEL_URI1 = Uri.parse("content://cb/channel1");
    private static final Uri CHANNEL_URI2 = Uri.parse("content://cb/channel2");
    private static final Uri CHANNEL_URI3 = Uri.parse("content://cb/channel3");
    
    private Uri mUri = CHANNEL_URI;

    private PreferenceScreen mLanguagePreference;
    private PreferenceScreen mAddChannelPreference;
    private PreferenceCategory mChannelListPreference;

    private MyHandler mHandler = new MyHandler();
    private Phone mPhone;
    private static final int LANGUAGE_NUM = 22;// the number of language,include "All languages"
    private static final int CB_MAX_CHANNEL = 65535;// the number of language,include "All languages"

    private ArrayList<CellBroadcastLanguage> mLanguageList = new ArrayList<CellBroadcastLanguage>();
    private HashMap<String, CellBroadcastLanguage> mLanguageMap;
    private ArrayList<CellBroadcastChannel> mChannelArray = new ArrayList<CellBroadcastChannel>();
    private HashMap<String, CellBroadcastChannel> mChannelMap;

    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Xlog.d(LOG_TAG, "[action = " + action + "]");
            if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                setScreenEnabled();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CallSettings.isMultipleSim()) {
            mSimId = getIntent().getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
            switch (mSimId){
                case PhoneConstants.GEMINI_SIM_1:
                    mUri = CHANNEL_URI;
                break;
                case PhoneConstants.GEMINI_SIM_2:
                    mUri = CHANNEL_URI1;
                break;
                case PhoneConstants.GEMINI_SIM_3:
                    mUri = CHANNEL_URI2;
                break;
                case PhoneConstants.GEMINI_SIM_4:
                    mUri = CHANNEL_URI3;
                break;
                default:
                    Xlog.d(LOG_TAG,"error no sim id matched with mSimId = " + mSimId);
                break;
            }
        }
        Xlog.d("CellBroadcastSetting", "Sim Id : " + mSimId);        

        mPhone = PhoneGlobals.getPhone();
        addPreferencesFromResource(R.xml.cell_broadcast_settings);
        initPreference();
        initLanguage();
        registerForContextMenu(this.getListView());

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE); 
        registerReceiver(mReceiver, intentFilter);
    }

    public void onResume() {
        super.onResume();
        getCellBroadcastConfig(true);
    }
    
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void initPreference() {
        mLanguagePreference = (PreferenceScreen) findPreference(KEY_LANGUAGE);
        mAddChannelPreference = (PreferenceScreen) findPreference(KEY_ADD_CHANNEL);
        mChannelListPreference = (PreferenceCategory) findPreference(KEY_CHANNEL_LIST);
        mLanguagePreference.setOnPreferenceClickListener(this);
        mAddChannelPreference.setOnPreferenceClickListener(this);
    }

    // update the channel list ui by channel array
    private void updateChannelUIList() {
        mChannelListPreference.removeAll();
        int length = mChannelArray.size();
        for (int i = 0; i < length; i++) {
            Preference channel = new Preference(this);
            int keyId = mChannelArray.get(i).getKeyId();
            String channelName = mChannelArray.get(i).getChannelName();
            int channelId = mChannelArray.get(i).getChannelId();
            boolean channelState = mChannelArray.get(i).getChannelState();
            String title = channelName + "(" + String.valueOf(channelId) + ")";
            channel.setTitle(title);
            final CellBroadcastChannel oldChannel = new CellBroadcastChannel(keyId, channelId, channelName, channelState);
            if (channelState) {
                channel.setSummary(R.string.enable);
            } else {
                channel.setSummary(R.string.disable);
            }

            channel.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    showEditChannelDialog(oldChannel);
                    return false;
                }
            });
            mChannelListPreference.addPreference(channel);
        }
    }

    private void updateLanguageSummary() {
        boolean[] temp = new boolean[LANGUAGE_NUM];
        int tLength = temp.length;
        for (int i = 1; i < tLength ; i++) {
            temp[i] = mLanguageList.get(i).getLanguageState();
        }
        setLanguageSummary(temp);
    }

    private void setLanguageSummary(boolean[] temp) {
        if (temp == null) {
            return;
        }
        boolean allLanguagesFlag = true;
        int tLength = temp.length;
        for (int i = 1; i < tLength; i++) {
            if (!temp[i]) {
                allLanguagesFlag = false;// not select all languages
                break;
            }
        }
        temp[0] = allLanguagesFlag;
        if (temp[0]) {
            mLanguagePreference.setSummary(R.string.cb_all_languages);
            return;
        }
        int flag = 0;
        String summary = "";
        int lastIndex = -1;
        for (int i = 1; i < tLength; i++) {
            if (temp[i] && flag < 2) {
                summary += mLanguageList.get(i).getLanguageName() + " ";
                flag++;
                lastIndex = i;
            }
            if (temp[i] && i > lastIndex && lastIndex != -1) {
                summary += " ...";
                break;
            }
        }
        mLanguagePreference.setSummary(summary);
    }

    private boolean insertChannelToDatabase(CellBroadcastChannel channel) {
        ContentValues values = new ContentValues();
        values.put(NAME, channel.getChannelName());
        values.put(NUMBER, channel.getChannelId());
        values.put(ENABLE, channel.getChannelState());
        try {
            Uri uri = getContentResolver().insert(mUri, values);
            int insertId = Integer.valueOf(uri.getLastPathSegment());
            channel.setKeyId(insertId);
            Xlog.d(LOG_TAG, "insertChannelToDatabase(), insertId: " + insertId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private void showUpdateDBErrorInfoDialog() {
         onError(mLanguagePreference, EXCEPTION_ERROR);
    }

    // reason=true means get config when init.
    // reason=false means get config after update
    private void getCellBroadcastConfig(boolean reason) {
        Message msg;
        if (reason) {
            msg = mHandler.obtainMessage(MESSAGE_GET_CONFIG, 0,MESSAGE_GET_CONFIG, null);
        } else {
            msg = mHandler.obtainMessage(MESSAGE_GET_CONFIG, 0,MESSAGE_SET_CONFIG, null);
        }

        if (CallSettings.isMultipleSim()) {
            ((GeminiPhone)mPhone).getCellBroadcastSmsConfigGemini(msg, mSimId);
        } else {
            mPhone.getCellBroadcastSmsConfig(msg);
        }

        if (reason) {
            onStarted(mLanguagePreference, reason);
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference.equals(mLanguagePreference)) {
            showLanguageSelectDialog();
            return true;
        } else if (preference.equals(mAddChannelPreference)) {
            showAddChannelDialog();
            return true;
        }
        return false;
    }

    private void showAddChannelDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View setView = inflater.inflate(R.layout.pref_add_channel, null);
        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(setView);
        builder.setTitle(R.string.cb_menu_add_channel);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText channelName = (EditText) setView
                        .findViewById(R.id.edit_channel_name);
                EditText channelNum = (EditText) setView
                        .findViewById(R.id.edit_channel_number);
                CheckBox channelState = (CheckBox) setView
                        .findViewById(R.id.checkbox_channel_enable);
                String name = channelName.getText().toString();
                String num = channelNum.getText().toString();
                boolean checked = channelState.isChecked();
                // check input
                String errorInfo = "";
                if (!checkChannelName(name)) {
                    errorInfo += getString(R.string.cb_error_channel_name);
                }
                if (!checkChannelNumber(num)) {
                    errorInfo += "\n" + getString(R.string.cb_error_channel_num);
                }
                if (errorInfo.equals("")) {
                    int channelId = Integer.valueOf(num).intValue();
                    if (!checkChannelIdExist(channelId)) {
                        dialog.dismiss();
                        CellBroadcastChannel channel = new CellBroadcastChannel(channelId, name, checked);
                        SmsBroadcastConfigInfo[] objectList = makeChannelConfigArray(channel);
                        if (insertChannelToDatabase(channel)) {
                            mChannelArray.add(channel);
                            mChannelMap.put(String.valueOf(channel.getChannelId()), channel);
                            updateChannelUIList();
                            if (channel.getChannelState()) {
                                setCellBroadcastConfig(objectList);
                            }
                        } else {
                            showUpdateDBErrorInfoDialog();
                        }
                    } else {
                        displayMessage(R.string.cb_error_channel_id_exist);
                    }
                } else {
                    displayMessage(errorInfo);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        dialog = builder.create();
        requestInputMethod(dialog);
        dialog.show();
    }

    // convert operation info to SmsBroadcastConfigInfo[]
    private SmsBroadcastConfigInfo[] makeChannelConfigArray(CellBroadcastChannel channel) {
        SmsBroadcastConfigInfo[] objectList = new SmsBroadcastConfigInfo[1];
        int tChannelId = channel.getChannelId();
        objectList[0] = new SmsBroadcastConfigInfo(tChannelId,tChannelId, -1, -1, channel.getChannelState());
        return objectList;
    }

    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    
    private void showEditChannelDialog(final CellBroadcastChannel oldChannel) {
        int keyId = oldChannel.getKeyId();
        int cid = oldChannel.getChannelId();
        String cname = oldChannel.getChannelName();
        boolean checked = oldChannel.getChannelState();
        LayoutInflater inflater = LayoutInflater.from(this);
        final View setView = inflater.inflate(R.layout.pref_add_channel, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(setView);
        builder.setTitle(R.string.cb_channel_dialog_edit_channel);
        final EditText channelName = (EditText) setView
                .findViewById(R.id.edit_channel_name);
        final EditText channelNum = (EditText) setView
                .findViewById(R.id.edit_channel_number);
        final CheckBox channelState = (CheckBox) setView
                .findViewById(R.id.checkbox_channel_enable);
        channelName.setText(cname);
        channelNum.setText(String.valueOf(cid));
        channelState.setChecked(checked);
        final AlertDialog dialog;
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = channelName.getText().toString();
                String num = channelNum.getText().toString();
                boolean checked = channelState.isChecked();
                String errorInfo = "";
                if (!checkChannelName(name)) {
                    errorInfo += getString(R.string.cb_error_channel_name);
                }
                if (!checkChannelNumber(num)) {
                    errorInfo += "\n" + getString(R.string.cb_error_channel_num);
                }
                if (errorInfo.equals("")) {
                    int newChannelId = Integer.valueOf(num).intValue();
                    int tempOldChannelId = oldChannel.getChannelId();
                    if (!checkChannelIdExist(newChannelId, oldChannel.getKeyId())) {
                        dialog.dismiss();
                        CellBroadcastChannel newChannel = 
                            new CellBroadcastChannel(oldChannel.getKeyId(), newChannelId, name,checked);
                        oldChannel.setChannelState(false);
                        int tempNewChannelId = newChannel.getChannelId();
                        SmsBroadcastConfigInfo[] objectList = new SmsBroadcastConfigInfo[2];
                        objectList[0] = new SmsBroadcastConfigInfo(tempOldChannelId, 
                                tempOldChannelId, -1, -1,false);
                        objectList[1] = new SmsBroadcastConfigInfo(tempNewChannelId, 
                                tempNewChannelId, -1, -1,newChannel.getChannelState());
                        if (updateChannelToDatabase(oldChannel, newChannel)) {
                            setCellBroadcastConfig(objectList);
                        } else {
                            showUpdateDBErrorInfoDialog();
                        }
                    } else {
                        displayMessage(getString(R.string.cb_error_channel_id_exist));
                    }
                } else {
                    displayMessage(errorInfo);
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        dialog = builder.create();
        dialog.show();
        requestInputMethod(dialog);
    }

    private void initChannelMap() {
        mChannelMap = new HashMap<String, CellBroadcastChannel>();
        int tSize = mChannelArray.size();
        for (int i = 0; i < tSize; i++) {
            int id = mChannelArray.get(i).getChannelId();
            mChannelMap.put(String.valueOf(id), mChannelArray.get(i));
        }
    }

    private void clearChannel() {
        if (mChannelArray != null) {
            mChannelArray.clear();
        }
    }

    private boolean queryChannelFromDatabase() {
        clearChannel();
        String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE };
        try {
            Cursor cursor = this.getContentResolver().query(mUri,projection, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    CellBroadcastChannel channel = new CellBroadcastChannel();
                    channel.setChannelId(cursor.getInt(2));
                    channel.setKeyId(cursor.getInt(0));
                    channel.setChannelName(cursor.getString(1));
                    channel.setChannelState(cursor.getInt(3) == 1);
                    mChannelArray.add(channel);
                }
                cursor.close();
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private boolean updateChannelToDatabase(CellBroadcastChannel oldChannel,CellBroadcastChannel newChannel) {
        String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE };
        final int id = newChannel.getKeyId();
        final String name = newChannel.getChannelName();
        final boolean enable = newChannel.getChannelState();
        final int number = newChannel.getChannelId();
        ContentValues values = new ContentValues();
        values.put(KEYID, id);
        values.put(NAME, name);
        values.put(NUMBER, number);
        values.put(ENABLE, Integer.valueOf(enable ? 1 : 0));
        String where = KEYID + "=" + oldChannel.getKeyId();
        try {
            int lines = this.getContentResolver().update(mUri, values,where, null);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private boolean deleteChannelFromDatabase(CellBroadcastChannel oldChannel) {
        String where = NUMBER + "=" + oldChannel.getChannelId();
        try {
            getContentResolver().delete(mUri, where, null);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private void updateChannelsWithSingleConfig(SmsBroadcastConfigInfo info) {
        int channelBeginIndex = info.getFromServiceId();
        int channelEndIndex = info.getToServiceId();
        boolean state = info.isSelected();
        Xlog.d(LOG_TAG, "updateChannelsWithSingleConfig STATE = " + state);
        
        if (channelBeginIndex != -1) {
            for (int j = channelBeginIndex; j <= channelEndIndex; j++) {
                String jStr = String.valueOf(j);
                CellBroadcastChannel channel = getChannelObjectFromKey(jStr);
                if (channel != null) {
                    channel.setChannelState(state);
                } else {
                    // add a new channel to dataBase while the channel doesn't exists
                    String tName = getString(R.string.cb_default_new_channel_name) + jStr;
                    CellBroadcastChannel newChannel = new CellBroadcastChannel(j,tName, state);
                    if (!insertChannelToDatabase(newChannel)) {
                        showUpdateDBErrorInfoDialog();
                    }
                    mChannelArray.add(newChannel);
                    mChannelMap.put(jStr,newChannel);
                }
            }
        }
    }

    private void updateLanguagesWithSingleConfig(SmsBroadcastConfigInfo info) {
        int languageBeginIndex = info.getFromCodeScheme();
        int languageEndIndex = info.getToCodeScheme();
        if (languageBeginIndex != -1 && languageBeginIndex != -2) {
            for (int j = languageBeginIndex; j <= languageEndIndex; j++) {
                CellBroadcastLanguage language = getLanguageObjectFromKey(String.valueOf(j));
                if (language != null) {
                    language.setLanguageState(info.isSelected());
                }
            }
        } else {
            Xlog.d(LOG_TAG, "Select all language!");
            if (languageBeginIndex == -2 && languageEndIndex == -2) {
                for (int i = 0; i < mLanguageList.size(); i++) {
                    CellBroadcastLanguage language = (CellBroadcastLanguage)mLanguageList.get(i);
                    CellBroadcastLanguage lang = getLanguageObjectFromKey(String.valueOf(language.getLanguageId()));
                    if (lang != null) {
                        lang.setLanguageState(true);
                    }
                }
            }
        }
    }

    private void updateCurrentChannelAndLanguage(ArrayList<SmsBroadcastConfigInfo> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        int number = list.size();
        for (int i = 0; i < number; i++) {
            SmsBroadcastConfigInfo info = list.get(i);
            updateLanguagesWithSingleConfig(info);
            dumpConfigInfo(info);
            updateChannelsWithSingleConfig(info);
        }
    }

    private void dumpConfigInfo(SmsBroadcastConfigInfo info) {
        Xlog.d(LOG_TAG, "dump start for " + info.toString());
        Xlog.d(LOG_TAG, "FromServiceId " + info.getFromServiceId() 
                + "  ToServiceId " + info.getToServiceId());
        Xlog.d(LOG_TAG, "FromCodeId " + info.getFromCodeScheme() 
                + "  ToCodeId " + info.getToCodeScheme());
        Xlog.d(LOG_TAG, "dump end for " + info.toString());
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_CONFIG:
                handleGetCellBroadcastConfigResponse(msg);
                break;
            case MESSAGE_SET_CONFIG:
                handleSetCellBroadcastConfigResponse(msg);
                break;
            default:
                break;
            }
        }

        private void handleGetCellBroadcastConfigResponse(Message msg) {
            if (msg.arg2 == MESSAGE_GET_CONFIG) {
                onFinished(mLanguagePreference, true);
            } else {
                onFinished(mLanguagePreference, false);
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar == null) {
                Xlog.i(LOG_TAG,"handleGetCellBroadcastConfigResponse,ar is null");
                onError(mLanguagePreference, RESPONSE_ERROR);
                if (msg.arg2 == MESSAGE_GET_CONFIG) {
                    mLanguagePreference.setEnabled(false);
                    mAddChannelPreference.setEnabled(false);
                }
                return;
            }
            if (ar.exception != null) {
                if (DBG) {
                    Xlog.d(LOG_TAG,"handleGetCellBroadcastConfigResponse: ar.exception=" + ar.exception);
                }
                onError(mLanguagePreference, EXCEPTION_ERROR);
                if (msg.arg2 == MESSAGE_GET_CONFIG) {
                    mLanguagePreference.setEnabled(false);
                    mAddChannelPreference.setEnabled(false);
                }
                return;
            } else {
                if (ar != null && ar.userObj instanceof Throwable) {
                    onError(mLanguagePreference, RESPONSE_ERROR);
                } else {
                    if (ar.result != null) {
                        ArrayList<SmsBroadcastConfigInfo> list = (ArrayList<SmsBroadcastConfigInfo>) ar.result;
                        if (queryChannelFromDatabase()) {
                            initChannelMap();
                            updateCurrentChannelAndLanguage(list);
                            updateChannelUIList();
                            updateLanguageSummary();
                        } else {
                            showUpdateDBErrorInfoDialog();
                        }
                    } else {
                        onError(mLanguagePreference, RESPONSE_ERROR);
                        Xlog.d(LOG_TAG,"handleGetCellBroadcastConfigResponse: ar.result is null");
                        if (msg.arg2 == MESSAGE_GET_CONFIG) {
                            mLanguagePreference.setEnabled(false);
                            mAddChannelPreference.setEnabled(false);
                        }
                    }
                }
            }
        }

        private void handleSetCellBroadcastConfigResponse(Message msg) {
            if (msg.arg2 == MESSAGE_SET_CONFIG) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    Xlog.i(LOG_TAG,"handleSetCellBroadcastConfigResponse,ar is null");
                    onError(mLanguagePreference, RESPONSE_ERROR);
                }
                if (ar.exception != null) {
                    if (DBG) {
                        Xlog.d(LOG_TAG,"handleSetCellBroadcastConfigResponse: ar.exception="
                                + ar.exception);
                    }
                    onError(mLanguagePreference, EXCEPTION_ERROR);
                }
                getCellBroadcastConfig(false);
            }
        }
    }

    private void initLanguageList() {
        boolean[] languageEnable = new boolean[LANGUAGE_NUM];
        String[] languageId = new String[LANGUAGE_NUM];
        String[] languageName = new String[LANGUAGE_NUM];
        languageName = getResources().getStringArray(R.array.language_list_values);
        languageId = getResources().getStringArray(R.array.language_list_id);
        for (int i = 0; i < LANGUAGE_NUM; i++) {
            int id = Integer.valueOf(languageId[i]).intValue();
            String name = languageName[i];
            boolean enable = languageEnable[i];
            CellBroadcastLanguage language = new CellBroadcastLanguage(id,name, enable);
            mLanguageList.add(language);
        }
    }

    private void initLanguageMap() {
        mLanguageMap = new HashMap<String, CellBroadcastLanguage>();
        for (int i = 0; i < LANGUAGE_NUM; i++) {
            CellBroadcastLanguage language = mLanguageList.get(i);
            if (language != null) {
                int id = language.getLanguageId();
                mLanguageMap.put(String.valueOf(id), language);
            }
        }
    }

    private void initLanguage() {
        initLanguageList();
        initLanguageMap(); // Map(LanguageId,CellBroadcastLanguage)
    }

    private CellBroadcastLanguage getLanguageObjectFromKey(String key) {
        return mLanguageMap.get(key);
    }

    private CellBroadcastChannel getChannelObjectFromKey(String key) {
        return mChannelMap.get(key);
    }

    private void showLanguageSelectDialog() {
        final boolean[] temp = new boolean[LANGUAGE_NUM];
        final boolean[] temp2 = new boolean[LANGUAGE_NUM];
        boolean allLanguagesFlag = true;
        for (int i = 1; i < temp.length; i++) {
            CellBroadcastLanguage tLanguage = mLanguageList.get(i);
            if (tLanguage != null) {
                Xlog.d(LOG_TAG, "language status " + tLanguage.getLanguageState());
                temp[i] = tLanguage.getLanguageState();
                temp2[i] = tLanguage.getLanguageState();
            } else {
                Xlog.i(LOG_TAG, "showLanguageSelectDialog() init the language list failed when i=" + i);
            }
            if (!temp[i]) {
                allLanguagesFlag = false;// not select all languages
            }
        }
        // init "All Languages" selection
        Xlog.d(LOG_TAG, "All language status " + allLanguagesFlag);
        mLanguageList.get(0).setLanguageState(allLanguagesFlag);
        temp[0] = allLanguagesFlag;
        temp2[0] = allLanguagesFlag;
        final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        dlgBuilder.setTitle(getString(R.string.cb_dialog_title_language_choice));
        dlgBuilder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int tLength = temp.length;
                        //if select "all languages"
                        if (temp[0]) {
                            for (int i = 0; i < tLength; i++) {
                                temp[i] = true;
                            }
                        }
                        // select a language at least
                        boolean flag = false;
                        for (int i = 0; i < tLength; i++) {
                            mLanguageList.get(i).setLanguageState(temp[i]);
                            if (temp[i]) {
                                flag = true;
                            }
                        }

                        if (flag) {
                            SmsBroadcastConfigInfo[] langList = makeLanguageConfigArray();
                            setCellBroadcastConfig(langList);
                        } else {
                            displayMessage(R.string.cb_error_language_select);
                            for (int i = 0; i < tLength; i++) {
                                mLanguageList.get(i).setLanguageState(temp2[i]);
                            }
                        }
                    }
                });
        dlgBuilder.setNegativeButton(R.string.cancel, null);
        DialogInterface.OnMultiChoiceClickListener multiChoiceListener = new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int whichButton,boolean isChecked) {
                    temp[whichButton] = isChecked;
                    AlertDialog languageDialog = null;
                    if (dialog instanceof AlertDialog) {
                        languageDialog = (AlertDialog)dialog;
                    }
                    if (whichButton == 0) {
                        if (languageDialog != null) {
                            for (int i = 1; i < temp.length; ++i) {
                                ListView items = languageDialog.getListView();
                                items.setItemChecked(i, isChecked);
                                temp[i] = isChecked;
                            }
                        }
                    } else {
                        if ((!isChecked) && (languageDialog != null)) {
                            ListView items = languageDialog.getListView();
                            items.setItemChecked(0, isChecked);
                            temp[0] = false;
                        }
                    }
                }
            };
        dlgBuilder.setMultiChoiceItems(R.array.language_list_values, temp,multiChoiceListener);
        AlertDialog languageDialog = dlgBuilder.create();
        if (languageDialog != null) {
            languageDialog.show();
        }
    }

    private void setCellBroadcastConfig(SmsBroadcastConfigInfo[] objectList) {
        Message msg = mHandler.obtainMessage(MESSAGE_SET_CONFIG, 0, MESSAGE_SET_CONFIG, null);
        if (CallSettings.isMultipleSim()) {
            ((GeminiPhone)mPhone).setCellBroadcastSmsConfigGemini(objectList, objectList, msg, mSimId);
        } else {
            mPhone.setCellBroadcastSmsConfig(objectList, objectList, msg);
        }

        onStarted(mLanguagePreference, false);
    }

    // convert selection info to SmsBroadcastConfigInfo[],and send to the system
    private SmsBroadcastConfigInfo[] makeLanguageConfigArray() {
        ArrayList<SmsBroadcastConfigInfo> list = new ArrayList<SmsBroadcastConfigInfo>();
        
        if (mLanguageList.get(0).getLanguageState()) {
            SmsBroadcastConfigInfo cBConfig = new SmsBroadcastConfigInfo(-1, -1, -2, -2, true);
            list.add(cBConfig);
        } else {
            int beginId = mLanguageList.get(1).getLanguageId();
            int endId = beginId;
            boolean beginState = mLanguageList.get(1).getLanguageState();
            int i = 2;
            int tSize = mLanguageList.size();
            for (i = 2; i < tSize; i++) {
                CellBroadcastLanguage tLanguage = mLanguageList.get(i);
                int tempId = tLanguage.getLanguageId();
                boolean tempState = tLanguage.getLanguageState();
                if ((tempId == endId + 1) && (beginState == tempState)) {
                    endId = tempId;
                } else {
                    SmsBroadcastConfigInfo cBConfig = new SmsBroadcastConfigInfo(-1, -1, beginId, endId, beginState);
                    list.add(cBConfig);
                    beginId = tempId;
                    endId = tempId;
                    beginState = tempState;
                }
            }

            if (i == mLanguageList.size()) {
                endId = mLanguageList.get(i - 1).getLanguageId();
                SmsBroadcastConfigInfo cBConfig = new SmsBroadcastConfigInfo(-1, -1, beginId, endId, beginState);
                list.add(cBConfig);
            }
        }
        
        return list.toArray(new SmsBroadcastConfigInfo[list.size()]);
    }

    private void displayMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    private void displayMessage(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = info.position - 3;
        CellBroadcastChannel oldChannel = mChannelArray.get(index);
        switch (item.getItemId()) {
        case MENU_CHANNEL_ENABLE_DISABLE:
            CellBroadcastChannel newChannel = new CellBroadcastChannel();
            newChannel = oldChannel;
            newChannel.setChannelState(!oldChannel.getChannelState());
            int tempOldChannelId = oldChannel.getChannelId();
            SmsBroadcastConfigInfo[] objectList = new SmsBroadcastConfigInfo[1];
            objectList[0] = new SmsBroadcastConfigInfo(
                    tempOldChannelId,tempOldChannelId, -1, -1, newChannel.getChannelState());
            if (updateChannelToDatabase(oldChannel, newChannel)) {
                setCellBroadcastConfig(objectList);
            } else {
                showUpdateDBErrorInfoDialog();
            }
            break;
        case MENU_CHANNEL_EDIT:
            showEditChannelDialog(oldChannel);
            break;
        case MENU_CHANNEL_DELETE:
            oldChannel.setChannelState(false);
            SmsBroadcastConfigInfo[] objectList1 = makeChannelConfigArray(oldChannel);
            if (deleteChannelFromDatabase(oldChannel)) {
                setCellBroadcastConfig(objectList1);
            } else {
                showUpdateDBErrorInfoDialog();
            }
            break;
        default:
            break;
        }
        return super.onContextItemSelected(item);
    }
  
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info == null) {
            Xlog.i(LOG_TAG, "onCreateContextMenu,menuInfo is null");
            return;
        }
        int position = info.position;
        if (position >= 3) {
            int index = position - 3;
            CellBroadcastChannel channel = mChannelArray.get(index);
            String channelName = channel.getChannelName();
            menu.setHeaderTitle(channelName);
            if (channel.getChannelState()) {
                menu.add(0, MENU_CHANNEL_ENABLE_DISABLE, 0, R.string.disable);
            } else {
                menu.add(0, MENU_CHANNEL_ENABLE_DISABLE, 0, R.string.enable);
            }
            menu.add(1, MENU_CHANNEL_EDIT, 0, R.string.cb_menu_edit);
            menu.add(2, MENU_CHANNEL_DELETE, 0, R.string.cb_menu_delete);
        }
    }

    private boolean checkChannelName(String name) {
        if (name == null || name.length() == 0) {
            name = "";
        }
        if (name.length() > CHANNEL_NAME_LENGTH) {
            return false;
        }
        return true;
    }

    private boolean checkChannelNumber(String number) {
        if (number == null || number.length() == 0) {
            return false;
        }
        int t = Integer.valueOf(number).intValue();
        if (t >=  CB_MAX_CHANNEL || t < 0) {
            return false;
        }
        return true;
    }

    private boolean checkChannelIdExist(int channelId) {
        int length = mChannelArray.size();
        for (int i = 0; i < length; i++) {
            if (mChannelArray.get(i).getChannelId() == channelId) {
                return true;
            }
        }
        return false;
    }

    private boolean checkChannelIdExist(int newChannelId, int keyId) {
        int length = mChannelArray.size();
        for (int i = 0; i < length; i++) {
            CellBroadcastChannel tChannel = mChannelArray.get(i);
            int tempChannelId = tChannel.getChannelId();
            int tempKeyId = tChannel.getKeyId();
            if (tempChannelId == newChannelId && tempKeyId != keyId) {
                return true;
            }
        }
        return false;
    }
    
    private void setScreenEnabled() {
        List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
        if (insertSim.size() == 0) {
            finish();
        } else if (CallSettings.isMultipleSim() && insertSim.size() == 1) {
            if (mSimId != insertSim.get(0).mSlot) {
                finish();
            }
        }
    }
}
