package com.mediatek.thememanager;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Define data elements and base columns for theme provider.
 */
public class Themes implements BaseColumns {

    public static final String AUTHORITY = "com.mediatek.thememanager.ThemeProvider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/theme");

    public static final String PACKAGE_NAME = "package_name";

    public static final String THEME_PATH = "theme_path";

    public static final String THEME_NAME_ID = "theme_name_id";
}
