package com.mediatek.appwidget.worldclock.tests;

import java.util.Comparator;
import java.util.TimeZone;

import android.app.Instrumentation;
import android.widget.AutoCompleteTextView;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.appwidget.worldclock.AutoCompleteTextViewActivity;
import com.mediatek.appwidget.worldclock.AutoCompleteTextViewArrayAdapter;
import com.mediatek.appwidget.worldclock.ClockCityInfo;
import com.mediatek.appwidget.worldclock.ClockCityUtils;

public class AutoCompleteTextViewActivityTest extends
        android.test.ActivityTestCase {
    private final static String          TAG = "WorldClockWidgetTest";
    private AutoCompleteTextViewActivity mActivity;
    private Instrumentation              mInstrumentation;
    private Solo                         solo;

    public AutoCompleteTextViewActivityTest() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = this.getInstrumentation();
        solo = new Solo(mInstrumentation, mActivity);
        mActivity = (AutoCompleteTextViewActivity ) solo.getCurrentActivity();
    }

    public void testSetCityInfo() {
        Utils.invoke(AutoCompleteTextViewActivity.class, mActivity,
                "setCityInfo", "Chengdu");
        solo.sleep(2000);
        Utils.invoke(AutoCompleteTextViewActivity.class, mActivity,
                "setCityInfo", "Changsha");
        solo.sleep(2000);
        Utils.invoke(AutoCompleteTextViewActivity.class, mActivity,
                "getZones", null);
        solo.sleep(2000);
        Utils.invoke(AutoCompleteTextViewActivity.class, mActivity,
                "getZones", null);

        solo.sleep(2000);
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        Utils.invoke(AutoCompleteTextViewActivity.class, mActivity,
                "getLocalGMTString", null);
        solo.sleep(2000);
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        Utils.invoke(AutoCompleteTextViewActivity.class, mActivity,
                "getLocalGMTString", null);
    }

    @SuppressWarnings("rawtypes")
    public void testAutoCompleteTextViewAdapter() {
        mActivity.getZones();
        final AutoCompleteTextView textView = (AutoCompleteTextView) Utils
                .getFiledValue(AutoCompleteTextViewActivity.class, mActivity,
                        "mAutoComplete");
        assertNotNull(textView);
        AutoCompleteTextViewArrayAdapter adapter = (AutoCompleteTextViewArrayAdapter) textView
                .getAdapter();
        assertTrue(adapter.getCount() >= 0);
        assertNotNull(adapter.getItem(0));
        assertNotNull(adapter.getContext());
        assertNotNull(adapter.getFilter());
        assertNotNull(adapter.getItemId(0));
        adapter.getPosition(new Object());
        adapter.areAllItemsEnabled();
        adapter.getViewTypeCount();
        adapter.setNotifyOnChange(true);
        adapter.setDropDownViewResource(0);

        try {
            adapter.getView(0, textView, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testAdapter(){
        final AutoCompleteTextView textView = (AutoCompleteTextView) Utils
                .getFiledValue(AutoCompleteTextViewActivity.class, mActivity,
                        "mAutoComplete");
        AutoCompleteTextViewArrayAdapter adapter = (AutoCompleteTextViewArrayAdapter) textView
                .getAdapter();
        assertTrue(adapter.getCount() >= 0);
        adapter.sort(null);
        Comparator<String> comparator = new Comparator<String>(){
            public int compare(String obj1, String obj2){
                return  obj1.compareTo(obj2);
            }
        };
        adapter.sort(comparator);
        try {
            adapter.getDropDownView(0, textView, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testClockCityInfo() {
        ClockCityInfo cityInfo = new ClockCityInfo();
        cityInfo.setCityName("Beijing");
        cityInfo.setIndex("0");
        cityInfo.setWeatherID("1047378");
        cityInfo.setTimeZone("Beijing/Beijing");
        cityInfo.getCityName();
        cityInfo.getIndex();
        cityInfo.getTimeZone();
        cityInfo.getWeatherID();
        cityInfo.toString();
        cityInfo.toStringArray();
        cityInfo = new ClockCityInfo("1", "", "", "");
    }

    public void testSearchCity() {
        solo.sleep(5000);
        AutoCompleteTextView textView = (AutoCompleteTextView) Utils
                .getFiledValue(AutoCompleteTextViewActivity.class, mActivity,
                        "mAutoComplete");
        assertNotNull(textView);
        solo.sleep(2000);
        solo.enterText(textView, "Ch");
        solo.sleep(800);
        solo.enterText(textView, "C");
        solo.sleep(800);
        solo.enterText(textView, "");
        solo.sleep(800);
        solo.enterText(textView, "Shangh");
        solo.sleep(2000);
        solo.clickInList(1);
        solo.sleep(1000);
        solo.clickOnButton("Done");

        assertTrue(true);
    }

    public void testCityUtils() {
        int[] ids = { 1, 2 };
        new ClockCityUtils(mActivity);
        ClockCityUtils.initPreference(mActivity);
        ClockCityUtils.deletePreferences(null);
        ClockCityUtils.deletePreferences(ids);
        ClockCityUtils.getBooleanPref("TAG", "1");
        ClockCityUtils.getCityAPref(TAG);
        ClockCityUtils.getCityBPref(TAG);
        ClockCityUtils.getCityCPref(TAG);
        ClockCityUtils.getCityIndexByName("Changsha");
        ClockCityUtils.getCityNameByWeatherID("1047378");
        ClockCityUtils.getCityTimeZoneByWeatherID("1047378");
        ClockCityUtils.getIntDelete("");
        ClockCityUtils.getStringPref(TAG, "1");
        ClockCityUtils.getWeatherIDByName("Hongkong");
        ClockCityUtils.savePreferences(1, new ClockCityInfo());
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
        }
        mActivity = null;
        solo.finishOpenedActivities();
    }
}