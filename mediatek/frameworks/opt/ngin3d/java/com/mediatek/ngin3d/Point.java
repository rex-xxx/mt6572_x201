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

import com.mediatek.ngin3d.utils.Ngin3dException;

/**
 * This class implements point/vectors in a three-dimensional space.
 */
public class Point {
    /**
     * First canonical vector (coordinates: 1, 0, 0).
     */
    public static final Point X_AXIS = new Point(1, 0, 0);
    /**
     * Second canonical vector (coordinates: 0, 1, 0).
     */
    public static final Point Y_AXIS = new Point(0, 1, 0);
    /**
     * Third canonical vector (coordinates: 0, 0, 1).
     */
    public static final Point Z_AXIS = new Point(0, 0, 1);

    /**
     * The value of X axis.
     */
    public float x;

    /**
     * The value of Y axis.
     */
    public float y;

    /**
     * The value of Z axis.
     */
    public float z;

    /**
     * The value of Point is normalized or not.
     */
    public boolean isNormalized;

    /**
     * Construct a (0, 0, 0) point.
     */
    public Point() {
        // Do nothing by default
    }

    /**
     * Construct a (0, 0, 0) point with specified normalized flag.
     *
     * @param isNormalized true for normalized
     */
    public Point(boolean isNormalized) {
        this.isNormalized = isNormalized;
    }

    /**
     * Simple constructor.
     * Build a point/vector from its coordinates
     *
     * @param x The value of X axis
     * @param y The value of Y axis
     */
    public Point(float x, float y) {
        set(x, y, 0);
    }

    public Point(float x, float y, boolean isNormalized) {
        set(x, y, 0);
        this.isNormalized = isNormalized;
    }

    /**
     * Simple constructor.
     * Build a point/vector from its coordinates
     *
     * @param x The value of X axis
     * @param y The value of Y axis
     * @param z The value of Z axis
     */
    public Point(float x, float y, float z) {
        set(x, y, z);
    }

    public Point(float x, float y, float z, boolean isNormalized) {
        set(x, y, z);
        this.isNormalized = isNormalized;
    }

    public Point(Point other) {
        set(other.x, other.y, other.z);
        this.isNormalized = other.isNormalized;
    }

    public final void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Multiplicative constructor
     * Build a point/vector from another one and a scale factor.
     * The vector built will be a * u
     *
     * @param a scale factor
     * @param u base (unscaled) point
     */
    public Point(float a, Point u) {
        this.x = a * u.x;
        this.y = a * u.y;
        this.z = a * u.z;
    }

    /**
     * Set the X value of the Point.
     *
     * @param x the value of x
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Get the X value of the Point.
     *
     * @return X of the point
     */
    public float getX() {
        return x;
    }

    /**
     * Set the Y value of the Point.
     *
     * @param y the value of y
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Get the Y value of the Point.
     *
     * @return Y of the point
     */
    public float getY() {
        return y;
    }

    /**
     * Set the Z value of the Point.
     *
     * @param z the value of z
     */
    public void setZ(float z) {
        this.z = z;
    }

    /**
     * Get the Z value of the Point.
     *
     * @return Z of the point
     */
    public float getZ() {
        return z;
    }

    /**
     * Get the distance of a point from the origin.
     *
     * @return the distance of a point from the origin
     */
    public float getLength() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Normalize.
     *
     * @return a new Point with length=1
     */
    public Point getNormalized() {
        float len = getLength();
        if (len > 0) {
            return new Point(x / len, y / len, z / len, true);
        } else {
            return new Point(0, 0, 0, false);
        }
    }

    /**
     * Get a new Point orthogonal to the instance.
     * <p>There are an infinite number of normalized vectors orthogonal
     * to the instance. This method picks up one of them almost
     * arbitrarily. It is useful when one needs to compute a reference
     * frame with one of the axes in a predefined direction. The
     * following example shows how to build a frame having the k axis
     * aligned with the known vector u :
     * <pre><code>
     *   Point k = u.getNormalized();
     *   Point i = k.getOrthogonal();
     *   Point j = Point.crossProduct(k, i);
     * </code></pre></p>
     *
     * @return a new normalized point orthogonal to the instance
     * @throws ArithmeticException if the norm of the instance is null
     */
    public Point getOrthogonal() {

        float threshold = 0.6f * getLength();
        if (threshold == 0) {
            throw new Ngin3dException("MathArithmeticException");
        }

        if ((x >= -threshold) && (x <= threshold)) {
            float inverse = 1 / (float) Math.sqrt(y * y + z * z);
            return new Point(0, inverse * z, -inverse * y);
        } else if ((y >= -threshold) && (y <= threshold)) {
            float inverse = 1 / (float) Math.sqrt(x * x + z * z);
            return new Point(-inverse * z, 0, inverse * x);
        }
        float inverse = 1 / (float) Math.sqrt(x * x + y * y);
        return new Point(inverse * y, -inverse * x, 0);

    }

    /**
     * Add a vector to the instance.
     *
     * @param v vector to add
     * @return a new vector
     */
    public Point add(Point v) {
        return new Point(x + v.x, y + v.y, z + v.z);
    }

    /**
     * Add a scaled vector to the instance.
     *
     * @param factor scale factor to apply to v before adding it
     * @param v      vector to add
     * @return a new vector
     */
    public Point add(float factor, Point v) {
        return new Point(x + factor * v.x, y + factor * v.y, z + factor * v.z);
    }

    /**
     * Subtract a vector from the instance.
     *
     * @param v vector to subtract
     * @return a new vector
     */
    public Point subtract(Point v) {
        return new Point(x - v.x, y - v.y, z - v.z);
    }

    /**
     * Subtract a scaled vector from the instance.
     *
     * @param factor scale factor to apply to v before subtracting it
     * @param v      vector to subtract
     * @return a new vector
     */
    public Point subtract(float factor, Point v) {
        return new Point(x - factor * v.x, y - factor * v.y, z - factor * v.z);
    }

    /**
     * Compute the angular separation between two vectors.
     * <p>This method computes the angular separation between two
     * vectors using the dot product for well separated vectors and the
     * cross product for almost aligned vectors. This allows to have a
     * good accuracy in all cases, even for vectors very close to each
     * other.</p>
     *
     * @param v1 first vector
     * @param v2 second vector
     * @return angular separation between v1 and v2
     */
    public static float angle(Point v1, Point v2) {

        float normProduct = v1.getLength() * v2.getLength();
        if (normProduct == 0) {
            throw new Ngin3dException("MathArithmeticException");
        }

        float dot = dotProduct(v1, v2);
        float threshold = normProduct * 0.9999f;
        if ((dot < -threshold) || (dot > threshold)) {
            // the vectors are almost aligned, compute using the sine
            Point v3 = crossProduct(v1, v2);
            if (dot >= 0) {
                return (float) Math.asin(v3.getLength() / normProduct);
            }
            return (float) Math.PI - (float) Math.asin(v3.getLength() / normProduct);
        }

        // the vectors are sufficiently separated to use the cosine
        return (float) Math.acos(dot / normProduct);

    }

    /**
     * Multiply the instance by a scalar
     *
     * @param a scalar
     * @return a new vector
     */
    public Point multiply(float a) {
        return new Point(a * x, a * y, a * z);
    }

    /**
     * Compute the dot-product of two vectors.
     *
     * @param v1 first vector
     * @param v2 second vector
     * @return the dot product v1.v2
     */
    public static float dotProduct(Point v1, Point v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    /**
     * Compute the cross-product of two vectors.
     *
     * @param v1 first vector
     * @param v2 second vector
     * @return the cross product v1 ^ v2 as a new Vector
     */
    public static Point crossProduct(Point v1, Point v2) {
        return new Point(v1.y * v2.z - v1.z * v2.y,
            v1.z * v2.x - v1.x * v2.z,
            v1.x * v2.y - v1.y * v2.x);
    }

    /**
     * Compute the distance between two points.
     *
     * @param v1 first point
     * @param v2 second point
     * @return the distance between v1 and v2
     */
    public static float distance(Point v1, Point v2) {
        final float dx = v2.x - v1.x;
        final float dy = v2.y - v1.y;
        final float dz = v2.z - v1.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (isNormalized != point.isNormalized) return false;
        if (Float.compare(point.x, x) != 0) return false;
        if (Float.compare(point.y, y) != 0) return false;
        if (Float.compare(point.z, z) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (x == +0.0f ? 0 : Float.floatToIntBits(x));
        result = 31 * result + (y == +0.0f ? 0 : Float.floatToIntBits(y));
        result = 31 * result + (z == +0.0f ? 0 : Float.floatToIntBits(z));
        result = 31 * result + (isNormalized ? 1 : 0);
        return result;
    }

    /**
     * Convert the point property to string for output
     * @return   output string
     */
    @Override
    public String toString() {
        return "Point:[" + this.x + ", " + this.y + ", " + this.z + "], isNormalized : " + isNormalized;
    }

    /**
     * Convert the point property to JSON formatted String
     * @return   output JSON formatted String
     */
    public String toJson() {
        return "{Point:[" + this.x + ", " + this.y + ", " + this.z + "], isNormalized : " + isNormalized + "}";
    }

    public static Point newFromString(String positionString) {
        float[] xyz = Utils.parseStringToFloat(positionString);
        return new Point(xyz[0], xyz[1], xyz[2]);
    }
}
