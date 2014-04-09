package com.mediatek.settings.ext;


import android.content.Intent;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.DialogPreference;

public interface ISimManagementExt {
    /**
     * Remove the Auto_wap push preference screen
     * @param parent parent preference to set
     */
   void updateSimManagementPref(PreferenceGroup parent);
    /**
     * Remove the Sim color preference
     * @param preference fragment
     */
   void updateSimEditorPref(PreferenceFragment pref);
    /**
     *Update change Data connect dialog state.
     *@Param preferenceFragment
     *@dialogID
     **/
    void dealWithDataConnChanged(Intent intent, boolean isResumed);
    
    /**
     *update default SIM Summary.
     *@Param preferenceFragment
     *@dialogID
     **/
    void updateDefaultSIMSummary(DialogPreference pref, Long simID);

    /**
     *set Preference Property.
     *@Param preferenceFragment
     *@dialogID
     **/
    void setPrefProperty(DialogPreference pref, long simId);
    /**
     *get Auto String.
     *
     *
     **/
    String getAutoString() ;
    
    /**
     *is Need set Auto Item.
     *@Param preferenceFragment
     *@dialogID
     **/

    public boolean isNeedsetAutoItem();

    /**
     *Show change data connection dialog
     *@Param preferenceFragment
     *@dialogID
     **/
    void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed);
        
    /**
     *Set to close sim slot id
     *@param simSlot
     **/
    void setToClosedSimSlot(int simSlot);
}
