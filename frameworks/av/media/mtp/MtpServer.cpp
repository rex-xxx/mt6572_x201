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

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/stat.h>
#include <dirent.h>

#include <cutils/properties.h>

#define LOG_TAG "MtpServer"

#include "MtpDebug.h"
#include "MtpDatabase.h"
#include "MtpObjectInfo.h"
#include "MtpProperty.h"
#include "MtpServer.h"
#include "MtpStorage.h"
#include "MtpStringBuffer.h"

#include <linux/usb/f_mtp.h>

//Added for USB Develpment debug, more log for more debuging help
//change all LOGV to LOGI
#include "cutils/xlog.h"
#include <utils/Timers.h>
//Added for USB Develpment debug, more log for more debuging help
//Added Modification for ALPS00280586
#define LIKELY( exp )       (__builtin_expect( (exp) != 0, true  ))
#define UNLIKELY( exp )     (__builtin_expect( (exp) != 0, false ))
//Added Modification for ALPS00280586

namespace android {

static const MtpOperationCode kSupportedOperationCodes[] = {
    MTP_OPERATION_GET_DEVICE_INFO,
    MTP_OPERATION_OPEN_SESSION,
    MTP_OPERATION_CLOSE_SESSION,
    MTP_OPERATION_GET_STORAGE_IDS,
    MTP_OPERATION_GET_STORAGE_INFO,
    MTP_OPERATION_GET_NUM_OBJECTS,
    MTP_OPERATION_GET_OBJECT_HANDLES,
    MTP_OPERATION_GET_OBJECT_INFO,
    MTP_OPERATION_GET_OBJECT,
    MTP_OPERATION_GET_THUMB,
    MTP_OPERATION_DELETE_OBJECT,
    MTP_OPERATION_SEND_OBJECT_INFO,
    MTP_OPERATION_SEND_OBJECT,
//    MTP_OPERATION_INITIATE_CAPTURE,
//    MTP_OPERATION_FORMAT_STORE,
//    MTP_OPERATION_RESET_DEVICE,
//    MTP_OPERATION_SELF_TEST,
//    MTP_OPERATION_SET_OBJECT_PROTECTION,
//    MTP_OPERATION_POWER_DOWN,
    MTP_OPERATION_GET_DEVICE_PROP_DESC,
    MTP_OPERATION_GET_DEVICE_PROP_VALUE,
    MTP_OPERATION_SET_DEVICE_PROP_VALUE,
    MTP_OPERATION_RESET_DEVICE_PROP_VALUE,
//    MTP_OPERATION_TERMINATE_OPEN_CAPTURE,
//    MTP_OPERATION_MOVE_OBJECT,
//    MTP_OPERATION_COPY_OBJECT,
    MTP_OPERATION_GET_PARTIAL_OBJECT,
//    MTP_OPERATION_INITIATE_OPEN_CAPTURE,
    MTP_OPERATION_GET_OBJECT_PROPS_SUPPORTED,
    MTP_OPERATION_GET_OBJECT_PROP_DESC,
    MTP_OPERATION_GET_OBJECT_PROP_VALUE,
    MTP_OPERATION_SET_OBJECT_PROP_VALUE,
    MTP_OPERATION_GET_OBJECT_PROP_LIST,
//    MTP_OPERATION_SET_OBJECT_PROP_LIST,
//    MTP_OPERATION_GET_INTERDEPENDENT_PROP_DESC,
//    MTP_OPERATION_SEND_OBJECT_PROP_LIST,
    MTP_OPERATION_GET_OBJECT_REFERENCES,
    MTP_OPERATION_SET_OBJECT_REFERENCES,
//    MTP_OPERATION_SKIP,
    // Android extension for direct file IO
    MTP_OPERATION_GET_PARTIAL_OBJECT_64,
    MTP_OPERATION_SEND_PARTIAL_OBJECT,
    MTP_OPERATION_TRUNCATE_OBJECT,
    MTP_OPERATION_BEGIN_EDIT_OBJECT,
    MTP_OPERATION_END_EDIT_OBJECT,
};

//Added Modification for ALPS00255822, bug from WHQL test
static const MtpOperationCode kWHQLSupportedOperationCodes[] = {
    MTP_OPERATION_GET_DEVICE_INFO,
    MTP_OPERATION_OPEN_SESSION,
    MTP_OPERATION_CLOSE_SESSION,
    MTP_OPERATION_GET_STORAGE_IDS,
    MTP_OPERATION_GET_STORAGE_INFO,
    MTP_OPERATION_GET_NUM_OBJECTS,
    MTP_OPERATION_GET_OBJECT_HANDLES,
    MTP_OPERATION_GET_OBJECT_INFO,
    MTP_OPERATION_GET_OBJECT,
    MTP_OPERATION_GET_THUMB,
    MTP_OPERATION_DELETE_OBJECT,
    MTP_OPERATION_SEND_OBJECT_INFO,
    MTP_OPERATION_SEND_OBJECT,
//    MTP_OPERATION_INITIATE_CAPTURE,
//    MTP_OPERATION_FORMAT_STORE,
//    MTP_OPERATION_RESET_DEVICE,
//    MTP_OPERATION_SELF_TEST,
//    MTP_OPERATION_SET_OBJECT_PROTECTION,
//    MTP_OPERATION_POWER_DOWN,
    MTP_OPERATION_GET_DEVICE_PROP_DESC,
    MTP_OPERATION_GET_DEVICE_PROP_VALUE,
    MTP_OPERATION_SET_DEVICE_PROP_VALUE,
    MTP_OPERATION_RESET_DEVICE_PROP_VALUE,
//    MTP_OPERATION_TERMINATE_OPEN_CAPTURE,
//    MTP_OPERATION_MOVE_OBJECT,
//    MTP_OPERATION_COPY_OBJECT,
    MTP_OPERATION_GET_PARTIAL_OBJECT,
//    MTP_OPERATION_INITIATE_OPEN_CAPTURE,
    MTP_OPERATION_GET_OBJECT_PROPS_SUPPORTED,
    MTP_OPERATION_GET_OBJECT_PROP_DESC,
    MTP_OPERATION_GET_OBJECT_PROP_VALUE,
    MTP_OPERATION_SET_OBJECT_PROP_VALUE,
    //MTP_OPERATION_GET_OBJECT_PROP_LIST,   //marked for WHQL test!!
//    MTP_OPERATION_SET_OBJECT_PROP_LIST,
//    MTP_OPERATION_GET_INTERDEPENDENT_PROP_DESC,
//    MTP_OPERATION_SEND_OBJECT_PROP_LIST,
    MTP_OPERATION_GET_OBJECT_REFERENCES,
    //MTP_OPERATION_SET_OBJECT_REFERENCES,  //marked for WHQL test!!
//    MTP_OPERATION_SKIP,
    // Android extension for direct file IO
    MTP_OPERATION_GET_PARTIAL_OBJECT_64,
    MTP_OPERATION_SEND_PARTIAL_OBJECT,
    MTP_OPERATION_TRUNCATE_OBJECT,
    MTP_OPERATION_BEGIN_EDIT_OBJECT,
    MTP_OPERATION_END_EDIT_OBJECT,
};
//Added Modification for ALPS00255822, bug from WHQL test

static const MtpEventCode kSupportedEventCodes[] = {
    MTP_EVENT_OBJECT_ADDED,
    MTP_EVENT_OBJECT_REMOVED,
    MTP_EVENT_STORE_ADDED,
    MTP_EVENT_STORE_REMOVED,
    //ALPS00289309, update Object
    MTP_EVENT_OBJECT_INFO_CHANGED
    //ALPS00289309, update Object
    //Added for Storage Update
    , MTP_EVENT_STORAGE_INFO_CHANGED
    //Added for Storage Update

};

MtpServer::MtpServer(int fd, MtpDatabase* database, bool ptp,
                    int fileGroup, int filePerm, int directoryPerm)
    :   mFD(fd),
        mDatabase(database),
        mPtp(ptp),
        mFileGroup(fileGroup),
        mFilePermission(filePerm),
        mDirectoryPermission(directoryPerm),
        mSessionID(0),
        mSessionOpen(false),
        mSendObjectHandle(kInvalidObjectHandle),
        mSendObjectFormat(0),
        mSendObjectFileSize(0)
        //Added Modification for ALPS00276320
        ,mStorageEventKeep(false)
        ,mFileTransfer(false)
        //Added Modification for ALPS00276320
{
}

MtpServer::~MtpServer() {
}

void MtpServer::addStorage(MtpStorage* storage) {

    //Added Modification for ALPS00276320
    //Mutex::Autolock autoLock(mMutex);
    Mutex::Autolock autoLock(mMutex_storage);
    //Added Modification for ALPS00276320

    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                 "%s, storage->getStorageID() = 0x%x \n", __func__, storage->getStorageID());
    //Added for USB Develpment debug, more log for more debuging help

    mStorages.push(storage);
    sendStoreAdded(storage->getStorageID());
    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                 "%s, mStorages.size = %d \n", __func__, mStorages.size());
    //Added for USB Develpment debug, more log for more debuging help
}

void MtpServer::removeStorage(MtpStorage* storage) {
    //Added Modification for ALPS00276320
    //Mutex::Autolock autoLock(mMutex);
    Mutex::Autolock autoLock(mMutex_storage);
    //Added Modification for ALPS00276320

    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                 "%s, storage->getStorageID() = 0x%x \n", __func__, storage->getStorageID());
    //Added for USB Develpment debug, more log for more debuging help

    for (int i = 0; i < mStorages.size(); i++) {
        if (mStorages[i] == storage) {
            mStorages.removeAt(i);
            sendStoreRemoved(storage->getStorageID());
            break;
        }
    }
}

MtpStorage* MtpServer::getStorage(MtpStorageID id) {
    if (id == 0)
        return mStorages[0];
    for (int i = 0; i < mStorages.size(); i++) {
        MtpStorage* storage = mStorages[i];
        if (storage->getStorageID() == id)
            return storage;
    }
    return NULL;
}

bool MtpServer::hasStorage(MtpStorageID id) {
    if (id == 0 || id == 0xFFFFFFFF)
        return mStorages.size() > 0;
    return (getStorage(id) != NULL);
}

//Added Modification for ALPS00255822, bug from WHQL test
MtpResponseCode MtpServer::closeSession() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;   //0x2003
    mSessionID = 0;
    mSessionOpen = false;
    mDatabase->sessionEnded();
    return MTP_RESPONSE_OK; //0x2001
}
//Added Modification for ALPS00255822, bug from WHQL test

void MtpServer::run() {
    int fd = mFD;

    ALOGI("MtpServer::run fd: %d\n", fd);

    while (1) {
    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                 "MtpServer::run mFD: %d\n", mFD);
    //Added for USB Develpment debug, more log for more debuging help

        int ret = mRequest.read(fd);
        if (ret < 0) {
            ALOGI("request read returned %d, errno: %d", ret, errno);
            if (errno == ECANCELED) {
                // return to top of loop and wait for next command
                //Added for USB Develpment debug, more log for more debuging help
                sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                             "ECANCELED!! returned %d, errno: %d\n", ret, errno);
                //Added for USB Develpment debug, more log for more debuging help
                continue;
            }
            //Added Modification for ALPS00255822, bug from WHQL test
            else if (errno == ESTALE) {
                // return to top of loop and wait for next command
                sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                             "ESTALE!! returned %d, errno: %d. Need hold on more time to query!! ", ret, errno);
                //delay for 0.05 msec = 50 micro secs = 50000 nano secs
                //nsecs_t nsdelay = 50000000;   //0.05 senonds
                nsecs_t nsdelay = 500000000;   //0.5 senonds
                nsecs_t delay = ns2us(nsdelay);
                usleep(delay);
                continue;
            }
            //Added Modification for ALPS00255822, bug from WHQL test
            //ALPS00120037, add log for support debug
            else if(errno == EIO)
            {
                ALOGE("Try to wait for next command until success!! returned %d, errno: %d", ret, errno);
                //Added Modification for ALPS00255822, bug from WHQL test
                ALOGE("EIO!! returned %d, errno: %d", ret, errno);
                //for temporary WHQL test
                //continue;
                //for temporary WHQL test
                //Added Modification for ALPS00255822, bug from WHQL test
            }
            //ALPS00120037, add log for support debug
            break;
        }
        MtpOperationCode operation = mRequest.getOperationCode();
        MtpTransactionID transaction = mRequest.getTransactionID();

        //ALPS00120037, add log for support debug
        //ALOGD("operation: %s", MtpDebug::getOperationCodeName(operation));
        ALOGI("operation: %s, 0x%x", MtpDebug::getOperationCodeName(operation), operation);
         //ALPS00120037, add log for support debug
        mRequest.dump();

        // FIXME need to generalize this
        bool dataIn = (operation == MTP_OPERATION_SEND_OBJECT_INFO
                    || operation == MTP_OPERATION_SET_OBJECT_REFERENCES
                    || operation == MTP_OPERATION_SET_OBJECT_PROP_VALUE
                    || operation == MTP_OPERATION_SET_DEVICE_PROP_VALUE);
        if (dataIn) {
            int ret = mData.read(fd);
            if (ret < 0) {
                ALOGE("data read returned %d, errno: %d", ret, errno);
                if (errno == ECANCELED) {
                    // return to top of loop and wait for next command
                    continue;
                }
                break;
            }
            ALOGI("received data:");
            mData.dump();
        } else {
            mData.reset();
        }

        if (handleRequest()) {
            if (!dataIn && mData.hasData()) {
                mData.setOperationCode(operation);
                mData.setTransactionID(transaction);
                ALOGI("sending data:");
                mData.dump();
                ret = mData.write(fd);
                if (ret < 0) {
                    ALOGE("request write returned %d, errno: %d", ret, errno);
                    if (errno == ECANCELED) {
                        // return to top of loop and wait for next command
                        continue;
                    }
                    break;
                }
            }

            mResponse.setTransactionID(transaction);
            ALOGI("sending response %04X", mResponse.getResponseCode());
            ret = mResponse.write(fd);
            mResponse.dump();
            if (ret < 0) {
                ALOGE("request write returned %d, errno: %d", ret, errno);
                if (errno == ECANCELED) {
                    // return to top of loop and wait for next command
                    continue;
                    /*ALOGE("request write returned %d, errno: %d: Don't cancel the response return!! sending response %04X ", ret, errno, mResponse.getResponseCode());
                    ret = mResponse.write(fd);
                    mResponse.dump();*/
                }
                break;
            }
        } else {
            ALOGI("skipping response\n");
        }
    }

    // commit any open edits
    int count = mObjectEditList.size();
    for (int i = 0; i < count; i++) {
        ObjectEdit* edit = mObjectEditList[i];
        commitEdit(edit);
        delete edit;
    }
    mObjectEditList.clear();

    if (mSessionOpen)
        mDatabase->sessionEnded();
    close(fd);
    mFD = -1;
}

void MtpServer::sendObjectAdded(MtpObjectHandle handle) {
    ALOGI("sendObjectAdded %d\n", handle);
    sendEvent(MTP_EVENT_OBJECT_ADDED, handle);
}

void MtpServer::sendObjectRemoved(MtpObjectHandle handle) {
    ALOGI("sendObjectRemoved %d\n", handle);
    sendEvent(MTP_EVENT_OBJECT_REMOVED, handle);
}

void MtpServer::sendStoreAdded(MtpStorageID id) {
    ALOGI("sendStoreAdded %08X\n", id);
    sendEvent(MTP_EVENT_STORE_ADDED, id);
}

void MtpServer::sendStoreRemoved(MtpStorageID id) {
    ALOGI("sendStoreRemoved %08X\n", id);
    sendEvent(MTP_EVENT_STORE_REMOVED, id);
}

//ALPS00289309, update Object
void MtpServer::sendObjectInfoChanged(MtpObjectHandle handle) {
    ALOGI("%s handle = %d\n", __func__, handle);
    sendEvent(MTP_EVENT_OBJECT_INFO_CHANGED, handle);
}
//ALPS00289309, update Object
//Added for Storage Update
void MtpServer::sendStorageInfoChanged(MtpStorageID id) {
    ALOGI("%s id = %d\n", __func__, id);
    sendEvent(MTP_EVENT_STORAGE_INFO_CHANGED, id);
}
//Added for Storage Update
void MtpServer::sendEvent(MtpEventCode code, uint32_t param1) {
    if (mSessionOpen) {
    //Added for USB Develpment debug, more log for more debuging help
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "%s: MtpEventCode code = 0x%x, ", __func__, code);
    //Added for USB Develpment debug, more log for more debuging help
        mEvent.setEventCode(code);
        mEvent.setTransactionID(mRequest.getTransactionID());
        mEvent.setParameter(1, param1);
        int ret = mEvent.write(mFD);
        ALOGI("mEvent.write returned %d\n", ret);
        //Added Modification for ALPS00276320
        if(ret<0)
        {    
            sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                        "mEvent.write returned %d, errno: %d\n", ret, errno);

            if(code == MTP_EVENT_STORE_ADDED || code == MTP_EVENT_STORE_REMOVED)
            {
                //File under Transfer, get or send, backup the event and send out after file transfer
                if(mFileTransfer || mSendObjectHandle != kInvalidObjectHandle)
                {
                    mStorageEventKeep = true;
                    mBackupEvent = code;
                    mBackupStorageId = param1;
                }
            }
        }
        //Added Modification for ALPS00276320
    }
    //Added for USB Develpment debug, more log for more debuging help
    else
    {
        sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                    "%s: Without OpenSession to send event code = 0x%x, ", __func__, code);
    }
    //Added for USB Develpment debug, more log for more debuging help
}

void MtpServer::addEditObject(MtpObjectHandle handle, MtpString& path,
        uint64_t size, MtpObjectFormat format, int fd) {
    ObjectEdit*  edit = new ObjectEdit(handle, path, size, format, fd);
    mObjectEditList.add(edit);
}

MtpServer::ObjectEdit* MtpServer::getEditObject(MtpObjectHandle handle) {
    int count = mObjectEditList.size();
    for (int i = 0; i < count; i++) {
        ObjectEdit* edit = mObjectEditList[i];
        if (edit->mHandle == handle) return edit;
    }
    return NULL;
}

void MtpServer::removeEditObject(MtpObjectHandle handle) {
    int count = mObjectEditList.size();
    for (int i = 0; i < count; i++) {
        ObjectEdit* edit = mObjectEditList[i];
        if (edit->mHandle == handle) {
            delete edit;
            mObjectEditList.removeAt(i);
            return;
        }
    }
    ALOGE("ObjectEdit not found in removeEditObject");
}

void MtpServer::commitEdit(ObjectEdit* edit) {
    mDatabase->endSendObject((const char *)edit->mPath, edit->mHandle, edit->mFormat, true);
}


bool MtpServer::handleRequest() {
    Mutex::Autolock autoLock(mMutex);

    MtpOperationCode operation = mRequest.getOperationCode();
    MtpResponseCode response;

    mResponse.reset();

    if (mSendObjectHandle != kInvalidObjectHandle && operation != MTP_OPERATION_SEND_OBJECT) {
        // FIXME - need to delete mSendObjectHandle from the database
        ALOGE("expected SendObject after SendObjectInfo");
        //Modification for ALPS00331676
        ALOGE("expected SendObject after SendObjectInfo: Delete it, mSendObjectHandle = %d, !!", mSendObjectHandle);
        mDatabase->endSendObject(mSendObjectFilePath, mSendObjectHandle, mSendObjectFormat, 0);
        //Modification for ALPS00331676
        mSendObjectHandle = kInvalidObjectHandle;
    }

    switch (operation) {
        case MTP_OPERATION_GET_DEVICE_INFO:
            response = doGetDeviceInfo();
            break;
        case MTP_OPERATION_OPEN_SESSION:
            response = doOpenSession();
            break;
        case MTP_OPERATION_CLOSE_SESSION:
            response = doCloseSession();
            break;
        case MTP_OPERATION_GET_STORAGE_IDS:
            response = doGetStorageIDs();
            break;
         case MTP_OPERATION_GET_STORAGE_INFO:
            response = doGetStorageInfo();
            break;
        case MTP_OPERATION_GET_OBJECT_PROPS_SUPPORTED:
            response = doGetObjectPropsSupported();
            break;
        case MTP_OPERATION_GET_OBJECT_HANDLES:
            response = doGetObjectHandles();
            break;
        case MTP_OPERATION_GET_NUM_OBJECTS:
            response = doGetNumObjects();
            break;
        case MTP_OPERATION_GET_OBJECT_REFERENCES:
            response = doGetObjectReferences();
            break;
        case MTP_OPERATION_SET_OBJECT_REFERENCES:
            response = doSetObjectReferences();
            break;
        case MTP_OPERATION_GET_OBJECT_PROP_VALUE:
            response = doGetObjectPropValue();
            break;
        case MTP_OPERATION_SET_OBJECT_PROP_VALUE:
            response = doSetObjectPropValue();
            break;
        case MTP_OPERATION_GET_DEVICE_PROP_VALUE:
            response = doGetDevicePropValue();
            break;
        case MTP_OPERATION_SET_DEVICE_PROP_VALUE:
            response = doSetDevicePropValue();
            break;
        case MTP_OPERATION_RESET_DEVICE_PROP_VALUE:
            response = doResetDevicePropValue();
            break;
        case MTP_OPERATION_GET_OBJECT_PROP_LIST:
            ALOGI("got MTP_OPERATION_GET_OBJECT_PROP_LIST command %s, line %d \n", MtpDebug::getOperationCodeName(operation), __LINE__);
            response = doGetObjectPropList();
            break;
        case MTP_OPERATION_GET_OBJECT_INFO:
            response = doGetObjectInfo();
            break;
        case MTP_OPERATION_GET_OBJECT:
            response = doGetObject();
            break;
        case MTP_OPERATION_GET_THUMB:
            response = doGetThumb();
            break;
        case MTP_OPERATION_GET_PARTIAL_OBJECT:
        case MTP_OPERATION_GET_PARTIAL_OBJECT_64:
            response = doGetPartialObject(operation);
            break;
        case MTP_OPERATION_SEND_OBJECT_INFO:
            response = doSendObjectInfo();
            break;
        case MTP_OPERATION_SEND_OBJECT:
            response = doSendObject();
            break;
        case MTP_OPERATION_DELETE_OBJECT:
            response = doDeleteObject();
            break;
        case MTP_OPERATION_GET_OBJECT_PROP_DESC:
            response = doGetObjectPropDesc();
            break;
        case MTP_OPERATION_GET_DEVICE_PROP_DESC:
            response = doGetDevicePropDesc();
            break;
        case MTP_OPERATION_SEND_PARTIAL_OBJECT:
            response = doSendPartialObject();
            break;
        case MTP_OPERATION_TRUNCATE_OBJECT:
            response = doTruncateObject();
            break;
        case MTP_OPERATION_BEGIN_EDIT_OBJECT:
            response = doBeginEditObject();
            break;
        case MTP_OPERATION_END_EDIT_OBJECT:
            response = doEndEditObject();
            break;
        default:
            ALOGE("got unsupported command %s", MtpDebug::getOperationCodeName(operation));
            ALOGE("got unsupported command %s \n", MtpDebug::getOperationCodeName(operation));
            //Added Modification for ALPS00255822, bug from WHQL test
            sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                        "%s: got unsupported command 0x%x \n", __func__, operation);
            //just for whql!!
            if(operation < MTP_OPERATION_GET_DEVICE_INFO)
            {
                sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                            "%s: got unknow command 0x%x \n", __func__, operation);
                return false;
            }
            else if(operation > MTP_OPERATION_INITIATE_OPEN_CAPTURE && operation < MTP_OPERATION_GET_OBJECT_PROPS_SUPPORTED)
            {
                sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                            "%s: got unknow command 0x%x \n", __func__, operation);
                return false;
            }
            else if(operation > MTP_OPERATION_SKIP)
            {
                sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                            "%s: got unknow command 0x%x \n", __func__, operation);
                return false;
            }
            else
            //Added Modification for ALPS00255822, bug from WHQL test
            response = MTP_RESPONSE_OPERATION_NOT_SUPPORTED;
            break;
    }

    if (response == MTP_RESPONSE_TRANSACTION_CANCELLED)
        return false;
    mResponse.setResponseCode(response);
    return true;
}

MtpResponseCode MtpServer::doGetDeviceInfo() {
    MtpStringBuffer   string;
    char prop_value[PROPERTY_VALUE_MAX];

    MtpObjectFormatList* playbackFormats = mDatabase->getSupportedPlaybackFormats();
    MtpObjectFormatList* captureFormats = mDatabase->getSupportedCaptureFormats();
    MtpDevicePropertyList* deviceProperties = mDatabase->getSupportedDeviceProperties();

    // fill in device info
    mData.putUInt16(MTP_STANDARD_VERSION);
    if (mPtp) {
        mData.putUInt32(0);
    } else {
        // MTP Vendor Extension ID
        mData.putUInt32(6);
    }
    mData.putUInt16(MTP_STANDARD_VERSION);

    //ALPS00120037, add log for support debug
    ALOGI("doGetDeviceInfo mPtp = %d, \n", mPtp);
    //ALPS00120037, add log for support debug

    if (mPtp) {
        // no extensions
        string.set("");
    } else {
        // MTP extensions
        string.set("microsoft.com: 1.0; android.com: 1.0;");
    }
    mData.putString(string); // MTP Extensions
    mData.putUInt16(0); //Functional Mode
    //Added Modification for ALPS00255822, bug from WHQL test
    //different supported command for whql test condition because of the database limitation
    property_get("ro.sys.usb.mtp.whql.enable", prop_value, "0");
    ALOGI("doGetDeviceInfo property_get, ro.sys.usb.mtp.whql.enable, property_get = %s, \n",prop_value);
    if(!(strcmp(prop_value, "1")))
    {
        ALOGI("doGetDeviceInfo property_get, WHQL enable!!\n");
        mData.putAUInt16(kWHQLSupportedOperationCodes,
                sizeof(kWHQLSupportedOperationCodes) / sizeof(uint16_t)); // Operations Supported
    }
    else
    {
        ALOGI("doGetDeviceInfo property_get, WHQL disable!!\n");
    //Added Modification for ALPS00255822, bug from WHQL test    
    mData.putAUInt16(kSupportedOperationCodes,
            sizeof(kSupportedOperationCodes) / sizeof(uint16_t)); // Operations Supported
    //Added Modification for ALPS00255822, bug from WHQL test
    }
    //Added Modification for ALPS00255822, bug from WHQL test
    mData.putAUInt16(kSupportedEventCodes,
            sizeof(kSupportedEventCodes) / sizeof(uint16_t)); // Events Supported
    mData.putAUInt16(deviceProperties); // Device Properties Supported
    mData.putAUInt16(captureFormats); // Capture Formats
    mData.putAUInt16(playbackFormats);  // Playback Formats

    property_get("ro.product.manufacturer", prop_value, "unknown manufacturer");
    string.set(prop_value);
    //ALPS00120037, add log for support debug
    ALOGI("doGetDeviceInfo property_get, ro.product.manufacturer, property_get = %s, \n",prop_value);
    //ALPS00120037, add log for support debug
    mData.putString(string);   // Manufacturer

    property_get("ro.product.model", prop_value, "MTP Device");
    string.set(prop_value);
    //ALPS00120037, add log for support debug
    ALOGI("doGetDeviceInfo property_get, ro.product.model, property_get = %s, \n",prop_value);
    //ALPS00120037, add log for support debug
    mData.putString(string);   // Model
    string.set("1.0");
    mData.putString(string);   // Device Version

    property_get("ro.serialno", prop_value, "????????");
    string.set(prop_value);
    //ALPS00120037, add log for support debug
    ALOGI("doGetDeviceInfo property_get, ro.serialno, property_get = %s, \n",prop_value);
    //ALPS00120037, add log for support debug
    mData.putString(string);   // Serial Number

    delete playbackFormats;
    delete captureFormats;
    delete deviceProperties;

    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doOpenSession() {
    if (mSessionOpen) {
        mResponse.setParameter(1, mSessionID);
        return MTP_RESPONSE_SESSION_ALREADY_OPEN;
    }
    mSessionID = mRequest.getParameter(1);
    mSessionOpen = true;

    mDatabase->sessionStarted();

    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doCloseSession() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;
    mSessionID = 0;
    mSessionOpen = false;
    mDatabase->sessionEnded();
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doGetStorageIDs() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;

    int count = mStorages.size();
    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "doGetStorageIDs: mStorageCount = %d", count);
    //Added for USB Develpment debug, more log for more debuging help
    mData.putUInt32(count);
    for (int i = 0; i < count; i++)
    //Added for USB Develpment debug, more log for more debuging help
    {
    //Added for USB Develpment debug, more log for more debuging help
        mData.putUInt32(mStorages[i]->getStorageID());

        sxlog_printf(ANDROID_LOG_VERBOSE, "MtpServer",
                    "doGetStorageIDs: StorageID[%d] = 0x%08x", i, mStorages[i]->getStorageID());
    //Added for USB Develpment debug, more log for more debuging help
    }
    //Added for USB Develpment debug, more log for more debuging help

    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doGetStorageInfo() {

    //Added Modification for ALPS00335910
    Mutex::Autolock autoLock(mMutex_storage);
    //Added Modification for ALPS00335910

    MtpStringBuffer   string;

    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;
    MtpStorageID id = mRequest.getParameter(1);
    MtpStorage* storage = getStorage(id);

    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "doGetStorageInfo: storage id = 0x%x", id);
    //Added for USB Develpment debug, more log for more debuging help

    if (!storage)
        return MTP_RESPONSE_INVALID_STORAGE_ID;

    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "doGetStorageInfo: storage type = 0x%x", storage->getType());
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "doGetStorageInfo: storage file system type = 0x%x", storage->getFileSystemType());
    //Added for USB Develpment debug, more log for more debuging help

    mData.putUInt16(storage->getType());
    mData.putUInt16(storage->getFileSystemType());
    mData.putUInt16(storage->getAccessCapability());
    mData.putUInt64(storage->getMaxCapacity());
    mData.putUInt64(storage->getFreeSpace());
    mData.putUInt32(1024*1024*1024); // Free Space in Objects
    string.set(storage->getDescription());
    mData.putString(string);
    mData.putEmptyString();   // Volume Identifier

    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doGetObjectPropsSupported() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;
    MtpObjectFormat format = mRequest.getParameter(1);
    MtpObjectPropertyList* properties = mDatabase->getSupportedObjectProperties(format);
    mData.putAUInt16(properties);
    delete properties;
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doGetObjectHandles() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;
    MtpStorageID storageID = mRequest.getParameter(1);      // 0xFFFFFFFF for all storage
    MtpObjectFormat format = mRequest.getParameter(2);      // 0 for all formats
    MtpObjectHandle parent = mRequest.getParameter(3);      // 0xFFFFFFFF for objects with no parent
                                                            // 0x00000000 for all objects
    //Added Modification for ALPS00255822, bug from WHQL test
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "%s: storageID = 0x%x, format = 0x%x, parent = 0x%x", __func__, storageID, format, parent);
    //Added Modification for ALPS00255822, bug from WHQL test

    if (!hasStorage(storageID))
        return MTP_RESPONSE_INVALID_STORAGE_ID;

    MtpObjectHandleList* handles = mDatabase->getObjectList(storageID, format, parent);
    //Modeification for ALPS00326143
    if(handles==NULL)
    {
        sxlog_printf(ANDROID_LOG_INFO, "MtpServer",
                    "%s, line %d: database under initialization, handles = %p \n", __func__, __LINE__, handles);
        return MTP_RESPONSE_DEVICE_BUSY;
    }
    else if((*handles)[0] == 0)
    {
        sxlog_printf(ANDROID_LOG_INFO, "MtpServer",
                    "%s, line %d: no objetc, handles[0] = %d \n", __func__, __LINE__, (*handles)[0]);

        delete handles;
        handles = NULL;
        
    }
    //Modeification for ALPS00326143
    mData.putAUInt32(handles);
    delete handles;
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doGetNumObjects() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;
    MtpStorageID storageID = mRequest.getParameter(1);      // 0xFFFFFFFF for all storage
    MtpObjectFormat format = mRequest.getParameter(2);      // 0 for all formats
    MtpObjectHandle parent = mRequest.getParameter(3);      // 0xFFFFFFFF for objects with no parent
                                                            // 0x00000000 for all objects
    //Added Modification for ALPS00255822, bug from WHQL test
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "%s: storageID = 0x%x, format = 0x%x, parent = 0x%x \n", __func__, storageID, format, parent);
    //Added Modification for ALPS00255822, bug from WHQL test
    if (!hasStorage(storageID))
        return MTP_RESPONSE_INVALID_STORAGE_ID;

    int count = mDatabase->getNumObjects(storageID, format, parent);
    if (count >= 0) {
        mResponse.setParameter(1, count);
        return MTP_RESPONSE_OK;
    } else {
        mResponse.setParameter(1, 0);
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    }
}

MtpResponseCode MtpServer::doGetObjectReferences() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);

    //Added Modification for ALPS00255822, bug from WHQL test
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "%s: handle = 0x%x \n", __func__, handle);
    //Added Modification for ALPS00255822, bug from WHQL test

    // FIXME - check for invalid object handle
    MtpObjectHandleList* handles = mDatabase->getObjectReferences(handle);
    if (handles) {
        mData.putAUInt32(handles);
        delete handles;
    } else {
        mData.putEmptyArray();
    }
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doSetObjectReferences() {
    if (!mSessionOpen)
        return MTP_RESPONSE_SESSION_NOT_OPEN;
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpStorageID handle = mRequest.getParameter(1);

    MtpObjectHandleList* references = mData.getAUInt32();

    //Added Modification for ALPS00255822, bug from WHQL test
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "%s: handle = 0x%x \n", __func__, handle);
    //Added Modification for ALPS00255822, bug from WHQL test

    MtpResponseCode result = mDatabase->setObjectReferences(handle, references);
    delete references;
    return result;
}

MtpResponseCode MtpServer::doGetObjectPropValue() {
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);
    MtpObjectProperty property = mRequest.getParameter(2);
    ALOGI("GetObjectPropValue %d %s\n", handle,
            MtpDebug::getObjectPropCodeName(property));

    //Added Modification for ALPS00255822, bug from WHQL test
    //to confirm that if the file handle is valid!!
    MtpObjectInfo info(handle);
    MtpResponseCode result = mDatabase->getObjectInfo(handle, info);

    if(result == MTP_RESPONSE_INVALID_OBJECT_HANDLE)
    {
        ALOGI("%s: handle = 0x%x, get the path result\n",  __func__, handle, result);
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    }
    //Added Modification for ALPS00255822, bug from WHQL test

    return mDatabase->getObjectPropertyValue(handle, property, mData);
}

MtpResponseCode MtpServer::doSetObjectPropValue() {
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);
    MtpObjectProperty property = mRequest.getParameter(2);
    ALOGI("SetObjectPropValue %d %s\n", handle,
            MtpDebug::getObjectPropCodeName(property));

    return mDatabase->setObjectPropertyValue(handle, property, mData);
}

MtpResponseCode MtpServer::doGetDevicePropValue() {
    MtpDeviceProperty property = mRequest.getParameter(1);
    ALOGI("GetDevicePropValue %s\n",
            MtpDebug::getDevicePropCodeName(property));

    return mDatabase->getDevicePropertyValue(property, mData);
}

MtpResponseCode MtpServer::doSetDevicePropValue() {
    MtpDeviceProperty property = mRequest.getParameter(1);
    ALOGI("SetDevicePropValue %s\n",
            MtpDebug::getDevicePropCodeName(property));

    return mDatabase->setDevicePropertyValue(property, mData);
}

MtpResponseCode MtpServer::doResetDevicePropValue() {
    MtpDeviceProperty property = mRequest.getParameter(1);
    ALOGI("ResetDevicePropValue %s\n",
            MtpDebug::getDevicePropCodeName(property));

    return mDatabase->resetDeviceProperty(property);
}

MtpResponseCode MtpServer::doGetObjectPropList() {
    if (!hasStorage())
    //Added Modification for ALPS00255822, bug from WHQL test
    {    
        ALOGE("%s: No storage or bad storage!! \n", __func__);
    //Added Modification for ALPS00255822, bug from WHQL test
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    //Added Modification for ALPS00255822, bug from WHQL test
    }
    //Added Modification for ALPS00255822, bug from WHQL test

    MtpObjectHandle handle = mRequest.getParameter(1);
    // use uint32_t so we can support 0xFFFFFFFF
    uint32_t format = mRequest.getParameter(2);
    uint32_t property = mRequest.getParameter(3);
    int groupCode = mRequest.getParameter(4);
    int depth = mRequest.getParameter(5);
    //ALPS00120037, add log for support debug
   /*ALOGD("GetObjectPropList %d format: %s property: %s group: %d depth: %d\n",
            handle, MtpDebug::getFormatCodeName(format),
            MtpDebug::getObjectPropCodeName(property), groupCode, depth);*/
   ALOGI("GetObjectPropList handle: 0x%x, format: %s, property: %s, group: %d, depth: %d\n",
            handle, MtpDebug::getFormatCodeName(format),
            MtpDebug::getObjectPropCodeName(property), groupCode, depth);
    //ALPS00120037, add log for support debug

    return mDatabase->getObjectPropertyList(handle, format, property, groupCode, depth, mData);
}

MtpResponseCode MtpServer::doGetObjectInfo() {
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);
    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "doGetObjectInfo handle = 0x%x \n", handle);
    //Added for USB Develpment debug, more log for more debuging help

    MtpObjectInfo info(handle);
    MtpResponseCode result = mDatabase->getObjectInfo(handle, info);
    if (result == MTP_RESPONSE_OK) {
        char    date[20];

        mData.putUInt32(info.mStorageID);
        mData.putUInt16(info.mFormat);
        mData.putUInt16(info.mProtectionStatus);

        // if object is being edited the database size may be out of date
        uint32_t size = info.mCompressedSize;
        ObjectEdit* edit = getEditObject(handle);
        if (edit)
            size = (edit->mSize > 0xFFFFFFFFLL ? 0xFFFFFFFF : (uint32_t)edit->mSize);
        mData.putUInt32(size);

        //Added for USB Develpment debug, more log for more debuging help
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo edit = %p, size=%d \n", edit, size);
        if(info.mName)
        {
            sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                        "doGetObjectInfo info.mName = %s \n", info.mName);
        }
/*        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mStorageID = 0x%08x \n", info.mStorageID);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mFormat = 0x%x \n", info.mFormat);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mCompressedSize = %d \n", info.mCompressedSize);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mThumbFormat = 0x%x \n", info.mThumbFormat);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mThumbCompressedSize = %d \n", info.mThumbCompressedSize);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mThumbPixWidth = %d \n", info.mThumbPixWidth);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mThumbPixHeight = %d \n", info.mThumbPixHeight);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mImagePixWidth = %d \n", info.mImagePixWidth);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mImagePixHeight = %d \n", info.mImagePixHeight);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mAssociationType = %d \n", info.mAssociationType);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mAssociationDesc = %d \n", info.mAssociationDesc);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mSequenceNumber = %d \n", info.mSequenceNumber);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                    "doGetObjectInfo info.mDateModified = %ld \n", info.mDateModified);
*/
        //Added for USB Develpment debug, more log for more debuging help

        mData.putUInt16(info.mThumbFormat);
        mData.putUInt32(info.mThumbCompressedSize);
        mData.putUInt32(info.mThumbPixWidth);
        mData.putUInt32(info.mThumbPixHeight);
        mData.putUInt32(info.mImagePixWidth);
        mData.putUInt32(info.mImagePixHeight);
        mData.putUInt32(info.mImagePixDepth);
        mData.putUInt32(info.mParent);
        mData.putUInt16(info.mAssociationType);
        mData.putUInt32(info.mAssociationDesc);
        mData.putUInt32(info.mSequenceNumber);
        mData.putString(info.mName);
        mData.putEmptyString();    // date created
        formatDateTime(info.mDateModified, date, sizeof(date));
        mData.putString(date);   // date modified
        mData.putEmptyString();   // keywords
    }
    return result;
}

MtpResponseCode MtpServer::doGetObject() {
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);
    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "doGetObject handle = 0x%x \n", handle);
    //Added for USB Develpment debug, more log for more debuging help

    MtpString pathBuf;
    int64_t fileLength;
    MtpObjectFormat format;
    int result = mDatabase->getObjectFilePath(handle, pathBuf, fileLength, format);
    if (result != MTP_RESPONSE_OK)
        return result;

    const char* filePath = (const char *)pathBuf;
    mtp_file_range  mfr;
    mfr.fd = open(filePath, O_RDONLY);
    if (mfr.fd < 0) {
        return MTP_RESPONSE_GENERAL_ERROR;
    }
    mfr.offset = 0;
    mfr.length = fileLength;
    mfr.command = mRequest.getOperationCode();
    mfr.transaction_id = mRequest.getTransactionID();

    //Added for USB Develpment debug, more log for more debuging help
    ALOGI("doGetObject filePath = %s \n", filePath);
    ALOGI("%s: mfr.length = %d \n", __func__, mfr.length);
    ALOGI("%s: mfr.transaction_id = %d \n", __func__, mfr.transaction_id);
    //Added for USB Develpment debug, more log for more debuging help

    //Added Modification for ALPS00264207, ALPS00248646
    /*sxlog_printf(ANDROID_LOG_VERBOSE, "MtpServer",
                 "%s: call getObjectBeginIndication!!\n", __func__);
    mDatabase->getObjectBeginIndication(filePath);*/
    //Added Modification for ALPS00264207, ALPS00248646
    //Added Modification for ALPS00276320
    mFileTransfer = true;
    //Added Modification for ALPS00276320

    // then transfer the file
    int ret = ioctl(mFD, MTP_SEND_FILE_WITH_HEADER, (unsigned long)&mfr);
    ALOGI("MTP_SEND_FILE_WITH_HEADER returned %d\n", ret);
    close(mfr.fd);
    //Added Modification for ALPS00264207, ALPS00248646
    /*sxlog_printf(ANDROID_LOG_VERBOSE, "MtpServer",
                 "%s: call getObjectEndIndication!!\n", __func__);
    mDatabase->getObjectEndIndication(filePath);*/

    //Added Modification for ALPS00276320
    mFileTransfer = false;
    //process the add/remove storage event;
    //Added Modification for ALPS00280586
    //if(mStorageEventKeep)
    if (UNLIKELY(mStorageEventKeep))
    //Added Modification for ALPS00280586
    {    
        sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                    "%s, line %d: after MTP_RECEIVE_FILE, process the StorageEvent, mStorageEventKeep = %d\n", __func__, __LINE__, mStorageEventKeep);

        sendEvent(mBackupEvent, mBackupStorageId);
        mStorageEventKeep = mBackupEvent = mBackupStorageId = 0; 
    }
    //Added Modification for ALPS00276320

    //Added Modification for ALPS00264207, ALPS00248646
    if (ret < 0) {
        //Added for USB Develpment debug, more log for more debuging help
        sxlog_printf(ANDROID_LOG_VERBOSE, "MtpServer",
                    "%s: read file error: errno = 0x%x \n", __func__, errno);
        //Added for USB Develpment debug, more log for more debuging help

        if (errno == ECANCELED)
            return MTP_RESPONSE_TRANSACTION_CANCELLED;
        else
            return MTP_RESPONSE_GENERAL_ERROR;
    }
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doGetThumb() {
    MtpObjectHandle handle = mRequest.getParameter(1);
    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "doGetThumb Handle 0x%x\n", handle);
    //Added for USB Develpment debug, more log for more debuging help
    size_t thumbSize;
    void* thumb = mDatabase->getThumbnail(handle, thumbSize);
    if (thumb) {
        // send data
        mData.setOperationCode(mRequest.getOperationCode());
        mData.setTransactionID(mRequest.getTransactionID());
        mData.writeData(mFD, thumb, thumbSize);
        free(thumb);
        return MTP_RESPONSE_OK;
    } else {
        //Added Modification for ALPS00255822, bug from WHQL test
        //return MTP_RESPONSE_GENERAL_ERROR;
        return MTP_RESPONSE_NO_THUMBNAIL_PRESENT;
        //Added Modification for ALPS00255822, bug from WHQL test
    }
}

MtpResponseCode MtpServer::doGetPartialObject(MtpOperationCode operation) {
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);
    uint64_t offset;
    uint32_t length;
    offset = mRequest.getParameter(2);
    if (operation == MTP_OPERATION_GET_PARTIAL_OBJECT_64) {
        // android extension with 64 bit offset
        uint64_t offset2 = mRequest.getParameter(3);
        offset = offset | (offset2 << 32);
        length = mRequest.getParameter(4);
    } else {
        // standard GetPartialObject
        length = mRequest.getParameter(3);
    }
    MtpString pathBuf;
    int64_t fileLength;
    MtpObjectFormat format;
    int result = mDatabase->getObjectFilePath(handle, pathBuf, fileLength, format);
    if (result != MTP_RESPONSE_OK)
        return result;
    if (offset + length > fileLength)
        length = fileLength - offset;

    const char* filePath = (const char *)pathBuf;
    mtp_file_range  mfr;
    mfr.fd = open(filePath, O_RDONLY);
    if (mfr.fd < 0) {
        return MTP_RESPONSE_GENERAL_ERROR;
    }
    mfr.offset = offset;
    mfr.length = length;
    mfr.command = mRequest.getOperationCode();
    mfr.transaction_id = mRequest.getTransactionID();
    mResponse.setParameter(1, length);

    //Added for USB Develpment debug, more log for more debuging help
    ALOGI("doGetPartialObject filePath = %s \n", filePath);
    //Added for USB Develpment debug, more log for more debuging help

    // transfer the file
    int ret = ioctl(mFD, MTP_SEND_FILE_WITH_HEADER, (unsigned long)&mfr);
    ALOGI("MTP_SEND_FILE_WITH_HEADER returned %d\n", ret);
    close(mfr.fd);
    if (ret < 0) {
        if (errno == ECANCELED)
            return MTP_RESPONSE_TRANSACTION_CANCELLED;
        else
            return MTP_RESPONSE_GENERAL_ERROR;
    }
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doSendObjectInfo() {
    MtpString path;
    MtpStorageID storageID = mRequest.getParameter(1);
    MtpStorage* storage = getStorage(storageID);
    MtpObjectHandle parent = mRequest.getParameter(2);
    if (!storage)
        return MTP_RESPONSE_INVALID_STORAGE_ID;

    //Added for USB Develpment debug, more log for more debuging help
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpServer",
                "%s: storageID: %08X, parent: 0x%x", __func__, storageID, parent );
    //Added for USB Develpment debug, more log for more debuging help
    // special case the root
    if (parent == MTP_PARENT_ROOT) {
        path = storage->getPath();
        parent = 0;
    } else {
        int64_t length;
        MtpObjectFormat format;
        int result = mDatabase->getObjectFilePath(parent, path, length, format);
        if (result != MTP_RESPONSE_OK)
            return result;
        if (format != MTP_FORMAT_ASSOCIATION)
            return MTP_RESPONSE_INVALID_PARENT_OBJECT;
    }

    // read only the fields we need
    mData.getUInt32();  // storage ID
    MtpObjectFormat format = mData.getUInt16();
    mData.getUInt16();  // protection status
    mSendObjectFileSize = mData.getUInt32();
    mData.getUInt16();  // thumb format
    mData.getUInt32();  // thumb compressed size
    mData.getUInt32();  // thumb pix width
    mData.getUInt32();  // thumb pix height
    mData.getUInt32();  // image pix width
    mData.getUInt32();  // image pix height
    mData.getUInt32();  // image bit depth
    mData.getUInt32();  // parent
    uint16_t associationType = mData.getUInt16();
    uint32_t associationDesc = mData.getUInt32();   // association desc
    mData.getUInt32();  // sequence number
    MtpStringBuffer name, created, modified;
    mData.getString(name);    // file name
    mData.getString(created);      // date created
    mData.getString(modified);     // date modified
    // keywords follow

    ALOGI("name: %s format: %04X\n", (const char *)name, format);
    time_t modifiedTime;
    if (!parseDateTime(modified, modifiedTime))
        modifiedTime = 0;

    if (path[path.size() - 1] != '/')
        path += "/";
    path += (const char *)name;

    // check space first
    if (mSendObjectFileSize > storage->getFreeSpace())
        return MTP_RESPONSE_STORAGE_FULL;
    uint64_t maxFileSize = storage->getMaxFileSize();
    // check storage max file size
    if (maxFileSize != 0) {
        // if mSendObjectFileSize is 0xFFFFFFFF, then all we know is the file size
        // is >= 0xFFFFFFFF
        if (mSendObjectFileSize > maxFileSize || mSendObjectFileSize == 0xFFFFFFFF)
            return MTP_RESPONSE_OBJECT_TOO_LARGE;
    }

    ALOGI("path: %s parent: %d storageID: %08X", (const char*)path, parent, storageID);
    MtpObjectHandle handle = mDatabase->beginSendObject((const char*)path,
            format, parent, storageID, mSendObjectFileSize, modifiedTime);
    if (handle == kInvalidObjectHandle) {
        return MTP_RESPONSE_GENERAL_ERROR;
    }
//Added Modification for ALPS00255822, bug from WHQL test
#if 0
    else if(handle == MTP_RESPONSE_OBJECT_WRITE_PROTECTED)
    {
        //return MTP_RESPONSE_ACCESS_DENIED;
        ALOGE("path: %s parent: %d storageID: %08X: File exist and it's not right here!! delete the file and return failed!!\n", (const char*)path, parent, storageID);
        return MTP_RESPONSE_DEVICE_BUSY;
        //return MTP_RESPONSE_TRANSACTION_CANCELLED;
        //return MTP_RESPONSE_INCOMPLETE_TRANSFER;
        //return MTP_RESPONSE_OBJECT_WRITE_PROTECTED;
        //Ainge Test
    }
#endif
//Added Modification for ALPS00255822, bug from WHQL test

  if (format == MTP_FORMAT_ASSOCIATION) {
        mode_t mask = umask(0);
        int ret = mkdir((const char *)path, mDirectoryPermission);
        umask(mask);
        if (ret && ret != -EEXIST)
            return MTP_RESPONSE_GENERAL_ERROR;
        chown((const char *)path, getuid(), mFileGroup);

        // SendObject does not get sent for directories, so call endSendObject here instead
        mDatabase->endSendObject(path, handle, MTP_FORMAT_ASSOCIATION, MTP_RESPONSE_OK);
    } else {
        mSendObjectFilePath = path;
        // save the handle for the SendObject call, which should follow
        mSendObjectHandle = handle;
        mSendObjectFormat = format;
    }

    mResponse.setParameter(1, storageID);
    mResponse.setParameter(2, parent);
    mResponse.setParameter(3, handle);

    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doSendObject() {
    if (!hasStorage())
        return MTP_RESPONSE_GENERAL_ERROR;
    MtpResponseCode result = MTP_RESPONSE_OK;
    mode_t mask;
    int ret, initialData;

    if (mSendObjectHandle == kInvalidObjectHandle) {
        ALOGE("Expected SendObjectInfo before SendObject");
        result = MTP_RESPONSE_NO_VALID_OBJECT_INFO;
        goto done;
    }

    // read the header, and possibly some data
    ret = mData.read(mFD);
    if (ret < MTP_CONTAINER_HEADER_SIZE) {
        result = MTP_RESPONSE_GENERAL_ERROR;
        //Added Modification for ALPS00331671
        ALOGE("ret %d < MTP_CONTAINER_HEADER_SIZE, errno= %d", ret, errno);
        if (errno == ECANCELED)
        {    
            ALOGE("%s, line %d: read data error. return MTP_RESPONSE_TRANSACTION_CANCELLED!!\n", __func__, __LINE__);
            result = MTP_RESPONSE_TRANSACTION_CANCELLED;
        }
        //Added Modification for ALPS00331671
        goto done;
    }
    initialData = ret - MTP_CONTAINER_HEADER_SIZE;

    mtp_file_range  mfr;
    mfr.fd = open(mSendObjectFilePath, O_RDWR | O_CREAT | O_TRUNC, S_IRUSR | S_IWUSR);
    if (mfr.fd < 0) {
        //Added Modification for ALPS00331671
        ALOGE("mfr.fd %d < 0", mfr.fd);
        //Added Modification for ALPS00331671
        result = MTP_RESPONSE_GENERAL_ERROR;
        goto done;
    }
    fchown(mfr.fd, getuid(), mFileGroup);
    // set permissions
    mask = umask(0);
    fchmod(mfr.fd, mFilePermission);
    umask(mask);

    if (initialData > 0)
        ret = write(mfr.fd, mData.getData(), initialData);

    if (mSendObjectFileSize - initialData > 0) {
        mfr.offset = initialData;
        if (mSendObjectFileSize == 0xFFFFFFFF) {
            // tell driver to read until it receives a short packet
            mfr.length = 0xFFFFFFFF;
        } else {
            mfr.length = mSendObjectFileSize - initialData;
        }

        ALOGI("receiving %s\n", (const char *)mSendObjectFilePath);
        // transfer the file
        ret = ioctl(mFD, MTP_RECEIVE_FILE, (unsigned long)&mfr);
        ALOGI("MTP_RECEIVE_FILE returned %d\n", ret);
    }
    close(mfr.fd);

    //Added Modification for ALPS00276320
    //process the add/remove storage event;
    if(mStorageEventKeep)
    {    
        sxlog_printf(ANDROID_LOG_ERROR, "MtpServer",
                    "%s, line %d: after MTP_RECEIVE_FILE, process the StorageEvent, mStorageEventKeep = %d\n", __func__, __LINE__, mStorageEventKeep);

        sendEvent(mBackupEvent, mBackupStorageId);
        mStorageEventKeep = mBackupEvent = mBackupStorageId = 0; 
    }
    //Added Modification for ALPS00276320

    if (ret < 0) {
        unlink(mSendObjectFilePath);
//Added Modification for ALPS00255822, bug from WHQL test
        if (errno == ECANCELED)
        {    
            ALOGE("%s: MTP_RECEIVE_FILE return MTP_RESPONSE_TRANSACTION_CANCELLED = 0x%x\n", __func__, MTP_RESPONSE_TRANSACTION_CANCELLED);
            //mSendObjectHandle = kInvalidObjectHandle;
            //mSendObjectFormat = 0;
            //return MTP_RESPONSE_TRANSACTION_CANCELLED;
            result = MTP_RESPONSE_TRANSACTION_CANCELLED;
        }
        else if(abs(ret) == MTP_RESPONSE_INCOMPLETE_TRANSFER)// || ret == -MTP_RESPONSE_INCOMPLETE_TRANSFER)
        {    
            ALOGE("%s: MTP_RECEIVE_FILE return MTP_RESPONSE_INCOMPLETE_TRANSFER = 0x%x\n", __func__, MTP_RESPONSE_INCOMPLETE_TRANSFER);
            result = MTP_RESPONSE_TRANSACTION_CANCELLED;
        }
        else if(ret == -8199)
        {    
            ALOGE("%s: MTP_RECEIVE_FILE return MTP_RESPONSE_INCOMPLETE_TRANSFER 2 = 0x%x\n", __func__, MTP_RESPONSE_INCOMPLETE_TRANSFER);
            result = MTP_RESPONSE_TRANSACTION_CANCELLED;
        }
        else
        {    
            ALOGE("%s: MTP_RECEIVE_FILE return MTP_RESPONSE_GENERAL_ERROR = 0x%x\n", __func__, MTP_RESPONSE_GENERAL_ERROR);
            result = MTP_RESPONSE_GENERAL_ERROR;
    }
        //Added Modification for ALPS00255822, bug from WHQL test
        //marked original information for WHQL 
        /*
        if (errno == ECANCELED)
            result = MTP_RESPONSE_TRANSACTION_CANCELLED;
        else
            result = MTP_RESPONSE_GENERAL_ERROR;
        */
        //Added Modification for ALPS00255822, bug from WHQL test
    }

done:
    // reset so we don't attempt to send the data back
    mData.reset();

    mDatabase->endSendObject(mSendObjectFilePath, mSendObjectHandle, mSendObjectFormat,
            result == MTP_RESPONSE_OK);
    mSendObjectHandle = kInvalidObjectHandle;
    mSendObjectFormat = 0;
    return result;
}

static void deleteRecursive(const char* path) {
    char pathbuf[PATH_MAX];
    int pathLength = strlen(path);
    if (pathLength >= sizeof(pathbuf) - 1) {
        ALOGE("path too long: %s\n", path);
    }
    strcpy(pathbuf, path);
    if (pathbuf[pathLength - 1] != '/') {
        pathbuf[pathLength++] = '/';
    }
    char* fileSpot = pathbuf + pathLength;
    int pathRemaining = sizeof(pathbuf) - pathLength - 1;

    DIR* dir = opendir(path);
    if (!dir) {
        ALOGE("opendir %s failed: %s", path, strerror(errno));
        return;
    }

    struct dirent* entry;
    while ((entry = readdir(dir))) {
        const char* name = entry->d_name;

        // ignore "." and ".."
        if (name[0] == '.' && (name[1] == 0 || (name[1] == '.' && name[2] == 0))) {
            continue;
        }

        int nameLength = strlen(name);
        if (nameLength > pathRemaining) {
            ALOGE("path %s/%s too long\n", path, name);
            continue;
        }
        strcpy(fileSpot, name);

        int type = entry->d_type;
        if (entry->d_type == DT_DIR) {
            deleteRecursive(pathbuf);
            rmdir(pathbuf);
        } else {
            unlink(pathbuf);
        }
    }
    closedir(dir);
}

static void deletePath(const char* path) {
    struct stat statbuf;
    if (stat(path, &statbuf) == 0) {
        if (S_ISDIR(statbuf.st_mode)) {
            deleteRecursive(path);
            rmdir(path);
        } else {
            unlink(path);
        }
    } else {
        ALOGE("deletePath stat failed for %s: %s", path, strerror(errno));
    }
}

MtpResponseCode MtpServer::doDeleteObject() {
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);
    MtpObjectFormat format = mRequest.getParameter(2);
    // FIXME - support deleting all objects if handle is 0xFFFFFFFF
    // FIXME - implement deleting objects by format

    MtpString filePath;
    int64_t fileLength;
    //int result = mDatabase->getObjectFilePath(handle, filePath, fileLength, format);
//Added Modification for ALPS00255822, bug from WHQL test
    int result;
    if(format != 0x00 && format != 0xFFFFFFFF)
    {
        return MTP_RESPONSE_SPECIFICATION_BY_FORMAT_UNSUPPORTED;
    }

    if (handle == MTP_PARENT_ROOT)
    {
        //For all storage
        MtpStorageID storageID = 0xFFFFFFFF;
        //For all
        MtpObjectHandle parent = 0;

        MtpObjectHandleList* handles = mDatabase->getObjectList(storageID, format, parent);
        if(handles)
        {
            int length = (*handles).size();
            //Added Modification for ALPS00255822, bug from WHQL test
            ALOGI("%s: Delete handlelist size = %d\n", __func__, length);
            //Added Modification for ALPS00255822, bug from WHQL test
    
            for (int i = 0; i < length; i++)
            {
                //MtpObjectHandle currentHandle;
                //handles->push(currentHandle);
                result = mDatabase->getObjectFilePath((*handles)[i], filePath, fileLength, format);
    
                sxlog_printf(ANDROID_LOG_VERBOSE, "MtpProperty",
                             "%s: Delete handles[%d] = 0x%x\n", __func__, i, (*handles)[i]);
    
                if (result == MTP_RESPONSE_OK)
                {
                    sxlog_printf(ANDROID_LOG_VERBOSE, "MtpProperty",
                                 "deleting %s", (const char *)filePath);
                    result = mDatabase->deleteFile((*handles)[i]);
                    // Don't delete the actual files unless the database deletion is allowed
                    if (result == MTP_RESPONSE_OK)
                    {
                        deletePath((const char *)filePath);
                    }
                }  
    
            }
            delete handles;
        }
        else
        {
            result = MTP_RESPONSE_INVALID_OBJECT_HANDLE;
        }
    }
    else
    {
        result = mDatabase->getObjectFilePath(handle, filePath, fileLength, format);

//Added Modification for ALPS00255822, bug from WHQL test

    if (result == MTP_RESPONSE_OK) {
        ALOGI("deleting %s", (const char *)filePath);
        result = mDatabase->deleteFile(handle);
        // Don't delete the actual files unless the database deletion is allowed
        if (result == MTP_RESPONSE_OK) {
            deletePath((const char *)filePath);
        }
    }
//Added Modification for ALPS00255822, bug from WHQL test
    }
//Added Modification for ALPS00255822, bug from WHQL test

    return result;
}

MtpResponseCode MtpServer::doGetObjectPropDesc() {
    MtpObjectProperty propCode = mRequest.getParameter(1);
    MtpObjectFormat format = mRequest.getParameter(2);
    ALOGI("GetObjectPropDesc %s %s\n", MtpDebug::getObjectPropCodeName(propCode),
                                        MtpDebug::getFormatCodeName(format));
    MtpProperty* property = mDatabase->getObjectPropertyDesc(propCode, format);
    if (!property)
        return MTP_RESPONSE_OBJECT_PROP_NOT_SUPPORTED;
    property->write(mData);
    delete property;
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doGetDevicePropDesc() {
    MtpDeviceProperty propCode = mRequest.getParameter(1);
    ALOGI("GetDevicePropDesc %s\n", MtpDebug::getDevicePropCodeName(propCode));
    MtpProperty* property = mDatabase->getDevicePropertyDesc(propCode);
    if (!property)
        return MTP_RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
    property->write(mData);
    delete property;
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doSendPartialObject() {
    if (!hasStorage())
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    MtpObjectHandle handle = mRequest.getParameter(1);
    uint64_t offset = mRequest.getParameter(2);
    uint64_t offset2 = mRequest.getParameter(3);
    offset = offset | (offset2 << 32);
    uint32_t length = mRequest.getParameter(4);

    ObjectEdit* edit = getEditObject(handle);
    if (!edit) {
        ALOGE("object not open for edit in doSendPartialObject");
        return MTP_RESPONSE_GENERAL_ERROR;
    }

    // can't start writing past the end of the file
    if (offset > edit->mSize) {
        ALOGI("writing past end of object, offset: %lld, edit->mSize: %lld", offset, edit->mSize);
        return MTP_RESPONSE_GENERAL_ERROR;
    }

    const char* filePath = (const char *)edit->mPath;
    ALOGI("receiving partial %s %lld %lld\n", filePath, offset, length);

    // read the header, and possibly some data
    int ret = mData.read(mFD);
    if (ret < MTP_CONTAINER_HEADER_SIZE)
        return MTP_RESPONSE_GENERAL_ERROR;
    int initialData = ret - MTP_CONTAINER_HEADER_SIZE;

    if (initialData > 0) {
        ret = write(edit->mFD, mData.getData(), initialData);
        offset += initialData;
        length -= initialData;
    }

    if (length > 0) {
        mtp_file_range  mfr;
        mfr.fd = edit->mFD;
        mfr.offset = offset;
        mfr.length = length;

        // transfer the file
        ret = ioctl(mFD, MTP_RECEIVE_FILE, (unsigned long)&mfr);
        ALOGI("MTP_RECEIVE_FILE returned %d", ret);
    }
    if (ret < 0) {
        mResponse.setParameter(1, 0);
        //Added Modification for ALPS00255822, bug from WHQL test
        sxlog_printf(ANDROID_LOG_ERROR, "MtpProperty",
                     "%s: MTP_RECEIVE_FILE returned %d, errorno = %d\n", __func__, ret, errno);
        //Added Modification for ALPS00255822, bug from WHQL test
        if (errno == ECANCELED)
            return MTP_RESPONSE_TRANSACTION_CANCELLED;
        else
            return MTP_RESPONSE_GENERAL_ERROR;
    }

    // reset so we don't attempt to send this back
    mData.reset();
    mResponse.setParameter(1, length);
    uint64_t end = offset + length;
    if (end > edit->mSize) {
        edit->mSize = end;
    }
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doTruncateObject() {
    MtpObjectHandle handle = mRequest.getParameter(1);
    ObjectEdit* edit = getEditObject(handle);
    if (!edit) {
        ALOGE("object not open for edit in doTruncateObject");
        return MTP_RESPONSE_GENERAL_ERROR;
    }

    uint64_t offset = mRequest.getParameter(2);
    uint64_t offset2 = mRequest.getParameter(3);
    offset |= (offset2 << 32);
    if (ftruncate(edit->mFD, offset) != 0) {
        return MTP_RESPONSE_GENERAL_ERROR;
    } else {
        edit->mSize = offset;
        return MTP_RESPONSE_OK;
    }
}

MtpResponseCode MtpServer::doBeginEditObject() {
    MtpObjectHandle handle = mRequest.getParameter(1);
    if (getEditObject(handle)) {
        ALOGE("object already open for edit in doBeginEditObject");
        return MTP_RESPONSE_GENERAL_ERROR;
    }

    MtpString path;
    int64_t fileLength;
    MtpObjectFormat format;
    int result = mDatabase->getObjectFilePath(handle, path, fileLength, format);
    if (result != MTP_RESPONSE_OK)
        return result;

    int fd = open((const char *)path, O_RDWR | O_EXCL);
    if (fd < 0) {
        ALOGE("open failed for %s in doBeginEditObject (%d)", (const char *)path, errno);
        return MTP_RESPONSE_GENERAL_ERROR;
    }

    addEditObject(handle, path, fileLength, format, fd);
    return MTP_RESPONSE_OK;
}

MtpResponseCode MtpServer::doEndEditObject() {
    MtpObjectHandle handle = mRequest.getParameter(1);
    ObjectEdit* edit = getEditObject(handle);
    if (!edit) {
        ALOGE("object not open for edit in doEndEditObject");
        return MTP_RESPONSE_GENERAL_ERROR;
    }

    commitEdit(edit);
    removeEditObject(handle);
    return MTP_RESPONSE_OK;
}

}  // namespace android
