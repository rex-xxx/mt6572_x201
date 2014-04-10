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
   Contains the ISO14443-3 implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( 14_3 )

#include "wme_context.h"
/* detetction message size*/
#define P_14443_3_TYPEA_DETECTION_MESSAGE_SIZE_MIN           7
#define P_14443_3_TYPEA_DETECTION_MESSAGE_SIZE_MAX           13
#define P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN           12      /* ATBB(11) + ATTRIB(1) */
#define P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MAX           (P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN + W_EMUL_HIGHER_LAYER_RESPONSE_MAX_LENGTH)   /* ATQB(11) + ATTRIB(254) */

/* default configuration parameters*/
#define P_14443_3_READER_CONFIG_PARAMETERS                  {0x00, 0x00, 0x00, 0x00, 0x08}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Declare a 14443-3 driver exchange data structure */
typedef struct __tP14P3DriverConnection
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

   /* Connection information */
   uint32_t                   nTimeout;

   /* TCL response buffer */
   uint8_t*                   pCardToReaderBufferTCL;
   /* TCL response buffer maximum length */
   uint32_t                   nCardToReaderBufferMaxLengthTCL;

   /* Buffer for the payload of a NFC HAL command (ISO 14443 frame (without CRC) + NFC HAL exchange control byte */
   uint8_t                    aReaderToCardBufferNAL[P_14443_3_FRAME_MAX_SIZE + 1];

   /* Callback context */
   tDFCDriverCCReference      pDriverCC;

   /* Service Context */
   uint8_t                    nServiceIdentifier;
   /* Service Operation */
   tNALServiceOperation       sServiceOperation;

   /* Operation handle */
   W_HANDLE hOperation;

   /* exchange Bit To Bit indicator */
   bool_t                     bExchangeBitToBitIndicator;
} t14P3DriverConnection;

/* Destroy connection callback */
static uint32_t static_P14P3DriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Connection registry 14443-3 type */
tHandleType g_s14P3DriverConnection = { static_P14P3DriverDestroyConnection,
                                          null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_14443_3_DRIVER_CONNECTION (&g_s14P3DriverConnection)

static uint32_t static_P14P3DriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   t14P3DriverConnection* p14P3DriverConnection = (t14P3DriverConnection*)pObject;

   PDebugTrace("static_P14P3DriverDestroyConnection");

   PDFCDriverFlushCall(p14P3DriverConnection->pDriverCC);

   /* Free the 14443-3 connection structure */
   CMemoryFree( p14P3DriverConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @see  PNALServiceExecuteCommand
 **/
static void static_P14P3DriverExecuteCommandCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter)
{
   t14P3DriverConnection* p14P3DriverConnection = (t14P3DriverConnection*)pCallbackParameter;
   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PDebugError(
         "static_P14P3DriverExecuteCommandCompleted: nError %s",
         PUtilTraceError(nError) );
      if ( nError == W_ERROR_CANCEL )
      {
         goto return_function;
      }
      nLength = 0;
   }
   else
   {
      PDebugTrace("static_P14P3DriverExecuteCommandCompleted: Response");

      PReaderDriverSetAntiReplayReference(pContext);

      if(p14P3DriverConnection->bExchangeBitToBitIndicator == W_TRUE)
      {
         nLength = ((nLength - 2) * 8) + p14P3DriverConnection->pCardToReaderBufferTCL[nLength - 1];
         p14P3DriverConnection->bExchangeBitToBitIndicator = W_FALSE;

         if( (nLength / 8) > p14P3DriverConnection->nCardToReaderBufferMaxLengthTCL )
         {
            nLength = 0;
            nError = W_ERROR_BUFFER_TOO_SHORT;
         }
      }
      else
      {
         /* Check if the buffer is to short */
         if( nLength > p14P3DriverConnection->nCardToReaderBufferMaxLengthTCL )
         {
            nLength = 0;
            nError = W_ERROR_BUFFER_TOO_SHORT;
         }
      }
   }

   /* Set the current operation as completed */
   if (p14P3DriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, p14P3DriverConnection->hOperation);
      PHandleClose(pContext, p14P3DriverConnection->hOperation);
      p14P3DriverConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the answer */
   PDFCDriverPostCC3(
      p14P3DriverConnection->pDriverCC,
      nLength,
      nError );

return_function:

   /* Release the reference after completion
      May destroy p14P3DriverConnection */
   PHandleDecrementReferenceCount(pContext, p14P3DriverConnection);
}

/** @see tPReaderDriverCreateConnection() */
static W_ERROR static_P14P3DriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   t14P3DriverConnection* p14P3DriverConnection;
   W_HANDLE hDriverConnection;
   W_ERROR nError;

   /* Check the parameters */
   if ( phDriverConnection == null )
   {
      PDebugError("static_P14P3DriverCreateConnection: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the 14443-3 buffer */
   p14P3DriverConnection = (t14P3DriverConnection*)CMemoryAlloc( sizeof(t14P3DriverConnection) );
   if ( p14P3DriverConnection == null )
   {
      PDebugError("static_P14P3DriverCreateConnection: p14P3DriverConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(p14P3DriverConnection, 0, sizeof(t14P3DriverConnection));

   /* Create the 14443-3 operation handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     p14P3DriverConnection,
                     P_HANDLE_TYPE_14443_3_DRIVER_CONNECTION,
                     &hDriverConnection ) ) != W_SUCCESS )
   {
      PDebugError("static_P14P3DriverCreateConnection: could not create 14443-3 connection handle");
      CMemoryFree(p14P3DriverConnection);
      return nError;
   }

   /* Store the connection information */
   p14P3DriverConnection->nServiceIdentifier = nServiceIdentifier;

   *phDriverConnection = hDriverConnection;

   return W_SUCCESS;
}

/** @see tPReaderDriverSetDetectionConfiguration() */
static W_ERROR static_P14P3DriverSetDetectionConfigurationTypeB(
                     tContext* pContext,
                     uint8_t* pCommandBuffer,
                     uint32_t* pnCommandBufferLength,
                     const uint8_t* pDetectionConfigurationBuffer,
                     uint32_t nDetectionConfigurationLength )
{
   const tP14P3DetectionConfigurationTypeB *p14P3DetectionConfigurationTypeB =
      (const tP14P3DetectionConfigurationTypeB*)pDetectionConfigurationBuffer;
   uint32_t nIndex = 0;
   static const uint8_t aCommandBuffer[] = P_14443_3_READER_CONFIG_PARAMETERS;
   if((pCommandBuffer == null)
   || (pnCommandBufferLength == null))
   {
      PDebugError("static_P14P3DriverSetDetectionConfigurationTypeB: bad Parameters");
      return W_ERROR_BAD_PARAMETER;
   }
   if((pDetectionConfigurationBuffer == null)
   || (nDetectionConfigurationLength == 0))
   {
      PDebugWarning("static_P14P3DriverSetDetectionConfigurationTypeB: set default parameters");
      CMemoryCopy(pCommandBuffer,
                  aCommandBuffer,
                  sizeof(aCommandBuffer));
      *pnCommandBufferLength = sizeof(aCommandBuffer);
      return W_SUCCESS;
   }
   else if(nDetectionConfigurationLength < (sizeof(tP14P3DetectionConfigurationTypeB) - W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH)
      || (nDetectionConfigurationLength > sizeof(tP14P3DetectionConfigurationTypeB)))
   {
      PDebugError("static_P14P3DriverSetDetectionConfigurationTypeB: bad nDetectionConfigurationLength (0x%x", nDetectionConfigurationLength);
      *pnCommandBufferLength = 0x00;
      return W_ERROR_BAD_PARAMETER;
   }
   /* Set the Data Rate code*/
   switch(p14P3DetectionConfigurationTypeB->nBaudRate)
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
   /* set AFI value*/
   pCommandBuffer[nIndex++] = p14P3DetectionConfigurationTypeB->nAFI;
   /* Set CID supported flag*/
   pCommandBuffer[nIndex++] = (uint8_t)p14P3DetectionConfigurationTypeB->bUseCID;
   /* Set the CID value*/
   pCommandBuffer[nIndex++] = p14P3DetectionConfigurationTypeB->nCID;
   /*set default FSD: this parameter is not provided by the caller*/
   pCommandBuffer[nIndex++] = P_14443_3_FSD_CODE_MAX_VALUE;
   /* set higher layer data*/
   if((p14P3DetectionConfigurationTypeB->nHigherLayerDataLength > 0)&&
      (p14P3DetectionConfigurationTypeB->nHigherLayerDataLength <= W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH))

   {
      CMemoryCopy(
        &pCommandBuffer[nIndex],
        p14P3DetectionConfigurationTypeB->aHigherLayerData,
        p14P3DetectionConfigurationTypeB->nHigherLayerDataLength);
      nIndex += p14P3DetectionConfigurationTypeB->nHigherLayerDataLength;
   }

   *pnCommandBufferLength = nIndex;
   return W_SUCCESS;
}

#ifdef P_READER_14P4_STANDALONE_SUPPORT

/** @see static_P14P3DriverSetDetectionConfigurationTypeB() */

W_ERROR P14P3DriverSetDetectionConfigurationTypeB(
                     tContext* pContext,
                     uint8_t* pCommandBuffer,
                     uint32_t* pnCommandBufferLength,
                     const uint8_t* pDetectionConfigurationBuffer,
                     uint32_t nDetectionConfigurationLength)
{
   return static_P14P3DriverSetDetectionConfigurationTypeB(
                     pContext,
                     pCommandBuffer,
                     pnCommandBufferLength,
                     pDetectionConfigurationBuffer,
                     nDetectionConfigurationLength );
}

#endif /* ifdef P_READER_14P4_STANDALONE_SUPPORT */

/** @see tPReaderDriverParseDetectionMessage() */
static W_ERROR static_P14P3DriverParseDetectionMessage(
               tContext* pContext,
               const uint8_t* pBuffer,
               uint32_t nLength,
               tPReaderDriverCardInfo* pCardInfo )
{
   uint32_t nIndex = 0;
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_P14P3DriverParseDetectionMessage()");

   switch ( pCardInfo->nProtocolBF )
   {
      case W_NFCC_PROTOCOL_READER_ISO_14443_3_A:
         if((pBuffer == null)
          ||(nLength < P_14443_3_TYPEA_DETECTION_MESSAGE_SIZE_MIN)
          ||(nLength > P_14443_3_TYPEA_DETECTION_MESSAGE_SIZE_MAX))
          {
             PDebugTrace("static_P14P3DriverParseDetectionMessage(): bad parameters");
             nError = W_ERROR_NFC_HAL_COMMUNICATION;
             goto return_function;
          }
          /* get UID size */
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
                  PDebugError("static_P14P3UserParseTypeA: wrong UID length");
                  nError = W_ERROR_NFC_HAL_COMMUNICATION;
                  goto return_function;
          }
          /* check if  card Protocol is compliant to 14443-4*/
          if ( (pBuffer[nIndex + 2] & 0x20) != 0 )
          {
             /* PICC compliant with ISO/IEC 14443-4 */
             PDebugTrace("static_P14P3DriverParseDetectionMessage: Type A - 14443-4 compliant");
          }
          else
          {
             /* PICC not compliant with ISO/IEC 14443-4 */
             PDebugWarning("static_P14P3DriverParseDetectionMessage: Type A - only 14443-3 compliant");
          }
          /* copy the UID*/
          if(pCardInfo->nUIDLength > P_READER_MAX_UID_LENGTH)
          {
             PDebugError("static_P14P3DriverParseDetectionMessage: UID too long");
             nError = W_ERROR_NFC_HAL_COMMUNICATION;
             goto return_function;
          }
          CMemoryCopy(pCardInfo->aUID, &pBuffer[nIndex + 3], pCardInfo->nUIDLength );
          break;
       case W_NFCC_PROTOCOL_READER_ISO_14443_3_B:
          /*pBuffer  carry byte 2 to 12 of ATQB and answer to ATTRIB*/
          if((pBuffer == null)
          ||(nLength < P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN)
          ||(nLength > P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MAX))
          {
             PDebugTrace("static_P14P3DriverParseDetectionMessage(): bad parameters");
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
             PDebugTrace("static_P14P3DriverParseDetectionMessage: 14443-4 compliant");
             pCardInfo->nProtocolBF |= W_NFCC_PROTOCOL_READER_ISO_14443_4_B;
          }
          else
          {
             /* PICC not compliant with ISO/IEC 14443-4 */
             PDebugTrace("static_P14P3DriverParseDetectionMessage: only 14443-3 compliant");
          }
          break;
      default:
         PDebugError("static_P14P3DriverParseDetectionMessage: protocol error");
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
         goto return_function;
   }

return_function:

   if(nError == W_SUCCESS)
   {
      PDebugTrace("static_P14P3DriverParseDetectionMessage: UID = ");
      PDebugTraceBuffer(pCardInfo->aUID, pCardInfo->nUIDLength);

   }
   else
   {
      PDebugTrace("static_P14P3DriverParseDetectionMessage: error %s", PUtilTraceError(nError));
   }

   return nError;
}

/* See Header file */
W_ERROR P14P3DriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout )
{
   t14P3DriverConnection* p14P3DriverConnection;
   W_ERROR nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_14443_3_DRIVER_CONNECTION,
      (void**)&p14P3DriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Check the timeout value */
      if ( nTimeout > 14 )
      {
         return W_ERROR_BAD_PARAMETER;
      }
      p14P3DriverConnection->nTimeout = nTimeout;
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
void P14P3DriverExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   t14P3DriverConnection* p14P3DriverConnection = pCancelParameter;

   PDebugTrace("P14P3DriverExchangeDataCancel");

   /* request the cancel of the NFC HAL operation */
   PNALServiceCancelOperation(pContext, & p14P3DriverConnection->sServiceOperation);
}

/* See Header file */
W_HANDLE P14P3DriverExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            bool_t bCheckResponseCRC,
            bool_t bCheckAckOrNack)
{
   t14P3DriverConnection* p14P3DriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;
   uint8_t nProtocolControlByte;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );

   nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_14443_3_DRIVER_CONNECTION,
      (void**)&p14P3DriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      p14P3DriverConnection->pDriverCC = pDriverCC;

      /* Check the parameters */
      if (  ( pReaderToCardBuffer == null )
         || ( nReaderToCardBufferLength == 0 )
         || ( nReaderToCardBufferLength > P_14443_3_FRAME_MAX_SIZE )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) ) )
      {
         PDebugError("P14P3DriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      /* Create the operation handle */

      p14P3DriverConnection->hOperation = PBasicCreateOperation(pContext, P14P3DriverExchangeDataCancel, p14P3DriverConnection);

      if (p14P3DriverConnection->hOperation == W_NULL_HANDLE)
      {
         PDebugError("P14P3DriverExchangeData : PBasicCreateOperation failed");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      nError = PHandleDuplicate(pContext, p14P3DriverConnection->hOperation, & hOperation);

      if (nError != W_SUCCESS)
      {
         PDebugError("P14P3DriverExchangeData : PHandleDuplicate failed %s", PUtilTraceError(nError));
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      /* Store the connection information */
      p14P3DriverConnection->nCardToReaderBufferMaxLengthTCL = nCardToReaderBufferMaxLength;
      /* Store the callback buffer */
      p14P3DriverConnection->pCardToReaderBufferTCL = pCardToReaderBuffer;

      /* Prepare the command */
      nProtocolControlByte = (uint8_t)(p14P3DriverConnection->nTimeout) | NAL_TIMEOUT_READER_XCHG_DATA_ENABLE;
      if(bCheckResponseCRC != W_FALSE)
      {
         /* Set the check CRC control byte */
         nProtocolControlByte |= NAL_ISO_14_A_3_CRC_CHECK;
      }

      if (bCheckAckOrNack != W_FALSE)
      {
         /* Set the check ACK/NACK control byte */
         nProtocolControlByte |= NAL_ISO_14_A_3_T2T_ACK_NACK_CHECK;
      }
      p14P3DriverConnection->aReaderToCardBufferNAL[0] = nProtocolControlByte;
      CMemoryCopy(
         &p14P3DriverConnection->aReaderToCardBufferNAL[1],
         pReaderToCardBuffer,
         nReaderToCardBufferLength );

      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_P14P3DriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(p14P3DriverConnection);

      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         p14P3DriverConnection->nServiceIdentifier,
         &p14P3DriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         p14P3DriverConnection->aReaderToCardBufferNAL,
         nReaderToCardBufferLength + 1,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength,
         static_P14P3DriverExecuteCommandCompleted,
         p14P3DriverConnection );
   }
   else
   {
      PDebugError("P14P3DriverExchangeData: could not get p14P3DriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }

   return (hOperation);

error:

   if (p14P3DriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, p14P3DriverConnection->hOperation);
      PHandleClose(pContext, p14P3DriverConnection->hOperation);
      p14P3DriverConnection->hOperation = W_NULL_HANDLE;
   }

   if (hOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hOperation);
   }

   /* Send the error */
   PDFCDriverPostCC3(
      p14P3DriverConnection->pDriverCC,
      0,
      nError );

   return (W_NULL_HANDLE);
}

/* See Header file */
W_HANDLE P14P3DriverExchangeRawMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength)
{
   t14P3DriverConnection* p14P3DriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;
   uint8_t nProtocolControlByte;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );

   nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_14443_3_DRIVER_CONNECTION,
      (void**)&p14P3DriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      p14P3DriverConnection->pDriverCC = pDriverCC;

      /* Check the parameters */
      if (  ( pReaderToCardBuffer == null )
         || ( nReaderToCardBufferLength == 0 )
         || ( nReaderToCardBufferLength > (P_14443_3_FRAME_MAX_SIZE * 8) )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) ))
      {
         PDebugError("P14P3DriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      /* Create the operation handle */
      p14P3DriverConnection->hOperation = PBasicCreateOperation(pContext, P14P3DriverExchangeDataCancel, p14P3DriverConnection);

      if (p14P3DriverConnection->hOperation == W_NULL_HANDLE)
      {
         PDebugError("P14P3DriverExchangeData : PBasicCreateOperation failed");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      nError = PHandleDuplicate(pContext, p14P3DriverConnection->hOperation, & hOperation);

      if (nError != W_SUCCESS)
      {
         PDebugError("P14P3DriverExchangeData : PHandleDuplicate failed %s", PUtilTraceError(nError));
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      /* Store the connection information */
      p14P3DriverConnection->nCardToReaderBufferMaxLengthTCL = nCardToReaderBufferMaxLength;


      /* Store the callback buffer */
      p14P3DriverConnection->pCardToReaderBufferTCL = pCardToReaderBuffer;

      /* Prepare the command */
      nProtocolControlByte = (uint8_t)(p14P3DriverConnection->nTimeout) | NAL_TIMEOUT_READER_XCHG_DATA_ENABLE | NAL_ISO_14_A_3_USE_MIFARE;

      p14P3DriverConnection->aReaderToCardBufferNAL[0] = nProtocolControlByte;
      CMemoryCopy(
         &p14P3DriverConnection->aReaderToCardBufferNAL[1],
         pReaderToCardBuffer,
         nReaderToCardBufferLength );


      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_P14P3DriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(p14P3DriverConnection);

      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         p14P3DriverConnection->nServiceIdentifier,
         &p14P3DriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         p14P3DriverConnection->aReaderToCardBufferNAL,
         nReaderToCardBufferLength + 1,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength, /* Number of bit is converted to the maximum number of Byte */
         static_P14P3DriverExecuteCommandCompleted,
         p14P3DriverConnection );
   }
   else
   {
      PDebugError("P14P3DriverExchangeData: could not get p14P3DriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }

   return (hOperation);

error:

   if (p14P3DriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, p14P3DriverConnection->hOperation);
      PHandleClose(pContext, p14P3DriverConnection->hOperation);
      p14P3DriverConnection->hOperation = W_NULL_HANDLE;
   }

   if (hOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hOperation);
   }

   /* Send the error */
   PDFCDriverPostCC3(
      p14P3DriverConnection->pDriverCC,
      0,
      nError );

   return (W_NULL_HANDLE);
}


/* See Header file */
W_HANDLE P14P3DriverExchangeRawBits(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t  nLastByteBitNumber,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            uint8_t  nExpectedBits)
{
   t14P3DriverConnection* p14P3DriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;
   uint8_t nProtocolControlByte;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );


   nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_14443_3_DRIVER_CONNECTION,
      (void**)&p14P3DriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      p14P3DriverConnection->pDriverCC = pDriverCC;

      /* Check the parameters */
      if (  ( pReaderToCardBuffer == null )
         || ( nReaderToCardBufferLength == 0 )
         || ( nReaderToCardBufferLength > (P_14443_3_FRAME_MAX_SIZE * 8) )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) )
         || ( nExpectedBits > 8 )
         || ( nLastByteBitNumber == 0 )
         || ( nLastByteBitNumber > 8 ))
      {
         PDebugError("P14P3DriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      /* Create the operation handle */

      p14P3DriverConnection->hOperation = PBasicCreateOperation(pContext, P14P3DriverExchangeDataCancel, p14P3DriverConnection);

      if (p14P3DriverConnection->hOperation == W_NULL_HANDLE)
      {
         PDebugError("P14P3DriverExchangeData : PBasicCreateOperation failed");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      nError = PHandleDuplicate(pContext, p14P3DriverConnection->hOperation, & hOperation);

      if (nError != W_SUCCESS)
      {
         PDebugError("P14P3DriverExchangeData : PHandleDuplicate failed %s", PUtilTraceError(nError));
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      /* Store the connection information */
      p14P3DriverConnection->nCardToReaderBufferMaxLengthTCL = nCardToReaderBufferMaxLength;


      /* Store the callback buffer */
      p14P3DriverConnection->pCardToReaderBufferTCL = pCardToReaderBuffer;

      /* Prepare the command */
      nProtocolControlByte = (uint8_t)(p14P3DriverConnection->nTimeout) | NAL_TIMEOUT_READER_XCHG_DATA_ENABLE | NAL_ISO_14_A_3_ADD_FIXED_BIT_NUMBER;

      p14P3DriverConnection->aReaderToCardBufferNAL[0] = nProtocolControlByte;
      CMemoryCopy(
         &p14P3DriverConnection->aReaderToCardBufferNAL[1],
         pReaderToCardBuffer,
         nReaderToCardBufferLength );

      p14P3DriverConnection->aReaderToCardBufferNAL[1 + nReaderToCardBufferLength] = nExpectedBits;
      p14P3DriverConnection->aReaderToCardBufferNAL[2 + nReaderToCardBufferLength] = nLastByteBitNumber;

      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_P14P3DriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(p14P3DriverConnection);

      p14P3DriverConnection->bExchangeBitToBitIndicator = W_TRUE;

      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         p14P3DriverConnection->nServiceIdentifier,
         &p14P3DriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         p14P3DriverConnection->aReaderToCardBufferNAL,
         nReaderToCardBufferLength + 3,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength, /* Number of bit is converted to the maximum number of Byte */
         static_P14P3DriverExecuteCommandCompleted,
         p14P3DriverConnection );
   }
   else
   {
      PDebugError("P14P3DriverExchangeData: could not get p14P3DriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }

   return (hOperation);

error:

   if (p14P3DriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, p14P3DriverConnection->hOperation);
      PHandleClose(pContext, p14P3DriverConnection->hOperation);
      p14P3DriverConnection->hOperation = W_NULL_HANDLE;
   }

   if (hOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hOperation);
   }

   /* Send the error */
   PDFCDriverPostCC3(
      p14P3DriverConnection->pDriverCC,
      0,
      nError );

   return (W_NULL_HANDLE);
}


/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sP14P3ReaderProtocolConstantTypeA = {
   W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
   NAL_SERVICE_READER_14_A_3,
   static_P14P3DriverCreateConnection,
   null,
   static_P14P3DriverParseDetectionMessage,
   null };

/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sP14P3ReaderProtocolConstantTypeB = {
   W_NFCC_PROTOCOL_READER_ISO_14443_3_B,
   NAL_SERVICE_READER_14_B_3,
   static_P14P3DriverCreateConnection,
   static_P14P3DriverSetDetectionConfigurationTypeB,
   static_P14P3DriverParseDetectionMessage,
   null };

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* 14443 defines */
#define P_14443_UID_MAX_LENGTH               0x0A
#define P_14443_PUPI_MAX_LENGTH              0x04
#define P_14443_B_ATQB_APPLICATION_DATA_LENGTH  0x04

/* Command types */
#define P_14443_COMMAND_INIT                 0x00
#define P_14443_COMMAND_EXECUTE              0x03

/*Conection Info structure Type A*/
typedef struct __tP14P3UserConnectionInfoTypeA
{
   uint8_t                    nUIDLength;
   uint8_t                    aUID[P_14443_UID_MAX_LENGTH];
   uint16_t                   nATQA;
   uint8_t                    nSAK;
   uint8_t                    nBitFrameAnticollision;
   uint8_t                    nATQAPropCoding;
}tP14P3UserConnectionInfoTypeA;

/*Conection Info structure Type B*/
typedef struct __tP14P3UserConnectionInfoTypeB
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
}tP14P3UserConnectionInfoTypeB;

/* Declare a 14443-3 exchange data structure */
typedef struct __tP14P3UserConnection
{
   /* Connection registry handle */
   tHandleObjectHeader        sObjectHeader;

   /* Driver connection handle */
   W_HANDLE                   hDriverConnection;

   /* Operation handles */
   W_HANDLE hCurrentOperation;
   W_HANDLE hCurrentDriverOperation;

   /* Connection information */
   bool_t                       bIsPart4;
   uint8_t                    nProtocol;
   uint8_t                    nCommandType;
   uint32_t                   nTimeout;
   union
   {
     tP14P3UserConnectionInfoTypeA sTypeA;
     tP14P3UserConnectionInfoTypeB sTypeB;
   }sPart3ConnectionInfo;

   /* Command buffer */
   uint8_t                    aReaderToCardSpecialBuffer[P_14443_3_FRAME_MAX_SIZE];
   /* Response buffer */
   uint8_t                    aCardToReaderSpecialBuffer[P_14443_3_FRAME_MAX_SIZE];
   uint8_t*                   pCardToReaderBuffer;

   /* Callback context */
   tDFCCallbackContext        sCallbackContext;

} tP14P3UserConnection;

/* Destroy connection callback */
static uint32_t static_P14P3UserDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get property numbet callback */
static uint32_t static_P14P3UserGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get properties connection callback */
static bool_t static_P14P3UserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_P14P3UserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

static W_ERROR static_P14P3UserParseCardInfo(
            tContext* pContext,
            tP14P3UserConnection* p14P3UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength );

/* Get identifier length */
static uint32_t static_P14P3UserGetIdentifierLength(
            tContext* pContext,
            void* pObject);

/* Get identifier */
static void static_P14P3UserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer);

/* Exchange Data */
static void static_P14Part3UserExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation );

/* Connection registry 14443-3 type */
tHandleType g_s14P3UserConnection = {     static_P14P3UserDestroyConnection,
                                          null,
                                          static_P14P3UserGetPropertyNumber,
                                          static_P14P3UserGetProperties,
                                          static_P14P3UserCheckProperties,
                                          static_P14P3UserGetIdentifierLength,
                                          static_P14P3UserGetIdentifier,
                                          static_P14Part3UserExchangeData,
                                          null };

#define P_HANDLE_TYPE_14443_3_USER_CONNECTION (&g_s14P3UserConnection)

/**
 * @brief   Destroyes a 14443-3 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_P14P3UserDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tP14P3UserConnection* p14P3UserConnection = (tP14P3UserConnection*)pObject;

   PDebugTrace("static_P14P3UserDestroyConnection");

   /* The driver connection is closed by the reader registry parent object */
   /* p14P3UserConnection->hDriverConnection */

   /* Flush the function calls */
   PDFCFlushCall(&p14P3UserConnection->sCallbackContext);

   /* Free the 14443-3 connection structure */
   CMemoryFree( p14P3UserConnection );

   return P_HANDLE_DESTROY_DONE;
}


/**
 * @brief   Gets the 14443-3 connection property number.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static uint32_t static_P14P3UserGetPropertyNumber(
            tContext* pContext,
            void * pObject)
{
   return 1;
}

/**
 * @brief   Gets the 14443-3 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_P14P3UserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tP14P3UserConnection* p14P3UserConnection = (tP14P3UserConnection*)pObject;

   PDebugTrace("static_P14P3UserGetProperties");

   if ( p14P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_P14P3UserGetProperties: no property");
      return W_FALSE;
   }

   pPropertyArray[0] = p14P3UserConnection->nProtocol;
   return W_TRUE;
}

/**
 * @brief   Checkes the 14443-3 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_P14P3UserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tP14P3UserConnection* p14P3UserConnection = (tP14P3UserConnection*)pObject;

   PDebugTrace(
      "static_P14P3UserCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( p14P3UserConnection->nProtocol != P_PROTOCOL_STILL_UNKNOWN )
   {
      if ( nPropertyValue == p14P3UserConnection->nProtocol )
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
      PDebugError("static_P14P3UserCheckProperties: no property");
      return W_FALSE;
   }
}

/* See P14Part3SetTimeout() */
static W_ERROR static_P14P3UserSetTimeout(
            tContext* pContext,
            tP14P3UserConnection* p14P3UserConnection,
            uint32_t nTimeout )
{
   W_ERROR nError;

   if ( ( nError = P14P3DriverSetTimeout(
                     pContext,
                     p14P3UserConnection->hDriverConnection,
                     nTimeout ) ) == W_SUCCESS )
   {
      p14P3UserConnection->nTimeout = nTimeout;
   }

   return nError;
}

/**
 * @brief   Parses the Type A card answer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p14P3UserConnection  The 14443-3 user connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P14P3UserParseTypeA(
            tContext* pContext,
            tP14P3UserConnection* p14P3UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
    uint32_t nIndex = 0;


    /* 0 - 1 : ATQA (2 bytes)
       2     : SAK  (1 byte)
       3 - n : UID  (4, 7, or 10 bytes)*/

    if((pBuffer == null)
    ||(nLength < P_14443_3_TYPEA_DETECTION_MESSAGE_SIZE_MIN)
    ||(nLength > P_14443_3_TYPEA_DETECTION_MESSAGE_SIZE_MAX))
    {
       PDebugTrace("static_P14P3UserParseTypeA(): bad parameters");
       return W_ERROR_RF_COMMUNICATION;
    }
    /* Proprietary coding */
    p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nATQAPropCoding = pBuffer[nIndex] & 0x0F;
    /* get UID size */
    switch ( ((pBuffer[nIndex + 1] >> 6) & 0x03) )
    {
       case 0:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength = 4;
          break;
       case 1:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength = 7;
          break;
       case 2:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength = 10;
            break;
         default:
            PDebugError("static_P14P3UserParseTypeA: wrong UID length");
            return W_ERROR_RF_COMMUNICATION;
    }
    PDebugTrace(
            "static_P14P3UserParseTypeA: UID length %d",
             p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
    /* copy the UID*/
    CMemoryCopy(
           p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
          &pBuffer[nIndex + 3],
           p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
           PDebugTrace("static_P14P3UserParseTypeA: UID");
           PDebugTraceBuffer(p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID, p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength);
    /* Bit frame */
    p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nBitFrameAnticollision = pBuffer[nIndex + 1] & 0x1F;
    PDebugTrace(
       "static_P14P3UserParseTypeA: nBitFrameCollision %d",
       p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nBitFrameAnticollision );
    /* store the ATQA code*/
    p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nATQA = PUtilReadUint16FromBigEndianBuffer(&pBuffer[nIndex]);
    /* evaluate the SAK value*/
    p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nSAK = pBuffer[nIndex + 2];
    if ( (pBuffer[nIndex + 2] & 0x20) != 0 )
    {
       /* PICC compliant with ISO/IEC 14443-4 */
       PDebugTrace("static_P14P3UserParseTypeA: Type A - 14443-4 compliant");
       p14P3UserConnection->bIsPart4 = W_TRUE;
    }
    else
    {
       /* PICC not compliant with ISO/IEC 14443-4 */
       PDebugWarning("static_P14P3UserParseTypeA: Type A - not 14443-4 compliant");
       p14P3UserConnection->bIsPart4 = W_FALSE;
    }
     /* Set Default Timeout: Timeout will be update if card is level 4*/
    p14P3UserConnection->nTimeout = P_14443_3_A_DEFAULT_TIMEOUT;
    static_P14P3UserSetTimeout(
      pContext,
      p14P3UserConnection,
      p14P3UserConnection->nTimeout );

   return W_SUCCESS;
}

/**
 * @brief   Parses the Type B card answer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p14P3UserConnection  The 14443-3 user connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P14P3UserParseTypeB(
            tContext* pContext,
            tP14P3UserConnection* p14P3UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   uint32_t nIndex = 0;
   W_ERROR nErrorCde = W_SUCCESS;

   /*pBuffer  carry byte 2 to 12 of ATQB and answer to ATTRIB*/
    if((pBuffer == null)
    ||(nLength < P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN)
    ||(nLength > P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MAX))
    {
       PDebugTrace("static_P14P3UserParseTypeB(): bad parameters");
       return W_ERROR_NFC_HAL_COMMUNICATION;
    }
    /* get the PUPI : bytes 2, 3, 4 and 5 of ATQB*/
    p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength = 4;
    CMemoryCopy(
       p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aPUPI,
       &pBuffer[nIndex],
       p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength );
    PDebugTrace("static_P14P3UserParseTypeB: PUPI");
    PDebugTraceBuffer( p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aPUPI, p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength );

    /* Application data: AFI */
    p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nAFI = pBuffer[nIndex + 4];
    PDebugTrace(
       "static_P14P3UserParseTypeB: AFI 0x%02X",
       p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nAFI );
   p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nApplicationDataLength = P_14443_B_ATQB_APPLICATION_DATA_LENGTH;
   CMemoryCopy(
      p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aApplicationData,
      &pBuffer[nIndex + 4],
      P_14443_B_ATQB_APPLICATION_DATA_LENGTH );

    /*store INFO_BLI_CID : 3 protocol bytes of ATQB + MBLI-CID byte*/
    CMemoryCopy(
       p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aInfoBliCid,
       &pBuffer[nIndex + 8],
       0x03 );
    p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aInfoBliCid[3] = pBuffer[nIndex + 11];


    /* Baud rate */
    if ( pBuffer[nIndex + 8] & 0x08 )
    {
      /* Addendum 3 :
         A PICC setting b4 = 1 is not compliant with this standard.
         Until the RFU values with b4 = 1 are assigned by ISO, a PCD receiving Bit_Rate_capability with b4 = 1 should
         interpret the Bit_Rate_capability byte as if b8 to b1 = 0 (only ~106 kbit/s in both directions). */

       PDebugWarning(
          "static_P14P3UserParseTypeB: wrong baud rate value %d",
          ( pBuffer[nIndex + 2] & 0x08 ) );

       p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 212;
    }
    else
    {
       if ( pBuffer[nIndex + 8] & 0x01 )
       {
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 212;
       }
       else
       {
          if ( pBuffer[nIndex + 8] & 0x02 )
          {
             p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 424;
          }
          else
          {
             if ( pBuffer[nIndex + 8] & 0x04 )
             {
                p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 847;
             }
             else
             {
                p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate = 106;
             }
          }
       }
    }
    PDebugTrace(
      "static_P14P3UserParseTypeB: baud rate %d kbit/s",
      p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate );

    /* Maximum input buffer size*/
    switch ( ( pBuffer[nIndex + 9] >> 4 ) & 0x0F )
    {
       case 0:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 16;
          break;
       case 1:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 24;
          break;
       case 2:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 32;
          break;
       case 3:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 40;
          break;
       case 4:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 16;
          break;
       case 5:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 64;
          break;
       case 6:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 96;
          break;
       case 7:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 128;
          break;
       case 8:
          p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 256;
          break;

       default:
          /* Addendum 3 :
             A PICC setting Maximum Frame Size Code = '9'-'F' is not compliant with this standard.
             Until the RFU values '9'-'F' are assigned by ISO, a PCD receiving Maximum_Frame_Size Code = '9'-'F' should
             interpret it as Maximum Frame Size Code = '8' (256 bytes).
             */
         PDebugWarning(
            "static_P14P3UserParseTypeB: wrong maximum buffer size value %d",
            ( pBuffer[nIndex + 3] >> 4 ) & 0x0F );
         p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize = 256;
         break;
   }
   p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nReaderInputBufferSize =  P_14443_3_BUFFER_MAX_SIZE;
   PDebugTrace(
      "static_P14P3UserParseTypeB: max input reader/card buffer %d bytes",
      p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize );

   /* Protocol type */
   switch (pBuffer[nIndex + 9] & 0x0F)
   {
      case 0x00 :
         /* PICC not compliant with ISO/IEC 14443-4 */
         PDebugWarning("static_P14P3UserParseTypeB: not 14443-4 compliant");
         p14P3UserConnection->bIsPart4 = W_FALSE;
         break;

      case 0x01 :

         /* PICC compliant with ISO/IEC 14443-4 */
         PDebugTrace("static_P14P3UserParseTypeB: 14443-4 compliant");
         p14P3UserConnection->bIsPart4 = W_TRUE;
         break;

      default:

         /* Addendum 3,
            The PCD should not continue communicating with a PICC that sets b4 to (1)b.
            -> but will not allow proper communication with PicoPass 32KS and 2KS ? */

         /* Invalid value, consider the  PICC non compliant with ISO/IEC 14443-4 */
         PDebugError("static_P14P3UserParseTypeB: invalid protocol type");
         p14P3UserConnection->bIsPart4 = W_FALSE;
         break;
   }

   /* CID */
   p14P3UserConnection->sPart3ConnectionInfo.sTypeB.bIsCIDSupported = ( ( pBuffer[nIndex + 10] & 0x01 ) == 0x01 ) ? W_TRUE: W_FALSE;
   if ( p14P3UserConnection->sPart3ConnectionInfo.sTypeB.bIsCIDSupported != W_FALSE )
   {
      PDebugTrace("static_P14P3UserParseTypeB: CID supported");
      /* Set a CID (from 0 to 14) */
      p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCID = (pBuffer[nIndex + 11] & 0x0F);
   }
   else
   {
      PDebugTrace("static_P14P3UserParseTypeB: CID not supported");
      /* Set the default CID to 0 */
      p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCID = 0x00;
   }

   /* NAD */
   p14P3UserConnection->sPart3ConnectionInfo.sTypeB.bIsNADSupported = ( ( ( pBuffer[nIndex + 10] >> 1 ) & 0x01 ) == 0x01 ) ? W_TRUE: W_FALSE;
   if ( p14P3UserConnection->sPart3ConnectionInfo.sTypeB.bIsNADSupported != W_FALSE )
   {
      PDebugTrace("static_P14P3UserParseTypeB: NAD supported");
   }
   else
   {
      PDebugTrace("static_P14P3UserParseTypeB: NAD not supported");
   }

   /* Timeout */
   p14P3UserConnection->nTimeout = ( ( pBuffer[nIndex + 10] >> 4 ) & 0x0F );


   if ( p14P3UserConnection->nTimeout > 14 )
   {
      PDebugWarning(
       "static_P14P3UserParseTypeB: wrong timeout value %d",
       p14P3UserConnection->nTimeout );

       /* Addendum 3
          A PICC setting FWI = 15 is not compliant with this standard.
          Until the RFU value 15 is assigned by ISO, a PCD receiving FWI = 15 should interpret it as FWI = 4 */
      p14P3UserConnection->nTimeout = 4;
   }
   else
   {
      PDebugTrace(
        "static_P14P3UserParseTypeB: timeout %d",
        p14P3UserConnection->nTimeout );
   }

   /* Set the timeout */
   static_P14P3UserSetTimeout(
           pContext,
           p14P3UserConnection,
           p14P3UserConnection->nTimeout );
   /**
   * Build the ATQB Frame without CRC
   **/

   /* set header byte*/
   p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aATQB[0] = 0x50;
   /* copy others bytes without crc */
   CMemoryCopy(
          &p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aATQB[1],
          pBuffer,
          11);
   /* now skip ATQB */
   nIndex += 11;
   if(nLength > P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN)
   {
      /* copy the higher layer response */
      CMemoryCopy(
            p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aHigherLayerResponse,
            &pBuffer[nIndex + 1],
            (nLength - P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN));

      p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerResponseLength = (uint8_t)(nLength - P_14443_3_TYPEB_DETECTION_MESSAGE_SIZE_MIN);
   }

   return nErrorCde;
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
static W_ERROR static_P14P3UserParseCardInfo(
            tContext* pContext,
            tP14P3UserConnection* p14P3UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   switch (p14P3UserConnection->nProtocol)
   {
      case W_PROP_ISO_14443_3_B:
         /* Get the Type B information */
         return static_P14P3UserParseTypeB(
                  pContext,
                  p14P3UserConnection,
                  pBuffer,
                  nLength );

      case W_PROP_ISO_14443_3_A:
         /* Get the Type A information */
         return static_P14P3UserParseTypeA(
                  pContext,
                  p14P3UserConnection,
                  pBuffer,
                  nLength );
      default:
         PDebugWarning("static_P14P3UserParseCardInfo: unknown protocol 0x%02X", p14P3UserConnection->nProtocol );
      break;
   }
   return W_ERROR_BAD_PARAMETER;
}


/* See Header file */
void P14P3UserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tP14P3UserConnection* p14P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the 14443-3 buffer */
   p14P3UserConnection = (tP14P3UserConnection*)CMemoryAlloc( sizeof(tP14P3UserConnection) );
   if ( p14P3UserConnection == null )
   {
      PDebugError("P14P3UserCreateConnection: p14P3UserConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_function;
   }
   CMemoryFill(p14P3UserConnection, 0, sizeof(tP14P3UserConnection));

   /* Store the callback context */
   p14P3UserConnection->sCallbackContext = sCallbackContext;

   /* Store the connection information */
   p14P3UserConnection->hDriverConnection  = hDriverConnection;
   p14P3UserConnection->nProtocol          = nProtocol;
   p14P3UserConnection->nCommandType       = P_14443_COMMAND_INIT;

   /* Parse the information */
   if ( ( nError = static_P14P3UserParseCardInfo(
                     pContext,
                     p14P3UserConnection,
                     pBuffer,
                     nLength ) ) != W_SUCCESS )
   {
      PDebugError( "P14P3UserCreateConnection: error while parsing the card information");
      CMemoryFree(p14P3UserConnection);
      goto return_function;
   }
   /* Add the 14443-3 structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     p14P3UserConnection,
                     P_HANDLE_TYPE_14443_3_USER_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("P14P3UserCreateConnection: could not add the 14443-3 buffer");
      /* Send the result */
      CMemoryFree(p14P3UserConnection);
      goto return_function;
   }

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError( "P14P3UserCreateConnection: returning %s",
         PUtilTraceError(nError) );
   }

   PDFCPostContext2(
         &sCallbackContext,
         nError );
}

/* See Header file*/
W_ERROR P14Part3ListenToCardDetectionTypeB(
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
   static const uint8_t nProtocol = W_PROP_ISO_14443_3_B;
   uint32_t nDetectionConfigurationBufferLength =
      sizeof(tP14P3DetectionConfigurationTypeB) - W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH;

   if (((pHigherLayerDataBuffer != null) && (nHigherLayerDataLength == 0)) ||
       ((pHigherLayerDataBuffer == null) && (nHigherLayerDataLength != 0)))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   CMemoryFill(&sDetectionConfiguration, 0, sizeof(tP14P3DetectionConfigurationTypeB));

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
         PDebugError("P14Part3ListenToCardDetectionTypeB: Bad Detection configuration parameters or bad priorty type passed");
         return W_ERROR_BAD_PARAMETER;
      }
      else
      {
          /* set the pointer to configuration buffer*/
         pDetectionConfigurationBuffer = &sDetectionConfiguration;
         /* set the AFI*/
         sDetectionConfiguration.nAFI = nAFI;
         /* set the CID*/
         sDetectionConfiguration.bUseCID = bUseCID;
         sDetectionConfiguration.nCID = nCID;
         /* set the bit rate*/
         sDetectionConfiguration.nBaudRate = nBaudRate;
         /* set the high layer data length */
         sDetectionConfiguration.nHigherLayerDataLength = nHigherLayerDataLength;

         /* set the hayer layer data*/
         if((nHigherLayerDataLength > 0)&&
            (nHigherLayerDataLength <= W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH)&&
            (pHigherLayerDataBuffer != null))
         {
            sDetectionConfiguration.nHigherLayerDataLength = nHigherLayerDataLength;
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

/* See Header file */
W_ERROR P14P3UserCheckPart4(
            tContext* pContext,
            W_HANDLE hConnection )
{
   tP14P3UserConnection* p14P3UserConnection;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);
   if ( nError == W_SUCCESS )
   {
      if ( p14P3UserConnection->bIsPart4 == W_FALSE )
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }

   return nError;
}

/* See Header file */
W_ERROR P14P3UserCheckMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength,
            uint8_t* pnType )
{
   tP14P3UserConnection* p14P3UserConnection = null;
   W_ERROR nError;
   uint8_t nSAK;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);
   if (W_SUCCESS != nError)
   {
      PDebugError("P14P3UserGenCheckMifare: could not get p14P3UserConnection buffer");
      return nError;
   }

   /* check the protocol*/
   if(p14P3UserConnection->nProtocol != W_PROP_ISO_14443_3_A)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* detection algorithm according to MIFARE ISO/IEC 14443 PICC Selection */

   nSAK = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nSAK;

   /* SAK bit 2 ? */
   if (nSAK & 0x02)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   else
   {
      /* SAK bit 4 ? */
      if (nSAK & 0x08)
      {
         /* SAK bit 5 ? */
         if (nSAK & 0x10)
         {
            * pnType = W_PROP_MIFARE_4K;
         }
         else
         {
            /* SAK Bit 1 ? */
            if (nSAK & 0x01)
            {
               * pnType = W_PROP_MIFARE_MINI;
            }
            else
            {
               * pnType = W_PROP_MIFARE_1K;
            }
         }
      }
      else
      {
         /* SAK bit 5 ? */
         if (nSAK & 0x10)
         {
            /* algorithm precises detected card are Mifare Plus in secure level 2,
               so, it's W_PROP_MIFARE_PLUS_X_2K or W_PROP_MIFARE_PLUS_X_4K since
               W_PROP_MIFARE_PLUS_S_2K or W_PROP_MIFARE_PLUS_S_4K do not support SL-2 ! */

            /* SAK bit 1 ? */
            if (nSAK & 0x02)
            {
               * pnType = W_PROP_MIFARE_PLUS_X_4K;
            }
            else
            {
               * pnType = W_PROP_MIFARE_PLUS_X_2K;
            }
         }
         else
         {
            /* SAK bit 6 */
            if (nSAK & 0x20)
            {
               /* Part 4 */
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }
            else
            {

               if (p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[0] == 0x04)
               {
                  * pnType = W_PROP_MIFARE_UL;
               }
               else
               {
                  return W_ERROR_CONNECTION_COMPATIBILITY;
               }
            }
         }
      }
   }

   /* Copy UID */
   CMemoryCopy(pUID,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength);

   *pnUIDLength = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;

   return W_SUCCESS;
}

/* See Header file */
W_ERROR P14P3UserCheckMyD(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength,
            uint8_t* pnType )
{
   tP14P3UserConnection* p14P3UserConnection = null;
   W_ERROR nError;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);
   if (W_SUCCESS != nError)
   {
      PDebugError("P14P3UserGenCheckMifare: could not get p14P3UserConnection buffer");
      return nError;
   }

   /* check the protocol*/
   if(p14P3UserConnection->nProtocol != W_PROP_ISO_14443_3_A)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* check UID */
   if (p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[0] == 0x05)    /* Infineon */
   {
      if (((p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[1] & 0xF0) == 0x20) ||
         ((p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[1] & 0xF0) == 0x10))
      {
         *pnType = W_PROP_MY_D_NFC;
      }
      else if ( (p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[1] & 0xF0) == 0x30)
      {
         *pnType = W_PROP_MY_D_MOVE;
      }
      else
      {
         PDebugLog("P14P3UserCheckMyD: unsupported My-d card (not a Type 2 tag)");
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }
   else
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Copy UID */
   CMemoryCopy(pUID,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength);
   *pnUIDLength = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;

   return W_SUCCESS;
}

#define P14P3_MANUCTURER_ID_KOVIO   0x37

/* See Header file */
W_ERROR P14P3UserCheckKovioRFID(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength)
{
   tP14P3UserConnection* p14P3UserConnection = null;
   W_ERROR nError;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);
   if (W_SUCCESS != nError)
   {
      PDebugError("P14P3UserGenCheckMifare: could not get p14P3UserConnection buffer");
      return nError;
   }

   /* check the protocol*/
   if(p14P3UserConnection->nProtocol != W_PROP_ISO_14443_3_A)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* check UID */
   if (p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[0] != P14P3_MANUCTURER_ID_KOVIO)    /* Infineon */
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Copy UID */
   CMemoryCopy(pUID,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength);
   *pnUIDLength = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;

   return W_SUCCESS;
}


/* See Header file */
W_ERROR P14P3UserCheckNDEF2Gen(
            tContext* pContext,
            W_HANDLE hConnection,
#if 0 /* RFU */
            uint8_t* pnATQA,
            uint8_t* pnSAK,
#endif /* 0 */
            uint8_t* pUID,
            uint8_t* pnUIDLength)
{
   tP14P3UserConnection* p14P3UserConnection;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);
   if (W_SUCCESS != nError)
   {
      PDebugError("P14P3UserCheckNDEF2Gen: could not get p14P3UserConnection buffer");
      return nError;
   }

   /* check the protocol*/
   if(p14P3UserConnection->nProtocol != W_PROP_ISO_14443_3_A)
   {
      PDebugLog("P14P3UserCheckNDEF2Gen: not ISO14443-A3");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Check ATQA and SAK */
   if (0x0044 != p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nATQA ||
         0x00 != p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nSAK)
   {
      PDebugLog("P14P3UserCheckNDEF2Gen: ATQA = %04X and SAK = %02X (expected values are 0044 and 00)",
         p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nATQA,
         p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nSAK);
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   if ( (p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[0] == 0x05) &&
       ((p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID[1] & 0xF0) == 0x10))
   {
      /* Specific case for Ny-D NFC that ARE NOT NFC !!!! */
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

#if 0 /* RFU */
   /* Copy ATQA */
   *pnATQA = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nATQA;

   /* Copy SAK */
   *pnSAK = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nSAK;
#endif /* 0 */

   /* Copy UID */
   CMemoryCopy(
      pUID,
      p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
      p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
   *pnUIDLength = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;

   return W_SUCCESS;
}

/* See Header file */
W_ERROR P14P3UserCheckPico(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pSerialNumber,
            uint8_t* pnLength )
{
   tP14P3UserConnection* p14P3UserConnection;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);
   if ( nError == W_SUCCESS )
   {
       /* check the protocol */
      if((p14P3UserConnection->nProtocol != W_PROP_ISO_14443_3_B)||
        (p14P3UserConnection->bIsPart4 != W_FALSE))
      {
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }
      if ( p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength == 0x04 )
      {
         /* Inside IC Manufacturer Register number is d18
          * @see INS_TST_002 Numro de srie des puces Inside.pdf document
          */
         if ((p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nApplicationDataLength == 0x04 )&&
            (p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aApplicationData[2] == 18))
         {
            /* Copy the PUPI */
            CMemoryCopy(
               pSerialNumber,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aPUPI,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength );

            /* Copy the rest of the serial number */
            CMemoryCopy(
               &pSerialNumber[p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength],
               p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aApplicationData,
               p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nApplicationDataLength );

            *pnLength = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nPUPILength + p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nApplicationDataLength;
            return W_SUCCESS;
         }
      }
      else
      {
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }

   *pnLength = 0x00;

   return W_ERROR_CONNECTION_COMPATIBILITY;
}

/* See Client API Specifications */
W_ERROR P14Part3SetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout )
{
   tP14P3UserConnection* p14P3UserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);

   if ( nError == W_SUCCESS )
   {
      nError = static_P14P3UserSetTimeout(pContext, p14P3UserConnection, nTimeout);
   }

   return nError;
}

/* Get identifier length */
static uint32_t static_P14P3UserGetIdentifierLength(
            tContext* pContext,
            void* pObject)
{
   tP14P3UserConnection* p14P3UserConnection = (tP14P3UserConnection*)pObject;

   switch (p14P3UserConnection->nProtocol)
   {
      case W_PROP_ISO_14443_3_B:
         return 4; /* PUPI */
      case W_PROP_ISO_14443_3_A:
         return p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;
      default:
         return 0;
   }
}

/* Get identifier */
static void static_P14P3UserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer)
{
   tP14P3UserConnection* p14P3UserConnection = (tP14P3UserConnection*)pObject;

   switch (p14P3UserConnection->nProtocol)
   {
      case W_PROP_ISO_14443_3_B:
         CMemoryCopy(
            pIdentifierBuffer,
            &p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aATQB[1], 4 );
         break;
      case W_PROP_ISO_14443_3_A:
         CMemoryCopy(
            pIdentifierBuffer,
            p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
            p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
         break;
   }
}

/* See Client API Specifications */
W_ERROR P14Part3GetConnectionInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tW14Part3ConnectionInfo* p14Part3ConnectionInfo )
{
   tP14P3UserConnection* p14P3UserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);

   if ( nError == W_SUCCESS )
   {
      if (p14Part3ConnectionInfo != null)
      {
         switch (p14P3UserConnection->nProtocol)
         {
            case W_PROP_ISO_14443_3_B:
               /*ATQB*/
               CMemoryCopy(
                  p14Part3ConnectionInfo->sW14TypeB.aATQB,
                  p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aATQB,
                  0x0C);
               /* AFI */
               p14Part3ConnectionInfo->sW14TypeB.nAFI               = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nAFI;
               /* CID support */
               p14Part3ConnectionInfo->sW14TypeB.bIsCIDSupported    = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.bIsCIDSupported;
               /* NAD support */
               p14Part3ConnectionInfo->sW14TypeB.bIsNADSupported    = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.bIsNADSupported;
               /* card Input buffer size */
               p14Part3ConnectionInfo->sW14TypeB.nCardInputBufferSize    = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nCardInputBufferSize;
               /* reader Input buffer size */
               p14Part3ConnectionInfo->sW14TypeB.nReaderInputBufferSize  = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nReaderInputBufferSize;
               /* Baud rate */
               p14Part3ConnectionInfo->sW14TypeB.nBaudRate               = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nBaudRate;
               /* Timeout */
               p14Part3ConnectionInfo->sW14TypeB.nTimeout                = p14P3UserConnection->nTimeout;
               /*higher layer data*/
               CMemoryCopy(
                     p14Part3ConnectionInfo->sW14TypeB.aHigherLayerData,
                     p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aHigherLayerData,
                     p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerDataLength );
               p14Part3ConnectionInfo->sW14TypeB.nHigherLayerDataLength = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerDataLength;
               /* higher layer response*/
               CMemoryCopy(
                     p14Part3ConnectionInfo->sW14TypeB.aHigherLayerResponse,
                     p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aHigherLayerResponse,
                     p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerResponseLength );
               p14Part3ConnectionInfo->sW14TypeB.nHigherLayerResponseLength = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.nHigherLayerResponseLength;
               /*store MBLI-CID : ATTRB first byte*/
               p14Part3ConnectionInfo->sW14TypeB.nMBLI_CID               = p14P3UserConnection->sPart3ConnectionInfo.sTypeB.aInfoBliCid[3];

            break;
            case W_PROP_ISO_14443_3_A:
               /* UID */
               CMemoryCopy(
                  p14Part3ConnectionInfo->sW14TypeA.aUID,
                  p14P3UserConnection->sPart3ConnectionInfo.sTypeA.aUID,
                  p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength );
               p14Part3ConnectionInfo->sW14TypeA.nUIDLength         = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nUIDLength;
               /*ATQA*/
               p14Part3ConnectionInfo->sW14TypeA.nATQA = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nATQA;
               /* SAK */
               p14Part3ConnectionInfo->sW14TypeA.nSAK = p14P3UserConnection->sPart3ConnectionInfo.sTypeA.nSAK;
            break;

            default:
               nError = W_ERROR_CONNECTION_COMPATIBILITY;
               PDebugError("P14Part3GetConnectionInfo: unknow protocol 0x%02X", p14P3UserConnection->nProtocol);
            break;
         }
      }
      else
      {
         nError = W_ERROR_BAD_PARAMETER;
      }
   }
   else
   {

#ifdef P_READER_14P4_STANDALONE_SUPPORT

      nError = P14Part4GetConnectionInfoPart3(
                     pContext,
                     hConnection,
                     p14Part3ConnectionInfo );
#endif /* ifdef P_READER_14P4_STANDALONE_SUPPORT */

   }

   if ((nError != W_SUCCESS) && (p14Part3ConnectionInfo != null))
   {
      CMemoryFill(p14Part3ConnectionInfo, 0, sizeof(tW14Part3ConnectionInfo));
   }

   return nError;
}


/* Same as PReaderExchangeData()
   with an flag to acivate the CRC checking in the response */

static void static_P14P3UserExchangeDataCompleted(tContext* pContext, void *pCallbackParameter, uint32_t nDataLength, W_ERROR nError);
static void static_P14P3UserExchangeDataCancel(tContext* pContext, void* pCancelParameter, bool_t bIsClosing);

void P14P3UserExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation,
            bool_t bCheckResponseCRC,
            bool_t bCheckAckOrNack)
{
   tP14P3UserConnection* p14P3UserConnection;
   W_ERROR nError;

   PDebugTrace("P14P3UserExchangeData");

   nError = PReaderUserGetConnectionObject(pContext, hConnection,
      P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);
   if ( nError != W_SUCCESS )
   {
      PDebugError("P14P3UserExchangeData: could not get p14P3UserConnection buffer");
      p14P3UserConnection = null;
   }

   P14Part3UserExchangeDataEx(
            pContext, p14P3UserConnection,
            pCallback, pCallbackParameter,
            pReaderToCardBuffer, nReaderToCardBufferLength,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength,
            phOperation,
            bCheckResponseCRC, bCheckAckOrNack);
}

/* See PReaderExchangeData */
void P14Part3UserExchangeDataEx(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation,
            bool_t bCheckResponseCRC,
            bool_t bCheckAckOrNack)
{
   tHandleObjectHeader* pObjectHeader = (tHandleObjectHeader*)pObject;
   tP14P3UserConnection* p14P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("P14Part3UserExchangeDataEx");

   while(pObjectHeader != null)
   {
      if(pObjectHeader->pType == P_HANDLE_TYPE_14443_3_USER_CONNECTION)
      {
         break;
      }

      pObjectHeader = pObjectHeader->pParentObject;
   }

   p14P3UserConnection = (tP14P3UserConnection*)pObjectHeader;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   if ( p14P3UserConnection == null )
   {
      PDebugError("P14P3UserExchangeData: could not get p14P3UserConnection buffer");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check if an operation is still pending */
   if ( p14P3UserConnection->hCurrentOperation != W_NULL_HANDLE )
   {
      PDebugError("P14P3UserExchangeData: operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( p14P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("P14P3UserExchangeData: protocol still unknown");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the card buffer size */
   if ((nReaderToCardBufferLength == 0) || (pReaderToCardBuffer == null))
   {
      PDebugError("P14P3UserExchangeData: nReaderToCardBufferLength / pReaderToCardBuffer can not be null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( ( nReaderToCardBufferLength + 2 /* EDC (CRC) */ ) > P_14443_3_BUFFER_MAX_SIZE )
   {
      PDebugError("P14P3UserExchangeData: W_ERROR_BUFFER_TOO_LARGE for the card input buffer");
      nError = W_ERROR_BUFFER_TOO_LARGE;
      goto return_error;
   }

   if ( ((pCardToReaderBuffer == null) && (nCardToReaderBufferMaxLength != 0)) ||
        ((pCardToReaderBuffer != null) && (nCardToReaderBufferMaxLength == 0)) )
   {
      PDebugError("P14P3UserExchangeData: inconsistency between pCardToReaderBuffer and nCardToReaderBufferMaxLength");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle if needed */
   if((p14P3UserConnection->hCurrentOperation = PBasicCreateOperation(
      pContext, static_P14P3UserExchangeDataCancel, p14P3UserConnection)) == W_NULL_HANDLE)
   {
      PDebugError("P14P3UserExchangeData: Cannot allocate the operation");
      goto return_error;
   }

   if(phOperation != null)
   {
      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, p14P3UserConnection->hCurrentOperation, phOperation );
      if(nError != W_SUCCESS)
      {
         PDebugError("P14P3UserExchangeData: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, p14P3UserConnection->hCurrentOperation);
         p14P3UserConnection->hCurrentOperation = W_NULL_HANDLE;

         goto return_error;
      }
   }

   p14P3UserConnection->hCurrentDriverOperation = P14P3DriverExchangeData(
         pContext,
         p14P3UserConnection->hDriverConnection,
         static_P14P3UserExchangeDataCompleted,
         p14P3UserConnection,
         pReaderToCardBuffer,
         nReaderToCardBufferLength,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength,
         bCheckResponseCRC,
         bCheckAckOrNack);

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      PDebugError("P14P3UserExchangeData: Error returned by PContextGetLastIoctlError()");
      PHandleClose(pContext, p14P3UserConnection->hCurrentOperation);
      p14P3UserConnection->hCurrentOperation = W_NULL_HANDLE;

      goto return_error;
   }

   /* Store the callback context */
   p14P3UserConnection->sCallbackContext = sCallbackContext;
   p14P3UserConnection->pCardToReaderBuffer = pCardToReaderBuffer;
   p14P3UserConnection->nCommandType = P_14443_COMMAND_EXECUTE;

   /* Increment the reference count to keep the connection object alive
      during the operation.
      The reference count is decreased in static_P14P3UserExchangeDataCompleted
      when the NFC HAL operation is completed */
   PHandleIncrementReferenceCount(p14P3UserConnection);

   return;

return_error:

   PDebugError("P14P3UserExchangeData: returning %s", PUtilTraceError(nError));

   PDFCPostContext3(
      &sCallbackContext,
      0,
      nError );
}

/* See tWBasicGenericDataCallbackFunction */
static void static_P14P3UserExchangeDataCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tP14P3UserConnection* p14P3UserConnection = (tP14P3UserConnection*)pCallbackParameter;

   /* Check if the operation has been cancelled by the user */
   if ( p14P3UserConnection->hCurrentOperation != W_NULL_HANDLE )
   {
      /* Check the operation state */
      if ( PBasicGetOperationState(pContext, p14P3UserConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED)
      {
         PDebugError("static_P14P3UserExchangeDataCompleted: Operation is cancelled");
         if(nError == W_SUCCESS)
         {
            nError = W_ERROR_CANCEL;
         }
      }
      else
      {
         PBasicSetOperationCompleted(pContext, p14P3UserConnection->hCurrentOperation);
      }

      PHandleClose(pContext, p14P3UserConnection->hCurrentOperation);
      p14P3UserConnection->hCurrentOperation = W_NULL_HANDLE;
   }

   PHandleClose(pContext, p14P3UserConnection->hCurrentDriverOperation);
   p14P3UserConnection->hCurrentDriverOperation = W_NULL_HANDLE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_P14P3UserExchangeDataCompleted: Returning %s",
         PUtilTraceError(nError));
      nDataLength = 0;
   }

   /* Send the result */
   PDFCPostContext3(
      &p14P3UserConnection->sCallbackContext,
      nDataLength,
      nError );

   /* Release the reference after completion
      May destroy p14P3UserConnection */
   PHandleDecrementReferenceCount(pContext, p14P3UserConnection);
}

static void static_P14P3UserExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tP14P3UserConnection* p14P3UserConnection = (tP14P3UserConnection*)pCancelParameter;

   PDebugTrace("static_P14P3UserExchangeDataCancel");

   PBasicCancelOperation(pContext, p14P3UserConnection->hCurrentDriverOperation);
   PHandleClose(pContext, p14P3UserConnection->hCurrentDriverOperation);
   p14P3UserConnection->hCurrentDriverOperation = W_NULL_HANDLE;
}

/* See PReaderExchangeData */
static void static_P14Part3UserExchangeData(
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
   P14Part3UserExchangeDataEx(
            pContext,
            pObject,
            pCallback, pCallbackParameter,
            pReaderToCardBuffer, nReaderToCardBufferLength,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength,
            phOperation,
            W_TRUE, W_FALSE);
}

void P14Part3ExchangeRawBits(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t nLastByteBitNumber,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            uint8_t  nExpectedBit,
            W_HANDLE* phOperation )
{
   tP14P3UserConnection* p14P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   bool_t bIsSupported = W_FALSE;

   PDebugTrace("P14Part3ExchangeDataBit");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   (void) PNFCControllerGetBooleanProperty(pContext,
                                           W_NFCC_PROP_READER_ISO_14443_A_BIT,
                                           &bIsSupported);
   if(bIsSupported == W_FALSE)
   {
      PDebugError("P14Part3ExchangeDataBit: API Raw Mifare not supported");
      /* Send the error */
      PDFCPostContext3(
         &sCallbackContext,
         0,
         W_ERROR_FEATURE_NOT_SUPPORTED);
      return;
   }

   nError = PReaderUserGetConnectionObject(pContext, hConnection,
      P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("P14Part3ExchangeDataBit: could not get p14P3UserConnection buffer");
      goto return_error;
   }

   if ( p14P3UserConnection == null )
   {
      PDebugError("P14P3UserExchangeData: could not get p14P3UserConnection buffer");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check if an operation is still pending */
   if ( p14P3UserConnection->hCurrentOperation != W_NULL_HANDLE )
   {
      PDebugError("P14P3UserExchangeData: operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( p14P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("P14P3UserExchangeData: protocol still unknown");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the card buffer size */
   if ((nReaderToCardBufferLength == 0) || (pReaderToCardBuffer == null))
   {
      PDebugError("P14P3UserExchangeData: nReaderToCardBufferLength / pReaderToCardBuffer can not be null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( nReaderToCardBufferLength > P_14443_3_BUFFER_MAX_SIZE )
   {
      PDebugError("P14P3UserExchangeData: W_ERROR_BUFFER_TOO_LARGE for the card input buffer");
      nError = W_ERROR_BUFFER_TOO_LARGE;
      goto return_error;
   }

   /* Get an operation handle if needed */
   if((p14P3UserConnection->hCurrentOperation = PBasicCreateOperation(
      pContext, static_P14P3UserExchangeDataCancel, p14P3UserConnection)) == W_NULL_HANDLE)
   {
      PDebugError("P14P3UserExchangeData: Cannot allocate the operation");
      goto return_error;
   }

   if(phOperation != null)
   {
      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, p14P3UserConnection->hCurrentOperation, phOperation );
      if(nError != W_SUCCESS)
      {
         PDebugError("P14P3UserExchangeData: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, p14P3UserConnection->hCurrentOperation);
         p14P3UserConnection->hCurrentOperation = W_NULL_HANDLE;

         goto return_error;
      }
   }

   p14P3UserConnection->hCurrentDriverOperation = P14P3DriverExchangeRawBits(
         pContext,
         p14P3UserConnection->hDriverConnection,
         static_P14P3UserExchangeDataCompleted,
         p14P3UserConnection,
         pReaderToCardBuffer,
         nReaderToCardBufferLength,
         nLastByteBitNumber,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength,
         nExpectedBit);

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      PDebugError("P14P3UserExchangeData: Error returned by PContextGetLastIoctlError()");
      PHandleClose(pContext, p14P3UserConnection->hCurrentOperation);
      p14P3UserConnection->hCurrentOperation = W_NULL_HANDLE;

      goto return_error;
   }

   /* Store the callback context */
   p14P3UserConnection->sCallbackContext = sCallbackContext;
   p14P3UserConnection->pCardToReaderBuffer = pCardToReaderBuffer;
   p14P3UserConnection->nCommandType = P_14443_COMMAND_EXECUTE;

   /* Increment the reference count to keep the connection object alive
      during the operation.
      The reference count is decreased in static_P14P3UserExchangeDataCompleted
      when the NFC HAL operation is completed */
   PHandleIncrementReferenceCount(p14P3UserConnection);

   return;

return_error:

   PDebugError("P14P3UserExchangeData: returning %s", PUtilTraceError(nError));

   PDFCPostContext3(
      &sCallbackContext,
      0,
      nError );
}

W_ERROR W14Part3ExchangeRawBitsSync(
               W_HANDLE hConnection,
               const uint8_t* pReaderToCardBuffer,
               uint32_t nReaderToCardBufferLength,
               uint8_t  nReaderToCardBufferLastByteBitNumber,
               uint8_t* pCardToReaderBuffer,
               uint32_t nCardToReaderBufferMaxLength,
               uint8_t  nExpectedBits,
               uint32_t* pnCardToReaderBufferActualLength)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareClassicReadBlockSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {

      W14Part3ExchangeRawBits(hConnection,
                              PBasicGenericSyncCompletionUint32,
                              &param,
                              pReaderToCardBuffer,
                              nReaderToCardBufferLength,
                              nReaderToCardBufferLastByteBitNumber,
                              pCardToReaderBuffer,
                              nCardToReaderBufferMaxLength,
                              nExpectedBits,
                              null);
   }


   return PBasicGenericSyncWaitForResultUint32(&param, pnCardToReaderBufferActualLength);
}

void P14Part3ExchangeRawMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation )
{
   tP14P3UserConnection* p14P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   bool_t bIsSupported = W_FALSE;

   PDebugTrace("P14Part3ExchangeRawMifare");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   (void) PNFCControllerGetBooleanProperty(pContext,
                                           W_NFCC_PROP_READER_MIFARE_CLASSIC_CRYPTO,
                                           &bIsSupported);
   if(bIsSupported == W_FALSE)
   {
      PDebugError("P14Part3ExchangeRawMifare: API Raw Mifare not supported");
      /* Send the error */
      PDFCPostContext3(
         &sCallbackContext,
         0,
         W_ERROR_FEATURE_NOT_SUPPORTED);
      return;
   }

   nError = PReaderUserGetConnectionObject(pContext, hConnection,
      P_HANDLE_TYPE_14443_3_USER_CONNECTION, (void**)&p14P3UserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("P14Part3ExchangeDataBit: could not get p14P3UserConnection buffer");
      goto return_error;
   }

   if ( p14P3UserConnection == null )
   {
      PDebugError("P14P3UserExchangeData: could not get p14P3UserConnection buffer");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check if an operation is still pending */
   if ( p14P3UserConnection->hCurrentOperation != W_NULL_HANDLE )
   {
      PDebugError("P14P3UserExchangeData: operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( p14P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("P14P3UserExchangeData: protocol still unknown");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the card buffer size */
   if ((nReaderToCardBufferLength == 0) || (pReaderToCardBuffer == null))
   {
      PDebugError("P14P3UserExchangeData: nReaderToCardBufferLength / pReaderToCardBuffer can not be null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( nReaderToCardBufferLength > P_14443_3_BUFFER_MAX_SIZE )
   {
      PDebugError("P14P3UserExchangeData: W_ERROR_BUFFER_TOO_LARGE for the card input buffer");
      nError = W_ERROR_BUFFER_TOO_LARGE;
      goto return_error;
   }

   /* Get an operation handle if needed */
   if((p14P3UserConnection->hCurrentOperation = PBasicCreateOperation(
      pContext, static_P14P3UserExchangeDataCancel, p14P3UserConnection)) == W_NULL_HANDLE)
   {
      PDebugError("P14P3UserExchangeData: Cannot allocate the operation");
      goto return_error;
   }

   if(phOperation != null)
   {
      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, p14P3UserConnection->hCurrentOperation, phOperation );
      if(nError != W_SUCCESS)
      {
         PDebugError("P14P3UserExchangeData: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, p14P3UserConnection->hCurrentOperation);
         p14P3UserConnection->hCurrentOperation = W_NULL_HANDLE;

         goto return_error;
      }
   }

   p14P3UserConnection->hCurrentDriverOperation = P14P3DriverExchangeRawMifare(
         pContext,
         p14P3UserConnection->hDriverConnection,
         static_P14P3UserExchangeDataCompleted,
         p14P3UserConnection,
         pReaderToCardBuffer,
         nReaderToCardBufferLength,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength);

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      PDebugError("P14P3UserExchangeData: Error returned by PContextGetLastIoctlError()");
      PHandleClose(pContext, p14P3UserConnection->hCurrentOperation);
      p14P3UserConnection->hCurrentOperation = W_NULL_HANDLE;

      goto return_error;
   }

   /* Store the callback context */
   p14P3UserConnection->sCallbackContext = sCallbackContext;
   p14P3UserConnection->pCardToReaderBuffer = pCardToReaderBuffer;
   p14P3UserConnection->nCommandType = P_14443_COMMAND_EXECUTE;

   /* Increment the reference count to keep the connection object alive
      during the operation.
      The reference count is decreased in static_P14P3UserExchangeDataCompleted
      when the NFC HAL operation is completed */
   PHandleIncrementReferenceCount(p14P3UserConnection);

   return;

return_error:

   PDebugError("P14P3UserExchangeData: returning %s", PUtilTraceError(nError));

   PDFCPostContext3(
      &sCallbackContext,
      0,
      nError );
}

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

void W14Part3ExchangeData(
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
   if( WBasicCheckConnectionProperty(hConnection, W_PROP_ISO_14443_3_A) == W_SUCCESS)
   {
      nPropertyUsed = W_PROP_ISO_14443_3_A;
   }else
   {
      nPropertyUsed = W_PROP_ISO_14443_3_B;
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


W_ERROR W14Part3ExchangeDataSync(
            W_HANDLE hConnection,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            uint32_t* pnCardToReaderBufferActualLength )
{
   uint8_t nPropertyUsed;
   if( WBasicCheckConnectionProperty(hConnection, W_PROP_ISO_14443_3_A) == W_SUCCESS)
   {
      nPropertyUsed = W_PROP_ISO_14443_3_A;
   }else
   {
      nPropertyUsed = W_PROP_ISO_14443_3_B;
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

