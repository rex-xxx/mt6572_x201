/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2009 The Android Open Source Project
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

package  com.mediatek.gallery3d.pq;
/**
 * Collection of utility functions used in this package.
 */
public class PictureQualityJni {
    private static final String TAG = "Gallery2/PictureQualityJni";

    private PictureQualityJni() {
    }

    static {
        System.loadLibrary("PQjni");
    }
// Base mode Sharpness 
    public static native int nativeGetSharpAdjRange();
    public static native int nativeGetSharpAdjIndex();
    public static native boolean nativeSetSharpAdjIndex(int index);
 // Base mode Global Sat
    public static native int nativeGetSatAdjRange();
    public static native int nativeGetSatAdjIndex();
    public static native boolean nativeSetSatAdjIndex(int index);
    //Base mode Skin Tone
    public static native int nativeGetSkinToneHRange();
    public static native int nativeGetSkinToneHIndex();
    public static native boolean nativeSetSkinToneHIndex(int index);
    //base mode Grass Tone
/*    {"nativeGetGrassToneHRange",  "()I", (void*)getGrassToneHRange },
    {"nativeGetGrassToneHIndex",  "()I", (void*)getGrassToneHIndex },
    {"nativeSetGrassToneHIndex",  "(I)Z", (void*)setGrassToneHIndex },*/
    
    public static native int nativeGetGrassToneHRange();
    public static native int nativeGetGrassToneHIndex();
    public static native boolean nativeSetGrassToneHIndex(int index);

    //base mode Sky Tone 
/*    {"nativeGetSkyToneHRange",  "()I", (void*)getSkyToneHRange },
    {"nativeGetSkyToneHIndex",  "()I", (void*)getSkyToneHIndex },
    {"nativeSetSkyToneHIndex",  "(I)Z", (void*)setSkyToneHIndex },*/
    public static native int nativeGetSkyToneHRange();
    public static native int nativeGetSkyToneHIndex();
    public static native boolean nativeSetSkyToneHIndex(int index);
    //base mode Skin Sat
/*    {"nativeGetSkinToneSRange",  "()I", (void*)getSkinToneSRange },
    {"nativeGetSkinToneSIndex",  "()I", (void*)getSkinToneSIndex },
    {"nativeSetSkinToneSIndex",  "(I)Z", (void*)setSkinToneSIndex },*/
    public static native int nativeGetSkinToneSRange();
    public static native int nativeGetSkinToneSIndex();
    public static native boolean nativeSetSkinToneSIndex(int index);
    
    //base mode Grass Sat
/*    {"nativeGetGrassToneSRange",  "()I", (void*)getGrassToneSRange },
    {"nativeGetGrassToneSIndex",  "()I", (void*)getGrassToneSIndex },
    {"nativeSetGrassToneSIndex",  "(I)Z", (void*)setGrassToneSIndex },*/
    public static native int nativeGetGrassToneSRange();
    public static native int nativeGetGrassToneSIndex();
    public static native boolean nativeSetGrassToneSIndex(int index);

    //base mode Sky Sat
/*    {"nativeGetSkyToneSRange",  "()I", (void*)getSkyToneSRange },
    {"nativeGetSkyToneSIndex",  "()I", (void*)getSkyToneSIndex },
    {"nativeSetSkyToneSIndex",  "(I)Z", (void*)setSkyToneSIndex },*/
    
    public static native int nativeGetSkyToneSRange();
    public static native int nativeGetSkyToneSIndex();
    public static native boolean nativeSetSkyToneSIndex(int index);

    //
    //   AVD mode
    //
    // get index of current pixel
/*    {"nativeGetXAxisRange",  "()I", (void*)getXAxisRange },
    {"nativeGetXAxisIndex",  "()I", (void*)getXAxisIndex },
    {"nativeSetXAxisIndex",  "(I)Z", (void*)setXAxisIndex},*/
    public static native int nativeGetXAxisRange();
    public static native int nativeGetXAxisIndex();
    public static native boolean nativeSetXAxisIndex(int index);
    
    // ADV mode   set 
/*    {"nativeGetYAxisRange",  "()I", (void*)getYAxisRange },
    {"nativeGetYAxisIndex",  "()I", (void*)getYAxisIndex },
    {"nativeSetYAxisIndex",  "(I)Z", (void*)setYAxisIndex},*/
    public static native int nativeGetYAxisRange();
    public static native int nativeGetYAxisIndex();
    public static native boolean nativeSetYAxisIndex(int index);
    //  ADV mode Hue
    public static native int nativeGetHueAdjRange();
    public static native int nativeGetHueAdjIndex();
    public static native boolean nativeSetHueAdjIndex(int index);

}
