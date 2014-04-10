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

#ifndef __WME_DRIVER_AUTOGEN_H
#define __WME_DRIVER_AUTOGEN_H

#ifndef P_CONFIG_CLIENT_SERVER

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_DRIVER)

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverExchangeData 0

typedef struct __tParams_in_P14P3DriverExchangeData
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
} tParams_in_P14P3DriverExchangeData;

typedef union __tParams_P14P3DriverExchangeData
{
   tParams_in_P14P3DriverExchangeData in;
   W_HANDLE out;
} tParams_P14P3DriverExchangeData;

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeRawBits()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverExchangeRawBits 1

typedef struct __tParams_in_P14P3DriverExchangeRawBits
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
} tParams_in_P14P3DriverExchangeRawBits;

typedef union __tParams_P14P3DriverExchangeRawBits
{
   tParams_in_P14P3DriverExchangeRawBits in;
   W_HANDLE out;
} tParams_P14P3DriverExchangeRawBits;

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeRawMifare()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverExchangeRawMifare 2

typedef struct __tParams_in_P14P3DriverExchangeRawMifare
{
   W_HANDLE hConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tParams_in_P14P3DriverExchangeRawMifare;

typedef union __tParams_P14P3DriverExchangeRawMifare
{
   tParams_in_P14P3DriverExchangeRawMifare in;
   W_HANDLE out;
} tParams_P14P3DriverExchangeRawMifare;

/* -----------------------------------------------------------------------------
      P14P3DriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P3DriverSetTimeout 3

typedef struct __tParams_in_P14P3DriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tParams_in_P14P3DriverSetTimeout;

typedef union __tParams_P14P3DriverSetTimeout
{
   tParams_in_P14P3DriverSetTimeout in;
   W_ERROR out;
} tParams_P14P3DriverSetTimeout;

/* -----------------------------------------------------------------------------
      P14P4DriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P4DriverExchangeData 4

typedef struct __tParams_in_P14P4DriverExchangeData
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
} tParams_in_P14P4DriverExchangeData;

typedef union __tParams_P14P4DriverExchangeData
{
   tParams_in_P14P4DriverExchangeData in;
   W_HANDLE out;
} tParams_P14P4DriverExchangeData;

/* -----------------------------------------------------------------------------
      P14P4DriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_P14P4DriverSetTimeout 5

typedef struct __tParams_in_P14P4DriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tParams_in_P14P4DriverSetTimeout;

typedef union __tParams_P14P4DriverSetTimeout
{
   tParams_in_P14P4DriverSetTimeout in;
   W_ERROR out;
} tParams_P14P4DriverSetTimeout;

/* -----------------------------------------------------------------------------
      P15P3DriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_P15P3DriverExchangeData 6

typedef struct __tParams_in_P15P3DriverExchangeData
{
   W_HANDLE hConnection;
   tP15P3DriverExchangeDataCompleted * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tParams_in_P15P3DriverExchangeData;

typedef union __tParams_P15P3DriverExchangeData
{
   tParams_in_P15P3DriverExchangeData in;
} tParams_P15P3DriverExchangeData;

/* -----------------------------------------------------------------------------
      P15P3DriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_P15P3DriverSetTimeout 7

typedef struct __tParams_in_P15P3DriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tParams_in_P15P3DriverSetTimeout;

typedef union __tParams_P15P3DriverSetTimeout
{
   tParams_in_P15P3DriverSetTimeout in;
   W_ERROR out;
} tParams_P15P3DriverSetTimeout;

/* -----------------------------------------------------------------------------
      PBasicDriverCancelOperation()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBasicDriverCancelOperation 8

typedef struct __tParams_in_PBasicDriverCancelOperation
{
   W_HANDLE hOperation;
} tParams_in_PBasicDriverCancelOperation;

typedef union __tParams_PBasicDriverCancelOperation
{
   tParams_in_PBasicDriverCancelOperation in;
} tParams_PBasicDriverCancelOperation;

/* -----------------------------------------------------------------------------
      PBasicDriverGetVersion()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBasicDriverGetVersion 9

typedef struct __tParams_in_PBasicDriverGetVersion
{
   void * pBuffer;
   uint32_t nBufferSize;
} tParams_in_PBasicDriverGetVersion;

typedef union __tParams_PBasicDriverGetVersion
{
   tParams_in_PBasicDriverGetVersion in;
   W_ERROR out;
} tParams_PBasicDriverGetVersion;

/* -----------------------------------------------------------------------------
      PBPrimeDriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBPrimeDriverExchangeData 10

typedef struct __tParams_in_PBPrimeDriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tParams_in_PBPrimeDriverExchangeData;

typedef union __tParams_PBPrimeDriverExchangeData
{
   tParams_in_PBPrimeDriverExchangeData in;
   W_HANDLE out;
} tParams_PBPrimeDriverExchangeData;

/* -----------------------------------------------------------------------------
      PBPrimeDriverSetTimeout()
----------------------------------------------------------------------------- */

#define P_Idenfier_PBPrimeDriverSetTimeout 11

typedef struct __tParams_in_PBPrimeDriverSetTimeout
{
   W_HANDLE hConnection;
   uint32_t nTimeout;
} tParams_in_PBPrimeDriverSetTimeout;

typedef union __tParams_PBPrimeDriverSetTimeout
{
   tParams_in_PBPrimeDriverSetTimeout in;
   W_ERROR out;
} tParams_PBPrimeDriverSetTimeout;

/* -----------------------------------------------------------------------------
      PCacheConnectionDriverRead()
----------------------------------------------------------------------------- */

#define P_Idenfier_PCacheConnectionDriverRead 12

typedef struct __tParams_in_PCacheConnectionDriverRead
{
   tCacheConnectionInstance * pCacheConnection;
   uint32_t nSize;
} tParams_in_PCacheConnectionDriverRead;

typedef union __tParams_PCacheConnectionDriverRead
{
   tParams_in_PCacheConnectionDriverRead in;
   W_ERROR out;
} tParams_PCacheConnectionDriverRead;

/* -----------------------------------------------------------------------------
      PCacheConnectionDriverWrite()
----------------------------------------------------------------------------- */

#define P_Idenfier_PCacheConnectionDriverWrite 13

typedef struct __tParams_in_PCacheConnectionDriverWrite
{
   const tCacheConnectionInstance * pCacheConnection;
   uint32_t nSize;
} tParams_in_PCacheConnectionDriverWrite;

typedef union __tParams_PCacheConnectionDriverWrite
{
   tParams_in_PCacheConnectionDriverWrite in;
   W_ERROR out;
} tParams_PCacheConnectionDriverWrite;

/* -----------------------------------------------------------------------------
      PContextDriverGenerateRandom()
----------------------------------------------------------------------------- */

#define P_Idenfier_PContextDriverGenerateRandom 14

typedef union __tParams_PContextDriverGenerateRandom
{
   uint32_t out;
} tParams_PContextDriverGenerateRandom;

/* -----------------------------------------------------------------------------
      PContextDriverGetMemoryStatistics()
----------------------------------------------------------------------------- */

#define P_Idenfier_PContextDriverGetMemoryStatistics 15

typedef struct __tParams_in_PContextDriverGetMemoryStatistics
{
   tContextDriverMemoryStatistics * pStatistics;
   uint32_t nSize;
} tParams_in_PContextDriverGetMemoryStatistics;

typedef union __tParams_PContextDriverGetMemoryStatistics
{
   tParams_in_PContextDriverGetMemoryStatistics in;
} tParams_PContextDriverGetMemoryStatistics;

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

typedef struct __tParams_in_PEmulCloseDriver
{
   W_HANDLE hHandle;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PEmulCloseDriver;

typedef union __tParams_PEmulCloseDriver
{
   tParams_in_PEmulCloseDriver in;
} tParams_PEmulCloseDriver;

/* -----------------------------------------------------------------------------
      PEmulGetMessageData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulGetMessageData 20

typedef struct __tParams_in_PEmulGetMessageData
{
   W_HANDLE hHandle;
   uint8_t * pDataBuffer;
   uint32_t nDataLength;
   uint32_t * pnActualDataLength;
} tParams_in_PEmulGetMessageData;

typedef union __tParams_PEmulGetMessageData
{
   tParams_in_PEmulGetMessageData in;
   W_ERROR out;
} tParams_PEmulGetMessageData;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver1()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver1 21

typedef struct __tParams_in_PEmulOpenConnectionDriver1
{
   tPBasicGenericCallbackFunction * pOpenCallback;
   void * pOpenCallbackParameter;
   const tWEmulConnectionInfo * pEmulConnectionInfo;
   uint32_t nSize;
   W_HANDLE * phHandle;
} tParams_in_PEmulOpenConnectionDriver1;

typedef union __tParams_PEmulOpenConnectionDriver1
{
   tParams_in_PEmulOpenConnectionDriver1 in;
} tParams_PEmulOpenConnectionDriver1;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver1Ex()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver1Ex 22

typedef struct __tParams_in_PEmulOpenConnectionDriver1Ex
{
   tPBasicGenericCallbackFunction * pOpenCallback;
   void * pOpenCallbackParameter;
   const tWEmulConnectionInfo * pEmulConnectionInfo;
   uint32_t nSize;
   W_HANDLE * phHandle;
} tParams_in_PEmulOpenConnectionDriver1Ex;

typedef union __tParams_PEmulOpenConnectionDriver1Ex
{
   tParams_in_PEmulOpenConnectionDriver1Ex in;
} tParams_PEmulOpenConnectionDriver1Ex;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver2()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver2 23

typedef struct __tParams_in_PEmulOpenConnectionDriver2
{
   W_HANDLE hHandle;
   tPEmulDriverEventReceived * pEventCallback;
   void * pEventCallbackParameter;
} tParams_in_PEmulOpenConnectionDriver2;

typedef union __tParams_PEmulOpenConnectionDriver2
{
   tParams_in_PEmulOpenConnectionDriver2 in;
} tParams_PEmulOpenConnectionDriver2;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver2Ex()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver2Ex 24

typedef struct __tParams_in_PEmulOpenConnectionDriver2Ex
{
   W_HANDLE hHandle;
   tPEmulDriverEventReceived * pEventCallback;
   void * pEventCallbackParameter;
} tParams_in_PEmulOpenConnectionDriver2Ex;

typedef union __tParams_PEmulOpenConnectionDriver2Ex
{
   tParams_in_PEmulOpenConnectionDriver2Ex in;
} tParams_PEmulOpenConnectionDriver2Ex;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver3()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver3 25

typedef struct __tParams_in_PEmulOpenConnectionDriver3
{
   W_HANDLE hHandle;
   tPEmulDriverCommandReceived * pCommandCallback;
   void * pCommandCallbackParameter;
} tParams_in_PEmulOpenConnectionDriver3;

typedef union __tParams_PEmulOpenConnectionDriver3
{
   tParams_in_PEmulOpenConnectionDriver3 in;
} tParams_PEmulOpenConnectionDriver3;

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver3Ex()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulOpenConnectionDriver3Ex 26

typedef struct __tParams_in_PEmulOpenConnectionDriver3Ex
{
   W_HANDLE hHandle;
   tPEmulDriverCommandReceived * pCommandCallback;
   void * pCommandCallbackParameter;
} tParams_in_PEmulOpenConnectionDriver3Ex;

typedef union __tParams_PEmulOpenConnectionDriver3Ex
{
   tParams_in_PEmulOpenConnectionDriver3Ex in;
} tParams_PEmulOpenConnectionDriver3Ex;

/* -----------------------------------------------------------------------------
      PEmulSendAnswer()
----------------------------------------------------------------------------- */

#define P_Idenfier_PEmulSendAnswer 27

typedef struct __tParams_in_PEmulSendAnswer
{
   W_HANDLE hDriverConnection;
   const uint8_t * pDataBuffer;
   uint32_t nDataLength;
} tParams_in_PEmulSendAnswer;

typedef union __tParams_PEmulSendAnswer
{
   tParams_in_PEmulSendAnswer in;
   W_ERROR out;
} tParams_PEmulSendAnswer;

/* -----------------------------------------------------------------------------
      PFeliCaDriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PFeliCaDriverExchangeData 28

typedef struct __tParams_in_PFeliCaDriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tParams_in_PFeliCaDriverExchangeData;

typedef union __tParams_PFeliCaDriverExchangeData
{
   tParams_in_PFeliCaDriverExchangeData in;
} tParams_PFeliCaDriverExchangeData;

/* -----------------------------------------------------------------------------
      PFeliCaDriverGetCardList()
----------------------------------------------------------------------------- */

#define P_Idenfier_PFeliCaDriverGetCardList 29

typedef struct __tParams_in_PFeliCaDriverGetCardList
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tParams_in_PFeliCaDriverGetCardList;

typedef union __tParams_PFeliCaDriverGetCardList
{
   tParams_in_PFeliCaDriverGetCardList in;
} tParams_PFeliCaDriverGetCardList;

/* -----------------------------------------------------------------------------
      PHandleCheckPropertyDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleCheckPropertyDriver 30

typedef struct __tParams_in_PHandleCheckPropertyDriver
{
   W_HANDLE hObject;
   uint8_t nPropertyValue;
} tParams_in_PHandleCheckPropertyDriver;

typedef union __tParams_PHandleCheckPropertyDriver
{
   tParams_in_PHandleCheckPropertyDriver in;
   W_ERROR out;
} tParams_PHandleCheckPropertyDriver;

/* -----------------------------------------------------------------------------
      PHandleCloseDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleCloseDriver 31

typedef struct __tParams_in_PHandleCloseDriver
{
   W_HANDLE hObject;
} tParams_in_PHandleCloseDriver;

typedef union __tParams_PHandleCloseDriver
{
   tParams_in_PHandleCloseDriver in;
} tParams_PHandleCloseDriver;

/* -----------------------------------------------------------------------------
      PHandleCloseSafeDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleCloseSafeDriver 32

typedef struct __tParams_in_PHandleCloseSafeDriver
{
   W_HANDLE hObject;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PHandleCloseSafeDriver;

typedef union __tParams_PHandleCloseSafeDriver
{
   tParams_in_PHandleCloseSafeDriver in;
} tParams_PHandleCloseSafeDriver;

/* -----------------------------------------------------------------------------
      PHandleGetCountDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleGetCountDriver 33

typedef union __tParams_PHandleGetCountDriver
{
   uint32_t out;
} tParams_PHandleGetCountDriver;

/* -----------------------------------------------------------------------------
      PHandleGetPropertiesDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleGetPropertiesDriver 34

typedef struct __tParams_in_PHandleGetPropertiesDriver
{
   W_HANDLE hObject;
   uint8_t * pPropertyArray;
   uint32_t nPropertyArrayLength;
} tParams_in_PHandleGetPropertiesDriver;

typedef union __tParams_PHandleGetPropertiesDriver
{
   tParams_in_PHandleGetPropertiesDriver in;
   W_ERROR out;
} tParams_PHandleGetPropertiesDriver;

/* -----------------------------------------------------------------------------
      PHandleGetPropertyNumberDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PHandleGetPropertyNumberDriver 35

typedef struct __tParams_in_PHandleGetPropertyNumberDriver
{
   W_HANDLE hObject;
   uint32_t * pnPropertyNumber;
} tParams_in_PHandleGetPropertyNumberDriver;

typedef union __tParams_PHandleGetPropertyNumberDriver
{
   tParams_in_PHandleGetPropertyNumberDriver in;
   W_ERROR out;
} tParams_PHandleGetPropertyNumberDriver;

/* -----------------------------------------------------------------------------
      PMultiTimerCancelDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PMultiTimerCancelDriver 36

typedef struct __tParams_in_PMultiTimerCancelDriver
{
   uint32_t nTimerIdentifier;
} tParams_in_PMultiTimerCancelDriver;

typedef union __tParams_PMultiTimerCancelDriver
{
   tParams_in_PMultiTimerCancelDriver in;
} tParams_PMultiTimerCancelDriver;

/* -----------------------------------------------------------------------------
      PMultiTimerSetDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PMultiTimerSetDriver 37

typedef struct __tParams_in_PMultiTimerSetDriver
{
   uint32_t nTimerIdentifier;
   uint32_t nAbsoluteTimeout;
   tPBasicGenericCompletionFunction * pCallbackFunction;
   void * pCallbackParameter;
} tParams_in_PMultiTimerSetDriver;

typedef union __tParams_PMultiTimerSetDriver
{
   tParams_in_PMultiTimerSetDriver in;
} tParams_PMultiTimerSetDriver;

/* -----------------------------------------------------------------------------
      PNALServiceDriverGetCurrentTime()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNALServiceDriverGetCurrentTime 38

typedef union __tParams_PNALServiceDriverGetCurrentTime
{
   uint32_t out;
} tParams_PNALServiceDriverGetCurrentTime;

/* -----------------------------------------------------------------------------
      PNALServiceDriverGetProtocolStatistics()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNALServiceDriverGetProtocolStatistics 39

typedef struct __tParams_in_PNALServiceDriverGetProtocolStatistics
{
   tNALProtocolStatistics * pStatistics;
   uint32_t nSize;
} tParams_in_PNALServiceDriverGetProtocolStatistics;

typedef union __tParams_PNALServiceDriverGetProtocolStatistics
{
   tParams_in_PNALServiceDriverGetProtocolStatistics in;
} tParams_PNALServiceDriverGetProtocolStatistics;

/* -----------------------------------------------------------------------------
      PNALServiceDriverResetProtocolStatistics()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNALServiceDriverResetProtocolStatistics 40

/* -----------------------------------------------------------------------------
      PNDEFRegisterNPPMessageHandlerDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRegisterNPPMessageHandlerDriver 41

typedef struct __tParams_in_PNDEFRegisterNPPMessageHandlerDriver
{
   tPBasicGenericDataCallbackFunction * pHandler;
   void * pHandlerParameter;
   uint8_t nPriority;
   W_HANDLE * phRegistry;
} tParams_in_PNDEFRegisterNPPMessageHandlerDriver;

typedef union __tParams_PNDEFRegisterNPPMessageHandlerDriver
{
   tParams_in_PNDEFRegisterNPPMessageHandlerDriver in;
   W_ERROR out;
} tParams_PNDEFRegisterNPPMessageHandlerDriver;

/* -----------------------------------------------------------------------------
      PNDEFRegisterSNEPMessageHandlerDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRegisterSNEPMessageHandlerDriver 42

typedef struct __tParams_in_PNDEFRegisterSNEPMessageHandlerDriver
{
   tPBasicGenericDataCallbackFunction * pHandler;
   void * pHandlerParameter;
   uint8_t nPriority;
   W_HANDLE * phRegistry;
} tParams_in_PNDEFRegisterSNEPMessageHandlerDriver;

typedef union __tParams_PNDEFRegisterSNEPMessageHandlerDriver
{
   tParams_in_PNDEFRegisterSNEPMessageHandlerDriver in;
   W_ERROR out;
} tParams_PNDEFRegisterSNEPMessageHandlerDriver;

/* -----------------------------------------------------------------------------
      PNDEFRetrieveNPPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRetrieveNPPMessageDriver 43

typedef struct __tParams_in_PNDEFRetrieveNPPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
} tParams_in_PNDEFRetrieveNPPMessageDriver;

typedef union __tParams_PNDEFRetrieveNPPMessageDriver
{
   tParams_in_PNDEFRetrieveNPPMessageDriver in;
   W_ERROR out;
} tParams_PNDEFRetrieveNPPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFRetrieveSNEPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFRetrieveSNEPMessageDriver 44

typedef struct __tParams_in_PNDEFRetrieveSNEPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
} tParams_in_PNDEFRetrieveSNEPMessageDriver;

typedef union __tParams_PNDEFRetrieveSNEPMessageDriver
{
   tParams_in_PNDEFRetrieveSNEPMessageDriver in;
   W_ERROR out;
} tParams_PNDEFRetrieveSNEPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFSendNPPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSendNPPMessageDriver 45

typedef struct __tParams_in_PNDEFSendNPPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PNDEFSendNPPMessageDriver;

typedef union __tParams_PNDEFSendNPPMessageDriver
{
   tParams_in_PNDEFSendNPPMessageDriver in;
   W_HANDLE out;
} tParams_PNDEFSendNPPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFSendSNEPMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSendSNEPMessageDriver 46

typedef struct __tParams_in_PNDEFSendSNEPMessageDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PNDEFSendSNEPMessageDriver;

typedef union __tParams_PNDEFSendSNEPMessageDriver
{
   tParams_in_PNDEFSendSNEPMessageDriver in;
   W_HANDLE out;
} tParams_PNDEFSendSNEPMessageDriver;

/* -----------------------------------------------------------------------------
      PNDEFSetWorkPerformedNPPDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSetWorkPerformedNPPDriver 47

typedef struct __tParams_in_PNDEFSetWorkPerformedNPPDriver
{
   bool_t bGiveToNextListener;
} tParams_in_PNDEFSetWorkPerformedNPPDriver;

typedef union __tParams_PNDEFSetWorkPerformedNPPDriver
{
   tParams_in_PNDEFSetWorkPerformedNPPDriver in;
} tParams_PNDEFSetWorkPerformedNPPDriver;

/* -----------------------------------------------------------------------------
      PNDEFSetWorkPerformedSNEPDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNDEFSetWorkPerformedSNEPDriver 48

typedef struct __tParams_in_PNDEFSetWorkPerformedSNEPDriver
{
   bool_t bGiveToNextListener;
} tParams_in_PNDEFSetWorkPerformedSNEPDriver;

typedef union __tParams_PNDEFSetWorkPerformedSNEPDriver
{
   tParams_in_PNDEFSetWorkPerformedSNEPDriver in;
} tParams_PNDEFSetWorkPerformedSNEPDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerDriverGetRFActivity()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerDriverGetRFActivity 49

typedef union __tParams_PNFCControllerDriverGetRFActivity
{
   uint32_t out;
} tParams_PNFCControllerDriverGetRFActivity;

/* -----------------------------------------------------------------------------
      PNFCControllerDriverGetRFLock()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerDriverGetRFLock 50

typedef struct __tParams_in_PNFCControllerDriverGetRFLock
{
   uint32_t nLockSet;
} tParams_in_PNFCControllerDriverGetRFLock;

typedef union __tParams_PNFCControllerDriverGetRFLock
{
   tParams_in_PNFCControllerDriverGetRFLock in;
   uint32_t out;
} tParams_PNFCControllerDriverGetRFLock;

/* -----------------------------------------------------------------------------
      PNFCControllerDriverReadInfo()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerDriverReadInfo 51

typedef struct __tParams_in_PNFCControllerDriverReadInfo
{
   void * pBuffer;
   uint32_t nBufferSize;
} tParams_in_PNFCControllerDriverReadInfo;

typedef union __tParams_PNFCControllerDriverReadInfo
{
   tParams_in_PNFCControllerDriverReadInfo in;
   W_ERROR out;
} tParams_PNFCControllerDriverReadInfo;

/* -----------------------------------------------------------------------------
      PNFCControllerFirmwareUpdateDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerFirmwareUpdateDriver 52

typedef struct __tParams_in_PNFCControllerFirmwareUpdateDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pUpdateBuffer;
   uint32_t nUpdateBufferLength;
   uint32_t nMode;
} tParams_in_PNFCControllerFirmwareUpdateDriver;

typedef union __tParams_PNFCControllerFirmwareUpdateDriver
{
   tParams_in_PNFCControllerFirmwareUpdateDriver in;
} tParams_PNFCControllerFirmwareUpdateDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerFirmwareUpdateState()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerFirmwareUpdateState 53

typedef union __tParams_PNFCControllerFirmwareUpdateState
{
   uint32_t out;
} tParams_PNFCControllerFirmwareUpdateState;

/* -----------------------------------------------------------------------------
      PNFCControllerGetMode()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerGetMode 54

typedef union __tParams_PNFCControllerGetMode
{
   uint32_t out;
} tParams_PNFCControllerGetMode;

/* -----------------------------------------------------------------------------
      PNFCControllerGetRawMessageData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerGetRawMessageData 55

typedef struct __tParams_in_PNFCControllerGetRawMessageData
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   uint32_t * pnActualLength;
} tParams_in_PNFCControllerGetRawMessageData;

typedef union __tParams_PNFCControllerGetRawMessageData
{
   tParams_in_PNFCControllerGetRawMessageData in;
   W_ERROR out;
} tParams_PNFCControllerGetRawMessageData;

/* -----------------------------------------------------------------------------
      PNFCControllerIsActive()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerIsActive 56

typedef union __tParams_PNFCControllerIsActive
{
   bool_t out;
} tParams_PNFCControllerIsActive;

/* -----------------------------------------------------------------------------
      PNFCControllerMonitorException()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerMonitorException 57

typedef struct __tParams_in_PNFCControllerMonitorException
{
   tPBasicGenericEventHandler * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tParams_in_PNFCControllerMonitorException;

typedef union __tParams_PNFCControllerMonitorException
{
   tParams_in_PNFCControllerMonitorException in;
   W_ERROR out;
} tParams_PNFCControllerMonitorException;

/* -----------------------------------------------------------------------------
      PNFCControllerMonitorFieldEvents()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerMonitorFieldEvents 58

typedef struct __tParams_in_PNFCControllerMonitorFieldEvents
{
   tPBasicGenericEventHandler * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tParams_in_PNFCControllerMonitorFieldEvents;

typedef union __tParams_PNFCControllerMonitorFieldEvents
{
   tParams_in_PNFCControllerMonitorFieldEvents in;
   W_ERROR out;
} tParams_PNFCControllerMonitorFieldEvents;

/* -----------------------------------------------------------------------------
      PNFCControllerProductionTestDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerProductionTestDriver 59

typedef struct __tParams_in_PNFCControllerProductionTestDriver
{
   const uint8_t * pParameterBuffer;
   uint32_t nParameterBufferLength;
   uint8_t * pResultBuffer;
   uint32_t nResultBufferLength;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PNFCControllerProductionTestDriver;

typedef union __tParams_PNFCControllerProductionTestDriver
{
   tParams_in_PNFCControllerProductionTestDriver in;
} tParams_PNFCControllerProductionTestDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerRegisterRawListener()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerRegisterRawListener 60

typedef struct __tParams_in_PNFCControllerRegisterRawListener
{
   tPBasicGenericDataCallbackFunction * pReceiveMessageEventHandler;
   void * pHandlerParameter;
} tParams_in_PNFCControllerRegisterRawListener;

typedef union __tParams_PNFCControllerRegisterRawListener
{
   tParams_in_PNFCControllerRegisterRawListener in;
   W_ERROR out;
} tParams_PNFCControllerRegisterRawListener;

/* -----------------------------------------------------------------------------
      PNFCControllerResetDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerResetDriver 61

typedef struct __tParams_in_PNFCControllerResetDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint32_t nMode;
} tParams_in_PNFCControllerResetDriver;

typedef union __tParams_PNFCControllerResetDriver
{
   tParams_in_PNFCControllerResetDriver in;
} tParams_PNFCControllerResetDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerSelfTestDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSelfTestDriver 62

typedef struct __tParams_in_PNFCControllerSelfTestDriver
{
   tPNFCControllerSelfTestCompleted * pCallback;
   void * pCallbackParameter;
} tParams_in_PNFCControllerSelfTestDriver;

typedef union __tParams_PNFCControllerSelfTestDriver
{
   tParams_in_PNFCControllerSelfTestDriver in;
} tParams_PNFCControllerSelfTestDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerSetRFLockDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSetRFLockDriver 63

typedef struct __tParams_in_PNFCControllerSetRFLockDriver
{
   uint32_t nLockSet;
   bool_t bReaderLock;
   bool_t bCardLock;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PNFCControllerSetRFLockDriver;

typedef union __tParams_PNFCControllerSetRFLockDriver
{
   tParams_in_PNFCControllerSetRFLockDriver in;
} tParams_PNFCControllerSetRFLockDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerSwitchStandbyMode()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSwitchStandbyMode 64

typedef struct __tParams_in_PNFCControllerSwitchStandbyMode
{
   bool_t bStandbyOn;
} tParams_in_PNFCControllerSwitchStandbyMode;

typedef union __tParams_PNFCControllerSwitchStandbyMode
{
   tParams_in_PNFCControllerSwitchStandbyMode in;
   W_ERROR out;
} tParams_PNFCControllerSwitchStandbyMode;

/* -----------------------------------------------------------------------------
      PNFCControllerSwitchToRawModeDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerSwitchToRawModeDriver 65

typedef struct __tParams_in_PNFCControllerSwitchToRawModeDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PNFCControllerSwitchToRawModeDriver;

typedef union __tParams_PNFCControllerSwitchToRawModeDriver
{
   tParams_in_PNFCControllerSwitchToRawModeDriver in;
} tParams_PNFCControllerSwitchToRawModeDriver;

/* -----------------------------------------------------------------------------
      PNFCControllerWriteRawMessageDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PNFCControllerWriteRawMessageDriver 66

typedef struct __tParams_in_PNFCControllerWriteRawMessageDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pBuffer;
   uint32_t nLength;
} tParams_in_PNFCControllerWriteRawMessageDriver;

typedef union __tParams_PNFCControllerWriteRawMessageDriver
{
   tParams_in_PNFCControllerWriteRawMessageDriver in;
} tParams_PNFCControllerWriteRawMessageDriver;

/* -----------------------------------------------------------------------------
      PP2PConnectDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PConnectDriver 67

typedef struct __tParams_in_PP2PConnectDriver
{
   W_HANDLE hSocket;
   W_HANDLE hLink;
   tPBasicGenericCallbackFunction * pEstablishmentCallback;
   void * pEstablishmentCallbackParameter;
} tParams_in_PP2PConnectDriver;

typedef union __tParams_PP2PConnectDriver
{
   tParams_in_PP2PConnectDriver in;
} tParams_PP2PConnectDriver;

/* -----------------------------------------------------------------------------
      PP2PCreateSocketDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PCreateSocketDriver 68

typedef struct __tParams_in_PP2PCreateSocketDriver
{
   uint8_t nType;
   const char16_t * pServiceURI;
   uint32_t nSize;
   uint8_t nSAP;
   W_HANDLE * phSocket;
} tParams_in_PP2PCreateSocketDriver;

typedef union __tParams_PP2PCreateSocketDriver
{
   tParams_in_PP2PCreateSocketDriver in;
   W_ERROR out;
} tParams_PP2PCreateSocketDriver;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver1()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver1 69

typedef struct __tParams_in_PP2PEstablishLinkDriver1
{
   tPBasicGenericHandleCallbackFunction * pEstablishmentCallback;
   void * pEstablishmentCallbackParameter;
} tParams_in_PP2PEstablishLinkDriver1;

typedef union __tParams_PP2PEstablishLinkDriver1
{
   tParams_in_PP2PEstablishLinkDriver1 in;
   W_HANDLE out;
} tParams_PP2PEstablishLinkDriver1;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver1Wrapper()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver1Wrapper 70

typedef struct __tParams_in_PP2PEstablishLinkDriver1Wrapper
{
   tPBasicGenericHandleCallbackFunction * pEstablishmentCallback;
   void * pEstablishmentCallbackParameter;
} tParams_in_PP2PEstablishLinkDriver1Wrapper;

typedef union __tParams_PP2PEstablishLinkDriver1Wrapper
{
   tParams_in_PP2PEstablishLinkDriver1Wrapper in;
   W_HANDLE out;
} tParams_PP2PEstablishLinkDriver1Wrapper;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver2()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver2 71

typedef struct __tParams_in_PP2PEstablishLinkDriver2
{
   W_HANDLE hLink;
   tPBasicGenericCallbackFunction * pReleaseCallback;
   void * pReleaseCallbackParameter;
   W_HANDLE * phOperation;
} tParams_in_PP2PEstablishLinkDriver2;

typedef union __tParams_PP2PEstablishLinkDriver2
{
   tParams_in_PP2PEstablishLinkDriver2 in;
} tParams_PP2PEstablishLinkDriver2;

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver2Wrapper()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PEstablishLinkDriver2Wrapper 72

typedef struct __tParams_in_PP2PEstablishLinkDriver2Wrapper
{
   W_HANDLE hLink;
   tPBasicGenericCallbackFunction * pReleaseCallback;
   void * pReleaseCallbackParameter;
   W_HANDLE * phOperation;
} tParams_in_PP2PEstablishLinkDriver2Wrapper;

typedef union __tParams_PP2PEstablishLinkDriver2Wrapper
{
   tParams_in_PP2PEstablishLinkDriver2Wrapper in;
} tParams_PP2PEstablishLinkDriver2Wrapper;

/* -----------------------------------------------------------------------------
      PP2PGetConfigurationDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PGetConfigurationDriver 73

typedef struct __tParams_in_PP2PGetConfigurationDriver
{
   tWP2PConfiguration * pConfiguration;
   uint32_t nSize;
} tParams_in_PP2PGetConfigurationDriver;

typedef union __tParams_PP2PGetConfigurationDriver
{
   tParams_in_PP2PGetConfigurationDriver in;
   W_ERROR out;
} tParams_PP2PGetConfigurationDriver;

/* -----------------------------------------------------------------------------
      PP2PGetLinkPropertiesDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PGetLinkPropertiesDriver 74

typedef struct __tParams_in_PP2PGetLinkPropertiesDriver
{
   W_HANDLE hLink;
   tWP2PLinkProperties * pProperties;
   uint32_t nSize;
} tParams_in_PP2PGetLinkPropertiesDriver;

typedef union __tParams_PP2PGetLinkPropertiesDriver
{
   tParams_in_PP2PGetLinkPropertiesDriver in;
   W_ERROR out;
} tParams_PP2PGetLinkPropertiesDriver;

/* -----------------------------------------------------------------------------
      PP2PGetSocketParameterDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PGetSocketParameterDriver 75

typedef struct __tParams_in_PP2PGetSocketParameterDriver
{
   W_HANDLE hSocket;
   uint32_t nParameter;
   uint32_t * pnValue;
} tParams_in_PP2PGetSocketParameterDriver;

typedef union __tParams_PP2PGetSocketParameterDriver
{
   tParams_in_PP2PGetSocketParameterDriver in;
   W_ERROR out;
} tParams_PP2PGetSocketParameterDriver;

/* -----------------------------------------------------------------------------
      PP2PReadDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PReadDriver 76

typedef struct __tParams_in_PP2PReadDriver
{
   W_HANDLE hConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint8_t * pReceptionBuffer;
   uint32_t nReceptionBufferLength;
   W_HANDLE * phOperation;
} tParams_in_PP2PReadDriver;

typedef union __tParams_PP2PReadDriver
{
   tParams_in_PP2PReadDriver in;
} tParams_PP2PReadDriver;

/* -----------------------------------------------------------------------------
      PP2PRecvFromDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PRecvFromDriver 77

typedef struct __tParams_in_PP2PRecvFromDriver
{
   W_HANDLE hSocket;
   tPP2PRecvFromCompleted * pCallback;
   void * pCallbackParameter;
   uint8_t * pReceptionBuffer;
   uint32_t nReceptionBufferLength;
   W_HANDLE * phOperation;
} tParams_in_PP2PRecvFromDriver;

typedef union __tParams_PP2PRecvFromDriver
{
   tParams_in_PP2PRecvFromDriver in;
} tParams_PP2PRecvFromDriver;

/* -----------------------------------------------------------------------------
      PP2PSendToDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PSendToDriver 78

typedef struct __tParams_in_PP2PSendToDriver
{
   W_HANDLE hSocket;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint8_t nSAP;
   const uint8_t * pSendBuffer;
   uint32_t nSendBufferLength;
   W_HANDLE * phOperation;
} tParams_in_PP2PSendToDriver;

typedef union __tParams_PP2PSendToDriver
{
   tParams_in_PP2PSendToDriver in;
} tParams_PP2PSendToDriver;

/* -----------------------------------------------------------------------------
      PP2PSetConfigurationDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PSetConfigurationDriver 79

typedef struct __tParams_in_PP2PSetConfigurationDriver
{
   const tWP2PConfiguration * pConfiguration;
   uint32_t nSize;
} tParams_in_PP2PSetConfigurationDriver;

typedef union __tParams_PP2PSetConfigurationDriver
{
   tParams_in_PP2PSetConfigurationDriver in;
   W_ERROR out;
} tParams_PP2PSetConfigurationDriver;

/* -----------------------------------------------------------------------------
      PP2PSetSocketParameter()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PSetSocketParameter 80

typedef struct __tParams_in_PP2PSetSocketParameter
{
   W_HANDLE hSocket;
   uint32_t nParameter;
   uint32_t nValue;
} tParams_in_PP2PSetSocketParameter;

typedef union __tParams_PP2PSetSocketParameter
{
   tParams_in_PP2PSetSocketParameter in;
   W_ERROR out;
} tParams_PP2PSetSocketParameter;

/* -----------------------------------------------------------------------------
      PP2PShutdownDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PShutdownDriver 81

typedef struct __tParams_in_PP2PShutdownDriver
{
   W_HANDLE hSocket;
   tPBasicGenericCallbackFunction * pReleaseCallback;
   void * pReleaseCallbackParameter;
} tParams_in_PP2PShutdownDriver;

typedef union __tParams_PP2PShutdownDriver
{
   tParams_in_PP2PShutdownDriver in;
} tParams_PP2PShutdownDriver;

/* -----------------------------------------------------------------------------
      PP2PURILookupDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PURILookupDriver 82

typedef struct __tParams_in_PP2PURILookupDriver
{
   W_HANDLE hLink;
   tPP2PURILookupCompleted * pCallback;
   void * pCallbackParameter;
   const char16_t * pServiceURI;
   uint32_t nSize;
} tParams_in_PP2PURILookupDriver;

typedef union __tParams_PP2PURILookupDriver
{
   tParams_in_PP2PURILookupDriver in;
} tParams_PP2PURILookupDriver;

/* -----------------------------------------------------------------------------
      PP2PWriteDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PP2PWriteDriver 83

typedef struct __tParams_in_PP2PWriteDriver
{
   W_HANDLE hConnection;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pSendBuffer;
   uint32_t nSendBufferLength;
   W_HANDLE * phOperation;
} tParams_in_PP2PWriteDriver;

typedef union __tParams_PP2PWriteDriver
{
   tParams_in_PP2PWriteDriver in;
} tParams_PP2PWriteDriver;

/* -----------------------------------------------------------------------------
      PReaderDriverGetLastReferenceTime()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverGetLastReferenceTime 84

typedef union __tParams_PReaderDriverGetLastReferenceTime
{
   uint32_t out;
} tParams_PReaderDriverGetLastReferenceTime;

/* -----------------------------------------------------------------------------
      PReaderDriverGetNbCardDetected()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverGetNbCardDetected 85

typedef union __tParams_PReaderDriverGetNbCardDetected
{
   uint8_t out;
} tParams_PReaderDriverGetNbCardDetected;

/* -----------------------------------------------------------------------------
      PReaderDriverRedetectCard()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverRedetectCard 86

typedef struct __tParams_in_PReaderDriverRedetectCard
{
   W_HANDLE hConnection;
} tParams_in_PReaderDriverRedetectCard;

typedef union __tParams_PReaderDriverRedetectCard
{
   tParams_in_PReaderDriverRedetectCard in;
   W_ERROR out;
} tParams_PReaderDriverRedetectCard;

/* -----------------------------------------------------------------------------
      PReaderDriverRegister()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverRegister 87

typedef struct __tParams_in_PReaderDriverRegister
{
   tPReaderDriverRegisterCompleted * pCallback;
   void * pCallbackParameter;
   uint8_t nPriority;
   uint32_t nRequestedProtocolsBF;
   uint32_t nDetectionConfigurationLength;
   uint8_t * pBuffer;
   uint32_t nBufferMaxLength;
   W_HANDLE * phListenerHandle;
} tParams_in_PReaderDriverRegister;

typedef union __tParams_PReaderDriverRegister
{
   tParams_in_PReaderDriverRegister in;
   W_ERROR out;
} tParams_PReaderDriverRegister;

/* -----------------------------------------------------------------------------
      PReaderDriverSetWorkPerformedAndClose()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverSetWorkPerformedAndClose 88

typedef struct __tParams_in_PReaderDriverSetWorkPerformedAndClose
{
   W_HANDLE hDriverListener;
} tParams_in_PReaderDriverSetWorkPerformedAndClose;

typedef union __tParams_PReaderDriverSetWorkPerformedAndClose
{
   tParams_in_PReaderDriverSetWorkPerformedAndClose in;
   W_ERROR out;
} tParams_PReaderDriverSetWorkPerformedAndClose;

/* -----------------------------------------------------------------------------
      PReaderDriverWorkPerformed()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderDriverWorkPerformed 89

typedef struct __tParams_in_PReaderDriverWorkPerformed
{
   W_HANDLE hConnection;
   bool_t bGiveToNextListener;
   bool_t bCardApplicationMatch;
} tParams_in_PReaderDriverWorkPerformed;

typedef union __tParams_PReaderDriverWorkPerformed
{
   tParams_in_PReaderDriverWorkPerformed in;
   W_ERROR out;
} tParams_PReaderDriverWorkPerformed;

/* -----------------------------------------------------------------------------
      PReaderErrorEventRegister()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderErrorEventRegister 90

typedef struct __tParams_in_PReaderErrorEventRegister
{
   tPBasicGenericEventHandler * pHandler;
   void * pHandlerParameter;
   uint8_t nEventType;
   bool_t bCardDetectionRequested;
   W_HANDLE * phRegistryHandle;
} tParams_in_PReaderErrorEventRegister;

typedef union __tParams_PReaderErrorEventRegister
{
   tParams_in_PReaderErrorEventRegister in;
   W_ERROR out;
} tParams_PReaderErrorEventRegister;

/* -----------------------------------------------------------------------------
      PReaderGetPulsePeriod()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderGetPulsePeriod 91

typedef struct __tParams_in_PReaderGetPulsePeriod
{
   uint32_t * pnTimeout;
} tParams_in_PReaderGetPulsePeriod;

typedef union __tParams_PReaderGetPulsePeriod
{
   tParams_in_PReaderGetPulsePeriod in;
   W_ERROR out;
} tParams_PReaderGetPulsePeriod;

/* -----------------------------------------------------------------------------
      PReaderSetPulsePeriodDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PReaderSetPulsePeriodDriver 92

typedef struct __tParams_in_PReaderSetPulsePeriodDriver
{
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint32_t nPulsePeriod;
} tParams_in_PReaderSetPulsePeriodDriver;

typedef union __tParams_PReaderSetPulsePeriodDriver
{
   tParams_in_PReaderSetPulsePeriodDriver in;
} tParams_PReaderSetPulsePeriodDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableApplyDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableApplyDriver 93

typedef struct __tParams_in_PRoutingTableApplyDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PRoutingTableApplyDriver;

typedef union __tParams_PRoutingTableApplyDriver
{
   tParams_in_PRoutingTableApplyDriver in;
} tParams_PRoutingTableApplyDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableGetConfigDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableGetConfigDriver 94

typedef union __tParams_PRoutingTableGetConfigDriver
{
   uint32_t out;
} tParams_PRoutingTableGetConfigDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableReadDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableReadDriver 95

typedef struct __tParams_in_PRoutingTableReadDriver
{
   uint8_t * pBuffer;
   uint32_t nBufferLength;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PRoutingTableReadDriver;

typedef union __tParams_PRoutingTableReadDriver
{
   tParams_in_PRoutingTableReadDriver in;
} tParams_PRoutingTableReadDriver;

/* -----------------------------------------------------------------------------
      PRoutingTableSetConfigDriver()
----------------------------------------------------------------------------- */

#define P_Idenfier_PRoutingTableSetConfigDriver 96

typedef struct __tParams_in_PRoutingTableSetConfigDriver
{
   uint32_t nConfig;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PRoutingTableSetConfigDriver;

typedef union __tParams_PRoutingTableSetConfigDriver
{
   tParams_in_PRoutingTableSetConfigDriver in;
} tParams_PRoutingTableSetConfigDriver;

/* -----------------------------------------------------------------------------
      PSecurityManagerDriverAuthenticate()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSecurityManagerDriverAuthenticate 97

typedef struct __tParams_in_PSecurityManagerDriverAuthenticate
{
   const uint8_t * pApplicationDataBuffer;
   uint32_t nApplicationDataBufferLength;
} tParams_in_PSecurityManagerDriverAuthenticate;

typedef union __tParams_PSecurityManagerDriverAuthenticate
{
   tParams_in_PSecurityManagerDriverAuthenticate in;
   W_ERROR out;
} tParams_PSecurityManagerDriverAuthenticate;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmCloseChannel()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmCloseChannel 98

typedef struct __tParams_in_PSeDriver7816SmCloseChannel
{
   W_HANDLE hDriverConnection;
   uint32_t nChannelReference;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PSeDriver7816SmCloseChannel;

typedef union __tParams_PSeDriver7816SmCloseChannel
{
   tParams_in_PSeDriver7816SmCloseChannel in;
   W_ERROR out;
} tParams_PSeDriver7816SmCloseChannel;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmExchangeApdu()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmExchangeApdu 99

typedef struct __tParams_in_PSeDriver7816SmExchangeApdu
{
   W_HANDLE hDriverConnection;
   uint32_t nChannelReference;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pSendApduBuffer;
   uint32_t nSendApduBufferLength;
   uint8_t * pReceivedApduBuffer;
   uint32_t nReceivedApduBufferMaxLength;
} tParams_in_PSeDriver7816SmExchangeApdu;

typedef union __tParams_PSeDriver7816SmExchangeApdu
{
   tParams_in_PSeDriver7816SmExchangeApdu in;
   W_ERROR out;
} tParams_PSeDriver7816SmExchangeApdu;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmGetData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmGetData 100

typedef struct __tParams_in_PSeDriver7816SmGetData
{
   W_HANDLE hDriverConnection;
   uint32_t nChannelReference;
   uint32_t nType;
   uint8_t * pBuffer;
   uint32_t nBufferMaxLength;
   uint32_t * pnActualLength;
} tParams_in_PSeDriver7816SmGetData;

typedef union __tParams_PSeDriver7816SmGetData
{
   tParams_in_PSeDriver7816SmGetData in;
   W_ERROR out;
} tParams_PSeDriver7816SmGetData;

/* -----------------------------------------------------------------------------
      PSeDriver7816SmOpenChannel()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSeDriver7816SmOpenChannel 101

typedef struct __tParams_in_PSeDriver7816SmOpenChannel
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   uint32_t nType;
   const uint8_t * pAID;
   uint32_t nAIDLength;
} tParams_in_PSeDriver7816SmOpenChannel;

typedef union __tParams_PSeDriver7816SmOpenChannel
{
   tParams_in_PSeDriver7816SmOpenChannel in;
   W_ERROR out;
} tParams_PSeDriver7816SmOpenChannel;

/* -----------------------------------------------------------------------------
      PSEDriverActivateSwpLine()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverActivateSwpLine 102

typedef struct __tParams_in_PSEDriverActivateSwpLine
{
   uint32_t nSlotIdentifier;
} tParams_in_PSEDriverActivateSwpLine;

typedef union __tParams_PSEDriverActivateSwpLine
{
   tParams_in_PSEDriverActivateSwpLine in;
   W_ERROR out;
} tParams_PSEDriverActivateSwpLine;

/* -----------------------------------------------------------------------------
      PSEDriverGetAtr()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverGetAtr 103

typedef struct __tParams_in_PSEDriverGetAtr
{
   W_HANDLE hDriverConnection;
   uint8_t * pAtrBuffer;
   uint32_t nAtrBufferLength;
   uint32_t * pnAtrLength;
} tParams_in_PSEDriverGetAtr;

typedef union __tParams_PSEDriverGetAtr
{
   tParams_in_PSEDriverGetAtr in;
   W_ERROR out;
} tParams_PSEDriverGetAtr;

/* -----------------------------------------------------------------------------
      PSEDriverGetInfo()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverGetInfo 104

typedef struct __tParams_in_PSEDriverGetInfo
{
   uint32_t nSlotIdentifier;
   tWSEInfoEx * pSEInfo;
   uint32_t nSize;
} tParams_in_PSEDriverGetInfo;

typedef union __tParams_PSEDriverGetInfo
{
   tParams_in_PSEDriverGetInfo in;
   W_ERROR out;
} tParams_PSEDriverGetInfo;

/* -----------------------------------------------------------------------------
      PSEDriverGetStatus()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverGetStatus 105

typedef struct __tParams_in_PSEDriverGetStatus
{
   uint32_t nSlotIdentifier;
   tPSEGetStatusCompleted * pCallback;
   void * pCallbackParameter;
} tParams_in_PSEDriverGetStatus;

typedef union __tParams_PSEDriverGetStatus
{
   tParams_in_PSEDriverGetStatus in;
} tParams_PSEDriverGetStatus;

/* -----------------------------------------------------------------------------
      PSEDriverImpersonateAndCheckAidAccess()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverImpersonateAndCheckAidAccess 106

typedef struct __tParams_in_PSEDriverImpersonateAndCheckAidAccess
{
   uint32_t nSlotIdentifier;
   const uint8_t * pAIDBuffer;
   uint32_t nAIDLength;
   const uint8_t * pImpersonationDataBuffer;
   uint32_t nImpersonationDataBufferLength;
} tParams_in_PSEDriverImpersonateAndCheckAidAccess;

typedef union __tParams_PSEDriverImpersonateAndCheckAidAccess
{
   tParams_in_PSEDriverImpersonateAndCheckAidAccess in;
   W_ERROR out;
} tParams_PSEDriverImpersonateAndCheckAidAccess;

/* -----------------------------------------------------------------------------
      PSEDriverOpenConnection()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverOpenConnection 107

typedef struct __tParams_in_PSEDriverOpenConnection
{
   uint32_t nSlotIdentifier;
   bool_t bForce;
   tPBasicGenericHandleCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PSEDriverOpenConnection;

typedef union __tParams_PSEDriverOpenConnection
{
   tParams_in_PSEDriverOpenConnection in;
} tParams_PSEDriverOpenConnection;

/* -----------------------------------------------------------------------------
      PSEDriverSetPolicy()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEDriverSetPolicy 108

typedef struct __tParams_in_PSEDriverSetPolicy
{
   uint32_t nSlotIdentifier;
   uint32_t nStorageType;
   uint32_t nProtocols;
   tPBasicGenericCallbackFunction * pCallback;
   void * pCallbackParameter;
} tParams_in_PSEDriverSetPolicy;

typedef union __tParams_PSEDriverSetPolicy
{
   tParams_in_PSEDriverSetPolicy in;
} tParams_PSEDriverSetPolicy;

/* -----------------------------------------------------------------------------
      PSEGetConnectivityEventParameter()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEGetConnectivityEventParameter 109

typedef struct __tParams_in_PSEGetConnectivityEventParameter
{
   uint32_t nSlotIdentifier;
   uint8_t * pDataBuffer;
   uint32_t nBufferLength;
   uint32_t * pnActualDataLength;
} tParams_in_PSEGetConnectivityEventParameter;

typedef union __tParams_PSEGetConnectivityEventParameter
{
   tParams_in_PSEGetConnectivityEventParameter in;
   W_ERROR out;
} tParams_PSEGetConnectivityEventParameter;

/* -----------------------------------------------------------------------------
      PSEGetTransactionAID()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEGetTransactionAID 110

typedef struct __tParams_in_PSEGetTransactionAID
{
   uint32_t nSlotIdentifier;
   uint8_t * pBuffer;
   uint32_t nBufferLength;
} tParams_in_PSEGetTransactionAID;

typedef union __tParams_PSEGetTransactionAID
{
   tParams_in_PSEGetTransactionAID in;
   uint32_t out;
} tParams_PSEGetTransactionAID;

/* -----------------------------------------------------------------------------
      PSEMonitorConnectivityEvent()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEMonitorConnectivityEvent 111

typedef struct __tParams_in_PSEMonitorConnectivityEvent
{
   uint32_t nSlotIdentifier;
   tPBasicGenericEventHandler2 * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tParams_in_PSEMonitorConnectivityEvent;

typedef union __tParams_PSEMonitorConnectivityEvent
{
   tParams_in_PSEMonitorConnectivityEvent in;
   W_ERROR out;
} tParams_PSEMonitorConnectivityEvent;

/* -----------------------------------------------------------------------------
      PSEMonitorEndOfTransaction()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEMonitorEndOfTransaction 112

typedef struct __tParams_in_PSEMonitorEndOfTransaction
{
   uint32_t nSlotIdentifier;
   tPBasicGenericEventHandler2 * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tParams_in_PSEMonitorEndOfTransaction;

typedef union __tParams_PSEMonitorEndOfTransaction
{
   tParams_in_PSEMonitorEndOfTransaction in;
   W_ERROR out;
} tParams_PSEMonitorEndOfTransaction;

/* -----------------------------------------------------------------------------
      PSEMonitorHotPlugEvents()
----------------------------------------------------------------------------- */

#define P_Idenfier_PSEMonitorHotPlugEvents 113

typedef struct __tParams_in_PSEMonitorHotPlugEvents
{
   uint32_t nSlotIdentifier;
   tPBasicGenericEventHandler2 * pHandler;
   void * pHandlerParameter;
   W_HANDLE * phEventRegistry;
} tParams_in_PSEMonitorHotPlugEvents;

typedef union __tParams_PSEMonitorHotPlugEvents
{
   tParams_in_PSEMonitorHotPlugEvents in;
   W_ERROR out;
} tParams_PSEMonitorHotPlugEvents;

/* -----------------------------------------------------------------------------
      PType1ChipDriverExchangeData()
----------------------------------------------------------------------------- */

#define P_Idenfier_PType1ChipDriverExchangeData 114

typedef struct __tParams_in_PType1ChipDriverExchangeData
{
   W_HANDLE hDriverConnection;
   tPBasicGenericDataCallbackFunction * pCallback;
   void * pCallbackParameter;
   const uint8_t * pReaderToCardBuffer;
   uint32_t nReaderToCardBufferLength;
   uint8_t * pCardToReaderBuffer;
   uint32_t nCardToReaderBufferMaxLength;
} tParams_in_PType1ChipDriverExchangeData;

typedef union __tParams_PType1ChipDriverExchangeData
{
   tParams_in_PType1ChipDriverExchangeData in;
   W_HANDLE out;
} tParams_PType1ChipDriverExchangeData;

#define P_DRIVER_FUNCTION_COUNT 115

#endif /* P_CONFIG_USER || P_CONFIG_DRIVER */

#endif /* #ifndef P_CONFIG_CLIENT_SERVER */


#endif /* #ifdef __WME_DRIVER_AUTOGEN_H */
