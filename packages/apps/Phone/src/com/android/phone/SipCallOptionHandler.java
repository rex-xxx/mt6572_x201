/**
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

package com.android.phone;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.sip.SipProfileDb;
import com.android.phone.sip.SipSettings;
import com.android.phone.sip.SipSharedPreferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

/**
 * Activity that selects the proper phone type for an outgoing call.
 *
 * This activity determines which Phone type (SIP or PSTN) should be used
 * for an outgoing phone call, depending on the outgoing "number" (which
 * may be either a PSTN number or a SIP address) as well as the user's SIP
 * preferences.  In some cases this activity has no interaction with the
 * user, but in other cases it may (by bringing up a dialog if the user's
 * preference is "Ask for each call".)
 */
public class SipCallOptionHandler extends Activity implements
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
        CompoundButton.OnCheckedChangeListener {
    static final String TAG = "SipCallOptionHandler";
    private static final boolean DBG = true;
            //(PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    static final int DIALOG_SELECT_PHONE_TYPE = 0;
    static final int DIALOG_SELECT_OUTGOING_SIP_PHONE = 1;
    static final int DIALOG_START_SIP_SETTINGS = 2;
    static final int DIALOG_NO_INTERNET_ERROR = 3;
    static final int DIALOG_NO_VOIP = 4;
    static final int DIALOG_SIZE = 5;

    private Intent mIntent;
    private List<SipProfile> mProfileList;
    private String mCallOption;
    private String mNumber;
    private SipSharedPreferences mSipSharedPreferences;
    private SipProfileDb mSipProfileDb;
    private Dialog[] mDialogs = new Dialog[DIALOG_SIZE];
    private SipProfile mOutgoingSipProfile;
    private TextView mUnsetPriamryHint;
    private boolean mUseSipPhone = false;
    private boolean mMakePrimary = false;
    //private ProgressDialog mProgressDialog;

    //protected CellConnMgr mCellConnMgr;
    //protected Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();

        // This activity is only ever launched with the
        // ACTION_SIP_SELECT_PHONE action.
        if (!OutgoingCallBroadcaster.ACTION_SIP_SELECT_PHONE.equals(action)) {
            Log.wtf(TAG, "onCreate: got intent action '" + action + "', expected "
                    + OutgoingCallBroadcaster.ACTION_SIP_SELECT_PHONE);
            finish();
            return;
        }

        // mIntent is a copy of the original CALL intent that started the
        // whole outgoing-call sequence.  This intent will ultimately be
        // passed to CallController.placeCall() after displaying the SIP
        // call options dialog (if necessary).
        mIntent = (Intent) intent.getParcelableExtra(OutgoingCallBroadcaster.EXTRA_NEW_CALL_INTENT);
        if (mIntent == null) {
            finish();
            return;
        }

        //mCellConnMgr = PhoneGlobals.getInstance().cellConnMgr;
        //mContext = this;
        
        // Allow this activity to be visible in front of the keyguard.
        // (This is only necessary for obscure scenarios like the user
        // initiating a call and then immediately pressing the Power
        // button.)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // If we're trying to make a SIP call, return a SipPhone if one is
        // available.
        //
        // - If it's a sip: URI, this is definitely a SIP call, regardless
        //   of whether the data is a SIP address or a regular phone
        //   number.
        //
        // - If this is a tel: URI but the data contains an "@" character
        //   (see PhoneNumberUtils.isUriNumber()) we consider that to be a
        //   SIP number too.
        //
        // TODO: Eventually we may want to disallow that latter case
        //       (e.g. "tel:foo@example.com").
        //
        // TODO: We should also consider moving this logic into the
        //       CallManager, where it could be made more generic.
        //       (For example, each "telephony provider" could be allowed
        //       to register the URI scheme(s) that it can handle, and the
        //       CallManager would then find the best match for every
        //       outgoing call.)

        boolean voipSupported = PhoneUtils.isVoipSupported();
        if (DBG) Log.v(TAG, "voipSupported: " + voipSupported);
        mSipProfileDb = new SipProfileDb(this);
        mSipSharedPreferences = new SipSharedPreferences(this);
        mCallOption = mSipSharedPreferences.getSipCallOption();
        if (DBG) Log.v(TAG, "Call option: " + mCallOption);
        Uri uri = mIntent.getData();
        String scheme = uri.getScheme();
        mNumber = PhoneNumberUtils.getNumberFromIntent(mIntent, this);
        boolean isInCellNetwork = PhoneGlobals.getInstance().phoneMgr.isRadioOn();
        boolean isKnownCallScheme = Constants.SCHEME_TEL.equals(scheme)
                || Constants.SCHEME_SIP.equals(scheme);
        boolean isRegularCall = Constants.SCHEME_TEL.equals(scheme)
                && !PhoneNumberUtils.isUriNumber(mNumber);

        /// M: Fix CR:ALPS00362366, Delete white spaces for Sip call from contacts.
        ///        And add it to intent's EXTRA_ACTUAL_NUMBER_TO_DIAL.
        ///    delete whitespace for number from DialPad is done in com.mediatek.phone.OutgoingCallReceiver.java
        ///    delete whitespace for normal call from contacts is done in
        ///        com.android.phone.OutgoingCallBroadcaster.OutgoingCallReceiver.doReceive();
        ///    But Sip call from contacts do not enter the above methods, so add delete operation here.
        addActualNumberForSipCall();

        // Bypass the handler if the call scheme is not sip or tel.
        if (!isKnownCallScheme) {
            setResultAndFinish();
            return;
        }

        // Check if VoIP feature is supported.
        if (!voipSupported) {
            if (!isRegularCall) {
                showDialog(DIALOG_NO_VOIP);
            } else {
                setResultAndFinish();
            }
            return;
        }

        // Since we are not sure if anyone has touched the number during
        // the NEW_OUTGOING_CALL broadcast, we just check if the provider
        // put their gateway information in the intent. If so, it means
        // someone has changed the destination number. We then make the
        // call via the default pstn network. However, if one just alters
        // the destination directly, then we still let it go through the
        // Internet call option process.
        if (!PhoneUtils.hasPhoneProviderExtras(mIntent)) {
            if (!isNetworkConnected()) {
                /**
                 * change feature by mediatek .inc
                 * description : for gemini support, start SipCallOptionHandler means
                 * that we will do make a sip call, so show the non-internet error dialog
                 */
                if (!isRegularCall /*|| FeatureOption.MTK_GEMINI_SUPPORT*/) {
                    showDialog(DIALOG_NO_INTERNET_ERROR);
                    return;
                }
            } else {
                /**
                 * change feature by mediatek .inc
                 * description : always use sip for mtk call here
                 */
                //if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    mUseSipPhone = true;
                /*} else {
                    if (mCallOption.equals(Settings.Global.SIP_ASK_ME_EACH_TIME)
                            && isRegularCall && isInCellNetwork) {
                        showDialog(DIALOG_SELECT_PHONE_TYPE);
                        return;
                    }
                    if (!mCallOption.equals(Settings.Global.SIP_ADDRESS_ONLY)
                            || !isRegularCall) {
                        mUseSipPhone = true;
                    }
                }*/
            }
        }

        if (mUseSipPhone) {
            // If there is no sip profile and it is a regular call, then we
            // should use pstn network instead.
            if ((mSipProfileDb.getProfilesCount() > 0) || !isRegularCall) {
                startGetPrimarySipPhoneThread();
                return;
            } else {
                mUseSipPhone = false;
            }
        }
        setResultAndFinish();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing()) {
            return;
        }
        for (Dialog dialog : mDialogs) {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        //dismissProgressIndication();
        finish();
    }

    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case DIALOG_SELECT_PHONE_TYPE:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.pick_outgoing_call_phone_type)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setSingleChoiceItems(R.array.phone_type_values, -1, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        case DIALOG_SELECT_OUTGOING_SIP_PHONE:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.pick_outgoing_sip_phone)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setSingleChoiceItems(getProfileNameArray(), -1, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            addMakeDefaultCheckBox(dialog);
            break;
        case DIALOG_START_SIP_SETTINGS:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_sip_account_found_title)
                    .setMessage(R.string.no_sip_account_found)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(R.string.sip_menu_add, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        case DIALOG_NO_INTERNET_ERROR:
            boolean wifiOnly = SipManager.isSipWifiOnly(this);
            dialog = new AlertDialog.Builder(this)
                    .setTitle(wifiOnly ? R.string.no_wifi_available_title
                                       : R.string.no_internet_available_title)
                    .setMessage(wifiOnly ? R.string.no_wifi_available
                                         : R.string.no_internet_available)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(android.R.string.ok, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        case DIALOG_NO_VOIP:
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_voip)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(android.R.string.ok, this)
                    .setOnCancelListener(this)
                    .create();
            break;
        default:
            dialog = null;
        }
        if (dialog != null) {
            mDialogs[id] = dialog;
        }
        return dialog;
    }

    private void addMakeDefaultCheckBox(Dialog dialog) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(
                com.android.internal.R.layout.always_use_checkbox, null);
        CheckBox makePrimaryCheckBox =
                (CheckBox)view.findViewById(com.android.internal.R.id.alwaysUse);
        makePrimaryCheckBox.setText(R.string.remember_my_choice);
        makePrimaryCheckBox.setOnCheckedChangeListener(this);
        mUnsetPriamryHint = (TextView)view.findViewById(
                com.android.internal.R.id.clearDefaultHint);
        mUnsetPriamryHint.setText(R.string.reset_my_choice_hint);
        mUnsetPriamryHint.setVisibility(View.GONE);
        ((AlertDialog)dialog).setView(view);
    }

    private CharSequence[] getProfileNameArray() {
        CharSequence[] entries = new CharSequence[mProfileList.size()];
        int i = 0;
        for (SipProfile p : mProfileList) {
            entries[i++] = p.getProfileName();
        }
        return entries;
    }

    public void onClick(DialogInterface dialog, int id) {
        if (id == DialogInterface.BUTTON_NEGATIVE) {
            // button negative is cancel
            finish();
            return;
        } else if (dialog == mDialogs[DIALOG_SELECT_PHONE_TYPE]) {
            String selection = getResources().getStringArray(
                    R.array.phone_type_values)[id];
            if (DBG) Log.v(TAG, "User pick phone " + selection);
            if (selection.equals(getString(R.string.internet_phone))) {
                mUseSipPhone = true;
                startGetPrimarySipPhoneThread();
                return;
            }
        } else if (dialog == mDialogs[DIALOG_SELECT_OUTGOING_SIP_PHONE]) {
            mOutgoingSipProfile = mProfileList.get(id);
        } else if ((dialog == mDialogs[DIALOG_NO_INTERNET_ERROR])
                || (dialog == mDialogs[DIALOG_NO_VOIP])) {
            finish();
            return;
        } else {
            if (id == DialogInterface.BUTTON_POSITIVE) {
                // Redirect to sip settings and drop the call.
                Intent newIntent = new Intent(this, SipSettings.class);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent);
            }
            finish();
            return;
        }
        setResultAndFinish();
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mMakePrimary = isChecked;
        if (isChecked) {
            mUnsetPriamryHint.setVisibility(View.VISIBLE);
        } else {
            mUnsetPriamryHint.setVisibility(View.INVISIBLE);
        }
    }

    private void createSipPhoneIfNeeded(SipProfile p) {
        CallManager cm = PhoneGlobals.getInstance().mCM;
        if (PhoneUtils.getSipPhoneFromUri(cm, p.getUriString()) != null) return;

        // Create the phone since we can not find it in CallManager
        try {
            SipManager.newInstance(this).open(p);
            Phone phone = PhoneFactory.makeSipPhone(p.getUriString());
            if (phone != null) {
                cm.registerPhone(phone);
            } else {
                Log.e(TAG, "cannot make sipphone profile" + p);
            }
        } catch (SipException e) {
            Log.e(TAG, "cannot open sip profile" + p, e);
        }
    }

    private void setResultAndFinish() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mOutgoingSipProfile != null) {
                    if (!isNetworkConnected()) {
                        showDialog(DIALOG_NO_INTERNET_ERROR);
                        return;
                    }
                    if (DBG) {
                        Log.v(TAG, "primary SIP URI is " +
                            mOutgoingSipProfile.getUriString());
                    }
                    createSipPhoneIfNeeded(mOutgoingSipProfile);
                    mIntent.putExtra(OutgoingCallBroadcaster.EXTRA_SIP_PHONE_URI,
                            mOutgoingSipProfile.getUriString());
                    if (mMakePrimary) {
                        mSipSharedPreferences.setPrimaryAccount(
                                mOutgoingSipProfile.getUriString());
                    }
                }

                if (mUseSipPhone && mOutgoingSipProfile == null) {
                    showDialog(DIALOG_START_SIP_SETTINGS);
                    return;
                } else {
                    //if (mUseSipPhone) {
                    // Woo hoo -- it's finally OK to initiate the outgoing call!
                    // MTK single sim card MO process is same as multiple sim card one,
                    // So if here, only Sip call case
                        PhoneGlobals.getInstance().callController.placeCall(mIntent);
                    //} else {
                        //Single sim and not use sip phone, check the sim status firstly
                    //    handleVoiceCall();
                    //    return ;
                    //}
                }
                finish();
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if ((ni == null) || !ni.isConnected()) {
                return false;
            }

            return ((ni.getType() == ConnectivityManager.TYPE_WIFI)
                    || !SipManager.isSipWifiOnly(this));
        }
        return false;
    }

    private void startGetPrimarySipPhoneThread() {
        new Thread(new Runnable() {
            public void run() {
                getPrimarySipPhone();
            }
        }).start();
    }

    private void getPrimarySipPhone() {
        String primarySipUri = mSipSharedPreferences.getPrimaryAccount();

        mOutgoingSipProfile = getPrimaryFromExistingProfiles(primarySipUri);
        if (mOutgoingSipProfile == null) {
            if ((mProfileList != null) && (mProfileList.size() > 0)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showDialog(DIALOG_SELECT_OUTGOING_SIP_PHONE);
                    }
                });
                return;
            }
        }
        setResultAndFinish();
    }

    private SipProfile getPrimaryFromExistingProfiles(String primarySipUri) {
        mProfileList = mSipProfileDb.retrieveSipProfileList();
        if (mProfileList == null) {
            return null;
        }
        for (SipProfile p : mProfileList) {
            if (p.getUriString().equals(primarySipUri)) {
                return p;
            }
        }
        return null;
    }
    
    /*private boolean needToCheckSIMStatus() {
        if (!PhoneGlobals.getInstance().phoneMgr.isRadioOn()) {
            return true;
        }
        
        return TelephonyManager.getDefault().getSimState() != TelephonyManager.SIM_STATE_READY
                || roamingRequest(0);
    }
    
    private boolean roamingRequest(int slot) {
        log("roamingRequest slot = " + slot);
        boolean bRoaming = false;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            bRoaming = TelephonyManager.getDefault().isNetworkRoamingGemini(slot);
        } else {
            bRoaming = TelephonyManager.getDefault().isNetworkRoaming();
        }

        if (bRoaming) {
            log("roamingRequest slot = " + slot + " is roaming");
        } else {
            log("roamingRequest slot = " + slot + " is not roaming");
            return false;
        }

        if (0 == Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ROAMING_REMINDER_MODE_SETTING, -1)
                && isRoamingNeeded()) {
            log("roamingRequest reminder once and need to indicate");
            return true;
        }

        if (1 == Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ROAMING_REMINDER_MODE_SETTING, -1)) {
            log("roamingRequest reminder always");
            return true;
        }

        log("roamingRequest result = false");
        return false;
    }
    
    void handleVoiceCall() {
        if (needToCheckSIMStatus()) {
            final int result = mCellConnMgr.handleCellConn(0, CellConnMgr.REQUEST_TYPE_ROAMING, mRunnable);
            log("result = " + result);
            if (result == mCellConnMgr.RESULT_WAIT) {
                 showProgressIndication();
            }
        } else {
            afterCheckSIMStatus(com.mediatek.CellConnService.CellConnMgr.RESULT_STATE_NORMAL, 0);
            //PhoneGlobals.getInstance().callController.placeCall(mIntent);
            finish();
        }
    }
    
    private Runnable mRunnable = new Runnable() {
        public void run() {
            //Profiler.trace(Profiler.CallOptionHandlerEnterRun);
            final int result = mCellConnMgr.getResult();
//            final int slot = mCellConnMgr.getPreferSlot();
//            log("run, result = "+result+" slot = "+slot);
            dismissProgressIndication();

            final boolean bailout = afterCheckSIMStatus(result, 0);
            //Profiler.trace(Profiler.CallOptionHandlerLeaveRun);
            finish();
        }
    };
    
    private boolean afterCheckSIMStatus(int result, int slot) {
        log("afterCheckSIMStatus, result = " + result+" slot = " + slot);

        if (result != com.mediatek.CellConnService.CellConnMgr.RESULT_STATE_NORMAL) {
            return true;
        }

        // ip dial only support voice call
        if (!mIntent.getBooleanExtra(Constants.EXTRA_IS_VIDEO_CALL, false)
                && mIntent.getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)) {
            final String ipPrefix = CallOptionUtils.queryIPPrefix(this, slot, false);
            if (TextUtils.isEmpty(ipPrefix)) {
                //pop toast to notify user
                Toast.makeText(PhoneGlobals.getInstance(),
                        R.string.ip_dial_error_toast_for_no_ip_prefix_number, Toast.LENGTH_SHORT).show();
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(Constants.PHONE_PACKAGE, Constants.IP_PREFIX_SETTING_CLASS_NAME);
                mContext.startActivity(intent);
                return true;
            } else {
                if (mNumber.indexOf(ipPrefix) != 0) {
                    mIntent.putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, ipPrefix + mNumber);
                }
            }
        }

        PhoneGlobals.getInstance().callController.placeCall(mIntent);
        return true;
    }

    private boolean isRoamingNeeded() {
        log("isRoamingNeeded = " + SystemProperties.getBoolean("gsm.roaming.indicator.needed", false));
        return SystemProperties.getBoolean("gsm.roaming.indicator.needed", false);
    }
    
    /*private String queryIPPrefix(int slot) {
        //final SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slot);
        StringBuilder builder = new StringBuilder();
        builder.append("ipprefix");
        //builder.append(simInfo.mSimId);
        final String key = builder.toString();
        
        final String ipPrefix = Settings.Global.getString(mContext.getContentResolver(), key);
        log("queryIPPrefix, ipPrefix = " + ipPrefix);
        return ipPrefix;
    }*/

    /**
     * Show an onscreen "progress indication" with the specified title and message.
     */
    /*private void showProgressIndication() {
        if (DBG) {
            log("showProgressIndication(message )...");
        }

        // TODO: make this be a no-op if the progress indication is
        // already visible with the exact same title and message.

        dismissProgressIndication();  // Clean up any prior progress indication
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(mContext.getResources().getString(R.string.sum_search_networks));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mProgressDialog.show();
    }*/

    /**
     * Dismiss the onscreen "progress indication" (if present).
     */
    /*private void dismissProgressIndication() {
        if (DBG) {
            log("dismissProgressIndication()...");
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismiss(); // safe even if already dismissed
            mProgressDialog = null;
        }
    }*/

    /// M: Fix CR:ALPS00362366;
    ///    Delete white spaces in the Sip call number from contacts.
    ///    And add Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL into intent.
    private void addActualNumberForSipCall() {
        Uri uri = mIntent.getData();
        if (Constants.SCHEME_SIP.equals(uri.getScheme())
                    && !(PhoneNumberUtils.isUriNumber(mNumber))) {
            mNumber = PhoneNumberUtils.stripSeparators(mNumber);
            if (!TextUtils.isEmpty(mNumber)) {
                mIntent.putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, mNumber);
            }
        }
    }

    void log(String msg) {
        Log.i(TAG, msg);
    }
}
