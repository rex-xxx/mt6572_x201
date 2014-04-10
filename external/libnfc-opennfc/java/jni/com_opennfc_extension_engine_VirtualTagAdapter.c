#include <JNIHelp.h>
#include <jni.h>
#include <android/log.h>
#include "open_nfc_extension.h"

static jclass classManager = NULL;
static jobject job = NULL;

extern void setLock(bool_t rLock, bool_t cLock);
extern bool_t getReaderLock();
extern bool_t getCardLock();

static jint com_opennfc_extension_engine_VirtualTagAdapter_virtualTagCreate(JNIEnv * e, jobject o, jint cardType, jbyteArray identifier,
		jint tagCapacity, jint virtualTagIndice)
{
	LogInformation(" Jni virtualTagCreate");
	W_HANDLE phHandle = 0;
	uint8_t vtIndice;
	uint8_t type;
	uint32_t maxLength;
	JNIEnv env = *e;
	vtIndice = virtualTagIndice;
	type = cardType;
	maxLength = tagCapacity;

	uint8_t * uid = (uint8_t *) env->GetByteArrayElements(e, identifier, NULL);
	uint32_t uidLen = (uint32_t) env->GetArrayLength(e, identifier);

	if (type == 2) // W_PROP_ISO_14443_4_A
	{
		type = W_PROP_NFC_TAG_TYPE_4_A;
	}
	else if (type == 4)
	{ // W_PROP_ISO_14443_4_B
		type = W_PROP_NFC_TAG_TYPE_4_B;
	}

	WVirtualTagCreate(type, uid, uidLen, maxLength, &phHandle);

	env->ReleaseByteArrayElements(e, identifier, (jbyte *) uid, JNI_ABORT);
	LogInformation("virtualTagCreate jni: handle = %d", phHandle);
	return (jint) phHandle;
}

/*	callback function to be called when virtualTag Event happens */
void virtualTagEventCallback(int indice, int event)
{
	LogInformation("virtualTagEventCallback: ENTER");

	JNIEnv* e = attachJniEnv();
	jmethodID method;
	JNIEnv env = *e;
	method = env->GetMethodID(e, classManager, "virtualTagEventCallback", "(II)V");
	if (method == 0)
	{
		LogError("virtualTagEventCallback(): can't call virtualTagEventCallback()");
		detachJniEnv();
		return;
	}
	env->CallVoidMethod(e, job, method, indice, event);
	detachJniEnv();
}

void vEventCallback(void *pHandlerParameter, uint32_t nEventCode)
{
	LogInformation(" Jni virtualTag: vEventCallback");
	int indice;
	indice = (int *) pHandlerParameter;
	virtualTagEventCallback(indice, nEventCode);
}

static jint com_opennfc_extension_engine_VirtualTagAdapter_virtualTagStart(JNIEnv * e, jobject o, jint handle, jint virtualTagIndice,
		jboolean isReadOnly)
{
	int error = WVirtualTagStartSync((W_HANDLE) handle, (tWBasicGenericEventHandler *) (&vEventCallback), (void *) virtualTagIndice,
			(bool_t) isReadOnly);
	return error;
}

static jint com_opennfc_extension_engine_VirtualTagAdapter_stopVirtualTag(JNIEnv * e, jobject o, jint handle)
{
	int error = WVirtualTagStopSync(handle);
	return error;
}

static void com_opennfc_extension_engine_VirtualTagAdapter_closeVirtualTag(JNIEnv * e, jobject o, jint handle)
{
	WBasicCloseHandle(handle);
}

static jboolean com_opennfc_extension_engine_VirtualTagAdapter_setVirtualTagMode(JNIEnv * e, jobject o, jboolean enable)
{
	W_ERROR error;
	bool_t vReaderLock = W_FALSE;
	bool_t vCardLock = W_FALSE;
	if (enable == JNI_TRUE)
	{
		WNFCControllerGetRFLock(W_NFCC_STORAGE_VOLATILE, &vReaderLock, &vCardLock);
		setLock (vReaderLock, vCardLock);
		error = WNFCControllerSetRFLockSync(W_NFCC_STORAGE_VOLATILE, W_TRUE, W_FALSE);
		error = WNFCControllerSwitchStandbyMode(W_FALSE);
	}
	else
	{
		vReaderLock = getReaderLock();
		vCardLock = getCardLock();
		error = WNFCControllerSetRFLockSync(W_NFCC_STORAGE_VOLATILE, vReaderLock, vCardLock);
		error = WNFCControllerSwitchStandbyMode(W_TRUE);
	}
	if (error == W_SUCCESS)
		return JNI_TRUE;

	return JNI_FALSE;
}

static void com_opennfc_extension_engine_VirtualTagAdapter_setVirtualTagAdapterObject(JNIEnv * e, jobject o)
{
	JNIEnv env = *e;
	job = env->NewGlobalRef(e, o);
}

static void com_opennfc_extension_engine_VirtualTagAdapter_closeVirtualTagAdapterObject(JNIEnv * e, jobject o)
{
	JNIEnv env = *e;
	env->DeleteGlobalRef(e, job);
}

static jbyteArray com_opennfc_extension_engine_VirtualTagAdapter_readNdefMessage(JNIEnv * e, jobject o, jint ndefType,
		jint virtualTagHandle)
{
	JNIEnv env = *e;
	jbyteArray result = NULL;
	uint32_t messageLength;
	W_HANDLE hMessage;

	WNDEFReadMessageSync(virtualTagHandle, (uint8_t) ndefType, null, &hMessage);
	if (hMessage == W_NULL_HANDLE)
	{
		LogError("hMessage is null, readNdefMessage returns null");
		return result;
	}

	LogInformation("readNdefMessage : virtualTagHandle=%d, hMessage=%d", virtualTagHandle, hMessage);

	/*
	 * read Ndef Message
	 */
	uint32_t nMessageLength;
	uint32_t nActualMessageLength;
	uint8_t* pMessage;
	W_HANDLE hNextMessage;
	uint32_t offset;
	uint8_t* writePoint = (uint8_t *) malloc(MAXIMUM_READ_NDEF_BUFFER_SIZE);
	if (writePoint == null)
		{
		LogError("malloc writePoint is null");
		return result;
		}
	offset = 0;
	uint8_t* startPoint = writePoint;
	while (1)
	{
		nMessageLength = WNDEFGetMessageLength(hMessage);
		pMessage = (uint8_t *) malloc(nMessageLength);
		LogInformation("readNdefMessage : nMessageLength=%d", nMessageLength);
		if (pMessage == null)
		{
			LogError("pMessage is null");
			break;
		}
		if (WNDEFGetMessageContent(hMessage, pMessage, nMessageLength, &nActualMessageLength) != W_SUCCESS)
		{
			free(pMessage);
			LogError("WNDEFGetMessageContent fails");
			break;
		}
		offset += nMessageLength;
		if (offset >= MAXIMUM_READ_NDEF_BUFFER_SIZE)
		{
			free(pMessage);
			offset -= nMessageLength;
			LogError("Out of max buffer size");
			break;
		}
		memcpy(writePoint, pMessage, nMessageLength);
		writePoint += nMessageLength;
		free(pMessage);
		hNextMessage = WNDEFGetNextMessage(hMessage);
		WBasicCloseHandle(hMessage);
		hMessage = hNextMessage;
		if (W_NULL_HANDLE == hNextMessage)
		{
			LogInformation("readNdefMessage : Tchao");
			break;
		}
	} // Fin while

	result = env->NewByteArray(e, offset);
	if (result != NULL)
	{
		env->SetByteArrayRegion(e, result, 0, offset, (jbyte *) startPoint);
	}
	free(startPoint);
	LogInformation("read NdefMessage OK with length = %d", offset);

	return result;
}

static jboolean com_opennfc_extension_engine_VirtualTagAdapter_writeNdefMessage(JNIEnv * e, jobject o, jbyteArray buf,
		jint virtualTagHandle)
{
	JNIEnv env = *e;
	W_HANDLE msg_handle;
	W_ERROR result = W_ERROR_TAG_WRITE;

	uint32_t ndefMessageLength = (uint32_t) env->GetArrayLength(e, buf);
	if (ndefMessageLength == 0)
		{
			LogError("VirtualTagAdapter_writeNdefMessage: Nothing to write");
			return JNI_FALSE;
		}

	uint8_t *buffer = (uint8_t *) env->GetByteArrayElements(e, buf, NULL);
	LogInformation("do writeNdefMessage");
	WNDEFBuildMessage(buffer, ndefMessageLength, &msg_handle);


	result = WNDEFWriteMessageSync(virtualTagHandle, msg_handle, W_NDEF_ACTION_BIT_FORMAT_NON_NDEF_TAG | W_NDEF_ACTION_BIT_ERASE);
	LogInformation("VirtualTagAdapter_writeNdefMessage OK with result = %d", result);
	env->ReleaseByteArrayElements(e, buf, (jbyte *) buffer, JNI_ABORT);
	return (result == W_SUCCESS ? JNI_TRUE : JNI_FALSE);
}

static JNINativeMethod gMethods[] =
{
{ "setVirtualTagAdapterObject", "()V", (void*) com_opennfc_extension_engine_VirtualTagAdapter_setVirtualTagAdapterObject },
{ "closeVirtualTagAdapterObject", "()V", (void*) com_opennfc_extension_engine_VirtualTagAdapter_closeVirtualTagAdapterObject },
{ "virtualTagCreate", "(I[BII)I", (void*) com_opennfc_extension_engine_VirtualTagAdapter_virtualTagCreate },
{ "virtualTagStart", "(IIZ)I", (void*) com_opennfc_extension_engine_VirtualTagAdapter_virtualTagStart },
{ "stopVirtualTag", "(I)I", (void*) com_opennfc_extension_engine_VirtualTagAdapter_stopVirtualTag },
{ "closeVirtualTag", "(I)V", (void*) com_opennfc_extension_engine_VirtualTagAdapter_closeVirtualTag },
{ "setVirtualTagMode", "(Z)Z", (void*) com_opennfc_extension_engine_VirtualTagAdapter_setVirtualTagMode },
{ "readNdefMessage", "(II)[B", (void*) com_opennfc_extension_engine_VirtualTagAdapter_readNdefMessage },
{ "writeNdefMessage", "([BI)Z", (void*) com_opennfc_extension_engine_VirtualTagAdapter_writeNdefMessage },

};

int initalize_com_opennfc_extension_engine_VirtualTagAdapter(JNIEnv * environmentJNI)
{
	JNIEnv env = *environmentJNI;
	const char* className = "com/opennfc/extension/engine/VirtualTagAdapter";
	jclass classTemporary = env->FindClass(environmentJNI, className);
	classManager = env->NewGlobalRef(environmentJNI, classTemporary);
	env->DeleteLocalRef(environmentJNI, classTemporary);
	return jniRegisterNativeMethods(environmentJNI, className, gMethods, NELEM(gMethods));
}

void deinitalize_com_opennfc_extension_engine_VirtualTagAdapter(JNIEnv * environmentJNI)
{
	(*environmentJNI)->DeleteGlobalRef(environmentJNI, classManager);
}
