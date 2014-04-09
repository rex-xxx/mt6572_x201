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

package com.mediatek.lbs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.R;
import com.mediatek.common.agps.MtkAgpsConfig;
import com.mediatek.common.agps.MtkAgpsManager;
import com.mediatek.xlog.Xlog;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class AgpsNotifyDialog extends Activity {

    private String mMessage;
    private String mRequestId;
    private String mCliecntName;
    private MtkAgpsManager mAgpsMgr;
    private Timer mTimer = new Timer();
    private boolean mIsUserResponse = false;
    private boolean mGetOtherNotify = false;
    private Dialog mDialog = null;
    private String mTitle = new String();
    private int mSessionId = -1;
    private int mType = MtkAgpsManager.AGPS_NOTIFY_NONE;

    public void sendNotification(Context context, int icon, String ticker,
            String title, String content, int id) {

        Intent intent = new Intent("");
        PendingIntent appIntent = PendingIntent.getBroadcast(context, 0,
                intent, 0);

        Notification notification = new Notification();
        notification.icon = icon;
        notification.tickerText = ticker;
        notification.defaults = 0;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(context, title, content, appIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    public void finishActivity(boolean notify) {
        mTimer.cancel();
        if (notify) {
            sendNotification(this, R.drawable.ic_btn_next, mTitle, mTitle,
                    mMessage, new Random().nextInt(10000));
        }
        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (!mGetOtherNotify) {
            finish();
        } else {
            mGetOtherNotify = false;
        }
    }

    // type=0: notify only, 1=verification
    private void setTimerIfNeed(int type) {
        mTimer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                log("timeout occurred");
                if (mType == MtkAgpsManager.AGPS_NOTIFY_ALLOW_NO_ANSWER
                        || mType == MtkAgpsManager.AGPS_NOTIFY_DENY_NO_ANSWER) {
                    mAgpsMgr.niUserResponse(mSessionId,
                            MtkAgpsManager.AGPS_NI_RESPONSE_NO_RESP);
                }
                finishActivity(true);
            }
        };

        MtkAgpsConfig config = mAgpsMgr.getConfig();

        int notifyTimeout = config.notifyTimeout;
        int verifyTimeout = config.verifyTimeout;
        boolean timerEnabled = (config.niTimer == 1) ? true : false;

        log("notifyTimeout=" + notifyTimeout + " verifyTimeout="
                + verifyTimeout + " timerEnabled=" + timerEnabled);

        if (timerEnabled) {
            int timeout = 0;
            if (type == 0) {
                timeout = notifyTimeout * 1000;
            } else {
                timeout = verifyTimeout * 1000;
            }
            mTimer.schedule(task, timeout);
        }

    }

    private void setup(Intent intent) {

        if (mAgpsMgr == null) {
            mAgpsMgr = (MtkAgpsManager) getSystemService(Context.MTK_AGPS_SERVICE);
        }

        Bundle bundle = intent.getExtras();
        int type = -1;

        if (bundle != null) {
            int sessionId = 0;
            type = bundle.getInt("type", 1);
            sessionId = bundle.getInt("session_id", 0);
            mRequestId = bundle.getString("request_id");
            mCliecntName = bundle.getString("client_name");
            boolean cancel = bundle.getBoolean("cancel", false);
            int mockUserResp = bundle.getInt("resp", 0);
            log("type=[" + type + "] sessionId=[" + sessionId
                    + "] pre-essionId=[" + mSessionId + "] mRequestId=["
                    + mRequestId + "] mCliecntName=[" + mCliecntName
                    + "] cancel=[" + cancel + "] mockUserResp=[" + mockUserResp
                    + "]");
            if (cancel) {
                mGetOtherNotify = false;
                finishActivity(true);
                if (mType == MtkAgpsManager.AGPS_NOTIFY_ALLOW_NO_ANSWER
                        || mType == MtkAgpsManager.AGPS_NOTIFY_DENY_NO_ANSWER) {
                    mAgpsMgr.niUserResponse(mSessionId,
                            MtkAgpsManager.AGPS_NI_RESPONSE_NO_RESP);
                }
                return;
            }
            if (mockUserResp > 0) {
                mGetOtherNotify = false;
                finishActivity(true);
                if (mockUserResp == 1) {
                    mAgpsMgr.niUserResponse(mSessionId,
                            MtkAgpsManager.AGPS_NI_RESPONSE_ACCEPT);
                } else if (mockUserResp == 2) {
                    mAgpsMgr.niUserResponse(mSessionId,
                            MtkAgpsManager.AGPS_NI_RESPONSE_DENY);
                } else {
                    log("ERR: unknown mock user response=" + mockUserResp);
                }
                return;
            }

            if (mGetOtherNotify) {
                finishActivity(true);
                if (mType == MtkAgpsManager.AGPS_NOTIFY_ALLOW_NO_ANSWER
                        || mType == MtkAgpsManager.AGPS_NOTIFY_DENY_NO_ANSWER) {
                    mAgpsMgr.niUserResponse(mSessionId,
                            MtkAgpsManager.AGPS_NI_RESPONSE_NO_RESP);
                }
            }
            mSessionId = sessionId;
            mType = type;
        } else {
            log("Error: Bundle is null");
            finishActivity(false);
            return;
        }

        mMessage = this.getString(NOTIFY_STRING_LIST[type]);

        if (mRequestId != null && mCliecntName != null) {
            mMessage = mMessage + "\n" + getString(R.string.NI_Request_ID)
                    + ": " + mRequestId + "\n"
                    + getString(R.string.NI_Request_ClientName) + ": "
                    + mCliecntName + "\n";
        } else if (mRequestId != null) {
            mMessage = mMessage + "\n" + getString(R.string.NI_Request_ID)
                    + ": " + mRequestId;
        } else if (mCliecntName != null) {
            mMessage = mMessage + "\n"
                    + getString(R.string.NI_Request_ClientName) + ": "
                    + mCliecntName;
        }

        if (mType == MtkAgpsManager.AGPS_NOTIFY_ONLY) {
            mTitle = getString(R.string.agps_str_notify);
            setTimerIfNeed(0);
        } else if (mType == MtkAgpsManager.AGPS_NOTIFY_ALLOW_NO_ANSWER) {
            mTitle = getString(R.string.agps_str_verify);
            setTimerIfNeed(1);
        } else if (mType == MtkAgpsManager.AGPS_NOTIFY_DENY_NO_ANSWER) {
            mTitle = getString(R.string.agps_str_verify);
            setTimerIfNeed(1);
        } else {
            log("ERR: unknown type recv type=" + mType);
        }
        showDialog(mType);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate v2");
        mIsUserResponse = false;
        setup(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        log("onNewIntent");
        mGetOtherNotify = true;
        setup(intent);
    }

    public Dialog onCreateDialog(int id) {
        switch (id) {
        case MtkAgpsManager.AGPS_NOTIFY_ONLY:
            mDialog = new AlertDialog.Builder(AgpsNotifyDialog.this)
                    .setTitle(R.string.agps_str_notify)
                    .setMessage(mMessage)
                    .setCancelable(false)
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    finishActivity(false);
                                }
                            })
                    .setPositiveButton(R.string.agps_OK,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    finishActivity(false);
                                }
                            }).create();
            break;
        case MtkAgpsManager.AGPS_NOTIFY_ALLOW_NO_ANSWER:
            mDialog = new AlertDialog.Builder(AgpsNotifyDialog.this)
                    .setTitle(R.string.agps_str_verify)
                    .setMessage(mMessage)
                    .setCancelable(false)
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    mAgpsMgr.niUserResponse(
                                            mSessionId,
                                            MtkAgpsManager.AGPS_NI_RESPONSE_NO_RESP);
                                    finishActivity(false);
                                }
                            })
                    .setPositiveButton(R.string.agps_str_allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    mAgpsMgr.niUserResponse(
                                            mSessionId,
                                            MtkAgpsManager.AGPS_NI_RESPONSE_ACCEPT);
                                    finishActivity(false);
                                }
                            })
                    .setNegativeButton(R.string.agps_str_deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    mAgpsMgr.niUserResponse(
                                            mSessionId,
                                            MtkAgpsManager.AGPS_NI_RESPONSE_DENY);
                                    finishActivity(false);
                                }
                            }).create();
            break;
        case MtkAgpsManager.AGPS_NOTIFY_DENY_NO_ANSWER:
            mDialog = new AlertDialog.Builder(AgpsNotifyDialog.this)
                    .setTitle(R.string.agps_str_verify)
                    .setMessage(mMessage)
                    .setCancelable(false)
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    mAgpsMgr.niUserResponse(
                                            mSessionId,
                                            MtkAgpsManager.AGPS_NI_RESPONSE_NO_RESP);
                                    finishActivity(false);
                                }
                            })
                    .setPositiveButton(R.string.agps_str_allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    mAgpsMgr.niUserResponse(
                                            mSessionId,
                                            MtkAgpsManager.AGPS_NI_RESPONSE_ACCEPT);
                                    finishActivity(false);
                                }
                            })
                    .setNegativeButton(R.string.agps_str_deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    mAgpsMgr.niUserResponse(
                                            mSessionId,
                                            MtkAgpsManager.AGPS_NI_RESPONSE_DENY);
                                    finishActivity(false);
                                }
                            }).create();
            break;
        default:
            log("WARNING: No such dialog");
        }
        return mDialog;
    }

    private static final int NOTIFY_STRING_LIST[] = {
            R.string.AGPS_DEFAULT_STRING, R.string.AGPS_NOTIFY_ONLY,
            R.string.AGPS_NOTIFY_ALLOW_NO_ANSWER,
            R.string.AGPS_NOTIFY_DENY_NO_ANSWER, R.string.AGPS_NOTIFY_PRIVACY };

    private void log(String info) {
        Xlog.d("Settings", "[AgpsNotify] " + info + " ");
    }

}
