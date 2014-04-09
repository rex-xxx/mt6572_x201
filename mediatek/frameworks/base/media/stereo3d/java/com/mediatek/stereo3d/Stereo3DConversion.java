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

package com.mediatek.stereo3d;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.mediatek.common.stereo3d.IStereo3DConversion;
import com.mediatek.xlog.Xlog;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class Stereo3DConversion implements IStereo3DConversion {
    private static final String TAG = "Stereo3DConversion";
    private static final int SUCCESS = 0;
    private static final int IMG_MAX_WIDTH = 2048; // GPU image width limitation
    private static final int IMG_MAX_HEIGHT = 2048; // GPU image height limitation

    static {
        System.loadLibrary("ipto3d");
        System.loadLibrary("ipto3djni");
    }

    private static native int init3DConversion(int inputWidth, int inputHeight, int smallWidth,
            int smallHeight);
    private static native Bitmap process3DConversion(Bitmap input, Bitmap small, int outputWidth,
            int outputHeight, boolean isMutable);
    private static native int close3DConversion();

    /**
     * Private constructor here, It is a singleton class.
     */
    private Stereo3DConversion() {
    }

    /**
     * This method converts 2D image into a side-by-side image used for 3D display.
     *
     * @param bitmap the 2D bitmap
     * @return the converted bitmap, i.e. side-by-side bitmap
     * @hide
     */
    public static Bitmap execute(Bitmap bitmap) {
        // We multiple output image width by 2 to keep the aspect ratio of the image.
        // For example, if input image is 100x100, we would like to get a left side image 100x100.
        // So the SBS output image will be 200x100
        return execute(bitmap, bitmap.getWidth() * 2, bitmap.getHeight(), false);
    }

    /**
     * This method converts 2D image into a side-by-side image used for 3D display.
     *
     * @param bitmap the 2D bitmap
     * @param isMutable whether the 2D bitmap can be changed or not
     * @return the converted bitmap, i.e. side-by-side bitmap
     * @hide
     */
    public static Bitmap execute(Bitmap bitmap, boolean isMutable) {
        return execute(bitmap, bitmap.getWidth() * 2, bitmap.getHeight(), isMutable);
    }

    /**
     * This method converts 2D image into a side-by-side image used for 3D display.
     *
     * @param bitmap the 2D bitmap
     * @param outputWidth the desired width of the output image
     * @param outputHeight the desired height of the output image
     * @return the converted bitmap, i.e. side-by-side bitmap
     * @hide
     */
    public static Bitmap execute(Bitmap bitmap, int outputWidth, int outputHeight) {
        return execute(bitmap, outputWidth, outputHeight, false);
    }

    /**
     * This method converts 2D image into a side-by-side image used for 3D display.
     *
     * @param bitmap the 2D bitmap
     * @param outputWidth the desired width of the output image
     * @param outputHeight the desired height of the output image
     * @param isMutable whether the 2D bitmap can be changed or not
     * @return the converted bitmap, i.e. side-by-side bitmap
     * @hide
     */
    public static Bitmap execute(Bitmap bitmap, int outputWidth, int outputHeight,
                                 boolean isMutable) {
        Xlog.i(TAG, "perform2DTo3DConversion");

        // We check the output image width by multiplying width by 2
        // because it contains left + right images
        int defaultOutputWidth = bitmap.getWidth() * 2;
        int defaultOutputHeight = bitmap.getHeight();

        if ((bitmap == null) || (outputWidth > IMG_MAX_WIDTH) || (outputHeight > IMG_MAX_HEIGHT)
                || (defaultOutputWidth > IMG_MAX_WIDTH) || (defaultOutputHeight > IMG_MAX_HEIGHT)) {

            Xlog.i(TAG, "Output image is null or too big: " + outputWidth + " x " + outputHeight);
            return null;
        }

        Bitmap smallImage = generateSmallImage(bitmap);

        Bitmap convertedBitmap = null;

        int result = -1;

        synchronized (Stereo3DConversion.class) {
            result = init3DConversion(bitmap.getWidth(), bitmap.getHeight(), smallImage.getWidth(),
                                      smallImage.getHeight());

            if (result == SUCCESS) {
                convertedBitmap = process3DConversion(bitmap, smallImage, outputWidth, outputHeight,
                                                      isMutable);
            }

            close3DConversion();
        }

        smallImage.recycle();

        return convertedBitmap;
    }

    /**
     * This method resizes the large image to the specified size
     *
     * @param oldBitmap the original bitmap
     * @param newWidth  the new width
     * @param newHeight the new height
     * @return the resized bitmap
     */
    private static Bitmap generateSmallImage(Bitmap oldBitmap) {
        int width = oldBitmap.getWidth();
        int height = oldBitmap.getHeight();

        Xlog.i(TAG, "Large bitmap size: " + width + " x " + height);

        float scale = 0.08333f; // small image is 1/12 of the large image

        Xlog.i(TAG, "Scale: " + scale);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(oldBitmap, 0, 0, width, height,
                                   matrix, true);
    }
}