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

package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.mediatek.browser.ext.Extensions;
import com.mediatek.browser.ext.IBrowserDownloadEx;
import com.mediatek.xlog.Xlog;

import java.io.File;
import java.net.URI;

/**
 * Handle download requests
 */
public class DownloadHandler {

    private static final boolean LOGD_ENABLED =
            com.android.browser.Browser.LOGD_ENABLED;

    private static final String LOGTAG = "DLHandler";
    private static final String XLOGTAG = "browser/DLHandler";
    private static IBrowserDownloadEx sBrowserDownloadEx = null;
    /**
     * Notify the host application a download should be done, or that
     * the data should be streamed if a streaming viewer is available.
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing tab.
     */
    public static void onDownloadStart(Activity activity, String url,
            String userAgent, String contentDisposition, String mimetype,
            String referer, boolean privateBrowsing, long  contentLength) {
        // if we're dealing wih A/V content that's not explicitly marked
        //     for download, check if it's streamable.
        if (contentDisposition == null
                || !contentDisposition.regionMatches(
                        true, 0, "attachment", 0, 10)) {
            // query the package manager to see if there's a registered handler
            //     that matches.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimetype);
            ResolveInfo info = activity.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                ComponentName myName = activity.getComponentName();
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.
                if (!myName.getPackageName().equals(
                        info.activityInfo.packageName)
                        || !myName.getClassName().equals(
                                info.activityInfo.name)) {
                    /// M: Add to open http live streaming media directly. @{ 
                    if (mimetype.equalsIgnoreCase("application/x-mpegurl") ||
                            mimetype.equalsIgnoreCase("application/vnd.apple.mpegurl")) {
                        activity.startActivity(intent);
                        return;
                    }
                    /// @}
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        showDownloadOrOpenContent(activity, intent, url, userAgent, 
                                // contentDisposition, mimetype, privateBrowsing);
                                contentDisposition, mimetype, privateBrowsing, contentLength);
                        return;
                    } catch (ActivityNotFoundException ex) {
                        if (LOGD_ENABLED) {
                            Log.d(LOGTAG, "activity not found for " + mimetype
                                    + " over " + Uri.parse(url).getScheme(),
                                    ex);
                        }
                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        }
        onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                mimetype, referer, privateBrowsing, contentLength);
    }

    // This is to work around the fact that java.net.URI throws Exceptions
    // instead of just encoding URL's properly
    // Helper method for onDownloadStartNoStream
    private static String encodePath(String path) {
        char[] chars = path.toCharArray();

        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                needed = true;
                break;
            }
        }
        if (needed == false) {
            return path;
        }

        StringBuilder sb = new StringBuilder("");
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Notify the host application a download should be done, even if there
     * is a streaming viewer available for thise type.
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing tab.
     */
    /*package */ public static void onDownloadStartNoStream(Activity activity,
            String url, String userAgent, String contentDisposition,
            String mimetype, String referer, boolean privateBrowsing, long contentLength) {

        String filename = URLUtil.guessFileName(url,
                contentDisposition, mimetype);
        Xlog.d(XLOGTAG, "Guess file name is: " + filename + 
                " mimetype is: " + mimetype);

        // Check to see if we have an SDCard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            int title;
            String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = activity.getString(R.string.download_no_sdcard_dlg_msg, filename);
                title = R.string.download_no_sdcard_dlg_title;
            }

            new AlertDialog.Builder(activity)
                .setTitle(title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
        }

        /// M: Check whether the download path of browser is available before begin to download. @{
        String mDownloadPath = BrowserSettings.getInstance().getDownloadPath();
        if (mDownloadPath.contains("sdcard1")) {
            if (! new File("/storage/sdcard1").canWrite()) {
                int mTitle = R.string.download_path_unavailable_dlg_title;
                String mMsg = activity.getString(R.string.download_path_unavailable_dlg_msg);
                new AlertDialog.Builder(activity)
                    .setTitle(mTitle)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(mMsg)
                    .setPositiveButton(R.string.ok, null)
                    .show();
                return;
            }
        }
        /// @}
        
        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        WebAddress webAddress;
        try {
            webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
        } catch (Exception e) {
            // This only happens for very bad urls, we want to chatch the
            // exception here
            Log.e(LOGTAG, "Exception trying to parse url:" + url);
            return;
        }

        String addressString = webAddress.toString();
        Uri uri = Uri.parse(addressString);
        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(uri);
        } catch (IllegalArgumentException e) {
            Toast.makeText(activity, R.string.cannot_download, Toast.LENGTH_SHORT).show();
            return;
        }
        request.setMimeType(mimetype);
        /// M: Operator Feature set RequestDestinationDir @{
        sBrowserDownloadEx = Extensions.getDownloadPlugin(activity);
        if (!sBrowserDownloadEx.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), 
                request, filename, mimetype)) {
        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs depending on mimetype?
            //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            String downloadPath = "file://" + BrowserSettings.getInstance().getDownloadPath()
                                            + File.separator + filename;
            Uri pathUri = Uri.parse(downloadPath);
            request.setDestinationUri(pathUri);
            Xlog.d(XLOGTAG, "request.setDestinationInExternalPublicDir, addressString: " + addressString);
        }
        /// @}
        
        // let this downloaded file be scanned by MediaScanner - so that it can 
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner();
        request.setDescription(webAddress.getHost());
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.addRequestHeader("Referer", referer);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setUserAgent(userAgent);
        if (mimetype == null) {
            if (TextUtils.isEmpty(addressString)) {
                return;
            }

            /// M: fix BUG: ALPS00256340 @{
            try {
                URI.create(addressString);
            } catch (IllegalArgumentException e) {
                Toast.makeText(activity, R.string.cannot_download, Toast.LENGTH_SHORT).show();
                return;
            }
            /// @}
            
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            new FetchUrlMimeType(activity, request, addressString, cookies,
                    userAgent).start();
        } else {
            final DownloadManager manager
                    = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            new Thread("Browser download") {
                public void run() {
                    manager.enqueue(request);
                }
            }.start();
        }

        /// M: Operator Feature show ToastWithFileSize @{
        sBrowserDownloadEx = Extensions.getDownloadPlugin(activity);
        if (contentLength > 0 && sBrowserDownloadEx.shouldShowToastWithFileSize()) {
            Toast.makeText(activity, activity.getString(R.string.download_pending_with_file_size) 
                    + Formatter.formatFileSize(activity, contentLength), Toast.LENGTH_SHORT)
                    .show();
        } else {
        /// @}
            Toast.makeText(activity, R.string.download_pending, Toast.LENGTH_SHORT)
                       .show();
        }
        
        /// M: Add to start Download activity. @{
        Intent pageView = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        pageView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(pageView);
        /// @}
    }
    
    /**
     * M: Notify the user download the content or open
     * the content. Add this to support Operator customization
     * @param intent The Intent.ACTION_VIEW intent
     * @param url The full url to the content that should be downloaded
     * @param contentDisposition Content-disposition http header, if
     *                           present.
     * @param mimetype The mimetype of the content reported by the server
     * @param contentLength The file size reported by the server
     */
    public static void showDownloadOrOpenContent(final Activity activity, final Intent intent, 
            final String url, final String userAgent,
            final String contentDisposition, final String mimetype, 
            // final boolean privateBrowsing) {
            final boolean privateBrowsing, final long contentLength) {
        new AlertDialog.Builder(activity)
            .setTitle(R.string.application_name)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage(R.string.download_or_open_content)
            .setPositiveButton(R.string.save_content,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            onDownloadStartNoStream(activity, url, userAgent, 
                                    // contentDisposition, mimetype, privateBrowsing);
                                    contentDisposition, mimetype, null, privateBrowsing, contentLength);
                            Xlog.d(XLOGTAG, "User decide to download the content");
                            return;
                        }
                    })
            .setNegativeButton(R.string.open_content,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            int nFlags = intent.getFlags();
                            nFlags &= (~Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setFlags(nFlags);
                            
                            activity.startActivity(intent);
                            Xlog.d(XLOGTAG, "User decide to open the content by startActivity");
                            return;
                        }})
            .setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            Xlog.d(XLOGTAG, "User cancel the download action");
                            return;
                        }
                    })
            .show();
    }

}
