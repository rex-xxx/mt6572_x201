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

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.music;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

/**
 * 
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int LONG_PRESS_DELAY = 1000;

    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;

    /// M: AVRCP and Android Music AP supports the FF/REWIND @{
    private static final int ACTION_TIME = 500;
    /// @}

    /// M: tag for media button intent receiver
    private static final String TAG = "MediaButtonIntentReceiver";

    private static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!mLaunched) {
                        Context context = (Context)msg.obj;
                        Intent i = new Intent();
                        i.putExtra("autoshuffle", "true");
                        i.setClass(context, MusicBrowserActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                        mLaunched = true;
                    }
                    break;
            }
        }
    };
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        MusicLogUtils.d("MediaButtonIntentReceiver", "intentAction " + intentAction);
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            /// M: Only when Music service has been running, we need to send a start commond to stop playing. @
            if (isMusicServiceRunning(context)) {
                Intent i = new Intent(context, MediaPlaybackService.class);
                i.setAction(MediaPlaybackService.SERVICECMD);
                i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPAUSE);
                context.startService(i);
            }
            /// @}
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = (KeyEvent)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();
            
            /// M: AVRCP and Android Music AP supports the FF/REWIND
            long deltaTime = eventtime - mLastClickTime;

            // single quick press: pause/resume. 
            // double press: next track
            // long press: start auto-shuffle mode.

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MediaPlaybackService.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MediaPlaybackService.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MediaPlaybackService.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MediaPlaybackService.CMDPREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = MediaPlaybackService.CMDPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = MediaPlaybackService.CMDPLAY;
                    break;
                /// M: AVRCP and Android Music AP supports the FF/REWIND @{
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    command = MediaPlaybackService.CMDFORWARD;
                    break;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    command = MediaPlaybackService.CMDREWIND;
                    break;
                default:
                    break;
                /// @}
            }

            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mDown) {
                        if ((MediaPlaybackService.CMDTOGGLEPAUSE.equals(command) ||
                                MediaPlaybackService.CMDPLAY.equals(command))
                                && mLastClickTime != 0 
                                && eventtime - mLastClickTime > LONG_PRESS_DELAY) {
                            sHandler.sendMessage(
                                    sHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context));
                        }
                        /// M: AVRCP and Android Music AP supports the FF/REWIND @{
                        if ((MediaPlaybackService.CMDFORWARD.equals(command)
                                || MediaPlaybackService.CMDREWIND.equals(command))
                                && (mLastClickTime != 0)
                                && deltaTime > ACTION_TIME) {
                            sendToStartService(context, command, deltaTime);
                            mLastClickTime = eventtime;
                        }
                        /// @}
                    } else if (event.getRepeatCount() == 0) {
                        // only consider the first event in a sequence, not the repeat events,
                        // so that we don't trigger in cases where the first event went to
                        // a different app (e.g. when the user ends a phone call by
                        // long pressing the headset button)

                        // The service may or may not be running, but we need to send it
                        // a command.
                        Intent i = new Intent(context, MediaPlaybackService.class);
                        i.setAction(MediaPlaybackService.SERVICECMD);
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK &&
                                eventtime - mLastClickTime < 300) {
                            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDNEXT);
                            context.startService(i);
                            mLastClickTime = 0;
                        /// M: AVRCP and Android Music AP supports the FF/REWIND @{
                        } else if (MediaPlaybackService.CMDFORWARD.equals(command)
                                || MediaPlaybackService.CMDREWIND.equals(command)) {
                                    mLastClickTime = eventtime;
                        /// @}
                        } else {
                            i.putExtra(MediaPlaybackService.CMDNAME, command);
                            context.startService(i);
                            mLastClickTime = eventtime;
                        }

                        mLaunched = false;
                        mDown = true;
                    }
                } else {
                    /// M: AVRCP and Android Music AP supports the FF/REWIND @{
                    if (MediaPlaybackService.CMDFORWARD.equals(command)
                            || MediaPlaybackService.CMDREWIND.equals(command)) {
                        sendToStartService(context, command, deltaTime);
                        mLastClickTime = 0;
                    }
                    /// @}
                    sHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                    mDown = false;
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
            }
        }
    }

    /**
     * start service
     * @param context
     * @param command
     * @param deltaTime
     */
    public void sendToStartService(Context context, String command, long deltaTime) {
        Intent i = new Intent(context, MediaPlaybackService.class);
        i.setAction(MediaPlaybackService.SERVICECMD);
        i.putExtra(MediaPlaybackService.CMDNAME, command);
        i.putExtra(MediaPlaybackService.DELTATIME, deltaTime);
        context.startService(i);
    }

    /**
     * M: Check whether Music service is running.
     * 
     * @param context The context
     * @return If running, return true, otherwise false.
     */
    private boolean isMusicServiceRunning(Context context) {
        boolean isServiceRuning = false;
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        final int maxServciesNum = 100;
        List<RunningServiceInfo> list = am.getRunningServices(maxServciesNum);
        for (RunningServiceInfo info : list) {
            if (MediaPlaybackService.class.getName().equals(info.service.getClassName())) {
                isServiceRuning = true;
                break;
            }
        }
        MusicLogUtils.d(TAG, "isMusicServiceRunning " + isServiceRuning + ", Runing service num is " + list.size());
        return isServiceRuning;
    }
}
