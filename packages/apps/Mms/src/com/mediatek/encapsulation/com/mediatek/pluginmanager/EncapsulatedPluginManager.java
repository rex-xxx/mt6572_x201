
package com.mediatek.encapsulation.com.mediatek.pluginmanager;

import android.content.pm.Signature;
import android.content.Context;
import android.util.AndroidException;

import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedPluginManager {

    /**
     * A simple API to create the plugin implementation which its class name
     * defined as value of meta-data, "class", in the first plug-in APP.
     * 
     * @param context The Context of host APP, through wihch it can access the
     *            current theme, resources, etc.
     * @param pluginIntent The intent action string that host APP defined.
     * @param signatures The signature that plug-in APP should be signed. It is
     *            optional.
     */
    public static Object createPluginObject(Context context, String pluginIntent,
            Signature... signatures) throws AndroidException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return PluginManager.createPluginObject(context, pluginIntent);
        } else {
            /** M: Can not complete for this branch. */
            throw new AndroidException();
        }
    }

}
