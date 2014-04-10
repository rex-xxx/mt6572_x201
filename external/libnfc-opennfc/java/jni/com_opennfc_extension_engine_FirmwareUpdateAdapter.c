/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "open_nfc_extension.h"

/* maximum number of characters that can be returned by WNFCControllerGetFirmwareProperty() for the firmware version */
#define VERSION_LENGTH 100

/* a structure to provide parameters to be used in firmware update callback function */
typedef struct
{
	/* semaphore for the end of operation */
	sem_t sem;
	/* buffer to new firmware binary data */
	uint8_t* pUpdateBuffer;
	/* length of the firmware binary data */
	uint32_t nUpdateBufferLength;
	/* status of the operation */
	W_ERROR status;
} firmwareUpdateCallbackParams_t;

/* sends notification about f/w update completion */
static void firmwareUpdateCompletion(firmwareUpdateCallbackParams_t* pCallbackParameter, W_ERROR status)
{
	LogInformation("firmwareUpdateCompletion: status=0x%X", status);
	pCallbackParameter->status = status;
	SEMAPHORE_POST(&pCallbackParameter->sem);
}

static void firmwareUpdateCallback(void *pCallbackParameter, W_ERROR nResult)
{
	firmwareUpdateCallbackParams_t* pFWUpdateCallbackParams = (firmwareUpdateCallbackParams_t*) pCallbackParameter;
	if (nResult != W_SUCCESS)
	{
		LogError("NFC firmware update failed (0x%X)", nResult);
	}
	else
	{
		LogInformation("NFC firmware is updated");
	}
	firmwareUpdateCompletion(pFWUpdateCallbackParams, nResult);
}

static void resetCallback(void *pCallbackParameter, W_ERROR nResult)
{
	firmwareUpdateCallbackParams_t* pFWUpdateCallbackParams = (firmwareUpdateCallbackParams_t*) pCallbackParameter;

	LogInformation("NFC chip reset callback: error=0x%X", nResult);
	/* start f/w update */
	WNFCControllerFirmwareUpdate(firmwareUpdateCallback, pFWUpdateCallbackParams, pFWUpdateCallbackParams->pUpdateBuffer,
			pFWUpdateCallbackParams->nUpdateBufferLength, W_NFCC_MODE_ACTIVE);

}

static jint com_opennfc_extension_engine_FirmwareUpdateAdapter_updateNative(JNIEnv * e, jobject o, jbyteArray data)
{
	JNIEnv env = *e;
	firmwareUpdateCallbackParams_t cbParams;

	if (SEMAPHORE_CREATE(&cbParams.sem) == -1) {
		return W_ERROR_PROGRAMMING;
	}

	cbParams.pUpdateBuffer = (uint8_t*) env->GetByteArrayElements(e, data, NULL);
	cbParams.nUpdateBufferLength = env->GetArrayLength(e, data);
	WNFCControllerReset(resetCallback, &cbParams, W_NFCC_MODE_MAINTENANCE);

	/* wait for the end of operation */
	SEMAPHORE_WAIT(&cbParams.sem);

	SEMAPHORE_DESTROY(&cbParams.sem);
	env->ReleaseByteArrayElements(e, data, (jbyte*) cbParams.pUpdateBuffer, JNI_ABORT);
	return (jint) cbParams.status;
}

static jstring com_opennfc_extension_engine_FirmwareUpdateAdapter_getVersionNative(JNIEnv * e, jobject o, jbyteArray data)
{

	JNIEnv env = *e;
	jboolean isCopy;
	jstring version = NULL;
	uint8_t* pUpdateBuffer = (uint8_t*) env->GetByteArrayElements(e, data, &isCopy);

	char16_t valueBuffer[VERSION_LENGTH];
	uint32_t  nValueLength = 0;

	W_ERROR error = WNFCControllerGetFirmwareProperty(pUpdateBuffer, env->GetArrayLength(e, data),
			W_NFCC_PROP_FIRMWARE_VERSION, valueBuffer, VERSION_LENGTH, &nValueLength);

	LogInformation("getVersionNative(): error=0x%X, nValueLength=%d, ", error, nValueLength);

	env->ReleaseByteArrayElements(e, data, (jbyte*) pUpdateBuffer, JNI_ABORT);
	if (error == W_SUCCESS)
	{
		version = env->NewString(e, (jchar*) &valueBuffer, nValueLength);
	} else
	{
		LogError("Can't read firmware version (error=0x%X)", error);
	}
	return version;
}

static JNINativeMethod gMethods[] =
{
	{"updateNative",	"([B)I",	(void*) com_opennfc_extension_engine_FirmwareUpdateAdapter_updateNative},
	{"getVersionNative", "([B)Ljava/lang/String;", (void*) com_opennfc_extension_engine_FirmwareUpdateAdapter_getVersionNative},
};

int initalize_com_opennfc_extension_engine_FirmwareUpdateAdapter(JNIEnv * environmentJNI)
{
	JNIEnv env = *environmentJNI;

	const char* className = "com/opennfc/extension/engine/FirmwareUpdateAdapter";
	jclass classTemporary = env->FindClass(environmentJNI, className);
	return jniRegisterNativeMethods(environmentJNI, className, gMethods, NELEM(gMethods));
}

void deinitalize_com_opennfc_extension_engine_FirmwareUpdateAdapter(JNIEnv * environmentJNI)
{
}
