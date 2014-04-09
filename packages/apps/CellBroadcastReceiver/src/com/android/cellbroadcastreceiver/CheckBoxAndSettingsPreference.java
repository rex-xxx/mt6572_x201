/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cellbroadcastreceiver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.telephony.gemini.GeminiSmsManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.telephony.SmsManagerEx;

public class CheckBoxAndSettingsPreference extends CheckBoxPreference {
    private final String TAG = "[CMAS]CheckBoxAndSettingsPreference";

    public static final String KEY_ENABLE_CMAS_PRESIDENTIAL_ALERTS = "enable_cmas_presidential_threat_alerts";
    public static final String KEY_ENABLE_CMAS_IMMINENT_ALERTS = "enable_cmas_imminent_threat_alerts";
    public static final String KEY_ENABLE_CMAS_AMBER_ALERTS = "enable_cmas_amber_threat_alerts";
    public static final String KEY_CMAS_PREVIEW_ALERTS_TONE = "cmas_preview_alert_tone";
    public static final String KEY_ENABLE_CMAS_EXTREME_ALERTS = "enable_cmas_extreme_threat_alerts";
    public static final String KEY_ENABLE_CMAS_SEVERE_ALERTS = "enable_cmas_severe_threat_alerts";
    public static final String KEY_ENABLE_ALERT_SPEECH = "enable_cmas_speech_threat_alerts";

    public static final String KEY_ENABLE_CELLBROADCAST = "enable_cell_broadcast";
    private static final float DISABLED_ALPHA = 0.4f;

    private static final int MESSAGE_ENABLE_CB_END = 101;
    private static final int MESSAGE_ENABLE_CB_END_ERROR = 102;

    private CBSettingHandler mHandler = new CBSettingHandler();
    private Context mContext;
    private boolean mEnableCB;
    private String mChannelsInfo;

    private TextView mTitleText;
    private TextView mSummaryText;
    private ImageView mSettingsButton;
    private View mLine;
    private CheckBox mCheckBox;
    private Intent mSettingsIntent;
    private OnEnableCBChangedListener mListener;

    public CheckBoxAndSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_item);
        setWidgetLayoutResource(R.layout.preference_item_checkbox);

        mContext = context;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View textLayout = view.findViewById(R.id.alert_pref);
        textLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String key = getKey();
                if (TextUtils.isEmpty(key)) {
                    return;
                }
                if (key.equals(KEY_ENABLE_CELLBROADCAST)) {
                    Log.d(TAG, "KEY_ENABLE_CELLBROADCAST,isChecked() = " + isChecked());
                    if (isChecked()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setIconAttribute(android.R.attr.alertDialogIcon)
                                .setCancelable(true).setPositiveButton(R.string.button_dismiss,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface arg0, int arg1) {
                                                onCheckBoxClicked();
                                            }
                                        }).setNegativeButton(R.string.button_cancel, null)
                                .setMessage(R.string.cmas_disable_cmas).show();
                    } else {
                        mEnableCB = true;
                        enableCB();
                        setChecked(true);
                    }
                } else {
                    onCheckBoxClicked();
                }
            }
        });

        mCheckBox = (CheckBox) view.findViewById(android.R.id.checkbox);
        mSettingsButton = (ImageView) view.findViewById(R.id.imminent_btn);
        mTitleText = (TextView) view.findViewById(android.R.id.title);
        mSummaryText = (TextView) view.findViewById(android.R.id.summary);
        mLine = (View) view.findViewById(R.id.line);
        mSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View clickedView) {
                mSettingsIntent = new Intent(getContext(), CellBroadcastSubSettings.class);
                getContext().startActivity(mSettingsIntent);
            }
        });
        //Log.d(TAG, "key = " + getKey());

        if (getKey().equals(KEY_ENABLE_CMAS_IMMINENT_ALERTS)) {
            if (mSettingsButton != null) {
                mSettingsButton.setVisibility(View.VISIBLE);
            }
            if (mLine != null) {
                mLine.setVisibility(View.VISIBLE);
            }
        }

        enableSettingsButton();
    }

    protected void onCheckBoxClicked() {
        String key = getKey();
        if (TextUtils.isEmpty(key)) {
            return;
        }

        if (key.equals(KEY_ENABLE_CELLBROADCAST)) {
            if (!isChecked()) {
                mEnableCB = true;
            } else {
                mEnableCB = false;
            }
            enableCB();
        }

        if (key.equals(KEY_ENABLE_CMAS_IMMINENT_ALERTS) || key.equals(KEY_ENABLE_CMAS_AMBER_ALERTS)
                || key.equals(KEY_ENABLE_CMAS_EXTREME_ALERTS)
                || key.equals(KEY_ENABLE_CMAS_SEVERE_ALERTS)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setIconAttribute(android.R.attr.alertDialogIcon).setCancelable(true)
                    .setPositiveButton(R.string.button_dismiss,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    SharedPreferences.Editor editor = getContext().getSharedPreferences(
                                           "com.android.cellbroadcastreceiver_preferences",
                                    getContext().MODE_WORLD_WRITEABLE | getContext().MODE_WORLD_WRITEABLE).edit();
                                    if (isChecked()) {
                                        setChecked(false);
                                        if (getKey().equals(KEY_ENABLE_CMAS_IMMINENT_ALERTS)) {
                                                    editor.putBoolean(KEY_ENABLE_CMAS_EXTREME_ALERTS, false);
                                                    editor.putBoolean(KEY_ENABLE_CMAS_SEVERE_ALERTS, false);
                                                }
                                    } else {
                                        setChecked(true);
                                        if (getKey().equals(KEY_ENABLE_CMAS_IMMINENT_ALERTS)) {
                                            editor.putBoolean(KEY_ENABLE_CMAS_EXTREME_ALERTS, true);
                                        }
                                    }
                                    editor.commit();
                                    CellBroadcastReceiver.startConfigService(getContext());
                                }
                            }).setNegativeButton(R.string.button_cancel, null).setMessage(
                                    R.string.cmas_change_settings).show();
        } else {
            if (isChecked()) {
                setChecked(false);
            } else {
                setChecked(true);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        enableSettingsButton();
    }

    private void enableSettingsButton() {
        //Log.e(TAG, "enableSettingsButton " + getKey());
        if (getKey().equals(KEY_ENABLE_CMAS_IMMINENT_ALERTS)) {
            //Log.e(TAG, "KEY_ENABLE_CMAS_IMMINENT_ALERTS");
            if (mSettingsButton != null) {
                final boolean enable = (isChecked() && isEnabled());
                mSettingsButton.setEnabled(enable);
                mSettingsButton.setClickable(enable);
                mSettingsButton.setFocusable(enable);
                if (!enable) {
                    mSettingsButton.setAlpha(DISABLED_ALPHA);
                }
            }
        }
    }

    private void enableCB() {
        Log.d(TAG, "enableCB inThread");
        mHandler.mCheckBox = this;
        setEnabled(false);
        CBEnableThread enableThread = new CBEnableThread();
        enableThread.start();
    }

    private class CBEnableThread extends Thread {
        Message mMsg;

        @Override
        public void run() {
            Log.d(TAG, "CBEnableThread run enableCB " + mEnableCB);

            //if (GeminiSmsManager.activateCellBroadcastSms(mEnableCB, CellBroadcastMainSettings.sSlotId)) {
            if (SmsManagerEx.activateCellBroadcastSms(mEnableCB, CellBroadcastMainSettings.sSlotId)) {
                mMsg = mHandler.obtainMessage(MESSAGE_ENABLE_CB_END, 0, MESSAGE_ENABLE_CB_END, null);

                if (mEnableCB) {
                    CellBroadcastReceiver.sEnableCbMsg = mMsg;
                    CellBroadcastReceiver.startConfigService(mContext);
                } else {
                    Log.d(TAG, mEnableCB + " mMsg.sendToTarget() ");
                    mMsg.sendToTarget();
                }
                Log.d(TAG, "activateCellBroadcastSms success! ");
            } else {
                mMsg = mHandler.obtainMessage(MESSAGE_ENABLE_CB_END_ERROR, 0, MESSAGE_ENABLE_CB_END_ERROR, null);
                mMsg.sendToTarget();
                Log.d(TAG, "activateCellBroadcastSms error! ");
            }

            Log.d(TAG, "CBEnableThread exit ");

        }

    }

    public interface OnEnableCBChangedListener {
        void onEnableCBChanged(CheckBoxAndSettingsPreference preference);
    }

    public void setOnEnableCBChangedListener(OnEnableCBChangedListener listener) {
        mListener = listener;
    }

    private class CBSettingHandler extends Handler {
        CheckBoxAndSettingsPreference mCheckBox;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_ENABLE_CB_END_ERROR:
                Log.d(TAG, "handleMessage MESSAGE_ENABLE_CB_END_ERROR ");
                // TODO: add a toast message to user for error.
                Toast.makeText(mContext,
                               mContext.getResources().getString(R.string.cmas_enable_cmas_failed),
                               Toast.LENGTH_LONG).show();
                mCheckBox.setChecked(false);
                break;
            case MESSAGE_ENABLE_CB_END:
                Log.d(TAG, "handleMessage MESSAGE_ENABLE_CB_END ");
                break;
            default:
                break;
            }
            mCheckBox.setEnabled(true);
            if(mListener!=null){
                mListener.onEnableCBChanged(mCheckBox);
            }

        }
    }
}
