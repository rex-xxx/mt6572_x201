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

package com.mediatek.contacts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.storage.IMountService;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.vcard.ExportVCardActivity;

import java.io.File;
import com.mediatek.storage.StorageManagerEx;

public class ShareContactViaSDCard extends Activity {

    private static final String TAG = "ShareContactViaSDCard";
    private String mAction;
    private Uri mDataUri;
    // if click the share menu in contact_detail_view,then the mSingleContactId
    // is not [-1]
    private int mSingleContactId = -1;
    // if click share visible contact in peopleActivity's menu,mLookUpUris is
    // the choosed contacts's lookupuri
    private String mLookUpUris;
    private Intent mIntent;
    private ProgressDialog mProgressDialog;
    private SearchContactThread mSearchContactThread;

    // check the sd card is full or exist.
    boolean mSdIsVisible = true;

    static final String[] CONTACTS_PROJECTION = new String[] { Contacts._ID, // 0
            Contacts.DISPLAY_NAME_PRIMARY, // 1
            Contacts.DISPLAY_NAME_ALTERNATIVE, // 2
            Contacts.SORT_KEY_PRIMARY, // 3
            Contacts.DISPLAY_NAME, // 4
    };

    static final int PHONE_ID_COLUMN_INDEX = 0;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mIntent = getIntent();
        mAction = mIntent.getAction();

        /** M: Bug Fix for CR: ALPS00395378 @{ 
         * Original Code:
         * mLookUpUris = mIntent.getStringExtra("LOOKUPURIS");
         */
        mLookUpUris = null;
        final Uri extraUri = (Uri) mIntent.getExtra(Intent.EXTRA_STREAM);
        if (null != extraUri) {
            mLookUpUris = extraUri.getLastPathSegment();
        }
        /** @} M: Bug fix for CR: ALPS00395378 */

        String contactId = mIntent.getStringExtra("contactId");
        String userProfile = mIntent.getStringExtra("userProfile");

        if (userProfile != null && "true".equals(userProfile)) {
            Toast.makeText(this.getApplicationContext(),
                    getString(R.string.user_profile_cannot_sd_card),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (contactId != null && !"".equals(contactId)) {
            mSingleContactId = Integer.parseInt(contactId);
        }

        Log.i(TAG, "mAction is " + mAction);

        if (!checkSDCardAvaliable()) {
            showAlertDialog(R.string.no_sdcard_message,
                    R.string.no_sdcard_title);
        }

        if (isSDCardFull()) {
            showAlertDialog(R.string.storage_full, R.string.storage_full);
        }
        /** M: Bug Fix for ALPS00407311 @{ */
        if ((null != extraUri && extraUri.toString().startsWith("file") && mSingleContactId == -1)
                || TextUtils.isEmpty(mLookUpUris)) {
            Toast.makeText(this.getApplicationContext(),
                    getString(R.string.file_already_on_sd_card), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        /** @} */

    }

    private void showAlertDialog(int msgText, int msgTitle) {
        AlertDialog alert = new AlertDialog.Builder(this).create();
        alert.setCanceledOnTouchOutside(true);
        alert.setMessage(this.getResources().getText(msgText));
        alert.setTitle(msgTitle);
        alert.setIconAttribute(android.R.attr.alertDialogIcon);
        alert.setButton(this.getResources().getText(android.R.string.ok),
                mCancelListener);
        alert.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        alert.show();

        mSdIsVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSdIsVisible) {
            if (Intent.ACTION_SEND.equals(mAction)
                    && mIntent.hasExtra(Intent.EXTRA_STREAM)) {
                showProgressDialog();
            }
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            String title = getString(R.string.please_wait);
            String message = getString(R.string.please_wait);
            mProgressDialog = ProgressDialog.show(this, title, message, true,
                    false);
            mProgressDialog.setOnCancelListener(mSearchContactThread);
            mSearchContactThread = new SearchContactThread();
            mSearchContactThread.start();
        }
    }

    /**
     * share main method,the logic block
     * @param lookUpUris
     */
    public void shareViaSDCard(String lookUpUris) {
        StringBuilder contactsID = new StringBuilder();
        int curIndex = 0;
        Cursor cursor = null;
        String id = null;
        if (mSingleContactId == -1) {
            StringBuilder selection = changeLookupUrisToSelection(lookUpUris);

            cursor = getContentResolver().query(
                    /* dataUri */Contacts.CONTENT_URI, CONTACTS_PROJECTION,
                    selection.toString(), null, null);
            Log.i(TAG, "cursor is " + cursor);
            if (null != cursor) {
                while (cursor.moveToNext()) {
                    if (cursor != null) {
                        id = cursor.getString(PHONE_ID_COLUMN_INDEX);
                    }
                    if (curIndex++ != 0) {
                        contactsID.append("," + id);
                    } else {
                        contactsID.append(id);
                    }
                }
                cursor.close();
            }
        } else {
            id = Integer.toString(mSingleContactId);
            contactsID.append(id);
        }

        String exportselection = Contacts._ID + " IN (" + contactsID.toString()
                + ")";

        Intent it = new Intent(this, ExportVCardActivity.class);
        it.putExtra("multi_export_type", 1);
        it.putExtra("exportselection", exportselection);
        this.startActivity(it);
        finish();
        return;
    }

    private StringBuilder changeLookupUrisToSelection(String lookUpUris) {
        String[] tempUris = lookUpUris.split(":");
        StringBuilder selection = new StringBuilder(Contacts.LOOKUP_KEY
                + " in (");
        int index = 0;
        for (int i = 0; i < tempUris.length; i++) {
            selection.append("'" + tempUris[i] + "'");
            if (index != tempUris.length - 1) {
                selection.append(",");
            }
            index++;
        }
        selection.append(")");
        return selection;
    }

    private boolean checkSDCardAvaliable() {
        return (Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED));
    }

    private boolean isSDCardFull() {
        getExternalStorageDirectory();
        String state = getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = getExternalStorageDirectory();
            String path = sdcardDir.getPath();
            if (TextUtils.isEmpty(path)) {
                return false;
            }
            Log.d(TAG, "isSDCardFull storage path is " + path);
            StatFs sf = null;
            try {
                sf = new StatFs(path);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return false;
            }

            if (sf == null) {
                Log.e(TAG, "isSDCardFull sf is null ");
                return false;
            }
            long availCount = sf.getAvailableBlocks();
            return !(availCount > 0);
        }

        return true;
    }

    private static File sFile;

    /**
     * adjust the storage file
     * @return
     */
    public File getExternalStorageDirectory() {
        String path = StorageManagerEx.getDefaultPath();
        final File file = getDirectory(path, Environment
                .getExternalStorageDirectory().toString());
        Log.i(TAG, "[getExternalStorageDirectory]file.path : "
                        + file.getPath());
        sFile = file;
        return file;
    }

    public File getDirectory(String path, String defaultPath) {
        Log.i("getDirectory", "path : " + path);
        return path == null ? new File(defaultPath) : new File(path);
    }

    public static String getExternalStorageState() {
        try {
            IMountService mountService = IMountService.Stub
                    .asInterface(ServiceManager.getService("mount"));
            Log.i(TAG, "[getExternalStorageState] mFile : " + sFile);
            return mountService.getVolumeState(sFile.toString());
        } catch (Exception rex) {
            return Environment.MEDIA_REMOVED;
        }
    }

    private class CancelListener implements DialogInterface,
            DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
            DialogInterface.OnKeyListener {
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }

        public void onCancel(DialogInterface dialog) {
            finish();
        }

        public void cancel() {
        }

        public void dismiss() {
            finish();
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            finish();
            return false;
        }
    }

    private CancelListener mCancelListener = new CancelListener();

    private class SearchContactThread extends Thread implements
            OnCancelListener, OnClickListener {
        // To avoid recursive link.
        private class CanceledException extends Exception {

        }

        public SearchContactThread() {

        }

        @Override
        public void run() {
            String type = mIntent.getType();
            mDataUri = (Uri) mIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            Log.i(TAG, "mDataUri is " + mDataUri);
            Log.i(TAG, "type is " + type);
            if (mDataUri != null && type != null) {
                shareViaSDCard(mLookUpUris);
            }
        }

        public void onCancel(DialogInterface dialog) {
            finish();
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            }
        }
    }

}