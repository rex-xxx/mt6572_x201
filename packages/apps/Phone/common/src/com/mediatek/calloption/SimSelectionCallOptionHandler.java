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

package com.mediatek.calloption;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.util.Log;
import android.widget.ListAdapter;

import com.android.phone.Constants;
import com.mediatek.phone.SIMInfoWrapper;

import java.util.ArrayList;

public abstract class SimSelectionCallOptionHandler extends CallOptionBaseHandler {

    private static final String TAG = "SimSelectionCallOptionHandler";

    private static final int MAKE_CALL_REASON_OK = 0;
    private static final int MAKE_CALL_REASON_ASK = 5;
    private static final int MAKE_CALL_REASON_ASSOCIATE_MISSING = 6;

    // used for runnable
    private Request mRequest;
    private AssociateSimMissingArgs mAssociateSimMissingArgs;

    public class AssociateSimMissingArgs {
        public static final int ASSOCIATE_SIM_MISSING_YES_NO = 0;
        public static final int ASSOCIATE_SIM_MISSING_YES_OTHER = 1;

        public SIMInfo viaSimInfo;
        public long suggested;

        // ASSOCIATE_SIM_MISSING_YES_NO : only one sim insert, show dialog with 'Yes' or 'No'
        // ASSOCIATE_SIM_MISSING_YES_OTHER : more than one sim inserted, show dialog with 'Yes or other'
        public int type;

        /**
         * The constructor
         */
        public AssociateSimMissingArgs() {
            //
        }
    }

    private class CallbackArgs {

        public int reason;
        //public int type;
        public String number;
        public Object args;
        public long id;

        /**
         * The default constructor
         */
        public CallbackArgs() {
            //
        }

        /**
         * The constructor for callback arg for making the call
         * @param _reason the reason that why go here to make a call
         * @param _type the call type, voice/video/sip call
         * @param _id the sim Id for setuping the call
         * @param _number the number to dial
         * @param _args the extra param
         */
        public CallbackArgs(int argReason, String argNumber, long argId, Object argArgs) {
            reason = argReason;
            //type = argType;
            number = argNumber;
            id = argId;
            args = argArgs;
        }
    }

    @Override
    public void handleRequest(final Request request) {
        log("handleRequest()");
        boolean isVoiceMail = false;
        if (Constants.VOICEMAIL_URI.equals(request.getIntent().getData().toString())) {
            isVoiceMail = true;
        }
        
        if (-1 != request.getIntent().getIntExtra(Constants.EXTRA_SLOT_ID, -1)) {
            //request.getResultHandler().onContinueCallProcess(request.getIntent());
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
            return;
        }
        mRequest = request;

        String number = CallOptionUtils.getInitialNumber(request.getApplicationContext(),
                                                         request.getIntent());

        CallbackArgs callbackArgs = new CallbackArgs(MAKE_CALL_REASON_OK, number, 0, null);

        final long originalSim =
            request.getIntent().getLongExtra(Constants.EXTRA_ORIGINAL_SIM_ID,
                                             Settings.System.DEFAULT_SIM_NOT_SET);
        long suggestedSim = Settings.System.DEFAULT_SIM_NOT_SET;
        long associateSim = Settings.System.DEFAULT_SIM_NOT_SET;
        int associateSimInserts = 0;
        boolean originalSimInsert = false;

        final SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getDefault();

        final long defaultSim = Settings.System.getLong(request.getApplicationContext().getContentResolver(),
                Settings.System.VOICE_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);

        SIMInfo simInfo;
        final ArrayList associateSims = SimAssociateHandler.getInstance(request.getApplicationContext()).query(number);
        final boolean hasAssociateSims = associateSims != null && associateSims.size() > 0;
        if (hasAssociateSims) {
            for (Object item : associateSims) {
                final int temp = ((Integer)item).intValue();
                final int slot = simInfoWrapper.getSimSlotById(temp);
                if (slot >= 0 && CallOptionUtils.isSimInsert(request, slot)) {
                    associateSimInserts++;
                    associateSim = temp;
                }
            }
        }

        int internetEnableSetting = Settings.System.getInt(request.getApplicationContext().getContentResolver(), 
                                                           Settings.System.ENABLE_INTERNET_CALL, 0);

        // for cta case, when there are no sim cards inserted it should can make a call...
        if (simInfoWrapper.getInsertedSimCount() == 0
                && defaultSim != Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
            //final boolean internetCallOn = internetEnableSetting == 1;

            // if internet call option is off then dial out directly
            if (1 != internetEnableSetting) {
                //callbackArgs.type = DIAL_TYPE_VOICE;
                callbackArgs.id = 0;
                callbackArgs.reason = MAKE_CALL_REASON_OK;
            } else {
                // internet call option is on, ask user whether to use sip ?
                callbackArgs.reason = MAKE_CALL_REASON_ASK;
                callbackArgs.args = Long.valueOf(Settings.System.VOICE_CALL_SIM_SETTING_INTERNET);
            }
            onMakeCall(callbackArgs);
            return;
        }

        if (defaultSim == Settings.System.DEFAULT_SIM_NOT_SET) {
            onMakeCall(callbackArgs);
            return;
        }
        if (originalSim != Settings.System.DEFAULT_SIM_NOT_SET) {
            final int slot = simInfoWrapper.getSimSlotById((int)originalSim);
            originalSimInsert = slot >= 0 && CallOptionUtils.isSimInsert(request, slot);
            if (originalSim == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                originalSimInsert = (internetEnableSetting == 1);
            }
        }

        log("makeVoiceCall, number = " + number + ", originalSim = " + originalSim
                + ", defaultSim = " + defaultSim + ", associateSims = " + associateSims);

        // default sim is always ask
        if (defaultSim == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
        //if (true) {
            // In some case there is no sim inserted and no internet account,
            // but setting tell us that always ask, it's strange!!
            // ALPS00235140
            //final int enabled = Settings.System.getInt(request.getApplicationContext().getContentResolver(),
            //                                           Settings.System.ENABLE_INTERNET_CALL, 0);
            //final int count = simInfoWrapper.getInsertedSimCount();
            
            if (1 != internetEnableSetting && 0 == simInfoWrapper.getInsertedSimCount()) {
                callbackArgs.reason = MAKE_CALL_REASON_OK;
                onMakeCall(callbackArgs);
                return;
            }
            // default sim is always ask, show sim selection dialog
            // but we must found if there is any sim to be suggested
            log("always, associateSimInserts = " + associateSimInserts + " originalSim = " + originalSim);
            if (associateSimInserts > 1) {
                suggestedSim = Settings.System.DEFAULT_SIM_NOT_SET;
            } else if (associateSimInserts == 1) {
                suggestedSim = associateSim;
            } else if (originalSimInsert) {
                suggestedSim = originalSim;
            }
            //Always ask, we don't know which number maybe used, so the 
            //suggest is invalid, so change it as not set.
            if (isVoiceMail) {
                suggestedSim = Settings.System.DEFAULT_SIM_NOT_SET;
            }
            
            callbackArgs.args = suggestedSim;
            callbackArgs.reason = MAKE_CALL_REASON_ASK;
            onMakeCall(callbackArgs);
            return;
        }

        //ALPS00230449, when set default sim as internet and dialout a call from call with original sim
        if (!hasAssociateSims && originalSim !=  Settings.System.DEFAULT_SIM_NOT_SET 
                && defaultSim == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET 
                && originalSim != defaultSim) {
            if (originalSimInsert) {
                callbackArgs.args = originalSim;
            } else {
                callbackArgs.args = Settings.System.DEFAULT_SIM_NOT_SET;
            }
            callbackArgs.reason = MAKE_CALL_REASON_ASK;
            onMakeCall(callbackArgs);
            return;
        }
        
        //ALPS00234703, when set default sim as internet and dialout from a call with associate
        if (defaultSim == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
            if (hasAssociateSims 
                    || (originalSim != Settings.System.DEFAULT_SIM_NOT_SET 
                            && originalSim != Settings.System.VOICE_CALL_SIM_SETTING_INTERNET)) {
                if (associateSimInserts > 1) {
                    suggestedSim = Settings.System.DEFAULT_SIM_NOT_SET;
                } else if (associateSimInserts == 1) {
                    suggestedSim = associateSim;
                } else if (originalSimInsert) {
                    suggestedSim = originalSim;
                }
                
                callbackArgs.args = suggestedSim;
                callbackArgs.reason = MAKE_CALL_REASON_ASK;
                onMakeCall(callbackArgs);
                return;
            }
            doSipCallOptionHandle();
            return;
        }

        // no associate sim nor originalSim
        if (originalSim == Settings.System.DEFAULT_SIM_NOT_SET && !hasAssociateSims) {
            log("deaultSim = " + defaultSim);
            callbackArgs.reason = MAKE_CALL_REASON_OK;
            callbackArgs.id = defaultSim;
            //Profiler.trace(Profiler.CallOptionHelperLeaveMakeVoiceCall);
            onMakeCall(callbackArgs);
            return;
        }

        // only has original sim
        if (originalSim != Settings.System.DEFAULT_SIM_NOT_SET && !hasAssociateSims) {
            // only has original sim
            if (defaultSim == originalSim || !originalSimInsert) {
                // ok, dial out
                callbackArgs.reason = MAKE_CALL_REASON_OK;
                callbackArgs.id = defaultSim;
                onMakeCall(callbackArgs);
                return;
            }

            // originalSim is not insert, show sim selection dialog
            suggestedSim = Settings.System.DEFAULT_SIM_NOT_SET;
            if (originalSimInsert) {
                suggestedSim = originalSim;
            }
            callbackArgs.reason = MAKE_CALL_REASON_ASK;
            callbackArgs.args = suggestedSim;
            onMakeCall(callbackArgs);
            return;
        }

        // only has associate sim
        if (originalSim == Settings.System.DEFAULT_SIM_NOT_SET && hasAssociateSims) {
            // more than 2 associate sims !!!
            if (associateSimInserts >= 2) {
                callbackArgs.reason = MAKE_CALL_REASON_ASK;
                callbackArgs.args = Settings.System.DEFAULT_SIM_NOT_SET;
                onMakeCall(callbackArgs);
                return;
            }

            if (associateSims.size() == 1) {
                associateSim = ((Integer)associateSims.get(0)).intValue();
            } else if (associateSims.size() >= 2) {
                //get the inserted sim
                for (Object item : associateSims) {
                    final int temp = ((Integer)item).intValue();
                    final int slot = simInfoWrapper.getSimSlotById(temp);
                    if (slot >= 0 && CallOptionUtils.isSimInsert(request, slot)) {
                        associateSim = temp;
                        break;
                    }
                }
            }
            
            if (associateSimInserts == 1) {
                if (defaultSim == associateSim) {
                    callbackArgs.reason = MAKE_CALL_REASON_OK;
                    callbackArgs.id = defaultSim;
                } else {
                    callbackArgs.reason = MAKE_CALL_REASON_ASK;
                    callbackArgs.args = associateSim;
                }
                onMakeCall(callbackArgs);
                return;
            }
        }

        // both has orignalSim and associateSim ...
        if (defaultSim == originalSim && defaultSim == associateSim) {
            callbackArgs.reason = MAKE_CALL_REASON_OK;
            callbackArgs.id = defaultSim;
            onMakeCall(callbackArgs);
            return;
        }
        
        //default is the orignal sim and associateSim missing
        if (defaultSim == originalSim && hasAssociateSims && associateSimInserts == 0) {
            callbackArgs.reason = MAKE_CALL_REASON_ASK;
            callbackArgs.args = originalSim;
            onMakeCall(callbackArgs);
            return;
        }
        
        //associateSim missing and the original sim exist (ALPS00251172)
         if (originalSim != Settings.System.DEFAULT_SIM_NOT_SET &&
             hasAssociateSims && associateSimInserts == 0) {
             final int slot = simInfoWrapper.getSimSlotById((int)originalSim);
             if ((slot >= 0) && (originalSim != defaultSim)
                     && CallOptionUtils.isSimInsert(request, slot)) {
                 callbackArgs.args = originalSim;
                 callbackArgs.reason = MAKE_CALL_REASON_ASK;
                 onMakeCall(callbackArgs);
                 return;
             }
         }
        
        if (associateSimInserts >= 2) {
            callbackArgs.reason = MAKE_CALL_REASON_ASK;
            callbackArgs.args = Settings.System.DEFAULT_SIM_NOT_SET;
            onMakeCall(callbackArgs);
            return;
        }
        
        if (associateSimInserts == 1) {
            callbackArgs.reason = MAKE_CALL_REASON_ASK;
            callbackArgs.args = associateSim;
        } else {
            //If the associate sim missing, the associateSim maybe not be set.
            if (associateSim == Settings.System.DEFAULT_SIM_NOT_SET) {
                associateSim = ((Integer)associateSims.get(0)).intValue();
            }
            callbackArgs.id = associateSim;
            callbackArgs.reason = MAKE_CALL_REASON_ASSOCIATE_MISSING;
            AssociateSimMissingArgs associateSimMissingArgs = new AssociateSimMissingArgs();
            if (SIMInfoWrapper.getDefault().getInsertedSimCount() <= 1) {
                associateSimMissingArgs.type = AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_NO;

                final long viaSimId = originalSimInsert ? originalSim : defaultSim;

                if (defaultSim == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                    associateSimMissingArgs.suggested = Settings.System.VOICE_CALL_SIM_SETTING_INTERNET;
                } else {
                    associateSimMissingArgs.viaSimInfo = simInfoWrapper.getSimInfoById((int)viaSimId);
                }
            } else {
                associateSimMissingArgs.type = AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_OTHER;
                associateSimMissingArgs.suggested = originalSimInsert ? originalSim : defaultSim;
                if (defaultSim != Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK 
                        && defaultSim != Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                    associateSimMissingArgs.viaSimInfo = simInfoWrapper.getSimInfoById((int)defaultSim);
                }
            }
            callbackArgs.args = associateSimMissingArgs;
        }
        onMakeCall(callbackArgs);
    }

    /**
     * make the call when the pre-handle finish
     * @param args the args used to judge the call conditions
     */
    public void onMakeCall(final CallbackArgs args) {

        log("onMakeCall, reason = " + args.reason + " args = " + args.args);

        switch (args.reason) {
            case MAKE_CALL_REASON_OK:
                int slot = SIMInfoWrapper.getDefault().getSimSlotById((int)args.id);

                // if slot == -1, it's likely that no sim cards inserted
                // using slot 0 by default
                if (slot == -1) {
                    slot = 0;
                }

                mRequest.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(mRequest);
                }
                break;

            case MAKE_CALL_REASON_ASK:
                final long suggestedSim = 
                    args.args == null ? Settings.System.DEFAULT_SIM_NOT_SET : ((Long)args.args).longValue();
                boolean addInternet =
                    (!mRequest.getIntent().getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)
                            // !!!! need confirm below judgement is needed or not,
                            // Yajun daily use issue maybe can solve no insert SIM card issue
                            || SIMInfoWrapper.getDefault().getInsertedSimCount() == 0)
                    && Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE 
                            != mRequest.getIntent().getIntExtra(Constants.EXTRA_INTERNATIONAL_DIAL_OPTION,
                                                                Constants.INTERNATIONAL_DIAL_OPTION_NORMAL);
                //boolean isIpCall = mRequest.getIntent().getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)
                //                       && SIMInfoWrapper.getDefault().getInsertedSimCount() > 0;
                if (Constants.VOICEMAIL_URI.equals(mRequest.getIntent().getData().toString())) {
                    addInternet = false;
                }
                showReasonAskDialog(mRequest, suggestedSim, addInternet, mReasonAskClickListener,
                                    mReasonAskDismissListener, mReasonAskCancelListener);
                break;

            case MAKE_CALL_REASON_ASSOCIATE_MISSING:
                mAssociateSimMissingArgs = (AssociateSimMissingArgs) args.args;
                SIMInfo associateSimInfo = SIMInfoWrapper.getDefault().getSimInfoById((int)args.id);
                showAssociateMissingDialog(mRequest, associateSimInfo, args.number,
                                           (AssociateSimMissingArgs) args.args,
                                           mAssociateMissingClickListener,
                                           mAssociateMissingDismissListener,
                                           mAssociateMissingCancelListener);
                break;

            default:
                log("onMakeCall: no match case found!");
                break;
        }
    }

    /**
     * the listener for a click event for the Dialog
     * @param dialog which the button belong to
     * @param which which button is cliecked
     */
    public void onClick(DialogInterface dialog, int which) {
        log("onClick, dialog = " + dialog + " which = " + which);
        //if (dialog == mDialogs[CallOptionHelper.MAKE_CALL_REASON_ASK]) {
        if (true) {
            final AlertDialog alert = (AlertDialog) dialog;
            final ListAdapter listAdapter = alert.getListView().getAdapter();
            final int slot = ((Integer)listAdapter.getItem(which)).intValue();

            // Below is for daily use issue, need to be deleted and does not add internet call item for ip dial
            log("onClick, slot = " + slot);
            if (slot == (int)Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                dialog.dismiss();
                doSipCallOptionHandle();
            } else {
                dialog.dismiss();
                mRequest.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(mRequest);
                }
            }
            //dialog.dismiss();
            //mClicked = true;
        } else { //if (dialog == mDialogs[CallOptionHelper.MAKE_CALL_REASON_ASSOCIATE_MISSING]) {
            AlertDialog alert = (AlertDialog) dialog;
            if (mAssociateSimMissingArgs != null) {
                if (which == alert.BUTTON_POSITIVE) {
                    if (mAssociateSimMissingArgs.viaSimInfo != null) {
                        // via SIM
                        final int slot = mAssociateSimMissingArgs.viaSimInfo.mSlot;
                        mRequest.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
                        if (null != mSuccessor) {
                            mSuccessor.handleRequest(mRequest);
                        }
                        dialog.dismiss();
                    } else {
                        // via internet
                        doSipCallOptionHandle();
                    }
                } else if (which == alert.BUTTON_NEGATIVE) {
                    // user click 'other' button, show SIM selection dialog
                    // with default SIM suggested
                    CallbackArgs callbackArgs = new CallbackArgs();
                    callbackArgs.args = mAssociateSimMissingArgs.suggested;
                    callbackArgs.reason = MAKE_CALL_REASON_ASK;
                    onMakeCall(callbackArgs);
                }
                mAssociateSimMissingArgs = null;
            }
        }
    }

    protected abstract void showReasonAskDialog(final Request request, final long suggestedSim, final boolean isIpCall,
                                       DialogInterface.OnClickListener clickListener,
                                       DialogInterface.OnDismissListener dismissListener,
                                       DialogInterface.OnCancelListener cancelListener);

    private DialogInterface.OnClickListener mReasonAskClickListener
                                            = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            final AlertDialog alert = (AlertDialog) dialog;
            final ListAdapter listAdapter = alert.getListView().getAdapter();
            final int slot = ((Integer)listAdapter.getItem(which)).intValue();

            // Below is for daily use issue,
            // need to be deleted and does not add internet call item for ip dial
            log("ReasonAskDismissDialog, onClick() is called, slot = " + slot);
            if (slot == (int)Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                dialog.dismiss();
                doSipCallOptionHandle();
            } else {
                dialog.dismiss();
                mRequest.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(mRequest);
                }
            }
        }
    };

    private DialogInterface.OnDismissListener mReasonAskDismissListener
                                            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            log("ReasonAskDismissDialog, onDismiss() is called");
        }
    };

    private DialogInterface.OnCancelListener mReasonAskCancelListener
                                            = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            log("ReasonAskDismissDialog, onCancel() is called");
            mRequest.getResultHandler().onHandlingFinish();
        }
    };

    protected abstract void showAssociateMissingDialog(final Request request, SIMInfo associateSimInfo, String number,
                                              AssociateSimMissingArgs associateSimMissingArgs,
                                              DialogInterface.OnClickListener clickListener,
                                              DialogInterface.OnDismissListener dismissListener,
                                              DialogInterface.OnCancelListener cancelListener);

    private DialogInterface.OnClickListener mAssociateMissingClickListener
                                            = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (DialogInterface.BUTTON_POSITIVE == which) {
                AlertDialog alert = (AlertDialog) dialog;
                if (mAssociateSimMissingArgs != null) {
                    if (mAssociateSimMissingArgs.viaSimInfo != null) {
                        // via SIM
                        dialog.dismiss();
                        final int slot = mAssociateSimMissingArgs.viaSimInfo.mSlot;
                        mRequest.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
                        if (null != mSuccessor) {
                            mSuccessor.handleRequest(mRequest);
                        }
                    } else {
                        dialog.dismiss();
                        // via internet
                        doSipCallOptionHandle();
                    }
                }
            } else if (DialogInterface.BUTTON_NEGATIVE == which) {
                if (AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_NO == mAssociateSimMissingArgs.type) {
                    dialog.cancel();
                } else if (AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_OTHER == mAssociateSimMissingArgs.type) {
                    // user click 'other' button, show SIM selection dialog
                    // with default SIM suggested
                    CallbackArgs callbackArgs = new CallbackArgs();
                    callbackArgs.args = mAssociateSimMissingArgs.suggested;
                    callbackArgs.reason = MAKE_CALL_REASON_ASK;
                    onMakeCall(callbackArgs);
                }
            }
        }
    };

    private DialogInterface.OnDismissListener mAssociateMissingDismissListener
                                            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
        }
    };

    private DialogInterface.OnCancelListener mAssociateMissingCancelListener
                                            = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            mRequest.getResultHandler().onHandlingFinish();
        }
    };

    private void doSipCallOptionHandle() {
        log("doSipCallOptionHandle()");
        if (null == mRequest.getIntent()) {
            log("doSipCallOptionHandle(), mRequest.getIntent() == null");
            return;
        }
        // extra follow sim manage already finish its task, so set it as false
        mRequest.getIntent().putExtra(Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, false);
        final String number = CallOptionUtils.getInitialNumber(mRequest.getApplicationContext(),
                                                               mRequest.getIntent());
        mRequest.getIntent().setData(Uri.fromParts(Constants.SCHEME_SIP, number, null));
        // below should use call option factory to create new InternetCallOptionHandler
        // because some functions of InternetCallOptionHandler do nothing
        mRequest.getCallOptionHandlerFactory().getInternetCallOptionHandler().handleRequest(mRequest);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}