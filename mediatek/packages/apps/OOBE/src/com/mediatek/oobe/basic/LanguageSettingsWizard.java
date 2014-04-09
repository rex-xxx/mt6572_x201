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

package com.mediatek.oobe.basic;

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.oobe.utils.OOBEStepPreferenceActivity;
import com.mediatek.oobe.utils.Utils;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

/**
 * @author mtk80547
 * 
 */
public class LanguageSettingsWizard extends OOBEStepPreferenceActivity {
    private static final String TAG = OOBEConstants.TAG;

    private Loc[] mLocales;
    private String[] mSpecialLocaleCodes;
    private String[] mSpecialLocaleNames;
    private Spinner mSpinner = null;
    private Button mEmergencybtn;
    private ArrayAdapter<CharSequence> mAdapter = null;

    private Locale mSelectedLoc;
    private Locale mCurrentLocale;
    private static final int DIALOG_WAITING_SWITCHING = 10001;
    private IntentFilter mSimStateIntentFilter;
    private boolean mShowEmergencycall;

    private static class Loc implements Comparable<Loc> {
        static Collator sCollator = Collator.getInstance();

        String mLabel;
        Locale mLocale;

        public Loc(String label, Locale locale) {
            this.mLabel = label;
            this.mLocale = locale;
        }

        @Override
        public String toString() {
            return this.mLabel;
        }

        @Override
        public int compareTo(Loc arg0) {
            // TODO Auto-generated method stub
            return sCollator.compare(this.mLabel, arg0.mLabel);
        }
    }

    /**
     * @author mtk54279 handle sim card status changed; use for updating emergency call button state change
     */
    private BroadcastReceiver mSimStateChangedListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String newState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
                Xlog.d(TAG, "sim card status:" + newState + ";" + slotId);

                // if a sim card is ready, we disable emergency call button
                if (newState.equals(IccCardConstants.INTENT_VALUE_ICC_READY)) {
                    mShowEmergencycall = false;
                    if (mEmergencybtn.isShown() != mShowEmergencycall) {
                        updateEmergencycallButton(mShowEmergencycall);
                    }
                } else if (newState.equals(IccCardConstants.INTENT_VALUE_ICC_NOT_READY)) {
                    mShowEmergencycall = true;
                    if (mEmergencybtn.isShown() != mShowEmergencycall) {
                        updateEmergencycallButton(mShowEmergencycall);
                    }
                }
                Xlog.d(TAG, "mShowEmergencycall:" + mShowEmergencycall 
                        + " mEmergencybtn.isShown:" + mEmergencybtn.isShown());
            }
        }
  
    };

    /**
     * @author mtk54279 update emergency call button status
     * @param mShowEmergencycall
     *            boolean
     */
    private void updateEmergencycallButton(boolean showEmergencycall) {
    	Xlog.d(TAG, "mShowEmergencycall:" + mShowEmergencycall);
    	 
    	boolean toShow = false;
    	if(Utils.isWifiOnly(this)) {
    		toShow = false;
    	} else {
    		toShow = showEmergencycall;
    	}
    	
    	Xlog.d(TAG, "[updateEmergencycallButton] toShow:" + toShow);
        if (toShow) {
            mEmergencybtn.setVisibility(View.VISIBLE);
            mEmergencybtn.setOnClickListener(mEmergencyButtonClickListener);
        } else {
            mEmergencybtn.setVisibility(View.GONE);
            mEmergencybtn.setOnClickListener(null);
        }

    }

    /**
     * mtk54279 get sim card sate, to disable/enable emergency call button.
     * 
     * @return true enable; false disable
     */
    private boolean isEmergencycallShow() {
        Xlog.d(TAG, "LanuageSettingsWizard getEmergencycallAvialiable() function");
        TelephonyManagerEx telManagerEx = TelephonyManagerEx.getDefault();

        if (Utils.isGemini()) {
            int sim1State = telManagerEx.getSimState(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1);

            int sim2State = telManagerEx.getSimState(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2);
            Xlog.d(TAG, "LanuageSettingsWizard isGemini true" + sim1State + " : " + sim2State);
            return (sim1State != TelephonyManager.SIM_STATE_READY && sim2State != TelephonyManager.SIM_STATE_READY);

        } else {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            Xlog.d(TAG, "LanuageSettingsWizard isGemini false");
            int simState = telephonyManager.getSimState();
            return (simState != TelephonyManager.SIM_STATE_READY);

        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.frame_layout);
        Xlog.d(TAG, "OnCreate LanguageSettingsWizard");
        mSimStateIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);

        mSpinner = (Spinner) findViewById(R.id.language_spinner);
        mSpinner.setVisibility(View.VISIBLE);
        mAdapter = new ArrayAdapter<CharSequence>(this, R.layout.locale_picker_item, R.id.locale);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(mLangSelectListener);

        View welcomeView = ((LinearLayout) findViewById(R.id.sp_layout));
        welcomeView.setVisibility(View.VISIBLE);
        View prefView = (findViewById(android.R.id.list));
        prefView.setVisibility(View.GONE);

        mShowEmergencycall = isEmergencycallShow();

        mEmergencybtn = (Button) findViewById(R.id.emergcy_call_button);
        updateEmergencycallButton(mShowEmergencycall);

        initSpecialLayout(R.string.oobe_title_language_setting, R.string.oobe_summary_language_setting);
    }

    protected OnItemSelectedListener mLangSelectListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // TODO Auto-generated method stub
            mSelectedLoc = mLocales[position].mLocale;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub

        }

    };

    private OnClickListener mEmergencyButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }

    };

    private void initLanguageSettings() {
        mSpecialLocaleCodes = getResources().getStringArray(R.array.special_locale_codes);
        mSpecialLocaleNames = getResources().getStringArray(R.array.special_locale_names);

        String[] locales = getAssets().getLocales();
        Arrays.sort(locales);

        final int origSize = locales.length;
        final int localeStringLength = 5;
        final int languageIndexEnd = 2;
        Loc[] preprocess = new Loc[origSize];
        int finalSize = 0;
        for (int i = 0; i < origSize; i++) {
            String s = locales[i];
            int len = s.length();
            if (len == localeStringLength) {
                String language = s.substring(0, languageIndexEnd);
                String country = s.substring(languageIndexEnd + 1, localeStringLength);
                Locale l = new Locale(language, country);

                if (finalSize == 0) {
                    if (OOBEConstants.DEBUG) {
                        Xlog.v(TAG, "adding initial " + toTitleCase(l.getDisplayLanguage(l)));
                    }
                    preprocess[finalSize++] = new Loc(toTitleCase(l.getDisplayLanguage(l)), l);
                } else {
                    // check previous entry:
                    // same lang and a country -> upgrade to full name and
                    // insert ours with full name
                    // diff lang -> insert ours with lang-only name
                    if (preprocess[finalSize - 1].mLocale.getLanguage().equals(language)) {
                        if (OOBEConstants.DEBUG) {
                            Xlog.v(TAG, "backing up and fixing " + preprocess[finalSize - 1].mLabel + " to "
                                    + getDisplayName(preprocess[finalSize - 1].mLocale));
                        }
                        preprocess[finalSize - 1].mLabel = toTitleCase(getDisplayName(preprocess[finalSize - 1].mLocale));
                        if (OOBEConstants.DEBUG) {
                            Xlog.v(TAG, "  and adding " + toTitleCase(getDisplayName(l)));
                        }
                        preprocess[finalSize++] = new Loc(toTitleCase(getDisplayName(l)), l);
                    } else {
                        String displayName;
                        if (s.equals("zz_ZZ")) {
                            displayName = "Pseudo...";
                        } else {
                            displayName = toTitleCase(l.getDisplayLanguage(l));
                        }
                        if (OOBEConstants.DEBUG) {
                            Xlog.v(TAG, "adding " + displayName);
                        }
                        preprocess[finalSize++] = new Loc(displayName, l);
                    }
                }
            }
        }

        mLocales = new Loc[finalSize];
        for (int i = 0; i < finalSize; i++) {
            mLocales[i] = preprocess[i];
        }
        Arrays.sort(mLocales);
        // Add to preference screen
        mCurrentLocale = getLanguage();
        mAdapter.clear();
        // Xlog.i(TAG, "-------->Current locale:"+String.valueOf(mCurrentLocale));
        // ActivityManagerNative.getDefault().getConfiguration().locale;
        for (int j = 0; j < finalSize; j++) {

            mAdapter.add(mLocales[j].toString());
            // Xlog.v(TAG, "mLocales["+j+"]"+mLocales[j].mLocale);

            if (mCurrentLocale != null && mCurrentLocale.equals(mLocales[j].mLocale)) {
                mSpinner.setSelection(j, true);
            }
        }
    }

    private static String toTitleCase(String s) {
        if (s.length() == 0) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getDisplayName(Locale l) {
        String code = l.toString();

        for (int i = 0; i < mSpecialLocaleCodes.length; i++) {
            if (mSpecialLocaleCodes[i].equals(code)) {
                return mSpecialLocaleNames[i];
            }
        }

        return l.getDisplayName(l);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_WAITING_SWITCHING) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.oobe_locale_switching));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        Xlog.v(TAG, "LanguageSetupActivity onDestroy");
        removeDialog(DIALOG_WAITING_SWITCHING);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        unregisterReceiver(mSimStateChangedListener);
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        initLanguageSettings();
        registerReceiver(mSimStateChangedListener, mSimStateIntentFilter);
    }

    private void updateHourFormat() {
        boolean is24Hour = DateFormat.is24HourFormat(getApplicationContext());
        if (!is24Hour) {
            String pattern = getResources().getString(com.android.internal.R.string.twelve_hour_time_format);
            if (pattern.indexOf('H') >= 0) {
                Settings.System.putString(getApplicationContext().getContentResolver(), Settings.System.TIME_12_24, "24");
            }
        }
    }

    private void setLanguage(Locale lang) {

        Xlog.v(TAG, "------Set Language: " + lang.getCountry());
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            config.locale = lang;
            config.userSetLocale = true;

            am.updateConfiguration(config);
        } catch (android.os.RemoteException e) {
            Xlog.e(TAG, "Excetpion");
            e.printStackTrace();
        }
        Xlog.v(TAG, "------Set Language: updateHourFormat");
        updateHourFormat();
        // Trigger the dirty bit for the Settings Provider.
        BackupManager.dataChanged("com.android.providers.settings");
    }

    private Locale getLanguage() {
        Xlog.v(TAG, "Get Language");
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            return config.locale;

        } catch (android.os.RemoteException e) {
            Xlog.e(TAG, "Excetpion");
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected String getStepSpecialTag() {
        return "LanguageSetupActivity";
    }


    @Override
    public void onNextStep(boolean isNext) {
        if (!isNext) {
            Xlog.i(TAG, "Back to former settings");
            finishActivityByResultCode(OOBEConstants.RESULT_CODE_BACK);
        } else {
            Xlog.i(TAG, "Forward to next settings");
            if (mLastStep) {
                finishActivityByResultCode(OOBEConstants.RESULT_CODE_FINISH);
            } else {
                if (mSelectedLoc != null && !mSelectedLoc.getCountry().equals(mCurrentLocale.getCountry())) {
                    Xlog.i(TAG, "Set Language");
                    showDialog(DIALOG_WAITING_SWITCHING);
                    setLanguage(mSelectedLoc);
                }
                Xlog.i(TAG, "------RESULT_CODE_NEXT");
                finishActivityByResultCode(OOBEConstants.RESULT_CODE_NEXT);
            }
        }
    }
}
