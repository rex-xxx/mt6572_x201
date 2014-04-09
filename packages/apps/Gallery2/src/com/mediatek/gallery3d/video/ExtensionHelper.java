package com.mediatek.gallery3d.video;

import android.content.Context;

import com.android.gallery3d.app.MovieActivity;
import com.mediatek.gallery3d.ext.ActivityHookerGroup;
import com.mediatek.gallery3d.ext.IActivityHooker;
import com.mediatek.gallery3d.ext.IMovieDrmExtension;
import com.mediatek.gallery3d.ext.IMovieExtension;
import com.mediatek.gallery3d.ext.IMovieStrategy;
import com.mediatek.gallery3d.ext.MovieDrmExtension;
import com.mediatek.gallery3d.ext.MovieExtension;
import com.mediatek.gallery3d.ext.MovieStrategy;
import com.mediatek.gallery3d.ext.MtkLog;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

import java.util.ArrayList;
import java.util.List;

public class ExtensionHelper {
    private static final String TAG = "Gallery2/VideoPlayer/ExtensionHelper";
    private static final boolean LOG = true;
    
    private static List<IMovieExtension> sMovieExtensions;
    private static void ensureMovieExtension(final Context context) {
        if (sMovieExtensions == null) {
            sMovieExtensions = new ArrayList<IMovieExtension>();
            boolean find = false;
            final PluginManager<IMovieExtension> pm = PluginManager.<IMovieExtension>create(
                    context, IMovieExtension.class.getName());
            for (int i = 0, count = pm.getPluginCount(); i < count; i++) {
                final Plugin<IMovieExtension> plugin = pm.getPlugin(i);
                try {
                    final IMovieExtension ext = plugin.createObject();
                    if (ext != null) {
                        if (LOG) {
                            MtkLog.v(TAG, "ensureMovieExtension() plugin[" + i + "]=" + ext);
                        }
                        sMovieExtensions.add(ext);
                        find = true;
                    }
                } catch (final Plugin.ObjectCreationException e1) {
                    //ingore any plugin creation exception.
                    MtkLog.w(TAG, "Cannot create plugin object.", e1);
                }
            }
            if (!find) { //add default implemetation
                sMovieExtensions.add(new MovieExtension());
            }
        }
    }

    public static IActivityHooker getHooker(final Context context) {
        ensureMovieExtension(context);
        final ActivityHookerGroup group = new ActivityHookerGroup();
        if (!(ExtensionHelper.getMovieStrategy(context).shouldEnableRewindAndForward())) {
            group.addHooker(new StopVideoHooker());//add it for common feature.
        }
        group.addHooker(new LoopVideoHooker()); //add it for common feature.
        if (com.mediatek.common.featureoption.FeatureOption.MTK_S3D_SUPPORT) {
            group.addHooker(new StereoVideoHooker());//should be modified for common feature
        }
        for (final IMovieExtension ext : sMovieExtensions) { //add common feature in host app
            final List<Integer> list = ext.getFeatureList();
            if (list != null) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    final int feature = list.get(i);
                    switch(feature) {
                    case IMovieExtension.FEATURE_ENABLE_STOP:
                        //group.addHooker(new StopVideoHooker());
                        break;
                    case IMovieExtension.FEATURE_ENABLE_NOTIFICATION_PLUS:
                        group.addHooker(new NotificationPlusHooker());
                        break;
                    case IMovieExtension.FEATURE_ENABLE_STREAMING:
                        group.addHooker(new StreamingHooker());
                        break;
                    case IMovieExtension.FEATURE_ENABLE_BOOKMARK:
                        group.addHooker(new BookmarkHooker());
                        break;
                    case IMovieExtension.FEATURE_ENABLE_VIDEO_LIST:
                        group.addHooker(new MovieListHooker());
                        break;
                    case IMovieExtension.FEATURE_ENABLE_STEREO_AUDIO:
                        group.addHooker(new StereoAudioHooker());
                        break;
                    case IMovieExtension.FEATURE_ENABLE_SETTINGS:
                        group.addHooker(new StepOptionSettingsHooker());
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        for (final IMovieExtension ext : sMovieExtensions) { //add other feature in plugin app
            final IActivityHooker hooker = ext.getHooker();
            if (hooker != null) {
                group.addHooker(hooker);
            }
        }
        for (int i = 0, count = group.size(); i < count; i++) {
            if (LOG) {
                MtkLog.v(TAG, "getHooker() [" + i + "]=" + group.getHooker(i));
            }
        }
        return group;
    }
    
    public static IMovieStrategy getMovieStrategy(final Context context) {
        ensureMovieExtension(context);
        for (final IMovieExtension ext : sMovieExtensions) {
            final IMovieStrategy strategy = ext.getMovieStrategy();
            if (strategy != null) {
                return strategy;
            }
        }
        return new MovieStrategy();
    }
    
    private static IMovieDrmExtension sMovieDrmExtension;
    public static IMovieDrmExtension getMovieDrmExtension(final Context context) {
        if (sMovieDrmExtension == null) {
            /*try {
                sMovieDrmExtension = (IMovieDrmExtension) PluginManager.createPluginObject(
                        context.getApplicationContext(), IMovieDrmExtension.class.getName());
            } catch (Plugin.ObjectCreationException e) {
                sMovieDrmExtension = new MovieDrmExtension();
            }*/
            //should be modified for common feature
            if (com.mediatek.common.featureoption.FeatureOption.MTK_DRM_APP) {
                sMovieDrmExtension = new MovieDrmExtensionImpl();
            } else {
                sMovieDrmExtension = new MovieDrmExtension();
            }
        }
        return sMovieDrmExtension;
    }
}
