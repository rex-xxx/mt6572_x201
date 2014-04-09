package com.mediatek.calendar.extension;

import android.content.Context;

import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.ext.DefaultLunarExtension;
import com.mediatek.calendar.ext.ILunarExtension;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

/**
 * M: this class is a factory to produce the operator plug-in object.
 */
public class OPExtensionFactory {

    private static final String TAG = "OPExtensionFactory";

    private static ILunarExtension sLunarExtension;
    /**
     * The Lunar Extension is an Single instance. it would hold the ApplicationContext, and
     * alive within the whole Application.
     * @param context PluginManager use it to retrieve the plug-in object
     * @return the single instance of Lunar Extension
     */
    public static ILunarExtension getLunarExtension(Context context) {
        if (sLunarExtension == null) {
            try {
                sLunarExtension = (ILunarExtension)PluginManager.createPluginObject(
                        context.getApplicationContext(), ILunarExtension.class.getName());
                LogUtil.i(TAG, "use lunar plugin");
            } catch (Plugin.ObjectCreationException e) {
                LogUtil.i(TAG, "get plugin failed, use default");
                sLunarExtension = new DefaultLunarExtension();
            }
        }
        return sLunarExtension;
    }
}
