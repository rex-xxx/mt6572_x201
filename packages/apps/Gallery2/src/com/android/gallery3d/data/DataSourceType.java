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

import com.android.gallery3d.util.MediaSetUtils;

import com.mediatek.gallery3d.stereo.StereoHelper;

public final class DataSourceType {
    public static final int TYPE_NOT_CATEGORIZED = 0;
    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_PICASA = 2;
    public static final int TYPE_MTP = 3;
    public static final int TYPE_CAMERA = 4;

    // M: added for stereo feature
    public static final int TYPE_STEREO = 64;

    private static final Path PICASA_ROOT = Path.fromString("/picasa");
    private static final Path LOCAL_ROOT = Path.fromString("/local");
    private static final Path MTP_ROOT = Path.fromString("/mtp");

    // M: added for Mediatek feature
    private static final String PICASA_ROOT_LABEL = "/picasa";
    private static final String LOCAL_ROOT_LABEL = "/local";
    private static final String MTP_ROOT_LABEL = "/mtp";

    private static Path mTempPicasaRoot = PICASA_ROOT;
    private static Path mTempLocalRoot = LOCAL_ROOT;
    private static Path mTempMtpRoot = MTP_ROOT;

    public static int identifySourceType(MediaSet set) {
        if (set == null) {
            return TYPE_NOT_CATEGORIZED;
        }

        Path path = set.getPath();
        if (MediaSetUtils.isCameraSource(path)) return TYPE_CAMERA;

        Path prefix = path.getPrefixPath();

        int sourceType = identifySourceTypeEx(prefix, set);
        if (TYPE_NOT_CATEGORIZED != sourceType) return sourceType;

        if (prefix == PICASA_ROOT) return TYPE_PICASA;
        if (prefix == MTP_ROOT) return TYPE_MTP;
        if (prefix == LOCAL_ROOT) return TYPE_LOCAL;

        return TYPE_NOT_CATEGORIZED;
    }

    public static int identifySourceTypeEx(Path prefix, MediaSet set) {
        if (null == prefix || 0 == prefix.getMtkInclusion() || null == set) {
            return TYPE_NOT_CATEGORIZED;
        }

        // M: added for stereo feature
        if (StereoHelper.isStereoMediaFolder(set)) return TYPE_STEREO;

        //note that we have to treat mtkinclusion
        int inclusion = prefix.getMtkInclusion();

        //check local
        if (null == mTempLocalRoot || mTempLocalRoot.getMtkInclusion() != inclusion) {
            mTempLocalRoot = Path.fromString(LOCAL_ROOT_LABEL, inclusion);
        }
        if (prefix == mTempLocalRoot) return TYPE_LOCAL;

        //check mtp
        if (null == mTempMtpRoot || mTempMtpRoot.getMtkInclusion() != inclusion) {
            mTempMtpRoot = Path.fromString(MTP_ROOT_LABEL, inclusion);
        }
        if (prefix == mTempMtpRoot) return TYPE_MTP;

        //check picasa
        if (null == mTempPicasaRoot || mTempPicasaRoot.getMtkInclusion() != inclusion) {
            mTempPicasaRoot = Path.fromString(PICASA_ROOT_LABEL, inclusion);
        }
        if (prefix == mTempPicasaRoot) return TYPE_PICASA;

        return TYPE_NOT_CATEGORIZED;
    }

}
