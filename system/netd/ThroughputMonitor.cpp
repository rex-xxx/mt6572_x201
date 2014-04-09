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

#include <arpa/inet.h>
#include <dirent.h>
#include <errno.h>
#include <linux/if.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <string.h>
#include <fcntl.h>


#define LOG_TAG "ThroughputMonitor"
#define DBG 1
#define HIGH_SWITCH 2048     /*KBps, =16Mbps*/
#define LOW_SWITCH 1536      /*KBps, =12Mbps*/

#include <cutils/log.h>

#include "ThroughputMonitor.h"

ThroughputMonitor::ThroughputMonitor() {
    mThread = 0;
	mRunning = 0;
}

ThroughputMonitor::~ThroughputMonitor() {
    mThread = 0;
	mRunning = 0;
}

int ThroughputMonitor::start() {
	if(mThread == 0){
	    pthread_create(&mThread, NULL,
	                   ThroughputMonitor::threadStart, this);
    }else{
		ALOGW("ThroughputMonitor is already running");
	}
	return 0;
}

void ThroughputMonitor::stop() {
	ALOGI("ThroughputMonitor try to stop!");
	if(mRunning != 0)
        mRunning = 0;
	mThread = 0;
	return;
}

void* ThroughputMonitor::threadStart(void* obj) {
    ThroughputMonitor* monitor = reinterpret_cast<ThroughputMonitor*>(obj);

    monitor->run();
    //delete monitor;   

	pthread_exit(NULL);
	ALOGI("ThroughputMonitor thread exit!");
    return NULL;
}

long ThroughputMonitor::readCount(char const* filename) {
    char buf[80];
    int fd = open(filename, O_RDONLY);
    if (fd < 0) {
        if (errno != ENOENT) ALOGE("Can't open %s: %s", filename, strerror(errno));
        return -1;
    }

    int len = read(fd, buf, sizeof(buf) - 1);
    if (len < 0) {
        ALOGE("Can't read %s: %s", filename, strerror(errno));
        close(fd);
        return -1;
    }

    close(fd);
    buf[len] = '\0';
    return atoll(buf);
}

int ThroughputMonitor::setUltraHigh(bool set){
	const char *proc_file = "/sys/bus/platform/drivers/dramc_high/dramc_high";

	int fd = open(proc_file, O_WRONLY);
    if (fd < 0) {
        ALOGE("Failed to open %s (%d, %s)", proc_file, errno, strerror(errno));
        return -1;
    }

    if (write(fd, (set ? "1" : "0"), 1) != 1) {
        ALOGE("Failed to write %s (%d, %s)", proc_file, errno, strerror(errno));
        close(fd);
        return -1;
    }
    close(fd);
	return 0;
}

void ThroughputMonitor::run() {
    char filename[80];
    int idx = 0; int uh_enabled = 0;
    long current = 0; long last = 0;
    long h_switch = HIGH_SWITCH * 1024 / 5;    /* bytes per 100ms */   
    long l_switch = LOW_SWITCH * 1024 / 5;     /* bytes per 100ms */ 
	const char* iface_list[] = {
		"ccmni0",
		"ccmni1",
		"ccmni2",
		0
	};
	
    mRunning = 1;
	ALOGI("ThroughputMonitor start, h = %ld, l = %ld !", h_switch, l_switch);
	ALOGI("ThroughputMonitor is running, thread id = %d!", gettid());
    while(mRunning){
		while (iface_list[idx] != 0) {
			snprintf(filename, sizeof(filename), "/sys/class/net/%s/statistics/rx_bytes",
					 iface_list[idx]);
			long number = readCount(filename);
			if (number >= 0) {
				current += number;
			}
			idx++;
		}
		//ALOGD("ThroughputMonitor delta %ld", current-last);
        if(current - last > h_switch && last != 0){
			if(uh_enabled == 0){
			    ALOGI("ThroughputMonitor enable ultra high!");
			    /*enable ultra high*/
				setUltraHigh(true);
			}
			uh_enabled = 5;
		}
        if(current >= last && current - last < l_switch){
			if(uh_enabled > 0){
				ALOGI("ThroughputMonitor un_enabled = %d!", uh_enabled);
				if(--uh_enabled == 0){
				   ALOGI("ThroughputMonitor disable ultra high!");
				   /*disable ultra high*/
				   setUltraHigh(false);
			   }
			}
		}
        last = current;
        current = 0;
		idx = 0;
        usleep(200000);
	}
}


