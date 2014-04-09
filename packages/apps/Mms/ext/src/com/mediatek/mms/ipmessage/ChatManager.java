/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.ipmessage;

import android.content.Context;
import android.content.ContextWrapper;

/**
* Provide chat management related interface
*/
public class ChatManager extends ContextWrapper {

    public Context mContext = null;

    public ChatManager(Context context) {
        super(context);
        mContext = context;
    }

    public boolean needShowInviteDlg(long threadId) {
        return false;
    }

    public boolean handleInviteDlgLater(long threadId) {
        return false;
    }

    public boolean handleInviteDlg(long threadId) {
        return false;
    }

    /**
     * Check if need show reminder dialog
     * @param threadId thread ID
     * @return 0: reminder invalid; 1: reminder invite;
     *         2: reminder actived; 3: reminder switch;
     *         4: reminder enable.
     */
    public int needShowReminderDlg(long threadId) {
        return 0;
    }

    public boolean needShowSwitchAcctDlg(long threadId) {
        return false;
    }

    public void enterChatMode(String number) {
        return;
    }

    public void sendChatMode(String number, int status) {
        return;
    }

    public void exitFromChatMode(String number) {
        return;
    }

    public void saveChatHistory(long[] threadIds) {
        return;
    }

    /**
     * Get ip message count of special type in a thread.
     * @param threadId thread ID
     * @param typeFlay refer to IpMessageMediaTypeFlag
     * @return int count of message
     */
    public int getIpMessageCountOfTypeInThread(long threadId, int typeFlag) {
        return 0;
    }

    public boolean deleteDraftMessageInThread(long threadId) {
        return false;
    }
}

