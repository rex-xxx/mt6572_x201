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

#include <JNIHelp.h>
#include <jni.h>
#include <android/log.h>
#include "open_nfc_extension.h"

static jclass classManager = NULL;
static jclass classSecureElement = NULL;
static jmethodID ctorSecureElement = NULL;

static jint com_opennfc_extension_engine_SecureElementAdapter_getSecureElementsNumber(JNIEnv * e, jobject o)
{
	int nbSE = 0;
	W_ERROR error = WNFCControllerGetIntegerProperty(W_NFCC_PROP_SE_NUMBER, &nbSE);
	LogInformation("getSecureElements(): SE number = %d", nbSE);

	if (error != W_SUCCESS)
	{
		LogError("Can't retrieve the number of Secure Elements (error=0x%X)", error);
	}
	return (jint) nbSE;
}

static jobject com_opennfc_extension_engine_SecureElementAdapter_getSecureElement(JNIEnv * e, jobject o, jint slot)
{
	JNIEnv env = *e;
	jobject newSecureElement = NULL;
	tWSEInfoEx seInformation;
	W_ERROR error = WSEGetInfoEx(slot, &seInformation);
	if (error != W_SUCCESS)
	{
		LogError("Can't retrieve info for SE #%d (error=0x%X)", slot, error);
	}
	else
	{

		// get the seName
		int indice = 0;

		while (seInformation.aDescription[indice] != 0)
		{
			indice++;
		}

		jstring seName = env->NewString(e, seInformation.aDescription, indice);

		newSecureElement = env->NewObject(e, classSecureElement, ctorSecureElement, seInformation.nSlotIdentifier, seName,
				seInformation.nSupportedProtocols, seInformation.nCapabilities, seInformation.nVolatilePolicy,
				seInformation.nPersistentPolicy);
		env->DeleteLocalRef(e, seName);
	}
	return newSecureElement;
}

static jint com_opennfc_extension_engine_SecureElementAdapter_setPolicy(JNIEnv * e, jobject o, jint slot, jint storageType, jint policy)
{
	JNIEnv env = *e;
	jobject newSecureElement = NULL;
	tWSEInfoEx seInformation;
	W_ERROR error = WSESetPolicySync(slot, storageType, policy);
	if (error != W_SUCCESS)
	{
		LogError("Can't retrieve set policy for SE #%d (error=0x%X)", slot, error);
	}
	return (jint) error;
}

static jint com_opennfc_extension_engine_SecureElementAdapter_getSWPStatus(JNIEnv * e, jobject o, jint slot)
{
	bool_t bIsPresent;
	uint32_t nSWPLinkStatus;

	W_ERROR error = WSEGetStatusSync((uint32_t) slot, &bIsPresent, &nSWPLinkStatus);
	if (error != W_SUCCESS)
	{
		LogError("Can't retrieve SWP status for SE #%d (error=0x%X)", slot, error);
	}
	return (jint) nSWPLinkStatus;
}

static jint com_opennfc_extension_engine_SecureElementAdapter_activateSWPLine(JNIEnv * e, jobject o, jint slot)
{
	W_ERROR error = WNFCControllerActivateSwpLine((uint32_t) slot);
	if (error != W_SUCCESS)
	{
		LogError("Can't activate SWP line for SE #%d (error=0x%X)", slot, error);
	}
	return (jint) error;
}

static JNINativeMethod gMethods[] =
{
{ "getSecureElementsNumber", "()I", (void*) com_opennfc_extension_engine_SecureElementAdapter_getSecureElementsNumber },
{ "getSecureElement", "(I)Lcom/opennfc/extension/SecureElement;",
		(void*) com_opennfc_extension_engine_SecureElementAdapter_getSecureElement },
{ "setPolicy", "(III)I", (void*) com_opennfc_extension_engine_SecureElementAdapter_setPolicy },
{ "getSWPStatus", "(I)I", (void*) com_opennfc_extension_engine_SecureElementAdapter_getSWPStatus },
{ "activateSWPLine", "(I)I", (void*) com_opennfc_extension_engine_SecureElementAdapter_activateSWPLine }, };

int initalize_com_opennfc_extension_engine_SecureElementAdapter(JNIEnv * environmentJNI)
{
	JNIEnv env = *environmentJNI;
	const char* className = "com/opennfc/extension/engine/SecureElementAdapter";
	jclass classTemporary = env->FindClass(environmentJNI, className);
	classManager = env->NewGlobalRef(environmentJNI, classTemporary);

	jclass classTemporary1 = env->FindClass(environmentJNI, "com/opennfc/extension/SecureElement");
	classSecureElement = env->NewGlobalRef(environmentJNI, classTemporary1);

	ctorSecureElement = env->GetMethodID(environmentJNI, classSecureElement, "<init>", "(ILjava/lang/String;IIII)V");

	env->DeleteLocalRef(environmentJNI, classTemporary1);
	env->DeleteLocalRef(environmentJNI, classTemporary);

	return jniRegisterNativeMethods(environmentJNI, className, gMethods, NELEM(gMethods));
}

void deinitalize_com_opennfc_extension_engine_SecureElementAdapter(JNIEnv * environmentJNI)
{
	(*environmentJNI)->DeleteGlobalRef(environmentJNI, classManager);
	(*environmentJNI)->DeleteGlobalRef(environmentJNI, classSecureElement);
}
