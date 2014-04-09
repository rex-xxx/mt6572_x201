/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.mediatek.gallery3d.jps;

import android.graphics.Rect;
import android.util.Log;

import com.mediatek.gallery3d.stereo.StereoHelper;

public class JpsHelper {
	
    private static final String TAG = "Gallery2/JpsHelper";

    public static final String FILE_EXTENSION = "jps";

    public static final String MIME_TYPE = "image/x-jps";

    public static void adjustRect(int layout,boolean firstFrame, 
                                  Rect imageRect) {
        Log.i(TAG,"adjustRect:got imageRect: "+imageRect);
        if (null == imageRect) {
            Log.e(TAG,"adjustRect:got null image rect");
            return;
        }
        if (StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT == layout) {
            if (firstFrame) {
                imageRect.set(imageRect.left, imageRect.top,
                              (imageRect.left + imageRect.right) / 2, 
                              imageRect.bottom);
            } else {
                imageRect.set((imageRect.left + imageRect.right) / 2, 
                              imageRect.top, imageRect.right, imageRect.bottom);
            }
        } else if (StereoHelper.STEREO_LAYOUT_TOP_AND_BOTTOM == layout) {
            if (firstFrame) {
                imageRect.set(imageRect.left, imageRect.top,
                              imageRect.right, 
                              (imageRect.top + imageRect.bottom) / 2);
            } else {
                imageRect.set(imageRect.left, 
                              (imageRect.top + imageRect.bottom) / 2, 
                              imageRect.right, imageRect.bottom);
            }
        }
        Log.d(TAG,"adjustRect:adjusted imageRect: "+imageRect);
    }

}
