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

import com.mediatek.util.JSON;

/**
 * A special class that is responsible for scale operation
 */
public class Scale implements JSON.ToJson {
    public float x;
    public float y;
    public float z;

    /**
     * Initialize the object with empty setting.
     */
    public Scale() {
        // Do nothing by default
    }

    /**
     * Initialize the object with specific x and y amount.
     */
    public Scale(float x, float y) {
        set(x, y, 1.0f);
    }

    /**
     * Initialize the object with specific x, y, and z amount.
     */
    public Scale(float x, float y, float z) {
        set(x, y, z);
    }

    /**
     * Initialize the scale equally in all directions.
     */
    public Scale(float xyz) {
        set(xyz, xyz, xyz);
    }

    /**
     *  Set the specific value to this scale object
     * @param x  x value
     * @param y  y value
     * @param z  z value
     */
    public final void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Compare the input object with this scale object.
     * @param o   the object to be compared
     * @return  true if two objects are the same or their properties are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scale scale = (Scale) o;

        if (Float.compare(scale.x, x) != 0) return false;
        if (Float.compare(scale.y, y) != 0) return false;
        if (Float.compare(scale.z, z) != 0) return false;

        return true;
    }

     /**
     * Create a new hash code.
     * @return  hash code
     */
    @Override
    public int hashCode() {
        int result = (x == +0.0f ? 0 : Float.floatToIntBits(x));
        result = 31 * result + (y == +0.0f ? 0 : Float.floatToIntBits(y));
        result = 31 * result + (z == +0.0f ? 0 : Float.floatToIntBits(z));
        return result;
    }

    /**
     * Convert the scale property to string for output
     * @return   output string
     */
    @Override
    public String toString() {
        return "Point:[" + this.x + ", " + this.y + ", " + this.z + "]";
    }

    /**
     * Convert the scale property to JSON formatted String
     * @return   output JSON formatted String
     */
    public String toJson() {
        return "{Point:[" + this.x + ", " + this.y + ", " + this.z + "]" + "}";
    }

    public static Scale newFromString(String positionString) {
        float[] xyz = Utils.parseStringToFloat(positionString);
        return new Scale(xyz[0], xyz[1], xyz[2]);
    }

}
