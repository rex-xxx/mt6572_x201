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
   Contains the ISO7816-4 implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( 78164 )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Queued operation type */
#define P_7816_QUEUED_NONE                0
#define P_7816_QUEUED_EXCHANGE            1
#define P_7816_QUEUED_OPEN_CHANNEL        2
#define P_7816_QUEUED_CLOSE               3

struct __tP7816SmRawInstance
{
   W_HANDLE hConnection;
   W_HANDLE hCurrentOperation;
};

struct __t7816Connection;

/* The structure of the 7816 channel */
typedef struct __tP7816Channel
{
   /* Header for the object registry */
   tHandleObjectHeader sObjectHeader;

   struct __t7816Connection* p7816Connection;
   uint32_t nChannelReference;
   bool_t bInternalChannel;

} tP7816Channel;

/* Destroy channel callback */
static uint32_t static_P7816DestroyChannelAsync(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            void* pObject );

/* Handle type for a logical channel */
tHandleType g_s7816LogicalChannel =
   { null, static_P7816DestroyChannelAsync, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_7816_CHANNEL (&g_s7816LogicalChannel)

static void static_P7816RawExchangeApdu(
                  tContext* pContext,
                  tP7816SmRawInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength);

static tP7816SmRawInterface static_7816RawInterface=
{
   static_P7816RawExchangeApdu
};

#define P_7816_4_ATR_MAX_LENGTH      0x30

/* Declare an exchange APDU structure for asynchronous mode */
typedef struct __t7816Connection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* hConnection handle */
   W_HANDLE                   hConnection;

   /* Callback context used by method called from user */
   tDFCCallbackContext        sCallbackContext;
   /* Callback context used by method called from another connection */
   tDFCCallbackContext        sCallbackContextInternal;

   /*
    * Pointer to a dynamically allocated structure containing a pointer to the sSmRawInstance field.
    * See also P7816CreateConnection and static_P7816RawExchangeApdu
    */
   tP7816SmRawInstance** ppSmRawInstance;

   tP7816SmRawInstance sSmRawInstance;
   tP7816SmInterface* pSmInterface;
   tP7816SmInstance* pSmInstance;

   /* true if the current channel operation (open/close) is internal (from ndef_type4) */
   bool_t bInternalRawChannelOperation;

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   /* This raw channel is used for P7816ExchangeApduInternal or the DEPRECATED behaviour of P7816ExchangeAPDU
      when the connection handle is directly used to ExchangeAPDU. */
   uint32_t nInternalSmRawChannelReference;

   /* Buffer used for the "deprecated" behavior of W7816GetResponseAPDU */
   uint8_t* pDeprecatedResponseBuffer;
   uint32_t nDeprecatedResponseBufferLength;

   /* Parameters passed to P7816ExchangeData (used with nInternalSmRawChannelReference) */
   const uint8_t* pSendAPDUBuffer;
   uint32_t nSendAPDUBufferLength;
   uint8_t* pReceivedAPDUBuffer;
   uint32_t nReceivedAPDUBufferMaxLength;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   /* Information to be notified when P7816ExchangeData returns (used with nInternalSmRawChannelReference) */
   uint32_t nDataLength;
   W_ERROR nCloseResult;

   uint32_t nATRLength;
   uint8_t aATR[P_7816_4_ATR_MAX_LENGTH];

   /* True if a 7816 exchange is pending. Used for SE or LogicalChannel. */
   bool_t bExchangePending;

   /* Hold data of the queued operation which will be executed after the polling completion */
   struct __tQueuedOperation
   {
      /* Type of operation: Read, Lock, Write... */
      uint32_t             nType;
      /* Exchange Data */
      tP7816Channel*       pChannel;
      const uint8_t*       pSendAPDUBuffer;
      uint32_t             nSendAPDUBufferLength;
      uint8_t*             pReceivedAPDUBuffer;
      uint32_t             nReceivedAPDUBufferMaxLength;
      /* Open channel data */
      uint32_t nChannelType;
      const uint8_t*       pAID;
      uint32_t             nAIDLength;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hCurrentOperation;
   } sQueuedOperation;

} t7816Connection;

/* Destroy connection callback */
static uint32_t static_P7816DestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static uint32_t static_P7816GetPropertyNumber(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static bool_t static_P7816GetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_P7816CheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Handle registry 7816 type */
tHandleType g_s7816Connection = {   static_P7816DestroyConnection,
                                    null,
                                    static_P7816GetPropertyNumber,
                                    static_P7816GetProperties,
                                    static_P7816CheckProperties,
                                    null, null, null, null };

#define P_HANDLE_TYPE_7816_CONNECTION (&g_s7816Connection)

/**
 * @brief   Creates a 7816 channel instance.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nChannelReference  The SM channel reference.
 *
 * @param[out]  phChannel  A pointer on a variable valued with the new object handle
 *
 * @return The error code.
 **/
static W_ERROR static_P7816CreateChannelInstance(
                  tContext* pContext,
                  struct __t7816Connection* p7816Connection,
                  bool_t bInternalRawChannel,
                  uint32_t nChannelReference,
                  W_HANDLE* phChannel)
{
   tP7816Channel* pChannel = (tP7816Channel*)null;
   W_ERROR nError = W_SUCCESS;
   W_HANDLE hChannel = W_NULL_HANDLE;

   pChannel = (tP7816Channel*)CMemoryAlloc(sizeof(tP7816Channel));
   if(pChannel == null)
   {
      PDebugError("static_P7816CreateChannelInstance: cannot allocate the channel memory");
      return W_ERROR_OUT_OF_RESOURCE;
   }

   nError = PHandleRegister(pContext, pChannel, P_HANDLE_TYPE_7816_CHANNEL, &hChannel);

   if(nError == W_SUCCESS)
   {
      pChannel->nChannelReference = nChannelReference;
      pChannel->p7816Connection = p7816Connection;
      pChannel->bInternalChannel = bInternalRawChannel;
   }
   else
   {
      PDebugError("static_P7816CreateChannelInstance: PHandleRegister returns error %s", PUtilTraceError(nError));
      CMemoryFree(pChannel);
      hChannel = W_NULL_HANDLE;
   }

   *phChannel = hChannel;
   return nError;
}

/**
 * @brief   Destroyes a 7816 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_P7816DestroyConnection(
            tContext* pContext,
            void* pObject )
{
   t7816Connection* p7816Connection = (t7816Connection*)pObject;

   PDebugTrace("static_P7816DestroyConnection");

   PDFCFlushCall(&p7816Connection->sCallbackContext);

   if (p7816Connection->ppSmRawInstance != null)
   {
      CMemoryFree(p7816Connection->ppSmRawInstance);
      p7816Connection->ppSmRawInstance = (tP7816SmRawInstance**)null;
   }

   /* No risk here, the connection object is only destroyed if all raw/basic/logical channels are already destroyed */
   /* NOTE: There is no need to delete the instance when the interface is g_sPSEUserSmInterface */
   if (p7816Connection->pSmInterface == &g_sP7816SmInterface)
   {
      P7816SmDestroyInstance(pContext, p7816Connection->pSmInstance);
      p7816Connection->pSmInstance = (tP7816SmInstance*)null;
   }

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   if(p7816Connection->pDeprecatedResponseBuffer != null)
   {
      CMemoryFree(p7816Connection->pDeprecatedResponseBuffer);
      p7816Connection->pDeprecatedResponseBuffer = (uint8_t*)null;
      p7816Connection->nDeprecatedResponseBufferLength = 0;
   }
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   CMemoryFree( p7816Connection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the 7816 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 **/
static uint32_t static_P7816GetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}


/**
 * @brief   Gets the 7816 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_P7816GetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   PDebugTrace("static_P7816GetProperties");

   pPropertyArray[0] = W_PROP_ISO_7816_4;

   return W_TRUE;
}

/**
 * @brief   Checkes the 7816 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_P7816CheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   PDebugTrace(
      "static_P7816CheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( nPropertyValue == W_PROP_ISO_7816_4 )
   {
      return W_TRUE;
   }
   else
   {
      return W_FALSE;
   }
}

/* For a non READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
   Manage the own 7816 echange pending flag */
static W_ERROR static_P7816NotifyExchange(t7816Connection* p7816Connection)
{
   if (p7816Connection->bExchangePending != W_FALSE)
   {
      /* A 7816 operation is already pending */
      return W_ERROR_BAD_STATE;
   }
   else
   {
      /* Set the 7816 exchange pending flag */
      p7816Connection->bExchangePending = W_TRUE;
      return W_SUCCESS;
   }
}

/* For a non READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
   Manage the own 7816 echange pending flag */
static void static_P7816NotifyExchangeCompletion(t7816Connection* p7816Connection)
{
   /* Reset the 7816 exchange pending flag */
   p7816Connection->bExchangePending = W_FALSE;
}


/**
 * @brief   Manage completion of an exchange data from user (not internal from another connection). Cleanup and send result.
 *          See tWBasicGenericDataCallbackFunction
 **/
static void static_P7816ExchangeDataCompleted(
            tContext*   pContext,
            void*       pCallbackParameter,
            uint32_t    nDataLength,
            W_ERROR     nError)
{
   t7816Connection* p7816Connection = (t7816Connection*) pCallbackParameter;

   PDebugTrace("static_P7816ExchangeDataCompleted: nError %s", PUtilTraceError(nError));

   if (p7816Connection->sSmRawInstance.hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, p7816Connection->sSmRawInstance.hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_P7816ExchangeDataCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, p7816Connection->sSmRawInstance.hCurrentOperation);
      PHandleClose(pContext, p7816Connection->sSmRawInstance.hCurrentOperation);
      p7816Connection->sSmRawInstance.hCurrentOperation = W_NULL_HANDLE;
   }

   /* Manage user connection status and polling */
   if(PReaderCheckConnection(pContext, p7816Connection->hConnection) != W_FALSE)
   {
      PReaderNotifyExchangeCompletion(pContext, p7816Connection->hConnection);
   }
   else
   {
      /* For a non READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
         Manage the own 7816 echange pending flag */
      static_P7816NotifyExchangeCompletion(p7816Connection);
   }

   /* Post result */
   PDFCPostContext3(
      &p7816Connection->sCallbackContext,
      nDataLength,
      nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p7816Connection);
}

/**
 * @brief   Manage completion of an open basic/logical channel from user. Cleanup and send result.
 *          See tPBasicGenericHandleCallbackFunction
 **/
static void static_P7816OpenChannelCompleted(
            tContext*   pContext,
            void*       pCallbackParameter,
            W_HANDLE    hChannel,
            W_ERROR     nError)
{
   t7816Connection* p7816Connection = (t7816Connection*) pCallbackParameter;

   PDebugTrace("static_P7816OpenChannelCompleted: nError %s", PUtilTraceError(nError));

   if (p7816Connection->sSmRawInstance.hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, p7816Connection->sSmRawInstance.hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_P7816OpenChannelCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, p7816Connection->sSmRawInstance.hCurrentOperation);
      PHandleClose(pContext, p7816Connection->sSmRawInstance.hCurrentOperation);
      p7816Connection->sSmRawInstance.hCurrentOperation = W_NULL_HANDLE;
   }

   /* Manage user connection status and polling */
   if(PReaderCheckConnection(pContext, p7816Connection->hConnection) != W_FALSE)
   {
      PReaderNotifyExchangeCompletion(pContext, p7816Connection->hConnection);
   }
   else
   {
      /* For a non READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
         Manage the own 7816 echange pending flag */
      static_P7816NotifyExchangeCompletion(p7816Connection);
   }

   /* Post result */
   PDFCPostContext3(
      &p7816Connection->sCallbackContext,
      hChannel,
      nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p7816Connection);
}

/**
 * @brief   Manage completion of a close logical channel from user. Cleanup and send result.
 *          See tPBasicGenericCallbackFunction
 **/
static void static_P7816CloseLogicalChannelCompleted(
            tContext*   pContext,
            void*       pCallbackParameter,
            W_ERROR     nError)
{
   t7816Connection* p7816Connection = (t7816Connection*) pCallbackParameter;

   PDebugTrace("static_P7816CloseLogicalChannelCompleted: nError %s", PUtilTraceError(nError));

   if (p7816Connection->sSmRawInstance.hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, p7816Connection->sSmRawInstance.hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_P7816CloseLogicalChannelCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, p7816Connection->sSmRawInstance.hCurrentOperation);
      PHandleClose(pContext, p7816Connection->sSmRawInstance.hCurrentOperation);
      p7816Connection->sSmRawInstance.hCurrentOperation = W_NULL_HANDLE;
   }

   if (p7816Connection->bInternalRawChannelOperation == W_FALSE)
   {
      /* Manage user connection status and polling */
      if(PReaderCheckConnection(pContext, p7816Connection->hConnection) != W_FALSE)
      {
         PReaderNotifyExchangeCompletion(pContext, p7816Connection->hConnection);
      }
      else
      {
         /* For a non READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
            Manage the own 7816 echange pending flag */
         static_P7816NotifyExchangeCompletion(p7816Connection);
      }
   }
   else
   {
      /* user connection status is ignored for internal operation */
      p7816Connection->bInternalRawChannelOperation = W_FALSE;
   }

   /* Post result */
   PDFCPostContext2(
      &p7816Connection->sCallbackContext,
      nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p7816Connection);
}

static void static_P7816RawExchangeApdu(
                  tContext* pContext,
                  tP7816SmRawInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   W_HANDLE hSubOperation = W_NULL_HANDLE;

   /* Beware! pInstance is actually a pointer to a tP7816SmRawInstance* - see P7816CreateConnection */
   CDebugAssert(pInstance != null);
   pInstance = *(tP7816SmRawInstance**)pInstance;
   CDebugAssert(pInstance != null);
   CDebugAssert(pInstance->hConnection != W_NULL_HANDLE);

   PReaderExchangeDataInternal(
      pContext,
      pInstance->hConnection,
      pCallback,
      pCallbackParameter,
      pSendApduBuffer,
      nSendApduBufferLength,
      pReceivedApduBuffer,
      nReceivedApduBufferMaxLength,
      ((pInstance->hCurrentOperation != W_NULL_HANDLE) ? &hSubOperation : null));

   if (pInstance->hCurrentOperation != W_NULL_HANDLE)
   {
      W_ERROR nResult = PBasicAddSubOperationAndClose(
         pContext,
         pInstance->hCurrentOperation,
         hSubOperation);

      if (nResult != W_SUCCESS)
      {
         PDebugTrace("static_P7816RawExchangeApdu: PBasicAddSubOperationAndClose failed with %s", PUtilTraceError(nResult));

         PHandleClose(pContext, hSubOperation);
         hSubOperation = W_NULL_HANDLE;
      }
   }
}

static void static_P7816SmInterfaceExchangeApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDataLength,
                  W_ERROR nResult )
{
   t7816Connection* p7816Connection = (t7816Connection*)pCallbackParameter;

   PDFCPostContext3(
      &p7816Connection->sCallbackContextInternal,
      nDataLength,
      nResult);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p7816Connection);
}

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

static void static_P7816RawChannelExchangeDataCloseRawChannelCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            W_ERROR nResult)
{
   t7816Connection* p7816Connection = (t7816Connection*)pCallbackParameter;

   CDebugAssert(p7816Connection->nInternalSmRawChannelReference != P_7816SM_NULL_CHANNEL);
   p7816Connection->nInternalSmRawChannelReference = P_7816SM_NULL_CHANNEL;

   /* Notify result to caller */
   static_P7816SmInterfaceExchangeApduCompleted(pContext, pCallbackParameter,
      p7816Connection->nDataLength, p7816Connection->nCloseResult);
}

static void static_P7816RawChannelExchangeApduCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nResult )
{
   t7816Connection* p7816Connection = (t7816Connection*)pCallbackParameter;

   /* Remember the result returned by the ExchangeData function */
   p7816Connection->nCloseResult = nResult;
   p7816Connection->nDataLength = nDataLength;

   /* Automatic loading of the response data for the Raw channel used for the deprecated behavior */
   if(p7816Connection->pDeprecatedResponseBuffer != null)
   {
      CMemoryFree(p7816Connection->pDeprecatedResponseBuffer);
      p7816Connection->pDeprecatedResponseBuffer = (uint8_t*)null;
      p7816Connection->nDeprecatedResponseBufferLength = 0;
   }
   if(nResult == W_ERROR_BUFFER_TOO_SHORT)
   {
      if(nDataLength != 0)
      {
         p7816Connection->pDeprecatedResponseBuffer = (uint8_t*)CMemoryAlloc(nDataLength);
         if(p7816Connection->pDeprecatedResponseBuffer == null)
         {
            PDebugError("static_P7816RawChannelExchangeApduCompleted: cannot allocate the memory buffer");
         }
         else
         {
            if(p7816Connection->pSmInterface->pGetData(
               pContext,
               p7816Connection->pSmInstance,
               p7816Connection->nInternalSmRawChannelReference,
               P_7816SM_DATA_TYPE_LAST_RESPONSE_APDU,
               p7816Connection->pDeprecatedResponseBuffer,
               nDataLength,
               &p7816Connection->nDeprecatedResponseBufferLength) != W_SUCCESS)
            {
               PDebugError("static_P7816RawChannelExchangeApduCompleted: error returned by the get data function");
               CMemoryFree(p7816Connection->pDeprecatedResponseBuffer);
               p7816Connection->pDeprecatedResponseBuffer = (uint8_t*)null;
               p7816Connection->nDeprecatedResponseBufferLength = 0;
            }
         }
      }
   }

   /* Close raw channel */
   nResult = p7816Connection->pSmInterface->pCloseChannel(pContext,
      p7816Connection->pSmInstance,
      p7816Connection->nInternalSmRawChannelReference,
      static_P7816RawChannelExchangeDataCloseRawChannelCompleted,
      pCallbackParameter);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_P7816RawChannelExchangeDataCloseRawChannelCompleted(pContext, pCallbackParameter, nResult);
   }
}

static void static_P7816RawChannelExchangeDataOpenRawChannelCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nChannelReference,
            W_ERROR nResult)
{
   t7816Connection* p7816Connection = (t7816Connection*)pCallbackParameter;

   if (nResult != W_SUCCESS)
   {
      static_P7816SmInterfaceExchangeApduCompleted(pContext, p7816Connection, 0, nResult);
      return;
   }

   CDebugAssert(nChannelReference != P_7816SM_NULL_CHANNEL);
   p7816Connection->nInternalSmRawChannelReference = nChannelReference;

   nResult = p7816Connection->pSmInterface->pExchangeApdu(
      pContext,
      p7816Connection->pSmInstance,
      nChannelReference,
      static_P7816RawChannelExchangeApduCompleted,
      p7816Connection,
      p7816Connection->pSendAPDUBuffer,
      p7816Connection->nSendAPDUBufferLength,
      p7816Connection->pReceivedAPDUBuffer,
      p7816Connection->nReceivedAPDUBufferMaxLength);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      CDebugAssert(nResult != W_SUCCESS);
      static_P7816RawChannelExchangeApduCompleted(pContext, p7816Connection, 0, nResult);
      return;
   }
}

#endif /* P_INCLUDE_DEPRECATED_FUNCTIONS */

static void static_P7816ExchangeData(
            tContext*            pContext,
            t7816Connection*     p7816Connection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void*                pCallbackParameter,
            tP7816Channel*       pChannel,
            const uint8_t*       pSendAPDUBuffer,
            uint32_t             nSendAPDUBufferLength,
            uint8_t*             pReceivedAPDUBuffer,
            uint32_t             nReceivedAPDUBufferMaxLength)
{
   W_ERROR nResult;
   uint32_t nChannelReference;

   /* Build internal callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &p7816Connection->sCallbackContextInternal);

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P7816SmInterfaceExchangeApduCompleted when the operation is completed */
   PHandleIncrementReferenceCount(p7816Connection);

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   if (pChannel == null)
   {
      /* pChannel is null with the DEPRECATED behaviour of P7816ExchangeAPDU */
      p7816Connection->pSendAPDUBuffer = pSendAPDUBuffer;
      p7816Connection->nSendAPDUBufferLength = nSendAPDUBufferLength;
      p7816Connection->pReceivedAPDUBuffer = pReceivedAPDUBuffer;
      p7816Connection->nReceivedAPDUBufferMaxLength = nReceivedAPDUBufferMaxLength;
      p7816Connection->nDataLength = 0;
      p7816Connection->nCloseResult = W_ERROR_UICC_COMMUNICATION;

      nResult = p7816Connection->pSmInterface->pOpenChannel(pContext, p7816Connection->pSmInstance,
         static_P7816RawChannelExchangeDataOpenRawChannelCompleted, p7816Connection,
         W_7816_CHANNEL_TYPE_RAW,
         (const uint8_t*)null, 0);

      if (nResult != W_ERROR_OPERATION_PENDING)
      {
         static_P7816SmInterfaceExchangeApduCompleted(pContext, p7816Connection, 0, nResult);
         return;
      }

      return;
   }
   else
#endif /* P_INCLUDE_DEPRECATED_FUNCTIONS */
   {
      /* A 7816 State Machine channel is already opened. */
      nChannelReference = pChannel->nChannelReference;
   }

   /* Send APDU */
   nResult = p7816Connection->pSmInterface->pExchangeApdu(
      pContext,
      p7816Connection->pSmInstance,
      nChannelReference,
      static_P7816SmInterfaceExchangeApduCompleted,
      p7816Connection,
      pSendAPDUBuffer,
      nSendAPDUBufferLength,
      pReceivedAPDUBuffer,
      nReceivedAPDUBufferMaxLength);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_P7816SmInterfaceExchangeApduCompleted(pContext, p7816Connection, 0, nResult);
      return;
   }
}

static void static_P7816SmInterfaceOpenChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nChannelReference,
                  W_ERROR nResult )
{
   t7816Connection* p7816Connection = (t7816Connection*)pCallbackParameter;
   W_HANDLE hChannel = W_NULL_HANDLE;

   if (nResult == W_SUCCESS)
   {
      nResult = static_P7816CreateChannelInstance(pContext, p7816Connection, p7816Connection->bInternalRawChannelOperation, nChannelReference, &hChannel);
      if(nResult != W_SUCCESS)
      {
         PDebugError("static_P7816SmInterfaceOpenChannelCompleted: static_P7816CreateChannelInstance returns error %s", PUtilTraceError(nResult));
      }
   }
   else
   {
      PDebugError("static_P7816SmInterfaceOpenChannelCompleted: returns error %s", PUtilTraceError(nResult));
   }

   PDFCPostContext3(
      &p7816Connection->sCallbackContextInternal,
      hChannel,
      nResult);

   /* Clear this flag since operation is completed */
   p7816Connection->bInternalRawChannelOperation = W_FALSE;

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p7816Connection);
}

static void static_P7816OpenChannel(
            tContext*            pContext,
            t7816Connection*     p7816Connection,
            tPBasicGenericHandleCallbackFunction* pCallback,
            void*                pCallbackParameter,
            bool_t               bInternalRawChannel,
            uint32_t             nChannelType,
            const uint8_t*       pAID,
            uint32_t             nAIDLength)
{
   W_ERROR nResult;

   /* Build internal callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &p7816Connection->sCallbackContextInternal);

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P7816SmInterfaceOpenLogicalChannelCompleted when the operation is completed */
   PHandleIncrementReferenceCount(p7816Connection);

   /* Save this flag to be recalled in the callback */
   p7816Connection->bInternalRawChannelOperation = bInternalRawChannel;

   nResult = p7816Connection->pSmInterface->pOpenChannel(
      pContext,
      p7816Connection->pSmInstance,
      static_P7816SmInterfaceOpenChannelCompleted,
      p7816Connection,
      nChannelType,
      pAID,
      nAIDLength);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_P7816SmInterfaceOpenChannelCompleted(pContext, p7816Connection, P_7816SM_NULL_CHANNEL, nResult);
      return;
   }
}

static void static_P7816SmInterfaceCloseChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  W_ERROR nResult )
{
   tP7816Channel* pChannel = (tP7816Channel*)pCallbackParameter;
   t7816Connection* p7816Connection = pChannel->p7816Connection;

   CDebugAssert(pChannel != null);
   CMemoryFree(pChannel);

   PDFCPostContext2(
      &p7816Connection->sCallbackContextInternal,
      nResult);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p7816Connection);
}

static void static_P7816CloseChannel(
            tContext*            pContext,
            t7816Connection*     p7816Connection,
            tPBasicGenericCallbackFunction* pCallback,
            void*                pCallbackParameter,
            tP7816Channel*       pChannel)
{
   W_ERROR nResult;

   /* Build internal callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &p7816Connection->sCallbackContextInternal);

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P7816SmInterfaceCloseLogicalChannelCompleted when the operation is completed */
   PHandleIncrementReferenceCount(p7816Connection);

   /* Send APDU */
   nResult = p7816Connection->pSmInterface->pCloseChannel(
      pContext,
      p7816Connection->pSmInstance,
      pChannel->nChannelReference,
      static_P7816SmInterfaceCloseChannelCompleted,
      pChannel);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_P7816SmInterfaceCloseChannelCompleted(pContext, pChannel, nResult);
   }
}

/* Execute the queued operation (after polling) */
static void static_P7816ExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   t7816Connection* p7816Connection = (t7816Connection*) pObject;

   PDebugTrace("static_P7816ExecuteQueuedExchange");

   /* Restore operation handle */
   p7816Connection->sSmRawInstance.hCurrentOperation = p7816Connection->sQueuedOperation.hCurrentOperation;
   /* Restore callback context */
   p7816Connection->sCallbackContext = p7816Connection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (p7816Connection->sSmRawInstance.hCurrentOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, p7816Connection->sSmRawInstance.hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_P7816ExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   switch (p7816Connection->sQueuedOperation.nType)
   {
   case P_7816_QUEUED_EXCHANGE:
      if (nResult != W_SUCCESS)
      {
         /* If an error has been detected during the polling, return directly */
         static_P7816ExchangeDataCompleted(pContext, p7816Connection, 0, nResult);
      }
      else
      {
         /* Send APDU */
         static_P7816ExchangeData(pContext,
                              p7816Connection,
                              static_P7816ExchangeDataCompleted,
                              p7816Connection,
                              p7816Connection->sQueuedOperation.pChannel,
                              p7816Connection->sQueuedOperation.pSendAPDUBuffer,
                              p7816Connection->sQueuedOperation.nSendAPDUBufferLength,
                              p7816Connection->sQueuedOperation.pReceivedAPDUBuffer,
                              p7816Connection->sQueuedOperation.nReceivedAPDUBufferMaxLength);
      }

      break;

   case P_7816_QUEUED_OPEN_CHANNEL:
      if (nResult != W_SUCCESS)
      {
         /* If an error has been detected during the polling, return directly */
         static_P7816OpenChannelCompleted(pContext, p7816Connection, W_NULL_HANDLE, nResult);
      }
      else
      {
         /* Open logical channel */
         static_P7816OpenChannel(
                              pContext,
                              p7816Connection,
                              static_P7816OpenChannelCompleted,
                              p7816Connection,
                              W_FALSE,
                              p7816Connection->sQueuedOperation.nChannelType,
                              p7816Connection->sQueuedOperation.pAID,
                              p7816Connection->sQueuedOperation.nAIDLength);
      }

      break;

   case P_7816_QUEUED_CLOSE:
      if (nResult != W_SUCCESS)
      {
         /* If an error has been detected during the polling, return directly */
         static_P7816CloseLogicalChannelCompleted(pContext, p7816Connection, nResult);
      }
      else
      {
         /* Close logical channel */
         static_P7816CloseChannel(
                              pContext,
                              p7816Connection,
                              static_P7816CloseLogicalChannelCompleted,
                              p7816Connection,
                              p7816Connection->sQueuedOperation.pChannel);
      }

      break;

   default:
      /* Not possible */
      CDebugAssert(0);
   }

   /* Reset data */
   CMemoryFill(&p7816Connection->sQueuedOperation, 0, sizeof(p7816Connection->sQueuedOperation));
}


/**
 * @brief   Stores the ATR.
 *
 * @param[in]  p7816Connection  The 7816-4 connection.
 *
 * @param[in]  pBuffer The buffer with the ATR extra data.
 *
 * @param[in]  nLength The buffer length.
 **/
static void static_P7816StoreAtr(
            t7816Connection* p7816Connection,
            const uint8_t* pBuffer,
            uint8_t nLength )
{
   uint8_t nXor;
   uint32_t i;

   p7816Connection->aATR[0] = 0x3B;
   p7816Connection->aATR[1] = 0x80 | nLength; /* T0 */
   p7816Connection->aATR[2] = 0x80;
   p7816Connection->aATR[3] = 0x01;

   /* Copy the historical bytes */
   if ( nLength != 0 )
   {
      /* Header + Historical bytes + TCK */
      CDebugAssert(nLength < (P_7816_4_ATR_MAX_LENGTH -4 -1));

      /* Store the higher layer response as historical bytes */
      CMemoryCopy(
         &p7816Connection->aATR[4],
         pBuffer, nLength );
   }
   p7816Connection->nATRLength = 4 + nLength + 1;

   /* Calculate the Exclusive-OR, starting at "T0" position (skip header 0x3B) */

   for ( i=1, nXor=0 ; i < (uint32_t)(p7816Connection->nATRLength -1) ; i++ )
   {
      nXor ^= p7816Connection->aATR[i];
   }

   p7816Connection->aATR[p7816Connection->nATRLength -1] = nXor;

   PDebugTrace("static_P7816StoreAtr: ATR");
   PDebugTraceBuffer( p7816Connection->aATR, p7816Connection->nATRLength );
}

/* See Header file */
void P7816CreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nConnectionProperty )
{
   tDFCCallbackContext sCallbackContext;
   tP7816SmInstance* pSmInstance;
   W_ERROR nError;
   t7816Connection* p7816Connection;
   tP7816SmRawInstance** ppSmRawInstance;

   /* nConnectionProperty is not used */

   /*
    * Create a level of indirection to store a pointer to pUSerConnection->sSmRawInstance.
    * We need to do that because pUSerConnection does not exist yet when the ISO7816 State Machine
    * is created. See also static_P7816RawExchangeApdu.
    */
   ppSmRawInstance = (tP7816SmRawInstance**)CMemoryAlloc(sizeof(tP7816SmRawInstance*));
   if (ppSmRawInstance == null)
   {
      PDebugTrace("P7816CreateConnection: Failed to allocate tP7816SmRawInstance");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(ppSmRawInstance, 0, sizeof(*ppSmRawInstance));

   /* Create the ISO-7816 State Machine */
   nError = P7816SmCreateInstance(pContext, &static_7816RawInterface, (tP7816SmRawInstance*)ppSmRawInstance, &pSmInstance);
   if (nError != W_SUCCESS)
   {
      PDebugError("P7816CreateConnection: Failed to create the ISO-7816 State Machine");
   }
   else
   {
      CDebugAssert(pSmInstance != null);

      nError = P7816CreateConnectionInternal(
            pContext, pSmInstance, &g_sP7816SmInterface, hUserConnection );
      if(nError == W_SUCCESS)
      {
         nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_7816_CONNECTION, (void**)&p7816Connection);

         if(nError == W_SUCCESS)
         {
            *ppSmRawInstance = &p7816Connection->sSmRawInstance;

            p7816Connection->ppSmRawInstance = ppSmRawInstance;
         }
      }
   }

return_error:

   if ((nError != W_SUCCESS) && (ppSmRawInstance != null))
   {
      CMemoryFree(ppSmRawInstance);
      ppSmRawInstance = (tP7816SmRawInstance**)null;
   }

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   PDFCPostContext2(
      &sCallbackContext,
      nError );
}

/* See Header file */
W_ERROR P7816CreateConnectionInternal(
            tContext* pContext,
            tP7816SmInstance* pSmInstance,
            tP7816SmInterface* pSmInterface,
            W_HANDLE hUserConnection )
{
   t7816Connection* p7816Connection;
   W_ERROR nError;

   CDebugAssert(pSmInterface != null);
   CDebugAssert(pSmInstance != null);

   /* Create the 7816-4 buffer */
   p7816Connection = (t7816Connection*)CMemoryAlloc( sizeof(t7816Connection) );
   if ( p7816Connection == null )
   {
      PDebugError("static_P7816CreateConnection: p7816Connection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(p7816Connection, 0, sizeof(t7816Connection));

   /* Add the 7816-4 structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     p7816Connection,
                     P_HANDLE_TYPE_7816_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("static_P7816CreateConnection: could not add the 7816-4 buffer");
      CMemoryFree(p7816Connection);
      goto return_error;
   }

   /* Set the SM */
   p7816Connection->pSmInterface = pSmInterface;
   p7816Connection->pSmInstance = pSmInstance;

   /* Store the connection handle */
   p7816Connection->hConnection = hUserConnection;

   /* Compute the ATR */
   if(PHandleCheckProperty(pContext, hUserConnection, W_PROP_ISO_14443_4_A) == W_SUCCESS)
   {
      tW14Part4ConnectionInfo sInfo;

      nError = P14Part4GetConnectionInfo(pContext, hUserConnection, &sInfo);
      if(nError != W_SUCCESS)
      {
         PDebugError("static_P7816CreateConnection: could not get the connection properties type A 4");
         goto return_error;
      }

      static_P7816StoreAtr(p7816Connection,
            sInfo.sW14TypeA.aApplicationData,
            sInfo.sW14TypeA.nApplicationDataLength );
   }
   else if(PHandleCheckProperty(pContext, hUserConnection, W_PROP_ISO_14443_4_B) == W_SUCCESS)
   {
      tW14Part3ConnectionInfo sInfo;
      uint8_t aHistoricalBytes[8];

      nError = P14Part3GetConnectionInfo(pContext, hUserConnection, &sInfo);
      if(nError != W_SUCCESS)
      {
         PDebugError("static_P7816CreateConnection: could not get the connection properties type B 3");
         goto return_error;
      }

      aHistoricalBytes[0] = sInfo.sW14TypeB.aATQB[5];
      aHistoricalBytes[1] = sInfo.sW14TypeB.aATQB[6];
      aHistoricalBytes[2] = sInfo.sW14TypeB.aATQB[7];
      aHistoricalBytes[3] = sInfo.sW14TypeB.aATQB[8];

      aHistoricalBytes[4] = sInfo.sW14TypeB.aATQB[9];
      aHistoricalBytes[5] = sInfo.sW14TypeB.aATQB[10];
      aHistoricalBytes[6] = sInfo.sW14TypeB.aATQB[11];

      aHistoricalBytes[7] = sInfo.sW14TypeB.nMBLI_CID & 0xF0;

      static_P7816StoreAtr(p7816Connection, aHistoricalBytes, 8 );
   }
   else if(PHandleCheckProperty(pContext, hUserConnection, W_PROP_SECURE_ELEMENT) == W_SUCCESS)
   {
      nError = PSEGetAtr(pContext, hUserConnection, p7816Connection->aATR, sizeof(p7816Connection->aATR), &p7816Connection->nATRLength);
      if(nError != W_SUCCESS)
      {
         PDebugError("static_P7816CreateConnection: could not get Secure Element ATR");
         p7816Connection->nATRLength = 0;
         goto return_error;
      }
   }
   else
   {
      /* No info available */
      static_P7816StoreAtr(p7816Connection, null, 0 );
   }

   /* Fill the raw instance */
   p7816Connection->sSmRawInstance.hConnection = p7816Connection->hConnection;

   nError = W_SUCCESS;

return_error:

   if(nError != W_SUCCESS)
   {
      PDebugError("P7816CreateConnection sending error %s", PUtilTraceError(nError));
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR P7816GetATRSize(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnSize )
{
   t7816Connection* p7816Connection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CONNECTION, (void**)&p7816Connection);

   if ( nError == W_SUCCESS )
   {
      /* Check the parameters */
      if ( pnSize == null )
      {
         PDebugError("P7816GetATRSize: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
      }
      else
      {
         *pnSize = p7816Connection->nATRLength;
      }
   }
   else
   {
      PDebugError("P7816GetATRSize: could not get p7816Connection buffer");
      if ( pnSize != null )
      {
         *pnSize = 0;
      }
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR P7816GetATR(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pBuffer,
            uint32_t nBufferMaxLength,
            uint32_t* pnActualLength)
{
   t7816Connection* p7816Connection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CONNECTION, (void**)&p7816Connection);

   if ( pnActualLength != null )
   {
      *pnActualLength = 0;
   }

   if ( nError != W_SUCCESS )
   {
      PDebugError("P7816GetATR: could not get p7816Connection buffer");
   }
   else if (( pBuffer == null )
         || ( pnActualLength == null ) )
   {
      PDebugError(" P7816GetATR: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
   }
   else if ( nBufferMaxLength < p7816Connection->nATRLength)
   {
      PDebugError(
         "P7816GetATR: Buffer to small (0x%08X)",
         *pnActualLength );
      nError = W_ERROR_BUFFER_TOO_SHORT;
   }
   else
   {
      *pnActualLength = p7816Connection->nATRLength;
      CMemoryCopy(pBuffer, p7816Connection->aATR, p7816Connection->nATRLength);
   }

   return nError;
}

/* See header */
void P7816ExchangeApduInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pSendAPDUBuffer,
            uint32_t nSendAPDUBufferLength,
            uint8_t* pReceivedAPDUBuffer,
            uint32_t nReceivedAPDUBufferMaxLength)
{
   tP7816Channel* pChannel = (tP7816Channel*)null;
   W_ERROR nError;

   PDebugTrace("P7816ExchangeApduInternal");

   /* Get channel object */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CHANNEL, (void**)&pChannel);
   if ( nError != W_SUCCESS )
   {
      tDFCCallbackContext sCallbackContext;
      PDebugError("P7816ExchangeApduInternal: return %s", PUtilTraceError(nError));
      PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );
      PDFCPostContext3(&sCallbackContext, 0, nError);
      return;
   }

   /* Send APDU */
   static_P7816ExchangeData(pContext,
                           pChannel->p7816Connection,
                           pCallback,
                           pCallbackParameter,
                           pChannel,
                           pSendAPDUBuffer,
                           nSendAPDUBufferLength,
                           pReceivedAPDUBuffer,
                           nReceivedAPDUBufferMaxLength);
}

/* See Client API Specifications */
void P7816ExchangeAPDU(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pSendAPDUBuffer,
            uint32_t nSendAPDUBufferLength,
            uint8_t* pReceivedAPDUBuffer,
            uint32_t nReceivedAPDUBufferMaxLength,
            W_HANDLE* phOperation )
{
   t7816Connection* p7816Connection = null;
   tP7816Channel* pChannel = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;
   W_ERROR nError;

   PDebugTrace("P7816ExchangeAPDU");

   /* Build callback context */
   PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback,
                  pCallbackParameter,
                  &sCallbackContext );

   /* Get connection object */
#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CONNECTION, (void**)&p7816Connection);
   if ( nError != W_SUCCESS )
#endif /* ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */
   {
      nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CHANNEL, (void**)&pChannel);
      if ( nError != W_SUCCESS )
      {
         goto return_error;
      }
      p7816Connection = pChannel->p7816Connection;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("P7816ExchangeAPDU: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("P7816ExchangeAPDU: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   if(PReaderCheckConnection(pContext, p7816Connection->hConnection) != W_FALSE)
   {
      nError = PReaderNotifyExchange(pContext, p7816Connection->hConnection, static_P7816ExecuteQueuedExchange, p7816Connection);
   }
   else
   {
      /* This is not a READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
         Manage the own 7816 echange pending flag */
      nError = static_P7816NotifyExchange(p7816Connection);
   }

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P7816ExchangeDataCompleted when the operation is completed */
      PHandleIncrementReferenceCount(p7816Connection);

      /* Store the operation handle */
      CDebugAssert(p7816Connection->sSmRawInstance.hCurrentOperation == W_NULL_HANDLE);
      p7816Connection->sSmRawInstance.hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      p7816Connection->sCallbackContext = sCallbackContext;

      /* Send APDU */
      static_P7816ExchangeData(pContext,
                           p7816Connection,
                           static_P7816ExchangeDataCompleted,
                           p7816Connection,
                           pChannel,
                           pSendAPDUBuffer,
                           nSendAPDUBufferLength,
                           pReceivedAPDUBuffer,
                           nReceivedAPDUBufferMaxLength);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p7816Connection);

      /* Save the operation handle */
      CDebugAssert(p7816Connection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      p7816Connection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      p7816Connection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      p7816Connection->sQueuedOperation.nType = P_7816_QUEUED_EXCHANGE;

      /* Save data */
      p7816Connection->sQueuedOperation.pChannel = pChannel;
      p7816Connection->sQueuedOperation.pSendAPDUBuffer = pSendAPDUBuffer;
      p7816Connection->sQueuedOperation.nSendAPDUBufferLength = nSendAPDUBufferLength;
      p7816Connection->sQueuedOperation.pReceivedAPDUBuffer = pReceivedAPDUBuffer;
      p7816Connection->sQueuedOperation.nReceivedAPDUBufferMaxLength = nReceivedAPDUBufferMaxLength;

      return;

   default:
      /* Return this error */
      if(hCurrentOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hCurrentOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("P7816ExchangeAPDU: return %s", PUtilTraceError(nError));

   PDFCPostContext3(&sCallbackContext, 0, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

W_ERROR P7816GetResponseAPDUData(
            tContext*  pContext,
            W_HANDLE   hConnection,
            uint8_t *  pReceivedAPDUBuffer,
            uint32_t   nReceivedAPDUBufferMaxLength,
            uint32_t * pnReceivedAPDUActualLength )
{
   t7816Connection* p7816Connection = (t7816Connection*)null;
   tP7816Channel* pChannel = (tP7816Channel*)null;
   W_ERROR nError;

   if (pnReceivedAPDUActualLength != null)
   {
      * pnReceivedAPDUActualLength = 0;
   }

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CONNECTION, (void**)&p7816Connection);
   if ( nError == W_SUCCESS )
   {
      if(pReceivedAPDUBuffer == null)
      {
         PDebugError("P7816GetResponseAPDUData: pReceivedAPDUBuffer == null");
         return W_ERROR_BAD_PARAMETER;
      }

      if (pnReceivedAPDUActualLength != null)
      {
         *pnReceivedAPDUActualLength = p7816Connection->nDeprecatedResponseBufferLength;
      }

      if(nReceivedAPDUBufferMaxLength < p7816Connection->nDeprecatedResponseBufferLength)
      {
         PDebugError("P7816GetResponseAPDUData: buffer too short");
         return W_ERROR_BUFFER_TOO_SHORT;
      }

      if(p7816Connection->pDeprecatedResponseBuffer != null)
      {
         CMemoryCopy(pReceivedAPDUBuffer, p7816Connection->pDeprecatedResponseBuffer, p7816Connection->nDeprecatedResponseBufferLength);
         CMemoryFree(p7816Connection->pDeprecatedResponseBuffer);
         p7816Connection->pDeprecatedResponseBuffer = (uint8_t*)null;
         p7816Connection->nDeprecatedResponseBufferLength = 0;
      }

      return W_SUCCESS;
   }
#endif /* ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CHANNEL, (void**)&pChannel);

   if ( nError != W_SUCCESS )
   {
      return (nError);
   }
   else if (pChannel == null)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   p7816Connection = pChannel->p7816Connection;

   CDebugAssert(p7816Connection != null);

   return p7816Connection->pSmInterface->pGetData(
      pContext,
      p7816Connection->pSmInstance,
      pChannel->nChannelReference,
      P_7816SM_DATA_TYPE_LAST_RESPONSE_APDU,
      pReceivedAPDUBuffer,
      nReceivedAPDUBufferMaxLength,
      pnReceivedAPDUActualLength);
}

/* See Client API Specifications */
W_ERROR W7816ExchangeAPDUSync(
            W_HANDLE hConnection,
            const uint8_t* pSendAPDUBuffer,
            uint32_t nSendAPDUBufferLength,
            uint8_t* pReceivedAPDUBuffer,
            uint32_t nReceivedAPDUBufferMaxLength,
            uint32_t* pnReceivedAPDUActualLength )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("W7816ExchangeAPDUSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      W7816ExchangeAPDU(
            hConnection,
            PBasicGenericSyncCompletionUint32,
            &param,
            pSendAPDUBuffer,
            nSendAPDUBufferLength,
            pReceivedAPDUBuffer,
            nReceivedAPDUBufferMaxLength,
            null  );
   }

   return PBasicGenericSyncWaitForResultUint32(&param, pnReceivedAPDUActualLength);
}

/* See Client API Specifications */
W_ERROR P7816GetAid(
                  tContext* pContext,
                  W_HANDLE hChannel,
                  uint8_t* pBuffer,
                  uint32_t nBufferMaxLength,
                  uint32_t* pnActualLength)
{
   W_ERROR nResult = W_SUCCESS;
   tP7816Channel* pChannel = null;

   *pnActualLength = 0;

   nResult = PReaderUserGetConnectionObject(pContext, hChannel, P_HANDLE_TYPE_7816_CHANNEL, (void**)&pChannel);
   if ( (nResult != W_SUCCESS) || (pChannel == null) )
   {
      PDebugError("P7816GetAid: Bad channel handle");
      return nResult;
   }

   if ( (pChannel->p7816Connection == null) || (pChannel->p7816Connection->pSmInterface == null) )
   {
      PDebugError("P7816GetAid: p7816Connection is null or has a null pSmInterface");
      return W_ERROR_BAD_HANDLE;
   }

   if (pBuffer == null)
   {
      PDebugError("P7816GetAid: pBuffer is null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pnActualLength == null)
   {
      PDebugError("P7816GetAid: pnActualLength is null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Get selected AID */
   return pChannel->p7816Connection->pSmInterface->pGetData(
               pContext,
               pChannel->p7816Connection->pSmInstance,
               pChannel->nChannelReference,
               P_7816SM_DATA_TYPE_AID,
               pBuffer,
               nBufferMaxLength,
               pnActualLength);
}

/* See header */
void P7816OpenChannelInternal(
                  tContext* pContext,
                  W_HANDLE hConnection,
                  tPBasicGenericHandleCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nChannelType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength)
{
   t7816Connection* p7816Connection = (t7816Connection*)null;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CONNECTION, (void**)&p7816Connection);
   if ( nError != W_SUCCESS )
   {
      tDFCCallbackContext sCallbackContext;
      PDebugError("P7816OpenChannelInternal: Bad connection handle");
      PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, W_NULL_HANDLE, nError);
      return;
   }

   /* Send APDU */
   static_P7816OpenChannel(
                        pContext,
                        p7816Connection,
                        pCallback,
                        pCallbackParameter,
                        W_TRUE,
                        nChannelType,
                        pAID,
                        nAIDLength);
}

/* See Client API Specifications */
void P7816OpenChannel(
                  tContext* pContext,
                  W_HANDLE hConnection,
                  tPBasicGenericHandleCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nChannelType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength,
                  W_HANDLE* phOperation)
{
   tDFCCallbackContext sCallbackContext;
   t7816Connection* p7816Connection = null;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;
   W_ERROR nError;

   PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback,
                  pCallbackParameter,
                  &sCallbackContext );

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_7816_CONNECTION, (void**)&p7816Connection);
   if ( nError != W_SUCCESS )
   {
      PDebugError("P7816OpenChannel: Bad connection handle");
      goto return_error;
   }

   if( ((pAID == null) && (nAIDLength != 0)) ||
       ((pAID != null) && (nAIDLength == 0)) )
   {
      PDebugError("P7816OpenChannel: Bad AID => (pAID == null AND nAIDLength != 0) OR (pAID != null AND nAIDLength == 0)");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("P7816OpenChannel: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("P7816OpenChannel: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   if(PReaderCheckConnection(pContext, p7816Connection->hConnection) != W_FALSE)
   {
      nError = PReaderNotifyExchange(pContext, p7816Connection->hConnection, static_P7816ExecuteQueuedExchange, p7816Connection);
   }
   else
   {
      /* This is not a READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
         Manage the own 7816 echange pending flag */
      nError = static_P7816NotifyExchange(p7816Connection);
   }

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p7816Connection);

      /* Store the operation handle */
      CDebugAssert(p7816Connection->sSmRawInstance.hCurrentOperation == W_NULL_HANDLE);
      p7816Connection->sSmRawInstance.hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      p7816Connection->sCallbackContext = sCallbackContext;

      /* Send APDU */
      static_P7816OpenChannel(
                           pContext,
                           p7816Connection,
                           static_P7816OpenChannelCompleted,
                           p7816Connection,
                           W_FALSE,
                           nChannelType,
                           pAID,
                           nAIDLength);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p7816Connection);

      /* Save the operation handle */
      CDebugAssert(p7816Connection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      p7816Connection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      p7816Connection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      p7816Connection->sQueuedOperation.nType = P_7816_QUEUED_OPEN_CHANNEL;

      /* Save data */
      p7816Connection->sQueuedOperation.nChannelType = nChannelType;
      p7816Connection->sQueuedOperation.pAID       = pAID;
      p7816Connection->sQueuedOperation.nAIDLength = nAIDLength;

      return;

   default:
      /* Return this error */
      if(hCurrentOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hCurrentOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("P7816OpenChannel: return %s", PUtilTraceError(nError));

   PDFCPostContext3(&sCallbackContext, W_NULL_HANDLE, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
W_ERROR W7816OpenChannelSync(
                  W_HANDLE hConnection,
                  uint32_t nChannelType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength,
                  W_HANDLE* phChannel)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("W7816OpenChannelSync");

   if (phChannel == null)
   {
      PDebugTrace("W7816OpenChannelSync: phChannel == null");
      return W_ERROR_BAD_PARAMETER;
   }
   else
   {
      *phChannel = W_NULL_HANDLE;
   }


   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      W7816OpenChannel(
            hConnection,
            PBasicGenericSyncCompletionHandle,
            &param,
            nChannelType,
            pAID,
            nAIDLength,
            null  );
   }

   return PBasicGenericSyncWaitForResultHandle(&param, phChannel);
}


/**
 * @brief   Destroyes a 7816 channel object.
 **/
static uint32_t static_P7816DestroyChannelAsync(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            void* pObject )
{
   t7816Connection* p7816Connection = (t7816Connection*)null;
   tP7816Channel* pChannel = (tP7816Channel*)pObject;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("static_P7816DestroyChannelAsync");

   PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback,
                  pCallbackParameter,
                  &sCallbackContext );

   p7816Connection = pChannel->p7816Connection;

   if (pChannel->nChannelReference == P_7816SM_NULL_CHANNEL)
   {
      PDebugError("static_P7816DestroyChannelAsync: Error bad logical channel status");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   if (pChannel->bInternalChannel == W_TRUE)
   {
      /* This is an internal operation. Save this flag to be recalled in the callback */
      p7816Connection->bInternalRawChannelOperation = W_TRUE;
      /* user connection status is ignored for internal operation */
      nError = W_SUCCESS;
   }
   /* Notify this exchange to manage user connection status and polling */
   else if(PReaderCheckConnection(pContext, p7816Connection->hConnection) != W_FALSE)
   {
      nError = PReaderNotifyExchange(pContext, p7816Connection->hConnection, static_P7816ExecuteQueuedExchange, p7816Connection);
   }
   else
   {
      /* This is not a READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
         Manage the own 7816 echange pending flag */
      nError = static_P7816NotifyExchange(p7816Connection);
   }

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p7816Connection);

      /* Store the callback context */
      p7816Connection->sCallbackContext = sCallbackContext;

      /* Send APDU */
      static_P7816CloseChannel(
                           pContext,
                           p7816Connection,
                           static_P7816CloseLogicalChannelCompleted,
                           p7816Connection,
                           pChannel);

      return P_HANDLE_DESTROY_PENDING;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p7816Connection);

      /* Save callback context */
      p7816Connection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      p7816Connection->sQueuedOperation.nType = P_7816_QUEUED_CLOSE;

      /* Save data */
      p7816Connection->sQueuedOperation.pChannel  = pChannel;

      return P_HANDLE_DESTROY_PENDING;

   default:
      /* Return this error */
      goto return_error;
   }

return_error:
   PDebugError("static_P7816DestroyChannelAsync: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   return P_HANDLE_DESTROY_PENDING;
}

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

/* See Client API Specifications */
void W7816OpenLogicalChannel(
                  W_HANDLE hConnection,
                  tWBasicGenericHandleCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pAID,
                  uint32_t nAIDLength,
                  W_HANDLE* phOperation)
{
   W7816OpenChannel(hConnection, pCallback, pCallbackParameter,
                  W_7816_CHANNEL_TYPE_LOGICAL, pAID, nAIDLength, phOperation);
}

/* See Client API Specifications */
W_ERROR W7816OpenLogicalChannelSync(
                  W_HANDLE hConnection,
                  const uint8_t* pAID,
                  uint32_t nAIDLength,
                  W_HANDLE* phChannel)
{
   return W7816OpenChannelSync(hConnection, W_7816_CHANNEL_TYPE_LOGICAL, pAID, nAIDLength, phChannel);
}

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
