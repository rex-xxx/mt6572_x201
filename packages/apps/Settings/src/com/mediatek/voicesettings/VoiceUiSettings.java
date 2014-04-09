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

package com.mediatek.voicesettings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings.Secure;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.mediatek.common.voicecommand.IVoiceCommandManager;
import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.xlog.Xlog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class VoiceUiSettings extends SettingsPreferenceFragment
    implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "VoiceUiSettings";
    private static final int SUCCESS_RESULT = 1;

    private static final String KEY_VOICE_UI_LANGUAGE = "language_settings";
    private static final String KEY_VOICE_UI_INCOMMING_CALL = "incomming_all_pref";
    private static final String KEY_VOICE_UI_ALARM = "alarm_pref";
    private static final String KEY_VOICE_UI_CAMERA = "camera_pref";
    private static final String KEY_VOICE_UI_MUSIC = "music_pref";
    private static final String KEY_VOICE_UI_FOR_APP_CATEGORY = "voice_ui_app";

    private static final String VOICE_CONTROL_ENABLED = "voice_control_enabled";
    private boolean mVoiceControlEnable = false;

    //default to English
    private static final String VOICE_CONTROL_DEFAULT_LANGUAGE = "voice_control_default_language";
    private static final String VOICE_UI_SUPPORT_LANGUAGES = "voice_ui_support_languages";
    private int mDefaultLangIndex = 0;
    private int mChoosedLanguage = 0;
    private String[] mSupportLangs;
    
    private Switch mEnabledSwitch;
    private PreferenceCategory mVoiceUiAppCategory;
    private Preference mLanguagePref;
    private Locale mLocale;
//    private String mLocaleLanguage;
    private String mLocaleCode;
    private HashMap<String, String[]> mVoiceKeyWordInfos = new HashMap<String, String[]>();
    //data get from locale save
    private HashMap<String, Integer> mVoiceUiAppStatus = new HashMap<String, Integer>();
    //data get from framework
    private HashMap<String, Integer> mRawData = new HashMap<String, Integer>();
    private IVoiceCommandManager mVoiceCmdMgr;
    private VoiceCommandListener mVoiceListener;
    
    private class VoiceCommandCallback extends VoiceCommandListener {
        public VoiceCommandCallback(Context context) {
            super(context);
        }

        public void onVoiceCommandNotified(int mainAction, int subAction,Bundle extraData) {
            int result = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT);
            Xlog.d(TAG, " VoiceCommandListener subAction=" + subAction + " result=" + result);
            switch (subAction) {
            case VoiceCommandListener.ACTION_VOICE_SETTING_PROCESSLIST:
                handleVoiceUiApps(result, extraData);
                break;
            case VoiceCommandListener.ACTION_VOICE_SETTING_PROCESSUPATE:
                //upadte ok
                Xlog.d(TAG, "Voice settings for apps is " + ((result == SUCCESS_RESULT) ? "ok" : "error"));
                break;
            case VoiceCommandListener.ACTION_VOICE_SETTING_LANGUAGELIST:
                handleSupportLanguageList(result, extraData);
                break;
            case VoiceCommandListener.ACTION_VOICE_SETTING_LANGUAGEUPDATE:
                //upadte ok
                Xlog.d(TAG, "Voice language changed is " + ((result == SUCCESS_RESULT) ? "ok" : "error"));
                Xlog.d(TAG,"Current Language: " + mSupportLangs[mDefaultLangIndex]);
                mLanguagePref.setSummary(mSupportLangs[mDefaultLangIndex]);
                break;
            case VoiceCommandListener.ACTION_VOICE_SETTING_KEYWORDPATH:
                Xlog.d(TAG, "keyword path get ok");
                if (result == SUCCESS_RESULT) {
                    String fileName = extraData.getString(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                    Xlog.d(TAG, "get keywords file name :" + fileName);
                    VoiceUiUtils cfgReader = new VoiceUiUtils();
                    cfgReader.readKeyWordFromXml(mVoiceKeyWordInfos, fileName);
                    Xlog.d(TAG, mVoiceKeyWordInfos.keySet().toString());
                    for (String key : mVoiceKeyWordInfos.keySet()) {
                        Xlog.d(TAG, Arrays.toString(mVoiceKeyWordInfos.get(key)));
                    }
                    updateKeywordsSummary(mVoiceKeyWordInfos);
                }
                break;
             default:
                Xlog.d(TAG, "never should here, maybe error subAction=" + subAction);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.voice_ui_settings);
        
        mVoiceUiAppCategory = (PreferenceCategory)findPreference(KEY_VOICE_UI_FOR_APP_CATEGORY);
        mLanguagePref = findPreference(KEY_VOICE_UI_LANGUAGE);
        
        Activity activity = getActivity();
        if(!(("tablet".equals(SystemProperties.get("ro.build.characteristics"))) &&
	   (getResources().getBoolean(com.android.internal.R.bool.preferences_prefer_dual_pane))))
        {
            activity.getActionBar().setTitle(R.string.voice_ui_title);
        }

        mVoiceUiAppStatus.put("com.android.phone", 1);
        mVoiceUiAppStatus.put("com.android.deskclock", 1);
        mVoiceUiAppStatus.put("com.android.gallery3d", 1);
        mVoiceUiAppStatus.put("com.android.music", 1);

        SharedPreferences processSharedPres = getActivity().getSharedPreferences("voice_ui_settings", Context.MODE_PRIVATE);
        for (String key : mVoiceUiAppStatus.keySet()) {
            int state = processSharedPres.getInt(key, 1);
            mVoiceUiAppStatus.put(key, state);
        }
        Xlog.d(TAG, "read from shared preference " + mVoiceUiAppStatus.toString());

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy(mVoiceUiAppStatus);

        mDefaultLangIndex = Secure.getInt(getContentResolver(), VOICE_CONTROL_DEFAULT_LANGUAGE, 0);
        mChoosedLanguage = mDefaultLangIndex;
        mVoiceCmdMgr = (IVoiceCommandManager)getSystemService("voicecommand");
        if (mVoiceCmdMgr != null) {
            mVoiceListener = new VoiceCommandCallback(getActivity());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);

        final int padding = activity.getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPadding(0, 0, padding, 0);
        mEnabledSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(
                mEnabledSwitch,
                new ActionBar.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    private void createPreferenceHierarchy(HashMap<String, Integer> appStatus) {
        for (String key : appStatus.keySet()) {
            CheckBoxPreference appPref = new CheckBoxPreference(getActivity());
            if (appPref != null) {
                int value = appStatus.get(key);
                appPref.setTitle(getProcessTitleResourceId(key));
                appPref.setEnabled((value == 1) ? true : false);
                appPref.setKey(key);
                mVoiceUiAppCategory.addPreference(appPref);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Xlog.d(TAG, "--->> onResume");
        if (mVoiceCmdMgr != null) {
            try {
                mVoiceCmdMgr.registerListener(mVoiceListener);

                Xlog.d(TAG, "send command mainAction = " + VoiceCommandListener.ACTION_MAIN_VOICE_SETTING 
                        + " subAction = " + VoiceCommandListener.ACTION_VOICE_SETTING_PROCESSLIST);

                mVoiceCmdMgr.sendCommand(getActivity(),
                        VoiceCommandListener.ACTION_MAIN_VOICE_SETTING,
                        VoiceCommandListener.ACTION_VOICE_SETTING_PROCESSLIST,null);
                Xlog.d(TAG, "Support language is " + ((mSupportLangs == null) ? "null" : "not null"));
                
                if (mSupportLangs == null) {
                    // request support language list
                    sendCommand(VoiceCommandListener.ACTION_VOICE_SETTING_LANGUAGELIST);

                    //get keyword path
                    sendCommand(VoiceCommandListener.ACTION_VOICE_SETTING_KEYWORDPATH);
                }
            } catch (RemoteException e) {
                Xlog.w(TAG, "Remote exception error" + e.getMessage());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Xlog.w(TAG, "IllegalAccessException exception error" + e.getMessage());
                e.printStackTrace();
            }
        }
        //change voice ui support language, because default language is changed.
        // change support language and update keywords summary
        if (mChoosedLanguage != mDefaultLangIndex && mSupportLangs.length > 0) {
            Xlog.d(TAG, "Default language is " + mSupportLangs[mDefaultLangIndex]);
            Xlog.d(TAG, "Use choose language is " + mSupportLangs[mChoosedLanguage]);
            Xlog.d(TAG, "in onResume change support language and update keywords summary");
            Secure.putInt(getContentResolver(), VOICE_CONTROL_DEFAULT_LANGUAGE, mChoosedLanguage);
            changeSupportLanguage(mChoosedLanguage);
            mDefaultLangIndex = mChoosedLanguage;
            mLanguagePref.setSummary(mSupportLangs[mDefaultLangIndex]);
        }

        mVoiceControlEnable = (Secure.getInt(getContentResolver(), VOICE_CONTROL_ENABLED, 0) == 1);
        Xlog.d(TAG, "--->> mVoiceControlEnable = " + mVoiceControlEnable);
        //mEnabledSwitch.setChecked(mVoiceControlEnable);
        //mVoiceUiAppCategory.setEnabled(mVoiceControlEnable);

    }

    @Override
    public void onPause() {
        super.onPause();
        Xlog.d(TAG, "--->> on Pause mVoiceControlEnable = " + mVoiceControlEnable);
        Xlog.d(TAG, "--->> mVoiceUiAppStatus " + mVoiceUiAppStatus.toString());
        Xlog.d(TAG, "--->> mRawData " + mRawData.toString());

        if (mVoiceCmdMgr != null) {
            try {
                mVoiceCmdMgr.unRegisterListener(mVoiceListener);
            } catch (RemoteException e) {
                Xlog.w(TAG, "Remote exception error" + e.getMessage());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Xlog.w(TAG, "IllegalAccessException exception error" + e.getMessage());
                e.printStackTrace();
            }
        }

        SharedPreferences processSharedPres = getActivity().getSharedPreferences("voice_ui_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = processSharedPres.edit();
        for (String key : mVoiceUiAppStatus.keySet()) {
            editor.putInt(key, mVoiceUiAppStatus.get(key));
            Xlog.d(TAG, "save state now");
            Xlog.d(TAG, key + " = " + mVoiceUiAppStatus.get(key));
        }
        editor.commit();
    }

    /**
     * get process preference according its name
     * used for update preference summary or status
     * @param processName the process name of that apps
     * @return that preference
     */
    public Preference getPreferenceFromProcessName(String processName) {
        if (mVoiceUiAppCategory != null) {
            return mVoiceUiAppCategory.findPreference(processName);
        } else {
            Xlog.d(TAG, "app checkbox list category is null");
            return null;
        }
    }

    /**
     * update keywords summary
     * @param voiceKeyWordInfos keywords map
     */
    public void updateKeywordsSummary(HashMap<String, String[]> voiceKeyWordInfos) {
        Xlog.d(TAG, "update keywords summary");
        if (voiceKeyWordInfos.isEmpty()) {
            Xlog.d(TAG, "keywords is empty");
            return;
        }
        for (String processName : voiceKeyWordInfos.keySet()) {
            String[] commands = voiceKeyWordInfos.get(processName);
            Xlog.d(TAG, "updateKeywordsSummary processName = " + processName + " commands = " + Arrays.toString(commands));
            Xlog.d(TAG, "commands length is " + commands.length);
            StringBuilder keywords = new StringBuilder();
            String lastWord = "\"" + commands[commands.length - 1] + "\"";
            
            for (int i = 0; i < commands.length - 1; i++) {
                keywords.append("\"").append(commands[i]).append("\"");
                if (i != commands.length - 2) {
                    keywords.append(",");
                }
            }

            int resId = getSummaryResourceId(processName);
            if (resId == 0) {
                continue;
            }
            String summary = getString(resId, keywords.toString(), lastWord);
            
            //update which summary.
            Preference processPref = getPreferenceFromProcessName(processName);
            if (processPref != null && commands.length > 1) {
                processPref.setSummary(summary);
            }
        }
        
    }

    private int getSummaryResourceId(String process) {
        if (process.equals("com.android.deskclock")) {
            return R.string.alarm_command_summary_format;
        } else if (process.equals("com.android.phone")) {
            return R.string.incomming_command_summary_format;
        } else if (process.equals("com.android.music")) {
            return R.string.music_command_summary_format;
        } else if (process.equals("com.android.gallery3d")) {
            return R.string.camera_command_summary_format;
        } else {
            Xlog.d(TAG, "voice ui not support " + process);
            return 0;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Secure.putInt(buttonView.getContext().getContentResolver(),
                VOICE_CONTROL_ENABLED, isChecked ? 1 : 0);
        mVoiceControlEnable = isChecked;

        Xlog.d(TAG, "on checked change switch is " + (isChecked ? "checked" : "unchecked"));
        mVoiceUiAppCategory.setEnabled(isChecked);
        if (mVoiceUiAppStatus.isEmpty()) {
            Xlog.d(TAG, "mVoiceUiAppStatus not initialized");
            return;
        }
        boolean isAllOff = isAllDisabled(mRawData);
        if (isAllOff == !isChecked) {
            //no need to update value
            return;
        }
        //when swith is off, unset all voice ui settings, the checkbox is remain checked on UI
        //when on, set the voice ui settings which previous is set checked.
        //pick the value is 1, and disabled them
        if (!mEnabledSwitch.isChecked()) {
            for (String key : mRawData.keySet()) {
                if (mRawData.get(key) == 1) {
                    Xlog.d(TAG, "disable process : " + key);
                    mRawData.put(key, 0);
                }
            }
        } else {
            for (String key : mVoiceUiAppStatus.keySet()) {
                if (mVoiceUiAppStatus.get(key) == 1) {
                    Xlog.d(TAG, "enable process : " + key);
                    mRawData.put(key, 1);
                }
            }
        }
        //if Voice Control is off/on, all;
        String[] procs = mRawData.keySet().toArray(new String[]{});
        int[] values = new int[procs.length];
        for (int i = 0; i < procs.length; i++) {
            values[i] = mRawData.get(procs[i]);
        }
        setApps(procs, values);
        Xlog.d(TAG, "update these values " + (mEnabledSwitch.isChecked() ? "on " : "off ") + Arrays.toString(procs));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (mSupportLangs == null || mVoiceUiAppStatus.isEmpty()) {
            Xlog.d(TAG, "Voice UI data got from framework is null");
            return false;
        }
        Xlog.d(TAG, " onPreferenceTreeClick " + mVoiceUiAppStatus.toString());
        Xlog.d(TAG, " onPreferenceTreeClick " + mRawData.toString());

        if (KEY_VOICE_UI_LANGUAGE.equals(preference.getKey())) {
            Bundle data = new Bundle();
            data.putStringArray(VOICE_UI_SUPPORT_LANGUAGES, mSupportLangs);
            data.putInt(VOICE_CONTROL_DEFAULT_LANGUAGE, mDefaultLangIndex);
            startFragment(this, VoiceUiAvailableLanguageFragment.class.getCanonicalName(), 1, data, 
                    R.string.voice_ui_language_title);
            return true;
        } else if (mVoiceUiAppStatus.containsKey(preference.getKey())) {
            final String processName = preference.getKey();
            int value = (((CheckBoxPreference)preference).isChecked() ? 1 : 0);
            mVoiceUiAppStatus.put(processName, value);
            mRawData.put(processName, value);
            setApps(new String[]{processName}, new int[] {value});
            Xlog.d(TAG, "changed app name = " + processName + " value = " + value);

            //judge if all checkbox is disabled
            if (isAllDisabled(mRawData)) {
                Xlog.d(TAG, " set switch to off, disable app preferences");
                Secure.putInt(getActivity().getContentResolver(),
                        VOICE_CONTROL_ENABLED, 0);
                mVoiceControlEnable = false;
                mEnabledSwitch.setChecked(mVoiceControlEnable);
                mVoiceUiAppCategory.setEnabled(mVoiceControlEnable);
            }

        } else {
            Xlog.d(TAG, "onPreferenceClick not support click this preference " + preference.getKey());
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Xlog.d(TAG, "-->>result " + resultCode + " data is " + ((data == null) ? "null" : "not null"));

        //fix JE; when Select null language
        if (resultCode != SUCCESS_RESULT || data == null) {
            return;
        }

        //update support language summary.
        mChoosedLanguage = data.getIntExtra(VOICE_CONTROL_DEFAULT_LANGUAGE, 0);
        String language = mSupportLangs[mChoosedLanguage];
        mLanguagePref.setSummary(language);
        Xlog.d(TAG, "Select Voice language " + language);
    }
    
    private void handleVoiceUiApps(int result, Bundle data) {
        Xlog.d(TAG, "handleVoiceUiApps");
        if (result != VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
            Xlog.d(TAG, "handleVoiceUiApps error = " + result);
            return;
        }
        //if camera value is change, this will set to comera process index.
        String[] voiceUiApps = data.getStringArray(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
        int[] voiceUiValues = data.getIntArray(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1);
        Xlog.d(TAG, "get process data from framework: " + Arrays.toString(voiceUiApps));
        Xlog.d(TAG, "get values data from framework: " + Arrays.toString(voiceUiValues));

        for (int i = 0; i < voiceUiApps.length; i++) {
            String processName = voiceUiApps[i];
            int value = voiceUiValues[i];
            for (String availabe : mVoiceUiAppStatus.keySet()) {
                if (availabe.equals(processName)) {
                    mRawData.put(processName, value);
                }
            }
        }
        boolean isAllProcessesOff = isAllDisabled(voiceUiApps, voiceUiValues, 
                mRawData.keySet().toArray(new String[]{}));
        Xlog.d(TAG, "isAllProcessesOff=" + isAllProcessesOff + " mVoiceControlEnable=" + mVoiceControlEnable);

        if (isAllProcessesOff) {
            if (mVoiceControlEnable) {
                for (String availabe : mVoiceUiAppStatus.keySet()) {
                    mVoiceUiAppStatus.put(availabe, 0);
                }
                mVoiceControlEnable = false;
                Secure.putInt(getActivity().getContentResolver(),
                        VOICE_CONTROL_ENABLED, 0);
            }
        } else {
            mVoiceControlEnable = true;
            Secure.putInt(getActivity().getContentResolver(),
                    VOICE_CONTROL_ENABLED, 1);

            //update value on the locale
            for (int i = 0; i < voiceUiApps.length; i++) {
                String processName = voiceUiApps[i];
                int value = voiceUiValues[i];
                for (String availabe : mVoiceUiAppStatus.keySet()) {
                    if (availabe.equals(processName)) {
                        mVoiceUiAppStatus.put(processName, value);
                    }
                }
            }
        }

        Xlog.d(TAG, "mVoiceUiAppStatus hash map: " + mVoiceUiAppStatus.toString());

        Xlog.d(TAG, "mVoiceControlEnable = " + mVoiceControlEnable);
        mEnabledSwitch.setChecked(mVoiceControlEnable);
        mVoiceUiAppCategory.setEnabled(mVoiceControlEnable);

        //update app voice ui status
        updateVoiceUiAppPrefs(mVoiceUiAppStatus);
    }

    private void updateVoiceUiAppPrefs(HashMap<String, Integer> voiceUiAppStatus) {
        for (String processName : voiceUiAppStatus.keySet()) {
            boolean enabled = (voiceUiAppStatus.get(processName) == 1);
            Preference pref = getPreferenceFromProcessName(processName);
            if (pref != null && pref instanceof CheckBoxPreference) {
                CheckBoxPreference processCheckBoxPref = (CheckBoxPreference)pref;
                processCheckBoxPref.setChecked(enabled);
            }
        }
    }

    /**
     * display language, current support voice ui language on the summary
     * @param result
     * @param data
     */
    private void handleSupportLanguageList(int result, Bundle data) {
        Xlog.d(TAG, "handleSupportLanguageList result=" + result);
        if (data != null && result == SUCCESS_RESULT) {
            mSupportLangs = data.getStringArray(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
            mDefaultLangIndex = data.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1);
            Xlog.d(TAG, "handleSupportLanguageList Default language is " + mSupportLangs[mDefaultLangIndex]);
            mChoosedLanguage = mDefaultLangIndex;
            String language = mSupportLangs[mDefaultLangIndex];
            mLanguagePref.setSummary(language);
        }
    }
    
    /**
     * change app voice ui settings.
     * if the processName voice ui setting is disable, enable it. otherwise, enable it.
     * @param processNames
     * @param values
     */
    private void setApps(String[] processNames, int[] values) {
        Xlog.d(TAG, "send command: set apps names " + Arrays.toString(processNames));
        Xlog.d(TAG, "send command: set apps values " + Arrays.toString(values));
        if (processNames.length != values.length) {
            return;
        }
        //Voice control
        if (mVoiceCmdMgr != null) {
            Bundle data = new Bundle();
            data.putStringArray(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, processNames);
            data.putIntArray(VoiceCommandListener.ACTION_EXTRA_SEND_INFO1, values);
            try {
                Xlog.d(TAG, "send command mainAction = " + VoiceCommandListener.ACTION_MAIN_VOICE_SETTING 
                        + " subAction = " + VoiceCommandListener.ACTION_VOICE_SETTING_PROCESSUPATE);

                mVoiceCmdMgr.sendCommand(getActivity(),
                        VoiceCommandListener.ACTION_MAIN_VOICE_SETTING,
                        VoiceCommandListener.ACTION_VOICE_SETTING_PROCESSUPATE,data);
            } catch (RemoteException e) {
                Xlog.w(TAG, "Remote exception error" + e.getMessage());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Xlog.w(TAG, "IllegalAccessException exception error" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void changeSupportLanguage(int langIndex) {
        if (mVoiceCmdMgr != null) {
            Xlog.d(TAG, "send change support language command");
            Bundle data = new Bundle();
            data.putInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, langIndex);
            try {
                Xlog.d(TAG, "send command mainAction = " + VoiceCommandListener.ACTION_MAIN_VOICE_SETTING 
                        + " subAction = " + VoiceCommandListener.ACTION_VOICE_SETTING_LANGUAGEUPDATE);

                mVoiceCmdMgr.sendCommand(getActivity(),
                        VoiceCommandListener.ACTION_MAIN_VOICE_SETTING,
                        VoiceCommandListener.ACTION_VOICE_SETTING_LANGUAGEUPDATE,data);

                sendCommand(VoiceCommandListener.ACTION_VOICE_SETTING_KEYWORDPATH);
            } catch (RemoteException e) {
                Xlog.w(TAG, "Remote exception error" + e.getMessage());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Xlog.w(TAG, "IllegalAccessException exception error" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void sendCommand(int cmdCode) {
        if (mVoiceCmdMgr != null) {
            try {
                Xlog.d(TAG, "send command mainAction = " + VoiceCommandListener.ACTION_MAIN_VOICE_SETTING 
                        + " subAction = " + cmdCode);

                mVoiceCmdMgr.sendCommand(getActivity(),
                        VoiceCommandListener.ACTION_MAIN_VOICE_SETTING,
                        cmdCode,null);
            } catch (RemoteException e) {
                Xlog.w(TAG, "Remote exception error" + e.getMessage());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Xlog.w(TAG, "IllegalAccessException exception error" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isAllDisabled(HashMap<String, Integer> appsStatus) {
        //if all the voice ui apps are disabled, the switch is disabled
        boolean allAppsDisabled = true;
        for (Integer status : appsStatus.values()) {
            if (status == 1) {
                allAppsDisabled = false;
                break;
            }
        }
        Xlog.d(TAG, "all app disabled is " + allAppsDisabled);
        return allAppsDisabled;
    }
    
    /**
     * judge if data from framework, if it's all disabled.
     * @param processNames
     * @param values
     * @return
     */
    private boolean isAllDisabled(String[] allProcessNames, int[] values, String[] availabedProcessNames) {
        //parse available process status from data, put into a map.
        HashMap<String, Integer> processStatus = new HashMap<String, Integer>();
        Xlog.d(TAG, Arrays.toString(allProcessNames));
        Xlog.d(TAG, Arrays.toString(values));
        for (int i = 0; i < allProcessNames.length; i++) {
            String processName = allProcessNames[i];
            int value = values[i];
            for (String availabe : availabedProcessNames) {
                if (availabe.equals(processName)) {
                   processStatus.put(processName, value);
                }
            }
        }
        Xlog.d(TAG, "judge is All Disabled " + processStatus.toString());
        
        //judge if the available processes all is disabled.
        for (String key : processStatus.keySet()) {
            if (processStatus.get(key) == 1) {
                Xlog.d(TAG, key + " is " + "on");
                return false;
            }
        }
        return true;
    }

    /**
     * According to the process name, get the process preference id 
     * @param processName
     * @return
     */
     private int getProcessTitleResourceId(String processName) {
         if (processName.equals("com.android.deskclock")) {
             return R.string.alarm_app_name;
         } else if (processName.equals("com.android.phone")) {
             return R.string.incoming_call_app_name;
         } else if (processName.equals("com.android.music")) {
             return R.string.music_app_name;
         } else if (processName.equals("com.android.gallery3d")) {
             return R.string.camera_app_name;
         } else {
             Xlog.d(TAG, "voice ui not support " + processName);
             return 0;
         }
     }
}
