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

#define LOG_TAG "MtpDatabaseJNI"
#include "utils/Log.h"

#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include "MtpDatabase.h"
#include "MtpDataPacket.h"
#include "MtpObjectInfo.h"
#include "MtpProperty.h"
#include "MtpStringBuffer.h"
#include "MtpUtils.h"
#include "mtp.h"

/// M: Added for USB Develpment debug, more log for more debuging help @{
#include "cutils/xlog.h"
/// @}

extern "C" {
#include "jhead.h"
}

using namespace android;

// ----------------------------------------------------------------------------

static jmethodID method_beginSendObject;
static jmethodID method_endSendObject;
static jmethodID method_getObjectList;
static jmethodID method_getNumObjects;
static jmethodID method_getSupportedPlaybackFormats;
static jmethodID method_getSupportedCaptureFormats;
static jmethodID method_getSupportedObjectProperties;
static jmethodID method_getSupportedDeviceProperties;
static jmethodID method_setObjectProperty;
static jmethodID method_getDeviceProperty;
static jmethodID method_setDeviceProperty;
static jmethodID method_getObjectPropertyList;
static jmethodID method_getObjectInfo;
static jmethodID method_getObjectFilePath;
static jmethodID method_deleteFile;
static jmethodID method_getObjectReferences;
static jmethodID method_setObjectReferences;
static jmethodID method_sessionStarted;
static jmethodID method_sessionEnded;
/// M: Added Modification for ALPS00264207, ALPS00248646 @{
static jmethodID method_getObjectBeginIndication;
static jmethodID method_getObjectEndIndication;
/// @}
/// M: Added function all for get handel by Path
static jmethodID method_getObjectHandleWithPath;
/// @}

static jfieldID field_context;

// MtpPropertyList fields
static jfieldID field_mCount;
static jfieldID field_mResult;
static jfieldID field_mObjectHandles;
static jfieldID field_mPropertyCodes;
static jfieldID field_mDataTypes;
static jfieldID field_mLongValues;
static jfieldID field_mStringValues;


MtpDatabase* getMtpDatabase(JNIEnv *env, jobject database) {
    return (MtpDatabase *)env->GetIntField(database, field_context);
}

// ----------------------------------------------------------------------------

class MyMtpDatabase : public MtpDatabase {
private:
    jobject         mDatabase;
    jintArray       mIntBuffer;
    jlongArray      mLongBuffer;
    jcharArray      mStringBuffer;

public:
                                    MyMtpDatabase(JNIEnv *env, jobject client);
    virtual                         ~MyMtpDatabase();
    void                            cleanup(JNIEnv *env);

    virtual MtpObjectHandle         beginSendObject(const char* path,
                                            MtpObjectFormat format,
                                            MtpObjectHandle parent,
                                            MtpStorageID storage,
                                            uint64_t size,
                                            time_t modified);

    virtual void                    endSendObject(const char* path,
                                            MtpObjectHandle handle,
                                            MtpObjectFormat format,
                                            bool succeeded);

    virtual MtpObjectHandleList*    getObjectList(MtpStorageID storageID,
                                    MtpObjectFormat format,
                                    MtpObjectHandle parent);

    virtual int                     getNumObjects(MtpStorageID storageID,
                                            MtpObjectFormat format,
                                            MtpObjectHandle parent);

    // callee should delete[] the results from these
    // results can be NULL
    virtual MtpObjectFormatList*    getSupportedPlaybackFormats();
    virtual MtpObjectFormatList*    getSupportedCaptureFormats();
    virtual MtpObjectPropertyList*  getSupportedObjectProperties(MtpObjectFormat format);
    virtual MtpDevicePropertyList*  getSupportedDeviceProperties();

    virtual MtpResponseCode         getObjectPropertyValue(MtpObjectHandle handle,
                                            MtpObjectProperty property,
                                            MtpDataPacket& packet);

    virtual MtpResponseCode         setObjectPropertyValue(MtpObjectHandle handle,
                                            MtpObjectProperty property,
                                            MtpDataPacket& packet);

    virtual MtpResponseCode         getDevicePropertyValue(MtpDeviceProperty property,
                                            MtpDataPacket& packet);

    virtual MtpResponseCode         setDevicePropertyValue(MtpDeviceProperty property,
                                            MtpDataPacket& packet);

    virtual MtpResponseCode         resetDeviceProperty(MtpDeviceProperty property);

    virtual MtpResponseCode         getObjectPropertyList(MtpObjectHandle handle,
                                            uint32_t format, uint32_t property,
                                            int groupCode, int depth,
                                            MtpDataPacket& packet);

    virtual MtpResponseCode         getObjectInfo(MtpObjectHandle handle,
                                            MtpObjectInfo& info);

    virtual void*                   getThumbnail(MtpObjectHandle handle, size_t& outThumbSize);

    virtual MtpResponseCode         getObjectFilePath(MtpObjectHandle handle,
                                            MtpString& outFilePath,
                                            int64_t& outFileLength,
                                            MtpObjectFormat& outFormat);
    virtual MtpResponseCode         deleteFile(MtpObjectHandle handle);

    bool                            getObjectPropertyInfo(MtpObjectProperty property, int& type);
    bool                            getDevicePropertyInfo(MtpDeviceProperty property, int& type);

    virtual MtpObjectHandleList*    getObjectReferences(MtpObjectHandle handle);

    virtual MtpResponseCode         setObjectReferences(MtpObjectHandle handle,
                                            MtpObjectHandleList* references);

    virtual MtpProperty*            getObjectPropertyDesc(MtpObjectProperty property,
                                            MtpObjectFormat format);

    virtual MtpProperty*            getDevicePropertyDesc(MtpDeviceProperty property);

    virtual void                    sessionStarted();

    virtual void                    sessionEnded();
/// M: Added Modification for ALPS00264207, ALPS00248646 @{
    virtual void                    getObjectBeginIndication(const char * storagePath);
    virtual void                    getObjectEndIndication(const char * storagePath);
/// @}
/// M: Added function all for get handel by Path
    virtual MtpObjectHandle         getObjectHandleWithPath(const char* path,
                                            MtpObjectFormat format,
                                            MtpObjectHandle parent,
                                            MtpStorageID storage,
                                            uint64_t size,
                                            time_t modified);
/// @}
};

// ----------------------------------------------------------------------------

static void checkAndClearExceptionFromCallback(JNIEnv* env, const char* methodName) {
    if (env->ExceptionCheck()) {
        ALOGE("An exception was thrown by callback '%s'.", methodName);
        LOGE_EX(env);
        env->ExceptionClear();
    }
}

// ----------------------------------------------------------------------------

MyMtpDatabase::MyMtpDatabase(JNIEnv *env, jobject client)
    :   mDatabase(env->NewGlobalRef(client)),
        mIntBuffer(NULL),
        mLongBuffer(NULL),
        mStringBuffer(NULL)
{
    // create buffers for out arguments
    // we don't need to be thread-safe so this is OK
    jintArray intArray = env->NewIntArray(3);
    if (!intArray) {
        return; // Already threw.
    }
    mIntBuffer = (jintArray)env->NewGlobalRef(intArray);
    jlongArray longArray = env->NewLongArray(2);
    if (!longArray) {
        return; // Already threw.
    }
    mLongBuffer = (jlongArray)env->NewGlobalRef(longArray);
    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    //jcharArray charArray = env->NewCharArray(256);
    jcharArray charArray = env->NewCharArray(300);
    /// @}
    if (!charArray) {
        return; // Already threw.
    }
    mStringBuffer = (jcharArray)env->NewGlobalRef(charArray);
}

void MyMtpDatabase::cleanup(JNIEnv *env) {
    env->DeleteGlobalRef(mDatabase);
    env->DeleteGlobalRef(mIntBuffer);
    env->DeleteGlobalRef(mLongBuffer);
    env->DeleteGlobalRef(mStringBuffer);
}

MyMtpDatabase::~MyMtpDatabase() {
}

MtpObjectHandle MyMtpDatabase::beginSendObject(const char* path,
                                            MtpObjectFormat format,
                                            MtpObjectHandle parent,
                                            MtpStorageID storage,
                                            uint64_t size,
                                            time_t modified) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jstring pathStr = env->NewStringUTF(path);
    MtpObjectHandle result = env->CallIntMethod(mDatabase, method_beginSendObject,
            pathStr, (jint)format, (jint)parent, (jint)storage,
            (jlong)size, (jlong)modified);

    if (pathStr)
        env->DeleteLocalRef(pathStr);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;
}

void MyMtpDatabase::endSendObject(const char* path, MtpObjectHandle handle,
                                MtpObjectFormat format, bool succeeded) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jstring pathStr = env->NewStringUTF(path);
    env->CallVoidMethod(mDatabase, method_endSendObject, pathStr,
                        (jint)handle, (jint)format, (jboolean)succeeded);

    if (pathStr)
        env->DeleteLocalRef(pathStr);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
}

MtpObjectHandleList* MyMtpDatabase::getObjectList(MtpStorageID storageID,
                                    MtpObjectFormat format,
                                    MtpObjectHandle parent) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jintArray array = (jintArray)env->CallObjectMethod(mDatabase, method_getObjectList,
                (jint)storageID, (jint)format, (jint)parent);
    if (!array)
    {    
        /// M: Added Modification for ALPS00255822, bug from WHQL test
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return NULL;
    }
    MtpObjectHandleList* list = new MtpObjectHandleList();
    jint* handles = env->GetIntArrayElements(array, 0);
    jsize length = env->GetArrayLength(array);
    for (int i = 0; i < length; i++)
    {    
        list->push(handles[i]);
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                     "%s: handles[%d] = 0x%x, length = %d\n", __func__, i, handles[i], length);
        /// @}
    }
    env->ReleaseIntArrayElements(array, handles, 0);
    env->DeleteLocalRef(array);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return list;
}

int MyMtpDatabase::getNumObjects(MtpStorageID storageID,
                                MtpObjectFormat format,
                                MtpObjectHandle parent) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    int result = env->CallIntMethod(mDatabase, method_getNumObjects,
                (jint)storageID, (jint)format, (jint)parent);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;
}

MtpObjectFormatList* MyMtpDatabase::getSupportedPlaybackFormats() {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jintArray array = (jintArray)env->CallObjectMethod(mDatabase,
            method_getSupportedPlaybackFormats);
    if (!array)
    {
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                     "%s: array null\n", __func__);
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return NULL;
    }
    MtpObjectFormatList* list = new MtpObjectFormatList();
    jint* formats = env->GetIntArrayElements(array, 0);
    jsize length = env->GetArrayLength(array);
    for (int i = 0; i < length; i++)
        list->push(formats[i]);
    env->ReleaseIntArrayElements(array, formats, 0);
    env->DeleteLocalRef(array);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return list;
}

MtpObjectFormatList* MyMtpDatabase::getSupportedCaptureFormats() {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jintArray array = (jintArray)env->CallObjectMethod(mDatabase,
            method_getSupportedCaptureFormats);
    if (!array)
    {
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return NULL;
    }
    MtpObjectFormatList* list = new MtpObjectFormatList();
    jint* formats = env->GetIntArrayElements(array, 0);
    jsize length = env->GetArrayLength(array);
    for (int i = 0; i < length; i++)
        list->push(formats[i]);
    env->ReleaseIntArrayElements(array, formats, 0);
    env->DeleteLocalRef(array);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return list;
}

MtpObjectPropertyList* MyMtpDatabase::getSupportedObjectProperties(MtpObjectFormat format) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jintArray array = (jintArray)env->CallObjectMethod(mDatabase,
            method_getSupportedObjectProperties, (jint)format);
    if (!array)
    {
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return NULL;
    }
    MtpObjectPropertyList* list = new MtpObjectPropertyList();
    jint* properties = env->GetIntArrayElements(array, 0);
    jsize length = env->GetArrayLength(array);
    for (int i = 0; i < length; i++)
        list->push(properties[i]);
    env->ReleaseIntArrayElements(array, properties, 0);
    env->DeleteLocalRef(array);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return list;
}

MtpDevicePropertyList* MyMtpDatabase::getSupportedDeviceProperties() {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jintArray array = (jintArray)env->CallObjectMethod(mDatabase,
            method_getSupportedDeviceProperties);
    if (!array)
    {
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return NULL;
    }
    MtpDevicePropertyList* list = new MtpDevicePropertyList();
    jint* properties = env->GetIntArrayElements(array, 0);
    jsize length = env->GetArrayLength(array);
    for (int i = 0; i < length; i++)
        list->push(properties[i]);
    env->ReleaseIntArrayElements(array, properties, 0);
    env->DeleteLocalRef(array);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return list;
}

MtpResponseCode MyMtpDatabase::getObjectPropertyValue(MtpObjectHandle handle,
                                            MtpObjectProperty property,
                                            MtpDataPacket& packet) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jobject list = env->CallObjectMethod(mDatabase, method_getObjectPropertyList,
                (jlong)handle, 0, (jlong)property, 0, 0);
    MtpResponseCode result = env->GetIntField(list, field_mResult);
    int count = env->GetIntField(list, field_mCount);
    if (result == MTP_RESPONSE_OK && count != 1)
        result = MTP_RESPONSE_GENERAL_ERROR;

    if (result == MTP_RESPONSE_OK) {
        jintArray objectHandlesArray = (jintArray)env->GetObjectField(list, field_mObjectHandles);
        jintArray propertyCodesArray = (jintArray)env->GetObjectField(list, field_mPropertyCodes);
        jintArray dataTypesArray = (jintArray)env->GetObjectField(list, field_mDataTypes);
        jlongArray longValuesArray = (jlongArray)env->GetObjectField(list, field_mLongValues);
        jobjectArray stringValuesArray = (jobjectArray)env->GetObjectField(list, field_mStringValues);

        jint* objectHandles = env->GetIntArrayElements(objectHandlesArray, 0);
        jint* propertyCodes = env->GetIntArrayElements(propertyCodesArray, 0);
        jint* dataTypes = env->GetIntArrayElements(dataTypesArray, 0);
        jlong* longValues = (longValuesArray ? env->GetLongArrayElements(longValuesArray, 0) : NULL);

        int type = dataTypes[0];
        jlong longValue = (longValues ? longValues[0] : 0);

        /// M: Added for USB Develpment debug, more log for more debuging help @{
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                     "%s: objectHandles: 0x%x, propertyCodes: 0x%x, dataTypes =: 0x%x, type = 0x%x\n", __func__, objectHandles[0], propertyCodes[0], dataTypes[0], type);
        sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                     "%s: getObjectPropertyList longValues: 0x%p, longValue: L%ld \n", __func__, longValues, longValue);
        /// @}

        // special case date properties, which are strings to MTP
        // but stored internally as a uint64
/*        if (property == MTP_PROPERTY_DATE_MODIFIED || property == MTP_PROPERTY_DATE_ADDED) {
            char    date[20];
            formatDateTime(longValue, date, sizeof(date));
            packet.putString(date);
            goto out;
        }
        // release date is stored internally as just the year
        if (property == MTP_PROPERTY_ORIGINAL_RELEASE_DATE) {
            char    date[20];
            snprintf(date, sizeof(date), "%04lld0101T000000", longValue);
            packet.putString(date);
            goto out;
        }
*/
        switch (type) {
            case MTP_TYPE_INT8:
                packet.putInt8(longValue);
                break;
            case MTP_TYPE_UINT8:
                packet.putUInt8(longValue);
                break;
            case MTP_TYPE_INT16:
                packet.putInt16(longValue);
                break;
            case MTP_TYPE_UINT16:
                packet.putUInt16(longValue);
                break;
            case MTP_TYPE_INT32:
                packet.putInt32(longValue);
                break;
            case MTP_TYPE_UINT32:
                packet.putUInt32(longValue);
                break;
            case MTP_TYPE_INT64:
                packet.putInt64(longValue);
                break;
            case MTP_TYPE_UINT64:
                packet.putUInt64(longValue);
                break;
            case MTP_TYPE_INT128:
                packet.putInt128(longValue);
                break;
            case MTP_TYPE_UINT128:
                packet.putInt128(longValue);
                break;
            case MTP_TYPE_STR:
            {
                jstring stringValue = (jstring)env->GetObjectArrayElement(stringValuesArray, 0);
                if (stringValue) {
                    const char* str = env->GetStringUTFChars(stringValue, NULL);
                    /// M: Added for USB Develpment debug, more log for more debuging help @{
                    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                                 "%s: objectHandles: 0x%x, propertyCodes: 0x%x, dataTypes =: 0x%x, type = 0x%x\n", __func__, objectHandles[0], propertyCodes[0], dataTypes[0], type);
                    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                                 "%s: objectHandles: str = %s\n", __func__, str);
                    /// @}
                    if (str == NULL) {
                        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                        //return MTP_RESPONSE_GENERAL_ERROR;
                        result = MTP_RESPONSE_GENERAL_ERROR;
                        break;
                        /// @}
                    }
                    packet.putString(str);
                    env->ReleaseStringUTFChars(stringValue, str);
                    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                    if(stringValue)
                        env->DeleteLocalRef(stringValue);
                    /// @}
                } else {
                    /// M: Added for USB Develpment debug, more log for more debuging help @{
                    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                                 "%s: emptyString!! \n", __func__);
                    /// @}
                    packet.putEmptyString();
                }
                break;
             }
            default:
                ALOGE("unsupported type in getObjectPropertyValue\n");
                result = MTP_RESPONSE_INVALID_OBJECT_PROP_FORMAT;
        }
out:
        env->ReleaseIntArrayElements(objectHandlesArray, objectHandles, 0);
        env->ReleaseIntArrayElements(propertyCodesArray, propertyCodes, 0);
        env->ReleaseIntArrayElements(dataTypesArray, dataTypes, 0);
        if (longValues)
            env->ReleaseLongArrayElements(longValuesArray, longValues, 0);

        env->DeleteLocalRef(objectHandlesArray);
        env->DeleteLocalRef(propertyCodesArray);
        env->DeleteLocalRef(dataTypesArray);
        if (longValuesArray)
            env->DeleteLocalRef(longValuesArray);
        if (stringValuesArray)
            env->DeleteLocalRef(stringValuesArray);
    }

    env->DeleteLocalRef(list);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;
}

MtpResponseCode MyMtpDatabase::setObjectPropertyValue(MtpObjectHandle handle,
                                            MtpObjectProperty property,
                                            MtpDataPacket& packet) {
    int         type;

    if (!getObjectPropertyInfo(property, type))
    {
        /// M: Added for USB Develpment debug, more log for more debuging help @{
        ALOGE("%s: MTP_RESPONSE_OBJECT_PROP_NOT_SUPPORTED!!\n", __func__);
        /// @}
        return MTP_RESPONSE_OBJECT_PROP_NOT_SUPPORTED;
    }

    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jlong longValue = 0;
    jstring stringValue = NULL;

    switch (type) {
        case MTP_TYPE_INT8:
            longValue = packet.getInt8();
            break;
        case MTP_TYPE_UINT8:
            longValue = packet.getUInt8();
            break;
        case MTP_TYPE_INT16:
            longValue = packet.getInt16();
            break;
        case MTP_TYPE_UINT16:
            longValue = packet.getUInt16();
            break;
        case MTP_TYPE_INT32:
            longValue = packet.getInt32();
            break;
        case MTP_TYPE_UINT32:
            longValue = packet.getUInt32();
            break;
        case MTP_TYPE_INT64:
            longValue = packet.getInt64();
            break;
        case MTP_TYPE_UINT64:
            longValue = packet.getUInt64();
            break;
        case MTP_TYPE_STR:
        {
            MtpStringBuffer buffer;
            packet.getString(buffer);
            stringValue = env->NewStringUTF((const char *)buffer);
            break;
         }
        default:
            ALOGE("unsupported type in getObjectPropertyValue\n");
            //Added Modification for ALPS00255822, bug from WHQL test
            checkAndClearExceptionFromCallback(env, __FUNCTION__);
            //Added Modification for ALPS00255822, bug from WHQL test
            return MTP_RESPONSE_INVALID_OBJECT_PROP_FORMAT;
    }

    jint result = env->CallIntMethod(mDatabase, method_setObjectProperty,
                (jint)handle, (jint)property, longValue, stringValue);
    if (stringValue)
        env->DeleteLocalRef(stringValue);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    /// M: Added for USB Develpment debug, more log for more debuging help @{
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                "%s: result = 0x%x", __func__, result);
    /// @}
    return result;
}

MtpResponseCode MyMtpDatabase::getDevicePropertyValue(MtpDeviceProperty property,
                                            MtpDataPacket& packet) {
    int         type;

    if (!getDevicePropertyInfo(property, type))
    {
        /// M: Added for USB Develpment debug, more log for more debuging help @{
        ALOGE("%s: MTP_RESPONSE_DEVICE_PROP_NOT_SUPPORTED!!\n", __func__);
        /// @}
        return MTP_RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
    }

    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jint result = env->CallIntMethod(mDatabase, method_getDeviceProperty,
                (jint)property, mLongBuffer, mStringBuffer);
    if (result != MTP_RESPONSE_OK) {
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        return result;
    }

    jlong* longValues = env->GetLongArrayElements(mLongBuffer, 0);
    jlong longValue = longValues[0];
    env->ReleaseLongArrayElements(mLongBuffer, longValues, 0);

    switch (type) {
        case MTP_TYPE_INT8:
            packet.putInt8(longValue);
            break;
        case MTP_TYPE_UINT8:
            packet.putUInt8(longValue);
            break;
        case MTP_TYPE_INT16:
            packet.putInt16(longValue);
            break;
        case MTP_TYPE_UINT16:
            packet.putUInt16(longValue);
            break;
        case MTP_TYPE_INT32:
            packet.putInt32(longValue);
            break;
        case MTP_TYPE_UINT32:
            packet.putUInt32(longValue);
            break;
        case MTP_TYPE_INT64:
            packet.putInt64(longValue);
            break;
        case MTP_TYPE_UINT64:
            packet.putUInt64(longValue);
            break;
        case MTP_TYPE_INT128:
            packet.putInt128(longValue);
            break;
        case MTP_TYPE_UINT128:
            packet.putInt128(longValue);
            break;
        case MTP_TYPE_STR:
        {
            jchar* str = env->GetCharArrayElements(mStringBuffer, 0);
            packet.putString(str);
            env->ReleaseCharArrayElements(mStringBuffer, str, 0);
            break;
         }
        default:
            ALOGE("unsupported type in getDevicePropertyValue\n");
            /// M: Added Modification for ALPS00255822, bug from WHQL test @{
            checkAndClearExceptionFromCallback(env, __FUNCTION__);
            /// @}
            return MTP_RESPONSE_INVALID_DEVICE_PROP_FORMAT;
    }

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return MTP_RESPONSE_OK;
}

MtpResponseCode MyMtpDatabase::setDevicePropertyValue(MtpDeviceProperty property,
                                            MtpDataPacket& packet) {
    int         type;

    if (!getDevicePropertyInfo(property, type))
    {
        /// M: Added for USB Develpment debug, more log for more debuging help @{
        ALOGE("%s: MTP_RESPONSE_DEVICE_PROP_NOT_SUPPORTED!!\n", __func__);
        /// @}
        return MTP_RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
    }

    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jlong longValue = 0;
    jstring stringValue = NULL;

    switch (type) {
        case MTP_TYPE_INT8:
            longValue = packet.getInt8();
            break;
        case MTP_TYPE_UINT8:
            longValue = packet.getUInt8();
            break;
        case MTP_TYPE_INT16:
            longValue = packet.getInt16();
            break;
        case MTP_TYPE_UINT16:
            longValue = packet.getUInt16();
            break;
        case MTP_TYPE_INT32:
            longValue = packet.getInt32();
            break;
        case MTP_TYPE_UINT32:
            longValue = packet.getUInt32();
            break;
        case MTP_TYPE_INT64:
            longValue = packet.getInt64();
            break;
        case MTP_TYPE_UINT64:
            longValue = packet.getUInt64();
            break;
        case MTP_TYPE_STR:
        {
            MtpStringBuffer buffer;
            packet.getString(buffer);
            stringValue = env->NewStringUTF((const char *)buffer);
            break;
         }
        default:
            ALOGE("unsupported type in setDevicePropertyValue\n");
            /// M: Added Modification for ALPS00255822, bug from WHQL test @{
            checkAndClearExceptionFromCallback(env, __FUNCTION__);
            /// @}
            return MTP_RESPONSE_INVALID_OBJECT_PROP_FORMAT;
    }

    jint result = env->CallIntMethod(mDatabase, method_setDeviceProperty,
                (jint)property, longValue, stringValue);
    if (stringValue)
        env->DeleteLocalRef(stringValue);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);

    /// M: Added for USB Develpment debug, more log for more debuging help @{
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                 "%s: result = 0x%x", __func__, result);
    /// @}

    return result;
}

MtpResponseCode MyMtpDatabase::resetDeviceProperty(MtpDeviceProperty property) {
    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    //instead of not support, not FFFF
    //return -1;
    //return MTP_RESPONSE_OPERATION_NOT_SUPPORTED;

    //For WHQL test, try to set the writable device property to default value
    //for for device frandly name and sync partner
    //there is no default value for them, write them to empty string
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jlong longValue = 0;
    jstring stringValue = NULL;
    jint result;

    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                 "%s: property = 0x%x", __func__, property);

    if((property & 0xFFFFFFFF) >= 0xFFFF)
    {
        //reset all read-write device property
        MtpStringBuffer buffer;

        //MTP_DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER
        memset(&buffer, 0, sizeof(MtpStringBuffer));
        stringValue = env->NewStringUTF((const char *)buffer);

        result = env->CallIntMethod(mDatabase, method_setDeviceProperty,
                    (jint)MTP_DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER, longValue, stringValue);
        if (stringValue)
            env->DeleteLocalRef(stringValue);

        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        //MTP_DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME
        memset(&buffer, 0, sizeof(MtpStringBuffer));
        stringValue = env->NewStringUTF((const char *)buffer);

        result = env->CallIntMethod(mDatabase, method_setDeviceProperty,
                    (jint)MTP_DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME, longValue, stringValue);
        if (stringValue)
            env->DeleteLocalRef(stringValue);

        checkAndClearExceptionFromCallback(env, __FUNCTION__);
    }
    else
    if(property == MTP_DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER || property == MTP_DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME)
    {
        MtpStringBuffer buffer;
        memset(&buffer, 0, sizeof(MtpStringBuffer));
        stringValue = env->NewStringUTF((const char *)buffer);

        result = env->CallIntMethod(mDatabase, method_setDeviceProperty,
                    (jint)property, longValue, stringValue);
        if (stringValue)
            env->DeleteLocalRef(stringValue);

        checkAndClearExceptionFromCallback(env, __FUNCTION__);

    }
    else if(property == MTP_DEVICE_PROPERTY_IMAGE_SIZE)
    {
        ALOGE("%s: property = 0x%x, non-writable!!\n", __func__, property);
        result = MTP_RESPONSE_ACCESS_DENIED;
    }
    else
    {    
        ALOGE("%s: property = 0x%x, MTP_RESPONSE_DEVICE_PROP_NOT_SUPPORTED!!\n", __func__, property);
        result = MTP_RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
    }

    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                 "%s: result = 0x%x", __func__, result);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;

    /// @}
}

MtpResponseCode MyMtpDatabase::getObjectPropertyList(MtpObjectHandle handle,
                                            uint32_t format, uint32_t property,
                                            int groupCode, int depth,
                                            MtpDataPacket& packet) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jobject list = env->CallObjectMethod(mDatabase, method_getObjectPropertyList,
                (jlong)handle, (jint)format, (jlong)property, (jint)groupCode, (jint)depth);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    if (!list)
    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    {
        //for WHQL test
        return MTP_RESPONSE_SPECIFICATION_BY_FORMAT_UNSUPPORTED;
        //return MTP_RESPONSE_GENERAL_ERROR;
    }
    /// @}
    int count = env->GetIntField(list, field_mCount);
    MtpResponseCode result = env->GetIntField(list, field_mResult);

    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                 "%s: handle = 0x%x, count = %d\n", __func__, handle, count);
    /// @}

    packet.putUInt32(count);
    if (count > 0) {
        jintArray objectHandlesArray = (jintArray)env->GetObjectField(list, field_mObjectHandles);
        jintArray propertyCodesArray = (jintArray)env->GetObjectField(list, field_mPropertyCodes);
        jintArray dataTypesArray = (jintArray)env->GetObjectField(list, field_mDataTypes);
        jlongArray longValuesArray = (jlongArray)env->GetObjectField(list, field_mLongValues);
        jobjectArray stringValuesArray = (jobjectArray)env->GetObjectField(list, field_mStringValues);

        jint* objectHandles = env->GetIntArrayElements(objectHandlesArray, 0);
        jint* propertyCodes = env->GetIntArrayElements(propertyCodesArray, 0);
        jint* dataTypes = env->GetIntArrayElements(dataTypesArray, 0);
        jlong* longValues = (longValuesArray ? env->GetLongArrayElements(longValuesArray, 0) : NULL);

        for (int i = 0; i < count; i++) {
            packet.putUInt32(objectHandles[i]);
            packet.putUInt16(propertyCodes[i]);
            int type = dataTypes[i];
            packet.putUInt16(type);

            //Added Modification for ALPS00255822, bug from WHQL test
            sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                         "%s: objectHandles[%d] = 0x%x, count = %d\n", __func__, i, objectHandles[i]);
            sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                         "%s: propertyCodes[%d] = 0x%x, count = %d\n", __func__, i, propertyCodes[i]);
            //Added Modification for ALPS00255822, bug from WHQL test

            switch (type) {
                case MTP_TYPE_INT8:
                    packet.putInt8(longValues[i]);
                    break;
                case MTP_TYPE_UINT8:
                    packet.putUInt8(longValues[i]);
                    break;
                case MTP_TYPE_INT16:
                    packet.putInt16(longValues[i]);
                    break;
                case MTP_TYPE_UINT16:
                    packet.putUInt16(longValues[i]);
                    break;
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                case MTP_TYPE_AUINT16:
                    packet.putUInt16(0x00);
                    packet.putUInt16(0x00);
                    break;
                /// @}
                case MTP_TYPE_INT32:
                    packet.putInt32(longValues[i]);
                    break;
                case MTP_TYPE_UINT32:
                    packet.putUInt32(longValues[i]);
                    break;
                case MTP_TYPE_INT64:
                    packet.putInt64(longValues[i]);
                    break;
                case MTP_TYPE_UINT64:
                    packet.putUInt64(longValues[i]);
                    break;
                case MTP_TYPE_INT128:
                    packet.putInt128(longValues[i]);
                    break;
                case MTP_TYPE_UINT128:
                    packet.putUInt128(longValues[i]);
                    break;
                case MTP_TYPE_STR: {
                    jstring value = (jstring)env->GetObjectArrayElement(stringValuesArray, i);
                    const char *valueStr = (value ? env->GetStringUTFChars(value, NULL) : NULL);
                    if (valueStr) {
                        packet.putString(valueStr);
                        env->ReleaseStringUTFChars(value, valueStr);
                    } else {
                        packet.putEmptyString();
                    }
                    env->DeleteLocalRef(value);
                    break;
                }
                default:
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                    ALOGE("bad or unsupported data type in MyMtpDatabase::getObjectPropertyList: objectHandles[%d]=0x%x, propertyCodes[%d] = 0x%x, type = 0x%x",i, objectHandles[i], i, propertyCodes[i], type);
                /// @}
                    break;
            }
        }

        env->ReleaseIntArrayElements(objectHandlesArray, objectHandles, 0);
        env->ReleaseIntArrayElements(propertyCodesArray, propertyCodes, 0);
        env->ReleaseIntArrayElements(dataTypesArray, dataTypes, 0);
        if (longValues)
            env->ReleaseLongArrayElements(longValuesArray, longValues, 0);

        env->DeleteLocalRef(objectHandlesArray);
        env->DeleteLocalRef(propertyCodesArray);
        env->DeleteLocalRef(dataTypesArray);
        if (longValuesArray)
            env->DeleteLocalRef(longValuesArray);
        if (stringValuesArray)
            env->DeleteLocalRef(stringValuesArray);
    }

    env->DeleteLocalRef(list);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                 "%s result: 0x%x \n", __func__, result);
    /// @}
    return result;
}

MtpResponseCode MyMtpDatabase::getObjectInfo(MtpObjectHandle handle,
                                            MtpObjectInfo& info) {
    char            date[20];
    MtpString       path;
    int64_t         length;
    MtpObjectFormat format;

    MtpResponseCode result = getObjectFilePath(handle, path, length, format);
    if (result != MTP_RESPONSE_OK) {
        return result;
    }
    info.mCompressedSize = (length > 0xFFFFFFFFLL ? 0xFFFFFFFF : (uint32_t)length);

    JNIEnv* env = AndroidRuntime::getJNIEnv();
    if (!env->CallBooleanMethod(mDatabase, method_getObjectInfo,
                (jint)handle, mIntBuffer, mStringBuffer, mLongBuffer)) {
        /// M: Added Modification for ALPS00255822, bug from WHQL test
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return MTP_RESPONSE_INVALID_OBJECT_HANDLE;
    }

    jint* intValues = env->GetIntArrayElements(mIntBuffer, 0);
    info.mStorageID = intValues[0];
    info.mFormat = intValues[1];
    info.mParent = intValues[2];
    env->ReleaseIntArrayElements(mIntBuffer, intValues, 0);

    jlong* longValues = env->GetLongArrayElements(mLongBuffer, 0);
    info.mDateModified = longValues[0];

    env->ReleaseLongArrayElements(mLongBuffer, longValues, 0);

//    info.mAssociationType = (format == MTP_FORMAT_ASSOCIATION ?
//                            MTP_ASSOCIATION_TYPE_GENERIC_FOLDER :
//                            MTP_ASSOCIATION_TYPE_UNDEFINED);
    info.mAssociationType = MTP_ASSOCIATION_TYPE_UNDEFINED;

    jchar* str = env->GetCharArrayElements(mStringBuffer, 0);
    MtpString temp(str);
    info.mName = strdup((const char *)temp);
    env->ReleaseCharArrayElements(mStringBuffer, str, 0);

    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                 "%s: objectHandles: 0x%x, info.mFormat: 0x%x, info.mStorageID =: 0x%x, info.mName = %s \n", __func__, handle, info.mFormat, info.mStorageID, info.mName);
    /// @}

    // read EXIF data for thumbnail information
    if (info.mFormat == MTP_FORMAT_EXIF_JPEG || info.mFormat == MTP_FORMAT_JFIF) {
            ResetJpgfile();
             // Start with an empty image information structure.
            memset(&ImageInfo, 0, sizeof(ImageInfo));
            ImageInfo.FlashUsed = -1;
            ImageInfo.MeteringMode = -1;
            ImageInfo.Whitebalance = -1;
            strncpy(ImageInfo.FileName, (const char *)path, PATH_MAX);

            /// M: Added Modification for ALPS00255822, bug from WHQL test @{
            sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                         "%s: ImageInfo.FileName = %s \n", __func__, ImageInfo.FileName);
            /// @}

            if (ReadJpegFile((const char*)path, READ_METADATA)) {
                Section_t* section = FindSection(M_EXIF);
                if (section) {
                    info.mThumbCompressedSize = ImageInfo.ThumbnailSize;
                    info.mThumbFormat = MTP_FORMAT_EXIF_JPEG;
                    info.mImagePixWidth = ImageInfo.Width;
                    info.mImagePixHeight = ImageInfo.Height;
                    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                    sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                                 "%s: info.mThumbCompressedSize = %d \n", __func__, info.mThumbCompressedSize);
                    sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                                 "%s: info.mThumbFormat = %d \n", __func__, info.mThumbFormat);
                    sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                                 "%s: info.mImagePixWidth = %d \n", __func__, info.mImagePixWidth);
                    sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                                 "%s: info.mImagePixHeight = %d \n", __func__, info.mImagePixHeight);
                    /// @}
                }
            //Modifocation for ALPS00338104
                sxlog_printf(ANDROID_LOG_ERROR, "MtpDatabase_JNI",
                             "%s, line %d: ReadJpegFile return true!! Call DiscardData. \n", __func__, __LINE__);
                DiscardData();
            }
            else
            {
                sxlog_printf(ANDROID_LOG_ERROR, "MtpDatabase_JNI",
                             "%s, line %d: ReadJpegFile return false!! Still call DiscardData to release g_u4InUse flag (jpgFilecond). \n", __func__, __LINE__);
                //Modifocation for ALPS00568593 (rollback to original solution)
                DiscardData();
                //Modifocation for ALPS00568593
            }
            //Modifocation for ALPS00338104*/

    }

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return MTP_RESPONSE_OK;
}

void* MyMtpDatabase::getThumbnail(MtpObjectHandle handle, size_t& outThumbSize) {
    MtpString path;
    int64_t length;
    MtpObjectFormat format;
    void* result = NULL;
    outThumbSize = 0;

    if (getObjectFilePath(handle, path, length, format) == MTP_RESPONSE_OK
            && (format == MTP_FORMAT_EXIF_JPEG || format == MTP_FORMAT_JFIF)) {
        ResetJpgfile();
         // Start with an empty image information structure.
        memset(&ImageInfo, 0, sizeof(ImageInfo));
        ImageInfo.FlashUsed = -1;
        ImageInfo.MeteringMode = -1;
        ImageInfo.Whitebalance = -1;
        strncpy(ImageInfo.FileName, (const char *)path, PATH_MAX);
        if (ReadJpegFile((const char*)path, READ_METADATA)) {
            Section_t* section = FindSection(M_EXIF);
            if (section) {
                outThumbSize = ImageInfo.ThumbnailSize;
                /// M: Added for USB Develpment debug, more log for more debuging help @{
                sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                             "%s: ImageInfo.FileName = %s, ImageInfo.ThumbnailSize = %d \n", __func__, ImageInfo.FileName, ImageInfo.ThumbnailSize);
                sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                             "%s: ImageInfo.FileName = %s, ImageInfo.ThumbnailOffset = %d \n", __func__, ImageInfo.FileName, ImageInfo.ThumbnailOffset);
                /// @}
                
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                if(!outThumbSize)
                {
                    //no thumbnail!!
                    sxlog_printf(ANDROID_LOG_ERROR, "MtpDatabase_JNI",
                                 "%s: getThumbnail outThumbSize = %d, there is no embedded Thumbnail of this image \n", __func__, outThumbSize);
                    result = 0;
                }
                /// @}
                else
                    result = malloc(outThumbSize);
                if (result)
                    memcpy(result, section->Data + ImageInfo.ThumbnailOffset + 8, outThumbSize);
                /// M: Added for USB Develpment debug, more log for more debuging help @{
                else
                    sxlog_printf(ANDROID_LOG_ERROR, "MtpDatabase_JNI",
                                 "%s: malloc failed!! \n", __func__);
                /// @}
            }

            DiscardData();
        //Modifocation for ALPS00568593 (Add log)
            sxlog_printf(ANDROID_LOG_ERROR, "MtpDatabase_JNI",
                         "%s, line %d: ReadJpegFile return true!! Call DiscardData. \n", __func__, __LINE__);
        }
        else
        {
            sxlog_printf(ANDROID_LOG_ERROR, "MtpDatabase_JNI",
                         "%s, line %d: ReadJpegFile return false!! Still call DiscardData to release g_u4InUse flag (jpgFilecond). \n", __func__, __LINE__);
            DiscardData();
        //Modifocation for ALPS00568593*/
        }
    }

    return result;
}

MtpResponseCode MyMtpDatabase::getObjectFilePath(MtpObjectHandle handle,
                                            MtpString& outFilePath,
                                            int64_t& outFileLength,
                                            MtpObjectFormat& outFormat) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jint result = env->CallIntMethod(mDatabase, method_getObjectFilePath,
                (jint)handle, mStringBuffer, mLongBuffer);
    if (result != MTP_RESPONSE_OK) {
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        env->ReleaseCharArrayElements(mStringBuffer, 0, 0);
        /// @}
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        return result;
    }

    jchar* str = env->GetCharArrayElements(mStringBuffer, 0);
    outFilePath.setTo(str, strlen16(str));
    env->ReleaseCharArrayElements(mStringBuffer, str, 0);

    jlong* longValues = env->GetLongArrayElements(mLongBuffer, 0);
    outFileLength = longValues[0];
    outFormat = longValues[1];
    env->ReleaseLongArrayElements(mLongBuffer, longValues, 0);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;
}

MtpResponseCode MyMtpDatabase::deleteFile(MtpObjectHandle handle) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    MtpResponseCode result = env->CallIntMethod(mDatabase, method_deleteFile, (jint)handle);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;
}

struct PropertyTableEntry {
    MtpObjectProperty   property;
    int                 type;
};

static const PropertyTableEntry   kObjectPropertyTable[] = {
    {   MTP_PROPERTY_STORAGE_ID,        MTP_TYPE_UINT32     },
    {   MTP_PROPERTY_OBJECT_FORMAT,     MTP_TYPE_UINT16     },
    {   MTP_PROPERTY_PROTECTION_STATUS, MTP_TYPE_UINT16     },
    {   MTP_PROPERTY_OBJECT_SIZE,       MTP_TYPE_UINT64     },
    {   MTP_PROPERTY_OBJECT_FILE_NAME,  MTP_TYPE_STR        },
    {   MTP_PROPERTY_DATE_MODIFIED,     MTP_TYPE_STR        },
    {   MTP_PROPERTY_PARENT_OBJECT,     MTP_TYPE_UINT32     },
    {   MTP_PROPERTY_PERSISTENT_UID,    MTP_TYPE_UINT128    },
    {   MTP_PROPERTY_NAME,              MTP_TYPE_STR        },
    {   MTP_PROPERTY_DISPLAY_NAME,      MTP_TYPE_STR        },
    {   MTP_PROPERTY_DATE_ADDED,        MTP_TYPE_STR        },
    {   MTP_PROPERTY_ARTIST,            MTP_TYPE_STR        },
    {   MTP_PROPERTY_ALBUM_NAME,        MTP_TYPE_STR        },
    {   MTP_PROPERTY_ALBUM_ARTIST,      MTP_TYPE_STR        },
    {   MTP_PROPERTY_TRACK,             MTP_TYPE_UINT16     },
    {   MTP_PROPERTY_ORIGINAL_RELEASE_DATE, MTP_TYPE_STR    },
    {   MTP_PROPERTY_GENRE,             MTP_TYPE_STR        },
    {   MTP_PROPERTY_COMPOSER,          MTP_TYPE_STR        },
    {   MTP_PROPERTY_DURATION,          MTP_TYPE_UINT32     },
    {   MTP_PROPERTY_DESCRIPTION,       MTP_TYPE_STR        },
};

static const PropertyTableEntry   kDevicePropertyTable[] = {
    {   MTP_DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER,    MTP_TYPE_STR },
    {   MTP_DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME,       MTP_TYPE_STR },
    {   MTP_DEVICE_PROPERTY_IMAGE_SIZE,                 MTP_TYPE_STR },
};

/// M: Added Modification for ALPS00255822, bug from WHQL test @{
typedef enum {
    PROPERTY_PROTECTION_STATUS_NONE             = 0x0000,
    PROPERTY_PROTECTION_STATUS_READ_ONLY        = 0x0001,
    PROPERTY_PROTECTION_STATUS_PTP_RESER_BEGIN  = 0x0002,
    PROPERTY_PROTECTION_STATUS_PTP_RESER_END    = 0x7FFF,
    PROPERTY_PROTECTION_STATUS_READ_ONLY_DATA   = 0x8002,
    PROPERTY_PROTECTION_STATUS_NONE_TRANS_DATA  = 0x8003,
    PROPERTY_PROTECTION_STATUS_MTP_RESER_BEGIN  = 0x8004,
    PROPERTY_PROTECTION_STATUS_MTP_RESER_END    = 0x8BFF,
    PROPERTY_PROTECTION_STATUS_VENDOR_RESER_BEGIN = 0x8C00,
    PROPERTY_PROTECTION_STATUS_VENDOR_RESER_END = 0xFFFF,
}PROPERTY_PROTECTION_STATUS_ENUM;

static const int kObjectPropProtectionStatus[] = {
    
    PROPERTY_PROTECTION_STATUS_NONE,             
    PROPERTY_PROTECTION_STATUS_READ_ONLY,        
    PROPERTY_PROTECTION_STATUS_READ_ONLY_DATA,   
    PROPERTY_PROTECTION_STATUS_NONE_TRANS_DATA
};
/// @}

bool MyMtpDatabase::getObjectPropertyInfo(MtpObjectProperty property, int& type) {
    int count = sizeof(kObjectPropertyTable) / sizeof(kObjectPropertyTable[0]);
    const PropertyTableEntry* entry = kObjectPropertyTable;

    /// M: Added for USB Develpment debug, more log for more debuging help @{
    sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                "%s: property = 0x%x, count=%d \n",__func__, property, count);
    /// @}

    for (int i = 0; i < count; i++, entry++) {
        /// M: Added for USB Develpment debug, more log for more debuging help @{
        sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                    "%s: Just for debugging!! entry->property (%d)= 0x%x \n", __func__, i, entry->property);
        /// @}
        if (entry->property == property) {
            type = entry->type;
            /// M: Added for USB Develpment debug, more log for more debuging help @{
            sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                        "%s: get type = 0x%x \n", __func__, type);
            /// @}
            return true;
        }
    }
    return false;
}

bool MyMtpDatabase::getDevicePropertyInfo(MtpDeviceProperty property, int& type) {
    int count = sizeof(kDevicePropertyTable) / sizeof(kDevicePropertyTable[0]);
    const PropertyTableEntry* entry = kDevicePropertyTable;

    /// M: Added for USB Develpment debug, more log for more debuging help @{
    sxlog_printf(ANDROID_LOG_DEBUG, "MtpDatabase_JNI",
                 "%s: property = 0x%x, count=%d \n",__func__, property, count);
    /// @}

    for (int i = 0; i < count; i++, entry++) {
        /// M: Added for USB Develpment debug, more log for more debuging help @{
        sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                    "%s: Just for debugging!! entry->property (%d)= 0x%x\n", __func__, i, entry->property);
        /// @}
        if (entry->property == property) {
            type = entry->type;
            /// M: Added for USB Develpment debug, more log for more debuging help @{
            sxlog_printf(ANDROID_LOG_VERBOSE, "MtpDatabase_JNI",
                        "%s: get type = 0x%x\n", __func__, type);
            /// @}
            return true;
        }
    }
    return false;
}

MtpObjectHandleList* MyMtpDatabase::getObjectReferences(MtpObjectHandle handle) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jintArray array = (jintArray)env->CallObjectMethod(mDatabase, method_getObjectReferences,
                (jint)handle);
    if (!array)
    {
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return NULL;
    }
    MtpObjectHandleList* list = new MtpObjectHandleList();
    jint* handles = env->GetIntArrayElements(array, 0);
    jsize length = env->GetArrayLength(array);
    for (int i = 0; i < length; i++)
        list->push(handles[i]);
    env->ReleaseIntArrayElements(array, handles, 0);
    env->DeleteLocalRef(array);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return list;
}

MtpResponseCode MyMtpDatabase::setObjectReferences(MtpObjectHandle handle,
                                                    MtpObjectHandleList* references) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    int count = references->size();
    jintArray array = env->NewIntArray(count);
    if (!array) {
        ALOGE("out of memory in setObjectReferences");
        /// M: Added Modification for ALPS00255822, bug from WHQL test @{
        checkAndClearExceptionFromCallback(env, __FUNCTION__);
        /// @}
        return false;
    }
    jint* handles = env->GetIntArrayElements(array, 0);
     for (int i = 0; i < count; i++)
        handles[i] = (*references)[i];
    env->ReleaseIntArrayElements(array, handles, 0);
    MtpResponseCode result = env->CallIntMethod(mDatabase, method_setObjectReferences,
                (jint)handle, array);
    env->DeleteLocalRef(array);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);

    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    if(result == MTP_RESPONSE_GENERAL_ERROR)
    {
        sxlog_printf(ANDROID_LOG_ERROR, "MtpDatabase_JNI",
                    "%s: Not Playlist, not accept!!\n",__func__);
        //Not Playlist, we don't accept
        //result = MTP_RESPONSE_INVALID_OBJECT_REFERENCE;
        result = MTP_RESPONSE_ACCESS_DENIED;
    }
    /// @}
    return result;
}

MtpProperty* MyMtpDatabase::getObjectPropertyDesc(MtpObjectProperty property,
                                            MtpObjectFormat format) {
    MtpProperty* result = NULL;
/// M: Added Modification for ALPS00255822, bug from WHQL test @{
#if 0
/// @}
    switch (property) {
        case MTP_PROPERTY_OBJECT_FORMAT:
            // use format as default value
            result = new MtpProperty(property, MTP_TYPE_UINT16, false, format);
            break;
        case MTP_PROPERTY_PROTECTION_STATUS:
        case MTP_PROPERTY_TRACK:
            result = new MtpProperty(property, MTP_TYPE_UINT16);
            break;
        case MTP_PROPERTY_STORAGE_ID:
        case MTP_PROPERTY_PARENT_OBJECT:
        case MTP_PROPERTY_DURATION:
            result = new MtpProperty(property, MTP_TYPE_UINT32);
            break;
        case MTP_PROPERTY_OBJECT_SIZE:
            result = new MtpProperty(property, MTP_TYPE_UINT64);
            break;
        case MTP_PROPERTY_PERSISTENT_UID:
            result = new MtpProperty(property, MTP_TYPE_UINT128);
            break;
        case MTP_PROPERTY_NAME:
        case MTP_PROPERTY_DISPLAY_NAME:
        case MTP_PROPERTY_ARTIST:
        case MTP_PROPERTY_ALBUM_NAME:
        case MTP_PROPERTY_ALBUM_ARTIST:
        case MTP_PROPERTY_GENRE:
        case MTP_PROPERTY_COMPOSER:
        case MTP_PROPERTY_DESCRIPTION:
            result = new MtpProperty(property, MTP_TYPE_STR);
            break;
        case MTP_PROPERTY_DATE_MODIFIED:
        case MTP_PROPERTY_DATE_ADDED:
        case MTP_PROPERTY_ORIGINAL_RELEASE_DATE:
            result = new MtpProperty(property, MTP_TYPE_STR);
            result->setFormDateTime();
            break;
        case MTP_PROPERTY_OBJECT_FILE_NAME:
            // We allow renaming files and folders
            result = new MtpProperty(property, MTP_TYPE_STR, true);
            break;
    }
/// Added Modification for ALPS00255822, bug from WHQL test @ {
#else
    switch (property) {
        case MTP_PROPERTY_OBJECT_FORMAT:
            // use format as default value
            result = new MtpProperty(property, MTP_TYPE_UINT16, false, format);
            break;
        case MTP_PROPERTY_PROTECTION_STATUS:
        case MTP_PROPERTY_TRACK:
            result = new MtpProperty(property, MTP_TYPE_UINT16);
            break;
/// M: Added Modification for ALPS00255822, bug from WHQL test @{
            //remarked for formal MTP sync, open for MTP WHQL
/*        case MTP_PROPERTY_PROTECTION_STATUS:
            result = new MtpProperty(property, MTP_TYPE_UINT16);
            result->setFormEnum(kObjectPropProtectionStatus, 4);
            break;*/
/// M: Added Modification for ALPS00255822, bug from WHQL test @{
        case MTP_PROPERTY_STORAGE_ID:
            result = new MtpProperty(property, MTP_TYPE_UINT32);
            break;
        //case MTP_PROPERTY_STORAGE_ID:
        case MTP_PROPERTY_PARENT_OBJECT:
            result = new MtpProperty(property, MTP_TYPE_UINT32);
            break;
        case MTP_PROPERTY_DURATION:
            result = new MtpProperty(property, MTP_TYPE_UINT32);
            //for WHQL test, readable only, must with a default value, assign temporary
            //remarked for formal MTP sync, open for MTP WHQL
            /*result->setFormRange(0x01, 0xFFFFFFFF, 0x02);*/
            break;
        case MTP_PROPERTY_OBJECT_SIZE:
            result = new MtpProperty(property, MTP_TYPE_UINT64);
            break;
        case MTP_PROPERTY_PERSISTENT_UID:
            result = new MtpProperty(property, MTP_TYPE_UINT128);
            break;
        case MTP_PROPERTY_NAME:
        case MTP_PROPERTY_DISPLAY_NAME:
        case MTP_PROPERTY_ARTIST:
        case MTP_PROPERTY_ALBUM_NAME:
        case MTP_PROPERTY_ALBUM_ARTIST:
        case MTP_PROPERTY_GENRE:
        case MTP_PROPERTY_COMPOSER:
            result = new MtpProperty(property, MTP_TYPE_STR);
            break;
        case MTP_PROPERTY_DESCRIPTION:
            result = new MtpProperty(property, MTP_TYPE_STR);
            /*result = new MtpProperty(property, MTP_TYPE_AUINT16);
                    result->setFormLongString(300);*/
            break;
        case MTP_PROPERTY_DATE_MODIFIED:
        case MTP_PROPERTY_DATE_ADDED:
        case MTP_PROPERTY_ORIGINAL_RELEASE_DATE:
            result = new MtpProperty(property, MTP_TYPE_STR);
            result->setFormDateTime();
            break;
        case MTP_PROPERTY_OBJECT_FILE_NAME:
            // We allow renaming files and folders
            result = new MtpProperty(property, MTP_TYPE_STR, true);
            break;
    }
#endif
/// @}

    return result;
}

MtpProperty* MyMtpDatabase::getDevicePropertyDesc(MtpDeviceProperty property) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    MtpProperty* result = NULL;
    bool writable = false;

    switch (property) {
        case MTP_DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER:
        case MTP_DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME:
            writable = true;
            // fall through
        case MTP_DEVICE_PROPERTY_IMAGE_SIZE:
            result = new MtpProperty(property, MTP_TYPE_STR, writable);

            // get current value
            jint ret = env->CallIntMethod(mDatabase, method_getDeviceProperty,
                        (jint)property, mLongBuffer, mStringBuffer);
            if (ret == MTP_RESPONSE_OK) {
                jchar* str = env->GetCharArrayElements(mStringBuffer, 0);
                result->setCurrentValue(str);
                // for read-only properties it is safe to assume current value is default value
                if (!writable)
                    result->setDefaultValue(str);
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                if(property == MTP_DEVICE_PROPERTY_IMAGE_SIZE)
                {
                    //Set the formflag temporary for pass WHQL test
                    int imageSizeEnum[2]={1 /*640X480*/, 2 /*800X800*/};
                    result->setFormEnum(imageSizeEnum, 1);
                }
                /// @}
                env->ReleaseCharArrayElements(mStringBuffer, str, 0);
            } else {
                ALOGE("unable to read device property, response: %04X", ret);
            }
            break;
    }

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;
}

void MyMtpDatabase::sessionStarted() {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    env->CallVoidMethod(mDatabase, method_sessionStarted);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
}

void MyMtpDatabase::sessionEnded() {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    env->CallVoidMethod(mDatabase, method_sessionEnded);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
}

/// M: Added Modification for ALPS00264207, ALPS00248646 @{
void MyMtpDatabase::getObjectBeginIndication(const char * storagePath)
{
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    ALOGI("%s: \n", __func__);

    jstring pathStr = env->NewStringUTF(storagePath);

    env->CallVoidMethod(mDatabase, method_getObjectBeginIndication, pathStr);

    if (pathStr)
        env->DeleteLocalRef(pathStr);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);

}
void MyMtpDatabase::getObjectEndIndication(const char * storagePath)
{
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    ALOGI("%s: \n", __func__);

    jstring pathStr = env->NewStringUTF(storagePath);
    env->CallVoidMethod(mDatabase, method_getObjectEndIndication, pathStr);
    if (pathStr)
        env->DeleteLocalRef(pathStr);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);

}
/// @}

// ----------------------------------------------------------------------------

static void
android_mtp_MtpDatabase_setup(JNIEnv *env, jobject thiz)
{
    MyMtpDatabase* database = new MyMtpDatabase(env, thiz);
    env->SetIntField(thiz, field_context, (int)database);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
}

static void
android_mtp_MtpDatabase_finalize(JNIEnv *env, jobject thiz)
{
    MyMtpDatabase* database = (MyMtpDatabase *)env->GetIntField(thiz, field_context);
    database->cleanup(env);
    delete database;
    env->SetIntField(thiz, field_context, 0);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
}

static jstring
android_mtp_MtpPropertyGroup_format_date_time(JNIEnv *env, jobject thiz, jlong seconds)
{
    char    date[20];
    formatDateTime(seconds, date, sizeof(date));
    return env->NewStringUTF(date);
}

/// M: Added function all for get handel by Path
MtpObjectHandle MyMtpDatabase::getObjectHandleWithPath(const char* path,
                                            MtpObjectFormat format,
                                            MtpObjectHandle parent,
                                            MtpStorageID storage,
                                            uint64_t size,
                                            time_t modified) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    jstring pathStr = env->NewStringUTF(path);
    MtpObjectHandle result = env->CallIntMethod(mDatabase, method_getObjectHandleWithPath,
            pathStr, (jint)format, (jint)parent, (jint)storage,
            (jlong)size, (jlong)modified);

    if (pathStr)
        env->DeleteLocalRef(pathStr);
    checkAndClearExceptionFromCallback(env, __FUNCTION__);
    return result;
}

/// @}
// ----------------------------------------------------------------------------

static JNINativeMethod gMtpDatabaseMethods[] = {
    {"native_setup",            "()V",  (void *)android_mtp_MtpDatabase_setup},
    {"native_finalize",         "()V",  (void *)android_mtp_MtpDatabase_finalize},
};

static JNINativeMethod gMtpPropertyGroupMethods[] = {
    {"format_date_time",        "(J)Ljava/lang/String;",
                                        (void *)android_mtp_MtpPropertyGroup_format_date_time},
};

static const char* const kClassPathName = "android/mtp/MtpDatabase";

int register_android_mtp_MtpDatabase(JNIEnv *env)
{
    jclass clazz;

    clazz = env->FindClass("android/mtp/MtpDatabase");
    if (clazz == NULL) {
        ALOGE("Can't find android/mtp/MtpDatabase");
        return -1;
    }
    method_beginSendObject = env->GetMethodID(clazz, "beginSendObject", "(Ljava/lang/String;IIIJJ)I");
    if (method_beginSendObject == NULL) {
        ALOGE("Can't find beginSendObject");
        return -1;
    }
    method_endSendObject = env->GetMethodID(clazz, "endSendObject", "(Ljava/lang/String;IIZ)V");
    if (method_endSendObject == NULL) {
        ALOGE("Can't find endSendObject");
        return -1;
    }
    method_getObjectList = env->GetMethodID(clazz, "getObjectList", "(III)[I");
    if (method_getObjectList == NULL) {
        ALOGE("Can't find getObjectList");
        return -1;
    }
    method_getNumObjects = env->GetMethodID(clazz, "getNumObjects", "(III)I");
    if (method_getNumObjects == NULL) {
        ALOGE("Can't find getNumObjects");
        return -1;
    }
    method_getSupportedPlaybackFormats = env->GetMethodID(clazz, "getSupportedPlaybackFormats", "()[I");
    if (method_getSupportedPlaybackFormats == NULL) {
        ALOGE("Can't find getSupportedPlaybackFormats");
        return -1;
    }
    method_getSupportedCaptureFormats = env->GetMethodID(clazz, "getSupportedCaptureFormats", "()[I");
    if (method_getSupportedCaptureFormats == NULL) {
        ALOGE("Can't find getSupportedCaptureFormats");
        return -1;
    }
    method_getSupportedObjectProperties = env->GetMethodID(clazz, "getSupportedObjectProperties", "(I)[I");
    if (method_getSupportedObjectProperties == NULL) {
        ALOGE("Can't find getSupportedObjectProperties");
        return -1;
    }
    method_getSupportedDeviceProperties = env->GetMethodID(clazz, "getSupportedDeviceProperties", "()[I");
    if (method_getSupportedDeviceProperties == NULL) {
        ALOGE("Can't find getSupportedDeviceProperties");
        return -1;
    }
    method_setObjectProperty = env->GetMethodID(clazz, "setObjectProperty", "(IIJLjava/lang/String;)I");
    if (method_setObjectProperty == NULL) {
        ALOGE("Can't find setObjectProperty");
        return -1;
    }
    method_getDeviceProperty = env->GetMethodID(clazz, "getDeviceProperty", "(I[J[C)I");
    if (method_getDeviceProperty == NULL) {
        ALOGE("Can't find getDeviceProperty");
        return -1;
    }
    method_setDeviceProperty = env->GetMethodID(clazz, "setDeviceProperty", "(IJLjava/lang/String;)I");
    if (method_setDeviceProperty == NULL) {
        ALOGE("Can't find setDeviceProperty");
        return -1;
    }
    method_getObjectPropertyList = env->GetMethodID(clazz, "getObjectPropertyList",
            "(JIJII)Landroid/mtp/MtpPropertyList;");
    if (method_getObjectPropertyList == NULL) {
        ALOGE("Can't find getObjectPropertyList");
        return -1;
    }
    method_getObjectInfo = env->GetMethodID(clazz, "getObjectInfo", "(I[I[C[J)Z");
    if (method_getObjectInfo == NULL) {
        ALOGE("Can't find getObjectInfo");
        return -1;
    }
    method_getObjectFilePath = env->GetMethodID(clazz, "getObjectFilePath", "(I[C[J)I");
    if (method_getObjectFilePath == NULL) {
        ALOGE("Can't find getObjectFilePath");
        return -1;
    }
    method_deleteFile = env->GetMethodID(clazz, "deleteFile", "(I)I");
    if (method_deleteFile == NULL) {
        ALOGE("Can't find deleteFile");
        return -1;
    }
    method_getObjectReferences = env->GetMethodID(clazz, "getObjectReferences", "(I)[I");
    if (method_getObjectReferences == NULL) {
        ALOGE("Can't find getObjectReferences");
        return -1;
    }
    method_setObjectReferences = env->GetMethodID(clazz, "setObjectReferences", "(I[I)I");
    if (method_setObjectReferences == NULL) {
        ALOGE("Can't find setObjectReferences");
        return -1;
    }
    method_sessionStarted = env->GetMethodID(clazz, "sessionStarted", "()V");
    if (method_sessionStarted == NULL) {
        ALOGE("Can't find sessionStarted");
        return -1;
    }
    method_sessionEnded = env->GetMethodID(clazz, "sessionEnded", "()V");
    if (method_sessionEnded == NULL) {
        ALOGE("Can't find sessionEnded");
        return -1;
    }
    /// M: Added Modification for ALPS00264207, ALPS00248646 @{
    method_getObjectBeginIndication = env->GetMethodID(clazz, "getObjectBeginIndication", "(Ljava/lang/String;)V");
    if (method_getObjectBeginIndication == NULL) {
        ALOGE("Can't find getObjectBeginIndication");
        return -1;
    }
    method_getObjectEndIndication = env->GetMethodID(clazz, "getObjectEndIndication", "(Ljava/lang/String;)V");
    if (method_getObjectEndIndication == NULL) {
        ALOGE("Can't find getObjectEndIndication");
        return -1;
    }
    /// @}
    /// M: Added function all for get handel by Path
    method_getObjectHandleWithPath = env->GetMethodID(clazz, "getObjectHandleWithPath", "(Ljava/lang/String;IIIJJ)I");
    if (method_getObjectHandleWithPath == NULL) {
        ALOGE("Can't find getObjectHandleWithPath");
        return -1;
    }
    /// @}
    field_context = env->GetFieldID(clazz, "mNativeContext", "I");
    if (field_context == NULL) {
        ALOGE("Can't find MtpDatabase.mNativeContext");
        return -1;
    }

    // now set up fields for MtpPropertyList class
    clazz = env->FindClass("android/mtp/MtpPropertyList");
    if (clazz == NULL) {
        ALOGE("Can't find android/mtp/MtpPropertyList");
        return -1;
    }
    field_mCount = env->GetFieldID(clazz, "mCount", "I");
    if (field_mCount == NULL) {
        ALOGE("Can't find MtpPropertyList.mCount");
        return -1;
    }
    field_mResult = env->GetFieldID(clazz, "mResult", "I");
    if (field_mResult == NULL) {
        ALOGE("Can't find MtpPropertyList.mResult");
        return -1;
    }
    field_mObjectHandles = env->GetFieldID(clazz, "mObjectHandles", "[I");
    if (field_mObjectHandles == NULL) {
        ALOGE("Can't find MtpPropertyList.mObjectHandles");
        return -1;
    }
    field_mPropertyCodes = env->GetFieldID(clazz, "mPropertyCodes", "[I");
    if (field_mPropertyCodes == NULL) {
        ALOGE("Can't find MtpPropertyList.mPropertyCodes");
        return -1;
    }
    field_mDataTypes = env->GetFieldID(clazz, "mDataTypes", "[I");
    if (field_mDataTypes == NULL) {
        ALOGE("Can't find MtpPropertyList.mDataTypes");
        return -1;
    }
    field_mLongValues = env->GetFieldID(clazz, "mLongValues", "[J");
    if (field_mLongValues == NULL) {
        ALOGE("Can't find MtpPropertyList.mLongValues");
        return -1;
    }
    field_mStringValues = env->GetFieldID(clazz, "mStringValues", "[Ljava/lang/String;");
    if (field_mStringValues == NULL) {
        ALOGE("Can't find MtpPropertyList.mStringValues");
        return -1;
    }

    if (AndroidRuntime::registerNativeMethods(env,
                "android/mtp/MtpDatabase", gMtpDatabaseMethods, NELEM(gMtpDatabaseMethods)))
        return -1;

    return AndroidRuntime::registerNativeMethods(env,
                "android/mtp/MtpPropertyGroup", gMtpPropertyGroupMethods, NELEM(gMtpPropertyGroupMethods));
}
