/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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

#ifndef __WME_MULTI_TIMER_H
#define __WME_MULTI_TIMER_H

/*******************************************************************************
   Contains the declaration of the multi-timer function
*******************************************************************************/

/* Timer identifiers */
#define TIMER_T9_P2P                         0
#define TIMER_T10_READER_DETECTION           1
#define TIMER_T13_NFCC_BOOT                  2
#define TIMER_T14_READER_DETECTION_RESTART   3
#define TIMER_T11_SE_WATCHDOG                4

/* User timer identifiers */
#define TIMER_T13_USER_TEST                  5

#define TIMER_T15_CARD_REMOVAL_DETECTION     6

/** The number of multiplexed timers */
#define P_MULTI_TIMER_NUMBER                 7

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Boot timeout */
#define TIMER_T13_TIMEOUT_BOOT                5000

/** Describes a timer element - Do not use directly */
typedef struct __tMultiTimerElement
{
   uint32_t nAbsoluteTimeout;
   tPBasicGenericCompletionFunction* pCallbackFunction; /* set to null for user timers */
   void* pCallbackParameter;
   tDFCDriverCCReference pDriverCC;  /* Used only for user timers */
} tMultiTimerElement;

/** Describes a multi-timer instance - Do not use directly */
typedef struct __tMultiTimerInstance
{
   uint32_t nLastTimerValue;
   tMultiTimerElement aTimers[ P_MULTI_TIMER_NUMBER ];
} tMultiTimerInstance;

/**
 * @brief Creates a multi-timer instance.
 *
 * @pre  Only one multi-timer instance is created at a given time.
 *
 * @param[out]  pMultiTimer  The multi-timer instance to initialize.
 **/
void PMultiTimerCreate(
         tMultiTimerInstance* pMultiTimer);

/**
 * @brief Destroyes a multi-timer instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending timer is cancelled.
 *
 * @post  PMultiTimerDestroy() does not return any error. The caller should always
 *        assume that the multi-timer instance is destroyed after this call.
 *
 * @post  The caller should never re-use the multi-timer instance value.
 *
 * @param[in]  pMultiTimer  The multi-timer instance to destroy.
 **/
void PMultiTimerDestroy(
         tMultiTimerInstance* pMultiTimer );

/**
 * @brief  Sets a timer.
 *
 * If the timer expires, the callback function is called.
 * The timer may be cancelled by a call to PMultiTimerCancel() before it expires.
 *
 * If a timer is already pending when PMultiTimerSet() is called.
 * The current timer is cancelled and replaced by the new one.
 *
 * @pre The timer identifier should be included in [0, P_MULTI_TIMER_NUMBER[.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nTimerIdentifier  The timer identifier in [0, P_MULTI_TIMER_NUMBER[.
 *
 * @param[in]  nTimeout  The relative timeout for the timer in ms.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPBasicGenericCompletionFunction().
 **/
void PMultiTimerSet(
         tContext* pContext,
         uint32_t nTimerIdentifier,
         uint32_t nTimeout,
         tPBasicGenericCompletionFunction* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Cancels a timer.
 *
 * If the timer is not expired, the timer is cancelled. If the timer already expired,
 * or if no timer was set, the function does nothing and returns.
 *
 * @pre The timer identifier should be included in [0, P_MULTI_TIMER_NUMBER[.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nTimerIdentifier  The timer identifier in [0, P_MULTI_TIMER_NUMBER[.
 **/
void PMultiTimerCancel(
         tContext* pContext,
         uint32_t nTimerIdentifier );

/**
 * Polls to check if a timer is expired.
 *
 * This function is called by the event pump to check the timers.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  bIsElapsed This call is due to HAL timer elapsed
 **/
void PMultiTimerPoll(
         tContext* pContext,
         bool_t bIsElapsed);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* __WME_MULTI_TIMER_H */
