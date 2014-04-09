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

import android.app.ActivityManagerNative;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
//import android.drm.DrmManagerClient;
//import android.drm.DrmStore;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmUiUtils;
//import android.drm.DrmUtils;
import com.mediatek.drm.OmaDrmUtils;
import android.drm.mobile1.DrmRawContent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Downloads;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import java.text.SimpleDateFormat;
import android.content.ContentResolver;

/**
 * List adapter for Cursors returned by {@link DownloadManager}.
 */
public class DownloadAdapter extends CursorAdapter {
    private final DownloadList mDownloadList;
    private Cursor mCursor;
    private Resources mResources;
    private DateFormat mDateFormat;
    private DateFormat mTimeFormat;

    private final int mTitleColumnId;
    private final int mDescriptionColumnId;
    private final int mStatusColumnId;
    private final int mReasonColumnId;
    private final int mTotalBytesColumnId;
    private final int mMediaTypeColumnId;
    private final int mDateColumnId;
    private final int mIdColumnId;
    private final int mFileNameColumnId;

    /// M: add to support progress bar @{
    private final int mCurrentBytesColumnId;
    
    // Add to suppor MTK DRM
    private int mLocalUriColumnId;
    private OmaDrmClient mDrmManageClient = null;
    private static final String XTAG_DRM = "DownloadManager/DRM";
    /// @}
    
    public DownloadAdapter(DownloadList downloadList, Cursor cursor) {
        super(downloadList, cursor);
        mDownloadList = downloadList;
        mCursor = cursor;
        mResources = mDownloadList.getResources();
        mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        mTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        mIdColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
        mTitleColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
        mDescriptionColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION);
        mStatusColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
        mReasonColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON);
        mTotalBytesColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
        mMediaTypeColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE);
        mDateColumnId =
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
        mFileNameColumnId =
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME);
        
        /// M: Add to support progress bar
        mCurrentBytesColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
        
        /// M: Add to support MTK DRM
        mLocalUriColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
        
    }

    public View newView() {
        final DownloadItem view = (DownloadItem) LayoutInflater.from(mDownloadList)
                .inflate(R.layout.download_list_item, null);
        view.setDownloadListObj(mDownloadList);
        return view;
    }

    public void bindView(View convertView, int position) {
        if (!(convertView instanceof DownloadItem)) {
            return;
        }

        long downloadId = mCursor.getLong(mIdColumnId);
        ((DownloadItem) convertView).setData(downloadId, position,
                mCursor.getString(mFileNameColumnId),
                mCursor.getString(mMediaTypeColumnId));

        // Retrieve the icon for this download
        retrieveAndSetIcon(convertView);

        String title = mCursor.getString(mTitleColumnId);
        if (title.isEmpty()) {
            title = mResources.getString(R.string.missing_title);
        }
        setTextForView(convertView, R.id.download_title, title);
        setTextForView(convertView, R.id.domain, mCursor.getString(mDescriptionColumnId));
        
        /// M: Add to support progress bar @{
        int downloadStatus = mCursor.getInt(mStatusColumnId);
        View progress = convertView.findViewById(R.id.download_progress);
        
        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL
                || downloadStatus == DownloadManager.STATUS_FAILED) {
            progress.setVisibility(View.GONE);
            setTextForView(convertView, R.id.status_text,
                    mResources.getString(getStatusStringId(mCursor.getInt(mStatusColumnId))));
            setTextForView(convertView, R.id.size_text, getSizeText());
            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                convertView.findViewById(R.id.last_modified_date).setVisibility(View.VISIBLE);
                setTextForView(convertView, R.id.last_modified_date, getDateString());
            }
        } else {
            progress.setVisibility(View.VISIBLE);
            ProgressBar pb = (ProgressBar) progress;
            StringBuilder sbStatus = new StringBuilder();
            StringBuilder sbCurrentBytes = new StringBuilder();
            long totalBytes = mCursor.getLong(mTotalBytesColumnId);
            Xlog.i(XTAG_DRM, "status is downloading, id : " + downloadId + ", totalBytes:" + totalBytes);
            long currentBytes = mCursor.getLong(mCurrentBytesColumnId);

            if (totalBytes > 0) {
                int progressAmount = (int)(currentBytes * 100 / totalBytes);
                sbStatus.append(mResources.getString(getStatusStringId(mCursor.getInt(mStatusColumnId))));
                sbStatus.append(' ');
                sbStatus.append(progressAmount);
                sbStatus.append("%");
                
                sbCurrentBytes.append("(");
                if (currentBytes >= 0) {
                    sbCurrentBytes.append(Formatter.formatFileSize(mContext, currentBytes));
                } else {
                    sbCurrentBytes.append(Formatter.formatFileSize(mContext, 0));
                }
                sbCurrentBytes.append("/");
                sbCurrentBytes.append(getSizeText());
                sbCurrentBytes.append(")");
                pb.setIndeterminate(false);
                pb.setProgress(progressAmount);
            } else {
                pb.setIndeterminate(true);
            }
            setTextForView(convertView, R.id.status_text, sbStatus.toString());
            setTextForView(convertView, R.id.size_text, sbCurrentBytes.toString());
            
            try {
                Configuration mConf = ActivityManagerNative.getDefault().getConfiguration();   
                if (mConf.fontScale == 1.15f || mConf.fontScale == 1.1f) {
	            convertView.findViewById(R.id.last_modified_date).setVisibility(View.INVISIBLE);
                } else {
                    convertView.findViewById(R.id.last_modified_date).setVisibility(View.VISIBLE);
                    setTextForView(convertView, R.id.last_modified_date, getDateString()); 
                }
            } catch (RemoteException e) { 
                Log.w(XTAG_DRM, "Unable to retrieve font size");
            }
        }
        /// @}

        ((DownloadItem) convertView).getCheckBox()
                .setChecked(mDownloadList.isDownloadSelected(downloadId));
    }

    private String getDateString() {
        Date date = new Date(mCursor.getLong(mDateColumnId));
        if (date.before(getStartOfToday())) {
            return mDateFormat.format(date);
        } else {
            /// M: Modify the date string for 24-hour system
            ContentResolver cr = mDownloadList.getContentResolver();
            String strTimeFormat = android.provider.Settings.System.getString(cr,
                        android.provider.Settings.System.TIME_12_24);
            if (strTimeFormat != null && strTimeFormat.equals("24")) {
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
                return formatter.format(date);     
            }
            /// @}
            return mTimeFormat.format(date);
        }
    }

    private Date getStartOfToday() {
        Calendar today = new GregorianCalendar();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTime();
    }

    private String getSizeText() {
        long totalBytes = mCursor.getLong(mTotalBytesColumnId);
        String sizeText = "";
        if (totalBytes >= 0) {
            sizeText = Formatter.formatFileSize(mContext, totalBytes);
        }
        return sizeText;
    }

    private int getStatusStringId(int status) {
        switch (status) {
            /// M: add case status fail @{
            case DownloadManager.STATUS_FAILED:
                if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
                    int failedReason = mCursor.getInt(mReasonColumnId);
                    if (failedReason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                        return R.string.download_error_insufficient_memory; 
                    } else if (failedReason == Downloads.Impl.STATUS_BAD_REQUEST) {
                        return R.string.download_error_invalid_descriptor;
                    } else if (failedReason == Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DDVERSION) {
                        return R.string.download_error_invalid_ddversion;
                    } else if (failedReason == Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH) {
                        return R.string.download_error_attribute_mismatch;
                    }
                }
                return R.string.download_error;
                /// @}
            case DownloadManager.STATUS_SUCCESSFUL:
                return R.string.download_success;

            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_RUNNING:
                return R.string.download_running;

            case DownloadManager.STATUS_PAUSED:
                final int reason = mCursor.getInt(mReasonColumnId);
                switch (reason) {
                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                        return R.string.download_queued;
                    /// M: add case paused by app
                    case DownloadManager.PAUSED_BY_APP:
                        return R.string.download_paused;
                    default:
                        return R.string.download_running;
                }
        }
        throw new IllegalStateException("Unknown status: " + mCursor.getInt(mStatusColumnId));
    }

    private void retrieveAndSetIcon(View convertView) {
        String mediaType = mCursor.getString(mMediaTypeColumnId);
        ImageView iconView = (ImageView) convertView.findViewById(R.id.download_icon);
        iconView.setVisibility(View.INVISIBLE);
        int downloadStatus = mCursor.getInt(mStatusColumnId);
        ///M: Add the downloadId in log @{
        long downloadId = mCursor.getLong(mIdColumnId);
        /// @}
        
        if (mediaType == null) {
            return;
        }

        /// M: Modify to support MTK DRM @{
        if (DrmRawContent.DRM_MIMETYPE_MESSAGE_STRING.equalsIgnoreCase(mediaType)
                && !FeatureOption.MTK_DRM_APP) {
            iconView.setImageResource(R.drawable.ic_launcher_drm_file);  
        } else {
            if (FeatureOption.MTK_DRM_APP &&
                    (mediaType.equalsIgnoreCase(OmaDrmStore.DrmObjectMime.MIME_DRM_MESSAGE) ||
                            mediaType.equalsIgnoreCase(OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT)) &&
                                                downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {

                // lazy initialization for mDrmManageClient
                if (null == mDrmManageClient) {
                    mDrmManageClient = new OmaDrmClient(mDownloadList);
                }
                Xlog.i(XTAG_DRM, "Download success, Need update DRM lock icon, mInfo.mId: " + downloadId);
                String localUri = mCursor.getString(mLocalUriColumnId);
                Bitmap background = null;

                if (localUri != null) {
                    String path = Uri.parse(localUri).getPath();
                    String oriMimeType = mDrmManageClient.getOriginalMimeType(path);
                    Xlog.i(XTAG_DRM, "in DownloadAdapter.retrieveAndSetIcon(),oriMimeType: " + oriMimeType);
                    int action = 0;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromParts("file", "", null), oriMimeType);
                    PackageManager pm = mContext.getPackageManager();
                    List<ResolveInfo> list = pm.queryIntentActivities(intent,
                            PackageManager.MATCH_DEFAULT_ONLY);

                    if (list.size() == 0) {
                        Drawable draw = mContext.getResources().getDrawable(
                                R.drawable.ic_download_misc_file_type);
                        background = ((BitmapDrawable) draw).getBitmap();
                    } else {
                        Drawable icon = list.get(0).activityInfo.loadIcon(pm);
                        background = ((BitmapDrawable) icon).getBitmap();
                    }

                    if (oriMimeType != null) {
                        action = OmaDrmUtils.getMediaActionType(oriMimeType);
                    }

                    if (path != null && (new File(path).exists()) && background != null) {
                        Bitmap bm = null;
                        int iconSize = iconView.getLayoutParams().width;
                        if (background.getHeight() > iconSize || background.getWidth() > iconSize) {
                            Bitmap compressBG = Bitmap.createScaledBitmap(background, iconSize,
                                    iconSize, false);
                            bm = OmaDrmUiUtils.overlayDrmIcon(mDrmManageClient, mResources, path, action,
                                    compressBG);
                        } else {
                            bm = OmaDrmUiUtils.overlayDrmIcon(mDrmManageClient, mResources, path, action,
                                    background);
                        }
                        if (bm != null) {
                            iconView.setImageBitmap(bm);
                            iconView.setVisibility(View.VISIBLE);
                            Xlog.d(XTAG_DRM, "Will Update DRM icon, file: " + path + " action:"
                                    + action + ", mInfo.mId: " + downloadId);
                            return;
                        }
                    }
                }
                iconView.setImageBitmap(background);

            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromParts("file", "", null), mediaType);
                PackageManager pm = mContext.getPackageManager();
                List<ResolveInfo> list = pm.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                if (list.size() == 0) {
                    // no icon found for this mediatype. use "unknown" icon
                    iconView.setImageResource(R.drawable.ic_download_misc_file_type);
                } else {
                    Drawable icon = list.get(0).activityInfo.loadIcon(pm);
                    iconView.setImageDrawable(icon);
                }
            }
        }
        // / @}
        iconView.setVisibility(View.VISIBLE);
    }

    private void setTextForView(View parent, int textViewId, CharSequence text) {
        TextView view = (TextView) parent.findViewById(textViewId);
        view.setText(text);
    }

    // CursorAdapter overrides

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return newView();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        bindView(view, cursor.getPosition());
    }
}
