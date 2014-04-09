package com.mediatek.browser.ext;

import android.content.Context;

import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

public class Extensions {
    private static volatile IBrowserSmallFeatureEx sSmallFeaturePlugin = null;
    private static volatile IBrowserDownloadEx sDownloadPlugin = null;
    private static volatile IBrowserProcessNetworkEx sProcessNetworkPlugin = null;
    private static volatile IBrowserSmsHandlerEx sSmsHandlerPlugin = null;

    private Extensions(){};
    public static IBrowserSmallFeatureEx getSmallFeaturePlugin(Context context) {
        if (sSmallFeaturePlugin == null) {
            synchronized (Extensions.class) {
                if (sSmallFeaturePlugin == null){
                    try {
                        sSmallFeaturePlugin = (IBrowserSmallFeatureEx)PluginManager.createPluginObject(context,
                                IBrowserSmallFeatureEx.class.getName());
                    } catch (Plugin.ObjectCreationException e) {
                        sSmallFeaturePlugin = new BrowserSmallFeatureEx(context);
                    }
                }
            }
        }
        return sSmallFeaturePlugin;
    }

    public static IBrowserDownloadEx getDownloadPlugin(Context context) {
        if (sDownloadPlugin == null) {
            synchronized (Extensions.class) {
                if (sDownloadPlugin == null) {
                    try {
                        sDownloadPlugin = (IBrowserDownloadEx)PluginManager.createPluginObject(context,
                                IBrowserDownloadEx.class.getName());
                    } catch (Plugin.ObjectCreationException e) {
                        sDownloadPlugin = new BrowserDownloadEx();
                    }
                }
            }
        }
        return sDownloadPlugin;
    }

    public static IBrowserProcessNetworkEx getProcessNetworkPlugin(Context context) {
        if (sProcessNetworkPlugin == null) {
            synchronized (Extensions.class) {
                if (sProcessNetworkPlugin == null) {
                    try {
                        sProcessNetworkPlugin = (IBrowserProcessNetworkEx)PluginManager.createPluginObject(context,
                                IBrowserProcessNetworkEx.class.getName());
                    } catch (Plugin.ObjectCreationException e) {
                        sProcessNetworkPlugin = new BrowserProcessNetworkEx();
                    }
                }
            }
        }
        return sProcessNetworkPlugin;
    }

    public static IBrowserSmsHandlerEx getSmsHandlerPlugin(Context context) {
        if (sSmsHandlerPlugin == null) {
            synchronized (Extensions.class) {
                if (sSmsHandlerPlugin == null) {
                    try {
                        sSmsHandlerPlugin = (IBrowserSmsHandlerEx)PluginManager.createPluginObject(context,
                                IBrowserSmsHandlerEx.class.getName());
                    } catch (Plugin.ObjectCreationException e) {
                        sSmsHandlerPlugin = new BrowserSmsHandlerEx();
                    }
                }
            }
        }
        return sSmsHandlerPlugin;
    }
}