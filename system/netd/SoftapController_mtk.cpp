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

#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>

#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <netinet/in.h>
#include <arpa/inet.h>

#include <linux/wireless.h>

#include <openssl/evp.h>
#include <openssl/sha.h>

#define LOG_TAG "SoftapController"
#include <cutils/log.h>
#include <netutils/ifc.h>
#include <private/android_filesystem_config.h>
#include "mtk_wifi.h"

#include "SoftapController_mtk.h"

static const char HOSTAPD_CONF_FILE[]    = "/data/misc/wifi/hostapd.conf";

SoftapController::SoftapController() {
    ALOGV("SoftapController");
    mPid = 0;
    mDaemonState = 0;
    mSock = socket(AF_INET, SOCK_DGRAM, 0);
    if (mSock < 0)
        ALOGE("Failed to open socket");
    memset(mIface, 0, sizeof(mIface));
}

SoftapController::~SoftapController() {
    ALOGV("~SoftapController");
    if (mSock >= 0)
        close(mSock);
}

int SoftapController::setCommand(char *iface, const char *cmd, unsigned buflen) {
    int connectTries = 0;
    unsigned replybuflen;
    int ret = 0;
    
    if (mDaemonState != 1) {
        ALOGD("Softap startap - daemon is not running");
        startDriver(mIface);
    }        
    
    if (buflen == 0) {
        replybuflen = SOFTAP_MAX_BUFFER_SIZE;
    } 
    else {
        replybuflen = buflen;
    }
    
    // <1> connect to the daemon
    while (true) {
        //ALOGD("try to connect to daemon");
        if (wifi_connect_to_supplicant(iface) == 0) {
            //ALOGD("connect to daemon");
            break;
        }
        //maximum delay 12s
        if (connectTries++ < 40) {
            sched_yield();
            //ALOGD("softap sleep %d us\n", AP_CONNECT_TO_DAEMON_DELAY);
            usleep(AP_CONNECT_TO_DAEMON_DELAY);
        } else {
            ALOGE("connect to daemon failed!");
            return -1;
        }
    }
       
    if (wifi_command(iface, cmd, mBuf, &buflen) != 0) {
        ALOGE("Command failed: \"%s\"", cmd);
        ret = -1;
    }
    else {
        ALOGD("Command OK: \"%s\"", cmd);
        mBuf[buflen] = '\0';
    }
    
    wifi_close_supplicant_connection(iface);
    
    return ret;
}

int SoftapController::startDriver(char *iface) {
    int ret;

    ALOGD("startDriver: %s", iface);
    
    if (mSock < 0) {
        ALOGE("Softap driver start - failed to open socket");
        return -1;
    }
    if (!iface || (iface[0] == '\0')) {
        ALOGD("Softap driver start - wrong interface");
        iface = mIface;
    }
    if (mDaemonState == 1) {
        ALOGD("Softap startap - daemon is already running");
        return -1;
    }        
    
    ifc_init();
    ret = ifc_up(iface);
    ifc_close();    
    
    *mBuf = 0;
    ret = wifi_ap_start_supplicant();
    if (ret < 0) {
        ALOGE("Softap daemon start: %d", ret);
        return ret;
    }

    mDaemonState = 1;
    
    usleep(AP_DRIVER_START_DELAY);
    ALOGV("Softap daemon start: %d", ret);
    return ret;
}

int SoftapController::stopDriver(char *iface) {
    int ret;

    ALOGD("stopDriver: %s", iface);
    
    if (mSock < 0) {
        ALOGE("Softap driver stop - failed to open socket");
        return -1;
    }
    if (!iface || (iface[0] == '\0')) {
        ALOGD("Softap driver stop - wrong interface");
        iface = mIface;
    }
    *mBuf = 0;

    ifc_init();
    ret = ifc_down(iface);
    ifc_close();
    if (ret < 0) {
        ALOGE("Softap %s down: %d", iface, ret);
    }

    ret = wifi_ap_stop_supplicant();
    
    mDaemonState = 0;
    
    ALOGV("Softap daemon stop: %d", ret);
    return ret;
}

int SoftapController::startSoftap() {
    pid_t pid = 1;
    int ret = 0;

    ALOGD("startSoftap: %s", mIface);
    
    if (mPid) {
        ALOGE("Softap already started");
        return 0;
    }
    if (mSock < 0) {
        ALOGE("Softap startap - failed to open socket");
        return -1;
    }
    if (!mDaemonState) {
        ALOGE("Softap startap - daemon is not running");
        return -1;
    }    
    
    if (!pid) {
        ALOGE("Should never get here!");
        return -1;
    } else {
        *mBuf = 0;
        ret = setCommand(mIface, "p2p_enable_device");
        ret = setCommand(mIface, "start_ap");
        if (ret) {
            ALOGE("Softap startap - failed: %d", ret);
        }
        else {
           mPid = pid;
           ALOGD("Softap startap - Ok");
           usleep(AP_BSS_START_DELAY);
        }
    }
    return ret;

}

int SoftapController::stopSoftap() {
    int ret;
    
    ALOGD("stopSoftap: %s", mIface);

    if (mPid == 0) {
        ALOGE("Softap already stopped");
        return 0;
    }

    if (mSock < 0) {
        ALOGE("Softap stopap - failed to open socket");
        return -1;
    }
    *mBuf = 0;
    ret = setCommand(mIface, "p2p_disable_device");
    ret = setCommand(mIface, "stop_ap");
    mPid = 0;
    ALOGV("Softap service stopped: %d", ret);
    usleep(AP_BSS_STOP_DELAY);
    return ret;
}

bool SoftapController::isSoftapStarted() {
    ALOGV("isSoftapStarted: %s", (mPid != 0 ? "TRUE" : "FALSE"));
    return (mPid != 0 ? true : false);
}

int SoftapController::setConfig(const char *cmd, const char *arg)
{
    char cmd_str[SOFTAP_MAX_BUFFER_SIZE];
    
    snprintf(cmd_str, SOFTAP_MAX_BUFFER_SIZE, "cfg_ap %s %s", cmd, arg);
    
    return setCommand(mIface, cmd_str);
}

/*
 * Arguments:
 *      argv[2] - wlan interface
 *      argv[3] - SSID
 *	argv[4] - Security
 *	argv[5] - Key
 *	argv[6] - Channel
 *	argv[7] - Preamble
 *	argv[8] - Max SCB
 */
int SoftapController::setSoftap(int argc, char *argv[]) {
    char psk_str[2*SHA256_DIGEST_LENGTH+1];
    char str_arg[SOFTAP_MAX_BUFFER_SIZE];
    int ret = 0, i = 0, fd;
    char *ssid, *iface;

    ALOGD("setSoftap");
    
    if (mSock < 0) {
        ALOGE("Softap set - failed to open socket");
        return -1;
    }
    if (argc < 4) {
        ALOGE("Softap set - missing arguments");
        return -1;
    }

    strncpy(mIface, "ap0", sizeof(mIface));
    iface = argv[2];

    /* Create command line */
    if (argc > 3) {
        ssid = argv[3];
    } else {
        ssid = (char *)"AndroidAP";
    }
    sprintf(str_arg, "\"%s\"", ssid);
    ret = setConfig("ssid", str_arg);
    if (argc > 4) {
        sprintf(str_arg, "\"%s\"", argv[4]);
        ret = setConfig("sec", str_arg);
    } else {
        ret = setConfig("sec", "\"open\"");
    }
    if (argc > 5) {
        sprintf(str_arg, "\"%s\"", argv[5]);
        ret = setConfig("key", str_arg);
    } else {
        ret = setConfig("key", "\"12345678\"");
    }
    if (argc > 6) {
        ret = setConfig("ch", argv[6]);
    } else {
        ret = setConfig("ch", "0");
    }
    if (argc > 7) {
        ret = setConfig("ch_width", argv[7]);
    } else {
        ret = setConfig("ch_width", "1");
    }
    if (argc > 8) {
        ret = setConfig("max_sta", argv[8]);
    } else {
        ret = setConfig("max_sta", "5");
    }
    if (argc > 9) {
        ret = setConfig("preamble", argv[9]);
    } else {
        ret = setConfig("preamble", "0");
    }
    if (argc > 10) {
        ret = setConfig("max_scb", argv[10]);
    } else {
        ret = setConfig("max_scb", "8");
    }

    if (ret) {
        ALOGE("Softap set - failed: %d", ret);
    }
    else {
        ALOGD("Softap set - Ok");
        usleep(AP_SET_CFG_DELAY);
    }
    return ret;
}

void SoftapController::generatePsk(char *ssid, char *passphrase, char *psk_str) {
    unsigned char psk[SHA256_DIGEST_LENGTH];
    int j;
    // Use the PKCS#5 PBKDF2 with 4096 iterations
    PKCS5_PBKDF2_HMAC_SHA1(passphrase, strlen(passphrase),
            reinterpret_cast<const unsigned char *>(ssid), strlen(ssid),
            4096, SHA256_DIGEST_LENGTH, psk);
    for (j=0; j < SHA256_DIGEST_LENGTH; j++) {
        sprintf(&psk_str[j<<1], "%02x", psk[j]);
    }
    psk_str[j<<1] = '\0';
}


/*
 * Arguments:
 *	argv[2] - interface name
 *	argv[3] - AP or STA
 */
int SoftapController::fwReloadSoftap(int argc, char *argv[])
{
    int ret, i = 0;
    char *iface;
    char *fwpath;

    if (mSock < 0) {
        ALOGE("Softap fwrealod - failed to open socket");
        return -1;
    }
    if (argc < 4) {
        ALOGE("Softap fwreload - missing arguments");
        return -1;
    }

    iface = argv[2];

    if (strcmp(argv[3], "AP") == 0) {
        fwpath = (char *)wifi_get_fw_path(WIFI_GET_FW_PATH_AP);
    } else if (strcmp(argv[3], "P2P") == 0) {
        fwpath = (char *)wifi_get_fw_path(WIFI_GET_FW_PATH_P2P);
    } else {
        fwpath = (char *)wifi_get_fw_path(WIFI_GET_FW_PATH_STA);
    }
    if (!fwpath)
        return -1;

    ret = wifi_change_fw_path((const char *)fwpath);

    if (ret) {
        ALOGE("Softap fwReload - failed: %d", ret);
    }
    else {
        ALOGV("Softap fwReload - Ok: %s", fwpath);
    }
    return ret;
}

int SoftapController::clientsSoftap(char **retbuf)
{
    int ret;

    if (mSock < 0) {
        ALOGE("Softap clients - failed to open socket");
        return -1;
    }
    *mBuf = 0;
    ret = setCommand(mIface, "AP_GET_STA_LIST", SOFTAP_MAX_BUFFER_SIZE);
    if (ret) {
        ALOGE("Softap clients - failed: %d", ret);
    } else {
        asprintf(retbuf, "Softap clients:%s", mBuf);
        ALOGD("Softap clients:%s", mBuf);
    }
    return ret;
}

#define PRIV_CMD_GET_CH_LIST    24
#define IOCTL_GET_INT           (SIOCIWFIRSTPRIV + 1)

uint32_t au4ChannelList[64] = { 0 };

int SoftapController::getChannelList(int buf_len, char *buf_list)
{
    struct iwreq wrq = {0};
    int i = 0, skfd = 0;

    /* initialize socket */
    skfd = socket(PF_INET, SOCK_DGRAM, 0);

    wrq.u.data.pointer = &(au4ChannelList[0]);
    wrq.u.data.length = sizeof(uint32_t) * 64;
    wrq.u.data.flags = PRIV_CMD_GET_CH_LIST;
    strncpy(wrq.ifr_name, "wlan0", IFNAMSIZ);

    /* do ioctl */
    if (ioctl(skfd, IOCTL_GET_INT, &wrq) >= 0) {
        if (wrq.u.data.length > 0) {
            // <1> retrieve the first string
            sprintf(buf_list, "%d ", au4ChannelList[0]);
            // <2> concat the following channel list
            for (i = 1; i < wrq.u.data.length; i++) {
                char tmp[16];
                sprintf(tmp, "%d ", au4ChannelList[i]);
                strcat(buf_list, tmp);
            }
        }
        buf_list[strlen(buf_list)] = '\0';
    } else {
        sprintf(buf_list, "CHANNEL_LIST_ERROR"); 
    }
    close(skfd);
    return 0;
}
