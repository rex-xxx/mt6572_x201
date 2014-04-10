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
   Contains the multi-timer implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( TIMER )

#include "nfc_hal_binding.h"

/* See header file */
NFC_HAL_INTERNAL void PNALMultiTimerCreate(
         tNALMultiTimerInstance* pMultiTimer,
         tNALTimerInstance* pTimer)
{
   CNALDebugAssert(pMultiTimer != null);
   CNALMemoryFill(pMultiTimer, 0, sizeof(tNALMultiTimerInstance));

   pMultiTimer->pTimer = pTimer;
   pMultiTimer->nLastTimerValue = (uint32_t)-1;
}

/* See header file */
NFC_HAL_INTERNAL void PNALMultiTimerDestroy(
         tNALMultiTimerInstance* pMultiTimer )
{
   if(pMultiTimer != null)
   {
      CNALMemoryFill(pMultiTimer, 0, sizeof(tNALMultiTimerInstance));
   }
}

/**
 * Calls the entropy callback of upper layer.
 *
 * @param[in]  pBindingContext The NFC HAL Binding Instance.
 *
 * @param[in]  value           The value
 **/

static void static_PNALBindingCallEntropyCallback(
            tNALBindingContext * pBindingContext,
            uint32_t value)
{
   if (pBindingContext->bInPoll == W_FALSE)
   {
      PNALDFCPost1(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallEntropyCallback, PNALUtilConvertUint32ToPointer(value));
   }
   else
   {
      pBindingContext->bInPoll = W_FALSE;
      CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);

      pBindingContext->pNALAntropySourceHandlerFunction(pBindingContext->pCallbackContext, value);

      CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);
      pBindingContext->bInPoll = W_TRUE;
   }
}

/* See header file */
NFC_HAL_INTERNAL void PNALMultiTimerSet(
         tNALBindingContext* pBindingContext,
         uint32_t nTimerIdentifier,
         uint32_t nTimeout,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALMultiTimerInstance* pMultiTimer = PNALContextGetMultiTimer(pBindingContext);
   tNALMultiTimerElement* pElement;

   CNALDebugAssert(pMultiTimer != null);
   CNALDebugAssert(nTimerIdentifier < P_NAL_MULTI_TIMER_NUMBER);

   pElement = &pMultiTimer->aTimers[nTimerIdentifier];
   pElement->nAbsoluteTimeout = CNALTimerGetCurrentTime(pMultiTimer->pTimer) + nTimeout;

   pElement->pCallbackFunction = pCallbackFunction;
   pElement->pCallbackParameter = pCallbackParameter;

   PNALDebugTrace("PNALMultiTimerSet : Timer #%d, %p ( %p )", nTimerIdentifier + 1, pCallbackFunction, pCallbackParameter );

   PNALMultiTimerPoll( pBindingContext, W_FALSE );

   /* generate some random entropy */
   static_PNALBindingCallEntropyCallback(pBindingContext, pElement->nAbsoluteTimeout);
}

/* See header file */
NFC_HAL_INTERNAL void PNALMultiTimerCancel(
         tNALBindingContext* pBindingContext,
         uint32_t nTimerIdentifier )
{
   tNALMultiTimerInstance* pMultiTimer = PNALContextGetMultiTimer(pBindingContext);
   tNALMultiTimerElement* pElement;

   CNALDebugAssert(pMultiTimer != null);
   CNALDebugAssert(nTimerIdentifier < P_NAL_MULTI_TIMER_NUMBER);

   PNALDebugTrace("PNALMultiTimerCancel : Timer #%d", nTimerIdentifier + 1 );

   pElement = &pMultiTimer->aTimers[nTimerIdentifier];
   if(pElement->nAbsoluteTimeout != 0)
   {
      pElement->nAbsoluteTimeout = 0;
      pElement->pCallbackFunction = null;
      pElement->pCallbackParameter = null;

      PNALMultiTimerPoll( pBindingContext, W_FALSE );
   }

   /* Remove to DFC calls for this timer */
   PNALDFCFlush( pBindingContext, pElement );
}

/* See header file */
NFC_HAL_INTERNAL void PNALMultiTimerPoll(
         tNALBindingContext* pBindingContext,
         bool_t      bIsElapsed)
{
   tNALMultiTimerInstance* pMultiTimer = PNALContextGetMultiTimer(pBindingContext);
   uint32_t nTimer;
   uint32_t nNow = CNALTimerGetCurrentTime(pMultiTimer->pTimer);
   uint32_t nExpirationTime;
   uint32_t nMinDelta = (uint32_t)-1;
   uint32_t nLimit = (uint32_t)-1;

   /* If the timer has elapsed, force the re-arm of the timer
      if there's at least one timer remaining */

   if (bIsElapsed == W_TRUE)
   {
      pMultiTimer->nLastTimerValue = nLimit;
   }

   /* Check if some of the timers are expired */
   for(nTimer = 0; nTimer < P_NAL_MULTI_TIMER_NUMBER; nTimer++)
   {
      tNALMultiTimerElement* pElement = &pMultiTimer->aTimers[nTimer];

      if((nExpirationTime = pElement->nAbsoluteTimeout) != 0)
      {
         uint32_t  nDelta;

         /* check if the timer has expired */

         if (nExpirationTime <= nNow)
         {
            nDelta = nNow - nExpirationTime;
         }
         else
         {
            nDelta  = 0xFFFFFFFF - nExpirationTime + nNow;
         }

         if (nDelta <= 0x7FFFFFFF)
         {
            PNALDebugTrace("PNALMultiTimerPoll : Timer #%d expired", nTimer + 1 );

            PNALDFCPost1( pBindingContext, pElement,
               pElement->pCallbackFunction, pElement->pCallbackParameter );

            pElement->nAbsoluteTimeout = 0;
            pElement->pCallbackFunction = null;
            pElement->pCallbackParameter = null;
         }
         else
         {
           nDelta = 0xFFFFFFFF - nDelta;

           if (nMinDelta > nDelta)
           {
              nLimit = nExpirationTime;
              nMinDelta = nDelta;
           }
         }
      }
   }

   if(pMultiTimer->nLastTimerValue != nLimit)
   {
      if(nLimit == (uint32_t)-1)
      {
         CNALTimerCancel(pMultiTimer->pTimer);
      }
      else
      {
         CNALTimerSet(pMultiTimer->pTimer, nLimit - nNow);
      }

      pMultiTimer->nLastTimerValue = nLimit;
   }
}
