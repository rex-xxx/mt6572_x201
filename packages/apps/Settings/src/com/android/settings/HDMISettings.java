package com.android.settings;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.hdmi.HDMILocalService;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class HDMISettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    // private static final String TAG = "HDMISettings";
    private static final String TAG = "hdmi";
    private static final String KEY_TOGGLE_HDMI = "hdmi_toggler";
    private static final String KEY_TOGGLE_AUDIO = "audio_toggler";
    private static final String KEY_TOGGLE_VIDEO = "video_toggler";
    private static final String KEY_VIDEO_RESOLUTION = "video_resolution";
    private static final String KEY_VIDEO_RESOLUTION_8193 = "video_resolution_8193";
    private static final int HDMI_VIDEO_RESOLUTION_DEFAULT = 2;//8193 720p_60Hz
    private CheckBoxPreference mToggleHDMIPref;
    private CheckBoxPreference mToggleAudioPref;
    private CheckBoxPreference mToggleVideoPref;
    private ListPreference mVideoResolutionPref;
    private ListPreference mVideoResolutionPrefTemp;
    
    private HDMILocalService mHDMIService = null;
    private boolean mIsHDMIEnabled = false;

    /**
     * Service connection observer for HDMI service
     */
    private ServiceConnection mHDMIServiceConn = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName name) {
            mHDMIService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mHDMIService = ((HDMILocalService.LocalBinder) service)
                    .getService();
            Xlog.w(TAG, "HDMISettings, HDMILocalService is connected");
            updateSettingsItemEnableStatus();
            updateSelectedResolution();            
        }
    };

    BroadcastReceiver mLocalServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (HDMILocalService.ACTION_CABLE_STATE_CHANGED.equals(action)) {
                updateSettingsItemEnableStatus();
            }
            if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
                if (HDMILocalService.ACTION_EDID_UPDATED.equals(action)) {
                    updatePref();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Xlog.i(TAG, ">>HDMISettings.onCreate()");
        addPreferencesFromResource(R.xml.hdmi_settings);
        mToggleHDMIPref = (CheckBoxPreference) findPreference(KEY_TOGGLE_HDMI);
        mToggleHDMIPref.setOnPreferenceChangeListener(this);
        mToggleAudioPref = (CheckBoxPreference) findPreference(KEY_TOGGLE_AUDIO);
        mToggleAudioPref.setOnPreferenceChangeListener(this);
        mToggleVideoPref = (CheckBoxPreference) findPreference(KEY_TOGGLE_VIDEO);
        mToggleVideoPref.setOnPreferenceChangeListener(this);        

        if (!FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            Xlog.e(TAG, "It is not MT8193");
            mVideoResolutionPref = (ListPreference) findPreference(KEY_VIDEO_RESOLUTION);
            mVideoResolutionPref.setOnPreferenceChangeListener(this);
            mVideoResolutionPrefTemp = (ListPreference) findPreference(KEY_VIDEO_RESOLUTION_8193);
            getPreferenceScreen().removePreference(mVideoResolutionPrefTemp);
        } else {
            Xlog.e(TAG, "It is MT8193");
            mVideoResolutionPref = (ListPreference) findPreference(KEY_VIDEO_RESOLUTION_8193);
            mVideoResolutionPref.setOnPreferenceChangeListener(this);
            mVideoResolutionPrefTemp = (ListPreference) findPreference(KEY_VIDEO_RESOLUTION);
            getPreferenceScreen().removePreference(mVideoResolutionPrefTemp);
        }

        boolean bindHdmiServiceFlag = getActivity().bindService(
                new Intent(getActivity(), HDMILocalService.class),
                mHDMIServiceConn, Context.BIND_AUTO_CREATE);
        if (!bindHdmiServiceFlag) {
            Xlog.e(TAG, "HDMISettings fail to bind HDMI service");
            mToggleHDMIPref.setEnabled(false);
            mToggleAudioPref.setEnabled(false);
            mToggleVideoPref.setEnabled(false);
            mVideoResolutionPref.setEnabled(false);
        }

        if (mToggleAudioPref != null) {
            getPreferenceScreen().removePreference(mToggleAudioPref);
        }
        if (mToggleVideoPref != null) {
            getPreferenceScreen().removePreference(mToggleVideoPref);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(HDMILocalService.ACTION_CABLE_STATE_CHANGED);
        getActivity().registerReceiver(mLocalServiceReceiver, filter);
    }

    @Override
    public void onResume() {
        updatePref();
        updateSettingsItemEnableStatus();
        super.onResume();
    }

    private void updatePref() {

        mIsHDMIEnabled = Settings.System.getInt(getContentResolver(),
                HDMILocalService.KEY_HDMI_ENABLE_STATUS, 1) == 1;
        boolean isAudioEnabled = Settings.System.getInt(getContentResolver(),
                HDMILocalService.KEY_HDMI_AUDIO_STATUS, 1) == 1;
        boolean isVideoEnabled = Settings.System.getInt(getContentResolver(),
                HDMILocalService.KEY_HDMI_VIDEO_STATUS, 0) == 1;
        mToggleHDMIPref.setChecked(mIsHDMIEnabled);
        if (mToggleAudioPref != null) {
            mToggleAudioPref.setChecked(isAudioEnabled);
        }
        if (mToggleVideoPref != null) {
            mToggleVideoPref.setChecked(isVideoEnabled);
        }
        updateSelectedResolution();        
    }

    /**
     * Update HDMI resolution selection Operation in this method will depend on
     * HDMILocalService
     */
    private void updateSelectedResolution() {
        Xlog.i(TAG, "HDMISettings>>updateSelectedResolution()");
        if (mHDMIService == null) {
            Xlog
                    .e(
                            TAG,
                            "HDMISettings>>updateSelectedResolution(), service have not been connected, wait");
            return;
        }
        String videoResolution = Settings.System.getString(
                getContentResolver(),
                HDMILocalService.KEY_HDMI_VIDEO_RESOLUTION);

        if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            if (videoResolution != null) {
                if ("102".equals(videoResolution) || "103".equals(videoResolution)) {
                    videoResolution = "100";
                }
            }
        }
            
        CharSequence[] resolutionValues = mVideoResolutionPref.getEntryValues();
        CharSequence[] resolutionEntries = null;
        List<CharSequence> realResolution = null;
        List<CharSequence> realResolutionValues = null;
        
        if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            resolutionEntries = mVideoResolutionPref.getEntries();
            realResolution = new ArrayList<CharSequence>();
            realResolutionValues = new ArrayList<CharSequence>();
            realResolution.add(resolutionEntries[0]);
            realResolutionValues.add(resolutionValues[0]);

            if (mHDMIService.sEdid != null) {
                //NTSC 720P 60Hz
                if (1 == ((mHDMIService.sEdid[0] >> 1) & 0x01)) {
                    realResolution.add(resolutionEntries[1]);
                    realResolutionValues.add(resolutionValues[1]);
                }

                //PAL 720P 50Hz
                if (1 == ((mHDMIService.sEdid[1] >> 11) & 0x01)) {
                    realResolution.add(resolutionEntries[2]);
                    realResolutionValues.add(resolutionValues[2]);
                }
                
                //NTSC 1080P 23.976Hz or PAL 1080P 24Hz
                if ((1 == ((mHDMIService.sEdid[0] >> 21) & 0x01))
                        || (1 == ((mHDMIService.sEdid[1] >> 20) & 0x01))) {
                    realResolution.add(resolutionEntries[3]);
                    realResolutionValues.add(resolutionValues[3]);
                }
            }

            resolutionEntries = realResolution.toArray(new CharSequence[realResolution.size()]);
            resolutionValues = realResolutionValues.toArray(new CharSequence[realResolutionValues.size()]);
            mVideoResolutionPref.setEntries(resolutionEntries);
            mVideoResolutionPref.setEntryValues(resolutionValues);
        }
        
        int selectIndex = -1;
        for (int i = 0; i < resolutionValues.length; i++) {
            if (resolutionValues[i].toString().equals(videoResolution)) {
                selectIndex = i;
                break;
            }
        }
        if (selectIndex != -1) {
            mVideoResolutionPref.setValueIndex(selectIndex);
        } else {
            Xlog
                    .i(TAG,
                            " set HDMI video resolution to default value, the first one");
            mVideoResolutionPref.setValueIndex(0);
            if (mHDMIService != null) {
                mHDMIService.setVideoResolution(Integer
                        .parseInt(resolutionValues[0].toString()));
            }
        }

    }
    
    @Override
    public void onDestroy() {
        getActivity().unbindService(mHDMIServiceConn);
        getActivity().unregisterReceiver(mLocalServiceReceiver);
        super.onDestroy();
    };

    /**
     * Each settings item's enabled status will depend on HDMI cable plug status
     */
    private void updateSettingsItemEnableStatus() {
        Xlog.i(TAG, "HDMISettings>>updateSettingsItemEnableStatus()");
        if (mHDMIService == null) {
            Xlog.i(TAG, "HDMI service has not connected, wait");
            return;
        }
        boolean isHDMICablePluged = mHDMIService.isCablePluged();
        mIsHDMIEnabled = Settings.System.getInt(getContentResolver(),
                HDMILocalService.KEY_HDMI_ENABLE_STATUS, 1) == 1;
        boolean shouldEnable = isHDMICablePluged && mIsHDMIEnabled;
        Xlog.d(TAG, "Is cable pluged?" + isHDMICablePluged + ", isHDMIEnabled?"
                + mIsHDMIEnabled);
        if (mToggleAudioPref != null) {
            mToggleAudioPref.setEnabled(shouldEnable);
        }
        if (mToggleVideoPref != null) {
            mToggleVideoPref.setEnabled(shouldEnable);
        }
        mVideoResolutionPref.setEnabled(shouldEnable);
    }    

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();        
        if (mHDMIService == null) {
            Xlog
                    .e(TAG,
                            "HDMISettings  -- Connection to HDMI local service have not been established.");
            return false;
        }
        if (KEY_TOGGLE_HDMI.equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            mHDMIService.enableHDMI(checked);
            Settings.System.putInt(getContentResolver(),
                    HDMILocalService.KEY_HDMI_ENABLE_STATUS, checked ? 1 : 0);
            updateSettingsItemEnableStatus();
        } else if (KEY_TOGGLE_AUDIO.equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            mHDMIService.enableAudio(checked);
            Settings.System.putInt(getContentResolver(),
                    HDMILocalService.KEY_HDMI_AUDIO_STATUS, checked ? 1 : 0);
        } else if (KEY_TOGGLE_VIDEO.equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            mHDMIService.enableVideo(checked);
            Settings.System.putInt(getContentResolver(),
                    HDMILocalService.KEY_HDMI_VIDEO_STATUS, checked ? 1 : 0);
        } else if (KEY_VIDEO_RESOLUTION.equals(key)) {
            Xlog.e(TAG, "HDMISettings  -- key: " + key + " newValue: " + (String)newValue);
            String newResolution = (String) newValue;
            mHDMIService.setVideoResolution(Integer.parseInt(newResolution));
            Settings.System.putString(getContentResolver(),
                    HDMILocalService.KEY_HDMI_VIDEO_RESOLUTION, newResolution);
        } else if (KEY_VIDEO_RESOLUTION_8193.equals(key)) {
            Xlog.e(TAG, "HDMISettings  -- key: " + key + " newValue: " + (String)newValue);
            String newResolution = (String) newValue;

            if (newResolution.equals("100")) { //Auto
                if (mHDMIService.sEdid != null) {                    
                    //Priority: 720p60 > 720p50 > 480p > 576p
                    if (1 == ((mHDMIService.sEdid[0] >> 1) & 0x01)) {
                        //NTSC 720P 60Hz (UI shows Auto)
                        newResolution = "102";
                    } else if (1 == ((mHDMIService.sEdid[1] >> 11) & 0x01)) {
                        //PAL 720P 50Hz (UI shows Auto)
                        newResolution = "103";
                    } else if (1 == (mHDMIService.sEdid[0] & 0x01)) {
                        //NTSC 480P (UI shows Auto)
                        newResolution = "100";
                    } else if (1 == ((mHDMIService.sEdid[1] >> 10) & 0x01)) {
                        //PAL 576P (UI shows Auto)
                        newResolution = "101";
                    }
                }
            } else if (newResolution.equals("8")) { //1080P
                //NTSC 1080P 23.976Hz
                if (1 == ((mHDMIService.sEdid[0] >> 21) & 0x01)) {
                    newResolution = "9";
                }
                //PAL 1080P 24Hz
                if (1 == ((mHDMIService.sEdid[1] >> 20) & 0x01)) {
                    newResolution = "8";
                }
            }
            
            Xlog.e(TAG, "Final resolution: " + newResolution);
            mHDMIService.setVideoResolution(Integer.parseInt(newResolution));
            Settings.System.putString(getContentResolver(),
                    HDMILocalService.KEY_HDMI_VIDEO_RESOLUTION, newResolution);
        }
        return true;
    }

}
