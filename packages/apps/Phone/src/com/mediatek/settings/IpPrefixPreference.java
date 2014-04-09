package com.mediatek.settings;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.R;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class IpPrefixPreference extends PreferenceActivity implements OnPreferenceChangeListener, TextWatcher {
    private static final String IP_PREFIX_NUMBER_EDIT_KEY = "button_ip_prefix_edit_key";
    private static final String TAG = "IpPrefixPreference";
    private EditTextPreference mButtonIpPrefix = null;
    private int mSlot = -1;
    private String mInitTitle = null;
    ///M: add for hot swap {
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(context);
                if (temp.size() == 0) {
                    Xlog.d(TAG, "Activity finished");
                    CallSettings.goUpToTopLevelSetting(IpPrefixPreference.this);
                } else if (CallSettings.isMultipleSim() && temp.size() == 1) {
                    if (temp.get(0).mSlot != mSlot) {
                        Xlog.d(TAG, "temp.size()=" + temp.size()
                                + "Activity finished");
                        finish();
                    }
                }
            }
        }
    };
    ///@}
    protected void onCreate(Bundle icicle) {
        
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.ip_prefix_setting);
        mButtonIpPrefix = (EditTextPreference)this.findPreference(IP_PREFIX_NUMBER_EDIT_KEY);
        mButtonIpPrefix.setOnPreferenceChangeListener(this);
        
        mSlot = getIntent().getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
        
        mInitTitle = getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME);
        if (mInitTitle != null) {
            this.setTitle(mInitTitle);
        }
        ///M: add for hot swap {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        registerReceiver(mReceiver, mIntentFilter);
        ///@}
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    protected void onResume() {
        super.onResume();
        String preFix = this.getIpPrefix(mSlot);
        if ((preFix != null) && (!"".equals(preFix))) {
            mButtonIpPrefix.setSummary(preFix);
            mButtonIpPrefix.setText(preFix);
        } else {
            mButtonIpPrefix.setSummary(R.string.ip_prefix_edit_default_sum);
            mButtonIpPrefix.setText("");
        }
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

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        this.mButtonIpPrefix.setSummary(newValue.toString());
        mButtonIpPrefix.setText(newValue.toString());
        if (newValue == null || "".equals(newValue)) {
            mButtonIpPrefix.setSummary(R.string.ip_prefix_edit_default_sum);
        }
        saveIpPrefix(newValue.toString());
        return false;
    }
    
    private void saveIpPrefix(String str) {
        String key = "ipprefix";
        if (CallSettings.isMultipleSim()) {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, mSlot);
            if (info != null) {
                key += Long.valueOf(info.mSimId).toString();
            }
        }
        if (!Settings.System.putString(this.getContentResolver(), key, str)) {
            Log.d("IpPrefixPreference", "Store ip prefix error!");
        }
    }
    
    private String getIpPrefix(int slot) {
        String key = "ipprefix";
        if (CallSettings.isMultipleSim()) {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, mSlot);
            if (info != null) {
                key += Long.valueOf(info.mSimId).toString();
            }
        }
        
        return Settings.System.getString(this.getContentResolver(),key);
    }
    
    public void beforeTextChanged(CharSequence s, int start,
            int count, int after) {
        
    }
    
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        
    }
    
    public void afterTextChanged(Editable s) {
        
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        ///M:add for hot swap {
        unregisterReceiver(mReceiver);
        ///@}
    }
}
