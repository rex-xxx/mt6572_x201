/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.webkit;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList; // M: for audio focus manager
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * HTML5 support class for Audio.
 *
 * This class runs almost entirely on the WebCore thread. The exception is when
 * accessing the WebView object to determine whether private browsing is
 * enabled.
 */
class HTML5Audio extends Handler
                 implements MediaPlayer.OnBufferingUpdateListener,
                            MediaPlayer.OnCompletionListener,
                            MediaPlayer.OnErrorListener,
                            MediaPlayer.OnPreparedListener,
                            MediaPlayer.OnSeekCompleteListener,
                            AudioManager.OnAudioFocusChangeListener {
    // Logging tag.
    private static final String LOGTAG = "HTML5Audio";

    private MediaPlayer mMediaPlayer;

    // The C++ MediaPlayerPrivateAndroid object.
    private int mNativePointer;
    // The private status of the view that created this player
    private IsPrivateBrowsingEnabledGetter mIsPrivateBrowsingEnabledGetter;

    private static int IDLE        =  0;
    private static int INITIALIZED =  1;
    private static int PREPARED    =  2;
    private static int STARTED     =  4;
    private static int COMPLETE    =  5;
    private static int PAUSED      =  6;
    private static int STOPPED     = -2;
    private static int ERROR       = -1;

    private int mState = IDLE;
    /// M: Store prevState in order to restore audio state when audio focus changed.
    private int mPrevState = mState;

    private String mUrl;
    private boolean mAskToPlay = false;
    private boolean mLoopEnabled = false;
    private boolean mProcessingOnEnd = false;
    private Context mContext;

    // Timer thread -> UI thread
    private static final int TIMEUPDATE = 100;
    /// M: message ID for webcore thread
    private static final int MSG_PAUSE  = 101;

    private static final String COOKIE = "Cookie";
    private static final String HIDE_URL_LOGS = "x-hide-urls-from-log";

    // The spec says the timer should fire every 250 ms or less.
    private static final int TIMEUPDATE_PERIOD = 250;  // ms
    // The timer for timeupate events.
    // See http://www.whatwg.org/specs/web-apps/current-work/#event-media-timeupdate
    private Timer mTimer;
    private final class TimeupdateTask extends TimerTask {
        public void run() {
            HTML5Audio.this.obtainMessage(TIMEUPDATE).sendToTarget();
        }
    }

    // Helper class to determine whether private browsing is enabled in the
    // given WebView. Queries the WebView on the UI thread. Calls to get()
    // block until the data is available.
    private class IsPrivateBrowsingEnabledGetter {
        private boolean mIsReady;
        private boolean mIsPrivateBrowsingEnabled;
        IsPrivateBrowsingEnabledGetter(Looper uiThreadLooper, final WebViewClassic webView) {
            new Handler(uiThreadLooper).post(new Runnable() {
                @Override
                public void run() {
                    synchronized(IsPrivateBrowsingEnabledGetter.this) {
                        mIsPrivateBrowsingEnabled = webView.isPrivateBrowsingEnabled();
                        mIsReady = true;
                        IsPrivateBrowsingEnabledGetter.this.notify();
                    }
                }
            });
        }
        synchronized boolean get() {
            while (!mIsReady) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            return mIsPrivateBrowsingEnabled;
        }
    };

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case TIMEUPDATE: {
                try {
                    if (mState != ERROR && mMediaPlayer.isPlaying()) {
                        int position = mMediaPlayer.getCurrentPosition();
                        nativeOnTimeupdate(position, mNativePointer);
                    }
                } catch (IllegalStateException e) {
                    mState = ERROR;
                }
            }
            break;

            /// M: invoking native function on webcore thread.
            case MSG_PAUSE:
                nativeOnPaused(mNativePointer);
            break;
            /// @}
        }
    }

    // event listeners for MediaPlayer
    // Those are called from the same thread we created the MediaPlayer
    // (i.e. the webviewcore thread here)

    // MediaPlayer.OnBufferingUpdateListener
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        nativeOnBuffering(percent, mNativePointer);
    }

    // MediaPlayer.OnCompletionListener;
    public void onCompletion(MediaPlayer mp) {
        /// M: since we don't handle onError, onCompletion will be invoked even if we don't 
        /// start playback. just stay in IDLE
        if (mState != IDLE) {
            mState = COMPLETE;
        }
        mProcessingOnEnd = true;
        nativeOnEnded(mNativePointer);
        mProcessingOnEnd = false;
        /// M: only loop when playback completes correctly
        if (mLoopEnabled == true && mState == COMPLETE) {
            nativeOnRequestPlay(mNativePointer);
            mLoopEnabled = false;
        }
    }

    // MediaPlayer.OnErrorListener
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mState = ERROR;
        resetMediaPlayer();
        mState = IDLE;
        return false;
    }

    // MediaPlayer.OnPreparedListener
    public void onPrepared(MediaPlayer mp) {
        mState = PREPARED;
        if (mTimer != null) {
            mTimer.schedule(new TimeupdateTask(),
                            TIMEUPDATE_PERIOD, TIMEUPDATE_PERIOD);
        }
        nativeOnPrepared(mp.getDuration(), 0, 0, mNativePointer);
        if (mAskToPlay) {
            mAskToPlay = false;
            play();
        }
    }

    // MediaPlayer.OnSeekCompleteListener
    public void onSeekComplete(MediaPlayer mp) {
        nativeOnTimeupdate(mp.getCurrentPosition(), mNativePointer);
    }


    /**
     * @param nativePtr is the C++ pointer to the MediaPlayerPrivate object.
     */
    public HTML5Audio(WebViewCore webViewCore, int nativePtr) {
        // Save the native ptr
        mNativePointer = nativePtr;
        resetMediaPlayer();
        mContext = webViewCore.getContext();
        mIsPrivateBrowsingEnabledGetter = new IsPrivateBrowsingEnabledGetter(
                webViewCore.getContext().getMainLooper(), webViewCore.getWebViewClassic());
    }

    private void resetMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        } else {
            mMediaPlayer.reset();
        }
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);

        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new Timer();
        mState = IDLE;
    }

    private void setDataSource(String url) {
        mUrl = url;
        try {
            if (mState != IDLE) {
                resetMediaPlayer();
            }
            String cookieValue = CookieManager.getInstance().getCookie(
                    url, mIsPrivateBrowsingEnabledGetter.get());
            Map<String, String> headers = new HashMap<String, String>();

            if (cookieValue != null) {
                headers.put(COOKIE, cookieValue);
            }
            if (mIsPrivateBrowsingEnabledGetter.get()) {
                headers.put(HIDE_URL_LOGS, "true");
            }

            mMediaPlayer.setDataSource(url, headers);
            mState = INITIALIZED;
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            String debugUrl = url.length() > 128 ? url.substring(0, 128) + "..." : url;
            Log.e(LOGTAG, "couldn't load the resource: "+ debugUrl +" exc: " + e);
            resetMediaPlayer();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN:
            // resume playback
            if (mMediaPlayer == null) {
                resetMediaPlayer();
            /// M: Store prevState in order to restore audio state when audio focus changed.
            } else if (mState != ERROR && !mMediaPlayer.isPlaying()
                    && mPrevState == STARTED) {
                mMediaPlayer.start();
                mState = STARTED;
            }
            break;

        case AudioManager.AUDIOFOCUS_LOSS:
            /// M: Store prevState in order to restore audio state when audio focus changed.
            mPrevState = mState;
            // Lost focus for an unbounded amount of time: stop playback.
            if (mState != ERROR && mMediaPlayer.isPlaying()) {
                /// M: assume it's paused, notify native layer that we ended. @{
                /// mMediaPlayer.stop();
                /// mState = STOPPED;
                pause();
                this.sendEmptyMessage(MSG_PAUSE);
                /// @}
            }
            break;

        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            /// M: Store prevState in order to restore audio state when audio focus changed.
            mPrevState = mState;
            // Lost focus for a short time, but we have to stop
            // playback.
            if (mState != ERROR && mMediaPlayer.isPlaying()) pause();
            break;
        }
    }


    private void play() {
        if (mState == COMPLETE && mLoopEnabled == true) {
            // Play it again, Sam
            mMediaPlayer.start();
            mState = STARTED;
            return;
        }

        if (((mState >= ERROR && mState < PREPARED)) && mUrl != null) {
            resetMediaPlayer();
            setDataSource(mUrl);
            mAskToPlay = true;
        }

        if (mState >= PREPARED) {
            /// M: change to audio focus manager @{
            /// AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            /// int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
            ///     AudioManager.AUDIOFOCUS_GAIN);
            AudioFocusManager afm = getAudioFocusManager();
            int result = afm.requestFocus(this);
            /// @}

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mMediaPlayer.start();
                mState = STARTED;
            }
        }
    }

    private void pause() {
        if (mState == STARTED) {
            if (mTimer != null) {
                mTimer.purge();
            }
            mMediaPlayer.pause();
            mState = PAUSED;
        /// M: fix race condition that onPrepared() come after pause is already being invoked @{
        } 
        else if (mAskToPlay) {
            mAskToPlay = false;
        /// @}
        }
    }

    private void seek(int msec) {
        if (mProcessingOnEnd == true && mState == COMPLETE && msec == 0) {
            mLoopEnabled = true;
        }
        if (mState >= PREPARED) {
            mMediaPlayer.seekTo(msec);
        }
    }

    /// M: set volume / mute @{
    private float mVolume = 1;
    private boolean mMuted;
    private void setVolume(float volume) {
        mVolume = volume;
        if (mMuted) {       // override volume with mute
            return;
        }
        mMediaPlayer.setVolume(volume, volume);
    }

    private void setMuted(boolean muted) {
        mMuted = muted;
        float volume = muted ? 0 : mVolume;
        mMediaPlayer.setVolume(volume, volume);
    }
    /// @}

    /**
     * Called only over JNI when WebKit is happy to
     * destroy the media player.
     */
    private void teardown() {
        mMediaPlayer.release();
        mMediaPlayer = null;
        mState = ERROR;
        mNativePointer = 0;

        /// M: abandon focus when finished
        if (sAudioFocusManager != null) {
            getAudioFocusManager().abandonFocus(this);
        }
    }

    private float getMaxTimeSeekable() {
        if (mState >= PREPARED) {
            return mMediaPlayer.getDuration() / 1000.0f;
        } else {
            return 0;
        }
    }

    /// M: implementation of AudioFocusManager @{
    static AudioFocusManager sAudioFocusManager;
    private AudioFocusManager getAudioFocusManager() {
        Log.v("HTML5Audio", "getAudioFocusManager(): " + sAudioFocusManager);
        if (sAudioFocusManager == null) {
            sAudioFocusManager = new AudioFocusManager(mContext);
        }
        return sAudioFocusManager;
    }

    private class AudioFocusManager implements AudioManager.OnAudioFocusChangeListener {
        Context mContext;

        // focus callback list.
        private ArrayList<AudioManager.OnAudioFocusChangeListener> mListeners =
            new ArrayList<AudioManager.OnAudioFocusChangeListener>();
        private int mListenerCount;
        private int mFocusStatus;

        AudioFocusManager(Context context) {
            mContext = context;
        }

        private boolean isFocusing() {
            return mFocusStatus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                   || mFocusStatus == AudioManager.AUDIOFOCUS_GAIN;
        }

        public int requestFocus(AudioManager.OnAudioFocusChangeListener ofcl) {
            Log.v("HTML5Audio", "requestFocus():" + mFocusStatus);
            int result;
            if (mListenerCount == 0 || !isFocusing()) {
                AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                result = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
                mFocusStatus = result;
            }

            if (mFocusStatus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mListeners.add(ofcl);
                mListenerCount++;
            }

            if (isFocusing()) {
                return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            }
            return mFocusStatus;
        }

        public void abandonFocus(AudioManager.OnAudioFocusChangeListener ofcl) {
            Log.v("HTML5Audio ", "abandonFocus");
            if (mListenerCount == 0) {
                return;
            }
            mListeners.remove(ofcl);
            mListenerCount--;

            if (mListenerCount == 0) {
                AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                am.abandonAudioFocus(this);
                mFocusStatus = AudioManager.AUDIOFOCUS_REQUEST_FAILED;
            }
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            // invoke the callback sequencally.
            Log.v("HTML5Audio", "onAudioFocusChange(): " + focusChange);
            mFocusStatus = focusChange;
            if (mListenerCount == 0) {
                return;
            }

            //invoke each callback
            for (AudioManager.OnAudioFocusChangeListener ofcl : mListeners) {
                ofcl.onAudioFocusChange(focusChange);
            }
        }
    }
    /// @}

    private native void nativeOnBuffering(int percent, int nativePointer);
    private native void nativeOnEnded(int nativePointer);
    private native void nativeOnRequestPlay(int nativePointer);
    private native void nativeOnPrepared(int duration, int width, int height, int nativePointer);
    private native void nativeOnTimeupdate(int position, int nativePointer);
    /// M: to make playback displayed correct. Add this API so Java layer could notify native layer.
    private native void nativeOnPaused(int nativePointer);

}
