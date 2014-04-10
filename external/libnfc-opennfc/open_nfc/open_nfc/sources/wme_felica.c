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
   Contains the implementation of the FeliCa functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( FELICA )

#include "wme_context.h"

/* default timeout used for xchange data is 618ms*/
#define P_FELICA_DEFAULT_TIMEOUT           0x0B
#define P_FELICA_ENABLE_TIMEOUT            0x10

/*configurations infos*/
#define P_FELICA_READER_CONFIG_PARAMETERS  {0xFF, 0xFF}
#define P_FELICA_SYSTEM_CODE_SIZE          0x02

/* Manufacture ID length */
#define P_FELICA_MANUFACTURER_ID_LENGTH           0x08
#define P_FELICA_MANUFACTURER_PARAMETER_LENGTH    0x08

/*Declare Parameters structure for the card*/
typedef struct __tPFeliCaDetectionConfiguration
{
   uint8_t     aSystemCode[P_FELICA_SYSTEM_CODE_SIZE];
}tPFeliCaDetectionConfiguration;

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* FeliCa limits for number of services */
#define P_FELICA_MINIMUM_NUMBER_SERVICES          0x01
#define P_FELICA_MAXIMUM_NUMBER_SERVICES          0x10

/* FeliCa limits for number of blocks */
#define P_FELICA_MINIMUM_NUMBER_BLOCKS            0x01
#define P_FELICA_MAXIMUM_READ_NUMBER_BLOCKS       0x0C
#define P_FELICA_MAXIMUM_WRITE_NUMBER_BLOCKS      0x08

/* FeliCa limits for number of services */
#define P_FELICA_READONLY_MASK                    0x0B
#define P_FELICA_READWRITE_MASK                   0x09

/* FeliCa command codes */

#define P_FELICA_COMMAND_CODE_POLL                0x00
#define P_FELICA_COMMAND_CODE_EXIST               0x04
#define P_FELICA_COMMAND_CODE_EXIST_RESPONSE      0x05
#define P_FELICA_COMMAND_CODE_CHECK               0x06
#define P_FELICA_COMMAND_CODE_CHECK_RESPONSE      0x07
#define P_FELICA_COMMAND_CODE_UPDATE              0x08
#define P_FELICA_COMMAND_CODE_UPDATE_RESPONSE     0x09
#define P_FELICA_COMMAND_CODE_REQUEST_SYSTEM_CODE 0x0C
#define P_FELICA_COMMAND_CODE_REQUEST_SYSTEM_CODE_RESPONSE 0x0D

/* FeliCa command codes */
#define P_FELICA_MEMORY_ERROR                     0x70
#define P_FELICA_EXCESSIVE_WRITES                 0x71

/* Block list element masks */
#define P_FELICA_LEN_MASK                         0x80
#define P_FELICA_ACCESS_MODE_MASK                 0x70
#define P_FELICA_SERVICE_CODE_ORDER_MASK          0x0F

/* Access attribute of service code */
#define P_FELICA_READ_WRITE                       0x0B
#define P_FELICA_READ_ONLY                        0x09

/* Block list offet */
#define P_FELICA_BLOCK_NUMBER_LENGTH1             0x01
#define P_FELICA_BLOCK_NUMBER_LENGTH2             0x02

/* Sub system constant */
#define P_FELICA_MAX_SUB_SYSTEM                   0x10

/* Queued operation type */
#define P_FELICA_QUEUED_NONE                    0
#define P_FELICA_QUEUED_READ                    1
#define P_FELICA_QUEUED_WRITE                   2
#define P_FELICA_QUEUED_SELECT_CARD             3
#define P_FELICA_QUEUED_SELECT_SYSTEM           4


/* Declare a FeliCa exchange data structure */
typedef struct __tPFeliCaUserConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;

   /* Connection handle */
   W_HANDLE                   hDriverConnection;
   W_HANDLE                   hUserConnection;
   /* Type of the protocol  */
   uint8_t                    nProtocol;

  /* manufacturer ID */
   uint8_t                    aManufacturerID[P_FELICA_MANUFACTURER_ID_LENGTH];
   /* manufacturer Parameters */
   uint8_t                    aManufacturerParameter[P_FELICA_MANUFACTURER_PARAMETER_LENGTH];

   /* Command buffer */
   uint8_t*                   pBuffer;
   /* Command buffer */
   uint8_t                    pReaderToCardBuffer[NAL_MESSAGE_MAX_LENGTH];
   /* Response buffer */
   uint8_t                    pCardToReaderBuffer[NAL_MESSAGE_MAX_LENGTH];

   /* Callback context used by method called from user */
   tDFCCallbackContext        sCallbackContext;
   /* Callback context used by method called from another connection */
   tDFCCallbackContext        sCallbackContextInternal;
   /* Operation handle */
   W_HANDLE                   hOperation;

   /* Array of system codes an entry for each system*/
   uint16_t                   aSystemCodes[P_FELICA_MAX_SUB_SYSTEM];

   uint32_t                   nCardNumber;
   tWFeliCaConnectionInfo     *pCardList;

   /* Hold data of the queued operation which will be executed after the polling completion */
   struct __tQueuedOperation
   {
      /* Type of operation: Read, Write... */
      uint32_t             nType;
      /* Data */
      uint8_t*             pBuffer;
      uint32_t             nLength;
      uint8_t              nNumberOfService;
      uint16_t             aServiceCodeList[P_FELICA_MAXIMUM_NUMBER_SERVICES];
      uint8_t              nNumberOfBlocks;
      uint8_t              aBlockList[P_FELICA_MAXIMUM_READ_NUMBER_BLOCKS];
      const tWFeliCaConnectionInfo * pFeliCaConnectionInfo;
      uint8_t              nIndexSubSystem;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hOperation;
   } sQueuedOperation;

} tPFeliCaUserConnection;

static uint8_t aIDMMultiCards[] = {0xFF, 0xFF, 0xFF, 0xFF,
                                   0xFF, 0xFF, 0xFF, 0xFF};

/* Destroy connection callback */
static uint32_t static_PFeliCaUserDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static uint32_t static_PFeliCaUserGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}


/* Get properties connection callback */
static bool_t static_PFeliCaUserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_PFeliCaUserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Simple wrapper for exchanging data */
static void static_PFeliCaDriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;

   PFeliCaDriverExchangeData(
            pContext, hDriverConnection,
            pCallback, pCallbackParameter,
            pReaderToCardBuffer, nReaderToCardBufferLength,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength );

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, 0, nError);
   }
}

static void static_PFeliCaDriverGetCardList(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;

   PFeliCaDriverGetCardList(
            pContext, hDriverConnection,
            pCallback, pCallbackParameter,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength );

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, 0, nError);
   }
}

/* Get identifier length */
static uint32_t static_PFeliCaUserGetIdentifierLength(
            tContext* pContext,
            void* pObject);

/* Get identifier */
static void static_PFeliCaUserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer);

/* Exchnage Data */
static void static_PFeliCaUserExchangeData(
      tContext* pContext,
      void* pObject,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength,
      W_HANDLE * phOperation);

/* See header file */
static void static_PFeliCaPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/* Execute the queued operation (after polling) */
static void static_PFeliCaExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult);

/* Handle registry FeliCa connection type */
tHandleType g_sFeliCaUserConnection = {   static_PFeliCaUserDestroyConnection,
                                    null,
                                    static_PFeliCaUserGetPropertyNumber,
                                    static_PFeliCaUserGetProperties,
                                    static_PFeliCaUserCheckProperties,
                                    static_PFeliCaUserGetIdentifierLength,
                                    static_PFeliCaUserGetIdentifier,
                                    static_PFeliCaUserExchangeData,
                                    static_PFeliCaPoll };

#define P_HANDLE_TYPE_FELICA_USER_CONNECTION (&g_sFeliCaUserConnection)

/**
 * @brief   Destroyes a FeliCa connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PFeliCaUserDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pObject;

   PDebugTrace("static_PFeliCaUserDestroyConnection");

   PDFCFlushCall(&pFeliCaUserConnection->sCallbackContext);

   /* Free the FeliCa connection structure */
   if(pFeliCaUserConnection->pCardList != null)
   {
      CMemoryFree(pFeliCaUserConnection->pCardList);
      pFeliCaUserConnection->pCardList = null;
   }

   CMemoryFree( pFeliCaUserConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the FeliCa connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PFeliCaUserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pObject;

   PDebugTrace("static_PFeliCaUserGetProperties");

   if ( pFeliCaUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_PFeliCaUserGetProperties: no property");
      return W_FALSE;
   }

   pPropertyArray[0] = pFeliCaUserConnection->nProtocol;
   return W_TRUE;
}

/**
 * @brief   Checks the FeliCa connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PFeliCaUserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pObject;

   PDebugTrace(
      "static_PFeliCaUserCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( pFeliCaUserConnection->nProtocol != P_PROTOCOL_STILL_UNKNOWN )
   {
      if ( nPropertyValue == pFeliCaUserConnection->nProtocol )
      {
         return W_TRUE;
      }
      else
      {
         return W_FALSE;
      }
   }
   else
   {
      PDebugError("static_PFeliCaUserCheckProperties: no property");
      return W_FALSE;
   }
}

/**
 * @brief   Sends an error to the process which has asked for the FeliCa exchange.
 *
 * @param[in]  pFeliCaUserConnection  The FeliCa connection.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PFeliCaUserSendError(
            tContext* pContext,
            tPFeliCaUserConnection* pFeliCaUserConnection,
            W_ERROR nError )
{
   if (pFeliCaUserConnection->hOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pFeliCaUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PFeliCaUserSendError: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pFeliCaUserConnection->hOperation);
      PHandleClose(pContext, pFeliCaUserConnection->hOperation);
      pFeliCaUserConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the error */
   PDFCPostContext2(&pFeliCaUserConnection->sCallbackContext, nError);

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pFeliCaUserConnection->hUserConnection);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pFeliCaUserConnection);
}

/**
 * @brief   Parses the card answer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pFeliCaUserConnection  FeliCa user connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_FeliCaUserParse(
            tContext* pContext,
            tPFeliCaUserConnection* pFeliCaUserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
    uint32_t nIndex = 0;
    uint8_t nSystemNumber;

    if((pBuffer == null)
    ||(nLength != NAL_READER_FELICA_DETECTION_MESSAGE_SIZE))
    {
       PDebugTrace("static_FeliCaUserParse(): bad parameters");
       return W_ERROR_RF_COMMUNICATION;
    }
    /* copy the Manufacuturer ID bytes */
    CMemoryCopy(
            pFeliCaUserConnection->aManufacturerID,
            &pBuffer[nIndex],
            P_FELICA_MANUFACTURER_ID_LENGTH);

    CMemoryCopy(
            pFeliCaUserConnection->pCardList[0].aManufacturerID,
            &pBuffer[nIndex],
            P_FELICA_MANUFACTURER_ID_LENGTH);

    nIndex += P_FELICA_MANUFACTURER_ID_LENGTH;
    /* copy the Manufacuturer parameter bytes */
    CMemoryCopy(
            pFeliCaUserConnection->aManufacturerParameter,
            &pBuffer[nIndex],
            P_FELICA_MANUFACTURER_PARAMETER_LENGTH);

    CMemoryCopy(
            pFeliCaUserConnection->pCardList[0].aManufacturerParameter,
            &pBuffer[nIndex],
            P_FELICA_MANUFACTURER_PARAMETER_LENGTH);

    nIndex += P_FELICA_MANUFACTURER_PARAMETER_LENGTH;

    /* get the system entry */
    nSystemNumber = (( pFeliCaUserConnection->aManufacturerID[0] >> 4 ) & 0x0F );

    /* get the system code bytes */
    pFeliCaUserConnection->aSystemCodes[nSystemNumber] = PUtilReadUint16FromBigEndianBuffer(&pBuffer[nIndex]);
    pFeliCaUserConnection->pCardList[0].nSystemCode = pFeliCaUserConnection->aSystemCodes[nSystemNumber];

    return W_SUCCESS;
}

/** see tWBasicGenericDataCallbackFunction**/
static void static_PFeliCaRequestSystemCodeCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pCallbackParameter;
   uint8_t nResponseOffset = 0;
   uint8_t *pCardToReaderBuffer = pFeliCaUserConnection->pCardToReaderBuffer;

   /* Used for systems codes filling*/
   uint8_t nSystemCodes;
   uint8_t nIndexTmp;
   uint8_t nCurrentIndex;

   if ( nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaRequestSystemCodeCompleted : nError %d\n", nError);
      goto send_result;
   }

   /* skip the first byte of the answer */
   nResponseOffset = 1;

   /* if the received code doesn't match the request system code */
   if( pCardToReaderBuffer[nResponseOffset] != P_FELICA_COMMAND_CODE_REQUEST_SYSTEM_CODE_RESPONSE )
   {

      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      PDebugError("static_PFeliCaRequestSystemCodeCompleted :\n command code received ( %02x ) != request system code response( %02x )\n",
                  pCardToReaderBuffer[nResponseOffset],
                  P_FELICA_COMMAND_CODE_REQUEST_SYSTEM_CODE_RESPONSE );
      goto send_result;
   }

   nResponseOffset += 1;

   /* if actual IDm does not match the Idm received */
   if(   CMemoryCompare( &pCardToReaderBuffer[nResponseOffset],
         pFeliCaUserConnection->aManufacturerID,
         P_FELICA_MANUFACTURER_ID_LENGTH))
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      PDebugError("static_PFeliCaRequestSystemCodeCompleted : wrong manufacturer id");
      goto send_result;
   }

   nResponseOffset += P_FELICA_MANUFACTURER_ID_LENGTH;

   nSystemCodes = pCardToReaderBuffer[nResponseOffset ++];
   if(nSystemCodes > P_FELICA_MAX_SUB_SYSTEM)
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      PDebugError("static_PFeliCaRequestSystemCodeCompleted : \n\
                  nbSystem Code received ( %d ) > max system code ( %d )",
                  nSystemCodes,
                  P_FELICA_MAX_SUB_SYSTEM);
      goto send_result;
   }

   nCurrentIndex = ( ( pFeliCaUserConnection->aManufacturerID[0] >> 4 ) & 0x0F );

   for(nIndexTmp = 0; nIndexTmp < nCurrentIndex; ++nIndexTmp)
   {
      CMemoryCopy(   &pFeliCaUserConnection->aSystemCodes[nIndexTmp],
                     &pCardToReaderBuffer[nResponseOffset],
                     2 ); /* 2 for sizeof uint16_t */

      nResponseOffset += 2;
   }

   nResponseOffset += 2;

   for(nIndexTmp = nCurrentIndex + 1 ; nIndexTmp < nSystemCodes; ++nIndexTmp)
   {
      CMemoryCopy(   &pFeliCaUserConnection->aSystemCodes[nIndexTmp],
                     &pCardToReaderBuffer[nResponseOffset],
                     2 ); /* 2 for sizeof uint16_t */

      nResponseOffset += 2;
   }

   nError = W_SUCCESS;

send_result:

   /* this command may not be supported by all FeliCa Card */
   if(nError == W_ERROR_TIMEOUT)
   {
      nError = W_SUCCESS;
   }

   static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, nError);
}

/**
 * @brief   Send a request system codes commands.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pFeliCaUserConnection  FeliCa user connection structure.
 *
***/
static void static_PFeliCaRequestSystemCode(
            tContext* pContext,
            tPFeliCaUserConnection* pFeliCaUserConnection)
{
   uint8_t* pBuffer = pFeliCaUserConnection->pReaderToCardBuffer;
   uint16_t nOffset = 0;

   pBuffer[nOffset++] = P_FELICA_COMMAND_CODE_REQUEST_SYSTEM_CODE;
   CMemoryCopy(&pBuffer[nOffset],
               pFeliCaUserConnection->aManufacturerID,
               P_FELICA_MANUFACTURER_ID_LENGTH);

   nOffset += P_FELICA_MANUFACTURER_ID_LENGTH;

   /* Send the command */
   static_PFeliCaDriverExchangeData(pContext, pFeliCaUserConnection->hDriverConnection,
                           static_PFeliCaRequestSystemCodeCompleted, pFeliCaUserConnection,
                           pFeliCaUserConnection->pReaderToCardBuffer, nOffset,
                           pFeliCaUserConnection->pCardToReaderBuffer,
                           NAL_MESSAGE_MAX_LENGTH );
}

static void static_PFeliCaGetCardListCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tPFeliCaUserConnection * pFeliCaUserConnection = (tPFeliCaUserConnection *) pCallbackParameter;

   uint16_t nIndexBuffer = 0;
   uint8_t nIndexConnectionInfo = 0;
   uint8_t * pBuffer;

   pBuffer = pFeliCaUserConnection->pCardToReaderBuffer;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaGetCardListCompleted: Error on callback");
      static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, nError);
      return;
   }
   else
   {
      while(nIndexBuffer < nDataLength)
      {
         CMemoryCopy( pFeliCaUserConnection->pCardList[ nIndexConnectionInfo ].aManufacturerID,
                      & pBuffer [ nIndexBuffer ],
                      P_FELICA_MANUFACTURER_ID_LENGTH);

         nIndexBuffer += P_FELICA_MANUFACTURER_ID_LENGTH;

         CMemoryCopy( pFeliCaUserConnection->pCardList[ nIndexConnectionInfo ].aManufacturerParameter,
                      & pBuffer [ nIndexBuffer ],
                      P_FELICA_MANUFACTURER_PARAMETER_LENGTH);

         nIndexBuffer += P_FELICA_MANUFACTURER_PARAMETER_LENGTH;

         pFeliCaUserConnection->pCardList[ nIndexConnectionInfo ].nSystemCode = PUtilReadUint16FromBigEndianBuffer(&pBuffer[nIndexBuffer]);;

         nIndexBuffer += 2;

         nIndexConnectionInfo++;
      }
   }

   static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, nError);
}

static void static_PFeliCaRequestCardList(
            tContext* pContext,
            tPFeliCaUserConnection * pFeliCaUserConnection)
{
   static_PFeliCaDriverGetCardList(
            pContext,
            pFeliCaUserConnection->hDriverConnection,
            static_PFeliCaGetCardListCompleted,
            pFeliCaUserConnection,
            pFeliCaUserConnection->pCardToReaderBuffer,
            sizeof(pFeliCaUserConnection->pCardToReaderBuffer));
}


/* See Header file */
void PFeliCaUserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tPFeliCaUserConnection* pFeliCaUserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the FeliCa connection structure */
   pFeliCaUserConnection = (tPFeliCaUserConnection*)CMemoryAlloc( sizeof(tPFeliCaUserConnection) );
   if ( pFeliCaUserConnection == null )
   {
      PDebugError("PFeliCaUserCreateConnection: pFeliCaUserConnection == null");
      /* Send the error */
      PDFCPostContext2(
         &sCallbackContext,
         W_ERROR_OUT_OF_RESOURCE );
      return;
   }

   CMemoryFill(pFeliCaUserConnection, 0, sizeof(tPFeliCaUserConnection));

   /* Set the card number detected */
   pFeliCaUserConnection->nCardNumber = PReaderUserGetNbCardDetected(pContext);
   pFeliCaUserConnection->pCardList = (tWFeliCaConnectionInfo*)CMemoryAlloc( sizeof(tWFeliCaConnectionInfo) * pFeliCaUserConnection->nCardNumber);
   if ( pFeliCaUserConnection->pCardList == null )
   {
      PDebugError("PFeliCaUserCreateConnection: pFeliCaUserConnection->pCardList == null");
      /* Send the error */
      PDFCPostContext2(
         &sCallbackContext,
         W_ERROR_OUT_OF_RESOURCE );

      CMemoryFree( pFeliCaUserConnection );
      return;
   }

   /* initialize the systemCodes array */
   CMemoryFill(pFeliCaUserConnection->aSystemCodes, 0xFF, sizeof(pFeliCaUserConnection->aSystemCodes));

   /* Add the FeliCa connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     pFeliCaUserConnection,
                     P_HANDLE_TYPE_FELICA_USER_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PFeliCaUserCreateConnection: could not add the FeliCa connection");
      /* Send the error */
      PDFCPostContext2(
         &sCallbackContext,
         nError );
      CMemoryFree( pFeliCaUserConnection->pCardList );
      CMemoryFree( pFeliCaUserConnection );

      return;
   }

   /* Store the callback context */
   pFeliCaUserConnection->sCallbackContext = sCallbackContext;

   /* Store the connection information */
   pFeliCaUserConnection->hUserConnection   = hUserConnection;
   pFeliCaUserConnection->hDriverConnection = hDriverConnection;
   pFeliCaUserConnection->nProtocol         = nProtocol;

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hUserConnection, null, null);
   if (nError != W_SUCCESS)
   {
      /* Send the error */
      PDFCPostContext2(
         &sCallbackContext,
         nError );
      CMemoryFree( pFeliCaUserConnection->pCardList );
      CMemoryFree( pFeliCaUserConnection );

      return;
   }

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PFeliCaUserSendError when the operation is completed */
   PHandleIncrementReferenceCount(pFeliCaUserConnection);

   /* If multiCard */
   if(pFeliCaUserConnection->nCardNumber > 1)
   {
      CMemoryCopy(pFeliCaUserConnection->aManufacturerID, aIDMMultiCards, P_FELICA_MANUFACTURER_ID_LENGTH);

      static_PFeliCaRequestCardList(pContext, pFeliCaUserConnection);
   }
   else
   {
      /* Parse the card Information */
      static_FeliCaUserParse(
                        pContext,
                        pFeliCaUserConnection,
                        pBuffer,
                        nLength);

      static_PFeliCaRequestSystemCode(pContext,pFeliCaUserConnection);
   }
}

/* See Header file */
W_ERROR PFeliCaUserCheckType(
            tContext* pContext,
            W_HANDLE hUserConnection)
{
   tPFeliCaUserConnection* pFeliCaUserConnection;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hUserConnection,
      P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);
   if ( nError == W_SUCCESS )
   {
      /* Check the card type */
      if(pFeliCaUserConnection->nProtocol != W_PROP_FELICA)
      {
         PDebugError("PFeliCaUserCheckType: wrong card");
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }
   else
   {
      PDebugError("PFeliCaUserCheckType: could not get pFeliCaUserConnection buffer");
      return nError;
   }

   return nError;
}

/* Get identifier length */
static uint32_t static_PFeliCaUserGetIdentifierLength(
            tContext* pContext,
            void* pObject)
{
   return P_FELICA_MANUFACTURER_ID_LENGTH;
}

/* Get identifier */
static void static_PFeliCaUserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pObject;

   CMemoryCopy(pIdentifierBuffer, pFeliCaUserConnection->aManufacturerID, P_FELICA_MANUFACTURER_ID_LENGTH);
}

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

/* See Client API Specifications */
W_ERROR PFeliCaGetConnectionInfo(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tWFeliCaConnectionInfo *pConnectionInfo )
{
   tPFeliCaUserConnection* pFeliCaUserConnection;
   W_ERROR nError;
   uint8_t nSystemEntry;

    /* Check the parameters */
   if ( pConnectionInfo == null )
   {
      PDebugError("PFeliCaGetConnectionInfo: pConnectionInfo == null");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PReaderUserGetConnectionObject(pContext, hUserConnection,
      P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("PFeliCaGetConnectionInfo: could not get pFeliCaUserConnection ");
      /* Fill in the structure with zero */
      CMemoryFill(pConnectionInfo, 0, sizeof(*pConnectionInfo));
   }
   else
   {
      CMemoryCopy(&pConnectionInfo->aManufacturerID[0],
         &pFeliCaUserConnection[0],
         P_FELICA_MANUFACTURER_ID_LENGTH);

      /* copy the Manufacuturer ID bytes */
      CMemoryCopy(
             pConnectionInfo->aManufacturerID,
             pFeliCaUserConnection->aManufacturerID,
             P_FELICA_MANUFACTURER_ID_LENGTH);

      /* copy the Manufacuturer parameter bytes */
      CMemoryCopy(
             pConnectionInfo->aManufacturerParameter,
             pFeliCaUserConnection->aManufacturerParameter,
             P_FELICA_MANUFACTURER_PARAMETER_LENGTH);

      nSystemEntry = (( pConnectionInfo->aManufacturerID[0] >> 4) & 0x0F );

      /* get the system code bytes */
      pConnectionInfo->nSystemCode = pFeliCaUserConnection->aSystemCodes[nSystemEntry];
   }
   return nError;
}

#endif /* P_INCLUDE_DEPRECATED_FUNCTIONS */


static W_ERROR static_PFeliCaGetBlockListLength(
            uint8_t  nNumberOfService,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList,
            uint32_t* nBlockListLength)
{
   uint8_t i;
   uint8_t nBlockListElementLength;
   uint8_t *pTempBuffer;

   *nBlockListLength = 0;

   pTempBuffer = (uint8_t*)pBlockList;

   for (i = 0; i < nNumberOfBlocks; i++)
   {
      if ((pTempBuffer[0] & P_FELICA_LEN_MASK) == 0)
      {
         /* three bytes block */
         nBlockListElementLength = 1 + P_FELICA_BLOCK_NUMBER_LENGTH2;
      }
      else
      {
         /* two bytes block */
         nBlockListElementLength = 1 + P_FELICA_BLOCK_NUMBER_LENGTH1;
      }

      if ( ((pTempBuffer[0] & P_FELICA_ACCESS_MODE_MASK) != 0)                        ||
           ((pTempBuffer[0] & P_FELICA_SERVICE_CODE_ORDER_MASK) >= nNumberOfService)  ||
           ((pTempBuffer[0] &P_FELICA_SERVICE_CODE_ORDER_MASK) > nNumberOfService))
      {
         return W_ERROR_BAD_PARAMETER;
      }

      *nBlockListLength += nBlockListElementLength;
      pTempBuffer = pTempBuffer + nBlockListElementLength;
   }

   return W_SUCCESS;
}


/* See tWBasicGenericCallbackFunction */
static void static_PFeliCaReadCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            W_ERROR nError)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*) pCallbackParameter;

   PDebugTrace("static_PFeliCaReadCompleted");

   static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, nError);
}

/* See tWBasicGenericDataCallbackFunction */
static void static_PFeliCaReadInternalCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pCallbackParameter;
   uint8_t nResponseOffset = 0;
   uint8_t *pCardToReaderBuffer = pFeliCaUserConnection->pCardToReaderBuffer;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaReadInternalCompleted : nError %s", PUtilTraceError(nError));
      goto send_result;
   }

   /* skip the first byte of the answer */
   nResponseOffset = 1;

   if (pCardToReaderBuffer[nResponseOffset++] == P_FELICA_COMMAND_CODE_CHECK_RESPONSE)
   {
      /* check the IDm */
      if (CMemoryCompare(&pFeliCaUserConnection->aManufacturerID[0], &pCardToReaderBuffer[nResponseOffset],P_FELICA_MANUFACTURER_ID_LENGTH) != 0)
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto send_result;
      }

      nResponseOffset += P_FELICA_MANUFACTURER_ID_LENGTH;

      /* check the status flags */
      if (pCardToReaderBuffer[nResponseOffset++] == 0x00)
      {
         /* skip status flags and block number */
         nResponseOffset+=2;

         /* Store the answer */
         CMemoryCopy(pFeliCaUserConnection->pBuffer, &pCardToReaderBuffer[nResponseOffset], nDataLength- nResponseOffset);

         /* Set the successfull test */
         nError = W_SUCCESS;
      }
      else
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }
   else
   {
      PDebugError("static_PFeliCaReadInternalCompleted : invalid answer format");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
   }

send_result:

   PDFCPostContext2(&pFeliCaUserConnection->sCallbackContextInternal, nError);
}

static void static_PFeliCaReadInternalEx(
            tContext* pContext,
            tPFeliCaUserConnection* pFeliCaUserConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t nOffset = 0;
   uint32_t nBlockListLength = 0;

   PDebugTrace("static_PFeliCaReadInternalEx");

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check connection object */
   if (pFeliCaUserConnection == null)
   {
      PDebugError("static_PFeliCaReadInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( pFeliCaUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_PFeliCaReadInternalEx: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check parameters */
   if ((nNumberOfService < P_FELICA_MINIMUM_NUMBER_SERVICES)||
       (nNumberOfService > P_FELICA_MAXIMUM_NUMBER_SERVICES)||
       (nNumberOfBlocks < P_FELICA_MINIMUM_NUMBER_BLOCKS)||
       (nNumberOfBlocks > P_FELICA_MAXIMUM_READ_NUMBER_BLOCKS)||
       (nLength < (uint32_t)(nNumberOfBlocks * P_FELICA_BLOCK_SIZE)))
   {
      PDebugError("static_PFeliCaReadInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   nError = static_PFeliCaGetBlockListLength(nNumberOfService, nNumberOfBlocks, pBlockList, &nBlockListLength);
   if (nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaReadInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Store the callback context */
   pFeliCaUserConnection->sCallbackContextInternal = sCallbackContext;

   /*
      * Build the read without authentication command
      */
   nOffset = 0;

   pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = P_FELICA_COMMAND_CODE_CHECK;

   CMemoryCopy( &pFeliCaUserConnection->pReaderToCardBuffer[nOffset], &pFeliCaUserConnection->aManufacturerID, P_FELICA_MANUFACTURER_ID_LENGTH);
   nOffset += P_FELICA_MANUFACTURER_ID_LENGTH;

   pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = nNumberOfService;

   CMemoryCopy(&pFeliCaUserConnection->pReaderToCardBuffer[nOffset], pServiceCodeList, nNumberOfService * 2);
   nOffset += nNumberOfService * 2;

   pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = nNumberOfBlocks;

   CMemoryCopy(&pFeliCaUserConnection->pReaderToCardBuffer[nOffset], pBlockList, nBlockListLength);
   nOffset += nBlockListLength;

   pFeliCaUserConnection->pBuffer = pBuffer;

   /* Send the command */
   static_PFeliCaDriverExchangeData(pContext, pFeliCaUserConnection->hDriverConnection,
                           static_PFeliCaReadInternalCompleted, pFeliCaUserConnection,
                           pFeliCaUserConnection->pReaderToCardBuffer, nOffset,
                           pFeliCaUserConnection->pCardToReaderBuffer,
                           NAL_MESSAGE_MAX_LENGTH);

   return;

return_error:
   PDebugError("static_PFeliCaReadInternalEx: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header */
void PFeliCaReadInternal(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = null;

   /* Check if the connection handle is valid */
   if (PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection) != W_SUCCESS)
   {
      pFeliCaUserConnection = null;
   }

   static_PFeliCaReadInternalEx(
         pContext,
         pFeliCaUserConnection,
         pCallback, pCallbackParameter,
         pBuffer,
         nLength,
         nNumberOfService,
         pServiceCodeList,
         nNumberOfBlocks,
         pBlockList);
}

/* See Client API Specifications */
void PFeliCaRead(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList,
            W_HANDLE* phOperation )
{
   tPFeliCaUserConnection* pFeliCaUserConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;
   uint32_t nBlockListLength = 0;

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);
   if (pFeliCaUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PFeliCaRead: Bad handle");
      goto return_error;
   }

   /* Check the protocol type */
   if (pFeliCaUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN)
   {
      PDebugError("PFeliCaRead: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check parameters */
   if ((nNumberOfService < P_FELICA_MINIMUM_NUMBER_SERVICES)||
       (nNumberOfService > P_FELICA_MAXIMUM_NUMBER_SERVICES)||
       (nNumberOfBlocks < P_FELICA_MINIMUM_NUMBER_BLOCKS)||
       (nNumberOfBlocks > P_FELICA_MAXIMUM_READ_NUMBER_BLOCKS)||
       (nLength < (uint32_t)(nNumberOfBlocks * P_FELICA_BLOCK_SIZE)))
   {
      PDebugError("PFeliCaRead: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   nError = static_PFeliCaGetBlockListLength(nNumberOfService, nNumberOfBlocks, pBlockList, &nBlockListLength);
   if (nError != W_SUCCESS)
   {
      PDebugError("PFeliCaRead: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PFeliCaRead: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PFeliCaRead: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hUserConnection, static_PFeliCaExecuteQueuedExchange, pFeliCaUserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Store the operation handle */
      CDebugAssert(pFeliCaUserConnection->hOperation == W_NULL_HANDLE);
      pFeliCaUserConnection->hOperation = hOperation;

      /* Store the callback context */
      pFeliCaUserConnection->sCallbackContext = sCallbackContext;

      /* Read data */
      static_PFeliCaReadInternalEx(
            pContext,
            pFeliCaUserConnection,
            static_PFeliCaReadCompleted, pFeliCaUserConnection,
            pBuffer,
            nLength,
            nNumberOfService,
            pServiceCodeList,
            nNumberOfBlocks,
            pBlockList);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Save the operation handle */
      CDebugAssert(pFeliCaUserConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pFeliCaUserConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pFeliCaUserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pFeliCaUserConnection->sQueuedOperation.nType = P_FELICA_QUEUED_READ;

      /* Save data */
      pFeliCaUserConnection->sQueuedOperation.pBuffer = pBuffer;
      pFeliCaUserConnection->sQueuedOperation.nLength = nLength;
      pFeliCaUserConnection->sQueuedOperation.nNumberOfService = nNumberOfService;
      CMemoryCopy(&pFeliCaUserConnection->sQueuedOperation.aServiceCodeList, pServiceCodeList, nNumberOfService * 2);
      pFeliCaUserConnection->sQueuedOperation.nNumberOfBlocks = nNumberOfBlocks;
      CMemoryCopy(&pFeliCaUserConnection->sQueuedOperation.aBlockList, pBlockList, nBlockListLength);

      return;

   default:
      /* Return this error */

      if(hOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PFeliCaRead: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See tWBasicGenericCallbackFunction */
static void static_PFeliCaWriteCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            W_ERROR nError)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*) pCallbackParameter;

   PDebugTrace("static_PFeliCaWriteCompleted");

   static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, nError);
}

/* See tWBasicGenericDataCallbackFunction */
static void static_PFeliCaWriteInternalCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*) pCallbackParameter;
   uint8_t nResponseOffset = 0;
   uint8_t *pCardToReaderBuffer = pFeliCaUserConnection->pCardToReaderBuffer;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaWriteInternalCompleted : nError %s", PUtilTraceError(nError));
      goto send_result;
   }

   /* skip the first byte of the answer */
   nResponseOffset = 1;

   if (pCardToReaderBuffer[nResponseOffset++] == P_FELICA_COMMAND_CODE_UPDATE_RESPONSE)
   {
      /* check the IDm */
      if (CMemoryCompare(&pFeliCaUserConnection->aManufacturerID[0], &pCardToReaderBuffer[nResponseOffset],P_FELICA_MANUFACTURER_ID_LENGTH) != 0)
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto send_result;
      }

      nResponseOffset += P_FELICA_MANUFACTURER_ID_LENGTH;

      /* check the status flags */
      if (pCardToReaderBuffer[nResponseOffset++] == 0x00)
      {
         nError = W_SUCCESS;
      }
      else
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }
   else
   {
      PDebugError("static_PFeliCaWriteInternalCompleted : invalid answer format");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
   }

send_result:

   PDFCPostContext2(&pFeliCaUserConnection->sCallbackContextInternal, nError);
}

/* See Client API Specifications */
static void static_PFeliCaWriteInternalEx(
            tContext* pContext,
            tPFeliCaUserConnection* pFeliCaUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t *pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t nOffset = 0;
   uint32_t nBlockListLength = 0;

   PDebugTrace("static_PFeliCaReadInternalEx");

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check connection object */
   if (pFeliCaUserConnection == null)
   {
      PDebugError("static_PFeliCaWriteInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( pFeliCaUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_PFeliCaWriteInternalEx: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check parameters */
   if ((nNumberOfService < P_FELICA_MINIMUM_NUMBER_SERVICES)      ||
       (nNumberOfService> P_FELICA_MAXIMUM_NUMBER_SERVICES)       ||
       (nNumberOfBlocks < P_FELICA_MINIMUM_NUMBER_BLOCKS)         ||
       (nNumberOfBlocks > P_FELICA_MAXIMUM_WRITE_NUMBER_BLOCKS)   ||
       (nLength < (uint32_t)(nNumberOfBlocks * P_FELICA_BLOCK_SIZE)))
   {
      PDebugError("static_PFeliCaWriteInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   nError = static_PFeliCaGetBlockListLength(nNumberOfService, nNumberOfBlocks, pBlockList, &nBlockListLength);
   if (nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaWriteInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Store the callback context */
   pFeliCaUserConnection->sCallbackContextInternal = sCallbackContext;

   /*
      * Build the write without authentication command
      */
   nOffset = 0;

   pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = P_FELICA_COMMAND_CODE_UPDATE;

   CMemoryCopy( &pFeliCaUserConnection->pReaderToCardBuffer[nOffset], &pFeliCaUserConnection->aManufacturerID[0], P_FELICA_MANUFACTURER_ID_LENGTH);
   nOffset += P_FELICA_MANUFACTURER_ID_LENGTH;

   pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = nNumberOfService;

   CMemoryCopy(&pFeliCaUserConnection->pReaderToCardBuffer[nOffset], pServiceCodeList, nNumberOfService * 2);
   nOffset += nNumberOfService * 2;

   pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = nNumberOfBlocks;

   CMemoryCopy(&pFeliCaUserConnection->pReaderToCardBuffer[nOffset], pBlockList, nBlockListLength);
   nOffset += nBlockListLength;

   CMemoryCopy(&pFeliCaUserConnection->pReaderToCardBuffer[nOffset], pBuffer, nNumberOfBlocks * P_FELICA_BLOCK_SIZE);
   nOffset += nNumberOfBlocks * P_FELICA_BLOCK_SIZE;

   /* Send the command */
   static_PFeliCaDriverExchangeData(pContext, pFeliCaUserConnection->hDriverConnection,
                           static_PFeliCaWriteInternalCompleted, pFeliCaUserConnection,
                           pFeliCaUserConnection->pReaderToCardBuffer, nOffset,
                           pFeliCaUserConnection->pCardToReaderBuffer,
                           NAL_MESSAGE_MAX_LENGTH);

   return;

return_error:
   PDebugError("static_PFeliCaWriteInternalEx: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header */
void PFeliCaWriteInternal(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t *pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = null;

   /* Check if the connection handle is valid */
   if (PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection) != W_SUCCESS)
   {
      pFeliCaUserConnection = null;
   }

   static_PFeliCaWriteInternalEx(
         pContext,
         pFeliCaUserConnection,
         pCallback, pCallbackParameter,
         pBuffer,
         nLength,
         nNumberOfService,
         pServiceCodeList,
         nNumberOfBlocks,
         pBlockList);
}

/* See Client API Specifications */
void PFeliCaWrite(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t *pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList,
            W_HANDLE* phOperation )
{
   tPFeliCaUserConnection* pFeliCaUserConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;
   uint32_t nBlockListLength = 0;

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);
   if (pFeliCaUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PFeliCaWrite: Bad handle");
      goto return_error;
   }

   /* Check the protocol type */
   if ( pFeliCaUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("PFeliCaWrite: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check parameters */
   if ((nNumberOfService < P_FELICA_MINIMUM_NUMBER_SERVICES)      ||
       (nNumberOfService> P_FELICA_MAXIMUM_NUMBER_SERVICES)       ||
       (nNumberOfBlocks < P_FELICA_MINIMUM_NUMBER_BLOCKS)         ||
       (nNumberOfBlocks > P_FELICA_MAXIMUM_WRITE_NUMBER_BLOCKS)   ||
       (nLength < (uint32_t)(nNumberOfBlocks * P_FELICA_BLOCK_SIZE)))
   {
      PDebugError("PFeliCaWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   nError = static_PFeliCaGetBlockListLength(nNumberOfService, nNumberOfBlocks, pBlockList, &nBlockListLength);
   if (nError != W_SUCCESS)
   {
      PDebugError("PFeliCaWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PFeliCaWrite: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PFeliCaWrite: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hUserConnection, static_PFeliCaExecuteQueuedExchange, pFeliCaUserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Store the operation handle */
      CDebugAssert(pFeliCaUserConnection->hOperation == W_NULL_HANDLE);
      pFeliCaUserConnection->hOperation = hOperation;

      /* Store the callback context */
      pFeliCaUserConnection->sCallbackContext = sCallbackContext;

      /*
       * Build the write without authentication command
       */
      static_PFeliCaWriteInternalEx(
            pContext,
            pFeliCaUserConnection,
            static_PFeliCaWriteCompleted, pFeliCaUserConnection,
            pBuffer,
            nLength,
            nNumberOfService,
            pServiceCodeList,
            nNumberOfBlocks,
            pBlockList);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Save the operation handle */
      CDebugAssert(pFeliCaUserConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pFeliCaUserConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pFeliCaUserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pFeliCaUserConnection->sQueuedOperation.nType = P_FELICA_QUEUED_WRITE;

      /* Save data */
      pFeliCaUserConnection->sQueuedOperation.pBuffer = (uint8_t*) pBuffer;
      pFeliCaUserConnection->sQueuedOperation.nLength = nLength;
      pFeliCaUserConnection->sQueuedOperation.nNumberOfService = nNumberOfService;
      CMemoryCopy(&pFeliCaUserConnection->sQueuedOperation.aServiceCodeList, pServiceCodeList, nNumberOfService * 2);
      pFeliCaUserConnection->sQueuedOperation.nNumberOfBlocks = nNumberOfBlocks;
      CMemoryCopy(&pFeliCaUserConnection->sQueuedOperation.aBlockList, pBlockList, nBlockListLength);

      return;

   default:
      /* Return this error */
      if(hOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PFeliCaWrite: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}


/* See tWBasicGenericDataCallbackFunction */
static void static_PFeliCaDriverExchangeDataCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pCallbackParameter;

   if ( nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaDriverExchangeDataCompleted : nError %d\n", nError);
   }

   PDFCPostContext3(&pFeliCaUserConnection->sCallbackContext, nDataLength, nError);
}

/* See Client API Specifications */
static void static_PFeliCaUserExchangeData(
      tContext* pContext,
      void* pObject,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength,
      W_HANDLE * phOperation)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pObject;
   tDFCCallbackContext sCallbackContext;

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   if ((pReaderToCardBuffer == null) || (nReaderToCardBufferLength == 0) || (pCardToReaderBuffer == null) || (nCardToReaderBufferMaxLength == 0))
   {
      PDebugError("static_PFeliCaUserExchangeData: W_ERROR_BAD_PARAMETER");

      /* Send the error */
      PDFCPostContext2(&sCallbackContext, W_ERROR_BAD_PARAMETER );
      return;
   }

   /* Get an operation handle */
   if(phOperation != null)
   {
      *phOperation = PBasicCreateOperation( pContext, null, null );

      if (* phOperation == W_NULL_HANDLE)
      {
         PDebugError("static_PFeliCaUserExchangeData: PBasicCreateOperation : out of resource");

         /* Send the error */
         PDFCPostContext2(&sCallbackContext, W_ERROR_BAD_STATE );
         return;
      }
   }

   /* Store the connection information */
   pFeliCaUserConnection->sCallbackContext   = sCallbackContext;

   /* Check the protocol type */
   if ( pFeliCaUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("PFeliCaWrite: W_ERROR_CONNECTION_COMPATIBILITY");

      /* Send the error */
      static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, W_ERROR_CONNECTION_COMPATIBILITY);
      return;
   }

   PFeliCaDriverExchangeData(pContext, pFeliCaUserConnection->hDriverConnection, static_PFeliCaDriverExchangeDataCompleted, pFeliCaUserConnection,
                                 pReaderToCardBuffer, nReaderToCardBufferLength, pCardToReaderBuffer, nCardToReaderBufferMaxLength );
}

/* See Client API Specifications */
W_ERROR PFeliCaListenToCardDetection (
            tContext* pContext,
            tPReaderCardDetectionHandler *pHandler,
            void *pHandlerParameter,
            uint8_t nPriority,
            uint16_t nSystemCode,
            W_HANDLE *phEventRegistry)
{
   /* Send to the NFC Controller the system code value */
   tPFeliCaDetectionConfiguration sDetectionConfiguration, *pDetectionConfigurationBuffer;
   uint32_t nDetectionConfigurationBufferLength;
   static const uint8_t nProtocol = W_PROP_FELICA;

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   /* The anti-collision parameter shall be 0xFFFF if W_PRIORITY_EXCLUSIVE is not used*/
   if(nSystemCode != 0xFFFF)
   {
      if(nPriority != W_PRIORITY_EXCLUSIVE)
      {
         PDebugError("PFeliCaListenToCardDetection: Bad Detection configuration parameters or bad priorty type");
         return W_ERROR_BAD_PARAMETER;

      }
      else
      {
         /* set the pointer of configuration buffer and  buffer length*/
         pDetectionConfigurationBuffer = &sDetectionConfiguration;
         nDetectionConfigurationBufferLength = sizeof(tPFeliCaDetectionConfiguration);
         /* set the systemcode*/
         PUtilWriteUint16ToBigEndianBuffer(
                                      nSystemCode,
                                      &sDetectionConfiguration.aSystemCode[0]);
      }
   }
   else /* The System code is not used*/
   {
      pDetectionConfigurationBuffer = null;
      nDetectionConfigurationBufferLength = 0x00;

   }
   /* Launch the card detection request */
   return PReaderUserListenToCardDetection(
                     pContext,
                     pHandler, pHandlerParameter,
                     nPriority,
                     &nProtocol, 1,
                     pDetectionConfigurationBuffer, nDetectionConfigurationBufferLength,
                     phEventRegistry );
}

/* See Client API Specifications */
W_ERROR WFeliCaReadSync(
            W_HANDLE hUserConnection,
            uint8_t* pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WFeliCaRead(
            hUserConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nLength,
            nNumberOfService, pServiceCodeList, nNumberOfBlocks, pBlockList,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WFeliCaWriteSync(
            W_HANDLE hUserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WFeliCaWrite(
         hUserConnection,
         PBasicGenericSyncCompletion,
         &param,
         pBuffer, nLength,
         nNumberOfService, pServiceCodeList, nNumberOfBlocks, pBlockList,
         null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* After Select system complete */
static void static_PFeliCaSelectSystemCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*)pCallbackParameter;
   uint8_t nResponseOffset = 0;
   uint8_t *pCardToReaderBuffer = pFeliCaUserConnection->pCardToReaderBuffer;
   uint8_t nOldSystemEntry;

   /* save the old system entry */
   nOldSystemEntry = ( ( pFeliCaUserConnection->aManufacturerID[0] >> 4 ) & 0x0F ) ;

   if ( nError != W_SUCCESS)
   {
      PDebugError("static_PFeliCaSelectSystemCompleted : nError %d\n", nError);
      goto send_result;
   }

   nResponseOffset = 1;

   if (pCardToReaderBuffer[nResponseOffset++] == P_FELICA_COMMAND_CODE_EXIST_RESPONSE)
   {
      pFeliCaUserConnection->aManufacturerID[0] =  ( pFeliCaUserConnection->aManufacturerID[0] & 0x0F )
                                                |  ( pCardToReaderBuffer[nResponseOffset] & 0xF0 );

      /* check the IDm */
      if (CMemoryCompare(&pFeliCaUserConnection->aManufacturerID[0], &pCardToReaderBuffer[nResponseOffset], P_FELICA_MANUFACTURER_ID_LENGTH) != 0)
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto send_result;
      }

      nResponseOffset += P_FELICA_MANUFACTURER_ID_LENGTH;
   }
   else
   {
      PDebugError("static_PFeliCaSelectSystemCompleted : invalid answer format");
      pFeliCaUserConnection->aManufacturerID[0] =  ( pFeliCaUserConnection->aManufacturerID[0] & 0x0F )
                                                |  ( ( nOldSystemEntry << 4 ) & 0xF0);
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
   }


send_result:
   static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, nError);
}


/* See Client API Specifications */
void PFeliCaSelectSystem(
               tContext* pContext,
               W_HANDLE hUserConnection,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter,
               const uint8_t nIndexSubSystem)
{
   tPFeliCaUserConnection * pFeliCaUserConnection;
   tDFCCallbackContext sCallbackContext;
   uint32_t nOffset;
   W_ERROR nError = W_SUCCESS;
   uint8_t nSystemEntry;

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);
   if (pFeliCaUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PFeliCaSelectSystem: Bad handle");
      goto return_error;
   }

   /* Check the protocol type */
   if (pFeliCaUserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN)
   {
      PDebugError("PFeliCaSelectSystem: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   if (nIndexSubSystem >= P_FELICA_MAX_SUB_SYSTEM)
   {
      PDebugError("PFeliCaSelectSystem: Sub system number ( %d ) > ( %d ) MAX SUB SYSTEM NUMBER",
                  nIndexSubSystem,
                  P_FELICA_MAX_SUB_SYSTEM);
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   nSystemEntry = (( pFeliCaUserConnection->aManufacturerID[0] >> 4 ) & 0x0F );

   /* If an object already exist, we reject the demand with an exclusive rejected error */
   if (nSystemEntry == nIndexSubSystem)
   {
      PDebugError("PFeliCaSelectSystem: A connection for the subsystem[%d] already exists", nIndexSubSystem);
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hUserConnection, static_PFeliCaExecuteQueuedExchange, pFeliCaUserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Store the callback context */
      pFeliCaUserConnection->sCallbackContext = sCallbackContext;

      /*
       * Build the get response command
       */
      nOffset = 0;
      pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = P_FELICA_COMMAND_CODE_EXIST;

      /* replace the first half byte by the nIndexSubSystem */
      pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = (( pFeliCaUserConnection->aManufacturerID[0] & 0x0F) | nIndexSubSystem << 4);

      CMemoryCopy( &pFeliCaUserConnection->pReaderToCardBuffer[nOffset], &pFeliCaUserConnection->aManufacturerID[1], (P_FELICA_MANUFACTURER_ID_LENGTH - 1) );
      nOffset += P_FELICA_MANUFACTURER_ID_LENGTH - 1;

      /* Send the command */
      static_PFeliCaDriverExchangeData(pContext, pFeliCaUserConnection->hDriverConnection,
                              static_PFeliCaSelectSystemCompleted, pFeliCaUserConnection,
                              pFeliCaUserConnection->pReaderToCardBuffer, nOffset,
                              pFeliCaUserConnection->pCardToReaderBuffer,
                              NAL_MESSAGE_MAX_LENGTH );

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Save callback context */
      pFeliCaUserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pFeliCaUserConnection->sQueuedOperation.nType = P_FELICA_QUEUED_SELECT_SYSTEM;

      /* Save data */
      pFeliCaUserConnection->sQueuedOperation.nIndexSubSystem = nIndexSubSystem;

      return;

   default:
      /* Return this error */
      goto return_error;
   }

return_error:
   PDebugError("PFeliCaSelectSystem: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See Client API Specifications */
W_ERROR WFeliCaSelectSystemSync(
               W_HANDLE hUserConnection,
               const uint8_t   nIndexSubSystem)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WFeliCaSelectSystem(
         hUserConnection,
         PBasicGenericSyncCompletion,
         &param,
         nIndexSubSystem);
   }

   return PBasicGenericSyncWaitForResult(&param);
}


/* See Client API Specifications */
void PFeliCaSelectCard(
               tContext* pContext,
               W_HANDLE hConnection,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter,
               const tWFeliCaConnectionInfo * pFeliCaConnectionInfo)
{
   tPFeliCaUserConnection * pFeliCaUserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   uint8_t nCurrentIndex;

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);
   if (pFeliCaUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PFeliCaSelectCard: Bad handle");
      goto return_error;
   }

   if( pFeliCaConnectionInfo == null)
   {
      PDebugError("pFeliCaConnectionInfo == null : BAD PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PFeliCaExecuteQueuedExchange, pFeliCaUserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Store the callback context */
      pFeliCaUserConnection->sCallbackContext = sCallbackContext;

      /* Copy data */
      CMemoryCopy( pFeliCaUserConnection->aManufacturerID, pFeliCaConnectionInfo->aManufacturerID, P_FELICA_MANUFACTURER_ID_LENGTH);
      CMemoryCopy( pFeliCaUserConnection->aManufacturerParameter, pFeliCaConnectionInfo->aManufacturerParameter, P_FELICA_MANUFACTURER_PARAMETER_LENGTH);
      nCurrentIndex = ( ( pFeliCaUserConnection->aManufacturerParameter[0] >> 4 ) & 0x0F);
      pFeliCaUserConnection->aSystemCodes[nCurrentIndex] = pFeliCaConnectionInfo->nSystemCode;

      /* Ask card to have the system codes list and call the callback after having the response*/
      static_PFeliCaRequestSystemCode(pContext, pFeliCaUserConnection);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PFeliCaUserSendError() when the operation is completed */
      PHandleIncrementReferenceCount(pFeliCaUserConnection);

      /* Save callback context */
      pFeliCaUserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pFeliCaUserConnection->sQueuedOperation.nType = P_FELICA_QUEUED_SELECT_CARD;

      /* Save data */
      pFeliCaUserConnection->sQueuedOperation.pFeliCaConnectionInfo = pFeliCaConnectionInfo;

      return;

   default:
      /* Return this error */
      goto return_error;
   }

return_error:
   PDebugError("PFeliCaRead: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See Client API Specifications */
W_ERROR WFeliCaSelectCardSync(
               W_HANDLE hConnection,
               const tWFeliCaConnectionInfo *   pFeliCaConnectionInfo)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WFeliCaSelectCard(
         hConnection,
         PBasicGenericSyncCompletion,
         &param,
         pFeliCaConnectionInfo);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See API Specifications */
W_ERROR PFeliCaGetCardNumber(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t * pnCardNumber)
{
   W_ERROR nError = W_SUCCESS;
   tPFeliCaUserConnection * pFeliCaUserConnection;

   if(pnCardNumber == null)
   {
       PDebugError("PFeliCaGetNumberCard: pnCardNumber null");
       return W_ERROR_BAD_PARAMETER;
   }

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("PFeliCaGetNumberCard: INVALID HANDLE");
      *pnCardNumber = 0;
      return nError;
   }

   *pnCardNumber = pFeliCaUserConnection->nCardNumber;
   return W_SUCCESS;
}

W_ERROR PFeliCaGetCardList(
                   tContext* pContext,
                   W_HANDLE hConnection,
                   tWFeliCaConnectionInfo * aFeliCaConnectionInfos,
                   const uint32_t nArraySize )
{
   tPFeliCaUserConnection * pFeliCaUserConnection;
   W_ERROR nError = W_SUCCESS;
   uint32_t nIndex;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_FELICA_USER_CONNECTION, (void**)&pFeliCaUserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("PFeliCaGetCardList: INVALID HANDLE");
      return nError;
   }

   if(aFeliCaConnectionInfos == null || nArraySize < pFeliCaUserConnection->nCardNumber)
   {
      PDebugError("PFeliCaGetCardList: BAD PARAMETER \n\
                   aFeliCaConnectionInfos => %p\n\
                   nArraySize => %d\n\
                   pFeliCaConnection->nCardNumber => %d", aFeliCaConnectionInfos, nArraySize,pFeliCaUserConnection->nCardNumber );
      return W_ERROR_BAD_PARAMETER;
   }

   for(nIndex = 0; nIndex < pFeliCaUserConnection->nCardNumber; ++nIndex)
   {
      aFeliCaConnectionInfos[nIndex] = pFeliCaUserConnection->pCardList[nIndex];
   }

   return W_SUCCESS;
}

/* See tWBasicGenericDataCallbackFunction */
static void static_PFeliCaPollCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tPFeliCaUserConnection * pFeliCaUserConnection = (tPFeliCaUserConnection *) pCallbackParameter;

   /* Send the error */
   PDFCPostContext2(&pFeliCaUserConnection->sCallbackContext, nError);

   /* Release the reference after completion. May destroy the object */
   PHandleDecrementReferenceCount(pContext, pFeliCaUserConnection);
}

/* See header file */
static void static_PFeliCaPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection *) pObject;

   CDebugAssert( pFeliCaUserConnection != null
              && pFeliCaUserConnection->nProtocol == W_PROP_FELICA);

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PFeliCaPollCompleted callback  */
   PHandleIncrementReferenceCount(pFeliCaUserConnection);

   /* store the callback context */
   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &pFeliCaUserConnection->sCallbackContext );

   /* Build the command */
   /* Poll command */
   pFeliCaUserConnection->pReaderToCardBuffer[0] = P_FELICA_COMMAND_CODE_POLL;
   /* System Code */
   CMemoryCopy(&pFeliCaUserConnection->pReaderToCardBuffer[1],
               pFeliCaUserConnection->aSystemCodes,
               P_FELICA_SYSTEM_CODE_SIZE);
   /* Request code: OxOO: no request */
   pFeliCaUserConnection->pReaderToCardBuffer[3] = 0x00;
   /* Time slot */
   pFeliCaUserConnection->pReaderToCardBuffer[4] = 0x04;

   /* Send the command */
   static_PFeliCaDriverExchangeData(
                           pContext, pFeliCaUserConnection->hDriverConnection,
                           static_PFeliCaPollCompleted, pFeliCaUserConnection,
                           pFeliCaUserConnection->pReaderToCardBuffer, 5,
                           pFeliCaUserConnection->pCardToReaderBuffer,
                           NAL_MESSAGE_MAX_LENGTH );
}

/* Execute the queued operation (after polling) */
static void static_PFeliCaExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tPFeliCaUserConnection* pFeliCaUserConnection = (tPFeliCaUserConnection*) pObject;

   PDebugTrace("static_PFeliCaExecuteQueuedExchange");

   /* Restore operation handle */
   pFeliCaUserConnection->hOperation = pFeliCaUserConnection->sQueuedOperation.hOperation;
   /* Restore callback context */
   pFeliCaUserConnection->sCallbackContext = pFeliCaUserConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (pFeliCaUserConnection->hOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pFeliCaUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PFeliCaExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, nResult);
   }
   else
   {
      uint32_t nOffset = 0;

      switch (pFeliCaUserConnection->sQueuedOperation.nType)
      {
      case P_FELICA_QUEUED_READ:
         /* Read data */
         static_PFeliCaReadInternalEx(
               pContext,
               pFeliCaUserConnection,
               static_PFeliCaReadCompleted, pFeliCaUserConnection,
               pFeliCaUserConnection->sQueuedOperation.pBuffer,
               pFeliCaUserConnection->sQueuedOperation.nLength,
               pFeliCaUserConnection->sQueuedOperation.nNumberOfService,
               pFeliCaUserConnection->sQueuedOperation.aServiceCodeList,
               pFeliCaUserConnection->sQueuedOperation.nNumberOfBlocks,
               pFeliCaUserConnection->sQueuedOperation.aBlockList);

         break;

      case P_FELICA_QUEUED_WRITE:
         static_PFeliCaWriteInternalEx(
               pContext,
               pFeliCaUserConnection,
               static_PFeliCaWriteCompleted, pFeliCaUserConnection,
               pFeliCaUserConnection->sQueuedOperation.pBuffer,
               pFeliCaUserConnection->sQueuedOperation.nLength,
               pFeliCaUserConnection->sQueuedOperation.nNumberOfService,
               pFeliCaUserConnection->sQueuedOperation.aServiceCodeList,
               pFeliCaUserConnection->sQueuedOperation.nNumberOfBlocks,
               pFeliCaUserConnection->sQueuedOperation.aBlockList);

         break;

      case P_FELICA_QUEUED_SELECT_CARD:
         /* Copy data */
         CMemoryCopy( pFeliCaUserConnection->aManufacturerID, pFeliCaUserConnection->sQueuedOperation.pFeliCaConnectionInfo->aManufacturerID, P_FELICA_MANUFACTURER_ID_LENGTH);
         CMemoryCopy( pFeliCaUserConnection->aManufacturerParameter, pFeliCaUserConnection->sQueuedOperation.pFeliCaConnectionInfo->aManufacturerParameter, P_FELICA_MANUFACTURER_PARAMETER_LENGTH);
         nOffset = ( ( pFeliCaUserConnection->aManufacturerParameter[0] >> 4 ) & 0x0F);
         pFeliCaUserConnection->aSystemCodes[nOffset] = pFeliCaUserConnection->sQueuedOperation.pFeliCaConnectionInfo->nSystemCode;

         /* Ask card to have the system codes list and call the callback after having the response*/
         static_PFeliCaRequestSystemCode(pContext, pFeliCaUserConnection);

         break;

      case P_FELICA_QUEUED_SELECT_SYSTEM:
         /* Build the get response command */
         nOffset = 0;
         pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = P_FELICA_COMMAND_CODE_EXIST;

         /* replace the first half byte by the nIndexSubSystem */
         pFeliCaUserConnection->pReaderToCardBuffer[nOffset++] = (( pFeliCaUserConnection->aManufacturerID[0] & 0x0F) | pFeliCaUserConnection->sQueuedOperation.nIndexSubSystem << 4);

         CMemoryCopy( &pFeliCaUserConnection->pReaderToCardBuffer[nOffset], &pFeliCaUserConnection->aManufacturerID[1], (P_FELICA_MANUFACTURER_ID_LENGTH - 1) );
         nOffset += P_FELICA_MANUFACTURER_ID_LENGTH - 1;

         /* Send the command */
         static_PFeliCaDriverExchangeData(pContext, pFeliCaUserConnection->hDriverConnection,
                                 static_PFeliCaSelectSystemCompleted, pFeliCaUserConnection,
                                 pFeliCaUserConnection->pReaderToCardBuffer, nOffset,
                                 pFeliCaUserConnection->pCardToReaderBuffer,
                                 NAL_MESSAGE_MAX_LENGTH );

         break;

      default:
         /* Return an error */
         PDebugError("static_PFeliCaExecuteQueuedExchange: unknown type of operation!");
         static_PFeliCaUserSendError(pContext, pFeliCaUserConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&pFeliCaUserConnection->sQueuedOperation, 0, sizeof(pFeliCaUserConnection->sQueuedOperation));
}


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* the maximum frame size */
#define P_FELICA_BUFFER_MAX_SIZE            256
/* Declare a connection structure */
typedef struct __tFeliCaDriverConnection
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

   /* Connection information */
   uint8_t                   nTimeout;
   /* response buffer */
   uint8_t*                   pCardToReaderBufferFeliCa;
   /* maximum length of response buffer*/
   uint32_t                   nCardToReaderBufferMaxLengthFeliCa;

   /* Buffer for the payload of a NFC HAL command */
   uint8_t                    aReaderToCardBufferNAL[P_FELICA_BUFFER_MAX_SIZE];

   /* Callback context */
   tDFCDriverCCReference      pDriverCC;

   /* Service Context */
   uint8_t                    nServiceIdentifier;
   /* Service Operation */
   tNALServiceOperation       sServiceOperation;
} tFeliCaDriverConnection;

/* Destroy connection callback */
static uint32_t static_PFeliCaDriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Connection registry */
tHandleType g_sFeliCaDriverConnection = { static_PFeliCaDriverDestroyConnection,
                                             null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_FELICA_DRIVER_CONNECTION (&g_sFeliCaDriverConnection)

/**
 * @brief   Destroyes a FeliCa connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PFeliCaDriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{

   tFeliCaDriverConnection* pFeliCaDriverConnection = (tFeliCaDriverConnection*)pObject;

   PDebugTrace("static_PFeliCaDriverDestroyConnection");

   PDFCDriverFlushCall(pFeliCaDriverConnection->pDriverCC);


   /* Free the connection structure */
   CMemoryFree( pFeliCaDriverConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @see  PNALServiceExecuteCommand
 **/
static void static_PFeliCaDriverExecuteCommandCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter)
{
   tFeliCaDriverConnection* pFeliCaDriverConnection = (tFeliCaDriverConnection*)pCallbackParameter;
   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PDebugError(
         "static_PFeliCaDriverExecuteCommandCompleted: nError %s",
         PUtilTraceError(nError) );
      if ( nError == W_ERROR_CANCEL )
      {
         goto return_function;
      }
      nLength = 0;
   }
   else
   {
      PDebugTrace("static_PFeliCaDriverExecuteCommandCompleted: Response");

      PReaderDriverSetAntiReplayReference(pContext);

      /* Check if the buffer is to short */
      if ( nLength > pFeliCaDriverConnection->nCardToReaderBufferMaxLengthFeliCa )
      {
         nLength = 0;
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   /* Send the answer */
   PDFCDriverPostCC3(
      pFeliCaDriverConnection->pDriverCC,
      nLength,
      nError );

return_function:

   /* Release the reference after completion
      May destroy pFeliCaDriverConnection */
   PHandleDecrementReferenceCount(pContext, pFeliCaDriverConnection);
}


/**
 * @see  PNALServiceExecuteCommand
 **/
static void static_PFeliCaDriverGetCardListCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError)
{
   tFeliCaDriverConnection* pFeliCaDriverConnection = (tFeliCaDriverConnection*)pCallbackParameter;
   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PDebugError(
         "static_PFeliCaDriverGetCardListCompleted: nError %s",
         PUtilTraceError(nError) );
      if ( nError == W_ERROR_CANCEL )
      {
         goto return_function;
      }
      nLength = 0;
   }
   else
   {
      PDebugTrace("static_PFeliCaDriverGetCardListCompleted: Response");

      /* Check if the buffer is to short */
      if ( nLength > pFeliCaDriverConnection->nCardToReaderBufferMaxLengthFeliCa )
      {
         nLength = 0;
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   /* Send the answer */
   PDFCDriverPostCC3(
      pFeliCaDriverConnection->pDriverCC,
      nLength,
      nError );

return_function:

   /* Release the reference after completion
      May destroy pFeliCaDriverConnection */
   PHandleDecrementReferenceCount(pContext, pFeliCaDriverConnection);
}


/** @see tPReaderDriverCreateConnection() */
static W_ERROR static_PFeliCaDriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   tFeliCaDriverConnection* pFeliCaDriverConnection;
   W_HANDLE hDriverConnection;
   W_ERROR nError;

   /* Check the parameters */
   if ( phDriverConnection == null )
   {
      PDebugError("PFeliCaCreateConnection: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the buffer */
   pFeliCaDriverConnection = (tFeliCaDriverConnection*)CMemoryAlloc( sizeof(tFeliCaDriverConnection) );
   if ( pFeliCaDriverConnection == null )
   {
      PDebugError("PFeliCaCreateConnection: pFeliCaDriverConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pFeliCaDriverConnection, 0, sizeof(tFeliCaDriverConnection));

   /* Register the connection structure */
   if ( ( nError = PHandleRegister(
                        pContext,
                        pFeliCaDriverConnection,
                        P_HANDLE_TYPE_FELICA_DRIVER_CONNECTION,
                        &hDriverConnection) ) != W_SUCCESS )
   {
      PDebugError("PFeliCaCreateConnection: error on PHandleRegister %s",
         PUtilTraceError(nError) );
      CMemoryFree(pFeliCaDriverConnection);
      return nError;
   }

   /* Store the connection information */
   pFeliCaDriverConnection->nServiceIdentifier = nServiceIdentifier;
   pFeliCaDriverConnection->nTimeout = P_FELICA_DEFAULT_TIMEOUT;
   *phDriverConnection = hDriverConnection;

   return W_SUCCESS;
}

/** @see tPReaderDriverSetDetectionConfiguration() */
static W_ERROR static_PFeliCaDriverSetDetectionConfiguration(
              tContext* pContext,
              uint8_t* pCommandBuffer,
              uint32_t* pnCommandBufferLength,
              const uint8_t* pDetectionConfigurationBuffer,
              uint32_t nDetectionConfigurationLength )
{
   const tPFeliCaDetectionConfiguration *pFeliCaDetectionConfiguration = (const tPFeliCaDetectionConfiguration*)pDetectionConfigurationBuffer;
   uint32_t nIndex = 0x00;
   static const uint8_t aCommandBuffer[] = P_FELICA_READER_CONFIG_PARAMETERS;

   if((pCommandBuffer == null)
   || (pnCommandBufferLength == null))
   {
      PDebugError("static_PFeliCaDriverSetDetectionConfiguration: bad Parameters");
      return W_ERROR_BAD_PARAMETER;
   }
   if((pDetectionConfigurationBuffer == null)
   || (nDetectionConfigurationLength == 0))
   {
      PDebugWarning("static_PFeliCaDriverSetDetectionConfiguration: set default parameters");
      CMemoryCopy(pCommandBuffer,
                  aCommandBuffer,
                  sizeof(aCommandBuffer));
      *pnCommandBufferLength = sizeof(aCommandBuffer);
      return W_SUCCESS;
   }
   else if(nDetectionConfigurationLength != sizeof(tPFeliCaDetectionConfiguration))
   {
      PDebugError("static_PFeliCaDriverSetDetectionConfiguration: bad nDetectionConfigurationLength (0x%x", nDetectionConfigurationLength);
      *pnCommandBufferLength = 0x00;
      return W_ERROR_BAD_PARAMETER;
   }
   CMemoryCopy(
         pCommandBuffer,
         pFeliCaDetectionConfiguration->aSystemCode,
         P_FELICA_SYSTEM_CODE_SIZE);
         nIndex += P_FELICA_SYSTEM_CODE_SIZE;
   /* Set command buffer length*/
   *pnCommandBufferLength = nIndex;
   return W_SUCCESS;
}
/** @see tPReaderDriverParseDetectionMessage() */
static W_ERROR static_PFeliCaDriverParseDetectionMessage(
               tContext* pContext,
               const uint8_t* pBuffer,
               uint32_t nLength,
               tPReaderDriverCardInfo* pCardInfo )
{
   PDebugTrace("static_PFeliCaDriverParseDetectionMessage()");

   if(pCardInfo->nProtocolBF != W_NFCC_PROTOCOL_READER_FELICA)
   {
      PDebugError("static_PFeliCaDriverParseDetectionMessage: protocol error");
      return W_ERROR_NFC_HAL_COMMUNICATION;
   }
   if((pBuffer == null)
   ||(nLength != NAL_READER_FELICA_DETECTION_MESSAGE_SIZE))
   {
      PDebugTrace("static_PFeliCaDriverParseDetectionMessage(): bad parameters");
      return W_ERROR_NFC_HAL_COMMUNICATION;
   }
   /* ID is UID */
   pCardInfo->nUIDLength = P_FELICA_MANUFACTURER_ID_LENGTH;
   CMemoryCopy(
           pCardInfo->aUID,
           pBuffer,
           pCardInfo->nUIDLength );
   /*reset AFI: not carried in the message*/
   pCardInfo->nAFI = 0x00;
   return W_SUCCESS;
 }

/** @see tPReaderDriverCheckCardMatchConfiguration() */
static bool_t static_PFeliCaDriverCheckCardMatchConfiguration(
               tContext* pContext,
               uint32_t nProtocolBF,
               const uint8_t* pDetectionConfigurationBuffer,
               uint32_t nDetectionConfigurationLength,
               tPReaderDriverCardInfo* pCardInfo)
{
   return W_TRUE;
}

/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sFeliCaReaderProtocolConstant = {
   W_NFCC_PROTOCOL_READER_FELICA,
   NAL_SERVICE_READER_FELICA,
   static_PFeliCaDriverCreateConnection,
   static_PFeliCaDriverSetDetectionConfiguration,
   static_PFeliCaDriverParseDetectionMessage,
   static_PFeliCaDriverCheckCardMatchConfiguration };

/* See header file */
void PFeliCaDriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength)
{
  tFeliCaDriverConnection* pFeliCaDriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );

   nError = PReaderDriverGetConnectionObject(
      pContext, hDriverConnection, P_HANDLE_TYPE_FELICA_DRIVER_CONNECTION,
      (void**)&pFeliCaDriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      pFeliCaDriverConnection->pDriverCC = pDriverCC;

      /* Check the parameters */
      if (  ( pReaderToCardBuffer == null )
         || ( nReaderToCardBufferLength == 0 )
         || ( nReaderToCardBufferLength > P_FELICA_BUFFER_MAX_SIZE - 2 )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) ) )
      {
         PDebugError("PFeliCaDriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      /* Store the connection information */
      pFeliCaDriverConnection->nCardToReaderBufferMaxLengthFeliCa = nCardToReaderBufferMaxLength;
      /* Store the callback buffer */
      pFeliCaDriverConnection->pCardToReaderBufferFeliCa = pCardToReaderBuffer;

      /* Prepare the command */
      pFeliCaDriverConnection->aReaderToCardBufferNAL[0] =  (NAL_TIMEOUT_READER_XCHG_DATA_ENABLE | pFeliCaDriverConnection->nTimeout);
      pFeliCaDriverConnection->aReaderToCardBufferNAL[1] = (uint8_t) (nReaderToCardBufferLength + 1);
      CMemoryCopy(
         &pFeliCaDriverConnection->aReaderToCardBufferNAL[2],
         pReaderToCardBuffer,
         nReaderToCardBufferLength );

      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_PFeliCaDriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(pFeliCaDriverConnection);

      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         pFeliCaDriverConnection->nServiceIdentifier,
         &pFeliCaDriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         pFeliCaDriverConnection->aReaderToCardBufferNAL,
         nReaderToCardBufferLength + 2,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength,
         static_PFeliCaDriverExecuteCommandCompleted,
         pFeliCaDriverConnection );
   }
   else
   {
      PDebugError("PFeliCaDriverExchangeData: could not get pFeliCaDriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }

   return;

error:

   /* Send the error */
   PDFCDriverPostCC3(
      pFeliCaDriverConnection->pDriverCC,
      0,
      nError);
}


/* See header file */
void PFeliCaDriverGetCardList(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength)
{
  tFeliCaDriverConnection* pFeliCaDriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );

   nError = PReaderDriverGetConnectionObject(
      pContext, hDriverConnection, P_HANDLE_TYPE_FELICA_DRIVER_CONNECTION,
      (void**)&pFeliCaDriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      pFeliCaDriverConnection->pDriverCC = pDriverCC;

      /* Check the parameters */
      if ( pCardToReaderBuffer == null
        || nCardToReaderBufferMaxLength == 0)
      {
         PDebugError("PFeliCaDriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      /* Store the connection information */
      pFeliCaDriverConnection->nCardToReaderBufferMaxLengthFeliCa = nCardToReaderBufferMaxLength;
      /* Store the callback buffer */
      pFeliCaDriverConnection->pCardToReaderBufferFeliCa = pCardToReaderBuffer;

      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_PFeliCaDriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(pFeliCaDriverConnection);


      PNALServiceGetParameter(
         pContext,
         pFeliCaDriverConnection->nServiceIdentifier,
         &(pFeliCaDriverConnection->sServiceOperation),
         NAL_PAR_LIST_CARDS,
         pFeliCaDriverConnection->pCardToReaderBufferFeliCa,
         pFeliCaDriverConnection->nCardToReaderBufferMaxLengthFeliCa,
         static_PFeliCaDriverGetCardListCompleted,
         pFeliCaDriverConnection);
   }
   else
   {
      PDebugError("PFeliCaDriverExchangeData: could not get pFeliCaDriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }

   return;

error:

   /* Send the error */
   PDFCDriverPostCC3(
      pFeliCaDriverConnection->pDriverCC,
      0,
      nError);
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
