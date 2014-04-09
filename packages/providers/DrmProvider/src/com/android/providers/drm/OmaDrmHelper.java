/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
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

package com.android.providers.drm;

import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;
import android.util.Log;

import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmUtils;

import java.io.UnsupportedEncodingException;

/**
 * OMA DRM utility class
 */
public class OmaDrmHelper {
    private static final String TAG = "DRM/OmaDrmHelper";
    private static final String EMPTY_STRING = "";
    
    /**
     * 
     * @param client OMA DRM client
     * @param offset The time offset between device local time and time_server
     * @return The status of updating clock. ERROR_NONE for success, ERROR_UNKNOWN for failed.
     */
    public static int updateClock(OmaDrmClient client, int offset) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_UPDATE_CLOCK);
        request.put(OmaDrmStore.DrmRequestKey.KEY_DATA, String.valueOf(offset));

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "updateClock : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }
    
    /**
     * 
     * @param client The OMA DRM client
     * @return the status of updating time base. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int updateTimeBase(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_UPDATE_TIME_BASE);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "updateTimeBase : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }
    
    /**
     * 
     * @param client The OMA DRM client
     * @return the status of updating offset. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int updateOffset(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_UPDATE_OFFSET);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "updateOffset : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    /**
     * 
     * @param client The OMA DRM client
     * @return the status of loading clock. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int loadClock(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_LOAD_CLOCK);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "loadClock : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }
    
    /**
     * 
     * @param client The OMA DRM Client
     * @return the status of saving clock. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int saveClock(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_SAVE_CLOCK);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "saveClock : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }
    
    /**
     * 
     * @param client The OMA DRM Client
     * @return the status of clock
     */
    public static boolean checkClock(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_CHECK_CLOCK);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "checkClock : > " + message);

        return message.equals("valid");
    }
    
    /**
     * 
     * @param client The OMA DRM Client
     * @return the device id
     */
    public static String loadDeviceId(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_LOAD_DEVICE_ID);

        DrmInfo info = client.acquireDrmInfo(request);
        String id = getStringFromDrmInfo(info);
        Log.d(TAG, "loadDeviceId : > " + id);

        return id;
    }
    
    /**
     * 
     * @param client The OMA DRM Client
     * @param deviceId The device id to be saved in file
     * @return the status of save device id. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int saveDeviceId(OmaDrmClient client, String deviceId) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_SAVE_DEVICE_ID);
        request.put(OmaDrmStore.DrmRequestKey.KEY_DATA, deviceId);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "saveDeviceId : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    private static String getStringFromDrmInfo(DrmInfo info) {
        byte[] data = info.getData();
        String message = EMPTY_STRING;
        if (null != data) {
            try {
                // the information shall be in format of ASCII string
                message = new String(data, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding type of the returned DrmInfo data");
                message = EMPTY_STRING;
            }
        }
        Log.v(TAG, "getStringFromDrmInfo : >" + message);
        return message;
    }

}
