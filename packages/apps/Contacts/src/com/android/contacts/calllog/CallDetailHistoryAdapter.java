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

package com.android.contacts.calllog;

import android.content.Context;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.contacts.PhoneCallDetails;
import com.android.contacts.R;
import com.android.contacts.editor.AggregationSuggestionEngine.RawContact;
/// The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.dialpad.DialerSearchAdapter.ViewHolder;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.VvmUtils;
import com.mediatek.phone.SIMInfoWrapper;
/// The previous lines are provided and maintained by Mediatek Inc.

/**
 * Adapter for a ListView containing history items from the details of a call.
 */
public class CallDetailHistoryAdapter extends BaseAdapter {
    /** The top element is a blank header, which is hidden under the rest of the UI. */
    private static final int VIEW_TYPE_HEADER = 0;
    /** Each history item shows the detail of a call. */
    private static final int VIEW_TYPE_HISTORY_ITEM = 1;

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final CallTypeHelper mCallTypeHelper;
    private final PhoneCallDetails[] mPhoneCallDetails;
    /** Whether the voicemail controls are shown. */
    private final boolean mShowVoicemail;
    /** Whether the call and SMS controls are shown. */
    private final boolean mShowCallAndSms;
    /** The controls that are shown on top of the history list. */
    private final View mControls;
    /** The listener to changes of focus of the header. */
    private View.OnFocusChangeListener mHeaderFocusChangeListener =
            new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            // When the header is focused, focus the controls above it instead.
            if (hasFocus) {
                mControls.requestFocus();
            }
        }
    };

    public CallDetailHistoryAdapter(Context context, LayoutInflater layoutInflater,
            CallTypeHelper callTypeHelper, PhoneCallDetails[] phoneCallDetails,
            boolean showVoicemail, boolean showCallAndSms, View controls) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mCallTypeHelper = callTypeHelper;
        mPhoneCallDetails = phoneCallDetails;
        mShowVoicemail = showVoicemail;
        mShowCallAndSms = showCallAndSms;
        mControls = controls;
        /** M: New Feature RCS @{ */
        if (null != mPhoneCallDetails[0] && null != mPhoneCallDetails[0].number) {
            mNumber = mPhoneCallDetails[0].number.toString();
        }
        /** @} */
    }

    @Override
    public boolean isEnabled(int position) {
        // None of history will be clickable.
        return false;
    }

    @Override
    public int getCount() {
        return mPhoneCallDetails.length + 1;
    }

    @Override
    public Object getItem(int position) {
        if (position == 0) {
            return null;
        }
        return mPhoneCallDetails[position - 1];
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) {
            return -1;
        }
        return position - 1;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_HISTORY_ITEM;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            /** M: [VVM] modify @ { */
            /**
            final View header = convertView == null
                    ? mLayoutInflater.inflate(R.layout.call_detail_history_header, parent, false)
                    : convertView;
            // Voicemail controls are only shown in the main UI if there is a voicemail.
            View voicemailContainer = header.findViewById(R.id.header_voicemail_container);
            voicemailContainer.setVisibility(mShowVoicemail ? View.VISIBLE : View.GONE);
            */
            View header = null;
            if (VvmUtils.isVvmEnabled()) {
                header = convertView == null ? mLayoutInflater.inflate(
                        R.layout.call_detail_history_with_voicemail_header, parent, false) : convertView;
                View voicemailContainer = header.findViewById(R.id.header_voicemail_container);
                voicemailContainer.setVisibility(mShowVoicemail ? View.VISIBLE : View.GONE);
            } else {
                header = convertView == null ? mLayoutInflater.inflate(
                        R.layout.call_detail_history_without_voicemail_header, parent, false) : convertView;
            }
            /** @ }*/

            Boolean hasSIMInsert = SIMInfoWrapper.getDefault().getInsertedSimCount() == 0 ? false
                    : true;
            
            // Call and SMS controls are only shown in the main UI if there is a known number.
            View callAndSmsContainer = header.findViewById(R.id.header_call_and_sms_container);
            callAndSmsContainer.setVisibility(mShowCallAndSms ? View.VISIBLE : View.GONE);
            
            /** M: New Feature Phone Landscape UI @{ */
            View topView = header.findViewById(R.id.main_action_push_layer);
            if (topView != null) {
                topView.setVisibility(mShowCallAndSms ? View.VISIBLE : View.GONE);
            }
            /** @ }*/

            /** M: add @ { */
            View videoCallContainer = header.findViewById(R.id.header_video_call_container);

            View ipCallContainer = header.findViewById(R.id.header_ip_call_container);
            if (mShowVoicemail) {
                ipCallContainer.setVisibility(View.GONE);
            } else {
                ipCallContainer.setVisibility((mShowCallAndSms && hasSIMInsert) ? View.VISIBLE : View.GONE);
            }
            View separator02 = header.findViewById(R.id.separator02);
            separator02.setVisibility((mShowCallAndSms && hasSIMInsert) ? View.VISIBLE : View.GONE);
            

            View separator01 = header.findViewById(R.id.separator01);

            /** M: New Feature RCS @{ */
            ExtensionManager.getInstance().getCallDetailExtension().setViewVisible(header, mNumber,
                    ExtensionManager.COMMD_FOR_RCS, R.id.header_RCS_container, R.id.header_RCS_container, 0, 0, 0, 0, 0);
            /** @} */
            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                videoCallContainer.setVisibility(mShowCallAndSms ? View.VISIBLE : View.GONE);
                separator01.setVisibility(mShowCallAndSms ? View.VISIBLE : View.GONE);
            } else {
                videoCallContainer.setVisibility(View.GONE);
                separator01.setVisibility(View.GONE);
            }
            /** @ }*/

            header.setFocusable(true);
            header.setOnFocusChangeListener(mHeaderFocusChangeListener);
            return header;
        }

        // Make sure we have a valid convertView to start with
        final View result  = convertView == null
                ? mLayoutInflater.inflate(R.layout.call_detail_history_item, parent, false)
                : convertView;

        PhoneCallDetails details = mPhoneCallDetails[position - 1];
        CallTypeIconsView callTypeIconView =
                (CallTypeIconsView) result.findViewById(R.id.call_type_icon);
        TextView callTypeTextView = (TextView) result.findViewById(R.id.call_type_text);
        TextView dateView = (TextView) result.findViewById(R.id.date);
        TextView durationView = (TextView) result.findViewById(R.id.duration);
        /** M:  modify @ { */
        /**
         * int callType = details.callTypes[0];
         */
        int callType = details.callType;
        /** @ } */

        callTypeIconView.clear();
        /** M:  modify @ { */
        /**
        callTypeIconView.add(callType);
        callTypeTextView.setText(mCallTypeHelper.getCallTypeText(callType));
         */
        int bVTCall = details.vtCall;
        callTypeIconView.set(callType, bVTCall);
        Log.d("CallDetailHistoryAdapter", "IP prefix:" + details.ipPrefix + " position: "
                + position);
        if (!TextUtils.isEmpty(details.ipPrefix) && callType == Calls.OUTGOING_TYPE) {
            Log.d("CallDetailHistoryAdapter ", " ipPrefix");
            String mIPOutgoingName = mContext
                    .getString(R.string.type_ip_outgoing, details.ipPrefix);
            callTypeTextView.setText(mIPOutgoingName);
        } else {
            callTypeTextView.setText(mCallTypeHelper.getCallTypeText(callType));
        }
        /** @ }*/
        // Set the date.
        CharSequence dateValue = DateUtils.formatDateRange(mContext, details.date, details.date,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
        dateView.setText(dateValue);
        // Set the duration
        /** M: New Feature Easy Porting @ { */
        /**
         * if (callType == Calls.MISSED_TYPE || callType == Calls.VOICEMAIL_TYPE) {
            durationView.setVisibility(View.GONE);
        } else {
            durationView.setVisibility(View.VISIBLE);
            durationView.setText(formatDuration(details.duration));
        }
         */
        ExtensionManager.getInstance().getCallDetailExtension().setTextView(callType, durationView,
                formatDuration(details.duration), ContactPluginDefault.COMMD_FOR_OP01);
            /** @ } */

        return result;
    }

    private String formatDuration(long elapsedSeconds) {
        long minutes = 0;
        long seconds = 0;

        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }
        seconds = elapsedSeconds;

        return mContext.getString(R.string.callDetailsDurationFormat, minutes, seconds);
    }
    /** M: New Feature RCS @{ */
    private String mNumber = null;
    /** @} */
}
