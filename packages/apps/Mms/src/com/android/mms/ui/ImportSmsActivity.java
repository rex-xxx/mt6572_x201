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
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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
package com.android.mms.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.provider.Telephony.Sms;
import android.util.Log;
import android.widget.Toast;

import com.android.mms.R;
import com.android.mms.util.Recycler;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.os.storage.EncapsulatedStorageManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/** M:
 * ImportSmsActivity
 */
public class ImportSmsActivity extends PreferenceActivity {
    public static final String PREF_IMPORT = "pref_key_import_sms";
    private static final String TAG = "MMS/ImportSmsActivity";
    private PreferenceCategory mSmsCategory;
    private ProgressDialog mProgressdialog = null;
    private static final String TABLE_SMS = "sms";
//    public static final String SDCARD_DIR_PATH = "//sdcard//message//";
    private static final String[] SMS_COLUMNS =
    { "thread_id", "address","m_size", "person", "date", "protocol", "read", "status", "type", "reply_path_present",
      "subject", "body", "service_center", "locked", "sim_id", "error_code", "seen"};
    public Handler mMainHandler; 
    private static final String[] ADDRESS_COLUMNS = {"address"};
    private static final int IMPORT_SMS    = 2;    
    private static final int NO_DATABASE   = 4;
    private static final int IMPORT_SUCCES = 5;
    private static final int IMPORT_FAILED = 6;
    private String mImportFileName = "";
    private static final int MAX_OPERATIONS_PER_PATCH = 20;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.importpreferences);
        mSmsCategory = (PreferenceCategory) findPreference(PREF_IMPORT);
        newMainHandler();
    }

    private void newMainHandler() {
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) { 
                int output = R.string.import_message_list_empty;  
                switch(msg.what) { 
                case NO_DATABASE:
                    output = R.string.import_message_list_empty;
                    showToast(output);
                    finish();
                    break;
                case IMPORT_SUCCES:
                    output = R.string.import_message_success;
                    break;
                case IMPORT_FAILED: 
                    output = R.string.import_message_fail;
                    break;
                default: 
                    break;
                }
                showToast(output);
                
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        MmsLog.d(TAG, "onResume");
        if (!isSDcardReady()) {
            finish();
            return;
        }
        new Thread() {
            public void run() {
                try {
                    String sdCardDirPath = EncapsulatedStorageManager.getDefaultPath()
                            + SmsPreferenceActivity.SDCARD_DIR_PATH;
                    getSMSFileRecursively(sdCardDirPath);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();
    }

    protected void onDestroy() {
        super.onDestroy();
        MmsLog.d(TAG, "onDestroy");
        new Thread() {
            public void run() {
                Log.d(TAG, "onDestroy delete old message");
                Recycler.getSmsRecycler().deleteOldMessages(getApplicationContext());  
                Log.d(TAG, "onDestroy delete old message end");
            }
        }.start();
    }
    
    private void getSMSFileRecursively(String path) throws IOException {
        mSmsCategory.removeAll();
        File directory = new File(path);
        FileFilter ff = new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else if (f.getName().toLowerCase().endsWith(".db")
                        && f.canRead()) {
                    return true;
                }
                return false;
            }
        };
        
        final File[] files = directory.listFiles(ff);

        // e.g. secured directory may return null toward listFiles().
        if (files == null) {
            MmsLog.w(TAG, "listFiles() returned null (directory: " + directory + ")");
            mMainHandler.sendEmptyMessage(NO_DATABASE);
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            String date = new Date(file.lastModified()).toLocaleString();
            Preference sms = new Preference(this);
            sms.setKey(fileName);
            sms.setTitle(fileName);
            sms.setSummary(date);
            sms.setOnPreferenceClickListener(mListener);
            mSmsCategory.addPreference(sms);
        }
    }
    
    private OnPreferenceClickListener mListener = new OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference arg0) {
            // TODO Auto-generated method stub
            mImportFileName = arg0.getKey();
            MmsLog.d(TAG, "Click listener you choosed file " + mImportFileName);
            //importMessages(key);
            showDialog(IMPORT_SMS);
            return false;
        }
        
    };
    @Override
    protected void onPrepareDialog(int id, Dialog d) { 
        if (id == IMPORT_SMS && d != null) {
            ((AlertDialog)d).setMessage(getString(R.string.whether_import_item) + " " + mImportFileName + "?");
        }
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == IMPORT_SMS) {
            return new AlertDialog.Builder(this)
            .setMessage(getString(R.string.whether_import_item) + " " + mImportFileName + "?")
            .setTitle(R.string.pref_summary_import_msg)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            importMessages(mImportFileName);
                            return;
                        }
                    })
            .setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int which) {
                            mImportFileName = "";
                        }
                    }).create();
        }

        return super.onCreateDialog(id);
    }

    private boolean importMessages(final String key) {
        MmsLog.d(TAG,"importMessages begin");
        if (!isSDcardReady() || key == null) {
            return false;
        }
        mProgressdialog = ProgressDialog.show(this, "",getString(R.string.import_message_ongoing), true);
        new Thread() {
            public void run() {
                SQLiteDatabase db;
                Cursor cursor = null; 
                try {
                    String sdCardDirPath = EncapsulatedStorageManager.getDefaultPath()
                            + MessagingPreferenceActivity.SDCARD_MESSAGE_DIR_PATH;
                    db = SQLiteDatabase.openDatabase(sdCardDirPath + key,null, SQLiteDatabase.OPEN_READONLY);
                    cursor = db.query(TABLE_SMS, SMS_COLUMNS, null, null, null, null, "date ASC");
                    if (cursor == null) {
                        MmsLog.w(TAG, "importDict sms cursor is null ");
                        mMainHandler.sendEmptyMessage(IMPORT_FAILED);
                        return;
                    }
                    int count = cursor.getCount();
                    MmsLog.d(TAG, "importDict sms count = " + count);
                    if (count > 0) {
                        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
                        int patchNumber = 0;
                        boolean loop = true;
                        while (cursor.moveToNext() && loop) {
                            if (mProgressdialog != null && !mProgressdialog.isShowing()) {
                                loop = false;
                            }
                            ContentValues v = getValueFromCursor(cursor);
                            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Sms.CONTENT_URI);
                            builder.withValues(v);
                            operationList.add(builder.build());
                            patchNumber++;
                            //if the number reached to the limit number, apply patch
                            if (patchNumber % MAX_OPERATIONS_PER_PATCH == 0 
                                    && operationList.size() > 0) {
                                 try {
                                      getContentResolver().applyBatch("sms", operationList);
                                      MmsLog.d(TAG, "apply end");
                                 } catch (android.os.RemoteException e) {
                                     MmsLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                                 } catch (android.content.OperationApplicationException e) {
                                     MmsLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                                 } finally {
                                     operationList.clear();
                                     patchNumber = 0;
                                 }
                            }
                        }
                        // the last time maybe not reach the limit number, apply the last patch
                        if (patchNumber > 0 && operationList.size() > 0) {
                             try {
                                  getContentResolver().applyBatch("sms", operationList);
                             } catch (android.os.RemoteException e) {
                                 MmsLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                             } catch (android.content.OperationApplicationException e) {
                                 MmsLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                             } finally {
                                 operationList.clear();
                                 patchNumber = 0;
                            }
                        }
                    } else {
                        MmsLog.w(TAG, "importDict sms is empty");
                        return;
                    }
                    MmsLog.d(TAG, "import message success");
                    if (mProgressdialog != null && mProgressdialog.isShowing()) {
                        mMainHandler.sendEmptyMessage(IMPORT_SUCCES);
                    }
                    db.close();
                } catch (SQLiteException e) {
                    MmsLog.e(TAG, "can't open the database file");
                    mMainHandler.sendEmptyMessage(IMPORT_FAILED);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (null != mProgressdialog) {
                        mProgressdialog.dismiss();
                    }
                }
                
            }
        }.start();
        return true;
    }
    
    private ContentValues getValueFromCursor(Cursor cursor) {
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        Long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        int currentSimId = cursor.getInt(cursor.getColumnIndexOrThrow("sim_id"));
        String serviceCenter = cursor.getString(cursor.getColumnIndexOrThrow("service_center"));
        int messageType = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
        int read = cursor.getInt(cursor.getColumnIndexOrThrow("read")); 
        // int seen = cursor.getInt(cursor.getColumnIndexOrThrow("seen")); 
        ContentValues insertValues = new ContentValues();
        insertValues.put(Sms.ADDRESS, address);
        insertValues.put(Sms.DATE, date);
        insertValues.put(Sms.BODY, body);
        insertValues.put(EncapsulatedTelephony.Sms.SIM_ID, currentSimId);
        insertValues.put(Sms.TYPE, messageType);
        insertValues.put(Sms.SERVICE_CENTER, serviceCenter);
        insertValues.put(Sms.READ, read);
        insertValues.put(Sms.SEEN, 1);
        insertValues.put("import_sms", true);
        return insertValues;
    }
    
    private boolean isSDcardReady() {
        boolean isSDcard = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); 
        if (!isSDcard) {
            showToast(R.string.no_sd_card);
            MmsLog.d(TAG, "there is no SD card");
            return false;
        }
        return true;
    }

    private void showToast(int id) { 
        Toast t = Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_SHORT);
        t.show();
    }

}
