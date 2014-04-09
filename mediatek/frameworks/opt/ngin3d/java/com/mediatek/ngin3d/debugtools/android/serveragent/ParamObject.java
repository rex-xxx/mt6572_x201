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

package com.mediatek.ngin3d.debugtools.android.serveragent;

public class ParamObject {
    public static final int PARAMETER_TYPE_NULL = 0;
    public static final int PARAMETER_TYPE_NAME = 1;
    public static final int PARAMETER_TYPE_COLOR = 2;
    public static final int PARAMETER_TYPE_ROTATION = 3;
    public static final int PARAMETER_TYPE_SCALE = 4;
    public static final int PARAMETER_TYPE_ANCHOR = 5;
    public static final int PARAMETER_TYPE_POSITION = 6;
    public static final int PARAMETER_TYPE_VISIBLE = 7;
    public static final int PARAMETER_TYPE_FLAG = 8;
    public static final int PARAMETER_TYPE_ZORDER_ON_TOP = 9;
    public int mParameterType;
    public String mNameR;
    public int mColorR;
    public int mColorG;
    public int mColorB;
    public int mColorH;
    public float mRotationX;
    public float mRotationY;
    public float mRotationZ;
    public float mScaleX;
    public float mScaleY;
    public float mScaleZ;
    public float mAnchorX;
    public float mAnchorY;
    public float mAnchorZ;
    public float mPositionX;
    public float mPositionY;
    public float mPositionZ;
    public boolean mIsVisible;
    public int mFlag;
    public int mZorderOnTop;

    public ParamObject() {
        super();
    }

    public boolean setParameters(String type, String value) {
        mParameterType = PARAMETER_TYPE_NULL;
        String[] values = value.split(",");

        try {
            if ("name".equals(type)) {
                mNameR = values[0];
                mParameterType = PARAMETER_TYPE_NAME;
            } else if ("visible".equals(type)) {

                if (values[0].equals("true")) {
                    mIsVisible = true;
                } else {
                    mIsVisible = false;
                }
                mParameterType = PARAMETER_TYPE_VISIBLE;
            } else if ("color".equals(type)) {
                mColorH = Integer.parseInt(values[0]);
                mColorR = Integer.parseInt(values[1]);
                mColorG = Integer.parseInt(values[2]);
                mColorB = Integer.parseInt(values[3]);
                mParameterType = PARAMETER_TYPE_COLOR;
            } else if ("rotation".equals(type)) {
                mRotationX = Float.parseFloat(values[0]);
                mRotationY = Float.parseFloat(values[1]);
                mRotationZ = Float.parseFloat(values[2]);
                mParameterType = PARAMETER_TYPE_ROTATION;
            } else if ("scale".equals(type)) {
                mScaleX = Float.parseFloat(values[0]);
                mScaleY = Float.parseFloat(values[1]);
                mScaleZ = Float.parseFloat(values[2]);
                mParameterType = PARAMETER_TYPE_SCALE;
            } else if ("anchorpoint".equals(type)) {
                mAnchorX = Float.parseFloat(values[0]);
                mAnchorY = Float.parseFloat(values[1]);
                mAnchorZ = Float.parseFloat(values[2]);
                mParameterType = PARAMETER_TYPE_ANCHOR;
            } else if ("position".equals(type)) {
                mPositionX = Float.parseFloat(values[0]);
                mPositionY = Float.parseFloat(values[1]);
                mPositionZ = Float.parseFloat(values[2]);
                mParameterType = PARAMETER_TYPE_POSITION;
            } else if ("flag".equals(type)) {
                mFlag = Integer.parseInt(values[0]);
                mParameterType = PARAMETER_TYPE_FLAG;
            } else if ("zorderontop".equals(type)) {
                mZorderOnTop = Integer.parseInt(values[0]);
                mParameterType = PARAMETER_TYPE_ZORDER_ON_TOP;
            }
        } catch (NumberFormatException e) {
            // if the format isn't right, we just return false
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            // if the format isn't right, we just return false
            return false;
        }

        return (mParameterType > 0);
    }

    public int getParameterType() {
        return mParameterType;
    }

}
