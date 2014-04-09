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
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * A type of <code>CameraPreference</code> whose number of possible values
 * is limited.
 */
public class ListPreference extends CameraPreference {
    private static final String TAG = "ListPreference";
    private static final boolean LOG = Log.LOGV;
    
    public static final int UNKNOWN = -1;
    private final String mKey;
    protected String mValue;
    private final CharSequence[] mDefaultValues;

    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private boolean mLoaded = false;

    private String mOverrideValue;
    private boolean mEnabled = true;
    private CharSequence[] mOriginalSupportedEntries;
    private CharSequence[] mOriginalSupportedEntryValues;
    private CharSequence[] mOriginalEntries;
    private CharSequence[] mOriginalEntryValues;
    private boolean mClickable = true;
    

    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ListPreference, 0, 0);

        mKey = Util.checkNotNull(
                a.getString(R.styleable.ListPreference_key));

        // We allow the defaultValue attribute to be a string or an array of
        // strings. The reason we need multiple default values is that some
        // of them may be unsupported on a specific platform (for example,
        // continuous auto-focus). In that case the first supported value
        // in the array will be used.
        int attrDefaultValue = R.styleable.ListPreference_defaultValue;
        TypedValue tv = a.peekValue(attrDefaultValue);
        if (tv != null && tv.type == TypedValue.TYPE_REFERENCE) {
            mDefaultValues = a.getTextArray(attrDefaultValue);
        } else {
            mDefaultValues = new CharSequence[1];
            mDefaultValues[0] = a.getString(attrDefaultValue);
        }

        setEntries(a.getTextArray(R.styleable.ListPreference_entries));
        setEntryValues(a.getTextArray(R.styleable.ListPreference_entryValues));
        a.recycle();
        
        /// M: here remember initial values
        mOriginalEntryValues = mEntryValues;
        mOriginalEntries = mEntries;
    }

    public String getKey() {
        return mKey;
    }

    public CharSequence[] getEntries() {
        return mEntries;
    }

    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }

    public void setEntries(CharSequence entries[]) {
        mEntries = entries == null ? new CharSequence[0] : entries;
    }

    public void setEntryValues(CharSequence values[]) {
        mEntryValues = values == null ? new CharSequence[0] : values;
    }

    public String getValue() {
        if (!mLoaded) {
            String defaultValue = findSupportedDefaultValue();
            mValue = getSharedPreferences().getString(mKey, defaultValue);
            mLoaded = true;
            if (LOG) {
                Log.v(TAG, "getValue() reload defaultValue=" + defaultValue + ", real=" + mValue);
            }
        }
        return mValue;
    }
    
    public String getDefaultValue() {
        if (mDefaultValues != null && mDefaultValues.length > 0) {
            return String.valueOf(mDefaultValues[0]);
        }
        return null;
    }

    // Find the first value in mDefaultValues which is supported.
    public synchronized String findSupportedDefaultValue() {
        for (int i = 0; i < mDefaultValues.length; i++) {
            for (int j = 0; j < mEntryValues.length; j++) {
                // Note that mDefaultValues[i] may be null (if unspecified
                // in the xml file).
                if (mEntryValues[j].equals(mDefaultValues[i])) {
                    return mDefaultValues[i].toString();
                }
            }
        }
        return null;
    }

    public  void setValue(String value) {
        if (findIndexOfValue(value) < 0) {
            throw new IllegalArgumentException();
        }
        mValue = value;
        persistStringValue(value);
    }

    public synchronized void setValueIndex(int index) {
        if (index < 0 || index >= mEntryValues.length) {
            print();
            Log.w(TAG, "setValueIndex(" + index + ")", new Throwable());
            return;
        }
        setValue(mEntryValues[index].toString());
    }

    public synchronized int findIndexOfValue(String value) {
        for (int i = 0, n = mEntryValues.length; i < n; ++i) {
            if (Util.equals(mEntryValues[i], value)) {
                return i;
            }
        }
        if (LOG) {
            print();
            Log.w(TAG, "findIndexOfValue(" + value + ") not find!!");
        }
        return -1;
    }

    public synchronized String getEntry() {
        int index = findIndexOfValue(getValue());
        if (index < 0 || index >= mEntries.length) {
            print();
            Log.w(TAG, "getEntry()", new Throwable());
            return null;
        }
        return mEntries[index].toString();
    }

    protected void persistStringValue(String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(mKey, value);
        editor.apply();
    }

    @Override
    public void reloadValue() {
        this.mLoaded = false;
    }

    public synchronized void filterUnsupported(List<String> supported) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
        for (int i = 0, len = mOriginalEntryValues.length; i < len; i++) {
            if (supported.indexOf(mOriginalEntryValues[i].toString()) >= 0) {
                entries.add(mOriginalEntries[i]);
                entryValues.add(mOriginalEntryValues[i]);
            }
        }
        int size = entries.size();
        mEntries = entries.toArray(new CharSequence[size]);
        mEntryValues = entryValues.toArray(new CharSequence[size]);
        /// M: here remember all supported values
        mOriginalSupportedEntries = mEntries;
        mOriginalSupportedEntryValues = mEntryValues;
    }

    public synchronized void filterUnsupportedEntries(List<String> supported) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
        for (int i = 0, len = mEntries.length; i < len; i++) {
            if (supported.indexOf(mEntries[i]) >= 0) {
                entries.add(mEntries[i]);
                entryValues.add(mEntryValues[i]);
            }
        }
        int size = entries.size();
        mEntries = entries.toArray(new CharSequence[size]);
        mEntryValues = entryValues.toArray(new CharSequence[size]);
        /// M: here remember all supported values
        mOriginalSupportedEntries = mEntries;
        mOriginalSupportedEntryValues = mEntryValues;
    }

    public synchronized void filterDisabled(List<String> supported) {
        if (LOG) {
            Log.v(TAG, "filterDisabled(" + supported + ")");
        }
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
        for (int i = 0, len = mOriginalSupportedEntryValues.length; i < len; i++) {
            if (supported.indexOf(mOriginalSupportedEntryValues[i].toString()) >= 0) {
                entries.add(mOriginalSupportedEntries[i]);
                entryValues.add(mOriginalSupportedEntryValues[i]);
            }
        }
        int size = entries.size();
        mEntries = entries.toArray(new CharSequence[size]);
        mEntryValues = entryValues.toArray(new CharSequence[size]);
    }
    
    public synchronized void restoreSupported() {
        if (LOG) {
            Log.v(TAG, "restoreSupported() mOriginalSupportedEntries=" + mOriginalSupportedEntries);
        }
        if (mOriginalSupportedEntries != null) {
            mEntries = mOriginalSupportedEntries;
        }
        if (mOriginalSupportedEntryValues != null) {
            mEntryValues = mOriginalSupportedEntryValues;
        }
    }

    public synchronized void print() {
        if (mEntryValues == null || mDefaultValues == null) {
            Log.v(TAG, "print() mEntryValues=" + mEntryValues + ", mDefaultValues=" + mDefaultValues);
            return;
        }
        Log.v(TAG, "Preference key=" + getKey() + ". value=" + getValue());
        for (int i = 0; i < mEntryValues.length; i++) {
            Log.v(TAG, "entryValues[" + i + "]=" + mEntryValues[i]);
        }
        for (int i = 0; i < mDefaultValues.length; i++) {
            Log.v(TAG, "defaultValues[" + i + "]=" + mDefaultValues[i]);
        }
    }

    public synchronized void setOverrideValue(String override, boolean restoreSupported) {
        if (LOG) {
            Log.v(TAG, "setOverrideValue(" + override + ", " + restoreSupported + ") " + this);
        }
        mOverrideValue = override;
        if (override == null) { //clear
            mEnabled = true;
            if (restoreSupported) {
                restoreSupported();
            }
        } else if (SettingUtils.isBuiltList(override)) {
            //mEnabled = true; //Do not change enable state.
            mOverrideValue = SettingUtils.getDefaultValue(override);
            filterDisabled(SettingUtils.getEnabledList(override));
        } else if (SettingUtils.isDisableValue(override)) { //disable
            mEnabled = false;
            mOverrideValue = null;
        } else { //reset
            mEnabled = false;
            //for special case, override value may be not in list.
            //for example: HDR not in user list, but can be set by user.
            if (mOverrideValue != null && findIndexOfValue(mOverrideValue) == -1) {
                mOverrideValue = findSupportedDefaultValue();
                Log.w(TAG, "setOverrideValue(" + override + ") not in list! mOverrideValue=" + mOverrideValue);
            }
        }
        mLoaded = false;
    }
    
    public void setOverrideValue(String override) {
        setOverrideValue(override, true);
    }
    
    public String getOverrideValue() {
        return mOverrideValue;
    }
    
    public int getIconId(int index) {
        return UNKNOWN;
    }
    
    public boolean isEnabled() {
        return mEnabled;
    }
    
    public void setEnabled(boolean enabled) {
        if (LOG) {
            Log.v(TAG, "setEnabled(" + enabled + ")");
        }
        mEnabled = enabled;
    }
    
    public CharSequence[] getOriginalEntryValues() {
        return mOriginalEntryValues;
    }
    
    public CharSequence[] getOriginalEntries() {
        return mOriginalEntries;
    }
    
    public CharSequence[] getOriginalSupportedEntryValues() {
        return mOriginalSupportedEntryValues;
    }
    
    public CharSequence[] getOriginalSupportedEntries() {
        return mOriginalSupportedEntries;
    }
    
    public void setClickable(boolean clickable) {
        mClickable = clickable;
    }
    
    public boolean isClickable() {
        return mClickable;
    }
    
    @Override
    public String toString() {
        return new StringBuilder().append("ListPreference(mKey=")
        .append(mKey)
        .append(", mTitle=")
        .append(getTitle())
        .append(", mOverride=")
        .append(mOverrideValue)
        .append(", mEnable=")
        .append(mEnabled)
        .append(", mValue=")
        .append(mValue)
        .append(", mClickable=")
        .append(mClickable)
        .append(")")
        .toString();
    }
}
