/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.android.bluetooth.pbap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;

import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
//import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.AdnRecord; //import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ITelephony;
import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConfig;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.obex.ResponseCodes;

public class BluetoothPbapSimAdn {
    private static final String TAG = "BluetoothPbapSimAdn";

    public static final boolean DEBUG = true;

    static final Uri ICC_URI = Uri.parse("content://icc/adn/");// 80794

    static final Uri ICC_URI1 = Uri.parse("content://icc/adn1/");

    static final Uri ICC_URI2 = Uri.parse("content://icc/adn2/");

    static final Uri ICC_USIM_URI = Uri.parse("content://icc/pbr");// 80794 for
                                                                  // CU operator

    static final Uri ICC_USIM1_URI = Uri.parse("content://icc/pbr1/");

    static final Uri ICC_USIM2_URI = Uri.parse("content://icc/pbr2/");

    private String mVCardPath = null;

    private final ITelephony mTel = ITelephony.Stub.asInterface(ServiceManager
            .getService(Context.TELEPHONY_SERVICE));

    private Context mContext = null;

    private List<AdnRecord> mListAdn = null;

    private int mErrCode = ResponseCodes.OBEX_HTTP_OK;

    public BluetoothPbapSimAdn(Context context) {
        mContext = context;
    }

    public class AdnComparator implements Comparator<AdnRecord> {
        private final String mEmpty = "";

        public int compare(AdnRecord adn1, AdnRecord adn2) {
            String name1 = adn1.getAlphaTag();
            String name2 = adn2.getAlphaTag();
            if (name1 == null) {
                name1 = mEmpty;
            }
            if (name2 == null) {
                name2 = mEmpty;
            }
            return name1.compareToIgnoreCase(name2);
        }
    }

    public boolean updateAdn() {
        Cursor cursor = null;
        Uri uri = null;
        int indexColIdx;
        int nameColIdx;
        int numberColIdx;
        int emailsColIdx;
        int anrColIdx;
        int groupColIdx;

        mErrCode = ResponseCodes.OBEX_HTTP_OK;
        uri = getSIMUri();
        if (uri == null) {
            return false;
        }
        log("uri=" + uri.toString());
        try {
            cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int i;
                int count = cursor.getCount();
                log("ADN count == " + count);
                indexColIdx = cursor.getColumnIndex("index");
                nameColIdx = cursor.getColumnIndex("name");
                numberColIdx = cursor.getColumnIndex("number");
                emailsColIdx = cursor.getColumnIndex("emails");
                anrColIdx = cursor.getColumnIndex("additionalNumber");
                groupColIdx = cursor.getColumnIndex("groupIds");
                mListAdn = new LinkedList();
                // listSIMColumns();
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    log("cursor.getString(name) = " + cursor.getString(nameColIdx));
                    log("cursor.getString(number) = " + cursor.getString(numberColIdx));
                    log("cursor.getString(emailsColIdx) = " + cursor.getString(emailsColIdx));
                    log("cursor.getString(additionalNumber) = " + cursor.getString(anrColIdx));
                    log("cursor.getString(groupIds) = " + cursor.getString(groupColIdx));
                    String email = cursor.getString(emailsColIdx);
                    String[] emails = null;
                    if (!TextUtils.isEmpty(email)) {
                        emails = email.split(",");
                        log("emails.length = " + emails.length);
                    }
                    mListAdn.add(new AdnRecord(0, cursor.getInt(indexColIdx), cursor
                            .getString(nameColIdx), cursor.getString(numberColIdx), cursor
                            .getString(anrColIdx), emails, null));
                    cursor.moveToNext();
                }
            } else {
                mErrCode = ResponseCodes.OBEX_HTTP_FORBIDDEN;
                errorLog("query ADN failed.");
                return false;
            }
            return true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void searchAdn(String search, boolean searchNnumber) {
        AdnRecord adn;
        String name;
        // log("searchAdn("+search+", "+String.valueOf(searchNnumber)+")");
        if (search != null && mListAdn != null) {
            ListIterator<AdnRecord> iterator = mListAdn.listIterator(0);
            if (iterator != null) {
                if (!searchNnumber) {
                    search = search.toUpperCase();
                    while (iterator.hasNext()) {
                        adn = iterator.next();
                        name = adn.getAlphaTag();
                        if (name == null || !name.toUpperCase().contains(search)) {
                            // log("name="+name.toUpperCase()+"search=");
                            iterator.remove();
                        }
                    }
                } else {
                    while (iterator.hasNext()) {
                        adn = iterator.next();
                        name = adn.getNumber();
                        if (name == null || !name.toUpperCase().contains(search)) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    public void sortAdn() {
        if (mListAdn != null) {
            Collections.sort(mListAdn, new AdnComparator());
        }
    }

    public int getCount() {
        if (mListAdn != null) {
            return mListAdn.size();
        }
        return 0;
    }

    public List<AdnRecord> getAdnList() {
        return mListAdn;
    }

    public String getVCardFilePath() {
        return mVCardPath;
    }

    public int composeVCard(boolean vcard21, int listOffset, int maxListCount, boolean email,
            boolean incOwner) {
        int ret = ResponseCodes.OBEX_HTTP_OK;
        VCardBuilder builder = null;
        BluetoothPbapWriter writer = null;
        ContentValues values = null;
        LinkedList<ContentValues> list;
        int slotId;
        AdnRecord adn;
        ListIterator<AdnRecord> iterator = null;

        log("composeVCard");

        writer = new BluetoothPbapWriter();
        writer.init(mContext);
        builder = new VCardBuilder(vcard21 ? VCardConfig.VCARD_TYPE_V21_GENERIC
                : VCardConfig.VCARD_TYPE_V30_GENERIC, "UTF-8");
        if (incOwner) {
            if (listOffset == 0) {
                if (maxListCount > 0) {
                    writer.write(getOwnerRecord(builder));
                    maxListCount--;
                }
            } else {
                listOffset--;
            }
        }

        log("listOffset=" + listOffset + ", maxListCount = " + maxListCount);

        if (mListAdn != null) {
            log("mListAdn.size = " + mListAdn.size());
            if (mListAdn.size() > listOffset) {
                iterator = mListAdn.listIterator(listOffset);
                list = new LinkedList<ContentValues>();
                if (maxListCount > (mListAdn.size() - listOffset)) {
                    maxListCount = (mListAdn.size() - listOffset);
                }
                while (maxListCount > 0 && iterator.hasNext()) {
                    adn = iterator.next();
                    builder.clear();
                    appendName(adn, list);
                    builder.appendNameProperties(list);
                    list.clear();
                    appendNumber(adn, list);
                    builder.appendPhones(list, null);
                    list.clear();
                    if (email) {
                        appendEmails(adn, list);
                        builder.appendEmails(list);
                        list.clear();
                    }
                    writer.write(builder.toString());
                    maxListCount--;
                }
            } else {
                ret = ResponseCodes.OBEX_HTTP_NOT_FOUND;
            }
        } else {
            ret = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }
        mVCardPath = writer.getPath();
        writer.terminate();
        return ret;
    }

    public String getOwnerName() {
        String ownName = null;
        //TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        TelephonyManagerEx tmEx = TelephonyManagerEx.getDefault();
        int slotId = getDefaultSIM();
        //ownName = tm.getLine1AlphaTagGemini(slotId);
        ownName = tmEx.getLine1AlphaTag(slotId);
        if (TextUtils.isEmpty(ownName)) {
            //ownName = tm.getLine1NumberGemini(slotId);
            ownName = tmEx.getLine1Number(slotId);
        }
        return (ownName == null) ? "" : ownName;

    }

    private void appendName(AdnRecord adn, List<ContentValues> list) {
        ContentValues valuse;
        String name = adn.getAlphaTag();
        if (list != null) {
            if (name == null) {
                name = "";
            }
            log("appendName : name=" + name);
            valuse = new ContentValues(1);
            valuse.put(StructuredName.DISPLAY_NAME, name);
            list.add(valuse);
        }
    }

    private void appendNumber(AdnRecord adn, List<ContentValues> list) {
        ContentValues values;
        String number = adn.getNumber();
        if (list != null) {
            if (!TextUtils.isEmpty(number)) {
                values = new ContentValues(2);
                values.put(Phone.NUMBER, number);
                log("appendNumber : number=" + number);
                values.put(Phone.IS_PRIMARY, 1);
                list.add(values);
                number = adn.getAdditionalNumber();
                if (!TextUtils.isEmpty(number)) {
                    log("Put additional number = " + number);
                    values = new ContentValues(1);
                    values.put(Phone.NUMBER, number);
                    list.add(values);
                }
            }
        }
    }

    private void appendEmails(AdnRecord adn, List<ContentValues> list) {
        ContentValues values;
        String[] emails = adn.getEmails();
        if (list != null && emails != null) {
            for (String email : emails) {
                if (!TextUtils.isEmpty(email)) {
                    values = new ContentValues(1);
                    values.put(Email.DATA, email);
                    list.add(values);
                }
            }
        }
    }

    private String getOwnerRecord(VCardBuilder builder) {
        String ownName = null;
        String ownNumber = null;
        //TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        TelephonyManagerEx tmEx = TelephonyManagerEx.getDefault();
        int slotId = getDefaultSIM();
        ContentValues values = new ContentValues();
        ArrayList list = new ArrayList(1);

        //ownName = tm.getLine1AlphaTagGemini(slotId);
        ownName = tmEx.getLine1AlphaTag(slotId);
        if (ownName == null) {
            ownName = "";
        }
        //ownNumber = tm.getLine1NumberGemini(slotId);
        ownNumber = tmEx.getLine1Number(slotId);
        if (ownNumber == null) {
            ownNumber = "";
        }
        log("getOwnerRecord : name=" + ownName + ", number=" + ownNumber);
        builder.clear();
        values.put(StructuredName.DISPLAY_NAME, ownName);
        list.add(values);
        builder.appendNameProperties(list);
        if (!TextUtils.isEmpty(ownNumber)) {
            values = new ContentValues();
            values.put(Phone.NUMBER, ownNumber);
            values.put(Phone.TYPE, Phone.TYPE_MOBILE);
            values.put(Phone.IS_PRIMARY, 1);
            list.add(values);
            builder.appendPhones(list, null);
        }
        return builder.toString();
    }

    private Uri getSIMUri() {
        try {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                int slotId = getDefaultSIM();
                if (mTel != null && mTel.getIccCardTypeGemini(slotId).equals("USIM")) {
                    if (slotId == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2) {
                        return ICC_USIM2_URI;
                    } else {
                        return ICC_USIM1_URI;
                    }
                } else {
                    if (slotId == com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2) {
                        return ICC_URI2;
                    } else {
                        return ICC_URI1;
                    }
                }
            } else {
                if (mTel != null && mTel.getIccCardType().equals("USIM")) {
                    return ICC_USIM_URI;
                } else {
                    return ICC_URI;
                }
            }
        } catch (RemoteException ex) {
            mErrCode = ResponseCodes.OBEX_HTTP_NOT_FOUND;
            log("getSIMUri : caught exception " + ex.toString());
            return null;
        }
    }

    public int getLastError() {
        return mErrCode;
    }

    private int getDefaultSIM() {
        int slotId = (int) Settings.System.getLong(mContext.getContentResolver(),
                Settings.System.VOICE_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
        log("getDefaultSIM : SIM ID=" + slotId);
        if (slotId == Settings.System.DEFAULT_SIM_NOT_SET
                || slotId == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
            log("No default SIM, get first inserted SIM");
            try {
                if (mTel.isSimInsert(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1)) {
                    log("getDefaultSim is sim1");
                    return com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1;
                } else {
                    if (mTel.isSimInsert(com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2)) {
                        log("getDefaultSim is sim2");
                        return com.android.internal.telephony.PhoneConstants.GEMINI_SIM_2;
                    } else {
                        log("getDefaultSim is no sim");
                        return com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1;
                    }
                }
            } catch (RemoteException ex) {
                return com.android.internal.telephony.PhoneConstants.GEMINI_SIM_1;
            }
        } else {
            slotId = SIMInfo.getSlotById(mContext, slotId);
            log("getDefaultSIM : slot ID=" + slotId);
        }

        return slotId;
    }

    private void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private void errorLog(String msg) {
        Log.e(TAG, msg);
    }

    private void listSIMColumns() {
        Uri uri = getSIMUri();
        Cursor cursor = null;
        try {
            if (uri != null) {
                cursor = mContext.getContentResolver().query(uri, null, null, null, null);
                if (cursor == null) {
                    log("listSIMColumns : query SIM failed");
                } else {
                    int count = cursor.getColumnCount();
                    log("listSIMColumns : column count = " + cursor.getColumnCount());
                    for (int i = 0; i < count; i++) {
                        log("column " + i + " : " + cursor.getColumnName(i));
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
