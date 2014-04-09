/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_TAG "BootAnimation"

#include <cutils/properties.h>

#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>

#include <utils/Log.h>
#include <utils/threads.h>

#if defined(HAVE_PTHREADS)
# include <pthread.h>
# include <sys/resource.h>
#endif

#include "BootAnimation.h"

using namespace android;

// ---------------------------------------------------------------------------

int main(int argc, char** argv)
{
	XLOGD("[BootAnimation %s %s %d]start %s %s",__FILE__,__FUNCTION__,__LINE__,__DATE__,__TIME__);
#if defined(HAVE_PTHREADS)
    setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_DISPLAY);
#endif

    char value[PROPERTY_VALUE_MAX];
    property_get("debug.sf.nobootanimation", value, "0");
    int noBootAnimation = atoi(value);
    if(noBootAnimation != 0) {
    ALOGI_IF(noBootAnimation,  "boot animation disabled");  //Jelly Bean changed
    }
    XLOGD("[BootAnimation %s %d]noBootAnimation=%d",__FUNCTION__,__LINE__,noBootAnimation); 
    if (!noBootAnimation) {

        sp<ProcessState> proc(ProcessState::self());
        ProcessState::self()->startThreadPool();

        // create the boot animation object
        bool setBoot = true;
		bool setRotated = false;
		bool sePaly = true;
		if(argc > 1){
           if(!strcmp(argv[1],"shut"))
		   	setBoot = false;
		}
		
		if(argc > 2){
			if(!strcmp(argv[2],"nomp3"))
		   	sePaly = false;
		}
		
		if(argc > 3){
			if(!strcmp(argv[3],"rotate"))
		   	setRotated = true;
		}
		XLOGD("[BootAnimation %s %d]setBoot=%d,sePaly=%d,setRotated=%d",__FUNCTION__,__LINE__,setBoot,sePaly,setRotated); 

		char volume[PROPERTY_VALUE_MAX];
        property_get("persist.sys.mute.state", volume, "-1");
	    int nVolume = -1;
		nVolume = atoi(volume);
		XLOGD("[BootAnimation %s %d]nVolume=%d",__FUNCTION__,__LINE__,nVolume); 
		if(nVolume == 0 || nVolume == 1 ){
			sePaly = false;
		}
        XLOGD("before new BootAnimation..."); 
    	XLOGD("[BootAnimation %s %d]before new BootAnimation...",__FUNCTION__,__LINE__);
        sp<BootAnimation> boot = new BootAnimation(setBoot,sePaly,setRotated);
        XLOGD("joinThreadPool..."); 
    	XLOGD("[BootAnimation %s %d]before joinThreadPool...",__FUNCTION__,__LINE__);
        IPCThreadState::self()->joinThreadPool();
        XLOGD("exit boot animation..."); 
    	XLOGD("[BootAnimation %s %d]after joinThreadPool...",__FUNCTION__,__LINE__);
    }
	XLOGD("[BootAnimation %s %s %d]end",__FILE__,__FUNCTION__,__LINE__);
    return 0;
}
