/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.vt;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.List;

public class VTSettings {

    private static final boolean DEBUG = true;
    private static final String TAG = "VTSettings";

    static final String VTSETTING_FILE = "vt_settings";

    static final String KEY_CAMERA = "camera";
    static final String KEY_VIDEO_QUALITY = "video_quality";
    static final String KEY_IS_MUTE = "microphone_is_mute";
    static final String KEY_SPEAKER_IS_ON = "SPEAKER_IS_ON";

    // video or image
    static final String KEY_LOCAL_VIDEO_TYPE = "KEY_LOCAL_VIDEO_TYPE";
    // default picture or selected picture or free me
    static final String KEY_LOCAL_IMAGE_TYPE = "KEY_LOCAL_IMAGE_TYPE";

    static final String KEY_LOCAL_IMAGE_PATH = "KEY_LOCAL_IMAGE_PATH";

    static final int CAMERA_ZOOM_SCALE_LEVELS = 16;

    public static final int OFF = 0;
    public static final int ON = 1;

    Context mContext;
    // SharedPreferences mSettings;
    // String mCameraSettings;
    CameraParamters mCameraParamters;
    int mCameraZoomIncVal;

    int mVideoQuality;

    // int mIsMute;
    // int mSpeakerIsOn;
    int mVideoType;
    String mImagePath;
    boolean mIsSwitch;
    SurfaceHolder mLocalSurface;
    SurfaceHolder mPeerSurface;

    void init(Context context) {
        if (mCameraParamters != null) {
            Log.e(TAG, "init error");
            return;
        }
        mContext = context;
        mVideoType = 0;
        mImagePath = null;
        mIsSwitch = false;
        mVideoQuality = VTManager.VT_VQ_NORMAL;
        mCameraZoomIncVal = 0;
        mCameraParamters = null;
    }

    void deinit() {
        mCameraParamters = null;
    }

    // call when camera is on
    void getCameraSettings() {
        mCameraParamters = VTelProvider.getParameters();
        if (mCameraParamters.isZoomSupported()) {
            mCameraZoomIncVal = 1;
            // if max-zoom is less than CAMERA_ZOOM_SCALE_LEVELS,
            // mCameraZoomIncVal will be zero.
            // mCameraZoomIncVal = mCameraParamters.getMaxZoom() /
            // (CAMERA_ZOOM_SCALE_LEVELS - 1);
        }
        return;
    }

    void getDefaultSettings() {
        mCameraParamters = null;
        // mCameraSettings = "";
        // mVideoQuality = QUALITY_NORMAL;
        // mIsMute = OFF;
        // mSpeakerIsOn = ON;
    }

    public void setColorEffect(String value) {
        if (null == mCameraParamters) {
            return;
        }
        mCameraParamters.setColorEffect(value);
    }

    public String getColorEffect() {
        if (null == mCameraParamters) {
            return null;
        }
        return mCameraParamters.getColorEffect();
    }

    public List<String> getSupportedColorEffects() {
        if (null == mCameraParamters) {
            return null;
        }
        return mCameraParamters.getSupportedColorEffects();
    }

    // brightness should mapping to public List<String> getSupportedExposure()
    public boolean incBrightness() {
        Log.i(TAG, "incBrightness");
        int value = mCameraParamters.getExposureCompensation();
        int max = mCameraParamters.getMaxExposureCompensation();
        float step = mCameraParamters.getExposureCompensationStep();
        value += step;
        if (value > max) {
            value = max;
        }
        mCameraParamters.setExposureCompensation(value);
        return true;
    }

    public boolean canIncBrightness() {
        Log.i(TAG, "getBrightnessMode");
        int value = mCameraParamters.getExposureCompensation();
        int max = mCameraParamters.getMaxExposureCompensation();
        return value < max;
    }

    public boolean decBrightness() {
        Log.i(TAG, "decBrightness");
        int value = mCameraParamters.getExposureCompensation();
        int min = mCameraParamters.getMinExposureCompensation();
        float step = mCameraParamters.getExposureCompensationStep();
        value -= step;
        if (value < min) {
            value = min;
        }
        mCameraParamters.setExposureCompensation(value);
        return true;
    }

    public boolean canDecBrightness() {
        Log.i(TAG, "canDecBrightness");
        int value = mCameraParamters.getExposureCompensation();
        int min = mCameraParamters.getMinExposureCompensation();
        return value > min;
    }

    // brightness on preview should mapping to Exposure
    public String getBrightnessMode() {
        if (null == mCameraParamters) {
            return null;
        }
        return mCameraParamters.getExposure();
    }

    // brightness on preview should mapping to Exposure
    public void setBrightnessMode(String value) {
        if (null == mCameraParamters) {
            return;
        }
        mCameraParamters.setExposure(value);
    }

    // brightness on preview should mapping to Exposure
    public List<String> getSupportedBrightnessMode() {
        if (null == mCameraParamters) {
            return null;
        }
        return mCameraParamters.getSupportedExposure();
    }

    public boolean incZoom() {
        Log.i(TAG, "incZoom");
        int value = getZoom() + mCameraZoomIncVal;
        int maxZoom = mCameraParamters.getMaxZoom();
        if (value > maxZoom) {
            value = maxZoom;
        }
        mCameraParamters.setZoom(value);
        return true;
    }

    public boolean canIncZoom() {
        Log.i(TAG, "canIncZoom");
        if (!mCameraParamters.isZoomSupported()) {
            return false;
        }
        return (getZoom() < mCameraParamters.getMaxZoom());
    }

    public boolean decZoom() {
        Log.i(TAG, "decZoom");
        int value = getZoom() - mCameraZoomIncVal;
        if (value < 0) {
            value = 0;
        }
        mCameraParamters.setZoom(value);
        return true;
    }

    public boolean canDecZoom() {
        Log.i(TAG, "canDecZoom");
        if (!mCameraParamters.isZoomSupported()) {
            return false;
        }
        return getZoom() > 0;
    }

    public boolean incContrast() {
        Log.i(TAG, "incContrast");
        String value = getContrastMode();
        if (value == null) {
            mCameraParamters.setContrastMode(CameraParamters.CONTRAST_HIGH);
        } else if (value.equals(CameraParamters.CONTRAST_LOW)) {
            mCameraParamters.setContrastMode(CameraParamters.CONTRAST_MIDDLE);
        } else if (value.equals(CameraParamters.CONTRAST_MIDDLE)) {
            mCameraParamters.setContrastMode(CameraParamters.CONTRAST_HIGH);
        } else {
            return false;
        }

        return true;
    }

    public boolean canIncContrast() {
        Log.i(TAG, "canIncContrast");
        List<String> list = mCameraParamters.getSupportedContrastMode();
        if (list == null || list.size() == 0) {
            return false;
        }

        return !CameraParamters.CONTRAST_HIGH.equals(getContrastMode());
    }

    public boolean decContrast() {
        Log.i(TAG, "decContrast");
        String value = getContrastMode();
        if (value == null) {
            mCameraParamters.setContrastMode(CameraParamters.CONTRAST_LOW);
        } else if (value.equals(CameraParamters.CONTRAST_HIGH)) {
            mCameraParamters.setContrastMode(CameraParamters.CONTRAST_MIDDLE);
        } else if (value.equals(CameraParamters.CONTRAST_MIDDLE)) {
            mCameraParamters.setContrastMode(CameraParamters.CONTRAST_LOW);
        } else {
            return false;
        }

        return true;
    }

    public boolean canDecContrast() {
        Log.i(TAG, "canDecContrast");
        List<String> list = mCameraParamters.getSupportedContrastMode();
        if (list == null || list.size() == 0) {
            return false;
        }

        return !CameraParamters.CONTRAST_LOW.equals(getContrastMode());
    }

    public void setZoom(int value) {
        Log.i(TAG, "setZoom");
        if (null == mCameraParamters) {
            return;
        }
        if (value < 0) {
            value = 0;
        }
        mCameraParamters.setZoom(value);
    }

    public List<Integer> getZoomRatios() {
        Log.i(TAG, "getZoomRatios");
        if (null == mCameraParamters) {
            return null;
        }
        return mCameraParamters.getZoomRatios();
    }

    public boolean isZoomSupported() {
        if (null == mCameraParamters) {
            return false;
        }
        return mCameraParamters.isZoomSupported();
    }

    public int getZoom() {
        Log.i(TAG, "getZoom");
        if (null == mCameraParamters) {
            return 0;
        }
        return mCameraParamters.getZoom();
    }

    public boolean isSupportNightMode() {
        List<String> list = getSupportedSceneModes();
        if (null == list) {
            return false;
        }
        for (String str : list) {
            if (str.equalsIgnoreCase("night")) {
                return true;
            }
        }
        return false;
    }

    public void setNightModeFrameRate(boolean isNightMode) {
        mCameraParamters.setPreviewFrameRate((isNightMode) ? 15 : 30);
    }

    public void setNightMode(boolean isOn) {
        String value = isOn ? "night" : "auto";
        setNightModeFrameRate(isOn);
        setSceneMode(value);
    }

    public boolean getNightMode() {
        if (null == getSceneMode()) {
            return false;
        }
        return getSceneMode().equals("night");
    }

    public List<String> getSupportedSceneModes() {
        if (null == mCameraParamters) {
            return null;
        }
        return mCameraParamters.getSupportedSceneModes();
    }

    public String getContrastMode() {
        if (null == mCameraParamters) {
            return null;
        }
        String value = mCameraParamters.getContrastMode();
        Log.i(TAG, "getContrastMode [" + value + "]");
        return value;
    }

    public void setContrastMode(String value) {
        Log.i(TAG, "setContrastMode [" + value + "]");
        if (null == mCameraParamters) {
            return;
        }
        mCameraParamters.setContrastMode(value);
    }

    public List<String> getSupportedContrastMode() {
        if (null == mCameraParamters) {
            return null;
        }
        return mCameraParamters.getSupportedContrastMode();
    }

    public String getSceneMode() {
        if (null == mCameraParamters) {
            return null;
        }
        if (null == mCameraParamters.getSceneMode()) {
            Log.i(TAG, "mCameraParamters.getSceneMode() is null");
            return null;
        } else {
            Log.i(TAG, mCameraParamters.getSceneMode().toString());
        }
        return mCameraParamters.getSceneMode();
    }

    public void setSceneMode(String value) {
        Log.i(TAG, "setSceneMode");
        Log.i(TAG, value);
        if (null == mCameraParamters) {
            return;
        }
        mCameraParamters.setSceneMode(value);
    }

    public int getVideoType() {
        return mVideoType;
    }

    public void setVideoType(int videoType) {
        this.mVideoType = videoType;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String imagePath) {
        this.mImagePath = imagePath;
    }

    public boolean getIsSwitch() {
        return mIsSwitch;
    }

    public void setIsSwitch(boolean isSwitch) {
        this.mIsSwitch = isSwitch;
    }

    public SurfaceHolder getLocalSurface() {
        return mLocalSurface;
    }

    public void setLocalSurface(SurfaceHolder localSurface) {
        this.mLocalSurface = localSurface;
    }

    public SurfaceHolder getPeerSurface() {
        return mPeerSurface;
    }

    public void setPeerSurface(SurfaceHolder peerSurface) {
        this.mPeerSurface = peerSurface;
    }

    public int getVideoQuality() {
        return mVideoQuality;
    }

    public void setVideoQuality(int videoQuality) {
        this.mVideoQuality = videoQuality;
    }
    
    // / M: add function getCameraSettingsForTest() for VTSettings functional
	// test @{
	public CameraParamters getCameraSettingsForTest() {
		Log.e(TAG, "call function getCameraSettings()");
		getCameraSettings();
		Log.e(TAG, "call function getCameraSettings() finished");
		return mCameraParamters;
	}

	// /@}	
    
}
