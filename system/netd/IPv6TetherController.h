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

#ifndef _IPv6_TETHER_CONTROLLER_H
#define _IPv6_TETHER_CONTROLLER_H

#include <linux/in.h>

#include <utils/List.h>

#include "SecondaryTableController.h"

#ifndef IFNAMSIZ
#define IFNAMSIZ 16
#endif


#ifdef  MTK_IPV6_TETHER_NDP_MODE
#define NDP_MODE    1
#else
#define RADVD_MODE  2
#endif

#define WIFI_FLAG    1
#define USB_FLAG     2
#define BT_FLAG      4

#define TETHER_IFACE_NUM	3

class IPv6TetherController {

public:
    IPv6TetherController();
    virtual ~IPv6TetherController();

    int enableIPv6Tether(const int argc, char **argv);
    int disableIPv6Tether(const int argc, char **argv);
    bool getIpv6FwdEnabled();
    int  setIpv6FwdEnabled(bool enable);
	int startDhcp6s(const char *intIface);
	int stopDhcp6s(const char *intIface);

private:
    int mCount;

    int setAllMulticast(bool set, const char *intIface, const char *extIface);       

    pid_t mPid;
	pid_t mPid_dhcp6s[TETHER_IFACE_NUM];
    int startRadvd();
    int stopRadvd();
    int updateRadvdConf();
	int accept_RA(bool set, const char *intIface);
	int enableProxy_ndp(bool set);

	int findInfaceNumber(const char *intIface);
    int findTableNumber(const char *iface, bool enable);
    int setDefaults();
    int runCmd(const char *path, const char *cmd);
    bool checkInterface(const char *iface);
};

#endif
