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
/** \file
 * Layer Presentation for J3M
 */
package com.mediatek.ngin3d.j3m;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.ngin3d.presentation.RenderLayer;
import com.mediatek.ngin3d.presentation.Presentation;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Quaternion;

import com.mediatek.j3m.AngularUnits;
import com.mediatek.j3m.Camera;
import com.mediatek.j3m.RenderBlock;
import com.mediatek.j3m.Renderer;
import com.mediatek.j3m.SceneNode;

/**
 * j3m implementation of RenderLayer interface
 * @hide
 */

public class LayerPresentation implements RenderLayer {

    private static final String TAG = "LayerPresentation";

    private final RenderBlock mRenderBlock;
    private final Camera mCamera;;
    private final SceneNode mRootSceneNode;

    /**
     * Initializes this object with A3M presentation engine
     * @param engine    Presentation engine
     * @param renderer  Renderer to use
     * @param presentation  actor containing root node for this layer
     */
    public LayerPresentation(J3mPresentationEngine engine,
            Renderer renderer, Presentation presentation) {
        ActorPresentation actorPresentation =
                (ActorPresentation) presentation;
        mRootSceneNode = actorPresentation.getRootSceneNode();
        mRootSceneNode.setParent(engine.getRenderBlockParent());

        mCamera = engine.getJ3m().createCamera();
        mRenderBlock = engine.getJ3m().createRenderBlock(
            renderer, mRootSceneNode, mCamera);
        mRenderBlock.setColourClear(false);
        mRenderBlock.setDepthClear(true);
    }

    public RenderBlock getRenderBlock() {
        return mRenderBlock;
    }

    public SceneNode getRootNode() {
        return mRootSceneNode;
    }

    public Camera getCamera() {
        return mCamera;
    }

    /**
     * Sets position of camera.
     *
     * @param pos    camera position
     */
    public void setCameraPosition(Point pos) {
        mCamera.setPosition(pos.x, pos.y, pos.z);
    }

    /**
     * Sets rotation of camera.
     *
     * @param rot    camera rotation
     */
    public void setCameraRotation(Rotation rot) {
        Quaternion q = rot.getQuaternion();
        mCamera.setRotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3());
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
        mCamera.setFov(AngularUnits.DEGREES, fov);
    }

    /**
     * Sets the near and far clipping distances. Note these are distances
     * from the camera, in the forward direction of the camera axis; they
     * are NOT planes positioned along the global Z axis despite often called
     * Znear and Zfar.
     *
     * @param near objects nearer than this are clipped
     * @param far objects further away than this are clipped
     */
    public void setClipDistances(float near, float far) {
        mCamera.setNear(near);
        mCamera.setFar(far);
    }

    /**
     * Set camera near clipping plane.
     *
     * @param near Camera near clipping plane
     */
    public void setCameraNear(float near) {
        mCamera.setNear(near);
    }

    /**
     * Set camera far clipping plane.
     *
     * @param far Camera far clipping plane
     */
    public void setCameraFar(float far) {
        mCamera.setFar(far);
    }

    /**
     * Set default projection mode
     */
    public void setProjectionMode(int mode) {
        mCamera.setProjectionType(mode);
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
        mCamera.setWidth(width);
    }

    /**
     * Sets the currently active camera.
     * Passing an empty string activates the default camera.
     *
     */
    public void useNamedCamera(String name) {
        if (name.isEmpty()) {
            mRenderBlock.setCamera(mCamera);
        } else {
            SceneNode node = mRootSceneNode.find(name);

            if (Camera.class.isInstance(node)) {
                mRenderBlock.setCamera((Camera)node);
            }
        }
    }

    /**
     * Sets z-buffer clearing behaviour.
     * Call this method with true will cause the z-buffer to be cleared before
     * the layer is rendered.
     *
     * @param clear true if the z buffer should be cleared
     */
    public void setDepthClear(boolean clear) {
        mRenderBlock.setDepthClear(clear);
    }

    /**
     * Returns a list of names of cameras in the scene.
     *
     */
    public String[] getGloCameraNames() {
        List<String> names = new ArrayList<String>();
        compileGloCameraNames(names, mRootSceneNode);
        String[] namesArray = new String[names.size()];
        names.toArray(namesArray);
        return namesArray;
    }

    /**
     * Traverses the scene graph and compiles a list of all the cameras.
     *
     */
    private void compileGloCameraNames(List<String> names, SceneNode node) {
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            SceneNode child = node.getChild(i);

            String name = child.getName();
            if ((!name.isEmpty()) && Camera.class.isInstance(child)) {
                names.add(name);
            }

            compileGloCameraNames(names, child);
        }
    }


}
