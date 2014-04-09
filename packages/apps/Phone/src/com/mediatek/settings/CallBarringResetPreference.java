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
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.FDN_FAIL;
import static com.android.phone.TimeConsumingPreferenceActivity.PASSWORD_ERROR;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.mediatek.phone.TimeConsumingPreferenceListener;
    
public class CallBarringResetPreference extends Preference implements
        OnPreferenceClickListener {
    private static final String LOG_TAG = "CallBarringResetPreference";
    private static final boolean DBG = true; 

    private Context mContext;
    private CallBarringInterface mCallBarringInterface = null;
    private static final int PASSWORD_LENGTH = 4;
    private MyHandler mHandler = new MyHandler();
    private TimeConsumingPreferenceListener mTcpListener;
    private Phone mPhone;
    
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;
    
    private int mServiceClass = CommandsInterface.SERVICE_CLASS_VOICE;

    public CallBarringResetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        mPhone = PhoneGlobals.getPhone();
    }

    public void setListener(TimeConsumingPreferenceListener listener) {
        mTcpListener = listener;
    }

    public void setCallBarringInterface(CallBarringInterface i, int simId) {
        mCallBarringInterface = i;
        mSimId = simId;
    }

    public boolean onPreferenceClick(Preference preference) {
        doPreferenceClick(mContext.getString(R.string.deactivate_all));
        return true;
    }

    public void doPreferenceClick(final String title) {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        final View textEntryView = inflater.inflate(
                R.layout.callbarring_option, null);
        TextView content = (TextView) textEntryView.findViewById(R.id.ViewTop);
        content.setText(mContext
                .getString(R.string.enter_callbarring_password));

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(textEntryView);
        builder.setTitle(title);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText passwordText = (EditText) textEntryView
                                .findViewById(R.id.EditPassword);
                        String password = passwordText.getText().toString();
                        if (!validatePassword(password)) {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(
                                    mContext);
                            builder1.setTitle(title);
                            builder1.setMessage(mContext
                                    .getText(R.string.wrong_password));
                            builder1.setCancelable(false);
                            builder1.setPositiveButton(R.string.ok, null);
                            builder1.create().show();
                            return;
                        }
                        // need notify parent the click event.
                        if (mCallBarringInterface != null) {
                            setCallState(password);
                        }
                    }
                });
        
        AlertDialog dlg = builder.create();
        
        if (dlg != null) {
            Window window = dlg.getWindow();
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dlg.show();
        }
        return;
    }
    
    private boolean validatePassword(String password) {
        return password != null && password.length() == PASSWORD_LENGTH;
    }

    private void init(Context context) {
        mContext = context;
        setEnabled(true);
        setOnPreferenceClickListener(this);
    }

    public void setCallState(String password) {
        setCallState(false, password);    
    }    

    private void setCallState(boolean enable, String password) {
        if (DBG) {
            Log.i(LOG_TAG, "setCallState() is called " + "password is " + password + "enable is "
                    + enable);
        }
        Message m = mHandler.obtainMessage(
                MyHandler.MESSAGE_SET_CALLBARRING_STATE, 0,
                0, null);

        if (CallSettings.isMultipleSim()) {
            if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                ((GeminiPhone)mPhone).setVtFacilityLockGemini(
                        CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m, mSimId);
            } else {
                ((GeminiPhone)mPhone).setFacilityLockGemini(
                        CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m, mSimId);
            }
        } else {
            if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                mPhone.setVtFacilityLock(CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m);
            } else {
                mPhone.setFacilityLock(CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m);
            }
        }

        if (mTcpListener != null) {
            mTcpListener.onStarted(this, false);
        }
    }
    
    private class MyHandler extends Handler {
        private static final int MESSAGE_SET_CALLBARRING_STATE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SET_CALLBARRING_STATE:
                mTcpListener.onFinished(CallBarringResetPreference.this, false);
                handleSetCallBarringResponse(msg);
                break;
            default:
                break;
            }
        }
        
        private void handleSetCallBarringResponse(Message msg) {
            int errorid;
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG) {
                    Log.i(LOG_TAG, "handleSetCallBarringResponse: ar.exception="
                            + ar.exception);
                }

                CommandException ce = (CommandException) ar.exception;
                if (ce.getCommandError() == CommandException.Error.PASSWORD_INCORRECT) {
                    errorid =  PASSWORD_ERROR;
                } else if (ce.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE) {
                    errorid = FDN_FAIL;
                } else {
                    errorid = EXCEPTION_ERROR;
                }
                mCallBarringInterface.setErrorState(errorid);
                mTcpListener.onError(CallBarringResetPreference.this, errorid);
            } else {
                if (DBG) {
                    Log.i(LOG_TAG,
                            "handleSetCallBarringResponse is called without exception");
                }
                if (mCallBarringInterface instanceof CallBarring) {
                    CallBarring cb = (CallBarring)mCallBarringInterface;
                    PreferenceScreen prefSet = cb.getPreferenceScreen();
                    CallBarringBasePreference mCallAllOutButton = (CallBarringBasePreference) prefSet
                    .findPreference("all_outing_key");
                    
                    if (mCallAllOutButton != null) {
                        cb.resetIndex(0);
                        mCallAllOutButton.init(cb, false, mSimId);
                    }
                } else {
                    mCallBarringInterface.doCancelAllState();
                }
            }
        }
    }
    
    public void setServiceClass(int serviceClass) {
        mServiceClass = serviceClass;
    }
}
