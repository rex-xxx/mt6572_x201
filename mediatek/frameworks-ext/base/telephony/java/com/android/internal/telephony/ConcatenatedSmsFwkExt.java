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

package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;

import com.android.internal.util.HexDump;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;


public class ConcatenatedSmsFwkExt implements IConcatenatedSmsFwkExt {
    private static final String TAG = "ConcatenatedSmsFwkExt";
    
    private static final Uri mRawUri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw");
    private static final String[] CONCATE_PROJECTION = {
            "reference_number",
            "count",
            "sequence"
    };
    private static final String[] PDU_SEQUENCE_PORT_PROJECTION = {
            "pdu",
            "sequence",
            "destination_port"
    };
    
    protected static final int DELAYED_TIME = 45 * 1000;
    
    private ArrayList<TimerRecord> mTimerRecords = new ArrayList<TimerRecord>(5);
    private Context mContext = null;
    private ContentResolver mResolver = null;
    private int mSimId = -1;
    
    /*
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    TimerRecord record = (TimerRecord)msg.obj;
                    Xlog.d(TAG, "timer is expired for concatenated sms " + record.toString());
                    break;
                    
                default:
                    Xlog.d(TAG, "unsupport message " + msg.what);
            }
        }
    }
    */
    
    public ConcatenatedSmsFwkExt(Context context, int simId) {
        if(context == null) {
            Xlog.d(TAG, "FAIL! context is null");
            return;
        }
        this.mContext = context;
        this.mResolver = mContext.getContentResolver();
        this.mSimId = simId;
    }
    
    public boolean isFirstConcatenatedSegment(String address, int refNumber) {
        Xlog.d(TAG, "call isFirstConcatenatedSegment: " + address + "/" + refNumber);
        
        try {
            String where = "address=? AND reference_number=? AND sim_id=?";
            String[] whereArgs = new String[] {
                address,
                Integer.toString(refNumber),
                Integer.toString(mSimId)
            };
            Cursor cursor = mResolver.query(mRawUri,
                    CONCATE_PROJECTION, where, whereArgs, null);
                    
            if(cursor != null) {
                if(cursor.moveToNext() == true) {
                    Xlog.d(TAG, "This segment is not first one");
                    cursor.close();
                    return false;
                }
                cursor.close();
            } else {
                Xlog.d(TAG, "FAIL! cursor is null");
            }
        } catch(SQLException e) {
            Xlog.d(TAG, "FAIL! SQLException");
            return false;
        }
        
        Xlog.d(TAG, "This segment is the first one");
        return true;
    }
    
    public boolean isLastConcatenatedSegment(String address, int refNumber, int msgCount) {
        Xlog.d(TAG, "call isLastConcatenatedSegment: " + address + "/" + refNumber);
        
        try {
            String where = "address=? AND reference_number=? AND sim_id=?";
            String[] whereArgs = new String[] {
                address,
                Integer.toString(refNumber),
                Integer.toString(mSimId)
            };
            Cursor cursor = mResolver.query(mRawUri,
                    CONCATE_PROJECTION, where, whereArgs, null);
                    
            if(cursor != null) {
                int messageCount = cursor.getCount();
                cursor.close();
                return (messageCount == msgCount - 1);
            } else {
                Xlog.d(TAG, "FAIL! cursor is null");
            }
        } catch(SQLException e) {
            Xlog.d(TAG, "FAIL! SQLException");
            return false;
        }
        
        return false;
    }
    
    public void startTimer(Handler h, Object r) {
        Xlog.d(TAG, "call startTimer");
        boolean isParamsValid = checkParamsForMessageOperation(h, r);
        if(isParamsValid == false) {
            Xlog.d(TAG, "FAIL! invalid params");
            return;
        }
        
        addTimerRecord((TimerRecord) r);
        Message m = h.obtainMessage(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        h.sendMessageDelayed(m, DELAYED_TIME);
    }
    
    public void cancelTimer(Handler h, Object r) {
        Xlog.d(TAG, "call cancelTimer");
        boolean isParamsValid = checkParamsForMessageOperation(h, r);
        if(isParamsValid == false) {
            Xlog.d(TAG, "FAIL! invalid params");
            return;
        }
        
        h.removeMessages(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        deleteTimerRecord((TimerRecord) r);
    }
    
    public void refreshTimer(Handler h, Object r) {
        Xlog.d(TAG, "call refreshTimer");
        boolean isParamsValid = checkParamsForMessageOperation(h, r);
        if(isParamsValid == false) {
            Xlog.d(TAG, "FAIL! invalid params");
            return;
        }
        
        h.removeMessages(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        Message m = h.obtainMessage(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        h.sendMessageDelayed(m, DELAYED_TIME);
    }
    
    public TimerRecord queryTimerRecord(String address, int refNumber) {
        Xlog.d(TAG, "call queryTimerRecord");
        
        Xlog.d(TAG, "find record by [" + address + ", " + refNumber + "]");
        for(TimerRecord record : mTimerRecords) {
            if(record.address.equals(address) && record.refNumber == refNumber) {
                Xlog.d(TAG, "find record");
                return record;
            }
        }
        
        Xlog.d(TAG, "don't find record");
        return null;
    }
    
    private void addTimerRecord(TimerRecord r) {
        Xlog.d(TAG, "call addTimerRecord");
        for(TimerRecord record : mTimerRecords) {
            if(record == r) {
                Xlog.d(TAG, "duplicated TimerRecord object be found");
                return;
            }
        }
        
        mTimerRecords.add(r);
    }
    
    private void deleteTimerRecord(TimerRecord r) {
        Xlog.d(TAG, "call deleteTimerRecord");
        
        /*
        int size = mTimerRecords.size();
        
        for(int i = 0; i < size; ++i) {
            TimerRecord record = mTimerRecords.get(i);
            boolean isEquivalent = r.equals(record);
            if(isEquivalent) {
                Xlog.d(TAG, "remove record at index " + i);
                mTimerRecords.remove(i);
                return;
            }
        }
        
        Xlog.d(TAG, "no record be found");
        */
        if(mTimerRecords == null || mTimerRecords.size() == 0) {
            Xlog.d(TAG, "no record can be removed ");
            return;
        }
        
        int countBeforeRemove = mTimerRecords.size();
        mTimerRecords.remove(r);
        int countAfterRemove = mTimerRecords.size();
        
        int countRemoved = countBeforeRemove - countAfterRemove;
        if(countRemoved > 0) {
            Xlog.d(TAG, "remove record(s)" + countRemoved);
        } else {
            Xlog.d(TAG, "no record be removed");
        }
    }
    
    private boolean checkParamsForMessageOperation(Handler h, Object r) {
        Xlog.d(TAG, "call checkParamsForMessageOperation");
        if(h == null) {
            Xlog.d(TAG, "FAIL! handler is null");
            return false;
        }
        if(r == null) {
            Xlog.d(TAG, "FAIL! record is null");
            return false;
        }
        if(!(r instanceof TimerRecord)) {
            Xlog.d(TAG, "FAIL! param r is not TimerRecord object");
            return false;
        }
        
        return true;
    }
    
    private boolean checkTimerRecord(TimerRecord r) {
        Xlog.d(TAG, "call checkTimerRecord");
        if(mTimerRecords.size() == 0) {
            return false;
        }
        
        for(TimerRecord record : mTimerRecords) {
            if(r == record) {
                return true;
            }
        }
        
        return false;
    }
    
    public byte[][] queryExistedSegments(TimerRecord record) {
        Xlog.d(TAG, "call queryExistedSegments");
        
        byte[][] pdus = null;
        try {
            String where = "address=? AND reference_number=? AND sim_id=? AND count=?";
            String[] whereArgs = new String[] {
                record.address,
                Integer.toString(record.refNumber),
                Integer.toString(mSimId),
                Integer.toString(record.msgCount)
            };
            Cursor cursor = mResolver.query(mRawUri,
                    PDU_SEQUENCE_PORT_PROJECTION, where, whereArgs, null);
                    
            if(cursor != null) {
                byte[][] tempPdus = new byte[record.msgCount][];
                
                int columnSeqence = cursor.getColumnIndex("sequence");
                //Xlog.d(TAG, "columnSeqence = " + columnSeqence);
                int columnPdu = cursor.getColumnIndex("pdu");
                //Xlog.d(TAG, "columnPdu = " + columnPdu);
                int columnPort = cursor.getColumnIndex("destination_port");
                //Xlog.d(TAG, "columnPort = " + columnPort);
                
                int cursorCount = cursor.getCount();
                Xlog.d(TAG, "miss " + (record.msgCount - cursorCount) + " segment(s)");
                for(int i = 0; i < cursorCount; ++i) {
                    cursor.moveToNext();
                    int cursorSequence = cursor.getInt(columnSeqence);
                    Xlog.d(TAG, "queried segment " + cursorSequence + ", ref = " + record.refNumber);
                    tempPdus[cursorSequence - 1] = HexDump.hexStringToByteArray(
                            cursor.getString(columnPdu));
                    if(tempPdus[cursorSequence - 1] == null) {
                        Xlog.d(TAG, "miss segment " + cursorSequence + ", ref = " + record.refNumber);
                    }
                    
                    if(!cursor.isNull(columnPort)) {
                        Xlog.d(TAG, "segment contain port " + cursor.getInt(columnPort));
                        cursor.close();
                        return null;
                    }
                }
                cursor.close();
                
                // Xlog.d(TAG, "filter null pdu");
                pdus = new byte[cursorCount][];
                int index = 0;
                for(int i = 0, len = tempPdus.length; i < len; ++i) {
                    if(tempPdus[i] != null) {
                        // Xlog.d(TAG, "add segment " + index + " into pdus");
                        pdus[index++] = tempPdus[i];
                    }
                }
            } else {
                Xlog.d(TAG, "FAIL! cursor is null");
            }
        } catch(SQLException e) {
            Xlog.d(TAG, "FAIL! SQLException");
            return null;
        }
        
        return pdus;
    }
    
    public void deleteExistedSegments(TimerRecord record) {
        Xlog.d(TAG, "call queryExistedSegments");
        
        try {
            String where = "address=? AND reference_number=? AND sim_id=?";
            String[] whereArgs = new String[] {
                record.address,
                Integer.toString(record.refNumber),
                Integer.toString(mSimId)
            };
            int numOfDeleted = mResolver.delete(mRawUri, where, whereArgs);
            Xlog.d(TAG, "remove " + numOfDeleted + " segments, ref =  " + record.refNumber);
        } catch(SQLException e) {
            Xlog.d(TAG, "FAIL! SQLException");
        }
        
        deleteTimerRecord(record);
    }
}
