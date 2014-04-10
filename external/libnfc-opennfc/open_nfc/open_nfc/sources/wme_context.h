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

#ifndef __WME_CONTEXT_H
#define __WME_CONTEXT_H

/*******************************************************************************
   Contains the common headers to inlcude in the portable source files
*******************************************************************************/

struct __tContext;

typedef struct __tContext tContext;

#include "porting_os.h"

#ifdef P_FEATURE_CONTROL_MASK

#undef P_SYNCHRONOUS_FUNCTION_DEBUG
#undef P_EXCLUDE_DEPRECATED_FUNCTIONS
#undef P_INCLUDE_SNEP_NPP
#undef P_INCLUDE_SE_SECURITY
#undef P_INCLUDE_PAIRING
#undef P_INCLUDE_TEST_ENGINE
#undef P_INCLUDE_MIFARE_CLASSIC
#undef P_INCLUDE_PICOPASS

#if P_FEATURE_CONTROL_MASK & 0x01
#define P_SYNCHRONOUS_FUNCTION_DEBUG
#endif

#if P_FEATURE_CONTROL_MASK & 0x02
#define P_EXCLUDE_DEPRECATED_FUNCTIONS
#endif

#if P_FEATURE_CONTROL_MASK & 0x04
#define P_INCLUDE_SNEP_NPP
#endif

#if P_FEATURE_CONTROL_MASK & 0x08
#define P_INCLUDE_SE_SECURITY
#endif

#if P_FEATURE_CONTROL_MASK & 0x10
#define P_INCLUDE_PAIRING
#endif

#if P_FEATURE_CONTROL_MASK & 0x20
#define P_INCLUDE_TEST_ENGINE
#endif

#if P_FEATURE_CONTROL_MASK & 0x40
#define P_INCLUDE_MIFARE_CLASSIC
#endif

#if P_FEATURE_CONTROL_MASK & 0x80
#define P_INCLUDE_PICOPASS
#endif

#endif


   /* Define to allow use of NFCC 14444-4 facilities
       if not defined, revert to previous 14P4 over 14P3 implementation */
#define  P_READER_14P4_STANDALONE_SUPPORT

#ifndef DO_NOT_USE_LWRAP_UNICODE
#define  DO_NOT_USE_LWRAP_UNICODE /* Do NOT change the original API Functions names */
#endif /* DO_NOT_USE_LWRAP_UNICODE */
#include "open_nfc.h"

/* Specific reader priority used internally for SE */

#define W_PRIORITY_SE            (W_PRIORITY_MAXIMUM + 1)
#define W_PRIORITY_SE_FORCED     (W_PRIORITY_MAXIMUM + 2)

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

#ifdef _LINUX_KERNEL_H
#error  -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
#error   LINUX Kernel Headers have been added to the Open NFC porting headers,
#error   infringing the GPL license.
#error   The license of Open NFC core source code is NOT compliant with the GPL license.
#error   Compilation has been stopped; please remove your modifications
#error   to the porting_<xxx>.h files.
#error  -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
#endif

#endif /* P_BUILD_CONFIG == P_CONFIG_DRIVER */

#include "porting_os.h"
#include "porting_user.h"
#include "porting_driver.h"
#include "porting_client.h"

#include "wme_cache_connection.h"

#include "wme_asn1.h"
#include "wme_dfc.h"
#include "wme_test_stub.h"
#include "wme_client_api_autogen.h"
#include "wme_handle.h"

#include "wme_smart_cache.h"

#include "wme_ndef.h"
#include "wme_ndef_type2_generic.h"
#include "wme_multi_timer.h"

#include "nfc_hal.h"

#include "wme_driver.h"
#include "wme_driver_autogen.h"

#include "wme_server_autogen.h"

#include "wme_test_engine.h"

#include "wme_util.h"

#include "wme_basic.h"

#include "wme_7816_4_state_machine.h"
#include "wme_nfc_hal_service.h"
#include "wme_secure_element_hal.h"
#include "wme_nfcc.h"
#include "wme_security_manager.h"
#include "wme_security_stack.h"
#include "wme_secure_element_driver.h"
#include "wme_secure_element.h"
#include "wme_secure_element_hal_state_machine.h"
#include "wme_reader_driver_registry.h"
#include "wme_reader_registry.h"
#include "wme_p2p.h"
#include "wme_p2p_initiator.h"
#include "wme_p2p_target.h"
#include "wme_routing_table.h"

#include "wme_14443_3.h"
#include "wme_14443_4.h"
#include "wme_7816_4.h"
#include "wme_15693.h"
#include "wme_pico.h"
#include "wme_mifare.h"
#include "wme_ndef_active.h"
#include "wme_virtual_tag.h"
#include "wme_emul.h"
#include "wme_type1chip.h"
#include "wme_felica.h"
#include "wme_myd.h"
#include "wme_b_prime.h"
#include "wme_kovio.h"
#include "wme_crypto_des.h"
#include "wme_jupiter.h"
#include "wme_connection_handover.h"
#include "wme_npp.h"
#include "wme_snep.h"
#include "wme_kovio_rfid.h"

#define P_PROTOCOL_STILL_UNKNOWN             0xFF

#if (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

struct __tUserInstance
{
   const tUserIdentity* pUserIdentity;

#ifdef P_INCLUDE_SE_SECURITY
   const uint8_t* pUserAuthenticationData;
   uint32_t nUserAuthenticationDataLength;

   bool_t bAuthenticationDone;
#endif /* P_INCLUDE_SE_SECURITY */
};

#endif /* P_BUILD_CONFIG == P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

struct __tUserInstance
{
   tContext* pContext;

   struct __tUserInstance* pNextInstance;

   const tUserIdentity* pUserIdentity;

#ifdef P_INCLUDE_SE_SECURITY
   const uint8_t* pUserAuthenticationData;
   uint32_t nUserAuthenticationDataLength;

   bool_t bAuthenticationDone;
#endif /* P_INCLUDE_SE_SECURITY */

   tDFCDriverCC* pDFCDriverCCListHead;

   tDFCDriverCC* pDFCDriverCCQueueFirst;

   P_SYNC_SEMAPHORE hSemaphore;

   uint32_t nExternalEventCount;

   bool_t bStopLoop;

   bool_t bIsEventPumpWaitingForSemaphore;

   bool_t bEventPumpShouldDestroyUserInstance;
};

/**
 * Closes the user instance.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pUserInstance  The user instance to close.
 **/
void PDriverCloseInternal(
               tContext* pContext,
               tUserInstance* pUserInstance );

#endif /* P_CONFIG_DRIVER */


#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * Returns the test engine instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The test engine instance.
 **/
tTestEngineInstance* PContextGetTestEngineInstance(
            tContext* pContext);

/**
 * Set the last IOCTL error code.
 *
 * @param[in]  pContext  The context.
 * @param[in]  nError    The error
 *
 **/

void PContextSetLastIoctlError(
            tContext* pContext,
            W_ERROR   nError);

/**
 * Get the last IOCTL error code.
 *
 * @param[in]  pContext  The context.
 *
 * @return The last IOCTL error code
 *
 **/

W_ERROR PContextGetLastIoctlError(
            tContext* pContext);



#if defined(P_SYNCHRONOUS_FUNCTION_DEBUG)

/**
 * Set/Reset the current thread ID used to post user callback
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  bSet : Set / Reset the ID
 *
 **/

void PContextSetCurrentThreadId(
            tContext * pContext,
            bool_t     bSet);

/**
 * Check if the current thread ID is different from the previously set
 *
 * @param[in]  pContext  The context.
 *
 * @return W_TRUE if the current thread id is different from the thread id previously set
 *
 **/

bool_t PContextCheckCurrentThreadId(
            tContext * pContext);

#endif /* defined(P_SYNCHRONOUS_FUNCTION_DEBUG */

/**
 * get the WNDEFSendMessage context
 *
 * @param[in]  pContext  The context.
 *
 * @return the address of the WNDEFSendMessage context
 *
 **/

tNDEFSendMessageInstance * PContextGetNDEFSendMessageInstance(tContext * pContext);

/**
 * get the tNDEFMessageHandlerManager instance
 *
 * @param[in]  pContext  The context.
 *
 * @return the address of the only instance of tNDEFMessageHandlerManager
 *
 **/
tNDEFMessageHandlerManager * PContextGetNDEFMessageHandlerManager(tContext * pContext);

/**
 * Returns the Routing Table instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The Routing Table instance.
 **/
tRoutingTableInstance* PContextGetRoutingTableInstance(
            tContext* pContext);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

/**
 * Returns the status of the context.
 *
 * @param[in]  pContext  The context.
 *
 * @return  W_TRUE if the context is destroyed.
 **/
bool_t PContextIsDead(
         tContext* pContext );

/**
 * Locks the context for the event pump
 *
 * @param[in]  pContext  The context.
 **/
void PContextLockForPump(
         tContext* pContext );

/**
 * Releases the lock on the context for the event pump
 *
 * @param[in]  pContext  The context.
 **/
void PContextReleaseLockForPump(
         tContext* pContext );

/**
 * Set the "in callback" flag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  bIsInCallback  The flag value.
 **/
void PContextSetIsInCallbackFlag(
         tContext* pContext,
         bool_t bIsInCallback );

/**
 * Returns the user instance
 *
 * @param[in]  pContext  The context.
 *
 * @return  The user instance.
 **/
void* PContextGetUserInstance(
         tContext* pContext );

/**
 * Sets the user instance
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pUserInstance  The user instance.
 **/
void PContextSetUserInstance(
         tContext* pContext,
         void* pUserInstance );

/**
 * Creates the global context.
 *
 * @return  The context.
 **/
tContext* PContextCreate( void );

/**
 * Destroyes the global context.
 *
 * @param[in]  pContext  The context.
 **/
void PContextDestroy(
         tContext* pContext );

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * Returns the current user instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The current user instance.
 **/
tUserInstance* PContextGetCurrentUserInstance(
            tContext* pContext);

/**
 * Sets the current user instance.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pUserInstance  The current user instance.
 **/
void PContextSetCurrentUserInstance(
         tContext* pContext,
         tUserInstance* pUserInstance );

/**
 * Returns the secure element instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The secure element instance.
 **/
tSEInstance* PContextGetSEInstance(
            tContext* pContext);

/**
 * Returns the secure element HAL instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The secure element HAL instance.
 **/
tPSeHalInstance* PContextGetSeHalInstance(
            tContext* pContext);

/**
 * Returns the card emulation instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The card emulation instance.
 **/
tEmulInstance* PContextGetEmulInstance(
            tContext* pContext);

/**
 * Returns the P2P instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The P2P instance.
 **/
tP2PInstance * PContextGetP2PInstance(
            tContext* pContext);

/**
 * Returns the Routing Table instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The Routing Table instance.
 **/
tRoutingTableDriverInstance* PContextGetRoutingTableDriverInstance(
            tContext* pContext);

/**
 * Returns the multi-timer instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The multi-timer instance.
 **/
tMultiTimerInstance* PContextGetMultiTimer(
            tContext* pContext);

/**
 * Returns the NFC HAL Service instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The NFC HAL Service instance.
 **/
tNALServiceInstance* PContextGetNALServiceInstance(
            tContext* pContext);

/**
 * Returns the reader driver registry.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The registry instance.
 **/
tPReaderDriverRegistry* PContextGetReaderDriverRegistry(
            tContext* pContext);

/**
 * Returns the NFC Controller instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The NFC Controller instance.
 **/
tNFCController* PContextGetNFCController(
            tContext* pContext);


#ifdef P_INCLUDE_SNEP_NPP
/**
 * Returns the SNEP Server instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The SNEP Server instance.
 **/

tSNEPServerDriverInstance * PContextGetSNEPServerDriverInstance(
            tContext* pContext);


/**
 * Returns the SNEP Client instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The SNEP Client instance.
 **/

tSNEPClientDriverInstance * PContextGetSNEPClientDriverInstance(
            tContext* pContext);


/**
 * Returns the NPP Server instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The NPP Server instance.
 **/

tNPPServerDriverInstance* PContextGetNPPServerDriverInstance(
            tContext* pContext);

/**
 * Returns the NPP Server instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The NPP Server instance.
 **/

tNPPClientDriverInstance* PContextGetNPPClientDriverInstance(
            tContext* pContext);

#endif /* P_INCLUDE_SNEP_NPP */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

bool_t PContextCheckDriverDFC(
            tContext* pContext,
            tDFCDriverCC* pDriverCC );

#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) */

#ifdef P_TRACE_ACTIVE

/**
 * Returns the trace buffer for the card protocol.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The pointer on the buffer.
 **/
 char* PContextGetCardProtocolTraceBuffer(
            tContext* pContext);

/**
 * Returns the trace buffer for the reader protocol.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The pointer on the buffer.
 **/
char* PContextGetReaderProtocolTraceBuffer(
            tContext* pContext);

/**
 * Returns the trace buffer for the NFC HAL protocol.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The pointer on the buffer.
 **/
char* PContextGetNALTraceBuffer(
            tContext* pContext);

#endif /* P_TRACE_ACTIVE */

/**
 * Returns the NFC Controller information.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The NFC Controller information.
 **/
tNFCControllerInfo* PContextGetNFCControllerInfo(
            tContext* pContext);

/**
 * Returns the DFC Queue instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The DFC queue instance.
 **/
tDFCQueue* PContextGetDFCQueue(
            tContext* pContext);

/**
 * Returns the handle list instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  The handle list instance.
 **/
tHandleList* PContextGetHandleList(
            tContext* pContext);

/**
 * Locks the context
 *
 * @param[in]  pContext  The context.
 **/
void PContextLock(
         tContext* pContext );

/**
 * Releases the lock on the context
 *
 * @param[in]  pContext  The context.
 **/
void PContextReleaseLock(
         tContext* pContext );

/**
 * Trigger a call to the event pump.
 *
 * @param[in]  pContext  The context.
 **/
void PContextTriggerEventPump(
         tContext* pContext );

/**
 * Returns the cache connection instance.
 *
 * @param[in]  pContext  The context.
 *
 * @return  cache connection instance.
 **/
tCacheConnectionInstance* PContextGetCacheConnectionInstance(
            tContext* pContext);

/**
  * Generates some random entropy
  *
  * @param[in]  pContext  The context.
  *
  * @param[in] nValue     Some random value
  **/

void PContextDriverGenerateEntropy(
            tContext * pContext,
            uint32_t   nValue);


#endif /* __WME_CONTEXT_H */
