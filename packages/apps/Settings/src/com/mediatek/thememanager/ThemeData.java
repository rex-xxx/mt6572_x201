package com.mediatek.thememanager;

/**
 * Package theme resource package information.
 */
public class ThemeData {

    /**
     * The Name of theme package.
     */
    private String mPackageName;

    /**
     * The path of theme package.
     */
    private String mThemePath;

    /**
     * The name of theme.
     */
    private String mThemeName;
    
    /**
     * Return package name of theme.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Set package name of theme.
     */
    public void setPackageName(final String packageName) {
        this.mPackageName = packageName;
    }

    /**
     * Return path of theme package.
     */
    public String getThemePath() {
        return mThemePath;
    }

    /**
     * Set path of theme package.
     */
    public void setThemePath(final String themePath) {
        this.mThemePath = themePath;
    }

    /**
     * Return name of theme package.
     */
    public String getThemeName() {
        return mThemeName;
    }

    /**
     * Set name of theme package.
     */
    public void setThemeName(final String themeName) {
        this.mThemeName = themeName;
    }
}
