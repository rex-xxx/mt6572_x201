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

#ifdef P_CONFIG_CLIENT_SERVER

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
   tMessage_in_out_P14P3DriverExchangeData params;
   W_ERROR nError;

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
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P14P3DriverExchangeData,
                &params, sizeof(tMessage_in_P14P3DriverExchangeData),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                sizeof(tMessage_out_P14P3DriverExchangeData));
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverExchangeData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverExchangeData(
           tContext* pContext,
           tMessage_in_P14P3DriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_P14P3DriverExchangeData*)pParams)->value = P14P3DriverExchangeData(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, pParams->bCheckResponseCRC, pParams->bCheckAckOrNack);
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
   tMessage_in_out_P14P3DriverExchangeRawBits params;
   W_ERROR nError;

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
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P14P3DriverExchangeRawBits,
                &params, sizeof(tMessage_in_P14P3DriverExchangeRawBits),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                sizeof(tMessage_out_P14P3DriverExchangeRawBits));
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverExchangeRawBits: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverExchangeRawBits(
           tContext* pContext,
           tMessage_in_P14P3DriverExchangeRawBits* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_P14P3DriverExchangeRawBits*)pParams)->value = P14P3DriverExchangeRawBits(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->nReaderToCardBufferLastByteBitNumber, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, pParams->nExpectedBits);
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
   tMessage_in_out_P14P3DriverExchangeRawMifare params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P14P3DriverExchangeRawMifare,
                &params, sizeof(tMessage_in_P14P3DriverExchangeRawMifare),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                sizeof(tMessage_out_P14P3DriverExchangeRawMifare));
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverExchangeRawMifare: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverExchangeRawMifare(
           tContext* pContext,
           tMessage_in_P14P3DriverExchangeRawMifare* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_P14P3DriverExchangeRawMifare*)pParams)->value = P14P3DriverExchangeRawMifare(pContext, pParams->hConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength);
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
   tMessage_in_out_P14P3DriverSetTimeout params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P14P3DriverSetTimeout,
                &params, sizeof(tMessage_in_P14P3DriverSetTimeout),
                null, 0,
                null, 0,
                sizeof(tMessage_out_P14P3DriverSetTimeout));
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P3DriverSetTimeout: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P3DriverSetTimeout(
           tContext* pContext,
           tMessage_in_P14P3DriverSetTimeout* pParams)
{
   ((tMessage_out_P14P3DriverSetTimeout*)pParams)->value = P14P3DriverSetTimeout(pContext, pParams->hConnection, pParams->nTimeout);
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
   tMessage_in_out_P14P4DriverExchangeData params;
   W_ERROR nError;

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
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P14P4DriverExchangeData,
                &params, sizeof(tMessage_in_P14P4DriverExchangeData),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                sizeof(tMessage_out_P14P4DriverExchangeData));
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P4DriverExchangeData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P4DriverExchangeData(
           tContext* pContext,
           tMessage_in_P14P4DriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_P14P4DriverExchangeData*)pParams)->value = P14P4DriverExchangeData(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, pParams->bSendNAD, pParams->nNAD, pParams->bCreateOperation);
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
   tMessage_in_out_P14P4DriverSetTimeout params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P14P4DriverSetTimeout,
                &params, sizeof(tMessage_in_P14P4DriverSetTimeout),
                null, 0,
                null, 0,
                sizeof(tMessage_out_P14P4DriverSetTimeout));
   if(nError != W_SUCCESS)
   {
      PDebugError("P14P4DriverSetTimeout: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P14P4DriverSetTimeout(
           tContext* pContext,
           tMessage_in_P14P4DriverSetTimeout* pParams)
{
   ((tMessage_out_P14P4DriverSetTimeout*)pParams)->value = P14P4DriverSetTimeout(pContext, pParams->hConnection, pParams->nTimeout);
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
   tMessage_in_out_P15P3DriverExchangeData params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P15P3DriverExchangeData,
                &params, sizeof(tMessage_in_P15P3DriverExchangeData),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("P15P3DriverExchangeData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P15P3DriverExchangeData(
           tContext* pContext,
           tMessage_in_P15P3DriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   P15P3DriverExchangeData(pContext, pParams->hConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength);
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
   tMessage_in_out_P15P3DriverSetTimeout params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_P15P3DriverSetTimeout,
                &params, sizeof(tMessage_in_P15P3DriverSetTimeout),
                null, 0,
                null, 0,
                sizeof(tMessage_out_P15P3DriverSetTimeout));
   if(nError != W_SUCCESS)
   {
      PDebugError("P15P3DriverSetTimeout: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_P15P3DriverSetTimeout(
           tContext* pContext,
           tMessage_in_P15P3DriverSetTimeout* pParams)
{
   ((tMessage_out_P15P3DriverSetTimeout*)pParams)->value = P15P3DriverSetTimeout(pContext, pParams->hConnection, pParams->nTimeout);
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
   tMessage_in_out_PBasicDriverCancelOperation params;
   W_ERROR nError;

   params.in.hOperation = hOperation;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PBasicDriverCancelOperation,
                &params, sizeof(tMessage_in_PBasicDriverCancelOperation),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PBasicDriverCancelOperation: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBasicDriverCancelOperation(
           tContext* pContext,
           tMessage_in_PBasicDriverCancelOperation* pParams)
{
   PBasicDriverCancelOperation(pContext, pParams->hOperation);
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
   tMessage_in_out_PBasicDriverGetVersion params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferSize = nBufferSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PBasicDriverGetVersion,
                &params, sizeof(tMessage_in_PBasicDriverGetVersion),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PBasicDriverGetVersion));
   if(nError != W_SUCCESS)
   {
      PDebugError("PBasicDriverGetVersion: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBasicDriverGetVersion(
           tContext* pContext,
           tMessage_in_PBasicDriverGetVersion* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (void *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferSize, ( P_SYNC_BUFFER_FLAG_O ))) == (void *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PBasicDriverGetVersion*)pParams)->value = PBasicDriverGetVersion(pContext, pParams->pBuffer, pParams->nBufferSize);
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
   tMessage_in_out_PBPrimeDriverExchangeData params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PBPrimeDriverExchangeData,
                &params, sizeof(tMessage_in_PBPrimeDriverExchangeData),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                sizeof(tMessage_out_PBPrimeDriverExchangeData));
   if(nError != W_SUCCESS)
   {
      PDebugError("PBPrimeDriverExchangeData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBPrimeDriverExchangeData(
           tContext* pContext,
           tMessage_in_PBPrimeDriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PBPrimeDriverExchangeData*)pParams)->value = PBPrimeDriverExchangeData(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength);
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
   tMessage_in_out_PBPrimeDriverSetTimeout params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.nTimeout = nTimeout;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PBPrimeDriverSetTimeout,
                &params, sizeof(tMessage_in_PBPrimeDriverSetTimeout),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PBPrimeDriverSetTimeout));
   if(nError != W_SUCCESS)
   {
      PDebugError("PBPrimeDriverSetTimeout: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PBPrimeDriverSetTimeout(
           tContext* pContext,
           tMessage_in_PBPrimeDriverSetTimeout* pParams)
{
   ((tMessage_out_PBPrimeDriverSetTimeout*)pParams)->value = PBPrimeDriverSetTimeout(pContext, pParams->hConnection, pParams->nTimeout);
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
   tMessage_in_out_PCacheConnectionDriverRead params;
   W_ERROR nError;

   params.in.pCacheConnection = pCacheConnection;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PCacheConnectionDriverRead,
                &params, sizeof(tMessage_in_PCacheConnectionDriverRead),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PCacheConnectionDriverRead));
   if(nError != W_SUCCESS)
   {
      PDebugError("PCacheConnectionDriverRead: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PCacheConnectionDriverRead(
           tContext* pContext,
           tMessage_in_PCacheConnectionDriverRead* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pCacheConnection = (tCacheConnectionInstance *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCacheConnection, pParams->nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tCacheConnectionInstance *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PCacheConnectionDriverRead*)pParams)->value = PCacheConnectionDriverRead(pContext, pParams->pCacheConnection, pParams->nSize);
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
   tMessage_in_out_PCacheConnectionDriverWrite params;
   W_ERROR nError;

   params.in.pCacheConnection = pCacheConnection;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PCacheConnectionDriverWrite,
                &params, sizeof(tMessage_in_PCacheConnectionDriverWrite),
                params.in.pCacheConnection, params.in.nSize,
                null, 0,
                sizeof(tMessage_out_PCacheConnectionDriverWrite));
   if(nError != W_SUCCESS)
   {
      PDebugError("PCacheConnectionDriverWrite: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PCacheConnectionDriverWrite(
           tContext* pContext,
           tMessage_in_PCacheConnectionDriverWrite* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pCacheConnection = (const tCacheConnectionInstance *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCacheConnection, pParams->nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tCacheConnectionInstance *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PCacheConnectionDriverWrite*)pParams)->value = PCacheConnectionDriverWrite(pContext, pParams->pCacheConnection, pParams->nSize);
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
   tMessage_in_out_PContextDriverGenerateRandom params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PContextDriverGenerateRandom,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PContextDriverGenerateRandom));
   if(nError != W_SUCCESS)
   {
      PDebugError("PContextDriverGenerateRandom: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PContextDriverGenerateRandom(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PContextDriverGenerateRandom*)pParams)->value = PContextDriverGenerateRandom(pContext);
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
   tMessage_in_out_PContextDriverGetMemoryStatistics params;
   W_ERROR nError;

   params.in.pStatistics = pStatistics;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PContextDriverGetMemoryStatistics,
                &params, sizeof(tMessage_in_PContextDriverGetMemoryStatistics),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PContextDriverGetMemoryStatistics: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PContextDriverGetMemoryStatistics(
           tContext* pContext,
           tMessage_in_PContextDriverGetMemoryStatistics* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pStatistics = (tContextDriverMemoryStatistics *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pStatistics, pParams->nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tContextDriverMemoryStatistics *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PContextDriverGetMemoryStatistics(pContext, pParams->pStatistics, pParams->nSize);
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
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PContextDriverResetMemoryStatistics,
                null, 0,
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PContextDriverResetMemoryStatistics: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
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
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PDFCDriverInterruptEventLoop,
                null, 0,
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PDFCDriverInterruptEventLoop: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
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
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PDFCDriverStopEventLoop,
                null, 0,
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PDFCDriverStopEventLoop: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
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
   tMessage_in_out_PEmulCloseDriver params;
   W_ERROR nError;

   params.in.hHandle = hHandle;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulCloseDriver,
                &params, sizeof(tMessage_in_PEmulCloseDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PEmulCloseDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulCloseDriver(
           tContext* pContext,
           tMessage_in_PEmulCloseDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulCloseDriver(pContext, pParams->hHandle, null, pDriverCC);
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
   tMessage_in_out_PEmulGetMessageData params;
   W_ERROR nError;

   params.in.hHandle = hHandle;
   params.in.pDataBuffer = pDataBuffer;
   params.in.nDataLength = nDataLength;
   params.in.pnActualDataLength = pnActualDataLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulGetMessageData,
                &params, sizeof(tMessage_in_PEmulGetMessageData),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PEmulGetMessageData));
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulGetMessageData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulGetMessageData(
           tContext* pContext,
           tMessage_in_PEmulGetMessageData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pDataBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pDataBuffer, pParams->nDataLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pnActualDataLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnActualDataLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PEmulGetMessageData*)pParams)->value = PEmulGetMessageData(pContext, pParams->hHandle, pParams->pDataBuffer, pParams->nDataLength, pParams->pnActualDataLength);
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
   tMessage_in_out_PEmulOpenConnectionDriver1 params;
   W_ERROR nError;

   params.in.pOpenCallback = pOpenCallback;
   params.in.pOpenCallbackParameter = pOpenCallbackParameter;
   params.in.pEmulConnectionInfo = pEmulConnectionInfo;
   params.in.nSize = nSize;
   params.in.phHandle = phHandle;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulOpenConnectionDriver1,
                &params, sizeof(tMessage_in_PEmulOpenConnectionDriver1),
                params.in.pEmulConnectionInfo, params.in.nSize,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PEmulOpenConnectionDriver1: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver1(
           tContext* pContext,
           tMessage_in_PEmulOpenConnectionDriver1* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternal(pContext, (tDFCCallback*)(pParams->pOpenCallback), pParams->pOpenCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pEmulConnectionInfo = (const tWEmulConnectionInfo *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pEmulConnectionInfo, pParams->nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tWEmulConnectionInfo *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phHandle, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PEmulOpenConnectionDriver1(pContext, null, pDriverCC, pParams->pEmulConnectionInfo, pParams->nSize, pParams->phHandle);
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
   tMessage_in_out_PEmulOpenConnectionDriver1Ex params;
   W_ERROR nError;

   params.in.pOpenCallback = pOpenCallback;
   params.in.pOpenCallbackParameter = pOpenCallbackParameter;
   params.in.pEmulConnectionInfo = pEmulConnectionInfo;
   params.in.nSize = nSize;
   params.in.phHandle = phHandle;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulOpenConnectionDriver1Ex,
                &params, sizeof(tMessage_in_PEmulOpenConnectionDriver1Ex),
                params.in.pEmulConnectionInfo, params.in.nSize,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PEmulOpenConnectionDriver1Ex: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver1Ex(
           tContext* pContext,
           tMessage_in_PEmulOpenConnectionDriver1Ex* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pOpenCallback), pParams->pOpenCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pEmulConnectionInfo = (const tWEmulConnectionInfo *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pEmulConnectionInfo, pParams->nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tWEmulConnectionInfo *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phHandle, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PEmulOpenConnectionDriver1Ex(pContext, null, pDriverCC, pParams->pEmulConnectionInfo, pParams->nSize, pParams->phHandle);
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
   tMessage_in_out_PEmulOpenConnectionDriver2 params;
   W_ERROR nError;

   params.in.hHandle = hHandle;
   params.in.pEventCallback = pEventCallback;
   params.in.pEventCallbackParameter = pEventCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulOpenConnectionDriver2,
                &params, sizeof(tMessage_in_PEmulOpenConnectionDriver2),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PEmulOpenConnectionDriver2: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver2(
           tContext* pContext,
           tMessage_in_PEmulOpenConnectionDriver2* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pEventCallback), pParams->pEventCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver2(pContext, pParams->hHandle, null, pDriverCC);
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
   tMessage_in_out_PEmulOpenConnectionDriver2Ex params;
   W_ERROR nError;

   params.in.hHandle = hHandle;
   params.in.pEventCallback = pEventCallback;
   params.in.pEventCallbackParameter = pEventCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulOpenConnectionDriver2Ex,
                &params, sizeof(tMessage_in_PEmulOpenConnectionDriver2Ex),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PEmulOpenConnectionDriver2Ex: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver2Ex(
           tContext* pContext,
           tMessage_in_PEmulOpenConnectionDriver2Ex* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->pEventCallback), pParams->pEventCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver2Ex(pContext, pParams->hHandle, null, pDriverCC);
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
   tMessage_in_out_PEmulOpenConnectionDriver3 params;
   W_ERROR nError;

   params.in.hHandle = hHandle;
   params.in.pCommandCallback = pCommandCallback;
   params.in.pCommandCallbackParameter = pCommandCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulOpenConnectionDriver3,
                &params, sizeof(tMessage_in_PEmulOpenConnectionDriver3),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PEmulOpenConnectionDriver3: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver3(
           tContext* pContext,
           tMessage_in_PEmulOpenConnectionDriver3* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pCommandCallback), pParams->pCommandCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver3(pContext, pParams->hHandle, null, pDriverCC);
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
   tMessage_in_out_PEmulOpenConnectionDriver3Ex params;
   W_ERROR nError;

   params.in.hHandle = hHandle;
   params.in.pCommandCallback = pCommandCallback;
   params.in.pCommandCallbackParameter = pCommandCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulOpenConnectionDriver3Ex,
                &params, sizeof(tMessage_in_PEmulOpenConnectionDriver3Ex),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PEmulOpenConnectionDriver3Ex: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulOpenConnectionDriver3Ex(
           tContext* pContext,
           tMessage_in_PEmulOpenConnectionDriver3Ex* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->pCommandCallback), pParams->pCommandCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PEmulOpenConnectionDriver3Ex(pContext, pParams->hHandle, null, pDriverCC);
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
   tMessage_in_out_PEmulSendAnswer params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.pDataBuffer = pDataBuffer;
   params.in.nDataLength = nDataLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PEmulSendAnswer,
                &params, sizeof(tMessage_in_PEmulSendAnswer),
                params.in.pDataBuffer, params.in.nDataLength,
                null, 0,
                sizeof(tMessage_out_PEmulSendAnswer));
   if(nError != W_SUCCESS)
   {
      PDebugError("PEmulSendAnswer: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PEmulSendAnswer(
           tContext* pContext,
           tMessage_in_PEmulSendAnswer* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pDataBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pDataBuffer, pParams->nDataLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PEmulSendAnswer*)pParams)->value = PEmulSendAnswer(pContext, pParams->hDriverConnection, pParams->pDataBuffer, pParams->nDataLength);
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
   tMessage_in_out_PFeliCaDriverExchangeData params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PFeliCaDriverExchangeData,
                &params, sizeof(tMessage_in_PFeliCaDriverExchangeData),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PFeliCaDriverExchangeData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PFeliCaDriverExchangeData(
           tContext* pContext,
           tMessage_in_PFeliCaDriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PFeliCaDriverExchangeData(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength);
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
   tMessage_in_out_PFeliCaDriverGetCardList params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PFeliCaDriverGetCardList,
                &params, sizeof(tMessage_in_PFeliCaDriverGetCardList),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PFeliCaDriverGetCardList: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PFeliCaDriverGetCardList(
           tContext* pContext,
           tMessage_in_PFeliCaDriverGetCardList* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PFeliCaDriverGetCardList(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength);
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
   tMessage_in_out_PHandleCheckPropertyDriver params;
   W_ERROR nError;

   params.in.hObject = hObject;
   params.in.nPropertyValue = nPropertyValue;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PHandleCheckPropertyDriver,
                &params, sizeof(tMessage_in_PHandleCheckPropertyDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PHandleCheckPropertyDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleCheckPropertyDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleCheckPropertyDriver(
           tContext* pContext,
           tMessage_in_PHandleCheckPropertyDriver* pParams)
{
   ((tMessage_out_PHandleCheckPropertyDriver*)pParams)->value = PHandleCheckPropertyDriver(pContext, pParams->hObject, pParams->nPropertyValue);
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
   tMessage_in_out_PHandleCloseDriver params;
   W_ERROR nError;

   params.in.hObject = hObject;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PHandleCloseDriver,
                &params, sizeof(tMessage_in_PHandleCloseDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PHandleCloseDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleCloseDriver(
           tContext* pContext,
           tMessage_in_PHandleCloseDriver* pParams)
{
   PHandleCloseDriver(pContext, pParams->hObject);
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
   tMessage_in_out_PHandleCloseSafeDriver params;
   W_ERROR nError;

   params.in.hObject = hObject;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PHandleCloseSafeDriver,
                &params, sizeof(tMessage_in_PHandleCloseSafeDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PHandleCloseSafeDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleCloseSafeDriver(
           tContext* pContext,
           tMessage_in_PHandleCloseSafeDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PHandleCloseSafeDriver(pContext, pParams->hObject, null, pDriverCC);
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
   tMessage_in_out_PHandleGetCountDriver params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PHandleGetCountDriver,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PHandleGetCountDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleGetCountDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleGetCountDriver(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PHandleGetCountDriver*)pParams)->value = PHandleGetCountDriver(pContext);
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
   tMessage_in_out_PHandleGetPropertiesDriver params;
   W_ERROR nError;

   params.in.hObject = hObject;
   params.in.pPropertyArray = pPropertyArray;
   params.in.nPropertyArrayLength = nPropertyArrayLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PHandleGetPropertiesDriver,
                &params, sizeof(tMessage_in_PHandleGetPropertiesDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PHandleGetPropertiesDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleGetPropertiesDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleGetPropertiesDriver(
           tContext* pContext,
           tMessage_in_PHandleGetPropertiesDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pPropertyArray = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pPropertyArray, pParams->nPropertyArrayLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PHandleGetPropertiesDriver*)pParams)->value = PHandleGetPropertiesDriver(pContext, pParams->hObject, pParams->pPropertyArray, pParams->nPropertyArrayLength);
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
   tMessage_in_out_PHandleGetPropertyNumberDriver params;
   W_ERROR nError;

   params.in.hObject = hObject;
   params.in.pnPropertyNumber = pnPropertyNumber;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PHandleGetPropertyNumberDriver,
                &params, sizeof(tMessage_in_PHandleGetPropertyNumberDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PHandleGetPropertyNumberDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleGetPropertyNumberDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PHandleGetPropertyNumberDriver(
           tContext* pContext,
           tMessage_in_PHandleGetPropertyNumberDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pnPropertyNumber = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnPropertyNumber, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PHandleGetPropertyNumberDriver*)pParams)->value = PHandleGetPropertyNumberDriver(pContext, pParams->hObject, pParams->pnPropertyNumber);
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
   tMessage_in_out_PMultiTimerCancelDriver params;
   W_ERROR nError;

   params.in.nTimerIdentifier = nTimerIdentifier;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PMultiTimerCancelDriver,
                &params, sizeof(tMessage_in_PMultiTimerCancelDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PMultiTimerCancelDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PMultiTimerCancelDriver(
           tContext* pContext,
           tMessage_in_PMultiTimerCancelDriver* pParams)
{
   PMultiTimerCancelDriver(pContext, pParams->nTimerIdentifier);
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
   tMessage_in_out_PMultiTimerSetDriver params;
   W_ERROR nError;

   params.in.nTimerIdentifier = nTimerIdentifier;
   params.in.nAbsoluteTimeout = nAbsoluteTimeout;
   params.in.pCallbackFunction = pCallbackFunction;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PMultiTimerSetDriver,
                &params, sizeof(tMessage_in_PMultiTimerSetDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PMultiTimerSetDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PMultiTimerSetDriver(
           tContext* pContext,
           tMessage_in_PMultiTimerSetDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallbackFunction), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PMultiTimerSetDriver(pContext, pParams->nTimerIdentifier, pParams->nAbsoluteTimeout, null, pDriverCC);
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
   tMessage_in_out_PNALServiceDriverGetCurrentTime params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNALServiceDriverGetCurrentTime,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNALServiceDriverGetCurrentTime));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNALServiceDriverGetCurrentTime: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNALServiceDriverGetCurrentTime(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PNALServiceDriverGetCurrentTime*)pParams)->value = PNALServiceDriverGetCurrentTime(pContext);
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
   tMessage_in_out_PNALServiceDriverGetProtocolStatistics params;
   W_ERROR nError;

   params.in.pStatistics = pStatistics;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNALServiceDriverGetProtocolStatistics,
                &params, sizeof(tMessage_in_PNALServiceDriverGetProtocolStatistics),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNALServiceDriverGetProtocolStatistics: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNALServiceDriverGetProtocolStatistics(
           tContext* pContext,
           tMessage_in_PNALServiceDriverGetProtocolStatistics* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pStatistics = (tNALProtocolStatistics *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pStatistics, pParams->nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tNALProtocolStatistics *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNALServiceDriverGetProtocolStatistics(pContext, pParams->pStatistics, pParams->nSize);
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
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNALServiceDriverResetProtocolStatistics,
                null, 0,
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNALServiceDriverResetProtocolStatistics: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
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
   tMessage_in_out_PNDEFRegisterNPPMessageHandlerDriver params;
   W_ERROR nError;

   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.nPriority = nPriority;
   params.in.phRegistry = phRegistry;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFRegisterNPPMessageHandlerDriver,
                &params, sizeof(tMessage_in_PNDEFRegisterNPPMessageHandlerDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNDEFRegisterNPPMessageHandlerDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRegisterNPPMessageHandlerDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRegisterNPPMessageHandlerDriver(
           tContext* pContext,
           tMessage_in_PNDEFRegisterNPPMessageHandlerDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNDEFRegisterNPPMessageHandlerDriver*)pParams)->value = PNDEFRegisterNPPMessageHandlerDriver(pContext, null, pDriverCC, pParams->nPriority, pParams->phRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PNDEFRegisterNPPMessageHandlerDriver*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PNDEFRegisterSNEPMessageHandlerDriver params;
   W_ERROR nError;

   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.nPriority = nPriority;
   params.in.phRegistry = phRegistry;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFRegisterSNEPMessageHandlerDriver,
                &params, sizeof(tMessage_in_PNDEFRegisterSNEPMessageHandlerDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNDEFRegisterSNEPMessageHandlerDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRegisterSNEPMessageHandlerDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRegisterSNEPMessageHandlerDriver(
           tContext* pContext,
           tMessage_in_PNDEFRegisterSNEPMessageHandlerDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNDEFRegisterSNEPMessageHandlerDriver*)pParams)->value = PNDEFRegisterSNEPMessageHandlerDriver(pContext, null, pDriverCC, pParams->nPriority, pParams->phRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PNDEFRegisterSNEPMessageHandlerDriver*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PNDEFRetrieveNPPMessageDriver params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFRetrieveNPPMessageDriver,
                &params, sizeof(tMessage_in_PNDEFRetrieveNPPMessageDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNDEFRetrieveNPPMessageDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRetrieveNPPMessageDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRetrieveNPPMessageDriver(
           tContext* pContext,
           tMessage_in_PNDEFRetrieveNPPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNDEFRetrieveNPPMessageDriver*)pParams)->value = PNDEFRetrieveNPPMessageDriver(pContext, pParams->pBuffer, pParams->nBufferLength);
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
   tMessage_in_out_PNDEFRetrieveSNEPMessageDriver params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFRetrieveSNEPMessageDriver,
                &params, sizeof(tMessage_in_PNDEFRetrieveSNEPMessageDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNDEFRetrieveSNEPMessageDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFRetrieveSNEPMessageDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFRetrieveSNEPMessageDriver(
           tContext* pContext,
           tMessage_in_PNDEFRetrieveSNEPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNDEFRetrieveSNEPMessageDriver*)pParams)->value = PNDEFRetrieveSNEPMessageDriver(pContext, pParams->pBuffer, pParams->nBufferLength);
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
   tMessage_in_out_PNDEFSendNPPMessageDriver params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFSendNPPMessageDriver,
                &params, sizeof(tMessage_in_PNDEFSendNPPMessageDriver),
                params.in.pBuffer, params.in.nBufferLength,
                null, 0,
                sizeof(tMessage_out_PNDEFSendNPPMessageDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFSendNPPMessageDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSendNPPMessageDriver(
           tContext* pContext,
           tMessage_in_PNDEFSendNPPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNDEFSendNPPMessageDriver*)pParams)->value = PNDEFSendNPPMessageDriver(pContext, pParams->pBuffer, pParams->nBufferLength, null, pDriverCC);
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
   tMessage_in_out_PNDEFSendSNEPMessageDriver params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFSendSNEPMessageDriver,
                &params, sizeof(tMessage_in_PNDEFSendSNEPMessageDriver),
                params.in.pBuffer, params.in.nBufferLength,
                null, 0,
                sizeof(tMessage_out_PNDEFSendSNEPMessageDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNDEFSendSNEPMessageDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSendSNEPMessageDriver(
           tContext* pContext,
           tMessage_in_PNDEFSendSNEPMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNDEFSendSNEPMessageDriver*)pParams)->value = PNDEFSendSNEPMessageDriver(pContext, pParams->pBuffer, pParams->nBufferLength, null, pDriverCC);
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
   tMessage_in_out_PNDEFSetWorkPerformedNPPDriver params;
   W_ERROR nError;

   params.in.bGiveToNextListener = bGiveToNextListener;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFSetWorkPerformedNPPDriver,
                &params, sizeof(tMessage_in_PNDEFSetWorkPerformedNPPDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNDEFSetWorkPerformedNPPDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSetWorkPerformedNPPDriver(
           tContext* pContext,
           tMessage_in_PNDEFSetWorkPerformedNPPDriver* pParams)
{
   PNDEFSetWorkPerformedNPPDriver(pContext, pParams->bGiveToNextListener);
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
   tMessage_in_out_PNDEFSetWorkPerformedSNEPDriver params;
   W_ERROR nError;

   params.in.bGiveToNextListener = bGiveToNextListener;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNDEFSetWorkPerformedSNEPDriver,
                &params, sizeof(tMessage_in_PNDEFSetWorkPerformedSNEPDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNDEFSetWorkPerformedSNEPDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNDEFSetWorkPerformedSNEPDriver(
           tContext* pContext,
           tMessage_in_PNDEFSetWorkPerformedSNEPDriver* pParams)
{
   PNDEFSetWorkPerformedSNEPDriver(pContext, pParams->bGiveToNextListener);
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
   tMessage_in_out_PNFCControllerDriverGetRFActivity params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerDriverGetRFActivity,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerDriverGetRFActivity));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerDriverGetRFActivity: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerDriverGetRFActivity(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PNFCControllerDriverGetRFActivity*)pParams)->value = PNFCControllerDriverGetRFActivity(pContext);
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
   tMessage_in_out_PNFCControllerDriverGetRFLock params;
   W_ERROR nError;

   params.in.nLockSet = nLockSet;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerDriverGetRFLock,
                &params, sizeof(tMessage_in_PNFCControllerDriverGetRFLock),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerDriverGetRFLock));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerDriverGetRFLock: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerDriverGetRFLock(
           tContext* pContext,
           tMessage_in_PNFCControllerDriverGetRFLock* pParams)
{
   ((tMessage_out_PNFCControllerDriverGetRFLock*)pParams)->value = PNFCControllerDriverGetRFLock(pContext, pParams->nLockSet);
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
   tMessage_in_out_PNFCControllerDriverReadInfo params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferSize = nBufferSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerDriverReadInfo,
                &params, sizeof(tMessage_in_PNFCControllerDriverReadInfo),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerDriverReadInfo));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerDriverReadInfo: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerDriverReadInfo(
           tContext* pContext,
           tMessage_in_PNFCControllerDriverReadInfo* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (void *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferSize, ( P_SYNC_BUFFER_FLAG_O ))) == (void *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNFCControllerDriverReadInfo*)pParams)->value = PNFCControllerDriverReadInfo(pContext, pParams->pBuffer, pParams->nBufferSize);
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
   tMessage_in_out_PNFCControllerFirmwareUpdateDriver params;
   W_ERROR nError;

   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pUpdateBuffer = pUpdateBuffer;
   params.in.nUpdateBufferLength = nUpdateBufferLength;
   params.in.nMode = nMode;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerFirmwareUpdateDriver,
                &params, sizeof(tMessage_in_PNFCControllerFirmwareUpdateDriver),
                params.in.pUpdateBuffer, params.in.nUpdateBufferLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNFCControllerFirmwareUpdateDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerFirmwareUpdateDriver(
           tContext* pContext,
           tMessage_in_PNFCControllerFirmwareUpdateDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pUpdateBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pUpdateBuffer, pParams->nUpdateBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNFCControllerFirmwareUpdateDriver(pContext, null, pDriverCC, pParams->pUpdateBuffer, pParams->nUpdateBufferLength, pParams->nMode);
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
   tMessage_in_out_PNFCControllerFirmwareUpdateState params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerFirmwareUpdateState,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerFirmwareUpdateState));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerFirmwareUpdateState: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerFirmwareUpdateState(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PNFCControllerFirmwareUpdateState*)pParams)->value = PNFCControllerFirmwareUpdateState(pContext);
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
   tMessage_in_out_PNFCControllerGetMode params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerGetMode,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerGetMode));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerGetMode: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerGetMode(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PNFCControllerGetMode*)pParams)->value = PNFCControllerGetMode(pContext);
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
   tMessage_in_out_PNFCControllerGetRawMessageData params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pnActualLength = pnActualLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerGetRawMessageData,
                &params, sizeof(tMessage_in_PNFCControllerGetRawMessageData),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerGetRawMessageData));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerGetRawMessageData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerGetRawMessageData(
           tContext* pContext,
           tMessage_in_PNFCControllerGetRawMessageData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pnActualLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnActualLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNFCControllerGetRawMessageData*)pParams)->value = PNFCControllerGetRawMessageData(pContext, pParams->pBuffer, pParams->nBufferLength, pParams->pnActualLength);
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
   tMessage_in_out_PNFCControllerIsActive params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerIsActive,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerIsActive));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerIsActive: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (bool_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerIsActive(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PNFCControllerIsActive*)pParams)->value = PNFCControllerIsActive(pContext);
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
   tMessage_in_out_PNFCControllerMonitorException params;
   W_ERROR nError;

   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerMonitorException,
                &params, sizeof(tMessage_in_PNFCControllerMonitorException),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerMonitorException));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerMonitorException: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerMonitorException(
           tContext* pContext,
           tMessage_in_PNFCControllerMonitorException* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNFCControllerMonitorException*)pParams)->value = PNFCControllerMonitorException(pContext, null, pDriverCC, pParams->phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PNFCControllerMonitorException*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PNFCControllerMonitorFieldEvents params;
   W_ERROR nError;

   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerMonitorFieldEvents,
                &params, sizeof(tMessage_in_PNFCControllerMonitorFieldEvents),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerMonitorFieldEvents));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerMonitorFieldEvents: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerMonitorFieldEvents(
           tContext* pContext,
           tMessage_in_PNFCControllerMonitorFieldEvents* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PNFCControllerMonitorFieldEvents*)pParams)->value = PNFCControllerMonitorFieldEvents(pContext, null, pDriverCC, pParams->phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PNFCControllerMonitorFieldEvents*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PNFCControllerProductionTestDriver params;
   W_ERROR nError;

   params.in.pParameterBuffer = pParameterBuffer;
   params.in.nParameterBufferLength = nParameterBufferLength;
   params.in.pResultBuffer = pResultBuffer;
   params.in.nResultBufferLength = nResultBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerProductionTestDriver,
                &params, sizeof(tMessage_in_PNFCControllerProductionTestDriver),
                params.in.pParameterBuffer, params.in.nParameterBufferLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNFCControllerProductionTestDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerProductionTestDriver(
           tContext* pContext,
           tMessage_in_PNFCControllerProductionTestDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pParameterBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pParameterBuffer, pParams->nParameterBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pResultBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pResultBuffer, pParams->nResultBufferLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNFCControllerProductionTestDriver(pContext, pParams->pParameterBuffer, pParams->nParameterBufferLength, pParams->pResultBuffer, pParams->nResultBufferLength, null, pDriverCC);
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
   tMessage_in_out_PNFCControllerRegisterRawListener params;
   W_ERROR nError;

   params.in.pReceiveMessageEventHandler = pReceiveMessageEventHandler;
   params.in.pHandlerParameter = pHandlerParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerRegisterRawListener,
                &params, sizeof(tMessage_in_PNFCControllerRegisterRawListener),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerRegisterRawListener));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerRegisterRawListener: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerRegisterRawListener(
           tContext* pContext,
           tMessage_in_PNFCControllerRegisterRawListener* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pReceiveMessageEventHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   ((tMessage_out_PNFCControllerRegisterRawListener*)pParams)->value = PNFCControllerRegisterRawListener(pContext, null, pDriverCC);
   if(((tMessage_out_PNFCControllerRegisterRawListener*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PNFCControllerResetDriver params;
   W_ERROR nError;

   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nMode = nMode;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerResetDriver,
                &params, sizeof(tMessage_in_PNFCControllerResetDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNFCControllerResetDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerResetDriver(
           tContext* pContext,
           tMessage_in_PNFCControllerResetDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PNFCControllerResetDriver(pContext, null, pDriverCC, pParams->nMode);
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
   tMessage_in_out_PNFCControllerSelfTestDriver params;
   W_ERROR nError;

   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerSelfTestDriver,
                &params, sizeof(tMessage_in_PNFCControllerSelfTestDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNFCControllerSelfTestDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSelfTestDriver(
           tContext* pContext,
           tMessage_in_PNFCControllerSelfTestDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
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
   tMessage_in_out_PNFCControllerSetRFLockDriver params;
   W_ERROR nError;

   params.in.nLockSet = nLockSet;
   params.in.bReaderLock = bReaderLock;
   params.in.bCardLock = bCardLock;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerSetRFLockDriver,
                &params, sizeof(tMessage_in_PNFCControllerSetRFLockDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNFCControllerSetRFLockDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSetRFLockDriver(
           tContext* pContext,
           tMessage_in_PNFCControllerSetRFLockDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PNFCControllerSetRFLockDriver(pContext, pParams->nLockSet, pParams->bReaderLock, pParams->bCardLock, null, pDriverCC);
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
   tMessage_in_out_PNFCControllerSwitchStandbyMode params;
   W_ERROR nError;

   params.in.bStandbyOn = bStandbyOn;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerSwitchStandbyMode,
                &params, sizeof(tMessage_in_PNFCControllerSwitchStandbyMode),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PNFCControllerSwitchStandbyMode));
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerSwitchStandbyMode: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSwitchStandbyMode(
           tContext* pContext,
           tMessage_in_PNFCControllerSwitchStandbyMode* pParams)
{
   ((tMessage_out_PNFCControllerSwitchStandbyMode*)pParams)->value = PNFCControllerSwitchStandbyMode(pContext, pParams->bStandbyOn);
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
   tMessage_in_out_PNFCControllerSwitchToRawModeDriver params;
   W_ERROR nError;

   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerSwitchToRawModeDriver,
                &params, sizeof(tMessage_in_PNFCControllerSwitchToRawModeDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNFCControllerSwitchToRawModeDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerSwitchToRawModeDriver(
           tContext* pContext,
           tMessage_in_PNFCControllerSwitchToRawModeDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
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
   tMessage_in_out_PNFCControllerWriteRawMessageDriver params;
   W_ERROR nError;

   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pBuffer = pBuffer;
   params.in.nLength = nLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PNFCControllerWriteRawMessageDriver,
                &params, sizeof(tMessage_in_PNFCControllerWriteRawMessageDriver),
                params.in.pBuffer, params.in.nLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PNFCControllerWriteRawMessageDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PNFCControllerWriteRawMessageDriver(
           tContext* pContext,
           tMessage_in_PNFCControllerWriteRawMessageDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PNFCControllerWriteRawMessageDriver(pContext, null, pDriverCC, pParams->pBuffer, pParams->nLength);
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
   tMessage_in_out_PP2PConnectDriver params;
   W_ERROR nError;

   params.in.hSocket = hSocket;
   params.in.hLink = hLink;
   params.in.pEstablishmentCallback = pEstablishmentCallback;
   params.in.pEstablishmentCallbackParameter = pEstablishmentCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PConnectDriver,
                &params, sizeof(tMessage_in_PP2PConnectDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PConnectDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PConnectDriver(
           tContext* pContext,
           tMessage_in_PP2PConnectDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pEstablishmentCallback), pParams->pEstablishmentCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PP2PConnectDriver(pContext, pParams->hSocket, pParams->hLink, null, pDriverCC);
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
   tMessage_in_out_PP2PCreateSocketDriver params;
   W_ERROR nError;

   params.in.nType = nType;
   params.in.pServiceURI = pServiceURI;
   params.in.nSize = nSize;
   params.in.nSAP = nSAP;
   params.in.phSocket = phSocket;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PCreateSocketDriver,
                &params, sizeof(tMessage_in_PP2PCreateSocketDriver),
                params.in.pServiceURI, params.in.nSize,
                null, 0,
                sizeof(tMessage_out_PP2PCreateSocketDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PCreateSocketDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PCreateSocketDriver(
           tContext* pContext,
           tMessage_in_PP2PCreateSocketDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pServiceURI = (const char16_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pServiceURI, pParams->nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const char16_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phSocket = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phSocket, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PP2PCreateSocketDriver*)pParams)->value = PP2PCreateSocketDriver(pContext, pParams->nType, pParams->pServiceURI, pParams->nSize, pParams->nSAP, pParams->phSocket);
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
   tMessage_in_out_PP2PEstablishLinkDriver1 params;
   W_ERROR nError;

   params.in.pEstablishmentCallback = pEstablishmentCallback;
   params.in.pEstablishmentCallbackParameter = pEstablishmentCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PEstablishLinkDriver1,
                &params, sizeof(tMessage_in_PP2PEstablishLinkDriver1),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PP2PEstablishLinkDriver1));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PEstablishLinkDriver1: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver1(
           tContext* pContext,
           tMessage_in_PP2PEstablishLinkDriver1* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternal(pContext, (tDFCCallback*)(pParams->pEstablishmentCallback), pParams->pEstablishmentCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   ((tMessage_out_PP2PEstablishLinkDriver1*)pParams)->value = PP2PEstablishLinkDriver1(pContext, null, pDriverCC);
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
   tMessage_in_out_PP2PEstablishLinkDriver1Wrapper params;
   W_ERROR nError;

   params.in.pEstablishmentCallback = pEstablishmentCallback;
   params.in.pEstablishmentCallbackParameter = pEstablishmentCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PEstablishLinkDriver1Wrapper,
                &params, sizeof(tMessage_in_PP2PEstablishLinkDriver1Wrapper),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PP2PEstablishLinkDriver1Wrapper));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PEstablishLinkDriver1Wrapper: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver1Wrapper(
           tContext* pContext,
           tMessage_in_PP2PEstablishLinkDriver1Wrapper* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pEstablishmentCallback), pParams->pEstablishmentCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   ((tMessage_out_PP2PEstablishLinkDriver1Wrapper*)pParams)->value = PP2PEstablishLinkDriver1Wrapper(pContext, null, pDriverCC);
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
   tMessage_in_out_PP2PEstablishLinkDriver2 params;
   W_ERROR nError;

   params.in.hLink = hLink;
   params.in.pReleaseCallback = pReleaseCallback;
   params.in.pReleaseCallbackParameter = pReleaseCallbackParameter;
   params.in.phOperation = phOperation;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PEstablishLinkDriver2,
                &params, sizeof(tMessage_in_PP2PEstablishLinkDriver2),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PEstablishLinkDriver2: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver2(
           tContext* pContext,
           tMessage_in_PP2PEstablishLinkDriver2* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternal(pContext, (tDFCCallback*)(pParams->pReleaseCallback), pParams->pReleaseCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phOperation, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PEstablishLinkDriver2(pContext, pParams->hLink, null, pDriverCC, pParams->phOperation);
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
   tMessage_in_out_PP2PEstablishLinkDriver2Wrapper params;
   W_ERROR nError;

   params.in.hLink = hLink;
   params.in.pReleaseCallback = pReleaseCallback;
   params.in.pReleaseCallbackParameter = pReleaseCallbackParameter;
   params.in.phOperation = phOperation;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PEstablishLinkDriver2Wrapper,
                &params, sizeof(tMessage_in_PP2PEstablishLinkDriver2Wrapper),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PEstablishLinkDriver2Wrapper: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PEstablishLinkDriver2Wrapper(
           tContext* pContext,
           tMessage_in_PP2PEstablishLinkDriver2Wrapper* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pReleaseCallback), pParams->pReleaseCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phOperation, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PEstablishLinkDriver2Wrapper(pContext, pParams->hLink, null, pDriverCC, pParams->phOperation);
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
   tMessage_in_out_PP2PGetConfigurationDriver params;
   W_ERROR nError;

   params.in.pConfiguration = pConfiguration;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PGetConfigurationDriver,
                &params, sizeof(tMessage_in_PP2PGetConfigurationDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PP2PGetConfigurationDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PGetConfigurationDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PGetConfigurationDriver(
           tContext* pContext,
           tMessage_in_PP2PGetConfigurationDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pConfiguration = (tWP2PConfiguration *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pConfiguration, pParams->nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tWP2PConfiguration *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PP2PGetConfigurationDriver*)pParams)->value = PP2PGetConfigurationDriver(pContext, pParams->pConfiguration, pParams->nSize);
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
   tMessage_in_out_PP2PGetLinkPropertiesDriver params;
   W_ERROR nError;

   params.in.hLink = hLink;
   params.in.pProperties = pProperties;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PGetLinkPropertiesDriver,
                &params, sizeof(tMessage_in_PP2PGetLinkPropertiesDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PP2PGetLinkPropertiesDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PGetLinkPropertiesDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PGetLinkPropertiesDriver(
           tContext* pContext,
           tMessage_in_PP2PGetLinkPropertiesDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pProperties = (tWP2PLinkProperties *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pProperties, pParams->nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tWP2PLinkProperties *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PP2PGetLinkPropertiesDriver*)pParams)->value = PP2PGetLinkPropertiesDriver(pContext, pParams->hLink, pParams->pProperties, pParams->nSize);
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
   tMessage_in_out_PP2PGetSocketParameterDriver params;
   W_ERROR nError;

   params.in.hSocket = hSocket;
   params.in.nParameter = nParameter;
   params.in.pnValue = pnValue;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PGetSocketParameterDriver,
                &params, sizeof(tMessage_in_PP2PGetSocketParameterDriver),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PP2PGetSocketParameterDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PGetSocketParameterDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PGetSocketParameterDriver(
           tContext* pContext,
           tMessage_in_PP2PGetSocketParameterDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pnValue = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnValue, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PP2PGetSocketParameterDriver*)pParams)->value = PP2PGetSocketParameterDriver(pContext, pParams->hSocket, pParams->nParameter, pParams->pnValue);
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
   tMessage_in_out_PP2PReadDriver params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReceptionBuffer = pReceptionBuffer;
   params.in.nReceptionBufferLength = nReceptionBufferLength;
   params.in.phOperation = phOperation;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PReadDriver,
                &params, sizeof(tMessage_in_PP2PReadDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PReadDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PReadDriver(
           tContext* pContext,
           tMessage_in_PP2PReadDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReceptionBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReceptionBuffer, pParams->nReceptionBufferLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PReadDriver(pContext, pParams->hConnection, null, pDriverCC, pParams->pReceptionBuffer, pParams->nReceptionBufferLength, pParams->phOperation);
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
   tMessage_in_out_PP2PRecvFromDriver params;
   W_ERROR nError;

   params.in.hSocket = hSocket;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReceptionBuffer = pReceptionBuffer;
   params.in.nReceptionBufferLength = nReceptionBufferLength;
   params.in.phOperation = phOperation;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PRecvFromDriver,
                &params, sizeof(tMessage_in_PP2PRecvFromDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PRecvFromDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PRecvFromDriver(
           tContext* pContext,
           tMessage_in_PP2PRecvFromDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReceptionBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReceptionBuffer, pParams->nReceptionBufferLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PRecvFromDriver(pContext, pParams->hSocket, null, pDriverCC, pParams->pReceptionBuffer, pParams->nReceptionBufferLength, pParams->phOperation);
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
   tMessage_in_out_PP2PSendToDriver params;
   W_ERROR nError;

   params.in.hSocket = hSocket;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nSAP = nSAP;
   params.in.pSendBuffer = pSendBuffer;
   params.in.nSendBufferLength = nSendBufferLength;
   params.in.phOperation = phOperation;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PSendToDriver,
                &params, sizeof(tMessage_in_PP2PSendToDriver),
                params.in.pSendBuffer, params.in.nSendBufferLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PSendToDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PSendToDriver(
           tContext* pContext,
           tMessage_in_PP2PSendToDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pSendBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pSendBuffer, pParams->nSendBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PSendToDriver(pContext, pParams->hSocket, null, pDriverCC, pParams->nSAP, pParams->pSendBuffer, pParams->nSendBufferLength, pParams->phOperation);
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
   tMessage_in_out_PP2PSetConfigurationDriver params;
   W_ERROR nError;

   params.in.pConfiguration = pConfiguration;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PSetConfigurationDriver,
                &params, sizeof(tMessage_in_PP2PSetConfigurationDriver),
                params.in.pConfiguration, params.in.nSize,
                null, 0,
                sizeof(tMessage_out_PP2PSetConfigurationDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PSetConfigurationDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PSetConfigurationDriver(
           tContext* pContext,
           tMessage_in_PP2PSetConfigurationDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pConfiguration = (const tWP2PConfiguration *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pConfiguration, pParams->nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const tWP2PConfiguration *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PP2PSetConfigurationDriver*)pParams)->value = PP2PSetConfigurationDriver(pContext, pParams->pConfiguration, pParams->nSize);
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
   tMessage_in_out_PP2PSetSocketParameter params;
   W_ERROR nError;

   params.in.hSocket = hSocket;
   params.in.nParameter = nParameter;
   params.in.nValue = nValue;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PSetSocketParameter,
                &params, sizeof(tMessage_in_PP2PSetSocketParameter),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PP2PSetSocketParameter));
   if(nError != W_SUCCESS)
   {
      PDebugError("PP2PSetSocketParameter: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PSetSocketParameter(
           tContext* pContext,
           tMessage_in_PP2PSetSocketParameter* pParams)
{
   ((tMessage_out_PP2PSetSocketParameter*)pParams)->value = PP2PSetSocketParameter(pContext, pParams->hSocket, pParams->nParameter, pParams->nValue);
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
   tMessage_in_out_PP2PShutdownDriver params;
   W_ERROR nError;

   params.in.hSocket = hSocket;
   params.in.pReleaseCallback = pReleaseCallback;
   params.in.pReleaseCallbackParameter = pReleaseCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PShutdownDriver,
                &params, sizeof(tMessage_in_PP2PShutdownDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PShutdownDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PShutdownDriver(
           tContext* pContext,
           tMessage_in_PP2PShutdownDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pReleaseCallback), pParams->pReleaseCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PP2PShutdownDriver(pContext, pParams->hSocket, null, pDriverCC);
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
   tMessage_in_out_PP2PURILookupDriver params;
   W_ERROR nError;

   params.in.hLink = hLink;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pServiceURI = pServiceURI;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PURILookupDriver,
                &params, sizeof(tMessage_in_PP2PURILookupDriver),
                params.in.pServiceURI, params.in.nSize,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PURILookupDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PURILookupDriver(
           tContext* pContext,
           tMessage_in_PP2PURILookupDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pServiceURI = (const char16_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pServiceURI, pParams->nSize, ( P_SYNC_BUFFER_FLAG_I ))) == (const char16_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PURILookupDriver(pContext, pParams->hLink, null, pDriverCC, pParams->pServiceURI, pParams->nSize);
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
   tMessage_in_out_PP2PWriteDriver params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pSendBuffer = pSendBuffer;
   params.in.nSendBufferLength = nSendBufferLength;
   params.in.phOperation = phOperation;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PP2PWriteDriver,
                &params, sizeof(tMessage_in_PP2PWriteDriver),
                params.in.pSendBuffer, params.in.nSendBufferLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PP2PWriteDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PP2PWriteDriver(
           tContext* pContext,
           tMessage_in_PP2PWriteDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pSendBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pSendBuffer, pParams->nSendBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phOperation = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phOperation, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PP2PWriteDriver(pContext, pParams->hConnection, null, pDriverCC, pParams->pSendBuffer, pParams->nSendBufferLength, pParams->phOperation);
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
   tMessage_in_out_PReaderDriverGetLastReferenceTime params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderDriverGetLastReferenceTime,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PReaderDriverGetLastReferenceTime));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverGetLastReferenceTime: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverGetLastReferenceTime(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PReaderDriverGetLastReferenceTime*)pParams)->value = PReaderDriverGetLastReferenceTime(pContext);
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
   tMessage_in_out_PReaderDriverGetNbCardDetected params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderDriverGetNbCardDetected,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PReaderDriverGetNbCardDetected));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverGetNbCardDetected: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint8_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverGetNbCardDetected(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PReaderDriverGetNbCardDetected*)pParams)->value = PReaderDriverGetNbCardDetected(pContext);
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
   tMessage_in_out_PReaderDriverRedetectCard params;
   W_ERROR nError;

   params.in.hConnection = hConnection;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderDriverRedetectCard,
                &params, sizeof(tMessage_in_PReaderDriverRedetectCard),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PReaderDriverRedetectCard));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverRedetectCard: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverRedetectCard(
           tContext* pContext,
           tMessage_in_PReaderDriverRedetectCard* pParams)
{
   ((tMessage_out_PReaderDriverRedetectCard*)pParams)->value = PReaderDriverRedetectCard(pContext, pParams->hConnection);
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
   tMessage_in_out_PReaderDriverRegister params;
   W_ERROR nError;

   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nPriority = nPriority;
   params.in.nRequestedProtocolsBF = nRequestedProtocolsBF;
   params.in.nDetectionConfigurationLength = nDetectionConfigurationLength;
   params.in.pBuffer = pBuffer;
   params.in.nBufferMaxLength = nBufferMaxLength;
   params.in.phListenerHandle = phListenerHandle;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderDriverRegister,
                &params, sizeof(tMessage_in_PReaderDriverRegister),
                params.in.pBuffer, params.in.nBufferMaxLength,
                null, 0,
                sizeof(tMessage_out_PReaderDriverRegister));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverRegister: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverRegister(
           tContext* pContext,
           tMessage_in_PReaderDriverRegister* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunctionEvent(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferMaxLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A | P_SYNC_BUFFER_FLAG_L ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->phListenerHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phListenerHandle, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PReaderDriverRegister*)pParams)->value = PReaderDriverRegister(pContext, null, pDriverCC, pParams->nPriority, pParams->nRequestedProtocolsBF, pParams->nDetectionConfigurationLength, pParams->pBuffer, pParams->nBufferMaxLength, pParams->phListenerHandle);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PReaderDriverRegister*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PReaderDriverSetWorkPerformedAndClose params;
   W_ERROR nError;

   params.in.hDriverListener = hDriverListener;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderDriverSetWorkPerformedAndClose,
                &params, sizeof(tMessage_in_PReaderDriverSetWorkPerformedAndClose),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PReaderDriverSetWorkPerformedAndClose));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverSetWorkPerformedAndClose: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverSetWorkPerformedAndClose(
           tContext* pContext,
           tMessage_in_PReaderDriverSetWorkPerformedAndClose* pParams)
{
   ((tMessage_out_PReaderDriverSetWorkPerformedAndClose*)pParams)->value = PReaderDriverSetWorkPerformedAndClose(pContext, pParams->hDriverListener);
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
   tMessage_in_out_PReaderDriverWorkPerformed params;
   W_ERROR nError;

   params.in.hConnection = hConnection;
   params.in.bGiveToNextListener = bGiveToNextListener;
   params.in.bCardApplicationMatch = bCardApplicationMatch;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderDriverWorkPerformed,
                &params, sizeof(tMessage_in_PReaderDriverWorkPerformed),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PReaderDriverWorkPerformed));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverWorkPerformed: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderDriverWorkPerformed(
           tContext* pContext,
           tMessage_in_PReaderDriverWorkPerformed* pParams)
{
   ((tMessage_out_PReaderDriverWorkPerformed*)pParams)->value = PReaderDriverWorkPerformed(pContext, pParams->hConnection, pParams->bGiveToNextListener, pParams->bCardApplicationMatch);
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
   tMessage_in_out_PReaderErrorEventRegister params;
   W_ERROR nError;

   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.nEventType = nEventType;
   params.in.bCardDetectionRequested = bCardDetectionRequested;
   params.in.phRegistryHandle = phRegistryHandle;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderErrorEventRegister,
                &params, sizeof(tMessage_in_PReaderErrorEventRegister),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PReaderErrorEventRegister));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderErrorEventRegister: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderErrorEventRegister(
           tContext* pContext,
           tMessage_in_PReaderErrorEventRegister* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phRegistryHandle = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phRegistryHandle, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PReaderErrorEventRegister*)pParams)->value = PReaderErrorEventRegister(pContext, null, pDriverCC, pParams->nEventType, pParams->bCardDetectionRequested, pParams->phRegistryHandle);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PReaderErrorEventRegister*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PReaderGetPulsePeriod params;
   W_ERROR nError;

   params.in.pnTimeout = pnTimeout;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderGetPulsePeriod,
                &params, sizeof(tMessage_in_PReaderGetPulsePeriod),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PReaderGetPulsePeriod));
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderGetPulsePeriod: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderGetPulsePeriod(
           tContext* pContext,
           tMessage_in_PReaderGetPulsePeriod* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pnTimeout = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnTimeout, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PReaderGetPulsePeriod*)pParams)->value = PReaderGetPulsePeriod(pContext, pParams->pnTimeout);
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
   tMessage_in_out_PReaderSetPulsePeriodDriver params;
   W_ERROR nError;

   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nPulsePeriod = nPulsePeriod;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PReaderSetPulsePeriodDriver,
                &params, sizeof(tMessage_in_PReaderSetPulsePeriodDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PReaderSetPulsePeriodDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PReaderSetPulsePeriodDriver(
           tContext* pContext,
           tMessage_in_PReaderSetPulsePeriodDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PReaderSetPulsePeriodDriver(pContext, null, pDriverCC, pParams->nPulsePeriod);
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
   tMessage_in_out_PRoutingTableApplyDriver params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PRoutingTableApplyDriver,
                &params, sizeof(tMessage_in_PRoutingTableApplyDriver),
                params.in.pBuffer, params.in.nBufferLength,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PRoutingTableApplyDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableApplyDriver(
           tContext* pContext,
           tMessage_in_PRoutingTableApplyDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PRoutingTableApplyDriver(pContext, pParams->pBuffer, pParams->nBufferLength, null, pDriverCC);
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
   tMessage_in_out_PRoutingTableGetConfigDriver params;
   W_ERROR nError;


   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PRoutingTableGetConfigDriver,
                &params, 0,
                null, 0,
                null, 0,
                sizeof(tMessage_out_PRoutingTableGetConfigDriver));
   if(nError != W_SUCCESS)
   {
      PDebugError("PRoutingTableGetConfigDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableGetConfigDriver(
           tContext* pContext,
           void* pParams)
{
   ((tMessage_out_PRoutingTableGetConfigDriver*)pParams)->value = PRoutingTableGetConfigDriver(pContext);
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
   tMessage_in_out_PRoutingTableReadDriver params;
   W_ERROR nError;

   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PRoutingTableReadDriver,
                &params, sizeof(tMessage_in_PRoutingTableReadDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PRoutingTableReadDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableReadDriver(
           tContext* pContext,
           tMessage_in_PRoutingTableReadDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   PRoutingTableReadDriver(pContext, pParams->pBuffer, pParams->nBufferLength, null, pDriverCC);
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
   tMessage_in_out_PRoutingTableSetConfigDriver params;
   W_ERROR nError;

   params.in.nConfig = nConfig;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PRoutingTableSetConfigDriver,
                &params, sizeof(tMessage_in_PRoutingTableSetConfigDriver),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PRoutingTableSetConfigDriver: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PRoutingTableSetConfigDriver(
           tContext* pContext,
           tMessage_in_PRoutingTableSetConfigDriver* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PRoutingTableSetConfigDriver(pContext, pParams->nConfig, null, pDriverCC);
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
   tMessage_in_out_PSecurityManagerDriverAuthenticate params;
   W_ERROR nError;

   params.in.pApplicationDataBuffer = pApplicationDataBuffer;
   params.in.nApplicationDataBufferLength = nApplicationDataBufferLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSecurityManagerDriverAuthenticate,
                &params, sizeof(tMessage_in_PSecurityManagerDriverAuthenticate),
                params.in.pApplicationDataBuffer, params.in.nApplicationDataBufferLength,
                null, 0,
                sizeof(tMessage_out_PSecurityManagerDriverAuthenticate));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSecurityManagerDriverAuthenticate: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSecurityManagerDriverAuthenticate(
           tContext* pContext,
           tMessage_in_PSecurityManagerDriverAuthenticate* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pApplicationDataBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pApplicationDataBuffer, pParams->nApplicationDataBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSecurityManagerDriverAuthenticate*)pParams)->value = PSecurityManagerDriverAuthenticate(pContext, pParams->pApplicationDataBuffer, pParams->nApplicationDataBufferLength);
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
   tMessage_in_out_PSeDriver7816SmCloseChannel params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.nChannelReference = nChannelReference;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSeDriver7816SmCloseChannel,
                &params, sizeof(tMessage_in_PSeDriver7816SmCloseChannel),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSeDriver7816SmCloseChannel));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmCloseChannel: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmCloseChannel(
           tContext* pContext,
           tMessage_in_PSeDriver7816SmCloseChannel* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   ((tMessage_out_PSeDriver7816SmCloseChannel*)pParams)->value = PSeDriver7816SmCloseChannel(pContext, pParams->hDriverConnection, pParams->nChannelReference, null, pDriverCC);
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
   tMessage_in_out_PSeDriver7816SmExchangeApdu params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.nChannelReference = nChannelReference;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pSendApduBuffer = pSendApduBuffer;
   params.in.nSendApduBufferLength = nSendApduBufferLength;
   params.in.pReceivedApduBuffer = pReceivedApduBuffer;
   params.in.nReceivedApduBufferMaxLength = nReceivedApduBufferMaxLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSeDriver7816SmExchangeApdu,
                &params, sizeof(tMessage_in_PSeDriver7816SmExchangeApdu),
                params.in.pSendApduBuffer, params.in.nSendApduBufferLength,
                null, 0,
                sizeof(tMessage_out_PSeDriver7816SmExchangeApdu));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmExchangeApdu: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmExchangeApdu(
           tContext* pContext,
           tMessage_in_PSeDriver7816SmExchangeApdu* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pSendApduBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pSendApduBuffer, pParams->nSendApduBufferLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pReceivedApduBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReceivedApduBuffer, pParams->nReceivedApduBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSeDriver7816SmExchangeApdu*)pParams)->value = PSeDriver7816SmExchangeApdu(pContext, pParams->hDriverConnection, pParams->nChannelReference, null, pDriverCC, pParams->pSendApduBuffer, pParams->nSendApduBufferLength, pParams->pReceivedApduBuffer, pParams->nReceivedApduBufferMaxLength);
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
   tMessage_in_out_PSeDriver7816SmGetData params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.nChannelReference = nChannelReference;
   params.in.nType = nType;
   params.in.pBuffer = pBuffer;
   params.in.nBufferMaxLength = nBufferMaxLength;
   params.in.pnActualLength = pnActualLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSeDriver7816SmGetData,
                &params, sizeof(tMessage_in_PSeDriver7816SmGetData),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSeDriver7816SmGetData));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmGetData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmGetData(
           tContext* pContext,
           tMessage_in_PSeDriver7816SmGetData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferMaxLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pnActualLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnActualLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSeDriver7816SmGetData*)pParams)->value = PSeDriver7816SmGetData(pContext, pParams->hDriverConnection, pParams->nChannelReference, pParams->nType, pParams->pBuffer, pParams->nBufferMaxLength, pParams->pnActualLength);
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
   tMessage_in_out_PSeDriver7816SmOpenChannel params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.nType = nType;
   params.in.pAID = pAID;
   params.in.nAIDLength = nAIDLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSeDriver7816SmOpenChannel,
                &params, sizeof(tMessage_in_PSeDriver7816SmOpenChannel),
                params.in.pAID, params.in.nAIDLength,
                null, 0,
                sizeof(tMessage_out_PSeDriver7816SmOpenChannel));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSeDriver7816SmOpenChannel: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSeDriver7816SmOpenChannel(
           tContext* pContext,
           tMessage_in_PSeDriver7816SmOpenChannel* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pAID = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pAID, pParams->nAIDLength, ( P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSeDriver7816SmOpenChannel*)pParams)->value = PSeDriver7816SmOpenChannel(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->nType, pParams->pAID, pParams->nAIDLength);
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
   tMessage_in_out_PSEDriverActivateSwpLine params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEDriverActivateSwpLine,
                &params, sizeof(tMessage_in_PSEDriverActivateSwpLine),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEDriverActivateSwpLine));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverActivateSwpLine: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverActivateSwpLine(
           tContext* pContext,
           tMessage_in_PSEDriverActivateSwpLine* pParams)
{
   ((tMessage_out_PSEDriverActivateSwpLine*)pParams)->value = PSEDriverActivateSwpLine(pContext, pParams->nSlotIdentifier);
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
   tMessage_in_out_PSEDriverGetAtr params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.pAtrBuffer = pAtrBuffer;
   params.in.nAtrBufferLength = nAtrBufferLength;
   params.in.pnAtrLength = pnAtrLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEDriverGetAtr,
                &params, sizeof(tMessage_in_PSEDriverGetAtr),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEDriverGetAtr));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverGetAtr: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverGetAtr(
           tContext* pContext,
           tMessage_in_PSEDriverGetAtr* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pAtrBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pAtrBuffer, pParams->nAtrBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pnAtrLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnAtrLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEDriverGetAtr*)pParams)->value = PSEDriverGetAtr(pContext, pParams->hDriverConnection, pParams->pAtrBuffer, pParams->nAtrBufferLength, pParams->pnAtrLength);
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
   tMessage_in_out_PSEDriverGetInfo params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pSEInfo = pSEInfo;
   params.in.nSize = nSize;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEDriverGetInfo,
                &params, sizeof(tMessage_in_PSEDriverGetInfo),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEDriverGetInfo));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverGetInfo: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverGetInfo(
           tContext* pContext,
           tMessage_in_PSEDriverGetInfo* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pSEInfo = (tWSEInfoEx *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pSEInfo, pParams->nSize, ( P_SYNC_BUFFER_FLAG_O ))) == (tWSEInfoEx *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEDriverGetInfo*)pParams)->value = PSEDriverGetInfo(pContext, pParams->nSlotIdentifier, pParams->pSEInfo, pParams->nSize);
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
   tMessage_in_out_PSEDriverGetStatus params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEDriverGetStatus,
                &params, sizeof(tMessage_in_PSEDriverGetStatus),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PSEDriverGetStatus: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverGetStatus(
           tContext* pContext,
           tMessage_in_PSEDriverGetStatus* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PSEDriverGetStatus(pContext, pParams->nSlotIdentifier, null, pDriverCC);
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
   tMessage_in_out_PSEDriverImpersonateAndCheckAidAccess params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pAIDBuffer = pAIDBuffer;
   params.in.nAIDLength = nAIDLength;
   params.in.pImpersonationDataBuffer = pImpersonationDataBuffer;
   params.in.nImpersonationDataBufferLength = nImpersonationDataBufferLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEDriverImpersonateAndCheckAidAccess,
                &params, sizeof(tMessage_in_PSEDriverImpersonateAndCheckAidAccess),
                params.in.pAIDBuffer, params.in.nAIDLength,
                params.in.pImpersonationDataBuffer, params.in.nImpersonationDataBufferLength,
                sizeof(tMessage_out_PSEDriverImpersonateAndCheckAidAccess));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverImpersonateAndCheckAidAccess: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverImpersonateAndCheckAidAccess(
           tContext* pContext,
           tMessage_in_PSEDriverImpersonateAndCheckAidAccess* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pAIDBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pAIDBuffer, pParams->nAIDLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pImpersonationDataBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pImpersonationDataBuffer, pParams->nImpersonationDataBufferLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEDriverImpersonateAndCheckAidAccess*)pParams)->value = PSEDriverImpersonateAndCheckAidAccess(pContext, pParams->nSlotIdentifier, pParams->pAIDBuffer, pParams->nAIDLength, pParams->pImpersonationDataBuffer, pParams->nImpersonationDataBufferLength);
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
   tMessage_in_out_PSEDriverOpenConnection params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.bForce = bForce;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEDriverOpenConnection,
                &params, sizeof(tMessage_in_PSEDriverOpenConnection),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PSEDriverOpenConnection: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverOpenConnection(
           tContext* pContext,
           tMessage_in_PSEDriverOpenConnection* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PSEDriverOpenConnection(pContext, pParams->nSlotIdentifier, pParams->bForce, null, pDriverCC);
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
   tMessage_in_out_PSEDriverSetPolicy params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.nStorageType = nStorageType;
   params.in.nProtocols = nProtocols;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEDriverSetPolicy,
                &params, sizeof(tMessage_in_PSEDriverSetPolicy),
                null, 0,
                null, 0,
                0);
   if(nError != W_SUCCESS)
   {
      PContextSetLastIoctlError(pContext, nError);
      PDebugError("PSEDriverSetPolicy: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEDriverSetPolicy(
           tContext* pContext,
           tMessage_in_PSEDriverSetPolicy* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   PSEDriverSetPolicy(pContext, pParams->nSlotIdentifier, pParams->nStorageType, pParams->nProtocols, null, pDriverCC);
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
   tMessage_in_out_PSEGetConnectivityEventParameter params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pDataBuffer = pDataBuffer;
   params.in.nBufferLength = nBufferLength;
   params.in.pnActualDataLength = pnActualDataLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEGetConnectivityEventParameter,
                &params, sizeof(tMessage_in_PSEGetConnectivityEventParameter),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEGetConnectivityEventParameter));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEGetConnectivityEventParameter: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEGetConnectivityEventParameter(
           tContext* pContext,
           tMessage_in_PSEGetConnectivityEventParameter* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pDataBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pDataBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pnActualDataLength = (uint32_t *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->pnActualDataLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (uint32_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEGetConnectivityEventParameter*)pParams)->value = PSEGetConnectivityEventParameter(pContext, pParams->nSlotIdentifier, pParams->pDataBuffer, pParams->nBufferLength, pParams->pnActualDataLength);
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
   tMessage_in_out_PSEGetTransactionAID params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pBuffer = pBuffer;
   params.in.nBufferLength = nBufferLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEGetTransactionAID,
                &params, sizeof(tMessage_in_PSEGetTransactionAID),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEGetTransactionAID));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEGetTransactionAID: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (uint32_t)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEGetTransactionAID(
           tContext* pContext,
           tMessage_in_PSEGetTransactionAID* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pBuffer, pParams->nBufferLength, ( P_SYNC_BUFFER_FLAG_O ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEGetTransactionAID*)pParams)->value = PSEGetTransactionAID(pContext, pParams->nSlotIdentifier, pParams->pBuffer, pParams->nBufferLength);
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
   tMessage_in_out_PSEMonitorConnectivityEvent params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEMonitorConnectivityEvent,
                &params, sizeof(tMessage_in_PSEMonitorConnectivityEvent),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEMonitorConnectivityEvent));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEMonitorConnectivityEvent: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEMonitorConnectivityEvent(
           tContext* pContext,
           tMessage_in_PSEMonitorConnectivityEvent* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEMonitorConnectivityEvent*)pParams)->value = PSEMonitorConnectivityEvent(pContext, pParams->nSlotIdentifier, null, pDriverCC, pParams->phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PSEMonitorConnectivityEvent*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PSEMonitorEndOfTransaction params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEMonitorEndOfTransaction,
                &params, sizeof(tMessage_in_PSEMonitorEndOfTransaction),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEMonitorEndOfTransaction));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEMonitorEndOfTransaction: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEMonitorEndOfTransaction(
           tContext* pContext,
           tMessage_in_PSEMonitorEndOfTransaction* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEMonitorEndOfTransaction*)pParams)->value = PSEMonitorEndOfTransaction(pContext, pParams->nSlotIdentifier, null, pDriverCC, pParams->phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PSEMonitorEndOfTransaction*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PSEMonitorHotPlugEvents params;
   W_ERROR nError;

   params.in.nSlotIdentifier = nSlotIdentifier;
   params.in.pHandler = pHandler;
   params.in.pHandlerParameter = pHandlerParameter;
   params.in.phEventRegistry = phEventRegistry;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PSEMonitorHotPlugEvents,
                &params, sizeof(tMessage_in_PSEMonitorHotPlugEvents),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PSEMonitorHotPlugEvents));
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEMonitorHotPlugEvents: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PSEMonitorHotPlugEvents(
           tContext* pContext,
           tMessage_in_PSEMonitorHotPlugEvents* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCExternalEvent(pContext, (tDFCCallback*)(pParams->pHandler), pParams->pHandlerParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->phEventRegistry = (W_HANDLE *)PDFCDriverRegisterUserWordBuffer(
      pDriverCC, pParams->phEventRegistry, ( P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_W ))) == (W_HANDLE *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PSEMonitorHotPlugEvents*)pParams)->value = PSEMonitorHotPlugEvents(pContext, pParams->nSlotIdentifier, null, pDriverCC, pParams->phEventRegistry);
   PDFCDriverSynchronizeUserBuffer(pDriverCC);
   if(((tMessage_out_PSEMonitorHotPlugEvents*)pParams)->value != W_SUCCESS)
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
   tMessage_in_out_PType1ChipDriverExchangeData params;
   W_ERROR nError;

   params.in.hDriverConnection = hDriverConnection;
   params.in.pCallback = pCallback;
   params.in.pCallbackParameter = pCallbackParameter;
   params.in.pReaderToCardBuffer = pReaderToCardBuffer;
   params.in.nReaderToCardBufferLength = nReaderToCardBufferLength;
   params.in.pCardToReaderBuffer = pCardToReaderBuffer;
   params.in.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;

   PContextSetLastIoctlError(pContext, W_SUCCESS);
   nError = PDFCClientCallFunction(
                pContext, P_Idenfier_PType1ChipDriverExchangeData,
                &params, sizeof(tMessage_in_PType1ChipDriverExchangeData),
                params.in.pReaderToCardBuffer, params.in.nReaderToCardBufferLength,
                null, 0,
                sizeof(tMessage_out_PType1ChipDriverExchangeData));
   if(nError != W_SUCCESS)
   {
      PDebugError("PType1ChipDriverExchangeData: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      PContextSetLastIoctlError(pContext, nError);
      return (W_HANDLE)0;
   }
   return params.out.value;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

static W_ERROR static_PType1ChipDriverExchangeData(
           tContext* pContext,
           tMessage_in_PType1ChipDriverExchangeData* pParams)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCCFunction(pContext, (tDFCCallback*)(pParams->pCallback), pParams->pCallbackParameter);
   if(pDriverCC == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }
   if((pParams->pReaderToCardBuffer = (const uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, ( P_SYNC_BUFFER_FLAG_I ))) == (const uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   if((pParams->pCardToReaderBuffer = (uint8_t *)PDFCDriverRegisterUserBuffer(
      pDriverCC, (void*)pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength, ( P_SYNC_BUFFER_FLAG_2 | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A ))) == (uint8_t *)(uintptr_t)1)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   ((tMessage_out_PType1ChipDriverExchangeData*)pParams)->value = PType1ChipDriverExchangeData(pContext, pParams->hDriverConnection, null, pDriverCC, pParams->pReaderToCardBuffer, pParams->nReaderToCardBufferLength, pParams->pCardToReaderBuffer, pParams->nCardToReaderBufferMaxLength);
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

#endif /* #ifdef P_CONFIG_CLIENT_SERVER */


/* End of file */
