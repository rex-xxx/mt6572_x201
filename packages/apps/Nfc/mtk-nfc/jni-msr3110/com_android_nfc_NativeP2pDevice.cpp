
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
/*
 * Modified by: Inside Secure
 */


#include <semaphore.h>
#include <errno.h>

#include "com_android_nfc.h"

//extern uint8_t device_connected_flag;


namespace android {

extern bool_t isServerSoketValid(uint32_t handle);
extern bool_t isClientSoketValid(uint32_t handle);

extern jbyteArray serverSocketTransceive(JNIEnv * jniEnvironment, uint32_t handle, jbyteArray data);
extern jbyteArray serverSocketReceive(JNIEnv * jniEnvironment, uint32_t handle);
extern jboolean serverSocketSend(JNIEnv * jniEnvironment, uint32_t handle, jbyteArray data);
extern jboolean serverSocketDisconnect(uint32_t handle);

extern jbyteArray socketTransceive(JNIEnv * jniEnvironment, uint32_t handle, jbyteArray data);
extern jbyteArray socketReceive(JNIEnv * jniEnvironment, uint32_t handle);
extern jboolean socketSend(JNIEnv * jniEnvironment, uint32_t handle, jbyteArray data);
extern jboolean socketDisconnect(uint32_t handle);

#define MAX_MIU_SIZE 256

bool_t internalP2PWrite(JNIEnv * jniEnvironment, W_HANDLE handle, jbyteArray data)
{
	uint8_t * dataBuffer = (uint8_t *)jniEnvironment->GetByteArrayElements(data, NULL);
	uint32_t dataLength = (uint32_t)jniEnvironment->GetArrayLength(data);

	W_ERROR error = WP2PWriteSync
	(
		handle,
		dataBuffer, dataLength
	);

	jniEnvironment->ReleaseByteArrayElements(data,(jbyte *)dataBuffer, JNI_ABORT);
	if(error != W_SUCCESS)
	{
		LogError("internalP2PWrite returns error=%x", error);
		return W_FALSE;
	}
	return W_TRUE;
}

jbyteArray internalReadP2P(JNIEnv * jniEnvironment, W_HANDLE handle)
{
	uint32_t dataLength = 0;
	uint8_t dataBuffer[MAX_MIU_SIZE];

	W_ERROR error = WP2PReadSync
	(
		handle,
		dataBuffer, MAX_MIU_SIZE,
		& dataLength
	);
	if(error != W_SUCCESS)
	{
		LogError("internalReadP2P returns error=%x", error);
		return NULL;
	}
	jbyteArray result = jniEnvironment->NewByteArray(dataLength);
	jniEnvironment->SetByteArrayRegion
	(
		result, 0,
		dataLength,
		(jbyte *)dataBuffer
	);

	return result;
}

static jboolean com_android_nfc_NativeP2pDevice_doConnect(JNIEnv *e, jobject o)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e, o);
	bool_t canUse = W_FALSE;

    jclass target_cls = NULL;
    jfieldID f;
	if(IS_SERVER_SOCKET(handle))
	{
		canUse = isServerSoketValid(handle);
	}
	else if(IS_CLIENT_SOCKET(handle))
	{
		canUse = isClientSoketValid(handle);
	}
	if(canUse==W_FALSE)
	{
		return JNI_FALSE;
	}
    target_cls = e->GetObjectClass(o);

    f = e->GetFieldID(target_cls, "mGeneralBytes", "[B");

	jbyteArray generalBytes = NULL;

    generalBytes = e->NewByteArray(GENERAL_BYTES_LENGTH);

    e->SetByteArrayRegion(generalBytes, 0, GENERAL_BYTES_LENGTH, (jbyte *)GENERAL_BYTES);

    e->SetObjectField(o, f, generalBytes);
	return JNI_TRUE;
}

static jboolean com_android_nfc_NativeP2pDevice_doDisconnect(JNIEnv *e, jobject o)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e, o);
	bool_t canUse = W_FALSE;

    jclass target_cls = NULL;
    jfieldID f;
	if(IS_SERVER_SOCKET(handle))
	{
		return serverSocketDisconnect(handle);
	}
	else if(IS_CLIENT_SOCKET(handle))
	{
		return socketDisconnect(handle);
	}
	return JNI_FALSE;
}

static jbyteArray com_android_nfc_NativeP2pDevice_doTransceive(JNIEnv *e,
   jobject o, jbyteArray data)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e, o);
	bool_t canUse = W_FALSE;

    jclass target_cls = NULL;
    jfieldID f;
	if(IS_SERVER_SOCKET(handle))
	{
		return serverSocketTransceive(e, handle, data);
	}
	else if(IS_CLIENT_SOCKET(handle))
	{
		return socketTransceive(e, handle, data);
	}
	return NULL;
}


static jbyteArray com_android_nfc_NativeP2pDevice_doReceive(
   JNIEnv *e, jobject o)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e, o);
	bool_t canUse = W_FALSE;

    jclass target_cls = NULL;
    jfieldID f;
	if(IS_SERVER_SOCKET(handle))
	{
		return serverSocketReceive(e, handle);
	}
	else if(IS_CLIENT_SOCKET(handle))
	{
		return socketReceive(e, handle);
	}
	return NULL;
}

static jboolean com_android_nfc_NativeP2pDevice_doSend(
   JNIEnv *e, jobject o, jbyteArray data)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e, o);
	bool_t canUse = W_FALSE;

    jclass target_cls = NULL;
    jfieldID f;
	if(IS_SERVER_SOCKET(handle))
	{
		return serverSocketSend(e, handle, data);
	}
	else if(IS_CLIENT_SOCKET(handle))
	{
		return socketSend(e, handle, data);
	}
	return JNI_FALSE;
}

/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] =
{
   {"doConnect", "()Z",
      (void *)com_android_nfc_NativeP2pDevice_doConnect},
   {"doDisconnect", "()Z",
      (void *)com_android_nfc_NativeP2pDevice_doDisconnect},
   {"doTransceive", "([B)[B",
      (void *)com_android_nfc_NativeP2pDevice_doTransceive},
   {"doReceive", "()[B",
      (void *)com_android_nfc_NativeP2pDevice_doReceive},
   {"doSend", "([B)Z",
      (void *)com_android_nfc_NativeP2pDevice_doSend},
};

int register_com_android_nfc_NativeP2pDevice(JNIEnv *e)
{
   return jniRegisterNativeMethods(e,
      "com/android/nfc/dhimpl/NativeP2pDevice",
      gMethods, NELEM(gMethods));
}

} // namepspace android
