package com.mediatek.downloadmanager.ext;

import android.content.Context;

import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

public class Extensions {
    private static IDownloadProviderFeatureEx sPlugin = null;
    public static IDownloadProviderFeatureEx getDefault(Context context) {
        if (sPlugin == null) {
            try {
                sPlugin = (IDownloadProviderFeatureEx)PluginManager.createPluginObject(context,
                        IDownloadProviderFeatureEx.class.getName());
            } catch (Plugin.ObjectCreationException e) {
                sPlugin = new DownloadProviderFeatureEx(context);
            }
        }
        return sPlugin;
    }
}