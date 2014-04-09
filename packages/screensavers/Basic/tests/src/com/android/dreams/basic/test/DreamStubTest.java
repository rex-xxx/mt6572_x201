package com.android.dreams.basic.test;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.android.dreams.basic.PreviewStubActivity;
import com.jayway.android.robotium.solo.Solo;

public class DreamStubTest extends
        ActivityInstrumentationTestCase2<PreviewStubActivity> {

    private static final String TAG = DreamStubTest.class.getSimpleName();
    private Context mContext;
    private Instrumentation mInstrumentation;
    private PreviewStubActivity mStubActivity;
    private Solo mSolo;

    public DreamStubTest() {
        super(PreviewStubActivity.class);
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

    public void testCase01precondition() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        assertNotNull(mStubActivity);
        assertNotNull(mSolo);
    }

    public void testCase02startDream() {
        mSolo.sleep(2000);
        mStubActivity.onSurfaceTextureSizeChanged(null, 320, 240);
        mSolo.sleep(2000);
    }

}
