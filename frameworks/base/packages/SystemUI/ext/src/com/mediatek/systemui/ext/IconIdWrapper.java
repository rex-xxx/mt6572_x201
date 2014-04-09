package com.mediatek.systemui.ext;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/**
 * M: This is a wrapper class for binding resource index with its object.
 */
public class IconIdWrapper implements Cloneable {

    private Resources mResources = null;
    private int mIconId = 0;

    public IconIdWrapper() {
        this(null, 0);
    }

    public IconIdWrapper(int iconId) {
        this(null, iconId);
    }

    public IconIdWrapper(Resources resources, int iconId) {
        this.mResources = resources;
        this.mIconId = iconId;
    }

    public Resources getResources() {
        return mResources;
    }

    public void setResources(Resources resources) {
        this.mResources = resources;
    }

    public int getIconId() {
        return mIconId;
    }

    public void setIconId(int iconId) {
        this.mIconId = iconId;
    }

    /**
     * Get the Drawable object which mIconId presented.
     */
    public Drawable getDrawable() {
        if (mResources != null && mIconId != 0) {
            return mResources.getDrawable(mIconId);
        }
        return null;
    }

    public IconIdWrapper clone() {
        IconIdWrapper clone = null;
        try {
            clone = (IconIdWrapper) super.clone();
            clone.mResources = this.mResources;
            clone.mIconId = this.mIconId;
        } catch (CloneNotSupportedException e) {
            clone = null;
        }
        return clone;
    }

}
