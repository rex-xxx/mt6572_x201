package com.android.soundrecorder;

import android.media.MediaRecorder;
import android.os.SystemClock;
import android.os.storage.StorageManager;

import com.android.soundrecorder.RecordParamsSetting.RecordParams;
import com.mediatek.media.MediaRecorderEx;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Recorder implements MediaRecorder.OnErrorListener {

    public static final String RECORD_FOLDER = "Recording";
    public static final String SAMPLE_SUFFIX = ".tmp";

    private static final String TAG = "SR/Recorder";
    private static final String SAMPLE_PREFIX = "record";

    // M: the three below are all in millseconds
    private long mSampleLength = 0;
    private long mSampleStart = 0;
    private long mPreviousTime = 0;

    private File mSampleFile = null;
    private final StorageManager mStorageManager;
    private MediaRecorder mRecorder = null;
    private RecorderListener mListener = null;
    private int mCurrentState = SoundRecorderService.STATE_IDLE;

    // M: used for audio pre-process
    private boolean[] mSelectEffect = null;

    // M: the listener when error occurs and state changes
    public interface RecorderListener {
        // M: when state changes, we will notify listener the new state code
        void onStateChanged(Recorder recorder, int stateCode);

        // M: when error occurs, we will notify listener the error code
        void onError(Recorder recorder, int errorCode);
    }

    @Override
    /**
     * M: the error callback of MediaRecorder
     */
    public void onError(MediaRecorder recorder, int errorType, int extraCode) {
        LogUtils.i(TAG, "<onError> errorType = " + errorType + "; extraCode = " + extraCode);
        stopRecording();
        mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
    }

    /**
     * M: Constructor of Recorder
     * @param storageManager
     * @param listener
     */
    public Recorder(StorageManager storageManager, RecorderListener listener) {
        mStorageManager = storageManager;
        mListener = listener;
    }

    /**
     * M: get the current amplitude of MediaRecorder, used by VUMeter
     * @return the amplitude value
     */
    public int getMaxAmplitude() {
        return (SoundRecorderService.STATE_RECORDING != mCurrentState) ? 0 : mRecorder
                .getMaxAmplitude();
    }

    /**
     * M: get the file path of current sample file
     * @return
     */
    public String getSampleFilePath() {
        return (null == mSampleFile) ? null : mSampleFile.getAbsolutePath();
    }

    public long getSampleLength() {
        return mSampleLength;
    }

    public File getSampFile() {
        return mSampleFile;
    }

    /**
     * M: get how long time we has recorded
     * @return the record length, in millseconds
     */
    public long getCurrentProgress() {
        if (SoundRecorderService.STATE_RECORDING == mCurrentState) {
            long current = SystemClock.elapsedRealtime();
            return (long) (current - mSampleStart + mPreviousTime);
        } else if (SoundRecorderService.STATE_PAUSE_RECORDING == mCurrentState) {
            return (long) (mPreviousTime);
        }
        return 0;
    }

    /**
     * M: set Recorder to initial state
     */
    public boolean reset() {
        /** M:modified for stop recording failed. @{ */
        boolean result = true;
        if (null != mRecorder) {
            try {
                mRecorder.stop();
            } catch (RuntimeException exception) {
                result = false;
                LogUtils.e(TAG,
                        "<stopRecording> recorder illegalstate exception in recorder.stop()");
            } finally {
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            }
        }
        mSampleFile = null;
        mPreviousTime = 0;
        mSampleLength = 0;
        mSampleStart = 0;
        /**
         * M: add for some error case for example pause or goon recording
         * failed. @{
         */
        mCurrentState = SoundRecorderService.STATE_IDLE;
        /** @} */
        return result;
    }

    public boolean startRecording(RecordParams params, int fileSizeLimit) {
        LogUtils.i(TAG, "<startRecording> begin");
        if (SoundRecorderService.STATE_IDLE != mCurrentState) {
            return false;
        }
        reset();

        createRecordingFile(params.mExtension);
        if (!initAndStartMediaRecorder(params, fileSizeLimit)) {
            LogUtils.i(TAG, "<startRecording> initAndStartMediaRecorder return false");
            return false;
        }
        mSampleStart = SystemClock.elapsedRealtime();
        setState(SoundRecorderService.STATE_RECORDING);
        LogUtils.i(TAG, "<startRecording> end");
        return true;
    }

    public boolean pauseRecording() {
        if ((SoundRecorderService.STATE_RECORDING != mCurrentState) || (null == mRecorder)) {
            return false;
        }
        try {
            MediaRecorderEx.pause(mRecorder);
        } catch (IllegalArgumentException e) {
            LogUtils.e(TAG, "<pauseRecording> IllegalArgumentException");
            handleException(false, e);
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            return false;
        }
        mPreviousTime += SystemClock.elapsedRealtime() - mSampleStart;
        setState(SoundRecorderService.STATE_PAUSE_RECORDING);
        return true;
    }

    public boolean goonRecording() {
        if ((SoundRecorderService.STATE_PAUSE_RECORDING != mCurrentState) || (null == mRecorder)) {
            return false;
        }
        try {
            mRecorder.start();
        } catch (IllegalArgumentException exception) {
            LogUtils.e(TAG, "<goOnRecording> IllegalArgumentException");
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            return false;
        }

        mSampleStart = SystemClock.elapsedRealtime();
        setState(SoundRecorderService.STATE_RECORDING);
        return true;
    }

    public boolean stopRecording() {
        LogUtils.i(TAG, "<stopRecording> start");
        if (((SoundRecorderService.STATE_PAUSE_RECORDING != mCurrentState) && 
             (SoundRecorderService.STATE_RECORDING != mCurrentState)) || (null == mRecorder)) {
            LogUtils.i(TAG, "<stopRecording> end 1");
            return false;
        }
        boolean isAdd = (SoundRecorderService.STATE_RECORDING == mCurrentState) ? true : false;
        try {
            mRecorder.stop();
        } catch (RuntimeException exception) {
            /** M:modified for stop recording failed. @{ */
            handleException(false, exception);
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            LogUtils.e(TAG, "<stopRecording> recorder illegalstate exception in recorder.stop()");
        }
        if (null != mRecorder) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
        /** @} */
        if (isAdd) {
            mPreviousTime += SystemClock.elapsedRealtime() - mSampleStart;
        }
        mSampleLength = mPreviousTime;
        LogUtils.i(TAG, "<stopRecording> mSampleLength in ms is " + mPreviousTime);
        LogUtils.i(TAG, "<stopRecording> mSampleLength in s is = " + mSampleLength);

        setState(SoundRecorderService.STATE_IDLE);
        LogUtils.i(TAG, "<stopRecording> end 2");
        return true;
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    private void setState(int state) {
        mCurrentState = state;
        mListener.onStateChanged(this, state);
    }

    private boolean initAndStartMediaRecorder(RecordParams recordParams, int fileSizeLimit) {
        LogUtils.i(TAG, "<initAndStartMediaRecorder> start");
        mSelectEffect = recordParams.mAudioEffect;
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(recordParams.mOutputFormat);
        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());
        if (RecordParamsSetting.canSelectMode()) {
            MediaRecorderEx.setHDRecordMode(mRecorder, recordParams.mHDRecordMode, false);
        }
        /** M:Add for create/activate/delete AudioEffect at native layer. @{ */
        if (RecordParamsSetting.canSelectEffect()) {
            int iSelEffects = 0;
            if (mSelectEffect[RecordParamsSetting.EFFECT_AEC]) {
                iSelEffects |= (1 << RecordParamsSetting.EFFECT_AEC);
            }
            if (mSelectEffect[RecordParamsSetting.EFFECT_NS]) {
                iSelEffects |= (1 << RecordParamsSetting.EFFECT_NS);
            }
            if (mSelectEffect[RecordParamsSetting.EFFECT_AGC]) {
                iSelEffects |= (1 << RecordParamsSetting.EFFECT_AGC);
            }
            MediaRecorderEx.setPreprocessEffect(mRecorder, iSelEffects);
        }
        /**@}*/
        mRecorder.setAudioEncoder(recordParams.mAudioEncoder);
        mRecorder.setAudioChannels(recordParams.mAudioChannels);
        mRecorder.setAudioEncodingBitRate(recordParams.mAudioEncodingBitRate);
        mRecorder.setAudioSamplingRate(recordParams.mAudioSamplingRate);
        if (fileSizeLimit > 0) {
            mRecorder.setMaxFileSize(fileSizeLimit);
        }
        mRecorder.setOnErrorListener(this);
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException exception) {
            LogUtils.e(TAG, "<initAndStartMediaRecorder> IO exception");
            // M:Add for when error ,the tmp file should been delete.
            handleException(true, exception);
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            return false;
        } catch (RuntimeException exception) {
            LogUtils.e(TAG, "<initAndStartMediaRecorder> RuntimeException");
            // M:Add for when error ,the tmp file should been delete.
            handleException(true, exception);
            mListener.onError(this, ErrorHandle.ERROR_RECORDER_OCCUPIED);
            return false;
        }
        LogUtils.i(TAG, "<initAndStartMediaRecorder> end");
        return true;
    }

    private void createRecordingFile(String extension) {
        LogUtils.i(TAG, "<createRecordingFile> begin");
        String myExtension = extension + SAMPLE_SUFFIX;
        File sampleDir = null;
        if (null == mStorageManager) {
            return;
        }
        sampleDir = new File(StorageManagerEx.getDefaultPath());
        LogUtils.i(TAG, "<createRecordingFile> sd card directory is:" + sampleDir);
        String sampleDirPath = sampleDir.getAbsolutePath() + File.separator + RECORD_FOLDER;
        sampleDir = new File(sampleDirPath);

        // find a available name of recording folder,
        // Recording/Recording(1)/Recording(2)
        int dirID = 1;
        while ((null != sampleDir) && sampleDir.exists() && !sampleDir.isDirectory()) {
            sampleDir = new File(sampleDirPath + '(' + dirID + ')');
            dirID++;
        }

        if ((null != sampleDir) && !sampleDir.exists() && !sampleDir.mkdirs()) {
            LogUtils.i(TAG, "<createRecordingFile> make directory [" + sampleDir.getAbsolutePath()
                    + "] fail");
        }

        try {
            if (null != sampleDir) {
                LogUtils.i(TAG, "<createRecordingFile> sample directory  is:"
                        + sampleDir.toString());
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String time = simpleDateFormat.format(new Date(System.currentTimeMillis()));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(SAMPLE_PREFIX).append(time).append(myExtension);
            String name = stringBuilder.toString();
            mSampleFile = new File(sampleDir, name);
            boolean result = mSampleFile.createNewFile();
            LogUtils.i(TAG, "<createRecordingFile> creat file success is " + result);
            LogUtils.i(TAG, "<createRecordingFile> mSampleFile.getAbsolutePath() is: "
                    + mSampleFile.getAbsolutePath());
        } catch (IOException e) {
            mListener.onError(this, ErrorHandle.ERROR_CREATE_FILE_FAILED);
            LogUtils.e(TAG, "<createRecordingFile> io exception happens");
            return;
        }
        LogUtils.i(TAG, "<createRecordingFile> end");
    }
    
    /**
     * M: Handle Exception when call the function of MediaRecorder
     */
    public void handleException(boolean isDeleteSample, Exception exception) {
        LogUtils.i(TAG, "<handleException> the exception is: " + exception);
        if (isDeleteSample) {
            mSampleFile.delete();
        }
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }
}
