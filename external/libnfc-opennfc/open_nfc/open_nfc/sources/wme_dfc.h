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

#ifndef __WME_DFC_H
#define __WME_DFC_H

/*******************************************************************************
   Contains the declaration of the internal DFC function
*******************************************************************************/

/* The DFC queue instance - Internal structure - Do not use directly */
struct __tDFCElement;
typedef struct __tDFCQueue
{
   uint32_t nFirstDFC;
   uint32_t nDFCNumber;
   uint32_t nDFCQueueSize;

   struct __tDFCElement* pDFCElementList;

#if (((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && defined(P_INCLUDE_JAVA_API))

   uint32_t nNextToCall;
   uint32_t nNextToAdd;
   uint32_t nElementNumber;
   struct __tDFCElement* pExternalList;

#endif /* (P_CONFIG_USER || P_CONFIG_MONOLITHIC) && P_INCLUDE_JAVA_API */

} tDFCQueue;

typedef void tDFCCallback( void );

typedef struct __tDFCExternal
{
   tDFCCallback* pFunction;
   void* pParameter;
} tDFCExternal;

/* The DFC callback context - Internal structure - Do not use directly */
typedef struct __tDFCCallbackContext
{
   tDFCCallback* pFunction;
   void* pParameter;
   tContext* pContext;
   bool_t bIsExternal;
   void* pType;

} tDFCCallbackContext;

/**
 * Fills a callback context.
 *
 * @param[in]   pContext  The context pointer received in parameter.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  The callback parameter.
 *
 * @param[out]  pCallbackContext  The callback context to fill.
 **/
void PDFCFillCallbackContext(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter,
         tDFCCallbackContext* pCallbackContext );

/**
 * Fills a typed callback context.
 *
 * @param[in]   pContext  The context pointer received in parameter.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  The callback parameter.
 *
 * @param[in]   pType  The type of the call.
 *
 * @param[out]  pCallbackContext  The callback context to fill.
 **/
void PDFCFillCallbackContextType(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter,
         void* pType,
         tDFCCallbackContext* pCallbackContext );

/**
 * Flushes the specified call.
 *
 * @param[in]   pCallbackContext  The callback context.
 **/
void PDFCFlushCall(
            tDFCCallbackContext* pCallbackContext);

/**
 * Posts a differed function call in the queue.
 *
 * INTERNAL FUNCTION - DO NOT CALL DIRECTLY.
 *
 * Use the function PDFCPostContext<X>().
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  nFlags  The flags. Contains the number of parameters (1, 2, 3)
 *             and the type of the call.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 **/
void PDFCPostInternalContext3(
            tDFCCallbackContext* pCallbackContext,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3);

/**
 * Posts a differed function call in the queue.
 *
 * INTERNAL FUNCTION - DO NOT CALL DIRECTLY.
 *
 * Use the function PDFCPostContext<X>().
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  nFlags  The flags. Contains the number of parameters (4, 5, 6).
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 *
 * @param[in]  nParam4  The fourth parameter.
 *
 * @param[in]  nParam5  The fifth parameter.
 *
 * @param[in]  nParam6  The sixth parameter.
 **/
void PDFCPostInternalContext6(
            tDFCCallbackContext* pCallbackContext,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3,
            uint32_t nParam4,
            uint32_t nParam5,
            uint32_t nParam6);

#if (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* In monolithic config, the driver callback context is equal to the user callback context */
#define tDFCDriverCC tDFCCallbackContext

#define PDFCDriverFlushCall( pDriverCC ) \
            PDFCFlushCall( &(pDriverCC) )

#define PDFCDriverFillCallbackContext  PDFCFillCallbackContext

#define tDFCDriverCCReference  tDFCCallbackContext

#define PDFCDriverPostCC1( pDriverCC ) \
            PDFCPostInternalContext3( &(pDriverCC), 1, 0, 0 )

#define PDFCDriverPostCC2( pDriverCC, pParam2 ) \
            PDFCPostInternalContext3( &(pDriverCC), 2, \
               (void*)(uintptr_t)(pParam2), 0 )

#define PDFCDriverPostCC3( pDriverCC, pParam2, nParam3 ) \
            PDFCPostInternalContext3( &(pDriverCC), 3, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3) )

#define PDFCDriverPostCC4( pDriverCC, pParam2, nParam3, nParam4 ) \
            PDFCPostInternalContext6( &(pDriverCC), 4, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), 0, 0 )

#define PDFCDriverPostCC5( pDriverCC, pParam2, nParam3, nParam4, nParam5 ) \
            PDFCPostInternalContext6( &(pDriverCC), 5, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), 0 )

#define PDFCDriverPostCC6( pDriverCC, pParam2, nParam3, nParam4, nParam5, nParam6 ) \
            PDFCPostInternalContext6( &(pDriverCC), 6, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), (uint32_t)(nParam6) )

/**
 * Gets the address of the caller buffer.
 *
 * The address of the caller buffer should not be used to access the buffer data.
 *
 * @param[in]   pDriverCC  The driver callback context.
 *
 * @param[in]   pKernelBuffer  The local buffer address.
 *
 * @return   The address of the caller buffer.
 **/
#define PDFCDriverGetCallerBufferAddress( pDriverCC, pKernelBuffer ) \
               ((void*)pKernelBuffer)

/**
 * Sets/resets the context user instance.
 *
 * If pDriverCC is defined, the context user instance is set accodingly.
 * If pDriverCC is null, the context user instance is set to null.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   pDriverCC  The driver callback context.
 **/
#define PDFCDriverSetCurrentUserInstance( pContext, pDriverCC )

#endif /* P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

/**
 * Pumps the dfc calls comming from the driver.
 *
 * @param[in]  pContext   The context.
 *
 * @param[in]  bWait  If set to W_TRUE, the function waits until an event is
 *             received or WBasicStopEventLoop() is called. If set to W_FALSE and
 *             there is no event in the event queue, the function returns
 *             immediately with the code W_ERROR_NO_EVENT.
 *
 * @return     One of the following error codes:
 *               - W_SUCCESS  An event was removed from the event queue and the
 *                 corresponding handler function is executed.
 *               - W_ERROR_NO_EVENT  The parameter bWait is set to W_FALSE and
 *                 there is no event in the event queue.
 *               - W_ERROR_WAIT_CANCELLED  The function WBasicStopEventLoop()
 *                 was called.
 *               - others  If any other error occurred.
 **/
W_ERROR PDFCDriverPumpEvent(
            tContext * pContext,
            bool_t bWait );

#endif /* P_CONFIG_USER  */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

#ifdef P_CONFIG_CLIENT_SERVER

/**
 * @brief  Calls a server function.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nCode  The command code.
 *
 * @param[in-out]  pParamInOut  The input/output parameters, may be null.
 *
 * @param[in]  nSizeIn  The size in bytes of the input parameters.
 *
 * @param[in]  pBuffer1  The input buffer 1, may be null.
 *
 * @param[in]  nBuffer1Length  The length in bytes of the input buffer 1.
 *
 * @param[in]  pBuffer2  The input buffer 2, may be null.
 *
 * @param[in]  nBuffer2Length  The length in bytes of the input buffer 2.
 *
 * @param[in]  nSizeOut  The size in bytes of the output parameters.
 *
 * @return  The error returned by the communication functions.
 **/
W_ERROR PDFCClientCallFunction(
            tContext * pContext,
            uint8_t nCode,
            void* pParamInOut,
            uint32_t nSizeIn,
            const void* pBuffer1,
            uint32_t nBuffer1Length,
            const void* pBuffer2,
            uint32_t nBuffer2Length,
            uint32_t nSizeOut);

#endif /* #ifdef P_CONFIG_CLIENT_SERVER */

#endif /* P_CONFIG_USER  */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

struct __tDFCDriverCallbackContext;

/* typedef struct __tDFCDriverCallbackContext* tDFCCallbackContext; */

typedef struct __tDFCDriverCallbackContext tDFCDriverCC;

typedef tDFCDriverCC* tDFCDriverCCReference;

#define PDFCDriverPostCC1( pDriverCC ) \
            PDFCDriverInternalPostCC3( pContext, &(pDriverCC), 1, 0, 0 )

#define PDFCDriverPostCC2( pDriverCC, pParam2 ) \
            PDFCDriverInternalPostCC3( pContext, &(pDriverCC), 2, \
               (void*)(uintptr_t)(pParam2), 0 )

#define PDFCDriverPostCC3( pDriverCC, pParam2, nParam3 ) \
            PDFCDriverInternalPostCC3( pContext, &(pDriverCC), 3, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3) )

#define PDFCDriverPostCC4( pDriverCC, pParam2, nParam3, nParam4 ) \
            PDFCDriverInternalPostCC6( pContext, &(pDriverCC), 4, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), 0, 0 )

#define PDFCDriverPostCC5( pDriverCC, pParam2, nParam3, nParam4, nParam5 ) \
            PDFCDriverInternalPostCC6( pContext, &(pDriverCC), 5, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), 0 )

#define PDFCDriverPostCC6( pDriverCC, pParam2, nParam3, nParam4, nParam5, nParam6 ) \
            PDFCDriverInternalPostCC6( pContext, &(pDriverCC), 6, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), (uint32_t)(nParam6) )

#define PDFCDriverFlushCall(pDriverCC) \
               PDFCDriverInternalFlushCall(pContext, pDriverCC)

/**
 * Fluses the DFC of the current client.
 *
 * @param[in]  pContext   The context.
 **/
void PDFCDriverFlushClient(
         tContext* pContext );

/**
 * Pumps the dfc calls comming from the driver.
 *
 * @param[in]  pContext   The context.
 *
 * @param[in]  pParams  The parameter list.
 *
 * @return     One of the following error codes:
 *               - W_SUCCESS  An event was removed from the event queue and the
 *                 corresponding handler function is executed.
 *               - W_ERROR_NO_EVENT  The parameter bWait is set to W_FALSE and
 *                 there is no event in the event queue.
 *               - W_ERROR_WAIT_CANCELLED  The function WBasicStopEventLoop()
 *                 was called.
 *               - others  If any other error occurred.
 **/
W_ERROR PDFCDriverPumpEvent(
            tContext * pContext,
            void* pParams );

/**
 * Allocates a callback context.
 *
 * @param[in]  pContext   The context.
 *
 * @return The callback context or null if there is not enough resources.
 **/
tDFCDriverCC* PDFCDriverAllocateCC(
         tContext* pContext );

/**
 * Allocates a callback context for an external callback function.
 *
 * @param[in]   pContext   The context.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  The callback parameter.
 *
 * @return The callback context or null if there is not enough resources.
 **/
tDFCDriverCC* PDFCDriverAllocateCCExternal(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter);

/**
 * Allocates a callback context for an internal callback function.
 *
 * @param[in]   pContext   The context.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  The callback parameter.
 *
 * @return The callback context or null if there is not enough resources.
 **/
tDFCDriverCC* PDFCDriverAllocateCCFunction(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter);

/**
 * Allocates a callback context for an external event handler function.
 *
 * @param[in]   pContext   The context.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  The callback parameter.
 *
 * @return The callback context or null if there is not enough resources.
 **/
tDFCDriverCC* PDFCDriverAllocateCCExternalEvent(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter);

/**
 * Allocates a callback context for an internal event handler function.
 *
 * @param[in]   pContext   The context.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  The callback parameter.
 *
 * @return The callback context or null if there is not enough resources.
 **/
tDFCDriverCC* PDFCDriverAllocateCCFunctionEvent(
         tContext* pContext,
         tDFCCallback* pCallbackFunction,
         void* pCallbackParameter);

/**
 * Fills a driver callback context.
 *
 * @param[in]   pContext  The context pointer received in parameter.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  The callback parameter.
 *
 * @param[out]  ppDriverCC  The driver callback context to fill.
 **/
#define PDFCDriverFillCallbackContext( pContext, pCallbackFunction, pCallbackParameter, ppDriverCC ) \
         { CDebugAssert(pCallbackFunction == null); \
           *(ppDriverCC) = (tDFCDriverCC*)(pCallbackParameter); }

/**
 * Gets the address of the caller buffer.
 *
 * The address of the caller buffer should not be used to access the buffer data.
 *
 * @param[in]   pCallbackContext  The callback context.
 *
 * @param[in]   pKernelBuffer  The local buffer address.
 *
 * @return   The address of the caller buffer.
 **/
void* PDFCDriverGetCallerBufferAddress(
         tDFCDriverCC* pDriverCC,
         void* pKernelBuffer );

/**
 * Sets/resets the context user instance.
 *
 * If pDriverCC is defined, the context user instance is set accodingly.
 * If pDriverCC is null, the context user instance is set to null.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   pDriverCC  The driver callback context.
 **/
void PDFCDriverSetCurrentUserInstance(
         tContext* pContext,
         tDFCDriverCC* pDriverCC );

/**
 * Registers a user buffer in the driver callback context.
 *
 * @param[in]  pDriverCC  The driver callback context.
 *
 * @param[in]  pUserBuffer The pointer on the user buffer.
 *
 * @param[in]  nBufferSize  The buffer size in bytes.
 *
 * @param[in]  nType  The buffer type.
 *
 * @return  The kernel address of the buffer.
 *          The value is null if the user buffer is null or empty.
 *          The value is 1 if an error of mapping is detected.
 **/
void* PDFCDriverRegisterUserBuffer(
         tDFCDriverCC* pDriverCC,
         void* pUserBuffer,
         uint32_t nBufferSize,
         uint32_t nType );

/**
 * Registers the user address of a word in the driver callback context.
 *
 * @param[in]  pDriverCC  The driver callback context.
 *
 * @param[in]  pUserWord The pointer on the user word.
 *
 * @param[in]  nType  The buffer type.
 *
 * @return  The kernel address of the buffer.
 *          The value is null if the user buffer is null.
 *          The value is 1 if an error of mapping is detected.
 **/
uint32_t* PDFCDriverRegisterUserWordBuffer(
         tDFCDriverCC* pDriverCC,
         void* pUserWord,
         uint32_t nType );

/**
 * Synchronizes the content of the kernel buffer with the content of the user buffer.
 *
 * @param[in]  pDriverCC  The driver callback context.
 **/
void PDFCDriverSynchronizeUserBuffer(
         tDFCDriverCC* pDriverCC );

/**
 * Frees a driver callback context.
 *
 * @param[in]  pDriverCC  The driver callback context.
 **/
void PDFCDriverFreeCC(
         tDFCDriverCC* pDriverCC );

/**
 * Flushes the specified call.
 *
 * @param[in]   pDriverCC  The driver callback context.
 **/

void PDFCDriverInternalFlushCall(
            tContext * pContext,
            tDFCDriverCC* pDriverCC);

/**
 * Checks if the specified DFC is still present in the user instance
 *
 * @param[in]   pInstance  The user instance
 * @param[in]   pDriverCC  The driver callback context.
 *
 * @return W_TRUE if the driver DFC still exists.
 **/

bool_t PDFCDriverCheckDriverDFCInUserInstance(
      tUserInstance * pInstance,
      tDFCDriverCC * pDriverCC);

/**
 * Posts a differed function call in the queue.
 *
 * INTERNAL FUNCTION - DO NOT CALL DIRECTLY.
 *
 * Use the function PDFCDriverPostCC<X>().
 *
 * @param[in]  pDriverCC   The driver callback context.
 *
 * @param[in]  nFlags  The flags. Contains the number of parameters (1, 2, 3)
 *             and the type of the call.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 **/
void PDFCDriverInternalPostCC3(
            tContext * pContext,
            tDFCDriverCC** ppDriverCC,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3);

/**
 * Posts a differed function call in the queue.
 *
 * INTERNAL FUNCTION - DO NOT CALL DIRECTLY.
 *
 * Use the function PDFCDriverPostCC<X>().
 *
 * @param[in]  pDriverCC   The driver callback context.
 *
 * @param[in]  nFlags  The flags. Contains the number of parameters (4, 5, 6).
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 *
 * @param[in]  nParam4  The fourth parameter.
 *
 * @param[in]  nParam5  The fifth parameter.
 *
 * @param[in]  nParam6  The sixth parameter.
 **/
void PDFCDriverInternalPostCC6(
            tContext * pContext,
            tDFCDriverCC ** ppDriverCC,
            uint32_t nFlags,
            void* pParam2,
            uint32_t nParam3,
            uint32_t nParam4,
            uint32_t nParam5,
            uint32_t nParam6);

#endif /* P_CONFIG_DRIVER */

/**
 * @brief Creates a DFC queue instance.
 *
 * @pre  Only one DFC queue instance is created at a given time.
 *
 * @param[out]  pDFCQueue  The DFC queue instance to initialize.
 *
 * @return  W_TRUE if the DFC queue is initialized, W_FALSE otherwise.
 **/
bool_t PDFCCreate(
         tDFCQueue* pDFCQueue );

/**
 * @brief Destroyes a DFC queue instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending DFC is cancelled.
 *
 * @post  PDFCDestroy() does not return any error. The caller should always
 *        assume that the DFC queue instance is destroyed after this call.
 *
 * @post  The caller should never re-use the DFC queue instance value.
 *
 * @param[in]  pDFCQueue  The DFC queue to destroy.
 **/
void PDFCDestroy(
         tDFCQueue* pDFCQueue );


/**
 * Posts a differed function call with 0 parameter. The callback parameter is not used.
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  pType  The type of the call.
 **/
#define PDFCPostContext0( pCallbackContext ) \
            PDFCPostInternalContext3( pCallbackContext, 0, 0, 0 )

/**
 * Posts a differed function call with 1 parameters.
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  pType  The type of the call.
 **/
#define PDFCPostContext1( pCallbackContext ) \
            PDFCPostInternalContext3( pCallbackContext, 1, 0, 0 )

/**
 * Posts a differed function call with 2 parameters.
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pParam2  The second parameter.
 **/
#define PDFCPostContext2( pCallbackContext, pParam2 ) \
            PDFCPostInternalContext3( pCallbackContext, 2, \
               (void*)(uintptr_t)(pParam2), 0 )

/**
 * Posts a differed function call with 3 parameters.
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 **/
#define PDFCPostContext3( pCallbackContext, pParam2, nParam3 ) \
            PDFCPostInternalContext3( pCallbackContext, 3, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3) )

/**
 * Posts a differed function call with 4 parameters.
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 *
 * @param[in]  nParam4  The fourth parameter.
 **/
#define PDFCPostContext4( pCallbackContext, pParam2, nParam3, nParam4 ) \
            PDFCPostInternalContext6( pCallbackContext, 4, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), 0, 0 )

/**
 * Posts a differed function call with 5 parameters.
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 *
 * @param[in]  nParam4  The fourth parameter.
 *
 * @param[in]  nParam5  The fifth parameter.
 **/
#define PDFCPostContext5( pCallbackContext, pParam2, nParam3, nParam4, nParam5 ) \
            PDFCPostInternalContext6( pCallbackContext, 5, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), 0 )

/**
 * Posts a differed function call with 6 parameters.
 *
 * @param[in]  pCallbackContext   The callback context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 *
 * @param[in]  nParam4  The fourth parameter.
 *
 * @param[in]  nParam5  The fifth parameter.
 *
 * @param[in]  nParam6  The sixth parameter.
 **/
#define PDFCPostContext6( pCallbackContext, pParam2, nParam3, nParam4, nParam5, nParam6 ) \
            PDFCPostInternalContext6( pCallbackContext, 6, \
               (void*)(uintptr_t)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), (uint32_t)(nParam6) )

/**
 * Flushes the DFC of a given type.
 *
 * @param[in]  pContext   The context.
 *
 * @param[in]  pType  The type of the DFC to remove from the queue.
 **/
void PDFCFlush(
         tContext* pContext,
         void* pType );

/**
 * Executes the pending calls in the DFC queue.
 *
 * The function returns when the DFC queue is empty.
 *
 * @param[in]  pContext   The context.
 *
 * @return  W_TRUE if at least a call is performed. W_FALSE if no call is perfomed.
 **/
bool_t PDFCPump(
         tContext* pContext );

#endif /* __WME_DFC_H */
