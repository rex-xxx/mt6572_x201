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

package com.android.exchange.provider;

import java.io.IOException;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.android.emailcommon.AccountManagerTypes;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.Mailbox;

/**
 * Simplified EmailContent class setup (condensed from ProviderTestUtils in
 * com.android.email)
 */
public class EmailContentSetupUtils {

    /**
     * No constructor - statics only
     */
    private EmailContentSetupUtils() {
    }

    /**
     * Create an account for test purposes
     */
    public static Account setupAccount(String name, boolean saveIt,
            Context context) {
        Account account = new Account();

        account.mDisplayName = name;
        account.mEmailAddress = name + "@android.com";
        account.mProtocolVersion = "2.5" + name;
        if (saveIt) {
            account.save(context);
        }
        return account;
    }

    /**
     * M: Setup an exchange test account
     */
    public static Account setupEasAccount(String name, boolean saveIt,
            Context context) {
        return setupEasAccount(name, saveIt, context, null);
    }

    public static Account setupEasAccount(String name, boolean saveIt,
            Context context, String protocolVersion) {
        Account account = new Account();

        account.mDisplayName = name;
        account.mEmailAddress = name + "@a.com";
        if (null == protocolVersion) {
            account.mProtocolVersion = "12.0";
        } else {
            account.mProtocolVersion = protocolVersion;
        }
        HostAuth auth = new HostAuth();
        auth.mAddress = account.mEmailAddress;
        auth.mProtocol = "eas";
        auth.mLogin = name;
        auth.mPassword = "password";
        auth.mPort = 80;
        account.mHostAuthRecv = auth;
        if (saveIt) {
            auth.save(context);
            account.mHostAuthKeyRecv = auth.mId;
            account.save(context);
        }
        return account;
    }

    /**
     * Create a mailbox for test purposes
     */
    public static Mailbox setupMailbox(String name, long accountId,
            boolean saveIt, Context context) {
        return setupMailbox(name, accountId, saveIt, context,
                Mailbox.TYPE_MAIL, null);
    }

    public static Mailbox setupMailbox(String name, long accountId,
            boolean saveIt, Context context, int type) {
        return setupMailbox(name, accountId, saveIt, context, type, null);
    }

    public static Mailbox setupMailbox(String name, long accountId,
            boolean saveIt, Context context, int type, Mailbox parentBox) {
        Mailbox box = new Mailbox();

        box.mDisplayName = name;
        box.mAccountKey = accountId;
        box.mSyncKey = "sync-key-" + name;
        box.mSyncLookback = 2;
        box.mSyncInterval = Account.CHECK_INTERVAL_NEVER;
        box.mType = type;
        box.mServerId = "serverid-" + name;
        box.mParentServerId = parentBox != null ? parentBox.mServerId
                : "parent-serverid-" + name;

        if (saveIt) {
            box.save(context);
        }
        return box;
    }

    public static Body setupBody(String name, long messageId, long sourceMsgId,
            boolean saveIt, Context context) {
        Body body = new Body();

        body.mMessageKey = messageId;
        body.mSourceKey = sourceMsgId;
        body.mTextContent = "mTextContent-" + name;
        body.mHtmlContent = "mHtmlContent" + name;
        body.mHtmlReply = "mHtmlReply-" + name;
        body.mTextReply = "mTextReply-" + name;
        body.mIntroText = "mIntroText" + name;

        if (saveIt) {
            body.save(context);
        }
        return body;
    }

    /**
     * Create a message for test purposes
     */
    public static Message setupMessage(String name, long accountId,
            long mailboxId, boolean addBody, boolean saveIt, Context context) {
        // Default starred, read, (backword compatibility)
        return setupMessage(name, accountId, mailboxId, addBody, saveIt,
                context, true, true);
    }

    /**
     * Create a message for test purposes
     */
    public static Message setupMessage(String name, long accountId,
            long mailboxId, boolean addBody, boolean saveIt, Context context,
            boolean starred, boolean read) {
        Message message = new Message();

        message.mDisplayName = name;
        message.mMailboxKey = mailboxId;
        message.mAccountKey = accountId;
        message.mFlagRead = read;
        message.mFlagLoaded = Message.FLAG_LOADED_UNLOADED;
        message.mFlagFavorite = starred;
        message.mServerId = "serverid " + name;

        if (addBody) {
            message.mText = "body text " + name;
            message.mHtml = "body html " + name;
        }

        if (saveIt) {
            message.save(context);
        }
        return message;
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
    public static android.accounts.Account createAccountManagerAccount(
            String username, String passward, Context context) {
        final android.accounts.Account account = new android.accounts.Account(
                username, AccountManagerTypes.TYPE_EXCHANGE);
        ;
        AccountManager.get(context).addAccountExplicitly(account, passward,
                null);
        return account;
    }

    /**
     * M: Delete the system account. Get all exchange accounts
     */
    public static android.accounts.Account[] getExchangeAccounts(Context context) {
        return AccountManager.get(context).getAccountsByType(
                AccountManagerTypes.TYPE_EXCHANGE);
    }

    /**
     * M: Delete the system account. Delete an exchange account.
     */
    public static void deleteAccountManagerAccount(
            android.accounts.Account account, Context context) {
        AccountManagerFuture<Boolean> future = AccountManager.get(context)
                .removeAccount(account, null, null);
        try {
            future.getResult();
        } catch (OperationCanceledException e) {
        } catch (AuthenticatorException e) {
        } catch (IOException e) {
        }
    }

    /**
     * M: Delete the system account. Find the deleted account' by name.
     */
    public static void deleteTemporaryAccountManagerAccounts(String name,
            Context context) {
        for (android.accounts.Account accountManagerAccount : getExchangeAccounts(context)) {
            if (accountManagerAccount.name.startsWith(name)
                    && accountManagerAccount.name.endsWith(name)) {
                deleteAccountManagerAccount(accountManagerAccount, context);
            }
        }
    }
}
