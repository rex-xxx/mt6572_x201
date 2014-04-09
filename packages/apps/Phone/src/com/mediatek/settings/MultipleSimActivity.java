package com.mediatek.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.SettingsExtension;
import com.mediatek.phone.vt.VTCallUtils;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MultipleSimActivity extends PreferenceActivity 
    implements Preference.OnPreferenceChangeListener {
    //Give the sub items type(ListPreference, CheckBoxPreference, Preference)
    private String mItemType = "PreferenceScreen";
    public static final String  INTENT_KEY = "ITEM_TYPE";
    public static final String SUB_TITLE_NAME = "sub_title_name";
    
    //Used for PreferenceScreen to start a activity
    public static final String  TARGET_CALSS = "TARGET_CLASS";
    
    //Used for ListPreference to initialize the list
    public static final String INIT_ARRAY = "INIT_ARRAY";
    public static final String INIT_ARRAY_VALUE = "INIT_ARRAY_VALUE";
    
    //Most time, we get the sim number by Framework's api, but we firstly check how many sim support a special feature
    //For example, although two sim inserted, but there's only one support the VT
    public static final String INIT_SIM_NUMBER = "INIT_SIM_NUMBER";
    public static final String INIT_FEATURE_NAME = "INIT_FEATURE_NAME";
    public static final String INIT_TITLE_NAME = "INIT_TITLE_NAME";
    public static final String INIT_SIM_ID = "INIT_SIM_ID";
    public static final String INIT_BASE_KEY = "INIT_BASE_KEY";
    private static final int MODEM_MASK_TDSCDMA = 0x08;

    //VT private:
    private static final String SELECT_DEFAULT_PICTURE    = "0";
    private static final String SELECT_MY_PICTURE         = "2";
    private static final String SELECT_DEFAULT_PICTURE2    = "0";
    private static final String SELECT_MY_PICTURE2         = "1";

    private static final int PROGRESS_DIALOG = 100;
    private static final int ALERT_DIALOG = 200;
    private static final int ALERT_DIALOG_DEFAULT = 300;

    
    private int mVTWhichToSave = 0;
    private int mVTSimId = 0;
    
    private int mSimNumbers = 0;
    private String mTargetClass = null;
    private String mFeatureName;
    private int mTitleName;
    private int mListTitle;
    private int mListEntries;
    private int mListValues;
    //for the key of checkbox and listpreference: mBaseKey + cardSlot || simId
    private String mBaseKey;
    private Phone mPhone = null;
    private List<SIMInfo> mSimList;
    private long[] mSimIds = null;
    private HashMap<Object, Integer> mPref2CardSlot = new HashMap<Object, Integer>();
    private static final String TAG = "MultipleSimActivity";
    private static final boolean DBG = true;
    
    public static final String VT_FEATURE_NAME = "VT";
    public static final String NETWORK_MODE_NAME = "NETWORK_MODE";
    public static final String LIST_TITLE = "LIST_TITLE_NAME";
    
    private ImageView mImage;
    private Bitmap mBitmap;
    private PreCheckForRunning mPreCheckForRunning;
    private TelephonyManagerEx mTelephonyManagerEx;
    private TelephonyManager mTelephonyManager;
    
    private IntentFilter mIntentFilter;
    private final MultipleSimReceiver mReceiver = new MultipleSimReceiver();
    
    private SettingsExtension mExtension;

    private class MultipleSimReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PhoneGlobals.NETWORK_MODE_CHANGE_RESPONSE.equals(action)) {
                removeDialog(PROGRESS_DIALOG);
                int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, 0);
                log("BroadcastReceiver  slotId = " + slotId);
                if (!intent.getBooleanExtra(PhoneGlobals.NETWORK_MODE_CHANGE_RESPONSE, true)) {
                    log("BroadcastReceiver: network mode change failed! restore the old value.");
                    int oldMode = intent.getIntExtra(PhoneGlobals.OLD_NETWORK_MODE, 0);
                    android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                            getNetworkModeName(slotId), oldMode);
                    log("BroadcastReceiver, oldMode = " + oldMode);
                    if (NETWORK_MODE_NAME.equals(mFeatureName)) {
                        log("setValue  to oldMode ");
                        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                            Preference p = getPreferenceScreen().getPreference(i);
                            if (slotId == mPref2CardSlot.get(p)) {
                                ((ListPreference)p).setValue(Integer.toString(oldMode));
                                break;
                            }
                        }
                    }
                } else {
                    log("BroadcastReceiver: network mode change success! set to the new value.");
                    //android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                    //        getNetworkModeName(slotId), intent.getIntExtra("NEW_NETWORK_MODE", 0));
                    log("BroadcastReceiver  = " + intent.getIntExtra("NEW_NETWORK_MODE", 0));
                }
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)
                        || Intent.ACTION_DUAL_SIM_MODE_CHANGED.equals(action)) {
                updatePreferenceEnableState();
            } else if (TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY.equals(action)) {
                if ("NETWORK_SEARCH".equals(mFeatureName)) {
                    mExtension.removeNMOpForMultiSim(mPhone, mSimList, mTargetClass);
                    Collections.sort(mSimList, new CallSettings.SIMInfoComparable());
                    createSubItems();
                }
            } else if (TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED.equals(action)) {
                updatePreferenceList();
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for hot swap {
                Xlog.d(TAG,"ACTION_SIM_INFO_UPDATE received");
                setSimList();
                PreferenceScreen prefSet = getPreferenceScreen();
                prefSet.removeAll();
                addPreferencesFromResource(R.xml.multiple_sim);
                createSubItems();
                updatePreferenceList();
                updatePreferenceEnableState();
                ///@}
            }
        }
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            log("onCallStateChanged ans state is " + state);
            switch(state) {
            case TelephonyManager.CALL_STATE_IDLE:
                updatePreferenceEnableState();
                break;
            default:
                break;
            }
        }
    };

   
    private void log(String msg) {
        if (DBG) {
            Xlog.d(TAG, msg);
        }
    }
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mSimIds = getIntent().getLongArrayExtra(INIT_SIM_ID);
        mTargetClass = getIntent().getStringExtra(TARGET_CALSS);
        mFeatureName = getIntent().getStringExtra(INIT_FEATURE_NAME);
        mBaseKey = getIntent().getStringExtra(INIT_BASE_KEY);

        mTitleName = getIntent().getIntExtra(INIT_TITLE_NAME, -1);
        mListTitle = getIntent().getIntExtra(LIST_TITLE, -1);
        mListEntries = getIntent().getIntExtra(INIT_ARRAY, -1);
        mListValues = getIntent().getIntExtra(INIT_ARRAY_VALUE, -1);
        String itemType = getIntent().getStringExtra(INTENT_KEY);
        if (itemType != null) {
            mItemType = itemType;
        }
        
        mPhone = PhoneGlobals.getPhone();
        mTelephonyManagerEx = new TelephonyManagerEx(this);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        setSimList();
        skipUsIfNeeded();
        addPreferencesFromResource(R.xml.multiple_sim);
        createSubItems();

        mExtension = ExtensionManager.getInstance().getSettingsExtension();
        mExtension.removeNMOpForMultiSim(mPhone, mSimList, mTargetClass);
        mIntentFilter =
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        if (NETWORK_MODE_NAME.equals(mFeatureName)) {
            mIntentFilter.addAction(PhoneGlobals.NETWORK_MODE_CHANGE_RESPONSE);
        }
        registerReceiver(mReceiver, mIntentFilter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume(); 
        updatePreferenceList();
        updatePreferenceEnableState();
        if (mTitleName > 0) {
            setTitle(mTitleName);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        log("-----------[onSaveInstanceState]-----------");
        super.onSaveInstanceState(outState);
        outState.putInt(INIT_TITLE_NAME, mTitleName);
        outState.putInt(LIST_TITLE, mListTitle);
        outState.putInt(INIT_ARRAY, mListEntries);
        outState.putInt(INIT_ARRAY_VALUE, mListValues);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        log("-----------[onRestoreInstanceState]-----------");
        super.onRestoreInstanceState(savedInstanceState);
        mTitleName = savedInstanceState.getInt(INIT_TITLE_NAME, -1);
        mListTitle = savedInstanceState.getInt(LIST_TITLE, -1);
        mListEntries = savedInstanceState.getInt(INIT_ARRAY, -1);
        mListValues = savedInstanceState.getInt(INIT_ARRAY_VALUE, -1);
    }

    //We will consider the feature and the number inserted sim then 
    //skip us and enter the setting directly
    private void skipUsIfNeeded() {
        //For VT and Network mode we will pay special handler, so we don't skip
        if (VT_FEATURE_NAME.equals(mFeatureName) || NETWORK_MODE_NAME.equals(mFeatureName)) {
            return;
        }
        
        if (mSimNumbers == 1 && mTargetClass != null) {
            Intent intent = new Intent();
            int position = mTargetClass.lastIndexOf('.');
            String pkName = mTargetClass.substring(0, position);
            pkName = pkName.replace("com.mediatek.settings", "com.android.phone");
            intent.setAction(Intent.ACTION_MAIN);
            int slotId = mSimList.get(0).mSlot;
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, slotId);
            if (FeatureOption.EVDO_DT_SUPPORT) {
                customizeForEVDO(intent, slotId, pkName, mTargetClass);
            } else {
                intent.setClassName(pkName, mTargetClass);
                checkToStart(slotId, intent);
            }
            finish();
        }
    }
    
    private void createSubItems() {
        PreferenceScreen prefSet = getPreferenceScreen();
        ArrayList<String> keys = new ArrayList<String>();
        for (int i = 0; i < prefSet.getPreferenceCount(); ++i) {
            String c = prefSet.getPreference(i).getKey();
            if (!c.startsWith(mItemType)) {
                keys.add(prefSet.getPreference(i).getKey());
            }
        }
        
        for (String s : keys) {
            prefSet.removePreference(prefSet.findPreference(s));
        }
        
        int prefCount = prefSet.getPreferenceCount();
        
        for (int i = prefCount - 1; i > mSimNumbers - 1; --i) {
            prefSet.removePreference(prefSet.getPreference(i));
        }
        
        if (mItemType.equals("PreferenceScreen")) {
            initPreferenceScreen();
        } else if (mItemType.equals("CheckBoxPreference")) {
            initCheckBoxPreference();
        } else if (mItemType.equals("ListPreference")) {
            initListPreference();
        }
    }
    
    private void initPreferenceScreen() {
        PreferenceScreen prefSet = getPreferenceScreen();
        for (int i = 0; i < mSimNumbers; ++i) {
            SimPreference p = (SimPreference)prefSet.getPreference(i);
            p.setTitle(mSimList.get(i).mDisplayName);
            p.setSimColor(mSimList.get(i).mColor);
            p.setSimSlot(mSimList.get(i).mSlot);
            p.setSimName(mSimList.get(i).mDisplayName);
            p.setSimNumber(mSimList.get(i).mNumber);
            p.setSimIconNumber(getProperOperatorNumber(mSimList.get(i)));
            mPref2CardSlot.put(prefSet.getPreference(i), Integer.valueOf(mSimList.get(i).mSlot));
        }
    }
    
    private void initCheckBoxPreference() {
        PreferenceScreen prefSet = getPreferenceScreen();
        for (int i = 0; i < mSimNumbers; ++i) {
            String key = null;
            CheckSimPreference p = (CheckSimPreference) prefSet.getPreference(i);
            p.setTitle(mSimList.get(i).mDisplayName);
            p.setSimColor(mSimList.get(i).mColor);
            p.setSimSlot(mSimList.get(i).mSlot);
            p.setSimName(mSimList.get(i).mDisplayName);
            p.setSimNumber(mSimList.get(i).mNumber);
            p.setSimIconNumber(getProperOperatorNumber(mSimList.get(i)));
            
            if (mBaseKey != null && mBaseKey.endsWith("@")) {
                key = mBaseKey.substring(0, mBaseKey.length() - 1);
                key = key + "_" + mSimList.get(i).mSlot;
                p.setKey(key);
            }
            mPref2CardSlot.put(prefSet.getPreference(i), Integer.valueOf(mSimList.get(i).mSlot));

            SharedPreferences sp = 
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            p.setChecked(sp.getBoolean(key, !key.startsWith("button_vt_auto_dropback_key")));
            p.setOnPreferenceChangeListener(this);
        }
    }
    
    private void initListPreference() {
        PreferenceScreen prefSet = getPreferenceScreen();
        for (int i = 0; i < mSimNumbers; ++i) {
            String key = null;
            ListSimPreference listPref = (ListSimPreference)prefSet.getPreference(i);
            listPref.setTitle(mSimList.get(i).mDisplayName);
            listPref.setSimColor(mSimList.get(i).mColor);
            listPref.setSimSlot(mSimList.get(i).mSlot);
            listPref.setSimName(mSimList.get(i).mDisplayName);
            listPref.setSimNumber(mSimList.get(i).mNumber);
            listPref.setSimIconNumber(getProperOperatorNumber(mSimList.get(i)));

            if (mBaseKey != null && mBaseKey.endsWith("@")) {
                key = mBaseKey.substring(0, mBaseKey.length() - 1);
                key = key + "_" + mSimList.get(i).mSlot;
                listPref.setKey(key);
            }
            mPref2CardSlot.put(prefSet.getPreference(i), Integer.valueOf(mSimList.get(i).mSlot));
            
            if (mListTitle > 0) {
                listPref.setDialogTitle(mListTitle);
            }
            
            if (mListEntries > 0) {
                int entryLocal = getNetworkModeEntry(mSimList.get(i).mSlot);
                if (NETWORK_MODE_NAME.equals(mFeatureName)) {
                    listPref.setEntries(entryLocal); 
                } else {
                    listPref.setEntries(mListEntries);    
                }
            }
            if (mListValues > 0) {
                int entryValueLocal = getNetworkModeEntryValues(mSimList.get(i).mSlot);
                if (NETWORK_MODE_NAME.equals(mFeatureName)) {
                    listPref.setEntryValues(entryValueLocal); 
                } else {
                    listPref.setEntryValues(mListValues);   
                }
            }
            SharedPreferences sp = 
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            listPref.setValue(sp.getString(key, "0"));
            listPref.setOnPreferenceChangeListener(this);

            if (NETWORK_MODE_NAME.equals(mFeatureName)) {
                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(), getNetworkModeName(mSimList.get(i).mSlot), 0);
                log("settingsNetworkMode in provider = " + settingsNetworkMode);
                /// M: add for wcdma prefer feature
                if(settingsNetworkMode == Phone.NT_MODE_GSM_UMTS){
                    settingsNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                }
                log("settingsNetworkMode init value = " + settingsNetworkMode);
                listPref.setValue(Integer.toString(settingsNetworkMode));
            }
        }
    }
    
    private int getNetworkModeEntry(int slot) {
        if ((CallSettings.getBaseband(slot) & MODEM_MASK_TDSCDMA) != 0) {
            return R.array.gsm_umts_network_preferences_choices_cmcc;
        } else {
            return R.array.gsm_umts_network_preferences_choices;
        }
    } 
    private int getNetworkModeEntryValues(int slot) {
        if ((CallSettings.getBaseband(slot) & MODEM_MASK_TDSCDMA) != 0) {
            return R.array.gsm_umts_network_preferences_values_cmcc;
        } else {
            return R.array.gsm_umts_network_preferences_values;
        }
    } 

    private boolean isNeededToCheckLock() {
        if ("com.mediatek.settings.IpPrefixPreference".equals(mTargetClass)) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        PreferenceScreen prefSet = getPreferenceScreen();

        GeminiPhone dualPhone = null;
        if (mPhone instanceof GeminiPhone) {
            dualPhone = (GeminiPhone)mPhone;
        }
         
        for (int i = 0; i < prefSet.getPreferenceCount(); i++) {
            if ((preference == prefSet.getPreference(i)) 
                    && (mTargetClass != null) && (dualPhone != null)) {
                int slotId = mPref2CardSlot.get(preference);
                if (dualPhone.isRadioOnGemini(slotId)) {
                    Intent intent = new Intent();
                    int position = mTargetClass.lastIndexOf('.');
                    String pkName = mTargetClass.substring(0, position);
                    pkName = pkName.replace("com.mediatek.settings", "com.android.phone");
                    intent.setClassName(pkName, mTargetClass);
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, slotId);
                    intent.putExtra(
                            SUB_TITLE_NAME, SIMInfo.getSIMInfoBySlot(this, slotId).mDisplayName);
                    if (mFeatureName != null && mFeatureName.equals("VT")) {
                        intent.putExtra("ISVT", true);
                    }
                    
                    if (FeatureOption.EVDO_DT_SUPPORT) {
                        customizeForEVDO(intent, slotId, pkName, mTargetClass);
                    } else {
                        intent.setClassName(pkName, mTargetClass);
                        checkToStart(slotId, intent);
                    }
                } 
            }
        }
        return false;
    }
    
    public void checkAllowedRun(Intent intent, Preference preference) {
        int slot = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, 0);
        GeminiPhone dualPhone = null;
        if (mPhone instanceof GeminiPhone) {
            dualPhone = (GeminiPhone)mPhone;
        }
    }

    private String getProperOperatorNumber(SIMInfo info) {
        String res = null;
        int charCount = 4;
        if (info == null) {
            return res;
        }
        res = info.mNumber;
        switch (info.mDispalyNumberFormat) {
        case SimInfo.DISPALY_NUMBER_NONE:
            res = "";
            break;
        case SimInfo.DISPLAY_NUMBER_FIRST:
            if (res != null && res.length() > 4) {
                res = res.substring(0, charCount);
            }
            break;
        case SimInfo.DISPLAY_NUMBER_LAST:
            if (res != null && res.length() > 4) {
                res = res.substring(res.length() - charCount, res.length());
            }
            break;
        default:
            res = "";
            break;
        }
        return res;
    }
    
    public int getNetworkMode(int buttonNetworkMode, int slotId) {
        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(), getNetworkModeName(slotId), 0);
        int modemNetworkMode = settingsNetworkMode;
        if (buttonNetworkMode != settingsNetworkMode) {
            switch(buttonNetworkMode) {
                case Phone.NT_MODE_GLOBAL:
                    modemNetworkMode = Phone.NT_MODE_GLOBAL;
                    break;
                case Phone.NT_MODE_EVDO_NO_CDMA:
                    modemNetworkMode = Phone.NT_MODE_EVDO_NO_CDMA;
                    break;
                case Phone.NT_MODE_CDMA_NO_EVDO:
                    modemNetworkMode = Phone.NT_MODE_CDMA_NO_EVDO;
                    break;
                case Phone.NT_MODE_CDMA:
                    modemNetworkMode = Phone.NT_MODE_CDMA;
                    break;
                case Phone.NT_MODE_GSM_UMTS:
                    modemNetworkMode = Phone.NT_MODE_GSM_UMTS;
                    break;
                case Phone.NT_MODE_WCDMA_ONLY:
                    modemNetworkMode = Phone.NT_MODE_WCDMA_ONLY;
                    break;
                case Phone.NT_MODE_GSM_ONLY:
                    modemNetworkMode = Phone.NT_MODE_GSM_ONLY;
                    break;
                case Phone.NT_MODE_WCDMA_PREF:
                    modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                    break;
                default:
                    modemNetworkMode = Phone.PREFERRED_NT_MODE;
            }
        }
        android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                getNetworkModeName(slotId), modemNetworkMode);
        return modemNetworkMode;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO Auto-generated method stub
        int slotId = mPref2CardSlot.get(preference);

        Xlog.d(TAG,"[slotId = " + slotId + "]");
        Xlog.d(TAG,"[newValue = " + newValue + "]");
        Xlog.d(TAG,"[key = " + preference.getKey() + "]");
        Xlog.d(TAG,"[mFeatureName = " + mFeatureName + "]");
        if (VT_FEATURE_NAME.equals(mFeatureName)) {
            VTCallUtils.checkVTFile(slotId);
            if (String.valueOf("button_vt_replace_expand_key_" + slotId)
                    .equals(preference.getKey())) {
                mVTWhichToSave = 0;
                mVTSimId = slotId;
                if (newValue.toString().equals(SELECT_DEFAULT_PICTURE)) {
                    showDialogPic(VTAdvancedSetting.getPicPathDefault(), ALERT_DIALOG_DEFAULT);
                } else if (newValue.toString().equals(SELECT_MY_PICTURE)) {
                    showDialogPic(VTAdvancedSetting.getPicPathUserselect(slotId), ALERT_DIALOG);
                }
                
            } else if (String.valueOf("button_vt_replace_peer_expand_key_" + slotId)
                    .equals(preference.getKey())) {
                mVTSimId = slotId;
                mVTWhichToSave = 1;
                if (newValue.toString().equals(SELECT_DEFAULT_PICTURE2)) {
                    showDialogPic(VTAdvancedSetting.getPicPathDefault2(), ALERT_DIALOG_DEFAULT);
                } else if (newValue.toString().equals(SELECT_MY_PICTURE2)) {
                    showDialogPic(VTAdvancedSetting.getPicPathUserselect2(slotId), ALERT_DIALOG);
                }
            }
            
        } else if (NETWORK_MODE_NAME.equals(mFeatureName)) {
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(), getNetworkModeName(slotId), 0);
            log("Current network mode = " + settingsNetworkMode);
            int networkMode = getNetworkMode(Integer.valueOf((String) newValue).intValue(), slotId);
            log("new network mode = " + networkMode);
            if (settingsNetworkMode != networkMode) {
                Intent intent = new Intent(PhoneGlobals.NETWORK_MODE_CHANGE, null);
                intent.putExtra(PhoneGlobals.OLD_NETWORK_MODE, settingsNetworkMode);
                intent.putExtra(PhoneGlobals.NETWORK_MODE_CHANGE, networkMode);
                intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, slotId);
                showDialog(PROGRESS_DIALOG);
                sendBroadcast(intent);
            }
        }
        return true;
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
        log("[onCreateDialog][" + id + "]");
        Dialog dialog = null;
        Xlog.d(TAG,"[mBitmap = " + mBitmap + "]");
        Xlog.d(TAG,"[mImage = " + mImage + "]");
        if (mBitmap == null || mImage == null) {
            return dialog;
        }

        switch(id) {
        case PROGRESS_DIALOG:
            dialog = new ProgressDialog(this);
            ((ProgressDialog)dialog).setMessage(getText(R.string.updating_settings));
            ((ProgressDialog)dialog).setCancelable(false);
            ((ProgressDialog)dialog).setIndeterminate(true);
            break;
        case ALERT_DIALOG:
            dialog = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.vt_change_my_pic, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                            intent.setType("image/*");
                            intent.putExtra("crop", "true");
                            intent.putExtra("aspectX", 1);
                            intent.putExtra("aspectY", 1);
                            intent.putExtra("outputX", getResources().getInteger(R.integer.qcif_x));
                            intent.putExtra("outputY", getResources().getInteger(R.integer.qcif_y));
                            intent.putExtra("return-data", true);
                            intent.putExtra("scaleUpIfNeeded", true);
                            startActivityForResult(intent, 
                                    VTAdvancedSetting.REQUESTCODE_PICTRUE_PICKED_WITH_DATA);
                        } catch (ActivityNotFoundException e) {
                            log("Pictrue not found , Gallery ActivityNotFoundException !");
                        }
                    }})
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }})
                .create();
            ((AlertDialog)dialog).setView(mImage);
            dialog.setTitle(getResources().getString(R.string.vt_pic_replace_local_mypic));                
            dialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mImage.setImageBitmap(null);
                    if (!mBitmap.isRecycled()) {
                        mBitmap.recycle();
                    }
                    removeDialog(ALERT_DIALOG);
                }
            });
            break;
        case ALERT_DIALOG_DEFAULT:
            dialog = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }})
                .create();
            ((AlertDialog)dialog).setView(mImage);
            dialog.setTitle(getResources().getString(R.string.vt_pic_replace_local_default));                
            dialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mImage.setImageBitmap(null);
                    if (!mBitmap.isRecycled()) {
                        mBitmap.recycle();
                    }
                    removeDialog(ALERT_DIALOG_DEFAULT);
                }
            });
            break;
        default:
            break;
        }
        dialog.show();
        return dialog;
    }

    private void showDialogPic(String filename, int dialog) {
        mImage = new ImageView(this);
        mBitmap = BitmapFactory.decodeFile(filename);
        mImage.setImageBitmap(mBitmap);
        showDialog(dialog);
        Xlog.d(TAG,"[showDialogPic][filename = " + filename + "]");
        Xlog.d(TAG,"[showDialogPic][mBitmap = " + mBitmap + "]");
        Xlog.d(TAG,"[showDialogPic][mImage = " + mImage + "]");
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log("onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        
        if (resultCode != RESULT_OK) {
            return;
        }
        
        switch (requestCode) {
        case VTAdvancedSettingEx.REQUESTCODE_PICTRUE_PICKED_WITH_DATA:
            try {
                final Bitmap bitmap = data.getParcelableExtra("data");
                if (bitmap != null) {
                    if (mVTWhichToSave == 0) {
                        VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect(mVTSimId), bitmap);
                    } else {
                        VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect2(mVTSimId), bitmap);
                    }
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mVTWhichToSave == 0) {
                showDialogPic(VTAdvancedSetting.getPicPathUserselect(mVTSimId), ALERT_DIALOG);  
            } else {
                showDialogPic(VTAdvancedSetting.getPicPathUserselect2(mVTSimId), ALERT_DIALOG);  
            }
            break;
        default:
            break;
        }
    }
    
    protected void onDestroy() {
        super.onDestroy();
        if (mPreCheckForRunning != null) {
            mPreCheckForRunning.deRegister();
        }
        unregisterReceiver(mReceiver);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
    }
    
    private void updatePreferenceEnableState() {
        PreferenceScreen prefSet = getPreferenceScreen();
        
        //For single sim or only one sim inserted, we couldn't go here
        GeminiPhone dualPhone = null;
        if (mPhone instanceof GeminiPhone) {
            dualPhone = (GeminiPhone)mPhone;
        }
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        for (int i = 0; i < prefSet.getPreferenceCount(); ++i) {
            Preference p = prefSet.getPreference(i);
            if (dualPhone != null) {
                if (NETWORK_MODE_NAME.equals(mFeatureName)) {
                    p.setEnabled(dualPhone.isRadioOnGemini(mPref2CardSlot.get(p)) && isIdle);
                } else {
                    p.setEnabled(dualPhone.isRadioOnGemini(mPref2CardSlot.get(p)));
                }
                if (!p.isEnabled()) {
                    if (p instanceof ListPreference) {
                        Dialog dialog = ((ListPreference)p).getDialog();
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                }
            }
        }
    }

    private void customizeForEVDO(Intent intent, int slotId, String pkName, String targetClass) {
        if (slotId == 0) {
            intent.setClassName(pkName, mTargetClass);
            checkToStart(slotId, intent);
            return;
        }
        if ("com.android.phone.GsmUmtsCallForwardOptions".equals(targetClass)) {
            intent.setClassName(pkName, "com.mediatek.settings.CdmaCallForwardOptions");
            checkToStart(slotId, intent);
        } else if ("com.android.phone.GsmUmtsAdditionalCallOptions".equals(targetClass)) {
            intent.setClassName(pkName, "com.mediatek.settings.CdmaCallWaitingOptions");
            checkToStart(slotId, intent);
        } else if ("com.mediatek.settings.FdnSetting2".equals(targetClass)
            || "com.mediatek.settings.CallBarring".equals(targetClass)
            || "com.android.phone.NetworkSetting".equals(mTargetClass)
            || "com.mediatek.settings.PLMNListPreference".equals(mTargetClass)) {
            Toast.makeText(this, getResources()
                    .getString(R.string.cdma_not_support), Toast.LENGTH_LONG).show();
        } else {
            intent.setClassName(pkName, mTargetClass);
            checkToStart(slotId, intent);
        }    
    }

    private void checkToStart(int slotId, Intent intent) {
        if (isNeededToCheckLock()) {
            if (mPreCheckForRunning == null) {
                mPreCheckForRunning = new PreCheckForRunning(this);
            }
            mPreCheckForRunning.checkToRun(intent, slotId, 302);
        } else {
            startActivity(intent);
        }
    }

    private void updatePreferenceList() {
        log("---------[update mutiple list views]---------");
        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.invalidateViews();
    }

    private void setSimList() {
        Xlog.d(TAG,"[simlist = " + mSimList + "]");
        Xlog.d(TAG,"[mSimIds = " + mSimIds + "]");
        if (mSimList == null) {
            mSimList = new ArrayList<SIMInfo>();
        }
        if (mSimIds != null) {
            mSimList.clear();
            for (int i = 0; i < mSimIds.length; ++i) {
                if (isSimInserted(mSimIds[i])) {
                    mSimList.add(SIMInfo.getSIMInfoById(this, mSimIds[i]));
                }
            }
        } else {
            mSimList = SIMInfo.getInsertedSIMList(this);
        }
        mSimNumbers = mSimList.size();
        Collections.sort(mSimList, new CallSettings.SIMInfoComparable());
        if (mSimNumbers == 0) {
            Xlog.d(TAG,"Activity finished");
            finish();
        }
    }

    private boolean isSimInserted(long simId) {
        boolean isInserted = false;
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
        for (SIMInfo simInfo : simList) {
            if (simInfo.mSimId == simId) {
                isInserted = true;
            }
        }
        return isInserted;
    }

    private String getNetworkModeName(int slotId) {
        String name = android.provider.Settings.Global.PREFERRED_NETWORK_MODE;
        switch (slotId) {
            case PhoneConstants.GEMINI_SIM_1:
                break;
            case PhoneConstants.GEMINI_SIM_2:
                name = android.provider.Settings.Global.PREFERRED_NETWORK_MODE_2;
                break;
            default:
                break;
        }
        return name;
    }
}
