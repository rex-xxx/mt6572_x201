package com.mediatek.browser.ext;

import android.app.DownloadManager.Request;

public interface IBrowserDownloadEx {
    /**
     * OP01/OP02 Feature
     * Usage location: DownloadHander.java
     * check whether need show ShowDownloadOrOpenContent dialog
     * @param pref                                the pref value to be set
     * @param fontFamily                          the pref value
     */
    boolean shouldShowDownloadOrOpenContent();

    /**
     * OP01/OP02 Feature
     * Usage location: DownloadHander.java FetchUrlMimeType.java
     * Set the dir to download.
     */
    boolean setRequestDestinationDir(String downloadPath, Request mRequest,
            String filename, String mimeType);
    
    /**
     * OP01 Feature
     * Usage location:DownloadHandler.java
     * show a toast with file size
     */
    boolean shouldShowToastWithFileSize();
}