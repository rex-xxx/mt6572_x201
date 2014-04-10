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
   Contains the ISO14443-4 implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( 14_4 )

#include "wme_context.h"

#ifdef P_READER_14P4_STANDALONE_SUPPORT

#define P_14443_4_TYPEA_DETECTION_MESSAGE_SIZE_MIN          8        /* ATQA (2) + SAK (1) + ATS[1] + UID [4] */
#define P_14443_4_TYPEA_DETECTION_MESSAGE_SIZE_MAX          33       /* ATQA (2) + SAK (1) + ATS[20] + UID [10] */
#define P_14443_4_TYPEB_DETECTION_MESSAGE_SIZE_MIN          12       /* ATBB(11) + ATTRIB(1) */
#define P_14443_4_TYPEB_DETECTION_MESSAGE_SIZE_MAX          (P_14443_4_TYPEB_DETECTION_MESSAGE_SIZE_MIN + W_EMUL_HIGHER_LAYER_RESPONSE_MAX_LENGTH)   /* ATQB(11) + ATTRIB(254) */

#define P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN          P_14443_4_TYPEB_DETECTION_MESSAGE_SIZE_MIN       /* ATBB(11) + ATTRIB(1) */
#define P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MAX          P_14443_4_TYPEB_DETECTION_MESSAGE_SIZE_MAX   /* ATQB(11) + ATTRIB(254) */

#define P_14443_4_APDU_MAX_SIZE                             261      /* From MR documentation */

#define P_14443_4_POLLING_TIMEOUT                           8


/* default configuration parameters*/
#define P_14443_A_4_READER_CONFIG_PARAMETERS                 {0x00, 0x00, 0x00, 0x08}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Declare a 14443-4 driver exchange data structure */
typedef struct __tP14P4DriverConnection
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

   /* Connection information */
   uint32_t                   nTimeout;

   /* TCL response buffer */
   uint8_t*                   pCardToReaderBuffer;

   /* Buffer for the payload of a NFC HAL command (ISO 14443 frame (without CRC) +  NFC HAL exchange control byte + optional NAD */
   uint8_t                    aReaderToCardBufferNAL[P_14443_4_APDU_MAX_SIZE + 2];

   uint32_t                   nCardToReaderBufferMaxLength;

   /* Callback context */
   bool_t bFromUser;
   tDFCDriverCCReference      pDriverCC;
   tDFCCallbackContext        sCallbackContext;

   /* Service Context */
   uint8_t                    nServiceIdentifier;
   /* Service Operation */
   tNALServiceOperation       sServiceOperation;

   /* Operation handle */
   W_HANDLE                   hOperation;

   /* Handle on this object */
   W_HANDLE hDriverConnection;

} t14P4DriverConnection;



/* Destroy connection callback */
static uint32_t static_P14P4DriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Exchange data cancellation callback */
static void static_P14P4DriverExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing);

/* The NFC HAL completion callback */
static void static_P14P4DriverExecuteCommandCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter);

/* Connection registry 14443-3 type */
tHandleType g_s14P4DriverConnection = { static_P14P4DriverDestroyConnection,
                                          null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_14443_4_DRIVER_CONNECTION (&g_s14P4DriverConnection)

/* Send the exchange data answer */
static void static_P14P4DriverSendDataAnswer(
            tContext* pContext,
            t14P4DriverConnection* p14P4DriverConnection,
            uint32_t nlength,
            W_ERROR nError)
{
   if(p14P4DriverConnection->bFromUser)
   {
      /* Send the error */
      PDFCDriverPostCC3(
         p14P4DriverConnection->pDriverCC,
         nlength, nError );
   }
   else
   {
      PDFCPostContext3(
         &(p14P4DriverConnection->sCallbackContext),
         nlength, nError);
   }
}

/** @see tPReaderDriverCreateConnection() */

static W_ERROR static_P14P4DriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   t14P4DriverConnection* p14P4DriverConnection;
   W_HANDLE hDriverConnection;
   W_ERROR nError;

   PDebugTrace("static_P14P4DriverCreateConnection");

   /* Check the parameters */
   if ( phDriverConnection == null )
   {
      PDebugError("static_P14P4DriverCreateConnection: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the 14443-4 buffer */
   p14P4DriverConnection = (t14P4DriverConnection*)CMemoryAlloc( sizeof(t14P4DriverConnection) );
   if ( p14P4DriverConnection == null )
   {
      PDebugError("static_P14P4DriverCreateConnection: p14P4DriverConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(p14P4DriverConnection, 0, sizeof(t14P4DriverConnection));

   /* Create the 14443-4 operation handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     p14P4DriverConnection,
                     P_HANDLE_TYPE_14443_4_DRIVER_CONNECTION,
                     &hDriverConnection ) ) != W_SUCCESS )
   {
      PDebugError("static_P14P4DriverCreateConnection: could not create 14443-4 connection handle");
      CMemoryFree(p14P4DriverConnection);
      return nError;
   }

   /* Store the connection information */
   p14P4DriverConnection->nServiceIdentifier = nServiceIdentifier;
   p14P4DriverConnection->hDriverConnection = hDriverConnection;

   *phDriverConnection = hDriverConnection;

   return W_SUCCESS;
}

/**
 * @brief   Destroyes a 14443-4 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/

static uint32_t static_P14P4DriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   t14P4DriverConnection* p14P4DriverConnection = (t14P4DriverConnection*)pObject;

   PDebugTrace("static_P14P4DriverDestroyConnection");

   if(p14P4DriverConnection->bFromUser)
   {
      PDFCDriverFlushCall(p14P4DriverConnection->pDriverCC);
   }
   else
   {
      PDFCFlushCall(&p14P4DriverConnection->sCallbackContext);
   }

   /* Free the 14443-4 connection structure */
   CMemoryFree( p14P4DriverConnection );

   return P_HANDLE_DESTROY_DONE;
}


/** @see tPReaderDriverSetDetectionConfiguration() */

static W_ERROR static_P14P4DriverSetDetectionConfigurationTypeA(
                     tContext* pContext,
                     uint8_t* pCommandBuffer,
                     uint32_t* pnCommandBufferLength,
                     const uint8_t* pDetectionConfigurationBuffer,
                     uint32_t nDetectionConfigurationLength )
{
   const tP14P3DetectionConfigurationTypeA *p14P3DetectionConfigurationTypeA =
      (const tP14P3DetectionConfigurationTypeA*)pDetectionConfigurationBuffer;

   uint32_t nIndex = 0;
   static const uint8_t aCommandBuffer[] = P_14443_A_4_READER_CONFIG_PARAMETERS;

   PDebugTrace("static_P14P4DriverSetDetectionConfigurationTypeA");

   if((pCommandBuffer == null)
   || (pnCommandBufferLength == null))
   {
      PDebugError("static_P14P4DriverSetDetectionConfigurationTypeA: bad Parameters");
      return W_ERROR_BAD_PARAMETER;
   }
   if((pDetectionConfigurationBuffer == null)
   || (nDetectionConfigurationLength == 0))
   {
      PDebugWarning("static_P14P4DriverSetDetectionConfigurationTypeA: set default parameters");
      CMemoryCopy(pCommandBuffer,
                  aCommandBuffer,
                  sizeof(aCommandBuffer));
      *pnCommandBufferLength = sizeof(aCommandBuffer);
      return W_SUCCESS;
   }
   else if(nDetectionConfigurationLength != sizeof(tP14P3DetectionConfigurationTypeA))
   {
      PDebugError("static_P14P4DriverSetDetectionConfigurationTypeA: bad nDetectionConfigurationLength (0x%x", nDetectionConfigurationLength);
      *pnCommandBufferLength = 0x00;
      return W_ERROR_BAD_PARAMETER;
   }
   /* Set the Data Rate code*/
   switch(p14P3DetectionConfigurationTypeA->nBaudRate)
   {
      case 0:
      case 106:
         pCommandBuffer[nIndex++] = 0x00;
      break;
      case 212:
         pCommandBuffer[nIndex++] = 0x01;
      break;
      case 424:
         pCommandBuffer[nIndex++] = 0x02;
      break;
      case 847:
         pCommandBuffer[nIndex++] = 0x04;
      break;
      default:
         /*bad rate parameter: set the default value*/
         return W_ERROR_BAD_PARAMETER;
   }
   /* Set CID supported flag*/
   pCommandBuffer[nIndex++] = (uint8_t)p14P3DetectionConfigurationTypeA->bUseCID;
   /* Set the CID value*/
   pCommandBuffer[nIndex++] = p14P3DetectionConfigurationTypeA->nCID;
   /*set default FSD: this parameter is not provided by the caller*/
   pCommandBuffer[nIndex++] = P_14443_3_FSD_CODE_MAX_VALUE;

   *pnCommandBufferLength = nIndex;
   return W_SUCCESS;
}

/** @see tPReaderDriverSetDetectionConfiguration() */
static W_ERROR static_P14P4DriverSetDetectionConfigurationTypeB(
                     tContext* pContext,
                     uint8_t* pCommandBuffer,
                     uint32_t* pnCommandBufferLength,
                     const uint8_t* pDetectionConfigurationBuffer,
                     uint32_t nDetectionConfigurationLength )
{
   W_ERROR nError;

   PDebugTrace("static_P14P4DriverSetDetectionConfigurationTypeB");

   /* The 14443-4 part B uses the same parameters than the 14443-3 type B */

   nError =  P14P3DriverSetDetectionConfigurationTypeB(
                     pContext, pCommandBuffer, pnCommandBufferLength, pDetectionConfigurationBuffer, nDetectionConfigurationLength);

   return (nError);
}


/** @see tPReaderDriverParseDetectionMessage() */

static W_ERROR static_P14P4DriverParseDetectionMessage(
               tContext* pContext,
               const uint8_t* pBuffer,
               uint32_t nLength,
               tPReaderDriverCardInfo* pCardInfo )
{
   uint32_t nIndex = 0;
   uint32_t nATSLength;
   W_ERROR nError = W_SUCCESS;


   PDebugTrace("static_P14P4DriverParseDetectionMessage()");

   switch ( pCardInfo->nProtocolBF )
   {
      case W_NFCC_PROTOCOL_READER_ISO_14443_4_A:

         if((pBuffer == null)
          ||(nLength < P_14443_4_TYPEA_DETECTION_MESSAGE_SIZE_MIN)
          ||(nLength > P_14443_4_TYPEA_DETECTION_MESSAGE_SIZE_MAX))
          {
             PDebugTrace("static_P14P4DriverParseDetectionMessage(): bad parameters");
             nError = W_ERROR_NFC_HAL_COMMUNICATION;
             goto return_function;
          }

         /* Offset   (in bytes)   Length (in bytes)   Description
               0                        2                  The ATQA parameter defined in [HCI].
               2                        1                  The SAK parameter defined in [HCI].
               3   Given by the first byte TL in [1, 20]   The ATS frame specified in [ISO 14443-4]. Includes TL, T0, TA(1), TB(1), TC(1), and the historical bytes T1  T15.
               4-23         4,7 or 10                     The UID parameter defined in [HCI].
         */

          /* get UID size from ATQA, 2nd  byte */
          switch ( ((pBuffer[nIndex + 1] >> 6) & 0x03) )
          {
             case 0:
                pCardInfo->nUIDLength = 4;
                break;
             case 1:
                pCardInfo->nUIDLength = 7;
                break;
             case 2:
                pCardInfo->nUIDLength = 10;
                  break;
               default:
                  PDebugError("static_P14P4DriverParseDetectionMessage: wrong UID length");
                  nError = W_ERROR_NFC_HAL_COMMUNICATION;
                  goto return_function;
          }

          /* check if  card Protocol is compliant to 14443-4 from SAK */
          if ( (pBuffer[nIndex + 2] & 0x20) != 0 )
          {
             /* PICC compliant with ISO/IEC 14443-4 */
             PDebugTrace("static_P14P4DriverParseDetectionMessage: Type A - 14443-4 compliant");
             pCardInfo->nProtocolBF |= W_NFCC_PROTOCOL_READER_ISO_14443_4_A;
          }
          else
          {
             /* PICC not compliant with ISO/IEC 14443-4 */
             PDebugError("static_P14P4DriverParseDetectionMessage: Type A - not 14443-4 compliant !!!!");
             nError = W_ERROR_NFC_HAL_COMMUNICATION;
             goto return_function;
          }

          /* Get the ATS length */
          nATSLength  = pBuffer[nIndex + 3];

#if 0 /* @todo */
          /* copy the ATS */
          if (nATSLength > P_READER_MAX_ATS_LENGTH)
          {
            PDebugError("static_P14P4DriverParseDetectionMessage: ATS too long");
            nError = W_ERROR_NFC_HAL_COMMUNICATION;
            goto return_function;
          }

          CMemoryCopy(pCardInfo->aATS, &pBuffer[nIndex + 4], nATSLength );
#endif /* 0 */
          /* copy the UID*/
          if(pCardInfo->nUIDLength > P_READER_MAX_UID_LENGTH)
          {
             PDebugError("static_P14P4DriverParseDetectionMessage: UID too long");
             nError = W_ERROR_NFC_HAL_COMMUNICATION;
             goto return_function;
          }

          CMemoryCopy(pCardInfo->aUID, &pBuffer[nIndex + 3 + nATSLength], pCardInfo->nUIDLength );

          break;

       case W_NFCC_PROTOCOL_READER_ISO_14443_4_B:

          /* Same as NAL_EVT_READER_TARGET_DISCOVERED for ISO 14443 B Part 3 */

          /*pBuffer  carry byte 2 to 12 of ATQB and answer to ATTRIB*/
          if((pBuffer == null)
          ||(nLength < P_14443_4_TYPEB_DETECTION_MESSAGE_SIZE_MIN)
          ||(nLength > P_14443_4_TYPEB_DETECTION_MESSAGE_SIZE_MAX))
          {
             PDebugTrace("static_P14P4DriverParseDetectionMessage(): bad parameters");
             nError = W_ERROR_NFC_HAL_COMMUNICATION;
             goto return_function;
          }
          /* get the PUPI : bytes 2, 3, 4 and 5 of ATQB*/
          pCardInfo->nUIDLength = 4;
          CMemoryCopy( pCardInfo->aUID, &pBuffer[nIndex], 4 );
          /* get the AFI value: byte 6 of ATQB */
          pCardInfo->nAFI = pBuffer[nIndex + 4];
          /* check if  card Protocol is compliant to 14443-4: byte 11 of ATQB*/
          if ( (pBuffer[nIndex + 9] & 0x01) != 0 )
          {
             /* PICC compliant with ISO/IEC 14443-4 */
             PDebugTrace("static_P14P4DriverParseDetectionMessage: 14443-4 compliant");
             pCardInfo->nProtocolBF |= W_NFCC_PROTOCOL_READER_ISO_14443_4_B;
          }
          else
          {
             /* PICC not compliant with ISO/IEC 14443-4 */
             PDebugTrace("static_P14P4DriverParseDetectionMessage: only 14443-3 compliant");
          }
          break;
      default:
         PDebugError("static_P14P4DriverParseDetectionMessage: protocol error");
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
         goto return_function;
   }

return_function:

   if(nError == W_SUCCESS)
   {
      PDebugTrace("static_P14P4DriverParseDetectionMessage: UID = ");
      PDebugTraceBuffer(pCardInfo->aUID, pCardInfo->nUIDLength);

   }
   else
   {
      PDebugTrace("static_P14P4DriverParseDetectionMessage: error %s", PUtilTraceError(nError));
   }

   return nError;
}


/* See Header file */
W_ERROR P14P4DriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout )
{
   t14P4DriverConnection* p14P4DriverConnection;
   W_ERROR nError;

   PDebugTrace("P14P4DriverSetTimeout");

   nError = PReaderDriverGetConnectionObject( pContext, hConnection, P_HANDLE_TYPE_14443_4_DRIVER_CONNECTION, (void**)&p14P4DriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Check the timeout value */
      if ( nTimeout > 14 )
      {
         return W_ERROR_BAD_PARAMETER;
      }
      p14P4DriverConnection->nTimeout = nTimeout;
   }

   return nError;
}

/* See Header file */
W_HANDLE P14P4DriverExchangeDataInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            bool_t     bSendNAD,
            uint8_t  nNAD,
            bool_t     bCreateOperation,
            bool_t     bFromUser)
{
   t14P4DriverConnection* p14P4DriverConnection;
   tNFCControllerInfo * pNFCControllerInfo = PContextGetNFCControllerInfo(pContext);

   W_ERROR nError;
   uint8_t nProtocolControlByte;
   uint32_t nIndex;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDebugTrace("P14P4DriverExchangeData");

   nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_14443_4_DRIVER_CONNECTION,
      (void**)&p14P4DriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      p14P4DriverConnection->bFromUser = bFromUser;
      if( bFromUser )
      {
         PDFCDriverFillCallbackContext(
            pContext,
            (tDFCCallback*)pCallback, pCallbackParameter,
            &(p14P4DriverConnection->pDriverCC) );
      }
      else
      {
         PDFCFillCallbackContext(
            pContext,
            (tDFCCallback *) pCallback, pCallbackParameter,
            &(p14P4DriverConnection->sCallbackContext));
      }

      /* Check the parameters */
      if ( ((pReaderToCardBuffer == null) && (nReaderToCardBufferLength != 0))      ||
           ((pReaderToCardBuffer != null) && (nReaderToCardBufferLength == 0))      ||
           ( nReaderToCardBufferLength > P_14443_4_APDU_MAX_SIZE )                  ||
           ((pCardToReaderBuffer == null ) && (nCardToReaderBufferMaxLength != 0))  ||
           ((pCardToReaderBuffer != null ) && (nCardToReaderBufferMaxLength == 0)) )
      {
         PDebugError("P14P4DriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      if(bCreateOperation != W_FALSE)
      {
         /* create an operation */

         p14P4DriverConnection->hOperation = PBasicCreateOperation(pContext, static_P14P4DriverExchangeDataCancel, p14P4DriverConnection);

         if (p14P4DriverConnection->hOperation == W_NULL_HANDLE)
         {
            PDebugError("P14P4DriverExchangeData : PBasicCreateOperation failed");
            nError = W_ERROR_OUT_OF_RESOURCE;
            goto error;
         }

         nError = PHandleDuplicate(pContext, p14P4DriverConnection->hOperation, & hOperation);

         if (nError != W_SUCCESS)
         {
            PDebugError("P14P4DriverExchangeData : PHandleDuplicate failed %s", PUtilTraceError(nError));
            goto error;
         }
      }

      /* Store the connection information */
      p14P4DriverConnection->nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;
      /* Store the callback buffer */
      p14P4DriverConnection->pCardToReaderBuffer = pCardToReaderBuffer;

      /* Prepare the command */

      nIndex = 0;

      /*  Offset    Length    Description
            0   1   Protocol Control Byte
            1   1   Optional NAD byte
            1 or 2   N   The payload data.
            */

      nProtocolControlByte = (uint8_t)(p14P4DriverConnection->nTimeout) | NAL_TIMEOUT_READER_XCHG_DATA_ENABLE;

      /* Do not send the NAD if not supported by the NFC controller */

      if (p14P4DriverConnection->nServiceIdentifier == NAL_SERVICE_READER_14_A_4)
      {
         if ((pNFCControllerInfo->nFirmwareCapabilities & NAL_CAPA_READER_ISO_14443_A_NAD) == 0)
         {
            bSendNAD = W_FALSE;
         }
      }
      else if (p14P4DriverConnection->nServiceIdentifier == NAL_SERVICE_READER_14_B_4)
      {
         if ((pNFCControllerInfo->nFirmwareCapabilities & NAL_CAPA_READER_ISO_14443_B_NAD) == 0)
         {
            bSendNAD = W_FALSE;
         }
      }


      if(bSendNAD != W_FALSE)
      {
         /* Set the NAD present control byte */
         nProtocolControlByte |= NAL_ISO_14_A_4_NAD_ENABLE;
      }

      p14P4DriverConnection->aReaderToCardBufferNAL[nIndex++] = nProtocolControlByte;

      if(bSendNAD != W_FALSE)
      {
         /* Set the NAD present control byte */
         p14P4DriverConnection->aReaderToCardBufferNAL[nIndex++] = nNAD;
      }

      /* Set the payload data */
      if (pReaderToCardBuffer != null)
      {
         CMemoryCopy(&p14P4DriverConnection->aReaderToCardBufferNAL[nIndex],
                     pReaderToCardBuffer,
                     nReaderToCardBufferLength);
      }

      nIndex += nReaderToCardBufferLength;

      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_P14P3DriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(p14P4DriverConnection);

      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         p14P4DriverConnection->nServiceIdentifier,
         &p14P4DriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         p14P4DriverConnection->aReaderToCardBufferNAL,
         nIndex,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength,
         static_P14P4DriverExecuteCommandCompleted,
         p14P4DriverConnection );
   }
   else
   {
      PDebugError("P14P4DriverExchangeData: could not get p14P4DriverConnection buffer");

      /* Send the error */
      if(bFromUser)
      {
         tDFCDriverCCReference pDriverCC;
         PDFCDriverFillCallbackContext(
            pContext,
            (tDFCCallback*)pCallback, pCallbackParameter,
            &pDriverCC );
         PDFCDriverPostCC3(
            pDriverCC,
            0, nError );
      }
      else
      {
         tDFCCallbackContext   sCallbackContext;
         PDFCFillCallbackContext(
            pContext,
            (tDFCCallback *) pCallback, pCallbackParameter,
            &sCallbackContext);
         PDFCPostContext3(
            &sCallbackContext,
            0, nError);
      }
   }

   return (hOperation);

error:

   /* Send the error */

   if (p14P4DriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, p14P4DriverConnection->hOperation);
      PHandleClose(pContext, p14P4DriverConnection->hOperation);
      p14P4DriverConnection->hOperation = W_NULL_HANDLE;
   }

   if (hOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hOperation);
   }

   static_P14P4DriverSendDataAnswer(pContext, p14P4DriverConnection, 0, nError);

   return W_NULL_HANDLE;
}

/* See Header file */
W_HANDLE P14P4DriverExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            bool_t     bSendNAD,
            uint8_t  nNAD,
            bool_t bCreateOperation)
{
   /* See Header file */
   return P14P4DriverExchangeDataInternal(
               pContext,
               hConnection,
               pCallback,
               pCallbackParameter,
               pReaderToCardBuffer,
               nReaderToCardBufferLength,
               pCardToReaderBuffer,
               nCardToReaderBufferMaxLength,
               bSendNAD,
               nNAD,
               bCreateOperation,
               W_TRUE);
}

/**
 * @see  PNALServiceExecuteCommand
 **/

static void static_P14P4DriverExecuteCommandCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter)
{
   t14P4DriverConnection* p14P4DriverConnection = (t14P4DriverConnection*)pCallbackParameter;


   PDebugTrace("static_P14P4DriverExecuteCommandCompleted");

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PDebugError(
         "static_P14P4DriverExecuteCommandCompleted: nError %s",
         PUtilTraceError(nError) );
      if ( nError == W_ERROR_CANCEL )
      {
         goto return_function;
      }
      nLength = 0;
   }
   else
   {
      PDebugTrace("static_P14P4DriverExecuteCommandCompleted: Response");

      PReaderDriverSetAntiReplayReference(pContext);

      /* Check if the buffer is to short */
      if ( nLength > p14P4DriverConnection->nCardToReaderBufferMaxLength )
      {
         nLength = 0;
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   /* Set the current operation as completed */
   if (p14P4DriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, p14P4DriverConnection->hOperation);
      PHandleClose(pContext, p14P4DriverConnection->hOperation);
      p14P4DriverConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the answer */
   static_P14P4DriverSendDataAnswer(pContext, p14P4DriverConnection, nLength, nError);

return_function:

   /* Release the reference after completion
      May destroy p14P3DriverConnection */
   PHandleDecrementReferenceCount(pContext, p14P4DriverConnection);
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

static void static_P14P4DriverExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   t14P4DriverConnection* p14P4DriverConnection = pCancelParameter;

   PDebugTrace("P14P4DriverExchangeDataCancel");

   /* request the cancel of the NFC HAL operation */
   PNALServiceCancelOperation(pContext, & p14P4DriverConnection->sServiceOperation);
}



/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sP14P4ReaderProtocolConstantTypeA = {
   W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
   NAL_SERVICE_READER_14_A_4,
   static_P14P4DriverCreateConnection,
   static_P14P4DriverSetDetectionConfigurationTypeA,
   static_P14P4DriverParseDetectionMessage,
   null };

/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sP14P4ReaderProtocolConstantTypeB = {
   W_NFCC_PROTOCOL_READER_ISO_14443_4_B,
   NAL_SERVICE_READER_14_B_4,
   static_P14P4DriverCreateConnection,
   static_P14P4DriverSetDetectionConfigurationTypeB,
   static_P14P4DriverParseDetectionMessage,
   null };

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */



#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_14443_UID_MAX_LENGTH               0x0A
#define P_14443_PUPI_MAX_LENGTH              0x04
#define P_14443_B_ATQB_APPLICATION_DATA_LENGTH  0x04


   /*Conection Info structure Type A*/
typedef struct __tP14P3ConnectionInfoTypeA
{
   uint8_t                    nUIDLength;
   uint8_t                    aUID[P_14443_UID_MAX_LENGTH];
   uint16_t                   nATQA;
   uint8_t                    nSAK;
   uint8_t                    nBitFrameAnticollision;
   uint8_t                    nATQAPropCoding;
}tP14P3ConnectionInfoTypeA;

/*Conection Info structure Type B*/
typedef struct __tP14P3ConnectionInfoTypeB
{
   uint8_t                    aATQB[12];
   bool_t                       bIsCIDSupported;
   bool_t                       bIsNADSupported;
   uint8_t                    nAFI;
   uint8_t                    nCID;
   uint8_t                    nPUPILength;
   uint8_t                    aPUPI[P_14443_PUPI_MAX_LENGTH];
   uint8_t                    nApplicationDataLength;
   uint8_t                    aApplicationData[P_14443_B_ATQB_APPLICATION_DATA_LENGTH];
   uint32_t                   nBaudRate;
   uint32_t                   nCardInputBufferSize;
   uint32_t                   nReaderInputBufferSize;
   uint8_t                    aHigherLayerData[W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH];
   uint8_t                    nHigherLayerDataLength;
   uint8_t                    aHigherLayerResponse[W_EMUL_HIGHER_LAYER_RESPONSE_MAX_LENGTH];
   uint8_t                    nHigherLayerResponseLength;
   uint8_t                    aInfoBliCid[4];
}tP14P3ConnectionInfoTypeB;

typedef struct __tP14P4ConnectionInfoTypeA
{
   /* flags  */
   bool_t                       bIsPPSSupported;
   bool_t                       bIsCIDSupported;
   bool_t                       bIsNADSupported;
   /* Card ID */
   uint8_t                    nCID;
   /*  Node Address */
   uint8_t                    nNAD;
   /*historical bytes*/
   uint8_t                    aApplicationData[W_EMUL_APPLICATION_DATA_MAX_LENGTH];
   uint8_t                    nApplicationDataLength;
    /* card buffer capabilities */
   uint32_t                   nCardInputBufferSize;
   /* Reader buffer capabilities */
   uint32_t                   nReaderInputBufferSize;
   /* time of latency */
   uint8_t                    nFWI_SFGI;
   /* bit rate connection*/
   uint8_t                    nDataRateMaxDiv;
   uint32_t                   nBaudRate;
}tP14P4ConnectionInfoTypeA;


typedef struct __tP14P4ConnectionInfoTypeB
{
   /* Node Address */
   uint8_t nNAD;
}tP14P4ConnectionInfoTypeB;


/* Declare a 14443-4 exchange data structure */
typedef struct __tP14P4UserConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;

   /* Driver connection handle */
   W_HANDLE                   hDriverConnection;

   /* Operation handle */
   W_HANDLE                   hCurrentOperation;
   W_HANDLE                   hCurrentDriverOperation;

   /* Connection information */
   uint8_t                    nProtocol;

   union
   {
      tP14P3ConnectionInfoTypeA sTypeA;
      tP14P3ConnectionInfoTypeB sTypeB;
   }sPart3ConnectionInfo;

   union
   {
      tP14P4ConnectionInfoTypeA sTypeA;
      tP14P4ConnectionInfoTypeB sTypeB;
   }tPart4ConnectionInfo;

#define s14typeA4 tPart4ConnectionInfo.sTypeA
#define s14typeB4 tPart4ConnectionInfo.sTypeB

   /*timeout*/
   uint32_t                   nTimeout;

   uint8_t*                   pCardToReaderBuffer;
   uint8_t                    aPollingBuffer[4];

   /* Part4 callback context */
   tDFCCallbackContext        sCallbackContext;

} tP14P4UserConnection;

/* Destroy connection callback */
static uint32_t static_P14P4UserDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static uint32_t static_P14P4UserGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get properties connection callback */
static bool_t static_P14P4UserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_P14P4UserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Get identifier length */
static uint32_t static_P14P4UserGetIdentifierLength(
            tContext* pContext,
            void* pObject);

/* Get identifier */
static void static_P14P4UserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer);

static void static_P14P4UserExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation );

/* Send polling command */
static void static_P14P4Poll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/* Handle registry 14443-4 type */
tHandleType g_s14P4Connection = { static_P14P4UserDestroyConnection,
                                    null,
                                    static_P14P4UserGetPropertyNumber,
                                    static_P14P4UserGetProperties,
                                    static_P14P4UserCheckProperties,
                                    static_P14P4UserGetIdentifierLength,
                                    static_P14P4UserGetIdentifier,
                                    static_P14P4UserExchangeData,
                                    static_P14P4Poll };


#define P_HANDLE_TYPE_14443_4_USER_CONNECTION (&g_s14P4Connection)

/**
 * @brief   Parses the Type A card response to a RATS command (ATS frame) .
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p14P4Connection  The 14443-4  connection structure.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P14P4UserParseATS(
            tContext* pContext,
            tP14P4UserConnection* p14P4Connection,
            W_HANDLE hConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   uint8_t nOffset = 0;

   PDebugTrace("static_P14P4UserParseATS");

   /* Check the length */
   if (pBuffer[0] > 1 )
   {
      /* Increase the offset: TO is present */
      nOffset++;

      /* Check the format byte coherence */
      if ( pBuffer[1] & 0x80 )
      {
         return W_ERROR_RF_COMMUNICATION;
      }

      /* Maximum PICC buffer */
      switch ( pBuffer[1] & 0x0F )
      {
         case 0:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 16;
            break;
         case 1:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 24;
            break;
         case 2:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 32;
            break;
         case 3:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 40;
            break;
         case 4:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 48;
            break;
         case 5:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 64;
            break;
         case 6:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 96;
            break;
         case 7:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 128;
            break;
         case 8:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 256;
            break;
         default:
            PDebugWarning(
               "static_P14P4ParseATS: wrong card input buffer size value %d",
               pBuffer[1] & 0x0F );
            /* A received value of FSDI = '9'-'F' should be interpreted as FSDI = '8' (FSD = 256 bytes). */
            p14P4Connection->s14typeA4.nCardInputBufferSize = 256;
            break;
      }
      PDebugTrace(
         "static_P14P4ParseATS: max card input buffer %d bytes",
         p14P4Connection->s14typeA4.nCardInputBufferSize );

      /* Check the format byte TA value */
      if ( pBuffer[1] & 0x10 )
      {
         nOffset++;

         if ( pBuffer[nOffset] & 0x08 )
         {
            return W_ERROR_RF_COMMUNICATION;
         }

         /* Different divisors for each direction */
         if ( pBuffer[nOffset] & 0x80 )
         {
            /* The PPS mode is not supported */
            p14P4Connection->s14typeA4.bIsPPSSupported = W_FALSE;
         }
         else
         {
            /* We need top manage the PPS mode */
            p14P4Connection->s14typeA4.bIsPPSSupported = W_TRUE;
         }

         /* Baud rate from PCD to PICC */
         if ( pBuffer[nOffset] & 0x01 )
         {
            p14P4Connection->s14typeA4.nBaudRate = 212;
         }
         else
         {
            if ( pBuffer[nOffset] & 0x02 )
            {
               p14P4Connection->s14typeA4.nBaudRate = 424;
            }
            else
            {
               if ( pBuffer[nOffset] & 0x04 )
               {
                  p14P4Connection->s14typeA4.nBaudRate = 847;
               }
               else
               {
                  p14P4Connection->s14typeA4.nBaudRate = 106;
               }
            }
         }
      }
      else
      {
         p14P4Connection->s14typeA4.nBaudRate = 106;
         p14P4Connection->s14typeA4.bIsPPSSupported = W_FALSE;
      }
      PDebugTrace(
         "static_P14P4ParseATS: baud rate %d kbit/s",
         p14P4Connection->s14typeA4.nBaudRate );

      if ( p14P4Connection->s14typeA4.bIsPPSSupported != W_FALSE )
      {
         PDebugTrace( "static_P14P4ParseATS: PPS mode supported" );
      }
      else
      {
         PDebugTrace( "static_P14P4ParseATS: PPS mode not supported" );
      }

      /* Check the format byte TB value */
      if ( pBuffer[1] & 0x60 )
      {
         /* Increase the offset */
         nOffset ++;

         p14P4Connection->tPart4ConnectionInfo.sTypeA.nFWI_SFGI = pBuffer[nOffset];

         /* Timeout */
         p14P4Connection->nTimeout = ( ( pBuffer[nOffset] >> 4 ) & 0x0F );
         if ( p14P4Connection->nTimeout > 14 )
         {
            PDebugWarning(
               "static_P14P4ParseATS: wrong timeout value %d",
               p14P4Connection->nTimeout );
            p14P4Connection->nTimeout = 14;
         }
         else
         {
            /* This limitation is removed */
            /*
            if ( p14P4Connection->nTimeout < 9 )
            {
               p14P4Connection->nTimeout = 9;
            }
            */
         }

         /* SFGI */
         /* nSFGI = p14P4Connection->aCardToReaderSpecialBuffer[nOffset] & 0x0F;*/
      }
      else
      {
         p14P4Connection->nTimeout = 14;
      }
      PDebugTrace(
         "static_P14P4ParseATS: timeout %d",
         p14P4Connection->nTimeout );


      /* Check the format byte TC value */
      if ( pBuffer[1] & 0x40 )
      {
         /* Increase the offset */
         nOffset ++;

         if ( pBuffer[nOffset] & 0xFC )
         {
            return W_ERROR_RF_COMMUNICATION;
         }

         /* CID */
         p14P4Connection->s14typeA4.bIsCIDSupported = ( ( pBuffer[nOffset] & 0x02 ) == 0x02 ) ? W_TRUE: W_FALSE;
         /* Set the default CID to 0 */
         p14P4Connection->s14typeA4.nCID = 0x00;
         /* NAD */
         p14P4Connection->s14typeA4.bIsNADSupported = ( ( pBuffer[nOffset] & 0x01 ) == 0x01 ) ? W_TRUE: W_FALSE;
      }
      else
      {
         p14P4Connection->s14typeA4.bIsCIDSupported = W_FALSE;
         p14P4Connection->s14typeA4.nCID = 0x00;
         p14P4Connection->s14typeA4.bIsNADSupported = W_FALSE;
      }
      if ( p14P4Connection->s14typeA4.bIsCIDSupported != W_FALSE )
      {
         PDebugTrace( "static_P14P4ParseATS: CID supported" );
      }
      else
      {
         PDebugTrace( "static_P14P4ParseATS: CID not supported" );
      }
      if ( p14P4Connection->s14typeA4.bIsNADSupported != W_FALSE )
      {
         PDebugTrace( "static_P14P4ParseATS: NAD supported" );
      }
      else
      {
         PDebugTrace( "static_P14P4ParseATS: NAD not supported" );
      }
      /*store application data*/
      p14P4Connection->s14typeA4.nApplicationDataLength = pBuffer[0] - nOffset - 1;
      if((p14P4Connection->s14typeA4.nApplicationDataLength > 0)
       &&(p14P4Connection->s14typeA4.nApplicationDataLength <= W_EMUL_APPLICATION_DATA_MAX_LENGTH))
      {
         CMemoryCopy(
            p14P4Connection->s14typeA4.aApplicationData,
            &pBuffer[nOffset+1],
            p14P4Connection->s14typeA4.nApplicationDataLength );
      }
   }

   return W_SUCCESS;
}

/**
 * @brief   Parses the Type A card answer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p14P4UserConnection  The 14443-4 user connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/

static W_ERROR static_P14P4UserParseTypeA(
            tContext* pContext,
            tP14P4UserConnection* p14P4UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   uint8_t     nATSLength;

   PDebugTrace("static_P14P4UserParseTypeA");

    if((pBuffer == null)
    ||(nLength < P_14443_4_TYPEA_DETECTION_MESSAGE_SIZE_MIN)
    ||(nLength > P_14443_4_TYPEA_DETECTION_MESSAGE_SIZE_MAX))
    {
       PDebugTrace("static_P14P4UserParseTypeA(): bad parameters");
       return W_ERROR_RF_COMMUNICATION;
    }

    /* Offset   (in bytes)   Length (in bytes)   Description
      0                        2                  The ATQA parameter defined in [HCI].
      2                        1                  The SAK parameter defined in [HCI].
      3   Given by the first byte TL in [1, 20]   The ATS frame specified in [ISO 14443-4]. Includes TL, T0, TA(1), TB(1), TC(1), and the historical bytes T1  T15.
      4-23         4,7 or 10                     The UID parameter defined in [HCI].
      */

    /* Proprietary coding */
    p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nATQAPropCoding = pBuffer[0] & 0x0F;

    /* get UID size */
    switch ( ((pBuffer[1] >> 6) & 0x03) )
    {
       case 0:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength = 4;
          break;
       case 1:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength = 7;
          break;
       case 2:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength = 10;
            break;
         default:
            PDebugError("static_P14P3UserParseTypeA: wrong UID length");
            return W_ERROR_RF_COMMUNICATION;
    }

    PDebugTrace("static_P14P4UserParseTypeA: UID length %d",
             p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );


    /* Get ATS length */
   nATSLength = pBuffer[3];

    /* copy the UID*/
    CMemoryCopy(
           p14P4UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
          &pBuffer[3 + nATSLength],
           p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
           PDebugTrace("static_P14P4UserParseTypeA: UID");
           PDebugTraceBuffer(p14P4UserConnection->sPart3ConnectionInfo.sTypeA.aUID, p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength);

     /* Bit frame */
    p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nBitFrameAnticollision = pBuffer[1] & 0x1F;

    PDebugTrace( "static_P14P4UserParseTypeA: nBitFrameCollision %d",
                     p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nBitFrameAnticollision );

    /* store the ATQA code*/
    p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nATQA = PUtilReadUint16FromBigEndianBuffer(&pBuffer[0]);

    /* evaluate the SAK value*/
    p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nSAK = pBuffer[2];

    /* Set Default Timeout: Timeout will be update if card is level 4*/
   p14P4UserConnection->nTimeout = P_14443_3_A_DEFAULT_TIMEOUT;

      /* Parse the ATS */
   static_P14P4UserParseATS(pContext, p14P4UserConnection, p14P4UserConnection->hDriverConnection, & pBuffer[3], pBuffer[3]);

   if (P14P4DriverSetTimeout(
      pContext,
      p14P4UserConnection->hDriverConnection,
      p14P4UserConnection->nTimeout ) != W_SUCCESS)
   {
      PDebugError("static_P14P4UserParseTypeA : P14P4DriverSetTimeout failed");
   }

   p14P4UserConnection->s14typeA4.nReaderInputBufferSize = P_14443_3_BUFFER_MAX_SIZE;

   return W_SUCCESS;
}

/* See Header file */
W_ERROR P14P4UserCheckMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength,
            uint8_t* pnType )
{
   tP14P4UserConnection* p14P4UserConnection;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_4_USER_CONNECTION, (void**)&p14P4UserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("P14P4UserCheckMifare: could not get p14P4UserConnection buffer");
      return nError;
   }

   /* check the protocol*/
   if(p14P4UserConnection->nProtocol != W_PROP_ISO_14443_4_A)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   if ((p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength == 7) &&
        (p14P4UserConnection->sPart3ConnectionInfo.sTypeA.aUID[0] != 0x04))
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* use the application data if present to try to distinguish the different cards */
   if (p14P4UserConnection->tPart4ConnectionInfo.sTypeA.nApplicationDataLength == 7)
   {
      uint8_t nTagByte     = p14P4UserConnection->tPart4ConnectionInfo.sTypeA.aApplicationData[0];
      uint8_t nLength      = p14P4UserConnection->tPart4ConnectionInfo.sTypeA.aApplicationData[1];
      uint8_t nChipType    = p14P4UserConnection->tPart4ConnectionInfo.sTypeA.aApplicationData[2];
      uint8_t nChipVersion = p14P4UserConnection->tPart4ConnectionInfo.sTypeA.aApplicationData[3];

      if ((nTagByte == 0xC1) && (nLength == 5))
      {
         switch (nChipType & 0xF0)
         {
            case 0x00 :
               /* (multiple) virtual cards ? */
               return W_ERROR_CONNECTION_COMPATIBILITY;

            case 0x10 :

               /* DESFire series */
               switch (nChipType & 0x0F)
               {
                  case 0x0F  : /* @todo : unspecified size. default to minimum size */
                  case 0x02  :
                     * pnType  = W_PROP_MIFARE_DESFIRE_EV1_2K;
                     break;

                  case 0x03 :
                     * pnType  = W_PROP_MIFARE_DESFIRE_EV1_4K;
                     break;

                  case 0x04 :
                     * pnType  = W_PROP_MIFARE_DESFIRE_EV1_8K;
                     break;

                  default :
                     /* RFU */
                     return W_ERROR_CONNECTION_COMPATIBILITY;
               }

               break;

            case 0x20 :

               /* Mifare plus series */
               switch (nChipType & 0x0F)
               {
                  case 0x0F :    /* @todo : unspecified size. default to minimum size */
                  case 0x02 :
                     * pnType  = (nChipVersion & 0x01) ? W_PROP_MIFARE_PLUS_X_2K : W_PROP_MIFARE_PLUS_S_2K;
                     break;

                  case 0x04 :
                     * pnType  = (nChipVersion & 0x01) ? W_PROP_MIFARE_PLUS_X_4K : W_PROP_MIFARE_PLUS_S_4K;
                     break;

                  default :
                     /* RFU */
                     return W_ERROR_CONNECTION_COMPATIBILITY;

               }

               break;

            default :

               /* RFU */
               return W_ERROR_CONNECTION_COMPATIBILITY;
         }
      }
   }
   else
   {

      /* no application data present, use the SAK */

      if (p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nATQA == 0x0344)
      {
         /* Desfire series;
            @todo : how to differentiate them ???? default to legacy W_PROP_MIFARE_DESFIRE_D40 */

         * pnType = W_PROP_MIFARE_DESFIRE_D40;
         goto return_success;
      }
   }


return_success:
   *pnUIDLength = p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;
   CMemoryCopy(
      pUID,
      p14P4UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
      p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );

   return W_SUCCESS;
}

/**
 * @brief   Parses the Type B card answer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p14P4UserConnection  The 14443-4 user connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P14P4UserParseTypeB(
            tContext* pContext,
            tP14P4UserConnection* p14P4UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   uint32_t nIndex = 0;

   PDebugTrace("static_P14P4UserParseTypeB");

   /*pBuffer  carry byte 2 to 12 of ATQB and answer to ATTRIB*/
   if((pBuffer == null)
      ||(nLength < P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN)
      ||(nLength > P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MAX))
   {
      PDebugTrace("static_P14P4UserParseTypeB(): bad parameters");
      return W_ERROR_NFC_HAL_COMMUNICATION;
   }

   /* get the PUPI : bytes 2, 3, 4 and 5 of ATQB*/
   p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength = 4;
   CMemoryCopy(
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aPUPI,
      &pBuffer[nIndex],
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength );
   PDebugTrace("static_P14P4UserParseTypeB: PUPI");
   PDebugTraceBuffer( p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aPUPI, p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength );

   /* Application data: AFI */
   p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nAFI = pBuffer[nIndex + 4];
   PDebugTrace(
      "static_P14P4UserParseTypeB: AFI 0x%02X",
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nAFI );
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nApplicationDataLength = P_14443_B_ATQB_APPLICATION_DATA_LENGTH;
   CMemoryCopy(
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aApplicationData,
      &pBuffer[nIndex + 4],
      P_14443_B_ATQB_APPLICATION_DATA_LENGTH );

   /*store INFO_BLI_CID : 3 protocol bytes of ATQB + MBLI-CID byte*/
   CMemoryCopy(
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aInfoBliCid,
      &pBuffer[nIndex + 8],
      0x03 );
   p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aInfoBliCid[3] = pBuffer[nIndex + 11];


    /* Baud rate */
    if ( pBuffer[nIndex + 8] & 0x08 )
    {
      /* Addendum 3 :
         A PICC setting b4 = 1 is not compliant with this standard.
         Until the RFU values with b4 = 1 are assigned by ISO, a PCD receiving Bit_Rate_capability with b4 = 1 should
         interpret the Bit_Rate_capability byte as if b8 to b1 = 0 (only ~106 kbit/s in both directions). */

       PDebugWarning(
          "static_P14P4UserParseTypeB: wrong baud rate value %d",
          ( pBuffer[nIndex + 2] & 0x08 ) );

       p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 212;
    }
    else
    {
       if ( pBuffer[nIndex + 8] & 0x01 )
       {
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 212;
       }
       else
       {
          if ( pBuffer[nIndex + 8] & 0x02 )
          {
             p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 424;
          }
          else
          {
             if ( pBuffer[nIndex + 8] & 0x04 )
             {
                p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 847;
             }
             else
             {
                p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 106;
             }
          }
       }
    }
    PDebugTrace(
      "static_P14P4UserParseTypeB: baud rate %d kbit/s",
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate );

    /* Maximum input buffer size*/
    switch ( ( pBuffer[nIndex + 9] >> 4 ) & 0x0F )
    {
       case 0:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 16;
          break;
       case 1:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 24;
          break;
       case 2:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 32;
          break;
       case 3:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 40;
          break;
       case 4:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 16;
          break;
       case 5:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 64;
          break;
       case 6:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 96;
          break;
       case 7:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 128;
          break;
       case 8:
          p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 256;
          break;

       default:
          /* Addendum 3 :
             A PICC setting Maximum Frame Size Code = '9'-'F' is not compliant with this standard.
             Until the RFU values '9'-'F' are assigned by ISO, a PCD receiving Maximum_Frame_Size Code = '9'-'F' should
             interpret it as Maximum Frame Size Code = '8' (256 bytes).
             */
         PDebugWarning(
            "static_P14P4UserParseTypeB: wrong maximum buffer size value %d",
            ( pBuffer[nIndex + 3] >> 4 ) & 0x0F );
         p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 256;
         break;
   }
   p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nReaderInputBufferSize = P_14443_3_BUFFER_MAX_SIZE;

   PDebugTrace(
      "static_P14P4UserParseTypeB: max input reader/card buffer %d bytes",
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize );

   /* Protocol type */
   switch (pBuffer[nIndex + 9] & 0x0F)
   {
      case 0x00 :
         /* PICC not compliant with ISO/IEC 14443-4 */
         PDebugError("static_P14P4UserParseTypeB: not 14443-4 compliant");
         break;

      case 0x01 :

         /* PICC compliant with ISO/IEC 14443-4 */
         PDebugTrace("static_P14P4UserParseTypeB: 14443-4 compliant");
         break;

      default:

         /* Addendum 3,
            The PCD should not continue communicating with a PICC that sets b4 to (1)b.
            -> but will not allow proper communication with PicoPass 32KS and 2KS ? */

         /* Invalid value, consider the  PICC non compliant with ISO/IEC 14443-4 */
         PDebugError("static_P14P4UserParseTypeB: invalid protocol type");
         break;
   }

   /* CID */
   p14P4UserConnection->sPart3ConnectionInfo.sTypeB.bIsCIDSupported = ( ( pBuffer[nIndex + 10] & 0x01 ) == 0x01 ) ? W_TRUE: W_FALSE;
   if ( p14P4UserConnection->sPart3ConnectionInfo.sTypeB.bIsCIDSupported != W_FALSE )
   {
      PDebugTrace("static_P14P4UserParseTypeB: CID supported");
      /* Set a CID (from 0 to 14) */
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCID = (pBuffer[nIndex + 11] & 0x0F);
   }
   else
   {
      PDebugTrace("static_P14P4UserParseTypeB: CID not supported");
      /* Set the default CID to 0 */
      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCID = 0x00;
   }

   /* NAD */
   p14P4UserConnection->sPart3ConnectionInfo.sTypeB.bIsNADSupported = ( ( ( pBuffer[nIndex + 10] >> 1 ) & 0x01 ) == 0x01 ) ? W_TRUE: W_FALSE;
   if ( p14P4UserConnection->sPart3ConnectionInfo.sTypeB.bIsNADSupported != W_FALSE )
   {
      PDebugTrace("static_P14P4UserParseTypeB: NAD supported");
   }
   else
   {
      PDebugTrace("static_P14P4UserParseTypeB: NAD not supported");
   }

   /* Timeout */
   p14P4UserConnection->nTimeout = ( ( pBuffer[nIndex + 10] >> 4 ) & 0x0F );


   if ( p14P4UserConnection->nTimeout > 14 )
   {
      PDebugWarning(
       "static_P14P4UserParseTypeB: wrong timeout value %d",
       p14P4UserConnection->nTimeout );

       /* Addendum 3
          A PICC setting FWI = 15 is not compliant with this standard.
          Until the RFU value 15 is assigned by ISO, a PCD receiving FWI = 15 should interpret it as FWI = 4 */
      p14P4UserConnection->nTimeout = 4;
   }
   else
   {
      PDebugTrace(
        "static_P14P4UserParseTypeB: timeout %d",
        p14P4UserConnection->nTimeout );
   }

   /* Set the timeout */
   if (P14P4DriverSetTimeout(
           pContext,
           p14P4UserConnection->hDriverConnection,
           p14P4UserConnection->nTimeout ) != W_SUCCESS)
   {
      PDebugError("static_P14P4UserParseTypeB : P14P4DriverSetTimeout failed");
   }

   /**
   * Build the ATQB Frame without CRC
   **/

   /* set header byte*/
   p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aATQB[0] = 0x50;
   /* copy others bytes without crc */
   CMemoryCopy(
          &p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aATQB[1],
          pBuffer,
          11);
   /* now skip ATQB */
   nIndex += 11;
   if(nLength > P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN)
   {
      /* copy the higher layer response */
      CMemoryCopy(
            p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aHigherLayerResponse,
            &pBuffer[nIndex + 1],
            (nLength - P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN));

      p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerResponseLength = (uint8_t)(nLength - P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN);
   }

   return (W_SUCCESS);
}


/**
 * @brief   Parses the Type A/B card response at start up.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p14P3UserConnection  The 14443-3 user connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P14P4UserParseCardInfo(
            tContext* pContext,
            tP14P4UserConnection* p14P4UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   PDebugTrace("static_P14P4UserParseCardInfo");

   switch (p14P4UserConnection->nProtocol)
   {
      case W_PROP_ISO_14443_4_A:
         /* Get the Type A information */
         return static_P14P4UserParseTypeA(
                  pContext,
                  p14P4UserConnection,
                  pBuffer,
                  nLength );

      case W_PROP_ISO_14443_4_B:
         /* Get the Type B information */
         return static_P14P4UserParseTypeB(
                  pContext,
                  p14P4UserConnection,
                  pBuffer,
                  nLength );
      default:
         PDebugWarning("static_P14P4UserParseCardInfo: unknown protocol 0x%02X", p14P4UserConnection->nProtocol );
      break;
   }
   return W_ERROR_BAD_PARAMETER;
}

/* See Header file */
static W_ERROR static_P14P4UserCreateConnectionSimple(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tP14P4UserConnection* p14P4UserConnection;
   W_ERROR nError;

   PDebugTrace("P14P4UserCreateConnection");

   /* Create the 14443-4 buffer */
   p14P4UserConnection = (tP14P4UserConnection*)CMemoryAlloc( sizeof(tP14P4UserConnection) );
   if ( p14P4UserConnection == null )
   {
      PDebugError("P14P4UserCreateConnection: p14P3UserConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_function;
   }
   CMemoryFill(p14P4UserConnection, 0, sizeof(tP14P4UserConnection));

   /* Store the connection information */
   p14P4UserConnection->hDriverConnection  = hDriverConnection;
   p14P4UserConnection->nProtocol          = nProtocol;

   /* Parse the information */
   if ( ( nError = static_P14P4UserParseCardInfo(
                     pContext,
                     p14P4UserConnection,
                     pBuffer,
                     nLength ) ) != W_SUCCESS )
   {
      PDebugError( "P14P4UserCreateConnection: error while parsing the card information");
      CMemoryFree(p14P4UserConnection);
      goto return_function;
   }
   /* Add the 14443-4 structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     p14P4UserConnection,
                     P_HANDLE_TYPE_14443_4_USER_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("P14P4UserCreateConnection: could not add the 14443-4 buffer");
      /* Send the result */
      CMemoryFree(p14P4UserConnection);
      goto return_function;
   }

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError( "P14P4UserCreateConnection: returning %s",
         PUtilTraceError(nError) );
   }

   return nError;
}

/* See Header file */
void P14P4UserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tDFCCallbackContext sCallbackContext;

   W_ERROR nError = static_P14P4UserCreateConnectionSimple(
            pContext,
            hUserConnection,
            hDriverConnection,
            nProtocol,
            pBuffer,
            nLength );

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   PDFCPostContext2(
         &sCallbackContext,
         nError );
}

/**
 * @brief   Destroyes a 14443-4 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/

static uint32_t static_P14P4UserDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pObject;

   PDebugTrace("static_P14P4UserDestroyConnection");

   PDFCFlushCall(&p14P4UserConnection->sCallbackContext);

   /* Free the 14443-4 connection structure */
   CMemoryFree( p14P4UserConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the 14443-4 connection property number.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/

static uint32_t static_P14P4UserGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/**
 * @brief   Gets the 14443-4 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/

static bool_t static_P14P4UserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pObject;

   PDebugTrace("static_P14P4UserGetProperties");

   if ( p14P4UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_P14P4UserGetProperties: no property");
      return W_FALSE;
   }

   pPropertyArray[0] = p14P4UserConnection->nProtocol;
   return W_TRUE;
}

/**
 * @brief   Checkes the 14443-4 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_P14P4UserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pObject;

   PDebugTrace(
      "static_P14P4UserCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( p14P4UserConnection->nProtocol != P_PROTOCOL_STILL_UNKNOWN )
   {
      if ( nPropertyValue == p14P4UserConnection->nProtocol )
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
      PDebugError("static_P14P4UserCheckProperties: no property");
      return W_FALSE;
   }
}

/* Get identifier length */
static uint32_t static_P14P4UserGetIdentifierLength(
            tContext* pContext,
            void* pObject)
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pObject;

   switch (p14P4UserConnection->nProtocol)
   {
      case W_PROP_ISO_14443_4_B:
         return 4; /* PUPI */
      case W_PROP_ISO_14443_4_A:
         return p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;
      default:
         return 0;
   }
}

/* Get identifier */
static void static_P14P4UserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer)
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pObject;

   switch (p14P4UserConnection->nProtocol)
   {
      case W_PROP_ISO_14443_4_B:
         CMemoryCopy(
            pIdentifierBuffer,
            &p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aATQB[1], 4 );
         break;
      case W_PROP_ISO_14443_4_A:
         CMemoryCopy(
            pIdentifierBuffer,
            p14P4UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
            p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
         break;
   }
}

/* See Client API Specifications */
W_ERROR P14Part4GetConnectionInfoPart3(
            tContext* pContext,
            W_HANDLE hConnection,
            tW14Part3ConnectionInfo* p14Part3ConnectionInfo )
{
   tP14P4UserConnection* p14P4UserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_4_USER_CONNECTION, (void**)&p14P4UserConnection);

   if ( nError == W_SUCCESS )
   {
      if (p14Part3ConnectionInfo != null)
      {
         switch (p14P4UserConnection->nProtocol)
         {
            case W_PROP_ISO_14443_4_B:
               /*ATQB*/
               CMemoryCopy(
                  p14Part3ConnectionInfo->sW14TypeB.aATQB,
                  p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aATQB,
                  0x0C);
               /* AFI */
               p14Part3ConnectionInfo->sW14TypeB.nAFI               = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nAFI;
               /* CID support */
               p14Part3ConnectionInfo->sW14TypeB.bIsCIDSupported    = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.bIsCIDSupported;
               /* NAD support */
               p14Part3ConnectionInfo->sW14TypeB.bIsNADSupported    = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.bIsNADSupported;
               /* card Input buffer size */
               p14Part3ConnectionInfo->sW14TypeB.nCardInputBufferSize    = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize;
               /* reader Input buffer size */
               p14Part3ConnectionInfo->sW14TypeB.nReaderInputBufferSize  = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nReaderInputBufferSize;
               /* Baud rate */
               p14Part3ConnectionInfo->sW14TypeB.nBaudRate               = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate;
               /* Timeout */
               p14Part3ConnectionInfo->sW14TypeB.nTimeout                = p14P4UserConnection->nTimeout;
               /*higher layer data*/
               CMemoryCopy(
                     p14Part3ConnectionInfo->sW14TypeB.aHigherLayerData,
                     p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aHigherLayerData,
                     p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerDataLength );
               p14Part3ConnectionInfo->sW14TypeB.nHigherLayerDataLength = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerDataLength;
               /* higher layer response*/
               CMemoryCopy(
                     p14Part3ConnectionInfo->sW14TypeB.aHigherLayerResponse,
                     p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aHigherLayerResponse,
                     p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerResponseLength );
               p14Part3ConnectionInfo->sW14TypeB.nHigherLayerResponseLength = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerResponseLength;
               /*store MBLI-CID : ATTRB first byte*/
               p14Part3ConnectionInfo->sW14TypeB.nMBLI_CID               = p14P4UserConnection->sPart3ConnectionInfo.sTypeB.aInfoBliCid[3];

            break;
            case W_PROP_ISO_14443_4_A:
               /* UID */
               CMemoryCopy(
                  p14Part3ConnectionInfo->sW14TypeA.aUID,
                  p14P4UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
                  p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
               p14Part3ConnectionInfo->sW14TypeA.nUIDLength         = p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;
               /*ATQA*/
               p14Part3ConnectionInfo->sW14TypeA.nATQA = p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nATQA;
               /* SAK */
               p14Part3ConnectionInfo->sW14TypeA.nSAK = p14P4UserConnection->sPart3ConnectionInfo.sTypeA.nSAK;
            break;

            default:
               nError = W_ERROR_CONNECTION_COMPATIBILITY;
               PDebugError("P14Part4GetConnectionInfo3: unknow protocol 0x%02X", p14P4UserConnection->nProtocol);
            break;
         }
      }
      else
      {
         nError = W_ERROR_BAD_PARAMETER;
      }
   }

   return nError;
}


/* See Client API Specifications */
W_ERROR P14Part4GetConnectionInfo(
                  tContext* pContext,
                  W_HANDLE hConnection,
                  tW14Part4ConnectionInfo* p14Part4ConnectionInfo )
{
   tP14P4UserConnection* p14P4Connection;
   W_ERROR nError;

   PDebugTrace("P14Part4GetConnectionInfo");

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_4_USER_CONNECTION, (void**)&p14P4Connection);

   if ( nError == W_SUCCESS )
   {

      if (p14Part4ConnectionInfo == null)
      {
         return W_ERROR_BAD_PARAMETER;
      }

      CMemoryFill(p14Part4ConnectionInfo, 0, sizeof(tW14Part4ConnectionInfo));

      switch (p14P4Connection->nProtocol)
      {
         case W_PROP_ISO_14443_4_A:
            /* CID */
            p14Part4ConnectionInfo->sW14TypeA.bIsCIDSupported    = p14P4Connection->s14typeA4.bIsCIDSupported;
            p14Part4ConnectionInfo->sW14TypeA.nCID               = p14P4Connection->s14typeA4.nCID;
            /* NAD */
            p14Part4ConnectionInfo->sW14TypeA.bIsNADSupported    = p14P4Connection->s14typeA4.bIsNADSupported;
            if ( p14P4Connection->s14typeA4.bIsNADSupported != W_FALSE )
            {
               p14Part4ConnectionInfo->sW14TypeA.nNAD            = p14P4Connection->s14typeA4.nNAD;
            }
            else
            {
               /* Set to a default value */
               p14Part4ConnectionInfo->sW14TypeA.nNAD            = 0x00;
            }
             /* application data */
            p14Part4ConnectionInfo->sW14TypeA.nApplicationDataLength = p14P4Connection->s14typeA4.nApplicationDataLength;
            CMemoryCopy(
               p14Part4ConnectionInfo->sW14TypeA.aApplicationData,
               p14P4Connection->s14typeA4.aApplicationData,
               p14P4Connection->s14typeA4.nApplicationDataLength );
            /* reader Input buffer size */
            p14Part4ConnectionInfo->sW14TypeA.nReaderInputBufferSize  = p14P4Connection->s14typeA4.nReaderInputBufferSize;
            /* card Input buffer size */
            p14Part4ConnectionInfo->sW14TypeA.nCardInputBufferSize   = p14P4Connection->s14typeA4.nCardInputBufferSize;
            /* FWI_SFGI */
            p14Part4ConnectionInfo->sW14TypeA.nFWI_SFGI = p14P4Connection->s14typeA4.nFWI_SFGI;
            /* Timeout */
            p14Part4ConnectionInfo->sW14TypeA.nTimeout           = p14P4Connection->nTimeout;
            /*DATA_RATE_MAX */
            p14Part4ConnectionInfo->sW14TypeA.nDataRateMaxDiv    = p14P4Connection->s14typeA4.nDataRateMaxDiv;
             /* Baud rate */
            p14Part4ConnectionInfo->sW14TypeA.nBaudRate          = p14P4Connection->s14typeA4.nBaudRate;
         break;

         case W_PROP_ISO_14443_4_B:

            p14Part4ConnectionInfo->sW14TypeB.nNAD            = p14P4Connection->s14typeB4.nNAD;
         break;

         default:
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            PDebugError("P14Part4GetConnectionInfo: unknow protocol 0x%02X", p14P4Connection->nProtocol);
         break;
      }
   }
   else
   {
      if (p14Part4ConnectionInfo != null)
      {
         CMemoryFill(p14Part4ConnectionInfo, 0, sizeof(tW14Part4ConnectionInfo));
      }
   }

   return nError;
}

/* Exchange data cancellation callback */

static void static_P14P4UserExchangeDataCancel(tContext* pContext, void* pCancelParameter, bool_t bIsClosing);
static void static_P14P4UserExchangeDataCompleted( tContext* pContext, void *pCallbackParameter, uint32_t nDataLength, W_ERROR nError);

/* See Client API Specifications */

static void static_P14P4UserExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation )
{

   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pObject;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   bool_t      bIsNADSupported;
   uint8_t   nNAD;

   PDebugTrace("static_P14P4UserExchangeData");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if an operation is still pending */
   if ( p14P4UserConnection->hCurrentOperation != W_NULL_HANDLE )
   {
      PDebugError("static_P14P4UserExchangeData: operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* Get the connection information */
   if(p14P4UserConnection->nProtocol == W_PROP_ISO_14443_4_A)
   {
      bIsNADSupported =      p14P4UserConnection->s14typeA4.bIsNADSupported;
      nNAD            =      p14P4UserConnection->s14typeA4.nNAD;
   }
   else if(p14P4UserConnection->nProtocol == W_PROP_ISO_14443_4_B)
   {
      bIsNADSupported =      p14P4UserConnection->s14typeA4.bIsNADSupported;
      nNAD           =       p14P4UserConnection->s14typeB4.nNAD;
   }
   else
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      PDebugWarning("static_P14P4UserExchangeData: unknow protocol 0x%02X", p14P4UserConnection->nProtocol );
      goto return_error;
   }


   /* Check the card buffer size */

   if ( ((pReaderToCardBuffer != null) && (nReaderToCardBufferLength == 0)) ||
        ((pReaderToCardBuffer == null) && (nReaderToCardBufferLength != 0)))
   {
      nError = W_ERROR_BAD_PARAMETER;

      PDebugError("static_P14P4UserExchangeData: pReaderToCardBuffer and nReaderToCardBuffer are not consistent");
      goto return_error;
   }

   if ( ((pCardToReaderBuffer == null) && (nCardToReaderBufferMaxLength != 0)) ||
        ((pCardToReaderBuffer != null) && (nCardToReaderBufferMaxLength == 0)))
   {
      nError = W_ERROR_BAD_PARAMETER;

      PDebugError("static_P14P4UserExchangeData: pCardToReaderBuffer and nCardToReaderBufferMaxLength are not consistent");
      goto return_error;
   }

   if ( nReaderToCardBufferLength > P_14443_4_APDU_MAX_SIZE )
   {
      PDebugError("static_P14P4UserExchangeData: W_ERROR_BUFFER_TOO_LARGE for the card input buffer");
      nError = W_ERROR_BUFFER_TOO_LARGE;
      goto return_error;
   }

   /* Get an operation handle if needed */
   if((p14P4UserConnection->hCurrentOperation = PBasicCreateOperation(pContext, static_P14P4UserExchangeDataCancel, p14P4UserConnection)) == W_NULL_HANDLE)
   {
      PDebugError("static_P14P4UserExchangeData: Cannot allocate the operation");
      goto return_error;
   }

   if(phOperation != null)
   {
      /* Duplicate the handle to be referenced internally and in the returned handle */

      nError = PHandleDuplicate(pContext, p14P4UserConnection->hCurrentOperation, phOperation );

      if(nError != W_SUCCESS)
      {
         PDebugError("static_P14P4UserExchangeData: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, p14P4UserConnection->hCurrentOperation);
         p14P4UserConnection->hCurrentOperation = W_NULL_HANDLE;

         goto return_error;
      }
   }

   p14P4UserConnection->hCurrentDriverOperation = P14P4DriverExchangeData(
      pContext,
      p14P4UserConnection->hDriverConnection,
      static_P14P4UserExchangeDataCompleted,
      p14P4UserConnection,
      pReaderToCardBuffer,
      nReaderToCardBufferLength,
      pCardToReaderBuffer,
      nCardToReaderBufferMaxLength,
      bIsNADSupported,
      nNAD,
      (phOperation != null)?W_TRUE:W_FALSE);

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      PDebugError("static_P14P4UserExchangeData: Error returned by PContextGetLastIoctlError()");
      PHandleClose(pContext, p14P4UserConnection->hCurrentOperation);
      p14P4UserConnection->hCurrentOperation = W_NULL_HANDLE;

      goto return_error;
   }

   /* Store the callback context */
   p14P4UserConnection->sCallbackContext = sCallbackContext;
   p14P4UserConnection->pCardToReaderBuffer = pCardToReaderBuffer;

   /* Increment the reference count to keep the connection object alive
      during the operation.
      The reference count is decreased in static_P14P4UserExchangeDataCompleted
      when the NFC HAL operation is completed */
   PHandleIncrementReferenceCount(p14P4UserConnection);

   return;

return_error:

   PDebugError("P14P4UserExchangeData: returning %s", PUtilTraceError(nError));

   PDFCPostContext3(
      &sCallbackContext,
      0,
      nError );
}

/* See tWBasicGenericDataCallbackFunction */
static void static_P14P4UserExchangeDataCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pCallbackParameter;

   PDebugTrace("static_P14P4UserExchangeDataCompleted");

   /* Check if the operation has been cancelled by the user */
   if ( PBasicGetOperationState(pContext, p14P4UserConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED)
   {
      PDebugError("static_P14P4UserExchangeDataCompleted: Operation is cancelled");

      if(nError == W_SUCCESS)
      {
         nError = W_ERROR_CANCEL;
      }
   }
   else
   {
      PBasicSetOperationCompleted(pContext, p14P4UserConnection->hCurrentOperation);
   }

   PHandleClose(pContext, p14P4UserConnection->hCurrentOperation);
   p14P4UserConnection->hCurrentOperation = W_NULL_HANDLE;

   /* close the driver operation handle */
   PHandleClose(pContext, p14P4UserConnection->hCurrentDriverOperation);
   p14P4UserConnection->hCurrentDriverOperation = W_NULL_HANDLE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_P14P4UserExchangeDataCompleted: Returning %s",
         PUtilTraceError(nError));
      nDataLength = 0;
   }

   /* Send the result */
   PDFCPostContext3(
      &p14P4UserConnection->sCallbackContext,
      nDataLength,
      nError );

   /* Release the reference after completion
      May destroy p14P3UserConnection */
   PHandleDecrementReferenceCount(pContext, p14P4UserConnection);
}

static void static_P14P4UserExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection*)pCancelParameter;

   PDebugTrace("static_P14P4UserExchangeDataCancel");

   PBasicCancelOperation(pContext, p14P4UserConnection->hCurrentDriverOperation);
   PHandleClose(pContext, p14P4UserConnection->hCurrentDriverOperation);
}

/* See Client API Specifications */
W_ERROR P14Part4ListenToCardDetectionTypeA(
                  tContext* pContext,
                  tPReaderCardDetectionHandler* pHandler,
                  void* pHandlerParameter,
                  uint8_t nPriority,
                  bool_t bUseCID,
                  uint8_t nCID,
                  uint32_t nBaudRate,
                  W_HANDLE* phEventRegistry)
{
   tP14P3DetectionConfigurationTypeA sDetectionConfiguration, *pDetectionConfigurationBuffer;
   uint32_t nDetectionConfigurationBufferLength;
   static const uint8_t nProtocol = W_PROP_ISO_14443_4_A;

   PDebugTrace("P14Part4ListenToCardDetectionTypeA");

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

    /* The anti-collision parameters shall be zero if W_PRIORITY_EXCLUSIVE is not used*/
   if((bUseCID != W_FALSE)
    ||(nCID != 0x00)
    ||(nBaudRate != 0x00))
   {
      if(nPriority != W_PRIORITY_EXCLUSIVE)
      {
         PDebugError("P14Part4ListenToCardDetectionTypeA: Bad Detection configuration parameters or bad priorty type passed");
         return W_ERROR_BAD_PARAMETER;
      }
      else
      {
         /* set the pointer to configuration buffer and length*/
         pDetectionConfigurationBuffer = &sDetectionConfiguration;
         nDetectionConfigurationBufferLength = sizeof(tP14P3DetectionConfigurationTypeA);
         /* set the CID*/
         sDetectionConfiguration.bUseCID = bUseCID;
         sDetectionConfiguration.nCID = nCID;
         /* set the bit rate*/
         sDetectionConfiguration.nBaudRate = nBaudRate;
      }
   }
   else
   {
      pDetectionConfigurationBuffer = null;
      nDetectionConfigurationBufferLength = 0x00;
   }

   /* Launch the card detection request */
   return PReaderUserListenToCardDetection(
                     pContext,
                     pHandler,
                     pHandlerParameter,
                     nPriority,
                     &nProtocol, 1,
                     pDetectionConfigurationBuffer, nDetectionConfigurationBufferLength,
                     phEventRegistry );
}

/* See Header file*/
W_ERROR P14Part4ListenToCardDetectionTypeB(
                     tContext* pContext,
                     tPReaderCardDetectionHandler* pHandler,
                     void* pHandlerParameter,
                     uint8_t nPriority,
                     uint8_t nAFI,
                     bool_t bUseCID,
                     uint8_t nCID,
                     uint32_t nBaudRate,
                     const uint8_t* pHigherLayerDataBuffer,
                     uint8_t  nHigherLayerDataLength,
                     W_HANDLE* phEventRegistry)
{
   tP14P3DetectionConfigurationTypeB sDetectionConfiguration, *pDetectionConfigurationBuffer;
   static const uint8_t nProtocol = W_PROP_ISO_14443_4_B;
   uint32_t nDetectionConfigurationBufferLength =
      sizeof(tP14P3DetectionConfigurationTypeB) - W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH;

   PDebugTrace("P14Part4ListenToCardDetectionTypeB");

   if (((pHigherLayerDataBuffer != null) && (nHigherLayerDataLength == 0)) ||
       ((pHigherLayerDataBuffer == null) && (nHigherLayerDataLength != 0)))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   /* The anti-collision parameters shall be zero if W_PRIORITY_EXCLUSIVE is not used*/
   if((nAFI != 0x00)
   ||(bUseCID != W_FALSE)
   ||(nCID != 0x00)
   ||(nBaudRate != 0x00)
   ||(pHigherLayerDataBuffer != null)
   ||(nHigherLayerDataLength != 0x00))
   {
      if(nPriority != W_PRIORITY_EXCLUSIVE)
      {
          PDebugError("P14Part4ListenToCardDetectionTypeB: Bad Detection configuration parameters or bad priorty type passed");
         return W_ERROR_BAD_PARAMETER;
      }
      else
      {
         /* set the pointer to configuration buffer*/
         pDetectionConfigurationBuffer = &sDetectionConfiguration;
         /* set the AFI */
         sDetectionConfiguration.nAFI = nAFI;
         /* set the CID */
         sDetectionConfiguration.bUseCID = bUseCID;
         sDetectionConfiguration.nCID = nCID;
         /* set the bit rate */
         sDetectionConfiguration.nBaudRate = nBaudRate;
         /* set the high layer data length */
         sDetectionConfiguration.nHigherLayerDataLength = nHigherLayerDataLength;

         /* set the hayer layer data */
         if((nHigherLayerDataLength > 0)&&
            (nHigherLayerDataLength <= W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH)&&
            (pHigherLayerDataBuffer != null))
         {
            CMemoryCopy(
               sDetectionConfiguration.aHigherLayerData,
               pHigherLayerDataBuffer,
               nHigherLayerDataLength);
            nDetectionConfigurationBufferLength += nHigherLayerDataLength;

         }
      }
   }
   else
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
W_ERROR P14Part4SetNAD(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t nNAD )
{
   tP14P4UserConnection* p14P4UserConnection;
   W_ERROR nError;

   PDebugTrace("P14Part4SetNAD");

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_4_USER_CONNECTION, (void**)&p14P4UserConnection);

   if ( nError == W_SUCCESS )
   {
      switch(p14P4UserConnection->nProtocol)
      {
         case W_PROP_ISO_14443_4_A:
            if (p14P4UserConnection->s14typeA4.bIsNADSupported != W_FALSE)
            {
               p14P4UserConnection->s14typeA4.nNAD = nNAD;
            }
         break;

         case W_PROP_ISO_14443_4_B:

            if (p14P4UserConnection->sPart3ConnectionInfo.sTypeB.bIsNADSupported != W_FALSE)
            {
               p14P4UserConnection->s14typeB4.nNAD = nNAD;
            }
         break;

         default:
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         PDebugError("P14Part4SetNAD: unknow protocol 0x%02X", p14P4UserConnection->nProtocol);
         break;
      }
   }

   return nError;
}

/* Polling command's callback */
static void static_P14P4PollCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   uint32_t nLength,
   W_ERROR nResult)
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection *)pCallbackParameter;

   PDebugTrace("P14P4Pollcompleted");

   /* Restore the Saved timeout for next exchanges*/
   (void) P14P4DriverSetTimeout(pContext, p14P4UserConnection->hDriverConnection, p14P4UserConnection->nTimeout);

   /* Send the error */
   PDFCPostContext2(&p14P4UserConnection->sCallbackContext, nResult);

   /* Release the reference after completion. May destroy the object */
   PHandleDecrementReferenceCount(pContext, p14P4UserConnection);
}

static const uint8_t static_aPollingCommand [] = {0xFF, 0xFF, 0xFF, 0xFF };

/* Send polling command */
static void static_P14P4Poll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tP14P4UserConnection* p14P4UserConnection = (tP14P4UserConnection *) pObject;

   CDebugAssert( p14P4UserConnection != null );
   CDebugAssert(p14P4UserConnection->nProtocol == W_PROP_ISO_14443_4_A
             || p14P4UserConnection->nProtocol == W_PROP_ISO_14443_4_B );

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P14P4PollCompleted callback  */
   PHandleIncrementReferenceCount(p14P4UserConnection);

   /* store the callback context */
   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &p14P4UserConnection->sCallbackContext );

   /* Change the driver timeout by the POLLING TIMEOUT */
   (void) P14P4DriverSetTimeout(pContext, p14P4UserConnection->hDriverConnection, P_14443_4_POLLING_TIMEOUT);

   /* Send the command */
   (void)P14P4DriverExchangeData(
            pContext,
            p14P4UserConnection->hDriverConnection,
            static_P14P4PollCompleted,
            p14P4UserConnection,
            static_aPollingCommand,
            sizeof(static_aPollingCommand),
            p14P4UserConnection->aPollingBuffer,
            4,
            W_FALSE,
            0,
            W_FALSE);
}

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

void W14Part4ExchangeData(
            W_HANDLE hConnection,
            tWBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation )
{
   uint8_t nPropertyUsed;
   if( WBasicCheckConnectionProperty(hConnection, W_PROP_ISO_14443_4_A) == W_SUCCESS)
   {
      nPropertyUsed = W_PROP_ISO_14443_4_A;
   }else
   {
      nPropertyUsed = W_PROP_ISO_14443_4_B;
   }

   WReaderExchangeDataEx(hConnection,
                         nPropertyUsed,
                         pCallback,
                         pCallbackParameter,
                         pReaderToCardBuffer,
                         nReaderToCardBufferLength,
                         pCardToReaderBuffer,
                         nCardToReaderBufferMaxLength,
                         phOperation);
}


W_ERROR W14Part4ExchangeDataSync(
            W_HANDLE hConnection,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            uint32_t* pnCardToReaderBufferActualLength )
{
   uint8_t nPropertyUsed;
   if( WBasicCheckConnectionProperty(hConnection, W_PROP_ISO_14443_4_A) == W_SUCCESS)
   {
      nPropertyUsed = W_PROP_ISO_14443_4_A;
   }else
   {
      nPropertyUsed = W_PROP_ISO_14443_4_B;
   }

   return WReaderExchangeDataExSync(hConnection,
                                     nPropertyUsed,
                                     pReaderToCardBuffer,
                                     nReaderToCardBufferLength,
                                     pCardToReaderBuffer,
                                     nCardToReaderBufferMaxLength,
                                     pnCardToReaderBufferActualLength);
}

#endif /* P_INCLUDE_DEPRECATED_FUNCTIONS */


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#else /* P_READER_14P4_STANDALONE_SUPPORT */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

W_HANDLE P14P4DriverExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            bool_t     bSendNAD,
            uint8_t  nNAD,
            bool_t bCreateOperation)
{
   /* dummy function for link only, never called */

   return (W_NULL_HANDLE);
}


W_ERROR P14P4DriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout )
{
   /* dummy function for link only, never called */

   return (W_SUCCESS);
}

#endif   /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* P_READER_14P4_STANDALONE_SUPPORT */

