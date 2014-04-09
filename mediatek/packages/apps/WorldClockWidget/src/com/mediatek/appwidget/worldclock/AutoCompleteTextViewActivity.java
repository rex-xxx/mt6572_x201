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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import com.mediatek.xlog.Xlog;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class AutoCompleteTextViewActivity extends Activity {
	static final String TAG = "AutoCompleteTextViewActivity";

	private int mAppwidgetId;

	private int mCityNumberInXml;

	private final int LEN = 50;
	private static final int TIMEZONE_ID = 0;

	private static final int WEATHER_ID = 1;

	private static final String ON_CLICK_APPWIDGETID = "onClickAppWidgetId";

	private ArrayList<String> mCityNameArrayBak = new ArrayList<String>();

	private ArrayList<String> mTimeZoneArray = new ArrayList<String>();

	private ArrayList<String> mWeatherIDArray = new ArrayList<String>();

	private ArrayList<String> mAdapterCityArray = new ArrayList<String>();

	private ArrayList<String> mAdapterLocalCityArray = new ArrayList<String>();

	private AutoCompleteTextView mAutoComplete;

	private ClockCityInfo mCityInfo = new ClockCityInfo();

	/**
	 * reference googleSearch behavior,when mAutoComplete obtain focus then auto
	 * pop up IME
	 */
	private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			if (mAutoComplete.isFocused()) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(mAutoComplete, 0);
			}
		}
	};

	/**
	 * create an onkeyListener
	 */
	private View.OnKeyListener onkeyListener = new View.OnKeyListener() {
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (!event.isLongPress() && KeyEvent.ACTION_UP == event.getAction()
					&& keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(mAutoComplete, 0);
			}
			return false;
		}
	};

	Button.OnClickListener buttonListener = new Button.OnClickListener() {
		public void onClick(View view) {
			String cityName = null;
			String weatherID = null;
			cityName = String.valueOf(mAutoComplete.getText());
			mCityInfo = setCityInfo(cityName);
			String timezone = null;
			if (mCityInfo.getTimeZone() == null) {
				/* no right city */
				Toast.makeText(AutoCompleteTextViewActivity.this,
						R.string.select_right_city, Toast.LENGTH_SHORT).show();
				return;
			} else {
			}
			Intent intentAutoToChoose = new Intent();
			intentAutoToChoose.putExtra("citynamefromauto", cityName);
			Log.i("citynamefromautoweatherid", mCityInfo.getWeatherID()
					+ "        mm");
			intentAutoToChoose.putExtra("citynamefromautoweatherid", mCityInfo
					.getWeatherID());
			AutoCompleteTextViewActivity.this.setResult(10, intentAutoToChoose);

			finish();
		}
	};

	private ClockCityInfo setCityInfo(String cityName) {
		ClockCityInfo cityInfo = new ClockCityInfo();
		cityInfo.setCityName(cityName);
		for (int i = 0; i < mCityNumberInXml; i++) {
			if (mCityNameArrayBak.get(i).equals(cityName)) {
			    cityInfo.setIndex(String.valueOf(i));
				cityInfo.setTimeZone(mTimeZoneArray.get(i));
				cityInfo.setWeatherID(mWeatherIDArray.get(i));
				break;
			}
		}
		return cityInfo;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			Xlog.d(TAG, "bundle != null");
			mAppwidgetId = bundle.getInt(ON_CLICK_APPWIDGETID);
		}
		if (mTimeZoneArray.isEmpty()) {
			Xlog.d(TAG, "mTimeZoneArray.isEmpty()");
			getZones();
		}
		Xlog.d(TAG, "mAppwidgetId = " + mAppwidgetId);
		setContentView(R.layout.auto_complete);
		setTitle(getResources().getString(R.string.search_city));

		AutoCompleteTextViewArrayAdapter<String> adapter = new AutoCompleteTextViewArrayAdapter<String>(
				getApplicationContext(),
				android.R.layout.simple_dropdown_item_1line, mAdapterCityArray
						.toArray(new String[0]));
		Comparator<String> comparator = getComparator();
		adapter.sort(comparator);
        mAutoComplete = (AutoCompleteTextView) findViewById(R.id.textview);
        mAutoComplete.setAdapter(adapter);
        mAutoComplete.addTextChangedListener(new TextWatcher() {
            private CharSequence temp = null;

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                int start = mAutoComplete.getSelectionStart();
                int end = mAutoComplete.getSelectionEnd();
                if (temp.length() > LEN) {
                    Log.v(TAG, "start = " + start + " end = " + end);
                    if(start > LEN){
                        start = LEN + 1;
                    }
                    arg0.delete(start - 1, end);
                    int tempIndex = start;
                    mAutoComplete.setText(arg0);
                    mAutoComplete.setSelection(tempIndex - 1);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) {
                // TODO Auto-generated method stub
                temp = arg0;
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                    int arg3) {
                // TODO Auto-generated method stub
            }

        });
		mAutoComplete.setOnKeyListener(onkeyListener);
		mAutoComplete.setOnFocusChangeListener(focusChangeListener);
		
//		mAutoComplete
//				.setOnClickListener(new AutoCompleteTextView.OnClickListener() {
//					public void onClick(View v) {
//						Xlog.d(TAG, "onClick");
//						mAutoComplete.showDropDown();
//					}
//				});
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				public void run() {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(mAutoComplete, 0);
				}
			}, 300);
		}
		Button nextButton = (Button) findViewById(R.id.next);
		nextButton.setOnClickListener(buttonListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * get information from a xml file.
	 */
	public void getZones() {
		XmlResourceParser xrp = null;
		String localCity = getLocalGMTString();
		try {
			xrp = getResources().getXml(R.xml.timezones);
			while (xrp.next() != XmlResourceParser.START_TAG) {
				continue;
			}
			xrp.next();
			int readCount = 0;
			String tempCitys[] = new String[ClockCityUtils.MAX_CITY_SIZE];
			String tempZones[] = new String[ClockCityUtils.MAX_CITY_SIZE];
			String tempWeahterID[] = new String[ClockCityUtils.MAX_CITY_SIZE];
			while (xrp.getEventType() != XmlResourceParser.END_TAG) {
				while (xrp.getEventType() != XmlResourceParser.START_TAG) {
					if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
						return;
					}
					xrp.next();
				}
				if (xrp.getName().equals(ClockCityUtils.XML_TAG_TIME_ZONE)) {
					String id = xrp.getAttributeValue(TIMEZONE_ID);
					String weatherID = xrp.getAttributeValue(WEATHER_ID);
					String displayName = xrp.nextText();
					if (readCount < ClockCityUtils.MAX_CITY_SIZE) {
						mCityNameArrayBak.add(displayName);
						mAdapterCityArray.add(displayName);
						mTimeZoneArray.add(id);
						if (id.equals(localCity)) {
							mAdapterLocalCityArray.add(displayName);
						}
						mWeatherIDArray.add(weatherID);
						readCount++;
					}
				}
				while (xrp.getEventType() != XmlResourceParser.END_TAG) {
					xrp.next();
				}
				xrp.next();
			}
			mCityNumberInXml = readCount;
			xrp.close();
		} catch (XmlPullParserException xppe) {
			Xlog.w(TAG, "Ill-formatted timezones.xml file");
		} catch (java.io.IOException ioe) {
			Xlog.w(TAG, "Unable to read timezones.xml file");
		} finally {
			if (null != xrp) {
				xrp.close();
			}
		}
	}

	private String getLocalGMTString() {
		TimeZone tz = TimeZone.getTimeZone(TimeZone.getDefault().getID());
		int offset = tz.getOffset(Calendar.getInstance().getTimeInMillis());
		int p = Math.abs(offset);
		StringBuilder name = new StringBuilder();
		name.append("GMT");

		if (offset < 0) {
			name.append('-');
		} else {
			name.append('+');
		}
		int hour = p / (3600000);
		if (hour < 10) {
			name.append('0');
			name.append(hour);
		} else {
			name.append(hour);
		}
		name.append(':');

		int min = p / 60000;
		min %= 60;

		if (min < 10) {
			name.append('0');
		}
		name.append(min);
		return name.toString();
	}

	private Comparator<String> getComparator() {
		Comparator<String> comparator = new Comparator<String>() {
			private final Collator collator = Collator.getInstance();

			public int compare(String object1, String object2) {
				return collator.compare(object1, object2);
			}
		};
		return comparator;
	}
}
