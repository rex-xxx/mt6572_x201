package com.mediatek.phone;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.phone.PhoneGlobals;
import com.mediatek.common.voicecommand.IVoiceCommandManager;
import com.mediatek.common.voicecommand.VoiceCommandListener;

public class VoiceCommandHandler {

    public interface Listener {
        void acceptIncomingCallByVoiceCommand();
        void rejectIncomingCallByVoiceCommand();
    }

    private static final String TAG = "VoiceCommandHandler";

    private static final int VOICE_COMMAND_RESULT_INCOMING_CALL_ACCEPT = 1;
    private static final int VOICE_COMMAND_RESULT_INCOMING_CALL_REJECT = 2;

    private Context mContext;
    private Listener mListener;
    private VoiceCommandListener mVoiceCommandListener;
    private boolean mIsVoiceIdentifying;

    public VoiceCommandHandler(Context context, Listener listener) {
        mContext = context;
        mListener = listener;

        mVoiceCommandListener = new VoiceCommandListener(mContext) {
            @Override
            public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
                if (VoiceCommandListener.ACTION_MAIN_VOICE_COMMON == mainAction) {
                    handleCommonVoiceCommand(subAction, extraData);
                } else if (VoiceCommandListener.ACTION_MAIN_VOICE_UI == mainAction) {
                    handleUIVoiceCommand(subAction, extraData);
                }
            }
        };
    }

    private void handleCommonVoiceCommand(int subAction, Bundle extraData) {
        log("handleCommonVoiceCommand(): subAction = " + subAction + ", extraData = " + extraData);

        switch (subAction) {

            case VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD:
                if (VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS
                        == extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT)) {
                    log("handleCommonVoiceCommand(): extraData = ACTION_EXTRA_RESULT_SUCCESS");
                    String[] comments = extraData.getStringArray(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                    if (null != comments && comments.length > 1) {
                        PhoneGlobals.getInstance().notificationMgr.showVoiceCommandNotification(comments[0], comments[1]);
                    } else {
                        log("ACTION_VOICE_UI_NOTIFY message extra comment is null or length < 2");
                    }
                } else {
                    log("ACTION_VOICE_UI_NOTIFY message's extra data is not SUCCESS");
                }
                break;

            default:
                break;
        }
    }

    private void handleUIVoiceCommand(int subAction, Bundle extraData) {
        log("handleUIVoiceCommand(): subAction = " + subAction + ", extraData = " + extraData);

        switch (subAction) {

            case VoiceCommandListener.ACTION_VOICE_UI_START:
                log("handleUIVoiceCommand(), VoiceCommandListener.ACTION_VOICE_UI_START");
                if (VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS
                        != extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT)) {
                    log("handleUIVoiceCommand(), ACTION_VOICE_UI_START message's extra data is not SUCCESS");
                    break;
                }
                if (null != mContext) {
                    IVoiceCommandManager voiceCommandManager =
                            (IVoiceCommandManager) mContext.getSystemService("voicecommand");
                    if (null != voiceCommandManager) {
                        try {
                            voiceCommandManager.sendCommand(mContext,
                                    VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                                    VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD, null);
                        } catch (RemoteException e) {
                            log("RemoteException happens during send main voice common command");
                        } catch (IllegalAccessException e) {
                            log("IllegalAccessException happens during send main voice common command");
                        }
                    }
                }
                break;

            case VoiceCommandListener.ACTION_VOICE_UI_STOP:
                log("handleUIVoiceCommand(), VoiceCommandListener.ACTION_VOICE_UI_STOP");
                break;

            case VoiceCommandListener.ACTION_VOICE_UI_NOTIFY:
                log("handleUIVoiceCommand(), VoiceCommandListener.ACTION_VOICE_UI_NOTIFY");
                PhoneGlobals.getInstance().notificationMgr.cancelVoiceCommandNotification();
                if (VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS
                        != extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT)) {
                    log("handleUIVoiceCommand(), ACTION_VOICE_UI_NOTIFY message's extra data is not SUCCESS");
                    break;
                }
                stopVoiceCommand();
                int commandId = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                if (VOICE_COMMAND_RESULT_INCOMING_CALL_ACCEPT == commandId) {
                    log("handleUIVoiceCommand(), accept");
                    if (null != mListener) {
                        mListener.acceptIncomingCallByVoiceCommand();
                    }
                } else if (VOICE_COMMAND_RESULT_INCOMING_CALL_REJECT == commandId) {
                    log("handleUIVoiceCommand(), reject");
                    if (null != mListener) {
                        mListener.rejectIncomingCallByVoiceCommand();
                    }
                } else {
                    log("invalid command id");
                }
                break;

            default:
                break;
        }
    }

    public void startVoiceCommand() {
        log("startVoiceCommand()");
        if (null == mContext) {
            log("mContext is null, just return");
            return;
        }
        if (mIsVoiceIdentifying) {
            log("already voice identifying, just return");
            return;
        }
        IVoiceCommandManager voiceCommandManager = (IVoiceCommandManager) mContext.getSystemService("voicecommand");
        if (null != voiceCommandManager) {
            try {
                voiceCommandManager.registerListener(mVoiceCommandListener);
                voiceCommandManager.sendCommand(mContext,
                                                VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                                                VoiceCommandListener.ACTION_VOICE_UI_START, null);
                mIsVoiceIdentifying = true;
            } catch (RemoteException e) {
                log("RemoteException happens during start voice command");
                return;
            } catch (IllegalAccessException e) {
                log("IllegalAccessException happens during start voice command");
                return;
            }
        }
    }

    public void stopVoiceCommand() {
        log("stopVoiceCommand()");
        if (null == mContext) {
            log("mContext is null, just return");
            return;
        }
        if (!mIsVoiceIdentifying) {
            log("already not voice voidentifying, just return");
            return;
        }
        PhoneGlobals.getInstance().notificationMgr.cancelVoiceCommandNotification();
        IVoiceCommandManager voiceCommandManager = (IVoiceCommandManager) mContext.getSystemService("voicecommand");
        if (null != voiceCommandManager) {
            try {
                voiceCommandManager.sendCommand(mContext,
                                                VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                                                VoiceCommandListener.ACTION_VOICE_UI_STOP, null);
                voiceCommandManager.unRegisterListener(mVoiceCommandListener);
                mIsVoiceIdentifying = false;
            } catch (RemoteException e) {
                log("RemoteException happens during stop voice command");
                return;
            } catch (IllegalAccessException e) {
                log("IllegalAccessException happens during stop voice command");
                return;
            }
        }
    }

    public void clear() {
        mContext = null;
        mListener = null;
    }

    public boolean isVoiceIdentifying() {
        return mIsVoiceIdentifying;
    }

    public static boolean isValidCondition() {
        Call firstRingCall = PhoneGlobals.getInstance().mCM.getFirstActiveRingingCall();
        if (null == firstRingCall) {
            return false;
        }
        return Call.State.INCOMING == firstRingCall.getState()
                && !PhoneGlobals.getInstance().mCM.hasActiveFgCall()
                && !PhoneGlobals.getInstance().mCM.hasActiveBgCall();
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
