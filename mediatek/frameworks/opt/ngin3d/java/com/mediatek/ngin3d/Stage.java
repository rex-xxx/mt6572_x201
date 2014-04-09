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

import java.util.List;
import java.util.ArrayList;

import android.content.res.Resources;
import android.util.Log;

import com.mediatek.ngin3d.presentation.PresentationEngine;
import com.mediatek.ngin3d.utils.Ngin3dException;
import com.mediatek.util.JSON;

/**

Stage is a special Container that displays its Actors on the screen.

 * \ingroup ngin3dClasses
 */
public class Stage extends Container {

    private PresentationEngine mPresentationEngine;
    private final UiHandler mUiHandler;
    protected List<Layer> mLayers = new ArrayList<Layer>();
    private static final ThreadLocal<Stage> THREAD_LOCAL_STAGE = new ThreadLocal<Stage>();

    /*
     * Projection modes
     */
    /** Orthographic projection mode. */
    public static final int ORTHOGRAPHIC = 0;
    /** Perspective projection mode. */
    public static final int PERSPECTIVE = 1;
    /** Perspective projection mode with fixed parameters to simplify UI applications. */
    public static final int UI_PERSPECTIVE = 2;
    /** Legacy projection mode
     * @deprecated
     * This is equivilent to UI_PERSPECTIVE, which should be used instead. */
    public static final int UI_PERSPECTIVE_LHC = 2;

    private static final int PM_MAX_LEGAL = 3; // highest legal value

    /**
     * It is important to name attached property as ATTACHED_PROP_*. If its name begins with
     * typical PROP_*, it will be treated as class-owned property and will not be dispatched
     * to property chain.
     */
    private static final Property<Stage> ATTACHED_PROP_ADD_LAYER
        = new Property<Stage>("layer", null);

    /**
     * Construct an empty stage [DEPRECATED].
     *
     * @deprecated should use Stage(UiHandler) instead.
     */
    @Deprecated
    public Stage() {
        // A dummy UI handle that run the specified runnable directly.
        this(new UiHandler() {
            public void post(Runnable runnable) {
                runnable.run();
            }
        });
    }

    /**
     * Construct an empty stage with specified UI handler.
     *
     * @param uiHandler a handler to run specified runnable in UI thread
     */
    public Stage(UiHandler uiHandler) {
        super();
        mUiHandler = uiHandler;
    }

    ///////////////////////////////////////////////////////////////////////////
    // public methods

    /*! \cond
     * Hide from Doxygen.
     */

    /**
     * Called to apply property changes to presentation engine for each frame rendering.
     *
     * @param presentationEngine presentation engine
     * @hide
     */
    public void applyChanges(PresentationEngine presentationEngine) {
        super.realize(presentationEngine);
        // Realize all layers belonging to the stage
        int size = mLayers.size();
        // Use indexing rather than iterator to prevent frequent GC
        for (int i = 0; i < size; ++i) {
            final Layer layer = mLayers.get(i);
            layer.realize(presentationEngine);
        }
    }

    /**
     * PresentationEngine will call this to initialize Stage.
     *
     * @param presentationEngine presentation engine
     * @hide
     */
    public void realize(PresentationEngine presentationEngine) {
        mPresentationEngine = presentationEngine;
        // Sometimes We need tasks to run on UI thread when current thread is GL Thread.
        // So we have to access UI handler when the executing Thread is GL.
        // This stage get the UI handler when constructing, and this(realize) method is invoked in GL thread, so we store
        // the instance of this stage into ThreadLocal here.
        registerCurrentThread();

        super.realize(presentationEngine);

        // Realize all layers belonging to the stage
        int size = mLayers.size();
        for (int i = 0; i < size; ++i) {
            final Layer layer = mLayers.get(i);
            layer.realize(presentationEngine);
        }

        reloadBitmapTexture(presentationEngine);
    }

    /**
     * Invoke the method to register caller thread to have this stage.
     * In order to make Animation callback run on UI thread, you have to register the thread which starts animation
     * @hide
     */
    public void registerCurrentThread() {
        // Remember this stage in TLS so that rendering thread can get it later.
        THREAD_LOCAL_STAGE.set(this);
    }

    /**
     * For rendering thread to get current UI handler.
     *
     * @return UI handler
     * @hide
     */
    public static UiHandler getUiHandler() {
        Stage stage = THREAD_LOCAL_STAGE.get();
        if (stage == null) {
            return null;
        } else {
            return stage.mUiHandler;
        }

    }

    /*! \endcond */

    class PropertyChainAddLayer implements Base.PropertyChain {
        public boolean applyAttachedProperty(Base obj, Property property, Object value) {
            if (property.sameInstance(ATTACHED_PROP_ADD_LAYER)) {
                if (value != Stage.this) {
                    throw new IllegalArgumentException("Unmatched child-parent!");
                }
                if (mPresentation == null) {
                    return false;
                }

                Layer layer = (Layer) obj;
                mPresentationEngine.addRenderLayer(layer.getRenderLayer());
                return true;
            }

            return false;
        }

        public Object getInheritedProperty(Property property) {
            return null;
        }
    }

    /**
     * Add the actors into this container.
     *
     * @param actors actors to add as children
     */
    @Override
    public void add(Actor... actors) {
        for (Actor child : actors) {
            if (child instanceof Layer) {
                addLayer((Layer) child);
            } else {
                super.add(child);
            }
        }
    }

    private void addLayer(Layer layer) {
        synchronized (this) {
            if (mLayers.contains(layer)) {
                Log.w(TAG, "The layer is already in the stage");
            } else {
                mLayers.add(layer);
                // Attach PARENT property to the child
                layer.setValue(ATTACHED_PROP_ADD_LAYER, this);

                // Listen for unhandled property so that we can apply PARENT
                layer.setPropertyChain(new PropertyChainAddLayer());
                requestRender();
            }
        }
    }

    /**
     * Gets the stage size of width.
     *
     * @return stage width
     */
    public int getWidth() {
        return mPresentationEngine.getWidth();
    }

    /**
     * Gets the stage size of height.
     *
     * @return stage height
     */
    public int getHeight() {
        return mPresentationEngine.getHeight();
    }

    /**
     * Get time the last frame cost.
     *
     * @return the time of frame interval
     */
    public int getFrameInterval() {
        if (mPresentationEngine == null) {
            return 0;
        }
        return mPresentationEngine.getFrameInterval();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Projections

    /**
     * Projection configuration.
     * (Public only for the purpose of automated testing)
     */
    public static class ProjectionConfig implements JSON.ToJson {
        public int mode;
        public float zNear;
        public float zFar;
        public float zStage;

        public ProjectionConfig(int mode, float zNear, float zFar, float zStage) {
            this.mode = mode;
            this.zNear = zNear;
            this.zFar = zFar;
            this.zStage = zStage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ProjectionConfig that = (ProjectionConfig) o;

            if (that.mode != mode) return false;
            if (Float.compare(that.zFar, zFar) != 0) return false;
            if (Float.compare(that.zNear, zNear) != 0) return false;
            if (Float.compare(that.zStage, zStage) != 0) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = mode;
            result = 31 * result + (zFar == +0.0f ? 0 : Float.floatToIntBits(zFar));
            result = 31 * result + (zNear == +0.0f ? 0 : Float.floatToIntBits(zNear));
            result = 31 * result + (zStage == +0.0f ? 0 : Float.floatToIntBits(zStage));
            return result;
        }

        @Override
        public String toString() {
            return "{Proj mode : " + mode + ", zNear : " + zNear
                + ", zFar : " + zFar + ", zStage : " + zStage + "}";
        }

        public String toJson() {
            return "{Proj mode : " + mode + ", zNear : " + zNear
                + ", zFar : " + zFar + ", zStage : " + zStage + "}";
        }

    }

    /**
     * The camera that is used to display the stage actors
     */
    public static class Camera implements JSON.ToJson {
        public Point position;
        public Point lookAt;

        public Camera(Point position, Point lookAt) {
            this.position = new Point(position);
            this.lookAt = new Point(lookAt);
        }

        @Override
        public String toString() {
            return "position : " + position + ", lookAt : " + lookAt;
        }

        public String toJson() {
            return "{position : " + position.toJson() + ", lookAt : " + lookAt.toJson() + "}";
        }
    }

    /**
     * Configuration for Stereo 3D.
     */
    public static class Stereo3D implements JSON.ToJson {
        public boolean enable;
        public float eyesDistance;
        public float intensity;

        public Stereo3D(boolean enable, float eyesDistance, float intensity) {
            this.enable = enable;
            this.eyesDistance = eyesDistance;
            this.intensity = intensity;
        }

        @Override
        public String toString() {
            return "enable : " + enable + ", eyesDistance : " + eyesDistance + ", intensity : " + intensity;
        }

        public String toJson() {
            return "{enable : " + enable + ", eyesDistance : " + eyesDistance + ", intensity : " + intensity + "}";
        }
    }


    /*! \cond
     * Hide from Doxygen.
     */

    ///////////////////////////////////////////////////////////////////////////
    // Property handling

    /**
     * @hide
     */
    static final Property<Object> PROP_PROJECTION = new Property<Object>(
            "projection", null);
    /**
     * @hide
     */
    static final Property<String> PROP_DEBUG_CAMERA = new Property<String>(
            "active_camera", null, PROP_PROJECTION);
    /**
     * @hide
     */
    static final Property<Camera> PROP_CAMERA = new Property<Camera>(
            "camera", null, PROP_PROJECTION);
    /**
     * @hide
     */
    static final Property<Float> PROP_CAMERA_FOV = new Property<Float>(
            "camera_fov", null, PROP_PROJECTION);
    /**
     * @hide
     */
    static final Property<Color> PROP_BACKGROUND_COLOR = new Property<Color>(
            "background_color", Color.BLACK);
    /**
     * @hide
     */
    static final Property<Float> PROP_FOG_DENSITY = new Property<Float>(
            "fog_density", 0.0f);
    /**
     * @hide
     */
    static final Property<Color> PROP_FOG_COLOR = new Property<Color>(
            "fog_color", Color.BLACK);
    /**
     * @hide
     */
    static final Property<Integer> PROP_MAX_FPS = new Property<Integer>(
            "max_fps", 0);
    /**
     * @hide
     */
    static final Property<Stereo3D> PROP_STEREO3D = new Property<Stereo3D>(
            "stereo3d", null);

    /**
     * @hide
     */
    @Override
    protected boolean applyValue(Property property, Object value) {
        if (super.applyValue(property, value)) {
            return true;
        }

        if (property.sameInstance(PROP_DEBUG_CAMERA)) {
            if (value != null) {
                String name = (String) value;
                mPresentationEngine.setDebugCamera(name);
            }
            return true;
        } else if (property.sameInstance(PROP_CAMERA)) {
            if (value != null) {
                Camera camera = (Camera) value;
                mPresentationEngine.setCamera(camera.position, camera.lookAt);
            }
            return true;
        } else if (property.sameInstance(PROP_CAMERA_FOV)) {
            if (value != null) {
                Float fov = (Float) value;
                mPresentationEngine.setCameraFov(fov);
            }
            return true;
        } else if (property.sameInstance(PROP_PROJECTION)) {
            if (value != null) {
                ProjectionConfig p = (ProjectionConfig) value;
                mPresentationEngine.setClipDistances(p.zNear, p.zFar);
                if (p.mode == UI_PERSPECTIVE) {
                    mPresentationEngine.setCameraZ(p.zStage);
                }
                mPresentationEngine.setProjectionMode(p.mode);
            }
            return true;
        } else if (property.sameInstance(PROP_BACKGROUND_COLOR)) {
            // Not necessary to apply cause PresentationEngine should read the background color automatically.
            return true;
        } else if (property.sameInstance(PROP_FOG_DENSITY)) {
            mPresentationEngine.setFogDensity((Float) value);
            return true;
        } else if (property.sameInstance(PROP_FOG_COLOR)) {
            mPresentationEngine.setFogColor((Color) value);
            return true;
        } else if (property.sameInstance(PROP_MAX_FPS)) {
            Integer fps = (Integer) value;
            mPresentationEngine.setMaxFPS(fps);
            return true;
        } else if (property.sameInstance(PROP_STEREO3D)) {
            if (value != null) {
                Stereo3D stereo3D = (Stereo3D) value;
                mPresentationEngine.enableStereoscopic3D(stereo3D.enable, stereo3D.eyesDistance, stereo3D.intensity);
            }
            return true;
        }

        return false;
    }
    /*! \endcond */


    /**
     * Configures the type of projection in use.
     *
     * Orthogonal and Perspective are as classical graphics rendering.
     * http://en.wikipedia.org/wiki/3D_projection
     *
     * UI-perspective is a UI-application projection where the camera position
     * and orientation is fixed, mid screen and pointing down the Z axis at the
     * XY plane where the UI objects are located. Equates to watching action
     * on a theatre stage.
     *
     * Use of setCamera() is only legal if the mode is PERSPECTIVE.  For the
     * other modes the position of the camera is either irrelevant (ORTHO) or
     * fixed (UI).
     *
     * @param projectionMode ORTHOGRAPHIC, PERSPECTIVE or UI_PERSPECTIVE
     * @param zNear distance from camera to near clipping plane
     * @param zFar distance from camera to far clipping plane
     * @param zStage Z position of camera for STAGE mode
     */
    public void setProjection(int projectionMode, float zNear, float zFar, float zStage) {
        if (projectionMode > PM_MAX_LEGAL || projectionMode < 0) {
            throw new Ngin3dException("Illegal projection mode " + projectionMode);
        } else {
            setValue(PROP_PROJECTION,
                new ProjectionConfig(projectionMode, zNear, zFar, zStage));
        }
    }

    /**
     * Query the current projection mode.
     * @return Projection mode
     */
    public Object getProjection() {
        return getValue(PROP_PROJECTION);
    }

    /**
     * Sets the currently active debug camera [DEPRECATED].
     * Passing an empty string activates the default camera.
     *
     * @deprecated This function is marked for removal in the near future.
     *
     * @param name Camera name (an empty string activates the default camera)
     */
    public void setDebugCamera(String name) {
        if (mPresentationEngine != null) {
            mPresentationEngine.setDebugCamera(name);
        }
    }

    /**
     * Returns a list of names of cameras in the scene [DEPRECATED].
     *
     * @deprecated This function is marked for removal in the near future.
     */
    public String[] getDebugCameraNames() {
        if (mPresentationEngine == null) {
            return null;
        } else {
            return mPresentationEngine.getDebugCameraNames();
        }
    }

    /**
     * Sets a virtual camera to see the stage for presentation.
     *
     * @param position Camera position
     * @param lookAt   Camera focus point
     */
    public void setCamera(Point position, Point lookAt) {
        setValueInTransaction(PROP_CAMERA, new Camera(position, lookAt));
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
     * Gets the virtual camera property of this object.
     *
     * @return Camera property
     */
    public Camera getCamera() {
        return getValue(PROP_CAMERA);
    }

    /**
     * Sets the background color of this stage object.
     *
     * @param bkgColor background color
     */
    public void setBackgroundColor(Color bkgColor) {
        setValue(PROP_BACKGROUND_COLOR, bkgColor);
    }

    /**
     * Gets the background color of this stage object.
     *
     * @return background color property
     */
    public Color getBackgroundColor() {
        return getValue(PROP_BACKGROUND_COLOR);
    }

    /**
     * Sets the global fog density.
     * When fog density is greater than zero (zero is the default), the scene
     * will gradually fade towards the fog color as it gets further from the
     * camera.  The higher the fog density, the faster the fog color will fade
     * in with distance.
     *
     * @param density Fog density
     */
    public void setFogDensity(float density) {
        setValue(PROP_FOG_DENSITY, density);
    }

    /**
     * Sets the global fog color.
     *
     * @param color Fog color
     */
    public void setFogColor(Color color) {
        setValue(PROP_FOG_COLOR, color);
    }

    /**
     * Sets a Stereo3D configuration.
     *
     * @param enable enable stereoscopic 3d effect.
     * @param focalDistance the distance between the camera and the object in
     *        the world space you would like to focus on.
     */
    public void setStereo3D(boolean enable, float focalDistance) {
        setStereo3D(enable, focalDistance, 1);
    }

    /**
     * Sets a Stereo3D configuration.
     *
     * @param enable enable stereoscopic 3d effect.
     * @param focalDistance the distance between the camera and the object in
     *        the world space you would like to focus on.
     * @param intensity Adjust the level of stereo separation. Normally 1.0,
     *                  1.1 increases the effect by 10%, for example.
     */
    public void setStereo3D(boolean enable, float focalDistance, float intensity) {
        setValue(PROP_STEREO3D, new Stereo3D(enable, focalDistance, intensity));
    }

    /**
     * Gets the Stereo3D configuration.
     *
     * @return the Stereo3D object
     */
    public Stereo3D getStereo3D() {
        return getValue(PROP_STEREO3D);
    }

    /**
     * Add a TextureAtlas into this stage using android resource information and JSON file.
     *
     * @param res      android resource
     * @param imageId  android resource id
     * @param scriptId JSON file id
     */
    public void addTextureAtlas(Resources res, int imageId, int scriptId) {
        TextureAtlas.getDefault().add(res, imageId, scriptId);
    }

    /**
     * Add a TextureAtlas into this stage using android asset information and JSON file.
     *
     * @param res      android resource
     * @param asset  android asset name
     * @param scriptId JSON file id
     */
    public void addTextureAtlas(Resources res, String asset, int scriptId) {
        TextureAtlas.getDefault().add(res, asset, scriptId);
    }

    /**
     * Set the maximum Frames-per-second rate.
     * @param fps Max FPS setting
     */
    public void setMaxFPS(int fps) {
        setValue(PROP_MAX_FPS, fps);
    }

    /**
     * Query the current 'Max FPS' setting.
     * @return Current Max FPS setting.
     */
    public int getMaxFPS() {
        return getValue(PROP_MAX_FPS);
    }

    /**
     * Check whether the container and all of its children are dirty or not.
     */
    public boolean isDirty() {
        synchronized (this) {
            if (super.isDirty()) {
                return true;
            }
            int size = mLayers.size();
            for (int i = 0; i < size; ++i) {
                if (mLayers.get(i).isDirty()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Check whether the animations that are applied to container are running.
     */
    public boolean isAnimationStarted() {
        synchronized (this) {
            if (super.isAnimationStarted()) {
                return true;
            }
            int size = mLayers.size();
            for (int i = 0; i < size; ++i) {
                if (mLayers.get(i).isAnimationStarted()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Touch property and make it dirty.
     * @param propertyName  selected property name
     * @hide
     */
    public void touchProperty(String propertyName) {
        super.touchProperty(propertyName);
        int size = mLayers.size();
        for (int i = 0; i < size; ++i) {
            mLayers.get(i).touchProperty(propertyName);
        }

    }

    /** dump function */
    public String dump() {
        String dump = super.dump();
        dump = JSON.wrap(dump);
        Log.d(TAG, dump);
        return dump;
    }

}
