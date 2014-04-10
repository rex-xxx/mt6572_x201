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

#include "wme_context.h"

#ifndef P_CONFIG_CLIENT_SERVER

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE P14P3DriverExchangeData(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength,
      bool_t bCheckResponseCRC,
      bool_t bCheckAckOrNack )
{
   W_ERROR nError;
   tParams_P14P3DriverExchangeData params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   params.in.bCheckResponseCRC = bCheckResponseCRC;
   params.in.bCheckAckOrNack = bCheckAckOrNack;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P14P3DriverExchangeData,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverExchangeData: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverExchangeData(
           tContext* pContext,
           tParams_P14P3DriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = P14P3DriverExchangeData(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, pParams->in.bCheckResponseCRC, pParams->in.bCheckAckOrNack);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeRawBits()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE P14P3DriverExchangeRawBits(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t nReaderToCardBufferLastByteBitNumber,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength,
      uint8_t nExpectedBits )
{
   W_ERROR nError;
   tParams_P14P3DriverExchangeRawBits params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.nReaderToCardBufferLastByteBitNumber = nReaderToCardBufferLastByteBitNumber;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   params.in.nExpectedBits = nExpectedBits;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P14P3DriverExchangeRawBits,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverExchangeRawBits: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverExchangeRawBits(
           tContext* pContext,
           tParams_P14P3DriverExchangeRawBits* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = P14P3DriverExchangeRawBits(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.nReaderToCardBufferLastByteBitNumber, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, pParams->in.nExpectedBits);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      P14P3DriverExchangeRawMifare()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE P14P3DriverExchangeRawMifare(
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;
   tParams_P14P3DriverExchangeRawMifare params;
   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P14P3DriverExchangeRawMifare,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverExchangeRawMifare: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverExchangeRawMifare(
           tContext* pContext,
           tParams_P14P3DriverExchangeRawMifare* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = P14P3DriverExchangeRawMifare(pContext, pParams->in.hConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      P14P3DriverSetTimeout()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR P14P3DriverSetTimeout(
      tContext * pContext,
      W_HANDLE hConnection,
      uint32_t nTimeout )
{
   W_ERROR nError;
   tParams_P14P3DriverSetTimeout params;
   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P14P3DriverSetTimeout,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverSetTimeout: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverSetTimeout(
           tContext* pContext,
           tParams_P14P3DriverSetTimeout* pParams)
{
   pParams->out = P14P3DriverSetTimeout(pContext, pParams->in.hConnection, pParams->in.nTimeout);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      P14P4DriverExchangeData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE P14P4DriverExchangeData(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength,
      bool_t bSendNAD,
      uint8_t nNAD,
      bool_t bCreateOperation )
{
   W_ERROR nError;
   tParams_P14P4DriverExchangeData params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   params.in.bSendNAD = bSendNAD;
   params.in.nNAD = nNAD;
   params.in.bCreateOperation = bCreateOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P14P4DriverExchangeData,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P4DriverExchangeData: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P4DriverExchangeData(
           tContext* pContext,
           tParams_P14P4DriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = P14P4DriverExchangeData(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, pParams->in.bSendNAD, pParams->in.nNAD, pParams->in.bCreateOperation);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      P14P4DriverSetTimeout()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR P14P4DriverSetTimeout(
      tContext * pContext,
      W_HANDLE hConnection,
      uint32_t nTimeout )
{
   W_ERROR nError;
   tParams_P14P4DriverSetTimeout params;
   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P14P4DriverSetTimeout,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P4DriverSetTimeout: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P4DriverSetTimeout(
           tContext* pContext,
           tParams_P14P4DriverSetTimeout* pParams)
{
   pParams->out = P14P4DriverSetTimeout(pContext, pParams->in.hConnection, pParams->in.nTimeout);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      P15P3DriverExchangeData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void P15P3DriverExchangeData(
      tContext * pContext,
      W_HANDLE hConnection,
      tP15P3DriverExchangeDataCompleted * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;
   tParams_P15P3DriverExchangeData params;
   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P15P3DriverExchangeData,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P15P3DriverExchangeData: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P15P3DriverExchangeData(
           tContext* pContext,
           tParams_P15P3DriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   P15P3DriverExchangeData(pContext, pParams->in.hConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      P15P3DriverSetTimeout()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR P15P3DriverSetTimeout(
      tContext * pContext,
      W_HANDLE hConnection,
      uint32_t nTimeout )
{
   W_ERROR nError;
   tParams_P15P3DriverSetTimeout params;
   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_P15P3DriverSetTimeout,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("P15P3DriverSetTimeout: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P15P3DriverSetTimeout(
           tContext* pContext,
           tParams_P15P3DriverSetTimeout* pParams)
{
   pParams->out = P15P3DriverSetTimeout(pContext, pParams->in.hConnection, pParams->in.nTimeout);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PBasicDriverCancelOperation()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PBasicDriverCancelOperation(
      tContext * pContext,
      W_HANDLE hOperation )
{
   W_ERROR nError;
   tParams_PBasicDriverCancelOperation params;
   params.in.hOperation = hOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PBasicDriverCancelOperation,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PBasicDriverCancelOperation: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBasicDriverCancelOperation(
           tContext* pContext,
           tParams_PBasicDriverCancelOperation* pParams)
{
   PBasicDriverCancelOperation(pContext, pParams->in.hOperation);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PBasicDriverGetVersion()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PBasicDriverGetVersion(
      tContext * pContext,
      void * pBuffer,
      uint32_t nBufferSize )
{
   W_ERROR nError;
   tParams_PBasicDriverGetVersion params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferSize = nBufferSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PBasicDriverGetVersion,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PBasicDriverGetVersion: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBasicDriverGetVersion(
           tContext* pContext,
           tParams_PBasicDriverGetVersion* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (void *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferSize, ( P_SYNC_BUFFER_FLAG_O ))) == (void *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PBasicDriverGetVersion(pContext, pParams->in.pBuffer, pParams->in.nBufferSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PBPrimeDriverExchangeData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE PBPrimeDriverExchangeData(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;
   tParams_PBPrimeDriverExchangeData params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PBPrimeDriverExchangeData,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PBPrimeDriverExchangeData: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBPrimeDriverExchangeData(
           tContext* pContext,
           tParams_PBPrimeDriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PBPrimeDriverExchangeData(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PBPrimeDriverSetTimeout()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PBPrimeDriverSetTimeout(
      tContext * pContext,
      W_HANDLE hConnection,
      uint32_t nTimeout )
{
   W_ERROR nError;
   tParams_PBPrimeDriverSetTimeout params;
   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PBPrimeDriverSetTimeout,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PBPrimeDriverSetTimeout: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBPrimeDriverSetTimeout(
           tContext* pContext,
           tParams_PBPrimeDriverSetTimeout* pParams)
{
   pParams->out = PBPrimeDriverSetTimeout(pContext, pParams->in.hConnection, pParams->in.nTimeout);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PCacheConnectionDriverRead()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PCacheConnectionDriverRead(
      tContext * pContext,
      tCacheConnectionInstance * pCacheConnection,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PCacheConnectionDriverRead params;
   params.in.pCacheConnection = pCacheConnection;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PCacheConnectionDriverRead,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PCacheConnectionDriverRead: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PCacheConnectionDriverRead(
           tContext* pContext,
           tParams_PCacheConnectionDriverRead* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pCacheConnection = (tCacheConnectionInstance *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCacheConnection, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tCacheConnectionInstance *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PCacheConnectionDriverRead(pContext, pParams->in.pCacheConnection, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PCacheConnectionDriverWrite()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PCacheConnectionDriverWrite(
      tContext * pContext,
      const tCacheConnectionInstance * pCacheConnection,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PCacheConnectionDriverWrite params;
   params.in.pCacheConnection = pCacheConnection;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PCacheConnectionDriverWrite,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PCacheConnectionDriverWrite: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PCacheConnectionDriverWrite(
           tContext* pContext,
           tParams_PCacheConnectionDriverWrite* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pCacheConnection = (const tCacheConnectionInstance *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCacheConnection, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tCacheConnectionInstance *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PCacheConnectionDriverWrite(pContext, pParams->in.pCacheConnection, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PContextDriverGenerateRandom()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PContextDriverGenerateRandom(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PContextDriverGenerateRandom params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PContextDriverGenerateRandom,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PContextDriverGenerateRandom: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PContextDriverGenerateRandom(
           tContext* pContext,
           tParams_PContextDriverGenerateRandom* pParams)
{
   pParams->out = PContextDriverGenerateRandom(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PContextDriverGetMemoryStatistics()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PContextDriverGetMemoryStatistics(
      tContext * pContext,
      tContextDriverMemoryStatistics * pStatistics,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PContextDriverGetMemoryStatistics params;
   params.in.pStatistics = pStatistics;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PContextDriverGetMemoryStatistics,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PContextDriverGetMemoryStatistics: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PContextDriverGetMemoryStatistics(
           tContext* pContext,
           tParams_PContextDriverGetMemoryStatistics* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pStatistics = (tContextDriverMemoryStatistics *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pStatistics, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tContextDriverMemoryStatistics *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PContextDriverGetMemoryStatistics(pContext, pParams->in.pStatistics, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PContextDriverResetMemoryStatistics()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PContextDriverResetMemoryStatistics(
      tContext * pContext )
{
   W_ERROR nError;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PContextDriverResetMemoryStatistics,
       null,
       0, 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PContextDriverResetMemoryStatistics: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PContextDriverResetMemoryStatistics(
           tContext* pContext,
           void* pParams)
{
   PContextDriverResetMemoryStatistics(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PDFCDriverInterruptEventLoop()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PDFCDriverInterruptEventLoop(
      tContext * pContext )
{
   W_ERROR nError;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PDFCDriverInterruptEventLoop,
       null,
       0, 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PDFCDriverInterruptEventLoop: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PDFCDriverInterruptEventLoop(
           tContext* pContext,
           void* pParams)
{
   PDFCDriverInterruptEventLoop(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PDFCDriverStopEventLoop()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PDFCDriverStopEventLoop(
      tContext * pContext )
{
   W_ERROR nError;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PDFCDriverStopEventLoop,
       null,
       0, 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PDFCDriverStopEventLoop: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PDFCDriverStopEventLoop(
           tContext* pContext,
           void* pParams)
{
   PDFCDriverStopEventLoop(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulCloseDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PEmulCloseDriver(
      tContext * pContext,
      W_HANDLE hHandle,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PEmulCloseDriver params;
   params.in.hHandle = hHandle;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulCloseDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulCloseDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulCloseDriver(
           tContext* pContext,
           tParams_PEmulCloseDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulCloseDriver(pContext, pParams->in.hHandle, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulGetMessageData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PEmulGetMessageData(
      tContext* pContext,
      W_HANDLE hHandle,
      uint8_t * pDataBuffer,
      uint32_t nDataLength,
      uint32_t * pnActualDataLength )
{
   W_ERROR nError;
   tParams_PEmulGetMessageData params;
   params.in.hHandle = hHandle;
   params.in.pDataBuffer = pDataBuffer;
   params.in.nDataLength = nDataLength;
   params.in.pnActualDataLength = pnActualDataLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulGetMessageData,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulGetMessageData: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulGetMessageData(
           tContext* pContext,
           tParams_PEmulGetMessageData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pDataBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pDataBuffer, pParams->in.nDataLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pnActualDataLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnActualDataLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PEmulGetMessageData(pContext, pParams->in.hHandle, pParams->in.pDataBuffer, pParams->in.nDataLength, pParams->in.pnActualDataLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver1()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PEmulOpenConnectionDriver1(
      tContext* pContext,
      tPBasicGenericCallbackFunction * pOpenCallback,
      void * pOpenCallbackParameter,
      const tWEmulConnectionInfo * pEmulConnectionInfo,
      uint32_t nSize,
      W_HANDLE * phHandle )
{
   W_ERROR nError;
   tParams_PEmulOpenConnectionDriver1 params;
   params.in.pOpenCallback = pOpenCallback;
   params.in.pOpenCallbackParameter = pOpenCallbackParameter;
   params.in.pEmulConnectionInfo = pEmulConnectionInfo;
   params.in.nSize = nSize;
   params.in.phHandle = phHandle;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulOpenConnectionDriver1,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulOpenConnectionDriver1: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver1(
           tContext* pContext,
           tParams_PEmulOpenConnectionDriver1* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternal(pContext, (tDFCCallback*)(pParams->in.pOpenCallback), pParams->in.pOpenCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pEmulConnectionInfo = (const tWEmulConnectionInfo *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pEmulConnectionInfo, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tWEmulConnectionInfo *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phHandle, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PEmulOpenConnectionDriver1(pContext, null, pDriverCC, pParams->in.pEmulConnectionInfo, pParams->in.nSize, pParams->in.phHandle);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver1Ex()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PEmulOpenConnectionDriver1Ex(
      tContext * pContext,
      tPBasicGenericCallbackFunction * pOpenCallback,
      void * pOpenCallbackParameter,
      const tWEmulConnectionInfo * pEmulConnectionInfo,
      uint32_t nSize,
      W_HANDLE * phHandle )
{
   W_ERROR nError;
   tParams_PEmulOpenConnectionDriver1Ex params;
   params.in.pOpenCallback = pOpenCallback;
   params.in.pOpenCallbackParameter = pOpenCallbackParameter;
   params.in.pEmulConnectionInfo = pEmulConnectionInfo;
   params.in.nSize = nSize;
   params.in.phHandle = phHandle;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulOpenConnectionDriver1Ex,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulOpenConnectionDriver1Ex: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver1Ex(
           tContext* pContext,
           tParams_PEmulOpenConnectionDriver1Ex* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pOpenCallback), pParams->in.pOpenCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pEmulConnectionInfo = (const tWEmulConnectionInfo *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pEmulConnectionInfo, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tWEmulConnectionInfo *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phHandle, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PEmulOpenConnectionDriver1Ex(pContext, null, pDriverCC, pParams->in.pEmulConnectionInfo, pParams->in.nSize, pParams->in.phHandle);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver2()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PEmulOpenConnectionDriver2(
      tContext* pContext,
      W_HANDLE hHandle,
      tPEmulDriverEventReceived * pEventCallback,
      void * pEventCallbackParameter )
{
   W_ERROR nError;
   tParams_PEmulOpenConnectionDriver2 params;
   params.in.hHandle = hHandle;
   params.in.pEventCallback = pEventCallback;
   params.in.pEventCallbackParameter = pEventCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulOpenConnectionDriver2,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulOpenConnectionDriver2: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver2(
           tContext* pContext,
           tParams_PEmulOpenConnectionDriver2* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pEventCallback), pParams->in.pEventCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver2(pContext, pParams->in.hHandle, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver2Ex()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PEmulOpenConnectionDriver2Ex(
      tContext * pContext,
      W_HANDLE hHandle,
      tPEmulDriverEventReceived * pEventCallback,
      void * pEventCallbackParameter )
{
   W_ERROR nError;
   tParams_PEmulOpenConnectionDriver2Ex params;
   params.in.hHandle = hHandle;
   params.in.pEventCallback = pEventCallback;
   params.in.pEventCallbackParameter = pEventCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulOpenConnectionDriver2Ex,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulOpenConnectionDriver2Ex: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver2Ex(
           tContext* pContext,
           tParams_PEmulOpenConnectionDriver2Ex* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->in.pEventCallback), pParams->in.pEventCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver2Ex(pContext, pParams->in.hHandle, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver3()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PEmulOpenConnectionDriver3(
      tContext* pContext,
      W_HANDLE hHandle,
      tPEmulDriverCommandReceived * pCommandCallback,
      void * pCommandCallbackParameter )
{
   W_ERROR nError;
   tParams_PEmulOpenConnectionDriver3 params;
   params.in.hHandle = hHandle;
   params.in.pCommandCallback = pCommandCallback;
   params.in.pCommandCallbackParameter = pCommandCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulOpenConnectionDriver3,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulOpenConnectionDriver3: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver3(
           tContext* pContext,
           tParams_PEmulOpenConnectionDriver3* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pCommandCallback), pParams->in.pCommandCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver3(pContext, pParams->in.hHandle, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulOpenConnectionDriver3Ex()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PEmulOpenConnectionDriver3Ex(
      tContext * pContext,
      W_HANDLE hHandle,
      tPEmulDriverCommandReceived * pCommandCallback,
      void * pCommandCallbackParameter )
{
   W_ERROR nError;
   tParams_PEmulOpenConnectionDriver3Ex params;
   params.in.hHandle = hHandle;
   params.in.pCommandCallback = pCommandCallback;
   params.in.pCommandCallbackParameter = pCommandCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulOpenConnectionDriver3Ex,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulOpenConnectionDriver3Ex: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver3Ex(
           tContext* pContext,
           tParams_PEmulOpenConnectionDriver3Ex* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->in.pCommandCallback), pParams->in.pCommandCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver3Ex(pContext, pParams->in.hHandle, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PEmulSendAnswer()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PEmulSendAnswer(
      tContext* pContext,
      W_HANDLE hDriverConnection,
      const uint8_t * pDataBuffer,
      uint32_t nDataLength )
{
   W_ERROR nError;
   tParams_PEmulSendAnswer params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pDataBuffer = pDataBuffer;
   params.in.nDataLength = nDataLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PEmulSendAnswer,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulSendAnswer: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulSendAnswer(
           tContext* pContext,
           tParams_PEmulSendAnswer* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pDataBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pDataBuffer, pParams->in.nDataLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PEmulSendAnswer(pContext, pParams->in.hDriverConnection, pParams->in.pDataBuffer, pParams->in.nDataLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PFeliCaDriverExchangeData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PFeliCaDriverExchangeData(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;
   tParams_PFeliCaDriverExchangeData params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PFeliCaDriverExchangeData,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PFeliCaDriverExchangeData: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PFeliCaDriverExchangeData(
           tContext* pContext,
           tParams_PFeliCaDriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PFeliCaDriverExchangeData(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PFeliCaDriverGetCardList()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PFeliCaDriverGetCardList(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;
   tParams_PFeliCaDriverGetCardList params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PFeliCaDriverGetCardList,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PFeliCaDriverGetCardList: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PFeliCaDriverGetCardList(
           tContext* pContext,
           tParams_PFeliCaDriverGetCardList* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PFeliCaDriverGetCardList(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PHandleCheckPropertyDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PHandleCheckPropertyDriver(
      tContext * pContext,
      W_HANDLE hObject,
      uint8_t nPropertyValue )
{
   W_ERROR nError;
   tParams_PHandleCheckPropertyDriver params;
   params.in.hObject = hObject;
   params.in.nPropertyValue = nPropertyValue;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PHandleCheckPropertyDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleCheckPropertyDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleCheckPropertyDriver(
           tContext* pContext,
           tParams_PHandleCheckPropertyDriver* pParams)
{
   pParams->out = PHandleCheckPropertyDriver(pContext, pParams->in.hObject, pParams->in.nPropertyValue);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PHandleCloseDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PHandleCloseDriver(
      tContext * pContext,
      W_HANDLE hObject )
{
   W_ERROR nError;
   tParams_PHandleCloseDriver params;
   params.in.hObject = hObject;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PHandleCloseDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleCloseDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleCloseDriver(
           tContext* pContext,
           tParams_PHandleCloseDriver* pParams)
{
   PHandleCloseDriver(pContext, pParams->in.hObject);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PHandleCloseSafeDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PHandleCloseSafeDriver(
      tContext * pContext,
      W_HANDLE hObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PHandleCloseSafeDriver params;
   params.in.hObject = hObject;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PHandleCloseSafeDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleCloseSafeDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleCloseSafeDriver(
           tContext* pContext,
           tParams_PHandleCloseSafeDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PHandleCloseSafeDriver(pContext, pParams->in.hObject, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PHandleGetCountDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PHandleGetCountDriver(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PHandleGetCountDriver params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PHandleGetCountDriver,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleGetCountDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleGetCountDriver(
           tContext* pContext,
           tParams_PHandleGetCountDriver* pParams)
{
   pParams->out = PHandleGetCountDriver(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PHandleGetPropertiesDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PHandleGetPropertiesDriver(
      tContext * pContext,
      W_HANDLE hObject,
      uint8_t * pPropertyArray,
      uint32_t nPropertyArrayLength )
{
   W_ERROR nError;
   tParams_PHandleGetPropertiesDriver params;
   params.in.hObject = hObject;
   params.in.pPropertyArray = pPropertyArray;
   params.in.nPropertyArrayLength = nPropertyArrayLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PHandleGetPropertiesDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleGetPropertiesDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleGetPropertiesDriver(
           tContext* pContext,
           tParams_PHandleGetPropertiesDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pPropertyArray = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pPropertyArray, pParams->in.nPropertyArrayLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PHandleGetPropertiesDriver(pContext, pParams->in.hObject, pParams->in.pPropertyArray, pParams->in.nPropertyArrayLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PHandleGetPropertyNumberDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PHandleGetPropertyNumberDriver(
      tContext * pContext,
      W_HANDLE hObject,
      uint32_t * pnPropertyNumber )
{
   W_ERROR nError;
   tParams_PHandleGetPropertyNumberDriver params;
   params.in.hObject = hObject;
   params.in.pnPropertyNumber = pnPropertyNumber;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PHandleGetPropertyNumberDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleGetPropertyNumberDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleGetPropertyNumberDriver(
           tContext* pContext,
           tParams_PHandleGetPropertyNumberDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pnPropertyNumber = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnPropertyNumber, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PHandleGetPropertyNumberDriver(pContext, pParams->in.hObject, pParams->in.pnPropertyNumber);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PMultiTimerCancelDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PMultiTimerCancelDriver(
      tContext * pContext,
      uint32_t nTimerIdentifier )
{
   W_ERROR nError;
   tParams_PMultiTimerCancelDriver params;
   params.in.nTimerIdentifier = nTimerIdentifier;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PMultiTimerCancelDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PMultiTimerCancelDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PMultiTimerCancelDriver(
           tContext* pContext,
           tParams_PMultiTimerCancelDriver* pParams)
{
   PMultiTimerCancelDriver(pContext, pParams->in.nTimerIdentifier);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PMultiTimerSetDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PMultiTimerSetDriver(
      tContext * pContext,
      uint32_t nTimerIdentifier,
      uint32_t nAbsoluteTimeout,
      tPBasicGenericCompletionFunction * pCallbackFunction,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PMultiTimerSetDriver params;
   params.in.nTimerIdentifier = nTimerIdentifier;
   params.in.nAbsoluteTimeout = nAbsoluteTimeout;
   params.in.pCallbackFunction = pCallbackFunction;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PMultiTimerSetDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PMultiTimerSetDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PMultiTimerSetDriver(
           tContext* pContext,
           tParams_PMultiTimerSetDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallbackFunction), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PMultiTimerSetDriver(pContext, pParams->in.nTimerIdentifier, pParams->in.nAbsoluteTimeout, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNALServiceDriverGetCurrentTime()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PNALServiceDriverGetCurrentTime(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PNALServiceDriverGetCurrentTime params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNALServiceDriverGetCurrentTime,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNALServiceDriverGetCurrentTime: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNALServiceDriverGetCurrentTime(
           tContext* pContext,
           tParams_PNALServiceDriverGetCurrentTime* pParams)
{
   pParams->out = PNALServiceDriverGetCurrentTime(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNALServiceDriverGetProtocolStatistics()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNALServiceDriverGetProtocolStatistics(
      tContext * pContext,
      tNALProtocolStatistics * pStatistics,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PNALServiceDriverGetProtocolStatistics params;
   params.in.pStatistics = pStatistics;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNALServiceDriverGetProtocolStatistics,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNALServiceDriverGetProtocolStatistics: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNALServiceDriverGetProtocolStatistics(
           tContext* pContext,
           tParams_PNALServiceDriverGetProtocolStatistics* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pStatistics = (tNALProtocolStatistics *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pStatistics, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tNALProtocolStatistics *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNALServiceDriverGetProtocolStatistics(pContext, pParams->in.pStatistics, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNALServiceDriverResetProtocolStatistics()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNALServiceDriverResetProtocolStatistics(
      tContext * pContext )
{
   W_ERROR nError;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNALServiceDriverResetProtocolStatistics,
       null,
       0, 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNALServiceDriverResetProtocolStatistics: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNALServiceDriverResetProtocolStatistics(
           tContext* pContext,
           void* pParams)
{
   PNALServiceDriverResetProtocolStatistics(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFRegisterNPPMessageHandlerDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNDEFRegisterNPPMessageHandlerDriver(
      tContext * pContext,
      tPBasicGenericDataCallbackFunction * pHandler,
      void * pHandlerParameter,
      uint8_t nPriority,
      W_HANDLE * phRegistry )
{
   W_ERROR nError;
   tParams_PNDEFRegisterNPPMessageHandlerDriver params;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.nPriority = nPriority;
   params.in.phRegistry = phRegistry;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFRegisterNPPMessageHandlerDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRegisterNPPMessageHandlerDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRegisterNPPMessageHandlerDriver(
           tContext* pContext,
           tParams_PNDEFRegisterNPPMessageHandlerDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNDEFRegisterNPPMessageHandlerDriver(pContext, null, pDriverCC, pParams->in.nPriority, pParams->in.phRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFRegisterSNEPMessageHandlerDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNDEFRegisterSNEPMessageHandlerDriver(
      tContext * pContext,
      tPBasicGenericDataCallbackFunction * pHandler,
      void * pHandlerParameter,
      uint8_t nPriority,
      W_HANDLE * phRegistry )
{
   W_ERROR nError;
   tParams_PNDEFRegisterSNEPMessageHandlerDriver params;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.nPriority = nPriority;
   params.in.phRegistry = phRegistry;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFRegisterSNEPMessageHandlerDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRegisterSNEPMessageHandlerDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRegisterSNEPMessageHandlerDriver(
           tContext* pContext,
           tParams_PNDEFRegisterSNEPMessageHandlerDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNDEFRegisterSNEPMessageHandlerDriver(pContext, null, pDriverCC, pParams->in.nPriority, pParams->in.phRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFRetrieveNPPMessageDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNDEFRetrieveNPPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength )
{
   W_ERROR nError;
   tParams_PNDEFRetrieveNPPMessageDriver params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFRetrieveNPPMessageDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRetrieveNPPMessageDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRetrieveNPPMessageDriver(
           tContext* pContext,
           tParams_PNDEFRetrieveNPPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNDEFRetrieveNPPMessageDriver(pContext, pParams->in.pBuffer, pParams->in.nBufferLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFRetrieveSNEPMessageDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNDEFRetrieveSNEPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength )
{
   W_ERROR nError;
   tParams_PNDEFRetrieveSNEPMessageDriver params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFRetrieveSNEPMessageDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRetrieveSNEPMessageDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRetrieveSNEPMessageDriver(
           tContext* pContext,
           tParams_PNDEFRetrieveSNEPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNDEFRetrieveSNEPMessageDriver(pContext, pParams->in.pBuffer, pParams->in.nBufferLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFSendNPPMessageDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE PNDEFSendNPPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PNDEFSendNPPMessageDriver params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFSendNPPMessageDriver,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFSendNPPMessageDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSendNPPMessageDriver(
           tContext* pContext,
           tParams_PNDEFSendNPPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNDEFSendNPPMessageDriver(pContext, pParams->in.pBuffer, pParams->in.nBufferLength, null, pDriverCC);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFSendSNEPMessageDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE PNDEFSendSNEPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PNDEFSendSNEPMessageDriver params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFSendSNEPMessageDriver,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFSendSNEPMessageDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSendSNEPMessageDriver(
           tContext* pContext,
           tParams_PNDEFSendSNEPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNDEFSendSNEPMessageDriver(pContext, pParams->in.pBuffer, pParams->in.nBufferLength, null, pDriverCC);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFSetWorkPerformedNPPDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNDEFSetWorkPerformedNPPDriver(
      tContext * pContext,
      bool_t bGiveToNextListener )
{
   W_ERROR nError;
   tParams_PNDEFSetWorkPerformedNPPDriver params;
   params.in.bGiveToNextListener = bGiveToNextListener;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFSetWorkPerformedNPPDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFSetWorkPerformedNPPDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSetWorkPerformedNPPDriver(
           tContext* pContext,
           tParams_PNDEFSetWorkPerformedNPPDriver* pParams)
{
   PNDEFSetWorkPerformedNPPDriver(pContext, pParams->in.bGiveToNextListener);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNDEFSetWorkPerformedSNEPDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNDEFSetWorkPerformedSNEPDriver(
      tContext * pContext,
      bool_t bGiveToNextListener )
{
   W_ERROR nError;
   tParams_PNDEFSetWorkPerformedSNEPDriver params;
   params.in.bGiveToNextListener = bGiveToNextListener;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNDEFSetWorkPerformedSNEPDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFSetWorkPerformedSNEPDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSetWorkPerformedSNEPDriver(
           tContext* pContext,
           tParams_PNDEFSetWorkPerformedSNEPDriver* pParams)
{
   PNDEFSetWorkPerformedSNEPDriver(pContext, pParams->in.bGiveToNextListener);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerDriverGetRFActivity()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PNFCControllerDriverGetRFActivity(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PNFCControllerDriverGetRFActivity params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerDriverGetRFActivity,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerDriverGetRFActivity: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerDriverGetRFActivity(
           tContext* pContext,
           tParams_PNFCControllerDriverGetRFActivity* pParams)
{
   pParams->out = PNFCControllerDriverGetRFActivity(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerDriverGetRFLock()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PNFCControllerDriverGetRFLock(
      tContext * pContext,
      uint32_t nLockSet )
{
   W_ERROR nError;
   tParams_PNFCControllerDriverGetRFLock params;
   params.in.nLockSet = nLockSet;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerDriverGetRFLock,
       &params, sizeof(params), sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerDriverGetRFLock: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerDriverGetRFLock(
           tContext* pContext,
           tParams_PNFCControllerDriverGetRFLock* pParams)
{
   pParams->out = PNFCControllerDriverGetRFLock(pContext, pParams->in.nLockSet);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerDriverReadInfo()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerDriverReadInfo(
      tContext * pContext,
      void * pBuffer,
      uint32_t nBufferSize )
{
   W_ERROR nError;
   tParams_PNFCControllerDriverReadInfo params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferSize = nBufferSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerDriverReadInfo,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerDriverReadInfo: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerDriverReadInfo(
           tContext* pContext,
           tParams_PNFCControllerDriverReadInfo* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (void *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferSize, ( P_SYNC_BUFFER_FLAG_O ))) == (void *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNFCControllerDriverReadInfo(pContext, pParams->in.pBuffer, pParams->in.nBufferSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerFirmwareUpdateDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerFirmwareUpdateDriver(
      tContext * pContext,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pUpdateBuffer,
      uint32_t nUpdateBufferLength,
      uint32_t nMode )
{
   W_ERROR nError;
   tParams_PNFCControllerFirmwareUpdateDriver params;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pUpdateBuffer = pUpdateBuffer;
   params.in.nUpdateBufferLength = nUpdateBufferLength;
   params.in.nMode = nMode;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerFirmwareUpdateDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerFirmwareUpdateDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerFirmwareUpdateDriver(
           tContext* pContext,
           tParams_PNFCControllerFirmwareUpdateDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pUpdateBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pUpdateBuffer, pParams->in.nUpdateBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNFCControllerFirmwareUpdateDriver(pContext, null, pDriverCC, pParams->in.pUpdateBuffer, pParams->in.nUpdateBufferLength, pParams->in.nMode);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerFirmwareUpdateState()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PNFCControllerFirmwareUpdateState(
      tContext* pContext )
{
   W_ERROR nError;
   tParams_PNFCControllerFirmwareUpdateState params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerFirmwareUpdateState,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerFirmwareUpdateState: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerFirmwareUpdateState(
           tContext* pContext,
           tParams_PNFCControllerFirmwareUpdateState* pParams)
{
   pParams->out = PNFCControllerFirmwareUpdateState(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerGetMode()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PNFCControllerGetMode(
      tContext* pContext )
{
   W_ERROR nError;
   tParams_PNFCControllerGetMode params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerGetMode,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerGetMode: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerGetMode(
           tContext* pContext,
           tParams_PNFCControllerGetMode* pParams)
{
   pParams->out = PNFCControllerGetMode(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerGetRawMessageData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerGetRawMessageData(
      tContext* pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      uint32_t * pnActualLength )
{
   W_ERROR nError;
   tParams_PNFCControllerGetRawMessageData params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pnActualLength = pnActualLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerGetRawMessageData,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerGetRawMessageData: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerGetRawMessageData(
           tContext* pContext,
           tParams_PNFCControllerGetRawMessageData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pnActualLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnActualLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNFCControllerGetRawMessageData(pContext, pParams->in.pBuffer, pParams->in.nBufferLength, pParams->in.pnActualLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerIsActive()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

bool_t PNFCControllerIsActive(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PNFCControllerIsActive params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerIsActive,
       &params, 0, sizeof(bool_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerIsActive: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (bool_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerIsActive(
           tContext* pContext,
           tParams_PNFCControllerIsActive* pParams)
{
   pParams->out = PNFCControllerIsActive(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerMonitorException()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerMonitorException(
      tContext* pContext,
      tPBasicGenericEventHandler * pHandler,
      void * pHandlerParameter,
      W_HANDLE * phEventRegistry )
{
   W_ERROR nError;
   tParams_PNFCControllerMonitorException params;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerMonitorException,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerMonitorException: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerMonitorException(
           tContext* pContext,
           tParams_PNFCControllerMonitorException* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNFCControllerMonitorException(pContext, null, pDriverCC, pParams->in.phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerMonitorFieldEvents()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerMonitorFieldEvents(
      tContext* pContext,
      tPBasicGenericEventHandler * pHandler,
      void * pHandlerParameter,
      W_HANDLE * phEventRegistry )
{
   W_ERROR nError;
   tParams_PNFCControllerMonitorFieldEvents params;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerMonitorFieldEvents,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerMonitorFieldEvents: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerMonitorFieldEvents(
           tContext* pContext,
           tParams_PNFCControllerMonitorFieldEvents* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PNFCControllerMonitorFieldEvents(pContext, null, pDriverCC, pParams->in.phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerProductionTestDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerProductionTestDriver(
      tContext * pContext,
      const uint8_t * pParameterBuffer,
      uint32_t nParameterBufferLength,
      uint8_t * pResultBuffer,
      uint32_t nResultBufferLength,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PNFCControllerProductionTestDriver params;
   params.in.pParameterBuffer = pParameterBuffer;
   params.in.nParameterBufferLength = nParameterBufferLength;
   params.in.pResultBuffer = pResultBuffer;
   params.in.nResultBufferLength = nResultBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerProductionTestDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerProductionTestDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerProductionTestDriver(
           tContext* pContext,
           tParams_PNFCControllerProductionTestDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pParameterBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pParameterBuffer, pParams->in.nParameterBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pResultBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pResultBuffer, pParams->in.nResultBufferLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNFCControllerProductionTestDriver(pContext, pParams->in.pParameterBuffer, pParams->in.nParameterBufferLength, pParams->in.pResultBuffer, pParams->in.nResultBufferLength, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerRegisterRawListener()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerRegisterRawListener(
      tContext* pContext,
      tPBasicGenericDataCallbackFunction * pReceiveMessageEventHandler,
      void * pHandlerParameter )
{
   W_ERROR nError;
   tParams_PNFCControllerRegisterRawListener params;
   params.in.pReceiveMessageEventHandler = pReceiveMessageEventHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerRegisterRawListener,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerRegisterRawListener: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerRegisterRawListener(
           tContext* pContext,
           tParams_PNFCControllerRegisterRawListener* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pReceiveMessageEventHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   pParams->out = PNFCControllerRegisterRawListener(pContext, null, pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerResetDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerResetDriver(
      tContext * pContext,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint32_t nMode )
{
   W_ERROR nError;
   tParams_PNFCControllerResetDriver params;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nMode = nMode;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerResetDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerResetDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerResetDriver(
           tContext* pContext,
           tParams_PNFCControllerResetDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PNFCControllerResetDriver(pContext, null, pDriverCC, pParams->in.nMode);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerSelfTestDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerSelfTestDriver(
      tContext * pContext,
      tPNFCControllerSelfTestCompleted * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PNFCControllerSelfTestDriver params;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerSelfTestDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerSelfTestDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSelfTestDriver(
           tContext* pContext,
           tParams_PNFCControllerSelfTestDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PNFCControllerSelfTestDriver(pContext, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerSetRFLockDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerSetRFLockDriver(
      tContext * pContext,
      uint32_t nLockSet,
      bool_t bReaderLock,
      bool_t bCardLock,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PNFCControllerSetRFLockDriver params;
   params.in.nLockSet = nLockSet;
   params.in.bReaderLock = bReaderLock;
   params.in.bCardLock = bCardLock;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerSetRFLockDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerSetRFLockDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSetRFLockDriver(
           tContext* pContext,
           tParams_PNFCControllerSetRFLockDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PNFCControllerSetRFLockDriver(pContext, pParams->in.nLockSet, pParams->in.bReaderLock, pParams->in.bCardLock, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerSwitchStandbyMode()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerSwitchStandbyMode(
      tContext* pContext,
      bool_t bStandbyOn )
{
   W_ERROR nError;
   tParams_PNFCControllerSwitchStandbyMode params;
   params.in.bStandbyOn = bStandbyOn;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerSwitchStandbyMode,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerSwitchStandbyMode: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSwitchStandbyMode(
           tContext* pContext,
           tParams_PNFCControllerSwitchStandbyMode* pParams)
{
   pParams->out = PNFCControllerSwitchStandbyMode(pContext, pParams->in.bStandbyOn);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerSwitchToRawModeDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerSwitchToRawModeDriver(
      tContext * pContext,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PNFCControllerSwitchToRawModeDriver params;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerSwitchToRawModeDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerSwitchToRawModeDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSwitchToRawModeDriver(
           tContext* pContext,
           tParams_PNFCControllerSwitchToRawModeDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PNFCControllerSwitchToRawModeDriver(pContext, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PNFCControllerWriteRawMessageDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerWriteRawMessageDriver(
      tContext * pContext,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pBuffer,
      uint32_t nLength )
{
   W_ERROR nError;
   tParams_PNFCControllerWriteRawMessageDriver params;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pBuffer = pBuffer;
   params.in.nLength = nLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PNFCControllerWriteRawMessageDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerWriteRawMessageDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerWriteRawMessageDriver(
           tContext* pContext,
           tParams_PNFCControllerWriteRawMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNFCControllerWriteRawMessageDriver(pContext, null, pDriverCC, pParams->in.pBuffer, pParams->in.nLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PConnectDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PConnectDriver(
      tContext * pContext,
      W_HANDLE hSocket,
      W_HANDLE hLink,
      tPBasicGenericCallbackFunction * pEstablishmentCallback,
      void * pEstablishmentCallbackParameter )
{
   W_ERROR nError;
   tParams_PP2PConnectDriver params;
   params.in.hSocket = hSocket;
   params.in.hLink = hLink;
   params.in.pEstablishmentCallback = pEstablishmentCallback;
   params.in.pEstablishmentCallbackParameter = pEstablishmentCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PConnectDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PConnectDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PConnectDriver(
           tContext* pContext,
           tParams_PP2PConnectDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pEstablishmentCallback), pParams->in.pEstablishmentCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PP2PConnectDriver(pContext, pParams->in.hSocket, pParams->in.hLink, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PCreateSocketDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PP2PCreateSocketDriver(
      tContext * pContext,
      uint8_t nType,
      const char16_t * pServiceURI,
      uint32_t nSize,
      uint8_t nSAP,
      W_HANDLE * phSocket )
{
   W_ERROR nError;
   tParams_PP2PCreateSocketDriver params;
   params.in.nType = nType;
   params.in.pServiceURI = pServiceURI;
   params.in.nSize = nSize;
   params.in.nSAP = nSAP;
   params.in.phSocket = phSocket;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PCreateSocketDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PCreateSocketDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PCreateSocketDriver(
           tContext* pContext,
           tParams_PP2PCreateSocketDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pServiceURI = (const char16_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pServiceURI, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const char16_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phSocket = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phSocket, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PP2PCreateSocketDriver(pContext, pParams->in.nType, pParams->in.pServiceURI, pParams->in.nSize, pParams->in.nSAP, pParams->in.phSocket);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver1()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE PP2PEstablishLinkDriver1(
      tContext* pContext,
      tPBasicGenericHandleCallbackFunction * pEstablishmentCallback,
      void * pEstablishmentCallbackParameter )
{
   W_ERROR nError;
   tParams_PP2PEstablishLinkDriver1 params;
   params.in.pEstablishmentCallback = pEstablishmentCallback;
   params.in.pEstablishmentCallbackParameter = pEstablishmentCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PEstablishLinkDriver1,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PEstablishLinkDriver1: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver1(
           tContext* pContext,
           tParams_PP2PEstablishLinkDriver1* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternal(pContext, (tDFCCallback*)(pParams->in.pEstablishmentCallback), pParams->in.pEstablishmentCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   pParams->out = PP2PEstablishLinkDriver1(pContext, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver1Wrapper()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE PP2PEstablishLinkDriver1Wrapper(
      tContext * pContext,
      tPBasicGenericHandleCallbackFunction * pEstablishmentCallback,
      void * pEstablishmentCallbackParameter )
{
   W_ERROR nError;
   tParams_PP2PEstablishLinkDriver1Wrapper params;
   params.in.pEstablishmentCallback = pEstablishmentCallback;
   params.in.pEstablishmentCallbackParameter = pEstablishmentCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PEstablishLinkDriver1Wrapper,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PEstablishLinkDriver1Wrapper: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver1Wrapper(
           tContext* pContext,
           tParams_PP2PEstablishLinkDriver1Wrapper* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pEstablishmentCallback), pParams->in.pEstablishmentCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   pParams->out = PP2PEstablishLinkDriver1Wrapper(pContext, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver2()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PEstablishLinkDriver2(
      tContext* pContext,
      W_HANDLE hLink,
      tPBasicGenericCallbackFunction * pReleaseCallback,
      void * pReleaseCallbackParameter,
      W_HANDLE * phOperation )
{
   W_ERROR nError;
   tParams_PP2PEstablishLinkDriver2 params;
   params.in.hLink = hLink;
   params.in.pReleaseCallback = pReleaseCallback;
   params.in.pReleaseCallbackParameter = pReleaseCallbackParameter;
   params.in.phOperation = phOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PEstablishLinkDriver2,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PEstablishLinkDriver2: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver2(
           tContext* pContext,
           tParams_PP2PEstablishLinkDriver2* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternal(pContext, (tDFCCallback*)(pParams->in.pReleaseCallback), pParams->in.pReleaseCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phOperation, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PEstablishLinkDriver2(pContext, pParams->in.hLink, null, pDriverCC, pParams->in.phOperation);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PEstablishLinkDriver2Wrapper()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PEstablishLinkDriver2Wrapper(
      tContext * pContext,
      W_HANDLE hLink,
      tPBasicGenericCallbackFunction * pReleaseCallback,
      void * pReleaseCallbackParameter,
      W_HANDLE * phOperation )
{
   W_ERROR nError;
   tParams_PP2PEstablishLinkDriver2Wrapper params;
   params.in.hLink = hLink;
   params.in.pReleaseCallback = pReleaseCallback;
   params.in.pReleaseCallbackParameter = pReleaseCallbackParameter;
   params.in.phOperation = phOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PEstablishLinkDriver2Wrapper,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PEstablishLinkDriver2Wrapper: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver2Wrapper(
           tContext* pContext,
           tParams_PP2PEstablishLinkDriver2Wrapper* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pReleaseCallback), pParams->in.pReleaseCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phOperation, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PEstablishLinkDriver2Wrapper(pContext, pParams->in.hLink, null, pDriverCC, pParams->in.phOperation);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PGetConfigurationDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PP2PGetConfigurationDriver(
      tContext * pContext,
      tWP2PConfiguration * pConfiguration,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PP2PGetConfigurationDriver params;
   params.in.pConfiguration = pConfiguration;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PGetConfigurationDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PGetConfigurationDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PGetConfigurationDriver(
           tContext* pContext,
           tParams_PP2PGetConfigurationDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pConfiguration = (tWP2PConfiguration *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pConfiguration, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tWP2PConfiguration *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PP2PGetConfigurationDriver(pContext, pParams->in.pConfiguration, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PGetLinkPropertiesDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PP2PGetLinkPropertiesDriver(
      tContext * pContext,
      W_HANDLE hLink,
      tWP2PLinkProperties * pProperties,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PP2PGetLinkPropertiesDriver params;
   params.in.hLink = hLink;
   params.in.pProperties = pProperties;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PGetLinkPropertiesDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PGetLinkPropertiesDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PGetLinkPropertiesDriver(
           tContext* pContext,
           tParams_PP2PGetLinkPropertiesDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pProperties = (tWP2PLinkProperties *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pProperties, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tWP2PLinkProperties *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PP2PGetLinkPropertiesDriver(pContext, pParams->in.hLink, pParams->in.pProperties, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PGetSocketParameterDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PP2PGetSocketParameterDriver(
      tContext * pContext,
      W_HANDLE hSocket,
      uint32_t nParameter,
      uint32_t * pnValue )
{
   W_ERROR nError;
   tParams_PP2PGetSocketParameterDriver params;
   params.in.hSocket = hSocket;
   params.in.nParameter = nParameter;
   params.in.pnValue = pnValue;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PGetSocketParameterDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PGetSocketParameterDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PGetSocketParameterDriver(
           tContext* pContext,
           tParams_PP2PGetSocketParameterDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pnValue = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnValue, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PP2PGetSocketParameterDriver(pContext, pParams->in.hSocket, pParams->in.nParameter, pParams->in.pnValue);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PReadDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PReadDriver(
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint8_t * pReceptionBuffer,
      uint32_t nReceptionBufferLength,
      W_HANDLE * phOperation )
{
   W_ERROR nError;
   tParams_PP2PReadDriver params;
   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReceptionBuffer = pReceptionBuffer;
   params.in.nReceptionBufferLength = nReceptionBufferLength;
   params.in.phOperation = phOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PReadDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PReadDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PReadDriver(
           tContext* pContext,
           tParams_PP2PReadDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReceptionBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReceptionBuffer, pParams->in.nReceptionBufferLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PReadDriver(pContext, pParams->in.hConnection, null, pDriverCC, pParams->in.pReceptionBuffer, pParams->in.nReceptionBufferLength, pParams->in.phOperation);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PRecvFromDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PRecvFromDriver(
      tContext * pContext,
      W_HANDLE hSocket,
      tPP2PRecvFromCompleted * pCallback,
      void * pCallbackParameter,
      uint8_t * pReceptionBuffer,
      uint32_t nReceptionBufferLength,
      W_HANDLE * phOperation )
{
   W_ERROR nError;
   tParams_PP2PRecvFromDriver params;
   params.in.hSocket = hSocket;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReceptionBuffer = pReceptionBuffer;
   params.in.nReceptionBufferLength = nReceptionBufferLength;
   params.in.phOperation = phOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PRecvFromDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PRecvFromDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PRecvFromDriver(
           tContext* pContext,
           tParams_PP2PRecvFromDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReceptionBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReceptionBuffer, pParams->in.nReceptionBufferLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PRecvFromDriver(pContext, pParams->in.hSocket, null, pDriverCC, pParams->in.pReceptionBuffer, pParams->in.nReceptionBufferLength, pParams->in.phOperation);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PSendToDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PSendToDriver(
      tContext * pContext,
      W_HANDLE hSocket,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint8_t nSAP,
      const uint8_t * pSendBuffer,
      uint32_t nSendBufferLength,
      W_HANDLE * phOperation )
{
   W_ERROR nError;
   tParams_PP2PSendToDriver params;
   params.in.hSocket = hSocket;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nSAP = nSAP;
   params.in.pSendBuffer = pSendBuffer;
   params.in.nSendBufferLength = nSendBufferLength;
   params.in.phOperation = phOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PSendToDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PSendToDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PSendToDriver(
           tContext* pContext,
           tParams_PP2PSendToDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pSendBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pSendBuffer, pParams->in.nSendBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PSendToDriver(pContext, pParams->in.hSocket, null, pDriverCC, pParams->in.nSAP, pParams->in.pSendBuffer, pParams->in.nSendBufferLength, pParams->in.phOperation);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PSetConfigurationDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PP2PSetConfigurationDriver(
      tContext * pContext,
      const tWP2PConfiguration * pConfiguration,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PP2PSetConfigurationDriver params;
   params.in.pConfiguration = pConfiguration;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PSetConfigurationDriver,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PSetConfigurationDriver: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PSetConfigurationDriver(
           tContext* pContext,
           tParams_PP2PSetConfigurationDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pConfiguration = (const tWP2PConfiguration *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pConfiguration, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tWP2PConfiguration *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PP2PSetConfigurationDriver(pContext, pParams->in.pConfiguration, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PSetSocketParameter()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PP2PSetSocketParameter(
      tContext* pContext,
      W_HANDLE hSocket,
      uint32_t nParameter,
      uint32_t nValue )
{
   W_ERROR nError;
   tParams_PP2PSetSocketParameter params;
   params.in.hSocket = hSocket;
   params.in.nParameter = nParameter;
   params.in.nValue = nValue;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PSetSocketParameter,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PSetSocketParameter: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PSetSocketParameter(
           tContext* pContext,
           tParams_PP2PSetSocketParameter* pParams)
{
   pParams->out = PP2PSetSocketParameter(pContext, pParams->in.hSocket, pParams->in.nParameter, pParams->in.nValue);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PShutdownDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PShutdownDriver(
      tContext * pContext,
      W_HANDLE hSocket,
      tPBasicGenericCallbackFunction * pReleaseCallback,
      void * pReleaseCallbackParameter )
{
   W_ERROR nError;
   tParams_PP2PShutdownDriver params;
   params.in.hSocket = hSocket;
   params.in.pReleaseCallback = pReleaseCallback;
   params.in.pReleaseCallbackParameter = pReleaseCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PShutdownDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PShutdownDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PShutdownDriver(
           tContext* pContext,
           tParams_PP2PShutdownDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pReleaseCallback), pParams->in.pReleaseCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PP2PShutdownDriver(pContext, pParams->in.hSocket, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PURILookupDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PURILookupDriver(
      tContext * pContext,
      W_HANDLE hLink,
      tPP2PURILookupCompleted * pCallback,
      void * pCallbackParameter,
      const char16_t * pServiceURI,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PP2PURILookupDriver params;
   params.in.hLink = hLink;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pServiceURI = pServiceURI;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PURILookupDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PURILookupDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PURILookupDriver(
           tContext* pContext,
           tParams_PP2PURILookupDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pServiceURI = (const char16_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pServiceURI, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const char16_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PURILookupDriver(pContext, pParams->in.hLink, null, pDriverCC, pParams->in.pServiceURI, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PP2PWriteDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PP2PWriteDriver(
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pSendBuffer,
      uint32_t nSendBufferLength,
      W_HANDLE * phOperation )
{
   W_ERROR nError;
   tParams_PP2PWriteDriver params;
   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pSendBuffer = pSendBuffer;
   params.in.nSendBufferLength = nSendBufferLength;
   params.in.phOperation = phOperation;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PP2PWriteDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PWriteDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PWriteDriver(
           tContext* pContext,
           tParams_PP2PWriteDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pSendBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pSendBuffer, pParams->in.nSendBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PWriteDriver(pContext, pParams->in.hConnection, null, pDriverCC, pParams->in.pSendBuffer, pParams->in.nSendBufferLength, pParams->in.phOperation);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderDriverGetLastReferenceTime()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PReaderDriverGetLastReferenceTime(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PReaderDriverGetLastReferenceTime params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderDriverGetLastReferenceTime,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverGetLastReferenceTime: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverGetLastReferenceTime(
           tContext* pContext,
           tParams_PReaderDriverGetLastReferenceTime* pParams)
{
   pParams->out = PReaderDriverGetLastReferenceTime(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderDriverGetNbCardDetected()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint8_t PReaderDriverGetNbCardDetected(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PReaderDriverGetNbCardDetected params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderDriverGetNbCardDetected,
       &params, 0, sizeof(uint8_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverGetNbCardDetected: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint8_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverGetNbCardDetected(
           tContext* pContext,
           tParams_PReaderDriverGetNbCardDetected* pParams)
{
   pParams->out = PReaderDriverGetNbCardDetected(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderDriverRedetectCard()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PReaderDriverRedetectCard(
      tContext * pContext,
      W_HANDLE hConnection )
{
   W_ERROR nError;
   tParams_PReaderDriverRedetectCard params;
   params.in.hConnection = hConnection;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderDriverRedetectCard,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverRedetectCard: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverRedetectCard(
           tContext* pContext,
           tParams_PReaderDriverRedetectCard* pParams)
{
   pParams->out = PReaderDriverRedetectCard(pContext, pParams->in.hConnection);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderDriverRegister()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PReaderDriverRegister(
      tContext * pContext,
      tPReaderDriverRegisterCompleted * pCallback,
      void * pCallbackParameter,
      uint8_t nPriority,
      uint32_t nRequestedProtocolsBF,
      uint32_t nDetectionConfigurationLength,
      uint8_t * pBuffer,
      uint32_t nBufferMaxLength,
      W_HANDLE * phListenerHandle )
{
   W_ERROR nError;
   tParams_PReaderDriverRegister params;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nPriority = nPriority;
   params.in.nRequestedProtocolsBF = nRequestedProtocolsBF;
   params.in.nDetectionConfigurationLength = nDetectionConfigurationLength;
   params.in.pBuffer = pBuffer;
   params.in.nBufferMaxLength = nBufferMaxLength;
   params.in.phListenerHandle = phListenerHandle;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderDriverRegister,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverRegister: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverRegister(
           tContext* pContext,
           tParams_PReaderDriverRegister* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferMaxLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A | P_SYNC_BUFFER_FLAG_L ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.phListenerHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phListenerHandle, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PReaderDriverRegister(pContext, null, pDriverCC, pParams->in.nPriority, pParams->in.nRequestedProtocolsBF, pParams->in.nDetectionConfigurationLength, pParams->in.pBuffer, pParams->in.nBufferMaxLength, pParams->in.phListenerHandle);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderDriverSetWorkPerformedAndClose()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PReaderDriverSetWorkPerformedAndClose(
      tContext * pContext,
      W_HANDLE hDriverListener )
{
   W_ERROR nError;
   tParams_PReaderDriverSetWorkPerformedAndClose params;
   params.in.hDriverListener = hDriverListener;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderDriverSetWorkPerformedAndClose,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverSetWorkPerformedAndClose: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverSetWorkPerformedAndClose(
           tContext* pContext,
           tParams_PReaderDriverSetWorkPerformedAndClose* pParams)
{
   pParams->out = PReaderDriverSetWorkPerformedAndClose(pContext, pParams->in.hDriverListener);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderDriverWorkPerformed()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PReaderDriverWorkPerformed(
      tContext * pContext,
      W_HANDLE hConnection,
      bool_t bGiveToNextListener,
      bool_t bCardApplicationMatch )
{
   W_ERROR nError;
   tParams_PReaderDriverWorkPerformed params;
   params.in.hConnection = hConnection;
   params.in.bGiveToNextListener = bGiveToNextListener;
   params.in.bCardApplicationMatch = bCardApplicationMatch;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderDriverWorkPerformed,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverWorkPerformed: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverWorkPerformed(
           tContext* pContext,
           tParams_PReaderDriverWorkPerformed* pParams)
{
   pParams->out = PReaderDriverWorkPerformed(pContext, pParams->in.hConnection, pParams->in.bGiveToNextListener, pParams->in.bCardApplicationMatch);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderErrorEventRegister()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PReaderErrorEventRegister(
      tContext* pContext,
      tPBasicGenericEventHandler * pHandler,
      void * pHandlerParameter,
      uint8_t nEventType,
      bool_t bCardDetectionRequested,
      W_HANDLE * phRegistryHandle )
{
   W_ERROR nError;
   tParams_PReaderErrorEventRegister params;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.nEventType = nEventType;
   params.in.bCardDetectionRequested = bCardDetectionRequested;
   params.in.phRegistryHandle = phRegistryHandle;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderErrorEventRegister,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderErrorEventRegister: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderErrorEventRegister(
           tContext* pContext,
           tParams_PReaderErrorEventRegister* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phRegistryHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phRegistryHandle, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PReaderErrorEventRegister(pContext, null, pDriverCC, pParams->in.nEventType, pParams->in.bCardDetectionRequested, pParams->in.phRegistryHandle);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderGetPulsePeriod()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PReaderGetPulsePeriod(
      tContext* pContext,
      uint32_t * pnTimeout )
{
   W_ERROR nError;
   tParams_PReaderGetPulsePeriod params;
   params.in.pnTimeout = pnTimeout;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderGetPulsePeriod,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderGetPulsePeriod: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderGetPulsePeriod(
           tContext* pContext,
           tParams_PReaderGetPulsePeriod* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pnTimeout = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnTimeout, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PReaderGetPulsePeriod(pContext, pParams->in.pnTimeout);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PReaderSetPulsePeriodDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PReaderSetPulsePeriodDriver(
      tContext * pContext,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint32_t nPulsePeriod )
{
   W_ERROR nError;
   tParams_PReaderSetPulsePeriodDriver params;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nPulsePeriod = nPulsePeriod;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PReaderSetPulsePeriodDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderSetPulsePeriodDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderSetPulsePeriodDriver(
           tContext* pContext,
           tParams_PReaderSetPulsePeriodDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PReaderSetPulsePeriodDriver(pContext, null, pDriverCC, pParams->in.nPulsePeriod);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PRoutingTableApplyDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PRoutingTableApplyDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PRoutingTableApplyDriver params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PRoutingTableApplyDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PRoutingTableApplyDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableApplyDriver(
           tContext* pContext,
           tParams_PRoutingTableApplyDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PRoutingTableApplyDriver(pContext, pParams->in.pBuffer, pParams->in.nBufferLength, null, pDriverCC);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PRoutingTableGetConfigDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PRoutingTableGetConfigDriver(
      tContext * pContext )
{
   W_ERROR nError;
   tParams_PRoutingTableGetConfigDriver params;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PRoutingTableGetConfigDriver,
       &params, 0, sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PRoutingTableGetConfigDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableGetConfigDriver(
           tContext* pContext,
           tParams_PRoutingTableGetConfigDriver* pParams)
{
   pParams->out = PRoutingTableGetConfigDriver(pContext);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PRoutingTableReadDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PRoutingTableReadDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PRoutingTableReadDriver params;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PRoutingTableReadDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PRoutingTableReadDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableReadDriver(
           tContext* pContext,
           tParams_PRoutingTableReadDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PRoutingTableReadDriver(pContext, pParams->in.pBuffer, pParams->in.nBufferLength, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PRoutingTableSetConfigDriver()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PRoutingTableSetConfigDriver(
      tContext * pContext,
      uint32_t nConfig,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PRoutingTableSetConfigDriver params;
   params.in.nConfig = nConfig;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PRoutingTableSetConfigDriver,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PRoutingTableSetConfigDriver: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableSetConfigDriver(
           tContext* pContext,
           tParams_PRoutingTableSetConfigDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PRoutingTableSetConfigDriver(pContext, pParams->in.nConfig, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSecurityManagerDriverAuthenticate()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSecurityManagerDriverAuthenticate(
      tContext * pContext,
      const uint8_t * pApplicationDataBuffer,
      uint32_t nApplicationDataBufferLength )
{
   W_ERROR nError;
   tParams_PSecurityManagerDriverAuthenticate params;
   params.in.pApplicationDataBuffer = pApplicationDataBuffer;
   params.in.nApplicationDataBufferLength = nApplicationDataBufferLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSecurityManagerDriverAuthenticate,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSecurityManagerDriverAuthenticate: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSecurityManagerDriverAuthenticate(
           tContext* pContext,
           tParams_PSecurityManagerDriverAuthenticate* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pApplicationDataBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pApplicationDataBuffer, pParams->in.nApplicationDataBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSecurityManagerDriverAuthenticate(pContext, pParams->in.pApplicationDataBuffer, pParams->in.nApplicationDataBufferLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSeDriver7816SmCloseChannel()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSeDriver7816SmCloseChannel(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      uint32_t nChannelReference,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PSeDriver7816SmCloseChannel params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.nChannelReference = nChannelReference;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSeDriver7816SmCloseChannel,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmCloseChannel: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmCloseChannel(
           tContext* pContext,
           tParams_PSeDriver7816SmCloseChannel* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   pParams->out = PSeDriver7816SmCloseChannel(pContext, pParams->in.hDriverConnection, pParams->in.nChannelReference, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSeDriver7816SmExchangeApdu()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSeDriver7816SmExchangeApdu(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      uint32_t nChannelReference,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pSendApduBuffer,
      uint32_t nSendApduBufferLength,
      uint8_t * pReceivedApduBuffer,
      uint32_t nReceivedApduBufferMaxLength )
{
   W_ERROR nError;
   tParams_PSeDriver7816SmExchangeApdu params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.nChannelReference = nChannelReference;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pSendApduBuffer = pSendApduBuffer;
   params.in.nSendApduBufferLength = nSendApduBufferLength;
   params.in.pReceivedApduBuffer = pReceivedApduBuffer;
   params.in.nReceivedApduBufferMaxLength = nReceivedApduBufferMaxLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSeDriver7816SmExchangeApdu,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmExchangeApdu: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmExchangeApdu(
           tContext* pContext,
           tParams_PSeDriver7816SmExchangeApdu* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pSendApduBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pSendApduBuffer, pParams->in.nSendApduBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pReceivedApduBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReceivedApduBuffer, pParams->in.nReceivedApduBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSeDriver7816SmExchangeApdu(pContext, pParams->in.hDriverConnection, pParams->in.nChannelReference, null, pDriverCC, pParams->in.pSendApduBuffer, pParams->in.nSendApduBufferLength, pParams->in.pReceivedApduBuffer, pParams->in.nReceivedApduBufferMaxLength);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSeDriver7816SmGetData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSeDriver7816SmGetData(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      uint32_t nChannelReference,
      uint32_t nType,
      uint8_t * pBuffer,
      uint32_t nBufferMaxLength,
      uint32_t * pnActualLength )
{
   W_ERROR nError;
   tParams_PSeDriver7816SmGetData params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.nChannelReference = nChannelReference;
   params.in.nType = nType;
   params.in.pBuffer = pBuffer;
   params.in.nBufferMaxLength = nBufferMaxLength;
   params.in.pnActualLength = pnActualLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSeDriver7816SmGetData,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmGetData: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmGetData(
           tContext* pContext,
           tParams_PSeDriver7816SmGetData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferMaxLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pnActualLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnActualLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSeDriver7816SmGetData(pContext, pParams->in.hDriverConnection, pParams->in.nChannelReference, pParams->in.nType, pParams->in.pBuffer, pParams->in.nBufferMaxLength, pParams->in.pnActualLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSeDriver7816SmOpenChannel()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSeDriver7816SmOpenChannel(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint32_t nType,
      const uint8_t * pAID,
      uint32_t nAIDLength )
{
   W_ERROR nError;
   tParams_PSeDriver7816SmOpenChannel params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nType = nType;
   params.in.pAID = pAID;
   params.in.nAIDLength = nAIDLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSeDriver7816SmOpenChannel,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmOpenChannel: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmOpenChannel(
           tContext* pContext,
           tParams_PSeDriver7816SmOpenChannel* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pAID = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pAID, pParams->in.nAIDLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSeDriver7816SmOpenChannel(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.nType, pParams->in.pAID, pParams->in.nAIDLength);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEDriverActivateSwpLine()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEDriverActivateSwpLine(
      tContext * pContext,
      uint32_t nSlotIdentifier )
{
   W_ERROR nError;
   tParams_PSEDriverActivateSwpLine params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEDriverActivateSwpLine,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverActivateSwpLine: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverActivateSwpLine(
           tContext* pContext,
           tParams_PSEDriverActivateSwpLine* pParams)
{
   pParams->out = PSEDriverActivateSwpLine(pContext, pParams->in.nSlotIdentifier);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEDriverGetAtr()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEDriverGetAtr(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      uint8_t * pAtrBuffer,
      uint32_t nAtrBufferLength,
      uint32_t * pnAtrLength )
{
   W_ERROR nError;
   tParams_PSEDriverGetAtr params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pAtrBuffer = pAtrBuffer;
   params.in.nAtrBufferLength = nAtrBufferLength;
   params.in.pnAtrLength = pnAtrLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEDriverGetAtr,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverGetAtr: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverGetAtr(
           tContext* pContext,
           tParams_PSEDriverGetAtr* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pAtrBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pAtrBuffer, pParams->in.nAtrBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pnAtrLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnAtrLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEDriverGetAtr(pContext, pParams->in.hDriverConnection, pParams->in.pAtrBuffer, pParams->in.nAtrBufferLength, pParams->in.pnAtrLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEDriverGetInfo()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEDriverGetInfo(
      tContext * pContext,
      uint32_t nSlotIdentifier,
      tWSEInfoEx * pSEInfo,
      uint32_t nSize )
{
   W_ERROR nError;
   tParams_PSEDriverGetInfo params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pSEInfo = pSEInfo;
   params.in.nSize = nSize;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEDriverGetInfo,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverGetInfo: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverGetInfo(
           tContext* pContext,
           tParams_PSEDriverGetInfo* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pSEInfo = (tWSEInfoEx *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pSEInfo, pParams->in.nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tWSEInfoEx *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEDriverGetInfo(pContext, pParams->in.nSlotIdentifier, pParams->in.pSEInfo, pParams->in.nSize);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEDriverGetStatus()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PSEDriverGetStatus(
      tContext * pContext,
      uint32_t nSlotIdentifier,
      tPSEGetStatusCompleted * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PSEDriverGetStatus params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEDriverGetStatus,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverGetStatus: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverGetStatus(
           tContext* pContext,
           tParams_PSEDriverGetStatus* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PSEDriverGetStatus(pContext, pParams->in.nSlotIdentifier, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEDriverImpersonateAndCheckAidAccess()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEDriverImpersonateAndCheckAidAccess(
      tContext * pContext,
      uint32_t nSlotIdentifier,
      const uint8_t * pAIDBuffer,
      uint32_t nAIDLength,
      const uint8_t * pImpersonationDataBuffer,
      uint32_t nImpersonationDataBufferLength )
{
   W_ERROR nError;
   tParams_PSEDriverImpersonateAndCheckAidAccess params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pAIDBuffer = pAIDBuffer;
   params.in.nAIDLength = nAIDLength;
   params.in.pImpersonationDataBuffer = pImpersonationDataBuffer;
   params.in.nImpersonationDataBufferLength = nImpersonationDataBufferLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEDriverImpersonateAndCheckAidAccess,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverImpersonateAndCheckAidAccess: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverImpersonateAndCheckAidAccess(
           tContext* pContext,
           tParams_PSEDriverImpersonateAndCheckAidAccess* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pAIDBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pAIDBuffer, pParams->in.nAIDLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pImpersonationDataBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pImpersonationDataBuffer, pParams->in.nImpersonationDataBufferLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEDriverImpersonateAndCheckAidAccess(pContext, pParams->in.nSlotIdentifier, pParams->in.pAIDBuffer, pParams->in.nAIDLength, pParams->in.pImpersonationDataBuffer, pParams->in.nImpersonationDataBufferLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEDriverOpenConnection()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PSEDriverOpenConnection(
      tContext * pContext,
      uint32_t nSlotIdentifier,
      bool_t bForce,
      tPBasicGenericHandleCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PSEDriverOpenConnection params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.bForce = bForce;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEDriverOpenConnection,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverOpenConnection: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverOpenConnection(
           tContext* pContext,
           tParams_PSEDriverOpenConnection* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PSEDriverOpenConnection(pContext, pParams->in.nSlotIdentifier, pParams->in.bForce, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEDriverSetPolicy()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PSEDriverSetPolicy(
      tContext * pContext,
      uint32_t nSlotIdentifier,
      uint32_t nStorageType,
      uint32_t nProtocols,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   W_ERROR nError;
   tParams_PSEDriverSetPolicy params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.nStorageType = nStorageType;
   params.in.nProtocols = nProtocols;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEDriverSetPolicy,
       &params,
       sizeof(params), 0)) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverSetPolicy: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
    }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverSetPolicy(
           tContext* pContext,
           tParams_PSEDriverSetPolicy* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PSEDriverSetPolicy(pContext, pParams->in.nSlotIdentifier, pParams->in.nStorageType, pParams->in.nProtocols, null, pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEGetConnectivityEventParameter()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEGetConnectivityEventParameter(
      tContext* pContext,
      uint32_t nSlotIdentifier,
      uint8_t * pDataBuffer,
      uint32_t nBufferLength,
      uint32_t * pnActualDataLength )
{
   W_ERROR nError;
   tParams_PSEGetConnectivityEventParameter params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pDataBuffer = pDataBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pnActualDataLength = pnActualDataLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEGetConnectivityEventParameter,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEGetConnectivityEventParameter: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEGetConnectivityEventParameter(
           tContext* pContext,
           tParams_PSEGetConnectivityEventParameter* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pDataBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pDataBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pnActualDataLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.pnActualDataLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEGetConnectivityEventParameter(pContext, pParams->in.nSlotIdentifier, pParams->in.pDataBuffer, pParams->in.nBufferLength, pParams->in.pnActualDataLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEGetTransactionAID()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

uint32_t PSEGetTransactionAID(
      tContext* pContext,
      uint32_t nSlotIdentifier,
      uint8_t * pBuffer,
      uint32_t nBufferLength )
{
   W_ERROR nError;
   tParams_PSEGetTransactionAID params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEGetTransactionAID,
       &params, sizeof(params), sizeof(uint32_t))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEGetTransactionAID: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEGetTransactionAID(
           tContext* pContext,
           tParams_PSEGetTransactionAID* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pBuffer, pParams->in.nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEGetTransactionAID(pContext, pParams->in.nSlotIdentifier, pParams->in.pBuffer, pParams->in.nBufferLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEMonitorConnectivityEvent()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEMonitorConnectivityEvent(
      tContext* pContext,
      uint32_t nSlotIdentifier,
      tPBasicGenericEventHandler2 * pHandler,
      void * pHandlerParameter,
      W_HANDLE * phEventRegistry )
{
   W_ERROR nError;
   tParams_PSEMonitorConnectivityEvent params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEMonitorConnectivityEvent,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEMonitorConnectivityEvent: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEMonitorConnectivityEvent(
           tContext* pContext,
           tParams_PSEMonitorConnectivityEvent* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEMonitorConnectivityEvent(pContext, pParams->in.nSlotIdentifier, null, pDriverCC, pParams->in.phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEMonitorEndOfTransaction()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEMonitorEndOfTransaction(
      tContext* pContext,
      uint32_t nSlotIdentifier,
      tPBasicGenericEventHandler2 * pHandler,
      void * pHandlerParameter,
      W_HANDLE * phEventRegistry )
{
   W_ERROR nError;
   tParams_PSEMonitorEndOfTransaction params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEMonitorEndOfTransaction,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEMonitorEndOfTransaction: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEMonitorEndOfTransaction(
           tContext* pContext,
           tParams_PSEMonitorEndOfTransaction* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEMonitorEndOfTransaction(pContext, pParams->in.nSlotIdentifier, null, pDriverCC, pParams->in.phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PSEMonitorHotPlugEvents()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PSEMonitorHotPlugEvents(
      tContext* pContext,
      uint32_t nSlotIdentifier,
      tPBasicGenericEventHandler2 * pHandler,
      void * pHandlerParameter,
      W_HANDLE * phEventRegistry )
{
   W_ERROR nError;
   tParams_PSEMonitorHotPlugEvents params;
   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PSEMonitorHotPlugEvents,
       &params, sizeof(params), sizeof(W_ERROR))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEMonitorHotPlugEvents: Error %08X returned by CUserIoctl()", nError);
      return nError;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEMonitorHotPlugEvents(
           tContext* pContext,
           tParams_PSEMonitorHotPlugEvents* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->in.pHandler), pParams->in.pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->in.phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PSEMonitorHotPlugEvents(pContext, pParams->in.nSlotIdentifier, null, pDriverCC, pParams->in.phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(pParams->out != W_SUCCESS)
   {
      PDFCDriverFreeCC(pDriverCC);
   }
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      PType1ChipDriverExchangeData()
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_HANDLE PType1ChipDriverExchangeData(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;
   tParams_PType1ChipDriverExchangeData params;
   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
   PContextSetLastIoctlError(pContext, W_SUCCESS);
   while((nError = CUserIoctl(PContextGetUserInstance(pContext),
       P_Idenfier_PType1ChipDriverExchangeData,
       &params, sizeof(params), sizeof(W_HANDLE))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PType1ChipDriverExchangeData: Error %08X returned by CUserIoctl()", nError);
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PType1ChipDriverExchangeData(
           tContext* pContext,
           tParams_PType1ChipDriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->in.pCallback), pParams->in.pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->in.pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->in.pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *) (uintptr_t) 1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParams->out = PType1ChipDriverExchangeData(pContext, pParams->in.hDriverConnection, null, pDriverCC, pParams->in.pReaderToCardBuffer, pParams->in.nReaderToCardBufferLength, pParams->in.pCardToReaderBuffer, pParams->in.nCardToReaderBufferMaxLength);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      Global dispatch table
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

tDriverFunction* const g_aDriverDispatchTable[] = {
   (tDriverFunction*)&static_P14P3DriverExchangeData,
   (tDriverFunction*)&static_P14P3DriverExchangeRawBits,
   (tDriverFunction*)&static_P14P3DriverExchangeRawMifare,
   (tDriverFunction*)&static_P14P3DriverSetTimeout,
   (tDriverFunction*)&static_P14P4DriverExchangeData,
   (tDriverFunction*)&static_P14P4DriverSetTimeout,
   (tDriverFunction*)&static_P15P3DriverExchangeData,
   (tDriverFunction*)&static_P15P3DriverSetTimeout,
   (tDriverFunction*)&static_PBasicDriverCancelOperation,
   (tDriverFunction*)&static_PBasicDriverGetVersion,
   (tDriverFunction*)&static_PBPrimeDriverExchangeData,
   (tDriverFunction*)&static_PBPrimeDriverSetTimeout,
   (tDriverFunction*)&static_PCacheConnectionDriverRead,
   (tDriverFunction*)&static_PCacheConnectionDriverWrite,
   (tDriverFunction*)&static_PContextDriverGenerateRandom,
   (tDriverFunction*)&static_PContextDriverGetMemoryStatistics,
   (tDriverFunction*)&static_PContextDriverResetMemoryStatistics,
   (tDriverFunction*)&static_PDFCDriverInterruptEventLoop,
   (tDriverFunction*)&static_PDFCDriverStopEventLoop,
   (tDriverFunction*)&static_PEmulCloseDriver,
   (tDriverFunction*)&static_PEmulGetMessageData,
   (tDriverFunction*)&static_PEmulOpenConnectionDriver1,
   (tDriverFunction*)&static_PEmulOpenConnectionDriver1Ex,
   (tDriverFunction*)&static_PEmulOpenConnectionDriver2,
   (tDriverFunction*)&static_PEmulOpenConnectionDriver2Ex,
   (tDriverFunction*)&static_PEmulOpenConnectionDriver3,
   (tDriverFunction*)&static_PEmulOpenConnectionDriver3Ex,
   (tDriverFunction*)&static_PEmulSendAnswer,
   (tDriverFunction*)&static_PFeliCaDriverExchangeData,
   (tDriverFunction*)&static_PFeliCaDriverGetCardList,
   (tDriverFunction*)&static_PHandleCheckPropertyDriver,
   (tDriverFunction*)&static_PHandleCloseDriver,
   (tDriverFunction*)&static_PHandleCloseSafeDriver,
   (tDriverFunction*)&static_PHandleGetCountDriver,
   (tDriverFunction*)&static_PHandleGetPropertiesDriver,
   (tDriverFunction*)&static_PHandleGetPropertyNumberDriver,
   (tDriverFunction*)&static_PMultiTimerCancelDriver,
   (tDriverFunction*)&static_PMultiTimerSetDriver,
   (tDriverFunction*)&static_PNALServiceDriverGetCurrentTime,
   (tDriverFunction*)&static_PNALServiceDriverGetProtocolStatistics,
   (tDriverFunction*)&static_PNALServiceDriverResetProtocolStatistics,
   (tDriverFunction*)&static_PNDEFRegisterNPPMessageHandlerDriver,
   (tDriverFunction*)&static_PNDEFRegisterSNEPMessageHandlerDriver,
   (tDriverFunction*)&static_PNDEFRetrieveNPPMessageDriver,
   (tDriverFunction*)&static_PNDEFRetrieveSNEPMessageDriver,
   (tDriverFunction*)&static_PNDEFSendNPPMessageDriver,
   (tDriverFunction*)&static_PNDEFSendSNEPMessageDriver,
   (tDriverFunction*)&static_PNDEFSetWorkPerformedNPPDriver,
   (tDriverFunction*)&static_PNDEFSetWorkPerformedSNEPDriver,
   (tDriverFunction*)&static_PNFCControllerDriverGetRFActivity,
   (tDriverFunction*)&static_PNFCControllerDriverGetRFLock,
   (tDriverFunction*)&static_PNFCControllerDriverReadInfo,
   (tDriverFunction*)&static_PNFCControllerFirmwareUpdateDriver,
   (tDriverFunction*)&static_PNFCControllerFirmwareUpdateState,
   (tDriverFunction*)&static_PNFCControllerGetMode,
   (tDriverFunction*)&static_PNFCControllerGetRawMessageData,
   (tDriverFunction*)&static_PNFCControllerIsActive,
   (tDriverFunction*)&static_PNFCControllerMonitorException,
   (tDriverFunction*)&static_PNFCControllerMonitorFieldEvents,
   (tDriverFunction*)&static_PNFCControllerProductionTestDriver,
   (tDriverFunction*)&static_PNFCControllerRegisterRawListener,
   (tDriverFunction*)&static_PNFCControllerResetDriver,
   (tDriverFunction*)&static_PNFCControllerSelfTestDriver,
   (tDriverFunction*)&static_PNFCControllerSetRFLockDriver,
   (tDriverFunction*)&static_PNFCControllerSwitchStandbyMode,
   (tDriverFunction*)&static_PNFCControllerSwitchToRawModeDriver,
   (tDriverFunction*)&static_PNFCControllerWriteRawMessageDriver,
   (tDriverFunction*)&static_PP2PConnectDriver,
   (tDriverFunction*)&static_PP2PCreateSocketDriver,
   (tDriverFunction*)&static_PP2PEstablishLinkDriver1,
   (tDriverFunction*)&static_PP2PEstablishLinkDriver1Wrapper,
   (tDriverFunction*)&static_PP2PEstablishLinkDriver2,
   (tDriverFunction*)&static_PP2PEstablishLinkDriver2Wrapper,
   (tDriverFunction*)&static_PP2PGetConfigurationDriver,
   (tDriverFunction*)&static_PP2PGetLinkPropertiesDriver,
   (tDriverFunction*)&static_PP2PGetSocketParameterDriver,
   (tDriverFunction*)&static_PP2PReadDriver,
   (tDriverFunction*)&static_PP2PRecvFromDriver,
   (tDriverFunction*)&static_PP2PSendToDriver,
   (tDriverFunction*)&static_PP2PSetConfigurationDriver,
   (tDriverFunction*)&static_PP2PSetSocketParameter,
   (tDriverFunction*)&static_PP2PShutdownDriver,
   (tDriverFunction*)&static_PP2PURILookupDriver,
   (tDriverFunction*)&static_PP2PWriteDriver,
   (tDriverFunction*)&static_PReaderDriverGetLastReferenceTime,
   (tDriverFunction*)&static_PReaderDriverGetNbCardDetected,
   (tDriverFunction*)&static_PReaderDriverRedetectCard,
   (tDriverFunction*)&static_PReaderDriverRegister,
   (tDriverFunction*)&static_PReaderDriverSetWorkPerformedAndClose,
   (tDriverFunction*)&static_PReaderDriverWorkPerformed,
   (tDriverFunction*)&static_PReaderErrorEventRegister,
   (tDriverFunction*)&static_PReaderGetPulsePeriod,
   (tDriverFunction*)&static_PReaderSetPulsePeriodDriver,
   (tDriverFunction*)&static_PRoutingTableApplyDriver,
   (tDriverFunction*)&static_PRoutingTableGetConfigDriver,
   (tDriverFunction*)&static_PRoutingTableReadDriver,
   (tDriverFunction*)&static_PRoutingTableSetConfigDriver,
   (tDriverFunction*)&static_PSecurityManagerDriverAuthenticate,
   (tDriverFunction*)&static_PSeDriver7816SmCloseChannel,
   (tDriverFunction*)&static_PSeDriver7816SmExchangeApdu,
   (tDriverFunction*)&static_PSeDriver7816SmGetData,
   (tDriverFunction*)&static_PSeDriver7816SmOpenChannel,
   (tDriverFunction*)&static_PSEDriverActivateSwpLine,
   (tDriverFunction*)&static_PSEDriverGetAtr,
   (tDriverFunction*)&static_PSEDriverGetInfo,
   (tDriverFunction*)&static_PSEDriverGetStatus,
   (tDriverFunction*)&static_PSEDriverImpersonateAndCheckAidAccess,
   (tDriverFunction*)&static_PSEDriverOpenConnection,
   (tDriverFunction*)&static_PSEDriverSetPolicy,
   (tDriverFunction*)&static_PSEGetConnectivityEventParameter,
   (tDriverFunction*)&static_PSEGetTransactionAID,
   (tDriverFunction*)&static_PSEMonitorConnectivityEvent,
   (tDriverFunction*)&static_PSEMonitorEndOfTransaction,
   (tDriverFunction*)&static_PSEMonitorHotPlugEvents,
   (tDriverFunction*)&static_PType1ChipDriverExchangeData,
};

#endif /* P_CONFIG_DRIVER */

#endif /* #ifndef P_CONFIG_CLIENT_SERVER */


/* End of file */
