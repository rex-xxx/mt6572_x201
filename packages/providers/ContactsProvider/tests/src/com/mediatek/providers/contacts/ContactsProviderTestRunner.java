package com.mediatek.providers.contacts;

import android.test.InstrumentationTestRunner;

import com.mediatek.providers.contacts.dialersearchtestcase.DialerSearchTestSuite;
import com.mediatek.providers.contacts.androidtests.DefaultAndroidTestSuite;

import junit.framework.TestSuite;

public class ContactsProviderTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();
        
        suite.addTest(DefaultAndroidTestSuite.suite());
        suite.addTest(DialerSearchTestSuite.suite());
        return suite;
    }
}