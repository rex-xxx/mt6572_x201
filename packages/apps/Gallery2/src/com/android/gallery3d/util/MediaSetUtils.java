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

package com.android.gallery3d.util;

import android.os.Environment;

import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.LocalMergeAlbum;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;

import java.util.Comparator;

public class MediaSetUtils {
    public static final Comparator<MediaSet> NAME_COMPARATOR = new NameComparator();

    public static final int CAMERA_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
    public static final int DOWNLOAD_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.DOWNLOAD);
    public static final int EDITED_ONLINE_PHOTOS_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.EDITED_ONLINE_PHOTOS);
    public static final int IMPORTED_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.IMPORTED);
    public static final int SNAPSHOT_BUCKET_ID = GalleryUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() +
            "/Pictures/Screenshots");

    private static final Path[] CAMERA_PATHS = {
            Path.fromString("/local/all/" + CAMERA_BUCKET_ID),
            Path.fromString("/local/image/" + CAMERA_BUCKET_ID),
            Path.fromString("/local/video/" + CAMERA_BUCKET_ID)};

    public static boolean isCameraSource(Path path) {
        // M: since we've added mtk inclusion for extension purpose,
        // we need to exclude mtk inclusion during comparison
        if (path != null) {
            int mtkInclusion = path.getMtkInclusion();
            if (mtkInclusion != 0) {
                return getCameraPathWithInclusion(0, mtkInclusion) == path ||
                        getCameraPathWithInclusion(1, mtkInclusion) == path || 
                        getCameraPathWithInclusion(2, mtkInclusion) == path;
            }
        }
        return CAMERA_PATHS[0] == path || CAMERA_PATHS[1] == path
                || CAMERA_PATHS[2] == path;
    }

    // Sort MediaSets by name
    public static class NameComparator implements Comparator<MediaSet> {
        @Override
        public int compare(MediaSet set1, MediaSet set2) {
            int result = set1.getName().compareToIgnoreCase(set2.getName());
            if (result != 0) return result;
            return set1.getPath().toString().compareTo(set2.getPath().toString());
        }
    }
    
    private static final String[] CAMERA_PATH_STRINGS = {
        CAMERA_PATHS[0].toString(),
        CAMERA_PATHS[1].toString(),
        CAMERA_PATHS[2].toString()
    };

    private static final Path[] mTempCameraPaths = {
        CAMERA_PATHS[0], CAMERA_PATHS[1], CAMERA_PATHS[2]};
    
    private static Path getCameraPathWithInclusion(int index, int mtkInclusion) {
        if (index < 0 || index > 2) {
            return null;
        }
        if (mTempCameraPaths[index] != null &&
            mTempCameraPaths[index].getMtkInclusion() == mtkInclusion) {
            return mTempCameraPaths[index];
        } else {
            mTempCameraPaths[index] =
                Path.fromString(CAMERA_PATH_STRINGS[index], mtkInclusion);
            return mTempCameraPaths[index];
        }
    }
    
}
