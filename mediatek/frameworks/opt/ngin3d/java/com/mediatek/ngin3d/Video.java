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
 * MediaTek Inc. (C) 2012. All rights reserved.
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

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.mediatek.ngin3d.presentation.ImageSource;
import com.mediatek.ngin3d.utils.Ngin3dException;

/**
 * A special actor that extend from Plane can show video steaming data.
 */
public final class Video extends Plane {

    static final Property<ImageSource> PROP_VIDEO_SRC = new Property<ImageSource>("video_source", null);
    static final Property<Boolean> PROP_PLAYING = new Property<Boolean>("playing", false, PROP_VIDEO_SRC);

    private Video(boolean isYUp) {
        super(isYUp);
    }

    /**
     * Apply the video information data
     *
     * @param property property type to be applied
     * @param value    property value to be applied
     * @return if the property is successfully applied
     * @hide
     */
    protected boolean applyValue(Property property, Object value) {
        if (super.applyValue(property, value)) {
            return true;
        }

        if (property.sameInstance(PROP_VIDEO_SRC)) {
            ImageSource src = (ImageSource) value;
            if (src == null) {
                return false;
            }
            getPresentation().setImageSource(src);
            initializeVideoPlayer();
            return true;
        }
        else if (property.sameInstance(PROP_PLAYING)) {
            VideoPlayer vp = getVideoPlayer();
            updateStreamingTexture();
            if ((Boolean) value) {
                // Keep PROP_PLAY dirty since the video is playing
                touchProperty(PROP_PLAYING);
                if (!vp.isPlaying()) {
                    vp.start();
                }
            } else {
                vp.pause();
            }
            return true;
        }
        return false;
    }

    /**
     * Create an Video object with video streaming content from uri.
     *
     * Video streaming is paused by default after prepared and shows first frame.
     * Client must call @play() specifically to start video playback.
     *
     * @param ctx   android context
     * @param uri       video uri
     * @param width       video object width
     * @param height       video object height
     * @return an Video object that is created from video uri
     */
    public static Video createFromVideo(Context ctx, Uri uri, int width, int height) {
        return createFromVideo(ctx, uri, width, height, false);
    }

    /**
     * Create an Video object with video streaming content from uri.
     *
     * Video streaming is paused by default after prepared and shows first frame.
     * Client must call @play() specifically to start video playback.
     *
     * @param ctx   android context
     * @param uri       video uri
     * @param width       video object width
     * @param height       video object height
     * @param isYUp   true for creating a Y-up quad, default is Y-down
     * @return an Video object that is created from video uri
     */
    public static Video createFromVideo(Context ctx, Uri uri, int width, int height, boolean isYUp) {
        Video video = new Video(isYUp);
        // Essential to set material here to get the shader uniform in place
        video.setMaterial("ngin3d#vidquad.mat");
        video.setVideoFromUri(ctx, uri, width, height);
        return video;
    }

    protected void setVideoFromUri(Context ctx, Uri uri, int width, int height) {
        Dimension dim = new Dimension(width, height);
        setValue(PROP_SIZE, dim);
        setValue(PROP_VIDEO_SRC, new ImageSource(ImageSource.VIDEO_TEXTURE, new VideoPlayer(ctx, uri)));
        setValue(PROP_VISIBLE, false); // default is invisible until the content is ready
    }

    protected void updateStreamingTexture() {
        ImageSource src = getValue(PROP_VIDEO_SRC);
        boolean isFirstUpdate = ((VideoPlayer) src.srcInfo).applyUpdate();
        if (isFirstUpdate && !getVisible()) {
            setVisible(true);
        }
    }

    private class VideoPlayerInitializer implements Runnable {
        private final VideoPlayer mVideoPlayer;

        VideoPlayerInitializer(VideoPlayer src) {
            mVideoPlayer = src;
        }

        public void run() {

            // After texture object name has been got from the engine,
            // generate surface texture, setup video player, and start to play.
            // Because SurfaceTexture can be created under any thread.
            // To accelerate display in GL thread, move initialization at UI thread.

            // This runnable is posted to ui thread, the ui thread might execute the
            // runnable while activity is in background and in this case the
            // presentation is null.
            if (mVideoPlayer != null && getPresentation() != null) {
                int textureName = getPresentation().getTexName();
                if (textureName > 0) {
                    mVideoPlayer.initialize(textureName);
                } else {
                    Log.w(TAG, "Video Texture texture name is invalid : " + textureName);
                }
            }
        }
    }

    public VideoPlayer getVideoPlayer() {
        ImageSource src = getValue(PROP_VIDEO_SRC);
        if (src != null) {
            return (VideoPlayer) src.srcInfo;
        }
        return null;
    }

    /**
     * Un-realize this Video.
     * @hide
     */
    public void unrealize() {
        super.unrealize();
        VideoPlayer vt = getVideoPlayer();
        if (vt != null) {
            vt.destory();
        }
    }

    public void initializeVideoPlayer() {
        UiHandler uiHandler = Stage.getUiHandler();
        if (uiHandler == null) {
            throw new Ngin3dException("No UI thread handler found, which is necessary for video texture display.");
        }

        VideoPlayer vp = getVideoPlayer();
        if (vp != null && !vp.isInitialized()) {
            uiHandler.post(new VideoPlayerInitializer(vp));
        }
    }

    /**
     * Start video texture playback
     */
    public void play() {
        setValue(PROP_PLAYING, true);
    }

    /**
     * Pause video texture playback
     */
    public void pause() {
        setValue(PROP_PLAYING, false);
    }

    /**
     * Configure to play video once or looping
     *
     * @param looping true for replay video automatically
     * @return an Video object
     */
    public Video setLooping(Boolean looping) {
        getVideoPlayer().setLooping(looping);
        return this;
    }

    /**
     * Sets the volume on this player.
     *
     * @param leftVolume Sets the left volume on this player.
     * @param rightVolume Sets the right     volume on this player.
     * @return an Video object
     */
    public Video setVolume(float leftVolume, float rightVolume) {
        getVideoPlayer().setVolume(leftVolume, rightVolume);
        return this;
    }

    /**
     * Return the value is dirty.
     * @return true if the dirty value is set
     */
    @Override
    public boolean isDirty() {
        VideoPlayer vp = getVideoPlayer();
        return super.isDirty() || vp.isPlaying();
    }

    /**
     * The video is playing or not
     * @return true if the the video is playing
     */
    public boolean isPlaying() {
        return getVideoPlayer().isPlaying();
    }

}
