/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ngin3d;

import android.content.res.Resources;
import android.graphics.Bitmap;
import com.mediatek.ngin3d.presentation.Model3d;

/**
 * A 3D Cube.
 */
public class Cube extends Basic3D {
    public Cube() {
        this(false);
    }

    public Cube(boolean isYUp) {
        super(Model3d.CUBE, isYUp);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods

    /**
     * Creates a cube which texture image is from file name reference.
     * @param filename  texture image file name.
     * @return  result cube.
     */
    public static Cube createFromFile(String filename) {
        return createFromFile(filename, false);
    }

    /**
     * Creates a cube which texture image is from file name reference.
     * @param filename  texture image file name.
     * @param isYUp   true for creating a Y-up cube, default is Y-down
     * @return  result cube.
     */
    public static Cube createFromFile(String filename, boolean isYUp) {
        Cube cube = new Cube();
        cube.setImageFromFile(filename);
        return cube;
    }

    /**
     * Creates a cube which texture image is from bitmap data.
     * @param bitmap   texture image bitmap data
     * @return  result cube.
     */
    public static Cube createFromBitmap(Bitmap bitmap) {
        return createFromBitmap(bitmap, false);
    }

    /**
     * Creates a cube which texture image is from bitmap data.
     * @param bitmap   texture image bitmap data
     * @param isYUp   true for creating a Y-up cube, default is Y-down
     * @return  result cube.
     */
    public static Cube createFromBitmap(Bitmap bitmap, boolean isYUp) {
        Cube cube = new Cube();
        cube.setImageFromBitmap(bitmap);
        return cube;
    }

    /**
     * Creates a cube which texture image is from android resource manager.
     * @param resources  gets android resource manager.
     * @param resId  resource id
     * @return   result cube
     */
    public static Cube createFromResource(Resources resources, int resId) {
        return createFromResource(resources, resId, false);
    }

    /**
     * Creates a cube which texture image is from android resource manager.
     * @param resources  gets android resource manager.
     * @param resId  resource id
     * @param isYUp   true for creating a Y-up cube, default is Y-down
     * @return   result cube
     */
    public static Cube createFromResource(Resources resources, int resId, boolean isYUp) {
        Cube cube = new Cube();
        cube.setImageFromResource(resources, resId);
        return cube;
    }
}

