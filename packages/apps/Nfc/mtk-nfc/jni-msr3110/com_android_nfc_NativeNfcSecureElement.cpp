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

#include "com_android_nfc.h"

/* maximum number of secure elements */
#define MAX_SE_NUMBER 3

/* the maximum buffer size to receive the answer of a transceive operation */
#define RECEIVE_APDU_BUFFER_MAX_LENGTH 	1024

/* number of available Secure Elements */
uint32_t seNumber = 0;

/* Secure Elements' information */
tWSEInfoEx seInfo[MAX_SE_NUMBER];

/* a flag of Secure Element initialization: it's set when the data about available Secure Elements is collected */
bool_t seInitialized = W_FALSE;

/* identifier of the Secure Element to be used for NativeNfcSecureElement communications */
uint32_t defaultSEId = UNDEFINED;

/* identifier of the UICC */
uint32_t uiccId = UNDEFINED;

/* SE connection handle */
W_HANDLE seConnection = 0;

namespace android {

W_ERROR getSecureElementList()
{
	LogInformation("getSecureElementList");

	W_ERROR nError;
	if ((nError = WNFCControllerGetIntegerProperty(W_NFCC_PROP_SE_NUMBER, &seNumber)) == W_SUCCESS)
	{
		LogInformation("getSecureElementList(): seNumber=%d", seNumber);

	} else
	{
		LogError("Can't get Secure Elements number (err=0x%X)", nError);
		return nError;
	}
	if (seNumber > MAX_SE_NUMBER)
	{
		LogWarning("Too many Secure Elements (%d), retrieve only %d", seNumber, MAX_SE_NUMBER);
		seNumber = MAX_SE_NUMBER;
	}
	for(uint32_t index = 0; index < seNumber; index++)
	{
		W_ERROR seError;
		if ((seError = WSEGetInfoEx(index, &seInfo[index])) != W_SUCCESS)
		{
			nError = seError;
			break;
		} else {
			tWSEInfoEx* pInfo = &seInfo[index];
			LogInformation("getSecureElementList(): SE #%d: protocols=0x%X, capabilities=0x%X",
					pInfo->nSlotIdentifier,
					pInfo->nSupportedProtocols, pInfo->nCapabilities);
			if ((pInfo->nCapabilities & W_SE_FLAG_COMMUNICATION) && (defaultSEId == UNDEFINED))
			{
				defaultSEId = index;
				LogInformation("getSecureElementList(): defaultSEId=%d, capabilities=0x%X", index,
						pInfo->nCapabilities);
			}
			if ((pInfo->nCapabilities & W_SE_FLAG_UICC) && (uiccId == UNDEFINED))
			{
				uiccId = index;
				LogInformation("getSecureElementList(): uiccId=%d, capabilities=0x%X", index,
						pInfo->nCapabilities);
			}
		}
	}

	if (defaultSEId == UNDEFINED)
	{
		LogWarning("Default Secure Element can't be selected");
	}
	return nError;
}

/* Gets identifier of the UICC */
uint32_t getUicc()
{
	if (uiccId == UNDEFINED)
	{
		getSecureElementList();
		if (uiccId == UNDEFINED)
		{
			LogWarning("UICC can't be detected");
		}
	}
	return uiccId;
}

/* Gets the protocols supported by the secure element */
uint32_t getSupportedProtocols(uint32_t slotId)
{
	if (slotId >= seNumber) {
		LogError("getSupportedProtocols(): wrong slot id (%d)", slotId);
		return 0;
	}
	return seInfo[slotId].nSupportedProtocols;
}

/* get Secure Elements' information */
tWSEInfoEx* getSecureElementInfo(uint32_t slotId)
{
	tWSEInfoEx* pInfo = NULL;
	if (slotId < seNumber) {
		pInfo = &seInfo[slotId];
	}
	else
	{
		LogError("getSecureElementInfo(): wrong slot id (%d)", slotId);
	}
	return pInfo;
}

static jint com_android_nfc_NativeNfcSecureElement_doOpenSecureElementConnection(JNIEnv *e, jobject o)
{
	LogInformation("NativeNfcSecureElement.doOpenSecureElementConnection()");
	W_HANDLE hChannel = 0;
	W_ERROR error = -1;
	if (seInitialized == W_FALSE)
	{
		getSecureElementList();
		seInitialized = W_TRUE;
	}
	if (defaultSEId != UNDEFINED)
	{
		error = WSEOpenConnectionSync(defaultSEId, W_TRUE, &seConnection);
		LogInformation("NativeNfcSecureElement.doOpenSecureElementConnection(): opened connection to SE: handle=%d", seConnection);
		if (error == W_SUCCESS)
		{
			error = W7816OpenChannelSync(seConnection, W_7816_CHANNEL_TYPE_RAW, NULL, 0, &hChannel);
			if (error != W_SUCCESS)
			{
				LogError("NativeNfcSecureElement.doOpenSecureElementConnection(): can't open Channel to SE: error=0x%X", error);
			}
		}else
		{
			LogError("NativeNfcSecureElement.doOpenSecureElementConnection(): can't open connection to SE: error=0x%X", error);
		}
	}
	else
	{
		LogWarning("NativeNfcSecureElement.doOpenSecureElementConnection(): default Secure Element is not set");
	}

	LogInformation("openSecureElementConnection - returned hChannel: %d", hChannel);
	return (jint) hChannel;
}

static jboolean com_android_nfc_NativeNfcSecureElement_doDisconnect(JNIEnv *e, jobject o, jint handle)
{
	LogInformation("NativeNfcSecureElement.doDisconnect()");
	jboolean status = JNI_TRUE;
	// Close SE RAW channel handle
	W_ERROR error = WBasicCloseHandleSafeSync((W_HANDLE) handle);
	// Close SE connection handle, only one SE connected for the current version
	error = WBasicCloseHandleSafeSync((W_HANDLE) seConnection);

	if (error != W_SUCCESS)
	{
		LogError("NativeNfcSecureElement.doDisconnect(): can't close SE connection handle: error=0x%X",
				error);
		status = JNI_FALSE;
	}
	return status;
}

static jbyteArray com_android_nfc_NativeNfcSecureElement_doTransceive(JNIEnv *e, jobject o, jint handle, jbyteArray data)
{
	jbyteArray result = NULL;
	uint8_t* pSendApduBuffer;
	uint32_t nSendApduBufferLength;
	uint8_t receivedApduBuffer[RECEIVE_APDU_BUFFER_MAX_LENGTH];
	uint32_t nReceivedApduActualLength;

	LogInformation("NativeNfcSecureElement.doTransceive() with handle %d", handle);

	pSendApduBuffer = (uint8_t *) e->GetByteArrayElements(data, NULL);
	nSendApduBufferLength = (uint32_t) e->GetArrayLength(data);

	W_ERROR error = W7816ExchangeAPDUSync((W_HANDLE) handle, pSendApduBuffer, nSendApduBufferLength, receivedApduBuffer,
			RECEIVE_APDU_BUFFER_MAX_LENGTH, &nReceivedApduActualLength);

	if (error != W_SUCCESS)
	{
		LogError("NativeNfcSecureElement.doTransceive(): exchange APDU failed: error=0x%X",
				error);
	} else
	{
	/* Copy results back to Java */
		LogInformation("NativeNfcSecureElement.doTransceive(): received APDU reply - %d bytes", nReceivedApduActualLength);
		result = e->NewByteArray(nReceivedApduActualLength);
	   if(result != NULL)
	   {
		  e->SetByteArrayRegion(result, 0, nReceivedApduActualLength, (jbyte *) receivedApduBuffer);
	   }
	}

	e->ReleaseByteArrayElements(data, (jbyte*) pSendApduBuffer, JNI_ABORT);

   return result;
}

static jbyteArray com_android_nfc_NativeNfcSecureElement_doGetUid(JNIEnv *e, jobject o, jint handle)
{
	LogInformation("NativeNfcSecureElement.doGetUid()");
	jbyteArray SecureElementUid;

/*
	if(handle <= (jint) seNumber)
	{
	  SecureElementUid = e->NewByteArray(SecureElementInfo->RemoteDevInfo.Iso14443A_Info.UidLength);
	  e->SetByteArrayRegion(SecureElementUid, 0, SecureElementInfo->RemoteDevInfo.Iso14443A_Info.UidLength,(jbyte *)SecureElementInfo->RemoteDevInfo.Iso14443A_Info.Uid);
	  return SecureElementUid;
	}
	else
	{
		LogWarning("NativeNfcSecureElement.doGetUid(): invalid handle=%d", handle);
		return NULL;
	}
*/
	LogWarning("NativeNfcSecureElement.doGetUid(): handle=%d", handle);
	return NULL;
}

static jintArray com_android_nfc_NativeNfcSecureElement_doGetTechList(JNIEnv *e, jobject o, jint handle)
{
	LogInformation("NativeNfcSecureElement.doGetTechList");
	jintArray techList;
	/*
		if(handle <= (jint) seNumber)
		{
			techList = e->NewIntArray(1);
			e->SetIntArrayRegion(techList, 0, 1, &SecureElementTech);
			return techList;
		}
		else
		{
			LogWarning("NativeNfcSecureElement.doGetUid(): invalid handle=%d", handle);
			return NULL;
		}
	*/
	LogWarning("NativeNfcSecureElement.doGetTechList(): handle=%d", handle);
    return NULL;
}

/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] =
{
   {"doNativeOpenSecureElementConnection", "()I",
      (void *)com_android_nfc_NativeNfcSecureElement_doOpenSecureElementConnection},
   {"doNativeDisconnectSecureElementConnection", "(I)Z",
      (void *)com_android_nfc_NativeNfcSecureElement_doDisconnect},
   {"doTransceive", "(I[B)[B",
      (void *)com_android_nfc_NativeNfcSecureElement_doTransceive},
   {"doGetUid", "(I)[B",
      (void *)com_android_nfc_NativeNfcSecureElement_doGetUid},
   {"doGetTechList", "(I)[I",
      (void *)com_android_nfc_NativeNfcSecureElement_doGetTechList},
};

int register_com_android_nfc_NativeNfcSecureElement(JNIEnv *e)
{
   return jniRegisterNativeMethods(e,
      "com/android/nfc/dhimpl/NativeNfcSecureElement",
      gMethods, NELEM(gMethods));
}

} // namespace android
