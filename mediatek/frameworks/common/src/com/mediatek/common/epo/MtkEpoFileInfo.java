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

package com.mediatek.common.epo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.GregorianCalendar;

public final class MtkEpoFileInfo implements Parcelable {
    
    public long downloadTime;//seconds
    public long startTime;   //seconds
    public long expireTime;  //seconds
    
    
    public MtkEpoFileInfo() {}
    public MtkEpoFileInfo(long downloadTime, long startTime, long expireTime) {
        this.downloadTime = downloadTime;
        this.startTime = startTime;
        this.expireTime = expireTime;
    }
    
    public static final Parcelable.Creator<MtkEpoFileInfo> CREATOR = new Parcelable.Creator<MtkEpoFileInfo>() {
        public MtkEpoFileInfo createFromParcel(Parcel in) {
                MtkEpoFileInfo fileInfo = new MtkEpoFileInfo();
                fileInfo.readFromParcel(in);
                return fileInfo;
        }
        public MtkEpoFileInfo[] newArray(int size) {
                return new MtkEpoFileInfo[size];
        }
    };

    //@Override
    public int describeContents() {
        return 0;
    }

    //@Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(downloadTime);
        out.writeLong(startTime);
        out.writeLong(expireTime);
    }

    //@Override
    public void readFromParcel(Parcel in) {
        downloadTime    =  in.readLong();
        startTime       =  in.readLong();
        expireTime      =  in.readLong();
    }

    public String getDownloadTimeString() {
        return timeInMillis2Date(downloadTime * 1000);
    }
    public String getStartTimeString() {
        return timeInMillis2Date(startTime * 1000);
    }
    public String getExpireTimeString() {
        return timeInMillis2Date(expireTime * 1000);
    }

    private String timeInMillis2Date(long time) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        String date = String.format("%04d-%02d-%02d %02d:%02d:%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY) + 1,
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND));
        return date;
    }
    
    public String toString() {
        String str = new String();
        str = " MtkEpoFileInfo downloadTime=" + downloadTime + " startTime=" + startTime + " expireTime=" + expireTime;
        return str;
    }
}

