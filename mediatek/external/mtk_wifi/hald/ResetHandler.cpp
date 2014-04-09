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
#include <stdlib.h>
#include <errno.h>

#define LOG_TAG "Hald"

#include <cutils/log.h>

#include "ResetHandler.h"
#include "ResetManager.h"
#include "ResponseCode.h"

ResetHandler::ResetHandler(CommandListener *cl, int listenerSocket) :
                ResetListener(listenerSocket) {
    mCl = cl;
    isRunningReset = false;
}

ResetHandler::~ResetHandler() {
}

int ResetHandler::start() {
    return this->startListener();
}

int ResetHandler::stop() {
    return this->stopListener();
}

void ResetHandler::onEvent(int evt) {

    switch(evt) {
        case CMD_RESET_START:
            if(isRunningReset) {
                ALOGE("Running Reset Now, Skip RESET_START Command");
            } else {
                pthread_mutex_lock(&(mCl->mLock));
                ALOGI("====WHOLE-CHIP RESET START!====");
                isRunningReset = true;
                //Do nothing now, just lock
            }
            break;
        case CMD_RESET_END:
            if(false == isRunningReset) {
                ALOGE("Not Running Reset, Skip RESET_END Command");
            } else {
                /*if wifi or sub function is on, power on*/
                if(mCl->sHaldCtrl->isFuncActive()) {
                    //power up
                    mCl->sHaldCtrl->powerOn();
                    if(mCl->sHaldCtrl->isP2pFuncActive()) {
                        mCl->sHaldCtrl->setP2pMode(1, 0);
                    }
                    else if(mCl->sHaldCtrl->isHotspotFuncActive()) {
                        mCl->sHaldCtrl->setP2pMode(1, 1);
                    }                    
                    
                }
                isRunningReset = false;
                pthread_mutex_unlock(&(mCl->mLock));
		ALOGI("====WHOLE-CHIP RESET END!  ====");
            }
            break;
        case CMD_UNKNOWN:
            ALOGD("Unknown command, Skip it!");
            break;
        default:
            //Do nothing
            break;
    }
}
