/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
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
 */

package com.android.exchange.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Entity;
import android.content.EntityIterator;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.SyncStateContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.provider.Account;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.AbstractSyncAdapter;
import com.android.exchange.adapter.ContactsSyncAdapter;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.adapter.ContactsSyncAdapter.EasBusiness;
import com.android.exchange.adapter.ContactsSyncAdapter.EasChildren;
import com.android.exchange.adapter.ContactsSyncAdapter.EasPersonal;
import com.android.exchange.provider.EmailContentSetupUtils;

public class ContactsSyncAdapterTests extends
        SyncAdapterTestCase<ContactsSyncAdapter> {
    protected static final String TEST_ACCOUNT_PREFIX = "__test";
    protected static final String TEST_ACCOUNT_SUFFIX = "@android.com";
    //public final android.accounts.Account mAccountManagerAccount;

    protected android.accounts.Account[] getExchangeAccounts() {
        return AccountManager.get(getContext()).getAccountsByType(
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
    }

    protected android.accounts.Account makeAccountManagerAccount(String username) {
        return new android.accounts.Account(username, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
    }

    protected void createAccountManagerAccount(String username) {
        final android.accounts.Account account = makeAccountManagerAccount(username);
        AccountManager.get(getContext()).addAccountExplicitly(account,
                "password", null);
    }

    protected Account setupProviderAndAccountManagerAccount(String username) {
        // Note that setupAccount creates the email address
        // username@android.com, so that's what
        // we need to use for the account manager
        createAccountManagerAccount(username + TEST_ACCOUNT_SUFFIX);
        return EmailContentSetupUtils.setupAccount(username, true,
                mProviderContext);
    }

    private boolean addContact(Account account, String[] status) {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        boolean result = false;
        // Create our RawContact
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI);
        builder.withValue(RawContacts.ACCOUNT_NAME, account.mEmailAddress);
        builder.withValue(RawContacts.ACCOUNT_TYPE, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
        builder.withValue(RawContacts.SOURCE_ID, status[0]);
        builder.withValue(RawContacts.SYNC1, status[1]);
        operationList.add(builder.build());

        try {
            mResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void testEasContactsSyncParser() throws IOException, CommandStatusException {
        String[] id = {"6577","6575"};
        EasSyncService service = getTestService();
        Account account = setupProviderAndAccountManagerAccount("mediatek");
        service.mAccount = account;
        service.mMailbox.mSyncKey = "sync-key";
        ContactsSyncAdapter adapter = new ContactsSyncAdapter(service);
        addContact(account, id);
        Serializer s = new Serializer();
        s.start(Tags.SYNC_SYNC).tag(Tags.SYNC_WAIT).start(Tags.SYNC_RESPONSES).tag(Tags.SYNC_WAIT)
        .start(Tags.SYNC_ADD).tag(Tags.SYNC_WAIT).data(Tags.SYNC_SERVER_ID, "6577")
        .data(Tags.SYNC_CLIENT_ID, "6575").data(Tags.SYNC_STATUS, "1").end()
        .start(Tags.SYNC_CHANGE).tag(Tags.SYNC_WAIT).data(Tags.SYNC_SERVER_ID, "6577")
        .data(Tags.SYNC_STATUS, "1").end().end().start(Tags.SYNC_COMMANDS).tag(Tags.SYNC_WAIT)
        .start(Tags.SYNC_CHANGE).tag(Tags.SYNC_WAIT).data(Tags.SYNC_SERVER_ID, "6577")
        .end().start(Tags.SYNC_ADD).tag(Tags.SYNC_WAIT)
        .data(Tags.SYNC_SERVER_ID, "6577").start(Tags.SYNC_APPLICATION_DATA).tag(Tags.SYNC_WAIT)
        .data(Tags.CONTACTS_FIRST_NAME, "zhang").data(Tags.CONTACTS_LAST_NAME, "san")
        .data(Tags.CONTACTS_MIDDLE_NAME, "wu").data(Tags.CONTACTS_SUFFIX, "suffix")
        .data(Tags.CONTACTS_COMPANY_NAME, "MTK").data(Tags.CONTACTS_JOB_TITLE, "Engineer")
        .data(Tags.CONTACTS_EMAIL1_ADDRESS, "123@163.com")
        .data(Tags.CONTACTS_BUSINESS2_TELEPHONE_NUMBER, "8782878800")
        .data(Tags.CONTACTS2_MMS, "13912345678").data(Tags.CONTACTS_BUSINESS_FAX_NUMBER, "7654321")
        .data(Tags.CONTACTS2_COMPANY_MAIN_PHONE, "13987654321")
        .data(Tags.CONTACTS_HOME_FAX_NUMBER, "87654321")
        .data(Tags.CONTACTS_HOME_TELEPHONE_NUMBER, "12345678")
        .data(Tags.CONTACTS_MOBILE_TELEPHONE_NUMBER, "13012345678")
        .data(Tags.CONTACTS_CAR_TELEPHONE_NUMBER, "13112345678")
        .data(Tags.CONTACTS_RADIO_TELEPHONE_NUMBER, "13212345678")
        .data(Tags.CONTACTS_PAGER_NUMBER, "13312345678")
        .data(Tags.CONTACTS_ASSISTANT_TELEPHONE_NUMBER, "13412345678")
        .data(Tags.CONTACTS2_IM_ADDRESS, "QQ:12345")
        .data(Tags.CONTACTS_BUSINESS_ADDRESS_CITY, "chengdu")
        .data(Tags.CONTACTS_BUSINESS_ADDRESS_COUNTRY, "china")
        .data(Tags.CONTACTS_BUSINESS_ADDRESS_POSTAL_CODE, "610041")
        .data(Tags.CONTACTS_BUSINESS_ADDRESS_STATE, "No")
        .data(Tags.CONTACTS_BUSINESS_ADDRESS_STREET, "NO.5")
        .data(Tags.CONTACTS_HOME_ADDRESS_CITY, "fushun")
        .data(Tags.CONTACTS_HOME_ADDRESS_COUNTRY, "china")
        .data(Tags.CONTACTS_HOME_ADDRESS_POSTAL_CODE, "643221")
        .data(Tags.CONTACTS_HOME_ADDRESS_STATE, "No")
        .data(Tags.CONTACTS_HOME_ADDRESS_STREET, "NO.2")
        .data(Tags.CONTACTS_OTHER_ADDRESS_CITY, "beijing")
        .data(Tags.CONTACTS_OTHER_ADDRESS_COUNTRY, "china")
        .data(Tags.CONTACTS_OTHER_ADDRESS_POSTAL_CODE, "100000")
        .data(Tags.CONTACTS_OTHER_ADDRESS_STATE, "No")
        .data(Tags.CONTACTS_OTHER_ADDRESS_STREET, "NO.1")
        .data(Tags.CONTACTS_YOMI_COMPANY_NAME, "YOMI_MTK")
        .data(Tags.CONTACTS_YOMI_FIRST_NAME, "YOMI_ZHANG")
        .data(Tags.CONTACTS_YOMI_LAST_NAME, "YOMI_SAN")
        .data(Tags.CONTACTS2_NICKNAME, "pig")
        .data(Tags.CONTACTS_ASSISTANT_NAME, "ASSISTANT")
        .data(Tags.CONTACTS2_MANAGER_NAME, "MANAGER")
        .data(Tags.CONTACTS_SPOUSE, "SPOUSE")
        .data(Tags.CONTACTS_DEPARTMENT, "DEVELOP")
        .data(Tags.CONTACTS_TITLE, "TITLE")
        .data(Tags.CONTACTS_OFFICE_LOCATION, "C11")
        .data(Tags.CONTACTS2_CUSTOMER_ID, "CUSTOMER_ID")
        .data(Tags.CONTACTS2_GOVERNMENT_ID, "GOVERNMENT_ID")
        .data(Tags.CONTACTS2_ACCOUNT_NAME, "account")
        .data(Tags.CONTACTS_ANNIVERSARY, "ANNIVERSARY")
        .data(Tags.CONTACTS_BIRTHDAY, "2010-02-23T16:00:00.000Z")
        .data(Tags.CONTACTS_WEBPAGE, "http://123.com")
        .data(Tags.CONTACTS_BODY, "I love this")
        .start(Tags.CONTACTS_CATEGORIES).tag(Tags.CONTACTS_PICTURE)
        .data(Tags.CONTACTS_CATEGORY, "group").end()
        .start(Tags.BASE_BODY).tag(Tags.BASE).data(Tags.BASE_DATA, "data").end()
        .start(Tags.CONTACTS_CHILDREN).tag(Tags.CONTACTS_PICTURE)
        .data(Tags.CONTACTS_CHILD, "child")
        .end().end().end()
        .start(Tags.SYNC_DELETE).tag(Tags.SYNC_WAIT).data(Tags.SYNC_SERVER_ID, "6577").end()
        .end().end().done();

        byte[] bytes = s.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        assertFalse(adapter.parse(bis));
        mResolver.delete(Uri.parse(ContactsContract.RawContacts.CONTENT_URI.toString()
                + "?" + ContactsContract.CALLER_IS_SYNCADAPTER + "=true"),
                ContactsContract.RawContacts._ID + ">0", null);
    }

    public void testsSendSyncOptions() throws IOException {
        EasSyncService service = getTestService();
        ContactsSyncAdapter adapter = new ContactsSyncAdapter(service);
        Serializer s = new Serializer();
        Double protocolVersion = 12.0;
        adapter.sendSyncOptions(protocolVersion, s, Eas.EAS_SYNC_IDLE);
        Serializer s1 = new Serializer();
        s1.start(Tags.SYNC_SUPPORTED);
        s1.tag(Tags.CONTACTS_FIRST_NAME);
        s1.tag(Tags.CONTACTS_LAST_NAME);
        s1.tag(Tags.CONTACTS_MIDDLE_NAME);
        s1.tag(Tags.CONTACTS_SUFFIX);
        s1.tag(Tags.CONTACTS_COMPANY_NAME);
        s1.tag(Tags.CONTACTS_JOB_TITLE);
        s1.tag(Tags.CONTACTS_EMAIL1_ADDRESS);
        s1.tag(Tags.CONTACTS_EMAIL2_ADDRESS);
        s1.tag(Tags.CONTACTS_EMAIL3_ADDRESS);
        s1.tag(Tags.CONTACTS_BUSINESS2_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS_BUSINESS_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS2_MMS);
        s1.tag(Tags.CONTACTS_BUSINESS_FAX_NUMBER);
        s1.tag(Tags.CONTACTS2_COMPANY_MAIN_PHONE);
        s1.tag(Tags.CONTACTS_HOME_FAX_NUMBER);
        s1.tag(Tags.CONTACTS_HOME_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS_HOME2_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS_MOBILE_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS_CAR_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS_RADIO_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS_PAGER_NUMBER);
        s1.tag(Tags.CONTACTS_ASSISTANT_TELEPHONE_NUMBER);
        s1.tag(Tags.CONTACTS2_IM_ADDRESS);
        s1.tag(Tags.CONTACTS2_IM_ADDRESS_2);
        s1.tag(Tags.CONTACTS2_IM_ADDRESS_3);
        s1.tag(Tags.CONTACTS_BUSINESS_ADDRESS_CITY);
        s1.tag(Tags.CONTACTS_BUSINESS_ADDRESS_COUNTRY);
        s1.tag(Tags.CONTACTS_BUSINESS_ADDRESS_POSTAL_CODE);
        s1.tag(Tags.CONTACTS_BUSINESS_ADDRESS_STATE);
        s1.tag(Tags.CONTACTS_BUSINESS_ADDRESS_STREET);
        s1.tag(Tags.CONTACTS_HOME_ADDRESS_CITY);
        s1.tag(Tags.CONTACTS_HOME_ADDRESS_COUNTRY);
        s1.tag(Tags.CONTACTS_HOME_ADDRESS_POSTAL_CODE);
        s1.tag(Tags.CONTACTS_HOME_ADDRESS_STATE);
        s1.tag(Tags.CONTACTS_HOME_ADDRESS_STREET);
        s1.tag(Tags.CONTACTS_OTHER_ADDRESS_CITY);
        s1.tag(Tags.CONTACTS_OTHER_ADDRESS_COUNTRY);
        s1.tag(Tags.CONTACTS_OTHER_ADDRESS_POSTAL_CODE);
        s1.tag(Tags.CONTACTS_OTHER_ADDRESS_STATE);
        s1.tag(Tags.CONTACTS_OTHER_ADDRESS_STREET);
        s1.tag(Tags.CONTACTS_YOMI_COMPANY_NAME);
        s1.tag(Tags.CONTACTS_YOMI_FIRST_NAME);
        s1.tag(Tags.CONTACTS_YOMI_LAST_NAME);
        s1.tag(Tags.CONTACTS2_NICKNAME);
        s1.tag(Tags.CONTACTS_ASSISTANT_NAME);
        s1.tag(Tags.CONTACTS2_MANAGER_NAME);
        s1.tag(Tags.CONTACTS_SPOUSE);
        s1.tag(Tags.CONTACTS_DEPARTMENT);
        s1.tag(Tags.CONTACTS_TITLE);
        s1.tag(Tags.CONTACTS_OFFICE_LOCATION);
        s1.tag(Tags.CONTACTS2_CUSTOMER_ID);
        s1.tag(Tags.CONTACTS2_GOVERNMENT_ID);
        s1.tag(Tags.CONTACTS2_ACCOUNT_NAME);
        s1.tag(Tags.CONTACTS_ANNIVERSARY);
        s1.tag(Tags.CONTACTS_BIRTHDAY);
        s1.tag(Tags.CONTACTS_WEBPAGE);
        s1.tag(Tags.CONTACTS_PICTURE);
        s1.end(); // SYNC_SUPPORTED

        String result = s.toString();
        String expected = s1.toString();
        assertEquals(expected, result);
        adapter.sendSyncOptions(protocolVersion, s, Eas.EAS_SYNC_NORMAL);
        s1.tag(Tags.SYNC_DELETES_AS_MOVES);
        s1.tag(Tags.SYNC_GET_CHANGES);
        s1.data(Tags.SYNC_WINDOW_SIZE, AbstractSyncAdapter.CONTACTS_WINDOW_SIZE);
        s1.start(Tags.SYNC_OPTIONS);
        s1.start(Tags.BASE_BODY_PREFERENCE);
        s1.data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_TEXT);
        s1.data(Tags.BASE_TRUNCATION_SIZE, Eas.EAS12_TRUNCATION_SIZE);
        s1.end();
        s1.end();

        result = s.toString();
        expected = s1.toString();
        assertEquals(expected, result);
    }

    public void testCleanUp() {
        String[][] array = {{"xiao ming", "13966666666", "beijibinghe@gmail.com"}};
        EasSyncService service = getTestService();
        Account account = setupProviderAndAccountManagerAccount("mtk");
        service.mAccount = account;
        service.mMailbox.mSyncKey = "sync-key-1";
        ContactsSyncAdapter adapter = new ContactsSyncAdapter(service);

        addContacts(array, 1);
        adapter.mDeletedIdList.add(new Long("0"));
        adapter.mUpdatedIdList.add(new Long("0"));
        adapter.cleanup();
        Cursor cursor = mResolver.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID},
                RawContacts.ACCOUNT_NAME + "=?", new String[] {"mtk@android.com"}, null);
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    public void addContacts(String[][] array, int num) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (int i = 0; i < num; i++) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts._ID, i)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                            ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT)
                    .build());
            // add name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, i)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, array[i][0])
                    .build());
            // add phone
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, i)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, array[i][1])
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,1)
                    .build());
            // add email
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, i)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, array[i][2])
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE,1)
                    .build());
            try {
                    mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
                } catch (Exception e){
                    e.printStackTrace();
                }
        }
    }

    public void addData(String[][] array, int num) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (int i = 0; i < num; i++) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE)
//                    .withValue(ContactsContract.RawContacts._ID, 0)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, "mtk@android.com")
//                    .withValue(ContactsContract.RawContacts.DIRTY, 1)
                    .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                            ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT)
                    .build());
            // add name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, array[i][0])
                    .build());
            // add phone
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, array[i][1])
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,1)
                    .build());
            // add email
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, array[i][2])
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE,1)
                    .build());
            addMoreInfo(ops);
            try {
                    mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
                } catch (Exception e){
                    e.printStackTrace();
                }
        }
    }

    protected void addMoreInfo(ArrayList<ContentProviderOperation> ops) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, 0)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Im.DATA, "12356")
                .withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, 4)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, "pig")
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Website.URL, "http://12306.com")
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, "IT")
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, "mtk")
                .withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, "AP")
                .withValue(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION, "HK")
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, "Must be happy!")
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Relation.DATA, "lily")
                .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, 1)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, 1)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        EasBusiness.CONTENT_ITEM_TYPE)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        EasChildren.CONTENT_ITEM_TYPE)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        EasPersonal.CONTENT_ITEM_TYPE)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        Event.CONTENT_ITEM_TYPE)
                .withValue(Event.TYPE, 3)
                .build());
    }

    public void testSendLocalChanges() throws Exception {
        String[][] array = {{"xiao ming", "13966666666", "beijibinghe@gmail.com"}};
        EasSyncService service = getTestService();
        Account account = setupProviderAndAccountManagerAccount("mtk");
        service.mAccount = account;
        service.mMailbox.mSyncKey = "sync-key-1";
        try {
            ContactsSyncAdapter adapter = new ContactsSyncAdapter(service);
            adapter.mService.mProtocolVersionDouble = 12.0;
            addData(array, 1);
            ContentProviderClient client = mResolver
                    .acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
            SyncStateContract.Helpers.set(client,
                    ContactsContract.SyncState.CONTENT_URI,
                    adapter.mAccountManagerAccount, service.mMailbox.mSyncKey
                            .getBytes());
            Serializer s = new Serializer();
            adapter.sendLocalChanges(s);
            adapter.setSyncKey("0", true);
            assertFalse(adapter.isSyncable());
        } finally {
            Configuration.shutDownTest();
        }
    }

    @Override
    public void tearDown() throws Exception {
        android.accounts.Account[] accounts = getExchangeAccounts();
        for (android.accounts.Account acct : accounts) {
            AccountManager.get(getContext()).removeAccount(acct, null, null);
        }
        super.tearDown();
    }
}
