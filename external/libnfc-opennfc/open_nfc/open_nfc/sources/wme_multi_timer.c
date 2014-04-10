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
   Contains the multi-timer implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( TIMER )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See header file */
void PMultiTimerCreate(
         tMultiTimerInstance* pMultiTimer )
{
   CDebugAssert(pMultiTimer != null);
   CMemoryFill(pMultiTimer, 0, sizeof(tMultiTimerInstance));

   pMultiTimer->nLastTimerValue = (uint32_t)-1;
}

/* See header file */
void PMultiTimerDestroy(
         tMultiTimerInstance* pMultiTimer )
{
   if(pMultiTimer != null)
   {
      uint32_t nTimer;

      /* Check if some of the timers are expired */
      for(nTimer = 0; nTimer < P_MULTI_TIMER_NUMBER; nTimer++)
      {
         tMultiTimerElement* pElement = &pMultiTimer->aTimers[nTimer];

         if(pElement->pCallbackParameter == null)
         {
#if 0 /* @todo */
            PDFCDriverFlushCall(pElement->pDriverCC);
#endif /* 0 */
         }
      }

      CMemoryFill(pMultiTimer, 0, sizeof(tMultiTimerInstance));
   }
}

/* See header file */
void PMultiTimerSet(
         tContext* pContext,
         uint32_t nTimerIdentifier,
         uint32_t nTimeout,
         tPBasicGenericCompletionFunction* pCallbackFunction,
         void* pCallbackParameter )
{
   tMultiTimerInstance* pMultiTimer = PContextGetMultiTimer(pContext);
   tMultiTimerElement* pElement;

   CDebugAssert(pMultiTimer != null);
   CDebugAssert(nTimerIdentifier < P_MULTI_TIMER_NUMBER);

   pElement = &pMultiTimer->aTimers[nTimerIdentifier];
   CMemoryFill(pElement, 0, sizeof(tMultiTimerElement));
   pElement->nAbsoluteTimeout = PNALServiceDriverGetCurrentTime(pContext) + nTimeout;
   pElement->pCallbackFunction = pCallbackFunction;
   pElement->pCallbackParameter = pCallbackParameter;

   PDebugTrace("PMultiTimerSet : Timer #%d, %p ( %p )", nTimerIdentifier + 1, pCallbackFunction, pCallbackParameter );

   PMultiTimerPoll( pContext, W_FALSE );

   /* generate some random entropy */
   PContextDriverGenerateEntropy(pContext, pElement->nAbsoluteTimeout);
}

/* See header file */
void PMultiTimerCancel(
         tContext* pContext,
         uint32_t nTimerIdentifier )
{
   tMultiTimerInstance* pMultiTimer = PContextGetMultiTimer(pContext);
   tMultiTimerElement* pElement;

   CDebugAssert(pMultiTimer != null);
   CDebugAssert(nTimerIdentifier < P_MULTI_TIMER_NUMBER);

   PDebugTrace("PMultiTimerCancel : Timer #%d", nTimerIdentifier + 1 );

   pElement = &pMultiTimer->aTimers[nTimerIdentifier];

   if(pElement->pCallbackFunction == null)
   {
      PDFCDriverFlushCall(pElement->pDriverCC);
   }

   pElement->pCallbackFunction = null;
   pElement->pCallbackParameter = null;

   if(pElement->nAbsoluteTimeout != 0)
   {
      pElement->nAbsoluteTimeout = 0;

      PMultiTimerPoll( pContext, W_FALSE );
   }

   /* Remove to DFC calls for this timer */
   PDFCFlush( pContext, pElement );
}

/* See header file */
void PMultiTimerPoll(
         tContext* pContext,
         bool_t      bIsElapsed)
{
   tMultiTimerInstance* pMultiTimer = PContextGetMultiTimer(pContext);
   uint32_t nTimer;
   uint32_t nNow = PNALServiceDriverGetCurrentTime(pContext);
   uint32_t nExpirationTime;
   uint32_t nMinDelta = (uint32_t)-1;
   uint32_t nLimit = (uint32_t)-1;

   /* If the timer has elapsed, force the re-arm of the timer
      if there's at least one timer remaining */

   if (bIsElapsed != W_FALSE)
   {
      pMultiTimer->nLastTimerValue = nLimit;
   }

   /* Check if some of the timers are expired */
   for(nTimer = 0; nTimer < P_MULTI_TIMER_NUMBER; nTimer++)
   {
      tMultiTimerElement* pElement = &pMultiTimer->aTimers[nTimer];

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
            PDebugTrace("PMultiTimerPoll : Timer #%d expired", nTimer + 1 );

            if(pElement->pCallbackFunction != null)
            {
               tDFCCallbackContext sCallbackContext;
               PDFCFillCallbackContextType(pContext, (tDFCCallback*) pElement->pCallbackFunction, pElement->pCallbackParameter, pElement, &sCallbackContext);
               PDFCPostContext1(&sCallbackContext);
            }
            else
            {
               /* User timer */
               PDFCDriverPostCC1(pElement->pDriverCC);
            }

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
         PNALServiceSetVariable(pContext, NAL_PARAM_CURRENT_TIMER, 0);
      }
      else
      {
         PNALServiceSetVariable(pContext, NAL_PARAM_CURRENT_TIMER, nLimit - nNow);
      }

      pMultiTimer->nLastTimerValue = nLimit;
   }
}

void PMultiTimerSetDriver(
                  tContext* pContext,
                  uint32_t nTimerIdentifier,
                  uint32_t nAbsoluteTimeout,
                  tPBasicGenericCompletionFunction* pCallbackFunction,
                  void* pCallbackParameter )
{
   tMultiTimerInstance* pMultiTimer = PContextGetMultiTimer(pContext);
   tMultiTimerElement* pElement;

   CDebugAssert(pMultiTimer != null);

   if( (nTimerIdentifier != TIMER_T13_USER_TEST)
   &&  (nTimerIdentifier != TIMER_T15_CARD_REMOVAL_DETECTION))
   {
      PDebugError("PMultiTimerSetDriver: Bad identifier %d", nTimerIdentifier);
      return;
   }

   pElement = &pMultiTimer->aTimers[nTimerIdentifier];

   PDFCDriverFillCallbackContext(
      pContext, (tDFCCallback*)pCallbackFunction, pCallbackParameter,
      &pElement->pDriverCC );

   pElement->nAbsoluteTimeout = nAbsoluteTimeout;
   pElement->pCallbackFunction = null;
   pElement->pCallbackParameter = null;

   PDebugTrace("PMultiTimerSetDriver : Timer #%d", nTimerIdentifier + 1 );

   PMultiTimerPoll( pContext, W_FALSE );
}

void PMultiTimerCancelDriver(
                  tContext* pContext,
                  uint32_t nTimerIdentifier )
{
   if((nTimerIdentifier != TIMER_T13_USER_TEST)
   && (nTimerIdentifier != TIMER_T15_CARD_REMOVAL_DETECTION))
   {
      PDebugError("PMultiTimerCancelDriver: Bad identifier");
      return;
   }

   PMultiTimerCancel(pContext, nTimerIdentifier);
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
