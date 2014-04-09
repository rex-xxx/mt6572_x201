package com.android.camera;

import android.content.Context;
import android.util.AttributeSet;

public class VoiceListPreference extends IconListPreference implements VoiceManager.Listener {
    private static final String TAG = "VoiceListPreference";
    private static final boolean LOG = Log.LOGV;

    private VoiceManager mVoiceManager;
    private String mDefaultValue;
    private Camera mCamera;
    
    public VoiceListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCamera = (Camera)context;
    }

    @Override
    public String getValue() {
        if (mVoiceManager != null) {
            mValue = mVoiceManager.getVoiceValue();
            if (mValue == null) {
                mValue = getSupportedDefaultValue();
            }
        } else {
            mValue = getSupportedDefaultValue();
        }
        if (LOG) {
            Log.v(TAG, "getValue() return " + mValue);
        }
        return mValue;
    }
    
    private String getSupportedDefaultValue() {
        if (mDefaultValue == null) {
            mDefaultValue = findSupportedDefaultValue();
        }
        return mDefaultValue;
    }
    
    @Override
    public void setValue(String value) {
        if (LOG) {
            Log.v(TAG, "setValue(" + value + ") mValue=" + mValue);
        }
        if (findIndexOfValue(value) < 0) {
            throw new IllegalArgumentException();
        }
        mValue = value;
        persistStringValue(value);
    }
    
    @Override
    protected void persistStringValue(String value) {
        if (LOG) {
            Log.v(TAG, "persistStringValue(" + value + ") mVoiceManager=" + mVoiceManager);
        }
        if (mVoiceManager != null) {
            mVoiceManager.setVoiceValue(value);
        }
    }
    
    @Override
    public boolean isEnabled() {
        if (LOG) {
            Log.v(TAG, "isEnabled() mVoiceManager=" + mVoiceManager);
        }
        if (mVoiceManager == null || mVoiceManager.getVoiceValue() == null) {
            return false;
        }
        return super.isEnabled();
    }
    
    public void setVoiceManager(VoiceManager voiceManager) {
        if (LOG) {
            Log.v(TAG, "setVoiceManager(" + voiceManager + ") mVoiceManager=" + mVoiceManager);
        }
        if (mVoiceManager != voiceManager) {
            if (mVoiceManager != null) {
                mVoiceManager.removeListener(this);
            }
            mVoiceManager = voiceManager;
            if (voiceManager != null) {
                voiceManager.addListener(this);
            }
        }
    }
    
    @Override
    public void onVoiceValueUpdated(String value) {
        if (LOG) {
            Log.v(TAG, "onVoiceValueUpdated(" + value + ")");
        }
        if (value == null) {
            value = getSupportedDefaultValue();
        }
        if (!((mValue == null && value == null) || (mValue != null && mValue.equals(value)))) {
            mValue = value;
            mCamera.getSettingManager().refresh();
        }
    }

    @Override
    public void onUserGuideUpdated(String userGuide) {
    }
        
    @Override
    public void onVoiceCommandReceive(int commandId) {
    }
}
