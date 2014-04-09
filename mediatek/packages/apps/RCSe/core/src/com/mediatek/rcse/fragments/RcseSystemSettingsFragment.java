/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcse.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.api.RegistrationApi;
import com.mediatek.rcse.service.ApiManager;
import com.mediatek.rcse.service.ApiManager.RcseComponentController;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.LauncherUtils;

/*
 * Displays RCS-e settings UI. It will be set up to a activity.
 */
public class RcseSystemSettingsFragment extends PreferenceFragment {

    private static final String TAG = "RcseSystemSettingsFragment";
    private CheckBoxPreference mRcseServiceActivation;
    private CheckBoxPreference mRcseRoming;
    private static final String RCS_ACTIVITATION = "rcs_activation";
    private static final String RCS_ROMING = "rcs_roaming";
    private static final String MESSAGE_RCS_ID = "messageResId";
    private static final String ACTION_PROVISION_PROFILE =
            "com.mediatek.rcse.action.PROVISION_PROFILE_SETTING";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG, "onCreateView() entry");
        addPreferencesFromResource(R.xml.rcse_system_settings);
        setupDebugItems(Logger.IS_DEBUG);
        mRcseServiceActivation = (CheckBoxPreference) findPreference(RCS_ACTIVITATION);
        mRcseServiceActivation.setTitle(R.string.rcs_settings_label_rcs_service);
        mRcseRoming = (CheckBoxPreference) findPreference(RCS_ROMING);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        new GetServiceStatusTask().execute();
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mRcseServiceActivation) {
            if (mRcseServiceActivation != null && !mRcseServiceActivation.isChecked()) {
                showAttentionDialog(R.string.rcse_settings_disable_rcse_service);
            } else if (mRcseServiceActivation != null && mRcseServiceActivation.isChecked()) {
                showAttentionDialog(R.string.rcse_settings_enable_rcse_service);
            }
        } else if (preference == mRcseRoming) {
            if (mRcseRoming != null && !mRcseRoming.isChecked()) {
                showAttentionDialog(R.string.rcse_settings_disable_rcse_service_roaming);
            } else if (mRcseRoming != null && mRcseRoming.isChecked()) {
                showAttentionDialog(R.string.rcse_settings_enable_rcse_service_roaming);
            }
        } else {
            Logger.w(TAG, "Unknown check preference clicked");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void showAttentionDialog(int messageResId) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(MESSAGE_RCS_ID, messageResId);
        confirmDialog.setArguments(bundle);

        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(confirmDialog, ConfirmDialog.TAG);
        transaction.commitAllowingStateLoss();
    }

    private void enableRcsService() {
        Logger.v(TAG, "enableRcsService()");
        Activity activity = getActivity();
        if (activity != null) {
            LauncherUtils.launchRcsService(activity.getApplicationContext(), false);
        } else {
            Logger.w(TAG, "activity is null, dialog become null.");
        }
    }

    private void disableRcsService() {
        Logger.v(TAG, "disableRcsService()");
        Activity activity = getActivity();
        if (activity != null) {
            LauncherUtils.stopRcsService(getActivity().getApplicationContext());
        } else {
            Logger.w(TAG, "activity is null, dialog become null.");
        }
    }

    /**
     * Start service thread
     */
    private class StartServiceTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            setServiceActivatedEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Logger.d(TAG, "StartServiceTask doInBackground() enable ChatMainActivity");

            RcseComponentController rcseComponentController =
                    ApiManager.getInstance().getRcseComponentController();
            if (rcseComponentController != null) {
                rcseComponentController.onServiceActiveStatusChanged(true);
            } else {
                Logger.e(TAG, "StartServiceTask doInBackground()" +
                        " ApiManager.getInstance().getRcseComponentController() is null");
            }

            Activity activity = getActivity();
            if (activity != null) {
                Logger.v(TAG, "Enable rcse service.");
                RegistrationApi.setServiceActivationState(true);
                enableRcsService();
            } else {
                Logger.w(TAG, "activity is null, dialog become null.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            setServiceActivatedEnabled(true);
            setServiceActivatedStatus(true);
            setRoamingAuthorizedEnabled(true);
        }
    }

    /**
     * Stop service thread
     */
    private class StopServiceTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "StopServiceTask";

        @Override
        protected void onPreExecute() {
            setServiceActivatedEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Activity activity = getActivity();
            if (activity != null) {
                RegistrationApi.setServiceActivationState(false);
                disableRcsService();
                boolean isRoamingAuthorized = RcsSettings.getInstance().isRoamingAuthorized();
                if (isRoamingAuthorized) {
                    Logger
                            .d(TAG,
                                    "doInBackground() isRoamingAuthorized is true and we need to make it false");
                    RcsSettings.getInstance().setRoamingAuthorizationState(false);
                } else {
                    Logger.d(TAG,
                            "doInBackground() isRoamingAuthorized is false and we need to nothing");
                }
                Logger.d(TAG, "StopServiceTask doInBackground() disable ChatMainActivity");

                RcseComponentController rcseComponentController =
                        ApiManager.getInstance().getRcseComponentController();
                if (rcseComponentController != null) {
                    rcseComponentController.onServiceActiveStatusChanged(false);
                } else {
                    Logger.e(TAG, "StopServiceTask doInBackground()" +
                            "ApiManager.getInstance().getRcseComponentController() is null");
                }
            } else {
                Logger.w(TAG, "activity is null, dialog become null.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            setServiceActivatedEnabled(true);
            setServiceActivatedStatus(false);
            setRoamingAuthorizedEnabled(false);
            setRoamingAuthorizedStatus(false);
        }
    }

    /**
     * This task is used to set up the value of roaming authorized status
     */
    private class RoamingAuthorizeTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "RoamingAuthorizeTask";

        private boolean mIsAuthorized = false;

        private RoamingAuthorizeTask(boolean isAuthorized) {
            mIsAuthorized = isAuthorized;
        }

        @Override
        protected void onPreExecute() {
            if (mRcseRoming != null) {
                mRcseRoming.setEnabled(false);
            } else {
                Logger.w(TAG, "onPreExecute() mRcseRoming is null");
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            Activity activity = getActivity();
            if (activity != null) {
                RcsSettings.getInstance().setRoamingAuthorizationState(mIsAuthorized);
            } else {
                Logger.w(TAG, "activity is null, dialog become null.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mRcseRoming != null) {
                setRoamingAuthorizedStatus(mIsAuthorized);
                mRcseRoming.setEnabled(true);
            } else {
                Logger.w(TAG, "mRcseServiceActivation is null");
            }
        }
    }

    private class GetServiceStatusTask extends AsyncTask<Void, Void, boolean[]> {

        private static final String TAG = "GetServiceStatusTask";
        private static final int INDEX_SERVICE_ACTIVATED = 0;
        private static final int INDEX_ROAMING_AUTHORIZED = 1;
        private static final int RESULTS_COUNT = 2;

        @Override
        protected boolean[] doInBackground(Void... params) {
            boolean isServiceActivated = RegistrationApi.isServiceActivated();
            boolean isRoamingAuthorized = RcsSettings.getInstance().isRoamingAuthorized();
            boolean[] results = new boolean[RESULTS_COUNT];
            if (!isServiceActivated && isRoamingAuthorized) {
                Logger
                        .w(TAG,
                                "doInBackground() isServiceActivated is false but isRoamingAuthorized is false");
                RcsSettings.getInstance().setRoamingAuthorizationState(false);
                isRoamingAuthorized = false;
            }
            results[INDEX_SERVICE_ACTIVATED] = isServiceActivated;
            results[INDEX_ROAMING_AUTHORIZED] = isRoamingAuthorized;

            RcseComponentController rcseComponentController =
                    ApiManager.getInstance().getRcseComponentController();
            if (rcseComponentController != null) {
                rcseComponentController.onServiceActiveStatusChanged(isServiceActivated);
            } else {
                Logger.e(TAG, "StopServiceTask doInBackground()" +
                        " ApiManager.getInstance().getRcseComponentController() is null");
            }
            return results;
        }

        @Override
        protected void onPostExecute(boolean[] results) {
            boolean isServiceActivated = results[INDEX_SERVICE_ACTIVATED];
            setServiceActivatedStatus(isServiceActivated);

            // Only when RCS-e service is activated can user modify the roaming
            // authorization status
            setRoamingAuthorizedEnabled(isServiceActivated);

            setRoamingAuthorizedStatus(results[INDEX_ROAMING_AUTHORIZED]);
        }
    }

    private void setServiceActivatedStatus(boolean result) {
        Logger.d(TAG, "setServiceActivatedStatus() result is " + result);
        if (mRcseServiceActivation != null) {
            mRcseServiceActivation.setChecked(result);
            if (result) {
                mRcseServiceActivation.setSummary(R.string.rcs_settings_summary_rcs_service_on);
            } else {
                mRcseServiceActivation.setSummary(R.string.rcs_settings_summary_rcs_service_off);
            }
        } else {
            Logger.w(TAG, "setServiceActivatedStatus() mRcseServiceActivation is null");
        }
    }

    private void setServiceActivatedEnabled(boolean isEnabled) {
        Logger.d(TAG, "setServiceActivatedStatus() result is " + isEnabled);
        if (mRcseServiceActivation != null) {
            mRcseServiceActivation.setEnabled(isEnabled);
        } else {
            Logger.w(TAG, "setServiceActivatedStatus() mRcseServiceActivation is null");
        }
    }

    private void setRoamingAuthorizedStatus(boolean result) {
        Logger.d(TAG, "setRoamingAuthorizedStatus() result is " + result);
        if (mRcseRoming != null) {
            mRcseRoming.setChecked(result);
        } else {
            Logger.w(TAG, "setRoamingAuthorizedStatus() mRcseRoming is null");
        }
    }

    private void setRoamingAuthorizedEnabled(boolean isEnabled) {
        Logger.d(TAG, "setRoamingAuthorizedEnabled() result is " + isEnabled);
        if (mRcseRoming != null) {
            mRcseRoming.setEnabled(isEnabled);
        } else {
            Logger.w(TAG, "setRoamingAuthorizedEnabled() mRcseRoming is null");
        }
    }

    private class ConfirmDialog extends DialogFragment implements DialogInterface.OnClickListener {
        private static final String TAG = "ConfirmDialog";
        private int mMessageResId;

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            switch (mMessageResId) {
                case R.string.rcse_settings_enable_rcse_service:
                case R.string.rcse_settings_disable_rcse_service:
                    if (mRcseServiceActivation != null) {
                        mRcseServiceActivation.setChecked(!mRcseServiceActivation.isChecked());
                    } else {
                        Log.e(TAG, "onCancel() mRcseServiceActivation is null");
                    }
                    break;
                case R.string.rcse_settings_enable_rcse_service_roaming:
                case R.string.rcse_settings_disable_rcse_service_roaming:
                    if (mRcseRoming != null) {
                        mRcseRoming.setChecked(!mRcseRoming.isChecked());
                    } else {
                        Log.e(TAG, "onCancel() mRcseRoming is null");
                    }
                    break;
                default:
                    break;
            }

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();
            mMessageResId = getArguments().getInt(MESSAGE_RCS_ID);
            if (activity != null) {
                return new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                        .setIconAttribute(android.R.attr.alertDialogIcon).setTitle(
                                R.string.attention_title).setMessage(
                                getString(mMessageResId)).setPositiveButton(
                                getString(R.string.rcs_dialog_positive_button), this).setNegativeButton(
                                getString(R.string.rcs_dialog_negative_button), this).create();
            } else {
                Logger.w(TAG, "activity is null, dialog become null.");
                return null;
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                switch (mMessageResId) {
                    case R.string.rcse_settings_enable_rcse_service:
                        Logger.v(TAG, "enableRcsService task");
                        new StartServiceTask().execute();
                        break;
                    case R.string.rcse_settings_disable_rcse_service:
                        Logger.v(TAG, "disbleRcsService task");
                        new StopServiceTask().execute();
                        break;
                    case R.string.rcse_settings_enable_rcse_service_roaming:
                        Logger.v(TAG, "enbleRcsService roaming");
                        new RoamingAuthorizeTask(true).execute();
                        break;
                    case R.string.rcse_settings_disable_rcse_service_roaming:
                        Logger.v(TAG, "disbleRcsService roaming");
                        new RoamingAuthorizeTask(false).execute();
                        break;
                    default:
                        Logger.w(TAG, "Unknown op code");
                        break;
                }
            } else {
                if ((mMessageResId == R.string.rcse_settings_enable_rcse_service 
                        || mMessageResId == R.string.rcse_settings_disable_rcse_service)
                        && mRcseServiceActivation != null) {
                    // Cancel enable or disable rcse service
                    mRcseServiceActivation.setChecked(!mRcseServiceActivation.isChecked());
                } else if ((mMessageResId == R.string.rcse_settings_enable_rcse_service_roaming 
                        || mMessageResId == R.string.rcse_settings_disable_rcse_service_roaming)
                        && mRcseRoming != null) {
                    // Cancel enable or disable rcse service roaming
                    mRcseRoming.setChecked(!mRcseRoming.isChecked());
                } else {
                    Logger.w(TAG, "Some error occur when cancel.");
                }
            }
            dismiss();
        }
    }
    
    private void setupDebugItems(final boolean isDebug) {
        Logger.d(TAG, "setupDebugItems() entry with isDebug " + isDebug);
        Resources resources = getResources();
        String profileKey = resources.getString(R.string.rcs_settings_title_provision_settings);
        Preference profilePreference = findPreference(profileKey);
        if (isDebug) {
            Logger.d(TAG, "setupDebugItems() this is a debug version");
            profilePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(ACTION_PROVISION_PROFILE);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(RcseSystemSettingsFragment.this.getActivity(),
                                R.string.rcse_toast_profile_not_exist, Toast.LENGTH_LONG).show();
                        return false;
                    }
                    return true;
                }
            });
        } else {
            Logger.d(TAG, "setupDebugItems() this is a user version, remove some debug items");
            String mapKey = resources.getString(R.string.number_to_account_settings);
            Preference mapPreference = findPreference(mapKey);
            getPreferenceScreen().removePreference(profilePreference);
            getPreferenceScreen().removePreference(mapPreference);
        }
    }
}
