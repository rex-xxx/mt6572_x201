/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.provider.Telephony.Mms;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.MmsMessageSender;
import com.android.mms.transaction.SmsMessageSender;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.MmsException;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.telephony.EncapsulatedTelephonyManagerEx;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ipmessage.message.IpAttachMessage;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.IpMessageConsts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * M:
 *
 */
public class MultiForwardMessageActivity extends Activity {

    private static final String TAG = "Mms/MultiForwardMessage";
    public static final String MULTI_FORWARD_PARAM_NUMBERS = "MultiForwardMessageParamNumbers";
    public static final String MULTI_FORWARD_PARAM_MESSAGEIDS = "MultiForwardMessageParamMessageIds";
    public static final String MULTI_FORWARD_PARAM_THREADID = "MultiForwardMessageParamThreadId";

    public static final String MULTI_FORWARD_ACTION = "com.android.mms.ui.MultiForwardMessageActivity";

    private static final String MMS = "mms";
    private static final String SMS = "sms";
    private static final String SELECT_TYPE             = "Select_type";
    private static final String EXIT_ECM_RESULT         = "exit_ecm_result";
    private static final String PROCESSING_FORWARD      = "forwardingMsg";

    private static final int SIM_SELECT_FOR_SEND_MSG                  = 1;
    private static final int SIM_SELECT_FOR_SAVE_MSG_TO_SIM           = 2;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM = 104;
    private static final int REQUEST_CODE_ECM_EXIT_DIALOG             = 107;

    private List<SIMInfo> mSimInfoList = null;
    private int mAssociatedSimId = 0;
    private long mMessageSimId = 0;
    private int mSimCount;
    private boolean mForwardingMsg = false;
    private int mAllCount = 0;
    private int mSentCount = 0;
    private int mFailedCount = 0;
    private int mSelectedSimId = 0;
    private String mNumbers = "";
    private String mMsgIds = "";
    private MmsColumnsMap mMmsColumnsMap = new MmsColumnsMap();
    private SmsColumnsMap mSmsColumnsMap = new SmsColumnsMap();

    private static CellConnMgr mCellMgr = null;

    private static final int INCREASE_PROCESS = 1;
    private static final int SHOW_RESULT = 2;
    private static final int SHOW_ERROR = 3;

    private static final String ALL_COUNT = "AllCount";
    private static final String SENT_COUNT = "SentCount";
    private static final String FAILED_COUNT = "FailedCount";

    private static final String ERROR_DIALOG_TITLE = "ErrorDialogTitle";
    private static final String ERROR_DIALOG_MSG = "ErrorDialogMsg";

    private ProgressBar mProgressBar;
    private TextView mProgressBarText;

    private int mUnDownloadedIpMessageCounter = 0;

    private static final String[] SMS_PROJECTION = new String[] {
        BaseColumns._ID,
        Sms.BODY,
        Sms.IPMSG_ID,
    };

    private static class SmsColumnsMap {
        public int mColumnMsgId;
        public int mColumnSmsBody;
        public int mColumnSmsIpMsgId;

        public SmsColumnsMap() {
            mColumnMsgId        = 0;
            mColumnSmsBody      = 1;
            mColumnSmsIpMsgId   = 2;
        }
    }

    private static final String[] MMS_PROJECTION = new String[] {
        BaseColumns._ID,
        Conversations.THREAD_ID,
        Mms.SUBJECT,
        Mms.SUBJECT_CHARSET,
        Mms.DATE,
        Mms.DATE_SENT,
        Mms.READ,
        Mms.MESSAGE_TYPE,
        Mms.MESSAGE_BOX,
        Mms.DELIVERY_REPORT,
        Mms.READ_REPORT,
        Mms.LOCKED,
        Mms.SIM_ID,
        /// M: fix bug ALPS00406912
        Mms.SERVICE_CENTER,
        Mms.STATUS
    };

    private static class MmsColumnsMap {

        public int mColumnMsgId;
        public int mColumnThreadId;
        public int mColumnMmsSubject;
        public int mColumnMmsSubjectCharset;
        public int mColumnMmsDate;
        public int mColumnMmsDateSent;
        public int mColumnMmsRead;
        public int mColumnMmsMessageType;
        public int mColumnMmsMessageBox;
        public int mColumnMmsDeliveryReport;
        public int mColumnMmsReadReport;
        public int mColumnMmsLocked;
        public int mColumnMmsSimId;
        public int mColumnMmsServiceCenter;
        /// M: fix bug ALPS00406912
        public int mColumnMmsStatus;

        public MmsColumnsMap() {
            mColumnMsgId              = 0;
            mColumnThreadId           = 1;
            mColumnMmsSubject         = 2;
            mColumnMmsSubjectCharset  = 3;
            mColumnMmsDate            = 4;
            mColumnMmsRead            = 6;
            mColumnMmsMessageType     = 7;
            mColumnMmsMessageBox      = 8;
            mColumnMmsDeliveryReport  = 9;
            mColumnMmsReadReport      = 10;
            mColumnMmsLocked          = 11;
            mColumnMmsSimId           = 12;
            mColumnMmsServiceCenter   = 13;
            /// M: fix bug ALPS00406912
            mColumnMmsStatus          = 14;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.multi_forward_msg);

       initProgressDialog();

        if (savedInstanceState != null) {
            mForwardingMsg = savedInstanceState.getBoolean(PROCESSING_FORWARD, false);
        }
        if (mCellMgr == null) {
            mCellMgr = new CellConnMgr();
        }
        mCellMgr.register(getApplication());
    }

    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
    private void initProgressDialog() {
        if (mProgressBar == null) {
            String stepForward = IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_dailog_multi_forward);
            mProgressBar = (ProgressBar) findViewById(R.id.progress);
            mProgressBarText = (TextView) findViewById(R.id.progress_percent);
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBarText.setVisibility(View.VISIBLE);
            mProgressBarText.setText(stepForward);
            mProgressBar.setMax(100);
            mProgressBar.setIndeterminate(true);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            default:
                break;
        }
        return false;
    }

    protected void onResume() {
        super.onResume();
        if (!mForwardingMsg) {
            mForwardingMsg = true;
            Intent intent = this.getIntent();
            mMsgIds = intent.getStringExtra(MULTI_FORWARD_PARAM_MESSAGEIDS);
            mNumbers = intent.getStringExtra(MULTI_FORWARD_PARAM_NUMBERS);
            MmsLog.d(TAG, "MessageIds:" + mMsgIds + " Numbers:" + mNumbers);
            checkSimInfoAndSendAsync();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mCellMgr != null) {
            mCellMgr.unregister();
            mCellMgr = null;
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mForwardingMsg) {
            outState.putBoolean(PROCESSING_FORWARD, true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ECM_EXIT_DIALOG) {
            finishSelf(RESULT_CANCELED);
        }
    }

    private void finishSelf(int resultOK) {
        setResult(resultOK);
        if (mUnDownloadedIpMessageCounter > 0) {
            Toast.makeText(this,
                    IpMessageUtils.getResourceManager(this)
                            .getSingleString(IpMessageConsts.string.ipmsg_multi_forward_failed_part),
                    Toast.LENGTH_SHORT).show();
        }
        MultiForwardMessageActivity.this.finish();
    }

    /**
     *M:
     *
     */
    private void checkSimInfoAndSendAsync() {
        String stepOne = IpMessageUtils.getResourceManager(this)
        .getSingleString(IpMessageConsts.string.ipmsg_multi_forward_sim_info);
        String stepForward = IpMessageUtils.getResourceManager(this)
            .getSingleString(IpMessageConsts.string.ipmsg_dailog_multi_forward);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mForwardingMsg = true;
                getSimInfoList();
                simSelection(mNumbers, mMsgIds);
            }
        }).start();
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case INCREASE_PROCESS:
                    mProgressBar.setProgress(msg.arg1);
                    break;
                case SHOW_RESULT:
                    /// M: do not show result, just exit.
                    finishSelf(RESULT_OK);
                    //Bundle data = msg.getData();
                    //showResultDialog(data.getInt(ALL_COUNT), data.getInt(FAILED_COUNT), data.getInt(SENT_COUNT));
                    break;
                case SHOW_ERROR:
                    Bundle errorData = msg.getData();
                    String title = errorData.getString(ERROR_DIALOG_TITLE);
                    String dlgMsg = errorData.getString(ERROR_DIALOG_MSG);
                    showErrorDialog(title, dlgMsg);
                    break;
                default:
                    break;
            }
        }
    };

    private void sendMsgToHandler(int target, int title, int message) {
        Message msg = new Message();
        msg.what = target;
        String titleStr = IpMessageUtils.getResourceManager(this).getSingleString(title);
        String messageStr = IpMessageUtils.getResourceManager(this).getSingleString(message);
        Bundle data = new Bundle();
        data.putString(ERROR_DIALOG_TITLE, titleStr);
        data.putString(ERROR_DIALOG_MSG, messageStr);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    private void forwardDirectly(final boolean checkECM) {
        if (checkECM) {
            // TODO: expose this in telephony layer for SDK build
            String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE);
            if (Boolean.parseBoolean(inEcm)) {
                try {
                    startActivityForResult(new Intent(
                            TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_ECM_EXIT_DIALOG);
                } catch (ActivityNotFoundException e) {
                    // continue to send message
                    Log.e(TAG, "Cannot find EmergencyCallbackModeExitDialog", e);
                    finishSelf(RESULT_CANCELED);
                    return;
                }
            } else {
                doForward(mNumbers, mMsgIds);
            }
        } else {
            doForward(mNumbers, mMsgIds);
        }
    }

    /**
     *  M:
     * @param bCheckEcmMode
     */
    private void checkEcmMode(boolean bCheckEcmMode) {
        mForwardingMsg = true;
        int requestType = CellConnMgr.REQUEST_TYPE_SIMLOCK;
        final Object waitObject = new Object();
        final int slotId;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            requestType = CellConnMgr.REQUEST_TYPE_ROAMING;
            slotId = SIMInfo.getSlotById(this, mSelectedSimId);
            MmsLog.d(MmsApp.TXN_TAG, "check pin and...: simId=" + mSelectedSimId + "\t slotId="
                + slotId);
        } else {
            slotId = 0;
        }
        final boolean bCEM = bCheckEcmMode;
        mCellMgr.handleCellConn(slotId, requestType, new Runnable() {
            public void run() {
                int nRet = mCellMgr.getResult();
                MmsLog.d(MmsApp.TXN_TAG, "serviceComplete result = "
                    + CellConnMgr.resultToString(nRet));
                if (mCellMgr.RESULT_ABORT == nRet || mCellMgr.RESULT_OK == nRet) {
                    MmsLog.e(MmsApp.TXN_TAG, "send failed. check CellConn failed");
                    sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                        IpMessageConsts.string.ipmsg_sim_status_error);
                    return;
                }
                if (slotId != mCellMgr.getPreferSlot()) {
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(MultiForwardMessageActivity.this,
                        mCellMgr.getPreferSlot());
                    if (si == null) {
                        MmsLog.e(MmsApp.TXN_TAG, "serviceComplete siminfo is null");
                        sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                            IpMessageConsts.string.ipmsg_multi_forward_no_sim);
                        return;
                    }
                    mSelectedSimId = (int) si.getSimId();
                }
                forwardDirectly(bCEM);
            }
        });
    }

    /**
     * M:
     * @param numbers
     * @return
     */
    private String[] splitNumbers(String numbers) {
        if (numbers == null || numbers.trim().equals("")) {
            return null;
        }
        numbers = numbers.replaceAll(",", ";");
        return numbers.split(";");
    }

    /**
     *
     * @param numbers
     * @param ids
     * @return
     */
    private boolean simSelection(String numbers, String ids) {
        MmsLog.d(TAG, "simSelection begin");
        int simId = 0;
        MmsLog.d(TAG, "simSelection begin. mSelectedSimId:" + mSelectedSimId);
        if (mSimInfoList == null || mSimInfoList.size() < 1) {
            sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                IpMessageConsts.string.ipmsg_multi_forward_no_sim);
            return false;
        } else if (mSimInfoList.size() == 1) {
            mSelectedSimId = (int) mSimInfoList.get(0).getSimId();
            checkEcmMode(true);
            return true;
        }
        if (!EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            checkEcmMode(true);
            return true;
        }
        if (mSelectedSimId >= 1) {
            checkEcmMode(true);
            return true;
        }
        if (mSimInfoList.size() > 1) {
            Intent intent = new Intent();
            intent.putExtra(SELECT_TYPE, SIM_SELECT_FOR_SEND_MSG);
            String[] numberArray = splitNumbers(numbers);
            // getContactSIM
            if (numberArray.length == 1) {
                mAssociatedSimId = MultiForwardUtils.getContactSIM(
                    MultiForwardMessageActivity.this, numberArray[0]); // 152188888888 is a contact number
            } else {
                mAssociatedSimId = -1;
            }
            MmsLog.d(TAG, "mAssociatedSimId = " + mAssociatedSimId);
            mMessageSimId = Settings.System.getLong(getContentResolver(),
                Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            if (mMessageSimId == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                // always ask, show SIM selection dialog
                showSimSelectedDialog(intent, numbers, ids);
            } else if (mMessageSimId == Settings.System.DEFAULT_SIM_NOT_SET) {
                /*
                 * not set default SIM: if recipients are morn than 2,or there is no associated SIM, show SIM selection
                 * dialog else send message via associated SIM
                 */
                if (mAssociatedSimId == -1) {
                    showSimSelectedDialog(intent, numbers, ids);
                } else {
                    mSelectedSimId = mAssociatedSimId;
                    checkEcmMode(true);
                }
            } else {
                /*
                 * default SIM: if recipients are morn than 2,or there is no associated SIM, send message via default
                 * SIM else show SIM selection dialog
                 */
                if (mAssociatedSimId == -1 || (mMessageSimId == mAssociatedSimId)) {
                    mSelectedSimId = (int) mMessageSimId;
                    checkEcmMode(true);
                } else {
                    showSimSelectedDialog(intent, numbers, ids);
                }
            }
        } else {
            sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                IpMessageConsts.string.ipmsg_getsim_failed);
            return false;
        }
        MmsLog.d(TAG,"simSelection end");
        return true;
    }

    /**
     *
     * @param numbers
     * @param ids
     */
    private void doForward(String numbers, String ids) {
        MmsLog.d(TAG,"doForward begin");
        if (numbers == null || numbers.trim().equals("")) {
            MmsLog.e(TAG, "multiForard failed for empty numbers");
            sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                IpMessageConsts.string.ipmsg_forward_norecipients);
            return;
        }
        if (ids == null || ids.trim().equals("")) {
            MmsLog.e(TAG, "multiForard failed for empty message ids");
            sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                IpMessageConsts.string.ipmsg_multiforward_no_message);
            return;
        }
        numbers = numbers.replaceAll(",", ";");
        final String[] numbersArray = numbers.split(";");
        if (numbersArray == null || numbersArray.length < 1) {
            MmsLog.e(TAG, "multiForard failed for empty numbers");
            sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                IpMessageConsts.string.ipmsg_forward_norecipients);
            return;
        }
        ids = ids.replaceAll(";", ",");
        final String[] messageIdsArray = ids.split(",");
        if (messageIdsArray == null || messageIdsArray.length < 1) {
            MmsLog.e(TAG, "multiForard failed for empty message ids");
            sendMsgToHandler(SHOW_ERROR, IpMessageConsts.string.ipmsg_forward_failed,
                IpMessageConsts.string.ipmsg_multiforward_no_message);
            return;
        }
        MmsLog.d(TAG, "Forward Begin; simId:" + mSelectedSimId + " msgIds:" + ids);
        final String finalNumbers = numbers;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Conversation conversation = Conversation.createNew(MultiForwardMessageActivity.this);
                ContactList contactList = ContactList.getByNumbers(finalNumbers, false, false);
                conversation.setRecipients(contactList);
                long threadId = conversation.ensureThreadId();
                int smsSendSuccessCount = 0;
                int smsSendFailedCount = 0;
                int mmsSendSuccessCount = 0;
                int mmsSendFailedCount = 0;
                int ipMsgSendSuccessCount = 0;
                int ipMsgSendFailedCount = 0;
                mAllCount = messageIdsArray.length;
                int sendCount = 0;
                for (String id : messageIdsArray) {
                    sendCount ++;
                    if (id.startsWith("-")) {
                        /// M: the message is a mms.
                        Cursor mmsCursor = MultiForwardMessageActivity.this.getContentResolver().query(Mms.CONTENT_URI, MMS_PROJECTION,
                            "_id = " + id.substring(1, id.length()), null, null);
                        try {
                            if (mmsCursor != null && mmsCursor.moveToFirst()) {
                                MessageItem messageItem = getMessageItemFromCursor(MultiForwardMessageActivity.this, mmsCursor, MMS);
                                if (messageItem != null) {
                                    if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == messageItem.mMessageType) {
                                        /// M: mms notification can not be forwarded.
                                        continue;
                                    }
                                    if ((messageItem.mIpMessage != null) && (messageItem.mIpMessage instanceof IpAttachMessage)) {
                                        IpAttachMessage ipAttachMessage = (IpAttachMessage)messageItem.mIpMessage;
                                        if (ipAttachMessage.isInboxMsgDownloalable()) {
                                            /// M: undownloaded ipmessage can not be forwarded.
                                            MmsLog.d(TAG, "get an undownloaded ipmessage");
                                            mUnDownloadedIpMessageCounter++;
                                            continue;
                                        }
                                    }
                                    if (forwardMms(messageItem, numbersArray, mSelectedSimId, threadId)) {
                                        mmsSendSuccessCount++;
                                    } else {
                                        mmsSendFailedCount++;
                                    }
                                } else {
                                    MmsLog.d(TAG, "forward MMs getItem: item is null");
                                }
                            } else {
                                MmsLog.d(TAG, "forward MMs gmmsCursor.moveToFirst failed :"
                                    + mmsCursor.getCount());
                            }
                        } finally {
                            if (mmsCursor != null) {
                                mmsCursor.close();
                            }
                        }
                    } else {
                        Cursor smsCursor = MultiForwardMessageActivity.this.getContentResolver().query(Sms.CONTENT_URI, SMS_PROJECTION,
                            "_id = " + id, null, null);
                        try {
                            MmsLog.d(TAG,"forward IpMessage begin");
                            if (smsCursor != null) {
                                if (smsCursor.moveToFirst()) {
                                    String smsBody = smsCursor.getString(mSmsColumnsMap.mColumnSmsBody);
                                    int msgId = smsCursor.getInt(mSmsColumnsMap.mColumnMsgId);
                                    int ipMsgId = smsCursor.getInt(mSmsColumnsMap.mColumnSmsIpMsgId);

                                    if (ipMsgId > 0) {
                                        boolean usersIsIpMessage = isRecipientIpMessageUser(numbersArray);
                                        boolean simEnableIpService = MmsConfig.isServiceEnabled(MultiForwardMessageActivity.this, mSelectedSimId);
                                        if (usersIsIpMessage && simEnableIpService) {
                                            // TODO: forward ipmessage
                                            IpMessage im = IpMessageUtils.getMessageManager(MultiForwardMessageActivity.this).getIpMsgInfo(msgId);
                                            im.setTo(finalNumbers.replaceAll(";",","));
                                            im.setSimId(mSelectedSimId);
                                            int result = IpMessageUtils.getMessageManager(MultiForwardMessageActivity.this).saveIpMsg(im, mSelectedSimId);
                                            if (result > 0) {
                                                ipMsgSendSuccessCount++;
                                            } else {
                                                ipMsgSendFailedCount++;
                                            }
                                        } else {
                                            IpMessage ipMessage =
                                                IpMessageUtils.getMessageManager(MultiForwardMessageActivity.this).getIpMsgInfo(msgId);
                                            MultiForwardUtils mfu = new MultiForwardUtils(MultiForwardMessageActivity.this);
                                            WorkingMessage wm = mfu.convertIpMsgToSmsOrMms(ipMessage,
                                                                    MultiForwardMessageActivity.this,
                                                                    conversation);
                                            if (wm != null) {
                                                if (wm.requiresMms()) {
                                                    if (sendMms(wm, threadId, mSelectedSimId)) {
                                                        ipMsgSendSuccessCount++;
                                                    } else {
                                                        ipMsgSendFailedCount++;
                                                    }
                                                } else {
                                                    if (forwardSms(wm.getText().toString(), numbersArray,
                                                        mSelectedSimId, threadId)) {
                                                        ipMsgSendSuccessCount++;
                                                    } else {
                                                        ipMsgSendFailedCount++;
                                                    }
                                                }
                                            } else {
                                                mUnDownloadedIpMessageCounter++;
                                            }
                                        }
                                    } else {
                                        if (forwardSms(smsBody, numbersArray, mSelectedSimId, threadId)) {
                                            smsSendSuccessCount++;
                                        } else {
                                            smsSendFailedCount++;
                                        }
                                    }
                                } else {
                                    smsSendFailedCount++;
                                }
                                MmsLog.d(TAG,"forward IpMessage end");
                            } else {
                                smsSendFailedCount++;
                            }
                        } finally {
                            if (smsCursor != null) {
                                smsCursor.close();
                            }
                        }
                    }
                }
                MmsLog.d(TAG,"doForward end");
                Message msg = new Message();
                msg.what = SHOW_RESULT;
                mHandler.sendMessage(msg);
            }
        }, "ForwardThread").start();
        /** M: do not show result dialog, just exit.
        mSentCount = mmsSendSuccessCount + smsSendSuccessCount + ipMsgSendSuccessCount;
        mFailedCount = mmsSendFailedCount + smsSendFailedCount + ipMsgSendFailedCount;
        Message msg = new Message();
        msg.what = SHOW_RESULT;
        Bundle data = new Bundle();
        data.putInt(ALL_COUNT, mAllCount);
        data.putInt(SENT_COUNT, mSentCount);
        data.putInt(FAILED_COUNT, mFailedCount);
        msg.setData(data);
        mHandler.sendMessage(msg);
        */
    }

    private void showResultDialog(int allCount, int failedCount, int successCount) {
        MmsLog.d(TAG,"showResultDialog begin");
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this,
                R.style.MmsHoloTheme));
        String allStr = String.format(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_dlg_send_all), allCount);
        String sentStr = String.format(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_dlg_send_sucess), successCount);
        String failedStr = String.format(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_dlg_send_failed), failedCount);
        builder.setTitle(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_dialog_send_result))
            .setMessage(allStr + "\n" + sentStr + "\n" + failedStr)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishSelf(RESULT_OK);
                    }
                }
            );
        AlertDialog alDialog = builder.create();
        alDialog.setCanceledOnTouchOutside(false);
        alDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        finishSelf(RESULT_OK);
                        return true;
                    case KeyEvent.KEYCODE_SEARCH:
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        alDialog.show();
        MmsLog.d(TAG,"showResultDialog end");
    }

    private void showErrorDialog(String title,String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this,R.style.MmsHoloTheme));
        builder.setTitle(title)
            .setMessage(message)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               finishSelf(RESULT_CANCELED);
            }});
        AlertDialog alDialog = builder.create();
        alDialog.setCanceledOnTouchOutside(false);
        alDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        finishSelf(RESULT_OK);
                        return true;
                    case KeyEvent.KEYCODE_SEARCH:
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        alDialog.show();
    }

    private MessageItem getMessageItemFromCursor(Context context, Cursor c,String type) {
        if (c != null && isCursorValid(c)) {
            boolean isDrawTimeDivider = false;
            boolean isDrawUnreadDivider = false;
            boolean isDrawLoadAllMessagesButton = false;
            int unreadCount = 0;
            if (type.equals(MMS)) {
                int boxId = c.getInt(mMmsColumnsMap.mColumnMmsMessageBox);
                int messageType = c.getInt(mMmsColumnsMap.mColumnMmsMessageType);
                int simId = EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT ? c
                        .getInt(mMmsColumnsMap.mColumnMmsSimId) : -1;
                int errorType = 0;
                int locked = c.getInt(mMmsColumnsMap.mColumnMmsLocked);
                int charset = c.getInt(mMmsColumnsMap.mColumnMmsSubjectCharset);
                long mMsgId = c.getLong(mMmsColumnsMap.mColumnMsgId);
                String mmsType = type;
                String subject = c.getString(mMmsColumnsMap.mColumnMmsSubject);
                String serviceCenter = c.getString(mMmsColumnsMap.mColumnMmsServiceCenter);
                String deliveryReport = c.getString(mMmsColumnsMap.mColumnMmsDeliveryReport);
                String readReport = c.getString(mMmsColumnsMap.mColumnMmsReadReport);
                /// M: fix bug ALPS00406912
                int mmsStatus = c.getInt(mMmsColumnsMap.mColumnMmsStatus);
                Pattern highlight = null;
                long indDate = c.getLong(mMmsColumnsMap.mColumnMmsDate);
                try {
                    MessageItem item = new MessageItem(context, boxId, messageType, simId,
                            errorType, locked, charset, mMsgId, mmsType, subject, serviceCenter,
                            deliveryReport, readReport, highlight, isDrawTimeDivider,
                            isDrawUnreadDivider, unreadCount, isDrawLoadAllMessagesButton,indDate, mmsStatus);
                    return item;
                } catch (MmsException e) {
                    Log.e(TAG, "getMessageItemFromCursor failed: ", e);
                }
            }
        }
        return null;
    }

    private boolean isCursorValid(Cursor cursor) {
        // Check whether the cursor is valid or not.
        if (cursor.isClosed() || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return false;
        }
        return true;
    }

    private boolean forwardMms(MessageItem msgItem, String[] dests, int simId, long threadId) {
        SendReq sendReq = new SendReq();
        String subject = getString(R.string.forward_prefix);
        if (msgItem.mSubject != null) {
            subject += msgItem.mSubject;
        }
        sendReq.setSubject(new EncodedStringValue(subject));
        sendReq.setBody(msgItem.mSlideshow.makeCopy());
        EncodedStringValue[] encodedNumbers = EncodedStringValue.encodeStrings(dests);
        if (encodedNumbers != null) {
            sendReq.setTo(encodedNumbers);
        }
        Uri uri = null;
        try {
            PduPersister persister = PduPersister.getPduPersister(this);
            // Copy the parts of the message here.
            uri = persister.persist(sendReq, Mms.Outbox.CONTENT_URI);
        } catch (MmsException e) {
            MmsLog.e(TAG, "forwardMms Failed: " + msgItem.mMessageUri, e);
            return false;
        }
        MessageSender sender = new MmsMessageSender(this, uri, msgItem.mSlideshow
                .getCurrentSlideshowSize());
        try {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                sender.setSimId(simId);
                sender.sendMessage(threadId);
            } else {
                sender.sendMessage(threadId);
            }
        } catch (MmsException e) {
            MmsLog.e(TAG, "forwardMms Failed: " + msgItem.mMessageUri, e);
            return false;
        }
        return true;
    }

    /**
     * M:
     * @param workingMessage
     * @return
     */
    private boolean sendMms(WorkingMessage workingMessage,long threadId,int simId) {
        if (workingMessage == null) {
            return false;
        }
        workingMessage.saveAsMms(false);
        Uri uri = workingMessage.getMessageUri();
        if (uri == null) {
            return false;
        }
        MessageSender sender = new MmsMessageSender(this, uri, workingMessage.getSlideshow()
                .getCurrentSlideshowSize());
        try {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                sender.setSimId(simId);
                sender.sendMessage(threadId);
            } else {
                sender.sendMessage(threadId);
            }
        } catch (MmsException e) {
            MmsLog.e(TAG, "forwardMms Failed: " + uri, e);
            return false;
        }
        return true;
    }

    /**
     * M:
     * @param smsBody
     * @param dests
     * @param simId
     * @param threadId
     * @return
     */
    private boolean forwardSms(String smsBody, String[] dests, int simId,long threadId) {
        // Using invalid threadId 0 here. When the message is inserted into the db, the
        // provider looks up the threadId based on the recipient(s).
        // M: extend for gemini
        SmsMessageSender smsMessageSender;
        smsMessageSender = new SmsMessageSender(this, dests, smsBody, threadId);
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            smsMessageSender.setSimId(simId);
        }
        try {
            // This call simply puts the message on a queue and sends a broadcast to start
            // a service to send the message. In queing up the message, however, it does
            // insert the message into the DB.
            smsMessageSender.sendMessage(threadId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS message, threadId=" + threadId, e);
            return false;
        }
        return true;
    }

    private void getSimInfoList() {
        mSimInfoList = new ArrayList<SIMInfo>();
        SIMInfo sim1Info = SIMInfo.getSIMInfoBySlot(this, EncapsulatedPhone.GEMINI_SIM_1);
        SIMInfo sim2Info = SIMInfo.getSIMInfoBySlot(this, EncapsulatedPhone.GEMINI_SIM_2);
        if (sim1Info != null) {
            mSimInfoList.add(sim1Info);
        }
        if (sim2Info != null) {
            mSimInfoList.add(sim2Info);
        }
        mSimCount = mSimInfoList.isEmpty() ? 0 : mSimInfoList.size();
        MmsLog.v(TAG, "MultiforwardMessage: getSimInfoList; mSimCount = " + mSimCount);
    }

    private boolean isRecipientIpMessageUser(String[] numbers) {
        if (numbers == null || numbers.length < 1) {
            return false;
        }
        for (String number : numbers) {
            if (!IpMessageUtils.getContactManager(this).isIpMessageNumber(number)) {
                return false;
            }
        }
        return true;
    }

    private void showSimSelectedDialog(Intent intent, final String numbers, final String ids) {
        MmsLog.d(TAG,"showSimSelectedDialog begin");
        final Intent it = intent;
        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        for (int i = 0; i < mSimCount; i++) {
            SIMInfo simInfo = mSimInfoList.get(i);
            HashMap<String, Object> entry = new HashMap<String, Object>();

            entry.put("simIcon", simInfo.getSimBackgroundRes());
            int state = MessageUtils.getSimStatus(i, mSimInfoList, EncapsulatedTelephonyManagerEx.getDefault());
            entry.put("simStatus", MessageUtils.getSimStatusResource(state));
            if (MmsConfig.isServiceEnabled(this, (int) simInfo.getSimId())) {
                MmsLog.d(TAG, "show ipmessage icon, simId = " + simInfo.getSimId());
                entry.put("ipmsg_indicator", IpMessageConsts.drawable.ipmsg_sim_indicator);
            } else {
                MmsLog.d(TAG, "hide ipmessage icon, simId = " + simInfo.getSimId());
                entry.put("ipmsg_indicator", 0);
            }
            String simNumber = "";
            if (!TextUtils.isEmpty(simInfo.getNumber())) {
                switch (simInfo.getDispalyNumberFormat()) {
                    case 0:// android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                        simNumber = "";
                        break;
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                        if (simInfo.getNumber().length() <= 4) {
                            simNumber = simInfo.getNumber();
                        } else {
                            simNumber = simInfo.getNumber().substring(simInfo.getNumber().length() - 4);
                        }
                        break;
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                        if (simInfo.getNumber().length() <= 4) {
                            simNumber = simInfo.getNumber();
                        } else {
                            simNumber = simInfo.getNumber().substring(0, 4);
                        }
                        break;
                    default:
                        break;
                }
            }
            if (TextUtils.isEmpty(simNumber)) {
                entry.put("simNumberShort", "");
            } else {
                entry.put("simNumberShort", simNumber);
            }

            entry.put("simName", simInfo.getDisplayName());
            if (TextUtils.isEmpty(simInfo.getNumber())) {
                entry.put("simNumber", "");
            } else {
                entry.put("simNumber", simInfo.getNumber());
            }
            if (mAssociatedSimId == (int) simInfo.getSimId()
                && it.getIntExtra(SELECT_TYPE, -1) != SIM_SELECT_FOR_SAVE_MSG_TO_SIM) {
                // if this SIM is contact SIM, set "Suggested"
                entry.put("suggested", getString(R.string.suggested));
            } else {
                // not suggested
                entry.put("suggested", "");
            }
            entries.add(entry);
        }

        final SimpleAdapter a = MessageUtils.createSimpleAdapter(entries, this);
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.sim_selected_dialog_title));
        b.setCancelable(true);
        b.setAdapter(a, new DialogInterface.OnClickListener() {
            @SuppressWarnings("unchecked")
            public final void onClick(DialogInterface dialog, int which) {
                mSelectedSimId = (int) mSimInfoList.get(which).getSimId();
                dialog.dismiss();
                if (it.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SEND_MSG) {
                    checkEcmMode(true);
                } else if (it.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SAVE_MSG_TO_SIM) {
                    ///M: do nothing , just return;
                    return;
                    // Message msg = mSaveMsgHandler.obtainMessage(MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM);
                    // msg.obj = it;
                    // mSaveMsgHandler.sendMessage(msg);
                }
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog mSIMSelectDialog = b.create();
                mSIMSelectDialog.setCanceledOnTouchOutside(false);
                mSIMSelectDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_BACK:
                                dialog.dismiss();
                                finishSelf(RESULT_CANCELED);
                                return true;
                            case KeyEvent.KEYCODE_SEARCH:
                                return true;
                            default:
                                break;
                        }
                        return false;
                    }
                });
                mSIMSelectDialog.show();
            }
        });
        MmsLog.d(TAG, "showSimSelectedDialog end");
    }
}
