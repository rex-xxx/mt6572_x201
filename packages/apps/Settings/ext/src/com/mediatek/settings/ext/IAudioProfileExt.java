package com.mediatek.settings.ext;

import android.content.Intent;
import android.media.AudioManager;
import android.view.View;


public interface IAudioProfileExt {
    
    /**
     * Get whether the profile can be edited
     * @return true, can be edited
     */
    boolean isPrefEditable();

    /**
     * Inflate different AudioProfilePreference layout for different operator
     * @param defaultLayoutId the default layout id
     * @return the view that has been inflated
     */
    View createView(int defaultLayoutId);
    
    /**
     * get the AudioProfilePreference title
     * @param defaultTitleId the title id of default AudioProfilePreference layout 
     * @return AudioProfilePreference title view
     */
    View getPreferenceTitle(int defaultTitleId);
    
    /**
     * get the AudioProfilePreference summary
     * @param defaultSummaryId the summary id of default AudioProfilePreference layout
     * @return AudioProfilePreference summary view
     */
    View getPreferenceSummary(int defaultSummaryId);
    
    /**
     * get the AudioProfilePreference radiobutton
     * @param defaultRBId the radiobutton id of default AudioProfilePreference layout
     * @return AudioProfilePreference radiobutton view
     */
    View getPrefRadioButton(int defaultRBId);
    
    /**
     * get the AudioProfilePreference Image view
     * @param defaultImageViewId the image view of default AudioProfilePreference layout
     * @return AudioProfilePreference image view
     */
    View getPrefImageView(int defaultImageViewId);

    /**
     * set different params of RingtonePickerActivity for different operator 
     * @param intent the intent that will send to RingtonePickerActivity
     */
    void setRingtonePickerParams(Intent intent);

    /**
     * set AudioProfile Ringer volume
     * @param audiomanager call audiomanager to set the volume
     * @param volume the volume that will be set
     */
    void setRingerVolume(AudioManager audiomanager, int volume);

    /**
     * set AudioProfile volume
     * @param audiomanager call audiomanager to set the volume
     * @param streamType the volume type that will be set
     * @param volume the volume that will be set
     */
    //set the notification or alarm volume
    void setVolume(AudioManager audiomanager, int streamType, int volume);
}
