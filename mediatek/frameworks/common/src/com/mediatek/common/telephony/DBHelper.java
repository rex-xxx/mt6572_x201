/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.common.telephony;


import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;   
import android.database.sqlite.SQLiteDatabase;   
import android.database.sqlite.SQLiteOpenHelper;   
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteFullException;



public class DBHelper extends SQLiteOpenHelper {  
  
    private static final String DATABASE_NAME = "setupMenu.db";  
    private static final int DATABASE_VERSION = 1; 
    private final static String TABLE_NAME = "menu_info";
    public final static String DB_ID = "_id"; 
    public final static String DB_INSTANCE = "instance"; 
    public final static String DB_TITLE = "data"; 
      
    public DBHelper(Context context) {  
        //CursorFactory set into null, using default value   
        super(context, DATABASE_NAME, null, DATABASE_VERSION);  
    }  
  
    //onCreate will be called when the db firstly created   
    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        MtkCatLog.d(this, "DBHelper: onCreate");
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + DB_ID + " INTEGER PRIMARY KEY, "
        + DB_INSTANCE + " text, "+ DB_TITLE + " text);";
        db.execSQL(sql);         
    }

    //if system find the version is diffrence from current version, onUpgrade() will be called
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        MtkCatLog.d(this, "DBHelper: onUpgrade");
        String sql=" DROP TABLE IF EXISTS "+TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
    
    public Cursor select() {
    	  MtkCatLog.d(this, "DBHelper: select");
        SQLiteDatabase db=this.getReadableDatabase();
        String[] columns = new String[]{DB_ID, DB_INSTANCE, DB_TITLE};

        Cursor cursor=db.query(TABLE_NAME, columns, null, null, null, null, null);

        return cursor;
    }
    
    public long insert(String inst, String newData) {
    	  MtkCatLog.d(this, "DBHelper: insert :" + newData + "for :" + inst);
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues cv=new ContentValues(); 
        cv.put(DB_INSTANCE, inst);
        cv.put(DB_TITLE, newData);
        long row=db.insert(TABLE_NAME, null, cv);
        return row;
    }
    
    public void delete(int id) {
    	  MtkCatLog.d(this, "DBHelper: delete");
        SQLiteDatabase db=this.getWritableDatabase();
        String where=DB_ID+"=?";
        String[] whereValue={Integer.toString(id)};
        db.delete(TABLE_NAME, where, whereValue);
    }
    
    public void update(int id,String newData) {
    	  MtkCatLog.d(this, "DBHelper: update: " + newData + "for:" + id);
        SQLiteDatabase db=this.getWritableDatabase();
        String where=DB_ID+"=?";
        String[] whereValue={Integer.toString(id)};
        ContentValues cv=new ContentValues(); 
        cv.put(DB_TITLE, newData);
        db.update(TABLE_NAME, cv, where, whereValue);
    }
    
    public String readDataFromDB(String s) {
    	  MtkCatLog.d(this, "DBHelper: readDataFromDB:" + s);
    	  String str = null;
    	  String s1 = null;
        Cursor cs = select();
        
        while(cs.moveToNext()) {
            s1 = cs.getString(cs.getColumnIndex(DB_INSTANCE));
            if(s.equals(s1)) {
                str = cs.getString(cs.getColumnIndex(DB_TITLE));
                MtkCatLog.d(this, "readDataFromDB:" + str);
                cs.close();
                return str;
            }
        }
        
        cs.close();	
        MtkCatLog.d(this, "readDataFromDB: null 0");
        return null;        	
    }
    
    public void saveDataToDB(String inst, String data) {
        MtkCatLog.d(this, "DBHelper: saveDataToDB: " + data + "into: " + inst);
        String str = null;
        String s1 = null;
        int id = -1;
        /* TODO: reset the flage of DB clear here */
        
        Cursor cs = select();
        
        while(cs.moveToNext()) {
            s1 = cs.getString(cs.getColumnIndex(DB_INSTANCE));
            if(inst.equals(s1)) {
                id = cs.getInt(cs.getColumnIndex(DB_ID));
                MtkCatLog.d(this, "saveDataToDB: update");
                try {
                    update (id, data);
                } catch (SQLiteFullException efx) {
                    MtkCatLog.d(this, "database or disk is full");
                }
            }
        }
        cs.close();
        
        //no corresponding data in table, we insert it.
        if (id == -1) {
            MtkCatLog.d(this, "saveDataToDB: insert");
            try {
                insert(inst, data);
            } catch (SQLiteFullException efx) {
                MtkCatLog.d(this, "database or disk is full when insert data");
            }
        }
    }
}  
