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
#include <errno.h>

#include <fcntl.h>

#define LOG_TAG "Hald"
#include <cutils/log.h>
#include <cutils/misc.h>
#include <cutils/properties.h>
#include "ModuleCtrl.h"

#ifdef HAVE_LIBC_SYSTEM_PROPERTIES
#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>
#endif

extern "C" int init_module(void *, unsigned long, const char *);
extern "C" int delete_module(const char *, unsigned int);

ModuleCtrl::ModuleCtrl() {
}
ModuleCtrl::~ModuleCtrl() {
}

#ifdef HANDLE_MODULE_BY_PROP

int ModuleCtrl::svcWrapper(const char *svc_name, const char *para)
{
    char cmd[PROPERTY_VALUE_MAX];
    char cmd_status[PROPERTY_VALUE_MAX] = {'\0'};
    char prop_name[PROPERTY_VALUE_MAX];
    int count = 100; /* wait at most 5 seconds for completion */
    
    snprintf(cmd, PROPERTY_VALUE_MAX, "%s:%s", svc_name, para);
    snprintf(prop_name, PROPERTY_VALUE_MAX, "init.svc.%s", svc_name);
    
    ALOGD("Do command [%s], check prop [%s]", cmd, prop_name);
    property_set("ctl.start", cmd);
    sched_yield();

    while (count-- > 0) {
        if (property_get(prop_name, cmd_status, NULL)) {
            if (strcmp(cmd_status, "stopped") == 0) {
                ALOGD("Command Done!");
                return 0;
            }
        }
	    usleep(50000);
    }   
    
    ALOGD("Command Failed!");
    return -1;
}


int ModuleCtrl::insmod(const char *filename, const char *args)
{
    char parameter[PROPERTY_VALUE_MAX];
    
    snprintf(parameter, PROPERTY_VALUE_MAX, "%s %s", filename, args);
    
    return svcWrapper("insmod", parameter);
}

int ModuleCtrl::rmmod(const char *modname)
{   
    return svcWrapper("rmmod", modname);
}

#else

int ModuleCtrl::insmod(const char *filename, const char *args)
{
    void *module;
    unsigned int size;
    int ret;

    module = load_file(filename, &size);
    if (!module)
        return -1;
    ret = init_module(module, size, args);
    free(module);

    return ret;
}

int ModuleCtrl::rmmod(const char *modname)
{
    int ret = -1;
    int maxtry = REMOVE_RETRY_COUNT;

    while ((maxtry--) > 0) {
        ret = delete_module(modname, O_NONBLOCK | O_EXCL);
        if (ret < 0 && errno == EAGAIN)
            usleep(REMOVE_RETRY_DELAY);
        else
            break;
    }

    if (ret != 0)
        ALOGD("Unable to unload driver module \"%s\": %s\n",
            modname, strerror(errno));
    return ret;
}
#endif

