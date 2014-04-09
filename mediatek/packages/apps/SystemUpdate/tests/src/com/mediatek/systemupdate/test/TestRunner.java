package com.mediatek.systemupdate.test;

import android.test.InstrumentationTestRunner;
import junit.framework.TestSuite;

public class TestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(SdPkgInstallActivityTests.class);
//        suite.addTestSuite(OtaPkgManagerActivityTests.class);
        suite.addTestSuite(MainEntryTests.class);

        suite.addTestSuite(UpdateOptionTests.class);

        return suite;
    }

}
