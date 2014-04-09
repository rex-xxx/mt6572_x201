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

/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.mediatek.oobe.qsg;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.xlog.Xlog;

import java.io.IOException;

public class QuickStartGuideMain extends Activity implements OnCompletionListener, OnPreparedListener,
        SurfaceHolder.Callback {

    private static final String TAG = "QuickStartGuideMain";
    private Button mSkipbtn;
    private Button mOkbtn;
    private Button mNextbtn;
    private Button mPlayAgainbtn;
    private LinearLayout mGroupButtons;
    private LinearLayout mNextButtons;
    private LinearLayout mProgressBar;
    private TextView mTitle;
    private TextView mSummary;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mPreview;
    private SurfaceHolder mHolder;;
    private int mCurrentStep = -1;
    // preload video to memory, avoid black flickr when first starting to play video and
    // last page to play again video.
    private boolean mIsPreload = false;
    // avoid press home key, and back to activiy will see the below preload playing video.
    // when in first start page or last page.
    private boolean mHideVideo = true;
    private int mVideoWidth;
    private int mVideoHeight;
    private String mPath;
    // private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private boolean mIsHolderCreated = false;
    // private ImageView mWallpaper;
    private boolean mIsFirstRun = false;
    private boolean mIsTablet = false;
    private boolean mSetScreen = false;
    private static final int TITLE[] = { 
            R.string.qsg_title2_home_screen, 
            R.string.qsg_title3_choose_widget,
            R.string.qsg_title4_launch_page, 
            R.string.qsg_title5_view_notification, 
            R.string.qsg_title_begin };
    private static final int SUMMARY[] = { 
            R.string.qsg_summary2_home_screen, 
            R.string.qsg_summary3_choose_widget,
            R.string.qsg_summary4_launch_page, 
            R.string.qsg_summary5_view_notification, 
            R.string.qsg_summary_end };

    private String[] mVideoTips = new String[] { 
            "JB_01View_Home_screen.mp4", 
            "JB_02Choose_some_widgets.mp4",
            "JB_03Launch_detail_page.mp4", 
            "JB_04ViewNotifications.mp4" };

    private void isTablet() {
        String deviceInfo = SystemProperties.get("ro.build.characteristics");
        if (deviceInfo.equals("tablet")) {
            mIsTablet = true;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        // TODO Auto-generated method stub
        super.onCreate(icicle);

        isTablet();
        if (!mIsTablet) {
            // Forced to set portrait mode for phone
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        Xlog.d(TAG, "QuickStartGuideMain onCreate ");

        setContentView(R.layout.videoview_layout);

        // when invoke play video, need full screen
        if ((getWindow().getAttributes().flags 
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != WindowManager.LayoutParams.FLAG_FULLSCREEN) {
            Xlog.d(TAG, " fullscreen = false");
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        mIsFirstRun = getIntent().getBooleanExtra("mIsFirstRun", false);
        Xlog.d(TAG, "mIsFirstRun = " + mIsFirstRun);

        mSkipbtn = (Button) findViewById(R.id.skip_btn);
        if (!mIsFirstRun) {
            // if is not first run after OOBE end, we hide skip button.
            mSkipbtn.setVisibility(View.INVISIBLE);
        } else {
            mSkipbtn.setOnClickListener(mSkipListener);
            mSkipbtn.setVisibility(View.VISIBLE);
        }

        mOkbtn = (Button) findViewById(R.id.ok_btn);
        mOkbtn.setOnClickListener(mStartListener);
        mGroupButtons = (LinearLayout) findViewById(R.id.group_buttons);
        mNextButtons = (LinearLayout) findViewById(R.id.nextbutton_layout);

        mNextbtn = (Button) findViewById(R.id.next_btn);
        mNextbtn.setOnClickListener(mNextListener);
        mPlayAgainbtn = (Button) findViewById(R.id.play_again_button);
        mPlayAgainbtn.setOnClickListener(mPlayAgainListener);

        mTitle = (TextView) findViewById(R.id.quickstartguide_title);
        mSummary = (TextView) findViewById(R.id.quickstartguide_summary);
        // mWallpaper = (ImageView) findViewById(R.id.wallpaper);

        mProgressBar = (LinearLayout) findViewById(R.id.progressbar_layout);

        mPreview = (SurfaceView) findViewById(R.id.surface);
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mMediaPlayer = new MediaPlayer();

        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(mErrorListener);

    }

    private OnClickListener mSkipListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finishGuickStartGuide();
        }
    };
    private OnClickListener mStartListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mHideVideo = false;
            mProgressBar.setVisibility(View.VISIBLE);
            mGroupButtons.setVisibility(View.GONE);
            mNextButtons.setVisibility(View.VISIBLE);
            updateProgress(mCurrentStep);
            hideTitleSummary();
            if (mMediaPlayer != null && mIsPreload) {
                mMediaPlayer.seekTo(0);

                if (mIsVideoReadyToBePlayed) {
                    startVideoPlayback();
                }

            }

        }

    };
    private OnClickListener mNextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // todo: play next video
            if (mNextbtn.getText().equals(getResources().getString(R.string.finish))) {
                Xlog.d(TAG, "finishGuickStartGuide");
                finishGuickStartGuide();
                return;
            }

            if (mCurrentStep == mVideoTips.length - 1) {
                Xlog.d(TAG, "go to last QSG page");
                mIsPreload = false;
                playVideo(mCurrentStep);

                updateTitleSummary(mVideoTips.length);
                mNextbtn.setText(R.string.finish);
                mPlayAgainbtn.setVisibility(View.VISIBLE);
                mPreview.setBackgroundResource(R.drawable.wallpaper);
                mHideVideo = true;
                mProgressBar.setVisibility(View.GONE);
                mCurrentStep++;
            } else {
                mCurrentStep++;
                hideTitleSummary();
                playVideo(mCurrentStep);
            }
        }
    };
    private OnClickListener mPlayAgainListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // todo: play again video from start
            if (mCurrentStep == mVideoTips.length) {
                mCurrentStep = 0;
                mHideVideo = false;
                mProgressBar.setVisibility(View.VISIBLE);
                resetProgressBar();
                mNextbtn.setText(R.string.next);
            }
            hideTitleSummary();
            playVideo(mCurrentStep);
        }
    };

    private OnErrorListener mErrorListener = new OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            // TODO Auto-generated method stub
            Xlog.d(TAG, "play error: " + what);
            releaseMediaPlayer();
            return false;
        }
    };

    private void playVideo(int step) {
        Xlog.d(TAG, "playVideo step=" + step);

        if (mIsPreload) {
            doCleanUp();
            updateProgress(step);
        }
        try {
            if (step >= 0 && step <= mVideoTips.length - 1) {
                mPath = mVideoTips[step];
            }

            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                AssetFileDescriptor afd = getAssets().openFd(mPath);
                mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mMediaPlayer.prepare();
                resizeSurfaceView();
            }

        } catch (IOException e) {
            Xlog.e(TAG, "unable to open file; error: " + e.getMessage(), e);
            mMediaPlayer.release();
            mMediaPlayer = null;
        } catch (IllegalStateException e) {
            Xlog.e(TAG, "media player is in illegal state; error: " + e.getMessage(), e);
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    protected void resetProgressBar() {
        ImageView image;
        for (int i = 0; i < mProgressBar.getChildCount(); i++) {
            image = (ImageView) mProgressBar.getChildAt(i);
            image.setImageResource(R.drawable.progress_radio_off);
            if (i == 0) {
                image.setImageResource(R.drawable.progress_radio_on);
            }
        }
    }

    protected void updateProgress(int step) {
        ImageView image;
        for (int i = 0; i < mVideoTips.length; i++) {
            image = (ImageView) mProgressBar.getChildAt(i);
            image.setImageResource(R.drawable.progress_radio_off);
            if (i == step) {
                image.setImageResource(R.drawable.progress_radio_on);
            }
            image.setVisibility(View.VISIBLE);
        }
    }

    protected void updateTitleSummary(int index) {
        mTitle.setText(TITLE[index]);
        mSummary.setText(SUMMARY[index]);
    }

    protected void hideTitleSummary() {
        mTitle.setText("");
        mSummary.setText("");
        mPlayAgainbtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCompletion(MediaPlayer arg0) {
        Xlog.d(TAG, "onCompletion called");
        if (!mHideVideo) {
            updateTitleSummary(mCurrentStep);
            mPlayAgainbtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaplayer) {
        Xlog.d(TAG, "onPrepared called");
        mIsVideoReadyToBePlayed = true;
        if (!mIsPreload && mIsHolderCreated) {
            Xlog.d(TAG, "first prepare video...");
            mIsPreload = true;
            mMediaPlayer.start();
            return;
        }
        if (mIsVideoReadyToBePlayed) {
            startVideoPlayback();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Xlog.d(TAG, "surfaceChanged called");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Xlog.d(TAG, "surfaceDestroyed called");
        mIsHolderCreated = false;
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Xlog.d(TAG, "surfaceCreated called");
        mIsHolderCreated = true;
        // Create a new media player and set the listeners
        if (mMediaPlayer != null) {
            this.mHolder = holder;
            mMediaPlayer.setDisplay(mHolder);
            if (mCurrentStep == -1) {
                mCurrentStep = 0;
                playVideo(mCurrentStep);
            } else {
                Xlog.d(TAG, "surfaceCreated play video");
                if (!mHideVideo) {
                    hideTitleSummary();
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();
                }
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Xlog.d(TAG, "onResume called: mHideVideo=" + mHideVideo);

        if (mMediaPlayer != null && mCurrentStep != -1 && !mHideVideo && mIsHolderCreated) {
            hideTitleSummary();
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Xlog.d(TAG, "onPause called");
        // releaseMediaPlayer();
        // doCleanUp();
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        doCleanUp();
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void doCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        // mIsVideoSizeKnown = false;
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void startVideoPlayback() {
        Xlog.v(TAG, "startVideoPlayback");
        // mHolder.setFixedSize(mVideoWidth, mVideoHeight);
        if (!mIsHolderCreated) {
            return;
        }
        mMediaPlayer.start();

        if (mMediaPlayer.isPlaying()) {
            Xlog.d(TAG, "QSG video is playing ");
            mPreview.setBackgroundColor(android.R.color.transparent);
        }
    }

    private void finishGuickStartGuide() {
        Xlog.v(TAG, "finish quick start guide");
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            Xlog.d(TAG, "video is playing ");
        }
        // if we are the first run of OOBE invoke quick start guide, we launch home when first time
        if (mIsFirstRun) {
            // Launcher intent
            //Intent homeIntent = new Intent("android.intent.action.MAIN");
            //homeIntent.addCategory("android.intent.category.HOME");

            // add for telling Launcher whether to load tips
            Settings.System.putInt(getContentResolver(), Settings.System.SHOW_QSG, 1);
            Xlog.w(TAG, "OOBE begin to start launcher with flag stored in settings providers");

            // start Launcher now
            //homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(homeIntent);
        }

        Xlog.v(TAG, "setResult code " + OOBEConstants.RESULT_QUICK_START_GUIDE_FINISH);
        // Intent intent = new Intent();
        // setResult(OOBEConstants.RESULT_QUICK_START_GUIDE_FINISH, intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            Xlog.i(TAG, "Press back button to former settings");
            Xlog.i(TAG, "Is first started?" + mIsFirstRun);

            if ((mIsFirstRun)) {
                return true;// prevent default behavior
            } else {
                // when back key pressed, just quit quick start guide.
                finishGuickStartGuide();
            }
            break;
        default:
            break;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void resizeSurfaceView() {
        Xlog.d(TAG, "resizeSurfaceView()");
        if (mSetScreen) {
            return;
        } else {
            mSetScreen = true;
        }
        int videoW= mMediaPlayer.getVideoWidth();
        int videoH = mMediaPlayer.getVideoHeight();
        int screenW = getWindowManager().getDefaultDisplay().getWidth();
        int screenH = getWindowManager().getDefaultDisplay().getHeight();
        android.view.ViewGroup.LayoutParams lp = mPreview.getLayoutParams();

        float videoScale = (float)videoH / (float)videoW;
        float screenScale = (float)screenH / (float)screenW;
        if (screenScale > videoScale) {
            lp.width = screenW;
            lp.height = (int)(videoScale * (float)screenW);
            Xlog.d(TAG, "screenScale > videoScale");
        } else {
            lp.height = screenH;
            lp.width = (int)((float)screenH / videoScale);
            Xlog.d(TAG, "screenScale < videoScale");
        }
        mPreview.setLayoutParams(lp);
    }
}
