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


package com.mediatek.phone.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.mediatek.calloption.CallOptionUtils;
import com.mediatek.telephony.PhoneNumberUtilsEx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The CallLog provider contains information about placed and received calls.
 */
public class CallHistory {

    private static final String TAG = "CallHistory";

    public static final String AUTHORITY = "call_history";

    /**
     * The content:// style URL for this provider
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Contains the recent calls.
     */
    public static class Calls implements BaseColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI_CALLS =
                Uri.parse("content://call_history/calls");

        public static final String DEFAULT_SORT_ORDER = "date DESC";

        /**
         * The MIME type of {@link #CONTENT_URI} and {@link #CONTENT_FILTER_URI}
         * providing a directory of calls.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/calls";

        /**
         * The phone number as the user entered it.
         * <P>Type: TEXT</P>
         */
        public static final String NUMBER = "number";

        /**
         * The date the call occured, in milliseconds since the epoch
         * <P>Type: TEXT</P>
         */
        public static final String DATE = "date";

        /**
         * The flag to indicate whether confirm dialog is shown or not
         * <P>Type: TEXT</P>
         */
        public static final String CONFIRM = "confirm";

        /**
         * The country code of the call number
         * <P>Type: TEXT</P>
         */
        public static final String COUNTRY_ISO = "country_iso";

        /**
         * The area code of the call number
         * <P>Type: TEXT</P>
         */
        public static final String AREA_CODE = "area_code";

        private static final String[] CALL_INFO_PROJECTION = {
            COUNTRY_ISO,
            AREA_CODE,
            CONFIRM
        };

        private static final String[] COUNTRY_ISO_PROJECTION = {
            COUNTRY_ISO
        };

        private static final String[] AREA_CODE_PROJECTION = {
            AREA_CODE
        };

        private static final int CALL_HISTORY_MAX_COUNT = 1000;

        public static final class CallInfo {
            public final String mCountryISO;
            public final String mAreaCode;
            public final long mConfirm;

            public CallInfo(String countryISO, String areaCode, long confirm) {
                mCountryISO = countryISO;
                mAreaCode = areaCode;
                mConfirm = confirm;
            }
        }

        public static void addCallNumber(Context context, String number, String currentCountryISO,
                                         long start, int slotId, boolean isMultiSim) {
            log("addCallNumber(), number = " + number + ", currentCountryISO = " + currentCountryISO
                    + ", slotId = " + slotId + ", isMultiSim = " + isMultiSim);

            // remove IP prefix
            String ipPrefix = CallOptionUtils.queryIPPrefix(context, slotId, isMultiSim);
            if (!TextUtils.isEmpty(ipPrefix)) {
                if (!number.equals(ipPrefix) && number.startsWith(ipPrefix)) {
                    number = number.replaceFirst(ipPrefix, "");
                }
            }
            CallOptionUtils.NumberInfo numberInfo
                    = CallOptionUtils.getNumberInfo(number, currentCountryISO);
            String countryISO = currentCountryISO;
            if (null == numberInfo) {
                log("addCallNumber(), numberInfo is null");
                addCallNumberInternal(context, number, countryISO, "", start);
                return;
            } else {
                if (!TextUtils.isEmpty(numberInfo.mCountryCode)) {
                    countryISO = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(
                            Integer.valueOf(numberInfo.mCountryCode));
                    log("addCallNumber(), numberInfo.mCountryCode is NOT empty, countryISO = " + countryISO);
                }
            }
            addCallNumberInternal(context, numberInfo.mSubscriber, countryISO, numberInfo.mAreaCode, start);
            if (!TextUtils.isEmpty(numberInfo.mAreaCode)) {
                // have area code case
                addCallNumberInternal(context,
                                      numberInfo.mAreaCodePrefix + numberInfo.mAreaCode + numberInfo.mSubscriber,
                                      countryISO, "", start);
            }
            // Check whether original number starts with international prefix
            String internationlPrefix = PhoneNumberUtilsEx.getInternationalPrefix(currentCountryISO);
            if (!TextUtils.isEmpty(internationlPrefix)) {
                Pattern pattern = Pattern.compile(internationlPrefix);
                Matcher matcher = pattern.matcher(number);
                if (!matcher.matches() && matcher.lookingAt()) {
                    // number start with international prefix case
                    addCallNumberInternal(context, number, currentCountryISO, "", start);
                }
            }
        }

        private static void addCallNumberInternal(Context context, String number, String countryISO,
                                                  String areaCode, long start) {
            final ContentResolver resolver = context.getContentResolver();

            Uri result = null;
            CallInfo callInfo = getCallInfo(context, number);
            if (null != callInfo) {
                long confirm = callInfo.mConfirm;

                if (1L == confirm) {
                     if (callInfo.mCountryISO.equals(countryISO) /*&&
                            callInfo.mAreaCode.equals(areaCode)*/) {
                        log("addCallNumber(), set confirm from 1 to 0");
                        confirm = 0L;
                    }
                }

                ContentValues values = new ContentValues(4);

                values.put(COUNTRY_ISO, countryISO);
                if (!TextUtils.isEmpty(areaCode)) {
                    values.put(AREA_CODE, areaCode);
                }
                values.put(DATE, Long.valueOf(start));
                values.put(CONFIRM, Long.valueOf(confirm));

                log("addCallNumber(), country iso = " + countryISO +
                        ", area code = " + areaCode + ", confirm = " + confirm);
                SelectionBuilder selectionBuilder = new SelectionBuilder(NUMBER + " = '" + number + "'");
                resolver.update(CONTENT_URI_CALLS, values, selectionBuilder.build(), null);
            } else {
                ContentValues values = new ContentValues(5);
                values.put(NUMBER, number);
                values.put(COUNTRY_ISO, countryISO);
                if (!TextUtils.isEmpty(areaCode)) {
                    values.put(AREA_CODE, areaCode);
                }
                values.put(DATE, Long.valueOf(start));
                values.put(CONFIRM, Long.valueOf(1L));
                result = resolver.insert(CONTENT_URI_CALLS, values);
                removeExpiredEntries(context);
            }
        }

        public static CallInfo getCallInfo(Context context, String number) {
            number = PhoneNumberUtils.stripSeparators(number);
            SelectionBuilder  selectionBuilder = new SelectionBuilder(NUMBER + " = '" + number + "'");
            log("select builder = " + selectionBuilder.build());
            Cursor cursor = context.getContentResolver().query(CONTENT_URI_CALLS,
                    CALL_INFO_PROJECTION, selectionBuilder.build(), null, DEFAULT_SORT_ORDER);
            if (cursor == null) {
                log("cursor is null...");
                return null;
            }

            if (cursor.getCount() <= 0) {
                log("cursor count is " + cursor.getCount());
                cursor.close();
                return null;
            }
            cursor.moveToFirst();
            if (TextUtils.isEmpty(cursor.getString(0)) || cursor.getLong(2) < 0) {
                log("country code is empty or count < 0");
                cursor.close();
                return null;
            }
            CallInfo callInfo = new CallInfo(cursor.getString(0), cursor.getString(1), cursor.getLong(2));
            cursor.close();
            return callInfo;
        }

        private static void removeExpiredEntries(Context context) {
            final ContentResolver resolver = context.getContentResolver();
            resolver.delete(CONTENT_URI_CALLS, _ID + " IN " +
                    "(SELECT " + _ID + " FROM calls ORDER BY " + DEFAULT_SORT_ORDER
                    + " LIMIT -1 OFFSET " + CALL_HISTORY_MAX_COUNT + ")", null);
        }

        public static Cursor getAllCountryISO(Context context) {
            Cursor cursor = context.getContentResolver().query(CONTENT_URI_CALLS,
                    COUNTRY_ISO_PROJECTION, null, null, DATE);
            if (cursor == null) {
                log("cursor is null...");
                return null;
            }
            return cursor;
        }

        public static String getLatestAreaCode(Context context, String countryISO) {
            SelectionBuilder selectionBuilder = new SelectionBuilder(COUNTRY_ISO + " = '" + countryISO + "'");
            selectionBuilder.addClause(AREA_CODE + " IS NOT NULL");
            Cursor cursor = context.getContentResolver().query(CONTENT_URI_CALLS,
                    AREA_CODE_PROJECTION, selectionBuilder.build(), null, DATE + " DESC LIMIT 1");
            if (cursor == null) {
                log("cursor is null...");
                return null;
            }
            if (0 == cursor.getCount()) {
                log("cursor count is 0");
                cursor.close();
                return null;
            }
            cursor.moveToFirst();
            String areaCode =  cursor.getString(0);
            cursor.close();
            return areaCode;
        }

        public static int updateConfirmFlag(Context context, String number, long confirm) {
            final ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues(1);
            values.put(CONFIRM, Long.valueOf(confirm));
            SelectionBuilder selectionBuilder = new SelectionBuilder(NUMBER + " = '" + number + "'");
            return resolver.update(CONTENT_URI_CALLS, values, selectionBuilder.build(), null);
        }

        public static int deleteNumber(Context context, String number) {
            final ContentResolver resolver = context.getContentResolver();
            String where = NUMBER + " = '" + number + "'";
            CallInfo callInfo = getCallInfo(context, number);
            if (null != callInfo && !TextUtils.isEmpty(callInfo.mCountryISO)) {
                CallOptionUtils.NumberInfo numberInfo
                        = CallOptionUtils.getNumberInfo(number, callInfo.mCountryISO);
                if (null != numberInfo) {
                    where += " OR " + NUMBER + " = '" + numberInfo.mSubscriber + "'";
                    if (!TextUtils.isEmpty(numberInfo.mAreaCode)) {
                        // have area code case
                        where += " OR " + NUMBER + " = '" + numberInfo.mAreaCodePrefix +
                                 numberInfo.mAreaCode + numberInfo.mSubscriber + "'";
                    }
                }
            }
            return resolver.delete(CONTENT_URI_CALLS, where, null);
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
