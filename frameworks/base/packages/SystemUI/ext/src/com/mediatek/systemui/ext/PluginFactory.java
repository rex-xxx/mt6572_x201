package com.mediatek.systemui.ext;

import android.content.Context;

import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.Plugin.ObjectCreationException;

/**
 * M: Plug-in helper class as the facade for accessing related add-ons.
 */
public class PluginFactory {
    private static IStatusBarPlugin mStatusBarPlugin = null;

    public static synchronized IStatusBarPlugin getStatusBarPlugin(Context context) {
        if (mStatusBarPlugin == null) {
            try {
                mStatusBarPlugin = (IStatusBarPlugin) PluginManager.createPluginObject(
                        context, IStatusBarPlugin.class.getName(), "1.0.0", Plugin.DEFAULT_HANDLER_NAME);
            } catch (ObjectCreationException e) {
                mStatusBarPlugin = new DefaultStatusBarPlugin(context);
            }
        }
        return mStatusBarPlugin;
    }
}
