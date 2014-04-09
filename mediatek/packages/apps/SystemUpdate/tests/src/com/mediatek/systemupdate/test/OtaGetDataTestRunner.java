package com.mediatek.systemupdate.test;

import android.test.InstrumentationTestRunner;
import junit.framework.TestSuite;

public class OtaGetDataTestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(OtaGetDataTests.class);

        return suite;
    }

}
