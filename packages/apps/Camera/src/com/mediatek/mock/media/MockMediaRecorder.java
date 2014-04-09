/*
 * Copyright (C) 2007 The Android Open Source Project
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
 */

package com.mediatek.mock.media;

import android.content.Context;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.android.camera.R;
import com.mediatek.mock.hardware.MockCamera;
import com.mediatek.xlog.Xlog;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class MockMediaRecorder extends MediaRecorder {
    private static final String TAG = "MockMediaRecorder";

    // The two fields below are accessed by native methods
    @SuppressWarnings("unused")
    private int mNativeContext;

    @SuppressWarnings("unused")
    private Surface mSurface;

    private String mPath;
    private FileDescriptor mFd;
    private EventHandler mEventHandler;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private MediaActionSound mCameraSound;
    private Context mContext;
    private Thread mSavingThread;

    private Runnable mVideoWriter = new Runnable() {
       public void run() {
           InputStream inputStream;
           Log.i(TAG, "Saving path = " + mPath);
           if (mPath != null) {
               inputStream = mContext.getResources().openRawResource(R.raw.video);
               FileOutputStream out = null;
               byte[] b = new byte[1024];
               int size = -1;
               try {
                   out = new FileOutputStream(mPath);
                   while ((size = inputStream.read(b)) != -1) {
                       out.write(b, 0, size);
                   }
               } catch (FileNotFoundException e) {
                   Log.e(TAG, "File: " + mPath + " not found!");
               } catch (IOException ioe) {
                   Log.i(TAG, "read blank.jpg in raw reault in error");
               } finally {
                   try {
                       if (inputStream != null) {
                           inputStream.close();
                       }
                       if (out != null) {
                           out.close();
                       }
                   } catch (IOException e) {
                       Log.i(TAG, "close inputStream fail");
                   }
               }
           } else if (mFd != null) {
               Log.i(TAG, "mFd != null");
           }
       }
    };

    /**
     * Default constructor.
     */
    public MockMediaRecorder() {

        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }
        native_setup(new WeakReference<MockMediaRecorder>(this));
    }

    public void setCamera(MockCamera c) {
        mCameraSound = c.getCameraSound();
        Log.d(TAG, "setCamera:mCameraSound=" + mCameraSound + "//Camera=" + c);
    }

    public void setPreviewDisplay(Surface sv) {
        mSurface = sv;
    }

    public final class AudioSource {
      /* Do not change these values without updating their counterparts
       * in system/core/include/system/audio.h!
       */
        private AudioSource() {}

        /** Default audio source **/
        public static final int DEFAULT = 0;

        /** Microphone audio source */
        public static final int MIC = 1;

        /** Voice call uplink (Tx) audio source */
        public static final int VOICE_UPLINK = 2;

        /** Voice call downlink (Rx) audio source */
        public static final int VOICE_DOWNLINK = 3;

        /** Voice call uplink + downlink audio source */
        public static final int VOICE_CALL = 4;

        /** Microphone audio source with same orientation as camera if available, the main
         *  device microphone otherwise */
        public static final int CAMCORDER = 5;

        /** Microphone audio source tuned for voice recognition if available, behaves like
         *  {@link #DEFAULT} otherwise. */
        public static final int VOICE_RECOGNITION = 6;

        /** Microphone audio source tuned for voice communications such as VoIP. It
         *  will for instance take advantage of echo cancellation or automatic gain control
         *  if available. It otherwise behaves like {@link #DEFAULT} if no voice processing
         *  is applied.
         */
        public static final int VOICE_COMMUNICATION = 7;
        
        /**
         * {@hide}
         */
        public static final int MATV = 98;
        
        /**
         * {@hide}
         */
        public static final int FM = 99;
    }

    public final class VideoSource {
      /* Do not change these values without updating their counterparts
       * in include/media/mediarecorder.h!
       */
        private VideoSource() {}
        public static final int DEFAULT = 0;
        /** Camera video source */
        public static final int CAMERA = 1;
        /** @hide */
        public static final int GRALLOC_BUFFER = 2;
    }

    public final class OutputFormat {
      /* Do not change these values without updating their counterparts
       * in include/media/mediarecorder.h!
       */
        private OutputFormat() {}
        public static final int DEFAULT = 0;
        /** 3GPP media file format*/
        public static final int THREE_GPP = 1;
        /** MPEG4 media file format*/
        public static final int MPEG_4 = 2;

        /** The following formats are audio only .aac or .amr formats */

        public static final int RAW_AMR = 3;

        /** AMR NB file format */
        public static final int AMR_NB = 3;

        /** AMR WB file format */
        public static final int AMR_WB = 4;

        /** @hide AAC ADIF file format */
        public static final int AAC_ADIF = 5;

        /** AAC ADTS file format */
        public static final int AAC_ADTS = 6;

        /** @hide Stream over a socket, limited to a single stream */
        public static final int OUTPUT_FORMAT_RTP_AVP = 7;

        /** @hide H.264/AAC data encapsulated in MPEG2/TS */
        public static final int OUTPUT_FORMAT_MPEG2TS = 8;
        
        /** @hide OGG */
        public static final int OUTPUT_FORMAT_OGG = 10;
    };

    public final class AudioEncoder {
      /* Do not change these values without updating their counterparts
       * in include/media/mediarecorder.h!
       */
        private AudioEncoder() {}
        public static final int DEFAULT = 0;
        /** AMR (Narrowband) audio codec */
        public static final int AMR_NB = 1;
        /** AMR (Wideband) audio codec */
        public static final int AMR_WB = 2;
        /** AAC Low Complexity (AAC-LC) audio codec */
        public static final int AAC = 3;
        /** High Efficiency AAC (HE-AAC) audio codec */
        public static final int HE_AAC = 4;
        /** Enhanced Low Delay AAC (AAC-ELD) audio codec */
        public static final int AAC_ELD = 5;
        
        /** @hide vorbis audio codec */
        public static final int VORBIS = 8;
    }

    public final class VideoEncoder {
      /* Do not change these values without updating their counterparts
       * in include/media/mediarecorder.h!
       */
        private VideoEncoder() {}
        public static final int DEFAULT = 0;
        public static final int H263 = 1;
        public static final int H264 = 2;
        public static final int MPEG_4_SP = 3;
    }

    public void setAudioSource(int audio_source) {
    }

//    public static final int getAudioSourceMax() { return AudioSource.VOICE_COMMUNICATION; }

    public void setVideoSource(int video_source) {
    }

    public void setProfile(CamcorderProfile profile) {
        setOutputFormat(profile.fileFormat);
        setVideoFrameRate(profile.videoFrameRate);
        setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        setVideoEncodingBitRate(profile.videoBitRate);
        setVideoEncoder(profile.videoCodec);
        if (profile.quality >= CamcorderProfile.QUALITY_TIME_LAPSE_LOW &&
             profile.quality <= CamcorderProfile.QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT) {
            // Nothing needs to be done. Call to setCaptureRate() enables
            // time lapse video recording.
        } else {
            setAudioEncodingBitRate(profile.audioBitRate);
            setAudioChannels(profile.audioChannels);
            setAudioSamplingRate(profile.audioSampleRate);
            setAudioEncoder(profile.audioCodec);
        }
    }

    public void setCaptureRate(double fps) {
        // Make sure that time lapse is enabled when this method is called.
        setParameter("time-lapse-enable=1");

        double timeBetweenFrameCapture = 1 / fps;
        int timeBetweenFrameCaptureMs = (int) (1000 * timeBetweenFrameCapture);
        setParameter(String.format(new Locale("en"), "time-between-time-lapse-frame-capture=%d",
                    timeBetweenFrameCaptureMs));
    }

    public void setOrientationHint(int degrees) {
        if (degrees != 0   &&
            degrees != 90  &&
            degrees != 180 &&
            degrees != 270) {
            throw new IllegalArgumentException("Unsupported angle: " + degrees);
        }
        setParameter("video-param-rotation-angle-degrees=" + degrees);
    }

    public void setLocation(float latitude, float longitude) {
        int latitudex10000  = (int) (latitude * 10000 + 0.5);
        int longitudex10000 = (int) (longitude * 10000 + 0.5);

        if (latitudex10000 > 900000 || latitudex10000 < -900000) {
            String msg = "Latitude: " + latitude + " out of range.";
            throw new IllegalArgumentException(msg);
        }
        if (longitudex10000 > 1800000 || longitudex10000 < -1800000) {
            String msg = "Longitude: " + longitude + " out of range";
            throw new IllegalArgumentException(msg);
        }

        setParameter("param-geotag-latitude=" + latitudex10000);
        setParameter("param-geotag-longitude=" + longitudex10000);
    }

    public void setOutputFormat(int output_format) {
    }

    public void setVideoSize(int width, int height) {
    }

    public void setVideoFrameRate(int rate) {
    }

    public void setMaxDuration(int max_duration_ms) {
    }

    public void setMaxFileSize(long max_filesize_bytes) {
    }

    public void setAudioEncoder(int audio_encoder) {
    }

    public void setVideoEncoder(int video_encoder) {
    }

    public void setAudioSamplingRate(int samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Audio sampling rate is not positive");
        }
        setParameter("audio-param-sampling-rate=" + samplingRate);
    }

    public void setAudioChannels(int numChannels) {
        if (numChannels <= 0) {
            throw new IllegalArgumentException("Number of channels is not positive");
        }
        setParameter("audio-param-number-of-channels=" + numChannels);
    }

    public void setAudioEncodingBitRate(int bitRate) {
        if (bitRate <= 0) {
            throw new IllegalArgumentException("Audio encoding bit rate is not positive");
        }
        setParameter("audio-param-encoding-bitrate=" + bitRate);
    }

    public void setVideoEncodingBitRate(int bitRate) {
        if (bitRate <= 0) {
            throw new IllegalArgumentException("Video encoding bit rate is not positive");
        }
        setParameter("video-param-encoding-bitrate=" + bitRate);
    }

    public void setAuxiliaryOutputFile(FileDescriptor fd) {
        Log.w(TAG, "setAuxiliaryOutputFile(FileDescriptor) is no longer supported.");
    }

    public void setAuxiliaryOutputFile(String path) {
        Log.w(TAG, "setAuxiliaryOutputFile(String) is no longer supported.");
    }

    public void setOutputFile(FileDescriptor fd) throws IllegalStateException {
        mPath = null;
        mFd = fd;
    }

    public void setOutputFile(String path) throws IllegalStateException {
        mFd = null;
        mPath = path;
    }

    private void _setOutputFile(FileDescriptor fd, long offset, long length) {
    }

    private void _prepare() {
    }

    public void prepare() throws IllegalStateException, IOException {
        if (mPath != null) {
            FileOutputStream fos = new FileOutputStream(mPath);
            try {
                _setOutputFile(fos.getFD(), 0, 0);
            } finally {
                fos.close();
            }
        } else if (mFd != null) {
            _setOutputFile(mFd, 0, 0);
        } else {
            throw new IOException("No valid output file");
        }

        _prepare();
    }

    public void start() {
        if (null != mCameraSound) {
            mCameraSound.play(MediaActionSound.START_VIDEO_RECORDING);
        }
        mSavingThread = new Thread(mVideoWriter);
        mSavingThread.start();

        mEventHandler.sendEmptyMessageDelayed(MEDIA_RECORDER_INFO_START_TIMER, 100);
    }

    public void stop() {
        if (null != mCameraSound) {
            mCameraSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
        }
        try {
            mSavingThread.join();
        } catch (InterruptedException e) {
            Log.i(TAG, "Video Saving done");
        }
        mSavingThread = null;
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Log.i(TAG, "Loading sound");
        }
    }

    public void reset() {
        native_reset();
        // make sure none of the listeners get called anymore
        mEventHandler.removeCallbacksAndMessages(null);
    }

    private void native_reset() {
    }
    public int getMaxAmplitude() {
        return 3;
    }

    public static final int MEDIA_RECORDER_ERROR_UNKNOWN = 1;

    /**
     * Interface definition for a callback to be invoked when an error
     * occurs while recording.
     */
    public interface OnErrorListener {
        void onError(MockMediaRecorder mr, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an error occurs while
     * recording.
     *
     * @param l the callback that will be run
     */
    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    public static final int MEDIA_RECORDER_INFO_UNKNOWN              = 1;
    public static final int MEDIA_RECORDER_INFO_MAX_DURATION_REACHED = 800;
    public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED = 801;
    /**
     * {@hide}
     */
    public static final int MEDIA_RECORDER_INFO_FPS_ADJUSTED               = 897;
    /**
     * {@hide}
     */
    public static final int MEDIA_RECORDER_INFO_BITRATE_ADJUSTED           = 898;
    /**
     * {@hide}
     */
    public static final int MEDIA_RECORDER_INFO_WRITE_SLOW                 = 899;

    /** informational events for individual tracks, for testing purpose.
     * The track informational event usually contains two parts in the ext1
     * arg of the onInfo() callback: bit 31-28 contains the track id; and
     * the rest of the 28 bits contains the informational event defined here.
     * For example, ext1 = (1 << 28 | MEDIA_RECORDER_TRACK_INFO_TYPE) if the
     * Please update the comment for onInfo also when these
     * events are unhidden so that application knows how to extract the track
     * id and the informational event type from onInfo callback.
     *
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_LIST_START        = 1000;
    /** Signal the completion of the track for the recording session.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_COMPLETION_STATUS = 1000;
    /** Indicate the recording progress in time (ms) during recording.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_PROGRESS_IN_TIME  = 1001;
    /** Indicate the track type: 0 for Audio and 1 for Video.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_TYPE              = 1002;
    /** Provide the track duration information.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_DURATION_MS       = 1003;
    /** Provide the max chunk duration in time (ms) for the given track.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_MAX_CHUNK_DUR_MS  = 1004;
    /** Provide the total number of recordd frames.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_ENCODED_FRAMES    = 1005;
    /** Provide the max spacing between neighboring chunks for the given track.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INTER_CHUNK_TIME_MS    = 1006;
    /** Provide the elapsed time measuring from the start of the recording
     * till the first output frame of the given track is received, excluding
     * any intentional start time offset of a recording session for the
     * purpose of eliminating the recording sound in the recorded file.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_INITIAL_DELAY_MS  = 1007;
    /** Provide the start time difference (delay) betweeen this track and
     * the start of the movie.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_START_OFFSET_MS   = 1008;
    /** Provide the total number of data (in kilo-bytes) encoded.
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_DATA_KBYTES       = 1009;
    /**
     * {@hide}
     */
    public static final int MEDIA_RECORDER_INFO_START_TIMER             = 1998;
    /**
     * {@hide}
     */
    public static final int MEDIA_RECORDER_INFO_CAMERA_RELEASE          = 1999;
    /**
     * {@hide}
     */
    public static final int MEDIA_RECORDER_ENCODER_ERROR                = -1103;
    /**
     * {@hide}
     */
    public static final int MEDIA_RECORDER_TRACK_INFO_LIST_END          = 2000;

    /**
     * Interface definition for a callback to be invoked when an error
     * occurs while recording.
     */
    public interface OnInfoListener {
        void onInfo(MockMediaRecorder mr, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an informational event occurs while
     * recording.
     *
     * @param listener the callback that will be run
     */
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    private class EventHandler extends Handler {
        private MockMediaRecorder mMediaRecorder;

        public EventHandler(MockMediaRecorder mr, Looper looper) {
            super(looper);
            mMediaRecorder = mr;
        }

        /* Do not change these values without updating their counterparts
         * in include/media/mediarecorder.h!
         */
        private static final int MEDIA_RECORDER_EVENT_LIST_START = 1;
        private static final int MEDIA_RECORDER_EVENT_ERROR      = 1;
        private static final int MEDIA_RECORDER_EVENT_INFO       = 2;
        private static final int MEDIA_RECORDER_EVENT_LIST_END   = 99;

        /* Events related to individual tracks */
        private static final int MEDIA_RECORDER_TRACK_EVENT_LIST_START = 100;
        private static final int MEDIA_RECORDER_TRACK_EVENT_ERROR      = 100;
        private static final int MEDIA_RECORDER_TRACK_EVENT_INFO       = 101;
        private static final int MEDIA_RECORDER_TRACK_EVENT_LIST_END   = 1000;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MEDIA_RECORDER_INFO_START_TIMER) {
                if (mOnInfoListener != null) {
                    mOnInfoListener.onInfo(mMediaRecorder, MEDIA_RECORDER_INFO_START_TIMER, 0);
                }
            }
            if (mMediaRecorder.mNativeContext == 0) {
                Log.w(TAG, "mediarecorder went away with unhandled events");
                return;
            }
            switch(msg.what) {
            case MEDIA_RECORDER_EVENT_ERROR:
            case MEDIA_RECORDER_TRACK_EVENT_ERROR:
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(mMediaRecorder, msg.arg1, msg.arg2);
                }

                return;

            case MEDIA_RECORDER_EVENT_INFO:
            case MEDIA_RECORDER_TRACK_EVENT_INFO:
                if (mOnInfoListener != null) {
                    mOnInfoListener.onInfo(mMediaRecorder, msg.arg1, msg.arg2);
                }

                return;
            default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }
    }

    /**
     * M: Special case for camera release notify.
     */
    private static final int MEDIA_RECORDER_EVENT_INFO = 2;

    private static void postEventFromNative(Object mediarecorder_ref,
                                            int what, int arg1, int arg2, Object obj) {
        MockMediaRecorder mr = (MockMediaRecorder)((WeakReference)mediarecorder_ref).get();
        if (mr == null) {
            return;
        }

        if (mr.mEventHandler != null) {
            if (what == MEDIA_RECORDER_EVENT_INFO &&
                    arg1 == MEDIA_RECORDER_INFO_CAMERA_RELEASE) {
                Xlog.v(TAG, "MockMediaRecorder MEDIA_RECORDER_INFO_CAMERA_RELEASE");
                if (mr.mOnCameraReleasedListener != null) {
                    /// M: call notify in binder thread, video camera can go on its job. 
                    mr.mOnCameraReleasedListener.onInfo(mr, MEDIA_RECORDER_INFO_CAMERA_RELEASE, 0);
                }
                return;
            }
            Message m = mr.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mr.mEventHandler.sendMessage(m);
        }
    }
    public void release() {
    }

    private static final void native_init() {
    }

    private final void native_setup(Object mediarecorder_this) {
    }

    private final void native_finalize() {
    }

    private void setParameter(String nameValuePair) {
    }

    public void setParametersExtra(String nameValuePair) {
    }

    @Override
    protected void finalize() { native_finalize(); }
    
    /**
     * M: Pauses recording. Call start() to resume.
     * Here we just implement the pause using setParameter() function.
     * 
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     * 
     * {@hide}
     */
    public void pause() throws IllegalStateException {
        setParametersExtra("media-param-pause=1");
    }

    /**
     * {@hide}
     */
    protected OnInfoListener mOnCameraReleasedListener;

    /**
     * M: Register a callback to invoked when release job has been done while recording.
     * 
     * @param listener the callback that will be run
     * 
     * {@hide}
     */
    public void setOnCameraReleasedListener(OnInfoListener listener) {
        mOnCameraReleasedListener = listener;
    }

    /**
     * {@hide}
     */
    public void setArtistTag(String artist) {
        if (artist == null) {
            Xlog.e(TAG, "setArtistTag: Null artist!");
            return;
        }
        setParameter("media-param-tag-artist=" + artist);
    }

    /**
     * {@hide}
     */
    public void setAlbumTag(String album) {
        if (album == null) {
            Xlog.e(TAG, "setAlbumTag: Null album!");
            return;
        }
        setParameter("media-param-tag-album=" + album);
    }

    /**
     * {@hide}
     */
    public void setStereo3DType(String stereo3dType) {
        if (stereo3dType.equals(Parameters.STEREO3D_TYPE_OFF)) {
            setParameter("video-param-stereo-mode=0");
        } else if (stereo3dType.equals(Parameters.STEREO3D_TYPE_FRAMESEQ)) {
            setParameter("video-param-stereo-mode=1");
        } else if (stereo3dType.equals(Parameters.STEREO3D_TYPE_SIDEBYSIDE)) {
            setParameter("video-param-stereo-mode=2");
        } else {
            setParameter("video-param-stereo-mode=3");
        }
    }

    /**
     * {@hide}
     */
    public void setTimeLapseEnable() {
        setParameter("time-lapse-enable=1");
    }

    /**
     * {@hide}
     */
    public void setZoomValue(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Zoom value is not positive");
        }
        setParametersExtra("video-param-camera-digizoom=" + value);
    } 

    /**
     * {@hide}
     */
    public final class HDRecordMode {
        /* M: Do not change these values without updating their counterparts
         * in AudioYusuHardware.cpp.
         */
        private HDRecordMode() {}
        public static final int NORMAL = 0;
        public static final int INDOOR = 1;
        public static final int OUTDOOR = 2;
    }

    /**
     * {@hide}
     */
    public void setHDRecordMode(int mode, boolean isVideo) {
        if (mode < HDRecordMode.NORMAL || mode > HDRecordMode.OUTDOOR) {
            throw new IllegalArgumentException("Illegal HDRecord mode:" + mode);
        }
        
        if (isVideo) {
            setParameter("audio-param-hdrecvideomode=" + mode);
        } else {
            setParameter("audio-param-hdrecvoicemode=" + mode);
        }
    }
    public void setContext(Context context) {
        mContext = context;
    }
}
