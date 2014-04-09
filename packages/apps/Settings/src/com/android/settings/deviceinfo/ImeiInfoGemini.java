package com.android.settings.deviceinfo;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.settings.R;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

public class ImeiInfoGemini extends PreferenceActivity {
    private static final String TAG = "ImeiInfoGemini";
    private static final String KEY_IMEI_SLOT1 = "imei_slot1";
    private static final String KEY_IMEI_SLOT2 = "imei_slot2";
    private static final String KEY_IMEI_SLOT3 = "imei_slot3";
    private static final String KEY_IMEI_SV_SLOT1 = "imei_sv_slot1";
    private static final String KEY_IMEI_SV_SLOT2 = "imei_sv_slot2";
    private static final String KEY_IMEI_SV_SLOT3 = "imei_sv_slot3";
    private static final String KEY_PRL_VERSION_SLOT1 = "prl_version_slot1";
    private static final String KEY_PRL_VERSION_SLOT2 = "prl_version_slot2";
    private static final String KEY_PRL_VERSION_SLOT3 = "prl_version_slot3";
    private static final String KEY_MEID_NUMBER_SLOT1 = "meid_number_slot1";
    private static final String KEY_MEID_NUMBER_SLOT2 = "meid_number_slot2";
    private static final String KEY_MEID_NUMBER_SLOT3 = "meid_number_slot3";
    private static final String KEY_MIN_NUMBER_SLOT1 = "min_number_slot1";
    private static final String KEY_MIN_NUMBER_SLOT2 = "min_number_slot2";
    private static final String KEY_MIN_NUMBER_SLOT3 = "min_number_slot3";
    
    private GeminiPhone mGeminiPhone = null;
    private static final String CDMA = "CDMA";
    private PreferenceScreen mParent;
    private Preference mRemovablePref;

    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.device_info_imei_info_gemini);
        
        mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
        
        setSlotStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    private void setSlotStatus() {
        mParent = getPreferenceScreen();
        // slot1: if it is not CDMA phone, deal with imei and imei sv, otherwise
        // deal with the min, prl version and meid info
        // NOTE "imei" is the "Device ID" since it represents the IMEI in GSM
        // and the MEID in CDMA
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            showPreference(PhoneConstants.GEMINI_SIM_1, KEY_IMEI_SLOT1, KEY_IMEI_SV_SLOT1,
                       KEY_MEID_NUMBER_SLOT1, KEY_MIN_NUMBER_SLOT1, KEY_PRL_VERSION_SLOT1);
            showPreference(PhoneConstants.GEMINI_SIM_2, KEY_IMEI_SLOT2, KEY_IMEI_SV_SLOT2,
                       KEY_MEID_NUMBER_SLOT2, KEY_MIN_NUMBER_SLOT2, KEY_PRL_VERSION_SLOT2);

            if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT) {
                showPreference(PhoneConstants.GEMINI_SIM_3, KEY_IMEI_SLOT3, KEY_IMEI_SV_SLOT3,
                        KEY_MEID_NUMBER_SLOT3, KEY_MIN_NUMBER_SLOT3, KEY_PRL_VERSION_SLOT3);
                Xlog.d(TAG, "MTK_GEMINI_3SIM_SUPPORT is true");
            }  else {
                removePreference(KEY_IMEI_SLOT3, KEY_IMEI_SV_SLOT3,
                        KEY_MEID_NUMBER_SLOT3, KEY_MIN_NUMBER_SLOT3,
                        KEY_PRL_VERSION_SLOT3);
                Xlog.d(TAG, "MTK_GEMINI_3SIM_SUPPORT is false");
            }
        }

    }

    private void showPreference(int slotId, String imeiKey, String imeiSvKey,
                       String meidNumberKey , String minNumberKey, String prlVersionKey) {
        if (mGeminiPhone.getPhoneNameGemini(slotId).equals(CDMA)) {
            setSummaryText(meidNumberKey, mGeminiPhone.getMeidGemini(slotId));
            setSummaryText(minNumberKey, mGeminiPhone.getCdmaMinGemini(slotId));
            setSummaryText(prlVersionKey, mGeminiPhone.getCdmaPrlVersionGemini(slotId));

            // device is not GSM/UMTS, do not display GSM/UMTS features
            // check Null in case no specified preference in overlay xml
            mRemovablePref = mParent.findPreference(imeiKey);
            mParent.removePreference(mRemovablePref);
            mRemovablePref = mParent.findPreference(imeiSvKey);
            mParent.removePreference(mRemovablePref);
        } else {
            setSummaryText(imeiKey, mGeminiPhone.getDeviceIdGemini(slotId));
            setSummaryText(imeiSvKey, mGeminiPhone.getDeviceSvnGemini(slotId));
            // device is not CDMA, do not display CDMA features
            // check Null in case no specified preference in overlay xml
            mRemovablePref = mParent.findPreference(prlVersionKey);
            mParent.removePreference(mRemovablePref);
            mRemovablePref = mParent.findPreference(meidNumberKey);
            mParent.removePreference(mRemovablePref);
            mRemovablePref = mParent.findPreference(minNumberKey);
            mParent.removePreference(mRemovablePref);
        }
    }
 

    private void removePreference(String imeiKey, String imeiSvKey,
            String meidNumberKey, String minNumberKey, String prlVersionKey) {
        mRemovablePref = mParent.findPreference(imeiKey);
        mParent.removePreference(mRemovablePref);
        mRemovablePref = mParent.findPreference(imeiSvKey);
        mParent.removePreference(mRemovablePref);

        mRemovablePref = mParent.findPreference(prlVersionKey);
        mParent.removePreference(mRemovablePref);
        mRemovablePref = mParent.findPreference(meidNumberKey);
        mParent.removePreference(mRemovablePref);
        mRemovablePref = mParent.findPreference(minNumberKey);
        mParent.removePreference(mRemovablePref);
    }

    private void setSummaryText(String preference, String text) {
        Preference p = mParent.findPreference(preference);
    
        if (TextUtils.isEmpty(text)) {
            p.setSummary(getResources().getString(R.string.device_info_default));
        } else {
            p.setSummary(text);
        }     
    }

}
