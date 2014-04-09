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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.util.Log;
import com.mediatek.ngin3d.utils.Ngin3dException;

/**
 * A base class for actor properties setting.
 */
public class Quaternion {

    private static final String TAG = "Quaternion";

    private static final float NORMALIZATION_TOLERANCE = 0.00001f;

    /**
     * Scalar coordinate of the quaternion.
     */
    private float mQ0;

    /**
     * First coordinate of the vectorial part of the quaternion.
     */
    private float mQ1;

    /**
     * Second coordinate of the vectorial part of the quaternion.
     */
    private float mQ2;

    /**
     * Third coordinate of the vectorial part of the quaternion.
     */
    private float mQ3;

    /**
     * Constructor, sets the four components of the quaternion.
     *
     * @param q0 The q0-component
     * @param q1 The q1-component
     * @param q2 The q2-component
     * @param q3 The q3-component
     */
    public Quaternion(float q0, float q1, float q2, float q3) {
        this.set(q0, q1, q2, q3);
    }

    public Quaternion() {
        idt();
    }

    /**
     * Constructor, sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion to copy.
     */
    public Quaternion(Quaternion quaternion) {
        this.set(quaternion);
    }

    /**
     * Constructor, sets the quaternion from the given axis vector and the angle around that axis in degrees.
     *
     * @param axis  The axis
     * @param angle The angle in degrees.
     */
    public Quaternion(Point axis, float angle) {
        this.set(axis, angle);
    }

    /**
     * Sets the components of the quaternion
     *
     * @param x The q0-component
     * @param y The q1-component
     * @param z The q2-component
     * @param w The q3-component
     * @return This quaternion for chaining
     */
    public final Quaternion set(float x, float y, float z, float w) {
        this.mQ0 = x;
        this.mQ1 = y;
        this.mQ2 = z;
        this.mQ3 = w;
        return this;
    }

    /**
     * Sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion.
     * @return This quaternion for chaining.
     */
    public final Quaternion set(Quaternion quaternion) {
        return this.set(quaternion.mQ0, quaternion.mQ1, quaternion.mQ2, quaternion.mQ3);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis  The axis
     * @param angle The angle in degrees
     * @return This quaternion for chaining.
     */
    public final Quaternion set(Point axis, float angle) {
        double norm = axis.getLength();
        if (norm == 0) {
            throw new Ngin3dException("MathRuntimeException");
        }
        double angrad = Math.toRadians(angle);

        double halfAngle = 0.5 * angrad;
        double coeff = Math.sin(halfAngle) / norm;

        return this.set((float) Math.cos(halfAngle), (float) (coeff * axis.getX()), (float) (coeff * axis.getY()), (float) (coeff * axis.getZ()));
    }

    /**
     * @return a copy of this quaternion
     */
    public Quaternion cpy() {
        return new Quaternion(this);
    }

    /**
     * @return the euclidian length of this quaternion
     */
    public float len() {
        return (float) Math.sqrt(mQ0 * mQ0 + mQ1 * mQ1 + mQ2 * mQ2 + mQ3 * mQ3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[" + mQ0 + "|" + mQ1 + "|" + mQ2 + "|" + mQ3 + "]";
    }

    /**
     * Sets the quaternion to the given euler angles.
     *
     * @param order  order of rotations to use
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     * @return this quaternion
     */
    public Quaternion setEulerAngles(EulerOrder order, float alpha1, float alpha2, float alpha3) {
        Quaternion r1 = new Quaternion(order.getA1(), alpha1);
        Quaternion r2 = new Quaternion(order.getA2(), alpha2);
        Quaternion r3 = new Quaternion(order.getA3(), alpha3);
        Quaternion composed = r1.applyTo(r2.applyTo(r3));

        mQ0 = composed.mQ0;
        mQ1 = composed.mQ1;
        mQ2 = composed.mQ2;
        mQ3 = composed.mQ3;

        return this;
    }

    /**
     * @return the length of this quaternion without square root
     */
    public float len2() {
        return mQ0 * mQ0 + mQ1 * mQ1 + mQ2 * mQ2 + mQ3 * mQ3;
    }

    /**
     * Normalizes this quaternion to unit length
     *
     * @return the quaternion for chaining
     */
    public Quaternion nor() {
        float len = len2();
        if (len != 0.f && (Math.abs(len - 1.0f) > NORMALIZATION_TOLERANCE)) {
            len = (float) Math.sqrt(len);
            mQ3 /= len;
            mQ0 /= len;
            mQ1 /= len;
            mQ2 /= len;
        }
        return this;
    }

    public Quaternion applyTo(Quaternion r) {
        float newQ0 = r.mQ0 * mQ0 - (r.mQ1 * mQ1 + r.mQ2 * mQ2 + r.mQ3 * mQ3);
        float newQ1 = r.mQ1 * mQ0 + r.mQ0 * mQ1 + r.mQ2 * mQ3 - r.mQ3 * mQ2;
        float newQ2 = r.mQ2 * mQ0 + r.mQ0 * mQ2 + r.mQ3 * mQ1 - r.mQ1 * mQ3;
        float newQ3 = r.mQ3 * mQ0 + r.mQ0 * mQ3 + r.mQ1 * mQ2 - r.mQ2 * mQ1;

        mQ0 = newQ0;
        mQ1 = newQ1;
        mQ2 = newQ2;
        mQ3 = newQ3;
        return this;
    }

    /**
     * Sets the quaternion to an identity Quaternion
     *
     * @return this quaternion for chaining
     */
    public final Quaternion idt() {
        this.set(1, 0, 0, 0);
        return this;
    }

    /**
     * Spherical linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
     * [0,1]. Taken from. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
     *
     * @param end   the end quaternion
     * @param alpha alpha in the range [0,1]
     * @return this quaternion for chaining
     */
    public Quaternion slerp(Quaternion end, float alpha) {
        if (this.equals(end)) {
            return this;
        }

        float result = dot(end);

        if (result < 0.0) {
            // Negate the second quaternion and the result of the dot product
            end.mul(-1);
            result = -result;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - alpha;
        float scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1) { // Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double theta = Math.acos(result);
            final double invSinTheta = 1f / Math.sin(theta);

            // Calculate the scale for mQ1 and mQ2, according to the angle and
            // it's sine value
            scale0 = (float) (Math.sin((1 - alpha) * theta) * invSinTheta);
            scale1 = (float) (Math.sin((alpha * theta)) * invSinTheta);
        }

        // Calculate the mQ0, mQ1, mQ2 and mQ3 values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        final float x = (scale0 * this.mQ0) + (scale1 * end.mQ0);
        final float y = (scale0 * this.mQ1) + (scale1 * end.mQ1);
        final float z = (scale0 * this.mQ2) + (scale1 * end.mQ2);
        final float w = (scale0 * this.mQ3) + (scale1 * end.mQ3);
        set(x, y, z, w);

        // Return the interpolated quaternion
        return this;
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quaternion)) {
            return false;
        }
        final Quaternion comp = (Quaternion) o;
        return this.mQ0 == comp.mQ0 && this.mQ1 == comp.mQ1 && this.mQ2 == comp.mQ2 && this.mQ3 == comp.mQ3;
    }

    @Override
    public int hashCode() {
        int result = (mQ0 == +0.0f ? 0 : Float.floatToIntBits(mQ0));
        result = 31 * result + (mQ1 == +0.0f ? 0 : Float.floatToIntBits(mQ1));
        result = 31 * result + (mQ2 == +0.0f ? 0 : Float.floatToIntBits(mQ2));
        result = 31 * result + (mQ3 == +0.0f ? 0 : Float.floatToIntBits(mQ3));
        return result;
    }

    /**
     * Dot product between this and the other quaternion.
     *
     * @param other the other quaternion.
     * @return this quaternion for chaining.
     */
    public float dot(Quaternion other) {
        return mQ0 * other.mQ0 + mQ1 * other.mQ1 + mQ2 * other.mQ2 + mQ3 * other.mQ3;
    }

    /**
     * Multiplies the components of this quaternion with the given scalar.
     *
     * @param scalar the scalar.
     * @return this quaternion for chaining.
     */
    public Quaternion mul(float scalar) {
        this.mQ0 *= scalar;
        this.mQ1 *= scalar;
        this.mQ2 *= scalar;
        this.mQ3 *= scalar;
        return this;
    }

    /**
     * Get the normalized axis of the rotation.
     *
     * @return normalized axis of the rotation
     */
    public Point getAxis() {
        double squaredSine = mQ1 * mQ1 + mQ2 * mQ2 + mQ3 * mQ3;
        if (squaredSine == 0) {
            return new Point(1, 0, 0);
        }
        float inverse = 1 / (float) Math.sqrt(squaredSine);
        return new Point(mQ1 * inverse, mQ2 * inverse, mQ3 * inverse);
    }

    /**
     * Get the angle of the rotation.
     *
     * @return angle of the rotation (between 0 and &pi * 2;)
     */
    public float getAxisAngle() {
        return 2 * (float) Math.toDegrees(Math.acos(mQ0));
    }

    /**
     * Get the Cardan or Euler angles corresponding to the instance.
     * <p/>
     * <p>The equations show that each rotation can be defined by two
     * different values of the Cardan or Euler angles set. For example
     * if Cardan angles are used, the rotation defined by the angles
     * a<sub>1</sub>, a<sub>2</sub> and a<sub>3</sub> is the same as
     * the rotation defined by the angles &pi; + a<sub>1</sub>, &pi;
     * - a<sub>2</sub> and &pi; + a<sub>3</sub>. This method implements
     * the following arbitrary choices:</p>
     * <ul>
     * <li>for Cardan angles, the chosen set is the one for which the
     * second angle is between -&pi;/2 and &pi;/2 (i.e its cosine is
     * positive),</li>
     * <li>for Euler angles, the chosen set is the one for which the
     * second angle is between 0 and &pi; (i.e its sine is positive).</li>
     * </ul>
     * <p/>
     * <p>Cardan and Euler angle have a very disappointing drawback: all
     * of them have singularities. This means that if the instance is
     * too close to the singularities corresponding to the given
     * rotation order, it will be impossible to retrieve the angles. For
     * Cardan angles, this is often called gimbal lock. There is
     * <em>nothing</em> to do to prevent this, it is an intrinsic problem
     * with Cardan and Euler representation (but not a problem with the
     * rotation itself, which is perfectly well defined). For Cardan
     * angles, singularities occur when the second angle is close to
     * -&pi;/2 or +&pi;/2, for Euler angle singularities occur when the
     * second angle is close to 0 or &pi;, this implies that the identity
     * rotation is always singular for Euler angles!</p>
     *
     * @param order rotation order to use
     * @return an array of three angles, in the order specified by the set
     *         singular with respect to the angles set specified
     */
    public float[] getEulerAngles(EulerOrder order) {
        if (order == EulerOrder.XYZ) {

            // r (Point.plusK) coordinates are :
            //  sin (theta), -cos (theta) sin (phi), cos (theta) cos (phi)
            // (-r) (Point.plusI) coordinates are :
            // cos (psi) cos (theta), -sin (psi) cos (theta), sin (theta)
            // and we can choose to have theta in the interval [-PI/2 ; +PI/2]
            Point v1 = applyTo(Point.Z_AXIS);
            Point v2 = applyInverseTo(Point.X_AXIS);
            if ((v2.getZ() < -0.9999999999) || (v2.getZ() > 0.9999999999)) {
                Log.w(TAG, "Touch Cardan Euler Singularity");
            }
            return new float[] {
                (float) -Math.toDegrees(Math.atan2(-(v1.getY()), v1.getZ())),
                (float) -Math.toDegrees(Math.asin(v2.getZ())),
                (float) -Math.toDegrees(Math.atan2(-(v2.getY()), v2.getX()))
            };

        } else if (order == EulerOrder.XZY) {

            // r (Point.plusJ) coordinates are :
            // -sin (psi), cos (psi) cos (phi), cos (psi) sin (phi)
            // (-r) (Point.plusI) coordinates are :
            // cos (theta) cos (psi), -sin (psi), sin (theta) cos (psi)
            // and we can choose to have psi in the interval [-PI/2 ; +PI/2]
            Point v1 = applyTo(Point.Y_AXIS);
            Point v2 = applyInverseTo(Point.X_AXIS);
            if ((v2.getY() < -0.9999999999) || (v2.getY() > 0.9999999999)) {
                Log.w(TAG, "Touch Cardan Euler Singularity");
            }
            return new float[] {
                (float) -Math.toDegrees(Math.atan2(v1.getZ(), v1.getY())),
                (float) -Math.toDegrees(-Math.asin(v2.getY())),
                (float) -Math.toDegrees(Math.atan2(v2.getZ(), v2.getX()))
            };

        } else if (order == EulerOrder.YXZ) {

            // r (Point.plusK) coordinates are :
            //  cos (phi) sin (theta), -sin (phi), cos (phi) cos (theta)
            // (-r) (Point.plusJ) coordinates are :
            // sin (psi) cos (phi), cos (psi) cos (phi), -sin (phi)
            // and we can choose to have phi in the interval [-PI/2 ; +PI/2]
            Point v1 = applyTo(Point.Z_AXIS);
            Point v2 = applyInverseTo(Point.Y_AXIS);
            if ((v2.getZ() < -0.9999999999) || (v2.getZ() > 0.9999999999)) {
                Log.w(TAG, "Touch Cardan Euler Singularity");
            }
            return new float[] {
                (float) -Math.toDegrees(Math.atan2(v1.getX(), v1.getZ())),
                (float) -Math.toDegrees(-Math.asin(v2.getZ())),
                (float) -Math.toDegrees(Math.atan2(v2.getX(), v2.getY()))
            };

        } else if (order == EulerOrder.YZX) {

            // r (Point.plusI) coordinates are :
            // cos (psi) cos (theta), sin (psi), -cos (psi) sin (theta)
            // (-r) (Point.plusJ) coordinates are :
            // sin (psi), cos (phi) cos (psi), -sin (phi) cos (psi)
            // and we can choose to have psi in the interval [-PI/2 ; +PI/2]
            Point v1 = applyTo(Point.X_AXIS);
            Point v2 = applyInverseTo(Point.Y_AXIS);
            if ((v2.getX() < -0.9999999999) || (v2.getX() > 0.9999999999)) {
                Log.w(TAG, "Touch Cardan Euler Singularity");
            }
            return new float[] {
                (float) -Math.toDegrees(Math.atan2(-(v1.getZ()), v1.getX())),
                (float) -Math.toDegrees(Math.asin(v2.getX())),
                (float) -Math.toDegrees(Math.atan2(-(v2.getZ()), v2.getY()))
            };

        } else if (order == EulerOrder.ZXY) {

            // r (Point.plusJ) coordinates are :
            // -cos (phi) sin (psi), cos (phi) cos (psi), sin (phi)
            // (-r) (Point.plusK) coordinates are :
            // -sin (theta) cos (phi), sin (phi), cos (theta) cos (phi)
            // and we can choose to have phi in the interval [-PI/2 ; +PI/2]
            Point v1 = applyTo(Point.Y_AXIS);
            Point v2 = applyInverseTo(Point.Z_AXIS);
            if ((v2.getY() < -0.9999999999) || (v2.getY() > 0.9999999999)) {
                Log.w(TAG, "Touch Cardan Euler Singularity");
            }
            return new float[] {
                (float) -Math.toDegrees(Math.atan2(-(v1.getX()), v1.getY())),
                (float) -Math.toDegrees(Math.asin(v2.getY())),
                (float) -Math.toDegrees(Math.atan2(-(v2.getX()), v2.getZ()))
            };

        } else { // last possibility is ZYX

            // r (Point.plusI) coordinates are :
            //  cos (theta) cos (psi), cos (theta) sin (psi), -sin (theta)
            // (-r) (Point.plusK) coordinates are :
            // -sin (theta), sin (phi) cos (theta), cos (phi) cos (theta)
            // and we can choose to have theta in the interval [-PI/2 ; +PI/2]
            Point v1 = applyTo(Point.X_AXIS);
            Point v2 = applyInverseTo(Point.Z_AXIS);
            if ((v2.getX() < -0.9999999999) || (v2.getX() > 0.9999999999)) {
                Log.w(TAG, "Touch Cardan Euler Singularity");
            }
            return new float[] {
                (float) -Math.toDegrees(Math.atan2(v1.getY(), v1.getX())),
                (float) -Math.toDegrees(-Math.asin(v2.getX())),
                (float) -Math.toDegrees(Math.atan2(v2.getY(), v2.getZ()))
            };
        }
    }

    public float[] getEulerAngles() {
        return getEulerAngles(EulerOrder.XYZ);
    }

    /**
     * Apply the rotation to a vector.
     *
     * @param u vector to apply the rotation to
     * @return a new vector which is the image of u by the rotation
     */
    public Point applyTo(Point u) {

        float x = u.getX();
        float y = u.getY();
        float z = u.getZ();

        float s = mQ1 * x + mQ2 * y + mQ3 * z;

        return new Point(2 * (mQ0 * (x * mQ0 - (mQ2 * z - mQ3 * y)) + s * mQ1) - x,
            2 * (mQ0 * (y * mQ0 - (mQ3 * x - mQ1 * z)) + s * mQ2) - y,
            2 * (mQ0 * (z * mQ0 - (mQ1 * y - mQ2 * x)) + s * mQ3) - z);

    }

    /**
     * Apply the inverse of the rotation to a vector.
     *
     * @param u vector to apply the inverse of the rotation to
     * @return a new vector which such that u is its image by the rotation
     */
    public Point applyInverseTo(Point u) {

        float x = u.getX();
        float y = u.getY();
        float z = u.getZ();

        float s = mQ1 * x + mQ2 * y + mQ3 * z;
        float m0 = -mQ0;

        return new Point(2 * (m0 * (x * m0 - (mQ2 * z - mQ3 * y)) + s * mQ1) - x,
            2 * (m0 * (y * m0 - (mQ3 * x - mQ1 * z)) + s * mQ2) - y,
            2 * (m0 * (z * m0 - (mQ1 * y - mQ2 * x)) + s * mQ3) - z);

    }

    /**
     * Get the scalar coordinate of the quaternion.
     *
     * @return scalar coordinate of the quaternion
     */
    public float getQ0() {
        return mQ0;
    }

    /**
     * Get the first coordinate of the vectorial part of the quaternion.
     *
     * @return first coordinate of the vectorial part of the quaternion
     */
    public float getQ1() {
        return mQ1;
    }

    /**
     * Get the second coordinate of the vectorial part of the quaternion.
     *
     * @return second coordinate of the vectorial part of the quaternion
     */
    public float getQ2() {
        return mQ2;
    }

    /**
     * Get the third coordinate of the vectorial part of the quaternion.
     *
     * @return third coordinate of the vectorial part of the quaternion
     */
    public float getQ3() {
        return mQ3;
    }

}

