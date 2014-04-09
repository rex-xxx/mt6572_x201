package com.android.exchange;

import android.content.ContentResolver;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;

import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.android.exchange.utility.ExchangeTestCase;

/**
 * M: Test the CalendarSyncAdapterService
 */
public class CalendarSyncAdapterServiceTest extends ExchangeTestCase {

    public void testPerformSync() {
        //Setup an exchange account
        Account sysAccount = createAccountManagerAccount("eas_test_1@a.com", "password");
        com.android.emailcommon.provider.Account account = setupTestEasAccount("eas_test_1", true);

        //Setup calendar folder
        Mailbox mailbox = EmailContentSetupUtils.setupMailbox("Calendar_Test",
                account.mId, true, mContext, Mailbox.TYPE_CALENDAR);
        //Start exchange service
        startExchangeService(mContext);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Logging.d(TAG, "testPerformSync sleep failed.");
        }
        boolean originalConnectivityHold = ExchangeService.sConnectivityHold;
        ExchangeService.sConnectivityHold = false;

        CalendarSyncAdapterService service = new CalendarSyncAdapterService();
        Bundle extras = new Bundle();
        // sync up changes, but has no changes
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, true);
        service.getTestSyncAdapter(mContext).onPerformSync(sysAccount, extras, null, null, null);
        if (ExchangeService.INSTANCE != null) {
            assertFalse(ExchangeService.INSTANCE.isSyncServiceRunning(mailbox.mId));
        }
        // do not sync up changes
        extras.clear();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
        service.getTestSyncAdapter(mContext).onPerformSync(sysAccount, extras, null, null, null);
        if (ExchangeService.INSTANCE != null) {
            assertTrue(ExchangeService.INSTANCE.isSyncServiceRunning(mailbox.mId));
        }

        ExchangeService.sConnectivityHold = originalConnectivityHold;
        AccountManager.get(getContext()).removeAccount(sysAccount, null, null);
    }
}
