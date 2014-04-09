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

package com.android.mms.ui;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;

import com.android.mms.util.Recycler;

/// M:
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.android.mms.data.WorkingMessage;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.os.storage.EncapsulatedStorageManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.IndexOutOfBoundsException;
import java.util.List;

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class MessagingPreferenceActivity extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener {
    
    // Symbolic names for the keys used for preference lookup
    public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
    public static final String EXPIRY_TIME              = "pref_key_mms_expiry";
    public static final String PRIORITY                 = "pref_key_mms_priority";
    public static final String READ_REPORT_MODE         = "pref_key_mms_read_reports";
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
    public static final String NOTIFICATION_ENABLED     = "pref_key_enable_notifications";
    public static final String NOTIFICATION_RINGTONE    = "pref_key_ringtone";
    public static final String AUTO_RETRIEVAL           = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
    public static final String AUTO_DELETE              = "pref_key_auto_delete";
    public static final String GROUP_MMS_MODE           = "pref_key_mms_group_mms";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS    = 1;
    private Preference mSmsLimitPref;
    private Preference mMmsLimitPref;
    private Preference mManageSimPref;
    private Preference mClearHistoryPref;
    private Recycler mSmsRecycler;
    private Recycler mMmsRecycler;
    private Preference mMmsGroupMmsPref;
    private static final int CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG = 3;

    /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
    private static final String TAG = "MessagingPreferenceActivity";
    private static final boolean DEBUG = false;
    /// M: add this for read report
    public static final String READ_REPORT_AUTO_REPLY   = "pref_key_mms_auto_reply_read_reports";    
    public static final String CREATION_MODE            = "pref_key_mms_creation_mode";
    public static final String MMS_SIZE_LIMIT           = "pref_key_mms_size_limit";
    public static final String SMS_QUICK_TEXT_EDITOR    = "pref_key_quick_text_editor";
    public static final String SMS_SERVICE_CENTER       = "pref_key_sms_service_center";
    public static final String SMS_VALIDITY_PERIOD      = "pref_key_sms_validity_period";
    public static final String SMS_MANAGE_SIM_MESSAGES  = "pref_key_manage_sim_messages";
    public static final String SMS_SAVE_LOCATION        = "pref_key_sms_save_location";
    public static final String MMS_ENABLE_TO_SEND_DELIVERY_REPORT = "pref_key_mms_enable_to_send_delivery_reports";
    /// M: Code analyze 002, new feature, Import/Export SMS. @{
    public static final String MSG_IMPORT               = "pref_key_import_msg";
    public static final String MSG_EXPORT               = "pref_key_export_msg";
    /// @}
    public static final String SMS_INPUT_MODE           = "pref_key_sms_input_mode";
    public static final String CELL_BROADCAST           = "pref_key_cell_broadcast";
    public static final String SMS_FORWARD_WITH_SENDER  = "pref_key_forward_with_sender";
    private static final int MAX_EDITABLE_LENGTH = 20;
    private Preference mStorageStatusPref;
    private Preference mSmsQuickTextEditorPref;
    private Preference mSmsServiceCenterPref;
    private Preference mSmsValidityPeriodPref;
    /// M: Code analyze 002, new feature, Import/Export SMS. @{
    private Preference mImportMessages;
    private Preference mExportMessages;
    /// @}
    private Preference mCBsettingPref;

    private Preference mFontSize;
    private AlertDialog mFontSizeDialog;
    private String[] mFontSizeChoices;
    private String[] mFontSizeValues;
    private static final int FONT_SIZE_DIALOG = 10;
    public static final String FONT_SIZE_SETTING = "pref_key_message_font_size";
    public static final String TEXT_SIZE = "message_font_size";

    /// all preferences need change key for single sim card
    private CheckBoxPreference mSmsDeliveryReport;
    private CheckBoxPreference mMmsDeliveryReport;
    private CheckBoxPreference mMmsEnableToSendDeliveryReport;
    private CheckBoxPreference mMmsReadReport;
    /// M: add this for read report
    private CheckBoxPreference mMmsAutoReplyReadReport;    
    private CheckBoxPreference mMmsAutoRetrieval;
    private CheckBoxPreference mMmsRetrievalDuringRoaming;
    private CheckBoxPreference mEnableNotificationsPref;
    private CheckBoxPreference mSmsForwardWithSender;

    /// M: all preferences need change key for multiple sim card
    private Preference mSmsDeliveryReportMultiSim;
    private Preference mMmsDeliveryReportMultiSim;
    private Preference mMmsEnableToSendDeliveryReportMultiSim;
    private Preference mMmsReadReportMultiSim;
    /// M: add this for read report
    private Preference mMmsAutoReplyReadReportMultiSim;
    private Preference mMmsAutoRetrievalMultiSim;
    private Preference mMmsRetrievalDuringRoamingMultiSim;
    private Preference mSmsServiceCenterPrefMultiSim;
    private Preference mSmsValidityPeriodPrefMultiSim;
    private Preference mManageSimPrefMultiSim;
    private Preference mCellBroadcastMultiSim;
    private Preference mSmsSaveLoactionMultiSim;

    private ListPreference mMmsPriority;
    private ListPreference mSmsLocation;
    private ListPreference mMmsCreationMode;
    private ListPreference mMmsSizeLimit;
    private ListPreference mSmsInputMode;
    
    private static final String PRIORITY_HIGH = "High";
    private static final String PRIORITY_LOW = "Low";
    private static final String PRIORITY_NORMAL = "Normal";
    
    private static final String LOCATION_PHONE = "Phone";
    private static final String LOCATION_SIM = "Sim";
    
    private static final String CREATION_MODE_RESTRICTED = "RESTRICTED";
    private static final String CREATION_MODE_WARNING = "WARNING";
    private static final String CREATION_MODE_FREE = "FREE";
    
    private static final String SIZE_LIMIT_100 = "100";
    private static final String SIZE_LIMIT_200 = "200";
    private static final String SIZE_LIMIT_300 = "300";
     

    /// M: Code analyze 002, new feature, Import/Export SMS. @{
    private Handler mSMSHandler = new Handler();
    private Handler mMMSHandler = new Handler();
    /// @}
    private EditText mNumberText;
    private AlertDialog mNumberTextDialog;
    private List<SIMInfo> mListSimInfo;
    int mSlotId;
    private NumberPickerDialog mSmsDisplayLimitDialog;
    private NumberPickerDialog mMmsDisplayLimitDialog;

    /// M: import or export SD card
    private ProgressDialog mProgressDialog = null;
    private static final String TABLE_SMS = "sms";
    private String mFileNamePrefix = "sms";
    private String mFileNameSuffix = "";
    private String mFileNameExtension = "db";
    private static final Uri SMS_URI = Uri.parse("content://sms");
    private static final Uri CANADDRESS_URI = Uri.parse("content://mms-sms/canonical-addresses");
    public static final String SDCARD_MESSAGE_DIR_PATH = "//message//";
    public static final String MEM_DIR_PATH = "//data//data//com.android.mms//message//sms001.db";
    private static final String[] SMS_COLUMNS =
    { "thread_id", "address","m_size", "person", "date", "protocol", "read", "status", "type", "reply_path_present",
      "subject", "body", "service_center", "locked", "sim_id", "error_code", "seen"};
    public Handler mMainHandler; 
    private static final String[] ADDRESS_COLUMNS = {"address"};
    private static final int EXPORT_SMS    = 2;    
    private static final int EXPORT_SUCCES = 3;
    private static final int EXPORT_FAILED = 4;
    private static final int IMPORT_SUCCES = 5;
    private static final int IMPORT_FAILED = 6;
    private static final int EXPORT_EMPTY_SMS = 7;
    private static final int DISK_IO_FAILED = 8;
    private static final int MIN_FILE_NUMBER = 1;
    private static final int MAX_FILE_NUMBER = 999;
    public String SUB_TITLE_NAME = "sub_title_name";
    private int mCurrentSimCount = 0;
    /// @}

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MmsLog.d(TAG, "onCreate");
        /// M: Code analyze 002, new feature, Import/Export SMS. @{
        newMainHandler();
        /// @}
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setMessagePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /// M: Code analyze 003, For fix bug ALPS00111897, the textview2 shows
        // wrong text Set the value after the preference screen shown. @{
        setListPrefSummary();
        /// @}
        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        setEnabledNotificationsPref();
    }

    /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
    private void setMessagePreferences() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            MmsLog.d(TAG, "MTK_GEMINI_SUPPORT is true");
            mCurrentSimCount = SIMInfo.getInsertedSIMCount(this);
            MmsLog.d(TAG, "mCurrentSimCount is :" + mCurrentSimCount);
            if (mCurrentSimCount <= 1) {
                addPreferencesFromResource(R.xml.preferences);
                if (MmsConfig.getDeliveryReportAllowed()) {
                    mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                } else {
                    mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                    PreferenceCategory mmsCategory = (PreferenceCategory) findPreference("pref_key_mms_settings");
                    mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
                } 
            } else {
                addPreferencesFromResource(R.xml.multicardpreferences);
            }
        } else {
            addPreferencesFromResource(R.xml.preferences);
            if (MmsConfig.getDeliveryReportAllowed()) {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            } else {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                PreferenceCategory mmsCategory = (PreferenceCategory) findPreference("pref_key_mms_settings");
                mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
           }
        }
        /// M: add for read report
        if (!EncapsulatedFeatureOption.MTK_SEND_RR_SUPPORT) {
            // remove read report entry
            MmsLog.d(MmsApp.TXN_TAG, "remove the read report entry, it should be hidden.");
            PreferenceCategory mmOptions = (PreferenceCategory)findPreference("pref_key_mms_settings");
            mmOptions.removePreference(findPreference(READ_REPORT_AUTO_REPLY));          
        }

        if (MmsConfig.getShowStorageStatusEnabled()) { 
            mStorageStatusPref = findPreference("pref_key_storage_status");
        } else {
            mStorageStatusPref = findPreference("pref_key_storage_status");
            PreferenceCategory storageCategory = (PreferenceCategory) findPreference("pref_key_storage_settings");
            storageCategory.removePreference(mStorageStatusPref);
        }
        /// M: Code analyze 013, new feature, adjust font size by moltitouch. @{
        if (MmsConfig.getAdjustFontSizeEnabled()) {
            mFontSizeChoices = getResourceArray(R.array.pref_message_font_size_choices);
            mFontSizeValues = getResourceArray(R.array.pref_message_font_size_values);
            mFontSize = (Preference)findPreference(FONT_SIZE_SETTING);
            mFontSize.setSummary(mFontSizeChoices[getPreferenceValueInt(FONT_SIZE_SETTING, 0)]);
        } else {
            PreferenceCategory fontSizeOptions =
                (PreferenceCategory)findPreference("pref_key_font_size_setting");
            getPreferenceScreen().removePreference(fontSizeOptions);
        }
        /// @}
        
        mCBsettingPref = findPreference(CELL_BROADCAST); 
        /// M: disable the cellbroadcast preference while no sim card. @{
        if (mCurrentSimCount == 0) {
            mCBsettingPref.setEnabled(false);
        }
        /// @}
        mSmsLimitPref = findPreference("pref_key_sms_delete_limit"); 
        mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        mMmsGroupMmsPref = findPreference("pref_key_mms_group_mms");
        mClearHistoryPref = findPreference("pref_key_mms_clear_history");
        mSmsQuickTextEditorPref = findPreference("pref_key_quick_text_editor");

        mMmsPriority = (ListPreference) findPreference("pref_key_mms_priority");
        mMmsPriority.setOnPreferenceChangeListener(this);
        mSmsLocation = (ListPreference) findPreference(SMS_SAVE_LOCATION);
        mSmsLocation.setOnPreferenceChangeListener(this);
        mMmsCreationMode = (ListPreference) findPreference("pref_key_mms_creation_mode");
        mMmsCreationMode.setOnPreferenceChangeListener(this);
        mMmsSizeLimit = (ListPreference) findPreference("pref_key_mms_size_limit");
        mMmsSizeLimit.setOnPreferenceChangeListener(this);
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(NOTIFICATION_ENABLED);
        PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) { 
            /// M: Code analyze 010, For fix bug ALPS00255812, remove SMS
            // validity period feature for non-OP01 project. @{
            if (!MmsConfig.getSmsValidityPeriodEnabled()) {
                mSmsValidityPeriodPref = findPreference(SMS_VALIDITY_PERIOD);
                if (mSmsValidityPeriodPref != null) {
                    smsCategory.removePreference(mSmsValidityPeriodPref);
                }
            /// @}
            }
            if (mCurrentSimCount == 0) {
                
                // No SIM card, remove the SIM-related prefs
                //smsCategory.removePreference(mManageSimPref);
                //If there is no SIM, this item will be disabled and can not be accessed.
                mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                mManageSimPref.setEnabled(false);
                if (!MmsConfig.getSIMSmsAtSettingEnabled()) {
                    smsCategory.removePreference(mManageSimPref);
                }
                
                mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
                mSmsServiceCenterPref.setEnabled(false);

                if (MmsConfig.getSmsValidityPeriodEnabled()) {
                    mSmsValidityPeriodPref = findPreference(SMS_VALIDITY_PERIOD);
                    mSmsValidityPeriodPref.setEnabled(false);
                }
            }
        } else {
             //remove SMS validity period feature for non-Gemini project

             mSmsValidityPeriodPref = findPreference(SMS_VALIDITY_PERIOD);
             smsCategory.removePreference(mSmsValidityPeriodPref);

             if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                 //smsCategory.removePreference(mManageSimPref);
                 //If there is no SIM, this item will be disabled and can not be accessed.
                 mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                 if (mManageSimPref != null) {
                 mManageSimPref.setEnabled(false);
                 }
                 if (!MmsConfig.getSIMSmsAtSettingEnabled()) {
                    smsCategory.removePreference(mManageSimPref);
                 }
                 mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
                 mSmsServiceCenterPref.setEnabled(false);
             } else {
                 mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                 if (!MmsConfig.getSIMSmsAtSettingEnabled()) {
                     smsCategory.removePreference(mManageSimPref);
                 }
                 mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");

                 mListSimInfo = SIMInfo.getInsertedSIMList(this);
                 mMmsReadReport = (CheckBoxPreference) findPreference(READ_REPORT_MODE);
                 mMmsAutoReplyReadReport = (CheckBoxPreference) findPreference(READ_REPORT_AUTO_REPLY);
                 /// M: Code analyze 012, For fix bug ALPS00296115, merge dual talk (EVDO) into ALPS.ICS auto merge. @{
                 if (EncapsulatedFeatureOption.EVDO_DT_SUPPORT && isUSimType(mListSimInfo.get(0).getSlot())) {
                     mMmsAutoReplyReadReport.setEnabled(false);
                     mMmsReadReport.setEnabled(false);
                 }
                 /// @}

             }
        }
        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            PreferenceCategory mmsOptions =
                (PreferenceCategory)findPreference("pref_key_mms_settings");
            getPreferenceScreen().removePreference(mmsOptions);

            PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageOptions.removePreference(findPreference("pref_key_mms_delete_limit"));
        }  else {
            PreferenceCategory mmsOptions =
                    (PreferenceCategory)findPreference("pref_key_mms_settings");
            // If the phone's SIM doesn't know it's own number, disable group mms.
            if (!MmsConfig.getGroupMmsEnabled()) {
                mmsOptions.removePreference(mMmsGroupMmsPref);
            }
        }

        
        setEnabledNotificationsPref();

        enablePushSetting();
        
        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();
        addSmsInputModePreference();
        // Change the key to the SIM-related key, if has one SIM card, else set default value.
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            MmsLog.d(TAG, "MTK_GEMINI_SUPPORT is true");
            if (mCurrentSimCount == 1) {
                MmsLog.d(TAG, "single sim");
                changeSingleCardKeyToSimRelated();
            } else if (mCurrentSimCount > 1) {
                setMultiCardPreference();
            }
        }
        /// M: Code analyze 004, new feature, Add this new feature:When
        // forwarding a received SMS, adding sender of the SMS to text content.
        // @{
        if (MmsConfig.getForwardWithSenderEnabled()) {
            mSmsForwardWithSender = (CheckBoxPreference) findPreference(SMS_FORWARD_WITH_SENDER);
            SharedPreferences sp = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
            if (mSmsForwardWithSender != null) {
                mSmsForwardWithSender.setChecked(sp.getBoolean(mSmsForwardWithSender.getKey(), true));
            }
            //mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
            if (MmsConfig.getMmsDirMode() && mManageSimPref != null) {
                ((PreferenceCategory)findPreference("pref_key_sms_settings")).removePreference(mManageSimPref);
            }
        } else {
            mSmsForwardWithSender = (CheckBoxPreference) findPreference(SMS_FORWARD_WITH_SENDER);
            smsCategory.removePreference(mSmsForwardWithSender);
        }
        /// @}
    }
    /// @}

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mSmsRecycler.getMessageLimit(this)));
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mMmsRecycler.getMessageLimit(this)));
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
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
        if (preference == mStorageStatusPref) {
            final String memoryStatus = MessageUtils.getStorageStatus(getApplicationContext());
            new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.pref_title_storage_status)
                    .setIcon(R.drawable.ic_dialog_info_holo_light)
                    .setMessage(memoryStatus)
                    .setCancelable(true)
                    .show();
        } else if (preference == mSmsLimitPref) {
            mSmsDisplayLimitDialog = 
            new NumberPickerDialog(this,
                    mSmsLimitListener,
                    mSmsRecycler.getMessageLimit(this),
                    mSmsRecycler.getMessageMinLimit(),
                    mSmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_sms_delete);
            mSmsDisplayLimitDialog.show();
        } else if (preference == mMmsLimitPref) {
            mMmsDisplayLimitDialog = 
            new NumberPickerDialog(this,
                    mMmsLimitListener,
                    mMmsRecycler.getMessageLimit(this),
                    mMmsRecycler.getMessageMinLimit(),
                    mMmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_mms_delete);
            mMmsDisplayLimitDialog.show();
        } else if (preference == mManageSimPref) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                mListSimInfo = SIMInfo.getInsertedSIMList(this);
                int slotId = mListSimInfo.get(0).getSlot();
                MmsLog.d(TAG, "slotId is : " + slotId);
                if (slotId != -1) {
                    Intent it = new Intent();
                    it.setClass(this, ManageSimMessages.class);
                    it.putExtra("SlotId", slotId);
                    startActivity(it);
                }
            } else {
                startActivity(new Intent(this, ManageSimMessages.class));
            }
        } else if (preference == mClearHistoryPref) {
            showDialog(CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG);
            return true;
        } else if (preference == mSmsQuickTextEditorPref) {
            Intent intent = new Intent();
            intent.setClass(this, SmsTemplateEditActivity.class);
            startActivity(intent);
        } else if (preference == mSmsDeliveryReportMultiSim 
                || preference == mMmsDeliveryReportMultiSim
                || preference == mMmsEnableToSendDeliveryReportMultiSim
                || preference == mMmsReadReportMultiSim 
                // M: add this for read report
                || preference == mMmsAutoReplyReadReportMultiSim
                || preference == mMmsAutoRetrievalMultiSim 
                || preference == mMmsRetrievalDuringRoamingMultiSim) {
            
            Intent it = new Intent();
            it.setClass(this, MultiSimPreferenceActivity.class);
            it.putExtra("preference", preference.getKey());
            it.putExtra("preferenceTitle", preference.getTitle());
            startActivity(it);
        } else if (preference == mSmsServiceCenterPref) {

            mListSimInfo = SIMInfo.getInsertedSIMList(this);
            if (mListSimInfo == null || (mListSimInfo != null && mListSimInfo.isEmpty())) {
                MmsLog.d(TAG, "there is no sim card");
                return true;
            }
            int id = mListSimInfo.get(0).getSlot();
            if (EncapsulatedFeatureOption.EVDO_DT_SUPPORT && isUSimType(id)) {
                showToast(R.string.cdma_not_support);
            } else {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            mNumberText = new EditText(dialog.getContext());
            mNumberText.setHint(R.string.type_to_compose_text_enter_to_send);
            mNumberText.computeScroll();
            mNumberText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_EDITABLE_LENGTH)});
            //mNumberText.setKeyListener(new DigitsKeyListener(false, true));
            mNumberText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_CLASS_PHONE);
            EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
            String gotScNumber;
            try {
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    int slotId = mListSimInfo.get(0).getSlot();
                    gotScNumber = teleService.getScAddressGemini(slotId);
                } else {
                    gotScNumber = teleService.getScAddressGemini(0);
                }
            } catch (RemoteException e) {
                gotScNumber = null;
                MmsLog.e(TAG, "getScAddressGemini is failed.\n" + e.toString());
            }
            MmsLog.d(TAG, "gotScNumber is: " + gotScNumber);
            mNumberText.setText(gotScNumber);
            mNumberTextDialog = dialog
            .setIcon(R.drawable.ic_dialog_info_holo_light)
            .setTitle(R.string.sms_service_center)
            .setView(mNumberText)
            .setPositiveButton(R.string.OK, new PositiveButtonListener())
            .setNegativeButton(R.string.Cancel, new NegativeButtonListener())
            .show();

            }

        } else if (preference == mSmsValidityPeriodPref) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                int slotId = mListSimInfo.get(0).getSlot();
                final int[] validityPeroids = {EncapsulatedSmsManager.VALIDITY_PERIOD_NO_DURATION,
                    EncapsulatedSmsManager.VALIDITY_PERIOD_ONE_HOUR, EncapsulatedSmsManager.VALIDITY_PERIOD_SIX_HOURS,
                    EncapsulatedSmsManager.VALIDITY_PERIOD_TWELVE_HOURS, EncapsulatedSmsManager.VALIDITY_PERIOD_ONE_DAY,
                    EncapsulatedSmsManager.VALIDITY_PERIOD_MAX_DURATION,};
                final CharSequence[] validityItems = {
                    getResources().getText(R.string.sms_validity_period_nosetting),
                    getResources().getText(R.string.sms_validity_period_1hour),
                    getResources().getText(R.string.sms_validity_period_6hours),
                    getResources().getText(R.string.sms_validity_period_12hours),
                    getResources().getText(R.string.sms_validity_period_1day),
                    getResources().getText(R.string.sms_validity_period_max)};
                /* check validity index */
                final String validityKey = Long.toString(slotId) + "_"
                    + MessagingPreferenceActivity.SMS_VALIDITY_PERIOD;
                int vailidity = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                    validityKey, EncapsulatedSmsManager.VALIDITY_PERIOD_NO_DURATION);
                int currentPosition = 0;
                MmsLog.d(TAG, "validity found the res = " + vailidity);
                for (int i = 0; i < validityPeroids.length; i++) {
                    if (vailidity == (validityPeroids[i])) {
                        MmsLog.d(TAG, "validity found the position = " + i);
                        currentPosition = i;
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getText(R.string.sms_validity_period));
                builder.setSingleChoiceItems(validityItems, currentPosition,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(MessagingPreferenceActivity.this)
                                    .edit();
                            editor.putInt(validityKey, validityPeroids[item]);
                            editor.commit();
                            dialog.dismiss();
                        }
                    });
                builder.show();
            }
        } else if (preference == mSmsServiceCenterPrefMultiSim
            || preference == mSmsValidityPeriodPrefMultiSim
                || preference == mManageSimPrefMultiSim
                || preference == mCellBroadcastMultiSim
                || (preference == mSmsSaveLoactionMultiSim && mCurrentSimCount > 1)) {
            Intent it = new Intent();
            it.setClass(this, SelectCardPreferenceActivity.class);
            it.putExtra("preference", preference.getKey());
            it.putExtra("preferenceTitle", preference.getTitle());
            startActivity(it);
        } else if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);
        } else if (preference == mImportMessages) {
            /// M: Code analyze 002, new feature, Import/Export SMS. @{
            Intent it = new Intent();
            it.setClass(this, ImportSmsActivity.class);
            startActivity(it);
        } else if (preference == mExportMessages) {
            showDialog(EXPORT_SMS);
            /// @}
        } else if (preference == mCBsettingPref) {
            /// M: Code analyze 005, For fix bug ALPS00233457, can't enable
            // SIM1's CB founction set slot id when insert simcard into
            // different slot. @{
             mListSimInfo = SIMInfo.getInsertedSIMList(this);
             if (mListSimInfo != null && mListSimInfo.isEmpty()) {
                 MmsLog.d(TAG, "there is no sim card");
                 return true;
             }
             int slotId = mListSimInfo.get(0).getSlot();
             MmsLog.d(TAG, "mCBsettingPref slotId is : " + slotId);

            if (EncapsulatedFeatureOption.EVDO_DT_SUPPORT && isUSimType(slotId)) {
                showToast(R.string.cdma_not_support);
             /// @}
            } else {
                Intent it = new Intent();
                it.setClassName("com.android.phone", "com.mediatek.settings.CellBroadcastActivity");
                it.setAction(Intent.ACTION_MAIN);
                it.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, slotId);
                it.putExtra(SUB_TITLE_NAME, SIMInfo.getSIMInfoBySlot(this, slotId).getDisplayName());
                startActivity(it);
            }
        } else if (preference == mFontSize) {
            showDialog(FONT_SIZE_DIALOG);
        }
        /// @}

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().clear().apply();
        setPreferenceScreen(null);
        setMessagePreferences();
        /// M: Code analyze 003, For fix bug ALPS00111897, the textview2 shows
        // wrong text Set the value after the preference screen shown. @{
        setListPrefSummary();
        /// @}
    }

    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
                if (limit <= mSmsRecycler.getMessageMinLimit()) {
                    limit = mSmsRecycler.getMessageMinLimit();
                } else if (limit >= mSmsRecycler.getMessageMaxLimit()) {
                    limit = mSmsRecycler.getMessageMaxLimit();
                }
                /// @}
                mSmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setSmsDisplayLimit();
                /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
                if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                    mProgressDialog = ProgressDialog.show(MessagingPreferenceActivity.this, "",
                            getString(R.string.deleting), true);
                }
                mSMSHandler.post(new Runnable() {
                    public void run() {
                        new Thread(new Runnable() {
                            public void run() {
                               Recycler.getSmsRecycler().deleteOldMessages(getApplicationContext());
                               if (EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT) {
                                   Recycler.getWapPushRecycler().deleteOldMessages(getApplicationContext());
                               }
                               if (null != mProgressDialog) {
                                   mProgressDialog.dismiss();
                               }
                             }
                        }, "DeleteSMSOldMsgAfterSetNum").start();
                    }
                });
                /// @}
            }
    };

    NumberPickerDialog.OnNumberSetListener mMmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
                if (limit <= mMmsRecycler.getMessageMinLimit()) {
                    limit = mMmsRecycler.getMessageMinLimit();
                } else if (limit >= mMmsRecycler.getMessageMaxLimit()) {
                    limit = mMmsRecycler.getMessageMaxLimit();
                } 
                /// @}
                mMmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setMmsDisplayLimit();
                /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
                if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                    mProgressDialog = ProgressDialog.show(MessagingPreferenceActivity.this, "",
                            getString(R.string.deleting), true);
                }
                mMMSHandler.post(new Runnable() {
                    public void run() {
                        new Thread(new Runnable() {
                            public void run() {
                                MmsLog.d("Recycler", "mMmsLimitListener");
                                Recycler.getMmsRecycler().deleteOldMessages(getApplicationContext());
                                if (null != mProgressDialog) {
                                    mProgressDialog.dismiss();
                                }
                            } 
                        }, "DeleteMMSOldMsgAfterSetNum").start();
                    }
                });
                /// @}
            }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG:
                return new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.confirm_clear_search_title)
                    .setMessage(R.string.confirm_clear_search_text)
                    .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SearchRecentSuggestions recent =
                                ((MmsApp)getApplication()).getRecentSuggestions();
                            if (recent != null) {
                                recent.clearHistory();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    /// M: Code analyze 008, For fix bug ALPS00241042,
                    // Warning icon is not clear after typing invalid
                    // recipient. @{
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    /// @}
                    .create();
            /// M: Code analyze 002, new feature, Import/Export SMS. @{
            case EXPORT_SMS:  
                return new AlertDialog.Builder(this)
                .setMessage(getString(R.string.whether_export_item))
                .setTitle(R.string.pref_summary_export_msg).setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                exportMessages();
                                return;
                            }
                        }).setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        }).create();
            /// @}
            /// M: Code analyze 013, new feature, adjust font size by moltitouch. @{
            case FONT_SIZE_DIALOG:
                FontSizeDialogAdapter adapter = new FontSizeDialogAdapter(MessagingPreferenceActivity.this, 
                        mFontSizeChoices, mFontSizeValues);
                mFontSizeDialog = new AlertDialog.Builder(MessagingPreferenceActivity.this)
                .setTitle(R.string.message_font_size_dialog_title)
                .setNegativeButton(R.string.message_font_size_dialog_cancel, new DialogInterface.OnClickListener() {
                                
                    public void onClick(DialogInterface dialog, int which) {
                        mFontSizeDialog.dismiss();
                    }
                })
                .setSingleChoiceItems(adapter, getPreferenceValueInt(FONT_SIZE_SETTING, 0),
                        new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor =
                            PreferenceManager.getDefaultSharedPreferences(MessagingPreferenceActivity.this).edit();
                        editor.putInt(FONT_SIZE_SETTING, which);
                        editor.putFloat(TEXT_SIZE, Float.parseFloat(mFontSizeValues[which]));
                        editor.apply();
                        mFontSizeDialog.dismiss();
                        mFontSize.setSummary(mFontSizeChoices[which]);
                    }
                }).create();
                return mFontSizeDialog;
            /// @}
        }
        return super.onCreateDialog(id);
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, enabled);

        editor.apply();
    }

    /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        final String key = arg0.getKey();
        int slotId = 0;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && MmsConfig.getSmsMultiSaveLocationEnabled()) {
            int currentSimCount = SIMInfo.getInsertedSIMCount(this);
            if (currentSimCount == 1) {
                slotId = SIMInfo.getInsertedSIMList(this).get(0).getSlot();
            }
        }
        String stored = (String)arg1;
        if (PRIORITY.equals(key)) {
            mMmsPriority.setSummary(getVisualTextName(stored, R.array.pref_key_mms_priority_choices,
                    R.array.pref_key_mms_priority_values));
        } else if (CREATION_MODE.equals(key)) {
            mMmsCreationMode.setSummary(getVisualTextName(stored, R.array.pref_mms_creation_mode_choices,
                    R.array.pref_mms_creation_mode_values));
            mMmsCreationMode.setValue(stored);
            WorkingMessage.updateCreationMode(this);
        } else if (MMS_SIZE_LIMIT.equals(key)) {
            mMmsSizeLimit.setSummary(getVisualTextName(stored, R.array.pref_mms_size_limit_choices,
                    R.array.pref_mms_size_limit_values));
            MmsConfig.setUserSetMmsSizeLimit(Integer.valueOf(stored));
            
        } else if (SMS_SAVE_LOCATION.equals(key)
            && !(mCurrentSimCount > 1 && MmsConfig.getSmsMultiSaveLocationEnabled())) {
            if (!getResources().getBoolean(R.bool.isTablet)) {
                mSmsLocation.setSummary(getVisualTextName(stored,
                    R.array.pref_sms_save_location_choices, R.array.pref_sms_save_location_values));
            } else {
                mSmsLocation.setSummary(getVisualTextName(stored,
                    R.array.pref_tablet_sms_save_location_choices,
                    R.array.pref_tablet_sms_save_location_values));
            }
        } else if ((Long.toString(slotId) + "_" + SMS_SAVE_LOCATION).equals(key)) {

            if (!getResources().getBoolean(R.bool.isTablet)) {
                mSmsLocation.setSummary(getVisualTextName(stored,
                    R.array.pref_sms_save_location_choices, R.array.pref_sms_save_location_values));
            } else {
                mSmsLocation.setSummary(getVisualTextName(stored,
                    R.array.pref_tablet_sms_save_location_choices,
                    R.array.pref_tablet_sms_save_location_values));
            }
        }
        return true;
    }
    private CharSequence getVisualTextName(String enumName, int choiceNameResId, int choiceValueResId) {
        CharSequence[] visualNames = getResources().getTextArray(
                choiceNameResId);
        CharSequence[] enumNames = getResources().getTextArray(
                choiceValueResId);

        // Sanity check
        if (visualNames.length != enumNames.length) {
            return "";
        }

        for (int i = 0; i < enumNames.length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }
        return "";
    }
    /// @}

    /// M: Code analyze 002, new feature, Import/Export SMS. @{
    private boolean exportMessages() {
        MmsLog.d(TAG,"exportMessages");
        if (!isSDcardReady()) {
            return false;
        }
        mProgressDialog = ProgressDialog.show(this, "", getString(R.string.export_message_ongoing), true); 
        final String sdCardDirPath = EncapsulatedStorageManager.getDefaultPath() + SDCARD_MESSAGE_DIR_PATH;
        new Thread() {
            public void run() {
                Cursor cursor = null;
                int quiteCode = 0;
                String storeFileName = "";
                try { 
                    File dir = new File(sdCardDirPath);
                    if (!dir.exists()) {
                        if (!dir.mkdir()) {
                            MmsLog.w(TAG, "exportMessages(). make dir has been failed.");
                            return;
                        }
                    }
                    storeFileName = getAppropriateFileName(sdCardDirPath);
                    if (null == storeFileName) {
                        MmsLog.w(TAG, "exportMessages sms file name is null");
                        return;
                    }
                    cursor = getContentResolver().query(SMS_URI, SMS_COLUMNS, null, null, null);
                    if (cursor == null || cursor.getCount() == 0) {
                        MmsLog.w(TAG, "exportMessages query sms cursor is null");
                        quiteCode = EXPORT_EMPTY_SMS;
                        return;
                    }
                    MmsLog.d(TAG, "exportMessages query sms cursor count is " + cursor.getCount());
                    /// M: Code analyze 007, For fix bug ALPS00239606,
                    // ANR:Store the sms to memory first, then copy it to SD
                    // card. @{
                    int exportCount = copyToPhoneMemory(cursor, MEM_DIR_PATH);
                    if (exportCount > 0) {
                    copyToSDMemory(MEM_DIR_PATH, storeFileName);
                    /// @}
                    mMainHandler.sendEmptyMessage(EXPORT_SUCCES);
                    MmsLog.d(TAG, "ExportDict success");
                    } else {
                        MmsLog.d(TAG, "ExportDict failure there is no message to export");
                        quiteCode = EXPORT_EMPTY_SMS;
                    }
                } catch (SQLiteDiskIOException e) {
                    mMainHandler.sendEmptyMessage(DISK_IO_FAILED);
                    //if the file is created, erase it
                    if (storeFileName != null && !storeFileName.equals("")) {
                        File file = new File(storeFileName);
                        if (file.exists()) {
                            if (!file.delete()) {
                                MmsLog.w(TAG, "delete file :" + storeFileName + " has failed");
                            }
                        }
                    } 
                    e.printStackTrace();
                } catch (SQLiteException e) {
                    MmsLog.e(MmsApp.TXN_TAG, e.getMessage());
                } catch (IOException e) {
                    MmsLog.e(TAG, "exportMessages can't create the database file");
                    //if the file is created, erase it
                    File file = new File(storeFileName);
                    if (file.exists()) {
                        file.delete();
                    } 
                    mMainHandler.sendEmptyMessage(EXPORT_FAILED);
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    File file = new File(MEM_DIR_PATH);
                    if (file.exists()) {
                      file.delete();
                    } 
                    if (null != mProgressDialog) {
                        mProgressDialog.dismiss();
                    } 
                    if (quiteCode == EXPORT_EMPTY_SMS) {
                        mMainHandler.sendEmptyMessage(EXPORT_EMPTY_SMS);
                    }
                } 
            }
        }.start();
        return true;
    } 
    /// @}

    /** M: Code analyze 002, new feature, Import/Export SMS.
     * Tries to get an appropriate filename. Returns null if it fails.
     */
    private String getAppropriateFileName(final String destDirectory) {
        //get max number of  digital
        int fileNumberStringLength = 0;
        int tmp = MAX_FILE_NUMBER;
        while (tmp > 0) {
            fileNumberStringLength++;
            tmp /= 10;
        }

        String bodyFormat = "%s%0" + fileNumberStringLength + "d%s";
        for (int i = MIN_FILE_NUMBER; i <= MAX_FILE_NUMBER; i++) {
            boolean isExitFile = false;
            String body = String.format(bodyFormat, mFileNamePrefix, i, mFileNameSuffix);
            String fileName = String.format("%s%s.%s", destDirectory, body, mFileNameExtension);
            File file = new File(fileName);
            if (file.exists()) {
                isExitFile = true;
            } 
            if (!isExitFile) {
                MmsLog.w(TAG, "exportMessages getAppropriateFileName fileName =" + fileName);
                return fileName;
            }
        }
        return null;
    }

    /// M: Code analyze 007, For fix bug ALPS00239606, ANR:Store the sms to
    // memory first, then copy it to SD card. @{
    private int copyToPhoneMemory(Cursor cursor, String dest) {
        SQLiteDatabase db =  openOrCreateDatabase(dest, 1, null);
        db.execSQL("CREATE TABLE sms ("
                + "_id INTEGER PRIMARY KEY,"
                + "thread_id INTEGER,"
                + "address TEXT,"
                + "m_size INTEGER,"
                + "person INTEGER,"
                + "date INTEGER,"
                + "date_sent INTEGER DEFAULT 0,"
                + "protocol INTEGER,"
                + "read INTEGER DEFAULT 0,"
                + "status INTEGER DEFAULT -1,"
                + "type INTEGER," + "reply_path_present INTEGER,"
                + "subject TEXT," + "body TEXT," + "service_center TEXT,"
                + "locked INTEGER DEFAULT 0," + "sim_id INTEGER DEFAULT -1,"
                + "error_code INTEGER DEFAULT 0," + "seen INTEGER DEFAULT 0"
                + ");");
        db.beginTransaction();
        MmsLog.d(TAG, "export mem begin");
        int count = 0;
        while (cursor.moveToNext()) {
            int messageType = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
            if (messageType == 3) {
                continue;
            }
            ContentValues smsValue = new ContentValues();
            int threadId = cursor.getInt(cursor.getColumnIndexOrThrow("thread_id"));
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            Long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
            int simId = cursor.getInt(cursor.getColumnIndexOrThrow("sim_id"));
            int read = cursor.getInt(cursor.getColumnIndexOrThrow("read")); 
            int seen = cursor.getInt(cursor.getColumnIndexOrThrow("seen")); 
            String serviceCenter = cursor.getString(cursor.getColumnIndexOrThrow("service_center"));
            smsValue.put(Sms.READ, read);
            smsValue.put(Sms.SEEN, seen);
            smsValue.put(Sms.BODY, body);
            smsValue.put(Sms.DATE, date);
            smsValue.put(EncapsulatedTelephony.Sms.SIM_ID, simId);
            smsValue.put(Sms.SERVICE_CENTER, serviceCenter);
            smsValue.put(Sms.TYPE, messageType);
            smsValue.put(Sms.ADDRESS, address);
            db.insert(TABLE_SMS, null, smsValue);
            count++;
        } 
        MmsLog.d(TAG, "export mem end count = " + count);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();   
        return count;
   }
    /// @}

    /// M: Code analyze 002, new feature, Import/Export SMS. @{
    private void copyToSDMemory(String src, String dst) throws IOException {
        MmsLog.d(TAG, "export sdcard begin dst = " + dst);
        InputStream myInput = null;
        OutputStream myOutput = null;
        try {
            final String sdCardDirPath = EncapsulatedStorageManager.getDefaultPath() + SDCARD_MESSAGE_DIR_PATH;
            myInput = new FileInputStream(src);
            File dir = new File(sdCardDirPath);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    MmsLog.w(TAG,"copyToSDMemory. mkDir:" + sdCardDirPath + "  has failed");
                    return;
                }
            }
            File dstFile = new File(dst);
            if (!dstFile.exists()) {
                if (!dstFile.createNewFile()) {
                    MmsLog.w(TAG,"copyToSDMemory. createNewFile:" + dst + "  has failed");
                    return;
                }
            }
            myOutput = new FileOutputStream(dstFile);
             //transfer bytes from the inputfile to the outputfile
             byte[] buffer = new byte[1024];
             int length;
             while ((length = myInput.read(buffer)) > 0) {
                 myOutput.write(buffer, 0, length);
             }
             myOutput.flush();
             myOutput.close();
             myInput.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            MmsLog.e(TAG, "export sdcard FileNotFoundException");
        } catch (IOException e) {
            MmsLog.e(TAG, "export sdcard IOException");
            throw e;
        } catch (IndexOutOfBoundsException e) {
            MmsLog.e(TAG, "export sdcard IndexOutOfBoundsException");
            e.printStackTrace();
        }
        MmsLog.d(TAG, "export sdcard end");
    }

    private boolean isSDcardReady() {
        boolean isSDcard = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); 
        if (!isSDcard) {
            showToast(R.string.no_sd_card);
            MmsLog.d(TAG, "there is no SD card");
            return false;
        }
        return true;
    }
    
    private void showToast(int id) { 
        Toast t = Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_SHORT);
        t.show();
    } 
    /// @}
    
    @Override
    /// M: Code analyze 011, For fix bug ALPS00275951, The
    // "Roaming auto-retrieve" checkbox doesnot displayed in the same vertical
    // line with others. @{
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged: newConfig = " + newConfig + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        this.getListView().clearScrapViewsIfNeeded();
    }
    /// @}

    /// M: Code analyze 012, For fix bug ALPS00296115, merge dual talk (EVDO)
    // into ALPS.ICS auto merge. @{
    public boolean isUSimType(int slot) {
        /** M: MTK Encapsulation ITelephony */
        // final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        EncapsulatedTelephonyService iTel = EncapsulatedTelephonyService.getInstance();
        if (iTel == null) {
            Log.d(TAG, "[isUIMType]: iTel = null");
            return false;
        }
        
        try {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                return iTel.getIccCardTypeGemini(slot).equals("UIM");
            } else {
                return iTel.getIccCardType().equals("UIM");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[isUSIMType]: " + String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (NullPointerException e) {
            Log.e(TAG, "[isUSIMType]: " + String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }
    /// @}

    @Override
    protected void onPause() {
        super.onPause();
        if (mSmsDisplayLimitDialog != null) {
            mSmsDisplayLimitDialog.dismiss();
        }
        if (mMmsDisplayLimitDialog != null) {
            mMmsDisplayLimitDialog.dismiss();
        }
    }

    private void setListPrefSummary() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        //For mMmsPriority;
        String stored = sp.getString(PRIORITY, getString(R.string.priority_normal));
        mMmsPriority.setSummary(getVisualTextName(stored, R.array.pref_key_mms_priority_choices,
                R.array.pref_key_mms_priority_values));
        /// M: Code analyze 006, For fix bug ALPS00239351, Set the key of save
        // location as (slot id)_key when op02. @{
        String saveLocation = null;
        if (MmsConfig.getSmsMultiSaveLocationEnabled()) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                int currentSimCount = SIMInfo.getInsertedSIMCount(this);
                int slotId = 0;
                if (currentSimCount == 1) {
                    slotId = SIMInfo.getInsertedSIMList(this).get(0).getSlot();
                    saveLocation = sp.getString((Long.toString(slotId) + "_" + SMS_SAVE_LOCATION), "Phone");
                }
                MmsLog.d(TAG, "setListPrefSummary mSmsLocation stored slotId = " + slotId + " stored =" + stored);
            } else {
                saveLocation = sp.getString(SMS_SAVE_LOCATION, "Phone");
                MmsLog.d(TAG, "setListPrefSummary mSmsLocation stored 2 =" + stored);
            }
        }
        /// @}

        if (saveLocation == null) {
            saveLocation = sp.getString(SMS_SAVE_LOCATION, "Phone");
        }
        if (!getResources().getBoolean(R.bool.isTablet)) {
            mSmsLocation.setSummary(getVisualTextName(saveLocation, R.array.pref_sms_save_location_choices,
                R.array.pref_sms_save_location_values));
        } else {
            mSmsLocation.setSummary(getVisualTextName(saveLocation, R.array.pref_tablet_sms_save_location_choices,
                R.array.pref_tablet_sms_save_location_values));
        }
        
        //For mMmsCreationMode
        stored = sp.getString(CREATION_MODE, "FREE");
        mMmsCreationMode.setSummary(getVisualTextName(stored, R.array.pref_mms_creation_mode_choices,
                R.array.pref_mms_creation_mode_values));
        
        //For mMmsSizeLimit
        stored = sp.getString(MMS_SIZE_LIMIT, "300");
        mMmsSizeLimit.setSummary(getVisualTextName(stored, R.array.pref_mms_size_limit_choices,
                R.array.pref_mms_size_limit_values));
    }

    /// M: Code analyze 002, new feature, Import/Export SMS. @{
    private void newMainHandler() {
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) { 
                int output = R.string.export_message_empty;  
                switch(msg.what) { 
                case EXPORT_SUCCES: 
                    output = R.string.export_message_success;
                    break;
                case EXPORT_FAILED: 
                    output = R.string.export_message_fail;
                    break;
                case IMPORT_SUCCES: 
                    output = R.string.import_message_success;
                    break;
                case IMPORT_FAILED: 
                    output = R.string.import_message_fail;
                    break;
                case EXPORT_EMPTY_SMS: 
                    output = R.string.export_message_empty;
                    break;    
                case DISK_IO_FAILED: 
                    output = R.string.export_disk_problem;
                    break;
                default: 
                    break;
                }
                showToast(output);
            }
        };
    }
    /// @}

    /// M: Code analyze 009, For fix bug ALPS00274221, Remove the entry of
    // import/export SMS. @{
    private void removeBackupMessage() {
        PreferenceCategory portPref = (PreferenceCategory)findPreference("pref_title_io_settings");
        getPreferenceScreen().removePreference(portPref);
    }
    /// @}

    /// M: add import/export Message 
    private void addBankupMessages() {
        mImportMessages = findPreference(MSG_IMPORT);
        mExportMessages = findPreference(MSG_EXPORT); 
    }

    /// M: add input mode setting for op03 request, if not remove it.
    private void addSmsInputModePreference() {
        if (MmsConfig.getSmsEncodingTypeEnabled()) {
             mSmsInputMode = (ListPreference) findPreference(SMS_INPUT_MODE);
        } else {
             PreferenceCategory smsCategory = (PreferenceCategory)findPreference("pref_key_sms_settings");
             mSmsInputMode = (ListPreference) findPreference(SMS_INPUT_MODE);
             if (mSmsInputMode != null) {
                smsCategory.removePreference(mSmsInputMode);
             }
        }
    }

    private void changeSingleCardKeyToSimRelated() {
        // get to know which one
        mListSimInfo = SIMInfo.getInsertedSIMList(this);
        SIMInfo singleCardInfo = null;
        if (mListSimInfo.size() != 0) {
            singleCardInfo = mListSimInfo.get(0);
        }
        if (singleCardInfo == null) {
            return;
        }
        Long simId = mListSimInfo.get(0).getSimId();
        MmsLog.d(TAG,"changeSingleCardKeyToSimRelated Got simId = " + simId);
        //translate all key to SIM-related key;
        mSmsDeliveryReport = (CheckBoxPreference) findPreference(SMS_DELIVERY_REPORT_MODE);
        mMmsDeliveryReport = (CheckBoxPreference) findPreference(MMS_DELIVERY_REPORT_MODE);
        mMmsReadReport = (CheckBoxPreference) findPreference(READ_REPORT_MODE);
        // M: add this for read report
        mMmsAutoReplyReadReport = (CheckBoxPreference) findPreference(READ_REPORT_AUTO_REPLY);

        if (EncapsulatedFeatureOption.EVDO_DT_SUPPORT && isUSimType(mListSimInfo.get(0).getSlot())) {
            mMmsAutoReplyReadReport.setEnabled(false);
            mMmsReadReport.setEnabled(false);
        }

        mMmsAutoRetrieval = (CheckBoxPreference) findPreference(AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoaming = (CheckBoxPreference) findPreference(RETRIEVAL_DURING_ROAMING);
        mSmsServiceCenterPref = findPreference(SMS_SERVICE_CENTER);
        mSmsValidityPeriodPref = findPreference(SMS_VALIDITY_PERIOD);
        mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
        mManageSimPrefMultiSim = null;
        PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");
        if (MmsConfig.getSmsMultiSaveLocationEnabled()) {
            int slotid = mListSimInfo.get(0).getSlot();
            mSmsLocation = (ListPreference) findPreference(SMS_SAVE_LOCATION);
            mSmsLocation.setKey(Long.toString(slotid) + "_" + SMS_SAVE_LOCATION);
            SharedPreferences spr = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
            mSmsLocation.setValue(spr.getString((Long.toString(slotid) + "_" + SMS_SAVE_LOCATION), "Phone"));
        }

        if (!MmsConfig.getSIMSmsAtSettingEnabled()) {
            if (mManageSimPref != null) {
                smsCategory.removePreference(mManageSimPref);
            }
        }

        mSmsDeliveryReport.setKey(Long.toString(simId) + "_" + SMS_DELIVERY_REPORT_MODE);
        mMmsDeliveryReport.setKey(Long.toString(simId) + "_" + MMS_DELIVERY_REPORT_MODE);  
        mMmsReadReport.setKey(Long.toString(simId) + "_" + READ_REPORT_MODE);
        // M: add this for read report
        if (mMmsAutoReplyReadReport != null) {
            mMmsAutoReplyReadReport.setKey(Long.toString(simId) + "_" + READ_REPORT_AUTO_REPLY); 
        }
        mMmsAutoRetrieval.setKey(Long.toString(simId) + "_" + AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoaming.setDependency(Long.toString(simId) + "_" + AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoaming.setKey(Long.toString(simId) + "_" + RETRIEVAL_DURING_ROAMING);
        
        if (MmsConfig.getDeliveryReportAllowed()) {
            mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            mMmsEnableToSendDeliveryReport.setKey(Long.toString(simId) + "_" + MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
        } else {
            mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            if (mMmsEnableToSendDeliveryReport != null) {
                mMmsEnableToSendDeliveryReport.setKey(Long.toString(simId) + "_" + MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                PreferenceCategory mmsCategory = (PreferenceCategory)findPreference("pref_key_mms_settings");
                mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
            } 
        }
        
        //get the stored value
        SharedPreferences sp = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
        if (mSmsDeliveryReport != null) {
            mSmsDeliveryReport.setChecked(sp.getBoolean(mSmsDeliveryReport.getKey(), false));
        }
        if (mMmsEnableToSendDeliveryReport != null) {
            mMmsEnableToSendDeliveryReport.setChecked(sp.getBoolean(mMmsEnableToSendDeliveryReport.getKey(), false));
        }
        if (mMmsDeliveryReport != null) {
            mMmsDeliveryReport.setChecked(sp.getBoolean(mMmsDeliveryReport.getKey(), false));
        }
        if (mMmsAutoRetrieval != null) {
            mMmsAutoRetrieval.setChecked(sp.getBoolean(mMmsAutoRetrieval.getKey(), true));
        }
        if (mMmsRetrievalDuringRoaming != null) {
            mMmsRetrievalDuringRoaming.setChecked(sp.getBoolean(mMmsRetrievalDuringRoaming.getKey(), false));
        }
        if (mMmsReadReport != null) {
            mMmsReadReport.setChecked(sp.getBoolean(mMmsReadReport.getKey(), false));
        }
        // M: add for read report
        if (mMmsAutoReplyReadReport != null) {
            mMmsAutoReplyReadReport.setChecked(sp.getBoolean(mMmsAutoReplyReadReport.getKey(), false));
        }        
    }

    private void setMultiCardPreference() {
        mSmsDeliveryReportMultiSim = findPreference(SMS_DELIVERY_REPORT_MODE);
        mMmsDeliveryReportMultiSim = findPreference(MMS_DELIVERY_REPORT_MODE);
        if (MmsConfig.getDeliveryReportAllowed()) {
            mMmsEnableToSendDeliveryReportMultiSim = findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
        } else {
            mMmsEnableToSendDeliveryReportMultiSim = findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            PreferenceCategory mmsCategory =
                (PreferenceCategory)findPreference("pref_key_mms_settings");
            mmsCategory.removePreference(mMmsEnableToSendDeliveryReportMultiSim);
        }
        
        mMmsReadReportMultiSim = findPreference(READ_REPORT_MODE);
        // M: add this for read report
        mMmsAutoReplyReadReportMultiSim = findPreference(READ_REPORT_AUTO_REPLY);
        mMmsAutoRetrievalMultiSim = findPreference(AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoamingMultiSim = findPreference(RETRIEVAL_DURING_ROAMING);
        mSmsServiceCenterPrefMultiSim = findPreference(SMS_SERVICE_CENTER);
        mSmsValidityPeriodPrefMultiSim = findPreference(SMS_VALIDITY_PERIOD);        
        mManageSimPrefMultiSim = findPreference(SMS_MANAGE_SIM_MESSAGES);
        mManageSimPref = null;
        PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");

        if (MmsConfig.getSmsMultiSaveLocationEnabled()) {
            if (mSmsLocation != null) {
                smsCategory.removePreference(mSmsLocation);
                Preference saveLocationMultiSim = new Preference(this);
                saveLocationMultiSim.setKey(SMS_SAVE_LOCATION);
                saveLocationMultiSim.setTitle(R.string.sms_save_location);
                saveLocationMultiSim.setSummary(R.string.sms_save_location);
                smsCategory.addPreference(saveLocationMultiSim);
                mSmsSaveLoactionMultiSim = findPreference(SMS_SAVE_LOCATION);
           }
        }

        if (!MmsConfig.getSIMSmsAtSettingEnabled()) {
            if (mManageSimPrefMultiSim != null) {
                smsCategory.removePreference(mManageSimPrefMultiSim);
            }
        }
        mSmsDeliveryReportMultiSim.setKey(MessagingPreferenceActivity.SMS_DELIVERY_REPORT_MODE);
        mMmsDeliveryReportMultiSim.setKey(MessagingPreferenceActivity.MMS_DELIVERY_REPORT_MODE);
        mMmsAutoRetrievalMultiSim.setKey(MessagingPreferenceActivity.AUTO_RETRIEVAL);
        mMmsReadReportMultiSim.setKey(MessagingPreferenceActivity.READ_REPORT_MODE);
        mMmsRetrievalDuringRoamingMultiSim.setKey(MessagingPreferenceActivity.RETRIEVAL_DURING_ROAMING);
        mMmsAutoReplyReadReportMultiSim.setKey(MessagingPreferenceActivity.READ_REPORT_AUTO_REPLY);
        mCellBroadcastMultiSim = findPreference(CELL_BROADCAST);
    }

    /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
    private class PositiveButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // write to the SIM Card.
            final EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                mSlotId = mListSimInfo.get(0).getSlot();
            } else {
                mSlotId = 0;
            }
            new Thread(new Runnable() {
                public void run() {
                    try {
                        teleService.setScAddressGemini(mNumberText.getText().toString(), mSlotId);
                    } catch(RemoteException e1) {
                        MmsLog.e(TAG,"setScAddressGemini is failed.\n" + e1.toString());
                    } catch(NullPointerException e2) {
                        MmsLog.e(TAG,"setScAddressGemini is failed.\n" + e2.toString());
                    }
                }
            }).start();
        }
    }
    /// @}

    /// M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang. @{
    private class NegativeButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // cancel
            dialog.dismiss();
        }
    }
    /// @}

    /** M: Code analyze 001, For fix bug ALPS00104512, use in device by Hongduo Wang.
     * Notes: if wap push is not support, wap push setting should be removed
     * 
     */
    private void enablePushSetting() {
        
        PreferenceCategory wapPushOptions =
            (PreferenceCategory)findPreference("pref_key_wappush_settings");
        
        if (EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT) {  
            if (!MmsConfig.getSlAutoLanuchEnabled()) {
                wapPushOptions.removePreference(findPreference("pref_key_wappush_sl_autoloading"));
            }
        } else {
            if (getPreferenceScreen() != null) {
                getPreferenceScreen().removePreference(wapPushOptions);
            }
        }
    }
    /// M: add for get resources array by id
    private String[] getResourceArray(int resId) {
        return getResources().getStringArray(resId);
    }
    
    /// M: add for get shared preference value(int) by key
    private int getPreferenceValueInt(String key, int defaultValue) {
        SharedPreferences sp = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
        return sp.getInt(key, defaultValue);
    }
}
