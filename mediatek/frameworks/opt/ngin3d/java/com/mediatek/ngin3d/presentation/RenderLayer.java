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

package com.mediatek.ngin3d.presentation;

import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;


/**
 * Class used to render part of a scene using its own camera.
 */
public interface RenderLayer {

    /**
     * Sets position of camera.
     *
     * @param pos    camera position
     */
    void setCameraPosition(Point pos);

    /**
     * Sets rotation of camera.
     *
     * @param rot    camera rotation
     */
    void setCameraRotation(Rotation rot);

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
    void setCameraFov(float fov);

    /**
     * Sets the near and far clipping distances. Note these are distances
     * from the camera, in the forward direction of the camera axis; they
     * are NOT planes positioned along the global Z axis despite often called
     * Znear and Zfar.
     *
     * @param near objects nearer than this are clipped
     * @param far objects further away than this are clipped
     */
    void setClipDistances(float near, float far);

    /**
     * Set camera near clipping plane.
     *
     * @param near Camera near clipping plane
     */
    void setCameraNear(float near);

    /**
     * Set camera far clipping plane.
     *
     * @param far Camera far clipping plane
     */
    void setCameraFar(float far);

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
    void setCameraWidth(float width);

    /**
     * Set projection mode
     *
     * @param mode Camera projection mode ORTHOGRAPHIC or PERSPECTIVE
     */
    void setProjectionMode(int mode);

    /**
     * Sets the currently active camera.
     * Passing an empty string activates the default camera.
     *
     * @param name camera node name
     */
    void useNamedCamera(String name);

    /**
     * Sets z-buffer clearing behaviour.
     * Call this method with true will cause the z-buffer to be cleared before
     * the layer is rendered.
     *
     * @param clear true if the z buffer should be cleared
     */
    void setDepthClear(boolean clear);

    /**
     * Returns a list of names of cameras in the scene.
     *
     */
    String[] getGloCameraNames();
}
