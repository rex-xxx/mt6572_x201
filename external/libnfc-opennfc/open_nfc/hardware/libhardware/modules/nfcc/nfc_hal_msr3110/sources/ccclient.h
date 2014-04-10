/*
 * Copyright (c) 2009-2011 Inside Secure, All Rights Reserved.
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

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include "porting_types.h"

#ifndef __CDECL
#  ifdef _WIN32
#     define __CDECL __cdecl
#  else
#     define __CDECL
#  endif
#endif

/*******************************************************************************
  Error codes
*******************************************************************************/

typedef enum __ccErrorCode {
   CC_SUCCESS                    =  0,    /* successfull connection */
   CC_ERROR_URI_SYNTAX           =  1,    /* syntax error in URI parameter */
   CC_ERROR_SOCKET_OPERATION     =  2,    /* error creating or configuring the TCP/IP socket */
   CC_ERROR_CONNECTION_FAILURE   =  3,    /* error connecting to the connection center */
   CC_ERROR_MEMORY               =  4,    /* memory allocation failure */
   CC_ERROR_EVENT_CREATION       =  5,    /* error creating or configuring the socket Event */
   CC_ERROR_COMMUNICATION        =  6,    /* error sending to / receiving from the Connection Center */
   CC_ERROR_PROTOCOL             =  7,    /* error in message received from Connection Center */
   CC_ERROR_CC_VERSION           =  8,    /* Connection Center with incompatible version */
   CC_ERROR_NO_PROVIDER          =  9,    /* connected to Connection Center, but no Service Provider yet connected */
   CC_ERROR_PROVIDER_BUSY        = 10,    /* connected to Connection Center, but Service Provider is already used */
   CC_ERROR_BAD_PARAMETER        = 11,    /* bad parameters */
   CC_ERROR_SERVICE_VERSION      = 12,    /* service provider found but the protocol version does not match */
} ccErrorCode;

/* Unknown Version value */
#define CC_UNKNOWN_VERSION ((uint8_t)0)

/* Maximum number of version supported */
#define CC_MAXIMUM_VERSION    30

/**
 * Extracts the protocol from the URI.
 *
 * The syntax of the URI is the following:
 *
 *    cc:[//<host>/]<protocol>[?[<query>][;<query>]*]
 *
 * where
 *   host ::= localhost|<host name>|<host address>
 *   query ::= <name=value> | <process=id>
 *
 * @param[in]  pProviderURI  The provider URI.
 *
 * @param[in]  pProtocolBuffer  The buffer receiving the protocol.
 *
 * @param[in]  nProtocolBufferLength  The buffer length in character.
 *
 * @return  W_TRUE in case of success, W_FALSE in case of error.
 **/
bool_t __CDECL CCClientGetProtocol(
            const char16_t* pProviderURI,
            char16_t* pProtocolBuffer,
            uint32_t nProtocolBufferLength);

/**
 * Opens a connection as client.
 *
 * @param[in] pProviderURI  The provider URI.
 *
 * @param[in] bWait  Wait for service provider (W_TRUE) or disconnect if no service provider is ready (W_FALSE).
 *
 * @param[in] pVersionSupported  The array of the version supported by the service client.
 *
 * @param[in] nVersionNumber  The number of version in the array pVersionSupported.
 *            This value is in the range ]0, CC_MAXIMUM_VERSION]
 *
 * @param[out] pnNegociatedVersion A pointer on the version negociated with the service provider.
 *             CC_UNKNOWN_VERSION if the version is unknown or if an error occured.
 *
 * @param[out] ppConnection  The connection, or null in case of error.
 *
 * @return CC_SUCCESS or the error code.
 **/
uint32_t __CDECL CCClientOpen(
            const char16_t* pProviderURI,
            bool_t bWait,
            const uint8_t* pVersionSupported,
            uint32_t nVersionNumber,
            uint8_t* pnNegociatedVersion,
            void** ppConnection);

/**
 * Opens a connection as service provider.
 *
 * @param[in] pServiceType  The service type.
 *
 * @param[in] pServiceName  The service name. This value may be null for the default name.
 *
 * @param[in] pVersionSupported  The array of the version supported by the service provider.
 *
 * @param[in] nVersionNumber  The number of version in the array pVersionSupported.
 *            This value is in the range ]0, CC_MAXIMUM_VERSION]
 *
 * @param[out] pnNegociatedVersion A pointer on the version negociated with the service client.
 *             CC_UNKNOWN_VERSION if the version is unknown or if an error occured.
 *
 * @param[out] ppConnection  The connection, or null in case of error.
 *
 * @return CC_SUCCESS or the error code.
 **/
uint32_t __CDECL CCClientOpenAsProvider(
            const char16_t* pServiceType,
            const char16_t* pServiceName,
            const uint8_t* pVersionSupported,
            uint32_t nVersionNumber,
            uint8_t* pnNegociatedVersion,
            void** ppConnection);

/**
 * Closes a connection.
 *
 * @param[in]  pConnection  The connection.
 **/
void __CDECL CCClientClose(
            void* pConnection);

/**
 * Sends some binary data.
 *
 * @param[in] pConnection  The connection.
 *
 * @param[in] pData  A pointer on the data buffer.
 *
 * @param[in] nDataLength  The length in bytes of the data.
 *
 * @return  Non zero value if the data is sent, zero if an error occured.
 **/
uint32_t __CDECL CCClientSendData(
            void* pConnection,
            const uint8_t* pData,
            uint32_t nDataLength);

/**
 * Sends some binary data.
 *
 * @param[in] pConnection  The connection.
 *
 * @param[in] nData1  The value of the first data buffer.
 *
 * @param[in] pData2  A pointer on the second data buffer.
 *
 * @param[in] nData2Length  The length in bytes of the second data buffer.
 *
 * @return  Non zero value if the data is sent, zero if an error occured.
 **/
uint32_t __CDECL CCClientSendDataEx(
            void* pConnection,
            const uint8_t nData1,
            const uint8_t* pData2,
            uint32_t nData2Length);

/**
 * Receives some binary data.
 *
 * @param[in]  pConnection  The connection.
 *
 * @param[in]  pBuffer  A pointer on the buffer used for the reception.
 *
 * @param[in]  nBufferLength  The length in bytes of the buffer.
 *
 * @param[out] ppData  A pointer on a variable valued with the address
 *             of the data in the buffer.
 *
* @param[in]  bWait  If W_TRUE, waits for data availability (blocking call)
 *
 * @return  The length in bytes of the data,
 *          zero if no data is received,
 *          a negative value if an error is detected.
 **/
int32_t __CDECL CCClientReceiveData(
            void* pConnection,
            uint8_t* pBuffer,
            uint32_t nBufferLength,
            uint8_t** ppData,
            bool_t bWait);

/**
 * Gets the reception event.
 *
 * @param[in]  pConnection  The connection.
 *
 * @return The reception event.
 **/
void* __CDECL CCClientGetReceptionEvent(
            void* pConnection);

/**
 * Sets the Connection Center address.
 *
 * If not set, the local-loop address is used.
 *
 * @param[in]  pAddress  The Connection Center address.
 **/
void __CDECL CCClientSetAddress(
            const char* pAddress);

/*******************************************************************************
  Print Functions
*******************************************************************************/

/* The trace levels */
#define P_TRACE_TRACE      1
#define P_TRACE_LOG        2
#define P_TRACE_WARNING    3
#define P_TRACE_ERROR      4
#define P_TRACE_NONE       5

/* See Functional Specifications Document */
void __CDECL CCTraceInit(void);

/* See Functional Specifications Document */
void __CDECL CCTracePrint(
            const char* pTag,
            uint32_t nTraceLevel,
            const char* pMessage,
            va_list list);

/* See Functional Specifications Document */
void __CDECL CCTracePrintBuffer(
            const char* pTag,
            uint32_t nTraceLevel,
            const uint8_t* pBuffer,
            uint32_t nLength);

#ifdef __cplusplus
}
#endif
