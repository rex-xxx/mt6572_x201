/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.appwidget.worldclock;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.mediatek.xlog.Xlog;

import android.view.View;
import android.widget.RemoteViews;

public class WorldClockWidgetProvider extends AppWidgetProvider {

	private static final String TAG = "WorldClockWidgetProvider";

	private static final String ON_CLICK_APPWIDGETID = "onClickAppWidgetId";

	private static final int ANOTHER_BUTTON = 1;
	private static final String DELETE_INTENT = "android.intent.action.mtk.worldclock.deleteIntent";
	@Override
	public void onReceive(Context context, Intent intent) {
		if(DELETE_INTENT.equals(intent.getAction())){
			String weatherNameDelete = intent.getStringExtra("weatherNameDelete");
			if (weatherNameDelete != null && (!"".equals(weatherNameDelete.trim()))) {
				ClockCityUtils.initPreference(context);
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
				ArrayList<Integer> mAppwidgetIdFromDeletedArrayList = ClockCityUtils.getIntDelete(weatherNameDelete);
				if (mAppwidgetIdFromDeletedArrayList != null
						&& mAppwidgetIdFromDeletedArrayList.size() > 0) {
					int[] mAppwidgetIdFromDeleted = new int[mAppwidgetIdFromDeletedArrayList.size()];
					for (int i = 0; i < mAppwidgetIdFromDeletedArrayList.size(); i++) {
						mAppwidgetIdFromDeleted[i] = mAppwidgetIdFromDeletedArrayList.get(i);
					}
					ClockCityUtils.deletePreferences(mAppwidgetIdFromDeleted);
					Xlog.d(TAG, "WorldClockWidgetProvider delete city weatherNameDelete = " + weatherNameDelete);
					onUpdate(context, appWidgetManager, mAppwidgetIdFromDeleted);
				}
			}
		}
		super.onReceive(context, intent);
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		ClockCityUtils.initPreference(context);
		final int N = appWidgetIds.length;
		Xlog.d(TAG, "onUpdate N = " + N);
		ClockCityInfo cityInfo;
		for (int i = 0; i < N; i++) {
			final int appWidgetId = appWidgetIds[i];
			cityInfo = ClockCityUtils.getCityCPref(String.valueOf(appWidgetId));
			final String weatherID = cityInfo.getWeatherID();
			Xlog.d(TAG, "cityInfo = " + "cityName = " + cityInfo.getCityName());
			updateCity(context, appWidgetId, cityInfo);
			if (null != cityInfo.getCityName()) {
				final ClockCityInfo newCityInfo = cityInfo;
				new AsyncTask<Void, Void, Void>() {
					protected Void doInBackground(Void... args) {
						ClockCityUtils.reLoadTimeZone(context);
						final String newCityName = ClockCityUtils
								.getCityNameByWeatherID(weatherID);
						newCityInfo.setCityName(newCityName);
						
						ClockCityInfo cityInfo = ClockCityUtils.getCityCPref(String.valueOf(appWidgetId));
						if(null != cityInfo.getCityName()){
							Xlog.d(TAG, "updateCity cityInfo = " + cityInfo);
							updateCity(context, appWidgetId, newCityInfo);
							ClockCityUtils.savePreferences(appWidgetId, newCityInfo);
						}
						return null;
					}
				}.execute();
			}
		}
	}

	private static PendingIntent createPendingIntent(Context context,
			int appWidgetId, int buttonId) {
		Intent intent = new Intent();
		Bundle bundle = new Bundle();
		Xlog.d(TAG, "setPendingIntent appWidgetId = " + appWidgetId);
		bundle.putInt(ON_CLICK_APPWIDGETID, appWidgetId);
		// intent.addCategory(ON_CLICK_CATEGORY);
		intent.setClassName(context.getPackageName(), ChooseActivity.class
				.getName());
		intent.setAction(Intent.ACTION_VIEW + appWidgetId);
		intent.putExtras(bundle);
		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
		return pi;
	}

	public static void updateCity(Context context, int appWidgetId,
			ClockCityInfo cityInfo) {
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.world_clock);
		String cityName = cityInfo.getCityName();
		if (null != cityName) {
		    if(cityName.contains(",")){
    		        String[] names = cityName.split(",");
    		        if(names.length == 2 && names[1].contains("D.C.")){
    		            //TODO:
    		        }else{
    		            cityName = names[names.length - 1].trim();
    		        }
		    }
		    if(cityName.length() > 10){
		        cityName = cityName.substring(0, 9) + " ...";
		    }
			views.setTextViewText(R.id.anothercity, cityName);
		}else{
			views.setTextViewText(R.id.anothercity,context.getString(R.string.another_city_text));
		}
		views.setOnClickPendingIntent(R.id.another_button, createPendingIntent(
				context, appWidgetId, ANOTHER_BUTTON));
//		views.setOnClickPendingIntent(R.id.remoteviewallid,
//				createPendingIntent(context, appWidgetId, ANOTHER_BUTTON));
		views.setViewVisibility(R.id.another_button, View.VISIBLE);
		views.setViewVisibility(R.id.anothercitytime, View.INVISIBLE);
		if (null == cityInfo.getCityName()) {
			views.setViewVisibility(R.id.another_button, View.VISIBLE);
			views.setViewVisibility(R.id.anothercitytime, View.INVISIBLE);
		} else {
			views.setViewVisibility(R.id.another_button, View.INVISIBLE);
			views.setViewVisibility(R.id.anothercitytime, View.VISIBLE);
			views.setOnClickPendingIntent(R.id.anothercitytime,
					createPendingIntent(context, appWidgetId, ANOTHER_BUTTON));
		}
		views.setInt(R.id.localcitytime, "setDateHeight",
				R.dimen.style_1_height_date_week);
		views.setInt(R.id.localcitytime, "setDateFormatString",
				R.string.month_day_no_year);
		views.setInt(R.id.localcitytime, "setDateFontSize",
				R.dimen.analog_date_font_size);

		views.setInt(R.id.anothercitytime, "setDateHeight",
				R.dimen.style_1_height_date_week);
		views.setInt(R.id.anothercitytime, "setDateFormatString",
				R.string.month_day_no_year);
		views.setInt(R.id.anothercitytime, "setDateFontSize",
				R.dimen.analog_date_font_size);

		views.setInt(R.id.localcitytime, "setDayDialResource",
				R.drawable.wgt_clock_style1_day_dial);
		views.setInt(R.id.localcitytime, "setDayHourResource",
				R.drawable.wgt_clock_style1_day_hour);
		views.setInt(R.id.localcitytime, "setDayMinuteResource",
				R.drawable.wgt_clock_style1_day_minute);
		views.setInt(R.id.localcitytime, "setNightDialResource",
				R.drawable.wgt_clock_style1_night_dial);
		views.setInt(R.id.localcitytime, "setNightHourResource",
				R.drawable.wgt_clock_style1_night_hour);
		views.setInt(R.id.localcitytime, "setNightMinuteResource",
				R.drawable.wgt_clock_style1_night_minute);
		views.setInt(R.id.localcitytime, "setDayHatResource",
				R.drawable.wgt_clock_style1_day_hat);
		views.setInt(R.id.localcitytime, "setNightHatResource",
				R.drawable.wgt_clock_style1_night_hat);

		views.setInt(R.id.anothercitytime, "setDayDialResource",
				R.drawable.wgt_clock_style1_day_dial);
		views.setInt(R.id.anothercitytime, "setDayHourResource",
				R.drawable.wgt_clock_style1_day_hour);
		views.setInt(R.id.anothercitytime, "setDayMinuteResource",
				R.drawable.wgt_clock_style1_day_minute);
		views.setInt(R.id.anothercitytime, "setNightDialResource",
				R.drawable.wgt_clock_style1_night_dial);
		views.setInt(R.id.anothercitytime, "setNightHourResource",
				R.drawable.wgt_clock_style1_night_hour);
		views.setInt(R.id.anothercitytime, "setNightMinuteResource",
				R.drawable.wgt_clock_style1_night_minute);
		views.setInt(R.id.anothercitytime, "setDayHatResource",
				R.drawable.wgt_clock_style1_day_hat);
		views.setInt(R.id.anothercitytime, "setNightHatResource",
				R.drawable.wgt_clock_style1_night_hat);

		views.setString(R.id.anothercitytime, "setTimeZone", cityInfo
				.getTimeZone());
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		ClockCityUtils.deletePreferences(appWidgetIds);
	}
}
