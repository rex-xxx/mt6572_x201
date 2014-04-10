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
   Contains the context implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( EVENT )

#include "wme_context.h"

struct __tContext
{
   tHandleList sHandleList;
   tDFCQueue sDFCQueue;
   tNFCControllerInfo sNFCControllerInfo;
   P_SYNC_CS hCriticalSection;

   bool_t bInPumpEvent;
   tCacheConnectionInstance sCacheConnectionInstance;

#if (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
   tUserInstance sUserInstance;
#endif /* P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifdef P_INCLUDE_TEST_ENGINE
   tTestEngineInstance sTestEngineInstance;
#endif /* P_INCLUDE_TEST_ENGINE */

   W_ERROR nLastIoctlError;


   /* The NDEF send message instance */
   tNDEFSendMessageInstance sNDEFSendMessageInstance;

   tNDEFMessageHandlerManager sNDEFMessageHandlerManagerInstance;

   tRoutingTableInstance sRoutingTableInstance;

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#ifdef P_TRACE_ACTIVE
   char aCardProtocolTrace[60];
   char aReaderProtocolTrace[60];
   char aNALTraceBuffer[1024];
#endif /* P_TRACE_ACTIVE */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
   tNFCController sNFCController;
   tMultiTimerInstance sMultiTimer;
   tNALServiceInstance sNALServiceInstance;
   tPReaderDriverRegistry sReaderDriverRegistry;

   tPSeHalInstance sSeHalInstance;
   tSEInstance sSEInstance;
   tEmulInstance sEmulInstance;
   tP2PInstance sP2PInstance;
   tRoutingTableDriverInstance sRoutingTableDriverInstance;

#ifdef P_INCLUDE_SNEP_NPP
   tSNEPServerDriverInstance sSNEPServer;
   tSNEPClientDriverInstance sSNEPClient;

   tNPPServerDriverInstance  sNPPServer;
   tNPPClientDriverInstance  sNPPClient;

#endif /* P_INCLUDE_SNEP_NPP */

   void* pPortingConfig;

   uint32_t  nRandom;
#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)
   void* pUserInstance;
   bool_t bIsInCallback;
   bool_t bIsAlive;
#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
   /* The current user instance */
   tUserInstance* pUserInstance;

   /* The list of user instances */
   tUserInstance* pUserInstanceListHead;
#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

   bool_t      bCurrentThreadIdSet;
   uintptr_t   nCurrentThreadId;
#endif /* (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

};

/* See header file */
void PContextLock(
         tContext* pContext )
{
   CSyncEnterCriticalSection(&pContext->hCriticalSection);
}

/* See header file */
void PContextReleaseLock(
         tContext* pContext )
{
   CSyncLeaveCriticalSection(&pContext->hCriticalSection);
}

/* See header file */
void PContextTriggerEventPump(
         tContext* pContext )
{
   if(pContext->bInPumpEvent == W_FALSE)
   {
#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

      CSyncTriggerEventPump(pContext->pPortingConfig);

#elif (P_BUILD_CONFIG == P_CONFIG_USER)

      PDFCDriverInterruptEventLoop(pContext);
      /* @todo here, if the IOCTL failed, the driver loop will not be interrupted */

#endif /* P_CONFIG_USER */
   }
}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

bool_t PContextCheckDriverDFC(
            tContext * pContext,
            tDFCDriverCC* pDriverCC )
{
   tUserInstance * pCurrentUserInstance;

   if (pDriverCC == null)
   {
      return W_FALSE;
   }

   pCurrentUserInstance = pContext->pUserInstanceListHead;

   while (pCurrentUserInstance != null)
   {
      if (PDFCDriverCheckDriverDFCInUserInstance(pCurrentUserInstance, pDriverCC) != W_FALSE)
      {
         return W_TRUE;
      }

      pCurrentUserInstance = pCurrentUserInstance->pNextInstance;
   }

   return W_FALSE;
}
#endif /* P_BUILD_CONFIG == P_CONFIG_DRIVER */

#ifdef P_TRACE_ACTIVE

/* See header file */
char* PContextGetCardProtocolTraceBuffer(
            tContext* pContext)
{
   return pContext->aCardProtocolTrace;
}

/* See header file */
char* PContextGetReaderProtocolTraceBuffer(
            tContext* pContext)
{
   return pContext->aReaderProtocolTrace;
}

/* See header file */
char* PContextGetNALTraceBuffer(
            tContext* pContext)
{
   return pContext->aNALTraceBuffer;
}

#endif /* P_TRACE_ACTIVE */

/* See header file */
tNFCControllerInfo* PContextGetNFCControllerInfo(
            tContext* pContext)
{
   return &pContext->sNFCControllerInfo;
}

/* See header file */
tDFCQueue* PContextGetDFCQueue(
            tContext* pContext)
{
   return &pContext->sDFCQueue;
}

/* See header file */
tHandleList* PContextGetHandleList(
            tContext* pContext)
{
   return &pContext->sHandleList;
}

/* See header file */
tCacheConnectionInstance* PContextGetCacheConnectionInstance(
            tContext* pContext)
{
   return &pContext->sCacheConnectionInstance;
}

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifdef P_INCLUDE_TEST_ENGINE

/* See header file */
tTestEngineInstance* PContextGetTestEngineInstance(
            tContext* pContext)
{
   return &pContext->sTestEngineInstance;
}

#endif /* ifdef P_INCLUDE_TEST_ENGINE */

tNDEFSendMessageInstance * PContextGetNDEFSendMessageInstance(tContext * pContext)
{
   return & pContext->sNDEFSendMessageInstance;
}

tNDEFMessageHandlerManager * PContextGetNDEFMessageHandlerManager(tContext * pContext)
{
   return &pContext->sNDEFMessageHandlerManagerInstance;
}

/* See header file */
tRoutingTableInstance* PContextGetRoutingTableInstance(
            tContext* pContext)
{
   return &pContext->sRoutingTableInstance;
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PContextSetLastIoctlError(
            tContext * pContext,
            W_ERROR nError)
{
   pContext->nLastIoctlError = nError;
}

W_ERROR PContextGetLastIoctlError(
            tContext * pContext)
{
   return pContext->nLastIoctlError;
}

#endif /* ( P_BUILD_CONFIG == P_CONFIG_USER ) */

#if (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


W_ERROR PContextGetLastIoctlError(
            tContext * pContext)
{
   return W_SUCCESS;
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */



#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See header file */
tNFCController* PContextGetNFCController(
            tContext* pContext)
{
   return &pContext->sNFCController;
}

/* See header file */
tPSeHalInstance* PContextGetSeHalInstance(
            tContext* pContext)
{
   return &pContext->sSeHalInstance;
}

/* See header file */
tSEInstance* PContextGetSEInstance(
            tContext* pContext)
{
   return &pContext->sSEInstance;
}

/* See header file */
tEmulInstance* PContextGetEmulInstance(
            tContext* pContext)
{
   return &pContext->sEmulInstance;
}

/* See header file */
tP2PInstance * PContextGetP2PInstance(
            tContext* pContext)
{
   return &pContext->sP2PInstance;
}

/* See header file */
tRoutingTableDriverInstance* PContextGetRoutingTableDriverInstance(
            tContext* pContext)
{
   return &pContext->sRoutingTableDriverInstance;
}


#ifdef P_INCLUDE_SNEP_NPP
tSNEPServerDriverInstance * PContextGetSNEPServerDriverInstance(
            tContext* pContext)
{
   return &pContext->sSNEPServer;
}


tSNEPClientDriverInstance * PContextGetSNEPClientDriverInstance(
            tContext* pContext)
{
   return &pContext->sSNEPClient;
}
#endif /*P_INCLUDE_SNEP_NPP */

#ifdef P_INCLUDE_SNEP_NPP

tNPPServerDriverInstance * PContextGetNPPServerDriverInstance(
            tContext* pContext)
{
   return &pContext->sNPPServer;
}


tNPPClientDriverInstance * PContextGetNPPClientDriverInstance(
            tContext* pContext)
{
   return &pContext->sNPPClient;
}

#endif /* P_INCLUDE_SNEP_NPP */

/* See header file */
tMultiTimerInstance* PContextGetMultiTimer(
            tContext* pContext)
{
   return &pContext->sMultiTimer;
}

/* See header file */
tNALServiceInstance* PContextGetNALServiceInstance(
            tContext* pContext)
{
   return &pContext->sNALServiceInstance;
}

/* See header file */
tPReaderDriverRegistry* PContextGetReaderDriverRegistry(
            tContext* pContext)
{
   return &pContext->sReaderDriverRegistry;
}

/* See porting guide */
bool_t PDriverIsStarted(
            tDriverInstance* pDriverInstance )
{
   tContext* pContext = (tContext*)pDriverInstance;

   return (PNFCControllerGetMode(pContext) != W_NFCC_MODE_BOOT_PENDING)?W_TRUE:W_FALSE;
}

#endif /* #if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

/**
 * Destroyes a context.
 *
 * @param[in] pContext  The context to destroy.
 **/
static void static_PContextDestroy(
         tContext* pContext )
{
   if(pContext != null)
   {
      CSyncEnterCriticalSection(&pContext->hCriticalSection);

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

      PSEDriverResetData(pContext);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_TEST_ENGINE)

      PTestEngineDestroy(&pContext->sTestEngineInstance);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

      PSEDriverDestroy(&pContext->sSEInstance);

      PSeHalDestroy(&pContext->sSeHalInstance);

      PEmulDestroy(&pContext->sEmulInstance);

      PP2PDestroy(&pContext->sP2PInstance);

#ifdef P_INCLUDE_SNEP_NPP
      PSNEPServerDriverInstanceDestroy(&pContext->sSNEPServer);

      PNPPServerDriverInstanceDestroy(&pContext->sNPPServer);

#endif /* P_INCLUDE_SNEP_NPP */

      PReaderDriverDisconnect(pContext);

      PReaderDriverRegistryDestroy(&pContext->sReaderDriverRegistry);

      PNFCControllerDestroy( &pContext->sNFCController );

      PNALServiceDestroy(&pContext->sNALServiceInstance);

      PMultiTimerDestroy(&pContext->sMultiTimer);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

      PDFCDestroy(&pContext->sDFCQueue);

      PHandleDestroy(&pContext->sHandleList);

      CSyncLeaveCriticalSection(&pContext->hCriticalSection);
      CSyncDestroyCriticalSection(&pContext->hCriticalSection);

#if (P_BUILD_CONFIG == P_CONFIG_USER)

      if(pContext->bIsInCallback != W_FALSE)
      {
         /* Reset all and set bIsAlive to W_FALSE */
         CMemoryFill(pContext, 0, sizeof(tContext));
         return;
      }

      CMemoryFree(pContext);

#else
      /* To be safer */
      CMemoryFill(pContext, 0, sizeof(tContext));

      CMemoryFree(pContext);

#endif /* P_CONFIG_USER */
   }
}

/**
 * Creates a context.
 *
 * @param[in]  pPortingConfig  the porting configuration.
 *
 * @param[in]  pReserved  The NFC HAL Binding structure.
 *
 * @param[in]  nReserved  The NFC HAL Binding structure length in bytes.
 *
 * @return  The new context or null if an error is detected.
 **/
static tContext* static_PContextCreate(
         void* pPortingConfig,
         void* pReserved,
         uint32_t nReserved)
{
   tContext* pContext;

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
   tNALBinding* pNALBinding;
#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

   pContext = (tContext*)CMemoryAlloc(sizeof(tContext));
   if(pContext == null)
   {
      PDebugError("static_PContextCreate: Cannot allocate the context");
      goto return_error;
   }

   CMemoryFill(pContext, 0, sizeof(tContext));

   CSyncCreateCriticalSection(&pContext->hCriticalSection);

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

   pContext->pPortingConfig = pPortingConfig;

   if((pReserved != null)
   && (nReserved == sizeof(tNALBinding))
   && (((tNALBinding*)pReserved)->nMagicWord == NAL_BINDING_MAGIC_WORD))
   {
      pNALBinding = (tNALBinding*)pReserved;
   }
   else
   {
      PDebugError("static_PContextCreate: Wrong NFC HAL implementation");
      goto return_error;
   }

   if (W_SUCCESS != PNALServiceCreate(&pContext->sNALServiceInstance, pNALBinding, pPortingConfig, pContext))
   {
      PDebugError("static_PContextCreate: Cannot create the NFC HAL Service");
      goto return_error;
   }

   PMultiTimerCreate(&pContext->sMultiTimer);

   PNFCControllerCreate( &pContext->sNFCController );

   PReaderDriverRegistryCreate(&pContext->sReaderDriverRegistry);

   PSeHalCreate(pContext, &pContext->sSeHalInstance);

   PSEDriverCreate(&pContext->sSEInstance);

   PEmulCreate(&pContext->sEmulInstance);

   PP2PCreate(&pContext->sP2PInstance);

#ifdef P_INCLUDE_SNEP_NPP
   PNPPServerDriverInstanceCreate(&pContext->sNPPServer);

   PSNEPServerDriverInstanceCreate(&pContext->sSNEPServer);

#endif /* P_INCLUDE_SNEP_NPP */

   PContextDriverGenerateEntropy(pContext, P_RANDOM_SEED);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_TEST_ENGINE)

   PTestEngineCreate(&pContext->sTestEngineInstance);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

   if(PDFCCreate(&pContext->sDFCQueue) == W_FALSE)
   {
      PDebugError("static_PContextCreate: Cannot create the DFC Queue");
      goto return_error;
   }

   if(PHandleCreate(&pContext->sHandleList) == W_FALSE)
   {
      PDebugError("static_PContextCreate: Cannot create the handle list");
      goto return_error;
   }

#if (P_BUILD_CONFIG == P_CONFIG_USER)

   pContext->bIsInCallback = W_FALSE;
   pContext->bIsAlive = W_TRUE;

#endif /* P_CONFIG_USER */

   return pContext;

return_error:

   static_PContextDestroy(pContext);
   return null;
}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

/* See header file */
tUserInstance* PContextGetCurrentUserInstance(
            tContext* pContext)
{
   return pContext->pUserInstance;
}

/* See header file */
void PContextSetCurrentUserInstance(
         tContext* pContext,
         tUserInstance* pUserInstance )
{
   pContext->pUserInstance = pUserInstance;
}

/* See Client API Specifications */
tDriverInstance* PDriverCreate(
         tPDriverCreateCompleted* pCompletionCallback,
         void* pPortingConfig,
         bool_t bForceReset,
         void* pNALBinding,
         uint32_t nNALBindingSize )
{
   tContext* pContext = static_PContextCreate(pPortingConfig, pNALBinding, nNALBindingSize);
   if(pContext != null)
   {
      pContext->pUserInstanceListHead = null;
      PNFCControllerPerformInitialReset(pContext,
         (tPNFCControllerInitialResetCompletionCallback*)pCompletionCallback, pPortingConfig,
         bForceReset);
   }
   return (tDriverInstance*)pContext;
}

static void static_PDriverPreClose(
               tContext* pContext,
               tUserInstance* pUserInstance )
{
   pContext->pUserInstance = pUserInstance;
   PHandleCloseAll(pContext, pUserInstance);
   PDFCDriverFlushClient(pContext);
   PDFCDriverStopEventLoop(pContext);
   pContext->pUserInstance = null;
}

void PDriverCloseInternal(
               tContext* pContext,
               tUserInstance* pUserInstance )
{
   tUserInstance* pOtherInstance;

   /* Check if the event pump thread is still waiting for the semaphore */
   if(pUserInstance->bIsEventPumpWaitingForSemaphore != W_FALSE)
   {
      /* Then, just flag the user instance, the event pump thread will call again this function */
      pUserInstance->bEventPumpShouldDestroyUserInstance = W_TRUE;
      return;
   }

   pOtherInstance = pContext->pUserInstanceListHead;

   CDebugAssert(pOtherInstance != null);

   if(pOtherInstance == pUserInstance)
   {
      pContext->pUserInstanceListHead = pUserInstance->pNextInstance;
   }
   else
   {
      for(;;)
      {
         CDebugAssert(pOtherInstance != null);
         if(pOtherInstance->pNextInstance == pUserInstance)
         {
            pOtherInstance->pNextInstance = pUserInstance->pNextInstance;
            break;
         }
         pOtherInstance = pOtherInstance->pNextInstance;
      }
   }

#ifdef P_INCLUDE_SE_SECURITY
   PSecurityManagerDriverReleaseUserData(pContext, pUserInstance);
#endif /* P_INCLUDE_SE_SECURITY */

   CSyncDestroySemaphore(&pUserInstance->hSemaphore);
   CMemoryFree(pUserInstance);
}

/* See Client API Specifications */
void PDriverDestroy(
         tDriverInstance* pDriverInstance )
{
   tContext* pContext = (tContext*)pDriverInstance;

   if(pContext != null)
   {
      tUserInstance* pUserInstance;

      CSyncEnterCriticalSection(&pContext->hCriticalSection);

      pUserInstance = pContext->pUserInstanceListHead;
      while(pUserInstance != null)
      {
         static_PDriverPreClose(pContext, pUserInstance);
         pUserInstance = pUserInstance->pNextInstance;
      }

      while(pContext->pUserInstanceListHead != null)
      {
         PDriverCloseInternal(pContext, pContext->pUserInstanceListHead);
      }

      CSyncLeaveCriticalSection(&pContext->hCriticalSection);

      static_PContextDestroy(pContext);
   }
}

tUserInstance* PDriverOpen(
          tDriverInstance* pDriverInstance,
          const tUserIdentity* pUserIdentity )
{
   tContext* pContext = (tContext*)pDriverInstance;
   tUserInstance* pUserInstance = (tUserInstance*)CMemoryAlloc(sizeof(tUserInstance));
   if(pUserInstance == null)
   {
      return null;
   }

   CMemoryFill(pUserInstance, 0, sizeof(tUserInstance));
   pUserInstance->pUserIdentity = pUserIdentity;
   pUserInstance->pContext = pContext;

   CSyncCreateSemaphore(&pUserInstance->hSemaphore);

   CSyncEnterCriticalSection(&pContext->hCriticalSection);

   /* Add the instance at the beginning of the list */
   pUserInstance->pNextInstance = pContext->pUserInstanceListHead;
   pContext->pUserInstanceListHead = pUserInstance;

   CSyncLeaveCriticalSection(&pContext->hCriticalSection);

   return pUserInstance;
}

const tUserIdentity* PDriverGetUserIdentity(
          tUserInstance* pUserInstance )
{
   return pUserInstance->pUserIdentity;
}

void PDriverPreClose(
          tUserInstance* pUserInstance )
{
   tContext* pContext = pUserInstance->pContext;
   CSyncEnterCriticalSection(&pContext->hCriticalSection);
   static_PDriverPreClose(pContext, pUserInstance);
   CSyncLeaveCriticalSection(&pContext->hCriticalSection);
}

void PDriverClose(
          tUserInstance* pUserInstance )
{
   tContext* pContext = pUserInstance->pContext;
   CSyncEnterCriticalSection(&pContext->hCriticalSection);
   PDriverCloseInternal(pContext, pUserInstance);
   CSyncLeaveCriticalSection(&pContext->hCriticalSection);
}

extern tDriverFunction* const g_aDriverDispatchTable[];

W_ERROR PDriverIoctl(
          tUserInstance* pUserInstance,
          uint8_t nCode,
          void* pBuffer)
{
   tContext* pContext = pUserInstance->pContext;
   W_ERROR nError;

#ifdef P_DEBUG_ACTIVE
   /* Check if the instance exists*/
   tUserInstance* pOtherInstance;
   CSyncEnterCriticalSection(&pContext->hCriticalSection);
   pOtherInstance = pContext->pUserInstanceListHead;
   while(pOtherInstance != null)
   {
      if(pOtherInstance == pUserInstance)
      {
         break;
      }
      pOtherInstance = pOtherInstance->pNextInstance;
   }
   CSyncLeaveCriticalSection(&pContext->hCriticalSection);
   if(pOtherInstance == null)
   {
      CDebugAssert(W_FALSE);
      return W_ERROR_BAD_PARAMETER;
   }
#endif /* P_DEBUG_ACTIVE */

   if(nCode > P_DRIVER_FUNCTION_COUNT)
   {
      nError = W_ERROR_BAD_PARAMETER;
   }
   else
   {
      CSyncEnterCriticalSection(&pContext->hCriticalSection);
      pContext->pUserInstance = pUserInstance;
      if(nCode == P_DRIVER_FUNCTION_COUNT)
      {
         nError = PDFCDriverPumpEvent(pContext, pBuffer);
      }
      else
      {
         nError = g_aDriverDispatchTable[nCode](pContext, pBuffer);
      }
      pContext->pUserInstance = null;
      CSyncLeaveCriticalSection(&pContext->hCriticalSection);
   }

   return nError;
}



/* See Client API Specifications */
void PDriverProcessEvents(
         tDriverInstance* pDriverInstance )
{
   tContext* pContext = (tContext*)pDriverInstance;
   CDebugAssert( pContext != null );

   CSyncEnterCriticalSection(&pContext->hCriticalSection);

   pContext->bInPumpEvent = W_TRUE;

   (void)PDFCPump( pContext );

   pContext->bInPumpEvent = W_FALSE;

   CSyncLeaveCriticalSection(&pContext->hCriticalSection);
}

/* See Client API Specifications */
P_SYNC_SEMAPHORE* PDriverGetUserSemaphoreHandle(
         tUserInstance* pUserInstance )
{
   return &pUserInstance->hSemaphore;
}

#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
tContext* g_pContext = null;

/* See header file */
tUserInstance* PContextGetCurrentUserInstance(
            tContext* pContext)
{
   return &pContext->sUserInstance;
}

/* See header file */
void PContextSetCurrentUserInstance(
         tContext* pContext,
         tUserInstance* pUserInstance )
{
}

/* See Client API Specifications */
tDriverInstance* PDriverCreate(
         tPDriverCreateCompleted* pCompletionCallback,
         void* pPortingConfig,
         bool_t bForceReset,
         void* pNALBinding,
         uint32_t nNALBindingSize )
{
   tContext* pContext = static_PContextCreate(pPortingConfig, pNALBinding, nNALBindingSize);

   if(pContext != null)
   {
      g_pContext = pContext;
      PNFCControllerPerformInitialReset(pContext,
         (tPNFCControllerInitialResetCompletionCallback*)pCompletionCallback, pPortingConfig,
         bForceReset);
   }

   return (tDriverInstance*)pContext;
}

/* See Client API Specifications */
void PDriverDestroy(
         tDriverInstance* pDriverInstance )
{
#ifdef P_INCLUDE_SE_SECURITY
   if(pDriverInstance != null)
   {
      PSecurityManagerDriverReleaseUserData((tContext*)pDriverInstance, &((tContext*)pDriverInstance)->sUserInstance);
   }
#endif /* P_INCLUDE_SE_SECURITY */

   static_PContextDestroy((tContext*)pDriverInstance);
   g_pContext = null;
}

/* See Client API Specifications */
void PDriverProcessEvents(
         tDriverInstance* pDriverInstance )
{
   tContext* pContext = (tContext*)pDriverInstance;
   CDebugAssert( pContext != null );

   CSyncEnterCriticalSection(&pContext->hCriticalSection);

   pContext->bInPumpEvent = W_TRUE;

   (void)PDFCPump( pContext );

   pContext->bInPumpEvent = W_FALSE;

   CSyncLeaveCriticalSection(&pContext->hCriticalSection);
}

#endif /* P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)
tContext* g_pContext = null;

/* See header file */
bool_t PContextIsDead(
         tContext* pContext )
{
   return (pContext->bIsAlive != W_FALSE)?W_FALSE:W_TRUE;
}

/* See header file */
void PContextLockForPump(
         tContext* pContext )
{
   CSyncEnterCriticalSection(&pContext->hCriticalSection);

   pContext->bInPumpEvent = W_TRUE;
}

/* See header file */
void PContextReleaseLockForPump(
         tContext* pContext )
{
   pContext->bInPumpEvent = W_FALSE;

   CSyncLeaveCriticalSection(&pContext->hCriticalSection);
}

/* See header file */
void PContextSetIsInCallbackFlag(
         tContext* pContext,
         bool_t bIsInCallback )
{
   pContext->bIsInCallback = bIsInCallback;
}

/* See header file */
void* PContextGetUserInstance(
         tContext* pContext )
{
   return pContext->pUserInstance;
}

/* See header file */
void PContextSetUserInstance(
         tContext* pContext,
         void* pUserInstance )
{
   pContext->pUserInstance = pUserInstance;
}


/* See header file */
tContext* PContextCreate( void )
{
   tContext* pContext = static_PContextCreate(null, null, 0);

   if(pContext != null)
   {
      g_pContext = pContext;
   }

   return pContext;
}

/* See header file */
void PContextDestroy(
         tContext* pContext )
{
   static_PContextDestroy(pContext);
   g_pContext = null;
}

#endif /* P_CONFIG_USER */


/* -----------------------------------------------------------------------------
      Memory Statistics
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

void PContextDriverResetMemoryStatistics(
            tContext* pContext)
{
   CMemoryResetStatistics();
}

void PContextDriverGetMemoryStatistics(
            tContext* pContext,
            tContextDriverMemoryStatistics* pStatistics,
            uint32_t nSize )
{
   if((pStatistics != null) && (nSize == sizeof(tContextDriverMemoryStatistics)))
   {
      CMemoryGetStatistics(&pStatistics->nCurrentMemory, &pStatistics->nPeakMemory);
   }
}

#endif /* P_CONFIG_DRIVER */

/* -----------------------------------------------------------------------------
      Random function
----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

uint32_t PContextDriverGenerateRandom(
            tContext * pContext)
{
   pContext->nRandom ^= (((pContext->nRandom * 1103515245 + 12345)>>16) & 077777) << 16;
   pContext->nRandom ^= (((pContext->nRandom * 1103515245 + 12345)>>16) & 077777);

   return (pContext->nRandom);
}

void PContextDriverGenerateEntropy(
            tContext * pContext,
            uint32_t   nValue)
{
   pContext->nRandom = ((pContext->nRandom >> 8) | (pContext->nRandom << 24)) ^ nValue;
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_SYNCHRONOUS_FUNCTION_DEBUG))

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
            bool_t     bSet)
{
   pContext->bCurrentThreadIdSet = bSet;

   if (bSet != W_FALSE)
   {
      pContext->nCurrentThreadId = CSyncGetCurrentTaskIdentifier();
   }
}

/**
 * Check if the current thread ID is different from the previously set (if any)
 *
 * @param[in]  pContext  The context.
 *
 * @return W_TRUE if the current thread id is different from the thread id previously set
 *
 **/

bool_t PContextCheckCurrentThreadId(
            tContext * pContext)
{
   if ((pContext->bCurrentThreadIdSet != W_FALSE) && (pContext->nCurrentThreadId == CSyncGetCurrentTaskIdentifier()))
   {
      return W_FALSE;
   }

   return W_TRUE;
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
