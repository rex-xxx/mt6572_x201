package com.mediatek.browser.ext;

import android.content.Context;
import android.webkit.WebView;

import com.mediatek.xlog.Xlog;

public class BrowserProcessNetworkEx implements IBrowserProcessNetworkEx {
    private static final String TAG = "BrowserPluginEx";

    @Override
    public boolean shouldProcessNetworkNotify(boolean isNetworkUp) {
        Xlog.i(TAG, "Enter: " + "shouldProcessNetworkNotify" + " --default implement");
        return false;
    }

    @Override
    public void processNetworkNotify(WebView view, Context context, boolean isNetworkUp,
            IBrowserControllerEx mBrowserControllerEx) {
        Xlog.i(TAG, "Enter: " + "processNetworkNotify" + " --default implement");
    }
}