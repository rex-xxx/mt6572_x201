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

import android.content.res.Resources;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.contacts.R;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.telephony.PhoneNumberUtilsEx;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * Helper for formatting and managing phone numbers.
 */
public class PhoneNumberHelper {
    private final Resources mResources;

    public PhoneNumberHelper(Resources resources) {
        mResources = resources;
    }

    /** Returns true if it is possible to place a call to the given number. */
    public boolean canPlaceCallsTo(CharSequence number) {
        return !(TextUtils.isEmpty(number)
                || number.equals(CallerInfo.UNKNOWN_NUMBER)
                || number.equals(CallerInfo.PRIVATE_NUMBER)
                || number.equals(CallerInfo.PAYPHONE_NUMBER));
    }

    /** Returns true if it is possible to send an SMS to the given number. */
    public boolean canSendSmsTo(CharSequence number) {
        /** M:  modify @ { */
       /**
        * return canPlaceCallsTo(number) && !isVoicemailNumber(number) && !isSipNumber(number);
        */
        return canPlaceCallsTo(number) && !isSipNumber(number);
        /** @ } */
    }

    /**
     * Returns the string to display for the given phone number.
     *
     * @param number the number to display
     * @param formattedNumber the formatted number if available, may be null
     */
    public CharSequence getDisplayNumber(CharSequence number, CharSequence formattedNumber) {
        if (TextUtils.isEmpty(number)) {
            return "";
        }
        if (number.equals(CallerInfo.UNKNOWN_NUMBER)) {
            return mResources.getString(R.string.unknown);
        }
        if (number.equals(CallerInfo.PRIVATE_NUMBER)) {
            return mResources.getString(R.string.private_num);
        }
        if (number.equals(CallerInfo.PAYPHONE_NUMBER)) {
            return mResources.getString(R.string.payphone);
        }
        
        /** M:  delete @ { */
        /*if (isVoicemailNumber(number)) {
            return mResources.getString(R.string.voicemail);
        } */
        /** @ }*/
        if (TextUtils.isEmpty(formattedNumber)) {
            return number;
        } else {
            return formattedNumber;
        }
    }

    /**
     * Returns true if the given number is the number of the configured voicemail.
     * To be able to mock-out this, it is not a static method.
     */
    public boolean isVoicemailNumber(CharSequence number, int simId) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            return PhoneNumberUtilsEx.isVoiceMailNumber(number.toString(),
                    SIMInfoWrapper.getDefault().getSlotIdBySimId(simId));
        } else {
            return PhoneNumberUtils.isVoiceMailNumber(number.toString());
        }
    }

    /**
     * Returns true if the given number is a SIP address.
     * To be able to mock-out this, it is not a static method.
     */
    public boolean isSipNumber(CharSequence number) {
        return PhoneNumberUtils.isUriNumber(number.toString());
    }
    
    /** M: add @ { */
    /** Returns true if the given number is a emergency number. */
    public boolean isEmergencyNumber(CharSequence number) {
        return PhoneNumberUtils.isEmergencyNumber(number.toString());
}
    
    /** Returns true if the given number is a isVoiceMailNumberForMtk . */
    public boolean isVoiceMailNumberForMtk(CharSequence number, int simId) {
        return PhoneNumberHelper.isSimVoiceMailNumber(number, simId);
    }

    /**
     * M: [Gemini+] Check whether the simId SIM's voice mail number info meets the number
     * @param number a given number be check if is the voice mail
     * @param simId SIM id, not slot id.
     * @return true if yes
     */
    public static boolean isSimVoiceMailNumber(CharSequence number, int simId) {
        final int slotId = SIMInfoWrapper.getDefault().getSimSlotById(simId);
        String voiceMailNumber = SlotUtils.getVoiceMailNumberForSlot(slotId);
        return (voiceMailNumber != null && PhoneNumberUtils.compare(voiceMailNumber, number.toString()));
    }

    public static void getVoiceMailNumber() {
        SlotUtils.updateVoiceMailNumber();
    }
   
   public static boolean isVoicemailUri(Uri uri) {
       if (uri == null) {
           return false;
       }
       
       String scheme = uri.getScheme();
       return "voicemail".equals(scheme);
   }
   
   public Uri getCallUri(String number, int simId) {
       if (isVoiceMailNumberForMtk(number, simId)) {
           return Uri.parse("voicemail:x");
       }
       if (SlotUtils.isGeminiEnabled()) {
           return Uri.fromParts("tel", number, null);
       } else {
           if (isSipNumber(number)) {
               return Uri.fromParts("sip", number, null);
           }
           return Uri.fromParts("tel", number, null);
       }
       
    }
   /** @ }*/

}
