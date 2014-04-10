/*
 * Copyright (c) 2007-2011 Inside Secure, All Rights Reserved.
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

#include "nfc_hal_binding.h"

/* The variable P_NAL_DFC_QUEUE_INITIAL_SIZE defines the initial size of the DFC queue in DFC entries.  */
#define P_NAL_DFC_QUEUE_INITIAL_SIZE  32

/* The variable P_NAL_DFC_QUEUE_DELTA defines he increment of the DFC queue in DFC entries. */
#define P_NAL_DFC_QUEUE_DELTA  16

/** The structure of a DFC element */
typedef struct __tNALDFCElement
{
   uint32_t nFlags;  /* The flags, including the number of parameters */
   void* pType; /* THe type of the DFC */
   tNALDFCCallback* pFunction;  /* The function pointer */
   void* pParam1;  /* The parameters ... */
   void* pParam2;
   uint32_t nParam3;
   uint32_t nParam4;
   uint32_t nParam5;
   uint32_t nParam6;
} tNALDFCElement;

/* The type for the callback function */
typedef void tNALDFCCallback0( tNALBindingContext* );
typedef void tNALDFCCallback1( tNALBindingContext*, void* );
typedef void tNALDFCCallback2( tNALBindingContext*, void*, void* );
typedef void tNALDFCCallback3( tNALBindingContext*, void*, void*, uint32_t nParam3 );
typedef void tNALDFCCallback4( tNALBindingContext*, void*, void*, uint32_t nParam3, uint32_t nParam4 );
typedef void tNALDFCCallback5( tNALBindingContext*, void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5 );
typedef void tNALDFCCallback6( tNALBindingContext*, void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5, uint32_t nParam6 );
typedef void tNALDFCCallbackExt1( void* );
typedef void tNALDFCCallbackExt2( void*, void* );
typedef void tNALDFCCallbackExt3( void*, void*, uint32_t nParam3 );
typedef void tNALDFCCallbackExt4( void*, void*, uint32_t nParam3, uint32_t nParam4 );
typedef void tNALDFCCallbackExt5( void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5 );
typedef void tNALDFCCallbackExt6( void*, void*, uint32_t nParam3, uint32_t nParam4, uint32_t nParam5, uint32_t nParam6 );

/* See header file */
NFC_HAL_INTERNAL bool_t PNALDFCCreate(
   tNALDFCQueue* pDFCQueue )
{
   tNALDFCElement* pDFCElementList;

   CNALMemoryFill(pDFCQueue, 0, sizeof(tNALDFCQueue));

   pDFCElementList = (tNALDFCElement*)CNALMemoryAlloc(P_NAL_DFC_QUEUE_INITIAL_SIZE * sizeof(tNALDFCElement));
   if(pDFCElementList == null)
   {
      PNALDebugError("PNALDFCCreate: Cannot allocate the DFC queue");
      return W_FALSE;
   }
   pDFCQueue->pDFCElementList = pDFCElementList;
   pDFCQueue->nDFCQueueSize = P_NAL_DFC_QUEUE_INITIAL_SIZE;

   return W_TRUE;
}

/* See header file */
NFC_HAL_INTERNAL void PNALDFCDestroy(
         tNALDFCQueue* pDFCQueue )
{
   if(pDFCQueue != null)
   {
      CNALMemoryFree(pDFCQueue->pDFCElementList);
      CNALMemoryFill(pDFCQueue, 0, sizeof(tNALDFCQueue));
   }
}

static void static_PNALDFCPerformCall(
         tNALBindingContext* pBindingContext,
         tNALDFCElement* pDFC)
{
   switch(pDFC->nFlags & 0x0000000F)
   {
   case 0:
      PNALDebugTrace("Calling %08X callback-%p( pBindingContext )", pDFC->nFlags, pDFC->pFunction );
      ((tNALDFCCallback0*)(pDFC->pFunction))(pBindingContext);
      break;
   case 1:
      PNALDebugTrace("Calling %08X callback-%p( pBindingContext, %p )", pDFC->nFlags, pDFC->pFunction,
         pDFC->pParam1);
      ((tNALDFCCallback1*)(pDFC->pFunction))(pBindingContext, pDFC->pParam1);
      break;
   case 2:
      PNALDebugTrace("Calling %08X callback-%p( pBindingContext, %p, %p )", pDFC->nFlags, pDFC->pFunction,
         pDFC->pParam1, pDFC->pParam2);
      ((tNALDFCCallback2*)(pDFC->pFunction))(pBindingContext, pDFC->pParam1, pDFC->pParam2);
      break;
   case 3:
      PNALDebugTrace("Calling %08X callback-%p( pBindingContext, %p, %p, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
         pDFC->pParam1, pDFC->pParam2, pDFC->nParam3);
      ((tNALDFCCallback3*)(pDFC->pFunction))(pBindingContext, pDFC->pParam1, pDFC->pParam2,
         pDFC->nParam3);
      break;
   case 4:
      PNALDebugTrace("Calling %08X callback-%p( pBindingContext, %p, %p, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
         pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4);
      ((tNALDFCCallback4*)(pDFC->pFunction))(pBindingContext, pDFC->pParam1, pDFC->pParam2,
         pDFC->nParam3, pDFC->nParam4);
      break;
   case 5:
      PNALDebugTrace("Calling %08X callback-%p( pBindingContext, %p, %p, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
         pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5);
      ((tNALDFCCallback5*)(pDFC->pFunction))(pBindingContext, pDFC->pParam1, pDFC->pParam2,
         pDFC->nParam3, pDFC->nParam4, pDFC->nParam5);
      break;
   case 6:
      PNALDebugTrace("Calling %08X callback-%p( pBindingContext, %p, %p, 0x%08X, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
         pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
      ((tNALDFCCallback6*)(pDFC->pFunction))(pBindingContext, pDFC->pParam1, pDFC->pParam2,
         pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
      break;
   default:
      PNALDebugError("static_PNALDFCPerformCall: Invalid flags");
      CNALDebugAssert(W_FALSE);
      break;
   }
}

/* See header file */
NFC_HAL_INTERNAL bool_t PNALDFCPump(
         tNALBindingContext* pBindingContext )
{
   tNALDFCQueue* pDFCQueue = PNALContextGetDFCQueue(pBindingContext);
   bool_t bSomeCall = W_FALSE;

   CNALDebugAssert( pDFCQueue != null );

   while( pDFCQueue->nDFCNumber != 0 )
   {
      tNALDFCElement* pDFC = &pDFCQueue->pDFCElementList[pDFCQueue->nFirstDFC++];
      bSomeCall = W_TRUE;

      if(pDFCQueue->nFirstDFC >= pDFCQueue->nDFCQueueSize)
      {
         pDFCQueue->nFirstDFC = 0;
      }
      pDFCQueue->nDFCNumber--;

      if(pDFC->pFunction != null)
      {
         static_PNALDFCPerformCall(pBindingContext, pDFC);
      }
   }

   return bSomeCall;
}

/* See header file */
NFC_HAL_INTERNAL void PNALDFCFlush(
         tNALBindingContext* pBindingContext,
         void* pType )
{
   uint32_t nFirstDFC, nDFCNumber;
   tNALDFCQueue* pDFCQueue = PNALContextGetDFCQueue(pBindingContext);

   CNALDebugAssert( pDFCQueue != null );

   nDFCNumber = pDFCQueue->nDFCNumber;
   nFirstDFC = pDFCQueue->nFirstDFC;

   while( nDFCNumber != 0 )
   {
      tNALDFCElement* pDFC = &pDFCQueue->pDFCElementList[nFirstDFC++];
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
static tNALDFCElement* static_PNALDFCPostInternal(
            tNALBindingContext* pBindingContext )
{
   uint32_t nIndex;
   tNALDFCQueue* pDFCQueue = PNALContextGetDFCQueue(pBindingContext);

   CNALDebugAssert( pDFCQueue != null );
   CNALDebugAssert( pDFCQueue->nDFCNumber <= pDFCQueue->nDFCQueueSize );

   if(pDFCQueue->nDFCNumber == pDFCQueue->nDFCQueueSize)
   {
      tNALDFCElement* pDFCElementList = CNALMemoryAlloc( (pDFCQueue->nDFCQueueSize + P_NAL_DFC_QUEUE_DELTA)*sizeof(tNALDFCElement));
      if (pDFCElementList != null)
      {
         CNALMemoryCopy(pDFCElementList, pDFCQueue->pDFCElementList,
             pDFCQueue->nDFCQueueSize * sizeof(tNALDFCElement));
         CNALMemoryFree( pDFCQueue->pDFCElementList );
         pDFCQueue->pDFCElementList = pDFCElementList;
      }
      else
      {
         PNALDebugError(
         "static_PNALDFCPostInternal: Critial error, cannot increase the size of the DFC queue, the DFC is lost");
         return null;
      }

      if(pDFCQueue->nFirstDFC + pDFCQueue->nDFCNumber > pDFCQueue->nDFCQueueSize)
      {
         uint32_t nRelocation = pDFCQueue->nDFCQueueSize - pDFCQueue->nFirstDFC;

         CNALMemoryMove(&pDFCElementList[pDFCQueue->nFirstDFC + P_NAL_DFC_QUEUE_DELTA],
         &pDFCElementList[pDFCQueue->nFirstDFC], nRelocation * sizeof(tNALDFCElement));

         pDFCQueue->nFirstDFC += P_NAL_DFC_QUEUE_DELTA;
      }

      pDFCQueue->pDFCElementList = pDFCElementList;
      pDFCQueue->nDFCQueueSize += P_NAL_DFC_QUEUE_DELTA;
   }

   nIndex = pDFCQueue->nFirstDFC + pDFCQueue->nDFCNumber;
   if(nIndex >= pDFCQueue->nDFCQueueSize)
   {
      nIndex -= pDFCQueue->nDFCQueueSize;
   }

   if(pDFCQueue->nDFCNumber == 0)
   {
      /* Trigger the event pump if needed */
      CNALSyncTriggerEventPump(pBindingContext->pPortingConfig);
   }

   pDFCQueue->nDFCNumber++;

   return &pDFCQueue->pDFCElementList[nIndex];
}

NFC_HAL_INTERNAL void PNALDFCPostInternal6(
            tNALBindingContext* pBindingContext,
            void* pType,
            uint32_t nFlags,
            tNALDFCCallback* pFunction,
            void* pParam1,
            void* pParam2,
            uint32_t nParam3,
            uint32_t nParam4,
            uint32_t nParam5,
            uint32_t nParam6)
{
   tNALDFCElement* pDFC = static_PNALDFCPostInternal(pBindingContext);

   CNALDebugAssert( pFunction != null );

   if(pDFC != null)
   {
      pDFC->pType = pType;
      pDFC->nFlags = nFlags;
      pDFC->pFunction = pFunction;
      pDFC->pParam1 = pParam1;
      pDFC->pParam2 = pParam2;
      pDFC->nParam3 = nParam3;
      pDFC->nParam4 = nParam4;
      pDFC->nParam5 = nParam5;
      pDFC->nParam6 = nParam6;

      PNALDebugTrace("Posting %08X callback-%p( %p, %p, 0x%08X, 0x%08X, 0x%08X, 0x%08X )", pDFC->nFlags, pDFC->pFunction,
               pDFC->pParam1, pDFC->pParam2, pDFC->nParam3, pDFC->nParam4, pDFC->nParam5, pDFC->nParam6);
   }
}

