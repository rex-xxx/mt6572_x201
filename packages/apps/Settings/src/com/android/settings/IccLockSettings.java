/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.xlog.Xlog;

import java.util.List;

/**
 * Implements the preference screen to enable/disable ICC lock and
 * also the dialogs to change the ICC PIN. In the former case, enabling/disabling
 * the ICC lock will prompt the user for the current PIN.
 * In the Change PIN case, it prompts the user for old pin, new pin and new pin
 * again before attempting to change it. Calls the SimCard interface to execute
 * these operations.
 *
 */
public class IccLockSettings extends PreferenceActivity
        implements EditPinPreference.OnPinEnteredListener {
    private static final int OFF_MODE = 0;
    // State when enabling/disabling ICC lock
    private static final int ICC_LOCK_MODE = 1;
    // State when entering the old pin
    private static final int ICC_OLD_MODE = 2;
    // State when entering the new pin - first time
    private static final int ICC_NEW_MODE = 3;
    // State when entering the new pin - second time
    private static final int ICC_REENTER_MODE = 4;
    // Keys in xml file
    private static final String PIN_DIALOG = "sim_pin";
    private static final String PIN_TOGGLE = "sim_toggle";
    // Keys in icicle
    private static final String DIALOG_STATE = "dialogState";
    private static final String DIALOG_PIN = "dialogPin";
    private static final String DIALOG_ERROR = "dialogError";
    private static final String ENABLE_TO_STATE = "enableState";

    // Save and restore inputted PIN code when configuration changed
    // (ex. portrait<-->landscape) during change PIN code
    private static final String OLD_PINCODE = "oldPinCode";
    private static final String NEW_PINCODE = "newPinCode";

    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    // Which dialog to show next when popped up
    private int mDialogState = OFF_MODE;

    private String mPin;
    private String mOldPin;
    private String mNewPin;
    private String mError;
    // Are we trying to enable or disable ICC lock?
    private boolean mToState;

    private Phone mPhone;

    private EditPinPreference mPinDialog;
    private CheckBoxPreference mPinToggle;

    private Resources mRes;
    /*
     *M: set constant value for no retry left
     *   add for debug
     */
    private static final String TAG = "IccLockSettings";
    private static final int GET_SIM_RETRY_EMPTY = -100;    
    
    // For async handler to identify request type
    private static final int MSG_ENABLE_ICC_PIN_COMPLETE = 100;
    private static final int MSG_CHANGE_ICC_PIN_COMPLETE = 101;
    private static final int MSG_SIM_STATE_CHANGED = 102;

    //For Gemini
    private int mSimId = -1;
    private GeminiPhone mGeminiPhone;
    private static final String SIM_ID = "sim_id";
    private boolean mIsUnlockFollow = false;
    private boolean mIsShouldBeFinished = false; 
    private boolean mIsDeadLocked = false;
	/* M: add for AT&T custom  @ { */
	private ISimRoamingExt simRoamingExt;
    /* @ } */
    // For replies from IccCard interface
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
            case MSG_ENABLE_ICC_PIN_COMPLETE:
                Xlog.d(TAG, "MSG_ENABLE_ICC_PIN_COMPLETE");
                iccLockChanged(ar.exception == null);
                break;
            case MSG_CHANGE_ICC_PIN_COMPLETE:
                Xlog.d(TAG, "MSG_CHANGE_ICC_PIN_COMPLETE");
                iccPinChanged(ar.exception == null);
                break;
            case MSG_SIM_STATE_CHANGED:
                Xlog.d(TAG, "MSG_SIM_STATE_CHANGED");
                updatePreferences();
                updateOnSimLockStateChanged();
                break;
            default: 
                break;
            }

            return;
        }
    };

    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SIM_STATE_CHANGED));
            } else if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                ///M: add for hot swap feature
                Xlog.d(TAG,"receive ACTION_SIM_INFO_UPDATE");
                List<SIMInfo> simList = SIMInfo.getInsertedSIMList(IccLockSettings.this);
                if (simList != null) {
                    if (simList.size() == 0) {
                        // Hot swap and no card so go to settings
                        Xlog.d(TAG, "Hot swap_simList.size()=" + simList.size());
                        gobackSettings();
                    } else if (simList.size() == 1) {
                        if (simList.get(0).mSlot != mSimId) {
                            finish();
                        }
                    }
                }
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean airplaneMode = intent.getBooleanExtra("state", false);
                if (airplaneMode) {
                    mCellMgr.handleCellConn(mSimId,CellConnMgr.REQUEST_TYPE_SIMLOCK);
                    mPinDialog.setEnabled(false);
                    mPinToggle.setEnabled(false);
                }
            }
        }
    };


     ///M:  unlock sim pin/ me lock
     private Runnable mServiceComplete = new Runnable() {
         public void run() {
             int nRet = mCellMgr.getResult();
              if (mCellMgr.RESULT_OK != nRet && mCellMgr.RESULT_STATE_NORMAL != nRet){
                  Xlog.d(TAG, "mCell Mgr Result is not OK");
                  mIsShouldBeFinished = true;
                  IccLockSettings.this.finish();
                  return;
              } else {
                  Xlog.d(TAG, "mServiceComplete + Enable mPinToggle");
                  mPinToggle.setEnabled(true);
              }
              mIsUnlockFollow = false;
         }
     };
     // create unlock object
     private CellConnMgr mCellMgr = new CellConnMgr(mServiceComplete);    
    // For top-level settings screen to query
    boolean isIccLockEnabled() {
        //return PhoneFactory.getDefaultPhone().getIccCard().getIccLockEnabled();
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            return mPhone.getIccCard().getIccLockEnabled();
        } else {
            return mGeminiPhone.getIccCardGemini(mSimId).getIccLockEnabled();    
        }        
    }

    String getSummary(Context context) {
        Resources res = context.getResources();
        String summary = isIccLockEnabled()
                ? res.getString(R.string.sim_lock_on)
                : res.getString(R.string.sim_lock_off);
        return summary;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Xlog.d(TAG, "onCreate");
        if (Utils.isMonkeyRunning()) {
            finish();
            return;
        }
        addPreferencesFromResource(R.xml.sim_lock_settings);
        ///M: add by MTK
        mCellMgr.register(this);
        Intent it = getIntent();
        mSimId = it.getIntExtra("slotid", -1);
        
        Xlog.d(TAG, "mSimId is : " + mSimId);
        /* M: simId == -1 is not supproted happen and usually it is caused by
         * 3rd party app so to compatibilty add this catch
         * */
        if (mSimId == -1) {
            mSimId = dealWith3AppLaunch();
        }
        SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(this, mSimId);
        int simCount = SIMInfo.getInsertedSIMCount(this);
        String simDisplayName = null;
        if (simCount > 1 && simInfo != null) {
            simDisplayName = simInfo.mDisplayName;
        }
        if (simDisplayName != null && !simDisplayName.equals("")) {
            setTitle(simDisplayName);
        }


        mPinDialog = (EditPinPreference) findPreference(PIN_DIALOG);
        mPinToggle = (CheckBoxPreference) findPreference(PIN_TOGGLE);
        Xlog.d(TAG, "mDialogState is : " + mDialogState);
        if (savedInstanceState != null && savedInstanceState.containsKey(DIALOG_STATE)) {
            mDialogState = savedInstanceState.getInt(DIALOG_STATE);
            mPin = savedInstanceState.getString(DIALOG_PIN);
            mError = savedInstanceState.getString(DIALOG_ERROR);
            mToState = savedInstanceState.getBoolean(ENABLE_TO_STATE);
            Xlog.d(TAG, "mDialogState is : " + mDialogState);
            Xlog.d(TAG, "mPin is : " + mPin);
            Xlog.d(TAG, "mError is : " + mError);
            Xlog.d(TAG, "mToState  is : " + mToState);
            // Restore inputted PIN code
            switch (mDialogState) {
                case ICC_NEW_MODE:
                    mOldPin = savedInstanceState.getString(OLD_PINCODE);
                    Xlog.d(TAG, "mOldPin  is : " + mOldPin);
                    break;

                case ICC_REENTER_MODE:
                    mOldPin = savedInstanceState.getString(OLD_PINCODE);
                    mNewPin = savedInstanceState.getString(NEW_PINCODE);
                    Xlog.d(TAG, "mOldPin  is : " + mOldPin);
                    Xlog.d(TAG, "mNewPin   is : " + mNewPin);
                    break;

                case ICC_LOCK_MODE:
                case ICC_OLD_MODE:
                default:
                    break;
            }
        }

        mPinDialog.setOnPinEnteredListener(this);

        // Don't need any changes to be remembered
        getPreferenceScreen().setPersistent(false);

        ///M: modify for Gemini
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();            
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }
        mRes = getResources();
        /* M: add for AT&T custom  @ { */
		simRoamingExt = Utils.getSimRoamingExtPlugin(this);
        /* @ } */
        updatePreferences();
    }

    //M: add for hot swap
    private void gobackSettings() {
        Intent intent = new Intent(this,com.android.settings.Settings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent); 
        finish();   
    }
    private void updatePreferences() {
        mPinToggle.setChecked(isIccLockEnabled());
    }
    private void updateOnSimLockStateChanged() {
        Xlog.d(TAG, "updateOnSimLockStateChanged()+mIsDeadLocked="
                + mIsDeadLocked);
        if (getRetryPinCount() > 0 && mIsDeadLocked) {
            Xlog.d(TAG, "Restore state");
            mPinToggle.setEnabled(true);
            mIsDeadLocked = false;
            resetDialogState();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Xlog.d(TAG, "onResume");
        ///M: add by MTK
        Xlog.d(TAG, "mIsShouldBeFinished: " + mIsShouldBeFinished);
        if (mIsShouldBeFinished) {
            finish();
            return;
        }
        if (!mIsUnlockFollow) {
            mIsUnlockFollow = true;
            mPinToggle.setEnabled(false);
            if (getRetryPinCount() == 0 || getRetryPinCount() == GET_SIM_RETRY_EMPTY) {
                Xlog.d(TAG,"OnResume: postDelay call - handleCellConn 1");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCellMgr.handleCellConn(IccLockSettings.this.mSimId, CellConnMgr.REQUEST_TYPE_SIMLOCK | 0x80000000);
                        }
                    },500);
                
            } else {
                Xlog.d(TAG,"OnResume: postDelay call - handleCellConn 2");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCellMgr.handleCellConn(IccLockSettings.this.mSimId, CellConnMgr.REQUEST_TYPE_SIMLOCK);
                    }
                },500); 
            }
        }        
        // ACTION_SIM_STATE_CHANGED is sticky, so we'll receive current state after this call,
        // which will call updatePreferences().
        final IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        ///M: add for hot swap
        filter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mSimStateReceiver, filter);

        if (mDialogState != OFF_MODE) {
            showPinDialog();
        } else {
            // Prep for standard click on "Change PIN"
            resetDialogState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Xlog.d(TAG, "onPause");
        unregisterReceiver(mSimStateReceiver);
    }
    /*
    * M: add for unregister cellMgr
    *
    */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Xlog.d(TAG, "onDestroy");
        mCellMgr.unregister();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        // Need to store this state for slider open/close
        // There is one case where the dialog is popped up by the preference
        // framework. In that case, let the preference framework store the
        // dialog state. In other cases, where this activity manually launches
        // the dialog, store the state of the dialog.
        if (mPinDialog.isDialogOpen()) {
            out.putInt(DIALOG_STATE, mDialogState);
            out.putString(DIALOG_PIN, mPinDialog.getEditText().getText().toString());
            out.putString(DIALOG_ERROR, mError);
            out.putBoolean(ENABLE_TO_STATE, mToState);
            out.putInt(SIM_ID,mSimId);

            // Save inputted PIN code
            switch (mDialogState) {
                case ICC_NEW_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    break;

                case ICC_REENTER_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    out.putString(NEW_PINCODE, mNewPin);
                    break;

                case ICC_LOCK_MODE:
                case ICC_OLD_MODE:
                default:
                    break;
            }
        } else {
            super.onSaveInstanceState(out);
        }
    }

    private void showPinDialog() {
        if (mDialogState == OFF_MODE) {
            return;
        }
        setDialogValues();

        mPinDialog.showPinDialog();
    }

    private void setDialogValues() {
        mPinDialog.setText(mPin);
        String message = "";
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                message = mRes.getString(R.string.sim_enter_pin);
                message = message + " " + mRes.getString(R.string.sim_enter_pin_hints);
                mPinDialog.setDialogTitle((mToState 
                        ? mRes.getString(R.string.sim_enable_sim_lock)
                        : mRes.getString(R.string.sim_disable_sim_lock)) + "  " + getRetryPin());
                break;
            case ICC_OLD_MODE:
                message = mRes.getString(R.string.sim_enter_old);
                message = message + " " + mRes.getString(R.string.sim_enter_pin_hints);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin) + "  " + getRetryPin());
                break;
            case ICC_NEW_MODE:
                message = mRes.getString(R.string.sim_enter_new);
                message = message + " " + mRes.getString(R.string.sim_enter_pin_hints);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case ICC_REENTER_MODE:
                message = mRes.getString(R.string.sim_reenter_new);
                message = message + " " + mRes.getString(R.string.sim_enter_pin_hints);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            default:
                break;
        }
        if (mError != null) {
            message = mError + "\n" + message;
            mError = null;
        }
        mPinDialog.setDialogMessage(message);
    }

    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (!positiveResult) {
            resetDialogState();
            return;
        }

        mPin = preference.getText();
        if (!reasonablePin(mPin)) {
            // inject error message and display dialog again
            mError = mRes.getString(R.string.sim_bad_pin);
            showPinDialog();
            return;
        }
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                tryChangeIccLockState();
                break;
            case ICC_OLD_MODE:
                mOldPin = mPin;
                mDialogState = ICC_NEW_MODE;
                mError = null;
                mPin = null;
                showPinDialog();
                break;
            case ICC_NEW_MODE:
                mNewPin = mPin;
                mDialogState = ICC_REENTER_MODE;
                mPin = null;
                showPinDialog();
                break;
            case ICC_REENTER_MODE:
                if (!mPin.equals(mNewPin)) {
                    mError = mRes.getString(R.string.sim_pins_dont_match);
                    mDialogState = ICC_NEW_MODE;
                    mPin = null;
                    showPinDialog();
                } else {
                    mError = null;
                    tryChangePin();
                }
                break;
            default:
                break;
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPinToggle) {
            // Get the new, preferred state
            mToState = mPinToggle.isChecked();
            // Flip it back and pop up pin dialog
            mPinToggle.setChecked(!mToState);
            mDialogState = ICC_LOCK_MODE;
            showPinDialog();
        } else if (preference == mPinDialog) {
            mDialogState = ICC_OLD_MODE;
            return false;
        }
        return true;
    }

    private void tryChangeIccLockState() {
        // Try to change icc lock. If it succeeds, toggle the lock state and
        // reset dialog state. Else inject error message and show dialog again.
        Message callback = Message.obtain(mHandler, MSG_ENABLE_ICC_PIN_COMPLETE);
        ///M: modify for gemini 
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            mPhone.getIccCard().setIccLockEnabled(mToState, mPin, callback);
            Xlog.d(TAG, "tryChangeIccLockState single sim");
            // Disable the setting till the response is received.
            mPinToggle.setEnabled(false);
        } else {
            mGeminiPhone.getIccCardGemini(mSimId).setIccLockEnabled(mToState, mPin, callback);     
            mPinToggle.setEnabled(false);
        }        

    }

    private void iccLockChanged(boolean success) {
        mPinToggle.setEnabled(true);
        if (success) {
            mPinToggle.setChecked(mToState);
            /* M: add for AT&T custom  @ { */
			simRoamingExt.showPinToast(mToState);
            /* @ } */
        } else {
            Toast.makeText(this, mRes.getString(R.string.sim_lock_failed), Toast.LENGTH_SHORT)
                    .show();
            ///M: add for solve a defect
            noRetryPinAvailable();       
        }
        resetDialogState();
    }

    /*
    * M: if no retry available then disable the selection
    */
    private boolean noRetryPinAvailable() {
        if (getRetryPinCount() == 0 || getRetryPinCount() == GET_SIM_RETRY_EMPTY) {
            Xlog.d(TAG,"getRetryPinCount() = " + getRetryPinCount());
            mPinToggle.setEnabled(false);
            mIsDeadLocked = true;
            return true;
        }    
        return false;
    }
    private void iccPinChanged(boolean success) {
        if (!success) {
            Toast.makeText(this, mRes.getString(R.string.sim_change_failed),
                    Toast.LENGTH_SHORT)
                    .show();
            ///M: add for fix bug
            noRetryPinAvailable();
        } else {
            Toast.makeText(this, mRes.getString(R.string.sim_change_succeeded),
                    Toast.LENGTH_SHORT)
                    .show();

        }
        resetDialogState();
    }

    private void tryChangePin() {
        Message callback = Message.obtain(mHandler, MSG_CHANGE_ICC_PIN_COMPLETE);
        ///M: modify for gemini 
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            mPhone.getIccCard().changeIccLockPassword(mOldPin,mNewPin, callback);
            Xlog.d(TAG, "tryChangePin single sim");
        } else {
            mGeminiPhone.getIccCardGemini(mSimId).changeIccLockPassword(mOldPin,mNewPin, callback);      
        }                
    }

    private boolean reasonablePin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH
                || pin.length() > MAX_PIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }

    private void resetDialogState() {
        mError = null;
        mDialogState = ICC_OLD_MODE; // Default for when Change PIN is clicked
        mPin = "";
        setDialogValues();
        mDialogState = OFF_MODE;
    }

    private String getRetryPin() {
        int mPinRetryCount = getRetryPinCount();
        switch (mPinRetryCount) {
        case GET_SIM_RETRY_EMPTY:
            return " ";
        default:
            Xlog.d(TAG, " retry pin " + getString(R.string.sim_remain,mPinRetryCount));
            return getString(R.string.sim_remain,mPinRetryCount);
        }
    }
    
    private int getRetryPinCount() {
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            return SystemProperties.getInt("gsm.sim.retry.pin1",
                    GET_SIM_RETRY_EMPTY);
        } else {
            if (PhoneConstants.GEMINI_SIM_1 == mSimId) {
                return SystemProperties.getInt("gsm.sim.retry.pin1",
                        GET_SIM_RETRY_EMPTY);
            } else if (PhoneConstants.GEMINI_SIM_2 == mSimId) {
                return SystemProperties.getInt("gsm.sim.retry.pin1.2",
                        GET_SIM_RETRY_EMPTY);
            } else if (PhoneConstants.GEMINI_SIM_3 == mSimId) {
                return SystemProperties.getInt("gsm.sim.retry.pin1.3",
                        GET_SIM_RETRY_EMPTY);
            } else if (PhoneConstants.GEMINI_SIM_4 == mSimId) {
                return SystemProperties.getInt("gsm.sim.retry.pin1.4",
                        GET_SIM_RETRY_EMPTY);
            } else {
                Xlog.e(TAG, "getRetryPinCount sim id error");
                return -1;
            }
        }
    }
    /**
     * M:   if the activity is launched by 3rd part app
     * and without any simid or sim slot information
     * to set a simid or sim slot automatically
     * @return the slot id, if two cards inserted return the slod id with
     * smaller, and if one card insterd only return this card, otherwise still
     * return -1
     */
    private int dealWith3AppLaunch() {
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
        int slotID;
        if (simList.size() == 0) {
            slotID = -1;
        } else if (simList.size() == 1) {
            slotID = simList.get(0).mSlot;
        } else {
            slotID = simList.get(0).mSlot;
            for (SIMInfo temp : simList) {
                if (slotID > temp.mSlot) {
                    slotID = temp.mSlot;
                }
            }
        }
        Xlog.d(TAG, "dealWith3AppLaunch() slotID=" + slotID);
        return slotID;
    }
}
