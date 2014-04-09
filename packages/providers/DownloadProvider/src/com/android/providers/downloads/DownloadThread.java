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

package com.android.providers.downloads;

import static com.android.providers.downloads.Constants.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
//import android.drm.DrmManagerClient;
import android.drm.DrmRights;
//import android.drm.DrmUtils;
import com.mediatek.drm.OmaDrmClient;
import android.media.MediaScannerConnection;
import android.net.INetworkPolicyListener;
import android.net.NetworkPolicyManager;
import android.net.Proxy;
import android.net.TrafficStats;
import android.net.WebAddress;
import android.net.http.HttpAuthHeader;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;

import com.mediatek.downloadmanager.ext.Extensions;
import com.mediatek.downloadmanager.ext.IDownloadProviderFeatureEx;
import com.mediatek.xlog.Xlog;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

/**
 * Runs an actual download
 */
public class DownloadThread extends Thread {

    private IDownloadProviderFeatureEx mDownloadProviderFeatureEx;
    private final Context mContext;
    private final DownloadInfo mInfo;
    private final SystemFacade mSystemFacade;
    private final StorageManager mStorageManager;
    private DrmConvertSession mDrmConvertSession;
    private boolean mDownloadAlreadyCompleted;

    private volatile boolean mPolicyDirty;
    private static final String TAG = "DownloadThread";
    
    /// M: Add for fix GMS low memory issue 332710. @{
    private static final String PLAY_STORE_RECEIVER = "com.google.android.finsky."
            + "download.DownloadBroadcastReceiver";
    private static final String PLAY_STORE_CLASS = "com.android.vending";
    /// @}
    
    public DownloadThread(Context context, SystemFacade systemFacade, DownloadInfo info,
            StorageManager storageManager) {
        mContext = context;
        mSystemFacade = systemFacade;
        mInfo = info;
        mStorageManager = storageManager;
    }

    /**
     * Returns the user agent provided by the initiating app, or use the default one
     */
    private String userAgent() {
        String userAgent = mInfo.mUserAgent;
        if (userAgent == null) {
            userAgent = Constants.DEFAULT_USER_AGENT;
        }
        return userAgent;
    }

    /**
     * State for the entire run() method.
     */
    static class State {
        public String mFilename;
        public FileOutputStream mStream;
        public String mMimeType;
        public boolean mCountRetry = false;
        public int mRetryAfter = 0;
        public int mRedirectCount = 0;
        public String mNewUri;
        public boolean mGotData = false;
        public String mRequestUri;
        public long mTotalBytes = -1;
        public long mCurrentBytes = 0;
        public String mHeaderETag;
        public boolean mContinuingDownload = false;
        public long mBytesNotified = 0;
        public long mTimeLastNotification = 0;

        /** Historical bytes/second speed of this download. */
        public long mSpeed;
        /** Time when current sample started. */
        public long mSpeedSampleStart;
        /** Bytes transferred since current sample started. */
        public long mSpeedSampleBytes;

        // M: Add to support OMA download
        public int mOmaDownload;
        public int mOmaDownloadStatus;
        public String mOmaDownloadInsNotifyUrl;

        // M: Add to support DRM
        public long mTotalWriteBytes = 0;

        public State(DownloadInfo info) {
            mMimeType = Intent.normalizeMimeType(info.mMimeType);
            mRequestUri = info.mUri;
            mFilename = info.mFileName;
            mTotalBytes = info.mTotalBytes;
            mCurrentBytes = info.mCurrentBytes;
            
            // Add to support OMA download
            mOmaDownload = info.mOmaDownload;
            mOmaDownloadStatus = info.mOmaDownloadStatus;
            mOmaDownloadInsNotifyUrl = info.mOmaDownloadInsNotifyUrl;
        }
    }

    /**
     * State within executeDownload()
     */
    private static class InnerState {
        public String mHeaderContentLength;
        public String mHeaderContentDisposition;
        public String mHeaderContentLocation;
        // M: Add to support Authenticate download
        public int mAuthScheme = HttpAuthHeader.UNKNOWN;
        public HttpAuthHeader mAuthHeader = null;
        public String mHost = null;
        public boolean mIsAuthNeeded = false;
        // M: As description on HttpHost, -1 means default port
        public int mPort = -1;
        public String mScheme = null;
    }

    /**
     * Raised from methods called by executeDownload() to indicate that the download should be
     * retried immediately.
     */
    private class RetryDownload extends Throwable {}

    /**
     * Executes the download in a separate thread
     */
    @Override
    public void run() {
        Log.i(TAG, "start run download thread");
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        try {
            runInternal();
        } finally {
            DownloadHandler.getInstance().dequeueDownload(mInfo.mId);
        }
    }
    
    private void runInternal() {
        // Skip when download already marked as finished; this download was
        // probably started again while racing with UpdateThread.
        if (DownloadInfo.queryDownloadStatus(mContext.getContentResolver(), mInfo.mId)
                == Downloads.Impl.STATUS_SUCCESS) {
            Log.d(TAG, "Download " + mInfo.mId + " already finished; skipping");
            return;
        }

        mDownloadAlreadyCompleted = false;
        State state = new State(mInfo);
        // Modify to support Authenticate download
        // AndroidHttpClient client = null;
        DefaultHttpClient client = null;
        PowerManager.WakeLock wakeLock = null;
        int finalStatus = Downloads.Impl.STATUS_UNKNOWN_ERROR;
        String errorMsg = null;

        final NetworkPolicyManager netPolicy = NetworkPolicyManager.from(mContext);
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        try {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG);
            wakeLock.acquire();

            // M: while performing download, register for rules updates
            netPolicy.registerListener(mPolicyListener);

            if (Constants.LOGV) {
                Log.v(Constants.TAG, "initiating download for " + mInfo.mUri);
            }

            // M: Modify to support Authenticate download
            // client = AndroidHttpClient.newInstance(userAgent(), mContext);
            HttpParams params = new BasicHttpParams();
            // As AndroidHttpClient, set these params
            HttpConnectionParams.setStaleCheckingEnabled(params, false);
            HttpConnectionParams.setConnectionTimeout(params, 60 * 1000);

            /// M: Operator Feature set the socket time out to 30S; 
            /// check whether need notify if the file already exsit. @{
            mDownloadProviderFeatureEx = Extensions.getDefault(mContext);
            mDownloadProviderFeatureEx.setHttpSocketTimeOut(params, 60 * 1000);
            
            HttpConnectionParams.setSocketBufferSize(params, 8192);
            HttpProtocolParams.setUserAgent(params, userAgent());
            HttpClientParams.setRedirecting(params, false);
            client = new DefaultHttpClient(params);
            
            // network traffic on this thread should be counted against the
            // requesting uid, and is tagged with well-known value.
            TrafficStats.setThreadStatsTag(TrafficStats.TAG_SYSTEM_DOWNLOAD);
            TrafficStats.setThreadStatsUid(mInfo.mUid);

            boolean finished = false;
            while(!finished) {
                Log.i(Constants.TAG, "Initiating request for download " + mInfo.mId +
                        /// M: add thread id in log @{
                        ",currentThread id: " + Thread.currentThread().getId());
                        /// @}
                // Set or unset proxy, which may have changed since last GET request.
                // setDefaultProxy() supports null as proxy parameter.
                ConnRouteParams.setDefaultProxy(client.getParams(),
                        Proxy.getPreferredHttpHost(mContext, state.mRequestUri));
                HttpGet request = new HttpGet(state.mRequestUri);
                try {
                    executeDownload(state, client, request);
                    finished = true;
                } catch (RetryDownload exc) {
                    // fall through
                } finally {
                    request.abort();
                    request = null;
                }
            }
            
            // M: Add this to support OMA DL. 
            // Need to before Handle DRM. Because if install notify failed,
            // the Media object will be discard
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
                handleOmaDownloadMediaObject(state);
            }

            if (Constants.LOGV) {
                Log.v(Constants.TAG, "download completed for " + mInfo.mUri);
            }
            finalizeDestinationFile(state);

            /// M: Add this to support OMA DL.
            /// Deal with .dd file
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
                handleOmaDownloadDescriptorFile(state);
            }
            
            finalStatus = Downloads.Impl.STATUS_SUCCESS;
            Xlog.i(Constants.DL_ENHANCE, "Download success" + mInfo.mUri + ",mInfo.mId: " 
                    + mInfo.mId + ",currentThread id: " + Thread.currentThread().getId());
        } catch (StopRequestException error) {
            // remove the cause before printing, in case it contains PII
            errorMsg = error.getMessage();
            String msg = "Aborting request for download " + mInfo.mId +
                    /// M: add thread id in log @{
                    ",currentThread id: " + Thread.currentThread().getId() +
                    /// @}
                    ",: " + errorMsg;
            Log.w(Constants.TAG, msg);
            if (Constants.LOGV) {
                Log.w(Constants.TAG, msg, error);
            }
            finalStatus = error.mFinalStatus;
            // fall through to finally block
            mDownloadProviderFeatureEx = Extensions.getDefault(mContext);
            if (mDownloadProviderFeatureEx.shouldNotifyFileAlreadyExist()) {
                if (finalStatus == Downloads.Impl.STATUS_FILE_ALREADY_EXISTS_ERROR) {
                    mInfo.notifyFileAlreadyExist(errorMsg);
                }
            }
            /// @}
            
            /// M: Add for support OMA Download
            /// Notify to web server if failed @{
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT
                    && ((errorMsg != null && errorMsg.equals(Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY))
                            || Downloads.Impl.isStatusError(finalStatus))
                    && state.mOmaDownload == 1 && state.mOmaDownloadInsNotifyUrl != null) {
                int notifyCode = OmaStatusHandler.SUCCESS;
                URL notifyUrl = null;
                try {
                    notifyUrl = new URL(state.mOmaDownloadInsNotifyUrl);
                } catch (MalformedURLException e) {
                    // TODO:need error handling
                    // There will update OMA_Download_Status, or the query will reuse
                    Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: New notify url failed" + state.mOmaDownloadInsNotifyUrl);
                }
                switch (state.mOmaDownloadStatus) {
                    case Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DESCRIPTOR:
                        notifyCode = OmaStatusHandler.INVALID_DESCRIPTOR;
                        break;
                    case Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH:
                        notifyCode = OmaStatusHandler.ATTRIBUTE_MISMATCH;
                        break;
                    case Downloads.Impl.OMADL_STATUS_ERROR_INSUFFICIENT_MEMORY:
                        notifyCode = OmaStatusHandler.INSUFFICIENT_MEMORY;
                        break;
                    case Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DDVERSION:
                        notifyCode = OmaStatusHandler.INVALID_DDVERSION;
                        break;
                    default:
                        //notifyCode = OmaStatusHandler.DEVICE_ABORTED;
                        notifyCode = OmaStatusHandler.LOADER_ERROR;
                        break;
                }
                
                notifyOMADownloadWebServerErrorStatus(notifyUrl, notifyCode);
            }
            /// @}
        } catch (Throwable ex) { //sometimes the socket code throws unchecked exceptions
            errorMsg = ex.getMessage();
            String msg = "Exception for id " + mInfo.mId + ": " + errorMsg;
            Log.w(Constants.TAG, msg, ex);
            /// M: falls through to the code that reports an error @{
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT && state.mOmaDownload == 1 
                    && state.mOmaDownloadInsNotifyUrl != null) {
                URL notifyUrl = null;
                try {
                    notifyUrl = new URL(state.mOmaDownloadInsNotifyUrl);
                } catch (MalformedURLException e) {
                    // TODO:need error handling
                    // There will update OMA_Download_Status, or the query will reuse
                    Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: New notify url failed" + state.mOmaDownloadInsNotifyUrl);
                }
                notifyOMADownloadWebServerErrorStatus(notifyUrl, OmaStatusHandler.LOADER_ERROR);
            }
            /// @}
            finalStatus = Downloads.Impl.STATUS_UNKNOWN_ERROR;
            // falls through to the code that reports an error
        } finally {
            TrafficStats.clearThreadStatsTag();
            TrafficStats.clearThreadStatsUid();

            if (client != null) {
                /// M: Modify to support Authenticate download
                // client.close();
                client.getConnectionManager().shutdown();
                client = null;
            }
            Xlog.d(Constants.DL_ENHANCE, "before cleanupDestination(), finalStauts : " + finalStatus);
            cleanupDestination(state, finalStatus);
            notifyDownloadCompleted(finalStatus, state.mCountRetry, state.mRetryAfter,
                                    state.mGotData, state.mFilename,
                                    state.mNewUri, state.mMimeType, errorMsg);
            
            /// M: Add for fix GMS low memory issue 332710. @{
            Xlog.d(Constants.DL_ENHANCE, "after notifyDownloadCompleted"
                    + " mInfo.mClass is: " + mInfo.mClass + " mInfo.mPackage "
                    + mInfo.mPackage + ",after cleanupDestination(), finalStatus: " + finalStatus
                    + " ,now state.mFilename = " + state.mFilename);
            if (finalStatus == Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR
                    && mInfo.mClass != null && mInfo.mPackage != null
                    && mInfo.mClass.equalsIgnoreCase(PLAY_STORE_RECEIVER)
                    && mInfo.mPackage.equalsIgnoreCase(PLAY_STORE_CLASS)) {
                mInfo.sendIntentIfRequested();
            }
            /// @}
            

            netPolicy.unregisterListener(mPolicyListener);

            if (wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }
        }
        mStorageManager.incrementNumDownloadsSoFar();
        mDownloadAlreadyCompleted = false;
    }

    /**
     * M: This function is used to notify webserver 
     * oma download error status 
     */
    private void notifyOMADownloadWebServerErrorStatus(URL notifyUrl, int notifyCode) {
        if (notifyUrl != null) {
            Xlog.i(Constants.LOG_OMA_DL, "DownloadThread: catch StopRequest and need to notify web server: " + 
                    notifyUrl.toString() + " and Notify code is:" + notifyCode);
            OmaDescription omaDescription = new OmaDescription();
            omaDescription.setInstallNotifyUrl(notifyUrl);
            omaDescription.setStatusCode(notifyCode);
            if (OmaDownload.installNotify(omaDescription, null) != OmaStatusHandler.READY) {
                Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: catch StopRequest but notify URL : " +
                        "" + notifyUrl + " failed");
            } else {
                Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: catch StopRequest and notify URL OK");
            }
        }
    }

    /**
     * M: After download complete, Check whether OMA DL or not.
     * Deal with OMA DL file (install Notify and next url)
     * 
     */
    private void handleOmaDownloadMediaObject(State state) throws StopRequestException {    
        if (state.mOmaDownload != 1 || state.mMimeType.equalsIgnoreCase("application/vnd.oma.dd+xml")) {
            return;
        }
        state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_DOWNLOAD_COMPLETELY;
        ContentValues values = new ContentValues(); 

        if (state.mOmaDownloadInsNotifyUrl != null) {
            Xlog.i(Constants.LOG_OMA_DL, "Handle Media object, notify URL is: " +
                    state.mOmaDownloadInsNotifyUrl);
            URL notifyUrl = null;
            try {
                notifyUrl = new URL(state.mOmaDownloadInsNotifyUrl);
            } catch (MalformedURLException e) {     
                values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                        Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                
                state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED;
                
                // There will update OMA_Download_Status, or the query will reuse
                Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadMediaObject(): " +
                        "New url failed" + state.mOmaDownloadInsNotifyUrl);
                throw new StopRequestException(Downloads.Impl.STATUS_UNKNOWN_ERROR, 
                        "OMA Download Installation Media Object Failure");
            }
            
            OmaDescription omaDescription = new OmaDescription();
            omaDescription.setInstallNotifyUrl(notifyUrl);
            omaDescription.setStatusCode(OmaStatusHandler.SUCCESS);
            if (OmaDownload.installNotify(omaDescription, null) != OmaStatusHandler.READY) {
                values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                        Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                
                state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED;
                throw new StopRequestException(Downloads.Impl.STATUS_UNKNOWN_ERROR, 
                        "OMA Download Installation Media Object Failure");
            }
            Xlog.i(Constants.LOG_OMA_DL, "Handle Media object, after notify URL");
        }

        if (mInfo.mOmaDownloadNextUrl != null) {
            Xlog.d(Constants.LOG_OMA_DL, "DownloadThread:handleOmaDownloadMediaObject(): " +
                    "next url is: " + mInfo.mOmaDownloadNextUrl);
            values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_SUCCESS);
            //mInfo.notifyOmaDownloadNextUrl(mInfo.mOmaDownloadNextUrl);
            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG, 1);
            // Download the Media Object success and install notify success and need to show user next URL
            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                    Downloads.Impl.OMADL_STATUS_HAS_NEXT_URL);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
        }
    }
    
    /**
     * M: Add this function to support OMA DL
     * This function is used to handle .dd file
     */
    private void handleOmaDownloadDescriptorFile (State state) throws StopRequestException {
        // Handle .dd file
        if (state.mMimeType != null) {
            if (state.mMimeType.equals("application/vnd.oma.dd+xml")) {             
                state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_DOWNLOAD_COMPLETELY;
                File ddFile = new File(state.mFilename);
                URL ddUrl = null;
                try {
                    ddUrl = new URL(state.mRequestUri);
                } catch (MalformedURLException e) {
                    // TODO:need error handling and update UI
                    // There will update OMA_Download_Status, or the query will reuse
                    Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDescriptorFile():" +
                            "New url failed" + state.mRequestUri);
                }
                Xlog.i(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDescriptorFile(): "
                        + "URL is " + ddUrl + "file path is " + ddFile);
                
                if (ddFile != null && ddUrl != null) {
                    OmaDescription omaDescription = new OmaDescription();
                    int parseStatus = OmaDownload.parseXml(ddUrl, ddFile, omaDescription);
                    
                    ContentValues values = new ContentValues(); 
                    if (omaDescription != null && parseStatus == OmaStatusHandler.SUCCESS) {
                        // Update downloads.db
                        // Show this is OMA DL
                        values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_SUCCESS);
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG, 1);
                        // Update the parse status to success
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                                Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS);
                        // Update the info. This info will show to user
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_NAME, 
                                omaDescription.getName());
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_VENDOR, 
                                omaDescription.getVendor());
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_SIZE, 
                                omaDescription.getSize());
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_TYPE,
                                omaDescription.getType().get(0));
                        Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadDescriptorFile(): " +
                                "dd file's mimtType is :" + omaDescription.getType().get(0));
                        
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_DESCRIPTION, 
                                omaDescription.getDescription());
  
                        if (omaDescription.getObjectUrl() != null) {
                            Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadDescriptorFile(): " +
                                    "dd file's object url :" + omaDescription.getObjectUrl().toString());
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_OBJECT_URL,
                                    omaDescription.getObjectUrl().toString());
                        }
                        if (omaDescription.getNextUrl() != null) {
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_NEXT_URL, 
                                    omaDescription.getNextUrl().toString());
                            mInfo.mOmaDownloadNextUrl = omaDescription.getNextUrl().toString();
                        }
                        if (omaDescription.getInstallNotifyUrl() != null) {
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_INSTALL_NOTIFY_URL, 
                                    omaDescription.getInstallNotifyUrl().toString());
                            state.mOmaDownloadInsNotifyUrl = omaDescription.getInstallNotifyUrl().toString();
                        }
                        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                        
                        // Note: these class members maybe change to DownloadThread class's local variable.
                        // So, the values can not be modified by DownloadService's updateDownload function.
                        state.mOmaDownload = 1;
                        state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS;
                        
                    } else {
                        Xlog.w(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadDescriptorFile(): " +
                                "parse .dd file failed, error is: " + parseStatus);
                        
                        // Update database
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG, 1);
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, parseStatus);
                        if (omaDescription.getInstallNotifyUrl() != null) {
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_INSTALL_NOTIFY_URL, 
                                    omaDescription.getInstallNotifyUrl().toString());
                        }
                        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                        
                        //Need to install notify
                        state.mOmaDownload = 1;
                        if (omaDescription.getInstallNotifyUrl() != null) {
                            state.mOmaDownloadInsNotifyUrl = omaDescription.getInstallNotifyUrl().toString();
                        }
                        if (parseStatus == OmaStatusHandler.INVALID_DDVERSION) {
                            state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DDVERSION;
                            throw new StopRequestException(Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DDVERSION, 
                                    Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY);
                        } else {
                            state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DESCRIPTOR;
                            throw new StopRequestException(Downloads.Impl.STATUS_BAD_REQUEST, 
                                    Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY);
                        }    
                    }
                }
            }
        }
        
    }
    
    
    /**
     * Fully execute a single download request - setup and send the request, handle the response,
     * and transfer the data to the destination file.
     */
    //private void executeDownload(State state, AndroidHttpClient client, HttpGet request)
    private void executeDownload(State state, DefaultHttpClient client, HttpGet request)
            throws StopRequestException, RetryDownload {
        InnerState innerState = new InnerState();
        byte data[] = new byte[Constants.BUFFER_SIZE];
        HttpResponse response = null;

        setupDestinationFile(state, innerState);
        addRequestHeaders(state, request);

        // skip when already finished; remove after fixing race in 5217390
        if (state.mCurrentBytes == state.mTotalBytes) {
            mDownloadAlreadyCompleted = true;
            Log.i(Constants.TAG, "Skipping initiating request for download " +
                  mInfo.mId + "; already completed");
            return;
        }

        // check just before sending the request to avoid using an invalid connection at all
        checkConnectivity();

        /// M: set innerState and other params. @{
        do {
            innerState.mIsAuthNeeded = false;
            // Add to support Authenticate download
            BasicHttpContext localcontext = new BasicHttpContext();
            WebAddress webAddress = new WebAddress(state.mRequestUri); 
            innerState.mHost = webAddress.getHost();
            
            innerState.mPort = webAddress.getPort();
            innerState.mScheme = webAddress.getScheme();
            if (mInfo.mUsername != null || mInfo.mPassword != null) {
                Xlog.d(Constants.DL_ENHANCE, "DownloadThread:executeLoad: " +
                        "do-while loop: mInfo.mUsername is " + mInfo.mUsername 
                        + " mInfo.mPassword is " + mInfo.mPassword);
                if (innerState.mAuthScheme != HttpAuthHeader.UNKNOWN && innerState.mAuthHeader != null) {
                    
                    client.getCredentialsProvider().setCredentials(
                            new AuthScope(innerState.mHost, -1),
                            new UsernamePasswordCredentials(mInfo.mUsername, mInfo.mPassword));
    
                    if (innerState.mAuthScheme == HttpAuthHeader.BASIC) {
                        // Generate BASIC scheme object and stick it to the
                        // local execution context
                        BasicScheme basicAuth = new BasicScheme();
                        localcontext.setAttribute("preemptive-auth", basicAuth);
    
                        // Add as the first request interceptor
                        client.addRequestInterceptor(new DownloadInfo.PreemptiveAuth(), 0);
                        Xlog.d(Constants.DL_ENHANCE, "Add basic interceptor for BASIC auth scheme ");
                    } else if (innerState.mAuthScheme == HttpAuthHeader.DIGEST) {
                        // Generate DIGEST scheme object, initialize it and
                        // stick it to
                        // the local execution context
                        DigestScheme digestAuth = new DigestScheme();
    
                        // set realm
                        String realm = innerState.mAuthHeader.getRealm();
                        digestAuth.overrideParamter("realm", realm);
                        // set nonce
                        String nonce = innerState.mAuthHeader.getNonce();
                        digestAuth.overrideParamter("nonce", nonce);
                        // set qop
                        String qop = innerState.mAuthHeader.getQop();
                        digestAuth.overrideParamter("qop", qop);
                        // set algorithm
                        String algorithm = innerState.mAuthHeader.getAlgorithm();
                        digestAuth.overrideParamter("algorithm", algorithm);
                        // set opaque
                        String opaque = innerState.mAuthHeader.getOpaque();
                        digestAuth.overrideParamter("opaque", opaque);
    
                        localcontext.setAttribute("preemptive-auth", digestAuth);
    
                        // Add as the first request interceptor
                        client.addRequestInterceptor(new DownloadInfo.PreemptiveAuth(), 0);
                        // Add as the last response interceptor
                        client.addResponseInterceptor(new DownloadInfo.PersistentDigest());
                        Xlog.d(Constants.DL_ENHANCE, "Add digest interceptor for DIGEST auth scheme ");
                    }
                }
            }
    
            // HttpResponse response = sendRequest(state, client, request);
            // Modify to support Authenticate download
            response = sendRequest(state, client, request, innerState, localcontext);
            handleExceptionalStatus(state, innerState, response);
        } while (innerState.mIsAuthNeeded);
        /// @}
        if (Constants.LOGV) {
            Log.v(Constants.TAG, "received response for " + mInfo.mUri);
        }

        processResponseHeaders(state, innerState, response);
        InputStream entityStream = openResponseEntity(state, response);
        transferData(state, innerState, data, entityStream);
        /// M: Add to support DRM
        state.mTotalWriteBytes = state.mCurrentBytes;
    }

    /**
     * Check if current connectivity is valid for this request.
     */
    private void checkConnectivity() throws StopRequestException {
        // checking connectivity will apply current policy
        mPolicyDirty = false;

        int networkUsable = mInfo.checkCanUseNetwork();
        if (networkUsable != DownloadInfo.NETWORK_OK) {
            int status = Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
            if (networkUsable == DownloadInfo.NETWORK_UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(true);
            } else if (networkUsable == DownloadInfo.NETWORK_RECOMMENDED_UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(false);
            }
            throw new StopRequestException(status,
                    mInfo.getLogMessageForNetworkError(networkUsable));
        }
    }

    /**
     * Transfer as much data as possible from the HTTP response to the destination file.
     * @param data buffer to use to read data
     * @param entityStream stream for reading the HTTP response entity
     */
    private void transferData(
            State state, InnerState innerState, byte[] data, InputStream entityStream)
            throws StopRequestException {
        for (;;) {
            int bytesRead = readFromResponse(state, innerState, data, entityStream);
            if (bytesRead == -1) { // success, end of stream already reached
                handleEndOfStream(state, innerState);
                return;
            }

            state.mGotData = true;
            writeDataToDestination(state, data, bytesRead);
            state.mCurrentBytes += bytesRead;
            reportProgress(state, innerState);

            if (Constants.LOGVV) {
                Log.v(Constants.TAG, "downloaded " + state.mCurrentBytes + " for "
                      + mInfo.mUri);
            }

            checkPausedOrCanceled(state);
        }
    }

    /**
     * Called after a successful completion to take any necessary action on the downloaded file.
     */
    private void finalizeDestinationFile(State state) throws StopRequestException {
        if (state.mFilename != null) {
            // make sure the file is readable
            FileUtils.setPermissions(state.mFilename, 0644, -1, -1);
            syncDestination(state);
            
            File file = new File(state.mFilename);
            Xlog.d(Constants.DL_DRM, "finalizeDestinationFile:MimeType is: "  + state.mMimeType +
                    "Total Write Bytes is: " + state.mTotalWriteBytes + "file length is: " + file.length()
                     + ", file.exists(): " + file.exists() + ",file location: " + state.mFilename);
            /// M: support MTK DRM @{
            if (file.length() == state.mTotalWriteBytes) {
                ContentValues values = new ContentValues();
                // If written bytes is not equal to file.length(), don't install DRM file
                if ((Constants.MTK_DRM_ENABLED)
                        && Helpers.isMtkDRMFile(state.mMimeType)) {
                    //DrmManagerClient drmClient = new DrmManagerClient(this.mContext);
                    OmaDrmClient drmClient = new OmaDrmClient(this.mContext);
                    if (Helpers.isMtkDRMFLOrCDFile(state.mMimeType)) {
                        int result = drmClient.installDrmMsg(state.mFilename);
                        Xlog.d(Constants.DL_DRM, "install FLCD result is"  + result + 
                                ",alfter install DRM Msg, new File(state.mFilename).exists(): " + 
                                new File(state.mFilename).exists() + "new File(state.mFilename).length()" +
                                new File(state.mFilename).length());
                        String[] paths = {state.mFilename};
                        String[] mimeTypes = {state.mMimeType};
                        MediaScannerConnection.scanFile(mContext, paths, mimeTypes, null);
                    } else if (Helpers.isMtkDRMRightFile(state.mMimeType)) {
                        try {
                            DrmRights rights = new DrmRights(state.mFilename, state.mMimeType);
                            int result = drmClient.saveRights(rights, null, null);
                            if (result == OmaDrmClient.ERROR_NONE) {
                                /*
                                String strCID = drmClient.getContentIdFromRights(rights);
                                Xlog.d(Constants.DL_DRM, "finalizeDestinationFile:saverights return CID:"
                                        + strCID);
                                DrmUtils.rescanDrmMediaFiles(mContext, strCID, null);
                                */
                                drmClient.rescanDrmMediaFiles(mContext, rights, null);
                            }
                            
                            // Mark for delete for DRM right file
                            values = new ContentValues();
                            values.put(Downloads.Impl.COLUMN_DELETED, 1);
                            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                            Xlog.e(Constants.DL_DRM, "Mark for delete DRM rights file");
                            
                        } catch (IOException e) {
                            Xlog.e(Constants.DL_DRM, "save rights " + state.mFilename + " exception");
                        }
                    }
                    /// M : when file length change after install drm msg, update state.mCurrentBytes. @{
                    if (new File(state.mFilename).length() != state.mTotalWriteBytes) {
                        state.mCurrentBytes = new File(state.mFilename).length();
                        state.mTotalWriteBytes = state.mCurrentBytes;
                    }
                    /// @}
                }
                values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, state.mCurrentBytes);
                Log.v(Constants.TAG, "finalizeDestinationFile(), update total bytes, state.mCurrentBytes: " +
                        state.mCurrentBytes + ", id: " + mInfo.mId);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                Xlog.d(Constants.DL_ENHANCE, "finalizeDestinationFile: " +
                        " Update Total Bytes:"  + state.mCurrentBytes);
            }
            /// @}
        }
    }
    

    /**
     * Called just before the thread finishes, regardless of status, to take any necessary action on
     * the downloaded file.
     */
    private void cleanupDestination(State state, int finalStatus) {
        if (mDrmConvertSession != null) {
            if ((Constants.MTK_DRM_ENABLED)) {
                finalStatus = mDrmConvertSession.close(state.mFilename);
            } else {
                mDrmConvertSession.close(state.mFilename);
            }
        }

        closeDestination(state);
        if (state.mFilename != null && Downloads.Impl.isStatusError(finalStatus)) {
            Slog.d(TAG, "cleanupDestination() deleting " + state.mFilename);
            new File(state.mFilename).delete();
            state.mFilename = null;
        }
    }

    /**
     * Sync the destination file to storage.
     */
    private void syncDestination(State state) {
        FileOutputStream downloadedFileStream = null;
        try {
            downloadedFileStream = new FileOutputStream(state.mFilename, true);
            downloadedFileStream.getFD().sync();
        } catch (FileNotFoundException ex) {
            Log.w(Constants.TAG, "file " + state.mFilename + " not found: " + ex);
        } catch (SyncFailedException ex) {
            Log.w(Constants.TAG, "file " + state.mFilename + " sync failed: " + ex);
        } catch (IOException ex) {
            Log.w(Constants.TAG, "IOException trying to sync " + state.mFilename + ": " + ex);
        } catch (RuntimeException ex) {
            Log.w(Constants.TAG, "exception while syncing file: ", ex);
        } finally {
            if(downloadedFileStream != null) {
                try {
                    downloadedFileStream.close();
                } catch (IOException ex) {
                    Log.w(Constants.TAG, "IOException while closing synced file: ", ex);
                } catch (RuntimeException ex) {
                    Log.w(Constants.TAG, "exception while closing file: ", ex);
                }
            }
        }
    }

    /**
     * Close the destination output stream.
     */
    private void closeDestination(State state) {
        try {
            // close the file
            if (state.mStream != null) {
                state.mStream.close();
                state.mStream = null;
            }
        } catch (IOException ex) {
            if (Constants.LOGV) {
                Log.v(Constants.TAG, "exception when closing the file after download : " + ex);
            }
            // nothing can really be done if the file can't be closed
        }
    }

    /**
     * Check if the download has been paused or canceled, stopping the request appropriately if it
     * has been.
     */
    private void checkPausedOrCanceled(State state) throws StopRequestException {
        synchronized (mInfo) {
            if (mInfo.mControl == Downloads.Impl.CONTROL_PAUSED) {
                Xlog.i(Constants.DL_ENHANCE, "DownloadThread: checkPausedOrCanceled: user pause download");
                throw new StopRequestException(
                        Downloads.Impl.STATUS_PAUSED_BY_APP, "download paused by owner");
            }
            if (mInfo.mStatus == Downloads.Impl.STATUS_CANCELED) {
                throw new StopRequestException(Downloads.Impl.STATUS_CANCELED, "download canceled");
            }
        }

        // if policy has been changed, trigger connectivity check
        if (mPolicyDirty) {
            checkConnectivity();
        }
    }

    /**
     * Report download progress through the database if necessary.
     */
    private void reportProgress(State state, InnerState innerState) {
        final long now = SystemClock.elapsedRealtime();

        final long sampleDelta = now - state.mSpeedSampleStart;
        if (sampleDelta > 500) {
            final long sampleSpeed = ((state.mCurrentBytes - state.mSpeedSampleBytes) * 1000)
                    / sampleDelta;

            if (state.mSpeed == 0) {
                state.mSpeed = sampleSpeed;
            } else {
                state.mSpeed = ((state.mSpeed * 3) + sampleSpeed) / 4;
            }

            state.mSpeedSampleStart = now;
            state.mSpeedSampleBytes = state.mCurrentBytes;

            DownloadHandler.getInstance().setCurrentSpeed(mInfo.mId, state.mSpeed);
        }

        if (state.mCurrentBytes - state.mBytesNotified > Constants.MIN_PROGRESS_STEP &&
            now - state.mTimeLastNotification > Constants.MIN_PROGRESS_TIME) {
            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
            state.mBytesNotified = state.mCurrentBytes;
            state.mTimeLastNotification = now;
        }
    }

    /**
     * Write a data buffer to the destination file.
     * @param data buffer containing the data to write
     * @param bytesRead how many bytes to write from the buffer
     */
    private void writeDataToDestination(State state, byte[] data, int bytesRead)
            throws StopRequestException {
        for (;;) {
            try {
                if (state.mStream == null) {
                    state.mStream = new FileOutputStream(state.mFilename, true);
                }
                mStorageManager.verifySpaceBeforeWritingToFile(mInfo.mDestination, state.mFilename,
                        bytesRead);
                if (!DownloadDrmHelper.isDrmConvertNeeded(mInfo.mMimeType)) {
                    state.mStream.write(data, 0, bytesRead);
                } else {
                    byte[] convertedData = mDrmConvertSession.convert(data, bytesRead);
                    if (convertedData != null) {
                        state.mStream.write(convertedData, 0, convertedData.length);
                    } else {
                        throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                                "Error converting drm data.");
                    }
                }
                return;
            } catch (IOException ex) {
                // couldn't write to file. are we out of space? check.
                // TODO this check should only be done once. why is this being done
                // in a while(true) loop (see the enclosing statement: for(;;)
                if (state.mStream != null) {
                    mStorageManager.verifySpace(mInfo.mDestination, state.mFilename, bytesRead);
                }
            } finally {
                if (mInfo.mDestination == Downloads.Impl.DESTINATION_EXTERNAL) {
                    closeDestination(state);
                }
            }
        }
    }

    /**
     * Called when we've reached the end of the HTTP response stream, to update the database and
     * check for consistency.
     */
    private void handleEndOfStream(State state, InnerState innerState) throws StopRequestException {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
        
        Xlog.d(Constants.DL_ENHANCE, "handleEndOfStream: " +
                "innerState.mHeaderContentLength: " +  innerState.mHeaderContentLength + 
                " state.mCurrentBytes: " + state.mCurrentBytes + 
                " mInfo.mTotalBytes: " + mInfo.mTotalBytes + 
                " state.mTotalBytes: " + state.mTotalBytes);
        
        if (innerState.mHeaderContentLength == null || mInfo.mTotalBytes < 0) {
            values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, state.mCurrentBytes);
            Log.v(Constants.TAG, "handleEndOfStream(), update total bytes, state.mCurrentBytes: " +
                    state.mCurrentBytes + ", id: " + mInfo.mId);
        }
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);

        boolean lengthMismatched = (innerState.mHeaderContentLength != null)
                && (state.mCurrentBytes != Integer.parseInt(innerState.mHeaderContentLength));
        if (lengthMismatched) {
            if (cannotResume(state)) {
                throw new StopRequestException(Downloads.Impl.STATUS_CANNOT_RESUME,
                        "mismatched content length");
            } else {
                throw new StopRequestException(getFinalStatusForHttpError(state),
                        "closed socket before end of file");
            }
        }
    }

    private boolean cannotResume(State state) {
        /// M: return state.mCurrentBytes > 0 && !mInfo.mNoIntegrity && state.mHeaderETag == null;
        /// Extend the resume condition, exclude the Etag influence
        Xlog.d(Constants.DL_ENHANCE, "innerState.mBytesSoFar is: " + 
                state.mCurrentBytes);
        return state.mCurrentBytes < 0;
    }

    /**
     * Read some data from the HTTP response stream, handling I/O errors.
     * @param data buffer to use to read data
     * @param entityStream stream for reading the HTTP response entity
     * @return the number of bytes actually read or -1 if the end of the stream has been reached
     */
    private int readFromResponse(State state, InnerState innerState, byte[] data,
                                 InputStream entityStream) throws StopRequestException {
        try {
            return entityStream.read(data);
        } catch (IOException ex) {
            logNetworkState(mInfo.mUid);
            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
            if (cannotResume(state)) {
                String message = "while reading response: " + ex.toString()
                + ", can't resume interrupted download with no ETag";
                throw new StopRequestException(Downloads.Impl.STATUS_CANNOT_RESUME,
                        message, ex);
            } else {
                /// M: The read() function didn't response until timeout.
                /// But user may click paused button at the moment. 
                /// So change the control status
                synchronized (mInfo) {
                    if (mInfo.mControl == Downloads.Impl.CONTROL_PAUSED) {
                        Xlog.e(Constants.DL_ENHANCE, "Before read response happen exception, " +
                                "user click paused");
                        throw new StopRequestException(
                                Downloads.Impl.STATUS_PAUSED_BY_APP, "download paused by owner");
                    }
                }
                throw new StopRequestException(getFinalStatusForHttpError(state),
                        "while reading response: " + ex.toString(), ex);
            }
        }
    }

    /**
     * Open a stream for the HTTP response entity, handling I/O errors.
     * @return an InputStream to read the response entity
     */
    private InputStream openResponseEntity(State state, HttpResponse response)
            throws StopRequestException {
        try {
            return response.getEntity().getContent();
        } catch (IOException ex) {
            logNetworkState(mInfo.mUid);
            throw new StopRequestException(getFinalStatusForHttpError(state),
                    "while getting entity: " + ex.toString(), ex);
        }
    }

    private void logNetworkState(int uid) {
        if (Constants.LOGX) {
            Log.i(Constants.TAG,
                    "Net " + (Helpers.isNetworkAvailable(mSystemFacade, uid) ? "Up" : "Down"));
        }
    }

    /**
     * Read HTTP response headers and take appropriate action, including setting up the destination
     * file and updating the database.
     */
    private void processResponseHeaders(State state, InnerState innerState, HttpResponse response)
            throws StopRequestException {
        if (state.mContinuingDownload) {
            // ignore response headers on resume requests
            return;
        }

        readResponseHeaders(state, innerState, response);
        if (DownloadDrmHelper.isDrmConvertNeeded(state.mMimeType)) {
            mDrmConvertSession = DrmConvertSession.open(mContext, state.mMimeType);
            if (mDrmConvertSession == null) {
                throw new StopRequestException(Downloads.Impl.STATUS_NOT_ACCEPTABLE, "Mimetype "
                        + state.mMimeType + " can not be converted.");
            }
        }

        state.mFilename = Helpers.generateSaveFile(
                mContext,
                mInfo.mUri,
                mInfo.mHint,
                innerState.mHeaderContentDisposition,
                innerState.mHeaderContentLocation,
                state.mMimeType,
                mInfo.mDestination,
                (innerState.mHeaderContentLength != null) ?
                        Long.parseLong(innerState.mHeaderContentLength) : 0,
                /// M: Modify to support CU customization @{
                mInfo.mIsPublicApi, mStorageManager, 
                mInfo.mContinueDownload,
                mInfo.mPackage, 
                mInfo.mDownloadPath);
                /// @}
        try {
            state.mStream = new FileOutputStream(state.mFilename);
        } catch (FileNotFoundException exc) {
            throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                    "while opening destination file: " + exc.toString(), exc);
        }
        //if (Constants.LOGV) {
            Log.v(Constants.TAG, "writing " + mInfo.mUri + " to " + state.mFilename);
        //}

        updateDatabaseFromHeaders(state, innerState);
        // check connectivity again now that we know the total size
        checkConnectivity();
    }

    /**
     * Update necessary database fields based on values of HTTP response headers that have been
     * read.
     */
    private void updateDatabaseFromHeaders(State state, InnerState innerState) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl._DATA, state.mFilename);
        if (state.mHeaderETag != null) {
            values.put(Constants.ETAG, state.mHeaderETag);
        }
        if (state.mMimeType != null) {
            values.put(Downloads.Impl.COLUMN_MIME_TYPE, state.mMimeType);
        }
        values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, mInfo.mTotalBytes);
        Log.v(Constants.TAG, "updateDatabaseFromHeaders(), update total bytes, mInfo.mTotalBytes: " +
                mInfo.mTotalBytes + ", id: " + mInfo.mId);
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
    }

    /**
     * Read headers from the HTTP response and store them into local state.
     */
    private void readResponseHeaders(State state, InnerState innerState, HttpResponse response)
            throws StopRequestException {
        Header header = response.getFirstHeader("Content-Disposition");
        if (header != null) {
            innerState.mHeaderContentDisposition = header.getValue();
        }
        header = response.getFirstHeader("Content-Location");
        if (header != null) {
            innerState.mHeaderContentLocation = header.getValue();
        }
        if (state.mMimeType == null) {
            header = response.getFirstHeader("Content-Type");
            if (header != null) {
                state.mMimeType = Intent.normalizeMimeType(header.getValue());
            }
        }
        header = response.getFirstHeader("ETag");
        if (header != null) {
            state.mHeaderETag = header.getValue();
        }
        String headerTransferEncoding = null;
        header = response.getFirstHeader("Transfer-Encoding");
        if (header != null) {
            headerTransferEncoding = header.getValue();
        }
        if (headerTransferEncoding == null) {
            header = response.getFirstHeader("Content-Length");
            if (header != null) {
                innerState.mHeaderContentLength = header.getValue();
		Log.v(Constants.TAG, "mInfo.mTotalBytes: " + mInfo.mTotalBytes);
                state.mTotalBytes = mInfo.mTotalBytes =
                        Long.parseLong(innerState.mHeaderContentLength);
		Log.v(Constants.TAG, "mInfo.mTotalBytes: " + mInfo.mTotalBytes);
            }
        } else {
            // Ignore content-length with transfer-encoding - 2616 4.4 3
            if (Constants.LOGVV) {
                Log.v(Constants.TAG,
                        "ignoring content-length because of xfer-encoding");
            }
        }
       // if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Content-Disposition: " +
                    innerState.mHeaderContentDisposition);
            Log.v(Constants.TAG, "Content-Length: " + innerState.mHeaderContentLength);
            Log.v(Constants.TAG, "Content-Location: " + innerState.mHeaderContentLocation);
            Log.v(Constants.TAG, "Content-Type: " + state.mMimeType);
            Log.v(Constants.TAG, "ETag: " + state.mHeaderETag);
            Log.v(Constants.TAG, "Transfer-Encoding: " + headerTransferEncoding);
       // }

        boolean noSizeInfo = innerState.mHeaderContentLength == null
                && (headerTransferEncoding == null
                    || !headerTransferEncoding.equalsIgnoreCase("chunked"));
        if (!mInfo.mNoIntegrity && noSizeInfo) {
            throw new StopRequestException(Downloads.Impl.STATUS_HTTP_DATA_ERROR,
                    "can't know size of download, giving up");
        }
        
        /// M: Add this for OMA_DL
        /// OMA_DL HLD: 4.4 Installation Failure: in the case of retrieval errors
        /// If MimeType is not same with .dd file description, throw exception ATTRIBUTE_MISMATCH exception
        /// && !state.mMimeType.equals("audio/mp3") @{
        if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT && state.mOmaDownload == 1 && 
                !state.mMimeType.equalsIgnoreCase("application/vnd.oma.dd+xml")) {
            header = response.getFirstHeader("Content-Type");
            if (header != null) {
                String mimeType = sanitizeMimeType(header.getValue());
                Xlog.d(Constants.LOG_OMA_DL, "DownloadThread:readResponseHeader():" +
                        " header mimeType is:" + mimeType 
                        + "state.mMimeType is :" + state.mMimeType);
                
                
                if (Helpers.isMtkDRMFile(mimeType)) {
                    state.mMimeType = mimeType;
                    return;
                }
                
                if (((state.mMimeType.equals("audio/mp3") || state.mMimeType.equals("audio/mpeg")) &&
                        (mimeType.equals("audio/mp3") || mimeType.equals("audio/mpeg")))) {
                    return;
                } 
                
                // This means ATTRIBUTE_MISMATCH
                if (!mimeType.equals(state.mMimeType)) {              
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS,
                            Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH);
                    mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                    
                    state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH;
                    throw new StopRequestException(Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH,
                            Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY);
                }
            } 
        }
        /// @}
    }

    /**
     * Check the HTTP response status and handle anything unusual (e.g. not 200/206).
     */
    private void handleExceptionalStatus(State state, InnerState innerState, HttpResponse response)
            throws StopRequestException, RetryDownload {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 503 && mInfo.mNumFailed < Constants.MAX_RETRIES) {
            handleServiceUnavailable(state, response);
        }
        if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307) {
            handleRedirect(state, response, statusCode);
        }
        /// M: Add this to support Authenticate Download @{
        if (statusCode == Downloads.Impl.STATUS_NEED_HTTP_AUTH) {
            if ((mInfo.mUsername != null || mInfo.mPassword != null) 
                    && innerState.mAuthScheme == HttpAuthHeader.UNKNOWN 
                    && innerState.mAuthHeader == null) {
                Header header = response.getFirstHeader("WWW-Authenticate");
                if (header != null) {
                    String headerAuthString = header.getValue();
                    Xlog.d(Constants.DL_ENHANCE, "response.getFirstHeader WWW-Authenticate is: " 
                            + headerAuthString);
                    //Using HttpAuthHeader parse Basic Auth.
                    //Only use first Auth header tag.
                    innerState.mAuthHeader = new HttpAuthHeader(headerAuthString);
                    
                    if (innerState.mAuthHeader != null) {
                       if (innerState.mAuthHeader.getScheme() == HttpAuthHeader.BASIC)
                       {
                           innerState.mAuthScheme = HttpAuthHeader.BASIC;
                       } else if (innerState.mAuthHeader.getScheme() ==  HttpAuthHeader.DIGEST) {
                           innerState.mAuthScheme = HttpAuthHeader.DIGEST;
                       }
                       Xlog.d(Constants.DL_ENHANCE, "Auth scheme and mAuthHeader.scheme is  " 
                               + innerState.mAuthScheme);
                       innerState.mIsAuthNeeded = true;
                       return;
                    }   
                } 
                
            } else {
                Xlog.w(Constants.DL_ENHANCE, "DownloadThread: handleExceptionalStatus:" +
                        " 401, need Authenticate ");
                throw new StopRequestException(Downloads.Impl.STATUS_NEED_HTTP_AUTH, "http error " + statusCode);
            }
            //handleAuthenticate(state, response, statusCode);
        }
        /// @}
        
        if (Constants.LOGV) {
            Log.i(Constants.TAG, "recevd_status = " + statusCode +
                    ", mContinuingDownload = " + state.mContinuingDownload);
        }

        /// M: add to retry url when got 200 code. @{
        if (statusCode == 200 && state.mContinuingDownload && mInfo.mNumFailed < Constants.MAX_RETRIES)    {
            state.mCountRetry = true;
            throw new StopRequestException(Downloads.Impl.STATUS_WAITING_TO_RETRY,
                    "got 200 status code when resume download, will retry later");
        }
        /// @}
        int expectedStatus = state.mContinuingDownload ? 206 : Downloads.Impl.STATUS_SUCCESS;
        if (statusCode != expectedStatus) {
            handleOtherStatus(state, innerState, statusCode);
        }
    }

    /**
     * Handle a status that we don't know how to deal with properly.
     */
    private void handleOtherStatus(State state, InnerState innerState, int statusCode)
            throws StopRequestException {
        if (statusCode == 416) {
            // range request failed. it should never fail.
            throw new IllegalStateException("Http Range request failure: totalBytes = " +
                    state.mTotalBytes + ", bytes recvd so far: " + state.mCurrentBytes);
        }
        int finalStatus;
        if (Downloads.Impl.isStatusError(statusCode)) {
            finalStatus = statusCode;
        } else if (statusCode >= 300 && statusCode < 400) {
            finalStatus = Downloads.Impl.STATUS_UNHANDLED_REDIRECT;
        } else if (state.mContinuingDownload && statusCode == Downloads.Impl.STATUS_SUCCESS) {
            finalStatus = Downloads.Impl.STATUS_CANNOT_RESUME;
        } else {
            finalStatus = Downloads.Impl.STATUS_UNHANDLED_HTTP_CODE;
        }
        throw new StopRequestException(finalStatus, "http error " +
                statusCode + ", mContinuingDownload: " + state.mContinuingDownload);
    }

    /**
     * Handle a 3xx redirect status.
     */
    private void handleRedirect(State state, HttpResponse response, int statusCode)
            throws StopRequestException, RetryDownload {
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "got HTTP redirect " + statusCode);
        }
        if (state.mRedirectCount >= Constants.MAX_REDIRECTS) {
            throw new StopRequestException(Downloads.Impl.STATUS_TOO_MANY_REDIRECTS,
                    "too many redirects");
        }
        Header header = response.getFirstHeader("Location");
        if (header == null) {
            return;
        }
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Location :" + header.getValue());
        }

        String newUri;
        try {
            newUri = new URI(mInfo.mUri).resolve(new URI(header.getValue())).toString();
        } catch(URISyntaxException ex) {
            if (Constants.LOGV) {
                Log.d(Constants.TAG, "Couldn't resolve redirect URI " + header.getValue()
                        + " for " + mInfo.mUri);
            }
            throw new StopRequestException(Downloads.Impl.STATUS_HTTP_DATA_ERROR,
                    "Couldn't resolve redirect URI");
        }
        ++state.mRedirectCount;
        state.mRequestUri = newUri;
        if (statusCode == 301 || statusCode == 303) {
            // use the new URI for all future requests (should a retry/resume be necessary)
            state.mNewUri = newUri;
        }
        throw new RetryDownload();
    }

    /**
     * Handle a 503 Service Unavailable status by processing the Retry-After header.
     */
    private void handleServiceUnavailable(State state, HttpResponse response)
            throws StopRequestException {
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "got HTTP response code 503");
        }
        state.mCountRetry = true;
        Header header = response.getFirstHeader("Retry-After");
        if (header != null) {
           try {
               if (Constants.LOGVV) {
                   Log.v(Constants.TAG, "Retry-After :" + header.getValue());
               }
               state.mRetryAfter = Integer.parseInt(header.getValue());
               if (state.mRetryAfter < 0) {
                   state.mRetryAfter = 0;
               } else {
                   if (state.mRetryAfter < Constants.MIN_RETRY_AFTER) {
                       state.mRetryAfter = Constants.MIN_RETRY_AFTER;
                   } else if (state.mRetryAfter > Constants.MAX_RETRY_AFTER) {
                       state.mRetryAfter = Constants.MAX_RETRY_AFTER;
                   }
                   state.mRetryAfter += Helpers.sRandom.nextInt(Constants.MIN_RETRY_AFTER + 1);
                   state.mRetryAfter *= 1000;
               }
           } catch (NumberFormatException ex) {
               // ignored - retryAfter stays 0 in this case.
           }
        }
        throw new StopRequestException(Downloads.Impl.STATUS_WAITING_TO_RETRY,
                "got 503 Service Unavailable, will retry later");
    }

    /**
     * Send the request to the server, handling any I/O exceptions.
     */
    private HttpResponse sendRequest(State state, DefaultHttpClient client, HttpGet request,
            InnerState innerState, BasicHttpContext localcontext) throws StopRequestException {
        try {
            return client.execute(request);
        } catch (IllegalArgumentException ex) {
            throw new StopRequestException(Downloads.Impl.STATUS_HTTP_DATA_ERROR,
                    "while trying to execute request: " + ex.toString(), ex);
        } catch (IOException ex) {
            logNetworkState(mInfo.mUid);
            throw new StopRequestException(getFinalStatusForHttpError(state),
                    "while trying to execute request: " + ex.toString(), ex);
        }
    }

    private int getFinalStatusForHttpError(State state) {
        int networkUsable = mInfo.checkCanUseNetwork();
        if (networkUsable != DownloadInfo.NETWORK_OK) {
            switch (networkUsable) {
                case DownloadInfo.NETWORK_UNUSABLE_DUE_TO_SIZE:
                case DownloadInfo.NETWORK_RECOMMENDED_UNUSABLE_DUE_TO_SIZE:
                    return Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                default:
                    return Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
            }
        } else if (mInfo.mNumFailed < Constants.MAX_RETRIES) {
            state.mCountRetry = true;
            return Downloads.Impl.STATUS_WAITING_TO_RETRY;
        } else {
            Log.w(Constants.TAG, "reached max retries for " + mInfo.mId);
            return Downloads.Impl.STATUS_HTTP_DATA_ERROR;
        }
    }

    /**
     * Prepare the destination file to receive data.  If the file already exists, we'll set up
     * appropriately for resumption.
     */
    private void setupDestinationFile(State state, InnerState innerState)
            throws StopRequestException {
        Xlog.d(Constants.TAG, "setupDestinationFile(): state.mFilename :" + state.mFilename); 
        if (!TextUtils.isEmpty(state.mFilename)) { // only true if we've already run a thread for this download
            if (Constants.LOGV) {
                Log.i(Constants.TAG, "have run thread before for id: " + mInfo.mId +
                        ", and state.mFilename: " + state.mFilename);
            }
            if (!Helpers.isFilenameValid(state.mFilename,
                    mStorageManager.getDownloadDataDirectory())) {
                // this should never happen
                throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                        "found invalid internal destination filename");
            }
            
            Xlog.d(Constants.TAG, "new File(state.mFilename).exists: " + new File(state.mFilename).exists() + "," +
            		"length: " + new File(state.mFilename).length());
            // We're resuming a download that got interrupted
            File f = new File(state.mFilename);
            if (f.exists()) {
                if (Constants.LOGV) {
                    Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                            ", and state.mFilename: " + state.mFilename);
                }
                long fileLength = f.length();

                /// M: modify to fix tablet cts testDwonloadManagerDestination case fail [ALPS00357624] @{
                // Because CTS test file is 0 bytes, the file will be deleted in this.
                // So add "state.mCurrentBytes != state.mTotalBytes". If they equal, it means
                // the predownload is complete.
                Xlog.i(Constants.DL_ENHANCE, "setupDestinationFile: file " + state.mFilename +
                        " exsit. File length is: " + fileLength +
                        "state.mCurrentBytes: " + state.mCurrentBytes +
                        "state.mTotalBytes: " + state.mTotalBytes);
                if (fileLength == 0 && (state.mCurrentBytes != state.mTotalBytes)) {
                /// @}
                    // The download hadn't actually started, we can restart from scratch
                    Slog.d(TAG, "setupDestinationFile() found fileLength=0, deleting "
                            + state.mFilename);
                    f.delete();
                    state.mFilename = null;
                    /// M: modify to fix tablet cts testDwonloadManagerDestination case fail [ALPS00357624] @{
                    //if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                                ", BUT starting from scratch again: Delete file: " + state.mFilename);
                    //}
                    /// @}
                /*
                } else if (mInfo.mETag == null && !mInfo.mNoIntegrity) {
                    // This should've been caught upon failure
                    Slog.d(TAG, "setupDestinationFile() unable to resume download, deleting "
                            + state.mFilename);
                    f.delete();
                    throw new StopRequestException(Downloads.Impl.STATUS_CANNOT_RESUME,
                            "Trying to resume a download that can't be resumed");
                */
                } else {
                    Log.v(Constants.TAG, "Can resume download");
                    // All right, we'll be able to resume this download
                    if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                                ", and starting with file of length: " + fileLength);
                    }
                    try {
                        state.mStream = new FileOutputStream(state.mFilename, true);
                    } catch (FileNotFoundException exc) {
                        throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                                "while opening destination for resuming: " + exc.toString(), exc);
                    }
                    state.mCurrentBytes = (int) fileLength;
                    if (mInfo.mTotalBytes != -1) {
                        innerState.mHeaderContentLength = Long.toString(mInfo.mTotalBytes);
                    }
                    state.mHeaderETag = mInfo.mETag;
                    state.mContinuingDownload = true;
                    if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                                ", state.mCurrentBytes: " + state.mCurrentBytes +
                                ", and setting mContinuingDownload to true: ");
                    }
                }
            } else {
                /// M: Add to to fix [ALPS00423697]. @{
                state.mCurrentBytes = 0;
                ContentValues values = new ContentValues();
                values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
               /// @}
            }
            Xlog.d(Constants.TAG, "state.mCurrentBytes : " + state.mCurrentBytes);
        }

        if (state.mStream != null && mInfo.mDestination == Downloads.Impl.DESTINATION_EXTERNAL) {
            closeDestination(state);
        }
    }

    /**
     * Add custom headers for this download to the HTTP request.
     */
    private void addRequestHeaders(State state, HttpGet request) {
        for (Pair<String, String> header : mInfo.getHeaders()) {
            request.addHeader(header.first, header.second);
        }

        if (state.mContinuingDownload) {
            /*
            if (state.mHeaderETag != null) {
                 request.addHeader("If-Match", state.mHeaderETag);  CR:269281
            }*/
            request.addHeader("Range", "bytes=" + state.mCurrentBytes + "-");
            if (Constants.LOGV) {
                Log.i(Constants.TAG, "Adding Range header: " +
                        "bytes=" + state.mCurrentBytes + "-");
                Log.i(Constants.TAG, "  totalBytes = " + state.mTotalBytes);
            }
	    Xlog.d(Constants.TAG, "Adding Range header: " +
                        "bytes=" + state.mCurrentBytes + "-");
        }
    }

    /**
     * Stores information about the completed download, and notifies the initiating application.
     */
    private void notifyDownloadCompleted(
            int status, boolean countRetry, int retryAfter, boolean gotData,
            String filename, String uri, String mimeType, String errorMsg) {
        notifyThroughDatabase(
                status, countRetry, retryAfter, gotData, filename, uri, mimeType,
                errorMsg);
        if (Downloads.Impl.isStatusCompleted(status) && !mDownloadAlreadyCompleted) {
            mInfo.sendIntentIfRequested();
        }
    }

    private void notifyThroughDatabase(
            int status, boolean countRetry, int retryAfter, boolean gotData,
            String filename, String uri, String mimeType, String errorMsg) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_STATUS, status);
        values.put(Downloads.Impl._DATA, filename);
        if (uri != null) {
            values.put(Downloads.Impl.COLUMN_URI, uri);
        }
        values.put(Downloads.Impl.COLUMN_MIME_TYPE, mimeType);
        values.put(Downloads.Impl.COLUMN_LAST_MODIFICATION, mSystemFacade.currentTimeMillis());
        values.put(Constants.RETRY_AFTER_X_REDIRECT_COUNT, retryAfter);
        if (!countRetry) {
            values.put(Constants.FAILED_CONNECTIONS, 0);
        } else if (gotData) {
            values.put(Constants.FAILED_CONNECTIONS, 1);
        } else {
            values.put(Constants.FAILED_CONNECTIONS, mInfo.mNumFailed + 1);
        }
        // save the error message. could be useful to developers.
        if (!TextUtils.isEmpty(errorMsg)) {
            values.put(Downloads.Impl.COLUMN_ERROR_MSG, errorMsg);
        }
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
    }

    /**
     * M: Clean up a mimeType string so it can be used to dispatch an intent to
     * view a downloaded asset.
     * @param mimeType either null or one or more mime types (semi colon separated).
     * @return null if mimeType was null. Otherwise a string which represents a
     * single mimetype in lowercase and with surrounding whitespaces trimmed.
     */
    private static String sanitizeMimeType(String mimeType) {
        try {
            mimeType = mimeType.trim().toLowerCase(Locale.ENGLISH);

            final int semicolonIndex = mimeType.indexOf(';');
            if (semicolonIndex != -1) {
                mimeType = mimeType.substring(0, semicolonIndex);
            }
            return mimeType;
        } catch (NullPointerException npe) {
            return null;
        }
    }

    private INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        @Override
        public void onUidRulesChanged(int uid, int uidRules) {
            // caller is NPMS, since we only register with them
            if (uid == mInfo.mUid) {
                mPolicyDirty = true;
            }
        }

        @Override
        public void onMeteredIfacesChanged(String[] meteredIfaces) {
            // caller is NPMS, since we only register with them
            mPolicyDirty = true;
        }

        @Override
        public void onRestrictBackgroundChanged(boolean restrictBackground) {
            // caller is NPMS, since we only register with them
            mPolicyDirty = true;
        }
    };
}
