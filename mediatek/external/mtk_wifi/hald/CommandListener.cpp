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
 * Copyright (C) 2010 The Android Open Source Project
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

#include <stdlib.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <dirent.h>
#include <errno.h>

#define LOG_TAG "Hald"
#include <cutils/log.h>

#include <sysutils/SocketClient.h>
#include <pthread.h>

#include "CommandListener.h"
#include "ResponseCode.h"
#include "ResetManager.h"

HaldController *CommandListener::sHaldCtrl = NULL;
pthread_mutex_t CommandListener::mLock;

CommandListener::CommandListener() :
                 FrameworkListener("hald") {
    registerCmd(new HaldCmd());
    pthread_mutex_init(&mLock, NULL);
    
    if (!sHaldCtrl) {
        sHaldCtrl = new HaldController();
    }

}

CommandListener::HaldCmd::HaldCmd() :
                 HaldCommand("hal") {
}

int CommandListener::HaldCmd::runCommand(SocketClient *cli, int argc, char **argv) {
    int rc = 0;
    
    if (argc < 3) {
        cli->sendMsg(ResponseCode::CommandSyntaxError, "Hald Missing argument", false);
        return 0;
    }
    ALOGD("======Receive cmd======");
    ALOGD("cmd <%s %s>", argv[1], argv[2]);
    //Load xxx driver
    if (!strcmp(argv[1], "load")) {
        pthread_mutex_lock(&mLock);
        rc = sHaldCtrl->loadDriver(argv[2]);
        pthread_mutex_unlock(&mLock);
    //Unload xxx driver
    } else if (!strcmp(argv[1], "unload")) {
        pthread_mutex_lock(&mLock);
        rc = sHaldCtrl->unloadDriver(argv[2]);
        pthread_mutex_unlock(&mLock);
    //Check xxx driver status
    } else if (!strcmp(argv[1], "status")) {
        pthread_mutex_lock(&mLock);
        //rc = sHaldCtrl->unloadDriver(argv[2]);
        pthread_mutex_unlock(&mLock);
#if CFG_ENABLE_RESET_MGR && 0       
    //Whole-Chip reset manager command
    } else if (!strcmp(argv[1], "rstmgr")) {
        //start reset manager
        if(!strcmp(argv[2], "start")) {
            sResetManager->start();
        //stop reset manager
        } else if(!strcmp(argv[2], "stop")) {
            sResetManager->stop();
        }
#endif
    } else if (!strcmp(argv[1], "")) {
        ALOGE("Unknown command");
    } else {
        cli->sendMsg(ResponseCode::CommandSyntaxError, "Hald Unknown cmd", false);
    }

    if(!rc) {
        cli->sendMsg(ResponseCode::CommandOkay, "Hald operation succeeded", false);
    } else {
        cli->sendMsg(ResponseCode::OperationFailed, "Hald operation failed", true);
    }
    return 0;
}
