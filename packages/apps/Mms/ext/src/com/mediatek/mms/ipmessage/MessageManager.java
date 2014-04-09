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
import android.content.Intent;

import com.mediatek.mms.ipmessage.message.IpMessage;

/**
* Provide message management related interface
*/
public class MessageManager extends ContextWrapper {

    public Context mContext = null;

    public MessageManager(Context context) {
        super(context);
        mContext = context;
    }

    public IpMessage getIpMsgInfo(long msgId) {
        return null;
    }

    public IpMessage getIpMessageFromIntent(Intent intent) {
        return null;
    }

    public int getIpDatabaseId(long msgId) {
        return 0;
    }

    public int getType(long msgId) {
        return 0;
    }

    public int getStatus(long msgId) {
        return 0;
    }

    public int getTime(long msgId) {
        return 0;
    }

    public int getSimId(long msgId) {
        return 0;
    }

    public boolean isReaded(long msgId) {
        return false;
    }

    public String getTo(long msgId) {
        return "";
    }

    public String getFrom(long msgId) {
        return "";
    }

    public int saveIpMsg(IpMessage msg, int sendMsgMode) {
        return 0;
    }

    public void deleteIpMsg(long[] ids, boolean delImportant, boolean delLocked) {
        return;
    }

    public void setIpMsgAsViewed(long msgId) {
        return;
    }

    public void setThreadAsViewed(long threadId) {
        return;
    }

    /*
     * Download message attachment, asynchronous, notification: DownloadStatus
     * will be received
     */
    public void downloadAttach(long msgId) {
        return;
    }

    /*
     * cancel downloading, asynchronous, notification: DownloadStatus will be
     * received, status is Failed
     */
    public void cancelDownloading(long msgId) {
        return;
    }

    public boolean isDownloading(long msgId) {
        return false;
    }

    public int getDownloadProcess(long msgId) {
        return 0;
    }

    public boolean addMessageToImportantList(long[] msgIds) {
        return false;
    }

    public boolean deleteMessageFromImportantList(long[] msgIds) {
        return false;
    }

    public void resendMessage(long msgId) {
        return;
    }

    public void resendMessage(long msgId, int simId) {
        return;
    }

    public void setIpMessageStatus(long msgId, int msgStatus) {
        return;
    }

    public void handleIpMessage(long msgId, int action) {
        return;
    }

    public String getIpMessageStatusString(long msgId) {
        return null;
    }
}

