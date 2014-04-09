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
import android.content.Context;
import android.database.Cursor;
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

public class PwrOffTest extends
        ActivityInstrumentationTestCase2<AlarmClock> {

    private final String TAG = "PwrOffTest";
    private Solo mSolo;
    private PreferenceActivity mActivity;
    private Context mContext;
    private Instrumentation mInstrumentation;
    private ListView mAlarmListView;



    public PwrOffTest() {
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
   
    //test power on while the screen is off
    public void testCase01_PowerOnTest() throws Throwable {
        Xlog.i(TAG, "testCase01_PowerOnTest()");
        //set power off time
        Calendar setCal = Calendar.getInstance();
        setCal.add(Calendar.MINUTE,2);
        int hour = setCal.getTime().getHours();
        int minute = setCal.getTime().getMinutes();
        setTime(1,hour,minute);
    }
    
    //test power off while the screen is on
    public void testCase02_PowerOffTest() throws Throwable {
    	Xlog.i(TAG, "testCase01_PowerOnTest()");
    	mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER));
    	mSolo.sleep(2000);
    	mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER));
    	mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
    	mSolo.sleep(1000);
    	mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
    	mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
    	Xlog.i(TAG, "[Performance test][Settings] PowerOff start time ["+System.currentTimeMillis()+"]" );
    	mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
    	
    	  	
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
