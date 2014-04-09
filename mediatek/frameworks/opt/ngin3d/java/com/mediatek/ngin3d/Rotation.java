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
 * This class is responsible for rotation operation in 3D space.
 * There are several mathematical entities to represent a Rotation.
 * The class hiding this implementation details in quaternion
 * and presents an higher level API for user. User can build a
 * rotation from any of these representations and quaternion can
 * help transform the entities between them.
 */

public class Rotation implements JSON.ToJson {

    /**
     * Use Quaternion to represent the rotation
     */
    public static final int MODE_QUATERNION = 0;
    /**
     * An Euler way to represent the rotation
     */
    public static final int MODE_XYZ_EULER = 1;
    /**
     * Axis and angle way to represent the rotation
     */
    public static final int MODE_AXIS_ANGLE = 2;

    /*
     * Thresholds for geometric operations
     */
    private static final float ZERO_THRESHOLD = 0.0001f;
    private static final float DOT_THRESHOLD = 0.9995f;
    /**
     * The Euler Angles in degree.
     */
    private final Quaternion mQuaternion = new Quaternion();

    /**
     * The Euler Angles in degree.
     */
    private float[] mEulerAngles = new float[3];

    /**
     * The angle of the rotation in degree
     */
    private float mAngle;

    /**
     * The axis of the rotation
     */
    private Point mAxis;

    /**
     * The intrinsic type of rotation.
     */
    private int mMode;

    public Rotation() {
        this(1, 0, 0, 0);
    }

    /**
     * Build a rotation from the quaternion coordinates.
     *
     * @param q0        scalar part of the quaternion
     * @param q1        first coordinate of the vectorial part of the quaternion
     * @param q2        second coordinate of the vectorial part of the quaternion
     * @param q3        third coordinate of the vectorial part of the quaternion
     * @param normalize if true, the coordinates are considered
     *                  not to be normalized, a normalization preprocessing step is performed
     *                  before using them
     */
    public Rotation(float q0, float q1, float q2, float q3, boolean normalize) {
        set(q0, q1, q2, q3, normalize);
    }

    public final void set(float q0, float q1, float q2, float q3, boolean normalize) {
        mQuaternion.set(q0, q1, q2, q3);
        if (normalize) {
            mQuaternion.nor();
        }
        mMode = MODE_QUATERNION;
    }

    /**
     * Build a rotation from an axis and an angle.
     *
     * @param axis  axis around which to rotate
     * @param angle rotation angle in degree.
     * @throws ArithmeticException if the axis norm is zero
     */
    public Rotation(Point axis, float angle) {
        set(axis, angle);
    }

    public final void set(Point axis, float angle) {
        mQuaternion.set(axis, angle);
        mMode = MODE_AXIS_ANGLE;
        mAngle = angle;
        mAxis = axis;
    }

    public Rotation(float x, float y, float z, float angle) {
        set(new Point(x, y, z), angle);
    }

    public final void set(float x, float y, float z, float angle) {
        set(new Point(x, y, z), angle);
    }

    /**
     * Build a rotation from three Euler elementary rotations with specific order.
     *
     * @param order order of rotations to use
     * @param x     angle of the first elementary rotation
     * @param y     angle of the second elementary rotation
     * @param z     angle of the third elementary rotation
     */
    public Rotation(EulerOrder order, float x, float y, float z) {
        set(order, x, y, z);
    }

    /**
     * Build a rotation from three Euler elementary rotations with default XYZ order.
     *
     * @param x     angle of the first elementary rotation
     * @param y     angle of the second elementary rotation
     * @param z     angle of the third elementary rotation
     */
    public Rotation(float x, float y, float z) {
        this(EulerOrder.XYZ , x, y, z);
    }

    public final void set(EulerOrder order, float x, float y, float z) {
        if (order.equals(EulerOrder.XYZ)) {
            mQuaternion.setEulerAngles(order, x, y, z);
        } else if (order.equals(EulerOrder.XZY)) {
            mQuaternion.setEulerAngles(order, x, z, y);
        } else if (order.equals(EulerOrder.ZYX)) {
            mQuaternion.setEulerAngles(order, z, y, x);
        } else if (order.equals(EulerOrder.ZXY)) {
            mQuaternion.setEulerAngles(order, z, x, y);
        } else if (order.equals(EulerOrder.YZX)) {
            mQuaternion.setEulerAngles(order, y, z, x);
        } else if (order.equals(EulerOrder.YXZ)) {
            mQuaternion.setEulerAngles(order, y, x, z);
        } else {
            mQuaternion.setEulerAngles(order, x, y, z);
        }

        mEulerAngles[0] = x;
        mEulerAngles[1] = y;
        mEulerAngles[2] = z;

        mMode = MODE_XYZ_EULER;
    }

    public final void set(float x, float y, float z) {
        set(EulerOrder.XYZ, x, y, z);
    }

    /**
     * Create a rotation pointing in a given direction.
     * The returned rotation will transform the "startingDirection" vector to
     * the "finishingDirection" vector, using the shortest arc possible.
     *
     * @param startingDirection  direction to transform from
     * @param finishingDirection  direction to transform to
     */
    public static final Rotation pointAt(
            Point startingDirection, Point finishingDirection) {
        Point from = startingDirection.getNormalized();
        Point to = finishingDirection.getNormalized();

        Point half = from.add(to);
        float halfLength = half.getLength();

        // First find a quaternion which will rotate the "from" vector to the
        // "to" vector
        Rotation rotation = new Rotation();
        Quaternion quaternion = rotation.getQuaternion();
        if (halfLength < ZERO_THRESHOLD) {
            Point axis = from.getOrthogonal();
            quaternion.set(axis, 180.f);
        } else {
            half = half.getNormalized();
            float dot = Point.dotProduct(from, half);
            Point cross = Point.crossProduct(from, half);
            quaternion.set(dot, cross.getX(), cross.getY(), cross.getZ());
        }

        return rotation;
    }

    /**
     * Create a rotation pointing in a given direction.
     * The returned rotation will transform the "startingDirection" vector to
     * the "finishingDirection" vector. It also transform the "up" vector to
     * lie in the plane containing the passed "up" vector and the "to" vector
     * (i.e. it will try to keep the objects it rotates "upright").
     *
     * e.g. To create a Rotation which will face a camera towards another object
     * work out the heading from the camera to the object
     * (from=cameraPos-objectPos), and then point the -ve z-axis of the camera
     * in that direction (to=(0,0,-1)), keeping the y-axis "up" (up=(0,1,0)).
     *
     * @param startingDirection  direction to transform from
     * @param finishingDirection  direction to transform to
     * @param startingUp         vector to keep up
     * @param finishingUp        vector to keep up
     */
    public static final Rotation pointAt(
            Point startingDirection, Point finishingDirection,
            Point startingUp, Point finishingUp) {
        Point from = startingDirection.getNormalized();
        Point to = finishingDirection.getNormalized();
        Point upStart = startingUp.getNormalized();
        Point up = finishingUp.getNormalized();

        // Perform basic point-at operation.
        Rotation rotation = pointAt(startingDirection, finishingDirection);
        Quaternion quaternion = rotation.getQuaternion();

        // After pointing, the rotation will have an arbitrary "roll" around
        // the "to" z-axis.  We must find the new position of the "up" vector
        // after rotation.
        Point rolledUp = quaternion.applyTo(upStart);

        // We now project this vector and the original "up" vector onto a plane
        // perpendicular to the "to" vector so that we can find the extra
        // rotation needed to rotate the rolled "up" vector onto the given
        // "up" vector. Note that these vectors won't have unit length.
        Point rolledUpProjected = rolledUp.subtract(
            Point.dotProduct(to, rolledUp), to);
        Point upProjected = up.subtract(Point.dotProduct(to, up), to);

        float magProduct = rolledUpProjected.getLength() * upProjected.getLength();
        if (magProduct > ZERO_THRESHOLD) {
            Point cross = Point.crossProduct(rolledUpProjected, upProjected);
            float sinAngle = cross.getLength() / magProduct;
            float cosAngle = Point.dotProduct(rolledUpProjected, upProjected)
                    / magProduct;
            float rollAngle = (float)Math.toDegrees(Math.atan2(sinAngle, cosAngle));
            // If the cross of the two up vectors point in the opposite
            // direction to the "to" vector, we need to rotate in the opposite
            // direction.
            if (Point.dotProduct(cross, to) < 0) {
                rollAngle = -rollAngle;
            }
            Quaternion rollRot = new Quaternion(to, rollAngle);

            // Combine the "roll-correct" rotation with the previous rotation.
            quaternion.applyTo(rollRot);
        }
        return rotation;
    }

    /**
     * Get the normalized axis of the rotation.
     *
     * @return normalized axis of the rotation
     * @see #Rotation(Point, float)
     */
    public Point getAxis() {
        if (mMode == MODE_AXIS_ANGLE) {
            return mAxis;
        } else {
            return mQuaternion.getAxis();
        }
    }

    /**
     * Get the angle of the rotation.
     *
     * @return angle of the rotation (between 0 and &pi;)
     * @see #Rotation(Point, float)
     */
    public float getAxisAngle() {
        if (mMode == MODE_AXIS_ANGLE) {
            return mAngle;
        } else {
            return mQuaternion.getAxisAngle();
        }
    }

    public float[] getEulerAngles(EulerOrder order) {
        if (mMode == MODE_XYZ_EULER) {
            return mEulerAngles;
        } else {
            return mQuaternion.getEulerAngles(order);
        }

    }

    public float[] getEulerAngles() {
        return getEulerAngles(EulerOrder.XYZ);
    }

    /**
     * Get the coordinate system of this rotation object
     *
     * @return coordinate system mode
     */
    public int getMode() {
        return mMode;
    }

    public Quaternion getQuaternion() {
        return mQuaternion;
    }

    /**
     * Compare the input object is the same as this rotation object.
     *
     * @param o input object to be compared
     * @return true if two objects are the same or their properties are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rotation rotation = (Rotation) o;

        if (mMode != rotation.getMode()) return false;

        if (mMode == MODE_XYZ_EULER) {
            float[] euler = rotation.getEulerAngles();
            if (Float.compare(mEulerAngles[0], euler[0]) != 0) return false;
            if (Float.compare(mEulerAngles[1], euler[1]) != 0) return false;
            if (Float.compare(mEulerAngles[2], euler[2]) != 0) return false;
        } else if (mMode == MODE_AXIS_ANGLE) {
            float angle = rotation.getAxisAngle();
            Point axis = rotation.getAxis();
            if (Float.compare(mAngle, angle) != 0) return false;

            return mAxis.equals(axis);
        } else {
            return mQuaternion.equals(rotation.getQuaternion());
        }

        return true;
    }

    /**
     * Create a new hash code.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result;
        if (mMode == MODE_XYZ_EULER) {
            result = (mEulerAngles[0] == +0.0f ? 0 : Float.floatToIntBits(mEulerAngles[0]));
            result = 31 * result + (mEulerAngles[1] == +0.0f ? 0 : Float.floatToIntBits(mEulerAngles[1]));
            result = 31 * result + (mEulerAngles[2] == +0.0f ? 0 : Float.floatToIntBits(mEulerAngles[2]));
            return result;
        } else if (mMode == MODE_AXIS_ANGLE) {
            result = (mAxis.getX() == +0.0f ? 0 : Float.floatToIntBits(mAxis.getX()));
            result = 31 * result + (mAxis.getY() == +0.0f ? 0 : Float.floatToIntBits(mAxis.getY()));
            result = 31 * result + (mAxis.getZ() == +0.0f ? 0 : Float.floatToIntBits(mAxis.getZ()));
            result = 31 * result + (mAngle == +0.0f ? 0 : Float.floatToIntBits(mAngle));
            return result;
        } else {
            return 31 * mQuaternion.hashCode();
        }
    }

    /**
     * Convert the rotation property to string for output
     * @return   output string
     */
    @Override
    public String toString() {
        if (mMode == MODE_AXIS_ANGLE) {
            return "Rotation:[" + mAxis.getX() + ", " + mAxis.getY() + ", " + mAxis.getZ() + "], Mode: \"Axis Angle\", Angle: " + mAngle;
        } else {
            return "Rotation:[" + mEulerAngles[0] + ", " + mEulerAngles[1] + ", " + mEulerAngles[2] + "], Mode: \"Euler\" ";
        }
    }

    /**
     * Convert the rotation property to JSON formatted String
     * @return   output JSON formatted String
     */
    public String toJson() {
        if (mMode == MODE_AXIS_ANGLE) {
            return "{Rotation:[" + mAxis.getX() + ", " + mAxis.getY() + ", " + mAxis.getZ() + "], Mode: \"Axis Angle\", Angle: " + mAngle + "}";
        } else {
            return "{Rotation:[" + mEulerAngles[0] + ", " + mEulerAngles[1] + ", " + mEulerAngles[2] + "], Mode: \"Euler\" " + "}";
        }
    }

    public static Rotation newFromString(String positionString) {
        float[] xyz = Utils.parseStringToFloat(positionString);
        return new Rotation(xyz[0], xyz[1], xyz[2]);
    }
}
