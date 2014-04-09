package com.mediatek.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;

import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.R;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class NetworkEditor extends PreferenceActivity 
        implements Preference.OnPreferenceChangeListener, TextWatcher {
    private static final String TAG = "NetworkEditor";
    private static final int MENU_DELETE = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_DISCARD = Menu.FIRST + 2;
    private static final int DIALOG_NETWORK_ID = 0;

    private static final String BUTTON_PRIORITY_KEY = "priority_key";
    private static final String BUTTON_NEWWORK_MODE_KEY = "network_mode_key";
    private static final String BUTTON_NETWORK_ID_KEY = "network_id_key";
    
    public static final String PLMN_NAME = "plmn_name";
    public static final String PLMN_CODE = "plmn_code";
    public static final String PLMN_PRIORITY = "plmn_priority";
    public static final String PLMN_SERVICE = "plmn_service";
    public static final String PLMN_SIZE = "plmn_size";
    public static final String PLMN_ADD = "plmn_add";
    public static final String PLMN_SLOT = "plmn_slot";
    public static final String PROPERTY_KEY = "gsm.baseband.capability";
    public static final int MODEM_MASK_TDSCDMA = 0x08;
    
    private Preference mNetworkId = null;
    private EditTextPreference mPriority = null;
    private ListPreference mNetworkMode = null;

    private String mPLMNName;

    public static final int RESULT_MODIFY = 100;
    public static final int RESULT_DELETE = 200;


    private String mNotSet = null;

    private TelephonyManager mTelephonyManager;
    private boolean mAirplaneModeEnabled = false;
    private int mDualSimMode = -1;
    private IntentFilter mIntentFilter;
    private int mSlotId;

    private static final int GSM = 0;
    private static final int WCDMA_TDSCDMA = 1;
    private static final int DUAL_MODE = 2;

    private static final int RIL_2G = 0x1;
    private static final int RIL_3G = 0x4;
    private static final int RIL_2G_3G = 0x5;

    private boolean mActSupport = true;
    private EditText mNetworkIdText;
    private AlertDialog mIdDialog = null;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch(state) {
            case TelephonyManager.CALL_STATE_IDLE:
                setScreenEnabled();
                break;
            default:
                break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); 
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                setScreenEnabled();
            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
                setScreenEnabled();
            }  else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for hot swap {
                Xlog.d(TAG, "ACTION_SIM_INFO_UPDATE received");
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(NetworkEditor.this);
                if (temp.size() == 0 || (temp.size() == 1 && temp.get(0).mSlot != mSlotId)) {
                    Xlog.d(TAG, "sim card number is " + temp.size());
                    CallSettings.goToMobileNetworkSettings(NetworkEditor.this);
                }
                ///@}
            }
        }
    };

    private OnClickListener mNetworkIdListener = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                String summary = checkNull(mNetworkIdText.getText().toString());
                Xlog.d(TAG, "input network id is " + summary);
                mNetworkId.setSummary(summary);
            }
        }
    };

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.plmn_editor);
        mNotSet = getResources().getString(R.string.voicemail_number_not_set);

        mNetworkId = (Preference)findPreference(BUTTON_NETWORK_ID_KEY);
        mPriority = (EditTextPreference)findPreference(BUTTON_PRIORITY_KEY);
        mNetworkMode = (ListPreference)findPreference(BUTTON_NEWWORK_MODE_KEY);
        mPriority.setOnPreferenceChangeListener(this);
        mNetworkMode.setOnPreferenceChangeListener(this);


        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        if (CallSettings.isMultipleSim()) {
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        registerReceiver(mReceiver, mIntentFilter);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    protected void onResume() {
        super.onResume();
        createNetworkInfo(getIntent());
        mAirplaneModeEnabled = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, -1) == 1;
        if (CallSettings.isMultipleSim()) {
            mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(), 
                    android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
        }
        setScreenEnabled();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String value = objValue.toString();
        if (preference == mPriority) {
            mPriority.setSummary(checkNull(value));
        } else if (preference == mNetworkMode) {
            mNetworkMode.setValue(value);
            String summary = "";
            int index = Integer.parseInt(value);
            if ((getBaseBand() & MODEM_MASK_TDSCDMA) == 0) {
                //WCDMA
                summary = getResources().getStringArray(
                    R.array.plmn_prefer_network_mode_choices)[index];
            } else {
                //TD-SCDMA
                summary = getResources().getStringArray(
                    R.array.plmn_prefer_network_mode_td_choices)[index];
            }
            mNetworkMode.setSummary(summary);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (!getIntent().getBooleanExtra(PLMN_ADD, false)) {
            menu.add(0, MENU_DELETE, 0, com.android.internal.R.string.delete);
        }
        menu.add(0, MENU_SAVE, 0, R.string.save);
        menu.add(0, MENU_DISCARD, 0, com.android.internal.R.string.cancel);
        return true;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        super.onMenuOpened(featureId, menu);
        boolean isShouldEnabled = false;
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        isShouldEnabled = isIdle && (!mAirplaneModeEnabled) && (mDualSimMode != 0);
        boolean isEmpty = mNotSet.equals(mNetworkId.getSummary()) || mNotSet.equals(mPriority.getSummary());
        if (menu != null) {
            menu.setGroupEnabled(0, isShouldEnabled);
            if (getIntent().getBooleanExtra(PLMN_ADD, true)) {
                menu.getItem(0).setEnabled(isShouldEnabled && !isEmpty);
            } else {
                menu.getItem(1).setEnabled(isShouldEnabled && !isEmpty);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DELETE:
            setRemovedNetwork();
            break;
        case MENU_SAVE:
            validateAndSetResult();
            break;
        case MENU_DISCARD:
            break;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        finish();
        return super.onOptionsItemSelected(item);
    }

    private void validateAndSetResult() {
        Intent intent = new Intent(this, PLMNListPreference.class);
        setResult(RESULT_MODIFY, intent);
        genNetworkInfo(intent);
    }
    
    private void genNetworkInfo(Intent intent) {
        intent.putExtra(NetworkEditor.PLMN_NAME, checkNotSet(mPLMNName));
        intent.putExtra(NetworkEditor.PLMN_CODE, mNetworkId.getSummary());
        int priority = 0;
        int size = getIntent().getIntExtra(PLMN_SIZE, 0);
        try {
            priority = Integer.parseInt(String.valueOf(mPriority.getSummary()));
        } catch (NumberFormatException e) {
            Xlog.d(TAG, "parse value of basband error");
        }
        if (getIntent().getBooleanExtra(PLMN_ADD, false)) {
            if (priority > size) {
                priority = size;
            }
        } else {
            if (priority >= size) {
                priority = size - 1;
            }
        }
        intent.putExtra(NetworkEditor.PLMN_PRIORITY, priority);
        try {
            intent.putExtra(NetworkEditor.PLMN_SERVICE, 
                    covertApNW2Ril(Integer.parseInt(String.valueOf(mNetworkMode.getValue()))));
        } catch (NumberFormatException e) {
            intent.putExtra(NetworkEditor.PLMN_SERVICE, covertApNW2Ril(0));
        }
    }
    
    private void setRemovedNetwork() {
        Intent intent = new Intent(this, PLMNListPreference.class);
        setResult(RESULT_DELETE, intent);
        genNetworkInfo(intent);
    }

    public static int covertRilNW2Ap(int mode) {
        int result = 0;
        if (mode >= RIL_2G_3G) {
            result = DUAL_MODE;
        } else if ((mode & RIL_3G) != 0) {
            result = WCDMA_TDSCDMA;
        } else {
            result = GSM;
        }
        return result;
    }
    
    public static int covertApNW2Ril(int mode) {
        int result = 0;
        if (mode == DUAL_MODE) {
            result = RIL_2G_3G;
        } else if (mode == WCDMA_TDSCDMA) {
            result = RIL_3G;
        } else {
            result = RIL_2G;
        }
        return result;
    }

    private void createNetworkInfo(Intent intent) {
        mPLMNName = intent.getStringExtra(PLMN_NAME);
        String number = intent.getStringExtra(PLMN_CODE);
        mNetworkId.setSummary(checkNull(number));
        int priority = intent.getIntExtra(PLMN_PRIORITY, 0);
        mPriority.setSummary(String.valueOf(priority));
        mPriority.setText(String.valueOf(priority));
        int act = intent.getIntExtra(PLMN_SERVICE, 0);

        //if act is not supported, disable mNetworkMode
        Xlog.d(TAG, "act = " + act);
        if (!getIntent().getBooleanExtra(PLMN_ADD, true)) {
            mActSupport = act != 0;
        }
        Xlog.d(TAG, "mActSupport = " + mActSupport);
        act = covertRilNW2Ap(act);
        if (act < GSM || act > DUAL_MODE) {
            act = GSM;
        }
        String summary = "";
        if ((getBaseBand() & MODEM_MASK_TDSCDMA) == 0) {
            //WCDMA
            summary = getResources().getStringArray(R.array.plmn_prefer_network_mode_choices)[act];
        } else {
            mNetworkMode.setEntries(getResources().getTextArray(
                R.array.plmn_prefer_network_mode_td_choices));
            summary = getResources().getStringArray(
                R.array.plmn_prefer_network_mode_td_choices)[act];
        }
        mNetworkMode.setSummary(summary);
        mNetworkMode.setValue(String.valueOf(act));
        mSlotId = intent.getIntExtra(PLMN_SLOT, -1);
    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(mNotSet)) {
            return "";
        } else {
            return value;
        }
    }

    private String checkNull(String value) {
        if (value == null || value.length() == 0) {
            return mNotSet;
        } else {
            return value;
        }
    }

    private void setScreenEnabled() {
        boolean isShouldEnabled = false;
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        isShouldEnabled = isIdle && (!mAirplaneModeEnabled) && (mDualSimMode != 0);
        getPreferenceScreen().setEnabled(isShouldEnabled);
        invalidateOptionsMenu();
        mNetworkMode.setEnabled(mActSupport && isShouldEnabled);
    }

    static int getBaseBand() {
        int value = 0;
        String capability = null;

        capability = SystemProperties.get(PROPERTY_KEY);
        if (capability == null || "".equals(capability)) {
            return value;
        }

        try {
            value = Integer.valueOf(capability, 16);
        } catch (NumberFormatException ne) {
            Xlog.d(TAG, "parse value of basband error");
        }
        return value;        
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == mNetworkId) {
            removeDialog(DIALOG_NETWORK_ID);
            showDialog(DIALOG_NETWORK_ID);
            validate();
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_NETWORK_ID) {
            mNetworkIdText = new EditText(this);
            if (!mNotSet.equals(mNetworkId.getSummary())) {
                mNetworkIdText.setText(mNetworkId.getSummary());
            }
            mNetworkIdText.addTextChangedListener(this);
            mNetworkIdText.setInputType(InputType.TYPE_CLASS_NUMBER);
            mIdDialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.network_id))
                .setView(mNetworkIdText)
                .setPositiveButton(getResources().getString(com.android.internal.R.string.ok), mNetworkIdListener)
                .setNegativeButton(getResources().getString(com.android.internal.R.string.cancel), null)
                .create();
            mIdDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            return mIdDialog;
        }
        return null;
    }

    public void validate() {
        int len = mNetworkIdText.getText().toString().length();
        boolean state = true;
        if (len < 5 || len > 6) {
            state = false;
        }
        if (mIdDialog != null) {
            mIdDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(state);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        validate();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
              int after) {
        // work done in afterTextChanged
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // work done in afterTextChanged
    }

}
