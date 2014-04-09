package com.mediatek.gemini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.xlog.Xlog;

import java.util.List;


public class SimRoamingSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String KEY_ROAING_REMINDER_SETTING = "roaming_reminder_settings";
    private static final String TAG = "SimRoamingSettings";
    private static final String KEY_ROAMING_ENTRANCE = "data_roaming_settings";
    private ListPreference mRoamReminder;
    private CharSequence[] mRoamingReminderSummary;
    private ITelephony mTelephony;
    private ISimRoamingExt mExt;
    ///M: add for sim hot swap @{
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                Xlog.d(TAG, "receive ACTION_SIM_INFO_UPDATE");
                List<SIMInfo> simList = SIMInfo.getInsertedSIMList(SimRoamingSettings.this.getActivity());
                if (simList != null && simList.size() == 0) {
                    // Hot swap and no card so go to settings
                    Xlog.d(TAG, "Hot swap_simList.size()=" + simList.size());
                    goBackSettings();
                }
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sim_roaming_settings);

        mRoamReminder = (ListPreference) findPreference(KEY_ROAING_REMINDER_SETTING);
        mRoamReminder.setOnPreferenceChangeListener(this);
        mRoamingReminderSummary = getResources().getTextArray(
                R.array.gemini_sim_roaming_reminder_entries);
        mTelephony = ITelephony.Stub.asInterface(ServiceManager
                .getService("phone"));
        mExt = Utils.getSimRoamingExtPlugin(this.getActivity());
        this.getActivity().registerReceiver(mSimReceiver, new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE));
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        int prevalue = Settings.System.getInt(getContentResolver(),
                Settings.System.ROAMING_REMINDER_MODE_SETTING, 0);

        Xlog.i(TAG, "prevalue is " + prevalue);
        mRoamReminder.setValueIndex(prevalue);
        mRoamReminder.setSummary(mRoamingReminderSummary[prevalue]);
        Preference p = this.findPreference(KEY_ROAMING_ENTRANCE);
        if (p != null) {
            mExt.setSummary(p);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getActivity().unregisterReceiver(mSimReceiver);
    }
    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {

        final String key = arg0.getKey();
        // TODO Auto-generated method stub
        if (KEY_ROAING_REMINDER_SETTING.equals(key)) {

            Xlog.i(TAG, "KEY_ROAING_REMINDER_SETTING.equals(key)");

            int value = Integer.parseInt((String) arg1);
            mRoamReminder.setValueIndex(value);
            mRoamReminder.setSummary(mRoamReminder.getEntry());
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ROAMING_REMINDER_MODE_SETTING, value);

            Intent intent = new Intent(
                    Intent.ACTION_ROAMING_REMINDER_SETTING_CHANGED);

            intent.putExtra("mode", value);

            getActivity().sendBroadcast(intent);

            if (value == 0) {

                if (mTelephony != null) {
                    try {
                        mTelephony
                                .setRoamingIndicatorNeddedProperty(true, true);

                    } catch (RemoteException e) {
                        Xlog.e(TAG, "mTelephony exception");

                    }

                }

            }

        }
        return false;
    }
    // M: add for hot swap
    private void goBackSettings() {
        if (this.isResumed()) {
            Intent intent = new Intent(this.getActivity(), com.android.settings.Settings.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }
}
