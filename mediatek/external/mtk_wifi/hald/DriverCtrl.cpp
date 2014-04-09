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

#include "DriverCtrl.h"

static const DriverParameter aDriverParameter[NETWORK_IFACE_NUM] =
    {
        {/*Legacy WiFi driver parameter*/
            WIFI_DRIVER_MODULE_NAME,                    /*module_name*/
            WIFI_DRIVER_MODULE_NAME " ",                /*module_tag*/
            WIFI_DRIVER_MODULE_PATH,                    /*module_path*/
            WIFI_DRIVER_MODULE_ARG,                     /*module_argment*/
            WIFI_FIRMWARE_LOADER,                       /*firmware loader*/
            "wlan.driver.status",                       /*system property name*/
            "wpa_supplicant",                           /*supplicant service name*/
            "init.svc.wpa_supplicant",                  /*supplicant property name*/
            "/system/etc/wifi/wpa_supplicant.conf",     /*supplicant config file*/
            "/data/misc/wifi/wpa_supplicant.conf"       /*supplicant config template file*/
        },
        {/*P2P driver parameter*/
            WIFI_P2P_DRIVER_MODULE_NAME,
            WIFI_P2P_DRIVER_MODULE_NAME " ",
            WIFI_P2P_DRIVER_MODULE_PATH,
            WIFI_P2P_DRIVER_MODULE_ARG,
            WIFI_P2P_FIRMWARE_LOADER,
            "wlan.p2p_driver.status",
            "p2p_supplicant0",
            "init.svc.p2p_supplicant0",
            "/data/misc/wifi/p2p.conf",
            "/data/misc/wifi/p2p.conf"
        },
        {/*Hotspot driver parameter*/
            WIFI_HOTSPOT_DRIVER_MODULE_NAME,
            WIFI_HOTSPOT_DRIVER_MODULE_NAME " ",
            WIFI_HOTSPOT_DRIVER_MODULE_PATH,
            WIFI_HOTSPOT_DRIVER_MODULE_ARG,
            WIFI_HOTSPOT_FIRMWARE_LOADER,
            "wlan.p2p_driver.status",
            "p2p_supplicant1",
            "init.svc.p2p_supplicant1",
            "/data/misc/wifi/p2p.conf",
            "/data/misc/wifi/p2p.conf"
        },
    };

static const char MODULE_FILE[] = "/proc/modules";
ModuleCtrl *DriverCtrl::sModuleCtrl = NULL;

DriverCtrl::DriverCtrl() {
    if (!sModuleCtrl) {
        sModuleCtrl = new ModuleCtrl();
    }

}
DriverCtrl::~DriverCtrl() {
}

int DriverCtrl::load(int u4NetIf)
{
    char driver_status[PROPERTY_VALUE_MAX];
    const DriverParameter *tarDrv = &aDriverParameter[u4NetIf];

    ALOGD("Load_driver %d", u4NetIf);

#ifdef LOAD_MODULE_ONCE

    if (!checkLoaded(tarDrv->acModuleTag, tarDrv->acPropName)){
            goto error;
    }else
 	    property_set(tarDrv->acPropName, "ok");

#else
    if (checkLoaded(tarDrv->acModuleTag, tarDrv->acPropName)){	
	if (strcmp(tarDrv->acFirmwareLoader,"") == 0) {
		property_set(tarDrv->acPropName, "ok");
	} else {
		property_set("ctl.start", tarDrv->acFirmwareLoader);
	}
    }else
	goto error;

#endif

    return 0;
error:
    return -1;

}

int DriverCtrl::unload(int u4NetIf)
{
    int count = UNLOAD_CHK_COUNT; /* wait at most 10 seconds for completion */
    const DriverParameter *tarDrv = &aDriverParameter[u4NetIf];

    char legacyModuleName[64];
    char legacyModuleTag[64];
    char *end;

    ALOGD("Unload_driver %d", u4NetIf);

    ALOGD("rmmod %s", tarDrv->acModuleName);
    if (sModuleCtrl->rmmod(tarDrv->acModuleName) == 0) {
        while (count-- > 0) {
            if (!checkLoaded(tarDrv->acModuleTag, tarDrv->acPropName))
                break;
            usleep(UNLOAD_CHK_DELAY);
        }
        if (count) {
            property_set(tarDrv->acPropName, "unloaded");
            return 0;
        }
    }

    //For legacy driver module
    sprintf(legacyModuleName, "%s", tarDrv->acModuleName);
    end = strchr(legacyModuleName, '_');
    if(!end) {
        ALOGD("unable to locate legacy module name");
        return -1;
    }
    *end = '\0';
    sprintf(legacyModuleTag, "%s ", legacyModuleName);

    ALOGD("rmmod %s", legacyModuleName);
    if (sModuleCtrl->rmmod(legacyModuleName) == 0) {
        while (count-- > 0) {
            if (!checkLoaded(legacyModuleTag, tarDrv->acPropName))
                break;
            usleep(UNLOAD_CHK_DELAY);
        }
        if (count) {
            property_set(tarDrv->acPropName, "unloaded");
            return 0;
        }
    }

    return -1;
}

int DriverCtrl::checkLoaded(
    const char *moduleTag,
    const char *propName)
{
    char driver_status[PROPERTY_VALUE_MAX];
    FILE *proc;
    char line[sizeof(moduleTag)+10];
    static int checked = 0;

    if( 0 ) {
        if (!property_get(propName, driver_status, NULL)
                || strcmp(driver_status, "ok") != 0) {
            return 0;  /* driver not loaded */
        }
    }
    /*
     * If the property says the driver is loaded, check to
     * make sure that the property setting isn't just left
     * over from a previous manual shutdown or a runtime
     * crash.
     */
    if ((proc = fopen(MODULE_FILE, "r")) == NULL) {
        ALOGW("Could not open %s: %s", MODULE_FILE, strerror(errno));
        property_set(propName, "unloaded");
        checked = 1;
        return 0;
    }
    while ((fgets(line, sizeof(line), proc)) != NULL) {
        if (strncmp(line, moduleTag, strlen(moduleTag)) == 0) {
            fclose(proc);
            checked = 1;
            return 1;
        }
    }
    checked = 1;
    fclose(proc);
    property_set(propName, "unloaded");
    return 0;
}

