/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.service.api.client.media.video;

import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRtpSender;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.H263Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.encoder.NativeH263Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.encoder.NativeH263EncoderParams;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264EncoderParams;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H263VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

import android.hardware.Camera;
import android.os.RemoteException;
import android.os.SystemClock;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Live RTP video player. Supports only H.263 and H264 QCIF formats.
 */
public class LiveVideoPlayer extends IMediaPlayer.Stub implements Camera.PreviewCallback {

    /**
     * List of supported video codecs
     */
    public static MediaCodec[] supportedMediaCodecs = {
            new VideoCodec(H264Config.CODEC_NAME, H264VideoFormat.PAYLOAD, H264Config.CLOCK_RATE, H264Config.CODEC_PARAMS,
                    H264Config.FRAME_RATE, H264Config.BIT_RATE, H264Config.VIDEO_WIDTH,
                    H264Config.VIDEO_HEIGHT).getMediaCodec(),
            new VideoCodec(H263Config.CODEC_NAME, H263VideoFormat.PAYLOAD, H263Config.CLOCK_RATE, H263Config.CODEC_PARAMS,
                    H263Config.FRAME_RATE, H263Config.BIT_RATE, H263Config.VIDEO_WIDTH,
                    H263Config.VIDEO_HEIGHT).getMediaCodec()
    };

    /**
     * Selected video codec
     */
    private VideoCodec selectedVideoCodec = null;

    /**
     * Video format
     */
    private VideoFormat videoFormat;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP sender session
     */
    private MediaRtpSender rtpSender = null;

    /**
     * RTP media input
     */
    private MediaRtpInput rtpInput = null;

    /**
     * Last video frame
     */
    private CameraBuffer frameBuffer = null;

    /**
     * Is player opened
     */
    private boolean opened = false;

    /**
     * Is player started
     */
    private boolean started = false;

    /**
     * Video start time
     */
    private long videoStartTime = 0L;

    /**
     * Media event listeners
     */
    private Vector<IMediaEventListener> listeners = new Vector<IMediaEventListener>();

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public LiveVideoPlayer() {
        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);
    }

    /**
     * Constructor. Force a video codec.
     *
     * @param codec Video codec
     */
    public LiveVideoPlayer(VideoCodec codec) {
        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the media codec
        setMediaCodec(codec.getMediaCodec());
    }

    /**
     * Constructor. Force a video codec.
     *
     * @param codec Video codec name
     */
    public LiveVideoPlayer(String codec) {
        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the media codec
        for (int i = 0; i < supportedMediaCodecs.length ; i++) {
            if (codec.toLowerCase().contains(supportedMediaCodecs[i].getCodecName().toLowerCase())) {
                setMediaCodec(supportedMediaCodecs[i]);
                break;
            }
        }
    }

    /** M: add for video sharing plugin @{ */
    public void addVideoSurfaceViewListener(IVideoSurfaceViewListener videoSurfaceViewListener) {
        // not used
    }

    /** @} */

    /**
     * Returns the local RTP port
     *
     * @return Port
     */
    public int getLocalRtpPort() {
        return localRtpPort;
    }

    /**
     * Reserve a port.
     *
     * @param port the port to reserve
     */
    private void reservePort(int port) {
        if (temporaryConnection == null) {
            try {
                temporaryConnection = NetworkFactory.getFactory().createDatagramConnection();
                temporaryConnection.open(port);
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Release the reserved port.
     */
    private void releasePort() {
        if (temporaryConnection != null) {
            try {
                temporaryConnection.close();
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Return the video start time
     *
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return videoStartTime;
    }

    /**
     * Is player opened
     *
     * @return Boolean
     */
    public boolean isOpened() {
        return opened;
    }

    /**
     * Is player started
     *
     * @return Boolean
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Open the player
     *
     * @param remoteHost Remote host
     * @param remotePort Remote port
     */
    public void open(String remoteHost, int remotePort) {
        if (opened) {
            // Already opened
            return;
        }

        // Check video codec
        if (selectedVideoCodec == null) {
            notifyPlayerEventError("Video Codec not selected");
            return;
        }

        // Init video encoder
        try {
            if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                // H264
                NativeH264EncoderParams nativeH264EncoderParams = new NativeH264EncoderParams();
                nativeH264EncoderParams.setFrameWidth(selectedVideoCodec.getWidth());
                nativeH264EncoderParams.setFrameHeight(selectedVideoCodec.getHeight());
                nativeH264EncoderParams.setFrameRate(selectedVideoCodec.getFramerate());
                nativeH264EncoderParams.setBitRate(selectedVideoCodec.getBitrate());
    
                // Codec profile level
                nativeH264EncoderParams.setProfile(NativeH264EncoderParams.PROFILE_BASELINE);
                nativeH264EncoderParams.setLevel(NativeH264EncoderParams.LEVEL_1B);
    
                // Codec settings optimization
                nativeH264EncoderParams.setBitRate(96000);
                nativeH264EncoderParams.setFrameRate(10);
                nativeH264EncoderParams.setEncMode(NativeH264EncoderParams.ENCODING_MODE_STREAMING);
                nativeH264EncoderParams.setPacketSize(JavaPacketizer.H264_MAX_FRAME_SIZE);
                nativeH264EncoderParams.setSceneDetection(true);
                nativeH264EncoderParams.setIFrameInterval(10);
                nativeH264EncoderParams.setFrameOrientation(0);
    
                int result = NativeH264Encoder.InitEncoder(nativeH264EncoderParams);
                if (result != 0) {
                   notifyPlayerEventError("Encoder init failed with error code " + result);
                   return;
                }
            } else if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H263Config.CODEC_NAME)) {
                // Default H263
                NativeH263EncoderParams params = new NativeH263EncoderParams();
                params.setEncFrameRate(selectedVideoCodec.getFramerate());
                params.setBitRate(selectedVideoCodec.getBitrate());
                params.setTickPerSrc(params.getTimeIncRes() / selectedVideoCodec.getFramerate());
                params.setIntraPeriod(-1);
                params.setNoFrameSkipped(false);
                int result = NativeH263Encoder.InitEncoder(params);
                if (result != 1) {
                    notifyPlayerEventError("Encoder init failed with error code " + result);
                    return;
                }
            }
        } catch (UnsatisfiedLinkError e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        // Init the RTP layer
        try {
            releasePort();
            rtpSender = new MediaRtpSender(videoFormat, localRtpPort);
            rtpInput = new MediaRtpInput();
            rtpInput.open();
            rtpSender.prepareSession(rtpInput, remoteHost, remotePort);
        } catch (Exception e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        // Player is opened
        opened = true;
        notifyPlayerEventOpened();
    }

    /**
     * M: fix NE for video share, which is caused by NOT synchronization between
     * EncodeFrame and DeinitEncoder @{
     */
    /**
     * Close the player
     */
    public void close() {
        if (!opened) {
            // Already closed
            return;
        }
        // Close the RTP layer
        rtpInput.close();
        rtpSender.stopSession();

        // Player is closed
        opened = false;
        notifyPlayerEventClosed();
    }

    private void closeEncoder() {
        try {
            // Close the video encoder
            if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                NativeH264Encoder.DeinitEncoder();
            } else if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H263Config.CODEC_NAME)) {
                NativeH263Encoder.DeinitEncoder();
            }
        } catch (UnsatisfiedLinkError e) {
            if (logger.isActivated()) {
                logger.error("Can't close correctly the video encoder", e);
            }
        }
    }

    /** @} */

    /**
     * Start the player
     */
    public synchronized void start() {
        if (!opened) {
            // Player not opened
            return;
        }

        if (started) {
            // Already started
            return;
        }

        // Start RTP layer
        rtpSender.startSession();

        // Start capture
        captureThread.start();

        // Player is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
        notifyPlayerEventStarted();
    }

    /**
     * Stop the player
     */
    public void stop() {
        if (!opened) {
            // Player not opened
            return;
        }

        if (!started) {
            // Already stopped
            return;
        }

        // Stop capture
        try {
            captureThread.interrupt();
        } catch (Exception e) {
        }

        // Player is stopped
        videoStartTime = 0L;
        started = false;
        notifyPlayerEventStopped();
    }

    /**
     * Add a media event listener
     *
     * @param listener Media event listener
     */
    public void addListener(IMediaEventListener listener) {
        listeners.addElement(listener);
    }

    /**
     * Remove all media event listeners
     */
    public void removeAllListeners() {
        listeners.removeAllElements();
    }

    /**
     * Get supported media codecs
     *
     * @return media Codecs list
     */
    public MediaCodec[] getSupportedMediaCodecs() {
        return supportedMediaCodecs;
    }

    /**
     * Get media codec
     *
     * @return Media Codec
     */
    public MediaCodec getMediaCodec() {
        if (selectedVideoCodec == null)
            return null;
        else
            return selectedVideoCodec.getMediaCodec();
    }

    /**
     * Set media codec
     *
     * @param mediaCodec Media codec
     */
    public void setMediaCodec(MediaCodec mediaCodec) {
        if (VideoCodec.checkVideoCodec(supportedMediaCodecs, new VideoCodec(mediaCodec))) {
            selectedVideoCodec = new VideoCodec(mediaCodec);
            videoFormat = (VideoFormat) MediaRegistry.generateFormat(mediaCodec.getCodecName());

            // Initialize frame buffer
            if (frameBuffer == null) {
                frameBuffer = new CameraBuffer();
            }
        } else {
            notifyPlayerEventError("Codec not supported");
        }
    }

    /**
     * Notify player event started
     */
    private void notifyPlayerEventStarted() {
        if (logger.isActivated()) {
            logger.debug("Player is started");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaStarted();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event stopped
     */
    private void notifyPlayerEventStopped() {
        if (logger.isActivated()) {
            logger.debug("Player is stopped");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaStopped();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event opened
     */
    private void notifyPlayerEventOpened() {
        if (logger.isActivated()) {
            logger.debug("Player is opened");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaOpened();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event closed
     */
    private void notifyPlayerEventClosed() {
        if (logger.isActivated()) {
            logger.debug("Player is closed");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaClosed();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event error
     */
    private void notifyPlayerEventError(String error) {
        if (logger.isActivated()) {
            logger.debug("Player error: " + error);
        }

        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaError(error);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Preview frame from the camera
     *
     * @param data Frame
     * @param camera Camera
     */
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (frameBuffer != null)
            frameBuffer.setFrame(data);
    }

    /**
     * M: Added to match the plug-in mechanism while implemented the video
     * share. @{
     */
    /**
     * Preview frame from the camera
     * 
     * @param data Frame
     */
    public void onPreviewFrame(byte[] data) {
        if (frameBuffer != null) {
            frameBuffer.setFrame(data);
        } else {
            if (logger.isActivated()) {
                logger.debug("onPreviewFrame frameBuffer is null");
            }
        }
    }
    /**
     * @}
     */

    /**
     * Camera buffer
     */
    private class CameraBuffer {
        /**
         * YUV frame where frame size is always (videoWidth*videoHeight*3)/2
         */
        private byte frame[] = new byte[(selectedVideoCodec.getWidth()
                * selectedVideoCodec.getHeight() * 3) / 2];

        /**
         * Set the last captured frame
         *
         * @param frame Frame
         */
        public void setFrame(byte[] frame) {
            this.frame = frame;
        }

        /**
         * Return the last captured frame
         *
         * @return Frame
         */
        public byte[] getFrame() {
            return frame;
        }
    }

    /**
     * Video capture thread
     */
    private Thread captureThread = new Thread() {
        /**
         * Timestamp
         */
        private long timeStamp = 0;

        /**
         * Processing
         */
        public void run() {
            if (rtpInput == null) {
                return;
            }

            int timeToSleep = 1000 / selectedVideoCodec.getFramerate();
            int timestampInc = 90000 / selectedVideoCodec.getFramerate();
            byte[] frameData;
            byte[] encodedFrame;
            long encoderTs = 0;
            long oldTs = System.currentTimeMillis();

            while (started) {
                // Set timestamp
                long time = System.currentTimeMillis();
                encoderTs = encoderTs + (time - oldTs);

                // Get data to encode
                frameData = frameBuffer.getFrame();

                // Encode frame
                int encodeResult = 0;
                if (!opened) {
                    if (logger.isActivated()) {
                        logger.debug("player is closed, do closeEncoder");
                    }
                    closeEncoder();
                    break;
                }
                if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                    encodedFrame = NativeH264Encoder.EncodeFrame(frameData, encoderTs);
                    encodeResult = NativeH264Encoder.getLastEncodeStatus();
                } else {
                	encodedFrame = NativeH263Encoder.EncodeFrame(frameData, encoderTs);
                }

                if ((encodeResult == 0) && (encodedFrame.length > 1)) {
                    // Send encoded frame
                    rtpInput.addFrame(encodedFrame, timeStamp);
                    timeStamp += timestampInc;
                } else {
                	if (logger.isActivated()) {
                		logger.error("Encoding error");
                	}
                }

                // Sleep between frames if necessary
                long delta = System.currentTimeMillis() - time;
                if (delta < timeToSleep) {
                    try {
                        Thread.sleep((timeToSleep - delta) - (((timeToSleep - delta) * 10) / 100));
                    } catch (InterruptedException e) {
                    }
                }

                // Update old timestamp
                oldTs = time;
            }
        }
    };

    /**
     * Media RTP input
     */
    private static class MediaRtpInput implements MediaInput {
        /**
         * Received frames
         */
        private FifoBuffer fifo = null;

        /**
         * Constructor
         */
        public MediaRtpInput() {
        }

        /**
         * Add a new video frame
         *
         * @param data Data
         * @param timestamp Timestamp
         */
        public void addFrame(byte[] data, long timestamp) {
            if (fifo != null) {
                fifo.addObject(new MediaSample(data, timestamp));
            }
        }

        /**
         * Open the player
         */
        public void open() {
            fifo = new FifoBuffer();
        }

        /**
         * Close the player
         */
        public void close() {
            if (fifo != null) {
                fifo.close();
                fifo = null;
            }
        }

        /**
         * Read a media sample (blocking method)
         *
         * @return Media sample
         * @throws MediaException
         */
        public MediaSample readSample() throws MediaException {
            try {
                if (fifo != null) {
                    return (MediaSample)fifo.getObject();
                } else {
                    throw new MediaException("Media input not opened");
                }
            } catch (Exception e) {
                throw new MediaException("Can't read media sample");
            }
        }
    }

    /**
     * M: Added for share progress control @{
     */
    /**
     * Resume video share
     */
    public synchronized void resume() {
        if (logger.isActivated()) {
            logger.debug("resume()");
        }
    }

    /**
     * Pause video share
     */
    public synchronized void pause() {
        if (logger.isActivated()) {
            logger.debug("pause()");
        }
    }

    /**
     * Drag to a specific timestamp to share video
     * 
     * @param timestamp The timestamp away from the first frame, in
     *            milliseconds.
     * @return The code to identify whether it is successful.
     */
    public int seekTo(long timestamp) {
        if (logger.isActivated()) {
            logger.debug("seekTo() timestamp: " + timestamp);
        }
        return 0;
    }

    /**
     * Get the total duration of the shared video
     * 
     * @return The total duration
     */
    public long getTotalDuration() {
        if (logger.isActivated()) {
            logger.debug("getTotalDuration()");
        }
        return 0L;
    }

    /**
     * Set the total duration of the shared video
     * 
     * @param duration The total duration
     */
    public void setTotalDuration(long duration) {
        if (logger.isActivated()) {
            logger.debug("setTotalDuration() duration: " + duration);
        }
    }
    /**
     * @}
     */
}
