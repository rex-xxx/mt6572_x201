package com.mediatek.browser.ext;

import android.app.Activity;

import com.mediatek.xlog.Xlog;

public class BrowserSmsHandlerEx implements IBrowserSmsHandlerEx {
    private static final String TAG = "BrowserPluginEx";
    @Override
    public SmsHandler createSmsHandler(Activity mActivity) {
        Xlog.i(TAG, "Enter: " + "createSmsHandler" + " --default implement");
        return null;
    }
}