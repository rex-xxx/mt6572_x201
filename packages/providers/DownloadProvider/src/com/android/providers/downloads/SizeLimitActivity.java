/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.downloads;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Downloads;
import android.text.format.Formatter;
import android.util.Log;

import com.mediatek.downloadmanager.ext.Extensions;
import com.mediatek.downloadmanager.ext.IDownloadProviderFeatureEx;
import com.mediatek.xlog.Xlog;

import java.util.LinkedList;
import java.util.Queue;




/**
 * Activity to show dialogs to the user when a download exceeds a limit on download sizes for
 * mobile networks.  This activity gets started by the background download service when a download's
 * size is discovered to be exceeded one of these thresholds.
 */
public class SizeLimitActivity extends Activity
        implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
    private IDownloadProviderFeatureEx mDownloadProviderFeatureEx;
    private Dialog mDialog;
    private Queue<Intent> mDownloadsToShow = new LinkedList<Intent>();
    private Uri mCurrentUri;
    private Intent mCurrentIntent;

    /// M: onclick call back once. fix issue: 426558 @{
    private boolean onClickCalled;
    /// @}
    public static final int NOTIFY_PAUSE_DUE_TO_SIZE = 0;
    public static final int NOTIFY_FILE_ALREADY_EXIST = 1;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            mDownloadsToShow.add(intent);
            setIntent(null);
            showNextDialog();
        }
        if (mDialog != null && !mDialog.isShowing()) {
            mDialog.show();
        }
    }

    private void showNextDialog() {
        if (mDialog != null) {
            return;
        }

        if (mDownloadsToShow.isEmpty()) {
            finish();
            return;
        }

        mCurrentIntent = mDownloadsToShow.poll();
        mCurrentUri = mCurrentIntent.getData();
        Cursor cursor = getContentResolver().query(mCurrentUri, null, null, null, null);
        try {
            if (!cursor.moveToFirst()) {
                Log.e(Constants.TAG, "Empty cursor for URI " + mCurrentUri);
                dialogClosed();
                return;
            }
            showDialog(cursor);
        } finally {
            cursor.close();
        }
    }

    private void showDialog(Cursor cursor) {
        int size = cursor.getInt(cursor.getColumnIndexOrThrow(Downloads.Impl.COLUMN_TOTAL_BYTES));
        
        /// M: Operator Feature get the reason for showing dialog. @{
        mDownloadProviderFeatureEx = Extensions.getDefault(this);
        int showDialogReason = mDownloadProviderFeatureEx.getShowDialogReasonInt(mCurrentIntent);
        /// @}
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        
        if (showDialogReason == NOTIFY_PAUSE_DUE_TO_SIZE) {
        String sizeString = Formatter.formatFileSize(this, size);
        String queueText = getString(R.string.button_queue_for_wifi);
        boolean isWifiRequired =
            mCurrentIntent.getExtras().getBoolean(DownloadInfo.EXTRA_IS_WIFI_REQUIRED);

            
        if (isWifiRequired) {
            builder.setTitle(R.string.wifi_required_title)
                    .setMessage(getString(R.string.wifi_required_body, sizeString, queueText))
                    .setPositiveButton(R.string.button_queue_for_wifi, this)
                    .setNegativeButton(R.string.button_cancel_download, this);
        } else {
            builder.setTitle(R.string.wifi_recommended_title)
                    .setMessage(getString(R.string.wifi_recommended_body, sizeString, queueText))
                    .setPositiveButton(R.string.button_start_now, this)
                    .setNegativeButton(R.string.button_queue_for_wifi, this);
        }
        } else {
            /// M: This for support CU customization @{
            mDownloadProviderFeatureEx.showFileAlreadyExistDialog(builder, getText(R.string.app_label), 
                    getText(R.string.download_file_already_exist), getString(R.string.ok), getString(R.string.cancel), this);
            /// @}
        }
        
        /// M: onclick call back once. fix issue: 426558 @{
        onClickCalled = false;
        /// @}

        mDialog = builder.setOnCancelListener(this).show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        /// M: Operator Feature get the reason for showing dialog. @{
        mDownloadProviderFeatureEx = Extensions.getDefault(this);
        int showDialogReason = mDownloadProviderFeatureEx.getShowDialogReasonInt(mCurrentIntent);
        /// @}
        if (showDialogReason == NOTIFY_PAUSE_DUE_TO_SIZE) {
        dialogClosed();
        } else {
            Xlog.d(Constants.DL_ENHANCE, "SizeLimitActivity.onCancel:FileAlreadyExist," +
                    " Delete download uri is" +
                    mCurrentUri);
            getContentResolver().delete(mCurrentUri, null, null);
            dialogClosed();
        }
    }

    private void dialogClosed() {
        mDialog = null;
        mCurrentUri = null;
        showNextDialog();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        /// M: onclick call back once. fix issue: 426558 @{
        if (onClickCalled) {
            return;
        } else {
            onClickCalled = true;
        }
        /// @}

        int showDialogReason = mCurrentIntent.getExtras().getInt(DownloadInfo.SHOW_DIALOG_REASON);
        if (showDialogReason == NOTIFY_PAUSE_DUE_TO_SIZE) {
        boolean isRequired =
                mCurrentIntent.getExtras().getBoolean(DownloadInfo.EXTRA_IS_WIFI_REQUIRED);
        if (isRequired && which == AlertDialog.BUTTON_NEGATIVE) {
            getContentResolver().delete(mCurrentUri, null, null);
        } else if (!isRequired && which == AlertDialog.BUTTON_POSITIVE) {
            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT, true);
            getContentResolver().update(mCurrentUri, values , null, null);
        }
        } else {
            /// M: add response for buttons @{
            if (which == AlertDialog.BUTTON_NEGATIVE) {
                int row = getContentResolver().delete(mCurrentUri, null, null);
                Xlog.d(Constants.DL_ENHANCE, "SizeLimitActivity.onClick:FileAlreadyExist," +
                        " Delete download row is" + row + " Delete download uri is " + 
                        mCurrentUri);
            } else if (which == AlertDialog.BUTTON_POSITIVE) {
                ContentValues values = new ContentValues();
                values.put(Downloads.Impl.CONTINUE_DOWNLOAD_WITH_SAME_FILENAME, 1);
                values.put(Downloads.Impl.COLUMN_STATUS,
                        Downloads.Impl.STATUS_PENDING);
                getContentResolver().update(mCurrentUri, values,
                        null, null);
                
                Cursor cursor = null;
                String fullFileName = mCurrentIntent.getExtras().getString(DownloadInfo.FULL_FILE_NAME);
                //String whereClause = Downloads.Impl._DATA + " == '" + fullFileName + "'";
		String whereClause = Downloads.Impl._DATA + " = ?";                  

                Xlog.i(Constants.DL_ENHANCE, "SizeLimitActivity.onClick:FileAlreadyExist," +
                        " continue download uri is" + mCurrentUri + 
                        " full file name is " + fullFileName);
                try {
                    cursor = getContentResolver().query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                            new String[] {Downloads.Impl._ID}, 
                            whereClause, new String[] {fullFileName},
                            Downloads.Impl.COLUMN_LAST_MODIFICATION + " DESC");
                    if (cursor != null && cursor.moveToFirst()) {
                        long downloadID = cursor.getLong(cursor.
                                getColumnIndexOrThrow(Downloads.Impl._ID));
                        int row = getContentResolver().delete(ContentUris.withAppendedId(
                                Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                                downloadID), null, null);
                        Xlog.d(Constants.DL_ENHANCE, "SizeLimitActivity: downloadID is: " + downloadID + 
                                " delete row is" + row + 
                                " full file name is: " + fullFileName);
                    }     
                } catch (IllegalStateException e) {
                    Xlog.e(Constants.DL_ENHANCE, "SizeLimitActivity delete exist error");
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            /// @}
        }
        dialogClosed();
    }
}
