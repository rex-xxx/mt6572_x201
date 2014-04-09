package com.mediatek.providers.contacts.dialersearchtestcase;

import junit.framework.Test;
import junit.framework.TestSuite;

public class DialerSearchTestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite(
                "Test for com.mediatek.providers.contacts.DialerDearchTestCase");

        suite.addTestSuite(GetTokensForDialerSearchTest.class);
        suite.addTestSuite(DialerSearchSupportTest.class);
        return suite;
    }

}
