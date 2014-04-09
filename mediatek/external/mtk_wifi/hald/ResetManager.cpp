/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <stdio.h>
#include <errno.h>

#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/un.h>

#include <linux/netlink.h>

#define LOG_TAG "Hald"

#include <cutils/log.h>

#include "ResetManager.h"
#include "ResetHandler.h"
#include "HaldController.h"

ResetManager *ResetManager::sInstance = NULL;

ResetManager *ResetManager::Instance(CommandListener *cl) {
    if (!sInstance)
        sInstance = new ResetManager(cl);
    return sInstance;
}

ResetManager::ResetManager(CommandListener *cl) {
    mCl = cl;
}

ResetManager::~ResetManager() {
}

int ResetManager::start() {
    struct sockaddr_nl nladdr;
    int sz = 64 * 1024;
    GENERIC_NETLINK_PACKET ans, req;
    struct nlattr *na;
    int id;
    int mlength;
    const char *message = "HELLO"; //message
    int rc;
    int count = 10;
    
    if(mHandler) {
        ALOGD("ResetManager is already started");
        return 0;
    }
    
    mlength = strlen(message) + 1;
    ALOGD("Size of initial message: %d  ", mlength);
    memset(&nladdr, 0, sizeof(nladdr));
    nladdr.nl_family = AF_NETLINK;
    nladdr.nl_pid = getpid();
    nladdr.nl_groups = 0xffffffff;

    if ((mSock = socket(AF_NETLINK,
                        SOCK_RAW,NETLINK_GENERIC)) < 0) {
        ALOGE("Unable to create uevent socket: %s", strerror(errno));
        return -1;
    }

    if (setsockopt(mSock, SOL_SOCKET, SO_RCVBUFFORCE, &sz, sizeof(sz)) < 0) {
        ALOGE("Unable to set uevent socket options: %s", strerror(errno));
        return -1;
    }

    if (bind(mSock, (struct sockaddr *) &nladdr, sizeof(nladdr)) < 0) {
        ALOGE("Unable to bind uevent socket: %s", strerror(errno));
        return -1;
    }

    while(count--) {
        /* 2. get family ID */
        id = get_family_id(mSock, "MTK_WIFI");
        if (-1 == id) {
            ALOGE("Unable to get family id, Retry");
            usleep(500000);
        } else {
            ALOGD("[MTK_WIFI] family id = %d\n", id);
            break;
        }
    }
    
    /* 2.9 Check WiFi Driver Support Chip Reset*/
    if(id < 0) {
        close(mSock);
        mSock = -1;
        ALOGW("WiFi Driver Cannot Support Chip Reset, Abort ResetManager");
        return -1;
    }

    /* 3. Prepare Dummy command */
    req.n.nlmsg_len = NLMSG_LENGTH(GENL_HDRLEN);
    req.n.nlmsg_type = id;
    req.n.nlmsg_flags = NLM_F_REQUEST;
    req.n.nlmsg_seq = 60;
    req.n.nlmsg_pid = getpid();
    req.g.cmd = 1; //MTK_WIFI_COMMAND_BIND
    
    na = (struct nlattr *) GENLMSG_DATA(&req);
    na->nla_type = 1; //MTK_WIFI_ATTR_MSG
    na->nla_len = mlength + NLA_HDRLEN; //message length
    memcpy(NLA_DATA(na), message, mlength);
    req.n.nlmsg_len += NLMSG_ALIGN(na->nla_len);

    ALOGD("Reset Manager Prepare Dummy command");
    
    /* 3.1 Send Command for binding */
    memset(&nladdr, 0, sizeof(nladdr));
    nladdr.nl_family = AF_NETLINK;

    rc = sendto(mSock, (char *)&req, req.n.nlmsg_len, 0,
            (struct sockaddr *) &nladdr, sizeof(nladdr));
    
    ALOGD("Reset Manager Send Command for binding");
    
    mHandler = new ResetHandler(mCl, mSock);
    if (mHandler->start()) {
        ALOGE("Unable to start ResetHandler: %s", strerror(errno));
        return -1;
    }
    return 0;
}

int ResetManager::stop() {
    
    if(!mHandler) {
        ALOGD("ResetManager is already stoped");
        return 0;
    }

    if (mHandler->stop()) {
        ALOGE("Unable to stop ResetHandler: %s", strerror(errno));
        return -1;
    }
    delete mHandler;
    mHandler = NULL;

    close(mSock);
    mSock = -1;

    return 0;
}


/*
 * Send netlink message to kernel
 */
int ResetManager::sendto_fd(int s, const char *buf, int bufLen)
{
    struct sockaddr_nl nladdr;
    int r;

    memset(&nladdr, 0, sizeof(nladdr));
    nladdr.nl_family = AF_NETLINK;

    while ((r = sendto(s, buf, bufLen, 0, (struct sockaddr *) &nladdr,
                    sizeof(nladdr))) < bufLen) {
        if (r > 0) {
            buf += r;
            bufLen -= r;
        } else if (errno != EAGAIN)
            return -1;
    }
    return 0;
}


/*
 * Probe the controller in genetlink to find the family id
 */
int ResetManager::get_family_id(int sk, const char *family_name)
{
    struct nlattr *na;
    int rep_len;
    int id = -1;
    GENERIC_NETLINK_PACKET family_req, ans;

    /* Get family name */
    family_req.n.nlmsg_type = GENL_ID_CTRL;
    family_req.n.nlmsg_flags = NLM_F_REQUEST;
    family_req.n.nlmsg_seq = 0;
    family_req.n.nlmsg_pid = getpid();
    family_req.n.nlmsg_len = NLMSG_LENGTH(GENL_HDRLEN);
    family_req.g.cmd = CTRL_CMD_GETFAMILY;
    family_req.g.version = 0x1;

    na = (struct nlattr *) GENLMSG_DATA(&family_req);
    na->nla_type = CTRL_ATTR_FAMILY_NAME;
    na->nla_len = strlen(family_name) + 1 + NLA_HDRLEN;
    strcpy((char *)NLA_DATA(na), family_name);
 
    family_req.n.nlmsg_len += NLMSG_ALIGN(na->nla_len);

    if (sendto_fd(sk, (char *) &family_req, family_req.n.nlmsg_len) < 0) {
        return -1;
    }

    rep_len = recv(sk, &ans, sizeof(ans), 0);
    if (rep_len < 0){
        ALOGE("no response\n");
        return -1;
    }
    /* Validate response message */
    else if (!NLMSG_OK((&ans.n), (unsigned int)rep_len)){
        ALOGE("invalid reply message\n");
        return -1;
    }
    else if (ans.n.nlmsg_type == NLMSG_ERROR) { /* error */
        ALOGE("received error\n");
        return -1;
    }

    na = (struct nlattr *) GENLMSG_DATA(&ans);
    na = (struct nlattr *) ((char *) na + NLA_ALIGN(na->nla_len));
    if (na->nla_type == CTRL_ATTR_FAMILY_ID) {
        id = *(__u16 *) NLA_DATA(na);
    }

    return id;
}
