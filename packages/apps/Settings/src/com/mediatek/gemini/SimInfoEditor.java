package com.mediatek.gemini;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.text.Editable;
import android.text.TextWatcher;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class SimInfoEditor extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, TextWatcher {
    private static final String TAG = "SimInfoEditor";
    private static final int DIALOG_SIM_NAME_DUP = 1010;
    private long mSimID;
    private static final String KEY_SIM_NAME = "sim_name";
    private static final String KEY_SIM_NUMBER = "sim_number";
    private static final String KEY_SIM_COLOR = "sim_color";
    private static final String KEY_SIM_NUMBER_FORMAT = "sim_number_format";
    private static final String KEY_SIM_STATUS = "status_info";
    private static final int NAME_EXISTED = -2;
    private static final int TYPE_NAME = 0;
    private static final int TYPE_COLOR = 1;
    private static final int TYPE_NUMBER = 2;
    private static final int TYPE_FORMAT = 3;

    private static String[] sArrayNumFormat;
    private static String sNotSet;
    private ListPreference mSimNumberFormat;
    private EditTextPreference mSimName;
    private EditTextPreference mSimNumber;
    private ColorPickerPreference mSimColor;
    private ISimManagementExt mExt;
    // Add to monitor airplane mode and to disable if airplane mode on
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                updateStatusUiState();
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                // /M: add for hot swap
                Xlog.d(TAG, "receive ACTION_SIM_INFO_UPDATE");
                List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());
                if (simList != null) {
                    if (simList.size() == 0) {
                        // Hot swap and no card so go to settings
                        Xlog.d(TAG, "Hot swap_simList.size()=" + simList.size());
                        goBackSettings();
                    } else if (simList.size() == 1) {
                        if (simList.get(0).mSimId != mSimID) {
                            finish();
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sNotSet = getResources().getString(R.string.apn_not_set);
        Bundle extras = getArguments();
        if (extras != null) {
            mSimID = extras.getLong(GeminiUtils.EXTRA_SIMID, -1);
        }
        Xlog.i(TAG, "simid is " + mSimID);
        sArrayNumFormat = getResources().getStringArray(
                R.array.gemini_sim_info_number_display_format_entries);
        addPreferencesFromResource(R.xml.sim_info_editor);
        mSimNumberFormat = (ListPreference) findPreference(KEY_SIM_NUMBER_FORMAT);
        mSimNumberFormat.setOnPreferenceChangeListener(this);
        mSimName = (EditTextPreference) findPreference(KEY_SIM_NAME);
        mSimName.setOnPreferenceChangeListener(this);
        mSimNumber = (EditTextPreference) findPreference(KEY_SIM_NUMBER);
        mSimNumber.setOnPreferenceChangeListener(this);
        mSimColor = (ColorPickerPreference) findPreference(KEY_SIM_COLOR);
        mSimColor.setSimID(mSimID);
        mSimColor.setOnPreferenceChangeListener(this);
        // /M: add for plug in
        PreferenceGroup parent = this.getPreferenceScreen();
        mExt = Utils.getSimManagmentExtPlugin(this.getActivity());
        mExt.updateSimManagementPref(parent);
		mExt.updateSimEditorPref(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInfo();
        mSimName.getEditText().addTextChangedListener(this);
        updateStatusUiState();
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        getActivity().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Xlog.d(TAG, "OnPause()");
        getActivity().unregisterReceiver(mReceiver);
    }

    private void updateStatusUiState() {
        boolean isAirplaneModeOn = Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1;
        Preference pref = findPreference(KEY_SIM_STATUS);
        if (pref != null) {
            if (isAirplaneModeOn) {
                pref.setEnabled(false);
            } else {
                pref.setEnabled(true);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference.getKey().equals(KEY_SIM_STATUS)) {
            Intent it = new Intent();
            it.setClassName("com.android.settings",
                    "com.android.settings.deviceinfo.SimStatusGemini");
            int slot = SIMInfo.getSlotById(getActivity(), mSimID);

            if (slot < 0) {
                return false;
            }
            it.putExtra("slotid", slot);
            Xlog.i(TAG, "slotid is " + slot);
            startActivity(it);
        }
        return false;
    }

    private void updateInfo() {
        SIMInfo siminfo = SIMInfo.getSIMInfoById(getActivity(), mSimID);
        if (siminfo != null) {
            if (siminfo.mDisplayName == null) {
                mSimName.setSummary(sNotSet);
            } else {
                mSimName.setSummary(siminfo.mDisplayName);
                mSimName.setText(siminfo.mDisplayName);
            }
            if ((siminfo.mNumber != null) && (siminfo.mNumber.length() != 0)) {
                mSimNumber.setSummary(siminfo.mNumber);
                mSimNumber.setText(siminfo.mNumber);
            } else {
                mSimNumber.setSummary(sNotSet);
                mSimNumber.setText("");
            }
            mSimColor.setInitValue(siminfo.mColor);
            int nIndex = turnNumformatValuetoIndex(siminfo.mDispalyNumberFormat);
            if (nIndex < 0) {
                return;
            }
            mSimNumberFormat.setValueIndex(nIndex);
            mSimNumberFormat.setSummary(sArrayNumFormat[nIndex]);
        }
    }

    private int turnNumformatValuetoIndex(int value) {
        if (value == 0) {
            return 2;
        }
        return (value - 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SIM_NAME.equals(key)) {
            Editable textName = mSimName.getEditText().getText();
            if (textName != null) {
                String name = mSimName.getEditText().getText().toString();
                SIMInfo siminfo = SIMInfo.getSIMInfoById(getActivity(), mSimID);
                Xlog.i(TAG, "name is " + name);
                if ((siminfo != null) && (name != null)) {
                    if (name.equals(siminfo.mDisplayName)) {
                        return false;
                    }
                }
                int result = SIMInfo
                        .setDisplayNameEx(getActivity(), name, mSimID,SimInfo.USER_INPUT);

                Xlog.i(TAG, "result is " + result);
                if (result > 0) {
                    // >0 means set successfully
                    mSimName.setSummary(name);
                    Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                    intent.putExtra("simid", mSimID);
                    intent.putExtra("type", TYPE_NAME);
                    getActivity().sendBroadcast(intent);
                } else {

                    if (result == NAME_EXISTED) {
                        showDialog(DIALOG_SIM_NAME_DUP);
                    }

                    if ((siminfo != null) && (siminfo.mDisplayName != null)) {
                        mSimName.setText(siminfo.mDisplayName);

                    }
                    return false;
                }
            }

        } else if (KEY_SIM_COLOR.equals(key)) {
            if (SIMInfo.setColor(getActivity(),
                    ((Integer) objValue).intValue(), mSimID) > 0) {
                Xlog.i(TAG, "set color succeed " + objValue);
                Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                intent.putExtra("simid", mSimID);
                intent.putExtra("type", TYPE_COLOR);
                getActivity().sendBroadcast(intent);
            }
        } else if (KEY_SIM_NUMBER.equals(key)) {
            Editable textNumber = mSimNumber.getEditText().getText();
            if (textNumber != null) {
                Xlog.i(TAG, "textNumber != null ");
                String number = textNumber.toString();
                if (SIMInfo.setNumber(getActivity(), number, mSimID) > 0) {

                    Xlog.i(TAG, "set number succeed " + number);
                    if ((number != null) && (number.length() != 0)) {
                        mSimNumber.setSummary(number);
                    } else {
                        mSimNumber.setSummary(sNotSet);
                    }

                    Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                    intent.putExtra("simid", mSimID);
                    intent.putExtra("type", TYPE_NUMBER);
                    getActivity().sendBroadcast(intent);
                } else {
                    SIMInfo siminfo = SIMInfo.getSIMInfoById(getActivity(),
                            mSimID);
                    if (siminfo != null) {
                        if ((siminfo.mNumber != null)
                                && (siminfo.mNumber.length() != 0)) {

                            mSimNumber.setText(siminfo.mNumber);

                        } else {

                            mSimNumber.setText("");

                        }

                    }
                    return false;
                }

            }

        } else if (KEY_SIM_NUMBER_FORMAT.equals(key)) {

            int value = Integer.parseInt((String) objValue);
            Xlog.i(TAG, "KEY_SIM_NUMBER_FORMAT is " + value);

            if (value < 0) {
                return false;
            }
            if (SIMInfo.setDispalyNumberFormat(getActivity(), value, mSimID) > 0) {

                Xlog.i(TAG, "set format succeed " + value);

                int nIndex = turnNumformatValuetoIndex(value);

                mSimNumberFormat.setSummary(sArrayNumFormat[nIndex]);
                Intent intent = new Intent(Intent.SIM_SETTINGS_INFO_CHANGED);
                intent.putExtra("simid", mSimID);
                intent.putExtra("type", TYPE_FORMAT);
                getActivity().sendBroadcast(intent);
            }
        }

        return true;
    }

    @Override
    public void afterTextChanged(Editable arg0) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub
        Dialog d = mSimName.getDialog();
        if (d instanceof AlertDialog) {
            ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(s.length() > 0);
        }

    }

    @Override
    public Dialog onCreateDialog(int id) {

        Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog alertDlg;
        switch (id) {
        case DIALOG_SIM_NAME_DUP:
            builder.setTitle(getResources().getString(
                    R.string.gemini_sim_info_editor_name_dup_title));
            builder.setIcon(com.android.internal.R.drawable.ic_dialog_alert);
            builder.setMessage(getResources().getString(
                    R.string.gemini_sim_info_editor_name_dup_msg));
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            // TODO Auto-generated method stub

                        }
                    });
            alertDlg = builder.create();
            return alertDlg;

        default:
            return null;
        }
    }

    // /M: add for hot plug
    private void goBackSettings() {
        Intent intent = new Intent(this.getActivity(),
                com.android.settings.Settings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
