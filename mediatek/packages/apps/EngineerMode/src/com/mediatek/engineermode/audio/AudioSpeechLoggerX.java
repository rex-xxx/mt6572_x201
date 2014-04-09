/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.engineermode.audio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioSystem;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.ShellExe;

import java.io.IOException;

//import com.mediatek.featureoption.FeatureOption;

public class AudioSpeechLoggerX extends Activity {
    public static final String ENGINEER_MODE_PREFERENCE =
        "engineermode_audiolog_preferences";
    public static final String EPL_STATUS = "epl_status";

    private CheckBox mCKSpeechLogger;
    private CheckBox mCKVOIPLogger;
    // private CheckBox mCKSpeechPlay;
    private CheckBox mCKCTM4WAY;

    // Added by mtk54045 2012.05.22, To simplify the way customers set the
    // Speech logger
    private RadioButton mRadioBtnBEPL;
    private RadioButton mRadioBtnBNormalVm;

    // private Boolean mIsEnable = false;
    // private Boolean mIsVOIPEnable = false;
    private byte mData[];
    private static final int DATA_SIZE = 1444;
    private static final int VM_LOG_POS = 1440;
    private int mVmLogState = 0;
    // used for Speech Logger
    private static final int SET_SPEECH_VM_ENABLE = 0x60;
    private static final int SET_DUMP_SPEECH_DEBUG_INFO = 0x61;
    private static final int DIALOG_GET_DATA_ERROR = 0;
    private static final int DIALOG_ID_NO_SDCARD = 1;
    private static final int DIALOG_ID_SDCARD_BUSY = 2;
    private static final int CONSTANT_256 = 256;
    private static final int CONSTANT_0XFF = 0xFF;
    private boolean mForRefresh = false; // Sloved radiobutton can not checked
    private final CheckBox.OnCheckedChangeListener mCheckedListener =
        new CheckBox.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                boolean checked) {

                SharedPreferences preferences =
                    getSharedPreferences(ENGINEER_MODE_PREFERENCE,
                        AudioSpeechLoggerX.MODE_WORLD_READABLE);
                Editor edit = preferences.edit();
                if (buttonView.equals(mCKSpeechLogger)) {
                    onClickSpeechLogger(edit, checked);
                } else if (buttonView.equals(mCKCTM4WAY)) {
                    if (checked) {
                        mData[VM_LOG_POS] |= 0x02;
                        mVmLogState |= 0x02;
                        Elog.d(Audio.TAG, "E mVmLogState " + mVmLogState);
                    } else {
                        mData[VM_LOG_POS] &= (~0x02);
                        mVmLogState &= (~0x02);
                        Elog.d(Audio.TAG, "D mVmLogState " + mVmLogState);
                    }
                    int index = AudioSystem.setEmParameter(mData, DATA_SIZE);
                    if (index != 0) {
                        Elog.i(Audio.TAG, "set CTM4WAY parameter failed");
                        Toast.makeText(AudioSpeechLoggerX.this,
                            R.string.set_failed_tip, Toast.LENGTH_LONG).show();
                    }
                } else if (buttonView.equals(mCKVOIPLogger)) {
                    if (checked) {
                        Elog.d(Audio.TAG, "mCKVOIPLogger checked");
                        setVOIP(1);
                    } else {
                        Elog.d(Audio.TAG, "mCKVOIPLogger Unchecked");
                        setVOIP(0);
                    }
                } else if (buttonView.equals(mRadioBtnBEPL)) {
                    if (checked) {
                        Elog.d(Audio.TAG, "mCKBEPL checked");
                        int ret =
                            AudioSystem
                                .setAudioCommand(SET_SPEECH_VM_ENABLE, 1);
                        AudioSystem.getEmParameter(mData, DATA_SIZE);
                        if (ret == -1) {
                            Elog.i(Audio.TAG, "set mCKBEPL parameter failed");
                            Toast.makeText(AudioSpeechLoggerX.this,
                                R.string.set_failed_tip, Toast.LENGTH_LONG)
                                .show();
                        }
                        edit.putInt(EPL_STATUS, 1);
                        edit.commit();
                    } else {
                        Elog.d(Audio.TAG, "mCKBEPL unchecked");
                    }
                } else if (buttonView.equals(mRadioBtnBNormalVm)) {
                    if (checked) {
                        Elog.d(Audio.TAG, "mCKBNormalVm checked");
                        if (mForRefresh) {
                            mForRefresh = false;
                        } else {
                            Elog.d(Audio.TAG, "mCKBNormalVm checked ok");
                            int ret =
                                AudioSystem.setAudioCommand(
                                    SET_SPEECH_VM_ENABLE, 0);
                            AudioSystem.getEmParameter(mData, DATA_SIZE);
                            if (ret == -1) {
                                Elog.i(Audio.TAG,
                                    "set mCKBNormalVm parameter failed");
                                Toast.makeText(AudioSpeechLoggerX.this,
                                    R.string.set_failed_tip, Toast.LENGTH_LONG)
                                    .show();
                            }
                            edit.putInt(EPL_STATUS, 0);
                            edit.commit();
                        }
                    } else {
                        Elog.d(Audio.TAG, "mCKBNormalVm unchecked");
                    }
                }
            }
        };

    private void onClickSpeechLogger(Editor edit, boolean checked) {
        if (checked) {
            Elog.d(Audio.TAG, "mCKSpeechLogger checked");
            if (!checkSDCardIsAvaliable()) {
                Elog.d(Audio.TAG, "mCKSpeechLogger checked 111");
                mCKSpeechLogger.setChecked(false);
                mRadioBtnBEPL.setEnabled(false);
                mRadioBtnBNormalVm.setEnabled(false);
                return;
            }
            mRadioBtnBEPL.setEnabled(true);
            mRadioBtnBNormalVm.setEnabled(true);
            mForRefresh = true;
            mRadioBtnBNormalVm.setChecked(true);
            mRadioBtnBEPL.setChecked(true);
            mData[VM_LOG_POS] |= 0x01;
            int index = AudioSystem.setEmParameter(mData, DATA_SIZE);
            if (index != 0) {
                Elog.i(Audio.TAG, "set mAutoVM parameter failed");
                Toast.makeText(AudioSpeechLoggerX.this,
                    R.string.set_failed_tip, Toast.LENGTH_LONG).show();
            }
        } else {
            Elog.d(Audio.TAG, "mCKSpeechLogger unchecked");
            if (mRadioBtnBEPL.isChecked()) {
                mRadioBtnBEPL.setChecked(false);
            }
            if (mRadioBtnBNormalVm.isChecked()) {
                mRadioBtnBNormalVm.setChecked(false);
            }

            mRadioBtnBEPL.setEnabled(false);
            mRadioBtnBNormalVm.setEnabled(false);
            //int ret = AudioSystem.SetAudioCommand(SET_SPEECH_VM_ENABLE, 0);
            AudioSystem.getEmParameter(mData, DATA_SIZE);
            //if (ret == -1) {
            //    Elog.i(Audio.TAG, "set mCKBEPL parameter failed 1");
            //    Toast.makeText(AudioSpeechLoggerX.this,
            //        R.string.set_failed_tip, Toast.LENGTH_LONG).show();
            //}
            edit.putInt(EPL_STATUS, 0);
            edit.commit();
            mData[VM_LOG_POS] &= (~0x01);
            int index = AudioSystem.setEmParameter(mData, DATA_SIZE);
            if (index != 0) {
                Elog.i(Audio.TAG, "set mAutoVM parameter failed");
                Toast.makeText(AudioSpeechLoggerX.this,
                    R.string.set_failed_tip, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_speechloggerx);

        mCKSpeechLogger =
            (CheckBox) findViewById(R.id.Audio_SpeechLogger_Enable);
        mCKVOIPLogger = (CheckBox) findViewById(R.id.Audio_VOIPLogger_Enable);
        // mCKSpeechPlay = (CheckBox)
        // findViewById(R.id.Audio_SpeechLogger_Play);
        mCKCTM4WAY = (CheckBox) findViewById(R.id.Audio_CTM4WAYLogger_Enable);
        TextView ctm4WayText =
            (TextView) findViewById(R.id.Audio_CTM4WAYLogger_EnableText);

        mRadioBtnBEPL = (RadioButton) findViewById(R.id.Audio_SpeechLogger_EPL);
        mRadioBtnBNormalVm =
            (RadioButton) findViewById(R.id.Audio_SpeechLogger_Normalvm);
        // Added by mtk54045 2012.05.22, To trigger audio driver to dump speech
        // debug info into ap side log
        Button dumpSpeechInfo = (Button) findViewById(R.id.Dump_Speech_DbgInfo);
        View spliteView = (View) this.findViewById(R.id.Audio_View1);

        if (!ChipSupport.isFeatureSupported(ChipSupport.MTK_TTY_SUPPORT)) {
            mCKCTM4WAY.setVisibility(View.GONE);
            ctm4WayText.setVisibility(View.GONE);
            spliteView.setVisibility(View.GONE);
        }
        final SharedPreferences preferences =
            getSharedPreferences(ENGINEER_MODE_PREFERENCE,
                AudioSpeechLoggerX.MODE_WORLD_READABLE);
        final int eplStatus = preferences.getInt(EPL_STATUS, 1);
        //int result = 0;
        //if (eplStatus == 1) {
        //    result = AudioSystem.SetAudioCommand(SET_SPEECH_VM_ENABLE, 1);
        //} else {
        //    result = AudioSystem.SetAudioCommand(SET_SPEECH_VM_ENABLE, 0);
        //}
        //if (result == -1) {
        //    Elog.i(Audio.TAG, "init mCKBEPL parameter failed");
        //}
        mData = new byte[DATA_SIZE];
        int ret = AudioSystem.getEmParameter(mData, DATA_SIZE);
        if (ret != 0) {
            showDialog(DIALOG_GET_DATA_ERROR);
            Elog.i(Audio.TAG,
                "Audio_SpeechLogger GetEMParameter return value is : " + ret);
        }

        mVmLogState = shortToInt(mData[VM_LOG_POS], mData[VM_LOG_POS + 1]);
        Elog.i(Audio.TAG,
            "Audio_SpeechLogger GetEMParameter return value is : "
                + mVmLogState);

        if ((mVmLogState & 0x01) == 0) {
            mCKSpeechLogger.setChecked(false);
            mRadioBtnBEPL.setEnabled(false);
            mRadioBtnBNormalVm.setEnabled(false);
            mRadioBtnBEPL.setChecked(false);
            mRadioBtnBNormalVm.setChecked(false);
        } else {
            mCKSpeechLogger.setChecked(true);
            mRadioBtnBEPL.setEnabled(true);
            mRadioBtnBNormalVm.setEnabled(true);
            if (eplStatus == 1) {
                mRadioBtnBEPL.setChecked(true);
            } else {
                mRadioBtnBNormalVm.setChecked(true);
            }
        }

        if ((mVmLogState & 0x02) == 0) {
            mCKCTM4WAY.setChecked(false);
        } else {
            mCKCTM4WAY.setChecked(true);
        }

        if (getVOIP() == 0) {
            mCKVOIPLogger.setChecked(false);
        } else {
            mCKVOIPLogger.setChecked(true);
        }

        mCKSpeechLogger.setOnCheckedChangeListener(mCheckedListener);
        mCKVOIPLogger.setOnCheckedChangeListener(mCheckedListener);
        // mCKSpeechPlay.setOnCheckedChangeListener(mCheckedListener);
        mCKCTM4WAY.setOnCheckedChangeListener(mCheckedListener);

        mRadioBtnBEPL.setOnCheckedChangeListener(mCheckedListener);
        mRadioBtnBNormalVm.setOnCheckedChangeListener(mCheckedListener);
        dumpSpeechInfo.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                Elog.d(Audio.TAG, "On Click  mDumpSpeechInfo button.");
                int ret =
                    AudioSystem.setAudioCommand(SET_DUMP_SPEECH_DEBUG_INFO, 1);
                if (ret == -1) {
                    Elog.i(Audio.TAG, "set mDumpSpeechInfo parameter failed");
                    Toast.makeText(AudioSpeechLoggerX.this,
                        R.string.set_failed_tip, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AudioSpeechLoggerX.this,
                        R.string.set_success_tip, Toast.LENGTH_LONG).show();
                }
            }

        });
    }

    private int getVOIP() {
        final String[] cmdx =
            { "/system/bin/sh", "-c",
                "cat /data/data/com.mediatek.engineermode/sharefile/audio_voip" }; // file
        try {
            if (ShellExe.execCommand(cmdx) != 0) {
                return 0;
            }
        } catch (IOException e) {
            Elog.e(Audio.TAG, e.toString());
            return 0;
        }
        return Integer.valueOf(ShellExe.getOutput());
    }

    private boolean setVOIP(int n) {
        final String[] cmd =
            { "/system/bin/sh", "-c",
                "mkdir /data/data/com.mediatek.engineermode/sharefile" }; // file
        try {
            ShellExe.execCommand(cmd);
//            if (ShellExe.execCommand(cmd) != 0) {
//                return false;
//            }
        } catch (IOException e) {
            Elog.e(Audio.TAG, e.toString());
            return false;
        }

        final String[] cmdx =
            {
                "/system/bin/sh",
                "-c",
                "echo "
                    + n
                    + " > /data/data/com.mediatek.engineermode/sharefile/audio_voip " }; // file

        try {
            if (ShellExe.execCommand(cmdx) != 0) {
                return false;
            }
        } catch (IOException e) {
            Elog.e(Audio.TAG, e.toString());
            return false;
        }
        return true;
    }

    private Boolean checkSDCardIsAvaliable() {
        final String state = Environment.getExternalStorageState();
        Elog
            .i(Audio.TAG, "Environment.getExternalStorageState() is : " + state);
        if (state.equals(Environment.MEDIA_REMOVED)) {
            showDialog(DIALOG_ID_NO_SDCARD);
            return false;
        }

        if (state.equals(Environment.MEDIA_SHARED)) {
            showDialog(DIALOG_ID_SDCARD_BUSY);
            return false;
        }
        return true;
    }

    private int shortToInt(byte low, byte high) {
        int temp = CONSTANT_0XFF & (high + CONSTANT_256);
        int highByte = CONSTANT_256 * temp;

        int lowByte = CONSTANT_0XFF & (low + CONSTANT_256);

        return highByte + lowByte;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
        case DIALOG_GET_DATA_ERROR:
            return new AlertDialog.Builder(AudioSpeechLoggerX.this).setTitle(
                R.string.get_data_error_title).setMessage(
                R.string.get_data_error_msg).setPositiveButton(
                android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // removeDialog(DIALOG_ID_GET_DATA_ERROR);
                        AudioSpeechLoggerX.this.finish();
                    }

                }).create();
        case DIALOG_ID_NO_SDCARD:
            return new AlertDialog.Builder(AudioSpeechLoggerX.this).setTitle(
                R.string.no_sdcard_title).setMessage(R.string.no_sdcard_msg)
                .setPositiveButton(android.R.string.ok, null).create();
        case DIALOG_ID_SDCARD_BUSY:
            return new AlertDialog.Builder(AudioSpeechLoggerX.this).setTitle(
                R.string.sdcard_busy_title)
                .setMessage(R.string.sdcard_busy_msg).setPositiveButton(
                    android.R.string.ok, null).create();
        default:
            return null;
        }
    }
}
