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

package com.mediatek.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ListAdapter;

import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.EditPhoneNumberPreference;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VoiceMailSetting extends  PreferenceActivity
        implements DialogInterface.OnClickListener,
        Preference.OnPreferenceChangeListener,
        EditPhoneNumberPreference.OnDialogClosedListener,
        EditPhoneNumberPreference.GetDefaultNumberListener {

    // intent action to bring up voice mail settings
    public static final String ACTION_ADD_VOICEMAIL =
        "com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL";
    // intent action sent by this activity to a voice mail provider
    // to trigger its configuration UI
    public static final String ACTION_CONFIGURE_VOICEMAIL =
        "com.android.phone.CallFeaturesSetting.CONFIGURE_VOICEMAIL";
    // Extra put in the return from VM provider config containing voicemail number to set
    public static final String VM_NUMBER_EXTRA = "com.android.phone.VoicemailNumber";
    // Extra put in the return from VM provider config containing call forwarding number to set
    public static final String FWD_NUMBER_EXTRA = "com.android.phone.ForwardingNumber";
    // Extra put in the return from VM provider config containing call forwarding number to set
    public static final String FWD_NUMBER_TIME_EXTRA = "com.android.phone.ForwardingNumberTime";
    // If the VM provider returns non null value in this extra we will force the user to
    // choose another VM provider
    public static final String SIGNOUT_EXTRA = "com.android.phone.Signout";

    // Used to tell the saving logic to leave forwarding number as is
    public static final CallForwardInfo[] FWD_SETTINGS_DONT_TOUCH = null;
    // Suffix appended to provider key for storing vm number
    public static final String VM_NUMBER_TAG = "#VMNumber";
    // Suffix appended to provider key for storing forwarding settings
    public static final String FWD_SETTINGS_TAG = "#FWDSettings";
    // Suffix appended to forward settings key for storing length of settings array
    public static final String FWD_SETTINGS_LENGTH_TAG = "#Length";
    // Suffix appended to forward settings key for storing an individual setting
    public static final String FWD_SETTING_TAG = "#Setting";
    // Suffixes appended to forward setting key for storing an individual setting properties
    public static final String FWD_SETTING_STATUS = "#Status";
    public static final String FWD_SETTING_REASON = "#Reason";
    public static final String FWD_SETTING_NUMBER = "#Number";
    public static final String FWD_SETTING_TIME = "#Time";

    // Key identifying the default vocie mail provider
    // Add for gemini+
    private static final String[] DEFAULT_VM_PROVIDER_KEY = {"1","2","3","4"}; 

    // Extra put into ACTION_ADD_VOICEMAIL call to indicate which provider
    // to remove from the list of providers presented to the user
    public static final String IGNORE_PROVIDER_EXTRA = "com.android.phone.ProviderToIgnore";

    // debug data
    private static final String LOG_TAG = "Settings/VoiceMailSetting";
    private static final boolean DBG = true; 

    // string constants
    private static final String NUM_PROJECTION[] = {CommonDataKinds.Phone.NUMBER};

    // String keys for preference lookup
    private static final String BUTTON_VOICEMAIL_KEY = "button_voicemail_key";
    private static final String BUTTON_VOICEMAIL_PROVIDER_KEY = "button_voicemail_provider_key";
    private static final String BUTTON_VOICEMAIL_SETTING_KEY = "button_voicemail_setting_key";

    ///M: add for gemini+
    private static final String[] VM_NUMBERS_SHARED_PREFERENCES_NAME = 
    {"vm_numbers", "vm_numbers_sim2", "vm_numbers_sim3", "vm_numbers_sim4"};

    /** Event for Async voicemail change call */
    private static final int EVENT_VOICEMAIL_CHANGED        = 500;
    private static final int EVENT_FORWARDING_CHANGED       = 501;
    private static final int EVENT_FORWARDING_GET_COMPLETED = 502;

    // Dtmf tone types
    static final int DTMF_TONE_TYPE_NORMAL = 0;
    static final int DTMF_TONE_TYPE_LONG   = 1;

    /** Handle to voicemail pref */
    private static final int VOICEMAIL_PREF_ID = 1;
    private static final int VOICEMAIL_PROVIDER_CFG_ID = 2;

    private Phone mPhone;
    private int mSimId;
    private String mInitTitle = null;

    private static final int VM_NOCHANGE_ERROR = 400;
    private static final int VM_RESPONSE_ERROR = 500;
    private static final int FW_SET_RESPONSE_ERROR = 501;
    private static final int FW_GET_RESPONSE_ERROR = 502;

    // dialog identifiers for voicemail
    private static final int VOICEMAIL_DIALOG_CONFIRM = 600;
    private static final int VOICEMAIL_FWD_SAVING_DIALOG = 601;
    private static final int VOICEMAIL_FWD_READING_DIALOG = 602;
    private static final int VOICEMAIL_REVERTING_DIALOG = 603;

    // special statuses for voicemail controls.
    private static final int MSG_VM_EXCEPTION = 400;
    private static final int MSG_FW_SET_EXCEPTION = 401;
    private static final int MSG_FW_GET_EXCEPTION = 402;
    private static final int MSG_VM_OK = 600;
    private static final int MSG_VM_NOCHANGE = 700;
    private final BroadcastReceiver mReceiver = new CallFeaturesSettingBroadcastReceiver();

    private EditPhoneNumberPreference mSubMenuVoicemailSettings;

    private ListPreference mVoicemailProviders;
    private PreferenceScreen mVoicemailSettings;
    
    //Add for Gemini enhancement

    private class VoiceMailProvider {
        public VoiceMailProvider(String name, Intent intent) {
            this.mName = name;
            this.mIntent = intent;
        }
        public String mName;
        public Intent mIntent;
    }

    /**
     * Forwarding settings we are going to save.
     */
    static final int [] FORWARDING_SETTINGS_REASONS = new int[] {
        CommandsInterface.CF_REASON_UNCONDITIONAL,
        CommandsInterface.CF_REASON_BUSY,
        CommandsInterface.CF_REASON_NO_REPLY,
        CommandsInterface.CF_REASON_NOT_REACHABLE
    };

    private class VoiceMailProviderSettings {
        /**
         * Constructs settings object, setting all conditional forwarding to the specified number
         */
        public VoiceMailProviderSettings(String voicemailNumber, String forwardingNumber,
                int timeSeconds) {
            mVoicemailNumber = voicemailNumber;
            if (forwardingNumber == null || forwardingNumber.length() == 0) {
                mForwardingSettings = FWD_SETTINGS_DONT_TOUCH;
            } else {
                mForwardingSettings = new CallForwardInfo[FORWARDING_SETTINGS_REASONS.length];
                for (int i = 0; i < mForwardingSettings.length; i++) {
                    CallForwardInfo fi = new CallForwardInfo();
                    mForwardingSettings[i] = fi;
                    fi.reason = FORWARDING_SETTINGS_REASONS[i];
                    fi.status = (fi.reason == CommandsInterface.CF_REASON_UNCONDITIONAL) ? 0 : 1;
                    fi.serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
                    fi.toa = PhoneNumberUtils.TOA_International;
                    fi.number = forwardingNumber;
                    fi.timeSeconds = timeSeconds;
                }
            }
        }

        public VoiceMailProviderSettings(String voicemailNumber, CallForwardInfo[] infos) {
            mVoicemailNumber = voicemailNumber;
            mForwardingSettings = infos;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (!(o instanceof VoiceMailProviderSettings)) {
                return false;
            }
            final VoiceMailProviderSettings v = (VoiceMailProviderSettings)o;

            return ((mVoicemailNumber == null && v.mVoicemailNumber == null) 
                    || mVoicemailNumber != null && mVoicemailNumber.equals(v.mVoicemailNumber))
                    && forwardingSettingsEqual(mForwardingSettings,
                            v.mForwardingSettings);
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        private boolean forwardingSettingsEqual(CallForwardInfo[] infos1,
                CallForwardInfo[] infos2) {
            if (infos1 == infos2) {
                return true;
            }
            if (infos1 == null || infos2 == null) {
                return false;
            }
            if (infos1.length != infos2.length) {
                return false;
            }
            for (int i = 0; i < infos1.length; i++) {
                CallForwardInfo i1 = infos1[i];
                CallForwardInfo i2 = infos2[i];
                if (i1.status != i2.status ||
                    i1.reason != i2.reason ||
                    i1.serviceClass != i2.serviceClass ||
                    i1.toa != i2.toa ||
                    i1.number != i2.number ||
                    i1.timeSeconds != i2.timeSeconds) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return mVoicemailNumber + ((mForwardingSettings != null) ? (", " +
                    mForwardingSettings.toString()) : "");
        }

        public String mVoicemailNumber;
        public CallForwardInfo[] mForwardingSettings;
    }

    SharedPreferences mPerProviderSavedVMNumbers;

    /**
     * Results of reading forwarding settings
     */
    CallForwardInfo[] mForwardingReadResults = null;

    /**
     * Result of forwarding number change.
     * Keys are reasons (eg. unconditional forwarding).
     */
    private Map<Integer, AsyncResult> mForwardingChangeResults = null;

    /**
     * Expected CF read result types.
     * This set keeps track of the CF types for which we've issued change
     * commands so we can tell when we've received all of the responses.
     */
    private Collection<Integer> mExpectedChangeResultReasons = null;

    /**
     * Result of vm number change
     */
    AsyncResult mVoicemailChangeResult = null;

    /**
     * Previous VM provider setting so we can return to it in case of failure.
     */
    String mPreviousVMProviderKey = null;

    /**
     * Id of the dialog being currently shown.
     */
    int mCurrentDialogId = 0;

    /**
     * Flag indicating that we are invoking settings for the voicemail provider programmatically
     * due to vm provider change.
     */
    boolean mVMProviderSettingsForced = false;

    /**
     * Flag indicating that we are making changes to vm or fwd numbers
     * due to vm provider change.
     */
    boolean mChangingVMorFwdDueToProviderChange = false;

    /**
     * True if we are in the process of vm & fwd number change and vm has already been changed.
     * This is used to decide what to do in case of rollback.
     */
    boolean mVMChangeCompletedSuccesfully = false;

    /**
     * True if we had full or partial failure setting forwarding numbers and so need to roll them
     * back.
     */
    boolean mFwdChangesRequireRollback = false;

    /**
     * Id of error msg to display to user once we are done reverting the VM provider to the previous
     * one.
     */
    int mVMOrFwdSetError = 0;

    /**
     * Data about discovered voice mail settings providers.
     * Is populated by querying which activities can handle ACTION_CONFIGURE_VOICEMAIL.
     * They key in this map is package name + activity name.
     * We always add an entry for the default provider with a key of empty
     * string and intent value of null.
     * @see #initVoiceMailProviders.
     */
    private final Map<String, VoiceMailProvider> mVMProvidersData =
        new HashMap<String, VoiceMailProvider>();

    /** string to hold old voicemail number as it is being updated. */
    private String mOldVmNumber;

    // New call forwarding settings and vm number we will be setting
    // Need to save these since before we get to saving we need to asynchronously
    // query the existing forwarding settings.
    private CallForwardInfo[] mNewFwdSettings;
    String mNewVMNumber;

    private boolean mForeground;

    @Override
    public void onPause() {
        super.onPause();
        mForeground = false;
    }

    /**
     * We have to pull current settings from the network for all kinds of
     * voicemail providers so we can tell whether we have to update them,
     * so use this bit to keep track of whether we're reading settings for the
     * default provider and should therefore save them out when done.
     */
    private boolean mReadingSettingsForDefaultProvider = false;

    /*
     * Click Listeners, handle click based on objects attached to UI.
     */

    // Click listener for all toggle events
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mVoicemailSettings && preference.getIntent() != null) {
            if (DBG) {
                log("Invoking cfg intent " + preference.getIntent().getPackage());
            }
            startActivityForResult(preference.getIntent(), VOICEMAIL_PROVIDER_CFG_ID);
            return true;
        }
        return false;
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes.
     *
     * @param preference is the preference to be changed
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mVoicemailProviders) {
            final String currentProviderKey = getCurrentVoicemailProviderKey();
            mPreviousVMProviderKey = currentProviderKey;
            final String newProviderKey = (String)objValue;
            if (DBG) {
                log("VM provider changes to " + newProviderKey + " from " +
                    mPreviousVMProviderKey);
            }
            if (mPreviousVMProviderKey.equals(newProviderKey)) {
                if (DBG) {
                    log("No change ");
                }
                return true;
            }
            
            //Save the current setting for dualsim
            if (CallSettings.isMultipleSim()) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(BUTTON_VOICEMAIL_PROVIDER_KEY + mSimId, newProviderKey);
                editor.commit();
            }
            
            updateVMPreferenceWidgets(newProviderKey);

            mPreviousVMProviderKey = currentProviderKey;

            final VoiceMailProviderSettings newProviderSettings =
                    loadSettingsForVoiceMailProvider(newProviderKey);

            // If the user switches to a voice mail provider and we have a
            // numbers stored for it we will automatically change the
            // phone's
            // voice mail and forwarding number to the stored ones.
            // Otherwise we will bring up provider's configuration UI.

            if (newProviderSettings == null) {
                // Force the user into a configuration of the chosen provider
                if (DBG) {
                    log("Saved preferences not found - invoking config");
                }
                mVMProviderSettingsForced = true;
            } else {
                if (DBG) {
                    log("Saved preferences found - switching to them");
                }
                // Set this flag so if we get a failure we revert to previous provider
                mChangingVMorFwdDueToProviderChange = true;
                saveVoiceMailAndForwardingNumber(newProviderKey, newProviderSettings);
            }
        } 
        // always let the preference setting proceed.
        return true;
    }

    // Preference click listener invoked on OnDialogClosed for EditPhoneNumberPreference.
    public void onDialogClosed(EditPhoneNumberPreference preference, int buttonClicked) {
        if (DBG) {
            log("onPreferenceClick: request preference click on dialog close: " +
                buttonClicked);
        }
        if (buttonClicked == DialogInterface.BUTTON_NEGATIVE) {
            return;
        }
        if (preference instanceof EditPhoneNumberPreference) {
            EditPhoneNumberPreference epn = preference;

            if (epn == mSubMenuVoicemailSettings) {
                handleVMBtnClickRequest();
            }
        }
    }

    /**
     * Implemented for EditPhoneNumberPreference.GetDefaultNumberListener.
     * This method set the default values for the various
     * EditPhoneNumberPreference dialogs.
     */
    public String onGetDefaultNumber(EditPhoneNumberPreference preference) {
        if (preference == mSubMenuVoicemailSettings) {
            // update the voicemail number field, which takes care of the
            // mSubMenuVoicemailSettings itself, so we should return null.
            if (DBG) {
                log("updating default for voicemail dialog");
            }
            updateVoiceNumberField();
            return null;
        }

        String vmDisplay;
        if (CallSettings.isMultipleSim()) {
            vmDisplay = ((GeminiPhone)mPhone).getVoiceMailNumberGemini(mSimId);
        } else {
            vmDisplay = mPhone.getVoiceMailNumber();
        }

        if (TextUtils.isEmpty(vmDisplay)) {
            // if there is no voicemail number, we just return null to
            // indicate no contribution.
            return null;
        }

        // Return the voicemail number prepended with "VM: "
        if (DBG) {
            log("updating default for call forwarding dialogs");
        }
        log("ongetDefaultnumber, vmDisplay:" + vmDisplay);
        return getString(R.string.voicemail_abbreviated) + " " + vmDisplay;
    }


    // override the startsubactivity call to make changes in state consistent.
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode == -1) {
            // this is an intent requested from the preference framework.
            super.startActivityForResult(intent, requestCode);
            return;
        }

        if (DBG) {
            log("startSubActivity: starting requested subactivity");
        }
        super.startActivityForResult(intent, requestCode);
    }

    private void switchToPreviousVoicemailProvider() {
        if (DBG) {
            log("switchToPreviousVoicemailProvider " + mPreviousVMProviderKey);
        }
        if (mPreviousVMProviderKey != null) {
            if (mVMChangeCompletedSuccesfully || mFwdChangesRequireRollback) {
                // we have to revert with carrier
                showDialogIfForeground(VOICEMAIL_REVERTING_DIALOG);
                VoiceMailProviderSettings prevSettings =
                    loadSettingsForVoiceMailProvider(mPreviousVMProviderKey);
                if (prevSettings == null) {
                    return;
                }
                if (mVMChangeCompletedSuccesfully) {
                    mNewVMNumber = prevSettings.mVoicemailNumber;
                    if (DBG) {
                        log("have to revert VM to " + mNewVMNumber);
                    }

                    if (CallSettings.isMultipleSim()) {
                        ((GeminiPhone)mPhone).setVoiceMailNumberGemini(
                                ((GeminiPhone)mPhone).getVoiceMailAlphaTagGemini(mSimId).toString(),
                                mNewVMNumber,
                                Message.obtain(mRevertOptionComplete, EVENT_VOICEMAIL_CHANGED), mSimId);
                    
                    } else {
                        mPhone.setVoiceMailNumber(
                                mPhone.getVoiceMailAlphaTag().toString(),
                                mNewVMNumber,
                                Message.obtain(mRevertOptionComplete, EVENT_VOICEMAIL_CHANGED));
                    }
                }
                if (mFwdChangesRequireRollback) {
                    if (DBG) {
                        log("have to revert fwd");
                    }
                    final CallForwardInfo[] prevFwdSettings =
                        prevSettings.mForwardingSettings;
                    if (prevFwdSettings != null) {
                        Map<Integer, AsyncResult> results =
                            mForwardingChangeResults;
                        resetForwardingChangeState();
                        for (int i = 0; i < prevFwdSettings.length; i++) {
                            CallForwardInfo fi = prevFwdSettings[i];
                            if (DBG) {
                                log("Reverting fwd #: " + i + ": " + fi.toString());
                            }
                            // Only revert the settings for which the update
                            // succeeded
                            AsyncResult result = results.get(fi.reason);
                            if (result != null && result.exception == null) {
                                mExpectedChangeResultReasons.add(fi.reason);

                                if (CallSettings.isMultipleSim()) {                           
                                    ((GeminiPhone)mPhone).setCallForwardingOptionGemini((fi.status == 1 ?
                                                    CommandsInterface.CF_ACTION_REGISTRATION :
                                                    CommandsInterface.CF_ACTION_DISABLE),
                                            fi.reason,
                                            fi.number,
                                            fi.timeSeconds,
                                            mRevertOptionComplete.obtainMessage(
                                                    EVENT_FORWARDING_CHANGED, i, 0), mSimId);                           
                                } else {
                                    mPhone.setCallForwardingOption((fi.status == 1 ?
                                                CommandsInterface.CF_ACTION_REGISTRATION :
                                                CommandsInterface.CF_ACTION_DISABLE),
                                        fi.reason,
                                        fi.number,
                                        fi.timeSeconds,
                                        mRevertOptionComplete.obtainMessage(
                                                EVENT_FORWARDING_CHANGED, i, 0));
                                }
                            }
                        }
                    }
                }
            } else {
                if (DBG) {
                    log("No need to revert");
                }
                onRevertDone();
            }
        }
    }

    void onRevertDone() {
        if (DBG) {
            log("Flipping provider key back to " + mPreviousVMProviderKey);
        }
        mVoicemailProviders.setValue(mPreviousVMProviderKey);
        updateVMPreferenceWidgets(mPreviousVMProviderKey);
        updateVoiceNumberField();
        if (mVMOrFwdSetError != 0) {
            showVMDialog(mVMOrFwdSetError);
            mVMOrFwdSetError = 0;
        }
    }

    // asynchronous result call after contacts are selected or after we return from
    // a call to the VM settings provider.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // there are cases where the contact picker may end up sending us more than one
        // request.  We want to ignore the request if we're not in the correct state.
        if (requestCode ==  VOICEMAIL_PROVIDER_CFG_ID) {
            boolean failure = false;

            // No matter how the processing of result goes lets clear the flag
            if (DBG) {
                log("mVMProviderSettingsForced: " + mVMProviderSettingsForced);
            }
            final boolean isVMProviderSettingsForced = mVMProviderSettingsForced;
            mVMProviderSettingsForced = false;

            String vmNum = null;
            if (resultCode != RESULT_OK) {
                if (DBG) {
                    log("onActivityResult: vm provider cfg result not OK.");
                }
                failure = true;
            } else {
                if (data == null) {
                    if (DBG) {
                        log("onActivityResult: vm provider cfg result has no data");
                    }
                    failure = true;
                } else {
                    if (data.getBooleanExtra(SIGNOUT_EXTRA, false)) {
                        if (DBG) {
                            log("Provider requested signout");
                        }
                        if (isVMProviderSettingsForced) {
                            if (DBG) {
                                log("Going back to previous provider on signout");
                            }
                            switchToPreviousVoicemailProvider();
                        } else {
                            final String victim = getCurrentVoicemailProviderKey();
                            if (DBG) {
                                log("Relaunching activity and ignoring " + victim);
                            }
                            Intent i = new Intent(ACTION_ADD_VOICEMAIL);
                            i.putExtra(IGNORE_PROVIDER_EXTRA, victim);
                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                        return;
                    }
                    vmNum = data.getStringExtra(VM_NUMBER_EXTRA);
                    if (vmNum == null || vmNum.length() == 0) {
                        if (DBG) {
                            log("onActivityResult: vm provider cfg result has no vmnum");
                        }
                        failure = true;
                    }
                }
            }
            if (failure) {
                if (DBG) {
                    log("Failure in return from voicemail provider");
                }
                if (isVMProviderSettingsForced) {
                    switchToPreviousVoicemailProvider();
                } else {
                    if (DBG) {
                        log("Not switching back the provider since this is not forced config");
                    }
                }
                return;
            }
            mChangingVMorFwdDueToProviderChange = isVMProviderSettingsForced;
            final String fwdNum = data.getStringExtra(FWD_NUMBER_EXTRA);

            // send it to the provider when it's config is invoked so it can use this as default
            final int fwdNumTime = data.getIntExtra(FWD_NUMBER_TIME_EXTRA, 20);

            if (DBG) {
                log("onActivityResult: vm provider cfg result " +
                        (fwdNum != null ? "has" : " does not have") + " forwarding number");
            }
            saveVoiceMailAndForwardingNumber(getCurrentVoicemailProviderKey(),
                    new VoiceMailProviderSettings(vmNum, fwdNum, fwdNumTime));
            return;
        }

        if (resultCode != RESULT_OK) {
            if (DBG) {
                log("onActivityResult: contact picker result not OK.");
            }
            return;
        }

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(data.getData(),
                    NUM_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                switch (requestCode) {
                case VOICEMAIL_PREF_ID:
                    mSubMenuVoicemailSettings.onPickActivityResult(cursor.getString(0));
                    break;
                default:
                    break;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Voicemail button logic
    private void handleVMBtnClickRequest() {
        // normally called on the dialog close.

        // Since we're stripping the formatting out on the getPhoneNumber()
        // call now, we won't need to do so here anymore.

        saveVoiceMailAndForwardingNumber(
                getCurrentVoicemailProviderKey(),
                new VoiceMailProviderSettings(mSubMenuVoicemailSettings.getPhoneNumber(),
                        FWD_SETTINGS_DONT_TOUCH));
    }


    /**
     * Wrapper around showDialog() that will silently do nothing if we're
     * not in the foreground.
     *
     * This is useful here because most of the dialogs we display from
     * this class are triggered by asynchronous events (like
     * success/failure messages from the telephony layer) and it's
     * possible for those events to come in even after the user has gone
     * to a different screen.
     */
    // TODO: this is too brittle: it's still easy to accidentally add new
    // code here that calls showDialog() directly (which will result in a
    // WindowManager$BadTokenException if called after the activity has
    // been stopped.)
    //
    // It would be cleaner to do the "if (mForeground)" check in one
    // central place, maybe by using a single Handler for all asynchronous
    // events (and have *that* discard events if we're not in the
    // foreground.)
    //
    // Unfortunately it's not that simple, since we sometimes need to do
    // actual work to handle these events whether or not we're in the
    // foreground (see the Handler code in mSetOptionComplete for
    // example.)
    private void showDialogIfForeground(int id) {
        if (mForeground) {
            showDialog(id);
        }
    }

    private void dismissDialogSafely(int id) {
        try {
            dismissDialog(id);
        } catch (IllegalArgumentException e) {
            log("IllegalArgumentException");
        }
    }

    private void saveVoiceMailAndForwardingNumber(String key,
            VoiceMailProviderSettings newSettings) {
        if (DBG) {
            log("saveVoiceMailAndForwardingNumber: " + newSettings.toString());
        }
        mNewVMNumber = newSettings.mVoicemailNumber;
        // empty vm number == clearing the vm number ?
        if (mNewVMNumber == null) {
            mNewVMNumber = "";
        }

        mNewFwdSettings = newSettings.mForwardingSettings;
        if (DBG) {
            log("newFwdNumber " +
                String.valueOf((mNewFwdSettings != null ? mNewFwdSettings.length : 0))
                + " settings");
        }

        // No fwd settings on CDMA
        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            if (DBG) {
                log("ignoring forwarding setting since this is CDMA phone");
            }
            mNewFwdSettings = FWD_SETTINGS_DONT_TOUCH;
        }

        //throw a warning if the vm is the same and we do not touch forwarding.
        if (mNewVMNumber.equals(mOldVmNumber) && mNewFwdSettings == FWD_SETTINGS_DONT_TOUCH) {
            showVMDialog(MSG_VM_NOCHANGE);
            return;
        }

        maybeSaveSettingsForVoicemailProvider(key, newSettings);
        mVMChangeCompletedSuccesfully = false;
        mFwdChangesRequireRollback = false;
        mVMOrFwdSetError = 0;
        // If we are switching to a non default provider - save previous forwarding
        // settings
        boolean isKeySame;
        if (CallSettings.isMultipleSim()) {
            isKeySame = mPreviousVMProviderKey.equals(DEFAULT_VM_PROVIDER_KEY[mSimId]);
        } else {
            isKeySame = mPreviousVMProviderKey.equals(DEFAULT_VM_PROVIDER_KEY[0]);
        }

        if (!key.equals(mPreviousVMProviderKey) && (isKeySame)) {

            if (DBG) {
                log("Reading current forwarding settings");
            }
            mForwardingReadResults = new CallForwardInfo[FORWARDING_SETTINGS_REASONS.length];
            for (int i = 0; i < FORWARDING_SETTINGS_REASONS.length; i++) {
                mForwardingReadResults[i] = null;
                if (CallSettings.isMultipleSim()) {
                    ((GeminiPhone)mPhone).getCallForwardingOptionGemini(FORWARDING_SETTINGS_REASONS[i],
                            mGetOptionComplete.obtainMessage(EVENT_FORWARDING_GET_COMPLETED, i, 0), mSimId);
                } else {
                    mPhone.getCallForwardingOption(FORWARDING_SETTINGS_REASONS[i],
                            mGetOptionComplete.obtainMessage(EVENT_FORWARDING_GET_COMPLETED, i, 0));
                }
            }
            showDialogIfForeground(VOICEMAIL_FWD_READING_DIALOG);
        } else {
            saveVoiceMailAndForwardingNumberStage2();
        }
    }

    private final Handler mGetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult result = (AsyncResult) msg.obj;
            switch (msg.what) {
            case EVENT_FORWARDING_GET_COMPLETED:
                handleForwardingSettingsReadResult(result, msg.arg1);
                break;
            default:
                break;
            }
        }
    };

    void handleForwardingSettingsReadResult(AsyncResult ar, int idx) {
        if (DBG) {
            Xlog.d(LOG_TAG, "handleForwardingSettingsReadResult: " + idx);
        }
        Throwable error = null;
        if (ar.exception != null) {
            if (DBG) {
                Xlog.d(LOG_TAG, "FwdRead: ar.exception=" +
                    ar.exception.getMessage());
            }
            error = ar.exception;
        }
        if (ar.userObj instanceof Throwable) {
            if (DBG) {
                Xlog.d(LOG_TAG, "FwdRead: userObj=" +
                    ((Throwable)ar.userObj).getMessage());
            }
            error = (Throwable)ar.userObj;
        }

        // We may have already gotten an error and decided to ignore the other results.
        if (mForwardingReadResults == null) {
            if (DBG) {
                Xlog.d(LOG_TAG, "ignoring fwd reading result: " + idx);
            }
            return;
        }

        // In case of error ignore other results, show an error dialog
        if (error != null) {
            if (DBG) {
                Xlog.d(LOG_TAG, "Error discovered for fwd read : " + idx);
            }
            mForwardingReadResults = null;
            dismissDialogSafely(VOICEMAIL_FWD_READING_DIALOG);
            showVMDialog(MSG_FW_GET_EXCEPTION);
            return;
        }

        // Get the forwarding info
        final CallForwardInfo cfInfoArray[] = (CallForwardInfo[]) ar.result;
        CallForwardInfo fi = null;
        for (int i = 0 ; i < cfInfoArray.length; i++) {
            if ((cfInfoArray[i].serviceClass & CommandsInterface.SERVICE_CLASS_VOICE) != 0) {
                fi = cfInfoArray[i];
                break;
            }
        }
        if (fi == null) {

            // In case we go nothing it means we need this reason disabled
            // so create a CallForwardInfo for capturing this
            if (DBG) {
                Xlog.d(LOG_TAG, "Creating default info for " + idx);
            }
            fi = new CallForwardInfo();
            fi.status = 0;
            fi.reason = FORWARDING_SETTINGS_REASONS[idx];
            fi.serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
        } else {
            // if there is not a forwarding number, ensure the entry is set to "not active."
            if (fi.number == null || fi.number.length() == 0) {
                fi.status = 0;
            }

            if (DBG) {
                Xlog.d(LOG_TAG, "Got  " + fi.toString() + " for " + idx);
            }
        }
        mForwardingReadResults[idx] = fi;

        // Check if we got all the results already
        boolean done = true;
        for (int i = 0; i < mForwardingReadResults.length; i++) {
            if (mForwardingReadResults[i] == null) {
                done = false;
                break;
            }
        }
        if (done) {
            if (DBG) {
                Xlog.d(LOG_TAG, "Done receiving fwd info");
            }
            dismissDialogSafely(VOICEMAIL_FWD_READING_DIALOG);
            if (mReadingSettingsForDefaultProvider) {
                if (CallSettings.isMultipleSim()) {
                    maybeSaveSettingsForVoicemailProvider(DEFAULT_VM_PROVIDER_KEY[mSimId],
                                new VoiceMailProviderSettings(this.mOldVmNumber, mForwardingReadResults));
                } else {
                    maybeSaveSettingsForVoicemailProvider(DEFAULT_VM_PROVIDER_KEY[0],
                            new VoiceMailProviderSettings(this.mOldVmNumber, mForwardingReadResults));
                }
                mReadingSettingsForDefaultProvider = false;
            }
            saveVoiceMailAndForwardingNumberStage2();
        } else {
            if (DBG) {
                Xlog.d(LOG_TAG, "Not done receiving fwd info");
            }
        }
    }

    private CallForwardInfo infoForReason(CallForwardInfo[] infos, int reason) {
        CallForwardInfo result = null;
        if (null != infos) {
            for (CallForwardInfo info : infos) {
                if (info.reason == reason) {
                    result = info;
                    break;
                }
            }
        }
        return result;
    }

    private boolean isUpdateRequired(CallForwardInfo oldInfo,
            CallForwardInfo newInfo) {
        boolean result = true;
        if (0 == newInfo.status) {
            // If we're disabling a type of forwarding, and it's already
            // disabled for the account, don't make any change
            if (oldInfo != null && oldInfo.status == 0) {
                result = false;
            }
        }
        return result;
    }

    private void resetForwardingChangeState() {
        mForwardingChangeResults = new HashMap<Integer, AsyncResult>();
        mExpectedChangeResultReasons = new HashSet<Integer>();
    }

    // Called after we are done saving the previous forwarding settings if
    // we needed.
    private void saveVoiceMailAndForwardingNumberStage2() {
        mForwardingChangeResults = null;
        mVoicemailChangeResult = null;
        if (mNewFwdSettings != FWD_SETTINGS_DONT_TOUCH) {
            resetForwardingChangeState();
            for (int i = 0; i < mNewFwdSettings.length; i++) {
                CallForwardInfo fi = mNewFwdSettings[i];

                final boolean doUpdate = isUpdateRequired(infoForReason(
                            mForwardingReadResults, fi.reason), fi);

                if (doUpdate) {
                    if (DBG) {
                        log("Setting fwd #: " + i + ": " + fi.toString());
                    }
                    mExpectedChangeResultReasons.add(i);

                    if (CallSettings.isMultipleSim()) {
                        ((GeminiPhone)mPhone).setCallForwardingOptionGemini(
                                (fi.status == 1 ?
                                CommandsInterface.CF_ACTION_REGISTRATION :
                                CommandsInterface.CF_ACTION_DISABLE),
                                fi.reason,
                                fi.number,
                                fi.timeSeconds,
                                mSetOptionComplete.obtainMessage(EVENT_FORWARDING_CHANGED, i, 0), mSimId);
                
                    } else {
                        mPhone.setCallForwardingOption(
                                (fi.status == 1 ?
                                    CommandsInterface.CF_ACTION_REGISTRATION :
                                CommandsInterface.CF_ACTION_DISABLE),
                            fi.reason,
                            fi.number,
                            fi.timeSeconds,
                                mSetOptionComplete.obtainMessage(EVENT_FORWARDING_CHANGED, i, 0));
                    }
                }
                showDialogIfForeground(VOICEMAIL_FWD_SAVING_DIALOG);
            }
        } else {
            if (DBG) {
                log("Not touching fwd #");
            }
            setVMNumberWithCarrier();
        }
    }

    void setVMNumberWithCarrier() {
        if (DBG) {
            log("save voicemail #: " + mNewVMNumber);
        }

        if (CallSettings.isMultipleSim()) {
            ((GeminiPhone)mPhone).setVoiceMailNumberGemini(
                    ((GeminiPhone)mPhone).getVoiceMailAlphaTagGemini(mSimId).toString(),
                    mNewVMNumber,
                    Message.obtain(mSetOptionComplete, EVENT_VOICEMAIL_CHANGED), mSimId);        
        } else {
            mPhone.setVoiceMailNumber(
                    mPhone.getVoiceMailAlphaTag().toString(),
                    mNewVMNumber,
                    Message.obtain(mSetOptionComplete, EVENT_VOICEMAIL_CHANGED));
        }
    }

    /**
     * Callback to handle option update completions
     */
    private final Handler mSetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult result = (AsyncResult) msg.obj;
            boolean done = false;
            switch (msg.what) {
                case EVENT_VOICEMAIL_CHANGED:
                    mVoicemailChangeResult = result;
                    mVMChangeCompletedSuccesfully = checkVMChangeSuccess() == null;
                    if (DBG) {
                        log("VM change complete msg, VM change done = " +
                                String.valueOf(mVMChangeCompletedSuccesfully));
                    }
                    done = true;
                    break;
                case EVENT_FORWARDING_CHANGED:
                    mForwardingChangeResults.put(msg.arg1, result);
                    if (result.exception != null) {
                        if (DBG) {
                            log("Error in setting fwd# " + msg.arg1 + ": " +
                                    result.exception.getMessage());
                        }
                    } else {
                        if (DBG) {
                            log("Success in setting fwd# " + msg.arg1);
                        }
                    }
                    final boolean completed = checkForwardingCompleted();
                    if (completed) {
                        if (checkFwdChangeSuccess() == null) {
                            if (DBG) {
                                log("Overall fwd changes completed ok, starting vm change");
                            }
                            setVMNumberWithCarrier();
                        } else {
                            if (DBG) {
                                log("Overall fwd changes completed, failure");
                            }
                            mFwdChangesRequireRollback = false;
                            Iterator<Map.Entry<Integer,AsyncResult>> it =
                                mForwardingChangeResults.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry<Integer,AsyncResult> entry = it.next();
                                if (entry.getValue().exception == null) {
                                    // If at least one succeeded we have to revert
                                    if (DBG) {
                                        log("Rollback will be required");
                                    }
                                    mFwdChangesRequireRollback = true;
                                    break;
                                }
                            }
                            done = true;
                        }
                    }
                    break;
                default:
                    // TODO: should never reach this, may want to throw exception
            }
            if (done) {
                if (DBG) {
                    log("All VM provider related changes done");
                }
                if (mForwardingChangeResults != null) {
                    dismissDialogSafely(VOICEMAIL_FWD_SAVING_DIALOG);
                }
                handleSetVMOrFwdMessage();
            }
        }
    };

    /**
     * Callback to handle option revert completions
     */
    private final Handler mRevertOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult result = (AsyncResult) msg.obj;
            switch (msg.what) {
                case EVENT_VOICEMAIL_CHANGED:
                    mVoicemailChangeResult = result;
                    if (DBG) {
                        log("VM revert complete msg");
                    }
                    break;
                case EVENT_FORWARDING_CHANGED:
                    mForwardingChangeResults.put(msg.arg1, result);
                    if (result.exception != null) {
                        if (DBG) {
                            log("Error in reverting fwd# " + msg.arg1 + ": " +
                                result.exception.getMessage());
                        }
                    } else {
                        if (DBG) {
                            log("Success in reverting fwd# " + msg.arg1);
                        }
                    }
                    if (DBG) {
                        log("FWD revert complete msg ");
                    }
                    break;
                default:
                    break;
            }
            final boolean done =
                (!mVMChangeCompletedSuccesfully || mVoicemailChangeResult != null) &&
                (!mFwdChangesRequireRollback || checkForwardingCompleted());
            if (done) {
                if (DBG) {
                    log("All VM reverts done");
                }
                dismissDialogSafely(VOICEMAIL_REVERTING_DIALOG);
                onRevertDone();
            }
        }
    };

    /**
     * @return true if forwarding change has completed
     */
    private boolean checkForwardingCompleted() {
        boolean result;
        if (mForwardingChangeResults == null) {
            result = true;
        } else {
            // return true iff there is a change result for every reason for
            // which we expected a result
            result = true;
            for (Integer reason : mExpectedChangeResultReasons) {
                if (mForwardingChangeResults.get(reason) == null) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    /**
     * @return error string or null if successful
     */
    private String checkFwdChangeSuccess() {
        String result = null;
        Iterator<Map.Entry<Integer,AsyncResult>> it =
            mForwardingChangeResults.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer,AsyncResult> entry = it.next();
            Throwable exception = entry.getValue().exception;
            if (exception != null) {
                result = exception.getMessage();
                if (result == null) {
                    result = "";
                }
                break;
            }
        }
        return result;
    }

    /**
     * @return error string or null if successful
     */
    private String checkVMChangeSuccess() {
        if (mVoicemailChangeResult.exception != null) {
            final String msg = mVoicemailChangeResult.exception.getMessage();
            if (msg == null) {
                return "";
            }
            return msg;
        }
        return null;
    }

    private void handleSetVMOrFwdMessage() {
        if (DBG) {
            log("handleSetVMMessage: set VM request complete");
        }
        boolean success = true;
        boolean fwdFailure = false;
        String exceptionMessage = "";
        if (mForwardingChangeResults != null) {
            exceptionMessage = checkFwdChangeSuccess();
            if (exceptionMessage != null) {
                success = false;
                fwdFailure = true;
            }
        }
        if (success) {
            exceptionMessage = checkVMChangeSuccess();
            if (exceptionMessage != null) {
                success = false;
            }
        }
        if (success) {
            if (DBG) {
                log("change VM success!");
            }
            handleVMAndFwdSetSuccess(MSG_VM_OK);
            updateVoiceNumberField();
        } else {
            if (fwdFailure) {
                log("change FW failed: " + exceptionMessage);
                handleVMOrFwdSetError(MSG_FW_SET_EXCEPTION);
            } else {
                log("change VM failed: " + exceptionMessage);
                handleVMOrFwdSetError(MSG_VM_EXCEPTION);
            }
        }
    }

    private void handleVMOrFwdSetError(int msgId) {
        if (mChangingVMorFwdDueToProviderChange) {
            mVMOrFwdSetError = msgId;
            mChangingVMorFwdDueToProviderChange = false;
            switchToPreviousVoicemailProvider();
            return;
        }
        mChangingVMorFwdDueToProviderChange = false;
        showVMDialog(msgId);
        updateVoiceNumberField();
    }

    private void handleVMAndFwdSetSuccess(int msgId) {
        mChangingVMorFwdDueToProviderChange = false;
        showVMDialog(msgId);
    }

    /*
     * Methods used to sync UI state with that of the network
     */
    // update the voicemail number from what we've recorded on the sim.
    private void updateVoiceNumberField() {
        if (mSubMenuVoicemailSettings == null) {
            return;
        }

        Xlog.d(LOG_TAG, "updateVoiceNumberField, simid:" + mSimId);
        if (CallSettings.isMultipleSim()) {
            mOldVmNumber = ((GeminiPhone)mPhone).getVoiceMailNumberGemini(mSimId);        
        } else {
            mOldVmNumber = mPhone.getVoiceMailNumber();
        }

        Xlog.d(LOG_TAG, "updateVoiceNumberField, mOldVmNumber:" + mOldVmNumber);

        if (mOldVmNumber == null) {
            mOldVmNumber = "";
        }
        mSubMenuVoicemailSettings.setPhoneNumber(mOldVmNumber);
        final String summary = (mOldVmNumber.length() > 0) ? mOldVmNumber :
            getString(R.string.voicemail_number_not_set);
        mSubMenuVoicemailSettings.setSummary(summary);
    }

    /*
     * Helper Methods for Activity class.
     * The initial query commands are split into two pieces now
     * for individual expansion.  This combined with the ability
     * to cancel queries allows for a much better user experience,
     * and also ensures that the user only waits to update the
     * data that is relevant.
     */

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        mCurrentDialogId = id;
    }

    // dialog creation method, called by showDialog()
    @Override
    protected Dialog onCreateDialog(int id) {
        if ((id == VM_RESPONSE_ERROR) || (id == VM_NOCHANGE_ERROR) ||
            (id == FW_SET_RESPONSE_ERROR) || (id == FW_GET_RESPONSE_ERROR) ||
                (id == VOICEMAIL_DIALOG_CONFIRM)) {

            AlertDialog.Builder b = new AlertDialog.Builder(this);

            int msgId;
            int titleId = R.string.error_updating_title;
            switch (id) {
                case VOICEMAIL_DIALOG_CONFIRM:
                    msgId = R.string.vm_changed;
                    titleId = R.string.voicemail;
                    // Set Button 2
                    b.setNegativeButton(R.string.close_dialog, this);
                    break;
                case VM_NOCHANGE_ERROR:
                    // even though this is technically an error,
                    // keep the title friendly.
                    msgId = R.string.no_change;
                    titleId = R.string.voicemail;
                    // Set Button 2
                    b.setNegativeButton(R.string.close_dialog, this);
                    break;
                case VM_RESPONSE_ERROR:
                    msgId = R.string.vm_change_failed;
                    // Set Button 1
                    b.setPositiveButton(R.string.close_dialog, this);
                    break;
                case FW_SET_RESPONSE_ERROR:
                    msgId = R.string.fw_change_failed;
                    // Set Button 1
                    b.setPositiveButton(R.string.close_dialog, this);
                    break;
                case FW_GET_RESPONSE_ERROR:
                    msgId = R.string.fw_get_in_vm_failed;
                    b.setPositiveButton(R.string.alert_dialog_yes, this);
                    b.setNegativeButton(R.string.alert_dialog_no, this);
                    break;
                default:
                    msgId = R.string.exception_error;
                    // Set Button 3, tells the activity that the error is
                    // not recoverable on dialog exit.
                    b.setNeutralButton(R.string.close_dialog, this);
                    break;
            }

            b.setTitle(getText(titleId));
            String message = getText(msgId).toString();
            b.setMessage(message);
            b.setCancelable(false);
            AlertDialog dialog = b.create();

            // make the dialog more obvious by bluring the background.
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

            return dialog;
        } else if (id == VOICEMAIL_FWD_SAVING_DIALOG || id == VOICEMAIL_FWD_READING_DIALOG ||
                id == VOICEMAIL_REVERTING_DIALOG) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle(getText(R.string.updating_title));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage(getText(
                    id == VOICEMAIL_FWD_SAVING_DIALOG ? R.string.updating_settings :
                    (id == VOICEMAIL_REVERTING_DIALOG ? R.string.reverting_settings :
                    R.string.reading_settings)));
            return dialog;
        }


         return super.onCreateDialog(id);
    }

    // This is a method implemented for DialogInterface.OnClickListener.
    // Used with the error dialog to close the app, voicemail dialog to just dismiss.
    // Close button is mapped to BUTTON_POSITIVE for the errors that close the activity,
    // while those that are mapped to BUTTON_NEUTRAL only move the preference focus.
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL:
                if (DBG) {
                    log("Neutral button");
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                if (DBG) {
                    log("Negative button");
                }
                if (mCurrentDialogId == FW_GET_RESPONSE_ERROR) {
                    // We failed to get current forwarding settings and the user
                    // does not wish to continue.
                    switchToPreviousVoicemailProvider();
                }
                break;
            case DialogInterface.BUTTON_POSITIVE:
                if (DBG) {
                    log("Positive button");
                }
                if (mCurrentDialogId == FW_GET_RESPONSE_ERROR) {
                    // We failed to get current forwarding settings but the user
                    // wishes to continue changing settings to the new vm provider
                    saveVoiceMailAndForwardingNumberStage2();
                } else {
                    finish();
                }
                return;
            default:
                break;
                // just let the dialog close and go back to the input
        }
        // In all dialogs, all buttons except BUTTON_POSITIVE lead to the end of user interaction
        // with settings UI. If we were called to explicitly configure voice mail then
        // we finish the settings activity here to come back to whatever the user was doing.
        if (ACTION_ADD_VOICEMAIL.equals(getIntent().getAction())) {
            finish();
        }
    }

    // set the app state with optional status.
    private void showVMDialog(int msgStatus) {
        switch (msgStatus) {
            // It's a bit worrisome to punt in the error cases here when we're
            // not in the foreground; maybe toast instead?
            case MSG_VM_EXCEPTION:
                showDialogIfForeground(VM_RESPONSE_ERROR);
                break;
            case MSG_FW_SET_EXCEPTION:
                showDialogIfForeground(FW_SET_RESPONSE_ERROR);
                break;
            case MSG_FW_GET_EXCEPTION:
                showDialogIfForeground(FW_GET_RESPONSE_ERROR);
                break;
            case MSG_VM_NOCHANGE:
                showDialogIfForeground(VM_NOCHANGE_ERROR);
                break;
            case MSG_VM_OK:
                showDialogIfForeground(VOICEMAIL_DIALOG_CONFIRM);
                break;
            default:
                break;
        }
    }

    /*
     * Activity class methods
     */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG) {
            log("Creating activity");
        }
        mPhone = PhoneGlobals.getPhone();
        mSimId = getIntent().getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);

        if (mSimId != -1) {
             mInitTitle = getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME);
             if (mInitTitle == null) {
                 mInitTitle = SIMInfo.getSIMInfoBySlot(this, mSimId).mDisplayName;
             } 
        }

        addPreferencesFromResource(R.xml.voice_mail);

        if (null != mInitTitle) {
            setTitle(mInitTitle);   
        }
        
        mSubMenuVoicemailSettings = (EditPhoneNumberPreference)findPreference(BUTTON_VOICEMAIL_KEY);
        if (mSubMenuVoicemailSettings != null) {
            mSubMenuVoicemailSettings.setParentActivity(this, VOICEMAIL_PREF_ID, this);
            mSubMenuVoicemailSettings.setDialogOnClosedListener(this);
            mSubMenuVoicemailSettings.setDialogTitle(R.string.voicemail_settings_number_label);
        }
        mVoicemailProviders = (ListPreference) findPreference(BUTTON_VOICEMAIL_PROVIDER_KEY);
        if (mVoicemailProviders != null) {
            mVoicemailProviders.setOnPreferenceChangeListener(this);
            mVoicemailSettings = (PreferenceScreen)findPreference(BUTTON_VOICEMAIL_SETTING_KEY);
            if (CallSettings.isMultipleSim()) {
                mVoicemailProviders.setKey(BUTTON_VOICEMAIL_PROVIDER_KEY + mSimId);
            }
            initVoiceMailProviders();
        }


        // check the intent that started this activity and pop up the voicemail
        // dialog if we've been asked to.
        // If we have at least one non default VM provider registered then bring up
        // the selection for the VM provider, otherwise bring up a VM number dialog.
        // We only bring up the dialog the first time we are called (not after orientation change)
        if (icicle == null) {
            if (ACTION_ADD_VOICEMAIL.equals(getIntent().getAction()) &&
                    mVoicemailProviders != null) {
                if (mVMProvidersData.size() > 1) {
                    simulatePreferenceClick(mVoicemailProviders);
                } else {
                    if (CallSettings.isMultipleSim()) {
                        onPreferenceChange(mVoicemailProviders, DEFAULT_VM_PROVIDER_KEY[mSimId]);
                        mVoicemailProviders.setValue(DEFAULT_VM_PROVIDER_KEY[mSimId]);
                    } else {
                        onPreferenceChange(mVoicemailProviders, DEFAULT_VM_PROVIDER_KEY[0]);
                        mVoicemailProviders.setValue(DEFAULT_VM_PROVIDER_KEY[0]);
                    }

                }
            }
        }
        mVMProviderSettingsForced = false;
  
        IntentFilter intentFilter =
            new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            intentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        ///M: add for hot swap {
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        ///@}

        registerReceiver(mReceiver, intentFilter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    
    @Override
    protected void onNewIntent(Intent intent) {
        /// M: alsp00569552 @{
        // when new SIM Card is not the last one. we finished this activity and reconstruct it.
        // because,this activity launch mode is singleTask.
        //
        // MTK add
        final int newSlotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
        log("onNewIntent newSlotId, oldSlotID " + newSlotId + "," + mSimId);
        if (newSlotId != mSimId){
            startActivity(intent);
            this.finish();
            return;
        }
        /// @}
        this.setIntent(intent);
        mSimId = newSlotId;
        if (mSimId != -1) {
             mInitTitle = intent.getStringExtra(MultipleSimActivity.SUB_TITLE_NAME);
             if (mInitTitle == null) {
                 mInitTitle = SIMInfo.getSIMInfoBySlot(this, mSimId).mDisplayName;
             } 
        }

        if (null != mInitTitle) {
            setTitle(mInitTitle);   
        }

        if (mSubMenuVoicemailSettings != null) {
            mSubMenuVoicemailSettings.setParentActivity(this, VOICEMAIL_PREF_ID, this);
            mSubMenuVoicemailSettings.setDialogOnClosedListener(this);
            mSubMenuVoicemailSettings.setDialogTitle(R.string.voicemail_settings_number_label);
        }
        if (mVoicemailProviders != null) {
            mVoicemailProviders.setOnPreferenceChangeListener(this);
            mVoicemailSettings = (PreferenceScreen)findPreference(BUTTON_VOICEMAIL_SETTING_KEY);
            if (CallSettings.isMultipleSim()) {
                mVoicemailProviders.setKey(BUTTON_VOICEMAIL_PROVIDER_KEY + mSimId);
            }
            initVoiceMailProviders();
        }
        updateVoiceNumberField();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        //update the voice mail number when user enter here by long press home key(ALPS00069703)
        updateVoiceNumberField();
        mForeground = true;
    }

    private boolean isAirplaneModeOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }
    
    private static void log(String msg) {
        Xlog.d(LOG_TAG, msg);
    }

    /**
     * Updates the look of the VM preference widgets based on current VM provider settings.
     * Note that the provider name is loaded form the found activity via loadLabel in
     * initVoiceMailProviders in order for it to be localizable.
     */
    private void updateVMPreferenceWidgets(String currentProviderSetting) {
        final String key = currentProviderSetting;
        final VoiceMailProvider provider = mVMProvidersData.get(key);

        /* This is the case when we are coming up on a freshly wiped phone and there is no
         persisted value for the list preference mVoicemailProviders.
         In this case we want to show the UI asking the user to select a voicemail provider as
         opposed to silently falling back to default one. */
        if (provider == null) {
            mVoicemailProviders.setSummary(getString(R.string.sum_voicemail_choose_provider));
            mVoicemailSettings.setSummary("");
            mVoicemailSettings.setEnabled(false);
            mVoicemailSettings.setIntent(null);
        } else {
            final String providerName = provider.mName;
            mVoicemailProviders.setSummary(providerName);
            mVoicemailSettings.setSummary(getApplicationContext().getString(
                    R.string.voicemail_settings_for, providerName));
            mVoicemailSettings.setEnabled(true);
            mVoicemailSettings.setIntent(provider.mIntent);
        }
    }

    /**
     * Enumerates existing VM providers and puts their data into the list and populates
     * the preference list objects with their names.
     * In case we are called with ACTION_ADD_VOICEMAIL intent the intent may have
     * an extra string called IGNORE_PROVIDER_EXTRA with "package.activityName" of the provider
     * which should be hidden when we bring up the list of possible VM providers to choose.
     * This allows a provider which is being disabled (e.g. GV user logging out) to force the user
     * to pick some other provider.
     */
    private void initVoiceMailProviders() {

        log("callFeaturesSettings, initVoiceMailProviders: simId = " + mSimId);
        if (CallSettings.isMultipleSim()) {
            mPerProviderSavedVMNumbers =
                this.getApplicationContext().getSharedPreferences(
                VM_NUMBERS_SHARED_PREFERENCES_NAME[mSimId], MODE_PRIVATE);
        } else {
            mPerProviderSavedVMNumbers =
            this.getApplicationContext().getSharedPreferences(
                VM_NUMBERS_SHARED_PREFERENCES_NAME[0], MODE_PRIVATE);
        }

        String providerToIgnore = null;
        if (ACTION_ADD_VOICEMAIL.equals(getIntent().getAction())) {
            if (DBG) {
                log("ACTION_ADD_VOICEMAIL");
            }
            if (getIntent().hasExtra(IGNORE_PROVIDER_EXTRA)) {
                providerToIgnore = getIntent().getStringExtra(IGNORE_PROVIDER_EXTRA);
            }
            if (DBG) {
                log("providerToIgnore=" + providerToIgnore);
            }
            if (providerToIgnore != null) {
                deleteSettingsForVoicemailProvider(providerToIgnore);
            }
        }

        mVMProvidersData.clear();

        // Stick the default element which is always there
        final String myCarrier = getString(R.string.voicemail_default);
        if (CallSettings.isMultipleSim()) {
            mVMProvidersData.put(DEFAULT_VM_PROVIDER_KEY[mSimId], new VoiceMailProvider(myCarrier, null));
        } else {
            mVMProvidersData.put(DEFAULT_VM_PROVIDER_KEY[0], new VoiceMailProvider(myCarrier, null));
        }

        // Enumerate providers
        PackageManager pm = getPackageManager();
        Intent intent = new Intent();
        intent.setAction(ACTION_CONFIGURE_VOICEMAIL);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        int len = resolveInfos.size() + 1; // +1 for the default choice we will insert.

        // Go through the list of discovered providers populating the data map
        // skip the provider we were instructed to ignore if there was one
        for (int i = 0; i < resolveInfos.size(); i++) {
            final ResolveInfo ri = resolveInfos.get(i);
            final ActivityInfo currentActivityInfo = ri.activityInfo;
            final String key = makeKeyForActivity(currentActivityInfo);
            if (DBG) {
                log("Loading " + key);
            }
            if (key.equals(providerToIgnore)) {
                if (DBG) {
                    log("Ignoring " + key);
                }
                len--;
                continue;
            }
            final String nameForDisplay = ri.loadLabel(pm).toString();
            Intent providerIntent = new Intent();
            providerIntent.setAction(ACTION_CONFIGURE_VOICEMAIL);
            providerIntent.setClassName(currentActivityInfo.packageName,
                    currentActivityInfo.name);
            mVMProvidersData.put(
                    key,
                    new VoiceMailProvider(nameForDisplay, providerIntent));

        }

        // Now we know which providers to display - create entries and values array for
        // the list preference
        String [] entries = new String [len];
        String [] values = new String [len];
        entries[0] = myCarrier;
        if (CallSettings.isMultipleSim()) {
            values[0] = DEFAULT_VM_PROVIDER_KEY[mSimId];
        } else {
            values[0] = DEFAULT_VM_PROVIDER_KEY[0];
        }

        int entryIdx = 1;
        for (int i = 0; i < resolveInfos.size(); i++) {
            final String key = makeKeyForActivity(resolveInfos.get(i).activityInfo);
            if (!mVMProvidersData.containsKey(key)) {
                continue;
            }
            entries[entryIdx] = mVMProvidersData.get(key).mName;
            values[entryIdx] = key;
            entryIdx++;
        }

        mVoicemailProviders.setEntries(entries);
        mVoicemailProviders.setEntryValues(values);

        mPreviousVMProviderKey = getCurrentVoicemailProviderKey();
        if (CallSettings.isMultipleSim()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            mVoicemailProviders.setValue(sp.getString(BUTTON_VOICEMAIL_PROVIDER_KEY + mSimId, null));
        }
                
        updateVMPreferenceWidgets(mPreviousVMProviderKey);
    }

    private String makeKeyForActivity(ActivityInfo ai) {
        return ai.name;
    }

    /**
     * Simulates user clicking on a passed preference.
     * Usually needed when the preference is a dialog preference and we want to invoke
     * a dialog for this preference programmatically.
     * TODO(iliat): figure out if there is a cleaner way to cause preference dlg to come up
     */
    private void simulatePreferenceClick(Preference preference) {
        // Go through settings until we find our setting
        // and then simulate a click on it to bring up the dialog
        final ListAdapter adapter = getPreferenceScreen().getRootAdapter();
        for (int idx = 0; idx < adapter.getCount(); idx++) {
            if (adapter.getItem(idx) == preference) {
                getPreferenceScreen().onItemClick(this.getListView(),
                        null, idx, adapter.getItemId(idx));
                break;
            }
        }
    }

    /**
     * Saves new VM provider settings associating them with the currently selected
     * provider if settings are different than the ones already stored for this
     * provider.
     * Later on these will be used when the user switches a provider.
     */
    private void maybeSaveSettingsForVoicemailProvider(String key,
            VoiceMailProviderSettings newSettings) {
        if (mVoicemailProviders == null) {
            return;
        }
        final VoiceMailProviderSettings curSettings = loadSettingsForVoiceMailProvider(key);
        if (newSettings.equals(curSettings)) {
            if (DBG) {
                log("Not saving setting for " + key + " since they have not changed");
            }
            return;
        }
        if (DBG) {
            log("Saving settings for " + key + ": " + newSettings.toString());
        }
        Editor editor = mPerProviderSavedVMNumbers.edit();
        editor.putString(key + VM_NUMBER_TAG,newSettings.mVoicemailNumber);
        String fwdKey = key + FWD_SETTINGS_TAG;
        CallForwardInfo[] s = newSettings.mForwardingSettings;
        if (s != FWD_SETTINGS_DONT_TOUCH) {
            editor.putInt(fwdKey + FWD_SETTINGS_LENGTH_TAG, s.length);
            for (int i = 0; i < s.length; i++) {
                final String settingKey = fwdKey + FWD_SETTING_TAG + String.valueOf(i);
                final CallForwardInfo fi = s[i];
                editor.putInt(settingKey + FWD_SETTING_STATUS, fi.status);
                editor.putInt(settingKey + FWD_SETTING_REASON, fi.reason);
                editor.putString(settingKey + FWD_SETTING_NUMBER, fi.number);
                editor.putInt(settingKey + FWD_SETTING_TIME, fi.timeSeconds);
            }
        } else {
            editor.putInt(fwdKey + FWD_SETTINGS_LENGTH_TAG, 0);
        }
        editor.apply();
    }

    /**
     * Returns settings previously stored for the currently selected
     * voice mail provider. If none is stored returns null.
     * If the user switches to a voice mail provider and we have settings
     * stored for it we will automatically change the phone's voice mail number
     * and forwarding number to the stored one. Otherwise we will bring up provider's configuration
     * UI.
     */
    private VoiceMailProviderSettings loadSettingsForVoiceMailProvider(String key) {
        final String vmNumberSetting = mPerProviderSavedVMNumbers.getString(key + VM_NUMBER_TAG,
                null);
        if (vmNumberSetting == null) {
            if (DBG) {
                log("Settings for " + key + " not found");
            }
            return null;
        }

        CallForwardInfo[] cfi = FWD_SETTINGS_DONT_TOUCH;
        String fwdKey = key + FWD_SETTINGS_TAG;
        final int fwdLen = mPerProviderSavedVMNumbers.getInt(fwdKey + FWD_SETTINGS_LENGTH_TAG, 0);
        if (fwdLen > 0) {
            cfi = new CallForwardInfo[fwdLen];
            for (int i = 0; i < cfi.length; i++) {
                final String settingKey = fwdKey + FWD_SETTING_TAG + String.valueOf(i);
                cfi[i] = new CallForwardInfo();
                cfi[i].status = mPerProviderSavedVMNumbers.getInt(
                        settingKey + FWD_SETTING_STATUS, 0);
                cfi[i].reason = mPerProviderSavedVMNumbers.getInt(
                        settingKey + FWD_SETTING_REASON,
                        CommandsInterface.CF_REASON_ALL_CONDITIONAL);
                cfi[i].serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
                cfi[i].toa = PhoneNumberUtils.TOA_International;
                cfi[i].number = mPerProviderSavedVMNumbers.getString(
                        settingKey + FWD_SETTING_NUMBER, "");
                cfi[i].timeSeconds = mPerProviderSavedVMNumbers.getInt(
                        settingKey + FWD_SETTING_TIME, 20);
            }
        }

        VoiceMailProviderSettings settings =  new VoiceMailProviderSettings(vmNumberSetting, cfi);
        if (DBG) {
            log("Loaded settings for " + key + ": " + settings.toString());
        }
        return settings;
    }

    /**
     * Deletes settings for the specified provider.
     */
    private void deleteSettingsForVoicemailProvider(String key) {
        if (DBG) {
            log("Deleting settings for" + key);
        }
        if (mVoicemailProviders == null) {
            return;
        }
        mPerProviderSavedVMNumbers.edit()
            .putString(key + VM_NUMBER_TAG, null)
            .putInt(key + FWD_SETTINGS_TAG + FWD_SETTINGS_LENGTH_TAG, 0)
            .commit();
    }

    private String getCurrentVoicemailProviderKey() {
        String key = mVoicemailProviders.getValue();
        
        if (CallSettings.isMultipleSim()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            key = sp.getString(BUTTON_VOICEMAIL_PROVIDER_KEY + mSimId, "");
        }
        
        if (CallSettings.isMultipleSim()) {
            return (key != null) ? key : DEFAULT_VM_PROVIDER_KEY[mSimId];
        } else {
            return (key != null) ? key : DEFAULT_VM_PROVIDER_KEY[0];
        }
    }
   
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    
    private class CallFeaturesSettingBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ((action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                        && intent.getBooleanExtra("state", false))
                    || (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)
                        && (intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1) == 0))) {
                finish();
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for hot swap {
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(context);
                if (temp.size() == 0) {
                    Xlog.d(LOG_TAG, "Activity finished");
                    CallSettings.goUpToTopLevelSetting(VoiceMailSetting.this);
                } else if (CallSettings.isMultipleSim() && temp.size() == 1) {
                    if (temp.get(0).mSlot != mSimId) {
                        Xlog.d(LOG_TAG, "temp.size()=" + temp.size() + "Activity finished");
                        finish();
                    }
                }
                ///@}
            }
        }
    }
}

