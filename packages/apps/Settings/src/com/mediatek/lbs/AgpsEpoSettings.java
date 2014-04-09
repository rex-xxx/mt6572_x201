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

package com.mediatek.lbs;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.mediatek.common.agps.MtkAgpsManager;
import com.mediatek.common.epo.MtkEpoClientManager;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.Observable;
import java.util.Observer;

public class AgpsEpoSettings extends SettingsPreferenceFragment implements
        CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "AgpsEpoSettings";

    // / M: epo enable and epo settings preference key
    private static final String KEY_EPO_ENABLER = "epo_enabler";
    private static final String KEY_EPO_SETTINGS = "epo_settings";

    // / M: Agps enable and agps settings preference key
    private static final String KEY_AGPS_ENABLER = "agps_enabler";
    private static final String KEY_AGPS_SETTINGS = "agps_settings";

    // / M: epo enable and epo settings preferenc
    private MtkEpoClientManager mEpoMgr;
    private CheckBoxPreference mEpoEnalberPref;
    private Preference mEpoSettingPref;
    private Switch mEnabledSwitch;

    // / M: Agps enable and agps settings preference
    private MtkAgpsManager mAgpsMgr;
    private CheckBoxPreference mAgpsCB;
    private Preference mAgpsPref;

    // / M: Epo and agps confirm dialog id
    private static final int CONFIRM_EPO_DIALOG_ID = 0;
    private static final int CONFIRM_AGPS_DIALOG_ID = 1;

    // These provide support for receiving notification when Location Manager
    // settings change.
    // This is necessary because the Network Location Provider can change
    // settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;
    private Cursor mSettingsCursor;

    private ContentResolver mContentResolver;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();

        mContentResolver = getContentResolver();
        // Make sure we reload the preference hierarchy since some of these
        // settings
        // depend on others...
        createPreferenceHierarchy();
        if(!(("tablet".equals(SystemProperties.get("ro.build.characteristics"))) &&
	   (getResources().getBoolean(com.android.internal.R.bool.preferences_prefer_dual_pane))))
        {
            activity.getActionBar().setTitle(R.string.location_gps);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);

        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                mContentResolver, LocationManager.GPS_PROVIDER);
        mEnabledSwitch.setChecked(gpsEnabled);

        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPadding(0, 0, padding, 0);
        mEnabledSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(
                mEnabledSwitch,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));

        // listen for Location Manager settings changes
        mSettingsCursor = mContentResolver.query(
                Settings.Secure.CONTENT_URI, null, "(" + Settings.System.NAME
                        + "=?)",
                new String[] { Settings.Secure.LOCATION_PROVIDERS_ALLOWED },
                null);
        mContentQueryMap = new ContentQueryMap(mSettingsCursor,
                Settings.System.NAME, true, null);

    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);

        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
        if (mSettingsCursor != null) {
            mSettingsCursor.close();
        }
    }

    private void createPreferenceHierarchy() {

        addPreferencesFromResource(R.xml.agps_epo_settings);

        mEpoEnalberPref = (CheckBoxPreference) findPreference(KEY_EPO_ENABLER);
        mEpoSettingPref = findPreference(KEY_EPO_SETTINGS);

        mEpoMgr = (MtkEpoClientManager) getSystemService(Context.MTK_EPO_CLIENT_SERVICE);

        // / M: If not support Agps, remove Agps related preference @{
        if (FeatureOption.MTK_AGPS_APP
                && (FeatureOption.MTK_GPS_SUPPORT || FeatureOption.MTK_EMULATOR_SUPPORT)) {
            mAgpsMgr = (MtkAgpsManager) getSystemService(Context.MTK_AGPS_SERVICE);
        }
        mAgpsPref = findPreference(KEY_AGPS_SETTINGS);
        mAgpsCB = (CheckBoxPreference) findPreference(KEY_AGPS_ENABLER);

        if ((!FeatureOption.MTK_GPS_SUPPORT && !FeatureOption.MTK_EMULATOR_SUPPORT)
                || !FeatureOption.MTK_AGPS_APP) {
            if (mAgpsPref != null) {
                getPreferenceScreen().removePreference(mAgpsPref);
            }
            if (mAgpsCB != null) {
                getPreferenceScreen().removePreference(mAgpsCB);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateLocationToggles();

        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    updateLocationToggles();
                }
            };
        }

        mContentQueryMap.addObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        if (preference == mEpoEnalberPref) {
            boolean flag = mEpoEnalberPref.isChecked();
            if (flag) {
                mEpoEnalberPref.setChecked(false);
                showDialog(CONFIRM_EPO_DIALOG_ID);
            } else {
                mEpoMgr.disable();
            }
        } else if (preference == mEpoSettingPref) {
            ((PreferenceActivity) getActivity()).startPreferencePanel(
                    EPOSettings.class.getName(), null,
                    R.string.epo_entrance_title, null, null, 0);
        } else if (preference == mAgpsCB) {
            if (mAgpsCB.isChecked()) {
                mAgpsCB.setChecked(false);
                showDialog(CONFIRM_AGPS_DIALOG_ID);
            } else {
                mAgpsMgr.disable();
            }
        } else if (preference == mAgpsPref) {
            ((PreferenceActivity) getActivity()).startPreferencePanel(
                    AgpsSettings.class.getName(), null,
                    R.string.agps_settings_title, null, null, 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Xlog.d(TAG,
                "[f] onCheckedChanged " + isChecked + "; "
                        + buttonView.isChecked());
        if (buttonView == mEnabledSwitch) {
            Xlog.d(TAG, "[f] onCheckedChanged buttonview is mEnableSwitch");
            Settings.Secure.setLocationProviderEnabled(mContentResolver,
                    LocationManager.GPS_PROVIDER, isChecked);

        }
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateLocationToggles() {

        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                mContentResolver, LocationManager.GPS_PROVIDER);
        mEnabledSwitch.setChecked(gpsEnabled);

        if (mAgpsCB != null && mAgpsMgr != null) {
            mAgpsCB.setChecked(mAgpsMgr.getStatus());
            mAgpsCB.setEnabled(mEnabledSwitch.isChecked());
        }

        if (mEpoMgr != null) {
            mEpoEnalberPref.setChecked(mEpoMgr.getStatus());
            mEpoEnalberPref.setEnabled(mEnabledSwitch.isChecked());
        }
    }

    public Dialog onCreateDialog(int id) {

        Dialog dialog = null;
        switch (id) {
        case CONFIRM_EPO_DIALOG_ID:
            dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.epo_enable_confirm_title)
                    .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.epo_enable_confirm_message)
                    .setPositiveButton(R.string.epo_enable_confirm_allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    mEpoEnalberPref.setChecked(true);
                                    mEpoMgr.enable();
                                }
                            })
                    .setNegativeButton(R.string.epo_enable_confirm_deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    mEpoEnalberPref.setChecked(false);
                                    Xlog.i(TAG, "User Deny Enbale EPO Service");
                                }
                            }).create();
            break;
        case CONFIRM_AGPS_DIALOG_ID:
            dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.agps_enable_confirm_title)
                    .setMessage(R.string.agps_enable_confirm)
                    .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.agps_enable_confirm_allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    mAgpsCB.setChecked(true);
                                    mAgpsMgr.enable();
                                }
                            })
                    .setNegativeButton(R.string.agps_enable_confirm_deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    // / M: see cr [alps00339992] enabe a-gps
                                    // should be disabeld when you tap cancel
                                    Xlog.d(TAG, "-->mAgpsMgr.getStatus()"
                                            + mAgpsMgr.getStatus());
                                    if (!mAgpsMgr.getStatus()) {
                                        mAgpsCB.setChecked(false);
                                    }
                                    Xlog.i(TAG, "DenyDenyDeny");
                                }
                            }).create();
            break;
        default:
            break;
        }
        return dialog;
    }

}
