package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.android.camera.Log;

/*
 * This is the temporary solution, should be refactored. 
 */
public class FlashPickerButton extends PickerButton {
    private static final String TAG = "FlashPickerButton";
    private static final boolean LOG = Log.LOGV;
    
    private static final String FLASH_TORCH = "torch";
    private static final String FLASH_AUTO = "auto";
    private static final String FLASH_ON = "on";
    private static final String FLASH_OFF = "off";
    
    public FlashPickerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getValidIndexIfNotFind(String value) {
        int index = 0;        
        if (FLASH_AUTO.equals(value)) { //if cannot find auto, it video mode, match off
            index = mPreference.findIndexOfValue(FLASH_OFF);
        } else if (FLASH_ON.equals(value)) { //if cannot find on, it video mode, match torch
            index = mPreference.findIndexOfValue(FLASH_TORCH);
        } else if (FLASH_TORCH.equals(value)) { //if cannot find torch, it video mode, match on
            index = mPreference.findIndexOfValue(FLASH_ON);
        }
        if (LOG) {
            Log.v(TAG, "getValidIndexIfNotFind(" + value + ") return " + index);
        }
        return index;
    }
}
