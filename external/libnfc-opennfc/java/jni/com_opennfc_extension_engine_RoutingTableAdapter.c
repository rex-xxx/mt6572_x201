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

static jclass classRoutingTableEntry = NULL;

static jmethodID ctorRoutingTableEntry = NULL;

static jint com_opennfc_extension_engine_RoutingTableAdapter_read(JNIEnv * e, jobject o)
{
	W_HANDLE handle;
	W_ERROR error = WRoutingTableReadSync(&handle);
	if (error != W_SUCCESS)
	{
		LogError("Can't read routing table (error=0x%X)", error);
	}
	return (jint) handle;
}

static jobject com_opennfc_extension_engine_RoutingTableAdapter_getTableEntry(JNIEnv* e, jobject o, jint handle, jint index)
{
	JNIEnv env = *e;
	jobject newEntry = NULL;
	tWRoutingTableEntry routingTableEntry;
	W_ERROR error = WRoutingTableGetEntry((W_HANDLE) handle, index, &routingTableEntry);

	if (error != W_SUCCESS)
	{
		LogError("Can't retrieve info for routing table entry #%d (error=0x%X)", index, error);
	} else {
		jbyteArray aid = env->NewByteArray(e, routingTableEntry.nAidLength);
		if (routingTableEntry.nAidLength > 0) {
			env->SetByteArrayRegion(e, aid, 0, routingTableEntry.nAidLength,
			  (jbyte*) routingTableEntry.aAid);
		}
		newEntry = env->NewObject(e, classRoutingTableEntry, ctorRoutingTableEntry,
				(jint) routingTableEntry.nEntryType, (jint) routingTableEntry.nValidityFlags,
				(jint) routingTableEntry.nTargetId, aid);
		env->DeleteLocalRef(e, aid);
	}
	return newEntry;
}

static jint com_opennfc_extension_engine_RoutingTableAdapter_readTableHandle(JNIEnv* e, jobject o)
{
	JNIEnv env = *e;
	W_HANDLE handle;
	W_ERROR error = WRoutingTableReadSync(&handle);
	if (error != W_SUCCESS)
	{
		LogError("Can't read routing table (error=0x%X)", error);
		handle = 0;
	}
	return (jint) handle;
}

static jint com_opennfc_extension_engine_RoutingTableAdapter_getTableEntryCount(JNIEnv* e, jobject o, jint handle)
{
	JNIEnv env = *e;
	uint16_t entryCount;

	W_ERROR error = WRoutingTableGetEntryCount((W_HANDLE) handle, &entryCount);
	if (error != W_SUCCESS)
	{
		LogError("Can't retrieve number of routing table entries (error=0x%X)", error);
	}
	return (jint) entryCount;
}

static jint com_opennfc_extension_engine_RoutingTableAdapter_create(JNIEnv * e, jobject o)
{
	W_HANDLE handle;
	W_ERROR error = WRoutingTableCreate(&handle);
	if (error != W_SUCCESS)
	{
		LogError("Can't create a new empty routing table (error=0x%X)", error);
	}
	return (jint) handle;
}

static jint com_opennfc_extension_engine_RoutingTableAdapter_enable(JNIEnv* e, jobject o, jboolean isEnabled)
{
	W_ERROR error = WRoutingTableEnableSync((bool_t) isEnabled);
	if (error != W_SUCCESS)
	{
		LogError("Can't enable routing table (error=0x%X)", error);
	}
	return (jint) error;
}

static jboolean com_opennfc_extension_engine_RoutingTableAdapter_isSupported(JNIEnv* e, jobject o)
{
	bool_t isEnabled;
	return (WRoutingTableIsEnabled(&isEnabled) == W_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}

static jboolean com_opennfc_extension_engine_RoutingTableAdapter_isEnabled(JNIEnv* e, jobject o)
{
	bool_t isEnabled;
	W_ERROR error = WRoutingTableIsEnabled(&isEnabled);
	if (error != W_SUCCESS)
	{
		LogError("Can't check if routing table is enabled (error=0x%X)", error);
	}
	return (jboolean) isEnabled;
}

static jint com_opennfc_extension_engine_RoutingTableAdapter_modify(JNIEnv* e, jobject o, jint handle, jint operation, jint index, jobject entry)
{
	JNIEnv env = *e;
	tWRoutingTableEntry routingTableEntry;
	tWRoutingTableEntry* pRoutingTableEntry = NULL;

	static jmethodID methodGetEntryTypeValue = NULL;
	static jmethodID methodGetValidityFlagsValue = NULL;
	static jfieldID fieldTargetId = NULL;
	static jfieldID fieldAID = NULL;

	if (fieldTargetId == NULL) {
		methodGetEntryTypeValue = env->GetMethodID(e, classRoutingTableEntry, "getEntryTypeValue",
				"()I");

		methodGetValidityFlagsValue = env->GetMethodID(e, classRoutingTableEntry, "getValidityFlagsValue",
				"()I");
		fieldTargetId = env->GetFieldID(e, classRoutingTableEntry, "targetId", "I");
		fieldAID = env->GetFieldID(e, classRoutingTableEntry, "aid", "[B");
	}

	if (entry != NULL) {
		routingTableEntry.nEntryType = (uint8_t) env->CallIntMethod(e, entry, methodGetEntryTypeValue);
		routingTableEntry.nValidityFlags = (uint8_t) env->CallIntMethod(e, entry, methodGetValidityFlagsValue);
		routingTableEntry.nTargetId = (uint32_t) env->GetIntField(e, entry, fieldTargetId);

		jbyteArray aidArray = env->GetObjectField(e, entry, fieldAID);
		routingTableEntry.nAidLength  = env->GetArrayLength(e, aidArray);
		jbyte* aidData = env->GetByteArrayElements(e, aidArray, NULL);
		memcpy(routingTableEntry.aAid, aidData, routingTableEntry.nAidLength);
		env->ReleaseByteArrayElements(e, aidArray, aidData, JNI_ABORT);
		pRoutingTableEntry = &routingTableEntry;
	}

	W_ERROR error = WRoutingTableModify((W_HANDLE) handle, (uint32_t) operation, (uint16_t) index, pRoutingTableEntry);
	if (error != W_SUCCESS)
	{
		LogError("Can't modify the routing table: operation=0x%X, index=%d (error=0x%X)", operation, index, error);
	}
	return (jint) error;
}

static void com_opennfc_extension_engine_RoutingTableAdapter_close(JNIEnv* e, jobject o, jint handle)
{
	WBasicCloseHandle ((W_HANDLE) handle);
}

static JNINativeMethod gMethods[] =
{
	{"readTableHandle",	"()I",	(void*) com_opennfc_extension_engine_RoutingTableAdapter_readTableHandle},
	{"doRead","()I", (void*) com_opennfc_extension_engine_RoutingTableAdapter_read},
	{"getTableEntry", "(II)Lcom/opennfc/extension/RoutingTableEntry;", (void*) com_opennfc_extension_engine_RoutingTableAdapter_getTableEntry},
	{"getTableEntryCount", "(I)I", (void*) com_opennfc_extension_engine_RoutingTableAdapter_getTableEntryCount},
	{"doCreate","()I", (void*) com_opennfc_extension_engine_RoutingTableAdapter_create},
	{"enable", "(Z)I", (void*) com_opennfc_extension_engine_RoutingTableAdapter_enable},
	{"isSupported", "()Z", (void*) com_opennfc_extension_engine_RoutingTableAdapter_isSupported},
	{"isEnabled", "()Z", (void*) com_opennfc_extension_engine_RoutingTableAdapter_isEnabled},
	{"modify","(IIILcom/opennfc/extension/RoutingTableEntry;)I", (void*) com_opennfc_extension_engine_RoutingTableAdapter_modify},
	{"doClose", "(I)V", (void*) com_opennfc_extension_engine_RoutingTableAdapter_close},
};

int initalize_com_opennfc_extension_engine_RoutingTableAdapter(JNIEnv * environmentJNI)
{
	JNIEnv env = *environmentJNI;

	const char* className = "com/opennfc/extension/RoutingTableEntry";
	jclass classTemporary = env->FindClass(environmentJNI, className);
	classRoutingTableEntry = env->NewGlobalRef(environmentJNI, classTemporary);
	env->DeleteLocalRef(environmentJNI, classTemporary);

	ctorRoutingTableEntry = env->GetMethodID(environmentJNI, classRoutingTableEntry, "<init>",
			"(III[B)V");

	return jniRegisterNativeMethods(environmentJNI, "com/opennfc/extension/engine/RoutingTableAdapter",
			gMethods, NELEM(gMethods));
}

void deinitalize_com_opennfc_extension_engine_RoutingTableAdapter(JNIEnv * environmentJNI)
{
	JNIEnv env = *environmentJNI;
	env->DeleteGlobalRef(environmentJNI, classRoutingTableEntry);
}
