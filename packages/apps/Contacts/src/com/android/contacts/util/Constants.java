/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.util;

import android.net.Uri;
import android.provider.CallLog;

public class Constants {
    public static final String MIME_TYPE_VIDEO_CHAT = "vnd.android.cursor.item/video-chat-address";

    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_SIP = "sip";

    /**
     * Log tag for performance measurement.
     * To enable: adb shell setprop log.tag.ContactsPerf VERBOSE
     */
    public static final String PERFORMANCE_TAG = "ContactsPerf";

    /**
     * Log tag for enabling/disabling StrictMode violation log.
     * To enable: adb shell setprop log.tag.ContactsStrictMode DEBUG
     */
    public static final String STRICT_MODE_TAG = "ContactsStrictMode";
    
    public static final String SIM_FILTER_PREF = "calllog_sim_filter";
    public static final String TYPE_FILTER_PREF = "calllog_type_filter";
    
    public static final int FILTER_BASE = 20000;
    public static final int FILTER_SIM_ALL = FILTER_BASE + 1;
    //public static final int FILTER_SIM_1 = FILTER_BASE + 2;
    //public static final int FILTER_SIM_2 = FILTER_BASE + 3;
    public static final int FILTER_SIP_CALL = FILTER_BASE + 4;
    public static final int FILTER_ALL_RESOURCES = FILTER_BASE + 5;

    public static final int FILTER_TYPE_ALL = FILTER_BASE + 11;
    public static final int FILTER_TYPE_INCOMING = FILTER_BASE + 12;
    public static final int FILTER_TYPE_MISSED = FILTER_BASE + 13;
    public static final int FILTER_TYPE_OUTGOING = FILTER_BASE + 14;
    public static final int FILTER_TYPE_AUTO_REJECT = FILTER_BASE + 21;

    public static final int FILTER_SIM_DEFAULT = FILTER_SIM_ALL;
    public static final int FILTER_TYPE_DEFAULT = FILTER_TYPE_ALL;
    
    public static final String EXTRA_SLOT_ID = "com.android.phone.extra.slot";
    public static final String EXTRA_ORIGINAL_SIM_ID = "com.android.phone.extra.original";
    public static final String EXTRA_IS_VIDEO_CALL = "com.android.phone.extra.video";
    public static final String EXTRA_IS_IP_DIAL = "com.android.phone.extra.ip";
    public static final String EXTRA_FOLLOW_SIM_MANAGEMENT = "follow_sim_management";
    public static final String EXTRA_ACTUAL_NUMBER_TO_DIAL = "android.phone.extra.ACTUAL_NUMBER_TO_DIAL";
    public static final String EXTRA_INTERNATIONAL_DIAL_OPTION = "com.android.phone.extra.international";

    public static final String VOICEMAIL_URI = "voicemail:";
    public static final String PHONE_PACKAGE = "com.android.phone";
    public static final String OUTGOING_CALL_BROADCASTER = "com.android.phone.OutgoingCallBroadcaster";
    public static final String CALL_SETTINGS_CLASS_NAME = "com.mediatek.settings.VoiceMailSetting";

    public static final String IS_GOOGLE_SEARCH = "false";
    
    public static final Uri CALLLOG_SEARCH_URI_BASE = Uri.parse("content://" + CallLog.AUTHORITY
            + "/calls/search_filter/");

    public static final int DIAL_NUMBER_INTENT_NORMAL = 0;
    public static final int DIAL_NUMBER_INTENT_IP = 1;
    public static final int DIAL_NUMBER_INTENT_VIDEO = 2;
}
