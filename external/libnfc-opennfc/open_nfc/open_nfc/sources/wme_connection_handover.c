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
   Contains the implementation of the Connection Handover functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( HANDOVER )

#include "wme_context.h"
#include "wme_connection_handover.h"

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC))  && (defined P_INCLUDE_PAIRING)

#define P_HANDOVER_MAX_MIU          128

#define P_NDEF_FLAG_MESSAGE_BEGIN   0x80
#define P_NDEF_FLAG_MESSAGE_END     0x40
#define P_NDEF_FLAG_CHUNK_FLAG      0x20
#define P_NDEF_FLAG_SHORT_RECORD    0x10
#define P_NDEF_ID_LENGTH_PRESENT    0x08
#define P_NDEF_MASK_TNF             0x07

static const uint8_t static_aReaderPropertiesSupported[]       = {W_PROP_NFC_TAGS};

static const char16_t static_aHandoverServiceName[]            = {'u','r','n',':','n','f','c',':','s','n',':','h','a','n','d','o','v','e','r'};

static const uint8_t static_aHandoverRequestType[]             = {'H','r'};
static const uint8_t static_aHandoverSelectType[]              = {'H','s'};
static const uint8_t static_aHandoverCollisionRecordType[]     = {'c','r'};
static const uint8_t static_aHandoverAlternativeCarrierType[]  = {'a','c'};
static const uint8_t static_aHandoverErrorRecordType[]         = {'e','r','r'};

#define P_HANDOVER_REQUEST_TYPE              static_aHandoverRequestType
#define P_HANDOVER_SELECT_TYPE               static_aHandoverSelectType
#define P_HANDOVER_COLLISON_RECORD_TYPE      static_aHandoverCollisionRecordType
#define P_HANDOVER_ALTERNATIVE_CARRIER_TYPE  static_aHandoverAlternativeCarrierType
#define P_HANDOVER_ERROR_RECORD_TYPE         static_aHandoverErrorRecordType

#define P_HANDOVER_CURRENT_VERSION           0x12 /* 1.2 */


#define P_HANDOVER_STATE_FREE                0x00
#define P_HANDOVER_STATE_PAIRING_STARTED     0x01
#define P_HANDOVER_STATE_WAIT_COMPLETION     0x02
#define P_HANDOVER_STATE_COMPLETION_STARTED  0x03
#define P_HANDOVER_STATE_COMPLETING          0x04


#define MAX(a,b) ((a < b)? b: a)
#define MIN(a,b) ((a > b)? b: a)

typedef struct __tPayloadReference
{
   uint8_t     nLength;
   uint8_t     * pName;
}tPayloadReference;



typedef struct __tPAlternativeCarrierRecord
{
   uint8_t            nCarrierPowerStateFlags;

   /* Record Name */
   tPHandoverCarrierType sCarrierType;

   /* Carrier Data reference */
   tPayloadReference  sPayloadReference;
   tPHandoverConfigurationData       sConfigurationData;

   /* Auxiliary Data Reference */
   uint8_t            nAuxiliaryDataReferenceCount;
   tPayloadReference* pAuxiliaryPayloadReference;
   tPHandoverConfigurationData*      pAuxiliaryConfigurationData;

   struct __tPAlternativeCarrierRecord * pNextCarrier;

}tPAlternativeCarrierRecord;


typedef struct __tPHandoverMessage
{
   bool_t        bIsRequestMessage;


   uint8_t     nVersion;
   union
   {
      uint16_t    nCollisionResolutionRecord;
      /* Error Management */
      tPHandoverError * pError;
   }uSpecificData;


   /* Array of Carrier Record */
   tPAlternativeCarrierRecord * pAlternativeCarrierRecord;
}tPHandoverMessage;

typedef struct __tPHandoverUserConnection
{
   /* Connection registry handle */
   tHandleObjectHeader sObjectHeader;
   W_HANDLE            hVirtualTag;
   W_HANDLE            hReaderRegistry;

   W_HANDLE            hReadOperation;
   W_HANDLE            hConnection;
   W_HANDLE            hWriteOperation;
   W_HANDLE            hSocket;             /* P2P socket */
   W_HANDLE            hLinkOperation;      /* P2P link establisment operation */
   W_HANDLE            hLink;               /* P2P link handle */

   W_HANDLE            hStopPairing;

   /* Used for passing nError */
   W_ERROR             nError;

   /* Callback context */
   tDFCCallbackContext sCallbackContext;

   /* Message set by the User information */
   tPHandoverMessage   sHandoverMessageSent;

   /* Select Message */
   tPHandoverMessage   sHandoverMessageReceived;

   /* For exchanging data */
   uint8_t  aTransferBuffer[P_HANDOVER_MAX_MIU];

   /* Used to save nb Data overRead*/
   uint32_t nLengthTransfered;
   uint8_t* pP2PMessageBuffer;
   uint32_t nP2PMessageBufferLength;

   uint32_t nLengthVerified;
   uint8_t* pP2PMessageRead;
   uint32_t nP2PMessageReadLength;



   uint8_t  nState;
   bool_t     bIsRequester;
   uint8_t  nCarrierNumberTransfered;

   uint8_t  nCarrierRecordAdded;

   uint32_t  nMode;

}tPHandoverUserConnection;



/* Destroy connection callback */
static uint32_t static_PHandoverUserDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Connection registry tPHandoverUserConnection type */
tHandleType g_sHandoverUserConnection = { static_PHandoverUserDestroyConnection,
                                          null, null, null, null, null, null, null, null };

static void static_PHandoverVirtualTagStopAndCloseCompleted(
            tContext* pContext,
            void* pObject,
            W_ERROR nError);

static void static_PHandoverStopPairingStart(
            tContext* pContext,
            void* pCallbackParameter,
            bool_t bIsClosing);

static void static_PHandoverStopPairingCompletion(
            tContext* pContext,
            void* pCallbackParameter,
            bool_t bIsClosing);

static tPAlternativeCarrierRecord * static_PDestroyAlternativeCarrierRecord(
                  tPAlternativeCarrierRecord * pAlternativeCarrierRecord);

static void static_PHandoverRemoveMessage(
                  tPHandoverMessage * pMessage);

static uint32_t static_PHandoverUserDestroyConnection(
                  tContext* pContext,
                  void* pObject );

static bool_t static_PHandoverFlushHandle(tContext * pContext,
                                        tPHandoverUserConnection * pUserConnection);

static tPAlternativeCarrierRecord * static_PHandoverSearchAlternativeRecord(
                  tPAlternativeCarrierRecord * pAlternativeRecord,
                  tPHandoverCarrierType * pCarrierSearched);

static void static_PHandoverSearchMatchingCarrier(
                  tPAlternativeCarrierRecord **ppCarrierFiltred,
                  tPAlternativeCarrierRecord *pCarrierAuthorized);

static uint32_t static_PHandoverGetAlternativeCarrierRecordLength(
                  tPAlternativeCarrierRecord * pAlternativeCarrier);

static uint32_t static_PHandoverGetSelectMessageLength(
                  tPHandoverMessage * pSelectMessage);

static uint32_t static_PHandoverGetRequestMessageLength(
                  tPHandoverMessage * pRequestMessage);

static uint32_t static_PHandoverBuildAlternativeRecord(
                  tPAlternativeCarrierRecord * pAlternativeRecord,
                  uint8_t  nNDEFHeaderFalgs,
                  uint8_t * pBuffer);

static uint32_t static_PHandoverBuildConfigurationDataRecord(
                  tPHandoverCarrierType * pCarrierType,
                  tPayloadReference * pPayloadReference,
                  tPHandoverConfigurationData * pConfigurationData,
                  uint8_t  nNDEFHeaderFalgs,
                  uint8_t * pBuffer);

static uint8_t * static_PHandoverBuildSelectMessage(
                  tPHandoverMessage * pSelectMessage,
                  uint32_t * nLength);

static uint8_t * static_PHandoverBuildRequestMessage(
                  tPHandoverMessage * pRequestMessage,
                  uint32_t * nLength);

static W_ERROR static_PHandoverGetConfigurationDataByPayloadID(
                  tPAlternativeCarrierRecord * pAlternativeCarrier,
                  tPayloadReference * pPayloadReference,
                  tPAlternativeCarrierRecord ** ppAlternativeCarrierReturned,
                  tPHandoverConfigurationData ** ppConfigurationDataReturned);

static W_ERROR static_PHandoverParseCarrierConfigurationData(
                  tPAlternativeCarrierRecord * pAlternativeCarrier,
                  uint8_t * pConfigurationRecord,
                  uint32_t nLength,
                  uint32_t * nEffectiveLength);

static W_ERROR static_PHandoverParseAlternativeCarrierRecord(
                  tPAlternativeCarrierRecord * *ppAlternativeRecord,
                  uint8_t * pAlternativeCarrierData,
                  uint32_t nLength,
                  uint32_t * nEffectiveLength);

static W_ERROR static_PHandoverParseSelectMessage(
                  tPHandoverMessage * pSelectMessage,
                  uint8_t * pBuffer,
                  uint32_t nLength);

static W_ERROR static_PHandoverParseRequestMessage(
                  tPHandoverMessage * pRequestMessage,
                  uint8_t * pBuffer,
                  uint32_t nLength);

static W_ERROR static_PHandoverParseMessage(
                  tPHandoverMessage * pHandoverMessage,
                  uint8_t* pNDEFMessageRead,
                  uint32_t pNDEFMessageReadLength);

static uint32_t static_PHandoverGetDataLengthOfAlternativeCarrier(
                  tPAlternativeCarrierRecord * pAlternativeRecord);

static void static_PHandoverPairingStarted(
                  tContext * pContext,
                  tPHandoverUserConnection * pUserConnection);

static void static_PHandoverPairingCompleted(
                  tContext * pContext,
                  tPHandoverUserConnection * pUserConnection,
                  W_ERROR nError);

static void static_PHandoverVirtualTagCompleted(
                  tContext* pContext,
                  void* pObject,
                  W_ERROR nError);

static void static_PHandoverVirtualTagEventReceived(
                  tContext* pContext,
                  void* pObject,
                  W_ERROR nEventCode);

static void static_PHandoverVirtualTagStartCompleted(
                  tContext* pContext,
                  void* pObject,
                  W_ERROR nError);

static void static_PHandoverWriteMessageCompleted(
                  tContext* pContext,
                  void* pObject,
                  W_ERROR nError);

static void  static_PHandoverReadSelectMessageCompleted(
                  tContext* pContext,
                  void * pCallbackParameter,
                  W_HANDLE hMessage,
                  W_ERROR nError );

static void static_PHandoverProcessSelectReceived(
                  tContext * pContext,
                  tPHandoverUserConnection * pHandoverUserConnection);


static void static_PHandoverP2PReadSelectMessage(
                  tContext *  pContext,
                  void *      pCallbackParameter,
                  uint32_t    nLength,
                  W_ERROR     nError);

static void static_PHandoverSelectMessageSent(
                  tContext *  pContext,
                  void * pCallbackParameter,
                  W_ERROR  nError);

static void static_PHandoverP2PConnectionEstablished(
                  tContext * pContext,
                  void     * pCallbackParameter,
                  W_ERROR     nError);

static void static_PHandoverP2PLinkEstablished(
                  tContext* pContext,
                  void * pCallbackParameter,
                  W_HANDLE hHandle,
                  W_ERROR nResult );

static void static_PHandoverProcessRequestReceived(
                  tContext * pContext,
                  tPHandoverUserConnection * pHandoverUserConnection);

static void static_PHandoverP2PFirstReadResponseCompleted(
                  tContext *  pContext,
                  void *      pCallbackParameter,
                  uint32_t    nLength,
                  W_ERROR     nError);

static void static_PHandoverP2PRequestMessageSent(
                  tContext * pContext,
                  void     * pCallbackParameter,
                  W_ERROR     nError);


static void static_PHandoverP2PShutdownCompleted(
                  tContext * pContext,
                  void * pCallbackParameter,
                  W_ERROR nError);


static void static_PHandoverP2PLinkReleased(
                  tContext* pContext,
                  void * pCallbackParameter,
                  W_ERROR nResult );


/**
 * This function notify the selector during a handover connection between
 * 2 NFC Forum Devices
 **/
static void static_PHandoverPairingStarted(
            tContext * pContext,
            tPHandoverUserConnection * pUserConnection)
{
   PDebugTrace ("####################### static_PHandoverPairingStarted ###################");

   /* if the operation has been closed */
   if(pUserConnection->nState == P_HANDOVER_STATE_FREE)
   {
      return;
   }

   pUserConnection->nState = P_HANDOVER_STATE_WAIT_COMPLETION;

   /* Remove carriers configuration Data Sent */
   static_PHandoverRemoveMessage(&pUserConnection->sHandoverMessageSent);

   PDFCPostContext2( &pUserConnection->sCallbackContext,
                     W_SUCCESS);

   PHandleDecrementReferenceCount(pContext, pUserConnection);
}


/**
 * @brief Close  pending operation and close all opened handle. Return after the first close operation pending.
 *
 * @param pContext Current context
 * @param pHandoverUserConnection the current Handover Connection structure
 * @return W_TRUE if all operation is canceled and if all opened of the current handover connection is used.
 *         W_FALSE if a pending operation is canceled and closed.
 *
 **/
static bool_t static_PHandoverFlushHandle(tContext * pContext,
                                        tPHandoverUserConnection * pUserConnection)
{
   switch(pUserConnection->nMode)
   {
      case W_HANDOVER_PAIRING_READER:
         if(pUserConnection->hReaderRegistry != W_NULL_HANDLE)
         {
            PHandleClose(
                  pContext,
                  pUserConnection->hReaderRegistry);

            pUserConnection->hReaderRegistry = W_NULL_HANDLE;
         }
         break;


      case W_HANDOVER_PAIRING_VIRTUAL_TAG:
         if(pUserConnection->hVirtualTag != W_NULL_HANDLE)
         {
            PVirtualTagStop(pContext,
                            pUserConnection->hVirtualTag,
                            static_PHandoverVirtualTagStopAndCloseCompleted,
                            pUserConnection);
         }
         break;
      case W_HANDOVER_PAIRING_P2P_ANY:

         if(pUserConnection->hReadOperation != W_NULL_HANDLE)
         {
            /* Handle closed by the read operation */
            PBasicCancelOperation(pContext, pUserConnection->hReadOperation);
            return W_FALSE;
         }

         if(pUserConnection->hWriteOperation != W_NULL_HANDLE)
         {
            /* Handle closed by the write operation */
            PBasicCancelOperation(pContext, pUserConnection->hWriteOperation);

            return W_FALSE;
         }

         if(pUserConnection->hLinkOperation != W_NULL_HANDLE)
         {
            /* Handle closed by the current established link Operation */
            PBasicCancelOperation(pContext,pUserConnection->hLinkOperation);
            return W_FALSE;
         }

         if(pUserConnection->pP2PMessageBuffer != null)
         {
            CMemoryFree(pUserConnection->pP2PMessageBuffer);
            pUserConnection->pP2PMessageBuffer = null;
         }

         if(pUserConnection->pP2PMessageRead != null)
         {
            CMemoryFree(pUserConnection->pP2PMessageRead);
            pUserConnection->pP2PMessageRead = null;
         }
         pUserConnection->nP2PMessageReadLength = 0;
         pUserConnection->nP2PMessageBufferLength = 0;

         pUserConnection->nLengthVerified = 0;

         pUserConnection->hStopPairing = W_NULL_HANDLE;

         if(pUserConnection->hSocket != W_NULL_HANDLE)
         {
            /* Close by the PP2PShutdown callback */
            PP2PShutdown(  pContext,
                  pUserConnection->hSocket,
                  static_PHandoverP2PShutdownCompleted,
                  pUserConnection);
            return W_FALSE;
         }

         if(pUserConnection->hLink != W_NULL_HANDLE)
         {
            PHandleClose(pContext, pUserConnection->hLink);
            pUserConnection->hLink = W_NULL_HANDLE;
         }
         break;

         /* No default case */
   }

   return W_TRUE;

}

/**
 *This function is used when the pairing complete
 **/
static void static_PHandoverPairingCompleted(
            tContext * pContext,
            tPHandoverUserConnection * pUserConnection,
            W_ERROR nError)
{
   PDebugTrace ("static_PHandoverPairingCompleted");

   if(nError != W_SUCCESS){
      PDebugTrace ("static_PHandoverPairingCompleted Error : %d", nError);
   }

   /* If pairing is not pending, nothing should be done */
   if(pUserConnection->nState == P_HANDOVER_STATE_FREE)
   {
      return;
   }

   /* if it is the first call of this function, I need to store the
      error returned code and the new state */
   if(pUserConnection->nState != P_HANDOVER_STATE_COMPLETING)
   {
      pUserConnection->nError = nError;
      pUserConnection->nState = P_HANDOVER_STATE_COMPLETING;
   }



   /* Flush Handle and running operation
      if an operation is running a cancel operation is fired. This function return and be recalled
      by the canceled operation */
   if(static_PHandoverFlushHandle(pContext, pUserConnection) == W_FALSE)
   {
      return;
   }


   /* Remove carriers configuration Data Sent */
   static_PHandoverRemoveMessage(&pUserConnection->sHandoverMessageSent);

   pUserConnection->nState = P_HANDOVER_STATE_FREE;

   PDFCPostContext2( &pUserConnection->sCallbackContext,
                     pUserConnection->nError);

   PHandleDecrementReferenceCount(pContext, pUserConnection);
}

static void static_PHandoverP2PShutdownCompleted(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{

   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;
   PDebugTrace ("static_PHandoverP2PShutdown Completed");

   if(nError != W_SUCCESS){
      PDebugTrace ("static_PHandoverP2PShutdown Error : %d", nError);
   }

   PHandleClose(pContext, pHandoverUserConnection->hSocket);
   pHandoverUserConnection->hSocket = W_NULL_HANDLE;

   static_PHandoverPairingCompleted(
            pContext,
            pHandoverUserConnection,
            /* nError */W_SUCCESS);
}

/**
 *@brief Free an Alternative carrier Record and free each elements of this structure
 *
 *@param in : pAlternativeCarrierRecord to free
**/
static tPAlternativeCarrierRecord * static_PDestroyAlternativeCarrierRecord(tPAlternativeCarrierRecord * pAlternativeCarrierRecord)
{
   tPAlternativeCarrierRecord * pReturned = pAlternativeCarrierRecord->pNextCarrier;
   uint8_t nIndex = 0;
   PDebugTrace ("static_PDestroyAlternativeCarrierRecord");

   if(pAlternativeCarrierRecord->nAuxiliaryDataReferenceCount > 0)
   {
      while(nIndex < pAlternativeCarrierRecord->nAuxiliaryDataReferenceCount)
      {
         CMemoryFree(pAlternativeCarrierRecord->pAuxiliaryPayloadReference[nIndex].pName);
         pAlternativeCarrierRecord->pAuxiliaryPayloadReference[nIndex++].pName = 0;
      }

      /* Remove AC */
      CMemoryFree(pAlternativeCarrierRecord->pAuxiliaryPayloadReference);

      /* Remove Auxiliary data */
      if(pAlternativeCarrierRecord->pAuxiliaryConfigurationData != null)
      {
         CMemoryFree(pAlternativeCarrierRecord->pAuxiliaryConfigurationData);
      }
   }

   CMemoryFree(pAlternativeCarrierRecord->sPayloadReference.pName);
   CMemoryFree(pAlternativeCarrierRecord->sCarrierType.pName);
   CMemoryFree(pAlternativeCarrierRecord);

   return pReturned;
}

/**
 * @brief Clean up Handover Message
 **/
static void static_PHandoverRemoveMessage(tPHandoverMessage * pMessage)
{
   tPAlternativeCarrierRecord * pAlternativeCarrier = pMessage->pAlternativeCarrierRecord;

   PDebugTrace("static_PHandoverRemoveMessage");

   pMessage->nVersion = P_HANDOVER_CURRENT_VERSION;
   pMessage->uSpecificData.nCollisionResolutionRecord = 0x00;

   while(pAlternativeCarrier != null)
   {
      pAlternativeCarrier = static_PDestroyAlternativeCarrierRecord(pAlternativeCarrier);
   }
   pMessage->pAlternativeCarrierRecord = null;

   if(pMessage->uSpecificData.pError != null)
   {
      CMemoryFree(pMessage->uSpecificData.pError);
   }

   pMessage->bIsRequestMessage = W_TRUE;
}

/**
 * @brief Search an AlternativeCarrierRecord in a list of AlternativeCarrierRecord by the carrierType
 **/
static tPAlternativeCarrierRecord * static_PHandoverSearchAlternativeRecord(
   tPAlternativeCarrierRecord * pAlternativeRecord,
         tPHandoverCarrierType * pCarrierSearched)
{
   PDebugTrace ("static_PHandoverSearchAlternativeRecord");

   while(pAlternativeRecord != null)
   {
      if( (pAlternativeRecord->sCarrierType.nLength == pCarrierSearched->nLength)
         && (CMemoryCompare(
               pAlternativeRecord->sCarrierType.pName,
               pCarrierSearched->pName,
               pCarrierSearched->nLength) == 0x00))
      {
         return pAlternativeRecord;
      }

      pAlternativeRecord = pAlternativeRecord->pNextCarrier;
   }

   return pAlternativeRecord;
}

/**
 *@brief Match 2 list of alternative carrier Record
 **/
static void static_PHandoverSearchMatchingCarrier(
               tPAlternativeCarrierRecord **ppCarrierFiltred,
               tPAlternativeCarrierRecord *pCarrierAuthorized)
{
   tPAlternativeCarrierRecord * pCurrent;

   PDebugTrace("static_PHandoverSearchMatchingCarrier");
   while(*ppCarrierFiltred != null)
   {
      pCurrent = * ppCarrierFiltred;

      /* if the filtred carrier exist, you take the next */
      if(static_PHandoverSearchAlternativeRecord(
               pCarrierAuthorized,
               &pCurrent->sCarrierType) != null)
      {
         ppCarrierFiltred = &((*ppCarrierFiltred)->pNextCarrier);
      }
      else
      {
         *ppCarrierFiltred = static_PDestroyAlternativeCarrierRecord(*ppCarrierFiltred);
      }
   }
}

/**
 * @brief Calculate the length for a list of alternativeCarrier
 **/
static uint32_t   static_PHandoverGetAlternativeCarrierRecordLength(
            tPAlternativeCarrierRecord * pAlternativeCarrier)
{
   uint8_t nIndex = 0;
   uint32_t nLength = 0;

   /* AlternativeCarrier */
   while(pAlternativeCarrier != null)
   {

      /* Alternalive carrier type records */
      nLength += 3;                                                     /* NDEF Header + RecordTypeLength + PayloadLength */
      nLength += sizeof(P_HANDOVER_ALTERNATIVE_CARRIER_TYPE);
      nLength += 1;                                                     /* CPS */
      nLength += 1 + pAlternativeCarrier->sPayloadReference.nLength;/* Length + Data */
      nLength += 1;                                                     /* nAuxiliary Data Reference */

      /* Configuration of carrier record */
      nLength += 4;                                                     /* NDEF Header + RecordTypeLength + PayloadLength + Payload Id Length*/
      nLength += pAlternativeCarrier->sCarrierType.nLength;             /* Carrier Type Length */
      nLength += pAlternativeCarrier->sPayloadReference.nLength;    /* Length */
      nLength += pAlternativeCarrier->sConfigurationData.nLength;


      nIndex = 0;
      while(nIndex < pAlternativeCarrier->nAuxiliaryDataReferenceCount)
      {
         nLength += 1 + pAlternativeCarrier->pAuxiliaryPayloadReference[nIndex].nLength; /* Length + its data */

         /* Configuration of carrier record */
         nLength += 4;                                                           /* NDEF Header + RecordTypeLength + PayloadLength + Payload Id Length*/
         nLength += pAlternativeCarrier->sCarrierType.nLength;                   /* Carrier Type Length */
         nLength += pAlternativeCarrier->pAuxiliaryPayloadReference[nIndex].nLength;/* Carrier Data reference length */
         nLength += pAlternativeCarrier->pAuxiliaryConfigurationData[nIndex].nLength;   /* Data Length */
         nIndex ++;
      }

      pAlternativeCarrier = pAlternativeCarrier->pNextCarrier;
   }

   return nLength;
}

/**
 * @brief Calculate the length of a Select message
 **/
static uint32_t   static_PHandoverGetSelectMessageLength(
            tPHandoverMessage * pSelectMessage)
{
   uint32_t nLength = 0;

   /* Header Select Message */
   nLength += 3;                             /* NDEF Header + RecordTypeLength + PayloadLength */
   nLength += sizeof(P_HANDOVER_SELECT_TYPE);
   nLength += 1;                             /* Version */

   nLength += static_PHandoverGetAlternativeCarrierRecordLength(pSelectMessage->pAlternativeCarrierRecord);

   if(pSelectMessage->uSpecificData.pError != null)
   {
      nLength += 3;
      nLength += sizeof(P_HANDOVER_ERROR_RECORD_TYPE);
      nLength += 1 + pSelectMessage->uSpecificData.pError->nErrorDataLength;
   }
   return nLength;
}

/**
 * @brief Calculate the length of a Request Message
 **/
static uint32_t   static_PHandoverGetRequestMessageLength(
            tPHandoverMessage * pRequestMessage)
{
   uint32_t nLength = 0;

   /* Header Select Message */
   nLength += 3;                             /* NDEF Header + RecordTypeLength + PayloadLength */
   nLength += sizeof(P_HANDOVER_SELECT_TYPE);
   nLength += 1;                             /* Version */

   /* Collision Record */
   nLength += 3;
   nLength += sizeof(P_HANDOVER_COLLISON_RECORD_TYPE);
   nLength += sizeof(pRequestMessage->uSpecificData.nCollisionResolutionRecord);

   nLength += static_PHandoverGetAlternativeCarrierRecordLength(pRequestMessage->pAlternativeCarrierRecord);

   return nLength;
}

/**
 * @brief Build an (only one) Alternative Carrer Record
 **/
static uint32_t static_PHandoverBuildAlternativeRecord(
         tPAlternativeCarrierRecord * pAlternativeRecord,
         uint8_t  nNDEFHeaderFalgs,
         uint8_t * pBuffer)
{
   uint32_t nOffset = 0;
   uint32_t nOffsetPayloadCarrierLength = 0;
   uint32_t nIndexAuxiliaryData = 0;
   uint32_t nOffsetStartData;

   /* NDEF Header */
   pBuffer[nOffset++]   =  ( nNDEFHeaderFalgs
                           | W_NDEF_TNF_WELL_KNOWN);

   /* REcord Type Length */
   pBuffer[nOffset++] = sizeof(P_HANDOVER_ALTERNATIVE_CARRIER_TYPE);

   /* PayloadLength Offset For alternative Carrier*/
   nOffsetPayloadCarrierLength = nOffset++;

   /* Record Type */
   CMemoryCopy(pBuffer + nOffset,
               P_HANDOVER_ALTERNATIVE_CARRIER_TYPE,
               sizeof(P_HANDOVER_ALTERNATIVE_CARRIER_TYPE));

   nOffset += sizeof(P_HANDOVER_ALTERNATIVE_CARRIER_TYPE);

   nOffsetStartData = nOffset;

   /* Carrier power state */
   pBuffer[nOffset++] = pAlternativeRecord->nCarrierPowerStateFlags;

   /* Carrier Reference */
   pBuffer[nOffset++] = pAlternativeRecord->sPayloadReference.nLength;
   CMemoryCopy(&pBuffer[nOffset],
               pAlternativeRecord->sPayloadReference.pName,
               pAlternativeRecord->sPayloadReference.nLength);

   nOffset +=  pAlternativeRecord->sPayloadReference.nLength;

   /* NAuxiliary Data Reference count */
   pBuffer[nOffset++] = pAlternativeRecord->nAuxiliaryDataReferenceCount;

   nIndexAuxiliaryData = 0;

   while(nIndexAuxiliaryData < pAlternativeRecord->nAuxiliaryDataReferenceCount)
   {
      /* Carrier Reference */
      pBuffer[nOffset++] = pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryData].nLength;
      CMemoryCopy(&pBuffer[nOffset],
                  pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryData].pName,
                  pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryData].nLength);

      nOffset += pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryData].nLength;

      nIndexAuxiliaryData ++;
   }

   /* Set the Payload length of th current Record */
   pBuffer[nOffsetPayloadCarrierLength] = (uint8_t)(nOffset - nOffsetStartData);

   return nOffset;
}

/**
 * @brief Build one configuration Data Record
 **/
static uint32_t static_PHandoverBuildConfigurationDataRecord(
         tPHandoverCarrierType * pCarrierType,
         tPayloadReference * pPayloadReference,
         tPHandoverConfigurationData * pConfigurationData,
         uint8_t  nNDEFHeaderFalgs,
         uint8_t * pBuffer)
{
   uint32_t nOffset = 0;

   /* NDEF Header */
   pBuffer[nOffset++]   = ( nNDEFHeaderFalgs
                          | W_NDEF_TNF_MEDIA
                          | P_NDEF_ID_LENGTH_PRESENT);

   pBuffer[nOffset++] = pCarrierType->nLength;
   pBuffer[nOffset++] = pConfigurationData->nLength;
   pBuffer[nOffset++] = pPayloadReference->nLength;

   CMemoryCopy(&pBuffer[nOffset],
               pCarrierType->pName,
               pCarrierType->nLength);
   nOffset += pCarrierType->nLength;

   CMemoryCopy(&pBuffer[nOffset],
               pPayloadReference->pName,
               pPayloadReference->nLength);
   nOffset += pPayloadReference->nLength;

   CMemoryCopy(&pBuffer[nOffset],
               pConfigurationData->pData,
               pConfigurationData->nLength);
   nOffset += pConfigurationData->nLength;

   return nOffset;
}

/**
 * @Build a Handover Select Message:
 *    - Make the memory Allocation
 *    - Build and copy the NDEF Message in the memory
 **/
static uint8_t * static_PHandoverBuildSelectMessage(
            tPHandoverMessage * pSelectMessage,
            uint32_t * nLength)
{
   uint32_t nOffset = 0;
   uint8_t nOffsetPayloadLength = 0;
   uint8_t nOffsetPayloadStarted = 0;
   uint8_t nAlternativeRecordFlags = 0;
   tPAlternativeCarrierRecord * pAlternativeRecord;

   uint8_t nIndexAuxiliaryConfigurationData = 0;

   uint8_t * pNDEFMessageData =
      (uint8_t *) CMemoryAlloc(static_PHandoverGetSelectMessageLength(pSelectMessage));

   if(pNDEFMessageData == null)
   {
      *nLength = 0;
      return pNDEFMessageData;
   }

   /* NDEF Record Header */
   if(pSelectMessage->uSpecificData.pError == null
      && pSelectMessage->pAlternativeCarrierRecord == null)
   {
      pNDEFMessageData[nOffset++] = ( P_NDEF_FLAG_MESSAGE_BEGIN
                                    | P_NDEF_FLAG_MESSAGE_END
                                    | P_NDEF_FLAG_SHORT_RECORD
                                    | W_NDEF_TNF_WELL_KNOWN);
   }
   else
   {
      pNDEFMessageData[nOffset++] = ( P_NDEF_FLAG_MESSAGE_BEGIN
                                    | P_NDEF_FLAG_SHORT_RECORD
                                    | W_NDEF_TNF_WELL_KNOWN);
   }


   /* Record Type Length */
   pNDEFMessageData[nOffset++] = sizeof(P_HANDOVER_SELECT_TYPE);

   /* Store the offset for payload length */
   nOffsetPayloadLength = (uint8_t)nOffset++;

   /* Record Type */
   CMemoryCopy(&pNDEFMessageData[nOffset],
                P_HANDOVER_SELECT_TYPE,
                sizeof(P_HANDOVER_SELECT_TYPE));

   nOffset += sizeof(P_HANDOVER_SELECT_TYPE);

   nOffsetPayloadStarted = (uint8_t)nOffset;

   /* Version Number */
   pNDEFMessageData[nOffset++] = pSelectMessage->nVersion;

   /********************************************************************/
   /*********               ALTERNATIVE CARRIER              ***********/
   /********************************************************************/
   pAlternativeRecord = pSelectMessage->pAlternativeCarrierRecord;

   /* For the first record */
   nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD | P_NDEF_FLAG_MESSAGE_BEGIN;

   while(pAlternativeRecord != null)
   {
      if(pAlternativeRecord->pNextCarrier == null)
      {
         nAlternativeRecordFlags |= P_NDEF_FLAG_MESSAGE_END;
      }

      nOffset +=
         static_PHandoverBuildAlternativeRecord(
         pAlternativeRecord,
         nAlternativeRecordFlags,
         &pNDEFMessageData[nOffset]);

      nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD;

      pAlternativeRecord = pAlternativeRecord->pNextCarrier;
   }

   /* Handover Request Message payload length */
   pNDEFMessageData[nOffsetPayloadLength] = (uint8_t) (nOffset - nOffsetPayloadStarted);

   /* Add Each carrier */
   pAlternativeRecord = pSelectMessage->pAlternativeCarrierRecord;
   while(pAlternativeRecord != null)
   {
      nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD;

      /* if it last record*/
      if(pAlternativeRecord->pNextCarrier == null
         && pAlternativeRecord->nAuxiliaryDataReferenceCount == 0
         && pSelectMessage->uSpecificData.pError == null)
      {
         nAlternativeRecordFlags |= P_NDEF_FLAG_MESSAGE_END;
      }

      nOffset += static_PHandoverBuildConfigurationDataRecord(
                     &pAlternativeRecord->sCarrierType,
                     &pAlternativeRecord->sPayloadReference,
                     &pAlternativeRecord->sConfigurationData,
                     nAlternativeRecordFlags,
                     &pNDEFMessageData[nOffset]);

      nIndexAuxiliaryConfigurationData = 0;

      while(nIndexAuxiliaryConfigurationData < pAlternativeRecord->nAuxiliaryDataReferenceCount)
      {
         nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD;

         if(   nIndexAuxiliaryConfigurationData == (pAlternativeRecord->nAuxiliaryDataReferenceCount - 1)
            && pAlternativeRecord->pNextCarrier == null
            && pSelectMessage->uSpecificData.pError == null)
         {
            nAlternativeRecordFlags |= P_NDEF_FLAG_MESSAGE_END;
         }

         nOffset += static_PHandoverBuildConfigurationDataRecord(
                     &pAlternativeRecord->sCarrierType,
                     &pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryConfigurationData],
                     &pAlternativeRecord->pAuxiliaryConfigurationData[nIndexAuxiliaryConfigurationData],
                     nAlternativeRecordFlags,
                     &pNDEFMessageData[nOffset]);

         nIndexAuxiliaryConfigurationData++;
      }

      pAlternativeRecord = pAlternativeRecord->pNextCarrier;
   }


   /* Add error Carrier */
   if(pSelectMessage->uSpecificData.pError != null)
   {
      pNDEFMessageData[nOffset++] = ( P_NDEF_FLAG_MESSAGE_BEGIN
                                    | P_NDEF_FLAG_MESSAGE_END
                                    | P_NDEF_FLAG_SHORT_RECORD
                                    | W_NDEF_TNF_WELL_KNOWN);

      pNDEFMessageData[nOffset++] = sizeof(P_HANDOVER_ERROR_RECORD_TYPE);
      pNDEFMessageData[nOffset++] = pSelectMessage->uSpecificData.pError->nErrorDataLength + 1;

      CMemoryCopy(&pNDEFMessageData[nOffset],
                  P_HANDOVER_ERROR_RECORD_TYPE,
                  sizeof(P_HANDOVER_ERROR_RECORD_TYPE));

      nOffset += sizeof(P_HANDOVER_ERROR_RECORD_TYPE);

      pNDEFMessageData[nOffset++] = pSelectMessage->uSpecificData.pError->nErrorReason;

      if(pSelectMessage->uSpecificData.pError->nErrorDataLength > 0)
      {
         CMemoryCopy(&pNDEFMessageData[nOffset],
                     pSelectMessage->uSpecificData.pError->aErrorData,
                     pSelectMessage->uSpecificData.pError->nErrorDataLength);

         nOffset += pSelectMessage->uSpecificData.pError->nErrorDataLength;
      }
   }

   *nLength = nOffset;

   return pNDEFMessageData;
}

/**
 * @Build a Handover Request Message:
 *    - Make the memory Allocation
 *    - Build and copy the NDEF Message in the memory
 **/
static uint8_t * static_PHandoverBuildRequestMessage(
            tPHandoverMessage * pRequestMessage,
            uint32_t * nLength)
{
   uint32_t nOffset = 0;
   uint8_t nOffsetPayloadLength = 0;
   uint8_t nOffsetPayloadStarted = 0;
   uint8_t nAlternativeRecordFlags = 0;
   tPAlternativeCarrierRecord * pAlternativeRecord;

   uint8_t nIndexAuxiliaryConfigurationData = 0;

   uint8_t * pNDEFMessageData =
      (uint8_t *) CMemoryAlloc(static_PHandoverGetRequestMessageLength(pRequestMessage));

   if(pNDEFMessageData == null)
   {
      *nLength = 0;
      return pNDEFMessageData;
   }

   /* NDEF Record Header */

   pNDEFMessageData[nOffset++] = ( P_NDEF_FLAG_MESSAGE_BEGIN
                                 | P_NDEF_FLAG_SHORT_RECORD
                                 | W_NDEF_TNF_WELL_KNOWN);


   /* Record Type Length */
   pNDEFMessageData[nOffset++] = sizeof(P_HANDOVER_REQUEST_TYPE);

   /* Store the offset for payload length */
   nOffsetPayloadLength = (uint8_t)nOffset++;

   /* Record Type */
   CMemoryCopy(&pNDEFMessageData[nOffset],
                P_HANDOVER_REQUEST_TYPE,
                sizeof(P_HANDOVER_REQUEST_TYPE));

   nOffset += sizeof(P_HANDOVER_REQUEST_TYPE);

   nOffsetPayloadStarted = (uint8_t)nOffset;

   /* Version Number */
   pNDEFMessageData[nOffset++] = pRequestMessage->nVersion;

   /********************************************************************/
   /*********               COLLISION RECORD                 ***********/
   /********************************************************************/

   pNDEFMessageData[nOffset++] = ( P_NDEF_FLAG_MESSAGE_BEGIN
                                 | P_NDEF_FLAG_SHORT_RECORD
                                 | W_NDEF_TNF_WELL_KNOWN);

   pNDEFMessageData[nOffset++] = sizeof(P_HANDOVER_COLLISON_RECORD_TYPE);
   pNDEFMessageData[nOffset++] = sizeof(pRequestMessage->uSpecificData.nCollisionResolutionRecord);

   /* Record Type */
   CMemoryCopy(&pNDEFMessageData[nOffset],
                P_HANDOVER_COLLISON_RECORD_TYPE,
                sizeof(P_HANDOVER_COLLISON_RECORD_TYPE));

   nOffset += sizeof(P_HANDOVER_COLLISON_RECORD_TYPE);

   pNDEFMessageData[nOffset++] = (uint8_t)((pRequestMessage->uSpecificData.nCollisionResolutionRecord >> 8) & 0x00FF);
   pNDEFMessageData[nOffset++] = (uint8_t)(pRequestMessage->uSpecificData.nCollisionResolutionRecord & 0x00FF);



   /********************************************************************/
   /*********               ALTERNATIVE CARRIER              ***********/
   /********************************************************************/
   pAlternativeRecord = pRequestMessage->pAlternativeCarrierRecord;

   /* For the first record */
   nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD;

   while(pAlternativeRecord != null)
   {
      if(pAlternativeRecord->pNextCarrier == null)
      {
         nAlternativeRecordFlags |= P_NDEF_FLAG_MESSAGE_END;
      }

      nOffset +=
         static_PHandoverBuildAlternativeRecord(
         pAlternativeRecord,
         nAlternativeRecordFlags,
         &pNDEFMessageData[nOffset]);

      nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD;

      pAlternativeRecord = pAlternativeRecord->pNextCarrier;
   }

   /* Handover Request Message payload length */
   pNDEFMessageData[nOffsetPayloadLength] = (uint8_t) (nOffset - nOffsetPayloadStarted);

   /* Add Each carrier */
   pAlternativeRecord = pRequestMessage->pAlternativeCarrierRecord;
   while(pAlternativeRecord != null)
   {
      nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD;

      /* if it last record*/
      if(pAlternativeRecord->pNextCarrier == null
         && pAlternativeRecord->nAuxiliaryDataReferenceCount == 0)
      {
         nAlternativeRecordFlags |= P_NDEF_FLAG_MESSAGE_END;
      }

      nOffset += static_PHandoverBuildConfigurationDataRecord(
                     &pAlternativeRecord->sCarrierType,
                     &pAlternativeRecord->sPayloadReference,
                     &pAlternativeRecord->sConfigurationData,
                     nAlternativeRecordFlags,
                     &pNDEFMessageData[nOffset]);

      nIndexAuxiliaryConfigurationData = 0;

      while(nIndexAuxiliaryConfigurationData < pAlternativeRecord->nAuxiliaryDataReferenceCount)
      {
         nAlternativeRecordFlags =  P_NDEF_FLAG_SHORT_RECORD;

         if(   nIndexAuxiliaryConfigurationData == (pAlternativeRecord->nAuxiliaryDataReferenceCount - 1)
            && pAlternativeRecord->pNextCarrier == null)
         {
            nAlternativeRecordFlags |= P_NDEF_FLAG_MESSAGE_END;
         }

         nOffset += static_PHandoverBuildConfigurationDataRecord(
                     &pAlternativeRecord->sCarrierType,
                     &pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryConfigurationData],
                     &pAlternativeRecord->pAuxiliaryConfigurationData[nIndexAuxiliaryConfigurationData],
                     nAlternativeRecordFlags,
                     &pNDEFMessageData[nOffset]);

         nIndexAuxiliaryConfigurationData++;
      }

      pAlternativeRecord = pAlternativeRecord->pNextCarrier;
   }

   *nLength = nOffset;

   return pNDEFMessageData;
}

/**
 * @brief Calculate the mandatory length to keep pairing data
 **/
W_ERROR PHandoverGetPairingInfoLength(
      tContext *  pContext,
      W_HANDLE hConnection,
      uint32_t * pnLength)
{
   tPHandoverUserConnection * pHandoverUserConnection;
   tPAlternativeCarrierRecord * pAlternativeCarrierRecord;

   /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);
   uint32_t nNameLength = 0;

   *pnLength = 0;
   PDebugTrace("PHandoverGetPairingInfoLength");

   if(nError != W_SUCCESS || pHandoverUserConnection == null)
   {
      PDebugError("PHandoverGetPairingInfoLength BAD Parameter");
      return nError;
   }

   /* First initialization */
   *pnLength = sizeof(tWHandoverPairingInfo);

   /* Add required slot for carrier */
   pAlternativeCarrierRecord = pHandoverUserConnection->sHandoverMessageReceived.pAlternativeCarrierRecord;
   while(pAlternativeCarrierRecord != null)
   {

      nNameLength = (PUtilConvertUTF8ToUTF16(null,
                                             pAlternativeCarrierRecord->sCarrierType.pName,
                                             pAlternativeCarrierRecord->sCarrierType.nLength) + 1);
      if(nNameLength == 0)
      {
         PDebugError("PHandoverGetPairingInfoLength Wrong RTD");
         return W_ERROR_WRONG_RTD;
      }

      *pnLength += sizeof(char16_t) * nNameLength;
      *pnLength += 1 ; /*one for each carrier power state */
      *pnLength += sizeof(char16_t *);
      pAlternativeCarrierRecord = pAlternativeCarrierRecord->pNextCarrier;
   }

   return W_SUCCESS;
}

/**
 * Returns pairing information
 **/
W_ERROR PHandoverGetPairingInfo(
      tContext *  pContext,
      W_HANDLE hConnection,
      tWHandoverPairingInfo* pPairingInfo)
{
   /* Init part */
   tPHandoverUserConnection * pHandoverUserConnection;
   tPAlternativeCarrierRecord * pAlternativeCarrierRecord;
   uint8_t * pOffset = 0;
   uint8_t nIndex = 0;
   uint32_t nNameLength;

   /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);

   PDebugTrace("PHandoverGetPairingInfo");

   if(nError != W_SUCCESS || pHandoverUserConnection == null)
   {
      PDebugError("PHandoverGetPairingInfo BAD Parameter");
      return nError;
   }

   pOffset = (uint8_t*) pPairingInfo + sizeof(tWHandoverPairingInfo);

   /* Requester ???? */
   pPairingInfo->bIsRequester = pHandoverUserConnection->bIsRequester;

   /* Carrier Getted*/

   /* First Loop for initializing each Carrier type and setting CarrierPowerStatesFlag and nCarrierFoundNumber*/
   pAlternativeCarrierRecord = pHandoverUserConnection->sHandoverMessageReceived.pAlternativeCarrierRecord;
   pPairingInfo->nCarrierFoundNumber = 0;
   while(pAlternativeCarrierRecord != null)
   {
      pPairingInfo->nCarrierFoundNumber ++;

      pAlternativeCarrierRecord = pAlternativeCarrierRecord->pNextCarrier;
   }

   /* Second loop for setting carrier power states */
   pAlternativeCarrierRecord = pHandoverUserConnection->sHandoverMessageReceived.pAlternativeCarrierRecord;
   if(pPairingInfo->nCarrierFoundNumber > 0)
   {
      pPairingInfo->pCarrierPowerState = pOffset;
      while(pAlternativeCarrierRecord != null)
      {
         *(pOffset++) = pAlternativeCarrierRecord->nCarrierPowerStateFlags;
         pAlternativeCarrierRecord = pAlternativeCarrierRecord->pNextCarrier;
      }

      /* third loop for setting data contained in each carrier type */
      pPairingInfo->pSupportedCarrier = (char16_t **) pOffset;
      pAlternativeCarrierRecord = pHandoverUserConnection->sHandoverMessageReceived.pAlternativeCarrierRecord;

      pOffset += (sizeof(char16_t ** ) * pPairingInfo->nCarrierFoundNumber);

      while(pAlternativeCarrierRecord != null)
      {
         nNameLength = PUtilConvertUTF8ToUTF16(
                           (char16_t *)pOffset,
                           pAlternativeCarrierRecord->sCarrierType.pName,
                           pAlternativeCarrierRecord->sCarrierType.nLength) + 1;

         if(nNameLength == 0)
         {
            PDebugError("PHandoverGetPairingInfo Wrong RTD");
            return W_ERROR_WRONG_RTD;
         }

         pPairingInfo->pSupportedCarrier[nIndex] = (char16_t *)pOffset;
         pOffset += sizeof(char16_t) * nNameLength;

         pPairingInfo->pSupportedCarrier[nIndex][pAlternativeCarrierRecord->sCarrierType.nLength] = '\0';

         nIndex += 1;

         pAlternativeCarrierRecord = pAlternativeCarrierRecord->pNextCarrier;
      }
   }
   else
   {
      pPairingInfo->pSupportedCarrier = null;
      pPairingInfo->pCarrierPowerState = null;
   }

   /* Error */
   pPairingInfo->bIsError = !(pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.pError == null);

   if(pPairingInfo->bIsError != W_FALSE)
   {
      pPairingInfo->sHandoverError.nErrorReason =
                           pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.pError->nErrorReason;
      pPairingInfo->sHandoverError.nErrorDataLength =
                           pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.pError->nErrorDataLength;

      if(pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.pError->nErrorDataLength > 0)
      {
         CMemoryCopy(pPairingInfo->sHandoverError.aErrorData,
                     pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.pError->aErrorData,
                     pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.pError->nErrorDataLength);
      }
   }
   else
   {
      CMemoryFill(pPairingInfo->sHandoverError.aErrorData,
                  0x00,
                  sizeof(pPairingInfo->sHandoverError.aErrorData));

      pPairingInfo->sHandoverError.nErrorDataLength = 0;
      pPairingInfo->sHandoverError.nErrorReason = 0;
   }

   return W_SUCCESS;
}

/**
 * @brief Search a configuration data contained in a list of alternative Carrier
 **/
static W_ERROR static_PHandoverGetConfigurationDataByPayloadID(
                  tPAlternativeCarrierRecord * pAlternativeCarrier,
                  tPayloadReference * pPayloadReference,
                  tPAlternativeCarrierRecord ** ppAlternativeCarrierReturned,
                  tPHandoverConfigurationData ** ppConfigurationDataReturned)
{
   uint8_t nIndex = 0;
   PDebugTrace("static_PHandoverGetConfigurationDataByPayloadID");

   while(pAlternativeCarrier != null)
   {
      /* if equals returns data found */
      if(   ( pPayloadReference->nLength == pAlternativeCarrier->sPayloadReference.nLength)
         && ( CMemoryCompare (pPayloadReference->pName,
                              pAlternativeCarrier->sPayloadReference.pName,
                              pPayloadReference->nLength) == 0x00))
      {
         *ppAlternativeCarrierReturned = pAlternativeCarrier;
         *ppConfigurationDataReturned  = &pAlternativeCarrier->sConfigurationData;
         return W_SUCCESS;
      }

      nIndex = 0;
      while( nIndex < pAlternativeCarrier->nAuxiliaryDataReferenceCount)
      {
         /* if equals returns data found */
         if(   ( pPayloadReference->nLength == pAlternativeCarrier->pAuxiliaryPayloadReference[nIndex].nLength)
            && ( CMemoryCompare (pPayloadReference->pName,
                                 pAlternativeCarrier->pAuxiliaryPayloadReference[nIndex].pName,
                                 pPayloadReference->nLength) == 0x00))
         {
            *ppAlternativeCarrierReturned = pAlternativeCarrier;
            *ppConfigurationDataReturned  = &pAlternativeCarrier->pAuxiliaryConfigurationData[nIndex];
            return W_SUCCESS;
         }

         nIndex ++;
      }

      pAlternativeCarrier = pAlternativeCarrier->pNextCarrier;
   }

   PDebugError("static_PHandoverGetConfigurationDataByPayloadID : Payload not found");
   return W_ERROR_ITEM_NOT_FOUND;
}

/**
 * @brief Parse Record message containing a Configuration data of an alternative Carrier
 **/
static W_ERROR static_PHandoverParseCarrierConfigurationData(
                  tPAlternativeCarrierRecord * pAlternativeCarrier,
                  uint8_t * pConfigurationRecord,
                  uint32_t nLength,
                  uint32_t * nEffectiveLength)
{
   uint32_t nOffset = 0;
   uint8_t  nRecordLength = 0;
   tPayloadReference sPayloadReference;
   tPHandoverCarrierType sCarrierType;
   bool_t bLastRecord = W_FALSE;
   tPHandoverConfigurationData * pConfigurationData;
   tPAlternativeCarrierRecord  * pCarrierFound;
   W_ERROR nError = 0;

   if(pAlternativeCarrier == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   while(nOffset < nLength
         && bLastRecord == W_FALSE)
   {
      if((pConfigurationRecord[nOffset] & P_NDEF_FLAG_MESSAGE_END) == P_NDEF_FLAG_MESSAGE_END)
      {
         bLastRecord = W_TRUE;
      }

      /* Read NDEF Header */
      if(((pConfigurationRecord[nOffset] & P_NDEF_FLAG_SHORT_RECORD) == 0)
         && ((pConfigurationRecord[nOffset] & P_NDEF_ID_LENGTH_PRESENT) == 0))
      {
         PDebugError("static_PHandoverParseCarrierConfigurationData : Connot execute the process on non short record NDEF Message");
         return W_ERROR_BAD_NDEF_FORMAT;
      }

      nOffset++;

      sCarrierType.nLength       = pConfigurationRecord[nOffset++];
      nRecordLength              = pConfigurationRecord[nOffset++];
      sPayloadReference.nLength  = pConfigurationRecord[nOffset++];

      sCarrierType.pName         = &pConfigurationRecord[nOffset];
      nOffset += sCarrierType.nLength;

      sPayloadReference.pName    = &pConfigurationRecord[nOffset];
      nOffset += sPayloadReference.nLength;

      nError = static_PHandoverGetConfigurationDataByPayloadID(
                     pAlternativeCarrier,
                     &sPayloadReference,
                     &pCarrierFound,
                     &pConfigurationData);

      if(nError != W_SUCCESS)
      {
         PDebugError("static_PHandoverParseCarrierConfigurationData : Connot execute the process on non short record NDEF Message");
         return nError;
      }

      if(pCarrierFound->sCarrierType.nLength == 0)
      {
         pCarrierFound->sCarrierType.pName = (uint8_t *)CMemoryAlloc(sCarrierType.nLength);

         if(pCarrierFound->sCarrierType.pName == null)
         {
            PDebugError("static_PHandoverParseCarrierConfigurationData : OUT OF RESOURCE");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         pCarrierFound->sCarrierType.nLength = sCarrierType.nLength;
         CMemoryCopy(pCarrierFound->sCarrierType.pName,
                     sCarrierType.pName,
                     sCarrierType.nLength);
      }
      else if(   (pCarrierFound->sCarrierType.nLength != sCarrierType.nLength)
         && (CMemoryCompare(sCarrierType.pName,
                           pCarrierFound->sCarrierType.pName,
                           sCarrierType.nLength) != 0))
      {
         PDebugError("static_PHandoverParseCarrierConfigurationData : MalFormated");
         return W_ERROR_BAD_NDEF_FORMAT;
      }

      pConfigurationData->nLength = nRecordLength;
      CMemoryCopy(pConfigurationData->pData,
                  &pConfigurationRecord[nOffset],
                  nRecordLength);

      nOffset += nRecordLength;
   }

   return W_SUCCESS;
}

/**
 * @brief Parse a Record NDEF containing an alternative carrier record
 **/
static W_ERROR static_PHandoverParseAlternativeCarrierRecord(
                  tPAlternativeCarrierRecord * *ppAlternativeRecord,
                  uint8_t * pAlternativeCarrierData,
                  uint32_t nLength,
                  uint32_t * nEffectiveLength)
{
   uint32_t nOffset = 0;
   uint8_t nIndex = 0;
   tPAlternativeCarrierRecord * pAlternativeCarrierRecord;
   tPayloadReference * pPayloadReference;

   while(nOffset < nLength)
   {
      /* Memory Allocation for the Alternative Carrier */
      pAlternativeCarrierRecord = (tPAlternativeCarrierRecord *) CMemoryAlloc(sizeof(tPAlternativeCarrierRecord));

      if(pAlternativeCarrierRecord == null)
      {
         PDebugError("static_PHandoverParserAlternativeCarrierRecord : out of resource");
         return W_ERROR_OUT_OF_RESOURCE;
      }

      *ppAlternativeRecord = pAlternativeCarrierRecord;

      CMemoryFill(pAlternativeCarrierRecord,
                  0x00,
                  sizeof(tPAlternativeCarrierRecord));

      /* Read NDEF Header */
      if((pAlternativeCarrierData[nOffset++] & P_NDEF_FLAG_SHORT_RECORD) == 0)
      {
         PDebugError("static_PHandoverParserAlternativeCarrierRecord : Connot execute the process on non short record NDEF Message");
         return W_ERROR_BAD_NDEF_FORMAT;
      }

      /* Check REcord Type */
      if(pAlternativeCarrierData[nOffset++] != sizeof(P_HANDOVER_ALTERNATIVE_CARRIER_TYPE))
      {
         PDebugError("static_PHandoverParserAlternativeCarrierRecord : item not found");
         return W_ERROR_ITEM_NOT_FOUND;
      }

      nOffset++; /* skip record length */

      if(CMemoryCompare(&pAlternativeCarrierData[nOffset],
                        P_HANDOVER_ALTERNATIVE_CARRIER_TYPE,
                        sizeof(P_HANDOVER_ALTERNATIVE_CARRIER_TYPE)) != 0x00)
      {
         PDebugError("static_PHandoverParserAlternativeCarrierRecord : item not found");
         return W_ERROR_ITEM_NOT_FOUND;
      }

      nOffset += sizeof(P_HANDOVER_ALTERNATIVE_CARRIER_TYPE);


      /* Get data of carrier */
      pAlternativeCarrierRecord->nCarrierPowerStateFlags = pAlternativeCarrierData[nOffset++];
      pAlternativeCarrierRecord->sPayloadReference.nLength = pAlternativeCarrierData[nOffset++];

      pAlternativeCarrierRecord->sPayloadReference.pName = (uint8_t *) CMemoryAlloc(pAlternativeCarrierRecord->sPayloadReference.nLength);
      if(pAlternativeCarrierRecord->sPayloadReference.pName == null)
      {
         PDebugError("static_PHandoverParserAlternativeCarrierRecord : out of resource");
         return W_ERROR_OUT_OF_RESOURCE;
      }

      CMemoryCopy(pAlternativeCarrierRecord->sPayloadReference.pName,
                  &pAlternativeCarrierData[nOffset],
                  pAlternativeCarrierRecord->sPayloadReference.nLength);

      nOffset += pAlternativeCarrierRecord->sPayloadReference.nLength;

      /* Auxiliary data */
      pAlternativeCarrierRecord->nAuxiliaryDataReferenceCount = pAlternativeCarrierData[nOffset++];

      if(pAlternativeCarrierRecord->nAuxiliaryDataReferenceCount > 0)
      {
         pAlternativeCarrierRecord->pAuxiliaryConfigurationData =
                  (tPHandoverConfigurationData *) CMemoryAlloc( pAlternativeCarrierRecord->nAuxiliaryDataReferenceCount * sizeof(tPHandoverConfigurationData));

         if(pAlternativeCarrierRecord->pAuxiliaryConfigurationData == null)
         {
            PDebugError("static_PHandoverParserAlternativeCarrierRecord : out of resource");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         pAlternativeCarrierRecord->pAuxiliaryPayloadReference =
               (tPayloadReference *) CMemoryAlloc( pAlternativeCarrierRecord->nAuxiliaryDataReferenceCount * sizeof(tPayloadReference));

         if(pAlternativeCarrierRecord->pAuxiliaryPayloadReference == null)
         {
            PDebugError("static_PHandoverParserAlternativeCarrierRecord : out of resource");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         nIndex = 0;
         while((nOffset < nLength)
            && (nIndex < pAlternativeCarrierRecord->nAuxiliaryDataReferenceCount))
         {
            pPayloadReference = &pAlternativeCarrierRecord->pAuxiliaryPayloadReference[nIndex ++];
            pPayloadReference->nLength = pAlternativeCarrierData[nOffset++];

            pPayloadReference->pName = (uint8_t *) CMemoryAlloc(pPayloadReference->nLength);

            if( pPayloadReference->pName == null)
            {
               PDebugError("static_PHandoverParserAlternativeCarrierRecord : out of resource");
               return W_ERROR_OUT_OF_RESOURCE;
            }

            CMemoryCopy(pPayloadReference->pName,
                        &pAlternativeCarrierData[nOffset],
                        pPayloadReference->nLength);

            nOffset += pPayloadReference->nLength;
         }
      }

      if(nOffset > nLength)
      {
         return W_ERROR_BAD_NDEF_FORMAT;
      }

      ppAlternativeRecord = &pAlternativeCarrierRecord->pNextCarrier;
   }

   if(nOffset != nLength)
   {
      PDebugError("static_PHandoverParserAlternativeCarrierRecord : length not OK");
      return W_ERROR_BAD_NDEF_FORMAT;
   }

   *nEffectiveLength = nOffset;

   return W_SUCCESS;
}

/**
 * @brief Parse an entire Select Message
 **/
static W_ERROR static_PHandoverParseSelectMessage(
                  tPHandoverMessage * pSelectMessage,
                  uint8_t * pBuffer,
                  uint32_t nLength)
{
   uint32_t  nRecordLength = 0;
   uint32_t  nIdPayloadLength = 0;
   uint32_t nOffset = 0;
   W_ERROR nError;

   bool_t    bNoCarrierConfiguration  = pBuffer[nOffset] & P_NDEF_FLAG_MESSAGE_END;
   bool_t    bHasPayloadId            = pBuffer[nOffset] & P_NDEF_ID_LENGTH_PRESENT;

   /* Read NDEF Header */
   if((pBuffer[nOffset++] & P_NDEF_FLAG_SHORT_RECORD) == 0)
   {
      PDebugError("static_PHandoverParseSelectMessage : Connot execute the process on non short record NDEF Message");
      return W_ERROR_BAD_NDEF_FORMAT;
   }

   /* Record type length */
   if( pBuffer[nOffset ++] != sizeof(P_HANDOVER_SELECT_TYPE))
   {
      PDebugError("static_PHandoverParseSelectMessage : it is not a Select message");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   nRecordLength = pBuffer[nOffset++];

   if(bHasPayloadId != W_FALSE)
   {
      nIdPayloadLength = pBuffer[nOffset++];
   }

   if(CMemoryCompare(&pBuffer[nOffset],
                     P_HANDOVER_SELECT_TYPE,
                     sizeof(P_HANDOVER_SELECT_TYPE)) != 0x00)
   {
      PDebugError("static_PHandoverParseSelectMessage : it is not a Select message");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   nOffset += sizeof(P_HANDOVER_SELECT_TYPE);

   nOffset += nIdPayloadLength;

   /* version */
   pSelectMessage->nVersion = pBuffer[nOffset++];

   if(bNoCarrierConfiguration)
   {
      return W_SUCCESS;
   }


   /* Alternative carrier Record */
   nError = static_PHandoverParseAlternativeCarrierRecord(
               &pSelectMessage->pAlternativeCarrierRecord,
               &pBuffer[nOffset],
               (nRecordLength - 1),
               &nRecordLength);

   nOffset += nRecordLength;

   /* if error occured */
   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverParseSelectMessage : error Occured");
      static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
      return nError;
   }

   while(nOffset < nLength)
   {
      /* If (this record is the last and if it has a Well KNOWN LOCAL Type )
            it shall be an
         Error Record*/
      if( ((pBuffer[nOffset] & P_NDEF_FLAG_MESSAGE_END) == P_NDEF_FLAG_MESSAGE_END))
      {
         if((pBuffer[nOffset] & W_NDEF_TNF_WELL_KNOWN) == W_NDEF_TNF_WELL_KNOWN)
         {

             /* Read NDEF Header */
            if((pBuffer[nOffset++] & P_NDEF_FLAG_SHORT_RECORD) == 0)
            {
               PDebugError("static_PHandoverParseSelectMessage : Connot execute the process on non short record NDEF Message");
               static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
               return W_ERROR_BAD_NDEF_FORMAT;
            }

            /* Check REcord Type */
            if(pBuffer[nOffset++] != sizeof(P_HANDOVER_ERROR_RECORD_TYPE))
            {
               PDebugError("static_PHandoverParseSelectMessage : item not found");
               static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
               return W_ERROR_ITEM_NOT_FOUND;
            }

            nRecordLength = pBuffer[nOffset++];

            if(CMemoryCompare(&pBuffer[nOffset],
                              P_HANDOVER_ERROR_RECORD_TYPE,
                              sizeof(P_HANDOVER_ERROR_RECORD_TYPE)) != 0x00)
            {
               PDebugError("static_PHandoverParseSelectMessage : item not found");
               static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
               return W_ERROR_ITEM_NOT_FOUND;
            }

            nOffset += sizeof(P_HANDOVER_ERROR_RECORD_TYPE);

            pSelectMessage->uSpecificData.pError = ( tPHandoverError *) CMemoryAlloc(sizeof(tPHandoverError));
            if(pSelectMessage->uSpecificData.pError == null)
            {
               PDebugError("static_PHandoverParseSelectMessage : out of ressource");
               static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
               return W_ERROR_OUT_OF_RESOURCE;
            }

            pSelectMessage->uSpecificData.pError->nErrorReason = pBuffer[nOffset++];
            pSelectMessage->uSpecificData.pError->nErrorDataLength = (uint8_t) nRecordLength - 1;

            if(nRecordLength > 1)
            {
               CMemoryCopy(pSelectMessage->uSpecificData.pError->aErrorData,
                           &pBuffer[nOffset],
                           pSelectMessage->uSpecificData.pError->nErrorDataLength);

               nOffset += pSelectMessage->uSpecificData.pError->nErrorDataLength;
            }
         }
         else
         {
            nRecordLength = nLength - nOffset;
            nError = static_PHandoverParseCarrierConfigurationData(
                           pSelectMessage->pAlternativeCarrierRecord,
                           &pBuffer[nOffset],
                           nRecordLength,
                           &nRecordLength);

            if(nError != W_SUCCESS)
            {
               PDebugError("static_PHandoverParseSelectMessage : error detected");
               static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
               return nError;
            }

            nOffset += nRecordLength;
         }

         break;
      }

      nRecordLength = nLength - nOffset;
      nError = static_PHandoverParseCarrierConfigurationData(
                     pSelectMessage->pAlternativeCarrierRecord,
                     &pBuffer[nOffset],
                     nRecordLength,
                     &nRecordLength);

      if(nError != W_SUCCESS)
      {
         PDebugError("static_PHandoverParseSelectMessage : error detected");
         static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
         return nError;
      }

      nOffset += nRecordLength;
   }

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverParseSelectMessage : error Occured");
      static_PDestroyAlternativeCarrierRecord(pSelectMessage->pAlternativeCarrierRecord);
      return nError;
   }

   return W_SUCCESS;
}

/**
 * @brief Parse an entire Select Message
 **/
static W_ERROR static_PHandoverParseRequestMessage(
                  tPHandoverMessage * pRequestMessage,
                  uint8_t * pBuffer,
                  uint32_t nLength)
{
   uint32_t nOffsetCollisionRecordStart;
   uint32_t  nRecordLength = 0;
   uint32_t  nIdPayloadLength = 0;
   uint32_t nOffset = 0;
   W_ERROR nError;

   bool_t    bNoMoreCarrierConfiguration = pBuffer[nOffset] & P_NDEF_FLAG_MESSAGE_END;
   bool_t    bHasPayloadId               = pBuffer[nOffset] & P_NDEF_ID_LENGTH_PRESENT;

   if(bNoMoreCarrierConfiguration != W_FALSE)
   {
      PDebugError("static_PHandoverParseRequestMessage : Connot execute the process on non short record NDEF Message");
      return W_ERROR_BAD_NDEF_FORMAT;
   }

   /* Read NDEF Header */
   if((pBuffer[nOffset++] & P_NDEF_FLAG_SHORT_RECORD) == 0)
   {
      PDebugError("static_PHandoverParseRequestMessage : Connot execute the process on non short record NDEF Message");
      return W_ERROR_BAD_NDEF_FORMAT;
   }

   /* Record type length */
   if( pBuffer[nOffset ++] != sizeof(P_HANDOVER_REQUEST_TYPE))
   {
      PDebugError("static_PHandoverParseRequestMessage : it is not a Request message");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   nRecordLength = pBuffer[nOffset++];

   if(bHasPayloadId != W_FALSE)
   {
      nIdPayloadLength = pBuffer[nOffset++];
   }

   if(CMemoryCompare(&pBuffer[nOffset],
                     P_HANDOVER_REQUEST_TYPE,
                     sizeof(P_HANDOVER_REQUEST_TYPE)) != 0x00)
   {
      PDebugError("static_PHandoverParseRequestMessage : it is not a Request message");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   nOffset += sizeof(P_HANDOVER_REQUEST_TYPE);

   nOffset += nIdPayloadLength;

   /* version */
   pRequestMessage->nVersion = pBuffer[nOffset++];

   nOffsetCollisionRecordStart = nOffset;

   /******* Collision Record  *******/
   /* Skip Collision Record Header */
   bHasPayloadId = pBuffer[nOffset++] & P_NDEF_ID_LENGTH_PRESENT;
   if(pBuffer[nOffset++] != sizeof(P_HANDOVER_COLLISON_RECORD_TYPE))
   {
      PDebugError("static_PHandoverParseRequestMessage : collision Record is not present");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   if(pBuffer[nOffset++] != sizeof(pRequestMessage->uSpecificData.nCollisionResolutionRecord))
   {
      PDebugError("static_PHandoverParseRequestMessage : Error on collision Record size");
      return W_ERROR_BAD_NDEF_FORMAT;
   }

   if(bHasPayloadId != W_FALSE)
   {
      nIdPayloadLength = pBuffer[nOffset++];
   }
   else
   {
      nIdPayloadLength = 0;
   }

   if(CMemoryCompare(&pBuffer[nOffset],
                     P_HANDOVER_COLLISON_RECORD_TYPE,
                     sizeof(P_HANDOVER_COLLISON_RECORD_TYPE)) != 0x00)
   {
      PDebugError("static_PHandoverParseRequestMessage : it is not a Request message");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   nOffset += sizeof(P_HANDOVER_COLLISON_RECORD_TYPE);

   /* skip payload id */
   nOffset += nIdPayloadLength;

   /* Get Collision Record */
   pRequestMessage->uSpecificData.nCollisionResolutionRecord = (pBuffer[nOffset] << 8) | pBuffer[nOffset + 1];
   nOffset += 2;

   /******** Alternative carrier Record **********/
   /* Alternative carrier Record */
   nError = static_PHandoverParseAlternativeCarrierRecord(
               &pRequestMessage->pAlternativeCarrierRecord,
               &pBuffer[nOffset],
               (nRecordLength - (nOffset - nOffsetCollisionRecordStart) -1),/* nREcordLength - Size of collision record - version*/
               &nRecordLength);

   nOffset += nRecordLength;

   /* if error occured */
   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverParseRequestMessage : error Occured");
      static_PDestroyAlternativeCarrierRecord(pRequestMessage->pAlternativeCarrierRecord);
      return nError;
   }

   /************* Configuration data Record *****************/
   bNoMoreCarrierConfiguration = W_FALSE;
   while(nOffset < nLength && bNoMoreCarrierConfiguration == W_FALSE)
   {
      bNoMoreCarrierConfiguration = pBuffer[nOffset] & P_NDEF_FLAG_MESSAGE_END;

      nRecordLength = nLength - nOffset;
      nError = static_PHandoverParseCarrierConfigurationData(
                     pRequestMessage->pAlternativeCarrierRecord,
                     &pBuffer[nOffset],
                     nRecordLength,
                     &nRecordLength);

      if(nError != W_SUCCESS)
      {
         PDebugError("static_PHandoverParseSelectMessage : error detected");
         static_PDestroyAlternativeCarrierRecord(pRequestMessage->pAlternativeCarrierRecord);
         return nError;
      }

      nOffset += nRecordLength;
   }

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverParseSelectMessage : error Occured");
      static_PDestroyAlternativeCarrierRecord(pRequestMessage->pAlternativeCarrierRecord);
      return nError;
   }

   return W_SUCCESS;
}

/**
 * @brief Parse an entire Handover Message
 **/
static W_ERROR static_PHandoverParseMessage(
      tPHandoverMessage * pHandoverMessage,
      uint8_t* pNDEFMessageRead,
      uint32_t pNDEFMessageReadLength)
{
   /* First verify if the message is an Handover Select or an Handover Request */
   uint32_t nOffset           = 0;
   uint8_t nRecordTypeLength  = 0;

   bool_t bHasPayloadID = pNDEFMessageRead[nOffset] & P_NDEF_ID_LENGTH_PRESENT;

   /* Read NDEF Header */
   if((pNDEFMessageRead[nOffset++] & P_NDEF_FLAG_SHORT_RECORD) == 0)
   {
      PDebugError("static_PHandoverParseSelectMessage : Connot execute the process on non short record NDEF Message");
      return W_ERROR_BAD_NDEF_FORMAT;
   }

   nRecordTypeLength = pNDEFMessageRead[nOffset++];

   /* skip Payload ID Length */
   if(bHasPayloadID != W_FALSE)
   {
      nOffset += 1;
   }

   /* skip Payload Length */
   nOffset += 1;

   /* Handover Message Type ? */
   /* Request ? */
   if(nRecordTypeLength == sizeof(P_HANDOVER_REQUEST_TYPE)
   && (CMemoryCompare(&pNDEFMessageRead[nOffset],
                      P_HANDOVER_REQUEST_TYPE,
                      nRecordTypeLength) == 0x00))
   {
      pHandoverMessage->bIsRequestMessage = W_TRUE;
      return static_PHandoverParseRequestMessage(pHandoverMessage,
                                                pNDEFMessageRead,
                                                pNDEFMessageReadLength);
   }
   /* Select */
   else if(nRecordTypeLength == sizeof(P_HANDOVER_SELECT_TYPE)
       && (CMemoryCompare(&pNDEFMessageRead[nOffset],
                           P_HANDOVER_SELECT_TYPE,
                           nRecordTypeLength) == 0x00))
   {
      pHandoverMessage->bIsRequestMessage = W_FALSE;
      return static_PHandoverParseSelectMessage(pHandoverMessage,
                                                pNDEFMessageRead,
                                                pNDEFMessageReadLength);
   }

   /* Error */
   return W_ERROR_FEATURE_NOT_SUPPORTED;
}

/**
 *See header
 */
W_ERROR PHandoverCreate(
      tContext * pContext,
      W_HANDLE* phMessage)
{
   tPHandoverUserConnection * pHandoverUserConnection;
   W_ERROR nError;

   /* Check the parameters */
   if ( phMessage == null )
   {
      PDebugError("PHandoverCreate: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the Handover structure */
   pHandoverUserConnection = (tPHandoverUserConnection*)CMemoryAlloc( sizeof(tPHandoverUserConnection) );
   if ( pHandoverUserConnection == null )
   {
      PDebugError("PHandoverCreate: pHandoverUserConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }

   CMemoryFill(pHandoverUserConnection, 0, sizeof(tPHandoverUserConnection));

   /* Register the Handover connection structure */
   if ( ( nError = PHandleRegister(
                        pContext,
                        pHandoverUserConnection,
                        &g_sHandoverUserConnection,
                        phMessage) ) != W_SUCCESS )
   {
      PDebugError("PHandoverCreate: error on PHandleRegister");
      CMemoryFree(pHandoverUserConnection);
      return nError;
   }

   pHandoverUserConnection->sHandoverMessageSent.nVersion      = P_HANDOVER_CURRENT_VERSION;
   pHandoverUserConnection->sHandoverMessageReceived.nVersion  = P_HANDOVER_CURRENT_VERSION;

   return W_SUCCESS;
}

/**
 * @brief Returns the configuration's data length of an tPAlternativeCarrierRecord
 **/
static uint32_t static_PHandoverGetDataLengthOfAlternativeCarrier(
   tPAlternativeCarrierRecord * pAlternativeRecord)
{

   uint8_t nIndex = 0;
   uint32_t nLength = pAlternativeRecord->sConfigurationData.nLength;

   while(nIndex < pAlternativeRecord->nAuxiliaryDataReferenceCount)
   {
      nLength += pAlternativeRecord->pAuxiliaryConfigurationData[nIndex].nLength;
      nIndex++;
   }
   return nLength;
}

/**
 *@brief Callback called when the VirtualTag is stopped
 **/
static void static_PHandoverVirtualTagStopAndCloseCompleted(
            tContext* pContext,
            void* pObject,
            W_ERROR nError)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pObject;
   PDebugTrace ("static_PHandoverVirtualTagStopPairing");

   PHandleClose(pContext,
                pHandoverUserConnection->hVirtualTag);

   pHandoverUserConnection->hVirtualTag = W_NULL_HANDLE;
}

/* Destroy connection callback */
static void static_PHandoverStopPairingStart(
            tContext* pContext,
            void* pCallbackParameter,
            bool_t bIsClosing)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;
   PDebugTrace ("static_PHandoverStopPairingStart");

   if(pHandoverUserConnection->nState == P_HANDOVER_STATE_PAIRING_STARTED)
   {
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       W_ERROR_CANCEL);
   }
}

/* Destroy connection callback */
static void static_PHandoverStopPairingCompletion(
            tContext* pContext,
            void* pCallbackParameter,
            bool_t bIsClosing)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;

   if(pHandoverUserConnection->nState == P_HANDOVER_STATE_COMPLETION_STARTED)
   {
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       W_ERROR_CANCEL);
   }
}

/**
 *@brief destructor function of the PHandoverUser structure
**/
static uint32_t static_PHandoverUserDestroyConnection(
            tContext* pContext,
            void* pObject )
{


   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection * ) pObject;

   PDebugTrace("static_PHandoverDestroyConnection");

   static_PHandoverRemoveMessage(&pHandoverUserConnection->sHandoverMessageSent);
   static_PHandoverRemoveMessage(&pHandoverUserConnection->sHandoverMessageReceived);
   PDFCFlushCall(&pHandoverUserConnection->sCallbackContext);

   /**
    * special case for P_HANDOVER_WAIT_COMPLETION
    * in this case sockets P2P are never released because the handover connection is never completed
    **/

   if(pHandoverUserConnection->nState == P_HANDOVER_STATE_WAIT_COMPLETION)
   {
      static_PHandoverFlushHandle(pContext,
                                  pHandoverUserConnection);
   }

   /* Free the Handover connection structure */
   CMemoryFree( pHandoverUserConnection );

   return W_SUCCESS;
}

/**
 *See Header
 **/
W_ERROR PHandoverAddCarrier(
   tContext * pContext,
   W_HANDLE hConnection,
   const tPHandoverCarrierType * pCarrierType,
   const uint8_t * pConfigurationData,
   uint32_t nConfigurationDataLength,
   uint8_t  nCarrierPowerState)
{
   /* Init part */
   uint32_t nOffset = 0;


   tPHandoverUserConnection * pHandoverUserConnection;
   tPAlternativeCarrierRecord * pAlternativeRecord;
   tPAlternativeCarrierRecord ** ppAlternativeRecordLast;
   tPHandoverConfigurationData * pConfigurationDataTmp;

   uint8_t nIndexAuxiliaryCarrier = 0;
   uint8_t nAuxiliryCarrierNumber = (uint8_t)((nConfigurationDataLength - 1)/ P_HANDOVER_MAX_PAYLOAD_LENGTH);

   /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);

   PDebugTrace("PHandoverAddCarrier nLength => %d", nConfigurationDataLength);

   if(nError != W_SUCCESS || pHandoverUserConnection == null)
   {
      PDebugError("PHandoverAddCarrier BAD Parameter");
      return nError;
   }



   /************************************************************************************/
   /**                     Memory Alloc of the pAlternativeRecord                     **/
   /************************************************************************************/
   pAlternativeRecord = (tPAlternativeCarrierRecord *) CMemoryAlloc(sizeof(tPAlternativeCarrierRecord));
   if(pAlternativeRecord == null)
   {
         PDebugError("PHandoverAddCarrier NOT Enough ressource");
         return W_ERROR_OUT_OF_RESOURCE;
   }

   CMemoryFill(pAlternativeRecord,
               0x00,
               sizeof(tPAlternativeCarrierRecord));


   /************************************************************************************/
   /**                     Memory Alloc of the Carrier type                           **/
   /************************************************************************************/
   pAlternativeRecord->sCarrierType.pName = (uint8_t*) CMemoryAlloc(pCarrierType->nLength);
   if(pAlternativeRecord->sCarrierType.pName == null)
   {
         PDebugError("PHandoverAddCarrier NOT Enough ressource");
         CMemoryFree(pAlternativeRecord);
         return W_ERROR_OUT_OF_RESOURCE;
   }

   /* Copy the record type Name */
   pAlternativeRecord->sCarrierType.nLength = pCarrierType->nLength;
   CMemoryCopy(pAlternativeRecord->sCarrierType.pName,
               pCarrierType->pName,
               pCarrierType->nLength);



   /* Copy the Record power state */
   pAlternativeRecord->nCarrierPowerStateFlags = nCarrierPowerState;

   /************************************************************************************/
   /**                     Memory Alloc of the Carrier Data Reference                 **/
   /************************************************************************************/

   pAlternativeRecord->sPayloadReference.pName = (uint8_t*) CMemoryAlloc(1);
   if(pAlternativeRecord->sPayloadReference.pName == null)
   {
         PDebugError("PHandoverAddCarrier NOT Enough ressource");
         CMemoryFree(pAlternativeRecord->sCarrierType.pName);
         CMemoryFree(pAlternativeRecord);
         return W_ERROR_OUT_OF_RESOURCE;
   }

   /* Set the Carrier Record */
   pAlternativeRecord->sPayloadReference.nLength = 1;
   pAlternativeRecord->sPayloadReference.pName[0] = pHandoverUserConnection->nCarrierRecordAdded++ + '0';

   /************************************************************************************/
   /**                     sConfigurationData data copy                               **/
   /************************************************************************************/
   if(nConfigurationDataLength > P_HANDOVER_MAX_PAYLOAD_LENGTH)
   {
      pAlternativeRecord->sConfigurationData.nLength = P_HANDOVER_MAX_PAYLOAD_LENGTH;
   }
   else
   {
      pAlternativeRecord->sConfigurationData.nLength = (uint8_t)nConfigurationDataLength;
   }


   CMemoryCopy(pAlternativeRecord->sConfigurationData.pData,
               &pConfigurationData[nOffset],
               pAlternativeRecord->sConfigurationData.nLength);

   nOffset += pAlternativeRecord->sConfigurationData.nLength;

   /************************************************************************************/
   /**                    Auxiliary Data                                              **/
   /************************************************************************************/
   /* Now do the auxiliary Carrier */
   if(nAuxiliryCarrierNumber > 0)
   {
      nIndexAuxiliaryCarrier = 0;
      pAlternativeRecord->nAuxiliaryDataReferenceCount = nAuxiliryCarrierNumber;

      /* Set the Auxiliaries Carriers Records */
      pAlternativeRecord->pAuxiliaryPayloadReference = (tPayloadReference*) CMemoryAlloc(sizeof(tPayloadReference) * nAuxiliryCarrierNumber);

      if(pAlternativeRecord->pAuxiliaryPayloadReference == null)
      {
         PDebugError("PHandoverAddCarrier NOT Enough ressource");
         static_PDestroyAlternativeCarrierRecord(pAlternativeRecord);
         return W_ERROR_OUT_OF_RESOURCE;
      }
      CMemoryFill(pAlternativeRecord->pAuxiliaryPayloadReference,
                  0,
                  sizeof(tPayloadReference) * nAuxiliryCarrierNumber);


      pAlternativeRecord->pAuxiliaryConfigurationData = (tPHandoverConfigurationData *) CMemoryAlloc(sizeof(tPHandoverConfigurationData) * nAuxiliryCarrierNumber);

      if(pAlternativeRecord->pAuxiliaryConfigurationData == null)
      {
         PDebugError("PHandoverAddCarrier NOT Enough ressource");
         static_PDestroyAlternativeCarrierRecord(pAlternativeRecord);

         return W_ERROR_OUT_OF_RESOURCE;
      }
      CMemoryFill(pAlternativeRecord->pAuxiliaryConfigurationData,
                  0,
                  sizeof(tPHandoverConfigurationData) * nAuxiliryCarrierNumber);


      /* For each auxiliary Carrier configuration, we allocate memory and set it.*/
      while(nIndexAuxiliaryCarrier < nAuxiliryCarrierNumber)
      {

         pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryCarrier].pName = (uint8_t *) CMemoryAlloc(1);

         if(pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryCarrier].pName == null)
         {
            PDebugError("PHandoverAddCarrier NOT Enough ressource");
            static_PDestroyAlternativeCarrierRecord(pAlternativeRecord);

            return W_ERROR_OUT_OF_RESOURCE;
         }

         pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryCarrier].nLength = 1;
         pAlternativeRecord->pAuxiliaryPayloadReference[nIndexAuxiliaryCarrier].pName[0] = '0' + pHandoverUserConnection->nCarrierRecordAdded++;

         pConfigurationDataTmp = &pAlternativeRecord->pAuxiliaryConfigurationData[nIndexAuxiliaryCarrier];

         /* if this auxiliary carrier is the last */
         if(nIndexAuxiliaryCarrier == (nAuxiliryCarrierNumber -1))
         {
            pConfigurationDataTmp->nLength = (uint8_t)(nConfigurationDataLength - nOffset);
         }
         else
         {
            pConfigurationDataTmp->nLength = P_HANDOVER_MAX_PAYLOAD_LENGTH;
         }


         CMemoryCopy(pConfigurationDataTmp->pData,
                     &pConfigurationData[nOffset],
                     pConfigurationDataTmp->nLength);

         nOffset += pConfigurationDataTmp->nLength;
         nIndexAuxiliaryCarrier += 1;
      }
   }

   ppAlternativeRecordLast = &pHandoverUserConnection->sHandoverMessageSent.pAlternativeCarrierRecord;
   while(*ppAlternativeRecordLast != null)
   {
      /* address = address of following current item */
      ppAlternativeRecordLast = &((*ppAlternativeRecordLast)->pNextCarrier);
   }

   *ppAlternativeRecordLast = pAlternativeRecord;
   return W_SUCCESS;
}

/**
 * See Header
 **/
W_ERROR PHandoverRemoveAllCarrier(
      tContext * pContext,
      W_HANDLE hConnection)
{
   /* Init part */
   tPHandoverUserConnection * pHandoverUserConnection;
   /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);

   PDebugTrace("PHandoverRemoveAllCarrier");

   if(nError != W_SUCCESS || pHandoverUserConnection == null)
   {
      PDebugError("PHandoverRemoveAllCarrier BAD Parameter");
      return nError;
   }

   static_PHandoverRemoveMessage(&pHandoverUserConnection->sHandoverMessageSent);
   /* automatic remove when the pairing or the completion start */
   /* static_PHandoverRemoveMessage(&pHandoverUserConnection->sHandoverMessageReceived);*/

   return W_SUCCESS;
}

static void static_PHandoverVirtualTagCompleted(
            tContext* pContext,
            void* pObject,
            W_ERROR nError)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pObject;
   PDebugTrace ("static_PHandoverVirtualTagCompleted");

   static_PHandoverVirtualTagStopAndCloseCompleted(pContext, pObject, nError);

   static_PHandoverPairingCompleted(pContext,
                                    pHandoverUserConnection,
                                    pHandoverUserConnection->nError);
}

/**
 * @brief Event received when the virtual card is read by an external reader
 **/
static void static_PHandoverVirtualTagEventReceived(
            tContext* pContext,
            void* pObject,
            W_ERROR nEventCode)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection * )pObject;
   PDebugTrace ("static_PHandoverVirtualTagEventReceived Event code => %d", nEventCode);

   if(nEventCode == W_VIRTUAL_TAG_EVENT_READER_READ_ONLY)
   {
      pHandoverUserConnection->nError = W_SUCCESS;
      PVirtualTagStop(pContext,
                      pHandoverUserConnection->hVirtualTag,
                      static_PHandoverVirtualTagCompleted,
                      pHandoverUserConnection);
   }
}

/**
 * @brief Virtual Tag started
 **/
static void static_PHandoverVirtualTagStartCompleted(
            tContext* pContext,
            void* pObject,
            W_ERROR nError)
{
   if(nError != W_SUCCESS)
   {
      tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection * )pObject;

      PDebugTrace ("static_PHandoverVirtualTagStartCompleted Error during writing message");

      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
   }
}

/**
 * @brief Function called when the NDEF message is writen in the virtual TAG
 **/
static void static_PHandoverWriteMessageCompleted(
            tContext* pContext,
            void* pObject,
            W_ERROR nError)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pObject;

   PDebugTrace ("static_PHandoverWriteMessageCompleted");
   if(pHandoverUserConnection->nState != P_HANDOVER_STATE_FREE)
   {
      if(nError != W_SUCCESS)
      {
         PDebugError ("static_PHandoverWriteMessageCompleted Error during writing message");
         static_PHandoverPairingCompleted(pContext, pHandoverUserConnection, nError);
      }
      else
      {
         PVirtualTagStart( pContext,
                           pHandoverUserConnection->hVirtualTag,
                           static_PHandoverVirtualTagStartCompleted,
                           pHandoverUserConnection,
                           static_PHandoverVirtualTagEventReceived,
                           pHandoverUserConnection,
                           W_TRUE);

      }
   }
}

/**
 *@brief Function called when "this" read a Message NDEF
 **/
static void  static_PHandoverReadSelectMessageCompleted(
         tContext* pContext,
         void * pCallbackParameter,
         W_HANDLE hMessage,
         W_ERROR nError )
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;
   uint32_t nLength = 0;
   uint8_t * pBuffer = null;
   PDebugTrace("static_ReadMessageCompleted");

   /* if error or cancel */
   if(nError != W_SUCCESS)
   {
      PDebugError("static_ReadMessageCompleted : Error");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   nLength = PNDEFGetMessageLength(pContext, hMessage);

   pBuffer = (uint8_t* ) CMemoryAlloc(nLength);
   nError = PNDEFGetMessageContent(
                  pContext,
                  hMessage,
                  pBuffer,
                  nLength,
                  &nLength);

   PHandleClose(  pContext,
                  hMessage);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_ReadMessageCompleted : Connot retrieve the NDEF Message content");
      CMemoryFree(pBuffer);
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   nError = static_PHandoverParseSelectMessage(
               &pHandoverUserConnection->sHandoverMessageReceived,
               pBuffer,
               nLength);

   CMemoryFree(pBuffer);

   static_PHandoverPairingCompleted(pContext,
                                    pHandoverUserConnection,
                                    nError);

}

static void static_PHandoverProcessSelectReceived(
            tContext * pContext,
            tPHandoverUserConnection * pHandoverUserConnection)
{
   pHandoverUserConnection->bIsRequester = W_TRUE;

   PDebugTrace("static_PHandoverProcessSelectReceived");

   PP2PShutdown(  pContext,
                  pHandoverUserConnection->hSocket,
                  static_PHandoverP2PShutdownCompleted,
                  pHandoverUserConnection);
}

static void static_PHandoverP2PReadSelectMessage(
            tContext *  pContext,
            void *      pCallbackParameter,
            uint32_t    nLength,
            W_ERROR     nError)
{
   bool_t bPayloadPresence   = W_FALSE;
   bool_t bLastRecord        = W_FALSE;
   uint32_t  nDataInRecord  = 0;
   uint32_t  nOffset        = 0;

   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;
   uint32_t nLengthDataNotVirified = 0;

   PHandleClose(pContext, pHandoverUserConnection->hReadOperation);
   pHandoverUserConnection->hReadOperation = W_NULL_HANDLE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverP2PReadSelectMessage Error occured");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   if(nLength > 0)
   {
      if(pHandoverUserConnection->pP2PMessageRead == null)
      {
         pHandoverUserConnection->pP2PMessageRead = (uint8_t *) CMemoryAlloc(nLength);

         if(pHandoverUserConnection->pP2PMessageRead == null)
         {
            PDebugError("static_PHandoverP2PReadSelectMessage out of resource");
            static_PHandoverPairingCompleted(pContext,
                                             pHandoverUserConnection,
                                             W_ERROR_OUT_OF_RESOURCE);
            return;
         }

         pHandoverUserConnection->nP2PMessageReadLength  = 0;
         pHandoverUserConnection->nLengthVerified = 0;
      }
      else
      {
         uint8_t * pSwapBuffer;
         uint8_t * pBuffer = (uint8_t *) CMemoryAlloc(nLength + pHandoverUserConnection->nP2PMessageReadLength);

         if(pBuffer == null)
         {
            PDebugError("static_PHandoverP2PReadSelectMessage out of resource");
            static_PHandoverPairingCompleted(pContext,
                                             pHandoverUserConnection,
                                             W_ERROR_OUT_OF_RESOURCE);
            return;
         }

         CMemoryCopy(pBuffer,
                     pHandoverUserConnection->pP2PMessageRead,
                     pHandoverUserConnection->nP2PMessageReadLength);

         pSwapBuffer = pHandoverUserConnection->pP2PMessageRead;
         pHandoverUserConnection->pP2PMessageRead = pBuffer;

         CMemoryFree(pSwapBuffer);
      }

      CMemoryCopy(&pHandoverUserConnection->pP2PMessageRead[pHandoverUserConnection->nP2PMessageReadLength],
                  pHandoverUserConnection->aTransferBuffer,
                  nLength);

      pHandoverUserConnection->nP2PMessageReadLength += nLength;
   }

   PDebugTrace("static_PHandoverP2PReadSelectMessage Length Read: %d", pHandoverUserConnection->nP2PMessageReadLength );

   /* Analyzed data read */
   nLengthDataNotVirified = (pHandoverUserConnection->nP2PMessageReadLength - pHandoverUserConnection->nLengthVerified);

   while(nLengthDataNotVirified >= 4)
   {
      nOffset = pHandoverUserConnection->nLengthVerified;

      /* NDEF Parsing */
      bLastRecord       = pHandoverUserConnection->pP2PMessageRead[nOffset]   & P_NDEF_FLAG_MESSAGE_END;
      bPayloadPresence  = pHandoverUserConnection->pP2PMessageRead[nOffset++] & P_NDEF_ID_LENGTH_PRESENT;
      nDataInRecord     = pHandoverUserConnection->pP2PMessageRead[nOffset++]; /* REcord Type Length */
      nDataInRecord    += pHandoverUserConnection->pP2PMessageRead[nOffset++]; /* Record Payload Length */

      /* Record ID LEngth */
      if(bPayloadPresence != W_FALSE)
      {
         nDataInRecord += pHandoverUserConnection->pP2PMessageRead[nOffset++];
      }

      nDataInRecord += nOffset - pHandoverUserConnection->nLengthVerified;

      if(nLengthDataNotVirified < nDataInRecord)
      {
         /* You must continue to read data */
         break;
      }else
      {
         nLengthDataNotVirified -= nDataInRecord;
         pHandoverUserConnection->nLengthVerified += nDataInRecord;
      }
   }

   /* if some data is missing to complete the current Record or the current record is not the last
      We must continue to read data on the P2P Socket */
   if(nLengthDataNotVirified != 0 || bLastRecord == W_FALSE)
   {
      PDebugTrace("static_PHandoverP2PReadSelect more data should be read");
      PP2PRead(
            pContext,
            pHandoverUserConnection->hSocket,
            static_PHandoverP2PReadSelectMessage,
            pHandoverUserConnection,
            pHandoverUserConnection->aTransferBuffer,
            sizeof(pHandoverUserConnection->aTransferBuffer),
            &pHandoverUserConnection->hReadOperation);

      return;
   }

   nError = static_PHandoverParseSelectMessage(&pHandoverUserConnection->sHandoverMessageReceived,
                                                pHandoverUserConnection->pP2PMessageRead,
                                                pHandoverUserConnection->nP2PMessageReadLength);


   CMemoryFree(pHandoverUserConnection->pP2PMessageRead);
   pHandoverUserConnection->pP2PMessageRead = null;
   pHandoverUserConnection->nP2PMessageReadLength = 0;
   pHandoverUserConnection->nLengthTransfered = 0;
   pHandoverUserConnection->nLengthVerified = 0;


   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverP2PReadSelectMessage error during parsing handover message");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   PDebugTrace("static_PHandoverP2PReadSelectMessage Parsing OK");
   pHandoverUserConnection->sHandoverMessageReceived.bIsRequestMessage = W_FALSE;

   static_PHandoverProcessSelectReceived( pContext,
                                          pHandoverUserConnection);
}

static void static_PHandoverSelectMessageSent(
            tContext *  pContext,
            void * pCallbackParameter,
            W_ERROR  nError)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;

   PDebugTrace("static_PHandoverSelectMessageSent : %s", PUtilTraceError(nError));

   PHandleClose(pContext, pHandoverUserConnection->hWriteOperation);
   pHandoverUserConnection->hWriteOperation = W_NULL_HANDLE;

   /* Error managed by the StopPairingCompletion */
   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverSelectMessageSent Error occured");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   if(pHandoverUserConnection->nLengthTransfered < pHandoverUserConnection->nP2PMessageBufferLength)
   {
      uint32_t nDataToTransferNumber = MIN(pHandoverUserConnection->nLengthTransfered + 128,
                                           pHandoverUserConnection->nP2PMessageBufferLength);

      nDataToTransferNumber -= pHandoverUserConnection->nLengthTransfered;

      PDebugTrace("static_PHandoverSelectMessageSent data must be sent\n \
                   Total Length => %d ,Current offset => %d, Length data sent => %d",pHandoverUserConnection->nP2PMessageBufferLength,

                                                                                     pHandoverUserConnection->nLengthTransfered,
                                                                                     nDataToTransferNumber);

      PP2PWrite(  pContext,
                  pHandoverUserConnection->hSocket,
                  static_PHandoverSelectMessageSent,
                  pHandoverUserConnection,
                  &pHandoverUserConnection->pP2PMessageBuffer[pHandoverUserConnection->nLengthTransfered],
                  nDataToTransferNumber,
                  &pHandoverUserConnection->hWriteOperation);

      pHandoverUserConnection->nLengthTransfered += nDataToTransferNumber;
      return;
   }

   PDebugTrace("static_PHandoverSelectMessageSent complete");

   CMemoryFree(pHandoverUserConnection->pP2PMessageBuffer);
   pHandoverUserConnection->pP2PMessageBuffer = null;
   pHandoverUserConnection->nP2PMessageBufferLength = 0;
   pHandoverUserConnection->nLengthTransfered = 0;


   PP2PShutdown(  pContext,
                  pHandoverUserConnection->hSocket,
                  static_PHandoverP2PShutdownCompleted,
                  pHandoverUserConnection);
}

static void static_PHandoverProcessRequestReceived(
            tContext * pContext,
            tPHandoverUserConnection * pHandoverUserConnection)
{
   uint16_t nCollisionRecordSent       = pHandoverUserConnection->sHandoverMessageSent.uSpecificData.nCollisionResolutionRecord;
   uint16_t nCollisionRecordReceived   = pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.nCollisionResolutionRecord;

   PDebugTrace("static_PHandoverProcessRequestReceived | Sent Number => %04x, Received Number => %04x", nCollisionRecordSent, nCollisionRecordReceived);

   if(nCollisionRecordReceived == nCollisionRecordSent)
   {
      static_PHandoverP2PConnectionEstablished( pContext,
                                                pHandoverUserConnection,
                                                W_SUCCESS);
      return;
   }
   else
   {
      /* If I must send a select response*/
      if( ((nCollisionRecordSent & 0x0001) == (nCollisionRecordReceived & 0x0001) && (nCollisionRecordSent > nCollisionRecordReceived))
      ||  ((nCollisionRecordSent & 0x0001) != (nCollisionRecordReceived & 0x0001) && (nCollisionRecordSent < nCollisionRecordReceived)) )
      {
         PDebugTrace("static_PHandoverProcessRequestReceived | I'm the Selector");

         /* both version must be compliant, else we send a select message without carrier data */
         if( (pHandoverUserConnection->sHandoverMessageReceived.nVersion & 0xF0) !=
                  (pHandoverUserConnection->sHandoverMessageSent.nVersion & 0xF0))
         {
            static_PHandoverRemoveMessage(&pHandoverUserConnection->sHandoverMessageReceived);
         }


         /* Matching should be done on both side */
         static_PHandoverSearchMatchingCarrier(&(pHandoverUserConnection->sHandoverMessageSent.pAlternativeCarrierRecord),
                                             pHandoverUserConnection->sHandoverMessageReceived.pAlternativeCarrierRecord);

         static_PHandoverSearchMatchingCarrier(&(pHandoverUserConnection->sHandoverMessageReceived.pAlternativeCarrierRecord),
                                             pHandoverUserConnection->sHandoverMessageSent.pAlternativeCarrierRecord);

         /* Collision number must be initialized */
         pHandoverUserConnection->sHandoverMessageSent.uSpecificData.nCollisionResolutionRecord = 0;
         pHandoverUserConnection->sHandoverMessageReceived.uSpecificData.nCollisionResolutionRecord = 0;

         /*  Error management */
         pHandoverUserConnection->sHandoverMessageSent.uSpecificData.pError = null;

         /* Callback select to allow user to modify (re-add) carrier configuration data*/
         static_PHandoverPairingStarted(pContext,
                                        pHandoverUserConnection);
      }
      /* I need to received select message */
      else
      {
         PDebugTrace("static_PHandoverProcessRequestReceived | I'm the Requester");

         static_PHandoverRemoveMessage(&pHandoverUserConnection->sHandoverMessageReceived);

         PP2PRead(   pContext,
                     pHandoverUserConnection->hSocket,
                     static_PHandoverP2PReadSelectMessage,
                     pHandoverUserConnection,
                     pHandoverUserConnection->aTransferBuffer,
                     sizeof(pHandoverUserConnection->aTransferBuffer),
                     &pHandoverUserConnection->hReadOperation);
      }
   }
}

static void static_PHandoverP2PFirstReadResponseCompleted(
            tContext *  pContext,
            void *      pCallbackParameter,
            uint32_t    nLength,
            W_ERROR     nError)
{
   bool_t bPayloadPresence   = W_FALSE;
   bool_t bLastRecord        = W_FALSE;
   uint32_t  nDataInRecord  = 0;
   uint32_t  nOffset        = 0;

   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;
   uint32_t nLengthDataNotVirified = 0;

   PDebugTrace("static_PHandoverP2PFirstReadResponseCompleted : %s", PUtilTraceError(nError));

   PHandleClose(pContext, pHandoverUserConnection->hReadOperation);
   pHandoverUserConnection->hReadOperation = W_NULL_HANDLE;

   /* if other error */
   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverP2PFirstReadResponseCompleted Error occured");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   if(nLength > 0)
   {
      if(pHandoverUserConnection->pP2PMessageRead == null)
      {
         pHandoverUserConnection->pP2PMessageRead = (uint8_t *) CMemoryAlloc(nLength);

         if(pHandoverUserConnection->pP2PMessageRead == null)
         {
            PDebugError("static_PHandoverP2PFirstReadResponseCompleted out of resource");
            static_PHandoverPairingCompleted(pContext,
                                             pHandoverUserConnection,
                                             W_ERROR_OUT_OF_RESOURCE);
            return;
         }

         pHandoverUserConnection->nP2PMessageReadLength  = 0;
         pHandoverUserConnection->nLengthVerified = 0;
      }
      else
      {
         uint8_t * pSwapBuffer;
         uint8_t * pBuffer = (uint8_t *) CMemoryAlloc(nLength + pHandoverUserConnection->nP2PMessageReadLength);

         if(pBuffer == null)
         {
            PDebugError("static_PHandoverP2PFirstReadResponseCompleted out of resource");
            static_PHandoverPairingCompleted(pContext,
                                             pHandoverUserConnection,
                                             W_ERROR_OUT_OF_RESOURCE);
            return;
         }

         CMemoryCopy(pBuffer,
                     pHandoverUserConnection->pP2PMessageRead,
                     pHandoverUserConnection->nP2PMessageReadLength);

         pSwapBuffer = pHandoverUserConnection->pP2PMessageRead;
         pHandoverUserConnection->pP2PMessageRead = pBuffer;

         CMemoryFree(pSwapBuffer);
      }

      CMemoryCopy(&pHandoverUserConnection->pP2PMessageRead[pHandoverUserConnection->nP2PMessageReadLength],
                  pHandoverUserConnection->aTransferBuffer,
                  nLength);

      pHandoverUserConnection->nP2PMessageReadLength += nLength;
   }

   /* Analyzed data read */
   nLengthDataNotVirified = (pHandoverUserConnection->nP2PMessageReadLength - pHandoverUserConnection->nLengthVerified);

   while(nLengthDataNotVirified >= 4)
   {
      nOffset = pHandoverUserConnection->nLengthVerified;

      /* NDEF Parsing */
      bLastRecord       = pHandoverUserConnection->pP2PMessageRead[nOffset]   & P_NDEF_FLAG_MESSAGE_END;
      bPayloadPresence  = pHandoverUserConnection->pP2PMessageRead[nOffset++] & P_NDEF_ID_LENGTH_PRESENT;
      nDataInRecord     = pHandoverUserConnection->pP2PMessageRead[nOffset++]; /* REcord Type Length */
      nDataInRecord    += pHandoverUserConnection->pP2PMessageRead[nOffset++]; /* Record Payload Length */

      /* Record ID LEngth */
      if(bPayloadPresence != W_FALSE)
      {
         nDataInRecord += pHandoverUserConnection->pP2PMessageRead[nOffset++];
      }

      nDataInRecord += nOffset - pHandoverUserConnection->nLengthVerified;

      if(nLengthDataNotVirified < nDataInRecord)
      {
         /* You must continue to read data */
         break;
      }else
      {
         nLengthDataNotVirified -= nDataInRecord;
         pHandoverUserConnection->nLengthVerified += nDataInRecord;
      }
   }

   /* if some data is missing to complete the current Record or the current record is not the last
      We must continue to read data on the P2P Socket */
   if(nLengthDataNotVirified != 0 || bLastRecord == W_FALSE)
   {
      PP2PRead(
            pContext,
            pHandoverUserConnection->hSocket,
            static_PHandoverP2PFirstReadResponseCompleted,
            pHandoverUserConnection,
            pHandoverUserConnection->aTransferBuffer,
            sizeof(pHandoverUserConnection->aTransferBuffer),
            &pHandoverUserConnection->hReadOperation);

      return;
   }

   nError = static_PHandoverParseMessage(&pHandoverUserConnection->sHandoverMessageReceived,
                                          pHandoverUserConnection->pP2PMessageRead,
                                          pHandoverUserConnection->nP2PMessageReadLength );

   CMemoryFree(pHandoverUserConnection->pP2PMessageRead);
   pHandoverUserConnection->pP2PMessageRead = null;
   pHandoverUserConnection->nP2PMessageReadLength  = 0;


   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverP2PFirstReadCompleted error during parsing handover message");
         static_PHandoverPairingCompleted(pContext,
                                          pHandoverUserConnection,
                                          nError);
         return;
   }

   /* If you receive a Select Message,  you can stop the traitement*/
   if(pHandoverUserConnection->sHandoverMessageReceived.bIsRequestMessage == W_FALSE)
   {
      static_PHandoverProcessSelectReceived( pContext,
                                             pHandoverUserConnection);
      return;
   }

   /* else it is a Request Message */
   CDebugAssert(pHandoverUserConnection->sHandoverMessageReceived.bIsRequestMessage != W_FALSE);

   static_PHandoverProcessRequestReceived(pContext,
                                          pHandoverUserConnection);
}

static void static_PHandoverP2PRequestMessageSent(
            tContext * pContext,
            void     * pCallbackParameter,
            W_ERROR     nError)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;
   PDebugTrace("static_PHandoverP2PRequestMessageSent : %s", PUtilTraceError(nError));

   PHandleClose(pContext, pHandoverUserConnection->hWriteOperation);
   pHandoverUserConnection->hWriteOperation = W_NULL_HANDLE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverP2PRequestMessageSent Error occured");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   /* If all data are not already transafered */
   if(pHandoverUserConnection->nLengthTransfered <
            pHandoverUserConnection->nP2PMessageBufferLength)
   {
      uint32_t nDataToTransferNumber;
      nDataToTransferNumber =       MIN(pHandoverUserConnection->nLengthTransfered + P_HANDOVER_MAX_MIU,
                                                       pHandoverUserConnection->nP2PMessageBufferLength);

      nDataToTransferNumber -= pHandoverUserConnection->nLengthTransfered;

      PDebugTrace("static_PHandoverP2PRequestMessageSent data must be sent\n \
                   Total Length => %d ,Current offset => %d, Length data sent => %d",pHandoverUserConnection->nP2PMessageBufferLength,
                                                                                     pHandoverUserConnection->nLengthTransfered,
                                                                                     nDataToTransferNumber);

      PP2PWrite(
         pContext,
         pHandoverUserConnection->hSocket,
         static_PHandoverP2PRequestMessageSent,
         pHandoverUserConnection,
         &pHandoverUserConnection->pP2PMessageBuffer[pHandoverUserConnection->nLengthTransfered],
         nDataToTransferNumber,
         &pHandoverUserConnection->hWriteOperation);

      pHandoverUserConnection->nLengthTransfered +=nDataToTransferNumber;
      return;
   }

   PDebugTrace("static_PHandoverP2PRequestMessageSent complete");

   /* All data sent */
   CMemoryFree(pHandoverUserConnection->pP2PMessageBuffer);
   pHandoverUserConnection->pP2PMessageBuffer = null;
   pHandoverUserConnection->nP2PMessageBufferLength = 0;
   pHandoverUserConnection->nLengthTransfered = 0;

}

static void static_PHandoverP2PConnectionEstablished(
            tContext * pContext,
            void     * pCallbackParameter,
            W_ERROR     nError)
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;

   PDebugTrace("static_PHandoverP2PConnectionEstablished : %s", PUtilTraceError(nError));

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandoverP2PConnectionEstablished Error occured");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       nError);
      return;
   }

   /* Generates Random, Build Resquest Message and sends it*/

   PContextDriverGenerateRandom(pContext);


   pHandoverUserConnection->sHandoverMessageSent.uSpecificData.nCollisionResolutionRecord =
                     (uint16_t) PContextDriverGenerateRandom(pContext);

   pHandoverUserConnection->pP2PMessageBuffer = static_PHandoverBuildRequestMessage(
                                                      &pHandoverUserConnection->sHandoverMessageSent,
                                                      &pHandoverUserConnection->nP2PMessageBufferLength);

   if(pHandoverUserConnection->pP2PMessageBuffer == null)
   {
      PDebugError("static_PHandoverP2PConnectionEstablished return null pointer during the creation of the NDEF representation of the request message");
      static_PHandoverPairingCompleted(pContext,
                                       pHandoverUserConnection,
                                       W_ERROR_OUT_OF_RESOURCE);
      return;
   }

   pHandoverUserConnection->nLengthTransfered = MIN(P_HANDOVER_MAX_MIU,
                                                    pHandoverUserConnection->nP2PMessageBufferLength);

   PDebugTrace("static_PHandoverP2PRequestMessageSent data must be sent\n \
                   Total Length => %d ,Current offset => %d, Length data sent => %d",pHandoverUserConnection->nP2PMessageBufferLength,
                                                                                     0,
                                                                                     pHandoverUserConnection->nLengthTransfered);

   PP2PWrite(
      pContext,
      pHandoverUserConnection->hSocket,
      static_PHandoverP2PRequestMessageSent,
      pHandoverUserConnection,
      pHandoverUserConnection->pP2PMessageBuffer,
      pHandoverUserConnection->nLengthTransfered,
      &pHandoverUserConnection->hWriteOperation);

   /* First Read the first Message Record */
   PP2PRead(
      pContext,
      pHandoverUserConnection->hSocket,
      static_PHandoverP2PFirstReadResponseCompleted,
      pHandoverUserConnection,
      pHandoverUserConnection->aTransferBuffer,
      sizeof(pHandoverUserConnection->aTransferBuffer),
      &pHandoverUserConnection->hReadOperation);

}

/**
 * @brief Callback received when the P2P Link is established
 **/
static void static_PHandoverP2PLinkEstablished(
            tContext* pContext,
            void * pCallbackParameter,
            W_HANDLE hHandle,
            W_ERROR nResult )
{
   tPHandoverUserConnection * pHandoverUserConnection = (tPHandoverUserConnection *) pCallbackParameter;

   PDebugTrace("static_PHandoverP2PLinkEstablished : %s", PUtilTraceError(nResult));

   PHandleClose(pContext,
                pHandoverUserConnection->hLinkOperation);
   pHandoverUserConnection->hLinkOperation = W_NULL_HANDLE;

   /* if an other error occured */
   if(nResult != W_SUCCESS)
   {
      PDebugError("static_PHandoverP2PLinkEstablished error during the establishment of the link");
      static_PHandoverPairingCompleted(
                  pContext,
                  pHandoverUserConnection,
                  nResult);
      return;
   }

   pHandoverUserConnection->hLink = hHandle;
   PP2PConnectDriver(pContext,
                     pHandoverUserConnection->hSocket,
                     pHandoverUserConnection->hLink,
                     static_PHandoverP2PConnectionEstablished,
                     pHandoverUserConnection);
}

/**
 * @brief Callback called when the P2P Connection is released
 **/
static void static_PHandoverP2PLinkReleased(
            tContext* pContext,
            void * pCallbackParameter,
            W_ERROR nResult )
{
   PDebugTrace("Connection P2P Released");
}

/**
 * See API Specification
 **/
void PHandoverPairingStart(
            tContext *  pContext,
            W_HANDLE hHandoverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void * pCallbackParameter,
            uint32_t nMode,
            W_HANDLE* phOperation)
{
   tDFCCallbackContext sCallbackContext;
   tPHandoverUserConnection * pHandoverUserConnection;

    /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hHandoverConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);

   PDFCFillCallbackContext(pContext,
                           (tDFCCallback *) pCallback,
                           pCallbackParameter,
                           &sCallbackContext);

   PDebugTrace("PHandoverPairingStart");

   if(nError != W_SUCCESS || pHandoverUserConnection == null)
   {
      PDebugError("PHandoverPairingStart BAD Parameter");
      PDFCPostContext2(&sCallbackContext, nError);
      return;
   }

   if(pHandoverUserConnection->nState != P_HANDOVER_STATE_FREE)
   {
      PDebugError("PHandoverPairingStart BAD state, operation already pending");
      PDFCPostContext2(&sCallbackContext, W_ERROR_BAD_STATE);
      return;
   }

   pHandoverUserConnection->sCallbackContext = sCallbackContext;

   /* Reset SelectMessage */
   static_PHandoverRemoveMessage(&pHandoverUserConnection->sHandoverMessageReceived);

   switch(nMode)
   {
      case W_HANDOVER_PAIRING_READER:
      {
         const char16_t aHandoverSelectMessageType[] = {'H','s', '\0'};

         pHandoverUserConnection->bIsRequester = W_TRUE;
         pHandoverUserConnection->hReaderRegistry = W_NULL_HANDLE;

         PDebugTrace("PHandoverPairingStart Reader Mode");

         PNDEFReadMessageOnAnyTagInternal(pContext,
                                          static_PHandoverReadSelectMessageCompleted,
                                          pHandoverUserConnection,
                                          W_PRIORITY_MAXIMUM,
                                          W_NDEF_TNF_WELL_KNOWN,
                                          aHandoverSelectMessageType,
                                          &pHandoverUserConnection->hReaderRegistry);

         break;
      }

      case W_HANDOVER_PAIRING_VIRTUAL_TAG:
      {
         uint8_t nTagType = 0;
         uint32_t nVirtualMessageLength = 0;
         W_HANDLE hMessage;
         uint8_t* pBuffer;
         const uint8_t aPUPI [] = {0x01, 0x02, 0x03, 0x04};
         pHandoverUserConnection->bIsRequester = W_FALSE;

         PDebugTrace("PHandoverPairingStart Virtual Mode");
         if(PEmulIsPropertySupported(pContext, W_PROP_NFC_TAG_TYPE_4_B) != W_FALSE)
         {
            nTagType = W_PROP_NFC_TAG_TYPE_4_B;
         }
         else if(PEmulIsPropertySupported(pContext, W_PROP_NFC_TAG_TYPE_4_A) != W_FALSE)
         {
            nTagType = W_PROP_NFC_TAG_TYPE_4_A;
         }
         else
         {
            PDebugError("PHandoverPairingStart Virtual Mode  not Supported");
            PDFCPostContext2(&sCallbackContext, W_ERROR_RF_PROTOCOL_NOT_SUPPORTED);
            return;
         }

         /* Write Select Message */
         pBuffer = static_PHandoverBuildSelectMessage(&pHandoverUserConnection->sHandoverMessageSent, &nVirtualMessageLength);

         if(pBuffer == null)
         {
            PDebugError("PHandoverPairingStart Virtual Mode  error during allocation of memory");
            PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
            return;
         }

         nError = PVirtualTagCreate(pContext,nTagType, aPUPI, sizeof(aPUPI), nVirtualMessageLength, &pHandoverUserConnection->hVirtualTag);

         if(nError != W_SUCCESS)
         {
            PDebugError("PHandoverPairingStart Virtual Mode  error on creating VirtualTag");
            CMemoryFree(pBuffer);
            PDFCPostContext2(&sCallbackContext, nError);
            return;
         }

         nError   =  PNDEFBuildMessage(pContext,
                                       pBuffer,
                                       nVirtualMessageLength,
                                       &hMessage);

         CMemoryFree(pBuffer);

         if(nError != W_SUCCESS)
         {
            PDebugError("PHandoverPairingStart Virtual Mode  error during the message parsing");
            PDFCPostContext2(&sCallbackContext, nError);
            return;
         }

         PNDEFWriteMessage(pContext,
                           pHandoverUserConnection->hVirtualTag,
                           static_PHandoverWriteMessageCompleted, pHandoverUserConnection,
                           hMessage,
                           W_NDEF_ACTION_BIT_ERASE | W_NDEF_ACTION_BIT_CHECK_WRITE,
                           null);

         PHandleClose( pContext,
                       hMessage);

         break;
      }


      case W_HANDOVER_PAIRING_P2P_ANY:
      {
         bool_t bValue = W_FALSE;

         if(((nError = PNFCControllerGetBooleanProperty(pContext, W_NFCC_PROP_P2P,  &bValue)) != W_SUCCESS )
         || (bValue == W_FALSE))
         {
            PDebugError("PHandoverPairingStart RF PROTOCOL NOT SUPPORTED (P2P)");
            PDFCPostContext2(&sCallbackContext, W_ERROR_RF_PROTOCOL_NOT_SUPPORTED);
            return;
         }

         nError = PP2PCreateSocketDriver(pContext, W_P2P_TYPE_CLIENT_SERVER,
               static_aHandoverServiceName, (1 + PUtilStringLength(static_aHandoverServiceName)) * sizeof (char16_t), 0, &pHandoverUserConnection->hSocket);

         if(nError != W_SUCCESS)
         {
            PDebugError("PHandoverPairingStart Error during the driver socket opening");
            PDFCPostContext2(&sCallbackContext, nError);
            return;
         }

         PP2PEstablishLinkWrapper(pContext, static_PHandoverP2PLinkEstablished, pHandoverUserConnection,static_PHandoverP2PLinkReleased, pHandoverUserConnection, &pHandoverUserConnection->hLinkOperation);
         break;
      }

      default:
         PDebugError("PHandoverPairingStart BAD parameter nMode = %d", nMode);
         PDFCPostContext2(&sCallbackContext, W_ERROR_BAD_PARAMETER);
         return;
   }

   pHandoverUserConnection->nState = P_HANDOVER_STATE_PAIRING_STARTED;
   pHandoverUserConnection->nMode = nMode;

   if(phOperation != null)
   {
      /* Get an operation handle */
      *phOperation                          = PBasicCreateOperation( pContext, static_PHandoverStopPairingStart, pHandoverUserConnection );
      pHandoverUserConnection->hStopPairing = *phOperation;
      pHandoverUserConnection->hStopPairing = W_NULL_HANDLE;
   }

   PHandleIncrementReferenceCount(pHandoverUserConnection);
}

void PHandoverPairingCompletion(
            tContext *  pContext,
            W_HANDLE hHandoverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void * pCallbackParameter,
            W_HANDLE* phOperation)
{
   tDFCCallbackContext sCallbackContext;
   tPHandoverUserConnection * pHandoverUserConnection;

    /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hHandoverConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);

   PDFCFillCallbackContext(pContext,
                           (tDFCCallback *) pCallback,
                           pCallbackParameter,
                           &sCallbackContext);

   PDebugTrace("PHandoverPairingCompletion");

   if(nError != W_SUCCESS || pHandoverUserConnection == null)
   {
      PDebugError("PHandoverPairingCompletion BAD Parameter");
      PDFCPostContext2(&sCallbackContext, nError);
      return;
   }

   if(pHandoverUserConnection->nState != P_HANDOVER_STATE_WAIT_COMPLETION)
   {
      PDebugError("PHandoverPairingCompletion BAD state, the completion cannot be used in this state");
      PDFCPostContext2(&sCallbackContext, W_ERROR_BAD_STATE);
      return;
   }

   pHandoverUserConnection->nState = P_HANDOVER_STATE_COMPLETION_STARTED;

   pHandoverUserConnection->sCallbackContext = sCallbackContext;

   PHandleIncrementReferenceCount(pHandoverUserConnection);

   if(phOperation != null)
   {
      /* Get an operation handle */
      *phOperation                          = PBasicCreateOperation( pContext, static_PHandoverStopPairingCompletion, pHandoverUserConnection );
      pHandoverUserConnection->hStopPairing = *phOperation;
      pHandoverUserConnection->hStopPairing = W_NULL_HANDLE;
   }

   /* Build Select Message */
   pHandoverUserConnection->pP2PMessageBuffer = static_PHandoverBuildSelectMessage(&pHandoverUserConnection->sHandoverMessageSent,
                                                                                 &pHandoverUserConnection->nP2PMessageBufferLength);

   pHandoverUserConnection->nLengthTransfered = MIN(P_HANDOVER_MAX_MIU,
                                                      pHandoverUserConnection->nP2PMessageBufferLength);

   PDebugTrace("PHandoverPairingCompletion data must be sent\n \
               Total Length => %d ,Current offset => %d, Length data sent => %d",pHandoverUserConnection->nP2PMessageBufferLength,
                                                                                 0,
                                                                                 pHandoverUserConnection->nLengthTransfered);
   PP2PWrite(  pContext,
               pHandoverUserConnection->hSocket,
               static_PHandoverSelectMessageSent,
               pHandoverUserConnection,
               pHandoverUserConnection->pP2PMessageBuffer,
               pHandoverUserConnection->nLengthTransfered,
               &pHandoverUserConnection->hWriteOperation);
}

/**
 *See Header
 **/
void PHandoverFormatTag(
      tContext *  pContext,
      W_HANDLE hHandoverConnection,
      tPBasicGenericCallbackFunction* pCallback,
      void * pCallbackParameter,
      uint32_t nActionMask,
      W_HANDLE* phRegistry)
{
   tDFCCallbackContext sCallbackContext;
   tPHandoverUserConnection * pHandoverUserConnection;
   uint8_t * pBuffer = null;
   uint32_t nLength = 0;
   W_HANDLE hMessage;

    /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hHandoverConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);

   PDFCFillCallbackContext(pContext,
                           (tDFCCallback *) pCallback,
                           pCallbackParameter,
                           &sCallbackContext);

   *phRegistry = W_NULL_HANDLE;

   PDebugTrace("PHandoverFormatTag");

   if(nError != W_SUCCESS || pHandoverUserConnection == null)
   {
      PDebugError("PHandoverFormatTag BAD Parameter");
      PDFCPostContext2(&sCallbackContext, nError);
      return;
   }

   if(pHandoverUserConnection->nState != P_HANDOVER_STATE_FREE)
   {
      PDebugError("PHandoverFormatTag BAD state, operation already pending");
      PDFCPostContext2(&sCallbackContext, W_ERROR_BAD_STATE);
      return;
   }
   pHandoverUserConnection->nState = P_HANDOVER_STATE_PAIRING_STARTED;

   pBuffer = static_PHandoverBuildSelectMessage(
                  &pHandoverUserConnection->sHandoverMessageSent,
                  &nLength);

   if(pBuffer == null)
   {
      pHandoverUserConnection->nState = P_HANDOVER_STATE_FREE;
      PDebugError("PHandoverFormatTag NOT ENOUGH Memory");
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
      return;
   }

   nError = PNDEFBuildMessage(
                     pContext,
                     pBuffer,
                     nLength,
                     &hMessage);



   CMemoryFree(pBuffer);
   if(nError != W_SUCCESS)
   {
      pHandoverUserConnection->nState = P_HANDOVER_STATE_FREE;
      PDebugError("PHandoverFormatTag during Builduing Select Message error");
      PDFCPostContext2(&sCallbackContext, nError);
      return;
   }

   PNDEFWriteMessageOnAnyTag(
                     pContext,
                     pCallback,
                     pCallbackParameter,
                     W_PRIORITY_MAXIMUM,
                     hMessage,
                     nActionMask,
                     phRegistry);

   /* Close Message when it will be released */
   PHandleClose(pContext, hMessage);
}

/**
 *See API Interface
 **/
W_ERROR PHandoverGetCarrierInfo(
      tContext *  pContext,
      W_HANDLE hConnection,
      tPHandoverCarrierType * pCarrierType,
      tPHandoverParserFunction * pParsingFunction,
      void * pData)
{
   tPHandoverUserConnection * pHandoverUserConnection;
   tPAlternativeCarrierRecord * pAlternativeCarrier;
   uint8_t * pTemporaryData;
   uint32_t nOffset = 0;
   uint8_t nIndex = 0;

    /* Get the PHandoverConnection Object by its handle */
   W_ERROR nError = PHandleGetObject(pContext, hConnection, &g_sHandoverUserConnection, (void **)&pHandoverUserConnection);

   PDebugTrace("PHandoverGetCarrierInfo");

   if (pHandoverUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if(nError != W_SUCCESS)
   {
      PDebugError("PHandoverGetCarrierInfo BAD Parameter");
      return nError;
   }

   pAlternativeCarrier = static_PHandoverSearchAlternativeRecord(
                              pHandoverUserConnection->sHandoverMessageReceived.pAlternativeCarrierRecord,
                              pCarrierType);

   if(pAlternativeCarrier == null)
   {
      return W_ERROR_ITEM_NOT_FOUND;
   }

   if(pAlternativeCarrier == null)
   {
      return W_ERROR_BAD_STATE;
   }

   pTemporaryData = (uint8_t *) CMemoryAlloc(
                     static_PHandoverGetDataLengthOfAlternativeCarrier(pAlternativeCarrier)
                     );

   if(pTemporaryData == null)
   {
      PDebugError("PHandoverPairingStart OUT OF RESOURCE");
      return W_ERROR_OUT_OF_RESOURCE;
   }

   CMemoryCopy(pTemporaryData,
               pAlternativeCarrier->sConfigurationData.pData,
               pAlternativeCarrier->sConfigurationData.nLength);

   nOffset += pAlternativeCarrier->sConfigurationData.nLength;

   while(nIndex < pAlternativeCarrier->nAuxiliaryDataReferenceCount)
   {
      CMemoryCopy(&pTemporaryData[nOffset],
                  pAlternativeCarrier->pAuxiliaryConfigurationData[nIndex].pData,
                  pAlternativeCarrier->pAuxiliaryConfigurationData[nIndex].nLength);

      nOffset += pAlternativeCarrier->pAuxiliaryConfigurationData[nIndex].nLength;

      nIndex++;
   }

   nError = pParsingFunction(pData, pTemporaryData, nOffset);

   CMemoryFree(pTemporaryData);
   return nError;
}

#endif /* ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC))  && (defined P_INCLUDE_PAIRING) */

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC))  && (! defined P_INCLUDE_PAIRING)

/* See client API */
W_ERROR PHandoverCreate(
      tContext * pContext,
      W_HANDLE* phMessage)
{
   if (phMessage != null)
   {
      * phMessage = W_NULL_HANDLE;
   }

   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See client API */

W_ERROR PHandoverRemoveAllCarrier(
      tContext * pContext,
      W_HANDLE hConnection)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See client API */
void PHandoverPairingStart(
   tContext *  pContext,
   W_HANDLE hHandoverConnection,
   tPBasicGenericCallbackFunction* pCallback,
   void * pCallbackParameter,
   uint32_t nMode,
   W_HANDLE* phOperation)
{
   tDFCCallbackContext sCallbackContext;
   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

/* See client API */
void PHandoverPairingCompletion(
            tContext *  pContext,
            W_HANDLE hHandoverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void * pCallbackParameter,
            W_HANDLE* phOperation)
{
   tDFCCallbackContext sCallbackContext;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

/* See client API */
void PHandoverFormatTag(
      tContext *  pContext,
      W_HANDLE hHandoverConnection,
      tPBasicGenericCallbackFunction* pCallback,
      void * pCallbackParameter,
      uint32_t nActionMask,
      W_HANDLE* phRegistry)
{
   tDFCCallbackContext sCallbackContext;

   if (phRegistry != null)
   {
      * phRegistry = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

/* See client API */
W_ERROR PHandoverGetPairingInfo(
      tContext *  pContext,
      W_HANDLE hConnection,
      tWHandoverPairingInfo* pPairingInfo)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See client API */
W_ERROR PHandoverGetPairingInfoLength(
      tContext *  pContext,
      W_HANDLE hConnection,
      uint32_t * pnLength)
{
   if (pnLength != null)
   {
      * pnLength = 0;
   }

   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

#endif /* ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC))  && (! defined P_INCLUDE_PAIRING) */

/* EOF */


