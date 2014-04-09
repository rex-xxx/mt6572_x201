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

namespace android
{

static jboolean com_android_nfc_NativeLlcpConnectionlessSocket_doSendTo(JNIEnv *e, jobject o, jint nsap, jbyteArray data)
{
	W_HANDLE handle = (W_HANDLE) nfc_jni_get_nfc_socket_handle(e, o);
	jboolean result = JNI_TRUE;

	if (data == NULL) {
		LogError("Can't send null p2p data via connectionless socket");
		return JNI_FALSE;
	}
	uint32_t nSendBufferLength = (uint32_t) e->GetArrayLength(data);
	if (nSendBufferLength == 0) {
		LogError("Can't send 0 bytes as p2p data via connectionless socket");
		return JNI_FALSE;
	}
	uint8_t* pSendBuffer = (uint8_t*) e->GetByteArrayElements(data, NULL);

	W_ERROR error = WP2PSendToSync(handle, (uint8_t) nsap, pSendBuffer, nSendBufferLength);
	if (error != W_SUCCESS)
	{
		LogError("Can't send p2p data via connectionless socket (error = 0x%X)", error);
		result = JNI_FALSE;
	}

	if (pSendBuffer != NULL)
	{
		e->ReleaseByteArrayElements(data, (jbyte*) pSendBuffer, JNI_ABORT);
	}
	return result;
}

static jobject com_android_nfc_NativeLlcpConnectionlessSocket_doReceiveFrom(JNIEnv *e, jobject o, jint linkMiu)
{
	jobject llcpPacket = NULL;
	jclass clsLlcpPacket;
	jfieldID f;
	W_HANDLE handle = (W_HANDLE) nfc_jni_get_nfc_socket_handle(e, o);
	uint8_t receptionBuffer[linkMiu];
	uint8_t sap;
	uint32_t nDataLength;
	jbyteArray receivedData = NULL;

	W_ERROR error = WP2PRecvFromSync(handle, receptionBuffer, linkMiu, &sap, &nDataLength);
	if (error != W_SUCCESS)
	{
		LogError("Can't receive p2p data via connectionless socket (error = 0x%X)", error);
		return NULL;
	}

	clsLlcpPacket = e->FindClass("com/android/nfc/LlcpPacket");
	if (clsLlcpPacket == NULL)
	{
		LogError("LlcpPacket get object class error");
		return NULL;
	}

	jmethodID ctor = e->GetMethodID(clsLlcpPacket, "<init>", "()V");

	llcpPacket = e->NewObject(clsLlcpPacket, ctor);
	if (llcpPacket == NULL)
	{
		LogError("LlcpPacket object creation error");
		return NULL;
	}

	/* Set Llcp Packet remote SAP */
	f = e->GetFieldID(clsLlcpPacket, "mRemoteSap", "I");
	e->SetIntField(llcpPacket, f, (jbyte) sap);

	/* Set Llcp Packet Buffer */
	f = e->GetFieldID(clsLlcpPacket, "mDataBuffer", "[B");
	receivedData = e->NewByteArray(nDataLength);
	e->SetByteArrayRegion(receivedData, 0, nDataLength, (jbyte *) receptionBuffer);
	e->SetObjectField(llcpPacket, f, receivedData);

	if (receivedData != NULL)
	{
		e->ReleaseByteArrayElements(receivedData, (jbyte*) receptionBuffer, 0);
	}

	return llcpPacket;
}

static jboolean com_android_nfc_NativeLlcpConnectionlessSocket_doClose(JNIEnv *e, jobject o)
{
	W_HANDLE handle = (W_HANDLE) nfc_jni_get_nfc_socket_handle(e, o);
	WBasicCloseHandle(handle);
	return JNI_TRUE;
}

/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] =
{
{ "doSendTo", "(I[B)Z", (void *) com_android_nfc_NativeLlcpConnectionlessSocket_doSendTo },

{ "doReceiveFrom", "(I)Lcom/android/nfc/LlcpPacket;", (void *) com_android_nfc_NativeLlcpConnectionlessSocket_doReceiveFrom },

{ "doClose", "()Z", (void *) com_android_nfc_NativeLlcpConnectionlessSocket_doClose }, };

int register_com_android_nfc_NativeLlcpConnectionlessSocket(JNIEnv *e)
{
	return jniRegisterNativeMethods(e, "com/android/nfc/dhimpl/NativeLlcpConnectionlessSocket", gMethods, NELEM(gMethods));
}

} // android namespace
