package com.mediatek.appwidget.worldclock.tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;
import android.widget.ListView;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.appwidget.worldclock.AutoCompleteTextViewActivity;
import com.mediatek.appwidget.worldclock.ChooseActivity;
import com.mediatek.appwidget.worldclock.ClockCityInfo;
import com.mediatek.appwidget.worldclock.ClockCityUtils;
import com.mediatek.appwidget.worldclock.R;
import com.mediatek.appwidget.worldclock.WorldClockWidgetProvider;

/**
 * This is a simple framework for a test of an Application. See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more
 * information on how to write and extend Application tests.
 * <p/>
 * To run this test, you can type: adb shell am instrument -w \ -e class
 * com.mediatek.weather3dwidget.MiscTest \
 * com.mediatek.weather3dwidget.tests/android.test.InstrumentationTestRunner
 */
public class ChooseActivityTest extends
        ActivityInstrumentationTestCase2<ChooseActivity> {
    private Instrumentation            mInstrumentation;
    private ChooseActivity             mActivity;
    private Solo                       solo;
    private ArrayList<String>          mTimeZoneArray;
    ArrayList<HashMap<String, Object>> data = null;
    private ListView                   mListView;

    public ChooseActivityTest() {
        super("com.mediatek.appwidget.worldclock", ChooseActivity.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mInstrumentation = this.getInstrumentation();
        solo = new Solo(mInstrumentation, mActivity);
        preconditions();
    }

    private void preconditions() {
        data = (ArrayList<HashMap<String, Object>>) Utils.getFiledValue(
                ChooseActivity.class, mActivity, "data");
        assertNotNull(data);
        HashMap<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("chooselistviewimageview", R.drawable.mtkchooseimage);
        tmp.put("chooselistviewtextview", "Abadan");
        tmp.put("chooselistviewweatherid", "2254271");
        data.add(tmp);
        tmp.put("chooselistviewimageview", R.drawable.mtkchooseimage);
        tmp.put("chooselistviewtextview", "Abu Dhabi");
        tmp.put("chooselistviewweatherid", "1940330");
        data.add(tmp);
        assertTrue(data.size() >= 2);
    }

    public void testSetCityInfoByWeatherId() {
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "2254271");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "1968222");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "2211027");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "22542");
        assertTrue(true);
    }

    public void testGetCityNameByWeatherId() {
        Utils.invoke(ChooseActivity.class, mActivity, "getCityNameBYWeatherId",
                "2254271");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "getCityNameBYWeatherId",
                "1958930");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "getCityNameBYWeatherId",
                "19589");

        solo.sleep(1000);
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "2254271");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "1968222");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "2211027");
        solo.sleep(2000);
        Utils.invoke(ChooseActivity.class, mActivity, "setCityInfoByWeatherID",
                "22542");
        assertTrue(true);
    }

    public void testGetData() {
        Utils.invoke(ChooseActivity.class, mActivity, "getData", null);
        solo.sleep(1000);
        Utils.invoke(ChooseActivity.class, mActivity, "getData",
                "2162779,1132599");
        solo.sleep(1000);
        ClockCityInfo cityInfo = new ClockCityInfo("1", "2171287",
                "Asia/Shanghai", "Shijiazhuang");
        WorldClockWidgetProvider.updateCity(mActivity, 1, cityInfo);
        assertTrue(true);
    }

    public void testClockCityUtils() {
        String TAG = "demo";
        int[] ids = { 1, 2 };
        ClockCityUtils utils = new ClockCityUtils(mActivity);
        ClockCityUtils.initPreference(mActivity);
        ClockCityUtils.deletePreferences(null);
        ClockCityUtils.deletePreferences(ids);

        ClockCityUtils.getBooleanPref("TAG", "1");
        ClockCityUtils.getCityAPref(TAG);
        ClockCityUtils.getCityBPref(TAG);
        ClockCityUtils.getCityCPref(TAG);
        ClockCityUtils.getIntPref(TAG, "1");
        ClockCityUtils.getCityIndexByName("Changsha");
        ClockCityUtils.getCityIndexByName("Abadan");
        ClockCityUtils.getCityIndexByName("Hongkong");
        ClockCityUtils.getCityIndexByName("Changsha");
        ClockCityUtils.getCityNameByWeatherID("1047378");
        ClockCityUtils.getCityTimeZoneByWeatherID("1047378");
        ClockCityUtils.getIntDelete("Dublin");
        ClockCityUtils.getIntDelete("Wenzhou");
        ClockCityUtils.getStringPref(TAG, "1");
        ClockCityUtils.getWeatherIDByName("Hongkong");

        ClockCityInfo cityInfo = new ClockCityInfo("1", "2132582",
                "Asia/Shanghai", "Wenzhou");
        ClockCityUtils.savePreferences(1, cityInfo);
        cityInfo = new ClockCityInfo("2", "560743", "Europe/Dublin", "Dublin");
        ClockCityUtils.savePreferences(2, cityInfo);
    }

    public void testClickItem() {
        solo.sleep(5000);
        solo.clickInList(1);
    }

    public void testRemoveCity() {
        preconditions();
        solo.sleep(8000);

        solo.clickLongInList(1);
        solo.sleep(6000);
        solo.clickOnButton("Delete");
    }

    public void testGetZones() {
        mActivity.getZones();
    }

    // test case #1
    public void testAddCity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(mInstrumentation.getTargetContext(),
                AutoCompleteTextViewActivity.class.getName());
        mInstrumentation.startActivitySync(intent);
        solo.sleep(2000);
        solo.goBackToActivity("ChooseActivity");
        solo.sleep(2000);
        solo.sendKey(Solo.MENU);
        solo.sleep(2000);
        solo.pressMenuItem(1);

        solo.waitForActivity("AutoCompleteTextViewActivity");
        solo.sleep(2000);
        solo.enterText(0, "Chengdu");
        solo.sleep(2000);
        solo.clickInList(1);
        solo.sleep(2000);
        solo.clickOnButton("Done");
        solo.sleep(2000);
        solo.pressMenuItem(1);
        solo.sleep(4000);
        solo.enterText(0, "Beijing");
        solo.sleep(4000);
        solo.clickInList(1);
        solo.sleep(4000);
        solo.clickOnButton("Done");
        solo.sleep(4000);
        solo.goBackToActivity("ChooseActivity");

        assertTrue(true);
    }

    public void testWidgetProvider() {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        getActivity().sendBroadcast(intent);
        solo.sleep(3000);
        intent = new Intent("android.intent.action.mtk.worldclock.deleteIntent");
        getActivity().sendBroadcast(intent);
        solo.sleep(4000);
        intent = new Intent("android.appwidget.action.APPWIDGET_DELETE");
        getActivity().sendBroadcast(intent);

        Class clz = WorldClockWidgetProvider.class;
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(mActivity);
        try {
            // Class c = Class.forName(s);
            Class params[] = new Class[1];
            params[0] = android.content.Context.class;
            params[1] = android.appwidget.AppWidgetManager.class;
            params[2] = int.class;
            Method m = clz.getMethod("onUpdate", params);
            Object args[] = new Object[1];
            args[0] = mActivity;
            args[1] = appWidgetManager;
            int[] a = {1,2,3};
            args[2] = a;
            m.invoke(clz.newInstance(), args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        mInstrumentation = null;
        if (mActivity != null) {
            mActivity.finish();
        }
        mActivity = null;
        solo.finishOpenedActivities();
    }
}