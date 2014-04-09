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

import java.util.Formatter;
import java.util.Locale;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AudioEffect.Descriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.audiofx.OpenSLESConstants;
import com.android.musicfx.seekbar.SeekBar;
import com.android.musicfx.seekbar.SeekBar.OnSeekBarChangeListener;

/**
 *
 */
public class ActivityMusic extends Activity implements OnSeekBarChangeListener {
    private final static String TAG = "MusicFXActivityMusic";

    /**
     * Max number of EQ bands supported
     */
    private final static int EQUALIZER_MAX_BANDS = 32;
    
    /**
     * M: Preset reverb none.
     */
    private static final int PRESET_REVERB_NONE = 0;
    
    /**
     * Indicates if Virtualizer effect is supported.
     */
    private boolean mVirtualizerSupported;
    private boolean mVirtualizerIsHeadphoneOnly;
    /**
     * Indicates if BassBoost effect is supported.
     */
    private boolean mBassBoostSupported;
    /**
     * Indicates if Equalizer effect is supported.
     */
    private boolean mEqualizerSupported;
    /**
     * Indicates if Preset Reverb effect is supported.
     */
    private boolean mPresetReverbSupported;

    // Equalizer fields
    private final SeekBar[] mEqualizerSeekBar = new SeekBar[EQUALIZER_MAX_BANDS];
    private int mNumberEqualizerBands;
    private int mEqualizerMinBandLevel;
    private int mEQPresetUserPos = 1;
    private int mEQPreset;
    private int mEQPresetPrevious;
    private int[] mEQPresetUserBandLevelsPrev;
    private String[] mEQPresetNames;

    private int mPRPreset;
    private int mPRPresetPrevious;

    private boolean mIsHeadsetOn = false;
    private CompoundButton mToggleSwitch;

    private StringBuilder mFormatBuilder = new StringBuilder();
    private Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

    /**
     * Mapping for the EQ widget ids per band
     */
    private static final int[][] EQViewElementIds = {
            { R.id.EQBand0TextView, R.id.EQBand0SeekBar },
            { R.id.EQBand1TextView, R.id.EQBand1SeekBar },
            { R.id.EQBand2TextView, R.id.EQBand2SeekBar },
            { R.id.EQBand3TextView, R.id.EQBand3SeekBar },
            { R.id.EQBand4TextView, R.id.EQBand4SeekBar },
            { R.id.EQBand5TextView, R.id.EQBand5SeekBar },
            { R.id.EQBand6TextView, R.id.EQBand6SeekBar },
            { R.id.EQBand7TextView, R.id.EQBand7SeekBar },
            { R.id.EQBand8TextView, R.id.EQBand8SeekBar },
            { R.id.EQBand9TextView, R.id.EQBand9SeekBar },
            { R.id.EQBand10TextView, R.id.EQBand10SeekBar },
            { R.id.EQBand11TextView, R.id.EQBand11SeekBar },
            { R.id.EQBand12TextView, R.id.EQBand12SeekBar },
            { R.id.EQBand13TextView, R.id.EQBand13SeekBar },
            { R.id.EQBand14TextView, R.id.EQBand14SeekBar },
            { R.id.EQBand15TextView, R.id.EQBand15SeekBar },
            { R.id.EQBand16TextView, R.id.EQBand16SeekBar },
            { R.id.EQBand17TextView, R.id.EQBand17SeekBar },
            { R.id.EQBand18TextView, R.id.EQBand18SeekBar },
            { R.id.EQBand19TextView, R.id.EQBand19SeekBar },
            { R.id.EQBand20TextView, R.id.EQBand20SeekBar },
            { R.id.EQBand21TextView, R.id.EQBand21SeekBar },
            { R.id.EQBand22TextView, R.id.EQBand22SeekBar },
            { R.id.EQBand23TextView, R.id.EQBand23SeekBar },
            { R.id.EQBand24TextView, R.id.EQBand24SeekBar },
            { R.id.EQBand25TextView, R.id.EQBand25SeekBar },
            { R.id.EQBand26TextView, R.id.EQBand26SeekBar },
            { R.id.EQBand27TextView, R.id.EQBand27SeekBar },
            { R.id.EQBand28TextView, R.id.EQBand28SeekBar },
            { R.id.EQBand29TextView, R.id.EQBand29SeekBar },
            { R.id.EQBand30TextView, R.id.EQBand30SeekBar },
            { R.id.EQBand31TextView, R.id.EQBand31SeekBar } };

    // Preset Reverb fields
    /**
     * Array containing the PR preset names.
     */
    private static final String[] PRESETREVERBPRESETSTRINGS = { "None", "SmallRoom", "MediumRoom",
            "LargeRoom", "MediumHall", "LargeHall", "Plate" };

    /// M: Send intent to attach or detach reverb effect. {@
    public static final String ATTACH_AUX_AUDIO_EFFECT = "com.android.music.attachauxaudioeffect";
    public static final String DETACH_AUX_AUDIO_EFFECT = "com.android.music.detachauxaudioeffect";
    private static final String AUX_AUDIO_EFFECT_ID = "auxaudioeffectid";
    /// @}

    /**
     * Context field
     */
    private Context mContext;

    /**
     * Calling package name field
     */
    private String mCallingPackageName = "empty";

    /**
     * Audio session field
     */
    private int mAudioSession = AudioEffect.ERROR_BAD_VALUE;

    /// M: Show toast only once whenever user click any time.
    private Toast mToast = null;

    // Broadcast receiver to handle wired and Bluetooth A2dp headset events
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            final boolean isHeadsetOnPrev = mIsHeadsetOn;
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                mIsHeadsetOn = (intent.getIntExtra("state", 0) == 1)
                        || audioManager.isBluetoothA2dpOn();
            } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                final int deviceClass = ((BluetoothDevice) intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).getBluetoothClass()
                        .getDeviceClass();
                if ((deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)
                        || (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET)) {
                    mIsHeadsetOn = true;
                }
            } else if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                mIsHeadsetOn = audioManager.isBluetoothA2dpOn() || audioManager.isWiredHeadsetOn();
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                final int deviceClass = ((BluetoothDevice) intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).getBluetoothClass()
                        .getDeviceClass();
                if ((deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)
                        || (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET)) {
                    mIsHeadsetOn = audioManager.isWiredHeadsetOn();
                }
            }
            if (isHeadsetOnPrev != mIsHeadsetOn) {
                updateUIHeadset();
            }
        }
    };

    /*
     * Declares and initializes all objects and widgets in the layouts and the CheckBox and SeekBar
     * onchange methods on creation.
     *
     * (non-Javadoc)
     *
     * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init context to be used in listeners
        mContext = this;

        // Receive intent
        // get calling intent
        final Intent intent = getIntent();
        mAudioSession = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                AudioEffect.ERROR_BAD_VALUE);
        Log.v(TAG, "audio session: " + mAudioSession);

        mCallingPackageName = getCallingPackage();

        // check for errors
        if (mCallingPackageName == null) {
            Log.e(TAG, "Package name is null");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setResult(RESULT_OK);

        Log.v(TAG, mCallingPackageName + " (" + mAudioSession + ")");

        ControlPanelEffect.initEffectsPreferences(mContext, mCallingPackageName, mAudioSession);

        // query available effects
        final Descriptor[] effects = AudioEffect.queryEffects();

        // Determine available/supported effects
        Log.v(TAG, "Available effects:");
        for (final Descriptor effect : effects) {
            Log.v(TAG, effect.name.toString() + ", type: " + effect.type.toString());

            if (effect.type.equals(AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
                mVirtualizerSupported = true;
                if (effect.uuid.equals(UUID.fromString("1d4033c0-8557-11df-9f2d-0002a5d5c51b"))) {
                    mVirtualizerIsHeadphoneOnly = true;
                }
            } else if (effect.type.equals(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                mBassBoostSupported = true;
            } else if (effect.type.equals(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                mEqualizerSupported = true;
            } else if (effect.type.equals(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                mPresetReverbSupported = true;
            }
        }

        setContentView(R.layout.music_main);
        final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.contentSoundEffects);

        // Fill array with presets from AudioEffects call.
        // allocate a space for 2 extra strings (CI Extreme & User)
        final int numPresets = ControlPanelEffect.getParameterInt(mContext, mCallingPackageName,
                mAudioSession, ControlPanelEffect.Key.eq_num_presets);
        mEQPresetNames = new String[numPresets + 2];
        
        /// M: Use our string to display with different language
        getStringForEq(numPresets);
       
        mEQPresetNames[numPresets] = getString(R.string.ci_extreme);
        mEQPresetNames[numPresets + 1] = getString(R.string.user);
        mEQPresetUserPos = numPresets + 1;
        
        /// M: Move out to avoid JE because mToggleSwitch will be use.
        // Set the listener for the main enhancements toggle button.
        // Depending on the state enable the supported effects if they were
        // checked in the setup tab.
        mToggleSwitch = new Switch(this);
        mToggleSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                    final boolean isChecked) {

                // set parameter and state
                ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName,
                        mAudioSession, ControlPanelEffect.Key.global_enabled, isChecked);
                // Enable Linear layout (in scroll layout) view with all
                // effect contents depending on checked state
                setEnabledAllChildren(viewGroup, isChecked);
                // update UI according to headset state
                updateUIHeadset();
                
                /// M: If preset reverb is none,send intend to music to detach reverb effect
                /// from media player, otherwise sent to attach the new reverb effect
                sendMessageToAttachDetachEffect(isChecked);
            }
        });
        
        // Watch for button clicks and initialization.
        if ((mVirtualizerSupported) || (mBassBoostSupported) || (mEqualizerSupported)
                || (mPresetReverbSupported)) {
            // Initialize the Virtualizer elements.
            // Set the SeekBar listener.
            if (mVirtualizerSupported) {
                // Show msg when disabled slider (layout) is touched
                findViewById(R.id.vILayout).setOnTouchListener(new OnTouchListener() {

                    @Override
                    public boolean onTouch(final View v, final MotionEvent event) {
                        /// M: Respond click event
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            showHeadsetMsg();
                        }
                        return false;
                    }
                });

                final SeekBar seekbar = (SeekBar) findViewById(R.id.vIStrengthSeekBar);
                seekbar.setMax(OpenSLESConstants.VIRTUALIZER_MAX_STRENGTH
                        - OpenSLESConstants.VIRTUALIZER_MIN_STRENGTH);

                seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    // Update the parameters while SeekBar changes and set the
                    // effect parameter.

                    @Override
                    public void onProgressChanged(final SeekBar seekBar, final int progress,
                            final boolean fromUser) {
                        // set parameter and state
                        ControlPanelEffect.setParameterInt(mContext, mCallingPackageName,
                                mAudioSession, ControlPanelEffect.Key.virt_strength, progress);
                    }

                    // If slider pos was 0 when starting re-enable effect
                    @Override
                    public void onStartTrackingTouch(final SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName,
                                    mAudioSession, ControlPanelEffect.Key.virt_enabled, true);
                        }
                    }

                    // If slider pos = 0 when stopping disable effect
                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
                            // disable
                            ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName,
                                    mAudioSession, ControlPanelEffect.Key.virt_enabled, false);
                        }
                    }
                });

                final Switch sw = (Switch) findViewById(R.id.vIStrengthToggle);
                sw.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView,
                            final boolean isChecked) {
                        ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName,
                                mAudioSession, ControlPanelEffect.Key.virt_enabled, isChecked);
                    }
                });
            }

            // Initialize the Bass Boost elements.
            // Set the SeekBar listener.
            if (mBassBoostSupported) {
                // Show msg when disabled slider (layout) is touched
                findViewById(R.id.bBLayout).setOnTouchListener(new OnTouchListener() {

                    @Override
                    public boolean onTouch(final View v, final MotionEvent event) {
                        /// M: Respond click event
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            showHeadsetMsg();
                        }
                        return false;
                    }
                });

                final SeekBar seekbar = (SeekBar) findViewById(R.id.bBStrengthSeekBar);
                seekbar.setMax(OpenSLESConstants.BASSBOOST_MAX_STRENGTH
                        - OpenSLESConstants.BASSBOOST_MIN_STRENGTH);

                seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    // Update the parameters while SeekBar changes and set the
                    // effect parameter.

                    @Override
                    public void onProgressChanged(final SeekBar seekBar, final int progress,
                            final boolean fromUser) {
                        // set parameter and state
                        ControlPanelEffect.setParameterInt(mContext, mCallingPackageName,
                                mAudioSession, ControlPanelEffect.Key.bb_strength, progress);
                    }

                    // If slider pos was 0 when starting re-enable effect
                    @Override
                    public void onStartTrackingTouch(final SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName,
                                    mAudioSession, ControlPanelEffect.Key.bb_enabled, true);
                        }
                    }

                    // If slider pos = 0 when stopping disable effect
                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
                            // disable
                            ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName,
                                    mAudioSession, ControlPanelEffect.Key.bb_enabled, false);
                        }

                    }
                });
            }

            // Initialize the Equalizer elements.
            if (mEqualizerSupported) {
                mEQPreset = ControlPanelEffect.getParameterInt(mContext, mCallingPackageName,
                        mAudioSession, ControlPanelEffect.Key.eq_current_preset);
                if (mEQPreset >= mEQPresetNames.length) {
                    mEQPreset = 0;
                }
                mEQPresetPrevious = mEQPreset;
                equalizerSpinnerInit((Spinner)findViewById(R.id.eqSpinner));
                equalizerBandsInit(findViewById(R.id.eqcontainer));
            }

            // Initialize the Preset Reverb elements.
            // Set Spinner listeners.
            if (mPresetReverbSupported) {
                mPRPreset = ControlPanelEffect.getParameterInt(mContext, mCallingPackageName,
                        mAudioSession, ControlPanelEffect.Key.pr_current_preset);
                mPRPresetPrevious = mPRPreset;
                reverbSpinnerInit((Spinner)findViewById(R.id.prSpinner));
            }

        } else {
            viewGroup.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.noEffectsTextView)).setVisibility(View.VISIBLE);
        }

        ActionBar ab = getActionBar();
        final int padding = getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mToggleSwitch.setPadding(0,0, padding, 0);
        ab.setCustomView(mToggleSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);

        /// M: Set navigation bar in MusicFX
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);
        /// M: Set text selected to add marquee @{
        ((TextView) findViewById(R.id.bBStrengthText)).setSelected(true);
        ((TextView) findViewById(R.id.vIStrengthText)).setSelected(true);
        ((TextView) findViewById(R.id.pRText)).setSelected(true);
        /// @}
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        if ((mVirtualizerSupported) || (mBassBoostSupported) || (mEqualizerSupported)
                || (mPresetReverbSupported)) {
            // Listen for broadcast intents that might affect the onscreen UI for headset.
            final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(mReceiver, intentFilter);

            // Check if wired or Bluetooth headset is connected/on
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mIsHeadsetOn = (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn());
            Log.v(TAG, "onResume: mIsHeadsetOn : " + mIsHeadsetOn);

            // Update UI
            updateUI();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Unregister for broadcast intents. (These affect the visible UI,
        // so we only care about them while we're in the foreground.)
        unregisterReceiver(mReceiver);
        /// M: Save effect settings to share preference when activity in background
        ControlPanelEffect.saveToSharePreference(mContext, mCallingPackageName);
    }

    private void reverbSpinnerInit(Spinner spinner) {
        /// M: PresetReverb has 7 presets: @{
        /// PRESET_NONE = 0;
        /// PRESET_SMALLROOM = 1; PRESET_MEDIUMROOM = 2;
        /// PRESET_LARGEROOM = 3; PRESET_MEDIUMHALL = 4;
        /// PRESET_LARGEHALL = 5; PRESET_PLATE = 6; 
        
        String[] reverbPresetList = getResources().getStringArray(R.array.reverb_preset_list);
        
        if (!getResources().getBoolean(R.bool.isTablet)) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    R.layout.musicfx_spinner_item, reverbPresetList);
            
            //Make reverb UI visible 
            //spinner.setVisibility(View.VISIBLE);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    R.layout.music_big_spinner_dropdown_item, reverbPresetList);
            adapter.setDropDownViewResource(R.layout.music_big_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }
        /// @}
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mPRPresetPrevious) {
                    presetReverbSetPreset(position);
                }
                mPRPresetPrevious = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setSelection(mPRPreset);
    }

    private void equalizerSpinnerInit(Spinner spinner) {
    
        /// M: Show different spinner in tablet @{
        if (!getResources().getBoolean(R.bool.isTablet)) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, mEQPresetNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    R.layout.music_big_spinner_dropdown_item, mEQPresetNames);
            adapter.setDropDownViewResource(R.layout.music_big_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }
        /// @}
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mEQPresetPrevious) {
                    equalizerSetPreset(position);
                }
                mEQPresetPrevious = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setSelection(mEQPreset);
    }


    /**
     * En/disables all children for a given view. For linear and relative layout children do this
     * recursively
     *
     * @param viewGroup
     * @param enabled
     */
    private void setEnabledAllChildren(final ViewGroup viewGroup, final boolean enabled) {
        final int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = viewGroup.getChildAt(i);
            if ((view instanceof LinearLayout) || (view instanceof RelativeLayout)) {
                final ViewGroup vg = (ViewGroup) view;
                setEnabledAllChildren(vg, enabled);
            }
            view.setEnabled(enabled);
        }
    }

    /**
     * Updates UI (checkbox, seekbars, enabled states) according to the current stored preferences.
     */
    private void updateUI() {
        final boolean isEnabled = ControlPanelEffect.getParameterBoolean(mContext,
                mCallingPackageName, mAudioSession, ControlPanelEffect.Key.global_enabled);
        mToggleSwitch.setChecked(isEnabled);
        setEnabledAllChildren((ViewGroup) findViewById(R.id.contentSoundEffects), isEnabled);
        updateUIHeadset();

        if (mVirtualizerSupported) {
            SeekBar bar = (SeekBar) findViewById(R.id.vIStrengthSeekBar);
            Switch sw = (Switch) findViewById(R.id.vIStrengthToggle);
            int strength = ControlPanelEffect
                    .getParameterInt(mContext, mCallingPackageName, mAudioSession,
                            ControlPanelEffect.Key.virt_strength);
            bar.setProgress(strength);
            /// M: TODO check why virt_strength_supported return false when the strength can modify.
            /*boolean hasStrength = ControlPanelEffect.getParameterBoolean(mContext,
                    mCallingPackageName, mAudioSession,
                    ControlPanelEffect.Key.virt_strength_supported);*/
            boolean hasStrength = true;
            if (hasStrength) {
                sw.setVisibility(View.GONE);
            } else {
                bar.setVisibility(View.GONE);
                sw.setChecked(sw.isEnabled() && strength != 0);
            }
        }
        if (mBassBoostSupported) {
            ((SeekBar) findViewById(R.id.bBStrengthSeekBar)).setProgress(ControlPanelEffect
                    .getParameterInt(mContext, mCallingPackageName, mAudioSession,
                            ControlPanelEffect.Key.bb_strength));
        }
        if (mEqualizerSupported) {
            equalizerUpdateDisplay();
        }
        if (mPresetReverbSupported) {
            int reverb = ControlPanelEffect.getParameterInt(
                                    mContext, mCallingPackageName, mAudioSession,
                                    ControlPanelEffect.Key.pr_current_preset);
            ((Spinner)findViewById(R.id.prSpinner)).setSelection(reverb);
        }
    }

    /**
     * Updates UI for headset mode. En/disable VI and BB controls depending on headset state
     * (on/off) if effects are on. Do the inverse for their layouts so they can take over
     * control/events.
     */
    private void updateUIHeadset() {
        if (mToggleSwitch.isChecked()) {
            ((TextView) findViewById(R.id.vIStrengthText)).setEnabled(
                    mIsHeadsetOn || !mVirtualizerIsHeadphoneOnly);
            ((SeekBar) findViewById(R.id.vIStrengthSeekBar)).setEnabled(
                    mIsHeadsetOn || !mVirtualizerIsHeadphoneOnly);
            findViewById(R.id.vILayout).setEnabled(!mIsHeadsetOn || !mVirtualizerIsHeadphoneOnly);
            ((TextView) findViewById(R.id.bBStrengthText)).setEnabled(mIsHeadsetOn);
            ((SeekBar) findViewById(R.id.bBStrengthSeekBar)).setEnabled(mIsHeadsetOn);
            findViewById(R.id.bBLayout).setEnabled(!mIsHeadsetOn);
        }
    }

    /**
     * Initializes the equalizer elements. Set the SeekBars and Spinner listeners.
     */
    private void equalizerBandsInit(View eqcontainer) {
        // Initialize the N-Band Equalizer elements.
        mNumberEqualizerBands = ControlPanelEffect.getParameterInt(mContext, mCallingPackageName,
                mAudioSession, ControlPanelEffect.Key.eq_num_bands);
        mEQPresetUserBandLevelsPrev = ControlPanelEffect.getParameterIntArray(mContext,
                mCallingPackageName, mAudioSession,
                ControlPanelEffect.Key.eq_preset_user_band_level);
        final int[] centerFreqs = ControlPanelEffect.getParameterIntArray(mContext,
                mCallingPackageName, mAudioSession, ControlPanelEffect.Key.eq_center_freq);
        final int[] bandLevelRange = ControlPanelEffect.getParameterIntArray(mContext,
                mCallingPackageName, mAudioSession, ControlPanelEffect.Key.eq_level_range);
        mEqualizerMinBandLevel = bandLevelRange[0];
        final int mEqualizerMaxBandLevel = bandLevelRange[1];

        for (int band = 0; band < mNumberEqualizerBands; band++) {
            // Unit conversion from mHz to Hz and use k prefix if necessary to display
            final int centerFreq = centerFreqs[band] / 1000;
            float centerFreqHz = centerFreq;
            String unitPrefix = "";
            if (centerFreqHz >= 1000) {
                centerFreqHz = centerFreqHz / 1000;
                unitPrefix = "k";
            }
            ((TextView) eqcontainer.findViewById(EQViewElementIds[band][0])).setText(
                    format("%.0f ", centerFreqHz) + unitPrefix + "Hz");
            mEqualizerSeekBar[band] = (SeekBar) eqcontainer
                    .findViewById(EQViewElementIds[band][1]);
            mEqualizerSeekBar[band].setMax(mEqualizerMaxBandLevel - mEqualizerMinBandLevel);
            mEqualizerSeekBar[band].setOnSeekBarChangeListener(this);
        }

        // Hide the inactive Equalizer bands.
        for (int band = mNumberEqualizerBands; band < EQUALIZER_MAX_BANDS; band++) {
            // CenterFreq text
            eqcontainer.findViewById(EQViewElementIds[band][0]).setVisibility(View.GONE);
            // SeekBar
            eqcontainer.findViewById(EQViewElementIds[band][1]).setVisibility(View.GONE);
        }

        // TODO: get the actual values from somewhere
        TextView tv = (TextView) findViewById(R.id.maxLevelText);
        tv.setText("+15 dB");
        tv = (TextView) findViewById(R.id.centerLevelText);
        tv.setText("0 dB");
        tv = (TextView) findViewById(R.id.minLevelText);
        tv.setText("-15 dB");
        equalizerUpdateDisplay();
    }

    private String format(String format, Object... args) {
        mFormatBuilder.setLength(0);
        mFormatter.format(format, args);
        return mFormatBuilder.toString();
    }

    /*
     * For the EQ Band SeekBars
     *
     * (non-Javadoc)
     *
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android
     * .widget.SeekBar, int, boolean)
     */

    @Override
    public void onProgressChanged(final SeekBar seekbar, final int progress, final boolean fromUser) {
        final int id = seekbar.getId();

        for (short band = 0; band < mNumberEqualizerBands; band++) {
            if (id == EQViewElementIds[band][1]) {
                final short level = (short) (progress + mEqualizerMinBandLevel);
                if (fromUser) {
                    equalizerBandUpdate(band, level);
                }
                break;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch(android
     * .widget.SeekBar)
     */

    @Override
    public void onStartTrackingTouch(final SeekBar seekbar) {
        // get current levels
        final int[] bandLevels = ControlPanelEffect.getParameterIntArray(mContext,
                mCallingPackageName, mAudioSession, ControlPanelEffect.Key.eq_band_level);
        // copy current levels to user preset
        for (short band = 0; band < mNumberEqualizerBands; band++) {
            equalizerBandUpdate(band, bandLevels[band]);
        }
        equalizerSetPreset(mEQPresetUserPos);
        ((Spinner)findViewById(R.id.eqSpinner)).setSelection(mEQPresetUserPos);
    }

    /*
     * Updates the EQ display when the user stops changing.
     *
     * (non-Javadoc)
     *
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch(android
     * .widget.SeekBar)
     */

    @Override
    public void onStopTrackingTouch(final SeekBar seekbar) {
        equalizerUpdateDisplay();
    }

    /**
     * Updates the EQ by getting the parameters.
     */
    private void equalizerUpdateDisplay() {
        // Update and show the active N-Band Equalizer bands.
        final int[] bandLevels = ControlPanelEffect.getParameterIntArray(mContext,
                mCallingPackageName, mAudioSession, ControlPanelEffect.Key.eq_band_level);
        for (short band = 0; band < mNumberEqualizerBands; band++) {
            final int level = bandLevels[band];
            final int progress = level - mEqualizerMinBandLevel;
            mEqualizerSeekBar[band].setProgress(progress);
        }
    }

    /**
     * Updates/sets a given EQ band level.
     *
     * @param band
     *            Band id
     * @param level
     *            EQ band level
     */
    private void equalizerBandUpdate(final int band, final int level) {
        ControlPanelEffect.setParameterInt(mContext, mCallingPackageName, mAudioSession,
                ControlPanelEffect.Key.eq_band_level, level, band);
    }

    /**
     * Sets the given EQ preset.
     *
     * @param preset
     *            EQ preset id.
     */
    private void equalizerSetPreset(final int preset) {
        ControlPanelEffect.setParameterInt(mContext, mCallingPackageName, mAudioSession,
                ControlPanelEffect.Key.eq_current_preset, preset);
        equalizerUpdateDisplay();
    }

    /**
     * Sets the given PR preset.
     *
     * @param preset
     *            PR preset id.
     */
    private void presetReverbSetPreset(final int preset) {
        ControlPanelEffect.setParameterInt(mContext, mCallingPackageName, mAudioSession,
                ControlPanelEffect.Key.pr_current_preset, preset);
        /// M: Send broadcast to app to attach or detach reverb effect s@{
        if (preset == PRESET_REVERB_NONE) {
            /// If preset reverb is none,send intend to music to detach reverb effect from media player
            /// and set reverb effect disabled
            Intent intent = new Intent(DETACH_AUX_AUDIO_EFFECT);
            intent.putExtra(AUX_AUDIO_EFFECT_ID, ControlPanelEffect.getAuxiliaryEffectId(mAudioSession));
            sendBroadcast(intent);
            ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName, mAudioSession,
                    ControlPanelEffect.Key.pr_enabled, false);
            Log.d(TAG, "presetReverbSetPreset ReverbEnabled = " + ControlPanelEffect.getReverbEnabled(mAudioSession));
            
        } else {
            /// If preset reverb is not none,send intend to music to attach reverb effect to media player
            /// and set reverb effect enabled
            Intent intent = new Intent(ATTACH_AUX_AUDIO_EFFECT);
            intent.putExtra(AUX_AUDIO_EFFECT_ID, ControlPanelEffect.getAuxiliaryEffectId(mAudioSession));
            sendBroadcast(intent);
            
            /// If reverb effect is already enabled,don't need to enable it again
            if (!ControlPanelEffect.getReverbEnabled(mAudioSession)) {
                ControlPanelEffect.setParameterBoolean(mContext, mCallingPackageName, mAudioSession,
                        ControlPanelEffect.Key.pr_enabled, true);
            }
            Log.d(TAG, "presetReverbSetPreset ReverbEnabled = " + ControlPanelEffect.getReverbEnabled(mAudioSession));
        }
        /// @}
        Log.d(TAG, "presetReverb = " + preset + " ,enabled = " + ControlPanelEffect.getReverbEnabled(mAudioSession));
    }

    /**
     * Show msg that headset needs to be plugged.
     */
    private void showHeadsetMsg() {
        /// M: Make toast only show one time whenever user click any time {@
        if (null == mToast) {
            final Context context = getApplicationContext();
            final int duration = Toast.LENGTH_SHORT;
            mToast = Toast.makeText(context, getString(R.string.headset_plug), duration);
            mToast.setGravity(Gravity.CENTER, mToast.getXOffset() / 2, mToast.getYOffset() / 2);
        }
        mToast.show();
        /// @}
    }

    /**
     * M: Get different language strings for equalizer.
     *  
     * @param numPresets Equalizer preset number
     */
    private void getStringForEq(int numPresets) {
        String presetName = null;
        for (short i = 0; i < numPresets; i++) {
            presetName = ControlPanelEffect.getParameterString(mContext,
                    mCallingPackageName, mAudioSession, ControlPanelEffect.Key.eq_preset_name, i);
            if ("Normal".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_normal);
            } else if ("Classical".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_classical);
            } else if ("Dance".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_dance);
            } else if ("Flat".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_flat);
            } else if ("Folk".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_folk);
            } else if ("Heavy Metal".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_heavymetal);
            } else if ("Hip Hop".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_hiphop);
            } else if ("Jazz".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_jazz);
            } else if ("Pop".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_pop);
            } else if ("Rock".equalsIgnoreCase(presetName)) {
                mEQPresetNames[i] = getString(R.string.eq_preset_rock);
            } else {
                /// M: The preset name string does not match any of our known preset name
                /// so use the default string returned from getPresetName()
                mEQPresetNames[i] = presetName;
            }
        }
    }

    /**
     * M: Send broadcast to music to attach or detach reverb effect.
     * 
     * @param isChecked The new checked state of buttonView.
     */
    private void sendMessageToAttachDetachEffect(boolean isChecked) {
        if (!isChecked || (PRESET_REVERB_NONE == mPRPresetPrevious)) {
            Intent intent = new Intent(DETACH_AUX_AUDIO_EFFECT);
            intent.putExtra(AUX_AUDIO_EFFECT_ID, ControlPanelEffect.getAuxiliaryEffectId(
                    mAudioSession));
            sendBroadcast(intent);
            Log.d(TAG, "presetReverbSetPreset ReverbEnabled = "
                    + ControlPanelEffect.getReverbEnabled(mAudioSession));
        } else {
            Intent intent = new Intent(ATTACH_AUX_AUDIO_EFFECT);
            intent.putExtra(AUX_AUDIO_EFFECT_ID, ControlPanelEffect.getAuxiliaryEffectId(
                    mAudioSession));
            sendBroadcast(intent);
            Log.d(TAG, "presetReverbSetPreset ReverbEnabled = "
                    + ControlPanelEffect.getReverbEnabled(mAudioSession));
        }
    }

    /**
     * M: Set the navigation bar as back key.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
