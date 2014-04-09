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

package com.android.exchange.utility;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.provider.Account;
import com.android.exchange.ExchangeService;
import com.android.exchange.provider.EmailContentSetupUtils;

public abstract class ExchangeTestCase extends AndroidTestCase {
    /// M: define some constant values
    public static final String TAG = "ExchangeTestCase";
    public static final String EXCHANGE_INTENT = "com.android.email.EXCHANGE_INTENT";

    private final ArrayList<Long> mCreatedAccountIds = new ArrayList<Long>();
    protected Context mProviderContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        // Could use MockContext here if we switch over
        mProviderContext = mContext;
        deleteAllExistedAccount();
        Configuration.openTest();
    }

    @Override
    public void tearDown() throws Exception {
        Configuration.shutDownTest();
        ContentResolver resolver = mProviderContext.getContentResolver();
        for (Long accountId : mCreatedAccountIds) {
            resolver.delete(ContentUris.withAppendedId(Account.CONTENT_URI,
                    accountId), null, null);
        }
        super.tearDown();
    }

    /**
     * M: Delete all the existed account
     */
    protected void deleteAllExistedAccount() {
        ContentResolver resolver = mProviderContext.getContentResolver();
        resolver.delete(Account.CONTENT_URI, null, null);
    }

    /**
     * Add an account to our list of test accounts; we'll delete it
     * automatically in tearDown()
     *
     * @param account
     *            the account to be added to our list of test accounts
     */
    protected void addTestAccount(Account account) {
        if (account.mId > 0) {
            mCreatedAccountIds.add(account.mId);
        }
    }

    /**
     * Create a test account that will be automatically deleted when the test is
     * finished
     *
     * @param name
     *            the name of the account
     * @param saveIt
     *            whether or not to save the account in EmailProvider
     * @return the account created
     */
    protected Account setupTestAccount(String name, boolean saveIt) {
        Account account = EmailContentSetupUtils.setupAccount(name, saveIt,
                mProviderContext);
        addTestAccount(account);
        return account;
    }

    /**
     * M: Create a exchange test account that will be automatically deleted when
     * the test is finished
     *
     * @param name
     *            the name of the account
     * @param saveIt
     *            saveIt whether or not to save the account in EmailProvider
     * @return the account created
     */
    protected Account setupTestEasAccount(String name, boolean saveIt) {
        Account account = EmailContentSetupUtils.setupEasAccount(name, saveIt,
                mProviderContext);
        addTestAccount(account);
        return account;
    }

    /**
     * M: Start service
     *
     * @param context
     *            the context for starting service
     * @param intentAction
     *            the intent action
     */
    public static void startService(Context context, String intentAction) {
        context.startService(new Intent(intentAction));
    }

    /**
     * M: Stop service
     *
     * @param context
     *            the context for stop service
     * @param intentAction
     *            the intent action
     */
    public static void stopService(Context context, String intentAction) {
        context.stopService(new Intent(intentAction));
    }

    /**
     * Start exchange service
     *
     * @param context
     *            the context for starting service
     */
    public static void startExchangeService(Context context) {
        startService(context, EXCHANGE_INTENT);
    }

    /**
     * M: Stop exchange service
     *
     * @param context
     *            the context for stop service
     */
    public static void stopExchangeService(Context context) {
        stopService(context, EXCHANGE_INTENT);
    }

    /**
     * M: Create a system account
     *
     * @param username
     *            the name of the account
     * @param passward
     *            the password of the account
     * @return the account created
     */
    public android.accounts.Account createAccountManagerAccount(
            String username, String passward) {

        return EmailContentSetupUtils.createAccountManagerAccount(username,
                passward, mProviderContext);
    }
}
