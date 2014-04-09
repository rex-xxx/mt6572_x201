/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.voicemail;

import static android.util.MathUtils.constrain;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.android.contacts.R;
import com.android.contacts.util.AsyncTaskExecutor;
import com.android.ex.variablespeed.MediaPlayerProxy;
import com.android.ex.variablespeed.SingleThreadedMediaPlayerProxy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Contains the controlling logic for a voicemail playback ui.
 * <p>
 * Specifically right now this class is used to control the
 * {@link com.android.contacts.voicemail.VoicemailPlaybackFragment}.
 * <p>
 * This class is not thread safe. The thread policy for this class is
 * thread-confinement, all calls into this class from outside must be done from
 * the main ui thread.
 */
@NotThreadSafe
@VisibleForTesting
public class VoicemailPlaybackPresenter {
    private static final String TAG = "VoicemailPlaybackPresenter";
    /** The stream used to playback voicemail. */
    private static final int PLAYBACK_STREAM = AudioManager.STREAM_VOICE_CALL;

    /** Contract describing the behaviour we need from the ui we are controlling. */
    public interface PlaybackView {
        Context getDataSourceContext();
        void runOnUiThread(Runnable runnable);
        void setStartStopListener(View.OnClickListener listener);
        void setPositionSeekListener(SeekBar.OnSeekBarChangeListener listener);
        void setSpeakerphoneListener(View.OnClickListener listener);
        void setIsBuffering();
        void setClipPosition(int clipPositionInMillis, int clipLengthInMillis);
        int getDesiredClipPosition();
        void playbackStarted();
        void playbackStopped();
        void playbackError(Exception e);
        boolean isSpeakerPhoneOn();
        void setSpeakerPhoneOn(boolean on);
        void finish();
        void setRateDisplay(float rate, int stringResourceId);
        void setRateIncreaseButtonListener(View.OnClickListener listener);
        void setRateDecreaseButtonListener(View.OnClickListener listener);
        void setIsFetchingContent();
        void disableUiElements();
        void enableUiElements();
        void sendFetchVoicemailRequest(Uri voicemailUri);
        boolean queryHasContent(Uri voicemailUri);
        void setFetchContentTimeout();
        void registerContentObserver(Uri uri, ContentObserver observer);
        void unregisterContentObserver(ContentObserver observer);
        void enableProximitySensor();
        void disableProximitySensor();
        void setVolumeControlStream(int streamType);
    }

    /** The enumeration of {@link AsyncTask} objects we use in this class. */
    public enum Tasks {
        CHECK_FOR_CONTENT,
        CHECK_CONTENT_AFTER_CHANGE,
        PREPARE_MEDIA_PLAYER,
        RESET_PREPARE_START_MEDIA_PLAYER,
    }

    /**M: modify the fps to 5 to enhance performance @{*/
    /** Update rate for the slider, 5fps. */
    private static final int SLIDER_UPDATE_PERIOD_MILLIS = 1000 / 5;
    /**@}*/
    /** Time our ui will wait for content to be fetched before reporting not available. */
    private static final long FETCH_CONTENT_TIMEOUT_MS = 20000;
    /**
     * If present in the saved instance bundle, we should not resume playback on
     * create.
     */
    private static final String PAUSED_STATE_KEY = VoicemailPlaybackPresenter.class.getName()
            + ".PAUSED_STATE_KEY";
    /**
     * If present in the saved instance bundle, indicates where to set the
     * playback slider.
     */
    private static final String CLIP_POSITION_KEY = VoicemailPlaybackPresenter.class.getName()
            + ".CLIP_POSITION_KEY";

    /**M: modify the speed rates to three levels @{*/
    /** The preset variable-speed rates.  Each is greater than the previous by 25%. */
    //    private static final float[] PRESET_RATES = new float[] {
    //        0.64f, 0.8f, 1.0f, 1.25f, 1.5625f
    //    };
    private static final float[] PRESET_RATES = new float[] {
        0.8f, 1.0f, 1.25f
    };
    /** The string resource ids corresponding to the names given to the above preset rates. */
    private static final int[] PRESET_NAMES = new int[] {
        // R.string.voicemail_speed_slowest,
        R.string.voicemail_speed_slower,
        R.string.voicemail_speed_normal,
        R.string.voicemail_speed_faster,
        // R.string.voicemail_speed_fastest,
    };
    /**@}*/

    /**
     * Pointer into the {@link VoicemailPlaybackPresenter#PRESET_RATES} array.
     * <p>
     * This doesn't need to be synchronized, it's used only by the {@link RateChangeListener}
     * which in turn is only executed on the ui thread.  This can't be encapsulated inside the
     * rate change listener since multiple rate change listeners must share the same value.
     */
    private int mRateIndex = 2;

    /**
     * The most recently calculated duration.
     * <p>
     * We cache this in a field since we don't want to keep requesting it from the player, as
     * this can easily lead to throwing {@link IllegalStateException} (any time the player is
     * released, it's illegal to ask for the duration).
     */
    private final AtomicInteger mDuration = new AtomicInteger(0);

    private final PlaybackView mView;
    private final MediaPlayerProxy mPlayer;
    private final PositionUpdater mPositionUpdater;

    /** Voicemail uri to play. */
    private final Uri mVoicemailUri;
    /** Start playing in onCreate iff this is true. */
    private final boolean mStartPlayingImmediately;
    /** Used to run async tasks that need to interact with the ui. */
    private final AsyncTaskExecutor mAsyncTaskExecutor;

    /**
     * Used to handle the result of a successful or time-out fetch result.
     * <p>
     * This variable is thread-contained, accessed only on the ui thread.
     */
    private FetchResultHandler mFetchResultHandler;
    private PowerManager.WakeLock mWakeLock;
    private AsyncTask<Void, ?, ?> mPrepareTask;

    private VoicemailPlaybackPresenter(PlaybackView view, MediaPlayerProxy player,
            Uri voicemailUri, ScheduledExecutorService executorService,
            boolean startPlayingImmediately, AsyncTaskExecutor asyncTaskExecutor,
            PowerManager.WakeLock wakeLock) {
        mView = view;
        mPlayer = player;
        mVoicemailUri = voicemailUri;
        mStartPlayingImmediately = startPlayingImmediately;
        mAsyncTaskExecutor = asyncTaskExecutor;
        mPositionUpdater = new PositionUpdater(executorService, SLIDER_UPDATE_PERIOD_MILLIS);
        mWakeLock = wakeLock;
        /**M: add to control the audio focus */
        mAudioManager = (AudioManager)mView.getDataSourceContext().getSystemService(Context.AUDIO_SERVICE);
        /**M: add to catch the headset plug/unplugs action */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        mView.getDataSourceContext().registerReceiver(mHeadsetPlugReceiver, intentFilter);
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(mView.getDataSourceContext(),
                mBluetoothHeadsetServiceListener, BluetoothProfile.HEADSET);
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(mView.getDataSourceContext(),
                mBluetoothA2dpServiceListener, BluetoothProfile.A2DP);
    }

    private static WeakReference<VoicemailPlaybackPresenter> sPresenterReference;

    public static VoicemailPlaybackPresenter getInstance(PlaybackView view, MediaPlayerProxy player, Uri voicemailUri,
            ScheduledExecutorService executorService, boolean startPlayingImmediately, AsyncTaskExecutor asyncTaskExecutor,
            PowerManager.WakeLock wakeLock) {
        Log.i(TAG, "getInstance, newView = " + view);
        Log.i(TAG, "getInstance, newPlayer = " + player);
        if (sPresenterReference != null && sPresenterReference.get() != null) {
            VoicemailPlaybackPresenter presenter = sPresenterReference.get();
            Log.i(TAG, "getInstance, oldView = " + presenter.mView);
            Log.i(TAG, "getInstance, oldPlayer = " + presenter.mPlayer);
            presenter.mIsPresenterDestroied = true;
            presenter.mPlayer.release();
            presenter.mView.finish();
        } else {
            Log.i(TAG, "getInstance, oldPresenter is null");
        }
        sPresenterReference = new WeakReference<VoicemailPlaybackPresenter>(new VoicemailPlaybackPresenter(view, player,
                voicemailUri, executorService, startPlayingImmediately, asyncTaskExecutor, wakeLock));
        return sPresenterReference.get();
    }

    public void onCreate(Bundle bundle) {
        Log.i(TAG, "[onCreate], mView =" + mView);
        mView.setVolumeControlStream(PLAYBACK_STREAM);
        checkThatWeHaveContent();
    }

    /**
     * Checks to see if we have content available for this voicemail.
     * <p>
     * This method will be called once, after the fragment has been created, before we know if the
     * voicemail we've been asked to play has any content available.
     * <p>
     * This method will notify the user through the ui that we are fetching the content, then check
     * to see if the content field in the db is set. If set, we proceed to
     * {@link #postSuccessfullyFetchedContent()} method. If not set, we will make a request to fetch
     * the content asynchronously via {@link #makeRequestForContent()}.
     */
    private void checkThatWeHaveContent() {
        mView.setIsFetchingContent();
        mAsyncTaskExecutor.submit(Tasks.CHECK_FOR_CONTENT, new AsyncTask<Void, Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void... params) {
                return mView.queryHasContent(mVoicemailUri);
            }

            @Override
            public void onPostExecute(Boolean hasContent) {
                if (mIsPresenterDestroied) {
                    //when mPlayer is reset in the background, main thread may release it.
                    Log.w(TAG, "[checkThatWeHaveContent] onPostExecute: mIsDestoryed = " + mIsPresenterDestroied);
                    return;
                }
                if (hasContent) {
                    postSuccessfullyFetchedContent();
                } else {
                    makeRequestForContent();
                }
            }
        });
    }

    /**
     * Makes a broadcast request to ask that a voicemail source fetch this content.
     * <p>
     * This method <b>must be called on the ui thread</b>.
     * <p>
     * This method will be called when we realise that we don't have content for this voicemail. It
     * will trigger a broadcast to request that the content be downloaded. It will add a listener to
     * the content resolver so that it will be notified when the has_content field changes. It will
     * also set a timer. If the has_content field changes to true within the allowed time, we will
     * proceed to {@link #postSuccessfullyFetchedContent()}. If the has_content field does not
     * become true within the allowed time, we will update the ui to reflect the fact that content
     * was not available.
     */
    private void makeRequestForContent() {
        Handler handler = new Handler();
        Preconditions.checkState(mFetchResultHandler == null, "mFetchResultHandler should be null");
        mFetchResultHandler = new FetchResultHandler(handler);
        mView.registerContentObserver(mVoicemailUri, mFetchResultHandler);
        handler.postDelayed(mFetchResultHandler.getTimeoutRunnable(), FETCH_CONTENT_TIMEOUT_MS);
        mView.sendFetchVoicemailRequest(mVoicemailUri);
    }

    @ThreadSafe
    private class FetchResultHandler extends ContentObserver implements Runnable {
        private AtomicBoolean mResultStillPending = new AtomicBoolean(true);
        private final Handler mHandler;

        public FetchResultHandler(Handler handler) {
            super(handler);
            mHandler = handler;
        }

        public Runnable getTimeoutRunnable() {
            return this;
        }

        @Override
        public void run() {
            if (mResultStillPending.getAndSet(false)) {
                mView.unregisterContentObserver(FetchResultHandler.this);
                mView.setFetchContentTimeout();
            }
        }

        public void destroy() {
            if (mResultStillPending.getAndSet(false)) {
                mView.unregisterContentObserver(FetchResultHandler.this);
                mHandler.removeCallbacks(this);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            mAsyncTaskExecutor.submit(Tasks.CHECK_CONTENT_AFTER_CHANGE,
                    new AsyncTask<Void, Void, Boolean>() {
                @Override
                public Boolean doInBackground(Void... params) {
                    return mView.queryHasContent(mVoicemailUri);
                }

                @Override
                public void onPostExecute(Boolean hasContent) {
                    if (hasContent) {
                        if (mResultStillPending.getAndSet(false)) {
                            mView.unregisterContentObserver(FetchResultHandler.this);
                            postSuccessfullyFetchedContent();
                        }
                    }
                }
            });
        }
    }

    /**
     * Prepares the voicemail content for playback.
     * <p>
     * This method will be called once we know that our voicemail has content (according to the
     * content provider). This method will try to prepare the data source through the media player.
     * If preparing the media player works, we will call through to
     * {@link #postSuccessfulPrepareActions()}. If preparing the media player fails (perhaps the
     * file the content provider points to is actually missing, perhaps it is of an unknown file
     * format that we can't play, who knows) then we will show an error on the ui.
     */
    private void postSuccessfullyFetchedContent() {
        mView.setIsBuffering();
        mAsyncTaskExecutor.submit(Tasks.PREPARE_MEDIA_PLAYER,
                new AsyncTask<Void, Void, Exception>() {
                    @Override
                    public Exception doInBackground(Void... params) {
                        try {
                            mPlayer.reset();
                            mPlayer.setDataSource(mView.getDataSourceContext(), mVoicemailUri);
                            mPlayer.setAudioStreamType(PLAYBACK_STREAM);
                            mPlayer.prepare();
                            return null;
                        } catch (Exception e) {
                            return e;
                        }
                    }

                    @Override
                    public void onPostExecute(Exception exception) {
                        if (mIsPresenterDestroied) {
                            //when mPlayer is reset in the background, main thread may release it.
                            Log.w(TAG, "[postSuccessfullyFetchedContent] onPostExecute: mIsDestoryed = " + mIsPresenterDestroied);
                            return;
                        }
                        if (exception == null) {
                            postSuccessfulPrepareActions();
                        } else {
                            mView.playbackError(exception);
                        }
                    }
                });
    }

    /**
     * Enables the ui, and optionally starts playback immediately.
     * <p>
     * This will be called once we have successfully prepared the media player, and will optionally
     * playback immediately.
     */
    private void postSuccessfulPrepareActions() {
        mView.enableUiElements();
        mView.setPositionSeekListener(new PlaybackPositionListener());
        mView.setStartStopListener(new StartStopButtonListener());
        mView.setSpeakerphoneListener(new SpeakerphoneListener());
        mPlayer.setOnErrorListener(new MediaPlayerErrorListener());
        mPlayer.setOnCompletionListener(new MediaPlayerCompletionListener());
        mView.setSpeakerPhoneOn(mView.isSpeakerPhoneOn());
        /**M: set default value as speaker it was */
        mIsCurrentSpeakerOn = mView.isSpeakerPhoneOn();
        mIsUserSetSpeakerOn = mIsCurrentSpeakerOn;
        mView.setRateDecreaseButtonListener(createRateDecreaseListener());
        mView.setRateIncreaseButtonListener(createRateIncreaseListener());
        mView.setClipPosition(0, mPlayer.getDuration());
        mView.playbackStopped();
        // Always disable on stop.
        mView.disableProximitySensor();
        if (mStartPlayingImmediately) {
            resetPrepareStartPlaying(0);
        }
        // TODO: Now I'm ignoring the bundle, when previously I was checking for contains against
        // the PAUSED_STATE_KEY, and CLIP_POSITION_KEY.
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(CLIP_POSITION_KEY, mView.getDesiredClipPosition());
        if (!mPlayer.isPlaying()) {
            outState.putBoolean(PAUSED_STATE_KEY, true);
        }
    }

    public void onDestroy() {
        Log.i(TAG, "[onDestroy], mView = " + mView);
        mIsPresenterDestroied = true;
        /**M: to mark the presenter status*/
        /**M: to release the audio focus */
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        /**M: to unregister the headset plug/unplugs receiver */
        mPlayer.release();
        mView.getDataSourceContext().unregisterReceiver(mHeadsetPlugReceiver);
        BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2dp);
        BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
        if (mFetchResultHandler != null) {
            mFetchResultHandler.destroy();
            mFetchResultHandler = null;
        }
        mPositionUpdater.stopUpdating();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private class MediaPlayerErrorListener implements MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mView.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleError(new IllegalStateException("MediaPlayer error listener invoked"));
                }
            });
            return true;
        }
    }

    private class MediaPlayerCompletionListener implements MediaPlayer.OnCompletionListener {
        @Override
        public void onCompletion(final MediaPlayer mp) {
            mView.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleCompletion(mp);
                }
            });
        }
    }

    public View.OnClickListener createRateDecreaseListener() {
        return new RateChangeListener(false);
    }

    public View.OnClickListener createRateIncreaseListener() {
        return new RateChangeListener(true);
    }

    /**
     * Listens to clicks on the rate increase and decrease buttons.
     * <p>
     * This class is not thread-safe, but all interactions with it will happen on the ui thread.
     */
    private class RateChangeListener implements View.OnClickListener {
        private final boolean mIncrease;

        public RateChangeListener(boolean increase) {
            mIncrease = increase;
        }

        @Override
        public void onClick(View v) {
            // Adjust the current rate, then clamp it to the allowed values.
            mRateIndex = constrain(mRateIndex + (mIncrease ? 1 : -1), 0, PRESET_RATES.length - 1);
            // Whether or not we have actually changed the index, call changeRate().
            // This will ensure that we show the "fastest" or "slowest" text on the ui to indicate
            // to the user that it doesn't get any faster or slower.
            changeRate(PRESET_RATES[mRateIndex], PRESET_NAMES[mRateIndex]);
        }
    }

    private void resetPrepareStartPlaying(final int clipPositionInMillis) {
        if (mPrepareTask != null) {
            mPrepareTask.cancel(false);
        }
        mPrepareTask = mAsyncTaskExecutor.submit(Tasks.RESET_PREPARE_START_MEDIA_PLAYER,
                new AsyncTask<Void, Void, Exception>() {
                    @Override
                    public Exception doInBackground(Void... params) {
                        try {
                            mPlayer.reset();
                            mPlayer.setDataSource(mView.getDataSourceContext(), mVoicemailUri);
                            mPlayer.setAudioStreamType(PLAYBACK_STREAM);
                            mPlayer.prepare();
                            /**M: to mark the player status*/
                            Log.i(TAG, "[resetPrepareStartPlaying] reseted in background, mplayer = " + mPlayer);
                            return null;
                        } catch (Exception e) {
                            return e;
                        }
                    }

                    @Override
                    public void onPostExecute(Exception exception) {
                        Log.i(TAG, "[resetPrepareStartPlaying] onPostExecute, mplayer = " + mPlayer);
                        if (mIsPresenterDestroied) {
                            //when mPlayer is reset in the background, main thread may release it.
                            Log.w(TAG, "[resetPrepareStartPlaying] onPostExecute: mIsDestoryed = " + mIsPresenterDestroied);
                            return;
                        }
                        mPrepareTask = null;
                        if (exception == null) {
                            /**M: to request the audio focus */
                            if (AudioManager.AUDIOFOCUS_REQUEST_FAILED == mAudioManager.requestAudioFocus(
                                    mAudioFocusListener, PLAYBACK_STREAM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)) {
                                Log.e(TAG, "AudioFocus request failed!");
                                return;
                            }
                            mDuration.set(mPlayer.getDuration());
                            int startPosition =
                                    constrain(clipPositionInMillis, 0, mDuration.get());
                            mView.setClipPosition(startPosition, mDuration.get());
                            /**M: set SpeakerStatus to the User selected */
                            mView.setSpeakerPhoneOn(mIsCurrentSpeakerOn);
                            mPlayer.seekTo(startPosition);
                            mPlayer.start();
                            mView.playbackStarted();
                            if (!mWakeLock.isHeld()) {
                                mWakeLock.acquire();
                            }
                            // Only enable if we are not currently using the speaker phone.
                            /**M: when headset inserted, should not enable sensor */
                            if (!mView.isSpeakerPhoneOn() && !mIsHeadsetPlugin && !isBluetoothAvailable()) {
                                mView.enableProximitySensor();
                            }
                            mPositionUpdater.startUpdating(startPosition, mDuration.get());
                        } else {
                            handleError(exception);
                        }
                    }
                });
    }

    private void handleError(Exception e) {
        /**M: to release the audio focus */
        Log.i(TAG, "[handleError]");
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mView.playbackError(e);
        mPositionUpdater.stopUpdating();
        mPlayer.release();
    }

    public void handleCompletion(MediaPlayer mediaPlayer) {
        /**M: to release the audio focus */
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        stopPlaybackAtPosition(0, mDuration.get());
    }

    private void stopPlaybackAtPosition(int clipPosition, int duration) {
        mPositionUpdater.stopUpdating();
        mView.playbackStopped();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        // Always disable on stop.
        mView.disableProximitySensor();
        mView.setClipPosition(clipPosition, duration);
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    private class PlaybackPositionListener implements SeekBar.OnSeekBarChangeListener {
        private boolean mShouldResumePlaybackAfterSeeking;

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            if (mPlayer.isPlaying()) {
                mShouldResumePlaybackAfterSeeking = true;
                stopPlaybackAtPosition(mPlayer.getCurrentPosition(), mDuration.get());
            } else {
                mShouldResumePlaybackAfterSeeking = false;
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            if (mPlayer.isPlaying()) {
                stopPlaybackAtPosition(mPlayer.getCurrentPosition(), mDuration.get());
            }
            if (mShouldResumePlaybackAfterSeeking) {
                resetPrepareStartPlaying(mView.getDesiredClipPosition());
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mView.setClipPosition(seekBar.getProgress(), seekBar.getMax());
        }
    }

    private void changeRate(float rate, int stringResourceId) {
        /**M: when change rate, firstly judge the player released or not @{ */
        if (mIsPresenterDestroied) {
            //when mPlayer is reset in the background, main thread may release it.
            Log.w(TAG, "[changeRate] onPostExecute: mIsDestoryed = " + mIsPresenterDestroied);
            return;
        }
        /**@}*/
        ((SingleThreadedMediaPlayerProxy) mPlayer).setVariableSpeed(rate);
        mView.setRateDisplay(rate, stringResourceId);
    }

    private class SpeakerphoneListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            boolean previousState = mView.isSpeakerPhoneOn();
            mView.setSpeakerPhoneOn(!previousState);
            /**M: mIsUserSetSpeakerOn to record the setting*/
            mIsUserSetSpeakerOn = !previousState;
            mIsCurrentSpeakerOn = mIsUserSetSpeakerOn;
            /**M: when headset inserted, should not enable sensor */
            if (mPlayer.isPlaying() && previousState && !mIsHeadsetPlugin && !isBluetoothAvailable()) {
                // If we are currently playing and we are disabling the speaker phone, enable the
                // sensor.
                mView.enableProximitySensor();
            } else {
                // If we are not currently playing, disable the sensor.
                mView.disableProximitySensor();
            }
        }
    }

    private class StartStopButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            if (mPlayer.isPlaying()) {
                stopPlaybackAtPosition(mPlayer.getCurrentPosition(), mDuration.get());
            } else {
                resetPrepareStartPlaying(mView.getDesiredClipPosition());
            }
        }
    }

    /**
     * Controls the animation of the playback slider.
     */
    @ThreadSafe
    private final class PositionUpdater implements Runnable {
        private final ScheduledExecutorService mExecutorService;
        private final int mPeriodMillis;
        private final Object mLock = new Object();
        @GuardedBy("mLock") private ScheduledFuture<?> mScheduledFuture;
        private final Runnable mSetClipPostitionRunnable = new Runnable() {
            @Override
            public void run() {
                int currentPosition = 0;
                synchronized (mLock) {
                    if (mScheduledFuture == null) {
                        // This task has been canceled. Just stop now.
                        return;
                    }
                    currentPosition = mPlayer.getCurrentPosition();
                }
                /** M: When it is playing , the position should not update to ZERO @{ */
                if (mPlayer.isPlaying() && currentPosition == 0) {
                    Log.d(TAG, "PositionUpdater mView =" + mView.getDesiredClipPosition());
                    return;
                }
                /** @} */
                mView.setClipPosition(currentPosition, mDuration.get());
            }
        };

        public PositionUpdater(ScheduledExecutorService executorService, int periodMillis) {
            mExecutorService = executorService;
            mPeriodMillis = periodMillis;
        }

        @Override
        public void run() {
            mView.runOnUiThread(mSetClipPostitionRunnable);
        }

        public void startUpdating(int beginPosition, int endPosition) {
            synchronized (mLock) {
                if (mScheduledFuture != null) {
                    mScheduledFuture.cancel(false);
                }
                mScheduledFuture = mExecutorService.scheduleAtFixedRate(this, 0, mPeriodMillis,
                        TimeUnit.MILLISECONDS);
            }
        }

        public void stopUpdating() {
            synchronized (mLock) {
                if (mScheduledFuture != null) {
                    mScheduledFuture.cancel(false);
                    mScheduledFuture = null;
                }
            }
        }
    }

    public void onPause() {
        /**M: to release the audio focus */
        Log.i(TAG, "[onPause]");
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        if (mPlayer.isPlaying()) {
            stopPlaybackAtPosition(mPlayer.getCurrentPosition(), mDuration.get());
        }
        /**M: release mediaplayer resource @ {*/
        mPositionUpdater.stopUpdating();
        // mPlayer.release();
        // mIsPlayerReleased = true;
        /** @} */
        if (mPrepareTask != null) {
            mPrepareTask.cancel(false);
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    /**M: add to control the audio focus @ { */
    private AudioManager mAudioManager;
    //use to record the user setting, can only be valued in onClick or initial.
    private boolean mIsUserSetSpeakerOn;
    //speaker on/off may be changed by other Apps, use this field to record.
    private boolean mIsCurrentSpeakerOn;
    public boolean mIsPresenterDestroied;
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.i(TAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                // onPause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                Log.i(TAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                // if (!mPlayer.isPlaying()) {
                //     resetPrepareStartPlaying(mView.getDesiredClipPosition());
                // }
                break;
            default:
                Log.e(TAG, "Unknown audio focus change code");
            }
        }
    };

    private boolean mIsHeadsetPlugin;
    private final BroadcastReceiver mHeadsetPlugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                // when headset state changed, reset the speaker status.
                int state = intent.getIntExtra("state", 0);
                mIsHeadsetPlugin = (state == 1) ? true : false ;
                if (mIsHeadsetPlugin) {
                    mView.setSpeakerPhoneOn(false);
                } else if (!mIsHeadsetPlugin && mIsUserSetSpeakerOn) {
                    mView.setSpeakerPhoneOn(true);
                }
                mIsCurrentSpeakerOn = mView.isSpeakerPhoneOn();

                // when headset state changed, reset the sensor enable/disable stauts
                if (mPlayer.isPlaying() && !mIsHeadsetPlugin && !mIsCurrentSpeakerOn) {
                    mView.enableProximitySensor();
                } else {
                    mView.disableProximitySensor();
                }
            } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                Log.d(TAG, "mHeadsetPlugReceiver: HEADSET_ACTION_CONNECTION_STATE_CHANGED, state=" + state);
                if (state == BluetoothHeadset.STATE_CONNECTING || state == BluetoothHeadset.STATE_CONNECTED || isBluetoothAvailable()) {
                    mView.disableProximitySensor();
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED && mPlayer.isPlaying()){
                    mView.enableProximitySensor();
                }
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)){
                int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                Log.d(TAG, "mHeadsetPlugReceiver: HEADSET_AUDIO_STATE_CHANGED_ACTION, state=" + state);
                // if (state != BluetoothHeadset.STATE_AUDIO_DISCONNECTED &&
                //      mPlayer.isPlaying()) {
                //      mView.disableProximitySensor();
                // }
            }
        }
    };

    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothA2dp mBluetoothA2dp;
    private BluetoothProfile.ServiceListener mBluetoothHeadsetServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "mBluetoothHeadsetServiceListener onServiceConnected");
            mBluetoothHeadset = (BluetoothHeadset) proxy;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "mBluetoothHeadsetServiceListener onServiceDisconnected");
            mBluetoothHeadset = null;
        }
    };

    private BluetoothProfile.ServiceListener mBluetoothA2dpServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "mBluetoothA2dpServiceListener onServiceConnected");
            mBluetoothA2dp = (BluetoothA2dp) proxy;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "mBluetoothA2dpServiceListener onServiceDisconnected");
            mBluetoothA2dp = null;
        }
    };

    protected boolean isBluetoothAvailable() {
        Log.d(TAG, "isBluetoothAvailable() entry");
        boolean isConnected = false;
        if (mBluetoothHeadset != null) {
            List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();
            int size = deviceList.size();
            Log.d(TAG, "isBluetoothAvailable() BluetoothHeadset size: " + size);
            if (size > 0) {
                isConnected = true;
            }
        }
        if (mBluetoothA2dp != null) {
            int size = mBluetoothA2dp.getConnectedDevices().size();
            Log.d(TAG, "isBluetoothAvailable() BluetoothA2dp size: " + size);
            if (mBluetoothA2dp.getConnectedDevices().size() > 0) {
                isConnected = true;
            }
        }
        Log.d(TAG, "isBluetoothAvailable() mBluetoothHeadset isConnected: " + isConnected);
        return isConnected;
    }
    /** @} */
}
