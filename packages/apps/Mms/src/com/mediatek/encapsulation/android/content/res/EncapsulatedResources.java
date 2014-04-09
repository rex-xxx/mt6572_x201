package com.mediatek.encapsulation.android.content.res;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.Movie;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.LongSparseArray;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedResources {

    private static final String TAG = "EncapsulatedResources";

    private Resources mResources;

    public EncapsulatedResources(Resources res) {
        mResources = res;
    }

    /**
     * Get specific color of related theme from theme apk asset resource path.
     *
     * @param colorName the key string of the color.
     * @return the color value for the current theme, if the current theme is
     *         the default theme, or the colorName is not present in the
     *         color.xml, return 0.
     */
    public int getThemeColor(String colorName) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mResources.getThemeColor(colorName);
        } else {
            InputStream raw = null;
            AssetManager am = mResources.getAssets();
            String themepath = SystemProperties.get("persist.sys.skin", DEFAULT_THEME_PATH);
            // If the current theme is the default theme, return 0 directly.
            if (DEFAULT_THEME_PATH.equals(themepath)) {
                return 0;
            }
            // get themeColor from cache
            Integer themeColor = mMtkColorCache.get(colorName);
            if (themeColor != null) {
                return themeColor;
            }
            // Add theme path to asset path to access it, if add asset path failed,
            // return 0 directly.
            int cookie = am.addAssetPath(themepath);
            if (cookie == 0) {
                return 0;
            }

            // Get color value from xml file.
            try {
                // Open color.xml as assets.
                raw = am.openNonAsset(cookie, THEME_COLOR_PATH, AssetManager.ACCESS_STREAMING);

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setValidating(false);
                XmlPullParser myxml = factory.newPullParser();
                myxml.setInput(raw, null);
                int eventType = myxml.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            if (STR_COLOR.equals(myxml.getName())) {
                                if (colorName.equals(myxml.getAttributeValue(0))) {
                                    String colorStr = myxml.nextText();
                                    themeColor = Color.parseColor(colorStr);
                                    mMtkColorCache.put(colorName, themeColor);
                                    return themeColor;
                                }
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            break;
                        default:
                            break;
                    }
                    eventType = myxml.next();
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException happened when getThemeColor, msg = " + e.getMessage());
            } catch (XmlPullParserException e) {
                Log.e(TAG, "XmlPullParserException happened when getThemeColor, msg = "
                        + e.getMessage());
            }

            return 0;
        }
    }

    /**
     * Get main color for current skin.
     */
    public int getThemeMainColor() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mResources.getThemeMainColor();
        } else {
            return getThemeColor("theme_main_color");
        }
    }

    private static final String DEFAULT_THEME_PATH = "/system/framework/framework-res.apk";
    private static final String THEME_COLOR_PATH = "assets/color/colors.xml";
    private static final String STR_COLOR = "color";

    // For getThemeColor add cache.
    private static HashMap<String, Integer> mMtkColorCache = new HashMap<String, Integer>();
}
