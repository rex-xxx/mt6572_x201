package com.android.internal.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SmsCb.CbChannel;

import com.android.internal.telephony.Phone;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import com.android.internal.telephony.PhoneConstants;

public class CellBroadcastFwkExt implements ICellBroadcastFwkExt {
    private static final String TAG = "CellBroadcastFwkExt";
    
    private static final int EVENT_QUERY_CB_CONFIG = 1;
    private static final int EVENT_OPEN_ETWS_CHANNEL_DONE = 2;
    private static final int EVENT_CLOSE_ETWS_CHANNEL_DONE = 3;
    
    private static final int NEXT_ACTION_NO_ACTION = 100;
    private static final int NEXT_ACTION_ONLY_ADD = 101;
    private static final int NEXT_ACTION_ONLY_REMOVE = 101;
    private static final int NEXT_ACTION_REMOVE_THEN_ADD = 102;
    
    public static final int CB_SET_TYPE_NORMAL = 0;
    public static final int CB_SET_TYPE_OPEN_ETWS_CHANNEL = 1;
    public static final int CB_SET_TYPE_CLOSE_ETWS_CHANNEL = 2;
    
    private PhoneBase mPhone = null;
    private CommandsInterface mCM = null;
    private Context mContext = null;
    private int mPhoneId = PhoneConstants.GEMINI_SIM_1;
    
    private Object mLock = null;
    
    private CellBroadcastConfigInfo mConfigInfo = null;
    private boolean mSuccess = false;
    
    private static final int MAX_ETWS_NOTIFICATION = 4;
    private ArrayList<EtwsNotification> mEtwsNotificationList = null;
    
    private static final Uri CHANNEL_URI = CbChannel.CONTENT_URI;
    private static final Uri CHANNEL_URI1 = Uri.parse("content://cb/channel1");
    
    CellBroadcastFwkExt(PhoneBase phone) {
        if(phone == null) {
            Xlog.d(TAG, "FAIL! phone is null");
            return;
        }
        
        this.mPhone = phone;
        this.mCM = phone.mCM;
        this.mContext = phone.getContext();
        this.mPhoneId = phone.getMySimId();
        
        this.mLock = new Object();
        
        mEtwsNotificationList = new ArrayList<EtwsNotification>(MAX_ETWS_NOTIFICATION);
    }
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Xlog.d(TAG, "receive message " + idToString(msg.what));
            AsyncResult ar;
            EtwsNotification noti;
            switch(msg.what) {
                case EVENT_QUERY_CB_CONFIG:
                    Xlog.d(TAG, "handle EVENT_QUERY_CB_CONFIG");
                    ar = (AsyncResult) msg.obj;
                    if(ar.exception != null) {
                        Xlog.d(TAG, "fail to query cb config");
                        return;
                    } else {
                        CellBroadcastConfigInfo cbConfigInfo = (CellBroadcastConfigInfo) ar.result;
                        String oldChannelConfig = cbConfigInfo.channelConfigInfo;
                        
                        int nextAction = msg.arg1;
                        noti = (EtwsNotification) ar.userObj;
                        handleQueriedConfig(oldChannelConfig, nextAction, noti);
                    }
                    break;
                    
                case EVENT_OPEN_ETWS_CHANNEL_DONE:
                    Xlog.d(TAG, "handle EVENT_OPEN_ETWS_CHANNEL_DONE");
                    ar = (AsyncResult) msg.obj;
                    noti = (EtwsNotification) ar.userObj;
                    if(ar.exception == null) {
                        Xlog.d(TAG, "success to open cb channel " + noti.messageId);
                        int nextAction = msg.arg1;
                        if(nextAction == NEXT_ACTION_ONLY_ADD) {
                            addEtwsNoti(noti);
                        } else if(nextAction == NEXT_ACTION_REMOVE_THEN_ADD) {
                            removeFirstEtwsNotiThenAdd(noti);
                        } else {
                            Xlog.d(TAG, "invalid next action " + nextAction);
                        }
                        updateDatabase(true);
                    } else {
                        Xlog.d(TAG, "fail to open cb channel");
                    }
                    break;
                    
                case EVENT_CLOSE_ETWS_CHANNEL_DONE:
                    Xlog.d(TAG, "handle EVENT_CLOSE_ETWS_CHANNEL_DONE");
                    ar = (AsyncResult) msg.obj;
                    noti = (EtwsNotification) ar.userObj;
                    if(ar.exception == null) {
                        Xlog.d(TAG, "success to close cb channel " + noti.messageId);
                        int nextAction = msg.arg1;
                        if(nextAction == NEXT_ACTION_ONLY_REMOVE) {
                            removeEtwsNoti(noti);
                        } else {
                            Xlog.d(TAG, "invalid next action " + nextAction);
                        }
                        updateDatabase(false);
                    } else {
                        Xlog.d(TAG, "fail to close cb channel");
                    }
                    break;
                default:
                    Xlog.d(TAG, "unknown CB event " + msg.what);
                    break;
            }
        }
    };
    
    public void openEtwsChannel(EtwsNotification newEtwsNoti) {
        Xlog.d(TAG, "openEtwsChannel");
        
        Message response = mHandler.obtainMessage(EVENT_QUERY_CB_CONFIG, EVENT_OPEN_ETWS_CHANNEL_DONE, 0, newEtwsNoti);
        mCM.queryCellBroadcastConfigInfo(response);
    }
    
    public void closeEtwsChannel(EtwsNotification newEtwsNoti) {
        Xlog.d(TAG, "closeEtwsChannel");
        
        Message response = mHandler.obtainMessage(EVENT_QUERY_CB_CONFIG, EVENT_CLOSE_ETWS_CHANNEL_DONE, 0, newEtwsNoti);
        mCM.queryCellBroadcastConfigInfo(response);
    }
    
    private String idToString(int id) {
        switch(id) {
            case EVENT_QUERY_CB_CONFIG: return "EVENT_QUERY_CB_CONFIG";
            case EVENT_OPEN_ETWS_CHANNEL_DONE: return "EVENT_OPEN_ETWS_CHANNEL_DONE";
            case EVENT_CLOSE_ETWS_CHANNEL_DONE: return "EVENT_CLOSE_ETWS_CHANNEL_DONE";
            default: return "unknown message id: " + id;
        }
    }
    
    private SortedSet<Integer> mergeConfigList(ArrayList<Integer> oldConfigList, ArrayList<Integer> newConfigList) {
        Xlog.d(TAG, "call mergeConfigInfoList");
        SortedSet sortedConfig = new TreeSet();
        if(oldConfigList != null && oldConfigList.size() > 0) {
            for(int i : oldConfigList) {
                sortedConfig.add(i);
            }
        } else {
            Xlog.d(TAG, "oldConfigList is null");
        }
        if(newConfigList != null && newConfigList.size() > 0) {
            for(int i : newConfigList) {
                sortedConfig.add(i);
            }
        } else {
            Xlog.d(TAG, "newConfigList is null");
        }
        
        return sortedConfig;
    }
    
    private SortedSet<Integer> minusConfigList(ArrayList<Integer> oldConfigList, ArrayList<Integer> newConfigList) {
        Xlog.d(TAG, "call minusConfigList");
        SortedSet sortedConfig = new TreeSet();
        if(oldConfigList == null || oldConfigList.size() == 0) {
            Xlog.d(TAG, "oldConfigList, no need to minus");
            return sortedConfig;
        }
        
        if(newConfigList != null && newConfigList.size() > 0) {
            for(int i : newConfigList) {
                for(int j = 0, n = oldConfigList.size(); j < n; ++j) {
                    if(i == oldConfigList.get(j)) {
                        Xlog.d(TAG, "delete config: " + i);
                        oldConfigList.remove(j);
                        break;
                    }
                }
            }
        }
        
        for(int i : oldConfigList) {
            sortedConfig.add(i);
        }
        
        return sortedConfig;
    }
    
    private ArrayList<Integer> parseConfigInfoToList(String config) {
        Xlog.d(TAG, "call parseConfigInfoToList");
        int left = 0;
        int right = 0;
        int value = 0;
        boolean meetMinus = false;
        ArrayList<Integer> ret = new ArrayList<Integer>();
        if( config == null || config.length() == 0) {
            return ret;
        } else if (config.length() == 1 && config.charAt(0) == ',') {
            return ret;
        }
        
        String temp = (config + ",");
        for(int i = 0, n = temp.length(); i < n; ++i) {
            char ch = temp.charAt(i);
            if (ch >= '0' && ch <= '9') {
                value = (value * 10 + (ch - '0'));
            } else if (ch == '-') {
                meetMinus = true;
                left = value;
                value = 0;
            } else if (ch == ',') {
                // add range [left, right] only if meetMinus is true
                if (meetMinus) {
                    right = value;
                    for (int j = left; j <= right; ++j) {
                        ret.add(j);
                    }
                    meetMinus = false;
                } else {
                    ret.add(value);
                }
                value = 0;
            }
        } // end temp for loop
        
        return ret;
    }
    
    private String parseSortedSetToString(SortedSet<Integer> sortedSet) {
        Xlog.d(TAG, "call parseSortedSet");
        if(sortedSet == null || sortedSet.size() == 0) {
            Xlog.d(TAG, "sortedSet is null");
            return null;
        }
        
        StringBuilder ret = new StringBuilder();
        for(int i : sortedSet) {
            ret.append(i);
            ret.append(',');
        }
        ret.deleteCharAt(ret.length() - 1);
        
        return ret.toString();
    }
    
    private void handleQueriedConfig(String config, int nextAction, EtwsNotification noti) {
        Xlog.d(TAG, "handleQueriedConfig");
        
        ArrayList<Integer> oldConfigList = parseConfigInfoToList(config);
        ArrayList<Integer> newConfigList = new ArrayList<Integer>();
        //newConfigList.add(noti.messageId);//4352,4353,4354,4355,4356
        newConfigList.add(4352);
        newConfigList.add(4353);
        newConfigList.add(4354);
        newConfigList.add(4355);
        newConfigList.add(4356);
        SortedSet<Integer> sortedConfig = null;
        String finalConfig = null;
        Message response = null;
        
        if(nextAction == EVENT_OPEN_ETWS_CHANNEL_DONE) {
            Xlog.d(TAG, "to open ETWS channel: " + noti.messageId);
            // sortedConfig = mergeConfigList(oldConfigList, newConfigList);
            int size = mEtwsNotificationList.size();
            if(size < MAX_ETWS_NOTIFICATION) {
                Xlog.d(TAG, "list is NOT full");
                // add new ETWS channel into current CB config
                sortedConfig = mergeConfigList(oldConfigList, newConfigList);
                response = mHandler.obtainMessage(nextAction, NEXT_ACTION_ONLY_ADD, 0, noti);
            } else {
                Xlog.d(TAG, "list is full");
                // remove earliest ETWS channel from current CB config
                EtwsNotification earliestNoti = mEtwsNotificationList.get(0);
                for(int i = 0; i < oldConfigList.size(); ++i) {
                    int ch = oldConfigList.get(i);
                    if(ch == earliestNoti.messageId) {
                        Xlog.d(TAG, "remove channel from old config: " + earliestNoti.messageId);
                        oldConfigList.remove(i);
                        break;
                    }
                }
                
                // add new ETWS channel into current CB config
                sortedConfig = mergeConfigList(oldConfigList, newConfigList);
                response = mHandler.obtainMessage(nextAction, NEXT_ACTION_REMOVE_THEN_ADD, 0, noti);
            }
            
            finalConfig = parseSortedSetToString(sortedConfig);
            mCM.setCellBroadcastChannelConfigInfo(finalConfig, CB_SET_TYPE_OPEN_ETWS_CHANNEL, response);
        } else if(nextAction == EVENT_CLOSE_ETWS_CHANNEL_DONE) {
            Xlog.d(TAG, "to close ETWS channel: " + noti.messageId);
            sortedConfig = minusConfigList(oldConfigList, newConfigList);
            response = mHandler.obtainMessage(nextAction, NEXT_ACTION_ONLY_REMOVE, 0, noti);
            finalConfig = parseSortedSetToString(sortedConfig);
            mCM.setCellBroadcastChannelConfigInfo(finalConfig, CB_SET_TYPE_CLOSE_ETWS_CHANNEL, response);
        } else {
            Xlog.d(TAG, "invalid action: " + nextAction);
            return;
        }
    }
    
    public boolean containDuplicatedEtwsNotification(EtwsNotification newEtwsNoti) {
        Xlog.d(TAG, "call containDuplicatedEtwsNotification");
        if(newEtwsNoti == null) {
            Xlog.d(TAG, "null EtwsNotification");
            return false;
        }
        
        for(EtwsNotification e : mEtwsNotificationList) {
            if(e.isDuplicatedEtws(newEtwsNoti)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void addEtwsNoti(EtwsNotification noti) {
        Xlog.d(TAG, "call addEtwsNoti");
        mEtwsNotificationList.add(noti);
    }
    
    private void removeEtwsNoti(EtwsNotification noti) {
        Xlog.d(TAG, "call removeEtwsNoti");
        
        int count = 0;
        for(int i = 0, n = mEtwsNotificationList.size(); i < n;) {
            EtwsNotification element = mEtwsNotificationList.get(i);
            if(element.messageId == noti.messageId) {
                mEtwsNotificationList.remove(i);
                n--;
                count++;
            } else {
                i++;
            }
        }
        
        Xlog.d(TAG, "remove noti " + count);
    }
    
    private void removeFirstEtwsNotiThenAdd(EtwsNotification noti) {
        Xlog.d(TAG, "call removeFirstEtwsNotiThenAdd");
        if (mEtwsNotificationList.size() >= MAX_ETWS_NOTIFICATION) {
            mEtwsNotificationList.remove(0);
        }
        mEtwsNotificationList.add(noti);
    }
    
    private void updateDatabase(boolean open) {
        Xlog.d(TAG, "updateDatabase " + open);
        Uri uri= CHANNEL_URI;
        if (mPhoneId == PhoneConstants.GEMINI_SIM_2) {
            uri = CHANNEL_URI1;
        }
        int channel = -1;
        String name = "";
        int enable = -1;
        int key = -1;
        int[] Channels = new int[]{4352,4353,4354,4355,4356};
        boolean[] handled = new boolean[]{false,false,false,false,false};
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    channel = cursor.getInt(cursor.getColumnIndexOrThrow(CbChannel.NUMBER));
                    Xlog.d(TAG, "updateDatabase channel:" + channel);
                    if (channel >= Channels[0] && channel <= Channels[4]) {
                        enable = cursor.getInt(cursor.getColumnIndexOrThrow(CbChannel.ENABLE));
                        handled[channel - Channels[0]] = true;
                        if ((enable == 1 && open) || (enable == 0 && !open)) {
                            continue;
                        }
                        key = cursor.getInt(cursor.getColumnIndexOrThrow(CbChannel._ID));
                        ContentValues value = new ContentValues(1);
                        value.put(CbChannel.ENABLE, open?1:0);
                        mContext.getContentResolver().update(uri, value, CbChannel._ID+"="+key, null);
                    }
                }
            }
        } catch (Exception ex) {
            Xlog.e(TAG, "get channels error:", ex);
            return;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        int len = handled.length;
        for (int i = 0; i < 5; i++) {
            if (!handled[i]) {
                channel = i + Channels[0];
                ContentValues values = new ContentValues();
                values.put(CbChannel.NAME, "" + channel);
                values.put(CbChannel.NUMBER, channel);
                values.put(CbChannel.ENABLE, open?1:0);
                mContext.getContentResolver().insert(uri, values);
            }
        }
    }
}
