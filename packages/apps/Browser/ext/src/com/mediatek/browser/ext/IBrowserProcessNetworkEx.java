package com.mediatek.browser.ext;

import android.content.Context;
import android.webkit.WebView;

public interface IBrowserProcessNetworkEx {
    boolean shouldProcessNetworkNotify(boolean isNetworkUp);
    void processNetworkNotify(WebView view, Context context,
            boolean isNetworkUp, IBrowserControllerEx mBrowserControllerEx);
}