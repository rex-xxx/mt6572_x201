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

package com.android.gallery3d.data;

import android.database.Cursor;

import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.app.PhotoDataAdapter.MavListener;

import java.text.DateFormat;
import java.util.Date;


import com.mediatek.gallery3d.util.MediatekFeature;

//
// LocalMediaItem is an abstract class captures those common fields
// in LocalImage and LocalVideo.
//
public abstract class LocalMediaItem extends MediaItem {

    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/LocalMediaItem";

    // database fields
    public int id;
    public String caption;
    public String mimeType;
    public long fileSize;
    public double latitude = INVALID_LATLNG;
    public double longitude = INVALID_LATLNG;
    public long dateTakenInMs;
    public long dateAddedInSec;
    public long dateModifiedInSec;
    public String filePath;
    public int bucketId;
    public int width;
    public int height;
    //added for DRM feature
    public int is_drm;
    public int drm_method;
    //added for stereo display feature
    public int stereoType;
    //added to indicating whether the data version has changed
    public boolean dataDirty;
    //added for mav feature, about mav progress bar
    public MavListener mMavListener;

    //added to support Mediatek features
    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean mIsMpoSupported = 
                                          MediatekFeature.isMpoSupported();
    private static final boolean mIsStereoDisplaySupported = 
                                          MediatekFeature.isStereoDisplaySupported();

    public LocalMediaItem(Path path, long version) {
        super(path, version);
    }

    @Override
    public long getDateInMs() {
        return dateTakenInMs;
    }

    @Override
    public String getName() {
        return caption;
    }

    @Override
    public void getLatLong(double[] latLong) {
        latLong[0] = latitude;
        latLong[1] = longitude;
    }

    abstract protected boolean updateFromCursor(Cursor cursor);

    public int getBucketId() {
        return bucketId;
    }

    protected void updateContent(Cursor cursor) {
        if (updateFromCursor(cursor)) {
            mDataVersion = nextVersionNumber();
            dataDirty = true;
        } else {
            dataDirty = false;
        }
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        details.addDetail(MediaDetails.INDEX_PATH, filePath);
        details.addDetail(MediaDetails.INDEX_TITLE, caption);
        DateFormat formater = DateFormat.getDateTimeInstance();
        details.addDetail(MediaDetails.INDEX_DATETIME,
                formater.format(new Date(dateModifiedInSec * 1000)));
        details.addDetail(MediaDetails.INDEX_WIDTH, width);
        details.addDetail(MediaDetails.INDEX_HEIGHT, height);

        if (GalleryUtils.isValidLocation(latitude, longitude)) {
            details.addDetail(MediaDetails.INDEX_LOCATION, new double[] {latitude, longitude});
        }
        if (fileSize > 0) details.addDetail(MediaDetails.INDEX_SIZE, fileSize);
        return details;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }
    
    @Override
    public long getSize() {
        return fileSize;
    }
    
    public boolean isDrm() {
        return drm_method != 0;
    }

    public boolean isDrmMethod(int method) {
        return drm_method == method;
    }

    @Override
    public void setMavListener(MavListener listener) {
        this.mMavListener = listener;
    }
}
