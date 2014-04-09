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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class MtkEpoXmlLoader {
    private boolean mEpoEnable = false;
    private boolean mAutoEnable = false;
    private int mUpdatePeriod = 4320; // The unit is minute, 3days = 60*24*3
    
    public boolean getEpoEnable() {
        return mEpoEnable;
    }
    
    public boolean getAutoEnable() {
        return mAutoEnable;
    }
    
    public int getUpdatePeriod() {
        return mUpdatePeriod;
    }
    
    public void dumpFile(String path) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            log("==== dumpFile path=" + path + " ====");
            String line = null;
            while ((line=reader.readLine()) != null) {
                log("dumpFile=" + line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    

    public void updateEpoProfile(String path) {
        XmlPullParser parser = null;
        InputStream is = null;
        try {
            int eventType;
            String name;
            String attrName;
            String attrValue;

            parser = Xml.newPullParser();
            is = new FileInputStream(path);
            
            parser.setInput(is, "utf-8");
            do {
                parser.next();
                eventType = parser.getEventType();
                name = parser.getName();
                int count = parser.getAttributeCount();
                //log("====== eventType=" + eventType + " name=" + name + " count=" + count + " =========");
                if(eventType != XmlPullParser.START_TAG)
                    continue;
                if(name.equals("epo_conf_para")) {
                    for(int i = 0; i < count; i ++) {
                        attrName = parser.getAttributeName(i);
                        attrValue = parser.getAttributeValue(i);
                        
                        if(attrName.equals("epo_enable")) {
                            mEpoEnable = attrValue.equals("yes")?true:false;
                        } else if(attrName.equals("auto_enable")) {
                            mAutoEnable = attrValue.equals("yes")?true:false;
                        } else if(attrName.equals("update_period")) {
                            mUpdatePeriod = Integer.valueOf(attrValue);
                            if(mUpdatePeriod < 1440)
                                mUpdatePeriod = 1440;
                        }
                    }
                }
                
            } while(eventType != XmlPullParser.END_DOCUMENT);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            dumpFile(path);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            dumpFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            dumpFile(path);
        } catch (Exception e) {
            e.printStackTrace();
            dumpFile(path);
        } finally {
            try {
                if(is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }//end of public void updateAgpsProfile(String path)
    
    public String toString() {
        String tmp = new String();
        tmp += " EpoXmlLoader epoEnable=" + mEpoEnable + " autoEnable=" + mAutoEnable + " updatePeriod=" + mUpdatePeriod;
        return tmp;
    }
    
    private void log(String msg) {
        Log.d("[MtkEpoClientManagerService]", msg);
    }
}

