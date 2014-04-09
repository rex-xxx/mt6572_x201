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

package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.phone.TimeConsumingPreferenceListener;
import com.mediatek.settings.CallSettings;
import com.mediatek.xlog.Xlog;

public class CallWaitingCheckBoxPreference extends CheckBoxPreference {
    private static final String LOG_TAG = "Settings/CallWaitingCheckBoxPreference";
    private static final boolean DBG = true;//(PhoneApp.DBG_LEVEL >= 2);

    private final MyHandler mHandler = new MyHandler();
    private final Phone mPhone;
    private TimeConsumingPreferenceListener mTcpListener;
    /// M: support for gemini phone
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;
    private int mServiceClass = CommandsInterface.SERVICE_CLASS_VOICE;

    public CallWaitingCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPhone = PhoneGlobals.getPhone();
    }

    public CallWaitingCheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public CallWaitingCheckBoxPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        Xlog.d(LOG_TAG,"init, simId = " + simId);
        mTcpListener = listener;
        mSimId = simId;
        
        if (!skipReading) {
            /// M: for gemini phone
            if (CallSettings.isMultipleSim()) {
                if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                    ((GeminiPhone)mPhone).getVtCallWaitingGemini(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
                            MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING), mSimId);
                } else {
                    ((GeminiPhone)mPhone).getCallWaitingGemini(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
                            MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING), mSimId);
                }
            } else {
                if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                    mPhone.getVtCallWaiting(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
                            MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING));
                } else {
                    mPhone.getCallWaiting(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
                            MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING));
                }
            }
            if (mTcpListener != null) {
                mTcpListener.onStarted(this, true);
            }
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        boolean toState = isChecked();
        setChecked(!toState);
        /// M: for gemini phone
        if (CallSettings.isMultipleSim()) {
            if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                ((GeminiPhone)mPhone).setVtCallWaitingGemini(toState,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING), mSimId);
            } else {
                ((GeminiPhone)mPhone).setCallWaitingGemini(toState,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING), mSimId);
            }
        } else {
            if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                mPhone.setVtCallWaiting(toState,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING));
            } else {
                mPhone.setCallWaiting(toState,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING));
            }
        }
        if (mTcpListener != null) {
            mTcpListener.onStarted(this, false);
        }
    }

    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_CALL_WAITING = 0;
        private static final int MESSAGE_SET_CALL_WAITING = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CALL_WAITING:
                    handleGetCallWaitingResponse(msg);
                    break;
                case MESSAGE_SET_CALL_WAITING:
                    handleSetCallWaitingResponse(msg);
                    break;
                default:
                    break;
            }
        }

        private void handleGetCallWaitingResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (mTcpListener != null) {
                if (msg.arg2 == MESSAGE_SET_CALL_WAITING) {
                    mTcpListener.onFinished(CallWaitingCheckBoxPreference.this, false);
                } else {
                    mTcpListener.onFinished(CallWaitingCheckBoxPreference.this, true);
                }
            }

            if (ar.exception != null) {
                if (DBG) {
                    Xlog.d(LOG_TAG, "handleGetCallWaitingResponse: ar.exception=" + ar.exception);
                }
                setEnabled(false);
                if (mTcpListener != null) {
                    mTcpListener.onException(CallWaitingCheckBoxPreference.this,
                            (CommandException)ar.exception);
                }
            } else if (ar.userObj instanceof Throwable) {
                if (mTcpListener != null) {
                    mTcpListener.onError(CallWaitingCheckBoxPreference.this, RESPONSE_ERROR);
                }
            } else {
                if (DBG) {
                    Xlog.d(LOG_TAG, "handleGetCallWaitingResponse: CW state successfully queried.");
                }
                int[] cwArray = (int[])ar.result;
                // If cwArray[0] is = 1, then cwArray[1] must follow,
                // with the TS 27.007 service class bit vector of services
                // for which call waiting is enabled.
                try {
                    setChecked(((cwArray[0] == 1) && ((cwArray[1] & 0x01) == 0x01)));
                } catch (ArrayIndexOutOfBoundsException e) {
                    Xlog.e(LOG_TAG, "handleGetCallWaitingResponse: improper result: err ="
                            + e.getMessage());
                }
            }
        }

        private void handleSetCallWaitingResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) {
                    Xlog.d(LOG_TAG, "handleSetCallWaitingResponse: ar.exception=" + ar.exception);
                }
            }
            if (DBG) {
                Xlog.d(LOG_TAG, "handleSetCallWaitingResponse: re get");
            }
            /// M: for gemini phone
            if (CallSettings.isMultipleSim()) {
                if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                    ((GeminiPhone)mPhone).getVtCallWaitingGemini(obtainMessage(MESSAGE_GET_CALL_WAITING,
                            MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception), mSimId);
                } else {
                    ((GeminiPhone)mPhone).getCallWaitingGemini(obtainMessage(MESSAGE_GET_CALL_WAITING,
                        MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception), mSimId);
                }
            } else {
                if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                    mPhone.getVtCallWaiting(obtainMessage(MESSAGE_GET_CALL_WAITING,
                            MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception));
                } else {
                    mPhone.getCallWaiting(obtainMessage(MESSAGE_GET_CALL_WAITING,
                        MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception));
                }
            }
        }
    }
    
    public void setServiceClass(int serviceClass) {
        mServiceClass = serviceClass;
    }
}
