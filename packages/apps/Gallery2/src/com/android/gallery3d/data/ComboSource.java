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

import com.android.gallery3d.app.GalleryApp;

import com.mediatek.gallery3d.util.MediatekFeature;

class ComboSource extends MediaSource {
    private static final int COMBO_ALBUMSET = 0;
    private static final int COMBO_ALBUM = 1;
    private GalleryApp mApplication;
    private PathMatcher mMatcher;

    private static final boolean IS_DRM_SUPPORTED = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean IS_STEREO_DISPLAY_SUPPORTED = 
                        MediatekFeature.isStereoDisplaySupported();

    public ComboSource(GalleryApp application) {
        super("combo");
        mApplication = application;
        mMatcher = new PathMatcher();
        mMatcher.add("/combo/*", COMBO_ALBUMSET);
        mMatcher.add("/combo/*/*", COMBO_ALBUM);
    }

    // The only path we accept is "/combo/{set1, set2, ...} and /combo/item/{set1, set2, ...}"
    @Override
    public MediaObject createMediaObject(Path path) {
        String[] segments = path.split();
        if (segments.length < 2) {
            throw new RuntimeException("bad path: " + path);
        }

        DataManager dataManager = mApplication.getDataManager();
        switch (mMatcher.match(path)) {
            case COMBO_ALBUMSET:
                if (!(IS_DRM_SUPPORTED || IS_STEREO_DISPLAY_SUPPORTED)) {
                    return new ComboAlbumSet(path, mApplication,
                            dataManager.getMediaSetsFromString(segments[1]));
                } else {
                    //for drm feature, we create media set with drm info
                    return new ComboAlbumSet(path, mApplication,
                            dataManager.getMediaSetsFromString(segments[1],
                                             path.getMtkInclusion()));
                }

            case COMBO_ALBUM:
                if (!(IS_DRM_SUPPORTED || IS_STEREO_DISPLAY_SUPPORTED)) {
                    return new ComboAlbum(path,
                            dataManager.getMediaSetsFromString(segments[2]), segments[1]);
                } else {
                    //for drm feature, we create media set with drm info
                    return new ComboAlbum(path,
                            dataManager.getMediaSetsFromString(segments[2],
                                             path.getMtkInclusion()), segments[1]);
                }
        }
        return null;
    }
}
