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

package com.mediatek.appwidget.worldclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParserException;
import com.mediatek.appwidget.worldclock.*;

public class ChooseActivity extends Activity {

    private static final String MTKWORLDCLOCKCHOOSE = "mtkworldclockchoose";
    private ClockCityInfo mCityInfo = new ClockCityInfo();
    private static int mAppwidgetId;
    private static final String ON_CLICK_APPWIDGETID = "onClickAppWidgetId";
    private ArrayList<String> mTimeZoneArray = new ArrayList<String>();
    private static final int TIMEZONE_ID = 0;
    private static final int WEATHER_ID = 1;
    private static final String TAG = "MTKWORLDCHOOSE";
    private static final String DELETE_INTENT = "android.intent.action.mtk.worldclock.deleteIntent";
    private ArrayList<String> mCityNameArrayBak = new ArrayList<String>();
    private ArrayList<String> mWeatherIDArray = new ArrayList<String>();
    private ArrayList<String> mAdapterCityArray = new ArrayList<String>();
    private ArrayList<String> mAdapterLocalCityArray = new ArrayList<String>();
    private int mCityNumberInXml;
    private static final int MENU_ADD = Menu.FIRST;
    private static final int MENU_UPDATE = Menu.FIRST + 1;
    private static final int CHOOSEACTIVITYREQUESTCODE = 10;
    private static final int AUTOCOMPLETECHOOSEACTIVITYRESULTCODE = 10;
    private ListView mListView;
    private TextView mNoCity;
    private SelectedCityListAdapter mChooserAdapter;
    private ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
    private static int mPosition = -1;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.v(TAG, "on create ....");
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            // Xlog.d(TAG, "bundle != null");
            mAppwidgetId = bundle.getInt(ON_CLICK_APPWIDGETID);
        }
        Log.v(TAG, "mAppwidgetId = "  + mAppwidgetId);
        mContext = ChooseActivity.this;
        setContentView(R.layout.choose);
        mListView = (ListView) findViewById(R.id.mtkchooselistviewid);
        mNoCity = (TextView) findViewById(R.id.no_city);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ADD, Menu.NONE, R.string.add_new_city)
                .setIcon(R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                Intent intentAutoCompleteTextView = new Intent(
                        ChooseActivity.this, AutoCompleteTextViewActivity.class);
                ChooseActivity.this.startActivityForResult(
                        intentAutoCompleteTextView, CHOOSEACTIVITYREQUESTCODE);
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSEACTIVITYREQUESTCODE
                && resultCode == AUTOCOMPLETECHOOSEACTIVITYRESULTCODE) {
            String citynamefromautoweatherids = data.getExtras().getString(
                    "citynamefromautoweatherid");
            getData(citynamefromautoweatherids);
        }
    }

    private void getData(String citynamefromautoweatherids) {
        if (data != null || data.size() > 0) {
            data.clear();
        }
        if (null != citynamefromautoweatherids) {
            SharedPreferences sharedPreferences = getSharedPreferences(
                    "chooseshared", MODE_PRIVATE);
            String chooses = sharedPreferences.getString(MTKWORLDCLOCKCHOOSE,
                    "first");
            // delete same city
            String sac[] = chooses.split(",");
            for (int i = 0; i < sac.length; i++) {
                if (citynamefromautoweatherids.equals(sac[i])) {
                    // also exsit ,don't add it
                    citynamefromautoweatherids = "";
                    String text = getCityNameBYWeatherId(sac[i]);
                    if(!TextUtils.isEmpty(text) && text.contains(",")){
                        String temp[] = text.split(",");
                        text = temp[temp.length - 1];
                    }
                    text = text + " " + mContext.getResources().getString(R.string.has_chosen);
                    Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                }
            }
            if (!TextUtils.isEmpty(citynamefromautoweatherids)) {
                Editor editor = sharedPreferences.edit();
                editor.putString(MTKWORLDCLOCKCHOOSE, chooses + ","
                        + citynamefromautoweatherids);
                editor.commit();
            }
            chooses = sharedPreferences.getString(MTKWORLDCLOCKCHOOSE, "first");
            if (chooses != null && chooses.length() > 5) {
                String[] datass = chooses.split(",");
                for (int i = 1; i < datass.length; i++) {
                    HashMap<String, Object> hashMap = new HashMap<String, Object>();
                    hashMap.put("chooselistviewimageview",
                            R.drawable.mtkchooseimage);
                    String cityNameFromWeatherid = getCityNameBYWeatherId(datass[i]);
                    hashMap.put("chooselistviewtextview", cityNameFromWeatherid);
                    hashMap.put("chooselistviewweatherid", datass[i]);
                    data.add(hashMap);
                }
            }
        }
    }

    private String getCityNameBYWeatherId(String weatherId) {
        // TODO Auto-generated method stub
        String cityName = null;
        for (int i = 0; i < mWeatherIDArray.size(); i++) {
            if (weatherId.equals(mWeatherIDArray.get(i))) {
                cityName = mCityNameArrayBak.get(i);
                break;
            }
        }
        return cityName;
    }

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
            Log.i("aaa", "Ill-formatted timezones.xml file");
        } catch (java.io.IOException ioe) {
            Log.i("bbb", "Unable to read timezones.xml file");
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

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        if (mTimeZoneArray.isEmpty()) {
            Log.i(TAG, "mTimeZoneArray.isEmpty()");
            getZones();
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        getData("");
        // lv.setDividerHeight(20);
        mChooserAdapter = new SelectedCityListAdapter(this, data, mAppwidgetId);
        if (data.size() != 0) {
            mNoCity.setVisibility(View.GONE);
        } else {
            mNoCity.setVisibility(View.VISIBLE);
        }
        mListView.setAdapter(mChooserAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                    long arg3) {
                Log.v(TAG, "on item click .... ");
                // TODO Auto-generated method stub
                String cityName = null;
                String weatherID = null;                
                TextView tv = (TextView) arg1.findViewById(R.id.chooselistviewweatherid);
                weatherID = String.valueOf(tv.getText().toString());
                mCityInfo = setCityInfoByWeatherID(weatherID);
                String timezone = null;
                if (mCityInfo.getTimeZone() == null) {
                    /* no right city */
                    Toast.makeText(ChooseActivity.this,
                            R.string.select_right_city, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                final Context context = ChooseActivity.this;
                ClockCityUtils.initPreference(context);
                ClockCityUtils.savePreferences(mAppwidgetId, mCityInfo);
                WorldClockWidgetProvider.updateCity(context, mAppwidgetId,
                        mCityInfo);
                Log.v(TAG, "position = " + pos);
                mPosition = pos;
                finish();
            }
        });

        mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
                // delete item
                TextView cityNameView = (TextView) arg1.findViewById(R.id.chooseactivitytextviewid);
                final String cityName = String.valueOf(cityNameView.getText().toString());
                TextView tvweather = (TextView) arg1.findViewById(R.id.chooselistviewweatherid);
                final String weatherName = String.valueOf(tvweather.getText()
                        .toString());
                new AlertDialog.Builder(ChooseActivity.this)
                        .setTitle(R.string.delete_city)
                        .setMessage(R.string.delete_city_info)
                        .setPositiveButton(R.string.deleteok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            DialogInterface dialoginterface,
                                            int i) {
                                        // delete item
                                        SharedPreferences sharedPreferences = getSharedPreferences(
                                                "chooseshared", MODE_PRIVATE);
                                        String chooses = sharedPreferences
                                                .getString(MTKWORLDCLOCKCHOOSE,
                                                        "first");
                                        chooses = chooses.replace(","
                                                + weatherName, "");
                                        sharedPreferences
                                                .edit()
                                                .putString(MTKWORLDCLOCKCHOOSE,
                                                        chooses).commit();
                                        if (weatherName != null
                                                && (!"".equals(weatherName
                                                        .trim()))) {
                                            Intent intent = new Intent(
                                                    DELETE_INTENT);
                                            intent.putExtra(
                                                    "weatherNameDelete",
                                                    weatherName);
                                            ChooseActivity.this
                                                    .sendBroadcast(intent);
                                        }
                                        onResume();
                                    }
                                })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        dialog.cancel();
                                    }
                                }).show();
                return true;
            }
        });
        if (mPosition != -1) {
            mListView.setSelection(mPosition);
        }
    }

    private ClockCityInfo setCityInfoByWeatherID(String weatherID) {
        ClockCityInfo cityInfo = new ClockCityInfo();
        cityInfo.setWeatherID(weatherID);
        for (int i = 0; i < mWeatherIDArray.size(); i++) {
            if (mWeatherIDArray.get(i).equals(weatherID)) {
                String timezone = mTimeZoneArray.get(i);
                String cityName = mCityNameArrayBak.get(i);
                cityInfo.setTimeZone(timezone);
                cityInfo.setIndex(String.valueOf(i));
                cityInfo.setCityName(cityName);
                break;
            }
        }
        return cityInfo;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mAppwidgetId = bundle.getInt(ON_CLICK_APPWIDGETID);
        }
        Log.v(TAG, "onNewIntent mAppwidgetId = " + mAppwidgetId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mAppwidgetId = savedInstanceState.getInt(ON_CLICK_APPWIDGETID);
            Log.v(TAG, "onRestoreInstanceState mAppwidgetId = " + mAppwidgetId);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ON_CLICK_APPWIDGETID, mAppwidgetId);
    }
}
