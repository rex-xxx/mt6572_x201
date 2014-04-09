package com.mediatek.camera.ext;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

public class ExtensionHelper {
    private static final String TAG = "ExtensionHelper";
    private static final boolean LOG = true;
    private static ICameraExtension sCameraExtension;
    private static IAppGuideExt sAppGuideExt;

    public static IFeatureExtension getFeatureExtension() {
        if (sCameraExtension == null) {
            throw new IllegalStateException("Please call ensureCameraExtension before getFeatureExtension");
        }
        IFeatureExtension feature = sCameraExtension.getFeatureExtension();
        return feature;
    }

    public static void ensureCameraExtension(Context context) {
        if (sCameraExtension == null) {
            try {
                sCameraExtension = (ICameraExtension) PluginManager.createPluginObject(context.getApplicationContext(),
                        ICameraExtension.class.getName());
            } catch (Plugin.ObjectCreationException e) {
                sCameraExtension = new CameraExtension();
                Log.w(TAG, "ensureCameraExtension()", e);
            }
        }
        if (LOG) {
            Log.v(TAG, "ensureCameraExtension() sCameraExtension=" + sCameraExtension);
        }
    }
    
    public static void showAppGuide(Activity activity) {
        final String type = "CAMERA";
        if (sAppGuideExt == null) {
            try {
                sAppGuideExt = (IAppGuideExt)PluginManager.createPluginObject(activity, IAppGuideExt.class.getName());
            } catch (Plugin.ObjectCreationException e) {
                sAppGuideExt = new AppGuideExt();
                Log.w(TAG, "showAppGuide()", e);
            }
        }
        if (sAppGuideExt != null) {
            sAppGuideExt.showCameraGuide(activity, type);
        }
        if (LOG) {
            Log.v(TAG, "showAppGuide() sAppGuideExt=" + sAppGuideExt);
        }
    }
}
