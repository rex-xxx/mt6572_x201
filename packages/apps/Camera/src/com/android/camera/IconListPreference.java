/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import java.util.List;

/** A {@code ListPreference} where each entry has a corresponding icon. */
public class IconListPreference extends ListPreference {
    private int mIconIds[];
    private int mOriginalSupportedIconIds[];
    private int mOriginalIconIds[];

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.IconListPreference, 0, 0);
        Resources res = context.getResources();
        mIconIds = getIds(res, a.getResourceId(
                R.styleable.IconListPreference_icons, 0));
        a.recycle();
        //Here we remember original values for strange scene mode:
        //backlight and backlight-portrait which were implemented by a strange way.
        //I have sent email to native FPM to correct this logic.
        //Anyway, we keep this logic until they do that.
        
        /// M: here remember initial values
        mOriginalIconIds = mIconIds;
    }

    public int[] getIconIds() {
        return mIconIds;
    }
    
    public void setIconIds(int[] iconIds) {
        mIconIds = iconIds;
    }

    private int[] getIds(Resources res, int iconsRes) {
        if (iconsRes == 0) { return null; }
        TypedArray array = res.obtainTypedArray(iconsRes);
        int n = array.length();
        int ids[] = new int[n];
        for (int i = 0; i < n; ++i) {
            ids[i] = array.getResourceId(i, 0);
        }
        array.recycle();
        return ids;
    }

    @Override
    public synchronized void filterUnsupported(List<String> supported) {
        CharSequence originalEntryValues[] = getOriginalEntryValues();
        IntArray iconIds = new IntArray();

        for (int i = 0, len = originalEntryValues.length; i < len; i++) {
            if (supported.indexOf(originalEntryValues[i].toString()) >= 0) {
                if (mIconIds != null) {
                    iconIds.add(mIconIds[i]);
                }
            }
        }
        if (mIconIds != null) {
            mIconIds = iconIds.toArray(new int[iconIds.size()]);
            mOriginalSupportedIconIds = mIconIds; //remember all supported values.
        }
        super.filterUnsupported(supported);
    }
    
    @Override
    public synchronized void filterDisabled(List<String> supported) {
        CharSequence originalSupportedEntryValues[] = getOriginalSupportedEntryValues();
        IntArray iconIds = new IntArray();
        for (int i = 0, len = originalSupportedEntryValues.length; i < len; i++) {
            if (supported.indexOf(originalSupportedEntryValues[i].toString()) >= 0) {
                if (mOriginalSupportedIconIds != null) {
                    iconIds.add(mOriginalSupportedIconIds[i]);
                }
            }
        }
        if (mIconIds != null) {
            mIconIds = iconIds.toArray(new int[iconIds.size()]);
        }
        super.filterDisabled(supported);
    }
    
    @Override
    public synchronized void restoreSupported() {
        super.restoreSupported();
        if (mOriginalSupportedIconIds != null) {
            mIconIds = mOriginalSupportedIconIds;
        }
    }
    
    @Override
    public int getIconId(int index) {
        if (mIconIds == null || index < 0 || index >= mIconIds.length) {
            return super.getIconId(index);
        }
        return mIconIds[index];
    }
    
    public int[] getOriginalSupportedIconIds() {
        return mOriginalSupportedIconIds;
    }
    
    public int[] getOriginalIconIds() {
        return mOriginalIconIds;
    }
}
