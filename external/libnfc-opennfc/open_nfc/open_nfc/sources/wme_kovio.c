/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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
   Contains the kovio implementation
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( KOVIO )

#include "wme_context.h"

#define P_KOVIO_MAX_LENGTH      0x20

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Declare a Kovio driver exchange data structure */
typedef struct __tPKovioDriverConnection
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

} tPKovioDriverConnection;

/* Destroy connection callback */
static uint32_t static_PKovioDriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Connection registry Kovio type */
tHandleType g_sKovioDriverConnection = { static_PKovioDriverDestroyConnection,
                                          null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_KOVIO_DRIVER_CONNECTION (&g_sKovioDriverConnection)

static uint32_t static_PKovioDriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tPKovioDriverConnection* pKovioDriverConnection = (tPKovioDriverConnection*)pObject;

   PDebugTrace("static_PKovioDriverDestroyConnection");

   CMemoryFree( pKovioDriverConnection );

   return P_HANDLE_DESTROY_DONE;
}

/** @see tPReaderDriverCreateConnection() */
static W_ERROR static_PKovioDriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   tPKovioDriverConnection* pKovioDriverConnection;
   W_HANDLE hDriverConnection;
   W_ERROR nError;

   /* Check the parameters */
   if ( phDriverConnection == null )
   {
      PDebugError("static_PKovioDriverCreateConnection: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the Kovio buffer */
   pKovioDriverConnection = (tPKovioDriverConnection*)CMemoryAlloc( sizeof(tPKovioDriverConnection) );
   if ( pKovioDriverConnection == null )
   {
      PDebugError("static_PKovioDriverCreateConnection: pKovioDriverConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pKovioDriverConnection, 0, sizeof(tPKovioDriverConnection));

   /* Create the Kovio operation handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pKovioDriverConnection,
                     P_HANDLE_TYPE_KOVIO_DRIVER_CONNECTION,
                     &hDriverConnection ) ) != W_SUCCESS )
   {
      PDebugError("static_PKovioDriverCreateConnection: could not create Kovio connection handle");
      CMemoryFree(pKovioDriverConnection);
      return nError;
   }

   *phDriverConnection = hDriverConnection;

   return W_SUCCESS;
}

/** @see tPReaderDriverParseDetectionMessage() */
static W_ERROR static_PKovioDriverParseDetectionMessage(
               tContext* pContext,
               const uint8_t* pBuffer,
               uint32_t nLength,
               tPReaderDriverCardInfo* pCardInfo )
{
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_PKovioDriverParseDetectionMessage()");
   if(pCardInfo->nProtocolBF != W_NFCC_PROTOCOL_READER_KOVIO)
   {
      PDebugError("static_P14P4DriverParseDetectionMessage: protocol error");
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }

   CMemoryCopy(pCardInfo->aUID,
               pBuffer,
               nLength);

   pCardInfo->nUIDLength = (uint8_t) nLength;
   pCardInfo->nProtocolBF |= W_NFCC_PROTOCOL_READER_KOVIO;

return_function:

   if(nError == W_SUCCESS)
   {
      PDebugTrace("static_PKovioDriverParseDetectionMessage: UID = ");
      PDebugTraceBuffer(pCardInfo->aUID, pCardInfo->nUIDLength);

   }
   else
   {
      PDebugTrace("static_PKovioDriverParseDetectionMessage: error %s", PUtilTraceError(nError));
   }

   return nError;
}

/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sPKovioReaderProtocolConstant = {
   W_NFCC_PROTOCOL_READER_KOVIO,
   NAL_SERVICE_READER_KOVIO,
   static_PKovioDriverCreateConnection,
   null,
   static_PKovioDriverParseDetectionMessage,
   null };

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef struct __tPKovioUserConnection
{
   /* Connection registry handle */
   tHandleObjectHeader        sObjectHeader;

   uint8_t                    aData[P_KOVIO_MAX_LENGTH];

   uint32_t                   nLength;

} tPKovioUserConnection;


/* Destroy connection callback */
static uint32_t static_PKovioUserDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get property numbet callback */
static uint32_t static_PKovioUserGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get property numbet callback */
static bool_t static_PKovioUserGetProperties(
         tContext* pContext,
         void* pObject,
         uint8_t* pPropertyArray );

/* Check the property */
static bool_t static_PKovioUseCheckProperty(
         tContext* pContext,
         void* pObject,
         uint8_t nPropertyValue );

/* Get identifier length */
static uint32_t static_PKovioGetIdentifierLength(
         tContext* pContext,
         void* pObject);

/* Get identifier */
static void static_PKovioGetIdentifier(
         tContext* pContext,
         void* pObject,
         uint8_t* pIdentifierBuffer);

/* Connection registry Kovio type */
tHandleType g_sKovioUserConnection = {   static_PKovioUserDestroyConnection,
                                          null,
                                          static_PKovioUserGetPropertyNumber,
                                          static_PKovioUserGetProperties,
                                          static_PKovioUseCheckProperty,
                                          static_PKovioGetIdentifierLength,
                                          static_PKovioGetIdentifier,
                                          null, null };


#define P_HANDLE_TYPE_KOVIO_USER_CONNECTION (&g_sKovioUserConnection)

/**
 * @brief   Destroyes a Kovio connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PKovioUserDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tPKovioUserConnection* pKovioUserConnection = (tPKovioUserConnection*)pObject;

   PDebugTrace("static_PKovioUserDestroyConnection");

   /* The driver connection is closed by the reader registry parent object */
   /* pKovioUserConnection->hDriverConnection */

   /* Free the Kovio connection structure */
   CMemoryFree( pKovioUserConnection );

   return P_HANDLE_DESTROY_DONE;
}


/**
 * @brief   Gets the Kovio connection property number.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static uint32_t static_PKovioUserGetPropertyNumber(
            tContext* pContext,
            void * pObject)
{
   return 1;
}


static bool_t static_PKovioUserGetProperties(
         tContext* pContext,
         void* pObject,
         uint8_t* pPropertyArray )
{
   PDebugTrace("static_PKovioUserGetProperties");

   pPropertyArray[0] = W_PROP_KOVIO;
   return W_TRUE;
}

static bool_t static_PKovioUseCheckProperty(
         tContext* pContext,
         void* pObject,
         uint8_t nPropertyValue )
{
   PDebugTrace(
      "static_PKovioUseCheckProperty: nPropertyValue= (0x%02X)", nPropertyValue  );

   return ( nPropertyValue == W_PROP_KOVIO )?W_TRUE:W_FALSE;

}

/* See Header file */
void PKovioUserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tPKovioUserConnection* pKovioUserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the Kovio buffer */
   pKovioUserConnection = (tPKovioUserConnection*)CMemoryAlloc( sizeof(tPKovioUserConnection) );
   if ( pKovioUserConnection == null )
   {
      PDebugError("PKovioUserCreateConnection: pKovioUserConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_function;
   }
   CMemoryFill(pKovioUserConnection, 0, sizeof(tPKovioUserConnection));

   if(nLength > P_KOVIO_MAX_LENGTH)
   {
      PDebugWarning("static_PKovioUserParseCardInfo: data length truncated to %d bytes", P_KOVIO_MAX_LENGTH);
      nLength = P_KOVIO_MAX_LENGTH;
   }

   CMemoryCopy(pKovioUserConnection->aData,
               pBuffer,
               nLength);

   pKovioUserConnection->nLength = nLength;

   /* Add the Kovio structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     pKovioUserConnection,
                     P_HANDLE_TYPE_KOVIO_USER_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PKovioUserCreateConnection: could not add the Kovio Connection");
      /* Send the result */
      CMemoryFree(pKovioUserConnection);
      goto return_function;
   }

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError( "PKovioUserCreateConnection: returning %s",
         PUtilTraceError(nError) );
   }

   PDFCPostContext2(
         &sCallbackContext,
         nError );
}

static uint32_t static_PKovioGetIdentifierLength(
         tContext* pContext,
         void* pObject)
{
   tPKovioUserConnection* pPKovioUserConnection = (tPKovioUserConnection*)pObject;

   return pPKovioUserConnection->nLength;
}

/* Get identifier */
static void static_PKovioGetIdentifier(
         tContext* pContext,
         void* pObject,
         uint8_t* pIdentifierBuffer)
{
   tPKovioUserConnection* pPKovioUserConnection = (tPKovioUserConnection*)pObject;

   CMemoryCopy(pIdentifierBuffer, pPKovioUserConnection->aData, pPKovioUserConnection->nLength);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

