package com.android.exchange.service;

import android.content.Context;
import android.content.Intent;

import com.android.emailcommon.Configuration;
import com.android.exchange.ExchangePreferences;
import com.android.exchange.utility.ExchangeTestCase;
import com.android.exchange.utility.TestUtils;

/*
 * M: Testcase for ExchangeBroadcastProcessorService.
 */
public class ExchangeBroadcaseProcessorServiceTests extends ExchangeTestCase {

    private Context mContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        Configuration.openTest();
    }

    @Override
    public void tearDown() throws Exception {
        Configuration.shutDownTest();
        super.tearDown();
    }

    /**
     * Send mock LOW_STORAGE broadcast and check if the exchange service can be
     * started or not.
     */
    public void testLowStorage() {
        Intent intent = new Intent(Intent.ACTION_DEVICE_STORAGE_LOW);
        ExchangeBroadcastProcessorService.processBroadcastIntent(mContext,
                intent);
        TestUtils.sleepAndWait();
        boolean status = getExchangePreferences().getLowStorage();
        assertTrue("testLowStorage", status);
    }

    public void testRecoveryStorage() {
        Intent intent = new Intent(Intent.ACTION_DEVICE_STORAGE_OK);
        ExchangeBroadcastProcessorService.processBroadcastIntent(mContext,
                intent);
        TestUtils.sleepAndWait();
        boolean status = getExchangePreferences().getLowStorage();
        assertFalse("testRecoveryStorage", status);
    }

    private ExchangePreferences getExchangePreferences() {
        return ExchangePreferences.getPreferences(mContext);
    }
}
