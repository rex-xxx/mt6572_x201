/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.audioprofile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference.BaseSavedState;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.common.audioprofile.AudioProfileListener;
import com.mediatek.settings.ext.IAudioProfileExt;
import com.mediatek.xlog.Xlog;

/**
 * Special preference type that allows configuration of both the ring volume and
 * notification volume.
 */
public class RingerVolumePreference extends SeekBarDialogPreference implements
        View.OnKeyListener {
    private static final String TAG = "Settings/VolPref";
    private static final boolean LOGV = true;

    private String mKey;
    private SeekBarVolumizer[] mSeekBarVolumizer;
    private VolumeReceiver mReceiver;
    private final AudioManager mAudioManager;
    private final AudioProfileManager mProfileManager;

    private boolean mIsDlgDismissed = true;

    private IAudioProfileExt mExt;

    private static final int[] SEEKBAR_ID = new int[] {
            R.id.notification_volume_seekbar, R.id.ringer_volume_seekbar,
            R.id.alarm_volume_seekbar };
    // set for volume
    private static final int[] SEEKBAR_TYPE = new int[] {
            AudioProfileManager.STREAM_NOTIFICATION,
            AudioProfileManager.STREAM_RING, AudioProfileManager.STREAM_ALARM };
    // set for ringtone
    private static final int[] STREAM_TYPE = new int[] {
            AudioProfileManager.TYPE_NOTIFICATION,
            AudioProfileManager.TYPE_RINGTONE, RingtoneManager.TYPE_ALARM };

    private static final int[] CHECKBOX_VIEW_ID = new int[] {
            R.id.ringer_mute_button, R.id.notification_mute_button,
            R.id.alarm_mute_button };

    private static final int[] SEEKBAR_UNMUTED_RES_ID = new int[] {
            com.android.internal.R.drawable.ic_audio_ring_notif,
            com.android.internal.R.drawable.ic_audio_notification,
            com.android.internal.R.drawable.ic_audio_alarm };

    /**
     * bind the preference with the profile
     * 
     * @param key
     *            the profile key
     */
    public void setProfile(String key) {
        mKey = key;
    }

    /**
     * The RingerVolumePreference construct method
     * 
     * @param context
     *            context, the context which is associated with, through which
     *            it can access the theme and the resources
     * @param attrs
     *            the attributes of XML tag that is inflating the preferenc
     */
    public RingerVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_dialog_ringervolume_audioprofile);
        setDialogIcon(R.drawable.ic_settings_sound);
        mAudioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        mProfileManager = (AudioProfileManager) context
                .getSystemService(Context.AUDIOPROFILE_SERVICE);
        mSeekBarVolumizer = new SeekBarVolumizer[SEEKBAR_ID.length];
        mExt = Utils.getAudioProfilePlgin(context);
    }

    /**
     * Bind views in the content view of the dialog to data
     * 
     * @param view
     *            The content view of the dialog
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        Context context = getContext();
        mReceiver = new VolumeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        context.registerReceiver(mReceiver, filter);

        mIsDlgDismissed = false;
        Xlog.d(TAG, "set mIsDlgDismissed to false ");

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            ImageView imageview = (ImageView) view
                    .findViewById(CHECKBOX_VIEW_ID[i]);
            if (imageview != null) {
                imageview.setImageResource(SEEKBAR_UNMUTED_RES_ID[i]);
            }

            SeekBar seekBar = (SeekBar) view.findViewById(SEEKBAR_ID[i]);
            if (seekBar != null) {
                if (i == 0) {
                    seekBar.requestFocus();
                }
                mSeekBarVolumizer[i] = new SeekBarVolumizer(context, seekBar,
                        SEEKBAR_TYPE[i]);
                seekBar.setOnKeyListener(this);
            }
        }

        view.setFocusableInTouchMode(true);

        // Disable either ringer+notifications or notifications
        int id;
        if (Utils.isVoiceCapable(getContext())) {
            id = R.id.notification_section;
            mSeekBarVolumizer[0].setVisible(false);
        } else {
            id = R.id.ringer_section;
            mSeekBarVolumizer[1].setVisible(false);
        }
        View hideSection = view.findViewById(id);

        hideSection.setVisibility(View.GONE);

        mProfileManager.listenAudioProfie(mListener,
                AudioProfileListener.LISTEN_RINGER_VOLUME_CHANGED);
    }

    /**
     * When the EditProfile is paused, stop sampling
     */
    public void stopPlaying() {

        if (mSeekBarVolumizer != null) {
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null && vol.isPlaying()) {
                    Xlog.d(TAG, "IsPlaying");
                    vol.stopSample();
                    Xlog.d(TAG, "stopPlaying");
                }
            }
        }
    }

    /**
     * When the EditPorifle is paused, but the user changed the volume and have
     * not save it, revert it.
     */
    public void revertVolume() {
        Xlog.d(TAG, "mIsDlgDismissed" + mIsDlgDismissed);
        if (mIsDlgDismissed) {
            return;
        }
        if (mSeekBarVolumizer != null) {
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null) {
                    vol.revertVolume();
                    vol.resume();
                }
            }
        }
    }

    /**
     * Press the hw volume key, change the current focus seekbar volume
     * 
     * @param v
     *            the ringer volume preference
     * @param keyCode
     *            the keycode of pressed key
     * @param event
     *            the event of key
     * @return true, if press volume up or volume down
     */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If key arrives immediately after the activity has been cleaned up.
        if (mSeekBarVolumizer == null) {
            return true;
        }

        boolean isdown = (event.getAction() == KeyEvent.ACTION_DOWN);
        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            if (vol != null && vol.getSeekBar() != null
                    && vol.getSeekBar().isFocused()) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (isdown) {
                        vol.changeVolumeBy(-1);
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (isdown) {
                        vol.changeVolumeBy(1);
                    }
                    return true;
                default:
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * when start sampling , firstly stop other sampling
     * 
     * @param volumizer
     *            the volumizer which will start sampling
     */
    protected void onSampleStarting(SeekBarVolumizer volumizer) {
        if (volumizer == null) {
            return;
        }
        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            if (vol != null && vol != volumizer) {
                vol.stopSample();
            }
        }
    }

    /**
     * Called when close the adjust volume dialog
     * 
     * @param positiveResult
     *            whether the pressed item is the positive button
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mSeekBarVolumizer == null) {
            return;
        }
        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            vol.stopSample();
        }
        if (positiveResult) {
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null && vol.getVisible()) {
                    vol.saveVolume();
                    vol.getSeekBar().setOnKeyListener(null);
                    vol.stop();
                    vol = null;
                }
            }
        } else {
            Xlog.d(TAG, "Cacel: Original checked.");
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null && vol.getVisible()) {
                    vol.revertVolume();
                    vol.getSeekBar().setOnKeyListener(null);
                    vol.stop();
                    vol = null;
                }
            }
        }
        mIsDlgDismissed = true;
        Xlog.d(TAG, "set mIsDlgDismissed to true");
        getContext().unregisterReceiver(mReceiver);
        mProfileManager.listenAudioProfie(mListener,
                AudioProfileListener.LISTEN_NONE);
    }

    /**
     * Save the current volume store object including the current volume,
     * original volume and the system volume
     * 
     * @return A parcelable object containing the current dynamic state of this
     *         preference
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        if (mSeekBarVolumizer != null) {
            VolumeStore[] volumeStore = myState
                    .getVolumeStore(SEEKBAR_ID.length);
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                SeekBarVolumizer vol = mSeekBarVolumizer[i];
                if (vol != null) {
                    vol.onSaveInstanceState(volumeStore[i]);
                }
            }
        }
        return myState;
    }

    /**
     * Allowing a preference to re-apply a presentation of its internal state.
     * 
     * @param state
     *            the volumeStore object returned by onSaveInstanceState()
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (mSeekBarVolumizer != null) {
            VolumeStore[] volumeStore = myState
                    .getVolumeStore(SEEKBAR_ID.length);
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                SeekBarVolumizer vol = mSeekBarVolumizer[i];
                if (vol != null) {
                    vol.onRestoreInstanceState(volumeStore[i]);
                }
            }
        }
    }

    public static class VolumeStore {
        public int mVolume = -1;
        public int mOriginalVolume = -1;
        public int mSystemVolume = -1;
    }

    private static class SavedState extends BaseSavedState {
        VolumeStore[] mVolumeStore;

        public SavedState(Parcel source) {
            super(source);
            mVolumeStore = new VolumeStore[SEEKBAR_ID.length];
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                mVolumeStore[i] = new VolumeStore();
                mVolumeStore[i].mVolume = source.readInt();
                mVolumeStore[i].mOriginalVolume = source.readInt();
                mVolumeStore[i].mSystemVolume = source.readInt();
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                dest.writeInt(mVolumeStore[i].mVolume);
                dest.writeInt(mVolumeStore[i].mOriginalVolume);
                dest.writeInt(mVolumeStore[i].mSystemVolume);
            }
        }

        VolumeStore[] getVolumeStore(int count) {
            if (mVolumeStore == null || mVolumeStore.length != count) {
                mVolumeStore = new VolumeStore[count];
                for (int i = 0; i < count; i++) {
                    mVolumeStore[i] = new VolumeStore();
                }
            }
            return mVolumeStore;
        }

        public SavedState(Parcelable superState) {
            super(superState);

        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public class SeekBarVolumizer implements OnSeekBarChangeListener, Runnable {

        private Context mContext;
        private Handler mHandler = new Handler();

        private final int mStreamType;
        public Ringtone mRingtone;
        public int mSystemVolume = -1;
        private int mOriginalVolume = -1;
        private int mLastProgress = -1;
        private final SeekBar mSeekBar;
        private Uri mDefaultUri = null;

        public boolean mProfileIsActive = false;

        private boolean mIsVisible = true;

        /**
         * SeekbarVolumizer construct method
         * 
         * @param context
         *            the context which is associated with, through which it can
         *            access the theme and the resources
         * @param seekBar
         * @param streamType
         *            the streamTYpe of the current seekbar
         */
        public SeekBarVolumizer(Context context, SeekBar seekBar, int streamType) {
            mContext = context;

            mStreamType = streamType;
            mSeekBar = seekBar;

            initSeekBar(seekBar);
        }

        /**
         * Init the seekbar about the max volume , current volume, default Uri
         * 
         * @param seekBar
         */
        private void initSeekBar(SeekBar seekBar) {

            seekBar.setMax(mProfileManager.getStreamMaxVolume(mStreamType));

            mSystemVolume = mAudioManager.getStreamVolume(mStreamType);
            Xlog.d(TAG, "" + mStreamType + " get Original SYSTEM Volume: "
                    + mSystemVolume);

            mOriginalVolume = mProfileManager
                    .getStreamVolume(mKey, mStreamType);
            Xlog.d(TAG, "" + mStreamType + " get Original Volume: "
                    + mOriginalVolume);

            mProfileIsActive = mProfileManager.isActive(mKey);
            // if the volume is changed to 1 for ringer mode changed and we
            // can't receive the
            // broadcast to adjust the volume, sync the profile volume with the
            // system
            if (mProfileIsActive) {
                if (mSystemVolume != mOriginalVolume) {
                    Xlog.d(TAG, " sync " + mStreamType + " original Volume to"
                            + mSystemVolume);
                    mOriginalVolume = mSystemVolume;
                }
            }

            mLastProgress = mOriginalVolume;
            seekBar.setProgress(mLastProgress);
            seekBar.setOnSeekBarChangeListener(this);

            if (mStreamType == AudioProfileManager.STREAM_RING) {
                mDefaultUri = mProfileManager.getRingtoneUri(mKey,
                        AudioProfileManager.TYPE_RINGTONE);
            } else if (mStreamType == AudioProfileManager.STREAM_NOTIFICATION) {
                mDefaultUri = mProfileManager.getRingtoneUri(mKey,
                        AudioProfileManager.TYPE_NOTIFICATION);
            } else if (mStreamType == AudioProfileManager.STREAM_ALARM) {
                mDefaultUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
            }

            mRingtone = RingtoneManager.getRingtone(mContext, mDefaultUri);
            if (mRingtone != null) {
                mRingtone.setStreamType(mStreamType);
            }
        }

        /**
         * Set the seekbar visible or unvisible
         * 
         * @param visible
         *            true, the seekbar visible
         */
        public void setVisible(boolean visible) {
            mIsVisible = visible;
        }

        /**
         * get the seekbar whether visible
         * 
         * @return the seekbar visible status
         */
        public boolean getVisible() {
            return mIsVisible;
        }

        /**
         * called when the adjust volume dialog closed unreister the Seekbar
         * change listener
         */
        public void stop() {
            mSeekBar.setOnSeekBarChangeListener(null);
            mContext = null;
            mHandler = null;
        }

        /**
         * return whether the seekbar is sampling
         * 
         * @return the sampling status of the seekbar
         */
        public boolean isPlaying() {

            if (mRingtone != null) {
                return mRingtone.isPlaying();
            }

            return false;
        }

        /**
         * After reverting the volume , sync the system volume, original volume
         * of the seekbar with AudioProfileManager framework
         */
        public void resume() {

            mSystemVolume = mAudioManager.getStreamVolume(mStreamType);
            Xlog.d(TAG, "" + mStreamType + " get Original SYSTEM Volume: "
                    + mSystemVolume);

            mOriginalVolume = mProfileManager
                    .getStreamVolume(mKey, mStreamType);
            Xlog.d(TAG, "" + mStreamType + " get Original Volume: "
                    + mOriginalVolume);

            mProfileIsActive = mProfileManager.isActive(mKey);
            // if the volume is changed to 1 for ringer mode changed and we
            // can't receive the
            // broadcast to adjust the volume, sync the profile volume with the
            // system
            if (mProfileIsActive) {
                if (mSystemVolume != mOriginalVolume) {
                    Xlog.d(TAG, " sync " + mStreamType + " original Volume to"
                            + mSystemVolume);
                    mOriginalVolume = mSystemVolume;
                }
            }

            mLastProgress = mOriginalVolume;
            if (mSeekBar != null) {
                mSeekBar.setProgress(mLastProgress);
            }
        }

        /**
         * When click the "Cancel" button or pause the volume dialog revert the
         * volume
         */
        public void revertVolume() {
            Xlog.d(TAG, "" + mStreamType + " revert Last Volume "
                    + mOriginalVolume);

            // if(mProfileManager.isActive(mKey)) {
            mProfileManager.setStreamVolume(mKey, mStreamType, mOriginalVolume);
            if (mStreamType == AudioProfileManager.STREAM_RING) {
                mProfileManager.setStreamVolume(mKey,
                        AudioProfileManager.STREAM_NOTIFICATION,
                        mOriginalVolume);
            }
            // }

            if (mProfileManager.isActive(mKey)) {
                Xlog.d(TAG, "" + mStreamType + " Active, Revert system Volume "
                        + mOriginalVolume);
                setVolume(mStreamType, mOriginalVolume, false);
            } else {
                if (!isSilentProfileActive()) {
                    Xlog.d(TAG, "" + mStreamType
                            + " not Active, Revert system Volume "
                            + mSystemVolume);
                    setVolume(mStreamType, mSystemVolume, false);
                }
            }
        }

        /**
         * When click the "Ok" button, set the volume to system
         */
        public void saveVolume() {
            Xlog.d(TAG, "" + mStreamType + " Save Last Volume " + mLastProgress);

            mProfileManager.setStreamVolume(mKey, mStreamType, mLastProgress);
            if (mStreamType == AudioProfileManager.STREAM_RING) {
                mProfileManager.setStreamVolume(mKey,
                        AudioProfileManager.STREAM_NOTIFICATION, mLastProgress);
            }

            if (mProfileManager.isActive(mKey)) {
                Xlog.d(TAG, "" + mStreamType + " Active, save system Volume "
                        + mLastProgress);
                setVolume(mStreamType, mLastProgress, false);
            } else {
                if (!isSilentProfileActive()) {
                    Xlog.d(TAG, "" + mStreamType
                            + " not Active, Revert system Volume "
                            + mSystemVolume);
                    setVolume(mStreamType, mSystemVolume, false);
                }
            }

        }

        /**
         * According to the streamType, volume ,flag, set the volume to the
         * system
         * 
         * @param streamType
         *            The StreamType of the volume which will be set
         * @param volume
         *            the volume value which will be set
         * @param flag
         *            true, set the volume by calling
         *            AudioManager.setAudioProfileStreamVolume, in this API,
         *            even though the volume is set to 0, the ringer Mode will
         *            not change, it is useful because in the general profile of
         *            common load, set the volume to 0 and not save, in this
         *            case we need not to change the ringermode, we change the
         *            ringermode if click the "ok" button.The same case is about
         *            the CMCC load, in CMCC, we need not to change the ringer
         *            mode no matter the volume we set is 0 even though we click
         *            "ok" button.
         */
        private void setVolume(int streamType, int volume, boolean flag) {
            if (streamType == AudioProfileManager.STREAM_RING) {

                if (flag) {
                    mAudioManager.setAudioProfileStreamVolume(mStreamType,
                            volume, 0);
                    mAudioManager.setAudioProfileStreamVolume(
                            AudioProfileManager.STREAM_NOTIFICATION, volume, 0);
                } else {
                    mExt.setRingerVolume(mAudioManager, volume);
                }

            } else {
                if (flag) {
                    mAudioManager.setAudioProfileStreamVolume(streamType,
                            volume, 0);
                } else {
                    mExt.setVolume(mAudioManager, streamType, volume);
                }
            }
        }

        /**
         * Get whether the current ringermode is normal
         * 
         * @return true, the current ringer mode is VIBRATE or Silent,
         *         corresponding to the MEETING or Silent profile
         */
        private boolean isSilentProfileActive() {
            return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
        }

        /** 
         * just for override 
         * @param seekBar the changed volume seekbar
         */
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        /**
         * Called when the progress of the seekbar changed
         * 
         * @param seekBar
         *            the seekbar whose progress is changed
         * @param progress
         *            the current progress level
         * @param fromTouch
         *            true if from the user's touch
         */
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromTouch) {
            Xlog.d(TAG, "onProgressChanged" + ": progress" + progress
                    + " : fromTouch" + fromTouch);
            mLastProgress = progress;
            if (!fromTouch) {
                return;
            }
            postSetVolume(progress);
        }

        /**
         * Post a runnable to start sampling when changing the volume
         * 
         * @param progress
         */
        void postSetVolume(int progress) {
            // Do the volume changing separately to give responsive UI
            mHandler.removeCallbacks(this);
            mHandler.post(this);
        }

        /**
         * Called when the user's touch leave the seekbar
         * 
         * @param seekBar
         *            the touched seekbar
         */
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mRingtone != null && !mRingtone.isPlaying()) {
                sample();
            }
        }

        /**
         * implements the runnable in which start sampling
         */
        public void run() {
            sample();
        }

        /**
         * Set the current volume to system and start playing ringtone
         */
        private void sample() {
            onSampleStarting(this);

            Xlog.d(TAG, "sample, set system Volume " + mLastProgress);
            if (!isSilentProfileActive()) {
                setVolume(mStreamType, mLastProgress, true);
            }

            if (mRingtone != null) {
                Xlog.d(TAG, "stream type " + mStreamType + " play sample");
                mRingtone.play();
            }
        }

        /**
         * Stop playing ringtone
         */
        public void stopSample() {
            if (mRingtone != null) {
                Xlog.d(TAG, "stream type " + mStreamType + " stop sample");
                mRingtone.stop();
            }
        }

        /**
         * get the seekbar object in the SeekbarVolumizer
         * 
         * @return the seekbar object
         */
        public SeekBar getSeekBar() {
            return mSeekBar;
        }

        /**
         * Change the volume by amount
         * 
         * @param amount
         *            the volume changed
         */
        public void changeVolumeBy(int amount) {
            mSeekBar.incrementProgressBy(amount);
            postSetVolume(mSeekBar.getProgress());
        }

        /**
         * Allowing a preference to re-apply a presentation of its internal
         * state.
         * 
         * @param volumeStore
         *            including last progress etc.
         */
        public void onSaveInstanceState(VolumeStore volumeStore) {
            if (mLastProgress >= 0) {
                volumeStore.mVolume = mLastProgress;
                volumeStore.mOriginalVolume = mOriginalVolume;
                volumeStore.mSystemVolume = mSystemVolume;
            }
        }

        /**
         * Allowing a preference to re-apply a presentation of its internal
         * state.
         * 
         * @param volumeStore
         *            including last progress etc.
         */
        public void onRestoreInstanceState(VolumeStore volumeStore) {
            if (volumeStore.mVolume != -1) {
                mLastProgress = volumeStore.mVolume;
                mOriginalVolume = volumeStore.mOriginalVolume;
                mSystemVolume = volumeStore.mSystemVolume;
                postSetVolume(mLastProgress);
            }
        }
    }

    /**
     * In the volume adjust dialog, change the system volume, if the current
     * profile is active, adjust the seekbar volume
     * 
     * @author mtk54151
     * 
     */
    private class VolumeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(AudioManager.VOLUME_CHANGED_ACTION)) {
                int streamType = intent.getIntExtra(
                        AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType != AudioManager.STREAM_RING) {
                    return;
                }
                if (mSeekBarVolumizer[1] != null) {
                    SeekBar seekBar = mSeekBarVolumizer[1].getSeekBar();
                    if (seekBar == null) {
                        return;
                    }
                    int volume = mAudioManager.getStreamVolume(streamType);
                    Xlog.d(TAG, "AudioManager Volume " + volume);
                    Xlog.d(TAG, "seekbar progress " + seekBar.getProgress());
                    if (seekBar.getProgress() != volume) {
                        if (volume >= 0) {
                            mSeekBarVolumizer[1].mSystemVolume = volume;
                            Xlog.d(TAG, "is SystemVolume Changed " + volume);
                        }
                    }
                }
            }
        }
    }

    /**
     * Receiving the profile volume change from framework
     */
    private final AudioProfileListener mListener = new AudioProfileListener() {
        @Override
        public void onRingerVolumeChanged(int oldVolume, int newVolume,
                String extra) {
            Xlog.d(TAG, extra + " :onRingerVolumeChanged from " + oldVolume
                    + " to " + newVolume);
            if (mKey.equals(extra) && mSeekBarVolumizer[1] != null) {
                SeekBar seekBar = mSeekBarVolumizer[1].getSeekBar();
                if (seekBar == null) {
                    return;
                }
                if (seekBar.getProgress() != newVolume && newVolume >= 0) {
                    seekBar.setProgress(newVolume);
                    Xlog.d(TAG,
                            "Profile Ringer volume change: mSeekBar.setProgress++ "
                                    + newVolume);
                }
            }
        }
    };

}
