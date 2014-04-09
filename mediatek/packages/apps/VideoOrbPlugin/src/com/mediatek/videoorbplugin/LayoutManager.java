package com.mediatek.videoorbplugin;

import android.content.Context;
import android.content.res.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import com.mediatek.ngin3d.Point;

public class LayoutManager {
    private static final String TAG = "vo.layout";
    private static final Point sAnimationPos[] = new Point[] {
            new Point(0.4916f, 0.5362f, 428, true), new Point(0.4916f, 0.2775f, 500, true),
            new Point(0.4916f, 0.62875f, 406, true), new Point(0.4916f, 0.4150f, 428, true),
            new Point(0.4916f, 0.89375f, 388, true), new Point(0.4816f, 0.53600f, 385, true),
            new Point(0.4816f, 0.3525f, 385, true), new Point(0.4816f, 0.72000f, 385, true) };

    private static final Point sVideoPos[] = new Point[] {
            new Point(0f, 0f, -500), new Point(0f, 0f, -500),
            new Point(0f, 0f, -500), new Point(0f, 0f, -400),
            new Point(0f, 0f, -500), new Point(0f, 0f, -500),
            new Point(0f, 0f, -500), new Point(0f, 0f, -400)
    };

    private static final int base = 720;
    private static final Point sLightPos = new Point(0.5f, 0.5f, 800, true);
    private static final Point sBackgroundPos = new Point(1.07f, 0.5f, 1200, true);

    private static DisplayMetrics dm;
    public static void setDisplayMetrics(Context ctx) {
        Log.v(TAG, "setDisplayMetrics : " + ctx);
        if (dm == null && ctx != null) {
            dm = new DisplayMetrics();
            WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(dm);
            Log.v(TAG, "Metrics : " + dm);
        }
    }

    public static Point[] getAnimationPos() {
        return sAnimationPos;
    }

    public static Point[] getActorPos() {
        Point dpVideoPos[] = new Point[sVideoPos.length];
        for (int i = 0; i < sVideoPos.length; ++i) {
            dpVideoPos[i] = px2dp(sVideoPos[i]);
        }
        return dpVideoPos;
    }

    public static Point getLightPos() {
        return px2dp(sLightPos);
    }

    public static Point getBackgroundPos() {
        return px2dp(sBackgroundPos);
    }

    public static Point px2dp(Point p) {
        Point dp = new Point(p);
        if (dm != null) {
            Log.v(TAG, "px2dp (px) change : " + (float)dm.widthPixels/base);
            dp.set(p.x, p.y, scale(p.z));
        }
        Log.v(TAG, "px2dp (px) : " + p + ", (dp) : " + dp);
        return dp;
    }

    public static float getScaleFactor() {
        return ((float)dm.widthPixels/base > 1) ? ((float)dm.widthPixels/base) * 0.85f : 1;
    }

    public static float scale(float o) {
        return getScaleFactor() * o ;
    }

    public static int[] getIntArray(Resources r, int id, boolean isScale) {
        int[] intArray = r.getIntArray(id);
        if (isScale) {
            for (int i = 0; i < intArray.length; ++i) {
                intArray[i] = (int)scale(intArray[i]);
            }
        }
        return intArray;
    }
}