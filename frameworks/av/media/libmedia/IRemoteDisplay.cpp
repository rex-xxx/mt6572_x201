/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include <stdint.h>
#include <sys/types.h>

#include <media/IRemoteDisplay.h>

namespace android {

enum {
    DISPOSE = IBinder::FIRST_CALL_TRANSACTION,
    PAUSE,
    RESUME,
#ifndef ANDROID_DEFAULT_CODE
    SENDGENERICMSG,
    SETBITRATECONTROL,
#endif
};

class BpRemoteDisplay: public BpInterface<IRemoteDisplay>
{
public:
    BpRemoteDisplay(const sp<IBinder>& impl)
        : BpInterface<IRemoteDisplay>(impl)
    {
    }

    virtual status_t pause() {
        Parcel data, reply;
        data.writeInterfaceToken(IRemoteDisplay::getInterfaceDescriptor());
        remote()->transact(PAUSE, data, &reply);
        return reply.readInt32();
    }

    virtual status_t resume() {
        Parcel data, reply;
        data.writeInterfaceToken(IRemoteDisplay::getInterfaceDescriptor());
        remote()->transact(RESUME, data, &reply);
        return reply.readInt32();
    }

    status_t dispose()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IRemoteDisplay::getInterfaceDescriptor());
        remote()->transact(DISPOSE, data, &reply);
        return reply.readInt32();
    }

    ///M: add for rtsp generic message@{
#ifndef ANDROID_DEFAULT_CODE
    status_t sendGenericMsg(int cmd)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IRemoteDisplay::getInterfaceDescriptor());
        data.writeInt32(cmd );

        remote()->transact(SENDGENERICMSG, data, &reply);
        return reply.readInt32();
    }
    status_t setBitrateControl(int level)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IRemoteDisplay::getInterfaceDescriptor());
        data.writeInt32(level);

        remote()->transact(SETBITRATECONTROL, data, &reply);
        return reply.readInt32();
    }
#endif
    ///@}
};

IMPLEMENT_META_INTERFACE(RemoteDisplay, "android.media.IRemoteDisplay");

// ----------------------------------------------------------------------

status_t BnRemoteDisplay::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch (code) {
        case DISPOSE: {
            CHECK_INTERFACE(IRemoteDisplay, data, reply);
            reply->writeInt32(dispose());
            return NO_ERROR;
        }

        case PAUSE:
        {
            CHECK_INTERFACE(IRemoteDisplay, data, reply);
            reply->writeInt32(pause());
            return OK;
        }

        case RESUME:
        {
            CHECK_INTERFACE(IRemoteDisplay, data, reply);
            reply->writeInt32(resume());
            return OK;
        }

        ///M:add for rtsp generic message{@
#ifndef ANDROID_DEFAULT_CODE        
        case SENDGENERICMSG:{
            CHECK_INTERFACE(IRemoteDisplay, data, reply);
            uint32_t cmd = data.readInt32();
            reply->writeInt32(sendGenericMsg(cmd));
            return NO_ERROR;
        }
        case SETBITRATECONTROL:{
            CHECK_INTERFACE(IRemoteDisplay, data, reply);
            uint32_t level = data.readInt32();
            reply->writeInt32(setBitrateControl(level));
            return NO_ERROR;
        }
#endif        
        ///@}
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

}; // namespace android
