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

#include "RfkillCtrl.h"


RfkillCtrl::RfkillCtrl() {
    wifiId = -1;
    wifiStatePath = NULL;
}

RfkillCtrl::~RfkillCtrl() {
}

int RfkillCtrl::checkWifiState()
{
    int sz;
    int fd = -1;
    int ret = -1;
    char buffer;

    if (wifiId == -1) {
        if (getWifiStatePath()) goto out;
    }

    fd = open(wifiStatePath, O_RDONLY);
    if (fd < 0) {
        goto out;
    }

    sz = read(fd, &buffer, 1);
    if (sz != 1) {
        goto out;
    }

    switch(buffer) {
    case '1':
        ret = 1;
        break;
    case '2':
        ret = 0;
        break;
    }

out:
    if (fd >= 0) close(fd);
    return ret;
}

int RfkillCtrl::getWifiStatePath()
{
    char path[64];
    char buf[16];
    int fd;
    int sz;
    int id;
    for (id = 0; ; id++) {
        snprintf(path, sizeof(path), "/sys/class/rfkill/rfkill%d/type", id);
        fd = open(path, O_RDONLY);
        if (fd < 0) {
            return -1;
        }
        sz = read(fd, &buf, sizeof(buf));
        close(fd);
        if (sz >= 4 && memcmp(buf, "wlan", 4) == 0) {
            wifiId = id;
            break;
        }
    }

	if(wifiStatePath) {
		free(wifiStatePath);
	}

    asprintf(&wifiStatePath, "/sys/class/rfkill/rfkill%d/state",
         wifiId);
    return 0;

}

int RfkillCtrl::setWifiState(int on)
{
    if (wifiId == -1 && getWifiStatePath()) {
		return -1;
    }

	return setStateByPath(on, wifiStatePath);
}

const char *RfkillCtrl::getName(int idx)
{
	static char name[128];
	ssize_t len;
	char *pos, filename[64];
	int fd;

	snprintf(filename, sizeof(filename) - 1,
				"/sys/class/rfkill/rfkill%u/name", idx);

	fd = open(filename, O_RDONLY);
	if (fd < 0)
		return NULL;

	memset(name, 0, sizeof(name));
	len = read(fd, name, sizeof(name) - 1);

	pos = strchr(name, '\n');
	if (pos)
		*pos = '\0';

	close(fd);

	return name;
}

int RfkillCtrl::setStateByPath(int on, char *statePath)
{
    int sz;
    int fd = -1;
    int ret = -1;
    const char buffer = (on ? '1' : '0');

    fd = open(statePath, O_WRONLY);
    if (fd < 0) {
        goto out;
    }
    sz = write(fd, &buffer, 1);
    if (sz < 0) {
        goto out;
    }
    ret = 0;
out:
    if (fd >= 0) close(fd);
    return ret;
}

int RfkillCtrl::setAllState(int on)
{
	struct rfkill_id id;
	struct rfkill_event event;
	const char *name;
	int len;
	int fd;

	/*Filter out WLAN device Only*/
	id.result = RFKILL_IS_TYPE;
	id.type = RFKILL_TYPE_WLAN;

	fd = open("/dev/rfkill", O_RDONLY);
	if (fd < 0) {
		  ALOGE("Can't open RFKILL control device");
		  return 1;
	}

	if (fcntl(fd, F_SETFL, O_NONBLOCK) < 0) {
		  ALOGE("Can't set RFKILL control device to non-blocking");
		  close(fd);
		  return 1;
	}

	while (1) {
		len = read(fd, &event, sizeof(event));
		if (len < 0) {
			if (errno == EAGAIN)
				  break;
			ALOGE("Reading of RFKILL events failed");
			break;
		}

		if (len != RFKILL_EVENT_SIZE_V1) {
			ALOGE("Wrong size of RFKILL event\n");
			continue;
		}

		if (event.op != RFKILL_OP_ADD)
			continue;

		/* filter out unwanted results */
		switch (id.result)
		{
		case RFKILL_IS_TYPE:
			if (event.type != id.type)
				  continue;
			break;
		case RFKILL_IS_INDEX:
			if (event.idx != id.index)
				  continue;
			break;
		case RFKILL_IS_INVALID:; /* must be last */
		}

		ALOGD("RFKILL id[%d] type[%d] name[%s] blocked[soft:%s hard:%s]",
			event.idx,
			event.type,
			getName(event.idx),
			event.soft ? "y" : "n",
			event.hard ? "y" : "n");

		if(event.soft || event.hard) {
			char path[64];
			snprintf(path, sizeof(path), "/sys/class/rfkill/rfkill%d/state", event.idx);
			setStateByPath(on, path);

			ALOGD("Enable RFKILL interface [%d]  ", event.idx);
		}
	}

	close(fd);
	return 0;

}


