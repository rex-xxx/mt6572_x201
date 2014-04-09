/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.MediatekDM.polling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.mediatek.MediatekDM.option.Options;
import com.mediatek.MediatekDM.util.FileLogger;

import java.util.Date;
import java.util.Random;

public class PollingScheduler {
    private static final String TAG = "PollingScheduler";

    private static final String PREF_NAME = "polling_sched";
    private static final String ALARM_TIME = "alarm_time";

    private static PollingScheduler inst = null;

    private Context context;
    private SharedPreferences settings;

    public static PollingScheduler getInstance(Context cxt) {
        if (inst == null) {
            inst = new PollingScheduler(cxt);
        }
        return inst;
    }

    /***
     * check if a polling alarm is time-up when boot completed.<br>
     * 1. alarm is not set ==> set an alarm after 2 weeks.<br>
     * 2. alarm is not time up ==> reset the alarm to alarm manager.<br>
     * 3. alarm time is over ==> should do polling now.<br>
     * 
     * @return false for 1,2, true for 3
     */
    public boolean checkTimeup() {
        long alarmTime = settings.getLong(ALARM_TIME, 0L);
        if (alarmTime == 0L) {
            // set an alarm if it's never set.
            Log.d(TAG, "alarm never set, set one now.");
            setNextAlarm();
            return false;
        }

        long currTime = System.currentTimeMillis();
        if (currTime < alarmTime) {
            // reset the alarm after reboot.
            Log.d(TAG, "reset alarm after reboot.");
            setAlarm(alarmTime);
            return false;
        }

        Log.d(TAG, "the saved alarm is time up!");
        return true;
    }

    /***
     * set a random alarm in around 10~17 days to alarm manager, and also save
     * to a pref file.
     */
    public void setNextAlarm() {
        long alarmTime = getNextTime();

        // set to alarm manager
        setAlarm(alarmTime);

        // save to pref file
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(ALARM_TIME, alarmTime);
        editor.commit();
        Log.d(TAG, "saved to preference:" + alarmTime);

        FileLogger.getInstance(context).logMsg("set alarm at " + new Date(alarmTime));
    }

    private void setAlarm(long time) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) {
            Log.e(TAG, "get alarm manager failed");
            return;
        }

        Intent intent = new Intent();
        intent.setClass(context, PollingService.class);
        intent.setAction(PollingService.ACTION);
        PendingIntent pending = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.cancel(pending);

        alarmMgr.set(AlarmManager.RTC_WAKEUP, time, pending);
        Log.d(TAG, "set to alarm manager:" + time);
    }

    private long getNextTime() {
        long curTime = System.currentTimeMillis();

        Random rd = new Random(curTime);
        long alarmTime = curTime + Options.Polling.INTERVAL_BASE
                + rd.nextInt(Options.Polling.INTERVAL_RANDOM);

        return alarmTime;
    }

    private PollingScheduler(Context cxt) {
        context = cxt;
        settings = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

}
