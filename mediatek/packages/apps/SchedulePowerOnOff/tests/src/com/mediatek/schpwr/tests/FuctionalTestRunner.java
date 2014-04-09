package com.mediatek.schpwronoff.tests;

import android.test.InstrumentationTestRunner;
import junit.framework.TestSuite;

public class FuctionalTestRunner extends JUnitInstrumentationTestRunner {
	@Override
	public TestSuite getAllTests(){
	    TestSuite tests = new TestSuite();
        tests.addTestSuite(SchPwrOnOffTest.class);
        return tests;
    }
}

