package com.android.camera;

import java.util.ArrayList;
import java.util.List;

public class Restriction {
    private static final String TAG = "Restriction";
    private static final boolean LOG = Log.LOGV;
    
    public static final int TYPE_SETTING = 0;
    public static final int TYPE_MODE = 1;
    private final int mSettingIndex;
    private boolean mEnable = true;
    private List<String> mValues;
    private List<Restriction> mRestrictions;
    private int mType = TYPE_SETTING;
        
    public Restriction(int index) {
        mSettingIndex = index;
    }
    
    public int getType() {
        return mType;
    }
    
    public int getIndex() {
        return mSettingIndex;
    }
    
    public boolean getEnable() {
        return mEnable;
    }
    
    public List<String> getValues() {
        return mValues;
    }
    
    public List<Restriction> getRestrictioins() {
        return mRestrictions;
    }
    
    public Restriction setEnable(boolean enable) {
        mEnable = enable;
        return this;
    }

    public Restriction setType(int type) {
        mType = type;
        return this;
    }
    
    public Restriction setValues(final String... values) {
        if (values != null) {
            mValues = new ArrayList<String>();
            for (String value : values) {
                mValues.add(value);
            }
        }
        return this;
    }
    
    public Restriction setRestrictions(final Restriction... restrictions) {
        if (restrictions != null) {
            mRestrictions = new ArrayList<Restriction>();
            for (Restriction value : restrictions) {
                mRestrictions.add(value);
            }
        }
        return this;
    }
    
    private MappingFinder mMappingFinder;
    public String findSupported(String value) {
        String supported = value;
        if (mMappingFinder != null) {
            supported = mMappingFinder.find(value, mValues);
        }
        if (mValues != null && !mValues.contains(supported)) {
            supported = mValues.get(0);
        }
        if (LOG) {
            Log.v(TAG, "findSupported(" + value + ") return " + supported);
        }
        return supported;
    }
    
    public Restriction setMappingFinder(MappingFinder finder) {
        mMappingFinder = finder;
        return this;
    }
    
    public interface MappingFinder {
        String find(String current, List<String> supportedList);
        int findIndex(String current, List<String> supportedList);
    }
}
