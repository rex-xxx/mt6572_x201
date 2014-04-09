/**
 * Calculates remaining recording time based on available disk space and
 * optionally a maximum recording file size.
 * 
 * The reason why this is not trivial is that the file grows in blocks every few
 * seconds or so, while we want a smooth count down.
 */
package com.android.soundrecorder;

import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;

import com.mediatek.storage.StorageManagerEx;

import java.io.File;

class RemainingTimeCalculator {
    private static final String TAG = "SR/RemainingTimeCalculator";
    public static final int UNKNOWN_LIMIT = 0;
    public static final int FILE_SIZE_LIMIT = 1;
    public static final int DISK_SPACE_LIMIT = 2;
    /** M: static variable about magic number @{ */
    private static final int ONE_SECOND = 1000;
    private static final int BIT_RATE = 8;
    private static final float RESERVE_SAPCE = SoundRecorderService.LOW_STORAGE_THRESHOLD / 2;
    /** @} */
    private static final String RECORDING = "Recording";

    // which of the two limits we will hit (or have fit) first
    private int mCurrentLowerLimit = UNKNOWN_LIMIT;
    // Rate at which the file grows
    private int mBytesPerSecond;

    /** M: using for calculating more accurate/normal remaining time @{ */
    // the last time run timeRemaining()
    private long mLastTimeRunTimeRemaining;
    // the last remaining time
    private long mLastRemainingTime = -1;
    /** @} */
    private long mMaxBytes;
    // time at which number of free blocks last changed
    private long mBlocksChangedTime;
    // number of available blocks at that time
    private long mLastBlocks;
    // time at which the size of the file has last changed
    private long mFileSizeChangedTime;
    // size of the file at that time
    private long mLastFileSize;

    // State for tracking file size of recording.
    private File mRecordingFile;
    private String mSDCardDirectory;
    private final StorageManager mStorageManager;
    // if recording has been pause
    private boolean mPauseTimeRemaining = false;
    private SoundRecorderService mService;
    private String mFilePath;

    /**
     * the construction of RemainingTimeCalculator
     * 
     * @param storageManager
     *            StorageManager
     */
    public RemainingTimeCalculator(StorageManager storageManager, SoundRecorderService service) {
        /** M: initialize mStorageManager */
        mStorageManager = storageManager;
        /** M: initialize mSDCardDirectory using a function */
        getSDCardDirectory();
        /** M: initialize mService */
        mService = service;
    }

    /**
     * If called, the calculator will return the minimum of two estimates: how
     * long until we run out of disk space and how long until the file reaches
     * the specified size.
     * 
     * @param file
     *            the file to watch
     * @param maxBytes
     *            the limit
     */
    public void setFileSizeLimit(File file, long maxBytes) {
        mRecordingFile = file;
        mMaxBytes = maxBytes;
    }

    /**
     * Resets the interpolation.
     */
    public void reset() {
        LogUtils.i(TAG, "<reset>");
        mCurrentLowerLimit = UNKNOWN_LIMIT;
        mBlocksChangedTime = -1;
        mFileSizeChangedTime = -1;
        /** M: reset new variable @{ */
        mPauseTimeRemaining = false;
        mLastRemainingTime = -1;
        mLastBlocks = -1;
        getSDCardDirectory();
        /** @} */
    }

    /**
     * M: return byte rate, using by SoundRecorder class when store state
     * 
     * @return byt e rate
     */
    public int getByteRate() {
        return mBytesPerSecond;
    }

    /**
     * M: in order to calculate more accurate remaining time, set
     * mPauseTimeRemaining as true when MediaRecorder pause recording
     * 
     * @param pause
     *            whether set mPauseTimeRemaining as true
     */
    public void setPauseTimeRemaining(boolean pause) {
        mPauseTimeRemaining = pause;
    }

    /**
     * M: Returns how long (in seconds) we can continue recording. Because the
     * remaining time is calculated by estimation, add man-made control to
     * remaining time, and make it not increase when available blocks is
     * reducing
     * 
     * @param isFirstTimeGetRemainingTime
     *            if the first time to getRemainingTime
     * @return the remaining time that Recorder can record
     */
    public long timeRemaining(boolean isFirstTimeGetRemainingTime) {
        /**
         * M:Modified for SD card hot plug-in/out. Should to check the savePath
         * of the current file rather than default write path.@{
         */
        mFilePath = mService.getCurrentFilePath();
        if (mFilePath != null) {
            int index = mFilePath.indexOf(RECORDING, 0) - 1;
            mSDCardDirectory = mFilePath.substring(0, index);
        }
        LogUtils.i(TAG, "timeRemaining --> mFilePath is :" + mFilePath);
        /**@}*/
        // Calculate how long we can record based on free disk space
        // LogUtils.i(TAG,"<timeRemaining> mBytesPerSecond = " +
        // mBytesPerSecond);
        boolean blocksNotChangeMore = false;
        StatFs fs = new StatFs(mSDCardDirectory);
        long blocks = fs.getAvailableBlocks() - 1;
        long blockSize = fs.getBlockSize();
        long now = SystemClock.elapsedRealtime();
        if ((-1 == mBlocksChangedTime) || (blocks != mLastBlocks)) {
            // LogUtils.i(TAG, "<timeRemaining> blocks has changed from " +
            // mLastBlocks + " to "
            // + blocks);
            blocksNotChangeMore = (blocks <= mLastBlocks) ? true : false;
            // LogUtils.i(TAG, "<timeRemaining> blocksNotChangeMore = " +
            // blocksNotChangeMore);
            mBlocksChangedTime = now;
            mLastBlocks = blocks;
        } else if (blocks == mLastBlocks) {
            blocksNotChangeMore = true;
        }

        /*
         * The calculation below always leaves one free block, since free space
         * in the block we're currently writing to is not added. This last block
         * might get nibbled when we close and flush the file, but we won't run
         * out of disk.
         */

        // at mBlocksChangedTime we had this much time
        float resultTemp = ((float) (mLastBlocks * blockSize - RESERVE_SAPCE)) / mBytesPerSecond;

        // if recording has been pause, we should add pause time to
        // mBlocksChangedTime
        // LogUtils.i(TAG, "<timeRemaining> mPauseTimeRemaining = " +
        // mPauseTimeRemaining);
        if (mPauseTimeRemaining) {
            mBlocksChangedTime += (now - mLastTimeRunTimeRemaining);
            mPauseTimeRemaining = false;
        }
        mLastTimeRunTimeRemaining = now;

        // so now we have this much time
        resultTemp -= ((float) (now - mBlocksChangedTime)) / ONE_SECOND;
        long resultDiskSpace = (long) resultTemp;
        mLastRemainingTime = (-1 == mLastRemainingTime) ? resultDiskSpace : mLastRemainingTime;
        if (blocksNotChangeMore && (resultDiskSpace > mLastRemainingTime)) {
            // LogUtils.i(TAG, "<timeRemaining> result = " + resultDiskSpace
            // + " blocksNotChangeMore = true");
            resultDiskSpace = mLastRemainingTime;
            // LogUtils.i(TAG, "<timeRemaining> result = " + resultDiskSpace);
        } else {
            mLastRemainingTime = resultDiskSpace;
            // LogUtils.i(TAG, "<timeRemaining> result = " + resultDiskSpace);
        }

        if ((null == mRecordingFile) && !isFirstTimeGetRemainingTime) {
            mCurrentLowerLimit = DISK_SPACE_LIMIT;
            // LogUtils.i(TAG,
            // "<timeRemaining> mCurrentLowerLimit = DISK_SPACE_LIMIT "
            // + mCurrentLowerLimit);
            return resultDiskSpace;
        }

        // If we have a recording file set, we calculate a second estimate
        // based on how long it will take us to reach mMaxBytes.
        if (null != mRecordingFile) {
            mRecordingFile = new File(mRecordingFile.getAbsolutePath());
            long fileSize = mRecordingFile.length();

            if ((-1 == mFileSizeChangedTime) || (fileSize != mLastFileSize)) {
                mFileSizeChangedTime = now;
                mLastFileSize = fileSize;
            }
            long resultFileSize = (mMaxBytes - fileSize) / mBytesPerSecond;
            resultFileSize -= (now - mFileSizeChangedTime) / ONE_SECOND;
            resultFileSize -= 1; // just for safety
            mCurrentLowerLimit = (resultDiskSpace < resultFileSize) ? DISK_SPACE_LIMIT
                    : FILE_SIZE_LIMIT;
            // LogUtils.i(TAG, "<timeRemaining> mCurrentLowerLimit = " +
            // mCurrentLowerLimit);
            return Math.min(resultDiskSpace, resultFileSize);
        }
        return 0;
    }

    /**
     * Indicates which limit we will hit (or have hit) first, by returning one
     * of FILE_SIZE_LIMIT or DISK_SPACE_LIMIT or UNKNOWN_LIMIT. We need this to
     * display the correct message to the user when we hit one of the limits.
     * 
     * @return current limit is FILE_SIZE_LIMIT or DISK_SPACE_LIMIT
     */
    public int currentLowerLimit() {
        return mCurrentLowerLimit;
    }

    /**
     * Sets the bit rate used in the interpolation.
     * 
     * @param bitRate
     *            the bit rate to set in bits/second.
     */
    public void setBitRate(int bitRate) {
        mBytesPerSecond = bitRate / BIT_RATE;
        LogUtils.i(TAG, "<setBitRate> mBytesPerSecond = " + mBytesPerSecond);
    }

    /** M: define a function to initialize the SD Card Directory */
    private void getSDCardDirectory() {
        if (null != mStorageManager) {
            mSDCardDirectory = StorageManagerEx.getDefaultPath();
        }
    }

    /**
     * the remaining disk space that Record can record
     * 
     * @return the remaining disk space
     */
    public long diskSpaceRemaining() {
        StatFs fs = new StatFs(mSDCardDirectory);
        long blocks = fs.getAvailableBlocks() - 1;
        long blockSize = fs.getBlockSize();
        return (long) ((blocks * blockSize) - RESERVE_SAPCE);
    }
}
