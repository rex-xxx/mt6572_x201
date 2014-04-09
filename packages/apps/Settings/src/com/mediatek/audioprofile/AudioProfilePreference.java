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

/**
 * 
 */
package com.mediatek.audioprofile;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import com.mediatek.settings.ext.IAudioProfileExt;
import com.mediatek.xlog.Xlog;

public class AudioProfilePreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, OnClickListener {

    private static final String XLOGTAG = "Settings/AudioP";
    private static final String TAG = "AudioProfilePreference:";

    private static CompoundButton sCurrentChecked = null;
    private static String sActiveKey = null;

    private String mPreferenceTitle = null;
    private String mPreferenceSummary = null;

    private TextView mTextView = null;
    private TextView mSummary = null;
    private RadioButton mCheckboxButton = null;

    private AudioProfileManager mProfileManager;
    private Context mContext;
    private String mKey;
    private IAudioProfileExt mExt;
    private LayoutInflater mInflater;

    private OnClickListener mOnSettingsClickListener;
    private Scenario mScenario;

    /**
     * AudioProfilePreference construct method
     * 
     * @param context
     *            the context which is associated with, through which it can
     *            access the theme and the resources
     * @param attrs
     *            the attributes of XML tag that is inflating the preference
     * @param defStyle
     *            the default style to apply to the preference
     */
    public AudioProfilePreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;

        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // get the title from audioprofile_settings.xml
        if (super.getTitle() != null) {
            mPreferenceTitle = super.getTitle().toString();
        }

        // get the summary from audioprofile_settings.xml
        if (super.getSummary() != null) {
            mPreferenceSummary = super.getSummary().toString();
        }

        mProfileManager = (AudioProfileManager) context
                .getSystemService(Context.AUDIOPROFILE_SERVICE);

        mKey = getKey();

        mExt = Utils.getAudioProfilePlgin(context);
    }

    /**
     * AudioProfilePreference construct method
     * 
     * @param context
     *            the context which is associated with, through which it can
     *            access the theme and the resources
     * @param attrs
     *            attrs the attributes of XML tag that is inflating the
     *            preference
     */
    public AudioProfilePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * AudioProfilePreference construct method
     * 
     * @param context
     *            which is associated with, through which it can access the
     *            theme and the resources
     */
    public AudioProfilePreference(Context context) {
        this(context, null);
    }

    /**
     * bind the preference with the profile
     * 
     * @param key
     *            the profile key
     */
    public void setProfileKey(String key) {
        setKey(key);
        mKey = key;
    }

    /**
     * Gets the view that will be shown in the PreferenceActivity
     * 
     * @param parent
     *            The parent that this view will eventually be attached
     * @return the preference object
     */
    @Override
    public View onCreateView(ViewGroup parent) {
        Xlog.d(XLOGTAG, TAG + "onCreateView " + getKey());
        View view = mExt.createView(R.layout.audio_profile_item);

        mCheckboxButton = (RadioButton) mExt
                .getPrefRadioButton(R.id.radiobutton);
        mTextView = (TextView) mExt.getPreferenceTitle(R.id.profiles_text);
        mSummary = (TextView) mExt.getPreferenceSummary(R.id.profiles_summary);

        if (mCheckboxButton != null) {
            // mCheckboxButton.setOnCheckedChangeListener(this);
            mCheckboxButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Xlog.d(XLOGTAG, TAG + "onClick " + getKey());

                    if (!mCheckboxButton.equals(sCurrentChecked)) {
                        if (sCurrentChecked != null) {
                            sCurrentChecked.setChecked(false);

                            mCheckboxButton.setChecked(true);
                            sCurrentChecked = mCheckboxButton;
                            mProfileManager.setActiveProfile(mKey);
                        }
                    } else {
                        Xlog.d(XLOGTAG, TAG
                                + "Click the active profile, do nothing return");
                    }
                }
            });

            mCheckboxButton.setChecked(isChecked());
            if (isChecked()) {
                setChecked();
            }
        }

        if (mPreferenceTitle != null && mTextView != null) {
            mTextView.setText(mPreferenceTitle);
        } else {
            Xlog.d(XLOGTAG, TAG + "PreferenceTitle is null");
        }

        dynamicShowSummary();

        ImageView detailsView = (ImageView) mExt
                .getPrefImageView(R.id.deviceDetails);
        ImageView mDividerImage = (ImageView) mExt
                .getPrefImageView(R.id.divider);
        // common version
        if (detailsView != null) {
            detailsView.setTag(mKey);
            Scenario scenario = AudioProfileManager.getScenario(mKey);
            if (Scenario.CUSTOM.equals(scenario)
                    || Scenario.GENERAL.equals(scenario)) {
                detailsView.setOnClickListener(this);
            } else {
                detailsView.setVisibility(View.GONE);
                mDividerImage.setVisibility(View.GONE);
                detailsView.setOnClickListener(null);
            }
        }
        return view;
    }

    /**
     * dynamic show the preference summary according to the profile eg: General
     * / Customer -> Ring/ Ring and vibrate Silent -> Silent only Meeting ->
     * vibrate only Outdoor -> Loudest ring and vibrate
     */
    public void dynamicShowSummary() {
        Xlog.d(XLOGTAG, TAG + mKey + " dynamicShowSummary");

        if (mSummary != null) {
            Scenario scenario = AudioProfileManager.getScenario(mKey);
            if ((Scenario.GENERAL).equals(scenario)
                    || (Scenario.CUSTOM).equals(scenario)) {

                boolean vibrationEnabled = mProfileManager
                        .getVibrationEnabled(mKey);

                Xlog.d(XLOGTAG, TAG + "vibrationEnabled" + vibrationEnabled);

                if (vibrationEnabled) {
                    mSummary.setText(mContext
                            .getString(R.string.ring_vibrate_summary));
                } else {
                    mSummary.setText(mContext.getString(R.string.ring_summary));
                }
            } else {
                if (mPreferenceSummary != null) {
                    mSummary.setText(mPreferenceSummary);
                }
            }
        }
    }

    /**
     * Process the check status change about the preference
     * 
     * @param buttonView
     *            the buttonView which change the check status
     * @param isChecked
     *            the current check status
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Xlog.d(XLOGTAG, TAG + "onCheckedChanged " + isChecked + getKey());

        if (isChecked) {
            if (sCurrentChecked != null) {
                sCurrentChecked.setChecked(false);
            }
            sCurrentChecked = buttonView;

            mProfileManager.setActiveProfile(mKey);
        }
    }

    /**
     * Return the preference current checked status
     * 
     * @return the current check status
     */
    public boolean isChecked() {
        if (sActiveKey != null) {
            return getKey().equals(sActiveKey);
        }
        return false;
    }

    /**
     * When delete /add some profile , set it checked Called in
     * AudioProfileSettings.java
     */
    public void setChecked() {
        sActiveKey = getKey();
        if (mCheckboxButton != null) {
            if (!mCheckboxButton.equals(sCurrentChecked)) {
                if (sCurrentChecked != null) {
                    sCurrentChecked.setChecked(false);
                }
                Xlog.d(XLOGTAG, TAG + "setChecked" + getKey());
                mCheckboxButton.setChecked(true);
                sCurrentChecked = mCheckboxButton;
            }

        } else {
            Xlog.d(XLOGTAG, TAG + "mCheckboxButton is null");
        }
    }

    /**
     * Set the preference title
     * 
     * @param title
     *            the preference title
     * @param setToProfile
     *            true, set the name to the profile framework
     */
    public void setTitle(String title, boolean setToProfile) {
        mPreferenceTitle = title;
        if (setToProfile) {
            mProfileManager.setProfileName(mKey, title);
        }
        if (mTextView != null) {
            mTextView.setText(title);
        }
    }

    /**
     * Get the preference title
     * 
     * @return the preference title
     */
    public String getTitle() {
        return mPreferenceTitle;
    }

    public void setOnSettingsClickListener(OnClickListener listener) {
        mOnSettingsClickListener = listener;
    }

    public void onClick(View v) {
        // Should never be null by construction
        if (mOnSettingsClickListener != null) {
            mOnSettingsClickListener.onClick(v);
        }
    }
}
