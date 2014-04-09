/*
**
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

// System headers required for setgroups, etc.
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_CMMBSP_SUPPORT

#include <sys/types.h>
#include <unistd.h>
#include <grp.h>
#include <linux/rtpm_prio.h>
#include <sys/prctl.h>
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <utils/Log.h>
#include <dlfcn.h>

typedef void (*my_Instantiate)(void);

using namespace android;

int main(int argc, char** argv)
{
		ALOGI("CMMB SP task init");
    sp<ProcessState> proc(ProcessState::self());
    sp<IServiceManager> sm = defaultServiceManager();
    ALOGI("ServiceManager: %p", sm.get());
 
    void *handle=NULL;
    const char* dlerr;
    handle = dlopen("/data/data/com.mediatek.cmmb.app/lib/libcmmbsp.so",RTLD_NOW);
    dlerr = dlerror();
    if (dlerr != NULL) ALOGE("dlopen() error: %s\n", dlerr);
    if(!handle){
        ALOGE("open /data/data/com.mediatek.cmmb.app/lib/libcmbmsp.so fail,then open /system/lib/so");
        handle = dlopen("libcmmbsp.so",RTLD_NOW);
        if(!handle){
            ALOGE("open /system/lib/libcmbmsp.so fail");
            return 0;
        }
    }
    ALOGI("open libcmmbsp.so success");
    if(handle){
        my_Instantiate F_instant;
        F_instant =(my_Instantiate)dlsym(handle,"BnCmmbSpinstant");
        dlerr = dlerror();
        if (dlerr != NULL){
            ALOGE( "dlsym() error: %s\n", dlerr);
            return 0;
        }
        if(F_instant)
            F_instant();
    }


    ProcessState::self()->startThreadPool();
    IPCThreadState::self()->joinThreadPool();

    if(handle){
        ALOGI("cmmbspso,dlcose");
        dlclose(handle);
    }
}

#endif
#endif
