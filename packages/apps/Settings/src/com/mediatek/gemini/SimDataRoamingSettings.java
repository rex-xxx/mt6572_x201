package com.mediatek.gemini;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;

import com.android.internal.telephony.ITelephony;
import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.xlog.Xlog;

public class SimDataRoamingSettings extends SimCheckboxEntrance implements OnClickListener {

    private static final String TAG = "SimDataRoamingSettings";

    private ITelephony mTelephony;

    private int mCurrentSimSlot;
    private long mCurrentSimID;
    private ISimRoamingExt mExt;
    private SimInfoPreference mSimInfoPref;
    private static final int DLG_ROAMING_WARNING = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTelephony = ITelephony.Stub.asInterface(ServiceManager
                .getService("phone"));
        mExt = Utils.getSimRoamingExtPlugin(this.getActivity());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        long simID = Long.parseLong(preference.getKey());
        SIMInfo simInfo = SIMInfo.getSIMInfoById(getActivity(), simID);
        if (simInfo != null) {
            int dataRoaming;
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                dataRoaming = simInfo.mDataRoaming;
            } else {
                if (getDataRoamingState()) {
                    dataRoaming = SimInfo.DATA_ROAMING_ENABLE;
                } else {
                    dataRoaming = SimInfo.DATA_ROAMING_DISABLE;
                }
            }
            mCurrentSimSlot = simInfo.mSlot;
            mCurrentSimID = simInfo.mSimId;
            mSimInfoPref = (SimInfoPreference) preference;
            if (dataRoaming == SimInfo.DATA_ROAMING_DISABLE) {
                showDialog(DLG_ROAMING_WARNING);
            } else {
                setDataRoaming(false);
                mSimInfoPref.setCheck(false);
            }
            return true;
        }
        return false;
    }

    
    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (DLG_ROAMING_WARNING == dialogId) {
            Context context = getActivity();
            String msg = mExt.getRoamingWarningMsg(context,R.string.roaming_warning);
            Xlog.d(TAG, "msg=" + msg);
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(msg)
                   .setTitle(android.R.string.dialog_alert_title)
                   .setIcon(android.R.drawable.ic_dialog_alert)
                   .setPositiveButton(android.R.string.yes,this)
                   .setNegativeButton(android.R.string.no,this);
            return builder.create();
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    protected boolean shouldDisableWhenRadioOff() {
        return true;
    }

    protected void updateCheckState(SimInfoPreference pref, SIMInfo siminfo) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            pref.setCheck(siminfo.mDataRoaming == SimInfo.DATA_ROAMING_ENABLE);
        } else {
            pref.setCheck(getDataRoamingState());
        }
    }
    /**
     * 
     * @return true data is roaming otherwise false
     */
    private boolean getDataRoamingState() {
        return Settings.Secure.getInt(getActivity().getContentResolver(), 
                                      Settings.Secure.DATA_ROAMING, 0) != 0;
    }
    private void setDataRoaming(boolean enable) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (SIMInfo.getSIMInfoBySlot(getActivity(), mCurrentSimSlot) != null) {
                try {
                    if (mTelephony != null) {
                        mTelephony.setDataRoamingEnabledGemini(enable,mCurrentSimSlot);
                    }
                } catch (RemoteException e) {
                    Xlog.e(TAG, "mTelephony exception");
                    return;
                }
                int roamingState;
                if (enable) {
                    roamingState = SimInfo.DATA_ROAMING_ENABLE;
                } else {
                    roamingState = SimInfo.DATA_ROAMING_DISABLE;
                }
                SIMInfo.setDataRoaming(getActivity(),roamingState,mCurrentSimID);
            } else {
                Xlog.d(TAG,"sim slot " + mCurrentSimSlot + " has been plug out");
            }
            
        } else {
            Settings.Secure.putInt(getActivity().getContentResolver(), Settings.Secure.DATA_ROAMING, enable ? 1 : 0);
        }
    }
    
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            setDataRoaming(true);
            mSimInfoPref.setCheck(true);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            mSimInfoPref.setCheck(false);
        }
    }

}
