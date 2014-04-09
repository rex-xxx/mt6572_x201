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
/// M: ALPS00527989 Extend TextView URL handling @ {
import android.widget.TextView;
/// @}

import org.apache.http.params.HttpParams;

public interface IMmsConfig {
    /**
     * Returns the text length threshold of change to mms from sms.
     * 
     * @return              the text length threshold of change to mms from sms.
     */
    int getSmsToMmsTextThreshold();

    /**
     * set the text length threshold of change to mms from sms according to the configuration file.
     
     * @param value         the threshold value to be set
     * 
     */
    void setSmsToMmsTextThreshold(int value);

    /**
     * Returns the max length of mms or sms text content.
     * 
     * @return              the max length of mms or sms text content.
     */
    int getMaxTextLimit();

    /**
     * Set the max length of mms or sms text content.
     *
     * @param value         the max length of mms or sms text content to be set
     * 
     */
    void setMaxTextLimit(int value);

    /**
     * Returns the max count of mms recipient.
     * 
     * @return              the max count of mms recipient.
     */
    int getMmsRecipientLimit();

    /**
     * Set the max count of mms recipient.
     *
     * @param value         the max count of mms recipient to be set
     * 
     */
    void setMmsRecipientLimit(int value);

    /**
     * Returns the max length of message recipient.
     * 
     * @param defaultUAP    the default UA Profile
     * @return              the max length of message recipient.
     */
    String getUAProf(final String defaultUAP);

    /// M:Code analyze 01,For new feature CMCC_Mms in ALPS00325381, MMS easy
    /// porting check in JB @{
    /**
     * Returns the socket timeout.
     * 
     * @return              the value of socket timeout in ms.
     */
    int getHttpSocketTimeout();

    /**
     * Set the socket timeout.
     * 
     * @param socketTimeout    the value of socket timeout in ms.
     */
    void setHttpSocketTimeout(int socketTimeout);

    /// @}

    /**
     * Returns the sms encoding preferred type. Defaultly, should return
     * SmsMessage.ENCODING_UNKNOWN.
     * 
     * @return whether enable the setting for sms encoding type.
     */
    boolean isEnableSmsEncodingType();

    /**
     * Returns whether enable the setting for Delivery Report Allowed.
     * 
     * @return              whether enable the setting for Delivery Report Allowed.
     */
    boolean isEnableReportAllowed();

    /**
     * Returns whether enable seperated sms save location at different SIM.
     * 
     * @return              whether enable seperated sms save location at different SIM.
     */
    boolean isEnableMultiSmsSaveLocation();

    /**
     * Returns whether prompt storage full when download mms.
     * 
     * @return              whether prompt storage full when download mms.
     */
    boolean isEnableStorageFullToast();

    /**
     * Returns whether enable folder view mode
     * 
     * @return              whether enable folder view mode.
     */
    boolean isEnableFolderMode();

    /**
     * Returns whether enable the feature - forward sms with original sender
     * 
     * @return              whether enable the feature - forward sms with original sender.
     */
    boolean isEnableForwardWithSender();

    /**
     * Returns whether prompt dialog before visit a clicked Url  
     * 
     * @return              whether prompt dialog before visit a clicked Url.
     */
    boolean isEnableDialogForUrl();

    /**
     * Returns whether show the message storage information at setting
     * 
     * @return              whether show the message storage information at setting.
     */
    boolean isEnableStorageStatusDisp();

    /**
     * Returns whether show SIM SMS at setting
     * 
     * @return              whether show SIM SMS at setting.
     */
    boolean isEnableSIMSmsForSetting();

    /**
     * Returns whether auto concatenate long sms in SIM card.
     * @return              whether auto concatenate long sms in SIM card. 
     */
    boolean isEnableSIMLongSmsConcatenate();

    /// M:Code analyze 02,For new feature CMCC_Mms in ALPS00325381, MMS easy
    /// porting check in JB @
    /**
     * Returns the intent of capture picture
     * 
     * @return the intent of capture picture.
     */

    Intent getCapturePictureIntent();

    /**
     * Returns whether show dialog when open url from browser
     * 
     * @return              whether show dialog when open url from browser
     */
    boolean isShowUrlDialog();
    
    /**
     * Returns whether enable the feature - font size setting
     * 
     * @return              whether enable the feature - font size setting
     */
    boolean isEnableAdjustFontSize();

    /**
     * Returns whether enable the feature - sms validity period
     * 
     * @return              whether enable the feature - sms validity period
     */
    boolean isEnableSmsValidityPeriod();
    /// @}
    /// M: Add for CMCC FT request to set send/receive related parameters
    /**
     * Returns which time of MMS transaction retry need prompt failure
     *
     * @return              Index of retry to show prompt
     */
    int getMmsRetryPromptIndex();

    /**
     * Returns Mms transaction retry scheme
     *
     * @return              Retry scheme array
     */
    int[] getMmsRetryScheme();

    /**
     * Returns Mms socket send timeout
     *
     * @return
     */
    void setSoSndTimeout(HttpParams params);

    /**
     * Returns is it allowed to retry for some permanent transaction fail.
     *
     * @return              true if allow, otherwise return false
     */
    boolean isAllowRetryForPermanentFail();

    /**
     * Returns is it retain retry Index when fail because incall going
     *
     * @return              true if retain, otherwise return false
     */
    boolean isRetainRetryIndexWhenInCall();

    /**
     * Returns is it show draft icon when conversation has draft
     *
     * @return              true if show, otherwise return false
     */
    boolean isShowDraftIcon();

    /**
     * Returns is it to send expired MM1_Notification_Res expired if
     * the Notification expired.
     *
     * @return              true if send, otherwise return false
     */
    boolean isSendExpiredResIfNotificationExpired();
    /// @}

    /**
     * Returns  is it need to exit composer after forward message
     *
     * @return              true if exit, otherwise return false
     */
    boolean isNeedExitComposerAfterForward();

    /**
     * in Convesation.startDeleteAll, append extra query parameter
     */
    Uri appendExtraQueryParameterForConversationDeleteAll(Uri uri);

    /// M: ALPS00440523, Print Mms memory usage @ {
    /**
     * Print Mms memory usage
     */
    void printMmsMemStat(Context context, String callerTag);
    /// @}

    /// M: ALPS00527989, Extend TextView URL handling @ {
    /**
     * Sets ExtendURLSpan for extended URL click handling
     *
     */
    void setExtendUrlSpan(TextView textView);
    /// @}
}

