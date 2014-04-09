/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.schpwronoff.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.mediatek.schpwronoff.R;
import com.mediatek.schpwronoff.*;
import com.jayway.android.robotium.solo.Solo;
import com.mediatek.xlog.Xlog;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class SchPwrOnOffTest extends ActivityInstrumentationTestCase2<AlarmClock> {

    private final String TAG = "SchPwrOnOffTest";
    private static final String KEY_SET_TIME = "time";
    private static final String KEY_SET_REPEAT = "setRepeat";
    private static final int KEY_LIST_COUNT = 2;
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    private Solo mSolo;
    private PreferenceActivity mActivity;
    private Context mContext;
    private Instrumentation mInstrumentation;

    private ListView mAlarmListView;



    public SchPwrOnOffTest() {
        super(AlarmClock.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        mActivity = getActivity();
        mSolo = new Solo(mInstrumentation, mActivity);
        mAlarmListView = (ListView) mActivity.findViewById(android.R.id.list);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        try{
        	mSolo.finishOpenedActivities();
        }catch(Exception e){
        	Xlog.d(TAG, "tearDown exception");;
        }
    }

	public void test01_Precondition() {
		Xlog.d(TAG, "test01_Precondition()");
		assertNotNull(mActivity);
		assertNotNull(mSolo);
                setDisable();
	}


    public void testCase02_CheckBoxInitStatus() throws Throwable {
        Xlog.d(TAG, "testCase02_CheckBoxInitStatus()");
        for (int i = 0; i < KEY_LIST_COUNT; i++) {
            mSolo.sleep(500);
            checkBoxStatusTest(i);
        }
            setDisable();
    }

    public void testCase03_CheckBoxClickTest() throws Throwable {
        Xlog.d(TAG, "testCase03_CheckBoxClickTest()");
        for (int i = 0; i < KEY_LIST_COUNT; i++) {
            mSolo.clickOnCheckBox(i);
            mSolo.sleep(1000);
            checkBoxStatusTest(i);
            mSolo.sleep(500);
        }
            setDisable();
    }

    public void testCase04_SetAlarmTextTest() throws Throwable {
        Xlog.d(TAG, "testCase04_ListViewClickTest()");
            for (int i = 0; i < KEY_LIST_COUNT; i++) {
            mSolo.clickInList(i+1);
            mSolo.sleep(500);

            TextView timeTextView = mSolo.getText(mSolo.getCurrentActivity()
					    .getString(R.string.schedule_power_on_off_time));
            RelativeLayout parentLayout = (RelativeLayout) timeTextView.getParent();
            TextView timeSummary = (TextView) parentLayout.getChildAt(1);
            Xlog.d(TAG, "summary text view value :"+timeSummary.getText());

            TextView repeatTextView = mSolo.getText(mActivity
					    .getString(R.string.schedule_power_on_off_repeat));
            parentLayout = (RelativeLayout) repeatTextView.getParent();
            TextView repeatSummary = (TextView) parentLayout.getChildAt(1);
            Xlog.d(TAG, "summary text view value :"+repeatSummary.getText());

            DataInDB mDataInDB = new DataInDB(mSolo.getCurrentActivity(), i);
            String timeSummaryInDB = mDataInDB.timeAmPmInDB;
            Xlog.d(TAG, "timeSummaryInDB text view value :"+timeSummaryInDB);
            String repeateSummaryInDB = mDataInDB.daysOfWeekInDB;
            Xlog.d(TAG, "repeateSummaryInDB text view value :"+repeateSummaryInDB+";");

            assertEquals(timeSummary.getText(), timeSummaryInDB);
            if(repeateSummaryInDB.length()!=0){
                assertEquals(repeatSummary.getText(), repeateSummaryInDB);
            }
            mSolo.sendKey(KeyEvent.KEYCODE_BACK);
        }
            setDisable();
    }


    public void testCase05_DigitalTimeTest() throws Throwable {
        Xlog.d(TAG, "testCase05_DigitalTimeTest()");
        int randomPos = 1;//(int) (2 * Math.random());
        Xlog.d(TAG, "testCase05 randomPos="+randomPos);
        DataInDB mOldDataInDB = new DataInDB(mSolo.getCurrentActivity(), randomPos-1);
        final Date oldDataInDB = mOldDataInDB.dateInDB;
        setTime(randomPos,10,0);
        mSolo.sleep(1000);
        DataInDB mNewDataInDB = new DataInDB(mSolo.getCurrentActivity(), randomPos-1);
        final Date newDataInDB = mNewDataInDB.dateInDB;

        assertEquals(newDataInDB.getHours(),10);
        assertEquals(newDataInDB.getMinutes(),0);

        setTime(randomPos,oldDataInDB.getHours(),oldDataInDB.getMinutes());
        mSolo.sleep(500);
        //mSolo.goBack();

        setDisable();
    }


    public void testCase06_SetRepeateTest() throws Throwable {
        Xlog.d(TAG, "testCase06_SetRepeateTest()");
        int randomPos = (int) (2 * Math.random());
        int randomDay = (int) (7 * Math.random());
        DataInDB mOldDataInDB = new DataInDB(mSolo.getCurrentActivity(), randomPos);
        int oldDaySet = mOldDataInDB.mDaysInDB;
        mSolo.sleep(500);
        setDay(randomPos+1,randomDay,false);
        mSolo.sleep(1000);
        DataInDB mNewDataInDB = new DataInDB(mSolo.getCurrentActivity(), randomPos);
        int newDaySet = mNewDataInDB.mDaysInDB;
        Xlog.d(TAG, "oldday="+oldDaySet+"newday="+newDaySet+"randomday="+randomDay);
        assertEquals(1<<(randomDay),oldDaySet^newDaySet);
        setDisable();
    }


    //test power off while the screen is on
    public void testCase07_PowerOffTest() throws Throwable {
        Xlog.d(TAG, "testCase07_PowerOffTest()");
        //set power off time
        Calendar setCal = Calendar.getInstance();
        setCal.add(Calendar.MINUTE,2);
        int hour = setCal.getTime().getHours();
        int minute = setCal.getTime().getMinutes();
        setTime(2,hour,minute);
        //set days of week
        DataInDB mDataInDB = new DataInDB(mSolo.getCurrentActivity(), 1);
        int daySet = mDataInDB.mDaysInDB;
        int today = setCal.getTime().getDay();
        if(today==0){
            today=6;
        }else{
            today-=1;
        }
        Xlog.d(TAG, "today="+today+";");
        if((daySet&(1<<today))==0)
            setDay(2,today,true);
        //aquir wake lock
        PowerManager pm =(PowerManager)mSolo.getCurrentActivity().
                            getSystemService(mSolo.getCurrentActivity().POWER_SERVICE);
        PowerManager.WakeLock sCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK|
                PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        sCpuWakeLock.acquire();

        //get power off activity
        boolean shutdown = mSolo.waitForActivity("ShutdownActivity", 120000);
        assertTrue(shutdown);
        assertEquals(Calendar.getInstance().getTime().getHours(),hour);
        assertEquals(Calendar.getInstance().getTime().getMinutes(),minute);

        sCpuWakeLock.release();
        mSolo.clickOnButton(mActivity.getString(com.android.internal.R.string.no));

        setDisable();
    }

    public void testCase08_powerOnReciver(){
        final int timeout = 2000;
        Intent intent = new Intent(mActivity, com.mediatek.schpwronoff.SchPwrOnReceiver.class);
        
        Alarm alarm = Alarms.getAlarm(mActivity.getContentResolver(), 1);
        long now = System.currentTimeMillis();
        alarm.mTime = now + 1000;
        Parcel out = Parcel.obtain();
        alarm.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(ALARM_RAW_DATA, out.marshall());
        
        mActivity.sendBroadcast(intent);
        
        mSolo.sleep(timeout);
    }
    
    
    // test init status of the CheckBox
    private void checkBoxStatusTest(int id) {
        Alarm alarm = Alarms.getAlarm(mActivity.getContentResolver(),
                id+1);
        Xlog.d(TAG, "checkBoxStatusTest , id = "+id+" mSolo.isCheckBoxChecked(id): "+mSolo.isCheckBoxChecked(id)
        		+" alarm.mEnabled: "+alarm.mEnabled);
        assertEquals(mSolo.isCheckBoxChecked(id), alarm.mEnabled);
    }

    // make sure the power on/off disable
    private void setDisable() {
        for (int i = 0; i < KEY_LIST_COUNT; i++) {
        	if (mSolo.isCheckBoxChecked(i)) {
                mSolo.clickOnCheckBox(i);
                mSolo.sleep(1000);
        	}
        }
    }

    //id:1 on;2 off
    private void setTime(final int id,final int hour,final int min)throws Throwable{
        mSolo.clickInList(id);
        mSolo.sleep(500);
        mSolo.clickInList(1);
        mSolo.sleep(800);
        final ArrayList<TimePicker> timePickerList = mSolo.getCurrentTimePickers();
        assertEquals(timePickerList.size(),1);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mSolo.setTimePicker(timePickerList.get(0),hour,min);
            }
        });
        mSolo.sleep(500);
        mSolo.clickOnButton(0);
        mSolo.sleep(500);
        mSolo.clickOnMenuItem(mActivity.getString(R.string.done));
        mSolo.sleep(500);
    }
    //id:0 on;1 off
    private void setDay(final int id,final int day,final boolean set) throws Throwable{
        String[] weekdays = new DateFormatSymbols().getWeekdays();
        String[] values = new String[] {
            weekdays[Calendar.MONDAY],
            weekdays[Calendar.TUESDAY],
            weekdays[Calendar.WEDNESDAY],
            weekdays[Calendar.THURSDAY],
            weekdays[Calendar.FRIDAY],
            weekdays[Calendar.SATURDAY],
            weekdays[Calendar.SUNDAY],
        };
        mSolo.clickInList(id);
        mSolo.sleep(1000);
        mSolo.clickInList(2);
        mSolo.sleep(1000);
        Xlog.d(TAG,"day:"+day+";");
        Xlog.d(TAG,"values:"+values[day]+";");
        if(!mSolo.isTextChecked(values[day]) || !set){
            mSolo.clickInList(day+1);
        }
        mSolo.sleep(1000);
        mSolo.clickOnButton("OK");
        mSolo.sleep(2000);
        mSolo.clickOnMenuItem(mActivity.getString(R.string.done));
        mSolo.sleep(2000);
    }

    private class DataInDB {

        public String daysOfWeekInDB;
        public String timeInDB;
        public String timeAmPmInDB;
        public boolean isMorning;
        boolean is24HourFormat;
        public Date dateInDB;
        public int mDaysInDB;

        public Calendar mCalendar;
        public Cursor mCursor;
        private final static String M12 = "h:mm";
        private final static String M12AMPM = "h:mm aa";
        private final static String M24 = "kk:mm";

        private int[] DAY_MAP = new int[] { Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
                Calendar.SATURDAY, Calendar.SUNDAY, };
        private final String[] ALARM_QUERY_COLUMNS = { Alarm.Columns._ID,
                Alarm.Columns.HOUR, Alarm.Columns.MINUTES,
                Alarm.Columns.DAYS_OF_WEEK, Alarm.Columns.ALARM_TIME,
                Alarm.Columns.ENABLED, Alarm.Columns.VIBRATE,
                Alarm.Columns.MESSAGE, Alarm.Columns.ALERT };

        DataInDB(Context context, int id) {
            mCursor = Alarms.getAlarmsCursor(context.getContentResolver());
            assertNotNull(mCursor);
            mCursor.moveToPosition(id);
            mDaysInDB = mCursor
                    .getInt(Alarm.Columns.ALARM_DAYS_OF_WEEK_INDEX);
            daysOfWeekInDB = this.daysOfWeekToString(context, mDaysInDB, false);

            // get time in db
            mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.HOUR_OF_DAY,
                    mCursor.getInt(Alarm.Columns.ALARM_HOUR_INDEX));
            mCalendar.set(Calendar.MINUTE,
                    mCursor.getInt(Alarm.Columns.ALARM_MINUTES_INDEX));

            is24HourFormat = android.text.format.DateFormat
                    .is24HourFormat(context);
            String mFormat = is24HourFormat ? M24 : M12;
            timeInDB = DateFormat.format(mFormat, mCalendar).toString();
            String mAmPmFormat = is24HourFormat ? M24 : M12AMPM;
            timeAmPmInDB = (String) DateFormat.format(mAmPmFormat, mCalendar);

            // get isMmorning in DB
            isMorning = (mCalendar.get(Calendar.AM_PM) == 0);
            dateInDB = mCalendar.getTime();
            mCursor.close();
        }

        // change value of daysOfWeek from int to string
        private String daysOfWeekToString(Context context, int mDays,
                boolean showNever) {
            StringBuilder ret = new StringBuilder();
            // no days
            if (mDays == 0) {
                return showNever ? context.getText(R.string.never).toString()
                        : "";
            }
            // every day
            if (mDays == 0x7f) {
                return context.getText(R.string.every_day).toString();
            }
            // count selected days
            int dayCount = 0, days = mDays;
            while (days > 0) {
                if ((days & 1) == 1)
                    dayCount++;
                days >>= 1;
            }
            // short or long form?
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] dayList = (dayCount > 1) ? dfs.getShortWeekdays() : dfs
                    .getWeekdays();
            // selected days
            for (int i = 0; i < 7; i++) {
                if ((mDays & (1 << i)) != 0) {
                    ret.append(dayList[DAY_MAP[i]]);
                    dayCount -= 1;
                    if (dayCount > 0)
                        ret.append(context.getText(R.string.day_concat));
                }
            }
            return ret.toString();
        }

    }
}
