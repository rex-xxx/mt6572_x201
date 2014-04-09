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

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;

import com.android.contacts.util.UriUtils;
import com.mediatek.phone.HyphonManager;

/**
 * Information for a contact as needed by the Call Log.
 */
public final class ContactInfo {
    public Uri lookupUri;
    public String name;
    public int type;
    public String label;
    public String number;
    public String formattedNumber;
    public String normalizedNumber;
    /** The photo for the contact, if available. */
    public long photoId;
    /** The high-res photo for the contact, if available. */
    public Uri photoUri;

    public static ContactInfo EMPTY = new ContactInfo();

    @Override
    public int hashCode() {
        // Uses only name and contactUri to determine hashcode.
        // This should be sufficient to have a reasonable distribution of hash codes.
        // Moreover, there should be no two people with the same lookupUri.
        final int prime = 31;
        int result = 1;
        result = prime * result + ((lookupUri == null) ? 0 : lookupUri.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ContactInfo other = (ContactInfo) obj;
        if (!UriUtils.areEqual(lookupUri, other.lookupUri)) return false;
        if (!TextUtils.equals(name, other.name)) return false;
        if (type != other.type) return false;
        if (!TextUtils.equals(label, other.label)) return false;
        if (!TextUtils.equals(number, other.number)) return false;
        if (!TextUtils.equals(formattedNumber, other.formattedNumber)) return false;
        if (!TextUtils.equals(normalizedNumber, other.normalizedNumber)) return false;
        if (photoId != other.photoId) return false;
        if (!UriUtils.areEqual(photoUri, other.photoUri)) return false;
        return true;
    }

    /** M: add @ { */
    //-1 indicates phone contacts, >0 indicates sim id for sim contacts.
    public int simId;
    public long duration;
    public String countryIso;
    public int vtCall;
    public String geocode;
    public int contactSimId;
    public long date;
    public int nNumberTypeId;
    public int isRead;
    public String ipPrefix;

    public static ContactInfo fromCursor(Cursor c) {
        if (null == c) {
            new Exception("ContactInfo.fromCursor(c) - c is null").printStackTrace();
            return null;
        }
        ContactInfo newContactInfo = new ContactInfo();
        if (null != newContactInfo) {
            try {
                newContactInfo.number = c.getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_NUMBER);
                newContactInfo.date = c.getLong(CallLogQuery.CALLS_JOIN_DATA_VIEW_DATE);
                newContactInfo.duration = c.getLong(CallLogQuery.CALLS_JOIN_DATA_VIEW_DURATION);
                newContactInfo.type = c.getInt(CallLogQuery.CALLS_JOIN_DATA_VIEW_CALL_TYPE);
                newContactInfo.countryIso = c
                        .getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_COUNTRY_ISO);
                newContactInfo.simId = c.getInt(CallLogQuery.CALLS_JOIN_DATA_VIEW_SIM_ID);
                newContactInfo.vtCall = c.getInt(CallLogQuery.CALLS_JOIN_DATA_VIEW_VTCALL);
                newContactInfo.name = c.getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_DISPLAY_NAME);
                newContactInfo.nNumberTypeId = c
                        .getInt(CallLogQuery.CALLS_JOIN_DATA_VIEW_CALL_NUMBER_TYPE_ID);
                newContactInfo.label = c
                        .getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_CALL_NUMBER_TYPE);
                newContactInfo.photoId = c.getLong(CallLogQuery.CALLS_JOIN_DATA_VIEW_PHOTO_ID);

                ///M: to fix number display order problem in CallLog in Arabic/Hebrew/Urdu
                String fmtNumber = HyphonManager.getInstance().formatNumber(newContactInfo.number);
                newContactInfo.formattedNumber = TextUtils.isEmpty(fmtNumber) ? fmtNumber : '\u202D' + fmtNumber + '\u202C';

                newContactInfo.geocode = c
                        .getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_GEOCODED_LOCATION);
                newContactInfo.contactSimId = c
                        .getInt(CallLogQuery.CALLS_JOIN_DATA_VIEW_INDICATE_PHONE_SIM);
                long contactId = c.getLong(CallLogQuery.CALLS_JOIN_DATA_VIEW_CONTACT_ID);
                String lookUp = c.getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_LOOKUP_KEY);
                newContactInfo.lookupUri = (contactId == 0) ? null : Contacts.getLookupUri(
                        contactId, lookUp);
                newContactInfo.isRead = c.getInt(CallLogQuery.CALLS_JOIN_DATA_VIEW_IS_READ);
                newContactInfo.ipPrefix = c.getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_IP_PREFIX);
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
        
        return newContactInfo;
    }
    /** @ }*/

}
