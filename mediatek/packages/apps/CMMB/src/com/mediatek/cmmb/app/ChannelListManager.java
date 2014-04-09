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

package com.mediatek.cmmb.app;

//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.app.ProgressDialog;
//import android.app.Service;
//import android.app.TabActivity;
//import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.net.Uri;
//import android.os.Binder;
//import android.os.Bundle;
import android.os.Handler;
//import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
//import android.text.format.DateFormat;
//import android.text.format.Time;
import android.util.Log;
/*
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabWidget;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
*/

//MBBMS Provider's interface
//import com.mediatek.mbbms.providers.MBBMSProvider;
import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.MBBMSStore.*;
import com.mediatek.mbbms.MBBMSStore.ESG.*;
import com.mediatek.mbbms.MBBMSStore.SG.*;
//MBBMS Service's interface
import com.mediatek.mbbms.service.CMMBServiceClient;

//import java.net.HttpURLConnection;
import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;


public class ChannelListManager {
  
    private static final String TAG = "CMMB::ChannelListManager";

    private static final int MSG_UPDATE_PROGRAM = 1;
    private static final int MSG_LOAD_CHANNEL_DONE = 2;
    
    
    private static final int MBBMS_MODE = CMMBServiceClient.CMMB_STATE_CONNECTED;
    private static final String CHN_LANG_CODE = "zho" + MBBMSStore.SEPARATOR_INNER;

    private static final String[] ESG_PROGRAM_PROJECTION = {ESG.ContentDetail.TITLE,ESG.ContentDetail.START_TIME
        ,ESG.ContentDetail.DURATION,ESG.ServiceDetail.FREQUENCY_NO,ESG.ServiceDetail.SERVICE_ID};
    private static final String[] SG_PROGRAM_PROJECTION = {SG.ContentDetail.NAME
        ,SG.ContentDetail.START_TIME,SG.ContentDetail.END_TIME
        ,SG.ServiceDetail.SERVICE_ID};
    

    private static ChannelListManager sChannelListManager;
    
    private ArrayList<ChannelListObserver> mChannelListObserverList;    
    
    private static boolean sIsMbbmsData;
    private static boolean sIsMbbmsUpdate;    
    private ArrayList<Channel> mChannelList;
    private ArrayList<Program> mProgramList;    
    private ArrayList<Integer> mFavoriteList;
    private boolean mAllowUpdate = true;
    private boolean mUpdating;
    private boolean mNeedUpdate;
    private boolean mOnUpdateServiceOk;
    
    private ContentObserver mContentObserver;    
    private Context mAppContext;    
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage what = " + msg.what);
            switch (msg.what) {
                case MSG_UPDATE_PROGRAM:                     
                    loadProgramList();
                    callChannelListObserver(false);
                    break;                 

                case MSG_LOAD_CHANNEL_DONE: 
                    loadChannelDone(msg);
                    break; 
                    
                default :
                    break;
            }
        }
    };    
    
    public static class Program {
        public String program;
        public long programStartTime;             
        public long programEndTime;
    }
 
    public static class Channel {
        public int rowID;
        public String serviceID;
        public boolean isFavorite;
        public byte type;
        public String name;
        public boolean forFree;
        public boolean isSubscribed;        
        public Bitmap logo;        
        public int freq;
        public boolean isEncrypted;
        public String sdp;
        public int cmmbServiceID;
        public int lock_stat; //lock status on UI, 0 - none, 1 -- lock, 2 -- unlock
        static final int LOCK_STATE_NONE = 0;
        static final int LOCK_STATE_LOCK = 1;
        static final int LOCK_STATE_UNLOCK = 2;
        //should combine the mode and the channel info together to avoid mode/data inconsistency.
        public boolean isAudio() {    
            if (!sIsMbbmsData) {
                return type == ESG.Service.SERVICE_CLASS_ENUM_AUDIO_BROADCAST;
            } else {
                return type == SG.Service.SERVICE_TYPE_ENUM_BASIC_RADIO;
            }
        }

        public boolean isTv() { 
            if (!sIsMbbmsData) {
                return type == ESG.Service.SERVICE_CLASS_ENUM_TV;
            } else {
                return type == SG.Service.SERVICE_TYPE_ENUM_BASIC_TV;
            }
        }
        


        public boolean isFree() {    
            return forFree;
        }
        
        public boolean isFavorite() {
            return isFavorite;
        }
        public boolean isSubscribed() { 
            return isSubscribed;
        }        
        
        public boolean isEncrypted() {
            return isEncrypted;
        }
        
        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return new String("name:" + name
                    + " , serviceID:" + serviceID
                    + " , cmmbServiceID:" + cmmbServiceID
                    + " , freq:" + freq
                    + " , sdp:" + sdp
                    + " , isFavorite:" + isFavorite
                    + " , type:" + type
                    + " , forFree:" + forFree
                    + " , isSubscribed:" + isSubscribed
                    + " , isEncrypted:" + isEncrypted
                    + " , lock_stat:" + lock_stat
                    );
        }
    }    

    private ChannelListManager(Context c) {
        mChannelList = new ArrayList<Channel>();
        mProgramList = new ArrayList<Program>();    
        mFavoriteList = new ArrayList<Integer>();        
        mChannelListObserverList = new ArrayList<ChannelListObserver>();    
        mAppContext = c.getApplicationContext();
        registerContentObserver();            
    }
    
    public static ChannelListManager getChannelListManager(Context c) {
        if (sChannelListManager == null) {
            sChannelListManager = new ChannelListManager(c);        
        }
        return sChannelListManager;
    }

    public void setAllowUpdate(boolean allow) {
        mAllowUpdate = allow;
    }

    public void registerContentObserver() {   
        if (mContentObserver == null) {        
            mContentObserver = new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {                
                    Log.d(TAG, "onChange mAllowUpdate = " + mAllowUpdate);
                    if (mAllowUpdate) {
                        loadChannelList(false,sIsMbbmsData);
                    }
                }

            };
            mAppContext.getContentResolver().registerContentObserver(ESG.ServiceDetail.CONTENT_URI, 
                    true, mContentObserver);        
            mAppContext.getContentResolver().registerContentObserver(SG.ServiceDetail.CONTENT_URI, 
                    true, mContentObserver);   
        }
    }
    
    public void unregisterContentObserver() {  
        if (mContentObserver != null) {
            mAllowUpdate = false;//at this point content may change and onChange may be called soon,set this flag to stop it.
            //TBD:confirm if it can unregister mContentObserver which is registered twice.
            mAppContext.getContentResolver().unregisterContentObserver(mContentObserver);
            mContentObserver = null;
        }           
    }    

    private void validateChannels() {
        for (Channel ch : mChannelList) {
            if (!ch.isFree() && !ch.isEncrypted()) {
                Log.e(TAG, "Channel: " + ch + " is Inavlid!!!");
                //reset SG data
                ContentResolver cr = mAppContext.getContentResolver();
                
                ContentValues cv = new ContentValues();
                cv.put(SG.SGDD.SGDD_ID, 0);
                cv.put(SG.SGDD.VERSIOIN, 0);
                cr.update(SG.SGDD.CONTENT_URI, cv, null, null);
            }
        }
    }
    
    private void loadChannelDone(Message msg) {
        mUpdating = false;
        if (mNeedUpdate) {
            loadChannelList(mOnUpdateServiceOk,sIsMbbmsUpdate);
            return;
        }

        mFavoriteList.clear();
        sIsMbbmsData = (msg.arg1 == 1);
        if (msg.obj != null) {
            mChannelList = (ArrayList<Channel>)msg.obj;                     
            int size = mChannelList.size();
            
            if (mProgramList.size() != size) {
                resetProgramList();
            }
        
            if (mOnUpdateServiceOk) {                            
                loadProgramList();
            }                        
            
            for (int i = 0;i < size;i++) {
                Channel ch = mChannelList.get(i);
                if (ch.isFavorite) {
                    mFavoriteList.add(i);
                }
            }                        
        } else {            
            mChannelList = new ArrayList<Channel>();
        }
        
        /*
         * For A366t project, there's invalid channel item that we cannot find the root cause .
         * Here, we add some fault tolerance code to enhance its robustness.
         */
        Log.d(TAG, "loadChannelDone mOnUpdateServiceOk:" + mOnUpdateServiceOk + " sIsMbbmsUpdate:" + sIsMbbmsUpdate);
        if (mOnUpdateServiceOk && sIsMbbmsData) {
            validateChannels();
        }
        
        callChannelListObserver(mOnUpdateServiceOk);
        mOnUpdateServiceOk = false;
    }

    //After release is called,one must call getChannelListManager again to get a valid ChannelListManager.
    public void release() {
        Log.d(TAG, "release");
        
        if (sChannelListManager != null && mChannelListObserverList.size() <= 0) {
            //no one cares about me,just suicide silently...
            Log.d(TAG, "releasing");
            mHandler.removeCallbacksAndMessages(null);        
            unregisterContentObserver();    
            sChannelListManager = null;
        }
    }

    public interface ChannelListObserver {
        void onChannelListChange(boolean onUpdateServiceOk);       
    }            

    public void registerChannelListObserver(ChannelListObserver l) {    
        mChannelListObserverList.add(l);
    }

    public void unregisterChannelListObserver(ChannelListObserver l) {              
        mChannelListObserverList.remove(l);
    }

    
    private void callChannelListObserver(boolean onUpdateServiceOk) {
        int size = mChannelListObserverList.size();
        for (int i = 0;i < size;i++) {
            mChannelListObserverList.get(i).onChannelListChange(onUpdateServiceOk);
        }
    }

    public ArrayList<Channel> getChannelList() {
        return mChannelList;
    }

    public ArrayList<Program> getProgramList() {
        return mProgramList;
    }
    
    public ArrayList<Integer> getFavoriteList() {
        return mFavoriteList;
    }    

    public void loadChannelList(boolean onUpdateServiceOk, final boolean isMbbmsMode) {
        loadChannelList(onUpdateServiceOk, isMbbmsMode, false);        
    }    

    public void loadChannelList(boolean onUpdateServiceOk, final boolean isMbbmsMode,boolean sync) {
        Log.d(TAG, "loadChannelList() onUpdateServiceOk = " + onUpdateServiceOk);
        if (!mAllowUpdate) {
            Log.d(TAG, "loadChannelList() mAllowUpdate = false");            
            return;
        }
        
        if (onUpdateServiceOk) {
            mOnUpdateServiceOk = true;
        }
        
        if (mUpdating) {
            mNeedUpdate = true;
            sIsMbbmsUpdate = isMbbmsMode;            
            return;
        }     

        mUpdating = true;
        mNeedUpdate = false;

        if (sync) {
            loadChannel(isMbbmsMode,true);
        } else {
            Thread worker;
            worker = new Thread(new Runnable() {
                public void run() {
                    loadChannel(isMbbmsMode,false);
                }
            });
            worker.start();    
        }
    }
    
    private void loadChannel(boolean isMbbmsMode,boolean sync) {
        Log.d(TAG, "enter loadChannel() isMbbmsMode = " + isMbbmsMode);
        Cursor cursor;
        ContentResolver cr = mAppContext.getContentResolver();
        if (!isMbbmsMode) {
            cursor = cr.query(ESG.ServiceDetail.CONTENT_URI
                ,new String []{ESG.ServiceDetail.SERVICE_ID,ESG.ServiceDetail.IS_FAVORITE,
                    ESG.ServiceDetail.SERVICE_NAME,ESG.ServiceDetail.SERVICE_CLASS,
                    ESG.ServiceDetail.FOR_FREE,ESG.ServiceDetail.ID,
                    ESG.ServiceDetail.FREQUENCY_NO}
                ,ESG.ServiceDetail.SERVICE_CLASS + " = " + ESG.Service.SERVICE_CLASS_ENUM_TV
                ,null
                ,ESG.ServiceDetail.ID + " ASC");            
        } else {
            Uri uri = SG.ServiceDetailMore.CONTENT_URI
                    .buildUpon()
                    .appendQueryParameter(MBBMSStore.QUERY_PARAM_GROUP_BY, SG.ServiceDetail.SERVICE_ID)
                    .build();
            cursor = cr.query(uri
                ,new String []{SG.ServiceDetail.SERVICE_ID,SG.ServiceDetail.IS_FAVORITE,
                    SG.ServiceDetail.NAME,SG.ServiceDetail.SERVICE_TYPE,
                    SG.ServiceDetail.FOR_FREE,SG.ServiceDetail.ID,
                    SG.ServiceDetail.FREQUENCY_NO,SG.ServiceDetail.IS_SUBSCRIBED,
                    SG.ServiceDetail.SDP,SG.ServiceDetail.CMMB_SERVICE_ID,
                    SG.ServiceDetail.ISENCRYPTED}
                ,SG.ServiceDetail.SERVICE_TYPE + " = " + SG.Service.SERVICE_TYPE_ENUM_BASIC_TV
                ,null
                ,SG.ServiceDetail.WEIGHT + " ASC");     
        }

        if (cursor == null || cursor.getCount() <= 0) {        
            if (cursor != null) {
                cursor.close(); 
            }

            Message msg = mHandler.obtainMessage(MSG_LOAD_CHANNEL_DONE,isMbbmsMode ? 1 : 0,0,null);
            if (sync) {
                loadChannelDone(msg);
            } else {
                mHandler.sendMessage(msg);    
            } 
            Log.d(TAG, "loadChannel() count = 0");
            return;
        }

        
        Log.d(TAG, "loadChannel() count = " + cursor.getCount());

        ArrayList<Channel> cl = new ArrayList<Channel>();
        
        cursor.moveToFirst();                        
        do {
            final Channel item = new Channel();


            item.isFavorite = cursor.getInt(1) == 1 ? true : false;
            item.name = removePrefixCode(cursor.getString(2));          
            item.type = (byte)cursor.getInt(3);
            item.forFree = cursor.getInt(4) == 1 ? true : false;
            item.rowID = cursor.getInt(5);          
            item.freq = cursor.getInt(6);

            //below are mode specific data.
            if (isMbbmsMode) {
                item.serviceID = cursor.getString(0);
                item.isSubscribed = (cursor.getInt(7) == 1 ? true : false);
                item.sdp = cursor.getString(8);                
                item.cmmbServiceID = cursor.getInt(9);                
                item.isEncrypted = cursor.getInt(10) == 1 ? true : false;
                item.logo = SG.Service.getPreviewBitmap(cr,item.rowID,SG.PREVIEW_USAGE_ENUM_SG_BROWSING);
                if (item.isEncrypted) {
                    if (item.forFree || item.isSubscribed) {
                        item.lock_stat = Channel.LOCK_STATE_UNLOCK;
                    } else {
                        item.lock_stat = Channel.LOCK_STATE_LOCK;
                    }
                } else {
                    item.lock_stat = Channel.LOCK_STATE_NONE;
                }
            } else {
                item.serviceID = String.valueOf(cursor.getInt(0));
                item.isSubscribed = false;
                item.logo = ESG.ServiceMedia.getMediaBitmap(cr,item.rowID,ESG.MEDIA_USAGE_ENUM_ICON);
                item.isEncrypted = false;
                item.lock_stat = item.forFree ? Channel.LOCK_STATE_NONE : Channel.LOCK_STATE_LOCK;
            }

            cl.add(item); 
            Log.d(TAG, "loadChannel() item = " + item);
            
        } while (cursor.moveToNext());
        
        cursor.close(); 

        Log.d(TAG, "loadChannel() end");
        Message msg = mHandler.obtainMessage(MSG_LOAD_CHANNEL_DONE,isMbbmsMode ? 1 : 0,0,cl);
        if (sync) {
            loadChannelDone(msg);
        } else {
            mHandler.sendMessage(msg);    
        } 
    }    

    private String removePrefixCode(String s) {
        int pos = s.indexOf(CHN_LANG_CODE);
        if (pos >= 0) {
          s = s.substring(pos + CHN_LANG_CODE.length());  
          pos = s.indexOf(MBBMSStore.SEPARATOR_OUTER);
          if (pos >= 0) {
              s = s.substring(0,pos);
          }
        } 
        return s;
    }
    
    private long getCurrentTime() {
        long now = System.currentTimeMillis();
        
        Log.d(TAG, "getCurrentTime() system now = " + now);
        return now;
    }
    
    //0 <= pos <= resultCount-1
    //cursor.moveToFirst() must be called before.
    private int findFirstMatchFreqServiceId(boolean isMbbmsMode,Cursor c,int pos,int resultCount,int freq,int sid) {
        //Log.d(TAG, "findFirstMatchFreqServiceId():freq = "+freq+" sid="+sid+" pos ="+pos);        
    
        int currpos = pos;
        int currfreq;
        int currsid;
        if (isMbbmsMode) {
            do {
                currsid = Integer.parseInt(c.getString(3));
                //Log.d(TAG, "findFirstMatchFreqServiceId():currsid = "+currsid);     
                
                if (currsid == sid) {
                    return currpos;
                }
                currpos++;
                if (currpos == resultCount) {
                    currpos = 0;
                }
                c.moveToPosition(currpos);
            } while (currpos != pos);
        } else {
            do {
                currfreq = c.getInt(3);
                //Log.d(TAG, "findFirstMatchFreqServiceId():currfreq = " + currfreq);        
                
                if (currfreq == freq) {
                    currsid = c.getInt(4);
                    
                    Log.d(TAG, "findFirstMatchFreqServiceId():curr_sid = " + currsid);     
                    if (currsid == sid) {
                        return currpos;
                    }
                }    

                currpos++;
                if (currpos == resultCount) {
                    currpos = 0;
                }
                c.moveToPosition(currpos);                
            } while (currpos != pos);
        }
        Log.w(TAG, "findFirstMatchFreqServiceId():no match candidate found!");        
        return -1;
    }
    
    private int fillProgram(boolean isMbbmsMode,Cursor c,int pos,int resultCount,int currIndex,long now,int freq,int sid) {
        Log.d(TAG, "fillProgram():pos = " + pos + " currIndex=" + currIndex + " now=" + now);
        
        int firstMatch = findFirstMatchFreqServiceId(isMbbmsMode, c,pos,resultCount,freq,sid);
        if (firstMatch == -1) {
            Log.d(TAG, "fillProgram():firstMatch = -1");
            return -1;
        } else {
            //found
            Log.d(TAG, "fillProgram():firstMatch = " + firstMatch);
            int currpos = firstMatch;
            long start;
            long end;
            if (isMbbmsMode) {
                do {
                    start = c.getLong(1);
                    end = c.getLong(2);
                    //Log.w(TAG, "fillProgram():start = "+start+" end="+end);                    
                    if (end >= now) { //best fit(start >= now and end >= now) or (end >= now)
                        Program pr = mProgramList.get(currIndex);
                        pr.programStartTime = start;
                        pr.programEndTime = end;
                        pr.program = removePrefixCode(c.getString(0)); 
                        return currpos;
                    }                
                    currpos++;
                    if (currpos == resultCount) {
                        currpos = 0;
                    }
                    c.moveToPosition(currpos);                        
                } while (currpos != pos);
            } else {
                do {
                    start = c.getLong(1);
                    end = start + c.getLong(2);
                    //Log.w(TAG, "fillProgram():start = "+start+" end="+end);
                    
                    if (end >= now) { //best fit(start >= now and end >= now) or (end >= now)
                        Program pr = mProgramList.get(currIndex);
                        pr.programStartTime = start;
                        pr.programEndTime = end;
                        pr.program = removePrefixCode(c.getString(0)); 
                        return currpos;
                    }                
                    currpos++;
                    if (currpos == resultCount) {
                        currpos = 0;
                    }
                    c.moveToPosition(currpos); 
                } while (currpos != pos);
            }
            Log.e(TAG, "fillProgram():should not reach here if firstMatch == -1 !!!");
            return -1;
        }
    }

    public void loadProgramList() {
        Cursor cursor;        
        ContentResolver cr = mAppContext.getContentResolver();
        long now = getCurrentTime();
        String nowstring = String.valueOf(now);
        String halfHourAfterNow = String.valueOf(now + 1800 * 1000);
        long minEndTime = Long.MAX_VALUE;
        
        //boolean isMbbmsMode = getMode() == MBBMS_MODE;

        mHandler.removeMessages(MSG_UPDATE_PROGRAM);
        
        if (!sIsMbbmsData) {
            cursor = cr.query(ESG.ContentDetail.CONTENT_URI
                ,ESG_PROGRAM_PROJECTION
                ,ESG.ContentDetail.START_TIME + " + " + ESG.ContentDetail.DURATION + " >= " + nowstring + " AND "
                    + ESG.ContentDetail.START_TIME + " <= " + halfHourAfterNow /*allow 30 minutes forecast*/
                ,null
                ,ESG.ContentDetail.FREQUENCY_NO + "," + ESG.ContentDetail.SERVICE_ID + "," 
                    + ESG.ContentDetail.START_TIME);

        } else {
            cursor = cr.query(SG.ContentDetail.CONTENT_URI
                ,SG_PROGRAM_PROJECTION
                ,SG.ContentDetail.END_TIME + " >= " + nowstring + " AND "
                    + SG.ContentDetail.START_TIME + " <= " + halfHourAfterNow                  
                ,null
                ,SG.ServiceDetail.SERVICE_ID + "," 
                    + SG.ContentDetail.START_TIME);

        }

        boolean noMatchFound = true;

        if (cursor != null) {
            int resultCount = cursor.getCount();
            Log.d(TAG, "loadProgramList() resultCount = " + resultCount); 
            if (cursor.moveToFirst()) {
                int pos = 0;    
                int size = mProgramList.size();
                noMatchFound = false;//must be at least one match program.

                //ArrayList<Channel> ch_list = (cl == null) ? mChannelList : cl;
                for (int i = 0;i < size;i++) {
                Channel ch = mChannelList.get(i);                    
                int match = fillProgram(sIsMbbmsData,cursor,pos,resultCount,i,now,ch.freq,Integer.parseInt(ch.serviceID));
                if (match == -1) {
                    noMatchFound = true;
                } else {
                    //if the order of result set is same with mChannelList,by doing so we can speed up the matching process.
                    pos = match;
                    Program pr = mProgramList.get(i);
                        if (pr.programEndTime > 0 && pr.programEndTime < minEndTime) {
                                minEndTime = pr.programEndTime;
                        }                    
                }
                }
            }

            cursor.close();             
            
        } else {
            Log.e(TAG, "loadProgramList() cursor == null !!!"); 
        }        

        long delay = getCurrentTime();
        Log.d(TAG, "now = " + delay);                
        delay = minEndTime - delay;
        //at this time,if noMatchFound = true,it means
        //1.no match found at all(cursor = null(something wrong) or resultCount = 0)
        //2.at least one no match found,because we don't know 
        //if it is filtered by 1800*1000 factor,we set delay = 1800*1000 here.
        if (noMatchFound && delay > 1800 * 1000) {
            delay = 1800 * 1000; 
        }
        Log.d(TAG, "delay = " + delay);         
        //delay may be minus here,however sendEmptyMessageDelayed will take care of that.
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRAM,delay);
                
    }    
    
    public void resetProgramList() {
        mHandler.removeMessages(MSG_UPDATE_PROGRAM);
        mProgramList.clear();
        int size = mChannelList.size();
        for (int i = 0; i < size; i++) {
            mProgramList.add(new Program());
        }
    }

    //we may miss some revert favorite operation if user quickly click on favorite button 
    //because the loadChannelList is done asynchrous.
    //anyway we don't care about such case.
    public int revertFavoriteAttr(int pos) {
        if (pos < 0 || pos >= mChannelList.size()) {
            Log.e(TAG, "revertFavoriteAttr wrong pos = " + pos + " while mChannelList.size = " + mChannelList.size());      
            return -1;
        }
        Channel ch = mChannelList.get(pos);
        ch.isFavorite = !ch.isFavorite;
        ContentResolver cr = mAppContext.getContentResolver();        
            
        ContentValues cv = new ContentValues();
        
        if (!sIsMbbmsData) {
            cv.put(ESG.ServiceDetail.IS_FAVORITE, ch.isFavorite);
            cr.update(ESG.ServiceDetail.CONTENT_URI,cv
                    , ESG.ServiceDetail.SERVICE_ID + "=? " + " AND "
                    + ESG.ServiceDetail.FREQUENCY_NO + "=? ", new String[]{ch.serviceID,String.valueOf(ch.freq)});
        } else {
            cv.put(SG.ServiceDetail.IS_FAVORITE, ch.isFavorite);
            cr.update(SG.ServiceDetail.CONTENT_URI, cv
                    , SG.ServiceDetail.SERVICE_ID + "=? ", new String[]{ch.serviceID});            
        }        
        
        return ch.isFavorite ? 1 : 0;
    }     
    
}
