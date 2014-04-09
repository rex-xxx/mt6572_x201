/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.mediatek.gallery3d.ui;

import android.graphics.Bitmap;

import com.android.gallery3d.ui.BitmapScreenNail;
import com.android.gallery3d.ui.GLCanvas;

import com.mediatek.gallery3d.drm.DrmHelper;

public class MtkBitmapScreenNail extends BitmapScreenNail {
    private static final String TAG = "Gallery2/MtkBitmapScreenNail";

    protected int mSubType = 0;
    protected int mOriginWidth = 0;
    protected int mOriginHeight = 0;

    public MtkBitmapScreenNail(Bitmap bitmap) {
        super(bitmap);
        mOriginWidth = mWidth;
        mOriginHeight = mHeight;
    }

    public MtkBitmapScreenNail(Bitmap bitmap, int width, int height) {
        super(bitmap);
        mOriginWidth = width;
        mOriginHeight = height;
    }

    public MtkBitmapScreenNail(int width, int height) {
        super(width, height);
        mOriginWidth = width;
        mOriginHeight = height;
    }

    public void setSubType(int subType) {
        mSubType = subType;
    }

    public int getSubType() {
        return mSubType;
    }

    public int getOriginWidth() {
        return mOriginWidth;
    }

    public int getOriginHeight() {
        return mOriginHeight;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        //Log.v(TAG,TAG+":draw(canvas,x="+x+",y="+y+",width="+width+",height="+height+")");
        if (DrmHelper.permitShowThumb(mSubType)) {
            super.draw(canvas, x, y, width, height);
        } else {
            canvas.fillRect(x, y, width, height, PLACEHOLDER_COLOR);
        }
        //we draw overlay on top of micro thumbnail
        DrmHelper.renderSubTypeOverlay(canvas, x, y, width, height, mSubType);
    }
}
