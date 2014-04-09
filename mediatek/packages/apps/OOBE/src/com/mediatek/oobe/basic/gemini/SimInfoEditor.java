/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.oobe.basic.gemini;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Spinner;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEStepPreferenceActivity;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class SimInfoEditor extends OOBEStepPreferenceActivity implements OnPreferenceChangeListener, TextWatcher {
    private static final String TAG = "SimInfoEditor";
    private static final int DIALOG_SIM_NAME_DUP = 1010;

    private static final String KEY_SIM_NAME = "sim_name";
    private static final String KEY_SIM_NUMBER = "sim_number";
    private static final String KEY_SIM_COLOR = "sim_color";
    private static final String KEY_SIM_NUMBER_FORMAT = "sim_number_format";

    private static final String KEY_SIM_STATUS = "status_info";

    private String[] mArrayNumFormat;

    private String mNotSet;
    private long mSimID;

    private ListPreference mSimNumberFormat;
    private EditTextPreference mSimName;
    private EditTextPreference mSimNumber;
    private ColorPickerPreference mSimColor;

    private IntentFilter mIntentFilter;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for hot swap {
                Xlog.d(TAG, "ACTION_SIM_INFO_UPDATE received");
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(SimInfoEditor.this);
                if (temp.size() == 0 || (temp.size() == 1 && temp.get(0).mSimId != mSimID)) {
                    Xlog.d(TAG, "sim card number is " + temp.size());
                    finish();
                }
                ///@}
            }
        }
    };


    @Override
    protected String getStepSpecialTag() {
        return "SimInfoEditor";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotSet = getResources().getString(R.string.apn_not_set);

        Intent intent = getIntent();

        Bundle extras = intent.getExtras();
        if (extras != null) {
            mSimID = extras.getLong(GeminiUtils.EXTRA_SIMID, -1);
        }
        Xlog.i(TAG, "simid is " + mSimID);

        mArrayNumFormat = getResources().getStringArray(R.array.gemini_sim_info_number_display_format_entries);

        initSpecialLayout(R.string.oobe_title_sim_edit, R.string.oobe_summary_sim_edit);
        addPreferencesFromResource(R.xml.sim_info_editor);

        mSimNumberFormat = (ListPreference) findPreference(KEY_SIM_NUMBER_FORMAT);
        mSimNumberFormat.setOnPreferenceChangeListener(this);

        mSimName = (EditTextPreference) findPreference(KEY_SIM_NAME);
        mSimName.setOnPreferenceChangeListener(this);

        mSimNumber = (EditTextPreference) findPreference(KEY_SIM_NUMBER);
        mSimNumber.setOnPreferenceChangeListener(this);

        mSimColor = (ColorPickerPreference) findPreference(KEY_SIM_COLOR);
        mSimColor.setSimID(mSimID);

        mSimColor.setOnPreferenceChangeListener(this);

        Spinner spinner = (Spinner) findViewById(R.id.language_spinner);
        if (spinner != null) {
            spinner.setVisibility(View.GONE);
        }
        mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE); 
    }

    @Override
    public void onResume() {
        super.onResume();

        updateInfo();

        mSimName.getEditText().addTextChangedListener(this);
        registerReceiver(mReceiver, mIntentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // TODO Auto-generated method stub

        if (preference.getKey().equals(KEY_SIM_STATUS)) {

            Intent it = new Intent();
            it.setClassName("com.android.settings", "com.android.settings.deviceinfo.SimStatusGemini");

            int slot = SIMInfo.getSlotById(this, mSimID);

            if (slot < 0) {
                return false;
            }
            it.putExtra("slotid", slot);
            Xlog.i(TAG, "slotid is " + slot);

            startActivity(it);
        }
        return false;
    }

    private void updateInfo() {
        SIMInfo siminfo = SIMInfo.getSIMInfoById(this, mSimID);
        if (siminfo != null) {

            if (siminfo.mDisplayName == null) {
                mSimName.setSummary(mNotSet);
            } else {
                mSimName.setSummary(siminfo.mDisplayName);
                mSimName.setText(siminfo.mDisplayName);
            }

            if ((siminfo.mNumber != null) && (siminfo.mNumber.length() != 0)) {

                mSimNumber.setSummary(siminfo.mNumber);
                mSimNumber.setText(siminfo.mNumber);

            } else {
                mSimNumber.setSummary(mNotSet);
                mSimNumber.setText("");

            }

            mSimColor.setInitValue(siminfo.mColor);
            int nIndex = turnNumformatValuetoIndex(siminfo.mDispalyNumberFormat);
            if (nIndex < 0) {
                return;
            }
            mSimNumberFormat.setValueIndex(nIndex);
            mSimNumberFormat.setSummary(mArrayNumFormat[nIndex]);

        }
    }

    private int turnNumformatValuetoIndex(int value) {

        if (value == 0) {
            return 2;
        }
        return (value - 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        if (KEY_SIM_NAME.equals(key)) {
            Editable textName = mSimName.getEditText().getText();

            if (textName != null) {
                String name = mSimName.getEditText().getText().toString();
                SIMInfo siminfo = SIMInfo.getSIMInfoById(this, mSimID);
                Xlog.i(TAG, "name is " + name);
                if ((siminfo != null) && (name != null)) {
                    if (name.equals(siminfo.mDisplayName)) {
                        return false;
                    }

                }

                int result = SIMInfo.setDisplayName(this, name, mSimID);

                Xlog.i(TAG, "result is " + result);
                if (result > 0) {

                    mSimName.setSummary(name);
                    Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                    intent.putExtra("simid", mSimID);
                    intent.putExtra("type", 0);
                    sendBroadcast(intent);
                } else {

                    if (result == SIMInfo.ErrorCode.ERROR_NAME_EXIST) {
                        showDialog(DIALOG_SIM_NAME_DUP);
                    }

                    if ((siminfo != null) && (siminfo.mDisplayName != null)) {
                        mSimName.setText(siminfo.mDisplayName);

                    }
                    return false;
                }
            }

        } else if (KEY_SIM_COLOR.equals(key)) {

            if (SIMInfo.setColor(this, ((Integer) objValue).intValue(), mSimID) > 0) {
                Xlog.i(TAG, "set color succeed " + objValue);
                Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                intent.putExtra("simid", mSimID);
                intent.putExtra("type", 1);
                sendBroadcast(intent);
            }

        } else if (KEY_SIM_NUMBER.equals(key)) {

            Editable textNumber = mSimNumber.getEditText().getText();
            if (textNumber != null) {
                Xlog.i(TAG, "textNumber != null ");
                String number = textNumber.toString();
                if (SIMInfo.setNumber(this, number, mSimID) > 0) {

                    Xlog.i(TAG, "set number succeed " + number);
                    if ((number != null) && (number.length() != 0)) {
                        mSimNumber.setSummary(number);
                    } else {
                        mSimNumber.setSummary(mNotSet);
                    }

                    Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                    intent.putExtra("simid", mSimID);
                    intent.putExtra("type", 2);
                    sendBroadcast(intent);
                } else {
                    SIMInfo siminfo = SIMInfo.getSIMInfoById(this, mSimID);
                    if (siminfo != null) {
                        if ((siminfo.mNumber != null) && (siminfo.mNumber.length() != 0)) {

                            mSimNumber.setText(siminfo.mNumber);

                        } else {

                            mSimNumber.setText("");

                        }

                    }
                    return false;
                }

            }

        } else if (KEY_SIM_NUMBER_FORMAT.equals(key)) {

            int value = Integer.parseInt((String) objValue);
            Xlog.i(TAG, "KEY_SIM_NUMBER_FORMAT is " + value);

            if (value < 0) {
                return false;
            }

            if (SIMInfo.setDispalyNumberFormat(this, value, mSimID) > 0) {

                Xlog.i(TAG, "set format succeed " + value);
                final int simNumType = 3;
                int nIndex = turnNumformatValuetoIndex(value);

                mSimNumberFormat.setSummary(mArrayNumFormat[nIndex]);
                Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                intent.putExtra("simid", mSimID);
                intent.putExtra("type", simNumType);
                sendBroadcast(intent);
            }
        }

        return true;
    }

    @Override
    public void onNextStep(boolean isNext) {
        finish();
        if (isNext) {
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
        } else {
            overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        }
    }

    @Override
    protected void finishActivityByResultCode(int resultCode) {
        finish();
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        Xlog.i(TAG, "SimInfoEditor Finish " + getStepSpecialTag());
    }

    @Override
    public void afterTextChanged(Editable arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub
        Dialog d = mSimName.getDialog();
        if (d instanceof AlertDialog) {
            ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.length() > 0);
        }

    }

    @Override
    public Dialog onCreateDialog(int id) {

        Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDlg;

        switch (id) {
        case DIALOG_SIM_NAME_DUP:
            builder.setTitle(getResources().getString(R.string.gemini_sim_info_editor_name_dup_title));
            builder.setIcon(com.android.internal.R.drawable.ic_dialog_alert);
            builder.setMessage(getResources().getString(R.string.gemini_sim_info_editor_name_dup_msg));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    // TODO Auto-generated method stub

                }
            });
            alertDlg = builder.create();
            return alertDlg;

        default:
            return null;
        }
    }

}
