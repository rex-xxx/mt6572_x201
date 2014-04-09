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

package android.telephony;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.ShortNumberUtil;
import com.android.i18n.phonenumbers.Phonemetadata.PhoneMetadata;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_IDP_STRING;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// M : [mtk04070][111116][ALPS00093395]Use Gemini object @{
import static com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1;
import static com.android.internal.telephony.PhoneConstants.GEMINI_DEFAULT_SIM_PROP;
import static com.android.internal.telephony.PhoneConstants.GEMINI_SIM_ID_KEY;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IPhoneNumberExt;
import com.android.internal.telephony.PhoneConstants;
/// @}

import android.os.ServiceManager;
import com.android.internal.telephony.ITelephony;


/**
 * Various utilities for dealing with phone number strings.
 */
public class PhoneNumberUtils
{
    /*
     * Special characters
     *
     * (See "What is a phone number?" doc)
     * 'p' --- GSM pause character, same as comma
     * 'n' --- GSM wild character
     * 'w' --- GSM wait character
     */
    public static final char PAUSE = ',';
    public static final char WAIT = ';';
    public static final char WILD = 'N';

    /*
     * Calling Line Identification Restriction (CLIR)
     */
    private static final String CLIR_ON = "*31#";
    private static final String CLIR_OFF = "#31#";

    /*
     * TOA = TON + NPI
     * See TS 24.008 section 10.5.4.7 for details.
     * These are the only really useful TOA values
     */
    public static final int TOA_International = 0x91;
    public static final int TOA_Unknown = 0x81;

    static final String LOG_TAG = "PhoneNumberUtils";
    private static final boolean DBG = false;

    /*
     * global-phone-number = ["+"] 1*( DIGIT / written-sep )
     * written-sep         = ("-"/".")
     */
    private static final Pattern GLOBAL_PHONE_NUMBER_PATTERN =
            Pattern.compile("[\\+]?[0-9.-]+");
            
    /// M: Add for validity checking of International dialing. @{            
    public static final int ID_VALID_ECC = 1;
    public static final int ID_VALID_BUT_NEED_AREA_CODE = 2;
    public static final int ID_VALID = 3;
    public static final int ID_VALID_DOMESTIC_ONLY = 4;
    public static final int ID_INVALID = 5;
    public static final int ID_VALID_WHEN_CALL_EXIST = 6;
    /// @}
            
    private static IPhoneNumberExt mPhoneNumberExt;
    static {
        try{
            mPhoneNumberExt = MediatekClassFactory.createInstance(IPhoneNumberExt.class);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /** DFO - CTA feature option value */
    private static final boolean MTK_CTA_SUPPORT_FLAG = FeatureOption.MTK_CTA_SUPPORT;

    /** True if c is ISO-LATIN characters 0-9 */
    public static boolean
    isISODigit (char c) {
        return c >= '0' && c <= '9';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # */
    public final static boolean
    is12Key(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD  */
    public final static boolean
    isDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD;
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , + (no WILD)  */
    public final static boolean
    isReallyDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD, WAIT, PAUSE   */
    public final static boolean
    isNonSeparator(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+'
                || c == WILD || c == WAIT || c == PAUSE;
    }

    /** This any anything to the right of this char is part of the
     *  post-dial string (eg this is PAUSE or WAIT)
     */
    public final static boolean
    isStartsPostDial (char c) {
        return c == PAUSE || c == WAIT;
    }

    private static boolean
    isPause (char c){
        return c == 'p'||c == 'P';
    }

    private static boolean
    isToneWait (char c){
        return c == 'w'||c == 'W';
    }


    /** Returns true if ch is not dialable or alpha char */
    private static boolean isSeparator(char ch) {
        return !isDialable(ch) && !(('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z'));
    }

    /** Extracts the phone number from an Intent.
     *
     * @param intent the intent to get the number of
     * @param context a context to use for database access
     *
     * @return the phone number that would be called by the intent, or
     *         <code>null</code> if the number cannot be found.
     */
    public static String getNumberFromIntent(Intent intent, Context context) {
        String number = null;

        Uri uri = intent.getData();
        String scheme = uri.getScheme();

        if (scheme.equals("tel") || scheme.equals("sip")) {
            return uri.getSchemeSpecificPart();
        }

        /// M: [mtk04070][111116][ALPS00093395]Return voice mail number according to sim id. @{
        // TODO: We don't check for SecurityException here (requires
        // READ_PHONE_STATE permission).
        if (scheme.equals("voicemail")) {
            int simId = SystemProperties.getInt(
                GEMINI_DEFAULT_SIM_PROP, 
                GEMINI_SIM_1);
            simId = intent.getIntExtra(GEMINI_SIM_ID_KEY, simId);
            
            try {
               ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
               return iTel.getVoiceMailNumber(simId);
            } catch (Exception e) {
            }
        }
        /// @}

        if (context == null) {
            return null;
        }

        String type = intent.resolveType(context);
        String phoneColumn = null;

        // Correctly read out the phone entry based on requested provider
        final String authority = uri.getAuthority();
        if (Contacts.AUTHORITY.equals(authority)) {
            phoneColumn = Contacts.People.Phones.NUMBER;
        } else if (ContactsContract.AUTHORITY.equals(authority)) {
            phoneColumn = ContactsContract.CommonDataKinds.Phone.NUMBER;
        }

        final Cursor c = context.getContentResolver().query(uri, new String[] {
            phoneColumn
        }, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    number = c.getString(c.getColumnIndex(phoneColumn));
                }
            } finally {
                c.close();
            }
        }

        return number;
    }

    /** Extracts the network address portion and canonicalizes
     *  (filters out separators.)
     *  Network address portion is everything up to DTMF control digit
     *  separators (pause or wait), but without non-dialable characters.
     *
     *  Please note that the GSM wild character is allowed in the result.
     *  This must be resolved before dialing.
     *
     *  Returns null if phoneNumber == null
     */
    public static String
    extractNetworkPortion(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (fullwidth, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                ret.append(digit);
            } else if (c == '+') {
                // Allow '+' as first character or after CLIR MMI prefix
                String prefix = ret.toString();
                if (prefix.length() == 0 || prefix.equals(CLIR_ON) || prefix.equals(CLIR_OFF)) {
                    ret.append(c);
                }
            } else if (isDialable(c)) {
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        return ret.toString();
    }

    /**
     * Extracts the network address portion and canonicalize.
     *
     * This function is equivalent to extractNetworkPortion(), except
     * for allowing the PLUS character to occur at arbitrary positions
     * in the address portion, not just the first position.
     *
     * @hide
     */
    public static String extractNetworkPortionAlt(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);
        boolean haveSeenPlus = false;

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (c == '+') {
                if (haveSeenPlus) {
                    continue;
                }
                haveSeenPlus = true;
            }
            if (isDialable(c)) {
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        Log.d(LOG_TAG, "[extractNetworkPortionAlt] phoneNumber: " + ret.toString());

        return ret.toString();
    }

    /**
     * Strips separators from a phone number string.
     * @param phoneNumber phone number to strip.
     * @return phone string stripped of separators.
     */
    public static String stripSeparators(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (fullwidth, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            /// M: Regard 'P' and 'W' as separators. @{
            if (digit != -1) {
                ret.append(digit);
            } else if (isNonSeparator(c) || isPause(c) || isToneWait(c)) {
                /* For Change request - [ALPS00252712]To support 'P' and 'W', mtk04070, 20120326 */
                ret.append(c);
            }
            /// @}
        }

        return ret.toString();
    }

    /**
     * Translates keypad letters to actual digits (e.g. 1-800-GOOG-411 will
     * become 1-800-4664-411), and then strips all separators (e.g. 1-800-4664-411 will become
     * 18004664411).
     *
     * @see #convertKeypadLettersToDigits(String)
     * @see #stripSeparators(String)
     *
     * @hide
     */
    public static String convertAndStrip(String phoneNumber) {
        return stripSeparators(convertKeypadLettersToDigits(phoneNumber));
    }

    /**
     * Converts pause and tonewait pause characters
     * to Android representation.
     * RFC 3601 says pause is 'p' and tonewait is 'w'.
     * @hide
     */
    public static String convertPreDial(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);

            if (isPause(c)) {
                c = PAUSE;
            } else if (isToneWait(c)) {
                c = WAIT;
            }
            ret.append(c);
        }
        return ret.toString();
    }

    /** or -1 if both are negative */
    static private int
    minPositive (int a, int b) {
        if (a >= 0 && b >= 0) {
            return (a < b) ? a : b;
        } else if (a >= 0) { /* && b < 0 */
            return a;
        } else if (b >= 0) { /* && a < 0 */
            return b;
        } else { /* a < 0 && b < 0 */
            return -1;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
    /** index of the last character of the network portion
     *  (eg anything after is a post-dial string)
     */
    static private int
    indexOfLastNetworkChar(String a) {
        int pIndex, wIndex;
        int origLength;
        int trimIndex;

        origLength = a.length();

        pIndex = a.indexOf(PAUSE);
        wIndex = a.indexOf(WAIT);

        trimIndex = minPositive(pIndex, wIndex);

        if (trimIndex < 0) {
            return origLength - 1;
        } else {
            return trimIndex - 1;
        }
    }

    /**
     * Extracts the post-dial sequence of DTMF control digits, pauses, and
     * waits. Strips separators. This string may be empty, but will not be null
     * unless phoneNumber == null.
     *
     * Returns null if phoneNumber == null
     */

    public static String
    extractPostDialPortion(String phoneNumber) {
        if (phoneNumber == null) return null;

        int trimIndex;
        StringBuilder ret = new StringBuilder();

        trimIndex = indexOfLastNetworkChar (phoneNumber);

        for (int i = trimIndex + 1, s = phoneNumber.length()
                ; i < s; i++
        ) {
            char c = phoneNumber.charAt(i);
            if (isNonSeparator(c)) {
                ret.append(c);
            }
        }

        return ret.toString();
    }

    /**
     * Compare phone numbers a and b, return true if they're identical enough for caller ID purposes.
     */
    public static boolean compare(String a, String b) {
        // We've used loose comparation at least Eclair, which may change in the future.

        return compare(a, b, false);
    }

    /**
     * Compare phone numbers a and b, and return true if they're identical
     * enough for caller ID purposes. Checks a resource to determine whether
     * to use a strict or loose comparison algorithm.
     */
    public static boolean compare(Context context, String a, String b) {
        boolean useStrict = context.getResources().getBoolean(
               com.android.internal.R.bool.config_use_strict_phone_number_comparation);
        return compare(a, b, useStrict);
    }

    /**
     * @hide only for testing.
     */
    public static boolean compare(String a, String b, boolean useStrictComparation) {
        return (useStrictComparation ? compareStrictly(a, b) : compareLoosely(a, b));
    }

    /**
     * Compare phone numbers a and b, return true if they're identical
     * enough for caller ID purposes.
     *
     * - Compares from right to left
     * - requires MIN_MATCH (7) characters to match
     * - handles common trunk prefixes and international prefixes
     *   (basically, everything except the Russian trunk prefix)
     *
     * Note that this method does not return false even when the two phone numbers
     * are not exactly same; rather; we can call this method "similar()", not "equals()".
     *
     * @hide
     */
    public static boolean
    compareLoosely(String a, String b) {
        int ia, ib;
        int matched;
        int numNonDialableCharsInA = 0;
        int numNonDialableCharsInB = 0;

        if (a == null || b == null) return a == b;

        if (a.length() == 0 || b.length() == 0) {
            return false;
        }

        ia = indexOfLastNetworkChar (a);
        ib = indexOfLastNetworkChar (b);
        matched = 0;

        while (ia >= 0 && ib >=0) {
            char ca, cb;
            boolean skipCmp = false;

            ca = a.charAt(ia);

            if (!isDialable(ca)) {
                ia--;
                skipCmp = true;
                numNonDialableCharsInA++;
            }

            cb = b.charAt(ib);

            if (!isDialable(cb)) {
                ib--;
                skipCmp = true;
                numNonDialableCharsInB++;
            }

            if (!skipCmp) {
                if (cb != ca && ca != WILD && cb != WILD) {
                    break;
                }
                ia--; ib--; matched++;
            }
        }

        /// M: [mtk04070][111116][ALPS00093395]Determine  the minimum match length acording to feature option. @{
        int minMatchLen = 0;
        if (MTK_CTA_SUPPORT_FLAG) {
            minMatchLen = MIN_MATCH_CTA;
        } else {
            minMatchLen = MIN_MATCH;
        }
        if (matched < minMatchLen) {
            int effectiveALen = a.length() - numNonDialableCharsInA;
            int effectiveBLen = b.length() - numNonDialableCharsInB;


            // if the number of dialable chars in a and b match, but the matched chars < MIN_MATCH,
            // treat them as equal (i.e. 404-04 and 40404)
            if (effectiveALen == effectiveBLen && effectiveALen == matched) {
                return true;
            }

            return false;
        }

        // At least one string has matched completely;
        if (matched >= minMatchLen && (ia < 0 || ib < 0)) {
            return true;
        }
        /// @}

        /*
         * Now, what remains must be one of the following for a
         * match:
         *
         *  - a '+' on one and a '00' or a '011' on the other
         *  - a '0' on one and a (+,00)<country code> on the other
         *     (for this, a '0' and a '00' prefix would have succeeded above)
         */

        if (matchIntlPrefix(a, ia + 1)
            && matchIntlPrefix (b, ib +1)
        ) {
            return true;
        }

        if (matchTrunkPrefix(a, ia + 1)
            && matchIntlPrefixAndCC(b, ib +1)
        ) {
            return true;
        }

        if (matchTrunkPrefix(b, ib + 1)
            && matchIntlPrefixAndCC(a, ia +1)
        ) {
            return true;
        }

        return false;
    }

    /**
     * @hide
     */
    public static boolean
    compareStrictly(String a, String b) {
        return compareStrictly(a, b, true);
    }

    /**
     * @hide
     */
    public static boolean
    compareStrictly(String a, String b, boolean acceptInvalidCCCPrefix) {
        if (a == null || b == null) {
            return a == b;
        } else if (a.length() == 0 && b.length() == 0) {
            return false;
        }

        int forwardIndexA = 0;
        int forwardIndexB = 0;

        CountryCallingCodeAndNewIndex cccA =
            tryGetCountryCallingCodeAndNewIndex(a, acceptInvalidCCCPrefix);
        CountryCallingCodeAndNewIndex cccB =
            tryGetCountryCallingCodeAndNewIndex(b, acceptInvalidCCCPrefix);
        boolean bothHasCountryCallingCode = false;
        boolean okToIgnorePrefix = true;
        boolean trunkPrefixIsOmittedA = false;
        boolean trunkPrefixIsOmittedB = false;
        if (cccA != null && cccB != null) {
            if (cccA.countryCallingCode != cccB.countryCallingCode) {
                // Different Country Calling Code. Must be different phone number.
                return false;
            }
            // When both have ccc, do not ignore trunk prefix. Without this,
            // "+81123123" becomes same as "+810123123" (+81 == Japan)
            okToIgnorePrefix = false;
            bothHasCountryCallingCode = true;
            forwardIndexA = cccA.newIndex;
            forwardIndexB = cccB.newIndex;
        } else if (cccA == null && cccB == null) {
            // When both do not have ccc, do not ignore trunk prefix. Without this,
            // "123123" becomes same as "0123123"
            okToIgnorePrefix = false;
        } else {
            if (cccA != null) {
                forwardIndexA = cccA.newIndex;
            } else {
                int tmp = tryGetTrunkPrefixOmittedIndex(b, 0);
                if (tmp >= 0) {
                    forwardIndexA = tmp;
                    trunkPrefixIsOmittedA = true;
                }
            }
            if (cccB != null) {
                forwardIndexB = cccB.newIndex;
            } else {
                int tmp = tryGetTrunkPrefixOmittedIndex(b, 0);
                if (tmp >= 0) {
                    forwardIndexB = tmp;
                    trunkPrefixIsOmittedB = true;
                }
            }
        }

        int backwardIndexA = a.length() - 1;
        int backwardIndexB = b.length() - 1;
        while (backwardIndexA >= forwardIndexA && backwardIndexB >= forwardIndexB) {
            boolean skip_compare = false;
            final char chA = a.charAt(backwardIndexA);
            final char chB = b.charAt(backwardIndexB);
            if (isSeparator(chA)) {
                backwardIndexA--;
                skip_compare = true;
            }
            if (isSeparator(chB)) {
                backwardIndexB--;
                skip_compare = true;
            }

            if (!skip_compare) {
                if (chA != chB) {
                    return false;
                }
                backwardIndexA--;
                backwardIndexB--;
            }
        }

        if (okToIgnorePrefix) {
            if ((trunkPrefixIsOmittedA && forwardIndexA <= backwardIndexA) ||
                !checkPrefixIsIgnorable(a, forwardIndexA, backwardIndexA)) {
                if (acceptInvalidCCCPrefix) {
                    // Maybe the code handling the special case for Thailand makes the
                    // result garbled, so disable the code and try again.
                    // e.g. "16610001234" must equal to "6610001234", but with
                    //      Thailand-case handling code, they become equal to each other.
                    //
                    // Note: we select simplicity rather than adding some complicated
                    //       logic here for performance(like "checking whether remaining
                    //       numbers are just 66 or not"), assuming inputs are small
                    //       enough.
                    return compare(a, b, false);
                } else {
                    return false;
                }
            }
            if ((trunkPrefixIsOmittedB && forwardIndexB <= backwardIndexB) ||
                !checkPrefixIsIgnorable(b, forwardIndexA, backwardIndexB)) {
                if (acceptInvalidCCCPrefix) {
                    return compare(a, b, false);
                } else {
                    return false;
                }
            }
        } else {
            // In the US, 1-650-555-1234 must be equal to 650-555-1234,
            // while 090-1234-1234 must not be equal to 90-1234-1234 in Japan.
            // This request exists just in US (with 1 trunk (NDD) prefix).
            // In addition, "011 11 7005554141" must not equal to "+17005554141",
            // while "011 1 7005554141" must equal to "+17005554141"
            //
            // In this comparison, we ignore the prefix '1' just once, when
            // - at least either does not have CCC, or
            // - the remaining non-separator number is 1
            boolean maybeNamp = !bothHasCountryCallingCode;
            while (backwardIndexA >= forwardIndexA) {
                final char chA = a.charAt(backwardIndexA);
                if (isDialable(chA)) {
                    if (maybeNamp && tryGetISODigit(chA) == 1) {
                        maybeNamp = false;
                    } else {
                        return false;
                    }
                }
                backwardIndexA--;
            }
            while (backwardIndexB >= forwardIndexB) {
                final char chB = b.charAt(backwardIndexB);
                if (isDialable(chB)) {
                    if (maybeNamp && tryGetISODigit(chB) == 1) {
                        maybeNamp = false;
                    } else {
                        return false;
                    }
                }
                backwardIndexB--;
            }
        }

        return true;
    }

    /**
     * Returns the rightmost MIN_MATCH (5) characters in the network portion
     * in *reversed* order
     *
     * This can be used to do a database lookup against the column
     * that stores getStrippedReversed()
     *
     * Returns null if phoneNumber == null
     */
    public static String
    toCallerIDMinMatch(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);

        /// M: [mtk04070][111116][ALPS00093395]Determine  the minimum match length acording to feature option. @{
        int minMatchLen = 0;
        if (MTK_CTA_SUPPORT_FLAG) {
            minMatchLen = MIN_MATCH_CTA;
        } else {
            minMatchLen = MIN_MATCH;
        }
        return internalGetStrippedReversed(np, minMatchLen);
        /// @}
    }

    /**
     * Returns the network portion reversed.
     * This string is intended to go into an index column for a
     * database lookup.
     *
     * Returns null if phoneNumber == null
     */
    public static String
    getStrippedReversed(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);

        if (np == null) return null;

        return internalGetStrippedReversed(np, np.length());
    }

    /**
     * Returns the last numDigits of the reversed phone number
     * Returns null if np == null
     */
    private static String
    internalGetStrippedReversed(String np, int numDigits) {
        if (np == null) return null;

        StringBuilder ret = new StringBuilder(numDigits);
        int length = np.length();

        for (int i = length - 1, s = length
            ; i >= 0 && (s - i) <= numDigits ; i--
        ) {
            char c = np.charAt(i);

            ret.append(c);
        }

        return ret.toString();
    }

    /**
     * Basically: makes sure there's a + in front of a
     * TOA_International number
     *
     * Returns null if s == null
     */
    public static String
    stringFromStringAndTOA(String s, int TOA) {
        if (s == null) return null;

        if (TOA == TOA_International && s.length() > 0 && s.charAt(0) != '+') {
            return "+" + s;
        }

        return s;
    }

    /**
     * Returns the TOA for the given dial string
     * Basically, returns TOA_International if there's a + prefix
     */

    public static int
    toaFromString(String s) {
        if (s != null && s.length() > 0 && s.charAt(0) == '+') {
            return TOA_International;
        }

        return TOA_Unknown;
    }

    /**
     *  3GPP TS 24.008 10.5.4.7
     *  Called Party BCD Number
     *
     *  See Also TS 51.011 10.5.1 "dialing number/ssc string"
     *  and TS 11.11 "10.3.1 EF adn (Abbreviated dialing numbers)"
     *
     * @param bytes the data buffer
     * @param offset should point to the TOA (aka. TON/NPI) octet after the length byte
     * @param length is the number of bytes including TOA byte
     *                and must be at least 2
     *
     * @return partial string on invalid decode
     *
     * FIXME(mkf) support alphanumeric address type
     *  currently implemented in SMSMessage.getAddress()
     */
    public static String
    calledPartyBCDToString (byte[] bytes, int offset, int length) {
        boolean prependPlus = false;
        StringBuilder ret = new StringBuilder(1 + length * 2);

        if (length < 2) {
            return "";
        }

        //Only TON field should be taken in consideration
        if ((bytes[offset] & 0xf0) == (TOA_International & 0xf0)) {
            prependPlus = true;
        }

        internalCalledPartyBCDFragmentToString(
                ret, bytes, offset + 1, length - 1);

        if (prependPlus && ret.length() == 0) {
            // If the only thing there is a prepended plus, return ""
            return "";
        }

        if (prependPlus) {
            /// M: [mtk04070][111116][ALPS00093395]Replace origin codes with prependPlusToNumber method. @{
            ret = new StringBuilder(mPhoneNumberExt.prependPlusToNumber(ret.toString()));
            /// @}
        }

        return ret.toString();
    }

    private static void
    internalCalledPartyBCDFragmentToString(
        StringBuilder sb, byte [] bytes, int offset, int length) {
        for (int i = offset ; i < length + offset ; i++) {
            byte b;
            char c;

            c = bcdToChar((byte)(bytes[i] & 0xf));

            if (c == 0) {
                return;
            }
            sb.append(c);

            // FIXME(mkf) TS 23.040 9.1.2.3 says
            // "if a mobile receives 1111 in a position prior to
            // the last semi-octet then processing shall commence with
            // the next semi-octet and the intervening
            // semi-octet shall be ignored"
            // How does this jive with 24.008 10.5.4.7

            b = (byte)((bytes[i] >> 4) & 0xf);

            if (b == 0xf && i + 1 == length + offset) {
                //ignore final 0xf
                break;
            }

            c = bcdToChar(b);
            if (c == 0) {
                return;
            }

            sb.append(c);
        }

    }

    /**
     * Like calledPartyBCDToString, but field does not start with a
     * TOA byte. For example: SIM ADN extension fields
     */

    public static String
    calledPartyBCDFragmentToString(byte [] bytes, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);

        internalCalledPartyBCDFragmentToString(ret, bytes, offset, length);

        return ret.toString();
    }

    /** returns 0 on invalid value */
    private static char
    bcdToChar(byte b) {
        if (b < 0xa) {
            return (char)('0' + b);
        } else switch (b) {
            case 0xa: return '*';
            case 0xb: return '#';
            case 0xc: return PAUSE;
            case 0xd: return WILD;
            /// M: add wait for ANR @{
            case 0xe: return WAIT;
            /// @}
            default: return 0;
        }
    }

    private static int
    charToBCD(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c == '*') {
            return 0xa;
        } else if (c == '#') {
            return 0xb;
        } else if (c == PAUSE) {
            return 0xc;
        } else if (c == WILD) {
            return 0xd;
        /// M: add wait for ANR @{
        } else if (c == WAIT) {
            return 0xe;
        /// @}
        } else {
            throw new RuntimeException ("invalid char for BCD " + c);
        }
    }

    /**
     * Return true iff the network portion of <code>address</code> is,
     * as far as we can tell on the device, suitable for use as an SMS
     * destination address.
     */
    public static boolean isWellFormedSmsAddress(String address) {
        /// M: [mtk04070][120104][ALPS00109412]Solve "can't send MMS with MSISDN in international format". @{
        //Merge from ALPS00089029
        //if (!isDialable(address)) {
        //    return false;
        //}
        /// @}

        String networkPortion =
                PhoneNumberUtils.extractNetworkPortion(address);

        return (!(networkPortion.equals("+")
                  || TextUtils.isEmpty(networkPortion)))
               && isDialable(networkPortion);
    }

    public static boolean isGlobalPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }

        Matcher match = GLOBAL_PHONE_NUMBER_PATTERN.matcher(phoneNumber);
        return match.matches();
    }

    private static boolean isDialable(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isDialable(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNonSeparator(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isNonSeparator(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    /**
     * Note: calls extractNetworkPortion(), so do not use for
     * SIM EF[ADN] style records
     *
     * Returns null if network portion is empty.
     */
    public static byte[]
    networkPortionToCalledPartyBCD(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(networkPortion, false);
    }

    /**
     * Same as {@link #networkPortionToCalledPartyBCD}, but includes a
     * one-byte length prefix.
     */
    public static byte[]
    networkPortionToCalledPartyBCDWithLength(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(networkPortion, true);
    }

    /**
     * Convert a dialing number to BCD byte array
     *
     * @param number dialing number string
     *        if the dialing number starts with '+', set to international TOA
     * @return BCD byte array
     */
    public static byte[]
    numberToCalledPartyBCD(String number) {
        return numberToCalledPartyBCDHelper(number, false);
    }

    /**
     * If includeLength is true, prepend a one-byte length value to
     * the return array.
     */
    private static byte[]
    numberToCalledPartyBCDHelper(String number, boolean includeLength) {
        int numberLenReal = number.length();
        int numberLenEffective = numberLenReal;
        boolean hasPlus = number.indexOf('+') != -1;
        if (hasPlus) numberLenEffective--;

        if (numberLenEffective == 0) return null;

        int resultLen = (numberLenEffective + 1) / 2;  // Encoded numbers require only 4 bits each.
        int extraBytes = 1;                            // Prepended TOA byte.
        if (includeLength) extraBytes++;               // Optional prepended length byte.
        resultLen += extraBytes;

        byte[] result = new byte[resultLen];

        int digitCount = 0;
        for (int i = 0; i < numberLenReal; i++) {
            char c = number.charAt(i);
            if (c == '+') continue;
            int shift = ((digitCount & 0x01) == 1) ? 4 : 0;
            result[extraBytes + (digitCount >> 1)] |= (byte)((charToBCD(c) & 0x0F) << shift);
            digitCount++;
        }

        // 1-fill any trailing odd nibble/quartet.
        if ((digitCount & 0x01) == 1) result[extraBytes + (digitCount >> 1)] |= 0xF0;

        int offset = 0;
        if (includeLength) result[offset++] = (byte)(resultLen - 1);
        result[offset] = (byte)(hasPlus ? TOA_International : TOA_Unknown);

        return result;
    }

    //================ Number formatting =========================

    /** The current locale is unknown, look for a country code or don't format */
    public static final int FORMAT_UNKNOWN = 0;
    /** NANP formatting */
    public static final int FORMAT_NANP = 1;
    /** Japanese formatting */
    public static final int FORMAT_JAPAN = 2;

    /** List of country codes for countries that use the NANP */
    private static final String[] NANP_COUNTRIES = new String[] {
        "US", // United States
        "CA", // Canada
        "AS", // American Samoa
        "AI", // Anguilla
        "AG", // Antigua and Barbuda
        "BS", // Bahamas
        "BB", // Barbados
        "BM", // Bermuda
        "VG", // British Virgin Islands
        "KY", // Cayman Islands
        "DM", // Dominica
        "DO", // Dominican Republic
        "GD", // Grenada
        "GU", // Guam
        "JM", // Jamaica
        "PR", // Puerto Rico
        "MS", // Montserrat
        "MP", // Northern Mariana Islands
        "KN", // Saint Kitts and Nevis
        "LC", // Saint Lucia
        "VC", // Saint Vincent and the Grenadines
        "TT", // Trinidad and Tobago
        "TC", // Turks and Caicos Islands
        "VI", // U.S. Virgin Islands
    };

    /**
     * Breaks the given number down and formats it according to the rules
     * for the country the number is from.
     *
     * @param source The phone number to format
     * @return A locally acceptable formatting of the input, or the raw input if
     *  formatting rules aren't known for the number
     */
    public static String formatNumber(String source) {
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        formatNumber(text, getFormatTypeForLocale(Locale.getDefault()));
        return text.toString();
    }

    /**
     * Formats the given number with the given formatting type. Currently
     * {@link #FORMAT_NANP} and {@link #FORMAT_JAPAN} are supported as a formating type.
     *
     * @param source the phone number to format
     * @param defaultFormattingType The default formatting rules to apply if the number does
     * not begin with +[country_code]
     * @return The phone number formatted with the given formatting type.
     *
     * @hide TODO: Should be unhidden.
     */
    public static String formatNumber(String source, int defaultFormattingType) {
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        formatNumber(text, defaultFormattingType);
        return text.toString();
    }

    /**
     * Returns the phone number formatting type for the given locale.
     *
     * @param locale The locale of interest, usually {@link Locale#getDefault()}
     * @return The formatting type for the given locale, or FORMAT_UNKNOWN if the formatting
     * rules are not known for the given locale
     */
    public static int getFormatTypeForLocale(Locale locale) {
        String country = locale.getCountry();

        return getFormatTypeFromCountryCode(country);
    }

    /**
     * Formats a phone number in-place. Currently {@link #FORMAT_JAPAN} and {@link #FORMAT_NANP}
     * is supported as a second argument.
     *
     * @param text The number to be formatted, will be modified with the formatting
     * @param defaultFormattingType The default formatting rules to apply if the number does
     * not begin with +[country_code]
     */
    public static void formatNumber(Editable text, int defaultFormattingType) {
        int formatType = defaultFormattingType;

        if (text.length() > 2 && text.charAt(0) == '+') {
            if (text.charAt(1) == '1') {
                formatType = FORMAT_NANP;
            } else if (text.length() >= 3 && text.charAt(1) == '8'
                && text.charAt(2) == '1') {
                formatType = FORMAT_JAPAN;
            } else {
                formatType = FORMAT_UNKNOWN;
            }
        }

        switch (formatType) {
            case FORMAT_NANP:
                formatNanpNumber(text);
                return;
            case FORMAT_JAPAN:
                formatJapaneseNumber(text);
                return;
            case FORMAT_UNKNOWN:
                removeDashes(text);
                return;
        }
    }

    private static final int NANP_STATE_DIGIT = 1;
    private static final int NANP_STATE_PLUS = 2;
    private static final int NANP_STATE_ONE = 3;
    private static final int NANP_STATE_DASH = 4;

    /**
     * Formats a phone number in-place using the NANP formatting rules. Numbers will be formatted
     * as:
     *
     * <p><code>
     * xxxxx
     * xxx-xxxx
     * xxx-xxx-xxxx
     * 1-xxx-xxx-xxxx
     * +1-xxx-xxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with the formatting
     */
    public static void formatNanpNumber(Editable text) {
        int length = text.length();
        if (length > "+1-nnn-nnn-nnnn".length()) {
            // The string is too long to be formatted
            return;
        } else if (length <= 5) {
            // The string is either a shortcode or too short to be formatted
            return;
        }

        CharSequence saved = text.subSequence(0, length);

        // Strip the dashes first, as we're going to add them back
        removeDashes(text);
        length = text.length();

        // When scanning the number we record where dashes need to be added,
        // if they're non-0 at the end of the scan the dashes will be added in
        // the proper places.
        int dashPositions[] = new int[3];
        int numDashes = 0;

        int state = NANP_STATE_DIGIT;
        int numDigits = 0;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '1':
                    if (numDigits == 0 || state == NANP_STATE_PLUS) {
                        state = NANP_STATE_ONE;
                        break;
                    }
                    // fall through
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                    if (state == NANP_STATE_PLUS) {
                        // Only NANP number supported for now
                        text.replace(0, length, saved);
                        return;
                    } else if (state == NANP_STATE_ONE) {
                        // Found either +1 or 1, follow it up with a dash
                        dashPositions[numDashes++] = i;
                    } else if (state != NANP_STATE_DASH && (numDigits == 3 || numDigits == 6)) {
                        // Found a digit that should be after a dash that isn't
                        dashPositions[numDashes++] = i;
                    }
                    state = NANP_STATE_DIGIT;
                    numDigits++;
                    break;

                case '-':
                    state = NANP_STATE_DASH;
                    break;

                case '+':
                    if (i == 0) {
                        // Plus is only allowed as the first character
                        state = NANP_STATE_PLUS;
                        break;
                    }
                    // Fall through
                default:
                    // Unknown character, bail on formatting
                    text.replace(0, length, saved);
                    return;
            }
        }

        if (numDigits == 7) {
            // With 7 digits we want xxx-xxxx, not xxx-xxx-x
            numDashes--;
        }

        // Actually put the dashes in place
        for (int i = 0; i < numDashes; i++) {
            int pos = dashPositions[i];
            text.replace(pos + i, pos + i, "-");
        }

        // Remove trailing dashes
        int len = text.length();
        while (len > 0) {
            if (text.charAt(len - 1) == '-') {
                text.delete(len - 1, len);
                len--;
            } else {
                break;
            }
        }
    }

    /**
     * Formats a phone number in-place using the Japanese formatting rules.
     * Numbers will be formatted as:
     *
     * <p><code>
     * 03-xxxx-xxxx
     * 090-xxxx-xxxx
     * 0120-xxx-xxx
     * +81-3-xxxx-xxxx
     * +81-90-xxxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with
     * the formatting
     */
    public static void formatJapaneseNumber(Editable text) {
        JapanesePhoneNumberFormatter.format(text);
    }

    /**
     * Removes all dashes from the number.
     *
     * @param text the number to clear from dashes
     */
    private static void removeDashes(Editable text) {
        int p = 0;
        while (p < text.length()) {
            if (text.charAt(p) == '-') {
                text.delete(p, p + 1);
           } else {
                p++;
           }
        }
    }

    /**
     * Format the given phoneNumber to the E.164 representation.
     * <p>
     * The given phone number must have an area code and could have a country
     * code.
     * <p>
     * The defaultCountryIso is used to validate the given number and generate
     * the E.164 phone number if the given number doesn't have a country code.
     *
     * @param phoneNumber
     *            the phone number to format
     * @param defaultCountryIso
     *            the ISO 3166-1 two letters country code
     * @return the E.164 representation, or null if the given phone number is
     *         not valid.
     *
     * @hide
     */
    public static String formatNumberToE164(String phoneNumber, String defaultCountryIso) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        String result = null;
        try {
            PhoneNumber pn = util.parse(extractNetworkPortion(phoneNumber), defaultCountryIso);
            if (util.isValidNumber(pn)) {
                result = util.format(pn, PhoneNumberFormat.E164);
            }
        } catch (NumberParseException e) {
        }
        return result;
    }

    /**
     * Format a phone number.
     * <p>
     * If the given number doesn't have the country code, the phone will be
     * formatted to the default country's convention.
     *
     * @param phoneNumber
     *            the number to be formatted.
     * @param defaultCountryIso
     *            the ISO 3166-1 two letters country code whose convention will
     *            be used if the given number doesn't have the country code.
     * @return the formatted number, or null if the given number is not valid.
     *
     * @hide
     */
    public static String formatNumber(String phoneNumber, String defaultCountryIso) {
        // Do not attempt to format numbers that start with a hash or star symbol.
        if (phoneNumber.startsWith("#") || phoneNumber.startsWith("*")) {
            return phoneNumber;
        }

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        String result = null;
        try {
            PhoneNumber pn = util.parseAndKeepRawInput(phoneNumber, defaultCountryIso);
            result = util.formatInOriginalFormat(pn, defaultCountryIso);
        } catch (NumberParseException e) {
        }
        return result;
    }

    /**
     * Format the phone number only if the given number hasn't been formatted.
     * <p>
     * The number which has only dailable character is treated as not being
     * formatted.
     *
     * @param phoneNumber
     *            the number to be formatted.
     * @param phoneNumberE164
     *            the E164 format number whose country code is used if the given
     *            phoneNumber doesn't have the country code.
     * @param defaultCountryIso
     *            the ISO 3166-1 two letters country code whose convention will
     *            be used if the phoneNumberE164 is null or invalid, or if phoneNumber
     *            contains IDD.
     * @return the formatted number if the given number has been formatted,
     *            otherwise, return the given number.
     *
     * @hide
     */
    public static String formatNumber(
            String phoneNumber, String phoneNumberE164, String defaultCountryIso) {
        int len = phoneNumber.length();
        for (int i = 0; i < len; i++) {
            if (!isDialable(phoneNumber.charAt(i))) {
                return phoneNumber;
            }
        }
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        // Get the country code from phoneNumberE164
        if (phoneNumberE164 != null && phoneNumberE164.length() >= 2
                && phoneNumberE164.charAt(0) == '+') {
            try {
                // The number to be parsed is in E164 format, so the default region used doesn't
                // matter.
                PhoneNumber pn = util.parse(phoneNumberE164, "ZZ");
                String regionCode = util.getRegionCodeForNumber(pn);
                if (!TextUtils.isEmpty(regionCode) &&
                    // This makes sure phoneNumber doesn't contain an IDD
                    normalizeNumber(phoneNumber).indexOf(phoneNumberE164.substring(1)) <= 0) {
                    defaultCountryIso = regionCode;
                }
            } catch (NumberParseException e) {
            }
        }
        String result = formatNumber(phoneNumber, defaultCountryIso);
        return result != null ? result : phoneNumber;
    }

    /**
     * Normalize a phone number by removing the characters other than digits. If
     * the given number has keypad letters, the letters will be converted to
     * digits first.
     *
     * @param phoneNumber
     *            the number to be normalized.
     * @return the normalized number.
     *
     * @hide
     */
    public static String normalizeNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int len = phoneNumber.length();
        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (fullwidth, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                sb.append(digit);
            } else if (i == 0 && c == '+') {
                sb.append(c);
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return normalizeNumber(PhoneNumberUtils.convertKeypadLettersToDigits(phoneNumber));
            }
        }
        return sb.toString();
    }

    /**
     * Replace arabic/unicode digits with decimal digits.
     * @param number
     *            the number to be normalized.
     * @return the replaced number.
     *
     * @hide
     */
    public static String replaceUnicodeDigits(String number) {
        StringBuilder normalizedDigits = new StringBuilder(number.length());
        for (char c : number.toCharArray()) {
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                normalizedDigits.append(digit);
            } else {
                normalizedDigits.append(c);
            }
        }
        return normalizedDigits.toString();
    }

    // Three and four digit phone numbers for either special services,
    // or 3-6 digit addresses from the network (eg carrier-originated SMS messages) should
    // not match.
    //
    // This constant used to be 5, but SMS short codes has increased in length and
    // can be easily 6 digits now days. Most countries have SMS short code length between
    // 3 to 6 digits. The exceptions are
    //
    // Australia: Short codes are six or eight digits in length, starting with the prefix "19"
    //            followed by an additional four or six digits and two.
    // Czech Republic: Codes are seven digits in length for MO and five (not billed) or
    //            eight (billed) for MT direction
    //
    // see http://en.wikipedia.org/wiki/Short_code#Regional_differences for reference
    //
    // However, in order to loose match 650-555-1212 and 555-1212, we need to set the min match
    // to 7.
    static final int MIN_MATCH = 7;
    /// M: [mtk04070][111116][ALPS00093395]Add a constant integer. @{
    static final int MIN_MATCH_CTA = 11;
    /// @}

    /// M: [mtk04070][111116][ALPS00093395]Refine isEmergencyNumber method. @{
    /**
     * Checks a given number against the list of
     * emergency numbers provided by the RIL and SIM card.
     *
     * @param number the number to look up.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, otherwise return false.
     */
    public static boolean isEmergencyNumber(String number) {
        // Return true only if the specified number *exactly* matches
        // one of the emergency numbers listed by the RIL / SIM.
        //return isEmergencyNumberInternal(number, true /* useExactMatch */);

        Log.d(LOG_TAG, "[isEmergencyNumber] number: " + number);

        String plusNumber = null;
        String numberPlus = null;
        // If the number passed in is null, just return false:
        if (number == null) return false;

        // If the number passed in is a SIP address, return false, since the
        // concept of "emergency numbers" is only meaningful for calls placed
        // over the cell network.
        // (Be sure to do this check *before* calling extractNetworkPortionAlt(),
        // since the whole point of extractNetworkPortionAlt() is to filter out
        // any non-dialable characters (which would turn 'abc911def@example.com'
        // into '911', for example.))
        if (isUriNumber(number)) {
            return false;
        }

        // Strip the separators from the number before comparing it
        // to the list.
        number = extractNetworkPortionAlt(number);

        // retrieve the list of emergency numbers
        // check read-write ecclist property first
        // Read from SIM1
        String numbers = SystemProperties.get("ril.ecclist");
        Log.d(LOG_TAG, "[isEmergencyNumber] ril.ecclist: " + numbers);
        if (!TextUtils.isEmpty(numbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : numbers.split(",")) {
                numberPlus = emergencyNum + "+";
                if (emergencyNum.equals(number)
                     || numberPlus.equals(number)) {
                    Log.d(LOG_TAG, "[isEmergencyNumber] ril.ecclist: " + "return true");
                    return true;
                }
            }
        }

        // Read from SIM2
        numbers = SystemProperties.get("ril.ecclist2");
        Log.d(LOG_TAG, "[isEmergencyNumber] ril.ecclist2: " + numbers);
        if (!TextUtils.isEmpty(numbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : numbers.split(",")) {
                numberPlus = emergencyNum + "+";
                if (emergencyNum.equals(number)
                     || numberPlus.equals(number)) {
                    Log.d(LOG_TAG, "[isEmergencyNumber] ril.ecclist2: " + "return true");
                    return true;
                }
            }
        }

        // Read from SIM3
        numbers = SystemProperties.get("ril.ecclist3");
        if (!TextUtils.isEmpty(numbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : numbers.split(",")) {
                numberPlus = emergencyNum + "+";
                if (emergencyNum.equals(number)
                     || numberPlus.equals(number)) {
                    Log.d(LOG_TAG, "[isEmergencyNumber] ril.ecclist3: " + "return true");
                    return true;
                }
            }
        }

        // Read from SIM4
        numbers = SystemProperties.get("ril.ecclist4");
        if (!TextUtils.isEmpty(numbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : numbers.split(",")) {
                numberPlus = emergencyNum + "+";
                if (emergencyNum.equals(number)
                     || numberPlus.equals(number)) {
                    Log.d(LOG_TAG, "[isEmergencyNumber] ril.ecclist4: " + "return true");
                    return true;
                }
            }
        }

        // then read-only ecclist property since old RIL only uses this
        // If at least one SIM card exists, the fixed ecc number is written in ril_sim.c
        numbers = SystemProperties.get("ro.ril.ecclist");
        Log.d(LOG_TAG, "[isEmergencyNumber] ro.ril.ecclist: " + numbers);
        if (!TextUtils.isEmpty(numbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : numbers.split(",")) {
                numberPlus = emergencyNum + "+";
                if (emergencyNum.equals(number)
                     || numberPlus.equals(number)) {
                    Log.d(LOG_TAG, "[isEmergencyNumber] ro.ril.ecclist: " + "return true");
                    return true;
                }
            }
            // no matches found against the list!

            // Customized ecc number
            // MTK_OP01_PROTECT_START
            if (mPhoneNumberExt.isCustomizedEmergencyNumber(number, plusNumber, numberPlus))
            {
                return true;
            }    

            Log.d(LOG_TAG, "[isEmergencyNumber] ro.ril.ecclist: " + "return false");
            // no matches found in customized ecc number list
            return false;
        }

        //no ecclist system property(without SIM card), so use our own list.
        /* Modified by ALPS00005641 add 000,08,110,118,119,999 */
        final int eccNo = 8;
        String []emergencyNumList = {"112", "911", "000", "08", "110", "118", "119", "999"};
        for (int i = 0; i < eccNo; i++) {
            numberPlus = emergencyNumList[i] + "+";
            if (emergencyNumList[i].equals(number)
                || numberPlus.equals(number)) {
                Log.d(LOG_TAG, "[isEmergencyNumber] no ecclist: " + "return true");
                return true;
            }
        }

        // Customized emergency number when SIM card is not inserted or the screen is locked
        // Modified by [ALPS00419857]
        if (mPhoneNumberExt.isCustomizedEmergencyNumberExt(number, plusNumber, numberPlus)) {
            return true;
        }

        return false;
    }
    /// @}


    /**
     * Checks if given number might *potentially* result in
     * a call to an emergency service on the current network.
     *
     * Specifically, this method will return true if the specified number
     * is an emergency number according to the list managed by the RIL or
     * SIM, *or* if the specified number simply starts with the same
     * digits as any of the emergency numbers listed in the RIL / SIM.
     *
     * This method is intended for internal use by the phone app when
     * deciding whether to allow ACTION_CALL intents from 3rd party apps
     * (where we're required to *not* allow emergency calls to be placed.)
     *
     * @param number the number to look up.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, *or* if the number starts with the
     *         same digits as any of those emergency numbers.
     *
     * @hide
     */
    public static boolean isPotentialEmergencyNumber(String number) {
        // Check against the emergency numbers listed by the RIL / SIM,
        // and *don't* require an exact match.
        return isEmergencyNumberInternal(number, false /* useExactMatch */);
    }

    /**
     * Helper function for isEmergencyNumber(String) and
     * isPotentialEmergencyNumber(String).
     *
     * @param number the number to look up.
     *
     * @param useExactMatch if true, consider a number to be an emergency
     *           number only if it *exactly* matches a number listed in
     *           the RIL / SIM.  If false, a number is considered to be an
     *           emergency number if it simply starts with the same digits
     *           as any of the emergency numbers listed in the RIL / SIM.
     *           (Setting useExactMatch to false allows you to identify
     *           number that could *potentially* result in emergency calls
     *           since many networks will actually ignore trailing digits
     *           after a valid emergency number.)
     *
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / sim, otherwise return false.
     */
    private static boolean isEmergencyNumberInternal(String number, boolean useExactMatch) {
        /// M: [mtk04070][111220][ALPS00093395]Replace this method with our own isEmergencyNumber method. @{
        return isEmergencyNumber(number);
        /// @}
    }

    /**
     * Checks if a given number is an emergency number for a specific country.
     *
     * @param number the number to look up.
     * @param defaultCountryIso the specific country which the number should be checked against
     * @return if the number is an emergency number for the specific country, then return true,
     * otherwise false
     *
     * @hide
     */
    public static boolean isEmergencyNumber(String number, String defaultCountryIso) {
        return isEmergencyNumberInternal(number,
                                         defaultCountryIso,
                                         true /* useExactMatch */);
    }

    /**
     * Checks if a given number might *potentially* result in a call to an
     * emergency service, for a specific country.
     *
     * Specifically, this method will return true if the specified number
     * is an emergency number in the specified country, *or* if the number
     * simply starts with the same digits as any emergency number for that
     * country.
     *
     * This method is intended for internal use by the phone app when
     * deciding whether to allow ACTION_CALL intents from 3rd party apps
     * (where we're required to *not* allow emergency calls to be placed.)
     *
     * @param number the number to look up.
     * @param defaultCountryIso the specific country which the number should be checked against
     * @return true if the number is an emergency number for the specific
     *         country, *or* if the number starts with the same digits as
     *         any of those emergency numbers.
     *
     * @hide
     */
    public static boolean isPotentialEmergencyNumber(String number, String defaultCountryIso) {
        return isEmergencyNumberInternal(number,
                                         defaultCountryIso,
                                         false /* useExactMatch */);
    }

    /**
     * Helper function for isEmergencyNumber(String, String) and
     * isPotentialEmergencyNumber(String, String).
     *
     * @param number the number to look up.
     * @param defaultCountryIso the specific country which the number should be checked against
     * @param useExactMatch if true, consider a number to be an emergency
     *           number only if it *exactly* matches a number listed in
     *           the RIL / SIM.  If false, a number is considered to be an
     *           emergency number if it simply starts with the same digits
     *           as any of the emergency numbers listed in the RIL / SIM.
     *
     * @return true if the number is an emergency number for the specified country.
     */
    private static boolean isEmergencyNumberInternal(String number,
                                                     String defaultCountryIso,
                                                     boolean useExactMatch) {
        /// M: Solve [ALPS00327050]ECC can not be dialed out without SIM. @{
        //Replace this method with our own isEmergencyNumber method.
        return isEmergencyNumber(number);
        /// @}
             
        /*                                        	
        // If the number passed in is null, just return false:
        if (number == null) return false;

        // If the number passed in is a SIP address, return false, since the
        // concept of "emergency numbers" is only meaningful for calls placed
        // over the cell network.
        // (Be sure to do this check *before* calling extractNetworkPortionAlt(),
        // since the whole point of extractNetworkPortionAlt() is to filter out
        // any non-dialable characters (which would turn 'abc911def@example.com'
        // into '911', for example.))
        if (isUriNumber(number)) {
            return false;
        }

        // Strip the separators from the number before comparing it
        // to the list.
        number = extractNetworkPortionAlt(number);

        // retrieve the list of emergency numbers
        // check read-write ecclist property first
        String numbers = SystemProperties.get("ril.ecclist");
        if (TextUtils.isEmpty(numbers)) {
            // then read-only ecclist property since old RIL only uses this
            numbers = SystemProperties.get("ro.ril.ecclist");
        }

        if (!TextUtils.isEmpty(numbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : numbers.split(",")) {
                // It is not possible to append additional digits to an emergency number to dial
                // the number in Brazil - it won't connect.
                if (useExactMatch || "BR".equalsIgnoreCase(defaultCountryIso)) {
                    if (number.equals(emergencyNum)) {
                        return true;
                    }
                } else {
                    if (number.startsWith(emergencyNum)) {
                        return true;
                    }
                }
            }
            // no matches found against the list!
            return false;
        }

        Log.d(LOG_TAG, "System property doesn't provide any emergency numbers."
                + " Use embedded logic for determining ones.");

        // No ecclist system property, so use our own list.
        if (defaultCountryIso != null) {
            ShortNumberUtil util = new ShortNumberUtil();
            if (useExactMatch) {
                return util.isEmergencyNumber(number, defaultCountryIso);
            } else {
                return util.connectsToEmergencyNumber(number, defaultCountryIso);
            }
        } else {
            if (useExactMatch) {
                return (number.equals("112") || number.equals("911"));
            } else {
                return (number.startsWith("112") || number.startsWith("911"));
            }
        }
        */
    }

    /**
     * Checks if a given number is an emergency number for the country that the user is in. The
     * current country is determined using the CountryDetector.
     *
     * @param number the number to look up.
     * @param context the specific context which the number should be checked against
     * @return true if the specified number is an emergency number for a local country, based on the
     *              CountryDetector.
     *
     * @see android.location.CountryDetector
     * @hide
     */
    public static boolean isLocalEmergencyNumber(String number, Context context) {
        return isLocalEmergencyNumberInternal(number,
                                              context,
                                              true /* useExactMatch */);
    }

    /**
     * Checks if a given number might *potentially* result in a call to an
     * emergency service, for the country that the user is in. The current
     * country is determined using the CountryDetector.
     *
     * Specifically, this method will return true if the specified number
     * is an emergency number in the current country, *or* if the number
     * simply starts with the same digits as any emergency number for the
     * current country.
     *
     * This method is intended for internal use by the phone app when
     * deciding whether to allow ACTION_CALL intents from 3rd party apps
     * (where we're required to *not* allow emergency calls to be placed.)
     *
     * @param number the number to look up.
     * @param context the specific context which the number should be checked against
     * @return true if the specified number is an emergency number for a local country, based on the
     *              CountryDetector.
     *
     * @see android.location.CountryDetector
     * @hide
     */
    public static boolean isPotentialLocalEmergencyNumber(String number, Context context) {
        return isLocalEmergencyNumberInternal(number,
                                              context,
                                              false /* useExactMatch */);
    }

    /**
     * Helper function for isLocalEmergencyNumber() and
     * isPotentialLocalEmergencyNumber().
     *
     * @param number the number to look up.
     * @param context the specific context which the number should be checked against
     * @param useExactMatch if true, consider a number to be an emergency
     *           number only if it *exactly* matches a number listed in
     *           the RIL / SIM.  If false, a number is considered to be an
     *           emergency number if it simply starts with the same digits
     *           as any of the emergency numbers listed in the RIL / SIM.
     *
     * @return true if the specified number is an emergency number for a
     *              local country, based on the CountryDetector.
     *
     * @see android.location.CountryDetector
     */
    private static boolean isLocalEmergencyNumberInternal(String number,
                                                          Context context,
                                                          boolean useExactMatch) {
        String countryIso;
        CountryDetector detector = (CountryDetector) context.getSystemService(
                Context.COUNTRY_DETECTOR);
        /// M: Check the return value of detectCounty() before using it.
        if ((detector != null) && (detector.detectCountry() != null)) {
            countryIso = detector.detectCountry().getCountryIso();
        } else {
            Locale locale = context.getResources().getConfiguration().locale;
            countryIso = locale.getCountry();
            Log.w(LOG_TAG, "No CountryDetector; falling back to countryIso based on locale: "
                    + countryIso);
        }
        return isEmergencyNumberInternal(number, countryIso, useExactMatch);
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     * @hide TODO: pending API Council approval
     */
    public static boolean isVoiceMailNumber(String number) {
        String vmNumber;

        try {
            vmNumber = TelephonyManager.getDefault().getVoiceMailNumber();
        } catch (SecurityException ex) {
            return false;
        }

        // Strip the separators from the number before comparing it
        // to the list.
        number = extractNetworkPortionAlt(number);

        // compare tolerates null so we need to make sure that we
        // don't return true when both are null.
        return !TextUtils.isEmpty(number) && compare(number, vmNumber);
    }

    /**
     * Translates any alphabetic letters (i.e. [A-Za-z]) in the
     * specified phone number into the equivalent numeric digits,
     * according to the phone keypad letter mapping described in
     * ITU E.161 and ISO/IEC 9995-8.
     *
     * @return the input string, with alpha letters converted to numeric
     *         digits using the phone keypad letter mapping.  For example,
     *         an input of "1-800-GOOG-411" will return "1-800-4664-411".
     */
    public static String convertKeypadLettersToDigits(String input) {
        if (input == null) {
            return input;
        }
        int len = input.length();
        if (len == 0) {
            return input;
        }

        char[] out = input.toCharArray();

        for (int i = 0; i < len; i++) {
            char c = out[i];
            // If this char isn't in KEYPAD_MAP at all, just leave it alone.
            out[i] = (char) KEYPAD_MAP.get(c, c);
        }

        return new String(out);
    }

    /**
     * The phone keypad letter mapping (see ITU E.161 or ISO/IEC 9995-8.)
     * TODO: This should come from a resource.
     */
    private static final SparseIntArray KEYPAD_MAP = new SparseIntArray();
    static {
        KEYPAD_MAP.put('a', '2'); KEYPAD_MAP.put('b', '2'); KEYPAD_MAP.put('c', '2');
        KEYPAD_MAP.put('A', '2'); KEYPAD_MAP.put('B', '2'); KEYPAD_MAP.put('C', '2');

        KEYPAD_MAP.put('d', '3'); KEYPAD_MAP.put('e', '3'); KEYPAD_MAP.put('f', '3');
        KEYPAD_MAP.put('D', '3'); KEYPAD_MAP.put('E', '3'); KEYPAD_MAP.put('F', '3');

        KEYPAD_MAP.put('g', '4'); KEYPAD_MAP.put('h', '4'); KEYPAD_MAP.put('i', '4');
        KEYPAD_MAP.put('G', '4'); KEYPAD_MAP.put('H', '4'); KEYPAD_MAP.put('I', '4');

        KEYPAD_MAP.put('j', '5'); KEYPAD_MAP.put('k', '5'); KEYPAD_MAP.put('l', '5');
        KEYPAD_MAP.put('J', '5'); KEYPAD_MAP.put('K', '5'); KEYPAD_MAP.put('L', '5');

        KEYPAD_MAP.put('m', '6'); KEYPAD_MAP.put('n', '6'); KEYPAD_MAP.put('o', '6');
        KEYPAD_MAP.put('M', '6'); KEYPAD_MAP.put('N', '6'); KEYPAD_MAP.put('O', '6');

        KEYPAD_MAP.put('p', '7'); KEYPAD_MAP.put('q', '7'); KEYPAD_MAP.put('r', '7'); KEYPAD_MAP.put('s', '7');
        KEYPAD_MAP.put('P', '7'); KEYPAD_MAP.put('Q', '7'); KEYPAD_MAP.put('R', '7'); KEYPAD_MAP.put('S', '7');

        KEYPAD_MAP.put('t', '8'); KEYPAD_MAP.put('u', '8'); KEYPAD_MAP.put('v', '8');
        KEYPAD_MAP.put('T', '8'); KEYPAD_MAP.put('U', '8'); KEYPAD_MAP.put('V', '8');

        KEYPAD_MAP.put('w', '9'); KEYPAD_MAP.put('x', '9'); KEYPAD_MAP.put('y', '9'); KEYPAD_MAP.put('z', '9');
        KEYPAD_MAP.put('W', '9'); KEYPAD_MAP.put('X', '9'); KEYPAD_MAP.put('Y', '9'); KEYPAD_MAP.put('Z', '9');
    }

    //================ Plus Code formatting =========================
    private static final char PLUS_SIGN_CHAR = '+';
    private static final String PLUS_SIGN_STRING = "+";
    private static final String NANP_IDP_STRING = "011";
    private static final int NANP_LENGTH = 10;

    /**
     * This function checks if there is a plus sign (+) in the passed-in dialing number.
     * If there is, it processes the plus sign based on the default telephone
     * numbering plan of the system when the phone is activated and the current
     * telephone numbering plan of the system that the phone is camped on.
     * Currently, we only support the case that the default and current telephone
     * numbering plans are North American Numbering Plan(NANP).
     *
     * The passed-in dialStr should only contain the valid format as described below,
     * 1) the 1st character in the dialStr should be one of the really dialable
     *    characters listed below
     *    ISO-LATIN characters 0-9, *, # , +
     * 2) the dialStr should already strip out the separator characters,
     *    every character in the dialStr should be one of the non separator characters
     *    listed below
     *    ISO-LATIN characters 0-9, *, # , +, WILD, WAIT, PAUSE
     *
     * Otherwise, this function returns the dial string passed in
     *
     * @param dialStr the original dial string
     * @return the converted dial string if the current/default countries belong to NANP,
     * and if there is the "+" in the original dial string. Otherwise, the original dial
     * string returns.
     *
     * This API is for CDMA only
     *
     * @hide TODO: pending API Council approval
     */
    public static String cdmaCheckAndProcessPlusCode(String dialStr) {
        if (!TextUtils.isEmpty(dialStr)) {
            if (isReallyDialable(dialStr.charAt(0)) &&
                isNonSeparator(dialStr)) {
                String currIso = SystemProperties.get(PROPERTY_OPERATOR_ISO_COUNTRY, "");
                String defaultIso = SystemProperties.get(PROPERTY_ICC_OPERATOR_ISO_COUNTRY, "");
                if (!TextUtils.isEmpty(currIso) && !TextUtils.isEmpty(defaultIso)) {
                    return cdmaCheckAndProcessPlusCodeByNumberFormat(dialStr,
                            getFormatTypeFromCountryCode(currIso),
                            getFormatTypeFromCountryCode(defaultIso));
                }
            }
        }
        return dialStr;
    }

    /**
     * This function should be called from checkAndProcessPlusCode only
     * And it is used for test purpose also.
     *
     * It checks the dial string by looping through the network portion,
     * post dial portion 1, post dial porting 2, etc. If there is any
     * plus sign, then process the plus sign.
     * Currently, this function supports the plus sign conversion within NANP only.
     * Specifically, it handles the plus sign in the following ways:
     * 1)+1NANP,remove +, e.g.
     *   +18475797000 is converted to 18475797000,
     * 2)+NANP or +non-NANP Numbers,replace + with the current NANP IDP, e.g,
     *   +8475797000 is converted to 0118475797000,
     *   +11875767800 is converted to 01111875767800
     * 3)+1NANP in post dial string(s), e.g.
     *   8475797000;+18475231753 is converted to 8475797000;18475231753
     *
     *
     * @param dialStr the original dial string
     * @param currFormat the numbering system of the current country that the phone is camped on
     * @param defaultFormat the numbering system of the country that the phone is activated on
     * @return the converted dial string if the current/default countries belong to NANP,
     * and if there is the "+" in the original dial string. Otherwise, the original dial
     * string returns.
     *
     * @hide
     */
    public static String
    cdmaCheckAndProcessPlusCodeByNumberFormat(String dialStr,int currFormat,int defaultFormat) {
        String retStr = dialStr;

        // Checks if the plus sign character is in the passed-in dial string
        if (dialStr != null &&
            dialStr.lastIndexOf(PLUS_SIGN_STRING) != -1) {
            // Format the string based on the rules for the country the number is from,
            // and the current country the phone is camped on.
            if ((currFormat == defaultFormat) && (currFormat == FORMAT_NANP)) {
                // Handle case where default and current telephone numbering plans are NANP.
                String postDialStr = null;
                String tempDialStr = dialStr;

                // Sets the retStr to null since the conversion will be performed below.
                retStr = null;
                if (DBG) log("checkAndProcessPlusCode,dialStr=" + dialStr);
                // This routine is to process the plus sign in the dial string by loop through
                // the network portion, post dial portion 1, post dial portion 2... etc. if
                // applied
                do {
                    String networkDialStr;
                    networkDialStr = extractNetworkPortion(tempDialStr);
                    // Handles the conversion within NANP
                    networkDialStr = processPlusCodeWithinNanp(networkDialStr);

                    // Concatenates the string that is converted from network portion
                    if (!TextUtils.isEmpty(networkDialStr)) {
                        if (retStr == null) {
                            retStr = networkDialStr;
                        } else {
                            retStr = retStr.concat(networkDialStr);
                        }
                    } else {
                        // This should never happen since we checked the if dialStr is null
                        // and if it contains the plus sign in the beginning of this function.
                        // The plus sign is part of the network portion.
                        Log.e("checkAndProcessPlusCode: null newDialStr", networkDialStr);
                        return dialStr;
                    }
                    postDialStr = extractPostDialPortion(tempDialStr);
                    if (!TextUtils.isEmpty(postDialStr)) {
                        int dialableIndex = findDialableIndexFromPostDialStr(postDialStr);

                        // dialableIndex should always be greater than 0
                        if (dialableIndex >= 1) {
                            retStr = appendPwCharBackToOrigDialStr(dialableIndex,
                                     retStr,postDialStr);
                            // Skips the P/W character, extracts the dialable portion
                            tempDialStr = postDialStr.substring(dialableIndex);
                        } else {
                            // Non-dialable character such as P/W should not be at the end of
                            // the dial string after P/W processing in CdmaConnection.java
                            // Set the postDialStr to "" to break out of the loop
                            if (dialableIndex < 0) {
                                postDialStr = "";
                            }
                            Log.e("wrong postDialStr=", postDialStr);
                        }
                    }
                    if (DBG) log("checkAndProcessPlusCode,postDialStr=" + postDialStr);
                } while (!TextUtils.isEmpty(postDialStr) && !TextUtils.isEmpty(tempDialStr));
            } else {
                // TODO: Support NANP international conversion and other telephone numbering plans.
                // Currently the phone is never used in non-NANP system, so return the original
                // dial string.
                Log.e("checkAndProcessPlusCode:non-NANP not supported", dialStr);
            }
        }
        return retStr;
     }

    // This function gets the default international dialing prefix
    private static String getDefaultIdp( ) {
        String ps = null;
        SystemProperties.get(PROPERTY_IDP_STRING, ps);
        if (TextUtils.isEmpty(ps)) {
            ps = NANP_IDP_STRING;
        }
        return ps;
    }

    private static boolean isTwoToNine (char c) {
        if (c >= '2' && c <= '9') {
            return true;
        } else {
            return false;
        }
    }

    private static int getFormatTypeFromCountryCode (String country) {
        // Check for the NANP countries
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(country) == 0) {
                return FORMAT_NANP;
            }
        }
        if ("jp".compareToIgnoreCase(country) == 0) {
            return FORMAT_JAPAN;
        }
        return FORMAT_UNKNOWN;
    }

    /**
     * This function checks if the passed in string conforms to the NANP format
     * i.e. NXX-NXX-XXXX, N is any digit 2-9 and X is any digit 0-9
     */
    private static boolean isNanp (String dialStr) {
        boolean retVal = false;
        if (dialStr != null) {
            if (dialStr.length() == NANP_LENGTH) {
                if (isTwoToNine(dialStr.charAt(0)) &&
                    isTwoToNine(dialStr.charAt(3))) {
                    retVal = true;
                    for (int i=1; i<NANP_LENGTH; i++ ) {
                        char c=dialStr.charAt(i);
                        if (!PhoneNumberUtils.isISODigit(c)) {
                            retVal = false;
                            break;
                        }
                    }
                }
            }
        } else {
            Log.e("isNanp: null dialStr passed in", dialStr);
        }
        return retVal;
    }

   /**
    * This function checks if the passed in string conforms to 1-NANP format
    */
    private static boolean isOneNanp(String dialStr) {
        boolean retVal = false;
        if (dialStr != null) {
            String newDialStr = dialStr.substring(1);
            if ((dialStr.charAt(0) == '1') && isNanp(newDialStr)) {
                retVal = true;
            }
        } else {
            Log.e("isOneNanp: null dialStr passed in", dialStr);
        }
        return retVal;
    }

    /**
     * Determines if the specified number is actually a URI
     * (i.e. a SIP address) rather than a regular PSTN phone number,
     * based on whether or not the number contains an "@" character.
     *
     * @hide
     * @param number
     * @return true if number contains @
     */
    public static boolean isUriNumber(String number) {
        // Note we allow either "@" or "%40" to indicate a URI, in case
        // the passed-in string is URI-escaped.  (Neither "@" nor "%40"
        // will ever be found in a legal PSTN number.)
        return number != null && (number.contains("@") || number.contains("%40"));
    }

    /**
     * @return the "username" part of the specified SIP address,
     *         i.e. the part before the "@" character (or "%40").
     *
     * @param number SIP address of the form "username@domainname"
     *               (or the URI-escaped equivalent "username%40domainname")
     * @see isUriNumber
     *
     * @hide
     */
    public static String getUsernameFromUriNumber(String number) {
        // The delimiter between username and domain name can be
        // either "@" or "%40" (the URI-escaped equivalent.)
        int delimiterIndex = number.indexOf('@');
        if (delimiterIndex < 0) {
            delimiterIndex = number.indexOf("%40");
        }
        if (delimiterIndex < 0) {
            Log.w(LOG_TAG,
                  "getUsernameFromUriNumber: no delimiter found in SIP addr '" + number + "'");
            delimiterIndex = number.length();
        }
        return number.substring(0, delimiterIndex);
    }

    /**
     * This function handles the plus code conversion within NANP CDMA network
     * If the number format is
     * 1)+1NANP,remove +,
     * 2)other than +1NANP, any + numbers,replace + with the current IDP
     */
    private static String processPlusCodeWithinNanp(String networkDialStr) {
        String retStr = networkDialStr;

        if (DBG) log("processPlusCodeWithinNanp,networkDialStr=" + networkDialStr);
        // If there is a plus sign at the beginning of the dial string,
        // Convert the plus sign to the default IDP since it's an international number
        if (networkDialStr != null &&
            networkDialStr.charAt(0) == PLUS_SIGN_CHAR &&
            networkDialStr.length() > 1) {
            String newStr = networkDialStr.substring(1);
            if (isOneNanp(newStr)) {
                // Remove the leading plus sign
                retStr = newStr;
             } else {
                 String idpStr = getDefaultIdp();
                 // Replaces the plus sign with the default IDP
                 retStr = networkDialStr.replaceFirst("[+]", idpStr);
            }
        }
        if (DBG) log("processPlusCodeWithinNanp,retStr=" + retStr);
        return retStr;
    }

    // This function finds the index of the dialable character(s)
    // in the post dial string
    private static int findDialableIndexFromPostDialStr(String postDialStr) {
        for (int index = 0;index < postDialStr.length();index++) {
             char c = postDialStr.charAt(index);
             if (isReallyDialable(c)) {
                return index;
             }
        }
        return -1;
    }

    // This function appends the non-dialable P/W character to the original
    // dial string based on the dialable index passed in
    private static String
    appendPwCharBackToOrigDialStr(int dialableIndex,String origStr, String dialStr) {
        String retStr;

        // There is only 1 P/W character before the dialable characters
        if (dialableIndex == 1) {
            StringBuilder ret = new StringBuilder(origStr);
            ret = ret.append(dialStr.charAt(0));
            retStr = ret.toString();
        } else {
            // It means more than 1 P/W characters in the post dial string,
            // appends to retStr
            String nonDigitStr = dialStr.substring(0,dialableIndex);
            retStr = origStr.concat(nonDigitStr);
        }
        return retStr;
    }

    //===== Beginning of utility methods used in compareLoosely() =====

    /**
     * Phone numbers are stored in "lookup" form in the database
     * as reversed strings to allow for caller ID lookup
     *
     * This method takes a phone number and makes a valid SQL "LIKE"
     * string that will match the lookup form
     *
     */
    /** all of a up to len must be an international prefix or
     *  separators/non-dialing digits
     */
    private static boolean
    matchIntlPrefix(String a, int len) {
        /* '([^0-9*#+pwn]\+[^0-9*#+pwn] | [^0-9*#+pwn]0(0|11)[^0-9*#+pwn] )$' */
        /*        0       1                           2 3 45               */

        int state = 0;
        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                break;

                default:
                    if (isNonSeparator(c)) return false;
                break;

            }
        }

        return state == 1 || state == 3 || state == 5;
    }

    /** all of 'a' up to len must be a (+|00|011)country code)
     *  We're fast and loose with the country code. Any \d{1,3} matches */
    private static boolean
    matchIntlPrefixAndCC(String a, int len) {
        /*  [^0-9*#+pwn]*(\+|0(0|11)\d\d?\d? [^0-9*#+pwn] $ */
        /*      0          1 2 3 45  6 7  8                 */

        int state = 0;
        for (int i = 0 ; i < len ; i++ ) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                break;

                case 1:
                case 3:
                case 5:
                    if      (isISODigit(c)) state = 6;
                    else if (isNonSeparator(c)) return false;
                break;

                case 6:
                case 7:
                    if      (isISODigit(c)) state++;
                    else if (isNonSeparator(c)) return false;
                break;

                default:
                    if (isNonSeparator(c)) return false;
            }
        }

        return state == 6 || state == 7 || state == 8;
    }

    /** all of 'a' up to len must match non-US trunk prefix ('0') */
    private static boolean
    matchTrunkPrefix(String a, int len) {
        boolean found;

        found = false;

        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            if (c == '0' && !found) {
                found = true;
            } else if (isNonSeparator(c)) {
                return false;
            }
        }

        return found;
    }

    //===== End of utility methods used only in compareLoosely() =====

    //===== Beginning of utility methods used only in compareStrictly() ====

    /*
     * If true, the number is country calling code.
     */
    private static final boolean COUNTRY_CALLING_CALL[] = {
        true, true, false, false, false, false, false, true, false, false,
        false, false, false, false, false, false, false, false, false, false,
        true, false, false, false, false, false, false, true, true, false,
        true, true, true, true, true, false, true, false, false, true,
        true, false, false, true, true, true, true, true, true, true,
        false, true, true, true, true, true, true, true, true, false,
        true, true, true, true, true, true, true, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, true, true, true, true, false, true, false, false, true,
        true, true, true, true, true, true, false, false, true, false,
    };
    private static final int CCC_LENGTH = COUNTRY_CALLING_CALL.length;

    /**
     * @return true when input is valid Country Calling Code.
     */
    private static boolean isCountryCallingCode(int countryCallingCodeCandidate) {
        return countryCallingCodeCandidate > 0 && countryCallingCodeCandidate < CCC_LENGTH &&
                COUNTRY_CALLING_CALL[countryCallingCodeCandidate];
    }

    /**
     * Returns integer corresponding to the input if input "ch" is
     * ISO-LATIN characters 0-9.
     * Returns -1 otherwise
     */
    private static int tryGetISODigit(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        } else {
            return -1;
        }
    }

    private static class CountryCallingCodeAndNewIndex {
        public final int countryCallingCode;
        public final int newIndex;
        public CountryCallingCodeAndNewIndex(int countryCode, int newIndex) {
            this.countryCallingCode = countryCode;
            this.newIndex = newIndex;
        }
    }

    /*
     * Note that this function does not strictly care the country calling code with
     * 3 length (like Morocco: +212), assuming it is enough to use the first two
     * digit to compare two phone numbers.
     */
    private static CountryCallingCodeAndNewIndex tryGetCountryCallingCodeAndNewIndex(
        String str, boolean acceptThailandCase) {
        // Rough regexp:
        //  ^[^0-9*#+]*((\+|0(0|11)\d\d?|166) [^0-9*#+] $
        //         0        1 2 3 45  6 7  89
        //
        // In all the states, this function ignores separator characters.
        // "166" is the special case for the call from Thailand to the US. Uguu!
        int state = 0;
        int ccc = 0;
        final int length = str.length();
        for (int i = 0 ; i < length ; i++ ) {
            char ch = str.charAt(i);
            switch (state) {
                case 0:
                    if      (ch == '+') state = 1;
                    else if (ch == '0') state = 2;
                    else if (ch == '1') {
                        if (acceptThailandCase) {
                            state = 8;
                        } else {
                            return null;
                        }
                    } else if (isDialable(ch)) {
                        return null;
                    }
                break;

                case 2:
                    if      (ch == '0') state = 3;
                    else if (ch == '1') state = 4;
                    else if (isDialable(ch)) {
                        return null;
                    }
                break;

                case 4:
                    if      (ch == '1') state = 5;
                    else if (isDialable(ch)) {
                        return null;
                    }
                break;

                case 1:
                case 3:
                case 5:
                case 6:
                case 7:
                    {
                        int ret = tryGetISODigit(ch);
                        if (ret > 0) {
                            ccc = ccc * 10 + ret;
                            if (ccc >= 100 || isCountryCallingCode(ccc)) {
                                return new CountryCallingCodeAndNewIndex(ccc, i + 1);
                            }
                            if (state == 1 || state == 3 || state == 5) {
                                state = 6;
                            } else {
                                state++;
                            }
                        } else if (isDialable(ch)) {
                            return null;
                        }
                    }
                    break;
                case 8:
                    if (ch == '6') state = 9;
                    else if (isDialable(ch)) {
                        return null;
                    }
                    break;
                case 9:
                    if (ch == '6') {
                        return new CountryCallingCodeAndNewIndex(66, i + 1);
                    } else {
                        return null;
                    }
                default:
                    return null;
            }
        }

        return null;
    }

    /**
     * Currently this function simply ignore the first digit assuming it is
     * trunk prefix. Actually trunk prefix is different in each country.
     *
     * e.g.
     * "+79161234567" equals "89161234567" (Russian trunk digit is 8)
     * "+33123456789" equals "0123456789" (French trunk digit is 0)
     *
     */
    private static int tryGetTrunkPrefixOmittedIndex(String str, int currentIndex) {
        int length = str.length();
        for (int i = currentIndex ; i < length ; i++) {
            final char ch = str.charAt(i);
            if (tryGetISODigit(ch) >= 0) {
                return i + 1;
            } else if (isDialable(ch)) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Return true if the prefix of "str" is "ignorable". Here, "ignorable" means
     * that "str" has only one digit and separator characters. The one digit is
     * assumed to be trunk prefix.
     */
    private static boolean checkPrefixIsIgnorable(final String str,
            int forwardIndex, int backwardIndex) {
        boolean trunk_prefix_was_read = false;
        while (backwardIndex >= forwardIndex) {
            if (tryGetISODigit(str.charAt(backwardIndex)) >= 0) {
                if (trunk_prefix_was_read) {
                    // More than one digit appeared, meaning that "a" and "b"
                    // is different.
                    return false;
                } else {
                    // Ignore just one digit, assuming it is trunk prefix.
                    trunk_prefix_was_read = true;
                }
            } else if (isDialable(str.charAt(backwardIndex))) {
                // Trunk prefix is a digit, not "*", "#"...
                return false;
            }
            backwardIndex--;
        }

        return true;
    }

    //==== End of utility methods used only in compareStrictly() =====

    /// M: [mtk04070][111116][ALPS00093395]MTK proprietary methods(functions). @{
    /**
     * @hide only for GsmMmiCode invoked by GSMPhone.dial
     */
    public static String
    extractGsmMmiNetworkPortion(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);
        boolean firstCharAdded = false;
        // mtk00732 allow "+" after "*" in GsmMmiCode
        boolean starfound = false;

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (isDialable(c) && (c != '+' || !firstCharAdded || 
                ((c == '+') && (i > 1) && (phoneNumber.charAt(i-1) == '*')))) {
                firstCharAdded = true;
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        return ret.toString();
    }

    /**
     * {@hide}
     */     
    public static String prependPlusToNumber(String number) {

        // This is an "international number" and should have
        // a plus prepended to the dialing number. But there
        // can also be Gsm MMI codes as defined in TS 22.030 6.5.2
        // so we need to handle those also.
        //
        // http://web.telia.com/~u47904776/gsmkode.htm is a
        // has a nice list of some of these GSM codes.
        //
        // Examples are:
        //   **21*+886988171479#
        //   **21*8311234567#
        //   *21#
        //   #21#
        //   *#21#
        //   *31#+11234567890
        //   #31#+18311234567
        //   #31#8311234567
        //   18311234567
        //   +18311234567#
        //   +18311234567
        // Odd ball cases that some phones handled
        // where there is no dialing number so they
        // append the "+"
        //   *21#+
        //   **21#+
        StringBuilder ret;
        String retString = number.toString();
        Pattern p = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$");
        Matcher m = p.matcher(retString);
        if (m.matches()) {
            if ("".equals(m.group(2))) {
                // Started with two [#*] ends with #
                // So no dialing number and we'll just
                // append a +, this handles **21#+
                ret = new StringBuilder();
                ret.append(m.group(1));
                ret.append(m.group(3));
                ret.append(m.group(4));
                ret.append(m.group(5));
                ret.append("+");
            } else {
                // Starts with [#*] and ends with #
                // Assume group 4 is a dialing number
                // such as *21*+1234554#
                ret = new StringBuilder();
                ret.append(m.group(1));
                ret.append(m.group(2));
                ret.append(m.group(3));
                ret.append("+");
                ret.append(m.group(4));
                ret.append(m.group(5));
            }
        } else {
            p = Pattern.compile("(^[#*])(.*)([#*])(.*)");
            m = p.matcher(retString);
            if (m.matches()) {
                // Starts with [#*] and only one other [#*]
                // Assume the data after last [#*] is dialing
                // number (i.e. group 4) such as *31#+11234567890.
                // This also includes the odd ball *21#+
                ret = new StringBuilder();
                ret.append(m.group(1));
                ret.append(m.group(2));
                ret.append(m.group(3));
                ret.append("+");
                ret.append(m.group(4));
            } else {
                // Does NOT start with [#*] just prepend '+'
                ret = new StringBuilder();
                ret.append('+');
                ret.append(retString);
            }
        }

        return ret.toString();
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     * @simId the SIM card ID
     * @hide TODO: pending API Council approval
     */
    public static boolean isVoiceMailNumberGemini(String number, int simId) {
        String vmNumber;

        log("number " + number + " simId: " + simId);

        try {
            ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            vmNumber = iTel.getVoiceMailNumber(simId);
        } catch (Exception ex) {
            return false;
        }

        // Strip the separators from the number before comparing it
        // to the list.
        number = extractNetworkPortionAlt(number);

        // compare tolerates null so we need to make sure that we
        // don't return true when both are null.
        return !TextUtils.isEmpty(number) && compare(number, vmNumber);
    }

    public static boolean isIdleSsString(String dialString) {
        Log.d(LOG_TAG, "isIdleSsString(): dialString = " + dialString);
        String newDialString = PhoneNumberUtils.stripSeparators(dialString);
        String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);

        Pattern sPatternSuppService = Pattern.compile(
        "((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
        Matcher m;
        boolean ret = false;

        m = sPatternSuppService.matcher(networkPortion);

        if (m.matches()) {
            String action = m.group(2);
            String sc = m.group(3);
            String dialNumber = m.group(12);
            Log.d(LOG_TAG, "action = " + action + ", sc = " + sc + ", dialNumber = " + dialNumber);
            if ((sc != null && sc.equals("31")) && (action != null && (action.equals("*") || action.equals("#"))) && (dialNumber != null && dialNumber.length() != 0)) {
                Log.d(LOG_TAG, networkPortion + " is temporary CLIR");
            } else {
                ret = true;
            }
        } else if (networkPortion.endsWith("#")) {
            ret = true;
        } else if ((networkPortion != null && networkPortion.length() <= 2)
             && !((networkPortion.length() == 2 && networkPortion.charAt(0) == '1') || networkPortion.equals("0") || networkPortion.equals("00"))) {
            ret = true;
        }

        Log.d(LOG_TAG, networkPortion + " isIdleSsString: " + ret);
        return ret;
    }

    public static boolean isIncallSsString(String dialString) {

        return ((dialString != null && dialString.length() <= 2)
                && !PhoneNumberUtils.isEmergencyNumber(dialString)
                && !(dialString.equals("0") || dialString.equals("00")));
    }

    public static boolean isSpecialEmergencyNumber(String dialString) {
        /* Special emergency number will show ecc in MMI but sent to nw as normal call */
        return mPhoneNumberExt.isSpecialEmergencyNumber(dialString);
    }

    public static String extractCLIRPortion(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        if (phoneNumber.startsWith("*31#") || phoneNumber.startsWith("#31#")) {
            log(phoneNumber + " Start with *31# or #31#, return " + phoneNumber.substring(4));
            return phoneNumber.substring(4);
        } else if (phoneNumber.indexOf(PLUS_SIGN_STRING) != -1 && 
                   phoneNumber.indexOf(PLUS_SIGN_STRING) == phoneNumber.lastIndexOf(PLUS_SIGN_STRING)){
            Pattern p = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$");
            Matcher m = p.matcher(phoneNumber);
            if (m.matches()) {
                if ("".equals(m.group(2))) {
                    // Started with two [#*] ends with #
                    // So no dialing number and we'll just return "" a +, this handles **21#+
                    log(phoneNumber + " matcher pattern1, return empty string.");
                    return "";
                } else if (m.group(4) != null && m.group(4).length() > 1 && m.group(4).charAt(0) == PLUS_SIGN_CHAR) {
                    // Starts with [#*] and ends with #
                    // Assume group 4 is a dialing number such as *21*+1234554#
                    log(phoneNumber + " matcher pattern1, return " + m.group(4));
                    return m.group(4);
                }
            } else {
                p = Pattern.compile("(^[#*])(.*)([#*])(.*)");
                m = p.matcher(phoneNumber);
                if (m.matches() && m.group(4) != null && m.group(4).length() > 1 && m.group(4).charAt(0) == PLUS_SIGN_CHAR) {
                    // Starts with [#*] and only one other [#*]
                    // Assume the data after last [#*] is dialing number (i.e. group 4) such as *31#+11234567890.
                    // This also includes the odd ball *21#+
                    log(phoneNumber + " matcher pattern2, return " + m.group(4));
                    return m.group(4);
                }
            }
        }
        
        return phoneNumber;
    }
    
    /**
     * Return the validity of phone number according to country iso.
     * The numbering rule for each country is different.
     *
     * @param countryIso  Country ISO.
     * @param phoneNumber phone number string to be checked.
     * @return Return value may be one of the following:
     *         ID_VALID_ECC(1) - Emergency numbers;
     *         ID_VALID_BUT_NEED_AREA_CODE(2) - Valid but needs area code;
     *         ID_VALID(3) - Valid with area code or no need for area code;
     *         ID_VALID_DOMESTIC_ONLY(4) - Domestic valid only, which means that this numbers can be dialed in its home country but cannot be dialed out side of its home country;
     *         ID_INVALID(5) - Invalid which means the number is invalid in its home country.
     */
    public static int isValidNumber(String countryIso, String phoneNumber) {
        Log.d(LOG_TAG, "[isValidNumber] countryIso: " + countryIso + ", phoneNumber: " + phoneNumber);
    
        if ((countryIso == null) || (phoneNumber == null)) {
    	    return ID_INVALID;
    	}
               
        String number = extractNetworkPortion(stripSeparators(phoneNumber));
        boolean matchResult = false;
        boolean areaCodeMatchResult = false;
        int result = ID_VALID;
        String patternString = "";
        String areaCodePattern = "";

        String[] CHINA_INTERNATIONAL_PREFIXS = {"00"};
        String[] TAIWAN_INTERNATIONAL_PREFIXS = {"002","005","006","007","009", "016", "017", "019"};

        if (countryIso.equalsIgnoreCase("cn")) {
            patternString = "1[3-8]{1}[0-9]{1}[0-9]{8}|" +           /* 11 digits with leading number between "130" and "189"  */
                            "01[3-8]{1}[0-9]{1}[0-9]{8}|" +          /* "0" + 11 digits with leading number between "130" and "189"  */
                            "[1-9]{1}[0-9]{5,7}|" +                  /* 6 or 7 or 8 digits with no leading "0" */
                            "11[0-9]{1}114|" +                       /* 6 digits, starts from "11" and ends with "114" */ 
                            "400[0-9]{7}|" +                         /* 10 digits with leading number "400" */
                            "179[0-9]{8,}|" +                        /* At least 11 digits with leading number "179" */
                            "125[0-9]{8,}|";                         /* At least 11 digits with leading number "125" */

            areaCodePattern = "010[1-9]{1}[0-9]{7}|" +                 /* 010(3 area code) + 8 digital number with no leading"0" */
                              "02[0-9]{1}[1-9]{1}[0-9]{7}|" +          /* 02X(3 area code) + 8 digital number with no leading"0" */
                              "0[3-9]{1}[0-9]{2}[1-9]{1}[0-9]{6,7}|" + /* 0XXX(4 area code) + 7 or 8 digital number with no leading"0" */
                              "010[1-9]{1}[0-9]{2,4}|" +               /* 010(3 area code) + 3~5 special number with no leading "0" */
                              "02[0-9]{1}[1-9]{1}[0-9]{2,4}|" +        /* 02X(3 area code) + 3~5 special number with no leading "0" */
                              "0[3-9]{1}[0-9]{2}[1-9]{1}[0-9]{2,4}|" + /* 0XXX(4 area code) + 3~5 special number with no leading "0" */
                              "01011[0-9]{1}114|" +                    /* 010(3 area code) + 6 digits, starts from "11" and ends with "114" */ 
                              "02[0-9]{1}11[0-9]{1}114|" +             /* 02X(3 area code) + 6 digits, starts from "11" and ends with "114" */ 
                              "0[3-9]{1}[0-9]{2}11[0-9]{1}114|";       /* 0XXX(4 area code) + 6 digits, starts from "11" and ends with "114" */ 

            /* International prefix match */
            for(String prefix : CHINA_INTERNATIONAL_PREFIXS){
                if(number.startsWith(prefix)){                      /* The number starts with CHINA_INTERNATIONAL_PREFIX */
                    Log.d(LOG_TAG, "isValidNumber = CN start with " + prefix);
                    return result;
                }
            }
        }
        else if (countryIso.equalsIgnoreCase("tw")) {
           patternString = "09[0-9]{8}|" +                        /* 10 digits with leading number "09" */
                           "0[2-8]{1}[0-9]{7,8}|";                /* 9 or 10 digits with leading number between "02" and "08" */

            /* International prefix match */
            for(String prefix : TAIWAN_INTERNATIONAL_PREFIXS){
                if(number.startsWith(prefix)){                      /* The number starts with TW_INTERNATIONAL_PREFIX */
                    Log.d(LOG_TAG, "isValidNumber = TW start with " + prefix);
                    return result;
                }
            }
        } else {
            // Currently only support "cn" and "tw", so return ID_VALID directly. 
            return ID_VALID;
        }
        
        patternString = patternString + "[1-9]{1}[0-9]{2,4}|" +   /* 3 to 5 digits with no leading "0" */
                                        "000|08";                 /* ECC number : "000" and "08" */

        Pattern p = Pattern.compile(patternString);
        Matcher m = p.matcher(number);
        matchResult = m.matches();
        Log.d(LOG_TAG, "number = " + number +", matchResult = " + matchResult);

        if (!matchResult && areaCodePattern.length() > 0)
        {
            p = Pattern.compile(areaCodePattern);
            m = p.matcher(number);
            areaCodeMatchResult = m.matches();
            Log.d(LOG_TAG, "number = " + number +", areaCodeMatchResult = " + areaCodeMatchResult);
        }
        
        if (matchResult || areaCodeMatchResult) {
            if (isEmergencyNumber(phoneNumber)) {
                result = ID_VALID_ECC;
            } else if (isAreaCodeNeeded(countryIso, phoneNumber)) {
                result = ID_VALID_BUT_NEED_AREA_CODE;
            } else if (isDomesticOnly(countryIso, phoneNumber)) {
                result = ID_VALID_DOMESTIC_ONLY;
            } else if (areaCodeMatchResult && isValidNationalNumber(countryIso, phoneNumber) == false) {
                result = ID_INVALID;
            }
        } else if (isSpecialMmiNumber(phoneNumber) == true) {
            result = ID_VALID_WHEN_CALL_EXIST;
        } else {
            result = ID_INVALID;
        }
        
        return result;
    }
    
    /**
     * Return the international prefix string according to country iso.
     *
     * @param countryIso  Country ISO.
     * @return Return international prefix.
     */
    public static String getInternationalPrefix(String countryIso) {
    	  if (countryIso == null) {
    	  	  return "";
    	  }
    	
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        PhoneMetadata metadata = util.getMetadataForRegion(countryIso);
        if (metadata != null) {
        	  String prefix = metadata.getInternationalPrefix();
        	  if (countryIso.equalsIgnoreCase("tw")) {
        	  	  prefix = "0(?:0[25679] | 16 | 17 | 19)";
        	  }
        	  return prefix;
        }
        
        return null;
    }
    
    
    /**
     * Check if the phone number is only for domestic(home country).
     *
     * @param countryIso  Country ISO.
     * @param phoneNumber phone number string to be checked.
     * @return Return true if the phone number is only for domestic, else return false.
     */
    private static boolean isDomesticOnly(String countryIso, String phoneNumber) {
    	  if ((countryIso == null) || (phoneNumber == null)) {
    	  	  return false;
    	  }

        boolean result = true;
        String number = extractNetworkPortion(stripSeparators(phoneNumber));
        if (countryIso.equalsIgnoreCase("cn") ||
                countryIso.equalsIgnoreCase("tw")) {
            String patternString = "[1-9]{1}[0-9]{2,5}";
            Pattern p = Pattern.compile(patternString);
            Matcher m = p.matcher(number);
            result = (m.matches() && !isEmergencyNumber(phoneNumber));
        }
        return result;
    }

    /**
     * Check if the phone number should be added area code to dial out according to country iso.
     * The numbering rule for each country is different.
     *
     * @param countryIso  Country ISO.
     * @param phoneNumber phone number string to be checked.
     * @return Return true if the phone number should be added area code before dialing out, else return false.
     */
    public static boolean isAreaCodeNeeded(String countryIso, String phoneNumber) {
        if ((countryIso == null) || (phoneNumber == null)) {
            return false;
        }

        String number = extractNetworkPortion(stripSeparators(phoneNumber));
        boolean result = false;
        if (countryIso.equalsIgnoreCase("cn")) {
           String patternString = "[1-9]{1}[0-9]{2,7}";  /* 3 to 8 digits with no leading "0" */
           Pattern p = Pattern.compile(patternString);
           Matcher m = p.matcher(number);
           result = m.matches();
        }
        
        return result;
    }

    /**
     * Check if the phone number is valid national number. (Only check "CN" case now)
     *
     * @param countryIso  Country ISO.
     * @param phoneNumber phone number string to be checked.
     * @return Return true if the phone number is match national number rules, else return false.
     */
    private static boolean isValidNationalNumber(String countryIso, String phoneNumber) {
    	if ((countryIso == null) || (phoneNumber == null)) {
    	    return false;
    	}

        Log.d(LOG_TAG, "[isValidNationalNumber]countryIso: " + countryIso + ", phonenumber: " + phoneNumber);
        
        if (phoneNumber.startsWith("0"))
        {
            phoneNumber = phoneNumber.substring(1, phoneNumber.length());
            Log.d(LOG_TAG, "[isValidNationalNumber] cut '0' - phonenumber: " + phoneNumber);
        }

        boolean result = false;
        String number = stripSeparators(phoneNumber);

        /*Reference: http://zh.wikipedia.org/wiki/%E4%B8%AD%E5%8D%8E%E4%BA%BA%E6%B0
            %91%E5%85%B1%E5%92%8C%E5%9B%BD%E7%94%B5%E8%AF%9D%E5%8C%BA%E5%8F%B7 */
        String[] CHINA_AREA_PREFIXS = {
            "10", 
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "20",
            "311", "312", "313", "314", "315", "316", "317", "318", "319", "310", "335",
            "349", "351", "352", "353", "354", "355", "356", "357", "358", "350",
            "371", "372", "373", "374", "375", "376", "377", "378", "379", "370",
            "391", "392", "393", "394", "395", "396", "397", "398",
            "411", "412", "414", "415", "416", "417", "418", "419", "421", "427", "429",
            "431", "432", "433", "434", "435", "436", "437", "438", "439", 
            "451", "452", "453", "454", "455", "456", "457", "458", "459", "464", "467", "468", "469",
            "471", "472", "473", "474", "475", "476", "477", "478", "479", "470", "482", "483",
            "511", "512", "513", "514", "515", "516", "517", "518", "519", "510", "523", "527",
            "531", "532", "533", "534", "535", "536", "537", "538", "539", "530", "543", "546", 
            "631", "632", "633", "634", "635", 
            "551", "552", "553", "554", "555", "555", "556", "557", "558", "559", 
            "561", "562", "563", "564", "565", "566",
            "571", "572", "573", "574", "575", "576", "577", "578", "579", "570", "580",
            "591", "592", "593", "594", "595", "596", "597", "598", "599",
            "631", "632", "633", "634", "635", 
            "660", "662", "663", "668",
            "691", "692", 
            "711", "712", "713", "714", "715", "716", "717", "718", "719", "710", "722", "724", "728",
            "731", "734", "735", "736", "737", "738", "739", "730", "743", "744", "745", "746",
            "750", "751", "752", "753", "754", "755", "756", "757", "758", "759", 
            "760", "762", "763", "766", "768", "769", "660", "662", "663", "668",
            "771", "772", "773", "774", "775", "776", "777", "778", "779", "770",
            "791", "792", "793", "794", "795", "796", "797", "798", "799", "790", "701",
            "812", "813", "816", "817", "818", "825", "826", "827", 
            "831", "832", "833", "834", "835", "836", "837", "838", "839", "830", 
            "851", "852", "853", "854", "855", "856", "857", "858", "859",
            "871", "872", "873", "874", "875", "876", "877", "878", "879", "870", 
            "883", "886", "887", "888", "691", "692",
            "891", "892", "893", "894", "895", "896", "897", "898",
            "911", "912", "913", "914", "915", "916", "917", "919",
            "931", "932", "933", "934", "935", "936", "937", "938", "939", "930", "940", "941", "943",
            "951", "952", "953", "954", "955",
            "971", "972", "973", "974", "975", "976", "977", "979", "970",
            "991", "992", "993", "994", "995", "996", "997", "998", "999", "990",
            "901", "902", "903", "906", "908", "909"};

        if (countryIso.equalsIgnoreCase("cn")) {
            for(String prefix : CHINA_AREA_PREFIXS){
                if(number.startsWith(prefix)) {
                    if (number.charAt(prefix.length()) == '0'){       /* The number after the area code is "0" - invalid number  */
                        Log.d(LOG_TAG, "isValidNationalNumber = CN invalid number " + number.substring(0, prefix.length() + 1));
                        result = false;
                    } else {
                        Log.d(LOG_TAG, "isValidNationalNumber = CN number " + number.substring(0, prefix.length() + 1));
                        result = true;
                    }
                    break;
                }
            }
        }
        return result;
    }


    /**
     * Check if the phone number is a special MMI number. 
     *
     * @param phoneNumber phone number string to be checked.
     * @return Return true if the phone number is match special MMI number, else return false.
     */
    private static boolean isSpecialMmiNumber(String phoneNumber) {
        String patternString = "[0-5]{1}|" +                 /* 0, 1, 2, 3, 4, 5 */
                               "[1-2]{1}[1-9]{1}";           /* 1x, 2x with x = 1~9  */
  
        Pattern p = Pattern.compile(patternString);
        Matcher m = p.matcher(phoneNumber);
        return m.matches();
    }

}
/// @}

