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
 * MediaTek Inc. (C) 2013. All rights reserved.
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

import com.mediatek.ngin3d.presentation.IActorNodePresentation;
import com.mediatek.ngin3d.presentation.IActorNodePresentation.CollisionType;
import com.mediatek.ngin3d.presentation.Presentation;

/**
 * References and controls a sub-section of an Actor.
 */
public class ActorNode extends Base {
    /** @hide */
    private static final String TAG = "ActorNode";

    /**
     * Actor node name referenced by this node.
     * @hide
     */
    private final String mNodeName;

    /**
     * Node presentation layer object.
     * @hide
     */
    private IActorNodePresentation mPresentation;

    /**
     * Custom user-defined tag.
     */
    private int mTag;

    /**
     * Creates an node pointing to a sub-section of an actor.
     */
    protected ActorNode(String nodeName) {
        mNodeName = nodeName;
    }

    /**
     * Visible property
     * @hide
     */
    public static final Property<Boolean> PROP_VISIBLE =
        new Property<Boolean>("visible", null);

    /**
     * Collision shape property
     * @hide
     */
    public static final Property<Integer> PROP_COLLISION_SHAPE =
        new Property<Integer>("collision_shape", null);

    /**
     * Collision position property
     * @hide
     */
    public static final Property<Point> PROP_COLLISION_POSITION =
        new Property<Point>("collision_position", null);

    /**
     * Collision rotation property
     * @hide
     */
    public static final Property<Rotation> PROP_COLLISION_ROTATION =
        new Property<Rotation>("collision_rotation", null);

    /**
     * Collision scale property
     * @hide
     */
    public static final Property<Scale> PROP_COLLISION_SCALE =
        new Property<Scale>("collision_scale", null);

    /**
     * Collision visible property
     * @hide
     */
    public static final Property<Boolean> PROP_COLLISION_VISIBLE =
        new Property<Boolean>("collision_visible", null);

    /**
     * Sets an actor property.
     * @param property Property to set
     * @param newValue Value to use
     * @return True if the property is set successfully
     * @hide
     */
    @Override
    public final <T> boolean setValue(
            Property<T> property, T newValue, boolean dirty) {
        if (super.setValue(property, newValue, dirty)) {
            // This function is overridden to ensure the scene is re-rendered.
            if (mPresentation != null) {
                mPresentation.requestRender();
            }
            return true;
        }
        return false;
    }

    /**
     * Sets an actor property.
     * @param property Property to set
     * @param newValue Value to use
     * @return True if the property is set successfully
     * @hide
     */
    @Override
    public final <T> boolean setValue(Property<T> property, T newValue) {
        return setValue(property, newValue, true);
    }

    /**
     * Applies a property to the node.
     *
     * @param property Property to apply
     * @param value Value to apply
     * @return True if the property was applied successfully
     * @hide
     */
    protected boolean applyValue(Property property, Object value) {
        // If the value is null, this is generally because the default property
        // value is null.  We take it to mean "do nothing".

        if (property.sameInstance(PROP_VISIBLE)) {
            if (value != null) {
                mPresentation.setVisible((Boolean) value);
            }
            return true;
        } else if (property.sameInstance(PROP_COLLISION_SHAPE)) {
            if (value != null) {
                mPresentation.setCollisionShape((Integer) value);
            }
            return true;
        } else if (property.sameInstance(PROP_COLLISION_POSITION)) {
            if (value != null) {
                mPresentation.setCollisionPosition((Point) value);
            }
            return true;
        } else if (property.sameInstance(PROP_COLLISION_ROTATION)) {
            if (value != null) {
                mPresentation.setCollisionRotation((Rotation) value);
            }
            return true;
        } else if (property.sameInstance(PROP_COLLISION_SCALE)) {
            if (value != null) {
                mPresentation.setCollisionScale((Scale) value);
            }
            return true;
        } else if (property.sameInstance(PROP_COLLISION_VISIBLE)) {
            if (value != null) {
                mPresentation.setCollisionVisible((Boolean) value);
            }
            return true;
        }

        return false;
    }

    /**
     * @hide
     */
    protected void applyBatchValues() {
        // Do nothing by default
    }

    /**
     * Realize this actor.
     * @param actorPresentation Presentation used for realizing actor
     * @hide
     */
    public void realize(Presentation actorPresentation) {
        if (mPresentation == null) {
            mPresentation = (IActorNodePresentation)
                actorPresentation.createActorNodePresentation(mNodeName);
            mPresentation.initialize(this);

            // There are dependencies between static properties, ex:
            // PROP_SRC_RECT depends on PROP_IMG_SRC In the end we have to use
            // applyAllProperties() to apply all properties with dependencies
            // which applyAllExistingValues doesn't consider it.
            applyAllProperties();
        }

        applyAllDirtyValues();
    }

    /**
     * Check if this actor is realized.
     * @return  ture if the actor is realized
     */
    public boolean isRealized() {
        return (mPresentation != null);
    }

    /**
     * Un-realize this actor.
     * @hide
     */
    public void unrealize() {
        if (mPresentation != null) {
            mPresentation.uninitialize();
            mPresentation = null;
        }
    }

    /**
     * Sets whether the node is visible.
     * @param visible Visibility flag
     */
    public void setVisible(boolean visible) {
        setValue(PROP_VISIBLE, visible);
    }

    /**
     * Sets the node to use an unbounded collision plane for hit-test
     * operations.
     */
    public void disableCollision() {
        setValue(PROP_COLLISION_SHAPE, CollisionType.NONE);
    }

    /**
     * Sets the node to use an unbounded collision plane for hit-test
     * operations.
     */
    public void useCollisionPlane() {
        setValue(PROP_COLLISION_SHAPE, CollisionType.PLANE);
    }

    /**
     * Sets the node to use a collision 1x1 square for hit-test operations.
     */
    public void useCollisionSquare() {
        setValue(PROP_COLLISION_SHAPE, CollisionType.SQUARE);
    }

    /**
     * Sets the node to use a collision sphere for hit-test operations.
     */
    public void useCollisionSphere() {
        setValue(PROP_COLLISION_SHAPE, CollisionType.SPHERE);
    }

    /**
     * Sets the position of the collision sphere relative to the node.
     * @param position Position of shape
     */
    public void setCollisionPosition(Point position) {
        setValue(PROP_COLLISION_POSITION, position);
    }

    /**
     * Sets the rotation of the collision sphere relative to the node.
     * @param position Rotation of shape
     */
    public void setCollisionRotation(Rotation rotation) {
        setValue(PROP_COLLISION_ROTATION, rotation);
    }

    /**
     * Sets the scale of the collision sphere relative to the node.
     * @param scale Scale of shape
     */
    public void setCollisionScale(Scale scale) {
        setValue(PROP_COLLISION_SCALE, scale);
    }

    /**
     * Sets whether to render the collision shape.
     * This function is useful for debugging and setting up collision shapes.
     * @param visible Visibility flag
     */
    public void setCollisionVisible(boolean visible) {
        setValue(PROP_COLLISION_VISIBLE, visible);
    }

    /**
     * Returns whether the node is dirty (needs re-rendering).
     * @return True if this node is dirty
     * @hide
     */
    public boolean isDirty() {
        return dirtyValueExists();
    }

    /**
     * Set the actor tag.
     * @param tag actor tag
     */
    public void setTag(int tag) {
        mTag = tag;
    }

    /**
     * Get the actor tag.
     * @return actor tag
     */
    public int getTag() {
        return mTag;
    }

    /**
     * Construct a string representation of the node.
     * @return  output string
     */
    @Override
    public String toString() {
        return "ActorNode{" + "mPresentation=" + mPresentation
            + ", mNodeName=" + mNodeName + "}";
    }
}
