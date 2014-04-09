/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.util.BucketNames;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.video.MTKVideoView;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class TrimVideo extends Activity implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener {

    private MTKVideoView mVideoView;
    /// M: trim text view
    private TextView mSaveVideoTextView;
    private TrimControllerOverlay mController;
    private Context mContext;
    private Uri mUri;
    private final Handler mHandler = new Handler();
    public static final String TRIM_ACTION = "com.android.camera.action.TRIM";

    ///M:change to static
    //public ProgressDialog mProgress;

    private int mTrimStartTime = 0;
    private int mTrimEndTime = 0;
    private int mVideoPosition = 0;
    public static final String KEY_TRIM_START = "trim_start";
    public static final String KEY_TRIM_END = "trim_end";
    public static final String KEY_VIDEO_POSITION = "video_pos";
    ///M:change to static
    //private boolean mHasPaused = false;

    private String mSrcVideoPath = null;
    private String mSaveFileName = null;
    private static final String TIME_STAMP_NAME = "'TRIM'_yyyyMMdd_HHmmss";
    private File mSrcFile = null;
    ///M:change to static
    //private File mDstFile = null;
    private File mSaveDirectory = null;
    // For showing the result.
    ///M:change to static
    //private String saveFolderName = null;
    private static final String TAG = "Gallery2/TrimVideo";
    
    // If the time bar is being dragged.
    private boolean mDragging;
    
    /// M: add for show dialog @{
    private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            showProgressDialog();
        }
    };
    /// @}
    /// M: add for show toast @{
    private final Runnable mShowToastRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.can_not_trim),
                    Toast.LENGTH_SHORT)
                    .show();
            /// M: modify mSaveVideoTextView to can click @{
            mSaveVideoTextView.setClickable(true);
            mSaveVideoTextView.setEnabled(true);
            /// @}
        }
    };
    /// @}

    ///M: if mProgress is null, TrimVideo has stopped,
    /// set mPlayTrimVideo as true. it will play trim video again
    /// after resume TrimVideo. @{
    private static boolean mPlayTrimVideo = false;
    private static boolean mIsSaving = false;
    public static ProgressDialog mProgress;
    private static File mDstFile = null;
    private static String saveFolderName = null;
    private static boolean mHasPaused = false;

    private final Runnable mStartVideoRunnable = new Runnable() {
        @Override
        public void run() {
            // TODO: change trimming into a service to avoid
            // this progressDialog and add notification properly.
            MtkLog.v(TAG, "StartVideoRunnable,HasPaused:" + mHasPaused);
            if (!mHasPaused) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.save_into) + " " + saveFolderName,
                        Toast.LENGTH_SHORT)
                        .show();
                if(mProgress != null) {
                    mProgress.dismiss();
                    mProgress = null;
                }
                // Show the result only when the activity not stopped.
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndTypeAndNormalize(Uri.fromFile(mDstFile), "video/*");
                intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
                startActivity(intent);
                mPlayTrimVideo = false;
                mIsSaving = false;
                mDstFile = null;
                saveFolderName = null;
                finish();
            } else {
                mPlayTrimVideo = true; 
            }
        }
    };
    ///}@
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        super.onCreate(savedInstanceState);
        MtkLog.v(TAG , "onCreate()");
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        ActionBar actionBar = getActionBar();
        int displayOptions = ActionBar.DISPLAY_SHOW_HOME;
        actionBar.setDisplayOptions(0, displayOptions);
        displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM;
        actionBar.setDisplayOptions(displayOptions, displayOptions);
        actionBar.setCustomView(R.layout.trim_menu);

        /// M: modify mSaveVideoTextView to private
        mSaveVideoTextView = (TextView) findViewById(R.id.start_trim);
        mSaveVideoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                /// M: modify mSaveVideoTextView to can not click @{
                mSaveVideoTextView.setClickable(false);
                mSaveVideoTextView.setEnabled(false);
                mIsSaving = true;
                /// @}
                MtkLog.v(TAG , "mSaveVideoTextView onclick");
                trimVideo();
            }
        });

        Intent intent = getIntent();
        mUri = intent.getData();
        mSrcVideoPath = intent.getStringExtra(PhotoPage.KEY_MEDIA_ITEM_PATH);
        setContentView(R.layout.trim_view);
        View rootView = findViewById(R.id.trim_view_root);

        mVideoView = (MTKVideoView) rootView.findViewById(R.id.surface_view);

        mController = new TrimControllerOverlay(mContext);
        ((ViewGroup) rootView).addView(mController.getView());
        mController.setListener(this);
        mController.setCanReplay(true);

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoURI(mUri, null, true);

        playVideo();
    }

    @Override
    public void onResume() {
        super.onResume();
        MtkLog.v(TAG , "onResume()");
        mDragging = false;//clear drag info
        if (mHasPaused) {
            // / M: Modified to avoid video location
            // incorrect limitation when suspend and
            // wake up in landscape mode@{
            mVideoView.setVisibility(View.VISIBLE);
            // /@
            mVideoView.seekTo(mVideoPosition);
            mVideoView.resume();
            mHasPaused = false;
        }
        mHandler.post(mProgressChecker);

        ///M: if mPlayTrimVideo is true, it need show toast 
        /// and play trim video @{
        if(mIsSaving) {
            if(mProgress == null) {
                showProgressDialog();
            }
            mSaveVideoTextView.setClickable(false);
            mSaveVideoTextView.setEnabled(false);
            if(mPlayTrimVideo) {
                mHandler.post(mStartVideoRunnable);
            }
        }
        /// }@
    }

    @Override
    public void onPause() {
        MtkLog.v(TAG , "onPause()");
        mHasPaused = true;
        mHandler.removeCallbacksAndMessages(null);
        mVideoPosition = mVideoView.getCurrentPosition();
        mVideoView.suspend();
        // / M: Modified to avoid video location
        // incorrect limitation when suspend and
        // wake up in landscape mode@{
        if (!isFinishing()) {
            mVideoView.setVisibility(View.INVISIBLE);
        }
        // /@
        super.onPause();
    }

    @Override
    public void onStop() {
        MtkLog.v(TAG , "onStop()");
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        MtkLog.v(TAG , "onDestroy()");
        mVideoView.stopPlayback();
        super.onDestroy();
    }

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 200 - (pos % 200));
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        MtkLog.v(TAG , "onSaveInstanceState()");
        savedInstanceState.putInt(KEY_TRIM_START, mTrimStartTime);
        savedInstanceState.putInt(KEY_TRIM_END, mTrimEndTime);
        savedInstanceState.putInt(KEY_VIDEO_POSITION, mVideoPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        MtkLog.v(TAG , "onRestoreInstanceState()");
        mTrimStartTime = savedInstanceState.getInt(KEY_TRIM_START, 0);
        mTrimEndTime = savedInstanceState.getInt(KEY_TRIM_END, 0);
        mVideoPosition = savedInstanceState.getInt(KEY_VIDEO_POSITION, 0);
        MtkLog.v(TAG, "mTrimStartTime is " + mTrimStartTime
                + ", mTrimEndTime is " + mTrimEndTime + ", mVideoPosition is "
                + mVideoPosition);
    }

    // This updates the time bar display (if necessary). It is called by
    // mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        MtkLog.v(TAG , "setProgress()");
        mVideoPosition = mVideoView.getCurrentPosition();
        // If the video position is smaller than the starting point of trimming,
        // correct it.
        if (mVideoPosition < mTrimStartTime) {
            mVideoView.seekTo(mTrimStartTime);
            mVideoPosition = mTrimStartTime;
        }
        // If the position is bigger than the end point of trimming, show the
        // replay button and pause.
        if (mVideoPosition >= mTrimEndTime && mTrimEndTime > 0) {
            if (mVideoPosition > mTrimEndTime) {
                mVideoView.seekTo(mTrimEndTime);
                mVideoPosition = mTrimEndTime;
            }
            mController.showEnded();
            mVideoView.pause();
        }

        int duration = mVideoView.getDuration();
        if (duration > 0 && mTrimEndTime == 0) {
            mTrimEndTime = duration;
        }
        mController.setTimes(mVideoPosition, duration, mTrimStartTime, mTrimEndTime);
        return mVideoPosition;
    }

    private void playVideo() {
        MtkLog.v(TAG , "playVideo()");
        mVideoView.start();
        mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        MtkLog.v(TAG , "pauseVideo()");
        mVideoView.pause();
        mController.showPaused();
    }

    // Copy from SaveCopyTask.java in terms of how to handle the destination
    // path and filename : querySource() and getSaveDirectory().
    private interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    private void querySource(String[] projection, ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(mUri, projection, null, null, null);
            if ((cursor != null) && cursor.moveToNext()) {
                MtkLog.v(TAG , "querySource() (cursor != null) && cursor.moveToNext()");
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private File getSaveDirectory() {
        final File[] dir = new File[1];
        querySource(new String[] {
                VideoColumns.DATA }, new ContentResolverQueryCallback() {

            @Override
            public void onCursorResult(Cursor cursor) {
                dir[0] = new File(cursor.getString(0)).getParentFile();
            }
        });
        return dir[0];
    }

    private void trimVideo() {
        MtkLog.v(TAG , "trimVideo() start");
        int delta = mTrimEndTime - mTrimStartTime;
        MtkLog.v(TAG , "delta is " + delta);
        // Considering that we only trim at sync frame, we don't want to trim
        // when the time interval is too short or too close to the origin.
        if (delta < 100 ) {
            MtkLog.v(TAG , "delta < 100");
            Toast.makeText(getApplicationContext(),
                getString(R.string.trim_too_short),
                Toast.LENGTH_SHORT).show();
            /// M: modify mSaveVideoTextView to can click @{
            mSaveVideoTextView.setClickable(true);
            mSaveVideoTextView.setEnabled(true);
            mIsSaving = false;
            /// @}
            return;
        }
        if (Math.abs(mVideoView.getDuration() - delta) < 100) {
            MtkLog.v(TAG , "it will be onBackPressed");
            // If no change has been made, go back
            onBackPressed();
            /// M: modify mSaveVideoTextView to can click @{
            mSaveVideoTextView.setClickable(true);
            mSaveVideoTextView.setEnabled(true);
            mIsSaving = false;
            /// @}
            return;
        }
        // Use the default save directory if the source directory cannot be
        // saved.
        mSaveDirectory = getSaveDirectory();
        if ((mSaveDirectory == null) || !mSaveDirectory.canWrite()) {
            MtkLog.v(TAG , "(mSaveDirectory == null) || !mSaveDirectory.canWrite()");
            mSaveDirectory = new File(Environment.getExternalStorageDirectory(),
                    BucketNames.DOWNLOAD);
            saveFolderName = getString(R.string.folder_download);
        } else {
            saveFolderName = mSaveDirectory.getName();
        }
        MtkLog.v(TAG , "saveFolderName is " + saveFolderName);
        mSaveFileName = new SimpleDateFormat(TIME_STAMP_NAME).format(
                new Date(System.currentTimeMillis()));
        MtkLog.v(TAG , "mSaveFileName is " + mSaveFileName);
        
        mDstFile = new File(mSaveDirectory, mSaveFileName + ".mp4");
        if(mDstFile.exists()){
            MtkLog.v(TAG , "mDstFile name is " + mDstFile.getAbsolutePath());
        }
        
        mSrcFile = new File(mSrcVideoPath);
        if(mSrcFile.exists()){
            MtkLog.v(TAG , "mSrcFile name is " + mSrcFile.getAbsolutePath());
        }
        
        ///M: after check this video can trim,show dialog
        //showProgressDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isTrimSuccessful = TrimVideoUtils.startTrim(mSrcFile, mDstFile, mTrimStartTime, mTrimEndTime, TrimVideo.this);
                    if(!isTrimSuccessful){
                        //to user toast
                        mHandler.removeCallbacks(mShowToastRunnable);
                        mHandler.post(mShowToastRunnable);
                        ///M:
                        mIsSaving = false;
                        return;
                    }
                    // Update the database for adding a new video file.
                    MtkLog.v(TAG, "save trim video succeed!");
                    insertContent(mDstFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // After trimming is done, trigger the UI changed.
                ///M:
                mHandler.post(mStartVideoRunnable);
            }
        }).start();
    }

    private void showProgressDialog() {
        // create a background thread to trim the video.
        // and show the progress.
        mProgress = new ProgressDialog(this);
        mProgress.setTitle(getString(R.string.trimming));
        mProgress.setMessage(getString(R.string.please_wait));
        // TODO: make this cancelable.
        mProgress.setCancelable(false);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
    }
    
    public void showDialogCommand(){
        mHandler.removeCallbacks(mShowDialogRunnable);
        mHandler.post(mShowDialogRunnable);
    }
    
    /**
     * Show toast when the video can't be trimmed
     */
    public void showToast() {
        mHandler.removeCallbacks(mShowToastRunnable);
        mHandler.post(mShowToastRunnable);
    }

    /**
     * Insert the content (saved file) with proper video properties.
     */
    private Uri insertContent(File file) {
        MtkLog.v(TAG , "insertContent()");
        long nowInMs = System.currentTimeMillis();
        long nowInSec = nowInMs / 1000;
        final ContentValues values = new ContentValues(12);
        values.put(Video.Media.TITLE, mSaveFileName);
        values.put(Video.Media.DISPLAY_NAME, file.getName());
        values.put(Video.Media.MIME_TYPE, "video/mp4");
        values.put(Video.Media.DATE_TAKEN, nowInMs);
        values.put(Video.Media.DATE_MODIFIED, nowInSec);
        values.put(Video.Media.DATE_ADDED, nowInSec);
        values.put(Video.Media.DATA, file.getAbsolutePath());
        values.put(Video.Media.SIZE, file.length());
        // Copy the data taken and location info from src.
        String[] projection = new String[] {
                VideoColumns.DATE_TAKEN,
                VideoColumns.LATITUDE,
                VideoColumns.LONGITUDE,
                VideoColumns.RESOLUTION,
        };

        // Copy some info from the source file.
        querySource(projection, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                long timeTaken = cursor.getLong(0);
                if (timeTaken > 0) {
                    values.put(Video.Media.DATE_TAKEN, timeTaken);
                }
                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                // TODO: Change || to && after the default location issue is
                // fixed.
                if ((latitude != 0f) || (longitude != 0f)) {
                    values.put(Video.Media.LATITUDE, latitude);
                    values.put(Video.Media.LONGITUDE, longitude);
                }
                values.put(Video.Media.RESOLUTION, cursor.getString(3));

            }
        });

        return getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        MtkLog.v(TAG , "onSeekStart() mDragging is " + mDragging);
        mDragging = true;
        pauseVideo();
    }

    @Override
    public void onSeekMove(int time) {
        MtkLog.v(TAG , "onSeekMove() seekto time is (" + time + ") mDragging is " + mDragging);
        if (!mDragging) {
            mVideoView.seekTo(time);
        }
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        MtkLog.v(TAG, "onSeekEnd() seekto time is " + time + ", start is "
                + start + ", end is " + end + " mDragging is " + mDragging);
        mDragging = false;
        mVideoView.seekTo(time);
        mTrimStartTime = start;
        mTrimEndTime = end;
        setProgress();
    }

    @Override
    public void onShown() {
    }

    @Override
    public void onHidden() {
    }

    @Override
    public boolean onIsRTSP() {
        return false;
    }

    @Override
    public void onReplay() {
        MtkLog.v(TAG , "onReplay()");
        mVideoView.seekTo(mTrimStartTime);
        playVideo();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        MtkLog.v(TAG , "onCompletion()");
        mController.showEnded();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
    
}
