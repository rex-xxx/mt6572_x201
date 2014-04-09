package com.android.exchange;

import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;

import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.android.exchange.utility.ExchangeTestCase;

/**
 * M: Test the EmailSyncAdapterService
 */
public class EmailSyncAdapterServiceTest extends ExchangeTestCase {

    public void testPerformSync() {
        //Setup an exchange account
        Account sysAccount = createAccountManagerAccount("eas_test_1@a.com", "password");
        com.android.emailcommon.provider.Account account = setupTestEasAccount("eas_test_1", true);

        //Setup folder
        Mailbox mailbox = EmailContentSetupUtils.setupMailbox("Calendar_Test",
                account.mId, true, mContext, Mailbox.TYPE_INBOX);
        //Start exchange service
        startExchangeService(mContext);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Logging.d(TAG, "testPerformSync sleep failed.");
        }
        boolean originalConnectivityHold = ExchangeService.sConnectivityHold;
        ExchangeService.sConnectivityHold = false;

        EmailSyncAdapterService service = new EmailSyncAdapterService();
        Bundle extras = new Bundle();
        service.getTestSyncAdapter(mContext).onPerformSync(sysAccount, extras, null, null, null);
        if (ExchangeService.INSTANCE != null) {
            assertTrue(ExchangeService.INSTANCE.isSyncServiceRunning(mailbox.mId));
        }

        ExchangeService.sConnectivityHold = originalConnectivityHold;
        AccountManager.get(getContext()).removeAccount(sysAccount, null, null);
    }
}
