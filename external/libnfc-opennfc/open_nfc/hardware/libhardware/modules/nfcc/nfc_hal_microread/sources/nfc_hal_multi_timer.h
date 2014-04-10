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

#ifndef __NFC_HAL_MULTI_TIMER_H
#define __NFC_HAL_MULTI_TIMER_H

/*******************************************************************************
   Contains the declaration of the multi-timer function
*******************************************************************************/

/* Timer identifiers */
#define TIMER_T1_SHDLC_ACK                   0
#define TIMER_T2_SHDLC_RESEND                1
#define TIMER_T3_SHDLC_RST                   2
#define TIMER_T4_SHDLC_PIGGYBACK             3
#define TIMER_T5_NFC_HAL_BOOT_UPDATE         4
#define TIMER_T6_SHDLC_RESEND                5
#define TIMER_T7_NFC_HAL_BINDING             6
#define TIMER_T8_HCI_INACTIVITY              7
#define TIMER_T9_UPPER_TIMER                 8

/** The number of multiplexed timers */
#define P_NAL_MULTI_TIMER_NUMBER             9

/** Describes a timer element - Do not use directly */
typedef struct __tNALMultiTimerElement
{
   uint32_t nAbsoluteTimeout;
   tPNALGenericCompletion* pCallbackFunction;
   void* pCallbackParameter;
} tNALMultiTimerElement;

/** Describes a multi-timer instance - Do not use directly */
typedef struct __tNALMultiTimerInstance
{
   tNALTimerInstance* pTimer;
   uint32_t nLastTimerValue;
   tNALMultiTimerElement aTimers[ P_NAL_MULTI_TIMER_NUMBER ];
} tNALMultiTimerInstance;

/**
 * @brief Creates a multi-timer instance.
 *
 * @pre  Only one multi-timer instance is created at a given time.
 *
 * @param[out]  pMultiTimer  The multi-timer instance to initialize.
 *
 * @param[in]   pTimer  The time instance.
 **/
NFC_HAL_INTERNAL void PNALMultiTimerCreate(
         tNALMultiTimerInstance* pMultiTimer,
         tNALTimerInstance* pTimer);

/**
 * @brief Destroyes a multi-timer instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending timer is cancelled.
 *
 * @post  PNALMultiTimerDestroy() does not return any error. The caller should always
 *        assume that the multi-timer instance is destroyed after this call.
 *
 * @post  The caller should never re-use the multi-timer instance value.
 *
 * @param[in]  pMultiTimer  The multi-timer instance to destroy.
 **/
NFC_HAL_INTERNAL void PNALMultiTimerDestroy(
         tNALMultiTimerInstance* pMultiTimer );

/**
 * @brief  Sets a timer.
 *
 * If the timer expires, the callback function is called.
 * The timer may be cancelled by a call to PNALMultiTimerCancel() before it expires.
 *
 * If a timer is already pending when PNALMultiTimerSet() is called.
 * The current timer is cancelled and replaced by the new one.
 *
 * @pre The timer identifier should be included in [0, P_NAL_MULTI_TIMER_NUMBER[.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nTimerIdentifier  The timer identifier in [0, P_NAL_MULTI_TIMER_NUMBER[.
 *
 * @param[in]  nTimeout  The relative timeout for the timer in ms.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALGenericCompletion().
 **/
NFC_HAL_INTERNAL void PNALMultiTimerSet(
         tNALBindingContext* pBindingContext,
         uint32_t nTimerIdentifier,
         uint32_t nTimeout,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Cancels a timer.
 *
 * If the timer is not expired, the timer is cancelled. If the timer already expired,
 * or if no timer was set, the function does nothing and returns.
 *
 * @pre The timer identifier should be included in [0, P_NAL_MULTI_TIMER_NUMBER[.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nTimerIdentifier  The timer identifier in [0, P_NAL_MULTI_TIMER_NUMBER[.
 **/
NFC_HAL_INTERNAL void PNALMultiTimerCancel(
         tNALBindingContext* pBindingContext,
         uint32_t nTimerIdentifier );

/**
 * Polls to check if a timer is expired.
 *
 * This function is called by the event pump to check the timers.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  bIsElapsed This call is due to HAL timer elapsed
 **/
NFC_HAL_INTERNAL void PNALMultiTimerPoll(
         tNALBindingContext* pBindingContext,
         bool_t bIsElapsed);

#endif /* __NFC_HAL_MULTI_TIMER_H */
