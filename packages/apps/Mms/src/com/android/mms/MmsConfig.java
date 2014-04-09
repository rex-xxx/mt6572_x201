/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mms;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.telephony.TelephonyProperties;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

/// M:
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ext.IMmsConfig;
import com.mediatek.mms.ext.MmsConfigImpl;
import android.content.Intent;
import android.util.AndroidException;
import android.provider.MediaStore;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
/// M: add for ipmessage
import java.io.File;
import java.util.ArrayList;

import com.android.internal.telephony.Phone;
import com.android.mms.ui.MessageUtils;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.custom.EncapsulatedCustomProperties;
import com.mediatek.mms.ipmessage.IpMessageConsts;
/// M: add for CMCC FT
import org.apache.http.params.HttpParams;

/// M: ALPS00527989, Extend TextView URL handling @ {
import android.widget.TextView;
/// @}

public class MmsConfig {
    private static final String TAG = "MmsConfig";
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = false;

    private static final String DEFAULT_HTTP_KEY_X_WAP_PROFILE = "x-wap-profile";
    private static final String DEFAULT_USER_AGENT = "Android-Mms/2.0";

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH  = 640;

    /**
     * Whether to hide MMS functionality from the user (i.e. SMS only).
     */
    private static boolean mTransIdEnabled = false;
    private static int mMmsEnabled = 1;                         // default to true
    private static int mMaxMessageSize = 300 * 1024;            // default to 300k max size    
    private static String mUserAgent = DEFAULT_USER_AGENT;
    private static String mUaProfTagName = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
    private static String mUaProfUrl = null;
    private static String mHttpParams = null;
    private static String mHttpParamsLine1Key = null;
    private static String mEmailGateway = null;
    private static int mMaxImageHeight = MAX_IMAGE_HEIGHT;      // default value
    private static int mMaxImageWidth = MAX_IMAGE_WIDTH;        // default value
    private static int mRecipientLimit = Integer.MAX_VALUE;     // default value    
    private static int mDefaultSMSMessagesPerThread = 500;      // default value
    private static int mDefaultMMSMessagesPerThread = 50;       // default value
    private static int mMinMessageCountPerThread = 2;           // default value
    private static int mMaxMessageCountPerThread = 5000;        // default value
    private static int mMinimumSlideElementDuration = 7;        // default to 7 sec
    private static boolean mNotifyWapMMSC = false;
    private static boolean mAllowAttachAudio = true;

    // This flag is somewhat confusing. If mEnableMultipartSMS is true, long sms messages are
    // always sent as multi-part sms messages, with no checked limit on the number of segments.
    // If mEnableMultipartSMS is false, then mSmsToMmsTextThreshold is used to determine the
    // limit of the number of sms segments before turning the long sms message into an mms
    // message. For example, if mSmsToMmsTextThreshold is 4, then a long sms message with three
    // or fewer segments will be sent as a multi-part sms. When the user types more characters
    // to cause the message to be 4 segments or more, the send button will show the MMS tag to
    // indicate the message will be sent as an mms.
    private static boolean mEnableMultipartSMS = true;

    private static boolean mEnableSlideDuration = true;
    private static boolean mEnableMMSReadReports = true;        // key: "enableMMSReadReports"
    private static boolean mEnableSMSDeliveryReports = true;    // key: "enableSMSDeliveryReports"
    private static boolean mEnableMMSDeliveryReports = true;    // key: "enableMMSDeliveryReports"
    private static int mMaxTextLength = -1;

    // This is the max amount of storage multiplied by mMaxMessageSize that we
    // allow of unsent messages before blocking the user from sending any more
    // MMS's.
    private static int mMaxSizeScaleForPendingMmsAllowed = 4;       // default value

    // Email gateway alias support, including the master switch and different rules
    private static boolean mAliasEnabled = false;
    private static int mAliasRuleMinChars = 2;
    private static int mAliasRuleMaxChars = 48;

    private static int mMaxSubjectLength = 40;  // maximum number of characters allowed for mms
                                                // subject

    /// M: google jb.mr1 patch, group mms
    // If mEnableGroupMms is true, a message with multiple recipients, regardless of contents,
    // will be sent as a single MMS message with multiple "TO" fields set for each recipient.
    // If mEnableGroupMms is false, the group MMS setting/preference will be hidden in the settings
    // activity.
    private static boolean mEnableGroupMms = true;

    private static final int RECIPIENTS_LIMIT = 50;
    
    /// M: Mms size limit, default 300K.
    private static int mUserSetMmsSizeLimit = 300; 
    /// M: Receive Mms size limit for 2G network
    private static int mReceiveMmsSizeLimitFor2G = 200;  
    /// M: Receive Mms size limit for TD network
    private static int mReceiveMmsSizeLimitForTD = 400;  
    
    /// M: default value
    private static int mMaxRestrictedImageHeight = 1200;
    private static int mMaxRestrictedImageWidth = 1600;            
    private static int mSmsRecipientLimit = 100;
    
    private static boolean mDeviceStorageFull = false;

    private static IMmsConfig mMmsConfigPlugin = null;
                                                
    private static void initPlugin(Context context){
        try {
            mMmsConfigPlugin = (IMmsConfig)EncapsulatedPluginManager.createPluginObject(context, IMmsConfig.class.getName());
            MmsLog.d(TAG, "operator mMmsConfigPlugin = " + mMmsConfigPlugin);
        } catch (AndroidException e) {
            mMmsConfigPlugin = new MmsConfigImpl();
            MmsLog.d(TAG, "default mMmsConfigPlugin = " + mMmsConfigPlugin);
        }
    }

    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.init()");
        }
        // Always put the mnc/mcc in the log so we can tell which mms_config.xml was loaded.
        Log.v(TAG, "mnc/mcc: " +
                android.os.SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC));
        
        // initialize the operator plugin
        initPlugin(context);
        
        loadMmsSettings(context);

        /// M: add for ipmessage
        if (IpMessageUtils.getIpMessagePlugin(context).isActualPlugin()) {
            initializeIpMessageFilePath(context);
        }
        loadSimInfo(context);
    }
    /// M: this method is changed to use plugin
    public static int getSmsToMmsTextThreshold() {
        // Operator Plugin
        return mMmsConfigPlugin.getSmsToMmsTextThreshold();
    }

    public static boolean getMmsEnabled() {
        return mMmsEnabled == 1 ? true : false;
    }

    public static int getMaxMessageSize() {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.getMaxMessageSize(): " + mMaxMessageSize);
        }
        return mMaxMessageSize;
    }

    /**
     * This function returns the value of "enabledTransID" present in mms_config file.
     * In case of single segment wap push message, this "enabledTransID" indicates whether
     * TransactionID should be appended to URI or not.
     */
    public static boolean getTransIdEnabled() {
        return mTransIdEnabled;
    }

    public static String getUserAgent() {
        /// M: @{
        String value = EncapsulatedCustomProperties.getString(
                EncapsulatedCustomProperties.MODULE_MMS,
                EncapsulatedCustomProperties.USER_AGENT,
                mUserAgent);
        /// @}
        return value;
    }

    public static String getUaProfTagName() {
        return mUaProfTagName;
    }

    public static String getUaProfUrl() {
        /// M: @{
        String value = EncapsulatedCustomProperties.getString(
                EncapsulatedCustomProperties.MODULE_MMS,
                EncapsulatedCustomProperties.UAPROF_URL,
                mUaProfUrl);
        /// @}
        return value;
    }

    public static String getHttpParams() {
        return mHttpParams;
    }

    public static String getHttpParamsLine1Key() {
        return mHttpParamsLine1Key;
    }

    public static String getEmailGateway() {
        return mEmailGateway;
    }

    public static int getMaxImageHeight() {
        return mMaxImageHeight;
    }

    public static int getMaxImageWidth() {
        return mMaxImageWidth;
    }

    public static int getRecipientLimit() {
        return mRecipientLimit;
    }

    /// M: change this to plugin
    public static int getMaxTextLimit() {
        return mMmsConfigPlugin.getMaxTextLimit();
    }

    public static int getDefaultSMSMessagesPerThread() {
        return mDefaultSMSMessagesPerThread;
    }

    public static int getDefaultMMSMessagesPerThread() {
        return mDefaultMMSMessagesPerThread;
    }

    public static int getMinMessageCountPerThread() {
        return mMinMessageCountPerThread;
    }

    public static int getMaxMessageCountPerThread() {
        return mMaxMessageCountPerThread;
    }

    public static int getHttpSocketTimeout() {
        return mMmsConfigPlugin.getHttpSocketTimeout();
    }

    public static int getMinimumSlideElementDuration() {
        return mMinimumSlideElementDuration;
    }

    public static boolean getMultipartSmsEnabled() {
        return mEnableMultipartSMS;
    }

    public static boolean getSlideDurationEnabled() {
        return mEnableSlideDuration;
    }

    public static boolean getMMSReadReportsEnabled() {
        return mEnableMMSReadReports;
    }

    public static boolean getSMSDeliveryReportsEnabled() {
        return mEnableSMSDeliveryReports;
    }

    public static boolean getMMSDeliveryReportsEnabled() {
        return mEnableMMSDeliveryReports;
    }

    public static boolean getNotifyWapMMSC() {
        return mNotifyWapMMSC;
    }

    public static int getMaxSizeScaleForPendingMmsAllowed() {
        return mMaxSizeScaleForPendingMmsAllowed;
    }

    public static boolean isAliasEnabled() {
        return mAliasEnabled;
    }

    public static int getAliasMinChars() {
        return mAliasRuleMinChars;
    }

    public static int getAliasMaxChars() {
        return mAliasRuleMaxChars;
    }

    public static boolean getAllowAttachAudio() {
        return mAllowAttachAudio;
    }

    public static int getMaxSubjectLength() {
        return mMaxSubjectLength;
    }

    /// M: google jb.mr1 patch, group mms
    public static boolean getGroupMmsEnabled() {
        return mEnableGroupMms;
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }

        if (type != parser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }
    }

    private static void loadMmsSettings(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.mms_config);

        try {
            beginDocument(parser, "mms_config");

            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }
                String name = parser.getAttributeName(0);
                String value = parser.getAttributeValue(0);
                String text = null;
                if (parser.next() == XmlPullParser.TEXT) {
                    text = parser.getText();
                }

                if (DEBUG) {
                    Log.v(TAG, "tag: " + tag + " value: " + value + " - " +
                            text);
                }
                if ("name".equalsIgnoreCase(name)) {
                    if ("bool".equals(tag)) {
                        // bool config tags go here
                        if ("enabledMMS".equalsIgnoreCase(value)) {
                            mMmsEnabled = "true".equalsIgnoreCase(text) ? 1 : 0;
                        } else if ("enabledTransID".equalsIgnoreCase(value)) {
                            mTransIdEnabled = "true".equalsIgnoreCase(text);
                        } else if ("enabledNotifyWapMMSC".equalsIgnoreCase(value)) {
                            mNotifyWapMMSC = "true".equalsIgnoreCase(text);
                        } else if ("aliasEnabled".equalsIgnoreCase(value)) {
                            mAliasEnabled = "true".equalsIgnoreCase(text);
                        } else if ("allowAttachAudio".equalsIgnoreCase(value)) {
                            mAllowAttachAudio = "true".equalsIgnoreCase(text);
                        } else if ("enableMultipartSMS".equalsIgnoreCase(value)) {
                            mEnableMultipartSMS = "true".equalsIgnoreCase(text);
                        } else if ("enableSlideDuration".equalsIgnoreCase(value)) {
                            mEnableSlideDuration = "true".equalsIgnoreCase(text);
                        } else if ("enableMMSReadReports".equalsIgnoreCase(value)) {
                            mEnableMMSReadReports = "true".equalsIgnoreCase(text);
                        } else if ("enableSMSDeliveryReports".equalsIgnoreCase(value)) {
                            mEnableSMSDeliveryReports = "true".equalsIgnoreCase(text);
                        } else if ("enableMMSDeliveryReports".equalsIgnoreCase(value)) {
                            mEnableMMSDeliveryReports = "true".equalsIgnoreCase(text);
                        /// M: google jb.mr1 patch, group mms
                        } else if ("enableGroupMms".equalsIgnoreCase(value)) {
                            mEnableGroupMms = "true".equalsIgnoreCase(text);
                        }
                    } else if ("int".equals(tag)) {
                        // int config tags go here
                        if ("maxMessageSize".equalsIgnoreCase(value)) {
                            mMaxMessageSize = Integer.parseInt(text);
                        } else if ("maxImageHeight".equalsIgnoreCase(value)) {
                            mMaxImageHeight = Integer.parseInt(text);
                        } else if ("maxImageWidth".equalsIgnoreCase(value)) {
                            mMaxImageWidth = Integer.parseInt(text);
                        } 
                        /// M: @{
                        else if ("maxRestrictedImageHeight".equalsIgnoreCase(value)) {
                            mMaxRestrictedImageHeight = Integer.parseInt(text);
                        }else if ("maxRestrictedImageWidth".equalsIgnoreCase(value)) {
                            mMaxRestrictedImageWidth = Integer.parseInt(text);
                        } 
                        /// @}
                        else if ("defaultSMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultSMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("defaultMMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultMMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("minMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMinMessageCountPerThread = Integer.parseInt(text);
                        } else if ("maxMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMaxMessageCountPerThread = Integer.parseInt(text);
                        } 
                        else if ("smsToMmsTextThreshold".equalsIgnoreCase(value)) {
                            /// M: Operator Plugin
                            mMmsConfigPlugin.setSmsToMmsTextThreshold(Integer.parseInt(text));
                        } 
                        else if ("recipientLimit".equalsIgnoreCase(value)) {
                            /// M: Operator Plugin
                            mMmsConfigPlugin.setMmsRecipientLimit(Integer.parseInt(text));
                        } else if ("httpSocketTimeout".equalsIgnoreCase(value)) {
                            mMmsConfigPlugin.setHttpSocketTimeout(Integer.parseInt(text));
                        } else if ("minimumSlideElementDuration".equalsIgnoreCase(value)) {
                            mMinimumSlideElementDuration = Integer.parseInt(text);
                        } else if ("maxSizeScaleForPendingMmsAllowed".equalsIgnoreCase(value)) {
                            mMaxSizeScaleForPendingMmsAllowed = Integer.parseInt(text);
                        } else if ("aliasMinChars".equalsIgnoreCase(value)) {
                            mAliasRuleMinChars = Integer.parseInt(text);
                        } else if ("aliasMaxChars".equalsIgnoreCase(value)) {
                            mAliasRuleMaxChars = Integer.parseInt(text);
                        } else if ("maxMessageTextSize".equalsIgnoreCase(value)) {
                            /// M: Operator Plugin
                            mMmsConfigPlugin.setMaxTextLimit(Integer.parseInt(text));
                        } else if ("maxSubjectLength".equalsIgnoreCase(value)) {
                            mMaxSubjectLength = Integer.parseInt(text);
                        }
                    } else if ("string".equals(tag)) {
                        // string config tags go here
                        if ("userAgent".equalsIgnoreCase(value)) {
                            mUserAgent = text;
                        } else if ("uaProfTagName".equalsIgnoreCase(value)) {
                            mUaProfTagName = text;
                        } else if ("uaProfUrl".equalsIgnoreCase(value)) {
                            /// M: Operator Plugin
                            mUaProfUrl = mMmsConfigPlugin.getUAProf(text);
                        } else if ("httpParams".equalsIgnoreCase(value)) {
                            mHttpParams = text;
                        } else if ("httpParamsLine1Key".equalsIgnoreCase(value)) {
                            mHttpParamsLine1Key = text;
                        } else if ("emailGatewayNumber".equalsIgnoreCase(value)) {
                            mEmailGateway = text;
                        }
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (NumberFormatException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (IOException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } finally {
            parser.close();
        }

        String errorStr = null;

        if (getMmsEnabled() && mUaProfUrl == null) {
            errorStr = "uaProfUrl";
        }

        if (errorStr != null) {
            String err =
                String.format("MmsConfig.loadMmsSettings mms_config.xml missing %s setting",
                        errorStr);
            Log.e(TAG, err);
        }
    }
    
    /// M:
    /**
     * Notes:for CMCC customization,whether to enable SL automatically lanuch.
     * default set false
     */
    private static boolean mSlAutoLanuchEnabled = false;
    public static boolean getSlAutoLanuchEnabled(){
        return mSlAutoLanuchEnabled;
    }

    public static void setDeviceStorageFullStatus(boolean bFull) {
        mDeviceStorageFull = bFull;
    }

    public static boolean getDeviceStorageFullStatus() {
        return mDeviceStorageFull;
    }

    /// M: add for cmcc dir ui @{
    public static void setMmsDirMode(boolean mode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("CmccMmsUiMode", mode);
        editor.commit();
    }

    public static boolean getMmsDirMode() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean dirMode = sp.getBoolean("CmccMmsUiMode", false);
        return dirMode;
    }
    // @}
    
    /// M: add for cmcc dir ui @{
    public static void setSimCardInfo(int simcard) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("CmccSimCardInfo", simcard);
        editor.commit();
    }
    
    public static int getSimCardInfo() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        int siminfo = sp.getInt("CmccSimCardInfo", 0);
        return siminfo;
    }
    /// @}

    /// M: new feature, init defualt quick text @{
    public static void setInitQuickText(boolean init) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("InitQuickText", init);
        editor.commit();
    }

    public static boolean getInitQuickText() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean isFristInit = sp.getBoolean("InitQuickText", true);
        return isFristInit;
    }

    public static void setPreQuickText(String[] strings) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("PreQuickTextNum", strings.length);
        for (int i = 0; i < strings.length; i++) {
            editor.putString("PreQuickText" + String.valueOf(i), strings[i]);
        }
        editor.commit();
    }

    public static ArrayList<String> getPreQuickText() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        int length = sp.getInt("PreQuickTextNum", 9);
        ArrayList<String> strings = new ArrayList<String>();
        for (int i = 0; i < length; i++) {
            String string = sp.getString("PreQuickText" + String.valueOf(i), "");
            strings.add(string);
        }
        return strings;
    }
    /// @}
   
    public static int getUserSetMmsSizeLimit(boolean isBytes) {
        if (true == isBytes) {
            return mUserSetMmsSizeLimit*1024;
        } else {
            return mUserSetMmsSizeLimit;
        }
    }
    
    public static void setUserSetMmsSizeLimit(int limit) {
        mUserSetMmsSizeLimit = limit;
    }
    
    public static int getMaxRestrictedImageHeight() {
        return mMaxRestrictedImageHeight;
    }

    public static int getMaxRestrictedImageWidth() {
        return mMaxRestrictedImageWidth;
    }

    /// M: change this method to plugin
    public static int getMmsRecipientLimit() {
        return mMmsConfigPlugin.getMmsRecipientLimit();
    }

    public static int getSmsRecipientLimit() {
        return mSmsRecipientLimit;
    }
    
    public static int getReceiveMmsLimitFor2G() {
        return mReceiveMmsSizeLimitFor2G;
    }

    public static int getReceiveMmsLimitForTD() {
        return mReceiveMmsSizeLimitForTD;
    }

    public static boolean getSmsEncodingTypeEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableSmsEncodingType(); 
    }

    public static boolean getDeliveryReportAllowed(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableReportAllowed(); 
    }

    public static boolean getSmsMultiSaveLocationEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableMultiSmsSaveLocation(); 
    }

    public static boolean getStorageFullToastEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableStorageFullToast(); 
    }

    public static boolean getFolderModeEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableFolderMode();
    }
    
    public static boolean getForwardWithSenderEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableForwardWithSender();
    }

    public static boolean getDialogForUrlEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableDialogForUrl();
    }

    public static boolean getShowStorageStatusEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableStorageStatusDisp();
    }

    public static boolean getSIMSmsAtSettingEnabled(){
        // Operator Plugin
        return mMmsConfigPlugin.isEnableSIMSmsForSetting();
    }

    public static boolean getSIMLongSmsConcatenateEnabled() {
        // Operator Plugin
        return mMmsConfigPlugin.isEnableSIMLongSmsConcatenate();
    }
    
    public static int getPluginMenuIDBase(){
        return 0x100;
    }

    public static Intent getCapturePictureIntent(){
        Intent intent = mMmsConfigPlugin.getCapturePictureIntent();
        if(intent == null){
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, TempFileProvider.SCRAP_CONTENT_URI);
        }
        return intent;
    }

    public static boolean isShowUrlDialog(){
        return mMmsConfigPlugin.isShowUrlDialog();
    }

    public static boolean getAdjustFontSizeEnabled() {
        return mMmsConfigPlugin.isEnableAdjustFontSize();
    }

    public static boolean getSmsValidityPeriodEnabled() {
        return mMmsConfigPlugin.isEnableSmsValidityPeriod();
    }

    /// M: add for ipmessage
    private static String sPicTempPath = "";
    private static String sAudioTempPath = "";
    private static String sVideoTempPath = "";
    private static String sVcardTempPath = "";
    private static String sCalendarTempPath = "";
    private static void initializeIpMessageFilePath(Context context) {
        if (IpMessageUtils.getSDCardStatus()) {
            sPicTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "picture";
            File picturePath = new File(sPicTempPath);
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }

            sAudioTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }

            sVideoTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }

            sVcardTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }

            sCalendarTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "calendar";
            File calendarPath = new File(sCalendarTempPath);
            if (!calendarPath.exists()) {
                calendarPath.mkdirs();
            }

            String cachePath = IpMessageUtils.getCachePath(context);
            File f = new File(cachePath);
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }

    public static String getPicTempPath(Context context) {
        if (TextUtils.isEmpty(sPicTempPath)) {
            sPicTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "picture";
            File picturePath = new File(sPicTempPath);
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }
        return sPicTempPath;
    }

    public static String getAudioTempPath(Context context) {
        if (TextUtils.isEmpty(sAudioTempPath)) {
            sAudioTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }
        }
        return sAudioTempPath;
    }

    public static String getVideoTempPath(Context context) {
        if (TextUtils.isEmpty(sVideoTempPath)) {
            sVideoTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }
        }
        return sVideoTempPath;
    }

    public static String getVcardTempPath(Context context) {
        if (TextUtils.isEmpty(sVcardTempPath)) {
            sVcardTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }
        }
        return sVcardTempPath;
    }

    public static String getVcalendarTempPath(Context context) {
        if (TextUtils.isEmpty(sCalendarTempPath)) {
            sCalendarTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "calendar";
            File calendarPath = new File(sCalendarTempPath);
            if (!calendarPath.exists()) {
                calendarPath.mkdirs();
            }
        }
        return sCalendarTempPath;
    }

    public static int getIpMessagServiceId(Context context) {
        return IpMessageUtils.getServiceManager(context).getIpMessageServiceId();
    }

    private static boolean sSlot1SimExist = true;
    private static boolean sSlot2SimExist = true;
    private static int sSlot1RetryCounter = 0;
    private static int sSlot2RetryCounter = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private static long sSim1Id = -1;
    private static long sSim2Id = -1;

    private static void loadSimInfo(Context context) {
        /// M: sim1 info maybe not loaded yet, load it.
        if (sSim1Id <= 0 && sSlot1SimExist) {
            sSim1Id = SIMInfo.getIdBySlot(context, EncapsulatedPhone.GEMINI_SIM_1);
            sSlot1RetryCounter++;
            /// M: if we tried 3 times and still can't get valid simId , we think the slot is empty.
            if (sSlot1RetryCounter == MAX_RETRY_COUNT && sSim1Id <= 0) {
                sSlot1SimExist = false;
            }
        }
        /// M: sim2 info maybe not loaded yet, load it.
        if (sSim2Id <= 0 && sSlot2SimExist) {
            sSim2Id = SIMInfo.getIdBySlot(context, EncapsulatedPhone.GEMINI_SIM_2);
            sSlot2RetryCounter++;
            /// M: if we tried 3 times and still can't get valid simId , we think the slot is empty.
            if (sSlot2RetryCounter == MAX_RETRY_COUNT && sSim2Id <= 0) {
                sSlot2SimExist = false;
            }
        }
    }

    public static boolean isActivated(Context context) {
        if (!IpMessageUtils.getServiceManager(context).serviceIsReady()) {
            return false;
        }
        loadSimInfo(context);
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            return (!sSlot1SimExist ? false : isActivated(context, (int)sSim1Id))
                    || (!sSlot2SimExist ? false : isActivated(context, (int)sSim2Id));
        } else {
            boolean actStatus = IpMessageUtils.getServiceManager(context).isActivated();
            MmsLog.d(TAG, "activation status:" + actStatus);
            return actStatus;
        }
    }

    public static boolean isActivated(Context context, int simId) {
        if (!IpMessageUtils.getServiceManager(context).serviceIsReady()) {
            return false;
        }
        boolean actStatus = IpMessageUtils.getServiceManager(context).isActivated(simId);
        MmsLog.d(TAG, "sim " + simId + " activation status:" + actStatus);
        return actStatus;
    }

    public static boolean isServiceEnabled(Context context) {
        loadSimInfo(context);
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            return (!sSlot1SimExist ? false : isServiceEnabled(context, (int)sSim1Id))
                || (!sSlot2SimExist ? false : isServiceEnabled(context, (int)sSim2Id));
        } else {
            boolean actStatus = IpMessageUtils.getServiceManager(context).isEnabled();
            MmsLog.d(TAG, "activation status:" + actStatus);
            return actStatus;
        }
    }

    public static boolean isServiceEnabled(Context context, int simId) {
        return IpMessageUtils.getServiceManager(context).isEnabled(simId);
    }

    public static int getMmsRetryPromptIndex() {
        return mMmsConfigPlugin.getMmsRetryPromptIndex();
    }

    public static int[] getMmsRetryScheme() {
        return mMmsConfigPlugin.getMmsRetryScheme();
    }

    public static void setSoSndTimeout(HttpParams params) {
        mMmsConfigPlugin.setSoSndTimeout(params);
    }

    public static boolean isAllowRetryForPermanentFail() {
        return mMmsConfigPlugin.isAllowRetryForPermanentFail();
    }

    public static boolean isRetainRetryIndexWhenInCall() {
        return mMmsConfigPlugin.isRetainRetryIndexWhenInCall();
    }

    ///M: add for cmcc draft @{
    public static boolean isShowDraftIcon() {
        return mMmsConfigPlugin.isShowDraftIcon();
    }
    /// }@

    public static boolean isSendExpiredResIfNotificationExpired() {
        return mMmsConfigPlugin.isSendExpiredResIfNotificationExpired();
    }

    public static boolean isNeedExitComposerAfterForward() {
        return mMmsConfigPlugin.isNeedExitComposerAfterForward();
    }

    public static Uri appendExtraQueryParameterForConversationDeleteAll(Uri uri) {
        return mMmsConfigPlugin.appendExtraQueryParameterForConversationDeleteAll(uri);
    }

    /// M: Print Mms memory usage @ {
    /**
     * Print Mms memory usage
     */
    public static void printMmsMemStat(Context context, String callerTag) {
        mMmsConfigPlugin.printMmsMemStat(context, callerTag);
    }
    /// @}

    /// M: ALPS00527989, Extend TextView URL handling @ {
    public static void setExtendUrlSpan(TextView textView) {
        mMmsConfigPlugin.setExtendUrlSpan(textView);
    }
    /// @}
}
