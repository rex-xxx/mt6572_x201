/*
 * Copyright (C) 2011 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "ICameraRecordingProxy"
#include <camera/ICameraRecordingProxy.h>
#include <camera/ICameraRecordingProxyListener.h>
#include <binder/IMemory.h>
#include <binder/Parcel.h>
#include <stdint.h>
#include <utils/Log.h>

namespace android {

enum {
    START_RECORDING = IBinder::FIRST_CALL_TRANSACTION,
    STOP_RECORDING,
    RELEASE_RECORDING_FRAME,
#ifndef ANDROID_DEFAULT_CODE
    GET_PARAMETERS,
    SET_PARAMETERS,
#endif    
};


class BpCameraRecordingProxy: public BpInterface<ICameraRecordingProxy>
{
public:
    BpCameraRecordingProxy(const sp<IBinder>& impl)
        : BpInterface<ICameraRecordingProxy>(impl)
    {
    }

    status_t startRecording(const sp<ICameraRecordingProxyListener>& listener)
    {
        ALOGV("startRecording");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraRecordingProxy::getInterfaceDescriptor());
        data.writeStrongBinder(listener->asBinder());
        remote()->transact(START_RECORDING, data, &reply);
        return reply.readInt32();
    }

    void stopRecording()
    {
        ALOGV("stopRecording");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraRecordingProxy::getInterfaceDescriptor());
        remote()->transact(STOP_RECORDING, data, &reply);
    }

    void releaseRecordingFrame(const sp<IMemory>& mem)
    {
        ALOGV("releaseRecordingFrame");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraRecordingProxy::getInterfaceDescriptor());
        data.writeStrongBinder(mem->asBinder());
        remote()->transact(RELEASE_RECORDING_FRAME, data, &reply);
    }

#ifndef ANDROID_DEFAULT_CODE
	String8 getParameters()
	{
        ALOGV("getParameters");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraRecordingProxy::getInterfaceDescriptor());
        remote()->transact(GET_PARAMETERS, data, &reply);
        return reply.readString8();
	}
	
	status_t setParameters(const String8& params)
	{
        ALOGV("setParameters");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraRecordingProxy::getInterfaceDescriptor());
        data.writeString8(params);
        remote()->transact(SET_PARAMETERS, data, &reply);
        return reply.readInt32();
	}
#endif
};

IMPLEMENT_META_INTERFACE(CameraRecordingProxy, "android.hardware.ICameraRecordingProxy");

// ----------------------------------------------------------------------

status_t BnCameraRecordingProxy::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case START_RECORDING: {
            ALOGV("START_RECORDING");
            CHECK_INTERFACE(ICameraRecordingProxy, data, reply);
            sp<ICameraRecordingProxyListener> listener =
                interface_cast<ICameraRecordingProxyListener>(data.readStrongBinder());
            reply->writeInt32(startRecording(listener));
            return NO_ERROR;
        } break;
        case STOP_RECORDING: {
            ALOGV("STOP_RECORDING");
            CHECK_INTERFACE(ICameraRecordingProxy, data, reply);
            stopRecording();
            return NO_ERROR;
        } break;
        case RELEASE_RECORDING_FRAME: {
            ALOGV("RELEASE_RECORDING_FRAME");
            CHECK_INTERFACE(ICameraRecordingProxy, data, reply);
            sp<IMemory> mem = interface_cast<IMemory>(data.readStrongBinder());
            releaseRecordingFrame(mem);
            return NO_ERROR;
        } break;
#ifndef ANDROID_DEFAULT_CODE		
        case GET_PARAMETERS: {
            ALOGV("GET_PARAMETERS");
            CHECK_INTERFACE(ICamera, data, reply);
            reply->writeString8(getParameters());
            return NO_ERROR;
         } break;
        case SET_PARAMETERS: {
            ALOGV("SET_PARAMETERS");
            CHECK_INTERFACE(ICamera, data, reply);
            String8 params(data.readString8());
            reply->writeInt32(setParameters(params));
            return NO_ERROR;
         } break;
#endif
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android

