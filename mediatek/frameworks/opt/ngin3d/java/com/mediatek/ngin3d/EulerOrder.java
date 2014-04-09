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


/**
 * This class is a utility representing a rotation order specification
 * for Cardan or Euler angles specification.
 *
 * This class cannot be instanciated by the user. He can only use one
 * of the twelve predefined supported orders as an argument to either
 * the {@link Rotation#Rotation(EulerOrder,float,float,float)}
 * constructor or the {@link Rotation#getEulerAngles} method.
 *
 * @version $Id: EulerOrder.java 1131229 2011-06-03 20:49:25Z luc $
 * @since 1.2
 */
public final class EulerOrder {

    /** Set of Cardan angles.
     * this ordered set of rotations is around X, then around Y, then
     * around Z
     */
    public static final EulerOrder XYZ =
        new EulerOrder("XYZ", Point.X_AXIS, Point.Y_AXIS, Point.Z_AXIS);

    /** Set of Cardan angles.
     * this ordered set of rotations is around X, then around Z, then
     * around Y
     */
    public static final EulerOrder XZY =
        new EulerOrder("XZY", Point.X_AXIS, Point.Z_AXIS, Point.Y_AXIS);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Y, then around X, then
     * around Z
     */
    public static final EulerOrder YXZ =
        new EulerOrder("YXZ", Point.Y_AXIS, Point.X_AXIS, Point.Z_AXIS);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Y, then around Z, then
     * around X
     */
    public static final EulerOrder YZX =
        new EulerOrder("YZX", Point.Y_AXIS, Point.Z_AXIS, Point.X_AXIS);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Z, then around X, then
     * around Y
     */
    public static final EulerOrder ZXY =
        new EulerOrder("ZXY", Point.Z_AXIS, Point.X_AXIS, Point.Y_AXIS);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Z, then around Y, then
     * around X
     */
    public static final EulerOrder ZYX =
        new EulerOrder("ZYX", Point.Z_AXIS, Point.Y_AXIS, Point.X_AXIS);

    /** Set of Euler angles.
     * this ordered set of rotations is around X, then around Y, then
     * around X
     */
    public static final EulerOrder XYX =
        new EulerOrder("XYX", Point.X_AXIS, Point.Y_AXIS, Point.X_AXIS);

    /** Set of Euler angles.
     * this ordered set of rotations is around X, then around Z, then
     * around X
     */
    public static final EulerOrder XZX =
        new EulerOrder("XZX", Point.X_AXIS, Point.Z_AXIS, Point.X_AXIS);

    /** Set of Euler angles.
     * this ordered set of rotations is around Y, then around X, then
     * around Y
     */
    public static final EulerOrder YXY =
        new EulerOrder("YXY", Point.Y_AXIS, Point.X_AXIS, Point.Y_AXIS);

    /** Set of Euler angles.
     * this ordered set of rotations is around Y, then around Z, then
     * around Y
     */
    public static final EulerOrder YZY =
        new EulerOrder("YZY", Point.Y_AXIS, Point.Z_AXIS, Point.Y_AXIS);

    /** Set of Euler angles.
     * this ordered set of rotations is around Z, then around X, then
     * around Z
     */
    public static final EulerOrder ZXZ =
        new EulerOrder("ZXZ", Point.Z_AXIS, Point.X_AXIS, Point.Z_AXIS);

    /** Set of Euler angles.
     * this ordered set of rotations is around Z, then around Y, then
     * around Z
     */
    public static final EulerOrder ZYZ =
        new EulerOrder("ZYZ", Point.Z_AXIS, Point.Y_AXIS, Point.Z_AXIS);

    /** Name of the rotations order. */
    private final String mName;

    /** Axis of the first rotation. */
    private final Point mA1;

    /** Axis of the second rotation. */
    private final Point mA2;

    /** Axis of the third rotation. */
    private final Point mA3;

    /** Private constructor.
     * This is a utility class that cannot be instantiated by the user,
     * so its only constructor is private.
     * @param name name of the rotation order
     * @param a1 axis of the first rotation
     * @param a2 axis of the second rotation
     * @param a3 axis of the third rotation
     */
    private EulerOrder(final String name,
                       final Point a1, final Point a2, final Point a3) {
        this.mName = name;
        this.mA1 = a1;
        this.mA2 = a2;
        this.mA3 = a3;
    }

    /** Get a string representation of the instance.
     * @return a string representation of the instance (in fact, its mName)
     */
    @Override
    public String toString() {
        return mName;
    }

    /** Get the axis of the first rotation.
     * @return axis of the first rotation
     */
    public Point getA1() {
        return mA1;
    }

    /** Get the axis of the second rotation.
     * @return axis of the second rotation
     */
    public Point getA2() {
        return mA2;
    }

    /** Get the axis of the second rotation.
     * @return axis of the second rotation
     */
    public Point getA3() {
        return mA3;
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

        EulerOrder order = (EulerOrder) o;
        if (!mName.equals(order.mName)) return false;
        if (!mA1.equals(order.mA1)) return false;
        if (!mA2.equals(order.mA2)) return false;
        if (!mA3.equals(order.mA3)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (mName.hashCode());
        result = 31 * result + mA1.hashCode();
        result = 31 * result + mA2.hashCode();
        result = 31 * result + mA3.hashCode();
        return result;
    }
}
