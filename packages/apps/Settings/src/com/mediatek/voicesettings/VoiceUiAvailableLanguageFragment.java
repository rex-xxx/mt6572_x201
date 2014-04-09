package com.mediatek.voicesettings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

public class VoiceUiAvailableLanguageFragment extends SettingsPreferenceFragment {
    private static final String TAG = "VoiceUiAvailableLanguageFragment";

    private static final int SUCCESS_RESULT = 1;
    private static final String VOICE_CONTROL_DEFAULT_LANGUAGE = "voice_control_default_language";
    private static final String VOICE_UI_SUPPORT_LANGUAGES = "voice_ui_support_languages";

    private ArrayList<RadioButtonPreference> mLanguagePreList = new ArrayList<RadioButtonPreference>();
    private RadioButtonPreference mLastSelectedPref;
    private int mDefaultLanguage = 0;
    private String[] mAvailableLangs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Xlog.d(TAG, "OnCreate VoiceUiAvailableLanguageFragment");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.voice_ui_available_langs);
        final PreferenceActivity parentActivity = (PreferenceActivity) getActivity();
        final boolean singlePane = parentActivity.onIsHidingHeaders() || !parentActivity.onIsMultiPane();

        Bundle data;
        if (singlePane) {
            data = getActivity().getIntent().getBundleExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        } else {
            data = this.getArguments();
        }
 
        if (data != null) {
            mAvailableLangs = data.getStringArray(VOICE_UI_SUPPORT_LANGUAGES);
            mDefaultLanguage = data.getInt(VOICE_CONTROL_DEFAULT_LANGUAGE);
        }
       
        Xlog.d(TAG, "voice ui deafult language: " + mAvailableLangs[mDefaultLanguage]);
        Xlog.d(TAG, mAvailableLangs.toString());

        for (int j = 0; j < mAvailableLangs.length; j++) {
            RadioButtonPreference pref = new RadioButtonPreference(
                    getActivity(), mAvailableLangs[j], "");
            pref.setKey(Integer.toString(j));

                Xlog.v(TAG, "available[" + j + "]" + mAvailableLangs[j]);

            if (mDefaultLanguage == j) {
                pref.setChecked(true);
                mLastSelectedPref = pref;
            }
            mLanguagePreList.add(pref);
            getPreferenceScreen().addPreference(pref);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference instanceof RadioButtonPreference) {
            selectLanguage((RadioButtonPreference) preference);
            Xlog.d(TAG, "default language changed to " + mAvailableLangs[mDefaultLanguage]);
            Intent data = new Intent();
            data.putExtra(VOICE_CONTROL_DEFAULT_LANGUAGE, mDefaultLanguage);
            getActivity().setResult(SUCCESS_RESULT, data);
            
            finishFragment();
        }
        return true;
    }

    private void selectLanguage(RadioButtonPreference preference) {

        if (mLastSelectedPref != null) {
            if (mLastSelectedPref == preference) {
                return;
            }
            mLastSelectedPref.setChecked(false);
        }

        preference.setChecked(true);
        mDefaultLanguage = Integer.parseInt(preference.getKey().toString());
        
        mLastSelectedPref = preference;
    }

    private void setLanguage(String lang) {
        Xlog.v(TAG, "Set Language: " + lang);

    }

    private String getLanguage() {
        Xlog.v(TAG, "Get Language");
        return null;
    }

}
