/*
 * Copyright (c) 2011-2012 Inside Secure, All Rights Reserved.
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
   Contains the B Prime implementation
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( B_PRIME )

#include "wme_context.h"

/* Default timeout value used for B PRIME          */
#define P_B_PRIME_DEFAULT_TIMEOUT          0x0B    /* 618 ms */
#define P_B_PRIME_SIZE_MAX_DATA            256     /* length is stored on the first byte, so max size is 256 */

/* default configuration parameters*/
#define P_B_PRIME_READER_CONFIG_PARAMETERS               {0x00, 0x0B, 0x7F}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Declare a B PRIME driver exchange data structure */
typedef struct __tPBPrimeDriverConnection
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

   /* Connection information */
   uint32_t                   nTimeout;

   /* TCL response buffer */
   uint8_t*                   pCardToReaderBufferTCL;
   /* TCL response buffer maximum length */
   uint32_t                   nCardToReaderBufferMaxLengthTCL;

   uint8_t                    aReaderToCardBufferNAL[P_B_PRIME_SIZE_MAX_DATA + 1];

   /* Callback context */
   tDFCDriverCCReference      pDriverCC;

   /* Service Context */
   uint8_t                    nServiceIdentifier;
   /* Service Operation */
   tNALServiceOperation       sServiceOperation;

   /* Operation handle */
   W_HANDLE hOperation;
} tPBPrimeDriverConnection;

/* Destroy connection callback */
static uint32_t static_PBPrimeDriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Connection registry B Prime type */
tHandleType g_sBPrimeDriverConnection = { static_PBPrimeDriverDestroyConnection,
                                          null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_P_PRIME_DRIVER_CONNECTION (&g_sBPrimeDriverConnection)

static uint32_t static_PBPrimeDriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tPBPrimeDriverConnection* pBPrimeDriverConnection = (tPBPrimeDriverConnection*)pObject;

   PDebugTrace("static_PBPrimeDriverDestroyConnection");

   PDFCDriverFlushCall(pBPrimeDriverConnection->pDriverCC);

   CMemoryFree( pBPrimeDriverConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @see  PNALServiceExecuteCommand
 **/
static void static_PBPrimeDriverExecuteCommandCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter)
{
   tPBPrimeDriverConnection* pBPrimeDriverConnection = (tPBPrimeDriverConnection*)pCallbackParameter;
   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PDebugError(
         "static_PBPrimeDriverExecuteCommandCompleted: nError %s",
         PUtilTraceError(nError) );
      if ( nError == W_ERROR_CANCEL )
      {
         goto return_function;
      }
      nLength = 0;
   }
   else
   {
      PDebugTrace("static_PBPrimeDriverExecuteCommandCompleted: Response");

      PReaderDriverSetAntiReplayReference(pContext);

      /* Check if the buffer is to short */
      if ( nLength > pBPrimeDriverConnection->nCardToReaderBufferMaxLengthTCL )
      {
         nLength = 0;
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   /* Set the current operation as completed */
   if (pBPrimeDriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, pBPrimeDriverConnection->hOperation);
      PHandleClose(pContext, pBPrimeDriverConnection->hOperation);
      pBPrimeDriverConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the answer */
   PDFCDriverPostCC3(
      pBPrimeDriverConnection->pDriverCC,
      nLength,
      nError );

return_function:

   /* Release the reference after completion
      May destroy pBPrimeDriverConnection */
   PHandleDecrementReferenceCount(pContext, pBPrimeDriverConnection);
}



/** @see tPReaderDriverCreateConnection() */
static W_ERROR static_PBPrimeDriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   tPBPrimeDriverConnection* pBPrimeDriverConnection;
   W_HANDLE hDriverConnection;
   W_ERROR nError;

   /* Check the parameters */
   if ( phDriverConnection == null )
   {
      PDebugError("static_PBPrimeDriverCreateConnection: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the BPrime buffer */
   pBPrimeDriverConnection = (tPBPrimeDriverConnection*)CMemoryAlloc( sizeof(tPBPrimeDriverConnection) );
   if ( pBPrimeDriverConnection == null )
   {
      PDebugError("static_PBPrimeDriverCreateConnection: pBPrimeDriverConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pBPrimeDriverConnection, 0, sizeof(tPBPrimeDriverConnection));

   /* Create the BPrime operation handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pBPrimeDriverConnection,
                     P_HANDLE_TYPE_P_PRIME_DRIVER_CONNECTION,
                     &hDriverConnection ) ) != W_SUCCESS )
   {
      PDebugError("static_PBPrimeDriverCreateConnection: could not create BPrime connection handle");
      CMemoryFree(pBPrimeDriverConnection);
      return nError;
   }

   /* Store the connection information */
   pBPrimeDriverConnection->nServiceIdentifier = nServiceIdentifier;

   *phDriverConnection = hDriverConnection;

   return W_SUCCESS;
}

/** @see tPReaderDriverSetDetectionConfiguration() */
static W_ERROR static_PBPrimeDriverSetDetectionConfiguration(
                     tContext* pContext,
                     uint8_t* pCommandBuffer,
                     uint32_t* pnCommandBufferLength,
                     const uint8_t* pDetectionConfigurationBuffer,
                     uint32_t nDetectionConfigurationLength )
{

   W_ERROR nError = W_SUCCESS;
   static const uint8_t aCommandBuffer[] = P_B_PRIME_READER_CONFIG_PARAMETERS;

   PDebugTrace("static_PBPrimeDriverSetDetectionConfiguration");

   if(pDetectionConfigurationBuffer == null
   || nDetectionConfigurationLength == 0)
   {
      CMemoryCopy(pCommandBuffer,
                  aCommandBuffer,
                  sizeof(aCommandBuffer));
      *pnCommandBufferLength = sizeof(aCommandBuffer);
   }
   else
   {
      CMemoryCopy(pCommandBuffer,
                  pDetectionConfigurationBuffer,
                  nDetectionConfigurationLength);
      *pnCommandBufferLength = nDetectionConfigurationLength;
   }

   return (nError);
}


/** @see tPReaderDriverParseDetectionMessage() */
static W_ERROR static_PBPrimeDriverParseDetectionMessage(
               tContext* pContext,
               const uint8_t* pBuffer,
               uint32_t nLength,
               tPReaderDriverCardInfo* pCardInfo )
{
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_PBPrimeDriverParseDetectionMessage()");
   if(pCardInfo->nProtocolBF != W_NFCC_PROTOCOL_READER_BPRIME)
   {
      PDebugError("static_P14P4DriverParseDetectionMessage: protocol error");
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }

   /* We set the AID by the REPGEN containing in the pBuffer*/
   CMemoryCopy(pCardInfo->aUID,
               pBuffer,
               P_B_PRIME_LENGTH_UID);

   pCardInfo->nUIDLength = (uint8_t) P_B_PRIME_LENGTH_UID;
   pCardInfo->nProtocolBF |= W_NFCC_PROTOCOL_READER_BPRIME;

return_function:

   if(nError == W_SUCCESS)
   {
      PDebugTrace("static_PBPrimeDriverParseDetectionMessage: UID = ");
      PDebugTraceBuffer(pCardInfo->aUID, pCardInfo->nUIDLength);

   }
   else
   {
      PDebugTrace("static_PBPrimeDriverParseDetectionMessage: error %s", PUtilTraceError(nError));
   }

   return nError;
}


/* See Header file */
W_ERROR PBPrimeDriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout )
{
   tPBPrimeDriverConnection* pBPrimeDriverConnection;
   W_ERROR nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_P_PRIME_DRIVER_CONNECTION,
      (void**)&pBPrimeDriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Check the timeout value */
      if ( nTimeout > 14 )
      {
         return W_ERROR_BAD_PARAMETER;
      }
      pBPrimeDriverConnection->nTimeout = nTimeout;
   }

   return nError;
}


/*
 * @brief Cancel the current data exchange operation
 *
 * @param[in] pContext The context
 *
 * @param[in] pCancelParametet The cancel parameter, e.g pType1ChipDriverConnection
 *
 * @param[in] bIsClosing
 *
 */
static void static_PBrimeDriverExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tPBPrimeDriverConnection* pBPrimeDriverConnection = pCancelParameter;

   PDebugTrace("static_PBrimeDriverExchangeDataCancel");

   /* request the cancel of the NFC HAL operation */
   PNALServiceCancelOperation(pContext, & pBPrimeDriverConnection->sServiceOperation);
}

/* See Header file */
W_HANDLE PBPrimeDriverExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength)
{
   tPBPrimeDriverConnection* pBPrimeDriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );

   nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_P_PRIME_DRIVER_CONNECTION,
      (void**)&pBPrimeDriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      pBPrimeDriverConnection->pDriverCC = pDriverCC;

      /* Check the parameters */
      if (  ( pReaderToCardBuffer == null )
         || ( nReaderToCardBufferLength == 0 )
         || ( nReaderToCardBufferLength > P_B_PRIME_SIZE_MAX_DATA )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) ) )
      {
         PDebugError("PBPrimeDriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      /* Create the operation handle */

      pBPrimeDriverConnection->hOperation = PBasicCreateOperation(pContext, static_PBrimeDriverExchangeDataCancel, pBPrimeDriverConnection);

      if (pBPrimeDriverConnection->hOperation == W_NULL_HANDLE)
      {
         PDebugError("PBPrimeDriverExchangeData : PBasicCreateOperation failed");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      nError = PHandleDuplicate(pContext, pBPrimeDriverConnection->hOperation, & hOperation);

      if (nError != W_SUCCESS)
      {
         PDebugError("PBPrimeDriverExchangeData : PHandleDuplicate failed %s", PUtilTraceError(nError));
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      /* Store the connection information */
      pBPrimeDriverConnection->nCardToReaderBufferMaxLengthTCL = nCardToReaderBufferMaxLength;
      /* Store the callback buffer */
      pBPrimeDriverConnection->pCardToReaderBufferTCL = pCardToReaderBuffer;

      CMemoryCopy(
         pBPrimeDriverConnection->aReaderToCardBufferNAL,
         pReaderToCardBuffer,
         nReaderToCardBufferLength );

      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_PBPrimeDriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(pBPrimeDriverConnection);

      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         pBPrimeDriverConnection->nServiceIdentifier,
         &pBPrimeDriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         pBPrimeDriverConnection->aReaderToCardBufferNAL,
         nReaderToCardBufferLength,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength,
         static_PBPrimeDriverExecuteCommandCompleted,
         pBPrimeDriverConnection );
   }
   else
   {
      PDebugError("PBPrimeDriverExchangeData: could not get pBPrimeDriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }

   return (hOperation);

error:

   if (pBPrimeDriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, pBPrimeDriverConnection->hOperation);
      PHandleClose(pContext, pBPrimeDriverConnection->hOperation);
      pBPrimeDriverConnection->hOperation = W_NULL_HANDLE;
   }

   if (hOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hOperation);
   }

   /* Send the error */
   PDFCDriverPostCC3(
      pBPrimeDriverConnection->pDriverCC,
      0,
      nError );

   return (W_NULL_HANDLE);
}

/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sPBPrimeReaderProtocolConstant = {
   W_NFCC_PROTOCOL_READER_BPRIME,
   NAL_SERVICE_READER_B_PRIME,
   static_PBPrimeDriverCreateConnection,
   static_PBPrimeDriverSetDetectionConfiguration,
   static_PBPrimeDriverParseDetectionMessage,
   null };



#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef struct __tPBPrimeUserConnection
{
   /* Connection registry handle */
   tHandleObjectHeader        sObjectHeader;

   /* Driver connection handle */
   W_HANDLE                   hDriverConnection;

   /* Operation handles */
   W_HANDLE hCurrentOperation;
   W_HANDLE hCurrentDriverOperation;

   /* Protocol                */
   uint8_t                    nProtocol;

   /* Connection information */
   uint32_t                   nTimeout;

   /**
    * The REPGEN length, in bytes.
    * The length may be any value between 0 and P_B_PRIME_SIZE_MAX_REPGEN
    */
   uint32_t nREPGENLength;

   /**
    * The response sent by a B prime card to ther APGEN
    **/
   uint8_t aREPGEN[P_B_PRIME_SIZE_MAX_REPGEN];

   /* Callback context */
   tDFCCallbackContext        sCallbackContext;
} tPBPrimeUserConnection;


/* Destroy connection callback */
static uint32_t static_PBPrimeUserDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get property numbet callback */
static uint32_t static_PBPrimeUserGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get property numbet callback */
static bool_t static_PBPrimeUserGetProperties(
         tContext* pContext,
         void* pObject,
         uint8_t* pPropertyArray );

/* Check the property */
static bool_t static_PBPrimeUserCheckProperty(
         tContext* pContext,
         void* pObject,
         uint8_t nPropertyValue );

/* Get identifier length */
static uint32_t static_PBPrimeUserGetIdentifierLength(
            tContext* pContext,
            void* pObject);

/* Get identifier */
static void static_PBPrimeUserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer);

/* Exchange Data */
static void static_PBPrimeUserExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation);

/* Connection registry B PRIME type */
tHandleType g_sBPrimeUserConnection = {   static_PBPrimeUserDestroyConnection,
                                          null,
                                          static_PBPrimeUserGetPropertyNumber,
                                          static_PBPrimeUserGetProperties,
                                          static_PBPrimeUserCheckProperty,
                                          static_PBPrimeUserGetIdentifierLength,
                                          static_PBPrimeUserGetIdentifier,
                                          static_PBPrimeUserExchangeData,
                                          null };


#define P_HANDLE_TYPE_B_PRIME_USER_CONNECTION (&g_sBPrimeUserConnection)

/**
 * @brief   Destroyes a B PRIME connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PBPrimeUserDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tPBPrimeUserConnection* pBPrimeUserConnection = (tPBPrimeUserConnection*)pObject;

   PDebugTrace("static_PBPrimeUserDestroyConnection");

   /* The driver connection is closed by the reader registry parent object */
   /* pBPrimeUserConnection->hDriverConnection */

   /* Flush the function calls */
   PDFCFlushCall(&pBPrimeUserConnection->sCallbackContext);

   /* Free the B Prime connection structure */
   CMemoryFree( pBPrimeUserConnection );

   return P_HANDLE_DESTROY_DONE;
}


/**
 * @brief   Gets the B Prime connection property number.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static uint32_t static_PBPrimeUserGetPropertyNumber(
            tContext* pContext,
            void * pObject)
{
   return 1;
}


static bool_t static_PBPrimeUserGetProperties(
         tContext* pContext,
         void* pObject,
         uint8_t* pPropertyArray )
{
   tPBPrimeUserConnection* pBPrimeUserConnection = (tPBPrimeUserConnection*)pObject;

   PDebugTrace("static_P14P3UserGetProperties");

   if ( pBPrimeUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_PBPrimeUserGetProperties: no property");
      return W_FALSE;
   }

   pPropertyArray[0] = pBPrimeUserConnection->nProtocol;
   return W_TRUE;
}

static bool_t static_PBPrimeUserCheckProperty(
         tContext* pContext,
         void* pObject,
         uint8_t nPropertyValue )
{
   tPBPrimeUserConnection * pBPrimeUserConnection = (tPBPrimeUserConnection*)pObject;

   PDebugTrace(
      "static_PBPrimeUserCheckProperty: nPropertyValue= (0x%02X)", nPropertyValue  );

   return ( nPropertyValue == pBPrimeUserConnection->nProtocol )?W_TRUE:W_FALSE;

}

static W_ERROR static_PBPrimeUserSetTimeout(
            tContext* pContext,
            tPBPrimeUserConnection* pBPrimeUserConnection,
            uint32_t nTimeout )
{
   W_ERROR nError;

   if ( ( nError = PBPrimeDriverSetTimeout(
                     pContext,
                     pBPrimeUserConnection->hDriverConnection,
                     nTimeout ) ) == W_SUCCESS )
   {
      pBPrimeUserConnection->nTimeout = nTimeout;
   }

   return nError;
}

/**
 * @brief   Parses the Type B Prime card response at start up.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pBPrimeUserConnection  The pBPrime user connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_PBPrimeUserParseCardInfo(
            tContext* pContext,
            tPBPrimeUserConnection* pPBPrimeUserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   if(nLength > sizeof(pPBPrimeUserConnection->aREPGEN))
   {
      PDebugError("static_PBPrimeUserParseCardInfo: info buffer too long");
      return W_ERROR_BAD_PARAMETER;
   }
   CMemoryCopy(pPBPrimeUserConnection->aREPGEN,
            pBuffer,
            nLength);

   pPBPrimeUserConnection->nREPGENLength = nLength;
   pPBPrimeUserConnection->nTimeout = P_B_PRIME_DEFAULT_TIMEOUT;

   return W_SUCCESS;
}

/* See Header file */
void PBPrimeUserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tPBPrimeUserConnection* pBPrimeUserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the B PRIME buffer */
   pBPrimeUserConnection = (tPBPrimeUserConnection*)CMemoryAlloc( sizeof(tPBPrimeUserConnection) );
   if ( pBPrimeUserConnection == null )
   {
      PDebugError("PBPrimeUserCreateConnection: pBPrimeUserConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_function;
   }
   CMemoryFill(pBPrimeUserConnection, 0, sizeof(tPBPrimeUserConnection));

   pBPrimeUserConnection->nProtocol          = nProtocol;

   /* Store the callback context */
   pBPrimeUserConnection->sCallbackContext = sCallbackContext;

   /* Store the connection information */
   pBPrimeUserConnection->hDriverConnection  = hDriverConnection;

   /* Parse the information */
   if ( ( nError = static_PBPrimeUserParseCardInfo(
                     pContext,
                     pBPrimeUserConnection,
                     pBuffer,
                     nLength ) ) != W_SUCCESS )
   {
      PDebugError( "PBPrimeUserCreateConnection: error while parsing the card information");
      CMemoryFree(pBPrimeUserConnection);
      goto return_function;
   }

   /* Add the B PRIME structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     pBPrimeUserConnection,
                     P_HANDLE_TYPE_B_PRIME_USER_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PBPrimeUserCreateConnection: could not add the B PRIME buffer");
      /* Send the result */
      CMemoryFree(pBPrimeUserConnection);
      goto return_function;
   }



return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError( "PBPrimeUserCreateConnection: returning %s",
         PUtilTraceError(nError) );
   }

   PDFCPostContext2(
         &sCallbackContext,
         nError );
}

/* See Header file*/
W_ERROR PBPrimeListenToCardDetection(
                     tContext* pContext,
                     tPReaderCardDetectionHandler* pHandler,
                     void* pHandlerParameter,
                     const uint8_t * pAPGENBuffer,
                     uint32_t nAPGENLength,
                     W_HANDLE* phEventRegistry)
{
   static const uint8_t nProtocol = W_PROP_BPRIME;

   if(
      (pAPGENBuffer == null)
      || (nAPGENLength < 3)
      || (nAPGENLength > 4))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   /* Launch the card detection request */
   return PReaderUserListenToCardDetection(
                     pContext,
                     pHandler, pHandlerParameter,
                     W_PRIORITY_EXCLUSIVE,
                     &nProtocol, 1,
                     pAPGENBuffer, nAPGENLength,
                     phEventRegistry);
}


/* See Client API Specifications */
W_ERROR PBPrimeSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout )
{
   tPBPrimeUserConnection* pPBPrimeUserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_B_PRIME_USER_CONNECTION, (void**)&pPBPrimeUserConnection);

   if ( nError == W_SUCCESS )
   {
      nError = static_PBPrimeUserSetTimeout(pContext, pPBPrimeUserConnection, nTimeout);
   }

   return nError;
}

/* Get identifier length */
static uint32_t static_PBPrimeUserGetIdentifierLength(
            tContext* pContext,
            void* pObject)
{
   tPBPrimeUserConnection* pPBPrimeUserConnection = (tPBPrimeUserConnection*)pObject;

   return pPBPrimeUserConnection->nREPGENLength;
}

/* Get identifier */
static void static_PBPrimeUserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer)
{
   tPBPrimeUserConnection* pPBPrimeUserConnection = (tPBPrimeUserConnection*)pObject;

   CMemoryCopy(
            pIdentifierBuffer,
            pPBPrimeUserConnection->aREPGEN,
            pPBPrimeUserConnection->nREPGENLength);
}

/* See Client API Specifications */
W_ERROR PBPrimeGetConnectionInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tWBPrimeConnectionInfo* pBPrimeConnectionInfo )
{
   tPBPrimeUserConnection* pPBPrimeUserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_B_PRIME_USER_CONNECTION, (void**)&pPBPrimeUserConnection);

   if ( nError == W_SUCCESS )
   {
      if (pBPrimeConnectionInfo != null)
      {
         CMemoryCopy(
            pBPrimeConnectionInfo->aREPGEN,
            pPBPrimeUserConnection->aREPGEN,
            pPBPrimeUserConnection->nREPGENLength);

         pBPrimeConnectionInfo->nREPGENLength = pPBPrimeUserConnection->nREPGENLength;
         pBPrimeConnectionInfo->nTimeout      = pPBPrimeUserConnection->nTimeout;
      }
      else
      {
         nError = W_ERROR_BAD_PARAMETER;
      }
   }

   if ((nError != W_SUCCESS) && (pBPrimeConnectionInfo != null))
   {
      CMemoryFill(pBPrimeConnectionInfo, 0, sizeof(pBPrimeConnectionInfo));
   }

   return nError;
}

static void static_PBPrimeUserExchangeDataCompleted(tContext* pContext, void *pCallbackParameter, uint32_t nDataLength, W_ERROR nError);
static void static_PBPrimeUserExchangeDataCancel(tContext* pContext, void* pCancelParameter, bool_t bIsClosing);

static void static_PBPrimeUserExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation)
{
   tPBPrimeUserConnection* pBPrimeUserConnection = (tPBPrimeUserConnection*)pObject;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_PBPrimeUserExchangeData");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if an operation is still pending */
   if ( pBPrimeUserConnection->hCurrentOperation != W_NULL_HANDLE )
   {
      PDebugError("static_PBPrimeUserExchangeData: operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* Check the card buffer size */
   if ((nReaderToCardBufferLength == 0) || (pReaderToCardBuffer == null))
   {
      PDebugError("static_PBPrimeUserExchangeData: nReaderToCardBufferLength / pReaderToCardBuffer can not be null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( ((pCardToReaderBuffer == null) && (nCardToReaderBufferMaxLength != 0)) ||
        ((pCardToReaderBuffer != null) && (nCardToReaderBufferMaxLength == 0)) )
   {
      PDebugError("static_PBPrimeUserExchangeData: inconsistency between pCardToReaderBuffer and nCardToReaderBufferMaxLength");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle if needed */
   if((pBPrimeUserConnection->hCurrentOperation = PBasicCreateOperation(
      pContext, static_PBPrimeUserExchangeDataCancel, pBPrimeUserConnection)) == W_NULL_HANDLE)
   {
      PDebugError("static_PBPrimeUserExchangeData: Cannot allocate the operation");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }

   if(phOperation != null)
   {
      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, pBPrimeUserConnection->hCurrentOperation, phOperation );
      if(nError != W_SUCCESS)
      {
         PDebugError("static_PBPrimeUserExchangeData: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, pBPrimeUserConnection->hCurrentOperation);
         pBPrimeUserConnection->hCurrentOperation = W_NULL_HANDLE;

         goto return_error;
      }
   }

   pBPrimeUserConnection->hCurrentDriverOperation = PBPrimeDriverExchangeData(
         pContext,
         pBPrimeUserConnection->hDriverConnection,
         static_PBPrimeUserExchangeDataCompleted,
         pBPrimeUserConnection,
         pReaderToCardBuffer,
         nReaderToCardBufferLength,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength);

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PBPrimeUserExchangeData: Error returned by PContextGetLastIoctlError()");
      PHandleClose(pContext, pBPrimeUserConnection->hCurrentOperation);
      pBPrimeUserConnection->hCurrentOperation = W_NULL_HANDLE;

      goto return_error;
   }

   /* Store the callback context */
   pBPrimeUserConnection->sCallbackContext = sCallbackContext;

   /* Increment the reference count to keep the connection object alive
      during the operation.
      The reference count is decreased in static_PBPrimeUserExchangeDataCompleted
      when the NFC HAL operation is completed */
   PHandleIncrementReferenceCount(pBPrimeUserConnection);

   return;

return_error:

   PDebugError("static_PBPrimeUserExchangeData: returning %s", PUtilTraceError(nError));

   PDFCPostContext3(
      &sCallbackContext,
      0,
      nError );
}

/* See tWBasicGenericDataCallbackFunction */
static void static_PBPrimeUserExchangeDataCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{


   tPBPrimeUserConnection* pBPrimeUserConnection = (tPBPrimeUserConnection*)pCallbackParameter;

   /* Check if the operation has been cancelled by the user */
   if ( pBPrimeUserConnection->hCurrentOperation != W_NULL_HANDLE )
   {
      /* Check the operation state */
      if ( PBasicGetOperationState(pContext, pBPrimeUserConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED)
      {
         PDebugError("static_PBPrimeUserExchangeDataCompleted: Operation is cancelled");
         if(nError == W_SUCCESS)
         {
            nError = W_ERROR_CANCEL;
         }
      }
      else
      {
         PBasicSetOperationCompleted(pContext, pBPrimeUserConnection->hCurrentOperation);
      }

      PHandleClose(pContext, pBPrimeUserConnection->hCurrentOperation);
      pBPrimeUserConnection->hCurrentOperation = W_NULL_HANDLE;
   }

   PHandleClose(pContext, pBPrimeUserConnection->hCurrentDriverOperation);
   pBPrimeUserConnection->hCurrentDriverOperation = W_NULL_HANDLE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PBPrimeUserExchangeDataCompleted: Returning %s",
         PUtilTraceError(nError));
      nDataLength = 0;
   }

   /* Send the result */
   PDFCPostContext3(
      &pBPrimeUserConnection->sCallbackContext,
      nDataLength,
      nError );

   /* Release the reference after completion
      May destroy pBPrimeUserConnection */
   PHandleDecrementReferenceCount(pContext, pBPrimeUserConnection);
}

static void static_PBPrimeUserExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tPBPrimeUserConnection* pBPrimeUserConnection = (tPBPrimeUserConnection*)pCancelParameter;

   PDebugTrace("static_PBPrimeUserExchangeDataCancel");

   PBasicCancelOperation(pContext, pBPrimeUserConnection->hCurrentDriverOperation);
   PHandleClose(pContext, pBPrimeUserConnection->hCurrentDriverOperation);
   pBPrimeUserConnection->hCurrentDriverOperation = W_NULL_HANDLE;
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

