/*
 * Copyright (C) 2010-2011 The Android Open Source Project
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

package com.android.musicfx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.util.Log;

public class ControlPanelReceiver extends BroadcastReceiver {

    private final static String TAG = "MusicFXControlPanelReceiver";
    /// M: Send intent to attach or detach reverb effect. {@
    public static final String ATTACH_AUX_AUDIO_EFFECT = "com.android.music.attachauxaudioeffect";
    public static final String DETACH_AUX_AUDIO_EFFECT = "com.android.music.detachauxaudioeffect";
    private static final String AUX_AUDIO_EFFECT_ID = "auxaudioeffectid";
    private static final int PRESET_REVERB_CURRENT_PRESET_DEFAULT = 0; // None
    private static final int PRESET_REVERB_NONE = 0;
    private Context mContext;
    private String mPackageName = null;
    private int mAudioSession = -1;
    /// @}
    
    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.v(TAG, "onReceive");

        if ((context == null) || (intent == null)) {
            Log.w(TAG, "Context or intent is null. Do nothing.");
            return;
        }
        mContext = context;
        
        final String action = intent.getAction();
        final String packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME);
        final int audioSession = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                AudioEffect.ERROR_BAD_VALUE);

        mAudioSession = audioSession;
        mPackageName = packageName;
        
        Log.v(TAG, "Action: " + action);
        Log.v(TAG, "Package name: " + packageName);
        Log.v(TAG, "Audio session: " + audioSession);

        // check package name
        if (packageName == null) {
            Log.w(TAG, "Null package name");
            return;
        }

        // check audio session
        if ((audioSession == AudioEffect.ERROR_BAD_VALUE) || (audioSession < 0)) {
            Log.w(TAG, "Invalid or missing audio session " + audioSession);
            return;
        }

        /// M: get reverb effect preset from share preference
        final short presetReverb = (short) context.getSharedPreferences(packageName,
                Context.MODE_PRIVATE).getInt(ControlPanelEffect.Key.pr_current_preset.toString(),
                        PRESET_REVERB_CURRENT_PRESET_DEFAULT);
        // open audio session
        if (action.equals(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)) {

            // retrieve the effect enabled state
            final boolean isGlobalEnabled = context.getSharedPreferences(packageName,
                    Context.MODE_PRIVATE).getBoolean(
                    ControlPanelEffect.Key.global_enabled.toString(),
                    ControlPanelEffect.GLOBAL_ENABLED_DEFAULT);

            /// M: If need reset reverb we should remove java instance and release native instance, so that new preset
            /// reverb will be create again.
            if (intent.getBooleanExtra("reset_reverb", false)) {
                ControlPanelEffect.resetPresetReverbInstances(mAudioSession);
            }
            ControlPanelEffect.openSession(context, packageName, audioSession);
            
            /// M: If global enabled and the preset reverb is not none ,send message to music to
            /// attach reverb effect to media player 
            sendMessageToMusicAttachEffect(isGlobalEnabled, presetReverb);
        }

        // close audio session
        if (action.equals(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)) {
            /// M: close session in a thread to avoid ANR
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    Log.d(TAG, "closeSession in a thead.");
                    ControlPanelEffect.closeSession(mContext, mPackageName, mAudioSession);
                    
                }
            }).start();
        }

        // set params
        if (action.equals("AudioEffect.ACTION_SET_PARAM")) {
            final String param = intent.getStringExtra("AudioEffect.EXTRA_PARAM");

            if (param.equals("GLOBAL_ENABLED")) {
                final Boolean value = intent.getBooleanExtra("AudioEffect.EXTRA_VALUE", false);
                ControlPanelEffect.setParameterBoolean(context, packageName, audioSession,
                        ControlPanelEffect.Key.global_enabled, value);
                
                /// M: If global enabled and the preset reverb is not none ,send message to music
                /// to attach reverb effect to media player 
                sendMessageToMusicAttachEffect(value.booleanValue(), presetReverb);
            }
        }

        // get params
        if (action.equals("AudioEffect.ACTION_GET_PARAM")) {
            final String param = intent.getStringExtra("AudioEffect.EXTRA_PARAM");

            if (param.equals("GLOBAL_ENABLED")) {
                final Boolean value = ControlPanelEffect.getParameterBoolean(context, packageName,
                        audioSession, ControlPanelEffect.Key.global_enabled);
                final Bundle extras = new Bundle();
                extras.putBoolean("GLOBAL_ENABLED", value);
                setResultExtras(extras);
            }
        }
    }

    private void sendMessageToMusicAttachEffect(boolean enabled, int presetReverb) {
        if (enabled && presetReverb != PRESET_REVERB_NONE) {
            Intent intent = new Intent(ATTACH_AUX_AUDIO_EFFECT);
            intent.putExtra(AUX_AUDIO_EFFECT_ID, ControlPanelEffect.getAuxiliaryEffectId(mAudioSession));
            mContext.sendBroadcast(intent);
        }
    }
}
