package com.mediatek.settings;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.view.MenuItem;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.R;
import com.android.phone.TimeConsumingPreferenceActivity;

import com.mediatek.xlog.Xlog;

import java.util.List;

public class CellBroadcastActivity extends TimeConsumingPreferenceActivity {
    private static final String BUTTON_CB_CHECKBOX_KEY     = "enable_cellBroadcast";
    private static final String BUTTON_CB_SETTINGS_KEY     = "cbsettings";
    private static final String LOG_TAG = "Settings/CellBroadcastActivity";
    int mSimId = PhoneConstants.GEMINI_SIM_1;
    
    private CellBroadcastCheckBox mCBCheckBox = null;
    private Preference mCBSetting = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Xlog.d(LOG_TAG, "[action = " + action + "]");
            if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                setScreenEnabled();
            }
        }
    };
     
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.cell_broad_cast);
        mSimId = getIntent().getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, 0);
        
        mCBCheckBox = (CellBroadcastCheckBox)findPreference(BUTTON_CB_CHECKBOX_KEY);
        mCBSetting = findPreference(BUTTON_CB_SETTINGS_KEY);
        mCBCheckBox.setSummary(mCBCheckBox.isChecked() 
                ? R.string.sum_cell_broadcast_control_on : R.string.sum_cell_broadcast_control_off);
        
        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME)) {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }
        
        if (mCBCheckBox != null) {
            mCBCheckBox.init(this, false, mSimId);
        }

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE); 
        registerReceiver(mReceiver, intentFilter);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        if (preference == mCBSetting) {
            Intent intent = new Intent(this, CellBroadcastSettings.class);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            this.startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void setScreenEnabled() {
        List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
        if (insertSim.size() == 0) {
            finish();
        } else if (insertSim.size() == 1) {
            if (mSimId != insertSim.get(0).mSlot) {
                finish();
            }
        }
    }
}
