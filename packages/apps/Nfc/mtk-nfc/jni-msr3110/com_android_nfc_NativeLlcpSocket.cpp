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

namespace android {

typedef enum SocketStatus
{
	SOCKET_AVAILABLE_SLOT = 0, // no corresponding handle in Java
	SOCKET_WAITING_FOR_CONNECTION,
	SOCKET_CONNECTED,
	SOCKET_ERROR
} SocketStatus;

typedef struct Socket
{
	uint8_t			index;
	SocketStatus	status;
	char16_t	*	serviceName;
	uint8_t			sap;
	W_HANDLE		handleLinkP2P;
	W_HANDLE		handleSocket;
	W_HANDLE		handleCancelEstablishLink;
	pthread_mutex_t 	mutexClient;
	sem_t			semaphoreWaitConnection;
	sem_t			semaphoreWaitConnectionStep2;
} Socket;

#define	MAX_NUMBER_OF_SOCKET	8
static	Socket	sockets[MAX_NUMBER_OF_SOCKET];
static	uint8_t	indexNextSocket	=	0;

extern bool_t internalP2PWrite(JNIEnv * jniEnvironment, W_HANDLE handle, jbyteArray data);
extern jbyteArray internalReadP2P(JNIEnv * jniEnvironment, W_HANDLE handle);
extern W_HANDLE obtainServerSocketHandleOpenNFC(uint32_t handle);
extern bool_t isServerSoketConnected(uint32_t handle);

void initP2PSockets()
{
    LogInformation("initP2PSockets");
	memset(sockets, 0, sizeof(sockets));
	indexNextSocket = 0;  //JIM ADD
}

bool_t isClientSoketValid(uint32_t handle)
{
	Socket * socket = & (sockets[GET_SOCKET_INDEX(handle)]);
	LogInformation("isClientSoketValid: status=%d", socket->status);

	switch(socket->status)
	{
		case SOCKET_CONNECTED :
		case SOCKET_WAITING_FOR_CONNECTION :
	return W_TRUE;
			
		case SOCKET_AVAILABLE_SLOT :
		case SOCKET_ERROR :
			break;
		}
	return W_FALSE;
	
}

static void linkEstablishedListener(void * parameter, W_HANDLE handleLinkP2P, W_ERROR error)
{
	LogInformation("(client)linkEstablishedListener");

	Socket * socket = (Socket *)parameter;

	(void)pthread_mutex_lock(&socket->mutexClient);
	WBasicCloseHandle(socket->handleCancelEstablishLink);
	socket->handleCancelEstablishLink = W_NULL_HANDLE;

	if (error == W_SUCCESS) {
		socket->handleLinkP2P = handleLinkP2P;
	} else {
		LogError("(client)linkEstablishedListener : Error while establish link : %x", error);

		socket->status = SOCKET_ERROR;
	}

	(void)pthread_mutex_unlock(&socket->mutexClient);

		SEMAPHORE_POST(&(socket->semaphoreWaitConnection));

}

/**
 * Called when RF link lost
 * @param	parameter	Socket description (Opaque)
 * @param	error		Error description
 */
static void linkReleaseListener(void * parameter, W_ERROR error)
{
	Socket * socket = (Socket *)parameter;

	LogInformation("linkReleaseListener : socket->handleSocket=0x%x error=%x",socket->handleSocket, error);

	(void)pthread_mutex_lock(&socket->mutexClient);

	if (socket->status != SOCKET_AVAILABLE_SLOT)
		socket->status = SOCKET_ERROR;

	if (socket->handleLinkP2P != W_NULL_HANDLE) {
		WBasicCloseHandle(socket->handleLinkP2P);
		socket->handleLinkP2P = W_NULL_HANDLE;
	}
	
	(void)pthread_mutex_unlock(&socket->mutexClient);

}


static Socket * createSocket(uint8_t sap)
{
	LogInformation("(client)createSocket: sap=%d", sap);
	uint8_t index = 0;
	for(;index<indexNextSocket;index++)
	{
		if(sockets[index].status == SOCKET_AVAILABLE_SLOT)
		{
		break;
	}
	}
	if(index>=indexNextSocket)
	{
		if(indexNextSocket>=MAX_NUMBER_OF_SOCKET)
		{
			LogError("No more space to create a new client socket");
			return NULL;
		}
		LogInformation("(client)createSocket: index=%d, sap=%d", index, sap);
		indexNextSocket ++;

		sockets[index].index = index;
		sockets[index].serviceName = NULL;
		
		sockets[index].handleLinkP2P = W_NULL_HANDLE;
		sockets[index].handleSocket = W_NULL_HANDLE;
		sockets[index].handleCancelEstablishLink = W_NULL_HANDLE;
		pthread_mutex_init( &sockets[index].mutexClient, NULL );
		
		SEMAPHORE_CREATE(& (sockets[index].semaphoreWaitConnection));
		SEMAPHORE_CREATE(& (sockets[index].semaphoreWaitConnectionStep2));
	}
	
	sockets[index].sap = sap;
	sockets[index].status = SOCKET_WAITING_FOR_CONNECTION;
	
	WP2PEstablishLink
	(
			linkEstablishedListener, &sockets[index],
			linkReleaseListener, &sockets[index],
			& (sockets[index].handleCancelEstablishLink)
	);

	return & sockets[index];
}

uint32_t createSocketLinkedHandle(uint8_t sap)
{
	Socket * socket = createSocket(sap);

	if(socket==NULL)
	{
		LogPosition("createSocketLinkedHandle: socket=NULL");
		return 0;
	}

	LogInformation("Handle JAVA = %x",CREATE_CLIENT_SOCKET(socket->index));
	return (uint32_t)CREATE_CLIENT_SOCKET(socket->index);
}

jbyteArray socketTransceive(JNIEnv * jniEnvironment, uint32_t handle, jbyteArray data)
{
	W_HANDLE handleOpenNFC = 0;

	if(IS_SERVER_SOCKET(handle))
	{
		handleOpenNFC = obtainServerSocketHandleOpenNFC(handle);
	}
	else
	{
		handleOpenNFC=sockets[GET_SOCKET_INDEX(handle)].handleSocket;
	}

	if(internalP2PWrite(jniEnvironment, handleOpenNFC, data) == W_FALSE)
	{
		LogError("Error in socketTransceive, internalP2PWrite");
		return NULL;
	}
	return internalReadP2P(jniEnvironment, handleOpenNFC);
}

jbyteArray socketReceive(JNIEnv * jniEnvironment, uint32_t handle)
{
	W_HANDLE handleOpenNFC = 0;

	if(IS_SERVER_SOCKET(handle))
	{
		handleOpenNFC = obtainServerSocketHandleOpenNFC(handle);
	}
	else
	{
		handleOpenNFC=sockets[GET_SOCKET_INDEX(handle)].handleSocket;
	}
	return internalReadP2P(jniEnvironment, handleOpenNFC);
}

jboolean socketSend(JNIEnv * jniEnvironment, uint32_t handle, jbyteArray data)
{
	W_HANDLE handleOpenNFC = 0;

	if(IS_SERVER_SOCKET(handle))
	{
		if(isServerSoketConnected(handle)==W_FALSE)
		{
			return JNI_FALSE;
		}

		handleOpenNFC = obtainServerSocketHandleOpenNFC(handle);
	}
	else
	{
		if(sockets[GET_SOCKET_INDEX(handle)].status!=SOCKET_CONNECTED)
		{
			return JNI_FALSE;
		}

		handleOpenNFC=sockets[GET_SOCKET_INDEX(handle)].handleSocket;
	}

	if(internalP2PWrite(jniEnvironment, handleOpenNFC, data) == W_TRUE)
	{
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

jboolean socketDisconnect(uint32_t handle)
{
	return JNI_TRUE;
}

static void connectListener(void *pCallbackParameter, W_ERROR nResult)
{
	Socket * socket = (Socket *)(pCallbackParameter);

	if(nResult != W_SUCCESS)
	{
		LogError("(Client %d)WP2PConnect error: 0x%x", socket->index, nResult);
		if (socket->status != SOCKET_AVAILABLE_SLOT)
			socket->status = SOCKET_ERROR;
	}

	SEMAPHORE_POST(& socket->semaphoreWaitConnectionStep2);
}

/*
 * Methods
 */
static jboolean internal_doconnect(Socket * socket) 
{
	W_ERROR nError;

	/* Wait for the link established */
	SEMAPHORE_WAIT(&(socket->semaphoreWaitConnection));

	if ((socket->status == SOCKET_ERROR) || (socket->status == SOCKET_AVAILABLE_SLOT))
	{
		LogInformation("doconnect(%d): Give up doConnect because there is an error in the link", socket->index);
		return JNI_FALSE;
	}

	/* We have a valid link */

	/* Create the socket */
	(void)pthread_mutex_lock(&socket->mutexClient);
	nError = WP2PCreateSocket ( W_P2P_TYPE_CLIENT, socket->serviceName, socket->sap, &(socket->handleSocket));
	(void)pthread_mutex_unlock(&socket->mutexClient);

	if (nError != W_SUCCESS) {
		LogInformation("doconnect(%d): WP2PCreateSocket failed (%x)", socket->index, nError);
		socket->status = SOCKET_ERROR;
		return JNI_FALSE;
	}

	/* Attempt to connect to remote server */
	WP2PConnect(socket->handleSocket, socket->handleLinkP2P, connectListener, socket);
	SEMAPHORE_WAIT(& (socket->semaphoreWaitConnectionStep2));

	if ((socket->status == SOCKET_ERROR) || (socket->status == SOCKET_AVAILABLE_SLOT))
	{
		LogError("doconnect(%d): Can't connect to the p2p device (sap=%d)", socket->index, socket->sap);
		return JNI_FALSE;
	}

	socket->status = SOCKET_CONNECTED;

	return JNI_TRUE;
}


static jboolean com_android_nfc_NativeLlcpSocket_doConnect(JNIEnv *e, jobject o, jint nSap)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e,o);

	Socket * socket = & (sockets[GET_SOCKET_INDEX(handle)]);

	socket->sap = nSap;
	socket->serviceName = NULL;
	
	return internal_doconnect(socket);
}

static jboolean com_android_nfc_NativeLlcpSocket_doConnectBy(JNIEnv *e, jobject o, jstring sn)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e,o);

	Socket * socket = & (sockets[GET_SOCKET_INDEX(handle)]);

	/* Service socket */
	if (sn == NULL)
	{
		LogError("The service name for p2p connection is not set");
		return JNI_FALSE;
	}

	int length = e->GetStringLength(sn);
	socket->serviceName = (char16_t*) malloc((length + 1) * sizeof(char16_t));
	if (socket->serviceName == NULL)
	{
		LogError("No memory for the service name for p2p connection");
		return JNI_FALSE;
	}
	const jchar *pSrcName = e->GetStringChars(sn, NULL);
	memcpy(socket->serviceName, pSrcName, length * sizeof(char16_t));
	e->ReleaseStringChars(sn, pSrcName);

	socket->serviceName[length] = 0;
	socket->sap = 0;

	return internal_doconnect(socket);
}

static jboolean com_android_nfc_NativeLlcpSocket_doClose(JNIEnv *e, jobject o)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e,o);
	if (IS_CLIENT_SOCKET(handle))
	{
		LogInformation("com_android_nfc_NativeLlcpSocket_doClose: handle=0x%X",handle);
		Socket * socket = & (sockets[GET_SOCKET_INDEX(handle)]);
		
		(void)pthread_mutex_lock(&socket->mutexClient);

		if (socket->handleSocket != W_NULL_HANDLE)  {
			WBasicCloseHandle(socket->handleSocket);
			socket->handleSocket = W_NULL_HANDLE;
		}
		
		if (socket->handleLinkP2P != W_NULL_HANDLE) {
			
			WBasicCloseHandle(socket->handleLinkP2P);
			socket->handleLinkP2P = W_NULL_HANDLE;
			
		} else if (socket->handleCancelEstablishLink != W_NULL_HANDLE) {

		WBasicCancelOperation(socket->handleCancelEstablishLink);
			(void)pthread_mutex_unlock(&socket->mutexClient);
			SEMAPHORE_WAIT(&(socket->semaphoreWaitConnection));
			(void)pthread_mutex_lock(&socket->mutexClient);
		}

		socket->status = SOCKET_AVAILABLE_SLOT;
		
		(void)pthread_mutex_unlock(&socket->mutexClient);

		if(socket->serviceName != NULL)
		{
			free(socket->serviceName);
			socket->serviceName = NULL;
		}
		socket->sap = 0;
	}
	return JNI_TRUE;
}

static jboolean com_android_nfc_NativeLlcpSocket_doSend(JNIEnv *e, jobject o, jbyteArray  data)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e,o);

	return socketSend(e, handle, data);
}

static jint com_android_nfc_NativeLlcpSocket_doReceive(JNIEnv *e, jobject o, jbyteArray  buffer)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e,o);

	LogInformation("NativeLlcpSocket_doReceive(): Handle JAVA=0x%X", handle);

	W_HANDLE handleOpenNFC = 0;

	if(IS_SERVER_SOCKET(handle))
	{
		if(isServerSoketConnected(handle)==W_FALSE)
		{
			return -1;
		}
		handleOpenNFC = obtainServerSocketHandleOpenNFC(handle);
	}
	else
	{
		if(sockets[GET_SOCKET_INDEX(handle)].status!=SOCKET_CONNECTED)
		{
			return -1;
		}

		handleOpenNFC=sockets[GET_SOCKET_INDEX(handle)].handleSocket;
	}

	LogInformation("Handle Open NFC=%x",handleOpenNFC);

	uint32_t dataLength = 0;

	uint8_t * receiveData = (uint8_t*)e->GetByteArrayElements(buffer, NULL);
	uint32_t receiveLength = (uint32_t)e->GetArrayLength(buffer);

	W_ERROR error = WP2PReadSync
	(
		handleOpenNFC,
		receiveData, receiveLength,
		& dataLength
	);

	e->ReleaseByteArrayElements(buffer, (jbyte*)receiveData, 0);
	if(error != W_SUCCESS)
	{
		LogError("Error in com_android_nfc_NativeLlcpSocket_doReceive error=0x%x",error);
		return -1;
	}
	return (jint)dataLength;
}

static jint com_android_nfc_NativeLlcpSocket_doGetRemoteSocketMIU(JNIEnv *e, jobject o)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e,o);
	Socket* pSocket = &sockets[GET_SOCKET_INDEX(handle)];
	uint32_t remoteMIU = 0;
	W_HANDLE handleOpenNFC;

	if(IS_SERVER_SOCKET(handle))
	{
		handleOpenNFC = obtainServerSocketHandleOpenNFC(handle);
	}
	else
	{
		handleOpenNFC = sockets[GET_SOCKET_INDEX(handle)].handleSocket;
	}

	LogInformation("doGetRemoteSocketMIU(): handleSocket = 0x%X, isServerSocket = %d",
			handleOpenNFC, IS_SERVER_SOCKET(handle));
	W_ERROR error = WP2PGetSocketParameter(handleOpenNFC, W_P2P_REMOTE_MIU, &remoteMIU);
	if (error != W_SUCCESS)
	{
		LogWarning("Can't get Remote MIU (error=0x%X)", error);
		remoteMIU = 0;
	}
	else
	{
		LogInformation("remoteMIU = %d", remoteMIU);
	}
	return (jint) remoteMIU;
}

static jint com_android_nfc_NativeLlcpSocket_doGetRemoteSocketRW(JNIEnv *e, jobject o)
{
	uint32_t handle = nfc_jni_get_nfc_socket_handle(e,o);
	Socket* pSocket = &sockets[GET_SOCKET_INDEX(handle)];
	uint32_t remoteRW = 0;
	LogInformation("doGetRemoteSocketRW(): handleSocket = 0x%X", pSocket->handleSocket);
	W_ERROR error = WP2PGetSocketParameter(pSocket->handleSocket, W_P2P_REMOTE_RW, &remoteRW);
	if (error != W_SUCCESS)
	{
		LogWarning("Can't get Remote RW (error=0x%X)", error);
		remoteRW = 0;
	}
	else
	{
		LogInformation("remoteRW = %d", remoteRW);
	}
	return (jint) remoteRW;
}


/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] =
{
   {"doConnect", "(I)Z",
      (void *)com_android_nfc_NativeLlcpSocket_doConnect},

   {"doConnectBy", "(Ljava/lang/String;)Z",
      (void *)com_android_nfc_NativeLlcpSocket_doConnectBy},
      
   {"doClose", "()Z",
      (void *)com_android_nfc_NativeLlcpSocket_doClose},
      
   {"doSend", "([B)Z",
      (void *)com_android_nfc_NativeLlcpSocket_doSend},

   {"doReceive", "([B)I",
      (void *)com_android_nfc_NativeLlcpSocket_doReceive},
      
   {"doGetRemoteSocketMiu", "()I",
      (void *)com_android_nfc_NativeLlcpSocket_doGetRemoteSocketMIU},
           
   {"doGetRemoteSocketRw", "()I",
      (void *)com_android_nfc_NativeLlcpSocket_doGetRemoteSocketRW},
};


int register_com_android_nfc_NativeLlcpSocket(JNIEnv *e)
{
   return jniRegisterNativeMethods(e,
      "com/android/nfc/dhimpl/NativeLlcpSocket",gMethods, NELEM(gMethods));
}

} // namespace android
