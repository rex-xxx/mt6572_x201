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

import android.util.Log;
import android.view.Surface;

public class VTelProvider {
    static {
        // todo "VTel_jni" is a temporary name
        System.loadLibrary("mtk_vt_client");
    }
    private static final boolean DEBUG = true;
    private static final String TAG = "VTelProvider";

    private static native void nativeSetParameters(String params);

    private static native String nativeGetParameters();

    public static native int switchCamera();

    public static final native int openVTService(int simId);

    public static native int initVTService(Surface local, Surface peer);

    public static final native int startVTService();

    public static native int stopVTService();

    public static native int closeVTService();

    public static native void setEndCallFlag();

    // type -> video / image / freeze me
    public static native void setLocalVideoType(int type, String path);

    public static native int replacePeerVideoSettings(int enableFlag, String filePath);

    // type -> local / peer
    public static native int snapshot(int type, String path);

    public static native int getCameraSensorCount();

    public static native void turnOnMicrophone(int isOn);

    public static native int isMicrophoneOn();

    public static native void turnOnSpeaker(int isOn);

    public static native int isSpeakerOn();

    public static native void setPeerVideo(int quality);

    public static native int setVTVisible(int isOn, Surface local, Surface peer);

    public static native void onUserInput(String input);

    public static native int lockPeerVideo();

    public static native int unlockPeerVideo();

    public static native int enableAlwaysAskSettings(int flag);

    public static native int userSelectYes(int flag);

    public static native int enableHideYou(int flag);

    public static native int enableHideMe(int flag);

    public static native int incomingVideoDispaly(int flag);

    public static native int incomingVTCall(int flag);

    public static native int setInvokeLockPeerVideoBeforeOpen(int invoked);

    public static native int startRecording(int type, String path, long maxSize);

    public static native int stopRecording(int type);

    // VT EM settings
    public static native void setEM(int item, int arg1, int arg2);

    /**
     * Sets the Parameters for pictures from this Camera service.
     * 
     * @param params the Parameters to use for this Camera service
     */
    public static void setParameters(CameraParamters params) {
        if(null == params){
            Log.e(TAG, "setParameters: params == null");
	    return;
        }
		
        Log.i(TAG, params.flatten());
        nativeSetParameters(params.flatten());
        Log.i(TAG, "setParameters ok");
    }

    /**
     * Returns the picture Parameters for this Camera service.
     */
    public static CameraParamters getParameters() {
        CameraParamters p = new CameraParamters();
        String s = nativeGetParameters();
        p.unflatten(s);
        p.dump();
        return p;
    }

    public static CameraParamters updateParameters(CameraParamters p) {
	if(null == p){
	    Log.e(TAG, "updateParameters: p == null");
	    return p;
        }
		
        String s = nativeGetParameters();
        p.unflatten(s);
        return p;
    }

    public static void postEventFromNative(int msg, int arg1, int arg2, Object obj) {
        VTManager.getInstance().postEventFromNative(msg, arg1, arg2, obj);
    }

}
