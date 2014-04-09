package com.android.dreams.phototable.test;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.View;
import android.view.ViewConfiguration;

import com.android.dreams.phototable.PhotoSourcePlexor;
import com.android.dreams.phototable.PhotoTable;
import com.android.dreams.phototable.PhotoTouchListener;
import com.android.dreams.phototable.PreviewStubActivity;
import com.jayway.android.robotium.solo.Solo;

import java.util.LinkedList;

public class StubTest extends
        ActivityInstrumentationTestCase2<PreviewStubActivity> {

    private static final String TAG = StubTest.class.getSimpleName();
    private static final String EXTRA_DREAM_TYPE = "dream";
    private static final String EXTRA_DREAM_FLIP = "flip";
    private static final String EXTRA_DREAM_TABLE = "table";
    private static final String EXTRA_DREAM_EMPTY = "empty";
    private static final String EXTRA_DREAM_FAST = "fast";
    private static final String EXTRA_DREAM_BADLIMIT = "badlimit";
    private Context mContext;
    private Instrumentation mInstrumentation;
    private PreviewStubActivity mStubActivity;
    private Solo mSolo;

    public StubTest() {
        super(PreviewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mStubActivity != null) {
            mStubActivity.finish();
            mStubActivity = null;
        }
        mSolo.finishOpenedActivities();
        mStubActivity = null;
        super.tearDown();
    }

    public void testCase01flipEmpty() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_FLIP);
        intent.putExtra(EXTRA_DREAM_EMPTY, true);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        mSolo.sleep(5000);
    }

    public void testCase02flipDream() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_FLIP);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        mSolo.sleep(5000);
        mSolo.drag(100, 300, 400, 400, 3);
        mSolo.sleep(2000);
    }

    // TODO
    public void testCase03flipBad() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_FLIP);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        intent.putExtra(EXTRA_DREAM_BADLIMIT, 0);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        mSolo.sleep(5000);
    }

    public void testCase04tableEmpty() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_TABLE);
        intent.putExtra(EXTRA_DREAM_EMPTY, true);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        mSolo.sleep(6000);
    }

    public void testCase05tableDream() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_TABLE);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        intent.putExtra(EXTRA_DREAM_FAST, 400);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        PhotoTable table = (PhotoTable) ReflectionHelper.getObjectValue(
                mStubActivity.getClass(), "mTable", mStubActivity);
        ReflectionHelper.setFieldValue(table, "mDropPeriod", 2000);
        ReflectionHelper.setFieldValue(table, "mTableCapacity", 5);
        PhotoSourcePlexor photoSource = (PhotoSourcePlexor) ReflectionHelper
                .getObjectValue(table.getClass(), "mPhotoSource", table);
        photoSource.setSeed(5);
        mSolo.sleep(6000);
        LinkedList<View> viewList = (LinkedList<View>) ReflectionHelper
                .getObjectValue(table.getClass(), "mOnTable", table);
        assertTrue(viewList.size() > 1);
    }

    public void testCase06tableBad() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_TABLE);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        intent.putExtra(EXTRA_DREAM_FAST, 400);
        intent.putExtra(EXTRA_DREAM_BADLIMIT, 0);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        mSolo.sleep(2000);
    }

    public void testCase07tableSelect() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_TABLE);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        PhotoTable table = (PhotoTable) ReflectionHelper.getObjectValue(
                mStubActivity.getClass(), "mTable", mStubActivity);
        LinkedList<View> viewList = (LinkedList<View>) ReflectionHelper
                .getObjectValue(table.getClass(), "mOnTable", table);
        View select = null;
        boolean cont = true;
        int[] loc = new int[2];
        int i = 0;
        while (cont) {
            try {
                select = viewList.get(i);
                mSolo.sleep((int) (select.animate().getDuration()));
                if (select.getX() < 0 || select.getY() < 0) {
                    i++;
                    continue;
                }
                cont = false;
            } catch (IndexOutOfBoundsException e) {
                mSolo.sleep(1000);
            }
        }

        select.getLocationInWindow(loc);
        mSolo.clickOnScreen(loc[0] + 20, loc[1] + 20);
        mSolo.sleep(1000);
        assertNotNull(table.getSelected());
        mSolo.sleep(15000);
        assertNull(table.getSelected());
        i = 0;
        while (cont) {
            try {
                select = viewList.get(i);
                mSolo.sleep((int) (select.animate().getDuration()));
                if (select.getX() < 0 || select.getY() < 0) {
                    i++;
                    continue;
                }
                cont = false;
            } catch (IndexOutOfBoundsException e) {
                mSolo.sleep(1000);
            }
        }
        select.getLocationInWindow(loc);
        mSolo.clickOnScreen(loc[0] + 20, loc[1] + 20);
        mSolo.sleep(1000);
        assertNotNull(table.getSelected());
        select.getLocationInWindow(loc);
        mSolo.clickOnScreen(loc[0] + 20, loc[1] + 20);
        mSolo.sleep(1000);
        assertNull(table.getSelected());
    }

    public void testCase08tableDrag() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_TABLE);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        PhotoTable table = (PhotoTable) ReflectionHelper.getObjectValue(
                mStubActivity.getClass(), "mTable", mStubActivity);
        LinkedList<View> viewList = (LinkedList<View>) ReflectionHelper
                .getObjectValue(table.getClass(), "mOnTable", table);
        View select = null;
        boolean cont = true;
        int[] loc = new int[2];
        int i = 0;
        while (cont) {
            try {
                select = viewList.get(i);
                mSolo.sleep((int) (select.animate().getDuration()));
                if (select.getX() < 0 || select.getY() < 0) {
                    i++;
                    continue;
                }
                cont = false;
            } catch (IndexOutOfBoundsException e) {
                mSolo.sleep(1000);
            }
        }
        int width = (Integer) ReflectionHelper.getObjectValue(table.getClass(),
                "mWidth", table);
        int height = (Integer) ReflectionHelper.getObjectValue(
                table.getClass(), "mHeight", table);
        boolean isLand = (Boolean) ReflectionHelper.getObjectValue(table
                .getClass(), "mIsLandscape", table);
        assertNull(table.getSelected());
        select.getLocationInWindow(loc);
        if (width - loc[0] < 100) {
            mSolo.drag(loc[0] + 20, loc[0] - 100, loc[1] + 20, loc[1] + 100, 9);
        } else {
            mSolo.drag(loc[0] + 20, loc[0] + 100, loc[1] + 20, loc[1] + 100, 9);
        }
        mSolo.sleep(500);
        select = viewList.getLast();
        mSolo.sleep((int) (select.animate().getDuration()));
        select.getLocationInWindow(loc);
        assertNull(table.getSelected());
        mSolo.drag(loc[0] + 20, width * 1.2f, loc[1] + 20, height * 1.2f, 5);
        mSolo.sleep(3000);
        assertNull(table.getSelected());
    }

    public void testCase09tableNotMove() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_TABLE);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        PhotoTable table = (PhotoTable) ReflectionHelper.getObjectValue(
                mStubActivity.getClass(), "mTable", mStubActivity);
        LinkedList<View> viewList = (LinkedList<View>) ReflectionHelper
                .getObjectValue(table.getClass(), "mOnTable", table);
        View select = null;
        boolean cont = true;
        int[] loc = new int[2];
        int i = 0;
        while (cont) {
            try {
                select = viewList.get(i);
                mSolo.sleep((int) (select.animate().getDuration()));
                if (select.getX() < 0 || select.getY() < 0) {
                    i++;
                    continue;
                }
                cont = false;
            } catch (IndexOutOfBoundsException e) {
                mSolo.sleep(1000);
            }
        }

        assertNull(table.getSelected());
        select.getLocationInWindow(loc);
        mSolo.drag(loc[0] + 20, loc[0] + 21, loc[1] + 20, loc[1] + 20, 1);
        mSolo.sleep(500);
        assertNotNull(table.getSelected());
        select.getLocationInWindow(loc);
        mSolo.drag(loc[0] + 20, loc[0] + 100, loc[1] + 20, loc[1] + 100, 1);
        mSolo.sleep(500);
        assertNull(table.getSelected());
    }

    public void testCase10photoRotate() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DREAM_TYPE, EXTRA_DREAM_TABLE);
        intent.putExtra(EXTRA_DREAM_EMPTY, false);
        setActivityIntent(intent);
        mStubActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mStubActivity);
        PhotoTable table = (PhotoTable) ReflectionHelper.getObjectValue(
                mStubActivity.getClass(), "mTable", mStubActivity);
        LinkedList<View> viewList = (LinkedList<View>) ReflectionHelper
                .getObjectValue(table.getClass(), "mOnTable", table);
        View select = null;
        boolean cont = true;
        int[] loc = new int[2];
        int i = 0;
        while (cont) {
            try {
                select = viewList.get(i);
                mSolo.sleep((int) (select.animate().getDuration()));
                if (select.getX() < 0 || select.getY() < 0) {
                    i++;
                    continue;
                }
                cont = false;
            } catch (IndexOutOfBoundsException e) {
                mSolo.sleep(1000);
            }
        }

        assertNull(table.getSelected());
        select.getLocationInWindow(loc);
        float backupRotation = select.getRotation();
        multiClickOnScreen(loc[0] + 20, loc[1] + 20, loc[0] + 40, loc[1] + 40);
        mSolo.sleep(500);
        assertTrue(Math.abs(backupRotation - select.getRotation()) < 0.1);

        Object lisInfo = ReflectionHelper.getObjectValue(View.class,
                "mListenerInfo", select);
        Class listInfoClass = ReflectionHelper.getNonPublicInnerClass(
                View.class, "ListenerInfo");
        PhotoTouchListener touchListener = (PhotoTouchListener) ReflectionHelper
                .getObjectValue(listInfoClass, "mOnTouchListener", lisInfo);
        ReflectionHelper.setFieldValue(touchListener, "mManualImageRotation",
                true);
        mSolo.sleep(500);
        select.getLocationInWindow(loc);
        multiClickOnScreen(loc[0] + 20, loc[1] + 20, loc[0] + 40, loc[1] + 40);
        mSolo.sleep(500);
        assertNotSame(backupRotation, select.getRotation());
    }

    private void multiClickOnScreen(float x, float y, float m, float n) {
        MotionEvent event;
        PointerProperties[] properties = new PointerProperties[2];

        PointerCoords[] pointerCoords = new PointerCoords[2];
        PointerProperties pp1 = new PointerProperties();
        pp1.id = 0;
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER;
        properties[0] = pp1;
        PointerCoords pc1 = new PointerCoords();
        pc1.x = x;
        pc1.y = y;
        pc1.pressure = 1;
        pc1.size = 1;
        pointerCoords[0] = pc1;
        PointerProperties pp2 = new PointerProperties();
        pp2.id = 1;
        pp2.toolType = MotionEvent.TOOL_TYPE_FINGER;
        properties[1] = pp2;
        PointerCoords pc2 = new PointerCoords();
        pc2.x = m;
        pc2.y = n;
        pc2.pressure = 1;
        pc2.size = 1;
        pointerCoords[1] = pc2;
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN
                | 0 << MotionEvent.ACTION_POINTER_INDEX_SHIFT, x, y, 0);
        mInstrumentation.sendPointerSync(event);
        mSolo.sleep(1000);
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_POINTER_DOWN
                        | 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT, 2,
                properties, pointerCoords, 0, 0, 1, 1, 0, 0, 0, 0);
        mInstrumentation.sendPointerSync(event);
        eventTime = SystemClock.uptimeMillis();
        pointerCoords[1].y += 20 * ViewConfiguration.getTouchSlop();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE
                | 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT, 2, properties,
                pointerCoords, 0, 0, 1, 1, 0, 0, 0, 0);
        mInstrumentation.sendPointerSync(event);
        mSolo.sleep(1000);
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_POINTER_UP
                        | 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT, 2,
                properties, pointerCoords, 0, 0, 1, 1, 0, 0, 0, 0);
        mInstrumentation.sendPointerSync(event);
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP
                | 0 << MotionEvent.ACTION_POINTER_INDEX_SHIFT, x, y, 0);
        mInstrumentation.sendPointerSync(event);
    }

}
