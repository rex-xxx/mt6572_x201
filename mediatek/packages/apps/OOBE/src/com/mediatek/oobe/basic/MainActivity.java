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


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.IWindowManager;
import android.view.Surface;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.oobe.R;
import com.mediatek.oobe.WizardActivity;
import com.mediatek.oobe.qsg.QuickStartGuideMain;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.oobe.utils.Utils;
import com.mediatek.xlog.Xlog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = OOBEConstants.TAG;
    private static final int REQUEST_CODE_ADVANCED_SETTINGS = 1001;
    private static final int QUICK_START_GUIDE_CODE = 4001;
    private static final int EVENT_MONITOR_SIM_TIME_OUT = 2001;
    private static final int EVENT_SIM_DETECTING_READY = 2002;
    private static final int DIALOG_WAITING_SIM = 3001;
    private static boolean sIsRunning = false;
    private boolean mIsStepInitiated = false;
    private boolean mIsFirstRun;
    // max time to wait before SIM card is ready
    private static final int TIME_MONITOR_SIM = 30000;
    private long mStartTime;
    private long mEndTime;
    private boolean mStatusBarDisabled = false;
    // advance meta model
    private static final String BOOT_MODE_INFO_FILE = "/sys/class/BOOT/BOOT/boot/boot_mode";
    private static final int BOOT_MODE_STR_LEN = 1;
    private static final int BOOT_MODE_NORMAL = 0;
    private static final int BOOT_MODE_META = 1;
    private static final int BOOT_MODE_ADV_META = 5;
    private int mBootMode = BOOT_MODE_NORMAL;

    // Whether OOBE is running, in case of this main activity may be destroyed unexpected
    private int mProgressbarMaxSize = 0;
    private int mCurrentStep = -1;
    private boolean mSimExist = false;
    private TelephonyManager mTelephonyManager;
    private boolean mIsGoToNextStep = true;
    private IntentFilter mSIMIntentFilter;
    private List<OOBEStepActivityInfo> mExecutingActivityList;
    private String[] mStepActivities = new String[] { 
            "1/com.mediatek.oobe.basic.OOBE_LANGUAGE_SETTING",
            "2/com.mediatek.oobe.basic.DATE_TIME_SETTINGS_WIZARD",
            "3/com.mediatek.oobe.basic.SIM_MANAGEMENT_SETTINGS_WIZARD",
            "4/com.mediatek.oobe.basic.DEFAULT_SIM_SETTINGS_WIZARD", 
            "5/com.mediatek.oobe.basic.OOBE_IMPORT_CONTACTS",
            "6/com.mediatek.oobe.basic.OOBE_INTERNET_CONNECTION", 
            "7/com.mediatek.oobe.basic.WIFI_SETTINGS_WIZARD" };

    private static String sDeviceInfo;
    private static boolean sIsTablet = false;

    private boolean isTablet() {
        sDeviceInfo = SystemProperties.get("ro.build.characteristics");
        Xlog.d(TAG, sDeviceInfo);
        if (sDeviceInfo.equals("tablet")) {
            sIsTablet = true;
        }
        return sIsTablet;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case EVENT_MONITOR_SIM_TIME_OUT:
                Xlog.v(TAG, "MainActivity handler wait SIM time out");
                break;
            case EVENT_SIM_DETECTING_READY:
                Xlog.v(TAG, "MainActivity handler SIM initialization finish");
                mHandler.removeMessages(EVENT_MONITOR_SIM_TIME_OUT);
                break;
            default:
                Xlog.v(TAG, "MainActivity handler unknown message");
                break;
            }
            Xlog.d(TAG, "sIsRunning=" + sIsRunning + ";mIsStepInitiated=" + mIsStepInitiated);
            if (!sIsRunning) {
                initStep();
                removeDialog(DIALOG_WAITING_SIM);
            }
        };
    };

    BroadcastReceiver mSIMStateReceiver = new BroadcastReceiver() {
        private boolean mSlotChecked[] = { false, false, false, false};

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Xlog.d("hotswapdbg", "MainActivity " + action.toString());
            if (action != null && action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String newState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
                Xlog.i(TAG, "MainActivity(), monitor SIM state change, new state=" + newState + "SIM slotId=" + slotId);

                if (!newState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT)
                        && !newState.equals(IccCardConstants.INTENT_VALUE_ICC_NOT_READY)) {
                    // if we don't get one sim slot has sim card
                    // we set that slot was checked.
                    mSlotChecked[slotId] = true;
                    mSimExist = true;
                    mEndTime = System.currentTimeMillis();
                    Xlog.i(TAG, "detecting SIM card using time " + (mEndTime - mStartTime) / 1000 + "s.");

                    mHandler.sendEmptyMessage(EVENT_SIM_DETECTING_READY);
                } else if (newState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT)) {
                    // if we don't get one sim slot has sim card
                    // we set that slot was checked.
                    mSlotChecked[slotId] = true;

                    if (PhoneConstants.GEMINI_SIM_NUM >= 2) {
                        // if two sim card was checked, we don't get the sim card
                        // we just told oobe, we had already checked two sim slots.
                        boolean isAllChecked = true;
                        for (int i = 0; i < PhoneConstants.GEMINI_SIM_NUM; i++) {
                            isAllChecked = isAllChecked && mSlotChecked[i];
                        }
                        if (isAllChecked) {
                            Xlog.i(TAG, "gemini: checked slots done ");
                            mSimExist = false;
                            mHandler.sendEmptyMessage(EVENT_SIM_DETECTING_READY);
                        }
                    } else {
                        // if signal card version, we just have to check on sim slot
                        Xlog.i(TAG, " signal card: checked slots done ");
                        mSimExist = false;
                        mHandler.sendEmptyMessage(EVENT_SIM_DETECTING_READY);
                    }
                }
            }
        };
    };

    /**
     * set running status
     * 
     * @param running
     *            status should be true or false
     */
    public static void setRunning(boolean running) {
        sIsRunning = running;
    }

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState
     *            bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Xlog.i(TAG, "onCreate OOBE 2.0");
        super.onCreate(savedInstanceState);
        mStartTime = System.currentTimeMillis();
        mSimExist = isSimExist();
        Xlog.d(TAG, "MainActivity onCreate mSimExist=" + mSimExist);

        if (savedInstanceState != null) {
            mCurrentStep = savedInstanceState.getInt("currentStep");
            mProgressbarMaxSize = savedInstanceState.getInt("totalSteps");
            Xlog.d(TAG, "restore saved instance state mCurrentStep=" + mCurrentStep + "mProgressbarMaxSize="
                    + mProgressbarMaxSize);
            setActivityList();
        }
        isTablet();
        if (!sIsTablet) {
            Xlog.i(TAG, "It's phone anyway");
            try {
                IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
                wm.freezeRotation(Surface.ROTATION_0);
                Xlog.i(TAG, "Can't be rotated");
            } catch (RemoteException exc) {
                Xlog.i(TAG, "Still enable to rotate the orientation");
            }

            // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
        int oobeHasRun = Settings.System.getInt(getContentResolver(), OOBEConstants.OOBE_HAS_RUN_KEY, 0);
        mIsFirstRun = ((oobeHasRun == 0) ? true : false);
        if (mIsFirstRun) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.OOBE_DISPLAY, Settings.System.OOBE_DISPLAY_ON);
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0);
        }
        // oobe first run
        if (oobeHasRun == 0) {
            // disable status bar expanded
            disableStatusBar(true);
        }

        // disable QSG on tablet
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, QuickStartGuideMain.class);
        int qsgDisabledState = pm.getComponentEnabledSetting(name);
        Xlog.d(TAG, "disable QSG on tablet qsgDisabledState" + qsgDisabledState);
        if (sIsTablet) {
            if (qsgDisabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                Xlog.d(TAG, "tablet disable qsg");
                pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        } else {
            if (qsgDisabledState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                Xlog.d(TAG, "phone enable qsg");
                pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        PackageManager.DONT_KILL_APP);
            }
        }

        mSIMIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mSIMIntentFilter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
        mBootMode = getBootMode();
        // if sim exist is false, and OOBE is first run, so we waiting for sim status change intent.
        // otherwise, we just get sim card status: has sim card, or not.
        if (!mSimExist && mIsFirstRun && mBootMode != BOOT_MODE_ADV_META && !Utils.isWifiOnly(this)) {
            mHandler.sendEmptyMessageDelayed(EVENT_MONITOR_SIM_TIME_OUT, TIME_MONITOR_SIM);
            showDialog(DIALOG_WAITING_SIM);
        } else {
            // SIM detected done
            mHandler.sendEmptyMessage(EVENT_SIM_DETECTING_READY);
        }
        registerReceiver(mSIMStateReceiver, mSIMIntentFilter);
    }

    /**
     * Init step at the very begining, depending on SIM card status
     */
    private void initStep() {
        if (mIsStepInitiated) {
            Xlog.v(TAG, "MainActivity have already init step list");
            return;
        }
        mIsStepInitiated = true;
        setActivityList();
        Xlog.i(TAG, "initStep sIsRunning?" + sIsRunning);
        if (!sIsRunning) {
            setRunning(true);
            if (mCurrentStep == -1) {
                nextActivity(true);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Xlog.d(TAG, " onResume() mIsStepInitiated=" + mIsStepInitiated);
        Xlog.d(TAG, " onResume() sIsRunning=" + sIsRunning);

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_WAITING_SIM) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.oobe_waiting_sim));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Xlog.i(TAG, "MainActivity.onDestroy()");
        unregisterReceiver(mSIMStateReceiver);
        if (!mSimExist && mIsFirstRun && mBootMode != BOOT_MODE_ADV_META) {
            mHandler.removeMessages(EVENT_MONITOR_SIM_TIME_OUT);
            removeDialog(DIALOG_WAITING_SIM);
        }

        mIsStepInitiated = false;
        setRunning(false);
        // enable status bar expanded
        if (mStatusBarDisabled) {
            disableStatusBar(false);
        }

        if (!sIsTablet) {
            Xlog.i(TAG, "It's phone anyway, prepare to get out of this");

            try {
                IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
                wm.thawRotation();
                Xlog.i(TAG, "Can be rotated again");
            } catch (RemoteException exc) {
                Xlog.i(TAG, "Still enable to rotate the orientation");
            }
            // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        super.onDestroy();
    }

    private boolean isSimExist() {
        Xlog.d(TAG, "MainActivity isSimExist() function");
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (Utils.isGemini()) {
            Xlog.d(TAG, "MainActivity isGemini true");
            boolean sim1State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1);

            boolean sim2State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2);
            return (sim1State || sim2State);

        }  else if (PhoneConstants.GEMINI_SIM_NUM == 3) {
            Xlog.d(TAG, "MainActivity isSimExist, 3 cards");
            boolean sim1State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1);
            boolean sim2State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2);
            boolean sim3State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_3);
            return (sim1State || sim2State || sim3State);
        }  else if (PhoneConstants.GEMINI_SIM_NUM == 4) {
            Xlog.d(TAG, "MainActivity isSimExist, 4 cards");
            boolean sim1State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1);
            boolean sim2State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2);
            boolean sim3State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_3);
            boolean sim4State = mTelephonyManager.hasIccCardGemini(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_4);
            return (sim1State || sim2State || sim3State || sim4State);
        } else {
            Xlog.d(TAG, "MainActivity isGemini false");
            return mTelephonyManager.hasIccCard();

        }

    }

    // Get the step list
    protected void setActivityList() {
        Xlog.d(TAG, "setActivityList()");
        mExecutingActivityList = new ArrayList<OOBEStepActivityInfo>();
        mProgressbarMaxSize = 0;

        for (int i = 0; i < mStepActivities.length; i++) {
            Xlog.i(TAG, mStepActivities[i]);
            if (mStepActivities[i].contains(OOBEConstants.ACTION_SIM_MANAGEMENT)
                    || mStepActivities[i].contains(OOBEConstants.ACTION_DEFAULT_SIM)) {
                if (!mSimExist || PhoneConstants.GEMINI_SIM_NUM == 1) {
                    continue;
                }
            } else if (mStepActivities[i].contains(OOBEConstants.ACTION_IMPORT_CONTACTS)) {
                if (!mSimExist) {
                    continue;
                }
            } else if (mStepActivities[i].contains(OOBEConstants.ACTION_INTERNET_CONNECTION)) {
                if (PhoneConstants.GEMINI_SIM_NUM > 1 || Utils.isWifiOnly(this)) {
                    continue;
                }
            }

            OOBEStepActivityInfo localStepSetting = new OOBEStepActivityInfo(mStepActivities[i]);
            mExecutingActivityList.add(localStepSetting);
            mProgressbarMaxSize++;
        }
    }

    protected void nextActivity(boolean bNextStep) {
        Xlog.i(TAG, "MainActivity.nextActivity(), next = " + bNextStep + ", mCurrentStep = " 
                + mCurrentStep + "mProgressbarMaxSize =" + mProgressbarMaxSize);
        mIsGoToNextStep = bNextStep;
        if (bNextStep) {
            mCurrentStep++;
        } else {
            mCurrentStep--;
        }
        if (mCurrentStep >= mProgressbarMaxSize) {
            Xlog.d(TAG, "start advance activity");
            startAdvancedSettings();
            return;
        }

        Xlog.d(TAG, "MainActivity.nextActivity(), " + "mCurrentStep = " + mCurrentStep + ", mExecutingActivityList size = "
                + mExecutingActivityList.size());

        if (mCurrentStep >= 0 && mCurrentStep < mExecutingActivityList.size()) {
            Xlog.i(TAG, "MainActivity.nextActivity(), step index = " + mCurrentStep);
            mExecutingActivityList.get(mCurrentStep).startStepActivity();
        }
    }

    /**
     * start next activity
     * 
     * @param intent
     *            Intent pass to next activity
     * @param nRequestCode
     *            int
     */
    public void startNextActivity(Intent intent, int nRequestCode) {

        Xlog.d(TAG, "MainActivity.startNextActivity(), " + "startActivityForResult nRequestCode == " + nRequestCode
                + ", mCurrentStep == " + mCurrentStep);

        if (nRequestCode == -1) {
            Xlog.w(TAG, "should not come here, request code == -1, finish it now");
            this.finish();
            setRunning(false);
            mCurrentStep = -1;
            return;
        }

        Xlog.i(TAG, "MainActivity.startNextActivity(), action=" + intent.getAction() + ", " + "total_step="
                + mProgressbarMaxSize + ", current_step=" + (mCurrentStep + 1));

        intent.putExtra(OOBEConstants.OOBE_BASIC_STEP_TOTAL, mProgressbarMaxSize);
        intent.putExtra(OOBEConstants.OOBE_BASIC_STEP_INDEX, mCurrentStep + 1);
        super.startActivityForResult(intent, nRequestCode);

        if (mIsGoToNextStep) {
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
        } else {
            overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        }
    }

    private void startAdvancedSettings() {
        Xlog.d(TAG, "startAdvancedSettings()");
        Intent intent = new Intent(OOBEConstants.ACTION_ADVANCED_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE_ADVANCED_SETTINGS);
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);

        // startActivity(intent);
        // finishOOBE();//Basic settings finish, start advanced settings
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        Xlog.d(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);

        if (mCurrentStep == -1 && requestCode == 1) {
            mCurrentStep = 0;
            Xlog.d(TAG, "onActivityResult: mCurrentStep is -1, set to 0");
            if (!sIsRunning) {
                initStep();
            }
            if (mProgressbarMaxSize == 0) {
                return;
            }
        }

        if ((0 == mCurrentStep) && (OOBEConstants.RESULT_CODE_BACK == resultCode)) {
            Xlog.d(TAG, "Finished by first activity");
            finishOOBE();
            return;
        }

        if (OOBEConstants.RESULT_CODE_BACK == resultCode) {
            nextActivity(false);
        } else if (OOBEConstants.RESULT_CODE_NEXT == resultCode) {
            nextActivity(true);
        } else if (OOBEConstants.RESULT_CODE_FINISH == resultCode) {
            Xlog.d(TAG, "MainActivity onActivityResult, cur step=" + mCurrentStep + ", total step=" + mProgressbarMaxSize);
            finishOOBE();

        }

    }

    private void finishOOBE() {
        // whether OOBE has been run once
        int oobeHasRun = Settings.System.getInt(getContentResolver(), OOBEConstants.OOBE_HAS_RUN_KEY, 0);
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, WizardActivity.class);
        int wizardDisabledState = pm.getComponentEnabledSetting(name);
        Xlog.i(TAG, "Finish settigns, if OOBE has been configured once, disable it. oobeHasRun=" + oobeHasRun
                + "wizardDisabledState=" + wizardDisabledState);

        if (oobeHasRun == 0 || wizardDisabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            Xlog.i(TAG, "Here we go, MainActivity.finishOOBE(), set oobe_has_run flag to 1 , start launcher now");
            Settings.System.putInt(getContentResolver(), OOBEConstants.OOBE_HAS_RUN_KEY, 1);

            /* this start quick start guide, if it's the first run of OOBE */
            if (!sIsTablet) {
                Intent intent = new Intent(OOBEConstants.ACTION_QUICK_START_GUIDE);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra("mIsFirstRun", true);
                startActivity(intent);
            }

            Settings.System.putInt(getContentResolver(), 
                Settings.System.OOBE_DISPLAY, Settings.System.OOBE_DISPLAY_DEFAULT);
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);

            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            // enable status bar expanded
            if (mStatusBarDisabled) {
                disableStatusBar(false);
            }
        }

        Xlog.d(TAG, "finishOOBE MainActivity finished");
        setRunning(false);
        mCurrentStep = -1;
        MainActivity.this.finish();

    }

    /**
     * Disable or enable status bar true - disable status bar false - enable status bar
     */
    private void disableStatusBar(boolean disable) {
        Xlog.i(TAG, "disable status bar " + disable);
        StatusBarManager statusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBarManager != null) {
            mStatusBarDisabled = disable;
            statusBarManager.disable(disable ? StatusBarManager.DISABLE_EXPAND : StatusBarManager.DISABLE_NONE);
        } else {
            Xlog.d(TAG, "Fail to get status bar instance");
        }
    }

    /**
     * save status when rotate screen
     * 
     * @author mtk54279 outState the status to save
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("currentStep", mCurrentStep);
        Xlog.d(TAG, "mCurrentStep = " + mCurrentStep);
        outState.putInt("totalSteps", mProgressbarMaxSize);
    }

    // a class for generating different intent action
    private class OOBEStepActivityInfo {

        private Intent mIntent;
        private int mActivityId = -1;
        private String mActivityInfo;
        private String mActionName;

        public OOBEStepActivityInfo(String infoString) {
            this.mActivityInfo = infoString;
            this.mIntent = new Intent();
            parseActivityInfo(mActivityInfo);
        }

        public void parseActivityInfo(String paramString) {
            if (OOBEConstants.DEBUG) {
                Xlog.i(TAG, "OOBEStepActivityInfo.parseActivityInfo(), paramString=" + paramString);
            }
            String[] arrayOfString = paramString.split("/");
            if (!arrayOfString[0].isEmpty()) {
                this.mActivityId = Integer.parseInt(arrayOfString[0]);
            }
            if (!arrayOfString[1].isEmpty()) {
                this.mActionName = arrayOfString[1];
            }
            if (OOBEConstants.DEBUG) {
                Xlog.i(TAG, "Integer is " + mActivityId + "  action name is " + mActionName);
            }
            mIntent.setAction(mActionName);
        }

        public void startStepActivity() {
            Xlog.i(TAG, "OOBEStepActivityInfo.execute(), mActivityId=" + mActivityId);
            startNextActivity(mIntent, mActivityId);
        }

        public Intent getIntent() {
            return this.mIntent;
        }

        public int getId() {
            return this.mActivityId;
        }

        public String getActionName() {
            return this.mActionName;
        }
    }

    private int getBootMode() {
        int mode = -1;
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(BOOT_MODE_INFO_FILE);
            br = new BufferedReader(fr);
            String readMode = br.readLine();
            if (readMode != null) {
                mode = Integer.parseInt(readMode);
            }
        } catch (FileNotFoundException e) {
            Xlog.d(TAG, "file not found; " + BOOT_MODE_INFO_FILE);
        } catch (IOException e) {
                Xlog.d(TAG, "read file error; " + e);
        } catch (NumberFormatException e) {
            Xlog.d(TAG, "NumberFormatException e =" + e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Xlog.d(TAG, "br file close error; " + BOOT_MODE_INFO_FILE);
            }
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException e) {
                Xlog.d(TAG, "fr file close error; " + BOOT_MODE_INFO_FILE);
            }
        }
        Xlog.d(TAG, "read mode;" + mode);
        return mode;
    }

}
