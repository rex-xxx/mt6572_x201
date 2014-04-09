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

#ifndef _DRIVERCTRL_H__
#define _DRIVERCTRL_H__

#include "ModuleCtrl.h"
#include "RfkillCtrl.h"

#define LOAD_MODULE_ONCE

#ifndef WIFI_DRIVER_MODULE_PATH
#define WIFI_DRIVER_MODULE_PATH             "/system/lib/modules/wlan.ko"
#endif
#ifndef WIFI_DRIVER_MODULE_NAME
#define WIFI_DRIVER_MODULE_NAME             "wlan"
#endif
#ifndef WIFI_DRIVER_MODULE_ARG
#define WIFI_DRIVER_MODULE_ARG              ""
#endif
#ifndef WIFI_FIRMWARE_LOADER
#define WIFI_FIRMWARE_LOADER                ""
#endif
#ifndef WIFI_HOTSPOT_DRIVER_MODULE_PATH
#define WIFI_HOTSPOT_DRIVER_MODULE_PATH     "/system/lib/modules/p2p.ko"
#endif
#ifndef WIFI_HOTSPOT_DRIVER_MODULE_NAME
#define WIFI_HOTSPOT_DRIVER_MODULE_NAME     "p2p"
#endif
#ifndef WIFI_HOTSPOT_DRIVER_MODULE_ARG
#define WIFI_HOTSPOT_DRIVER_MODULE_ARG      "mode=1"
#endif
#ifndef WIFI_HOTSPOT_FIRMWARE_LOADER
#define WIFI_HOTSPOT_FIRMWARE_LOADER        ""
#endif
#ifndef WIFI_P2P_DRIVER_MODULE_PATH
#define WIFI_P2P_DRIVER_MODULE_PATH         "/system/lib/modules/p2p.ko"
#endif
#ifndef WIFI_P2P_DRIVER_MODULE_NAME
#define WIFI_P2P_DRIVER_MODULE_NAME         "p2p"
#endif
#ifndef WIFI_P2P_DRIVER_MODULE_ARG
#define WIFI_P2P_DRIVER_MODULE_ARG          ""
#endif
#ifndef WIFI_P2P_FIRMWARE_LOADER
#define WIFI_P2P_FIRMWARE_LOADER            ""
#endif

#define PARA_LENGTH 40
#define UNLOAD_CHK_COUNT 20
#define UNLOAD_CHK_DELAY 500000

typedef enum _ENUM_NETWORK_IFACE_T {
    NETWORK_IFACE_WIFI = 0,
    NETWORK_IFACE_P2P,
    NETWORK_IFACE_HOTSPOT,
    NETWORK_IFACE_NUM
} ENUM_NETWORK_IFACE_T;

class DriverParameter {
public:
    char acModuleName[PARA_LENGTH];
    char acModuleTag[PARA_LENGTH];
    char acModulePath[PARA_LENGTH];
    char acModuleArg[PARA_LENGTH];
    char acFirmwareLoader[PARA_LENGTH];
    char acPropName[PARA_LENGTH];
    char acSuppServiceName[PARA_LENGTH];
    char acSuppPropName[PARA_LENGTH];
    char acSuppConfigTemplate[PARA_LENGTH];
    char acSuppConfigFile[PARA_LENGTH];
};

class DriverCtrl {
    static ModuleCtrl *sModuleCtrl;

public:
    DriverCtrl();
    virtual ~DriverCtrl();
    int load(int u4NetIf);
    int unload(int u4NetIf);

private:

    int checkLoaded(
        const char *moduleName,
        const char *propName);

};


#endif
