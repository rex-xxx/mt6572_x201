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

package com.mediatek.calloption;

import android.content.Context;
import android.content.Intent;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.android.phone.Constants;
import com.mediatek.phone.GeminiConstants;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.PhoneNumberUtilsEx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CallOptionUtils {

    private static final String TAG = "CallOptionUtils";

    public static final int MODEM_MASK_WCDMA = 0x04;
    public static final int MODEM_MASK_TDSCDMA = 0x08;

    public static final class NumberInfo {
        public String mCountryCode;
        public String mAreaCode;
        public String mSubscriber;
        public String mAreaCodePrefix;

        public NumberInfo(String countryCode, String areaCode, String subscriber, String areaCodePrefix) {
            mCountryCode = countryCode;
            mAreaCode = areaCode;
            mSubscriber = subscriber;
            mAreaCodePrefix = areaCodePrefix;
        }

        @Override
        public String toString() {
            return "country code = " + mCountryCode + ", area code = " + mAreaCode
                    + ", subscriber number = " + mSubscriber + ", area code prefix = " + mAreaCodePrefix;
        }
    }

    public static String getInitialNumber(Context context, Intent intent) {
        log("getInitialNumber(): " + intent);

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return null;
        }

        // If the EXTRA_ACTUAL_NUMBER_TO_DIAL extra is present, get the phone
        // number from there.  (That extra takes precedence over the actual data
        // included in the intent.)
        if (intent.hasExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL)) {
            String actualNumberToDial =
                intent.getStringExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL);
            return actualNumberToDial;
        }

        return getNumberFromIntent(context, intent);
    }

    private static String getNumberFromIntent(Context context, Intent intent) {
        Uri uri = intent.getData();
        String scheme = uri.getScheme();

        // The sip: scheme is simple: just treat the rest of the URI as a
        // SIP address.
        if (Constants.SCHEME_SIP.equals(scheme)) {
            return uri.getSchemeSpecificPart();
        }
        
        if (Constants.VOICEMAIL_URI.equals(intent.getData().toString())) {
            final long defaultSim = Settings.System.getLong(context.getContentResolver(),
                    Settings.System.VOICE_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            final SIMInfoWrapper simInfoWrapper = com.mediatek.phone.SIMInfoWrapper.getDefault();
            
            if (defaultSim > 0 && simInfoWrapper.getSlotIdBySimId((int)defaultSim) >= 0) {
                intent.putExtra("simId", simInfoWrapper.getSlotIdBySimId((int)defaultSim));
            }
        }
        
        log("getNumberFromIntent .....");

        // Otherwise, let PhoneNumberUtils.getNumberFromIntent() handle
        // the other cases (i.e. tel: and voicemail: and contact: URIs.)

        return PhoneNumberUtils.getNumberFromIntent(intent, context);
    }

    public static boolean isSimInsert(final Request request, final int slot) {
        if (null == request.getTelephonyInterface()) {
            return false;
        }
        boolean result = false;
        try {
            result = request.getTelephonyInterface().isSimInsert(slot);
        } catch (RemoteException e) {
            log("RemoteException happens in isSimInsert()");
            return false;
        }
        return result;
    }

    /**
     * Check the SIM's state in the slot. If the SIM is ready, return true.
     *
     * @param request
     * @param slot
     * @return
     */
    public static boolean isSimReady(final Request request, final int slot) {
        return getSimState(request, slot) == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * Gets a constant indicating the state of the device SIM card.
     *
     * <p>
     * @return Constant indicating the state of the device SIM card.
     * Constant may be one of the following items.
     * <p>
     * android.telephony.TelephonyManager.SIM_STATE_UNKNOWN
     * <p>
     * android.telephony.TelephonyManager.SIM_STATE_ABSENT
     * <p>
     * android.telephony.TelephonyManager.SIM_STATE_PIN_REQUIRED
     * <p>
     * android.telephony.TelephonyManager.SIM_STATE_PUK_REQUIRED
     * <p>
     * android.telephony.TelephonyManager.SIM_STATE_NETWORK_LOCKED
     * <p>
     * android.telephony.TelephonyManager.SIM_STATE_READY
     */
    public static int getSimState(final Request request, final int slot){
        int state = TelephonyManager.SIM_STATE_UNKNOWN;
        if (null == request.getTelephonyInterface()) {
            return state;
        }
        try {
            state = request.getTelephonyInterface().getSimState(slot);
        } catch (RemoteException e) {
            log("RemoteException happens in getSimState()");
        }
        return state;
    }

    public static boolean isRadioOn(final Request request, final int slot) {
        if (null == request.getTelephonyInterface()) {
            return false;
        }
        boolean result = false;
        try {
            if (request.isMultipleSim()) {
                request.getTelephonyInterface().isRadioOnGemini(slot);
            } else {
                request.getTelephonyInterface().isRadioOn();
            }
        } catch (android.os.RemoteException e) {
            log("RemoteException happens in isRadioOn()");
            return false;
        }
        return result;
    }

    public static int get3GCapabilitySIM(final Request request) {
        if (null == request.getTelephonyInterface()) {
            return 0;
        }
        try {
            return request.getTelephonyInterface().get3GCapabilitySIM();
        } catch (android.os.RemoteException e) {
            log("RemoteException happens in get3GCapabilitySIM()");
            return 0;
        }
    }

    /**
     * @return The ISO 3166-1 two letters country code of the country the user
     *         is in.
     */
    public static final String getCurrentCountryISO(Context context) {
        CountryDetector detector =
                (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
        return detector.detectCountry().getCountryIso();
    }

    public static NumberInfo getNumberInfo(String number, String countryISO) {
        log("getNumberInfo()..");
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneUtil.parse(number, countryISO);
        } catch (NumberParseException e) {
            log("NumberParseException happens");
            return new NumberInfo(countryISO, "", number, "");
        }
        if (null == phoneNumber) {
            log("phoneNumber is null");
            return new NumberInfo(countryISO, "", number, "");
        }
        String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(phoneNumber);
        String areaCode = "";
        String subscriberNumber = "";
        String areaCodePrefix = phoneNumber.getPossibleNationalPrefix();
        log("phone number = " + phoneNumber);
        log("nationalSignificantNumber = " + nationalSignificantNumber);
        int areaCodeLength = phoneUtil.getLengthOfGeographicalAreaCode(phoneNumber);
        if (areaCodeLength > 0) {
              areaCode = nationalSignificantNumber.substring(0, areaCodeLength);
              subscriberNumber = nationalSignificantNumber.substring(areaCodeLength);
              log("areaCode = " + areaCode);
              log("subscriberNumber = " + subscriberNumber);
        } else {
            subscriberNumber = nationalSignificantNumber;
        }
        return new NumberInfo(String.valueOf(phoneNumber.getCountryCode()), areaCode,
                                             subscriberNumber, areaCodePrefix);
    }

    public static String getCityGeoDescription(String number, String countryISO, Locale locale) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();

        PhoneNumber pn = null;
        try {
            log("parsing '" + number
                            + "' for countryIso '" + countryISO + "'...");
            pn = util.parse(number, countryISO);
            log("- parsed number: " + pn);
        } catch (NumberParseException e) {
            log("getGeoDescription: NumberParseException for incoming number '" + number + "'");
        }

        if (pn != null) {
            String description = geocoder.getDescriptionForNumber(pn, locale);
            log("- got description = " + description);
            return description;
        } else {
            return null;
        }
    }

    /*public static String getInternationalPrefix(String countryISO) {
        PhoneMetadata metadata = PhoneNumberUtil.getInstance().getMetadataForRegion(countryISO);
        if (null != metadata) {
            return metadata.getInternationalPrefix();
        } else {
            return null;
        }
    }*/

    public static boolean isValidNumberForCountryISO(Context context, String number, String countryISO) {
        boolean result = false;
        try {
            result = PhoneNumberUtil.getInstance().isValidNumberForRegion(
                            PhoneNumberUtil.getInstance().parse(number, countryISO), countryISO);
        } catch (NumberParseException e) {
            log("catch NumberParseException, exception info = " + e.toString());
        }
        log("isValidNumberForCountryISO(), PhoneNumberUtil.isValidNumberForRegion() result = " + result);
        if (!result) {
            int result2 = PhoneNumberUtilsEx.isValidNumber(countryISO, number);
            log("isValidNumberForCountryISO(), result2 = " + result2);
            switch (result2) {
                // For ECC case, should judge here, should judge it in the beginning of MO call
                case PhoneNumberUtilsEx.ID_VALID_ECC:
                case PhoneNumberUtilsEx.ID_INVALID:
                    result = false;
                    break;
                case PhoneNumberUtilsEx.ID_VALID:
                case PhoneNumberUtilsEx.ID_VALID_BUT_NEED_AREA_CODE:
                    result = true;
                    break;
                case PhoneNumberUtilsEx.ID_VALID_DOMESTIC_ONLY:
                    if (getCurrentCountryISO(context).equals(countryISO)) {
                        result = true;
                    } else {
                        result = false;
                    }
                    break;
                case PhoneNumberUtilsEx.ID_VALID_WHEN_CALL_EXIST:
                    result = false;
                    break;
                default:
                    result = false;
                    break;
            }
        }
        log("isValidNumberForCountryISO(), number = " + number
                + ", country ISO = " + countryISO + ", result = " + result);
        return result;
    }

    public static String queryIPPrefix(Context context, int slot, boolean isMultipleSim) {
        StringBuilder builder = new StringBuilder();
        builder.append("ipprefix");
        if (isMultipleSim) {
            final SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slot);
            builder.append(simInfo.mSimId);
        }
        final String key = builder.toString();
        final String ipPrefix = Settings.System.getString(context.getContentResolver(), key);
        log("queryIPPrefix, ipPrefix = " + ipPrefix);
        return ipPrefix;
    }

    /**
     * Get the slots support 3G
     * @return
     */
    public static List<Integer> get3GCapabilitySlots() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int slot : GeminiConstants.SLOTS) {
            if (get3GCapabilitySIMBySlot(slot)) {
                list.add(slot);
            }
        }
        return list;
    }

     public static boolean get3GCapabilitySIMBySlot(int slot) {
        int mask = 0;
        boolean modemIs3G = false;
        String propertyKey = "gsm.baseband.capability";
        String capability = null;
        if (slot == 1) {
            propertyKey += "2";
        }
        capability = SystemProperties.get(propertyKey);
        Log.d(TAG, "gsm.baseband.capability " + capability);
        if (capability == null || "".equals(capability)) {
            return false;
        }

        try {
            mask = Integer.valueOf(capability, 16);
            if (((mask & MODEM_MASK_TDSCDMA) == MODEM_MASK_TDSCDMA)
                    || ((mask & MODEM_MASK_WCDMA) == MODEM_MASK_WCDMA)) {
                modemIs3G = true;
            } else {
                modemIs3G = false;
            }
        } catch (NumberFormatException ne) {
            log("parse value of basband error");
        }
        return modemIs3G;        
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
