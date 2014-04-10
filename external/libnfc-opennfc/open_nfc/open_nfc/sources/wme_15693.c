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
   Contains the implementation of the ISO 15693 Support functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( 15693 )

#include "wme_context.h"

/* UID size*/
#define P_15693_UID_MAX_LENGTH               0x08
/* The default timeout */
#define P_15693_DEFAULT_TIMEOUT              0x0A
#define P_15693_SMALL_TIMEOUT                0x08
/* Buffer maximum size */
#define P_15693_BUFFER_MAX_SIZE              257

/* default configuration parameters*/
#define P_15693_READER_CONFIG_PARAMETERS    {0x00}

/* Queued operation type */
#define P_15_QUEUED_NONE                   0
#define P_15_QUEUED_READ                   1
#define P_15_QUEUED_WRITE                  2
#define P_15_QUEUED_SET_ATTRIBUTE          3

/*Declare Parameters structure for card type 15693-3*/
typedef struct __tP15P3DetectionConfiguration
{
   uint8_t      nAFI;
}tP15P3DetectionConfiguration;

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Declare a 15693-3 exchange data structure */
typedef struct __tP15P3DriverConnection
{
   /* Connection registry handle */
   tHandleObjectHeader        sObjectHeader;

   /* Connection information */
   bool_t                     bIsTimeoutUsed;
   uint8_t                    nAttempt;
   uint32_t                   nTimeout;

   /* ISO 15693 command buffer length */
   uint32_t                   nReaderToCardBufferLength15693;
   /* ISO 15693 response buffer maximum length */
   uint32_t                   nCardToReaderBufferMaxLength15693;

   /* command buffer */
   uint8_t                    aReaderToCardBuffer[P_15693_BUFFER_MAX_SIZE];
   /* response buffer */
   uint8_t*                   pCardToReaderBuffer;

   /* Part3 callback context */
   tDFCDriverCCReference      pDriverCC;

   /* Service Context */
   uint8_t                    nServiceIdentifier;
   /* Service Operation */
   tNALServiceOperation       sServiceOperation;

} tP15P3DriverConnection;

/* Destroy connection callback */
static uint32_t static_P15P3DriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Connection registry 15693-3 type */
tHandleType g_s15P3DriverConnection = { static_P15P3DriverDestroyConnection,
                                    null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_15693_3_DRIVER_CONNECTION (&g_s15P3DriverConnection)

/**
 * @brief   Destroyes a 15693-3 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_P15P3DriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tP15P3DriverConnection* p15P3DriverConnection = (tP15P3DriverConnection*)pObject;

   PDebugTrace("static_P15P3DriverDestroyConnection");

   PDFCDriverFlushCall(p15P3DriverConnection->pDriverCC);

   /* Free the 15693-3 connection structure */
   CMemoryFree( p15P3DriverConnection );

   return P_HANDLE_DESTROY_DONE;
}

/** @see  PNALServiceExecuteCommand **/
static void static_P15P3DriverExchangeDataCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter)
{
   tP15P3DriverConnection* p15P3DriverConnection = (tP15P3DriverConnection*)pCallbackParameter;

   uint8_t *pBuffer = p15P3DriverConnection->pCardToReaderBuffer;

   PDebugTrace("static_P15P3DriverExchangeDataCompleted: nError 0x%08X", nError );

   if(nError == W_SUCCESS)
   {
      PReaderDriverSetAntiReplayReference(pContext);
   }
   else
   {
      PDebugError("static_P15P3DriverExchangeDataCompleted: nError 0x%08X", nError );
   }

   /* Check the error code */
   if ( (  ( nError != W_SUCCESS )
         && ( nError != W_ERROR_CANCEL ) )
      || (  ( nError == W_SUCCESS )
         /* Check the command error code */
         && ( (pBuffer[0] & 0x01) == 0x01 )
               /* Block not successfully programmed */
         && (  ( pBuffer[1] == 0x13 )
               /* Block not successfully locked */
            || ( pBuffer[1] == 0x14 ) ) ) )
   {
      /* Check if a timeout was sent on a Tag It card */
      if (  ( nError == W_ERROR_TIMEOUT )
         && ( p15P3DriverConnection->bIsTimeoutUsed == W_FALSE ) )
      {
         PDebugTrace( "static_P15P3DriverExchangeDataCompleted: timeout not used" );
      }
      else
      {
         /* Check if we can send the command again */
         if ( p15P3DriverConnection->nAttempt < ( P_15693_3_MAX_ATTEMPT - 1 ) )
         {

            PDebugTrace( "static_P15P3DriverExchangeDataCompleted: SEND SAME COMMAND AGAIN ==> %d", p15P3DriverConnection->nAttempt);
            p15P3DriverConnection->nAttempt ++;

            /* Resend the command */
            PNALServiceExecuteCommand(
               pContext,
               NAL_SERVICE_READER_15_3,
               &p15P3DriverConnection->sServiceOperation,
               NAL_CMD_READER_XCHG_DATA,
               p15P3DriverConnection->aReaderToCardBuffer,
               p15P3DriverConnection->nReaderToCardBufferLength15693 + 1,
               p15P3DriverConnection->pCardToReaderBuffer,
               p15P3DriverConnection->nCardToReaderBufferMaxLength15693,
               static_P15P3DriverExchangeDataCompleted,
               p15P3DriverConnection );
           return;
         }
      }
   }

   if (nError == W_SUCCESS)
   {
      p15P3DriverConnection->nReaderToCardBufferLength15693 = nLength;
   }
   else
   {
      p15P3DriverConnection->nReaderToCardBufferLength15693 = 0;
   }

   if ( nError != W_ERROR_CANCEL )
   {

      PDebugTrace("static_P15P3DriverExchangeDataCompleted : PHandleDecrementReferenceCount");
      PHandleDecrementReferenceCount(pContext, p15P3DriverConnection);

      /* Send the result */
      PDFCDriverPostCC3(
         p15P3DriverConnection->pDriverCC,
         p15P3DriverConnection->nReaderToCardBufferLength15693,
         nError );
   }
}

/* See Header file */
W_ERROR P15P3DriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout )
{
   tP15P3DriverConnection* p15P3DriverConnection;
   W_ERROR nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_15693_3_DRIVER_CONNECTION, (void**)&p15P3DriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Check the timeout value */
      if ( nTimeout > 14 )
      {
         return W_ERROR_BAD_PARAMETER;
      }
      p15P3DriverConnection->nTimeout = nTimeout;
   }

   return nError;
}

/** @see tPReaderDriverCreateConnection() */
static W_ERROR static_P15P3DriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   tP15P3DriverConnection* p15P3DriverConnection;
   W_HANDLE hDriverConnection;
   W_ERROR nError;

   /* Check the parameters */
   if ( phDriverConnection == null )
   {
      PDebugError("P156933CreateConnection: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the 15693-3 buffer */
   p15P3DriverConnection = (tP15P3DriverConnection*)CMemoryAlloc( sizeof(tP15P3DriverConnection) );
   if ( p15P3DriverConnection == null )
   {
      PDebugError("P156933CreateConnection: p15P3DriverConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(p15P3DriverConnection, 0, sizeof(tP15P3DriverConnection));

   /* Register the 156933 connection structure */
   if ( ( nError = PHandleRegister(
                        pContext,
                        p15P3DriverConnection,
                        P_HANDLE_TYPE_15693_3_DRIVER_CONNECTION,
                        &hDriverConnection) ) != W_SUCCESS )
   {
      PDebugError("P156933CreateConnection: error on PHandleRegister");
      CMemoryFree(p15P3DriverConnection);
      return nError;
   }

   /* Store the connection information */
   p15P3DriverConnection->nServiceIdentifier = nServiceIdentifier;
   p15P3DriverConnection->nTimeout = P_15693_DEFAULT_TIMEOUT;
   *phDriverConnection = hDriverConnection;

   return W_SUCCESS;
}

/** @see tPReaderDriverSetDetectionConfiguration() */
static W_ERROR static_P15P3DriverSetDetectionConfiguration(
              tContext* pContext,
              uint8_t* pCommandBuffer,
              uint32_t* pnCommandBufferLength,
              const uint8_t* pDetectionConfigurationBuffer,
              uint32_t nDetectionConfigurationLength )
{
   const tP15P3DetectionConfiguration *p15P3DetectionConfiguration = (const tP15P3DetectionConfiguration*)pDetectionConfigurationBuffer;
   uint32_t nIndex = 0;
   static const uint8_t aCommandBuffer[] = P_15693_READER_CONFIG_PARAMETERS;

   if((pCommandBuffer == null)
   || (pnCommandBufferLength == null))
   {
      PDebugError("static_P15P3DriverSetDetectionConfiguration: bad Parameters");
      return W_ERROR_BAD_PARAMETER;
   }
   if((pDetectionConfigurationBuffer == null)
   || (nDetectionConfigurationLength == 0))
   {
      PDebugWarning("static_P15P3DriverSetDetectionConfiguration: set default parameters");
      CMemoryCopy(pCommandBuffer,
                  aCommandBuffer,
                  sizeof(aCommandBuffer));
      *pnCommandBufferLength = sizeof(aCommandBuffer);
      return W_SUCCESS;
   }
   else if(nDetectionConfigurationLength != sizeof(tP15P3DetectionConfiguration))
   {
      PDebugError("static_P15P3DriverSetDetectionConfiguration: bad nDetectionConfigurationLength (0x%x", nDetectionConfigurationLength);
      *pnCommandBufferLength = 0x00;
      return W_ERROR_BAD_PARAMETER;
   }
   pCommandBuffer[nIndex++] = p15P3DetectionConfiguration->nAFI;
   *pnCommandBufferLength = nIndex;
   return W_SUCCESS;
}

/** @see tPReaderDriverParseDetectionMessage() */
static W_ERROR static_P15P3DriverParseDetectionMessage(
               tContext* pContext,
               const uint8_t* pBuffer,
               uint32_t nLength,
               tPReaderDriverCardInfo* pCardInfo )
{
   uint32_t nIndex = 0;
   W_ERROR nError = W_SUCCESS;
   uint32_t i;

   PDebugTrace("static_P15P3DriverParseDetectionMessage()");

   if(pCardInfo->nProtocolBF != W_NFCC_PROTOCOL_READER_ISO_15693_3)
   {
      PDebugError("static_P15P3DriverParseDetectionMessage: protocol error");
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }
   /*skip flag byte and DSFID byte*/
   nIndex +=2;
   pCardInfo->nUIDLength = P_15693_UID_MAX_LENGTH;
   for (i=0;i<P_15693_UID_MAX_LENGTH;i++)
   {
      pCardInfo->aUID[i] = pBuffer[nIndex + P_15693_UID_MAX_LENGTH - i];
   }

return_function:

   if(nError == W_SUCCESS)
   {
      PDebugTrace("static_P15P3DriverParseDetectionMessage: UID = ");
      PDebugTraceBuffer(pCardInfo->aUID, pCardInfo->nUIDLength);
      /*reset AFI: not carried in the message*/
      pCardInfo->nAFI = 0x00;
   }
   else
   {
      PDebugTrace("static_P15P3DriverParseDetectionMessage: error %s", PUtilTraceError(nError));
   }

   return nError;
}

/** @see tPReaderDriverCheckCardMatchConfiguration() */
static bool_t static_P15P3DriverCheckCardMatchConfiguration(
               tContext* pContext,
               uint32_t nProtocolBF,
               const uint8_t* pDetectionConfigurationBuffer,
               uint32_t nDetectionConfigurationLength,
               tPReaderDriverCardInfo* pCardInfo)
{
   return W_TRUE;
}

/* See Header file */
void P15P3DriverExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tP15P3DriverExchangeDataCompleted* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength )
{
   tP15P3DriverConnection* p15P3DriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );

   nError = PReaderDriverGetConnectionObject(
      pContext, hConnection, P_HANDLE_TYPE_15693_3_DRIVER_CONNECTION, (void**)&p15P3DriverConnection);
   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      p15P3DriverConnection->pDriverCC = pDriverCC;
      /* Store the callback buffer */
      p15P3DriverConnection->pCardToReaderBuffer   = pCardToReaderBuffer;

      /* Check the parameters */
      if (  (  ( pReaderToCardBuffer == null )
            && ( nReaderToCardBufferLength != 0 ) )
         || (  ( pReaderToCardBuffer != null )
            && ( nReaderToCardBufferLength == 0 ) )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) ) )
      {
         PDebugError("P15P3DriverExchangeData: W_ERROR_BAD_PARAMETER");
         /* Send the error */
         PDFCDriverPostCC3(
            pDriverCC,
            0,
            W_ERROR_BAD_PARAMETER );
         return;
      }


      /* Increment the ref count, will be decreased in static_P15P3DriverExchangeDataCompleted */

      PDebugTrace("P15P3DriverExchangeData : PHandleIncrementReferenceCount");
      PHandleIncrementReferenceCount(p15P3DriverConnection);

      /* Set the connection information */
      p15P3DriverConnection->nReaderToCardBufferLength15693      = nReaderToCardBufferLength;
      p15P3DriverConnection->nCardToReaderBufferMaxLength15693   = nCardToReaderBufferMaxLength;
      p15P3DriverConnection->nAttempt                           = 0;

      /* Prepare the command */
      if ( nReaderToCardBufferLength == 0 )
      {
         p15P3DriverConnection->aReaderToCardBuffer[0] = (uint8_t)(p15P3DriverConnection->nTimeout) | NAL_TIMEOUT_READER_XCHG_DATA_ENABLE | NAL_ISO_15_3_SEND_EOF_ONLY;
      }
      else
      {
         p15P3DriverConnection->aReaderToCardBuffer[0] = (uint8_t)(p15P3DriverConnection->nTimeout) | NAL_TIMEOUT_READER_XCHG_DATA_ENABLE;
         CMemoryCopy(
            &p15P3DriverConnection->aReaderToCardBuffer[1],
            pReaderToCardBuffer,
            p15P3DriverConnection->nReaderToCardBufferLength15693 );
      }
      /* Check if the timeout is used */
      if ( p15P3DriverConnection->nTimeout == 0x00 )
      {
         p15P3DriverConnection->bIsTimeoutUsed = W_FALSE;
      }
      else
      {
         p15P3DriverConnection->bIsTimeoutUsed = W_TRUE;
      }


      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         NAL_SERVICE_READER_15_3,
         &p15P3DriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         p15P3DriverConnection->aReaderToCardBuffer,
         p15P3DriverConnection->nReaderToCardBufferLength15693 + 1,
         p15P3DriverConnection->pCardToReaderBuffer,
         p15P3DriverConnection->nCardToReaderBufferMaxLength15693,
         static_P15P3DriverExchangeDataCompleted,
         p15P3DriverConnection );
   }
   else
   {
      PDebugError("P15P3DriverExchangeData: could not get p15P3DriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }
}

/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sP15P3ReaderProtocoConstant = {
   W_NFCC_PROTOCOL_READER_ISO_15693_3,
   NAL_SERVICE_READER_15_3,
   static_P15P3DriverCreateConnection,
   static_P15P3DriverSetDetectionConfiguration,
   static_P15P3DriverParseDetectionMessage,
   static_P15P3DriverCheckCardMatchConfiguration };

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* ISO 15693 maximum sector number */
#define P_15693_SECTOR_NUMBER_MAX                256
#define P_15693_SECTOR_NUMBER_MIN                8
#define P_15693_SECTOR_SIZE_MAX                  32
#define P_15693_SECTOR_SIZE_MIN                  4
#define P_15693_WRITE_INDEX_START                5

/* ISO 15693 UID information */
#define P_15693_UID_HEADER                       0xE0
#define P_15693_UID_MANUFACTURER_ST              0x02
#define P_15693_UID_MANUFACTURER_PHILIPS         0x04
#define P_15693_UID_MANUFACTURER_TI              0x07
#define P_15693_UID_MANUFACTURER_INSIDE          0x12
#define P_15693_UID_MANUFACTURER_UNKNOWN         0xFF

/* Philips block size and number */
#define P_15693_PHILIPS_SL2ICS20                 0x01
#define P_15693_PHILIPS_SL2ICS20_SECTOR_NUMBER   28
#define P_15693_PHILIPS_SL2ICS20_SECTOR_SIZE     4

#define P_15693_PHILIPS_SL2ICS53                 0x02
#define P_15693_PHILIPS_SL2ICS53_SECTOR_NUMBER   40
#define P_15693_PHILIPS_SL2ICS53_SECTOR_SIZE     4

#define P_15693_PHILIPS_SL2ICS50                 0x03
#define P_15693_PHILIPS_SL2ICS50_SECTOR_NUMBER   8
#define P_15693_PHILIPS_SL2ICS50_SECTOR_SIZE     4
#define P_15693_PHILIPS_SL2ICS50_FAKE_SECTOR_NUMBER   48 /* Returned by Get System Info */

/* ST block size and number */
#define P_15693_ST_LRI512_SECTOR_NUMBER          16
#define P_15693_ST_LRI2K_SECTOR_NUMBER           64

/* TI block size and number */
#define P_15693_TI_TAGIT_SECTOR_NUMBER           64 /* With the Tag It tag, it is compulsary to add the option flag */
                                                   /* in the write, lock, write/lock AFI, write/lock DSFID */
                                                   /* commands code flag (P_15693_CODE_FLAG_OPTION) */
#define P_15693_TI_TAGIT_SECTOR_SIZE             4

/* Inside block size and number */
#define P_15693_INSIDE_SECTOR_SIZE               8
#define P_15693_INSIDE_SECTOR_NUMBER             32

/* ISO 15693 read or write actions */
#define P_15693_ACTION_RESET                     0x00
#define P_15693_ACTION_READ                      0x01
#define P_15693_ACTION_UPDATE                    0x02
#define P_15693_ACTION_UPDATE_FIRST_BLOCK        0x03
#define P_15693_ACTION_UPDATE_LAST_BLOCK         0x04

/* Command types */
#define P_15693_COMMAND_INIT                     0x00
#define P_15693_COMMAND_GET_SYSTEMINFO           0x01
#define P_15693_COMMAND_GET_SECURITYINFO         0x02
#define P_15693_COMMAND_READ                     0x03
#define P_15693_COMMAND_WRITE                    0x04
#define P_15693_COMMAND_LOCK                     0x05
#define P_15693_COMMAND_ATTRIBUTE                0x06

/* Command code */
#define P_15693_CODE_FLAG                        0x02
                                                /* 0 0 0 0: RFU, option flag, address flag, select flag */
                                                /* 0 0 1 0: no protocol format extension, not inventory, low data rate, single sub-carrier */
#define P_15693_CODE_FLAG_OPTION                 0x42
                                                /* 0 1 0 0: RFU, option flag, address flag, select flag */
                                                /* 0 0 1 0: no protocol format extension, not inventory, low data rate, single sub-carrier */
#define P_15693_CODE_GET_SYSTEMINFO              0x2B
#define P_15693_CODE_GET_SECURITYINFO            0x2C
#define P_15693_CODE_READ_SINGLE                 0x20
#define P_15693_CODE_WRITE_SINGLE                0x21
#define P_15693_CODE_LOCK                        0x22
#define P_15693_CODE_READ_MULTIPLE               0x23
#define P_15693_CODE_WRITE_MULTIPLE              0x24
#define P_15693_CODE_WRITE_AFI                   0x27
#define P_15693_CODE_LOCK_AFI                    0x28
#define P_15693_CODE_WRITE_DSFID                 0x29
#define P_15693_CODE_LOCK_DSFID                  0x2A

/*cache Connection defines*/
#define P_15693_BLOCK_NUMBER_MAX                 P_15693_SECTOR_NUMBER_MAX
#define P_15693_IDENTIFIER_LEVEL                 ZERO_IDENTIFIER_LEVEL
#define P_15693_SECURITYINFO_INDEX               0x00
#define P_15693_DATA_INDEX                       0x01
#define P_15693_SECURITYINFO_LENGTH              P_15693_SECTOR_NUMBER_MAX
#define P_15693_DATA_CACHE_SIZE                  0x800
#define P_15693_CACHE_INFO_SIZE                  P_15693_BLOCK_NUMBER_MAX + 4
#define P_15693_CACHE_INFO_INDEX                 0x10

/* Declare an ISO 15693 structure for asynchronous mode */
typedef struct __tP15P3UserConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* hConnection handle */
   W_HANDLE                   hConnection;
   /* Driver connection handle */
   W_HANDLE                   hDriverConnection;
   /* Operation handle */
   W_HANDLE hCurrentOperation;

   /* Connection information */
   bool_t                     bIsEOFSent;
   bool_t                     bIsReadMultipleSupported;
   bool_t                     bIsWriteMultipleSupported;
   bool_t                     bIsAFISupported;
   bool_t                     bIsAFILocked;
   bool_t                     bIsDSFIDSupported;
   bool_t                     bIsDSFIDLocked;

   bool_t                     bIsSectorLocked[P_15693_SECTOR_NUMBER_MAX];
   uint8_t                    nAFI;
   uint8_t                    nDSFID;
   uint8_t                    nIC;

   uint8_t                    UID[8];
   uint8_t                    nProtocol;

   uint8_t                    nCommandType;
   uint8_t                    nActions;
   uint8_t                    nAFINew;
   uint8_t                    nDSFIDNew;

   uint8_t                    nCardManufacturer;
   uint8_t                    nCardSecondaryConnection;
   uint8_t                    nPendingCardSecondaryConnection;
   bool_t                     bIsTagSizeSetFromSystemInfo;
   uint16_t                   nSectorNumber;
   uint8_t                    nSectorSize;

   /* Command buffer */
   uint8_t                    aReaderToCardBuffer15693[P_15693_BUFFER_MAX_SIZE];
   /* Response buffer */
   uint8_t                    aCardToReaderBuffer15693[P_15693_BUFFER_MAX_SIZE];


   bool_t                     bIsSmartCacheInitialized;  /**< Smart cache initialized or not */
   tSmartCache                sSmartCache;               /**< Smart cache */
   tDFCCallbackContext        sCacheCallbackContext;     /**< Callback context used by smart cache operations */
   uint8_t *                  pSmartCacheBuffer;         /**< Current smart cache operation buffer */

   uint8_t                    nBlockStart;               /**< First block to be read / written */
   uint8_t                    nBlockEnd;                 /**< Last block of the read / written */

   bool_t                     bLockSectors;              /**< If set to W_TRUE, the blocks in the range nLockBlockStart - nBlockLockEnd will be locked */
   uint8_t                    nLockBlockStart;           /**< First block to be locked */
   uint8_t                    nLockBlockEnd;             /**< Last block to be locked */


   /* Callback context used by method called from user */
   tDFCCallbackContext        sCallbackContext;
   /* Callback context used by method called from another connection */
   tDFCCallbackContext        sCallbackContextInternal;

   /* Hold data of the queued operation which will be executed after the polling completion */
   struct __tQueuedOperation
   {
      /* Type of operation: Read, Lock, Write... */
      uint32_t             nType;
      /* Data */
      uint8_t*             pBuffer;
      uint32_t             nOffset;
      uint32_t             nLength;
      bool_t               bLockSectors;
      uint8_t              nActions;
      uint8_t              nAFI;
      uint8_t              nDSFID;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hCurrentOperation;
   } sQueuedOperation;

} tP15P3UserConnection;

/* Sends a command frame */
static void static_P15P3UserSendCommand(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            uint8_t nCommandType );

/* Destroy connection callback */
static uint32_t static_P15P3UserDestroyConnection(
            tContext* pContext,
            void* pObject );


/* Get properties connection callback */
static uint32_t static_P15P3UserGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get properties connection callback */
static bool_t static_P15P3UserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_P15P3UserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Get identifier length */
static uint32_t static_P15P3UserGetIdentifierLength(
            tContext* pContext,
            void* pObject);

/* Get identifier */
static void static_P15P3UserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer);

/* Exchange data */
static void static_P15ExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t *pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t *pCardToReaderBuffer,
            uint32_t nCardToReaderMaxMaxLength,
            W_HANDLE * phOperation);

/* Send polling command */
static void static_P15P3Poll(
      tContext * pContext,
      void* pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/* Execute the queued operation (after polling) */
static void static_P15ExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult);

/* Handle registry ISO 15693 type */
tHandleType g_s15P3UserConnection = {   static_P15P3UserDestroyConnection,
                                    null,
                                    static_P15P3UserGetPropertyNumber,
                                    static_P15P3UserGetProperties,
                                    static_P15P3UserCheckProperties,
                                    static_P15P3UserGetIdentifierLength,
                                    static_P15P3UserGetIdentifier,
                                    static_P15ExchangeData,
                                    static_P15P3Poll };

#define P_HANDLE_TYPE_15693_USER_CONNECTION (&g_s15P3UserConnection)

/**
 * @brief   Destroyes a ISO 15693 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_P15P3UserDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pObject;

   PDebugTrace("static_P15P3UserDestroyConnection");

   /* free the smart cache */
   PSmartCacheDestroyCache(pContext, & p15P3UserConnection->sSmartCache);

   /* The driver connection is closed by the reader registry parent object */
   /* p15P3UserConnection->hDriverConnection */

   /* Flush the function calls */
   PDFCFlushCall(&p15P3UserConnection->sCallbackContext);

   CMemoryFree( p15P3UserConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the ISO 15693 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/

static uint32_t static_P15P3UserGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pObject;

   if (p15P3UserConnection->nCardSecondaryConnection != 0)
   {
      return 3;
   }
   else
   {
      return 2;
   }
}

/**
 * @brief   Gets the ISO 15693 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_P15P3UserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pObject;

   PDebugTrace("static_P15P3UserGetProperties");

   if ( p15P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_P15P3UserGetProperties: no property");
      return W_FALSE;
   }

   if (p15P3UserConnection->nCardSecondaryConnection != 0)
   {
      * pPropertyArray++ = p15P3UserConnection->nCardSecondaryConnection;
   }

   * pPropertyArray++  = W_PROP_ISO_15693_3;
   * pPropertyArray++  = W_PROP_ISO_15693_2;

   return W_TRUE;
}

/**
 * @brief   Checkes the ISO 15693 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_P15P3UserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pObject;

   PDebugTrace(
      "static_P15P3UserCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( p15P3UserConnection->nProtocol != P_PROTOCOL_STILL_UNKNOWN )
   {
      if((nPropertyValue == W_PROP_ISO_15693_3)
      || (nPropertyValue == W_PROP_ISO_15693_2)
      || (nPropertyValue == p15P3UserConnection->nCardSecondaryConnection))
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
      PDebugError("static_P15P3UserCheckProperties: no property");
      return W_FALSE;
   }
}

/**
 * @brief   Manage completion of a user command. Cleanup and send result.
 *          See tWBasicGenericCallbackFunction
 **/
static void static_P15P3UserCommandCompleted(
      tContext * pContext,
      void     * pCallbackParameter,
      W_ERROR    nError)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*) pCallbackParameter;

   if (p15P3UserConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, p15P3UserConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_P15P3UserCommandCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, p15P3UserConnection->hCurrentOperation);
      PHandleClose(pContext, p15P3UserConnection->hCurrentOperation);
      p15P3UserConnection->hCurrentOperation = W_NULL_HANDLE;
   }

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, p15P3UserConnection->hConnection);

   /* Send the error */
   PDFCPostContext2(&p15P3UserConnection->sCallbackContext, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p15P3UserConnection);
}

/**
 * @brief   Parses the UID.
 *
 * @param[in]  p15P3UserConnection  The ISO 15693 connection structure.
 **/
static void static_P15P3UserParseUID(
            tP15P3UserConnection* p15P3UserConnection )
{
   /* Check the UID header */
   if ( p15P3UserConnection->UID[0] == P_15693_UID_HEADER )
   {
      /* Check the manufacturer code */
      switch ( p15P3UserConnection->UID[1] )
      {
         case P_15693_UID_MANUFACTURER_ST:
            PDebugTrace("static_P15P3UserParseUID: ST");
            /* Set the default values */
            p15P3UserConnection->nCardManufacturer = P_15693_UID_MANUFACTURER_ST;
            p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;
            p15P3UserConnection->bIsWriteMultipleSupported = W_FALSE;
            break;

         case P_15693_UID_MANUFACTURER_PHILIPS:
            PDebugTrace("static_P15P3UserParseUID: Philips");
            /* Set the default values */
            p15P3UserConnection->nCardManufacturer = P_15693_UID_MANUFACTURER_PHILIPS;
            if ( p15P3UserConnection->UID[2] == P_15693_PHILIPS_SL2ICS20 )
            {
               p15P3UserConnection->bIsReadMultipleSupported = W_TRUE;
            }
            else
            {
               p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;
            }
            p15P3UserConnection->bIsWriteMultipleSupported = W_FALSE;
            break;

         case P_15693_UID_MANUFACTURER_TI:
            PDebugTrace("static_P15P3UserParseUID: TI");
            /* Set the default values */
            p15P3UserConnection->nCardManufacturer = P_15693_UID_MANUFACTURER_TI;
            p15P3UserConnection->bIsReadMultipleSupported = W_TRUE;
            p15P3UserConnection->bIsWriteMultipleSupported = W_FALSE;
            break;

         case P_15693_UID_MANUFACTURER_INSIDE:
            PDebugTrace("static_P15P3UserParseUID: Inside Picotag/iClass 2K");
            /* Set the default values */
            p15P3UserConnection->nCardManufacturer = P_15693_UID_MANUFACTURER_INSIDE;
            p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;
            p15P3UserConnection->bIsWriteMultipleSupported = W_FALSE;
            break;

         default:
            PDebugWarning("static_P15P3UserParseUID: unknown manufacturer code 0x%02X", p15P3UserConnection->UID[1]);
            /* Set the default values */
            p15P3UserConnection->nCardManufacturer = P_15693_UID_MANUFACTURER_UNKNOWN;
            p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;
            p15P3UserConnection->bIsWriteMultipleSupported = W_FALSE;
            break;
      }
   }
}

/**
 * @brief   Parses the Type 15693 card response at start up..
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p15P3UserConnection  The ISO 15693 connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P15P3UserParseInventory(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   uint32_t i;
   uint32_t nIndex = 0x00;

   /* skip flags*/
   nIndex++;
   /* DSFID */
   p15P3UserConnection->nDSFID = pBuffer[nIndex++];
   PDebugTrace(
      "static_P15P3UserParseInventory: nDSFID 0x%02X",
      p15P3UserConnection->nDSFID );
   /* UID */
   for (i=0;i<P_15693_UID_MAX_LENGTH;i++)
   {
      p15P3UserConnection->UID[i] = pBuffer[nIndex + P_15693_UID_MAX_LENGTH - 1 - i];
   }
   PDebugTrace("static_P15P3UserParseInventory: UID");
   PDebugTraceBuffer( p15P3UserConnection->UID, P_15693_UID_MAX_LENGTH );

   /* Parse the UID */
   static_P15P3UserParseUID( p15P3UserConnection );

   return W_SUCCESS;
}

/**
 * @brief   Parses the response to a get system info command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p15P3UserConnection  The ISO 15693 connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P15P3UserParseSystemInfo(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            uint8_t* pBuffer,
            uint32_t nLength )
{
   uint8_t i = 0x00;
   uint8_t nIndex = 0x0A;

   /* UID */
   for (i=0;i<P_15693_UID_MAX_LENGTH;i++)
   {
      p15P3UserConnection->UID[i] = pBuffer[2 + P_15693_UID_MAX_LENGTH - 1 - i];
   }
   PDebugTrace("static_P15P3UserParseSystemInfo: UID");
   PDebugTraceBuffer( p15P3UserConnection->UID, P_15693_UID_MAX_LENGTH );

   /* Parse the UID */
   static_P15P3UserParseUID( p15P3UserConnection );

   /* DSFID */
   if ( pBuffer[1] & 0x01 )
   {
      PDebugTrace("static_P15P3UserParseSystemInfo: DSFID supported");
      p15P3UserConnection->bIsDSFIDSupported = W_TRUE;
      p15P3UserConnection->nDSFID = pBuffer[nIndex ++];
      PDebugTrace(
         "static_P15P3UserParseSystemInfo: nDSFID 0x%02X",
         p15P3UserConnection->nDSFID );
   }
   else
   {
      PDebugTrace("static_P15P3UserParseSystemInfo: DSFID not supported");
      p15P3UserConnection->bIsDSFIDSupported = W_FALSE;
      p15P3UserConnection->bIsDSFIDLocked = W_TRUE;
      p15P3UserConnection->nDSFID = 0;
   }

   /* AFI */
   if ( pBuffer[1] & 0x02 )
   {
      PDebugTrace("static_P15P3UserParseSystemInfo: AFI supported");
      p15P3UserConnection->bIsAFISupported = W_TRUE;
      p15P3UserConnection->nAFI = pBuffer[nIndex ++];
      PDebugTrace(
         "static_P15P3UserParseSystemInfo: nAFI 0x%02X",
         p15P3UserConnection->nAFI );
   }
   else
   {
      PDebugTrace("static_P15P3UserParseSystemInfo: AFI not supported");
      p15P3UserConnection->bIsAFISupported = W_FALSE;
      p15P3UserConnection->bIsAFILocked = W_TRUE;
      p15P3UserConnection->nAFI = 0x00;
   }

   /* Memory size */
   if ( pBuffer[1] & 0x04 )
   {
      p15P3UserConnection->bIsTagSizeSetFromSystemInfo = W_TRUE;
      p15P3UserConnection->nSectorNumber = pBuffer[nIndex ++] + 1;
      p15P3UserConnection->nSectorSize   = (pBuffer[nIndex ++] & 0x1F) + 1;
      PDebugTrace(
         "static_P15P3UserParseSystemInfo: nSectorNumber %d",
         p15P3UserConnection->nSectorNumber );
      PDebugTrace(
         "static_P15P3UserParseSystemInfo: nSectorSize %d bytes",
         p15P3UserConnection->nSectorSize );

      /* Check if it is a ST LRI 2K tag*/
      if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_ST )
      {
         /* Check if it is a 2K card */
         if ( p15P3UserConnection->nSectorNumber == P_15693_ST_LRI2K_SECTOR_NUMBER )
         {
            p15P3UserConnection->bIsReadMultipleSupported = W_TRUE;
            p15P3UserConnection->nPendingCardSecondaryConnection = W_PROP_ST_LRI_2K;
         }
      }

      /* Check if it is a ICODE TAG */
      if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_PHILIPS )
      {
         /* Check returned values */
         if (p15P3UserConnection->nSectorSize == P_15693_PHILIPS_SL2ICS20_SECTOR_SIZE)
         {
            if ((p15P3UserConnection->nSectorNumber == P_15693_PHILIPS_SL2ICS20_SECTOR_NUMBER) ||
                (p15P3UserConnection->nSectorNumber == P_15693_PHILIPS_SL2ICS53_SECTOR_NUMBER))
            {
               p15P3UserConnection->nPendingCardSecondaryConnection = W_PROP_NXP_ICODE;
            }
            else if ((p15P3UserConnection->nSectorNumber == P_15693_PHILIPS_SL2ICS50_FAKE_SECTOR_NUMBER) &&
                     (p15P3UserConnection->UID[2] == P_15693_PHILIPS_SL2ICS50))
            {
               p15P3UserConnection->nPendingCardSecondaryConnection = W_PROP_NXP_ICODE;
               /* Replace fake value */
               PDebugTrace("static_P15P3UserSendCommandCompleted: SL2ICS50 - replace fake sector number with real sector number");
               p15P3UserConnection->nSectorNumber = P_15693_PHILIPS_SL2ICS50_SECTOR_NUMBER;
               PDebugTrace("static_P15P3UserParseSystemInfo: nSectorNumber %d", p15P3UserConnection->nSectorNumber);
            }
         }
      }

      /* Check if it is a TAGIT */
      if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI)
      {
         if ((p15P3UserConnection->nSectorNumber == P_15693_TI_TAGIT_SECTOR_NUMBER) &&
             (p15P3UserConnection->nSectorSize == P_15693_TI_TAGIT_SECTOR_SIZE))
         {
            p15P3UserConnection->nPendingCardSecondaryConnection = W_PROP_TI_TAGIT;
         }
      }
   }
   else
   {
      PDebugWarning("static_P15P3UserParseSystemInfo: nSectorSize & nSectorNumber not available");
   }

   /* IC reference */
   if ( pBuffer[1] & 0x08 )
   {
      p15P3UserConnection->nIC = pBuffer[nIndex ++];
      PDebugTrace(
         "static_P15P3UserParseSystemInfo: IC reference 0x%02X",
         p15P3UserConnection->nIC );
   }

   /* Check if the data is consistent */
   if ( nIndex != nLength )
   {
      PDebugError(
         "static_P15P3UserParseSystemInfo: nIndex %d != nLength %d",
         nIndex,
         nLength );
   }

   return W_SUCCESS;
}


/**
 * @brief   Parses the response to a get security info command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p15P3UserConnection  The ISO 15693 connection structure.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P15P3UserParseSecurityInfo(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            uint8_t* pBuffer,
            uint32_t nLength )
{
   uint8_t nIndex;

   /* Check if the data is consistent */
   if ( p15P3UserConnection->nSectorNumber != ( nLength - 1 ) )
   {
      PDebugWarning(
         "static_P15P3UserParseSecurityInfo: nSectorNumber %d != nLength %d",
         p15P3UserConnection->nSectorNumber,
         nLength - 1 );
   }


   for ( nIndex=0; nIndex<nLength-1; nIndex++)
   {
      if ( pBuffer[nIndex+1] & 0x01 )
      {
         p15P3UserConnection->bIsSectorLocked[nIndex] = W_TRUE;
      }
      else
      {
         p15P3UserConnection->bIsSectorLocked[nIndex] = W_FALSE;
      }
   }

   return W_SUCCESS;
}

static void static_P15P3DriverExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tP15P3DriverExchangeDataCompleted* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;

   P15P3DriverExchangeData(pContext, hConnection,
            pCallback, pCallbackParameter,
            pReaderToCardBuffer, nReaderToCardBufferLength,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength);


   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, 0, nError);
   }
}

/* See tWBasicGenericCallbackFunction */
static void static_P15P3SendResultInternal(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pCallbackParameter;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_P15P3UserSendCommandCompleted: error %s", PUtilTraceError(nError));
   }

   /* Post result */
   PDFCPostContext2(&p15P3UserConnection->sCallbackContextInternal, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p15P3UserConnection);
}

/* See tWBasicGenericDataCallbackFunction */
static void static_P15P3UserSendCommandCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError )
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pCallbackParameter;
   bool_t bIsCommandSupported = W_TRUE;
   uint8_t* pCardToReaderBuffer = p15P3UserConnection->aCardToReaderBuffer15693;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_P15P3UserSendCommandCompleted: receiving %s", PUtilTraceError(nError));
   }

   /* Check if the operation has been cancelled by the user */
   if (p15P3UserConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check the operation state */
      if (PBasicGetOperationState(pContext, p15P3UserConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED)
      {
         PDebugError("static_P15P3UserSendCommandCompleted: command cancelled");
         nError = W_ERROR_CANCEL;
         goto return_error;
      }
   }

   if (nError == W_ERROR_TIMEOUT)
   {
      bIsCommandSupported = W_FALSE;
   }
   else if (nError != W_SUCCESS)
   {
      goto return_error;
   }

   /* Check the error code */
   if ((nLength >= 2 ) && ( pCardToReaderBuffer[0] & 0x01 ))
   {
      PDebugError(
         "static_P15P3UserSendCommandCompleted: error code 0x%02X",
         pCardToReaderBuffer[1] );
      switch ( pCardToReaderBuffer[1] )
      {
         /* Command not supported */
         case 0x01:
         /* Command not recognized */
         case 0x02:
         /* Command option not supported */
         case 0x03:
            bIsCommandSupported = W_FALSE;
            break;

         /* Block already locked */
         case 0x11:
            break;

         /* Block locked */
         case 0x12:
            nError = W_ERROR_ITEM_LOCKED;
            goto return_error;

         /* Wrong block */
         case 0x10:
         /* Block not successfully programmed */
         case 0x13:
         /* Block not successfully locked */
         case 0x14:
         /* Other error */
         default:
            nError = W_ERROR_RF_COMMUNICATION;
            goto return_error;
      }
   }

   /* Check the command type */
   switch ( p15P3UserConnection->nCommandType )
   {
      case P_15693_COMMAND_GET_SYSTEMINFO:

         PDebugTrace("static_P15P3UserSendCommandCompleted: P_15693_COMMAND_GET_SYSTEMINFO");

         /* If the command is supported */
         if ( bIsCommandSupported != W_FALSE )
         {
            /* Parse the card answer */
            if ( ( nError = static_P15P3UserParseSystemInfo(
                              pContext,
                              p15P3UserConnection,
                              pCardToReaderBuffer,
                              nLength ) ) != W_SUCCESS )
            {
               PDebugError("static_P15P3UserSendCommandCompleted: wrong response to system info 0x%08X", nError);
               goto return_error;
            }

            /* Send the command */
            static_P15P3UserSendCommand( pContext, p15P3UserConnection, P_15693_COMMAND_GET_SECURITYINFO );
            return;
         }
         else
         {
              /* Set the default value */
            p15P3UserConnection->nSectorSize   = P_15693_SECTOR_SIZE_MIN;
            p15P3UserConnection->nSectorNumber = P_15693_SECTOR_NUMBER_MIN;
            p15P3UserConnection->bIsDSFIDSupported = W_FALSE;
            p15P3UserConnection->bIsDSFIDLocked = W_TRUE;
            p15P3UserConnection->nDSFID = 0x00;
            p15P3UserConnection->bIsAFISupported = W_FALSE;
            p15P3UserConnection->bIsAFILocked = W_TRUE;
            p15P3UserConnection->nAFI = 0x00;
            p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;
            p15P3UserConnection->bIsWriteMultipleSupported = W_FALSE;
            p15P3UserConnection->nIC = 0x00;

            /* Check the UID header */
            if ( p15P3UserConnection->UID[0] == P_15693_UID_HEADER )
            {
               /* Check the manufacturer code */
               switch ( p15P3UserConnection->UID[1] )
               {
                  case P_15693_UID_MANUFACTURER_ST:

                     PDebugTrace("static_P15P3UserSendCommandCompleted: ST");
                     /* Set the default values */
                     p15P3UserConnection->nSectorNumber = P_15693_ST_LRI512_SECTOR_NUMBER;
                     p15P3UserConnection->nPendingCardSecondaryConnection = W_PROP_ST_LRI_512;
                     p15P3UserConnection->bIsAFISupported = W_TRUE;
                     p15P3UserConnection->bIsAFILocked = W_FALSE;
                     p15P3UserConnection->nAFI = 0;
                     p15P3UserConnection->bIsReadMultipleSupported = W_TRUE;
                     break;

                  case P_15693_UID_MANUFACTURER_PHILIPS:

                     PDebugTrace("static_P15P3UserSendCommandCompleted: Philips");

                     if ( p15P3UserConnection->UID[2] == P_15693_PHILIPS_SL2ICS20 )
                     {
                        PDebugTrace("static_P15P3UserSendCommandCompleted: SL2ICS20");
                        p15P3UserConnection->nSectorSize   = P_15693_PHILIPS_SL2ICS20_SECTOR_SIZE;
                        p15P3UserConnection->nSectorNumber = P_15693_PHILIPS_SL2ICS20_SECTOR_NUMBER;
                        p15P3UserConnection->nPendingCardSecondaryConnection  = W_PROP_NXP_ICODE;
                        p15P3UserConnection->bIsReadMultipleSupported = W_TRUE;
                     }
                     else if ( p15P3UserConnection->UID[2] == P_15693_PHILIPS_SL2ICS53 )
                     {
                        PDebugTrace("static_P15P3UserSendCommandCompleted: SL2ICS53");
                        p15P3UserConnection->nSectorSize   = P_15693_PHILIPS_SL2ICS53_SECTOR_SIZE;
                        p15P3UserConnection->nSectorNumber = P_15693_PHILIPS_SL2ICS53_SECTOR_NUMBER;
                        p15P3UserConnection->nPendingCardSecondaryConnection  = W_PROP_NXP_ICODE;
                        p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;
                     }
                     else if ( p15P3UserConnection->UID[2] == P_15693_PHILIPS_SL2ICS50 )
                     {
                        PDebugTrace("static_P15P3UserSendCommandCompleted: SL2ICS50");
                        p15P3UserConnection->nSectorSize   = P_15693_PHILIPS_SL2ICS50_SECTOR_SIZE;
                        p15P3UserConnection->nSectorNumber = P_15693_PHILIPS_SL2ICS50_SECTOR_NUMBER;
                        p15P3UserConnection->nPendingCardSecondaryConnection  = W_PROP_NXP_ICODE;
                        p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;
                     }
                     else
                     {
                        PDebugWarning("static_P15P3UserSendCommandCompleted: unknown type");
                     }
                     break;

                  case P_15693_UID_MANUFACTURER_TI:

                     PDebugTrace("static_P15P3UserSendCommandCompleted: TI");
                     /* Set the default values */
                     p15P3UserConnection->nSectorNumber = P_15693_TI_TAGIT_SECTOR_NUMBER;
                     p15P3UserConnection->nPendingCardSecondaryConnection = W_PROP_TI_TAGIT;
                     p15P3UserConnection->bIsReadMultipleSupported = W_TRUE;
                     break;

                  case P_15693_UID_MANUFACTURER_INSIDE:

                     PDebugTrace("static_P15P3UserSendCommandCompleted: Inside Picotag/iClass 2K");

                     /* Set the default values */
                     p15P3UserConnection->nSectorSize   = P_15693_INSIDE_SECTOR_SIZE;
                     p15P3UserConnection->nSectorNumber = P_15693_INSIDE_SECTOR_NUMBER;
                     p15P3UserConnection->bIsReadMultipleSupported = W_TRUE;
                     break;

                  default:
                     PDebugWarning("static_P15P3UserSendCommandCompleted: unknown manufacturer code 0x%02X", p15P3UserConnection->UID[1] );
                     break;
               }
            }
            else
            {
               PDebugWarning("static_P15P3UserSendCommandCompleted: incorrect UID");
            }

            PDebugTrace("static_P15P3UserSendCommandCompleted: default nSectorNumber %d", p15P3UserConnection->nSectorNumber );
            PDebugTrace("static_P15P3UserSendCommandCompleted: default nSectorSize %d bytes",p15P3UserConnection->nSectorSize );

            /* Send the result */
            nError = W_SUCCESS;
            goto send_result;
         }
         break;

      case P_15693_COMMAND_GET_SECURITYINFO:

         PDebugTrace("static_P15P3UserSendCommandCompleted: P_15693_COMMAND_GET_SECURITYINFO");

         /* If the command is not supported */
         if ( bIsCommandSupported != W_FALSE )
         {
            /* no need to cache the result, this operation is done only once */

            /* Parse the card answer */
            if ( ( nError = static_P15P3UserParseSecurityInfo(pContext, p15P3UserConnection, pCardToReaderBuffer, nLength )) != W_SUCCESS )
            {
               PDebugError("static_P15P3UserSendCommandCompleted: wrong response to security info 0x%08X", nError );
               goto return_error;
            }
         }

         /* Send the result */
         nError = W_SUCCESS;
         goto send_result;

      case P_15693_COMMAND_READ:

         /* the read operation has been completed */

         PDebugTrace("static_P15P3UserSendCommandCompleted : P_15693_COMMAND_READ");

         if ( bIsCommandSupported != W_FALSE )
         {
#if 0       /* @todo ??? */

            if (  ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_UNKNOWN )
                  && ( p15P3UserConnection->bIsReadMultipleSupported == W_FALSE )
                  && ( p15P3UserConnection->nSectorSize != nLength - 1 ) )
               {
                  p15P3UserConnection->nSectorSize = (uint8_t)nLength - 1;
               }
#endif /* 0 */

            PDebugTrace("static_P15P3UserSendCommandCompleted : bIsCommandSupported != W_FALSE");

            /* Copy the response in the buffer provided by smart cache */
            CMemoryCopy(p15P3UserConnection->pSmartCacheBuffer, &pCardToReaderBuffer[1], nLength-1);
            nError = W_SUCCESS;
         }
         else
         {
            /* The current READ or READ_MULTIPLE operation is not supported */

            PDebugTrace("static_P15P3UserSendCommandCompleted : bIsCommandSupported == W_FALSE");

            if (p15P3UserConnection->bIsReadMultipleSupported != W_FALSE)
            {
               PDebugError("static_P15P3UserSendCommandCompleted: ISO READ_MULTIPLE command not supported");

               /* the READ_MULTIPLE operation is not supported, retry using a READ operation */
               p15P3UserConnection->bIsReadMultipleSupported = W_FALSE;

               /* Restart a READ operation (not using MULTIPLE READ) */
               static_P15P3UserSendCommand( pContext, p15P3UserConnection, P_15693_COMMAND_READ);
               return;

            }
            else
            {
               PDebugError("static_P15P3UserSendCommandCompleted: neither ISO READ nor READ_MULTIPLE command is supported !");
               nError = W_ERROR_TIMEOUT;
            }
         }

         /* Post the result */
         goto send_result;

      case P_15693_COMMAND_WRITE:

         PDebugTrace("static_P15P3UserSendCommandCompleted : P_15693_COMMAND_WRITE");

         /* If the command is not supported */
         if ( bIsCommandSupported == W_FALSE )
         {
            /* Prepare the command */
            if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
            {
               if ( p15P3UserConnection->bIsEOFSent == W_FALSE )
               {
                  /* Set the flag */
                  p15P3UserConnection->bIsEOFSent = W_TRUE;

                  /* Send the EOF */
                  static_P15P3DriverExchangeData(
                     pContext,
                     p15P3UserConnection->hDriverConnection,
                     static_P15P3UserSendCommandCompleted,
                     p15P3UserConnection,
                     null,
                     0,
                     p15P3UserConnection->aCardToReaderBuffer15693,
                     P_15693_BUFFER_MAX_SIZE );

                  return;
               }
               else
               {
                  nError = W_ERROR_TIMEOUT;
                  goto return_error;
               }
            }

            /* The current WRITE or WRITE_MULTIPLE operation is not supported */

            if (p15P3UserConnection->bIsReadMultipleSupported != W_FALSE)
            {
               PDebugError("static_P15P3UserSendCommandCompleted: ISO WRITE_MULTIPLE command not supported");

               /* the WRITE_MULTIPLE operation is not supported, retry using a WRITE operation */
               p15P3UserConnection->bIsWriteMultipleSupported  = W_FALSE;

               /* Restart a WRITE operation (not using MULTIPLE WRITE) */
               static_P15P3UserSendCommand( pContext, p15P3UserConnection, P_15693_COMMAND_WRITE);
               return;
            }
            else
            {
               PDebugError("static_P15P3UserSendCommandCompleted: neither ISO WRITE nor WRITE_MULTIPLE command is supported !");
               nError = W_ERROR_TIMEOUT;
            }
         }

         /* Post the result */
         goto send_result;

      case P_15693_COMMAND_LOCK:

         PDebugTrace("static_P15P3UserSendCommandCompleted : P_15693_COMMAND_LOCK");

         /* If the command is not supported */
         if ( bIsCommandSupported == W_FALSE )
         {
            /* Prepare the command */
            if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
            {
               if ( p15P3UserConnection->bIsEOFSent == W_FALSE )
               {
                  /* Set the flag */
                  p15P3UserConnection->bIsEOFSent = W_TRUE;

                  /* Send the EOF */
                  static_P15P3DriverExchangeData(
                     pContext,
                     p15P3UserConnection->hDriverConnection,
                     static_P15P3UserSendCommandCompleted,
                     p15P3UserConnection,
                     null,
                     0,
                     p15P3UserConnection->aCardToReaderBuffer15693,
                     P_15693_BUFFER_MAX_SIZE );

                  return;
               }
               else
               {
                  PDebugError("static_P15P3UserSendCommandCompleted: W_ERROR_TIMEOUT");
                  /* Send the error */
                  nError = W_ERROR_TIMEOUT;
                  goto return_error;
               }
            }
            else
            {
               PDebugError("static_P15P3UserSendCommandCompleted: W_ERROR_TIMEOUT");
               /* Send the error */
               nError = W_ERROR_TIMEOUT;
               goto return_error;
            }
         }

         /* Lock the block */
         p15P3UserConnection->bIsSectorLocked[p15P3UserConnection->nLockBlockStart] = W_TRUE;

         /* Move to the next block */
         p15P3UserConnection->nLockBlockStart ++;

         if ( p15P3UserConnection->nLockBlockStart <= p15P3UserConnection->nLockBlockEnd )
         {
            /* Send the command */
            static_P15P3UserSendCommand(pContext, p15P3UserConnection, P_15693_COMMAND_LOCK );
            return;
         }

         /* Send the result */
         nError = W_SUCCESS;
         goto send_result;

      case P_15693_COMMAND_ATTRIBUTE:

         PDebugTrace("static_P15P3UserSendCommandCompleted: P_15693_COMMAND_ATTRIBUTE");

         /* If the command is not supported */
         if ( bIsCommandSupported == W_FALSE )
         {
            /* Prepare the command */
            if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
            {
               if ( p15P3UserConnection->bIsEOFSent == W_FALSE )
               {
                  /* Set the flag */
                  p15P3UserConnection->bIsEOFSent = W_TRUE;

                  /* Send the EOF */
                  static_P15P3DriverExchangeData(
                     pContext,
                     p15P3UserConnection->hDriverConnection,
                     static_P15P3UserSendCommandCompleted,
                     p15P3UserConnection,
                     null,
                     0,
                     p15P3UserConnection->aCardToReaderBuffer15693,
                     P_15693_BUFFER_MAX_SIZE );

                  return;
               }
               else
               {
                  PDebugError("static_P15P3UserSendCommandCompleted: W_ERROR_TIMEOUT");
                  /* Send the error */
                  nError = W_ERROR_TIMEOUT;
                  goto return_error;
               }
            }
         }
         /* Check the action sent */
         /* Set AFI */
         if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_SET_AFI )
         {
            p15P3UserConnection->nAFI = p15P3UserConnection->nAFINew;
            /* Cancel the action */
            p15P3UserConnection->nActions -= W_15_ATTRIBUTE_ACTION_SET_AFI;
         }
         else
         {
            /* Lock AFI */
            if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_LOCK_AFI )
            {
               p15P3UserConnection->bIsAFILocked = W_TRUE;
               /* Cancel the action */
               p15P3UserConnection->nActions -= W_15_ATTRIBUTE_ACTION_LOCK_AFI;
            }
            else
            {
               /* Set DSFID */
               if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_SET_DSFID )
               {
                  p15P3UserConnection->nDSFID = p15P3UserConnection->nDSFIDNew;
                  /* Cancel the action */
                  p15P3UserConnection->nActions -= W_15_ATTRIBUTE_ACTION_SET_DSFID;
               }
               else
               {
                  /* Lock DSFID */
                  if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_LOCK_DSFID )
                  {
                     p15P3UserConnection->bIsDSFIDLocked = W_TRUE;
                     /* Cancel the action */
                     p15P3UserConnection->nActions -= W_15_ATTRIBUTE_ACTION_LOCK_DSFID;
                  }
               }
            }
         }

         /* Check if there is still an action to process */
         if ( p15P3UserConnection->nActions != 0x00 )
         {
            /* Send the command(s) */
            static_P15P3UserSendCommand(pContext, p15P3UserConnection, P_15693_COMMAND_ATTRIBUTE );
            return;
         }

         /* Send the result */
         nError = W_SUCCESS;
         goto send_result;
   }

return_error:
   PDebugError("static_P15P3UserSendCommandCompleted: error %s", PUtilTraceError(nError));

send_result:
   if ((p15P3UserConnection->nCommandType == P_15693_COMMAND_READ) || (p15P3UserConnection->nCommandType == P_15693_COMMAND_WRITE))
   {
      PDFCPostContext2(&p15P3UserConnection->sCacheCallbackContext, nError);
   }
   else
   {
      static_P15P3SendResultInternal(pContext, p15P3UserConnection, nError);
   }
}

/**
 * @brief   Sends a command frame.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   p15P3UserConnection  The ISO 15693 connection structure.
 *
 * @param[in]   nCommandType  The command type.
 *
 * @param[in]   pBuffer  The buffer to add in the command, if needed.
 *
 * @param[in]   nLength  The length of the buffer.
 **/
static void static_P15P3UserSendCommand(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            uint8_t nCommandType )
{
   uint32_t nCommandLength = 0x00;
   uint8_t nIndex;
   uint8_t* pBuffer;
   uint32_t nDataSize;

   /* Store the command type */
   p15P3UserConnection->nCommandType = nCommandType;

   /* Send the corresponding command */
   switch ( nCommandType )
   {
      case P_15693_COMMAND_GET_SYSTEMINFO:

         PDebugTrace("static_P15P3UserSendCommand: P_15693_COMMAND_GET_SYSTEMINFO");

         /* note : operation performed only once, do not need to use a cache */

         /* Prepare the command */
         p15P3UserConnection->aReaderToCardBuffer15693[0] = P_15693_CODE_FLAG;
         p15P3UserConnection->aReaderToCardBuffer15693[1] = P_15693_CODE_GET_SYSTEMINFO;

         /* Send the command */
         static_P15P3DriverExchangeData(
            pContext,
            p15P3UserConnection->hDriverConnection,
            static_P15P3UserSendCommandCompleted,
            p15P3UserConnection,
            p15P3UserConnection->aReaderToCardBuffer15693,
            2,
            p15P3UserConnection->aCardToReaderBuffer15693,
            P_15693_BUFFER_MAX_SIZE );
         break;

      case P_15693_COMMAND_GET_SECURITYINFO:

         PDebugTrace("static_P15P3UserSendCommand: P_15693_COMMAND_GET_SECURITYINFO");

         /* note : operation performed only once, do not need to use a cache */

         /* Prepare the command */
         p15P3UserConnection->aReaderToCardBuffer15693[0] = P_15693_CODE_FLAG;
         p15P3UserConnection->aReaderToCardBuffer15693[1] = P_15693_CODE_GET_SECURITYINFO;
         p15P3UserConnection->aReaderToCardBuffer15693[2] = 0x00;                                 /* Block 0 */
         p15P3UserConnection->aReaderToCardBuffer15693[3] = (uint8_t)(p15P3UserConnection->nSectorNumber - 1);   /* Block number */

         /* Send the command */
         static_P15P3DriverExchangeData(
            pContext,
            p15P3UserConnection->hDriverConnection,
            static_P15P3UserSendCommandCompleted,
            p15P3UserConnection,
            p15P3UserConnection->aReaderToCardBuffer15693,
            4,
            p15P3UserConnection->aCardToReaderBuffer15693,
            P_15693_BUFFER_MAX_SIZE );
         break;


      case P_15693_COMMAND_READ:

         PDebugTrace("static_P15P3UserSendCommand: P_15693_COMMAND_READ");

         /* Prepare the command */
         p15P3UserConnection->aReaderToCardBuffer15693[0] = P_15693_CODE_FLAG;

         /* Check if the read multiple is supported or not */

         if ( p15P3UserConnection->bIsReadMultipleSupported != W_FALSE )
         {
            PDebugTrace("static_P15P3UserSendCommand : READ_MULTIPLE");

            p15P3UserConnection->aReaderToCardBuffer15693[1] = P_15693_CODE_READ_MULTIPLE;
            p15P3UserConnection->aReaderToCardBuffer15693[2] = p15P3UserConnection->nBlockStart;
            p15P3UserConnection->aReaderToCardBuffer15693[3] = p15P3UserConnection->nBlockEnd - p15P3UserConnection->nBlockStart;
            nCommandLength = 4;
         }
         else
         {
            PDebugTrace("static_P15P3UserSendCommand : READ_SINGLE");

            p15P3UserConnection->aReaderToCardBuffer15693[1] = P_15693_CODE_READ_SINGLE;
            p15P3UserConnection->aReaderToCardBuffer15693[2] = p15P3UserConnection->nBlockStart;
            nCommandLength = 3;
         }

         /* Send the command */
         static_P15P3DriverExchangeData(
            pContext,
            p15P3UserConnection->hDriverConnection,
            static_P15P3UserSendCommandCompleted,
            p15P3UserConnection,
            p15P3UserConnection->aReaderToCardBuffer15693,
            nCommandLength,
            p15P3UserConnection->aCardToReaderBuffer15693,
            P_15693_BUFFER_MAX_SIZE );
         break;

      case P_15693_COMMAND_WRITE:
         PDebugTrace("static_P15P3UserSendCommand: P_15693_COMMAND_WRITE");

         pBuffer = p15P3UserConnection->aReaderToCardBuffer15693;

         /* Prepare the command */
         if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
         {
            /* Add the option flag */
            pBuffer[0] = P_15693_CODE_FLAG_OPTION;
            nCommandLength ++;
         }
         else
         {
            /* Add the option flag */
            pBuffer[0] = P_15693_CODE_FLAG;
            nCommandLength ++;
         }

         /* Check if the write multiple is supported or not */
         if ( p15P3UserConnection->bIsWriteMultipleSupported != W_FALSE )
         {
            pBuffer[nCommandLength ++] = P_15693_CODE_WRITE_MULTIPLE;
            pBuffer[nCommandLength ++] = p15P3UserConnection->nBlockStart;
            pBuffer[nCommandLength ++] = p15P3UserConnection->nBlockEnd - p15P3UserConnection->nBlockStart;

            nCommandLength += ( (p15P3UserConnection->nBlockEnd - p15P3UserConnection->nBlockStart + 1) * p15P3UserConnection->nSectorSize );
            nDataSize    = (p15P3UserConnection->nBlockEnd - p15P3UserConnection->nBlockStart + 1) * p15P3UserConnection->nSectorSize;
         }
         else
         {
            pBuffer[nCommandLength ++] = P_15693_CODE_WRITE_SINGLE;
            pBuffer[nCommandLength ++] = p15P3UserConnection->nBlockStart;
            nDataSize    =  p15P3UserConnection->nSectorSize;
         }

         /* Copy the data to write */
         CMemoryCopy(&pBuffer[nCommandLength], p15P3UserConnection->pSmartCacheBuffer, nDataSize);
         nCommandLength += nDataSize;

         if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
         {
            /* Reset the EOF flag */
            p15P3UserConnection->bIsEOFSent = W_FALSE;

            if (P15P3DriverSetTimeout(
               pContext,
               p15P3UserConnection->hDriverConnection,
               P_15693_SMALL_TIMEOUT ) != W_SUCCESS)
            {
               PDebugError("static_P15P3UserSendCommand : P15P3DriverSetTimeout() failed");
            }
         }

         /* Send the command */
         static_P15P3DriverExchangeData(
            pContext,
            p15P3UserConnection->hDriverConnection,
            static_P15P3UserSendCommandCompleted,
            p15P3UserConnection,
            pBuffer,
            nCommandLength,
            p15P3UserConnection->aCardToReaderBuffer15693,
            P_15693_BUFFER_MAX_SIZE );

         if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
         {
            /* Set the timeout */
            if (P15P3DriverSetTimeout(
               pContext,
               p15P3UserConnection->hDriverConnection,
               P_15693_DEFAULT_TIMEOUT ) != W_SUCCESS)
            {
               PDebugError("static_P15P3UserSendCommand : P15P3DriverSetTimeout() failed");
            }
         }
         break;

      case P_15693_COMMAND_LOCK:

         PDebugTrace("static_P15P3UserSendCommand: P_15693_COMMAND_LOCK : locking block %d", p15P3UserConnection->nLockBlockStart);

         /* Prepare the command */
         if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
         {
            /* Add the option flag */
            p15P3UserConnection->aReaderToCardBuffer15693[0] = P_15693_CODE_FLAG_OPTION;
            nCommandLength ++;
         }
         else
         {
            p15P3UserConnection->aReaderToCardBuffer15693[0] = P_15693_CODE_FLAG;
            nCommandLength ++;
         }
         p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = P_15693_CODE_LOCK;
         p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = p15P3UserConnection->nLockBlockStart;

         if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
         {
            /* Reset the EOF flag */
            p15P3UserConnection->bIsEOFSent = W_FALSE;
            /* Set the timeout */
            if (P15P3DriverSetTimeout(
               pContext,
               p15P3UserConnection->hDriverConnection,
               P_15693_SMALL_TIMEOUT ) != W_SUCCESS)
            {
               PDebugError("static_P15P3UserSendCommand : P15P3DriverSetTimeout failed");
            }
         }
         /* Send the command */
         static_P15P3DriverExchangeData(
            pContext,
            p15P3UserConnection->hDriverConnection,
            static_P15P3UserSendCommandCompleted,
            p15P3UserConnection,
            p15P3UserConnection->aReaderToCardBuffer15693,
            nCommandLength,
            p15P3UserConnection->aCardToReaderBuffer15693,
            P_15693_BUFFER_MAX_SIZE );

         if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
         {
            /* Set the timeout */
            if (P15P3DriverSetTimeout(
               pContext,
               p15P3UserConnection->hDriverConnection,
               P_15693_DEFAULT_TIMEOUT ) != W_SUCCESS)
            {
               PDebugError("static_P15P3UserSendCommand : P15P3DriverSetTimeout failed");
            }
         }
         break;

      case P_15693_COMMAND_ATTRIBUTE:
         PDebugTrace("static_P15P3UserSendCommand: P_15693_COMMAND_ATTRIBUTE");

         /* Prepare the command */
         if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
         {
            /* Add the option flag */
            p15P3UserConnection->aReaderToCardBuffer15693[0] = P_15693_CODE_FLAG_OPTION;
            nCommandLength ++;
         }
         else
         {
            p15P3UserConnection->aReaderToCardBuffer15693[0] = P_15693_CODE_FLAG;
            nCommandLength ++;
         }
         /* Set AFI */
         if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_SET_AFI )
         {
            p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = P_15693_CODE_WRITE_AFI;
            p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = p15P3UserConnection->nAFINew;
         }
         else
         {
            /* Lock AFI */
            if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_LOCK_AFI )
            {
               p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = P_15693_CODE_LOCK_AFI;
            }
            else
            {
               /* Set DSFID */
               if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_SET_DSFID )
               {
                  p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = P_15693_CODE_WRITE_DSFID;
                  p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = p15P3UserConnection->nDSFIDNew;
               }
               else
               {
                  /* Lock DSFID */
                  if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_LOCK_DSFID )
                  {
                     p15P3UserConnection->aReaderToCardBuffer15693[nCommandLength ++] = P_15693_CODE_LOCK_DSFID;
                  }
                  else
                  {
                     /* Lock tag */
                     if ( p15P3UserConnection->nActions & W_15_ATTRIBUTE_ACTION_LOCK_TAG )
                     {
                        /* Check if the card is not already fully locked */
                        for (nIndex=0; nIndex<=p15P3UserConnection->nSectorNumber-1; nIndex++)
                        {
                           if ( p15P3UserConnection->bIsSectorLocked[nIndex] == W_FALSE )
                           {
                              /* Set the values */
                              p15P3UserConnection->nLockBlockStart = nIndex;
                              p15P3UserConnection->nLockBlockEnd   = (uint8_t)(p15P3UserConnection->nSectorNumber - 1);  /* very odd */
                              /* Cancel the action */
                              p15P3UserConnection->nActions     -= W_15_ATTRIBUTE_ACTION_LOCK_TAG;
                              p15P3UserConnection->bLockSectors  = W_TRUE;
                              /* Lock the tag on all the blocks */
                              static_P15P3UserSendCommand(
                                 pContext,
                                 p15P3UserConnection,
                                 P_15693_COMMAND_LOCK );
                              return;
                           }
                        }

                        /* Send the result */
                        static_P15P3SendResultInternal(pContext, p15P3UserConnection, W_SUCCESS);
                        return;
                     }
                  }
               }
            }
         }

         /* Check if a command needs to be sent */
         if ( nCommandLength != 0x00 )
         {
            if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
            {
               /* Reset the EOF flag */
               p15P3UserConnection->bIsEOFSent = W_FALSE;

               /* Set the timeout */
               if (P15P3DriverSetTimeout(
                  pContext,
                  p15P3UserConnection->hDriverConnection,
                  P_15693_SMALL_TIMEOUT ) != W_SUCCESS)
               {
                  PDebugError("static_P15P3UserSendCommand : P15P3DriverSetTimeout failed");
               }
            }
            /* Send the command */
            static_P15P3DriverExchangeData(
               pContext,
               p15P3UserConnection->hDriverConnection,
               static_P15P3UserSendCommandCompleted,
               p15P3UserConnection,
               p15P3UserConnection->aReaderToCardBuffer15693,
               nCommandLength,
               p15P3UserConnection->aCardToReaderBuffer15693,
               P_15693_BUFFER_MAX_SIZE );

            if ( p15P3UserConnection->nCardManufacturer == P_15693_UID_MANUFACTURER_TI )
            {
               /* Set the timeout */
               if (P15P3DriverSetTimeout(
                  pContext,
                  p15P3UserConnection->hDriverConnection,
                  P_15693_DEFAULT_TIMEOUT ) != W_SUCCESS)
               {
                  PDebugError("static_P15P3UserSendCommand : P15P3DriverSetTimeout failed");
               }
            }
         }
         else
         {
            PDebugError("static_P15P3UserSendCommand: no more command to send");

            /* Send the error */
            static_P15P3SendResultInternal(pContext, p15P3UserConnection, W_ERROR_BAD_PARAMETER);
         }
         break;
   }
}

/* See tWBasicGenericDataCallbackFunction */
static void static_P15ExchangeDataCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError )
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pCallbackParameter;

   PDebugError("static_P15P3UserExchangeDataCompleted: nError %s", PUtilTraceError(nError));

   if (p15P3UserConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, p15P3UserConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_P15ExchangeDataCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, p15P3UserConnection->hCurrentOperation);
      PHandleClose(pContext, p15P3UserConnection->hCurrentOperation);
      p15P3UserConnection->hCurrentOperation = W_NULL_HANDLE;
   }

   /* Send the result */
   PDFCPostContext3(
      &p15P3UserConnection->sCallbackContext,
      nLength,
      nError );

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, p15P3UserConnection);
}

/* See Header file */
static void static_P15ExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t *pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t *pCardToReaderBuffer,
            uint32_t nCardToReaderMaxMaxLength,
            W_HANDLE * phOperation)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pObject;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check the protocol type */
   if (p15P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN)
   {
      PDebugError("static_P15ExchangeData: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Creates an internal operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("static_P15ExchangeData: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &p15P3UserConnection->hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("static_P15ExchangeData: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P15ExchangeDataCompleted when the operation is completed */
   PHandleIncrementReferenceCount(p15P3UserConnection);

   /* Store the callback context */
   p15P3UserConnection->sCallbackContext = sCallbackContext;

   PSmartCacheInvalidateCache(pContext, & p15P3UserConnection->sSmartCache, 0,  p15P3UserConnection->nSectorSize * p15P3UserConnection->nSectorNumber);

   /* Send the command */
   static_P15P3DriverExchangeData(
      pContext,
      p15P3UserConnection->hDriverConnection,
      static_P15ExchangeDataCompleted,
      p15P3UserConnection,
      pReaderToCardBuffer,
      nReaderToCardBufferLength,
      pCardToReaderBuffer,
      nCardToReaderMaxMaxLength );

   return;

return_error:

   PDFCPostContext3(&sCallbackContext, 0, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Header file */
void P15P3UserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tP15P3UserConnection* p15P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the ISO 15693 buffer */
   p15P3UserConnection = (tP15P3UserConnection*)CMemoryAlloc( sizeof(tP15P3UserConnection) );
   if ( p15P3UserConnection == null )
   {
      PDebugError("P15P3UserCreateConnection: p15P3UserConnection == null");
      /* Send the result */
      PDFCPostContext2(
         &sCallbackContext,
         W_ERROR_OUT_OF_RESOURCE );
      return;
   }
   CMemoryFill(p15P3UserConnection, 0, sizeof(tP15P3UserConnection));

   /* Add the ISO 15693 structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     p15P3UserConnection,
                     P_HANDLE_TYPE_15693_USER_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("P15P3UserCreateConnection: could not add the ISO 15693 buffer");
      /* Send the result */
      PDFCPostContext2(&sCallbackContext, nError);
      CMemoryFree(p15P3UserConnection);
      return;
   }

   /* Store the connection information */
   p15P3UserConnection->hConnection        = hUserConnection;
   p15P3UserConnection->hDriverConnection  = hDriverConnection;
   p15P3UserConnection->nProtocol          = W_PROP_ISO_15693_3;

   /* Parse the information */
   if ( ( nError = static_P15P3UserParseInventory(
                     pContext,
                     p15P3UserConnection,
                     pBuffer,
                     nLength ) ) != W_SUCCESS )
   {
      PDebugError("P15P3UserCreateConnection: activate error 0x%08X", nError);
      /* Set the default values */
      p15P3UserConnection->nCardManufacturer  = P_15693_UID_MANUFACTURER_UNKNOWN;
      p15P3UserConnection->nProtocol = P_PROTOCOL_STILL_UNKNOWN;
      /* Send the result */
      PDFCPostContext2(&sCallbackContext, nError);
      return;
   }

   /* Store the callback context */
   p15P3UserConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P15P3SendResultInternal when the operation is completed */
   PHandleIncrementReferenceCount(p15P3UserConnection);

   /* Get the system info */
   static_P15P3UserSendCommand(
      pContext,
      p15P3UserConnection,
      P_15693_COMMAND_GET_SYSTEMINFO );
}

/* See Header file */
W_ERROR P15P3UserCheckType6(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumSpaceSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable )
{
   tP15P3UserConnection* p15P3UserConnection;
   uint32_t nIndex;
   W_ERROR nError;
   bool_t    bIsLocked;

   /* Reset the maximum tag size */
   *pnMaximumSpaceSize = 0;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if ( nError == W_SUCCESS )
   {
      /* return the tag size */
      *pnMaximumSpaceSize  = p15P3UserConnection->nSectorNumber * p15P3UserConnection->nSectorSize;

      bIsLocked = W_FALSE;

      /* on a well formatted tag, all lock bytes must have the same value, but for better compatibility
         we relax this check and allow read only access to a tag that is partially locked */

      for (nIndex=0; nIndex<p15P3UserConnection->nSectorNumber; nIndex++)
      {
         if (p15P3UserConnection->bIsSectorLocked[nIndex] != W_FALSE)
         {
            bIsLocked = W_TRUE;
            break;
         }
      }

      /* Get the read only mode */
      if ( bIsLocked == W_FALSE )
      {
         PDebugTrace("P15P3UserCheckType6: card not locked");
         *pbIsLocked = W_FALSE;
         *pbIsLockable = W_TRUE;
         *pbIsFormattable = W_TRUE;
      }
      else
      {

         for (nIndex=1; nIndex<p15P3UserConnection->nSectorNumber; nIndex++)
         {
            if ( p15P3UserConnection->bIsSectorLocked[nIndex - 1] != p15P3UserConnection->bIsSectorLocked[nIndex] )
            {
               PDebugWarning("The lock bytes of this tag are not consistent, we allow only read-only access");
            }
         }

         PDebugTrace("P15P3UserCheckType6: card locked");
         *pbIsLocked = W_TRUE;
         *pbIsLockable = W_FALSE;
         *pbIsFormattable = W_FALSE;
      }

   }

   return nError;
}

/* See Client API Specifications */
W_ERROR P15ListenToCardDetection(
            tContext* pContext,
            tPReaderCardDetectionHandler *pHandler,
            void *pHandlerParameter,
            uint8_t nPriority,
            uint8_t nAFI,
            W_HANDLE *phEventRegistry )
{
   tP15P3DetectionConfiguration sDetectionConfiguration, *pDetectionConfigurationBuffer;
   uint32_t nDetectionConfigurationBufferLength;
   static const uint8_t nProtocol = W_PROP_ISO_15693_3;

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   /* The anti-collision parameters shall be zero if W_PRIORITY_EXCLUSIVE is not used*/
   if(nAFI != 0x00)
   {
      if(nPriority != W_PRIORITY_EXCLUSIVE)
      {
         PDebugError("P15ListenToCardDetection: Bad Detection configuration parameters or bad priorty type");
         return W_ERROR_BAD_PARAMETER;

      }
      else
      {
         /* set the pointer of configuration buffer and  buffer length*/
         pDetectionConfigurationBuffer = &sDetectionConfiguration;
         nDetectionConfigurationBufferLength = sizeof(tP15P3DetectionConfiguration);
         /* set the AFI*/
         sDetectionConfiguration.nAFI = nAFI;
      }
   }
   else /* The AFI is not used*/
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

/* See Client API Specifications */
W_ERROR P15SetTagSize(
            tContext* pContext,
            W_HANDLE hConnection,
            uint16_t nSectorNumber,
            uint8_t nSectorSize )
{
   tP15P3UserConnection* p15P3UserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if ( nError == W_SUCCESS )
   {
      if (p15P3UserConnection->bIsTagSizeSetFromSystemInfo == W_FALSE)
      {
         if ((nSectorSize == 0) || (nSectorNumber == 0))
         {
            nError = W_ERROR_BAD_PARAMETER;
         }
         else
         {
            p15P3UserConnection->nSectorSize     = nSectorSize;
            p15P3UserConnection->nSectorNumber   = nSectorNumber;
         }
      }
      else
      {
         if ((p15P3UserConnection->nSectorSize != nSectorSize) || (p15P3UserConnection->nSectorNumber != nSectorNumber))
         {
            nError = W_ERROR_BAD_PARAMETER;
         }
      }
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR P15SetSupportedCommands(
            tContext* pContext,
            W_HANDLE hConnection,
            bool_t bIsReadMultipleSupported,
            bool_t bIsWriteMultipleSupported )
{
   tP15P3UserConnection* p15P3UserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if ( nError == W_SUCCESS )
   {
      p15P3UserConnection->bIsReadMultipleSupported  = bIsReadMultipleSupported;
      p15P3UserConnection->bIsWriteMultipleSupported = bIsWriteMultipleSupported;
   }

   return nError;
}

/* Get identifier length */
static uint32_t static_P15P3UserGetIdentifierLength(
            tContext* pContext,
            void* pObject)
{
   return P_15693_UID_MAX_LENGTH;
}

/* Get identifier */
static void static_P15P3UserGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pObject;

   CMemoryCopy(pIdentifierBuffer, p15P3UserConnection->UID, P_15693_UID_MAX_LENGTH);
}

/* See Client API Specifications */
W_ERROR P15GetConnectionInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tW15ConnectionInfo *pConnectionInfo )
{
   tP15P3UserConnection* p15P3UserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   /* Check the parameters */
   if ( pConnectionInfo == null )
   {
      PDebugError("P15GetConnectionInfo: pConnectionInfo == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ( nError == W_SUCCESS )
   {
      /* AFI */
      pConnectionInfo->bIsAFISupported    = p15P3UserConnection->bIsAFISupported;
      pConnectionInfo->nAFI               = p15P3UserConnection->nAFI;
      pConnectionInfo->bIsAFILocked       = p15P3UserConnection->bIsAFILocked;
      /* DSFID */
      pConnectionInfo->bIsDSFIDSupported  = p15P3UserConnection->bIsDSFIDSupported;
      pConnectionInfo->nDSFID             = p15P3UserConnection->nDSFID;
      pConnectionInfo->bIsDSFIDLocked     = p15P3UserConnection->bIsDSFIDLocked;
      /* Sector information */
      pConnectionInfo->nSectorSize        = p15P3UserConnection->nSectorSize;
      pConnectionInfo->nSectorNumber      = p15P3UserConnection->nSectorNumber;
      /* UID */
      CMemoryCopy(
         pConnectionInfo->UID,
         p15P3UserConnection->UID,
         P_15693_UID_MAX_LENGTH );
      return W_SUCCESS;
   }
   else
   {
      PDebugError("P15GetConnectionInfo: could not get p15P3UserConnection buffer");

      /* Fill in the structure with zeros */
      CMemoryFill(pConnectionInfo, 0, sizeof(tW15ConnectionInfo));

      return nError;
   }
}

/* See Client API Specifications */
W_ERROR P15IsWritable(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t nSectorIndex )
{
   tP15P3UserConnection* p15P3UserConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if ( nError == W_SUCCESS )
   {
      /* Check the parameter */
      if ( nSectorIndex >= p15P3UserConnection->nSectorNumber )
      {
         PDebugError("P15IsWritable: W_ERROR_BAD_PARAMETER");
         return W_ERROR_BAD_PARAMETER;
      }

      /* Send the sector lock status */
      if ( p15P3UserConnection->bIsSectorLocked[nSectorIndex] == W_FALSE )
      {
         return W_SUCCESS;
      }
      else
      {
         return W_ERROR_ITEM_LOCKED;
      }
   }
   else
   {
      PDebugError("P15IsWritable: could not get p15P3UserConnection buffer");
      return nError;
   }
}

static void static_P15SetAttributeInternalEx(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t nActions,
            uint8_t nAFI,
            uint8_t nDSFID)
{
   /* Store the connection information */
   p15P3UserConnection->nActions = nActions;
   if (nActions & W_15_ATTRIBUTE_ACTION_SET_AFI)
   {
      p15P3UserConnection->nAFINew = nAFI;
      PDebugTrace("static_P15SetAttributeInternalEx: set AFI 0x%02X", p15P3UserConnection->nAFINew );
   }
   if ( nActions & W_15_ATTRIBUTE_ACTION_SET_DSFID )
   {
      p15P3UserConnection->nDSFIDNew = nDSFID;
      PDebugTrace("static_P15SetAttributeInternalEx: set DSFID 0x%02X", p15P3UserConnection->nDSFIDNew );
   }

   /* Store the internal callback context used by static_P15P3SendResultInternal */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*) pCallback,
      pCallbackParameter,
      &p15P3UserConnection->sCallbackContextInternal);

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P15P3SendResultInternal when the operation is completed */
   PHandleIncrementReferenceCount(p15P3UserConnection);

   /* Send the command */
   static_P15P3UserSendCommand(
      pContext,
      p15P3UserConnection,
      P_15693_COMMAND_ATTRIBUTE);
}

/* See Client API Specifications */
void P15SetAttribute(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t nActions,
            uint8_t nAFI,
            uint8_t nDSFID,
            W_HANDLE *phOperation )
{
   tP15P3UserConnection* p15P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("P15SetAttribute: wrong connection handle");
      goto return_error;
   }

   /* Check the protocol type */
   if ( p15P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("P15SetAttribute: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the parameters */
   if (  ( ( nActions & W_15_ATTRIBUTE_ACTION_SET_AFI ) == 0x00 )
      && ( ( nActions & W_15_ATTRIBUTE_ACTION_LOCK_AFI ) == 0x00 )
      && ( ( nActions & W_15_ATTRIBUTE_ACTION_SET_DSFID ) == 0x00 )
      && ( ( nActions & W_15_ATTRIBUTE_ACTION_LOCK_DSFID ) == 0x00 )
      && ( ( nActions & W_15_ATTRIBUTE_ACTION_LOCK_TAG ) == 0x00 ) )
   {
      PDebugError("P15SetAttribute: W_ERROR_BAD_PARAMETER (incorrect action)");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the AFI values */
   if (  (  nActions & W_15_ATTRIBUTE_ACTION_SET_AFI )
      || (  nActions & W_15_ATTRIBUTE_ACTION_LOCK_AFI ) )
   {
      if ( p15P3UserConnection->bIsAFISupported == W_FALSE )
      {
         PDebugError("P15SetAttribute: W_ERROR_BAD_PARAMETER (AFI not supported)");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }
      if ( p15P3UserConnection->bIsAFILocked != W_FALSE )
      {
         PDebugError("P15SetAttribute: W_ERROR_ITEM_LOCKED (AFI locked)");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }
   }

   /* Check the DSFID values */
   if (  (  nActions & W_15_ATTRIBUTE_ACTION_SET_DSFID )
      || (  nActions & W_15_ATTRIBUTE_ACTION_LOCK_DSFID ) )
   {
      if ( p15P3UserConnection->bIsDSFIDSupported == W_FALSE )
      {
         PDebugError("P15SetAttribute: W_ERROR_BAD_PARAMETER (DSFID not supported)");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }
      if ( p15P3UserConnection->bIsDSFIDLocked != W_FALSE )
      {
         PDebugError("P15SetAttribute: W_ERROR_ITEM_LOCKED (DSFID locked)");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }
   }

   /* Check the lock tag value */
   if ( nActions & W_15_ATTRIBUTE_ACTION_LOCK_TAG )
   {
      PDebugTrace("P15SetAttribute: lock tag");
      /* Check the memory information */
      if ( p15P3UserConnection->nSectorNumber == 0x00 )
      {
         PDebugError("P15SetAttribute: unknown memory information");
         nError = W_ERROR_MISSING_INFO;
         goto return_error;
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("P15SetAttribute: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("P15SetAttribute: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_P15ExecuteQueuedExchange, p15P3UserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p15P3UserConnection);

      /* Store the operation handle */
      CDebugAssert(p15P3UserConnection->hCurrentOperation == W_NULL_HANDLE);
      p15P3UserConnection->hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      p15P3UserConnection->sCallbackContext = sCallbackContext;

      /* Set attribute */
      static_P15SetAttributeInternalEx(
         pContext,
         p15P3UserConnection,
         static_P15P3UserCommandCompleted,
         p15P3UserConnection,
         nActions,
         nAFI,
         nDSFID);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p15P3UserConnection);

      /* Save the operation handle */
      CDebugAssert(p15P3UserConnection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      p15P3UserConnection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      p15P3UserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      p15P3UserConnection->sQueuedOperation.nType = P_15_QUEUED_SET_ATTRIBUTE;

      /* Save data */
      p15P3UserConnection->sQueuedOperation.nActions = nActions;
      p15P3UserConnection->sQueuedOperation.nAFI = nAFI;
      p15P3UserConnection->sQueuedOperation.nDSFID = nDSFID;

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
   PDebugError("P15SetAttribute: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError );

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

#define P_15P3_IDENTIFIER_LEVEL     0
                                                            \
#define PCacheDescriptorN(n)                                \
{                                                           \
   P_15P3_IDENTIFIER_LEVEL,  &g_sSectorSize##n,             \
      {                                                     \
         { 1, 1, static_P15P3AtomicRead },                 \
         { 0, 0, null }                                     \
      },                                                    \
      {                                                     \
         { 1, 1, static_P15P3AtomicWrite },                \
         { 0, 0, null }                                     \
      }                                                     \
}

static void static_P15P3AtomicRead(tContext * pContext, void * pConnection, uint32_t nSectorOffset, uint32_t nSectorNumber,
                     uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_P15P3AtomicWrite(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     const uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

extern tSmartCacheSectorSize g_sSectorSize1;
extern tSmartCacheSectorSize g_sSectorSize2;
extern tSmartCacheSectorSize g_sSectorSize3;
extern tSmartCacheSectorSize g_sSectorSize4;
extern tSmartCacheSectorSize g_sSectorSize5;
extern tSmartCacheSectorSize g_sSectorSize6;
extern tSmartCacheSectorSize g_sSectorSize7;
extern tSmartCacheSectorSize g_sSectorSize8;
extern tSmartCacheSectorSize g_sSectorSize9;
extern tSmartCacheSectorSize g_sSectorSize10;
extern tSmartCacheSectorSize g_sSectorSize11;
extern tSmartCacheSectorSize g_sSectorSize12;
extern tSmartCacheSectorSize g_sSectorSize13;
extern tSmartCacheSectorSize g_sSectorSize14;
extern tSmartCacheSectorSize g_sSectorSize15;
extern tSmartCacheSectorSize g_sSectorSize16;
extern tSmartCacheSectorSize g_sSectorSize17;
extern tSmartCacheSectorSize g_sSectorSize18;
extern tSmartCacheSectorSize g_sSectorSize19;
extern tSmartCacheSectorSize g_sSectorSize20;
extern tSmartCacheSectorSize g_sSectorSize21;
extern tSmartCacheSectorSize g_sSectorSize22;
extern tSmartCacheSectorSize g_sSectorSize23;
extern tSmartCacheSectorSize g_sSectorSize24;
extern tSmartCacheSectorSize g_sSectorSize25;
extern tSmartCacheSectorSize g_sSectorSize26;
extern tSmartCacheSectorSize g_sSectorSize27;
extern tSmartCacheSectorSize g_sSectorSize28;
extern tSmartCacheSectorSize g_sSectorSize29;
extern tSmartCacheSectorSize g_sSectorSize30;
extern tSmartCacheSectorSize g_sSectorSize31;
extern tSmartCacheSectorSize g_sSectorSize32;

static tSmartCacheDescriptor  asSmartCacheDescriptors[] =
{
   PCacheDescriptorN(1),
   PCacheDescriptorN(2),
   PCacheDescriptorN(3),
   PCacheDescriptorN(4),
   PCacheDescriptorN(5),
   PCacheDescriptorN(6),
   PCacheDescriptorN(7),
   PCacheDescriptorN(8),
   PCacheDescriptorN(9),
   PCacheDescriptorN(10),
   PCacheDescriptorN(11),
   PCacheDescriptorN(12),
   PCacheDescriptorN(13),
   PCacheDescriptorN(14),
   PCacheDescriptorN(15),
   PCacheDescriptorN(16),
   PCacheDescriptorN(17),
   PCacheDescriptorN(18),
   PCacheDescriptorN(19),
   PCacheDescriptorN(20),
   PCacheDescriptorN(21),
   PCacheDescriptorN(22),
   PCacheDescriptorN(23),
   PCacheDescriptorN(24),
   PCacheDescriptorN(25),
   PCacheDescriptorN(26),
   PCacheDescriptorN(27),
   PCacheDescriptorN(28),
   PCacheDescriptorN(29),
   PCacheDescriptorN(30),
   PCacheDescriptorN(31),
   PCacheDescriptorN(32)
};

W_ERROR static_P15P3UserCreateSmartCache(
   tContext              * pContext,
   tP15P3UserConnection  * p15P3UserConnection)
{

   W_ERROR nError;

   if ((p15P3UserConnection->nSectorNumber != 0) && (p15P3UserConnection->nSectorSize != 0))
   {
      /* create the smart cache */

      nError = PSmartCacheCreateCache(pContext, &p15P3UserConnection->sSmartCache, p15P3UserConnection->nSectorNumber,
         & asSmartCacheDescriptors[p15P3UserConnection->nSectorSize-1], p15P3UserConnection);

      if (nError == W_SUCCESS)
      {
         p15P3UserConnection->bIsSmartCacheInitialized = W_TRUE;
      }

   }
   else
   {
      nError = W_ERROR_MISSING_INFO;
   }

   return (nError);
}

/**
 * @brief Reads from a 15693-3 chip
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pConnection  The pointer on the connection.
 *
 * @param[in]  nSectorOffset  The zero-based index of the first sector to read.
 *
 * @param[in]  nSectorNumber  The number of sector to read.
 *
 * @param[out] pBuffer  The pointer on the buffer where to store the read data.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to the callback function.
 **/

static void static_P15P3AtomicRead(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      uint8_t                          * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pConnection;

   PDebugTrace("static_P15P3AtomicRead [%d-%d]", nSectorOffset * p15P3UserConnection->nSectorSize, (nSectorOffset+nSectorNumber) * p15P3UserConnection->nSectorSize - 1 );

   p15P3UserConnection->nBlockStart = (uint8_t) nSectorOffset;
   p15P3UserConnection->nBlockEnd   = (uint8_t) (nSectorOffset + nSectorNumber - 1);
   p15P3UserConnection->pSmartCacheBuffer = pBuffer;

   /* prepare the callback */
   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &p15P3UserConnection->sCacheCallbackContext);

   static_P15P3UserSendCommand(pContext, p15P3UserConnection, P_15693_COMMAND_READ);
}

static void static_P15ReadInternalEx(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_P15ReadInternalEx");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check connection object */
   if (p15P3UserConnection == null)
   {
      PDebugError("static_P15ReadInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check the protocol type */
   if (p15P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_P15ReadInternalEx: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the memory information */
   if (  ( p15P3UserConnection->nSectorSize   == 0x00 )
      || ( p15P3UserConnection->nSectorNumber == 0x00 ) )
   {
      PDebugError("static_P15ReadInternalEx: unknown memory information");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

      /* Check the parameters */
   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("static_P15ReadInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the parameters */
   if (( nOffset + nLength ) > (uint32_t)( p15P3UserConnection->nSectorSize * p15P3UserConnection->nSectorNumber ))
   {
      PDebugError("static_P15ReadInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Create the smart cache if not already created */
   if (p15P3UserConnection->bIsSmartCacheInitialized == W_FALSE)
   {
      nError = static_P15P3UserCreateSmartCache(pContext, p15P3UserConnection);
      if (nError != W_SUCCESS)
      {
         PDebugError("static_P15ReadInternalEx: static_P15P3userCreateSmartCache failed");
         goto return_error;
      }
   }

   /* Store the callback context */
   p15P3UserConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P15P3SendResultInternal when the operation is completed */
   PHandleIncrementReferenceCount(p15P3UserConnection);

   /* Read */
   PSmartCacheRead(  pContext,
                     &p15P3UserConnection->sSmartCache,
                     nOffset,
                     nLength,
                     pBuffer,
                     static_P15P3SendResultInternal,
                     p15P3UserConnection);

   return;

return_error:
   PDebugError("static_P15ReadInternalEx: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError );
}

/* See header */
void P15ReadInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength)
{
   tP15P3UserConnection* p15P3UserConnection = null;

   /* Check if the connection handle is valid */
   if (PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection) != W_SUCCESS)
   {
      p15P3UserConnection = null;
   }

   static_P15ReadInternalEx(
         pContext,
         p15P3UserConnection,
         pCallback, pCallbackParameter,
         pBuffer,
         nOffset,
         nLength);
}

/* See Client API Specifications */
void P15Read(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation )
{
   tP15P3UserConnection* p15P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;

   PDebugTrace("P15Read [%d-%d]", nOffset, nOffset+nLength-1);

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("P15Read: wrong connection handle");
      goto return_error;
   }

   /* Check the protocol type */
   if (p15P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("P15Read: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the memory information */
   if (  ( p15P3UserConnection->nSectorSize   == 0x00 )
      || ( p15P3UserConnection->nSectorNumber == 0x00 ) )
   {
      PDebugError("P15Read: unknown memory information");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

      /* Check the parameters */
   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("P15Read: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the parameters */
   if (( nOffset + nLength ) > (uint32_t)( p15P3UserConnection->nSectorSize * p15P3UserConnection->nSectorNumber ))
   {
      PDebugError("P15Read: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("P15Read: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("P15Read: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_P15ExecuteQueuedExchange, p15P3UserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p15P3UserConnection);

      /* Store the operation handle */
      CDebugAssert(p15P3UserConnection->hCurrentOperation == W_NULL_HANDLE);
      p15P3UserConnection->hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      p15P3UserConnection->sCallbackContext = sCallbackContext;

      /* Read */
      static_P15ReadInternalEx(
            pContext,
            p15P3UserConnection,
            static_P15P3UserCommandCompleted, p15P3UserConnection,
            pBuffer,
            nOffset,
            nLength);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p15P3UserConnection);

      /* Save the operation handle */
      CDebugAssert(p15P3UserConnection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      p15P3UserConnection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      p15P3UserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      p15P3UserConnection->sQueuedOperation.nType = P_15_QUEUED_READ;

      /* Save data */
      p15P3UserConnection->sQueuedOperation.pBuffer = pBuffer;
      p15P3UserConnection->sQueuedOperation.nOffset = nOffset;
      p15P3UserConnection->sQueuedOperation.nLength = nLength;

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
   PDebugError("P15Read: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError );

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}


/**
 * @brief Writes into a 15693-3 chip
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pConnection  The pointer on the connection.
 *
 * @param[in]  nSectorOffset  The zero-based index of the first sector to write.
 *
 * @param[in]  nSectorNumber  The number of sector to write.
 *
 * @param[in]  pBuffer  The pointer on the buffer with the data to write.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to the callback function.
 **/

static void static_P15P3AtomicWrite(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      const uint8_t                    * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*)pConnection;

   PDebugTrace("static_P15P3AtomicWrite [%d-%d]", nSectorOffset * p15P3UserConnection->nSectorSize, (nSectorOffset+nSectorNumber) * p15P3UserConnection->nSectorSize - 1 );

   p15P3UserConnection->nBlockStart = (uint8_t) nSectorOffset;
   p15P3UserConnection->nBlockEnd   = (uint8_t) (nSectorOffset + nSectorNumber - 1);
   p15P3UserConnection->pSmartCacheBuffer = (uint8_t *) pBuffer;

      /* prepare the callback */
   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &p15P3UserConnection->sCacheCallbackContext);

   static_P15P3UserSendCommand(pContext, p15P3UserConnection, P_15693_COMMAND_WRITE);
}


/**
 * @brief Completion routine called when write operation has been performed
 *
 * @param[in] pContext           The context
 *
 * @param[in] pCallbackParameter The callback parameter, e.g p15P3UserConnection
 *
 * @param[in] nError             The result of the write operation
 *
 */
static void static_P15WriteInternalCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError)
{
   tP15P3UserConnection* p15P3UserConnection = (tP15P3UserConnection*) pCallbackParameter;

   PDebugTrace("static_P15WriteInternalCompleted");

   if ((nError == W_SUCCESS) && (p15P3UserConnection->bLockSectors != W_FALSE))
   {
      /* Lock the sectors */
      static_P15P3UserSendCommand(pContext, p15P3UserConnection, P_15693_COMMAND_LOCK);
   }
   else
   {
      /* send the result of the operation */
      static_P15P3SendResultInternal(pContext, p15P3UserConnection, nError);
   }
}

static void static_P15WriteInternalEx(
            tContext* pContext,
            tP15P3UserConnection* p15P3UserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockSectors)
{
   tDFCCallbackContext sCallbackContext;
   uint32_t nIndex = 0x00;
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_P15WriteInternalEx");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check connection object */
   if (p15P3UserConnection == null)
   {
      PDebugError("static_P15WriteInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( p15P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_P15WriteInternalEx: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the memory information */
   if (  ( p15P3UserConnection->nSectorSize   == 0x00 )
      || ( p15P3UserConnection->nSectorNumber == 0x00 ) )
   {
      PDebugError("static_P15WriteInternalEx: unknown memory information");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockSectors == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("static_P15WriteInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockSectors == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("static_P15WriteInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (     ((bLockSectors == W_FALSE) && ((pBuffer == null) || (nLength == 0)))
         || ((nOffset+nLength) > (uint32_t)(p15P3UserConnection->nSectorSize * p15P3UserConnection->nSectorNumber))
      )
   {
      PDebugError("static_P15WriteInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the action */
   if ((bLockSectors != W_FALSE) && (pBuffer==null) && (nLength == 0))
   {
      nLength = p15P3UserConnection->nSectorSize * p15P3UserConnection->nSectorNumber;
   }

   /* Check if the Mifare card is locked/lockable */
   for (nIndex=(nOffset / p15P3UserConnection->nSectorSize); nIndex <= ((nOffset + nLength - 1) / p15P3UserConnection->nSectorSize); nIndex++)
   {
      if ( p15P3UserConnection->bIsSectorLocked[nIndex] != W_FALSE )
      {
         PDebugError("static_P15WriteInternalEx: item locked");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }
   }

   /* Create the smart cache if not already created */
   if (p15P3UserConnection->bIsSmartCacheInitialized == W_FALSE)
   {
      nError = static_P15P3UserCreateSmartCache(pContext, p15P3UserConnection);
      if (nError != W_SUCCESS)
      {
         PDebugError("static_P15WriteInternalEx: static_P15P3userCreateSmartCache failed");
         goto return_error;
      }
   }

   /* Set the connection information */
   p15P3UserConnection->bLockSectors      = bLockSectors;
   p15P3UserConnection->nLockBlockStart   = (uint8_t)(nOffset / p15P3UserConnection->nSectorSize);
   p15P3UserConnection->nLockBlockEnd     = (uint8_t)(nOffset + nLength - 1) / p15P3UserConnection->nSectorSize;

   /* Store the callback context */
   p15P3UserConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P15P3SendResultInternal when the operation is completed */
   PHandleIncrementReferenceCount(p15P3UserConnection);

   /* Check if we can lock the sectors directly */
   if ( (bLockSectors != W_FALSE) && (pBuffer == null))
   {
      /* Lock the sectors */
      static_P15P3UserSendCommand(pContext, p15P3UserConnection, P_15693_COMMAND_LOCK );
   }
   else
   {
      /* Write data */
      PSmartCacheWrite(pContext, &p15P3UserConnection->sSmartCache, nOffset, nLength, pBuffer, static_P15WriteInternalCompleted, p15P3UserConnection);
   }

   return;

return_error:
   PDebugError("static_P15WriteInternalEx: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header */
void P15WriteInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockSectors)
{
   tP15P3UserConnection* p15P3UserConnection = null;

   /* Check if the connection handle is valid */
   if (PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection) != W_SUCCESS)
   {
      p15P3UserConnection = null;
   }

   static_P15WriteInternalEx(
         pContext,
         p15P3UserConnection,
         pCallback, pCallbackParameter,
         pBuffer,
         nOffset,
         nLength,
         bLockSectors);
}

void P15Write(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockSectors,
            W_HANDLE *phOperation )
{
   tP15P3UserConnection* p15P3UserConnection;
   tDFCCallbackContext sCallbackContext;
   uint32_t nIndex = 0x00;
   W_ERROR nError = W_SUCCESS;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;

   PDebugTrace("P15Write [%d-%d], lock : %s", nOffset, nOffset+nLength-1, bLockSectors != W_FALSE ? "W_TRUE": "W_FALSE");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("P15Write: wrong connection handle");
      goto return_error;
   }

   /* Check the protocol type */
   if ( p15P3UserConnection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("P15Write: W_ERROR_CONNECTION_COMPATIBILITY");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the memory information */
   if (  ( p15P3UserConnection->nSectorSize   == 0x00 )
      || ( p15P3UserConnection->nSectorNumber == 0x00 ) )
   {
      PDebugError("P15Write: unknown memory information");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockSectors == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("P15Write: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockSectors == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("P15Write: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (     ((bLockSectors == W_FALSE) && ((pBuffer == null) || (nLength == 0)))
         || ((nOffset+nLength) > (uint32_t)(p15P3UserConnection->nSectorSize * p15P3UserConnection->nSectorNumber))
      )
   {
      PDebugError("P15Write: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the action */
   if ((bLockSectors != W_FALSE) && (pBuffer==null) && (nLength == 0))
   {
      nLength = p15P3UserConnection->nSectorSize * p15P3UserConnection->nSectorNumber;
   }

   /* Check if the Mifare card is locked/lockable */
   for (nIndex=(nOffset / p15P3UserConnection->nSectorSize); nIndex <= ((nOffset + nLength - 1) / p15P3UserConnection->nSectorSize); nIndex++)
   {
      if ( p15P3UserConnection->bIsSectorLocked[nIndex] != W_FALSE )
      {
         PDebugError("P15Write: item locked");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("P15Write: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("P15Write: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_P15ExecuteQueuedExchange, p15P3UserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p15P3UserConnection);

      /* Store the operation handle */
      CDebugAssert(p15P3UserConnection->hCurrentOperation == W_NULL_HANDLE);
      p15P3UserConnection->hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      p15P3UserConnection->sCallbackContext = sCallbackContext;

      static_P15WriteInternalEx(
            pContext,
            p15P3UserConnection,
            static_P15P3UserCommandCompleted, p15P3UserConnection,
            pBuffer,
            nOffset,
            nLength,
            bLockSectors);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_P15P3UserSendError when the operation is completed */
      PHandleIncrementReferenceCount(p15P3UserConnection);

      /* Save the operation handle */
      CDebugAssert(p15P3UserConnection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      p15P3UserConnection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      p15P3UserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      p15P3UserConnection->sQueuedOperation.nType = P_15_QUEUED_WRITE;

      /* Save data */
      p15P3UserConnection->sQueuedOperation.pBuffer = (uint8_t*) pBuffer;
      p15P3UserConnection->sQueuedOperation.nOffset = nOffset;
      p15P3UserConnection->sQueuedOperation.nLength = nLength;
      p15P3UserConnection->sQueuedOperation.bLockSectors = bLockSectors;

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
   PDebugError("P15Write: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError );

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}


/* See Client API Specifications */
W_ERROR W15ReadSync(
            W_HANDLE hConnection,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      W15Read(
         hConnection,
         PBasicGenericSyncCompletion,
         &param,
         pBuffer, nOffset, nLength,
         null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR W15SetAttributeSync(
            W_HANDLE hConnection,
            uint8_t nActions,
            uint8_t nAFI,
            uint8_t nDSFID )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      W15SetAttribute(
         hConnection,
         PBasicGenericSyncCompletion,
         &param,
         nActions,
         nAFI,
         nDSFID,
         null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR W15WriteSync(
            W_HANDLE hConnection,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockSectors )
{
    tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      W15Write(
         hConnection,
         PBasicGenericSyncCompletion,
         &param,
         pBuffer, nOffset, nLength,
         bLockSectors,
         null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See header file */
W_ERROR P15InvalidateCache(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nOffset,
            uint32_t nLength)
{
   tP15P3UserConnection* p15P3UserConnection;
   W_ERROR nError;

   PDebugTrace("P15InvalidateCache");

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("P15InvalidateCache: Bad handle");
      return nError;
   }

   PSmartCacheInvalidateCache(pContext, & p15P3UserConnection->sSmartCache, nOffset, nLength);
   return W_SUCCESS;
}

/* See header file */
void P15P3UserCreateSecondaryConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   tP15P3UserConnection* p15P3UserConnection;
   W_ERROR               nError;
   tDFCCallbackContext   sCallbackContext;

   PDebugTrace("P15InvalidateCache : nProperty %d", nProperty);

   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if ((nError != W_SUCCESS) || (p15P3UserConnection == null))
   {
      PDebugError("P15P3CreateSecondaryConnection : bad connection handle");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto send_result;
   }

   if (p15P3UserConnection->nPendingCardSecondaryConnection != nProperty)
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto send_result;
   }

   p15P3UserConnection->nCardSecondaryConnection = p15P3UserConnection->nPendingCardSecondaryConnection;
   nError = W_SUCCESS;


send_result:

   /* Send the result */

   PDFCPostContext2(
      & sCallbackContext,
      nError );

}

/** See tPReaderUserRemoveSecondaryConnection */
void P15P3UserRemoveSecondaryConnection(
            tContext* pContext,
            W_HANDLE hConnection )
{
   tP15P3UserConnection* p15P3UserConnection;
   W_ERROR               nError;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_15693_USER_CONNECTION, (void**)&p15P3UserConnection);

   if ((nError != W_SUCCESS) || (p15P3UserConnection == null))
   {
      PDebugError("P15P3UserRemoveSecondaryConnection : bad connection handle");
      return;
   }

   p15P3UserConnection->nCardSecondaryConnection = 0;
}

/* Polling command's callback */
static void static_P15P3PollCompleted(
      tContext * pContext,
      void * pCallbackParameter,
      W_ERROR nError)
{
   tP15P3UserConnection * p15P3UserConnection = (tP15P3UserConnection *) pCallbackParameter;

   PDebugTrace("static_P15P3PollCompleted");

   /* Send the error */
   PDFCPostContext2(&p15P3UserConnection->sCallbackContext, nError);

   /* Release the reference after completion. May destroy the object */
   PHandleDecrementReferenceCount(pContext, p15P3UserConnection);
}

/* Send polling command */
static void static_P15P3Poll(
      tContext * pContext,
      void* pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tP15P3UserConnection * p15P3UserConnection = (tP15P3UserConnection *) pObject;

   PDebugTrace("static_P15P3Poll");

   CDebugAssert( p15P3UserConnection != null
              && p15P3UserConnection->nProtocol== W_PROP_ISO_15693_3);

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_P15P3PollCompleted callback  */
   PHandleIncrementReferenceCount(p15P3UserConnection);

   /* store the callback context */
   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &p15P3UserConnection->sCallbackContext );

   /* Send the command */
   PSmartCacheInvalidateCache(
      pContext,
      &p15P3UserConnection->sSmartCache,
      0,
      1);

   static_P15P3AtomicRead(
      pContext,
      p15P3UserConnection,
      0,
      1,
      p15P3UserConnection->aCardToReaderBuffer15693,
      static_P15P3PollCompleted,
      p15P3UserConnection);
}

/* Execute the queued operation (after polling) */
static void static_P15ExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tP15P3UserConnection * p15P3UserConnection = (tP15P3UserConnection *) pObject;

   PDebugTrace("static_P15ExecuteQueuedExchange");

   /* Restore operation handle */
   p15P3UserConnection->hCurrentOperation = p15P3UserConnection->sQueuedOperation.hCurrentOperation;
   /* Restore callback context */
   p15P3UserConnection->sCallbackContext = p15P3UserConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (p15P3UserConnection->hCurrentOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, p15P3UserConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_P15ExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_P15P3UserCommandCompleted(pContext, p15P3UserConnection, nResult);
   }
   else
   {
      switch (p15P3UserConnection->sQueuedOperation.nType)
      {
      case P_15_QUEUED_READ:
         /* Read */
         static_P15ReadInternalEx(
               pContext,
               p15P3UserConnection,
               static_P15P3UserCommandCompleted, p15P3UserConnection,
               p15P3UserConnection->sQueuedOperation.pBuffer,
               p15P3UserConnection->sQueuedOperation.nOffset,
               p15P3UserConnection->sQueuedOperation.nLength);

         break;

      case P_15_QUEUED_WRITE:
         /* Write */
         static_P15WriteInternalEx(
               pContext,
               p15P3UserConnection,
               static_P15P3UserCommandCompleted, p15P3UserConnection,
               p15P3UserConnection->sQueuedOperation.pBuffer,
               p15P3UserConnection->sQueuedOperation.nOffset,
               p15P3UserConnection->sQueuedOperation.nLength,
               p15P3UserConnection->sQueuedOperation.bLockSectors);

         break;

      case P_15_QUEUED_SET_ATTRIBUTE:
         /* Set attribute */
         static_P15SetAttributeInternalEx(
            pContext,
            p15P3UserConnection,
            static_P15P3UserCommandCompleted,
            p15P3UserConnection,
            p15P3UserConnection->sQueuedOperation.nActions,
            p15P3UserConnection->sQueuedOperation.nAFI,
            p15P3UserConnection->sQueuedOperation.nDSFID);

         break;

      default:
         /* Return an error */
         PDebugError("static_P15ExecuteQueuedExchange: unknown type of operation!");
         static_P15P3UserCommandCompleted(pContext, p15P3UserConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&p15P3UserConnection->sQueuedOperation, 0, sizeof(p15P3UserConnection->sQueuedOperation));
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

