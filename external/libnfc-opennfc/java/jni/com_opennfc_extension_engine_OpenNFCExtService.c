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

static JavaVM* globalJvm = NULL;
static jclass classManager = NULL;
static jclass classRFActivity = NULL;
static jmethodID ctorRFActivity = NULL;
static jclass classRFLock = NULL;
static jmethodID ctorRFLock = NULL;

/* maximum number of characters that can be returned by WNFCControllerGetProperty() */
#define MAXIMUM_PROPERTY_LENGTH 100

/**Open NFC version*/
static const char16_t VERSION_OPEN_NFC_TARGET[] = { '4', '.', '4', 0 };

JNIEnv* attachJniEnv() {
	JNIEnv* ret = NULL;
	JavaVMAttachArgs thread_args;

	thread_args.name = "open_nfc_ext_thread";
	thread_args.version = JNI_VERSION_1_6;
	thread_args.group = NULL;

	(*globalJvm)->AttachCurrentThread(globalJvm, &ret, &thread_args);
	return ret;
}

void detachJniEnv() {
	(*globalJvm)->DetachCurrentThread(globalJvm);
}

/**
 * Pump the events in separate thread
 */
void * basicExecuteEventThread(void * parameter)
{
	LogInformation("basicExecuteEventThread()");

/*
	JavaVM * javaVM = getJavaVM();
	JNIEnv * jniEnvironment=NULL;


	javaVM->GetEnv( (void **)&jniEnvironment, JNI_VERSION_1_6);

	bool attached = false;
	if (jniEnvironment == NULL)
	{
		attached = true;
		JavaVMAttachArgs thread_args;
		thread_args.name = "threadedWBasicExecuteEventLoop";
		thread_args.version = JNI_VERSION_1_6;
		thread_args.group = NULL;
		javaVM->AttachCurrentThread(&jniEnvironment, &thread_args);
	}
*/

	WBasicExecuteEventLoop();
	LogInformation("closing basicExecuteEventThread()");

/*
	if (attached == true)
	{
		javaVM->DetachCurrentThread();
	}
*/

	return NULL;
}

static void com_opennfc_extension_engine_OpenNfcExtService_initialize(JNIEnv * e, jobject o)
{
	LogInformation("initialize()");
	pthread_t threadId;
	W_ERROR error = WBasicInit(VERSION_OPEN_NFC_TARGET);
	if (error != W_SUCCESS)
	{
		LogError("WBasicInit() failed (error = 0x%X)",error);
		return;
	}

	if (pthread_create(&threadId, NULL, basicExecuteEventThread, NULL) != 0)
	{
		LogError("Can't launch thread for WBasicExecuteEventLoop");
	}
}

static void com_opennfc_extension_engine_OpenNfcExtService_terminate(JNIEnv * e, jobject o)
{
	LogInformation("terminate()");
	WBasicTerminate();
}

static jstring com_opennfc_extension_engine_OpenNfcExtService_getStringProperty(JNIEnv * e, jobject o, jint propertyId)
{

	JNIEnv env = *e;
	jstring property = NULL;

	char16_t propertyBuffer[MAXIMUM_PROPERTY_LENGTH];
	uint32_t  nValueLength = 0;

	W_ERROR error = WNFCControllerGetProperty(propertyId, propertyBuffer,
			MAXIMUM_PROPERTY_LENGTH, &nValueLength);

	LogInformation("getStringProperty(): error=0x%X, nValueLength=%d, ", error, nValueLength);
	if (error == W_SUCCESS)
	{
		property = env->NewString(e, (jchar*) &propertyBuffer, nValueLength);
	} else
	{
		LogError("Can't read property (error=0x%X)", error);
	}
	return property;
}

static jobject com_opennfc_extension_engine_OpenNfcExtService_getRFActivity(JNIEnv * e, jobject o)
{
	JNIEnv env = *e;
	jobject newObject = NULL;
	uint8_t nReaderState;
	uint8_t nCardState;
	uint8_t nP2PState;
	WNFCControllerGetRFActivity(&nReaderState, &nCardState, &nP2PState);
	LogInformation("getRFActivity(): nReaderState=0x%X, nCardState=0x%X, nP2PState=0x%X",
			nReaderState, nCardState, nP2PState);

	newObject = env->NewObject(e, classRFActivity, ctorRFActivity, (jint) nReaderState, (jint) nCardState, (jint) nP2PState);
	return newObject;
}

static jobject com_opennfc_extension_engine_OpenNfcExtService_getRFLock(JNIEnv * e, jobject o)
{
	JNIEnv env = *e;
	jobject newObject = NULL;
	bool_t bReaderLock;
	bool_t bCardLock;
	WNFCControllerGetRFLock(W_NFCC_STORAGE_VOLATILE, &bReaderLock, &bCardLock);

	LogInformation("getRFLock(): bReaderLock=%d, bCardLock=%d", bReaderLock, bCardLock);

	newObject = env->NewObject(e, classRFLock, ctorRFLock, (jboolean) bReaderLock, (jboolean) bCardLock);
	return newObject;
}


static int com_opennfc_extension_engine_OpenNfcExtService_setRFLock(JNIEnv * e, jobject o, jboolean bReaderLock, jboolean bCardLock)
{
	JNIEnv env = *e;

	W_ERROR error = WNFCControllerSetRFLockSync(W_NFCC_STORAGE_VOLATILE, bReaderLock, bCardLock);
	if (error != W_SUCCESS)
	{
		LogError("Can't set RFLock (error=0x%X)", error);
	}
	return (jint) error;
}

static JNINativeMethod gMethods[] =
{
		{"initialize", "()V",	(void*) com_opennfc_extension_engine_OpenNfcExtService_initialize},
		{"terminate", "()V",	(void*) com_opennfc_extension_engine_OpenNfcExtService_terminate},
		{"getStringProperty", "(I)Ljava/lang/String;",	(void*) com_opennfc_extension_engine_OpenNfcExtService_getStringProperty},
		{"getRFActivity", "()Lcom/opennfc/extension/RFActivity;",	(void*) com_opennfc_extension_engine_OpenNfcExtService_getRFActivity},
		{"getRFLock", "()Lcom/opennfc/extension/RFLock;",	(void*) com_opennfc_extension_engine_OpenNfcExtService_getRFLock},
		{"setRFLock", "(ZZ)I",	(void*) com_opennfc_extension_engine_OpenNfcExtService_setRFLock},
};

int initalize_com_opennfc_extension_engine_OpenNfcExtService(JNIEnv * environmentJNI)
{
	JNIEnv env = *environmentJNI;
	const char* className = "com/opennfc/extension/engine/OpenNfcExtService";
	jclass classTemporary = env->FindClass(environmentJNI, className);
	classManager = env->NewGlobalRef(environmentJNI, classTemporary);
	env->DeleteLocalRef(environmentJNI, classTemporary);

	jclass classTemporary1 = env->FindClass(environmentJNI, "com/opennfc/extension/RFActivity");
	classRFActivity = env->NewGlobalRef(environmentJNI, classTemporary1);
	ctorRFActivity = env->GetMethodID(environmentJNI, classRFActivity, "<init>",
			"(III)V");
	env->DeleteLocalRef(environmentJNI, classTemporary1);

	classTemporary1 = env->FindClass(environmentJNI, "com/opennfc/extension/RFLock");
	classRFLock = env->NewGlobalRef(environmentJNI, classTemporary1);
	ctorRFLock = env->GetMethodID(environmentJNI, classRFLock, "<init>",
			"(ZZ)V");
	env->DeleteLocalRef(environmentJNI, classTemporary1);

	return jniRegisterNativeMethods(environmentJNI, className, gMethods, NELEM(gMethods));
}

void deinitalize_com_opennfc_extension_engine_OpenNfcExtService(JNIEnv * environmentJNI)
{
	(*environmentJNI)->DeleteGlobalRef(environmentJNI, classManager);
	(*environmentJNI)->DeleteGlobalRef(environmentJNI, classRFActivity);
	(*environmentJNI)->DeleteGlobalRef(environmentJNI, classRFLock);
}

jint JNI_OnLoad(JavaVM *jvm, void *reserved)
{
	JNIEnv*	environmentJNI;

	if((*jvm)->GetEnv(jvm, (void **)(& environmentJNI), JNI_VERSION_1_6))
	{
		return JNI_ERR;
	}

	if(initalize_com_opennfc_extension_engine_OpenNfcExtService(environmentJNI))
	{
		return JNI_ERR;
	}

	if(initalize_com_opennfc_extension_engine_SecureElementAdapter(environmentJNI))
	{
		return JNI_ERR;
	}
	if(initalize_com_opennfc_extension_engine_FirmwareUpdateAdapter(environmentJNI))
	{
		return JNI_ERR;
	}
	if(initalize_com_opennfc_extension_engine_CardEmulationAdapter(environmentJNI))
	{
		return JNI_ERR;
	}
	if(initalize_com_opennfc_extension_engine_VirtualTagAdapter(environmentJNI))
	{
		return JNI_ERR;
	}
	if(initalize_com_opennfc_extension_engine_RoutingTableAdapter(environmentJNI))
	{
		return JNI_ERR;
	}
	if(initalize_com_opennfc_extension_engine_CardEmulationAdapter(environmentJNI))
	{
		return JNI_ERR;
	}
	globalJvm = jvm;
	return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *jvm, void *reserved) {
	LogInformation("JNI_OnUnload()");
	JNIEnv*	environmentJNI;

	(*jvm)->GetEnv(jvm, (void **)(& environmentJNI), JNI_VERSION_1_6);

	deinitalize_com_opennfc_extension_engine_OpenNfcExtService(environmentJNI);
	deinitalize_com_opennfc_extension_engine_SecureElementAdapter(environmentJNI);
	deinitalize_com_opennfc_extension_engine_FirmwareUpdateAdapter(environmentJNI);
	deinitalize_com_opennfc_extension_engine_CardEmulationAdapter(environmentJNI);
	deinitalize_com_opennfc_extension_engine_VirtualTagAdapter(environmentJNI);
	deinitalize_com_opennfc_extension_engine_RoutingTableAdapter(environmentJNI);
}
