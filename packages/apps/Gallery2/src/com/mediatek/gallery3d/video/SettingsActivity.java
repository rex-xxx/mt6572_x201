package com.mediatek.gallery3d.video;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.telephony.SimInfoManager;
import android.view.WindowManager;

import com.android.gallery3d.R;
import com.android.gallery3d.app.Log;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.gallery3d.util.MtkLog;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "Gallery2/VideoPlayer/SettingsActivity";
    private static final boolean LOG = true;
    
    private static final String PREF_KEY_APN = "apn_settings";
    private static final String PREF_KEY_ENABLE_RTSP_PROXY = "enable_rtsp_proxy";
    private static final String PREF_KEY_RTSP_PROXY = "rtsp_proxy_settings";
    private static final String PREF_KEY_ENABLE_HTTP_PROXY = "enable_http_proxy";
    private static final String PREF_KEY_HTTP_PROXY = "http_proxy_settings";
    private static final String PREF_KEY_UDP_PORT = "udp_port_settings";
    
    private static final String SETTING_KEY_RTSP_PROXY_ENABLED = MediaStore.Streaming.Setting.RTSP_PROXY_ENABLED;
    private static final String SETTING_KEY_RTSP_PROXY_HOST = MediaStore.Streaming.Setting.RTSP_PROXY_HOST;
    private static final String SETTING_KEY_RTSP_PROXY_PORT = MediaStore.Streaming.Setting.RTSP_PROXY_PORT;
    
    private static final String SETTING_KEY_HTTP_PROXY_ENABLED = MediaStore.Streaming.Setting.HTTP_PROXY_ENABLED;
    private static final String SETTING_KEY_HTTP_PROXY_HOST = MediaStore.Streaming.Setting.HTTP_PROXY_HOST;
    private static final String SETTING_KEY_HTTP_PROXY_PORT = MediaStore.Streaming.Setting.HTTP_PROXY_PORT;
    
    private static final String SETTING_KEY_MAX_PORT = MediaStore.Streaming.Setting.MAX_UDP_PORT;
    private static final String SETTING_KEY_MIN_PORT = MediaStore.Streaming.Setting.MIN_UDP_PORT;
    
    private static final int UNKNOWN_PORT = -1;
    
    private static final String ACTION_APN = "android.settings.APN_SETTINGS";
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
    
    private Preference mApnPref;
    private CheckBoxPreference mRtspProxyEnabler;
    private Preference mRtspProxyPref;
    private CheckBoxPreference mHttpProxyEnabler;
    private Preference mHttpProxyPref;
    private Preference mUdpPortPref;
    private ConnectivityManager mCM;
    
    private ProxyDialog mProxyDialog;
    private AlertDialog mUdpDialog;
    private ContentResolver mCr;
    private IntentFilter mMobileStateFilter;
    
    private static final String PREF_KEY_HTTP_BUFFER_SIZE = "http_buffer_size";
    private static final String PREF_KEY_RTSP_BUFFER_SIZE = "rtsp_buffer_size";
    private static final String KEY_HTTP_BUFFER_SIZE = "MTK-HTTP-CACHE-SIZE";
    private static final String KEY_RTSP_BUFFER_SIZE = "MTK-RTSP-CACHE-SIZE";
    private static final int DEFAULT_HTTP_BUFFER_SIZE = 10;//seconds
    private static final int DEFAULT_RTSP_BUFFER_SIZE = 6;//seconds
    private Preference mBufferSizeHttpPref;
    private Preference mBufferSizeRtspPref;
    
    public static final String KEY_LOGO_BITMAP = "logo-bitmap";
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOG) {
            MtkLog.v(TAG, "onCreate");
        }
        Bitmap logo = getIntent().getParcelableExtra(KEY_LOGO_BITMAP);
        if (logo != null) {
            getActionBar().setLogo(new BitmapDrawable(getResources(), logo));
        }

        addPreferencesFromResource(R.xml.movie_settings);
        mApnPref = findPreference(PREF_KEY_APN);
        mRtspProxyEnabler = (CheckBoxPreference)findPreference(PREF_KEY_ENABLE_RTSP_PROXY);
        mRtspProxyPref = findPreference(PREF_KEY_RTSP_PROXY);
        mHttpProxyEnabler = (CheckBoxPreference)findPreference(PREF_KEY_ENABLE_HTTP_PROXY);
        mHttpProxyPref = findPreference(PREF_KEY_HTTP_PROXY);
        mUdpPortPref = findPreference(PREF_KEY_UDP_PORT);
        mBufferSizeHttpPref = findPreference(PREF_KEY_HTTP_BUFFER_SIZE);
        mBufferSizeRtspPref = findPreference(PREF_KEY_RTSP_BUFFER_SIZE);
        
        mCM = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        mCr = this.getContentResolver();
        
        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mMobileStateFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        mMobileStateFilter.addAction(TRANSACTION_START);
        mMobileStateFilter.addAction(TRANSACTION_STOP);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshApn();
        refreshRtspProxy();
        refreshHttpProxy();
        refreshUdpPort();
        refreshBufferSizeHttp();
        refreshBufferSizeRtsp();
        registerReceiver(mMobileStateReceiver, mMobileStateFilter);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mMobileStateReceiver);
    }

    private void refreshApn() {
        refreshSIMInfo();
        if (mSimInfo == null) {
            mApnPref.setEnabled(false);
            mApnPref.setSummary(R.string.apn_settings_not_valid);
        } else {
            mApnPref.setEnabled(true);
            mApnPref.setSummary(String.format(
                getString(R.string.apn_settings_summary), mSimInfo.mDisplayName, getApnName()));
        }
    }
    
    private void refreshRtspProxy() {
        final boolean enableProxy = (Settings.System.getInt(mCr, SETTING_KEY_RTSP_PROXY_ENABLED, 0) == 1);
        final String host = Settings.System.getString(mCr, SETTING_KEY_RTSP_PROXY_HOST);
        final int port = Settings.System.getInt(mCr, SETTING_KEY_RTSP_PROXY_PORT, UNKNOWN_PORT);
        if (enableProxy && host != null && host.length() != 0 && port != UNKNOWN_PORT) {
            mRtspProxyPref.setSummary(host + ":" + port);
        } else {
            mRtspProxyPref.setSummary(R.string.rtsp_proxy_settings_summary);
        }
        mRtspProxyEnabler.setChecked(enableProxy);
        if (LOG) {
            MtkLog.v(TAG, "refreshRtspProxy() enableProxy=" + enableProxy + ", host=" + host + ", mPort=" + port);
        }
    }
    
    private void refreshHttpProxy() {
        final boolean enableProxy = (Settings.System.getInt(mCr, SETTING_KEY_HTTP_PROXY_ENABLED, 0) == 1);
        final String host = Settings.System.getString(mCr, SETTING_KEY_HTTP_PROXY_HOST);
        final int port = Settings.System.getInt(mCr, SETTING_KEY_HTTP_PROXY_PORT, UNKNOWN_PORT);
        if (enableProxy && host != null && host.length() != 0 && port != UNKNOWN_PORT) {
            mHttpProxyPref.setSummary(host + ":" + port);
        } else {
            mHttpProxyPref.setSummary(R.string.http_proxy_settings_summary);
        }
        mHttpProxyEnabler.setChecked(enableProxy);
        if (LOG) {
            MtkLog.v(TAG, "refreshHttpProxy() enableProxy=" + enableProxy + ", host=" + host + ", mPort=" + port);
        }
    }
    
    private void refreshUdpPort() {
        final int minport = Settings.System.getInt(mCr, MediaStore.Streaming.Setting.MIN_UDP_PORT, UNKNOWN_PORT);
        final int maxport = Settings.System.getInt(mCr, MediaStore.Streaming.Setting.MAX_UDP_PORT, UNKNOWN_PORT);
        if (minport != UNKNOWN_PORT && maxport != UNKNOWN_PORT && maxport >= minport) {
            mUdpPortPref.setSummary(minport + " - " + maxport);
        } else {
            mUdpPortPref.setSummary(R.string.udp_port_settings_summary);
        }
        if (LOG) {
            MtkLog.v(TAG, "refreshUdpPort() maxport=" + maxport + ", minport=" + minport);
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (LOG) {
            MtkLog.v(TAG, "onPreferenceTreeClick(" + preference + ")");
        }
        if (preference == mApnPref) {
            showApnDialog();
        } else if (preference == mRtspProxyPref) {
            showProxyDialog(ProxyDialog.TYPE_RTSP);
        } else if (preference == mHttpProxyPref) {
            showProxyDialog(ProxyDialog.TYPE_HTTP);
        } else if (preference == mUdpPortPref) {
            showUdpPortDialog();
        } else if (preference == mRtspProxyEnabler) {
            final boolean enable = mRtspProxyEnabler.isChecked();
            Settings.System.putInt(mCr, SETTING_KEY_RTSP_PROXY_ENABLED, (enable ? 1 : 0));
            refreshRtspProxy();
        } else if (preference == mHttpProxyEnabler) {
            final boolean enable = mHttpProxyEnabler.isChecked();
            Settings.System.putInt(mCr, SETTING_KEY_HTTP_PROXY_ENABLED, (enable ? 1 : 0));
            refreshHttpProxy();
        } else if (preference == mBufferSizeHttpPref) {
            showBufferSizeHttpDialog();
        } else if (preference == mBufferSizeRtspPref) {
            showBufferSizeRtspDialog();
        }
        return true;
    }
    
    private void showUdpPortDialog() {
        if (mUdpDialog != null) {
            mUdpDialog.dismiss();
        }
        mUdpDialog = new PortDialog(this);
        mUdpDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mUdpDialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(final DialogInterface dialog) {
                refreshUdpPort();
            }
            
        });
        mUdpDialog.show();
    }
    
    private void showApnDialog() {
        if (mSimInfo != null) {
            final Intent intent = new Intent();
            intent.setAction(ACTION_APN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra("simId", mSimInfo.mSimSlotId);
            startActivity(intent);
        } else {
            refreshApn();
        }
    }
    
    private void showProxyDialog(final int type) {
        if (mProxyDialog != null) {
            mProxyDialog.dismiss();
        }
        mProxyDialog = new ProxyDialog(this, type);
        mProxyDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mProxyDialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(final DialogInterface dialog) {
                if (ProxyDialog.TYPE_RTSP == mProxyDialog.getType()) {
                    refreshRtspProxy();
                } else {
                    refreshHttpProxy();
                }
            }
            
        });
        mProxyDialog.show();
    }
    
    //APN info
    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SIM_CARD_SINGLE = 2;
    private static final int SIM_CARD_UNDEFINED = -1;
    
    public static final String RESTORE_CARRIERS_URI = "content://telephony/carriers/restore";
    public static final String RESTORE_CARRIERS_URI_GEMINI = "content://telephony/carriers_gemini/restore";
    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    public static final String PREFERRED_APN_URI_GEMINI = "content://telephony/carriers_gemini/preferapn";

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri DEFAULTAPN_URI_GEMINI = Uri
            .parse(RESTORE_CARRIERS_URI_GEMINI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);
    private static final Uri PREFERAPN_URI_GEMINI = Uri
            .parse(PREFERRED_APN_URI_GEMINI);
    
    private Uri mUri;
    private Uri mDefaultApnUri;
    private Uri mRestoreCarrierUri;
    
    private String getQueryWhere() {
        String where = "";
        if (isGemini()) {
            final int slotid = (int)mSimInfo.mSimSlotId;
            switch (slotid) {
                case SIM_CARD_1:
                    mUri = Telephony.Carriers.CONTENT_URI;
                    mRestoreCarrierUri = PREFERAPN_URI;
                    where = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
                    break;
                case SIM_CARD_2:
                    mUri = Telephony.Carriers.GeminiCarriers.CONTENT_URI;
                    mRestoreCarrierUri = PREFERAPN_URI_GEMINI;
                    where = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "-1");
                    break;
                case SIM_CARD_SINGLE:
                    mUri = Telephony.Carriers.CONTENT_URI;
                    mRestoreCarrierUri = PREFERAPN_URI;
                    where = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "");
                    break;
                default:
                    MtkLog.w(TAG, "Can't get any valid SIM information");
                    break;
            }
        } else {
            mUri = Telephony.Carriers.CONTENT_URI;
            mRestoreCarrierUri = PREFERAPN_URI;
            where = SystemProperties.get("gsm.sim.operator.numeric", "");
        }
        MtkLog.v(TAG, "getQueryWhere() mUri=" + mUri);
        MtkLog.v(TAG, "getQueryWhere() mRestoreCarrierUri=" + mRestoreCarrierUri);
        MtkLog.v(TAG, "getQueryWhere() where=" + where);
        return where;
    }
    
    private String getApnName() {
        final String where = "numeric=\"" + getQueryWhere() + "\"";
        Cursor cursor = null;
        String name = null;
        try {
            cursor = mCr.query(
                mUri,
                new String[] { "_id", "name" },
                where,
                null, Telephony.Carriers.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                final int key = getSelectedApnKey();
                if (key != -1) {
                    while (cursor.moveToNext()) {
                        if (key == cursor.getInt(0)) {
                            name = cursor.getString(1);
                            break;
                        }
                    }
                }
            }
        } catch (final SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "getApnName() return " + name);
        }
        return name;
    }
    
    private int getSelectedApnKey() {
        int key = -1;
        Cursor cursor = null;
        try {
            cursor = mCr.query(
                mRestoreCarrierUri, 
                new String[] {"_id"},
                null,
                null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);
            if (cursor != null && cursor.moveToFirst()) {
                key = cursor.getInt(0);
            }
        } catch (final SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "getSelectedApnKey() key=" + key);
        }
        return key;
    }
    
    private SimInfoRecord mSimInfo;
    private void refreshSIMInfo() {
        mSimInfo = null;
        if (isGemini()) {
            //get simid from data connection.
            final long simid = Settings.System.getLong(mCr, 
                    Settings.System.GPRS_CONNECTION_SIM_SETTING,
                    Settings.System.DEFAULT_SIM_NOT_SET);
            mSimInfo = SimInfoManager.getSimInfoById(this, simid);
        } else {
            final List<SimInfoRecord> list = SimInfoManager.getAllSimInfoList(this);
            if (list != null && list.size() > 0) {
                mSimInfo = list.get(0);
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "refreshSIMInfo() mSimInfo=" + mSimInfo);
        }
        if (LOG && mSimInfo != null) {
            MtkLog.i(TAG, "refreshSIMInfo() simid=" + mSimInfo.mSimInfoId + ", slot=" + mSimInfo.mSimSlotId
                    + ", displayName=" + mSimInfo.mDisplayName);
        }
    }
    
    private boolean isGemini() {
        final boolean gemini = com.mediatek.common.featureoption.FeatureOption.MTK_GEMINI_SUPPORT;
        if (LOG) {
            MtkLog.v(TAG, "isGemini() return " + gemini);
        }
        return gemini;
    }
    
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (LOG) {
                MtkLog.v(TAG, "mMobileStateReceiver.onReceive(" + intent + ")");
            }
            refreshApn();
        }
    };
    
    private void refreshBufferSizeHttp() {
        final int bufferSize = Settings.System.getInt(mCr, KEY_HTTP_BUFFER_SIZE, DEFAULT_HTTP_BUFFER_SIZE);
        mBufferSizeHttpPref.setSummary(getString(R.string.http_buffer_size_text, bufferSize));
        if (LOG) {
            Log.i(TAG, "refreshBufferSizeHttp() bufferSize=" + bufferSize);
        }
    }
    
    private void refreshBufferSizeRtsp() {
        final int bufferSize = Settings.System.getInt(mCr, KEY_RTSP_BUFFER_SIZE, DEFAULT_RTSP_BUFFER_SIZE);
        mBufferSizeRtspPref.setSummary(getString(R.string.rtsp_buffer_size_text, bufferSize));
        if (LOG) {
            Log.i(TAG, "refreshBufferSizeRtsp() bufferSize=" + bufferSize);
        }
    }
    
    private void showBufferSizeHttpDialog() {
        showBufferSizeDialog(LimitDialog.TYPE_HTTP);
    }
    
    private void showBufferSizeRtspDialog() {
        showBufferSizeDialog(LimitDialog.TYPE_RTSP);
    }
    
    private void showBufferSizeDialog(final int type) {
        final LimitDialog limitDialog = new LimitDialog(this, type);
        limitDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        limitDialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(final DialogInterface dialog) {
                if (type == LimitDialog.TYPE_HTTP) {
                    refreshBufferSizeHttp();
                } else {
                    refreshBufferSizeRtsp();
                }
            }
            
        });
        limitDialog.show();
    }
}
