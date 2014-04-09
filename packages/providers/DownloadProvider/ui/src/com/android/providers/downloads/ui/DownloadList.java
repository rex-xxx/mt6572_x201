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

package com.android.providers.downloads.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
//import android.drm.DrmManagerClient;
//import android.drm.DrmStore;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.Downloads;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.android.providers.downloads.Constants;
import com.android.providers.downloads.OpenHelper;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.downloadmanager.ext.Extensions;
import com.mediatek.downloadmanager.ext.IDownloadProviderFeatureEx;
import com.mediatek.xlog.Xlog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 *  View showing a list of all downloads the Download Manager knows about.
 */
public class DownloadList extends Activity {
    static final String LOG_TAG = "DownloadList";
    
    //add this string wth OMA DL log for debug
    private static IDownloadProviderFeatureEx sDownloadProviderFeatureEx;
    private static final String LOG_OMA_DL = "DownloadManager/OMA";
    private static final String XTAG_DRM = "DownloadManager/DRM";
    private static final String XTAG_ENHANCE = "DownloadManager/Enhance";

    private ExpandableListView mDateOrderedListView;
    private ListView mSizeOrderedListView;
    private View mEmptyView;

    private DownloadManager mDownloadManager;
    private Cursor mDateSortedCursor;
    private DateSortedDownloadAdapter mDateSortedAdapter;
    private Cursor mSizeSortedCursor;
    private DownloadAdapter mSizeSortedAdapter;
    private ActionMode mActionMode;
    private MyContentObserver mContentObserver = new MyContentObserver();
    private MyDataSetObserver mDataSetObserver = new MyDataSetObserver();

    private int mStatusColumnId;
    private int mIdColumnId;
    private int mLocalUriColumnId;
    private int mMediaTypeColumnId;
    private int mReasonColumndId;

    // TODO this shouldn't be necessary
    private final Map<Long, SelectionObjAttrs> mSelectedIds =
            new HashMap<Long, SelectionObjAttrs>();
    private static class SelectionObjAttrs {
        private String mFileName;
        private String mMimeType;
        SelectionObjAttrs(String fileName, String mimeType) {
            mFileName = fileName;
            mMimeType = mimeType;
        }
        String getFileName() {
            return mFileName;
        }
        String getMimeType() {
            return mMimeType;
        }
    }
    private ListView mCurrentView;
    private Cursor mCurrentCursor;
    private boolean mCurrentViewIsExpandableListView = false;
    private boolean mIsSortedBySize = false;

    /**
     * We keep track of when a dialog is being displayed for a pending download, because if that
     * download starts running, we want to immediately hide the dialog.
     */
    private Long mQueuedDownloadId = null;
    private AlertDialog mQueuedDialog;
    String mSelectedCountFormat;

    private Button mSortOption;

    /// M: These variable is used to store COLUMN_ID when download dd file @{
    private int mDDFileCursor;
    private AlertDialog mDialog;
    private AlertDialog mCurrentDialog;
    private Queue<AlertDialog> mDownloadsToShow = new LinkedList<AlertDialog>();
    /// @}
    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            handleDownloadsChanged();
            
            /// M: Add this function to support OMA download
            handleOmaDownload();
            
            //if (FeatureOption.MTK_DRM_APP) {
                //handleDRMRight();
            //}
        }
    }
    
    /**
     *  M: Add this to handle MTK DRM
     */
    private void handleDRMRight() {
        String selection = "(" + Downloads.Impl.COLUMN_MIME_TYPE + " == "
                + OmaDrmStore.DrmObjectMime.MIME_RIGHTS_WBXML + ") OR ("
                + Downloads.Impl.COLUMN_MIME_TYPE + " == " + OmaDrmStore.DrmObjectMime.MIME_RIGHTS_XML
                + ")";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                    new String[] {
                            Downloads.Impl._ID, Downloads.Impl.COLUMN_STATUS
                    },
                    "mimetype = ? OR mimetype = ?",
                    new String[] {
                            OmaDrmStore.DrmObjectMime.MIME_RIGHTS_WBXML,
                            OmaDrmStore.DrmObjectMime.MIME_RIGHTS_XML
                    }, null);
            Xlog.i(XTAG_DRM, "handleDRMRight: before query");
            if (cursor != null) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    Xlog.v(XTAG_DRM, "handleDRMRight: cursor is not null");
                    // Has DRM rights need to delete
                    long downloadID = cursor.getLong(cursor
                            .getColumnIndexOrThrow(Downloads.Impl._ID));
                    int downloadStatus = cursor.getInt(cursor
                            .getColumnIndexOrThrow(Downloads.Impl.COLUMN_STATUS));
                    if (Downloads.Impl.isStatusCompleted(downloadStatus)) {
                        Xlog.v(XTAG_DRM, "handleDRMRight: DRM right is complete and need delete");
                        deleteDownload(downloadID);
                    }
                }
            }
        } catch (IllegalStateException e) {
            Xlog.e(XTAG_DRM, "handleDRMRight: query encounter exception");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    
    
    /**
     *  M: This class is contain the info of OMA DL
     */
    private class DownloadInfo {
        public String mName;
        public String mVendor;
        public String mType;
        public String mObjectUrl;
        public String mNextUrl;
        public String mInstallNotifyUrl;
        public String mDescription;
        public boolean mSupportByDevice;
        public int mSize;
        
        
        DownloadInfo(String name, String vendor, String type, String objectUrl,
                String nextUrl, String installNotifyUrl, String description, 
                int size, boolean isSupportByDevice) {
            mName = name;
            mVendor = vendor;
            mType = type;
            mObjectUrl = objectUrl;
            mNextUrl = nextUrl;
            mInstallNotifyUrl = installNotifyUrl;
            mDescription = description;
            mSize = size;
            mSupportByDevice = isSupportByDevice;
        }
    }
    
    /**
     * M: This function is used to handle OMA DL. Include .dd file and download MediaObject
     */
    private void handleOmaDownload() {
        String whereClause = null;
        if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
            whereClause =  "(" + Downloads.Impl.COLUMN_STATUS + " == '" + Downloads.Impl.STATUS_NEED_HTTP_AUTH + "') OR (" + 
                            Downloads.Impl.COLUMN_STATUS + " == '" + 
                            Downloads.Impl.OMADL_STATUS_DOWNLOAD_COMPLETELY + "' AND " +
                            Downloads.Impl.COLUMN_VISIBILITY + " != '" + Downloads.Impl.VISIBILITY_HIDDEN + "'" + " AND " +
                            Downloads.Impl.COLUMN_DELETED + " != '1' AND " +
                            Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG + " == '" + "1' AND (" + // '1' means it is OMA DL
                            Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS + " == '" + 
                            Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS + "' OR " + 
                            Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS + " == '" +
                            Downloads.Impl.OMADL_STATUS_HAS_NEXT_URL + "'))";
            // Note: OMA_Download_Status '201': .dd file download and parsed success. 
            // OMA_Download_Status '203': Download OMA Download media object success and it has next url.
        } else {
            whereClause = Downloads.Impl.COLUMN_STATUS + " == '" + Downloads.Impl.STATUS_NEED_HTTP_AUTH + "'";
        }

        Cursor cursor = null;
        
        try {
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
            cursor = getContentResolver().query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                    new String[] { Downloads.Impl._ID,
                    Downloads.Impl.COLUMN_STATUS,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_NAME,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_VENDOR,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_SIZE, 
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_TYPE,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_DESCRIPTION,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_OBJECT_URL,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_NEXT_URL,
                    Downloads.Impl.COLUMN_OMA_DOWNLOAD_INSTALL_NOTIFY_URL}, whereClause, null,
                    Downloads.Impl.COLUMN_LAST_MODIFICATION + " DESC");
            } else {
                cursor = getContentResolver().query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                        new String[] { Downloads.Impl._ID,
                        Downloads.Impl.COLUMN_STATUS}, whereClause, null,
                        Downloads.Impl.COLUMN_LAST_MODIFICATION + " DESC");
            }
            if (cursor != null) {
                showAlertDialog(cursor);
            }
            
        } catch (IllegalStateException e) {
            Xlog.e(LOG_OMA_DL, "DownloadList:handleOmaDownload()", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
    }

    /**
     *  Delete dd file of OMA download
     */
    private void deleteOMADownloadDDFile(long downloadID) {
        Xlog.d(LOG_OMA_DL, "deleteOMADownload(): downloadID is: " + downloadID);
        mDownloadManager.markRowDeleted(downloadID);
        NotificationManager mNotifManager = (NotificationManager) this.getApplicationContext().getSystemService(
                Context.NOTIFICATION_SERVICE);
        mNotifManager.cancel((int)downloadID);
        Xlog.d(LOG_OMA_DL, "deleteOMADownload(): cancel notification, id : " + downloadID); 
    }
    
    /**
     *  M: Pop up the alert dialog. Show the OMA DL info or Authenticate info 
     */
    private void showAlertDialog(Cursor cursor) {
        //if (cursor.moveToFirst()) {
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            StringBuilder message = new StringBuilder();
            StringBuilder title = new StringBuilder();
            int showReason = 0;
            ContentValues values = new ContentValues();
            int omaDownloadID = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
            int downloadStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Downloads.Impl.COLUMN_STATUS));
            if (downloadStatus == Downloads.Impl.STATUS_NEED_HTTP_AUTH) {
                title.append(getText(R.string.authenticate_dialog_title));
                showReason = downloadStatus;
                Xlog.d(LOG_OMA_DL, "DownloadList: showAlertDialog(): Show Alert dialog reason is " 
                        + showReason);
                
                values.put(Downloads.Impl.COLUMN_STATUS, 
                        Downloads.Impl.OMADL_STATUS_ERROR_ALERTDIALOG_SHOWED);
                int row = getContentResolver().update(
                        ContentUris.withAppendedId(
                                Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                                omaDownloadID), values, null, null);
                popAlertDialog(omaDownloadID, null, title.toString(), message.toString(), showReason);
            } else {
                if (!Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
                    return;
                }
                Xlog.d(LOG_OMA_DL, "DownloadList: showAlertDialog(): Show Alert dialog reason is " 
                        + showReason);
                showReason = cursor.getInt(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_NAME));
                String vendor = cursor.getString(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_VENDOR));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_TYPE));
                String objectUrl = cursor.getString(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_OBJECT_URL));
                String nextUrl = cursor.getString(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_NEXT_URL));
                String notifyUrl = cursor.getString(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_INSTALL_NOTIFY_URL));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_DESCRIPTION));
                int size = cursor.getInt(cursor.getColumnIndexOrThrow(
                        Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_SIZE));
                
                boolean isSupportByDevice = true;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                PackageManager pm = getPackageManager();
                intent.setDataAndType(Uri.fromParts("file", "", null), type);
                ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (ri == null) {
                    isSupportByDevice = false;
                }
                DownloadInfo downloadInfo = new DownloadInfo(name, vendor, type, 
                        objectUrl, nextUrl, notifyUrl, description, size, isSupportByDevice);
                
                if (showReason == Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS) {
                    title.append(getText(R.string.confirm_oma_download_title));
                    
                    if (name != null) {
                        message.append(getText(R.string.oma_download_name) + " " + name + "\n");
                    }
                    if (vendor != null) {
                        message.append(getText(R.string.oma_download_vendor) + " " + vendor + "\n");
                    }
                    if (type != null) {
                        message.append(getText(R.string.oma_download_type) + " " + type + "\n");
                    }
                    message.append(getText(R.string.oma_download_size) + " " + size + "\n");
                    if (description != null) {
                        message.append(getText(R.string.oma_download_description) 
                                + " " + description + "\n");
                    }
                    
                    if (!isSupportByDevice) {
                        message.append("\n" + getText(R.string.oma_download_content_not_supported));
                    }
                    
                    
                } else if (showReason == Downloads.Impl.OMADL_STATUS_HAS_NEXT_URL) {
                    //title.append("OMA Download");
                    message.append(getText(R.string.confirm_oma_download_next_url) 
                            + "\n\n" + downloadInfo.mNextUrl);
                } 
                
                //update the status so that this download item can not be queried and show alert dialog.
                values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                        Downloads.Impl.OMADL_STATUS_ERROR_ALERTDIALOG_SHOWED); 
                int row = getContentResolver().update(
                        ContentUris.withAppendedId(
                                Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                                omaDownloadID), values, null, null);
                popAlertDialog(omaDownloadID, downloadInfo, title.toString(), 
                        message.toString(), showReason);
            }
        }
    }
    
    private void popAlertDialog(final int downloadID,
            final DownloadInfo downloadInfo, final String title,
            final String message, final int showReason) {
        
        final View v =  LayoutInflater.from(this).inflate(R.layout.http_authentication, null);
        String positiveString = null;
        if (showReason == Downloads.Impl.STATUS_NEED_HTTP_AUTH && downloadInfo == null) {
            positiveString = getString(R.string.action);
        } else {
            positiveString = getString(R.string.ok);
        }
        mDialog = new AlertDialog.Builder(DownloadList.this)
                        .setTitle(title)
                        .setPositiveButton(positiveString, 
                                getOmaDownloadPositiveClickHandler(downloadInfo, downloadID, showReason, v))
                        .setNegativeButton(R.string.cancel, 
                                getOmaDownloadCancelClickHandler(downloadInfo, downloadID, showReason))
                        .setOnCancelListener(getOmaDownloadBackKeyClickHanlder(downloadInfo, downloadID, showReason))
                        .create();
        
        if (showReason == Downloads.Impl.STATUS_NEED_HTTP_AUTH && downloadInfo == null) {
            mDialog.setView(v);
            mDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            v.findViewById(R.id.username_edit).requestFocus();
        } else {
            mDialog.setMessage(message);
        }
        
        mDownloadsToShow.add(mDialog);
        Xlog.d(LOG_OMA_DL, "DownloadList: Popup Alert dialog is: **" + mDialog + "**");
        showNextDialog();
    }
    
    private void showNextDialog() {
        if (mCurrentDialog != null) {
            return;
        }
        
        if (mDownloadsToShow != null && !mDownloadsToShow.isEmpty()) {
            synchronized (mDownloadsToShow) {
                mCurrentDialog = mDownloadsToShow.poll();
                if (mCurrentDialog != null && !mCurrentDialog.isShowing()) {
                    Xlog.d(LOG_OMA_DL, "DownloadList: Current dialog is: **" + mCurrentDialog + "**");
                    mCurrentDialog.show();
                }
            }
        }
    }
    
    
    
    /**
     *  M: Click "OK" to download the media object
     */
    // Define for Authenticate, will move to Framework
    //public static final String Downloads_Impl_COLUMN_USERNAME = "username";
    //public static final String Downloads_Impl_COLUMN_PASSWORD = "password";
    
    private DialogInterface.OnClickListener getOmaDownloadPositiveClickHandler(final DownloadInfo downloadInfo,
            final int downloadID, final int showReason, final View v) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Xlog.i(LOG_OMA_DL, "DownloadList: getOmaDownloadPositiveClickHandler");
                if (showReason == Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS) {
                    // Insert database to download media object. 
                    // We don't use DownloadManager, because of it didn't handle OMA_Download column
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_URI, downloadInfo.mObjectUrl);
                    Xlog.d(LOG_OMA_DL, "DownloadList:getOmaDownloadClickHandler(): onClick(): object url is" 
                            + downloadInfo.mObjectUrl + "mime Type is: " + downloadInfo.mType);
                    values.put(Downloads.Impl.COLUMN_NOTIFICATION_PACKAGE, getPackageName());
                    values.put(Downloads.Impl.COLUMN_NOTIFICATION_CLASS, 
                            OMADLOpenDownloadReceiver.class.getCanonicalName());
                    values.put(Downloads.Impl.COLUMN_VISIBILITY,
                            Downloads.Impl.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    values.put(Downloads.Impl.COLUMN_MIME_TYPE, downloadInfo.mType);
                    values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG, 1);// 1 means it is a OMA_DL
                    values.put(Downloads.Impl.COLUMN_DESTINATION, 
                            Downloads.Impl.DESTINATION_EXTERNAL);
                    if (downloadInfo.mNextUrl != null) {
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_NEXT_URL, 
                                downloadInfo.mNextUrl); // Insert the next url
                        Xlog.d(LOG_OMA_DL, "DownloadList:getOmaDownloadClickHandler(): onClick():" +
                                " next url is" + downloadInfo.mNextUrl);
                    }
                    if (downloadInfo.mInstallNotifyUrl != null) {
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_INSTALL_NOTIFY_URL, 
                                downloadInfo.mInstallNotifyUrl);
                        Xlog.d(LOG_OMA_DL, "DownloadList:getOmaDownloadClickHandler(): onClick():" +
                                " install Notify url is" + downloadInfo.mInstallNotifyUrl);
                    }
                    
                    ///M: Add user agent string to oma download. @{
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(
                                ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, downloadID), 
                                new String[] {Downloads.Impl.DOWNLOAD_PATH_SELECTED_FROM_FILEMANAGER, Downloads.Impl.COLUMN_USER_AGENT}, 
                                null, null, null);
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            String userAgentString = cursor.getString(cursor.getColumnIndex(Downloads.Impl.COLUMN_USER_AGENT));
                            values.put(Downloads.Impl.COLUMN_USER_AGENT, userAgentString);
                            Xlog.d(LOG_OMA_DL, "DownloadList:getOmaDownloadClickHandler(): onClick():" +
                                    " userAgent is " + userAgentString);
                            
                            /// M: Operator Feature get download path from file manager APP. @{
                            sDownloadProviderFeatureEx = Extensions.getDefault(DownloadList.this);
                            if (sDownloadProviderFeatureEx.shouldSetDownloadPathSelectFileMager()) {
                                String selectedPath = cursor.getString(
                                        cursor.getColumnIndex(
                                                Downloads.Impl.DOWNLOAD_PATH_SELECTED_FROM_FILEMANAGER));
                                values.put(Downloads.Impl.DOWNLOAD_PATH_SELECTED_FROM_FILEMANAGER, selectedPath);
                                Xlog.d(LOG_OMA_DL, "DownloadList:getOmaDownloadClickHandler(): onClick():" + 
                                        "OP01 implement, selectedPath is " + selectedPath);
                            }
                            ///@}
                        }
                    } catch (IllegalStateException e) {
                        Xlog.e(LOG_OMA_DL, "Query selected download path failed");
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    ///@}
                    
                    try {
                        WebAddress webAddress = new WebAddress(downloadInfo.mObjectUrl);
                        values.put(Downloads.Impl.COLUMN_DESCRIPTION, webAddress.getHost());
                        getContentResolver().insert(Downloads.Impl.CONTENT_URI, values);
                    } catch (ParseException e) {
                        Xlog.e(LOG_OMA_DL, "Exception trying to parse url:" + downloadInfo.mObjectUrl);
                        getContentResolver().insert(Downloads.Impl.CONTENT_URI, values);
                        mCurrentDialog = null;
                        showNextDialog();
                    }
                    
                    // Delete the .dd file
                    ContentValues ddFilevalues = new ContentValues();
                    ddFilevalues.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                            Downloads.Impl.OMADL_STATUS_ERROR_USER_DOWNLOAD_MEDIA_OBJECT); 
                    int row = getContentResolver().update(ContentUris.withAppendedId(
                                    Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                                    downloadID), ddFilevalues, null, null);
                    //mDownloadManager.markRowDeleted(downloadID);
                    deleteOMADownloadDDFile(downloadID);
                    // If button is shown, dismiss it.
                    // clearSelection();
                    
                    // If we follow the Google dafault delete flow, the dd file no need to deleted.
                    // So, call deleteDownload(downloadID) function.
                    // deleteDownload(downloadID);
                    
                } else if (showReason == Downloads.Impl.OMADL_STATUS_HAS_NEXT_URL) {
                    if (downloadInfo.mNextUrl != null) {
                        Uri uri = Uri.parse(downloadInfo.mNextUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(intent);
                    }
                } else if (showReason == Downloads.Impl.STATUS_NEED_HTTP_AUTH && v != null) {
                    String nm = ((EditText) v
                            .findViewById(R.id.username_edit))
                            .getText().toString();
                    String pw = ((EditText) v
                            .findViewById(R.id.password_edit))
                            .getText().toString();
                    Xlog.d(XTAG_ENHANCE, "DownloadList:getOmaDownloadClickHandler:onClick():" +
                            "Autenticate UserName is " + nm + " Password is " + pw);
                    
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_USERNAME, nm);
                    values.put(Downloads.Impl.COLUMN_PASSWORD, pw);
                    values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_PENDING);
                    int row = getContentResolver().update(
                            ContentUris.withAppendedId(
                                    Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                                    downloadID), values, null, null);
                }
                
                mCurrentDialog = null;
                showNextDialog();
            }
        };
    }
    
    /**
     *  M: Click "Cancel" to cancel download media object
     */
    private DialogInterface.OnClickListener getOmaDownloadCancelClickHandler(final DownloadInfo downloadInfo,
            final int downloadID,  final int showReason) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (showReason == Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS) {
                    Xlog.i(LOG_OMA_DL, "DownloadList:getOmaDownloadClickHandler(): user click Cancel");
                    // Delete the .dd file
                    ContentValues values = new ContentValues();
                    if (!downloadInfo.mSupportByDevice) {
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                                Downloads.Impl.OMADL_STATUS_ERROR_NON_ACCEPTABLE_CONTENT);
                    } else {
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                                Downloads.Impl.OMADL_STATUS_ERROR_USER_CANCELLED);
                    }
                    int row = getContentResolver().update(ContentUris.withAppendedId(
                                    Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                                    downloadID), values, null, null);
                    //mDownloadManager.markRowDeleted(downloadID);
                    deleteOMADownloadDDFile(downloadID);
                    // If button is shown, dismiss it.
                    // clearSelection();
                    
                    // If we follow the Google dafault delete flow, the dd file no need to deleted.
                    // So, call deleteDownload(downloadID) function.
                    //deleteDownload(downloadID);
                    
                } else if (showReason == Downloads.Impl.STATUS_NEED_HTTP_AUTH) {
                    Xlog.i(XTAG_ENHANCE, "DownloadList:getOmaDownloadClickHandler():" +
                            " Authencticate Download:user click Cancel");
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_UNKNOWN_ERROR);
                    int row = getContentResolver().update(ContentUris.withAppendedId(
                            Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                            downloadID), values, null, null);
                }
                
                mCurrentDialog = null;
                showNextDialog();
            }
        };
    }
    
    /**
     *  M: Click "Back key" to cancel download media object
     */
    private DialogInterface.OnCancelListener getOmaDownloadBackKeyClickHanlder(final DownloadInfo downloadInfo,
            final int downloadID, final int showReason) {
        return new DialogInterface.OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface dialog) {
                if (showReason == Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS) {
                    Xlog.i(LOG_OMA_DL, "DownloadList:getOmaDownloadClickHandler(): user click Back key");
                    // DeleteDownloads
                    ContentValues values = new ContentValues();
                    if (!downloadInfo.mSupportByDevice) {
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                                Downloads.Impl.OMADL_STATUS_ERROR_NON_ACCEPTABLE_CONTENT);
                    } else {
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                                Downloads.Impl.OMADL_STATUS_ERROR_USER_CANCELLED);
                    }
                    int row = getContentResolver().update(ContentUris.withAppendedId(
                                    Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                                    downloadID), values, null, null);
                    //mDownloadManager.markRowDeleted(downloadID);
                    deleteOMADownloadDDFile(downloadID);
                    // If button is shown, dismiss it.
                    // clearSelection();
                    
                    // If we follow the Google dafault delete flow, the dd file no need to deleted.
                    // So, call deleteDownload(downloadID) function.
                    //deleteDownload(downloadID);
                } else if (showReason == Downloads.Impl.STATUS_NEED_HTTP_AUTH) {
                    Xlog.i(XTAG_ENHANCE, "DownloadList:getOmaDownloadClickHandler(): " +
                            "Authencticate Download:user click Cancel");
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_UNKNOWN_ERROR);
                    int row = getContentResolver().update(ContentUris.withAppendedId(
                            Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                            downloadID), values, null, null);
        }
                mCurrentDialog = null;
                showNextDialog();
            }
        };
    }

    
    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            // ignore change notification if there are selections
            if (mSelectedIds.size() > 0) {
                return;
            }
            // may need to switch to or from the empty view
            chooseListToShow();
            ensureSomeGroupIsExpanded();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        Xlog.d(XTAG_ENHANCE,"Downloadlist:onCreate() called");
        super.onCreate(icicle);
        setFinishOnTouchOutside(true);
        setupViews();

        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadManager.setAccessAllDownloads(true);
        DownloadManager.Query baseQuery = new DownloadManager.Query()
                .setOnlyIncludeVisibleInDownloadsUi(true);
        //TODO don't do both queries - do them as needed
        mDateSortedCursor = mDownloadManager.query(baseQuery);
        mSizeSortedCursor = mDownloadManager.query(baseQuery
                                                  .orderBy(DownloadManager.COLUMN_TOTAL_SIZE_BYTES,
                                                          DownloadManager.Query.ORDER_DESCENDING));

        // only attach everything to the listbox if we can access the download database. Otherwise,
        // just show it empty
        if (haveCursors()) {
            startManagingCursor(mDateSortedCursor);
            startManagingCursor(mSizeSortedCursor);

            mStatusColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
            mIdColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
            mLocalUriColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
            mMediaTypeColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE);
            mReasonColumndId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON);

            mDateSortedAdapter = new DateSortedDownloadAdapter(this, mDateSortedCursor);
            mDateOrderedListView.setAdapter(mDateSortedAdapter);
            mSizeSortedAdapter = new DownloadAdapter(this, mSizeSortedCursor);
            mSizeOrderedListView.setAdapter(mSizeSortedAdapter);

            ensureSomeGroupIsExpanded();
        }

        // did the caller want  to display the data sorted by size?
        Bundle extras = getIntent().getExtras();
        if (extras != null &&
                extras.getBoolean(DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, false)) {
            mIsSortedBySize = true;
        }
        mSortOption = (Button) findViewById(R.id.sort_button);
        mSortOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // flip the view
                mIsSortedBySize = !mIsSortedBySize;
                // clear all selections
                mSelectedIds.clear();
                chooseListToShow();
            }
        });

        chooseListToShow();
        mSelectedCountFormat = getString(R.string.selected_count);
    }

    /**
     * If no group is expanded in the date-sorted list, expand the first one.
     */
    private void ensureSomeGroupIsExpanded() {
        mDateOrderedListView.post(new Runnable() {
            public void run() {
                if (mDateSortedAdapter.getGroupCount() == 0) {
                    return;
                }
                for (int group = 0; group < mDateSortedAdapter.getGroupCount(); group++) {
                    if (mDateOrderedListView.isGroupExpanded(group)) {
                        return;
                    }
                }
                mDateOrderedListView.expandGroup(0);
            }
        });
    }

    private void setupViews() {
        setContentView(R.layout.download_list);
        ModeCallback modeCallback = new ModeCallback(this);

        //TODO don't create both views. create only the one needed.
        mDateOrderedListView = (ExpandableListView) findViewById(R.id.date_ordered_list);
        mDateOrderedListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mDateOrderedListView.setMultiChoiceModeListener(modeCallback);
        mDateOrderedListView.setOnChildClickListener(new OnChildClickListener() {
            // called when a child is clicked on (this is NOT the checkbox click)
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                if (!(v instanceof DownloadItem)) {
                    // can this even happen?
                    return false;
                }
                if (mSelectedIds.size() > 0) {
                    ((DownloadItem)v).setChecked(true);
                } else {
                    mDateSortedAdapter.moveCursorToChildPosition(groupPosition, childPosition);
                    handleItemClick(mDateSortedCursor);
                }
                return true;
            }
        });
        mSizeOrderedListView = (ListView) findViewById(R.id.size_ordered_list);
        mSizeOrderedListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mSizeOrderedListView.setMultiChoiceModeListener(modeCallback);
        mSizeOrderedListView.setOnItemClickListener(new OnItemClickListener() {
            // handle a click from the size-sorted list. (this is NOT the checkbox click)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSizeSortedCursor.moveToPosition(position);
                handleItemClick(mSizeSortedCursor);
            }
        });
        mEmptyView = findViewById(R.id.empty);
    }

    private static class ModeCallback implements MultiChoiceModeListener {
        private final DownloadList mDownloadList;

        public ModeCallback(DownloadList downloadList) {
            mDownloadList = downloadList;
        }

        @Override public void onDestroyActionMode(ActionMode mode) {
            mDownloadList.mSelectedIds.clear();
            mDownloadList.mActionMode = null;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mDownloadList.haveCursors()) {
                final MenuInflater inflater = mDownloadList.getMenuInflater();
                inflater.inflate(R.menu.download_menu, menu);
            }
            mDownloadList.mActionMode = mode;
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (mDownloadList.mSelectedIds.size() == 0) {
                // nothing selected.
                return true;
            }
            switch (item.getItemId()) {
                case R.id.delete_download:
                    for (Long downloadId : mDownloadList.mSelectedIds.keySet()) {
                        mDownloadList.deleteDownload(downloadId);
                    }
                    // uncheck all checked items
                    ListView lv = mDownloadList.getCurrentView();
                    SparseBooleanArray checkedPositionList = lv.getCheckedItemPositions();
                    int checkedPositionListSize = checkedPositionList.size();
                    ArrayList<DownloadItem> sharedFiles = null;
                    for (int i = 0; i < checkedPositionListSize; i++) {
                        int position = checkedPositionList.keyAt(i);
                        if (checkedPositionList.get(position, false)) {
                            lv.setItemChecked(position, false);
                            onItemCheckedStateChanged(mode, position, 0, false);
                        }
                    }
                    mDownloadList.mSelectedIds.clear();
                    // update the subtitle
                    onItemCheckedStateChanged(mode, 1, 0, false);
                    break;
                case R.id.share_download:
                    mDownloadList.shareDownloadedFiles();
                    break;
            }
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            // ignore long clicks on groups
            if (mDownloadList.isCurrentViewExpandableListView()) {
                ExpandableListView ev = mDownloadList.getExpandableListView();
                long pos = ev.getExpandableListPosition(position);
                if (checked && (ExpandableListView.getPackedPositionType(pos) ==
                        ExpandableListView.PACKED_POSITION_TYPE_GROUP)) {
                    // ignore this click
                    ev.setItemChecked(position, false);
                    return;
                }
            }
            mDownloadList.setActionModeTitle(mode);
        }
    }

    void setActionModeTitle(ActionMode mode) {
        int numSelected = mSelectedIds.size();
        if (numSelected > 0) {
            mode.setTitle(String.format(mSelectedCountFormat, numSelected,
                    mCurrentCursor.getCount()));
        } else {
            mode.setTitle("");
        }
    }

    private boolean haveCursors() {
        return mDateSortedCursor != null && mSizeSortedCursor != null;
    }

    @Override
    protected void onResume() {
        Xlog.d(XTAG_ENHANCE,"Downloadlist:onResume() called");
        super.onResume();
        if (haveCursors()) {
            mDateSortedCursor.registerContentObserver(mContentObserver);
            mDateSortedCursor.registerDataSetObserver(mDataSetObserver);
            refresh();
        }
    }

    @Override
    protected void onPause() {
        Xlog.d(XTAG_ENHANCE,"Downloadlist:onPause() called");
        super.onPause();
        if (haveCursors()) {
            mDateSortedCursor.unregisterContentObserver(mContentObserver);
            mDateSortedCursor.unregisterDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    protected void onDestroy() {
        Xlog.d(XTAG_ENHANCE,"Downloadlist:onDestroy() called");
        super.onDestroy();
    }

    private static final String BUNDLE_SAVED_DOWNLOAD_IDS = "download_ids";
    private static final String BUNDLE_SAVED_FILENAMES = "filenames";
    private static final String BUNDLE_SAVED_MIMETYPES = "mimetypes";
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isSortedBySize", mIsSortedBySize);
        int len = mSelectedIds.size();
        if (len == 0) {
            return;
        }
        long[] selectedIds = new long[len];
        String[] fileNames = new String[len];
        String[] mimeTypes = new String[len];
        int i = 0;
        for (long id : mSelectedIds.keySet()) {
            selectedIds[i] = id;
            SelectionObjAttrs obj = mSelectedIds.get(id);
            fileNames[i] = obj.getFileName();
            mimeTypes[i] = obj.getMimeType();
            i++;
        }
        outState.putLongArray(BUNDLE_SAVED_DOWNLOAD_IDS, selectedIds);
        outState.putStringArray(BUNDLE_SAVED_FILENAMES, fileNames);
        outState.putStringArray(BUNDLE_SAVED_MIMETYPES, mimeTypes);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mIsSortedBySize = savedInstanceState.getBoolean("isSortedBySize");
        mSelectedIds.clear();
        long[] selectedIds = savedInstanceState.getLongArray(BUNDLE_SAVED_DOWNLOAD_IDS);
        String[] fileNames = savedInstanceState.getStringArray(BUNDLE_SAVED_FILENAMES);
        String[] mimeTypes = savedInstanceState.getStringArray(BUNDLE_SAVED_MIMETYPES);
        if (selectedIds != null && selectedIds.length > 0) {
            for (int i = 0; i < selectedIds.length; i++) {
                mSelectedIds.put(selectedIds[i], new SelectionObjAttrs(fileNames[i], mimeTypes[i]));
            }
        }
        chooseListToShow();
    }

    /**
     * Show the correct ListView and hide the other, or hide both and show the empty view.
     */
    private void chooseListToShow() {
        mDateOrderedListView.setVisibility(View.GONE);
        mSizeOrderedListView.setVisibility(View.GONE);

        if (mDateSortedCursor == null || mDateSortedCursor.getCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            ///M: when emptyView is visible, the sort button is invisible
            mSortOption.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            ///M: when emptyview is invisible, the sort button is visible
            mSortOption.setVisibility(View.VISIBLE);
            ListView lv = activeListView();
            lv.setVisibility(View.VISIBLE);
            lv.invalidateViews(); // ensure checkboxes get updated
        }
        // restore the ActionMode title if there are selections
        if (mActionMode != null) {
            setActionModeTitle(mActionMode);
        }
    }

    ListView getCurrentView() {
        return mCurrentView;
    }

    ExpandableListView getExpandableListView() {
        return mDateOrderedListView;
    }

    boolean isCurrentViewExpandableListView() {
        return mCurrentViewIsExpandableListView;
    }

    private ListView activeListView() {
        if (mIsSortedBySize) {
            mCurrentCursor = mSizeSortedCursor;
            mCurrentView = mSizeOrderedListView;
            setTitle(R.string.download_title_sorted_by_size);
            mSortOption.setText(R.string.button_sort_by_date);
            mCurrentViewIsExpandableListView = false;
        } else {
            mCurrentCursor = mDateSortedCursor;
            mCurrentView = mDateOrderedListView;
            setTitle(R.string.download_title_sorted_by_date);
            mSortOption.setText(R.string.button_sort_by_size);
            mCurrentViewIsExpandableListView = true;
        }
        if (mActionMode != null) {
            mActionMode.finish();
        }
        return mCurrentView;
    }

    /**
     * @return an OnClickListener to delete the given downloadId from the Download Manager
     */
    private DialogInterface.OnClickListener getDeleteClickHandler(final long downloadId) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteDownload(downloadId);
                /// M :Add to support Queue AlertDialog
                mCurrentDialog = null;
                showNextDialog();
            }
        };
    }

    /**
     * @return an OnClickListener to restart the given downloadId in the Download Manager
     */
    private DialogInterface.OnClickListener getRestartClickHandler(final long downloadId) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mDownloadManager.restartDownload(downloadId);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(DownloadList.this, R.string.download_error, Toast.LENGTH_LONG)
                            .show();
                }
                // / M: Add to support Queue AlertDialog
                mCurrentDialog = null;
                showNextDialog();
            }
        };
    }

    /**
     * Send an Intent to open the download currently pointed to by the given cursor.
     */
    private void openCurrentDownload(Cursor cursor) {
        final Uri localUri = Uri.parse(cursor.getString(mLocalUriColumnId));
        try {
            getContentResolver().openFileDescriptor(localUri, "r").close();
        } catch (FileNotFoundException exc) {
            Log.d(LOG_TAG, "Failed to open download " + cursor.getLong(mIdColumnId), exc);
            showFailedDialog(cursor.getLong(mIdColumnId),
                    getString(R.string.dialog_file_missing_body));
            return;
        } catch (IOException exc) {
            // close() failed, not a problem
        }

        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        final Intent intent = OpenHelper.buildViewIntent(this, id);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.download_no_application_title, Toast.LENGTH_LONG).show();
        }
    }

    private void handleItemClick(Cursor cursor) {
        long id = cursor.getInt(mIdColumnId);
        switch (cursor.getInt(mStatusColumnId)) {
            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_RUNNING:
                sendRunningDownloadClickedBroadcast(id);
                break;

            case DownloadManager.STATUS_PAUSED:
                if (isPausedForWifi(cursor)) {
                    mQueuedDownloadId = id;
                    mQueuedDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.dialog_title_queued_body)
                            .setMessage(R.string.dialog_queued_body)
                            .setPositiveButton(R.string.keep_queued_download, null)
                            .setNegativeButton(R.string.remove_download, getDeleteClickHandler(id))
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                /**
                                 * Called when a dialog for a pending download is canceled.
                                 */
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    mQueuedDownloadId = null;
                                    mQueuedDialog = null;
                                    mCurrentDialog = null;
                                }
                            })
                            //.show();
                            .create();
                    /// M: Add to support Queue AlertDialog @{
                    Xlog.d(LOG_OMA_DL, "DownloadList:handleItemClick: Alert dialog is: **" + mQueuedDialog + "**");
                    mDownloadsToShow.add(mQueuedDialog);
                    showNextDialog();
                    /// @}
                } else {
                    sendRunningDownloadClickedBroadcast(id);
                }
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                openCurrentDownload(cursor);
                break;

            case DownloadManager.STATUS_FAILED:
                showFailedDialog(id, getErrorMessage(cursor));
                break;
        }
    }

    /**
     * @return the appropriate error message for the failed download pointed to by cursor
     */
    private String getErrorMessage(Cursor cursor) {
        switch (cursor.getInt(mReasonColumndId)) {
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                if (isOnExternalStorage(cursor)) {
                    return getString(R.string.dialog_file_already_exists);
                } else {
                    // the download manager should always find a free filename for cache downloads,
                    // so this indicates a strange internal error
                    return getUnknownErrorMessage();
                }

            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                if (isOnExternalStorage(cursor)) {
                    return getString(R.string.dialog_insufficient_space_on_external);
                } else {
                    return getString(R.string.dialog_insufficient_space_on_cache);
                }

            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return getString(R.string.dialog_media_not_found);

            case DownloadManager.ERROR_CANNOT_RESUME:
                return getString(R.string.dialog_cannot_resume);

            default:
                return getUnknownErrorMessage();
        }
    }

    private boolean isOnExternalStorage(Cursor cursor) {
        String localUriString = cursor.getString(mLocalUriColumnId);
        if (localUriString == null) {
            return false;
        }
        Uri localUri = Uri.parse(localUriString);
        if (!localUri.getScheme().equals("file")) {
            return false;
        }
        String path = localUri.getPath();
        String externalRoot = Environment.getExternalStorageDirectory().getPath();
        return path.startsWith(externalRoot);
    }

    private String getUnknownErrorMessage() {
        return getString(R.string.dialog_failed_body);
    }

    private void showFailedDialog(long downloadId, String dialogBody) {
        /// M: Add to support Queue AlertDialog @{
        AlertDialog failedDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_not_available)
                .setMessage(dialogBody)
                                    .setNegativeButton(R.string.delete_download, 
                                            getDeleteClickHandler(downloadId))
                                    .setPositiveButton(R.string.retry_download, 
                                            getRestartClickHandler(downloadId))
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            mCurrentDialog = null;
                                        }
                                    }).create();
        
        // Add to support Queue AlertDialog
        Xlog.d(LOG_OMA_DL, "DownloadList(): showFailedDialog: Alert dialog is: **" + failedDialog + "**");
        mDownloadsToShow.add(failedDialog);
        showNextDialog();
        /// @}
    }

    private void sendRunningDownloadClickedBroadcast(long id) {
        final Intent intent = new Intent(Constants.ACTION_LIST);
        intent.setPackage(Constants.PROVIDER_PACKAGE_NAME);
        intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS,
                new long[] { id });
        sendBroadcast(intent);
    }

    // handle a click on one of the download item checkboxes
    public void onDownloadSelectionChanged(long downloadId, boolean isSelected,
            String fileName, String mimeType) {
        if (isSelected) {
            mSelectedIds.put(downloadId, new SelectionObjAttrs(fileName, mimeType));
        } else {
            mSelectedIds.remove(downloadId);
        }
    }

    /**
     * Requery the database and update the UI.
     */
    private void refresh() {
        mDateSortedCursor.requery();
        mSizeSortedCursor.requery();
        // Adapters get notification of changes and update automatically
        
        /// M: Add to support OMA_DL
        /// When resume this activity, we will update database.
        /// Then the content observer will rework again.
        handleOmaDownload();
    }

    /**
     * Delete a download from the Download Manager.
     */
    private void deleteDownload(long downloadId) {
        // let DownloadService do the job of cleaning up the downloads db, mediaprovider db,
        // and removal of file from sdcard
        // TODO do the following in asynctask - not on main thread.
        
        mDownloadManager.markRowDeleted(downloadId);
        sDownloadProviderFeatureEx = Extensions.getDefault(DownloadList.this);
        if (sDownloadProviderFeatureEx.shouldFinishThisActivity()) {
            finish();
        }
    }

    public boolean isDownloadSelected(long id) {
        return mSelectedIds.containsKey(id);
    }

    /**
     * Called when there's a change to the downloads database.
     */
    void handleDownloadsChanged() {
	Xlog.d(XTAG_ENHANCE,"Downloadlist: handleDownloadsChanged() called before, mDateSortedCursor.isClosed(): " +	
                  mDateSortedCursor.isClosed() + ",mSizeSortedCursor.isClosed(): " + mSizeSortedCursor.isClosed());
	
	if (mDateSortedCursor.isClosed()) {
            Xlog.d(XTAG_ENHANCE, "mDateSortedCursor have closed, return");
	    return;
	}

        checkSelectionForDeletedEntries();

        if (mQueuedDownloadId != null && moveToDownload(mQueuedDownloadId)) {
            if (mDateSortedCursor.getInt(mStatusColumnId) != DownloadManager.STATUS_PAUSED
                    || !isPausedForWifi(mDateSortedCursor)) {
                mQueuedDialog.cancel();
            }
        }
	Xlog.d(XTAG_ENHANCE,"Downloadlist: handleDownloadsChanged() called end, mDateSortedCursor.isClosed(): " +	
                  mDateSortedCursor.isClosed() + ",mSizeSortedCursor.isClosed(): " + mSizeSortedCursor.isClosed());
    }

    private boolean isPausedForWifi(Cursor cursor) {
        return cursor.getInt(mReasonColumndId) == DownloadManager.PAUSED_QUEUED_FOR_WIFI;
    }

    /**
     * Check if any of the selected downloads have been deleted from the downloads database, and
     * remove such downloads from the selection.
     */
    private void checkSelectionForDeletedEntries() {
        // gather all existing IDs...
        Set<Long> allIds = new HashSet<Long>();
        for (mDateSortedCursor.moveToFirst(); !mDateSortedCursor.isAfterLast();
                mDateSortedCursor.moveToNext()) {
            allIds.add(mDateSortedCursor.getLong(mIdColumnId));
        }

        // ...and check if any selected IDs are now missing
        for (Iterator<Long> iterator = mSelectedIds.keySet().iterator(); iterator.hasNext(); ) {
            if (!allIds.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

    /**
     * Move {@link #mDateSortedCursor} to the download with the given ID.
     * @return true if the specified download ID was found; false otherwise
     */
    private boolean moveToDownload(long downloadId) {
        for (mDateSortedCursor.moveToFirst(); !mDateSortedCursor.isAfterLast();
                mDateSortedCursor.moveToNext()) {
            if (mDateSortedCursor.getLong(mIdColumnId) == downloadId) {
                return true;
            }
        }
        return false;
    }

    /**
     * handle share menu button click when one more files are selected for sharing
     */
    public boolean shareDownloadedFiles() {
        Intent intent = new Intent();
        if (mSelectedIds.size() > 1) {
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Parcelable> attachments = new ArrayList<Parcelable>();
            ArrayList<String> mimeTypes = new ArrayList<String>();
            for (SelectionObjAttrs item : mSelectedIds.values()) {
                String fileName = item.getFileName();
                String mimeType = item.getMimeType();
                if (null == fileName) {
                    Xlog.w(XTAG_ENHANCE, "shareDownloadedFiles: File name isn't exist");
                    Toast.makeText(DownloadList.this, R.string.share_failed_with_download_error, 
                            Toast.LENGTH_LONG).show();
                    return false;
                }
                
                /// M:Add for MTK DRM. Don't share DRM file @{
                if (FeatureOption.MTK_DRM_APP && null != mimeType) {
                    if (mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT)) {
                        OmaDrmClient drmClient = new OmaDrmClient(this);
                        String oriMimeType = drmClient.getOriginalMimeType(fileName); 
                        if (null != oriMimeType && !oriMimeType.isEmpty()) {
                            mimeType = oriMimeType;
                            Xlog.d(XTAG_DRM, "DownloadList: share a DRM file:" + fileName + 
                                    " original MimeType is:" + mimeType);
                        }
                    } else if (mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_DRM_MESSAGE)
                               || mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_RIGHTS_XML)
                               || mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_RIGHTS_WBXML)) {
                        Toast.makeText(DownloadList.this, 
                                       com.mediatek.internal.R.string.drm_forwardforbidden_message,
                                       Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                /// @}
                
                attachments.add(Uri.fromFile(new File(fileName)));
                mimeTypes.add(mimeType);
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
            intent.setType(findCommonMimeType(mimeTypes));
        } else {
            // get the entry
            // since there is ONLY one entry in this, we can do the following
            for (SelectionObjAttrs item : mSelectedIds.values()) {
                intent.setAction(Intent.ACTION_SEND);
                String mimeType = item.getMimeType();
                
                if (null == item.getFileName()) {
                    Xlog.w(XTAG_ENHANCE, "shareDownloadedFiles: File name isn't exist");
                    Toast.makeText(DownloadList.this, R.string.share_failed_with_download_error, 
                            Toast.LENGTH_LONG).show();
                    return false;
                }
                
                /// M:Add for MTK DRM. Don't share DRM file @{
                if (FeatureOption.MTK_DRM_APP && null != mimeType) {
                    if (mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT)) {
                        OmaDrmClient drmClient = new OmaDrmClient(this);
                        String oriMimeType = drmClient.getOriginalMimeType(item.getFileName()); 
                        if (null != oriMimeType && !oriMimeType.isEmpty()) {
                            mimeType = oriMimeType;
                            Xlog.d(XTAG_DRM, "DownloadList: share one DRM file:" + item.getFileName() + 
                                    " original MimeType is:" + mimeType);
                        }
                    } else if (mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_DRM_MESSAGE)
                               || mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_RIGHTS_XML)
                               || mimeType.equals(OmaDrmStore.DrmObjectMime.MIME_RIGHTS_WBXML)) {
                        Toast.makeText(DownloadList.this, 
                                       com.mediatek.internal.R.string.drm_forwardforbidden_message,
                                       Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                /// @}
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(item.getFileName())));
                intent.setType(mimeType);
            }
        }
        intent = Intent.createChooser(intent, getText(R.string.download_share_dialog));
        startActivity(intent);
        return true;
    }

    private String findCommonMimeType(ArrayList<String> mimeTypes) {
        // are all mimeypes the same?
        String str = findCommonString(mimeTypes);
        if (str != null) {
            return str;
        }

        // are all prefixes of the given mimetypes the same?
        ArrayList<String> mimeTypePrefixes = new ArrayList<String>();
        for (String s : mimeTypes) {
            mimeTypePrefixes.add(s.substring(0, s.indexOf('/')));
        }
        str = findCommonString(mimeTypePrefixes);
        if (str != null) {
            return str + "/*";
        }

        // return generic mimetype
        return "*/*";
    }
    private String findCommonString(Collection<String> set) {
        String str = null;
        boolean found = true;
        for (String s : set) {
            if (str == null) {
                str = s;
            } else if (!str.equals(s)) {
                found = false;
                break;
            }
        }
        return (found) ? str : null;
    }
}
