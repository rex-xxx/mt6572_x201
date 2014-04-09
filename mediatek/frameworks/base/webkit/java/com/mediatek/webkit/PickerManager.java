/*
 * Copyright (C) 2010 Daniel Nilsson
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

package com.mediatek.webkit;

import android.app.Dialog;
import android.content.Context;
import com.mediatek.common.webkit.IPicker;
import com.mediatek.common.webkit.IOnChangedListener;

/**
 * @hide
 */
public class PickerManager implements IPicker {
    private String mPickerType = IPicker.COLOR_PICKER;
    private IOnChangedListener mListener;
    private Dialog mDialog;

    public static PickerManager getInstance(String type) {
        if (isValid(type)) {
            return new PickerManager(type);
        }
        return null;
    }

    private static boolean isValid(String type) {
        return type.equals(IPicker.COLOR_PICKER) ||
               type.equals(IPicker.MONTH_PICKER) ||
               type.equals(IPicker.WEEK_PICKER);
    }

    public String getType() {
        return mPickerType;
    }

    public void setOnChangedListener(IOnChangedListener listener) {
        mListener = listener;
    }

    public void show(Context context, int initialValue1, int initialValue2, Object initialObj) {
        if (mPickerType.equals(IPicker.COLOR_PICKER)) {
            if (context != null) {
                ColorPickerDialog mDialog = new ColorPickerDialog(context, initialValue1);
                if (mListener != null) {
                    mDialog.setOnColorChangedListener(new ColorChangedListener(mListener));
                }
                mDialog.show();
            }
        }
    }

    // Only allow create via getInstance()
    private PickerManager(String type) {
        mPickerType = type;
    }

    class ColorChangedListener implements ColorPickerDialog.OnColorChangedListener {
        private IOnChangedListener mListener;
        public ColorChangedListener(IOnChangedListener listener) {
            mListener = listener;
        }

        public void onColorChanged(int color) {
            if (mListener != null) {
                // Only value1 is valid for color picker.
                mListener.onChanged(IPicker.COLOR_PICKER, color, 0, null);
            }
        }
    }
}
