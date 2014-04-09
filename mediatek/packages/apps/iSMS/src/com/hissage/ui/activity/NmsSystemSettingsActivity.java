package com.hissage.ui.activity;

import java.util.Date;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hissage.R;
import com.hissage.api.NmsIpMessageApiNative;
import com.hissage.api.NmsStartActivityApi;
import com.hissage.config.NmsCommonUtils;
import com.hissage.config.NmsConfig;
import com.hissage.config.NmsProfileSettings;
import com.hissage.jni.engineadapter;
import com.hissage.message.ip.NmsIpMessageConsts;
import com.hissage.platfrom.NmsPlatformAdapter;
import com.hissage.service.NmsService;
import com.hissage.struct.SNmsSimInfo;
import com.hissage.struct.SNmsSimInfo.NmsSimActivateStatus;
import com.hissage.struct.SNmsSystemStatus;
import com.hissage.ui.view.NmsPreferenceForProfile;
import com.hissage.ui.view.NmsSwitchPreference;
import com.hissage.util.data.NmsConsts;
import com.hissage.util.data.NmsDateTool;
import com.hissage.util.log.NmsLog;

public class NmsSystemSettingsActivity extends PreferenceActivity {
    private static final String TAG = "NmsSystemSettingsActivity";

    private class SNmsSimInfoMTK {
        long sim_id = NmsConsts.INVALID_SIM_ID;
        String simName = null;
        String phone = null;
        int color = -1;
        boolean simEnable = false;
    }

    private SNmsSimInfoMTK[] mSimInfo = new SNmsSimInfoMTK[NmsConsts.SIM_CARD_COUNT];

    private static final String KEY_PROFILE = "nms_profile";
    private static final String KEY_SENDASSMS = "nms_send_as_sms";
    private static final String KEY_CAPTION = "nms_caption";
    private static final String KEY_AUTODOWNLOAD = "nms_auto_download";
    private static final String KEY_NETWORKUSAGE = "nms_network_usage";
    // private static final String KEY_UPDATE = "nms_software_update";
    private static final String KEY_SIM_INFO_CATE = "nms_sim_info";
    private static final String KEY_SIM_INFO1 = "nms_sim1";
    private static final String KEY_SIM_INFO2 = "nms_sim2";
    private static final String KEY_PREFRENCE_CATE = "nms_preferences";
    private static final String KEY_ABOUT = "nms_about";
    private static final String KEY_READ_STATUS = "nms_show_read_status";
    private static final String KEY_REMINDERS = "nms_show_reminders";

    private NmsPreferenceForProfile mProfile = null;
    private PreferenceCategory mSimInfoCategory = null;
    private CheckBoxPreference mSimInfo1 = null;
    private CheckBoxPreference mSimInfo2 = null;
    private PreferenceCategory mPrefrenceCategory = null;
    private CheckBoxPreference mSendAsSms = null;
    private NmsSwitchPreference mCaptions = null;
    private CheckBoxPreference mAutoDownload = null;
    private Preference mNetworkUsage = null;
    private Preference mAbout = null;
    // private boolean mIgnore = false;
    private CheckBoxPreference mReadStatus = null;
    private CheckBoxPreference mReminders = null;
    private NmsSetProfileResultRecver mResultRecver = null;
    private AlertDialog mActivationDlg = null;
    private ProgressDialog mWaitDlg = null;
    private long mSimId = NmsConsts.INVALID_SIM_ID;
    private  boolean isWaitDlg = false ;
    private  int soltId = -1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
            case 0xffff:
                if (mWaitDlg != null) {
                    mWaitDlg.cancel();
                    mWaitDlg = null;
                }
                checkSimCardStatus();

                // updateStatus();
                return;
            default:
                // do nothing
                break;
            }
        }
    };

    private void getAllSimInfo() {

        for (int i = 0; i < NmsConsts.SIM_CARD_COUNT; ++i) {
            if (null == mSimInfo[i]) {
                mSimInfo[i] = new SNmsSimInfoMTK();
            }
            mSimInfo[i].sim_id = NmsPlatformAdapter.getInstance(this).getSimIdBySlotId(i);
            if (mSimInfo[i].sim_id > 0) {
                mSimInfo[i].simName = NmsPlatformAdapter.getInstance(this).getSimName(
                        mSimInfo[i].sim_id);
                mSimInfo[i].color = NmsPlatformAdapter.getInstance(this).getSimColor(
                        mSimInfo[i].sim_id);
                SNmsSimInfo sim = NmsIpMessageApiNative
                        .nmsGetSimInfoViaSimId((int) mSimInfo[i].sim_id);
                if (null != sim) {
                    mSimInfo[i].simEnable = NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED == sim.status;
                    mSimInfo[i].phone = sim.number;
                } else {
                    // do nothing
                }
            } else {
                // do nothing
            }
        }
    }

    private void showWaitDlg() {

        if (mWaitDlg != null) {
            return;
        }
        mWaitDlg = new ProgressDialog(this);
        mWaitDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWaitDlg.setMessage(getString(R.string.STR_NMS_WAIT));
        mWaitDlg.setIndeterminate(false);
        mWaitDlg.setCancelable(false);
        mWaitDlg.show();
        new Thread() {
            public void run() {
                try { 
                	isWaitDlg = true ;
                    sleep(10000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mHandler.sendEmptyMessage(0xffff);
            }
        }.start();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.resetSetting) {
            resetDefaultSetting();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private Drawable getAvatar(String filepath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(filepath, options);
        options.inJustDecodeBounds = false;
        int l = Math.max(options.outHeight, options.outWidth);
        int be = (int) (l / 500);
        if (be <= 0)
            be = 1;
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(filepath, options);
        if (null != bitmap) {
            return new BitmapDrawable(bitmap);
        } else {
        	bitmap = BitmapFactory.decodeResource(this.getResources(),
                    R.drawable.contact_default_avatar);
        	 NmsLog.error(TAG, "can not parse profile setting avatar file: " + filepath);
        	 if(null != bitmap){
        		 return new BitmapDrawable(bitmap);
        	 }else{
        		 NmsLog.error(TAG, "can not parse default avatar");
        		 return null;
        	 }
        }
    }

    private void resetDefaultSetting() {
        NmsConfig.setSendAsSMSFlag(0);

        NmsConfig.setCaptionFlag(0);
        NmsConfig.setAudioCaptionFlag(0);
        NmsConfig.setVideoCaptionFlag(0);
        NmsConfig.setPhotoCaptionFlag(0);

        NmsConfig.setAutoDownloadFlag(0);

        engineadapter.get().nmsUISetShowReadStatus(0);

        NmsConfig.setShowRemindersFlag(0);
        updateStatus();
    }

    private void checkSimCardStatus() {
        boolean enable = mSimInfo[soltId].simEnable;
        long simId = NmsPlatformAdapter.getInstance(this).getSimIdBySlotId(soltId);

        if ((simId > 0 && ((NmsIpMessageApiNative.nmsGetActivationStatus((int) simId) == NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED) == enable))) {
            if (enable) {
                // disable failed
                Toast.makeText(this, R.string.STR_NMS_DISABLE_FAILED, Toast.LENGTH_SHORT).show();
            } else {
                // enable failed
                Toast.makeText(this, R.string.STR_NMS_ENABLE_FAILED, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (enable) {
                // disable success
                Toast.makeText(this, R.string.STR_NMS_DISABLE_SUCCESS, Toast.LENGTH_SHORT).show();

            } else {
                // enable success
                Toast.makeText(this, R.string.STR_NMS_ENABLE_SUCCESS, Toast.LENGTH_SHORT).show();

            }
        }
        isWaitDlg = false ;
        updateStatus();
    }

    private void updateStatus() {
    	if (!isWaitDlg) {
    		getAllSimInfo();
    	}

        long simId1 = NmsPlatformAdapter.getInstance(this).getSimIdBySlotId(
                NmsConsts.SIM_CARD_SLOT_1);
        long simId2 = NmsPlatformAdapter.getInstance(this).getSimIdBySlotId(
                NmsConsts.SIM_CARD_SLOT_2);

        if ((simId1 > 0 && NmsIpMessageApiNative.nmsGetActivationStatus((int) simId1) == NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED)
                || (simId2 > 0 && NmsIpMessageApiNative.nmsGetActivationStatus((int) simId2) == NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED)) {
            mPrefrenceCategory.setEnabled(true);
            mNetworkUsage.setEnabled(true);
        } else {
            mPrefrenceCategory.setEnabled(false);
            mNetworkUsage.setEnabled(false);
        }

        NmsProfileSettings profile = engineadapter.get().nmsUIGetUserInfo();
        if (null != profile) {
            if (!TextUtils.isEmpty(profile.name)) {
                mProfile.setTitle(profile.name);
            }
            if (!TextUtils.isEmpty(profile.fileName)
                    && NmsCommonUtils.isExistsFile(profile.fileName)) {
                mProfile.setIcon(getAvatar(profile.fileName));
            } else {
                mProfile.setIcon(R.drawable.contact_default_avatar);
            }
        }

        mSendAsSms.setChecked(NmsConfig.getSendAsSMSFlag());

        mCaptions.setChecked(NmsConfig.getCaptionFlag());

        mAutoDownload.setChecked(NmsConfig.getAutoDownloadFlag());

        mReadStatus.setChecked(engineadapter.get().nmsUIGetShowReadStatus() != 0);

        mReminders.setChecked(NmsConfig.getShowRemindersFlag());

        if (mSimInfo[NmsConsts.SIM_CARD_SLOT_1].sim_id > 0) {
            mSimInfoCategory.addPreference(mSimInfo1);
            mSimInfo1.setEnabled(true);
            if (TextUtils.isEmpty(mSimInfo[NmsConsts.SIM_CARD_SLOT_1].simName)) {
                mSimInfo1.setTitle("");
            } else {
                mSimInfo1.setTitle(mSimInfo[NmsConsts.SIM_CARD_SLOT_1].simName);
            }
            mSimInfo1.setSummary(mSimInfo[NmsConsts.SIM_CARD_SLOT_1].phone);
            mSimInfo1.setChecked(mSimInfo[NmsConsts.SIM_CARD_SLOT_1].simEnable);
            mSimInfo1.setIcon(mSimInfo[NmsConsts.SIM_CARD_SLOT_1].color);
        } else {
            mSimInfoCategory.removePreference(mSimInfo1);
        }

        if (mSimInfo[NmsConsts.SIM_CARD_SLOT_2].sim_id > 0) {
            mSimInfoCategory.addPreference(mSimInfo2);
            mSimInfo2.setEnabled(true);
            if (TextUtils.isEmpty(mSimInfo[NmsConsts.SIM_CARD_SLOT_2].simName)) {
                mSimInfo2.setTitle("");
            } else {
                mSimInfo2.setTitle(mSimInfo[NmsConsts.SIM_CARD_SLOT_2].simName);
            }
            mSimInfo2.setSummary(mSimInfo[NmsConsts.SIM_CARD_SLOT_2].phone);
            mSimInfo2.setChecked(mSimInfo[NmsConsts.SIM_CARD_SLOT_2].simEnable);
            mSimInfo2.setIcon(mSimInfo[NmsConsts.SIM_CARD_SLOT_2].color);
        } else {
            mSimInfoCategory.removePreference(mSimInfo2);
        }

    }

    private void init() {
        mProfile = (NmsPreferenceForProfile) findPreference(KEY_PROFILE);
        mSimInfoCategory = (PreferenceCategory) findPreference(KEY_SIM_INFO_CATE);
        mSimInfo1 = (CheckBoxPreference) findPreference(KEY_SIM_INFO1);
        mSimInfo2 = (CheckBoxPreference) findPreference(KEY_SIM_INFO2);

        mSendAsSms = (CheckBoxPreference) findPreference(KEY_SENDASSMS);
        mCaptions = (NmsSwitchPreference) findPreference(KEY_CAPTION);
        mAutoDownload = (CheckBoxPreference) findPreference(KEY_AUTODOWNLOAD);
        mNetworkUsage = findPreference(KEY_NETWORKUSAGE);
        mPrefrenceCategory = (PreferenceCategory) findPreference(KEY_PREFRENCE_CATE);
        mAbout = findPreference(KEY_ABOUT);
        mReadStatus = (CheckBoxPreference) findPreference(KEY_READ_STATUS);
        mReminders = (CheckBoxPreference) findPreference(KEY_REMINDERS);

    }

    private void showNetUsage() {
        long lastTime = NmsConfig.getClearFlowTime();
        String time = getString(R.string.STR_NMS_NEVER_CLEAR);
        SNmsSystemStatus sysStatus = engineadapter.get().nmsUIGetSystemStatus();
        if (null == sysStatus) {
            NmsLog.error(TAG, "get system status error");
            return;
        }
        if (lastTime > 0) {
            time = NmsDateTool.getDateFormat(this).format(new Date(lastTime));
            time += (" " + DateFormat.getTimeFormat(this).format(new Date(lastTime)));
        }

        String dlgContent = String.format(getString(R.string.STR_NMS_NETWORK_DLG_CONTENT),
                (float) ((float) sysStatus.sendBytes) / 1024,
                (float) ((float) sysStatus.recvBytes / 1024), time);

        new AlertDialog.Builder(this).setTitle(R.string.STR_NMS_NETWORK_USAGE_TITLE)
                .setMessage(dlgContent)
                .setPositiveButton(R.string.STR_NMS_CANCEL, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).setNegativeButton(R.string.STR_NMS_RESET, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        NmsConfig.setClearFlowTime(System.currentTimeMillis());
                        engineadapter.get().nmsUIClearFlow();
                    }
                }).create().show();

    }

    private class MyURLSpan extends ClickableSpan {
        @Override
        public void onClick(View widget) {
            if (mActivationDlg != null) {
                mActivationDlg.dismiss();
                mActivationDlg = null;
            }
            Intent i = new Intent(NmsSystemSettingsActivity.this, NmsTermActivity.class);
            i.putExtra(NmsConsts.SIM_ID, mSimId);
            startActivity(i);
        }
    }

    protected void showActivitionDlg(final long sim_id, int mode) {
        mSimId = sim_id;

        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.alert_dialog_text_view, null);

        TextView textView = (TextView) view.findViewById(R.id.term_textview);

        String termContent = getString(NmsBaseActivity.WELCOME == mode ? R.string.STR_NMS_TERM_WARN_WELCOME
                : R.string.STR_NMS_TERM_WARN_ACTIVATE);
        SpannableString ss = new SpannableString(termContent);

        ss.setSpan(new URLSpan("noting"),
                termContent.indexOf(getString(R.string.STR_NMS_MENU_LICENSE_AGREEMENT)), termContent.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ((TextView) textView).setText(ss);

        textView.setMovementMethod(LinkMovementMethod.getInstance());
        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            int end = text.length();
            Spannable sp = (Spannable) textView.getText();
            URLSpan[] urls = sp.getSpans(0, end, URLSpan.class);
            SpannableStringBuilder style = new SpannableStringBuilder(text);
            style.clearSpans();// should clear old spans
            for (URLSpan url : urls) {
                MyURLSpan myURLSpan = new MyURLSpan();
                style.setSpan(myURLSpan, sp.getSpanStart(url), sp.getSpanEnd(url),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(style);
        }

        mActivationDlg = new AlertDialog.Builder(this)
                .setTitle(
                        NmsBaseActivity.WELCOME == mode ? R.string.STR_NMS_WELCOME_ACTIVE
                                : R.string.STR_NMS_ACTIVE)
                .setView(view)
                .setPositiveButton(R.string.STR_NMS_AGREE_AND_CONTINUE,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                NmsStartActivityApi.nmsStartActivitionActivity(
                                        NmsSystemSettingsActivity.this, (int) sim_id);
                            }
                        }).setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                }).create();
        mActivationDlg.show();

    }

    private void regRecver() {
        mResultRecver = new NmsSetProfileResultRecver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NmsProfileSettings.CMD_RESULT_KEY);
        filter.addAction(NmsIpMessageConsts.NmsSimStatus.NMS_SIM_STATUS_ACTION);
        registerReceiver(mResultRecver, filter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setLogo(R.drawable.isms);

        addPreferencesFromResource(R.xml.system_settings);
        init();
        updateStatus();

        regRecver();

    }
    @Override
    public void onPause(){
    	super.onPause() ;

    }
    @Override
    public void onResume() {
        super.onResume();
        updateStatus();

    }

    @Override
    public void onDestroy() {
        if (mResultRecver != null) {
            unregisterReceiver(mResultRecver);
        }
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mProfile) {
            Intent i = new Intent(this, NmsProfileSettingsActivity.class);
            this.startActivity(i);
        } else if (preference == mSimInfo1) {
            getAllSimInfo() ;
            boolean isChecked = mSimInfo1.isChecked();

            SNmsSimInfo info = NmsIpMessageApiNative
                    .nmsGetSimInfoViaSimId((int) mSimInfo[NmsConsts.SIM_CARD_SLOT_1].sim_id);
            if (null == info) {
                mSimInfo1.setChecked(false);
                Toast.makeText(this, R.string.STR_NMS_ENABLE_FAILED, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (isChecked && info.status < NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED) {
                mSimInfo1.setChecked(false);
                if (NmsCommonUtils.isNetworkReady(this)) {
                    showActivitionDlg(mSimInfo[NmsConsts.SIM_CARD_SLOT_1].sim_id,
                            NmsBaseActivity.WELCOME);
                } else {
                    Toast.makeText(this, R.string.STR_NMS_NO_CONNECTION, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            soltId = 0;
            if (isChecked) {
                if (NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED != info.status) {
                    NmsIpMessageApiNative
                            .nmsEnableIpService((int) mSimInfo[NmsConsts.SIM_CARD_SLOT_1].sim_id);
                    showWaitDlg();
                }
            } else {
                if (NmsSimActivateStatus.NMS_SIM_STATUS_DISABLED != info.status) {
                    NmsIpMessageApiNative
                            .nmsDisableIpService((int) mSimInfo[NmsConsts.SIM_CARD_SLOT_1].sim_id);
                    showWaitDlg();
                }
            }
        } else if (preference == mSimInfo2) {
            getAllSimInfo() ;
            boolean isChecked = mSimInfo2.isChecked();
            SNmsSimInfo info = NmsIpMessageApiNative
                    .nmsGetSimInfoViaSimId((int) mSimInfo[NmsConsts.SIM_CARD_SLOT_2].sim_id);
            if (null == info) {
                mSimInfo2.setChecked(false);
                Toast.makeText(this, R.string.STR_NMS_ENABLE_FAILED, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (isChecked && info.status < NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED) {
                mSimInfo2.setChecked(false);
                if (NmsCommonUtils.isNetworkReady(this)) {
                    showActivitionDlg(mSimInfo[NmsConsts.SIM_CARD_SLOT_2].sim_id,
                            NmsBaseActivity.WELCOME);
                } else {
                    Toast.makeText(this, R.string.STR_NMS_NO_CONNECTION, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            soltId = 1;
            if (isChecked) {
                if (NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED != info.status) {
                    NmsIpMessageApiNative
                            .nmsEnableIpService((int) mSimInfo[NmsConsts.SIM_CARD_SLOT_2].sim_id);
                    showWaitDlg();
                }
            } else {
                if (NmsSimActivateStatus.NMS_SIM_STATUS_DISABLED != info.status) {
                    NmsIpMessageApiNative
                            .nmsDisableIpService((int) mSimInfo[NmsConsts.SIM_CARD_SLOT_2].sim_id);
                    showWaitDlg();
                }
            }
        } else if (preference == mAutoDownload) {
            NmsConfig.setAutoDownloadFlag(mAutoDownload.isChecked() ? 1 : 0);
        } else if (preference == mReadStatus) {
            engineadapter.get().nmsUISetShowReadStatus(mReadStatus.isChecked() ? 1 : 0);
        } else if (preference == mReminders) {
            NmsConfig.setShowRemindersFlag(mReminders.isChecked() ? 0 : 1);
        } else if (preference == mCaptions) {
            Intent i = new Intent(this, NmsCaptionSettingsActivity.class);
            this.startActivity(i);
            return false;
        } else if (preference == mSendAsSms) {
            NmsConfig.setSendAsSMSFlag(mSendAsSms.isChecked() ? 0 : 1);
        } else if (preference == mNetworkUsage) {
            showNetUsage();
        } else if (preference == mAbout) {
            Intent i = new Intent(this, NmsAboutActivity.class);
            this.startActivity(i);
        }
        return true;
    }

    public class NmsSetProfileResultRecver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent || !NmsProfileSettings.CMD_RESULT_KEY.equals(intent.getAction())) {
                NmsLog.error(TAG, "recv error intent: " + intent);
                return;
            }

            NmsLog.error(TAG, "recv intent action: " + intent.getAction());
            if (intent.getAction().equals(NmsIpMessageConsts.NmsSimStatus.NMS_SIM_STATUS_ACTION)) {
                mHandler.sendEmptyMessage(0xffff);
            } else {
                int cmdCode = intent.getIntExtra(NmsProfileSettings.CMD_CODE, -1);
                if (NmsProfileSettings.CMD_SET_HESINE_INFO_ACK == cmdCode) {
                    int errorCode = intent.getIntExtra(NmsProfileSettings.CMD_RESULT_KEY, -1000);
                    if (0 == errorCode) {
                        Toast.makeText(NmsService.getInstance(), R.string.STR_NMS_SET_PROFILE_OK,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(NmsService.getInstance(), R.string.STR_NMS_SET_PROFILE_FAIL,
                                Toast.LENGTH_SHORT).show();
                    }
                }

                updateStatus();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.system_setting_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

}
