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

package com.mediatek.phone.vt;

import android.util.Log;

import com.android.internal.telephony.Connection;
import com.mediatek.phone.ext.ExtensionManager;

public final class VTInCallScreenFlags {

    public boolean mVTIsMT;
    public boolean mVTHideMeNow;
    public boolean mVTFrontCameraNow;
    public boolean mVTSettingReady;
    public boolean mVTSurfaceChangedL;
    public boolean mVTSurfaceChangedH;
    public boolean mVTVideoConnected;
    public boolean mVTVideoReady;
    public boolean mVTHasReceiveFirstFrame;
    public boolean mVTInLocalZoomSetting;
    public boolean mVTInLocalBrightnessSetting;
    public boolean mVTInLocalContrastSetting;
    public boolean mVTInControlRes;
    public boolean mVTInEndingCall;
    public boolean mVTShouldCloseVTManager;
    public VTConnectionStarttime mVTConnectionStarttime;
    public boolean mVTInSnapshot;
    public boolean mVTInSwitchCamera;
    public boolean mVTPeerBigger;
    public boolean mVTIsInflate;
    public boolean mVTVoiceAnswer;
    public String mVTVoiceAnswerPhoneNumber;
    public int mVTSlotId;

    private static VTInCallScreenFlags sVTInCallScreenFlags
            = new VTInCallScreenFlags();

    /**
     * Get the VTInCallScreenFlags instance
     * @return    VTInCallScreenFlags instance
     */
    public static VTInCallScreenFlags getInstance() {
        return sVTInCallScreenFlags;
    }

    private VTInCallScreenFlags() {
        mVTConnectionStarttime = new VTConnectionStarttime();
        reset();
    }

    /**
     * reset all flags
     */
    public void reset() {
        mVTIsMT = false;
        mVTHideMeNow = false;
        mVTFrontCameraNow = true;
        mVTSettingReady = false;
        mVTSurfaceChangedL = false;
        mVTSurfaceChangedH = false;
        mVTVideoConnected = false;
        mVTVideoReady = false;
        mVTHasReceiveFirstFrame = false;
        mVTInLocalZoomSetting = false;
        mVTInLocalBrightnessSetting = false;
        mVTInLocalContrastSetting = false;
        mVTInControlRes = false;
        mVTInEndingCall = false;
        mVTShouldCloseVTManager = true;

        if (mVTConnectionStarttime != null) {
            mVTConnectionStarttime.reset();
        }

        mVTInSnapshot = false;
        mVTInSwitchCamera = false;
        mVTPeerBigger = true;
        mVTVoiceAnswer = false;
        mVTVoiceAnswerPhoneNumber = null;
        ExtensionManager.getInstance().
            getVTInCallScreenFlagsExtension().reset();
        mVTSlotId = 0;
    }

    /**
     * reset partial flags
     */
    public void resetPartial() {
        mVTIsMT = false;
        mVTHideMeNow = false;
        mVTFrontCameraNow = true;
        mVTSettingReady = false;
        mVTSurfaceChangedL = false;
        mVTSurfaceChangedH = false;
        mVTVideoConnected = false;
        mVTVideoReady = false;
        mVTHasReceiveFirstFrame = false;
        mVTInLocalZoomSetting = false;
        mVTInLocalBrightnessSetting = false;
        mVTInLocalContrastSetting = false;
        mVTInControlRes = false;
        mVTInEndingCall = false;

        if (mVTConnectionStarttime != null) {
            mVTConnectionStarttime.reset();
        }

        mVTInSnapshot = false;
        mVTInSwitchCamera = false;
        mVTPeerBigger = true;
        mVTVoiceAnswer = false;
        mVTVoiceAnswerPhoneNumber = null;
        ExtensionManager.getInstance().
            getVTInCallScreenFlagsExtension().reset();
        mVTSlotId = 0;
    }

    /**
     * reset time related flags
     */
    public void resetTiming() {
        if (mVTConnectionStarttime != null) {
            mVTConnectionStarttime.reset();
        }
    }

    public class VTConnectionStarttime {
        public Connection mConnection;
        public long mStarttime;

        /**
         * Constructor function
         */
        public VTConnectionStarttime() {
            reset();
        }

        /**
         * reset start time
         */
        public void reset() {
            Log.d("VTConnectionStarttime", "reset...");
            mConnection = null;
            mStarttime = -1;
        }
    }
}