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

public class ClockCityInfo {

	// the follow constants reference toStringArray()
	public static final int INDEX = 0;

	public static final int WEATHER_ID = 1;

	public static final int TIME_ZONE = 2;

	public static final int CITY_NAME = 3;

	// initial index value not exist in HashMap(timezones.xml)
	private String index = "-1";

	private String weatherID = "";

	private String timeZone;

	private String cityName;

	/**
	 * an empty construct fun
	 */
	public ClockCityInfo() {
	}

	/**
	 * @param index
	 * @param weatherID
	 * @param timeZone
	 * @param cityName
	 */
	public ClockCityInfo(String index, String weatherID, String timeZone,
			String cityName) {
		super();
		this.index = index;
		this.weatherID = weatherID;
		this.timeZone = timeZone;
		this.cityName = cityName;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getWeatherID() {
		return weatherID;
	}

	public void setWeatherID(String weatherID) {
		this.weatherID = weatherID;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	/**
	 * convert ClockCityInfor object to a string array
	 * 
	 * @return String Array
	 */
	public String[] toStringArray() {
		String[] cityArray = new String[4];
		cityArray[INDEX] = this.index;
		cityArray[WEATHER_ID] = this.weatherID;
		cityArray[TIME_ZONE] = this.timeZone;
		cityArray[CITY_NAME] = this.cityName;
		return cityArray;
	}

	@Override
	public boolean equals(Object o) {
		boolean ret = false;
		if (o instanceof ClockCityInfo) {
			String cityName = ((ClockCityInfo) o).getCityName();
			if (null != this.cityName && null != cityName) {
				ret = this.cityName.equals(cityName);
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		return "index:" + index + ",weatherID:" + weatherID + ",timeZone:"
				+ timeZone + ",cityName:" + cityName;
	}

}
