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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.widget.Toast;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.mediatek.phone.TimeConsumingPreferenceListener;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

public class CellBroadcastCheckBox extends CheckBoxPreference {
    private static final String LOG_TAG = "Settings/CellBroadcastCheckBox";
    private static final boolean DBG = true; //(PhoneGlobals.DBG_LEVEL >= 2);
    private static final int MESSAGE_GET_STATE = 100;
    private static final int MESSAGE_SET_STATE = 101;

    private TimeConsumingPreferenceListener mListener;
    private MyHandler mHandler = new MyHandler();
    private Phone mPhone;
    private boolean mLastCheckStatus;
    int mSimId;

    public CellBroadcastCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPhone = PhoneGlobals.getPhone();
    }

    @Override
    protected void onClick() {
        super.onClick();
        boolean state = isChecked();
        mLastCheckStatus = !state;
        setCBState(state ? 0 : 1);
        setChecked(!state);
    }
    
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        Xlog.d(LOG_TAG,"init, simId = " + simId);
        mListener = listener;
        mSimId = simId;
        
        if (!skipReading) {
            TelephonyManager telephonyManager = 
                (TelephonyManager) ((CellBroadcastActivity)listener)
                .getSystemService(Context.TELEPHONY_SERVICE);
                boolean hasIccCard;
                if (CallSettings.isMultipleSim()) {
                    hasIccCard = telephonyManager.hasIccCardGemini(mSimId);
                } else {
                    hasIccCard = telephonyManager.hasIccCard();
                }
        
            if (hasIccCard) {
                getCBState(true);
            } else {
                setChecked(false);
                setEnabled(false);
            }
        }
    }

    private void getCBState(boolean reason) {
        Message msg;
        if (reason) {
            msg = mHandler.obtainMessage(MESSAGE_GET_STATE, 0,MESSAGE_GET_STATE, null);
        } else {
            msg = mHandler.obtainMessage(MESSAGE_GET_STATE, 0,MESSAGE_SET_STATE, null);
        }
        if (CallSettings.isMultipleSim()) {
            ((GeminiPhone)mPhone).queryCellBroadcastSmsActivationGemini(msg, mSimId);
        } else {
            mPhone.queryCellBroadcastSmsActivation(msg);
        }

        if (reason) {
            if (mListener != null && msg.arg2 == MESSAGE_SET_STATE) {
                mListener.onStarted(CellBroadcastCheckBox.this, reason);
            }
        }
    }

    private void setCBState(int state) {
        Message msg;
        msg = mHandler.obtainMessage(MESSAGE_SET_STATE, 0, MESSAGE_SET_STATE,null);
        if (CallSettings.isMultipleSim()) {
            ((GeminiPhone)mPhone).activateCellBroadcastSmsGemini(state,msg, mSimId);
        } else {
            mPhone.activateCellBroadcastSms(state,msg);
        }

        if (mListener != null) {
            mListener.onStarted(CellBroadcastCheckBox.this, false);
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_STATE:
                handleGetStateResponse(msg);
                break;
            case MESSAGE_SET_STATE:
                handleSetStateResponse(msg);
                break;
            default:
                break;
            }
        }

        private void handleGetStateResponse(Message msg) {
            if (msg.arg2 == MESSAGE_GET_STATE) {
                if (mListener != null) {
                    //mListener.onFinished(CellBroadcastCheckBox.this, true);
                    if (DBG) {
                        Xlog.d(LOG_TAG, "For init query, there's no reading dialog!");
                    }
                }
            } else {
                if (mListener != null) {
                    mListener.onFinished(CellBroadcastCheckBox.this, false);
                    if (!mLastCheckStatus) {
                        RecoverChannelSettings setting = 
                            new RecoverChannelSettings(mSimId, getContext().getContentResolver());
                        setting.updateChannelStatus();
                    }
                }
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar == null) {
                Xlog.i(LOG_TAG, "handleGetStateResponse,ar is null");
                if (msg.arg2 == MESSAGE_GET_STATE) {
                    CellBroadcastCheckBox.this.setChecked(false);
                    CellBroadcastCheckBox.this.setEnabled(false);
                } else {
                    if (mListener != null) {
                        mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                    }
                }
                return;
            }
            if (ar.exception != null) {
                if (DBG) {
                    Xlog.d(LOG_TAG, "handleGetStateResponse: ar.exception=" + ar.exception);
                }
                if (msg.arg2 == MESSAGE_GET_STATE) {
                    CellBroadcastCheckBox.this.setChecked(false);
                    CellBroadcastCheckBox.this.setEnabled(false);
                } else {
                      if (mListener != null) {
                          mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                      }
                }
                return;
            } else {
                if (ar.userObj instanceof Throwable) {
                    if (msg.arg2 == MESSAGE_GET_STATE) {
                        CellBroadcastCheckBox.this.setChecked(false);
                        CellBroadcastCheckBox.this.setEnabled(false);
                    } else {
                        if (mListener != null) {
                            mListener.onError(CellBroadcastCheckBox.this,RESPONSE_ERROR);
                        }
                    }
                    return;
                } else {
                    if (ar.result != null) {
                        Boolean state = (Boolean) ar.result;
                        CellBroadcastCheckBox.this.setChecked(state.booleanValue());
                    } else {
                        if (msg.arg2 == MESSAGE_GET_STATE) {
                            CellBroadcastCheckBox.this.setChecked(false);
                            CellBroadcastCheckBox.this.setEnabled(false);
                        } else {
                            if (mListener != null) {
                                mListener.onError(CellBroadcastCheckBox.this,RESPONSE_ERROR);
                            }
                        }
                        return;
                    }
                }
            }
        }

        private void handleSetStateResponse(Message msg) {
            if (msg.arg2 == MESSAGE_SET_STATE) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    Xlog.i(LOG_TAG, "handleSetStateResponse,ar is null");
                    mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                    return;
                }
                if (ar.exception != null) {
                    if (DBG) {
                        Xlog.d(LOG_TAG, "handleSetStateResponse: ar.exception=" + ar.exception);
                    }
                    if (mListener != null) {
                        mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                    }
                } else {
                        Xlog.i(LOG_TAG, "handleSetStateResponse: re get ok");
                        getCBState(false);
                }
            }
        }
    }
}

class RecoverChannelSettings extends Handler {

    private static final int MESSAGE_SET_CONFIG = 101;
    private static final String LOG_TAG = "RecoverChannelSettings";
    private static final String KEYID = "_id";
    private static final String NAME = "name";
    private static final String NUMBER = "number";
    private static final String ENABLE = "enable";
    private static final Uri CHANNEL_URI = Uri.parse("content://cb/channel");
    private static final Uri CHANNEL_URI1 = Uri.parse("content://cb/channel1");
    ///M: add for Gemini+
    private static final Uri CHANNEL_URI2 = Uri.parse("content://cb/channel2");
    private static final Uri CHANNEL_URI3 = Uri.parse("content://cb/channel3");
    
    private Uri mUri = CHANNEL_URI;
    private int mSimId;    
    Phone mPhone = null;
    private ContentResolver mResolver = null;
    
    public RecoverChannelSettings(int simId, ContentResolver resolver) {
        mSimId = simId;
        mPhone = PhoneGlobals.getPhone();
        this.mResolver = resolver;
        
        if (CallSettings.isMultipleSim()) {
            switch (mSimId) {
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
                Xlog.d(LOG_TAG,"error with simid = " + mSimId);
                break;
            }
        }
    }
    
    private ArrayList<CellBroadcastChannel> mChannelArray = new ArrayList<CellBroadcastChannel>();
    
    private boolean updateChannelToDatabase(int index) {
        final CellBroadcastChannel channel = mChannelArray.get(index);
        final int id = channel.getKeyId();
        final String name = channel.getChannelName();
        final boolean enable = false;
        final int number = channel.getChannelId();
        ContentValues values = new ContentValues();
        values.put(KEYID, id);
        values.put(NAME, name);
        values.put(NUMBER, number);
        values.put(ENABLE, Integer.valueOf(enable ? 1 : 0));
        String where = KEYID + "=" + channel.getKeyId();
        final int rows = mResolver.update(mUri, values,where, null);
        return rows > 0;
    }

    boolean queryChannelFromDatabase() {
        String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE };
        Cursor cursor = null;
        try {
            cursor = mResolver.query(mUri,projection, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    CellBroadcastChannel channel = new CellBroadcastChannel();
                    channel.setChannelId(cursor.getInt(2));
                    channel.setKeyId(cursor.getInt(0));// keyid for delete or edit
                    channel.setChannelName(cursor.getString(1));
                    channel.setChannelState(cursor.getInt(3) == 1);
                    mChannelArray.add(channel);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return true;
    }

    /**
     * when enable channels, we set once that these channels which are enable
     * and channelId neighboring in the DB to reduce times to reduce API.
     * eg: the channel id maybe is 1(true),2(true),3(false) ,4(true), 5(false),6(true)
     * we send three times (1,2; 4; 6)
      */
    public void updateChannelStatus() {
        if (!queryChannelFromDatabase()) {
            Xlog.d(LOG_TAG, "queryChannelFromDatabase failure!");
            return ;
        }
        int length = mChannelArray.size();
        Xlog.d(LOG_TAG, "updateChannelStatus length: " + length);
        SmsBroadcastConfigInfo infoList = null;
        int channelId = -1;
        boolean channelState;
        for (int i = 0; i < length; i++) {
            channelId = mChannelArray.get(i).getChannelId();
            channelState = mChannelArray.get(i).getChannelState();
            if (channelState) {
                if (infoList == null) {
                    infoList = new SmsBroadcastConfigInfo(channelId, channelId, -1, -1, true);
                } else if (infoList.getToServiceId() != (channelId - 1)) {
                    SmsBroadcastConfigInfo[] info = new SmsBroadcastConfigInfo[1];
                    info[0] = infoList;
                    setCellBroadcastConfig(info, infoList.getToServiceId());
                    infoList = new SmsBroadcastConfigInfo(channelId, channelId, -1, -1, true);
                } else {
                    infoList.setToServiceId(channelId);
                }
            }
        }
        if (infoList != null) {
            Xlog.d(LOG_TAG, "updateChannelStatus last times");
            SmsBroadcastConfigInfo[] info = new SmsBroadcastConfigInfo[1];
            info[0] = infoList;
            setCellBroadcastConfig(info, infoList.getToServiceId());
        }
    }

    private void setCellBroadcastConfig(SmsBroadcastConfigInfo[] objectList, int index) {
        Message msg = obtainMessage(MESSAGE_SET_CONFIG, 0, index, null);
        if (CallSettings.isMultipleSim()) {
            ((GeminiPhone)mPhone).setCellBroadcastSmsConfigGemini(objectList, objectList, msg, mSimId);
        } else {
            mPhone.setCellBroadcastSmsConfig(objectList, objectList, msg);
        }
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
        case MESSAGE_SET_CONFIG:
            handleSetCellBroadcastConfigResponse(msg);
            break;
        default:
            break;
        }
    }

    /**
     * when set cell broadcast exception, we set DB false.
     *
     * and when enable channels, we set once that these channels which are enable
     * and channelId neighboring in the DB , so happened exception, set these channel
     * false
     * @param msg
     */
    private void handleSetCellBroadcastConfigResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar == null) {
                Xlog.i(LOG_TAG,"handleSetCellBroadcastConfigResponse,ar is null");
            }
            if (ar.exception != null) {
                int location = -1;
                int length = mChannelArray.size();
                int channelId;
                boolean channelState;
                // find the back channel id index.
                for (int i = 0; i < length; i++) {
                    channelId = mChannelArray.get(i).getChannelId();
                    if (channelId == msg.arg2) {
                        location = i;
                        break;
                    }
                }
                Xlog.d(LOG_TAG,"handleSetCellBroadcastConfigResponse: ar.exception = " + ar.exception);
                for (int i = location; i >= 0; i--) {
                    channelState = mChannelArray.get(i).getChannelState();
                    //neighboring channelId maybe one team, so find it, and set it false.
                    if (channelState) {
                        this.updateChannelToDatabase(i);
                    } else {
                        break;
                    }
                }
            }
    }
}
