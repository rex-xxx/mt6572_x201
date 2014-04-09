/*
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

package com.android.browser;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRouteParams;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Proxy;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import com.mediatek.browser.ext.Extensions;
import com.mediatek.browser.ext.IBrowserDownloadEx;
import com.mediatek.xlog.Xlog;

import java.io.File;
import java.io.IOException;

/**
 * This class is used to pull down the http headers of a given URL so that
 * we can analyse the mimetype and make any correction needed before we give
 * the URL to the download manager.
 * This operation is needed when the user long-clicks on a link or image and
 * we don't know the mimetype. If the user just clicks on the link, we will
 * do the same steps of correcting the mimetype down in
 * android.os.webkit.LoadListener rather than handling it here.
 *
 */
class FetchUrlMimeType extends Thread {

    private final static String LOGTAG = "FetchUrlMimeType";

    private Context mContext;
    private DownloadManager.Request mRequest;
    private String mUri;
    private String mCookies;
    private String mUserAgent;
    public static final String XLOGTAG = "Browser/FetchMimeType";

    public FetchUrlMimeType(Context context, DownloadManager.Request request,
            String uri, String cookies, String userAgent) {
        mContext = context.getApplicationContext();
        mRequest = request;
        mUri = uri;
        mCookies = cookies;
        mUserAgent = userAgent;
    }
    private IBrowserDownloadEx mBrowserDownloadEx = null;
    @Override
    public void run() {
        // User agent is likely to be null, though the AndroidHttpClient
        // seems ok with that.
        AndroidHttpClient client = AndroidHttpClient.newInstance(mUserAgent);
        HttpHost httpHost;
        try {
            httpHost = Proxy.getPreferredHttpHost(mContext, mUri);
            if (httpHost != null) {
                ConnRouteParams.setDefaultProxy(client.getParams(), httpHost);
            }
        } catch (IllegalArgumentException ex) {
            Log.e(LOGTAG,"Download failed: " + ex);
            client.close();
            return;
        }
        HttpHead request = new HttpHead(mUri);

        if (mCookies != null && mCookies.length() > 0) {
            request.addHeader("Cookie", mCookies);
        }

        HttpResponse response;
        String mimeType = null;
        String contentDisposition = null;
        
        HttpGet requestGet = null;
        try {
            response = client.execute(request);
            /// M: fix 452066. @{
            if (response.getStatusLine().getStatusCode() == 501 ||
                    response.getStatusLine().getStatusCode() == 400) {
                requestGet = new HttpGet(mUri);
                response = client.execute(requestGet);
                Xlog.d(XLOGTAG, "FetchUrlMimeType:  use Get method");
            }
            /// @}
            // We could get a redirect here, but if we do lets let
            // the download manager take care of it, and thus trust that
            // the server sends the right mimetype
            if (response.getStatusLine().getStatusCode() == 200) {
                Header header = response.getFirstHeader("Content-Type");
                if (header != null) {
                    mimeType = header.getValue();
                    final int semicolonIndex = mimeType.indexOf(';');
                    if (semicolonIndex != -1) {
                        mimeType = mimeType.substring(0, semicolonIndex);
                    }
                }
                Header contentDispositionHeader = response.getFirstHeader("Content-Disposition");
                if (contentDispositionHeader != null) {
                    contentDisposition = contentDispositionHeader.getValue();
                }
            }
        } catch (IllegalArgumentException ex) {
            request.abort();
            if (requestGet != null) {
                requestGet.abort();
			}
        } catch (IOException ex) {
            request.abort();
			if (requestGet != null) {
                requestGet.abort();
			}
        } finally {
            client.close();
        }

       if (mimeType != null) {
           if (mimeType.equalsIgnoreCase("text/plain") ||
                   mimeType.equalsIgnoreCase("application/octet-stream")) {
               String newMimeType =
                       MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                           MimeTypeMap.getFileExtensionFromUrl(mUri));
               if (newMimeType != null) {
                   mimeType = newMimeType;
                   mRequest.setMimeType(newMimeType);
               }
           }
       }

       String filename = URLUtil.guessFileName(mUri, contentDisposition,
               mimeType);
       Xlog.d(XLOGTAG, "FetchUrlMimeType: Guess file name is: " + filename + 
               " mimeType is: " + mimeType);
       /// M: Operator Feature set RequestDestinationDir @{
       mBrowserDownloadEx = Extensions.getDownloadPlugin(mContext);
       if (!mBrowserDownloadEx.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(),
               mRequest, filename, mimeType)) {
           // set downloaded file destination to /sdcard/Download.
           // or, should it be set to one of several Environment.DIRECTORY* dirs depending on mimetype?
            String downloadPath = "file://" + BrowserSettings.getInstance().getDownloadPath()
                                            + File.separator + filename;
            Uri pathUri = Uri.parse(downloadPath);
            mRequest.setDestinationUri(pathUri);
       }
       /// @}
       
       // Start the download
       DownloadManager manager = (DownloadManager) mContext.getSystemService(
               Context.DOWNLOAD_SERVICE);
       manager.enqueue(mRequest);
       
       // M: Add to start Download activity
       Intent pageView = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
       pageView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       mContext.startActivity(pageView);
    }

}
