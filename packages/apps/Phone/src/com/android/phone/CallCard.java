/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.animation.LayoutTransition;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.phone.DualTalkUtils;
import com.mediatek.phone.HyphonManager;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.gemini.GeminiUtils;
import com.mediatek.phone.vt.VTCallUtils;

import java.util.List;

/**
 * "Call card" UI element: the in-call screen contains a tiled layout of call
 * cards, each representing the state of a current "call" (ie. an active call,
 * a call on hold, or an incoming call.)
 */
public class CallCard extends LinearLayout
        implements CallTime.OnTickListener, CallerInfoAsyncQuery.OnQueryCompleteListener,
                   ContactsAsyncHelper.OnImageLoadCompleteListener {
    private static final String LOG_TAG = "CallCard";
    private static final boolean DBG = true;//(PhoneGlobals.DBG_LEVEL >= 2);

    private static final int TOKEN_UPDATE_PHOTO_FOR_CALL_STATE = 0;
    private static final int TOKEN_DO_NOTHING = 1;

    /**
     * Used with {@link ContactsAsyncHelper#startObtainPhotoAsync(int, Context, Uri,
     * ContactsAsyncHelper.OnImageLoadCompleteListener, Object)}
     */
    private static class AsyncLoadCookie {
        public final ImageView imageView;
        public final CallerInfo callerInfo;
        public final Call call;
        public AsyncLoadCookie(ImageView imageView, CallerInfo callerInfo, Call call) {
            this.imageView = imageView;
            this.callerInfo = callerInfo;
            this.call = call;
        }
    }

    /**
     * Reference to the InCallScreen activity that owns us.  This may be
     * null if we haven't been initialized yet *or* after the InCallScreen
     * activity has been destroyed.
     */
    private InCallScreen mInCallScreen;

    // Phone app instance
    private PhoneGlobals mApplication;

    // Top-level subviews of the CallCard
    /** Container for info about the current call(s) */
    private ViewGroup mCallInfoContainer;
    /** Primary "call info" block (the foreground or ringing call) */
    private ViewGroup mPrimaryCallInfo;
    /** "Call banner" for the primary call */
    private ViewGroup mPrimaryCallBanner;
    /** Secondary "call info" block (the background "on hold" call) */
    private ViewStub mSecondaryCallInfo;

    /**
     * Container for both provider info and call state. This will take care of showing/hiding
     * animation for those views.
     */
    private ViewGroup mSecondaryInfoContainer;
    private ViewGroup mProviderInfo;
    private TextView mProviderLabel;
    private TextView mProviderAddress;

    // "Call state" widgets
    private TextView mCallStateLabel;
    
    /** M: Change feature: display call duration in call status label */
    //private TextView mElapsedTime;

    // Text colors, used for various labels / titles
    private int mTextColorCallTypeSip;

    // The main block of info about the "primary" or "active" call,
    // including photo / name / phone number / etc.
    private ImageView mPhoto;
    private View mPhotoDimEffect;

    private TextView mName;
    private TextView mPhoneNumber;
    private TextView mLabel;
    private TextView mCallTypeLabel;
    // private TextView mSocialStatus;

    /**
     * Uri being used to load contact photo for mPhoto. Will be null when nothing is being loaded,
     * or a photo is already loaded.
     */
    private Uri mLoadingPersonUri;

    // Info about the "secondary" call, which is the "call on hold" when
    // two lines are in use.
    private TextView mSecondaryCallName;
    private TextView mSecondaryCallStatus;
    private ImageView mSecondaryCallPhoto;
    private View mSecondaryCallPhotoDimEffect;

    // Onscreen hint for the incoming call RotarySelector widget.
    private int mIncomingCallWidgetHintTextResId;
    private int mIncomingCallWidgetHintColorResId;

    //private CallTime mCallTime;

    // Track the state for the photo.
    private ContactsAsyncHelper.ImageTracker mPhotoTracker;

    // Cached DisplayMetrics density.
    private float mDensity;

    /**
     * Sent when it takes too long (MESSAGE_DELAY msec) to load a contact photo for the given
     * person, at which we just start showing the default avatar picture instead of the person's
     * one. Note that we will *not* cancel the ongoing query and eventually replace the avatar
     * with the person's photo, when it is available anyway.
     */
    private static final int MESSAGE_SHOW_UNKNOWN_PHOTO = 101;
    private static final int MESSAGE_DELAY = 500; // msec
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SHOW_UNKNOWN_PHOTO:
                    showImage(mPhoto, R.drawable.picture_unknown);
                    break;
                default:
                    Log.wtf(LOG_TAG, "mHandler: unexpected message: " + msg);
                    break;
            }
        }
    };
    ///M: #Only emergency# text replace in case IMEI locked @{
    private IServiceStateExt mServiceStateExt;
    ///@}

    public CallCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        ///M: #Only emergency# text replace in case IMEI locked @{
        mServiceStateExt = MediatekClassFactory.createInstance(IServiceStateExt.class, getContext());
        ///@}

        if (DBG) log("CallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);

        mApplication = PhoneGlobals.getInstance();

        //mCallTime = new CallTime(this);

        // create a new object to track the state for the photo.
        mPhotoTracker = new ContactsAsyncHelper.ImageTracker();

        if (DualTalkUtils.isSupportDualTalk()) {
            mDualTalk = DualTalkUtils.getInstance();
        }

        mDensity = getResources().getDisplayMetrics().density;
        mCallBannerSidePadding = getResources().getDimensionPixelSize(R.dimen.incoming_call_2_banner_side_padding);
        mCallBannerTopBottomPadding = getResources().getDimensionPixelSize(R.dimen.call_banner_top_bottom_padding);
        mSimIndicatorPaddingLeft = getResources().getDimensionPixelSize(R.dimen.call_banner_sim_indicator_padding_left);
        mSimIndicatorPaddingRight = getResources().getDimensionPixelSize(R.dimen.call_banner_sim_indicator_padding_right);
        if (DBG) log("- Density: " + mDensity);
    }

    /* package */ void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    @Override
    public void onTickForCallTimeElapsed(long timeElapsed) {
        // While a call is in progress, update the elapsed time shown
        // onscreen.
        updateElapsedTimeWidget(timeElapsed);
    }

    /* package */ /*void stopTimer() {
        mCallTime.cancelTimer();
    }*/

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("CallCard onFinishInflate(this = " + this + ")...");

        mCallInfoContainer = (ViewGroup) findViewById(R.id.call_info_container);
        mPrimaryCallInfo = (ViewGroup) findViewById(R.id.primary_call_info);
        mPrimaryCallBanner = (ViewGroup) findViewById(R.id.primary_call_banner);

        mSecondaryInfoContainer = (ViewGroup) findViewById(R.id.secondary_info_container);
        mProviderInfo = (ViewGroup) findViewById(R.id.providerInfo);
        mProviderLabel = (TextView) findViewById(R.id.providerLabel);
        mProviderAddress = (TextView) findViewById(R.id.providerAddress);
        mCallStateLabel = (TextView) findViewById(R.id.callStateLabel);
        /** M: Change feature: display call duration in call status label */
        //mElapsedTime = (TextView) findViewById(R.id.elapsedTime);

        // Text colors
        mTextColorCallTypeSip = getResources().getColor(R.color.incall_callTypeSip);

        // "Caller info" area, including photo / name / phone numbers / etc
        mPhoto = (ImageView) findViewById(R.id.photo);
        mPhotoDimEffect = findViewById(R.id.dim_effect_for_primary_photo);

        mName = (TextView) findViewById(R.id.name);
        mPhoneNumber = (TextView) findViewById(R.id.phoneNumber);
        mLabel = (TextView) findViewById(R.id.label);
        mCallTypeLabel = (TextView) findViewById(R.id.callTypeLabel);
        // mSocialStatus = (TextView) findViewById(R.id.socialStatus);

        // Secondary info area, for the background ("on hold") call
        mSecondaryCallInfo = (ViewStub) findViewById(R.id.secondary_call_info);

        /** M: Change feature @{ */
        mOperatorName = (TextView) findViewById(R.id.operatorName);
        mSimIndicator = (TextView) findViewById(R.id.simIndicator);
        mPhoneNumberGeoDescription = (TextView) findViewById(R.id.phoneNumberGeoDescription);

        if (DualTalkUtils.isSupportDualTalk()) {
            mIncomingCall2Info = (ViewGroup) findViewById(R.id.inset_incoming_call_2_info);
            //mIncomingCall2Info.setVisibility(View.VISIBLE);
            mPhotoIncomingPre = (ImageView) findViewById(R.id.inset_Incoming_call_2_Photo);

            m2ndIncomingName = (TextView) findViewById(R.id.incoming_call_2_name);
            m2ndIncomingState = (TextView) findViewById(R.id.incoming_call_2_state);
            mSecondIncomingCallBanner = (ViewGroup) findViewById(R.id.incoming_call_2_banner);

            mIncomingCall2PhotoDimEffect = findViewById(R.id.dim_effect_for_incoming_call_2_photo);
            mIncomingCall2PhotoDimEffect.setEnabled(true);
            mIncomingCall2PhotoDimEffect.setClickable(true);
            mIncomingCall2PhotoDimEffect.setOnClickListener(callCardListener);
            // Add a custom OnTouchListener to manually shrink the
            // "hit target".
            mIncomingCall2PhotoDimEffect
                    .setOnTouchListener(new SmallerHitTargetTouchListener());

            mIncomingCallInfoWidth = getContext().getResources().getDimensionPixelSize(
                    R.dimen.incoming_call_2_total_width);
            mIncomingCallInfoHeight = getContext().getResources().getDimensionPixelSize(
                    R.dimen.incoming_call_2_total_height);
            mIncomingCallInfoTopMargin = getContext().getResources().getDimensionPixelSize(
                    R.dimen.incoming_call_2_name_margintop);
        }

        ExtensionManager.getInstance().getCallCardExtension().onFinishInflate(this);
        /** @{ */
    }

    /**
     * Updates the state of all UI elements on the CallCard, based on the
     * current state of the phone.
     */
    /* package */ void updateState(CallManager cm) {
        if (DBG) log("updateState(" + cm + ")...");

        if (DBG) {
            PhoneUtils.dumpCallManager();
        }
        log("updateState: current active call is : " + cm.getActiveFgCall().getConnections());
        // Update the onscreen UI based on the current state of the phone.

        PhoneConstants.State state = cm.getState();  // IDLE, RINGING, or OFFHOOK
        Call ringingCall = cm.getFirstActiveRingingCall();
        Call fgCall = cm.getActiveFgCall();
        Call bgCall = cm.getFirstActiveBgCall();

        // Update the overall layout of the onscreen elements.
        if (!PhoneUtils.isLandscape(this.getContext())) {
            updateCallInfoLayout(state);
        }

        // If the FG call is dialing/alerting, we should display for that call
        // and ignore the ringing call. This case happens when the telephony
        // layer rejects the ringing call while the FG call is dialing/alerting,
        // but the incoming call *does* briefly exist in the DISCONNECTING or
        // DISCONNECTED state.
        if ((ringingCall.getState() != Call.State.IDLE)
                && (!fgCall.getState().isDialing() || DualTalkUtils.isSupportDualTalk()
                        && mDualTalk.isRingingWhenOutgoing())) {
            // A phone call is ringing, call waiting *or* being rejected
            // (ie. another call may also be active as well.)
            /** M: bug fix: ALPS000231023 @{ */
            /// M: bug fix: ALPS00383608, add fgCall.getState() == Call.State.DISCONNECTED
            ///    For 2G, Data connection and vioce can not exist at the same time.
            ///    when dialing a sip call through data connection, a MT voice call comes in, framework will reject it.
            ///    Then, fg = DISCONECTED, and rg = DISCONNECTING, and Phone state = Ringing;
            ///    ANd we should skip updateRingingcall, or callcard will display hangingup the MT call.
            final boolean skipUpdateRingingCall = (fgCall.getState() == Call.State.ACTIVE
                    || fgCall.getState() == Call.State.DISCONNECTING
                    || fgCall.getState() == Call.State.DISCONNECTED
                    || bgCall.getState() == Call.State.HOLDING)
                    && !ringingCall.getState().isAlive();
            if (skipUpdateRingingCall) {
                updateForegroundCall(cm);
                return;
            }
            /** @{ */

            updateRingingCall(cm);
        } else if ((fgCall.getState() != Call.State.IDLE)
                || (bgCall.getState() != Call.State.IDLE)) {
            // We are here because either:
            // (1) the phone is off hook. At least one call exists that is
            // dialing, active, or holding, and no calls are ringing or waiting,
            // or:
            // (2) the phone is IDLE but a call just ended and it's still in
            // the DISCONNECTING or DISCONNECTED state. In this case, we want
            // the main CallCard to display "Hanging up" or "Call ended".
            // The normal "foreground call" code path handles both cases.
            updateForegroundCall(cm);
        } else {
            // We don't have any DISCONNECTED calls, which means that the phone
            // is *truly* idle.
            if (mApplication.inCallUiState.showAlreadyDisconnectedState) {
                // showAlreadyDisconnectedState implies the phone call is disconnected
                // and we want to show the disconnected phone call for a moment.
                //
                // This happens when a phone call ends while the screen is off,
                // which means the user had no chance to see the last status of
                // the call. We'll turn off showAlreadyDisconnectedState flag
                // and bail out of the in-call screen soon.
                updateAlreadyDisconnected(cm);
            } else {
                // It's very rare to be on the InCallScreen at all in this
                // state, but it can happen in some cases:
                // - A stray onPhoneStateChanged() event came in to the
                //   InCallScreen *after* it was dismissed.
                // - We're allowed to be on the InCallScreen because
                //   an MMI or USSD is running, but there's no actual "call"
                //   to display.
                // - We're displaying an error dialog to the user
                //   (explaining why the call failed), so we need to stay on
                //   the InCallScreen so that the dialog will be visible.
                //
                // In these cases, put the callcard into a sane but "blank" state:
                updateNoCall(cm);
            }
        }
        /** M: Change feature: Add for easy porting */
        ExtensionManager.getInstance().getCallCardExtension().updateState(cm);
}

    /**
     * Updates the overall size and positioning of mCallInfoContainer and
     * the "Call info" blocks, based on the phone state.
     */
    private void updateCallInfoLayout(PhoneConstants.State state) {
        /** M: Change feature: Add for easy porting @{ */
        if (ExtensionManager.getInstance().getCallCardExtension()
                            .updateCallInfoLayout(state)) {
            return;
        }
        /** @} */

        boolean ringing = (state == PhoneConstants.State.RINGING);
        if (DBG) log("updateCallInfoLayout()...  ringing = " + ringing);

        // Based on the current state, update the overall
        // CallCard layout:

        // - Update the bottom margin of mCallInfoContainer to make sure
        //   the call info area won't overlap with the touchable
        //   controls on the bottom part of the screen.

        int reservedVerticalSpace;
        
        /** M: New Feature Phone Landscape UI @{ */
        if (PhoneUtils.isLandscape(this.getContext())) {
            //Call card will cover the right half of screen
            reservedVerticalSpace = 0;
        } else {
        /** @ }*/
            reservedVerticalSpace = mInCallScreen.getInCallTouchUi().getTouchUiHeight();
        }
        
        ViewGroup.MarginLayoutParams callInfoLp =
                (ViewGroup.MarginLayoutParams) getLayoutParams();
        callInfoLp.bottomMargin = reservedVerticalSpace;  // Equivalent to setting
                                                          // android:layout_marginBottom in XML
        if (DBG) log("  ==> callInfoLp.bottomMargin: " + reservedVerticalSpace);
        setLayoutParams(callInfoLp);
    }

    /**
     * Updates the UI for the state where the phone is in use, but not ringing.
     */
    private void updateForegroundCall(CallManager cm) {
        if (DBG) log("updateForegroundCall()...");
        // if (DBG) PhoneUtils.dumpCallManager();

        /** M: Change feature: Add for dual talk @{ */
        Call fgCall = null;
        Call bgCall = null;
        
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.isDualTalkMultipleHoldCase()) {
            //Three calls exist.
            //Note: For C+G platform, the only case is: CDMA has a Active call + GSM has the Active call and Holding call
            fgCall = mDualTalk.getActiveFgCall();
            bgCall = mDualTalk.getFirstActiveBgCall();
        } else if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasDualHoldCallsOnly()) {
            //Only two holds exit
            //Note: for C+G project, must not go here(CDMA call always in ACTIVE status)
            fgCall = mDualTalk.getFirstActiveBgCall();
            bgCall = mDualTalk.getSecondActiveBgCall();
        } else if (DualTalkUtils.isSupportDualTalk() && mDualTalk.isCdmaAndGsmActive()) {
            //in that case, we always consider the CDMA "Hold" call has higher priority
            fgCall = mDualTalk.getActiveFgCall();
            bgCall = mDualTalk.getFirstActiveBgCall();
            if (DBG) {
                log("isCdmaAndGsmActive: fgCall = " + fgCall + "  bgCall = " + bgCall);
            }
        } else {
            fgCall = cm.getActiveFgCall();
            bgCall = cm.getFirstActiveBgCall();
            if (DBG) {
                log("updateForegroundCall: common case : fgCall " + fgCall + "  bgCall = " + bgCall);
            }
        }
        /** }@ */
        
        /** M: Bug fix: if there is an dialing call, then receives the DISCONNECTING ringing call message @{ */
        Call ringingCall = cm.getFirstActiveRingingCall();
        if (ringingCall != null && fgCall != null) {
            if ((ringingCall.getState() != Call.State.IDLE)
                    && fgCall.getState().isDialing()) {
                return;
            }
        }
        /** }@ */

        if (fgCall.getState() == Call.State.IDLE) {
            if (DBG) log("updateForegroundCall: no active call, show holding call");
            // TODO: make sure this case agrees with the latest UI spec.

            // Display the background call in the main info area of the
            // CallCard, since there is no foreground call.  Note that
            // displayMainCallStatus() will notice if the call we passed in is on
            // hold, and display the "on hold" indication.
            fgCall = bgCall;

            // And be sure to not display anything in the "on hold" box.
            bgCall = null;
        }

        /// M: for ALPS00606095 @{
        // should update mCallStateLabel to active fg call's time as soon as active fg call is shown.
        if (fgCall.getState() == Call.State.ACTIVE) {
            updateElapsedTimeWidget(fgCall);
        }
        /// @}
        displayMainCallStatus(cm, fgCall);

        Phone phone = fgCall.getPhone();

        /** M: Change feature: Modify for dual talk @{ */
        int phoneType = phone.getPhoneType();
        if (DBG) {
            log("updateForegroundCall: fgCall phoneType " + phoneType);
        }
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.isCdmaAndGsmActive()) {
            if (phoneType == PhoneConstants.PHONE_TYPE_GSM && (bgCall != null 
                    && bgCall.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)) {
                displaySecondaryCallStatus(cm, bgCall);
                displaySecondHoldCallStatus(mDualTalk.getSecondActiveBgCall());
            } else {
                displaySecondaryCallStatus(cm, null);
                displaySecondHoldCallStatus(bgCall);
            }
        } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            if ((mApplication.cdmaPhoneCallState.getCurrentCallState()
                    == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                    && mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                displaySecondaryCallStatus(cm, fgCall);
            } else {
                //This is required so that even if a background call is not present
                // we need to clean up the background call area.
                displaySecondaryCallStatus(cm, bgCall);
            }
        } else if (((phoneType == PhoneConstants.PHONE_TYPE_GSM) || (phoneType == PhoneConstants.PHONE_TYPE_SIP))) {
            boolean isSecondary = isSecondaryCallVisible(cm);
            if (isSecondary) {
                displaySecondaryCallStatus(cm, bgCall);
            } else {
                displaySecondaryCallStatus(cm, null);
            }
            
            if (isSecondHoldCallVisible(cm)) {
                if (isSecondary) {
                    displaySecondHoldCallStatus(mDualTalk.getSecondActiveBgCall());
                } else {
                    displaySecondHoldCallStatus(bgCall);
                }
            } else {
                displaySecondHoldCallStatus(null);
            }
        }
        /** }@ */
    }
    
    private boolean isSecondaryCallVisible(CallManager cm) {
        boolean result = false;
        if (DualTalkUtils.isSupportDualTalk() 
                && mDualTalk != null
                && mDualTalk.isDualTalkMultipleHoldCase()) {
            //there are two hold calls + one active call, so must display secondary call.
            result = true;
        } else if (DualTalkUtils.isSupportDualTalk() 
                && mDualTalk != null
                && mDualTalk.hasDualHoldCallsOnly()) {
            //There are two hold call only
            Call firstHold = mDualTalk.getFirstActiveBgCall();
            Call secondHold = mDualTalk.getSecondActiveBgCall();
            result = firstHold.getPhone() == secondHold.getPhone();
        } else {
            Call fgCall = cm.getActiveFgCall();
            Call bgCall = cm.getFirstActiveBgCall();
            if (fgCall != null && (fgCall.getState() != Call.State.IDLE) && bgCall != null && bgCall.getState().isAlive()) {
                result = fgCall.getPhone() == bgCall.getPhone();
            }
        }
        
        return result;
    }
    
    private boolean isSecondHoldCallVisible(CallManager cm) {
        boolean result = false;
        if (DualTalkUtils.isSupportDualTalk()
                && mDualTalk != null
                && mDualTalk.isDualTalkMultipleHoldCase()) {
            //There are two hold calls, so must display second hold call.
            result = true;
        } else if (DualTalkUtils.isSupportDualTalk() 
                && mDualTalk != null
                && mDualTalk.hasDualHoldCallsOnly()) {
            //There are two hold call only
            Call firstHold = mDualTalk.getFirstActiveBgCall();
            Call secondHold = mDualTalk.getSecondActiveBgCall();
            result = firstHold.getPhone() != secondHold.getPhone();
        } else {
            Call fgCall = cm.getActiveFgCall();
            Call bgCall = cm.getFirstActiveBgCall();
            if (fgCall != null && (fgCall.getState() != Call.State.IDLE) && bgCall != null && bgCall.getState().isAlive()) {
                result = fgCall.getPhone() != bgCall.getPhone();
            }
        }
        
        return result;
    }

    /**
     * Updates the UI for the state where an incoming call is ringing (or
     * call waiting), regardless of whether the phone's already offhook.
     */
    private void updateRingingCall(CallManager cm) {
        if (DBG) log("updateRingingCall()...");

        /** M: Change feature: Modify for dual talk @{ */
        Call ringingCall = null;
        
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasMultipleRingingCall()) {
            ringingCall = mDualTalk.getFirstActiveRingingCall();
        } else {
            ringingCall = cm.getFirstActiveRingingCall();
        }

        // Display caller-id info and photo from the incoming call:
        displayMainCallStatus(cm, ringingCall);

        //Display second incoming call info for dualtalk
        if (DualTalkUtils.isSupportDualTalk()) {
            if (mDualTalk.hasMultipleRingingCall()) {
                displaySecondIncomingCallStatus(mDualTalk.getSecondActiveRingCall());
            } else {
                displaySecondIncomingCallStatus(null);
            }
        }
        /** }@ */

        // And even in the Call Waiting case, *don't* show any info about
        // the current ongoing call and/or the current call on hold.
        // (Since the caller-id info for the incoming call totally trumps
        // any info about the current call(s) in progress.)
        displaySecondaryCallStatus(cm, null);
        displaySecondHoldCallStatus(null);
    }

    /**
     * Updates the UI for the state where an incoming call is just disconnected while we want to
     * show the screen for a moment.
     *
     * This case happens when the whole in-call screen is in background when phone calls are hanged
     * up, which means there's no way to determine which call was the last call finished. Right now
     * this method simply shows the previous primary call status with a photo, closing the
     * secondary call status. In most cases (including conference call or misc call happening in
     * CDMA) this behaves right.
     *
     * If there were two phone calls both of which were hung up but the primary call was the
     * first, this would behave a bit odd (since the first one still appears as the
     * "last disconnected").
     */
    private void updateAlreadyDisconnected(CallManager cm) {
        // For the foreground call, we manually set up every component based on previous state.
        mPrimaryCallInfo.setVisibility(View.VISIBLE);
        mSecondaryInfoContainer.setLayoutTransition(null);
        mProviderInfo.setVisibility(View.GONE);
        mCallStateLabel.setVisibility(View.VISIBLE);
        mCallStateLabel.setText(mContext.getString(R.string.card_title_call_ended));
        /** M: Change feature: Display call duration in call status label @{ */
        // !!!! mElapsedTime is deleted by MTK, but need consider other sub GUIs whether needs set here
        //mElapsedTime.setVisibility(View.VISIBLE);
        /** }@ */
        //mCallTime.cancelTimer();

        // Just hide it.
        displaySecondaryCallStatus(cm, null);
        displaySecondHoldCallStatus(null); 
    }

    /**
     * Updates the UI for the state where the phone is not in use.
     * This is analogous to updateForegroundCall() and updateRingingCall(),
     * but for the (uncommon) case where the phone is
     * totally idle.  (See comments in updateState() above.)
     *
     * This puts the callcard into a sane but "blank" state.
     */
    private void updateNoCall(CallManager cm) {
        if (DBG) log("updateNoCall()...");

        /** M: Bug fix: This is ugly and boring for ALPS00111659 (dial out an long invalid number) @{ */
        InCallUiState.FakeCall fakeCall = mApplication.inCallUiState.latestDisconnectCall;
        if (fakeCall != null) {
            displayFakeCallStatus(fakeCall);
        } else {
            displayMainCallStatus(cm, null);
            displaySecondaryCallStatus(cm, null);
            displaySecondHoldCallStatus(null); 
        }
        /** }@ */
    }

    /**
     * Updates the main block of caller info on the CallCard
     * (ie. the stuff in the primaryCallInfo block) based on the specified Call.
     */
    private void displayMainCallStatus(CallManager cm, Call call) {
        if (DBG) log("displayMainCallStatus(call " + call + ")...");

        if (call == null) {
            // There's no call to display, presumably because the phone is idle.
            mPrimaryCallInfo.setVisibility(View.GONE);
            return;
        }

        /** M: Change feature @{ */
        if (DBG) {
            log("displayMainCallStatus(call " + call.getConnections() + ")...");
        }

        mPrimaryCallInfo.setVisibility(View.VISIBLE);

        updateCallStateWidgets(call);

        // get sim information: display name and color to mSimInfo
        //updateSimInfo(call);
        SIMInfo simInfo = PhoneUtils.getSimInfoByCall(call);
        if (simInfo != null && !TextUtils.isEmpty(simInfo.mDisplayName)
                && (call.getPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_SIP)) {
            mSimIndicator.setText(simInfo.mDisplayName);
            mSimIndicator.setVisibility(View.VISIBLE);
        } else if (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
            mSimIndicator.setText(R.string.incall_call_type_label_sip);
            mSimIndicator.setVisibility(View.VISIBLE);
        } else {
            mSimIndicator.setVisibility(View.GONE);
        }

        // update call banner background according to mSimInfo.mColor
        updateCallBannerBackground(call, mPrimaryCallBanner);
        if (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
            //For sip call, always display "internet call"
            mOperatorName.setVisibility(View.VISIBLE);
            mOperatorName.setText(R.string.incall_call_type_label_sip);
        } else {
            if (!PhoneUtils.isEccCall(call)) {
                mOperatorName.setText(GeminiUtils.getNetworkOperatorName(call));
                mOperatorName.setVisibility(View.VISIBLE);
            } else {
                mOperatorName.setVisibility(View.GONE);
            }
        }
        /** }@ */

        if (PhoneUtils.isConferenceCall(call)) {
            // Update onscreen info for a conference call.
            updateDisplayForConference(call);
        } else {
            // Update onscreen info for a regular call (which presumably
            // has only one connection.)
            Connection conn = null;
            int phoneType = call.getPhone().getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                conn = call.getLatestConnection();
            } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                  || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                conn = call.getEarliestConnection();
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }

            if (conn == null) {
                if (DBG) log("displayMainCallStatus: connection is null, using default values.");
                // if the connection is null, we run through the behaviour
                // we had in the past, which breaks down into trivial steps
                // with the current implementation of getCallerInfo and
                // updateDisplayForPerson.
                CallerInfo info = PhoneUtils.getCallerInfo(getContext(), null /* conn */);
                updateDisplayForPerson(info, PhoneConstants.PRESENTATION_ALLOWED, false, call, conn);
            } else {
                if (DBG) log("  - CONN: " + conn + ", state = " + conn.getState());
                int presentation = conn.getNumberPresentation();

                // make sure that we only make a new query when the current
                // callerinfo differs from what we've been requested to display.
                boolean runQuery = true;
                Object o = conn.getUserData();
                if (o instanceof PhoneUtils.CallerInfoToken) {
                    runQuery = mPhotoTracker.isDifferentImageRequest(
                            ((PhoneUtils.CallerInfoToken) o).currentInfo);
                } else {
                    runQuery = mPhotoTracker.isDifferentImageRequest(conn);
                }

                // Adding a check to see if the update was caused due to a Phone number update
                // or CNAP update. If so then we need to start a new query
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    Object obj = conn.getUserData();
                    String updatedNumber = conn.getAddress();
                    String updatedCnapName = conn.getCnapName();
                    CallerInfo info = null;
                    if (obj instanceof PhoneUtils.CallerInfoToken) {
                        info = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                    } else if (o instanceof CallerInfo) {
                        info = (CallerInfo) o;
                    }

                    if (info != null) {
                        if (updatedNumber != null && !updatedNumber.equals(info.phoneNumber)) {
                            if (DBG) log("- displayMainCallStatus: updatedNumber = "
                                    + updatedNumber);
                            runQuery = true;
                        }
                        if (updatedCnapName != null && !updatedCnapName.equals(info.cnapName)) {
                            if (DBG) log("- displayMainCallStatus: updatedCnapName = "
                                    + updatedCnapName);
                            runQuery = true;
                        }
                    }
                }

                if (runQuery) {
                    if (DBG) log("- displayMainCallStatus: starting CallerInfo query...");
                    /** M: Change feature @{ */
                    if (mLCforUserData) {
                        if (DBG) {
                            log("- displayMainCallStatus: the language changed to clear userdata");
                        }
                        conn.clearUserData();
                        mLCforUserData = false;
                    }

                    CallInfoCookie callInfoCookie = new CallInfoCookie(call, PRIMARY_CALL_BANNER);
                    /** }@ */
                    PhoneUtils.CallerInfoToken info =
                            PhoneUtils.startGetCallerInfo(getContext(), conn, this, callInfoCookie);
                    updateDisplayForPerson(info.currentInfo, presentation, !info.isFinal,
                                           call, conn);
                } else {
                    // No need to fire off a new query.  We do still need
                    // to update the display, though (since we might have
                    // previously been in the "conference call" state.)
                    if (DBG) log("- displayMainCallStatus: using data we already have...");
                    if (o instanceof CallerInfo) {
                        CallerInfo ci = (CallerInfo) o;
                        // Update CNAP information if Phone state change occurred
                        ci.cnapName = conn.getCnapName();
                        ci.numberPresentation = conn.getNumberPresentation();
                        ci.namePresentation = conn.getCnapNamePresentation();
                        if (DBG) log("- displayMainCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                        if (DBG) log("   ==> Got CallerInfo; updating display: ci = " + ci);
                        updateDisplayForPerson(ci, presentation, false, call, conn);
                    } else if (o instanceof PhoneUtils.CallerInfoToken) {
                        CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        if (DBG) log("- displayMainCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                        if (DBG) log("   ==> Got CallerInfoToken; updating display: ci = " + ci);
                        updateDisplayForPerson(ci, presentation, true, call, conn);
                    } else {
                        Log.w(LOG_TAG, "displayMainCallStatus: runQuery was false, "
                              + "but we didn't have a cached CallerInfo object!  o = " + o);
                        // TODO: any easy way to recover here (given that
                        // the CallCard is probably displaying stale info
                        // right now?)  Maybe force the CallCard into the
                        // "Unknown" state?
                    }
                }
            }
        }

        // In some states we override the "photo" ImageView to be an
        // indication of the current state, rather than displaying the
        // regular photo as set above.
        updatePhotoForCallState(call, mPhoto);

        // One special feature of the "number" text field: For incoming
        // calls, while the user is dragging the RotarySelector widget, we
        // use mPhoneNumber to display a hint like "Rotate to answer".
        if (mIncomingCallWidgetHintTextResId != 0) {
            // Display the hint!
            mPhoneNumber.setText(mIncomingCallWidgetHintTextResId);
            mPhoneNumber.setTextColor(getResources().getColor(mIncomingCallWidgetHintColorResId));
            mPhoneNumber.setVisibility(View.VISIBLE);
            mLabel.setVisibility(View.GONE);
        }
        // If we don't have a hint to display, just don't touch
        // mPhoneNumber and mLabel. (Their text / color / visibility have
        // already been set correctly, by either updateDisplayForPerson()
        // or updateDisplayForConference().)
    }

    /**
     * Implemented for CallerInfoAsyncQuery.OnQueryCompleteListener interface.
     * refreshes the CallCard data when it called.
     */
    @Override
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        if (DBG) log("onQueryComplete: token " + token + ", cookie " + cookie + ", ci " + ci);

        /** M: Change feature: update second call banner after change language @{ */
        if (cookie instanceof CallInfoCookie) {
            // grab the call object and update the display for an individual call,
            // as well as the successive call to update image via call state.
            // If the object is a textview instead, we update it as we need to.
            if (DBG) log("callerinfo query complete, cookie is CallInfo");
            CallInfoCookie callInfo = (CallInfoCookie) cookie;
            int bannerNumber = callInfo.bannerNumber;
            Call call = callInfo.call;
            Connection conn = null;
            int phoneType = call.getPhone().getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                conn = call.getLatestConnection();
            } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                  || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                conn = call.getEarliestConnection();
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            //PhoneUtils.CallerInfoToken cit =
            //       PhoneUtils.startGetCallerInfo(getContext(), conn, this, null);

            int presentation = PhoneConstants.PRESENTATION_ALLOWED;
            if (conn != null) presentation = conn.getNumberPresentation();
            if (DBG) log("- onQueryComplete: presentation=" + presentation
                    + ", contactExists=" + ci.contactExists);

            // Depending on whether there was a contact match or not, we want to pass in different
            // CallerInfo (for CNAP). Therefore if ci.contactExists then use the ci passed in.
            // Otherwise, regenerate the CIT from the Connection and use the CallerInfo from there.
            if (PRIMARY_CALL_BANNER == bannerNumber) {
                if (DBG) {
                    log("banner number is PRIMARY_CALL_BANNER");
                }
                PhoneUtils.CallerInfoToken cit =
                    PhoneUtils.startGetCallerInfo(getContext(), conn, this, null);
                if (ci.contactExists) {
                    updateDisplayForPerson(ci, PhoneConstants.PRESENTATION_ALLOWED, false, call, conn);
                } else {
                    updateDisplayForPerson(cit.currentInfo, presentation, false, call, conn);
                }
                updatePhotoForCallState(call, mPhoto);
            } else if (SECONDARY_CALL_BANNER == bannerNumber) {
                if (DBG) {
                    log("banner number is SECONDARY_CALL_BANNER");
                }
                PhoneUtils.CallerInfoToken infoToken =
                    PhoneUtils.startGetCallerInfo(getContext(), call, this, mSecondaryCallName);
                mSecondaryCallName.setText(
                        PhoneUtils.getCompactNameFromCallerInfo(infoToken.currentInfo,
                                                                getContext()));
                updateDisplayForPerson(infoToken.currentInfo, presentation, false, call, conn,
                        mSecondaryCallName, true, null, null, mSecondaryCallPhoto);
            } else if (SECOND_HOLD_CALL_BANNER == bannerNumber) {
                PhoneUtils.CallerInfoToken infoToken =
                        PhoneUtils.startGetCallerInfo(getContext(), call, this, m2ndHoldName);
                m2ndHoldName.setText(
                            PhoneUtils.getCompactNameFromCallerInfo(infoToken.currentInfo,
                                                                    getContext()));
                    updateDisplayForPerson(infoToken.currentInfo, presentation, false, call, conn,
                            m2ndHoldName, true, null, null, mPhotoHoldPre);
            }
            /** }@ */

        } else if (cookie instanceof TextView) {
            if (DBG) log("callerinfo query complete, updating ui from ongoing or onhold");
            ((TextView) cookie).setText(PhoneUtils.getCompactNameFromCallerInfo(ci, mContext));
        } else if (cookie instanceof InCallUiState.FakeCall) {
            log("callerinfo query complete, updating fake call ui!");
            if (ci != null) {
                log("ci.name = " + ci.name);
                log("ci.phoneNumber = " + ci.phoneNumber);
                if (!TextUtils.isEmpty(ci.name)) {
                    mName.setText(ci.name);
                }
            }
        }
    }

    /**
     * Implemented for ContactsAsyncHelper.OnImageLoadCompleteListener interface.
     * make sure that the call state is reflected after the image is loaded.
     */
    @Override
    public void onImageLoadComplete(int token, Drawable photo, Bitmap photoIcon, Object cookie) {
        mHandler.removeMessages(MESSAGE_SHOW_UNKNOWN_PHOTO);
        if (mLoadingPersonUri != null) {
            // Start sending view notification after the current request being done.
            // New image may possibly be available from the next phone calls.
            //
            // TODO: may be nice to update the image view again once the newer one
            // is available on contacts database.
            PhoneUtils.sendViewNotificationAsync(mApplication, mLoadingPersonUri);
        } else {
            // This should not happen while we need some verbose info if it happens..
            Log.w(LOG_TAG, "Person Uri isn't available while Image is successfully loaded.");
        }
        mLoadingPersonUri = null;

        AsyncLoadCookie asyncLoadCookie = (AsyncLoadCookie) cookie;
        CallerInfo callerInfo = asyncLoadCookie.callerInfo;
        ImageView imageView = asyncLoadCookie.imageView;
        Call call = asyncLoadCookie.call;

        callerInfo.cachedPhoto = photo;
        callerInfo.cachedPhotoIcon = photoIcon;
        callerInfo.isCachedPhotoCurrent = true;

        // Note: previously ContactsAsyncHelper has done this job.
        // TODO: We will need fade-in animation. See issue 5236130.
        if (photo != null) {
            showImage(imageView, photo);
        } else if (photoIcon != null) {
            showImage(imageView, photoIcon);
        } else {
            showImage(imageView, R.drawable.picture_unknown);
        }

        if (token == TOKEN_UPDATE_PHOTO_FOR_CALL_STATE) {
            updatePhotoForCallState(call, imageView);
        }
    }

    /**
     * Updates the "call state label" and the elapsed time widget based on the
     * current state of the call.
     */
    private void updateCallStateWidgets(Call call) {
        if (DBG) log("updateCallStateWidgets(call " + call + ")...");
        final Call.State state = call.getState();
        final Context context = getContext();
        final Phone phone = call.getPhone();
        final int phoneType = phone.getPhoneType();

        String callStateLabel = null;  // Label to display as part of the call banner
        int bluetoothIconId = 0;  // Icon to display alongside the call state label

        switch (state) {
            case IDLE:
                // "Call state" is meaningless in this state.
                /** M: Modify for CR: set callStateLabel as empty, not null @{ */
                callStateLabel = "";
                /** }@ */
                break;

            case ACTIVE:
                // We normally don't show a "call state label" at all in
                // this state (but see below for some special cases).
                break;

            case HOLDING:
                callStateLabel = context.getString(R.string.card_title_on_hold);
                break;

            case DIALING:
            case ALERTING:
                callStateLabel = context.getString(R.string.card_title_dialing);
                break;

            case INCOMING:
            case WAITING:
                /** M: Change feature: Display "incoming video call if video call" @{ */
                if (VTCallUtils.isVideoCall(call)) {
                    callStateLabel = context.getString(R.string.card_title_incoming_vt_call);
                } else {
                    callStateLabel = context.getString(R.string.card_title_incoming_call);
                }
                /** }@ */

                // Also, display a special icon (alongside the "Incoming call"
                // label) if there's an incoming call and audio will be routed
                // to bluetooth when you answer it.
                if (mApplication.showBluetoothIndication()) {
                    bluetoothIconId = R.drawable.ic_incoming_call_bluetooth;
                }
                break;

            case DISCONNECTING:
                // While in the DISCONNECTING state we display a "Hanging up"
                // message in order to make the UI feel more responsive.  (In
                // GSM it's normal to see a delay of a couple of seconds while
                // negotiating the disconnect with the network, so the "Hanging
                // up" state at least lets the user know that we're doing
                // something.  This state is currently not used with CDMA.)
                callStateLabel = context.getString(R.string.card_title_hanging_up);
                break;

            case DISCONNECTED:
                callStateLabel = getCallFailedString(call);
                break;

            default:
                Log.wtf(LOG_TAG, "updateCallStateWidgets: unexpected call state: " + state);
                break;
        }

        // Check a couple of other special cases (these are all CDMA-specific).

        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            if ((state == Call.State.ACTIVE)
                && mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                // Display "Dialing" while dialing a 3Way call, even
                // though the foreground call state is actually ACTIVE.
                callStateLabel = context.getString(R.string.card_title_dialing);
            } else if (PhoneGlobals.getInstance().notifier.getIsCdmaRedialCall()) {
                callStateLabel = context.getString(R.string.card_title_redialing);
            }
        }
        if (PhoneUtils.isPhoneInEcm(phone)) {
            // In emergency callback mode (ECM), use a special label
            // that shows your own phone number.
            callStateLabel = PhoneUtils.getECMCardTitle(context, phone);
        }

        final InCallUiState inCallUiState = mApplication.inCallUiState;
        if (DBG) {
            log("==> callStateLabel: '" + callStateLabel
                    + "', bluetoothIconId = " + bluetoothIconId
                    + ", providerInfoVisible = " + inCallUiState.providerInfoVisible);
        }

        // Animation will be done by mCallerDetail's LayoutTransition, but in some cases, we don't
        // want that.
        // - DIALING: This is at the beginning of the phone call.
        // - DISCONNECTING, DISCONNECTED: Screen will disappear soon; we have no time for animation.
        final boolean skipAnimation = (state == Call.State.DIALING
                || state == Call.State.DISCONNECTING
                || state == Call.State.DISCONNECTED);
        LayoutTransition layoutTransition = null;
        if (skipAnimation) {
            // Evict LayoutTransition object to skip animation.
            layoutTransition = mSecondaryInfoContainer.getLayoutTransition();
            mSecondaryInfoContainer.setLayoutTransition(null);
        }

        if (inCallUiState.providerInfoVisible) {
            mProviderInfo.setVisibility(View.VISIBLE);
            mProviderLabel.setText(context.getString(R.string.calling_via_template,
                    inCallUiState.providerLabel));
            mProviderAddress.setText(inCallUiState.providerAddress);

            mInCallScreen.requestRemoveProviderInfoWithDelay();
        } else {
            mProviderInfo.setVisibility(View.GONE);
        }

        /** M: Change feature @{ */
        //if (!TextUtils.isEmpty(callStateLabel)) {
            mCallStateLabel.setVisibility(View.VISIBLE);
            /** M: Modify for CR: if callStateLabel is null, not set label to avoid call time flicking @{ */
            if (null != callStateLabel) {
                mCallStateLabel.setText(callStateLabel);
            }
            /** }@ */
            if (DualTalkUtils.isSupportDualTalk()) {
                log("DualTalkUtils.isSupportDualTalk()");
                /*
                if (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                    mCallStateLabel.setBackgroundResource(R.drawable.incall_status_color8);
                } else {
                    SIMInfo simInfo = getSimInfoByCall(call);
                    if (simInfo != null) {
                        mCallStateLabel.setBackgroundResource(mSimColorMap[simInfo.mColor]);
                    }
                }*/
                //mCallStateLabel.getBackground().setAlpha(125);
            }

            // ...and display the icon too if necessary.
            if (bluetoothIconId != 0) {
                if (FeatureOption.MTK_PHONE_NUMBER_GEODESCRIPTION) {
                    mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, bluetoothIconId, 0);
                } else {
                mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(bluetoothIconId, 0, 0, 0);
                }
                mCallStateLabel.setCompoundDrawablePadding((int) (mDensity * 5));
            } else {
                // Clear out any icons
                mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        //} else {
        //    mCallStateLabel.setVisibility(View.GONE);
            // Gravity is aligned left when receiving an incoming call in landscape.
            // In that rare case, the gravity needs to be reset to the right.
            // Also, setText("") is used since there is a delay in making the view GONE,
            // so the user will otherwise see the text jump to the right side before disappearing.
            //if(mCallStateLabel.getGravity() != Gravity.RIGHT) {
            //    mCallStateLabel.setText("");
            //    mCallStateLabel.setGravity(Gravity.RIGHT);
            //}
        //}
        /** }@ */

        if (skipAnimation) {
            // Restore LayoutTransition object to recover animation.
            mSecondaryInfoContainer.setLayoutTransition(layoutTransition);
        }

        /** M: Change feature: Display call duration in call status label @{ */
        // ...and update the elapsed time widget too.
        /*switch (state) {
            case ACTIVE:
            case DISCONNECTING:
                // Show the time with fade-in animation.
                AnimationUtils.Fade.show(mElapsedTime);
                updateElapsedTimeWidget(call);
                break;

            case DISCONNECTED:
                // In the "Call ended" state, leave the mElapsedTime widget
                // visible, but don't touch it (so we continue to see the
                // elapsed time of the call that just ended.)
                // Check visibility to keep possible fade-in animation.
                if (mElapsedTime.getVisibility() != View.VISIBLE) {
                    mElapsedTime.setVisibility(View.VISIBLE);
                }
                break;

            default:
                // Call state here is IDLE, ACTIVE, HOLDING, DIALING, ALERTING,
                // INCOMING, or WAITING.
                // In all of these states, the "elapsed time" is meaningless, so
                // don't show it.
                AnimationUtils.Fade.hide(mElapsedTime, View.INVISIBLE);

                // Additionally, in call states that can only occur at the start
                // of a call, reset the elapsed time to be sure we won't display
                // stale info later (like if we somehow go straight from DIALING
                // or ALERTING to DISCONNECTED, which can actually happen in
                // some failure cases like "line busy").
                if ((state ==  Call.State.DIALING) || (state == Call.State.ALERTING)) {
                    updateElapsedTimeWidget(0);
                }

                break;
        }*/
        /** }@ */
    }

    /**
     * Updates mElapsedTime based on the given {@link Call} object's information.
     *
     * @see CallTime#getCallDuration(Call)
     * @see Connection#getDurationMillis()
     */
    /* package */ void updateElapsedTimeWidget(Call call) {
        long duration = CallTime.getCallDuration(call);  // msec
        updateElapsedTimeWidget(duration / 1000);
        // Also see onTickForCallTimeElapsed(), which updates this
        // widget once per second while the call is active.
    }

    /**
     * Updates mElapsedTime based on the specified number of seconds.
     */
    public void updateElapsedTimeWidget(long timeElapsed) {
        if (DBG) log("updateElapsedTimeWidget: " + timeElapsed);
        mCallStateLabel.setText(DateUtils.formatElapsedTime(timeElapsed));
    }

    /**
     * Updates the "on hold" box in the "other call" info area
     * (ie. the stuff in the secondaryCallInfo block)
     * based on the specified Call.
     * Or, clear out the "on hold" box if the specified call
     * is null or idle.
     */
    private void displaySecondaryCallStatus(CallManager cm, Call call) {
        if (DBG) log("displaySecondaryCallStatus(call =" + call + ")...");

        if ((call == null) || (PhoneGlobals.getInstance().isOtaCallInActiveState())) {
            mSecondaryCallInfo.setVisibility(View.GONE);
            return;
        }

        Call.State state = call.getState();
        /** M: Change feature @{ */
        SIMInfo simInfo = PhoneUtils.getSimInfoByCall(call);
        switch (state) {
            case HOLDING:
                // Ok, there actually is a background call on hold.
                // Display the "on hold" box.

                // Note this case occurs only on GSM devices.  (On CDMA,
                // the "call on hold" is actually the 2nd connection of
                // that ACTIVE call; see the ACTIVE case below.)
                showSecondaryCallInfo();

                // The secondary banner is always updated after the primary,
                // so no need update Sim info again here
                //updateSimInfo();
                /*if (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                    mSecondaryCallSimIndicator.setText(R.string.incall_call_type_label_sip);
                    mSecondaryCallSimIndicator.setVisibility(View.VISIBLE);
                } else if (simInfo != null && !TextUtils.isEmpty(simInfo.mDisplayName)) {
                    mSecondaryCallSimIndicator.setText(simInfo.mDisplayName);
                    mSecondaryCallSimIndicator.setVisibility(View.VISIBLE);
                } else
                    mSecondaryCallSimIndicator.setVisibility(View.GONE);
                */
                // update background color for the secondary call banner
                if (null != mSecondaryCallBanner) {
                    updateCallBannerBackground(call, mSecondaryCallBanner);
                }
                // Ok, there actually is a background call on hold.
                // Display the "on hold" box.

                if (PhoneUtils.isConferenceCall(call)) {
                    if (DBG) log("==> conference call.");
                    mSecondaryCallName.setText(getContext().getString(R.string.confCall));
                    /*mSecondaryLabel.setVisibility(View.GONE);
                    mSecondaryPhoneNumber.setVisibility(View.GONE);*/
                    showImage(mSecondaryCallPhoto, R.drawable.picture_conference);
                } else {
                    // perform query and update the name temporarily
                    // make sure we hand the textview we want updated to the
                    // callback function.
                    if (DBG) log("==> NOT a conf call; call startGetCallerInfo...");
                    PhoneUtils.CallerInfoToken infoToken = null;
                    if (mLCforUserDataHoldCall) {
                        if (DBG) {
                            log("- displaySecondaryCallStatus: the language changed to clear userdata");
                        }
                        CallInfoCookie callInfoCookie = new CallInfoCookie(call, SECONDARY_CALL_BANNER);
                        infoToken = PhoneUtils.startGetCallerInfo(getContext(), call, this, callInfoCookie, true);
                        mLCforUserDataHoldCall = false;
                    } else {
                        infoToken = PhoneUtils.startGetCallerInfo(getContext(), call, this, mSecondaryCallName);
                    }
                    mSecondaryCallName.setText(
                            PhoneUtils.getCompactNameFromCallerInfo(infoToken.currentInfo, getContext()));

                    // Also pull the photo out of the current CallerInfo.
                    // (Note we assume we already have a valid photo at
                    // this point, since *presumably* the caller-id query
                    // was already run at some point *before* this call
                    // got put on hold.  If there's no cached photo, just
                    // fall back to the default "unknown" image.)
                    if (infoToken.isFinal) {
                        Connection conn = null;
                        int phoneType = call.getPhone().getPhoneType();
                        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                            conn = call.getLatestConnection();
                        } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                              || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                            conn = call.getEarliestConnection();
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }

                        final int presentation = conn.getNumberPresentation();
                        updateDisplayForPerson(infoToken.currentInfo, presentation, false, call, conn,
                                mSecondaryCallName, true, null, null, mSecondaryCallPhoto);
           /** }@ */
                    } else {
                        showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
                    }
                }

                AnimationUtils.Fade.show(mSecondaryCallPhotoDimEffect);
                break;

            case ACTIVE:
                /** M: Change feature: Modify for dual talk @{ */
                showSecondaryCallInfo();
                if (DualTalkUtils.isSupportDualTalk() 
                        && call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    /*if (simInfo != null && !TextUtils.isEmpty(simInfo.mDisplayName)) {
                        mSecondaryCallSimIndicator.setText(simInfo.mDisplayName);
                        mSecondaryCallSimIndicator.setVisibility(View.VISIBLE);
                    } else {
                        mSecondaryCallSimIndicator.setVisibility(View.GONE);
                    }*/
                    // update background color for the secondary call banner
                    if (null != mSecondaryCallBanner) {
                        updateCallBannerBackground(call, mSecondaryCallBanner);
                    }
                }
                /** }@ */

                // CDMA: This is because in CDMA when the user originates the second call,
                // although the Foreground call state is still ACTIVE in reality the network
                // put the first call on hold.
                if (mApplication.phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    showSecondaryCallInfo();

                    List<Connection> connections = call.getConnections();
                    if (connections.size() > 2) {
                        // This means that current Mobile Originated call is the not the first 3-Way
                        // call the user is making, which in turn tells the PhoneGlobals that we no
                        // longer know which previous caller/party had dropped out before the user
                        // made this call.
                        mSecondaryCallName.setText(
                                getContext().getString(R.string.card_title_in_call));
                        showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
                    } else {
                        // This means that the current Mobile Originated call IS the first 3-Way
                        // and hence we display the first callers/party's info here.
                        Connection conn = call.getEarliestConnection();
                        PhoneUtils.CallerInfoToken infoToken = PhoneUtils.startGetCallerInfo(
                                getContext(), conn, this, mSecondaryCallName);

                        // Get the compactName to be displayed, but then check that against
                        // the number presentation value for the call. If it's not an allowed
                        // presentation, then display the appropriate presentation string instead.
                        CallerInfo info = infoToken.currentInfo;

                        String name = PhoneUtils.getCompactNameFromCallerInfo(info, getContext());
                        boolean forceGenericPhoto = false;
                        if (info != null && info.numberPresentation !=
                                PhoneConstants.PRESENTATION_ALLOWED) {
                            name = PhoneUtils.getPresentationString(
                                    getContext(), info.numberPresentation);
                            forceGenericPhoto = true;
                        }
                        mSecondaryCallName.setText(name);

                        /** M: Change feature: Modify for dual talk @{ */
                        if (infoToken.isFinal) {
                            if (!showCachedImage(mSecondaryCallPhoto, infoToken.currentInfo)) {
                                //In some case the cached will be not used, this is strange,
                                //but it occurs, the only thing we can do is to
                                //force to reload the picture for holding call...
                                Uri personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                                        infoToken.currentInfo.person_id);
                                // Remember which person's photo is being loaded right now so that we won't issue
                                // unnecessary load request multiple times, which will mess up animation around
                                // the contact photo.
                                mLoadingPersonUri = personUri;
                                // Forget the drawable previously used.
                                mSecondaryCallPhoto.setTag(null);
                                // Show empty screen for a moment.
                                mSecondaryCallPhoto.setVisibility(View.INVISIBLE);
                                // Load the image with a callback to update the image state.
                                // When the load is finished, onImageLoadComplete() will be called.
                                ContactsAsyncHelper.startObtainPhotoAsync(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE,
                                        getContext(), personUri, this, new AsyncLoadCookie(mSecondaryCallPhoto, info, call));
                                // If the image load is too slow, we show a default avatar icon afterward.
                                // If it is fast enough, this message will be canceled on onImageLoadComplete().
                                mHandler.removeMessages(MESSAGE_SHOW_UNKNOWN_PHOTO);
                                mHandler.sendEmptyMessageDelayed(MESSAGE_SHOW_UNKNOWN_PHOTO, MESSAGE_DELAY);
                            }

                            final int presentation = conn.getNumberPresentation();
                            updateDisplayForPerson(infoToken.currentInfo, presentation, false, call, conn,
                                    mSecondaryCallName, true, null, null, mSecondaryCallPhoto);
                        }
                        /** }@ */
                        
                        // Also pull the photo out of the current CallerInfo.
                        // (Note we assume we already have a valid photo at
                        // this point, since *presumably* the caller-id query
                        // was already run at some point *before* this call
                        // got put on hold.  If there's no cached photo, just
                        // fall back to the default "unknown" image.)
                        if (!forceGenericPhoto && infoToken.isFinal) {
                            showCachedImage(mSecondaryCallPhoto, info);
                        } else {
                            showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
                        }
                    }
                } else {
                    // We shouldn't ever get here at all for non-CDMA devices.
                    Log.w(LOG_TAG, "displaySecondaryCallStatus: ACTIVE state on non-CDMA device");
                    mSecondaryCallInfo.setVisibility(View.GONE);
                }

                AnimationUtils.Fade.hide(mSecondaryCallPhotoDimEffect, View.GONE);
                break;

            default:
                // There's actually no call on hold.  (Presumably this call's
                // state is IDLE, since any other state is meaningless for the
                // background call.)
                mSecondaryCallInfo.setVisibility(View.GONE);
                break;
        }
    }

    private void showSecondaryCallInfo() {
        if (DBG) {
            log("showSecondaryCallInfo()");
        }
        // This will call ViewStub#inflate() when needed.
        mSecondaryCallInfo.setVisibility(View.VISIBLE);
        if (mSecondaryCallName == null) {
            mSecondaryCallName = (TextView) findViewById(R.id.secondaryCallName);
        }
        if (mSecondaryCallStatus == null) {
            mSecondaryCallStatus = (TextView) findViewById(R.id.secondaryCallStatus);
        }
        if (mSecondaryCallPhoto == null) {
            mSecondaryCallPhoto = (ImageView) findViewById(R.id.secondaryCallPhoto);
        }
        if (mSecondaryCallPhotoDimEffect == null) {
            mSecondaryCallPhotoDimEffect = findViewById(R.id.dim_effect_for_secondary_photo);
            mSecondaryCallPhotoDimEffect.setOnClickListener(mInCallScreen);
            // Add a custom OnTouchListener to manually shrink the "hit target".
            mSecondaryCallPhotoDimEffect.setOnTouchListener(new SmallerHitTargetTouchListener());
        }
        /** M: Change feature @{ */
        if (null == mSecondaryCallBanner) {
            mSecondaryCallBanner = (ViewGroup) findViewById(R.id.secondary_call_banner);
        }
        /** }@ */
        mInCallScreen.updateButtonStateOutsideInCallTouchUi();
    }

    /**
     * Method which is expected to be called from
     * {@link InCallScreen#updateButtonStateOutsideInCallTouchUi()}.
     */
    /* package */ void setSecondaryCallClickable(boolean clickable) {
        if (mSecondaryCallPhotoDimEffect != null) {
            mSecondaryCallPhotoDimEffect.setEnabled(clickable);
        }
    }

    private String getCallFailedString(Call call) {
        Connection c = call.getEarliestConnection();
        int resID;

        if (c == null) {
            if (DBG) log("getCallFailedString: connection is null, using default values.");
            // if this connection is null, just assume that the
            // default case occurs.
            resID = R.string.card_title_call_ended;
        } else {

            Connection.DisconnectCause cause = c.getDisconnectCause();

            // TODO: The card *title* should probably be "Call ended" in all
            // cases, but if the DisconnectCause was an error condition we should
            // probably also display the specific failure reason somewhere...

            switch (cause) {
                case BUSY:
                    resID = R.string.callFailed_userBusy;
                    break;

                case CONGESTION:
                    resID = R.string.callFailed_congestion;
                    break;

                case TIMED_OUT:
                    resID = R.string.callFailed_timedOut;
                    break;

                case SERVER_UNREACHABLE:
                    resID = R.string.callFailed_server_unreachable;
                    break;

                case NUMBER_UNREACHABLE:
                    resID = R.string.callFailed_number_unreachable;
                    break;

                case INVALID_CREDENTIALS:
                    resID = R.string.callFailed_invalid_credentials;
                    break;

                case SERVER_ERROR:
                    resID = R.string.callFailed_server_error;
                    break;

                case OUT_OF_NETWORK:
                    resID = R.string.callFailed_out_of_network;
                    break;

                case LOST_SIGNAL:
                case CDMA_DROP:
                    resID = R.string.callFailed_noSignal;
                    break;

                case LIMIT_EXCEEDED:
                    resID = R.string.callFailed_limitExceeded;
                    break;

                case POWER_OFF:
                    resID = R.string.callFailed_powerOff;
                    break;

                case ICC_ERROR:
                    ///M: #Only emergency# text replace in case IMEI locked @{
                    resID = mServiceStateExt.isImeiLocked() ?
                            R.string.callFailed_cdma_notEmergency :
                                R.string.callFailed_simError;
                    ///@}
                    break;

                case OUT_OF_SERVICE:
                    resID = R.string.callFailed_outOfService;
                    break;

                case INVALID_NUMBER:
                case UNOBTAINABLE_NUMBER:
                    resID = R.string.callFailed_unobtainable_number;
                    break;

                default:
                    resID = R.string.card_title_call_ended;
                    break;
            }
        }
        return getContext().getString(resID);
    }

    /**
     * Updates the name / photo / number / label fields on the CallCard
     * based on the specified CallerInfo.
     *
     * If the current call is a conference call, use
     * updateDisplayForConference() instead.
     */
    private void updateDisplayForPerson(CallerInfo info,
                                        int presentation,
                                        boolean isTemporary,
                                        Call call,
                                        Connection conn) {
        if (DBG) log("updateDisplayForPerson(" + info + ")\npresentation:" +
                     presentation + " isTemporary:" + isTemporary);

        // inform the state machine that we are displaying a photo.
        mPhotoTracker.setPhotoRequest(info);
        mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);

        /// M: For ALPS00504327, ALPS00516974 @{
        // remove the logic of skipping update name and number.Always update.
        //
        // MTK delete
        // boolean displayNameIsNumber = false;
        /// @}

        // The actual strings we're going to display onscreen:
        String displayName;
        String displayNumber = null;
        String label = null;
        Uri personUri = null;
        // String socialStatusText = null;
        // Drawable socialStatusBadge = null;
        /** M: Change feature @{ */
        String numberGeoDescription = null;
        boolean hasMultipleRingingCalls = false;
        boolean bFirstIncoming = true;
        /** }@ */
        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

            /** M: Change feature @{ */
            // when language changed if in emergency call, the String 
            // "Emergency number" should be updated.
            if (mLocaleChanged) {
                if (info.isEmergencyNumber()) {
                    info.phoneNumber = getContext().getString(
                            com.android.internal.R.string.emergency_call_dialog_number_for_display);
                }
                mLocaleChanged = false;
            }
            /** }@ */
            // Currently, info.phoneNumber may actually be a SIP address, and
            // if so, it might sometimes include the "sip:" prefix.  That
            // prefix isn't really useful to the user, though, so strip it off
            // if present.  (For any other URI scheme, though, leave the
            // prefix alone.)
            // TODO: It would be cleaner for CallerInfo to explicitly support
            // SIP addresses instead of overloading the "phoneNumber" field.
            // Then we could remove this hack, and instead ask the CallerInfo
            // for a "user visible" form of the SIP address.
            String number = info.phoneNumber;
            if ((number != null) && number.startsWith("sip:")) {
                number = number.substring(4);
            }

            /** M: Change feature: Format phone number @{ */
            if (number != null) {
                number = HyphonManager.getInstance().formatNumber(number);
            }
            /** }@ */
            
            if (TextUtils.isEmpty(info.name)) {
                // No valid "name" in the CallerInfo, so fall back to
                // something else.
                // (Typically, we promote the phone number up to the "name" slot
                // onscreen, and possibly display a descriptive string in the
                // "number" slot.)
                if (TextUtils.isEmpty(number)) {
                    // No name *or* number!  Display a generic "unknown" string
                    // (or potentially some other default based on the presentation.)
                    displayName = PhoneUtils.getPresentationString(getContext(), presentation);
                    if (DBG) log("  ==> no name *or* number! displayName = " + displayName);
                } else if (presentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a phone #
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = PhoneUtils.getPresentationString(getContext(), presentation);
                    if (DBG) log("  ==> presentation not allowed! displayName = " + displayName);
                } else if (!TextUtils.isEmpty(info.cnapName)) {
                    // No name, but we do have a valid CNAP name, so use that.
                    displayName = info.cnapName;
                    info.name = info.cnapName;
                    displayNumber = number;
                    if (DBG) log("  ==> cnapName available: displayName '"
                                 + displayName + "', displayNumber '" + displayNumber + "'");
                } else {
                    // No name; all we have is a number.  This is the typical
                    // case when an incoming call doesn't match any contact,
                    // or if you manually dial an outgoing number using the
                    // dialpad.

                    // Promote the phone number up to the "name" slot:
                    displayName = number;

                    /// M: For ALPS00504327, ALPS00516974 @{
                    // remove the logic of skipping update name and number.Always update.
                    //
                    // MTK delete
                    // displayNameIsNumber = true;
                    /// @}

                    // ...and use the "number" slot for a geographical description
                    // string if available (but only for incoming calls.)
                    if ((conn != null)) { // && (conn.isIncoming())) {
                        // TODO (CallerInfoAsyncQuery cleanup): Fix the CallerInfo
                        // query to only do the geoDescription lookup in the first
                        // place for incoming calls.
                        
                        /** M: Consistent UI: Phone number geodescription @{ */
                        //displayNumber = info.geoDescription; // may be null
                        if (FeatureOption.MTK_PHONE_NUMBER_GEODESCRIPTION) {
                            numberGeoDescription = info.geoDescription;
                        }
                        /** }@ */
                    }

                    if (DBG) log("  ==>  no name; falling back to number: displayName '"
                                 + displayName + "', numberGeoDescription '" + numberGeoDescription + "'");
                }
            } else {
                // We do have a valid "name" in the CallerInfo.  Display that
                // in the "name" slot, and the phone number in the "number" slot.
                if (presentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a name
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = PhoneUtils.getPresentationString(getContext(), presentation);
                    if (DBG) log("  ==> valid name, but presentation not allowed!"
                                 + " displayName = " + displayName);
                } else {
                    displayName = info.name;
                    displayNumber = number;
                    label = info.phoneLabel;
                    if (DBG) log("  ==>  name is present in CallerInfo: displayName '"
                                 + displayName + "', displayNumber '" + displayNumber + "'");
                    /** M: Change feature: Phone number geodescription @{ */
                    if (FeatureOption.MTK_PHONE_NUMBER_GEODESCRIPTION) {
                        numberGeoDescription = info.geoDescription;
                        if (DBG) {
                            log("  ==>  name is present in CallerInfo: numberGeooDescription '"
                                    + numberGeoDescription + "'");
                        }
                    }
                    /** }@ */
                }
            }
            personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, info.person_id);
            if (DBG) log("- got personUri: '" + personUri
                         + "', based on info.person_id: " + info.person_id);
        } else {
            displayName = PhoneUtils.getPresentationString(getContext(), presentation);
        }

        /// M: For ALPS00504327, ALPS00516974 @{
        // remove the logic of skipping update name and number.Always update.
        //
        // MTK delete
        /*
        boolean updateNameAndNumber = true;
        // If the new info is just a phone number, check to make sure it's not less
        // information than what's already being displayed.
        if (displayNameIsNumber) {
            // If the new number is the same as the number already displayed, ignore it
            // because that means we're also already displaying a name for it.
            // If the new number is the same as the name currently being displayed, only
            // display if the new number is longer (ie, has formatting).
            String visiblePhoneNumber = null;
            if (mPhoneNumber.getVisibility() == View.VISIBLE) {
                visiblePhoneNumber = mPhoneNumber.getText().toString();
            }
             if ((visiblePhoneNumber != null &&
                  PhoneNumberUtils.compare(visiblePhoneNumber, displayName)) ||
                 (PhoneNumberUtils.compare(mName.getText().toString(), displayName) &&
                  displayName.length() < mName.length())) {
                if (DBG) log("chose not to update display {" + mName.getText() + ", "
                             + visiblePhoneNumber + "} with number " + displayName);
                updateNameAndNumber = false;
            }
        }

        if (updateNameAndNumber) {
        */
        /// @}
            if (call.isGeneric()) {
                mName.setText(R.string.card_title_in_call);
            } else {
                mName.setText(displayName);
            }
            mName.setVisibility(View.VISIBLE);

            if (displayNumber != null && !call.isGeneric()) {
                mPhoneNumber.setText(displayNumber);
                mPhoneNumber.setVisibility(View.VISIBLE);
            } else {
                mPhoneNumber.setVisibility(View.GONE);
            }

            if (label != null && !call.isGeneric()) {
                mLabel.setText(label);
                mLabel.setVisibility(View.VISIBLE);
            } else {
                mLabel.setVisibility(View.GONE);
            }
        // }

        // Update mPhoto
        // if the temporary flag is set, we know we'll be getting another call after
        // the CallerInfo has been correctly updated.  So, we can skip the image
        // loading until then.

        // If the photoResource is filled in for the CallerInfo, (like with the
        // Emergency Number case), then we can just set the photo image without
        // requesting for an image load. Please refer to CallerInfoAsyncQuery.java
        // for cases where CallerInfo.photoResource may be set.  We can also avoid
        // the image load step if the image data is cached.
        
        /** M: Change feature: Modify for dual talk @{ */
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasMultipleRingingCall()) {
            hasMultipleRingingCalls = true;
            Call fisrtCall = mDualTalk.getFirstActiveRingingCall();
            Call secondCall = mDualTalk.getSecondActiveRingCall();
            if (call == secondCall) {
                bFirstIncoming = false;
            }
        }

        if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
            mPhoto.setTag(null);
            mPhoto.setVisibility(View.INVISIBLE);
        } else if (info != null && info.photoResource != 0) {
            if (bFirstIncoming) {
                showImage(mPhoto, info.photoResource);
            } else {
                showImage(mPhotoIncomingPre, info.photoResource);
            }
        }  else if (!showCachedImage(bFirstIncoming ? mPhoto : mPhotoIncomingPre, info)) {
            if (personUri == null) {
                Log.w(LOG_TAG, "personPri is null. Just use Unknown picture.");
                showImage(bFirstIncoming ? mPhoto : mPhotoIncomingPre, R.drawable.picture_unknown);
            } else if (personUri.equals(mLoadingPersonUri)) {
                if (DBG) {
                    log("The requested Uri (" + personUri + ") is being loaded already."
                            + " Ignoret the duplicate load request.");
                }
            } else {
                // Remember which person's photo is being loaded right now so that we won't issue
                // unnecessary load request multiple times, which will mess up animation around
                // the contact photo.
                mLoadingPersonUri = personUri;

                // Forget the drawable previously used.
                mPhoto.setTag(null);
                // Show empty screen for a moment.
                mPhoto.setVisibility(View.INVISIBLE);
                // Load the image with a callback to update the image state.
                // When the load is finished, onImageLoadComplete() will be called.
                ContactsAsyncHelper.startObtainPhotoAsync(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE,
                        getContext(), personUri, this, new AsyncLoadCookie(mPhoto, info, call));

                // If the image load is too slow, we show a default avatar icon afterward.
                // If it is fast enough, this message will be canceled on onImageLoadComplete().
                mHandler.removeMessages(MESSAGE_SHOW_UNKNOWN_PHOTO);
                mHandler.sendEmptyMessageDelayed(MESSAGE_SHOW_UNKNOWN_PHOTO, MESSAGE_DELAY);
            }
            // !!!! below logic has problem, need consider in future dual talk testing
            if (hasMultipleRingingCalls) {
                ContactsAsyncHelper.startObtainPhotoAsync(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE,
                        getContext(), personUri, this, new AsyncLoadCookie(bFirstIncoming ? mPhoto
                                : mPhotoIncomingPre, info, call));
            }
        }
        /** }@ */

        // If the phone call is on hold, show it with darker status.
        // Right now we achieve it by overlaying opaque View.
        // Note: See also layout file about why so and what is the other possibilities.
        if (call.getState() == Call.State.HOLDING) {
            AnimationUtils.Fade.show(mPhotoDimEffect);
        } else {
            AnimationUtils.Fade.hide(mPhotoDimEffect, View.GONE);
        }

        /** M: Change feature: Phone number geodescription @{ */
        if (TextUtils.isEmpty(numberGeoDescription)) {
            mPhoneNumberGeoDescription.setVisibility(View.INVISIBLE);
        } else {
            mPhoneNumberGeoDescription.setText(numberGeoDescription);
            mPhoneNumberGeoDescription.setVisibility(View.VISIBLE);
        }
        /** }@ */

        // Other text fields:
        updateCallTypeLabel(call);
        // updateSocialStatus(socialStatusText, socialStatusBadge, call);  // Currently unused
    }

    /**
     * Updates the name / photo / number / label fields
     * for the special "conference call" state.
     *
     * If the current call has only a single connection, use
     * updateDisplayForPerson() instead.
     */
    private void updateDisplayForConference(Call call) {
        if (DBG) log("updateDisplayForConference()...");
        /** M: Change feature: Add for dual talk @{ */
        boolean hasMultipleRingingCalls = false;
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasMultipleRingingCall()) {
            hasMultipleRingingCalls = true;
        }
        /** }@ */

        int phoneType = call.getPhone().getPhoneType();
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            // This state corresponds to both 3-Way merged call and
            // Call Waiting accepted call.
            // In this case we display the UI in a "generic" state, with
            // the generic "dialing" icon and no caller information,
            // because in this state in CDMA the user does not really know
            // which caller party he is talking to.
            showImage(mPhoto, R.drawable.picture_dialing);
            /** M: Change feature: Add for dual talk @{ */
            if (hasMultipleRingingCalls) {
                showImage(mPhotoIncomingPre, R.drawable.picture_dialing);
            }
            /** }@ */
            mName.setText(R.string.card_title_in_call);
        } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
            // Normal GSM (or possibly SIP?) conference call.
            // Display the "conference call" image as the contact photo.
            // TODO: Better visual treatment for contact photos in a
            // conference call (see bug 1313252).
            showImage(mPhoto, R.drawable.picture_conference);
            /** M: Change feature: Add for dual talk @{ */
            if (hasMultipleRingingCalls) {
                showImage(mPhotoIncomingPre, R.drawable.picture_conference);
            }
            /** }@ */
            mName.setText(R.string.card_title_conf_call);
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }

        mName.setVisibility(View.VISIBLE);

        // TODO: For a conference call, the "phone number" slot is specced
        // to contain a summary of who's on the call, like "Bill Foldes
        // and Hazel Nutt" or "Bill Foldes and 2 others".
        // But for now, just hide it:
        mPhoneNumber.setVisibility(View.GONE);
        mLabel.setVisibility(View.GONE);

        if (null != mPhoneNumberGeoDescription) {
            mPhoneNumberGeoDescription.setVisibility(View.GONE);
        }

        // Other text fields:
        updateCallTypeLabel(call);
        // updateSocialStatus(null, null, null);  // socialStatus is never visible in this state

        // TODO: for a GSM conference call, since we do actually know who
        // you're talking to, consider also showing names / numbers /
        // photos of some of the people on the conference here, so you can
        // see that info without having to click "Manage conference".  We
        // probably have enough space to show info for 2 people, at least.
        //
        // To do this, our caller would pass us the activeConnections
        // list, and we'd call PhoneUtils.getCallerInfo() separately for
        // each connection.
    }

    /**
     * Updates the CallCard "photo" IFF the specified Call is in a state
     * that needs a special photo (like "busy" or "dialing".)
     *
     * If the current call does not require a special image in the "photo"
     * slot onscreen, don't do anything, since presumably the photo image
     * has already been set (to the photo of the person we're talking, or
     * the generic "picture_unknown" image, or the "conference call"
     * image.)
     */
    private void updatePhotoForCallState(Call call, ImageView view) {
        if (DBG) log("updatePhotoForCallState(" + call + ")...");
        int photoImageResource = 0;

        // Check for the (relatively few) telephony states that need a
        // special image in the "photo" slot.
        Call.State state = call.getState();
        switch (state) {
            case DISCONNECTED:
                // Display the special "busy" photo for BUSY or CONGESTION.
                // Otherwise (presumably the normal "call ended" state)
                // leave the photo alone.
                // if the connection is null, we assume the default case,
                // otherwise update the image resource normally.
                /** M: Change feature: DOT NOT USE special images @{ */
                /*Connection c = call.getEarliestConnection();
                if (c != null) {
                    Connection.DisconnectCause cause = c.getDisconnectCause();
                    if ((cause == Connection.DisconnectCause.BUSY)
                        || (cause == Connection.DisconnectCause.CONGESTION)) {
                        photoImageResource = R.drawable.picture_busy;
                    }
                } else if (DBG) {
                    log("updatePhotoForCallState: connection is null, ignoring.");
                }

                // TODO: add special images for any other DisconnectCauses?
                break;
                */
                /** }@ */

            case ALERTING:
            case DIALING:
            default:
                // Leave the photo alone in all other states.
                // If this call is an individual call, and the image is currently
                // displaying a state, (rather than a photo), we'll need to update
                // the image.
                // This is for the case where we've been displaying the state and
                // now we need to restore the photo.  This can happen because we
                // only query the CallerInfo once, and limit the number of times
                // the image is loaded. (So a state image may overwrite the photo
                // and we would otherwise have no way of displaying the photo when
                // the state goes away.)

                // if the photoResource field is filled-in in the Connection's
                // caller info, then we can just use that instead of requesting
                // for a photo load.

                // look for the photoResource if it is available.
                CallerInfo ci = null;
                {
                    Connection conn = null;
                    int phoneType = call.getPhone().getPhoneType();
                    if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                        conn = call.getLatestConnection();
                    } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                            || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                        conn = call.getEarliestConnection();
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }

                    if (conn != null) {
                        Object o = conn.getUserData();
                        if (o instanceof CallerInfo) {
                            ci = (CallerInfo) o;
                        } else if (o instanceof PhoneUtils.CallerInfoToken) {
                            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        }
                    }
                }

                if (ci != null) {
                    photoImageResource = ci.photoResource;
                }

                // If no photoResource found, check to see if this is a conference call. If
                // it is not a conference call:
                //   1. Try to show the cached image
                //   2. If the image is not cached, check to see if a load request has been
                //      made already.
                //   3. If the load request has not been made [DISPLAY_DEFAULT], start the
                //      request and note that it has started by updating photo state with
                //      [DISPLAY_IMAGE].
                if (photoImageResource == 0) {
                    if (!PhoneUtils.isConferenceCall(call)) {
                        if (!showCachedImage(view, ci) && (mPhotoTracker.getPhotoState() ==
                                ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT)) {
                            Uri photoUri = mPhotoTracker.getPhotoUri();
                            if (photoUri == null) {
                                Log.w(LOG_TAG, "photoUri became null. Show default avatar icon");
                                showImage(view, R.drawable.picture_unknown);
                            } else {
                                if (DBG) {
                                    log("start asynchronous load inside updatePhotoForCallState()");
                                }
                                view.setTag(null);
                                // Make it invisible for a moment
                                view.setVisibility(View.INVISIBLE);
                                ContactsAsyncHelper.startObtainPhotoAsync(TOKEN_DO_NOTHING,
                                        getContext(), photoUri, this,
                                        new AsyncLoadCookie(view, ci, null));
                            }
                            mPhotoTracker.setPhotoState(
                                    ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                        }
                    }
                } else {
                    showImage(view, photoImageResource);
                    mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                    return;
                }
                break;
        }

        if (photoImageResource != 0) {
            if (DBG) log("- overrriding photo image: " + photoImageResource);
            showImage(view, photoImageResource);
            // Track the image state.
            mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT);
        }
    }

    /**
     * Try to display the cached image from the callerinfo object.
     *
     *  @return true if we were able to find the image in the cache, false otherwise.
     */
    private static final boolean showCachedImage(ImageView view, CallerInfo ci) {
        if ((ci != null) && ci.isCachedPhotoCurrent) {
            if (ci.cachedPhoto != null) {
                log("showCachedImage: using the cachedPhoto!");
                showImage(view, ci.cachedPhoto);
            } else {
                log("showCachedImage: using picture_unknown!");
                showImage(view, R.drawable.picture_unknown);
            }
            return true;
        }
        log("showCachedImage: return false!");
        return false;
    }

    /** Helper function to display the resource in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, int resource) {
        showImage(view, view.getContext().getResources().getDrawable(resource));
    }

    private static final void showImage(ImageView view, Bitmap bitmap) {
        showImage(view, new BitmapDrawable(view.getContext().getResources(), bitmap));
    }

    /** Helper function to display the drawable in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, Drawable drawable) {
        Resources res = view.getContext().getResources();
        Drawable current = (Drawable) view.getTag();

        if (current == null) {
            if (DBG) log("Start fade-in animation for " + view);
            view.setImageDrawable(drawable);
            AnimationUtils.Fade.show(view);
            view.setTag(drawable);
        } else {
            AnimationUtils.startCrossFade(view, current, drawable);
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Returns the special card title used in emergency callback mode (ECM),
     * which shows your own phone number.
     */
    /** M: Change feature: Move this function to PhoneUtils @{ */
    /*private String getECMCardTitle(Context context, Phone phone) {
        String rawNumber = phone.getLine1Number();  // may be null or empty
        String formattedNumber;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        } else {
            formattedNumber = context.getString(R.string.unknown);
        }
        String titleFormat = context.getString(R.string.card_title_my_phone_number);
        return String.format(titleFormat, formattedNumber);
    }*/
    /** }@ */

    /**
     * Updates the "Call type" label, based on the current foreground call.
     * This is a special label and/or branding we display for certain
     * kinds of calls.
     *
     * (So far, this is used only for SIP calls, which get an
     * "Internet call" label.  TODO: But eventually, the telephony
     * layer might allow each pluggable "provider" to specify a string
     * and/or icon to be displayed here.)
     */
    private void updateCallTypeLabel(Call call) {
        int phoneType = (call != null) ? call.getPhone().getPhoneType() : PhoneConstants.PHONE_TYPE_NONE;
        if (phoneType == PhoneConstants.PHONE_TYPE_SIP) {
            mCallTypeLabel.setVisibility(View.VISIBLE);
            mCallTypeLabel.setText(R.string.incall_call_type_label_sip);
            mCallTypeLabel.setTextColor(mTextColorCallTypeSip);
            // If desired, we could also display a "badge" next to the label, as follows:
            //   mCallTypeLabel.setCompoundDrawablesWithIntrinsicBounds(
            //           callTypeSpecificBadge, null, null, null);
            //   mCallTypeLabel.setCompoundDrawablePadding((int) (mDensity * 6));
        } else {
            mCallTypeLabel.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the "social status" label with the specified text and
     * (optional) badge.
     */
    /*private void updateSocialStatus(String socialStatusText,
                                    Drawable socialStatusBadge,
                                    Call call) {
        // The socialStatus field is *only* visible while an incoming call
        // is ringing, never in any other call state.
        if ((socialStatusText != null)
                && (call != null)
                && call.isRinging()
                && !call.isGeneric()) {
            mSocialStatus.setVisibility(View.VISIBLE);
            mSocialStatus.setText(socialStatusText);
            mSocialStatus.setCompoundDrawablesWithIntrinsicBounds(
                    socialStatusBadge, null, null, null);
            mSocialStatus.setCompoundDrawablePadding((int) (mDensity * 6));
        } else {
            mSocialStatus.setVisibility(View.GONE);
        }
    }*/

    /**
     * Hides the top-level UI elements of the call card:  The "main
     * call card" element representing the current active or ringing call,
     * and also the info areas for "ongoing" or "on hold" calls in some
     * states.
     *
     * This is intended to be used in special states where the normal
     * in-call UI is totally replaced by some other UI, like OTA mode on a
     * CDMA device.
     *
     * To bring back the regular CallCard UI, just re-run the normal
     * updateState() call sequence.
     */
    public void hideCallCardElements() {
        mPrimaryCallInfo.setVisibility(View.GONE);
        mSecondaryCallInfo.setVisibility(View.GONE);
    }

    /*
     * Updates the hint (like "Rotate to answer") that we display while
     * the user is dragging the incoming call RotarySelector widget.
     */
    /* package */ void setIncomingCallWidgetHint(int hintTextResId, int hintColorResId) {
        mIncomingCallWidgetHintTextResId = hintTextResId;
        mIncomingCallWidgetHintColorResId = hintColorResId;
    }

    // Accessibility event support.
    // Since none of the CallCard elements are focusable, we need to manually
    // fill in the AccessibilityEvent here (so that the name / number / etc will
    // get pronounced by a screen reader, for example.)
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            dispatchPopulateAccessibilityEvent(event, mName);
            dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
            return true;
        }

        dispatchPopulateAccessibilityEvent(event, mCallStateLabel);
        dispatchPopulateAccessibilityEvent(event, mPhoto);
        dispatchPopulateAccessibilityEvent(event, mName);
        dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
        dispatchPopulateAccessibilityEvent(event, mLabel);
        // dispatchPopulateAccessibilityEvent(event, mSocialStatus);
        if (mSecondaryCallName != null) {
            dispatchPopulateAccessibilityEvent(event, mSecondaryCallName);
        }
        if (mSecondaryCallPhoto != null) {
            dispatchPopulateAccessibilityEvent(event, mSecondaryCallPhoto);
        }
        return true;
    }

    private void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // if no text added write null to keep relative position
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }




    // Debugging / testing code

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    //The following lines are provided and maintained by Mediatek Inc.
    private static final int PRIMARY_CALL_BANNER = 0;
    private static final int SECONDARY_CALL_BANNER = 1;
    private static final int SECOND_HOLD_CALL_BANNER = 2;

    private class CallInfoCookie {
        CallInfoCookie(Call newCall, int bannerNo) {
            bannerNumber = bannerNo;
            call = newCall;
        }
        public Call call;
        public int bannerNumber;
    }

    // When Locale changed the boolean will be true;
    private static boolean mLocaleChanged = false;
    private static boolean mLCforUserData = false;
    private static boolean mLCforUserDataHoldCall = false;
    //ALPS00421973
    private static boolean sLCforUserDataSecondHoldCall = false;

    private int[] mSimColorMap = {
            R.drawable.incall_status_color0,
            R.drawable.incall_status_color1,
            R.drawable.incall_status_color2,
            R.drawable.incall_status_color3,
        };

    private int[] mSimBorderMap = {
            R.drawable.sim_light_blue,
            R.drawable.sim_light_orange,
            R.drawable.sim_light_green,
            R.drawable.sim_light_purple,
        };

    private ViewGroup mSecondaryCallBanner;

    // Operator name and sim info
    private TextView mOperatorName;
    private TextView mSimIndicator;
    private int mSimIndicatorPaddingLeft;
    private int mSimIndicatorPaddingRight;
    //private TextView mSecondaryCallSimIndicator;
    private SIMInfo mSimInfo;

    private ImageView mPhotoIncomingPre;
    private ImageView mPhotoHoldPre;

    //private TextView mSecondaryPhoneNumber;
    //private TextView mSecondaryLabel;

    // Info about phone number GeoDescription when contact info is displayed
    private TextView mPhoneNumberGeoDescription;

    private int mCallBannerSidePadding;
    private int mCallBannerTopBottomPadding;

    // dual talk
    DualTalkUtils mDualTalk;
    TextView m2ndIncomingName;
    TextView m2ndIncomingState;
    private ViewGroup mSecondIncomingCallBanner;
    TextView m2ndHoldName;
    TextView m2ndHoldState;
    private ViewStub mDualTalkSecondaryCallInfo;
    private ViewGroup mIncomingCall2Info;
    private View mDualTalkSecondaryCallPhotoDimEffect;
    private View mIncomingCall2PhotoDimEffect;
    private ViewGroup mSecondHoldCallBanner;
    private boolean mNeedShowIncomingCall2Animator;
    private boolean mShowSwtchCall2Animator;
    private int mShowAnimator2End = 1;
    private int mIncomingCallInfoWidth;
    private int mIncomingCallInfoHeight;
    private int mIncomingCallInfoTopMargin;

    // When language changed should call this function.
    public void updateForLanguageChange() {
        // Update String "Press Menu for call options".
        // mMenuButtonHint.setText(R.string.menuButtonHint);
        mLocaleChanged = true;
        mLCforUserData = true;
        mLCforUserDataHoldCall = true;
        sLCforUserDataSecondHoldCall = true;
    }

    private void updateSimInfo(Call call) {
        if (GeminiUtils.isGeminiSupport()) {
            GeminiPhone phone = (GeminiPhone) PhoneGlobals.getInstance().phone;
            int slot = GeminiUtils.getSlotNotIdle(phone);

            if (slot == -1) {
                int[] geminiSlots = GeminiUtils.getSlots();
                for (int gs: geminiSlots) {
                    if (phone.getPendingMmiCodesGemini(gs).size() != 0) {
                        slot = gs;
                        break;
                    }
                }
                mSimInfo = null;
                //for call end display
                if (slot == -1 && (call != null)) {
                    mSimInfo = PhoneUtils.getSimInfoByCall(call);
                }
                
                if (DBG) {
                    log("updateSimIndicator, running mmi, slot = " + slot);
                }
            } else {
                mSimInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slot);
                if (mSimInfo != null) {
                    if (DBG) {
                        log("updateSimIndicator slot = " + slot + " mSimInfo :");
                        log("displayName = " + mSimInfo.mDisplayName);
                        log("color       = "  + mSimInfo.mColor);
                    }
                    if (mSimInfo.mDisplayName == null) {
                        mSimInfo = SIMInfo.getSIMInfoBySlot(mInCallScreen,slot);
                        if (DBG) {
                            log("displayName = " + mSimInfo.mDisplayName);
                        }
                    }
                }
            }
        }
    }
    
    private void updateCallBannerBackground(Call call, ViewGroup callBanner) {
        SIMInfo simInfo = PhoneUtils.getSimInfoByCall(call);
        if (GeminiUtils.isGeminiSupport()) {
            final Phone phone = call.getPhone();
            final int phoneType = phone.getPhoneType();
            if (DBG) {
                log("Phone type is " + phoneType);
            }

            if (PhoneConstants.PHONE_TYPE_SIP == phoneType) {
                //callBanner.setBackgroundResource(R.drawable.incall_status_color8);
                if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
                    mSimIndicator.setBackgroundResource(R.drawable.sim_light_internet_call);
                }
                if (null != mSecondaryInfoContainer && mSecondaryInfoContainer.getVisibility() == View.VISIBLE) {
                    mSecondaryInfoContainer.setBackgroundResource(R.drawable.incall_status_color8);
                }
                if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
                    mSecondaryCallBanner.setBackgroundResource(R.drawable.incall_status_color8);
                }
            } else {
                if (null == simInfo || null == mSimColorMap || simInfo.mColor < 0
                        || simInfo.mColor >= mSimColorMap.length) {
                    if (DBG) {
                        log("mSimInfo is null or mSimInfo.mColor invalid, do not update background");
                    }
                    return;
                }
                //callBanner.setBackgroundResource(mSimColorMap[simInfo.mColor]);
                if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
                    mSimIndicator.setBackgroundResource(mSimBorderMap[simInfo.mColor]);
                }
                if (null != mSecondaryInfoContainer && mSecondaryInfoContainer.getVisibility() == View.VISIBLE) {
                    mSecondaryInfoContainer.setBackgroundResource(mSimColorMap[simInfo.mColor]);
                }
                if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
                    mSecondaryCallBanner.setBackgroundResource(mSimColorMap[simInfo.mColor]);
                }
            }
        } else {
            if (null == simInfo || null == mSimColorMap || simInfo.mColor < 0
                    || simInfo.mColor >= mSimColorMap.length) {
                if (DBG) {
                    log("mSimInfo is null or mSimInfo.mColor invalid, set them to default value");
                }
                final Phone phone = call.getPhone();
                final int phoneType = phone.getPhoneType();
                if (PhoneConstants.PHONE_TYPE_SIP == phoneType) {
                    //callBanner.setBackgroundResource(R.drawable.incall_status_color8);
                    if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
                        mSimIndicator.setBackgroundResource(R.drawable.sim_light_internet_call);
                    }
                    if (null != mSecondaryInfoContainer && mSecondaryInfoContainer.getVisibility() == View.VISIBLE) {
                        mSecondaryInfoContainer.setBackgroundResource(R.drawable.incall_status_color8);
                    }
                    if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
                        mSecondaryCallBanner.setBackgroundResource(R.drawable.incall_status_color8);
                    }
                }
                return;
            }
            //callBanner.setBackgroundResource(R.drawable.incall_status_color3);
            if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
                mSimIndicator.setBackgroundResource(mSimBorderMap[simInfo.mColor]);
            }
            if (null != mSecondaryInfoContainer && mSecondaryInfoContainer.getVisibility() == View.VISIBLE) {
                mSecondaryInfoContainer.setBackgroundResource(mSimColorMap[simInfo.mColor]);
            }
            if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
                mSecondaryCallBanner.setBackgroundResource(mSimColorMap[simInfo.mColor]);
            }
        }
        if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
            mSecondaryCallBanner.setPadding(mCallBannerSidePadding, 0, mCallBannerSidePadding, 0);
        }
        if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
            mSimIndicator.setPadding(mSimIndicatorPaddingLeft, 0, mSimIndicatorPaddingRight, 0);
        }

        // callBanner.setPadding(mCallBannerSidePadding,
        // mCallBannerTopBottomPadding, mCallBannerSidePadding,
        // mCallBannerTopBottomPadding);
    }

    private void updateDisplayForPerson(CallerInfo info, int presentation, boolean isTemporary,
            Call call, Connection conn, TextView nameView, boolean isOnHold, TextView numberView,
            TextView labelView, ImageView photoView) {
        if (DBG) {
            log("updateDisplayForPerson(), info: " + info + " presentation:" + presentation
                    + " isTemporary: " + isTemporary + " call: " + call + " conn: " + conn + " nameView: "
                    + nameView + " isOnHold: +" + isOnHold + " numberView: " + numberView + " labelView: "
                    + labelView + "photoView: " + photoView);
        }

        // inform the state machine that we are displaying a photo.
        // for background call, it's not necessary to update PhotoTracker
        if (!isOnHold) {
            mPhotoTracker.setPhotoRequest(info);
            mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
        }

        // The actual strings we're going to display onscreen:
        String displayName;
        String displayNumber = null;
        String label = null;
        Uri personUri = null;
        String socialStatusText = null;
        Drawable socialStatusBadge = null;

        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

            // when language changed if in emergency call, the String 
            // "Emergency number" should be updated.
            if (mLocaleChanged) {
                if (info.isEmergencyNumber()) {
                    info.phoneNumber = getContext().getString(
                            com.android.internal.R.string.emergency_call_dialog_number_for_display);
                }
                mLocaleChanged = false;
            }

            // Currently, info.phoneNumber may actually be a SIP address, and
            // if so, it might sometimes include the "sip:" prefix. That
            // prefix isn't really useful to the user, though, so strip it off
            // if present. (For any other URI scheme, though, leave the
            // prefix alone.)
            // TODO: It would be cleaner for CallerInfo to explicitly support
            // SIP addresses instead of overloading the "phoneNumber" field.
            // Then we could remove this hack, and instead ask the CallerInfo
            // for a "user visible" form of the SIP address.
            String number = info.phoneNumber;
            if ((number != null) && number.startsWith("sip:")) {
                number = number.substring(4);
            }
            number = HyphonManager.getInstance().formatNumber(number);

            if (TextUtils.isEmpty(info.name)) {
                // No valid "name" in the CallerInfo, so fall back to
                // something else.
                // (Typically, we promote the phone number up to the "name" slot
                // onscreen, and possibly display a descriptive string in the
                // "number" slot.)
                if (TextUtils.isEmpty(number)) {
                    // No name *or* number! Display a generic "unknown" string
                    // (or potentially some other default based on the
                    // presentation.)
                    displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
                    if (DBG) {
                        log("  ==> no name *or* number! displayName = " + displayName);
                    }
                } else if (presentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should
                    // never send a phone #
                    // AND a restricted presentation. However we leave it here
                    // in case of weird
                    // network behavior
                    displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
                    if (DBG) {
                        log("  ==> presentation not allowed! displayName = " + displayName);
                    }
                } else if (!TextUtils.isEmpty(info.cnapName)) {
                    // No name, but we do have a valid CNAP name, so use that.
                    displayName = info.cnapName;
                    info.name = info.cnapName;
                    displayNumber = number;
                    if (DBG) {
                        log("  ==> cnapName available: displayName '" + displayName
                                + "', displayNumber '" + displayNumber + "'");
                    }
                } else {
                    // No name; all we have is a number. This is the typical
                    // case when an incoming call doesn't match any contact,
                    // or if you manually dial an outgoing number using the
                    // dialpad.

                    // Promote the phone number up to the "name" slot:
                    displayName = number;

                    // ...and use the "number" slot for a geographical
                    // description
                    // string if available (but only for incoming calls.)
                    if ((conn != null) && (conn.isIncoming()) && (call.getState() != Call.State.HOLDING)) {
                        // TODO (CallerInfoAsyncQuery cleanup): Fix the
                        // CallerInfo
                        // query to only do the geoDescription lookup in the
                        // first
                        // place for incoming calls.
                        /** M: Consistent UI: Phone number geodescription @{ */
                        //displayNumber = info.geoDescription; // may be null
                        if (TextUtils.isEmpty(info.geoDescription)) {
                            mPhoneNumberGeoDescription.setVisibility(View.INVISIBLE);
                        } else {
                            mPhoneNumberGeoDescription.setText(info.geoDescription);
                            mPhoneNumberGeoDescription.setVisibility(View.VISIBLE);
                        }
                        /** }@ */
                    }

                    if (DBG) {
                        log("  ==>  no name; falling back to number: displayName '" + displayName
                                + "', displayNumber '" + displayNumber + "'");
                    }
                }
            } else {
                // We do have a valid "name" in the CallerInfo. Display that
                // in the "name" slot, and the phone number in the "number"
                // slot.
                if (presentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should
                    // never send a name
                    // AND a restricted presentation. However we leave it here
                    // in case of weird
                    // network behavior
                    displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
                    if (DBG) {
                        log("  ==> valid name, but presentation not allowed!" + " displayName = "
                                + displayName);
                    }
                } else {
                    displayName = info.name;
                    displayNumber = number;
                    label = info.phoneLabel;
                    if (DBG) {
                        log("  ==>  name is present in CallerInfo: displayName '" + displayName
                                + "', displayNumber '" + displayNumber + "'");
                    }
                }
            }
            personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, info.person_id);
            if (DBG) {
                log("- got personUri: '" + personUri + "', based on info.person_id: "
                        + info.person_id);
            }
        } else {
            displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
        }

        if (call.isGeneric() && nameView != null) {
            nameView.setText(R.string.card_title_in_call);
        } else if (nameView != null) {
            nameView.setText(displayName);
        }
        if (nameView != null) {
            nameView.setVisibility(View.VISIBLE);
        }

        // Update mPhoto
        // if the temporary flag is set, we know we'll be getting another call
        // after
        // the CallerInfo has been correctly updated. So, we can skip the image
        // loading until then.

        // If the photoResource is filled in for the CallerInfo, (like with the
        // Emergency Number case), then we can just set the photo image without
        // requesting for an image load. Please refer to
        // CallerInfoAsyncQuery.java
        // for cases where CallerInfo.photoResource may be set. We can also
        // avoid
        // the image load step if the image data is cached.
        if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
            photoView.setTag(null);
            photoView.setVisibility(View.INVISIBLE);
        } else if (info != null && info.photoResource != 0) {
            showImage(photoView, info.photoResource);
        } else if (!showCachedImage(photoView, info)) {
            // Remember which person's photo is being loaded right now so that we won't issue
            // unnecessary load request multiple times, which will mess up animation around
            // the contact photo.
            mLoadingPersonUri = personUri;
            // Forget the drawable previously used.
            photoView.setTag(null);
            // Show empty screen for a moment.
            photoView.setVisibility(View.INVISIBLE);
            
            if (personUri == null) {
                Log.w(LOG_TAG, "personPri is null. Just use Unknown picture.");
                showImage(photoView, R.drawable.picture_unknown);
            } else {
                // Load the image with a callback to update the image state.
                // When the load is finished, onImageLoadComplete() will be called.
                ContactsAsyncHelper.startObtainPhotoAsync(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE,
                        getContext(), personUri, this, new AsyncLoadCookie(photoView, info, call));
    
                // If the image load is too slow, we show a default avatar icon afterward.
                // If it is fast enough, this message will be canceled on onImageLoadComplete().
                mHandler.removeMessages(MESSAGE_SHOW_UNKNOWN_PHOTO);
                mHandler.sendEmptyMessageDelayed(MESSAGE_SHOW_UNKNOWN_PHOTO, MESSAGE_DELAY);
            }
        }

        if (numberView != null && displayNumber != null && !call.isGeneric()) {
            numberView.setText(displayNumber);
            numberView.setVisibility(View.VISIBLE);
        } else if (numberView != null) {
            numberView.setVisibility(View.GONE);
        }

        if (labelView != null && label != null && !call.isGeneric()) {
            labelView.setText(label);
            labelView.setVisibility(View.VISIBLE);
        } else if (labelView != null) {
            labelView.setVisibility(View.GONE);
        }
    }
    
    /**
     * This is ugly and boring for ALPS00111659 (dial out an long invalid number)
     */
    void displayFakeCallStatus(InCallUiState.FakeCall call) {
        if (call == null) {
            // There's no call to display, presumably because the phone is idle.
            mPrimaryCallInfo.setVisibility(View.GONE);
            return;
        }
        
        if (DBG) {
            log("displayFakeCallStatus...");
        }
        
        PhoneUtils.CallerInfoToken info =
                PhoneUtils.startGetCallerInfo(getContext(), call.conn, this, call);
        
        mPrimaryCallInfo.setVisibility(View.VISIBLE);
        
        //display call widget
        mCallStateLabel.setVisibility(View.VISIBLE);
        mCallStateLabel.setText(getCallFailedString(call.cause));
        
        if (GeminiUtils.isGeminiSupport()) {
            mSimInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(call.slotId);
        } else {
            mSimInfo = null;
        }
        
        if (mSimInfo != null) {
            mSimIndicator.setText(mSimInfo.mDisplayName);
            mSimIndicator.setVisibility(View.VISIBLE);
        }
        
        updateCallBannerBackground(call, mPrimaryCallBanner);
        
        if (call.phoneType == PhoneConstants.PHONE_TYPE_SIP) {
            //For sip call, always display "internet call"
            mOperatorName.setText(R.string.incall_call_type_label_sip);
        } else {
            /// M:Gemini+
            final String operatorName = GeminiUtils.getOperatorName(call.slotId);
            mOperatorName.setText(operatorName);
            mOperatorName.setVisibility(View.VISIBLE);
        }
        
        //Refresh the phone number & label text (ALPS00249779)
        if (mPhoneNumber.getVisibility() == View.VISIBLE) {
            mPhoneNumber.setText("");
        }
        if (mLabel.getVisibility() == View.VISIBLE) {
            mLabel.setText("");
        }
        
        if (mSimInfo != null && !TextUtils.isEmpty(mSimInfo.mDisplayName)
                && (call.phoneType != PhoneConstants.PHONE_TYPE_SIP)) {
            mSimIndicator.setText(mSimInfo.mDisplayName);
            mSimIndicator.setVisibility(View.VISIBLE);
        } else {
            mSimIndicator.setVisibility(View.GONE);
        }
        if (info != null && info.isFinal) {
            if (info.currentInfo != null && !TextUtils.isEmpty(info.currentInfo.name)) {
                mName.setText(info.currentInfo.name);
            } else {
                mName.setText(call.number);
            }
        } else {
            mName.setText(call.number);
        }
        showImage(mPhoto, R.drawable.picture_unknown);
        displaySecondaryCallStatus(mApplication.mCM, null);
        displaySecondHoldCallStatus(null); 
    }

    private String getCallFailedString(Connection.DisconnectCause cause) {
        int resID;

        if (cause == null) {
            if (DBG) {
                log("getCallFailedString: connection is null, using default values.");
            }
            // if this connection is null, just assume that the
            // default case occurs.
            resID = R.string.card_title_call_ended;
        } else {
            switch (cause) {
                case BUSY:
                    resID = R.string.callFailed_userBusy;
                    break;

                case CONGESTION:
                    resID = R.string.callFailed_congestion;
                    break;

                case TIMED_OUT:
                    resID = R.string.callFailed_timedOut;
                    break;

                case SERVER_UNREACHABLE:
                    resID = R.string.callFailed_server_unreachable;
                    break;

                case NUMBER_UNREACHABLE:
                    resID = R.string.callFailed_number_unreachable;
                    break;

                case INVALID_CREDENTIALS:
                    resID = R.string.callFailed_invalid_credentials;
                    break;

                case SERVER_ERROR:
                    resID = R.string.callFailed_server_error;
                    break;

                case OUT_OF_NETWORK:
                    resID = R.string.callFailed_out_of_network;
                    break;

                case LOST_SIGNAL:
                case CDMA_DROP:
                    resID = R.string.callFailed_noSignal;
                    break;

                case LIMIT_EXCEEDED:
                    resID = R.string.callFailed_limitExceeded;
                    break;

                case POWER_OFF:
                    resID = R.string.callFailed_powerOff;
                    break;

                case ICC_ERROR:
                    ///M: #Only emergency# text replace in case IMEI locked @{
                    resID = mServiceStateExt.isImeiLocked() ?
                            R.string.callFailed_cdma_notEmergency :
                                R.string.callFailed_simError;
                    ///@}
                    break;

                case OUT_OF_SERVICE:
                    resID = R.string.callFailed_outOfService;
                    break;

                case INVALID_NUMBER:
                case UNOBTAINABLE_NUMBER:
                    resID = R.string.callFailed_unobtainable_number;
                    break;

                default:
                    resID = R.string.card_title_call_ended;
                    break;
            }
        }
        return getContext().getString(resID);
    }
    
    /**
     * This is ugly and boring for ALPS00111659 (dial out an long invalid number)
     */
    private void updateCallBannerBackground(InCallUiState.FakeCall call, ViewGroup callBanner) {
        if (DBG) {
            log("displayFakeCallStatus...");
        }
        if (GeminiUtils.isGeminiSupport()) {
            final int phoneType = call.phoneType;
            if (DBG) {
                log("Phone type is " + phoneType);
            }
            if (PhoneConstants.PHONE_TYPE_SIP == phoneType) {
                //callBanner.setBackgroundResource(R.drawable.incall_status_color8);
                if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
                    mSimIndicator.setBackgroundResource(R.drawable.sim_light_internet_call);
                }
                if (null != mSecondaryInfoContainer && mSecondaryInfoContainer.getVisibility() == View.VISIBLE) {
                    mSecondaryInfoContainer.setBackgroundResource(R.drawable.incall_status_color8);
                }
                if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
                    mSecondaryCallBanner.setBackgroundResource(R.drawable.incall_status_color8);
                }
            } else {
                if (null == mSimInfo || null == mSimColorMap || mSimInfo.mColor < 0
                        || mSimInfo.mColor >= mSimColorMap.length) {
                    if (DBG) {
                        log("mSimInfo is null or mSimInfo.mColor invalid, do not update background");
                    }
                    return;
                }
                //callBanner.setBackgroundResource(mSimColorMap[mSimInfo.mColor]);
                if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
                    mSimIndicator.setBackgroundResource(mSimBorderMap[mSimInfo.mColor]);
                }
                if (null != mSecondaryInfoContainer && mSecondaryInfoContainer.getVisibility() == View.VISIBLE) {
                    mSecondaryInfoContainer.setBackgroundResource(mSimColorMap[mSimInfo.mColor]);
                }
                if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
                    mSecondaryCallBanner.setBackgroundResource(mSimColorMap[mSimInfo.mColor]);
                }
            }
        } else {
            //callBanner.setBackgroundResource(R.drawable.incall_status_color3);
            if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
                mSimIndicator.setBackgroundResource(R.drawable.sim_light_purple);
            }
            if (null != mSecondaryInfoContainer && mSecondaryInfoContainer.getVisibility() == View.VISIBLE) {
                mSecondaryInfoContainer.setBackgroundResource(R.drawable.incall_status_color3);
            }
            if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
                mSecondaryCallBanner.setBackgroundResource(R.drawable.incall_status_color3);
            }
        }
        if (null != mSecondaryCallBanner && mSecondaryCallBanner.getVisibility() == View.VISIBLE) {
            mSecondaryCallBanner.setPadding(mCallBannerSidePadding, 0, mCallBannerSidePadding, 0);
        }
        if (null != mSimIndicator && mSimIndicator.getVisibility() == View.VISIBLE) {
            mSimIndicator.setPadding(mSimIndicatorPaddingLeft, 0, mSimIndicatorPaddingRight, 0);
        }

        // callBanner.setPadding(mCallBannerSidePadding,
        // mCallBannerTopBottomPadding, mCallBannerSidePadding,
        // mCallBannerTopBottomPadding);
    }

    String getOperatorNameByCall(Call call) {
        if (call == null) {
            return null;
        }

        if (GeminiUtils.isGeminiSupport()) {
            SIMInfo info = PhoneUtils.getSimInfoByCall(call);
            if (info != null && !TextUtils.isEmpty(info.mDisplayName)
                        && (call.getPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_SIP)) {
                return info.mDisplayName;
            } else if (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                return getContext().getString(R.string.incall_call_type_label_sip);
            }
        } else {
            return SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        }
        
        return null;
    }

    int getOperatorColorByCall(Call call) {
        if (call == null) {
            return -1;
        }

        if (GeminiUtils.isGeminiSupport()) {
            SIMInfo info = PhoneUtils.getSimInfoByCall(call);
            if (info != null && !TextUtils.isEmpty(info.mDisplayName)
                        && (call.getPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_SIP)) {
                return info.mColor;
            } 
        }

        return -1;
    }

    void displaySecondIncomingCallStatus(Call call) {
        if (DBG) {
            log("displaySecondIncomingCallStatus(call =" + call + ")...");
        }
        if (call == null) {
            if (null != m2ndIncomingName) {
                m2ndIncomingName.setVisibility(View.GONE);
            }
            if (null != m2ndIncomingState) {
                m2ndIncomingState.setVisibility(View.GONE);
            }
            if (null != mPhotoIncomingPre) {
                mPhotoIncomingPre.setVisibility(View.GONE);
            }
            if (null != mIncomingCall2PhotoDimEffect) {
                AnimationUtils.Fade.hide(mIncomingCall2PhotoDimEffect, View.GONE);
            }
            if (null != mSecondIncomingCallBanner) {
                mSecondIncomingCallBanner.setVisibility(View.GONE);
            }
            if (null != mIncomingCall2Info) {
                mIncomingCall2Info.setVisibility(View.GONE);
                mNeedShowIncomingCall2Animator = false;
            }
            return ;
        }
        
        if (DBG) {
            log("displaySecondIncomingCallStatus ==> " + call.getConnections());
        }

        if (null != mIncomingCall2Info && View.GONE == mIncomingCall2Info.getVisibility()) {
            mNeedShowIncomingCall2Animator = true;
        }
        if (mNeedShowIncomingCall2Animator) {
            displaySecondaryIncomingAnimator();
        } 

        if (null != mIncomingCall2Info && null != mIncomingCall2PhotoDimEffect) {
            mIncomingCall2Info.setVisibility(View.VISIBLE);
            AnimationUtils.Fade.show(mIncomingCall2PhotoDimEffect);
        }

        if (null != mPhotoIncomingPre) {
            mPhotoIncomingPre.setVisibility(View.VISIBLE); 
        }

        if (PhoneUtils.isConferenceCall(call)) {
            // Update onscreen info for a conference call.
            updateDisplayForConference(call);
        } else {
            // Update onscreen info for a regular call (which presumably
            // has only one connection.)
            Connection conn = null;
            int phoneType = call.getPhone().getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                conn = call.getLatestConnection();
            } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                  || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                conn = call.getEarliestConnection();
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }

            if (conn == null) {
                if (DBG) {
                    log("displaySecondIncomingCallStatus: connection is null, using default values.");
                }
                // if the connection is null, we run through the behaviour
                // we had in the past, which breaks down into trivial steps
                // with the current implementation of getCallerInfo and
                // updateDisplayForPerson.
                CallerInfo info = PhoneUtils.getCallerInfo(getContext(), null /* conn */);
                updateDisplayForPerson(info, PhoneConstants.PRESENTATION_ALLOWED, false, call, conn);
            } else {
                if (DBG) {
                    log("  - CONN: " + conn + ", state = " + conn.getState());
                }
                int presentation = conn.getNumberPresentation();

                // make sure that we only make a new query when the current
                // callerinfo differs from what we've been requested to display.
                boolean runQuery = true;
                Object o = conn.getUserData();
                if (o instanceof PhoneUtils.CallerInfoToken) {
                    runQuery = mPhotoTracker.isDifferentImageRequest(
                            ((PhoneUtils.CallerInfoToken) o).currentInfo);
                } else {
                    runQuery = mPhotoTracker.isDifferentImageRequest(conn);
                }

                // Adding a check to see if the update was caused due to a Phone number update
                // or CNAP update. If so then we need to start a new query
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    Object obj = conn.getUserData();
                    String updatedNumber = conn.getAddress();
                    String updatedCnapName = conn.getCnapName();
                    CallerInfo info = null;
                    if (obj instanceof PhoneUtils.CallerInfoToken) {
                        info = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                    } else if (o instanceof CallerInfo) {
                        info = (CallerInfo) o;
                    }

                    if (info != null) {
                        if (updatedNumber != null && !updatedNumber.equals(info.phoneNumber)) {
                            if (DBG) {
                                log("- displayMainCallStatus: updatedNumber = " + updatedNumber);
                            }
                            runQuery = true;
                        }
                        if (updatedCnapName != null && !updatedCnapName.equals(info.cnapName)) {
                            if (DBG) {
                                log("- displayMainCallStatus: updatedCnapName = "
                                    + updatedCnapName);
                            }
                            runQuery = true;
                        }
                    }
                }

                if (runQuery) {
                    if (DBG) {
                        log("- displaySecondIncomingCallStatus: starting CallerInfo query...");
                    }
                    if (mLCforUserData) {
                        if (DBG) {
                            log("- displaySecondIncomingCallStatus: the language changed to clear userdata");
                        }
                        conn.clearUserData();
                        mLCforUserData = false;
                    }

                    CallInfoCookie callInfoCookie = new CallInfoCookie(call, PRIMARY_CALL_BANNER);
                    PhoneUtils.CallerInfoToken info =
                            PhoneUtils.startGetCallerInfo(getContext(), conn, this, callInfoCookie);
                    
                    
                    m2ndIncomingName.setText(getCallerName(info.currentInfo, presentation, !info.isFinal, call, conn));
                    /*updateDisplayForPerson(info.currentInfo, presentation, !info.isFinal,
                        call, conn);*/
                    updateDisplayForPerson(info.currentInfo, presentation, false, call, conn, null,
                            false, null, null, mPhotoIncomingPre);
                } else {
                    // No need to fire off a new query.  We do still need
                    // to update the display, though (since we might have
                    // previously been in the "conference call" state.)
                    if (DBG) {
                        log("- displaySecondIncomingCallStatus: using data we already have...");
                    }
                    if (o instanceof CallerInfo) {
                        CallerInfo ci = (CallerInfo) o;
                        // Update CNAP information if Phone state change occurred
                        ci.cnapName = conn.getCnapName();
                        ci.numberPresentation = conn.getNumberPresentation();
                        ci.namePresentation = conn.getCnapNamePresentation();
                        if (DBG) {
                            log("- displaySecondIncomingCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                            log("   ==> Got CallerInfo; updating display: ci = " + ci);
                        }
                        //updateDisplayForPerson(ci, presentation, false, call, conn);
                        updateDisplayForPerson(ci, presentation, false, call, conn, null, false,
                                null, null, mPhotoIncomingPre);
                        m2ndIncomingName.setText(getCallerName(ci, presentation, false, call, conn));
                    } else if (o instanceof PhoneUtils.CallerInfoToken) {
                        CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        if (DBG) {
                            log("- displaySecondIncomingCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                            log("   ==> Got CallerInfoToken; updating display: ci = " + ci);
                        }
                        //updateDisplayForPerson(ci, presentation, true, call, conn);
                        updateDisplayForPerson(ci, presentation, false, call, conn, null, false,
                                null, null, mPhotoIncomingPre);
                        m2ndIncomingName.setText(getCallerName(ci, presentation, true, call, conn));
                    } else {
                        Log.w(LOG_TAG, "displaySecondIncomingCallStatus: runQuery was false, "
                              + "but we didn't have a cached CallerInfo object!  o = " + o);
                        // TODO: any easy way to recover here (given that
                        // the CallCard is probably displaying stale info
                        // right now?)  Maybe force the CallCard into the
                        // "Unknown" state?
                    }
                }
            }
        }
        SIMInfo info = PhoneUtils.getSimInfoByCall(call);
        if (info != null && (info.mColor >= 0 && info.mColor < mSimColorMap.length)) {
            //m2ndIncomingName.setBackgroundResource(mSimColorMap[info.mColor]);
            //m2ndIncomingState.setBackgroundResource(mSimColorMap[info.mColor]);
            if (null != mSecondIncomingCallBanner) {
                mSecondIncomingCallBanner.setBackgroundResource(mSimColorMap[info.mColor]);
            }
        } else if (info == null && (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP)) {
            //m2ndIncomingName.setBackgroundResource(R.drawable.incall_status_color8);
            //m2ndIncomingState.setBackgroundResource(R.drawable.incall_status_color8);
            if (null != mSecondIncomingCallBanner) {
                mSecondIncomingCallBanner.setBackgroundResource(R.drawable.incall_status_color8);
            }
        }
        //m2ndIncomingState.getBackground().setAlpha(125);
        if (null != mSecondIncomingCallBanner) {
            mSecondIncomingCallBanner.setVisibility(View.VISIBLE);
        }
        
        m2ndIncomingName.setVisibility(View.VISIBLE);
        m2ndIncomingState.setVisibility(View.VISIBLE);
        //mPhotoIncomingPre.setVisibility(View.VISIBLE);
        String callState = "";
        if (VTCallUtils.isVideoCall(call)) {
            callState = getContext().getString(R.string.card_title_incoming_vt_call);
        } else {
            callState = getContext().getString(R.string.card_title_incoming_call);
        }
        m2ndIncomingState.setText(callState);
    }
    
    String getCallerName(CallerInfo info,
            int presentation,
            boolean isTemporary,
            Call call,
            Connection conn) {
        String displayName = "";
        
        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

            // when language changed if in emergency call, the String 
            // "Emergency number" should be updated.
            if (mLocaleChanged) {
                if (info.isEmergencyNumber()) {
                    info.phoneNumber = getContext().getString(
                            com.android.internal.R.string.emergency_call_dialog_number_for_display);
                }
                mLocaleChanged = false;
            }
            // Currently, info.phoneNumber may actually be a SIP address, and
            // if so, it might sometimes include the "sip:" prefix.  That
            // prefix isn't really useful to the user, though, so strip it off
            // if present.  (For any other URI scheme, though, leave the
            // prefix alone.)
            // TODO: It would be cleaner for CallerInfo to explicitly support
            // SIP addresses instead of overloading the "phoneNumber" field.
            // Then we could remove this hack, and instead ask the CallerInfo
            // for a "user visible" form of the SIP address.
            String number = info.phoneNumber;
            if ((number != null) && number.startsWith("sip:")) {
                number = number.substring(4);
            }
            
            if (number != null) {
                number = HyphonManager.getInstance().formatNumber(number);
            }
            
            if (TextUtils.isEmpty(info.name)) {
                // No valid "name" in the CallerInfo, so fall back to
                // something else.
                // (Typically, we promote the phone number up to the "name" slot
                // onscreen, and possibly display a descriptive string in the
                // "number" slot.)
                if (TextUtils.isEmpty(number)) {
                    // No name *or* number!  Display a generic "unknown" string
                    // (or potentially some other default based on the presentation.)
                    displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
                    if (DBG) {
                        log("  ==> no name *or* number! displayName = " + displayName);
                    }
                } else if (presentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a phone #
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
                    if (DBG) {
                        log("  ==> presentation not allowed! displayName = " + displayName);
                    }
                } else if (!TextUtils.isEmpty(info.cnapName)) {
                    // No name, but we do have a valid CNAP name, so use that.
                    displayName = info.cnapName;
                    info.name = info.cnapName;
                    if (DBG) {
                        log("  ==> cnapName available: displayName '"
                                 + displayName + "'");
                    }
                } else {
                    // No name; all we have is a number.  This is the typical
                    // case when an incoming call doesn't match any contact,
                    // or if you manually dial an outgoing number using the
                    // dialpad.

                    // Promote the phone number up to the "name" slot:
                    displayName = number;

                    // ...and use the "number" slot for a geographical description
                    // string if available (but only for incoming calls.)
                    if ((conn != null) && (conn.isIncoming())) {
                        // TODO (CallerInfoAsyncQuery cleanup): Fix the CallerInfo
                        // query to only do the geoDescription lookup in the first
                        // place for incoming calls.
                        //displayNumber = info.geoDescription;  // may be null
                        if (DBG) {
                            log(" (conn != null) && (conn.isIncoming())");
                        }
                    }

//                    if (DBG) log("  ==>  no name; falling back to number: displayName '"
//                                 + displayName + "', displayNumber '" + displayNumber + "'");
                }
            } else {
                // We do have a valid "name" in the CallerInfo.  Display that
                // in the "name" slot, and the phone number in the "number" slot.
                if (presentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a name
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
                    if (DBG) {
                        log("  ==> valid name, but presentation not allowed!"
                                 + " displayName = " + displayName);
                    }
                } else {
                    displayName = info.name;
                    //displayNumber = number;
                    //label = info.phoneLabel;
//                    if (DBG) log("  ==>  name is present in CallerInfo: displayName '"
//                                 + displayName + "', displayNumber '" + displayNumber + "'");
                }
            }
//            personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, info.person_id);
//            if (DBG) log("- got personUri: '" + personUri
//                         + "', based on info.person_id: " + info.person_id);
        } else {
            displayName = PhoneUtils.getPresentationString(mInCallScreen, presentation);
        }
        
        return displayName;
    }
    
    
    void displaySecondHoldCallStatus(Call call) {
        if (DBG) {
            log("displaySecondHoldCallStatus(call =" + call + ")...");
        }

        if (call == null) {
            if (null != m2ndHoldName) {
                m2ndHoldName.setVisibility(View.GONE);
            }
            if (null != m2ndHoldState) {
                m2ndHoldState.setVisibility(View.GONE);
            }
            if (null != mPhotoHoldPre) {
                mPhotoHoldPre.setVisibility(View.GONE);
            }
            if (null != mDualTalkSecondaryCallInfo) {
                mDualTalkSecondaryCallInfo.setVisibility(View.GONE);
            }
            if (null != mDualTalkSecondaryCallPhotoDimEffect) {
                AnimationUtils.Fade.hide(mDualTalkSecondaryCallPhotoDimEffect, View.GONE);
            }
            return ;
        } else {
            if (null == mDualTalkSecondaryCallInfo) {
                mDualTalkSecondaryCallInfo = (ViewStub) findViewById(R.id.dual_talk_secondary_call_info);
                mDualTalkSecondaryCallInfo.setVisibility(View.VISIBLE);
                mPhotoHoldPre = (ImageView) findViewById(R.id.inset_hold_call_2_Photo);
                m2ndHoldName = (TextView) findViewById(R.id.hold_call_2_name);
                m2ndHoldState = (TextView) findViewById(R.id.hold_call_2_state);
                mSecondHoldCallBanner = (ViewGroup) findViewById(R.id.hold_call_2_banner);
                mDualTalkSecondaryCallPhotoDimEffect = findViewById(R.id.dim_effect_for_dual_talk_secondary_photo);
                mDualTalkSecondaryCallPhotoDimEffect.setEnabled(true);
                mDualTalkSecondaryCallPhotoDimEffect.setClickable(true);
                mDualTalkSecondaryCallPhotoDimEffect.setOnClickListener(callCardListener);
                // Add a custom OnTouchListener to manually shrink the
                // "hit target".
                mDualTalkSecondaryCallPhotoDimEffect
                        .setOnTouchListener(new SmallerHitTargetTouchListener());
            } else {
                mDualTalkSecondaryCallInfo.setVisibility(View.VISIBLE);
            }
            m2ndHoldName.setVisibility(View.VISIBLE);
            m2ndHoldState.setVisibility(View.VISIBLE);
            mPhotoHoldPre.setVisibility(View.VISIBLE);
            AnimationUtils.Fade.show(mDualTalkSecondaryCallPhotoDimEffect);
        }
        
        if (DBG) {
            log("displaySecondHoldCallStatus ==> " + call.getConnections());
        }
        
        SIMInfo info = PhoneUtils.getSimInfoByCall(call);
        if (info != null && (info.mColor >= 0 && info.mColor < mSimColorMap.length)) {
            //m2ndHoldName.setBackgroundResource(mSimColorMap[info.mColor]);
            mSecondHoldCallBanner.setBackgroundResource(mSimColorMap[info.mColor]);
        } else if (info == null && (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP)) {
            //m2ndHoldName.setBackgroundResource(R.drawable.incall_status_color8);
            mSecondHoldCallBanner.setBackgroundResource(R.drawable.incall_status_color8);
        }
        mSecondHoldCallBanner.setPadding(mCallBannerSidePadding, 0, mCallBannerSidePadding, 0);

        boolean showSecondaryCallInfo = false;
        Call.State state = call.getState();
        switch (state) {
            case ACTIVE:
                if (DBG) {
                    log("displaySecondHoldCallStatus : meet cdma case!");
                }
                //mDualTalkSecondaryCallInfo.setVisibility(View.GONE);
            case HOLDING:
                // update background color for the secondary call banner
                //updateCallBannerBackground(call, mSecondaryCallBanner);
                // Ok, there actually is a background call on hold.
                // Display the "on hold" box.

                // Note this case occurs only on GSM devices.  (On CDMA,
                // the "call on hold" is actually the 2nd connection of
                // that ACTIVE call; see the ACTIVE case below.)

                if (PhoneUtils.isConferenceCall(call)) {
                    if (DBG) {
                        log("==> conference call.");
                    }
                    m2ndHoldName.setText(getContext().getString(R.string.confCall));
                    if (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                        showImage(mPhotoHoldPre, R.drawable.picture_dialing);
                    } else {
                    showImage(mPhotoHoldPre, R.drawable.picture_conference);
                    }
                } else {
                    PhoneUtils.CallerInfoToken infoToken = null;
                    if (sLCforUserDataSecondHoldCall) {
                        if (DBG) {
                            log("- displaySecondHoldCallStatus: the language changed to clear userdata");
                        }
                        CallInfoCookie callInfoCookie = new CallInfoCookie(call, SECOND_HOLD_CALL_BANNER);
                        infoToken = PhoneUtils.startGetCallerInfo(getContext(), call, this, callInfoCookie, true);
                        sLCforUserDataSecondHoldCall = false;
                    } else {
                        infoToken = PhoneUtils.startGetCallerInfo(getContext(), call, this, m2ndHoldName);
                    }
                    
                    // perform query and update the name temporarily
                    // make sure we hand the textview we want updated to the
                    // callback function.
                    if (DBG) {
                        log("==> NOT a conf call; call startGetCallerInfo...");
                    }
                    
                    m2ndHoldName.setText(
                            PhoneUtils.getCompactNameFromCallerInfo(infoToken.currentInfo,
                                                                    getContext()));

                    // Also pull the photo out of the current CallerInfo.
                    // (Note we assume we already have a valid photo at
                    // this point, since *presumably* the caller-id query
                    // was already run at some point *before* this call
                    // got put on hold.  If there's no cached photo, just
                    // fall back to the default "unknown" image.)
                    if (infoToken.isFinal) {
                        Connection conn = null;
                        int phoneType = call.getPhone().getPhoneType();
                        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                            conn = call.getLatestConnection();
                        } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                              || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                            conn = call.getEarliestConnection();
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }

                        final int presentation = conn.getNumberPresentation();
                        updateDisplayForPerson(infoToken.currentInfo, presentation, false, call,
                                conn, null, false, null, null, mPhotoHoldPre);
                    } else {
                        //showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
                        showImage(mPhotoHoldPre, R.drawable.picture_unknown);
                    }
                }
                m2ndHoldState.setText(getContext().getString(R.string.card_title_on_hold));
                break;

            default:
                // There's actually no call on hold.  (Presumably this call's
                // state is IDLE, since any other state is meaningless for the
                // background call.)
                //showSecondaryCallInfo = false;
                mDualTalkSecondaryCallInfo.setVisibility(View.GONE);
                break;
        }
    }
    
    View.OnClickListener callCardListener = new View.OnClickListener() {
        
        public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.dim_effect_for_incoming_call_2_photo:
                    mShowSwtchCall2Animator = true;
                    displaySwitchIncomingAnimator();
                    break;

                case R.id.dim_effect_for_dual_talk_secondary_photo:
                    log("callCardListener: " + "Which call to disconnected?");
                    //mInCallScreen.askWhichCallDisconnected(mDualTalk.getAllNoIdleCalls(), false);
                    if (mDualTalk.isSupportDualTalk() && mDualTalk.isDualTalkMultipleHoldCase()) {
                        mInCallScreen.handleUnholdAndEnd(mDualTalk.getActiveFgCall());
                    } else if (mDualTalk.isSupportDualTalk() && mDualTalk.isMultiplePhoneActive()
                            && !mDualTalk.hasMultipleRingingCall()) {
                        PhoneUtils.switchHoldingAndActive(mDualTalk.getSecondActiveBgCall());
                    } else {
                        PhoneUtils.switchHoldingAndActive(mApplication.mCM.getFirstActiveBgCall());
                    }
                    break;
                
                default:
                    log("do nothing");
                    break;
            }
        }
        
    };

    public String getCallInfoName(int position) {
        if (position == 0) {
            if (mName != null && mName.getText() != null) {
                return mName.getText().toString();
            } else {
                return null;
            }
        } else if (position == 1) {
            if (mSecondaryCallName != null && mSecondaryCallName.getText() != null) {
                return mSecondaryCallName.getText().toString();
            } else {
                return null;
            }
        } else {
            if (m2ndHoldName != null && m2ndHoldName.getText() != null) {
                return m2ndHoldName.getText().toString();
            } else {
                return null;
            }
        }
    }

    public void hideCallStates(CallManager cm) {
        if (mSimInfo != null && TextUtils.isEmpty(mSimInfo.mDisplayName)) {
            if (DBG) {
                log("mSimIndicator GONE");
            }
            mSimIndicator.setVisibility(View.GONE);
        }
        if (cm != null && cm.getFirstActiveBgCall() == null) {
            if (DBG) {
                log("mSecondaryCallInfo GONE");
            }
            mSecondaryCallInfo.setVisibility(View.GONE);
        }
    }

    /**
     * There are multiple phones in non-idle state
     * 
     * @return
     */
    public boolean hasMultiplePhoneActive() {
        CallManager cm = mApplication.mCM;
        if (null == cm || cm.getState() == PhoneConstants.State.IDLE) {
            if (DBG) {
                log("CallManager says in idle state!");
            }
            return false;
        }

        List<Phone> phoneList = cm.getAllPhones();
        log("CallManager says in idle state!" + phoneList);
        int count = 0;
        // Maybe need to check the call status??
        for (Phone phone : phoneList) {
            if (phone.getState() == PhoneConstants.State.OFFHOOK) {
                count++;
                if (DBG) {
                    log("non IDLE phone = " + phone.toString());
                }
                if (count > 1) {
                    if (DBG) {
                        log("More than one phone active!");
                    }
                    return true;
                }
            }
        }
        if (DBG) {
            log("Strange! no phone active but we go here!");
        }
        return false;
    }

    Animation.AnimationListener mAnimationListener = new Animation.AnimationListener() {
        public void onAnimationEnd(Animation animation) {
            log("onAnimationEnd");
            boolean allAnimationEnd = false;
            int animationCount = ((AnimationSet)animation).getAnimations().size();
            mShowAnimator2End++;

            log(" mShowAnimator2End " + mShowAnimator2End + " animationCount " + animationCount);

            if (mShowAnimator2End == animationCount) {
                allAnimationEnd = true;
            }

            if (allAnimationEnd && mShowSwtchCall2Animator) {
                //when animation end, switch 2 incomming call
                mDualTalk.switchCalls();
                mInCallScreen.requestUpdateScreen();
                mShowSwtchCall2Animator = false;
            }
            if (allAnimationEnd) {
                RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.inset_incoming_call_2_info);
                ViewGroup.MarginLayoutParams source = new ViewGroup.MarginLayoutParams(
                        mIncomingCallInfoWidth, mIncomingCallInfoHeight);
                source.topMargin = mIncomingCallInfoTopMargin;
                RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(source);
                param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                relativeLayout.setLayoutParams(param);
                mIncomingCall2Info.clearAnimation();
                mShowAnimator2End = 0;
            }
        }
        public void onAnimationRepeat(Animation animation) {
            log("onAnimationRepeat");
        }
        public void onAnimationStart(Animation animation) {
            log("onAnimationStart");
            RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.inset_incoming_call_2_info);
            ViewGroup.MarginLayoutParams source = new ViewGroup.MarginLayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            source.topMargin = 0;
            RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(source);
            relativeLayout.setLayoutParams(param);
            if (mNeedShowIncomingCall2Animator) {
                mNeedShowIncomingCall2Animator = false;
            }
        }
    };

    private void displaySecondaryIncomingAnimator() {
        // Animate the photo view into its end location.
        AnimationSet animationSet = new AnimationSet(true);

        int dx = mPrimaryCallInfo.getRight() + mIncomingCallInfoWidth;
        int dy = mIncomingCallInfoTopMargin + mIncomingCallInfoHeight;
        TranslateAnimation translateAnimation = new TranslateAnimation(0, dx, 0, dy);
        translateAnimation.setDuration(300);
        animationSet.addAnimation(translateAnimation);

        ScaleAnimation myAnimationScale = new ScaleAnimation(0.0f, 0.4f, 0.0f, 0.4f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        myAnimationScale.setDuration(0);
        animationSet.addAnimation(myAnimationScale);

        animationSet.setAnimationListener(mAnimationListener);
        mIncomingCall2Info.startAnimation(animationSet);
    }

    private void displaySwitchIncomingAnimator() {
        // Animate the photo view into its end location.
        int dx = mPrimaryCallInfo.getRight() - mIncomingCallInfoWidth;

        AnimationSet animationSet = new AnimationSet(true);
        TranslateAnimation translateAnimation = new TranslateAnimation(dx, 0, mIncomingCallInfoTopMargin, 0);
        translateAnimation.setDuration(500);
        animationSet.addAnimation(translateAnimation);

        ScaleAnimation myAnimationScale = new ScaleAnimation(0.4f, 0.0f, 0.4f, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        myAnimationScale.setDuration(500);
        animationSet.addAnimation(myAnimationScale);
        animationSet.setAnimationListener(mAnimationListener);

        mIncomingCall2Info.startAnimation(animationSet);
    }

}
