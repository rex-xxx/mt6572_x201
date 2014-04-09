package com.android.exchange.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.exchange.CommandStatusException;
import com.android.exchange.EasSyncService;
import com.android.exchange.provider.EmailContentSetupUtils;

public class FolderSyncParserTests2 extends SyncAdapterTestCase<EmailSyncAdapter> {
    private static final String[] ACCOUNT_ARGUMENT = new String[1];
    private static final String WHERE_ACCOUNT_KEY = Message.ACCOUNT_KEY + "=?";

    private FolderSyncParser mParser;
    private String[] getAccountArgument(long id) {
        ACCOUNT_ARGUMENT[0] = Long.toString(id);
        return ACCOUNT_ARGUMENT;
    }

    void setupSyncAdapter(Account account, Mailbox mailbox) throws IOException {
        EasSyncService service = getTestService(account, mailbox);
        mSyncAdapter = new EmailSyncAdapter(service);
    }

    ArrayList<Long> setupAccountMailboxAndMessages(int numMessages) {
        ArrayList<Long> ids = new ArrayList<Long>();

        // Create account and two mailboxes
        mAccount = EmailContentSetupUtils.setupEasAccount("account", true, mProviderContext);
        mMailbox = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext);

        for (int i = 0; i < numMessages; i++) {
            Message msg = EmailContentSetupUtils.setupMessage("message" + i, mAccount.mId,
                    mMailbox.mId, true, true, mProviderContext);
            ids.add(msg.mId);
        }

        assertEquals(numMessages, EmailContent.count(mProviderContext, Message.CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
        return ids;
    }

    public void testFolderSyncParse() throws IOException, CommandStatusException {
        setupAccountMailboxAndMessages(1);
        setupSyncAdapter(mAccount, mMailbox);
        Serializer s = new Serializer();
        s.start(Tags.FOLDER_FOLDER_SYNC).tag(Tags.FOLDER).start(Tags.FOLDER_CHANGES)
        .tag(Tags.FOLDER).start(Tags.FOLDER_UPDATE).tag(Tags.FOLDER)
        .data(Tags.FOLDER_DISPLAY_NAME, "test").data(Tags.FOLDER_PARENT_ID, "parentId")
        .data(Tags.FOLDER_SERVER_ID, "serverid-box1").end().start(Tags.FOLDER_DELETE)
        .tag(Tags.FOLDER).data(Tags.FOLDER_SERVER_ID, "serverid-box1").end().end()
        .data(Tags.FOLDER_STATUS, "136").end();
        ByteArrayInputStream byis = new ByteArrayInputStream(s.toByteArray());
        mParser = new FolderSyncParser(byis, mSyncAdapter);
        assertTrue(mParser.parse());
    }
}
