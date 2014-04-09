/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.ext;

/// M: ALPS00440523, Print Mms memory usage @ {
import android.content.Context;
/// @}
import android.content.Intent;
import android.net.Uri;
/// M: ALPS00527989, Extend TextView URL handling @ {
import android.widget.TextView;
/// @}

import com.mediatek.encapsulation.MmsLog;

import org.apache.http.params.HttpParams;

public class MmsConfigImpl implements IMmsConfig {
    private static final String TAG = "Mms/MmsConfigImpl";
    
    private static int sSmsToMmsTextThreshold = 4;
    private static int sMaxTextLimit = 2048;
    private static int sMmsRecipientLimit = 20;                 // default value
/// M:Code analyze 01,For new feature CMCC_Mms in ALPS00325381, MMS easy porting check in JB @{   
    private static int sHttpSocketTimeout = 60*1000;
/// @}
    /// M: For common case, default retry scheme not change
    private static final int[] DEFAULTRETRYSCHEME = {
        0, 1 * 60 * 1000, 5 * 60 * 1000, 10 * 60 * 1000, 30 * 60 * 1000};
    /// @}

    public int getSmsToMmsTextThreshold() {
        MmsLog.d(TAG, "get SmsToMmsTextThreshold: " + sSmsToMmsTextThreshold);
        return sSmsToMmsTextThreshold;
    }

    public void setSmsToMmsTextThreshold(int value) {
        if (value > -1) {
            sSmsToMmsTextThreshold = value;
        }
        MmsLog.d(TAG, "set SmsToMmsTextThreshold: " + sSmsToMmsTextThreshold);
    }

    public int getMaxTextLimit() {
        MmsLog.d(TAG, "get MaxTextLimit: " + sMaxTextLimit);
        return sMaxTextLimit;
    }

    public void setMaxTextLimit(int value) {
        if (value > -1) {
            sMaxTextLimit = value;
        } 
        
        MmsLog.d(TAG, "set MaxTextLimit: " + sMaxTextLimit);
    }
    
    public int getMmsRecipientLimit() {
        MmsLog.d(TAG, "RecipientLimit: " + sMmsRecipientLimit);
        return sMmsRecipientLimit;
    }
    
    public void setMmsRecipientLimit(int value) {
        if (value > -1) {
            sMmsRecipientLimit = value;
        } 

        MmsLog.d(TAG, "set RecipientLimit: " + sMmsRecipientLimit);
    }
    
    public String getUAProf(final String defaultUAP) {
        MmsLog.d(TAG, "set default UAProf is: " + defaultUAP);
        return defaultUAP;
    }
/// M:Code analyze 02,For new feature CMCC_Mms in ALPS00325381, MMS easy porting check in JB @{
    public int getHttpSocketTimeout() {
        MmsLog.d(TAG, "get default socket timeout: " + sHttpSocketTimeout);
        return sHttpSocketTimeout;
    }

    public void setHttpSocketTimeout(int socketTimeout) {
        MmsLog.d(TAG, "set default socket timeout: " + socketTimeout);
        sHttpSocketTimeout = socketTimeout;
    }
/// @}
    public boolean isEnableSmsEncodingType() {
        MmsLog.d(TAG, "Disable Sms Encoding type ");
        return false;
    }

    public boolean isEnableReportAllowed() {
        MmsLog.d(TAG, "Disable ReportAllowed ");
        return false;
    }

    public boolean isEnableMultiSmsSaveLocation() {
        MmsLog.d(TAG, "Disable MultiSmsSaveLocation ");
        return true;
    }

    public boolean isEnableStorageFullToast() {
        MmsLog.d(TAG, "Disable StorageFullToast ");
        return false;
    }

    public boolean isEnableFolderMode() {
        MmsLog.d(TAG, "Disable FolderMode ");
        return false;
    }

    public boolean isEnableForwardWithSender() {
        MmsLog.d(TAG, "Disable ForwardWithSender ");
        return false;
    }

    public boolean isEnableDialogForUrl() {
        MmsLog.d(TAG, "Disable ForwardWithSender ");
        return false;
    }

    public boolean isEnableStorageStatusDisp() {
        MmsLog.d(TAG, "Disable display storage status ");
        return false;
    }

    public boolean isEnableSIMSmsForSetting() {
        MmsLog.d(TAG, "Enable display storage status ");
        return true;
    }
/// M:Code analyze 03,For new feature CMCC_Mms in ALPS00325381, MMS easy porting check in JB @{
    public boolean isEnableSIMLongSmsConcatenate() {
        MmsLog.d(TAG, "Enable concatenate long sms in sim card status");
        return true;
    }

    public Intent getCapturePictureIntent() {
        MmsLog.d(TAG, "get capture picture intent: null");
        return null;
    }

    public boolean isShowUrlDialog() {
        MmsLog.d(TAG, "Enable show dialog when open browser: " + false);
        return false;
    }
    
    public boolean isEnableAdjustFontSize() {
        MmsLog.d(TAG, "Enable adjust font size");
        return true;
    }

    public boolean isEnableSmsValidityPeriod() {
        MmsLog.d(TAG, "Enable sms validity period");
        return false;
    }
/// @}

    public int getMmsRetryPromptIndex() {
        MmsLog.d(TAG, "getMmsRetryPromptIndex");
        return 1;
    }

    public int[] getMmsRetryScheme() {
        MmsLog.d(TAG, "getMmsRetryScheme");
        return DEFAULTRETRYSCHEME;
    }

    public void setSoSndTimeout(HttpParams params) {
        MmsLog.d(TAG, "setSoSndTimeout");
        return;
    }

    public boolean isAllowRetryForPermanentFail() {
        MmsLog.d(TAG, "setSoSndTimeout");
        return false;
    }

    public boolean isRetainRetryIndexWhenInCall() {
        MmsLog.d(TAG, "isIncreaseRetryIndexWhenInCall: " + false);
        return false;
    }

    public boolean isShowDraftIcon() {
        MmsLog.d(TAG, "isShowDraftIcon: " + false);
        return false;
    }

    public boolean isSendExpiredResIfNotificationExpired() {
        MmsLog.d(TAG, "isSendExpiredResIfNotificationExpired: " + true);
        return true;
    }

    public boolean isNeedExitComposerAfterForward() {
        MmsLog.d(TAG, "isNeedExitComposerAfterForward: " + true);
        return true;
    }

    public Uri appendExtraQueryParameterForConversationDeleteAll(Uri uri) {
        MmsLog.d(TAG, "appendExtraQueryParameterForConversationDeleteAll; null ");
        return uri;
    }

    /// M: ALPS00440523, Print Mms memory usage @ {
    /**
     * Print Mms memory usage
     */
    public void printMmsMemStat(Context context, String callerTag) {
        MmsLog.d(TAG, "printMmsMemStat");
    }
    /// @}

    /// M: ALPS00527989, Extend TextView URL handling @ {
    public void setExtendUrlSpan(TextView textView) {
        MmsLog.d(TAG, "setExtendUrlSpan");
    }
    /// @}
}

