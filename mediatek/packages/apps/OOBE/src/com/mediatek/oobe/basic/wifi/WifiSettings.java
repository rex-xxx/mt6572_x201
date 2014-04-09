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

/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mediatek.oobe.basic.wifi;

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.oobe.ext.IWifiSettingsExt;
import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.ButtonPreference;
import com.mediatek.oobe.utils.SettingsPreferenceFragment;
import com.mediatek.oobe.utils.Utils;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two types of UI are provided here.
 * 
 * The first is for "usual Settings", appearing as any other Setup fragment.
 * 
 * The second is for Setup Wizard, with a simplified interface that hides the action bar and menus.
 */
public class WifiSettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener {
    private static final String TAG = "WifiSettings";
    // private static final int MENU_ID_WPS_PBC = Menu.FIRST;
    // private static final int MENU_ID_WPS_PIN = Menu.FIRST + 1;
    // private static final int MENU_ID_P2P = Menu.FIRST + 2;
    private static final int MENU_ID_ADD_NETWORK = Menu.FIRST + 3;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 4;
    private static final int MENU_ID_SCAN = Menu.FIRST + 5;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;

    private static final int WIFI_DIALOG_ID = 1;
    // private static final int WPS_PBC_DIALOG_ID = 2;
    // private static final int WPS_PIN_DIALOG_ID = 3;
    private static final int WIFI_SKIPPED_DIALOG_ID = 4;
    private static final int WIFI_AND_MOBILE_SKIPPED_DIALOG_ID = 5;

    // Combo scans can take 5-6s to complete - set to 10s.
    private static final int WIFI_RESCAN_INTERVAL_MS = 6 * 1000;

    // Instance state keys
    private static final String SAVE_DIALOG_EDIT_MODE = "edit_mode";
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;
    private TelephonyManagerEx mTelephonyManagerEx;

    private WifiManager mWifiManager;
    private WifiManager.ActionListener mConnectListener;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager.ActionListener mForgetListener;
    // private boolean mP2pSupported;

    private WifiEnabler mWifiEnabler;
    // An access point being editted is stored here.
    private AccessPoint mSelectedAccessPoint;

    private DetailedState mLastState;
    private WifiInfo mLastInfo;

    private AtomicBoolean mConnected = new AtomicBoolean(false);

    private int mKeyStoreNetworkId = INVALID_NETWORK_ID;

    private WifiDialog mDialog;

    private TextView mEmptyView;
    /** M: mtk54279 @{ **/
    private Preference mWifiEnablerPref;
    private ButtonPreference mAddNetworkButton;
    private PreferenceCategory mWifiApCategory;
    private IWifiSettingsExt mExt;
    /** @} */
    /* Used in Wifi Setup context */

    // this boolean extra specifies whether to disable the Next button when not connected
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";
    private static final String WIFI_SWITCH_ENABLER = "wifi_switch";
    // this boolean extra specifies whether to auto finish when connection is established
    private static final String EXTRA_AUTO_FINISH_ON_CONNECT = "wifi_auto_finish_on_connect";

    // this boolean extra shows a custom button that we can control
    protected static final String EXTRA_SHOW_CUSTOM_BUTTON = "wifi_show_custom_button";

    // show a text regarding data charges when wifi connection is required during setup wizard
    protected static final String EXTRA_SHOW_WIFI_REQUIRED_INFO = "wifi_show_wifi_required_info";

    // this boolean extra is set if we are being invoked by the Setup Wizard
    private static final String EXTRA_IS_FIRST_RUN = "firstRun";

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;

    // should activity finish once we have a connection?
    private boolean mAutoFinishOnConnection;

    // Save the dialog details
    private boolean mDlgEdit;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;

    // the action bar uses a different set of controls for Setup Wizard
    private boolean mSetupWizardMode;

    /* End of "used in Wifi Setup context" */

    public WifiSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        /// M: add no WAPI certification action
        mFilter.addAction(WifiManager.NO_CERTIFICATION_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };

        mScanner = new Scanner();
    }

    @Override
    public void onCreate(Bundle icicle) {
        // Set this flag early, as it's needed by getHelpResource(), which is called by super
        mSetupWizardMode = getActivity().getIntent().getBooleanExtra(EXTRA_IS_FIRST_RUN, false);

        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mSetupWizardMode) {
            View view = inflater.inflate(R.layout.setup_preference, container, false);
            View other = view.findViewById(R.id.other_network);
            other.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mWifiManager.isWifiEnabled()) {
                        onAddNetworkPressed();
                    }
                }
            });

            Intent intent = getActivity().getIntent();
            if (intent.getBooleanExtra(EXTRA_SHOW_CUSTOM_BUTTON, false)) {
                view.findViewById(R.id.button_bar).setVisibility(View.VISIBLE);
                view.findViewById(R.id.back_button).setVisibility(View.INVISIBLE);
                view.findViewById(R.id.skip_button).setVisibility(View.INVISIBLE);
                view.findViewById(R.id.next_button).setVisibility(View.INVISIBLE);

                Button customButton = (Button) view.findViewById(R.id.custom_button);
                customButton.setVisibility(View.VISIBLE);
                customButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isPhone() && !hasSimProblem()) {
                            showDialog(WIFI_SKIPPED_DIALOG_ID);
                        } else {
                            showDialog(WIFI_AND_MOBILE_SKIPPED_DIALOG_ID);
                        }
                    }
                });
            }

            if (intent.getBooleanExtra(EXTRA_SHOW_WIFI_REQUIRED_INFO, false)) {
                view.findViewById(R.id.wifi_required_info).setVisibility(View.VISIBLE);
            }

            return view;
        } else {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        /// M: get telephony manager @{
        mExt = Utils.getWifiSettingsPlugin(getActivity());
        if (FeatureOption.MTK_EAP_SIM_AKA) {
            mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        }
        /// @}

        // mP2pSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
        mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mConnectListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
               Activity activity = getActivity();
               if (activity != null) {
                   Toast.makeText(activity, R.string.wifi_failed_connect_message, Toast.LENGTH_SHORT).show();
               }
            }
        };

        mSaveListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_failed_save_message, Toast.LENGTH_SHORT).show();
                }
            }
        };

        mForgetListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
               Activity activity = getActivity();
               if (activity != null) {
                   Toast.makeText(activity, R.string.wifi_failed_forget_message, Toast.LENGTH_SHORT).show();
               }
            }
        };

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
            mDlgEdit = savedInstanceState.getBoolean(SAVE_DIALOG_EDIT_MODE);
            mAccessPointSavedState = savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
        }

        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();

        // first if we're supposed to finish once we have a connection
        mAutoFinishOnConnection = intent.getBooleanExtra(EXTRA_AUTO_FINISH_ON_CONNECT, false);

        if (mAutoFinishOnConnection) {
            // Hide the next button
            if (hasNextButton()) {
                getNextButton().setVisibility(View.GONE);
            }

            final ConnectivityManager connectivity = (ConnectivityManager) activity.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            if (connectivity != null && connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                activity.setResult(Activity.RESULT_OK);
                activity.finish();
                return;
            }
        }

        // if we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);

        if (mEnableNextOnConnection) {
            if (hasNextButton()) {
                final ConnectivityManager connectivity = (ConnectivityManager) activity.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
                if (connectivity != null) {
                    NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    changeNextButtonState(info.isConnected());
                }
            }
        }

        addPreferencesFromResource(R.xml.wifi_settings);
        /** M: mtk54279 @{ */
        mWifiEnablerPref = getPreferenceScreen().findPreference(WIFI_SWITCH_ENABLER);

        mWifiApCategory = (PreferenceCategory) getPreferenceScreen().findPreference("wifi_aplist");

        mAddNetworkButton = (ButtonPreference) getPreferenceScreen().findPreference("add_network_button");
        mAddNetworkButton.setPrefButtonCallback(mAddNetworkButtonCbk);
        getPreferenceScreen().removePreference(mAddNetworkButton);
        mWifiEnabler = new WifiEnabler(activity, mWifiEnablerPref);
        /** @} */

        if (mSetupWizardMode) {
            getView().setSystemUiVisibility(
                    View.STATUS_BAR_DISABLE_BACK | View.STATUS_BAR_DISABLE_HOME | View.STATUS_BAR_DISABLE_RECENT
                            | View.STATUS_BAR_DISABLE_NOTIFICATION_ALERTS | View.STATUS_BAR_DISABLE_CLOCK);
        }

        // On/off switch is hidden for Setup Wizard
        if (!mSetupWizardMode) {
            Switch actionBarSwitch = new Switch(activity);

            if (activity instanceof PreferenceActivity) {
                PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
                if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                    final int padding = activity.getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
                    actionBarSwitch.setPadding(0, 0, padding, 0);
                    activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
                    activity.getActionBar().setCustomView(
                            actionBarSwitch,
                            new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                                    ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END));
                }
            }
            // / M: mtk54279
            // mWifiEnabler = new WifiEnabler(activity, actionBarSwitch);
        }

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        getListView().setEmptyView(mEmptyView);

        if (!mSetupWizardMode) {
            registerForContextMenu(getListView());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWifiEnabler != null) {
            mWifiEnabler.resume();
        }

        getActivity().registerReceiver(mReceiver, mFilter);
        if (mKeyStoreNetworkId != INVALID_NETWORK_ID && KeyStore.getInstance().state() == KeyStore.State.UNLOCKED) {
            mWifiManager.connect(mKeyStoreNetworkId, mConnectListener);
        }
        mKeyStoreNetworkId = INVALID_NETWORK_ID;

        updateAccessPoints();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
        getActivity().unregisterReceiver(mReceiver);
        mScanner.pause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            outState.putBoolean(SAVE_DIALOG_EDIT_MODE, mDlgEdit);
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
        if (info instanceof AdapterContextMenuInfo) {
            Preference preference = (Preference) getListView().getItemAtPosition(((AdapterContextMenuInfo) info).position);

            if (preference instanceof AccessPoint) {
                mSelectedAccessPoint = (AccessPoint) preference;
                menu.setHeaderTitle(mSelectedAccessPoint.mSsid);
                if (mSelectedAccessPoint.getLevel() != -1 && mSelectedAccessPoint.getState() == null) {
                    menu.add(Menu.NONE, MENU_ID_CONNECT, 0, R.string.wifi_menu_connect);
                }
                if (mSelectedAccessPoint.mNetworkId != INVALID_NETWORK_ID) {
                    /// M: should add forget menu
                    if (mExt.shouldAddForgetMenu(mSelectedAccessPoint.mSsid,mSelectedAccessPoint.mSecurity)) {
                        menu.add(Menu.NONE, MENU_ID_FORGET, 0, R.string.wifi_menu_forget);
                    }
                    menu.add(Menu.NONE, MENU_ID_MODIFY, 0, R.string.wifi_menu_modify);
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mSelectedAccessPoint == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
        case MENU_ID_CONNECT: 
            if (mSelectedAccessPoint.mNetworkId != INVALID_NETWORK_ID) {
                if (!requireKeyStore(mSelectedAccessPoint.getConfig())) {
                    mWifiManager.connect( mSelectedAccessPoint.mNetworkId, mConnectListener);
                }
            } else if (mSelectedAccessPoint.mSecurity == AccessPoint.SECURITY_NONE) {
                /** Bypass dialog for unsecured networks */
                mSelectedAccessPoint.generateOpenNetworkConfig();
                mWifiManager.connect(mSelectedAccessPoint.getConfig(), mConnectListener);
            } else {
                showDialog(mSelectedAccessPoint, false);
            }
            return true;
        case MENU_ID_FORGET: 
            mWifiManager.forget(mSelectedAccessPoint.mNetworkId, mForgetListener);
            return true;
        case MENU_ID_MODIFY: 
            showDialog(mSelectedAccessPoint, true);
            return true;
        default:
            break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPoint) {
            mSelectedAccessPoint = (AccessPoint) preference;
            /** Bypass dialog for unsecured, unsaved networks */
            if (mSelectedAccessPoint.mSecurity == AccessPoint.SECURITY_NONE
                    && mSelectedAccessPoint.mNetworkId == INVALID_NETWORK_ID) {
                mSelectedAccessPoint.generateOpenNetworkConfig();
                mWifiManager.connect(mSelectedAccessPoint.getConfig(), mConnectListener);
            } else {
                showDialog(mSelectedAccessPoint, false);
            }
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            removeDialog(WIFI_DIALOG_ID);
            mDialog = null;
            mAccessPointSavedState = null;
        }

        // Save the access point and edit mode
        mDlgAccessPoint = accessPoint;
        mDlgEdit = edit;

        showDialog(WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WIFI_DIALOG_ID:
                AccessPoint ap = mDlgAccessPoint; // For manual launch
                if (ap == null) { // For re-launch from saved state
                    if (mAccessPointSavedState != null) {
                        ap = new AccessPoint(getActivity(), mAccessPointSavedState);
                        // For repeated orientation changes
                        mDlgAccessPoint = ap;
                    }
                }
                // If it's still null, fine, it's for Add Network
                mSelectedAccessPoint = ap;
                mDialog = new WifiDialog(getActivity(), this, ap, mDlgEdit);
                return mDialog;
                /**
                 * mtk54279 case WPS_PBC_DIALOG_ID: return new WpsDialog(getActivity(), WpsInfo.PBC); case WPS_PIN_DIALOG_ID:
                 * return new WpsDialog(getActivity(), WpsInfo.DISPLAY);
                 */
            case WIFI_SKIPPED_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.wifi_skipped_message)
                            .setCancelable(false)
                            .setNegativeButton(R.string.wifi_skip_anyway,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    getActivity().setResult(Activity.RESULT_CANCELED);
                                    getActivity().finish();
                                }
                            })
                            .setPositiveButton(R.string.wifi_dont_skip,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            })
                            .create();
            case WIFI_AND_MOBILE_SKIPPED_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.wifi_and_mobile_skipped_message)
                            .setCancelable(false)
                            .setNegativeButton(R.string.wifi_skip_anyway,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    getActivity().setResult(Activity.RESULT_CANCELED);
                                    getActivity().finish();
                                }
                            })
                            .setPositiveButton(R.string.wifi_dont_skip,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            })
                            .create();
            default:
                break;
        }
        return super.onCreateDialog(dialogId);
    }

    private boolean isPhone() {
        final TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(
                Context.TELEPHONY_SERVICE);
        return telephonyManager != null
                && telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    /**
    * Return true if there's any SIM related impediment to connectivity.
    * Treats Unknown as OK. (Only returns true if we're sure of a SIM problem.)
    */
   protected boolean hasSimProblem() {
       final TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(
               Context.TELEPHONY_SERVICE);
       return telephonyManager != null
               && telephonyManager.getCurrentPhoneType() == TelephonyManager.PHONE_TYPE_GSM
               && telephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY
               && telephonyManager.getSimState() != TelephonyManager.SIM_STATE_UNKNOWN;
   }

    private boolean requireKeyStore(WifiConfiguration config) {
        if (WifiConfigController.requireKeyStore(config) && KeyStore.getInstance().state() != KeyStore.State.UNLOCKED) {
            mKeyStoreNetworkId = config.networkId;
            Credentials.getInstance().unlock(getActivity());
            return true;
        }
        return false;
    }

    /**
     * Shows the latest access points available with supplimental information like the strength of network and the security
     * for it.
     */
    private void updateAccessPoints() {
        // Safeguard from some delayed event handling
        if (getActivity() == null) {
            return;
        }

        final int wifiState = mWifiManager.getWifiState();

        switch (wifiState) {
        case WifiManager.WIFI_STATE_ENABLED:
            // AccessPoints are automatically sorted with TreeSet.
            final Collection<AccessPoint> accessPoints = constructAccessPoints();
            // / M:mtk54279 getPreferenceScreen().removeAll();
            mWifiApCategory.removeAll();
            if (accessPoints.size() == 0) {
                addMessagePreference(R.string.wifi_empty_list_wifi_on);
            }
            for (AccessPoint accessPoint : accessPoints) {
                // / M:mtk54279 getPreferenceScreen().addPreference(accessPoint);
                mWifiApCategory.addPreference(accessPoint);
            }
            getPreferenceScreen().addPreference(mWifiApCategory);
            getPreferenceScreen().addPreference(mAddNetworkButton);
            break;

        case WifiManager.WIFI_STATE_ENABLING:
            // getPreferenceScreen().removeAll();
            mWifiApCategory.removeAll();
            break;

        case WifiManager.WIFI_STATE_DISABLING:
            addMessagePreference(R.string.wifi_stopping);
            getPreferenceScreen().removePreference(mWifiApCategory);
            getPreferenceScreen().removePreference(mAddNetworkButton);
            break;

        case WifiManager.WIFI_STATE_DISABLED:
            addMessagePreference(R.string.wifi_empty_list_wifi_off);
            getPreferenceScreen().removePreference(mWifiApCategory);
            getPreferenceScreen().removePreference(mAddNetworkButton);
            break;
        default:
            break;
        }
    }

    private void addMessagePreference(int messageId) {
        if (mEmptyView != null) {
            mEmptyView.setText(messageId);
        }
        // getPreferenceScreen().removeAll();
        mWifiApCategory.removeAll();
    }

    /** Returns sorted list of access points */
    private List<AccessPoint> constructAccessPoints() {
        ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        /**
         * Lookup table to more quickly update AccessPoints by only considering objects with the correct SSID. Maps SSID ->
         * List of AccessPoints with the given SSID.
         */
        Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();

        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                /// M: Add for EAP-SIM begin @{
                if (FeatureOption.MTK_EAP_SIM_AKA && config.imsi != null && !config.imsi.equals("\"none\"")) {
                    Xlog.d(TAG,"config.SSID " + config.SSID);
                    Xlog.d(TAG,"config.imsi " + config.imsi);
                    Xlog.d(TAG,"config.simSlot " + config.simSlot);
                    Xlog.d(TAG,"config.networkId " + config.networkId);
                    int slot = 0;
                    if (config.simSlot.equals("\"0\"")) {
                        slot = 0;
                    } else if (config.simSlot.equals("\"1\"")) {
                        slot = 1;
                    }
                    //Add for gemini+
                    String[] simslots = config.simSlot.split("\"");
                    if (simslots.length > 1) {
                        slot = Integer.parseInt(simslots[1]);  
                    }
                    //in simulator mode, skip
                    if ((config.imsi).equals("\"1232010000000000@wlan.mnc001.mcc232.3gppnetwork.org\"")
                            || (config.imsi).equals("\"0232010000000000@wlan.mnc001.mcc232.3gppnetwork.org\"")) {
                        Xlog.d(TAG,"in simulator mode, skip");
                    } else if (config.toString().contains("eap: SIM")) {
                        Xlog.d(TAG,"mTelephonyManagerEx.getSubscriberId() " + mTelephonyManagerEx.getSubscriberId(slot));
                        Xlog.d(TAG,"makeNAI(mTelephonyManagerEx.getSubscriberId(), SIM); " +
                            WifiDialog.makeNAI(mTelephonyManagerEx.getSubscriberId(slot), "SIM"));
                        if (config.imsi.equals(WifiDialog.makeNAI(mTelephonyManagerEx.getSubscriberId(slot), "SIM"))) {
                            Xlog.d(TAG,"user doesn't change or remove sim card");
                        } else {
                            if(!mExt.isTustAP(AccessPoint.removeDoubleQuotes(config.SSID), AccessPoint.getSecurity(config))){
                                Xlog.d(TAG,"user change or remove sim card");
                                Xlog.d(TAG," >>mWifiManager.removeNetwork(config.networkId);");
                                boolean s = mWifiManager.removeNetwork(config.networkId);
                                Xlog.d(TAG," <<mWifiManager.removeNetwork(config.networkId); s: " + s);
                                Xlog.d(TAG,"   >>saveNetworks();");
                                s = mWifiManager.saveConfiguration();
                                Xlog.d(TAG,"saveNetworks(): " + s);
                                continue;
                            }
                        }  
                    } else if (config.toString().contains("eap: AKA")) {
                        Xlog.d(TAG,"mTelephonyManagerEx.getSubscriberId() " + mTelephonyManagerEx.getSubscriberId(slot));
                        Xlog.d(TAG,"makeNAI(mTelephonyManagerEx.getSubscriberId(), AKA); " +
                            WifiDialog.makeNAI(mTelephonyManagerEx.getSubscriberId(slot), "AKA"));
                        if (WifiDialog.makeNAI(mTelephonyManagerEx.getSubscriberId(slot), "AKA").equals(config.imsi)) {
                            Xlog.d(TAG,"user doesn't change or remove usim card");
                        } else {
                            if(!mExt.isTustAP(AccessPoint.removeDoubleQuotes(config.SSID), AccessPoint.getSecurity(config))){
                                Xlog.d(TAG,"user change or remove usim card");
                                Xlog.d(TAG," >> mWifiManager.removeNetwork(config.networkId);");
                                boolean s = mWifiManager.removeNetwork(config.networkId);
                                Xlog.d(TAG," << mWifiManager.removeNetwork(config.networkId); s: " + s);
                                Xlog.d(TAG,"   >>saveNetworks();");
                                s = mWifiManager.saveConfiguration();
                                Xlog.d(TAG,"saveNetworks(): " + s);
                                Xlog.d(TAG,"   <<saveNetworks();");
                                continue;
                            }
                        }
                    }
                }
                /// @}

                AccessPoint accessPoint = new AccessPoint(getActivity(), config);
                accessPoint.update(mLastInfo, mLastState);
                accessPoints.add(accessPoint);
                apMap.put(accessPoint.mSsid, accessPoint);
            }
        }

        final List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 || result.capabilities.contains("[IBSS]")) {
                    continue;
                }

                boolean found = false;
                for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
                    if (accessPoint.update(result)) {
                        found = true;
                    }
                }
                if (!found) {
                    AccessPoint accessPoint = new AccessPoint(getActivity(), result);
                    accessPoints.add(accessPoint);
                    apMap.put(accessPoint.mSsid, accessPoint);
                }
            }
        }

        // Pre-sort accessPoints to speed preference insertion
        ArrayList<AccessPoint> origAccessPoints = new ArrayList<AccessPoint>(accessPoints.size());
        origAccessPoints.addAll(accessPoints);
        try {
        Collections.sort(accessPoints);
        } catch (ClassCastException e) {
            Xlog.d(TAG,"collection.sort exception;origAccessPoints=" + origAccessPoints);
            return origAccessPoints;
        } catch (UnsupportedOperationException e) {
            Xlog.d(TAG,"collection.sort exception;origAccessPoints=" + origAccessPoints);
            return origAccessPoints;          
        }
        return accessPoints;
    }

    /** A restricted multimap for use in constructAccessPoints */
    private class Multimap<K, V> {
        private HashMap<K, List<V>> mStore = new HashMap<K, List<V>>();

        /** retrieve a non-null list of values with key K */
        List<V> getAll(K key) {
            List<V> values = mStore.get(key);
            return values != null ? values : Collections.<V> emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = mStore.get(key);
            if (curVals == null) {
                curVals = new ArrayList<V>(3);
                mStore.put(key, curVals);
            }
            curVals.add(val);
        }
    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        Xlog.d(TAG, "action = " + action);
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) ||
                WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action) ||
                WifiManager.LINK_CONFIGURATION_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            // Ignore supplicant state changes when network is connected
            // TODO: we should deprecate SUPPLICANT_STATE_CHANGED_ACTION and
            // introduce a broadcast that combines the supplicant and network
            // network state change events so the apps dont have to worry about
            // ignoring supplicant state change when network is connected
            // to get more fine grained information.
            SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (!mConnected.get() /*&& SupplicantState.isHandshakeState(state)*/) {
                updateConnectionState(WifiInfo.getDetailedStateOf(state));
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            mConnected.set(info.isConnected());
            changeNextButtonState(info.isConnected());
            updateAccessPoints();
            updateConnectionState(info.getDetailedState());
            if (mAutoFinishOnConnection && info.isConnected()) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.setResult(Activity.RESULT_OK);
                    activity.finish();
                }
                return;
            }
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);
        } else if (WifiManager.NO_CERTIFICATION_ACTION.equals(action)) { 
            /// M: show error message @{
            String apSSID = "";
            if (mSelectedAccessPoint != null) {
                apSSID = "[" + mSelectedAccessPoint.mSsid + "] ";
            }
            Xlog.i(TAG, "Receive  no certification broadcast for AP " + apSSID);
            String message = getResources().getString(R.string.wifi_no_cert_for_wapi) + apSSID;
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            /// @}
        }
    }

    private void updateConnectionState(DetailedState state) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }

        if (state == DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }

        mLastInfo = mWifiManager.getConnectionInfo();
        if (state != null) {
            mLastState = state;
        }

        for (int i = mWifiApCategory.getPreferenceCount() - 1; i >= 0; --i) {
            // Maybe there's a WifiConfigPreference
            Preference preference = mWifiApCategory.getPreference(i);
            if (preference instanceof AccessPoint) {
                final AccessPoint accessPoint = (AccessPoint) preference;
                accessPoint.update(mLastInfo, mLastState);
            }
        }
    }

    private void updateWifiState(int state) {
        switch (state) {
        case WifiManager.WIFI_STATE_ENABLED:
            mScanner.resume();
            return; // not break, to avoid the call to pause() below

        case WifiManager.WIFI_STATE_ENABLING:
            addMessagePreference(R.string.wifi_starting);
            break;

        case WifiManager.WIFI_STATE_DISABLED:
            addMessagePreference(R.string.wifi_empty_list_wifi_off);
            getPreferenceScreen().removePreference(mAddNetworkButton);
            break;
        default:
            break;
        }

        mLastInfo = null;
        mLastState = null;
        mScanner.pause();
    }

    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiManager.startScanActive()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_fail_to_scan,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
            sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
        }
    }

    /**
     * Renames/replaces "Next" button when appropriate. "Next" button usually exists in Wifi setup screens, not in usual wifi
     * settings screen.
     * 
     * @param connected
     *            true when the device is connected to a wifi network.
     */
    private void changeNextButtonState(boolean connected) {
        if (mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(connected);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
            forget();
        } else if (button == WifiDialog.BUTTON_SUBMIT) {
            submit(mDialog.getController());
        }
    }

    /* package */void submit(WifiConfigController configController) {

        final WifiConfiguration config = configController.getConfig();

        /// M: add for EAP_SIM/AKA start,remind user when he use eap-sim/aka in a wrong way @{
        try {
            if (config != null && FeatureOption.MTK_EAP_SIM_AKA && config.imsi != null) {
                if (config.toString().contains("eap: SIM") || config.toString().contains("eap: AKA")) {
                    // cannot use eap-sim/aka under airplane mode
                    if (Settings.System.getInt(this.getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
                        Toast.makeText(getActivity(), R.string.eap_sim_aka_airplanemode, Toast.LENGTH_LONG).show();
                        return;
                    }
                    // cannot use eap-sim/aka without a sim/usimcard
                    if (config.imsi.equals("\"error\"")) {
                        Toast.makeText(getActivity(), R.string.eap_sim_aka_no_sim_error, Toast.LENGTH_LONG).show();
                        return;
                    }
                    // cannot use eap-sim/aka if user doesn't select a sim slot
                    if ((FeatureOption.MTK_GEMINI_SUPPORT) && (config.imsi.equals("\"none\""))) {
                        Toast.makeText(getActivity(), R.string.eap_sim_aka_no_sim_slot_selected,Toast.LENGTH_LONG).show();
                        return;
                    }
                }

            }
        } catch (Exception e) {
            Xlog.d(TAG,"submit exception() " + e.toString());
        }
        /// @}

        if (config == null) {

            /// M: add for EAP_SIM/AKA start, cannot use eap-sim/aka under airplane mode @{
            if (FeatureOption.MTK_EAP_SIM_AKA) {
                Xlog.d(TAG,"mSelectedAccessPoint " + mSelectedAccessPoint);
                List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
                if (configs != null) {
                    for (WifiConfiguration mConfig : configs) {
                        Xlog.d(TAG,"onClick() >>if ((mConfig.SSID).equals(mSelectedAccessPoint.mSsid)) {");
                        Xlog.d(TAG,"onClick()" + mConfig.SSID);
                        Xlog.d(TAG,"onClick() " + mSelectedAccessPoint.mSsid);
                        if (mConfig != null && mConfig.SSID != null && 
                            (mConfig.SSID).equals(WifiDialog.addQuote(mSelectedAccessPoint.mSsid)) &&
                            (Settings.System.getInt(this.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) == 1) &&
                            (mConfig.toString().contains("eap: SIM") || mConfig.toString().contains("eap: AKA"))) {
                            Xlog.d(TAG, "remind user: cannot user eap-sim/aka under airplane mode");
                            Toast.makeText(getActivity(), R.string.eap_sim_aka_airplanemode, Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            /// @}

            if (mSelectedAccessPoint != null
                    && !requireKeyStore(mSelectedAccessPoint.getConfig())
                    && mSelectedAccessPoint.mNetworkId != INVALID_NETWORK_ID) {
                /// M: whether mSelectedAccessPoint is null @{
                DetailedState state = mSelectedAccessPoint.getState();
                if (state == null) {
                /// @}
                    mWifiManager.connect(mSelectedAccessPoint.mNetworkId, mConnectListener);
                }
            }
        } else if (config.networkId != INVALID_NETWORK_ID) {
            if (mSelectedAccessPoint != null) {
                mWifiManager.save(config, mSaveListener);
            }
        } else {
            if (configController.isEdit() || requireKeyStore(config)) {
                mWifiManager.save(config, mSaveListener);
            } else {
                mWifiManager.connect(config, mConnectListener);
            }
        }

        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();
    }

    /* package */void forget() {
        if (mSelectedAccessPoint.mNetworkId == INVALID_NETWORK_ID) {
            // Should not happen, but a monkey seems to triger it
            Xlog.e(TAG, "Failed to forget invalid network " + mSelectedAccessPoint.getConfig());
            return;
        }

        mWifiManager.forget(mSelectedAccessPoint.mNetworkId, mForgetListener);

        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    /**
     * Called when "add network" button is pressed.
     */
    /* package */void onAddNetworkPressed() {
        // No exact access point is selected.
        mSelectedAccessPoint = null;
        showDialog(null, true);
    }


    ButtonPreference.PrefButtonClickedListener mAddNetworkButtonCbk = new ButtonPreference.PrefButtonClickedListener() {
        @Override
        public void onPreButtonClicked() {
            onAddNetworkPressed();
        }
    };
    /**
     * M: handle configuration change event
     * @param configuration
     * @return
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDialog != null) {
            mDialog.closeSpinnerDialog();
        }
    }
}
