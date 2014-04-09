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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.SystemClock;
import android.util.Log;
import com.mediatek.xlog.Xlog;

public class ClockCityUtils {

    public static final String TAG = "ClockUtils";

    protected static final String PREF_CLOCK_WIDGET_INFO = "pref_clock_widget_info";

    protected static final String PANEL_INDEX = "panelIndex";

    protected static final int STYLE_1 = 0;

    protected static final int STYLE_2 = 1;

    protected static final int STYLE_3 = 2;

    protected static final int MAX_CITY_SIZE = 500;

    protected static final String XML_TAG_TIME_ZONE = "timezone";

    protected static final String STYLE_A_CITY_INFOR = "com.mediatek.appwidget.clock.citya";

    protected static final String STYLE_B_CITY_INFOR = "com.mediatek.appwidget.clock.cityb";

    protected static final String STYLE_C_CITY_INFOR = "com.mediatek.appwidget.clock.cityc";

    /**
     * save old city information in timezones.xml for special language
     */
    protected static final String SHOW_WEATHER = "ShowWeather";

    protected static final String APPWIDGET_STYLE = "WidgetStyle";

    // TODO:should use an id-->(STYLEA_WEATHERA_ID) determine city is not
    // duplicate in timezones.xml
    // the city1 of style1
    // protected static final String STYLEA_WEATHERA_ID = "StyleA_WeatherAID";
    protected static final String STYLEA_CITYA_INDEX = "StyleA_CityAIndex";

    protected static final String STYLEA_CITYA_NAME = "StyleA_CityAName";

    protected static final String STYLEA_CITYA_ZONE = "StyleA_CityAZone";

    protected static final String STYLEA_WEATHERA_ID = "StyleA_WeatherAID";

    protected static final String STYLEA_CITYB_INDEX = "StyleA_CityBIndex";

    protected static final String STYLEA_CITYB_NAME = "StyleA_CityBName";

    protected static final String STYLEA_CITYB_ZONE = "StyleA_CityBZone";

    protected static final String STYLEA_WEATHERB_ID = "StyleA_WeatherBID";

    protected static final String STYLEC_CITY_INDEX = "StyleC_CityIndex";

    protected static final String STYLEC_CITY_NAME = "StyleC_CityName";

    protected static final String STYLEC_CITY_ZONE = "StyleC_CityZone";

    protected static final String STYLEC_WEATHER_ID = "StyleC_WeatherCID";

    private static HashMap<String, ClockCityInfo> mTimeZoneMap = null;

    private static final String NAME_SPACE = null;

    private static final String WEATHER_ID = "weatherID";

    private static final String GMT_ZONE = "id";

    private static SharedPreferences pref = null;

    private static Context context;

    public ClockCityUtils(Context ctx) {
        context = ctx;
        pref = context.getSharedPreferences(PREF_CLOCK_WIDGET_INFO, 0);
    }

    public static void initPreference(Context ctx) {
        pref = ctx.getSharedPreferences(PREF_CLOCK_WIDGET_INFO, 0);
        context = ctx;
    }

    private static HashMap<String, ClockCityInfo> getTimeZoneMap() {
        if (null == mTimeZoneMap || mTimeZoneMap.size() <= 0) {
            initTimeZone();
        }
        return mTimeZoneMap;
    }

    /**
     * @param keyTag
     * @param widgetId
     * @return
     */
    public static int getIntPref(String keyTag, String widgetId) {
        String name = keyTag + widgetId;
        return pref.getInt(name, -1);
    }

    /**
     * @param keyTag
     * @param widgetId
     * @return
     */
    public static String getStringPref(String keyTag, String widgetId) {
        String name = keyTag + widgetId;
        return pref.getString(name, null);
    }

    /**
     * @param keyTag
     * @param widgetId
     * @return
     */
    public static boolean getBooleanPref(String keyTag, String widgetId) {
        String name = keyTag + widgetId;
        return pref.getBoolean(name, false);
    }

    public static ClockCityInfo getCityAPref(String key) {
        ClockCityInfo city1 = new ClockCityInfo();
        String index = getStringPref(STYLEA_CITYA_INDEX, key);
        String weatherID = getStringPref(STYLEA_WEATHERA_ID, key);
        String oldCityName = getStringPref(STYLEA_CITYA_NAME, key);
        // String newName = getCityNameByIndex(index);
        // String newName = getCityNameByWeatherID(weatherID);
        city1.setIndex(index);
        city1.setWeatherID(weatherID);
        city1.setCityName(oldCityName);
        city1.setTimeZone(getStringPref(ClockCityUtils.STYLEA_CITYA_ZONE, key));
        return city1;
    }

    public static ClockCityInfo getCityBPref(String key) {
        ClockCityInfo city2 = new ClockCityInfo();
        String index = getStringPref(STYLEA_CITYB_INDEX, key);
        String weatherID = getStringPref(STYLEA_WEATHERB_ID, key);
        String oldCityName = getStringPref(STYLEA_CITYB_NAME, key);
        // String newName = getCityNameByIndex(index);
        // String newName = getCityNameByWeatherID(weatherID);
        city2.setIndex(index);
        city2.setWeatherID(weatherID);
        city2.setCityName(oldCityName);
        city2.setTimeZone(getStringPref(ClockCityUtils.STYLEA_CITYB_ZONE, key));
        return city2;
    }

    public static ClockCityInfo getCityCPref(String key) {
        ClockCityInfo city3 = new ClockCityInfo();
        String index = getStringPref(STYLEC_CITY_INDEX, key);
        String weatherID = getStringPref(STYLEC_WEATHER_ID, key);
        String oldCityName = getStringPref(STYLEC_CITY_NAME, key);
        // String newName = getCityNameByIndex(index);
        // String newName = getCityNameByWeatherID(weatherID);
        // city3.setIndex(index);
        city3.setWeatherID(weatherID);
        city3.setCityName(oldCityName);
        Xlog.d(TAG, "oldCityName = " + oldCityName);
        city3.setTimeZone(getStringPref(ClockCityUtils.STYLEC_CITY_ZONE, key));
        return city3;
    }

    /**
     * @param name
     * @return return city name from HashMap(timezons.xml) otherwise return null
     */
    public static String getCityIndexByName(String name) {
        HashMap<String, ClockCityInfo> hashMap = getTimeZoneMap();
        if (null == hashMap) {
            return null;
        }
        int size = hashMap.size();
        for (int i = 0; i < size; i++) {
            String key = String.valueOf(i);
            ClockCityInfo cityInfor = hashMap.get(key);
            if (null != cityInfor && null != cityInfor.getCityName()) {
                if (name.equalsIgnoreCase(cityInfor.getCityName())) {
                    return cityInfor.getIndex();
                }
            }
        }
        return null;
    }

    /**
     * @param name
     * @return return city weahterID from HashMap(timezons.xml) otherwise return
     *         null
     */
    public static String getWeatherIDByName(String name) {
        HashMap<String, ClockCityInfo> hashMap = getTimeZoneMap();
        if (null == hashMap) {
            return null;
        }
        int size = hashMap.size();
        for (int i = 0; i < size; i++) {
            String key = String.valueOf(i);
            ClockCityInfo cityInfor = hashMap.get(key);
            if (null != cityInfor && null != cityInfor.getCityName()) {
                if (name.equalsIgnoreCase(cityInfor.getCityName())) {
                    return cityInfor.getWeatherID();
                }
            }
        }
        return null;
    }

    /**
     * @param name
     * @return return city time zone from HashMap(timezons.xml) otherwise return
     *         null
     */
    protected static String getCityTimeZoneByName(String name) {
        HashMap<String, ClockCityInfo> hashMap = getTimeZoneMap();
        if (null == hashMap) {
            return null;
        }
        int size = hashMap.size();
        for (int i = 0; i < size; i++) {
            String key = String.valueOf(i);
            ClockCityInfo cityInfor = hashMap.get(key);
            if (null != cityInfor && null != cityInfor.getCityName()) {
                if (name.equalsIgnoreCase(cityInfor.getCityName())) {
                    return cityInfor.getTimeZone();
                }
            }
        }
        return null;
    }

    /**
     * @param weatherID
     * @return return city time zone from HashMap(timezons.xml) otherwise return
     *         null
     */
    public static String getCityTimeZoneByWeatherID(String weatherID) {
        HashMap<String, ClockCityInfo> hashMap = getTimeZoneMap();
        if (null == hashMap) {
            return null;
        }
        ClockCityInfo cityInfor = hashMap.get(weatherID);
        if (null == cityInfor) {
            return null;
        }
        return cityInfor.getTimeZone();
    }

    /**
     * @param index
     * @return return city name from HashMap(timezons.xml) otherwise return null
     */
    protected String getCityNameByIndex(String index) {
        HashMap<String, ClockCityInfo> hashMap = getTimeZoneMap();
        if (null == hashMap) {
            return null;
        }
        ClockCityInfo cityInfor = hashMap.get(index);
        if (null != cityInfor) {
            if (null == cityInfor.getCityName()
                    || cityInfor.getCityName().trim().length() == 0) {
                return null;
            }
            return cityInfor.getCityName();
        }
        return null;
    }

    /**
     * @param weatherID
     * @return return city name from HashMap(timezons.xml) otherwise return null
     */
    public static String getCityNameByWeatherID(String weatherID) {
        HashMap<String, ClockCityInfo> hashMap = getTimeZoneMap();
        if (null == hashMap) {
            return null;
        }
        ClockCityInfo cityInfor = hashMap.get(weatherID);
        if (null == cityInfor) {
            return null;
        }
        return cityInfor.getCityName();
    }

    /**
     * @param appWidgetManager
     * @param widgetId
     * @param cityA
     * @param cityB
     * @param city
     * @param style
     * @param isShowWeather
     */
    public static void savePreferences(int widgetId, final ClockCityInfo city) {
        String appWidgetId = String.valueOf(widgetId);
        SharedPreferences.Editor preEdt = pref.edit();
        if (null != city && !("-1".equals(city.getIndex()))) {
            Xlog.d(TAG, "ClockUtils save prefs");
            preEdt.putString(STYLEC_CITY_INDEX + appWidgetId, city.getIndex());
            preEdt.putString(STYLEC_WEATHER_ID + appWidgetId,
                    city.getWeatherID());
            preEdt.putString(STYLEC_CITY_NAME + appWidgetId, city.getCityName());
            preEdt.putString(STYLEC_CITY_ZONE + appWidgetId, city.getTimeZone());
        }
        preEdt.commit();
    }

    public static String getCityName(Context ctx, int widgetId) {
        if (pref == null) {
            pref = ctx.getSharedPreferences(PREF_CLOCK_WIDGET_INFO, 0);
        }
        return pref.getString(STYLEC_CITY_NAME + widgetId, "");
    }

    /**
     * Delete the correlative data from SharedPreferences
     *
     * @param widgetIds
     */
    public static void deletePreferences(int[] widgetIds) {
        if (pref == null) {
            return;
        }
        try {
            SharedPreferences.Editor preEdt = pref.edit();
            for (int widgetId : widgetIds) {
                Xlog.d(TAG, " Delete widgetId:" + widgetId + " preferences ");
                String key = String.valueOf(widgetId);
                preEdt.remove(STYLEC_CITY_INDEX + key);
                preEdt.remove(STYLEC_CITY_NAME + key);
                preEdt.remove(STYLEC_CITY_ZONE + key);
                preEdt.remove(STYLEC_WEATHER_ID + key);
            }
            preEdt.commit();
        } catch (Exception e) {
            Xlog.w(TAG,
                    "remove data from SharedPreferences:pref_clock_widget_info.xml fail!");
        }
    }

    /**
     * initial time zone
     */
    protected static void initTimeZone() {
        if (null == mTimeZoneMap || mTimeZoneMap.size() <= 0) {
            Xlog.d(TAG, " Initial timezones data from timezones.xml ");
            if (mTimeZoneMap == null) {
                mTimeZoneMap = new HashMap<String, ClockCityInfo>();
                Xlog.d(TAG, "inittimezone new hashmap");
            }
            long l = SystemClock.uptimeMillis();
            reLoadTimeZone(context);
            Xlog.d("myTag", "time - l = " + (SystemClock.uptimeMillis() - l));
        }
    }

    /**
     * load file:xml/timezones.xml
     *
     * @param context
     *            ctx
     */
    protected static void reLoadTimeZone(Context context) {
        if (mTimeZoneMap == null) {
            mTimeZoneMap = new HashMap<String, ClockCityInfo>();
        }
        synchronized (mTimeZoneMap) {
            if (mTimeZoneMap.size() > 0) {
                mTimeZoneMap.clear();
            }
            XmlResourceParser xrp = null;
            try {
                xrp = context.getResources().getXml(R.xml.timezones);
                // skip comment until START_TAG
                while (xrp.next() != XmlResourceParser.START_TAG) {
                    continue;
                }
                int eventType = xrp.getEventType();
                int index = 0;
                while (eventType != XmlResourceParser.END_DOCUMENT) {
                    if (eventType == XmlResourceParser.START_TAG) {
                        // get tag of time zone
                        if (xrp.getName().equals(XML_TAG_TIME_ZONE)) {
                            String weatherID = xrp.getAttributeValue(
                                    NAME_SPACE, WEATHER_ID);
                            String gmtZone = xrp.getAttributeValue(NAME_SPACE,
                                    GMT_ZONE);
                            String cityName = xrp.nextText();
                            String key = String.valueOf(index);
                            ClockCityInfo cityInfor = new ClockCityInfo(key,
                                    weatherID, gmtZone, cityName);
                            mTimeZoneMap.put(weatherID, cityInfor);
                            // mTimeZoneMap.put(key, cityInfor);
                            index++;
                        }
                    }
                    eventType = xrp.next();
                }
                xrp.close();
            } catch (XmlPullParserException xppe) {
                Xlog.w(TAG,
                        "Ill-formatted timezones.xml file:" + xppe.getMessage());
            } catch (java.io.IOException ioe) {
                Xlog.w(TAG, "Unable to read timezones.xml file");
            } finally {
                if (null != xrp) {
                    xrp.close();
                }
            }
        }
    }

    public static ArrayList<Integer> getIntDelete(String weatherNameDelete) {
        // TODO Auto-generated method stub
        if (pref != null) {
            ArrayList<Integer> al = new ArrayList<Integer>();
            HashMap<String, String> ha = (HashMap<String, String>) pref
                    .getAll();
            Set<String> set = ha.keySet();
            Iterator<String> iterator = set.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = ha.get(key);
                if (weatherNameDelete.equals(value)) {
                    key = key.substring(17);
                    al.add(Integer.valueOf(key));
                }
            }
            return al;
        }
        return null;
    }

}
