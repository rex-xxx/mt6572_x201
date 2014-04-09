/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.android.internal.telephony;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import com.android.internal.telephony.PhoneConstants;

/**
 * {@hide}
 */
public class IccProvider extends ContentProvider {
    private static final String TAG = "IccProvider";
    private static final boolean DBG = true;

    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = new String[] {
            "index",
            "name",
            "number",
            "emails",
            "additionalNumber",
            "groupIds",
            "_id",
            "aas",
            "sne",
    };
    private static final int ADDRESS_SUPPORT_AAS = 8;
    private static final int ADDRESS_SUPPORT_SNE = 9;
    private static final String[] UPB_GRP_COLUMN_NAMES = new String[] {
            "index",
            "name"
    };

    // for google default
    private static final int ADN = 1;
    private static final int FDN = 2;
    private static final int SDN = 3;
    // MTK-START [mtk80601][111215][ALPS00093395]
    private static final int UPB = 4;
    // for MTK GEMINI
    private static final int ADN1 = 101;
    private static final int FDN1 = 102;
    private static final int SDN1 = 103;
    private static final int UPB1 = 104;
    private static final int ADN2 = 201;
    private static final int FDN2 = 202;
    private static final int SDN2 = 203;
    private static final int UPB2 = 204;
    // for MTK GEMINI 2.0+
    private static final int ADN3 = 301;
    private static final int FDN3 = 302;
    private static final int SDN3 = 303;
    private static final int UPB3 = 304;
    private static final int ADN4 = 401;
    private static final int FDN4 = 402;
    private static final int SDN4 = 403;
    private static final int UPB4 = 404;
    // MTK-END [mtk80601][111215][ALPS00093395]

    private static final String STR_TAG = "tag";
    private static final String STR_NUMBER = "number";
    private static final String STR_EMAILS = "emails";
    private static final String STR_PIN2 = "pin2";
    private static final String STR_ANR = "anr";
    private static final String STR_INDEX = "index";
    private static final UriMatcher URL_MATCHER =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // for google default
        URL_MATCHER.addURI("icc", "adn", ADN);
        URL_MATCHER.addURI("icc", "fdn", FDN);
        URL_MATCHER.addURI("icc", "sdn", SDN);
        // MTK-START [mtk80601][111215][ALPS00093395]
        URL_MATCHER.addURI("icc", "pbr", UPB);

        // for MTK GEMINI
        URL_MATCHER.addURI("icc", "adn1", ADN1);
        URL_MATCHER.addURI("icc", "adn2", ADN2);
        URL_MATCHER.addURI("icc", "fdn1", FDN1);
        URL_MATCHER.addURI("icc", "fdn2", FDN2);
        URL_MATCHER.addURI("icc", "sdn1", SDN1);
        URL_MATCHER.addURI("icc", "sdn2", SDN2);
        URL_MATCHER.addURI("icc", "pbr1", UPB1);
        URL_MATCHER.addURI("icc", "pbr2", UPB2);

        // for MTK GEMINI 2.0 +
        URL_MATCHER.addURI("icc", "adn3", ADN3);
        URL_MATCHER.addURI("icc", "adn4", ADN4);
        URL_MATCHER.addURI("icc", "fdn3", FDN3);
        URL_MATCHER.addURI("icc", "fdn4", FDN4);
        URL_MATCHER.addURI("icc", "sdn3", SDN3);
        URL_MATCHER.addURI("icc", "sdn4", SDN4);
        URL_MATCHER.addURI("icc", "pbr3", UPB3);
        URL_MATCHER.addURI("icc", "pbr4", UPB4);
		
        // MTK-END [mtk80601][111215][ALPS00093395]
    }
    // MTK-START [mtk80601][111215][ALPS00093395]
    public static final int ERROR_ICC_PROVIDER_NO_ERROR = 1;
    public static final int ERROR_ICC_PROVIDER_UNKNOWN = 0;
    public static final int ERROR_ICC_PROVIDER_NUMBER_TOO_LONG = -1;
    public static final int ERROR_ICC_PROVIDER_TEXT_TOO_LONG = -2;
    public static final int ERROR_ICC_PROVIDER_STORAGE_FULL = -3;
    public static final int ERROR_ICC_PROVIDER_NOT_READY = -4;
    public static final int ERROR_ICC_PROVIDER_PASSWORD_ERROR = -5;
    public static final int ERROR_ICC_PROVIDER_ANR_TOO_LONG = -6;
    public static final int ERROR_ICC_PROVIDER_GENERIC_FAILURE = -10;
    public static final int ERROR_ICC_PROVIDER_ADN_LIST_NOT_EXIST = -11;
    public static final int ERROR_ICC_PROVIDER_EMAIL_FULL = -12;
    public static final int ERROR_ICC_PROVIDER_EMAIL_TOOLONG = -13;

    // MTK-END [mtk80601][111215][ALPS00093395]

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        if (DBG) {
            log("query " + url);
        }
        int efType;
        int simId;
        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);
        Cursor results = loadFromEf(efType, simId);
        return results;
    }

    @Override
    public String getType(Uri url) {
        switch (URL_MATCHER.match(url)) {
            case ADN:
            case ADN1:
            case ADN2:
            case ADN3:
            case ADN4:
            case FDN:
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
            case SDN:
            case SDN1:
            case SDN2:
            case SDN3:
            case SDN4:
            case UPB:
            case UPB1:
            case UPB2:
            case UPB3:
            case UPB4:
                return "vnd.android.cursor.dir/sim-contact";

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        Uri resultUri;
        int efType;
        String pin2 = null;
        int simId;

        if (DBG) {
            log("insert " + url);
        }
        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);
        switch (match) {
            case FDN: /* default SIM */
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
                pin2 = initialValues.getAsString("pin2");
                break;
            case UPB: /* default USIM */
            case UPB1:
            case UPB2:
            case UPB3:
            case UPB4:
                efType = IccConstants.EF_ADN;
                break;
            default:
                break;
        }

        String tag = initialValues.getAsString("tag");
        String number = initialValues.getAsString("number");
        int result = 0;
        if (UPB == match || UPB1 == match || UPB2 == match || UPB3 == match || UPB4 == match) {
            String strGas = initialValues.getAsString("gas");
            String strAnr = initialValues.getAsString("anr");
            String strEmail = initialValues.getAsString("emails");
            if (ADDRESS_BOOK_COLUMN_NAMES.length >= ADDRESS_SUPPORT_AAS) {
                Integer aasIndex = initialValues.getAsInteger("aas");
                if (number == null) {
                    number = "";
                }
                if (tag == null) {
                    tag = "";
                }
                AdnRecord record = new AdnRecord(efType, 0, tag, number);
                record.setAnr(strAnr);
                record.setGrpIds(strGas);
                String[] emails = null;
                if (strEmail != null && !strEmail.equals("")) {
                    emails = new String[1];
                    emails[0] = strEmail;
                }
                record.setEmails(emails);
                if (aasIndex != null) {
                    record.setAasIndex(aasIndex);
                }
                if (ADDRESS_BOOK_COLUMN_NAMES.length >= ADDRESS_SUPPORT_SNE) {
                    String sne = initialValues.getAsString("sne");
                    record.setSne(sne);
                }
                result = updateUsimPBRecordsBySearchWithError(efType, new AdnRecord("", "", ""),
                        record, simId);
            } else {
                result = addUsimRecordToEf(efType, tag, number, strAnr, strEmail, strGas, simId);
            }
        } else {
            // TODO(): Read email instead of sending null.
            result = addIccRecordToEf(efType, tag, number, null, pin2, simId);
        }
        StringBuilder buf = new StringBuilder("content://icc/");

        if (result <= ERROR_ICC_PROVIDER_UNKNOWN) {
            buf.append("error/");
            buf.append(result);
        } else {

            switch (match) {
                case ADN: /* default SIM */
                    buf.append("adn/");
                    break;
                case ADN1:
                    buf.append("adn1/");
                    break;
                case ADN2:
                    buf.append("adn2/");
                    break;
                case ADN3:
                    buf.append("adn3/");
                    break;
                case ADN4:
                    buf.append("adn4/");
                    break;
                case FDN: /* default SIM */
                    buf.append("fdn/");
                    break;
                case FDN1:
                    buf.append("fdn1/");
                    break;
                case FDN2:
                    buf.append("fdn2/");
                    break;
                case FDN3:
                    buf.append("fdn3/");
                    break;
                case FDN4:
                    buf.append("fdn4/");
                    break;
                case UPB: /* default USIM */
                    buf.append("pbr/");
                    break;
                case UPB1:
                    buf.append("pbr1/");
                    break;
                case UPB2:
                    buf.append("pbr2/");
                    break;
                case UPB3:
                    buf.append("pbr3/");
                    break;
                case UPB4:
                    buf.append("pbr4/");
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Cannot insert into URL: " + url);
            }

            // TODO: we need to find out the rowId for the newly added record
            buf.append(result);
        }

        resultUri = Uri.parse(buf.toString());

        log(resultUri.toString());

        /*
         * // notify interested parties that an insertion happened
         * getContext().getContentResolver().notifyInsert( resultUri, rowID,
         * null);
         */

        return resultUri;
    }

    private String normalizeValue(String inVal) {
        int len = inVal.length();
        String retVal = inVal;

        if (inVal.charAt(0) == '\'' && inVal.charAt(len - 1) == '\'') {
            retVal = inVal.substring(1, len - 1);
        }

        return retVal;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int efType;
        int simId;

        if (DBG) {
            log("delete " + url);
        }
        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);
        switch (match) {
            case UPB:
            case UPB1:
            case UPB2:
            case UPB3:
            case UPB4:
                efType = IccConstants.EF_ADN;
                break;
            default:
                break;
        }
        // parse where clause
        String tag = "";
        String number = "";
        String[] emails = null;
        String pin2 = null;
        int nIndex = -1;

        String[] tokens = where.split("AND");
        int n = tokens.length;

        while (--n >= 0) {
            String param = tokens[n];
            if (DBG) {
                log("parsing '" + param + "'");
            }
            int index = param.indexOf('=');
            if (index == -1) {
                Log.e(TAG, "resolve: bad whereClause parameter: " + param);
                continue;
            }

            String key = param.substring(0, index).trim();
            String val = param.substring(index + 1).trim();
            log("parsing key is " + key + " index of = is " + index +
                    " val is " + val);

            /*
             * String[] pair = param.split("="); if (pair.length != 2) {
             * Log.e(TAG, "resolve: bad whereClause parameter: " + param);
             * continue; } String key = pair[0].trim(); String val =
             * pair[1].trim();
             */

            if (STR_INDEX.equals(key)) {
                nIndex = Integer.parseInt(val);
            } else if (STR_TAG.equals(key)) {
                tag = normalizeValue(val);
            } else if (STR_NUMBER.equals(key)) {
                number = normalizeValue(val);
            } else if (STR_EMAILS.equals(key)) {
                // TODO(): Email is null.
                emails = null;
            } else if (STR_PIN2.equals(key)) {
                pin2 = normalizeValue(val);
            }
        }

        int result = ERROR_ICC_PROVIDER_UNKNOWN;
        if (nIndex > 0) {
            log("delete index is " + nIndex);
            if (UPB == match || UPB1 == match || UPB2 == match || UPB3 == match || UPB4 == match) {
                result = deleteUsimRecordFromEfByIndex(efType, nIndex, simId);
            } else {
                result = deleteIccRecordFromEfByIndex(efType, nIndex, pin2, simId);
            }
            return result;
        }

        if (efType == IccConstants.EF_FDN && TextUtils.isEmpty(pin2)) {
            return ERROR_ICC_PROVIDER_PASSWORD_ERROR;
        }

        if (tag.length() == 0 && number.length() == 0) {
            return ERROR_ICC_PROVIDER_UNKNOWN;
        }

        if (UPB == match || UPB1 == match || UPB2 == match || UPB3 == match || UPB4 == match) {
            if (ADDRESS_BOOK_COLUMN_NAMES.length >= ADDRESS_SUPPORT_AAS) {
                result = updateUsimPBRecordsBySearchWithError(efType,
                        new AdnRecord(tag, number, ""), new AdnRecord("", "", ""), simId);
            } else {
                result = deleteUsimRecordFromEf(efType, tag, number, emails, simId);
            }
        } else {
            result = deleteIccRecordFromEf(efType, tag, number, emails, pin2, simId);
        }
        return result;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int efType;
        String pin2 = null;
        int simId;

        log("update " + url);

        int match = URL_MATCHER.match(url);
        efType = getRequestType(match);
        simId = getRequestSim(match);
        switch (match) {
            case FDN:
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
                pin2 = values.getAsString("pin2");
                break;
            case UPB:
            case UPB1:
            case UPB2:
            case UPB3:
            case UPB4:
                efType = IccConstants.EF_ADN;
                break;
            default:
                break;
        }

        String tag = values.getAsString("tag");
        String number = values.getAsString("number");

        String newTag = values.getAsString("newTag");
        String newNumber = values.getAsString("newNumber");
        Integer idInt = values.getAsInteger("index");
        int index = 0;
        if (idInt != null) {
            index = idInt.intValue();
        }
        log("update: index=" + index);
        int result = 0;
        if (UPB == match || UPB1 == match || UPB2 == match || UPB3 == match || UPB4 == match) {
            String strAnr = values.getAsString("newAnr");
            String strEmail = values.getAsString("newEmails");

            Integer aasIndex = values.getAsInteger("aas");
            String sne = values.getAsString("sne");
            if (newNumber == null) {
                newNumber = "";
            }
            if (newTag == null) {
                newTag = "";
            }
            AdnRecord record = new AdnRecord(efType, 0, newTag, newNumber);
            record.setAnr(strAnr);
            String[] emails = null;
            if (strEmail != null && !strEmail.equals("")) {
                emails = new String[1];
                emails[0] = strEmail;
            }
            record.setEmails(emails);
            if (aasIndex != null) {
                record.setAasIndex(aasIndex);
            }
            if (sne != null) {
                record.setSne(sne);
            }
            if (index > 0) {
                if (ADDRESS_BOOK_COLUMN_NAMES.length >= ADDRESS_SUPPORT_AAS) {
                    result = updateUsimPBRecordsByIndexWithError(efType, record, index, simId);
                } else {
                    result = updateUsimRecordInEfByIndex(efType, index, newTag, newNumber, strAnr,
                            strEmail, simId);
                }
            } else {
                if (ADDRESS_BOOK_COLUMN_NAMES.length >= ADDRESS_SUPPORT_AAS) {
                    result = updateUsimPBRecordsBySearchWithError(efType, new AdnRecord(tag,
                            number, ""), record, simId);
                } else {
                    result = updateUsimRecordInEf(efType, tag, number, newTag, newNumber, strAnr,
                            strEmail, simId);
                }

            }
        } else {
            if (index > 0) {
                result = updateIccRecordInEfByIndex(efType, index, newTag, newNumber, pin2, simId);
            } else {
                result = updateIccRecordInEf(efType, tag, number, newTag, newNumber, pin2, simId);
            }
        }
        return result;
    }

    private MatrixCursor loadFromEf(int efType, int simId) {
        List<AdnRecord> adnRecords = null;

        if (DBG) {
            log("loadFromEf: efType=" + efType);
        }
        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                adnRecords = iccIpb.getAdnRecordsInEf(efType);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        if (adnRecords != null) {
            // Load the results
            final int size = adnRecords.size();
            final MatrixCursor cursor = new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES, size);
            if (DBG) {
                log("adnRecords.size=" + size);
            }
            for (int i = 0; i < size; i++) {
                loadRecord(adnRecords.get(i), cursor, i);
            }
            return cursor;
        } else {
            // No results to load
            Log.w(TAG, "Cannot load ADN records");
            return new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES);
        }
    }

    private int addIccRecordToEf(int efType, String name,
            String number, String[] emails, String pin2, int simId) {

        if (DBG) {
            log("addIccRecordToEf: efType=" + efType + ", name=" + name +
                    ", number=" + number + ", emails=" + emails + ", simId=" + simId);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        // TODO: do we need to call getAdnRecordsInEf() before calling
        // updateAdnRecordsInEfBySearch()? In any case, we will leave
        // the UI level logic to fill that prereq if necessary. But
        // hopefully, we can remove this requirement.

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateAdnRecordsInEfBySearchWithError(efType, "", "",
                        name, number, pin2);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("addIccRecordToEf: " + result);
        return result;
    }

    // MTK-START [mtk80601][111215][ALPS00093395]
    /* Insert USIM contact record */
    private int addUsimRecordToEf(int efType, String name, String number, String strAnr,
            String strEmail, String strGas, int simId) {

        if (DBG) {
            log("addUSIMRecordToEf: efType=" + efType + ", name=" + name +
                    ", number=" + number + ", anr =" + strAnr + ", emails=" + strEmail + ", simId="
                    + simId);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        // TODO: do we need to call getAdnRecordsInEf() before calling
        // updateAdnRecordsInEfBySearch()? In any case, we will leave
        // the UI level logic to fill that prereq if necessary. But
        // hopefully, we can remove this requirement.
        String[] emails = null;
        if (strEmail != null && !strEmail.equals("")) {
            emails = new String[1];
            emails[0] = strEmail;
        }

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateUsimPBRecordsInEfBySearchWithError(efType,
                        "", "", "", null, null, name, number, strAnr, null, emails);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("addUsimRecordToEf: " + result);
        return result;
    }

    private int updateIccRecordInEf(int efType, String oldName, String oldNumber,
            String newName, String newNumber, String pin2, int simId) {
        if (DBG) {
            log("updateIccRecordInEf: efType=" + efType +
                    ", oldname=" + oldName + ", oldnumber=" + oldNumber +
                    ", newname=" + newName + ", newnumber=" + newNumber);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateAdnRecordsInEfBySearchWithError(efType,
                        oldName, oldNumber, newName, newNumber, pin2);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateIccRecordInEf: " + result);
        return result;
    }

    private int updateIccRecordInEfByIndex(int efType, int nIndex, String newName,
            String newNumber, String pin2, int simId) {
        if (DBG) {
            log("updateIccRecordInEfByIndex: efType=" + efType + ", index=" + nIndex
                    + ", newname=" + newName + ", newnumber=" + newNumber);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateAdnRecordsInEfByIndexWithError(efType,
                        newName, newNumber, nIndex, pin2);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateIccRecordInEfByIndex: " + result);
        return result;
    }

    private int updateUsimRecordInEf(int efType, String oldName, String oldNumber,
            String newName, String newNumber, String strAnr, String strEmail, int simId) {

        if (DBG) {
            log("updateUsimRecordInEf: efType=" + efType +
                    ", oldname=" + oldName + ", oldnumber=" + oldNumber +
                    ", newname=" + newName + ", newnumber=" + newNumber + ", anr =" + strAnr
                    + ", emails=" + strEmail);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        String[] emails = null;
        if (strEmail != null) {
            emails = new String[1];
            emails[0] = strEmail;
        }

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateUsimPBRecordsInEfBySearchWithError(efType,
                        oldName, oldNumber, "", null, null, newName, newNumber, strAnr, null,
                        emails);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateUsimRecordInEf: " + result);
        return result;
    }

    private int updateUsimRecordInEfByIndex(int efType, int nIndex, String newName,
            String newNumber,
            String strAnr, String strEmail, int simId) {

        if (DBG) {
            log("updateUsimRecordInEfByIndex: efType=" + efType + ", Index=" + nIndex
                    + ", newname=" + newName +
                    ", newnumber=" + newNumber + ", anr =" + strAnr + ", emails=" + strEmail);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        String[] emails = null;
        if (strEmail != null) {
            emails = new String[1];
            emails[0] = strEmail;
        }

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateUsimPBRecordsInEfByIndexWithError(efType,
                        newName, newNumber, strAnr, null, emails, nIndex);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateUsimRecordInEfByIndex: " + result);
        return result;
    }

    // MTK-END [mtk80601][111215][ALPS00093395]
    private int deleteIccRecordFromEf(int efType, String name,
            String number, String[] emails, String pin2, int simId) {
        if (DBG) {
            log("deleteIccRecordFromEf: efType=" + efType +
                    ", name=" + name + ", number=" + number + ", emails=" + emails + ", pin2="
                    + pin2);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateAdnRecordsInEfBySearchWithError(efType,
                        name, number, "", "", pin2);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("deleteIccRecordFromEf: " + result);
        return result;
    }

    // MTK-START [mtk80601][111215][ALPS00093395]
    private int deleteIccRecordFromEfByIndex(int efType, int nIndex, String pin2, int simId) {
        if (DBG) {
            log("deleteIccRecordFromEfByIndex: efType=" + efType +
                    ", index=" + nIndex + ", pin2=" + pin2);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateAdnRecordsInEfByIndexWithError(efType, "", "", nIndex, pin2);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("deleteIccRecordFromEfByIndex: " + result);
        return result;
    }

    private int deleteUsimRecordFromEf(int efType, String name,
            String number, String[] emails, int simId) {
        if (DBG) {
            log("deleteUsimRecordFromEf: efType=" + efType +
                    ", name=" + name + ", number=" + number + ", emails=" + emails);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateUsimPBRecordsInEfBySearchWithError(efType,
                        name, number, "", null, null, "", "", "", null, null);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("deleteUsimRecordFromEf: " + result);
        return result;
    }

    private int deleteUsimRecordFromEfByIndex(int efType, int nIndex, int simId) {
        if (DBG) {
            log("deleteUsimRecordFromEfByIndex: efType=" + efType + ", index=" + nIndex);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateUsimPBRecordsInEfByIndexWithError(efType,
                        "", "", "", null, null, nIndex);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("deleteUsimRecordFromEfByIndex: " + result);
        return result;
    }

    // MTK-END [mtk80601][111215][ALPS00093395]

    /**
     * Loads an AdnRecord into a MatrixCursor. Must be called with mLock held.
     * 
     * @param record the ADN record to load from
     * @param cursor the cursor to receive the results
     */
    private void loadRecord(AdnRecord record, MatrixCursor cursor, int id) {
        int len = ADDRESS_BOOK_COLUMN_NAMES.length;
        if (!record.isEmpty()) {
            Object[] contact = new Object[len];
            String alphaTag = record.getAlphaTag();
            String number = record.getNumber();
            String[] emails = record.getEmails();
            String anr = record.getAdditionalNumber();
            String grpIds = record.getGrpIds();
            String index = Integer.toString(record.getRecordIndex());

            if (len >= ADDRESS_SUPPORT_AAS) {
                int aasIndex = record.getAasIndex();
                contact[7] = aasIndex;
            }
            if (len >= ADDRESS_SUPPORT_SNE) {
                String sne = record.getSne();
                contact[8] = sne;
            }
            if (DBG) {
                log("loadRecord: record:" + record);
            }
            contact[0] = index;
            contact[1] = alphaTag;
            contact[2] = number;

            if (emails != null) {
                StringBuilder emailString = new StringBuilder();
                for (String email : emails) {
                    if (DBG) {
                        log("Adding email:" + email);
                    }
                    emailString.append(email);
                    emailString.append(",");
                }
                contact[3] = emailString.toString();
            }
            contact[4] = anr;
            contact[5] = grpIds;
            contact[6] = id;
            cursor.addRow(contact);
        }
    }

    private void log(String msg) {
        Log.d(TAG, "[IccProvider] " + msg);
    }

    // MTK-START [mtk80601][111215][ALPS00093395]
    private IIccPhoneBook getIccPhbService(int simId) {

        IIccPhoneBook iccIpb;
        if (simId == PhoneConstants.GEMINI_SIM_1) {
            iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
        } else if (simId == PhoneConstants.GEMINI_SIM_2){
            iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook2"));
        } else if (simId == PhoneConstants.GEMINI_SIM_3){
            iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook3"));
        } else {
            iccIpb = IIccPhoneBook.Stub.asInterface(
            ServiceManager.getService("simphonebook4"));
        }

        return iccIpb;
    }

    private int getDefaultSim() {
        return SystemProperties.getInt(
                PhoneConstants.GEMINI_DEFAULT_SIM_PROP,
                PhoneConstants.GEMINI_SIM_1);
    }

    private int updateUsimPBRecordsBySearchWithError(int efType, AdnRecord oldAdn,
            AdnRecord newAdn, int simId) {
        if (DBG) {
            log("updateUsimRecordBySearch simId:" + simId + ",oldAdn:" + oldAdn + ",newAdn:"
                    + newAdn);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateUsimPBRecordsBySearchWithError(efType, oldAdn, newAdn);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateUsimRecordInEf: " + result);
        return result;
    }

    private int updateUsimPBRecordsByIndexWithError(int efType, AdnRecord newAdn, int index,
            int simId) {
        if (DBG) {
            log("updateUsimRecordByIndex simId:" + simId + ",index:" + index + ",newAdn:" + newAdn);
        }
        int result = ERROR_ICC_PROVIDER_UNKNOWN;

        try {
            IIccPhoneBook iccIpb = getIccPhbService(simId);

            if (iccIpb != null) {
                result = iccIpb.updateUsimPBRecordsByIndexWithError(efType, newAdn, index);
            }
        } catch (RemoteException ex) {
            log(ex.toString());
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateUsimRecordInEf: " + result);
        return result;
    }

    private int getRequestType(int match) {
        switch (match) {
            case ADN:
            case ADN1:
            case ADN2:
            case ADN3:
            case ADN4:
                return IccConstants.EF_ADN;
            case FDN:
            case FDN1:
            case FDN2:
            case FDN3:
            case FDN4:
                return IccConstants.EF_FDN;
            case SDN:
            case SDN1:
            case SDN2:
            case SDN3:
            case SDN4:
                return IccConstants.EF_SDN;
            case UPB:
            case UPB1:
            case UPB2:
            case UPB3:
            case UPB4:
                return IccConstants.EF_PBR;
            default:
                throw new IllegalArgumentException("Unknown URL " + match);
        }
    }

    private int getRequestSim(int match) {
        switch (match) {
            case ADN:
            case FDN:
            case SDN:
            case UPB:
                return getDefaultSim();
            case ADN1:
            case FDN1:
            case SDN1:
            case UPB1:
                return PhoneConstants.GEMINI_SIM_1;
            case ADN2:
            case FDN2:
            case SDN2:
            case UPB2:
                return PhoneConstants.GEMINI_SIM_2;
            case ADN3:
            case FDN3:
            case SDN3:
            case UPB3:
                return PhoneConstants.GEMINI_SIM_3;
            case ADN4:
            case FDN4:
            case SDN4:
            case UPB4:
                return PhoneConstants.GEMINI_SIM_4;
            default:
                throw new IllegalArgumentException("Unknown URL " + match);
        }
    }
    // MTK-END [mtk80601][111215][ALPS00093395]
}
