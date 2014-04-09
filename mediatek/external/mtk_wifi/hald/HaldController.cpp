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

#include <cutils/properties.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <arpa/inet.h>
#include <net/if_arp.h>		    /* For ARPHRD_ETHER */
#include <sys/socket.h>		    /* For AF_INET & struct sockaddr */
#include <netinet/in.h>         /* For struct sockaddr_in */
#include <netinet/if_ether.h>

#include <netutils/ifc.h>

#include <linux/wireless.h>

#define LOG_TAG "Hald"
#include <cutils/log.h>

#include "HaldController.h"
#ifdef MTK_SDIORETRY_SUPPORT
#include "libnvram.h"
#endif

#define MAX_NVRAM_RESTORE_READY_RETRY_NUM (10)
#define NVRAM_RESTORE_POLLING_TIME_USEC   (500 * 1000)

RfkillCtrl *HaldController::sRfkillCtrl = NULL;
DriverCtrl *HaldController::sDriverCtrl = NULL;

HaldController::HaldController() {
    if (!sRfkillCtrl) {
        sRfkillCtrl = new RfkillCtrl();
    }
    if (!sDriverCtrl) {
        sDriverCtrl = new DriverCtrl();
    }

    isWifiActive = false;
    isP2pActive = false;
    isHotspotActive = false;
}

HaldController::~HaldController() {
}

#ifdef MTK_SDIORETRY_SUPPORT
typedef struct
{
 int clk_src;
 int clk_src_freq;
 int data_red;
 int cmd_driving;
 int clk_latch;
 int clkpad_red;
 int cmd_phase;
 int data_phase;
} SDIO_RETRY_REG;

typedef struct
{
 int retry_flag;
 SDIO_RETRY_REG sdio;
}MT6573_SDIO_RETRY;

int  sdio_retry = {0};
int rec_size = 0;
int rec_num = 0;
MT6573_SDIO_RETRY  sdioRetrySetting;
int needReadSetting = 0;

extern int iFileSDIO_RETRYLID;

int writeRetrySetting()
{
	int ret = -1, fd = -1;

	fd = open("/proc/sdio_retry", O_WRONLY);
	if(0 < fd)
		ret = write(fd, &sdioRetrySetting, sizeof(sdioRetrySetting));
	else
		ALOGD("Open /proc/sdio_retry failed\n");

	close(fd);
	ALOGD("Write /proc/sdio_retry return %d\n", ret);

	return ret;
}

int readRetrySetting()
{
	int ret = -1, fd = -1;

	fd = open("/proc/sdio_retry", O_RDONLY);
	if(0 < fd)
		ret = read(fd, &sdioRetrySetting, sizeof(sdioRetrySetting));
	else
		ALOGD("Open /proc/sdio_retry failed\n");

	close(fd);
	ALOGD("Read /proc/sdio_retry return %d\n", ret);

	return ret;
}
#endif

int HaldController::loadDriver(const char *ifname){
#ifdef CFG_ENABLE_NVRAM_CHECK
    int nvram_restore_ready_retry = 0;
    char nvram_init_val[32];
	static bool fg_is_nvram_chk_failed = false;

    ALOGD("Check NvRAM status.");
    while(nvram_restore_ready_retry < MAX_NVRAM_RESTORE_READY_RETRY_NUM) {
        nvram_restore_ready_retry++;
        property_get("nvram_init", nvram_init_val, NULL);
        if(strcmp(nvram_init_val, "Ready") == 0) {
            ALOGD("NvRAM is READY!");
			fg_is_nvram_chk_failed = false;
            break;
        } else if (fg_is_nvram_chk_failed){
			ALOGE("NvRAM status check is still failed! NvRAM content may be WRONG!");
			break;
        } else {
            usleep(NVRAM_RESTORE_POLLING_TIME_USEC);
		}
    }
    if(nvram_restore_ready_retry >= MAX_NVRAM_RESTORE_READY_RETRY_NUM) {
		fg_is_nvram_chk_failed = true;
        ALOGE("NvRAM status check timeout(%dus)! NvRAM content may be WRONG!", MAX_NVRAM_RESTORE_READY_RETRY_NUM * NVRAM_RESTORE_POLLING_TIME_USEC);
    }
#endif

#ifdef MTK_SDIORETRY_SUPPORT
	//write the sdio retry setting. add by mtk80743
	{

		sdio_retry = NVM_GetFileDesc(iFileSDIO_RETRYLID, &rec_size, &rec_num, true);
		if(read(sdio_retry, &sdioRetrySetting, rec_num*rec_size) < 0){
					ALOGD("read iFileSDIO_RETRYLID failed %s\n", strerror(errno));
		}else{
			if(sdioRetrySetting.retry_flag == 0 && sdioRetrySetting.sdio.clk_src == 0){
				needReadSetting = 1;
			}else{
				needReadSetting = 0;
				writeRetrySetting();
			}
		}
		NVM_CloseFileDesc(sdio_retry);
	}
#endif
    /*LOAD WIFI*/
    if (!strcmp(ifname, "wifi")) {
        /*if wifi or its sub function is on, no need to load wifi again*/
        if(isHotspotActive || isP2pActive || isWifiActive) {
            /*do nothing*/
            ALOGE("Wifi driver is already loaded, no need to load again.");
            isWifiActive = true;
            return 0;
        /*load wifi driver*/
        } else {
            ALOGD("Start load wifi driver.");
            /*load wifi driver*/
            sDriverCtrl->load(NETWORK_IFACE_WIFI);
            /*turn on wifi power*/
            if(0 > powerOn())
				return -1;
            /*set flag*/
            isWifiActive = true;
            return 0;
        }
    /*LOAD HOTSPOT*/
    } else if (!strcmp(ifname, "hotspot")) {
        if(isHotspotActive) {
            ALOGE("Hotspot driver is already loaded, no need to load again.");
            return 0;
        }
		/*if wifi is not on, MUST load wifi and turn on wifi power first*/
		if(!isWifiActive){
			sDriverCtrl->load(NETWORK_IFACE_WIFI);
			powerOn();
		}
		/*if p2p is on, unload p2p driver*/
		if(isP2pActive) {
            ALOGE("Unload P2P driver first.");
            //sDriverCtrl->unload(NETWORK_IFACE_P2P);
            setP2pMode(0,0);
            isP2pActive = false;
        }
        /*load hotspot driver*/
        ALOGD("Start load hotspot driver.");
        //sDriverCtrl->load(NETWORK_IFACE_HOTSPOT);
        setP2pMode(1,1);
        isHotspotActive = true;
#ifdef CFG_ENABLE_RFKILL_IF_FOR_CFG80211
		/*enable hotspot rfkill interface for cfg80211*/
		sRfkillCtrl->setAllState(1);
#endif
        return 0;
    /*LOAD P2P*/
    } else if (!strcmp(ifname, "p2p")) {
        if(isP2pActive) {
            ALOGE("P2P driver is already loaded, no need to load again.");
            return 0;
        }
        /*if wifi is not on, MUST load wifi and turn on wifi power first*/
        if(!isWifiActive){
            sDriverCtrl->load(NETWORK_IFACE_WIFI);
            powerOn();
        }
		/*if hotspot is on, unload hotspot driver*/
		if(isHotspotActive) {
            ALOGE("Unload Hotspot driver first.");
            //sDriverCtrl->unload(NETWORK_IFACE_HOTSPOT);
            setP2pMode(0,0);
            isHotspotActive = false;
		}
        /*load p2p driver*/
        ALOGD("Start load P2P driver.");
        //sDriverCtrl->load(NETWORK_IFACE_P2P);
        setP2pMode(1,0);
        isP2pActive = true;
#ifdef CFG_ENABLE_RFKILL_IF_FOR_CFG80211
		/*enable p2p rfkill interface for cfg80211*/
		sRfkillCtrl->setAllState(1);
#endif
        return 0;
    }
    return -1;
}

int HaldController::unloadDriver(const char *ifname){
#ifdef MTK_SDIORETRY_SUPPORT
    //read the sdio retry setting. add by mtk80743
    if(needReadSetting == 1){
	readRetrySetting();
	sdio_retry = NVM_GetFileDesc(iFileSDIO_RETRYLID, &rec_size, &rec_num, false);
	if(lseek(sdio_retry,0,SEEK_SET)<0){
		ALOGD("lseek %d iFileSDIO_RETRYLID failed %s\n",
			sdio_retry, strerror(errno));
		}
	if(write(sdio_retry, &sdioRetrySetting, rec_num*rec_size) < 0){
		ALOGD("write %d iFileSDIO_RETRYLID failed %s\n",
			sdio_retry, strerror(errno));
	}else{
		ALOGD("write iFileSDIO_RETRYLID successed\n");
	}
	NVM_CloseFileDesc(sdio_retry);
    }
#endif
    /*UNLOAD WIFI*/
    if (!strcmp(ifname, "wifi")) {
        /*if sub function is on or wifi is already off, do nothing*/
        if(isHotspotActive || isP2pActive || (!isWifiActive)) {
            ALOGD("No need to unload wifi driver");
            isWifiActive = false;
            /*do nothing*/
            return 0;
        /*if sub function are off and wifi is on*/
        } else {
            ALOGD("Start unload wifi driver.");
            /*No need to unload wifi driver*/
            powerOff();
            isWifiActive = false;
            return 0;
        }
    /*UNLOAD HOTSPOT*/
    } else if (!strcmp(ifname, "hotspot")) {
    	/* Note: for cfg80211 rfkill issue,
    		  *           the power off sequence shall be garanteed,
		  *	1. remove p2p module
		  *   2. power off wlan
    		  */
        if(false == isHotspotActive) {
            ALOGE("Hotspot driver is already unloaded, no need to unload again.");
    	} else {
			ALOGD("Start unload Hotspot driver.");
			/*unload hotspot driver*/
			//sDriverCtrl->unload(NETWORK_IFACE_HOTSPOT);
            setP2pMode(0,0);
		}
        /*if wifi is not on, turn off power*/
    	if(false == isWifiActive){
        	powerOff();
    	}
		isHotspotActive = false;
        return 0;
    /*UNLOAD P2P*/
    } else if (!strcmp(ifname, "p2p")) {
    	/* Note: for cfg80211 rfkill issue,
    		  *           the power off sequence shall be garanteed,
		  *	1. remove p2p module
		  *   2. power off wlan
    		  */
        if(false == isP2pActive) {
            ALOGE("P2P driver is already unloaded, no need to unload again.");
		} else {
			ALOGD("Start unload P2P driver.");
			/*unload p2p driver*/
			//sDriverCtrl->unload(NETWORK_IFACE_P2P);
            setP2pMode(0,0);
		}
        /*if wifi is not on, turn off power*/
        if(false == isWifiActive){
            powerOff();
        }
        isP2pActive = false;
        return 0;
    }
    return -1;
}

int HaldController::setPower(int enable) {
    int sz;
    int fd = -1;
    const char buffer = (enable ? '1' : '0');

    fd = open(WIFI_POWER_PATH, O_WRONLY);
    if (fd < 0) {
        ALOGE("Open \"%s\" failed", WIFI_POWER_PATH);
        goto out;
    }
    sz = write(fd, &buffer, 1);
    if (sz < 0) {
        ALOGE("Set \"%s\" [%c] failed", WIFI_POWER_PATH, buffer);
        goto out;
    }

out:
    if (fd >= 0) close(fd);
    return sz;
}

void HaldController::powerOff() {
	ALOGD("Power off wlan.");
    //sRfkillCtrl->setWifiState(0);
    setPower(0);
}

int HaldController::powerOn() {
	ALOGD("Power on wlan.");
    //sRfkillCtrl->setWifiState(1);
    return (setPower(1));
}

void HaldController::checkInterface(const char *iface, int enable) {
    int u4Count = 0;
    int if_idx;
    int ret_check;
    
    if(ifc_init() != 0) {
        ALOGE("[%s] Interface check failed", iface);
        return;
    }
    
    if(enable) {
        ret_check = -1;
    }
    else {
        ret_check = 0;
    }

    usleep(200000);
    
    while(ifc_get_ifindex(iface, &if_idx) == ret_check) {
        ALOGD("[%s] interface is not ready, wait %dus", iface, 300000);
        sched_yield();
        usleep(300000);
        if (++u4Count >= 40) {
            ALOGE("[%s] Interface check failed", iface);
            ifc_close();
            return;
        }
    }
        
    if(enable) {
        ALOGD("[%s] interface is appear", iface);
        }
    else {
        ALOGD("[%s] interface is disappear", iface);
    }

    ifc_close();
    return;
}

void HaldController::setP2pMode(int enable, int mode) {
    struct iwreq wrq = {0};
    int i = 0, skfd = 0;
    int param[2];
    
    param[0] = enable;
    param[1] = mode;
    
    /* initialize socket */
    skfd = socket(PF_INET, SOCK_DGRAM, 0);
    
    wrq.u.data.pointer = &(param[0]);
    wrq.u.data.length = 2;
    wrq.u.mode = PRIV_CMD_P2P_MODE;
    memcpy(wrq.u.name + 4, param, sizeof(int) * 2);
    
    strncpy(wrq.ifr_name, "wlan0", IFNAMSIZ);

    /* do ioctl */
    if (ioctl(skfd, IOCTL_SET_INT, &wrq) >= 0) {
        ALOGD("SET_P2P_MODE enable[%d], mode[%d] Success", enable, mode);
    } else {
        ALOGE("SET_P2P_MODE enable[%d], mode[%d] Failed", enable, mode);
        ALOGE("%s", strerror(errno));
    }
    close(skfd);
    
#if 0    
    if(mode) {
        checkInterface("ap0", enable);
    }
    else {
        checkInterface("p2p0", enable);
    }
#endif    
}

/*check wifi or sub function is active or not*/
bool HaldController::isFuncActive() {
    return (isWifiActive || isP2pActive || isHotspotActive);
}

bool HaldController::isP2pFuncActive() {
    return isP2pActive;
}

bool HaldController::isHotspotFuncActive() {
    return isHotspotActive;
}
