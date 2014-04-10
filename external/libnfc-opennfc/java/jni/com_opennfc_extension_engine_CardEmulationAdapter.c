#include <JNIHelp.h>
#include <jni.h>
#include <android/log.h>
#include "open_nfc_extension.h"

static jclass classManager = NULL;
static jobject job = NULL;

bool_t readerLock = W_FALSE;
bool_t cardLock = W_FALSE;

void setLock(bool_t rLock, bool_t cLock){
	readerLock = rLock;
	cardLock = cLock;
	LogInformation("start Virtual Tag Mode with readerLock %d cardLock %d", readerLock, cardLock);
}

bool_t getReaderLock(){
	LogInformation("shut down Virtual Tag mode with ReaderLock %d", readerLock);
	return readerLock;
}

bool_t getCardLock(){
	LogInformation("shut down Virtual Tag mode with cardLock %d", cardLock);
	return cardLock;
}


/*	callback function to be called when cardEmulation Event happens */
void cardEmulationEventCallback(int indice, int event)
{
	LogInformation("cardEmulationEventCallback: ENTER");

	JNIEnv* e = attachJniEnv();
	jmethodID method;
	JNIEnv env = *e;
	method = env->GetMethodID(e, classManager, "cardEmulEventCallback", "(II)V");
	if (method == 0)
	{
		LogError("cardEmulationEventCallback(): can't call cardEmulEventCallback()");
		detachJniEnv();
		return;
	}
	env->CallVoidMethod(e, job, method, indice, event);
	detachJniEnv();
}

void pEventCallback(void *pHandlerParameter, uint32_t nEventCode)
{
	LogInformation(" Jni pEventCallback");
	int indice;
	indice = (int *) pHandlerParameter;
	cardEmulationEventCallback(indice, nEventCode);
}

/*	callback function to be called when cardEmulation Command happens */
void cardEmulationCommandEventCallback(int indice)
{
	LogInformation("cardEmulationCommandEventCallback: ENTER");
	JNIEnv* e = attachJniEnv();
	jmethodID method;
	JNIEnv env = *e;
	jbyteArray returnData = NULL;

	method = env->GetMethodID(e, classManager, "getCardEmulationHandle", "(I)I");
	if (method == 0)
	{
		LogError("getCardEmulationHandle(): can't call getCardEmulationHandle()");
		detachJniEnv();
		return;
	}
	jint handle = (jint) env->CallIntMethod(e, job, method, indice);

	LogInformation(" getCommandData: indice = %d   handle = %d", indice, handle);
	uint8_t * pDataBuffer = (uint8_t*) malloc(RETURN_MESSAGE_MAX_BUFFER_SIZE);
	if (pDataBuffer != null)
	{
	uint32_t pnActualDataLength = 0;
	WEmulGetMessageData((W_HANDLE) handle, pDataBuffer, TRANSCEIVE_MAX_BUFFER_SIZE, &pnActualDataLength);

	returnData = env->NewByteArray(e, pnActualDataLength);
	if (returnData != NULL)
	{
		env->SetByteArrayRegion(e, returnData, 0, pnActualDataLength, (jbyte *) pDataBuffer);
	}

	method = env->GetMethodID(e, classManager, "cardEmulCommandCallback", "(I[B)V");
	if (method == 0)
	{
		LogError("cardEmulationEventCallback(): can't call cardEmulEventCallback()");
		free(pDataBuffer);
		env->DeleteLocalRef(e, returnData);
		detachJniEnv();
		return;
	}
	env->CallVoidMethod(e, job, method, indice, returnData);
	free(pDataBuffer);
	}
	env->DeleteLocalRef(e, returnData);
	detachJniEnv();
}

void pCommandCallback(void *pCallbackParameter, uint32_t nDataLength)
{
	LogInformation(" Jni pCommandCallback");
	int indice;
	indice = (int *) pCallbackParameter;
	cardEmulationCommandEventCallback(indice);
}

static jint com_opennfc_extension_engine_CardEmulationAdapter_emulOpenConnection(JNIEnv * e, jobject o, jint cardType,
		jbyteArray identifier, jint randomIdentifierLength, jint cardEmulationIndice)
{
	LogInformation(" Jni emulOpenConnection");
	W_HANDLE phHandle = 0;
	int cardEmuIndice;
	JNIEnv env = *e;

	uint8_t * uid = (uint8_t *) env->GetByteArrayElements(e, identifier, NULL);
	uint32_t uidLen = (uint32_t) env->GetArrayLength(e, identifier);

	cardEmuIndice = cardEmulationIndice;

	tWEmulConnectionInfo * pEmulConnectionInfo = (tWEmulConnectionInfo *) malloc(sizeof(tWEmulConnectionInfo));
	if (pEmulConnectionInfo != null)
	{ // W_PROP_ISO_14443_4_A
		if (cardType == W_PROP_ISO_14443_4_A)
		{
			pEmulConnectionInfo->nCardType = W_PROP_ISO_14443_4_A;
			pEmulConnectionInfo->sCardInfo.s14A.bSetCIDSupport = W_FALSE;
			pEmulConnectionInfo->sCardInfo.s14A.nApplicationDataLength = 0;
			pEmulConnectionInfo->sCardInfo.s14A.nCID = 0;
			pEmulConnectionInfo->sCardInfo.s14A.nDataRateMax = 0x00;
			pEmulConnectionInfo->sCardInfo.s14A.nFWI_SFGI = 0xB0;
			pEmulConnectionInfo->sCardInfo.s14A.nNAD = 0;
			pEmulConnectionInfo->sCardInfo.s14A.nSAK = 0x20;
			pEmulConnectionInfo->sCardInfo.s14A.nATQA = 0x0010;
			if (uid != null)
			{
				pEmulConnectionInfo->sCardInfo.s14A.nUIDLength = (uint8_t) uidLen;
				switch (uidLen)
				{
					case 7:
						pEmulConnectionInfo->sCardInfo.s14A.nATQA |= 1 << 6;
						break;
					case 10:
						pEmulConnectionInfo->sCardInfo.s14A.nATQA |= 2 << 6;
						break;
				}
				memcpy(pEmulConnectionInfo->sCardInfo.s14A.UID, uid, (uidLen <= 10) ? uidLen : 10);
			}
			else
			{
				/* random UID, length is always 4 (current limitation) */
				pEmulConnectionInfo->sCardInfo.s14A.nUIDLength = 1;
			}
			//  W_PROP_ISO_14443_4_B
		}
		else if (cardType == W_PROP_ISO_14443_4_B)
		{

			pEmulConnectionInfo->nCardType = W_PROP_ISO_14443_4_B;
			pEmulConnectionInfo->sCardInfo.s14B.nAFI = 0;
			pEmulConnectionInfo->sCardInfo.s14B.nATQB = 0x00B4;
			pEmulConnectionInfo->sCardInfo.s14B.bSetCIDSupport = W_FALSE;
			pEmulConnectionInfo->sCardInfo.s14B.nCID = 0;
			pEmulConnectionInfo->sCardInfo.s14B.nNAD = 0;
			pEmulConnectionInfo->sCardInfo.s14B.nHigherLayerResponseLength = 0;
			if (uid != null)
			{
				pEmulConnectionInfo->sCardInfo.s14B.nPUPILength = (uint8_t) uidLen;
				memcpy(pEmulConnectionInfo->sCardInfo.s14B.PUPI, uid, (uidLen <= 4) ? uidLen : 4);
			}
			else
			{
				/* random UID */
				pEmulConnectionInfo->sCardInfo.s14B.nPUPILength = 0;
			}
		}

		WEmulOpenConnectionSync((tWBasicGenericEventHandler *) (&pEventCallback), (void *) cardEmuIndice,
				(tWEmulCommandReceived *) (&pCommandCallback), (void *) cardEmuIndice, pEmulConnectionInfo, &phHandle);
		free(pEmulConnectionInfo);
	}
	env->ReleaseByteArrayElements(e, identifier, (jbyte *) uid, JNI_ABORT);
	LogInformation(" Jni emulOpenConnection: returned handle = %d", phHandle);
	return (jint) phHandle;
}

static jboolean com_opennfc_extension_engine_CardEmulationAdapter_readerIsPropertySupported(JNIEnv * e, jobject o, jint cardType)
{
	bool_t isSupported = WReaderIsPropertySupported(cardType);
	return (isSupported == W_TRUE) ? JNI_TRUE : JNI_FALSE;
}

static jint com_opennfc_extension_engine_CardEmulationAdapter_stopCardEmulation(JNIEnv * e, jobject o, jint handle)
{
	uint32_t result = -1;
	result = WEmulCloseSync(handle);
	return (jint) result;
}

static jint com_opennfc_extension_engine_CardEmulationAdapter_sendAnswer(JNIEnv * e, jobject o, jint handle, jbyteArray reponse)
{
	uint32_t result = -1;
	LogInformation("Jni: sendAnswer ");
	JNIEnv env = *e;
	uint8_t * rps = (uint8_t *) env->GetByteArrayElements(e, reponse, NULL);
	uint32_t rpsLen = (uint32_t) env->GetArrayLength(e, reponse);
	result = WEmulSendAnswer((W_HANDLE) handle, rps, rpsLen);
	env->ReleaseByteArrayElements(e, reponse, (jbyte *) rps, JNI_ABORT);
	return (jint) result;
}

static jboolean com_opennfc_extension_engine_CardEmulationAdapter_setCardEmulationMode(JNIEnv * e, jobject o, jboolean enable)
{
	W_ERROR error;
	if (enable == JNI_TRUE)
	{
		WNFCControllerGetRFLock(W_NFCC_STORAGE_VOLATILE, &readerLock, &cardLock);
		LogInformation("start CardEmulation Mode with readerLock %d cardLock %d", readerLock, cardLock);
		error = WNFCControllerSetRFLockSync(W_NFCC_STORAGE_VOLATILE, W_TRUE, W_FALSE);
		error = WNFCControllerSwitchStandbyMode(W_FALSE);
	}
	else
	{
		error = WNFCControllerSetRFLockSync(W_NFCC_STORAGE_VOLATILE, readerLock, cardLock);
		LogInformation("shut down CardEmulation mode with readerLock %d", readerLock);
		LogInformation("shut down CardEmulation mode with cardLock %d", cardLock);
		error = WNFCControllerSwitchStandbyMode(W_TRUE);
	}
	if (error == W_SUCCESS)
		return JNI_TRUE;

	return JNI_FALSE;
}

static void com_opennfc_extension_engine_CardEmulationAdapter_setCardEmulationAdapterObject(JNIEnv * e, jobject o)
{
	JNIEnv env = *e;
	job = env->NewGlobalRef(e, o);
}

static void com_opennfc_extension_engine_CardEmulationAdapter_closeCardEmulationAdapterObject(JNIEnv * e, jobject o)
{
	JNIEnv env = *e;
	env->DeleteGlobalRef(e, job);
}

static JNINativeMethod gMethods[] =
{
{ "setCardEmulationAdapterObject", "()V", (void*) com_opennfc_extension_engine_CardEmulationAdapter_setCardEmulationAdapterObject },
{ "closeCardEmulationAdapterObject", "()V", (void*) com_opennfc_extension_engine_CardEmulationAdapter_closeCardEmulationAdapterObject },
{ "emulOpenConnection", "(I[BII)I", (void*) com_opennfc_extension_engine_CardEmulationAdapter_emulOpenConnection },
{ "readerIsPropertySupported", "(I)Z", (void*) com_opennfc_extension_engine_CardEmulationAdapter_readerIsPropertySupported },
{ "stopCardEmulation", "(I)I", (void*) com_opennfc_extension_engine_CardEmulationAdapter_stopCardEmulation },
{ "sendAnswer", "(I[B)I", (void*) com_opennfc_extension_engine_CardEmulationAdapter_sendAnswer },
{ "setCardEmulationMode", "(Z)Z", (void*) com_opennfc_extension_engine_CardEmulationAdapter_setCardEmulationMode }, };

int initalize_com_opennfc_extension_engine_CardEmulationAdapter(JNIEnv * environmentJNI)
{
	JNIEnv env = *environmentJNI;
	const char* className = "com/opennfc/extension/engine/CardEmulationAdapter";
	jclass classTemporary = env->FindClass(environmentJNI, className);
	classManager = env->NewGlobalRef(environmentJNI, classTemporary);
	env->DeleteLocalRef(environmentJNI, classTemporary);
	return jniRegisterNativeMethods(environmentJNI, className, gMethods, NELEM(gMethods));
}

void deinitalize_com_opennfc_extension_engine_CardEmulationAdapter(JNIEnv * environmentJNI)
{
	(*environmentJNI)->DeleteGlobalRef(environmentJNI, classManager);
}
