package com.android.camera;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.camera.R;
import com.mediatek.common.voicecommand.IVoiceCommandManager;
import com.mediatek.common.voicecommand.VoiceCommandListener;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class VoiceManager {
    private static final String TAG = "VoiceManager";
    private static final boolean LOG = Log.LOGV;
    
    public interface Listener {
        //For voice command id defined in VoiceManager of system server.
        int VOICE_COMMAND_CAPTURE = 3;
        int VOICE_COMMAND_CHEESE = 4;
        void onUserGuideUpdated(String userGuide);
        void onVoiceValueUpdated(String value);
        void onVoiceCommandReceive(int commandId);
    }
    
    public static final String VOICE_ON = "on";
    public static final String VOICE_OFF = "off";
    private static final String VOICE_SERVICE = "voicecommand";
    private static final int UNKNOWN = -1;
    
    private Context mContext;
    private String mVoiceValue;
    private String[] mKeywords;
    private List<Listener> mListeners = new CopyOnWriteArrayList<Listener>();
    private boolean mStartUpdate;
    private boolean mRegistered;
    
    public VoiceManager(Context context) {
        mContext = context;
    }

    public boolean addListener(Listener l) {
        if (LOG) {
            Log.v(TAG, "addListener(" + l + ")");
        }
        boolean added = false;
        if (!mListeners.contains(l)) {
            added = mListeners.add(l);
        }
        notifyUserGuideIfNeed();
        return added;
    }
    
    public boolean removeListener(Listener l) {
        if (LOG) {
            Log.v(TAG, "removeListener(" + l + ")");
        }
        return mListeners.remove(l);
    }

    private void notifyUserGuideIfNeed() {
        if (LOG) {
            Log.v(TAG, "notifyUserGuideIfNeed() mKeywords=" + mKeywords);
        }
        if (mKeywords != null) {
            String userGuide = getUserVoiceGuide(mKeywords);
            if (userGuide != null) {
                for(Listener listener : mListeners) {
                    listener.onUserGuideUpdated(userGuide);
                }
            }
        }
    }
    
    private void notifyStateChangedIfNeed() {
        if (LOG) {
            Log.v(TAG, "notifyStateChangedIfNeed() mVoiceValue=" + mVoiceValue);
        }
        for(Listener listener : mListeners) {
            listener.onVoiceValueUpdated(mVoiceValue);
        }
    }

    private void notifyCommandIfNeed(int commandId) {
        if (LOG) {
            Log.v(TAG, "notifyCommandIfNeed(" + commandId + ")");
        }
        for(Listener listener : mListeners) {
            listener.onVoiceCommandReceive(commandId);
        }
    }

    private String getUserVoiceGuide(String[] voice) {
        String userGuide = null;
        if (voice != null && voice.length >= 2) {
            userGuide = mContext.getString(R.string.voice_guide, voice[0], voice[1]);
        }
        if (LOG) {
            Log.v(TAG, "getUserVoiceGuide(" + voice + ") return " + userGuide);
        }
        return userGuide;
    }
    
    public void setVoiceValue(String value) {
        if (LOG) {
            Log.v(TAG, "setVoiceValue(" + value + ") mVoiceValue=" + mVoiceValue);
        }
        if (mVoiceValue == null || !mVoiceValue.equals(value)) {
            mVoiceValue = value;
            if (VOICE_ON.equals(mVoiceValue)) {
                enableVoice();
            } else {
                disableVoice();
            }
        }
    }
    
    public String getVoiceValue() {
        if (LOG) {
            Log.v(TAG, "getVoiceValue() return " + mVoiceValue);
        }
        return mVoiceValue;
    }
    
    public void startUpdateVoiceState() {
        if (LOG) {
            Log.v(TAG, "startUpdateVoiceState() mStartUpdate=" + mStartUpdate);
        }
        if (FeatureSwitcher.isVoiceEnabled()) {
            if (!mStartUpdate) {
                startGetVoiceState();
                mStartUpdate = true;
            }
        }
    }
    
    public void stopUpdateVoiceState() {
        if (LOG) {
            Log.v(TAG, "stopUpdateVoiceState() mStartUpdate=" + mStartUpdate);
        }
        if (FeatureSwitcher.isVoiceEnabled()) {
            if (mStartUpdate) {
                stopVoice();
                //set voice value off for don't update indicator before get voice state.
                mVoiceValue = VOICE_OFF;
                mStartUpdate = false;
            }
        }
    }
    
    public void enableVoice() {
        if (LOG) {
            Log.v(TAG, "enableVoice()");
        }
        ensureManager();
        registerManagerListener();
        startVoiceCommand(VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                VoiceCommandListener.ACTION_VOICE_UI_ENABLE, null);
        startVoiceCommand(VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD, null);
    }
    
    private void disableVoice() {
        if (LOG) {
            Log.v(TAG, "disableVoice()");
        }
        ensureManager();
        startVoiceCommand(VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                VoiceCommandListener.ACTION_VOICE_UI_DISALBE, null);
        unRegisterManagerListener();
    }
    
    private void stopVoice() {
        if (LOG) {
            Log.v(TAG, "stopVoice()");
        }
        ensureManager();
        startVoiceCommand(VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                VoiceCommandListener.ACTION_VOICE_UI_STOP, null);
        unRegisterManagerListener();
    }
    
    private void startGetVoiceState() {
        if (LOG) {
            Log.v(TAG, "startGetVoiceState()");
        }
        ensureManager();
        registerManagerListener();
        startVoiceCommand(VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                VoiceCommandListener.ACTION_VOICE_COMMON_PROCSTATE, null);
    }
    
    private IVoiceCommandManager mVoiceManager;
    private MyVoiceCommandListener mVoiceListener;
    private void startVoiceCommand(int mainAction, int subAction, Bundle extra) {
        if (LOG) {
            Log.v(TAG, "startVoiceCommand(" + mainAction + ", " + subAction + ", " + extra + ")");
        }
        if (mVoiceManager != null) {
            try {
                mVoiceManager.sendCommand(mContext, mainAction, subAction, extra);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (LOG) {
            Log.v(TAG, "startVoiceCommand() mVoiceManager=" + mVoiceManager);
        }
    }
    
    private void ensureManager() {
        if (mVoiceManager == null) {
            mVoiceManager = (IVoiceCommandManager)mContext.getSystemService(VOICE_SERVICE);
            mVoiceListener = new MyVoiceCommandListener(mContext);
        }
    }
    
    private void registerManagerListener() {
        if (LOG) {
            Log.v(TAG, "registerManagerListener() mRegistered=" + mRegistered);
        }
        if (mVoiceManager != null && !mRegistered) {
            try {
                mVoiceManager.registerListener(mVoiceListener);
                mRegistered = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void unRegisterManagerListener() {
        if (LOG) {
            Log.v(TAG, "unRegisterManagerListener() mRegistered=" + mRegistered);
        }
        if (mVoiceManager != null && mRegistered) {
            try {
                mVoiceManager.unRegisterListener(mVoiceListener);
                mRegistered = false;
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class MyVoiceCommandListener extends VoiceCommandListener {

        public MyVoiceCommandListener(Context context) {
            super(context);
        }
    
        @Override
        public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
            if (LOG) {
                Log.v(TAG, "onVoiceCommandNotified(" + mainAction + ", " + subAction + ", " + extraData + ")");
            }
            int result = UNKNOWN;
            switch (mainAction) {
            case ACTION_MAIN_VOICE_UI:
                switch (subAction) {
                case ACTION_VOICE_UI_ENABLE:
                    break;
                case ACTION_VOICE_UI_DISALBE:
                    break;
                case ACTION_VOICE_UI_START:
                    break;
                case ACTION_VOICE_UI_STOP:
                    break;
                case ACTION_VOICE_UI_NOTIFY:
                    if (extraData != null) {
                        printExtraData(extraData);
                        result = extraData.getInt(ACTION_EXTRA_RESULT, UNKNOWN);
                        if (result == ACTION_EXTRA_RESULT_SUCCESS) {
                            int commandId = extraData.getInt(ACTION_EXTRA_RESULT_INFO, UNKNOWN);
                            notifyCommandIfNeed(commandId);
                        }
                    }
                    break;
                default:
                    break;
                }
                break;
            case ACTION_MAIN_VOICE_COMMON:
                switch (subAction) {
                case ACTION_VOICE_COMMON_KEYWORD:
                    if (extraData != null) {
                        printExtraData(extraData);
                        result = extraData.getInt(ACTION_EXTRA_RESULT, UNKNOWN);
                        if (result == ACTION_EXTRA_RESULT_SUCCESS) {
                            mKeywords = extraData.getStringArray(ACTION_EXTRA_RESULT_INFO);
                            notifyUserGuideIfNeed();
                        }
                    }
                    break;
                case ACTION_VOICE_COMMON_PROCSTATE:
                    if (extraData != null) {
                        printExtraData(extraData);
                        result = extraData.getInt(ACTION_EXTRA_RESULT, UNKNOWN);
                        if (result == ACTION_EXTRA_RESULT_SUCCESS) {
                            boolean enabled = extraData.getBoolean(ACTION_EXTRA_RESULT_INFO, false);
                            mVoiceValue = (enabled ? VOICE_ON : VOICE_OFF);
                            notifyStateChangedIfNeed();
                        }
                    }
                    break;
                default:
                    break;
                }
                break;
            default:
                break;
            }
        }
        
        private void printExtraData(Bundle extraData) {
            if (LOG) {
                Set<String> keys = extraData.keySet();
                for(String key : keys) {
                    Log.v(TAG, "printExtraData() extraData[" + key + "]=" + extraData.get(key));
                }
            }
        }
    }
}
