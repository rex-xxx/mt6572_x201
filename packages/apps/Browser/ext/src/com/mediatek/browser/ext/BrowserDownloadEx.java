package com.mediatek.browser.ext;

import android.app.DownloadManager.Request;

import com.mediatek.xlog.Xlog;

public class BrowserDownloadEx implements IBrowserDownloadEx {
    private static final String TAG = "BrowserPluginEx";
    @Override
    public boolean setRequestDestinationDir(String downloadPath,
            Request mRequest, String filename, String mimeType) {
        Xlog.i(TAG, "Enter: " + "setRequestDestinationDir" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldShowDownloadOrOpenContent() {
        //
        Xlog.i(TAG, "Enter: " + "shouldShowDownloadOrOpenContent" + " --default implement");
        return false;
    }
    
    @Override
    public boolean shouldShowToastWithFileSize() {
        Xlog.i(TAG, "Enter: " + "shouldShowToastWithFileSize" + " --default implement");
        return false;
    }
}