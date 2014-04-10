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

#ifndef __WME_EMUL_H
#define __WME_EMUL_H

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
   Contains the declaration of the card emulation functions
*******************************************************************************/

/* the maximum length in bytes of a message data */
#define W_EMUL_DRIVER_MAX_LENGTH       NAL_MESSAGE_MAX_LENGTH - 3

/* Declare a card emulation information structure */
typedef struct __tEmulCardInfo
{
   /* Connection header */
   tHandleObjectHeader  sObjectHeader;

   /* card emulation created from user or from kernel */
   bool_t bFromUser;

   /* Emulation state */
   uint8_t  nState;
   /* Destruction rescheduled flag */
   bool_t bDestructionScheduled;
   /* The card state */
   uint8_t nCardState;

   /* Data reception flag, used to avoid event crossing */
   bool_t bReaderDataAvailable;

   /* The card protocol constant */
   tPRegistryDriverCardProtocolConstant* pConstant;

   /* Data buffer */
   uint8_t aDataBuffer[W_EMUL_DRIVER_MAX_LENGTH];

   /* Reception data length */
   uint32_t nReaderToCardBufferLength;

   /* Callback context */

   tDFCDriverCCReference  pOpenDriverCC;
   tDFCCallbackContext    sOpenCC;

   tDFCDriverCCReference  pReceptionDriverCC;
   tDFCCallbackContext    sReceptionCC;

   tDFCDriverCCReference  pEventDriverCC;
   tDFCCallbackContext    sEventCC;
   bool_t bIsEventCallbackSet;

   tDFCDriverCCReference  pCloseDriverCC;
   tDFCCallbackContext    sCloseCC;
   bool_t bIsCloseCallbackSet;

   /* Service Operation */
   tNALServiceOperation sServiceOperationSendEvent;

} tEmulCardInfo;

/* Declare a card emulation instance */
typedef struct __tEmulInstance
{
   /* Card emulation information 14443-4 Type A */
   tEmulCardInfo                       s14ACardInfo;
   /* Card emulation information 14443-4 Type B */
   tEmulCardInfo                       s14BCardInfo;
   /* Card emulation information 18092 */
   tEmulCardInfo                       s18CardInfo;

} tEmulInstance;

/**
 * @brief   Creates a card emulation instance.
 *
 * @pre  Only one card emulation instance is created at a given time.
 *
 * @param[in]  pEmulInstance The card emulation instance to initialize.
 **/
void PEmulCreate(
      tEmulInstance* pEmulInstance );

/**
 * @brief   Destroyes a card emulation instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  PEmulDestroy() does not return any error. The caller should always
 *        assume that the card emulation instance is destroyed after this call.
 *
 * @post  The caller should never re-use the card emulation instance value.
 *
 * @param[in]  pEmulInstance  The card emulation instance to destroy.
 **/
void PEmulDestroy(
      tEmulInstance* pEmulInstance );

/**
 * @brief Gets the current RF activity state.
 *
 * @param[in]  pContext  The current context.
 *
 * @return  The RF activity state:
 *               - W_NFCC_RF_ACTIVITY_INACTIVE
 *               - W_NFCC_RF_ACTIVITY_DETECTION
 *               - W_NFCC_RF_ACTIVITY_ACTIVE
 **/
uint8_t PEmulGetRFActivity(
                  tContext* pContext);

/**
 * @brief Gets the active protocols.
 *
 * @param[in]  pContext  The current context.
 *
 * @return  The active protocol bit field.
 **/
uint32_t PEmulGetActiveProtocol(
                  tContext* pContext);

void PEmulOpenConnectionHelper(
   tContext * pContext,
   tPBasicGenericCallbackFunction* pOpenCallback,
   void* pOpenCallbackParameter,
   tPBasicGenericEventHandler* pEventCallback,
   void* pEventCallbackParameter,
   tPEmulCommandReceived* pCommandCallback,
   void* pCommandCallbackParameter,
   tWEmulConnectionInfo* pEmulConnectionInfo,
   W_HANDLE* phHandle);

#endif /* P_CONFIG_DRIVER ||P_CONFIG_MONOLITHIC */

#endif /* __WME_EMUL_H */
