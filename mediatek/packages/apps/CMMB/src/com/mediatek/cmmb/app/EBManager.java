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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mediatek.mbbms.MBBMSStore.EB;
import com.mediatek.mbbms.service.CMMBServiceClient.CMMBEmergencyMessage;
import com.mediatek.mbbms.service.CMMBServiceClient.onClientEBMListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.PriorityQueue;
import java.util.TimeZone;

/**
 * Note: should in the same process with app and service. If service is moved to
 * other process, this class should be modified.
 */
public class EBManager implements onClientEBMListener {
    private static final String TAG = "EBManager";
    private static final boolean LOG = true;

    private static final int MAX_CAPABILITY = 50;
    private final PriorityQueue<Profile> mCacheQueue = new PriorityQueue<Profile>(MAX_CAPABILITY, Profile.getComparator());
    private Context mContext;
    private NotificationManager mNotificationManager;
    private ContentResolver mContentResolver;
    private boolean mLoad;

    private static final String[] EB_PROJECTION = new String[] { EB.Broadcast.ID, EB.Broadcast.NET_LEVEL,
            EB.Broadcast.NET_ID, EB.Broadcast.MSG_ID, EB.Broadcast.RECEIVE_TIME };
    private static final int COL_ROW_ID = 0;
    private static final int COL_NET_LEVEL = 1;
    private static final int COL_NET_ID = 2;
    private static final int COL_MSG_ID = 3;
    private static final int COL_ADD_TIME = 4;

    private int[] mICONS = { R.drawable.ebm_level1, R.drawable.ebm_level2, R.drawable.ebm_level3, R.drawable.ebm_level4 };

    private EBManager(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void loadProfiles() {
        if (mLoad) {
            return;
        }
        Cursor cursor = mContentResolver.query(EB.Broadcast.CONTENT_URI, EB_PROJECTION, null, null, null);
        if (cursor != null) {
            // here don't check max count for we control the insert.
            while (cursor.moveToNext()) {
                Profile profile = new Profile();
                profile.id = cursor.getLong(COL_ROW_ID);
                profile.netLevel = cursor.getInt(COL_NET_LEVEL);
                profile.netId = cursor.getInt(COL_NET_ID);
                profile.msgId = cursor.getInt(COL_MSG_ID);
                profile.priority = cursor.getLong(COL_ADD_TIME);
                mCacheQueue.add(profile);
            }
            if (LOG) {
                Log.v(TAG, "loadProfiles() mCacheQueue.size()=" + mCacheQueue.size());
            }
            cursor.close();
        }
        mLoad = true;
    }

    private boolean isFormal(int level) {
        return (level >= 1 && level <= 4);
    }

    // Random rnd = new Random();
    // private void testMsg(CMMBEmergencyMessage msg) {
    // msg.msg_id = rnd.nextInt(60);
    // msg.msg_level = rnd.nextInt(4)+ 1;
    // msg.msg_content = "" + msg.msg_level + Profile.copy(msg);
    // }

    public synchronized void onEB(CMMBEmergencyMessage msg) {
        // testMsg(msg);
        if (msg == null || !isFormal(msg.msg_level)) {
            if (LOG) {
                Log.v(TAG, "EBMCallback() not formal " + msg);
            }
            return;
        }
        loadProfiles();
        Profile profile = Profile.copy(msg);
        if (!mCacheQueue.contains(profile)) {
            if (mCacheQueue.size() < MAX_CAPABILITY) {
                add(msg, profile);
            } else {
                Profile old = mCacheQueue.peek();// peek it
                if (deleteDB(mContentResolver, old.id)) {
                    mNotificationManager.cancel((int) old.id);
                    mCacheQueue.poll();// drop it
                    add(msg, profile);
                }
            }// end < MAX_CAPABILITY
        }// end contains
        if (LOG) {
            Log.v(TAG, "EBMCallback() size=" + mCacheQueue.size() + ", " + profile);
        }
    }

    private void checkNotify(CMMBEmergencyMessage msg, Profile profile, Uri data) {
        int formalLevel = msg.msg_level;
        Notification notification = new Notification();
        notification.defaults |= Notification.DEFAULT_ALL;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.when = profile.getTime();
        String title = String.format(mContext.getResources().getString(R.string.ebm_level), formalLevel);
        notification.icon = mICONS[formalLevel - 1];
        notification.tickerText = msg.msg_content;
        notification.number = 0;
        Intent notifyIntent = new Intent(mContext, MessageDetailActivity.class);
        notifyIntent.setData(data);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notifyIntent, 0);
        notification.setLatestEventInfo(mContext, title, msg.msg_content, pendingIntent);
        mNotificationManager.notify((int) profile.id, notification);// need
        // check

        if (ModeSwitchManager.isInForground()) {
            if (formalLevel < 3) {
                // 1,2 will show detail, 3,4 just notify
                goToMessageDetails(data);
            } else {
                // notify other listener
                if (sListeners != null) {
                    for (EBListener listener : sListeners) {
                        if (listener != null) {
                            listener.notify(formalLevel, msg.msg_content, data);
                        }
                    }
                }
            }
        }
        if (LOG) {
            Log.w(TAG, "checkNotify(" + profile + ", " + data + ") level=" + msg.msg_level + ", front="
                    + ModeSwitchManager.isInForground());
        }
    }

    private void goToMessageDetails(Uri uri) {
        Intent intent = new Intent();
        intent.setClass(mContext, MessageDetailActivity.class);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void add(CMMBEmergencyMessage msg, Profile profile) {
        Uri uri = insertDB(msg, profile.getTime());
        profile.id = getID(uri);
        if (profile.id > 0) {
            mCacheQueue.add(profile);
            checkNotify(msg, profile, uri);
        } else {
            Log.e(TAG, "insert() Cannot insert EB into database. profile=" + profile);
        }
    }

    private Uri insertDB(CMMBEmergencyMessage msg, long receiveTime) {
        ContentValues values = new ContentValues(7);
        values.put(EB.BroadcastColumns.NET_LEVEL, msg.net_level);
        values.put(EB.BroadcastColumns.NET_ID, msg.net_id);
        values.put(EB.BroadcastColumns.MSG_ID, msg.msg_id);
        values.put(EB.BroadcastColumns.LEVEL, msg.msg_level);
        values.put(EB.BroadcastColumns.HAS_READ, false);
        values.put(EB.BroadcastColumns.MESSAGE, msg.msg_content);
        values.put(EB.BroadcastColumns.RECEIVE_TIME, receiveTime);
        Uri newUri = mContentResolver.insert(EB.Broadcast.CONTENT_URI, values);
        if (LOG) {
            Log.v(TAG, "insertDB(" + msg + ", " + receiveTime + ") return " + newUri);
        }
        return newUri;
    }

    private long getID(Uri uri) {
        long id = -1;
        try {
            id = Long.parseLong(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            // ignore it
        }
        if (LOG) {
            Log.v(TAG, "getID(" + uri + ") return " + id);
        }
        return id;
    }

    private void remove(long id) {
        if (deleteDB(mContentResolver, id)) {
            mNotificationManager.cancel((int) id);
            Profile find = null;
            for (Profile profile : mCacheQueue) {
                if (profile.id == id) {
                    find = profile;
                    break;
                }
            }
            if (find != null) {
                mCacheQueue.remove(find); // drop it
            } else {
                Log.e(TAG, "remove(" + id + ") Can not find the EB message in cache.");
            }
        }
        if (LOG) {
            Log.v(TAG, "remove(" + id + ")");
        }
    }

    private static boolean deleteDB(ContentResolver cr, long id) {
        int result = cr.delete(EB.Broadcast.CONTENT_URI, EB.Broadcast.ID + "=? ", new String[] { String.valueOf(id) });
        if (result != 1) {
            Log.e(TAG, "deleteDB(" + id + ") result=" + result);
        }
        if (LOG) {
            Log.v(TAG, "deleteDB(" + id + ") result=" + result);
        }
        return result == 1;
    }

    private void clear() {
        if (LOG) {
            Log.v(TAG, "clear() mCacheQueue.size()=" + mCacheQueue.size());
        }
        synchronized (mCacheQueue) {
            clearDB(mContentResolver);
            mNotificationManager.cancelAll();
            mCacheQueue.clear();
        }
    }

    private static void clearDB(ContentResolver cr) {
        int result = cr.delete(EB.Broadcast.CONTENT_URI, null, null);
        if (LOG) {
            Log.v(TAG, "clearDB() result=" + result);
        }
    }

    private static EBManager sManager;
    private static ArrayList<EBListener> sListeners;

    /**
     * Singlton mode. Return the instance of EBMananger.
     * 
     * @param context
     * @return
     */
    /* package */static EBManager getInstance(Context context) {
        if (sManager == null) {
            sManager = new EBManager(context);
        }
        return sManager;
    }

    /**
     * Delete EB from database and cache.
     * 
     * @param cr
     * @param id
     */
    public static synchronized void delete(ContentResolver cr, long id) {
        if (sManager != null) {
            sManager.remove(id);
        } else {
            deleteDB(cr, id);
        }
    }

    /**
     * Clear all EB from database and cache.
     * 
     * @param cr
     */
    public static synchronized void clear(ContentResolver cr) {
        if (sManager != null) {
            sManager.clear();
        } else {
            clearDB(cr);
        }
    }

    /**
     * call all notifications of EB messages.
     */
    public static void cancelNotification() {
        if (sManager != null) {
            sManager.mNotificationManager.cancelAll();
        }
    }

    /**
     * Cancel one notification of specified EB message.
     * 
     * @param id
     *            the row id of EB message.
     */
    public static void cancelNotification(int id) {
        if (sManager != null) {
            sManager.mNotificationManager.cancel(id);
        }
    }

    /**
     * Add the listener for EB.
     * 
     * @param listener
     * @return success or not
     */
    public static boolean addEBListener(EBListener listener) {
        if (sListeners == null) {
            sListeners = new ArrayList<EBListener>();
        }
        return sListeners.add(listener);
    }

    /**
     * Remove the listener from list.
     * 
     * @param listener
     * @return success or not
     */
    public static boolean removeEBListener(EBListener listener) {
        if (sListeners != null) {
            return sListeners.remove(listener);
        }
        return false;
    }

    /**
     * Contain current listener or not.
     * 
     * @param listener
     * @return
     */
    public static boolean containEBListener(EBListener listener) {
        if (sListeners != null) {
            return sListeners.contains(listener);
        }
        return false;
    }

    /**
     * Notify user when EB is coming.
     * 
     */
    public interface EBListener {
        void notify(int level, String content, Uri uri);
    }

    /**
     * Profile of Emergency Broadcast. net_level, net_id and msg_id will be
     * identity. If these three members are same, we think profiles are the
     * same.
     * 
     */
    private static class Profile {
        public long id;
        public int netLevel;
        public int netId;
        public int msgId;
        public long priority;
        
        public Profile() {
            // Do nothing, only for improve emma coverage
        }

        /* package */static Profile copy(CMMBEmergencyMessage em) {
            Profile profile = new Profile();
            profile.netLevel = em.net_level;
            profile.netId = em.net_id;
            profile.msgId = em.msg_id;
            // profile.priority = getTime(em.msg_date, em.msg_time);//need check
            profile.priority = System.currentTimeMillis();
            return profile;
        }

        private static Calendar sCal = new GregorianCalendar(TimeZone.getTimeZone("GMT+08:00"));

        private static long getTime(int date, int time) {
            int year1 = (int) ((date - 15078.2) / 365.25);
            int month1 = (int) ((date - 14956.1 - (int) (year1 * 365.25)) / 30.6001);
            int day = date - 14956 - (int) (year1 * 365.25) - (int) (month1 * 30.6001);
            int key = 0;
            if (month1 == 14 || month1 == 15) {
                key = 1;
            }
            int year = year1 + key + 1900;
            int month = month1 - 1 - key * 12;
            month = month - 1;// magic reduce, not from spec.
            byte[] ba = intToByteArray(time);
            int hour = byteToInt(ba[2]);
            int minute = byteToInt(ba[1]);
            int second = byteToInt(ba[0]);
            sCal.set(year, month, day, hour, minute, second);
            long result = sCal.getTimeInMillis();
            if (LOG) {
                Log.v(TAG, "getTime(" + date + ", " + time + ") "
                        + String.format("%d-%d-%d %d:%d:%d", year, month, day, hour, minute, second) + " result=" + result);
            }
            return result;
        }

        private static byte[] intToByteArray(int i) {
            byte[] result = new byte[4];
            result[3] = (byte) ((i >> 24) & 0xFF);
            result[2] = (byte) ((i >> 16) & 0xFF);
            result[1] = (byte) ((i >> 8) & 0xFF);
            result[0] = (byte) (i & 0xFF);
            return result;
        }

        private static int byteToInt(byte value) {
            return (value >> 4) * 10 + (value & 0x0F);
        }

        public long getTime() {
            return priority;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Profile)) {
                return false;
            }
            Profile cmp = (Profile) o;
            return (netLevel == cmp.netLevel && netId == cmp.netId && msgId == cmp.msgId);
        }

        @Override
        public String toString() {
            return new StringBuilder().append("(net_level=").append(netLevel).append(", net_id=").append(netId).append(
                    ", msg_id=").append(msgId).append(", prority=").append(priority).append(", _id=").append(id).append(
                    ")").toString();
        };

        static Comparator<Profile> getComparator() {
            return new Comparator<Profile>() {

                public int compare(Profile r1, Profile r2) {
                    return (r1.priority == r2.priority) ? 0 : ((r1.priority < r2.priority) ? -1 : 1);
                }

            };
        }
    }

}
