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
   Contains the DFC queue implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( DFC )

#include "wme_context.h"

/* The variable P_DFC_QUEUE_INITIAL_SIZE defines the initial size of the DFC queue in DFC entries.  */
#define P_DFC_QUEUE_INITIAL_SIZE  32

/* The variable P_DFC_QUEUE_DELTA defines he increment of the DFC queue in DFC entries. */
#define P_DFC_QUEUE_DELTA  16

/** The structure of a DFC element */
typedef struct __tDFCElement
{
   uint32_t nFlags;  /* The flags, including the number of parameters */
   void* pType; /* THe type of the DFC */
   tDFCCallback* pFunction;  /* The function pointer */
   void* pParam1;  /* The parameters ... */
   void* pParam2;
   uint32_t nParam3;
   uint32_t nParam4;
   uint32_t nParam5;
   uint32_t nParam6;

} tDFCElement;

/* The call context flag */
#define P_DFC_FLAG_USED                0x00001000
#define P_DFC_FLAG_CALLBACK            0x00002000
#define P_DFC_FLAG_EXTERNAL            0x00004000
#define P_DFC_FLAG_EVENT_HANDLER       0x00008000
#define P_DFC_FLAG_EVENT_HANDLER_USED  0x00010000

/* The type for the callback function */
typedef void tDFCCallback0( tContext* );
typedef void tDFCCallback1( tContext*, void* );
typedef void tDFCCallback2( tContext*, void*, void* );
typedef void tDFCCallback3( tContext*, void*, void*, uint32_t nParam3 );
typedef void tDFCCallback4( tContext*, void*, void*, uint32_t nParam3, uint32_t nParam4 );
typedef void tDFCCallback5( tContext*, void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5 );
typedef void tDFCCallback6( tContext*, void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5, uint32_t nParam6 );
typedef void tDFCCallbackExt1( void* );
typedef void tDFCCallbackExt2( void*, void* );
typedef void tDFCCallbackExt3( void*, void*, uint32_t nParam3 );
typedef void tDFCCallbackExt4( void*, void*, uint32_t nParam3, uint32_t nParam4 );
typedef void tDFCCallbackExt5( void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5 );
typedef void tDFCCallbackExt6( void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5, uint32_t nParam6 );

/* See header file */
bool_t PDFCCreate(
         tDFCQueue* pDFCQueue )
{
   tDFCElement* pDFCElementList;

   CMemoryFill(pDFCQueue, 0, sizeof(tDFCQueue));

   pDFCElementList = (tDFCElement*)CMemoryAlloc(P_DFC_QUEUE_INITIAL_SIZE * sizeof(tDFCElement));
   if(pDFCElementList == null)
   {
      PDebugError("PDFCCreate: Cannot allocate the DFC queue");
      return W_FALSE;
   }

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_INCLUDE_JAVA_API))

   pDFCQueue->pExternalList = (tDFCElement*)CMemoryAlloc(P_DFC_QUEUE_INITIAL_SIZE * sizeof(tDFCElement));
   if(pDFCQueue->pExternalList == null)
   {
      PDebugError("PDFCCreate: Cannot allocate the external DFC queue");
      CMemoryFree(pDFCElementList);
      return W_FALSE;
   }

   pDFCQueue->nNextToCall = 0;
   pDFCQueue->nNextToAdd = 0;
   pDFCQueue->nElementNumber = 0;

#endif /* (P_CONFIG_USER || P_CONFIG_MONOLITHIC) && P_INCLUDE_JAVA_API */

   pDFCQueue->pDFCElementList = pDFCElementList;
   pDFCQueue->nDFCQueueSize = P_DFC_QUEUE_INITIAL_SIZE;

   return W_TRUE;
}

/* See header file */
void PDFCDestroy(
         tDFCQueue* pDFCQueue )
{
   if(pDFCQueue != null)
   {
      CMemoryFree(pDFCQueue->pDFCElementList);

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_INCLUDE_JAVA_API))
      CMemoryFree(pDFCQueue->pExternalList);
#endif /* (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_INCLUDE_JAVA_API)) */

      CMemoryFill(pDFCQueue, 0, sizeof(tDFCQueue));
   }
}

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_INCLUDE_JAVA_API))

static void static_PDFCPushJNICallback(
         tContext* pContext,
         tDFCElement* pDFC);

#endif /* (P_CONFIG_USER || P_CONFIG_MONOLITHIC) && P_INCLUDE_JAVA_API */

static void static_PDFCPerformCall(
         tContext* pContext,
         tDFCElement* pDFC)
{
   if((pDFC->nFlags & P_DFC_FLAG_EXTERNAL) != 0)
   {
#ifdef P_TRACE_ACTIVE
      switch(pDFC->nFlags & 0x0000000F)
      {
      case 1:
         PDebugTrace("Calling %08X callback-%p( %p )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1);
         break;
      case 2:
         PDebugTrace("Calling %08X callback-%p( %p, %p )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2);
         break;
      case 3:
         PDebugTrace("Calling %08X callback-%p( %p, %p, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3);
         break;
      case 4:
         PDebugTrace("Calling %08X callback-%p( %p, %p, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4);
         break;
      case 5:
         PDebugTrace("Calling %08X callback-%p( %p, %p, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5);
         break;
      case 6:
         PDebugTrace("Calling %08X callback-%p( %p, %p, 0x%08X, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
         break;
      default:
         PDebugError("static_PDFCPerformCall: Invalid flags");
         CDebugAssert(W_FALSE);
         break;
      }
#endif /* #ifdef P_TRACE_ACTIVE */

#if (P_BUILD_CONFIG == P_CONFIG_USER)
      PContextSetIsInCallbackFlag(pContext, W_TRUE);
#endif /* P_CONFIG_USER */


#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_SYNCHRONOUS_FUNCTION_DEBUG))
      PContextSetCurrentThreadId(pContext, W_TRUE);
#endif /* (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_SYNCHRONOUS_FUNCTION_DEBUG)) */

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_INCLUDE_JAVA_API))

      static_PDFCPushJNICallback(pContext, pDFC);

#else /* (P_CONFIG_USER || P_CONFIG_MONOLITHIC) && P_INCLUDE_JAVA_API */

      /* Release the protection of the context */
      PContextReleaseLock(pContext);

      switch(pDFC->nFlags & 0x0000000F)
      {
      case 1:
         ((tDFCCallbackExt1*)(pDFC->pFunction))(pDFC->pParam1);
         break;
      case 2:
         ((tDFCCallbackExt2*)(pDFC->pFunction))(pDFC->pParam1, pDFC->pParam2);
         break;
      case 3:
         ((tDFCCallbackExt3*)(pDFC->pFunction))(pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3);
         break;
      case 4:
         ((tDFCCallbackExt4*)(pDFC->pFunction))(pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3, pDFC->nParam4);
         break;
      case 5:
         ((tDFCCallbackExt5*)(pDFC->pFunction))(pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3, pDFC->nParam4, pDFC->nParam5);
         break;
      case 6:
         ((tDFCCallbackExt6*)(pDFC->pFunction))(pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
         break;
      default:
         break;
      }

#if (P_BUILD_CONFIG == P_CONFIG_USER)
      /* Check if the context is still alive */
      if(PContextIsDead(pContext))
      {
         return;
      }
#endif /* P_CONFIG_USER */

      /* Lock the protection of the context */
      PContextLock(pContext);

#endif /* (P_CONFIG_USER || P_CONFIG_MONOLITHIC) && P_INCLUDE_JAVA_API */

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_SYNCHRONOUS_FUNCTION_DEBUG))
      PContextSetCurrentThreadId(pContext, W_FALSE);
#endif /* (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_SYNCHRONOUS_FUNCTION_DEBUG)) */

#if (P_BUILD_CONFIG == P_CONFIG_USER)
      PContextSetIsInCallbackFlag(pContext, W_FALSE);
#endif /* P_CONFIG_USER */
   }
   else
   {
      switch(pDFC->nFlags & 0x0000000F)
      {
      case 0:
         PDebugTrace("Calling %08X callback-%p( pContext )", pDFC->nFlags, pDFC->pFunction );
         ((tDFCCallback0*)(pDFC->pFunction))(pContext);
         break;
      case 1:
         PDebugTrace("Calling %08X callback-%p( pContext, %p )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1);
         ((tDFCCallback1*)(pDFC->pFunction))(pContext, pDFC->pParam1);
         break;
      case 2:
         PDebugTrace("Calling %08X callback-%p( pContext, %p, %p )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2);
         ((tDFCCallback2*)(pDFC->pFunction))(pContext, pDFC->pParam1, pDFC->pParam2);
         break;
      case 3:
         PDebugTrace("Calling %08X callback-%p( pContext, %p, %p, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3);
         ((tDFCCallback3*)(pDFC->pFunction))(pContext, pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3);
         break;
      case 4:
         PDebugTrace("Calling %08X callback-%p( pContext, %p, %p, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4);
         ((tDFCCallback4*)(pDFC->pFunction))(pContext, pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3, pDFC->nParam4);
         break;
      case 5:
         PDebugTrace("Calling %08X callback-%p( pContext, %p, %p, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5);
         ((tDFCCallback5*)(pDFC->pFunction))(pContext, pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3, pDFC->nParam4, pDFC->nParam5);
         break;
      case 6:
         PDebugTrace("Calling %08X callback-%p( pContext, %p, %p, 0x%08X, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
            pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
         ((tDFCCallback6*)(pDFC->pFunction))(pContext, pDFC->pParam1, pDFC->pParam2,
            pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
         break;
      default:
         PDebugError("static_PDFCPerformCall: Invalid flags");
         CDebugAssert(W_FALSE);
         break;
      }
   }
}

/* See header file */
bool_t PDFCPump(
         tContext* pContext )
{
   tDFCQueue* pDFCQueue = PContextGetDFCQueue(pContext);
   bool_t bSomeCall = W_FALSE;

   CDebugAssert( pDFCQueue != null );

   while( pDFCQueue->nDFCNumber != 0 )
   {
      tDFCElement* pDFC = &pDFCQueue->pDFCElementList[pDFCQueue->nFirstDFC++];
      bSomeCall = W_TRUE;

      if(pDFCQueue->nFirstDFC >= pDFCQueue->nDFCQueueSize)
      {
         pDFCQueue->nFirstDFC = 0;
      }
      pDFCQueue->nDFCNumber--;

      if(pDFC->pFunction != null)
      {
         static_PDFCPerformCall(pContext, pDFC);

#if (P_BUILD_CONFIG == P_CONFIG_USER)
         /* Check if the context is still alive */
         if(PContextIsDead(pContext))
         {
            break;
         }
#endif /* P_CONFIG_USER */
      }
   }

   return bSomeCall;
}

/* See header file */
void PDFCFlush(
         tContext* pContext,
         void* pType )
{
   uint32_t nFirstDFC, nDFCNumber;
   tDFCQueue* pDFCQueue = PContextGetDFCQueue(pContext);

   CDebugAssert( pDFCQueue != null );

   nDFCNumber = pDFCQueue->nDFCNumber;
   nFirstDFC = pDFCQueue->nFirstDFC;

   while( nDFCNumber != 0 )
   {
      tDFCElement* pDFC = &pDFCQueue->pDFCElementList[nFirstDFC++];
      if(nFirstDFC >= pDFCQueue->nDFCQueueSize)
      {
         nFirstDFC = 0;
      }
      nDFCNumber--;

      if(pDFC->pType == pType)
      {
         pDFC->pFunction = null;
      }
   }
}

/* See header file */
static tDFCElement* static_PDFCPostInternal(
            tContext* pContext )
{
   uint32_t nIndex;
   tDFCQueue* pDFCQueue = PContextGetDFCQueue(pContext);

   CDebugAssert( pDFCQueue != null );
   CDebugAssert( pDFCQueue->nDFCNumber <= pDFCQueue->nDFCQueueSize );

   if(pDFCQueue->nDFCNumber == pDFCQueue->nDFCQueueSize)
   {
      tDFCElement* pDFCElementList = CMemoryAlloc( (pDFCQueue->nDFCQueueSize + P_DFC_QUEUE_DELTA)*sizeof(tDFCElement));
      if (pDFCElementList != null)
      {
         CMemoryCopy(pDFCElementList, pDFCQueue->pDFCElementList,
             pDFCQueue->nDFCQueueSize * sizeof(tDFCElement));
         CMemoryFree( pDFCQueue->pDFCElementList );
         pDFCQueue->pDFCElementList = pDFCElementList;
      }
      else
      {
         PDebugError(
         "static_PDFCPostInternal: Critial error, cannot increase the size of the DFC queue, the DFC is lost");
         return null;
      }

      if(pDFCQueue->nFirstDFC + pDFCQueue->nDFCNumber > pDFCQueue->nDFCQueueSize)
      {
         uint32_t nRelocation = pDFCQueue->nDFCQueueSize - pDFCQueue->nFirstDFC;

         CMemoryMove(&pDFCElementList[pDFCQueue->nFirstDFC + P_DFC_QUEUE_DELTA],
         &pDFCElementList[pDFCQueue->nFirstDFC], nRelocation * sizeof(tDFCElement));

         pDFCQueue->nFirstDFC += P_DFC_QUEUE_DELTA;
      }

      pDFCQueue->pDFCElementList = pDFCElementList;
      pDFCQueue->nDFCQueueSize += P_DFC_QUEUE_DELTA;
   }

   nIndex = pDFCQueue->nFirstDFC + pDFCQueue->nDFCNumber;
   if(nIndex >= pDFCQueue->nDFCQueueSize)
   {
      nIndex -= pDFCQueue->nDFCQueueSize;
   }

   if(pDFCQueue->nDFCNumber == 0)
   {
      /* Trigger the event pump if needed */
      PContextTriggerEventPump(pContext);
   }

   pDFCQueue->nDFCNumber++;

   return &pDFCQueue->pDFCElementList[nIndex];
}

void PDFCFillCallbackContext(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter,
         tDFCCallbackContext* pCallbackContext )
{
   PDFCFillCallbackContextType(
         pContext,
         pCallbackFunction,
         pCallbackParameter,
         null,
         pCallbackContext );
}

void PDFCFillCallbackContextType(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter,
         void* pType,
         tDFCCallbackContext* pCallbackContext )
{
   if(pCallbackFunction != null)
   {
      pCallbackContext->bIsExternal = W_FALSE;
      pCallbackContext->pFunction = pCallbackFunction;
      pCallbackContext->pParameter = pCallbackParameter;
   }
   else
   {
      if((pCallbackParameter != null) && (((tDFCExternal*)pCallbackParameter)->pFunction != null))
      {
         pCallbackContext->bIsExternal = W_TRUE;
         pCallbackContext->pFunction = ((tDFCExternal*)pCallbackParameter)->pFunction;
         pCallbackContext->pParameter = ((tDFCExternal*)pCallbackParameter)->pParameter;
      }
      else
      {
         pCallbackContext->bIsExternal = W_FALSE;
         pCallbackContext->pFunction = null;
         pCallbackContext->pParameter = null;
      }
   }
   pCallbackContext->pContext = pContext;
   pCallbackContext->pType = pType;
}

void PDFCFlushCall(
            tDFCCallbackContext* pCallbackContext)
{
   if(pCallbackContext != null)
   {
      pCallbackContext->pFunction = null;
   }
}

void PDFCPostInternalContext3(
            tDFCCallbackContext* pCallbackContext,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3)
{
   tContext* pContext = pCallbackContext->pContext;
   tDFCElement* pDFC;

   if(pCallbackContext->pFunction == null)
   {
      return;
   }

   if((pDFC = static_PDFCPostInternal(pContext)) != null)
   {
      if(pCallbackContext->bIsExternal != W_FALSE)
      {
         nFlags |= P_DFC_FLAG_EXTERNAL;
      }

      pDFC->pType = pCallbackContext->pType;
      pDFC->nFlags = nFlags;
      pDFC->pFunction = pCallbackContext->pFunction;
      pDFC->pParam1 = pCallbackContext->pParameter;
      pDFC->pParam2 = pParam2;
      pDFC->nParam3 = nParam3;
      pDFC->nParam4 = 0;
      pDFC->nParam5 = 0;
      pDFC->nParam6 = 0;

      PDebugTrace("Posting %08X callback-%p( %p, %p, 0x%08X  )", pDFC->nFlags, pDFC->pFunction,
               pDFC->pParam1, pDFC->pParam2, pDFC->nParam3);
   }
}

void PDFCPostInternalContext6(
            tDFCCallbackContext* pCallbackContext,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3,
            uint32_t nParam4,
            uint32_t nParam5,
            uint32_t nParam6)
{
   tContext* pContext = pCallbackContext->pContext;
   tDFCElement* pDFC;

   if(pCallbackContext->pFunction == null)
   {
      return;
   }

   if((pDFC = static_PDFCPostInternal(pContext)) != null)
   {
      if(pCallbackContext->bIsExternal != W_FALSE)
      {
         nFlags |= P_DFC_FLAG_EXTERNAL;
      }

      pDFC->nFlags = nFlags;
      pDFC->pType =  pCallbackContext->pType;
      pDFC->pFunction = pCallbackContext->pFunction;
      pDFC->pParam1 = pCallbackContext->pParameter;
      pDFC->pParam2 = pParam2;
      pDFC->nParam3 = nParam3;
      pDFC->nParam4 = nParam4;
      pDFC->nParam5 = nParam5;
      pDFC->nParam6 = nParam6;

      PDebugTrace("Posting %08X callback-%p( %p, %p, 0x%08X, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
               pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
   }
}


#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_DRIVER)
#ifdef P_CONFIG_CLIENT_SERVER

#define P_Identifier_PDFCDriverPumpEvent  P_DRIVER_FUNCTION_COUNT

typedef struct __tMessage_in_PDFCDriverPumpEvent
{
   bool_t bWait;

} tMessage_in_PDFCDriverPumpEvent;

typedef struct __tMessage_out_PDFCDriverPumpEvent
{
   tDFCElement sDFC;
   W_ERROR nError;

} tMessage_out_PDFCDriverPumpEvent;


typedef union __tMessage_in_out_PDFCDriverPumpEvent
{
   tMessage_in_PDFCDriverPumpEvent in;
   tMessage_out_PDFCDriverPumpEvent out;
} tMessage_in_out_PDFCDriverPumpEvent;


#endif /* #ifdef P_CONFIG_CLIENT_SERVER */
#endif /* P_CONFIG_USER || P_CONFIG_DRIVER */


#if (P_BUILD_CONFIG == P_CONFIG_USER)
#ifdef P_CONFIG_CLIENT_SERVER

/* See header file */
W_ERROR PDFCClientCallFunction(
            tContext * pContext,
            uint8_t nCode,
            void* pParamInOut,
            uint32_t nSizeIn,
            const void* pBuffer1,
            uint32_t nBuffer1Length,
            const void* pBuffer2,
            uint32_t nBuffer2Length,
            uint32_t nSizeOut)
{
   void* pInstance = PContextGetUserInstance(pContext);
   W_ERROR nError;

   if ( ((pBuffer1 == null) && (nBuffer1Length != 0)) ||
        ((pBuffer1 != null) && (nBuffer1Length == 0)) ||
        ((pBuffer2 == null) && (nBuffer2Length != 0)) ||
        ((pBuffer2 != null) && (nBuffer2Length == 0)))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   nError = CUserCallFunction(pInstance, nCode, pParamInOut, nSizeIn, pBuffer1, nBuffer1Length, pBuffer2, nBuffer2Length, nSizeOut);
   return (nError);
}

/* See header file */
W_ERROR PDFCDriverPumpEvent(
            tContext * pContext,
            bool_t bWait )
{
   tMessage_in_out_PDFCDriverPumpEvent params;
   void * pUserInstance = PContextGetUserInstance(pContext);

   W_ERROR nError;
   bool_t    bWaitInServer;
   bool_t    bInterrupted;

   if (bWait)
   {
      bInterrupted = CUserWaitForServerEvent(pUserInstance, &bWaitInServer);

      if (bInterrupted != W_FALSE)
      {
         return W_ERROR_WAIT_CANCELLED;
      }

      params.in.bWait = bWaitInServer;
   }
   else
   {
      params.in.bWait = W_FALSE;
   }

   nError = PDFCClientCallFunction(
                pContext, P_Identifier_PDFCDriverPumpEvent,
                &params, sizeof(tMessage_in_PDFCDriverPumpEvent),
                null, 0,
                null, 0,
                sizeof(tMessage_out_PDFCDriverPumpEvent));

   if(nError != W_SUCCESS)
   {
      PDebugError("PDFCDriverPumpEvent: Error %s returned by PDFCClientCallFunction()", PUtilTraceError(nError));
      return nError;
   }

   if(params.out.nError != W_SUCCESS)
   {
      if (params.out.nError != W_ERROR_NO_EVENT)
      {
         PDebugError("PDFCDriverPumpEvent: Error %s returned by remote function", PUtilTraceError(params.out.nError));
      }
      return params.out.nError;
   }

   PContextLockForPump(pContext);
   static_PDFCPerformCall(pContext, &params.out.sDFC);
   PContextReleaseLockForPump(pContext);

   /* Check if the context is still alive */
   if(PContextIsDead(pContext))
   {
      return W_ERROR_BAD_STATE;
   }

   return W_SUCCESS;
}

#endif /* #ifdef P_CONFIG_CLIENT_SERVER */
#endif /* P_CONFIG_USER */


#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_DRIVER)
#ifndef P_CONFIG_CLIENT_SERVER

#define P_Identifier_PDFCDriverPumpEvent  P_DRIVER_FUNCTION_COUNT

typedef struct __tMessage_in_PDFCDriverPumpEvent
{
   bool_t bWait;

} tMessage_in_PDFCDriverPumpEvent;

typedef struct __tMessage_out_PDFCDriverPumpEvent
{
   tDFCElement sDFC;
   W_ERROR nError;

} tMessage_out_PDFCDriverPumpEvent;


typedef union __tMessage_in_out_PDFCDriverPumpEvent
{
   tMessage_in_PDFCDriverPumpEvent in;
   tMessage_out_PDFCDriverPumpEvent out;
} tMessage_in_out_PDFCDriverPumpEvent;

typedef union __tParams_PDFCDriverPumpEvent
{
   bool_t bWait;
   tDFCElement sDFC;
   W_ERROR nError;
} tParams_PDFCDriverPumpEvent;

#endif /* #ifndef P_CONFIG_CLIENT_SERVER */
#endif /* P_CONFIG_USER || P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER)
#ifndef P_CONFIG_CLIENT_SERVER

/* See header file */
W_ERROR PDFCDriverPumpEvent(
            tContext * pContext,
            bool_t bWait )
{
   W_ERROR nError;
   tMessage_in_out_PDFCDriverPumpEvent params;
   void* pInstance = PContextGetUserInstance(pContext);

   params.in.bWait = bWait;

   while((nError = CUserIoctl(pInstance,
       P_Identifier_PDFCDriverPumpEvent,
       &params,
       sizeof(tMessage_in_PDFCDriverPumpEvent),
       sizeof(tMessage_out_PDFCDriverPumpEvent))) == W_ERROR_RETRY) {}
   if(nError != W_SUCCESS)
   {
      PDebugError("PDFCDriverPumpEvent: Error %s returned by CUserIoctl()", PUtilTraceError(nError));
      return nError;
   }
   if(params.out.nError != W_SUCCESS)
   {
      return params.out.nError;
   }

   PContextLockForPump(pContext);
   static_PDFCPerformCall(pContext, &params.out.sDFC);
   PContextReleaseLockForPump(pContext);

   /* Check if the context is still alive */
   if(PContextIsDead(pContext))
   {
      return W_ERROR_BAD_STATE;
   }

   return W_SUCCESS;
}

#endif /* #ifndef P_CONFIG_CLIENT_SERVER */
#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

#ifdef P_CONFIG_CLIENT_SERVER

typedef struct __tDFCDriverSharedBuffer
{
   const tUserIdentity* pUserIdentity;
   void* pUserBufferAddress;
   void* pKernelBuffer;
   uint32_t nBufferLength;
   uint32_t nFlags;

} tDFCDriverSharedBuffer;

/* See header file */
static void* static_PDFCDriverMapUserBuffer(
         tUserInstance* pUserInstance,
         tDFCDriverSharedBuffer* pSharedBuffer,
         void* pUserBuffer,
         uint32_t nBufferSize,
         uint32_t nType )
{
   void* pKernelBuffer = null;


   if (pUserBuffer != null)
   {
      if (nBufferSize != 0)
      {
         CDebugAssert(pUserInstance != null);

         pKernelBuffer = CMemoryAlloc(nBufferSize);
         if(pKernelBuffer == null)
         {
            PDebugError("static_PDFCDriverMapUserBuffer: cannot allocate the server buffer");
            return (void*)(uintptr_t)1;
         }

         if((nType & P_SYNC_BUFFER_FLAG_I) != 0)
         {
            /* Read the buffer here */
            W_ERROR nError = CServerRead(pUserInstance->pUserIdentity, pKernelBuffer, nBufferSize);
            if(nError != W_SUCCESS)
            {
               PDebugError("static_PDFCDriverMapUserBuffer: Error %s returned by CServerRead",
                  PUtilTraceError(nError));
               CMemoryFree(pKernelBuffer);
               return (void*)(uintptr_t)1;
            }
         }

         pSharedBuffer->pUserIdentity = pUserInstance->pUserIdentity;
         pSharedBuffer->pUserBufferAddress = pUserBuffer;
         pSharedBuffer->pKernelBuffer = pKernelBuffer;
         pSharedBuffer->nBufferLength = nBufferSize;
         pSharedBuffer->nFlags = nType;
      }
      else
      {
         PDebugError("static_PDFCDriverMapUserBuffer: pBuffer == null with buffer length != 0");
         return (void*)(uintptr_t)1;
      }
   }
   else
   {
      pSharedBuffer->pUserIdentity = null;
      pSharedBuffer->pUserBufferAddress = null;
      pSharedBuffer->pKernelBuffer = null;
      pSharedBuffer->nBufferLength = 0;
      pSharedBuffer->nFlags = nType;
   }

   return pKernelBuffer;
}


static void static_DFCDriverCopyToUserBuffer(
         tDFCDriverSharedBuffer* pSharedBuffer)
{
   W_ERROR nError;

   CDebugAssert((pSharedBuffer->nFlags & P_SYNC_BUFFER_FLAG_O) != 0);

   if(pSharedBuffer->nBufferLength != 0)
   {

      nError = CServerCopyToClientBuffer(pSharedBuffer->pUserIdentity, pSharedBuffer->pUserBufferAddress, pSharedBuffer->pKernelBuffer, pSharedBuffer->nBufferLength);

      if (nError != W_SUCCESS)
      {
         PDebugError("static_DFCDriverCopyToUserBuffer: Error %s returned by CServerCopyToUserBuffer", PUtilTraceError(nError));
      }
   }
}

static void static_DFCDriverUnmapUserBuffer(
         tDFCDriverSharedBuffer* pSharedBuffer)
{
   if(pSharedBuffer->nFlags != 0)
   {
      if(pSharedBuffer->nBufferLength != 0)
      {
         /* If the buffer is used for output, the content of the user buffer
          * is synchronized with the kernel buffer */
         if((pSharedBuffer->nFlags & P_SYNC_BUFFER_FLAG_O) != 0)
         {
            static_DFCDriverCopyToUserBuffer(pSharedBuffer);
         }

         CMemoryFree(pSharedBuffer->pKernelBuffer);
      }

      CMemoryFill(pSharedBuffer, 0, sizeof(tDFCDriverSharedBuffer));
   }
}

#else /* #ifdef P_CONFIG_CLIENT_SERVER */

typedef struct __tDFCDriverSharedBuffer
{
   void* pUserBufferAddress;
   void* pKernelBuffer;
   uint32_t nBufferLength;
   P_SYNC_BUFFER hBuffer;
   uint32_t nFlags;

} tDFCDriverSharedBuffer;

/* See header file */
static void* static_PDFCDriverMapUserBuffer(
         tUserInstance* pUserInstance,
         tDFCDriverSharedBuffer* pSharedBuffer,
         void* pUserBuffer,
         uint32_t nBufferSize,
         uint32_t nType )
{
   void* pKernelBuffer = null;

   if (pUserBuffer != null)
   {
      if (nBufferSize != 0)
      {
         /* Mask the type flags for the sync function */
         if((pKernelBuffer = CSyncMapUserBuffer(&pSharedBuffer->hBuffer,
            pUserBuffer, nBufferSize,
            nType & (P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A))) != null)
         {
            pSharedBuffer->pUserBufferAddress = pUserBuffer;
            pSharedBuffer->pKernelBuffer = pKernelBuffer;
            pSharedBuffer->nBufferLength = nBufferSize;
            pSharedBuffer->nFlags = nType;
         }
         else
         {
            return (void*)(uintptr_t)1;
         }
      }
      else
      {
         PDebugError("static_PDFCDriverMapUserBuffer: pBuffer == null with buffer length != 0");
         return (void*)(uintptr_t)1;
      }
   }
   else
   {
      pSharedBuffer->pUserBufferAddress = null;
      pSharedBuffer->pKernelBuffer = null;
      pSharedBuffer->nBufferLength = 0;
      pSharedBuffer->nFlags = nType;
   }

   return pKernelBuffer;
}

static void static_DFCDriverCopyToUserBuffer(
         tDFCDriverSharedBuffer* pSharedBuffer)
{
   CSyncCopyToUserBuffer(
      &pSharedBuffer->hBuffer,
      pSharedBuffer->pUserBufferAddress,
      pSharedBuffer->pKernelBuffer,
      pSharedBuffer->nBufferLength);
}

static void static_DFCDriverUnmapUserBuffer(
         tDFCDriverSharedBuffer* pSharedBuffer)
{
   if(pSharedBuffer->nFlags != 0)
   {
      if(pSharedBuffer->nBufferLength != 0)
      {
         /* If the buffer is used for output, the content of the user buffer
          * is synchronized with the kernel buffer */
         if((pSharedBuffer->nFlags & P_SYNC_BUFFER_FLAG_O) != 0)
         {
            static_DFCDriverCopyToUserBuffer(pSharedBuffer);
         }

         CSyncUnmapUserBuffer(
            &pSharedBuffer->hBuffer,
            pSharedBuffer->pUserBufferAddress,
            pSharedBuffer->pKernelBuffer,
            pSharedBuffer->nBufferLength);
      }

      CMemoryFill(pSharedBuffer, 0, sizeof(tDFCDriverSharedBuffer));
   }
}

#endif /* #ifdef P_CONFIG_CLIENT_SERVER */

struct __tDFCDriverCallbackContext
{
   tDFCElement sDFC;

   tUserInstance* pUserInstance;
   struct __tDFCDriverCallbackContext* pNextList;
   struct __tDFCDriverCallbackContext* pNextQueue;

   tDFCDriverSharedBuffer sSharedBuffer1;
   tDFCDriverSharedBuffer sSharedBuffer2;
};

void PDFCDriverStopEventLoop(
            tContext * pContext )
{
   tUserInstance* pUserInstance = PContextGetCurrentUserInstance(pContext);

   if(pUserInstance->bStopLoop == W_FALSE)
   {
      pUserInstance->bStopLoop = W_TRUE;
      CSyncIncrementSemaphore(&pUserInstance->hSemaphore);
   }
}

void PDFCDriverInterruptEventLoop(
            tContext * pContext )
{
   tUserInstance* pUserInstance = PContextGetCurrentUserInstance(pContext);

   pUserInstance->nExternalEventCount++;
   CSyncIncrementSemaphore(&pUserInstance->hSemaphore);
}

W_ERROR PDFCDriverPumpEvent(
            tContext * pContext,
            void* pParams )
{
   tMessage_in_out_PDFCDriverPumpEvent* pParameters = (tMessage_in_out_PDFCDriverPumpEvent*)pParams;
   tUserInstance* pUserInstance = PContextGetCurrentUserInstance(pContext);
   tDFCDriverCC* pDriverCC;
   bool_t bInterrupted;

   /* If Callback Context Queue Empty, wait on Semaphore */
   while((pDriverCC = pUserInstance->pDFCDriverCCQueueFirst) == null)
   {
      if ( (pUserInstance->bStopLoop == W_FALSE) &&
            (pUserInstance->nExternalEventCount == 0) &&
            (pParameters->in.bWait == W_FALSE))
      {

         pParameters->out.nError = W_ERROR_NO_EVENT;
         return W_SUCCESS;
      }

      /* Reset the context user instance */
      PContextSetCurrentUserInstance(
         pContext,
         null );

      pUserInstance->bIsEventPumpWaitingForSemaphore = W_TRUE;

      PContextReleaseLock(pContext);
      bInterrupted = CSyncWaitSemaphore(&pUserInstance->hSemaphore);
      PContextLock(pContext);

      pUserInstance->bIsEventPumpWaitingForSemaphore = W_FALSE;

      if(bInterrupted == W_FALSE)
      {
         pParameters->out.nError = W_ERROR_WAIT_CANCELLED;
         return W_ERROR_RETRY;
      }

      if(pUserInstance->bEventPumpShouldDestroyUserInstance != W_FALSE)
      {
         PDriverCloseInternal(pContext, pUserInstance);
         return W_SUCCESS;
      }

      if(pUserInstance->bStopLoop != W_FALSE)
      {
         pUserInstance->bStopLoop = W_FALSE;
         pParameters->out.nError = W_ERROR_WAIT_CANCELLED;
         return W_SUCCESS;
      }

      if(pUserInstance->nExternalEventCount != 0)
      {
         pUserInstance->nExternalEventCount--;
         pParameters->out.nError = W_ERROR_NO_EVENT;
         return W_SUCCESS;
      }
   }

   /* Here, a new CC should have been received */
   CDebugAssert(pDriverCC != null);

   /* Remove the call from the queue */
   pUserInstance->pDFCDriverCCQueueFirst = pDriverCC->pNextQueue;

   /* Copy the parameters */
   pParameters->out.sDFC = pDriverCC->sDFC;

   /* Synchronize the buffers */
   if((pDriverCC->sSharedBuffer1.nFlags & P_SYNC_BUFFER_FLAG_A) != 0)
   {
      if((pDriverCC->sSharedBuffer1.nFlags & P_SYNC_BUFFER_FLAG_L) == 0)
      {
         static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer1);
      }
      else
      {
         /* The 'L' flag is only possible with event handlers */
         CDebugAssert((pDriverCC->sDFC.nFlags & P_DFC_FLAG_EVENT_HANDLER) != 0);

         if(pDriverCC->sSharedBuffer1.nBufferLength != 0)
         {
            static_DFCDriverCopyToUserBuffer(&pDriverCC->sSharedBuffer1);
         }
      }
   }
   if((pDriverCC->sSharedBuffer2.nFlags & P_SYNC_BUFFER_FLAG_A) != 0)
   {
      if((pDriverCC->sSharedBuffer2.nFlags & P_SYNC_BUFFER_FLAG_L) == 0)
      {
         static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer2);
      }
      else
      {
         /* The 'L' flag is only possible with event handlers */
         CDebugAssert((pDriverCC->sDFC.nFlags & P_DFC_FLAG_EVENT_HANDLER) != 0);

         if(pDriverCC->sSharedBuffer2.nBufferLength != 0)
         {
            static_DFCDriverCopyToUserBuffer(&pDriverCC->sSharedBuffer2);
         }
      }
   }

   /* Recycle the call context */
   if((pDriverCC->sDFC.nFlags & P_DFC_FLAG_EVENT_HANDLER) == 0)
   {
      tDFCDriverCC* pNext = pDriverCC->pNextList;
      CMemoryFill(pDriverCC, 0, sizeof(tDFCDriverCC));
      pDriverCC->pNextList = pNext;
   }
   else
   {
      CDebugAssert((pDriverCC->sDFC.nFlags & P_DFC_FLAG_EVENT_HANDLER_USED) != 0);

      /* Simply reset the usage flag */
      pDriverCC->sDFC.nFlags &= ~P_DFC_FLAG_EVENT_HANDLER_USED;
      pDriverCC->pNextQueue = null;
   }

   pParameters->out.nError = W_SUCCESS;

   return W_SUCCESS;
}

/* See header file */
tDFCDriverCC* PDFCDriverAllocateCC(
         tContext* pContext )
{
   tUserInstance* pUserInstance = PContextGetCurrentUserInstance(pContext);
   tDFCDriverCC* pDriverCC = pUserInstance->pDFCDriverCCListHead;

   while(pDriverCC != null)
   {
      if(pDriverCC->sDFC.nFlags == 0)
      {
         break;
      }
      pDriverCC = pDriverCC->pNextList;
   }

   if(pDriverCC == null)
   {
      pDriverCC = (tDFCDriverCC*)CMemoryAlloc(sizeof(tDFCDriverCC));
      if(pDriverCC == null)
      {
         return null;
      }

      CMemoryFill(pDriverCC, 0, sizeof(tDFCDriverCC));
      pDriverCC->pNextList = pUserInstance->pDFCDriverCCListHead;
      pUserInstance->pDFCDriverCCListHead = pDriverCC;

   }

   pDriverCC->sDFC.nFlags = P_DFC_FLAG_USED;
   pDriverCC->pUserInstance = pUserInstance;

   return pDriverCC;
}

/* See header file */
tDFCDriverCC* PDFCDriverAllocateCCExternal(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC != null)
   {
      pDriverCC->sDFC.nFlags = P_DFC_FLAG_USED | P_DFC_FLAG_EXTERNAL;
      pDriverCC->sDFC.pFunction = pCallbackFunction;
      pDriverCC->sDFC.pParam1 = pCallbackParameter;
   }
   return pDriverCC;
}

/* See header file */
tDFCDriverCC* PDFCDriverAllocateCCFunction(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC != null)
   {
      pDriverCC->sDFC.pFunction = pCallbackFunction;
      pDriverCC->sDFC.pParam1 = pCallbackParameter;
   }
   return pDriverCC;
}

/* See header file */
tDFCDriverCC* PDFCDriverAllocateCCExternalEvent(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC != null)
   {
      pDriverCC->sDFC.nFlags = P_DFC_FLAG_USED | P_DFC_FLAG_EXTERNAL | P_DFC_FLAG_EVENT_HANDLER;
      pDriverCC->sDFC.pFunction = pCallbackFunction;
      pDriverCC->sDFC.pParam1 = pCallbackParameter;
   }
   return pDriverCC;
}

/* See header file */
tDFCDriverCC* PDFCDriverAllocateCCFunctionEvent(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter)
{
   tDFCDriverCC* pDriverCC = PDFCDriverAllocateCC(pContext);
   if(pDriverCC != null)
   {
      pDriverCC->sDFC.nFlags = P_DFC_FLAG_USED | P_DFC_FLAG_EVENT_HANDLER;
      pDriverCC->sDFC.pFunction = pCallbackFunction;
      pDriverCC->sDFC.pParam1 = pCallbackParameter;
   }
   return pDriverCC;
}

/* See header file */
void* PDFCDriverGetCallerBufferAddress(
         tDFCDriverCC* pDriverCC,
         void* pKernelBuffer )
{
   if(pKernelBuffer != null)
   {
      if(pDriverCC->sSharedBuffer1.nFlags != 0)
      {
         if(pDriverCC->sSharedBuffer1.pKernelBuffer == pKernelBuffer)
         {
            return pDriverCC->sSharedBuffer1.pUserBufferAddress;
         }
      }

      if(pDriverCC->sSharedBuffer2.nFlags != 0)
      {
         if(pDriverCC->sSharedBuffer2.pKernelBuffer == pKernelBuffer)
         {
            return pDriverCC->sSharedBuffer2.pUserBufferAddress;
         }
      }
   }

   return null;
}

/* See header file */
void PDFCDriverSetCurrentUserInstance(
         tContext* pContext,
         tDFCDriverCC* pDriverCC )
{
   if ( pDriverCC != null )
   {
      CDebugAssert(PContextGetCurrentUserInstance(pContext) == null);
      PContextSetCurrentUserInstance(
         pContext,
         pDriverCC->pUserInstance );
   }
   else
   {
      CDebugAssert(PContextGetCurrentUserInstance(pContext) != null);
      PContextSetCurrentUserInstance(
         pContext,
         null );
   }
}
/* See header */
bool_t PDFCDriverCheckDriverDFCInUserInstance(
      tUserInstance * pUserInstance,
      tDFCDriverCC* pDriverCC )
{
   tDFCDriverCC * pCurrentDriverCC;

   pCurrentDriverCC = pUserInstance->pDFCDriverCCListHead;

   while (pCurrentDriverCC != null)
   {
      if (pCurrentDriverCC == pDriverCC)
      {
         if (pUserInstance == pDriverCC->pUserInstance)
         {
            return W_TRUE;
         }
         else
         {
            return W_FALSE;
         }
      }

      pCurrentDriverCC = pCurrentDriverCC->pNextList;
   }

   return W_FALSE;
}

/* See header file */
void* PDFCDriverRegisterUserBuffer(
         tDFCDriverCC* pDriverCC,
         void* pUserBuffer,
         uint32_t nBufferSize,
         uint32_t nType )
{
   void* pKernelBuffer = null;

   CDebugAssert(pDriverCC->sSharedBuffer2.nFlags == 0);

   if(pDriverCC->sSharedBuffer1.nFlags == 0)
   {
      CDebugAssert((nType & P_SYNC_BUFFER_FLAG_2) == 0);

      pKernelBuffer = static_PDFCDriverMapUserBuffer(
         pDriverCC->pUserInstance,
         &pDriverCC->sSharedBuffer1, pUserBuffer, nBufferSize, nType );
   }
   else
   {
      CDebugAssert((nType & P_SYNC_BUFFER_FLAG_2) != 0);

      pKernelBuffer = static_PDFCDriverMapUserBuffer(
         pDriverCC->pUserInstance,
         &pDriverCC->sSharedBuffer2, pUserBuffer, nBufferSize, nType );
   }

   if (pKernelBuffer == (void *)(uintptr_t)1)
   {
      /* An error occurred during the map of the buffer,
         free the allocated driver CC since autogen does not perform this cleanup
         this will also unmap the buffers successfully mapped */

      PDFCDriverFreeCC(pDriverCC);
   }

   return pKernelBuffer;
}

/* See header file */
uint32_t* PDFCDriverRegisterUserWordBuffer(
         tDFCDriverCC* pDriverCC,
         void* pUserWord,
         uint32_t nType )
{
   return (uint32_t*)PDFCDriverRegisterUserBuffer(pDriverCC, pUserWord, sizeof(uint32_t), nType);
}

/* See header file */
void PDFCDriverSynchronizeUserBuffer(
         tDFCDriverCC* pDriverCC )
{
   bool_t bKeep = W_FALSE;

   if(pDriverCC->sSharedBuffer1.nFlags != 0)
   {
      if((pDriverCC->sSharedBuffer1.nFlags & P_SYNC_BUFFER_FLAG_A) == 0)
      {
         static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer1);
      }
      else
      {
         bKeep = W_TRUE;
      }
   }
   if(pDriverCC->sSharedBuffer2.nFlags != 0)
   {
      if((pDriverCC->sSharedBuffer2.nFlags & P_SYNC_BUFFER_FLAG_A) == 0)
      {
         static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer2);
      }
      else
      {
         bKeep = W_TRUE;
      }
   }

   /* Recycle the call context */
   if(bKeep == W_FALSE)
   {
      if(pDriverCC->sDFC.pFunction == null)
      {
         tDFCDriverCC* pNext = pDriverCC->pNextList;
         CMemoryFill(pDriverCC, 0, sizeof(tDFCDriverCC));
         pDriverCC->pNextList = pNext;
      }
   }
}

/* See header file */
void PDFCDriverFreeCC(
         tDFCDriverCC* pDriverCC )
{
   tDFCDriverCC* pNext;

   static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer1);

   static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer2);

   pNext = pDriverCC->pNextList;
   CMemoryFill(pDriverCC, 0, sizeof(tDFCDriverCC));
   pDriverCC->pNextList = pNext;
}

/* See header file */
void PDFCDriverFlushClient(
         tContext* pContext )
{
   tUserInstance* pUserInstance = PContextGetCurrentUserInstance(pContext);
   tDFCDriverCC* pDriverCC = pUserInstance->pDFCDriverCCListHead;

   pUserInstance->pDFCDriverCCQueueFirst = null;
   pUserInstance->pDFCDriverCCListHead = null;

   while(pDriverCC != null)
   {
      tDFCDriverCC* pNextDriverCC = pDriverCC->pNextList;

      static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer1);
      static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer2);

      CMemoryFree(pDriverCC);
      pDriverCC = pNextDriverCC;
   }
}

void PDFCDriverInternalFlushCall(
            tContext * pContext,
            tDFCDriverCC* pDriverCC)
{
   if (PContextCheckDriverDFC(pContext, pDriverCC) != W_FALSE)
   {
      if(pDriverCC->sDFC.nFlags != 0)
      {
         tUserInstance* pUserInstance = pDriverCC->pUserInstance;
         tDFCDriverCC* pOtherDriverCC;

         CDebugAssert(pUserInstance != null);

         pOtherDriverCC = pUserInstance->pDFCDriverCCQueueFirst;

         /* Remove the call from the queue */
         if(pOtherDriverCC == pDriverCC)
         {
            pUserInstance->pDFCDriverCCQueueFirst = pDriverCC->pNextQueue;
         }
         else
         {
            while(pOtherDriverCC != null)
            {
               if(pOtherDriverCC->pNextQueue == pDriverCC)
               {
                  pOtherDriverCC->pNextQueue = pDriverCC->pNextQueue;
                  break;
               }
               pOtherDriverCC = pOtherDriverCC->pNextQueue;
            }
         }

         /* Synchronize the buffers */
         if((pDriverCC->sSharedBuffer1.nFlags & P_SYNC_BUFFER_FLAG_A) != 0)
         {
            static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer1);
         }
         if((pDriverCC->sSharedBuffer2.nFlags & P_SYNC_BUFFER_FLAG_A) != 0)
         {
            static_DFCDriverUnmapUserBuffer(&pDriverCC->sSharedBuffer2);
         }

         /* Recycle the call context, for a callback or an event handler */
         {
            tDFCDriverCC* pNext = pDriverCC->pNextList;
            CMemoryFill(pDriverCC, 0, sizeof(tDFCDriverCC));
            pDriverCC->pNextList = pNext;
         }
      }
   }
}

/* See header file */
void PDFCDriverInternalPostCC3(
            tContext * pContext,
            tDFCDriverCC** ppDriverCC,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3)
{
   tUserInstance* pUserInstance;
   tDFCDriverCC* pDriverCC = * ppDriverCC;
   tDFCDriverCC* pOtherDriverCC;
   bool_t          bIsInDFCQueue = W_FALSE;

   if ((PContextCheckDriverDFC(pContext, pDriverCC) == W_FALSE) || (pDriverCC->sDFC.pFunction == null))
   {
      return;
   }

   pUserInstance = pDriverCC->pUserInstance;
   pOtherDriverCC = pUserInstance->pDFCDriverCCQueueFirst;

   /* check if the DFC is used */
   while (pOtherDriverCC != null)
   {
      if (pOtherDriverCC == pDriverCC )
      {
         bIsInDFCQueue = W_TRUE;
         PDebugTrace("PDFCDriverInternalPostCC3 : DFC %p is used", pDriverCC);
      }

      if (pOtherDriverCC->pNextQueue == null)
         break;

      pOtherDriverCC = pOtherDriverCC->pNextQueue;
   }

   nFlags |= pDriverCC->sDFC.nFlags;
   if((nFlags & P_DFC_FLAG_EVENT_HANDLER) != 0)
   {
      if((nFlags & P_DFC_FLAG_EVENT_HANDLER_USED) != 0)
      {
         PDebugWarning("PDFCDriverInternalPostCC3: Event handler already posted");
         PDebugTrace("Replacing %08X callback-%p( %p, %p, 0x%08X  )", pDriverCC->sDFC.nFlags, pDriverCC->sDFC.pFunction,
               pDriverCC->sDFC.pParam1, pDriverCC->sDFC.pParam2, pDriverCC->sDFC.nParam3);

         CDebugAssert(bIsInDFCQueue != W_FALSE);
      }
      else
      {
          CDebugAssert(pDriverCC->pNextQueue == null);
          CDebugAssert(bIsInDFCQueue == W_FALSE);
      }

      nFlags |= P_DFC_FLAG_EVENT_HANDLER_USED;
   }
   else
   {
      CDebugAssert(pDriverCC->pNextQueue == null);
      CDebugAssert(bIsInDFCQueue == W_FALSE);
   }

   pDriverCC->sDFC.pType = null;
   pDriverCC->sDFC.nFlags = nFlags;
   pDriverCC->sDFC.pParam2 = pParam2;
   pDriverCC->sDFC.nParam3 = nParam3;
   pDriverCC->sDFC.nParam4 = 0;
   pDriverCC->sDFC.nParam5 = 0;
   pDriverCC->sDFC.nParam6 = 0;

   if (bIsInDFCQueue == W_FALSE)
   {
      PDebugTrace("Posting %08X callback-%p( %p, %p, 0x%08X  )", pDriverCC->sDFC.nFlags, pDriverCC->sDFC.pFunction,
               pDriverCC->sDFC.pParam1, pDriverCC->sDFC.pParam2, pDriverCC->sDFC.nParam3);

      if(pOtherDriverCC == null)
      {
         pUserInstance->pDFCDriverCCQueueFirst = pDriverCC;
      }
      else
      {
         pOtherDriverCC->pNextQueue = pDriverCC;
      }
   }

   if ((nFlags & P_DFC_FLAG_EVENT_HANDLER) == 0)
   {
      * ppDriverCC = null;
   }

   CSyncIncrementSemaphore(&pUserInstance->hSemaphore);
}

void PDFCDriverInternalPostCC6(
            tContext * pContext,
            tDFCDriverCC** ppDriverCC,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3,
            uint32_t nParam4,
            uint32_t nParam5,
            uint32_t nParam6)
{
   tUserInstance* pUserInstance;
   tDFCDriverCC* pDriverCC = * ppDriverCC;
   tDFCDriverCC* pOtherDriverCC;
   bool_t          bIsInDFCQueue = W_FALSE;

   if ((PContextCheckDriverDFC(pContext, pDriverCC) == W_FALSE) || (pDriverCC->sDFC.pFunction == null))
   {
      return;
   }

   pUserInstance = pDriverCC->pUserInstance;
   pOtherDriverCC = pUserInstance->pDFCDriverCCQueueFirst;

   /* check if the DFC is used */
   while (pOtherDriverCC != null)
   {
      if (pOtherDriverCC == pDriverCC )
      {
         bIsInDFCQueue = W_TRUE;
         PDebugTrace("PDFCDriverInternalPostCC3 : DFC %p is used", pDriverCC);
      }

      if (pOtherDriverCC->pNextQueue == null)
         break;

      pOtherDriverCC = pOtherDriverCC->pNextQueue;
   }

   nFlags |= pDriverCC->sDFC.nFlags;
    if((nFlags & P_DFC_FLAG_EVENT_HANDLER) != 0)
   {
      if((nFlags & P_DFC_FLAG_EVENT_HANDLER_USED) != 0)
      {
         PDebugWarning("PDFCDriverInternalPostCC6: Event handler already posted");
         PDebugTrace("Replacing %08X callback-%p( %p, %p, 0x%08X, 0x%08X, 0x%08X, 0x%08X )",
            pDriverCC->sDFC.nFlags, pDriverCC->sDFC.pFunction,
            pDriverCC->sDFC.pParam1, pDriverCC->sDFC.pParam2, pDriverCC->sDFC.nParam3,
            pDriverCC->sDFC.nParam4, pDriverCC->sDFC.nParam5, pDriverCC->sDFC.nParam6);

         CDebugAssert(bIsInDFCQueue != W_FALSE);
      }
      else
      {
          CDebugAssert(pDriverCC->pNextQueue == null);
          CDebugAssert(bIsInDFCQueue == W_FALSE);
      }

      nFlags |= P_DFC_FLAG_EVENT_HANDLER_USED;
   }
   else
   {
      CDebugAssert(pDriverCC->pNextQueue == null);
      CDebugAssert(bIsInDFCQueue == W_FALSE);
   }

   pDriverCC->sDFC.pType = null;
   pDriverCC->sDFC.nFlags = nFlags;
   pDriverCC->sDFC.pParam2 = pParam2;
   pDriverCC->sDFC.nParam3 = nParam3;
   pDriverCC->sDFC.nParam4 = nParam4;
   pDriverCC->sDFC.nParam5 = nParam5;
   pDriverCC->sDFC.nParam6 = nParam6;

   if (bIsInDFCQueue == W_FALSE)
   {
      PDebugTrace("Posting %08X callback-%p( %p, %p, 0x%08X, 0x%08X, 0x%08X, 0x%08X )",
            pDriverCC->sDFC.nFlags, pDriverCC->sDFC.pFunction,
            pDriverCC->sDFC.pParam1, pDriverCC->sDFC.pParam2, pDriverCC->sDFC.nParam3,
            pDriverCC->sDFC.nParam4, pDriverCC->sDFC.nParam5, pDriverCC->sDFC.nParam6);

      if(pOtherDriverCC == null)
      {
         pUserInstance->pDFCDriverCCQueueFirst = pDriverCC;
      }
      else
      {
         pOtherDriverCC->pNextQueue = pDriverCC;
      }
   }

   if ((nFlags & P_DFC_FLAG_EVENT_HANDLER) == 0)
   {
      * ppDriverCC = null;
   }

   CSyncIncrementSemaphore(&pUserInstance->hSemaphore);
}

#endif /* P_CONFIG_DRIVER */

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_INCLUDE_JAVA_API))

/**
 * Pushes a DFC call in the external callback list.
 *
 * @param[in]  pContext  The curent context.
 *
 * @param[in]  pDFC  The dfc call to push.
 **/
static void static_PDFCPushJNICallback(
         tContext* pContext,
         tDFCElement* pDFC)
{
   tDFCQueue* pDFCQueue = PContextGetDFCQueue(pContext);

   if(pDFCQueue->nElementNumber == P_DFC_QUEUE_INITIAL_SIZE)
   {
      PDebugError("static_PDFCPushJNICallback: Too many calls in the list");
   }
   else
   {
      pDFCQueue->pExternalList[pDFCQueue->nNextToAdd++] = *pDFC;
      if(pDFCQueue->nNextToAdd == P_DFC_QUEUE_INITIAL_SIZE)
      {
         pDFCQueue->nNextToAdd = 0;
      }
      pDFCQueue->nElementNumber++;
   }
}

/* See header file */
bool_t PDFCPumpJNICallback(
         tContext * pContext,
         uint32_t* pArgs,
         uint32_t nArgsSize)
{
   tDFCQueue* pDFCQueue = PContextGetDFCQueue(pContext);

   if(pDFCQueue->nElementNumber != 0)
   {
      tDFCElement* pDFC = &pDFCQueue->pExternalList[pDFCQueue->nNextToCall++];
      pDFCQueue->nElementNumber--;

      pArgs[0] = (uint32_t)PUtilConvertPointerToUint( pDFC->pFunction );
      pArgs[1] = (uint32_t)PUtilConvertPointerToUint( pDFC->pParam2 );
      pArgs[2] = pDFC->nParam3;
      pArgs[3] = pDFC->nParam4;
      pArgs[4] = pDFC->nParam5;
      pArgs[5] = pDFC->nParam6;

      if(pDFCQueue->nNextToCall == P_DFC_QUEUE_INITIAL_SIZE)
      {
         pDFCQueue->nNextToCall = 0;
      }

      return W_TRUE;
   }

   return W_FALSE;
}

#endif /* (P_CONFIG_USER || P_CONFIG_MONOLITHIC) && P_INCLUDE_JAVA_API */
