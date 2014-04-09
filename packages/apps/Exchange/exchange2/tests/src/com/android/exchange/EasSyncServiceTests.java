/*
 * Copyright (C) 2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

package com.android.exchange;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.service.EmailServiceProxy;
import com.android.exchange.Eas;
import com.android.exchange.EasAccountService;
import com.android.exchange.EasOutboxService;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.Exchange;
import com.android.exchange.ExchangeService;
import com.android.exchange.FetchMailRequest;
import com.android.exchange.MeetingResponseRequest;
import com.android.exchange.MessageMoveRequest;
import com.android.exchange.PartRequest;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.MockEasResponse;
import com.android.exchange.adapter.SyncAdapterTestCase;
import com.android.exchange.provider.EmailContentSetupUtils;

import com.android.exchange.provider.GalResult;
import com.android.exchange.utility.TestUtils;
import com.android.exchange.utility.UriCodec;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.EasSyncServiceTests exchange
 */
@SmallTest
public class EasSyncServiceTests extends SyncAdapterTestCase<EmailSyncAdapter> {
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String HOST = "xxx.host.zzz";
    private static final String ID = "id";
    static Context sContext;
    Context mMockContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mMockContext = getContext();
        sContext = mMockContext;
        EasResponse.sCallback = new MockEasResponse();
        Configuration.openTest();
    }

    public void testAddHeaders() {
        HttpRequestBase method = new HttpPost();
        EasSyncService svc = new EasSyncService();
        svc.mAuthString = "auth";
        svc.mProtocolVersion = "12.1";
        svc.mAccount = null;
        // With second argument false, there should be no header
        svc.setHeaders(method, false);
        Header[] headers = method.getHeaders("X-MS-PolicyKey");
        assertEquals(0, headers.length);
        // With second argument true, there should always be a header
        // The value will be "0" without an account
        method.removeHeaders("X-MS-PolicyKey");
        svc.setHeaders(method, true);
        headers = method.getHeaders("X-MS-PolicyKey");
        assertEquals(1, headers.length);
        assertEquals("0", headers[0].getValue());
        // With an account, but null security key, the header's value should be "0"
        Account account = new Account();
        account.mSecuritySyncKey = null;
        svc.mAccount = account;
        method.removeHeaders("X-MS-PolicyKey");
        svc.setHeaders(method, true);
        headers = method.getHeaders("X-MS-PolicyKey");
        assertEquals(1, headers.length);
        assertEquals("0", headers[0].getValue());
        // With an account and security key, the header's value should be the security key
        account.mSecuritySyncKey = "key";
        svc.mAccount = account;
        method.removeHeaders("X-MS-PolicyKey");
        svc.setHeaders(method, true);
        headers = method.getHeaders("X-MS-PolicyKey");
        assertEquals(1, headers.length);
        assertEquals("key", headers[0].getValue());
    }

    public void testGetProtocolVersionDouble() {
        assertEquals(Eas.SUPPORTED_PROTOCOL_EX2003_DOUBLE,
                Eas.getProtocolVersionDouble(Eas.SUPPORTED_PROTOCOL_EX2003));
        assertEquals(Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE,
                Eas.getProtocolVersionDouble(Eas.SUPPORTED_PROTOCOL_EX2007));
        assertEquals(Eas.SUPPORTED_PROTOCOL_EX2007_SP1_DOUBLE,
                Eas.getProtocolVersionDouble(Eas.SUPPORTED_PROTOCOL_EX2007_SP1));
    }

    private EasSyncService setupService(String user) {
        EasSyncService svc = new EasSyncService();
        svc.mUserName = user;
        svc.mPassword = PASSWORD;
        svc.mDeviceId = ID;
        svc.mHostAddress = HOST;
        return svc;
    }

    public void testMakeUriString() throws IOException {
        // Simple user name and command
        EasSyncService svc = setupService(USER);
        String uriString = svc.makeUriString("Sync", null);
        // These next two should now be cached
        assertNotNull(svc.mAuthString);
        assertNotNull(svc.mUserString);
        assertEquals("Basic " + Base64.encodeToString((USER+":"+PASSWORD).getBytes(),
                Base64.NO_WRAP), svc.mAuthString);
        assertEquals("&User=" + USER + "&DeviceId=" + ID + "&DeviceType=" +
                EasSyncService.DEVICE_TYPE, svc.mUserString);
        assertEquals("https://" + HOST + "/Microsoft-Server-ActiveSync?Cmd=Sync" +
                svc.mUserString, uriString);
        // User name that requires encoding
        String user = "name_with_underscore@foo%bar.com";
        svc = setupService(user);
        uriString = svc.makeUriString("Sync", null);
        assertEquals("Basic " + Base64.encodeToString((user+":"+PASSWORD).getBytes(),
                Base64.NO_WRAP), svc.mAuthString);
        String safeUserName = "name_with_underscore%40foo%25bar.com";
        assertEquals("&User=" + safeUserName + "&DeviceId=" + ID + "&DeviceType=" +
                EasSyncService.DEVICE_TYPE, svc.mUserString);
        assertEquals("https://" + HOST + "/Microsoft-Server-ActiveSync?Cmd=Sync" +
                svc.mUserString, uriString);
    }

    void setupSyncParserAndAdapter(Account account, Mailbox mailbox) throws IOException {
        EasSyncService service = getTestService(account, mailbox);
        mSyncAdapter = new EmailSyncAdapter(service);
        mSyncParser = mSyncAdapter.new EasEmailSyncParser(getTestInputStream(), mSyncAdapter);
    }

    ArrayList<Long> setupAccountMailboxAndMessages(int numMessages) {
        ArrayList<Long> ids = new ArrayList<Long>();
        String expected = "6" + '\2' + "UID" + '\1' + "2012-01-02T23:00:01.000Z"
        + '\2' + "DTSTAMP" + '\1' + "7" + '\2' + "RESPONSE" + '\1' + "4@a.com" + '\2'
        + "ORGMAIL" + '\1' + "2012-01-02T23:00:01.000Z" + '\2' + "DTSTART"
        + '\1' + "1" + '\2' + "ALLDAY" + '\1' + "2012-01-02T24:00:01.000Z"
        + '\2' + "DTEND" + '\1' + "8" + '\2' + "TITLE" + '\1' + "5" + '\2' + "LOC";

        // Create account and two mailboxes
        mAccount = EmailContentSetupUtils.setupEasAccount("account", true, mProviderContext, "14.1");
        mMailbox = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_OUTBOX, null);

        for (int i = 0; i < numMessages; i++) {
            Message msg = setupMessage("message" + i, mAccount.mId,
                    mMailbox.mId, expected, false, true, mProviderContext, true, true);
            ids.add(msg.mId);
        }

        return ids;
    }

    /**
     * M: Create a message for test purposes
     */
    public static Message setupMessage(String name, long accountId, long mailboxId, String msgInfo,
            boolean addBody, boolean saveIt, Context context, boolean starred, boolean read) {
        Message message = new Message();

        message.mDisplayName = name;
        message.mMailboxKey = mailboxId;
        message.mAccountKey = accountId;
        message.mFlagRead = read;
        message.mFlagLoaded = Message.FLAG_LOADED_UNLOADED;
        message.mFlagFavorite = starred;
        message.mServerId = "serverid " + name;
        message.mMeetingInfo = msgInfo;
        message.mFlags |= Message.FLAG_TYPE_REPLY;

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
     * M: Test the sync function
     */
    public void testSync() throws IOException {
        ArrayList<Long> ids = setupAccountMailboxAndMessages(4);
        setupSyncParserAndAdapter(mAccount, mMailbox);
        mSyncAdapter.mService = EasSyncService.setupServiceForAccount(mProviderContext, mAccount);
        mSyncAdapter.mService.mMailbox = mMailbox;
        mSyncAdapter.mService.mContentResolver = mResolver;
        MessageMoveRequest req1 = new MessageMoveRequest(ids.get(0), mMailbox.mId);
        FetchMailRequest req2 = new FetchMailRequest(ids.get(1));
        MeetingResponseRequest req3 = new MeetingResponseRequest(ids.get(2), 2);
        Attachment att = new Attachment();
        att.mId = 1;
        att.mLocation = "location1";
        att.mMessageKey = ids.get(3);
        att.mAccountKey = mAccount.mId;
        PartRequest req4 = new PartRequest(att, "dest1", "content1");

        ContentValues cv = new ContentValues();
        cv.put(Message.MAILBOX_KEY, 2);
        mResolver.update(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI,
                ids.get(0)), cv, null, null);

        Message msg = Message.restoreMessageWithId(mProviderContext, ids.get(2));
        Exchange.sBadSyncKeyMailboxId = mMailbox.mId;
        mSyncAdapter.mService.addRequest(req4);
        mSyncAdapter.mService.sync(mSyncAdapter);
        mSyncAdapter.mService.clearRequests();
        mSyncAdapter.mService.addRequest(req1);
        mSyncAdapter.mService.addRequest(req2);
        mSyncAdapter.mService.addRequest(req3);
        mSyncAdapter.mService.mProtocolVersionDouble = 12.0;
        mSyncAdapter.mService.sync(mSyncAdapter);
        mSyncAdapter.mService.clearRequests();
        mSyncAdapter.mService.addRequest(req4);
        mSyncAdapter.mService.mProtocolVersionDouble = 2.5;
        mSyncAdapter.mService.sync(mSyncAdapter);
        assertEquals(1, mSyncAdapter.mService.getSendStatus());
        assertEquals(Eas.EAS_SYNC_RECOVER, mSyncAdapter.mService.getSnycStatus());
    }

    public static InputStream openTestFile() throws IOException {
        AssetManager am = sContext.getAssets();
        InputStream is = am.open("EasSyncServiceTest.xml");
        return is;
    }

    public void testTryAutodiscover() throws RemoteException, IOException {
        String userName = "panda@a.com";
        String password = "cute";

        EasSyncService service = new EasSyncService();
        Bundle bundle1 = service.tryAutodiscover(userName, password);
        Bundle bundle2 = new Bundle();
        bundle2.putInt(EmailServiceProxy.AUTO_DISCOVER_BUNDLE_ERROR_CODE,
                MessagingException.NO_ERROR);
        HostAuth hostAuth = new HostAuth();
        hostAuth.mAddress = "google.com";
        hostAuth.mLogin = userName;
        hostAuth.mPassword = password;
        hostAuth.mPort = 443;
        hostAuth.mProtocol = "eas";
        hostAuth.mFlags =
            HostAuth.FLAG_SSL | HostAuth.FLAG_AUTHENTICATE;
        bundle2.putParcelable(
                EmailServiceProxy.AUTO_DISCOVER_BUNDLE_HOST_AUTH, hostAuth);
        assertEquals(bundle2.toString(), bundle1.toString());
    }

    public void testTryProvision() throws IOException {
        setupAccountMailboxAndMessages(0);
        setupSyncParserAndAdapter(mAccount, mMailbox);

        mSyncAdapter.mService.setupService();
        mSyncAdapter.mService.mProtocolVersionDouble = 14.1;
        mSyncAdapter.mService.tryProvision(mSyncAdapter.mService);
        mSyncAdapter.mService.mProtocolVersionDouble = 14.0;
        boolean result = mSyncAdapter.mService.tryProvision(mSyncAdapter.mService);
        assertFalse(result);
    }

    public void testGetServiceForMailbox() throws Exception {
        ArrayList<Long> iList = setupAccountMailboxAndMessages(2);
        Body body = EmailContentSetupUtils.setupBody("body", iList.get(0), iList.get(1), true, mProviderContext);

        setupSyncParserAndAdapter(mAccount, mMailbox);
        EasSyncService service = EasSyncService.getServiceForMailbox(mProviderContext, mMailbox);
        Thread thread = new Thread(service);
        thread.start();
        assertTrue(service instanceof EasOutboxService);
        mMailbox.mType = Mailbox.TYPE_EAS_ACCOUNT_MAILBOX;
        service = EasSyncService.getServiceForMailbox(mProviderContext, mMailbox);
        assertTrue(service instanceof EasAccountService);
        mMailbox.mType = Mailbox.TYPE_INBOX;
        service = EasSyncService.getServiceForMailbox(mProviderContext, mMailbox);
        assertTrue(service instanceof EasSyncService);
        TestUtils.sleepAndWait(5000);
    }

    public void testSearchGal() throws Exception {
        setupAccountMailboxAndMessages(2);
        GalResult result = EasSyncService.searchGal(mProviderContext, mAccount.mId, "search", 12);
        assertNotNull(result);
        assertEquals(10, result.total);
        result = EasSyncService.searchGal(mProviderContext, mAccount.mId - 1, "search", 12);
        assertNull(result);
    }

    public void testValidateAccount() throws Exception {
        setupAccountMailboxAndMessages(0);
        HostAuth auth = new HostAuth();
        auth.mAddress = mAccount.mEmailAddress;
        auth.mProtocol = "eas";
        auth.mLogin = mAccount.mDisplayName;
        auth.mPassword = "password";
        auth.mPort = 80;
        EasSyncService eas = getTestService(mAccount, mMailbox);
        Bundle bundle = eas.validateAccount(auth, mProviderContext);
        assertEquals("14.1", bundle.get(EmailServiceProxy
                .VALIDATE_BUNDLE_PROTOCOL_VERSION));
        assertEquals(MessagingException.NO_ERROR, bundle.get(EmailServiceProxy
                .VALIDATE_BUNDLE_RESULT_CODE));
    }

    public void testEasService() throws Exception, IOException {
        setupAccountMailboxAndMessages(0);
        setupSyncParserAndAdapter(mAccount, mMailbox);
        mSyncAdapter.mService.setupService();
        EasSyncService svc = mSyncAdapter.mService;
        Attachment att = new Attachment();
        att.mId = 100;
        att.mLocation = "location1";
        PartRequest req = new PartRequest(att, "dest1", "content1");
        svc.addRequest(req);
        svc.cancelPartRequest(att.mId);
        EasSyncService.trustAllHttpsCertificates();
        int num = MockEasResponse.getSettingNums();
        assertTrue(svc.getSendingStatus());
        assertEquals(num + 1, MockEasResponse.getSettingNums());
        MockEasResponse.setSettingNums(num);
        svc.resetSecurityPolicies();
        String reString = UriCodec.decode("1234%4234");
        assertEquals("1234B34", reString);
    }

    @Override
    public void tearDown() throws Exception {
        EasResponse.sCallback = null;
        Configuration.shutDownTest();
        super.tearDown();
    }
}
