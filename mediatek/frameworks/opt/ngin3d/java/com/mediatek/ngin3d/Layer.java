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

import com.mediatek.ngin3d.presentation.Presentation;
import com.mediatek.ngin3d.presentation.PresentationEngine;
import com.mediatek.ngin3d.presentation.PresentationHitTestResult;
import com.mediatek.ngin3d.presentation.RenderLayer;

/**
 * Layer is a special Container that displays its Actors on the screen.
 * Each Layer object has its own camera settings. Layer objects are added to
 * the Stage object. They are drawn in the order that they are added to the
 * Stage.
 *
 * \ingroup ngin3dClasses
 */
public class Layer extends Container {
    protected static final String TAG = "Layer";
    /*
     * Projection modes
     */
    /** Orthographic projection mode. */
    public static final int ORTHOGRAPHIC = 0;
    /** Perspective projection mode. */
    public static final int PERSPECTIVE = 1;

    private RenderLayer mRenderLayer;
    private Point mPosition;

    /**
     * @hide
     */
    @Override
    protected Presentation createPresentation(PresentationEngine engine) {
        Presentation p = super.createPresentation(engine);
        p.initialize(this);
        if (mRenderLayer == null) {
            mRenderLayer = engine.createRenderLayer(p);
        }
        return p;
    }

    public RenderLayer getRenderLayer() {
        return mRenderLayer;
    }

    /**
     * @hide
     */
    static final Property<Point> PROP_CAMERA_POS = new Property<Point>(
            "camera", null);
    /**
     * @hide
     */
    static final Property<Rotation> PROP_CAMERA_ROT = new Property<Rotation>(
            "camera_rotation", null);
    /**
     * @hide
     */
    static final Property<Float> PROP_CAMERA_FOV = new Property<Float>(
            "camera_fov", null);
    /**
     * @hide
     */
    static final Property<Float> PROP_CAMERA_NEAR = new Property<Float>(
            "camera_near", null);
    /**
     * @hide
     */
    static final Property<Float> PROP_CAMERA_FAR = new Property<Float>(
            "camera_far", null);
    /**
     * @hide
     */
    static final Property<Float> PROP_CAMERA_WIDTH = new Property<Float>(
            "camera_width", null);
    /**
     * @hide
     */
    static final Property<Integer> PROP_PROJECTION_MODE = new Property<Integer>(
            "proj_mode", null);
    /**
     * @hide
     */
    static final Property<Boolean> PROP_CLEAR_DEPTH = new Property<Boolean>(
            "clear_depth", null);
    /**
     * @hide
     */
    static final Property<String> PROP_NAMED_CAMERA = new Property<String>(
            "named_camera", null);


    protected boolean applyValue(Property property, Object value) {
        if (super.applyValue(property, value)) {
            return true;
        }

        if (property.sameInstance(PROP_CAMERA_POS)) {
            if (value != null) {
                Point position = (Point) value;
                mRenderLayer.setCameraPosition(position);
            }
            return true;
        } else if (property.sameInstance(PROP_CAMERA_ROT)) {
            if (value != null) {
                Rotation rotation = (Rotation) value;
                mRenderLayer.setCameraRotation(rotation);
            }
            return true;
        } else if (property.sameInstance(PROP_CAMERA_FOV)) {
            if (value != null) {
                Float fov = (Float) value;
                mRenderLayer.setCameraFov(fov);
            }
            return true;
        } else if (property.sameInstance(PROP_CAMERA_NEAR)) {
            if (value != null) {
                Float near = (Float) value;
                mRenderLayer.setCameraNear(near);
            }
            return true;
        } else if (property.sameInstance(PROP_CAMERA_FAR)) {
            if (value != null) {
                Float far = (Float) value;
                mRenderLayer.setCameraFar(far);
            }
            return true;
        } else if (property.sameInstance(PROP_CAMERA_WIDTH)) {
            if (value != null) {
                Float far = (Float) value;
                mRenderLayer.setCameraWidth(far);
            }
            return true;
        } else if (property.sameInstance(PROP_PROJECTION_MODE)) {
            if (value != null) {
                Integer mode = (Integer) value;
                mRenderLayer.setProjectionMode(mode);
            }
            return true;
        } else if (property.sameInstance(PROP_NAMED_CAMERA)) {
            if (value != null) {
                String name = (String) value;
                mRenderLayer.useNamedCamera(name);
            }
            return true;
        } else if (property.sameInstance(PROP_CLEAR_DEPTH)) {
            if (value != null) {
                mRenderLayer.setDepthClear((Boolean)value);
            }
            return true;
        }

        return false;
    }

    /**
     * Sets the camera position.
     * The camera will be set to the given position. The orientation is
     * unaffected.
     *
     * @param position Camera position
     */
    public void setCameraPosition(Point position) {
        mPosition = position;
        setValueInTransaction(PROP_CAMERA_POS, position);
    }

    /**
     * Sets the camera orientation.
     * The camera will oriented to the given rotation.
     *
     * @param rotation Camera orientation
     */
    public void setCameraRotation(Rotation rotation) {
        setValueInTransaction(PROP_CAMERA_ROT, rotation);
    }

    /**
     * Sets the camera position and orientation.
     * The camera will be set to the given position and oriented so that
     * it is pointing towards the "lookAt" point. If you subsequently change
     * the camera position it will not re-orient itself to look at the given
     * point. The subsequent camera rotation will have its "up" vector as
     * close to (0,1,0) as possible.
     *
     * @param lookAt    Camera focus point
     */
    public void setCameraLookAt(Point lookAt) {
        setCameraLookAt(lookAt, new Point(0, 1, 0));
    }

    /**
     * Sets the camera position and orientation.
     * The camera will be set to the given position and oriented so that
     * it is pointing towards the "lookAt" point. If you subsequently change
     * the camera position it will not re-orient itself to look at the given
     * point.
     *
     * @param lookAt    Camera focus point
     * @param up        Vector to keep up (usually (0,1,0) or (0,-1,0))
     */
    public void setCameraLookAt(Point lookAt, Point up) {
        Point to = new Point(lookAt.x - mPosition.x,
                lookAt.y - mPosition.y, lookAt.z - mPosition.z);
        Rotation rotation = Rotation.pointAt(new Point(0, 0, -1),
                to, new Point(0, 1, 0), new Point(up.x, up.y, up.z));
        setValueInTransaction(PROP_CAMERA_ROT, rotation);
    }

    /**
     * Set camera field of view (FOV) in degrees.
     * The field of view for the smaller screen dimension is specified (e.g. if
     * the screen is taller than it is wide, the horizontal FOV is specified).
     *
     * This parameter is only used by the PERSPECTIVE projection. In the 'UI'
     * projections the FOV is derived from the camera Z position and the screen
     * width (pixels) which are considered to be in the same coordinate space.
     *
     * @param fov Camera field of view in degrees
     */
    public void setCameraFov(float fov) {
        setValueInTransaction(PROP_CAMERA_FOV, fov);
    }

    /**
     * Set camera near clipping plane.
     *
     * @param near Camera near clipping plane
     */
    public void setCameraNear(float near) {
        setValueInTransaction(PROP_CAMERA_NEAR, near);
    }

    /**
     * Set camera width.
     * Sets the width of the viewing frustum in world-units when using an
     * ORTHOGRAPHIC projection.
     *
     * When using an PERSPECTIVE projection, this parameter has no visible
     * effect.
     *
     * @param width Width of viewing frustum
     */
    public void setCameraWidth(float width) {
        setValueInTransaction(PROP_CAMERA_WIDTH, width);
    }

    /**
     * Set camera far clipping plane.
     *
     * @param far Camera far clipping plane
     */
    public void setCameraFar(float far) {
        setValueInTransaction(PROP_CAMERA_FAR, far);
    }

    /**
     * Set projection mode
     *
     * @param mode Camera projection mode ORTHOGRAPHIC or PERSPECTIVE
     */
    public void setProjectionMode(int mode) {
        setValueInTransaction(PROP_PROJECTION_MODE, mode);
    }

    /**
     * Sets depth buffer clearing behaviour.
     * By default the depth buffer is cleared before each layer is rendered.
     * Call this method with false to prevent the z-buffer being cleared before
     * the layer is rendered.
     *
     * @param clear true if the depth buffer should be cleared
     */
    public void setDepthClear(boolean clear) {
        setValueInTransaction(PROP_CLEAR_DEPTH, clear);
    }

    /**
     * Use a camera from the scene
     * Use a camera created in a DCC tool and exported to a glo file. You should
     * pass in the name you gave the camera in 3ds Max or Blender. Pass an empty
     * string to revert to using the Layer's camera parameters.
     *
     * @param name Camera in scene to use
     */
    public void useNamedCamera(String name) {
        setValueInTransaction(PROP_NAMED_CAMERA, name);
    }

    /**
     * Use UI perspective projection
     * The camera will be positioned at the given (zPos) distance from the
     * X/Y plane and the field of view set such that the camera covers an
     * area of the plane so that x=0 corresponds to the left of the screen,
     * x=width corresponds to the right of the screen, y=0 corresponds to
     * the top of the screen and y=height corresponds to the bottom of the
     * screen.
     *
     * @param width     width of screen
     * @param height    height of screen
     * @param zPos      distance of camera from X/Y plane
     */
    public void setUiPerspective(float width, float height, float zPos) {
        float smallerDim = Math.min(width, height);
        float zDistance = Math.abs(zPos);

        float fov = (float) Math.toDegrees(
            Math.atan((smallerDim / 2) / zDistance) * 2);
        setCameraFov(fov);

        float centX = width / 2;
        float centY = height / 2;

        Point cameraPosition = new Point(centX, centY, zPos);

        setCameraPosition(cameraPosition);

        float zDir = (zPos < 0) ? 1 : -1;
        Rotation rotation = Rotation.pointAt(new Point(0, 0, -1),
                new Point(0, 0, zDir),
                new Point(0, 1, 0),
                new Point(0, -1, 0));
        setCameraRotation(rotation);

        // Set near and far planes such that the z=0 plane is half-way between
        // them and the ratio near/far = 1/100
        setCameraNear(2 * zDistance / 101);
        setCameraFar(200 * zDistance / 101);
    }

    /**
     * Used by hitTestFull()
     * Overridden by this class to provide a RenderLayer object (and perform
     * the hit test with the associated camera).
     *
     * @param screenPoint Point on the screen to test
     * @return Details about the hit test and its result
     */
    @Override
    protected PresentationHitTestResult doHitTest(Point screenPoint) {
        return mPresentation.hitTest(screenPoint, mRenderLayer);
    }

}
