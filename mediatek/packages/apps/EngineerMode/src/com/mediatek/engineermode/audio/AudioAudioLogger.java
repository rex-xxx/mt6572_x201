/*
 *  Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly
 * prohibited.
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
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY
 * ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY
 * THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK
 * SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO
 * RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN
 * FORUM.
 * RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
 * LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation
 * ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.engineermode.audio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.media.AudioSystem;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

//import com.mediatek.featureoption.FeatureOption;

/**
 * @author mtk54045 To make user mode can dump audio data Time: 2012.05.22
 */
public class AudioAudioLogger extends Activity {

    /** check box Audio Stream Output Dump. */
    private CheckBox mAudioStrmOtptDump;
    /** check box Audio Mixer buffer Dump. */
    private CheckBox mAudioMixerBufDump;
    /** check box Audio track buffer Dump. */
    private CheckBox mAudioTrackBufDump;
    /** check box Audio A2DP stream Output Dump. */
    private CheckBox mAudioA2DPStrmDump;
    /** check box Audio Stream Input Dump. */
    private CheckBox mAudioStrmInptDump;
    // private CheckBox mAudioIdleRecdVMDump;

    // used for Audio Logger
    /** SET_DUMP_AUDIO_DEBUG_INFO position. */
    private static final int SET_DUMP_AUDIO_DEBUG_INFO = 0x62;
    /** SET_DUMP_AUDIO_STREAM_OUT position. */
    private static final int SET_DUMP_AUDIO_STREAM_OUT = 0x63;
    /** SET_DUMP_AUDIO_DEBUG_INFO position. */
    private static final int GET_DUMP_AUDIO_STREAM_OUT = 0x64;
    /** GET_DUMP_AUDIO_STREAM_OUT position. */
    private static final int SET_DUMP_AUDIO_MIXER_BUF = 0x65;
    /** GET_DUMP_AUDIO_MIXER_BUF position. */
    private static final int GET_DUMP_AUDIO_MIXER_BUF = 0x66;
    /** SET_DUMP_AUDIO_TRACK_BUF position. */
    private static final int SET_DUMP_AUDIO_TRACK_BUF = 0x67;
    /** GET_DUMP_AUDIO_TRACK_BUF position. */
    private static final int GET_DUMP_AUDIO_TRACK_BUF = 0x68;
    /** SET_DUMP_A2DP_STREAM_OUT position. */
    private static final int SET_DUMP_A2DP_STREAM_OUT = 0x69;
    /** GET_DUMP_A2DP_STREAM_OUT position. */
    private static final int GET_DUMP_A2DP_STREAM_OUT = 0x6A;
    /** SET_DUMP_AUDIO_STREAM_IN position. */
    private static final int SET_DUMP_AUDIO_STREAM_IN = 0x6B;
    /** GET_DUMP_AUDIO_STREAM_IN position. */
    private static final int GET_DUMP_AUDIO_STREAM_IN = 0x6C;
    /** Dialog no sdcard id. */
    private static final int DIALOG_ID_NO_SDCARD = 1;
    /** Dialog busy id. */
    private static final int DIALOG_ID_SDCARD_BUSY = 2;

    /** resource value. */

    // private static final int SET_DUMP_IDLE_VM_RECORD = 0x6D;
    // private static final int GET_DUMP_IDLE_VM_RECORD = 0x6E;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_audiologger);
        mAudioStrmOtptDump = (CheckBox) findViewById(R.id.Audio_StrmOtpt_Dump);
        mAudioMixerBufDump = (CheckBox) findViewById(R.id.Audio_MixerBuf_Dump);
        mAudioTrackBufDump = (CheckBox) findViewById(R.id.Audio_TrackBuf_Dump);
        mAudioA2DPStrmDump = (CheckBox) findViewById(R.id.Audio_A2DPOtpt_Dump);
        mAudioStrmInptDump = (CheckBox) findViewById(R.id.Audio_StrmInpt_Dump);
        // mAudioIdleRecdVMDump = (CheckBox)
        // findViewById(R.id.Audio_IDModeVM_Dump);
        Button mDumpAudioDbgInfo =
            (Button) findViewById(R.id.Dump_Audio_DebgInfo);

        // if (!FeatureOption.MTK_AUDIO_HD_REC_SUPPORT) {
        // mAudioIdleRecdVMDump.setVisibility(View.GONE);
        // }

        if (AudioSystem.getAudioCommand(GET_DUMP_AUDIO_STREAM_OUT) == 1) {
            mAudioStrmOtptDump.setChecked(true);
        } else {
            mAudioStrmOtptDump.setChecked(false);
        }
        if (AudioSystem.getAudioCommand(GET_DUMP_AUDIO_MIXER_BUF) == 1) {
            mAudioMixerBufDump.setChecked(true);
        } else {
            mAudioMixerBufDump.setChecked(false);
        }
        if (AudioSystem.getAudioCommand(GET_DUMP_AUDIO_TRACK_BUF) == 1) {
            mAudioTrackBufDump.setChecked(true);
        } else {
            mAudioTrackBufDump.setChecked(false);
        }
        if (AudioSystem.getAudioCommand(GET_DUMP_A2DP_STREAM_OUT) == 1) {
            mAudioA2DPStrmDump.setChecked(true);
        } else {
            mAudioA2DPStrmDump.setChecked(false);
        }
        if (AudioSystem.getAudioCommand(GET_DUMP_AUDIO_STREAM_IN) == 1) {
            mAudioStrmInptDump.setChecked(true);
        } else {
            mAudioStrmInptDump.setChecked(false);
        }
        // if (AudioSystem.GetAudioCommand(GET_DUMP_IDLE_VM_RECORD)
        // == 1) {
        // mAudioIdleRecdVMDump.setChecked(true);
        // } else {
        // mAudioIdleRecdVMDump.setChecked(false);
        // }

        mAudioStrmOtptDump.setOnCheckedChangeListener(mCheckedListener);
        mAudioMixerBufDump.setOnCheckedChangeListener(mCheckedListener);
        mAudioTrackBufDump.setOnCheckedChangeListener(mCheckedListener);
        mAudioA2DPStrmDump.setOnCheckedChangeListener(mCheckedListener);
        mAudioStrmInptDump.setOnCheckedChangeListener(mCheckedListener);
        // mAudioIdleRecdVMDump
        // .setOnCheckedChangeListener(mCheckedListener);
        mDumpAudioDbgInfo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                int ret =
                    AudioSystem.setAudioCommand(SET_DUMP_AUDIO_DEBUG_INFO, 1);
                if (ret == -1) {
                    Elog.i(Audio.TAG, "set DumpAudioDbgInfo"
                        + " parameter failed");
                    Toast.makeText(AudioAudioLogger.this,
                        R.string.set_failed_tip, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AudioAudioLogger.this,
                        R.string.set_success_tip, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /** check or uncheck checkbox items. */
    private final CheckBox.OnCheckedChangeListener mCheckedListener =
        new CheckBox.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
                int ret = -1;
                if (!checkSDCardIsAvaliable() && isChecked) {
                    buttonView.setChecked(false);
                    return;
                }
                if (buttonView.equals(mAudioStrmOtptDump)) {
                    if (isChecked) {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_STREAM_OUT, 1);
                    } else {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_STREAM_OUT, 0);
                    }
                } else if (buttonView.equals(mAudioMixerBufDump)) {
                    if (isChecked) {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_MIXER_BUF, 1);
                    } else {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_MIXER_BUF, 0);
                    }
                } else if (buttonView.equals(mAudioTrackBufDump)) {
                    if (isChecked) {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_TRACK_BUF, 1);
                    } else {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_TRACK_BUF, 0);
                    }
                } else if (buttonView.equals(mAudioA2DPStrmDump)) {
                    if (isChecked) {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_A2DP_STREAM_OUT, 1);
                    } else {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_A2DP_STREAM_OUT, 0);
                    }
                } else if (buttonView.equals(mAudioStrmInptDump)) {
                    if (isChecked) {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_STREAM_IN, 1);
                    } else {
                        ret =
                            AudioSystem.setAudioCommand(
                                SET_DUMP_AUDIO_STREAM_IN, 0);
                    }
                }
                // else if (buttonView == mAudioIdleRecdVMDump) {
                // if (isChecked) {
                // Elog.d(Audio.TAG, "Checked mAudioIdleRecdVMDump");
                // if (!CheckSDCardIsAvaliable()) {
                // mAudioIdleRecdVMDump.setChecked(false);
                // return;
                // }
                // ret = AudioSystem.SetAudioCommand(
                // SET_DUMP_IDLE_VM_RECORD, 1);
                // } else {
                // Elog.d(Audio.TAG, "UnChecked mAudioIdleRecdVMDump");
                // ret = AudioSystem.SetAudioCommand(
                // SET_DUMP_IDLE_VM_RECORD, 0);08;,./2abilnr
                // }
                // if (ret == -1) {
                // Elog.i(Audio.TAG, "set
                // mAudioIdleRecdVMDump parameter failed");
                // }
                // }
                if (ret == -1) {
                    Elog.i(Audio.TAG, "set" + buttonView + isChecked
                        + "parameter failed");
                    Toast.makeText(AudioAudioLogger.this,
                        R.string.set_failed_tip, Toast.LENGTH_LONG).show();
                    buttonView.setChecked(!isChecked);
                } else {
                    Elog.i(Audio.TAG, "set" + buttonView + isChecked
                        + "parameter success");
                }
            }

        };

    /**
     * Check the sdcard is available.
     * 
     * @return is sdcard avilable.
     */
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

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
        case DIALOG_ID_NO_SDCARD:
            return new AlertDialog.Builder(AudioAudioLogger.this).setTitle(
                R.string.no_sdcard_title).setMessage(R.string.no_sdcard_msg)
                .setPositiveButton(android.R.string.ok, null).create();
        case DIALOG_ID_SDCARD_BUSY:
            return new AlertDialog.Builder(AudioAudioLogger.this).setTitle(
                R.string.sdcard_busy_title)
                .setMessage(R.string.sdcard_busy_msg).setPositiveButton(
                    android.R.string.ok, null).create();
        default:
            return null;
        }
    }
}