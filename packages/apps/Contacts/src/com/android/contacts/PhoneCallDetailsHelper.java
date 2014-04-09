/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.contacts.calllog.CallTypeHelper;
import com.android.contacts.calllog.PhoneNumberHelper;
import com.android.contacts.format.FormatUtils;
import com.android.contacts.test.NeededForTesting;
import com.android.internal.telephony.CallerInfo;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.calllog.CallLogListItemView;
import com.mediatek.contacts.calllog.CallLogSimInfoHelper;
import com.mediatek.phone.SIMInfoWrapper;

import java.util.Date;

/**
 * Helper class to fill in the views in {@link PhoneCallDetailsViews}.
 */
public class PhoneCallDetailsHelper {

    /** M:  modify @ { */
    /** The maximum number of icons will be shown to represent the call types in a group. */
    //private static final int MAX_CALL_TYPE_ICONS = 3;
    private static final int MAX_CALL_TYPE_ICONS = 1;

    /** @ }*/

    private final Resources mResources;
    /** The injected current time in milliseconds since the epoch. Used only by tests. */
    private Long mCurrentTimeMillisForTest;
    // Helper classes.
    private final CallTypeHelper mCallTypeHelper;
    private final PhoneNumberHelper mPhoneNumberHelper;

    /** M:  delete @ { */
    /**
     * Creates a new instance of the helper.
     * <p>
     * Generally you should have a single instance of this helper in any context.
     *
     * @param resources used to look up strings
     */
//    public PhoneCallDetailsHelper(Resources resources, CallTypeHelper callTypeHelper,
//            PhoneNumberHelper phoneNumberHelper) {
//        mResources = resources;
//        mCallTypeHelper = callTypeHelper;
//        mPhoneNumberHelper = phoneNumberHelper;
//    }
    /** @ } */

    /** Fills the call details views with content. */
    /** M:  modify @ { */
    /**
     * 
    public void setPhoneCallDetails(PhoneCallDetailsViews views, PhoneCallDetails details,
            boolean isHighlighted) {
        // Display up to a given number of icons.
        views.callTypeIcons.clear();
        int count = details.callTypes.length;
        for (int index = 0; index < count && index < MAX_CALL_TYPE_ICONS; ++index) {
            views.callTypeIcons.add(details.callTypes[index]);
        }
        views.callTypeIcons.setVisibility(View.VISIBLE);

        // Show the total call count only if there are more than the maximum number of icons.
        final Integer callCount;
        if (count > MAX_CALL_TYPE_ICONS) {
            callCount = count;
        } else {
            callCount = null;
        }
     */

   public void setPhoneCallDetails(CallLogListItemView views, PhoneCallDetails details,
                                    boolean isHighlighted, boolean isEmergencyNumber,
                                    boolean isVoiceMailNumber) {
       //set the call type 
        views.setCallType(details.callType, details.vtCall);
        // The color to highlight the count and date in, if any. This is based
        // on the first call.
        Integer highlightColor =
            isHighlighted ? mCallTypeHelper.getHighlightedColor(details.callType) : null;

        // The date of this call, relative to the current time.
        /** M: modify @ { */
        /**
        * 
        CharSequence dateText =
            DateUtils.getRelativeTimeSpanString(details.date,
                    getCurrentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);

        // Set the call count and date.
        setCallCountAndDate(views, callCount, dateText, highlightColor);
        */
        // set the call date
        String dateText = DateFormat.getTimeFormat(mContext).format(new Date(details.date));
        setCallDate(views.getCallTimeTextView(), dateText, highlightColor);
        // Set the call count
        setCallCount(views.getCallCountTextView(), details.callCount, highlightColor);
        /** @ }*/
        CharSequence numberFormattedLabel = null;
        // Only show a label if the number is shown and it is not a SIP address.
        if (!TextUtils.isEmpty(details.number) && !PhoneNumberUtils.isUriNumber(details.number.toString())) {
            /** M: modify @ { */
            /**
             * numberFormattedLabel = Phone.getTypeLabel(mResources, details.numberType,
             * details.numberLabel);
             */
            if (0 == details.numberType) {
                numberFormattedLabel = mResources.getString(R.string.list_filter_custom);
            } else {
                /** M:AAS @ {
                numberFormattedLabel = Phone.getTypeLabel(mResources, details.numberType, details.numberLabel); */
                int slotId = SIMInfoWrapper.getDefault().getSimSlotById((int) details.simId);
                Log.d(TAG, "setPhoneCallDetails() simId=" + details.simId + " slotId=" + slotId);
                numberFormattedLabel = ExtensionManager.getInstance().getContactAccountExtension().getTypeLabel(
                        mResources, details.numberType, details.numberLabel, slotId, ExtensionManager.COMMD_FOR_AAS);
                /** @ } */
            }
            /** @ } */
        }

        final CharSequence nameText;
        final CharSequence numberText;
        /** M:  modify @ { */
       /**
        *
        final CharSequence labelText;
        final CharSequence displayNumber =
            mPhoneNumberHelper.getDisplayNumber(details.number, details.formattedNumber);
        if (TextUtils.isEmpty(details.name)) {
            nameText = displayNumber;
            if (TextUtils.isEmpty(details.geocode)
                    || mPhoneNumberHelper.isVoicemailNumber(details.number)) {
                numberText = mResources.getString(R.string.call_log_empty_gecode);
            } else {
                numberText = details.geocode;
            }
            labelText = null;
        } else {
            nameText = details.name;
            numberText = displayNumber;
            labelText = numberFormattedLabel;
        }

        views.nameView.setText(nameText);
        views.numberView.setText(numberText);
        views.labelView.setText(labelText);
        views.labelView.setVisibility(TextUtils.isEmpty(labelText) ? View.GONE : View.VISIBLE);
        */
        boolean bSpecialNumber = (details.number.equals(CallerInfo.UNKNOWN_NUMBER)
                               || details.number.equals(CallerInfo.PRIVATE_NUMBER)
                               || details.number.equals(CallerInfo.PAYPHONE_NUMBER));

        if (isEmergencyNumber) {
            nameText = mResources.getString(R.string.emergencycall);
            numberText = details.number;
        } else if (isVoiceMailNumber) {
            nameText = mResources.getString(R.string.voicemail);
            numberText = details.number;
        } else {
            ///M: to fix number display order problem in Dialpad in Arabic/Hebrew/Urdu, {@
            final String dsplNumber =
                mPhoneNumberHelper.getDisplayNumber(details.number, details.formattedNumber).toString();
            final CharSequence displayNumber = TextUtils.isEmpty(dsplNumber) ? dsplNumber : '\u202D' + dsplNumber + '\u202C';
            ///@}
            if (TextUtils.isEmpty(details.name) || bSpecialNumber) {
                nameText = displayNumber;
                if (TextUtils.isEmpty(details.geocode)) {
                    numberText = mResources.getString(R.string.call_log_empty_gecode);
                } else {
                    numberText = details.geocode;
                }
            } else {
                nameText = details.name;
                if (numberFormattedLabel != null) {
                    numberText = FormatUtils.applyStyleToSpan(
                            Typeface.BOLD, numberFormattedLabel + " "
                            + displayNumber, 0,
                            numberFormattedLabel.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    numberText = displayNumber;
                }
            }
        }

        views.setCallLogName(nameText.toString());
        views.setNumber(numberText.toString());
        // Set the sim name
        //if (FeatureOption.MTK_GEMINI_SUPPORT) {
            log("setPhoneCallDetails()  MTK_GEMINI_SUPPORT");
            if (null != mCallLogSimInfoHelper) {
                setSimInfo(views.getSimNameTextView(), mCallLogSimInfoHelper
                        .getSimDisplayNameById(details.simId), mCallLogSimInfoHelper
                        .getSimColorDrawableById(details.simId));
            }
        /*} else {
            views.getSimNameTextView().setVisibility(View.GONE);
        }*/
        /** @ }*/
    }

    /** Sets the text of the header view for the details page of a phone call. */
    public void setCallDetailsHeader(TextView nameView, PhoneCallDetails details) {
        final CharSequence nameText;
        final CharSequence displayNumber =
                mPhoneNumberHelper.getDisplayNumber(details.number,
                        mResources.getString(R.string.recentCalls_addToContact));
        if (TextUtils.isEmpty(details.name)) {
            /** M: modify @ { */
            // nameText = displayNumber;
            if (mPhoneNumberHelper.isSipNumber(details.number) && null != details.contactUri) {
                nameText = mResources.getString(R.string.missing_name);
            } else {
                nameText = displayNumber;
            }
            /** @ }*/
        } else {
            nameText = details.name;
        }

        nameView.setText(nameText);
    }

    @NeededForTesting
    public void setCurrentTimeForTest(long currentTimeMillis) {
        mCurrentTimeMillisForTest = currentTimeMillis;
    }

    /**
     * Returns the current time in milliseconds since the epoch.
     * <p>
     * It can be injected in tests using {@link #setCurrentTimeForTest(long)}.
     */
    private long getCurrentTimeMillis() {
        if (mCurrentTimeMillisForTest == null) {
            return System.currentTimeMillis();
        } else {
            return mCurrentTimeMillisForTest;
        }
    }

    /** M: delete @ { */
    /** Sets the call count and date. */
    /**
     * 
    private void setCallCountAndDate(PhoneCallDetailsViews views, Integer callCount,
            CharSequence dateText, Integer highlightColor) {
        // Combine the count (if present) and the date.
        final CharSequence text;
        if (callCount != null) {
            text = mResources.getString(
                    R.string.call_log_item_count_and_date, callCount.intValue(), dateText);
        } else {
            text = dateText;
        }

        // Apply the highlight color if present.
        final CharSequence formattedText;
        if (highlightColor != null) {
            formattedText = addBoldAndColor(text, highlightColor);
        } else {
            formattedText = text;
        }

        views.callTypeAndDate.setText(formattedText);
    }
     */
    /** @ }*/
    /** Creates a SpannableString for the given text which is bold and in the given color. */
    private CharSequence addBoldAndColor(CharSequence text, int color) {
        int flags = Spanned.SPAN_INCLUSIVE_INCLUSIVE;
        SpannableString result = new SpannableString(text);
        result.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), flags);
        result.setSpan(new ForegroundColorSpan(color), 0, text.length(), flags);
        return result;
    }

    /** M: add @ { */
    private static final String TAG = "PhoneCallDetailsHelper";
    private final CallLogSimInfoHelper mCallLogSimInfoHelper;
    private final Context mContext;
    private int mLPadding;
    private int mRPadding;
    public PhoneCallDetailsHelper(Resources resources, CallTypeHelper callTypeHelper,
            PhoneNumberHelper phoneNumberHelper, CallLogSimInfoHelper callLogSimInfoHelper,
            Context context) {
        mResources = resources;
        mCallTypeHelper = callTypeHelper;
        mPhoneNumberHelper = phoneNumberHelper;
        mCallLogSimInfoHelper = callLogSimInfoHelper;
        mContext = context;
        mRPadding = mResources.getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_right);
        mLPadding = mResources.getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_left);

    }
    
    /** Set the call simInfo. */
    private void setSimInfo(TextView view, final String simName, final Drawable simColor) {
        log("setSimInfo() simName = " + simName);
        if (TextUtils.isEmpty(simName)) {
            log("setSimInfo() simName is null or   simColor is null, simname will not show");
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(simName);
        view.setBackgroundDrawable(simColor);
        view.setPadding(mLPadding, 1, mRPadding, 1);
    }
    /** Set the call date . */
    private void setCallDate(TextView view, CharSequence dateText, Integer highlightColor) {
        // Apply the highlight color if present.
        final CharSequence formattedText;
        if (highlightColor != null) {
            formattedText = addBoldAndColor(dateText, highlightColor);
        } else {
            formattedText = dateText;
        }

        view.setText(formattedText);
    }
    
    /** Set the call count . */
    private void setCallCount(TextView view, int callCount,
                              Integer highlightColor) {
        // Combine the count (if present) and the date.
        CharSequence text = "";
        if (callCount > MAX_CALL_TYPE_ICONS) {
            if (callCount < 10) {
                text = mResources.getString(R.string.call_log_item_count, callCount);
            } else {
                text = "(9+)";
            }
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }

        // Apply the highlight color if present.
        final CharSequence formattedText;
        if (highlightColor != null) {
            formattedText = addBoldAndColor(text, highlightColor);
        } else {
            formattedText = text;
        }

        view.setText(formattedText);
    }
    
    private void log(String log) {
        Log.i(TAG, log);
    }
    /** @ }*/
}
