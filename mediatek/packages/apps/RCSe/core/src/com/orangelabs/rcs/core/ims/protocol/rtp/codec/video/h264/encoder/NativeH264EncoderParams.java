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

package com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder;

/**
 * H264 Encoder settings
 *
 * @author Deutsche Telekom
 */
public class NativeH264EncoderParams {

    // ----- Contants -----
    // This constants values must be updated with those that were in the encoder
    // codec

    // - Targeted profile to encode -
    public static final int PROFILE_DEFAULT = 0;
    public static final int PROFILE_BASELINE = 1;
    public static final int PROFILE_MAIN = 2;
    public static final int PROFILE_EXTENDED = 3;
    public static final int PROFILE_HIGH = 4;
    public static final int PROFILE_HIGH10 = 5;
    public static final int PROFILE_HIGH422 = 6;
    public static final int PROFILE_HIGH444 = 7;

    // - Targeted level to encode -
    public static final int LEVEL_AUTODETECT = 0;
    public static final int LEVEL_1 = 1;
    public static final int LEVEL_1B = 2;
    public static final int LEVEL_11 = 3;
    public static final int LEVEL_12 = 4;
    public static final int LEVEL_13 = 5;
    public static final int LEVEL_2 = 6;
    public static final int LEVEL_21 = 7;
    public static final int LEVEL_22 = 8;
    public static final int LEVEL_3 = 9;
    public static final int LEVEL_31 = 10;
    public static final int LEVEL_32 = 11;
    public static final int LEVEL_4 = 12;
    public static final int LEVEL_41 = 13;
    public static final int LEVEL_42 = 14;
    public static final int LEVEL_5 = 15;
    public static final int LEVEL_51 = 16;

    // - Contains supported video input format -
    public static final int VIDEO_FORMAT_RGB24 = 0;
    public static final int VIDEO_FORMAT_RGB12 = 1;
    public static final int VIDEO_FORMAT_YUV420 = 2;
    public static final int VIDEO_FORMAT_UYVY = 3;
    public static final int VIDEO_FORMAT_YUV420SEMIPLANAR = 4;

    // - Type of contents for optimal encoding mode -
    public static final int ENCODING_MODE_TWOWAY = 0;
    public static final int ENCODING_MODE_RECORDER = 1;
    public static final int ENCODING_MODE_STREAMING = 2;
    public static final int ENCODING_MODE_DOWNLOAD = 3;

    // - Output format -
    public static final int OUTPUT_FORMAT_ANNEXB = 0;
    public static final int OUTPUT_FORMAT_MP4 = 1;
    public static final int OUTPUT_FORMAT_RTP = 2;

    // - Rate control type -
    public static final int RATE_CONTROL_TYPE_CONSTANT_Q = 0;
    public static final int RATE_CONTROL_TYPE_CBR_1 = 1;
    public static final int RATE_CONTROL_TYPE_VBR_1 = 2;

    // ----- Properties -----

    private int frameWidth;
    private int frameHeight;
    private float frameRate;
    private int frameOrientation; // TODO not implemented yet on the codec side
    private int videoFormat;
    private int encodeID;
    private int profile;
    private int level;
    private int numLayer;
    private int bitRate;
    private int encMode;
    private boolean outOfBandParamSet;
    private int outputFormat;
    private int packetSize;
    private int rateControlType;
    private float bufferDelay;
    private int iquant;
    private int pquant;
    private int bquant;
    private boolean sceneDetection;
    private int iFrameInterval;
    private int numIntraMBRefresh;
    private int clipDuration;
    private byte[] fSIBuff;
    private int fSIBuffLength;

    // ----- Constructors -----

    public NativeH264EncoderParams() {
        // Default parameter that were being used in the codec, some of them
        // hard coded
        this.frameWidth = 176;
        this.frameHeight = 144;
        this.frameRate = 15;
        this.frameOrientation = 0;
        this.videoFormat = VIDEO_FORMAT_YUV420SEMIPLANAR;
        this.encodeID = 0;
        this.profile = PROFILE_BASELINE;
        this.level = LEVEL_1B;
        this.numLayer = 1;
        this.bitRate = 64000;
        this.encMode = ENCODING_MODE_TWOWAY;
        this.outOfBandParamSet = true;
        this.outputFormat = OUTPUT_FORMAT_RTP;
        this.packetSize = 8192;
        this.rateControlType = RATE_CONTROL_TYPE_CBR_1;
        this.bufferDelay = 2;
        this.iquant = 15;
        this.pquant = 12;
        this.bquant = 0;
        this.sceneDetection = false;
        this.iFrameInterval = 15;
        this.numIntraMBRefresh = 50;
        this.clipDuration = 0;
        this.fSIBuff = null;
        this.fSIBuffLength = 0;
    }

    public NativeH264EncoderParams(int profile, int level, int frameWidth, int frameHeight,
    		int bitRate, float frameRate, int packetSize) {
        this(); // to fill the default parameters

        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.bitRate = bitRate;
        this.frameRate = frameRate;
        this.packetSize = packetSize;
        setProfile(profile);
        setLevel(level);
    }

    // ----- Getters and Setters -----

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(float frameRate) {
        this.frameRate = frameRate;
    }

    public int getFrameOrientation() {
        return frameOrientation;
    }

    public void setFrameOrientation(int frameOrientation) {
        this.frameOrientation = frameOrientation;
    }

    public int getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(int videoFormat) {
        this.videoFormat = videoFormat;
    }

    public int getEncodeID() {
        return encodeID;
    }

    public void setEncodeID(int encodeID) {
        this.encodeID = encodeID;
    }

    public int getProfile() {
        return profile;
    }

    public void setProfile(int profile) {
        this.profile = profile;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getNumLayer() {
        return numLayer;
    }

    public void setNumLayer(int numLayer) {
        this.numLayer = numLayer;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getEncMode() {
        return encMode;
    }

    public void setEncMode(int encMode) {
        this.encMode = encMode;
    }

    public boolean isOutOfBandParamSet() {
        return outOfBandParamSet;
    }

    public void setOutOfBandParamSet(boolean outOfBandParamSet) {
        this.outOfBandParamSet = outOfBandParamSet;
    }

    public int getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(int outputFormat) {
        this.outputFormat = outputFormat;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public int getRateControlType() {
        return rateControlType;
    }

    public void setRateControlType(int rateControlType) {
        this.rateControlType = rateControlType;
    }

    public float getBufferDelay() {
        return bufferDelay;
    }

    public void setBufferDelay(float bufferDelay) {
        this.bufferDelay = bufferDelay;
    }

    public int getIquant() {
        return iquant;
    }

    public void setIquant(int iquant) {
        this.iquant = iquant;
    }

    public int getPquant() {
        return pquant;
    }

    public void setiPquant(int pquant) {
        this.pquant = pquant;
    }

    public int getBquant() {
        return bquant;
    }

    public void setiBquant(int bquant) {
        this.bquant = bquant;
    }

    public boolean isSceneDetection() {
        return sceneDetection;
    }

    public void setSceneDetection(boolean sceneDetection) {
        this.sceneDetection = sceneDetection;
    }

    public int getIFrameInterval() {
        return iFrameInterval;
    }

    public void setIFrameInterval(int iFrameInterval) {
        this.iFrameInterval = iFrameInterval;
    }

    public int getNumIntraMBRefresh() {
        return numIntraMBRefresh;
    }

    public void setNumIntraMBRefresh(int numIntraMBRefresh) {
        this.numIntraMBRefresh = numIntraMBRefresh;
    }

    public int getClipDuration() {
        return clipDuration;
    }

    public void setClipDuration(int clipDuration) {
        this.clipDuration = clipDuration;
    }

    public byte[] getFSIBuff() {
        return fSIBuff;
    }

    public void setFSIBuff(byte[] fSIBuff) {
        this.fSIBuff = fSIBuff;
    }

    public int getFSIBuffLength() {
        return fSIBuffLength;
    }

    public void setFSIBuffLength(int fSIBuffLength) {
        this.fSIBuffLength = fSIBuffLength;
    }
}
