/*
 * Copyright (c) 2007-2012 Inside Secure, All Rights Reserved.
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

/*******************************************************************************
 File auto-generated with the autogen.exe tool - Do not modify manually
 The autogen.exe binary tool, the generation scripts and the files used
 for the source of the generation are available under Apache License, Version 2.0
 ******************************************************************************/

#ifndef __WME_SERVER_AUTOGEN_H
#define __WME_SERVER_AUTOGEN_H

#ifdef P_CONFIG_CLIENT_SERVER

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_DRIVER)

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverExchangeData 0

typedef struct __tMessage_in_P14P3DriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
   bool_t bCheckResponseCRC;
   bool_t bCheckAckOrNack;
} tMessage_in_P14P3DriverExchangeData;

typedef struct __tMessage_out_P14P3DriverExchangeData
{
   W_HANDLE value;
} tMessage_out_P14P3DriverExchangeData;

typedef union __tMessage_in_out_P14P3DriverExchangeData
{
   tMessage_in_P14P3DriverExchangeData in;
   tMessage_out_P14P3DriverExchangeData out;
} tMessage_in_out_P14P3DriverExchangeData;

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeRawBits()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverExchangeRawBits 1

typedef struct __tMessage_in_P14P3DriverExchangeRawBits
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t nReaderToCardBufferLastByteBitNumber;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
   uint8_t nExpectedBits;
} tMessage_in_P14P3DriverExchangeRawBits;

typedef struct __tMessage_out_P14P3DriverExchangeRawBits
{
   W_HANDLE value;
} tMessage_out_P14P3DriverExchangeRawBits;

typedef union __tMessage_in_out_P14P3DriverExchangeRawBits
{
   tMessage_in_P14P3DriverExchangeRawBits in;
   tMessage_out_P14P3DriverExchangeRawBits out;
} tMessage_in_out_P14P3DriverExchangeRawBits;

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeRawMifare()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverExchangeRawMifare 2

typedef struct __tMessage_in_P14P3DriverExchangeRawMifare
{
   W_HANDLE hConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tMessage_in_P14P3DriverExchangeRawMifare;

typedef struct __tMessage_out_P14P3DriverExchangeRawMifare
{
   W_HANDLE value;
} tMessage_out_P14P3DriverExchangeRawMifare;

typedef union __tMessage_in_out_P14P3DriverExchangeRawMifare
{
   tMessage_in_P14P3DriverExchangeRawMifare in;
   tMessage_out_P14P3DriverExchangeRawMifare out;
} tMessage_in_out_P14P3DriverExchangeRawMifare;

/* -----------------------------------------------------------------------------
      P14P3DriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverSetTimeout 3

typedef struct __tMessage_in_P14P3DriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tMessage_in_P14P3DriverSetTimeout;

typedef struct __tMessage_out_P14P3DriverSetTimeout
{
   W_ERROR value;
} tMessage_out_P14P3DriverSetTimeout;

typedef union __tMessage_in_out_P14P3DriverSetTimeout
{
   tMessage_in_P14P3DriverSetTimeout in;
   tMessage_out_P14P3DriverSetTimeout out;
} tMessage_in_out_P14P3DriverSetTimeout;

/* -----------------------------------------------------------------------------
      P14P4DriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P4DriverExchangeData 4

typedef struct __tMessage_in_P14P4DriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
   bool_t bSendNAD;
   uint8_t nNAD;
   bool_t bCreateOperation;
} tMessage_in_P14P4DriverExchangeData;

typedef struct __tMessage_out_P14P4DriverExchangeData
{
   W_HANDLE value;
} tMessage_out_P14P4DriverExchangeData;

typedef union __tMessage_in_out_P14P4DriverExchangeData
{
   tMessage_in_P14P4DriverExchangeData in;
   tMessage_out_P14P4DriverExchangeData out;
} tMessage_in_out_P14P4DriverExchangeData;

/* -----------------------------------------------------------------------------
      P14P4DriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P4DriverSetTimeout 5

typedef struct __tMessage_in_P14P4DriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tMessage_in_P14P4DriverSetTimeout;

typedef struct __tMessage_out_P14P4DriverSetTimeout
{
   W_ERROR value;
} tMessage_out_P14P4DriverSetTimeout;

typedef union __tMessage_in_out_P14P4DriverSetTimeout
{
   tMessage_in_P14P4DriverSetTimeout in;
   tMessage_out_P14P4DriverSetTimeout out;
} tMessage_in_out_P14P4DriverSetTimeout;

/* -----------------------------------------------------------------------------
      P15P3DriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_P15P3DriverExchangeData 6

typedef struct __tMessage_in_P15P3DriverExchangeData
{
   W_HANDLE hConnection;
   tP15P3DriverExchangeDataCompleted * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tMessage_in_P15P3DriverExchangeData;

typedef union __tMessage_in_out_P15P3DriverExchangeData
{
   tMessage_in_P15P3DriverExchangeData in;
} tMessage_in_out_P15P3DriverExchangeData;

/* -----------------------------------------------------------------------------
      P15P3DriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_P15P3DriverSetTimeout 7

typedef struct __tMessage_in_P15P3DriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tMessage_in_P15P3DriverSetTimeout;

typedef struct __tMessage_out_P15P3DriverSetTimeout
{
   W_ERROR value;
} tMessage_out_P15P3DriverSetTimeout;

typedef union __tMessage_in_out_P15P3DriverSetTimeout
{
   tMessage_in_P15P3DriverSetTimeout in;
   tMessage_out_P15P3DriverSetTimeout out;
} tMessage_in_out_P15P3DriverSetTimeout;

/* -----------------------------------------------------------------------------
      PBasicDriverCancelOperation()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBasicDriverCancelOperation 8

typedef struct __tMessage_in_PBasicDriverCancelOperation
{
   W_HANDLE hOperation;
} tMessage_in_PBasicDriverCancelOperation;

typedef union __tMessage_in_out_PBasicDriverCancelOperation
{
   tMessage_in_PBasicDriverCancelOperation in;
} tMessage_in_out_PBasicDriverCancelOperation;

/* -----------------------------------------------------------------------------
      PBasicDriverGetVersion()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBasicDriverGetVersion 9

typedef struct __tMessage_in_PBasicDriverGetVersion
{
   void * pBuffer;
   uint32_t nBufferSize;
} tMessage_in_PBasicDriverGetVersion;

typedef struct __tMessage_out_PBasicDriverGetVersion
{
   W_ERROR value;
} tMessage_out_PBasicDriverGetVersion;

typedef union __tMessage_in_out_PBasicDriverGetVersion
{
   tMessage_in_PBasicDriverGetVersion in;
   tMessage_out_PBasicDriverGetVersion out;
} tMessage_in_out_PBasicDriverGetVersion;

/* -----------------------------------------------------------------------------
      PBPrimeDriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBPrimeDriverExchangeData 10

typedef struct __tMessage_in_PBPrimeDriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tMessage_in_PBPrimeDriverExchangeData;

typedef struct __tMessage_out_PBPrimeDriverExchangeData
{
   W_HANDLE value;
} tMessage_out_PBPrimeDriverExchangeData;

typedef union __tMessage_in_out_PBPrimeDriverExchangeData
{
   tMessage_in_PBPrimeDriverExchangeData in;
   tMessage_out_PBPrimeDriverExchangeData out;
} tMessage_in_out_PBPrimeDriverExchangeData;

/* -----------------------------------------------------------------------------
      PBPrimeDriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBPrimeDriverSetTimeout 11

typedef struct __tMessage_in_PBPrimeDriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tMessage_in_PBPrimeDriverSetTimeout;

typedef struct __tMessage_out_PBPrimeDriverSetTimeout
{
   W_ERROR value;
} tMessage_out_PBPrimeDriverSetTimeout;

typedef union __tMessage_in_out_PBPrimeDriverSetTimeout
{
   tMessage_in_PBPrimeDriverSetTimeout in;
   tMessage_out_PBPrimeDriverSetTimeout out;
} tMessage_in_out_PBPrimeDriverSetTimeout;

/* -----------------------------------------------------------------------------
      PCacheConnectionDriverRead()
----------------------------------------------------------------------------- */

#define P_Idenfier_PCacheConnectionDriverRead 12

typedef struct __tMessage_in_PCacheConnectionDriverRead
{
   tCacheConnectionInstance * pCacheConnection;
   uint32_t nSize;
} tMessage_in_PCacheConnectionDriverRead;

typedef struct __tMessage_out_PCacheConnectionDriverRead
{
   W_ERROR value;
} tMessage_out_PCacheConnectionDriverRead;

typedef union __tMessage_in_out_PCacheConnectionDriverRead
{
   tMessage_in_PCacheConnectionDriverRead in;
   tMessage_out_PCacheConnectionDriverRead out;
} tMessage_in_out_PCacheConnectionDriverRead;

/* -----------------------------------------------------------------------------
      PCacheConnectionDriverWrite()
----------------------------------------------------------------------------- */

#define P_Idenfier_PCacheConnectionDriverWrite 13

typedef struct __tMessage_in_PCacheConnectionDriverWrite
{
   const tCacheConnectionInstance * pCacheConnection;
   uint32_t nSize;
} tMessage_in_PCacheConnectionDriverWrite;

typedef struct __tMessage_out_PCacheConnectionDriverWrite
{
   W_ERROR value;
} tMessage_out_PCacheConnectionDriverWrite;

typedef union __tMessage_in_out_PCacheConnectionDriverWrite
{
   tMessage_in_PCacheConnectionDriverWrite in;
   tMessage_out_PCacheConnectionDriverWrite out;
} tMessage_in_out_PCacheConnectionDriverWrite;

/* -----------------------------------------------------------------------------
      PContextDriverGenerateRandom()
----------------------------------------------------------------------------- */

#define P_Idenfier_PContextDriverGenerateRandom 14

typedef struct __tMessage_out_PContextDriverGenerateRandom
{
   uint32_t value;
} tMessage_out_PContextDriverGenerateRandom;

typedef union __tMessage_in_out_PContextDriverGenerateRandom
{
   tMessage_out_PContextDriverGenerateRandom out;
} tMessage_in_out_PContextDriverGenerateRandom;

/* -----------------------------------------------------------------------------
      PContextDriverGetMemoryStatistics()
----------------------------------------------------------------------------- */

#define P_Idenfier_PContextDriverGetMemoryStatistics 15

typedef struct __tMessage_in_PContextDriverGetMemoryStatistics
{
   tContextDriverMemoryStatistics * pStatistics;
   uint32_t nSize;
} tMessage_in_PContextDriverGetMemoryStatistics;

typedef union __tMessage_in_out_PContextDriverGetMemoryStatistics
{
   tMessage_in_PContextDriverGetMemoryStatistics in;
} tMessage_in_out_PContextDriverGetMemoryStatistics;

/* -----------------------------------------------------------------------------
      PContextDriverResetMemoryStatistics()
----------------------------------------------------------------------------- */

#define P_Idenfier_PContextDriverResetMemoryStatistics 16

/* -----------------------------------------------------------------------------
      PDFCDriverInterruptEventLoop()
----------------------------------------------------------------------------- */

#define P_Idenfier_PDFCDriverInterruptEventLoop 17

/* -----------------------------------------------------------------------------
      PDFCDriverStopEventLoop()
----------------------------------------------------------------------------- */

#define P_Idenfier_PDFCDriverStopEventLoop 18

/* -----------------------------------------------------------------------------
      PEmulCloseDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulCloseDriver 19

typedef struct __tMessage_in_PEmulCloseDriver
{
   W_HANDLE hHandle;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PEmulCloseDriver;

typedef union __tMessage_in_out_PEmulCloseDriver
{
   tMessage_in_PEmulCloseDriver in;
} tMessage_in_out_PEmulCloseDriver;

/* -----------------------------------------------------------------------------
      PEmulGetMessageData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulGetMessageData 20

typedef struct __tMessage_in_PEmulGetMessageData
{
   W_HANDLE hHandle;
   uint8_t * pDataBuffer;
   uint32_t nDataLength;
   uint32_t * pnActualDataLength;
} tMessage_in_PEmulGetMessageData;

typedef struct __tMessage_out_PEmulGetMessageData
{
   W_ERROR value;
} tMessage_out_PEmulGetMessageData;

typedef union __tMessage_in_out_PEmulGetMessageData
{
   tMessage_in_PEmulGetMessageData in;
   tMessage_out_PEmulGetMessageData out;
} tMessage_in_out_PEmulGetMessageData;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver1()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver1 21

typedef struct __tMessage_in_PEmulOpenConnectionDriver1
{
   tPBasicGenericCallbackFunction * pOpenCallback;
   void * pOpenCallbackParameter;
   const tWEmulConnectionInfo * pEmulConnectionInfo;
   uint32_t nSize;
   W_HANDLE * phHandle;
} tMessage_in_PEmulOpenConnectionDriver1;

typedef union __tMessage_in_out_PEmulOpenConnectionDriver1
{
   tMessage_in_PEmulOpenConnectionDriver1 in;
} tMessage_in_out_PEmulOpenConnectionDriver1;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver1Ex()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver1Ex 22

typedef struct __tMessage_in_PEmulOpenConnectionDriver1Ex
{
   tPBasicGenericCallbackFunction * pOpenCallback;
   void * pOpenCallbackParameter;
   const tWEmulConnectionInfo * pEmulConnectionInfo;
   uint32_t nSize;
   W_HANDLE * phHandle;
} tMessage_in_PEmulOpenConnectionDriver1Ex;

typedef union __tMessage_in_out_PEmulOpenConnectionDriver1Ex
{
   tMessage_in_PEmulOpenConnectionDriver1Ex in;
} tMessage_in_out_PEmulOpenConnectionDriver1Ex;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver2()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver2 23

typedef struct __tMessage_in_PEmulOpenConnectionDriver2
{
   W_HANDLE hHandle;
   tPEmulDriverEventReceived * pEventCallback;
   void * pEventCallbackParameter;
} tMessage_in_PEmulOpenConnectionDriver2;

typedef union __tMessage_in_out_PEmulOpenConnectionDriver2
{
   tMessage_in_PEmulOpenConnectionDriver2 in;
} tMessage_in_out_PEmulOpenConnectionDriver2;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver2Ex()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver2Ex 24

typedef struct __tMessage_in_PEmulOpenConnectionDriver2Ex
{
   W_HANDLE hHandle;
   tPEmulDriverEventReceived * pEventCallback;
   void * pEventCallbackParameter;
} tMessage_in_PEmulOpenConnectionDriver2Ex;

typedef union __tMessage_in_out_PEmulOpenConnectionDriver2Ex
{
   tMessage_in_PEmulOpenConnectionDriver2Ex in;
} tMessage_in_out_PEmulOpenConnectionDriver2Ex;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver3()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver3 25

typedef struct __tMessage_in_PEmulOpenConnectionDriver3
{
   W_HANDLE hHandle;
   tPEmulDriverCommandReceived * pCommandCallback;
   void * pCommandCallbackParameter;
} tMessage_in_PEmulOpenConnectionDriver3;

typedef union __tMessage_in_out_PEmulOpenConnectionDriver3
{
   tMessage_in_PEmulOpenConnectionDriver3 in;
} tMessage_in_out_PEmulOpenConnectionDriver3;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver3Ex()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver3Ex 26

typedef struct __tMessage_in_PEmulOpenConnectionDriver3Ex
{
   W_HANDLE hHandle;
   tPEmulDriverCommandReceived * pCommandCallback;
   void * pCommandCallbackParameter;
} tMessage_in_PEmulOpenConnectionDriver3Ex;

typedef union __tMessage_in_out_PEmulOpenConnectionDriver3Ex
{
   tMessage_in_PEmulOpenConnectionDriver3Ex in;
} tMessage_in_out_PEmulOpenConnectionDriver3Ex;

/* -----------------------------------------------------------------------------
      PEmulSendAnswer()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulSendAnswer 27

typedef struct __tMessage_in_PEmulSendAnswer
{
   W_HANDLE hDriverConnection;
   const uint8_t * pDataBuffer;
   uint32_t nDataLength;
} tMessage_in_PEmulSendAnswer;

typedef struct __tMessage_out_PEmulSendAnswer
{
   W_ERROR value;
} tMessage_out_PEmulSendAnswer;

typedef union __tMessage_in_out_PEmulSendAnswer
{
   tMessage_in_PEmulSendAnswer in;
   tMessage_out_PEmulSendAnswer out;
} tMessage_in_out_PEmulSendAnswer;

/* -----------------------------------------------------------------------------
      PFeliCaDriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PFeliCaDriverExchangeData 28

typedef struct __tMessage_in_PFeliCaDriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tMessage_in_PFeliCaDriverExchangeData;

typedef union __tMessage_in_out_PFeliCaDriverExchangeData
{
   tMessage_in_PFeliCaDriverExchangeData in;
} tMessage_in_out_PFeliCaDriverExchangeData;

/* -----------------------------------------------------------------------------
      PFeliCaDriverGetCardList()
----------------------------------------------------------------------------- */

#define P_Idenfier_PFeliCaDriverGetCardList 29

typedef struct __tMessage_in_PFeliCaDriverGetCardList
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tMessage_in_PFeliCaDriverGetCardList;

typedef union __tMessage_in_out_PFeliCaDriverGetCardList
{
   tMessage_in_PFeliCaDriverGetCardList in;
} tMessage_in_out_PFeliCaDriverGetCardList;

/* -----------------------------------------------------------------------------
      PHandleCheckPropertyDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleCheckPropertyDriver 30

typedef struct __tMessage_in_PHandleCheckPropertyDriver
{
   W_HANDLE hObject;
   uint8_t nPropertyValue;
} tMessage_in_PHandleCheckPropertyDriver;

typedef struct __tMessage_out_PHandleCheckPropertyDriver
{
   W_ERROR value;
} tMessage_out_PHandleCheckPropertyDriver;

typedef union __tMessage_in_out_PHandleCheckPropertyDriver
{
   tMessage_in_PHandleCheckPropertyDriver in;
   tMessage_out_PHandleCheckPropertyDriver out;
} tMessage_in_out_PHandleCheckPropertyDriver;

/* -----------------------------------------------------------------------------
      PHandleCloseDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleCloseDriver 31

typedef struct __tMessage_in_PHandleCloseDriver
{
   W_HANDLE hObject;
} tMessage_in_PHandleCloseDriver;

typedef union __tMessage_in_out_PHandleCloseDriver
{
   tMessage_in_PHandleCloseDriver in;
} tMessage_in_out_PHandleCloseDriver;

/* -----------------------------------------------------------------------------
      PHandleCloseSafeDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleCloseSafeDriver 32

typedef struct __tMessage_in_PHandleCloseSafeDriver
{
   W_HANDLE hObject;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PHandleCloseSafeDriver;

typedef union __tMessage_in_out_PHandleCloseSafeDriver
{
   tMessage_in_PHandleCloseSafeDriver in;
} tMessage_in_out_PHandleCloseSafeDriver;

/* -----------------------------------------------------------------------------
      PHandleGetCountDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleGetCountDriver 33

typedef struct __tMessage_out_PHandleGetCountDriver
{
   uint32_t value;
} tMessage_out_PHandleGetCountDriver;

typedef union __tMessage_in_out_PHandleGetCountDriver
{
   tMessage_out_PHandleGetCountDriver out;
} tMessage_in_out_PHandleGetCountDriver;

/* -----------------------------------------------------------------------------
      PHandleGetPropertiesDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleGetPropertiesDriver 34

typedef struct __tMessage_in_PHandleGetPropertiesDriver
{
   W_HANDLE hObject;
   uint8_t * pPropertyArray;
   uint32_t nPropertyArrayLength;
} tMessage_in_PHandleGetPropertiesDriver;

typedef struct __tMessage_out_PHandleGetPropertiesDriver
{
   W_ERROR value;
} tMessage_out_PHandleGetPropertiesDriver;

typedef union __tMessage_in_out_PHandleGetPropertiesDriver
{
   tMessage_in_PHandleGetPropertiesDriver in;
   tMessage_out_PHandleGetPropertiesDriver out;
} tMessage_in_out_PHandleGetPropertiesDriver;

/* -----------------------------------------------------------------------------
      PHandleGetPropertyNumberDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleGetPropertyNumberDriver 35

typedef struct __tMessage_in_PHandleGetPropertyNumberDriver
{
   W_HANDLE hObject;
   uint32_t * pnPropertyNumber;
} tMessage_in_PHandleGetPropertyNumberDriver;

typedef struct __tMessage_out_PHandleGetPropertyNumberDriver
{
   W_ERROR value;
} tMessage_out_PHandleGetPropertyNumberDriver;

typedef union __tMessage_in_out_PHandleGetPropertyNumberDriver
{
   tMessage_in_PHandleGetPropertyNumberDriver in;
   tMessage_out_PHandleGetPropertyNumberDriver out;
} tMessage_in_out_PHandleGetPropertyNumberDriver;

/* -----------------------------------------------------------------------------
      PMultiTimerCancelDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PMultiTimerCancelDriver 36

typedef struct __tMessage_in_PMultiTimerCancelDriver
{
   uint32_t nTimerIdentifier;
} tMessage_in_PMultiTimerCancelDriver;

typedef union __tMessage_in_out_PMultiTimerCancelDriver
{
   tMessage_in_PMultiTimerCancelDriver in;
} tMessage_in_out_PMultiTimerCancelDriver;

/* -----------------------------------------------------------------------------
      PMultiTimerSetDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PMultiTimerSetDriver 37

typedef struct __tMessage_in_PMultiTimerSetDriver
{
   uint32_t nTimerIdentifier;
   uint32_t nAbsoluteTimeout;
   tPBasicGenericCompletionFunction * pCallbackFunction;
   void * pCallbackParameter;
} tMessage_in_PMultiTimerSetDriver;

typedef union __tMessage_in_out_PMultiTimerSetDriver
{
   tMessage_in_PMultiTimerSetDriver in;
} tMessage_in_out_PMultiTimerSetDriver;

/* -----------------------------------------------------------------------------
      PNALServiceDriverGetCurrentTime()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNALServiceDriverGetCurrentTime 38

typedef struct __tMessage_out_PNALServiceDriverGetCurrentTime
{
   uint32_t value;
} tMessage_out_PNALServiceDriverGetCurrentTime;

typedef union __tMessage_in_out_PNALServiceDriverGetCurrentTime
{
   tMessage_out_PNALServiceDriverGetCurrentTime out;
} tMessage_in_out_PNALServiceDriverGetCurrentTime;

/* -----------------------------------------------------------------------------
      PNALServiceDriverGetProtocolStatistics()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNALServiceDriverGetProtocolStatistics 39

typedef struct __tMessage_in_PNALServiceDriverGetProtocolStatistics
{
   tNALProtocolStatistics * pStatistics;
   uint32_t nSize;
} tMessage_in_PNALServiceDriverGetProtocolStatistics;

typedef union __tMessage_in_out_PNALServiceDriverGetProtocolStatistics
{
   tMessage_in_PNALServiceDriverGetProtocolStatistics in;
} tMessage_in_out_PNALServiceDriverGetProtocolStatistics;

/* -----------------------------------------------------------------------------
      PNALServiceDriverResetProtocolStatistics()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNALServiceDriverResetProtocolStatistics 40

/* -----------------------------------------------------------------------------
      PNDEFRegisterNPPMessageHandlerDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRegisterNPPMessageHandlerDriver 41

typedef struct __tMessage_in_PNDEFRegisterNPPMessageHandlerDriver
{
   tPBasicGenericDataCallbackFunction * pHandler;
   void * pHandlerParameter;
   uint8_t nPriority;
   W_HANDLE * phRegistry;
} tMessage_in_PNDEFRegisterNPPMessageHandlerDriver;

typedef struct __tMessage_out_PNDEFRegisterNPPMessageHandlerDriver
{
   W_ERROR value;
} tMessage_out_PNDEFRegisterNPPMessageHandlerDriver;

typedef union __tMessage_in_out_PNDEFRegisterNPPMessageHandlerDriver
{
   tMessage_in_PNDEFRegisterNPPMessageHandlerDriver in;
   tMessage_out_PNDEFRegisterNPPMessageHandlerDriver out;
} tMessage_in_out_PNDEFRegisterNPPMessageHandlerDriver;

/* -----------------------------------------------------------------------------
      PNDEFRegisterSNEPMessageHandlerDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRegisterSNEPMessageHandlerDriver 42

typedef struct __tMessage_in_PNDEFRegisterSNEPMessageHandlerDriver
{
   tPBasicGenericDataCallbackFunction * pHandler;
   void * pHandlerParameter;
   uint8_t nPriority;
   W_HANDLE * phRegistry;
} tMessage_in_PNDEFRegisterSNEPMessageHandlerDriver;

typedef struct __tMessage_out_PNDEFRegisterSNEPMessageHandlerDriver
{
   W_ERROR value;
} tMessage_out_PNDEFRegisterSNEPMessageHandlerDriver;

typedef union __tMessage_in_out_PNDEFRegisterSNEPMessageHandlerDriver
{
   tMessage_in_PNDEFRegisterSNEPMessageHandlerDriver in;
   tMessage_out_PNDEFRegisterSNEPMessageHandlerDriver out;
} tMessage_in_out_PNDEFRegisterSNEPMessageHandlerDriver;

/* -----------------------------------------------------------------------------
      PNDEFRetrieveNPPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRetrieveNPPMessageDriver 43

typedef struct __tMessage_in_PNDEFRetrieveNPPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
} tMessage_in_PNDEFRetrieveNPPMessageDriver;

typedef struct __tMessage_out_PNDEFRetrieveNPPMessageDriver
{
   W_ERROR value;
} tMessage_out_PNDEFRetrieveNPPMessageDriver;

typedef union __tMessage_in_out_PNDEFRetrieveNPPMessageDriver
{
   tMessage_in_PNDEFRetrieveNPPMessageDriver in;
   tMessage_out_PNDEFRetrieveNPPMessageDriver out;
} tMessage_in_out_PNDEFRetrieveNPPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFRetrieveSNEPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRetrieveSNEPMessageDriver 44

typedef struct __tMessage_in_PNDEFRetrieveSNEPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
} tMessage_in_PNDEFRetrieveSNEPMessageDriver;

typedef struct __tMessage_out_PNDEFRetrieveSNEPMessageDriver
{
   W_ERROR value;
} tMessage_out_PNDEFRetrieveSNEPMessageDriver;

typedef union __tMessage_in_out_PNDEFRetrieveSNEPMessageDriver
{
   tMessage_in_PNDEFRetrieveSNEPMessageDriver in;
   tMessage_out_PNDEFRetrieveSNEPMessageDriver out;
} tMessage_in_out_PNDEFRetrieveSNEPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFSendNPPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSendNPPMessageDriver 45

typedef struct __tMessage_in_PNDEFSendNPPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PNDEFSendNPPMessageDriver;

typedef struct __tMessage_out_PNDEFSendNPPMessageDriver
{
   W_HANDLE value;
} tMessage_out_PNDEFSendNPPMessageDriver;

typedef union __tMessage_in_out_PNDEFSendNPPMessageDriver
{
   tMessage_in_PNDEFSendNPPMessageDriver in;
   tMessage_out_PNDEFSendNPPMessageDriver out;
} tMessage_in_out_PNDEFSendNPPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFSendSNEPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSendSNEPMessageDriver 46

typedef struct __tMessage_in_PNDEFSendSNEPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PNDEFSendSNEPMessageDriver;

typedef struct __tMessage_out_PNDEFSendSNEPMessageDriver
{
   W_HANDLE value;
} tMessage_out_PNDEFSendSNEPMessageDriver;

typedef union __tMessage_in_out_PNDEFSendSNEPMessageDriver
{
   tMessage_in_PNDEFSendSNEPMessageDriver in;
   tMessage_out_PNDEFSendSNEPMessageDriver out;
} tMessage_in_out_PNDEFSendSNEPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFSetWorkPerformedNPPDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSetWorkPerformedNPPDriver 47

typedef struct __tMessage_in_PNDEFSetWorkPerformedNPPDriver
{
   bool_t bGiveToNextListener;
} tMessage_in_PNDEFSetWorkPerformedNPPDriver;

typedef union __tMessage_in_out_PNDEFSetWorkPerformedNPPDriver
{
   tMessage_in_PNDEFSetWorkPerformedNPPDriver in;
} tMessage_in_out_PNDEFSetWorkPerformedNPPDriver;

/* -----------------------------------------------------------------------------
      PNDEFSetWorkPerformedSNEPDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSetWorkPerformedSNEPDriver 48

typedef struct __tMessage_in_PNDEFSetWorkPerformedSNEPDriver
{
   bool_t bGiveToNextListener;
} tMessage_in_PNDEFSetWorkPerformedSNEPDriver;

typedef union __tMessage_in_out_PNDEFSetWorkPerformedSNEPDriver
{
   tMessage_in_PNDEFSetWorkPerformedSNEPDriver in;
} tMessage_in_out_PNDEFSetWorkPerformedSNEPDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerDriverGetRFActivity()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerDriverGetRFActivity 49

typedef struct __tMessage_out_PNFCControllerDriverGetRFActivity
{
   uint32_t value;
} tMessage_out_PNFCControllerDriverGetRFActivity;

typedef union __tMessage_in_out_PNFCControllerDriverGetRFActivity
{
   tMessage_out_PNFCControllerDriverGetRFActivity out;
} tMessage_in_out_PNFCControllerDriverGetRFActivity;

/* -----------------------------------------------------------------------------
      PNFCControllerDriverGetRFLock()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerDriverGetRFLock 50

typedef struct __tMessage_in_PNFCControllerDriverGetRFLock
{
   uint32_t nLockSet;
} tMessage_in_PNFCControllerDriverGetRFLock;

typedef struct __tMessage_out_PNFCControllerDriverGetRFLock
{
   uint32_t value;
} tMessage_out_PNFCControllerDriverGetRFLock;

typedef union __tMessage_in_out_PNFCControllerDriverGetRFLock
{
   tMessage_in_PNFCControllerDriverGetRFLock in;
   tMessage_out_PNFCControllerDriverGetRFLock out;
} tMessage_in_out_PNFCControllerDriverGetRFLock;

/* -----------------------------------------------------------------------------
      PNFCControllerDriverReadInfo()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerDriverReadInfo 51

typedef struct __tMessage_in_PNFCControllerDriverReadInfo
{
   void * pBuffer;
   uint32_t nBufferSize;
} tMessage_in_PNFCControllerDriverReadInfo;

typedef struct __tMessage_out_PNFCControllerDriverReadInfo
{
   W_ERROR value;
} tMessage_out_PNFCControllerDriverReadInfo;

typedef union __tMessage_in_out_PNFCControllerDriverReadInfo
{
   tMessage_in_PNFCControllerDriverReadInfo in;
   tMessage_out_PNFCControllerDriverReadInfo out;
} tMessage_in_out_PNFCControllerDriverReadInfo;

/* -----------------------------------------------------------------------------
      PNFCControllerFirmwareUpdateDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerFirmwareUpdateDriver 52

typedef struct __tMessage_in_PNFCControllerFirmwareUpdateDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pUpdateBuffer;
   uint32_t nUpdateBufferLength;
   uint32_t nMode;
} tMessage_in_PNFCControllerFirmwareUpdateDriver;

typedef union __tMessage_in_out_PNFCControllerFirmwareUpdateDriver
{
   tMessage_in_PNFCControllerFirmwareUpdateDriver in;
} tMessage_in_out_PNFCControllerFirmwareUpdateDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerFirmwareUpdateState()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerFirmwareUpdateState 53

typedef struct __tMessage_out_PNFCControllerFirmwareUpdateState
{
   uint32_t value;
} tMessage_out_PNFCControllerFirmwareUpdateState;

typedef union __tMessage_in_out_PNFCControllerFirmwareUpdateState
{
   tMessage_out_PNFCControllerFirmwareUpdateState out;
} tMessage_in_out_PNFCControllerFirmwareUpdateState;

/* -----------------------------------------------------------------------------
      PNFCControllerGetMode()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerGetMode 54

typedef struct __tMessage_out_PNFCControllerGetMode
{
   uint32_t value;
} tMessage_out_PNFCControllerGetMode;

typedef union __tMessage_in_out_PNFCControllerGetMode
{
   tMessage_out_PNFCControllerGetMode out;
} tMessage_in_out_PNFCControllerGetMode;

/* -----------------------------------------------------------------------------
      PNFCControllerGetRawMessageData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerGetRawMessageData 55

typedef struct __tMessage_in_PNFCControllerGetRawMessageData
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   uint32_t * pnActualLength;
} tMessage_in_PNFCControllerGetRawMessageData;

typedef struct __tMessage_out_PNFCControllerGetRawMessageData
{
   W_ERROR value;
} tMessage_out_PNFCControllerGetRawMessageData;

typedef union __tMessage_in_out_PNFCControllerGetRawMessageData
{
   tMessage_in_PNFCControllerGetRawMessageData in;
   tMessage_out_PNFCControllerGetRawMessageData out;
} tMessage_in_out_PNFCControllerGetRawMessageData;

/* -----------------------------------------------------------------------------
      PNFCControllerIsActive()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerIsActive 56

typedef struct __tMessage_out_PNFCControllerIsActive
{
   bool_t value;
} tMessage_out_PNFCControllerIsActive;

typedef union __tMessage_in_out_PNFCControllerIsActive
{
   tMessage_out_PNFCControllerIsActive out;
} tMessage_in_out_PNFCControllerIsActive;

/* -----------------------------------------------------------------------------
      PNFCControllerMonitorException()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerMonitorException 57

typedef struct __tMessage_in_PNFCControllerMonitorException
{
   tPBasicGenericEventHandler * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tMessage_in_PNFCControllerMonitorException;

typedef struct __tMessage_out_PNFCControllerMonitorException
{
   W_ERROR value;
} tMessage_out_PNFCControllerMonitorException;

typedef union __tMessage_in_out_PNFCControllerMonitorException
{
   tMessage_in_PNFCControllerMonitorException in;
   tMessage_out_PNFCControllerMonitorException out;
} tMessage_in_out_PNFCControllerMonitorException;

/* -----------------------------------------------------------------------------
      PNFCControllerMonitorFieldEvents()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerMonitorFieldEvents 58

typedef struct __tMessage_in_PNFCControllerMonitorFieldEvents
{
   tPBasicGenericEventHandler * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tMessage_in_PNFCControllerMonitorFieldEvents;

typedef struct __tMessage_out_PNFCControllerMonitorFieldEvents
{
   W_ERROR value;
} tMessage_out_PNFCControllerMonitorFieldEvents;

typedef union __tMessage_in_out_PNFCControllerMonitorFieldEvents
{
   tMessage_in_PNFCControllerMonitorFieldEvents in;
   tMessage_out_PNFCControllerMonitorFieldEvents out;
} tMessage_in_out_PNFCControllerMonitorFieldEvents;

/* -----------------------------------------------------------------------------
      PNFCControllerProductionTestDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerProductionTestDriver 59

typedef struct __tMessage_in_PNFCControllerProductionTestDriver
{
   const uint8_t * pParameterBuffer;
   uint32_t nParameterBufferLength;
   uint8_t * pResultBuffer;
   uint32_t nResultBufferLength;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PNFCControllerProductionTestDriver;

typedef union __tMessage_in_out_PNFCControllerProductionTestDriver
{
   tMessage_in_PNFCControllerProductionTestDriver in;
} tMessage_in_out_PNFCControllerProductionTestDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerRegisterRawListener()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerRegisterRawListener 60

typedef struct __tMessage_in_PNFCControllerRegisterRawListener
{
   tPBasicGenericDataCallbackFunction * pReceiveMessageEventHandler;
   void * pHandlerParameter;
} tMessage_in_PNFCControllerRegisterRawListener;

typedef struct __tMessage_out_PNFCControllerRegisterRawListener
{
   W_ERROR value;
} tMessage_out_PNFCControllerRegisterRawListener;

typedef union __tMessage_in_out_PNFCControllerRegisterRawListener
{
   tMessage_in_PNFCControllerRegisterRawListener in;
   tMessage_out_PNFCControllerRegisterRawListener out;
} tMessage_in_out_PNFCControllerRegisterRawListener;

/* -----------------------------------------------------------------------------
      PNFCControllerResetDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerResetDriver 61

typedef struct __tMessage_in_PNFCControllerResetDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint32_t nMode;
} tMessage_in_PNFCControllerResetDriver;

typedef union __tMessage_in_out_PNFCControllerResetDriver
{
   tMessage_in_PNFCControllerResetDriver in;
} tMessage_in_out_PNFCControllerResetDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerSelfTestDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSelfTestDriver 62

typedef struct __tMessage_in_PNFCControllerSelfTestDriver
{
   tPNFCControllerSelfTestCompleted * pCallback;
   void * pCallbackParameter;
} tMessage_in_PNFCControllerSelfTestDriver;

typedef union __tMessage_in_out_PNFCControllerSelfTestDriver
{
   tMessage_in_PNFCControllerSelfTestDriver in;
} tMessage_in_out_PNFCControllerSelfTestDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerSetRFLockDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSetRFLockDriver 63

typedef struct __tMessage_in_PNFCControllerSetRFLockDriver
{
   uint32_t nLockSet;
   bool_t bReaderLock;
   bool_t bCardLock;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PNFCControllerSetRFLockDriver;

typedef union __tMessage_in_out_PNFCControllerSetRFLockDriver
{
   tMessage_in_PNFCControllerSetRFLockDriver in;
} tMessage_in_out_PNFCControllerSetRFLockDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerSwitchStandbyMode()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSwitchStandbyMode 64

typedef struct __tMessage_in_PNFCControllerSwitchStandbyMode
{
   bool_t bStandbyOn;
} tMessage_in_PNFCControllerSwitchStandbyMode;

typedef struct __tMessage_out_PNFCControllerSwitchStandbyMode
{
   W_ERROR value;
} tMessage_out_PNFCControllerSwitchStandbyMode;

typedef union __tMessage_in_out_PNFCControllerSwitchStandbyMode
{
   tMessage_in_PNFCControllerSwitchStandbyMode in;
   tMessage_out_PNFCControllerSwitchStandbyMode out;
} tMessage_in_out_PNFCControllerSwitchStandbyMode;

/* -----------------------------------------------------------------------------
      PNFCControllerSwitchToRawModeDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSwitchToRawModeDriver 65

typedef struct __tMessage_in_PNFCControllerSwitchToRawModeDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PNFCControllerSwitchToRawModeDriver;

typedef union __tMessage_in_out_PNFCControllerSwitchToRawModeDriver
{
   tMessage_in_PNFCControllerSwitchToRawModeDriver in;
} tMessage_in_out_PNFCControllerSwitchToRawModeDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerWriteRawMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerWriteRawMessageDriver 66

typedef struct __tMessage_in_PNFCControllerWriteRawMessageDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pBuffer;
   uint32_t nLength;
} tMessage_in_PNFCControllerWriteRawMessageDriver;

typedef union __tMessage_in_out_PNFCControllerWriteRawMessageDriver
{
   tMessage_in_PNFCControllerWriteRawMessageDriver in;
} tMessage_in_out_PNFCControllerWriteRawMessageDriver;

/* -----------------------------------------------------------------------------
      PP2PConnectDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PConnectDriver 67

typedef struct __tMessage_in_PP2PConnectDriver
{
   W_HANDLE hSocket;
   W_HANDLE hLink;
   tPBasicGenericCallbackFunction * pEstablishmentCallback;
   void * pEstablishmentCallbackParameter;
} tMessage_in_PP2PConnectDriver;

typedef union __tMessage_in_out_PP2PConnectDriver
{
   tMessage_in_PP2PConnectDriver in;
} tMessage_in_out_PP2PConnectDriver;

/* -----------------------------------------------------------------------------
      PP2PCreateSocketDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PCreateSocketDriver 68

typedef struct __tMessage_in_PP2PCreateSocketDriver
{
   uint8_t nType;
   const char16_t * pServiceURI;
   uint32_t nSize;
   uint8_t nSAP;
   W_HANDLE * phSocket;
} tMessage_in_PP2PCreateSocketDriver;

typedef struct __tMessage_out_PP2PCreateSocketDriver
{
   W_ERROR value;
} tMessage_out_PP2PCreateSocketDriver;

typedef union __tMessage_in_out_PP2PCreateSocketDriver
{
   tMessage_in_PP2PCreateSocketDriver in;
   tMessage_out_PP2PCreateSocketDriver out;
} tMessage_in_out_PP2PCreateSocketDriver;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver1()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver1 69

typedef struct __tMessage_in_PP2PEstablishLinkDriver1
{
   tPBasicGenericHandleCallbackFunction * pEstablishmentCallback;
   void * pEstablishmentCallbackParameter;
} tMessage_in_PP2PEstablishLinkDriver1;

typedef struct __tMessage_out_PP2PEstablishLinkDriver1
{
   W_HANDLE value;
} tMessage_out_PP2PEstablishLinkDriver1;

typedef union __tMessage_in_out_PP2PEstablishLinkDriver1
{
   tMessage_in_PP2PEstablishLinkDriver1 in;
   tMessage_out_PP2PEstablishLinkDriver1 out;
} tMessage_in_out_PP2PEstablishLinkDriver1;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver1Wrapper()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver1Wrapper 70

typedef struct __tMessage_in_PP2PEstablishLinkDriver1Wrapper
{
   tPBasicGenericHandleCallbackFunction * pEstablishmentCallback;
   void * pEstablishmentCallbackParameter;
} tMessage_in_PP2PEstablishLinkDriver1Wrapper;

typedef struct __tMessage_out_PP2PEstablishLinkDriver1Wrapper
{
   W_HANDLE value;
} tMessage_out_PP2PEstablishLinkDriver1Wrapper;

typedef union __tMessage_in_out_PP2PEstablishLinkDriver1Wrapper
{
   tMessage_in_PP2PEstablishLinkDriver1Wrapper in;
   tMessage_out_PP2PEstablishLinkDriver1Wrapper out;
} tMessage_in_out_PP2PEstablishLinkDriver1Wrapper;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver2()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver2 71

typedef struct __tMessage_in_PP2PEstablishLinkDriver2
{
   W_HANDLE hLink;
   tPBasicGenericCallbackFunction * pReleaseCallback;
   void * pReleaseCallbackParameter;
   W_HANDLE * phOperation;
} tMessage_in_PP2PEstablishLinkDriver2;

typedef union __tMessage_in_out_PP2PEstablishLinkDriver2
{
   tMessage_in_PP2PEstablishLinkDriver2 in;
} tMessage_in_out_PP2PEstablishLinkDriver2;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver2Wrapper()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver2Wrapper 72

typedef struct __tMessage_in_PP2PEstablishLinkDriver2Wrapper
{
   W_HANDLE hLink;
   tPBasicGenericCallbackFunction * pReleaseCallback;
   void * pReleaseCallbackParameter;
   W_HANDLE * phOperation;
} tMessage_in_PP2PEstablishLinkDriver2Wrapper;

typedef union __tMessage_in_out_PP2PEstablishLinkDriver2Wrapper
{
   tMessage_in_PP2PEstablishLinkDriver2Wrapper in;
} tMessage_in_out_PP2PEstablishLinkDriver2Wrapper;

/* -----------------------------------------------------------------------------
      PP2PGetConfigurationDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PGetConfigurationDriver 73

typedef struct __tMessage_in_PP2PGetConfigurationDriver
{
   tWP2PConfiguration * pConfiguration;
   uint32_t nSize;
} tMessage_in_PP2PGetConfigurationDriver;

typedef struct __tMessage_out_PP2PGetConfigurationDriver
{
   W_ERROR value;
} tMessage_out_PP2PGetConfigurationDriver;

typedef union __tMessage_in_out_PP2PGetConfigurationDriver
{
   tMessage_in_PP2PGetConfigurationDriver in;
   tMessage_out_PP2PGetConfigurationDriver out;
} tMessage_in_out_PP2PGetConfigurationDriver;

/* -----------------------------------------------------------------------------
      PP2PGetLinkPropertiesDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PGetLinkPropertiesDriver 74

typedef struct __tMessage_in_PP2PGetLinkPropertiesDriver
{
   W_HANDLE hLink;
   tWP2PLinkProperties * pProperties;
   uint32_t nSize;
} tMessage_in_PP2PGetLinkPropertiesDriver;

typedef struct __tMessage_out_PP2PGetLinkPropertiesDriver
{
   W_ERROR value;
} tMessage_out_PP2PGetLinkPropertiesDriver;

typedef union __tMessage_in_out_PP2PGetLinkPropertiesDriver
{
   tMessage_in_PP2PGetLinkPropertiesDriver in;
   tMessage_out_PP2PGetLinkPropertiesDriver out;
} tMessage_in_out_PP2PGetLinkPropertiesDriver;

/* -----------------------------------------------------------------------------
      PP2PGetSocketParameterDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PGetSocketParameterDriver 75

typedef struct __tMessage_in_PP2PGetSocketParameterDriver
{
   W_HANDLE hSocket;
   uint32_t nParameter;
   uint32_t * pnValue;
} tMessage_in_PP2PGetSocketParameterDriver;

typedef struct __tMessage_out_PP2PGetSocketParameterDriver
{
   W_ERROR value;
} tMessage_out_PP2PGetSocketParameterDriver;

typedef union __tMessage_in_out_PP2PGetSocketParameterDriver
{
   tMessage_in_PP2PGetSocketParameterDriver in;
   tMessage_out_PP2PGetSocketParameterDriver out;
} tMessage_in_out_PP2PGetSocketParameterDriver;

/* -----------------------------------------------------------------------------
      PP2PReadDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PReadDriver 76

typedef struct __tMessage_in_PP2PReadDriver
{
   W_HANDLE hConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint8_t * pReceptionBuffer;
   uint32_t nReceptionBufferLength;
   W_HANDLE * phOperation;
} tMessage_in_PP2PReadDriver;

typedef union __tMessage_in_out_PP2PReadDriver
{
   tMessage_in_PP2PReadDriver in;
} tMessage_in_out_PP2PReadDriver;

/* -----------------------------------------------------------------------------
      PP2PRecvFromDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PRecvFromDriver 77

typedef struct __tMessage_in_PP2PRecvFromDriver
{
   W_HANDLE hSocket;
   tPP2PRecvFromCompleted * pCallback;
   void * pCallbackParameter;
   uint8_t * pReceptionBuffer;
   uint32_t nReceptionBufferLength;
   W_HANDLE * phOperation;
} tMessage_in_PP2PRecvFromDriver;

typedef union __tMessage_in_out_PP2PRecvFromDriver
{
   tMessage_in_PP2PRecvFromDriver in;
} tMessage_in_out_PP2PRecvFromDriver;

/* -----------------------------------------------------------------------------
      PP2PSendToDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PSendToDriver 78

typedef struct __tMessage_in_PP2PSendToDriver
{
   W_HANDLE hSocket;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint8_t nSAP;
   const uint8_t * pSendBuffer;
   uint32_t nSendBufferLength;
   W_HANDLE * phOperation;
} tMessage_in_PP2PSendToDriver;

typedef union __tMessage_in_out_PP2PSendToDriver
{
   tMessage_in_PP2PSendToDriver in;
} tMessage_in_out_PP2PSendToDriver;

/* -----------------------------------------------------------------------------
      PP2PSetConfigurationDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PSetConfigurationDriver 79

typedef struct __tMessage_in_PP2PSetConfigurationDriver
{
   const tWP2PConfiguration * pConfiguration;
   uint32_t nSize;
} tMessage_in_PP2PSetConfigurationDriver;

typedef struct __tMessage_out_PP2PSetConfigurationDriver
{
   W_ERROR value;
} tMessage_out_PP2PSetConfigurationDriver;

typedef union __tMessage_in_out_PP2PSetConfigurationDriver
{
   tMessage_in_PP2PSetConfigurationDriver in;
   tMessage_out_PP2PSetConfigurationDriver out;
} tMessage_in_out_PP2PSetConfigurationDriver;

/* -----------------------------------------------------------------------------
      PP2PSetSocketParameter()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PSetSocketParameter 80

typedef struct __tMessage_in_PP2PSetSocketParameter
{
   W_HANDLE hSocket;
   uint32_t nParameter;
   uint32_t nValue;
} tMessage_in_PP2PSetSocketParameter;

typedef struct __tMessage_out_PP2PSetSocketParameter
{
   W_ERROR value;
} tMessage_out_PP2PSetSocketParameter;

typedef union __tMessage_in_out_PP2PSetSocketParameter
{
   tMessage_in_PP2PSetSocketParameter in;
   tMessage_out_PP2PSetSocketParameter out;
} tMessage_in_out_PP2PSetSocketParameter;

/* -----------------------------------------------------------------------------
      PP2PShutdownDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PShutdownDriver 81

typedef struct __tMessage_in_PP2PShutdownDriver
{
   W_HANDLE hSocket;
   tPBasicGenericCallbackFunction * pReleaseCallback;
   void * pReleaseCallbackParameter;
} tMessage_in_PP2PShutdownDriver;

typedef union __tMessage_in_out_PP2PShutdownDriver
{
   tMessage_in_PP2PShutdownDriver in;
} tMessage_in_out_PP2PShutdownDriver;

/* -----------------------------------------------------------------------------
      PP2PURILookupDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PURILookupDriver 82

typedef struct __tMessage_in_PP2PURILookupDriver
{
   W_HANDLE hLink;
   tPP2PURILookupCompleted * pCallback;
   void * pCallbackParameter;
   const char16_t * pServiceURI;
   uint32_t nSize;
} tMessage_in_PP2PURILookupDriver;

typedef union __tMessage_in_out_PP2PURILookupDriver
{
   tMessage_in_PP2PURILookupDriver in;
} tMessage_in_out_PP2PURILookupDriver;

/* -----------------------------------------------------------------------------
      PP2PWriteDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PWriteDriver 83

typedef struct __tMessage_in_PP2PWriteDriver
{
   W_HANDLE hConnection;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pSendBuffer;
   uint32_t nSendBufferLength;
   W_HANDLE * phOperation;
} tMessage_in_PP2PWriteDriver;

typedef union __tMessage_in_out_PP2PWriteDriver
{
   tMessage_in_PP2PWriteDriver in;
} tMessage_in_out_PP2PWriteDriver;

/* -----------------------------------------------------------------------------
      PReaderDriverGetLastReferenceTime()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverGetLastReferenceTime 84

typedef struct __tMessage_out_PReaderDriverGetLastReferenceTime
{
   uint32_t value;
} tMessage_out_PReaderDriverGetLastReferenceTime;

typedef union __tMessage_in_out_PReaderDriverGetLastReferenceTime
{
   tMessage_out_PReaderDriverGetLastReferenceTime out;
} tMessage_in_out_PReaderDriverGetLastReferenceTime;

/* -----------------------------------------------------------------------------
      PReaderDriverGetNbCardDetected()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverGetNbCardDetected 85

typedef struct __tMessage_out_PReaderDriverGetNbCardDetected
{
   uint8_t value;
} tMessage_out_PReaderDriverGetNbCardDetected;

typedef union __tMessage_in_out_PReaderDriverGetNbCardDetected
{
   tMessage_out_PReaderDriverGetNbCardDetected out;
} tMessage_in_out_PReaderDriverGetNbCardDetected;

/* -----------------------------------------------------------------------------
      PReaderDriverRedetectCard()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverRedetectCard 86

typedef struct __tMessage_in_PReaderDriverRedetectCard
{
   W_HANDLE hConnection;
} tMessage_in_PReaderDriverRedetectCard;

typedef struct __tMessage_out_PReaderDriverRedetectCard
{
   W_ERROR value;
} tMessage_out_PReaderDriverRedetectCard;

typedef union __tMessage_in_out_PReaderDriverRedetectCard
{
   tMessage_in_PReaderDriverRedetectCard in;
   tMessage_out_PReaderDriverRedetectCard out;
} tMessage_in_out_PReaderDriverRedetectCard;

/* -----------------------------------------------------------------------------
      PReaderDriverRegister()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverRegister 87

typedef struct __tMessage_in_PReaderDriverRegister
{
   tPReaderDriverRegisterCompleted * pCallback;
   void * pCallbackParameter;
   uint8_t nPriority;
   uint32_t nRequestedProtocolsBF;
   uint32_t nDetectionConfigurationLength;
   uint8_t * pBuffer;
   uint32_t nBufferMaxLength;
   W_HANDLE * phListenerHandle;
} tMessage_in_PReaderDriverRegister;

typedef struct __tMessage_out_PReaderDriverRegister
{
   W_ERROR value;
} tMessage_out_PReaderDriverRegister;

typedef union __tMessage_in_out_PReaderDriverRegister
{
   tMessage_in_PReaderDriverRegister in;
   tMessage_out_PReaderDriverRegister out;
} tMessage_in_out_PReaderDriverRegister;

/* -----------------------------------------------------------------------------
      PReaderDriverSetWorkPerformedAndClose()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverSetWorkPerformedAndClose 88

typedef struct __tMessage_in_PReaderDriverSetWorkPerformedAndClose
{
   W_HANDLE hDriverListener;
} tMessage_in_PReaderDriverSetWorkPerformedAndClose;

typedef struct __tMessage_out_PReaderDriverSetWorkPerformedAndClose
{
   W_ERROR value;
} tMessage_out_PReaderDriverSetWorkPerformedAndClose;

typedef union __tMessage_in_out_PReaderDriverSetWorkPerformedAndClose
{
   tMessage_in_PReaderDriverSetWorkPerformedAndClose in;
   tMessage_out_PReaderDriverSetWorkPerformedAndClose out;
} tMessage_in_out_PReaderDriverSetWorkPerformedAndClose;

/* -----------------------------------------------------------------------------
      PReaderDriverWorkPerformed()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverWorkPerformed 89

typedef struct __tMessage_in_PReaderDriverWorkPerformed
{
   W_HANDLE hConnection;
   bool_t bGiveToNextListener;
   bool_t bCardApplicationMatch;
} tMessage_in_PReaderDriverWorkPerformed;

typedef struct __tMessage_out_PReaderDriverWorkPerformed
{
   W_ERROR value;
} tMessage_out_PReaderDriverWorkPerformed;

typedef union __tMessage_in_out_PReaderDriverWorkPerformed
{
   tMessage_in_PReaderDriverWorkPerformed in;
   tMessage_out_PReaderDriverWorkPerformed out;
} tMessage_in_out_PReaderDriverWorkPerformed;

/* -----------------------------------------------------------------------------
      PReaderErrorEventRegister()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderErrorEventRegister 90

typedef struct __tMessage_in_PReaderErrorEventRegister
{
   tPBasicGenericEventHandler * pHandler;
   void * pHandlerParameter;
   uint8_t nEventType;
   bool_t bCardDetectionRequested;
   W_HANDLE * phRegistryHandle;
} tMessage_in_PReaderErrorEventRegister;

typedef struct __tMessage_out_PReaderErrorEventRegister
{
   W_ERROR value;
} tMessage_out_PReaderErrorEventRegister;

typedef union __tMessage_in_out_PReaderErrorEventRegister
{
   tMessage_in_PReaderErrorEventRegister in;
   tMessage_out_PReaderErrorEventRegister out;
} tMessage_in_out_PReaderErrorEventRegister;

/* -----------------------------------------------------------------------------
      PReaderGetPulsePeriod()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderGetPulsePeriod 91

typedef struct __tMessage_in_PReaderGetPulsePeriod
{
   uint32_t * pnTimeout;
} tMessage_in_PReaderGetPulsePeriod;

typedef struct __tMessage_out_PReaderGetPulsePeriod
{
   W_ERROR value;
} tMessage_out_PReaderGetPulsePeriod;

typedef union __tMessage_in_out_PReaderGetPulsePeriod
{
   tMessage_in_PReaderGetPulsePeriod in;
   tMessage_out_PReaderGetPulsePeriod out;
} tMessage_in_out_PReaderGetPulsePeriod;

/* -----------------------------------------------------------------------------
      PReaderSetPulsePeriodDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderSetPulsePeriodDriver 92

typedef struct __tMessage_in_PReaderSetPulsePeriodDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint32_t nPulsePeriod;
} tMessage_in_PReaderSetPulsePeriodDriver;

typedef union __tMessage_in_out_PReaderSetPulsePeriodDriver
{
   tMessage_in_PReaderSetPulsePeriodDriver in;
} tMessage_in_out_PReaderSetPulsePeriodDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableApplyDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableApplyDriver 93

typedef struct __tMessage_in_PRoutingTableApplyDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PRoutingTableApplyDriver;

typedef union __tMessage_in_out_PRoutingTableApplyDriver
{
   tMessage_in_PRoutingTableApplyDriver in;
} tMessage_in_out_PRoutingTableApplyDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableGetConfigDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableGetConfigDriver 94

typedef struct __tMessage_out_PRoutingTableGetConfigDriver
{
   uint32_t value;
} tMessage_out_PRoutingTableGetConfigDriver;

typedef union __tMessage_in_out_PRoutingTableGetConfigDriver
{
   tMessage_out_PRoutingTableGetConfigDriver out;
} tMessage_in_out_PRoutingTableGetConfigDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableReadDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableReadDriver 95

typedef struct __tMessage_in_PRoutingTableReadDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PRoutingTableReadDriver;

typedef union __tMessage_in_out_PRoutingTableReadDriver
{
   tMessage_in_PRoutingTableReadDriver in;
} tMessage_in_out_PRoutingTableReadDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableSetConfigDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableSetConfigDriver 96

typedef struct __tMessage_in_PRoutingTableSetConfigDriver
{
   uint32_t nConfig;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PRoutingTableSetConfigDriver;

typedef union __tMessage_in_out_PRoutingTableSetConfigDriver
{
   tMessage_in_PRoutingTableSetConfigDriver in;
} tMessage_in_out_PRoutingTableSetConfigDriver;

/* -----------------------------------------------------------------------------
      PSecurityManagerDriverAuthenticate()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSecurityManagerDriverAuthenticate 97

typedef struct __tMessage_in_PSecurityManagerDriverAuthenticate
{
   const uint8_t * pApplicationDataBuffer;
   uint32_t nApplicationDataBufferLength;
} tMessage_in_PSecurityManagerDriverAuthenticate;

typedef struct __tMessage_out_PSecurityManagerDriverAuthenticate
{
   W_ERROR value;
} tMessage_out_PSecurityManagerDriverAuthenticate;

typedef union __tMessage_in_out_PSecurityManagerDriverAuthenticate
{
   tMessage_in_PSecurityManagerDriverAuthenticate in;
   tMessage_out_PSecurityManagerDriverAuthenticate out;
} tMessage_in_out_PSecurityManagerDriverAuthenticate;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmCloseChannel()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmCloseChannel 98

typedef struct __tMessage_in_PSeDriver7816SmCloseChannel
{
   W_HANDLE hDriverConnection;
   uint32_t nChannelReference;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PSeDriver7816SmCloseChannel;

typedef struct __tMessage_out_PSeDriver7816SmCloseChannel
{
   W_ERROR value;
} tMessage_out_PSeDriver7816SmCloseChannel;

typedef union __tMessage_in_out_PSeDriver7816SmCloseChannel
{
   tMessage_in_PSeDriver7816SmCloseChannel in;
   tMessage_out_PSeDriver7816SmCloseChannel out;
} tMessage_in_out_PSeDriver7816SmCloseChannel;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmExchangeApdu()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmExchangeApdu 99

typedef struct __tMessage_in_PSeDriver7816SmExchangeApdu
{
   W_HANDLE hDriverConnection;
   uint32_t nChannelReference;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pSendApduBuffer;
   uint32_t nSendApduBufferLength;
   uint8_t * pReceivedApduBuffer;
   uint32_t nReceivedApduBufferMaxLength;
} tMessage_in_PSeDriver7816SmExchangeApdu;

typedef struct __tMessage_out_PSeDriver7816SmExchangeApdu
{
   W_ERROR value;
} tMessage_out_PSeDriver7816SmExchangeApdu;

typedef union __tMessage_in_out_PSeDriver7816SmExchangeApdu
{
   tMessage_in_PSeDriver7816SmExchangeApdu in;
   tMessage_out_PSeDriver7816SmExchangeApdu out;
} tMessage_in_out_PSeDriver7816SmExchangeApdu;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmGetData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmGetData 100

typedef struct __tMessage_in_PSeDriver7816SmGetData
{
   W_HANDLE hDriverConnection;
   uint32_t nChannelReference;
   uint32_t nType;
   uint8_t * pBuffer;
   uint32_t nBufferMaxLength;
   uint32_t * pnActualLength;
} tMessage_in_PSeDriver7816SmGetData;

typedef struct __tMessage_out_PSeDriver7816SmGetData
{
   W_ERROR value;
} tMessage_out_PSeDriver7816SmGetData;

typedef union __tMessage_in_out_PSeDriver7816SmGetData
{
   tMessage_in_PSeDriver7816SmGetData in;
   tMessage_out_PSeDriver7816SmGetData out;
} tMessage_in_out_PSeDriver7816SmGetData;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmOpenChannel()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmOpenChannel 101

typedef struct __tMessage_in_PSeDriver7816SmOpenChannel
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint32_t nType;
   const uint8_t * pAID;
   uint32_t nAIDLength;
} tMessage_in_PSeDriver7816SmOpenChannel;

typedef struct __tMessage_out_PSeDriver7816SmOpenChannel
{
   W_ERROR value;
} tMessage_out_PSeDriver7816SmOpenChannel;

typedef union __tMessage_in_out_PSeDriver7816SmOpenChannel
{
   tMessage_in_PSeDriver7816SmOpenChannel in;
   tMessage_out_PSeDriver7816SmOpenChannel out;
} tMessage_in_out_PSeDriver7816SmOpenChannel;

/* -----------------------------------------------------------------------------
      PSEDriverActivateSwpLine()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverActivateSwpLine 102

typedef struct __tMessage_in_PSEDriverActivateSwpLine
{
   uint32_t nSlotIdentifier;
} tMessage_in_PSEDriverActivateSwpLine;

typedef struct __tMessage_out_PSEDriverActivateSwpLine
{
   W_ERROR value;
} tMessage_out_PSEDriverActivateSwpLine;

typedef union __tMessage_in_out_PSEDriverActivateSwpLine
{
   tMessage_in_PSEDriverActivateSwpLine in;
   tMessage_out_PSEDriverActivateSwpLine out;
} tMessage_in_out_PSEDriverActivateSwpLine;

/* -----------------------------------------------------------------------------
      PSEDriverGetAtr()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverGetAtr 103

typedef struct __tMessage_in_PSEDriverGetAtr
{
   W_HANDLE hDriverConnection;
   uint8_t * pAtrBuffer;
   uint32_t nAtrBufferLength;
   uint32_t * pnAtrLength;
} tMessage_in_PSEDriverGetAtr;

typedef struct __tMessage_out_PSEDriverGetAtr
{
   W_ERROR value;
} tMessage_out_PSEDriverGetAtr;

typedef union __tMessage_in_out_PSEDriverGetAtr
{
   tMessage_in_PSEDriverGetAtr in;
   tMessage_out_PSEDriverGetAtr out;
} tMessage_in_out_PSEDriverGetAtr;

/* -----------------------------------------------------------------------------
      PSEDriverGetInfo()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverGetInfo 104

typedef struct __tMessage_in_PSEDriverGetInfo
{
   uint32_t nSlotIdentifier;
   tWSEInfoEx * pSEInfo;
   uint32_t nSize;
} tMessage_in_PSEDriverGetInfo;

typedef struct __tMessage_out_PSEDriverGetInfo
{
   W_ERROR value;
} tMessage_out_PSEDriverGetInfo;

typedef union __tMessage_in_out_PSEDriverGetInfo
{
   tMessage_in_PSEDriverGetInfo in;
   tMessage_out_PSEDriverGetInfo out;
} tMessage_in_out_PSEDriverGetInfo;

/* -----------------------------------------------------------------------------
      PSEDriverGetStatus()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverGetStatus 105

typedef struct __tMessage_in_PSEDriverGetStatus
{
   uint32_t nSlotIdentifier;
   tPSEGetStatusCompleted * pCallback;
   void * pCallbackParameter;
} tMessage_in_PSEDriverGetStatus;

typedef union __tMessage_in_out_PSEDriverGetStatus
{
   tMessage_in_PSEDriverGetStatus in;
} tMessage_in_out_PSEDriverGetStatus;

/* -----------------------------------------------------------------------------
      PSEDriverImpersonateAndCheckAidAccess()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverImpersonateAndCheckAidAccess 106

typedef struct __tMessage_in_PSEDriverImpersonateAndCheckAidAccess
{
   uint32_t nSlotIdentifier;
   const uint8_t * pAIDBuffer;
   uint32_t nAIDLength;
   const uint8_t * pImpersonationDataBuffer;
   uint32_t nImpersonationDataBufferLength;
} tMessage_in_PSEDriverImpersonateAndCheckAidAccess;

typedef struct __tMessage_out_PSEDriverImpersonateAndCheckAidAccess
{
   W_ERROR value;
} tMessage_out_PSEDriverImpersonateAndCheckAidAccess;

typedef union __tMessage_in_out_PSEDriverImpersonateAndCheckAidAccess
{
   tMessage_in_PSEDriverImpersonateAndCheckAidAccess in;
   tMessage_out_PSEDriverImpersonateAndCheckAidAccess out;
} tMessage_in_out_PSEDriverImpersonateAndCheckAidAccess;

/* -----------------------------------------------------------------------------
      PSEDriverOpenConnection()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverOpenConnection 107

typedef struct __tMessage_in_PSEDriverOpenConnection
{
   uint32_t nSlotIdentifier;
   bool_t bForce;
   tPBasicGenericHandleCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PSEDriverOpenConnection;

typedef union __tMessage_in_out_PSEDriverOpenConnection
{
   tMessage_in_PSEDriverOpenConnection in;
} tMessage_in_out_PSEDriverOpenConnection;

/* -----------------------------------------------------------------------------
      PSEDriverSetPolicy()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverSetPolicy 108

typedef struct __tMessage_in_PSEDriverSetPolicy
{
   uint32_t nSlotIdentifier;
   uint32_t nStorageType;
   uint32_t nProtocols;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tMessage_in_PSEDriverSetPolicy;

typedef union __tMessage_in_out_PSEDriverSetPolicy
{
   tMessage_in_PSEDriverSetPolicy in;
} tMessage_in_out_PSEDriverSetPolicy;

/* -----------------------------------------------------------------------------
      PSEGetConnectivityEventParameter()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEGetConnectivityEventParameter 109

typedef struct __tMessage_in_PSEGetConnectivityEventParameter
{
   uint32_t nSlotIdentifier;
   uint8_t * pDataBuffer;
   uint32_t nBufferLength;
   uint32_t * pnActualDataLength;
} tMessage_in_PSEGetConnectivityEventParameter;

typedef struct __tMessage_out_PSEGetConnectivityEventParameter
{
   W_ERROR value;
} tMessage_out_PSEGetConnectivityEventParameter;

typedef union __tMessage_in_out_PSEGetConnectivityEventParameter
{
   tMessage_in_PSEGetConnectivityEventParameter in;
   tMessage_out_PSEGetConnectivityEventParameter out;
} tMessage_in_out_PSEGetConnectivityEventParameter;

/* -----------------------------------------------------------------------------
      PSEGetTransactionAID()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEGetTransactionAID 110

typedef struct __tMessage_in_PSEGetTransactionAID
{
   uint32_t nSlotIdentifier;
   uint8_t * pBuffer;
   uint32_t nBufferLength;
} tMessage_in_PSEGetTransactionAID;

typedef struct __tMessage_out_PSEGetTransactionAID
{
   uint32_t value;
} tMessage_out_PSEGetTransactionAID;

typedef union __tMessage_in_out_PSEGetTransactionAID
{
   tMessage_in_PSEGetTransactionAID in;
   tMessage_out_PSEGetTransactionAID out;
} tMessage_in_out_PSEGetTransactionAID;

/* -----------------------------------------------------------------------------
      PSEMonitorConnectivityEvent()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEMonitorConnectivityEvent 111

typedef struct __tMessage_in_PSEMonitorConnectivityEvent
{
   uint32_t nSlotIdentifier;
   tPBasicGenericEventHandler2 * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tMessage_in_PSEMonitorConnectivityEvent;

typedef struct __tMessage_out_PSEMonitorConnectivityEvent
{
   W_ERROR value;
} tMessage_out_PSEMonitorConnectivityEvent;

typedef union __tMessage_in_out_PSEMonitorConnectivityEvent
{
   tMessage_in_PSEMonitorConnectivityEvent in;
   tMessage_out_PSEMonitorConnectivityEvent out;
} tMessage_in_out_PSEMonitorConnectivityEvent;

/* -----------------------------------------------------------------------------
      PSEMonitorEndOfTransaction()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEMonitorEndOfTransaction 112

typedef struct __tMessage_in_PSEMonitorEndOfTransaction
{
   uint32_t nSlotIdentifier;
   tPBasicGenericEventHandler2 * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tMessage_in_PSEMonitorEndOfTransaction;

typedef struct __tMessage_out_PSEMonitorEndOfTransaction
{
   W_ERROR value;
} tMessage_out_PSEMonitorEndOfTransaction;

typedef union __tMessage_in_out_PSEMonitorEndOfTransaction
{
   tMessage_in_PSEMonitorEndOfTransaction in;
   tMessage_out_PSEMonitorEndOfTransaction out;
} tMessage_in_out_PSEMonitorEndOfTransaction;

/* -----------------------------------------------------------------------------
      PSEMonitorHotPlugEvents()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEMonitorHotPlugEvents 113

typedef struct __tMessage_in_PSEMonitorHotPlugEvents
{
   uint32_t nSlotIdentifier;
   tPBasicGenericEventHandler2 * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tMessage_in_PSEMonitorHotPlugEvents;

typedef struct __tMessage_out_PSEMonitorHotPlugEvents
{
   W_ERROR value;
} tMessage_out_PSEMonitorHotPlugEvents;

typedef union __tMessage_in_out_PSEMonitorHotPlugEvents
{
   tMessage_in_PSEMonitorHotPlugEvents in;
   tMessage_out_PSEMonitorHotPlugEvents out;
} tMessage_in_out_PSEMonitorHotPlugEvents;

/* -----------------------------------------------------------------------------
      PType1ChipDriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PType1ChipDriverExchangeData 114

typedef struct __tMessage_in_PType1ChipDriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tMessage_in_PType1ChipDriverExchangeData;

typedef struct __tMessage_out_PType1ChipDriverExchangeData
{
   W_HANDLE value;
} tMessage_out_PType1ChipDriverExchangeData;

typedef union __tMessage_in_out_PType1ChipDriverExchangeData
{
   tMessage_in_PType1ChipDriverExchangeData in;
   tMessage_out_PType1ChipDriverExchangeData out;
} tMessage_in_out_PType1ChipDriverExchangeData;

#define P_DRIVER_FUNCTION_COUNT 115

#endif /* P_CONFIG_USER || P_CONFIG_DRIVER */

#endif /* #ifdef P_CONFIG_CLIENT_SERVER */


#endif /* #ifdef __WME_SERVER_AUTOGEN_H */
