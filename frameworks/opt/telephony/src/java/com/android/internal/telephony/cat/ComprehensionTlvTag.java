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

package com.android.internal.telephony.cat;

/**
 * Enumeration for representing the tag value of COMPREHENSION-TLV objects. If
 * you want to get the actual value, call {@link #value() value} method. {@hide
 * }
 */
public enum ComprehensionTlvTag {
    COMMAND_DETAILS(0x01),
    DEVICE_IDENTITIES(0x02),
    RESULT(0x03),
    DURATION(0x04),
    ALPHA_ID(0x05),
    USSD_STRING(0x0a),
    SMS_TPDU(0x0b),
    TEXT_STRING(0x0d),
    TONE(0x0e),
    ITEM(0x0f),
    ITEM_ID(0x10),
    RESPONSE_LENGTH(0x11),
    FILE_LIST(0x12),
    HELP_REQUEST(0x15),
    DEFAULT_TEXT(0x17),
    EVENT_LIST(0x19),
    ICON_ID(0x1e),
    ITEM_ICON_ID_LIST(0x1f),
    IMMEDIATE_RESPONSE(0x2b),
    LANGUAGE(0x2d),
    URL(0x31),
    BROWSER_TERMINATION_CAUSE(0x34),
        // Add by Huibin Mao MTK80229
    // ICS Migration start
    // data object for class "e"
    BEARER_DESCRIPTION(0x35),
    CHANNEL_DATA(0x36),
    CHANNEL_DATA_LENGTH(0x37),
    CHANNEL_STATUS(0x38),
    BUFFER_SIZE(0x39),
    SIM_ME_INTERFACE_TRANSPORT_LEVEL(0x3C),
    OTHER_ADDRESS(0x3E),
    NETWORK_ACCESS_NAME(0x47),
    ADDRESS(0x06),
    NEXT_ACTION_INDICATOR(0x18),
    DATE_TIME_AND_TIMEZONE(0x26),
        // ICS Migration end
    TEXT_ATTRIBUTE(0x50);

    private int mValue;

    ComprehensionTlvTag(int value) {
        mValue = value;
    }

    /**
     * Returns the actual value of this COMPREHENSION-TLV object.
     * 
     * @return Actual tag value of this object
     */
    public int value() {
        return mValue;
    }

    public static ComprehensionTlvTag fromInt(int value) {
        for (ComprehensionTlvTag e : ComprehensionTlvTag.values()) {
            if (e.mValue == value) {
                return e;
            }
        }
        return null;
    }
}
