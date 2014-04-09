package com.mediatek.voiceunlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.KeyStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

import libcore.util.MutableBoolean;

import java.io.File;

import com.mediatek.voiceunlock.R;

import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManager;
import com.mediatek.xlog.Xlog;

public class VoiceUnlock extends PreferenceActivity {
    static final String TAG = "VoiceUnlockSetting";
    static final boolean DEBUG = true;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, VoiceUnlockFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.unlock_set_unlock_mode_voice_weak);
        showBreadCrumbs(msg, msg);
    }

    public static class VoiceUnlockFragment extends SettingsPreferenceFragment {
        
        static final String KEY_VOICE_UNLOCK = "voice_unlock";
        static final String KEY_VOICE_COMMAND1 = "voice_command1";
        static final String KEY_VOICE_COMMAND2 = "voice_command2";
        static final String KEY_VOICE_COMMAND3 = "voice_command3";
        
        static final String KEY_COMMAND_SUMMARY = "command_summary";
        
        private static final String CONFIRM_CREDENTIALS = "confirm_credentials";
        
        private static final int OPTIONS_DIALOG = 0;
        private static final int COMFIRM_RESET_DIALOG = 1;
        
        private static final int MSG_PLAY_PASSWORD = 0;
        private static final int MSG_SERVICE_ERROR = 1;
        
        private VoiceUnlockPreference mUnlock;
        private VoiceUnlockPreference mCommand1;
        private VoiceUnlockPreference mCommand2;
        private VoiceUnlockPreference mCommand3;
        
        boolean voice_unlock_set;
        boolean voice_command1_set;
        boolean voice_command2_set;
        boolean voice_command3_set;
        
        private IVoiceCommandManager mVoiceCmdManager;
        private VoiceCommandListener mVoiceCmdListener;
        
        private Handler mHandler;
        
        private String mClickedCmdKey;
        private CharSequence mClickedCmdSummary;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.voice_unlock);
            mVoiceCmdManager = (IVoiceCommandManager) getSystemService("voicecommand");
            if (mVoiceCmdManager != null) {
                mVoiceCmdListener = new VoiceCommandListener(getActivity()) {
                    @Override
                    public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
                        int result = extraData.getInt(ACTION_EXTRA_RESULT);
                        log("onNotified result = " + result);
                        if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                            if (subAction == VoiceCommandListener.ACTION_VOICE_TRAINING_PSWDFILE) {
                                String path = extraData.getString(ACTION_EXTRA_RESULT_INFO);
                                log("onNotified TRAINING_PSWDFILE path = " + path);
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_PLAY_PASSWORD, path));
                            }
                        } else if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_ERROR) {
                            String errorMsg = extraData.getString(ACTION_EXTRA_RESULT_INFO1);
                            log("onNotified RESULT_ERROR errorMsg = " + errorMsg);
                            mHandler.sendMessage(mHandler.obtainMessage(MSG_SERVICE_ERROR, errorMsg));
                        }
                    }
                };
            }
            
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_PLAY_PASSWORD:
                            handlePlayPassword((String) msg.obj);
                            break;
                        case MSG_SERVICE_ERROR:
                            handleServiceError((String) msg.obj);
                            break;
                        default:
                            break;
                    }
                }
            };
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                Preference preference) {
            final String key = preference.getKey();
            final VoiceUnlockPreference vuPreference = (VoiceUnlockPreference)preference;
            boolean handled = true;
            if (vuPreference.isChecked()) {
                mClickedCmdKey = key;
                if (KEY_VOICE_UNLOCK.equals(key)) {
                    mClickedCmdSummary = getResources().getText(R.string.voice_command_record_description_unlock_screen);
                } else {
                    mClickedCmdSummary = vuPreference.getSummary();
                }
                showDialog(OPTIONS_DIALOG);
                return handled;
            }
            
            if (KEY_VOICE_UNLOCK.equals(key)) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), VoiceUnlockSetupIntro.class);
                intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_KEY, Settings.System.VOICE_UNLOCK_SCREEN);
                intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_VALUE, "set");
                startActivity(intent);
            } else if (KEY_VOICE_COMMAND1.equals(key)) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), VoiceCommandSelect.class);
                intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_KEY, Settings.System.VOICE_UNLOCK_AND_LAUNCH1);
                startActivity(intent);
            } else if (KEY_VOICE_COMMAND2.equals(key)) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), VoiceCommandSelect.class);
                intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_KEY, Settings.System.VOICE_UNLOCK_AND_LAUNCH2);
                startActivity(intent);
            } else if (KEY_VOICE_COMMAND3.equals(key)) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), VoiceCommandSelect.class);
                intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_KEY, Settings.System.VOICE_UNLOCK_AND_LAUNCH3);
                startActivity(intent);
            } else {
                handled = false;
            }
            
            return handled;
        }
        
        @Override
        public Dialog onCreateDialog(int dialogId) {
            switch (dialogId) {
            case OPTIONS_DIALOG:
                return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.voice_unlock_options_title)
                .setItems(R.array.voice_unlock_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        case 0:
                            playCommand(mClickedCmdKey);
                            break;
                        case 1:
                            showDialog(COMFIRM_RESET_DIALOG);
                            break;
                        default:
                            break;
                        }
                    }
                })
                .create();
            case COMFIRM_RESET_DIALOG:
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.reset_command_title)
                .setNegativeButton(R.string.voice_unlock_cancel_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .setPositiveButton(R.string.voice_unlock_ok_label, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (isLastCommand()) {
                                resetCommand(mClickedCmdKey, true);
                            } else {
                                resetCommand(mClickedCmdKey, false);
                            }
                        }
                    })
                .create();
                if (isLastCommand()) {
                    dialog.setMessage(getResources().getString(R.string.reset_command_prompt_last));
                } else {
                    dialog.setMessage(getResources().getString(R.string.reset_command_prompt));
                }
                return dialog;
            default:
                return null;
            }
            
        }
        
        private void playCommand(String key) {
            if (mVoiceCmdManager != null) {
                try {
                    Bundle extra = new Bundle();
                    int commandId = getCommandId(key);
                    log("sendCommand TRAINING_PSWDFILE commandId = " + commandId);
                    extra.putInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, commandId);
                    mVoiceCmdManager.sendCommand(getActivity(),
                            VoiceCommandListener.ACTION_MAIN_VOICE_TRAINING,
                            VoiceCommandListener.ACTION_VOICE_TRAINING_PSWDFILE, extra);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void handlePlayPassword(String path) {
            log("handlePlayPassword path = " + path);
            File file = new File(path);
            Intent intent = new Intent();
            intent.setClass(getActivity(), PswPreview.class);
            intent.putExtra(KEY_COMMAND_SUMMARY, mClickedCmdSummary);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            log("handlePlayPassword uri = " + uri);
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.pass_word_file_missing, Toast.LENGTH_SHORT).show();
            }
        }

        private void handleServiceError(String errorMsg) {
            Toast.makeText(getActivity(), errorMsg, Toast.LENGTH_SHORT);
        }
        
        private int getCommandId(String key) {
            if (KEY_VOICE_COMMAND1.equals(key)) {
                return 1;
            } else if (KEY_VOICE_COMMAND2.equals(key)) {
                return 2;
            } else if (KEY_VOICE_COMMAND3.equals(key)) {
                return 3;
            } else {
                return 0;
            }
        }
        
        private void resetCommand(String key, boolean last) {
            if (mVoiceCmdManager != null) {
                try {
                    Bundle extra = new Bundle();
                    int commandId = getCommandId(key);
                    log("sendCommand TRAINING_RESET commandId = " + commandId);
                    extra.putInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, commandId);
                    mVoiceCmdManager.sendCommand(getActivity(),
                            VoiceCommandListener.ACTION_MAIN_VOICE_TRAINING,
                            VoiceCommandListener.ACTION_VOICE_TRAINING_RESET, extra);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (KEY_VOICE_UNLOCK.equals(key)) {
                Settings.System.putString(getContentResolver(),
                        Settings.System.VOICE_UNLOCK_SCREEN, null);
            } else if (KEY_VOICE_COMMAND1.equals(key)) {
                Settings.System.putString(getContentResolver(),
                        Settings.System.VOICE_UNLOCK_AND_LAUNCH1, null);
            } else if (KEY_VOICE_COMMAND2.equals(key)) {
                Settings.System.putString(getContentResolver(),
                        Settings.System.VOICE_UNLOCK_AND_LAUNCH2, null);
            } else if (KEY_VOICE_COMMAND3.equals(key)) {
                Settings.System.putString(getContentResolver(),
                        Settings.System.VOICE_UNLOCK_AND_LAUNCH3, null);
            }
            
            if (last) {
                LockPatternUtils utils = new LockPatternUtils(getActivity());
                utils.clearLock(false);
                utils.setLockScreenDisabled(false);
            }
            updateCommandStatusAndSummary();
        }
        
        private boolean isLastCommand() {
            int count = 0;
            String voice_unlock_screen = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_SCREEN);
            if (voice_unlock_screen != null) {
                count ++;
            }
            String voice_command1_app = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_AND_LAUNCH1);
            if (voice_command1_app != null) {
                count ++;
            }
            String voice_command2_app = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_AND_LAUNCH2);
            if (voice_command2_app != null) {
                count ++;
            }
            String voice_command3_app = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_AND_LAUNCH3);
            if (voice_command3_app != null) {
                count ++;
            }
            if (count == 1) {
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = super.onCreateView(inflater, container, savedInstanceState);
            
            mUnlock = (VoiceUnlockPreference) findPreference(KEY_VOICE_UNLOCK);
            mCommand1 = (VoiceUnlockPreference) findPreference(KEY_VOICE_COMMAND1);
            mCommand2 = (VoiceUnlockPreference) findPreference(KEY_VOICE_COMMAND2);
            mCommand3 = (VoiceUnlockPreference) findPreference(KEY_VOICE_COMMAND3);
            return v;
        }
        
        @Override
        public void onResume() {
            updateCommandStatusAndSummary();
            super.onResume();
            if (mVoiceCmdManager != null) {
                try {
                    log("register to service");
                    mVoiceCmdManager.registerListener(mVoiceCmdListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public void onPause() {
            super.onPause();
            if (mVoiceCmdManager != null) {
                try {
                    log("sendCommand TRAINING_STOP");
                    mVoiceCmdManager.sendCommand(getActivity(),
                            VoiceCommandListener.ACTION_MAIN_VOICE_TRAINING,
                            VoiceCommandListener.ACTION_VOICE_TRAINING_STOP, null);
                    log("unregister to service");
                    mVoiceCmdManager.unRegisterListener(mVoiceCmdListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        private void updateCommandStatusAndSummary() {
            log("updateCommandStatus ");
            String voice_unlock_screen = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_SCREEN);
            voice_unlock_set = voice_unlock_screen != null;
            mUnlock.setChecked(voice_unlock_set);
            
            String voice_command1_app = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_AND_LAUNCH1);
            voice_command1_set = voice_command1_app != null;
            if (voice_command1_set) {
                mCommand1.setSummary(getCommandSummary(voice_command1_app));
            }
            mCommand1.setChecked(voice_command1_set);
            
            String voice_command2_app = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_AND_LAUNCH2);
            voice_command2_set = voice_command2_app != null;
            if (voice_command2_set) {
                mCommand2.setSummary(getCommandSummary(voice_command2_app));
            }
            mCommand2.setChecked(voice_command2_set);
            
            String voice_command3_app = Settings.System.getString(getContentResolver(),
                    Settings.System.VOICE_UNLOCK_AND_LAUNCH3);
            voice_command3_set = voice_command3_app != null;
            if (voice_command3_set) {
                mCommand3.setSummary(getCommandSummary(voice_command3_app));
            }
            mCommand3.setChecked(voice_command3_set);
            
        }
        
        private String getCommandSummary (String commandValue){
            ComponentName cn = ComponentName.unflattenFromString(commandValue);
            ActivityInfo info;
            CharSequence name = "";
            try {
                info = getPackageManager().getActivityInfo(cn, 
                        PackageManager.GET_SHARED_LIBRARY_FILES);
                name = info.loadLabel(getPackageManager());
            } catch (NameNotFoundException e) {
                log("Cann't get app activityInfo via mCommandValue");
            }
            String summary = getResources().getString(R.string.command_summary, name);
            return summary;
        }

        private void log(String msg) {
            if (DEBUG) {
                Xlog.d(TAG, "VoiceUnlock: " + msg);
            }
        }

    }
}
