/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "RemoteDisplay.h"

#include "ANetworkSession.h"
#include "source/WifiDisplaySource.h"

#include <media/IRemoteDisplayClient.h>

namespace android {

RemoteDisplay::RemoteDisplay(
        const sp<IRemoteDisplayClient> &client, const char *iface)
    : mLooper(new ALooper),
      mNetSession(new ANetworkSession),
      mSource(new WifiDisplaySource(mNetSession, client)) {
    mLooper->setName("wfd_looper");
    mLooper->registerHandler(mSource);

    mNetSession->start();

#ifndef ANDROID_DEFAULT_CODE	
    //set playbacksession thread high priority.
    mLooper->start(
            false /* runOnCallingThread */,
            false /* canCallJava */,
            PRIORITY_AUDIO);
#else
    mLooper->start();
#endif

    mSource->start(iface);
}

#ifndef ANDROID_DEFAULT_CODE
RemoteDisplay::RemoteDisplay(
        const sp<IRemoteDisplayClient> &client, const char *iface, const uint32_t wfdFlags)
    : mLooper(new ALooper),
      mNetSession(new ANetworkSession),
      mSource(new WifiDisplaySource(mNetSession, client, wfdFlags)) {
    mLooper->setName("wfd_looper");
    mLooper->registerHandler(mSource);

    mNetSession->start();
    mLooper->start();

    mSource->start(iface);
}

///M: add for rtsp generic message{@
status_t RemoteDisplay::sendGenericMsg(int cmd) {
    mSource->sendGenericMsg(cmd);

    return OK;
}

status_t RemoteDisplay::setBitrateControl(int level) {
    mSource->setBitrateControl(level);

    return OK;
}
///@}


#endif

RemoteDisplay::~RemoteDisplay() {
}

status_t RemoteDisplay::pause() {
    return mSource->pause();
}

status_t RemoteDisplay::resume() {
    return mSource->resume();
}

status_t RemoteDisplay::dispose() {
    mSource->stop();

    mLooper->stop();
    mNetSession->stop();

    return OK;
}

}  // namespace android
