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

#ifndef __NFC_HAL_DFC_H
#define __NFC_HAL_DFC_H

/*******************************************************************************
   Contains the declaration of the internal DFC function
*******************************************************************************/

/* DFC types */
#define P_DFC_TYPE_SHDLC_FRAME    ((void*)(uintptr_t)1)
#define P_DFC_TYPE_SHDLC          ((void*)(uintptr_t)2)
#define P_DFC_TYPE_HCI_FRAME      ((void*)(uintptr_t)3)
#define P_DFC_TYPE_HCI_SERVICE    ((void*)(uintptr_t)4)
#define P_DFC_TYPE_NFC_HAL_BINDING ((void*)(uintptr_t)5)

/* The DFC queue instance - Internal structure - Do not use directly */
struct __tNALDFCElement;
typedef struct __tNALDFCQueue
{
   uint32_t nFirstDFC;
   uint32_t nDFCNumber;
   uint32_t nDFCQueueSize;

   struct __tNALDFCElement* pDFCElementList;
} tNALDFCQueue;

/* Generic callback type */
typedef void tNALDFCCallback( void );

/* The DFC callback context - Internal structure - Do not use directly */
typedef struct __tNALDFCCallbackContext
{
   tNALDFCCallback* pFunction;
   void* pParameter;
   tNALBindingContext* pBindingContext;
} tNALDFCCallbackContext;

/**
 * @brief Creates a DFC queue instance.
 *
 * @pre  Only one DFC queue instance is created at a given time.
 *
 * @param[out]  pDFCQueue  The DFC queue instance to initialize.
 *
 * @param[in]  pCallbackContext  The callback context for the polling function.
 *
 * @return  W_TRUE if the DFC queue is initialized, W_FALSE otherwise.
 **/
NFC_HAL_INTERNAL bool_t PNALDFCCreate(
         tNALDFCQueue* pDFCQueue
         );

/**
 * @brief Destroyes a DFC queue instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending DFC is cancelled.
 *
 * @post  PNALDFCDestroy() does not return any error. The caller should always
 *        assume that the DFC queue instance is destroyed after this call.
 *
 * @post  The caller should never re-use the DFC queue instance value.
 *
 * @param[in]  pDFCQueue  The DFC queue to destroy.
 **/
NFC_HAL_INTERNAL void PNALDFCDestroy(
         tNALDFCQueue* pDFCQueue );

/**
 * Posts a differed function call in the queue.
 *
 * INTERNAL FUNCTION - DO NOT CALL DIRECTLY.
 *
 * Use the function PNALDFCPost<X>().
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  nFlags  The flags. Contains the number of parameters (2, 3 ,4, 5, 6).
 *
 * @param[in]  pFunction  The callback function.
 *
 * @param[in]  pParam1  The first parameter.
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
            uint32_t nParam6);

/**
 * Posts a differed function call with 0 parameter.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pFunction  The callback function.
 **/
#define PNALDFCPost0( pBindingContext, pType, pFunction ) \
            PNALDFCPostInternal6( pBindingContext, (pType), 0, (tNALDFCCallback*)(pFunction), \
               null, null, 0, 0, 0, 0 )

/**
 * Posts a differed function call with 1 parameter.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pFunction  The callback function.
 *
 * @param[in]  pParam1  The first parameter.
 **/
#define PNALDFCPost1( pBindingContext, pType, pFunction, pParam1 ) \
            PNALDFCPostInternal6( pBindingContext, (pType), 1, (tNALDFCCallback*)(pFunction), \
               (void*)(pParam1), null, 0, 0, 0, 0 )

/**
 * Posts a differed function call with 2 parameters.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pFunction  The callback function.
 *
 * @param[in]  pParam1  The first parameter.
 *
 * @param[in]  pParam2  The second parameter.
 **/
#define PNALDFCPost2( pBindingContext, pType, pFunction, pParam1, pParam2 ) \
            PNALDFCPostInternal6( pBindingContext, (pType), 2, (tNALDFCCallback*)(pFunction), \
               (void*)(pParam1), (void*)(pParam2), 0, 0, 0, 0 )

/**
 * Posts a differed function call with 3 parameters.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pFunction  The callback function.
 *
 * @param[in]  pParam1  The first parameter.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 **/
#define PNALDFCPost3( pBindingContext, pType, pFunction, pParam1, pParam2, nParam3 ) \
            PNALDFCPostInternal6( pBindingContext, (pType), 3, (tNALDFCCallback*)(pFunction), \
               (void*)(pParam1), (void*)(pParam2), (uint32_t)(nParam3), 0, 0, 0 )

/**
 * Posts a differed function call with 4 parameters.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pFunction  The callback function.
 *
 * @param[in]  pParam1  The first parameter.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 *
 * @param[in]  nParam4  The fourth parameter.
 **/
#define PNALDFCPost4( pBindingContext, pType, pFunction, pParam1, pParam2, nParam3, nParam4 ) \
            PNALDFCPostInternal6( pBindingContext, (pType), 4, (tNALDFCCallback*)(pFunction), \
               (void*)(pParam1), (void*)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), 0, 0 )

/**
 * Posts a differed function call with 5 parameters.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pFunction  The callback function.
 *
 * @param[in]  pParam1  The first parameter.
 *
 * @param[in]  pParam2  The second parameter.
 *
 * @param[in]  nParam3  The third parameter.
 *
 * @param[in]  nParam4  The fourth parameter.
 *
 * @param[in]  nParam5  The fifth parameter.
 **/
#define PNALDFCPost5( pBindingContext, pType, pFunction, pParam1, pParam2, nParam3, nParam4, nParam5 ) \
            PNALDFCPostInternal6( pBindingContext, (pType), 5, (tNALDFCCallback*)(pFunction), \
               (void*)(pParam1), (void*)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), 0 )

/**
 * Posts a differed function call with 6 parameters.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the call.
 *
 * @param[in]  pFunction  The callback function.
 *
 * @param[in]  pParam1  The first parameter.
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
#define PNALDFCPost6( pBindingContext, pType, pFunction, pParam1, pParam2, nParam3, nParam4, nParam5, nParam6 ) \
            PNALDFCPostInternal6( pBindingContext, (pType), 6, (tNALDFCCallback*)(pFunction), \
               (void*)(pParam1), (void*)(pParam2), (uint32_t)(nParam3), \
               (uint32_t)(nParam4), (uint32_t)(nParam5), (uint32_t)(nParam6) )

/**
 * Flushes the DFC of a given type.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @param[in]  pType  The type of the DFC to remove from the queue.
 **/
NFC_HAL_INTERNAL void PNALDFCFlush(
         tNALBindingContext* pBindingContext,
         void* pType );

/**
 * Executes the pending calls in the DFC queue.
 *
 * The function returns when the DFC queue is empty.
 *
 * @param[in]  pBindingContext   The context.
 *
 * @return  W_TRUE if at least a call is performed. W_FALSE if no call is perfomed.
 **/
NFC_HAL_INTERNAL bool_t PNALDFCPump(
         tNALBindingContext* pBindingContext );

#endif /* __NFC_HAL_DFC_H */
