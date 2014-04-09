package com.android.dreams.phototable.test;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.android.dreams.phototable.FlipperDreamSettings;
import com.jayway.android.robotium.solo.Solo;

public class FlipperDreamSettingsTest extends
        ActivityInstrumentationTestCase2<FlipperDreamSettings> {

    private static final String TAG = PhotoTableDreamSettingsTest.class
            .getSimpleName();
    private Context mContext;
    private Instrumentation mInstrumentation;
    private FlipperDreamSettings mStubActivity;
    private Solo mSolo;

    public FlipperDreamSettingsTest() {
        super(FlipperDreamSettings.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mStubActivity != null) {
            mStubActivity.finish();
            mStubActivity = null;
        }
        mSolo.finishOpenedActivities();
        super.tearDown();
    }

    public void testCase01select() {
        precondition();
        mSolo.sleep(1000);
        if (mSolo.searchText("Download")) {
            for (int i = 0; i < 3; i++) {
                mSolo.clickOnText("Download");
                mSolo.sleep(600);
            }
        }
    }

    private void precondition() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        assertNotNull(mStubActivity);
        assertNotNull(mSolo);
    }
}
