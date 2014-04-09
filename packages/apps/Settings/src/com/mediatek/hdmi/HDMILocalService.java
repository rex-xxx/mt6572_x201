package com.mediatek.hdmi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;

import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.hdmi.IHDMINative;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.Arrays;


public class HDMILocalService extends Service {
    private static final String TAG = "hdmi";
    private static final String LOCAL_TAG = " >> HDMILocalService.";
    // Action broadcast to HDMI settings and other App
    public static final String ACTION_CABLE_STATE_CHANGED = "com.mediatek.hdmi.localservice.action.CABLE_STATE_CHANGED";
    public static final String ACTION_EDID_UPDATED = "com.mediatek.hdmi.localservice.action.EDID_UPDATED";
    public static final String ACTION_IPO_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN_IPO";
    public static final String ACTION_IPO_BOOTUP = "android.intent.action.ACTION_BOOT_IPO";

    public static final String KEY_HDMI_ENABLE_STATUS = "hdmi_enable_status";
    public static final String KEY_HDMI_AUDIO_STATUS = "hdmi_audio_status";
    public static final String KEY_HDMI_VIDEO_STATUS = "hdmi_video_status";
    public static final String KEY_HDMI_VIDEO_RESOLUTION = "hdmi_video_resolution";
    public static final String KEY_HDMI_COLOR_SPACE = "hdmi_color_space";
    public static final String KEY_HDMI_DEEP_COLOR = "hdmi_deep_color";
    private static final int SLEEP_TIME = 140;
    // add for MFR
    private IHDMINative mHdmiNative = null;
    private static boolean sIsRunning = true;
    private static boolean sIsCablePluged = false;
    private static int sWiredHeadSetPlugState = 0;
    private static boolean sIsCallStateIdle = true;

    public static int[] sEdid = null;
    public static int[] sEdidPrev = null;

    /*
     * flag for cache audio status, when phone state changed, restore or load
     * cached status, enable(true) or disable(false)
     */
    private static boolean sHMDIAudioTargetState = true;
    private static boolean sHMDITargetState = true;
    private static boolean sHMDIVideoTargetState = true;

    private AudioManager mAudioManager = null;
    private TelephonyManager mTelephonyManager = null;
    private static PowerManager.WakeLock sWakeLock = null;

    public class LocalBinder extends Binder {
        /**
         * 
         * @return the service
         */
        public HDMILocalService getService() {
            return HDMILocalService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();
    private HDMIServiceReceiver mReceiver = null;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, ">>HDMILocalService.onBind()");
        return mBinder;
    }

    private class HDMIServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_HDMI_PLUG.equals(action)) {
                int hdmiCableState = intent.getIntExtra("state", 0);
                sIsCablePluged = (hdmiCableState == 1);
                dealWithCablePluged();
            } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                Log.e(TAG,"receive the headset plugin and do nothing");
                //sWiredHeadSetPlugState = intent.getIntExtra("state", 0);
                //dealWithHeadSetChanged();
            } else if (ACTION_IPO_BOOTUP.equals(action)) {
                Log.e(TAG,"HDMI local service receive IPO boot up broadcast");
                dealWithIPO(true);
            } else if (ACTION_IPO_SHUTDOWN.equals(action)) {
                dealWithIPO(false);
            }
        }
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.i(TAG, LOCAL_TAG + " Phone state changed, new state=" + state);
            if (state != TelephonyManager.CALL_STATE_IDLE) {
                sIsCallStateIdle = false;
                dealWithCallStateChanged();
                return;
            } else {
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    if (mTelephonyManager == null) {
                        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    }
                    TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
                    int sim1State = telephonyManagerEx.getCallState(PhoneConstants.GEMINI_SIM_1);
                    int sim2State = telephonyManagerEx.getCallState(PhoneConstants.GEMINI_SIM_2);
                    Log.e(TAG, LOCAL_TAG + "phone state change, sim1="
                            + sim1State + ", sim2=" + sim2State);
                    if (sim1State != TelephonyManager.CALL_STATE_IDLE
                            || sim2State != TelephonyManager.CALL_STATE_IDLE) {
                        Log.e(TAG, LOCAL_TAG
                                + " phone is not idle for gemini phone");
                        sIsCallStateIdle = false;
                        dealWithCallStateChanged();
                        return;
                    }
                }
            }
            sIsCallStateIdle = true;
            dealWithCallStateChanged();
        }

    };

    @Override
    public void onCreate() {
        Log.i(TAG, ">>HDMILocalService.onCreate()");
        mHdmiNative = MediatekClassFactory.createInstance(IHDMINative.class);
        if (mHdmiNative == null) {
            Log.e(TAG, "Native is not created");
        }
        if (mReceiver == null) {
            mReceiver = new HDMIServiceReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HDMI_PLUG);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(ACTION_IPO_BOOTUP);
        filter.addAction(ACTION_IPO_SHUTDOWN);
        registerReceiver(mReceiver, filter);

        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
        if (sWakeLock == null) {
            PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            sWakeLock = mPowerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK, "HDMILocalService");
        }
        if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            //0 in ioctl means success
            if (setHDCPKey()) {
                Log.i(TAG, "Set HDCP key fail!");
            } else {
                Log.i(TAG, "Set HDCP key successfully!");
            }
        }
        initHDMITargetState();        
        super.onCreate();
    }

    private void initHDMITargetState() {
        int initHDMIState = Settings.System.getInt(getContentResolver(),
                HDMILocalService.KEY_HDMI_ENABLE_STATUS, 1);
        int initHDMIAudioState = Settings.System.getInt(getContentResolver(),
                HDMILocalService.KEY_HDMI_AUDIO_STATUS, 1);
        int initHDMIVideoState = Settings.System.getInt(getContentResolver(),
                HDMILocalService.KEY_HDMI_VIDEO_STATUS, 0);
        Log.i(TAG, LOCAL_TAG + "initHDMITargetState(), initHDMIState="
                + initHDMIState + ", initHDMIAudioState=" + initHDMIAudioState
                + ", initHDMIVideoState=" + initHDMIVideoState);
        sHMDITargetState = (initHDMIState == 1);
        sHMDIAudioTargetState = (initHDMIAudioState == 1);
        sHMDIVideoTargetState = (initHDMIVideoState == 1);

        if (sHMDITargetState) {
            Log.i(TAG, LOCAL_TAG + " enable HDMI after boot up complete");
            enableHDMI(true);
        } else {
            Log.i(TAG, LOCAL_TAG + " disable HDMI after boot up complete");
            enableHDMI(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, ">>HDMILocalService.onStartCommand(), startId=" + startId
                + ": intent=" + intent);
        sIsRunning = true;
        if (intent != null) {
            String bootUpTypeAction = intent.getStringExtra("bootup_action");
            if (bootUpTypeAction != null) {
                // IPO boot up, need to resume HDMI driver
                if (bootUpTypeAction.equals(ACTION_IPO_BOOTUP)) {
                    Log
                            .i(TAG,
                                    "IPO boot up complete, try to resume HDMI driver status");
                    dealWithIPO(true);
                }
            }
        }
        return Service.START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        sIsRunning = false;
        // enableHDMI(false);
        if (mHdmiNative.ishdmiForceAwake() && (sWakeLock != null)
                && (sWakeLock.isHeld())) {
            sWakeLock.release();
        }
        unregisterReceiver(mReceiver);
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
        Log.i(TAG, ">>HDMILocalService.onDestroy()");
    }

    private static final String GET_HDMI_AUDIO_STATUS = "GetHDMIAudioStatus";
    private static final String HDMI_AUDIO_STATUS_ENABLED = "GetHDMIAudioStatus=true";
    private static final String HDMI_AUDIO_STATUS_DISABLED = "GetHDMIAudioStatus=false";
    private static final String SET_HDMI_AUDIO_ENABLED = "SetHDMIAudioEnable=1";
    private static final String SET_HDMI_AUDIO_DISABLED = "SetHDMIAudioEnable=0";
    private static final int    AP_CFG_RDCL_FILE_HDCP_KEY_LID = 42;

    /**
     * set whether enable the hdmi
     * 
     * @param enabled
     *            status
     * @return a native call enableHDMIImpl() to enable/disable hdmi
     */
    public boolean enableHDMI(boolean enabled) {
        Log.i(TAG, LOCAL_TAG + "enableHDMI(), new state=" + enabled);
        sHMDITargetState = enabled;
        return enableHDMIImpl(enabled);
    }

    private boolean enableHDMIImpl(boolean enabled) {
        Log.e(TAG, LOCAL_TAG + "enableHDMIImpl(), new state=" + enabled);
        return mHdmiNative.enableHDMI(enabled);
    }

    /**
     * Enable the audio or disable the auido
     * 
     * @param enabled
     *            the flag
     * @return update the audio state
     */
    public boolean enableAudio(boolean enabled) {
        Log.i(TAG, ">>HDMILocalService.enableAudio(), new state=" + enabled);
        sHMDIAudioTargetState = enabled;
        return updateAudioState();
    }

    private boolean enableAudioImp(boolean enabled) {
        Log.e(TAG, LOCAL_TAG + "enableAudioImp(" + enabled + ")");
        String state = null;
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        if (mAudioManager != null) {
            state = mAudioManager.getParameters(GET_HDMI_AUDIO_STATUS);
            if ((enabled && HDMI_AUDIO_STATUS_ENABLED.equals(state))
                    || (!enabled && HDMI_AUDIO_STATUS_DISABLED.equals(state))) {
                Log.i(TAG, LOCAL_TAG
                        + "  audio driver status is already what we need ["
                        + state + "]");
                return true;
            } else {
                if (mAudioManager.enableHdmiAudio(enabled)) {
                    Log.i(TAG, LOCAL_TAG + "enableAudio(" + enabled
                            + ") success");
                    return true;
                } else {
                    Log.i(TAG, LOCAL_TAG + "enableAudio(" + enabled
                            + ") fail, current state=" + state);
                    return false;
                }
            }
        } else {
            Log
                    .e(TAG,
                            ">>HDMILocalService.enableAudio(), fail to get AudioManager service");
            return false;
        }
    }

    /**
     * 
     * @param enabled
     *            status of video
     * @return true for successfully enable the video
     */
    public boolean enableVideo(boolean enabled) {
        Log.i(TAG, ">>HDMILocalService.enableVideo(), new state=" + enabled);
        sHMDIVideoTargetState = enabled;
        return enableVideoImp(enabled);
    }

    private boolean enableVideoImp(boolean enabled) {
        Log.e(TAG, ">>HDMILocalService.enableVideoImp, new state=" + enabled);
        return mHdmiNative.enableVideo(enabled);
    }

    public boolean enableCEC(boolean enabled) {
        Log.e(TAG, ">>HDMILocalService.enableCEC, new state=" + enabled);
        return mHdmiNative.enableCEC(enabled);
    }

    public void setCECAddr(byte laNum, byte[] la, char pa, char svc) {
        Log.e(TAG, ">>HDMILocalService.setCECAddr");
        mHdmiNative.setCECAddr(laNum, la, pa, svc);
    }

    public void setCECCmd(byte initAddr, byte destAddr, char opCode, byte[] operand, int size, byte enqueueOk) {
        Log.e(TAG, ">>HDMILocalService.setCECCmd");
        mHdmiNative.setCECCmd(initAddr, destAddr, opCode, operand, size, enqueueOk);
    }
    

    /**
     * Check whether HDMI audio is enabled
     * 
     * @return true for successful get the status of audio
     */
    public boolean getAudioEnabledStatus() {
        Log.i(TAG, ">>HDMILocalService.getAudioEnabledStatus");
        String state = null;
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        if (mAudioManager != null) {
            state = mAudioManager.getParameters(GET_HDMI_AUDIO_STATUS);
            if (HDMI_AUDIO_STATUS_ENABLED.equals(state)) {
                Log.i(TAG, "HDMI audeo is enabled");
                return true;
            }
        }
        Log.i(TAG, "HDMI audio is disabled");
        return false;
    }    

    /**
     * Get EDID
     * 
     * @return call native method to get EDID
     */
    public int[] getEDID() {
        Log.i(TAG, ">>HDMILocalService.getEDID");
        return mHdmiNative.getEDID();
    }

    public char[] getCECAddr() {
        Log.i(TAG, ">>HDMILocalService.getCECAddr");
        return mHdmiNative.getCECAddr();
    }

    public int[] getCECCmd() {
        Log.i(TAG, ">>HDMILocalService.getCECCmd");
        return mHdmiNative.getCECCmd();
    }
   

    /**
     * Set the resolution for the video
     * 
     * @param resolution
     *            the value of resolution
     * @return call native method to set config
     */
    public boolean setVideoResolution(int resolution) {
        Log.i(TAG, ">>HDMILocalService.setVideoResolution(), new resolution="
                + resolution);
        if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            //To determine Auto-720P60Hz(102) or Auto-720P50Hz(103)
            //Auto-480P(100) or Auto-576P(101)
            if (resolution >= 100) {
                resolution = resolution - 100;
            }
        }
        return mHdmiNative.setVideoConfig(resolution);
    }

    /**
     * Set the deep color for the video
     * 
     * @param colorSpace
     *            the value of colorSpace
     * @param deepColor
     *            the value of deepColor
     * @return call native method to set deep color
     */
    public boolean setDeepColor(int colorSpace, int deepColor) {
        Log.i(TAG, ">>HDMILocalService.setVideoResolution(), new deepColor="
                + deepColor + " colorspace=" + colorSpace);
        return mHdmiNative.setDeepColor(colorSpace, deepColor);
    }

    /**
     * Set HDCP key
     * 
     * @return call native method to set HDCP key
     */
    public boolean setHDCPKey() {
        byte[] key = null;
        Log.i(TAG, ">>HDMILocalService.setHDCPKey()");

        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);

        if (agent != null) {
            try { 
                Log.i(TAG, "Read HDCP key from nvram");
                key = agent.readFile(AP_CFG_RDCL_FILE_HDCP_KEY_LID);
                //FOR DEBUG
                for (int i = 0; i < 287; i++) {
                    Log.i(TAG, "HDCP Key[" + i + "] = " + key[i]);
                }
            } catch (RemoteException e) { 
                e.printStackTrace(); 
            }
        } else {
            Log.i(TAG, "NvRAMAgent is null!");
        }

        if (key != null) {
            return mHdmiNative.setHDCPKey(key);
        } else {
            return false;
        }
    }

    private void updateEDID(boolean isIn) {
        if (isIn) {
            sEdid = getEDID();
            if (sEdid != null) {
                for (int i = 0; i < sEdid.length; i++) {
                    Log.i(TAG, "sEdid[" + i + "] = " + sEdid[i]);
                }
                Intent intent = new Intent(ACTION_EDID_UPDATED);
                //intent.putExtra("cable_pluged", sIsCablePluged);
                sendBroadcast(intent);
            } else {
                Log.i(TAG, "sEdid is still null!");
            }

            if (sEdidPrev != null) {
                for (int i = 0; i < sEdidPrev.length; i++) {
                    Log.i(TAG, "sEdidPrev[" + i + "] = " + sEdidPrev[i]);
                }
            } else {
                Log.i(TAG, "sEdidPrev is null!");
            }
        } else {
            sEdidPrev = sEdid;
            sEdid = null;
        }
    }

    private void initVideoResolution() {
        if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            initVideoResolution8193();
        } else {
            String videoResolution = Settings.System.getString(this
                    .getContentResolver(), KEY_HDMI_VIDEO_RESOLUTION);
            Log.i(TAG, ">>HDMILocalService.initVideoResolution(), init resolution="
                    + videoResolution);
            if (videoResolution == null || "".equals(videoResolution)) {
                Log.e(TAG, ">>No init resolution, set it to Auto by default");
                videoResolution = "3";// auto by default                
            }
            setVideoResolution(Integer.parseInt(videoResolution));
        }
    }

    private void initVideoResolution8193() {
        boolean isChanged = false;
        String videoResolution = Settings.System.getString(this
                .getContentResolver(), KEY_HDMI_VIDEO_RESOLUTION);
        Log.i(TAG, ">>HDMILocalService.initVideoResolution8193(), init resolution="
                + videoResolution);

        if (sEdidPrev == null) {
            isChanged = false;
        } else if (Arrays.equals(sEdid, sEdidPrev)) {
            isChanged = false;
        } else {
            isChanged = true;
        }

        if (videoResolution == null || "".equals(videoResolution) || isChanged) {
            Log.e(TAG, ">>No init resolution or EDID changed, set it by default");

            if (sEdid != null) {
                //Priority: 720p60 > 720p50 > 480p > 576p
                if (1 == ((sEdid[0] >> 1) & 0x01)) {
                    //NTSC 720P 60Hz (UI does not show Auto)
                    videoResolution = "2";
                } else if (1 == ((sEdid[1] >> 11) & 0x01)) {
                    //PAL 720P 50Hz (UI does not show Auto)
                    videoResolution = "3";
                } else if (1 == (sEdid[0] & 0x01)) {
                    //NTSC 480P (UI shows Auto)
                    videoResolution = "100";
                } else if (1 == ((sEdid[1] >> 10) & 0x01)) {
                    //PAL 576P (UI shows Auto)
                    videoResolution = "101";
                } else {
                    videoResolution = "100";
                }
            } else {
                videoResolution = "100";
            }
        }
        Log.i(TAG, ">>Final resolution in init resolution is: " + videoResolution);
        setVideoResolution(Integer.parseInt(videoResolution));
    }

    private void initColorSpaceAndDeepColor() {        
        Log.i(TAG, ">>HDMILocalService.initColorSpaceAndDeepColor(), init color space="
                    + "RGB, init deep color=8bit");
        //Set colorspace and deepcolor to default value = RGB + no deep color (8bit)
        setDeepColor(0, 1);
    }

    /**
     * 
     * @return true for cable is plugged in
     */
    public boolean isCablePluged() {
        Log.d(TAG, LOCAL_TAG + "isCablePluged?" + sIsCablePluged);
        return sIsCablePluged;
    }

    /**
     * when hdmi cable is pluged in or out, make next action for it
     */
    private void dealWithCablePluged() {
        Log.e(TAG, LOCAL_TAG + "dealWithCablePluged(), is cable pluged in?"
                + sIsCablePluged);
        Intent intent = new Intent(ACTION_CABLE_STATE_CHANGED);
        intent.putExtra("cable_pluged", sIsCablePluged);
        sendBroadcast(intent);
        showNotification(sIsCablePluged);

        if (sIsCablePluged) {
            if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
                updateEDID(true);
            }
            updateAudioState();
            enableVideoImp(sHMDIVideoTargetState);
            if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
                initColorSpaceAndDeepColor();
            }
            initVideoResolution();
            if (mHdmiNative.ishdmiForceAwake() && (sWakeLock != null)
                    && (!sWakeLock.isHeld())) {
                sWakeLock.acquire();
            }
        } else {
            Log.e(TAG, LOCAL_TAG
                    + "dealWithCablePluged() sleep 140ms for audio");            
            try {
                // sleep 140ms for notification data consumed in hardware buffer
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
                updateEDID(false);
            }
            enableAudioImp(false);
            enableVideoImp(false);
            if (mHdmiNative.ishdmiForceAwake() && (sWakeLock != null)
                    && (sWakeLock.isHeld())) {
                sWakeLock.release();
            }
        }
    }

    private void showNotification(boolean hasCable) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "Fail to get NotificationManager instance");
            return;
        }
        if (hasCable) {
            Log.i(TAG, "HDMI cable is pluged in, give notification now");
            Notification notification = new Notification();
            String titleStr = getResources().getString(
                    R.string.hdmi_notification_title);
            String contentStr = getResources().getString(
                    R.string.hdmi_notification_content);
            notification.icon = R.drawable.ic_hdmi_notification;
            notification.tickerText = titleStr;
            /*
             * Remove the notification sound as this may cause many other
             * problems on audio notification.sound =
             * RingtoneManager.getActualDefaultRingtoneUri(this,
             * RingtoneManager.TYPE_NOTIFICATION);
             */
            notification.flags = Notification.FLAG_ONGOING_EVENT
                    | Notification.FLAG_NO_CLEAR
                    | Notification.FLAG_SHOW_LIGHTS;

            Intent intent = Intent
                    .makeRestartActivityTask(new ComponentName(
                            "com.android.settings",
                            "com.android.settings.HDMISettings"));
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    intent, 0);
            notification.setLatestEventInfo(this, titleStr, contentStr,
                    pendingIntent);
            notificationManager.notify(R.drawable.ic_hdmi_notification,
                    notification);
        } else {
            Log.i(TAG, "HDMI cable is pluged out, clear notification now");
            notificationManager.cancel(R.drawable.ic_hdmi_notification);
        }
    }

    private void dealWithHeadSetChanged() {
        Log.e(TAG, LOCAL_TAG + "dealWithHeadSetChanged(), headset new state = "
                + sWiredHeadSetPlugState);
        if (sHMDIAudioTargetState) {
            updateAudioState();
        } else {
            Log.i(TAG, LOCAL_TAG + " audio was off, just return");
        }
    }

    /**
     * When phone state change, modify audio state synchronized
     */
    private void dealWithCallStateChanged() {
        Log.i(TAG, LOCAL_TAG + "updateAudioStateByPhone()");
        if (sHMDIAudioTargetState) {
            updateAudioState();
        } else {
            Log.i(TAG, LOCAL_TAG + " audio was off, just return");
        }
    }

    private boolean updateAudioState() {
        Log.i(TAG, LOCAL_TAG + "updateAudioState(), HDMI target state="
                + sHMDITargetState + "\n sIsCablePluged=" + sIsCablePluged
                + "\n audioTargetState=" + sHMDIAudioTargetState
                + "\n sIsCallStateIdle=" + sIsCallStateIdle
                + "\n sWiredHeadSetPlugState=" + sWiredHeadSetPlugState);
        // HDMI is disabled or no cable pluged now, no audio
        if (!sHMDITargetState || !sIsCablePluged) {
            return false;
        }
        if (sHMDIAudioTargetState && sIsCallStateIdle
                && (sWiredHeadSetPlugState != 1)) {
            return enableAudioImp(true);
        } else {
            return enableAudioImp(false);
        }
    }

    /**
     * send command to HDMI driver when IPO boot up or shut down, to
     * resume/pause HDMI
     * 
     * @param isBootUp
     */
    private void dealWithIPO(boolean isBootUp) {
        Log.i(TAG, "dealWithIPO(), is bootUp?" + isBootUp
                + ", sHMDITargetState=" + sHMDITargetState);
        if (sHMDITargetState) {
            mHdmiNative.enableHDMI(isBootUp);
        }
        if (isBootUp) {
            Log.i(TAG, "reset audio state for IPO boot up");
            updateAudioState();
        } else {
            Log.i(TAG, "shut down audio for IPO shut down");
            enableAudioImp(false);
        }
    }
}
