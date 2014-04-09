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

#define LOG_TAG "mediaserver"
//#define LOG_NDEBUG 0
// System headers required for setgroups, etc.
#include <sys/types.h>
#include <unistd.h>
#include <grp.h>
#include <linux/rtpm_prio.h>
#include <sys/prctl.h>
#include <private/android_filesystem_config.h>

#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <utils/Log.h>

// from LOCAL_C_INCLUDES
#include "AudioFlinger.h"
#include "CameraService.h"
#include "MediaPlayerService.h"

#include "AudioPolicyService.h"

#ifndef ANDROID_DEFAULT_CODE
#include <memorydumper/MemoryDumper.h>
#endif

using namespace android;

int main(int argc, char** argv)
{
    signal(SIGPIPE, SIG_IGN);
    sp<ProcessState> proc(ProcessState::self());
    sp<IServiceManager> sm = defaultServiceManager();
    ALOGI("ServiceManager: %p", sm.get());
    
    AudioFlinger::instantiate();
    MediaPlayerService::instantiate();
#ifndef ANDROID_DEFAULT_CODE
    MemoryDumper::instantiate();
#endif
    CameraService::instantiate();

    AudioPolicyService::instantiate();

    if (AID_ROOT == getuid()) {
        ALOGI("[%s] re-adjust caps for its thread, and set uid to media", __func__);
        if (-1 == prctl(PR_SET_KEEPCAPS, 1, 0, 0, 0)) {
            ALOGW("mediaserver prctl for set caps failed: %s", strerror(errno));
        } else {
            __user_cap_header_struct hdr;
            __user_cap_data_struct data;

            setuid(AID_MEDIA);         // change user to media
    
            hdr.version = _LINUX_CAPABILITY_VERSION;    // set caps again
            hdr.pid = 0;
            data.effective = (1 << CAP_SYS_NICE);
            data.permitted = (1 << CAP_SYS_NICE);
            data.inheritable = 0xffffffff;
            if (-1 == capset(&hdr, &data)) {
                ALOGW("mediaserver cap re-setting failed, %s", strerror(errno));
            }
        }

    } else {
        ALOGI("[%s] re-adjust caps is not in root user", __func__);
    }    

    ProcessState::self()->startThreadPool();
    IPCThreadState::self()->joinThreadPool();
}
