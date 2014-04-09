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

import com.mediatek.ngin3d.presentation.ImageDisplay;
import com.mediatek.ngin3d.presentation.PresentationEngine;

/**
 * Plane is actor with width and height.
 */
public class Plane extends Actor {
    protected boolean mIsYUp;

    // Previously, default anchor point is shifted to Point (0.5, 0.5) in actor
    // class. It would appy all actors. But for 3D model, this shift is less
    // relevant. Therefore, the shift moves to here which applies to 2D quad
    // only.
    public Plane() {
        this(false);
    }

    public Plane(boolean isYUp) {
        setAnchorPoint(new Point(0.5f, 0.5f));
        mIsYUp = isYUp;
    }

    /**
     * @hide
     */
    @Override
    protected ImageDisplay createPresentation(PresentationEngine engine) {
        return engine.createImageDisplay(mIsYUp);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Property handling

    /**
     * @hide
     */
    public static final Property<Dimension> PROP_SIZE = new Property<Dimension>("size", new Dimension());
    /**
     * @hide
     */
    public static final Property<Box> PROP_SRC_RECT = new Property<Box>("src_rect", null);

    static final Property<Boolean> PROP_DOUBLE_SIDED = new Property<Boolean>("double_sided", false);
    protected boolean applyValue(Property property, Object value) {
        if (super.applyValue(property, value)) {
            return true;
        }

        if (property.sameInstance(PROP_SRC_RECT)) {
            Box box = (Box) value;
            getPresentation().setSourceRect(box);
            return true;
        } else if (property.sameInstance(PROP_SIZE)) {
            Dimension size = (Dimension) value;
            getPresentation().setSize(size);
            return true;
        } else if (property.sameInstance(PROP_DOUBLE_SIDED)) {
            Boolean enable = (Boolean) value;
            getPresentation().enableDoubleSided(enable);
            return true;
        }
        return false;
    }

    /**
     * Create a simple plane.
     * @param isYUp   true for creating a Y-up quad, default is Y-down
     */
    public static Plane create(boolean isYUp) {
        return new Plane(isYUp);
    }

    /**
     * Create a plane with specific size.
     * @param size  the dimension of plane
     */
    public static Plane create(Dimension size) {
        return create(size, false);
    }

    /**
     * Create a plane with specific size.
     * @param size  the dimension of plane
     * @param isYUp   true for creating a Y-up quad, default is Y-down
     */
    public static Plane create(Dimension size, boolean isYUp) {
        Plane plane = create(isYUp);
        plane.setSize(size);
        return plane;
    }

    public void setSize(Dimension size) {
        if (size.width < 0 || size.height < 0) {
            throw new IllegalArgumentException("negative value");
        }
        setValueInTransaction(PROP_SIZE, size);
    }

    public Dimension getSize() {
        return getValue(PROP_SIZE);
    }

    public void setSourceRect(Box srcRect) {
        setValueInTransaction(PROP_SRC_RECT, srcRect);
    }

    public Box getSourceRect() {
        return getValue(PROP_SRC_RECT);
    }

    public void setZOrderOnTop(boolean enable, int zOrder) {
        // enable parameter is now redundant
        setValue(PROP_ZORDER_ON_TOP, zOrder);
    }

    /**
     * Set whether the image is double-sided or not. Normally polygons facing
     * away from the camera are omitted from the rendering to optimise speed.
     * Occasionally it is necessary to mark certain polygon as
     * visible-from-both-sides so this optimisation is to be turned off and
     * the polygon drawn regardless.
     *
     * @param enable true to make the image double-sided
     */
    public void setDoubleSided(boolean enable) {
        setValue(PROP_DOUBLE_SIDED, enable);
    }

    /**
     * Returns the Actor's presentation cast to the instantiated type.
     * @return Presentation object
     */
    @Override
    public ImageDisplay getPresentation() {
        return (ImageDisplay) mPresentation;
    }
}
