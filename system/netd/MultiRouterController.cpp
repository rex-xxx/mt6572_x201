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
#include <sys/socket.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <cutils/properties.h>


#define LOG_TAG "MultiRouterController"
#include <cutils/log.h>

#include "MultiRouterController.h"

#define TETHER_TAB_NO 71

extern "C" int logwrap(int argc, const char **argv, int background);

static char IP_PATH[] = "/system/bin/ip";
//static char IP_PATH[] = "/system/bin/busybox";


MultiRouterController::MultiRouterController() {
    MultiRouterCount = 0;
}

MultiRouterController::~MultiRouterController() {
}

int MultiRouterController::runIpCmd(const char *cmd) {
    char buffer[255];

    strncpy(buffer, cmd, sizeof(buffer)-1);

    const char *args[16];
    char *next = buffer;
    char *tmp;

    args[0] = IP_PATH;
    //args[1] = "--verbose";
    //int i = 2;
    int i = 1;
    while ((tmp = strsep(&next, " "))) {
        args[i++] = tmp;
        if (i == 16) {
            ALOGE("iptables argument overflow");
            errno = E2BIG;
            return -1;
        }
    }
    args[i] = NULL;

    return logwrap(i, args, 0);
}

int MultiRouterController::setDefaults() {

   char cmd[128];
   snprintf(cmd, sizeof(cmd), "route flush table %d",TETHER_TAB_NO);
	if (runIpCmd(cmd))
        {
          ALOGE("setDefaults: RUN: %s fail ",cmd);
		  return -1;
	    }
   
    return 0;
}


bool MultiRouterController::interfaceExists(const char *iface) {
    // XXX: STOPSHIP - Implement this
    return true;
}




int MultiRouterController::doMultiRouterCommands(const char *intIface,const char *extIface, bool add) {
    char cmd[255];
    char prop_value[32] ;

  /*
	 property_get("ro.operator.optr", prop_value, NULL);
   	 if(strcmp(prop_value, "op03") != 0 && strcmp(prop_value, "OP03") != 0 )
   	 {
       ALOGE("ERR:Call MultiRouterCommands In No-Orange Building" );
	   return -1;
	 }*/

	ALOGD("doMultiRouterCommands:downstreaminterface: %s, upstreaminterface: %s, add = %d, MultiRouterCount =%d ",intIface,extIface,add,MultiRouterCount );
    if (add == false) {
        if (MultiRouterCount <= 0) {
            int ret = setDefaults();
            if (ret == 0) {
                MultiRouterCount=0;
            }
            return ret;
        }
    }

    if (!interfaceExists(extIface)||!interfaceExists(intIface) ) {
        ALOGE("Invalid interface specified");
        errno = ENODEV;
        return -1;
    }
    
//add table -> add route entry to talbe -> delete  route entry  -> delete table
if (add) {

   snprintf(cmd, sizeof(cmd), "rule %s iif %s table 71",
             (add ? "add" : "del"),intIface);
    if (runIpCmd(cmd)) {
		 ALOGE("run %s fail",cmd );
		 setDefaults();
        MultiRouterCount = 0;
        return -1;
      }
    }
	
    ALOGD("run cmd %s successfully ", cmd);
	
    /*
    snprintf(cmd, sizeof(cmd), "ip route %s default dev %s table 71", (add ? "add" : "del"),
            intIface);
    if (runIpCmd(cmd)) {
        ALOGE("run %s fail",cmd );
        return -1;
    } */
  
  if( ((0 == MultiRouterCount)&& add) ||((1 == MultiRouterCount)&& !add) )
   {
	if (add) {
         snprintf(cmd, sizeof(cmd), "route %s default dev %s table 71",  "add" ,
            extIface);
   
    } else {
              snprintf(cmd, sizeof(cmd), "route flush table 71" );
           }
	 if (runIpCmd(cmd)) {
        ALOGE("run %s fail",cmd );
        setDefaults();
        MultiRouterCount = 0;
        return -1;
      }
   ALOGD(" run cmd %s successfully ", cmd);
   }else
   {
     ALOGD(" skip add default route to 71 "); 
   }


   
   
    if (add) {
        MultiRouterCount++;
    } else {
        MultiRouterCount--;
    }
    return 0;
}

int MultiRouterController::enableMultiRouter(const char *intIface,const char *extIface) {
    return doMultiRouterCommands(intIface,extIface, true);
}

int MultiRouterController::disableMultiRouter(const char *intIface,const char *extIface) {
    return doMultiRouterCommands(intIface,extIface,  false);
}
